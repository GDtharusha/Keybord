package com.keybord.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.getcapacitor.BridgeActivity;

import java.util.List;

public class MainActivity extends BridgeActivity {
    
    private SharedPreferences prefs;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE);
        
        // Add JavaScript bridge
        getBridge().getWebView().addJavascriptInterface(new SettingsBridge(), "NativeSettings");
    }
    
    /**
     * Settings Bridge - HTML/JS වලින් keyboard settings control කරන්න
     */
    public class SettingsBridge {
        
        // ═══════════════════════════════════════════════════════════════
        // KEYBOARD SETUP
        // ═══════════════════════════════════════════════════════════════
        
        @JavascriptInterface
        public void openKeyboardSettings() {
            runOnUiThread(() -> {
                Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            });
        }
        
        @JavascriptInterface
        public void showKeyboardPicker() {
            runOnUiThread(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showInputMethodPicker();
                }
            });
        }
        
        @JavascriptInterface
        public boolean isKeyboardEnabled() {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm == null) return false;
            
            List<InputMethodInfo> enabledMethods = imm.getEnabledInputMethodList();
            String myPackage = getPackageName();
            
            for (InputMethodInfo info : enabledMethods) {
                if (info.getPackageName().equals(myPackage)) {
                    return true;
                }
            }
            return false;
        }
        
        @JavascriptInterface
        public boolean isKeyboardActive() {
            String currentIme = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD
            );
            return currentIme != null && currentIme.contains(getPackageName());
        }
        
        // ═══════════════════════════════════════════════════════════════
        // SETTINGS SAVE/LOAD
        // ═══════════════════════════════════════════════════════════════
        
        @JavascriptInterface
        public void saveSetting(String key, String value) {
            prefs.edit().putString(key, value).apply();
            notifyKeyboard();
        }
        
        @JavascriptInterface
        public void saveSettingBool(String key, boolean value) {
            prefs.edit().putBoolean(key, value).apply();
            notifyKeyboard();
        }
        
        @JavascriptInterface
        public void saveSettingInt(String key, int value) {
            prefs.edit().putInt(key, value).apply();
            notifyKeyboard();
        }
        
        @JavascriptInterface
        public String getSetting(String key, String defaultValue) {
            return prefs.getString(key, defaultValue);
        }
        
        @JavascriptInterface
        public boolean getSettingBool(String key, boolean defaultValue) {
            return prefs.getBoolean(key, defaultValue);
        }
        
        @JavascriptInterface
        public int getSettingInt(String key, int defaultValue) {
            return prefs.getInt(key, defaultValue);
        }
        
        private void notifyKeyboard() {
            Intent intent = new Intent("com.keybord.app.SETTINGS_UPDATED");
            sendBroadcast(intent);
        }
        
        // ═══════════════════════════════════════════════════════════════
        // FLOATING WINDOW
        // ═══════════════════════════════════════════════════════════════
        
        @JavascriptInterface
        public boolean canDrawOverlays() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Settings.canDrawOverlays(MainActivity.this);
            }
            return true;
        }
        
        @JavascriptInterface
        public void requestOverlayPermission() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(MainActivity.this)) {
                        Intent intent = new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())
                        );
                        startActivity(intent);
                    }
                }
            });
        }
        
        @JavascriptInterface
        public void showFloatingWindow() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(MainActivity.this)) {
                        Toast.makeText(MainActivity.this, 
                            "Please enable 'Display over other apps' permission first", 
                            Toast.LENGTH_LONG).show();
                        requestOverlayPermission();
                        return;
                    }
                }
                
                Intent intent = new Intent(MainActivity.this, FloatingWindowService.class);
                startService(intent);
            });
        }
        
        @JavascriptInterface
        public void hideFloatingWindow() {
            runOnUiThread(() -> {
                Intent intent = new Intent(MainActivity.this, FloatingWindowService.class);
                stopService(intent);
            });
        }
        
        // ═══════════════════════════════════════════════════════════════
        // UTILITY
        // ═══════════════════════════════════════════════════════════════
        
        @JavascriptInterface
        public void showToast(String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
        }
    }
}