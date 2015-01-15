package c4.subnetzero.shipsdroid.model;

public class EnemyFleetModel extends AbstractFleetModel
{
   public EnemyFleetModel(final ModelUpdateListener updateListener)
   {
      super(updateListener);
      listener.onTotalUpdate(this);
   }

   public void update(final int i, final int j, final int resultFlag, final Ship ship)
   {
      if (ship != null) {
         shipsDestroyed++;
         ships[ship.getNumber() - 1] = ship;
         for (int m = 0, ix = ship.getStartI(), jy = ship.getStartJ(); m < ship.getSize(); m++) {
            seaGrid[ix][jy] = ship.getNumber();
            ix += (ship.getDir() == 0) ? 1 : 0;
            jy += (ship.getDir() != 0) ? 1 : 0;
         }
         listener.onTotalUpdate(this);
      } else {
         seaGrid[i + 1][j + 1] = resultFlag;
         listener.onPartialUpdate(this, i, j, resultFlag);
      }
   }
}
