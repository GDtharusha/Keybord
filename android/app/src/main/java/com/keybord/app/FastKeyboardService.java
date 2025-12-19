package com.keybord.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
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
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FastKeyboardService extends InputMethodService {
    
    private static final String TAG = "FastKeyboardService";
    
    private String colorBackground = "#1a1a2e";
    private String colorKeyNormal = "#3d3d5c";
    private String colorKeySpecial = "#252540";
    private String colorKeyEnter = "#2563eb";
    private String colorKeySpace = "#303050";
    private String colorTextNormal = "#FFFFFF";
    private String colorTextSpecial = "#9ca3af";
    
    private int keyboardHeight = 245;
    private int keyRadius = 8;
    private int keyMargin = 2;
    private int keyTextSize = 20;
    
    private boolean vibrateEnabled = true;
    private int vibrateDuration = 5;
    private boolean soundEnabled = false;
    private boolean showEmojiRow = false;
    private int longPressDelay = 350;
    private int repeatInterval = 35;
    
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
    
    private LinearLayout keyboardView;
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
    
    private BroadcastReceiver settingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();
            if (action == null) return;
            
            Log.d(TAG, "Received: " + action);
            
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
        
        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception e) {
            Log.e(TAG, "Error getting vibrator", e);
        }
        
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, 50);
        } catch (Exception e) {
            Log.e(TAG, "Error creating ToneGenerator", e);
        }
        
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(KeyboardSettings.ACTION_SETTINGS_CHANGED);
            filter.addAction(KeyboardSettings.ACTION_TYPE_TEXT);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(settingsReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(settingsReceiver, filter);
            }
            Log.d(TAG, "Receiver registered");
        } catch (Exception e) {
            Log.e(TAG, "Error registering receiver", e);
        }
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopRepeat();
        
        try {
            unregisterReceiver(settingsReceiver);
        } catch (Exception e) {}
        
        try {
            if (toneGenerator != null) {
                toneGenerator.release();
                toneGenerator = null;
            }
        } catch (Exception e) {}
        
        super.onDestroy();
    }
    
    @Override
    public View onCreateInputView() {
        Log.d(TAG, "onCreateInputView");
        
        try {
            loadSettings();
            keyboardView = createKeyboardLayout();
            return keyboardView;
        } catch (Exception e) {
            Log.e(TAG, "Error creating input view", e);
            LinearLayout errorView = new LinearLayout(this);
            errorView.setBackgroundColor(Color.RED);
            return errorView;
        }
    }
    
    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        Log.d(TAG, "onStartInputView");
        
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
            rebuildKeyboard();
        } catch (Exception e) {
            Log.e(TAG, "Error in onStartInputView", e);
        }
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
            colorTextNormal = settings.getColorText();
            colorTextSpecial = settings.getColorTextSpecial();
            
            keyboardHeight = settings.getKeyboardHeight();
            keyRadius = settings.getKeyRadius();
            keyTextSize = settings.getKeyTextSize();
            
            vibrateEnabled = settings.isVibrationEnabled();
            vibrateDuration = settings.getVibrationStrength();
            soundEnabled = settings.isSoundEnabled();
            showEmojiRow = settings.isShowEmojiRow();
            longPressDelay = settings.getLongPressDelay();
            
            Log.d(TAG, "Settings loaded: height=" + keyboardHeight);
        } catch (Exception e) {
            Log.e(TAG, "Error loading settings", e);
        }
    }
    
    private LinearLayout createKeyboardLayout() {
        LinearLayout keyboard = new LinearLayout(this);
        keyboard.setOrientation(LinearLayout.VERTICAL);
        
        try {
            keyboard.setBackgroundColor(Color.parseColor(colorBackground));
        } catch (Exception e) {
            keyboard.setBackgroundColor(Color.parseColor("#1a1a2e"));
        }
        
        int emojiRowHeight = showEmojiRow ? dp(40) : 0;
        int mainHeight = dp(keyboardHeight);
        int totalHeight = emojiRowHeight + mainHeight;
        
        keyboard.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            totalHeight
        ));
        
        keyboard.setPadding(dp(3), dp(4), dp(3), dp(6));
        
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
        
        try {
            row.setBackgroundColor(Color.parseColor(colorKeySpecial));
        } catch (Exception e) {
            row.setBackgroundColor(Color.parseColor("#252540"));
        }
        
        String emojiStr = "😀,😂,❤️,👍,🔥,✨,🎉,💯";
        if (settings != null) {
            try {
                emojiStr = settings.getQuickEmojis();
            } catch (Exception e) {}
        }
        
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
                    playSound();
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
        
        int vertPad = dp(2);
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
        
        int textColor;
        float textSize;
        
        try {
            textColor = Color.parseColor(isSpecialKey(key) ? colorTextSpecial : colorTextNormal);
        } catch (Exception e) {
            textColor = Color.WHITE;
        }
        
        textSize = isSpecialKey(key) ? 13 : keyTextSize;
        
        if (key.equals("↵")) {
            textColor = Color.WHITE;
            textSize = 18;
        }
        if (key.equals("⇧")) {
            if (isCaps) {
                textColor = Color.parseColor("#10b981");
            } else if (isShift) {
                textColor = Color.parseColor("#3b82f6");
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
        params.setMargins(dp(keyMargin), 0, dp(keyMargin), 0);
        tv.setLayoutParams(params);
        
        tv.setBackground(createKeyBackground(key));
        
        tv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return handleKeyTouch(v, event, key);
            }
        });
        tv.setClickable(true);
        
        return tv;
    }
    
    private String getKeyDisplay(String key) {
        if (key.equals("⇧")) {
            return (isShift || isCaps) ? "⬆" : "⇧";
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
        
        try {
            bg.setColor(Color.parseColor(color));
        } catch (Exception e) {
            bg.setColor(Color.parseColor("#3d3d5c"));
        }
        
        return bg;
    }
    
    private boolean handleKeyTouch(View v, MotionEvent event, String key) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                v.setAlpha(0.7f);
                v.setScaleX(0.96f);
                v.setScaleY(0.96f);
                doVibrate();
                playSound();
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
            ic.deleteSurroundingText(1, 0);
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
        if (keyboardView != null) {
            keyboardView.removeAllViews();
            
            try {
                keyboardView.setBackgroundColor(Color.parseColor(colorBackground));
            } catch (Exception e) {
                keyboardView.setBackgroundColor(Color.parseColor("#1a1a2e"));
            }
            
            int emojiRowHeight = showEmojiRow ? dp(40) : 0;
            int mainHeight = dp(keyboardHeight);
            int totalHeight = emojiRowHeight + mainHeight;
            
            ViewGroup.LayoutParams params = keyboardView.getLayoutParams();
            if (params != null) {
                params.height = totalHeight;
                keyboardView.setLayoutParams(params);
            }
            
            if (showEmojiRow) {
                keyboardView.addView(createEmojiRow());
            }
            
            String[][] layout = getActiveLayout();
            for (int i = 0; i < layout.length; i++) {
                keyboardView.addView(createRow(layout[i], i));
            }
        }
    }
    
    private void openPopup(String mode) {
        try {
            Intent intent = new Intent(this, PopupActivity.class);
            intent.putExtra("mode", mode);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Cannot open popup", e);
        }
    }
    
    private void doVibrate() {
        if (!vibrateEnabled) return;
        if (vibrator == null) return;
        
        try {
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(vibrateDuration, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(vibrateDuration);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Vibration error", e);
        }
    }
    
    private void playSound() {
        if (!soundEnabled) return;
        if (toneGenerator == null) return;
        
        try {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 30);
        } catch (Exception e) {
            Log.e(TAG, "Sound error", e);
        }
    }
    
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}