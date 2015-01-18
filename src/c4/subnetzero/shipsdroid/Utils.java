package c4.subnetzero.shipsdroid;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class Utils
{
   public static volatile boolean sIsDialogOpen = false;
   private static final String LOG_TAG = "Utils";

   private Utils()
   {
      throw new IllegalStateException();
   }

   public static void showOkMsg(final Context context, final int resourceId, final DialogInterface.OnClickListener action)
   {
      Utils.showOkMsg(context, context.getString(resourceId), action);
   }

   public static void showOkMsg(final Context context, final String okMsg, final DialogInterface.OnClickListener action)
   {
      if (Utils.sIsDialogOpen) {
         return;
      }

      ((Activity) context).runOnUiThread(new Runnable()
      {
         @Override
         public void run()
         {
            Utils.sIsDialogOpen = true;
            AlertDialog dialog = new AlertDialog.Builder(context).create();
            dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Ok", new DialogInterface.OnClickListener()
            {
               @Override
               public void onClick(DialogInterface dialog, int which)
               {
                  Utils.sIsDialogOpen = false;
               }
            });
            dialog.setTitle("ShipsDroid");
            dialog.setMessage(okMsg);
            dialog.show();
         }
      });
   }

   /*
   public static void showCancelMsg(final Context context, final String infoMsg)
   {
      AlertDialog dialog = new AlertDialog.Builder(context).create();
      dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Ok", (DialogInterface.OnClickListener) null);
      dialog.setTitle("ShipsDroid");
   }*/

   public static Inet4Address getLocalIpAddress()
   {

      try {
         for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
            NetworkInterface networkInterface = en.nextElement();
            for (Enumeration<InetAddress> enumIpAddr = networkInterface.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
               InetAddress inetAddress = enumIpAddr.nextElement();
               if (!inetAddress.isLoopbackAddress()) {
                  if (inetAddress instanceof Inet4Address) {
                     return (Inet4Address)inetAddress;
                  }
                  //return inetAddress.getHostAddress().toString();
               }
            }
         }
      } catch (Exception e) {
         Log.e(LOG_TAG, "getLocalIpAddress()",e);
      }

      Inet4Address addr = null;
      try { addr = (Inet4Address)Inet4Address.getByName("1.1.1.1"); } catch (Exception e) {}
      return addr;
   }


   public static void launchWifiSettings(final Activity activity)
   {
      try {
         Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
         activity.startActivity(intent);
      } catch (ActivityNotFoundException e) {
         Log.e(LOG_TAG, "Unable to launch wifi settings activity", e);
         activity.runOnUiThread(new Runnable()
         {
            @Override
            public void run()
            {
               Toast.makeText(activity,"Please enable Wifi Direct", Toast.LENGTH_SHORT).show();
            }
         });
      }
   }
}
