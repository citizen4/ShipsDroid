package c4.subnetzero.shipsdroid.controller.state;


import c4.subnetzero.shipsdroid.R;
import c4.subnetzero.shipsdroid.Utils;
import c4.subnetzero.shipsdroid.controller.GameEngine;
import c4.subnetzero.shipsdroid.net.Message;

public class PeerReady extends GameStateAdapter
{
   private GameEngine mGameEngine = null;

   public PeerReady(final GameEngine engine)
   {
      mGameEngine = engine;
   }

   @Override
   public void newGame()
   {
      Message newGameMsg = new Message();
      newGameMsg.TYPE = Message.GAME;
      newGameMsg.SUB_TYPE = Message.NEW;
      mGameEngine.getNetService().sendMessage(newGameMsg);
   }

   @Override
   public void pauseGame()
   {
      Utils.showOkMsg(mGameEngine.getContext(), R.string.no_game_running_msg, null);
   }

   @Override
   public void resumeGame()
   {
      Utils.showOkMsg(mGameEngine.getContext(), R.string.no_game_running_msg, null);
   }

   @Override
   public void abortGame()
   {
      Utils.showOkMsg(mGameEngine.getContext(), R.string.no_game_running_msg, null);
   }

}
