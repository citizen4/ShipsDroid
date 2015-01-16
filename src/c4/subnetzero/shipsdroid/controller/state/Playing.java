package c4.subnetzero.shipsdroid.controller.state;


import c4.subnetzero.shipsdroid.R;
import c4.subnetzero.shipsdroid.Utils;
import c4.subnetzero.shipsdroid.controller.GameEngine;
import c4.subnetzero.shipsdroid.net.Message;

public class Playing extends GameStateAdapter
{
   private GameEngine engine = null;

   public Playing(final GameEngine engine)
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
      Utils.showOkMsg(engine.getContext(), R.string.player_already_connected_msg);
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
      Utils.showOkMsg(engine.getContext(), R.string.abort_game_first_msg);
   }

   @Override
   public void abortGame()
   {
      Message abortGameMsg = new Message();
      abortGameMsg.TYPE = Message.GAME;
      abortGameMsg.SUB_TYPE = Message.ABORT;
      engine.getNetService().sendMessage(abortGameMsg);
      engine.setPlayerEnabled(true);
      engine.getShotClock().stop();
      engine.setState(new PeerReady(engine));
   }

   @Override
   public void shoot(final int i, final int j)
   {
      Message bombMsg = new Message();
      bombMsg.TYPE = Message.GAME;
      bombMsg.SUB_TYPE = Message.SHOOT;
      bombMsg.PAYLOAD = new Object[]{i, j};
      engine.getNetService().sendMessage(bombMsg);
      engine.getShotClock().reset();
   }


   @Override
   public void stopNetReceiver()
   {
      disconnectPeer();
      engine.setState(new Stopped(engine));
   }
}
