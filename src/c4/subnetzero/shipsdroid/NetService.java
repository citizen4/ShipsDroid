package c4.subnetzero.shipsdroid;


import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import c4.subnetzero.shipsdroid.net.Constants;
import c4.subnetzero.shipsdroid.net.Message;
import c4.subnetzero.shipsdroid.net.NetController;


public class NetService extends Service
{
   private static final String LOG_TAG = "NetService";
   private LocalBinder mLocalBinder = new LocalBinder();
   private NetController mNetController;
   private Listener mListener;
   private String mPeerId = "0.0.0.0:0";

   @Override
   public void onCreate()
   {
      super.onCreate();
      Log.d(LOG_TAG, "onCreate()");
      setup();
   }

   @Override
   public IBinder onBind(Intent intent)
   {
      Log.d(LOG_TAG, "onBind()");
      return mLocalBinder;
   }

   @Override
   public void onDestroy()
   {
      Log.d(LOG_TAG, "onDestroy()");
      mNetController.stopReceiverThread();
      super.onDestroy();
   }

   public int getPort()
   {
      return mNetController.getPort();
   }

   public void start()
   {
      mNetController.startReceiverThread();
   }

   public void stop()
   {
      mNetController.stopReceiverThread();
   }

   public void setListener(Listener listener)
   {
      mListener = listener;
   }

   public void sendServerHello(final String groupOwnerId)
   {
      sendMessage("ServerHello", groupOwnerId);
   }

   public void sendMessage(final String message, final String peerId)
   {
      mNetController.sendMessage(message, (peerId != null) ? peerId : mPeerId);
   }

   public void sendMessage(final Message message)
   {
      Log.d(LOG_TAG, "peerId: " + mPeerId);
      mNetController.sendMessage(message, mPeerId);
   }

   private void setup()
   {
      mNetController = new NetController(new NetController.Listener()
      {
         @Override
         public void onMessage(Message newMsg, String peerId)
         {
            if (mListener != null) {
               mListener.onMessage(newMsg, peerId);
            }
         }

         @Override
         public void onMessage(String newMsg, String peerId)
         {
            if (newMsg.equals("ServerHello")) {
               mPeerId = peerId;
               mNetController.sendMessage("ClientHello", mPeerId);
               if (mListener != null) {
                  mListener.onPeerReady();
               }
            }

            if (newMsg.equals("ClientHello")) {
               mPeerId = peerId;
               if (mListener != null) {
                  mListener.onPeerReady();
               }
            }
         }

         @Override
         public void onError(String errMsg)
         {
         }
      });
   }


   public void broadcastStatus()
   {
      Intent intent = new Intent(Constants.LOCAL_SERVER_CHANGE_ACTION);
      intent.putExtra(Constants.EXTRA_LOCAL_SERVER_NAME, "ShipsDroid");
      intent.putExtra(Constants.EXTRA_LOCAL_SERVER_PORT, mNetController.getPort());
      sendBroadcast(intent);
   }

   public class LocalBinder extends Binder
   {
      public NetService getService()
      {
         return NetService.this;
      }
   }

   public interface Listener
   {
      void onPeerReady();

      void onMessage(Message newMsg, String peerId);
   }

}
