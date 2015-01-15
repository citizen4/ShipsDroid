package c4.subnetzero.shipsdroid.view;

import android.content.Context;
import android.view.ViewGroup;
import c4.subnetzero.shipsdroid.model.AbstractFleetModel;
import c4.subnetzero.shipsdroid.model.Ship;


public class OwnFleetView extends AbstractFleetView
{
   public OwnFleetView(Context context, ViewGroup boardView, int size)
   {
      super(context, boardView, null, size);
   }


   /*
      Must be called from UI Thread!
    */

   @Override
   public void updatePartialViewOnUi(final AbstractFleetModel fleetModel, final int i, final int j)
   {
      int gridValue = fleetModel.getSeaGrid()[i + 1][j + 1];

      // Just water
      if (gridValue == 0 || gridValue == AbstractFleetModel.MISS) {
         gridButtons[i][j].setBackground(gridValue == 0 ? mDrawableMap.get("WATER") : mDrawableMap.get("MISS"));
         gridButtons[i][j].setText(gridValue == 0 ? "" : "x");
         return;
      }

      // Ship is undamaged or destroyed
      if (gridValue > 0) {
         Ship ship = fleetModel.getShips()[gridValue - 1];
         gridButtons[i][j].setBackground(ship.isDestroyed() ? mDrawableMap.get("DESTROYED") : mDrawableMap.get("SHIP"));
         gridButtons[i][j].setText(ship.isDestroyed() ? Constants.DEAD_SYMBOL : "" + ship.getSize());
         return;
      }

      // Ship is partially damaged
      if (gridValue < 0) {
         gridButtons[i][j].setBackground(mDrawableMap.get("HIT"));
         return;
      }
   }
}
