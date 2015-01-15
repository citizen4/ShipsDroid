package c4.subnetzero.shipsdroid.model;

public class Ship
{
   private boolean destroyed = false;
   private int n;      //position in fleed
   private int startI; //start position i of ship in grid
   private int startJ; //start position j of ship in grid
   private int dir;    //ship heading (horizontal or vertical)
   private int size;   //size of ship
   private int hits;   //hits til destruction


   public Ship(int n, int si, int sj, int size, int dir)
   {
      this.n = n;
      this.startI = si;
      this.startJ = sj;
      this.dir = dir;
      this.size = size;
      this.hits = size;
   }

   public boolean isDestroyed()
   {
      return destroyed;
   }

   public void hit()
   {
      if (!destroyed) {
         hits--;
      }

      destroyed = hits == 0;
   }

   public int getSize()
   {
      return size;
   }

   public int getDir()
   {
      return dir;
   }

   public int getStartI()
   {
      return startI;
   }

   public int getStartJ()
   {
      return startJ;
   }

   public int getNumber()
   {
      return n;
   }

}
