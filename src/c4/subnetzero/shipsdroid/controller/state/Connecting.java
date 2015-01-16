package c4.subnetzero.shipsdroid.controller.state;

import c4.subnetzero.shipsdroid.R;
import c4.subnetzero.shipsdroid.Utils;
import c4.subnetzero.shipsdroid.controller.GameEngine;

public class Connecting extends GameStateAdapter
{

   private GameEngine engine;

   public Connecting(final GameEngine engine)
   {
      this.engine = engine;
   }

   @Override
   public void startNetReceiver()
   {

   }

   @Override
   public void connectPeer(String peerId)
   {
      Utils.showOkMsg(engine.getContext(), R.string.connection_attempt_msg);
   }

   @Override
   public void disconnectPeer()
   {
      engine.setState(new Disconnected(engine));
      Utils.showOkMsg(engine.getContext(), R.string.connection_attempt_aborted_msg);
   }

   @Override
   public void newGame()
   {
      Utils.showOkMsg(engine.getContext(), R.string.no_player_connected_msg);
   }

   @Override
   public void abortGame()
   {
      Utils.showOkMsg(engine.getContext(), R.string.no_game_running_msg);
   }

   @Override
   public void stopNetReceiver()
   {
      //engine.getNetController().stopReceiverThread();
      engine.setState(new Stopped(engine));
   }
}
