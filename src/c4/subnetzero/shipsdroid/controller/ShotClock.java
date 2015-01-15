package c4.subnetzero.shipsdroid.controller;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ShotClock
{
   private static final String LOG_TAG = "ShotClock";
   private static final int TIMEOUT = 9;
   private Thread shotClockThread = null;
   private volatile AtomicInteger timeout = new AtomicInteger(TIMEOUT);
   private volatile AtomicBoolean isStopped = new AtomicBoolean(false);
   private final Object waitLock = new Object();
   private Listener mListener = null;


   public ShotClock()
   {
   }

   public void setListener(final Listener listener)
   {
      mListener = listener;
   }


   public void shutdown()
   {
      if (shotClockThread != null && shotClockThread.isAlive()) {
         shotClockThread.interrupt();
      }
   }

   public void stop()
   {
      if (mListener != null) {
         mListener.onTick(TIMEOUT + 1);
      }
      isStopped.set(true);
   }

   public void reset()
   {
      timeout.set(TIMEOUT);
      isStopped.set(false);

      if (mListener != null) {
         mListener.onTick(TIMEOUT + 1);
      }

      if (shotClockThread == null) {
         startThread();
      }

      synchronized (waitLock) {
         waitLock.notify();
      }
   }

   private void startThread()
   {
      shotClockThread = new Thread(new Runnable()
      {
         @Override
         public void run()
         {
            timeout.set(TIMEOUT);

            while (!Thread.currentThread().isInterrupted()) {

               try {
                  Thread.sleep(1000);

                  if (isStopped.get()) {
                     continue;
                  }

                  if (mListener != null) {
                     mListener.onTick(timeout.intValue());
                  }

                  if (timeout.getAndDecrement() == 0) {

                     if (mListener != null) {
                        mListener.onTimeIsUp();
                     }
                     synchronized (waitLock) {
                        waitLock.wait();
                     }
                  }

               } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
               }
            }
         }
      }, "ShotClockThread");

      shotClockThread.start();
   }


   public interface Listener
   {
      public void onTick(final int tick);
      public void onTimeIsUp();
   }
}
