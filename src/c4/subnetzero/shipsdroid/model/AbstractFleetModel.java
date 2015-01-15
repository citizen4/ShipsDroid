package c4.subnetzero.shipsdroid.model;

public abstract class AbstractFleetModel
{
   public static final int NUMBER_OF_SHIPS = 10;
   public static final int MISS = 400;
   public static final int HIT = 100;
   public static final int AGAIN = 200;
   public static final int DESTROYED = 500;

   protected static final int DIM = SeaArea.DIM;
   protected int[][] seaGrid = new int[(DIM + 2)][(DIM + 2)];
   protected Ship[] ships = new Ship[NUMBER_OF_SHIPS];
   protected int shipsDestroyed = 0;
   protected ModelUpdateListener listener = null;

   public AbstractFleetModel(final ModelUpdateListener listener)
   {
      this.listener = listener;
   }

   public int getShipsLeft()
   {
      return NUMBER_OF_SHIPS - shipsDestroyed;
   }

   public boolean isFleetDestroyed()
   {
      return shipsDestroyed == NUMBER_OF_SHIPS;
   }

   public int[][] getSeaGrid()
   {
      return seaGrid;
   }

   public Ship[] getShips()
   {
      return ships;
   }

   public void reset()
   {
      seaGrid = new int[(DIM + 2)][(DIM + 2)];
      ships = new Ship[NUMBER_OF_SHIPS];
   }

   public interface ModelUpdateListener
   {
      public void onTotalUpdate(final AbstractFleetModel model);

      public void onPartialUpdate(final AbstractFleetModel model, final int i, final int j, final int flag);
   }

}
