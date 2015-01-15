package c4.subnetzero.shipsdroid.controller.state;


import android.util.Log;
import c4.subnetzero.shipsdroid.Utils;
import c4.subnetzero.shipsdroid.controller.GameEngine;
import c4.subnetzero.shipsdroid.net.Message;


public class Disconnected extends GameStateAdapter
{
   private static final String LOG_TAG = "Disconnected_State";
   private GameEngine gameEngine;

   public Disconnected(final GameEngine engine)
   {
      gameEngine = engine;
   }

   @Override
   public void startNetReceiver()
   {
      Log.w(LOG_TAG, "Net Receiver already up and running");
   }

   @Override
   public void connectPeer(String peerId)
   {
      Message connectMsg = new Message();
      connectMsg.SUB_TYPE = Message.CONNECT;
      gameEngine.getNetService().sendMessage(connectMsg);
      gameEngine.setState(new Connecting(gameEngine));

      /*
         if (Dialogs.showCancelMsg("Connecting...") == 0) {
            //connection attempt aborted by user
            gameEngine.setState(new Disconnected(gameEngine));
         }
      */
   }

   @Override
   public void disconnectPeer()
   {
      Utils.showOkMsg(gameEngine.getContext(), "No Player connected!");
   }

   @Override
   public void newGame()
   {
      Utils.showOkMsg(gameEngine.getContext(), "No Player connected!");
   }

   @Override
   public void abortGame()
   {
      Utils.showOkMsg(gameEngine.getContext(), "No Game running!");
   }

   @Override
   public void stopNetReceiver()
   {
      //gameEngine.getNetController().stopReceiverThread();
      gameEngine.setState(new Stopped(gameEngine));
   }

}
