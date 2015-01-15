package c4.subnetzero.shipsdroid.controller.state;

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
      Utils.showOkMsg(engine.getContext(), "Connection in progress!");
   }

   @Override
   public void disconnectPeer()
   {
      engine.setState(new Disconnected(engine));
      Utils.showOkMsg(engine.getContext(), "Connection attempt aborted by user!");
   }

   @Override
   public void newGame()
   {
      Utils.showOkMsg(engine.getContext(), "No Player connected yet!");
   }

   @Override
   public void abortGame()
   {
      Utils.showOkMsg(engine.getContext(), "No Game running!");
   }

   @Override
   public void stopNetReceiver()
   {
      //engine.getNetController().stopReceiverThread();
      engine.setState(new Stopped(engine));
   }
}
