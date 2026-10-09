package com.floatingkeys;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.core.app.NotificationCompat;

public class FloatingKeyService extends Service {

    public static boolean isRunning = false;
    private static final String CHANNEL_ID = "FloatingKeyChannel";
    private static final int NOTIF_ID = 1;

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;

    // Drag tracking
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification());

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_keys, null);

        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.x = 0;
        params.y = 100;

        windowManager.addView(floatingView, params);

        setupButtons();
        setupDrag();
    }

    private void setupButtons() {
        ImageButton btnUp = floatingView.findViewById(R.id.btnUp);
        ImageButton btnDown = floatingView.findViewById(R.id.btnDown);
        ImageButton btnLeft = floatingView.findViewById(R.id.btnLeft);
        ImageButton btnRight = floatingView.findViewById(R.id.btnRight);
        View btnSpace = floatingView.findViewById(R.id.btnSpace);

        btnUp.setOnTouchListener((v, event) -> handleKeyTouch(event, KeyEvent.KEYCODE_DPAD_UP, v));
        btnDown.setOnTouchListener((v, event) -> handleKeyTouch(event, KeyEvent.KEYCODE_DPAD_DOWN, v));
        btnLeft.setOnTouchListener((v, event) -> handleKeyTouch(event, KeyEvent.KEYCODE_DPAD_LEFT, v));
        btnRight.setOnTouchListener((v, event) -> handleKeyTouch(event, KeyEvent.KEYCODE_DPAD_RIGHT, v));
        btnSpace.setOnTouchListener((v, event) -> handleKeyTouch(event, KeyEvent.KEYCODE_SPACE, v));
    }

    /**
     * Injects real KeyEvents into the system event queue.
     * This simulates a physical keyboard press that games will receive.
     */
    private boolean handleKeyTouch(MotionEvent event, int keyCode, View v) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            v.setPressed(true);
            injectKey(keyCode, KeyEvent.ACTION_DOWN);
            return true;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            v.setPressed(false);
            injectKey(keyCode, KeyEvent.ACTION_UP);
            return true;
        }
        return false;
    }

    private void injectKey(int keyCode, int action) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        KeyEvent keyEvent = new KeyEvent(
                downTime,
                eventTime,
                action,
                keyCode,
                0,  // repeat
                0,  // meta state
                KeyEvent.KEYCODE_UNKNOWN, // device id
                0,  // scan code
                KeyEvent.FLAG_FROM_SYSTEM | KeyEvent.FLAG_SOFT_KEYBOARD,
                0x101 // source: keyboard
        );
        // Dispatch to the root window
        floatingView.getRootView().dispatchKeyEvent(keyEvent);
    }

    private void setupDrag() {
        LinearLayout dragHandle = floatingView.findViewById(R.id.dragHandle);
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Floating Keys Service", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps floating keyboard overlay active");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent stopIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Floating Arrow Keys")
                .setContentText("Overlay keyboard is active")
                .setSmallIcon(android.R.drawable.ic_menu_directions)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
        }
    }
}
