package c4.subnetzero.shipsdroid.controller.state;


import c4.subnetzero.shipsdroid.R;
import c4.subnetzero.shipsdroid.Utils;
import c4.subnetzero.shipsdroid.controller.GameEngine;
import c4.subnetzero.shipsdroid.net.Message;

public class Paused extends GameStateAdapter
{

   private GameEngine mGameEngine = null;

   public Paused(final GameEngine engine)
   {
      mGameEngine = engine;
   }

   @Override
   public void newGame()
   {
      Utils.showOkMsg(mGameEngine.getContext(), R.string.abort_game_first_msg, null);
   }

   @Override
   public void pauseGame()
   {
      Utils.showOkMsg(mGameEngine.getContext(), R.string.game_already_paused, null);
   }

   @Override
   public void resumeGame()
   {
      Message abortGameMsg = new Message();
      abortGameMsg.TYPE = Message.GAME;
      abortGameMsg.SUB_TYPE = Message.RESUME;
      mGameEngine.getNetService().sendMessage(abortGameMsg);
   }

   @Override
   public void abortGame()
   {
      Message abortGameMsg = new Message();
      abortGameMsg.TYPE = Message.GAME;
      abortGameMsg.SUB_TYPE = Message.ABORT;
      mGameEngine.getNetService().sendMessage(abortGameMsg);
   }
}
