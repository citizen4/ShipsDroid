package c4.subnetzero.shipsdroid.controller;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ShotClock
{
   private static final String LOG_TAG = "ShotClock";
   private static final int TIMEOUT = 10;
   private Thread shotClockThread = null;
   private volatile AtomicInteger timeout = new AtomicInteger(TIMEOUT);
   private volatile AtomicBoolean isStopped = new AtomicBoolean(false);
   private volatile boolean mQuitFlag;
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

   public void start()
   {
      timeout.set(TIMEOUT);
      isStopped.set(false);

      synchronized (waitLock) {
         waitLock.notify();
      }

      if (shotClockThread == null) {
         startThread();
      }
   }

   public void stop()
   {
      isStopped.set(true);

      if (mListener != null) {
         mListener.onTick(0);
      }
   }

   public void pause()
   {
      isStopped.set(true);
   }

   public void resume()
   {
      isStopped.set(false);
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
                  synchronized (waitLock) {
                     waitLock.wait(1000);
                  }
                  if (!isStopped.get()) {
                     if (mListener != null) {
                        mListener.onTick(timeout.intValue());
                     }
                     if (timeout.getAndDecrement() <= 0) {
                        if (mListener != null) {
                           mListener.onTimeIsUp();
                        }
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
