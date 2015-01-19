package c4.subnetzero.shipsdroid.net;

import c4.subnetzero.shipsdroid.model.Ship;

public class Message
{
   //Message TYPEs:
   public static final int CTRL = 0;
   public static final int GAME = 1;
   //Message CTRL SUB_TYPEs
   public static final int KEEP_ALIVE = 0;
   public static final int PEER_IS_HIDDEN = 1;
   public static final int PEER_QUIT = 2;
   //Message GAME SUB_TYPEs
   public static final int NEW = 3;
   public static final int PAUSE = 4;
   public static final int RESUME = 5;
   public static final int GAME_OVER = 6;
   public static final int ABORT = 7;
   public static final int SHOOT = 8;
   public static final int TIMEOUT = 9;


   public boolean ACK_FLAG = false;
   public boolean RST_FLAG = false;
   public int TYPE = CTRL;
   public int SUB_TYPE = KEEP_ALIVE;
   public Object PAYLOAD = null;
   public Ship SHIP = null;
   public long SEQ = 0L;
}
