package com.keybord.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class PopupActivity extends Activity {
    
    private static final String TAG = "PopupActivity";
    private WebView webView;
    private KeyboardAPI keyboardAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        getWindow().setLayout(
            (int)(getResources().getDisplayMetrics().widthPixels * 0.92),
            (int)(getResources().getDisplayMetrics().heightPixels * 0.55)
        );
        getWindow().setGravity(Gravity.CENTER);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setDimAmount(0.6f);
        
        webView = new WebView(this);
        webView.setBackgroundColor(Color.TRANSPARENT);
        
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        
        // Add both bridges
        keyboardAPI = new KeyboardAPI(this);
        webView.addJavascriptInterface(keyboardAPI, "Android");
        webView.addJavascriptInterface(new PopupBridge(), "PopupBridge");
        
        webView.setWebViewClient(new WebViewClient());
        
        FrameLayout container = new FrameLayout(this);
        container.setBackgroundColor(Color.TRANSPARENT);
        container.addView(webView);
        
        setContentView(container);
        webView.loadUrl("file:///android_asset/public/popup/popup.html");
    }
    
    public class PopupBridge {
        
        @JavascriptInterface
        public void close() {
            runOnUiThread(() -> finish());
        }
        
        @JavascriptInterface
        public void openMainApp() {
            try {
                Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                finish();
            } catch (Exception e) {
                Log.e(TAG, "Error opening main app", e);
            }
        }
        
        @JavascriptInterface
        public void log(String msg) {
            Log.d(TAG, "JS: " + msg);
        }
    }
    
    @Override
    public void onBackPressed() {
        finish();
    }
}