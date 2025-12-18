package com.keybord.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import com.getcapacitor.BridgeActivity;

import java.util.List;

public class MainActivity extends BridgeActivity {
    
    private static final String TAG = "MainActivity";
    private KeyboardSettings settings;
    private Handler handler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        settings = new KeyboardSettings(this);
        handler = new Handler(Looper.getMainLooper());
        
        // Add bridge with multiple retries
        for (int delay : new int[]{0, 50, 100, 200, 300, 500, 800, 1000, 1500, 2000}) {
            handler.postDelayed(this::addBridgeAndNotify, delay);
        }
    }
    
    private void addBridgeAndNotify() {
        try {
            if (getBridge() != null && getBridge().getWebView() != null) {
                WebView webView = getBridge().getWebView();
                
                // Add interface (safe to call multiple times)
                webView.removeJavascriptInterface("NativeSettings");
                webView.addJavascriptInterface(new NativeSettingsBridge(), "NativeSettings");
                
                // Notify JavaScript
                webView.evaluateJavascript(
                    "window.nativeBridgeReady = true; " +
                    "if(typeof onNativeBridgeReady === 'function') { onNativeBridgeReady(); }",
                    null
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Bridge add error", e);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        handler.postDelayed(this::addBridgeAndNotify, 100);
        handler.postDelayed(this::addBridgeAndNotify, 300);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        handler.postDelayed(this::addBridgeAndNotify, 100);
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // JAVASCRIPT INTERFACE
    // ════════════════════════════════════════════════════════════════════════════
    
    public class NativeSettingsBridge {
        
        @JavascriptInterface
        public boolean isAvailable() {
            return true;
        }
        
        @JavascriptInterface
        public String ping() {
            return "pong";
        }
        
        @JavascriptInterface
        public boolean isKeyboardEnabled() {
            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                List<InputMethodInfo> enabledMethods = imm.getEnabledInputMethodList();
                for (InputMethodInfo info : enabledMethods) {
                    if (info.getPackageName().equals(getPackageName())) {
                        return true;
                    }
                }
            } catch (Exception e) {}
            return false;
        }
        
        @JavascriptInterface
        public boolean isKeyboardActive() {
            try {
                String currentIME = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.DEFAULT_INPUT_METHOD
                );
                return currentIME != null && currentIME.contains(getPackageName());
            } catch (Exception e) {}
            return false;
        }
        
        @JavascriptInterface
        public boolean canDrawOverlays() {
            try {
                return Settings.canDrawOverlays(MainActivity.this);
            } catch (Exception e) {
                return false;
            }
        }
        
        @JavascriptInterface
        public void openKeyboardSettings() {
            try {
                Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {}
        }
        
        @JavascriptInterface
        public void showKeyboardPicker() {
            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.showInputMethodPicker();
            } catch (Exception e) {}
        }
        
        @JavascriptInterface
        public void requestOverlayPermission() {
            try {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                );
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {}
        }
        
        @JavascriptInterface
        public void saveSetting(String key, String value) {
            try {
                android.content.SharedPreferences prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE);
                prefs.edit().putString(key, value).apply();
                notifyKeyboard();
            } catch (Exception e) {}
        }
        
        @JavascriptInterface
        public String getSetting(String key, String defaultValue) {
            try {
                android.content.SharedPreferences prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE);
                return prefs.getString(key, defaultValue);
            } catch (Exception e) {
                return defaultValue;
            }
        }
        
        @JavascriptInterface
        public void saveSettingBool(String key, boolean value) {
            try {
                switch (key) {
                    case "vibration":
                        settings.setVibrationEnabled(value);
                        break;
                    case "sound":
                        settings.setSoundEnabled(value);
                        break;
                    case "emoji_bar":
                    case "emoji_row":
                        settings.setShowEmojiRow(value);
                        break;
                    default:
                        android.content.SharedPreferences prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE);
                        prefs.edit().putBoolean(key, value).apply();
                }
                notifyKeyboard();
            } catch (Exception e) {}
        }
        
        @JavascriptInterface
        public boolean getSettingBool(String key, boolean defaultValue) {
            try {
                switch (key) {
                    case "vibration":
                        return settings.isVibrationEnabled();
                    case "sound":
                        return settings.isSoundEnabled();
                    case "emoji_bar":
                    case "emoji_row":
                        return settings.isShowEmojiRow();
                    default:
                        android.content.SharedPreferences prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE);
                        return prefs.getBoolean(key, defaultValue);
                }
            } catch (Exception e) {
                return defaultValue;
            }
        }
        
        @JavascriptInterface
        public void saveSettingInt(String key, int value) {
            try {
                switch (key) {
                    case "vibration_strength":
                        settings.setVibrationStrength(value);
                        break;
                    case "keyboard_height":
                        settings.setKeyboardHeight(value);
                        break;
                    case "key_radius":
                        settings.setKeyRadius(value);
                        break;
                    case "key_text_size":
                        settings.setKeyTextSize(value);
                        break;
                    default:
                        android.content.SharedPreferences prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE);
                        prefs.edit().putInt(key, value).apply();
                }
                notifyKeyboard();
            } catch (Exception e) {}
        }
        
        @JavascriptInterface
        public int getSettingInt(String key, int defaultValue) {
            try {
                switch (key) {
                    case "vibration_strength":
                        return settings.getVibrationStrength();
                    case "keyboard_height":
                        return settings.getKeyboardHeight();
                    case "key_radius":
                        return settings.getKeyRadius();
                    case "key_text_size":
                        return settings.getKeyTextSize();
                    default:
                        android.content.SharedPreferences prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE);
                        return prefs.getInt(key, defaultValue);
                }
            } catch (Exception e) {
                return defaultValue;
            }
        }
        
        @JavascriptInterface
        public void applyTheme(String themeName) {
            settings.applyTheme(themeName);
            notifyKeyboard();
        }
        
        @JavascriptInterface
        public String getCurrentTheme() {
            return settings.getCurrentTheme();
        }
        
        @JavascriptInterface
        public void setColorBackground(String color) {
            settings.setColorBackground(color);
            notifyKeyboard();
        }
        
        @JavascriptInterface
        public void setColorAccent(String color) {
            settings.setColorKeyEnter(color);
            notifyKeyboard();
        }
        
        @JavascriptInterface
        public String getColorBackground() {
            return settings.getColorBackground();
        }
        
        @JavascriptInterface
        public String getColorAccent() {
            return settings.getColorKeyEnter();
        }
        
        @JavascriptInterface
        public void showFloatingWindow() {
            try {
                Intent intent = new Intent(MainActivity.this, PopupActivity.class);
                intent.putExtra("mode", "tools");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {}
        }
        
        @JavascriptInterface
        public void resetAllSettings() {
            settings.resetToDefaults();
            notifyKeyboard();
            showToastSafe("Settings reset!");
        }
        
        @JavascriptInterface
        public void showToast(String message) {
            showToastSafe(message);
        }
        
        @JavascriptInterface
        public void log(String message) {
            Log.d(TAG, "JS: " + message);
        }
        
        private void notifyKeyboard() {
            try {
                Intent intent = new Intent(KeyboardSettings.ACTION_SETTINGS_CHANGED);
                intent.setPackage(getPackageName());
                sendBroadcast(intent);
            } catch (Exception e) {}
        }
    }
    
    private void showToastSafe(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
}