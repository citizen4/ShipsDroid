package c4.subnetzero.shipsdroid;


import android.app.Application;
import android.util.Log;

public class ShipsDroidApp extends Application
{
   private static final String LOG_TAG = "MainApp";


   @Override
   public void onCreate()
   {
      Log.d(LOG_TAG, "onCreate()");
      super.onCreate();
   }

   @Override
   public void onTerminate()
   {
      Log.d(LOG_TAG,"onTerminate()");
      super.onTerminate();
   }

}
