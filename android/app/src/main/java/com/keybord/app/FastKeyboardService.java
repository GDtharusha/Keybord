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
import java.util.LinkedHashMap;
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
    // SINHALA LABELS (Keys මත පෙන්වන සිංහල)
    // ═══════════════════════════════════════════════════════════════════
    private static final Map<String, String> SINHALA_LABELS = new LinkedHashMap<>();
    private static final Map<String, String> SINHALA_LABELS_SHIFT = new LinkedHashMap<>();
    static {
        // Normal (lowercase)
        SINHALA_LABELS.put("q", "ෘ"); SINHALA_LABELS.put("w", "ව"); SINHALA_LABELS.put("e", "එ");
        SINHALA_LABELS.put("r", "ර"); SINHALA_LABELS.put("t", "ට"); SINHALA_LABELS.put("y", "ය");
        SINHALA_LABELS.put("u", "උ"); SINHALA_LABELS.put("i", "ඉ"); SINHALA_LABELS.put("o", "ඔ");
        SINHALA_LABELS.put("p", "ප"); SINHALA_LABELS.put("a", "අ"); SINHALA_LABELS.put("s", "ස");
        SINHALA_LABELS.put("d", "ඩ"); SINHALA_LABELS.put("f", "ෆ"); SINHALA_LABELS.put("g", "ග");
        SINHALA_LABELS.put("h", "හ"); SINHALA_LABELS.put("j", "ජ"); SINHALA_LABELS.put("k", "ක");
        SINHALA_LABELS.put("l", "ල"); SINHALA_LABELS.put("z", "ඤ"); SINHALA_LABELS.put("x", "ං");
        SINHALA_LABELS.put("c", "ච"); SINHALA_LABELS.put("v", "ව"); SINHALA_LABELS.put("b", "බ");
        SINHALA_LABELS.put("n", "න"); SINHALA_LABELS.put("m", "ම");
        
        // Shifted (uppercase) - different characters
        SINHALA_LABELS_SHIFT.put("q", "ඍ"); SINHALA_LABELS_SHIFT.put("w", "ව"); SINHALA_LABELS_SHIFT.put("e", "ඒ");
        SINHALA_LABELS_SHIFT.put("r", "ර"); SINHALA_LABELS_SHIFT.put("t", "ට"); SINHALA_LABELS_SHIFT.put("y", "ය");
        SINHALA_LABELS_SHIFT.put("u", "ඌ"); SINHALA_LABELS_SHIFT.put("i", "ඊ"); SINHALA_LABELS_SHIFT.put("o", "ඕ");
        SINHALA_LABELS_SHIFT.put("p", "ඵ"); SINHALA_LABELS_SHIFT.put("a", "ඇ"); SINHALA_LABELS_SHIFT.put("s", "ෂ");
        SINHALA_LABELS_SHIFT.put("d", "ඩ"); SINHALA_LABELS_SHIFT.put("f", "ෆ"); SINHALA_LABELS_SHIFT.put("g", "ඝ");
        SINHALA_LABELS_SHIFT.put("h", "හ"); SINHALA_LABELS_SHIFT.put("j", "ඣ"); SINHALA_LABELS_SHIFT.put("k", "ඛ");
        SINHALA_LABELS_SHIFT.put("l", "ළ"); SINHALA_LABELS_SHIFT.put("z", "ඤ"); SINHALA_LABELS_SHIFT.put("x", "ං");
        SINHALA_LABELS_SHIFT.put("c", "ඡ"); SINHALA_LABELS_SHIFT.put("v", "ව"); SINHALA_LABELS_SHIFT.put("b", "භ");
        SINHALA_LABELS_SHIFT.put("n", "ණ"); SINHALA_LABELS_SHIFT.put("m", "ම");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SINGLISH CONSONANTS (හල් අකුරු - ්  සමග)
    // Priority: 3-letter → 2-letter → 1-letter
    // ═══════════════════════════════════════════════════════════════════
    private static final Map<String, String> CONSONANTS = new LinkedHashMap<>();
    static {
        // 3-letter combinations (MUST check first)
        CONSONANTS.put("ndh", "ඳ");
        CONSONANTS.put("nDh", "ඳ");
        
        // 2-letter combinations
        CONSONANTS.put("kh", "ඛ"); CONSONANTS.put("Kh", "ඛ");
        CONSONANTS.put("gh", "ඝ"); CONSONANTS.put("Gh", "ඝ");
        CONSONANTS.put("ng", "ඟ"); CONSONANTS.put("Ng", "ඟ");
        CONSONANTS.put("ch", "ච"); CONSONANTS.put("Ch", "ඡ");
        CONSONANTS.put("jh", "ඣ"); CONSONANTS.put("Jh", "ඣ");
        CONSONANTS.put("ny", "ඤ"); CONSONANTS.put("Ny", "ඤ");
        CONSONANTS.put("kn", "ඤ"); CONSONANTS.put("Kn", "ඤ");
        CONSONANTS.put("gn", "ඥ"); CONSONANTS.put("Gn", "ඥ");
        CONSONANTS.put("th", "ත"); CONSONANTS.put("Th", "ථ");
        CONSONANTS.put("dh", "ද"); CONSONANTS.put("Dh", "ධ");
        CONSONANTS.put("nd", "ඳ"); CONSONANTS.put("Nd", "ඳ");
        CONSONANTS.put("ph", "ඵ"); CONSONANTS.put("Ph", "ඵ");
        CONSONANTS.put("bh", "භ"); CONSONANTS.put("Bh", "භ");
        CONSONANTS.put("mb", "ඹ"); CONSONANTS.put("Mb", "ඹ");
        CONSONANTS.put("sh", "ශ"); CONSONANTS.put("Sh", "ෂ");
        
        // 1-letter consonants
        CONSONANTS.put("k", "ක"); CONSONANTS.put("K", "ඛ");
        CONSONANTS.put("g", "ග"); CONSONANTS.put("G", "ඝ");
        CONSONANTS.put("c", "ච"); CONSONANTS.put("C", "ඡ");
        CONSONANTS.put("j", "ජ"); CONSONANTS.put("J", "ඣ");
        CONSONANTS.put("t", "ට"); CONSONANTS.put("T", "ට");
        CONSONANTS.put("d", "ඩ"); CONSONANTS.put("D", "ඩ");
        CONSONANTS.put("n", "න"); CONSONANTS.put("N", "ණ");
        CONSONANTS.put("p", "ප"); CONSONANTS.put("P", "ඵ");
        CONSONANTS.put("b", "බ"); CONSONANTS.put("B", "භ");
        CONSONANTS.put("m", "ම");
        CONSONANTS.put("y", "ය"); CONSONANTS.put("Y", "ය");
        CONSONANTS.put("r", "ර");
        CONSONANTS.put("l", "ල"); CONSONANTS.put("L", "ළ");
        CONSONANTS.put("w", "ව"); CONSONANTS.put("v", "ව");
        CONSONANTS.put("s", "ස"); CONSONANTS.put("S", "ෂ");
        CONSONANTS.put("h", "හ");
        CONSONANTS.put("f", "ෆ");
        CONSONANTS.put("x", "ං");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // STANDALONE VOWELS (වචන මුලට - ස්වර අකුරු)
    // ═══════════════════════════════════════════════════════════════════
    private static final Map<String, String> VOWELS = new LinkedHashMap<>();
    static {
        // 2-letter vowels (check first)
        VOWELS.put("aa", "ආ"); VOWELS.put("AA", "ඈ");
        VOWELS.put("ii", "ඊ"); VOWELS.put("ee", "ඊ"); VOWELS.put("II", "ඊ");
        VOWELS.put("uu", "ඌ"); VOWELS.put("UU", "ඌ");
        VOWELS.put("oo", "ඕ"); VOWELS.put("OO", "ඕ");
        VOWELS.put("ai", "ඓ"); VOWELS.put("Ai", "ඓ");
        VOWELS.put("au", "ඖ"); VOWELS.put("Au", "ඖ");
        VOWELS.put("Ru", "ඍ");
        
        // 1-letter vowels
        VOWELS.put("a", "අ"); VOWELS.put("A", "ඇ");
        VOWELS.put("i", "ඉ"); VOWELS.put("I", "ඊ");
        VOWELS.put("u", "උ"); VOWELS.put("U", "ඌ");
        VOWELS.put("e", "එ"); VOWELS.put("E", "ඒ");
        VOWELS.put("o", "ඔ"); VOWELS.put("O", "ඕ");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // VOWEL MODIFIERS (පිල්ලම් - ව්‍යංජනයට පසුව)
    // ═══════════════════════════════════════════════════════════════════
    private static final Map<String, String> MODIFIERS = new LinkedHashMap<>();
    static {
        // 2-letter modifiers (check first)
        MODIFIERS.put("aa", "ා"); MODIFIERS.put("AA", "ෑ");
        MODIFIERS.put("ii", "ී"); MODIFIERS.put("ee", "ී"); MODIFIERS.put("II", "ී");
        MODIFIERS.put("uu", "ූ"); MODIFIERS.put("UU", "ූ");
        MODIFIERS.put("oo", "ෝ"); MODIFIERS.put("OO", "ෝ");
        MODIFIERS.put("ai", "ෛ"); MODIFIERS.put("Ai", "ෛ");
        MODIFIERS.put("au", "ෞ"); MODIFIERS.put("Au", "ෞ");
        MODIFIERS.put("Ru", "ෘ");
        
        // 1-letter modifiers
        MODIFIERS.put("a", ""); // හල් කිරීම ඉවත් කරන්න පමණයි
        MODIFIERS.put("A", "ැ");
        MODIFIERS.put("i", "ි"); MODIFIERS.put("I", "ී");
        MODIFIERS.put("u", "ු"); MODIFIERS.put("U", "ූ");
        MODIFIERS.put("e", "ෙ"); MODIFIERS.put("E", "ේ");
        MODIFIERS.put("o", "ො"); MODIFIERS.put("O", "ෝ");
    }
    
    // Characters that can extend to longer sequences
    private static final String EXTENDABLE = "aeiouAEIOUR";
    private static final String CONSONANT_EXTENDABLE = "tkdgcjnpbsmlhKTDGCJNPBS";
    
    // ═══════════════════════════════════════════════════════════════════
    // VIEWS AND STATE
    // ═══════════════════════════════════════════════════════════════════
    private FrameLayout rootContainer;
    private LinearLayout keyboardView;
    private LinearLayout emojiRowView;
    private Handler handler;
    private Vibrator vibrator;
    private KeyboardSettings settings;
    
    private PopupWindow keyPreviewPopup;
    private TextView keyPreviewText;
    
    private boolean isShift = false;
    private boolean isCaps = false;
    private boolean isNumbers = false;
    private boolean isSymbols = false;
    private boolean isRepeating = false;
    private Runnable repeatRunnable;
    private boolean isSinhalaMode = false;
    
    // Singlish buffer
    private StringBuilder buffer = new StringBuilder();
    private boolean lastWasConsonant = false;
    private String lastConsonant = "";
    
    private int navigationBarHeight = 0;
    
    // Key tracking for touch layer
    private List<KeyInfo> keyInfoList = new ArrayList<>();
    private KeyInfo currentPressedKey = null;
    
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
    
    // ═══════════════════════════════════════════════════════════════════
    // BROADCAST RECEIVER
    // ═══════════════════════════════════════════════════════════════════
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            if (KeyboardSettings.ACTION_SETTINGS_CHANGED.equals(intent.getAction())) {
                handler.post(() -> { loadSettings(); rebuildKeyboard(); });
            } else if (KeyboardSettings.ACTION_TYPE_TEXT.equals(intent.getAction())) {
                String text = intent.getStringExtra("text");
                if (text != null) handler.post(() -> commitText(text));
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
        setupKeyPreview();
        
        try { vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE); } catch (Exception e) {}
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(KeyboardSettings.ACTION_SETTINGS_CHANGED);
        filter.addAction(KeyboardSettings.ACTION_TYPE_TEXT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }
    }
    
    @Override
    public void onDestroy() {
        stopRepeat();
        hideKeyPreview();
        try { unregisterReceiver(receiver); } catch (Exception e) {}
        super.onDestroy();
    }
    
    private void setupKeyPreview() {
        keyPreviewText = new TextView(this);
        keyPreviewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
        keyPreviewText.setTextColor(Color.WHITE);
        keyPreviewText.setTypeface(Typeface.DEFAULT_BOLD);
        keyPreviewText.setGravity(Gravity.CENTER);
        keyPreviewText.setPadding(dp(28), dp(18), dp(28), dp(18));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#505050"));
        bg.setCornerRadius(dp(16));
        keyPreviewText.setBackground(bg);
        
        keyPreviewPopup = new PopupWindow(keyPreviewText, 
            ViewGroup.LayoutParams.WRAP_CONTENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT);
        keyPreviewPopup.setClippingEnabled(false);
    }
    
    private void calculateNavBarHeight() {
        try {
            int id = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (id > 0) navigationBarHeight = getResources().getDimensionPixelSize(id);
            if (navigationBarHeight == 0) navigationBarHeight = dp(48);
        } catch (Exception e) { navigationBarHeight = dp(48); }
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
    // INPUT VIEW
    // ═══════════════════════════════════════════════════════════════════
    @Override
    public View onCreateInputView() {
        loadSettings();
        calculateNavBarHeight();
        
        rootContainer = new FrameLayout(this);
        rootContainer.setBackgroundColor(parseColor(colorBackground));
        
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(parseColor(colorBackground));
        
        if (showEmojiRow) {
            emojiRowView = createEmojiRow();
            main.addView(emojiRowView);
        }
        
        keyboardView = createKeyboard();
        main.addView(keyboardView);
        
        FrameLayout.LayoutParams mainParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        mainParams.gravity = Gravity.BOTTOM;
        rootContainer.addView(main, mainParams);
        
        // Touch layer on top
        View touchLayer = new View(this);
        touchLayer.setBackgroundColor(Color.TRANSPARENT);
        touchLayer.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        touchLayer.setOnTouchListener(this::handleTouch);
        rootContainer.addView(touchLayer);
        
        int emojiH = showEmojiRow ? dp(44) : 0;
        int totalH = emojiH + dp(keyboardHeight) + navigationBarHeight;
        rootContainer.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, totalH));
        rootContainer.setPadding(0, 0, 0, navigationBarHeight);
        
        rootContainer.post(this::updateKeyBounds);
        return rootContainer;
    }
    
    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        isShift = false;
        isCaps = false;
        isSymbols = false;
        buffer.setLength(0);
        lastWasConsonant = false;
        lastConsonant = "";
        
        if (info != null) {
            int cls = info.inputType & EditorInfo.TYPE_MASK_CLASS;
            isNumbers = (cls == EditorInfo.TYPE_CLASS_NUMBER || cls == EditorInfo.TYPE_CLASS_PHONE);
        }
        loadSettings();
        rebuildKeyboard();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // TOUCH HANDLING - User Experience Enhancement
    // ═══════════════════════════════════════════════════════════════════
    private void updateKeyBounds() {
        for (KeyInfo ki : keyInfoList) ki.updateBounds();
    }
    
    private boolean handleTouch(View v, MotionEvent ev) {
        float x = ev.getRawX(), y = ev.getRawY();
        
        // Find key at touch point or nearest key
        KeyInfo target = null;
        float minDist = Float.MAX_VALUE;
        
        for (KeyInfo ki : keyInfoList) {
            if (ki.bounds.contains((int)x, (int)y)) {
                target = ki;
                break;
            }
            float cx = ki.bounds.centerX(), cy = ki.bounds.centerY();
            float dist = (float)Math.sqrt((x-cx)*(x-cx) + (y-cy)*(y-cy));
            if (dist < minDist) { minDist = dist; target = ki; }
        }
        
        // Allow nearby touch (within 45dp)
        if (target == null || (minDist > dp(45) && !target.bounds.contains((int)x,(int)y))) {
            if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) {
                resetAllKeys();
            }
            return true;
        }
        
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPressedKey = target;
                animateKeyDown(target);
                vibrate();
                showKeyPreview(target);
                processKey(target.key);
                if (target.key.equals("⌫") || target.key.equals("SPACE")) startRepeat(target.key);
                break;
                
            case MotionEvent.ACTION_MOVE:
                // Allow sliding to nearby keys
                if (currentPressedKey != null && target != currentPressedKey) {
                    resetAllKeys();
                    currentPressedKey = target;
                    animateKeyDown(target);
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                resetAllKeys();
                hideKeyPreview();
                stopRepeat();
                currentPressedKey = null;
                break;
        }
        return true;
    }
    
    private void animateKeyDown(KeyInfo ki) {
        ki.view.setAlpha(0.6f);
        ki.view.setScaleX(0.92f);
        ki.view.setScaleY(0.92f);
    }
    
    private void resetAllKeys() {
        for (KeyInfo ki : keyInfoList) {
            ki.view.setAlpha(1f);
            ki.view.setScaleX(1f);
            ki.view.setScaleY(1f);
        }
    }
    
    private void showKeyPreview(KeyInfo ki) {
        if (isSpecialKey(ki.key)) return;
        
        String display = getDisplayText(ki.key);
        if (display.equals("GD Keyboard") || display.isEmpty()) return;
        
        keyPreviewText.setText(display);
        keyPreviewText.measure(0, 0);
        
        int[] loc = new int[2];
        ki.view.getLocationOnScreen(loc);
        int pw = Math.max(keyPreviewText.getMeasuredWidth(), dp(60));
        int ph = keyPreviewText.getMeasuredHeight();
        int px = loc[0] + ki.view.getWidth()/2 - pw/2;
        int py = loc[1] - ph - dp(15);
        if (py < 0) py = dp(5);
        
        if (keyPreviewPopup.isShowing()) {
            keyPreviewPopup.update(px, py, pw, ph);
        } else {
            keyPreviewPopup.setWidth(pw);
            keyPreviewPopup.setHeight(ph);
            keyPreviewPopup.showAtLocation(ki.view, Gravity.NO_GRAVITY, px, py);
        }
    }
    
    private void hideKeyPreview() {
        try { if (keyPreviewPopup.isShowing()) keyPreviewPopup.dismiss(); } catch (Exception e) {}
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
        row.setElevation(dp(10));
        row.setClickable(true);
        
        String emojis = settings.getQuickEmojis();
        for (String emoji : emojis.split(",")) {
            final String e = emoji.trim();
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
            
            tv.setOnClickListener(view -> { vibrate(); commitText(e); });
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
        int pad = dp(2);
        row.setPadding(rowIdx == 1 ? dp(14) : 0, pad, rowIdx == 1 ? dp(14) : 0, pad);
        
        for (String key : keys) row.addView(createKey(key));
        return row;
    }
    
    private View createKey(String key) {
        FrameLayout container = new FrameLayout(this);
        TextView tv = new TextView(this);
        
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        
        String display = getDisplayText(key);
        int textColor = parseColor(colorText);
        float textSize = isSpecialKey(key) ? 14 : keyTextSize;
        
        if (key.equals("↵")) { display = "↵"; textColor = Color.WHITE; textSize = 22; }
        else if (key.equals("⇧")) {
            textSize = 26;
            if (isCaps) { display = "⇪"; textColor = Color.parseColor("#10b981"); }
            else if (isShift) { display = "⬆"; textColor = Color.parseColor("#3b82f6"); }
            else { display = "⇧"; }
        }
        else if (key.equals("⌫")) { display = "⌫"; textSize = 24; }
        else if (key.equals("SPACE")) { display = "GD Keyboard"; textSize = 10; textColor = Color.parseColor("#666666"); }
        else if (key.equals("🌐")) {
            display = isSinhalaMode ? "SI" : "EN";
            textSize = 14;
            textColor = isSinhalaMode ? Color.parseColor("#10b981") : Color.parseColor("#3b82f6");
        }
        else if (key.equals("✨")) { display = "✨"; textSize = 20; }
        
        tv.setText(display);
        tv.setTextColor(textColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        
        // Sinhala label on key
        if (isSinhalaMode && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            Map<String, String> labels = (isShift || isCaps) ? SINHALA_LABELS_SHIFT : SINHALA_LABELS;
            String sinhala = labels.get(key.toLowerCase());
            if (sinhala != null) {
                TextView lbl = new TextView(this);
                lbl.setText(sinhala);
                lbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                lbl.setTextColor(Color.parseColor("#888888"));
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2);
                lp.gravity = Gravity.TOP | Gravity.END;
                lp.setMargins(0, dp(2), dp(4), 0);
                container.addView(lbl, lp);
            }
        }
        
        float weight = getKeyWeight(key);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -1, weight);
        cp.setMargins(dp(keyGap), dp(keyGap), dp(keyGap), dp(keyGap));
        container.setLayoutParams(cp);
        
        container.addView(tv, new FrameLayout.LayoutParams(-1, -1));
        container.setBackground(createKeyBg(key));
        
        keyInfoList.add(new KeyInfo(key, container));
        return container;
    }
    
    private String getDisplayText(String key) {
        if (key.equals("SPACE")) return "GD Keyboard";
        if (key.length() == 1 && Character.isLetter(key.charAt(0))) {
            return (isShift || isCaps) ? key.toUpperCase() : key.toLowerCase();
        }
        return key;
    }
    
    private boolean isSpecialKey(String key) {
        return "⇧⌫↵SPACE123ABC#+=🌐✨".contains(key) || key.equals("SPACE");
    }
    
    private float getKeyWeight(String key) {
        if (key.equals("SPACE")) return 3.5f;
        if (key.equals("⇧") || key.equals("⌫")) return 1.5f;
        if (key.equals("↵") || key.equals("123") || key.equals("ABC") || key.equals("#+=")) return 1.3f;
        if (key.equals("🌐") || key.equals("✨")) return 1.0f;
        return 1f;
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
        else if (key.equals("⌫") || key.equals("123") || key.equals("ABC") || key.equals("#+="))
            color = colorKeySpecial;
        else if (key.equals("SPACE")) color = colorKeySpace;
        else if (key.equals("🌐") || key.equals("✨")) color = colorKeySpecial;
        
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
            case "↵": flushBuffer(ic); handleEnter(ic); break;
            case "SPACE": flushBuffer(ic); ic.commitText(" ", 1); lastWasConsonant = false; autoUnshift(); break;
            case "123": flushBuffer(ic); isNumbers = true; isSymbols = false; rebuildKeyboard(); break;
            case "ABC": flushBuffer(ic); isNumbers = false; isSymbols = false; rebuildKeyboard(); break;
            case "#+=": flushBuffer(ic); isSymbols = true; isNumbers = false; rebuildKeyboard(); break;
            case "🌐": flushBuffer(ic); isSinhalaMode = !isSinhalaMode; buffer.setLength(0); lastWasConsonant = false; rebuildKeyboard(); break;
            case "✨": flushBuffer(ic); openPopup(); break;
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
        if (buffer.length() > 0) {
            buffer.deleteCharAt(buffer.length() - 1);
            if (buffer.length() == 0) { lastWasConsonant = false; lastConsonant = ""; }
            return;
        }
        ic.deleteSurroundingText(1, 0);
    }
    
    private void handleEnter(InputConnection ic) {
        EditorInfo ei = getCurrentInputEditorInfo();
        if (ei != null) {
            int action = ei.imeOptions & EditorInfo.IME_MASK_ACTION;
            if (action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED)
                ic.commitText("\n", 1);
            else ic.performEditorAction(action);
        } else ic.commitText("\n", 1);
    }
    
    private void handleChar(InputConnection ic, String key) {
        String ch = key;
        if ((isShift || isCaps) && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            ch = key.toUpperCase();
        }
        
        if (isSinhalaMode && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            processSinglish(ic, ch);
        } else {
            ic.commitText(ch, 1);
            autoUnshift();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SINGLISH TO SINHALA ENGINE
    // ═══════════════════════════════════════════════════════════════════
    private void processSinglish(InputConnection ic, String input) {
        buffer.append(input);
        tryConvert(ic);
        autoUnshift();
    }
    
    private void tryConvert(InputConnection ic) {
        while (buffer.length() > 0) {
            String b = buffer.toString();
            boolean matched = false;
            
            // Try longest match first (3, 2, 1)
            for (int len = Math.min(3, b.length()); len >= 1; len--) {
                String sub = b.substring(0, len);
                boolean canExtend = (len < b.length()) ? false : couldExtend(sub);
                
                // If last output was consonant, try vowel modifiers
                if (lastWasConsonant && MODIFIERS.containsKey(sub)) {
                    if (!canExtend || b.length() > len) {
                        String mod = MODIFIERS.get(sub);
                        // Remove hal kirima (්) and add modifier
                        ic.deleteSurroundingText(1, 0);
                        if (!mod.isEmpty()) ic.commitText(mod, 1);
                        buffer.delete(0, len);
                        lastWasConsonant = false;
                        lastConsonant = "";
                        matched = true;
                        break;
                    }
                }
                
                // Try consonants
                if (CONSONANTS.containsKey(sub)) {
                    if (!canExtend || b.length() > len || !CONSONANT_EXTENDABLE.contains(sub)) {
                        String consonant = CONSONANTS.get(sub);
                        ic.commitText(consonant + "්", 1); // Output with hal kirima
                        buffer.delete(0, len);
                        lastWasConsonant = true;
                        lastConsonant = consonant;
                        matched = true;
                        break;
                    }
                }
                
                // Try standalone vowels (only if not after consonant)
                if (!lastWasConsonant && VOWELS.containsKey(sub)) {
                    if (!canExtend || b.length() > len) {
                        ic.commitText(VOWELS.get(sub), 1);
                        buffer.delete(0, len);
                        lastWasConsonant = false;
                        matched = true;
                        break;
                    }
                }
            }
            
            if (!matched) {
                // Need more input or unknown char
                if (b.length() >= 3) {
                    // Force output first char
                    ic.commitText(String.valueOf(b.charAt(0)), 1);
                    buffer.deleteCharAt(0);
                    lastWasConsonant = false;
                } else {
                    break; // Wait for more input
                }
            }
        }
    }
    
    private boolean couldExtend(String s) {
        if (s.isEmpty()) return false;
        char last = s.charAt(s.length() - 1);
        return EXTENDABLE.indexOf(last) >= 0 || CONSONANT_EXTENDABLE.indexOf(last) >= 0;
    }
    
    private void flushBuffer(InputConnection ic) {
        while (buffer.length() > 0) {
            String b = buffer.toString();
            boolean found = false;
            
            for (int len = Math.min(3, b.length()); len >= 1; len--) {
                String sub = b.substring(0, len);
                
                if (lastWasConsonant && MODIFIERS.containsKey(sub)) {
                    String mod = MODIFIERS.get(sub);
                    ic.deleteSurroundingText(1, 0);
                    if (!mod.isEmpty()) ic.commitText(mod, 1);
                    buffer.delete(0, len);
                    lastWasConsonant = false;
                    found = true;
                    break;
                }
                if (CONSONANTS.containsKey(sub)) {
                    ic.commitText(CONSONANTS.get(sub) + "්", 1);
                    buffer.delete(0, len);
                    lastWasConsonant = true;
                    found = true;
                    break;
                }
                if (!lastWasConsonant && VOWELS.containsKey(sub)) {
                    ic.commitText(VOWELS.get(sub), 1);
                    buffer.delete(0, len);
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                ic.commitText(String.valueOf(b.charAt(0)), 1);
                buffer.deleteCharAt(0);
                lastWasConsonant = false;
            }
        }
        lastConsonant = "";
    }
    
    private void commitText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(text, 1);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════
    private void autoUnshift() {
        if (isShift && !isCaps) { isShift = false; rebuildKeyboard(); }
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
        if (repeatRunnable != null) handler.removeCallbacks(repeatRunnable);
    }
    
    private void vibrate() {
        if (!vibrateEnabled || vibrator == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                vibrator.vibrate(VibrationEffect.createOneShot(vibrateDuration, VibrationEffect.DEFAULT_AMPLITUDE));
            else vibrator.vibrate(vibrateDuration);
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
        keyInfoList.clear();
        rootContainer.removeAllViews();
        
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(parseColor(colorBackground));
        
        if (showEmojiRow) { emojiRowView = createEmojiRow(); main.addView(emojiRowView); }
        keyboardView = createKeyboard();
        main.addView(keyboardView);
        
        FrameLayout.LayoutParams mp = new FrameLayout.LayoutParams(-1, -2);
        mp.gravity = Gravity.BOTTOM;
        rootContainer.addView(main, mp);
        
        View touch = new View(this);
        touch.setBackgroundColor(Color.TRANSPARENT);
        touch.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        touch.setOnTouchListener(this::handleTouch);
        rootContainer.addView(touch);
        
        int eh = showEmojiRow ? dp(44) : 0;
        int th = eh + dp(keyboardHeight) + navigationBarHeight;
        ViewGroup.LayoutParams rp = rootContainer.getLayoutParams();
        if (rp != null) { rp.height = th; rootContainer.setLayoutParams(rp); }
        rootContainer.setPadding(0, 0, 0, navigationBarHeight);
        rootContainer.setBackgroundColor(parseColor(colorBackground));
        
        rootContainer.post(this::updateKeyBounds);
    }
    
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private int parseColor(String c) { try { return Color.parseColor(c); } catch (Exception e) { return Color.BLACK; } }
}