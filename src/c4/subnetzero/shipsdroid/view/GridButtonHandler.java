package c4.subnetzero.shipsdroid.view;

import android.view.View;
import c4.subnetzero.shipsdroid.controller.GameEngine;
import c4.subnetzero.shipsdroid.model.SeaArea;


public class GridButtonHandler implements View.OnClickListener
{
   private static final String LOG_TAG = "GridButtonHandler";
   private GameEngine mGameEngine;
   private boolean isEnabled = true;

   @Override
   public void onClick(View view)
   {
      if (!isEnabled) {
         return;
      }

      int buttonIndex = view.getId();

      int i = buttonIndex % SeaArea.DIM;
      int j = buttonIndex / SeaArea.DIM;

      if (mGameEngine != null) {
         mGameEngine.shoot(i, j);
      }
   }

   public void setGameEngine(final GameEngine gameEngine)
   {
      mGameEngine = gameEngine;
   }

   public void setEnabled(final boolean enabled)
   {
      isEnabled = enabled;
   }

}
