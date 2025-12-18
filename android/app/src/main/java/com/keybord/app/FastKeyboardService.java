package com.keybord.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FastKeyboardService extends InputMethodService {

    private static final String TAG = "FastKB";
    private static final int KEYBOARD_HEIGHT_DP = 300;
    
    private FrameLayout rootContainer;
    private LinearLayout nativeKeyboard;
    private WebView webView;
    private Handler handler;
    private Vibrator vibrator;
    
    private boolean isShift = false;
    private boolean isCaps = false;
    private boolean isNumbers = false;
    private boolean isSymbols = false;
    private boolean isRepeating = false;
    private Runnable repeatRunnable;
    
    private boolean webViewReady = false;
    private boolean webViewFailed = false;
    
    private static final String[][] LETTERS = {
        {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
        {"a", "s", "d", "f", "g", "h", "j", "k", "l"},
        {"SHIFT", "z", "x", "c", "v", "b", "n", "m", "DEL"},
        {"123", ",", "SPACE", ".", "ENTER"}
    };
    
    private static final String[][] NUMBERS = {
        {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
        {"@", "#", "$", "%", "&", "-", "+", "(", ")"},
        {"SYM", "*", "\"", "'", ":", ";", "!", "?", "DEL"},
        {"ABC", ",", "SPACE", ".", "ENTER"}
    };
    
    private static final String[][] SYMBOLS = {
        {"~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆"},
        {"£", "€", "¥", "^", "°", "=", "{", "}", "\\"},
        {"123", "©", "®", "™", "✓", "[", "]", "<", "DEL"},
        {"ABC", ",", "SPACE", ".", "ENTER"}
    };

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception e) {
            Log.e(TAG, "Vibrator error", e);
        }
    }

    @Override
    public View onCreateInputView() {
        rootContainer = new FrameLayout(this);
        rootContainer.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dp(KEYBOARD_HEIGHT_DP)
        ));
        rootContainer.setBackgroundColor(Color.parseColor("#1a1a2e"));
        
        nativeKeyboard = createNativeKeyboard();
        rootContainer.addView(nativeKeyboard);
        
        handler.postDelayed(() -> {
            if (!webViewFailed) {
                tryCreateWebView();
            }
        }, 200);
        
        return rootContainer;
    }
    
    private LinearLayout createNativeKeyboard() {
        LinearLayout kb = new LinearLayout(this);
        kb.setOrientation(LinearLayout.VERTICAL);
        kb.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
        kb.setBackgroundColor(Color.parseColor("#1a1a2e"));
        kb.setPadding(dp(4), dp(8), dp(4), dp(12));
        
        String[][] layout = getCurrentLayout();
        for (int i = 0; i < layout.length; i++) {
            kb.addView(createNativeRow(layout[i], i));
        }
        
        return kb;
    }
    
    private String[][] getCurrentLayout() {
        if (isSymbols) return SYMBOLS;
        if (isNumbers) return NUMBERS;
        return LETTERS;
    }
    
    private LinearLayout createNativeRow(String[] keys, int rowIndex) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ));
        row.setPadding(0, dp(4), 0, dp(4));
        
        if (rowIndex == 1) {
            row.setPadding(dp(20), dp(4), dp(20), dp(4));
        }
        
        for (String key : keys) {
            row.addView(createNativeKey(key));
        }
        
        return row;
    }
    
    private TextView createNativeKey(String key) {
        TextView tv = new TextView(this);
        tv.setText(getKeyDisplay(key));
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(getKeyColor(key));
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, getKeyTextSize(key));
        
        float weight = getKeyWeight(key);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight);
        params.setMargins(dp(3), 0, dp(3), 0);
        tv.setLayoutParams(params);
        
        tv.setBackground(createKeyBg(key));
        tv.setOnTouchListener((v, e) -> onKeyTouch(v, e, key));
        tv.setClickable(true);
        
        return tv;
    }
    
    private String getKeyDisplay(String key) {
        switch (key) {
            case "SHIFT": return (isShift || isCaps) ? "⬆" : "⇧";
            case "DEL": return "⌫";
            case "ENTER": return "↵";
            case "SPACE": return "space";
            case "123": return "123";
            case "ABC": return "ABC";
            case "SYM": return "#+=";
            default:
                if (key.length() == 1 && Character.isLetter(key.charAt(0))) {
                    return (isShift || isCaps) ? key.toUpperCase() : key;
                }
                return key;
        }
    }
    
    private int getKeyColor(String key) {
        switch (key) {
            case "ENTER": return Color.parseColor("#60a5fa");
            case "SHIFT":
                if (isCaps) return Color.parseColor("#34d399");
                if (isShift) return Color.parseColor("#60a5fa");
                return Color.parseColor("#9ca3af");
            case "DEL":
            case "123":
            case "ABC":
            case "SYM":
                return Color.parseColor("#9ca3af");
            case "SPACE":
                return Color.parseColor("#6b7280");
            default:
                return Color.WHITE;
        }
    }
    
    private float getKeyTextSize(String key) {
        if (key.equals("SHIFT") || key.equals("DEL") || key.equals("ENTER")) return 20;
        if (key.length() > 1) return 14;
        return 22;
    }
    
    private float getKeyWeight(String key) {
        switch (key) {
            case "SPACE": return 5f;
            case "SHIFT":
            case "DEL":
            case "ENTER":
            case "123":
            case "ABC":
            case "SYM":
                return 1.5f;
            default:
                return 1f;
        }
    }
    
    private StateListDrawable createKeyBg(String key) {
        StateListDrawable states = new StateListDrawable();
        
        int normalColor, pressedColor;
        switch (key) {
            case "ENTER":
                normalColor = Color.parseColor("#1d4ed8");
                pressedColor = Color.parseColor("#2563eb");
                break;
            case "SHIFT":
                if (isCaps) {
                    normalColor = Color.parseColor("#059669");
                    pressedColor = Color.parseColor("#10b981");
                } else if (isShift) {
                    normalColor = Color.parseColor("#1d4ed8");
                    pressedColor = Color.parseColor("#2563eb");
                } else {
                    normalColor = Color.parseColor("#252540");
                    pressedColor = Color.parseColor("#353560");
                }
                break;
            case "DEL":
            case "123":
            case "ABC":
            case "SYM":
                normalColor = Color.parseColor("#252540");
                pressedColor = Color.parseColor("#353560");
                break;
            case "SPACE":
                normalColor = Color.parseColor("#303050");
                pressedColor = Color.parseColor("#404070");
                break;
            default:
                normalColor = Color.parseColor("#3d3d5c");
                pressedColor = Color.parseColor("#5a5a8c");
        }
        
        GradientDrawable pressed = new GradientDrawable();
        pressed.setColor(pressedColor);
        pressed.setCornerRadius(dp(8));
        states.addState(new int[]{android.R.attr.state_pressed}, pressed);
        
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(normalColor);
        normal.setCornerRadius(dp(8));
        states.addState(new int[]{}, normal);
        
        return states;
    }
    
    private boolean onKeyTouch(View v, MotionEvent e, String key) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                v.setPressed(true);
                doVibrate();
                handleKey(key);
                if (key.equals("DEL")) startRepeat(key);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                v.setPressed(false);
                stopRepeat();
                return true;
        }
        return false;
    }
    
    private void startRepeat(String key) {
        isRepeating = true;
        repeatRunnable = () -> {
            if (isRepeating) {
                handleKey(key);
                doVibrate();
                handler.postDelayed(repeatRunnable, 50);
            }
        };
        handler.postDelayed(repeatRunnable, 400);
    }
    
    private void stopRepeat() {
        isRepeating = false;
        if (repeatRunnable != null) {
            handler.removeCallbacks(repeatRunnable);
        }
    }
    
    private void handleKey(String key) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        
        switch (key) {
            case "SHIFT":
                if (isCaps) { isCaps = false; isShift = false; }
                else if (isShift) { isCaps = true; }
                else { isShift = true; }
                refreshNativeKeyboard();
                break;
            case "DEL":
                ic.deleteSurroundingText(1, 0);
                break;
            case "ENTER":
                EditorInfo ei = getCurrentInputEditorInfo();
                if (ei != null) {
                    int action = ei.imeOptions & EditorInfo.IME_MASK_ACTION;
                    if (action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED) {
                        ic.commitText("\n", 1);
                    } else {
                        ic.performEditorAction(action);
                    }
                } else {
                    ic.commitText("\n", 1);
                }
                break;
            case "SPACE":
                ic.commitText(" ", 1);
                autoUnshift();
                break;
            case "123":
                isNumbers = true;
                isSymbols = false;
                refreshNativeKeyboard();
                break;
            case "ABC":
                isNumbers = false;
                isSymbols = false;
                refreshNativeKeyboard();
                break;
            case "SYM":
                isSymbols = true;
                isNumbers = false;
                refreshNativeKeyboard();
                break;
            default:
                String text = key;
                if ((isShift || isCaps) && key.length() == 1 && Character.isLetter(key.charAt(0))) {
                    text = key.toUpperCase();
                }
                ic.commitText(text, 1);
                autoUnshift();
        }
    }
    
    private void autoUnshift() {
        if (isShift && !isCaps) {
            isShift = false;
            refreshNativeKeyboard();
        }
    }
    
    private void refreshNativeKeyboard() {
        if (nativeKeyboard != null) {
            nativeKeyboard.removeAllViews();
            String[][] layout = getCurrentLayout();
            for (int i = 0; i < layout.length; i++) {
                nativeKeyboard.addView(createNativeRow(layout[i], i));
            }
        }
    }
    
    private void tryCreateWebView() {
        try {
            Context themedContext = new ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault);
            webView = new WebView(themedContext);
            
            webView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ));
            
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            settings.setSupportZoom(false);
            settings.setBuiltInZoomControls(false);
            
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            webView.setVerticalScrollBarEnabled(false);
            webView.setHorizontalScrollBarEnabled(false);
            webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            webView.setBackgroundColor(Color.TRANSPARENT);
            
            webView.addJavascriptInterface(new JSBridge(), "Native");
            
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    webViewReady = true;
                    handler.post(() -> {
                        if (nativeKeyboard != null) nativeKeyboard.setVisibility(View.GONE);
                        webView.setVisibility(View.VISIBLE);
                    });
                }
                
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    webViewFailed = true;
                    handler.post(() -> {
                        if (webView != null) webView.setVisibility(View.GONE);
                        if (nativeKeyboard != null) nativeKeyboard.setVisibility(View.VISIBLE);
                    });
                }
            });
            
            webView.setVisibility(View.INVISIBLE);
            rootContainer.addView(webView);
            
            String html = buildKeyboardHtml();
            webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null);
            
        } catch (Exception e) {
            webViewFailed = true;
        }
    }
    
    private String buildKeyboardHtml() {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'><style>*{margin:0;padding:0;box-sizing:border-box;-webkit-tap-highlight-color:transparent;user-select:none}html,body{width:100%;height:100%;overflow:hidden;background:#1a1a2e;font-family:system-ui,sans-serif}#kb{display:flex;flex-direction:column;height:100%;padding:8px 4px 12px}.r{display:flex;justify-content:center;gap:5px;flex:1;margin:4px 0}.r:nth-child(2){padding:0 5%}.k{display:flex;align-items:center;justify-content:center;flex:1;min-width:28px;max-width:42px;background:linear-gradient(180deg,#3d3d5c,#2a2a45);border:none;border-radius:8px;color:#fff;font-size:22px;font-weight:500;box-shadow:0 2px 0 rgba(0,0,0,.3)}.k:active{background:linear-gradient(180deg,#5a5a8c,#4a4a7a);transform:scale(.95)}.k.sp{background:linear-gradient(180deg,#252540,#1a1a30);color:#9ca3af;font-size:14px;max-width:56px;flex:1.5}.k.ent{background:linear-gradient(180deg,#2563eb,#1d4ed8);color:#fff}.k.spc{max-width:none;flex:5;font-size:12px;color:#6b7280}.k.sh.on{background:linear-gradient(180deg,#2563eb,#1d4ed8);color:#fff}.k.sh.caps{background:linear-gradient(180deg,#059669,#047857);color:#fff}</style></head><body><div id='kb'></div><script>var sh=false,caps=false,nums=false,syms=false;var L={l:[['q','w','e','r','t','y','u','i','o','p'],['a','s','d','f','g','h','j','k','l'],['SH','z','x','c','v','b','n','m','DEL'],['123',',','SPC','.','ENT']],n:[['1','2','3','4','5','6','7','8','9','0'],['@','#','$','%','&','-','+','(',')'],['SYM','*','\"',\"'\":',';','!','?','DEL'],['ABC',',','SPC','.','ENT']],s:[['~','`','|','•','√','π','÷','×','¶','∆'],['£','€','¥','^','°','=','{','}','\\\\'],['123','©','®','™','✓','[',']','<','DEL'],['ABC',',','SPC','.','ENT']]};function render(){var layout=syms?L.s:nums?L.n:L.l;var h='';layout.forEach(function(row){h+='<div class=\"r\">';row.forEach(function(k){var c='k',t=k;if(k=='SH'){c+=' sp sh';if(caps)c+=' caps';else if(sh)c+=' on';t=sh||caps?'⬆':'⇧';}else if(k=='DEL'){c+=' sp';t='⌫';}else if(k=='ENT'){c+=' sp ent';t='↵';}else if(k=='SPC'){c+=' spc';t='space';}else if(k=='123'||k=='ABC'||k=='SYM'){c+=' sp';}else if(k.length==1&&k.match(/[a-z]/)){t=(sh||caps)?k.toUpperCase():k;}h+='<div class=\"'+c+'\" data-k=\"'+k+'\">'+t+'</div>';});h+='</div>';});document.getElementById('kb').innerHTML=h;}function handle(k){Native.vibrate();if(k=='SH'){if(caps){caps=false;sh=false;}else if(sh){caps=true;}else{sh=true;}render();}else if(k=='DEL'){Native.del();}else if(k=='ENT'){Native.enter();}else if(k=='SPC'){Native.type(' ');if(sh&&!caps){sh=false;render();}}else if(k=='123'){nums=true;syms=false;render();}else if(k=='ABC'){nums=false;syms=false;render();}else if(k=='SYM'){syms=true;nums=false;render();}else{var t=k;if((sh||caps)&&k.match(/^[a-z]$/))t=k.toUpperCase();Native.type(t);if(sh&&!caps){sh=false;render();}}}document.addEventListener('DOMContentLoaded',function(){render();document.getElementById('kb').addEventListener('touchstart',function(e){e.preventDefault();var el=e.target.closest('.k');if(el)handle(el.dataset.k);},{passive:false});document.getElementById('kb').addEventListener('mousedown',function(e){var el=e.target.closest('.k');if(el)handle(el.dataset.k);});});</script></body></html>";
    }
    
    public class JSBridge {
        @JavascriptInterface
        public void type(String t) {
            handler.post(() -> {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null && t != null) ic.commitText(t, 1);
            });
        }
        
        @JavascriptInterface
        public void del() {
            handler.post(() -> {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.deleteSurroundingText(1, 0);
            });
        }
        
        @JavascriptInterface
        public void enter() {
            handler.post(() -> {
                InputConnection ic = getCurrentInputConnection();
                EditorInfo ei = getCurrentInputEditorInfo();
                if (ic != null) {
                    if (ei != null) {
                        int action = ei.imeOptions & EditorInfo.IME_MASK_ACTION;
                        if (action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED) {
                            ic.commitText("\n", 1);
                        } else {
                            ic.performEditorAction(action);
                        }
                    } else {
                        ic.commitText("\n", 1);
                    }
                }
            });
        }
        
        @JavascriptInterface
        public void vibrate() { doVibrate(); }
    }
    
    private void doVibrate() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(3, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(3);
                }
            }
        } catch (Exception e) {}
    }
    
    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
    
    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        isShift = false;
        isCaps = false;
        int inputClass = info.inputType & EditorInfo.TYPE_MASK_CLASS;
        isNumbers = (inputClass == EditorInfo.TYPE_CLASS_NUMBER || inputClass == EditorInfo.TYPE_CLASS_PHONE);
        isSymbols = false;
        refreshNativeKeyboard();
        
        if (webViewReady && webView != null) {
            String js = isNumbers ? "nums=true;syms=false;render();" : "nums=false;syms=false;render();";
            webView.evaluateJavascript(js, null);
        }
    }
    
    @Override
    public void onDestroy() {
        stopRepeat();
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}