package c4.subnetzero.shipsdroid.controller.state;


import c4.subnetzero.shipsdroid.R;
import c4.subnetzero.shipsdroid.Utils;
import c4.subnetzero.shipsdroid.controller.GameEngine;
import c4.subnetzero.shipsdroid.net.Message;

public class Playing extends GameStateAdapter
{
   private GameEngine mGameEngine = null;

   public Playing(final GameEngine engine)
   {
      mGameEngine = engine;
   }

   @Override
   public void newGame()
   {
      Utils.showOkMsg(mGameEngine.getContext(), R.string.abort_game_first_msg);
   }

   @Override
   public void pauseGame()
   {

   }

   @Override
   public void abortGame()
   {
      Message abortGameMsg = new Message();
      abortGameMsg.TYPE = Message.GAME;
      abortGameMsg.SUB_TYPE = Message.ABORT;
      mGameEngine.getNetService().sendMessage(abortGameMsg);
      mGameEngine.setPlayerEnabled(true, false);
      mGameEngine.getShotClock().stop();
      mGameEngine.setState(new PeerReady(mGameEngine));
   }
}
