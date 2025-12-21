package com.keybord.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.webkit.JavascriptInterface
import android.widget.Toast

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * KEYBOARD API - THE UNIVERSAL REMOTE
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * This is a standalone Kotlin class that defines the JavaScript Bridge.
 * It sends commands to FastKeyboardService via Broadcasts.
 * 
 * ARCHITECTURE:
 * ┌──────────────────┐     ┌──────────────────┐     ┌──────────────────────────┐
 * │   JavaScript     │────▶│   KeyboardAPI    │────▶│  FastKeyboardService     │
 * │   (WebView)      │     │   (Bridge)       │     │  (BroadcastReceiver)     │
 * └──────────────────┘     └──────────────────┘     └──────────────────────────┘
 *                                │
 *                                ▼
 *                          sendBroadcast()
 * 
 * ZONES:
 * - Zone A (Core): Context, broadcast sender - STABLE, rarely changes
 * - Zone B (Modules): Feature sets - Can add/remove freely
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 */
class KeyboardAPI(private val context: Context) {

    companion object {
        private const val TAG = "KeyboardAPI"
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ZONE A: CORE (Stable - Rarely Changes)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Send API command to FastKeyboardService via Broadcast
     * 
     * This is the ONLY connection point between API and Keyboard Service.
     * The service doesn't know about this class - it just receives broadcasts.
     */
    private fun sendApiCommand(command: String, data: String = "", count: Int = 1) {
        try {
            val intent = Intent(FastKeyboardService.ACTION_API_EVENT).apply {
                setPackage(context.packageName)
                putExtra(FastKeyboardService.EXTRA_COMMAND, command)
                putExtra(FastKeyboardService.EXTRA_DATA, data)
                putExtra(FastKeyboardService.EXTRA_COUNT, count)
            }
            context.sendBroadcast(intent)
            Log.d(TAG, "Sent command: $command, data=$data, count=$count")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending command: ${e.message}")
        }
    }
    
    /**
     * Notify keyboard that settings changed
     */
    private fun notifySettingsChanged() {
        try {
            val intent = Intent(KeyboardSettings.ACTION_SETTINGS_CHANGED).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying settings: ${e.message}")
        }
    }
    
    /**
     * Get Vibrator service
     */
    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    /**
     * Get Clipboard Manager
     */
    private fun getClipboard(): ClipboardManager {
        return context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    
    /**
     * Get Settings instance
     */
    private fun getSettings(): KeyboardSettings = KeyboardSettings(context)

    // ═══════════════════════════════════════════════════════════════════════════
    // ZONE B: MODULE 1 - TYPING
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Type text into current input field
     * 
     * Usage: Android.typeText("Hello")
     * 
     * If keyboard is in Sinhala mode, text will be processed through Singlish engine.
     * Otherwise, text is committed directly.
     */
    @JavascriptInterface
    fun typeText(text: String) {
        sendApiCommand(FastKeyboardService.CMD_TYPE_TEXT, text)
    }
    
    /**
     * Type a single character
     * 
     * Usage: Android.typeChar("a")
     */
    @JavascriptInterface
    fun typeChar(char: String) {
        if (char.isNotEmpty()) {
            sendApiCommand(FastKeyboardService.CMD_TYPE_TEXT, char[0].toString())
        }
    }
    
    /**
     * Delete characters (backspace)
     * 
     * Usage: Android.backspace() or Android.backspace(5)
     * 
     * @param count Number of characters to delete (default: 1)
     */
    @JavascriptInterface
    fun backspace(count: Int = 1) {
        sendApiCommand(FastKeyboardService.CMD_BACKSPACE, "", count)
    }
    
    /**
     * Single backspace
     * 
     * Usage: Android.deleteChar()
     */
    @JavascriptInterface
    fun deleteChar() {
        backspace(1)
    }
    
    /**
     * Press Enter key
     * 
     * Usage: Android.enter()
     * 
     * Behavior depends on input field type (may submit form, add newline, etc.)
     */
    @JavascriptInterface
    fun enter() {
        sendApiCommand(FastKeyboardService.CMD_ENTER)
    }
    
    /**
     * Press Space key
     * 
     * Usage: Android.space()
     */
    @JavascriptInterface
    fun space() {
        typeText(" ")
    }
    
    /**
     * Clear all text in input field
     * 
     * Usage: Android.clearAll()
     */
    @JavascriptInterface
    fun clearAll() {
        sendApiCommand(FastKeyboardService.CMD_CLEAR_ALL)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ZONE B: MODULE 2 - CURSOR CONTROL
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Move cursor left
     * 
     * Usage: Android.moveCursorLeft()
     */
    @JavascriptInterface
    fun moveCursorLeft() {
        sendApiCommand(FastKeyboardService.CMD_CURSOR_LEFT)
    }
    
    /**
     * Move cursor right
     * 
     * Usage: Android.moveCursorRight()
     */
    @JavascriptInterface
    fun moveCursorRight() {
        sendApiCommand(FastKeyboardService.CMD_CURSOR_RIGHT)
    }
    
    /**
     * Move cursor left by N positions
     * 
     * Usage: Android.cursorLeft(3)
     */
    @JavascriptInterface
    fun cursorLeft(count: Int = 1) {
        repeat(count) { moveCursorLeft() }
    }
    
    /**
     * Move cursor right by N positions
     * 
     * Usage: Android.cursorRight(3)
     */
    @JavascriptInterface
    fun cursorRight(count: Int = 1) {
        repeat(count) { moveCursorRight() }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ZONE B: MODULE 3 - DATA ACCESS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Get clipboard text
     * 
     * Usage: var text = Android.getClipboardText()
     * 
     * @return Clipboard content as string, or empty string if clipboard is empty
     */
    @JavascriptInterface
    fun getClipboardText(): String {
        return try {
            val clip = getClipboard().primaryClip
            if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0).text?.toString() ?: ""
            } else ""
        } catch (e: Exception) {
            Log.e(TAG, "Error getting clipboard: ${e.message}")
            ""
        }
    }
    
    /**
     * Copy text to clipboard
     * 
     * Usage: Android.copyToClipboard("Hello World")
     * 
     * @param text Text to copy
     */
    @JavascriptInterface
    fun copyToClipboard(text: String) {
        try {
            val clip = ClipData.newPlainText("Copied Text", text)
            getClipboard().setPrimaryClip(clip)
            Log.d(TAG, "Copied to clipboard: $text")
        } catch (e: Exception) {
            Log.e(TAG, "Error copying to clipboard: ${e.message}")
        }
    }
    
    /**
     * Paste from clipboard
     * 
     * Usage: Android.paste()
     * 
     * Gets clipboard text and types it
     */
    @JavascriptInterface
    fun paste() {
        val text = getClipboardText()
        if (text.isNotEmpty()) {
            typeText(text)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ZONE B: MODULE 4 - SYSTEM
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Vibrate device
     * 
     * Usage: Android.vibrate(50)
     * 
     * @param ms Vibration duration in milliseconds (default: 50)
     */
    @JavascriptInterface
    fun vibrate(ms: Int = 50) {
        try {
            val vibrator = getVibrator()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(
                    ms.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(ms.toLong())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating: ${e.message}")
        }
    }
    
    /**
     * Show toast message
     * 
     * Usage: Android.toast("Hello!")
     * 
     * @param message Message to show
     */
    @JavascriptInterface
    fun toast(message: String) {
        try {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing toast: ${e.message}")
        }
    }
    
    /**
     * Log message to Android Logcat
     * 
     * Usage: Android.log("Debug message")
     * 
     * @param message Message to log
     */
    @JavascriptInterface
    fun log(message: String) {
        Log.d(TAG, "JS: $message")
    }
    
    /**
     * Check if keyboard is enabled in system settings
     * 
     * Usage: var enabled = Android.isKeyboardEnabled()
     * 
     * @return true if keyboard is enabled
     */
    @JavascriptInterface
    fun isKeyboardEnabled(): Boolean {
        return try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) 
                as android.view.inputmethod.InputMethodManager
            val enabledMethods = imm.enabledInputMethodList
            enabledMethods.any { it.packageName == context.packageName }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if keyboard is currently active/selected
     * 
     * Usage: var active = Android.isKeyboardActive()
     * 
     * @return true if keyboard is active
     */
    @JavascriptInterface
    fun isKeyboardActive(): Boolean {
        return try {
            val currentIME = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            )
            currentIME?.contains(context.packageName) == true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if app can draw overlays
     * 
     * Usage: var canOverlay = Android.canDrawOverlays()
     * 
     * @return true if overlay permission is granted
     */
    @JavascriptInterface
    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ZONE B: MODULE 5 - WINDOW & NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Hide keyboard
     * 
     * Usage: Android.hideKeyboard()
     */
    @JavascriptInterface
    fun hideKeyboard() {
        sendApiCommand(FastKeyboardService.CMD_HIDE_KEYBOARD)
    }
    
    /**
     * Open keyboard settings
     * 
     * Usage: Android.openKeyboardSettings()
     */
    @JavascriptInterface
    fun openKeyboardSettings() {
        try {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening settings: ${e.message}")
        }
    }
    
    /**
     * Show keyboard picker dialog
     * 
     * Usage: Android.showKeyboardPicker()
     */
    @JavascriptInterface
    fun showKeyboardPicker() {
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) 
                as android.view.inputmethod.InputMethodManager
            imm.showInputMethodPicker()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing picker: ${e.message}")
        }
    }
    
    /**
     * Request overlay permission
     * 
     * Usage: Android.requestOverlayPermission()
     */
    @JavascriptInterface
    fun requestOverlayPermission() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting permission: ${e.message}")
        }
    }
    
    /**
     * Show floating popup window
     * 
     * Usage: Android.showFloatingWindow()
     */
    @JavascriptInterface
    fun showFloatingWindow() {
        try {
            val intent = Intent(context, PopupActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing popup: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ZONE B: MODULE 6 - SETTINGS MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Save boolean setting
     * 
     * Usage: Android.saveSettingBool("vibration", true)
     */
    @JavascriptInterface
    fun saveSettingBool(key: String, value: Boolean) {
        try {
            val settings = getSettings()
            when (key) {
                "vibration" -> settings.isVibrationEnabled = value
                "emoji_row" -> settings.isShowEmojiRow = value
                else -> {
                    val prefs = context.getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean(key, value).apply()
                }
            }
            notifySettingsChanged()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bool: ${e.message}")
        }
    }
    
    /**
     * Get boolean setting
     * 
     * Usage: var vibration = Android.getSettingBool("vibration", true)
     */
    @JavascriptInterface
    fun getSettingBool(key: String, defaultValue: Boolean): Boolean {
        return try {
            val settings = getSettings()
            when (key) {
                "vibration" -> settings.isVibrationEnabled
                "emoji_row" -> settings.isShowEmojiRow
                else -> {
                    val prefs = context.getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE)
                    prefs.getBoolean(key, defaultValue)
                }
            }
        } catch (e: Exception) {
            defaultValue
        }
    }
    
    /**
     * Save integer setting
     * 
     * Usage: Android.saveSettingInt("keyboard_height", 245)
     */
    @JavascriptInterface
    fun saveSettingInt(key: String, value: Int) {
        try {
            val settings = getSettings()
            when (key) {
                "vibration_strength" -> settings.vibrationStrength = value
                "keyboard_height" -> settings.keyboardHeight = value
                "key_radius" -> settings.keyRadius = value
                "key_text_size" -> settings.keyTextSize = value
                "key_gap" -> settings.keyGap = value
                "button_height" -> settings.buttonHeight = value
                else -> {
                    val prefs = context.getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putInt(key, value).apply()
                }
            }
            notifySettingsChanged()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving int: ${e.message}")
        }
    }
    
    /**
     * Get integer setting
     * 
     * Usage: var height = Android.getSettingInt("keyboard_height", 245)
     */
    @JavascriptInterface
    fun getSettingInt(key: String, defaultValue: Int): Int {
        return try {
            val settings = getSettings()
            when (key) {
                "vibration_strength" -> settings.vibrationStrength
                "keyboard_height" -> settings.keyboardHeight
                "key_radius" -> settings.keyRadius
                "key_text_size" -> settings.keyTextSize
                "key_gap" -> settings.keyGap
                "button_height" -> settings.buttonHeight
                else -> {
                    val prefs = context.getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE)
                    prefs.getInt(key, defaultValue)
                }
            }
        } catch (e: Exception) {
            defaultValue
        }
    }
    
    /**
     * Set keyboard background color
     * 
     * Usage: Android.setBackgroundColor("#000000")
     */
    @JavascriptInterface
    fun setBackgroundColor(color: String) {
        try {
            val settings = getSettings()
            settings.colorBackground = color
            
            // Auto-adjust key colors based on background
            if (color == "#000000" || color.startsWith("#0") || color.startsWith("#1")) {
                settings.colorKey = "#1a1a1a"
                settings.colorKeySpecial = "#0d0d0d"
                settings.colorKeySpace = "#1a1a1a"
            } else if (color == "#f5f5f5" || color == "#ffffff" || color.startsWith("#f") || color.startsWith("#e")) {
                settings.colorKey = "#ffffff"
                settings.colorKeySpecial = "#e0e0e0"
                settings.colorKeySpace = "#ffffff"
            } else {
                settings.colorKey = lightenColor(color, 0.1f)
                settings.colorKeySpecial = darkenColor(color, 0.1f)
                settings.colorKeySpace = lightenColor(color, 0.1f)
            }
            
            notifySettingsChanged()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting bg color: ${e.message}")
        }
    }
    
    /**
     * Set text color
     * 
     * Usage: Android.setTextColor("#ffffff")
     */
    @JavascriptInterface
    fun setTextColor(color: String) {
        try {
            getSettings().colorText = color
            notifySettingsChanged()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting text color: ${e.message}")
        }
    }
    
    /**
     * Set key background color
     * 
     * Usage: Android.setKeyBackgroundColor("#1a1a1a")
     */
    @JavascriptInterface
    fun setKeyBackgroundColor(color: String) {
        try {
            val settings = getSettings()
            settings.colorKey = color
            
            val c = android.graphics.Color.parseColor(color)
            val r = android.graphics.Color.red(c)
            val g = android.graphics.Color.green(c)
            val b = android.graphics.Color.blue(c)
            
            // Auto-generate special key color (darker)
            val special = String.format("#%02x%02x%02x",
                maxOf(0, r - 30),
                maxOf(0, g - 30),
                maxOf(0, b - 30)
            )
            settings.colorKeySpecial = special
            settings.colorKeySpace = color
            
            notifySettingsChanged()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting key bg color: ${e.message}")
        }
    }
    
    /**
     * Reset all settings to defaults
     * 
     * Usage: Android.resetAllSettings()
     */
    @JavascriptInterface
    fun resetAllSettings() {
        try {
            getSettings().resetToDefaults()
            notifySettingsChanged()
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting settings: ${e.message}")
        }
    }
    
    /**
     * Clear background image
     * 
     * Usage: Android.clearBackgroundImage()
     */
    @JavascriptInterface
    fun clearBackgroundImage() {
        try {
            getSettings().backgroundImage = ""
            
            // Delete file
            val bgFile = java.io.File(context.filesDir, "keyboard_bg/background.jpg")
            if (bgFile.exists()) {
                bgFile.delete()
            }
            
            notifySettingsChanged()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing bg image: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ZONE A: HELPER FUNCTIONS (Core - Stable)
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Lighten a color
     */
    private fun lightenColor(color: String, factor: Float): String {
        return try {
            val c = android.graphics.Color.parseColor(color)
            val r = minOf(255, (android.graphics.Color.red(c) + 255 * factor).toInt())
            val g = minOf(255, (android.graphics.Color.green(c) + 255 * factor).toInt())
            val b = minOf(255, (android.graphics.Color.blue(c) + 255 * factor).toInt())
            String.format("#%02x%02x%02x", r, g, b)
        } catch (e: Exception) {
            color
        }
    }
    
    /**
     * Darken a color
     */
    private fun darkenColor(color: String, factor: Float): String {
        return try {
            val c = android.graphics.Color.parseColor(color)
            val r = maxOf(0, (android.graphics.Color.red(c) - 255 * factor).toInt())
            val g = maxOf(0, (android.graphics.Color.green(c) - 255 * factor).toInt())
            val b = maxOf(0, (android.graphics.Color.blue(c) - 255 * factor).toInt())
            String.format("#%02x%02x%02x", r, g, b)
        } catch (e: Exception) {
            color
        }
    }
}