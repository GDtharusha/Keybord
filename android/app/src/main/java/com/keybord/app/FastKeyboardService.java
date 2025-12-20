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

import java.util.HashMap;
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
    
    // Sinhala labels for each English key
    private static final Map<String, String> SINHALA_LABELS = new HashMap<>();
    static {
        SINHALA_LABELS.put("q", "්");
        SINHALA_LABELS.put("w", "ව");
        SINHALA_LABELS.put("e", "එ");
        SINHALA_LABELS.put("r", "ර");
        SINHALA_LABELS.put("t", "ත");
        SINHALA_LABELS.put("y", "ය");
        SINHALA_LABELS.put("u", "උ");
        SINHALA_LABELS.put("i", "ඉ");
        SINHALA_LABELS.put("o", "ඔ");
        SINHALA_LABELS.put("p", "ප");
        SINHALA_LABELS.put("a", "අ");
        SINHALA_LABELS.put("s", "ස");
        SINHALA_LABELS.put("d", "ද");
        SINHALA_LABELS.put("f", "ෆ");
        SINHALA_LABELS.put("g", "ග");
        SINHALA_LABELS.put("h", "හ");
        SINHALA_LABELS.put("j", "ජ");
        SINHALA_LABELS.put("k", "ක");
        SINHALA_LABELS.put("l", "ල");
        SINHALA_LABELS.put("z", "ඤ");
        SINHALA_LABELS.put("x", "ඥ");
        SINHALA_LABELS.put("c", "ච");
        SINHALA_LABELS.put("v", "ව");
        SINHALA_LABELS.put("b", "බ");
        SINHALA_LABELS.put("n", "න");
        SINHALA_LABELS.put("m", "ම");
    }
    
    // Singlish to Sinhala conversion map
    private static final Map<String, String> SINGLISH_MAP = new HashMap<>();
    static {
        // Single consonants
        SINGLISH_MAP.put("k", "ක");
        SINGLISH_MAP.put("g", "ග");
        SINGLISH_MAP.put("c", "ච");
        SINGLISH_MAP.put("j", "ජ");
        SINGLISH_MAP.put("t", "ත");
        SINGLISH_MAP.put("d", "ද");
        SINGLISH_MAP.put("p", "ප");
        SINGLISH_MAP.put("b", "බ");
        SINGLISH_MAP.put("m", "ම");
        SINGLISH_MAP.put("y", "ය");
        SINGLISH_MAP.put("r", "ර");
        SINGLISH_MAP.put("l", "ල");
        SINGLISH_MAP.put("w", "ව");
        SINGLISH_MAP.put("v", "ව");
        SINGLISH_MAP.put("s", "ස");
        SINGLISH_MAP.put("h", "හ");
        SINGLISH_MAP.put("n", "න");
        SINGLISH_MAP.put("f", "ෆ");
        
        // Double consonants
        SINGLISH_MAP.put("kh", "ඛ");
        SINGLISH_MAP.put("gh", "ඝ");
        SINGLISH_MAP.put("ch", "ඡ");
        SINGLISH_MAP.put("jh", "ඣ");
        SINGLISH_MAP.put("th", "ථ");
        SINGLISH_MAP.put("dh", "ධ");
        SINGLISH_MAP.put("ph", "ඵ");
        SINGLISH_MAP.put("bh", "භ");
        SINGLISH_MAP.put("sh", "ශ");
        SINGLISH_MAP.put("ng", "ඞ");
        SINGLISH_MAP.put("ny", "ඤ");
        
        // Vowels (standalone)
        SINGLISH_MAP.put("a", "අ");
        SINGLISH_MAP.put("aa", "ආ");
        SINGLISH_MAP.put("i", "ඉ");
        SINGLISH_MAP.put("ii", "ඊ");
        SINGLISH_MAP.put("u", "උ");
        SINGLISH_MAP.put("uu", "ඌ");
        SINGLISH_MAP.put("e", "එ");
        SINGLISH_MAP.put("ee", "ඒ");
        SINGLISH_MAP.put("o", "ඔ");
        SINGLISH_MAP.put("oo", "ඕ");
        SINGLISH_MAP.put("A", "ඇ");
        SINGLISH_MAP.put("AA", "ඈ");
    }
    
    // Vowel signs (after consonant)
    private static final Map<String, String> VOWEL_SIGNS = new HashMap<>();
    static {
        VOWEL_SIGNS.put("a", "");
        VOWEL_SIGNS.put("aa", "ා");
        VOWEL_SIGNS.put("i", "ි");
        VOWEL_SIGNS.put("ii", "ී");
        VOWEL_SIGNS.put("u", "ු");
        VOWEL_SIGNS.put("uu", "ූ");
        VOWEL_SIGNS.put("e", "ෙ");
        VOWEL_SIGNS.put("ee", "ේ");
        VOWEL_SIGNS.put("o", "ො");
        VOWEL_SIGNS.put("oo", "ෝ");
        VOWEL_SIGNS.put("A", "ැ");
        VOWEL_SIGNS.put("AA", "ෑ");
    }
    
    // Views
    private FrameLayout rootContainer;
    private LinearLayout keyboardView;
    private LinearLayout emojiRowView;
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
    
    private int navigationBarHeight = 0;
    
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
        keyPreviewText.setPadding(dp(20), dp(14), dp(20), dp(14));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#333333"));
        bg.setCornerRadius(dp(12));
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
    
    @Override
    public View onCreateInputView() {
        try {
            loadSettings();
            calculateNavigationBarHeight();
            
            rootContainer = new FrameLayout(this);
            rootContainer.setBackgroundColor(parseColor(colorBackground));
            
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
            
            int emojiRowHeight = showEmojiRow ? dp(44) : 0;
            int mainHeight = dp(keyboardHeight);
            int totalHeight = emojiRowHeight + mainHeight + navigationBarHeight;
            
            rootContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                totalHeight
            ));
            
            rootContainer.setPadding(0, 0, 0, navigationBarHeight);
            
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
    
    private LinearLayout createEmojiRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(44)
        ));
        row.setPadding(dp(4), dp(4), dp(4), dp(4));
        row.setBackgroundColor(parseColor(colorKeySpecial));
        row.setElevation(dp(4));
        
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
    
    private LinearLayout createKeyboardLayout() {
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
        
        // Set key display text
        String displayText = getKeyDisplay(key);
        tv.setText(displayText);
        
        // Text styling
        int textColor = parseColor(colorText);
        float textSize = isSpecialKey(key) ? 14 : keyTextSize;
        
        if (key.equals("↵")) {
            textColor = Color.WHITE;
            textSize = 20;
        } else if (key.equals("⇧")) {
            textSize = 24;
            if (isCaps) {
                tv.setText("⇪");
                textColor = Color.parseColor("#10b981");
            } else if (isShift) {
                tv.setText("⬆");
                textColor = Color.parseColor("#3b82f6");
            } else {
                tv.setText("⇧");
            }
        } else if (key.equals("⌫")) {
            tv.setText("⌫");
            textSize = 22;
        } else if (key.equals("SPACE")) {
            tv.setText("GD Keyboard");
            textSize = 12;
            textColor = Color.parseColor("#666666");
        } else if (key.equals("🌐")) {
            tv.setText(isSinhalaMode ? "SI" : "EN");
            textSize = 13;
            textColor = isSinhalaMode ? Color.parseColor("#10b981") : Color.parseColor("#3b82f6");
        } else if (key.equals("✨")) {
            tv.setText("✨");
            textSize = 18;
        }
        
        tv.setTextColor(textColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        
        // Add Sinhala subscript label for letter keys in Sinhala mode
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
                labelParams.setMargins(0, dp(3), dp(5), 0);
                sinhalaLabelView.setLayoutParams(labelParams);
                
                keyContainer.addView(sinhalaLabelView);
            }
        }
        
        // Layout
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
        
        // Touch handling with preview
        keyContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setAlpha(0.7f);
                        v.setScaleX(0.95f);
                        v.setScaleY(0.95f);
                        doVibrate();
                        
                        // Show key preview for letter/number keys
                        if (!isSpecialKey(key)) {
                            showKeyPreview(v, getKeyDisplay(key));
                        }
                        
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
                        hideKeyPreview();
                        stopRepeat();
                        return true;
                }
                return false;
            }
        });
        
        keyContainer.setClickable(true);
        return keyContainer;
    }
    
    private void showKeyPreview(View anchor, String text) {
        if (text == null || text.isEmpty() || text.equals("GD Keyboard")) return;
        
        try {
            if (keyPreviewPopup == null || keyPreviewText == null) {
                setupKeyPreviewPopup();
            }
            
            keyPreviewText.setText(text);
            keyPreviewText.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            
            int[] location = new int[2];
            anchor.getLocationInWindow(location);
            
            int popupWidth = Math.max(keyPreviewText.getMeasuredWidth(), dp(50));
            int popupHeight = keyPreviewText.getMeasuredHeight();
            
            int x = location[0] + (anchor.getWidth() / 2) - (popupWidth / 2);
            int y = location[1] - popupHeight - dp(15);
            
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
        } catch (Exception e) {
            Log.e(TAG, "Error hiding preview", e);
        }
    }
    
    private String getKeyDisplay(String key) {
        if (key.equals("⇧")) {
            if (isCaps) return "⇪";
            if (isShift) return "⬆";
            return "⇧";
        }
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
        } else if (key.equals("SPACE")) {
            flushSinglishBuffer(ic);
            ic.commitText(" ", 1);
            autoUnshift();
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
            rebuildKeyboard();
        } else if (key.equals("✨")) {
            flushSinglishBuffer(ic);
            openPopup();
        } else {
            // Regular character
            String text = key;
            if ((isShift || isCaps) && key.length() == 1 && Character.isLetter(key.charAt(0))) {
                text = key.toUpperCase();
            }
            
            if (isSinhalaMode && key.length() == 1 && Character.isLetter(key.charAt(0))) {
                processSinglishInput(ic, text);
            } else {
                ic.commitText(text, 1);
            }
            
            autoUnshift();
        }
    }
    
    private void processSinglishInput(InputConnection ic, String input) {
        singlishBuffer.append(input.toLowerCase());
        String buffer = singlishBuffer.toString();
        
        // Try to find longest matching pattern
        String result = null;
        int matchLength = 0;
        
        // Check for multi-character matches first (longest match)
        for (int len = Math.min(buffer.length(), 3); len >= 1; len--) {
            String sub = buffer.substring(buffer.length() - len);
            if (SINGLISH_MAP.containsKey(sub)) {
                result = SINGLISH_MAP.get(sub);
                matchLength = len;
                break;
            }
        }
        
        if (result != null) {
            // Delete previously typed characters if any
            if (matchLength > 1) {
                ic.deleteSurroundingText(matchLength - 1, 0);
            }
            ic.commitText(result, 1);
            singlishBuffer.setLength(0);
        } else if (buffer.length() > 2) {
            // No match found and buffer is getting long, commit first char as-is
            ic.commitText(String.valueOf(buffer.charAt(0)), 1);
            singlishBuffer.deleteCharAt(0);
        }
    }
    
    private void flushSinglishBuffer(InputConnection ic) {
        if (singlishBuffer.length() > 0 && ic != null) {
            String remaining = singlishBuffer.toString();
            if (SINGLISH_MAP.containsKey(remaining)) {
                ic.commitText(SINGLISH_MAP.get(remaining), 1);
            }
            singlishBuffer.setLength(0);
        }
    }
    
    private void deleteCharacter(InputConnection ic) {
        if (ic == null) return;
        
        // Clear singlish buffer first if not empty
        if (singlishBuffer.length() > 0) {
            singlishBuffer.setLength(singlishBuffer.length() - 1);
            return;
        }
        
        // Simple fast delete
        CharSequence before = ic.getTextBeforeCursor(1, 0);
        if (before != null && before.length() > 0) {
            char c = before.charAt(0);
            if (Character.isLowSurrogate(c)) {
                ic.deleteSurroundingText(2, 0);
            } else {
                ic.deleteSurroundingText(1, 0);
            }
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
        } catch (Exception e) {
            Log.e(TAG, "Vibration error", e);
        }
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