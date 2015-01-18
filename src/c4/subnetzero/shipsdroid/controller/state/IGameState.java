package c4.subnetzero.shipsdroid.controller.state;

public interface IGameState
{
   public void newGame();

   public void pauseGame();

   public void resumeGame();
   public void abortGame();

   public void winGame();

   public void loseGame();

   //FIXME: don't belong here
   //public void shoot(final int i, final int j);
}
