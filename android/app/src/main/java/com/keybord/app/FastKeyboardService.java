package com.keybord.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
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
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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
    // SINGLISH ENGINE - FIXED MAPPINGS
    // ═══════════════════════════════════════════════════════════════════
    
    // Consonants → Sinhala base letter
    private static final Map<String, String> CONSONANTS = new HashMap<>();
    static {
        // 3-letter combinations
        CONSONANTS.put("ndh", "ඳ"); CONSONANTS.put("nDh", "ඳ");
        CONSONANTS.put("ngh", "ඟ");
        CONSONANTS.put("mbh", "ඹ");
        CONSONANTS.put("thth", "ත්ථ");
        
        // 2-letter combinations
        CONSONANTS.put("kh", "ඛ"); CONSONANTS.put("Kh", "ඛ");
        CONSONANTS.put("gh", "ඝ"); CONSONANTS.put("Gh", "ඝ");
        CONSONANTS.put("ng", "ඟ"); CONSONANTS.put("Ng", "ඟ");
        CONSONANTS.put("ch", "ච"); CONSONANTS.put("Ch", "ඡ");
        CONSONANTS.put("jh", "ඣ"); CONSONANTS.put("Jh", "ඣ");
        CONSONANTS.put("ny", "ඤ"); CONSONANTS.put("Ny", "ඤ");
        CONSONANTS.put("jn", "ඥ"); CONSONANTS.put("Jn", "ඥ");
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
        CONSONANTS.put("LH", "ළ");
        
        // 1-letter consonants
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
        CONSONANTS.put("z", "ඞ"); CONSONANTS.put("Z", "ඞ");
    }
    
    // Standalone vowels (word start / after space)
    private static final Map<String, String> VOWELS_STANDALONE = new HashMap<>();
    static {
        // Long combinations first (3-char)
        VOWELS_STANDALONE.put("aae", "ඈ"); VOWELS_STANDALONE.put("AAe", "ඈ");
        
        // 2-char combinations
        VOWELS_STANDALONE.put("aa", "ආ"); VOWELS_STANDALONE.put("Aa", "ආ"); VOWELS_STANDALONE.put("AA", "ආ");
        VOWELS_STANDALONE.put("ae", "ඇ"); VOWELS_STANDALONE.put("Ae", "ඇ"); VOWELS_STANDALONE.put("AE", "ඈ");
        VOWELS_STANDALONE.put("ii", "ඊ"); VOWELS_STANDALONE.put("II", "ඊ");
        VOWELS_STANDALONE.put("ee", "ඒ"); VOWELS_STANDALONE.put("EE", "ඒ"); // FIXED: ee = ē not ī
        VOWELS_STANDALONE.put("uu", "ඌ"); VOWELS_STANDALONE.put("UU", "ඌ");
        VOWELS_STANDALONE.put("oo", "ඕ"); VOWELS_STANDALONE.put("OO", "ඕ"); // FIXED: oo = ō not ū
        VOWELS_STANDALONE.put("au", "ඖ"); VOWELS_STANDALONE.put("Au", "ඖ"); VOWELS_STANDALONE.put("AU", "ඖ");
        VOWELS_STANDALONE.put("ai", "ඓ"); VOWELS_STANDALONE.put("Ai", "ඓ"); VOWELS_STANDALONE.put("AI", "ඓ");
        VOWELS_STANDALONE.put("ru", "ඍ"); VOWELS_STANDALONE.put("Ru", "ඍ");
        
        // 1-char vowels
        VOWELS_STANDALONE.put("a", "අ"); VOWELS_STANDALONE.put("A", "ඇ");
        VOWELS_STANDALONE.put("i", "ඉ"); VOWELS_STANDALONE.put("I", "ඊ");
        VOWELS_STANDALONE.put("u", "උ"); VOWELS_STANDALONE.put("U", "ඌ");
        VOWELS_STANDALONE.put("e", "එ"); VOWELS_STANDALONE.put("E", "ඒ");
        VOWELS_STANDALONE.put("o", "ඔ"); VOWELS_STANDALONE.put("O", "ඕ");
    }
    
    // Vowel modifiers (after consonant - replaces hal kirima)
    private static final Map<String, String> VOWEL_MODIFIERS = new HashMap<>();
    static {
        // Long combinations first (3-char)
        VOWEL_MODIFIERS.put("aae", "ෑ"); VOWEL_MODIFIERS.put("AAe", "ෑ");
        
        // 2-char combinations  
        VOWEL_MODIFIERS.put("aa", "ා"); VOWEL_MODIFIERS.put("Aa", "ා"); VOWEL_MODIFIERS.put("AA", "ා");
        VOWEL_MODIFIERS.put("ae", "ැ"); VOWEL_MODIFIERS.put("Ae", "ැ"); VOWEL_MODIFIERS.put("AE", "ෑ");
        VOWEL_MODIFIERS.put("ii", "ී"); VOWEL_MODIFIERS.put("II", "ී");
        VOWEL_MODIFIERS.put("ee", "ේ"); VOWEL_MODIFIERS.put("EE", "ේ"); // FIXED: ee = ē not ī
        VOWEL_MODIFIERS.put("uu", "ූ"); VOWEL_MODIFIERS.put("UU", "ූ");
        VOWEL_MODIFIERS.put("oo", "ෝ"); VOWEL_MODIFIERS.put("OO", "ෝ"); // FIXED: oo = ō not ū
        VOWEL_MODIFIERS.put("au", "ෞ"); VOWEL_MODIFIERS.put("Au", "ෞ"); VOWEL_MODIFIERS.put("AU", "ෞ");
        VOWEL_MODIFIERS.put("ai", "ෛ"); VOWEL_MODIFIERS.put("Ai", "ෛ"); VOWEL_MODIFIERS.put("AI", "ෛ");
        VOWEL_MODIFIERS.put("ru", "ෘ"); VOWEL_MODIFIERS.put("Ru", "ෘ");
        
        // 1-char modifiers
        VOWEL_MODIFIERS.put("a", ""); // Just remove hal - IMPORTANT
        VOWEL_MODIFIERS.put("A", "ැ");
        VOWEL_MODIFIERS.put("i", "ි"); VOWEL_MODIFIERS.put("I", "ී");
        VOWEL_MODIFIERS.put("u", "ු"); VOWEL_MODIFIERS.put("U", "ූ");
        VOWEL_MODIFIERS.put("e", "ෙ"); VOWEL_MODIFIERS.put("E", "ේ");
        VOWEL_MODIFIERS.put("o", "ො"); VOWEL_MODIFIERS.put("O", "ෝ");
    }
    
    // Characters that can extend
    private static final String VOWEL_CHARS = "aeiouAEIOU";
    
    // ═══════════════════════════════════════════════════════════════════
    // STATE VARIABLES
    // ═══════════════════════════════════════════════════════════════════
    private FrameLayout rootContainer;
    private LinearLayout keyboardContainer;
    private LinearLayout keyboardView;
    private LinearLayout emojiRowView;
    private TextView keyPreviewView;
    private Handler handler;
    private Vibrator vibrator;
    private KeyboardSettings settings;
    
    // Keyboard State
    private boolean isShift = false;
    private boolean isCaps = false;
    private boolean isNumbers = false;
    private boolean isSymbols = false;
    private boolean isSinhalaMode = false;
    
    // Repeat handling
    private boolean isRepeating = false;
    private Runnable repeatRunnable;
    
    // Singlish state - Simplified
    private boolean lastWasConsonantWithHal = false;
    
    // Touch tracking
    private List<KeyInfo> keyInfoList = new ArrayList<>();
    private KeyInfo currentPressedKey = null;
    private long lastSpecialKeyTime = 0;
    private static final long SPECIAL_KEY_DEBOUNCE = 200; // ms
    
    private int navigationBarHeight = 0;
    
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
        
        boolean containsPoint(float x, float y) {
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
        
        try {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        } catch (Exception e) {}
        
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
        try { unregisterReceiver(settingsReceiver); } catch (Exception e) {}
        super.onDestroy();
    }
    
    private void calculateNavBarHeight() {
        try {
            int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                navigationBarHeight = getResources().getDimensionPixelSize(resourceId);
            }
        } catch (Exception e) {}
        if (navigationBarHeight == 0) navigationBarHeight = dp(48);
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
        
        // Root container
        rootContainer = new FrameLayout(this);
        rootContainer.setBackgroundColor(parseColor(colorBackground));
        
        // Keyboard container (emoji row + keyboard)
        keyboardContainer = new LinearLayout(this);
        keyboardContainer.setOrientation(LinearLayout.VERTICAL);
        keyboardContainer.setBackgroundColor(parseColor(colorBackground));
        
        if (showEmojiRow) {
            emojiRowView = createEmojiRow();
            keyboardContainer.addView(emojiRowView);
        }
        
        keyboardView = createKeyboard();
        keyboardContainer.addView(keyboardView);
        
        FrameLayout.LayoutParams kbParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        kbParams.gravity = Gravity.BOTTOM;
        rootContainer.addView(keyboardContainer, kbParams);
        
        // Key Preview View (at top, high elevation)
        keyPreviewView = new TextView(this);
        keyPreviewView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
        keyPreviewView.setTextColor(Color.WHITE);
        keyPreviewView.setTypeface(Typeface.DEFAULT_BOLD);
        keyPreviewView.setGravity(Gravity.CENTER);
        keyPreviewView.setPadding(dp(28), dp(16), dp(28), dp(16));
        keyPreviewView.setVisibility(View.GONE);
        keyPreviewView.setElevation(dp(100));
        
        GradientDrawable previewBg = new GradientDrawable();
        previewBg.setColor(Color.parseColor("#505050"));
        previewBg.setCornerRadius(dp(14));
        keyPreviewView.setBackground(previewBg);
        
        FrameLayout.LayoutParams previewParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        previewParams.gravity = Gravity.TOP | Gravity.START;
        rootContainer.addView(keyPreviewView, previewParams);
        
        // Touch layer (covers everything, handles all touches)
        View touchLayer = new View(this);
        touchLayer.setBackgroundColor(Color.TRANSPARENT);
        touchLayer.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
        touchLayer.setOnTouchListener(this::handleTouch);
        rootContainer.addView(touchLayer);
        
        // Set total height
        int emojiH = showEmojiRow ? dp(44) : 0;
        int totalHeight = emojiH + dp(keyboardHeight) + navigationBarHeight;
        rootContainer.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, totalHeight));
        rootContainer.setPadding(0, 0, 0, navigationBarHeight);
        
        // Update bounds after layout
        rootContainer.post(this::updateAllKeyBounds);
        
        return rootContainer;
    }
    
    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        
        isShift = false;
        isCaps = false;
        isSymbols = false;
        lastWasConsonantWithHal = false;
        
        if (info != null) {
            int inputClass = info.inputType & EditorInfo.TYPE_MASK_CLASS;
            isNumbers = (inputClass == EditorInfo.TYPE_CLASS_NUMBER || 
                        inputClass == EditorInfo.TYPE_CLASS_PHONE);
        }
        
        loadSettings();
        rebuildKeyboard();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // TOUCH HANDLING
    // ═══════════════════════════════════════════════════════════════════
    private void updateAllKeyBounds() {
        for (KeyInfo ki : keyInfoList) {
            ki.updateBounds();
        }
    }
    
    private KeyInfo findKeyAt(float x, float y) {
        // Direct hit check first
        for (KeyInfo ki : keyInfoList) {
            if (ki.containsPoint(x, y)) {
                return ki;
            }
        }
        
        // Find nearest key within threshold
        float minDist = Float.MAX_VALUE;
        KeyInfo nearest = null;
        float maxDistance = dp(40);
        
        for (KeyInfo ki : keyInfoList) {
            float dist = ki.distanceTo(x, y);
            if (dist < minDist && dist < maxDistance) {
                minDist = dist;
                nearest = ki;
            }
        }
        
        return nearest;
    }
    
    private boolean handleTouch(View v, MotionEvent event) {
        float x = event.getRawX();
        float y = event.getRawY();
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                KeyInfo key = findKeyAt(x, y);
                if (key != null) {
                    currentPressedKey = key;
                    onKeyDown(key);
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                KeyInfo moveKey = findKeyAt(x, y);
                if (moveKey != null && moveKey != currentPressedKey) {
                    // Changed key
                    if (currentPressedKey != null) {
                        resetKeyVisual(currentPressedKey);
                    }
                    currentPressedKey = moveKey;
                    onKeyDown(moveKey);
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                hideKeyPreview();
                stopRepeat();
                resetAllKeyVisuals();
                currentPressedKey = null;
                break;
        }
        
        return true;
    }
    
    private void onKeyDown(KeyInfo ki) {
        // Visual feedback
        applyPressedVisual(ki);
        
        // Vibrate
        vibrate();
        
        // Show preview
        showKeyPreview(ki);
        
        // Process key (with debounce for special keys)
        if (isSpecialActionKey(ki.key)) {
            long now = System.currentTimeMillis();
            if (now - lastSpecialKeyTime > SPECIAL_KEY_DEBOUNCE) {
                lastSpecialKeyTime = now;
                processKey(ki.key);
            }
        } else {
            processKey(ki.key);
        }
        
        // Start repeat for repeatable keys
        if (ki.key.equals("⌫")) {
            startRepeat(ki.key);
        }
    }
    
    private boolean isSpecialActionKey(String key) {
        return key.equals("⇧") || key.equals("123") || key.equals("ABC") || 
               key.equals("#+=") || key.equals("🌐");
    }
    
    private void applyPressedVisual(KeyInfo ki) {
        if (ki.view != null) {
            ki.view.setAlpha(0.6f);
            ki.view.setScaleX(0.93f);
            ki.view.setScaleY(0.93f);
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
    // KEY PREVIEW - Using View (not PopupWindow)
    // ═══════════════════════════════════════════════════════════════════
    private void showKeyPreview(KeyInfo ki) {
        if (isSpecialKey(ki.key)) {
            hideKeyPreview();
            return;
        }
        
        String displayText = getPreviewText(ki.key);
        if (displayText.isEmpty()) {
            hideKeyPreview();
            return;
        }
        
        keyPreviewView.setText(displayText);
        keyPreviewView.setVisibility(View.VISIBLE);
        
        // Measure
        keyPreviewView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        
        int previewW = keyPreviewView.getMeasuredWidth();
        int previewH = keyPreviewView.getMeasuredHeight();
        
        // Position above key
        int[] keyLoc = new int[2];
        ki.view.getLocationInWindow(keyLoc);
        
        int previewX = keyLoc[0] + (ki.view.getWidth() / 2) - (previewW / 2);
        int previewY = keyLoc[1] - previewH - dp(8);
        
        // Keep on screen
        int screenW = getResources().getDisplayMetrics().widthPixels;
        if (previewX < dp(4)) previewX = dp(4);
        if (previewX + previewW > screenW - dp(4)) previewX = screenW - previewW - dp(4);
        if (previewY < dp(4)) previewY = dp(4);
        
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) keyPreviewView.getLayoutParams();
        params.leftMargin = previewX;
        params.topMargin = previewY;
        keyPreviewView.setLayoutParams(params);
    }
    
    private void hideKeyPreview() {
        if (keyPreviewView != null) {
            keyPreviewView.setVisibility(View.GONE);
        }
    }
    
    private String getPreviewText(String key) {
        if (key.equals("SPACE") || key.equals("123") || key.equals("ABC") || key.equals("#+=")) {
            return "";
        }
        
        // For Sinhala mode, show Sinhala character
        if (isSinhalaMode && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            Map<String, String> labels = (isShift || isCaps) ? SINHALA_LABELS_SHIFT : SINHALA_LABELS;
            String sinhala = labels.get(key.toLowerCase());
            if (sinhala != null) return sinhala;
        }
        
        // For letters, show uppercase if shift
        if (key.length() == 1 && Character.isLetter(key.charAt(0))) {
            return (isShift || isCaps) ? key.toUpperCase() : key.toLowerCase();
        }
        
        return key;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // KEYBOARD BUILDING
    // ═══════════════════════════════════════════════════════════════════
    private LinearLayout createEmojiRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(44)));
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
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1f);
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
        
        LinearLayout kb = new LinearLayout(this);
        kb.setOrientation(LinearLayout.VERTICAL);
        kb.setBackgroundColor(parseColor(colorBackground));
        kb.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(keyboardHeight)));
        kb.setPadding(dp(3), dp(6), dp(3), dp(6));
        
        String[][] layout;
        if (isSymbols) layout = LAYOUT_SYMBOLS;
        else if (isNumbers) layout = LAYOUT_NUMBERS;
        else layout = LAYOUT_LETTERS;
        
        for (int i = 0; i < layout.length; i++) {
            kb.addView(createKeyRow(layout[i], i));
        }
        
        return kb;
    }
    
    private LinearLayout createKeyRow(String[] keys, int rowIdx) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        
        int sidePad = (rowIdx == 1) ? dp(14) : 0;
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
        
        String displayText;
        int textColor = parseColor(colorText);
        float textSize = keyTextSize;
        
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
                displayText = isSinhalaMode ? "සිංහල" : "English";
                textSize = 11;
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
                displayText = key;
                textSize = 13;
                break;
            default:
                if (key.length() == 1 && Character.isLetter(key.charAt(0))) {
                    displayText = (isShift || isCaps) ? key.toUpperCase() : key.toLowerCase();
                } else {
                    displayText = key;
                }
        }
        
        keyText.setText(displayText);
        keyText.setTextColor(textColor);
        keyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        
        // Add Sinhala label for letter keys
        if (isSinhalaMode && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            Map<String, String> labels = (isShift || isCaps) ? SINHALA_LABELS_SHIFT : SINHALA_LABELS;
            String sinhalaLabel = labels.get(key.toLowerCase());
            if (sinhalaLabel != null) {
                TextView labelView = new TextView(this);
                labelView.setText(sinhalaLabel);
                labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
                labelView.setTextColor(Color.parseColor("#888888"));
                
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2);
                lp.gravity = Gravity.TOP | Gravity.END;
                lp.setMargins(0, dp(2), dp(3), 0);
                container.addView(labelView, lp);
            }
        }
        
        // Layout params
        float weight = getKeyWeight(key);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -1, weight);
        cp.setMargins(dp(keyGap), dp(keyGap), dp(keyGap), dp(keyGap));
        container.setLayoutParams(cp);
        
        container.addView(keyText, new FrameLayout.LayoutParams(-1, -1));
        container.setBackground(createKeyBackground(key));
        
        keyInfoList.add(new KeyInfo(key, container));
        
        return container;
    }
    
    private boolean isSpecialKey(String key) {
        return "⇧⌫↵SPACE123ABC#+=🌐✨".contains(key) || key.equals("SPACE");
    }
    
    private float getKeyWeight(String key) {
        switch (key) {
            case "SPACE": return 3.5f;
            case "⇧": case "⌫": return 1.5f;
            case "↵": case "123": case "ABC": case "#+=": return 1.3f;
            case "🌐": case "✨": return 1.0f;
            default: return 1.0f;
        }
    }
    
    private GradientDrawable createKeyBackground(String key) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(keyRadius));
        
        String color;
        switch (key) {
            case "↵": color = colorKeyEnter; break;
            case "⇧":
                if (isCaps) color = "#10b981";
                else if (isShift) color = "#3b82f6";
                else color = colorKeySpecial;
                break;
            case "⌫": case "123": case "ABC": case "#+=": case "🌐": case "✨":
                color = colorKeySpecial;
                break;
            case "SPACE": color = colorKeySpace; break;
            default: color = colorKeyNormal;
        }
        
        bg.setColor(parseColor(color));
        return bg;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // KEY PROCESSING
    // ═══════════════════════════════════════════════════════════════════
    private void processKey(String key) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        
        switch (key) {
            case "⇧":
                handleShift();
                break;
            case "⌫":
                handleBackspace(ic);
                break;
            case "↵":
                handleEnter(ic);
                break;
            case "SPACE":
                ic.commitText(" ", 1);
                lastWasConsonantWithHal = false;
                autoResetShift();
                break;
            case "123":
                isNumbers = true;
                isSymbols = false;
                rebuildKeyboard();
                break;
            case "ABC":
                isNumbers = false;
                isSymbols = false;
                rebuildKeyboard();
                break;
            case "#+=":
                isSymbols = true;
                isNumbers = false;
                rebuildKeyboard();
                break;
            case "🌐":
                isSinhalaMode = !isSinhalaMode;
                lastWasConsonantWithHal = false;
                rebuildKeyboard();
                break;
            case "✨":
                openPopupWindow();
                break;
            default:
                handleCharacter(ic, key);
        }
    }
    
    private void handleShift() {
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
    
    private void handleBackspace(InputConnection ic) {
        ic.deleteSurroundingText(1, 0);
        lastWasConsonantWithHal = false;
    }
    
    private void handleEnter(InputConnection ic) {
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
        lastWasConsonantWithHal = false;
    }
    
    private void handleCharacter(InputConnection ic, String key) {
        String ch = key;
        
        // Apply shift for letters
        if ((isShift || isCaps) && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            ch = key.toUpperCase();
        }
        
        if (isSinhalaMode && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            processSinglish(ic, ch);
        } else {
            ic.commitText(ch, 1);
            lastWasConsonantWithHal = false;
            autoResetShift();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SINGLISH ENGINE - REAL-TIME, FIXED
    // ═══════════════════════════════════════════════════════════════════
    private void processSinglish(InputConnection ic, String input) {
        char c = input.charAt(0);
        boolean isVowel = VOWEL_CHARS.indexOf(c) >= 0;
        
        if (isVowel) {
            processVowel(ic, input);
        } else {
            processConsonant(ic, input);
        }
        
        autoResetShift();
    }
    
    private void processVowel(InputConnection ic, String vowel) {
        if (lastWasConsonantWithHal) {
            // After consonant - need to get previous char and check for 2-char combo
            CharSequence before = ic.getTextBeforeCursor(3, 0);
            
            if (before != null && before.length() >= 1) {
                // Check if previous vowel + current forms a 2-char combo
                char prevChar = before.charAt(before.length() - 1);
                String combo = String.valueOf(prevChar) + vowel;
                
                // Check if the character before hal is a vowel modifier being built
                if (before.length() >= 2) {
                    char beforeHal = before.charAt(before.length() - 2);
                    
                    // Building aa, ee, oo, etc.
                    if (VOWEL_CHARS.indexOf(beforeHal) < 0) {
                        // Last was hal (්), check for double vowel
                        String doubleVowel = vowel.toLowerCase() + vowel.toLowerCase();
                        if (vowel.equalsIgnoreCase(String.valueOf(prevChar)) && VOWEL_MODIFIERS.containsKey(doubleVowel)) {
                            // Skip - this is handled by repeated vowel
                        }
                    }
                }
            }
            
            // Get what's before cursor (should be consonant + hal)
            CharSequence textBefore = ic.getTextBeforeCursor(10, 0);
            if (textBefore != null && textBefore.length() >= 1) {
                String text = textBefore.toString();
                
                // Check for double vowel (previous char was same vowel)
                if (text.length() >= 2) {
                    // Look for pattern: consonant + hal + we need to handle modifier
                    String lastTwo = text.substring(Math.max(0, text.length() - 2));
                    
                    // Check if last char is hal
                    if (lastTwo.endsWith("්")) {
                        // Remove hal and add modifier
                        ic.deleteSurroundingText(1, 0); // Remove hal
                        
                        String modifier = VOWEL_MODIFIERS.get(vowel);
                        if (modifier != null && !modifier.isEmpty()) {
                            ic.commitText(modifier, 1);
                        }
                        // If modifier is empty (for 'a'), just removing hal is enough
                        
                        lastWasConsonantWithHal = false;
                        return;
                    }
                }
            }
            
            // Default: remove hal and add modifier
            ic.deleteSurroundingText(1, 0);
            String modifier = VOWEL_MODIFIERS.get(vowel);
            if (modifier != null && !modifier.isEmpty()) {
                ic.commitText(modifier, 1);
            }
            lastWasConsonantWithHal = false;
            
        } else {
            // Standalone vowel or after another vowel
            CharSequence before = ic.getTextBeforeCursor(2, 0);
            
            if (before != null && before.length() >= 1) {
                char prev = before.charAt(before.length() - 1);
                String combo = String.valueOf(prev) + vowel;
                
                // Check for double vowel combinations (aa, ee, ii, oo, uu)
                // These should modify the previous standalone vowel
                if (VOWELS_STANDALONE.containsKey(combo.toLowerCase())) {
                    // Check if previous was a standalone vowel we can extend
                    String prevLower = String.valueOf(prev).toLowerCase();
                    if (VOWEL_CHARS.indexOf(Character.toLowerCase(prev)) >= 0) {
                        // Previous was a vowel char in English - check for combo
                        String lowerCombo = combo.toLowerCase();
                        
                        // aa -> ආ, ee -> ඒ, ii -> ඊ, oo -> ඕ, uu -> ඌ
                        if (lowerCombo.equals("aa") || lowerCombo.equals("ee") || 
                            lowerCombo.equals("ii") || lowerCombo.equals("oo") || 
                            lowerCombo.equals("uu") || lowerCombo.equals("ae") ||
                            lowerCombo.equals("ai") || lowerCombo.equals("au")) {
                            
                            // Delete previous Sinhala vowel and output combined
                            ic.deleteSurroundingText(1, 0);
                            String combined = VOWELS_STANDALONE.get(combo);
                            if (combined == null) combined = VOWELS_STANDALONE.get(combo.toLowerCase());
                            if (combined != null) {
                                ic.commitText(combined, 1);
                                return;
                            }
                        }
                    }
                }
            }
            
            // Output standalone vowel
            String standalone = VOWELS_STANDALONE.get(vowel);
            if (standalone == null) standalone = VOWELS_STANDALONE.get(vowel.toLowerCase());
            if (standalone != null) {
                ic.commitText(standalone, 1);
            } else {
                ic.commitText(vowel, 1);
            }
        }
    }
    
    private void processConsonant(InputConnection ic, String consonant) {
        // Check for 2-char consonant combinations
        CharSequence before = ic.getTextBeforeCursor(2, 0);
        
        if (before != null && before.length() >= 1) {
            char prev = before.charAt(before.length() - 1);
            String combo = String.valueOf(prev) + consonant;
            
            // Check if previous + current forms a 2-char consonant
            if (CONSONANTS.containsKey(combo)) {
                // If previous was a consonant with hal, remove it and output combined
                if (lastWasConsonantWithHal) {
                    // Remove previous consonant + hal
                    CharSequence textBefore = ic.getTextBeforeCursor(5, 0);
                    if (textBefore != null) {
                        String text = textBefore.toString();
                        // Find and remove previous consonant output
                        int halPos = text.lastIndexOf("්");
                        if (halPos >= 0) {
                            // Delete from consonant start to end
                            int toDelete = text.length() - halPos + 1;
                            if (halPos > 0) toDelete++; // Include the consonant before hal
                            ic.deleteSurroundingText(Math.min(toDelete, 3), 0);
                        }
                    }
                }
                
                // Output combined consonant with hal
                String combined = CONSONANTS.get(combo);
                ic.commitText(combined + "්", 1);
                lastWasConsonantWithHal = true;
                return;
            }
        }
        
        // Single consonant
        String sinhala = CONSONANTS.get(consonant);
        if (sinhala == null) sinhala = CONSONANTS.get(consonant.toLowerCase());
        
        if (sinhala != null) {
            ic.commitText(sinhala + "්", 1);
            lastWasConsonantWithHal = true;
        } else {
            ic.commitText(consonant, 1);
            lastWasConsonantWithHal = false;
        }
    }
    
    private void commitTextDirect(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(text, 1);
            lastWasConsonantWithHal = false;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════
    private void autoResetShift() {
        if (isShift && !isCaps) {
            isShift = false;
            rebuildKeyboard();
        }
    }
    
    private void startRepeat(String key) {
        isRepeating = true;
        repeatRunnable = () -> {
            if (isRepeating) {
                processKey(key);
                vibrate();
                handler.postDelayed(repeatRunnable, repeatInterval);
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
                vibrator.vibrate(VibrationEffect.createOneShot(vibrateDuration, VibrationEffect.DEFAULT_AMPLITUDE));
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
        } catch (Exception e) {}
    }
    
    private void rebuildKeyboard() {
        if (rootContainer == null) return;
        
        hideKeyPreview();
        keyInfoList.clear();
        
        // Remove old keyboard container
        rootContainer.removeView(keyboardContainer);
        
        // Create new keyboard container
        keyboardContainer = new LinearLayout(this);
        keyboardContainer.setOrientation(LinearLayout.VERTICAL);
        keyboardContainer.setBackgroundColor(parseColor(colorBackground));
        
        if (showEmojiRow) {
            emojiRowView = createEmojiRow();
            keyboardContainer.addView(emojiRowView);
        }
        
        keyboardView = createKeyboard();
        keyboardContainer.addView(keyboardView);
        
        // Add at index 0 (behind preview and touch layer)
        FrameLayout.LayoutParams kbParams = new FrameLayout.LayoutParams(-1, -2);
        kbParams.gravity = Gravity.BOTTOM;
        rootContainer.addView(keyboardContainer, 0, kbParams);
        
        // Update height
        int emojiH = showEmojiRow ? dp(44) : 0;
        int totalH = emojiH + dp(keyboardHeight) + navigationBarHeight;
        ViewGroup.LayoutParams rp = rootContainer.getLayoutParams();
        if (rp != null) {
            rp.height = totalH;
            rootContainer.setLayoutParams(rp);
        }
        rootContainer.setPadding(0, 0, 0, navigationBarHeight);
        rootContainer.setBackgroundColor(parseColor(colorBackground));
        
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