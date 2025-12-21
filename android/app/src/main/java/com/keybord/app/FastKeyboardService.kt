package com.keybord.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import java.io.File

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * ULTRA FAST KEYBOARD SERVICE
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * OPTIMIZATIONS:
 * - ZERO debounce for character keys
 * - Minimal debounce for action keys only
 * - Inline processing without Handler delays
 * - Efficient touch detection
 * - No unnecessary rebuilds
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 */
class FastKeyboardService : InputMethodService() {

    companion object {
        private const val TAG = "FastKeyboard"
        
        // Sinhala Characters
        private const val HAL = "්"
        private const val YANSAYA = "්‍ය"
        private const val RAKARANSAYA = "්‍ර"
        
        // API Broadcast
        const val ACTION_API_EVENT = "com.keybord.app.API_EVENT"
        const val EXTRA_COMMAND = "command"
        const val EXTRA_DATA = "data"
        const val EXTRA_COUNT = "count"
        
        const val CMD_TYPE_TEXT = "TYPE_TEXT"
        const val CMD_BACKSPACE = "BACKSPACE"
        const val CMD_ENTER = "ENTER"
        const val CMD_CURSOR_LEFT = "CURSOR_LEFT"
        const val CMD_CURSOR_RIGHT = "CURSOR_RIGHT"
        const val CMD_CLEAR_ALL = "CLEAR_ALL"
        const val CMD_VIBRATE = "VIBRATE"
        const val CMD_HIDE_KEYBOARD = "HIDE_KEYBOARD"
        
        // ULTRA FAST - Minimal delays
        private const val ACTION_KEY_DEBOUNCE = 150L  // Only for shift/mode keys
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // KEY INFO
    // ═══════════════════════════════════════════════════════════════════════════
    
    data class KeyInfo(
        val key: String,
        val view: View,
        var bounds: Rect = Rect()
    ) {
        fun updateBounds() {
            val loc = IntArray(2)
            view.getLocationOnScreen(loc)
            bounds.set(loc[0], loc[1], loc[0] + view.width, loc[1] + view.height)
        }
        
        fun contains(x: Float, y: Float): Boolean = 
            bounds.contains(x.toInt(), y.toInt())
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SETTINGS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private var colorBackground = "#000000"
    private var colorKeyNormal = "#1a1a1a"
    private var colorKeySpecial = "#0d0d0d"
    private var colorKeyEnter = "#2563eb"
    private var colorKeySpace = "#1a1a1a"
    private var colorText = "#ffffff"
    
    private var keyboardHeight = 245
    private var keyRadius = 8
    private var keyGap = 2
    private var keyTextSize = 20
    
    private var vibrateEnabled = true
    private var vibrateDuration = 5
    private var showEmojiRow = false
    private var longPressDelay = 250
    private var repeatInterval = 25

    // ═══════════════════════════════════════════════════════════════════════════
    // KEYBOARD LAYOUTS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private val layoutLetters = arrayOf(
        arrayOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        arrayOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        arrayOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫"),
        arrayOf("123", "🌐", ",", "SPACE", ".", "✨", "↵")
    )
    
    private val layoutNumbers = arrayOf(
        arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        arrayOf("@", "#", "$", "%", "&", "-", "+", "(", ")"),
        arrayOf("#+=", "*", "\"", "'", ":", ";", "!", "?", "⌫"),
        arrayOf("ABC", "🌐", ",", "SPACE", ".", "✨", "↵")
    )
    
    private val layoutSymbols = arrayOf(
        arrayOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆"),
        arrayOf("£", "€", "¥", "^", "°", "=", "{", "}", "\\"),
        arrayOf("123", "©", "®", "™", "✓", "[", "]", "<", "⌫"),
        arrayOf("ABC", "🌐", ",", "SPACE", ".", "✨", "↵")
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // SINHALA LABELS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private val sinhalaLabels = mapOf(
        "a" to "අ", "b" to "බ", "c" to "ච", "d" to "ඩ", "e" to "එ",
        "f" to "ෆ", "g" to "ග", "h" to "හ", "i" to "ඉ", "j" to "ජ",
        "k" to "ක", "l" to "ල", "m" to "ම", "n" to "න", "o" to "ඔ",
        "p" to "ප", "q" to "ක", "r" to "ර", "s" to "ස", "t" to "ට",
        "u" to "උ", "v" to "ව", "w" to "ව", "x" to "ං", "y" to "ය",
        "z" to "ඤ"
    )
    
    private val sinhalaLabelsShift = mapOf(
        "a" to "ඇ", "b" to "භ", "c" to "ඡ", "d" to "ඪ", "e" to "ඓ",
        "f" to "ෆ", "g" to "ඝ", "h" to "ඃ", "i" to "ඊ", "j" to "ඣ",
        "k" to "ඛ", "l" to "ළ", "m" to "ඹ", "n" to "ණ", "o" to "ඕ",
        "p" to "ඵ", "q" to "ඛ", "r" to "ර", "s" to "ෂ", "t" to "ඨ",
        "u" to "ඌ", "v" to "ව", "w" to "ව", "x" to "ඞ", "y" to "ය",
        "z" to "ඥ"
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // SINGLISH ENGINE - FIXED CORRECT MAPPINGS
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Priority 1: 4-5 letter special combos
    private val consonantsSpecial = mapOf(
        "zdha" to "ඳ", "zja" to "ඦ", "zda" to "ඬ", "zga" to "ඟ",
        "zdh" to "ඳ", "zqa" to "ඳ", "zka" to "ඤ", "zha" to "ඥ",
        "ksha" to "ක්ෂ", "ksh" to "ක්ෂ", "thth" to "ත්ථ",
        "nDh" to "ඳ", "ngh" to "ඟ"
    )
    
    // Priority 2: 3 letter
    private val consonants3 = mapOf(
        "Sha" to "ෂ", "Cha" to "ඡ", "Tha" to "ථ", "Dha" to "ධ",
        "kha" to "ඛ", "gha" to "ඝ", "pha" to "ඵ", "bha" to "භ",
        "sha" to "ශ", "ruu" to "ඎ"
    )
    
    // Priority 3: 2 letter consonants
    private val consonants2 = mapOf(
        "kh" to "ඛ", "gh" to "ඝ", "ch" to "ච", "Ch" to "ඡ",
        "jh" to "ඣ", "Ja" to "ඣ", "th" to "ත", "Th" to "ථ",
        "dh" to "ද", "Dh" to "ධ", "ph" to "ඵ", "bh" to "භ",
        "sh" to "ශ", "Sh" to "ෂ", "Ta" to "ඨ", "Da" to "ඪ",
        "Na" to "ණ", "La" to "ළ", "Lu" to "ළු", "Ba" to "ඹ",
        "zb" to "ඹ", "zn" to "ං"
    )
    
    // Priority 4: 1 letter consonants
    private val consonants1 = mapOf(
        "k" to "ක", "g" to "ග", "c" to "ච", "j" to "ජ",
        "t" to "ට", "d" to "ඩ", "n" to "න", "p" to "ප",
        "b" to "බ", "m" to "ම", "y" to "ය", "r" to "ර",
        "l" to "ල", "w" to "ව", "v" to "ව", "s" to "ස",
        "h" to "හ", "f" to "ෆ", "z" to "ඤ", "q" to "ක",
        "K" to "ඛ", "G" to "ඝ", "C" to "ඡ", "J" to "ඣ",
        "T" to "ඨ", "D" to "ඪ", "N" to "ණ", "P" to "ඵ",
        "B" to "භ", "L" to "ළ", "S" to "ෂ", "F" to "ෆ",
        "Z" to "ඥ", "Q" to "ඛ"
    )
    
    // Special (no hal)
    private val specialConsonants = mapOf(
        "x" to "ං", "X" to "ඞ", "H" to "ඃ"
    )
    
    // Standalone vowels
    private val vowelsStandalone = mapOf(
        "ruu" to "ඎ", "aa" to "ආ", "Aa" to "ඈ", "AA" to "ඈ",
        "ae" to "ඇ", "Ae" to "ඈ", "ii" to "ඊ", "uu" to "ඌ",
        "ee" to "ඒ", "ei" to "ඒ", "oo" to "ඕ", "oe" to "ඕ",
        "au" to "ඖ", "ai" to "ඓ", "ru" to "ඍ",
        "a" to "අ", "A" to "ඇ", "i" to "ඉ", "I" to "ඊ",
        "u" to "උ", "U" to "ඌ", "e" to "එ", "E" to "ඓ",
        "o" to "ඔ", "O" to "ඕ"
    )
    
    // Vowel modifiers (pilla)
    private val vowelModifiers = mapOf(
        "ruu" to "ෲ", "aa" to "ා", "Aa" to "ෑ", "AA" to "ෑ",
        "ae" to "ැ", "Ae" to "ෑ", "ii" to "ී", "uu" to "ූ",
        "ee" to "ේ", "ei" to "ේ", "oo" to "ෝ", "oe" to "ෝ",
        "au" to "ෞ", "ai" to "ෛ", "ru" to "ෘ",
        "a" to "", "A" to "ැ", "i" to "ි", "I" to "ී",
        "u" to "ු", "U" to "ූ", "e" to "ෙ", "E" to "ෛ",
        "o" to "ො", "O" to "ෝ"
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private var rootContainer: FrameLayout? = null
    private var keyboardContainer: LinearLayout? = null
    private var keyboardView: LinearLayout? = null
    private var backgroundImageView: ImageView? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private val settings by lazy { KeyboardSettings(this) }
    
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    private var previewPopup: PopupWindow? = null
    private var previewText: TextView? = null
    
    // State
    private var isShift = false
    private var isCaps = false
    private var isNumbers = false
    private var isSymbols = false
    private var isSinhalaMode = false
    
    // Repeat
    private var isRepeating = false
    private var repeatRunnable: Runnable? = null
    
    // Singlish buffer
    private val englishBuffer = StringBuilder()
    private var currentSinhalaLength = 0
    
    // Touch - ULTRA FAST
    private val keyInfoList = mutableListOf<KeyInfo>()
    private var currentPressedKey: KeyInfo? = null
    private var lastActionKeyTime = 0L
    
    private var navigationBarHeight = 0

    // ═══════════════════════════════════════════════════════════════════════════
    // RECEIVERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private val settingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == KeyboardSettings.ACTION_SETTINGS_CHANGED) {
                handler.post { loadSettings(); rebuildKeyboard() }
            }
        }
    }
    
    private val apiEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_API_EVENT) return
            val command = intent.getStringExtra(EXTRA_COMMAND) ?: return
            val data = intent.getStringExtra(EXTRA_DATA) ?: ""
            val count = intent.getIntExtra(EXTRA_COUNT, 1)
            executeApiCommand(command, data, count)
        }
    }
    
    private fun executeApiCommand(command: String, data: String, count: Int) {
        val ic = currentInputConnection ?: return
        when (command) {
            CMD_TYPE_TEXT -> {
                if (isSinhalaMode && data.all { it.isLetter() }) {
                    data.forEach { processSinglish(ic, it) }
                } else {
                    flushBuffer(); ic.commitText(data, 1)
                }
            }
            CMD_BACKSPACE -> repeat(count) { handleBackspace(ic) }
            CMD_ENTER -> { flushBuffer(); handleEnter(ic) }
            CMD_CURSOR_LEFT -> { flushBuffer(); moveCursor(ic, -1) }
            CMD_CURSOR_RIGHT -> { flushBuffer(); moveCursor(ic, 1) }
            CMD_CLEAR_ALL -> { flushBuffer(); ic.performContextMenuAction(android.R.id.selectAll); ic.commitText("", 1) }
            CMD_VIBRATE -> vibrateMs(data.toIntOrNull() ?: 50)
            CMD_HIDE_KEYBOARD -> requestHideSelf(0)
        }
    }
    
    private fun moveCursor(ic: InputConnection, dir: Int) {
        val before = ic.getTextBeforeCursor(1000, 0)?.length ?: 0
        val after = ic.getTextAfterCursor(1000, 0)?.length ?: 0
        val newPos = if (dir < 0) maxOf(0, before - 1) else minOf(before + after, before + 1)
        ic.setSelection(newPos, newPos)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════
    
    override fun onCreate() {
        super.onCreate()
        loadSettings()
        calculateNavBarHeight()
        initPreviewPopup()
        
        val filter1 = IntentFilter(KeyboardSettings.ACTION_SETTINGS_CHANGED)
        val filter2 = IntentFilter(ACTION_API_EVENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(settingsReceiver, filter1, Context.RECEIVER_NOT_EXPORTED)
            registerReceiver(apiEventReceiver, filter2, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(settingsReceiver, filter1)
            registerReceiver(apiEventReceiver, filter2)
        }
    }
    
    override fun onDestroy() {
        stopRepeat()
        hidePreview()
        try { unregisterReceiver(settingsReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(apiEventReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }
    
    private fun calculateNavBarHeight() {
        navigationBarHeight = try {
            val id = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (id > 0) resources.getDimensionPixelSize(id) else dp(48)
        } catch (_: Exception) { dp(48) }
    }
    
    private fun initPreviewPopup() {
        previewText = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(14), dp(10), dp(14))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#424242"))
                cornerRadius = dp(8).toFloat()
            }
        }
        previewPopup = PopupWindow(previewText, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            isClippingEnabled = false
            isTouchable = false
        }
    }
    
    private fun loadSettings() {
        colorBackground = settings.colorBackground
        colorKeyNormal = settings.colorKey
        colorKeySpecial = settings.colorKeySpecial
        colorKeyEnter = settings.colorKeyEnter
        colorKeySpace = settings.colorKeySpace
        colorText = settings.colorText
        keyboardHeight = settings.keyboardHeight
        keyRadius = settings.keyRadius
        keyGap = settings.keyGap
        keyTextSize = settings.keyTextSize
        vibrateEnabled = settings.isVibrationEnabled
        vibrateDuration = settings.vibrationStrength
        showEmojiRow = settings.isShowEmojiRow
        longPressDelay = settings.longPressDelay
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INPUT VIEW
    // ═══════════════════════════════════════════════════════════════════════════
    
    override fun onCreateInputView(): View {
        loadSettings()
        
        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(parseColor(colorBackground))
        }
        
        backgroundImageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(-1, -1)
        }
        rootContainer?.addView(backgroundImageView)
        loadBackgroundImage()
        
        keyboardContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (showEmojiRow) keyboardContainer?.addView(createEmojiRow())
        keyboardView = createKeyboard()
        keyboardContainer?.addView(keyboardView)
        
        rootContainer?.addView(keyboardContainer, FrameLayout.LayoutParams(-1, -2).apply { gravity = Gravity.BOTTOM })
        
        // Touch layer - ULTRA FAST
        rootContainer?.addView(View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = FrameLayout.LayoutParams(-1, -1)
            setOnTouchListener { _, event -> handleTouch(event) }
        })
        
        val emojiH = if (showEmojiRow) dp(44) else 0
        rootContainer?.layoutParams = ViewGroup.LayoutParams(-1, emojiH + dp(keyboardHeight) + navigationBarHeight)
        rootContainer?.setPadding(0, 0, 0, navigationBarHeight)
        rootContainer?.post { updateKeyBounds() }
        
        return rootContainer!!
    }
    
    private fun loadBackgroundImage() {
        try {
            val path = settings.backgroundImage
            if (!path.isNullOrEmpty()) {
                val file = File(path)
                if (file.exists()) {
                    BitmapFactory.decodeFile(path)?.let {
                        backgroundImageView?.setImageBitmap(it)
                        backgroundImageView?.alpha = 0.3f
                        return
                    }
                }
            }
            backgroundImageView?.setImageBitmap(null)
        } catch (_: Exception) { backgroundImageView?.setImageBitmap(null) }
    }
    
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        isShift = false; isCaps = false; isSymbols = false
        clearBuffer()
        info?.let {
            val cls = it.inputType and EditorInfo.TYPE_MASK_CLASS
            isNumbers = (cls == EditorInfo.TYPE_CLASS_NUMBER || cls == EditorInfo.TYPE_CLASS_PHONE)
        }
        rebuildKeyboard()
    }
    
    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        hidePreview(); clearBuffer()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ULTRA FAST TOUCH HANDLING
    // ═══════════════════════════════════════════════════════════════════════════
    
    private fun updateKeyBounds() { keyInfoList.forEach { it.updateBounds() } }
    
    private fun findKey(x: Float, y: Float): KeyInfo? = keyInfoList.find { it.contains(x, y) }
    
    private fun handleTouch(event: MotionEvent): Boolean {
        val x = event.rawX
        val y = event.rawY
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                findKey(x, y)?.let { key ->
                    currentPressedKey = key
                    applyPress(key)
                    showPreview(key)
                    if (key.key == "⌫") startRepeat(key.key)
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                findKey(x, y)?.let { newKey ->
                    if (newKey != currentPressedKey) {
                        currentPressedKey?.let { resetPress(it) }
                        currentPressedKey = newKey
                        applyPress(newKey)
                        showPreview(newKey)
                        stopRepeat()
                        if (newKey.key == "⌫") startRepeat(newKey.key)
                    }
                }
            }
            
            MotionEvent.ACTION_UP -> {
                hidePreview()
                stopRepeat()
                currentPressedKey?.let { key ->
                    // ULTRA FAST - No debounce for regular keys
                    if (isActionKey(key.key)) {
                        val now = System.currentTimeMillis()
                        if (now - lastActionKeyTime >= ACTION_KEY_DEBOUNCE) {
                            lastActionKeyTime = now
                            vibrate()
                            processKey(key.key)
                        }
                    } else {
                        vibrate()
                        processKey(key.key)
                    }
                    resetPress(key)
                }
                currentPressedKey = null
            }
            
            MotionEvent.ACTION_CANCEL -> {
                hidePreview(); stopRepeat()
                currentPressedKey?.let { resetPress(it) }
                currentPressedKey = null
            }
        }
        return true
    }
    
    private fun isActionKey(key: String) = key in listOf("⇧", "123", "ABC", "#+=", "🌐")
    private fun applyPress(ki: KeyInfo) { ki.view.alpha = 0.6f; ki.view.scaleX = 0.95f; ki.view.scaleY = 0.95f }
    private fun resetPress(ki: KeyInfo) { ki.view.alpha = 1f; ki.view.scaleX = 1f; ki.view.scaleY = 1f }

    // ═══════════════════════════════════════════════════════════════════════════
    // PREVIEW
    // ═══════════════════════════════════════════════════════════════════════════
    
    private fun showPreview(ki: KeyInfo) {
        if (isSpecialKey(ki.key)) { hidePreview(); return }
        
        val text = if (isSinhalaMode && ki.key.length == 1 && ki.key[0].isLetter()) {
            (if (isShift || isCaps) sinhalaLabelsShift else sinhalaLabels)[ki.key.lowercase()] ?: ki.key
        } else if (ki.key.length == 1 && ki.key[0].isLetter()) {
            if (isShift || isCaps) ki.key.uppercase() else ki.key.lowercase()
        } else ki.key
        
        if (text.isEmpty() || text == "SPACE") { hidePreview(); return }
        
        previewText?.text = text
        previewText?.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        
        val loc = IntArray(2); ki.view.getLocationOnScreen(loc)
        val pw = maxOf(ki.view.width, (previewText?.measuredWidth ?: 0) + dp(16))
        val ph = (ki.view.height * 1.5f).toInt()
        var px = loc[0] + (ki.view.width - pw) / 2
        val py = loc[1] - ph - dp(6)
        
        val sw = resources.displayMetrics.widthPixels
        px = px.coerceIn(dp(4), sw - pw - dp(4))
        
        try {
            if (previewPopup?.isShowing == true) previewPopup?.update(px, py, pw, ph)
            else { previewPopup?.width = pw; previewPopup?.height = ph; previewPopup?.showAtLocation(rootContainer, Gravity.NO_GRAVITY, px, py) }
        } catch (_: Exception) {}
    }
    
    private fun hidePreview() { try { if (previewPopup?.isShowing == true) previewPopup?.dismiss() } catch (_: Exception) {} }

    // ═══════════════════════════════════════════════════════════════════════════
    // KEYBOARD BUILD
    // ═══════════════════════════════════════════════════════════════════════════
    
    private fun createEmojiRow(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(-1, dp(44))
            setPadding(dp(4), dp(4), dp(4), dp(4))
            settings.quickEmojis.split(",").forEach { emoji ->
                val e = emoji.trim()
                if (e.isNotEmpty()) addView(TextView(this@FastKeyboardService).apply {
                    text = e; this.gravity = Gravity.CENTER
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    layoutParams = LinearLayout.LayoutParams(0, -1, 1f).apply { setMargins(dp(2), 0, dp(2), 0) }
                    background = GradientDrawable().apply { setColor(parseColor(colorKeyNormal)); cornerRadius = dp(8).toFloat() }
                    setOnClickListener { vibrate(); flushBuffer(); currentInputConnection?.commitText(e, 1) }
                })
            }
        }
    }
    
    private fun createKeyboard(): LinearLayout {
        keyInfoList.clear()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, dp(keyboardHeight))
            setPadding(dp(3), dp(6), dp(3), dp(6))
            val layout = when { isSymbols -> layoutSymbols; isNumbers -> layoutNumbers; else -> layoutLetters }
            layout.forEachIndexed { idx, row -> addView(createRow(row, idx)) }
        }
    }
    
    private fun createRow(keys: Array<String>, rowIdx: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            setPadding(if (rowIdx == 1) dp(14) else 0, dp(2), if (rowIdx == 1) dp(14) else 0, dp(2))
            keys.forEach { addView(createKey(it)) }
        }
    }
    
    private fun createKey(key: String): View {
        val container = FrameLayout(this)
        val tv = TextView(this).apply { gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD; includeFontPadding = false }
        
        val (display, textColor, textSize) = getKeyConfig(key)
        tv.text = display; tv.setTextColor(textColor); tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
        
        if (isSinhalaMode && key.length == 1 && key[0].isLetter()) {
            (if (isShift || isCaps) sinhalaLabelsShift else sinhalaLabels)[key.lowercase()]?.let { lbl ->
                container.addView(TextView(this).apply {
                    text = lbl; setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f); setTextColor(Color.parseColor("#888888"))
                    layoutParams = FrameLayout.LayoutParams(-2, -2).apply { gravity = Gravity.TOP or Gravity.END; setMargins(0, dp(2), dp(3), 0) }
                })
            }
        }
        
        container.layoutParams = LinearLayout.LayoutParams(0, -1, getWeight(key)).apply { setMargins(dp(keyGap), dp(keyGap), dp(keyGap), dp(keyGap)) }
        container.addView(tv, FrameLayout.LayoutParams(-1, -1))
        container.background = createKeyBg(key)
        keyInfoList.add(KeyInfo(key, container))
        return container
    }
    
    private fun getKeyConfig(key: String): Triple<String, Int, Float> {
        val def = parseColor(colorText)
        return when (key) {
            "↵" -> Triple("↵", Color.WHITE, 22f)
            "⇧" -> when { isCaps -> Triple("⇪", Color.parseColor("#10b981"), 24f); isShift -> Triple("⬆", Color.parseColor("#3b82f6"), 24f); else -> Triple("⇧", def, 24f) }
            "⌫" -> Triple("⌫", def, 22f)
            "SPACE" -> Triple(if (isSinhalaMode) "සිංහල" else "English", Color.parseColor("#666666"), 11f)
            "🌐" -> Triple(if (isSinhalaMode) "සිං" else "EN", if (isSinhalaMode) Color.parseColor("#10b981") else Color.parseColor("#3b82f6"), 12f)
            "✨" -> Triple("✨", def, 18f)
            "123", "ABC", "#+=" -> Triple(key, def, 13f)
            else -> Triple(if (key.length == 1 && key[0].isLetter()) (if (isShift || isCaps) key.uppercase() else key.lowercase()) else key, def, keyTextSize.toFloat())
        }
    }
    
    private fun isSpecialKey(key: String) = key in listOf("⇧", "⌫", "↵", "SPACE", "123", "ABC", "#+=", "🌐", "✨")
    private fun getWeight(key: String) = when (key) { "SPACE" -> 3.5f; "⇧", "⌫" -> 1.5f; "↵", "123", "ABC", "#+=" -> 1.3f; else -> 1f }
    
    private fun createKeyBg(key: String): GradientDrawable {
        val color = when (key) {
            "↵" -> colorKeyEnter
            "⇧" -> when { isCaps -> "#10b981"; isShift -> "#3b82f6"; else -> colorKeySpecial }
            "⌫", "123", "ABC", "#+=", "🌐", "✨" -> colorKeySpecial
            "SPACE" -> colorKeySpace
            else -> colorKeyNormal
        }
        return GradientDrawable().apply { setColor(parseColor(color)); cornerRadius = dp(keyRadius).toFloat() }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // KEY PROCESSING
    // ═══════════════════════════════════════════════════════════════════════════
    
    private fun processKey(key: String) {
        val ic = currentInputConnection ?: return
        when (key) {
            "⇧" -> handleShift()
            "⌫" -> handleBackspace(ic)
            "↵" -> { flushBuffer(); handleEnter(ic) }
            "SPACE" -> { flushBuffer(); ic.commitText(" ", 1) }
            ",", "." -> { flushBuffer(); ic.commitText(key, 1) }
            "123" -> { flushBuffer(); isNumbers = true; isSymbols = false; rebuildKeyboard() }
            "ABC" -> { flushBuffer(); isNumbers = false; isSymbols = false; rebuildKeyboard() }
            "#+=" -> { flushBuffer(); isSymbols = true; rebuildKeyboard() }
            "🌐" -> { flushBuffer(); isSinhalaMode = !isSinhalaMode; rebuildKeyboard() }
            "✨" -> { flushBuffer(); openPopup() }
            else -> handleChar(ic, key)
        }
    }
    
    private fun handleShift() {
        when { isCaps -> { isCaps = false; isShift = false }; isShift -> isCaps = true; else -> isShift = true }
        rebuildKeyboard()
    }
    
    private fun handleBackspace(ic: InputConnection) {
        if (englishBuffer.isNotEmpty()) {
            if (currentSinhalaLength > 0) ic.deleteSurroundingText(currentSinhalaLength, 0)
            englishBuffer.deleteCharAt(englishBuffer.length - 1)
            if (englishBuffer.isNotEmpty()) {
                val s = convertToSinhala(englishBuffer.toString())
                ic.commitText(s, 1); currentSinhalaLength = s.length
            } else currentSinhalaLength = 0
        } else ic.deleteSurroundingText(1, 0)
    }
    
    private fun handleEnter(ic: InputConnection) {
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        if (action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED) ic.commitText("\n", 1)
        else ic.performEditorAction(action)
    }
    
    private fun handleChar(ic: InputConnection, key: String) {
        var c = key[0]
        if ((isShift || isCaps) && c.isLetter()) c = c.uppercaseChar()
        
        if (isSinhalaMode && c.isLetter()) processSinglish(ic, c)
        else { flushBuffer(); ic.commitText(c.toString(), 1) }
        
        if (isShift && !isCaps) { isShift = false; rebuildKeyboard() }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SINGLISH ENGINE - FIXED
    // ═══════════════════════════════════════════════════════════════════════════
    
    private fun processSinglish(ic: InputConnection, c: Char) {
        if (currentSinhalaLength > 0) ic.deleteSurroundingText(currentSinhalaLength, 0)
        englishBuffer.append(c)
        val s = convertToSinhala(englishBuffer.toString())
        ic.commitText(s, 1)
        currentSinhalaLength = s.length
    }
    
    private fun convertToSinhala(english: String): String {
        val result = StringBuilder()
        var i = 0
        var lastWasConsonant = false
        
        while (i < english.length) {
            var matched: String? = null
            var matchLen = 0
            var isConsonant = false
            var needsHal = false
            
            // Priority 1: Special (4-5 chars)
            for (len in minOf(5, english.length - i) downTo 3) {
                val sub = english.substring(i, i + len)
                consonantsSpecial[sub]?.let { matched = it; matchLen = len; isConsonant = true; needsHal = !sub.endsWith("a") }
                if (matched != null) break
            }
            
            // Priority 2: 3 letter
            if (matched == null && i + 3 <= english.length) {
                val sub = english.substring(i, i + 3)
                consonants3[sub]?.let { matched = it; matchLen = 3; isConsonant = true; needsHal = !sub.endsWith("a") }
                if (matched == null && lastWasConsonant) vowelModifiers[sub]?.let {
                    if (result.isNotEmpty() && result.endsWith(HAL)) result.deleteCharAt(result.length - 1)
                    matched = it; matchLen = 3; isConsonant = false; lastWasConsonant = false
                }
                if (matched == null && !lastWasConsonant) vowelsStandalone[sub]?.let { matched = it; matchLen = 3; isConsonant = false }
            }
            
            // Priority 3: 2 letter
            if (matched == null && i + 2 <= english.length) {
                val sub = english.substring(i, i + 2)
                consonants2[sub]?.let { matched = it; matchLen = 2; isConsonant = true; needsHal = sub !in listOf("Lu", "zn", "zb") && !sub.endsWith("a") }
                if (matched == null && lastWasConsonant) vowelModifiers[sub]?.let {
                    if (result.isNotEmpty() && result.endsWith(HAL)) result.deleteCharAt(result.length - 1)
                    matched = it; matchLen = 2; isConsonant = false; lastWasConsonant = false
                }
                if (matched == null && !lastWasConsonant) vowelsStandalone[sub]?.let { matched = it; matchLen = 2; isConsonant = false }
            }
            
            // Priority 4: 1 letter
            if (matched == null && i < english.length) {
                val sub = english.substring(i, i + 1)
                val ch = sub[0]
                
                specialConsonants[sub]?.let { matched = it; matchLen = 1; isConsonant = false }
                if (matched == null) consonants1[sub]?.let { matched = it; matchLen = 1; isConsonant = true; needsHal = true }
                if (matched == null && lastWasConsonant) vowelModifiers[sub]?.let {
                    if (result.isNotEmpty() && result.endsWith(HAL)) result.deleteCharAt(result.length - 1)
                    matched = it; matchLen = 1; isConsonant = false; lastWasConsonant = false
                }
                if (matched == null && !lastWasConsonant) vowelsStandalone[sub]?.let { matched = it; matchLen = 1; isConsonant = false }
                
                // Yansaya
                if (matched == null && (ch == 'y' || ch == 'Y') && lastWasConsonant) {
                    if (result.isNotEmpty() && result.endsWith(HAL)) { result.deleteCharAt(result.length - 1); result.append(YANSAYA) }
                    matchLen = 1; lastWasConsonant = false; i += matchLen; continue
                }
                
                // Rakaransaya
                if (matched == null && (ch == 'r' || ch == 'R') && lastWasConsonant && i + 1 < english.length && english[i + 1] in "aeiouAEIOU") {
                    if (result.isNotEmpty() && result.endsWith(HAL)) { result.deleteCharAt(result.length - 1); result.append(RAKARANSAYA) }
                    matchLen = 1; lastWasConsonant = true; i += matchLen; continue
                }
            }
            
            if (matched != null) {
                result.append(matched)
                if (isConsonant && needsHal) { result.append(HAL); lastWasConsonant = true }
                else if (!isConsonant) lastWasConsonant = false
                else lastWasConsonant = isConsonant
                i += matchLen
            } else { result.append(english[i]); lastWasConsonant = false; i++ }
        }
        return result.toString()
    }
    
    private fun flushBuffer() { englishBuffer.clear(); currentSinhalaLength = 0 }
    private fun clearBuffer() { englishBuffer.clear(); currentSinhalaLength = 0 }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private fun startRepeat(key: String) {
        isRepeating = true
        repeatRunnable = object : Runnable {
            override fun run() {
                if (isRepeating) { processKey(key); vibrate(); handler.postDelayed(this, repeatInterval.toLong()) }
            }
        }
        handler.postDelayed(repeatRunnable!!, longPressDelay.toLong())
    }
    
    private fun stopRepeat() { isRepeating = false; repeatRunnable?.let { handler.removeCallbacks(it) } }
    
    private fun vibrate() { if (vibrateEnabled) vibrateMs(vibrateDuration) }
    
    private fun vibrateMs(ms: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator?.vibrate(VibrationEffect.createOneShot(ms.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION") vibrator?.vibrate(ms.toLong())
        } catch (_: Exception) {}
    }
    
    private fun openPopup() {
        try { startActivity(Intent(this, PopupActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
    }
    
    private fun rebuildKeyboard() {
        rootContainer ?: return
        hidePreview(); keyInfoList.clear()
        keyboardContainer?.let { rootContainer?.removeView(it) }
        loadBackgroundImage()
        keyboardContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (showEmojiRow) keyboardContainer?.addView(createEmojiRow())
        keyboardView = createKeyboard(); keyboardContainer?.addView(keyboardView)
        rootContainer?.addView(keyboardContainer, 1, FrameLayout.LayoutParams(-1, -2).apply { gravity = Gravity.BOTTOM })
        val emojiH = if (showEmojiRow) dp(44) else 0
        rootContainer?.layoutParams?.height = emojiH + dp(keyboardHeight) + navigationBarHeight
        rootContainer?.setPadding(0, 0, 0, navigationBarHeight)
        rootContainer?.post { updateKeyBounds() }
    }
    
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun parseColor(c: String) = try { Color.parseColor(c) } catch (_: Exception) { Color.BLACK }
}