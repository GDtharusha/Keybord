package com.keybord.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
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
import android.view.LayoutInflater;
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
        } catch (Exception e) {}
        
        calculateNavigationBarHeight();
        setupKeyPreviewPopup();
        
        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception e) {}
        
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(KeyboardSettings.ACTION_SETTINGS_CHANGED);
            filter.addAction(KeyboardSettings.ACTION_TYPE_TEXT);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(settingsReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(settingsReceiver, filter);
            }
        } catch (Exception e) {}
    }
    
    private void setupKeyPreviewPopup() {
        keyPreviewText = new TextView(this);
        keyPreviewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        keyPreviewText.setTextColor(Color.WHITE);
        keyPreviewText.setTypeface(Typeface.DEFAULT_BOLD);
        keyPreviewText.setGravity(Gravity.CENTER);
        keyPreviewText.setPadding(dp(16), dp(12), dp(16), dp(12));
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#333333"));
        bg.setCornerRadius(dp(12));
        keyPreviewText.setBackground(bg);
        
        keyPreviewPopup = new PopupWindow(keyPreviewText, 
            ViewGroup.LayoutParams.WRAP_CONTENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT);
        keyPreviewPopup.setClippingEnabled(false);
    }
    
    @Override
    public void onDestroy() {
        stopRepeat();
        if (keyPreviewPopup != null && keyPreviewPopup.isShowing()) {
            keyPreviewPopup.dismiss();
        }
        try { unregisterReceiver(settingsReceiver); } catch (Exception e) {}
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
            
            // Main keyboard layout
            LinearLayout mainLayout = new LinearLayout(this);
            mainLayout.setOrientation(LinearLayout.VERTICAL);
            mainLayout.setBackgroundColor(parseColor(colorBackground));
            
            // Emoji row (separate, not affected by touch layer)
            if (showEmojiRow) {
                emojiRowView = createEmojiRow();
                mainLayout.addView(emojiRowView);
            }
            
            // Keyboard view
            keyboardView = createKeyboardLayout();
            mainLayout.addView(keyboardView);
            
            FrameLayout.LayoutParams mainParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            );
            mainParams.gravity = Gravity.BOTTOM;
            rootContainer.addView(mainLayout, mainParams);
            
            // Calculate total height
            int emojiRowHeight = showEmojiRow ? dp(42) : 0;
            int mainHeight = dp(keyboardHeight);
            int totalHeight = emojiRowHeight + mainHeight + navigationBarHeight;
            
            rootContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                totalHeight
            ));
            
            rootContainer.setPadding(0, 0, 0, navigationBarHeight);
            
            return rootContainer;
            
        } catch (Exception e) {
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
            
            if (info != null) {
                int inputClass = info.inputType & EditorInfo.TYPE_MASK_CLASS;
                isNumbers = (inputClass == EditorInfo.TYPE_CLASS_NUMBER || 
                             inputClass == EditorInfo.TYPE_CLASS_PHONE);
            }
            
            loadSettings();
            calculateNavigationBarHeight();
            rebuildKeyboard();
        } catch (Exception e) {}
    }
    
    private void loadSettings() {
        if (settings == null) settings = new KeyboardSettings(this);
        
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
        } catch (Exception e) {}
    }
    
    private LinearLayout createEmojiRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(42)
        ));
        row.setPadding(dp(4), dp(4), dp(4), dp(4));
        row.setBackgroundColor(parseColor(colorKeySpecial));
        row.setElevation(dp(4)); // Raise above keyboard
        
        String emojiStr = "😀,😂,❤️,👍,🔥,✨,🎉,💯";
        try { emojiStr = settings.getQuickEmojis(); } catch (Exception e) {}
        
        String[] emojis = emojiStr.split(",");
        for (final String emoji : emojis) {
            final String trimmedEmoji = emoji.trim();
            TextView tv = new TextView(this);
            tv.setText(trimmedEmoji);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
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
        
        keyboard.setPadding(dp(2), dp(4), dp(2), dp(4));
        
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
            row.setPadding(dp(12), vertPad, dp(12), vertPad);
        } else {
            row.setPadding(0, vertPad, 0, vertPad);
        }
        
        for (String key : keys) {
            row.addView(createKey(key));
        }
        
        return row;
    }
    
    private View createKey(final String key) {
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
            textSize = 18;
        } else if (key.equals("⇧")) {
            textSize = 20;
            if (isCaps) {
                textColor = Color.parseColor("#10b981");
            } else if (isShift) {
                textColor = Color.parseColor("#3b82f6");
            }
        } else if (key.equals("⌫")) {
            textSize = 18;
        } else if (key.equals("SPACE")) {
            textSize = 11;
            textColor = Color.parseColor("#666666");
        }
        
        tv.setTextColor(textColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        
        // Layout
        float weight = getKeyWeight(key);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.MATCH_PARENT, weight
        );
        params.setMargins(dp(keyGap), 0, dp(keyGap), 0);
        tv.setLayoutParams(params);
        
        // Background
        tv.setBackground(createKeyBackground(key));
        
        // Touch handling with preview
        tv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setAlpha(0.8f);
                        doVibrate();
                        
                        // Show key preview for letter keys
                        if (!isSpecialKey(key) && key.length() == 1) {
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
                        hideKeyPreview();
                        stopRepeat();
                        return true;
                }
                return false;
            }
        });
        
        tv.setClickable(true);
        return tv;
    }
    
    private void showKeyPreview(View anchor, String text) {
        try {
            keyPreviewText.setText(text);
            
            int[] location = new int[2];
            anchor.getLocationOnScreen(location);
            
            int x = location[0] + (anchor.getWidth() / 2) - dp(30);
            int y = location[1] - dp(60);
            
            if (keyPreviewPopup.isShowing()) {
                keyPreviewPopup.update(x, y, -1, -1);
            } else {
                keyPreviewPopup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);
            }
        } catch (Exception e) {}
    }
    
    private void hideKeyPreview() {
        try {
            if (keyPreviewPopup != null && keyPreviewPopup.isShowing()) {
                keyPreviewPopup.dismiss();
            }
        } catch (Exception e) {}
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
               key.equals("#+=");
    }
    
    private float getKeyWeight(String key) {
        if (key.equals("SPACE")) return 5f;
        if (key.equals("⇧") || key.equals("⌫")) return 1.5f;
        if (key.equals("↵") || key.equals("123") || key.equals("ABC") || key.equals("#+=")) return 1.4f;
        return 1f;
    }
    
    private GradientDrawable createKeyBackground(String key) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(keyRadius));
        
        String color = colorKeyNormal;
        
        if (key.equals("↵")) {
            color = colorKeyEnter;
        } else if (key.equals("⇧")) {
            if (isCaps) color = "#10b981";
            else if (isShift) color = "#3b82f6";
            else color = colorKeySpecial;
        } else if (key.equals("⌫") || key.equals("123") || key.equals("ABC") || key.equals("#+=")) {
            color = colorKeySpecial;
        } else if (key.equals("SPACE")) {
            color = colorKeySpace;
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
            ic.commitText(" ", 1);
            autoUnshift();
        } else if (key.equals("123")) {
            isNumbers = true;
            isSymbols = false;
            rebuildKeyboard();
        } else if (key.equals("ABC")) {
            isNumbers = false;
            isSymbols = false;
            rebuildKeyboard();
        } else if (key.equals("#+=")) {
            isSymbols = true;
            isNumbers = false;
            rebuildKeyboard();
        } else {
            String text = key;
            if ((isShift || isCaps) && key.length() == 1 && Character.isLetter(key.charAt(0))) {
                text = key.toUpperCase();
            }
            ic.commitText(text, 1);
            autoUnshift();
        }
    }
    
    private void deleteCharacter(InputConnection ic) {
        if (ic == null) return;
        
        CharSequence beforeCursor = ic.getTextBeforeCursor(2, 0);
        
        if (beforeCursor != null && beforeCursor.length() >= 2) {
            String text = beforeCursor.toString();
            char lastChar = text.charAt(text.length() - 1);
            char secondLastChar = text.charAt(text.length() - 2);
            
            // Check for surrogate pair (emoji)
            if (Character.isLowSurrogate(lastChar) && Character.isSurrogatePair(secondLastChar, lastChar)) {
                ic.deleteSurroundingText(2, 0);
                return;
            }
            
            // Check for variation selector
            if (lastChar == '\uFE0F' || lastChar == '\u200D') {
                ic.deleteSurroundingText(2, 0);
                return;
            }
        }
        
        ic.deleteSurroundingText(1, 0);
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
        
        int emojiRowHeight = showEmojiRow ? dp(42) : 0;
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