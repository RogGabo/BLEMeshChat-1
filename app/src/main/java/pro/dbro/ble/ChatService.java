package pro.dbro.ble;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Created by davidbrodsky on 11/4/14.
 */
public class ChatService extends Service implements ActivityRecevingMessagesIndicator {
    public static final String TAG = "ChatService";

    private ChatApp mApp;
    private boolean mActivityRecevingMessages;

    private ChatServiceBinder mBinder;

    private Looper mBackgroundLooper;
    private BackgroundThreadHandler mBackgroundHandler;
    private Handler mForegroundHandler;

    /** Handler Messages */
    public static final int CONNECT       = 0;
    public static final int SEND_MESSAGEE = 1;
    public static final int SHUTDOWN      = 2;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        mApp = new ChatApp(getApplicationContext());
        mApp.setActivityBoundIndicator(this);
        // Immediately make us available if an identity is available
        if (mApp.getPrimaryIdentity() != null) mApp.makeAvailable();

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread(TAG, android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mBackgroundLooper = thread.getLooper();
        mBackgroundHandler = new BackgroundThreadHandler(mBackgroundLooper);
        mForegroundHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (mBinder == null) mBinder = new ChatServiceBinder();
        Log.i(TAG, "Bind service");
        return mBinder;
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    /** ActivityRecevingMessagesIndicator */
    @Override
    public boolean isActivityReceivingMessages() {
        return mActivityRecevingMessages;
    }

    /** Binder through which Activities can interact with this Service */
    public class ChatServiceBinder extends Binder {

//        public ChatService getService() {
//            // Return this instance of LocalService so clients can call public methods
//            return ChatService.this;
//        }

        public void connect() {
            mBackgroundHandler.sendMessage(mBackgroundHandler.obtainMessage(CONNECT));
        }

        public void sendPublicMessageFromPrimaryIdentity(String message) {
            mBackgroundHandler.sendMessage(mBackgroundHandler.obtainMessage(SEND_MESSAGEE, message));
        }

        public ChatApp getChatApp() {
            return mApp;
        }

        public void shutdown() {
            mBackgroundHandler.sendMessage(mBackgroundHandler.obtainMessage(SHUTDOWN));
        }

        /**
         * Set by Activity bound to this Service. If isActive is false, this Service
         * should post incoming messages as Notifications.
         */
        public void setActivityReceivingMessages(boolean receivingMessages) {
            mActivityRecevingMessages = receivingMessages;
            Log.i(TAG, "setActivityRecevingMessages " + receivingMessages);
        }
    }

    /** Handler that processes Messages on a background thread */
    private final class BackgroundThreadHandler extends Handler {
        public BackgroundThreadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONNECT:
                    Log.i(TAG, "handling connect");
                    mApp.makeAvailable();
                    break;
                case SEND_MESSAGEE:
                    mApp.sendPublicMessageFromPrimaryIdentity((String) msg.obj);
                    break;
                case SHUTDOWN:
                    Log.i(TAG, "handling shutdown");
                    mApp.makeUnavailable();

                    // Stop the service using the startId, so that we don't stop
                    // the service in the middle of handling another job
                    stopSelf(msg.arg1);
                    break;
            }
        }
    }
}
