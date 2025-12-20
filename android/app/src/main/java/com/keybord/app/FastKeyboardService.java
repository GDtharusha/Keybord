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
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    // ═══════════════════════════════════════════════════════════════════
    // SINHALA LABELS (shown on keys in Sinhala mode)
    // ═══════════════════════════════════════════════════════════════════
    private static final Map<String, String> SINHALA_LABELS = new HashMap<>();
    static {
        SINHALA_LABELS.put("q", "ෘ");
        SINHALA_LABELS.put("w", "ව");
        SINHALA_LABELS.put("e", "එ");
        SINHALA_LABELS.put("r", "ර");
        SINHALA_LABELS.put("t", "ට");
        SINHALA_LABELS.put("y", "ය");
        SINHALA_LABELS.put("u", "උ");
        SINHALA_LABELS.put("i", "ඉ");
        SINHALA_LABELS.put("o", "ඔ");
        SINHALA_LABELS.put("p", "ප");
        SINHALA_LABELS.put("a", "අ");
        SINHALA_LABELS.put("s", "ස");
        SINHALA_LABELS.put("d", "ඩ");
        SINHALA_LABELS.put("f", "ෆ");
        SINHALA_LABELS.put("g", "ග");
        SINHALA_LABELS.put("h", "හ");
        SINHALA_LABELS.put("j", "ජ");
        SINHALA_LABELS.put("k", "ක");
        SINHALA_LABELS.put("l", "ල");
        SINHALA_LABELS.put("z", "ඤ");
        SINHALA_LABELS.put("x", "ං");
        SINHALA_LABELS.put("c", "ච");
        SINHALA_LABELS.put("v", "ව");
        SINHALA_LABELS.put("b", "බ");
        SINHALA_LABELS.put("n", "න");
        SINHALA_LABELS.put("m", "ම");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SINGLISH TO SINHALA - CONSONANTS (හල් අකුරු)
    // ═══════════════════════════════════════════════════════════════════
    private static final Map<String, String> CONSONANTS = new HashMap<>();
    static {
        // 3-letter combinations (check first)
        CONSONANTS.put("ndh", "ඳ්");
        CONSONANTS.put("nth", "න්ථ්");
        
        // 2-letter combinations
        CONSONANTS.put("th", "ත්");
        CONSONANTS.put("Th", "ථ්");
        CONSONANTS.put("dh", "ද්");
        CONSONANTS.put("Dh", "ධ්");
        CONSONANTS.put("kh", "ඛ්");
        CONSONANTS.put("gh", "ඝ්");
        CONSONANTS.put("ng", "ඟ්");
        CONSONANTS.put("ch", "ච්");
        CONSONANTS.put("Ch", "ඡ්");
        CONSONANTS.put("jh", "ඣ්");
        CONSONANTS.put("nd", "ඳ්");
        CONSONANTS.put("gn", "ඥ්");
        CONSONANTS.put("kn", "ඤ්");
        CONSONANTS.put("ph", "ඵ්");
        CONSONANTS.put("bh", "භ්");
        CONSONANTS.put("mb", "ඹ්");
        CONSONANTS.put("sh", "ශ්");
        CONSONANTS.put("Sh", "ෂ්");
        
        // Single letters
        CONSONANTS.put("t", "ට්");
        CONSONANTS.put("T", "ට්");
        CONSONANTS.put("d", "ඩ්");
        CONSONANTS.put("D", "ඩ්");
        CONSONANTS.put("k", "ක්");
        CONSONANTS.put("K", "ඛ්");
        CONSONANTS.put("g", "ග්");
        CONSONANTS.put("G", "ඝ්");
        CONSONANTS.put("c", "ච්");
        CONSONANTS.put("C", "ඡ්");
        CONSONANTS.put("j", "ජ්");
        CONSONANTS.put("J", "ඣ්");
        CONSONANTS.put("n", "න්");
        CONSONANTS.put("N", "ණ්");
        CONSONANTS.put("p", "ප්");
        CONSONANTS.put("P", "ඵ්");
        CONSONANTS.put("b", "බ්");
        CONSONANTS.put("B", "භ්");
        CONSONANTS.put("m", "ම්");
        CONSONANTS.put("y", "ය්");
        CONSONANTS.put("Y", "ය්");
        CONSONANTS.put("r", "ර්");
        CONSONANTS.put("l", "ල්");
        CONSONANTS.put("L", "ළ්");
        CONSONANTS.put("v", "ව්");
        CONSONANTS.put("w", "ව්");
        CONSONANTS.put("s", "ස්");
        CONSONANTS.put("S", "ෂ්");
        CONSONANTS.put("h", "හ්");
        CONSONANTS.put("f", "ෆ්");
        CONSONANTS.put("x", "ං");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SINGLISH TO SINHALA - STANDALONE VOWELS (වචන මුලදී)
    // ═══════════════════════════════════════════════════════════════════
    private static final Map<String, String> VOWELS = new HashMap<>();
    static {
        VOWELS.put("aa", "ආ");
        VOWELS.put("AA", "ඈ");
        VOWELS.put("ii", "ඊ");
        VOWELS.put("ee", "ඊ");
        VOWELS.put("uu", "ඌ");
        VOWELS.put("oo", "ඕ");
        VOWELS.put("ai", "ඓ");
        VOWELS.put("au", "ඖ");
        VOWELS.put("Ru", "ඍ");
        VOWELS.put("a", "අ");
        VOWELS.put("A", "ඇ");
        VOWELS.put("i", "ඉ");
        VOWELS.put("I", "ඊ");
        VOWELS.put("u", "උ");
        VOWELS.put("U", "ඌ");
        VOWELS.put("e", "එ");
        VOWELS.put("E", "ඒ");
        VOWELS.put("o", "ඔ");
        VOWELS.put("O", "ඕ");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SINGLISH TO SINHALA - VOWEL MODIFIERS (පිල්ලම්)
    // ═══════════════════════════════════════════════════════════════════
    private static final Map<String, String> VOWEL_MODIFIERS = new HashMap<>();
    static {
        VOWEL_MODIFIERS.put("aa", "ා");
        VOWEL_MODIFIERS.put("AA", "ෑ");
        VOWEL_MODIFIERS.put("ii", "ී");
        VOWEL_MODIFIERS.put("ee", "ී");
        VOWEL_MODIFIERS.put("uu", "ූ");
        VOWEL_MODIFIERS.put("oo", "ෝ");
        VOWEL_MODIFIERS.put("ai", "ෛ");
        VOWEL_MODIFIERS.put("au", "ෞ");
        VOWEL_MODIFIERS.put("Ru", "ෘ");
        VOWEL_MODIFIERS.put("a", "");  // Remove hal kirima
        VOWEL_MODIFIERS.put("A", "ැ");
        VOWEL_MODIFIERS.put("i", "ි");
        VOWEL_MODIFIERS.put("I", "ී");
        VOWEL_MODIFIERS.put("u", "ු");
        VOWEL_MODIFIERS.put("U", "ූ");
        VOWEL_MODIFIERS.put("e", "ෙ");
        VOWEL_MODIFIERS.put("E", "ේ");
        VOWEL_MODIFIERS.put("o", "ො");
        VOWEL_MODIFIERS.put("O", "ෝ");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // VIEWS AND STATE
    // ═══════════════════════════════════════════════════════════════════
    private FrameLayout rootContainer;
    private LinearLayout keyboardView;
    private LinearLayout emojiRowView;
    private View touchLayer;
    private Handler handler;
    private Vibrator vibrator;
    private KeyboardSettings settings;
    
    // Key preview popup
    private PopupWindow keyPreviewPopup;
    private TextView keyPreviewText;
    
    // State
    private boolean isShift = false;
    private boolean isCaps = false;
    private boolean isNumbers = false;
    private boolean isSymbols = false;
    private boolean isRepeating = false;
    private Runnable repeatRunnable;
    private boolean isSinhalaMode = false;
    private StringBuilder singlishBuffer = new StringBuilder();
    private boolean lastWasConsonant = false;
    
    private int navigationBarHeight = 0;
    
    // Key info for touch detection
    private List<KeyInfo> keyInfoList = new ArrayList<>();
    
    private static class KeyInfo {
        String key;
        View view;
        Rect bounds = new Rect();
        
        KeyInfo(String key, View view) {
            this.key = key;
            this.view = view;
        }
        
        void updateBounds() {
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            bounds.set(location[0], location[1],
                      location[0] + view.getWidth(),
                      location[1] + view.getHeight());
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // BROADCAST RECEIVER
    // ═══════════════════════════════════════════════════════════════════
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
    
    // ═══════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════
    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        
        try {
            settings = new KeyboardSettings(this);
            loadSettings();
        } catch (Exception e) {
            Log.e(TAG, "Error creating settings", e);
        }
        
        calculateNavigationBarHeight();
        setupKeyPreviewPopup();
        
        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception e) {
            Log.e(TAG, "Error getting vibrator", e);
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
        } catch (Exception e) {
            Log.e(TAG, "Error registering receiver", e);
        }
    }
    
    private void setupKeyPreviewPopup() {
        keyPreviewText = new TextView(this);
        keyPreviewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        keyPreviewText.setTextColor(Color.WHITE);
        keyPreviewText.setTypeface(Typeface.DEFAULT_BOLD);
        keyPreviewText.setGravity(Gravity.CENTER);
        keyPreviewText.setPadding(dp(24), dp(16), dp(24), dp(16));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#444444"));
        bg.setCornerRadius(dp(14));
        keyPreviewText.setBackground(bg);
        
        keyPreviewPopup = new PopupWindow(keyPreviewText,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        keyPreviewPopup.setClippingEnabled(false);
        keyPreviewPopup.setAnimationStyle(0);
    }
    
    @Override
    public void onDestroy() {
        stopRepeat();
        hideKeyPreview();
        try {
            unregisterReceiver(settingsReceiver);
        } catch (Exception e) {}
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
    
    // ═══════════════════════════════════════════════════════════════════
    // INPUT VIEW CREATION
    // ═══════════════════════════════════════════════════════════════════
    @Override
    public View onCreateInputView() {
        try {
            loadSettings();
            calculateNavigationBarHeight();
            
            rootContainer = new FrameLayout(this);
            rootContainer.setBackgroundColor(parseColor(colorBackground));
            
            // Main keyboard layout
            LinearLayout mainLayout = new LinearLayout(this);
            mainLayout.setOrientation(LinearLayout.VERTICAL);
            mainLayout.setBackgroundColor(parseColor(colorBackground));
            
            if (showEmojiRow) {
                emojiRowView = createEmojiRow();
                mainLayout.addView(emojiRowView);
            }
            
            keyboardView = createKeyboardLayout();
            mainLayout.addView(keyboardView);
            
            FrameLayout.LayoutParams mainParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            );
            mainParams.gravity = Gravity.BOTTOM;
            rootContainer.addView(mainLayout, mainParams);
            
            // Touch layer for gap detection (on top of keyboard)
            touchLayer = new View(this);
            touchLayer.setBackgroundColor(Color.TRANSPARENT);
            FrameLayout.LayoutParams touchParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            );
            touchLayer.setLayoutParams(touchParams);
            touchLayer.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return handleTouchLayerEvent(event);
                }
            });
            rootContainer.addView(touchLayer);
            
            // Calculate total height
            int emojiRowHeight = showEmojiRow ? dp(44) : 0;
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
                    updateAllKeyBounds();
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
            singlishBuffer.setLength(0);
            lastWasConsonant = false;
            
            if (info != null) {
                int inputClass = info.inputType & EditorInfo.TYPE_MASK_CLASS;
                isNumbers = (inputClass == EditorInfo.TYPE_CLASS_NUMBER ||
                             inputClass == EditorInfo.TYPE_CLASS_PHONE);
            }
            
            loadSettings();
            calculateNavigationBarHeight();
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
            colorText = settings.getColorText();
            
            keyboardHeight = settings.getKeyboardHeight();
            keyRadius = settings.getKeyRadius();
            keyGap = settings.getKeyGap();
            keyTextSize = settings.getKeyTextSize();
            
            vibrateEnabled = settings.isVibrationEnabled();
            vibrateDuration = settings.getVibrationStrength();
            showEmojiRow = settings.isShowEmojiRow();
            longPressDelay = settings.getLongPressDelay();
        } catch (Exception e) {
            Log.e(TAG, "Error loading settings", e);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // TOUCH LAYER FOR BETTER KEY DETECTION
    // ═══════════════════════════════════════════════════════════════════
    private void updateAllKeyBounds() {
        for (KeyInfo info : keyInfoList) {
            info.updateBounds();
        }
    }
    
    private boolean handleTouchLayerEvent(MotionEvent event) {
        float x = event.getRawX();
        float y = event.getRawY();
        
        // Find which key was touched or nearest key
        KeyInfo touchedKey = null;
        KeyInfo nearestKey = null;
        float minDistance = Float.MAX_VALUE;
        
        for (KeyInfo info : keyInfoList) {
            if (info.bounds.contains((int)x, (int)y)) {
                touchedKey = info;
                break;
            }
            
            // Calculate distance to center of key
            float centerX = info.bounds.centerX();
            float centerY = info.bounds.centerY();
            float distance = (float) Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
            
            if (distance < minDistance) {
                minDistance = distance;
                nearestKey = info;
            }
        }
        
        // Use touched key or nearest key if within threshold
        KeyInfo targetKey = touchedKey;
        if (targetKey == null && nearestKey != null && minDistance < dp(40)) {
            targetKey = nearestKey;
        }
        
        if (targetKey == null) return false;
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                targetKey.view.setAlpha(0.7f);
                targetKey.view.setScaleX(0.95f);
                targetKey.view.setScaleY(0.95f);
                doVibrate();
                
                if (!isSpecialKey(targetKey.key)) {
                    showKeyPreview(targetKey.view, getKeyDisplay(targetKey.key));
                }
                
                processKey(targetKey.key);
                
                if (targetKey.key.equals("⌫") || targetKey.key.equals("SPACE")) {
                    startRepeat(targetKey.key);
                }
                return true;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Reset all keys
                for (KeyInfo info : keyInfoList) {
                    info.view.setAlpha(1f);
                    info.view.setScaleX(1f);
                    info.view.setScaleY(1f);
                }
                hideKeyPreview();
                stopRepeat();
                return true;
        }
        
        return false;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // EMOJI ROW
    // ═══════════════════════════════════════════════════════════════════
    private LinearLayout createEmojiRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44)
        ));
        row.setPadding(dp(4), dp(4), dp(4), dp(4));
        row.setBackgroundColor(parseColor(colorKeySpecial));
        row.setElevation(dp(8));
        row.setClickable(true); // Prevent touch pass-through
        
        String emojiStr = "😀,😂,❤️,👍,🔥,✨,🎉,💯";
        try {
            emojiStr = settings.getQuickEmojis();
        } catch (Exception e) {}
        
        String[] emojis = emojiStr.split(",");
        for (final String emoji : emojis) {
            final String trimmedEmoji = emoji.trim();
            TextView tv = new TextView(this);
            tv.setText(trimmedEmoji);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
            );
            params.setMargins(dp(2), 0, dp(2), 0);
            tv.setLayoutParams(params);
            
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(parseColor(colorKeyNormal));
            bg.setCornerRadius(dp(8));
            tv.setBackground(bg);
            
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
    
    // ═══════════════════════════════════════════════════════════════════
    // KEYBOARD LAYOUT
    // ═══════════════════════════════════════════════════════════════════
    private LinearLayout createKeyboardLayout() {
        keyInfoList.clear();
        
        LinearLayout keyboard = new LinearLayout(this);
        keyboard.setOrientation(LinearLayout.VERTICAL);
        keyboard.setBackgroundColor(parseColor(colorBackground));
        
        keyboard.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(keyboardHeight)
        ));
        
        keyboard.setPadding(dp(3), dp(6), dp(3), dp(6));
        
        String[][] layout = getActiveLayout();
        for (int i = 0; i < layout.length; i++) {
            keyboard.addView(createRow(layout[i], i));
        }
        
        return keyboard;
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
            row.setPadding(dp(14), vertPad, dp(14), vertPad);
        } else {
            row.setPadding(0, vertPad, 0, vertPad);
        }
        
        for (String key : keys) {
            row.addView(createKey(key));
        }
        
        return row;
    }
    
    private View createKey(final String key) {
        final FrameLayout keyContainer = new FrameLayout(this);
        final TextView tv = new TextView(this);
        
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        
        // Text styling
        int textColor = parseColor(colorText);
        float textSize = isSpecialKey(key) ? 14 : keyTextSize;
        String displayText = getKeyDisplay(key);
        
        if (key.equals("↵")) {
            displayText = "↵";
            textColor = Color.WHITE;
            textSize = 20;
        } else if (key.equals("⇧")) {
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
        } else if (key.equals("⌫")) {
            displayText = "⌫";
            textSize = 22;
        } else if (key.equals("SPACE")) {
            displayText = "GD Keyboard";
            textSize = 10;
            textColor = Color.parseColor("#666666");
        } else if (key.equals("🌐")) {
            displayText = isSinhalaMode ? "SI" : "EN";
            textSize = 13;
            textColor = isSinhalaMode ? Color.parseColor("#10b981") : Color.parseColor("#3b82f6");
        } else if (key.equals("✨")) {
            displayText = "✨";
            textSize = 18;
        }
        
        tv.setText(displayText);
        tv.setTextColor(textColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        
        // Add Sinhala label for letter keys in Sinhala mode
        if (isSinhalaMode && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            String sinhalaLabel = SINHALA_LABELS.get(key.toLowerCase());
            if (sinhalaLabel != null) {
                TextView sinhalaLabelView = new TextView(this);
                sinhalaLabelView.setText(sinhalaLabel);
                sinhalaLabelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
                sinhalaLabelView.setTextColor(Color.parseColor("#888888"));
                
                FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                );
                labelParams.gravity = Gravity.TOP | Gravity.END;
                labelParams.setMargins(0, dp(2), dp(4), 0);
                sinhalaLabelView.setLayoutParams(labelParams);
                
                keyContainer.addView(sinhalaLabelView);
            }
        }
        
        // Layout params
        float weight = getKeyWeight(key);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.MATCH_PARENT, weight
        );
        containerParams.setMargins(dp(keyGap), dp(keyGap), dp(keyGap), dp(keyGap));
        keyContainer.setLayoutParams(containerParams);
        
        // Add TextView to container
        FrameLayout.LayoutParams tvParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        tv.setLayoutParams(tvParams);
        keyContainer.addView(tv, 0);
        
        // Background
        keyContainer.setBackground(createKeyBackground(key));
        
        // Store key info for touch detection
        KeyInfo keyInfo = new KeyInfo(key, keyContainer);
        keyInfoList.add(keyInfo);
        
        return keyContainer;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // KEY PREVIEW
    // ═══════════════════════════════════════════════════════════════════
    private void showKeyPreview(View anchor, String text) {
        if (text == null || text.isEmpty() || text.equals("GD Keyboard")) return;
        
        try {
            if (keyPreviewPopup == null || keyPreviewText == null) {
                setupKeyPreviewPopup();
            }
            
            keyPreviewText.setText(text);
            keyPreviewText.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            
            int[] location = new int[2];
            anchor.getLocationOnScreen(location);
            
            int popupWidth = Math.max(keyPreviewText.getMeasuredWidth(), dp(56));
            int popupHeight = keyPreviewText.getMeasuredHeight();
            
            int x = location[0] + (anchor.getWidth() / 2) - (popupWidth / 2);
            int y = location[1] - popupHeight - dp(12);
            
            if (y < 0) y = dp(10);
            
            if (keyPreviewPopup.isShowing()) {
                keyPreviewPopup.update(x, y, popupWidth, popupHeight);
            } else {
                keyPreviewPopup.setWidth(popupWidth);
                keyPreviewPopup.setHeight(popupHeight);
                keyPreviewPopup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing preview", e);
        }
    }
    
    private void hideKeyPreview() {
        try {
            if (keyPreviewPopup != null && keyPreviewPopup.isShowing()) {
                keyPreviewPopup.dismiss();
            }
        } catch (Exception e) {}
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // KEY HELPERS
    // ═══════════════════════════════════════════════════════════════════
    private String getKeyDisplay(String key) {
        if (key.equals("SPACE")) {
            return "GD Keyboard";
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
        if (key.equals("SPACE")) return 3.5f;
        if (key.equals("⇧") || key.equals("⌫")) return 1.5f;
        if (key.equals("↵") || key.equals("123") || key.equals("ABC") || key.equals("#+=")) return 1.3f;
        if (key.equals("🌐") || key.equals("✨")) return 1.0f;
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
    
    // ═══════════════════════════════════════════════════════════════════
    // REPEAT (BACKSPACE / SPACE)
    // ═══════════════════════════════════════════════════════════════════
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
    
    // ═══════════════════════════════════════════════════════════════════
    // KEY PROCESSING
    // ═══════════════════════════════════════════════════════════════════
    private void processKey(String key) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        
        if (key.equals("⇧")) {
            handleShiftKey();
        } else if (key.equals("⌫")) {
            handleBackspace(ic);
        } else if (key.equals("↵")) {
            handleEnterKey(ic);
        } else if (key.equals("SPACE")) {
            handleSpaceKey(ic);
        } else if (key.equals("123")) {
            flushSinglishBuffer(ic);
            isNumbers = true;
            isSymbols = false;
            rebuildKeyboard();
        } else if (key.equals("ABC")) {
            flushSinglishBuffer(ic);
            isNumbers = false;
            isSymbols = false;
            rebuildKeyboard();
        } else if (key.equals("#+=")) {
            flushSinglishBuffer(ic);
            isSymbols = true;
            isNumbers = false;
            rebuildKeyboard();
        } else if (key.equals("🌐")) {
            flushSinglishBuffer(ic);
            isSinhalaMode = !isSinhalaMode;
            singlishBuffer.setLength(0);
            lastWasConsonant = false;
            rebuildKeyboard();
        } else if (key.equals("✨")) {
            flushSinglishBuffer(ic);
            openPopup();
        } else {
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
    
    private void handleBackspace(InputConnection ic) {
        // Clear singlish buffer first
        if (singlishBuffer.length() > 0) {
            singlishBuffer.setLength(singlishBuffer.length() - 1);
            if (singlishBuffer.length() == 0) {
                lastWasConsonant = false;
            }
            return;
        }
        
        // Fast simple delete
        ic.deleteSurroundingText(1, 0);
    }
    
    private void handleEnterKey(InputConnection ic) {
        flushSinglishBuffer(ic);
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
    
    private void handleSpaceKey(InputConnection ic) {
        flushSinglishBuffer(ic);
        ic.commitText(" ", 1);
        lastWasConsonant = false;
        autoUnshift();
    }
    
    private void handleCharacterKey(InputConnection ic, String key) {
        String text = key;
        if ((isShift || isCaps) && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            text = key.toUpperCase();
        }
        
        if (isSinhalaMode && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            processSinglishInput(ic, text);
        } else {
            ic.commitText(text, 1);
            autoUnshift();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SINGLISH TO SINHALA ENGINE
    // ═══════════════════════════════════════════════════════════════════
    private void processSinglishInput(InputConnection ic, String input) {
        singlishBuffer.append(input);
        String buffer = singlishBuffer.toString();
        
        // Try to find matches - check longest first (3, then 2, then 1)
        String result = tryConvert(buffer);
        
        if (result != null) {
            // Delete buffer content that was shown
            int deleteCount = singlishBuffer.length() - input.length();
            if (deleteCount > 0) {
                // We need to delete what was previously typed as raw
            }
            
            ic.commitText(result, 1);
            singlishBuffer.setLength(0);
            
            // Track if last output was a consonant (ends with hal kirima)
            lastWasConsonant = result.endsWith("්");
        } else {
            // Check if buffer is getting too long without match
            if (buffer.length() >= 3) {
                // Output first character and continue
                String firstChar = String.valueOf(buffer.charAt(0));
                String firstResult = tryConvertSingle(firstChar);
                if (firstResult != null) {
                    ic.commitText(firstResult, 1);
                    lastWasConsonant = firstResult.endsWith("්");
                } else {
                    ic.commitText(firstChar, 1);
                    lastWasConsonant = false;
                }
                singlishBuffer.deleteCharAt(0);
            }
        }
        
        autoUnshift();
    }
    
    private String tryConvert(String buffer) {
        // Check if this is a vowel after consonant (need to apply modifier)
        if (lastWasConsonant && buffer.length() >= 1) {
            // Try vowel modifiers (longest first)
            for (int len = Math.min(buffer.length(), 2); len >= 1; len--) {
                String sub = buffer.substring(0, len);
                if (VOWEL_MODIFIERS.containsKey(sub)) {
                    String modifier = VOWEL_MODIFIERS.get(sub);
                    // We need to remove the hal kirima from last consonant
                    // This is done by the modifier itself
                    if (sub.equals("a") && modifier.isEmpty()) {
                        // Just remove hal kirima - return empty to signal completion
                        return ""; // Will cause buffer clear
                    }
                    return modifier;
                }
            }
        }
        
        // Try consonants (3-letter, 2-letter, 1-letter)
        for (int len = Math.min(buffer.length(), 3); len >= 1; len--) {
            String sub = buffer.substring(0, len);
            if (CONSONANTS.containsKey(sub)) {
                return CONSONANTS.get(sub);
            }
        }
        
        // Try standalone vowels (only at word start, but we'll allow anywhere for simplicity)
        for (int len = Math.min(buffer.length(), 2); len >= 1; len--) {
            String sub = buffer.substring(0, len);
            if (VOWELS.containsKey(sub)) {
                return VOWELS.get(sub);
            }
        }
        
        return null;
    }
    
    private String tryConvertSingle(String c) {
        if (CONSONANTS.containsKey(c)) return CONSONANTS.get(c);
        if (VOWELS.containsKey(c)) return VOWELS.get(c);
        return null;
    }
    
    private void flushSinglishBuffer(InputConnection ic) {
        if (singlishBuffer.length() > 0 && ic != null) {
            String remaining = singlishBuffer.toString();
            String result = tryConvert(remaining);
            if (result != null && !result.isEmpty()) {
                ic.commitText(result, 1);
            } else {
                // Commit as-is
                ic.commitText(remaining, 1);
            }
            singlishBuffer.setLength(0);
            lastWasConsonant = false;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════
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
    
    private void openPopup() {
        try {
            Intent intent = new Intent(this, PopupActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Cannot open popup", e);
        }
    }
    
    private void rebuildKeyboard() {
        if (rootContainer == null) return;
        
        keyInfoList.clear();
        rootContainer.removeAllViews();
        
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(parseColor(colorBackground));
        
        if (showEmojiRow) {
            emojiRowView = createEmojiRow();
            mainLayout.addView(emojiRowView);
        }
        
        keyboardView = createKeyboardLayout();
        mainLayout.addView(keyboardView);
        
        FrameLayout.LayoutParams mainParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        mainParams.gravity = Gravity.BOTTOM;
        rootContainer.addView(mainLayout, mainParams);
        
        // Re-add touch layer
        touchLayer = new View(this);
        touchLayer.setBackgroundColor(Color.TRANSPARENT);
        FrameLayout.LayoutParams touchParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        );
        touchLayer.setLayoutParams(touchParams);
        touchLayer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return handleTouchLayerEvent(event);
            }
        });
        rootContainer.addView(touchLayer);
        
        int emojiRowHeight = showEmojiRow ? dp(44) : 0;
        int mainHeight = dp(keyboardHeight);
        int totalHeight = emojiRowHeight + mainHeight + navigationBarHeight;
        
        ViewGroup.LayoutParams containerParams = rootContainer.getLayoutParams();
        if (containerParams != null) {
            containerParams.height = totalHeight;
            rootContainer.setLayoutParams(containerParams);
        }
        
        rootContainer.setPadding(0, 0, 0, navigationBarHeight);
        rootContainer.setBackgroundColor(parseColor(colorBackground));
        
        // Update key bounds after layout
        rootContainer.post(new Runnable() {
            @Override
            public void run() {
                updateAllKeyBounds();
            }
        });
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