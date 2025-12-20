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
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
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
    // SINHALA LABELS (Keys මත පෙන්වන සිංහල)
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
        SINHALA_LABELS_SHIFT.put("d", "ධ"); SINHALA_LABELS_SHIFT.put("f", "ෆ"); SINHALA_LABELS_SHIFT.put("g", "ඝ");
        SINHALA_LABELS_SHIFT.put("h", "ඃ"); SINHALA_LABELS_SHIFT.put("j", "ඣ"); SINHALA_LABELS_SHIFT.put("k", "ඛ");
        SINHALA_LABELS_SHIFT.put("l", "ළ"); SINHALA_LABELS_SHIFT.put("z", "ඥ"); SINHALA_LABELS_SHIFT.put("x", "ඃ");
        SINHALA_LABELS_SHIFT.put("c", "ඡ"); SINHALA_LABELS_SHIFT.put("v", "ව"); SINHALA_LABELS_SHIFT.put("b", "භ");
        SINHALA_LABELS_SHIFT.put("n", "ණ"); SINHALA_LABELS_SHIFT.put("m", "ඹ");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // OPTIMIZED SINGLISH ENGINE - TRIE BASED FOR O(1) LOOKUP
    // ═══════════════════════════════════════════════════════════════════
    
    // Trie Node for ultra-fast pattern matching
    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        String consonant = null;      // Sinhala consonant at this node
        String vowel = null;          // Standalone vowel at this node
        String modifier = null;       // Vowel modifier (pilla) at this node
        boolean canExtend = false;    // Can this pattern be extended?
    }
    
    private static final TrieNode consonantTrie = new TrieNode();
    private static final TrieNode vowelTrie = new TrieNode();
    private static final TrieNode modifierTrie = new TrieNode();
    
    // Build tries at class load time for maximum performance
    static {
        // ═══════════════════════════════════════════════════════════════
        // CONSONANTS (ව්‍යංජන) - Longest match first priority built into trie
        // ═══════════════════════════════════════════════════════════════
        String[][] consonants = {
            // 4-letter
            {"nndh", "ඳ"},
            // 3-letter
            {"ndh", "ඳ"}, {"nDh", "ඳ"}, {"thh", "ථ"}, {"dhh", "ධ"},
            {"chh", "ඡ"}, {"shh", "ෂ"},
            // 2-letter  
            {"kh", "ඛ"}, {"Kh", "ඛ"}, {"gh", "ඝ"}, {"Gh", "ඝ"},
            {"ng", "ඟ"}, {"Ng", "ඟ"}, {"ch", "ච"}, {"Ch", "ඡ"},
            {"jh", "ඣ"}, {"Jh", "ඣ"}, {"JH", "ඣ"},
            {"ny", "ඤ"}, {"Ny", "ඤ"}, {"jn", "ඥ"}, {"Jn", "ඥ"},
            {"tt", "ට"}, {"TT", "ට"}, {"Tt", "ට"},
            {"dd", "ඩ"}, {"DD", "ඩ"}, {"Dd", "ඩ"},
            {"NN", "ණ"}, {"Nn", "ණ"},
            {"th", "ත"}, {"Th", "ථ"}, {"TH", "ථ"},
            {"dh", "ද"}, {"Dh", "ධ"}, {"DH", "ධ"},
            {"nd", "ඳ"}, {"Nd", "ඳ"},
            {"ph", "ඵ"}, {"Ph", "ඵ"}, {"PH", "ඵ"},
            {"bh", "භ"}, {"Bh", "භ"}, {"BH", "භ"},
            {"mb", "ඹ"}, {"Mb", "ඹ"},
            {"sh", "ශ"}, {"Sh", "ෂ"}, {"SH", "ෂ"},
            {"ll", "ළ"}, {"LL", "ළ"}, {"Ll", "ළ"},
            {"rr", "ඍ"},
            // 1-letter
            {"k", "ක"}, {"K", "ඛ"},
            {"g", "ග"}, {"G", "ඝ"},
            {"c", "ච"}, {"C", "ඡ"},
            {"j", "ජ"}, {"J", "ඣ"},
            {"t", "ට"}, {"T", "ඨ"},
            {"d", "ඩ"}, {"D", "ඪ"},
            {"n", "න"}, {"N", "ණ"},
            {"p", "ප"}, {"P", "ඵ"},
            {"b", "බ"}, {"B", "භ"},
            {"m", "ම"}, {"M", "ඹ"},
            {"y", "ය"}, {"Y", "ය"},
            {"r", "ර"}, {"R", "ර"},
            {"l", "ල"}, {"L", "ළ"},
            {"w", "ව"}, {"W", "ව"},
            {"v", "ව"}, {"V", "ව"},
            {"s", "ස"}, {"S", "ෂ"},
            {"h", "හ"}, {"H", "හ"},
            {"f", "ෆ"}, {"F", "ෆ"},
            {"z", "ඤ"}, {"Z", "ඥ"},
            {"x", "ං"}, {"X", "ඃ"},
        };
        
        for (String[] pair : consonants) {
            insertConsonant(pair[0], pair[1]);
        }
        
        // ═══════════════════════════════════════════════════════════════
        // STANDALONE VOWELS (ස්වර)
        // ═══════════════════════════════════════════════════════════════
        String[][] vowels = {
            // 3-letter
            {"aae", "ඈ"}, {"Aae", "ඈ"}, {"AAe", "ඈ"},
            // 2-letter
            {"aa", "ආ"}, {"Aa", "ඈ"}, {"AA", "ඈ"},
            {"ae", "ඇ"}, {"Ae", "ඈ"},
            {"ii", "ඊ"}, {"II", "ඊ"}, {"Ii", "ඊ"},
            {"ee", "ඒ"}, {"EE", "ඒ"}, {"Ee", "ඒ"},
            {"uu", "ඌ"}, {"UU", "ඌ"}, {"Uu", "ඌ"},
            {"oo", "ඕ"}, {"OO", "ඕ"}, {"Oo", "ඕ"},
            {"ai", "ඓ"}, {"Ai", "ඓ"}, {"AI", "ඓ"},
            {"au", "ඖ"}, {"Au", "ඖ"}, {"AU", "ඖ"},
            {"Ru", "ඍ"}, {"RU", "ඎ"},
            {"Lu", "ඏ"}, {"LU", "ඐ"},
            // 1-letter
            {"a", "අ"}, {"A", "ඇ"},
            {"i", "ඉ"}, {"I", "ඊ"},
            {"u", "උ"}, {"U", "ඌ"},
            {"e", "එ"}, {"E", "ඒ"},
            {"o", "ඔ"}, {"O", "ඕ"},
        };
        
        for (String[] pair : vowels) {
            insertVowel(pair[0], pair[1]);
        }
        
        // ═══════════════════════════════════════════════════════════════
        // VOWEL MODIFIERS (පිල්ලම්)
        // ═══════════════════════════════════════════════════════════════
        String[][] modifiers = {
            // 3-letter
            {"aae", "ෑ"}, {"Aae", "ෑ"}, {"AAe", "ෑ"},
            // 2-letter
            {"aa", "ා"}, {"Aa", "ෑ"}, {"AA", "ෑ"},
            {"ae", "ැ"}, {"Ae", "ෑ"}, {"AE", "ෑ"},
            {"ii", "ී"}, {"II", "ී"}, {"Ii", "ී"},
            {"ee", "ේ"}, {"EE", "ේ"}, {"Ee", "ේ"},
            {"uu", "ූ"}, {"UU", "ූ"}, {"Uu", "ූ"},
            {"oo", "ෝ"}, {"OO", "ෝ"}, {"Oo", "ෝ"},
            {"ai", "ෛ"}, {"Ai", "ෛ"}, {"AI", "ෛ"},
            {"au", "ෞ"}, {"Au", "ෞ"}, {"AU", "ෞ"},
            {"Ru", "ෘ"}, {"RU", "ෲ"},
            {"Lu", "ෟ"},
            // 1-letter
            {"a", ""},   // Just removes hal
            {"A", "ැ"},
            {"i", "ි"}, {"I", "ී"},
            {"u", "ු"}, {"U", "ූ"},
            {"e", "ෙ"}, {"E", "ේ"},
            {"o", "ො"}, {"O", "ෝ"},
        };
        
        for (String[] pair : modifiers) {
            insertModifier(pair[0], pair[1]);
        }
    }
    
    private static void insertConsonant(String pattern, String value) {
        TrieNode node = consonantTrie;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (!node.children.containsKey(c)) {
                node.children.put(c, new TrieNode());
            }
            // Mark intermediate nodes as extendable
            if (i < pattern.length() - 1) {
                node.canExtend = true;
            }
            node = node.children.get(c);
        }
        node.consonant = value;
        // Check if this pattern could be prefix of longer pattern
        node.canExtend = !node.children.isEmpty();
    }
    
    private static void insertVowel(String pattern, String value) {
        TrieNode node = vowelTrie;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (!node.children.containsKey(c)) {
                node.children.put(c, new TrieNode());
            }
            node = node.children.get(c);
        }
        node.vowel = value;
        node.canExtend = !node.children.isEmpty();
    }
    
    private static void insertModifier(String pattern, String value) {
        TrieNode node = modifierTrie;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (!node.children.containsKey(c)) {
                node.children.put(c, new TrieNode());
            }
            node = node.children.get(c);
        }
        node.modifier = value;
        node.canExtend = !node.children.isEmpty();
    }
    
    // Match result class for clean returns
    private static class MatchResult {
        String value;
        int length;
        boolean canExtend;
        
        MatchResult(String value, int length, boolean canExtend) {
            this.value = value;
            this.length = length;
            this.canExtend = canExtend;
        }
    }
    
    // Ultra-fast trie lookup
    private MatchResult findConsonant(String input, int start) {
        TrieNode node = consonantTrie;
        String lastMatch = null;
        int lastMatchLen = 0;
        boolean canExtend = false;
        
        for (int i = start; i < input.length(); i++) {
            char c = input.charAt(i);
            if (!node.children.containsKey(c)) break;
            node = node.children.get(c);
            if (node.consonant != null) {
                lastMatch = node.consonant;
                lastMatchLen = i - start + 1;
            }
            canExtend = node.canExtend || !node.children.isEmpty();
        }
        
        if (lastMatch != null) {
            return new MatchResult(lastMatch, lastMatchLen, canExtend);
        }
        return null;
    }
    
    private MatchResult findVowel(String input, int start) {
        TrieNode node = vowelTrie;
        String lastMatch = null;
        int lastMatchLen = 0;
        boolean canExtend = false;
        
        for (int i = start; i < input.length(); i++) {
            char c = input.charAt(i);
            if (!node.children.containsKey(c)) break;
            node = node.children.get(c);
            if (node.vowel != null) {
                lastMatch = node.vowel;
                lastMatchLen = i - start + 1;
            }
            canExtend = node.canExtend || !node.children.isEmpty();
        }
        
        if (lastMatch != null) {
            return new MatchResult(lastMatch, lastMatchLen, canExtend);
        }
        return null;
    }
    
    private MatchResult findModifier(String input, int start) {
        TrieNode node = modifierTrie;
        String lastMatch = null;
        int lastMatchLen = 0;
        boolean canExtend = false;
        
        for (int i = start; i < input.length(); i++) {
            char c = input.charAt(i);
            if (!node.children.containsKey(c)) break;
            node = node.children.get(c);
            if (node.modifier != null) {
                lastMatch = node.modifier;
                lastMatchLen = i - start + 1;
            }
            canExtend = node.canExtend || !node.children.isEmpty();
        }
        
        if (lastMatch != null) {
            return new MatchResult(lastMatch, lastMatchLen, canExtend);
        }
        return null;
    }
    
    // Check if more input could create a longer match
    private boolean couldExtendConsonant(String input) {
        TrieNode node = consonantTrie;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (!node.children.containsKey(c)) return false;
            node = node.children.get(c);
        }
        return !node.children.isEmpty();
    }
    
    private boolean couldExtendVowel(String input) {
        TrieNode node = vowelTrie;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (!node.children.containsKey(c)) return false;
            node = node.children.get(c);
        }
        return !node.children.isEmpty();
    }
    
    private boolean couldExtendModifier(String input) {
        TrieNode node = modifierTrie;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (!node.children.containsKey(c)) return false;
            node = node.children.get(c);
        }
        return !node.children.isEmpty();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // VIEWS AND STATE
    // ═══════════════════════════════════════════════════════════════════
    private FrameLayout rootContainer;
    private LinearLayout keyboardView;
    private LinearLayout emojiRowView;
    private Handler handler;
    private Vibrator vibrator;
    private KeyboardSettings settings;
    
    // Key preview popup
    private PopupWindow keyPreviewPopup;
    private TextView keyPreviewText;
    private FrameLayout keyPreviewContainer;
    
    // State flags
    private boolean isShift = false;
    private boolean isCaps = false;
    private boolean isNumbers = false;
    private boolean isSymbols = false;
    private boolean isRepeating = false;
    private Runnable repeatRunnable;
    private boolean isSinhalaMode = false;
    
    // Optimized Singlish state
    private StringBuilder buffer = new StringBuilder(8); // Pre-sized buffer
    private boolean lastWasConsonant = false;
    private String pendingConsonant = ""; // Consonant waiting for vowel
    
    private int navigationBarHeight = 0;
    
    // Key tracking for touch layer
    private List<KeyInfo> keyInfoList = new ArrayList<>(40); // Pre-sized
    private KeyInfo currentPressedKey = null;
    private KeyInfo lastHighlightedKey = null;
    
    private static class KeyInfo {
        String key;
        View view;
        Rect bounds = new Rect();
        int centerX, centerY;
        
        KeyInfo(String key, View view) {
            this.key = key;
            this.view = view;
        }
        
        void updateBounds() {
            int[] loc = new int[2];
            view.getLocationOnScreen(loc);
            bounds.set(loc[0], loc[1], loc[0] + view.getWidth(), loc[1] + view.getHeight());
            centerX = bounds.centerX();
            centerY = bounds.centerY();
        }
        
        // Fast distance calculation (squared, no sqrt needed for comparison)
        int distanceSquared(int x, int y) {
            int dx = x - centerX;
            int dy = y - centerY;
            return dx * dx + dy * dy;
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
                if (text != null) handler.post(() -> commitTextDirect(text));
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
        // Create beautiful preview container
        keyPreviewContainer = new FrameLayout(this);
        
        keyPreviewText = new TextView(this);
        keyPreviewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        keyPreviewText.setTextColor(Color.WHITE);
        keyPreviewText.setTypeface(Typeface.DEFAULT_BOLD);
        keyPreviewText.setGravity(Gravity.CENTER);
        keyPreviewText.setIncludeFontPadding(false);
        
        // Stylish background with shadow effect
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2a2a2a"));
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(2), Color.parseColor("#404040"));
        keyPreviewText.setBackground(bg);
        keyPreviewText.setPadding(dp(22), dp(14), dp(22), dp(16));
        keyPreviewText.setElevation(dp(12));
        
        keyPreviewContainer.addView(keyPreviewText);
        
        keyPreviewPopup = new PopupWindow(keyPreviewContainer,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        keyPreviewPopup.setClippingEnabled(false);
        keyPreviewPopup.setAnimationStyle(0); // No system animation, we do custom
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
        
        // Invisible touch layer on top for gap handling
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
        
        // Update key bounds after layout
        rootContainer.post(this::updateKeyBounds);
        return rootContainer;
    }
    
    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        // Reset state
        isShift = false;
        isCaps = false;
        isSymbols = false;
        clearBuffer();
        
        if (info != null) {
            int cls = info.inputType & EditorInfo.TYPE_MASK_CLASS;
            isNumbers = (cls == EditorInfo.TYPE_CLASS_NUMBER || cls == EditorInfo.TYPE_CLASS_PHONE);
        }
        loadSettings();
        rebuildKeyboard();
    }
    
    @Override
    public void onFinishInputView(boolean finishingInput) {
        hideKeyPreview();
        super.onFinishInputView(finishingInput);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // TOUCH HANDLING - ULTRA SMOOTH
    // ═══════════════════════════════════════════════════════════════════
    private void updateKeyBounds() {
        for (KeyInfo ki : keyInfoList) ki.updateBounds();
    }
    
    private boolean handleTouch(View v, MotionEvent ev) {
        int x = (int) ev.getRawX();
        int y = (int) ev.getRawY();
        
        // Find nearest key (handles gaps automatically)
        KeyInfo target = findNearestKey(x, y);
        
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (target != null) {
                    currentPressedKey = target;
                    highlightKey(target);
                    vibrate();
                    showKeyPreview(target);
                    processKeyDown(target.key);
                }
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (target != null && target != lastHighlightedKey) {
                    // Finger moved to different key
                    if (lastHighlightedKey != null) {
                        unhighlightKey(lastHighlightedKey);
                    }
                    highlightKey(target);
                    showKeyPreview(target);
                    // Don't trigger key yet, wait for UP
                }
                break;
                
            case MotionEvent.ACTION_UP:
                hideKeyPreview();
                stopRepeat();
                
                if (target != null && target == lastHighlightedKey) {
                    // Released on same key as highlighted
                    if (target != currentPressedKey) {
                        // Slid to new key, process it
                        processKeyDown(target.key);
                    }
                }
                
                unhighlightAllKeys();
                currentPressedKey = null;
                lastHighlightedKey = null;
                break;
                
            case MotionEvent.ACTION_CANCEL:
                hideKeyPreview();
                stopRepeat();
                unhighlightAllKeys();
                currentPressedKey = null;
                lastHighlightedKey = null;
                break;
        }
        return true;
    }
    
    private KeyInfo findNearestKey(int x, int y) {
        KeyInfo nearest = null;
        int minDist = Integer.MAX_VALUE;
        
        for (KeyInfo ki : keyInfoList) {
            // First check if point is inside bounds
            if (ki.bounds.contains(x, y)) {
                return ki;
            }
            // Otherwise find nearest by center distance
            int dist = ki.distanceSquared(x, y);
            if (dist < minDist) {
                minDist = dist;
                nearest = ki;
            }
        }
        
        // Only return if reasonably close (within ~50dp)
        int maxDistSquared = dp(50) * dp(50);
        if (minDist < maxDistSquared) {
            return nearest;
        }
        return null;
    }
    
    private void highlightKey(KeyInfo ki) {
        if (ki == null) return;
        lastHighlightedKey = ki;
        
        // Smooth press animation
        ki.view.animate()
            .scaleX(0.88f)
            .scaleY(0.88f)
            .alpha(0.7f)
            .setDuration(50)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }
    
    private void unhighlightKey(KeyInfo ki) {
        if (ki == null) return;
        
        // Smooth release animation with slight overshoot
        ki.view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(120)
            .setInterpolator(new OvershootInterpolator(1.5f))
            .start();
    }
    
    private void unhighlightAllKeys() {
        for (KeyInfo ki : keyInfoList) {
            ki.view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(100)
                .start();
        }
    }
    
    private void showKeyPreview(KeyInfo ki) {
        if (ki == null) return;
        if (isSpecialKey(ki.key)) {
            hideKeyPreview();
            return;
        }
        
        String display = getPreviewText(ki.key);
        if (display == null || display.isEmpty()) {
            hideKeyPreview();
            return;
        }
        
        keyPreviewText.setText(display);
        
        // Measure popup
        keyPreviewText.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        
        int popupWidth = keyPreviewText.getMeasuredWidth();
        int popupHeight = keyPreviewText.getMeasuredHeight();
        
        // Position above the key
        int[] loc = new int[2];
        ki.view.getLocationOnScreen(loc);
        
        int px = loc[0] + (ki.view.getWidth() - popupWidth) / 2;
        int py = loc[1] - popupHeight - dp(8);
        
        // Keep on screen
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        if (px < dp(4)) px = dp(4);
        if (px + popupWidth > screenWidth - dp(4)) px = screenWidth - popupWidth - dp(4);
        if (py < dp(4)) py = loc[1] + ki.view.getHeight() + dp(4); // Show below if no space
        
        try {
            if (keyPreviewPopup.isShowing()) {
                keyPreviewPopup.update(px, py, popupWidth, popupHeight);
            } else {
                keyPreviewPopup.setWidth(popupWidth);
                keyPreviewPopup.setHeight(popupHeight);
                keyPreviewPopup.showAtLocation(rootContainer, Gravity.NO_GRAVITY, px, py);
            }
            
            // Pop animation
            keyPreviewContainer.setScaleX(0.5f);
            keyPreviewContainer.setScaleY(0.5f);
            keyPreviewContainer.setAlpha(0f);
            keyPreviewContainer.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(80)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
        } catch (Exception e) {
            Log.e(TAG, "Error showing preview", e);
        }
    }
    
    private String getPreviewText(String key) {
        if (key.equals("SPACE")) return null;
        
        // For Sinhala mode, show the Sinhala character
        if (isSinhalaMode && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            Map<String, String> labels = (isShift || isCaps) ? SINHALA_LABELS_SHIFT : SINHALA_LABELS;
            String sinhala = labels.get(key.toLowerCase());
            if (sinhala != null) {
                return sinhala;
            }
        }
        
        // For English, show the character
        if (key.length() == 1) {
            return (isShift || isCaps) ? key.toUpperCase() : key.toLowerCase();
        }
        
        return key;
    }
    
    private void hideKeyPreview() {
        try {
            if (keyPreviewPopup != null && keyPreviewPopup.isShowing()) {
                keyPreviewPopup.dismiss();
            }
        } catch (Exception e) {}
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
            
            tv.setOnClickListener(view -> { vibrate(); commitTextDirect(e); });
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
        container.setClipChildren(false);
        container.setClipToPadding(false);
        
        TextView tv = new TextView(this);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setIncludeFontPadding(false);
        
        String display = getDisplayText(key);
        int textColor = parseColor(colorText);
        float textSize = isSpecialKey(key) ? 14 : keyTextSize;
        
        // Special key styling
        if (key.equals("↵")) { 
            display = "↵"; 
            textColor = Color.WHITE; 
            textSize = 22; 
        } else if (key.equals("⇧")) {
            textSize = 26;
            if (isCaps) { 
                display = "⇪"; 
                textColor = Color.parseColor("#10b981"); 
            } else if (isShift) { 
                display = "⬆"; 
                textColor = Color.parseColor("#3b82f6"); 
            } else { 
                display = "⇧"; 
            }
        } else if (key.equals("⌫")) { 
            display = "⌫"; 
            textSize = 24; 
        } else if (key.equals("SPACE")) { 
            display = isSinhalaMode ? "සිංහල" : "GD Keyboard"; 
            textSize = 11; 
            textColor = Color.parseColor("#666666"); 
        } else if (key.equals("🌐")) {
            display = isSinhalaMode ? "සිං" : "EN";
            textSize = 14;
            textColor = isSinhalaMode ? Color.parseColor("#10b981") : Color.parseColor("#3b82f6");
        } else if (key.equals("✨")) { 
            display = "✨"; 
            textSize = 20; 
        }
        
        tv.setText(display);
        tv.setTextColor(textColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        
        // Sinhala sub-label on keys
        if (isSinhalaMode && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            Map<String, String> labels = (isShift || isCaps) ? SINHALA_LABELS_SHIFT : SINHALA_LABELS;
            String sinhala = labels.get(key.toLowerCase());
            if (sinhala != null) {
                TextView lbl = new TextView(this);
                lbl.setText(sinhala);
                lbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
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
        container.setElevation(dp(2));
        
        keyInfoList.add(new KeyInfo(key, container));
        return container;
    }
    
    private String getDisplayText(String key) {
        if (key.equals("SPACE")) return isSinhalaMode ? "සිංහල" : "GD Keyboard";
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
    
    private GradientDrawable createKeyBg(String key) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(keyRadius));
        
        String color = colorKeyNormal;
        if (key.equals("↵")) color = colorKeyEnter;
        else if (key.equals("⇧")) {
            if (isCaps) color = "#10b981";
            else if (isShift) color = "#3b82f6";
            else color = colorKeySpecial;
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
    // KEY PROCESSING
    // ═══════════════════════════════════════════════════════════════════
    private void processKeyDown(String key) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        
        switch (key) {
            case "⇧": handleShift(); break;
            case "⌫": handleBackspace(ic); startRepeat(key); break;
            case "↵": flushBuffer(ic); handleEnter(ic); break;
            case "SPACE": flushBuffer(ic); ic.commitText(" ", 1); startRepeat(key); break;
            case "123": flushBuffer(ic); isNumbers = true; isSymbols = false; rebuildKeyboard(); break;
            case "ABC": flushBuffer(ic); isNumbers = false; isSymbols = false; rebuildKeyboard(); break;
            case "#+=": flushBuffer(ic); isSymbols = true; isNumbers = false; rebuildKeyboard(); break;
            case "🌐": 
                flushBuffer(ic); 
                isSinhalaMode = !isSinhalaMode; 
                clearBuffer(); 
                rebuildKeyboard(); 
                break;
            case "✨": flushBuffer(ic); openPopup(); break;
            default: handleCharacter(ic, key);
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
        if (buffer.length() > 0) {
            buffer.deleteCharAt(buffer.length() - 1);
            if (buffer.length() == 0) {
                clearBuffer();
            }
            return;
        }
        // Delete from text field
        ic.deleteSurroundingText(1, 0);
    }
    
    private void handleEnter(InputConnection ic) {
        EditorInfo ei = getCurrentInputEditorInfo();
        if (ei != null) {
            int action = ei.imeOptions & EditorInfo.IME_MASK_ACTION;
            if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                ic.performEditorAction(action);
                return;
            }
        }
        ic.commitText("\n", 1);
    }
    
    private void handleCharacter(InputConnection ic, String key) {
        String ch = key;
        if ((isShift || isCaps) && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            ch = key.toUpperCase();
        }
        
        if (isSinhalaMode && key.length() == 1 && Character.isLetter(key.charAt(0))) {
            processSinglish(ic, ch);
        } else {
            flushBuffer(ic);
            ic.commitText(ch, 1);
        }
        autoUnshift();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // SINGLISH ENGINE - ULTRA FAST TRIE-BASED
    // ═══════════════════════════════════════════════════════════════════
    
    private void clearBuffer() {
        buffer.setLength(0);
        lastWasConsonant = false;
        pendingConsonant = "";
    }
    
    private void processSinglish(InputConnection ic, String input) {
        buffer.append(input);
        processBuffer(ic, false);
    }
    
    private void processBuffer(InputConnection ic, boolean flush) {
        while (buffer.length() > 0) {
            String bufStr = buffer.toString();
            boolean matched = false;
            
            // If last output was consonant with hal, try to find modifier first
            if (lastWasConsonant) {
                MatchResult modMatch = findModifier(bufStr, 0);
                
                if (modMatch != null) {
                    // Check if could extend to longer modifier
                    String remaining = bufStr.substring(0, modMatch.length);
                    boolean canExtend = couldExtendModifier(remaining) && !flush && modMatch.length < buffer.length();
                    
                    // If we have more chars or flushing, use this match
                    if (modMatch.length < buffer.length() || flush || !canExtend) {
                        // Remove the hal (්) from previous consonant
                        ic.deleteSurroundingText(1, 0);
                        // Add the modifier (if not empty - 'a' just removes hal)
                        if (!modMatch.value.isEmpty()) {
                            ic.commitText(modMatch.value, 1);
                        }
                        buffer.delete(0, modMatch.length);
                        lastWasConsonant = false;
                        matched = true;
                        continue;
                    } else {
                        // Wait for more input
                        return;
                    }
                }
            }
            
            // Try consonant match
            MatchResult consMatch = findConsonant(bufStr, 0);
            if (consMatch != null) {
                String remaining = bufStr.substring(0, consMatch.length);
                boolean canExtend = couldExtendConsonant(remaining) && !flush;
                
                if (consMatch.length < buffer.length() || flush || !canExtend) {
                    // Output consonant with hal kirima
                    ic.commitText(consMatch.value + "්", 1);
                    buffer.delete(0, consMatch.length);
                    lastWasConsonant = true;
                    matched = true;
                    continue;
                } else {
                    // Could extend, wait for more
                    return;
                }
            }
            
            // Try standalone vowel (only if not after consonant)
            if (!lastWasConsonant) {
                MatchResult vowelMatch = findVowel(bufStr, 0);
                if (vowelMatch != null) {
                    String remaining = bufStr.substring(0, vowelMatch.length);
                    boolean canExtend = couldExtendVowel(remaining) && !flush;
                    
                    if (vowelMatch.length < buffer.length() || flush || !canExtend) {
                        ic.commitText(vowelMatch.value, 1);
                        buffer.delete(0, vowelMatch.length);
                        lastWasConsonant = false;
                        matched = true;
                        continue;
                    } else {
                        return;
                    }
                }
            }
            
            // No match found
            if (!matched) {
                if (flush || buffer.length() >= 4) {
                    // Output first character as-is
                    ic.commitText(String.valueOf(buffer.charAt(0)), 1);
                    buffer.deleteCharAt(0);
                    lastWasConsonant = false;
                } else {
                    // Wait for more input
                    return;
                }
            }
        }
    }
    
    private void flushBuffer(InputConnection ic) {
        if (buffer.length() > 0) {
            processBuffer(ic, true);
        }
        // Output any remaining characters
        if (buffer.length() > 0) {
            ic.commitText(buffer.toString(), 1);
            clearBuffer();
        }
        lastWasConsonant = false;
    }
    
    private void commitTextDirect(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            flushBuffer(ic);
            ic.commitText(text, 1);
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
    
    private void startRepeat(String key) {
        isRepeating = true;
        repeatRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRepeating) {
                    if (key.equals("⌫")) {
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) handleBackspace(ic);
                    } else if (key.equals("SPACE")) {
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) ic.commitText(" ", 1);
                    }
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
        } catch (Exception e) {
            Log.e(TAG, "Error opening popup", e);
        }
    }
    
    private void rebuildKeyboard() {
        if (rootContainer == null) return;
        keyInfoList.clear();
        rootContainer.removeAllViews();
        
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(parseColor(colorBackground));
        
        if (showEmojiRow) { 
            emojiRowView = createEmojiRow(); 
            main.addView(emojiRowView); 
        }
        keyboardView = createKeyboard();
        main.addView(keyboardView);
        
        FrameLayout.LayoutParams mp = new FrameLayout.LayoutParams(-1, -2);
        mp.gravity = Gravity.BOTTOM;
        rootContainer.addView(main, mp);
        
        // Touch layer on top
        View touch = new View(this);
        touch.setBackgroundColor(Color.TRANSPARENT);
        touch.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        touch.setOnTouchListener(this::handleTouch);
        rootContainer.addView(touch);
        
        int eh = showEmojiRow ? dp(44) : 0;
        int th = eh + dp(keyboardHeight) + navigationBarHeight;
        ViewGroup.LayoutParams rp = rootContainer.getLayoutParams();
        if (rp != null) { 
            rp.height = th; 
            rootContainer.setLayoutParams(rp); 
        }
        rootContainer.setPadding(0, 0, 0, navigationBarHeight);
        rootContainer.setBackgroundColor(parseColor(colorBackground));
        
        rootContainer.post(this::updateKeyBounds);
    }
    
    private int dp(int v) { 
        return Math.round(v * getResources().getDisplayMetrics().density); 
    }
    
    private int parseColor(String c) { 
        try { 
            return Color.parseColor(c); 
        } catch (Exception e) { 
            return Color.BLACK; 
        } 
    }
}