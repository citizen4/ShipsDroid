package c4.subnetzero.shipsdroid.controller;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import c4.subnetzero.shipsdroid.GameActivity;
import c4.subnetzero.shipsdroid.NetService;
import c4.subnetzero.shipsdroid.R;
import c4.subnetzero.shipsdroid.Utils;
import c4.subnetzero.shipsdroid.controller.state.IGameState;
import c4.subnetzero.shipsdroid.controller.state.Paused;
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
   private IGameState currentState = new PeerReady(this);
   private Context mContext;
   private Message mLastMessage;

   public GameEngine(final Context context, final NetService netService)
   {
      mContext = context;
      mNetService = netService;
      mNetService.setListener(this);

      if (mContext instanceof GameActivity) {
         mUiHandler = ((GameActivity) mContext).getUiHandler();
      } else {
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
      return currentState.toString();
   }

   public IGameState getCurrentStateInstance()
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

   public void newGame()
   {
      currentState.newGame();
   }

   public void pauseGame()
   {
      currentState.pauseGame();
   }

   public void resumeGame()
   {
      currentState.resumeGame();
   }

   public void abortGame()
   {
      currentState.abortGame();
   }

   public void shoot(final int i, final int j)
   {
      if (currentState.toString().equals("Playing")) {
         mShotClock.pause();
         Message bombMsg = new Message();
         bombMsg.TYPE = Message.GAME;
         bombMsg.SUB_TYPE = Message.SHOOT;
         bombMsg.PAYLOAD = new Object[]{i, j};
         mNetService.sendMessage(bombMsg);
      }
   }

   public void shutDown()
   {
      mShotClock.stop();
      mShotClock.shutdown();
   }

   // FIXME: This method obviously needs some refactoring ;)
   @Override
   public void onMessage(Message msg, final String peerId)
   {
      if (Utils.sIsDialogOpen) {
         Log.d(LOG_TAG, "Open Dialog: Message ignored!");
         mLastMessage = msg;
         return;
      }

      switch (currentState.toString()) {
         case "Paused":
            if (msg.TYPE == Message.GAME) {
               if (msg.SUB_TYPE == Message.RESUME) {
                  if (!msg.ACK_FLAG) {
                     msg.ACK_FLAG = true;
                     mNetService.sendMessage(msg);
                  }
                  if (myTurnFlag) {
                     mShotClock.resume();
                  }
                  setState(new Playing(this));
                  return;
               }

               if (msg.SUB_TYPE == Message.ABORT) {
                  setPlayerEnabled(true, false);
                  mShotClock.stop();
                  setState(new PeerReady(this));
                  Utils.showOkMsg(mContext, R.string.game_aborted_msg, null);
                  return;
               }
            }
            break;

         case "PeerReady":
         case "Playing":

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

               if (msg.SUB_TYPE == Message.PAUSE) {
                  if (!msg.ACK_FLAG) {
                     msg.ACK_FLAG = true;
                     mNetService.sendMessage(msg);
                     Utils.showOkMsg(mContext, R.string.paused_by_peer_msg, null);
                  } else {
                     Utils.showOkMsg(mContext, R.string.game_paused_msg, null);
                  }
                  mShotClock.pause();
                  setState(new Paused(this));
                  return;
               }

               if (msg.SUB_TYPE == Message.GAME_OVER) {
                  setPlayerEnabled(true, false);
                  mShotClock.stop();
                  setState(new PeerReady(this));
                  //setState(new Finished(this));
                  Utils.showOkMsg(mContext, R.string.game_lose_msg, null);
                  return;
               }

               if (msg.SUB_TYPE == Message.ABORT) {
                  setPlayerEnabled(true, false);
                  mShotClock.stop();
                  setState(new PeerReady(this));
                  Utils.showOkMsg(mContext, R.string.game_aborted_msg, null);
                  return;
               }

               if (msg.SUB_TYPE == Message.TIMEOUT) {
                  Log.d(LOG_TAG, "Timeout received");
                  setPlayerEnabled(true, false);
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
                        setScore(ownFleetModel.getShipsLeft(), 0);
                        currentState.finishGame();
                        Utils.showOkMsg(mContext, R.string.game_win_msg, null);
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

                     myTurnFlag = !(resultFlag == AbstractFleetModel.HIT || resultFlag == AbstractFleetModel.DESTROYED);
                  }

                  if (resultFlag == AbstractFleetModel.DESTROYED) {
                     setScore(ownFleetModel.getShipsLeft(), enemyFleetModel.getShipsLeft());
                  }

                  setPlayerEnabled(myTurnFlag, false);
               }
            }
            break;

         case "Finished":


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

      setPlayerEnabled(myTurnFlag, true);
   }

   private void setScore(final int myShips, final int enemyShips)
   {
      android.os.Message msg = android.os.Message.obtain();
      msg.what = GameActivity.UPDATE_SCORE_BOARD;
      msg.arg1 = myShips;
      msg.arg2 = enemyShips;
      mUiHandler.sendMessage(msg);
   }


   public void setPlayerEnabled(final boolean enable, final boolean newGame)
   {
      if (enable) {
         mShotClock.start();
      } else {
         mShotClock.stop();
      }
      ((EnemyFleetView) enemyFleetModelUpdateListener).setEnabled(enable, newGame);
   }


   @Override
   public void onTimeIsUp()
   {
      Log.d(LOG_TAG, "onTimeIsUp()");
      setPlayerEnabled(false, false);
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
