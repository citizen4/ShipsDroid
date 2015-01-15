package c4.subnetzero.shipsdroid.controller.state;


import android.util.Log;
import c4.subnetzero.shipsdroid.controller.GameEngine;


public class Started extends GameStateAdapter
{
   private static final String LOG_TAG = "Started_State";
   private GameEngine engine = null;

   public Started(final GameEngine engine)
   {
      this.engine = engine;
   }

   @Override
   public void startNetReceiver()
   {
      //engine.getNetController().startReceiverThread();
      engine.setState(new Disconnected(engine));
   }

   @Override
   public void connectPeer(String peerId)
   {
      Log.e(LOG_TAG, "Wrong state transition");
   }

   @Override
   public void disconnectPeer()
   {
      Log.e(LOG_TAG, "Wrong state transition");
   }

   @Override
   public void newGame()
   {
      Log.e(LOG_TAG, "Wrong state transition");
   }

   @Override
   public void abortGame()
   {
      Log.e(LOG_TAG, "Wrong state transition");
   }

   @Override
   public void stopNetReceiver()
   {
      engine.setState(new Stopped(engine));
   }


}
