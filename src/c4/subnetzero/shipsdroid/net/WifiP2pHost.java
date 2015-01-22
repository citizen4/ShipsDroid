package c4.subnetzero.shipsdroid.net;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.*;
import android.os.Message;
import android.util.Log;

import java.net.InetAddress;

public class WifiP2pHost implements Handler.Callback
{
   private static final String LOG_TAG = "WifiP2pHost";

   private static final int RESTART_DISCOVERY = 0;
   private static final int QUIT = 1;

   private Context mContext;
   private HandlerThread mHandlerThread;
   private Handler mHandler;
   private Listener mListener;
   private WifiP2pManager mWifiManager;
   private WifiP2pManager.Channel mChannel;
   private IntentFilter mWifiP2pFilter;
   private WifiP2pReceiver mWifiP2pReceiver;
   //private LocalServiceReceiver mLocalServiceReceiver;
   private WifiP2pDevice mRemoteDevice;
   private WifiP2pDevice mLocalDevice;
   //private WifiP2pGroup mGroup;
   private String mServiceId;
   private boolean mIsEnabled;
   private boolean mIsLocalServiceRegistered;
   private boolean mIsServiceRequestRegistered;
   private boolean mIsConnecting;
   private boolean mIsConnected;
   private int mServerPort;


   public WifiP2pHost(final Context context, final Listener listener)
   {
      mContext = context;
      mListener = listener;
      mIsEnabled = true;
      setup();
   }

   public void setServiceId(final String serviceId)
   {
      mServiceId = serviceId;
   }

   public void start()
   {
      mContext.registerReceiver(mWifiP2pReceiver, mWifiP2pFilter);
      mWifiManager.setDnsSdResponseListeners(mChannel, new ResponseListener(), null);

      if (mServiceId != null) {
         registerService(mServiceId, Constants.SERVICE_TYPE);
      } else {
         Log.e(LOG_TAG, "No Service Id!");
      }

      mHandler.sendEmptyMessageDelayed(RESTART_DISCOVERY, 66666);
   }


   public void stop()
   {
      mListener = null;
      mContext.unregisterReceiver(mWifiP2pReceiver);
      unregisterServices();

      if (!mIsConnected) {
         unregisterServiceRequest();
         stopDiscovery();
      }

      mHandler.removeMessages(0);
      mHandler.sendEmptyMessageDelayed(QUIT, 666);
   }

   /*
   public void quit()
   {
      mHandlerThread.quit();
   }*/

   public void connect()
   {
      connectDevice(mRemoteDevice);
   }

   public void disconnect()
   {
      if (mLocalDevice != null && mLocalDevice.status == WifiP2pDevice.CONNECTED) {
         mWifiManager.removeGroup(mChannel, new WifiP2pActionListener("removeGroup"));
      }
   }


   private void registerService(String serviceName, String serviceType)
   {
      if (mIsEnabled && !mIsLocalServiceRegistered) {
         Log.d(LOG_TAG, "Register service " + serviceName + " type " + serviceType);
         WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(serviceName, serviceType, null);
         mWifiManager.addLocalService(mChannel, serviceInfo, new WifiP2pActionListener("addLocalService"));
         startDiscovery();
         mIsLocalServiceRegistered = true;
      }
   }

   private void unregisterServices()
   {
      if (mIsEnabled && mIsLocalServiceRegistered) {
         mWifiManager.clearLocalServices(mChannel, new WifiP2pActionListener("clearLocalServices"));
         stopDiscovery();
         mIsLocalServiceRegistered = false;
      }
   }

   private void startDiscovery()
   {
      Log.i(LOG_TAG, "Start Wifi-P2P discovery...");

      if (!mIsServiceRequestRegistered) {
         WifiP2pDnsSdServiceRequest request = WifiP2pDnsSdServiceRequest.newInstance(Constants.SERVICE_TYPE);
         mWifiManager.addServiceRequest(mChannel, request, new WifiP2pActionListener("addServiceRequest"));
         mIsServiceRequestRegistered = true;
      }

      mWifiManager.discoverServices(mChannel, new WifiP2pActionListener("discoverService")
      {
         @Override
         public void onFailure(int reason)
         {
            super.onFailure(reason);
            //XXX
            resetDiscovery();
         }

         @Override
         public void onSuccess()
         {
            super.onSuccess();
            if (mListener != null) {
               mListener.onStartDiscovery();
            }
         }

      });
   }

   private void stopDiscovery()
   {
      Log.i(LOG_TAG, "Stop Wifi P2P discovery");
      mWifiManager.stopPeerDiscovery(mChannel, new WifiP2pActionListener("stopPeerDiscovery"));
   }

   private void unregisterServiceRequest()
   {
      mWifiManager.clearServiceRequests(mChannel, new WifiP2pActionListener("clearServiceRequest"));
      mIsServiceRequestRegistered = false;
   }

   private void connectDevice(WifiP2pDevice device)
   {
      Log.i(LOG_TAG, "Wifi device " + device.deviceName + " mac: " +
              device.deviceAddress + " status is " + devStateToStr(device.status));

      if (device.status == WifiP2pDevice.AVAILABLE || device.status == WifiP2pDevice.INVITED) {

         Log.i(LOG_TAG, "Trying to connect...");

         WifiP2pConfig config = new WifiP2pConfig();

         config.deviceAddress = device.deviceAddress;
         config.wps.setup = WpsInfo.PBC;
         //config.groupOwnerIntent = (mType == Type.AP) ? 15 : 1;

         if (!mIsConnecting) {
            mWifiManager.connect(mChannel, config, new WifiP2pActionListener("connect")
            {
               @Override
               public void onFailure(int reason)
               {
                  super.onFailure(reason);
                  mIsConnecting = false;
               }

               @Override
               public void onSuccess()
               {
                  super.onSuccess();
                  mIsConnecting = false;
               }
            });

            mIsConnecting = true;
         }
      }
   }


   private void resetDiscovery()
   {
      Log.i(LOG_TAG, "Reset service discovery...");

      // Initiate a stop on service discovery
      mWifiManager.stopPeerDiscovery(mChannel, new WifiP2pActionListener("stopPeerDiscovery")
      {
         @Override
         public void onSuccess()
         {
            // Initiate clearing of the all service requests
            super.onSuccess();
            mWifiManager.clearServiceRequests(mChannel, new WifiP2pActionListener("clearServiceRequests")
            {
               @Override
               public void onSuccess()
               {
                  super.onSuccess();
                  mIsServiceRequestRegistered = false;
                  startDiscovery();
               }
            });
         }
      });
   }

   public void setup()
   {
      mWifiManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
      mHandlerThread = new HandlerThread(LOG_TAG);
      mHandlerThread.start();
      mHandler = new Handler(mHandlerThread.getLooper(), this);

      mChannel = mWifiManager.initialize(mContext, mHandlerThread.getLooper(), null);

      mWifiP2pFilter = new IntentFilter();
      mWifiP2pFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
      mWifiP2pFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
      mWifiP2pFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

      mWifiP2pReceiver = new WifiP2pReceiver();
   }


   @Override
   public boolean handleMessage(Message msg)
   {
      switch (msg.what) {
         case RESTART_DISCOVERY:
            resetDiscovery();
            mHandler.sendEmptyMessageDelayed(RESTART_DISCOVERY, 66666);
            break;
         case QUIT:
            Log.d(LOG_TAG, "Shutting down WifiP2pHost-Thread");
            mHandlerThread.quitSafely();
            break;
      }

      return true;
   }

   private String devStateToStr(int status)
   {
      String statusStr = "Unknown";

      switch (status) {
         case WifiP2pDevice.AVAILABLE:
            statusStr = "AVAILABLE";
            break;
         case WifiP2pDevice.CONNECTED:
            statusStr = "CONNECTED";
            break;
         case WifiP2pDevice.INVITED:
            statusStr = "INVITED";
            break;
         case WifiP2pDevice.FAILED:
            statusStr = "FAILED";
            break;
         case WifiP2pDevice.UNAVAILABLE:
            statusStr = "UNAVAILABLE";
            break;
      }

      return statusStr;
   }


   private class ResponseListener implements WifiP2pManager.DnsSdServiceResponseListener
   {
      private static final String LOG_TAG = "ResponseListener";

      @Override
      public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice)
      {
         Log.i(LOG_TAG, "onDnsSdServiceAvailable()");
         Log.d(LOG_TAG, "instanceName: " + instanceName + " registrationType: " + registrationType);

         //XXX extract service port (NSD did not cut it)
         mServerPort = Integer.parseInt(instanceName.split(":")[1]);
         mRemoteDevice = srcDevice;

         mHandler.removeMessages(0);

         if (mListener != null) {
            mListener.onReadyToConnect();
         }
      }
   }

   private class WifiP2pReceiver extends BroadcastReceiver
   {
      private static final String LOG_TAG = "WifiP2pReceiver";

      @Override
      public void onReceive(Context context, Intent intent)
      {
         String action = intent.getAction();

         switch (action) {
            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
               onConnectionChanged(intent);
               break;
            case WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION:
               onDiscoveryChanged(intent);
               break;
            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
               onLocalDeviceChanged(intent);
               break;
         }
      }


      private void onConnectionChanged(Intent intent)
      {
         NetworkInfo netInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

         Log.i(LOG_TAG, "Wifi P2P connection changed " + netInfo);
         mIsConnected = netInfo.isConnected();

         Log.i(LOG_TAG, "Wifi P2P connection is connected ? " + mIsConnected);

         if (netInfo.getState() == NetworkInfo.State.CONNECTED) {

            mHandler.removeMessages(RESTART_DISCOVERY);

            mWifiManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener()
            {
               @Override
               public void onConnectionInfoAvailable(final WifiP2pInfo info)
               {
                  Log.i(LOG_TAG, "Wifi P2P connection is formed ? " + info.groupFormed);
                  mWifiManager.requestGroupInfo(mChannel, new WifiP2pManager.GroupInfoListener()
                  {
                     @Override
                     public void onGroupInfoAvailable(WifiP2pGroup group)
                     {
                        if (group != null) {
                           //WifiP2pHost.this.mGroup = group;
                           Log.i(LOG_TAG, "Wifi P2P connection group is  " + group.getNetworkName());
                           Log.i(LOG_TAG, "Group size " + group.getClientList().size());
                           if (mListener != null) {
                              mListener.onConnected(info.groupOwnerAddress, mServerPort, group.isGroupOwner());
                           }
                        }
                     }
                  });
               }
            });

         } else {
            //mGroup = null;
            Log.d(LOG_TAG, "Connection state: " + netInfo.getDetailedState());
            if (mListener != null && netInfo.getState() == NetworkInfo.State.DISCONNECTED) {
               mListener.onDisconnected();
               resetDiscovery();
            }
            //resetDiscovery();
         }
      }

      private void onDiscoveryChanged(Intent intent)
      {
         int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1);

         if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
            Log.i(LOG_TAG, "Wifi P2P discovery started");
         } else {
            Log.i(LOG_TAG, "Wifi P2P discovery stopped");
         }
      }

      private void onLocalDeviceChanged(Intent intent)
      {
         Log.i(LOG_TAG, "Local Wifi P2P device has changed");
         WifiP2pDevice localDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);

         mLocalDevice = localDevice;

         if (localDevice.status == WifiP2pDevice.CONNECTED) {
            Log.i(LOG_TAG, "Local device is connected");
         } else {
            Log.i(LOG_TAG, "Local device is not connected");
         }
      }

   }


   private class WifiP2pActionListener implements WifiP2pManager.ActionListener
   {
      private static final String LOG_TAG = "WifiP2pActionListener";
      private final String mOperation;

      public WifiP2pActionListener(String operation)
      {
         mOperation = operation;
      }

      @Override
      public void onFailure(int reason)
      {
         String errStr = "UNKNOWN";

         // All the shit that can happen:
         switch (reason) {
            case WifiP2pManager.P2P_UNSUPPORTED:
               errStr = "P2P_UNSUPPORTED";
               break;
            case WifiP2pManager.BUSY:
               errStr = "BUSY";
               break;
            case WifiP2pManager.ERROR:
               errStr = "INTERNAL ERROR";
               break;
            case WifiP2pManager.NO_SERVICE_REQUESTS:
               errStr = "NO_SERVICE_REQUESTS";
               break;
         }

         Log.e(LOG_TAG, "Error during Wifi P2P operation: \"" + mOperation + "\" reason: " + errStr);
      }


      @Override
      public void onSuccess()
      {
         Log.d(LOG_TAG, "Wifi P2P operation \"" + mOperation + "\" success");
      }
   }

   public interface Listener
   {
      void onStartDiscovery();

      void onReadyToConnect();

      void onConnected(InetAddress serverIp, int serverPort, boolean isGroupOwner);

      void onDisconnected();
   }

}
