package c4.subnetzero.shipsdroid.view;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;
import c4.subnetzero.shipsdroid.model.AbstractFleetModel;


public class EnemyFleetView extends AbstractFleetView
{
   private static final String LOG_TAG = "EnemyFleetView";

   public EnemyFleetView(Context context, final ViewGroup boardView, final GridButtonHandler gridButtonHandler, int size)
   {
      super(context, boardView, gridButtonHandler, size);
   }

   /*
      Must be called from UI Thread!
    */

   @Override
   public void updatePartialViewOnUi(final AbstractFleetModel fleetModel, final int i, final int j)
   {
      int gridValue = fleetModel.getSeaGrid()[i + 1][j + 1];

      Button gridButton = gridButtons[i][j];

      // Just water
      if (gridValue == 0 || gridValue == AbstractFleetModel.MISS) {
         gridButton.setBackground(gridValue == 0 ? mDrawableMap.get("WATER") : mDrawableMap.get("MISS"));
         gridButton.setText(gridValue == 0 ? "" : "x");
         return;
      }

      if (gridValue == AbstractFleetModel.HIT) {
         gridButton.setBackground(mDrawableMap.get("HIT"));
         return;
      }

      if (gridValue > 0 && gridValue < AbstractFleetModel.NUMBER_OF_SHIPS + 1) {
         gridButton.setBackground(mDrawableMap.get("DESTROYED"));
         gridButton.setText(Constants.DEAD_SYMBOL);
      }

   }
}
