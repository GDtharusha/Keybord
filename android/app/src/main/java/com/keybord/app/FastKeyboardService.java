package com.keybord.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FastKeyboardService extends InputMethodService {

    // ╔═══════════════════════════════════════════════════════════════════════════╗
    // ║  🎨 CUSTOMIZATION SECTION - මෙතනින් ඔබට කැමති විදිහට වෙනස් කරන්න!         ║
    // ╚═══════════════════════════════════════════════════════════════════════════╝
    
    // ─────────────────────────────────────────────────────────────────────────────
    // COLORS - වර්ණ වෙනස් කරන්න
    // ─────────────────────────────────────────────────────────────────────────────
    private static final String COLOR_BACKGROUND = "#1a1a2e";      // Keyboard background
    private static final String COLOR_KEY_NORMAL = "#3d3d5c";      // Normal key background
    private static final String COLOR_KEY_PRESSED = "#5a5a8c";     // Pressed key background
    private static final String COLOR_KEY_SPECIAL = "#252540";     // Special keys (shift, del, 123)
    private static final String COLOR_KEY_ENTER = "#2563eb";       // Enter key
    private static final String COLOR_KEY_SPACE = "#303050";       // Space bar
    private static final String COLOR_TEXT_NORMAL = "#FFFFFF";     // Normal text
    private static final String COLOR_TEXT_SPECIAL = "#9ca3af";    // Special key text
    private static final String COLOR_SHIFT_ACTIVE = "#3b82f6";    // Shift active
    private static final String COLOR_CAPS_ACTIVE = "#10b981";     // Caps lock active
    
    // ─────────────────────────────────────────────────────────────────────────────
    // SIZES - ප්‍රමාණ වෙනස් කරන්න (dp units)
    // ─────────────────────────────────────────────────────────────────────────────
    private static final int KEYBOARD_HEIGHT = 500;                // Keyboard height
    private static final int KEY_RADIUS = 10;                      // Key corner radius
    private static final int KEY_MARGIN = 3;                       // Space between keys
    private static final int KEY_TEXT_SIZE = 28;                   // Letter size
    private static final int KEY_SPECIAL_TEXT_SIZE = 16;           // Special key text size
    private static final int KEY_ICON_SIZE = 25;                   // Icon size
    private static final int PADDING_HORIZONTAL = 4;               // Left/right padding
    private static final int PADDING_TOP = 8;                     // Top padding
    private static final int PADDING_BOTTOM = 14;                  // Bottom padding
    private static final int ROW_SPACING = 5;                      // Space between rows
    
    // ─────────────────────────────────────────────────────────────────────────────
    // HAPTICS - Vibration settings
    // ─────────────────────────────────────────────────────────────────────────────
    private static final boolean VIBRATE_ENABLED = true;           // Vibration on/off
    private static final int VIBRATE_DURATION = 10;                 // Vibration duration (ms)
    
    // ─────────────────────────────────────────────────────────────────────────────
    // LONG PRESS - දිගු press settings
    // ─────────────────────────────────────────────────────────────────────────────
    private static final int LONG_PRESS_DELAY = 400;               // Long press delay (ms)
    private static final int REPEAT_INTERVAL = 50;                 // Key repeat speed (ms)
    
    // ─────────────────────────────────────────────────────────────────────────────
    // KEYBOARD LAYOUTS - Layouts වෙනස් කරන්න
    // ─────────────────────────────────────────────────────────────────────────────
    
    // Letters layout
    private static final String[][] LAYOUT_LETTERS = {
        {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
        {"a", "s", "d", "f", "g", "h", "j", "k", "l"},
        {"⇧", "z", "x", "c", "v", "b", "n", "m", "⌫"},
        {"123", ",", "SPACE", ".", "↵"}
    };
    
    // Numbers layout
    private static final String[][] LAYOUT_NUMBERS = {
        {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
        {"@", "#", "$", "%", "&", "-", "+", "(", ")"},
        {"#+=", "*", "\"", "'", ":", ";", "!", "?", "⌫"},
        {"ABC", ",", "SPACE", ".", "↵"}
    };
    
    // Symbols layout
    private static final String[][] LAYOUT_SYMBOLS = {
        {"~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆"},
        {"£", "€", "¥", "^", "°", "=", "{", "}", "\\"},
        {"123", "©", "®", "™", "✓", "[", "]", "<", "⌫"},
        {"ABC", ",", "SPACE", ".", "↵"}
    };
    
    // ╔═══════════════════════════════════════════════════════════════════════════╗
    // ║  🔧 INTERNAL CODE - මෙතනට පහළ කෝඩ් වෙනස් කරන්න ඕනේ නැහැ                  ║
    // ╚═══════════════════════════════════════════════════════════════════════════╝
    
    private LinearLayout keyboard;
    private Handler handler;
    private Vibrator vibrator;
    private boolean isShift = false;
    private boolean isCaps = false;
    private boolean isNumbers = false;
    private boolean isSymbols = false;
    private boolean isRepeating = false;
    private Runnable repeatRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception e) {}
    }

    @Override
    public View onCreateInputView() {
        keyboard = buildKeyboard();
        return keyboard;
    }
    
    private LinearLayout buildKeyboard() {
        LinearLayout kb = new LinearLayout(this);
        kb.setOrientation(LinearLayout.VERTICAL);
        kb.setBackgroundColor(Color.parseColor(COLOR_BACKGROUND));
        kb.setPadding(dp(PADDING_HORIZONTAL), dp(PADDING_TOP), dp(PADDING_HORIZONTAL), dp(PADDING_BOTTOM));
        kb.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, dp(KEYBOARD_HEIGHT)
        ));
        
        String[][] layout = getActiveLayout();
        for (int i = 0; i < layout.length; i++) {
            kb.addView(buildRow(layout[i], i));
        }
        
        return kb;
    }
    
    private String[][] getActiveLayout() {
        if (isSymbols) return LAYOUT_SYMBOLS;
        if (isNumbers) return LAYOUT_NUMBERS;
        return LAYOUT_LETTERS;
    }
    
    private LinearLayout buildRow(String[] keys, int rowIndex) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ));
        row.setPadding(0, dp(ROW_SPACING / 2), 0, dp(ROW_SPACING / 2));
        
        // Row 2 (A-L) gets extra side padding
        if (rowIndex == 1) {
            row.setPadding(dp(18), dp(ROW_SPACING / 2), dp(18), dp(ROW_SPACING / 2));
        }
        
        for (String key : keys) {
            row.addView(buildKey(key));
        }
        
        return row;
    }
    
    private View buildKey(String key) {
        TextView tv = new TextView(this);
        String display = getKeyDisplay(key);
        tv.setText(display);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        
        // Text color and size based on key type
        int textColor = Color.parseColor(isSpecialKey(key) ? COLOR_TEXT_SPECIAL : COLOR_TEXT_NORMAL);
        float textSize = isSpecialKey(key) ? KEY_SPECIAL_TEXT_SIZE : KEY_TEXT_SIZE;
        
        // Special cases
        if (key.equals("↵")) {
            textColor = Color.WHITE;
            textSize = KEY_ICON_SIZE;
        }
        if (key.equals("⇧")) {
            if (isCaps) textColor = Color.WHITE;
            else if (isShift) textColor = Color.WHITE;
            textSize = KEY_ICON_SIZE;
        }
        if (key.equals("⌫")) {
            textSize = KEY_ICON_SIZE;
        }
        
        tv.setTextColor(textColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        
        // Layout params with weight
        float weight = getKeyWeight(key);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.MATCH_PARENT, weight
        );
        params.setMargins(dp(KEY_MARGIN), 0, dp(KEY_MARGIN), 0);
        tv.setLayoutParams(params);
        
        // Background
        tv.setBackground(createKeyBackground(key));
        
        // Touch handler
        tv.setOnTouchListener((v, e) -> handleTouch(v, e, key));
        tv.setClickable(true);
        
        return tv;
    }
    
    private String getKeyDisplay(String key) {
        switch (key) {
            case "⇧": return (isShift || isCaps) ? "⬆" : "⇧";
            case "SPACE": return "space";
            default:
                if (key.length() == 1 && Character.isLetter(key.charAt(0))) {
                    return (isShift || isCaps) ? key.toUpperCase() : key.toLowerCase();
                }
                return key;
        }
    }
    
    private boolean isSpecialKey(String key) {
        return key.equals("⇧") || key.equals("⌫") || key.equals("↵") || 
               key.equals("SPACE") || key.equals("123") || key.equals("ABC") || key.equals("#+=");
    }
    
    private float getKeyWeight(String key) {
        switch (key) {
            case "SPACE": return 5f;
            case "⇧":
            case "⌫":
            case "↵":
            case "123":
            case "ABC":
            case "#+=":
                return 1.5f;
            default:
                return 1f;
        }
    }
    
    private GradientDrawable createKeyBackground(String key) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(KEY_RADIUS));
        
        String color;
        switch (key) {
            case "↵":
                color = COLOR_KEY_ENTER;
                break;
            case "⇧":
                if (isCaps) color = COLOR_CAPS_ACTIVE;
                else if (isShift) color = COLOR_SHIFT_ACTIVE;
                else color = COLOR_KEY_SPECIAL;
                break;
            case "⌫":
            case "123":
            case "ABC":
            case "#+=":
                color = COLOR_KEY_SPECIAL;
                break;
            case "SPACE":
                color = COLOR_KEY_SPACE;
                break;
            default:
                color = COLOR_KEY_NORMAL;
        }
        
        bg.setColor(Color.parseColor(color));
        return bg;
    }
    
    private boolean handleTouch(View v, MotionEvent e, String key) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                v.setAlpha(0.7f);
                v.setScaleX(0.95f);
                v.setScaleY(0.95f);
                doVibrate();
                processKey(key);
                if (key.equals("⌫")) startRepeat(key);
                return true;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                v.setAlpha(1f);
                v.setScaleX(1f);
                v.setScaleY(1f);
                stopRepeat();
                return true;
        }
        return false;
    }
    
    private void startRepeat(String key) {
        isRepeating = true;
        repeatRunnable = () -> {
            if (isRepeating) {
                processKey(key);
                doVibrate();
                handler.postDelayed(repeatRunnable, REPEAT_INTERVAL);
            }
        };
        handler.postDelayed(repeatRunnable, LONG_PRESS_DELAY);
    }
    
    private void stopRepeat() {
        isRepeating = false;
        if (repeatRunnable != null) handler.removeCallbacks(repeatRunnable);
    }
    
    private void processKey(String key) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        
        switch (key) {
            case "⇧":
                if (isCaps) { isCaps = false; isShift = false; }
                else if (isShift) { isCaps = true; }
                else { isShift = true; }
                refresh();
                break;
                
            case "⌫":
                ic.deleteSurroundingText(1, 0);
                break;
                
            case "↵":
                EditorInfo ei = getCurrentInputEditorInfo();
                if (ei != null) {
                    int action = ei.imeOptions & EditorInfo.IME_MASK_ACTION;
                    if (action == EditorInfo.IME_ACTION_NONE || 
                        action == EditorInfo.IME_ACTION_UNSPECIFIED) {
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
                refresh();
                break;
                
            case "ABC":
                isNumbers = false;
                isSymbols = false;
                refresh();
                break;
                
            case "#+=":
                isSymbols = true;
                isNumbers = false;
                refresh();
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
            refresh();
        }
    }
    
    private void refresh() {
        if (keyboard != null) {
            keyboard.removeAllViews();
            String[][] layout = getActiveLayout();
            for (int i = 0; i < layout.length; i++) {
                keyboard.addView(buildRow(layout[i], i));
            }
        }
    }
    
    private void doVibrate() {
        if (!VIBRATE_ENABLED || vibrator == null) return;
        try {
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(VIBRATE_DURATION, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(VIBRATE_DURATION);
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
        refresh();
    }
    
    @Override
    public void onDestroy() {
        stopRepeat();
        super.onDestroy();
    }
}