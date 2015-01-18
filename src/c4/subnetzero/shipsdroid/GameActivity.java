package c4.subnetzero.shipsdroid;


import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import c4.subnetzero.shipsdroid.controller.GameEngine;
import c4.subnetzero.shipsdroid.view.EnemyFleetView;
import c4.subnetzero.shipsdroid.view.GridButtonHandler;
import c4.subnetzero.shipsdroid.view.OwnFleetView;

public class GameActivity extends Activity implements Handler.Callback,ServiceConnection
{

   private static final String LOG_TAG = "GameActivity";

   private boolean mRestarted;
   private Handler mUiHandler;
   private View mEnemyBoard;
   private View mOwnBoard;
   private EnemyFleetView mEnemyFleetView;
   private OwnFleetView mOwnFleetView;
   private GameEngine mGameEngine;
   private NetService mNetService;
   private GridButtonHandler mGridButtonHandler;
   private TextView mShotClockView;
   private TextView mEnemyShipsView;
   private TextView mMyShipsView;
   //private ActionBar mActionBar;

   public static final int UPDATE_SHOT_CLOCK  = 1;
   public static final int UPDATE_SCORE_BOARD = 2;


   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      Log.d(LOG_TAG, "onCreate()");
      super.onCreate(savedInstanceState);
      setContentView(R.layout.game);

      mUiHandler = new Handler(this);
      mRestarted = false;
      setup();
   }

   @Override
   protected void onRestart()
   {
      Log.d(LOG_TAG, "onRestart()");
      super.onRestart();
      mRestarted = true;
   }

   @Override
   protected void onStart()
   {
      Log.d(LOG_TAG, "onStart()");
      super.onStart();
   }


   @Override
   protected void onResume()
   {
      Log.d(LOG_TAG, "onResume()");
      super.onResume();
   }


   @Override
   protected void onPause()
   {
      Log.d(LOG_TAG, "onPause()");
      super.onPause();
   }


   @Override
   protected void onDestroy()
   {
      Log.d(LOG_TAG, "onDestroy()");
      if (mGameEngine != null) {
         mGameEngine.shutDown();
      }
      unbindService(this);
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


   // Now the size of the views should be available
   @Override
   public void onWindowFocusChanged(boolean hasFocus)
   {
      super.onWindowFocusChanged(hasFocus);

      if (mEnemyFleetView == null) {
         buildGameBoard();
      }
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu)
   {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.game, menu);
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item)
   {

      switch (item.getItemId()) {
         case R.id.new_game:
            mGameEngine.newGame();
            break;
         case R.id.abort_game:
            mGameEngine.abortGame();
            break;
         case R.id.quit_game_app:
            finish();
            break;
      }

      return true;
   }

   @Override
   public boolean handleMessage(Message msg)
   {
      switch (msg.what){
         case UPDATE_SHOT_CLOCK:
            mShotClockView.setText(String.valueOf(msg.arg1));
            break;
         case UPDATE_SCORE_BOARD:
            mMyShipsView.setText(String.valueOf(msg.arg1));
            mEnemyShipsView.setText(String.valueOf(msg.arg2));
            break;
         default:
            // Get me a beer!!
            break;

      }


      return true;
   }

   @Override
   public void onServiceConnected(ComponentName name, IBinder service)
   {
      Log.d(LOG_TAG, "onServiceConnected()");
      mNetService = ((NetService.LocalBinder) service).getService();

      mGameEngine = new GameEngine(this, mNetService);
      mGameEngine.setModelUpdateListener(mOwnFleetView, mEnemyFleetView);

      mGridButtonHandler.setGameEngine(mGameEngine);
   }

   @Override
   public void onServiceDisconnected(ComponentName name)
   {
      Log.d(LOG_TAG, "onServiceDisconnected()");
      mNetService.stop();
      mNetService = null;
   }


   public Handler getUiHandler()
   {
      return mUiHandler;
   }

   private void setup()
   {
      mShotClockView = (TextView)findViewById(R.id.shot_clock);
      ActionBar mActionBar = getActionBar();

      if ( mActionBar != null ) {
         //LayoutInflater inflater = LayoutInflater.from(this);
         LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         View scoreBoardView = (View)inflater.inflate(R.layout.score, null);

         mEnemyShipsView = (TextView)scoreBoardView.findViewById(R.id.enemy_ships);
         mMyShipsView    = (TextView)scoreBoardView.findViewById(R.id.my_ships);

         mActionBar.setDisplayShowHomeEnabled(true);
         mActionBar.setDisplayHomeAsUpEnabled(false);
         mActionBar.setDisplayUseLogoEnabled(true);
         mActionBar.setDisplayShowTitleEnabled(false);
         mActionBar.setDisplayShowCustomEnabled(true);

         mActionBar.setCustomView(scoreBoardView);
      }

      mGridButtonHandler = new GridButtonHandler(this);
      bindService(new Intent(this, NetService.class), this, BIND_AUTO_CREATE);
   }

   private void buildGameBoard()
   {
      DisplayMetrics metrics = getResources().getDisplayMetrics();
      ViewGroup enemyFrame = (ViewGroup) findViewById(R.id.enemy_fleet_frame);
      ViewGroup ownFrame = (ViewGroup) findViewById(R.id.own_fleet_frame);

      int minEnemyFrameSize = enemyFrame.getMeasuredWidth() > enemyFrame.getMeasuredHeight() ?
              enemyFrame.getMeasuredHeight() : enemyFrame.getMeasuredWidth();

      int minOwnFrameSize = ownFrame.getMeasuredWidth() > ownFrame.getMeasuredHeight() ?
              ownFrame.getMeasuredHeight() : ownFrame.getMeasuredWidth();

      minEnemyFrameSize = 12 * (int) (minEnemyFrameSize / 12.0f);
      minOwnFrameSize = 12 * (int) (minOwnFrameSize / 12.0f);

      LayoutInflater inflater = LayoutInflater.from(this);

      mEnemyBoard = inflater.inflate(R.layout.board, enemyFrame, false);
      mEnemyFleetView = new EnemyFleetView(this, (ViewGroup) mEnemyBoard, mGridButtonHandler, minEnemyFrameSize - 12);
      enemyFrame.addView(mEnemyBoard);

      mOwnBoard = inflater.inflate(R.layout.board, enemyFrame, false);
      mOwnFleetView = new OwnFleetView(this, (ViewGroup) mOwnBoard, minOwnFrameSize - (int) (30 * metrics.density));
      ownFrame.addView(mOwnBoard);

      if (mGameEngine != null) {
         mGameEngine.setModelUpdateListener(mOwnFleetView, mEnemyFleetView);
      }

   }

}
