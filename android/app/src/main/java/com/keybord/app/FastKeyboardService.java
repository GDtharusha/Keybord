package com.keybord.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
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
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FastKeyboardService extends InputMethodService {
    
    private static final String TAG = "FastKeyboard";
    
    // ═══════════════════════════════════════════════════════════════════
    // SETTINGS
    // ═══════════════════════════════════════════════════════════════════
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
    private int longPressDelay = 300;
    private int repeatInterval = 30;
    
    // ═══════════════════════════════════════════════════════════════════
    // KEYBOARD LAYOUTS
    // ═══════════════════════════════════════════════════════════════════
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
    
    // ═══════════════════════════════════════════════════════════════════
    // SINHALA KEY LABELS
    // ═══════════════════════════════════════════════════════════════════
    private static final Map<String, String> SINHALA_LABELS = new HashMap<>();
    private static final Map<String, String> SINHALA_LABELS_SHIFT = new HashMap<>();
    static {
        SINHALA_LABELS.put("q", "ෘ"); SINHALA_LABELS.put("w", "ව"); SINHALA_LABELS.put("e", "එ");
        SINHALA_LABELS.put("r", "ර"); SINHALA_LABELS.put("t", "ට"); SINHALA_LABELS.put("y", "ය");
        SINHALA_LABELS.put("u", "උ"); SINHALA_LABELS.put("i", "ඉ"); SINHALA_LABELS.put("o", "ඔ");
        SINHALA_LABELS.put("p", "ප"); SINHALA_LABELS.put("a", "අ"); SINHALA_LABELS.put("s", "ස");
        SINHALA_LABELS.put("d", "ඩ"); SINHALA_LABELS.put("f", "ෆ"); SINHALA_LABELS.put("g", "ග");
        SINHALA_LABELS.put("h", "හ"); SINHALA_LABELS.put("j", "ජ"); SINHALA_LABELS.put("k", "ක");
        SINHALA_LABELS.put("l", "ල"); SINHALA_LABELS.put("z", "ඤ"); SINHALA_LABELS.put("x", "ං");
        SINHALA_LABELS.put("c", "ච"); SINHALA_LABELS.put("v", "ව"); SINHALA_LABELS.put("b", "බ");
        SINHALA_LABELS.put("n", "න"); SINHALA_LABELS.put("m", "ම");
        
        SINHALA_LABELS_SHIFT.put("q", "ඍ"); SINHALA_LABELS_SHIFT.put("w", "ව"); SINHALA_LABELS_SHIFT.put("e", "ඒ");
        SINHALA_LABELS_SHIFT.put("r", "ර"); SINHALA_LABELS_SHIFT.put("t", "ඨ"); SINHALA_LABELS_SHIFT.put("y", "ය");
        SINHALA_LABELS_SHIFT.put("u", "ඌ"); SINHALA_LABELS_SHIFT.put("i", "ඊ"); SINHALA_LABELS_SHIFT.put("o", "ඕ");
        SINHALA_LABELS_SHIFT.put("p", "ඵ"); SINHALA_LABELS_SHIFT.put("a", "ඇ"); SINHALA_LABELS_SHIFT.put("s", "ෂ");
        SINHALA_LABELS_SHIFT.put("d", "ඪ"); SINHALA_LABELS_SHIFT.put("f", "ෆ"); SINHALA_LABELS_SHIFT.put("g", "ඝ");
        SINHALA_LABELS_SHIFT.put("h", "ළ"); SINHALA_LABELS_SHIFT.put("j", "ඣ"); SINHALA_LABELS_SHIFT.put("k", "ඛ");
        SINHALA_LABELS_SHIFT.put("l", "ළ"); SINHALA_LABELS_SHIFT.put("z", "ඥ"); SINHALA_LABELS_SHIFT.put("x", "ඃ");
        SINHALA_LABELS_SHIFT.put("c", "ඡ"); SINHALA_LABELS_SHIFT.put("v", "ව"); SINHALA_LABELS_SHIFT.put("b", "භ");
        SINHALA_LABELS_SHIFT.put("n", "ණ"); SINHALA_LABELS_SHIFT.put("m", "ඹ");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SINGLISH ENGINE - OPTIMIZED MAPS
    // ═══════════════════════════════════════════════════════════════════
    
    // Consonants → Sinhala base (without hal)
    private static final Map<String, String> CONSONANTS = new HashMap<>();
    static {
        // 3-letter
        CONSONANTS.put("ndh", "ඳ"); CONSONANTS.put("nDh", "ඳ");
        CONSONANTS.put("ngh", "ඟ");
        CONSONANTS.put("mbh", "ඹ");
        CONSONANTS.put("nch", "ඤ්ච");
        CONSONANTS.put("njh", "ඤ්ජ");
        
        // 2-letter
        CONSONANTS.put("kh", "ඛ"); CONSONANTS.put("Kh", "ඛ");
        CONSONANTS.put("gh", "ඝ"); CONSONANTS.put("Gh", "ඝ");
        CONSONANTS.put("ng", "ඟ"); CONSONANTS.put("Ng", "ඟ");
        CONSONANTS.put("ch", "ච"); CONSONANTS.put("Ch", "ඡ");
        CONSONANTS.put("jh", "ඣ"); CONSONANTS.put("Jh", "ඣ");
        CONSONANTS.put("ny", "ඤ"); CONSONANTS.put("Ny", "ඤ");
        CONSONANTS.put("gn", "ඥ"); CONSONANTS.put("Gn", "ඥ");
        CONSONANTS.put("Th", "ඨ"); CONSONANTS.put("th", "ත");
        CONSONANTS.put("Dh", "ඪ"); CONSONANTS.put("dh", "ද");
        CONSONANTS.put("DH", "ධ");
        CONSONANTS.put("nd", "ඳ"); CONSONANTS.put("Nd", "ඳ");
        CONSONANTS.put("ph", "ඵ"); CONSONANTS.put("Ph", "ඵ");
        CONSONANTS.put("bh", "භ"); CONSONANTS.put("Bh", "භ");
        CONSONANTS.put("mb", "ඹ"); CONSONANTS.put("Mb", "ඹ");
        CONSONANTS.put("sh", "ශ"); CONSONANTS.put("Sh", "ෂ");
        CONSONANTS.put("SH", "ෂ");
        CONSONANTS.put("lh", "ළ"); CONSONANTS.put("Lh", "ළ");
        
        // 1-letter
        CONSONANTS.put("k", "ක"); CONSONANTS.put("K", "ඛ");
        CONSONANTS.put("g", "ග"); CONSONANTS.put("G", "ඝ");
        CONSONANTS.put("c", "ච"); CONSONANTS.put("C", "ඡ");
        CONSONANTS.put("j", "ජ"); CONSONANTS.put("J", "ඣ");
        CONSONANTS.put("t", "ට"); CONSONANTS.put("T", "ඨ");
        CONSONANTS.put("d", "ඩ"); CONSONANTS.put("D", "ඪ");
        CONSONANTS.put("N", "ණ"); CONSONANTS.put("n", "න");
        CONSONANTS.put("p", "ප"); CONSONANTS.put("P", "ඵ");
        CONSONANTS.put("b", "බ"); CONSONANTS.put("B", "භ");
        CONSONANTS.put("m", "ම"); CONSONANTS.put("M", "ම");
        CONSONANTS.put("y", "ය"); CONSONANTS.put("Y", "ය");
        CONSONANTS.put("r", "ර"); CONSONANTS.put("R", "ර");
        CONSONANTS.put("l", "ල"); CONSONANTS.put("L", "ළ");
        CONSONANTS.put("w", "ව"); CONSONANTS.put("W", "ව");
        CONSONANTS.put("v", "ව"); CONSONANTS.put("V", "ව");
        CONSONANTS.put("s", "ස"); CONSONANTS.put("S", "ෂ");
        CONSONANTS.put("h", "හ"); CONSONANTS.put("H", "හ");
        CONSONANTS.put("f", "ෆ"); CONSONANTS.put("F", "ෆ");
        CONSONANTS.put("x", "ක්ෂ"); CONSONANTS.put("X", "ක්ෂ");
        CONSONANTS.put("z", "ඤ"); CONSONANTS.put("Z", "ඤ");
    }
    
    // Standalone vowels (word start / after vowel)
    private static final Map<String, String> VOWELS = new HashMap<>();
    static {
        VOWELS.put("aa", "ආ"); VOWELS.put("AA", "ඈ"); VOWELS.put("Aa", "ආ");
        VOWELS.put("ae", "ඇ"); VOWELS.put("AE", "ඈ"); VOWELS.put("Ae", "ඇ");
        VOWELS.put("aE", "ඈ");
        VOWELS.put("ii", "ඊ"); VOWELS.put("II", "ඊ"); VOWELS.put("ee", "ඊ");
        VOWELS.put("uu", "ඌ"); VOWELS.put("UU", "ඌ"); VOWELS.put("oo", "ඌ");
        VOWELS.put("ea", "ඒ"); VOWELS.put("EA", "ඒ");
        VOWELS.put("oe", "ඕ"); VOWELS.put("OE", "ඕ");
        VOWELS.put("ai", "ඓ"); VOWELS.put("Ai", "ඓ"); VOWELS.put("AI", "ඓ");
        VOWELS.put("au", "ඖ"); VOWELS.put("Au", "ඖ"); VOWELS.put("AU", "ඖ");
        VOWELS.put("ru", "ඍ"); VOWELS.put("Ru", "ඍ"); VOWELS.put("RU", "ඎ");
        
        VOWELS.put("a", "අ"); VOWELS.put("A", "ඇ");
        VOWELS.put("i", "ඉ"); VOWELS.put("I", "ඊ");
        VOWELS.put("u", "උ"); VOWELS.put("U", "ඌ");
        VOWELS.put("e", "එ"); VOWELS.put("E", "ඒ");
        VOWELS.put("o", "ඔ"); VOWELS.put("O", "ඕ");
    }
    
    // Vowel modifiers (after consonant - replaces hal kirima)
    private static final Map<String, String> MODIFIERS = new HashMap<>();
    static {
        MODIFIERS.put("aa", "ා"); MODIFIERS.put("AA", "ෑ"); MODIFIERS.put("Aa", "ා");
        MODIFIERS.put("ae", "ැ"); MODIFIERS.put("AE", "ෑ"); MODIFIERS.put("Ae", "ැ");
        MODIFIERS.put("aE", "ෑ");
        MODIFIERS.put("ii", "ී"); MODIFIERS.put("II", "ී"); MODIFIERS.put("ee", "ී");
        MODIFIERS.put("uu", "ූ"); MODIFIERS.put("UU", "ූ"); MODIFIERS.put("oo", "ූ");
        MODIFIERS.put("ea", "ේ"); MODIFIERS.put("EA", "ේ");
        MODIFIERS.put("oe", "ෝ"); MODIFIERS.put("OE", "ෝ");
        MODIFIERS.put("ai", "ෛ"); MODIFIERS.put("Ai", "ෛ"); MODIFIERS.put("AI", "ෛ");
        MODIFIERS.put("au", "ෞ"); MODIFIERS.put("Au", "ෞ"); MODIFIERS.put("AU", "ෞ");
        MODIFIERS.put("ru", "ෘ"); MODIFIERS.put("Ru", "ෘ"); MODIFIERS.put("RU", "ෲ");
        
        MODIFIERS.put("a", ""); // Just remove hal kirima
        MODIFIERS.put("A", "ැ");
        MODIFIERS.put("i", "ි"); MODIFIERS.put("I", "ී");
        MODIFIERS.put("u", "ු"); MODIFIERS.put("U", "ූ");
        MODIFIERS.put("e", "ෙ"); MODIFIERS.put("E", "ේ");
        MODIFIERS.put("o", "ො"); MODIFIERS.put("O", "ෝ");
    }
    
    // Special sequences
    private static final String VOWEL_CHARS = "aeiouAEIOU";
    private static final String CONSONANT_STARTERS = "kKgGcCjJtTdDnNpPbBmMyYrRlLwWvVsShHfFxXzZ";
    
    // ═══════════════════════════════════════════════════════════════════
    // STATE VARIABLES
    // ═══════════════════════════════════════════════════════════════════
    private FrameLayout rootContainer;
    private LinearLayout keyboardView;
    private LinearLayout emojiRowView;
    private Handler handler;
    private Vibrator vibrator;
    private KeyboardSettings settings;
    
    // Key Preview
    private PopupWindow keyPreviewPopup;
    private TextView keyPreviewText;
    private View currentPreviewAnchor;
    
    // Keyboard State
    private boolean isShift = false;
    private boolean isCaps = false;
    private boolean isNumbers = false;
    private boolean isSymbols = false;
    private boolean isSinhalaMode = false;
    
    // Repeat handling
    private boolean isRepeating = false;
    private Runnable repeatRunnable;
    
    // Singlish state
    private StringBuilder singlishBuffer = new StringBuilder();
    private boolean lastOutputWasConsonant = false;
    
    private int navigationBarHeight = 0;
    
    // Touch tracking
    private List<KeyInfo> keyInfoList = new ArrayList<>();
    private KeyInfo currentPressedKey = null;
    private float lastTouchX = 0;
    private float lastTouchY = 0;
    
    private static class KeyInfo {
        String key;
        View view;
        Rect bounds = new Rect();
        
        KeyInfo(String key, View view) {
            this.key = key;
            this.view = view;
        }
        
        void updateBounds() {
            int[] loc = new int[2];
            view.getLocationOnScreen(loc);
            bounds.set(loc[0], loc[1], loc[0] + view.getWidth(), loc[1] + view.getHeight());
        }
        
        boolean contains(float x, float y) {
            return bounds.contains((int) x, (int) y);
        }
        
        float distanceTo(float x, float y) {
            float cx = bounds.centerX();
            float cy = bounds.centerY();
            return (float) Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // BROADCAST RECEIVER
    // ═══════════════════════════════════════════════════════════════════
    private BroadcastReceiver settingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            
            if (KeyboardSettings.ACTION_SETTINGS_CHANGED.equals(intent.getAction())) {
                handler.post(() -> {
                    loadSettings();
                    rebuildKeyboard();
                });
            } else if (KeyboardSettings.ACTION_TYPE_TEXT.equals(intent.getAction())) {
                String text = intent.getStringExtra("text");
                if (text != null) {
                    handler.post(() -> commitTextDirect(text));
                }
            }
        }
    };
    
    // ═══════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════
    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        settings = new KeyboardSettings(this);
        loadSettings();
        calculateNavBarHeight();
        initKeyPreview();
        
        try {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        } catch (Exception e) {
            Log.e(TAG, "Vibrator init failed", e);
        }
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(KeyboardSettings.ACTION_SETTINGS_CHANGED);
        filter.addAction(KeyboardSettings.ACTION_TYPE_TEXT);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(settingsReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(settingsReceiver, filter);
        }
    }
    
    @Override
    public void onDestroy() {
        stopRepeat();
        dismissKeyPreview();
        try {
            unregisterReceiver(settingsReceiver);
        } catch (Exception e) {}
        super.onDestroy();
    }
    
    private void initKeyPreview() {
        keyPreviewText = new TextView(this);
        keyPreviewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        keyPreviewText.setTextColor(Color.WHITE);
        keyPreviewText.setTypeface(Typeface.DEFAULT_BOLD);
        keyPreviewText.setGravity(Gravity.CENTER);
        keyPreviewText.setPadding(dp(24), dp(14), dp(24), dp(14));
        keyPreviewText.setIncludeFontPadding(false);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#404040"));
        bg.setCornerRadius(dp(12));
        keyPreviewText.setBackground(bg);
        
        keyPreviewPopup = new PopupWindow(
            keyPreviewText,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        );
        keyPreviewPopup.setClippingEnabled(false);
        keyPreviewPopup.setTouchable(false);
        keyPreviewPopup.setOutsideTouchable(false);
    }
    
    private void calculateNavBarHeight() {
        try {
            int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                navigationBarHeight = getResources().getDimensionPixelSize(resourceId);
            }
            if (navigationBarHeight == 0) {
                navigationBarHeight = dp(48);
            }
        } catch (Exception e) {
            navigationBarHeight = dp(48);
        }
    }
    
    private void loadSettings() {
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
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // INPUT VIEW CREATION
    // ═══════════════════════════════════════════════════════════════════
    @Override
    public View onCreateInputView() {
        loadSettings();
        calculateNavBarHeight();
        
        rootContainer = new FrameLayout(this);
        rootContainer.setBackgroundColor(parseColor(colorBackground));
        
        // Main content layout
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(parseColor(colorBackground));
        
        if (showEmojiRow) {
            emojiRowView = createEmojiRow();
            mainLayout.addView(emojiRowView);
        }
        
        keyboardView = createKeyboard();
        mainLayout.addView(keyboardView);
        
        FrameLayout.LayoutParams mainParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        mainParams.gravity = Gravity.BOTTOM;
        rootContainer.addView(mainLayout, mainParams);
        
        // Invisible touch layer on top - handles all touches
        View touchLayer = new View(this);
        touchLayer.setBackgroundColor(Color.TRANSPARENT);
        FrameLayout.LayoutParams touchParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        touchLayer.setLayoutParams(touchParams);
        touchLayer.setOnTouchListener(this::handleGlobalTouch);
        rootContainer.addView(touchLayer);
        
        // Calculate total height
        int emojiHeight = showEmojiRow ? dp(44) : 0;
        int totalHeight = emojiHeight + dp(keyboardHeight) + navigationBarHeight;
        
        rootContainer.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            totalHeight
        ));
        rootContainer.setPadding(0, 0, 0, navigationBarHeight);
        
        // Update key bounds after layout
        rootContainer.post(this::updateAllKeyBounds);
        
        return rootContainer;
    }
    
    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        
        // Reset state
        isShift = false;
        isCaps = false;
        isSymbols = false;
        singlishBuffer.setLength(0);
        lastOutputWasConsonant = false;
        
        // Check input type
        if (info != null) {
            int inputClass = info.inputType & EditorInfo.TYPE_MASK_CLASS;
            isNumbers = (inputClass == EditorInfo.TYPE_CLASS_NUMBER || 
                        inputClass == EditorInfo.TYPE_CLASS_PHONE);
        }
        
        loadSettings();
        rebuildKeyboard();
    }
    
    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        dismissKeyPreview();
        flushSinglishBuffer();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // TOUCH HANDLING - SMOOTH & RESPONSIVE
    // ═══════════════════════════════════════════════════════════════════
    private void updateAllKeyBounds() {
        for (KeyInfo ki : keyInfoList) {
            ki.updateBounds();
        }
    }
    
    private KeyInfo findKeyAt(float x, float y) {
        // First, check direct hit
        for (KeyInfo ki : keyInfoList) {
            if (ki.contains(x, y)) {
                return ki;
            }
        }
        
        // If no direct hit, find nearest key within threshold
        float minDist = Float.MAX_VALUE;
        KeyInfo nearest = null;
        float maxDistance = dp(35); // Maximum distance to register as nearby touch
        
        for (KeyInfo ki : keyInfoList) {
            float dist = ki.distanceTo(x, y);
            if (dist < minDist && dist < maxDistance) {
                minDist = dist;
                nearest = ki;
            }
        }
        
        return nearest;
    }
    
    private boolean handleGlobalTouch(View v, MotionEvent event) {
        float x = event.getRawX();
        float y = event.getRawY();
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;
                
                KeyInfo key = findKeyAt(x, y);
                if (key != null) {
                    currentPressedKey = key;
                    onKeyPressed(key);
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                // Check if moved to different key
                KeyInfo moveKey = findKeyAt(x, y);
                if (moveKey != null && moveKey != currentPressedKey) {
                    // Release old key
                    if (currentPressedKey != null) {
                        onKeyReleased(currentPressedKey, false);
                    }
                    // Press new key
                    currentPressedKey = moveKey;
                    onKeyPressed(moveKey);
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (currentPressedKey != null) {
                    onKeyReleased(currentPressedKey, event.getAction() == MotionEvent.ACTION_UP);
                }
                currentPressedKey = null;
                dismissKeyPreview();
                stopRepeat();
                resetAllKeyVisuals();
                break;
        }
        
        return true;
    }
    
    private void onKeyPressed(KeyInfo ki) {
        // Visual feedback
        applyPressedVisual(ki);
        
        // Vibration
        vibrate();
        
        // Show preview popup
        showKeyPreview(ki);
        
        // Process key action
        processKeyPress(ki.key);
        
        // Start repeat for repeatable keys
        if (ki.key.equals("⌫") || ki.key.equals("SPACE")) {
            startRepeat(ki.key);
        }
    }
    
    private void onKeyReleased(KeyInfo ki, boolean committed) {
        resetKeyVisual(ki);
        stopRepeat();
    }
    
    private void applyPressedVisual(KeyInfo ki) {
        if (ki.view != null) {
            ki.view.setAlpha(0.6f);
            ki.view.setScaleX(0.95f);
            ki.view.setScaleY(0.95f);
        }
    }
    
    private void resetKeyVisual(KeyInfo ki) {
        if (ki.view != null) {
            ki.view.setAlpha(1.0f);
            ki.view.setScaleX(1.0f);
            ki.view.setScaleY(1.0f);
        }
    }
    
    private void resetAllKeyVisuals() {
        for (KeyInfo ki : keyInfoList) {
            resetKeyVisual(ki);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // KEY PREVIEW POPUP
    // ═══════════════════════════════════════════════════════════════════
    private void showKeyPreview(KeyInfo ki) {
        // Don't show preview for special keys
        if (isSpecialKey(ki.key)) {
            dismissKeyPreview();
            return;
        }
        
        String displayText = getKeyDisplayText(ki.key);
        if (displayText.isEmpty() || displayText.equals("GD Keyboard")) {
            dismissKeyPreview();
            return;
        }
        
        // For Sinhala mode, show the Sinhala character
        if (isSinhalaMode && ki.key.length() == 1 && Character.isLetter(ki.key.charAt(0))) {
            Map<String, String> labels = (isShift || isCaps) ? SINHALA_LABELS_SHIFT : SINHALA_LABELS;
            String sinhalaChar = labels.get(ki.key.toLowerCase());
            if (sinhalaChar != null) {
                displayText = sinhalaChar;
            }
        }
        
        keyPreviewText.setText(displayText);
        
        // Measure the preview
        keyPreviewText.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        
        int previewWidth = Math.max(keyPreviewText.getMeasuredWidth(), dp(56));
        int previewHeight = keyPreviewText.getMeasuredHeight();
        
        // Position above the key
        int[] keyLocation = new int[2];
        ki.view.getLocationOnScreen(keyLocation);
        
        int previewX = keyLocation[0] + (ki.view.getWidth() / 2) - (previewWidth / 2);
        int previewY = keyLocation[1] - previewHeight - dp(12);
        
        // Ensure preview stays on screen
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        if (previewX < dp(4)) previewX = dp(4);
        if (previewX + previewWidth > screenWidth - dp(4)) {
            previewX = screenWidth - previewWidth - dp(4);
        }
        if (previewY < dp(4)) previewY = dp(4);
        
        try {
            if (keyPreviewPopup.isShowing()) {
                keyPreviewPopup.update(previewX, previewY, previewWidth, previewHeight);
            } else {
                keyPreviewPopup.setWidth(previewWidth);
                keyPreviewPopup.setHeight(previewHeight);
                keyPreviewPopup.showAtLocation(
                    rootContainer,
                    Gravity.NO_GRAVITY,
                    previewX,
                    previewY
                );
            }
            currentPreviewAnchor = ki.view;
        } catch (Exception e) {
            Log.e(TAG, "Error showing key preview", e);
        }
    }
    
    private void dismissKeyPreview() {
        try {
            if (keyPreviewPopup != null && keyPreviewPopup.isShowing()) {
                keyPreviewPopup.dismiss();
            }
        } catch (Exception e) {}
        currentPreviewAnchor = null;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // KEYBOARD BUILDING
    // ═══════════════════════════════════════════════════════════════════
    private LinearLayout createEmojiRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44)));
        row.setPadding(dp(4), dp(4), dp(4), dp(4));
        row.setBackgroundColor(parseColor(colorKeySpecial));
        
        String emojis = settings.getQuickEmojis();
        for (String emoji : emojis.split(",")) {
            final String e = emoji.trim();
            if (e.isEmpty()) continue;
            
            TextView tv = new TextView(this);
            tv.setText(e);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 
                LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            params.setMargins(dp(2), 0, dp(2), 0);
            tv.setLayoutParams(params);
            
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(parseColor(colorKeyNormal));
            bg.setCornerRadius(dp(8));
            tv.setBackground(bg);
            
            tv.setOnClickListener(view -> {
                vibrate();
                commitTextDirect(e);
            });
            
            row.addView(tv);
        }
        
        return row;
    }
    
    private LinearLayout createKeyboard() {
        keyInfoList.clear();
        
        LinearLayout keyboard = new LinearLayout(this);
        keyboard.setOrientation(LinearLayout.VERTICAL);
        keyboard.setBackgroundColor(parseColor(colorBackground));
        keyboard.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(keyboardHeight)));
        keyboard.setPadding(dp(3), dp(6), dp(3), dp(6));
        
        String[][] layout;
        if (isSymbols) {
            layout = LAYOUT_SYMBOLS;
        } else if (isNumbers) {
            layout = LAYOUT_NUMBERS;
        } else {
            layout = LAYOUT_LETTERS;
        }
        
        for (int rowIndex = 0; rowIndex < layout.length; rowIndex++) {
            keyboard.addView(createKeyRow(layout[rowIndex], rowIndex));
        }
        
        return keyboard;
    }
    
    private LinearLayout createKeyRow(String[] keys, int rowIndex) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        
        // Add side padding for middle row
        int sidePad = (rowIndex == 1) ? dp(14) : 0;
        row.setPadding(sidePad, dp(2), sidePad, dp(2));
        
        for (String key : keys) {
            row.addView(createKeyView(key));
        }
        
        return row;
    }
    
    private View createKeyView(String key) {
        FrameLayout container = new FrameLayout(this);
        TextView keyText = new TextView(this);
        
        keyText.setGravity(Gravity.CENTER);
        keyText.setTypeface(Typeface.DEFAULT_BOLD);
        keyText.setIncludeFontPadding(false);
        
        // Determine display text and styling
        String displayText = getKeyDisplayText(key);
        int textColor = parseColor(colorText);
        float textSize = keyTextSize;
        
        // Special key styling
        switch (key) {
            case "↵":
                displayText = "↵";
                textColor = Color.WHITE;
                textSize = 22;
                break;
            case "⇧":
                textSize = 24;
                if (isCaps) {
                    displayText = "⇪";
                    textColor = Color.parseColor("#10b981");
                } else if (isShift) {
                    displayText = "⬆";
                    textColor = Color.parseColor("#3b82f6");
                } else {
                    displayText = "⇧";
                }
                break;
            case "⌫":
                displayText = "⌫";
                textSize = 22;
                break;
            case "SPACE":
                displayText = "GD Keyboard";
                textSize = 10;
                textColor = Color.parseColor("#666666");
                break;
            case "🌐":
                displayText = isSinhalaMode ? "සිං" : "EN";
                textSize = 12;
                textColor = isSinhalaMode ? Color.parseColor("#10b981") : Color.parseColor("#3b82f6");
                break;
            case "✨":
                displayText = "✨";
                textSize = 18;
                break;
            case "123":
            case "ABC":
            case "#+=":
                textSize = 13;
                break;
        }
        
        keyText.setText(displayText);
        keyText.setTextColor(textColor);
        keyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        
        // Add Sinhala label in corner for letter keys
        if (isSinhalaMode && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            Map<String, String> labels = (isShift || isCaps) ? SINHALA_LABELS_SHIFT : SINHALA_LABELS;
            String sinhalaLabel = labels.get(key.toLowerCase());
            if (sinhalaLabel != null) {
                TextView labelView = new TextView(this);
                labelView.setText(sinhalaLabel);
                labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
                labelView.setTextColor(Color.parseColor("#888888"));
                labelView.setIncludeFontPadding(false);
                
                FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                );
                labelParams.gravity = Gravity.TOP | Gravity.END;
                labelParams.setMargins(0, dp(3), dp(4), 0);
                container.addView(labelView, labelParams);
            }
        }
        
        // Container layout params
        float weight = getKeyWeight(key);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.MATCH_PARENT, weight);
        containerParams.setMargins(dp(keyGap), dp(keyGap), dp(keyGap), dp(keyGap));
        container.setLayoutParams(containerParams);
        
        // Add text view
        container.addView(keyText, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
        
        // Background
        container.setBackground(createKeyBackground(key));
        
        // Store key info for touch handling
        keyInfoList.add(new KeyInfo(key, container));
        
        return container;
    }
    
    private String getKeyDisplayText(String key) {
        if (key.equals("SPACE")) return "GD Keyboard";
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
        switch (key) {
            case "SPACE": return 3.5f;
            case "⇧":
            case "⌫": return 1.5f;
            case "↵":
            case "123":
            case "ABC":
            case "#+=": return 1.3f;
            case "🌐":
            case "✨": return 1.0f;
            default: return 1.0f;
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
            case "⌫":
            case "123":
            case "ABC":
            case "#+=":
            case "🌐":
            case "✨":
                color = colorKeySpecial;
                break;
            case "SPACE":
                color = colorKeySpace;
                break;
            default:
                color = colorKeyNormal;
        }
        
        bg.setColor(parseColor(color));
        return bg;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // KEY PROCESSING
    // ═══════════════════════════════════════════════════════════════════
    private void processKeyPress(String key) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        
        switch (key) {
            case "⇧":
                handleShiftKey();
                break;
            case "⌫":
                handleBackspaceKey(ic);
                break;
            case "↵":
                flushSinglishBuffer();
                handleEnterKey(ic);
                break;
            case "SPACE":
                flushSinglishBuffer();
                ic.commitText(" ", 1);
                lastOutputWasConsonant = false;
                autoResetShift();
                break;
            case "123":
                flushSinglishBuffer();
                isNumbers = true;
                isSymbols = false;
                rebuildKeyboard();
                break;
            case "ABC":
                flushSinglishBuffer();
                isNumbers = false;
                isSymbols = false;
                rebuildKeyboard();
                break;
            case "#+=":
                flushSinglishBuffer();
                isSymbols = true;
                isNumbers = false;
                rebuildKeyboard();
                break;
            case "🌐":
                flushSinglishBuffer();
                isSinhalaMode = !isSinhalaMode;
                singlishBuffer.setLength(0);
                lastOutputWasConsonant = false;
                rebuildKeyboard();
                break;
            case "✨":
                flushSinglishBuffer();
                openPopupWindow();
                break;
            default:
                handleCharacterKey(ic, key);
        }
    }
    
    private void handleShiftKey() {
        if (isCaps) {
            isCaps = false;
            isShift = false;
        } else if (isShift) {
            isCaps = true;
        } else {
            isShift = true;
        }
        rebuildKeyboard();
    }
    
    private void handleBackspaceKey(InputConnection ic) {
        if (singlishBuffer.length() > 0) {
            // Delete from buffer
            singlishBuffer.deleteCharAt(singlishBuffer.length() - 1);
            if (singlishBuffer.length() == 0) {
                lastOutputWasConsonant = false;
            }
        } else {
            // Delete from input
            ic.deleteSurroundingText(1, 0);
            lastOutputWasConsonant = false;
        }
    }
    
    private void handleEnterKey(InputConnection ic) {
        EditorInfo editorInfo = getCurrentInputEditorInfo();
        if (editorInfo != null) {
            int imeAction = editorInfo.imeOptions & EditorInfo.IME_MASK_ACTION;
            if (imeAction == EditorInfo.IME_ACTION_NONE || 
                imeAction == EditorInfo.IME_ACTION_UNSPECIFIED) {
                ic.commitText("\n", 1);
            } else {
                ic.performEditorAction(imeAction);
            }
        } else {
            ic.commitText("\n", 1);
        }
    }
    
    private void handleCharacterKey(InputConnection ic, String key) {
        String character = key;
        
        // Apply shift
        if ((isShift || isCaps) && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            character = key.toUpperCase();
        }
        
        if (isSinhalaMode && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            processSinglishInput(ic, character);
        } else {
            // Direct commit for non-Sinhala
            ic.commitText(character, 1);
            autoResetShift();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SINGLISH ENGINE - OPTIMIZED & FIXED
    // ═══════════════════════════════════════════════════════════════════
    private void processSinglishInput(InputConnection ic, String input) {
        singlishBuffer.append(input);
        processBuffer(ic);
        autoResetShift();
    }
    
    private void processBuffer(InputConnection ic) {
        while (singlishBuffer.length() > 0) {
            String buffer = singlishBuffer.toString();
            boolean matched = false;
            
            // If last output was consonant with hal, check for vowel modifiers first
            if (lastOutputWasConsonant) {
                // Try 2-char modifiers first (aa, ii, uu, etc.)
                if (buffer.length() >= 2) {
                    String twoChar = buffer.substring(0, 2);
                    if (MODIFIERS.containsKey(twoChar)) {
                        // Check if could extend to longer
                        if (buffer.length() == 2 && couldExtendVowel(twoChar)) {
                            return; // Wait for more input
                        }
                        applyModifier(ic, twoChar);
                        singlishBuffer.delete(0, 2);
                        matched = true;
                        continue;
                    }
                }
                
                // Try 1-char modifiers
                if (buffer.length() >= 1) {
                    String oneChar = buffer.substring(0, 1);
                    if (MODIFIERS.containsKey(oneChar)) {
                        // Check if could extend
                        if (buffer.length() == 1 && couldExtendVowel(oneChar)) {
                            return; // Wait for more input
                        }
                        applyModifier(ic, oneChar);
                        singlishBuffer.delete(0, 1);
                        matched = true;
                        continue;
                    }
                }
            }
            
            // Try consonants (3-char, 2-char, 1-char)
            for (int len = Math.min(3, buffer.length()); len >= 1; len--) {
                String sub = buffer.substring(0, len);
                
                if (CONSONANTS.containsKey(sub)) {
                    // Check if could extend to longer consonant
                    if (buffer.length() == len && couldExtendConsonant(sub)) {
                        return; // Wait for more input
                    }
                    
                    String consonant = CONSONANTS.get(sub);
                    ic.commitText(consonant + "්", 1); // Consonant + hal kirima
                    singlishBuffer.delete(0, len);
                    lastOutputWasConsonant = true;
                    matched = true;
                    break;
                }
            }
            
            if (matched) continue;
            
            // Try standalone vowels (2-char, 1-char)
            if (!lastOutputWasConsonant) {
                for (int len = Math.min(2, buffer.length()); len >= 1; len--) {
                    String sub = buffer.substring(0, len);
                    
                    if (VOWELS.containsKey(sub)) {
                        // Check if could extend
                        if (buffer.length() == len && couldExtendVowel(sub)) {
                            return; // Wait for more input
                        }
                        
                        ic.commitText(VOWELS.get(sub), 1);
                        singlishBuffer.delete(0, len);
                        lastOutputWasConsonant = false;
                        matched = true;
                        break;
                    }
                }
            }
            
            if (matched) continue;
            
            // No match found
            if (buffer.length() >= 3) {
                // Output first character as-is and continue
                ic.commitText(String.valueOf(buffer.charAt(0)), 1);
                singlishBuffer.deleteCharAt(0);
                lastOutputWasConsonant = false;
            } else {
                // Wait for more input
                return;
            }
        }
    }
    
    private void applyModifier(InputConnection ic, String vowelKey) {
        String modifier = MODIFIERS.get(vowelKey);
        
        // Remove hal kirima (්)
        ic.deleteSurroundingText(1, 0);
        
        // Add modifier if not empty (single 'a' just removes hal)
        if (modifier != null && !modifier.isEmpty()) {
            ic.commitText(modifier, 1);
        }
        
        lastOutputWasConsonant = false;
    }
    
    private boolean couldExtendVowel(String current) {
        // Check if adding another character could form a valid longer sequence
        String[] extensions = {"a", "e", "i", "o", "u", "A", "E", "I", "O", "U"};
        for (String ext : extensions) {
            String test = current + ext;
            if (MODIFIERS.containsKey(test) || VOWELS.containsKey(test)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean couldExtendConsonant(String current) {
        // Check if adding another character could form a valid longer consonant
        String[] extensions = {"h", "H", "g", "y", "n", "d"};
        for (String ext : extensions) {
            String test = current + ext;
            if (CONSONANTS.containsKey(test)) {
                return true;
            }
        }
        return false;
    }
    
    private void flushSinglishBuffer() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || singlishBuffer.length() == 0) return;
        
        // Force process remaining buffer
        while (singlishBuffer.length() > 0) {
            String buffer = singlishBuffer.toString();
            boolean matched = false;
            
            // Try modifiers if after consonant
            if (lastOutputWasConsonant) {
                for (int len = Math.min(2, buffer.length()); len >= 1; len--) {
                    String sub = buffer.substring(0, len);
                    if (MODIFIERS.containsKey(sub)) {
                        applyModifier(ic, sub);
                        singlishBuffer.delete(0, len);
                        matched = true;
                        break;
                    }
                }
                if (matched) continue;
            }
            
            // Try consonants
            for (int len = Math.min(3, buffer.length()); len >= 1; len--) {
                String sub = buffer.substring(0, len);
                if (CONSONANTS.containsKey(sub)) {
                    ic.commitText(CONSONANTS.get(sub) + "්", 1);
                    singlishBuffer.delete(0, len);
                    lastOutputWasConsonant = true;
                    matched = true;
                    break;
                }
            }
            if (matched) continue;
            
            // Try standalone vowels
            if (!lastOutputWasConsonant) {
                for (int len = Math.min(2, buffer.length()); len >= 1; len--) {
                    String sub = buffer.substring(0, len);
                    if (VOWELS.containsKey(sub)) {
                        ic.commitText(VOWELS.get(sub), 1);
                        singlishBuffer.delete(0, len);
                        matched = true;
                        break;
                    }
                }
            }
            if (matched) continue;
            
            // Output as-is
            ic.commitText(String.valueOf(buffer.charAt(0)), 1);
            singlishBuffer.deleteCharAt(0);
            lastOutputWasConsonant = false;
        }
    }
    
    private void commitTextDirect(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            flushSinglishBuffer();
            ic.commitText(text, 1);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════
    private void autoResetShift() {
        if (isShift && !isCaps) {
            isShift = false;
            rebuildKeyboard();
        }
    }
    
    private void startRepeat(String key) {
        isRepeating = true;
        repeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRepeating) {
                    processKeyPress(key);
                    vibrate();
                    handler.postDelayed(this, repeatInterval);
                }
            }
        };
        handler.postDelayed(repeatRunnable, longPressDelay);
    }
    
    private void stopRepeat() {
        isRepeating = false;
        if (repeatRunnable != null) {
            handler.removeCallbacks(repeatRunnable);
            repeatRunnable = null;
        }
    }
    
    private void vibrate() {
        if (!vibrateEnabled || vibrator == null) return;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(
                    vibrateDuration, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(vibrateDuration);
            }
        } catch (Exception e) {}
    }
    
    private void openPopupWindow() {
        try {
            Intent intent = new Intent(this, PopupActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening popup", e);
        }
    }
    
    private void rebuildKeyboard() {
        if (rootContainer == null) return;
        
        dismissKeyPreview();
        keyInfoList.clear();
        rootContainer.removeAllViews();
        
        // Rebuild main layout
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(parseColor(colorBackground));
        
        if (showEmojiRow) {
            emojiRowView = createEmojiRow();
            mainLayout.addView(emojiRowView);
        }
        
        keyboardView = createKeyboard();
        mainLayout.addView(keyboardView);
        
        FrameLayout.LayoutParams mainParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        mainParams.gravity = Gravity.BOTTOM;
        rootContainer.addView(mainLayout, mainParams);
        
        // Add touch layer
        View touchLayer = new View(this);
        touchLayer.setBackgroundColor(Color.TRANSPARENT);
        touchLayer.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
        touchLayer.setOnTouchListener(this::handleGlobalTouch);
        rootContainer.addView(touchLayer);
        
        // Update height
        int emojiHeight = showEmojiRow ? dp(44) : 0;
        int totalHeight = emojiHeight + dp(keyboardHeight) + navigationBarHeight;
        
        ViewGroup.LayoutParams rootParams = rootContainer.getLayoutParams();
        if (rootParams != null) {
            rootParams.height = totalHeight;
            rootContainer.setLayoutParams(rootParams);
        }
        rootContainer.setPadding(0, 0, 0, navigationBarHeight);
        rootContainer.setBackgroundColor(parseColor(colorBackground));
        
        // Update key bounds after layout
        rootContainer.post(this::updateAllKeyBounds);
    }
    
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
    
    private int parseColor(String colorString) {
        try {
            return Color.parseColor(colorString);
        } catch (Exception e) {
            return Color.BLACK;
        }
    }
}