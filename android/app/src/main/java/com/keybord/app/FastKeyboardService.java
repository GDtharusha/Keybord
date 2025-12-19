package com.keybord.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class FastKeyboardService extends InputMethodService {
    
    private static final String TAG = "FastKeyboardService";
    
    // Settings
    private String colorBackground = "#000000";
    private String colorKeyNormal = "#1a1a1a";
    private String colorKeySpecial = "#0d0d0d";
    private String colorKeyEnter = "#2563eb";
    private String colorKeySpace = "#1a1a1a";
    private String colorText = "#ffffff";
    
    private int keyboardHeight = 245;
    private int keyRadius = 8;
    private int keyGap = 2;
    private int keyTextSize = 20;
    
    private boolean vibrateEnabled = true;
    private int vibrateDuration = 5;
    private boolean showEmojiRow = false;
    private int longPressDelay = 350;
    private int repeatInterval = 35;
    
    // Layouts
    private static final String[][] LAYOUT_LETTERS = {
        {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
        {"a", "s", "d", "f", "g", "h", "j", "k", "l"},
        {"⇧", "z", "x", "c", "v", "b", "n", "m", "⌫"},
        {"123", "🌐", ",", "SPACE", ".", "✨", "↵"}
    };
    
    private static final String[][] LAYOUT_NUMBERS = {
        {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
        {"@", "#", "$", "%", "&", "-", "+", "(", ")"},
        {"#+=", "*", "\"", "'", ":", ";", "!", "?", "⌫"},
        {"ABC", "🌐", ",", "SPACE", ".", "✨", "↵"}
    };
    
    private static final String[][] LAYOUT_SYMBOLS = {
        {"~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆"},
        {"£", "€", "¥", "^", "°", "=", "{", "}", "\\"},
        {"123", "©", "®", "™", "✓", "[", "]", "<", "⌫"},
        {"ABC", "🌐", ",", "SPACE", ".", "✨", "↵"}
    };
    
    // Views and state
    private FrameLayout rootContainer;
    private LinearLayout keyboardView;
    private FrameLayout touchLayer;
    private Handler handler;
    private Vibrator vibrator;
    private ToneGenerator toneGenerator;
    private KeyboardSettings settings;
    
    private boolean isShift = false;
    private boolean isCaps = false;
    private boolean isNumbers = false;
    private boolean isSymbols = false;
    private boolean isRepeating = false;
    private Runnable repeatRunnable;
    
    private int navigationBarHeight = 0;
    
    // Store key positions for touch detection
    private List<KeyInfo> keyInfoList = new ArrayList<>();
    
    private static class KeyInfo {
        String key;
        Rect bounds;
        View view;
        
        KeyInfo(String key, View view) {
            this.key = key;
            this.view = view;
            this.bounds = new Rect();
        }
        
        void updateBounds() {
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            bounds.set(location[0], location[1], 
                      location[0] + view.getWidth(), 
                      location[1] + view.getHeight());
        }
    }
    
    private BroadcastReceiver settingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            if (action == null) return;
            
            if (KeyboardSettings.ACTION_SETTINGS_CHANGED.equals(action)) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        loadSettings();
                        rebuildKeyboard();
                    }
                });
            } else if (KeyboardSettings.ACTION_TYPE_TEXT.equals(action)) {
                final String text = intent.getStringExtra("text");
                if (text != null && !text.isEmpty()) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            typeText(text);
                        }
                    });
                }
            }
        }
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        
        handler = new Handler(Looper.getMainLooper());
        
        try {
            settings = new KeyboardSettings(this);
            loadSettings();
        } catch (Exception e) {
            Log.e(TAG, "Error creating settings", e);
        }
        
        calculateNavigationBarHeight();
        
        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception e) {}
        
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, 50);
        } catch (Exception e) {}
        
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(KeyboardSettings.ACTION_SETTINGS_CHANGED);
            filter.addAction(KeyboardSettings.ACTION_TYPE_TEXT);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(settingsReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(settingsReceiver, filter);
            }
        } catch (Exception e) {}
    }
    
    @Override
    public void onDestroy() {
        stopRepeat();
        try { unregisterReceiver(settingsReceiver); } catch (Exception e) {}
        try { if (toneGenerator != null) toneGenerator.release(); } catch (Exception e) {}
        super.onDestroy();
    }
    
    private void calculateNavigationBarHeight() {
        navigationBarHeight = 0;
        try {
            Resources resources = getResources();
            int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                navigationBarHeight = resources.getDimensionPixelSize(resourceId);
            }
            if (navigationBarHeight == 0) {
                navigationBarHeight = dp(48);
            }
        } catch (Exception e) {
            navigationBarHeight = dp(48);
        }
    }
    
    @Override
    public View onCreateInputView() {
        Log.d(TAG, "onCreateInputView");
        
        try {
            loadSettings();
            calculateNavigationBarHeight();
            
            rootContainer = new FrameLayout(this);
            rootContainer.setBackgroundColor(parseColor(colorBackground));
            
            keyboardView = createKeyboardLayout();
            
            FrameLayout.LayoutParams keyboardParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            );
            keyboardParams.gravity = Gravity.BOTTOM;
            rootContainer.addView(keyboardView, keyboardParams);
            
            // Add invisible touch layer for gap detection
            touchLayer = new FrameLayout(this);
            touchLayer.setBackgroundColor(Color.TRANSPARENT);
            touchLayer.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ));
            touchLayer.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return handleGapTouch(event);
                }
            });
            rootContainer.addView(touchLayer);
            
            int emojiRowHeight = showEmojiRow ? dp(40) : 0;
            int mainHeight = dp(keyboardHeight);
            int totalHeight = emojiRowHeight + mainHeight + navigationBarHeight;
            
            rootContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                totalHeight
            ));
            
            rootContainer.setPadding(0, 0, 0, navigationBarHeight);
            
            // Update key bounds after layout
            rootContainer.post(new Runnable() {
                @Override
                public void run() {
                    updateKeyBounds();
                }
            });
            
            return rootContainer;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating input view", e);
            return new LinearLayout(this);
        }
    }
    
    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        
        try {
            isShift = false;
            isCaps = false;
            isSymbols = false;
            
            if (info != null) {
                int inputClass = info.inputType & EditorInfo.TYPE_MASK_CLASS;
                isNumbers = (inputClass == EditorInfo.TYPE_CLASS_NUMBER || 
                             inputClass == EditorInfo.TYPE_CLASS_PHONE);
            }
            
            loadSettings();
            calculateNavigationBarHeight();
            rebuildKeyboard();
        } catch (Exception e) {}
    }
    
    private void loadSettings() {
        if (settings == null) {
            settings = new KeyboardSettings(this);
        }
        
        try {
            colorBackground = settings.getColorBackground();
            colorKeyNormal = settings.getColorKey();
            colorKeySpecial = settings.getColorKeySpecial();
            colorKeyEnter = settings.getColorKeyEnter();
            colorKeySpace = settings.getColorKeySpace();
            colorText = settings.getColorText();
            
            keyboardHeight = settings.getKeyboardHeight();
            keyRadius = settings.getKeyRadius();
            keyGap = settings.getKeyGap();
            keyTextSize = settings.getKeyTextSize();
            
            vibrateEnabled = settings.isVibrationEnabled();
            vibrateDuration = settings.getVibrationStrength();
            showEmojiRow = settings.isShowEmojiRow();
            longPressDelay = settings.getLongPressDelay();
        } catch (Exception e) {}
    }
    
    private void updateKeyBounds() {
        for (KeyInfo info : keyInfoList) {
            info.updateBounds();
        }
    }
    
    private boolean handleGapTouch(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            return false;
        }
        
        float x = event.getRawX();
        float y = event.getRawY();
        
        // Find nearest key
        KeyInfo nearest = null;
        float minDistance = Float.MAX_VALUE;
        
        for (KeyInfo info : keyInfoList) {
            float centerX = info.bounds.centerX();
            float centerY = info.bounds.centerY();
            float distance = (float) Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
            
            if (distance < minDistance) {
                minDistance = distance;
                nearest = info;
            }
        }
        
        // If touch is close enough to a key (within reasonable distance), trigger it
        if (nearest != null && minDistance < dp(50)) {
            // Check if touch is NOT directly on a key
            boolean onKey = false;
            for (KeyInfo info : keyInfoList) {
                if (info.bounds.contains((int)x, (int)y)) {
                    onKey = true;
                    break;
                }
            }
            
            if (!onKey) {
                // Touch was in gap - trigger nearest key
                doVibrate();
                processKey(nearest.key);
                return true;
            }
        }
        
        return false;
    }
    
    private LinearLayout createKeyboardLayout() {
        keyInfoList.clear();
        
        LinearLayout keyboard = new LinearLayout(this);
        keyboard.setOrientation(LinearLayout.VERTICAL);
        keyboard.setBackgroundColor(parseColor(colorBackground));
        
        int emojiRowHeight = showEmojiRow ? dp(40) : 0;
        int mainHeight = dp(keyboardHeight);
        int totalHeight = emojiRowHeight + mainHeight;
        
        keyboard.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            totalHeight
        ));
        
        keyboard.setPadding(dp(2), dp(4), dp(2), dp(4));
        
        if (showEmojiRow) {
            keyboard.addView(createEmojiRow());
        }
        
        String[][] layout = getActiveLayout();
        for (int i = 0; i < layout.length; i++) {
            keyboard.addView(createRow(layout[i], i));
        }
        
        return keyboard;
    }
    
    private LinearLayout createEmojiRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(40)
        ));
        row.setPadding(dp(2), dp(2), dp(2), dp(4));
        row.setBackgroundColor(parseColor(colorKeySpecial));
        
        String emojiStr = "😀,😂,❤️,👍,🔥,✨,🎉,💯";
        try { emojiStr = settings.getQuickEmojis(); } catch (Exception e) {}
        
        String[] emojis = emojiStr.split(",");
        for (final String emoji : emojis) {
            final String trimmedEmoji = emoji.trim();
            TextView tv = new TextView(this);
            tv.setText(trimmedEmoji);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doVibrate();
                    typeText(trimmedEmoji);
                }
            });
            row.addView(tv);
        }
        
        return row;
    }
    
    private String[][] getActiveLayout() {
        if (isSymbols) return LAYOUT_SYMBOLS;
        if (isNumbers) return LAYOUT_NUMBERS;
        return LAYOUT_LETTERS;
    }
    
    private LinearLayout createRow(String[] keys, int rowIndex) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ));
        
        int vertPad = dp(1);
        if (rowIndex == 1) {
            row.setPadding(dp(10), vertPad, dp(10), vertPad);
        } else {
            row.setPadding(0, vertPad, 0, vertPad);
        }
        
        for (String key : keys) {
            row.addView(createKey(key));
        }
        
        return row;
    }
    
    private View createKey(final String key) {
        TextView tv = new TextView(this);
        tv.setText(getKeyDisplay(key));
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        
        int textColor = parseColor(colorText);
        float textSize = isSpecialKey(key) ? 14 : keyTextSize;
        
        if (key.equals("↵")) {
            textColor = Color.WHITE;
            textSize = 18;
        }
        
        // Shift key icons - FIXED
        if (key.equals("⇧")) {
            if (isCaps) {
                tv.setText("⇪"); // Caps lock icon
                textColor = Color.parseColor("#10b981");
            } else if (isShift) {
                tv.setText("⬆"); // Shift active icon
                textColor = Color.parseColor("#3b82f6");
            } else {
                tv.setText("⇧"); // Normal shift icon
            }
            textSize = 18;
        }
        
        if (key.equals("⌫") || key.equals("🌐") || key.equals("✨")) {
            textSize = 16;
        }
        
        tv.setTextColor(textColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        
        float weight = getKeyWeight(key);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.MATCH_PARENT, weight
        );
        params.setMargins(dp(keyGap), 0, dp(keyGap), 0);
        tv.setLayoutParams(params);
        
        tv.setBackground(createKeyBackground(key));
        
        tv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return handleKeyTouch(v, event, key);
            }
        });
        tv.setClickable(true);
        
        // Store key info for gap touch detection
        KeyInfo keyInfo = new KeyInfo(key, tv);
        keyInfoList.add(keyInfo);
        
        return tv;
    }
    
    private String getKeyDisplay(String key) {
        if (key.equals("⇧")) {
            if (isCaps) return "⇪";
            if (isShift) return "⬆";
            return "⇧";
        }
        if (key.equals("SPACE")) {
            return "";
        }
        if (key.length() == 1 && Character.isLetter(key.charAt(0))) {
            return (isShift || isCaps) ? key.toUpperCase() : key.toLowerCase();
        }
        return key;
    }
    
    private boolean isSpecialKey(String key) {
        return key.equals("⇧") || key.equals("⌫") || key.equals("↵") || 
               key.equals("SPACE") || key.equals("123") || key.equals("ABC") || 
               key.equals("#+=") || key.equals("🌐") || key.equals("✨");
    }
    
    private float getKeyWeight(String key) {
        if (key.equals("SPACE")) return 4.5f;
        if (key.equals("⇧") || key.equals("⌫")) return 1.4f;
        if (key.equals("↵") || key.equals("123") || key.equals("ABC") || key.equals("#+=")) return 1.3f;
        if (key.equals("🌐") || key.equals("✨")) return 0.85f;
        return 1f;
    }
    
    private GradientDrawable createKeyBackground(String key) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(keyRadius));
        
        String color = colorKeyNormal;
        
        if (key.equals("↵")) {
            color = colorKeyEnter;
        } else if (key.equals("⇧")) {
            if (isCaps) {
                color = "#10b981";
            } else if (isShift) {
                color = "#3b82f6";
            } else {
                color = colorKeySpecial;
            }
        } else if (key.equals("⌫") || key.equals("123") || key.equals("ABC") || key.equals("#+=")) {
            color = colorKeySpecial;
        } else if (key.equals("SPACE")) {
            color = colorKeySpace;
        } else if (key.equals("🌐") || key.equals("✨")) {
            color = colorKeySpecial;
        }
        
        bg.setColor(parseColor(color));
        return bg;
    }
    
    private boolean handleKeyTouch(View v, MotionEvent event, String key) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                v.setAlpha(0.7f);
                v.setScaleX(0.96f);
                v.setScaleY(0.96f);
                doVibrate();
                processKey(key);
                if (key.equals("⌫") || key.equals("SPACE")) {
                    startRepeat(key);
                }
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
    
    private void startRepeat(final String key) {
        isRepeating = true;
        repeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRepeating) {
                    processKey(key);
                    doVibrate();
                    handler.postDelayed(repeatRunnable, repeatInterval);
                }
            }
        };
        handler.postDelayed(repeatRunnable, longPressDelay);
    }
    
    private void stopRepeat() {
        isRepeating = false;
        if (repeatRunnable != null && handler != null) {
            handler.removeCallbacks(repeatRunnable);
        }
    }
    
    private void processKey(String key) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        
        if (key.equals("⇧")) {
            if (isCaps) {
                isCaps = false;
                isShift = false;
            } else if (isShift) {
                isCaps = true;
            } else {
                isShift = true;
            }
            rebuildKeyboard();
        } else if (key.equals("⌫")) {
            deleteCharacter(ic);
        } else if (key.equals("↵")) {
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
        } else if (key.equals("SPACE")) {
            ic.commitText(" ", 1);
            autoUnshift();
        } else if (key.equals("123")) {
            isNumbers = true;
            isSymbols = false;
            rebuildKeyboard();
        } else if (key.equals("ABC")) {
            isNumbers = false;
            isSymbols = false;
            rebuildKeyboard();
        } else if (key.equals("#+=")) {
            isSymbols = true;
            isNumbers = false;
            rebuildKeyboard();
        } else if (key.equals("🌐")) {
            openPopup("language");
        } else if (key.equals("✨")) {
            openPopup("tools");
        } else {
            String text = key;
            if ((isShift || isCaps) && key.length() == 1 && Character.isLetter(key.charAt(0))) {
                text = key.toUpperCase();
            }
            ic.commitText(text, 1);
            autoUnshift();
        }
    }
    
    // FIXED: Delete emoji/symbol in one click
    private void deleteCharacter(InputConnection ic) {
        if (ic == null) return;
        
        // Get text before cursor
        CharSequence beforeCursor = ic.getTextBeforeCursor(2, 0);
        
        if (beforeCursor != null && beforeCursor.length() > 0) {
            String text = beforeCursor.toString();
            int length = text.length();
            
            // Check if we need to delete more than 1 code unit
            if (length >= 2) {
                // Check if this is a surrogate pair (emoji)
                char lastChar = text.charAt(length - 1);
                char secondLastChar = text.charAt(length - 2);
                
                if (Character.isLowSurrogate(lastChar) && Character.isSurrogatePair(secondLastChar, lastChar)) {
                    // It's a surrogate pair (emoji), delete 2 code units
                    ic.deleteSurroundingText(2, 0);
                    return;
                }
                
                // Check for variation selectors and ZWJ sequences (complex emojis)
                if (lastChar == '\uFE0F' || lastChar == '\u200D') {
                    // Part of emoji sequence, delete more
                    ic.deleteSurroundingText(2, 0);
                    return;
                }
            }
            
            // Check for single surrogate (shouldn't happen but just in case)
            if (length >= 1) {
                char lastChar = text.charAt(length - 1);
                if (Character.isLowSurrogate(lastChar)) {
                    ic.deleteSurroundingText(2, 0);
                    return;
                }
            }
        }
        
        // Default: delete 1 character
        ic.deleteSurroundingText(1, 0);
    }
    
    private void autoUnshift() {
        if (isShift && !isCaps) {
            isShift = false;
            rebuildKeyboard();
        }
    }
    
    private void typeText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null && text != null) {
            ic.commitText(text, 1);
        }
    }
    
    private void rebuildKeyboard() {
        if (keyboardView == null || rootContainer == null) return;
        
        keyInfoList.clear();
        keyboardView.removeAllViews();
        
        keyboardView.setBackgroundColor(parseColor(colorBackground));
        rootContainer.setBackgroundColor(parseColor(colorBackground));
        
        int emojiRowHeight = showEmojiRow ? dp(40) : 0;
        int mainHeight = dp(keyboardHeight);
        int keyboardTotalHeight = emojiRowHeight + mainHeight;
        
        ViewGroup.LayoutParams keyboardParams = keyboardView.getLayoutParams();
        if (keyboardParams != null) {
            keyboardParams.height = keyboardTotalHeight;
            keyboardView.setLayoutParams(keyboardParams);
        }
        
        int containerTotalHeight = keyboardTotalHeight + navigationBarHeight;
        ViewGroup.LayoutParams containerParams = rootContainer.getLayoutParams();
        if (containerParams != null) {
            containerParams.height = containerTotalHeight;
            rootContainer.setLayoutParams(containerParams);
        }
        
        rootContainer.setPadding(0, 0, 0, navigationBarHeight);
        
        keyboardView.setPadding(dp(2), dp(4), dp(2), dp(4));
        
        if (showEmojiRow) {
            keyboardView.addView(createEmojiRow());
        }
        
        String[][] layout = getActiveLayout();
        for (int i = 0; i < layout.length; i++) {
            keyboardView.addView(createRow(layout[i], i));
        }
        
        // Update key bounds after layout
        rootContainer.post(new Runnable() {
            @Override
            public void run() {
                updateKeyBounds();
            }
        });
    }
    
    private void openPopup(String mode) {
        try {
            Intent intent = new Intent(this, PopupActivity.class);
            intent.putExtra("mode", mode);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {}
    }
    
    private void doVibrate() {
        if (!vibrateEnabled || vibrator == null) return;
        try {
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(vibrateDuration, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(vibrateDuration);
                }
            }
        } catch (Exception e) {}
    }
    
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
    
    private int parseColor(String color) {
        try {
            return Color.parseColor(color);
        } catch (Exception e) {
            return Color.parseColor("#000000");
        }
    }
}