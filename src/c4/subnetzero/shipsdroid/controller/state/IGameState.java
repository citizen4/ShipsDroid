package c4.subnetzero.shipsdroid.controller.state;

public interface IGameState
{
   public void newGame();
   public void pauseGame();
   public void resumeGame();
   public void abortGame();

   public void finishGame();
}
