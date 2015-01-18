package c4.subnetzero.shipsdroid.view;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import c4.subnetzero.shipsdroid.R;
import c4.subnetzero.shipsdroid.controller.GameEngine;
import c4.subnetzero.shipsdroid.model.SeaArea;


public class GridButtonHandler implements View.OnClickListener
{
   private static final String LOG_TAG = "GridButtonHandler";
   private GameEngine mGameEngine;
   private Animation mBtnAnim;
   private boolean isEnabled = true;

   public GridButtonHandler(final Context context)
   {
      mBtnAnim = AnimationUtils.loadAnimation(context, R.anim.grow);
   }

   @Override
   public void onClick(View view)
   {
      if (!isEnabled) {
         return;
      }

      int buttonIndex = view.getId();

      int i = buttonIndex % SeaArea.DIM;
      int j = buttonIndex / SeaArea.DIM;

      if (mGameEngine != null && mGameEngine.getStateName().equals("Playing")) {
         view.startAnimation(mBtnAnim);
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
