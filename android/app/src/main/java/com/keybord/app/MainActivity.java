package com.keybord.app;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
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
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        settings = new KeyboardSettings(this);
        
        // Add JavaScript interface after bridge is ready
        getBridge().getWebView().post(() -> {
            WebView webView = getBridge().getWebView();
            webView.addJavascriptInterface(new NativeSettingsBridge(), "NativeSettings");
            Log.d(TAG, "NativeSettings bridge added");
        });
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // JAVASCRIPT INTERFACE - index.html එකෙන් call කරන්න පුළුවන්
    // ════════════════════════════════════════════════════════════════════════════
    
    public class NativeSettingsBridge {
        
        // ──────────────────────────────────────────────────────────────────────
        // KEYBOARD STATUS METHODS
        // ──────────────────────────────────────────────────────────────────────
        
        @JavascriptInterface
        public boolean isKeyboardEnabled() {
            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                List<InputMethodInfo> enabledMethods = imm.getEnabledInputMethodList();
                for (InputMethodInfo info : enabledMethods) {
                    if (info.getPackageName().equals(getPackageName())) {
                        Log.d(TAG, "Keyboard is ENABLED");
                        return true;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking keyboard enabled", e);
            }
            Log.d(TAG, "Keyboard is NOT enabled");
            return false;
        }
        
        @JavascriptInterface
        public boolean isKeyboardActive() {
            try {
                String currentIME = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.DEFAULT_INPUT_METHOD
                );
                boolean isActive = currentIME != null && currentIME.contains(getPackageName());
                Log.d(TAG, "Keyboard active: " + isActive + " (current: " + currentIME + ")");
                return isActive;
            } catch (Exception e) {
                Log.e(TAG, "Error checking keyboard active", e);
            }
            return false;
        }
        
        @JavascriptInterface
        public boolean canDrawOverlays() {
            boolean can = Settings.canDrawOverlays(MainActivity.this);
            Log.d(TAG, "Can draw overlays: " + can);
            return can;
        }
        
        // ──────────────────────────────────────────────────────────────────────
        // KEYBOARD SETUP METHODS
        // ──────────────────────────────────────────────────────────────────────
        
        @JavascriptInterface
        public void openKeyboardSettings() {
            Log.d(TAG, "Opening keyboard settings");
            try {
                Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error opening keyboard settings", e);
                showToastSafe("Cannot open settings");
            }
        }
        
        @JavascriptInterface
        public void showKeyboardPicker() {
            Log.d(TAG, "Showing keyboard picker");
            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.showInputMethodPicker();
            } catch (Exception e) {
                Log.e(TAG, "Error showing keyboard picker", e);
                showToastSafe("Cannot show keyboard picker");
            }
        }
        
        @JavascriptInterface
        public void requestOverlayPermission() {
            Log.d(TAG, "Requesting overlay permission");
            try {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                );
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error requesting overlay permission", e);
                showToastSafe("Cannot open permission settings");
            }
        }
        
        // ──────────────────────────────────────────────────────────────────────
        // SETTINGS SAVE/LOAD METHODS
        // ──────────────────────────────────────────────────────────────────────
        
        @JavascriptInterface
        public void saveSetting(String key, String value) {
            Log.d(TAG, "saveSetting: " + key + " = " + value);
            try {
                android.content.SharedPreferences prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE);
                prefs.edit().putString(key, value).apply();
                notifyKeyboard();
            } catch (Exception e) {
                Log.e(TAG, "Error saving setting", e);
            }
        }
        
        @JavascriptInterface
        public String getSetting(String key, String defaultValue) {
            try {
                android.content.SharedPreferences prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE);
                return prefs.getString(key, defaultValue);
            } catch (Exception e) {
                Log.e(TAG, "Error getting setting", e);
                return defaultValue;
            }
        }
        
        @JavascriptInterface
        public void saveSettingBool(String key, boolean value) {
            Log.d(TAG, "saveSettingBool: " + key + " = " + value);
            try {
                // Map UI setting names to KeyboardSettings names
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
                    case "toolbar":
                        // Save as generic setting
                        android.content.SharedPreferences prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE);
                        prefs.edit().putBoolean(key, value).apply();
                        break;
                    default:
                        android.content.SharedPreferences prefs2 = getSharedPreferences("keyboard_prefs", MODE_PRIVATE);
                        prefs2.edit().putBoolean(key, value).apply();
                }
                notifyKeyboard();
            } catch (Exception e) {
                Log.e(TAG, "Error saving bool setting", e);
            }
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
                Log.e(TAG, "Error getting bool setting", e);
                return defaultValue;
            }
        }
        
        @JavascriptInterface
        public void saveSettingInt(String key, int value) {
            Log.d(TAG, "saveSettingInt: " + key + " = " + value);
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
            } catch (Exception e) {
                Log.e(TAG, "Error saving int setting", e);
            }
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
                Log.e(TAG, "Error getting int setting", e);
                return defaultValue;
            }
        }
        
        // ──────────────────────────────────────────────────────────────────────
        // THEME METHODS
        // ──────────────────────────────────────────────────────────────────────
        
        @JavascriptInterface
        public void applyTheme(String themeName) {
            Log.d(TAG, "Applying theme: " + themeName);
            settings.applyTheme(themeName);
            notifyKeyboard();
        }
        
        @JavascriptInterface
        public String getCurrentTheme() {
            return settings.getCurrentTheme();
        }
        
        @JavascriptInterface
        public void setColorBackground(String color) {
            Log.d(TAG, "Setting background color: " + color);
            settings.setColorBackground(color);
            notifyKeyboard();
        }
        
        @JavascriptInterface
        public void setColorAccent(String color) {
            Log.d(TAG, "Setting accent color: " + color);
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
        
        // ──────────────────────────────────────────────────────────────────────
        // UTILITY METHODS
        // ──────────────────────────────────────────────────────────────────────
        
        @JavascriptInterface
        public void showFloatingWindow() {
            Log.d(TAG, "Showing floating window");
            try {
                Intent intent = new Intent(MainActivity.this, PopupActivity.class);
                intent.putExtra("mode", "tools");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error showing floating window", e);
                showToastSafe("Cannot open floating window");
            }
        }
        
        @JavascriptInterface
        public void resetAllSettings() {
            Log.d(TAG, "Resetting all settings");
            settings.resetToDefaults();
            notifyKeyboard();
            showToastSafe("Settings reset to defaults");
        }
        
        @JavascriptInterface
        public void showToast(String message) {
            showToastSafe(message);
        }
        
        @JavascriptInterface
        public void log(String message) {
            Log.d(TAG, "JS: " + message);
        }
        
        // ──────────────────────────────────────────────────────────────────────
        // NOTIFY KEYBOARD SERVICE
        // ──────────────────────────────────────────────────────────────────────
        
        private void notifyKeyboard() {
            try {
                Intent intent = new Intent(KeyboardSettings.ACTION_SETTINGS_CHANGED);
                intent.setPackage(getPackageName());
                sendBroadcast(intent);
                Log.d(TAG, "Settings change broadcast sent");
            } catch (Exception e) {
                Log.e(TAG, "Error sending broadcast", e);
            }
        }
    }
    
    private void showToastSafe(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
}