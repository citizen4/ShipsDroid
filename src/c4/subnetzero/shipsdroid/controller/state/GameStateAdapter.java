package c4.subnetzero.shipsdroid.controller.state;

public abstract class GameStateAdapter implements IGameState
{
   private static final String LOG_TAG = "GameStateAdapter";
   private static final boolean LOG_STATE_TRANSITION = false;

   @Override
   public void newGame()
   {
   }

   @Override
   public void pauseGame()
   {
   }

   @Override
   public void resumeGame()
   {
   }

   @Override
   public void abortGame()
   {
   }

   @Override
   public void finishGame()
   {
   }

   @Override
   public String toString()
   {
      return this.getClass().getSimpleName();
   }

   @Override
   public boolean equals(Object o)
   {
      if (o instanceof IGameState) {
         return this.toString().equals(o.toString());
      }

      return false;
   }
}
