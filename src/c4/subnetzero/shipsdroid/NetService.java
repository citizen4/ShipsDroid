package c4.subnetzero.shipsdroid;


import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

//import c4.subnetzero.shipsdroid.net.Constants;
import c4.subnetzero.shipsdroid.net.Message;
import c4.subnetzero.shipsdroid.net.NetController;
import c4.subnetzero.shipsdroid.net.WifiP2pHost;

import java.net.InetAddress;


public class NetService extends Service implements NetController.Listener, WifiP2pHost.Listener
{
   private static final String LOG_TAG = "NetService";
   private LocalBinder mLocalBinder = new LocalBinder();
   private boolean mRestartAtDisconnect = false;
   private volatile boolean mConnected = false;
   private NetController mNetController;
   private WifiP2pHost mWifiP2pHost;
   private Listener mListener;
   private String mPeerId = "0.0.0.0:0";

   @Override
   public void onCreate()
   {
      super.onCreate();
      Log.d(LOG_TAG, "onCreate()");
      mNetController = new NetController(this);
      mWifiP2pHost = new WifiP2pHost(this, this);
      mWifiP2pHost.setServiceId("ShipsDroid:" + mNetController.getPort());
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
      stop();
      super.onDestroy();
   }


   public void start()
   {
      mWifiP2pHost.start();
      mNetController.startReceiverThread();
   }

   public void stop()
   {
      mListener = null;
      mNetController.stopReceiverThread();
      mWifiP2pHost.stop();
   }

   public void setListener(Listener listener)
   {
      if (listener != null && mListener != null) {
         throw new IllegalStateException("Listener must be null before set!");
      } else {
         mListener = listener;
      }
   }

   public void sendServerHello(final String groupOwnerId)
   {
      Log.d(LOG_TAG, "sendServerHell() -> " + groupOwnerId);
      sendMessage("ServerHello", groupOwnerId);
   }

   public void sendMessage(final String message, final String peerId)
   {
      if (mConnected) {
         mNetController.sendMessage(message, (peerId != null) ? peerId : mPeerId);
      }
   }

   public void sendMessage(final Message message)
   {
      if (mConnected) {
         mNetController.sendMessage(message, mPeerId);
      }
   }

   public void connect()
   {
      mWifiP2pHost.connect();
   }

   public void disconnect()
   {
      mWifiP2pHost.disconnect();
   }

   /**
    * ******************************************
    * Implementation of NetController Callbacks *
    * *******************************************
    */

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
         mRestartAtDisconnect = true;
         mPeerId = peerId;
         mNetController.sendMessage("ClientHello", mPeerId);
         //try {
         //   mNetController.startReachableThread(InetAddress.getByName(peerId.split(":")[0]));
         //} catch (UnknownHostException e) {}
         if (mListener != null) {
            mListener.onPeerReady();
         }
      }

      if (newMsg.equals("ClientHello")) {
         mRestartAtDisconnect = true;
         mPeerId = peerId;
         //try {
         //   mNetController.startReachableThread(InetAddress.getByName(peerId.split(":")[0]));
         //} catch (UnknownHostException e) {}
         if (mListener != null) {
            mListener.onPeerReady();
         }
      }
   }

   @Override
   public void onReachabilityChanged(final boolean reachable, final String peerId)
   {
      Log.d(LOG_TAG, "Reachablility has changed to: " + reachable);
      if (mListener != null) {
         mListener.onReachabilityChanged(reachable);
      }
   }

   @Override
   public void onError(String errMsg)
   {
   }

   /**
    * *****************************************
    * Implementation of WifiP2pHost Callbacks  *
    * ******************************************
    */

   @Override
   public void onStartDiscovery()
   {
      if (mListener != null) {
         mListener.onStartDiscovery();
      }
   }

   @Override
   public void onReadyToConnect()
   {
      if (mListener != null) {
         mListener.onReadyToConnect();
      }

      connect();
   }

   @Override
   public void onConnected(final InetAddress groupOwnerIp, final int groupOwnerPort, final boolean isGroupOwner)
   {
      mConnected = true;

      if (mListener != null) {
         mListener.onConnected(groupOwnerIp, groupOwnerPort, isGroupOwner);
      }

      if (!isGroupOwner) {
         sendServerHello(groupOwnerIp.getHostAddress() + ":" + groupOwnerPort);
      }
   }

   @Override
   public void onDisconnected()
   {
      mConnected = false;

      if (mRestartAtDisconnect) {
         mRestartAtDisconnect = false;
         mWifiP2pHost.stop();
         mWifiP2pHost = null;
         new Thread(new Runnable()
         {
            @Override
            public void run()
            {
               try {
                  Thread.sleep(3333);
               } catch (InterruptedException e) {
                  e.printStackTrace();
               }
               Log.d(LOG_TAG, "********************* RESTART");
               mWifiP2pHost = new WifiP2pHost(NetService.this, NetService.this);
               mWifiP2pHost.setServiceId("ShipsDroid:" + mNetController.getPort());
               mWifiP2pHost.start();
            }
         }).start();
      }

      if (mListener != null) {
         mListener.onDisconnected();
      }
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

      void onReachabilityChanged(final boolean reachable);

      void onStartDiscovery();

      void onReadyToConnect();

      void onConnected(InetAddress serverIp, int serverPort, boolean isGroupOwner);

      void onDisconnected();
   }

}
