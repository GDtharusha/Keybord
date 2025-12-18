package com.keybord.app;

import android.content.Context;
import android.content.SharedPreferences;

public class KeyboardSettings {
    
    private static final String PREFS_NAME = "keyboard_prefs";
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    
    // Broadcast action for settings changed
    public static final String ACTION_SETTINGS_CHANGED = "com.keybord.app.SETTINGS_CHANGED";
    public static final String ACTION_TYPE_TEXT = "com.keybord.app.TYPE_TEXT";
    
    // ════════════════════════════════════════════════════════════════
    // DEFAULT VALUES - මෙතනින් default settings වෙනස් කරන්න පුළුවන්
    // ════════════════════════════════════════════════════════════════
    
    // Colors
    public static final String DEF_COLOR_BG = "#1a1a2e";
    public static final String DEF_COLOR_KEY = "#3d3d5c";
    public static final String DEF_COLOR_KEY_PRESSED = "#5a5a8c";
    public static final String DEF_COLOR_KEY_SPECIAL = "#252540";
    public static final String DEF_COLOR_KEY_ENTER = "#2563eb";
    public static final String DEF_COLOR_KEY_SPACE = "#303050";
    public static final String DEF_COLOR_TEXT = "#FFFFFF";
    public static final String DEF_COLOR_TEXT_SPECIAL = "#9ca3af";
    
    // Sizes
    public static final int DEF_KEYBOARD_HEIGHT = 320;
    public static final int DEF_KEY_TEXT_SIZE = 24;
    public static final int DEF_KEY_RADIUS = 10;
    public static final int DEF_KEY_MARGIN = 3;
    
    // Haptics & Sound
    public static final boolean DEF_VIBRATION_ENABLED = true;
    public static final int DEF_VIBRATION_STRENGTH = 5;
    public static final boolean DEF_SOUND_ENABLED = false;
    public static final int DEF_SOUND_VOLUME = 50;
    
    // Features
    public static final boolean DEF_SHOW_EMOJI_ROW = false;
    public static final boolean DEF_POPUP_ON_PRESS = true;
    public static final int DEF_LONG_PRESS_DELAY = 400;
    
    // ════════════════════════════════════════════════════════════════
    
    public KeyboardSettings(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }
    
    // ──────────────────────────────────────────────────────────────
    // COLOR GETTERS & SETTERS
    // ──────────────────────────────────────────────────────────────
    
    public String getColorBackground() {
        return prefs.getString("color_bg", DEF_COLOR_BG);
    }
    public void setColorBackground(String color) {
        editor.putString("color_bg", color).apply();
    }
    
    public String getColorKey() {
        return prefs.getString("color_key", DEF_COLOR_KEY);
    }
    public void setColorKey(String color) {
        editor.putString("color_key", color).apply();
    }
    
    public String getColorKeyPressed() {
        return prefs.getString("color_key_pressed", DEF_COLOR_KEY_PRESSED);
    }
    public void setColorKeyPressed(String color) {
        editor.putString("color_key_pressed", color).apply();
    }
    
    public String getColorKeySpecial() {
        return prefs.getString("color_key_special", DEF_COLOR_KEY_SPECIAL);
    }
    public void setColorKeySpecial(String color) {
        editor.putString("color_key_special", color).apply();
    }
    
    public String getColorKeyEnter() {
        return prefs.getString("color_key_enter", DEF_COLOR_KEY_ENTER);
    }
    public void setColorKeyEnter(String color) {
        editor.putString("color_key_enter", color).apply();
    }
    
    public String getColorKeySpace() {
        return prefs.getString("color_key_space", DEF_COLOR_KEY_SPACE);
    }
    public void setColorKeySpace(String color) {
        editor.putString("color_key_space", color).apply();
    }
    
    public String getColorText() {
        return prefs.getString("color_text", DEF_COLOR_TEXT);
    }
    public void setColorText(String color) {
        editor.putString("color_text", color).apply();
    }
    
    public String getColorTextSpecial() {
        return prefs.getString("color_text_special", DEF_COLOR_TEXT_SPECIAL);
    }
    public void setColorTextSpecial(String color) {
        editor.putString("color_text_special", color).apply();
    }
    
    // ──────────────────────────────────────────────────────────────
    // SIZE GETTERS & SETTERS
    // ──────────────────────────────────────────────────────────────
    
    public int getKeyboardHeight() {
        return prefs.getInt("keyboard_height", DEF_KEYBOARD_HEIGHT);
    }
    public void setKeyboardHeight(int height) {
        editor.putInt("keyboard_height", height).apply();
    }
    
    public int getKeyTextSize() {
        return prefs.getInt("key_text_size", DEF_KEY_TEXT_SIZE);
    }
    public void setKeyTextSize(int size) {
        editor.putInt("key_text_size", size).apply();
    }
    
    public int getKeyRadius() {
        return prefs.getInt("key_radius", DEF_KEY_RADIUS);
    }
    public void setKeyRadius(int radius) {
        editor.putInt("key_radius", radius).apply();
    }
    
    public int getKeyMargin() {
        return prefs.getInt("key_margin", DEF_KEY_MARGIN);
    }
    public void setKeyMargin(int margin) {
        editor.putInt("key_margin", margin).apply();
    }
    
    // ──────────────────────────────────────────────────────────────
    // HAPTICS & SOUND GETTERS & SETTERS
    // ──────────────────────────────────────────────────────────────
    
    public boolean isVibrationEnabled() {
        return prefs.getBoolean("vibration_enabled", DEF_VIBRATION_ENABLED);
    }
    public void setVibrationEnabled(boolean enabled) {
        editor.putBoolean("vibration_enabled", enabled).apply();
    }
    
    public int getVibrationStrength() {
        return prefs.getInt("vibration_strength", DEF_VIBRATION_STRENGTH);
    }
    public void setVibrationStrength(int strength) {
        editor.putInt("vibration_strength", strength).apply();
    }
    
    public boolean isSoundEnabled() {
        return prefs.getBoolean("sound_enabled", DEF_SOUND_ENABLED);
    }
    public void setSoundEnabled(boolean enabled) {
        editor.putBoolean("sound_enabled", enabled).apply();
    }
    
    public int getSoundVolume() {
        return prefs.getInt("sound_volume", DEF_SOUND_VOLUME);
    }
    public void setSoundVolume(int volume) {
        editor.putInt("sound_volume", volume).apply();
    }
    
    // ──────────────────────────────────────────────────────────────
    // FEATURE GETTERS & SETTERS
    // ──────────────────────────────────────────────────────────────
    
    public boolean isShowEmojiRow() {
        return prefs.getBoolean("show_emoji_row", DEF_SHOW_EMOJI_ROW);
    }
    public void setShowEmojiRow(boolean show) {
        editor.putBoolean("show_emoji_row", show).apply();
    }
    
    public boolean isPopupOnPress() {
        return prefs.getBoolean("popup_on_press", DEF_POPUP_ON_PRESS);
    }
    public void setPopupOnPress(boolean enabled) {
        editor.putBoolean("popup_on_press", enabled).apply();
    }
    
    public int getLongPressDelay() {
        return prefs.getInt("long_press_delay", DEF_LONG_PRESS_DELAY);
    }
    public void setLongPressDelay(int delay) {
        editor.putInt("long_press_delay", delay).apply();
    }
    
    // ──────────────────────────────────────────────────────────────
    // THEME PRESETS
    // ──────────────────────────────────────────────────────────────
    
    public String getCurrentTheme() {
        return prefs.getString("current_theme", "dark");
    }
    public void setCurrentTheme(String theme) {
        editor.putString("current_theme", theme).apply();
    }
    
    public void applyTheme(String themeName) {
        switch (themeName) {
            case "dark":
                setColorBackground("#1a1a2e");
                setColorKey("#3d3d5c");
                setColorKeySpecial("#252540");
                setColorKeyEnter("#2563eb");
                setColorText("#FFFFFF");
                break;
            case "light":
                setColorBackground("#f0f0f5");
                setColorKey("#ffffff");
                setColorKeySpecial("#e0e0e5");
                setColorKeyEnter("#2563eb");
                setColorText("#1a1a2e");
                break;
            case "blue":
                setColorBackground("#0f172a");
                setColorKey("#1e3a5f");
                setColorKeySpecial("#0c2340");
                setColorKeyEnter("#3b82f6");
                setColorText("#FFFFFF");
                break;
            case "green":
                setColorBackground("#0f1f0f");
                setColorKey("#1a3d1a");
                setColorKeySpecial("#0d2d0d");
                setColorKeyEnter("#10b981");
                setColorText("#FFFFFF");
                break;
            case "purple":
                setColorBackground("#1a0a2e");
                setColorKey("#3d2d5c");
                setColorKeySpecial("#251540");
                setColorKeyEnter("#8b5cf6");
                setColorText("#FFFFFF");
                break;
            case "red":
                setColorBackground("#1f0f0f");
                setColorKey("#3d1a1a");
                setColorKeySpecial("#2d0d0d");
                setColorKeyEnter("#ef4444");
                setColorText("#FFFFFF");
                break;
        }
        setCurrentTheme(themeName);
    }
    
    // ──────────────────────────────────────────────────────────────
    // QUICK EMOJIS
    // ──────────────────────────────────────────────────────────────
    
    public String getQuickEmojis() {
        return prefs.getString("quick_emojis", "😀,😂,❤️,👍,🔥,✨,🎉,💯");
    }
    public void setQuickEmojis(String emojis) {
        editor.putString("quick_emojis", emojis).apply();
    }
    
    // ──────────────────────────────────────────────────────────────
    // RESET TO DEFAULTS
    // ──────────────────────────────────────────────────────────────
    
    public void resetToDefaults() {
        editor.clear().apply();
    }
}