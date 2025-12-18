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
import android.widget.Toast;

public class PopupActivity extends Activity {
    
    private static final String TAG = "PopupActivity";
    private WebView webView;
    private String mode = "tools";
    private KeyboardSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        
        settings = new KeyboardSettings(this);
        
        // Make it look like a popup
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        getWindow().setLayout(
            (int)(getResources().getDisplayMetrics().widthPixels * 0.94),
            (int)(getResources().getDisplayMetrics().heightPixels * 0.7)
        );
        getWindow().setGravity(Gravity.CENTER);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setDimAmount(0.6f);
        
        // Get mode from intent
        if (getIntent() != null && getIntent().hasExtra("mode")) {
            mode = getIntent().getStringExtra("mode");
        }
        
        // Create WebView
        webView = new WebView(this);
        webView.setBackgroundColor(Color.TRANSPARENT);
        
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        
        webView.addJavascriptInterface(new PopupBridge(), "Android");
        webView.setWebViewClient(new WebViewClient());
        
        FrameLayout container = new FrameLayout(this);
        container.setBackgroundColor(Color.TRANSPARENT);
        container.addView(webView);
        
        setContentView(container);
        
        // Load HTML content
        loadContent();
    }
    
    private void loadContent() {
        // Get current settings to pass to HTML
        String currentTheme = settings.getCurrentTheme();
        boolean vibrationEnabled = settings.isVibrationEnabled();
        int vibrationStrength = settings.getVibrationStrength();
        boolean soundEnabled = settings.isSoundEnabled();
        boolean emojiRowEnabled = settings.isShowEmojiRow();
        int keyboardHeight = settings.getKeyboardHeight();
        int keyRadius = settings.getKeyRadius();
        
        String html = buildHtml(currentTheme, vibrationEnabled, vibrationStrength, 
                               soundEnabled, emojiRowEnabled, keyboardHeight, keyRadius);
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }
    
    private String buildHtml(String currentTheme, boolean vibrationEnabled, 
                            int vibrationStrength, boolean soundEnabled,
                            boolean emojiRowEnabled, int keyboardHeight, int keyRadius) {
        
        // ════════════════════════════════════════════════════════════════
        // 🎨 මෙම HTML/CSS/JS ඔබට කැමති විදිහට modify කරන්න පුළුවන්!
        // ════════════════════════════════════════════════════════════════
        
        return "<!DOCTYPE html>\n" +
"<html><head>\n" +
"<meta charset='UTF-8'>\n" +
"<meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>\n" +
"<style>\n" +
"* { margin:0; padding:0; box-sizing:border-box; -webkit-tap-highlight-color:transparent; user-select:none; }\n" +
"body { font-family:system-ui,-apple-system,sans-serif; background:#1a1a2e; color:#fff; min-height:100vh; border-radius:24px; overflow:hidden; }\n" +
".header { display:flex; align-items:center; justify-content:space-between; padding:18px 20px; background:linear-gradient(135deg,#1e1e3f,#141428); border-bottom:1px solid rgba(255,255,255,0.08); }\n" +
".header h1 { font-size:18px; font-weight:700; display:flex; align-items:center; gap:10px; }\n" +
".close-btn { width:36px; height:36px; display:flex; align-items:center; justify-content:center; background:rgba(255,100,100,0.15); border-radius:50%; color:#ff6b6b; font-size:18px; cursor:pointer; transition:all 0.2s; }\n" +
".close-btn:active { transform:scale(0.9); background:rgba(255,100,100,0.3); }\n" +
".tabs { display:flex; gap:6px; padding:12px 16px; background:rgba(0,0,0,0.2); overflow-x:auto; }\n" +
".tab { padding:10px 16px; text-align:center; background:rgba(255,255,255,0.05); border-radius:12px; font-size:13px; font-weight:500; cursor:pointer; transition:all 0.2s; white-space:nowrap; }\n" +
".tab.active { background:linear-gradient(135deg,#3b82f6,#8b5cf6); color:#fff; }\n" +
".content { padding:16px; overflow-y:auto; max-height:calc(100vh - 140px); }\n" +
".section { margin-bottom:20px; }\n" +
".section-title { font-size:13px; font-weight:600; color:rgba(255,255,255,0.5); margin-bottom:12px; text-transform:uppercase; letter-spacing:0.5px; }\n" +
".themes-grid { display:grid; grid-template-columns:repeat(3,1fr); gap:12px; }\n" +
".theme-card { aspect-ratio:1; border-radius:16px; cursor:pointer; transition:all 0.2s; position:relative; overflow:hidden; }\n" +
".theme-card.active { box-shadow:0 0 0 3px #3b82f6; }\n" +
".theme-card:active { transform:scale(0.95); }\n" +
".theme-label { position:absolute; bottom:8px; left:8px; right:8px; font-size:11px; font-weight:600; text-align:center; background:rgba(0,0,0,0.5); padding:4px; border-radius:6px; }\n" +
".setting-row { display:flex; align-items:center; justify-content:space-between; padding:14px 16px; background:rgba(255,255,255,0.05); border-radius:14px; margin-bottom:10px; }\n" +
".setting-info { display:flex; flex-direction:column; gap:2px; }\n" +
".setting-label { font-size:15px; font-weight:500; }\n" +
".setting-desc { font-size:12px; color:rgba(255,255,255,0.4); }\n" +
".toggle { width:52px; height:30px; background:rgba(255,255,255,0.1); border-radius:15px; position:relative; cursor:pointer; transition:background 0.3s; }\n" +
".toggle.active { background:#3b82f6; }\n" +
".toggle::after { content:''; position:absolute; width:24px; height:24px; background:#fff; border-radius:50%; top:3px; left:3px; transition:transform 0.3s; }\n" +
".toggle.active::after { transform:translateX(22px); }\n" +
".slider-container { padding:14px 16px; background:rgba(255,255,255,0.05); border-radius:14px; margin-bottom:10px; }\n" +
".slider-header { display:flex; justify-content:space-between; margin-bottom:10px; }\n" +
".slider-label { font-size:15px; font-weight:500; }\n" +
".slider-value { font-size:14px; color:#3b82f6; font-weight:600; }\n" +
".slider { width:100%; height:6px; background:rgba(255,255,255,0.1); border-radius:3px; -webkit-appearance:none; appearance:none; }\n" +
".slider::-webkit-slider-thumb { -webkit-appearance:none; width:22px; height:22px; background:#3b82f6; border-radius:50%; cursor:pointer; }\n" +
".emoji-grid { display:grid; grid-template-columns:repeat(8,1fr); gap:4px; }\n" +
".emoji { font-size:26px; padding:10px 4px; text-align:center; cursor:pointer; border-radius:10px; transition:background 0.15s; }\n" +
".emoji:active { background:rgba(255,255,255,0.15); }\n" +
".btn-row { display:flex; gap:10px; margin-top:16px; }\n" +
".btn { flex:1; padding:14px; text-align:center; border-radius:12px; font-size:14px; font-weight:600; cursor:pointer; transition:all 0.2s; }\n" +
".btn-primary { background:linear-gradient(135deg,#3b82f6,#8b5cf6); color:#fff; }\n" +
".btn-secondary { background:rgba(255,255,255,0.1); color:#fff; }\n" +
".btn:active { transform:scale(0.97); opacity:0.9; }\n" +
".toast { position:fixed; bottom:20px; left:50%; transform:translateX(-50%); background:rgba(0,0,0,0.8); color:#fff; padding:12px 24px; border-radius:25px; font-size:14px; z-index:9999; animation:fadeIn 0.3s; }\n" +
"@keyframes fadeIn { from { opacity:0; transform:translateX(-50%) translateY(10px); } }\n" +
"</style></head><body>\n" +
"<div class='header'>\n" +
"  <h1>⚙️ <span id='title'>Settings</span></h1>\n" +
"  <div class='close-btn' onclick='closePopup()'>✕</div>\n" +
"</div>\n" +
"<div class='tabs'>\n" +
"  <div class='tab active' data-tab='themes' onclick='switchTab(this)'>🎨 Themes</div>\n" +
"  <div class='tab' data-tab='haptics' onclick='switchTab(this)'>📳 Haptics</div>\n" +
"  <div class='tab' data-tab='layout' onclick='switchTab(this)'>⌨️ Layout</div>\n" +
"  <div class='tab' data-tab='emoji' onclick='switchTab(this)'>😀 Emoji</div>\n" +
"  <div class='tab' data-tab='about' onclick='switchTab(this)'>ℹ️ About</div>\n" +
"</div>\n" +
"<div class='content' id='content'></div>\n" +
"\n" +
"<script>\n" +
"// ════════════════════════════════════════════════════════════════\n" +
"// Current Settings (loaded from Android)\n" +
"// ════════════════════════════════════════════════════════════════\n" +
"var currentSettings = {\n" +
"  theme: '" + currentTheme + "',\n" +
"  vibrationEnabled: " + vibrationEnabled + ",\n" +
"  vibrationStrength: " + vibrationStrength + ",\n" +
"  soundEnabled: " + soundEnabled + ",\n" +
"  emojiRowEnabled: " + emojiRowEnabled + ",\n" +
"  keyboardHeight: " + keyboardHeight + ",\n" +
"  keyRadius: " + keyRadius + "\n" +
"};\n" +
"\n" +
"var currentTab = 'themes';\n" +
"var titles = {\n" +
"  themes: '🎨 Theme Selection',\n" +
"  haptics: '📳 Haptic Feedback',\n" +
"  layout: '⌨️ Keyboard Layout',\n" +
"  emoji: '😀 Quick Emojis',\n" +
"  about: 'ℹ️ About KeyBord'\n" +
"};\n" +
"\n" +
"function switchTab(el) {\n" +
"  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));\n" +
"  el.classList.add('active');\n" +
"  currentTab = el.getAttribute('data-tab');\n" +
"  document.getElementById('title').textContent = titles[currentTab].split(' ').slice(1).join(' ');\n" +
"  render();\n" +
"}\n" +
"\n" +
"function render() {\n" +
"  var html = '';\n" +
"\n" +
"  if (currentTab === 'themes') {\n" +
"    html = '<div class=\"section\"><div class=\"section-title\">Select Theme</div><div class=\"themes-grid\">';\n" +
"    var themes = [\n" +
"      { id:'dark', name:'Dark', colors:['#1a1a2e','#3d3d5c','#2563eb'] },\n" +
"      { id:'light', name:'Light', colors:['#f0f0f5','#ffffff','#2563eb'] },\n" +
"      { id:'blue', name:'Ocean', colors:['#0f172a','#1e3a5f','#3b82f6'] },\n" +
"      { id:'green', name:'Forest', colors:['#0f1f0f','#1a3d1a','#10b981'] },\n" +
"      { id:'purple', name:'Purple', colors:['#1a0a2e','#3d2d5c','#8b5cf6'] },\n" +
"      { id:'red', name:'Cherry', colors:['#1f0f0f','#3d1a1a','#ef4444'] }\n" +
"    ];\n" +
"    themes.forEach(function(t) {\n" +
"      var active = currentSettings.theme === t.id ? ' active' : '';\n" +
"      var gradient = 'linear-gradient(135deg,' + t.colors[0] + ',' + t.colors[1] + ')';\n" +
"      html += '<div class=\"theme-card' + active + '\" style=\"background:' + gradient + ';border:2px solid ' + t.colors[2] + '\" onclick=\"selectTheme(\\'' + t.id + '\\')\">';\n" +
"      html += '<div class=\"theme-label\">' + t.name + '</div></div>';\n" +
"    });\n" +
"    html += '</div></div>';\n" +
"  }\n" +
"\n" +
"  else if (currentTab === 'haptics') {\n" +
"    html = '<div class=\"section\"><div class=\"section-title\">Feedback Settings</div>';\n" +
"    // Vibration toggle\n" +
"    html += '<div class=\"setting-row\">';\n" +
"    html += '<div class=\"setting-info\"><span class=\"setting-label\">Vibration</span><span class=\"setting-desc\">Vibrate on key press</span></div>';\n" +
"    html += '<div class=\"toggle' + (currentSettings.vibrationEnabled ? ' active' : '') + '\" onclick=\"toggleVibration()\"></div>';\n" +
"    html += '</div>';\n" +
"    // Vibration strength slider\n" +
"    if (currentSettings.vibrationEnabled) {\n" +
"      html += '<div class=\"slider-container\">';\n" +
"      html += '<div class=\"slider-header\"><span class=\"slider-label\">Vibration Strength</span><span class=\"slider-value\" id=\"vibStrength\">' + currentSettings.vibrationStrength + 'ms</span></div>';\n" +
"      html += '<input type=\"range\" class=\"slider\" min=\"1\" max=\"30\" value=\"' + currentSettings.vibrationStrength + '\" oninput=\"updateVibrationStrength(this.value)\">';\n" +
"      html += '</div>';\n" +
"    }\n" +
"    // Sound toggle\n" +
"    html += '<div class=\"setting-row\">';\n" +
"    html += '<div class=\"setting-info\"><span class=\"setting-label\">Key Sound</span><span class=\"setting-desc\">Play sound on key press</span></div>';\n" +
"    html += '<div class=\"toggle' + (currentSettings.soundEnabled ? ' active' : '') + '\" onclick=\"toggleSound()\"></div>';\n" +
"    html += '</div>';\n" +
"    html += '</div>';\n" +
"  }\n" +
"\n" +
"  else if (currentTab === 'layout') {\n" +
"    html = '<div class=\"section\"><div class=\"section-title\">Layout Options</div>';\n" +
"    // Emoji row toggle\n" +
"    html += '<div class=\"setting-row\">';\n" +
"    html += '<div class=\"setting-info\"><span class=\"setting-label\">Quick Emoji Row</span><span class=\"setting-desc\">Show emoji row above keyboard</span></div>';\n" +
"    html += '<div class=\"toggle' + (currentSettings.emojiRowEnabled ? ' active' : '') + '\" onclick=\"toggleEmojiRow()\"></div>';\n" +
"    html += '</div>';\n" +
"    // Keyboard height slider\n" +
"    html += '<div class=\"slider-container\">';\n" +
"    html += '<div class=\"slider-header\"><span class=\"slider-label\">Keyboard Height</span><span class=\"slider-value\" id=\"kbHeight\">' + currentSettings.keyboardHeight + 'dp</span></div>';\n" +
"    html += '<input type=\"range\" class=\"slider\" min=\"260\" max=\"400\" value=\"' + currentSettings.keyboardHeight + '\" oninput=\"updateKeyboardHeight(this.value)\">';\n" +
"    html += '</div>';\n" +
"    // Key radius slider\n" +
"    html += '<div class=\"slider-container\">';\n" +
"    html += '<div class=\"slider-header\"><span class=\"slider-label\">Key Roundness</span><span class=\"slider-value\" id=\"keyRad\">' + currentSettings.keyRadius + 'dp</span></div>';\n" +
"    html += '<input type=\"range\" class=\"slider\" min=\"0\" max=\"20\" value=\"' + currentSettings.keyRadius + '\" oninput=\"updateKeyRadius(this.value)\">';\n" +
"    html += '</div>';\n" +
"    html += '</div>';\n" +
"  }\n" +
"\n" +
"  else if (currentTab === 'emoji') {\n" +
"    html = '<div class=\"section\"><div class=\"section-title\">Frequently Used</div><div class=\"emoji-grid\">';\n" +
"    var emojis = ['😀','😃','😄','😁','😅','😂','🤣','😊','😇','🥰','😍','🤩','😘','😗','😚','😋','😛','😜','🤪','😝','🤗','🤭','🤫','🤔'];\n" +
"    emojis.forEach(function(e) {\n" +
"      html += '<div class=\"emoji\" onclick=\"typeEmoji(\\'' + e + '\\')\">' + e + '</div>';\n" +
"    });\n" +
"    html += '</div></div>';\n" +
"    html += '<div class=\"section\"><div class=\"section-title\">Gestures & Hands</div><div class=\"emoji-grid\">';\n" +
"    var gestures = ['👍','👎','👊','✊','🤛','🤜','👏','🙌','👐','🤲','🤝','🙏','✌️','🤞','🤟','🤘','👌','🤌','🤏','👈','👉','👆','👇','☝️'];\n" +
"    gestures.forEach(function(e) {\n" +
"      html += '<div class=\"emoji\" onclick=\"typeEmoji(\\'' + e + '\\')\">' + e + '</div>';\n" +
"    });\n" +
"    html += '</div></div>';\n" +
"    html += '<div class=\"section\"><div class=\"section-title\">Hearts & Love</div><div class=\"emoji-grid\">';\n" +
"    var hearts = ['❤️','🧡','💛','💚','💙','💜','🖤','🤍','🤎','💔','❣️','💕','💞','💓','💗','💖','💘','💝'];\n" +
"    hearts.forEach(function(e) {\n" +
"      html += '<div class=\"emoji\" onclick=\"typeEmoji(\\'' + e + '\\')\">' + e + '</div>';\n" +
"    });\n" +
"    html += '</div></div>';\n" +
"  }\n" +
"\n" +
"  else if (currentTab === 'about') {\n" +
"    html = '<div class=\"section\">';\n" +
"    html += '<div style=\"text-align:center;padding:30px 0;\">';\n" +
"    html += '<div style=\"font-size:64px;margin-bottom:16px;\">⌨️</div>';\n" +
"    html += '<h2 style=\"font-size:24px;margin-bottom:8px;\">KeyBord</h2>';\n" +
"    html += '<p style=\"color:rgba(255,255,255,0.5);margin-bottom:24px;\">Version 1.0.0</p>';\n" +
"    html += '<p style=\"color:rgba(255,255,255,0.6);line-height:1.6;\">A customizable keyboard built with Capacitor.<br>Settings sync in real-time! ⚡</p>';\n" +
"    html += '</div>';\n" +
"    html += '<div class=\"btn-row\">';\n" +
"    html += '<div class=\"btn btn-secondary\" onclick=\"resetSettings()\">🔄 Reset All</div>';\n" +
"    html += '</div>';\n" +
"    html += '</div>';\n" +
"  }\n" +
"\n" +
"  document.getElementById('content').innerHTML = html;\n" +
"}\n" +
"\n" +
"// ════════════════════════════════════════════════════════════════\n" +
"// SETTINGS FUNCTIONS - Save and notify keyboard\n" +
"// ════════════════════════════════════════════════════════════════\n" +
"\n" +
"function selectTheme(themeId) {\n" +
"  currentSettings.theme = themeId;\n" +
"  Android.applyTheme(themeId);\n" +
"  Android.notifySettingsChanged();\n" +
"  showToast('Theme: ' + themeId.charAt(0).toUpperCase() + themeId.slice(1));\n" +
"  render();\n" +
"}\n" +
"\n" +
"function toggleVibration() {\n" +
"  currentSettings.vibrationEnabled = !currentSettings.vibrationEnabled;\n" +
"  Android.setVibrationEnabled(currentSettings.vibrationEnabled);\n" +
"  Android.notifySettingsChanged();\n" +
"  showToast('Vibration: ' + (currentSettings.vibrationEnabled ? 'ON' : 'OFF'));\n" +
"  render();\n" +
"}\n" +
"\n" +
"function updateVibrationStrength(value) {\n" +
"  currentSettings.vibrationStrength = parseInt(value);\n" +
"  document.getElementById('vibStrength').textContent = value + 'ms';\n" +
"  Android.setVibrationStrength(parseInt(value));\n" +
"  Android.notifySettingsChanged();\n" +
"}\n" +
"\n" +
"function toggleSound() {\n" +
"  currentSettings.soundEnabled = !currentSettings.soundEnabled;\n" +
"  Android.setSoundEnabled(currentSettings.soundEnabled);\n" +
"  Android.notifySettingsChanged();\n" +
"  showToast('Key Sound: ' + (currentSettings.soundEnabled ? 'ON' : 'OFF'));\n" +
"  render();\n" +
"}\n" +
"\n" +
"function toggleEmojiRow() {\n" +
"  currentSettings.emojiRowEnabled = !currentSettings.emojiRowEnabled;\n" +
"  Android.setEmojiRowEnabled(currentSettings.emojiRowEnabled);\n" +
"  Android.notifySettingsChanged();\n" +
"  showToast('Emoji Row: ' + (currentSettings.emojiRowEnabled ? 'ON' : 'OFF'));\n" +
"  render();\n" +
"}\n" +
"\n" +
"function updateKeyboardHeight(value) {\n" +
"  currentSettings.keyboardHeight = parseInt(value);\n" +
"  document.getElementById('kbHeight').textContent = value + 'dp';\n" +
"  Android.setKeyboardHeight(parseInt(value));\n" +
"  Android.notifySettingsChanged();\n" +
"}\n" +
"\n" +
"function updateKeyRadius(value) {\n" +
"  currentSettings.keyRadius = parseInt(value);\n" +
"  document.getElementById('keyRad').textContent = value + 'dp';\n" +
"  Android.setKeyRadius(parseInt(value));\n" +
"  Android.notifySettingsChanged();\n" +
"}\n" +
"\n" +
"function typeEmoji(emoji) {\n" +
"  Android.typeText(emoji);\n" +
"  showToast('Sent: ' + emoji);\n" +
"}\n" +
"\n" +
"function resetSettings() {\n" +
"  if (confirm('Reset all settings to defaults?')) {\n" +
"    Android.resetSettings();\n" +
"    Android.notifySettingsChanged();\n" +
"    showToast('Settings Reset!');\n" +
"    closePopup();\n" +
"  }\n" +
"}\n" +
"\n" +
"function closePopup() {\n" +
"  Android.close();\n" +
"}\n" +
"\n" +
"function showToast(msg) {\n" +
"  var old = document.querySelector('.toast');\n" +
"  if (old) old.remove();\n" +
"  var toast = document.createElement('div');\n" +
"  toast.className = 'toast';\n" +
"  toast.textContent = msg;\n" +
"  document.body.appendChild(toast);\n" +
"  setTimeout(function() { toast.remove(); }, 2000);\n" +
"}\n" +
"\n" +
"// Initial render\n" +
"render();\n" +
"</script></body></html>";
    }
    
    // ════════════════════════════════════════════════════════════════
    // JAVASCRIPT INTERFACE - Popup -> Android -> Keyboard
    // ════════════════════════════════════════════════════════════════
    
    public class PopupBridge {
        
        @JavascriptInterface
        public void showToast(String msg) {
            runOnUiThread(() -> Toast.makeText(PopupActivity.this, msg, Toast.LENGTH_SHORT).show());
        }
        
        @JavascriptInterface
        public void typeText(String text) {
            Intent intent = new Intent(KeyboardSettings.ACTION_TYPE_TEXT);
            intent.setPackage(getPackageName());
            intent.putExtra("text", text);
            sendBroadcast(intent);
        }
        
        @JavascriptInterface
        public void close() {
            finish();
        }
        
        // ══════════════════════════════════════════════════════════
        // SETTINGS METHODS
        // ══════════════════════════════════════════════════════════
        
        @JavascriptInterface
        public void applyTheme(String themeName) {
            settings.applyTheme(themeName);
            Log.d(TAG, "Theme applied: " + themeName);
        }
        
        @JavascriptInterface
        public void setVibrationEnabled(boolean enabled) {
            settings.setVibrationEnabled(enabled);
            Log.d(TAG, "Vibration enabled: " + enabled);
        }
        
        @JavascriptInterface
        public void setVibrationStrength(int strength) {
            settings.setVibrationStrength(strength);
            Log.d(TAG, "Vibration strength: " + strength);
        }
        
        @JavascriptInterface
        public void setSoundEnabled(boolean enabled) {
            settings.setSoundEnabled(enabled);
            Log.d(TAG, "Sound enabled: " + enabled);
        }
        
        @JavascriptInterface
        public void setEmojiRowEnabled(boolean enabled) {
            settings.setShowEmojiRow(enabled);
            Log.d(TAG, "Emoji row enabled: " + enabled);
        }
        
        @JavascriptInterface
        public void setKeyboardHeight(int height) {
            settings.setKeyboardHeight(height);
            Log.d(TAG, "Keyboard height: " + height);
        }
        
        @JavascriptInterface
        public void setKeyRadius(int radius) {
            settings.setKeyRadius(radius);
            Log.d(TAG, "Key radius: " + radius);
        }
        
        @JavascriptInterface
        public void resetSettings() {
            settings.resetToDefaults();
            Log.d(TAG, "Settings reset to defaults");
        }
        
        @JavascriptInterface
        public void notifySettingsChanged() {
            // Send broadcast to keyboard service
            Intent intent = new Intent(KeyboardSettings.ACTION_SETTINGS_CHANGED);
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
            Log.d(TAG, "Settings changed broadcast sent");
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