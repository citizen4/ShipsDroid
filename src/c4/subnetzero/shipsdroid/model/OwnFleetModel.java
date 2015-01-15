package c4.subnetzero.shipsdroid.model;


import android.util.Log;

public class OwnFleetModel extends AbstractFleetModel
{
   private static final String LOG_TAG = "OwnFleetModel";

   public OwnFleetModel(final ModelUpdateListener updateListener)
   {
      super(updateListener);
      placeNewFleed();
   }

   // Update the model based on shoot at i,j
   public Object[] update(final int i, final int j)
   {
      int gridValue = seaGrid[i + 1][j + 1];

      Log.d(LOG_TAG, "gridValue:" + gridValue);

      // You hit that before
      if (gridValue < 0 || gridValue == AbstractFleetModel.MISS) {
         return new Object[]{AGAIN, null};
      }

      if (gridValue > 0 && gridValue < NUMBER_OF_SHIPS + 1) {
         Ship ship = ships[gridValue - 1];

         // No need to wast bombs ;)
         if (ship.isDestroyed()) {
            return new Object[]{AGAIN, null};
         }

         //boom beng:
         ship.hit();

         if (ship.isDestroyed()) {
            shipsDestroyed++;
            for (int m = 0, ix = ship.getStartI(), jy = ship.getStartJ(); m < ship.getSize(); m++) {
               seaGrid[ix][jy] = Math.abs(gridValue);
               ix += (ship.getDir() == 0) ? 1 : 0;
               jy += (ship.getDir() != 0) ? 1 : 0;
            }
            listener.onTotalUpdate(this);
            return new Object[]{DESTROYED, ship};
         } else {
            seaGrid[i + 1][j + 1] = -gridValue;
            listener.onPartialUpdate(this, i, j, AbstractFleetModel.HIT);
            return new Object[]{HIT, null};
         }
      }

      seaGrid[i + 1][j + 1] = AbstractFleetModel.MISS;
      listener.onPartialUpdate(this, i, j, AbstractFleetModel.MISS);
      return new Object[]{MISS, null};
   }

   private void placeNewFleed()
   {
      while (createFleet() < NUMBER_OF_SHIPS) ;
      listener.onTotalUpdate(this);
   }

   private int createFleet()
   {
      int i = 0, k = 0;
      int[] shipTypes = {5, 2, 3, 4, 2, 4, 2, 3, 2, 3};

      reset();

      while (i < NUMBER_OF_SHIPS && k++ < 10000) {
         int si = 1 + (int) (DIM * Math.random());
         int sj = 1 + (int) (DIM * Math.random());

         if (checkAndPlaceShip(i + 1, si, sj, shipTypes[i], si % 2)) {
            ships[i] = new Ship(i + 1, si, sj, shipTypes[i], si % 2);
            i++;
         }
      }

      return i;
   }

   // I've no idea what I'm doing here ;)
   private boolean checkAndPlaceShip(int n, int si, int sj, int size, int dir)
   {
      int ci1 = si - 1;
      int cj1 = sj - 1;
      int ci2 = (dir == 0) ? si + size : si + 1;
      int cj2 = (dir == 1) ? sj + size : sj + 1;

      if (ci2 > DIM + 1 || cj2 > DIM + 1) {
         return false;
      }

      for (int j = cj1; j < cj2 + 1; j++) {
         for (int i = ci1; i < ci2 + 1; i++) {
            if (seaGrid[i][j] != 0)
               return false;
         }
      }

      for (int k = 0, i = si, j = sj; k < size; k++) {
         seaGrid[i][j] = n;
         i += (dir == 0) ? 1 : 0;
         j += (dir != 0) ? 1 : 0;
      }

      return true;
   }

}
