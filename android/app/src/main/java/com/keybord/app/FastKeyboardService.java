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
    
    // Sinhala Labels
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
    // SINGLISH MAPPINGS - EXACT FROM YOUR IMAGE
    // ═══════════════════════════════════════════════════════════════════
    
    // Two-char consonant combinations (checked FIRST)
    private static final Map<String, String> CONSONANT_COMBOS = new HashMap<>();
    static {
        CONSONANT_COMBOS.put("kh", "ඛ"); CONSONANT_COMBOS.put("gh", "ඝ");
        CONSONANT_COMBOS.put("ch", "ච"); CONSONANT_COMBOS.put("Ch", "ඡ");
        CONSONANT_COMBOS.put("jh", "ඣ"); CONSONANT_COMBOS.put("Jh", "ඣ");
        CONSONANT_COMBOS.put("th", "ත"); CONSONANT_COMBOS.put("Th", "ථ");
        CONSONANT_COMBOS.put("dh", "ද"); CONSONANT_COMBOS.put("Dh", "ධ");
        CONSONANT_COMBOS.put("ph", "ඵ"); CONSONANT_COMBOS.put("bh", "භ");
        CONSONANT_COMBOS.put("sh", "ශ"); CONSONANT_COMBOS.put("Sh", "ෂ");
        CONSONANT_COMBOS.put("zj", "ඦ"); CONSONANT_COMBOS.put("zd", "ඬ");
        CONSONANT_COMBOS.put("zq", "ඳ"); CONSONANT_COMBOS.put("zk", "ඤ");
        CONSONANT_COMBOS.put("zh", "ඥ"); CONSONANT_COMBOS.put("zb", "ඹ");
        CONSONANT_COMBOS.put("zn", "ං"); CONSONANT_COMBOS.put("Lu", "ළු");
        CONSONANT_COMBOS.put("ng", "ඟ"); CONSONANT_COMBOS.put("nd", "ඳ");
        CONSONANT_COMBOS.put("mb", "ඹ");
    }
    
    // Single consonants
    private static final Map<Character, String> CONSONANTS = new HashMap<>();
    static {
        CONSONANTS.put('k', "ක"); CONSONANTS.put('K', "ඛ");
        CONSONANTS.put('g', "ග"); CONSONANTS.put('G', "ඝ");
        CONSONANTS.put('c', "ච"); CONSONANTS.put('C', "ඡ");
        CONSONANTS.put('j', "ජ"); CONSONANTS.put('J', "ඣ");
        CONSONANTS.put('t', "ට"); CONSONANTS.put('T', "ඨ");
        CONSONANTS.put('d', "ඩ"); CONSONANTS.put('D', "ඪ");
        CONSONANTS.put('n', "න"); CONSONANTS.put('N', "ණ");
        CONSONANTS.put('p', "ප"); CONSONANTS.put('P', "ඵ");
        CONSONANTS.put('b', "බ"); CONSONANTS.put('B', "ඹ");
        CONSONANTS.put('m', "ම"); CONSONANTS.put('M', "ම");
        CONSONANTS.put('y', "ය"); CONSONANTS.put('Y', "ය");
        CONSONANTS.put('r', "ර"); CONSONANTS.put('R', "ර");
        CONSONANTS.put('l', "ල"); CONSONANTS.put('L', "ළ");
        CONSONANTS.put('w', "ව"); CONSONANTS.put('W', "ව");
        CONSONANTS.put('v', "ව"); CONSONANTS.put('V', "ව");
        CONSONANTS.put('s', "ස"); CONSONANTS.put('S', "ෂ");
        CONSONANTS.put('h', "හ"); CONSONANTS.put('H', "හ");
        CONSONANTS.put('f', "ෆ"); CONSONANTS.put('F', "ෆ");
        CONSONANTS.put('z', "ඤ"); CONSONANTS.put('Z', "ඥ");
        CONSONANTS.put('q', "ක"); CONSONANTS.put('Q', "ඛ");
    }
    
    // Standalone vowels
    private static final Map<String, String> VOWELS = new HashMap<>();
    static {
        VOWELS.put("a", "අ"); VOWELS.put("A", "ඇ");
        VOWELS.put("aa", "ආ"); VOWELS.put("Aa", "ඈ"); VOWELS.put("AA", "ඈ");
        VOWELS.put("i", "ඉ"); VOWELS.put("I", "ඊ"); VOWELS.put("ii", "ඊ");
        VOWELS.put("u", "උ"); VOWELS.put("U", "ඌ"); VOWELS.put("uu", "ඌ");
        VOWELS.put("e", "එ"); VOWELS.put("E", "ඓ");
        VOWELS.put("ee", "ඒ"); VOWELS.put("ei", "ඒ");
        VOWELS.put("o", "ඔ"); VOWELS.put("O", "ඕ");
        VOWELS.put("oo", "ඕ"); VOWELS.put("oe", "ඕ");
        VOWELS.put("au", "ඖ");
        VOWELS.put("ru", "ඍ"); VOWELS.put("ruu", "ඎ");
    }
    
    // Vowel modifiers (pilla)
    private static final Map<String, String> MODIFIERS = new HashMap<>();
    static {
        MODIFIERS.put("a", ""); // Just removes hal
        MODIFIERS.put("A", "ැ");
        MODIFIERS.put("aa", "ා"); MODIFIERS.put("Aa", "ෑ"); MODIFIERS.put("AA", "ෑ");
        MODIFIERS.put("i", "ි"); MODIFIERS.put("I", "ී"); MODIFIERS.put("ii", "ී");
        MODIFIERS.put("u", "ු"); MODIFIERS.put("U", "ූ"); MODIFIERS.put("uu", "ූ");
        MODIFIERS.put("e", "ෙ"); MODIFIERS.put("E", "ෛ");
        MODIFIERS.put("ee", "ේ"); MODIFIERS.put("ei", "ේ");
        MODIFIERS.put("o", "ො"); MODIFIERS.put("O", "ෝ");
        MODIFIERS.put("oo", "ෝ"); MODIFIERS.put("oe", "ෝ");
        MODIFIERS.put("au", "ෞ");
        MODIFIERS.put("ru", "ෘ"); MODIFIERS.put("ruu", "ෲ");
    }
    
    // Special symbols
    private static final String HAL = "්";
    private static final String YANSAYA = "්‍ය";
    private static final String RAKARANSAYA = "්‍ර";
    
    // ═══════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════
    private FrameLayout rootContainer;
    private LinearLayout keyboardContainer;
    private LinearLayout keyboardView;
    private TextView keyPreviewView;
    private ImageView backgroundImageView;
    private Handler handler;
    private Vibrator vibrator;
    private KeyboardSettings settings;
    
    private boolean isShift = false;
    private boolean isCaps = false;
    private boolean isNumbers = false;
    private boolean isSymbols = false;
    private boolean isSinhalaMode = false;
    
    private boolean isRepeating = false;
    private Runnable repeatRunnable;
    
    // For tracking last typed character (for combos)
    private char lastTypedChar = 0;
    private long lastTypedTime = 0;
    private boolean lastWasHal = false;
    
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
    
    // Broadcast receiver
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
        
        try {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        } catch (Exception e) {}
        
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
        
        if (showEmojiRow) {
            keyboardContainer.addView(createEmojiRow());
        }
        
        keyboardView = createKeyboard();
        keyboardContainer.addView(keyboardView);
        
        FrameLayout.LayoutParams kbParams = new FrameLayout.LayoutParams(-1, -2);
        kbParams.gravity = Gravity.BOTTOM;
        rootContainer.addView(keyboardContainer, kbParams);
        
        // Key Preview (ABOVE keyboard)
        keyPreviewView = new TextView(this);
        keyPreviewView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        keyPreviewView.setTextColor(Color.WHITE);
        keyPreviewView.setTypeface(Typeface.DEFAULT_BOLD);
        keyPreviewView.setGravity(Gravity.CENTER);
        keyPreviewView.setVisibility(View.INVISIBLE);
        
        GradientDrawable previewBg = new GradientDrawable();
        previewBg.setColor(Color.parseColor("#404040"));
        previewBg.setCornerRadius(dp(8));
        keyPreviewView.setBackground(previewBg);
        
        FrameLayout.LayoutParams previewParams = new FrameLayout.LayoutParams(dp(52), dp(70));
        rootContainer.addView(keyPreviewView, previewParams);
        
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
        lastTypedChar = 0;
        lastWasHal = false;
        
        if (info != null) {
            int cls = info.inputType & EditorInfo.TYPE_MASK_CLASS;
            isNumbers = (cls == EditorInfo.TYPE_CLASS_NUMBER || cls == EditorInfo.TYPE_CLASS_PHONE);
        }
        rebuildKeyboard();
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
        // Find nearest
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
        // Debounce for action keys
        long now = System.currentTimeMillis();
        if (isActionKey(ki.key)) {
            if (now - lastActionTime < 200) return;
            lastActionTime = now;
        }
        
        applyPressVisual(ki);
        vibrate();
        showPreview(ki);
        processKey(ki.key);
        
        if (ki.key.equals("⌫")) startRepeat(ki.key);
    }
    
    private boolean isActionKey(String k) {
        return k.equals("⇧") || k.equals("123") || k.equals("ABC") || 
               k.equals("#+=") || k.equals("🌐");
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
    // KEY PREVIEW - FIXED
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
        
        keyPreviewView.setText(text);
        keyPreviewView.setVisibility(View.VISIBLE);
        
        // Position above key
        int[] loc = new int[2];
        ki.view.getLocationInWindow(loc);
        
        int keyW = ki.view.getWidth();
        int keyH = ki.view.getHeight();
        int previewW = keyW;
        int previewH = (int)(keyH * 1.5);
        
        int px = loc[0] + (keyW - previewW) / 2;
        int py = loc[1] - previewH - dp(6);
        
        // Keep on screen
        int screenW = getResources().getDisplayMetrics().widthPixels;
        if (px < dp(2)) px = dp(2);
        if (px + previewW > screenW - dp(2)) px = screenW - previewW - dp(2);
        if (py < 0) py = dp(2);
        
        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) keyPreviewView.getLayoutParams();
        p.width = previewW;
        p.height = previewH;
        p.leftMargin = px;
        p.topMargin = py;
        keyPreviewView.setLayoutParams(p);
    }
    
    private void hidePreview() {
        keyPreviewView.setVisibility(View.INVISIBLE);
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
            case "SPACE": 
                display = isSinhalaMode ? "සිංහල" : "English"; 
                textSize = 11; 
                textColor = Color.parseColor("#666666"); 
                break;
            case "🌐":
                display = isSinhalaMode ? "සිං" : "EN";
                textSize = 12;
                textColor = isSinhalaMode ? Color.parseColor("#10b981") : Color.parseColor("#3b82f6");
                break;
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
            case "↵": handleEnter(ic); break;
            case "SPACE": commitDirect(" "); resetSinglishState(); break;
            case "123": isNumbers = true; isSymbols = false; rebuildKeyboard(); break;
            case "ABC": isNumbers = false; isSymbols = false; rebuildKeyboard(); break;
            case "#+=": isSymbols = true; rebuildKeyboard(); break;
            case "🌐": isSinhalaMode = !isSinhalaMode; resetSinglishState(); rebuildKeyboard(); break;
            case "✨": openPopup(); break;
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
        ic.deleteSurroundingText(1, 0);
        resetSinglishState();
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
        resetSinglishState();
    }
    
    private void handleChar(InputConnection ic, String key) {
        char c = key.charAt(0);
        if ((isShift || isCaps) && Character.isLetter(c)) {
            c = Character.toUpperCase(c);
        }
        
        if (isSinhalaMode && Character.isLetter(c)) {
            processSinglish(ic, c);
        } else {
            ic.commitText(String.valueOf(c), 1);
            autoResetShift();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SINGLISH ENGINE - ULTRA FAST, REAL-TIME
    // ═══════════════════════════════════════════════════════════════════
    private void processSinglish(InputConnection ic, char c) {
        long now = System.currentTimeMillis();
        boolean isVowel = "aeiouAEIOU".indexOf(c) >= 0;
        
        // Check for two-char combo with last character
        if (now - lastTypedTime < 500 && lastTypedChar != 0) {
            String combo = "" + lastTypedChar + c;
            
            // Check consonant combos (kh, gh, ch, etc.)
            if (CONSONANT_COMBOS.containsKey(combo)) {
                // Delete previous consonant+hal and output combined
                ic.deleteSurroundingText(2, 0); // Remove consonant + hal
                String combined = CONSONANT_COMBOS.get(combo);
                ic.commitText(combined + HAL, 1);
                lastTypedChar = c;
                lastTypedTime = now;
                lastWasHal = true;
                autoResetShift();
                return;
            }
            
            // Check double vowels (aa, ee, ii, oo, uu)
            if (lastWasHal && MODIFIERS.containsKey(combo)) {
                // Delete hal + previous modifier
                CharSequence before = ic.getTextBeforeCursor(2, 0);
                if (before != null && before.length() >= 1) {
                    String mod = MODIFIERS.get(combo);
                    // Delete previous output and apply combined modifier
                    ic.deleteSurroundingText(1, 0); // Remove hal or previous modifier
                    if (mod != null && !mod.isEmpty()) {
                        ic.commitText(mod, 1);
                    }
                    lastTypedChar = c;
                    lastTypedTime = now;
                    lastWasHal = false;
                    autoResetShift();
                    return;
                }
            }
            
            // Check for double vowel standalone (aa, ee, etc.)
            if (!lastWasHal && VOWELS.containsKey(combo)) {
                ic.deleteSurroundingText(1, 0); // Remove previous vowel
                ic.commitText(VOWELS.get(combo), 1);
                lastTypedChar = c;
                lastTypedTime = now;
                lastWasHal = false;
                autoResetShift();
                return;
            }
        }
        
        // Single character processing
        if (isVowel) {
            if (lastWasHal) {
                // Apply vowel modifier
                String singleMod = MODIFIERS.get(String.valueOf(c));
                if (singleMod != null) {
                    ic.deleteSurroundingText(1, 0); // Remove hal
                    if (!singleMod.isEmpty()) {
                        ic.commitText(singleMod, 1);
                    }
                    lastWasHal = false;
                } else {
                    // Unknown vowel - output as is
                    ic.commitText(String.valueOf(c), 1);
                    lastWasHal = false;
                }
            } else {
                // Standalone vowel
                String vowel = VOWELS.get(String.valueOf(c));
                if (vowel != null) {
                    ic.commitText(vowel, 1);
                } else {
                    ic.commitText(String.valueOf(c), 1);
                }
                lastWasHal = false;
            }
        } else {
            // Consonant
            
            // Special case: Yansaya (y after consonant)
            if ((c == 'y' || c == 'Y') && lastWasHal) {
                ic.deleteSurroundingText(1, 0); // Remove hal
                ic.commitText(YANSAYA, 1);
                lastWasHal = false;
                lastTypedChar = c;
                lastTypedTime = now;
                autoResetShift();
                return;
            }
            
            // Special case: Rakaransaya (r after consonant, when next is vowel - handle later)
            // For now, 'r' after consonant starts new consonant
            
            // Special: x = ං, H = ඃ (no hal)
            if (c == 'x') {
                ic.commitText("ං", 1);
                lastWasHal = false;
            } else if (c == 'X') {
                ic.commitText("ඞ", 1);
                lastWasHal = false;
            } else if (c == 'H' && !lastWasHal) {
                ic.commitText("ඃ", 1);
                lastWasHal = false;
            } else {
                // Normal consonant
                String cons = CONSONANTS.get(c);
                if (cons != null) {
                    ic.commitText(cons + HAL, 1);
                    lastWasHal = true;
                } else {
                    ic.commitText(String.valueOf(c), 1);
                    lastWasHal = false;
                }
            }
        }
        
        lastTypedChar = c;
        lastTypedTime = now;
        autoResetShift();
    }
    
    private void resetSinglishState() {
        lastTypedChar = 0;
        lastTypedTime = 0;
        lastWasHal = false;
    }
    
    private void commitDirect(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(text, 1);
            resetSinglishState();
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