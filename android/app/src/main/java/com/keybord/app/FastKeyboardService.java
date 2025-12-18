package com.keybord.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class FastKeyboardService extends InputMethodService {

    // ══════════════════════════════════════════════════════════════════════
    // 🎨 CUSTOMIZATION - Colors
    // ══════════════════════════════════════════════════════════════════════
    private String COLOR_BACKGROUND = "#1a1a2e";
    private String COLOR_TOOLBAR = "#141424";
    private String COLOR_KEY_NORMAL = "#3d3d5c";
    private String COLOR_KEY_SPECIAL = "#252540";
    private String COLOR_KEY_ENTER = "#2563eb";
    private String COLOR_KEY_SPACE = "#303050";
    private String COLOR_TEXT = "#FFFFFF";
    private String COLOR_TEXT_SECONDARY = "#9ca3af";
    private String COLOR_ACCENT = "#3b82f6";
    private String COLOR_EMOJI_BAR = "#1e1e38";
    private String COLOR_OPTIONS_BG = "#12121f";
    
    // ══════════════════════════════════════════════════════════════════════
    // 📏 CUSTOMIZATION - Sizes
    // ══════════════════════════════════════════════════════════════════════
    private static final int TOOLBAR_HEIGHT = 44;
    private static final int EMOJI_BAR_HEIGHT = 40;
    private static final int KEYBOARD_HEIGHT = 240;
    private static final int OPTIONS_PANEL_HEIGHT = 300;
    private static final int KEY_RADIUS = 8;
    private static final int KEY_MARGIN = 3;
    private static final int KEY_TEXT_SIZE = 22;
    
    // ══════════════════════════════════════════════════════════════════════
    // 🔧 Toolbar Buttons
    // ══════════════════════════════════════════════════════════════════════
    private static final String[][] TOOLBAR_BUTTONS = {
        {"🎤", "Voice"},
        {"📋", "Clipboard"},
        {"😀", "Emoji"},
        {"🎨", "Themes"},
        {"⚙️", "Settings"},
        {"📱", "More"}
    };
    
    // ══════════════════════════════════════════════════════════════════════
    // 😀 Quick Emoji Bar
    // ══════════════════════════════════════════════════════════════════════
    private static final String[] QUICK_EMOJIS = {
        "😀", "😂", "🥰", "😎", "🤔", "👍", "❤️", "🔥", "✨", "🎉",
        "😊", "😍", "🤣", "😢", "😡", "🙏", "💪", "🎵", "📷", "💯"
    };
    
    // ══════════════════════════════════════════════════════════════════════
    // 📱 Options Panel Items
    // ══════════════════════════════════════════════════════════════════════
    private static final String[][] OPTIONS_ITEMS = {
        {"🌐", "Languages", "Switch keyboard language"},
        {"🎨", "Themes", "Customize keyboard appearance"},
        {"📐", "Resize", "Change keyboard size"},
        {"🔊", "Sound", "Key press sounds"},
        {"📳", "Vibration", "Haptic feedback settings"},
        {"🔤", "Fonts", "Change font style"},
        {"⌨️", "Layout", "Keyboard layout options"},
        {"☁️", "Backup", "Backup & restore settings"},
        {"🤖", "AI Mode", "Enable AI suggestions"},
        {"📝", "Text Tools", "Text editing tools"},
        {"🔒", "Privacy", "Privacy settings"},
        {"ℹ️", "About", "App information"}
    };
    
    // ══════════════════════════════════════════════════════════════════════
    // ⌨️ Keyboard Layouts
    // ══════════════════════════════════════════════════════════════════════
    private static final String[][] LAYOUT_LETTERS = {
        {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
        {"a", "s", "d", "f", "g", "h", "j", "k", "l"},
        {"⇧", "z", "x", "c", "v", "b", "n", "m", "⌫"},
        {"123", ",", "SPACE", ".", "↵"}
    };
    
    private static final String[][] LAYOUT_NUMBERS = {
        {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
        {"@", "#", "$", "%", "&", "-", "+", "(", ")"},
        {"#+=", "*", "\"", "'", ":", ";", "!", "?", "⌫"},
        {"ABC", ",", "SPACE", ".", "↵"}
    };
    
    private static final String[][] LAYOUT_SYMBOLS = {
        {"~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆"},
        {"£", "€", "¥", "^", "°", "=", "{", "}", "\\"},
        {"123", "©", "®", "™", "✓", "[", "]", "<", "⌫"},
        {"ABC", ",", "SPACE", ".", "↵"}
    };

    // ══════════════════════════════════════════════════════════════════════
    // 🔧 Internal State
    // ══════════════════════════════════════════════════════════════════════
    private LinearLayout mainContainer;
    private LinearLayout toolbarView;
    private LinearLayout emojiBarView;
    private LinearLayout keyboardView;
    private ScrollView optionsPanelView;
    
    private Handler handler;
    private Vibrator vibrator;
    private SharedPreferences prefs;
    
    private boolean isShift = false;
    private boolean isCaps = false;
    private boolean isNumbers = false;
    private boolean isSymbols = false;
    private boolean isOptionsVisible = false;
    private boolean isRepeating = false;
    private Runnable repeatRunnable;
    
    // Settings receiver
    private BroadcastReceiver settingsReceiver;

    // ══════════════════════════════════════════════════════════════════════
    // 🚀 Lifecycle
    // ══════════════════════════════════════════════════════════════════════
    
    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            prefs = getSharedPreferences("keyboard_settings", MODE_PRIVATE);
            loadSettings();
            registerSettingsReceiver();
        } catch (Exception e) {}
    }
    
    private void loadSettings() {
        COLOR_BACKGROUND = prefs.getString("color_background", COLOR_BACKGROUND);
        COLOR_ACCENT = prefs.getString("color_accent", COLOR_ACCENT);
        // Add more settings as needed
    }
    
    private void registerSettingsReceiver() {
        settingsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadSettings();
                refreshAll();
            }
        };
        IntentFilter filter = new IntentFilter("com.keybord.app.SETTINGS_UPDATED");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(settingsReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(settingsReceiver, filter);
        }
    }

    @Override
    public View onCreateInputView() {
        mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setBackgroundColor(Color.parseColor(COLOR_BACKGROUND));
        
        int totalHeight = TOOLBAR_HEIGHT + EMOJI_BAR_HEIGHT + KEYBOARD_HEIGHT;
        mainContainer.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, dp(totalHeight)
        ));
        
        // Build all sections
        toolbarView = buildToolbar();
        emojiBarView = buildEmojiBar();
        keyboardView = buildKeyboard();
        optionsPanelView = buildOptionsPanel();
        
        // Add views
        mainContainer.addView(toolbarView);
        mainContainer.addView(emojiBarView);
        mainContainer.addView(keyboardView);
        
        return mainContainer;
    }

    // ══════════════════════════════════════════════════════════════════════
    // 🔧 TOOLBAR
    // ══════════════════════════════════════════════════════════════════════
    
    private LinearLayout buildToolbar() {
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setBackgroundColor(Color.parseColor(COLOR_TOOLBAR));
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(8), 0, dp(8), 0);
        toolbar.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(TOOLBAR_HEIGHT)
        ));
        
        for (String[] btn : TOOLBAR_BUTTONS) {
            toolbar.addView(createToolbarButton(btn[0], btn[1]));
        }
        
        return toolbar;
    }
    
    private View createToolbarButton(String icon, String action) {
        TextView btn = new TextView(this);
        btn.setText(icon);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(12), dp(8), dp(12), dp(8));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        btn.setLayoutParams(params);
        
        btn.setOnClickListener(v -> {
            doVibrate();
            handleToolbarAction(action);
        });
        
        return btn;
    }
    
    private void handleToolbarAction(String action) {
        switch (action) {
            case "Voice":
                showToast("🎤 Voice Input - Coming Soon!");
                break;
            case "Clipboard":
                showToast("📋 Clipboard Manager - Coming Soon!");
                break;
            case "Emoji":
                showToast("😀 Full Emoji Panel - Coming Soon!");
                break;
            case "Themes":
                showToast("🎨 Theme Selector - Coming Soon!");
                break;
            case "Settings":
                showToast("⚙️ Opening Settings...");
                openSettingsApp();
                break;
            case "More":
                toggleOptionsPanel();
                break;
        }
    }
    
    private void openSettingsApp() {
        Intent intent = new Intent();
        intent.setClassName(getPackageName(), getPackageName() + ".MainActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception e) {
            showToast("Cannot open settings");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 😀 EMOJI BAR
    // ══════════════════════════════════════════════════════════════════════
    
    private LinearLayout buildEmojiBar() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setBackgroundColor(Color.parseColor(COLOR_EMOJI_BAR));
        container.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(EMOJI_BAR_HEIGHT)
        ));
        
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
        ));
        
        LinearLayout emojiRow = new LinearLayout(this);
        emojiRow.setOrientation(LinearLayout.HORIZONTAL);
        emojiRow.setPadding(dp(4), 0, dp(4), 0);
        emojiRow.setGravity(Gravity.CENTER_VERTICAL);
        
        for (String emoji : QUICK_EMOJIS) {
            emojiRow.addView(createEmojiButton(emoji));
        }
        
        scroll.addView(emojiRow);
        container.addView(scroll);
        
        return container;
    }
    
    private View createEmojiButton(String emoji) {
        TextView btn = new TextView(this);
        btn.setText(emoji);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(10), dp(4), dp(10), dp(4));
        
        btn.setOnClickListener(v -> {
            doVibrate();
            typeText(emoji);
        });
        
        return btn;
    }

    // ══════════════════════════════════════════════════════════════════════
    // 📱 OPTIONS PANEL
    // ══════════════════════════════════════════════════════════════════════
    
    private ScrollView buildOptionsPanel() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor(COLOR_OPTIONS_BG));
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(OPTIONS_PANEL_HEIGHT)
        ));
        scroll.setVisibility(View.GONE);
        
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));
        
        // Title
        TextView title = new TextView(this);
        title.setText("⚡ Quick Options");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, dp(16));
        content.addView(title);
        
        // Options grid
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        
        for (int i = 0; i < OPTIONS_ITEMS.length; i += 3) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            
            for (int j = 0; j < 3 && (i + j) < OPTIONS_ITEMS.length; j++) {
                row.addView(createOptionItem(OPTIONS_ITEMS[i + j]));
            }
            
            grid.addView(row);
        }
        
        content.addView(grid);
        
        // Back button
        TextView backBtn = new TextView(this);
        backBtn.setText("⌨️ Back to Keyboard");
        backBtn.setTextColor(Color.parseColor(COLOR_ACCENT));
        backBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        backBtn.setGravity(Gravity.CENTER);
        backBtn.setPadding(dp(16), dp(16), dp(16), dp(16));
        backBtn.setOnClickListener(v -> toggleOptionsPanel());
        
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        backParams.setMargins(0, dp(16), 0, 0);
        backBtn.setLayoutParams(backParams);
        
        content.addView(backBtn);
        scroll.addView(content);
        
        return scroll;
    }
    
    private View createOptionItem(String[] item) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(dp(8), dp(12), dp(8), dp(12));
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        container.setLayoutParams(params);
        
        // Icon
        TextView icon = new TextView(this);
        icon.setText(item[0]);
        icon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        icon.setGravity(Gravity.CENTER);
        
        // Label
        TextView label = new TextView(this);
        label.setText(item[1]);
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, dp(4), 0, 0);
        
        container.addView(icon);
        container.addView(label);
        
        // Background
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2a2a4a"));
        bg.setCornerRadius(dp(12));
        container.setBackground(bg);
        
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        containerParams.setMargins(dp(4), dp(4), dp(4), dp(4));
        container.setLayoutParams(containerParams);
        
        container.setOnClickListener(v -> {
            doVibrate();
            showToast(item[0] + " " + item[1] + "\n" + item[2]);
        });
        
        return container;
    }
    
    private void toggleOptionsPanel() {
        isOptionsVisible = !isOptionsVisible;
        
        if (isOptionsVisible) {
            // Hide keyboard, show options
            keyboardView.setVisibility(View.GONE);
            emojiBarView.setVisibility(View.GONE);
            
            if (optionsPanelView.getParent() == null) {
                mainContainer.addView(optionsPanelView);
            }
            optionsPanelView.setVisibility(View.VISIBLE);
            
            // Resize container
            mainContainer.getLayoutParams().height = dp(TOOLBAR_HEIGHT + OPTIONS_PANEL_HEIGHT);
            mainContainer.requestLayout();
        } else {
            // Show keyboard, hide options
            optionsPanelView.setVisibility(View.GONE);
            emojiBarView.setVisibility(View.VISIBLE);
            keyboardView.setVisibility(View.VISIBLE);
            
            // Resize container
            mainContainer.getLayoutParams().height = dp(TOOLBAR_HEIGHT + EMOJI_BAR_HEIGHT + KEYBOARD_HEIGHT);
            mainContainer.requestLayout();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // ⌨️ KEYBOARD
    // ══════════════════════════════════════════════════════════════════════
    
    private LinearLayout buildKeyboard() {
        LinearLayout kb = new LinearLayout(this);
        kb.setOrientation(LinearLayout.VERTICAL);
        kb.setBackgroundColor(Color.parseColor(COLOR_BACKGROUND));
        kb.setPadding(dp(4), dp(6), dp(4), dp(10));
        kb.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(KEYBOARD_HEIGHT)
        ));
        
        String[][] layout = getActiveLayout();
        for (int i = 0; i < layout.length; i++) {
            kb.addView(buildKeyboardRow(layout[i], i));
        }
        
        return kb;
    }
    
    private String[][] getActiveLayout() {
        if (isSymbols) return LAYOUT_SYMBOLS;
        if (isNumbers) return LAYOUT_NUMBERS;
        return LAYOUT_LETTERS;
    }
    
    private LinearLayout buildKeyboardRow(String[] keys, int rowIndex) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ));
        row.setPadding(0, dp(3), 0, dp(3));
        
        if (rowIndex == 1) {
            row.setPadding(dp(16), dp(3), dp(16), dp(3));
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
        
        // Text styling
        boolean isSpecial = isSpecialKey(key);
        tv.setTextColor(Color.parseColor(isSpecial ? COLOR_TEXT_SECONDARY : COLOR_TEXT));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, isSpecial && !isIconKey(key) ? 14 : KEY_TEXT_SIZE);
        
        if (key.equals("↵")) tv.setTextColor(Color.WHITE);
        if ((key.equals("⇧") && (isShift || isCaps))) tv.setTextColor(Color.WHITE);
        
        // Layout
        float weight = getKeyWeight(key);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight);
        params.setMargins(dp(KEY_MARGIN), 0, dp(KEY_MARGIN), 0);
        tv.setLayoutParams(params);
        
        // Background
        tv.setBackground(createKeyBg(key));
        
        // Touch
        tv.setOnTouchListener((v, e) -> handleKeyTouch(v, e, key));
        tv.setClickable(true);
        
        return tv;
    }
    
    private String getKeyDisplay(String key) {
        switch (key) {
            case "⇧": return (isShift || isCaps) ? "⬆" : "⇧";
            case "SPACE": return "space";
            default:
                if (key.length() == 1 && Character.isLetter(key.charAt(0))) {
                    return (isShift || isCaps) ? key.toUpperCase() : key.toLowerCase();
                }
                return key;
        }
    }
    
    private boolean isSpecialKey(String key) {
        return key.equals("⇧") || key.equals("⌫") || key.equals("↵") || 
               key.equals("SPACE") || key.equals("123") || key.equals("ABC") || key.equals("#+=");
    }
    
    private boolean isIconKey(String key) {
        return key.equals("⇧") || key.equals("⌫") || key.equals("↵");
    }
    
    private float getKeyWeight(String key) {
        switch (key) {
            case "SPACE": return 5f;
            case "⇧": case "⌫": case "↵": case "123": case "ABC": case "#+=": return 1.5f;
            default: return 1f;
        }
    }
    
    private GradientDrawable createKeyBg(String key) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(KEY_RADIUS));
        
        String color;
        switch (key) {
            case "↵": color = COLOR_KEY_ENTER; break;
            case "⇧": 
                if (isCaps) color = "#10b981";
                else if (isShift) color = COLOR_ACCENT;
                else color = COLOR_KEY_SPECIAL;
                break;
            case "⌫": case "123": case "ABC": case "#+=": color = COLOR_KEY_SPECIAL; break;
            case "SPACE": color = COLOR_KEY_SPACE; break;
            default: color = COLOR_KEY_NORMAL;
        }
        
        bg.setColor(Color.parseColor(color));
        return bg;
    }
    
    private boolean handleKeyTouch(View v, MotionEvent e, String key) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                v.setAlpha(0.7f);
                v.setScaleX(0.95f);
                v.setScaleY(0.95f);
                doVibrate();
                processKey(key);
                if (key.equals("⌫")) startRepeat(key);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                v.setAlpha(1f);
                v.setScaleX(1f);
                v.setScaleY(1f);
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
                handler.postDelayed(repeatRunnable, 50);
            }
        };
        handler.postDelayed(repeatRunnable, 400);
    }
    
    private void stopRepeat() {
        isRepeating = false;
        if (repeatRunnable != null) handler.removeCallbacks(repeatRunnable);
    }
    
    private void processKey(String key) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        
        switch (key) {
            case "⇧":
                if (isCaps) { isCaps = false; isShift = false; }
                else if (isShift) { isCaps = true; }
                else { isShift = true; }
                refreshKeyboard();
                break;
            case "⌫": ic.deleteSurroundingText(1, 0); break;
            case "↵": handleEnter(ic); break;
            case "SPACE": typeText(" "); autoUnshift(); break;
            case "123": isNumbers = true; isSymbols = false; refreshKeyboard(); break;
            case "ABC": isNumbers = false; isSymbols = false; refreshKeyboard(); break;
            case "#+=": isSymbols = true; isNumbers = false; refreshKeyboard(); break;
            default:
                String text = key;
                if ((isShift || isCaps) && key.length() == 1 && Character.isLetter(key.charAt(0))) {
                    text = key.toUpperCase();
                }
                typeText(text);
                autoUnshift();
        }
    }
    
    private void handleEnter(InputConnection ic) {
        EditorInfo ei = getCurrentInputEditorInfo();
        if (ei != null) {
            int action = ei.imeOptions & EditorInfo.IME_MASK_ACTION;
            if (action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED) {
                typeText("\n");
            } else {
                ic.performEditorAction(action);
            }
        } else {
            typeText("\n");
        }
    }
    
    private void typeText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(text, 1);
    }
    
    private void autoUnshift() {
        if (isShift && !isCaps) {
            isShift = false;
            refreshKeyboard();
        }
    }
    
    private void refreshKeyboard() {
        if (keyboardView != null) {
            keyboardView.removeAllViews();
            String[][] layout = getActiveLayout();
            for (int i = 0; i < layout.length; i++) {
                keyboardView.addView(buildKeyboardRow(layout[i], i));
            }
        }
    }
    
    private void refreshAll() {
        if (mainContainer != null) {
            // Rebuild everything with new settings
            mainContainer.removeAllViews();
            
            toolbarView = buildToolbar();
            emojiBarView = buildEmojiBar();
            keyboardView = buildKeyboard();
            optionsPanelView = buildOptionsPanel();
            
            mainContainer.addView(toolbarView);
            mainContainer.addView(emojiBarView);
            mainContainer.addView(keyboardView);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 🔧 Utilities
    // ══════════════════════════════════════════════════════════════════════
    
    private void doVibrate() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(5, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(5);
                }
            }
        } catch (Exception e) {}
    }
    
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    
    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ══════════════════════════════════════════════════════════════════════
    // 🔄 Lifecycle
    // ══════════════════════════════════════════════════════════════════════
    
    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        isShift = false;
        isCaps = false;
        isOptionsVisible = false;
        
        int inputClass = info.inputType & EditorInfo.TYPE_MASK_CLASS;
        isNumbers = (inputClass == EditorInfo.TYPE_CLASS_NUMBER || inputClass == EditorInfo.TYPE_CLASS_PHONE);
        isSymbols = false;
        
        // Ensure keyboard is visible, not options
        if (optionsPanelView != null) optionsPanelView.setVisibility(View.GONE);
        if (emojiBarView != null) emojiBarView.setVisibility(View.VISIBLE);
        if (keyboardView != null) keyboardView.setVisibility(View.VISIBLE);
        
        if (mainContainer != null) {
            mainContainer.getLayoutParams().height = dp(TOOLBAR_HEIGHT + EMOJI_BAR_HEIGHT + KEYBOARD_HEIGHT);
            mainContainer.requestLayout();
        }
        
        refreshKeyboard();
    }
    
    @Override
    public void onDestroy() {
        stopRepeat();
        if (settingsReceiver != null) {
            try {
                unregisterReceiver(settingsReceiver);
            } catch (Exception e) {}
        }
        super.onDestroy();
    }
}