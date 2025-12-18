package com.keybord.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

public class PopupActivity extends Activity {

    private WebView webView;
    private String mode = "tools";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make it look like a popup
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        getWindow().setLayout(
            (int)(getResources().getDisplayMetrics().widthPixels * 0.92),
            (int)(getResources().getDisplayMetrics().heightPixels * 0.6)
        );
        getWindow().setGravity(Gravity.CENTER);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setDimAmount(0.5f);
        
        // Get mode from intent
        if (getIntent() != null && getIntent().hasExtra("mode")) {
            mode = getIntent().getStringExtra("mode");
        }
        
        // Create WebView - THIS WORKS in Activity context!
        webView = new WebView(this);
        webView.setBackgroundColor(Color.TRANSPARENT);
        
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        
        webView.addJavascriptInterface(new PopupBridge(), "PopupBridge");
        webView.setWebViewClient(new WebViewClient());
        
        FrameLayout container = new FrameLayout(this);
        container.setBackgroundColor(Color.TRANSPARENT);
        container.addView(webView);
        
        setContentView(container);
        
        // Load HTML content
        loadContent();
    }
    
    private void loadContent() {
        String html = buildHtml();
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }
    
    private String buildHtml() {
        // ════════════════════════════════════════════════════════════════
        // 🎨 මෙම HTML/CSS/JS ඔබට කැමති විදිහට modify කරන්න පුළුවන්!
        // ════════════════════════════════════════════════════════════════
        return "<!DOCTYPE html>" +
"<html><head>" +
"<meta charset='UTF-8'>" +
"<meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>" +
"<style>" +
"* { margin:0; padding:0; box-sizing:border-box; -webkit-tap-highlight-color:transparent; user-select:none; }" +
"body { font-family:system-ui,sans-serif; background:#1a1a2e; color:#fff; min-height:100vh; border-radius:20px; overflow:hidden; }" +
".header { display:flex; align-items:center; justify-content:space-between; padding:16px 18px; background:#141424; border-bottom:1px solid rgba(255,255,255,0.1); }" +
".header h1 { font-size:17px; font-weight:600; }" +
".close-btn { width:34px; height:34px; display:flex; align-items:center; justify-content:center; background:rgba(255,100,100,0.15); border-radius:50%; color:#ff6b6b; font-size:18px; cursor:pointer; }" +
".tabs { display:flex; gap:8px; padding:12px; background:#12121f; }" +
".tab { flex:1; padding:10px; text-align:center; background:rgba(255,255,255,0.05); border-radius:10px; font-size:13px; cursor:pointer; transition:all 0.2s; }" +
".tab.active { background:linear-gradient(135deg,#3b82f6,#8b5cf6); color:#fff; }" +
".content { padding:14px; overflow-y:auto; max-height:calc(100vh - 130px); }" +
".grid { display:grid; grid-template-columns:repeat(4,1fr); gap:10px; }" +
".item { display:flex; flex-direction:column; align-items:center; padding:14px 8px; background:rgba(255,255,255,0.05); border-radius:14px; cursor:pointer; transition:all 0.15s; }" +
".item:active { transform:scale(0.95); background:rgba(255,255,255,0.1); }" +
".item .icon { font-size:28px; margin-bottom:6px; }" +
".item .label { font-size:11px; color:rgba(255,255,255,0.7); }" +
".emoji-grid { display:grid; grid-template-columns:repeat(8,1fr); gap:2px; }" +
".emoji { font-size:26px; padding:10px 6px; text-align:center; cursor:pointer; border-radius:10px; transition:background 0.1s; }" +
".emoji:active { background:rgba(255,255,255,0.15); }" +
".section { margin-bottom:16px; }" +
".section-title { font-size:12px; color:rgba(255,255,255,0.4); margin-bottom:10px; padding-left:4px; }" +
".clip-item { background:rgba(255,255,255,0.05); padding:12px 14px; border-radius:10px; margin-bottom:8px; font-size:14px; cursor:pointer; }" +
".clip-item:active { background:rgba(255,255,255,0.1); }" +
".empty { text-align:center; padding:40px 20px; color:rgba(255,255,255,0.4); }" +
"</style></head><body>" +
"<div class='header'>" +
"  <h1 id='title'>✨ Quick Tools</h1>" +
"  <div class='close-btn' onclick='closePopup()'>✕</div>" +
"</div>" +
"<div class='tabs'>" +
"  <div class='tab active' data-tab='tools' onclick='switchTab(this)'>🔧 Tools</div>" +
"  <div class='tab' data-tab='emoji' onclick='switchTab(this)'>😀 Emoji</div>" +
"  <div class='tab' data-tab='clips' onclick='switchTab(this)'>📋 Clips</div>" +
"  <div class='tab' data-tab='ai' onclick='switchTab(this)'>🤖 AI</div>" +
"</div>" +
"<div class='content' id='content'></div>" +
"<script>" +
"var currentTab = 'tools';" +
"" +
"function switchTab(el) {" +
"  document.querySelectorAll('.tab').forEach(function(t) { t.classList.remove('active'); });" +
"  el.classList.add('active');" +
"  currentTab = el.getAttribute('data-tab');" +
"  render();" +
"}" +
"" +
"function render() {" +
"  var html = '';" +
"  var titles = { tools:'✨ Quick Tools', emoji:'😀 Emoji', clips:'📋 Clipboard', ai:'🤖 AI Assistant' };" +
"  document.getElementById('title').textContent = titles[currentTab] || 'Tools';" +
"" +
"  if (currentTab === 'tools') {" +
"    html = '<div class=\"grid\">';" +
"    var tools = [" +
"      ['🎨','Themes'],['🔤','Fonts'],['📐','Size'],['🌐','Language']," +
"      ['🔊','Sound'],['📳','Vibrate'],['⌨️','Layout'],['☁️','Sync']," +
"      ['🌙','Dark'],['📱','Float'],['🔍','Search'],['⚙️','Settings']" +
"    ];" +
"    tools.forEach(function(t) {" +
"      html += '<div class=\"item\" onclick=\"toolClick(\\'' + t[1] + '\\')\"><span class=\"icon\">' + t[0] + '</span><span class=\"label\">' + t[1] + '</span></div>';" +
"    });" +
"    html += '</div>';" +
"  }" +
"" +
"  else if (currentTab === 'emoji') {" +
"    var cats = {" +
"      'Smileys': ['😀','😃','😄','😁','😅','😂','🤣','😊','😇','🥰','😍','🤩','😘','😗','😚','😋','😛','😜','🤪','😝','🤑','🤗','🤭','🤫','🤔','🤐','🤨','😐','😑','😶']," +
"      'Gestures': ['👍','👎','👊','✊','🤛','🤜','👏','🙌','👐','🤲','🤝','🙏','✌️','🤞','🤟','🤘','👌','🤌','🤏','👈','👉','👆','👇','☝️','✋','🤚','🖐️','🖖','👋','🤙']," +
"      'Hearts': ['❤️','🧡','💛','💚','💙','💜','🖤','🤍','🤎','💔','❣️','💕','💞','💓','💗','💖','💘','💝']," +
"      'Animals': ['🐶','🐱','🐭','🐹','🐰','🦊','🐻','🐼','🐨','🐯','🦁','🐮','🐷','🐸','🐵','🐔','🐧','🐦','🐤','🦆','🦅','🦉','🦇','🐺','🐗','🐴']" +
"    };" +
"    for (var cat in cats) {" +
"      html += '<div class=\"section\"><div class=\"section-title\">' + cat + '</div><div class=\"emoji-grid\">';" +
"      cats[cat].forEach(function(e) {" +
"        html += '<div class=\"emoji\" onclick=\"typeEmoji(\\'' + e + '\\')\">' + e + '</div>';" +
"      });" +
"      html += '</div></div>';" +
"    }" +
"  }" +
"" +
"  else if (currentTab === 'clips') {" +
"    html = '<div class=\"section\"><div class=\"section-title\">Recent</div>';" +
"    var clips = ['Hello World!', 'https://example.com', 'Lorem ipsum dolor sit amet...'];" +
"    clips.forEach(function(c, i) {" +
"      html += '<div class=\"clip-item\" onclick=\"typeText(\\'' + c.replace(/'/g, \"\\\\'\") + '\\')\">' + c + '</div>';" +
"    });" +
"    html += '</div>';" +
"    html += '<div class=\"empty\">💡 Tip: Copy text to see it here</div>';" +
"  }" +
"" +
"  else if (currentTab === 'ai') {" +
"    html = '<div class=\"section\">';" +
"    html += '<div class=\"grid\">';" +
"    var aiTools = [" +
"      ['✏️','Rewrite'],['📝','Summarize'],['🌐','Translate'],['✅','Fix Grammar']," +
"      ['💼','Formal'],['😊','Casual'],['📧','Email'],['💬','Reply']" +
"    ];" +
"    aiTools.forEach(function(t) {" +
"      html += '<div class=\"item\" onclick=\"aiAction(\\'' + t[1] + '\\')\"><span class=\"icon\">' + t[0] + '</span><span class=\"label\">' + t[1] + '</span></div>';" +
"    });" +
"    html += '</div></div>';" +
"    html += '<div class=\"empty\" style=\"padding:20px\">🤖 Select text in any app, then use AI tools to transform it!</div>';" +
"  }" +
"" +
"  document.getElementById('content').innerHTML = html;" +
"}" +
"" +
"function toolClick(name) {" +
"  PopupBridge.showToast(name + ' clicked!');" +
"}" +
"" +
"function typeEmoji(emoji) {" +
"  PopupBridge.typeText(emoji);" +
"  PopupBridge.showToast('Sent: ' + emoji);" +
"}" +
"" +
"function typeText(text) {" +
"  PopupBridge.typeText(text);" +
"  PopupBridge.showToast('Pasted!');" +
"}" +
"" +
"function aiAction(action) {" +
"  PopupBridge.showToast('AI ' + action + ' - Coming Soon!');" +
"}" +
"" +
"function closePopup() {" +
"  PopupBridge.close();" +
"}" +
"" +
"render();" +
"</script></body></html>";
    }
    
    // JavaScript Bridge
    public class PopupBridge {
        @JavascriptInterface
        public void showToast(String msg) {
            runOnUiThread(() -> Toast.makeText(PopupActivity.this, msg, Toast.LENGTH_SHORT).show());
        }
        
        @JavascriptInterface
        public void typeText(String text) {
            // Send to keyboard
            android.content.Intent intent = new android.content.Intent("com.keybord.app.TYPE_TEXT");
            intent.putExtra("text", text);
            sendBroadcast(intent);
        }
        
        @JavascriptInterface
        public void close() {
            finish();
        }
        
        @JavascriptInterface
        public void log(String msg) {
            android.util.Log.d("PopupActivity", msg);
        }
    }
    
    @Override
    public void onBackPressed() {
        finish();
    }
}