package com.keybord.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

public class FloatingWindowService extends Service {

    private static final String CHANNEL_ID = "floating_window_channel";
    
    private WindowManager windowManager;
    private FrameLayout floatingView;
    private WebView webView;
    private WindowManager.LayoutParams params;
    private String currentMode = "tools";
    
    // Touch handling for drag
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            currentMode = intent.getStringExtra("mode");
            if (currentMode == null) currentMode = "tools";
        }
        
        // Start as foreground service (required for Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("KeyBord Tools")
                .setContentText("Floating panel is active")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
            startForeground(1, notification);
        }
        
        if (floatingView == null) {
            createFloatingWindow();
        } else {
            // Update content if already open
            loadContent();
        }
        
        return START_STICKY;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Floating Window",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void createFloatingWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // Create container
        floatingView = new FrameLayout(this);
        floatingView.setBackgroundColor(Color.parseColor("#1a1a2e"));
        
        // Create WebView for HTML content
        webView = new WebView(this);
        webView.setBackgroundColor(Color.TRANSPARENT);
        
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        
        webView.addJavascriptInterface(new WebBridge(), "NativeBridge");
        
        webView.setWebViewClient(new WebViewClient());
        
        floatingView.addView(webView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
        
        // Window params
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int width = (int) (dm.widthPixels * 0.9);
        int height = (int) (dm.heightPixels * 0.55);
        
        params = new WindowManager.LayoutParams(
            width,
            height,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.CENTER;
        
        // Touch listener for dragging
        floatingView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    isDragging = false;
                    return true;
                    
                case MotionEvent.ACTION_MOVE:
                    int dx = (int) (event.getRawX() - initialTouchX);
                    int dy = (int) (event.getRawY() - initialTouchY);
                    
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isDragging = true;
                        params.x = initialX + dx;
                        params.y = initialY + dy;
                        windowManager.updateViewLayout(floatingView, params);
                    }
                    return true;
                    
                case MotionEvent.ACTION_UP:
                    return isDragging;
            }
            return false;
        });
        
        windowManager.addView(floatingView, params);
        loadContent();
    }
    
    private void loadContent() {
        String html = getHtmlContent();
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }
    
    private String getHtmlContent() {
        return "<!DOCTYPE html>" +
"<html><head>" +
"<meta charset='UTF-8'>" +
"<meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>" +
"<style>" +
"*{margin:0;padding:0;box-sizing:border-box;-webkit-tap-highlight-color:transparent;user-select:none}" +
"body{font-family:system-ui,sans-serif;background:#1a1a2e;color:#fff;height:100vh;display:flex;flex-direction:column}" +
".header{display:flex;align-items:center;justify-content:space-between;padding:14px 16px;background:#141424;border-bottom:1px solid rgba(255,255,255,0.1)}" +
".header h1{font-size:16px;font-weight:600}" +
".close-btn{width:32px;height:32px;display:flex;align-items:center;justify-content:center;background:rgba(255,100,100,0.2);border-radius:50%;color:#ff6b6b;font-size:18px;cursor:pointer}" +
".tabs{display:flex;padding:8px 12px;gap:8px;background:#12121f}" +
".tab{flex:1;padding:10px;text-align:center;background:rgba(255,255,255,0.05);border-radius:8px;font-size:13px;cursor:pointer;transition:all 0.2s}" +
".tab.active{background:#3b82f6;color:#fff}" +
".content{flex:1;overflow-y:auto;padding:12px}" +
".grid{display:grid;grid-template-columns:repeat(4,1fr);gap:10px}" +
".item{display:flex;flex-direction:column;align-items:center;padding:14px 8px;background:rgba(255,255,255,0.05);border-radius:12px;cursor:pointer;transition:all 0.15s}" +
".item:active{transform:scale(0.95);background:rgba(255,255,255,0.1)}" +
".item .icon{font-size:28px;margin-bottom:6px}" +
".item .label{font-size:11px;color:rgba(255,255,255,0.7);text-align:center}" +
".emoji-grid{display:grid;grid-template-columns:repeat(8,1fr);gap:4px}" +
".emoji{font-size:24px;padding:8px;text-align:center;cursor:pointer;border-radius:8px}" +
".emoji:active{background:rgba(255,255,255,0.1)}" +
".section-title{font-size:12px;color:rgba(255,255,255,0.5);margin:12px 0 8px;padding-left:4px}" +
"</style></head><body>" +
"<div class='header'>" +
"  <h1>✨ Quick Tools</h1>" +
"  <div class='close-btn' onclick='closeWindow()'>✕</div>" +
"</div>" +
"<div class='tabs'>" +
"  <div class='tab active' onclick='showTab(this,\"tools\")'>🔧 Tools</div>" +
"  <div class='tab' onclick='showTab(this,\"emoji\")'>😀 Emoji</div>" +
"  <div class='tab' onclick='showTab(this,\"clip\")'>📋 Clip</div>" +
"</div>" +
"<div class='content' id='content'></div>" +
"<script>" +
"var currentTab='tools';" +
"function showTab(el,tab){" +
"  document.querySelectorAll('.tab').forEach(t=>t.classList.remove('active'));" +
"  el.classList.add('active');" +
"  currentTab=tab;" +
"  render();" +
"}" +
"function render(){" +
"  var html='';" +
"  if(currentTab==='tools'){" +
"    html='<div class=\"grid\">';" +
"    var tools=[" +
"      ['🎨','Themes'],['🔤','Fonts'],['📐','Size'],['🌐','Language']," +
"      ['🔊','Sound'],['📳','Vibrate'],['⌨️','Layout'],['☁️','Sync']," +
"      ['🤖','AI'],['📝','Notes'],['🔍','Search'],['⚙️','Settings']" +
"    ];" +
"    tools.forEach(function(t){" +
"      html+='<div class=\"item\" onclick=\"toolClick(\\''+t[1]+'\\')\"><span class=\"icon\">'+t[0]+'</span><span class=\"label\">'+t[1]+'</span></div>';" +
"    });" +
"    html+='</div>';" +
"  }else if(currentTab==='emoji'){" +
"    var categories={" +
"      'Smileys':['😀','😃','😄','😁','😅','😂','🤣','😊','😇','🥰','😍','🤩','😘','😗','😚','😋','😛','😜','🤪','😝','🤑','🤗','🤭','🤫','🤔','🤐','🤨','😐','😑','😶']," +
"      'Gestures':['👍','👎','👊','✊','🤛','🤜','👏','🙌','👐','🤲','🤝','🙏','✌️','🤞','🤟','🤘','👌','🤌','🤏','👈','👉','👆','👇','☝️','✋','🤚','🖐️','🖖','👋','🤙']," +
"      'Hearts':['❤️','🧡','💛','💚','💙','💜','🖤','🤍','🤎','💔','❣️','💕','💞','💓','💗','💖','💘','💝','💟']" +
"    };" +
"    for(var cat in categories){" +
"      html+='<div class=\"section-title\">'+cat+'</div><div class=\"emoji-grid\">';" +
"      categories[cat].forEach(function(e){" +
"        html+='<div class=\"emoji\" onclick=\"typeEmoji(\\''+e+'\\')\">' +e+'</div>';" +
"      });" +
"      html+='</div>';" +
"    }" +
"  }else if(currentTab==='clip'){" +
"    html='<div class=\"section-title\">Recent Clips</div>';" +
"    html+='<div style=\"padding:20px;text-align:center;color:rgba(255,255,255,0.5)\">No clipboard history yet.<br><br>Copy some text to see it here!</div>';" +
"  }" +
"  document.getElementById('content').innerHTML=html;" +
"}" +
"function toolClick(name){" +
"  NativeBridge.showToast(name+' clicked!');" +
"}" +
"function typeEmoji(emoji){" +
"  NativeBridge.typeText(emoji);" +
"  NativeBridge.showToast('Emoji: '+emoji);" +
"}" +
"function closeWindow(){" +
"  NativeBridge.close();" +
"}" +
"render();" +
"</script></body></html>";
    }
    
    // JavaScript Bridge
    public class WebBridge {
        @JavascriptInterface
        public void showToast(String msg) {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                Toast.makeText(FloatingWindowService.this, msg, Toast.LENGTH_SHORT).show();
            });
        }
        
        @JavascriptInterface
        public void typeText(String text) {
            // Send broadcast to keyboard to type this text
            Intent intent = new Intent("com.keybord.app.TYPE_TEXT");
            intent.putExtra("text", text);
            sendBroadcast(intent);
        }
        
        @JavascriptInterface
        public void close() {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null && windowManager != null) {
            try {
                windowManager.removeView(floatingView);
            } catch (Exception e) {}
            floatingView = null;
        }
    }
}