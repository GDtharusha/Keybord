package com.keybord.app

import android.content.Context
import android.content.SharedPreferences

class KeyboardSettings(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "keyboard_prefs"
        const val ACTION_SETTINGS_CHANGED = "com.keybord.app.SETTINGS_CHANGED"
        const val ACTION_TYPE_TEXT = "com.keybord.app.TYPE_TEXT"
        
        // Defaults
        const val DEF_COLOR_BG = "#000000"
        const val DEF_COLOR_KEY = "#1a1a1a"
        const val DEF_COLOR_KEY_SPECIAL = "#0d0d0d"
        const val DEF_COLOR_KEY_ENTER = "#2563eb"
        const val DEF_COLOR_KEY_SPACE = "#1a1a1a"
        const val DEF_COLOR_TEXT = "#ffffff"
        
        const val DEF_KEYBOARD_HEIGHT = 245
        const val DEF_KEY_TEXT_SIZE = 20
        const val DEF_KEY_RADIUS = 8
        const val DEF_KEY_GAP = 2
        
        const val DEF_VIBRATION_ENABLED = true
        const val DEF_VIBRATION_STRENGTH = 5
        const val DEF_SHOW_EMOJI_ROW = false
        const val DEF_LONG_PRESS_DELAY = 350
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Colors
    var colorBackground: String
        get() = prefs.getString("color_bg", DEF_COLOR_BG) ?: DEF_COLOR_BG
        set(value) = prefs.edit().putString("color_bg", value).apply()
    
    var colorKey: String
        get() = prefs.getString("color_key", DEF_COLOR_KEY) ?: DEF_COLOR_KEY
        set(value) = prefs.edit().putString("color_key", value).apply()
    
    var colorKeySpecial: String
        get() = prefs.getString("color_key_special", DEF_COLOR_KEY_SPECIAL) ?: DEF_COLOR_KEY_SPECIAL
        set(value) = prefs.edit().putString("color_key_special", value).apply()
    
    var colorKeyEnter: String
        get() = prefs.getString("color_key_enter", DEF_COLOR_KEY_ENTER) ?: DEF_COLOR_KEY_ENTER
        set(value) = prefs.edit().putString("color_key_enter", value).apply()
    
    var colorKeySpace: String
        get() = prefs.getString("color_key_space", DEF_COLOR_KEY_SPACE) ?: DEF_COLOR_KEY_SPACE
        set(value) = prefs.edit().putString("color_key_space", value).apply()
    
    var colorText: String
        get() = prefs.getString("color_text", DEF_COLOR_TEXT) ?: DEF_COLOR_TEXT
        set(value) = prefs.edit().putString("color_text", value).apply()
    
    // Sizes
    var keyboardHeight: Int
        get() = prefs.getInt("keyboard_height", DEF_KEYBOARD_HEIGHT)
        set(value) = prefs.edit().putInt("keyboard_height", value).apply()
    
    var keyTextSize: Int
        get() = prefs.getInt("key_text_size", DEF_KEY_TEXT_SIZE)
        set(value) = prefs.edit().putInt("key_text_size", value).apply()
    
    var keyRadius: Int
        get() = prefs.getInt("key_radius", DEF_KEY_RADIUS)
        set(value) = prefs.edit().putInt("key_radius", value).apply()
    
    var keyGap: Int
        get() = prefs.getInt("key_gap", DEF_KEY_GAP)
        set(value) = prefs.edit().putInt("key_gap", value).apply()
    
    // Haptics
    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean("vibration_enabled", DEF_VIBRATION_ENABLED)
        set(value) = prefs.edit().putBoolean("vibration_enabled", value).apply()
    
    var vibrationStrength: Int
        get() = prefs.getInt("vibration_strength", DEF_VIBRATION_STRENGTH)
        set(value) = prefs.edit().putInt("vibration_strength", value).apply()
    
    // Features
    var isShowEmojiRow: Boolean
        get() = prefs.getBoolean("show_emoji_row", DEF_SHOW_EMOJI_ROW)
        set(value) = prefs.edit().putBoolean("show_emoji_row", value).apply()
    
    var longPressDelay: Int
        get() = prefs.getInt("long_press_delay", DEF_LONG_PRESS_DELAY)
        set(value) = prefs.edit().putInt("long_press_delay", value).apply()
    
    // Theme
    var theme: String
        get() = prefs.getString("theme", "black") ?: "black"
        set(value) = prefs.edit().putString("theme", value).apply()
    
    // Background image
    var backgroundImage: String?
        get() = prefs.getString("bg_image", "")
        set(value) = prefs.edit().putString("bg_image", value).apply()
    
    // Button height
    var buttonHeight: Int
        get() = prefs.getInt("button_height", 100)
        set(value) = prefs.edit().putInt("button_height", value).apply()
    
    // Quick emojis
    var quickEmojis: String
        get() = prefs.getString("quick_emojis", "😀,😂,❤️,👍,🔥,✨,🎉,💯") ?: "😀,😂,❤️,👍,🔥,✨,🎉,💯"
        set(value) = prefs.edit().putString("quick_emojis", value).apply()
    
    fun resetToDefaults() = prefs.edit().clear().apply()
}