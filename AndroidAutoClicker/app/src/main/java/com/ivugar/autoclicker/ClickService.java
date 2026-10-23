package com.ivugar.autoclicker;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Сервис спец. возможностей: плавающая панель + прицел, клики через dispatchGesture. */
public class ClickService extends AccessibilityService {

    static ClickService instance;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private LinearLayout panel;
    private TargetView target;
    private WindowManager.LayoutParams panelLp, targetLp;
    private Button playBtn;
    private TextView countText;

    private boolean running;
    private int interval;
    private int limit;
    private int done;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            clickAtTarget();
            done++;
            countText.setText(String.valueOf(done));
            if (limit > 0 && done >= limit) {
                stop();
                return;
            }
            handler.postDelayed(this, interval);
        }
    };

    @Override
    protected void onServiceConnected() {
        instance = this;
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        showOverlay();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
        stop();
    }

    @Override
    public void onDestroy() {
        stop();
        hideOverlay();
        instance = null;
        super.onDestroy();
    }

    void showOverlay() {
        if (panel != null) return;
        int dp = (int) getResources().getDisplayMetrics().density;

        // Прицел
        target = new TargetView(this);
        int size = 64 * dp;
        targetLp = baseParams(size, size);
        targetLp.x = getResources().getDisplayMetrics().widthPixels / 2 - size / 2;
        targetLp.y = getResources().getDisplayMetrics().heightPixels / 2 - size / 2;
        target.setOnTouchListener(new DragListener(target, targetLp));
        wm.addView(target, targetLp);

        // Панель управления
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = 6 * dp;
        panel.setPadding(pad, pad, pad, pad);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC222222);
        bg.setCornerRadius(12 * dp);
        panel.setBackground(bg);

        TextView handle = new TextView(this);
        handle.setText("✥");
        handle.setTextColor(Color.WHITE);
        handle.setTextSize(22);
        handle.setGravity(Gravity.CENTER);
        panel.addView(handle);

        playBtn = smallButton("▶");
        playBtn.setOnClickListener(v -> {
            if (running) stop(); else start();
        });
        panel.addView(playBtn);

        countText = new TextView(this);
        countText.setTextColor(Color.WHITE);
        countText.setGravity(Gravity.CENTER);
        countText.setText("0");
        panel.addView(countText);

        Button settingsBtn = smallButton("⚙");
        settingsBtn.setOnClickListener(v -> {
            stop();
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });
        panel.addView(settingsBtn);

        Button closeBtn = smallButton("✕");
        closeBtn.setOnClickListener(v -> {
            stop();
            hideOverlay();
        });
        panel.addView(closeBtn);

        panelLp = baseParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT);
        panelLp.x = 8 * dp;
        panelLp.y = 200 * dp;
        handle.setOnTouchListener(new DragListener(panel, panelLp));
        wm.addView(panel, panelLp);
    }

    void hideOverlay() {
        if (panel != null) {
            wm.removeView(panel);
            panel = null;
        }
        if (target != null) {
            wm.removeView(target);
            target = null;
        }
    }

    boolean isOverlayShown() {
        return panel != null;
    }

    private void start() {
        SharedPreferences p = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE);
        interval = Math.max(MainActivity.MIN_INTERVAL, p.getInt(MainActivity.KEY_INTERVAL, 500));
        limit = p.getInt(MainActivity.KEY_LIMIT, 0);
        done = 0;
        running = true;
        playBtn.setText("■");
        // Прицел не должен ловить наши же нажатия
        targetLp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        wm.updateViewLayout(target, targetLp);
        target.setAlpha(0.5f);
        handler.postDelayed(tick, 300);
    }

    private void stop() {
        running = false;
        handler.removeCallbacks(tick);
        if (playBtn != null) playBtn.setText("▶");
        if (target != null) {
            targetLp.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            wm.updateViewLayout(target, targetLp);
            target.setAlpha(1f);
        }
    }

    private void clickAtTarget() {
        int[] loc = new int[2];
        target.getLocationOnScreen(loc);
        float x = loc[0] + target.getWidth() / 2f;
        float y = loc[1] + target.getHeight() / 2f;
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 10))
                .build();
        dispatchGesture(gesture, null, null);
    }

    private WindowManager.LayoutParams baseParams(int w, int h) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                w, h,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        return lp;
    }

    private Button smallButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(18);
        int s = (int) (48 * getResources().getDisplayMetrics().density);
        b.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        b.setPadding(0, 0, 0, 0);
        return b;
    }

    /** Перетаскивание окна оверлея пальцем. */
    private class DragListener implements View.OnTouchListener {
        private final View view;
        private final WindowManager.LayoutParams lp;
        private int startX, startY;
        private float touchX, touchY;

        DragListener(View view, WindowManager.LayoutParams lp) {
            this.view = view;
            this.lp = lp;
        }

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = lp.x;
                    startY = lp.y;
                    touchX = e.getRawX();
                    touchY = e.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    lp.x = startX + (int) (e.getRawX() - touchX);
                    lp.y = startY + (int) (e.getRawY() - touchY);
                    wm.updateViewLayout(view, lp);
                    return true;
            }
            return false;
        }
    }

    /** Круглый прицел, центр которого — точка клика. */
    private static class TargetView extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

        TargetView(Context c) {
            super(c);
            fill.setColor(0x55E53935);
            stroke.setColor(0xFFE53935);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(3 * c.getResources().getDisplayMetrics().density);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float r = Math.min(cx, cy) - stroke.getStrokeWidth();
            canvas.drawCircle(cx, cy, r, fill);
            canvas.drawCircle(cx, cy, r, stroke);
            canvas.drawLine(cx - r, cy, cx + r, cy, stroke);
            canvas.drawLine(cx, cy - r, cx, cy + r, stroke);
        }
    }
}
