package c4.subnetzero.shipsdroid.net;


import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class NetController
{
   private static final String LOG_TAG = "NetController";
   private static final int PORT = 60000;
   private static final int ECHO_PORT = 64777;
   private DatagramSocket mP2pSocket;
   private DatagramSocket mEchoSocket;
   private Thread mReceiverThread;
   private Thread mReachableThread;
   private Listener mListener;
   private volatile boolean mIsReachable;
   private volatile boolean mIsReachableThreadPaused;
   private InetAddress mPeerIp;
   private volatile int mPort;

   public NetController(final Listener listener)
   {
      mListener = listener;
      mIsReachableThreadPaused = true;
      setupSockets();
   }

   public void setListener(final Listener listener)
   {
      mListener = listener;
   }

   public int getPort()
   {
      return mPort;
   }

   public void pauseReachableThread(final boolean pauseFlag)
   {
      mIsReachableThreadPaused = pauseFlag;
   }

   public void startReceiverThread()
   {
      if (mReceiverThread == null || !mReceiverThread.isAlive()) {

         mReceiverThread = new Thread(new Runnable()
         {
            @Override
            public void run()
            {
               Log.i(LOG_TAG, "P2P-Receiver thread started...");
               String msg, id;

               while (!mReceiverThread.isInterrupted()) {
                  byte[] packetData = new byte[1024];
                  DatagramPacket packet = new DatagramPacket(packetData, packetData.length);
                  try {
                     // blocking call
                     mP2pSocket.receive(packet);
                     mPeerIp = packet.getAddress();
                     id = mPeerIp.getHostAddress() + ":" + packet.getPort();
                     msg = new String(packet.getData(), "UTF-8");
                     parsePacket(msg, id);
                  } catch (IOException e) {
                     if (!(e instanceof SocketTimeoutException)) {
                        Log.e(LOG_TAG, "Receiver Thread:", e);
                     }
                  }
               }

               mP2pSocket.close();

               Log.d(LOG_TAG, "Out of while loop");
            }
         }, "P2pReceiverThread");

         mReceiverThread.start();
      }
   }

   public void stopReceiverThread()
   {
      Log.i(LOG_TAG, "Stopping P2P-Receiver Thread");
      mListener = null;
      if (mReceiverThread != null && mReceiverThread.isAlive()) {
         mReceiverThread.interrupt();
      }
   }

   public void sendMessage(final String message, final String peerAddress)
   {
      Log.d(LOG_TAG, "TX: " + message + " -> " + peerAddress);
      sendString(message, peerAddress);
   }

   public void sendMessage(final Message message, final String peerAddress)
   {
      final Gson gson = new Gson();
      Log.d(LOG_TAG, "TX: " + gson.toJson(message) + " -> " + peerAddress);
      sendString(gson.toJson(message), peerAddress);
   }

   private void sendString(final String message, final String peerAddress)
   {
      new Thread(new Runnable()
      {
         @Override
         public void run()
         {
            try {
               int peerPort = PORT;
               String peerIp = peerAddress;
               String[] ipAndPort = peerAddress.split(":");

               if (ipAndPort.length > 1) {
                  peerIp = ipAndPort[0];
                  peerPort = Integer.parseInt(ipAndPort[1]);
               }

               Log.d(LOG_TAG, "peerIp: " + peerIp + " peerPort: " + peerPort);

               byte[] pktData = message.getBytes("UTF-8");
               DatagramPacket packet = new DatagramPacket(pktData, pktData.length,
                     InetAddress.getByName(peerIp), peerPort);
               mP2pSocket.send(packet);
            } catch (IOException e) {
               Log.e(LOG_TAG, "", e);
            }
         }
      }, "P2pSenderThread").start();
   }

   private void parsePacket(String jsonMsg, final String peerId)
   {
      Gson gson = new GsonBuilder().serializeNulls().create();

      try {
         jsonMsg = jsonMsg.trim();
         Log.d(LOG_TAG, "RX: " + jsonMsg + " <- " + peerId);
         Message newMsg = gson.fromJson(jsonMsg, Message.class);
         if (mListener != null) {
            mListener.onMessage(newMsg, peerId);
         }
      } catch (JsonSyntaxException e) {
         if (mListener != null) {
            mListener.onMessage(jsonMsg, peerId);
         }
         //Log.e(LOG_TAG, "", e);
      }
   }


   private void setupSockets()
   {
      try {

         mP2pSocket = new DatagramSocket();
         mP2pSocket.setSoTimeout(299);

         mPort = mP2pSocket.getLocalPort();

         Log.i(LOG_TAG, "Bound to: " + mP2pSocket.getLocalAddress().getHostAddress() + ":" + mPort);

      } catch (SocketException e) {
         Log.e(LOG_TAG, "", e);
      }
   }


   public void startReachableThread()
   {
      if (mReachableThread == null || !mReachableThread.isAlive()) {

         mReachableThread = new Thread(new Runnable()
         {
            @Override
            public void run()
            {
               Log.d(LOG_TAG, "Starting P2P-Reachable Thread");
               byte[] echoData = "ShipsDroid".getBytes();

               if (mEchoSocket == null) {
                  try {
                     mEchoSocket = new DatagramSocket(ECHO_PORT);
                     mEchoSocket.setReuseAddress(true);
                     mEchoSocket.setSoTimeout(499);
                  } catch (SocketException e) {
                     Log.e(LOG_TAG, "Echo socket create:", e);
                  }
               }

               while (!mReachableThread.isInterrupted()) {
                  boolean isReachable = false;
                  try {

                     if (mPeerIp == null || mIsReachableThreadPaused) {
                        Thread.sleep(200);
                        continue;
                     }

                     if (mIsReachable) {
                        Thread.sleep(888);
                     }

                     //isReachable = peerAddress.isReachable(2123);
                     mEchoSocket.send(new DatagramPacket(echoData, echoData.length, mPeerIp, ECHO_PORT));
                     mEchoSocket.receive(new DatagramPacket(new byte[echoData.length], echoData.length));
                     isReachable = true;
                  } catch (Exception e) {
                     if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                        continue;
                     }
                     isReachable = (e instanceof PortUnreachableException);
                  } finally {
                     //Log.d(LOG_TAG, "Reachable Loop: " + isReachable+"/"+mIsReachable);
                     if (mIsReachable != isReachable) {
                        mIsReachable = isReachable;
                        if (mListener != null) {
                           mListener.onReachabilityChanged(isReachable, null);
                        }
                     }
                  }
               }

               Log.d(LOG_TAG, "Out of ReachableThread");
               mEchoSocket.close();
               mEchoSocket = null;
            }
         }, "P2pReachableThread");

         mReachableThread.start();
      }
   }

   public void stopReachableThread()
   {
      Log.i(LOG_TAG, "Stopping P2P-Reachable Thread");
      if (mReachableThread != null && mReachableThread.isAlive()) {
         mReachableThread.interrupt();
         //mReachableThread = null;
      }
   }


   public interface Listener
   {
      void onMessage(final Message newMsg, final String peerId);

      void onMessage(final String newMsg, final String peerId);

      void onReachabilityChanged(final boolean reachable, final String peerId);

      void onError(final String errMsg);
   }

}
