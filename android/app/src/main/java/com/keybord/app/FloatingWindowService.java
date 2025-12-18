package com.keybord.app;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class FloatingWindowService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (floatingView == null) {
            createFloatingWindow();
        }
        return START_STICKY;
    }

    private void createFloatingWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // Create the floating view
        floatingView = buildFloatingUI();
        
        // Window parameters
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int width = (int) (dm.widthPixels * 0.85);
        int height = (int) (dm.heightPixels * 0.6);
        
        params = new WindowManager.LayoutParams(
            width,
            height,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.CENTER;
        params.x = 0;
        params.y = 0;
        
        windowManager.addView(floatingView, params);
    }
    
    private View buildFloatingUI() {
        // Main container
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(Color.parseColor("#1a1a2e"));
        container.setPadding(dp(16), dp(16), dp(16), dp(16));
        
        // Add rounded corners effect
        container.setElevation(dp(10));
        
        // Header with drag handle
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(16));
        
        // Drag indicator
        View dragHandle = new View(this);
        LinearLayout.LayoutParams dragParams = new LinearLayout.LayoutParams(dp(40), dp(4));
        dragParams.gravity = Gravity.CENTER;
        dragHandle.setLayoutParams(dragParams);
        dragHandle.setBackgroundColor(Color.parseColor("#4a4a6a"));
        
        // Title
        TextView title = new TextView(this);
        title.setText("⚡ Quick Tools");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(titleParams);
        
        // Close button
        TextView closeBtn = new TextView(this);
        closeBtn.setText("✕");
        closeBtn.setTextColor(Color.parseColor("#ff6b6b"));
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        closeBtn.setPadding(dp(12), dp(8), dp(12), dp(8));
        closeBtn.setOnClickListener(v -> stopSelf());
        
        header.addView(title);
        header.addView(closeBtn);
        
        // Make header draggable
        header.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });
        
        // Content area with scroll
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ));
        
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(8), 0, dp(8));
        
        // Add tool buttons
        String[][] tools = {
            {"🎨", "Themes", "Customize keyboard colors"},
            {"📋", "Clipboard", "View clipboard history"},
            {"😀", "Stickers", "Send fun stickers"},
            {"🔤", "Fonts", "Change text style"},
            {"🌐", "Translate", "Quick translation"},
            {"📝", "Notes", "Quick notes"},
            {"🔊", "TTS", "Text to speech"},
            {"🤖", "AI Write", "AI writing assistant"}
        };
        
        for (String[] tool : tools) {
            content.addView(createToolButton(tool[0], tool[1], tool[2]));
        }
        
        scroll.addView(content);
        
        // Build container
        container.addView(header);
        container.addView(scroll);
        
        return container;
    }
    
    private View createToolButton(String icon, String title, String subtitle) {
        LinearLayout btn = new LinearLayout(this);
        btn.setOrientation(LinearLayout.HORIZONTAL);
        btn.setGravity(Gravity.CENTER_VERTICAL);
        btn.setPadding(dp(12), dp(14), dp(12), dp(14));
        btn.setBackgroundColor(Color.parseColor("#252545"));
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(0, dp(4), 0, dp(4));
        btn.setLayoutParams(btnParams);
        
        // Icon
        TextView iconView = new TextView(this);
        iconView.setText(icon);
        iconView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        iconView.setPadding(0, 0, dp(16), 0);
        
        // Text container
        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        titleView.setTypeface(null, Typeface.BOLD);
        
        TextView subtitleView = new TextView(this);
        subtitleView.setText(subtitle);
        subtitleView.setTextColor(Color.parseColor("#9ca3af"));
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        
        textContainer.addView(titleView);
        textContainer.addView(subtitleView);
        
        // Arrow
        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextColor(Color.parseColor("#6b7280"));
        arrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        
        btn.addView(iconView);
        btn.addView(textContainer);
        btn.addView(arrow);
        
        btn.setOnClickListener(v -> {
            // Show toast for now - you can add real functionality later
            android.widget.Toast.makeText(this, icon + " " + title + " clicked!", android.widget.Toast.LENGTH_SHORT).show();
        });
        
        return btn;
    }
    
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }
    }
}