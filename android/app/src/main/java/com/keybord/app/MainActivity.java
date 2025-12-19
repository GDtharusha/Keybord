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
        
        // Add JavaScript interface BEFORE page loads
        getBridge().getWebView().addJavascriptInterface(new NativeSettingsBridge(), "NativeSettings");
        Log.d(TAG, "NativeSettings bridge added in onCreate");
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
            } catch (Exception e) {
                Log.e(TAG, "Error checking keyboard enabled", e);
            }
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
            } catch (Exception e) {
                Log.e(TAG, "Error checking keyboard active", e);
            }
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
            } catch (Exception e) {
                Log.e(TAG, "Error opening keyboard settings", e);
            }
        }
        
        @JavascriptInterface
        public void showKeyboardPicker() {
            try {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.showInputMethodPicker();
            } catch (Exception e) {
                Log.e(TAG, "Error showing keyboard picker", e);
            }
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
            } catch (Exception e) {
                Log.e(TAG, "Error requesting overlay permission", e);
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
                    case "emoji_row":
                        settings.setShowEmojiRow(value);
                        break;
                    default:
                        android.content.SharedPreferences prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE);
                        prefs.edit().putBoolean(key, value).apply();
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
        public void setColorAccent(String color) {
            settings.setColorKeyEnter(color);
            notifyKeyboard();
        }
        
        @JavascriptInterface
        public void showFloatingWindow() {
            try {
                Intent intent = new Intent(MainActivity.this, PopupActivity.class);
                intent.putExtra("mode", "tools");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error showing floating window", e);
            }
        }
        
        @JavascriptInterface
        public void resetAllSettings() {
            settings.resetToDefaults();
            notifyKeyboard();
        }
        
        @JavascriptInterface
        public void showToast(String message) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        private void notifyKeyboard() {
            try {
                Intent intent = new Intent(KeyboardSettings.ACTION_SETTINGS_CHANGED);
                intent.setPackage(getPackageName());
                sendBroadcast(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error sending broadcast", e);
            }
        }
    }
}