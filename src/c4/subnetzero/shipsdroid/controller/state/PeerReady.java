package c4.subnetzero.shipsdroid.controller.state;


import c4.subnetzero.shipsdroid.Utils;
import c4.subnetzero.shipsdroid.controller.GameEngine;
import c4.subnetzero.shipsdroid.net.Message;

public class PeerReady extends GameStateAdapter
{

   private GameEngine engine = null;

   public PeerReady(final GameEngine engine)
   {
      this.engine = engine;
   }

   @Override
   public void startNetReceiver()
   {
      // should never happen
   }

   @Override
   public void connectPeer(String peerId)
   {
      Utils.showOkMsg(engine.getContext(), "Disconnect current Player first!");
   }

   @Override
   public void disconnectPeer()
   {
      Message disconnectMsg = new Message();
      disconnectMsg.SUB_TYPE = Message.DISCONNECT;
      engine.getNetService().sendMessage(disconnectMsg);
      engine.setState(new Disconnected(engine));
   }

   @Override
   public void newGame()
   {
      Message newGameMsg = new Message();
      newGameMsg.TYPE = Message.GAME;
      newGameMsg.SUB_TYPE = Message.NEW;
      engine.getNetService().sendMessage(newGameMsg);
   }

   @Override
   public void abortGame()
   {
      Utils.showOkMsg(engine.getContext(), "No running game to abort!");
   }

   @Override
   public void stopNetReceiver()
   {
      disconnectPeer();
      engine.setState(new Stopped(engine));
   }
}
