package com.keybord.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
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
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FastKeyboardService extends InputMethodService {
    
    private static final String TAG = "FastKeyboardService";
    
    // ════════════════════════════════════════════════════════════════
    // SETTINGS - SharedPreferences වලින් load වෙනවා
    // ════════════════════════════════════════════════════════════════
    
    // Colors (loaded from settings)
    private String colorBackground;
    private String colorKeyNormal;
    private String colorKeyPressed;
    private String colorKeySpecial;
    private String colorKeyEnter;
    private String colorKeySpace;
    private String colorTextNormal;
    private String colorTextSpecial;
    
    // Sizes (loaded from settings)
    private int keyboardHeight;
    private int keyRadius;
    private int keyMargin = 2; // REDUCED from 3 to 2
    private int keyTextSize;
    private int keySpecialTextSize;
    
    // Haptics (loaded from settings)
    private boolean vibrateEnabled;
    private int vibrateDuration;
    private boolean soundEnabled;
    
    // Features (loaded from settings)
    private boolean showEmojiRow;
    private int longPressDelay;
    private int repeatInterval = 35; // Faster repeat for better UX
    
    // ════════════════════════════════════════════════════════════════
    // LAYOUTS - COMPACT (reduced gaps built into layout)
    // ════════════════════════════════════════════════════════════════
    
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
    
    private static final String[] EMOJI_ROW = {"😀", "😂", "❤️", "👍", "🔥", "✨", "🎉", "💯", "😍", "🙏"};
    
    // ════════════════════════════════════════════════════════════════
    // STATE VARIABLES
    // ════════════════════════════════════════════════════════════════
    
    private LinearLayout keyboard;
    private FrameLayout rootContainer;
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
    
    // ════════════════════════════════════════════════════════════════
    // BROADCAST RECEIVER
    // ════════════════════════════════════════════════════════════════
    
    private BroadcastReceiver settingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Received broadcast: " + action);
            
            if (KeyboardSettings.ACTION_SETTINGS_CHANGED.equals(action)) {
                loadSettings();
                refreshKeyboard();
                showToast("⚙️ Settings Updated!");
            } 
            else if (KeyboardSettings.ACTION_TYPE_TEXT.equals(action)) {
                String text = intent.getStringExtra("text");
                if (text != null) {
                    typeText(text);
                }
            }
        }
    };
    
    // ════════════════════════════════════════════════════════════════
    // LIFECYCLE METHODS
    // ════════════════════════════════════════════════════════════════
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        
        handler = new Handler(Looper.getMainLooper());
        settings = new KeyboardSettings(this);
        
        loadSettings();
        calculateNavigationBarHeight();
        
        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception e) {
            Log.e(TAG, "Vibrator init error", e);
        }
        
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, 50);
        } catch (Exception e) {
            Log.e(TAG, "ToneGenerator init error", e);
        }
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(KeyboardSettings.ACTION_SETTINGS_CHANGED);
        filter.addAction(KeyboardSettings.ACTION_TYPE_TEXT);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(settingsReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(settingsReceiver, filter);
        }
        
        Log.d(TAG, "BroadcastReceiver registered");
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        stopRepeat();
        
        try {
            unregisterReceiver(settingsReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Unregister receiver error", e);
        }
        
        try {
            if (toneGenerator != null) {
                toneGenerator.release();
            }
        } catch (Exception e) {}
        
        super.onDestroy();
    }
    
    // ════════════════════════════════════════════════════════════════
    // NAVIGATION BAR HEIGHT DETECTION
    // ════════════════════════════════════════════════════════════════
    
    private void calculateNavigationBarHeight() {
        try {
            int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                navigationBarHeight = getResources().getDimensionPixelSize(resourceId);
                Log.d(TAG, "Navigation bar height: " + navigationBarHeight);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting nav bar height", e);
            navigationBarHeight = dp(48); // Default fallback
        }
    }
    
    private boolean hasNavigationBar() {
        // Check if device uses gesture navigation (no visible nav bar)
        int resourceId = getResources().getIdentifier("config_showNavigationBar", "bool", "android");
        if (resourceId > 0) {
            return getResources().getBoolean(resourceId);
        }
        return true; // Assume it has nav bar by default
    }
    
    // ════════════════════════════════════════════════════════════════
    // INPUT VIEW CREATION
    // ════════════════════════════════════════════════════════════════
    
    @Override
    public View onCreateInputView() {
        Log.d(TAG, "onCreateInputView");
        loadSettings();
        calculateNavigationBarHeight();
        
        // Create root container
        rootContainer = new FrameLayout(this);
        rootContainer.setBackgroundColor(Color.parseColor(colorBackground));
        
        // Build keyboard
        keyboard = buildKeyboard();
        rootContainer.addView(keyboard);
        
        return rootContainer;
    }
    
    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        Log.d(TAG, "onStartInputView");
        
        isShift = false;
        isCaps = false;
        isSymbols = false;
        
        int inputClass = info.inputType & EditorInfo.TYPE_MASK_CLASS;
        isNumbers = (inputClass == EditorInfo.TYPE_CLASS_NUMBER || 
                     inputClass == EditorInfo.TYPE_CLASS_PHONE);
        
        loadSettings();
        refreshKeyboard();
    }
    
    @Override
    public void onComputeInsets(Insets outInsets) {
        super.onComputeInsets(outInsets);
        
        if (rootContainer != null) {
            // Calculate proper insets to position above navigation bar
            int totalHeight = dp(keyboardHeight) + (showEmojiRow ? dp(44) : 0);
            
            outInsets.contentTopInsets = outInsets.visibleTopInsets;
            outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_FRAME;
        }
    }
    
    // ════════════════════════════════════════════════════════════════
    // SETTINGS LOADING
    // ════════════════════════════════════════════════════════════════
    
    private void loadSettings() {
        Log.d(TAG, "Loading settings...");
        
        // Colors
        colorBackground = settings.getColorBackground();
        colorKeyNormal = settings.getColorKey();
        colorKeyPressed = settings.getColorKeyPressed();
        colorKeySpecial = settings.getColorKeySpecial();
        colorKeyEnter = settings.getColorKeyEnter();
        colorKeySpace = settings.getColorKeySpace();
        colorTextNormal = settings.getColorText();
        colorTextSpecial = settings.getColorTextSpecial();
        
        // Sizes - IMPROVED DEFAULTS
        keyboardHeight = settings.getKeyboardHeight();
        keyRadius = settings.getKeyRadius();
        keyMargin = 2; // Fixed smaller margin for better touch targets
        keyTextSize = settings.getKeyTextSize();
        keySpecialTextSize = 14;
        
        // Haptics
        vibrateEnabled = settings.isVibrationEnabled();
        vibrateDuration = settings.getVibrationStrength();
        soundEnabled = settings.isSoundEnabled();
        
        // Features
        showEmojiRow = settings.isShowEmojiRow();
        longPressDelay = settings.getLongPressDelay();
        
        Log.d(TAG, "Settings loaded - height:" + keyboardHeight + ", vibrate:" + vibrateEnabled);
    }
    
    // ════════════════════════════════════════════════════════════════
    // KEYBOARD BUILDING
    // ════════════════════════════════════════════════════════════════
    
    private LinearLayout buildKeyboard() {
        LinearLayout kb = new LinearLayout(this);
        kb.setOrientation(LinearLayout.VERTICAL);
        kb.setBackgroundColor(Color.parseColor(colorBackground));
        
        // Minimal padding for maximum key space
        int sidePadding = dp(3);
        int topPadding = dp(6);
        int bottomPadding = dp(8);
        
        kb.setPadding(sidePadding, topPadding, sidePadding, bottomPadding);
        
        // Calculate total height
        int totalHeight = dp(keyboardHeight);
        if (showEmojiRow) {
            totalHeight += dp(44);
        }
        
        kb.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 
            totalHeight
        ));
        
        // Add emoji row if enabled
        if (showEmojiRow) {
            kb.addView(buildEmojiRow());
        }
        
        // Add main keyboard rows
        String[][] layout = getActiveLayout();
        for (int i = 0; i < layout.length; i++) {
            kb.addView(buildRow(layout[i], i));
        }
        
        return kb;
    }
    
    private LinearLayout buildEmojiRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44)
        ));
        row.setPadding(dp(2), dp(2), dp(2), dp(4));
        row.setBackgroundColor(Color.parseColor(colorKeySpecial));
        
        String emojiStr = settings.getQuickEmojis();
        String[] emojis = emojiStr.split(",");
        
        for (String emoji : emojis) {
            TextView tv = new TextView(this);
            tv.setText(emoji.trim());
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
            tv.setOnClickListener(v -> {
                doVibrate();
                playSound();
                typeText(emoji.trim());
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
    
    private LinearLayout buildRow(String[] keys, int rowIndex) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ));
        
        // Minimal vertical padding between rows
        int vertPadding = dp(2);
        row.setPadding(0, vertPadding, 0, vertPadding);
        
        // Add slight horizontal padding for second row (QWERTY offset)
        if (rowIndex == 1) {
            row.setPadding(dp(12), vertPadding, dp(12), vertPadding);
        }
        
        for (String key : keys) {
            row.addView(buildKey(key));
        }
        
        return row;
    }
    
    private View buildKey(String key) {
        TextView tv = new TextView(this);
        tv.setText(getKeyDisplay(key));
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        
        // Text color and size
        int textColor = Color.parseColor(isSpecialKey(key) ? colorTextSpecial : colorTextNormal);
        float textSize = isSpecialKey(key) ? keySpecialTextSize : keyTextSize;
        
        // Special key adjustments
        if (key.equals("↵")) {
            textColor = Color.WHITE;
            textSize = 20;
        }
        if (key.equals("⇧")) {
            if (isCaps) textColor = Color.parseColor("#10b981");
            else if (isShift) textColor = Color.parseColor("#3b82f6");
            textSize = 20;
        }
        if (key.equals("⌫") || key.equals("🌐") || key.equals("✨")) {
            textSize = 18;
        }
        
        tv.setTextColor(textColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        
        // Layout params with weight - REDUCED MARGINS
        float weight = getKeyWeight(key);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.MATCH_PARENT, weight
        );
        
        // Minimal margins for better touch targets
        int margin = dp(keyMargin);
        params.setMargins(margin, 0, margin, 0);
        tv.setLayoutParams(params);
        
        // Background
        tv.setBackground(createKeyBackground(key));
        
        // Touch listener
        tv.setOnTouchListener((v, e) -> handleTouch(v, e, key));
        tv.setClickable(true);
        
        return tv;
    }
    
    private String getKeyDisplay(String key) {
        switch (key) {
            case "⇧":
                return (isShift || isCaps) ? "⬆" : "⇧";
            case "SPACE":
                return ""; // Empty for cleaner look
            default:
                if (key.length() == 1 && Character.isLetter(key.charAt(0))) {
                    return (isShift || isCaps) ? key.toUpperCase() : key.toLowerCase();
                }
                return key;
        }
    }
    
    private boolean isSpecialKey(String key) {
        return key.equals("⇧") || key.equals("⌫") || key.equals("↵") || 
               key.equals("SPACE") || key.equals("123") || key.equals("ABC") || 
               key.equals("#+=") || key.equals("🌐") || key.equals("✨");
    }
    
    private float getKeyWeight(String key) {
        switch (key) {
            case "SPACE": return 4.5f; // Wider space bar
            case "⇧": case "⌫": return 1.4f;
            case "↵": case "123": case "ABC": case "#+=": return 1.3f;
            case "🌐": case "✨": return 0.85f;
            default: return 1f;
        }
    }
    
    private GradientDrawable createKeyBackground(String key) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(keyRadius));
        
        String color;
        switch (key) {
            case "↵":
                color = colorKeyEnter;
                break;
            case "⇧":
                if (isCaps) color = "#10b981";
                else if (isShift) color = "#3b82f6";
                else color = colorKeySpecial;
                break;
            case "⌫": case "123": case "ABC": case "#+=":
                color = colorKeySpecial;
                break;
            case "SPACE":
                color = colorKeySpace;
                break;
            case "🌐": case "✨":
                color = colorKeySpecial;
                break;
            default:
                color = colorKeyNormal;
        }
        
        bg.setColor(Color.parseColor(color));
        return bg;
    }
    
    // ════════════════════════════════════════════════════════════════
    // TOUCH HANDLING - IMPROVED
    // ════════════════════════════════════════════════════════════════
    
    private boolean handleTouch(View v, MotionEvent e, String key) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Visual feedback
                v.setAlpha(0.7f);
                v.setScaleX(0.96f);
                v.setScaleY(0.96f);
                
                // Haptic & sound feedback
                doVibrate();
                playSound();
                
                // Process key
                processKey(key);
                
                // Start repeat for backspace and space
                if (key.equals("⌫") || key.equals("SPACE")) {
                    startRepeat(key);
                }
                return true;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Reset visual
                v.setAlpha(1f);
                v.setScaleX(1f);
                v.setScaleY(1f);
                
                // Stop repeat
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
                handler.postDelayed(repeatRunnable, repeatInterval);
            }
        };
        handler.postDelayed(repeatRunnable, longPressDelay);
    }
    
    private void stopRepeat() {
        isRepeating = false;
        if (repeatRunnable != null) {
            handler.removeCallbacks(repeatRunnable);
        }
    }
    
    // ════════════════════════════════════════════════════════════════
    // KEY PROCESSING
    // ════════════════════════════════════════════════════════════════
    
    private void processKey(String key) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        
        switch (key) {
            case "⇧":
                if (isCaps) {
                    isCaps = false;
                    isShift = false;
                } else if (isShift) {
                    isCaps = true;
                } else {
                    isShift = true;
                }
                refreshKeyboard();
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
                refreshKeyboard();
                break;
                
            case "ABC":
                isNumbers = false;
                isSymbols = false;
                refreshKeyboard();
                break;
                
            case "#+=":
                isSymbols = true;
                isNumbers = false;
                refreshKeyboard();
                break;
                
            case "🌐":
                openPopup("language");
                break;
                
            case "✨":
                openPopup("tools");
                break;
                
            default:
                String text = key;
                if ((isShift || isCaps) && key.length() == 1 && 
                    Character.isLetter(key.charAt(0))) {
                    text = key.toUpperCase();
                }
                ic.commitText(text, 1);
                autoUnshift();
        }
    }
    
    private void autoUnshift() {
        if (isShift && !isCaps) {
            isShift = false;
            refreshKeyboard();
        }
    }
    
    private void typeText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null && text != null) {
            ic.commitText(text, 1);
        }
    }
    
    // ════════════════════════════════════════════════════════════════
    // POPUP WINDOW
    // ════════════════════════════════════════════════════════════════
    
    private void openPopup(String mode) {
        try {
            Intent intent = new Intent(this, PopupActivity.class);
            intent.putExtra("mode", mode);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Cannot open popup", e);
            showToast("Cannot open popup");
        }
    }
    
    // ════════════════════════════════════════════════════════════════
    // FEEDBACK
    // ════════════════════════════════════════════════════════════════
    
    private void doVibrate() {
        if (!vibrateEnabled || vibrator == null) return;
        
        try {
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(
                        vibrateDuration, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(vibrateDuration);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Vibration error", e);
        }
    }
    
    private void playSound() {
        if (!soundEnabled || toneGenerator == null) return;
        
        try {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 30);
        } catch (Exception e) {
            Log.e(TAG, "Sound error", e);
        }
    }
    
    // ════════════════════════════════════════════════════════════════
    // REFRESH KEYBOARD
    // ════════════════════════════════════════════════════════════════
    
    private void refreshKeyboard() {
        if (keyboard != null) {
            keyboard.removeAllViews();
            
            if (showEmojiRow) {
                keyboard.addView(buildEmojiRow());
            }
            
            String[][] layout = getActiveLayout();
            for (int i = 0; i < layout.length; i++) {
                keyboard.addView(buildRow(layout[i], i));
            }
            
            keyboard.setBackgroundColor(Color.parseColor(colorBackground));
            
            // Update keyboard height
            int totalHeight = dp(keyboardHeight);
            if (showEmojiRow) {
                totalHeight += dp(44);
            }
            
            ViewGroup.LayoutParams params = keyboard.getLayoutParams();
            if (params != null) {
                params.height = totalHeight;
                keyboard.setLayoutParams(params);
            }
        }
        
        if (rootContainer != null) {
            rootContainer.setBackgroundColor(Color.parseColor(colorBackground));
        }
    }
    
    // ════════════════════════════════════════════════════════════════
    // UTILITIES
    // ════════════════════════════════════════════════════════════════
    
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
    
    private void showToast(String message) {
        handler.post(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
}