package c4.subnetzero.shipsdroid;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import c4.subnetzero.shipsdroid.net.WifiP2pHost;

import java.net.InetAddress;

public class StartActivity extends Activity implements Handler.Callback, ServiceConnection
{
   private static final String LOG_TAG = "StartActivity";

   private static final int WIFI_DISABLED = 20;
   private static final int WIFI_ENABLED = 21;
   private static final int WIFI_P2P_START = 22;
   private static final int FINISH_ACTIVITY = 30;
   private static final int READY_TO_CONNECT = 1;
   private static final int L2_CONNECTED = 2;
   private static final int L2_CONNECTING = 3;
   private static final int L2_DISCONNECTED = 4;
   private static final int HANDSHAKE_SEND = 10;
   private static final int HANDSHAKE_RECEIVED = 11;
   private static final int L3_CONNECTED = 12;
   private static final int L2_DISCOVERY_STARTED = 7;

   private Handler mUiHandler;
   private NetService mNetService;
   private WifiP2pHost mWifiP2pHost;
   private WifiP2pReceiver mWifiP2pReceiver;
   private NetService.Listener mNetServiceListener;
   private volatile boolean mIsServiceConnected;
   private volatile boolean mIsWifiP2pEnabled;
   private boolean mIsConnected;

   private InetAddress mGoIp;
   private int mGoPort;
   private String mGroupOwnerId = "0.0.0.0:0";
   private String mPeerId = "0.0.0.0:0";
   private TextView mStateLabel;
   private Button mConDisconBtn;

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.start);
      mUiHandler = new Handler(this);
      setup();
   }

   @Override
   protected void onResume()
   {
      Log.d(LOG_TAG, "onResume()");
      super.onResume();

      registerReceiver(mWifiP2pReceiver, new IntentFilter(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION));
   }


   @Override
   protected void onPause()
   {
      Log.d(LOG_TAG, "onPause()");
      super.onPause();

      if (mWifiP2pHost != null && mIsWifiP2pEnabled) {
         mWifiP2pHost.stop();
      }

      unregisterReceiver(mWifiP2pReceiver);
   }


   @Override
   protected void onDestroy()
   {
      Log.d(LOG_TAG, "onDestroy()");

      if (mWifiP2pHost != null && mIsWifiP2pEnabled) {
         mWifiP2pHost.quit();
      }

      if (mNetService != null) {
         unbindService(this);
      }

      super.onDestroy();
   }

   @Override
   protected void onRestoreInstanceState(Bundle savedInstanceState)
   {
      Log.d(LOG_TAG, "onRestoreInstanceState()");
      super.onRestoreInstanceState(savedInstanceState);
   }

   @Override
   protected void onSaveInstanceState(Bundle outState)
   {
      Log.d(LOG_TAG, "onSaveInstanceState()");
      super.onSaveInstanceState(outState);
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.start, menu);
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   {
      switch (item.getItemId()) {
         case R.id.quit_app:
            finish();
            break;
      }

      return true;
   }



   @Override
   public boolean handleMessage(Message msg)
   {
      switch (msg.what) {
         case WIFI_P2P_START:
            mWifiP2pHost.start();
            break;

         case L2_DISCOVERY_STARTED:
            mStateLabel.setText(getString(R.string.searching_peers_msg));
            mConDisconBtn.setVisibility(View.INVISIBLE);
            break;

         case READY_TO_CONNECT:
            mConDisconBtn.setVisibility(View.VISIBLE);
            mConDisconBtn.setText(getString(R.string.connect_btn));
            mStateLabel.setText(getString(R.string.connect_ready_msg));
            break;

         case L2_CONNECTING:
            mStateLabel.setText(getString(R.string.connecting_msg));
            mConDisconBtn.setVisibility(View.INVISIBLE);
            break;

         case L2_CONNECTED:
            mConDisconBtn.setVisibility(View.VISIBLE);
            mConDisconBtn.setText(getString(R.string.disconnect_btn));
            if (!mWifiP2pHost.isGroupOwner()) {
               mStateLabel.setText(getString(R.string.wifi_connected));
               mNetService.sendServerHello(mGroupOwnerId);
            } else {
               mStateLabel.setText(getString(R.string.wifi_connected_go));
            }
            break;

         case L2_DISCONNECTED:
            mConDisconBtn.setText(getString(R.string.connect_btn));
            mStateLabel.setText("Disconnected!");
            break;

         case L3_CONNECTED:
            mStateLabel.setText(getString(R.string.connected_ready_msg));
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("peerId", mPeerId);
            startActivity(intent);
            mUiHandler.sendEmptyMessageDelayed(FINISH_ACTIVITY,2000);
            break;

         case WIFI_DISABLED:
            mStateLabel.setText(getString(R.string.wifi_disabled_msg));
            break;

         case FINISH_ACTIVITY:
            finish();
            break;
      }

      return true;
   }


   @Override
   public void onServiceConnected(ComponentName name, IBinder service)
   {
      Log.d(LOG_TAG, "onServiceConnected()");
      mNetService = ((NetService.LocalBinder) service).getService();
      mNetService.broadcastStatus();
      mNetService.setListener(mNetServiceListener);
      mNetService.start();
      mIsServiceConnected = true;
   }

   @Override
   public void onServiceDisconnected(ComponentName name)
   {
      Log.d(LOG_TAG, "onServiceDisconnected()");
      mIsServiceConnected = false;
      //mNetService.stop();
      //mNetService = null;
   }


   private void setup()
   {
      mIsServiceConnected = false;
      mIsWifiP2pEnabled = false;

      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
         Log.e(LOG_TAG, "API Level 16 or higher required!");
         finish();
         return;
      }

      bindService(new Intent(this, NetService.class), this, BIND_AUTO_CREATE);

      mStateLabel = (TextView) findViewById(R.id.state_label);
      mConDisconBtn = (Button) findViewById(R.id.con_discon_btn);

      mConDisconBtn.setOnClickListener(new View.OnClickListener()
      {
         @Override
         public void onClick(View v)
         {
            if (!mIsConnected) {

               if (mWifiP2pHost != null) {
                  mUiHandler.sendEmptyMessage(L2_CONNECTING);
                  mWifiP2pHost.connect();
               }

            } else {

               if (mWifiP2pHost != null) {
                  mWifiP2pHost.disconnect();
               }
            }
         }
      });

      mWifiP2pReceiver = new WifiP2pReceiver();

      mWifiP2pHost = new WifiP2pHost(StartActivity.this, new WifiP2pHost.Listener()
      {
         @Override
         public void onStartDiscovery()
         {
            mUiHandler.sendEmptyMessage(L2_DISCOVERY_STARTED);
         }

         @Override
         public void onReadyToConnect()
         {
            mUiHandler.sendEmptyMessage(READY_TO_CONNECT);
         }

         @Override
         public void onConnected(final InetAddress groupOwnerIp, final int groupOwnerPort)
         {
            mIsConnected = true;

            Log.d(LOG_TAG, "Server address: " + groupOwnerIp.getHostAddress() + ":" + groupOwnerPort);
            Log.i(LOG_TAG, "Im the " + (mWifiP2pHost.isGroupOwner() ? "server" : "client"));

            mGoIp = groupOwnerIp;
            mGoPort = groupOwnerPort;

            mGroupOwnerId = mGoIp.getHostAddress() + ":" + mGoPort;

            mUiHandler.sendEmptyMessage(L2_CONNECTED);
         }

         @Override
         public void onDisconnected()
         {
            mIsConnected = false;
            mUiHandler.sendEmptyMessage(L2_DISCONNECTED);
         }
      });


      mNetServiceListener = new NetService.Listener()
      {
         @Override
         public void onPeerReady()
         {
            mUiHandler.sendEmptyMessage(L3_CONNECTED);
         }

         @Override
         public void onMessage(c4.subnetzero.shipsdroid.net.Message newMsg, String peerId)
         {
            //Nothing here
         }
      };

      new Thread(new Runnable()
      {
         @Override
         public void run()
         {
            while (!mIsServiceConnected || !mIsWifiP2pEnabled) {
               try {
                  Thread.sleep(1000);
               } catch (InterruptedException e) {
               }
            }

            mUiHandler.sendEmptyMessageDelayed(WIFI_P2P_START, 1000);
         }
      }).start();

   }

   private class WifiP2pReceiver extends BroadcastReceiver
   {
      private static final String LOG_TAG = "WifiP2pReceiver";

      @Override
      public void onReceive(Context context, Intent intent)
      {
         String action = intent.getAction();

         switch (action) {
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
               onStateChanged(intent);
               break;
         }
      }

      private void onStateChanged(Intent intent)
      {
         int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);

         if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            Log.i(LOG_TAG, "Wifi P2P is enabled");
            mIsWifiP2pEnabled = true;
         } else {
            Log.i(LOG_TAG, "Wifi P2P is disabled. State " + state);
            mIsWifiP2pEnabled = false;
            Utils.launchWifiSettings(StartActivity.this);
         }
      }
   }


}
