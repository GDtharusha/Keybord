package com.keybord.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        settings = new KeyboardSettings(this);
        
        getBridge().getWebView().addJavascriptInterface(new NativeSettingsBridge(), "NativeSettings");
        Log.d(TAG, "NativeSettings bridge added");
    }
    
    public class NativeSettingsBridge {
        
        @JavascriptInterface
        public void log(String message) {
            Log.d(TAG, "JS: " + message);
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
            return Settings.canDrawOverlays(MainActivity.this);
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
        public void saveSettingBool(String key, boolean value) {
            try {
                switch (key) {
                    case "vibration":
                        settings.setVibrationEnabled(value);
                        break;
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
                    case "emoji_row":
                        return settings.isShowEmojiRow();
                    default:
                        android.content.SharedPreferences prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE);
                        return prefs.getBoolean(key, defaultValue);
                }
            } catch (Exception e) {}
            return defaultValue;
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
                    case "key_gap":
                        settings.setKeyGap(value);
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
                    case "key_gap":
                        return settings.getKeyGap();
                    default:
                        android.content.SharedPreferences prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE);
                        return prefs.getInt(key, defaultValue);
                }
            } catch (Exception e) {}
            return defaultValue;
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
            } catch (Exception e) {}
            return defaultValue;
        }
        
        @JavascriptInterface
        public void setBackgroundColor(String color) {
            settings.setColorBackground(color);
            // Also update key colors based on background
            if (color.equals("#000000") || color.startsWith("#0") || color.startsWith("#1")) {
                // Dark background
                settings.setColorKey("#1a1a1a");
                settings.setColorKeySpecial("#0d0d0d");
                settings.setColorKeySpace("#1a1a1a");
            } else if (color.equals("#f5f5f5") || color.equals("#ffffff") || color.startsWith("#f") || color.startsWith("#e")) {
                // Light background
                settings.setColorKey("#ffffff");
                settings.setColorKeySpecial("#e0e0e0");
                settings.setColorKeySpace("#ffffff");
            } else {
                // Custom - slightly lighter keys
                settings.setColorKey(lightenColor(color, 0.1f));
                settings.setColorKeySpecial(darkenColor(color, 0.1f));
                settings.setColorKeySpace(lightenColor(color, 0.1f));
            }
            notifyKeyboard();
        }
        
        @JavascriptInterface
        public void setTextColor(String color) {
            settings.setColorText(color);
            notifyKeyboard();
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
        }

        @JavascriptInterface
        public void setKeyBackgroundColor(String color) {
            settings.setColorKey(color);
            // Also update special key colors
            try {
                int c = android.graphics.Color.parseColor(color);
                int r = android.graphics.Color.red(c);
                int g = android.graphics.Color.green(c);
                int b = android.graphics.Color.blue(c);
                // Darken for special keys
                String special = String.format("#%02x%02x%02x", 
                    Math.max(0, r - 30), Math.max(0, g - 30), Math.max(0, b - 30));
                settings.setColorKeySpecial(special);
                settings.setColorKeySpace(color);
            } catch (Exception e) {}
            notifyKeyboard();
        }

        @JavascriptInterface
        public void selectBackgroundImage() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    startActivityForResult(intent, 1001);
                }
            });
        }

        @JavascriptInterface
        public void clearBackgroundImage() {
            settings.setBackgroundImage("");
            notifyKeyboard();
        }
        
        private void notifyKeyboard() {
            try {
                Intent intent = new Intent(KeyboardSettings.ACTION_SETTINGS_CHANGED);
                intent.setPackage(getPackageName());
                sendBroadcast(intent);
            } catch (Exception e) {}
        }
        
        private String lightenColor(String color, float factor) {
            try {
                int c = android.graphics.Color.parseColor(color);
                int r = Math.min(255, (int)(android.graphics.Color.red(c) + 255 * factor));
                int g = Math.min(255, (int)(android.graphics.Color.green(c) + 255 * factor));
                int b = Math.min(255, (int)(android.graphics.Color.blue(c) + 255 * factor));
                return String.format("#%02x%02x%02x", r, g, b);
            } catch (Exception e) {
                return color;
            }
        }
        
        private String darkenColor(String color, float factor) {
            try {
                int c = android.graphics.Color.parseColor(color);
                int r = Math.max(0, (int)(android.graphics.Color.red(c) - 255 * factor));
                int g = Math.max(0, (int)(android.graphics.Color.green(c) - 255 * factor));
                int b = Math.max(0, (int)(android.graphics.Color.blue(c) - 255 * factor));
                return String.format("#%02x%02x%02x", r, g, b);
            } catch (Exception e) {
                return color;
            }
        }
    }
}