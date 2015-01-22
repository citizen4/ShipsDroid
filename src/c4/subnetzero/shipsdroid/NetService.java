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
import java.net.UnknownHostException;


public class NetService extends Service implements NetController.Listener, WifiP2pHost.Listener
{
   private static final String LOG_TAG = "NetService";
   private LocalBinder mLocalBinder = new LocalBinder();
   private volatile State mState = State.DISCONNECTED;
   private NetController mNetController;
   private WifiP2pHost mWifiP2pHost;
   private Listener mListener;
   private String mPeerId = "0.0.0.0:0";

   public enum State
   {
      DISCONNECTED,
      CONNECTED,
      REACHABLE,
      UNREACHABLE
   }

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
      mNetController.startReachableThread();
   }

   public void stop()
   {
      mListener = null;
      mNetController.pauseReachableThread(true);
      mNetController.stopReachableThread();
      mNetController.stopReceiverThread();
      mWifiP2pHost.stop();
   }

   public State getState()
   {
      return mState;
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
      Log.d(LOG_TAG, "sendServerHello() -> " + groupOwnerId);
      sendMessage("ServerHello", groupOwnerId);
   }

   public void sendMessage(final String message, final String peerId)
   {
      if (mState == State.CONNECTED) {
         mNetController.sendMessage(message, (peerId != null) ? peerId : mPeerId);
      }
   }

   public void sendMessage(final Message message)
   {
      if (mState == State.REACHABLE) {
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
         //mState = State.REACHABLE;
         mNetController.pauseReachableThread(false);
         mPeerId = peerId;
         mNetController.sendMessage("ClientHello", mPeerId);
         if (mListener != null) {
            mListener.onPeerReady();
         }
      }

      if (newMsg.equals("ClientHello")) {
         //mState = State.REACHABLE;
         mNetController.pauseReachableThread(false);
         mPeerId = peerId;
         if (mListener != null) {
            mListener.onPeerReady();
         }
      }


   }

   @Override
   public void onReachabilityChanged(final boolean reachable, final String peerId)
   {
      Log.d(LOG_TAG, "Reachability has changed to: " + reachable);
      mState = reachable ? State.REACHABLE : State.UNREACHABLE;
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
      connect();
   }

   @Override
   public void onConnected(final InetAddress groupOwnerIp, final int groupOwnerPort, final boolean isGroupOwner)
   {
      mState = State.CONNECTED;
      //mNetController.pauseReachableThread(false);

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
      mState = State.DISCONNECTED;
      mNetController.pauseReachableThread(true);

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

      void onConnected(InetAddress serverIp, int serverPort, boolean isGroupOwner);

      void onDisconnected();
   }

}
