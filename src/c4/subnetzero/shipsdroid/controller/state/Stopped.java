package c4.subnetzero.shipsdroid.controller.state;


import c4.subnetzero.shipsdroid.controller.GameEngine;

public class Stopped extends GameStateAdapter
{
   private GameEngine mGameEngine;

   public Stopped(final GameEngine engine)
   {
      mGameEngine = engine;
   }

   @Override
   public void startNetReceiver()
   {

   }

   @Override
   public void connectPeer(String peerId)
   {

   }

   @Override
   public void disconnectPeer()
   {

   }

   @Override
   public void newGame()
   {

   }

   @Override
   public void abortGame()
   {

   }

   @Override
   public void stopNetReceiver()
   {

   }
}
