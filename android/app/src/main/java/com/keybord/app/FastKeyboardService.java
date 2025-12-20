package com.keybord.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
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
        SINHALA_LABELS.put("a", "අ"); SINHALA_LABELS.put("b", "බ"); SINHALA_LABELS.put("c", "ච");
        SINHALA_LABELS.put("d", "ඩ"); SINHALA_LABELS.put("e", "එ"); SINHALA_LABELS.put("f", "ෆ");
        SINHALA_LABELS.put("g", "ග"); SINHALA_LABELS.put("h", "හ"); SINHALA_LABELS.put("i", "ඉ");
        SINHALA_LABELS.put("j", "ජ"); SINHALA_LABELS.put("k", "ක"); SINHALA_LABELS.put("l", "ල");
        SINHALA_LABELS.put("m", "ම"); SINHALA_LABELS.put("n", "න"); SINHALA_LABELS.put("o", "ඔ");
        SINHALA_LABELS.put("p", "ප"); SINHALA_LABELS.put("q", "ෘ"); SINHALA_LABELS.put("r", "ර");
        SINHALA_LABELS.put("s", "ස"); SINHALA_LABELS.put("t", "ට"); SINHALA_LABELS.put("u", "උ");
        SINHALA_LABELS.put("v", "ව"); SINHALA_LABELS.put("w", "ව"); SINHALA_LABELS.put("x", "ං");
        SINHALA_LABELS.put("y", "ය"); SINHALA_LABELS.put("z", "ඤ");
        
        SINHALA_LABELS_SHIFT.put("a", "ඇ"); SINHALA_LABELS_SHIFT.put("b", "භ"); SINHALA_LABELS_SHIFT.put("c", "ඡ");
        SINHALA_LABELS_SHIFT.put("d", "ඪ"); SINHALA_LABELS_SHIFT.put("e", "ඓ"); SINHALA_LABELS_SHIFT.put("f", "ෆ");
        SINHALA_LABELS_SHIFT.put("g", "ඝ"); SINHALA_LABELS_SHIFT.put("h", "ඃ"); SINHALA_LABELS_SHIFT.put("i", "ඊ");
        SINHALA_LABELS_SHIFT.put("j", "ඣ"); SINHALA_LABELS_SHIFT.put("k", "ඛ"); SINHALA_LABELS_SHIFT.put("l", "ළ");
        SINHALA_LABELS_SHIFT.put("m", "ඹ"); SINHALA_LABELS_SHIFT.put("n", "ණ"); SINHALA_LABELS_SHIFT.put("o", "ඕ");
        SINHALA_LABELS_SHIFT.put("p", "ඵ"); SINHALA_LABELS_SHIFT.put("q", "ඎ"); SINHALA_LABELS_SHIFT.put("r", "ර");
        SINHALA_LABELS_SHIFT.put("s", "ෂ"); SINHALA_LABELS_SHIFT.put("t", "ඨ"); SINHALA_LABELS_SHIFT.put("u", "ඌ");
        SINHALA_LABELS_SHIFT.put("v", "ව"); SINHALA_LABELS_SHIFT.put("w", "ව"); SINHALA_LABELS_SHIFT.put("x", "ඞ");
        SINHALA_LABELS_SHIFT.put("y", "ය"); SINHALA_LABELS_SHIFT.put("z", "ඥ");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SINGLISH ENGINE - EXACT MAPPING FROM IMAGE
    // ═══════════════════════════════════════════════════════════════════
    
    // CONSONANTS (Base Letters + Aspirated)
    private static final Map<String, String> CONSONANTS = new HashMap<>();
    static {
        // Special 3-letter combinations (check first)
        CONSONANTS.put("zdh", "ඳ");
        CONSONANTS.put("ruu", "ඎ"); // Special vowel but handled here
        CONSONANTS.put("Sha", "ෂ");
        
        // 2-letter consonants
        CONSONANTS.put("kh", "ඛ");
        CONSONANTS.put("gh", "ඝ");
        CONSONANTS.put("ch", "ච");
        CONSONANTS.put("Ch", "ඡ");
        CONSONANTS.put("jh", "ඣ");
        CONSONANTS.put("Jh", "ඣ");
        CONSONANTS.put("th", "ත");
        CONSONANTS.put("Th", "ථ");
        CONSONANTS.put("dh", "ද");
        CONSONANTS.put("Dh", "ධ");
        CONSONANTS.put("ph", "ඵ");
        CONSONANTS.put("bh", "භ");
        CONSONANTS.put("sh", "ශ");
        CONSONANTS.put("Sh", "ෂ");
        CONSONANTS.put("zj", "ඦ");
        CONSONANTS.put("zd", "ඬ");
        CONSONANTS.put("zq", "ඳ");
        CONSONANTS.put("zk", "ඤ");
        CONSONANTS.put("zh", "ඥ");
        CONSONANTS.put("zb", "ඹ");
        CONSONANTS.put("zn", "ං");
        CONSONANTS.put("Lu", "ළු");
        CONSONANTS.put("ru", "ඍ");
        
        // 1-letter consonants
        CONSONANTS.put("k", "ක");
        CONSONANTS.put("K", "ඛ");
        CONSONANTS.put("g", "ග");
        CONSONANTS.put("G", "ඝ");
        CONSONANTS.put("c", "ච");
        CONSONANTS.put("C", "ඡ");
        CONSONANTS.put("j", "ජ");
        CONSONANTS.put("J", "ඣ");
        CONSONANTS.put("t", "ට");
        CONSONANTS.put("T", "ඨ");
        CONSONANTS.put("d", "ඩ");
        CONSONANTS.put("D", "ඪ");
        CONSONANTS.put("n", "න");
        CONSONANTS.put("N", "ණ");
        CONSONANTS.put("p", "ප");
        CONSONANTS.put("P", "ඵ");
        CONSONANTS.put("b", "බ");
        CONSONANTS.put("B", "ඹ");
        CONSONANTS.put("m", "ම");
        CONSONANTS.put("M", "ම");
        CONSONANTS.put("y", "ය");
        CONSONANTS.put("Y", "ය");
        CONSONANTS.put("r", "ර");
        CONSONANTS.put("R", "ර");
        CONSONANTS.put("l", "ල");
        CONSONANTS.put("L", "ළ");
        CONSONANTS.put("w", "ව");
        CONSONANTS.put("W", "ව");
        CONSONANTS.put("v", "ව");
        CONSONANTS.put("V", "ව");
        CONSONANTS.put("s", "ස");
        CONSONANTS.put("S", "ෂ");
        CONSONANTS.put("h", "හ");
        CONSONANTS.put("H", "ඃ"); // Visargaya when standalone
        CONSONANTS.put("f", "ෆ");
        CONSONANTS.put("F", "ෆ");
        CONSONANTS.put("x", "ං"); // Binduwa
        CONSONANTS.put("X", "ඞ"); // Inga
        CONSONANTS.put("z", "ඤ");
        CONSONANTS.put("Z", "ඥ");
    }
    
    // STANDALONE VOWELS (Start of word)
    private static final Map<String, String> VOWELS_STANDALONE = new HashMap<>();
    static {
        // 3-letter combinations
        VOWELS_STANDALONE.put("ruu", "ඎ");
        
        // 2-letter combinations  
        VOWELS_STANDALONE.put("aa", "ආ");
        VOWELS_STANDALONE.put("Aa", "ඈ");
        VOWELS_STANDALONE.put("AA", "ඈ");
        VOWELS_STANDALONE.put("ii", "ඊ");
        VOWELS_STANDALONE.put("uu", "ඌ");
        VOWELS_STANDALONE.put("ee", "ඒ");
        VOWELS_STANDALONE.put("ei", "ඒ");
        VOWELS_STANDALONE.put("oo", "ඕ");
        VOWELS_STANDALONE.put("oe", "ඕ");
        VOWELS_STANDALONE.put("au", "ඖ");
        VOWELS_STANDALONE.put("ru", "ඍ");
        
        // 1-letter vowels
        VOWELS_STANDALONE.put("a", "අ");
        VOWELS_STANDALONE.put("A", "ඇ");
        VOWELS_STANDALONE.put("i", "ඉ");
        VOWELS_STANDALONE.put("I", "ඊ");
        VOWELS_STANDALONE.put("u", "උ");
        VOWELS_STANDALONE.put("U", "ඌ");
        VOWELS_STANDALONE.put("e", "එ");
        VOWELS_STANDALONE.put("E", "ඓ"); // Kombu Deka
        VOWELS_STANDALONE.put("o", "ඔ");
        VOWELS_STANDALONE.put("O", "ඕ");
    }
    
    // VOWEL MODIFIERS (After consonant - Pilla)
    private static final Map<String, String> VOWEL_MODIFIERS = new HashMap<>();
    static {
        // 3-letter combinations
        VOWEL_MODIFIERS.put("ruu", "ෲ");
        
        // 2-letter combinations
        VOWEL_MODIFIERS.put("aa", "ා");
        VOWEL_MODIFIERS.put("Aa", "ෑ");
        VOWEL_MODIFIERS.put("AA", "ෑ");
        VOWEL_MODIFIERS.put("ii", "ී");
        VOWEL_MODIFIERS.put("uu", "ූ");
        VOWEL_MODIFIERS.put("ee", "ේ");
        VOWEL_MODIFIERS.put("ei", "ේ");
        VOWEL_MODIFIERS.put("oo", "ෝ");
        VOWEL_MODIFIERS.put("oe", "ෝ");
        VOWEL_MODIFIERS.put("au", "ෞ");
        VOWEL_MODIFIERS.put("ru", "ෘ");
        
        // 1-letter modifiers
        VOWEL_MODIFIERS.put("a", ""); // Just removes hal
        VOWEL_MODIFIERS.put("A", "ැ");
        VOWEL_MODIFIERS.put("i", "ි");
        VOWEL_MODIFIERS.put("I", "ී");
        VOWEL_MODIFIERS.put("u", "ු");
        VOWEL_MODIFIERS.put("U", "ූ");
        VOWEL_MODIFIERS.put("e", "ෙ");
        VOWEL_MODIFIERS.put("E", "ෛ"); // Kombu Deka - CRUCIAL
        VOWEL_MODIFIERS.put("o", "ො");
        VOWEL_MODIFIERS.put("O", "ෝ");
    }
    
    // Characters that are vowels
    private static final String VOWEL_CHARS = "aeiouAEIOU";
    private static final String CONSONANT_CHARS = "bcdfghjklmnpqrstvwxyzBCDFGHJKLMNPQRSTVWXYZ";
    
    // ═══════════════════════════════════════════════════════════════════
    // STATE VARIABLES
    // ═══════════════════════════════════════════════════════════════════
    private FrameLayout rootContainer;
    private LinearLayout keyboardContainer;
    private LinearLayout keyboardView;
    private LinearLayout emojiRowView;
    private FrameLayout keyPreviewContainer;
    private TextView keyPreviewView;
    private ImageView backgroundImageView;
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
    
    // Singlish state
    private StringBuilder inputBuffer = new StringBuilder();
    private boolean lastWasConsonantWithHal = false;
    private long lastKeyTime = 0;
    private static final long BUFFER_TIMEOUT = 800; // ms
    
    // Touch tracking
    private List<KeyInfo> keyInfoList = new ArrayList<>();
    private KeyInfo currentPressedKey = null;
    private long lastSpecialKeyTime = 0;
    private static final long SPECIAL_KEY_DEBOUNCE = 250;
    
    private int navigationBarHeight = 0;
    private int currentKeyWidth = 0;
    private int currentKeyHeight = 0;
    
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
        
        // Background image (if set)
        backgroundImageView = new ImageView(this);
        backgroundImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        backgroundImageView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        rootContainer.addView(backgroundImageView);
        loadBackgroundImage();
        
        // Keyboard container
        keyboardContainer = new LinearLayout(this);
        keyboardContainer.setOrientation(LinearLayout.VERTICAL);
        
        if (showEmojiRow) {
            emojiRowView = createEmojiRow();
            keyboardContainer.addView(emojiRowView);
        }
        
        keyboardView = createKeyboard();
        keyboardContainer.addView(keyboardView);
        
        FrameLayout.LayoutParams kbParams = new FrameLayout.LayoutParams(-1, -2);
        kbParams.gravity = Gravity.BOTTOM;
        rootContainer.addView(keyboardContainer, kbParams);
        
        // Key Preview Container (higher z-index)
        keyPreviewContainer = new FrameLayout(this);
        keyPreviewContainer.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        keyPreviewContainer.setClickable(false);
        keyPreviewContainer.setFocusable(false);
        rootContainer.addView(keyPreviewContainer);
        
        // Key Preview View
        keyPreviewView = new TextView(this);
        keyPreviewView.setTextColor(Color.WHITE);
        keyPreviewView.setTypeface(Typeface.DEFAULT_BOLD);
        keyPreviewView.setGravity(Gravity.CENTER);
        keyPreviewView.setVisibility(View.GONE);
        
        GradientDrawable previewBg = new GradientDrawable();
        previewBg.setColor(Color.parseColor("#333333"));
        previewBg.setCornerRadius(dp(10));
        keyPreviewView.setBackground(previewBg);
        
        keyPreviewContainer.addView(keyPreviewView);
        
        // Touch layer (topmost)
        View touchLayer = new View(this);
        touchLayer.setBackgroundColor(Color.TRANSPARENT);
        touchLayer.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        touchLayer.setOnTouchListener(this::handleTouch);
        rootContainer.addView(touchLayer);
        
        // Set total height
        int emojiH = showEmojiRow ? dp(44) : 0;
        int totalHeight = emojiH + dp(keyboardHeight) + navigationBarHeight;
        rootContainer.setLayoutParams(new ViewGroup.LayoutParams(-1, totalHeight));
        rootContainer.setPadding(0, 0, 0, navigationBarHeight);
        
        // Update bounds after layout
        rootContainer.post(this::updateAllKeyBounds);
        
        return rootContainer;
    }
    
    private void loadBackgroundImage() {
        try {
            String imagePath = settings.getBackgroundImage();
            if (imagePath != null && !imagePath.isEmpty()) {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                    if (bitmap != null) {
                        backgroundImageView.setImageBitmap(bitmap);
                        backgroundImageView.setAlpha(0.3f); // Semi-transparent
                        return;
                    }
                }
                
                // Try as Base64
                if (imagePath.startsWith("data:image")) {
                    String base64 = imagePath.substring(imagePath.indexOf(",") + 1);
                    byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    if (bitmap != null) {
                        backgroundImageView.setImageBitmap(bitmap);
                        backgroundImageView.setAlpha(0.3f);
                        return;
                    }
                }
            }
            backgroundImageView.setImageBitmap(null);
        } catch (Exception e) {
            Log.e(TAG, "Error loading background image", e);
            backgroundImageView.setImageBitmap(null);
        }
    }
    
    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        
        isShift = false;
        isCaps = false;
        isSymbols = false;
        inputBuffer.setLength(0);
        lastWasConsonantWithHal = false;
        
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
        flushBuffer();
        hideKeyPreview();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // TOUCH HANDLING
    // ═══════════════════════════════════════════════════════════════════
    private void updateAllKeyBounds() {
        for (KeyInfo ki : keyInfoList) {
            ki.updateBounds();
            // Store key dimensions for preview
            if (currentKeyWidth == 0 && ki.view != null) {
                currentKeyWidth = ki.view.getWidth();
                currentKeyHeight = ki.view.getHeight();
            }
        }
    }
    
    private KeyInfo findKeyAt(float x, float y) {
        // Direct hit
        for (KeyInfo ki : keyInfoList) {
            if (ki.containsPoint(x, y)) {
                return ki;
            }
        }
        
        // Nearest key within threshold
        float minDist = Float.MAX_VALUE;
        KeyInfo nearest = null;
        float maxDist = dp(35);
        
        for (KeyInfo ki : keyInfoList) {
            float dist = ki.distanceTo(x, y);
            if (dist < minDist && dist < maxDist) {
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
        applyPressedVisual(ki);
        vibrate();
        showKeyPreview(ki);
        
        // Debounce special keys
        if (isSpecialActionKey(ki.key)) {
            long now = System.currentTimeMillis();
            if (now - lastSpecialKeyTime > SPECIAL_KEY_DEBOUNCE) {
                lastSpecialKeyTime = now;
                processKey(ki.key);
            }
        } else {
            processKey(ki.key);
        }
        
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
            ki.view.setAlpha(0.65f);
            ki.view.setScaleX(0.94f);
            ki.view.setScaleY(0.94f);
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
    // KEY PREVIEW - FIXED SIZE AND POSITION
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
        
        // Set text with smaller font
        keyPreviewView.setText(displayText);
        keyPreviewView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22); // Smaller font
        keyPreviewView.setVisibility(View.VISIBLE);
        
        // Get key position
        int[] keyLoc = new int[2];
        ki.view.getLocationOnScreen(keyLoc);
        
        int keyW = ki.view.getWidth();
        int keyH = ki.view.getHeight();
        
        // Preview size: width = key width, height = 2x key height
        int previewW = keyW;
        int previewH = (int)(keyH * 1.8f);
        
        // Position: above key, centered, with gap
        int previewX = keyLoc[0];
        int previewY = keyLoc[1] - previewH - dp(12); // 12dp gap above key
        
        // Adjust for screen bounds
        int screenW = getResources().getDisplayMetrics().widthPixels;
        if (previewX < dp(2)) previewX = dp(2);
        if (previewX + previewW > screenW - dp(2)) previewX = screenW - previewW - dp(2);
        if (previewY < dp(5)) previewY = dp(5);
        
        // Apply layout params
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(previewW, previewH);
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
        
        // For Sinhala mode
        if (isSinhalaMode && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            Map<String, String> labels = (isShift || isCaps) ? SINHALA_LABELS_SHIFT : SINHALA_LABELS;
            String sinhala = labels.get(key.toLowerCase());
            if (sinhala != null) return sinhala;
        }
        
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
        
        // Sinhala label
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
                flushBuffer();
                handleShift();
                break;
            case "⌫":
                handleBackspace(ic);
                break;
            case "↵":
                flushBuffer();
                handleEnter(ic);
                break;
            case "SPACE":
                flushBuffer();
                ic.commitText(" ", 1);
                lastWasConsonantWithHal = false;
                autoResetShift();
                break;
            case "123":
                flushBuffer();
                isNumbers = true;
                isSymbols = false;
                rebuildKeyboard();
                break;
            case "ABC":
                flushBuffer();
                isNumbers = false;
                isSymbols = false;
                rebuildKeyboard();
                break;
            case "#+=":
                flushBuffer();
                isSymbols = true;
                isNumbers = false;
                rebuildKeyboard();
                break;
            case "🌐":
                flushBuffer();
                isSinhalaMode = !isSinhalaMode;
                inputBuffer.setLength(0);
                lastWasConsonantWithHal = false;
                rebuildKeyboard();
                break;
            case "✨":
                flushBuffer();
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
        if (inputBuffer.length() > 0) {
            inputBuffer.deleteCharAt(inputBuffer.length() - 1);
        } else {
            ic.deleteSurroundingText(1, 0);
        }
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
        
        if ((isShift || isCaps) && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            ch = key.toUpperCase();
        }
        
        if (isSinhalaMode && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            processSinglishRealTime(ic, ch);
        } else {
            ic.commitText(ch, 1);
            lastWasConsonantWithHal = false;
            autoResetShift();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SINGLISH ENGINE - REAL-TIME WITH EXACT MAPPING
    // ═══════════════════════════════════════════════════════════════════
    private void processSinglishRealTime(InputConnection ic, String input) {
        char c = input.charAt(0);
        inputBuffer.append(c);
        lastKeyTime = System.currentTimeMillis();
        
        processBufferRealTime(ic);
        autoResetShift();
    }
    
    private void processBufferRealTime(InputConnection ic) {
        while (inputBuffer.length() > 0) {
            String buffer = inputBuffer.toString();
            boolean matched = false;
            
            // Check if we should wait for more input
            if (shouldWaitForMore(buffer)) {
                return;
            }
            
            // Try to match longest sequence first (3 -> 2 -> 1)
            for (int len = Math.min(3, buffer.length()); len >= 1 && !matched; len--) {
                String seq = buffer.substring(0, len);
                
                // If after consonant with hal, try vowel modifiers first
                if (lastWasConsonantWithHal) {
                    if (VOWEL_MODIFIERS.containsKey(seq)) {
                        // Apply modifier
                        String mod = VOWEL_MODIFIERS.get(seq);
                        ic.deleteSurroundingText(1, 0); // Remove hal
                        if (mod != null && !mod.isEmpty()) {
                            ic.commitText(mod, 1);
                        }
                        inputBuffer.delete(0, len);
                        lastWasConsonantWithHal = false;
                        matched = true;
                        continue;
                    }
                    
                    // Check for Yansaya (y after consonant)
                    if (seq.equals("y") || seq.equals("Y")) {
                        ic.commitText("්‍ය", 1); // Yansaya
                        inputBuffer.delete(0, 1);
                        lastWasConsonantWithHal = false;
                        matched = true;
                        continue;
                    }
                    
                    // Check for Rakaranshaya (r after consonant for conjunct)
                    if (seq.equals("r") && buffer.length() == 1) {
                        // Could be rakaranshaya or new 'r' consonant
                        // Wait for more input
                        return;
                    }
                }
                
                // Try consonants
                if (CONSONANTS.containsKey(seq)) {
                    String consonant = CONSONANTS.get(seq);
                    
                    // Special cases that don't take hal
                    if (seq.equals("x") || seq.equals("X") || seq.equals("H") || 
                        seq.equals("zn") || seq.equals("Lu")) {
                        ic.commitText(consonant, 1);
                        lastWasConsonantWithHal = false;
                    } else if (seq.equals("ru") || seq.equals("ruu")) {
                        // These are special vowel-like
                        if (lastWasConsonantWithHal) {
                            ic.deleteSurroundingText(1, 0);
                            String mod = VOWEL_MODIFIERS.get(seq);
                            if (mod != null) ic.commitText(mod, 1);
                            lastWasConsonantWithHal = false;
                        } else {
                            ic.commitText(consonant, 1);
                            lastWasConsonantWithHal = false;
                        }
                    } else {
                        ic.commitText(consonant + "්", 1);
                        lastWasConsonantWithHal = true;
                    }
                    inputBuffer.delete(0, len);
                    matched = true;
                    continue;
                }
                
                // Try standalone vowels
                if (!lastWasConsonantWithHal && VOWELS_STANDALONE.containsKey(seq)) {
                    ic.commitText(VOWELS_STANDALONE.get(seq), 1);
                    inputBuffer.delete(0, len);
                    lastWasConsonantWithHal = false;
                    matched = true;
                    continue;
                }
            }
            
            // No match - output first char as-is if buffer is long enough
            if (!matched) {
                if (buffer.length() >= 3) {
                    ic.commitText(String.valueOf(buffer.charAt(0)), 1);
                    inputBuffer.deleteCharAt(0);
                    lastWasConsonantWithHal = false;
                } else {
                    // Wait for more input
                    return;
                }
            }
        }
    }
    
    private boolean shouldWaitForMore(String buffer) {
        if (buffer.isEmpty()) return false;
        
        int len = buffer.length();
        
        // Single character - might extend
        if (len == 1) {
            char c = buffer.charAt(0);
            // Vowels might double (aa, ee, etc.)
            if (VOWEL_CHARS.indexOf(c) >= 0) return true;
            // Consonants might have h, H, etc.
            if ("kgcjtdpbszKGCJTDPBSZ".indexOf(c) >= 0) return true;
            // r might be ru, ruu
            if (c == 'r' || c == 'R') return true;
            // z combinations
            if (c == 'z' || c == 'Z') return true;
            // L might be Lu
            if (c == 'L') return true;
        }
        
        // Two characters - some might extend to 3
        if (len == 2) {
            // ru might become ruu
            if (buffer.equalsIgnoreCase("ru")) return true;
            // Check other 3-char possibilities
            if (buffer.equals("zd") || buffer.equals("Sh") || buffer.equals("sh")) return true;
            // aa, ee, etc. are complete - don't wait
            if (buffer.equals("aa") || buffer.equals("ee") || buffer.equals("ii") ||
                buffer.equals("oo") || buffer.equals("uu") || buffer.equals("Aa") ||
                buffer.equals("AA") || buffer.equals("ei") || buffer.equals("oe") ||
                buffer.equals("au")) {
                return false;
            }
        }
        
        return false;
    }
    
    private void flushBuffer() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || inputBuffer.length() == 0) return;
        
        // Force process remaining buffer
        while (inputBuffer.length() > 0) {
            String buffer = inputBuffer.toString();
            boolean matched = false;
            
            for (int len = Math.min(3, buffer.length()); len >= 1 && !matched; len--) {
                String seq = buffer.substring(0, len);
                
                if (lastWasConsonantWithHal && VOWEL_MODIFIERS.containsKey(seq)) {
                    String mod = VOWEL_MODIFIERS.get(seq);
                    ic.deleteSurroundingText(1, 0);
                    if (mod != null && !mod.isEmpty()) {
                        ic.commitText(mod, 1);
                    }
                    inputBuffer.delete(0, len);
                    lastWasConsonantWithHal = false;
                    matched = true;
                    continue;
                }
                
                if (CONSONANTS.containsKey(seq)) {
                    String consonant = CONSONANTS.get(seq);
                    if (seq.equals("x") || seq.equals("X") || seq.equals("H") || seq.equals("zn")) {
                        ic.commitText(consonant, 1);
                        lastWasConsonantWithHal = false;
                    } else {
                        ic.commitText(consonant + "්", 1);
                        lastWasConsonantWithHal = true;
                    }
                    inputBuffer.delete(0, len);
                    matched = true;
                    continue;
                }
                
                if (!lastWasConsonantWithHal && VOWELS_STANDALONE.containsKey(seq)) {
                    ic.commitText(VOWELS_STANDALONE.get(seq), 1);
                    inputBuffer.delete(0, len);
                    lastWasConsonantWithHal = false;
                    matched = true;
                    continue;
                }
            }
            
            if (!matched) {
                ic.commitText(String.valueOf(buffer.charAt(0)), 1);
                inputBuffer.deleteCharAt(0);
                lastWasConsonantWithHal = false;
            }
        }
    }
    
    private void commitTextDirect(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            flushBuffer();
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
        if (keyboardContainer != null) {
            rootContainer.removeView(keyboardContainer);
        }
        
        // Reload background image
        loadBackgroundImage();
        
        // Create new keyboard container
        keyboardContainer = new LinearLayout(this);
        keyboardContainer.setOrientation(LinearLayout.VERTICAL);
        
        if (showEmojiRow) {
            emojiRowView = createEmojiRow();
            keyboardContainer.addView(emojiRowView);
        }
        
        keyboardView = createKeyboard();
        keyboardContainer.addView(keyboardView);
        
        FrameLayout.LayoutParams kbParams = new FrameLayout.LayoutParams(-1, -2);
        kbParams.gravity = Gravity.BOTTOM;
        rootContainer.addView(keyboardContainer, 1, kbParams); // Index 1 (after bg image)
        
        // Update height
        int emojiH = showEmojiRow ? dp(44) : 0;
        int totalH = emojiH + dp(keyboardHeight) + navigationBarHeight;
        ViewGroup.LayoutParams rp = rootContainer.getLayoutParams();
        if (rp != null) {
            rp.height = totalH;
            rootContainer.setLayoutParams(rp);
        }
        rootContainer.setPadding(0, 0, 0, navigationBarHeight);
        
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