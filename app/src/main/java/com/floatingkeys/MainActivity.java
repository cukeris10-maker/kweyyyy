package com.floatingkeys;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Simple setup screen.
 * Guides the user through two one-time permission grants:
 *   1. "Draw over other apps" (SYSTEM_ALERT_WINDOW)
 *   2. Accessibility Service (for key injection)
 * Then lets them start/stop the floating overlay.
 */
public class MainActivity extends AppCompatActivity {

    private Button btnOverlay, btnAccessibility, btnToggle;
    private TextView tvStatusOverlay, tvStatusAccessibility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatusOverlay     = findViewById(R.id.tvStatusOverlay);
        tvStatusAccessibility = findViewById(R.id.tvStatusAccessibility);
        btnOverlay          = findViewById(R.id.btnOverlayPermission);
        btnAccessibility    = findViewById(R.id.btnAccessibility);
        btnToggle           = findViewById(R.id.btnToggleOverlay);

        btnOverlay.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            startActivity(intent);
        });

        btnAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        btnToggle.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                btnOverlay.performClick();
                return;
            }
            if (KeyInjectionAccessibilityService.instance == null) {
                btnAccessibility.performClick();
                return;
            }
            toggleOverlay();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        boolean hasOverlay   = Settings.canDrawOverlays(this);
        boolean hasA11y      = KeyInjectionAccessibilityService.instance != null;

        tvStatusOverlay.setText(hasOverlay
                ? "✅ Overlay permission granted"
                : "❌ Overlay permission needed (Step 1)");

        tvStatusAccessibility.setText(hasA11y
                ? "✅ Accessibility service enabled"
                : "❌ Accessibility service needed (Step 2)");

        if (hasOverlay && hasA11y) {
            btnToggle.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF2E7D32));
        }
    }

    private boolean overlayRunning = false;

    private void toggleOverlay() {
        Intent svc = new Intent(this, FloatingOverlayService.class);
        if (!overlayRunning) {
            startForegroundService(svc);
            btnToggle.setText("⏹ Stop Floating Keyboard");
            btnToggle.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFB71C1C));
            overlayRunning = true;
        } else {
            stopService(svc);
            btnToggle.setText("▶ Start Floating Keyboard");
            btnToggle.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF2E7D32));
            overlayRunning = false;
        }
    }
}
