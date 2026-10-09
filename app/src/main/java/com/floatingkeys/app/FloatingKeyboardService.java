package com.floatingkeys.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import androidx.core.app.NotificationCompat;

public class FloatingKeyboardService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;

    private int initialX, initialY;
    private float initialTouchX, initialTouchY;

    private static final String CHANNEL_ID = "FloatingKeyboardChannel";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification());

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_keyboard, null);

        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.BOTTOM | Gravity.START;
        params.x = 50;
        params.y = 100;

        windowManager.addView(floatingView, params);

        setupButtons();
        setupDrag();
    }

    private void setupButtons() {
        setupKey(R.id.btn_up, KeyEvent.KEYCODE_DPAD_UP);
        setupKey(R.id.btn_down, KeyEvent.KEYCODE_DPAD_DOWN);
        setupKey(R.id.btn_left, KeyEvent.KEYCODE_DPAD_LEFT);
        setupKey(R.id.btn_right, KeyEvent.KEYCODE_DPAD_RIGHT);
        setupKey(R.id.btn_space, KeyEvent.KEYCODE_SPACE);
    }

    private void setupKey(int viewId, int keyCode) {
        View btn = floatingView.findViewById(viewId);
        btn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    injectKey(keyCode, KeyEvent.ACTION_DOWN);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    injectKey(keyCode, KeyEvent.ACTION_UP);
                    return true;
            }
            return false;
        });
    }

    private void injectKey(int keyCode, int action) {
        long downTime = android.os.SystemClock.uptimeMillis();
        KeyEvent event = new KeyEvent(downTime, downTime, action, keyCode, 0);
        // Dispatch via instrumentation - works for most game frameworks
        try {
            android.app.Instrumentation inst = new android.app.Instrumentation();
            if (action == KeyEvent.ACTION_DOWN) {
                inst.sendKeyDownUpSync(keyCode);
            }
        } catch (Exception e) {
            // Fallback: broadcast the key event
            Intent keyIntent = new Intent("com.floatingkeys.KEY_EVENT");
            keyIntent.putExtra("keyCode", keyCode);
            keyIntent.putExtra("action", action);
            sendBroadcast(keyIntent);
        }
    }

    private void setupDrag() {
        View dragHandle = floatingView.findViewById(R.id.drag_handle);
        dragHandle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    params.x = initialX + (int) (event.getRawX() - initialTouchX);
                    params.y = initialY - (int) (event.getRawY() - initialTouchY);
                    windowManager.updateViewLayout(floatingView, params);
                    return true;
            }
            return false;
        });
    }

    private Notification buildNotification() {
        Intent stopIntent = new Intent(this, FloatingKeyboardService.class);
        stopIntent.setAction("STOP");
        PendingIntent stopPending = PendingIntent.getService(this, 0, stopIntent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Floating Keys Active")
                .setContentText("Arrow keys overlay is running")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .addAction(android.R.drawable.ic_delete, "Stop", stopPending)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Floating Keyboard", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps the floating keyboard overlay running");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) windowManager.removeView(floatingView);
    }
}
