package c4.subnetzero.shipsdroid.controller;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import c4.subnetzero.shipsdroid.GameActivity;
import c4.subnetzero.shipsdroid.NetService;
import c4.subnetzero.shipsdroid.Utils;
import c4.subnetzero.shipsdroid.controller.state.Disconnected;
import c4.subnetzero.shipsdroid.controller.state.IGameState;
import c4.subnetzero.shipsdroid.controller.state.PeerReady;
import c4.subnetzero.shipsdroid.controller.state.Playing;
import c4.subnetzero.shipsdroid.model.AbstractFleetModel;
import c4.subnetzero.shipsdroid.model.EnemyFleetModel;
import c4.subnetzero.shipsdroid.model.OwnFleetModel;
import c4.subnetzero.shipsdroid.model.Ship;
import c4.subnetzero.shipsdroid.net.Message;
import c4.subnetzero.shipsdroid.view.EnemyFleetView;

import java.util.ArrayList;


public final class GameEngine implements NetService.Listener, ShotClock.Listener
{
   private static final String LOG_TAG = "GameEngine";
   private ShotClock mShotClock;
   private NetService mNetService;
   private Handler mUiHandler;
   private OwnFleetModel ownFleetModel = null;
   private EnemyFleetModel enemyFleetModel = null;
   private AbstractFleetModel.ModelUpdateListener ownFleetModelUpdateListener = null;
   private AbstractFleetModel.ModelUpdateListener enemyFleetModelUpdateListener = null;
   private StateListener stateListener = null;
   private ScoreListener scoreListener = null;
   private volatile boolean myTurnFlag = false;
   private boolean gotAWinner = false;
   //private IGameState currentState = new Started(this);
   private IGameState currentState = new PeerReady(this);
   private Context mContext;

   public GameEngine(final Context context, final NetService netService)
   {
      mContext = context;
      mNetService = netService;
      mNetService.setListener(this);

      if(mContext instanceof GameActivity){
         mUiHandler = ((GameActivity)mContext).getUiHandler();
      }else {
         throw new IllegalStateException("Called from wrong activity");
      }


      mShotClock = new ShotClock();
      mShotClock.setListener(this);
   }

   public void setModelUpdateListener(final AbstractFleetModel.ModelUpdateListener own, final AbstractFleetModel.ModelUpdateListener enemy)
   {
      ownFleetModelUpdateListener = own;
      enemyFleetModelUpdateListener = enemy;
   }

   public void setStateListener(final StateListener listener)
   {
      stateListener = listener;
   }

   public void setScoreListener(final ScoreListener listener)
   {
      scoreListener = listener;
   }

   public void setState(final IGameState newState)
   {
      currentState = newState;
      if (stateListener != null) {
         stateListener.onStateChange(newState);
      }
   }

   public NetService getNetService()
   {
      return mNetService;
   }

   public String getStateName()
   {
      return currentState.getClass().getSimpleName();
   }

   public IGameState getState()
   {
      return currentState;
   }

   public ShotClock getShotClock()
   {
      return mShotClock;
   }

   public Context getContext()
   {
      return mContext;
   }

   public void startNetReveiver()
   {
      currentState.startNetReceiver();
   }

   public void stopNetReceiver()
   {
      currentState.stopNetReceiver();
   }

   public void connectPeer()
   {
      currentState.connectPeer(null);
   }

   public void disconnectPeer()
   {
      currentState.disconnectPeer();
   }

   public void newGame()
   {
      currentState.newGame();
   }

   public void abortGame()
   {
      currentState.abortGame();
   }

   public void shoot(final int i, final int j)
   {
      currentState.shoot(i, j);
   }

   // FIXME: This method obviously needs some refactoring ;)
   @Override
   public void onMessage(Message msg, final String peerId)
   {
      switch (getStateName()) {
         case "Disconnected":
            if (msg.TYPE == Message.CTRL && msg.SUB_TYPE == Message.CONNECT) {
               msg.ACK_FLAG = true;
               msg.RST_FLAG = false;
               mNetService.sendMessage(msg);
               setState(new PeerReady(this));
            }
            break;
         case "Connecting":
            if (msg.TYPE == Message.CTRL && msg.SUB_TYPE == Message.CONNECT) {

               //Dialogs.closeMsgDialog();

               if (msg.ACK_FLAG && !msg.RST_FLAG) {
                  setState(new PeerReady(this));
               }

               /*
               if (msg.ACK_FLAG && msg.RST_FLAG) {
                  setState(new Disconnected(this));
                  Dialogs.showOkMsg("Connection rejected!");
               }*/
            }
            break;
         case "PeerReady":
         case "Playing":

            if (msg.TYPE == Message.CTRL) {

               /*
               if (msg.SUB_TYPE == Message.CONNECT) {
                  msg.ACK_FLAG = true;
                  msg.RST_FLAG = true;
                  netController.sendMessage(msg, peerId.split(":")[0]);
               }*/

               if (msg.SUB_TYPE == Message.DISCONNECT) {
                  setState(new Disconnected(this));
                  return;
               }
            }

            if (msg.TYPE == Message.GAME) {

               if (msg.SUB_TYPE == Message.NEW) {
                  myTurnFlag = msg.ACK_FLAG;
                  if (!msg.ACK_FLAG && !msg.RST_FLAG) {
                     msg.ACK_FLAG = true;
                     mNetService.sendMessage(msg);
                  }
                  setState(new Playing(this));
                  startNewGame();
                  return;
               }

               if (msg.SUB_TYPE == Message.ABORT) {
                  setPlayerEnabled(true);
                  mShotClock.stop();
                  setState(new PeerReady(this));

                  if (!gotAWinner) {
                     Utils.showOkMsg(mContext, "Game aborted by peer!");
                  }
                  return;
               }

               if (msg.SUB_TYPE == Message.TIMEOUT) {
                  Log.d(LOG_TAG, "Timeout received");
                  setPlayerEnabled(true);
                  return;
               }

               if (msg.SUB_TYPE == Message.SHOOT) {

                  ArrayList payload = (ArrayList) msg.PAYLOAD;

                  int resultFlag;
                  int i = ((Double) payload.get(0)).intValue();
                  int j = ((Double) payload.get(1)).intValue();

                  if (msg.ACK_FLAG) {

                     resultFlag = ((Double) payload.get(2)).intValue();
                     Ship ship = msg.SHIP;

                     enemyFleetModel.update(i, j, resultFlag, ship);

                     if (enemyFleetModel.isFleetDestroyed()) {
                        gotAWinner = true;
                        setScore(ownFleetModel.getShipsLeft(), 0);
                        /*
                        if (scoreListener != null) {
                           scoreListener.onScoreUpdate(ownFleetModel.getShipsLeft(), 0);
                        }*/
                        //FIXME: should be currentState.finishGame();
                        currentState.abortGame();
                        Utils.showOkMsg(mContext, "You are the Winner!");
                        return;
                     }

                     myTurnFlag = resultFlag == AbstractFleetModel.HIT || resultFlag == AbstractFleetModel.DESTROYED;

                  } else {

                     Object[] result = ownFleetModel.update(i, j);

                     resultFlag = (Integer) result[0];
                     Ship ship = (Ship) result[1];

                     msg.ACK_FLAG = true;
                     msg.PAYLOAD = new Object[]{i, j, resultFlag};
                     msg.SHIP = ship;
                     mNetService.sendMessage(msg);

                     if (ownFleetModel.isFleetDestroyed()) {
                        gotAWinner = true;
                        setScore(0, enemyFleetModel.getShipsLeft());
                        /*
                        if (scoreListener != null) {
                           scoreListener.onScoreUpdate(0, enemyFleetModel.getShipsLeft());
                        }*/
                        Utils.showOkMsg(mContext, "You lose!");
                        return;
                     }

                     myTurnFlag = !(resultFlag == AbstractFleetModel.HIT || resultFlag == AbstractFleetModel.DESTROYED);
                  }

                  if (resultFlag == AbstractFleetModel.DESTROYED) {
                     setScore(ownFleetModel.getShipsLeft(), enemyFleetModel.getShipsLeft());
                     /*
                     if (scoreListener != null) {
                        scoreListener.onScoreUpdate(ownFleetModel.getShipsLeft(), enemyFleetModel.getShipsLeft());
                     }*/
                  }

                  setPlayerEnabled(myTurnFlag);
               }
            }

            break;
         default:
            //TODO: Maybe send some sort of reject message
            break;
      }
   }

   @Override
   public void onPeerReady()
   {

   }

   private void startNewGame()
   {
      gotAWinner = false;
      ownFleetModel = new OwnFleetModel(ownFleetModelUpdateListener);
      enemyFleetModel = new EnemyFleetModel(enemyFleetModelUpdateListener);

      setScore(AbstractFleetModel.NUMBER_OF_SHIPS, AbstractFleetModel.NUMBER_OF_SHIPS);
      /*
      if (scoreListener != null) {
         scoreListener.onScoreUpdate(AbstractFleetModel.NUMBER_OF_SHIPS, AbstractFleetModel.NUMBER_OF_SHIPS);
      }*/

      setPlayerEnabled(myTurnFlag);
   }

   private void setScore(final int myShips, final int enemyShips)
   {
      android.os.Message msg = android.os.Message.obtain();
      msg.what = GameActivity.UPDATE_SCORE_BOARD;
      msg.arg1 = myShips;
      msg.arg2 = enemyShips;
      mUiHandler.sendMessage(msg);
   }


   public void setPlayerEnabled(final boolean enable)
   {
      Log.d(LOG_TAG, "setPlayerEnabled():" + enable);

      if (enable) {
         mShotClock.reset();
      } else {
         mShotClock.stop();
      }
      ((EnemyFleetView) enemyFleetModelUpdateListener).setEnabled(enable);
   }

   /*
   @Override
   public void onError(String errMsg)
   {
      //TODO: Inform the user
   }*/

   @Override
   public void onTimeIsUp()
   {
      Log.d(LOG_TAG, "onTimeIsUp()");
      setPlayerEnabled(false);
      Message timeoutMsg = new Message();
      timeoutMsg.TYPE = Message.GAME;
      timeoutMsg.SUB_TYPE = Message.TIMEOUT;
      mNetService.sendMessage(timeoutMsg);
   }

   @Override
   public void onTick(final int tick)
   {
      //Log.d(LOG_TAG,"Tick: "+tick);
      android.os.Message msg = android.os.Message.obtain();
      msg.what = GameActivity.UPDATE_SHOT_CLOCK;
      msg.arg1 = tick;
      mUiHandler.sendMessage(msg);
   }

   public interface StateListener
   {
      public void onStateChange(final IGameState newState);
   }

   public interface ScoreListener
   {
      public void onScoreUpdate(final int myShips, final int enemyShips);
   }

}
