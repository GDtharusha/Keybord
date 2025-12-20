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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FastKeyboardService extends InputMethodService {
    
    private static final String TAG = "FastKeyboard";
    
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
    private int longPressDelay = 300;
    private int repeatInterval = 30;
    
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
    
    // Sinhala Labels on Keys
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
    // SINGLISH MAPPINGS - PRIORITY ORDER
    // ═══════════════════════════════════════════════════════════════════
    
    private static final String HAL = "්";
    
    // Priority 1: 4-Letter Consonants
    private static final Map<String, String> CONSONANTS_4 = new HashMap<>();
    static {
        CONSONANTS_4.put("thth", "ත්ථ");
    }
    
    // Priority 2: 3-Letter Consonants
    private static final Map<String, String> CONSONANTS_3 = new HashMap<>();
    static {
        CONSONANTS_3.put("ndh", "ඳ");
        CONSONANTS_3.put("nDh", "ඳ");
        CONSONANTS_3.put("ngh", "ඟ");
        CONSONANTS_3.put("zdh", "ඳ");
        CONSONANTS_3.put("zda", "ඬ");
        CONSONANTS_3.put("zja", "ඦ");
        CONSONANTS_3.put("zka", "ඤ");
        CONSONANTS_3.put("ksh", "ක්ෂ");
        CONSONANTS_3.put("Ksh", "ක්ෂ");
    }
    
    // Priority 3: 2-Letter Consonants
    private static final Map<String, String> CONSONANTS_2 = new HashMap<>();
    static {
        CONSONANTS_2.put("th", "ත"); CONSONANTS_2.put("Th", "ථ");
        CONSONANTS_2.put("dh", "ද"); CONSONANTS_2.put("Dh", "ධ");
        CONSONANTS_2.put("sh", "ශ"); CONSONANTS_2.put("Sh", "ෂ");
        CONSONANTS_2.put("ch", "ච"); CONSONANTS_2.put("Ch", "ඡ");
        CONSONANTS_2.put("kh", "ඛ"); CONSONANTS_2.put("Kh", "ඛ");
        CONSONANTS_2.put("gh", "ඝ"); CONSONANTS_2.put("Gh", "ඝ");
        CONSONANTS_2.put("ph", "ඵ"); CONSONANTS_2.put("Ph", "ඵ");
        CONSONANTS_2.put("bh", "භ"); CONSONANTS_2.put("Bh", "භ");
        CONSONANTS_2.put("jh", "ඣ"); CONSONANTS_2.put("Jh", "ඣ");
        CONSONANTS_2.put("mb", "ඹ"); CONSONANTS_2.put("Mb", "ඹ");
        CONSONANTS_2.put("ng", "ඟ"); CONSONANTS_2.put("Ng", "ඟ");
        CONSONANTS_2.put("nd", "ඳ"); CONSONANTS_2.put("Nd", "ඳ");
        CONSONANTS_2.put("ny", "ඤ"); CONSONANTS_2.put("Ny", "ඤ");
        CONSONANTS_2.put("kn", "ඤ"); CONSONANTS_2.put("Kn", "ඤ");
        CONSONANTS_2.put("gn", "ඥ"); CONSONANTS_2.put("Gn", "ඥ");
        CONSONANTS_2.put("zk", "ඤ");
        CONSONANTS_2.put("zh", "ඥ");
        CONSONANTS_2.put("zn", "ං");
        CONSONANTS_2.put("zb", "ඹ");
        CONSONANTS_2.put("Lu", "ළු");
    }
    
    // Priority 4: 1-Letter Consonants
    private static final Map<String, String> CONSONANTS_1 = new HashMap<>();
    static {
        CONSONANTS_1.put("k", "ක"); CONSONANTS_1.put("K", "ඛ");
        CONSONANTS_1.put("g", "ග"); CONSONANTS_1.put("G", "ඝ");
        CONSONANTS_1.put("c", "ච"); CONSONANTS_1.put("C", "ඡ");
        CONSONANTS_1.put("j", "ජ"); CONSONANTS_1.put("J", "ඣ");
        CONSONANTS_1.put("t", "ට"); CONSONANTS_1.put("T", "ඨ");
        CONSONANTS_1.put("d", "ඩ"); CONSONANTS_1.put("D", "ඪ");
        CONSONANTS_1.put("n", "න"); CONSONANTS_1.put("N", "ණ");
        CONSONANTS_1.put("p", "ප"); CONSONANTS_1.put("P", "ඵ");
        CONSONANTS_1.put("b", "බ"); CONSONANTS_1.put("B", "භ");
        CONSONANTS_1.put("m", "ම"); CONSONANTS_1.put("M", "ම");
        CONSONANTS_1.put("y", "ය"); CONSONANTS_1.put("Y", "ය");
        CONSONANTS_1.put("r", "ර"); CONSONANTS_1.put("R", "ර");
        CONSONANTS_1.put("l", "ල"); CONSONANTS_1.put("L", "ළ");
        CONSONANTS_1.put("w", "ව"); CONSONANTS_1.put("W", "ව");
        CONSONANTS_1.put("v", "ව"); CONSONANTS_1.put("V", "ව");
        CONSONANTS_1.put("s", "ස"); CONSONANTS_1.put("S", "ෂ");
        CONSONANTS_1.put("h", "හ"); CONSONANTS_1.put("H", "හ");
        CONSONANTS_1.put("f", "ෆ"); CONSONANTS_1.put("F", "ෆ");
        CONSONANTS_1.put("z", "ඤ"); CONSONANTS_1.put("Z", "ඥ");
        CONSONANTS_1.put("q", "ක"); CONSONANTS_1.put("Q", "ඛ");
    }
    
    // Special (no hal)
    private static final Map<String, String> SPECIAL_CONSONANTS = new HashMap<>();
    static {
        SPECIAL_CONSONANTS.put("x", "ං");
        SPECIAL_CONSONANTS.put("X", "ඞ");
    }
    
    // Standalone Vowels
    private static final Map<String, String> VOWELS_STANDALONE = new HashMap<>();
    static {
        // 3-letter
        VOWELS_STANDALONE.put("ruu", "ඎ");
        VOWELS_STANDALONE.put("Ruu", "ඎ");
        // 2-letter
        VOWELS_STANDALONE.put("aa", "ආ");
        VOWELS_STANDALONE.put("Aa", "ඈ");
        VOWELS_STANDALONE.put("AA", "ඈ");
        VOWELS_STANDALONE.put("ae", "ඇ");
        VOWELS_STANDALONE.put("Ae", "ඈ");
        VOWELS_STANDALONE.put("ii", "ඊ");
        VOWELS_STANDALONE.put("ee", "ඒ");
        VOWELS_STANDALONE.put("ei", "ඒ");
        VOWELS_STANDALONE.put("uu", "ඌ");
        VOWELS_STANDALONE.put("oo", "ඕ");
        VOWELS_STANDALONE.put("oe", "ඕ");
        VOWELS_STANDALONE.put("au", "ඖ");
        VOWELS_STANDALONE.put("Au", "ඖ");
        VOWELS_STANDALONE.put("ai", "ඓ");
        VOWELS_STANDALONE.put("Ai", "ඓ");
        VOWELS_STANDALONE.put("ru", "ඍ");
        VOWELS_STANDALONE.put("Ru", "ඍ");
        // 1-letter
        VOWELS_STANDALONE.put("a", "අ");
        VOWELS_STANDALONE.put("A", "ඇ");
        VOWELS_STANDALONE.put("i", "ඉ");
        VOWELS_STANDALONE.put("I", "ඊ");
        VOWELS_STANDALONE.put("u", "උ");
        VOWELS_STANDALONE.put("U", "ඌ");
        VOWELS_STANDALONE.put("e", "එ");
        VOWELS_STANDALONE.put("E", "ඓ");
        VOWELS_STANDALONE.put("o", "ඔ");
        VOWELS_STANDALONE.put("O", "ඕ");
    }
    
    // Vowel Modifiers (Pilla) - After consonant+hal
    private static final Map<String, String> VOWEL_MODIFIERS = new HashMap<>();
    static {
        // 3-letter
        VOWEL_MODIFIERS.put("ruu", "ෲ");
        VOWEL_MODIFIERS.put("Ruu", "ෲ");
        // 2-letter
        VOWEL_MODIFIERS.put("aa", "ා");
        VOWEL_MODIFIERS.put("Aa", "ෑ");
        VOWEL_MODIFIERS.put("AA", "ෑ");
        VOWEL_MODIFIERS.put("ae", "ැ");
        VOWEL_MODIFIERS.put("Ae", "ෑ");
        VOWEL_MODIFIERS.put("ii", "ී");
        VOWEL_MODIFIERS.put("ee", "ේ");
        VOWEL_MODIFIERS.put("ei", "ේ");
        VOWEL_MODIFIERS.put("uu", "ූ");
        VOWEL_MODIFIERS.put("oo", "ෝ");
        VOWEL_MODIFIERS.put("oe", "ෝ");
        VOWEL_MODIFIERS.put("au", "ෞ");
        VOWEL_MODIFIERS.put("Au", "ෞ");
        VOWEL_MODIFIERS.put("ai", "ෛ");
        VOWEL_MODIFIERS.put("Ai", "ෛ");
        VOWEL_MODIFIERS.put("ru", "ෘ");
        VOWEL_MODIFIERS.put("Ru", "ෘ");
        // 1-letter
        VOWEL_MODIFIERS.put("a", "");  // Just remove hal
        VOWEL_MODIFIERS.put("A", "ැ");
        VOWEL_MODIFIERS.put("i", "ි");
        VOWEL_MODIFIERS.put("I", "ී");
        VOWEL_MODIFIERS.put("u", "ු");
        VOWEL_MODIFIERS.put("U", "ූ");
        VOWEL_MODIFIERS.put("e", "ෙ");
        VOWEL_MODIFIERS.put("E", "ෛ");
        VOWEL_MODIFIERS.put("o", "ො");
        VOWEL_MODIFIERS.put("O", "ෝ");
    }
    
    // Reverse mapping: Sinhala consonant+hal -> English
    private static final Map<String, String> REVERSE_MAP = new HashMap<>();
    static {
        // Build reverse map from all consonant maps
        for (Map.Entry<String, String> e : CONSONANTS_1.entrySet()) {
            REVERSE_MAP.put(e.getValue() + HAL, e.getKey());
            REVERSE_MAP.put(e.getValue(), e.getKey()); // Without hal too
        }
        for (Map.Entry<String, String> e : CONSONANTS_2.entrySet()) {
            REVERSE_MAP.put(e.getValue() + HAL, e.getKey());
            REVERSE_MAP.put(e.getValue(), e.getKey());
        }
        for (Map.Entry<String, String> e : CONSONANTS_3.entrySet()) {
            REVERSE_MAP.put(e.getValue() + HAL, e.getKey());
            REVERSE_MAP.put(e.getValue(), e.getKey());
        }
        for (Map.Entry<String, String> e : CONSONANTS_4.entrySet()) {
            REVERSE_MAP.put(e.getValue() + HAL, e.getKey());
            REVERSE_MAP.put(e.getValue(), e.getKey());
        }
        // Standalone vowels
        for (Map.Entry<String, String> e : VOWELS_STANDALONE.entrySet()) {
            REVERSE_MAP.put(e.getValue(), e.getKey());
        }
        // Special
        REVERSE_MAP.put("ං", "x");
        REVERSE_MAP.put("ඞ", "X");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════
    private FrameLayout rootContainer;
    private LinearLayout keyboardContainer;
    private LinearLayout keyboardView;
    private ImageView backgroundImageView;
    private Handler handler;
    private Vibrator vibrator;
    private KeyboardSettings settings;
    
    // Key Preview Popup
    private PopupWindow previewPopup;
    private TextView previewText;
    
    private boolean isShift = false;
    private boolean isCaps = false;
    private boolean isNumbers = false;
    private boolean isSymbols = false;
    private boolean isSinhalaMode = false;
    
    private boolean isRepeating = false;
    private Runnable repeatRunnable;
    
    // Singlish Buffer - English characters typed
    private StringBuilder englishBuffer = new StringBuilder();
    private int currentSinhalaLength = 0; // Length of Sinhala output for current buffer
    
    private List<KeyInfo> keyInfoList = new ArrayList<>();
    private KeyInfo currentPressedKey = null;
    private long lastActionTime = 0;
    
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
    }
    
    private BroadcastReceiver settingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && KeyboardSettings.ACTION_SETTINGS_CHANGED.equals(intent.getAction())) {
                handler.post(() -> {
                    loadSettings();
                    rebuildKeyboard();
                });
            }
        }
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        settings = new KeyboardSettings(this);
        loadSettings();
        calculateNavBarHeight();
        initPreviewPopup();
        
        try { vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE); } catch (Exception e) {}
        
        IntentFilter filter = new IntentFilter(KeyboardSettings.ACTION_SETTINGS_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(settingsReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(settingsReceiver, filter);
        }
    }
    
    @Override
    public void onDestroy() {
        stopRepeat();
        hidePreview();
        try { unregisterReceiver(settingsReceiver); } catch (Exception e) {}
        super.onDestroy();
    }
    
    private void calculateNavBarHeight() {
        try {
            int id = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (id > 0) navigationBarHeight = getResources().getDimensionPixelSize(id);
        } catch (Exception e) {}
        if (navigationBarHeight == 0) navigationBarHeight = dp(48);
    }
    
    private void initPreviewPopup() {
        previewText = new TextView(this);
        previewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        previewText.setTextColor(Color.WHITE);
        previewText.setTypeface(Typeface.DEFAULT_BOLD);
        previewText.setGravity(Gravity.CENTER);
        previewText.setPadding(dp(8), dp(12), dp(8), dp(12));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#424242"));
        bg.setCornerRadius(dp(8));
        previewText.setBackground(bg);
        
        previewPopup = new PopupWindow(previewText,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        previewPopup.setClippingEnabled(false);
        previewPopup.setTouchable(false);
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
    
    @Override
    public View onCreateInputView() {
        loadSettings();
        
        rootContainer = new FrameLayout(this);
        rootContainer.setBackgroundColor(parseColor(colorBackground));
        
        // Background image
        backgroundImageView = new ImageView(this);
        backgroundImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        backgroundImageView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        rootContainer.addView(backgroundImageView);
        loadBackgroundImage();
        
        // Keyboard
        keyboardContainer = new LinearLayout(this);
        keyboardContainer.setOrientation(LinearLayout.VERTICAL);
        
        if (showEmojiRow) keyboardContainer.addView(createEmojiRow());
        keyboardView = createKeyboard();
        keyboardContainer.addView(keyboardView);
        
        FrameLayout.LayoutParams kbParams = new FrameLayout.LayoutParams(-1, -2);
        kbParams.gravity = Gravity.BOTTOM;
        rootContainer.addView(keyboardContainer, kbParams);
        
        // Touch layer
        View touchLayer = new View(this);
        touchLayer.setBackgroundColor(Color.TRANSPARENT);
        touchLayer.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        touchLayer.setOnTouchListener(this::handleTouch);
        rootContainer.addView(touchLayer);
        
        int emojiH = showEmojiRow ? dp(44) : 0;
        int totalH = emojiH + dp(keyboardHeight) + navigationBarHeight;
        rootContainer.setLayoutParams(new ViewGroup.LayoutParams(-1, totalH));
        rootContainer.setPadding(0, 0, 0, navigationBarHeight);
        
        rootContainer.post(this::updateKeyBounds);
        
        return rootContainer;
    }
    
    private void loadBackgroundImage() {
        try {
            String path = settings.getBackgroundImage();
            if (path != null && !path.isEmpty()) {
                File f = new File(path);
                if (f.exists()) {
                    Bitmap bmp = BitmapFactory.decodeFile(path);
                    if (bmp != null) {
                        backgroundImageView.setImageBitmap(bmp);
                        backgroundImageView.setAlpha(0.3f);
                        return;
                    }
                }
            }
            backgroundImageView.setImageBitmap(null);
        } catch (Exception e) {
            backgroundImageView.setImageBitmap(null);
        }
    }
    
    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        isShift = false;
        isCaps = false;
        isSymbols = false;
        clearSinglishBuffer();
        
        if (info != null) {
            int cls = info.inputType & EditorInfo.TYPE_MASK_CLASS;
            isNumbers = (cls == EditorInfo.TYPE_CLASS_NUMBER || cls == EditorInfo.TYPE_CLASS_PHONE);
        }
        rebuildKeyboard();
    }
    
    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        hidePreview();
        clearSinglishBuffer();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // TOUCH HANDLING
    // ═══════════════════════════════════════════════════════════════════
    private void updateKeyBounds() {
        for (KeyInfo ki : keyInfoList) ki.updateBounds();
    }
    
    private KeyInfo findKey(float x, float y) {
        for (KeyInfo ki : keyInfoList) {
            if (ki.bounds.contains((int)x, (int)y)) return ki;
        }
        float minD = Float.MAX_VALUE;
        KeyInfo nearest = null;
        for (KeyInfo ki : keyInfoList) {
            float cx = ki.bounds.centerX(), cy = ki.bounds.centerY();
            float d = (float)Math.sqrt((x-cx)*(x-cx)+(y-cy)*(y-cy));
            if (d < minD && d < dp(40)) { minD = d; nearest = ki; }
        }
        return nearest;
    }
    
    private boolean handleTouch(View v, MotionEvent ev) {
        float x = ev.getRawX(), y = ev.getRawY();
        
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                KeyInfo key = findKey(x, y);
                if (key != null) {
                    currentPressedKey = key;
                    onKeyPress(key);
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                KeyInfo moveKey = findKey(x, y);
                if (moveKey != null && moveKey != currentPressedKey) {
                    resetKeyVisual(currentPressedKey);
                    currentPressedKey = moveKey;
                    onKeyPress(moveKey);
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                hidePreview();
                stopRepeat();
                resetAllVisuals();
                currentPressedKey = null;
                break;
        }
        return true;
    }
    
    private void onKeyPress(KeyInfo ki) {
        long now = System.currentTimeMillis();
        if (isActionKey(ki.key) && now - lastActionTime < 200) return;
        if (isActionKey(ki.key)) lastActionTime = now;
        
        applyPressVisual(ki);
        vibrate();
        showPreview(ki);
        processKey(ki.key);
        
        if (ki.key.equals("⌫")) startRepeat(ki.key);
    }
    
    private boolean isActionKey(String k) {
        return k.equals("⇧") || k.equals("123") || k.equals("ABC") || k.equals("#+=") || k.equals("🌐");
    }
    
    private void applyPressVisual(KeyInfo ki) {
        if (ki != null && ki.view != null) {
            ki.view.setAlpha(0.6f);
            ki.view.setScaleX(0.94f);
            ki.view.setScaleY(0.94f);
        }
    }
    
    private void resetKeyVisual(KeyInfo ki) {
        if (ki != null && ki.view != null) {
            ki.view.setAlpha(1f);
            ki.view.setScaleX(1f);
            ki.view.setScaleY(1f);
        }
    }
    
    private void resetAllVisuals() {
        for (KeyInfo ki : keyInfoList) resetKeyVisual(ki);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // KEY PREVIEW - POPUP WINDOW (Shows above keyboard area)
    // ═══════════════════════════════════════════════════════════════════
    private void showPreview(KeyInfo ki) {
        if (isSpecialKey(ki.key)) {
            hidePreview();
            return;
        }
        
        String text = getPreviewText(ki.key);
        if (text.isEmpty()) {
            hidePreview();
            return;
        }
        
        previewText.setText(text);
        previewText.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        
        int keyW = ki.view.getWidth();
        int keyH = ki.view.getHeight();
        int previewW = Math.max(keyW, previewText.getMeasuredWidth() + dp(16));
        int previewH = (int)(keyH * 1.6f);
        
        int[] loc = new int[2];
        ki.view.getLocationOnScreen(loc);
        
        int px = loc[0] + (keyW - previewW) / 2;
        int py = loc[1] - previewH - dp(8);
        
        // Keep on screen
        int screenW = getResources().getDisplayMetrics().widthPixels;
        if (px < dp(4)) px = dp(4);
        if (px + previewW > screenW - dp(4)) px = screenW - previewW - dp(4);
        if (py < dp(10)) py = dp(10);
        
        try {
            if (previewPopup.isShowing()) {
                previewPopup.update(px, py, previewW, previewH);
            } else {
                previewPopup.setWidth(previewW);
                previewPopup.setHeight(previewH);
                previewPopup.showAtLocation(rootContainer, Gravity.NO_GRAVITY, px, py);
            }
        } catch (Exception e) {}
    }
    
    private void hidePreview() {
        try { if (previewPopup != null && previewPopup.isShowing()) previewPopup.dismiss(); } catch (Exception e) {}
    }
    
    private String getPreviewText(String key) {
        if (key.equals("SPACE")) return "";
        if (isSinhalaMode && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            Map<String, String> labels = (isShift || isCaps) ? SINHALA_LABELS_SHIFT : SINHALA_LABELS;
            String s = labels.get(key.toLowerCase());
            if (s != null) return s;
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
        
        String emojis = settings.getQuickEmojis();
        for (String emoji : emojis.split(",")) {
            final String e = emoji.trim();
            if (e.isEmpty()) continue;
            
            TextView tv = new TextView(this);
            tv.setText(e);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1f);
            p.setMargins(dp(2), 0, dp(2), 0);
            tv.setLayoutParams(p);
            
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(parseColor(colorKeyNormal));
            bg.setCornerRadius(dp(8));
            tv.setBackground(bg);
            
            tv.setOnClickListener(view -> {
                vibrate();
                flushSinglishBuffer();
                commitDirect(e);
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
        
        String[][] layout = isSymbols ? LAYOUT_SYMBOLS : (isNumbers ? LAYOUT_NUMBERS : LAYOUT_LETTERS);
        for (int i = 0; i < layout.length; i++) {
            kb.addView(createRow(layout[i], i));
        }
        return kb;
    }
    
    private LinearLayout createRow(String[] keys, int rowIdx) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        row.setPadding(rowIdx == 1 ? dp(14) : 0, dp(2), rowIdx == 1 ? dp(14) : 0, dp(2));
        
        for (String key : keys) row.addView(createKey(key));
        return row;
    }
    
    private View createKey(String key) {
        FrameLayout container = new FrameLayout(this);
        TextView tv = new TextView(this);
        
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        
        String display;
        int textColor = parseColor(colorText);
        float textSize = keyTextSize;
        
        switch (key) {
            case "↵": display = "↵"; textColor = Color.WHITE; textSize = 22; break;
            case "⇧":
                textSize = 24;
                if (isCaps) { display = "⇪"; textColor = Color.parseColor("#10b981"); }
                else if (isShift) { display = "⬆"; textColor = Color.parseColor("#3b82f6"); }
                else { display = "⇧"; }
                break;
            case "⌫": display = "⌫"; textSize = 22; break;
            case "SPACE": display = isSinhalaMode ? "සිංහල" : "English"; textSize = 11; textColor = Color.parseColor("#666666"); break;
            case "🌐": display = isSinhalaMode ? "සිං" : "EN"; textSize = 12; textColor = isSinhalaMode ? Color.parseColor("#10b981") : Color.parseColor("#3b82f6"); break;
            case "✨": display = "✨"; textSize = 18; break;
            case "123": case "ABC": case "#+=": display = key; textSize = 13; break;
            default:
                display = (key.length() == 1 && Character.isLetter(key.charAt(0))) ?
                    ((isShift || isCaps) ? key.toUpperCase() : key.toLowerCase()) : key;
        }
        
        tv.setText(display);
        tv.setTextColor(textColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        
        // Sinhala label
        if (isSinhalaMode && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            Map<String, String> labels = (isShift || isCaps) ? SINHALA_LABELS_SHIFT : SINHALA_LABELS;
            String lbl = labels.get(key.toLowerCase());
            if (lbl != null) {
                TextView lblView = new TextView(this);
                lblView.setText(lbl);
                lblView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
                lblView.setTextColor(Color.parseColor("#888888"));
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2);
                lp.gravity = Gravity.TOP | Gravity.END;
                lp.setMargins(0, dp(2), dp(3), 0);
                container.addView(lblView, lp);
            }
        }
        
        float weight = getWeight(key);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -1, weight);
        cp.setMargins(dp(keyGap), dp(keyGap), dp(keyGap), dp(keyGap));
        container.setLayoutParams(cp);
        
        container.addView(tv, new FrameLayout.LayoutParams(-1, -1));
        container.setBackground(createKeyBg(key));
        
        keyInfoList.add(new KeyInfo(key, container));
        return container;
    }
    
    private boolean isSpecialKey(String k) {
        return "⇧⌫↵SPACE123ABC#+=🌐✨".contains(k);
    }
    
    private float getWeight(String k) {
        switch (k) {
            case "SPACE": return 3.5f;
            case "⇧": case "⌫": return 1.5f;
            case "↵": case "123": case "ABC": case "#+=": return 1.3f;
            default: return 1f;
        }
    }
    
    private GradientDrawable createKeyBg(String key) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(keyRadius));
        
        String color = colorKeyNormal;
        if (key.equals("↵")) color = colorKeyEnter;
        else if (key.equals("⇧")) {
            if (isCaps) color = "#10b981";
            else if (isShift) color = "#3b82f6";
            else color = colorKeySpecial;
        }
        else if ("⌫123ABC#+=🌐✨".contains(key)) color = colorKeySpecial;
        else if (key.equals("SPACE")) color = colorKeySpace;
        
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
            case "⇧": handleShift(); break;
            case "⌫": handleBackspace(ic); break;
            case "↵": flushSinglishBuffer(); handleEnter(ic); break;
            case "SPACE": flushSinglishBuffer(); ic.commitText(" ", 1); break;
            case ",": flushSinglishBuffer(); ic.commitText(",", 1); break;
            case ".": flushSinglishBuffer(); ic.commitText(".", 1); break;
            case "123": flushSinglishBuffer(); isNumbers = true; isSymbols = false; rebuildKeyboard(); break;
            case "ABC": flushSinglishBuffer(); isNumbers = false; isSymbols = false; rebuildKeyboard(); break;
            case "#+=": flushSinglishBuffer(); isSymbols = true; rebuildKeyboard(); break;
            case "🌐": flushSinglishBuffer(); isSinhalaMode = !isSinhalaMode; rebuildKeyboard(); break;
            case "✨": flushSinglishBuffer(); openPopup(); break;
            default: handleChar(ic, key);
        }
    }
    
    private void handleShift() {
        if (isCaps) { isCaps = false; isShift = false; }
        else if (isShift) { isCaps = true; }
        else { isShift = true; }
        rebuildKeyboard();
    }
    
    private void handleBackspace(InputConnection ic) {
        if (englishBuffer.length() > 0) {
            // Delete from our buffer and recompute
            if (currentSinhalaLength > 0) {
                ic.deleteSurroundingText(currentSinhalaLength, 0);
            }
            englishBuffer.deleteCharAt(englishBuffer.length() - 1);
            
            if (englishBuffer.length() > 0) {
                String newSinhala = convertToSinhala(englishBuffer.toString());
                ic.commitText(newSinhala, 1);
                currentSinhalaLength = newSinhala.length();
            } else {
                currentSinhalaLength = 0;
            }
        } else {
            ic.deleteSurroundingText(1, 0);
        }
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
    }
    
    private void handleChar(InputConnection ic, String key) {
        char c = key.charAt(0);
        if ((isShift || isCaps) && Character.isLetter(c)) {
            c = Character.toUpperCase(c);
        }
        
        if (isSinhalaMode && Character.isLetter(c)) {
            processSinglish(ic, c);
        } else {
            flushSinglishBuffer();
            ic.commitText(String.valueOf(c), 1);
        }
        
        autoResetShift();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SINGLISH ENGINE - GREEDY BACK-MATCHING (NO TIMER)
    // ═══════════════════════════════════════════════════════════════════
    
    private void processSinglish(InputConnection ic, char c) {
        // Delete current Sinhala output for buffer
        if (currentSinhalaLength > 0) {
            ic.deleteSurroundingText(currentSinhalaLength, 0);
        }
        
        // Add new character to buffer
        englishBuffer.append(c);
        
        // Convert entire buffer to Sinhala
        String sinhala = convertToSinhala(englishBuffer.toString());
        
        // Commit new Sinhala
        ic.commitText(sinhala, 1);
        currentSinhalaLength = sinhala.length();
    }
    
    private String convertToSinhala(String english) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        boolean lastWasConsonant = false;
        
        while (i < english.length()) {
            String matched = null;
            int matchLen = 0;
            boolean isConsonant = false;
            boolean needsHal = false;
            boolean isSpecial = false;
            
            // Try 4-letter consonant
            if (i + 4 <= english.length()) {
                String sub = english.substring(i, i + 4);
                if (CONSONANTS_4.containsKey(sub)) {
                    matched = CONSONANTS_4.get(sub);
                    matchLen = 4;
                    isConsonant = true;
                    needsHal = true;
                }
            }
            
            // Try 3-letter consonant
            if (matched == null && i + 3 <= english.length()) {
                String sub = english.substring(i, i + 3);
                if (CONSONANTS_3.containsKey(sub)) {
                    matched = CONSONANTS_3.get(sub);
                    matchLen = 3;
                    isConsonant = true;
                    needsHal = true;
                }
                // 3-letter vowel
                else if (lastWasConsonant && VOWEL_MODIFIERS.containsKey(sub)) {
                    matched = VOWEL_MODIFIERS.get(sub);
                    matchLen = 3;
                    isConsonant = false;
                } else if (!lastWasConsonant && VOWELS_STANDALONE.containsKey(sub)) {
                    matched = VOWELS_STANDALONE.get(sub);
                    matchLen = 3;
                    isConsonant = false;
                }
            }
            
            // Try 2-letter consonant
            if (matched == null && i + 2 <= english.length()) {
                String sub = english.substring(i, i + 2);
                if (CONSONANTS_2.containsKey(sub)) {
                    matched = CONSONANTS_2.get(sub);
                    matchLen = 2;
                    isConsonant = true;
                    needsHal = !sub.equals("Lu") && !sub.equals("zn"); // Lu and zn don't need hal
                    if (sub.equals("Lu") || sub.equals("zn")) isSpecial = true;
                }
                // 2-letter vowel modifier
                else if (lastWasConsonant && VOWEL_MODIFIERS.containsKey(sub)) {
                    // Remove hal from result and add modifier
                    if (result.length() > 0 && result.toString().endsWith(HAL)) {
                        result.deleteCharAt(result.length() - 1);
                    }
                    matched = VOWEL_MODIFIERS.get(sub);
                    matchLen = 2;
                    isConsonant = false;
                    lastWasConsonant = false;
                }
                // 2-letter standalone vowel
                else if (!lastWasConsonant && VOWELS_STANDALONE.containsKey(sub)) {
                    matched = VOWELS_STANDALONE.get(sub);
                    matchLen = 2;
                    isConsonant = false;
                }
            }
            
            // Try 1-letter
            if (matched == null && i + 1 <= english.length()) {
                String sub = english.substring(i, i + 1);
                char ch = sub.charAt(0);
                
                // Special consonants (no hal)
                if (SPECIAL_CONSONANTS.containsKey(sub)) {
                    matched = SPECIAL_CONSONANTS.get(sub);
                    matchLen = 1;
                    isConsonant = false;
                    isSpecial = true;
                }
                // Regular consonant
                else if (CONSONANTS_1.containsKey(sub)) {
                    matched = CONSONANTS_1.get(sub);
                    matchLen = 1;
                    isConsonant = true;
                    needsHal = true;
                }
                // Vowel modifier (after consonant)
                else if (lastWasConsonant && VOWEL_MODIFIERS.containsKey(sub)) {
                    // Remove hal and add modifier
                    if (result.length() > 0 && result.toString().endsWith(HAL)) {
                        result.deleteCharAt(result.length() - 1);
                    }
                    matched = VOWEL_MODIFIERS.get(sub);
                    matchLen = 1;
                    isConsonant = false;
                    lastWasConsonant = false;
                }
                // Standalone vowel
                else if (!lastWasConsonant && VOWELS_STANDALONE.containsKey(sub)) {
                    matched = VOWELS_STANDALONE.get(sub);
                    matchLen = 1;
                    isConsonant = false;
                }
                // Yansaya: 'y' after consonant with hal
                else if ((ch == 'y' || ch == 'Y') && lastWasConsonant) {
                    if (result.length() > 0 && result.toString().endsWith(HAL)) {
                        result.deleteCharAt(result.length() - 1);
                        result.append("්‍ය");
                    }
                    matchLen = 1;
                    isConsonant = false;
                    lastWasConsonant = false;
                    i += matchLen;
                    continue;
                }
                // Rakaransaya: 'r' after consonant with hal followed by vowel
                else if ((ch == 'r' || ch == 'R') && lastWasConsonant && i + 2 <= english.length()) {
                    char next = english.charAt(i + 1);
                    if ("aeiouAEIOU".indexOf(next) >= 0) {
                        if (result.length() > 0 && result.toString().endsWith(HAL)) {
                            result.deleteCharAt(result.length() - 1);
                            result.append("්‍ර");
                        }
                        matchLen = 1;
                        isConsonant = true;
                        needsHal = false;
                        lastWasConsonant = true;
                        i += matchLen;
                        continue;
                    }
                }
            }
            
            // Apply match
            if (matched != null) {
                result.append(matched);
                if (isConsonant && needsHal) {
                    result.append(HAL);
                    lastWasConsonant = true;
                } else if (isConsonant && !needsHal) {
                    lastWasConsonant = false;
                } else {
                    lastWasConsonant = false;
                }
                i += matchLen;
            } else {
                // No match - output as-is
                result.append(english.charAt(i));
                lastWasConsonant = false;
                i++;
            }
        }
        
        return result.toString();
    }
    
    private void flushSinglishBuffer() {
        englishBuffer.setLength(0);
        currentSinhalaLength = 0;
    }
    
    private void clearSinglishBuffer() {
        englishBuffer.setLength(0);
        currentSinhalaLength = 0;
    }
    
    private void commitDirect(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(text, 1);
        }
    }
    
    private void autoResetShift() {
        if (isShift && !isCaps) {
            isShift = false;
            rebuildKeyboard();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════
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
        if (repeatRunnable != null) handler.removeCallbacks(repeatRunnable);
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
    
    private void openPopup() {
        try {
            Intent i = new Intent(this, PopupActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {}
    }
    
    private void rebuildKeyboard() {
        if (rootContainer == null) return;
        
        hidePreview();
        keyInfoList.clear();
        
        if (keyboardContainer != null) rootContainer.removeView(keyboardContainer);
        loadBackgroundImage();
        
        keyboardContainer = new LinearLayout(this);
        keyboardContainer.setOrientation(LinearLayout.VERTICAL);
        
        if (showEmojiRow) keyboardContainer.addView(createEmojiRow());
        keyboardView = createKeyboard();
        keyboardContainer.addView(keyboardView);
        
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(-1, -2);
        p.gravity = Gravity.BOTTOM;
        rootContainer.addView(keyboardContainer, 1, p);
        
        int emojiH = showEmojiRow ? dp(44) : 0;
        int totalH = emojiH + dp(keyboardHeight) + navigationBarHeight;
        ViewGroup.LayoutParams rp = rootContainer.getLayoutParams();
        if (rp != null) { rp.height = totalH; rootContainer.setLayoutParams(rp); }
        rootContainer.setPadding(0, 0, 0, navigationBarHeight);
        
        rootContainer.post(this::updateKeyBounds);
    }
    
    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
    
    private int parseColor(String c) {
        try { return Color.parseColor(c); } catch (Exception e) { return Color.BLACK; }
    }
}