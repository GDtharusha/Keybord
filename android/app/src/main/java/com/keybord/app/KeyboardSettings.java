package com.keybord.app;

import android.content.Context;
import android.content.SharedPreferences;

public class KeyboardSettings {
    
    private static final String PREFS_NAME = "keyboard_prefs";
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    
    public static final String ACTION_SETTINGS_CHANGED = "com.keybord.app.SETTINGS_CHANGED";
    public static final String ACTION_TYPE_TEXT = "com.keybord.app.TYPE_TEXT";
    
    // Default values
    public static final String DEF_COLOR_BG = "#000000";
    public static final String DEF_COLOR_KEY = "#1a1a1a";
    public static final String DEF_COLOR_KEY_SPECIAL = "#0d0d0d";
    public static final String DEF_COLOR_KEY_ENTER = "#2563eb";
    public static final String DEF_COLOR_KEY_SPACE = "#1a1a1a";
    public static final String DEF_COLOR_TEXT = "#ffffff";
    
    public static final int DEF_KEYBOARD_HEIGHT = 245;
    public static final int DEF_KEY_TEXT_SIZE = 20;
    public static final int DEF_KEY_RADIUS = 8;
    public static final int DEF_KEY_GAP = 2;
    
    public static final boolean DEF_VIBRATION_ENABLED = true;
    public static final int DEF_VIBRATION_STRENGTH = 5;
    public static final boolean DEF_SHOW_EMOJI_ROW = false;
    public static final int DEF_LONG_PRESS_DELAY = 350;
    
    public KeyboardSettings(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }
    
    // Colors
    public String getColorBackground() { return prefs.getString("color_bg", DEF_COLOR_BG); }
    public void setColorBackground(String color) { editor.putString("color_bg", color).apply(); }
    
    public String getColorKey() { return prefs.getString("color_key", DEF_COLOR_KEY); }
    public void setColorKey(String color) { editor.putString("color_key", color).apply(); }
    
    public String getColorKeySpecial() { return prefs.getString("color_key_special", DEF_COLOR_KEY_SPECIAL); }
    public void setColorKeySpecial(String color) { editor.putString("color_key_special", color).apply(); }
    
    public String getColorKeyEnter() { return prefs.getString("color_key_enter", DEF_COLOR_KEY_ENTER); }
    public void setColorKeyEnter(String color) { editor.putString("color_key_enter", color).apply(); }
    
    public String getColorKeySpace() { return prefs.getString("color_key_space", DEF_COLOR_KEY_SPACE); }
    public void setColorKeySpace(String color) { editor.putString("color_key_space", color).apply(); }
    
    public String getColorText() { return prefs.getString("color_text", DEF_COLOR_TEXT); }
    public void setColorText(String color) { editor.putString("color_text", color).apply(); }
    
    // Sizes
    public int getKeyboardHeight() { return prefs.getInt("keyboard_height", DEF_KEYBOARD_HEIGHT); }
    public void setKeyboardHeight(int height) { editor.putInt("keyboard_height", height).apply(); }
    
    public int getKeyTextSize() { return prefs.getInt("key_text_size", DEF_KEY_TEXT_SIZE); }
    public void setKeyTextSize(int size) { editor.putInt("key_text_size", size).apply(); }
    
    public int getKeyRadius() { return prefs.getInt("key_radius", DEF_KEY_RADIUS); }
    public void setKeyRadius(int radius) { editor.putInt("key_radius", radius).apply(); }
    
    public int getKeyGap() { return prefs.getInt("key_gap", DEF_KEY_GAP); }
    public void setKeyGap(int gap) { editor.putInt("key_gap", gap).apply(); }
    
    // Haptics
    public boolean isVibrationEnabled() { return prefs.getBoolean("vibration_enabled", DEF_VIBRATION_ENABLED); }
    public void setVibrationEnabled(boolean enabled) { editor.putBoolean("vibration_enabled", enabled).apply(); }
    
    public int getVibrationStrength() { return prefs.getInt("vibration_strength", DEF_VIBRATION_STRENGTH); }
    public void setVibrationStrength(int strength) { editor.putInt("vibration_strength", strength).apply(); }
    
    // Features
    public boolean isShowEmojiRow() { return prefs.getBoolean("show_emoji_row", DEF_SHOW_EMOJI_ROW); }
    public void setShowEmojiRow(boolean show) { editor.putBoolean("show_emoji_row", show).apply(); }
    
    public int getLongPressDelay() { return prefs.getInt("long_press_delay", DEF_LONG_PRESS_DELAY); }
    public void setLongPressDelay(int delay) { editor.putInt("long_press_delay", delay).apply(); }
    
    // Theme
    public String getTheme() { return prefs.getString("theme", "black"); }
    public void setTheme(String theme) { editor.putString("theme", theme).apply(); }
    
    public String getQuickEmojis() { return prefs.getString("quick_emojis", "😀,😂,❤️,👍,🔥,✨,🎉,💯"); }
    public void setQuickEmojis(String emojis) { editor.putString("quick_emojis", emojis).apply(); }
    
    public void resetToDefaults() { editor.clear().apply(); }
}