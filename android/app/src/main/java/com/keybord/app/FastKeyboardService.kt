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

class FastKeyboardService : InputMethodService() {

    companion object {
        private const val TAG = "FastKeyboard"
        private const val HAL = "්"
        private const val YANSAYA = "්‍ය"
        private const val RAKARANSAYA = "්‍ර"
        
        // Debounce time in milliseconds
        private const val DEBOUNCE_TIME = 100L
        private const val ACTION_KEY_DEBOUNCE = 250L
    }

    // ═══════════════════════════════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════════════════════════════
    
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
        
        fun contains(x: Float, y: Float): Boolean = bounds.contains(x.toInt(), y.toInt())
        
        fun distanceTo(x: Float, y: Float): Float {
            val cx = bounds.centerX().toFloat()
            val cy = bounds.centerY().toFloat()
            return kotlin.math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy))
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // SETTINGS
    // ═══════════════════════════════════════════════════════════════════
    
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
    private var longPressDelay = 300
    private var repeatInterval = 30

    // ═══════════════════════════════════════════════════════════════════
    // KEYBOARD LAYOUTS
    // ═══════════════════════════════════════════════════════════════════
    
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

    // ═══════════════════════════════════════════════════════════════════
    // SINHALA KEY LABELS
    // ═══════════════════════════════════════════════════════════════════
    
    private val sinhalaLabels = mapOf(
        "a" to "අ", "b" to "බ", "c" to "ච", "d" to "ඩ", "e" to "එ",
        "f" to "ෆ", "g" to "ග", "h" to "හ", "i" to "ඉ", "j" to "ජ",
        "k" to "ක", "l" to "ල", "m" to "ම", "n" to "න", "o" to "ඔ",
        "p" to "ප", "q" to "ෘ", "r" to "ර", "s" to "ස", "t" to "ට",
        "u" to "උ", "v" to "ව", "w" to "ව", "x" to "ං", "y" to "ය",
        "z" to "ඤ"
    )
    
    private val sinhalaLabelsShift = mapOf(
        "a" to "ඇ", "b" to "භ", "c" to "ඡ", "d" to "ඪ", "e" to "ඓ",
        "f" to "ෆ", "g" to "ඝ", "h" to "ඃ", "i" to "ඊ", "j" to "ඣ",
        "k" to "ඛ", "l" to "ළ", "m" to "ඹ", "n" to "ණ", "o" to "ඕ",
        "p" to "ඵ", "q" to "ඎ", "r" to "ර", "s" to "ෂ", "t" to "ඨ",
        "u" to "ඌ", "v" to "ව", "w" to "ව", "x" to "ඞ", "y" to "ය",
        "z" to "ඥ"
    )

    // ═══════════════════════════════════════════════════════════════════
    // SINGLISH MAPPINGS - STRICT PRIORITY ORDER
    // ═══════════════════════════════════════════════════════════════════
    
    // PRIORITY 1: Special Z-Combinations (4-5 letters) - CHECK FIRST!
    private val consonantsSpecial = mapOf(
        "zdha" to "ඳ",    // MUST use zdha, NOT ndha
        "zga" to "ඟ",
        "zda" to "ඬ",
        "zja" to "ඦ",
        "zka" to "ඤ",
        "zha" to "ඥ",
        "thth" to "ත්ථ",
        "ksha" to "ක්ෂ",
        "ksh" to "ක්ෂ",
        "Ksh" to "ක්ෂ"
    )
    
    // PRIORITY 2: 3-Letter Consonants
    private val consonants3 = mapOf(
        "nDh" to "ඳ",
        "ngh" to "ඟ",
        "Sha" to "ෂ"
    )
    
    // PRIORITY 3: 2-Letter Consonants
    private val consonants2 = mapOf(
        "th" to "ත", "Th" to "ථ",
        "dh" to "ද", "Dh" to "ධ",
        "sh" to "ශ", "Sh" to "ෂ",
        "ch" to "ච", "Ch" to "ඡ",
        "kh" to "ඛ", "Kh" to "ඛ",
        "gh" to "ඝ", "Gh" to "ඝ",
        "ph" to "ඵ", "Ph" to "ඵ",
        "bh" to "භ", "Bh" to "භ",
        "jh" to "ඣ", "Jh" to "ඣ",
        "mb" to "ඹ", "Mb" to "ඹ",
        "ng" to "ඟ", "Ng" to "ඟ",
        "nd" to "ඳ", "Nd" to "ඳ",
        "ny" to "ඤ", "Ny" to "ඤ",
        "kn" to "ඤ", "Kn" to "ඤ",
        "gn" to "ඥ", "Gn" to "ඥ",
        "zk" to "ඤ", "zh" to "ඥ",
        "zn" to "ං", "zb" to "ඹ",
        "Lu" to "ළු"
    )
    
    // PRIORITY 4: 1-Letter Consonants
    private val consonants1 = mapOf(
        "k" to "ක", "K" to "ඛ",
        "g" to "ග", "G" to "ඝ",
        "c" to "ච", "C" to "ඡ",
        "j" to "ජ", "J" to "ඣ",
        "t" to "ට", "T" to "ඨ",
        "d" to "ඩ", "D" to "ඪ",
        "n" to "න", "N" to "ණ",
        "p" to "ප", "P" to "ඵ",
        "b" to "බ", "B" to "භ",
        "m" to "ම", "M" to "ම",
        "y" to "ය", "Y" to "ය",
        "r" to "ර", "R" to "ර",
        "l" to "ල", "L" to "ළ",
        "w" to "ව", "W" to "ව",
        "v" to "ව", "V" to "ව",
        "s" to "ස", "S" to "ෂ",
        "h" to "හ",
        "f" to "ෆ", "F" to "ෆ",
        "z" to "ඤ", "Z" to "ඥ",
        "q" to "ක", "Q" to "ඛ"
    )
    
    // Special consonants (no hal)
    private val specialConsonants = mapOf(
        "x" to "ං",
        "X" to "ඞ",
        "H" to "ඃ"
    )
    
    // Standalone Vowels
    private val vowelsStandalone = mapOf(
        // 3-letter
        "ruu" to "ඎ", "Ruu" to "ඎ",
        // 2-letter
        "aa" to "ආ", "Aa" to "ඈ", "AA" to "ඈ",
        "ae" to "ඇ", "Ae" to "ඈ",
        "ii" to "ඊ", "II" to "ඊ",
        "ee" to "ඒ", "ei" to "ඒ",
        "uu" to "ඌ", "UU" to "ඌ",
        "oo" to "ඕ", "oe" to "ඕ",
        "au" to "ඖ", "Au" to "ඖ",
        "ai" to "ඓ", "Ai" to "ඓ",
        "ru" to "ඍ", "Ru" to "ඍ",
        // 1-letter
        "a" to "අ", "A" to "ඇ",
        "i" to "ඉ", "I" to "ඊ",
        "u" to "උ", "U" to "ඌ",
        "e" to "එ", "E" to "ඓ",
        "o" to "ඔ", "O" to "ඕ"
    )
    
    // Vowel Modifiers (Pilla)
    private val vowelModifiers = mapOf(
        // 3-letter
        "ruu" to "ෲ", "Ruu" to "ෲ",
        // 2-letter
        "aa" to "ා", "Aa" to "ෑ", "AA" to "ෑ",
        "ae" to "ැ", "Ae" to "ෑ",
        "ii" to "ී", "II" to "ී",
        "ee" to "ේ", "ei" to "ේ",
        "uu" to "ූ", "UU" to "ූ",
        "oo" to "ෝ", "oe" to "ෝ",
        "au" to "ෞ", "Au" to "ෞ",
        "ai" to "ෛ", "Ai" to "ෛ",
        "ru" to "ෘ", "Ru" to "ෘ",
        // 1-letter
        "a" to "",      // Just removes hal
        "A" to "ැ",
        "i" to "ි", "I" to "ී",
        "u" to "ු", "U" to "ූ",
        "e" to "ෙ", "E" to "ෛ",
        "o" to "ො", "O" to "ෝ"
    )

    // ═══════════════════════════════════════════════════════════════════
    // STATE VARIABLES
    // ═══════════════════════════════════════════════════════════════════
    
    private var rootContainer: FrameLayout? = null
    private var keyboardContainer: LinearLayout? = null
    private var keyboardView: LinearLayout? = null
    private var backgroundImageView: ImageView? = null
    
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val settings by lazy { KeyboardSettings(this) }
    
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    // Key Preview Popup
    private var previewPopup: PopupWindow? = null
    private var previewText: TextView? = null
    
    // Keyboard State
    private var isShift = false
    private var isCaps = false
    private var isNumbers = false
    private var isSymbols = false
    private var isSinhalaMode = false
    
    // Repeat handling
    private var isRepeating = false
    private var repeatRunnable: Runnable? = null
    
    // Singlish Buffer
    private val englishBuffer = StringBuilder()
    private var currentSinhalaLength = 0
    
    // Touch tracking - FIX FOR DOUBLE TYPING
    private val keyInfoList = mutableListOf<KeyInfo>()
    private var currentPressedKey: KeyInfo? = null
    private var lastKeyPressTime = 0L
    private var lastActionKeyTime = 0L
    private var hasProcessedCurrentTouch = false  // Prevents double processing
    
    private var navigationBarHeight = 0

    // ═══════════════════════════════════════════════════════════════════
    // BROADCAST RECEIVER
    // ═══════════════════════════════════════════════════════════════════
    
    private val settingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == KeyboardSettings.ACTION_SETTINGS_CHANGED) {
                handler.post {
                    loadSettings()
                    rebuildKeyboard()
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════
    
    override fun onCreate() {
        super.onCreate()
        loadSettings()
        calculateNavBarHeight()
        initPreviewPopup()
        
        val filter = IntentFilter(KeyboardSettings.ACTION_SETTINGS_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(settingsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(settingsReceiver, filter)
        }
    }
    
    override fun onDestroy() {
        stopRepeat()
        hidePreview()
        try { unregisterReceiver(settingsReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }
    
    private fun calculateNavBarHeight() {
        navigationBarHeight = try {
            val id = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (id > 0) resources.getDimensionPixelSize(id) else dp(48)
        } catch (_: Exception) {
            dp(48)
        }
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
        
        previewPopup = PopupWindow(
            previewText,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
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

    // ═══════════════════════════════════════════════════════════════════
    // INPUT VIEW CREATION
    // ═══════════════════════════════════════════════════════════════════
    
    override fun onCreateInputView(): View {
        loadSettings()
        
        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(parseColor(colorBackground))
        }
        
        // Background image
        backgroundImageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = FrameLayout.LayoutParams(-1, -1)
        }
        rootContainer?.addView(backgroundImageView)
        loadBackgroundImage()
        
        // Keyboard container
        keyboardContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        if (showEmojiRow) {
            keyboardContainer?.addView(createEmojiRow())
        }
        
        keyboardView = createKeyboard()
        keyboardContainer?.addView(keyboardView)
        
        val kbParams = FrameLayout.LayoutParams(-1, -2).apply {
            gravity = Gravity.BOTTOM
        }
        rootContainer?.addView(keyboardContainer, kbParams)
        
        // Touch layer
        val touchLayer = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = FrameLayout.LayoutParams(-1, -1)
            setOnTouchListener { _, event -> handleTouch(event) }
        }
        rootContainer?.addView(touchLayer)
        
        // Set height
        val emojiH = if (showEmojiRow) dp(44) else 0
        val totalH = emojiH + dp(keyboardHeight) + navigationBarHeight
        rootContainer?.layoutParams = ViewGroup.LayoutParams(-1, totalH)
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
                    val bmp = BitmapFactory.decodeFile(path)
                    if (bmp != null) {
                        backgroundImageView?.setImageBitmap(bmp)
                        backgroundImageView?.alpha = 0.3f
                        return
                    }
                }
            }
            backgroundImageView?.setImageBitmap(null)
        } catch (_: Exception) {
            backgroundImageView?.setImageBitmap(null)
        }
    }
    
    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        isShift = false
        isCaps = false
        isSymbols = false
        clearSinglishBuffer()
        
        info?.let {
            val cls = it.inputType and EditorInfo.TYPE_MASK_CLASS
            isNumbers = (cls == EditorInfo.TYPE_CLASS_NUMBER || cls == EditorInfo.TYPE_CLASS_PHONE)
        }
        rebuildKeyboard()
    }
    
    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        hidePreview()
        clearSinglishBuffer()
    }

    // ═══════════════════════════════════════════════════════════════════
    // TOUCH HANDLING - FIXED DOUBLE TYPING
    // ═══════════════════════════════════════════════════════════════════
    
    private fun updateKeyBounds() {
        keyInfoList.forEach { it.updateBounds() }
    }
    
    private fun findKey(x: Float, y: Float): KeyInfo? {
        // Direct hit
        keyInfoList.find { it.contains(x, y) }?.let { return it }
        
        // Nearest within threshold
        var minDist = Float.MAX_VALUE
        var nearest: KeyInfo? = null
        val maxDist = dp(40).toFloat()
        
        keyInfoList.forEach { ki ->
            val dist = ki.distanceTo(x, y)
            if (dist < minDist && dist < maxDist) {
                minDist = dist
                nearest = ki
            }
        }
        return nearest
    }
    
    private fun handleTouch(event: MotionEvent): Boolean {
        val x = event.rawX
        val y = event.rawY
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                hasProcessedCurrentTouch = false
                findKey(x, y)?.let { key ->
                    currentPressedKey = key
                    applyPressVisual(key)
                    showPreview(key)
                    
                    // Start repeat for backspace only
                    if (key.key == "⌫") {
                        startRepeat(key.key)
                    }
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                findKey(x, y)?.let { moveKey ->
                    if (moveKey != currentPressedKey) {
                        // Cancel current key
                        currentPressedKey?.let { resetKeyVisual(it) }
                        hasProcessedCurrentTouch = false
                        
                        // Press new key
                        currentPressedKey = moveKey
                        applyPressVisual(moveKey)
                        showPreview(moveKey)
                    }
                }
            }
            
            MotionEvent.ACTION_UP -> {
                hidePreview()
                stopRepeat()
                
                // CRITICAL FIX: Only process on ACTION_UP, and only once
                currentPressedKey?.let { key ->
                    if (!hasProcessedCurrentTouch) {
                        val now = System.currentTimeMillis()
                        
                        // Debounce check
                        val debounce = if (isActionKey(key.key)) ACTION_KEY_DEBOUNCE else DEBOUNCE_TIME
                        val lastTime = if (isActionKey(key.key)) lastActionKeyTime else lastKeyPressTime
                        
                        if (now - lastTime >= debounce) {
                            if (isActionKey(key.key)) lastActionKeyTime = now else lastKeyPressTime = now
                            
                            vibrate()
                            processKey(key.key)
                            hasProcessedCurrentTouch = true
                        }
                    }
                    resetKeyVisual(key)
                }
                
                currentPressedKey = null
                resetAllVisuals()
            }
            
            MotionEvent.ACTION_CANCEL -> {
                hidePreview()
                stopRepeat()
                resetAllVisuals()
                currentPressedKey = null
                hasProcessedCurrentTouch = false
            }
        }
        return true
    }
    
    private fun isActionKey(key: String): Boolean =
        key in listOf("⇧", "123", "ABC", "#+=", "🌐")
    
    private fun applyPressVisual(ki: KeyInfo) {
        ki.view.alpha = 0.6f
        ki.view.scaleX = 0.94f
        ki.view.scaleY = 0.94f
    }
    
    private fun resetKeyVisual(ki: KeyInfo) {
        ki.view.alpha = 1f
        ki.view.scaleX = 1f
        ki.view.scaleY = 1f
    }
    
    private fun resetAllVisuals() {
        keyInfoList.forEach { resetKeyVisual(it) }
    }

    // ═══════════════════════════════════════════════════════════════════
    // KEY PREVIEW
    // ═══════════════════════════════════════════════════════════════════
    
    private fun showPreview(ki: KeyInfo) {
        if (isSpecialKey(ki.key)) {
            hidePreview()
            return
        }
        
        val text = getPreviewText(ki.key)
        if (text.isEmpty()) {
            hidePreview()
            return
        }
        
        previewText?.text = text
        previewText?.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        
        val keyW = ki.view.width
        val keyH = ki.view.height
        val previewW = maxOf(keyW, (previewText?.measuredWidth ?: 0) + dp(16))
        val previewH = (keyH * 1.6f).toInt()
        
        val loc = IntArray(2)
        ki.view.getLocationOnScreen(loc)
        
        var px = loc[0] + (keyW - previewW) / 2
        var py = loc[1] - previewH - dp(8)
        
        // Keep on screen
        val screenW = resources.displayMetrics.widthPixels
        if (px < dp(4)) px = dp(4)
        if (px + previewW > screenW - dp(4)) px = screenW - previewW - dp(4)
        if (py < dp(10)) py = dp(10)
        
        try {
            if (previewPopup?.isShowing == true) {
                previewPopup?.update(px, py, previewW, previewH)
            } else {
                previewPopup?.width = previewW
                previewPopup?.height = previewH
                previewPopup?.showAtLocation(rootContainer, Gravity.NO_GRAVITY, px, py)
            }
        } catch (_: Exception) {}
    }
    
    private fun hidePreview() {
        try {
            if (previewPopup?.isShowing == true) previewPopup?.dismiss()
        } catch (_: Exception) {}
    }
    
    private fun getPreviewText(key: String): String {
        if (key == "SPACE") return ""
        
        if (isSinhalaMode && key.length == 1 && key[0].isLetter()) {
            val labels = if (isShift || isCaps) sinhalaLabelsShift else sinhalaLabels
            labels[key.lowercase()]?.let { return it }
        }
        
        return if (key.length == 1 && key[0].isLetter()) {
            if (isShift || isCaps) key.uppercase() else key.lowercase()
        } else key
    }

    // ═══════════════════════════════════════════════════════════════════
    // KEYBOARD BUILDING
    // ═══════════════════════════════════════════════════════════════════
    
    private fun createEmojiRow(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(-1, dp(44))
            setPadding(dp(4), dp(4), dp(4), dp(4))
            
            val emojis = settings.quickEmojis
            emojis.split(",").forEach { emoji ->
                val e = emoji.trim()
                if (e.isNotEmpty()) {
                    addView(TextView(this@FastKeyboardService).apply {
                        text = e
                        this.gravity = Gravity.CENTER
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                        layoutParams = LinearLayout.LayoutParams(0, -1, 1f).apply {
                            setMargins(dp(2), 0, dp(2), 0)
                        }
                        background = GradientDrawable().apply {
                            setColor(parseColor(colorKeyNormal))
                            cornerRadius = dp(8).toFloat()
                        }
                        setOnClickListener {
                            vibrate()
                            flushSinglishBuffer()
                            commitDirect(e)
                        }
                    })
                }
            }
        }
    }
    
    private fun createKeyboard(): LinearLayout {
        keyInfoList.clear()
        
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, dp(keyboardHeight))
            setPadding(dp(3), dp(6), dp(3), dp(6))
            
            val layout = when {
                isSymbols -> layoutSymbols
                isNumbers -> layoutNumbers
                else -> layoutLetters
            }
            
            layout.forEachIndexed { rowIdx, row ->
                addView(createRow(row, rowIdx))
            }
        }
    }
    
    private fun createRow(keys: Array<String>, rowIdx: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            val sidePad = if (rowIdx == 1) dp(14) else 0
            setPadding(sidePad, dp(2), sidePad, dp(2))
            
            keys.forEach { key -> addView(createKey(key)) }
        }
    }
    
    private fun createKey(key: String): View {
        val container = FrameLayout(this)
        val tv = TextView(this).apply {
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }
        
        val (display, textColor, textSize) = getKeyDisplayConfig(key)
        tv.text = display
        tv.setTextColor(textColor)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
        
        // Sinhala label
        if (isSinhalaMode && key.length == 1 && key[0].isLetter()) {
            val labels = if (isShift || isCaps) sinhalaLabelsShift else sinhalaLabels
            labels[key.lowercase()]?.let { lbl ->
                container.addView(TextView(this).apply {
                    text = lbl
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
                    setTextColor(Color.parseColor("#888888"))
                    layoutParams = FrameLayout.LayoutParams(-2, -2).apply {
                        gravity = Gravity.TOP or Gravity.END
                        setMargins(0, dp(2), dp(3), 0)
                    }
                })
            }
        }
        
        val weight = getWeight(key)
        container.layoutParams = LinearLayout.LayoutParams(0, -1, weight).apply {
            setMargins(dp(keyGap), dp(keyGap), dp(keyGap), dp(keyGap))
        }
        
        container.addView(tv, FrameLayout.LayoutParams(-1, -1))
        container.background = createKeyBg(key)
        
        keyInfoList.add(KeyInfo(key, container))
        return container
    }
    
    private fun getKeyDisplayConfig(key: String): Triple<String, Int, Float> {
        val defaultColor = parseColor(colorText)
        
        return when (key) {
            "↵" -> Triple("↵", Color.WHITE, 22f)
            "⇧" -> when {
                isCaps -> Triple("⇪", Color.parseColor("#10b981"), 24f)
                isShift -> Triple("⬆", Color.parseColor("#3b82f6"), 24f)
                else -> Triple("⇧", defaultColor, 24f)
            }
            "⌫" -> Triple("⌫", defaultColor, 22f)
            "SPACE" -> Triple(
                if (isSinhalaMode) "සිංහල" else "English",
                Color.parseColor("#666666"),
                11f
            )
            "🌐" -> Triple(
                if (isSinhalaMode) "සිං" else "EN",
                if (isSinhalaMode) Color.parseColor("#10b981") else Color.parseColor("#3b82f6"),
                12f
            )
            "✨" -> Triple("✨", defaultColor, 18f)
            "123", "ABC", "#+=" -> Triple(key, defaultColor, 13f)
            else -> {
                val display = if (key.length == 1 && key[0].isLetter()) {
                    if (isShift || isCaps) key.uppercase() else key.lowercase()
                } else key
                Triple(display, defaultColor, keyTextSize.toFloat())
            }
        }
    }
    
    private fun isSpecialKey(key: String): Boolean =
        key in listOf("⇧", "⌫", "↵", "SPACE", "123", "ABC", "#+=", "🌐", "✨")
    
    private fun getWeight(key: String): Float = when (key) {
        "SPACE" -> 3.5f
        "⇧", "⌫" -> 1.5f
        "↵", "123", "ABC", "#+=" -> 1.3f
        else -> 1f
    }
    
    private fun createKeyBg(key: String): GradientDrawable {
        val color = when (key) {
            "↵" -> colorKeyEnter
            "⇧" -> when {
                isCaps -> "#10b981"
                isShift -> "#3b82f6"
                else -> colorKeySpecial
            }
            "⌫", "123", "ABC", "#+=", "🌐", "✨" -> colorKeySpecial
            "SPACE" -> colorKeySpace
            else -> colorKeyNormal
        }
        
        return GradientDrawable().apply {
            setColor(parseColor(color))
            cornerRadius = dp(keyRadius).toFloat()
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // KEY PROCESSING
    // ═══════════════════════════════════════════════════════════════════
    
    private fun processKey(key: String) {
        val ic = currentInputConnection ?: return
        
        when (key) {
            "⇧" -> handleShift()
            "⌫" -> handleBackspace(ic)
            "↵" -> { flushSinglishBuffer(); handleEnter(ic) }
            "SPACE" -> { flushSinglishBuffer(); ic.commitText(" ", 1) }
            ",", "." -> { flushSinglishBuffer(); ic.commitText(key, 1) }
            "123" -> { flushSinglishBuffer(); isNumbers = true; isSymbols = false; rebuildKeyboard() }
            "ABC" -> { flushSinglishBuffer(); isNumbers = false; isSymbols = false; rebuildKeyboard() }
            "#+=" -> { flushSinglishBuffer(); isSymbols = true; rebuildKeyboard() }
            "🌐" -> { flushSinglishBuffer(); isSinhalaMode = !isSinhalaMode; rebuildKeyboard() }
            "✨" -> { flushSinglishBuffer(); openPopup() }
            else -> handleChar(ic, key)
        }
    }
    
    private fun handleShift() {
        when {
            isCaps -> { isCaps = false; isShift = false }
            isShift -> isCaps = true
            else -> isShift = true
        }
        rebuildKeyboard()
    }
    
    private fun handleBackspace(ic: InputConnection) {
        if (englishBuffer.isNotEmpty()) {
            // Delete current Sinhala and recompute
            if (currentSinhalaLength > 0) {
                ic.deleteSurroundingText(currentSinhalaLength, 0)
            }
            englishBuffer.deleteCharAt(englishBuffer.length - 1)
            
            if (englishBuffer.isNotEmpty()) {
                val newSinhala = convertToSinhala(englishBuffer.toString())
                ic.commitText(newSinhala, 1)
                currentSinhalaLength = newSinhala.length
            } else {
                currentSinhalaLength = 0
            }
        } else {
            ic.deleteSurroundingText(1, 0)
        }
    }
    
    private fun handleEnter(ic: InputConnection) {
        val ei = currentInputEditorInfo
        val action = ei?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        
        if (action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.commitText("\n", 1)
        } else {
            ic.performEditorAction(action)
        }
    }
    
    private fun handleChar(ic: InputConnection, key: String) {
        var c = key[0]
        if ((isShift || isCaps) && c.isLetter()) {
            c = c.uppercaseChar()
        }
        
        if (isSinhalaMode && c.isLetter()) {
            processSinglish(ic, c)
        } else {
            flushSinglishBuffer()
            ic.commitText(c.toString(), 1)
        }
        
        autoResetShift()
    }

    // ═══════════════════════════════════════════════════════════════════
    // SINGLISH ENGINE - GREEDY BACK-MATCHING (NO TIMER)
    // ═══════════════════════════════════════════════════════════════════
    
    private fun processSinglish(ic: InputConnection, c: Char) {
        // Delete current Sinhala output
        if (currentSinhalaLength > 0) {
            ic.deleteSurroundingText(currentSinhalaLength, 0)
        }
        
        // Add new character to buffer
        englishBuffer.append(c)
        
        // Convert entire buffer to Sinhala
        val sinhala = convertToSinhala(englishBuffer.toString())
        
        // Commit new Sinhala
        ic.commitText(sinhala, 1)
        currentSinhalaLength = sinhala.length
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
            
            // PRIORITY 1: Try Special Z-Combinations (4-5 chars) - CHECK FIRST!
            for (len in minOf(5, english.length - i) downTo 3) {
                val sub = english.substring(i, i + len)
                consonantsSpecial[sub]?.let {
                    matched = it
                    matchLen = len
                    isConsonant = true
                    needsHal = !sub.endsWith("a") // 'a' removes hal
                    return@let
                }
                if (matched != null) break
            }
            
            // PRIORITY 2: Try 3-Letter Consonants
            if (matched == null && i + 3 <= english.length) {
                val sub = english.substring(i, i + 3)
                
                consonants3[sub]?.let {
                    matched = it
                    matchLen = 3
                    isConsonant = true
                    needsHal = true
                }
                
                // 3-letter vowel modifier
                if (matched == null && lastWasConsonant) {
                    vowelModifiers[sub]?.let {
                        if (result.isNotEmpty() && result.endsWith(HAL)) {
                            result.deleteCharAt(result.length - 1)
                        }
                        matched = it
                        matchLen = 3
                        isConsonant = false
                        lastWasConsonant = false
                    }
                }
                
                // 3-letter standalone vowel
                if (matched == null && !lastWasConsonant) {
                    vowelsStandalone[sub]?.let {
                        matched = it
                        matchLen = 3
                        isConsonant = false
                    }
                }
            }
            
            // PRIORITY 3: Try 2-Letter Consonants
            if (matched == null && i + 2 <= english.length) {
                val sub = english.substring(i, i + 2)
                
                consonants2[sub]?.let {
                    matched = it
                    matchLen = 2
                    isConsonant = true
                    needsHal = sub !in listOf("Lu", "zn") // These don't need hal
                }
                
                // 2-letter vowel modifier
                if (matched == null && lastWasConsonant) {
                    vowelModifiers[sub]?.let {
                        if (result.isNotEmpty() && result.endsWith(HAL)) {
                            result.deleteCharAt(result.length - 1)
                        }
                        matched = it
                        matchLen = 2
                        isConsonant = false
                        lastWasConsonant = false
                    }
                }
                
                // 2-letter standalone vowel
                if (matched == null && !lastWasConsonant) {
                    vowelsStandalone[sub]?.let {
                        matched = it
                        matchLen = 2
                        isConsonant = false
                    }
                }
            }
            
            // PRIORITY 4: Try 1-Letter
            if (matched == null && i < english.length) {
                val sub = english.substring(i, i + 1)
                val ch = sub[0]
                
                // Special consonants (no hal)
                specialConsonants[sub]?.let {
                    matched = it
                    matchLen = 1
                    isConsonant = false
                }
                
                // Regular consonant
                if (matched == null) {
                    consonants1[sub]?.let {
                        matched = it
                        matchLen = 1
                        isConsonant = true
                        needsHal = true
                    }
                }
                
                // Vowel modifier (after consonant)
                if (matched == null && lastWasConsonant) {
                    vowelModifiers[sub]?.let {
                        if (result.isNotEmpty() && result.endsWith(HAL)) {
                            result.deleteCharAt(result.length - 1)
                        }
                        matched = it
                        matchLen = 1
                        isConsonant = false
                        lastWasConsonant = false
                    }
                }
                
                // Standalone vowel
                if (matched == null && !lastWasConsonant) {
                    vowelsStandalone[sub]?.let {
                        matched = it
                        matchLen = 1
                        isConsonant = false
                    }
                }
                
                // Yansaya: 'y' after consonant with hal
                if (matched == null && (ch == 'y' || ch == 'Y') && lastWasConsonant) {
                    if (result.isNotEmpty() && result.endsWith(HAL)) {
                        result.deleteCharAt(result.length - 1)
                        result.append(YANSAYA)
                    }
                    matchLen = 1
                    isConsonant = false
                    lastWasConsonant = false
                    i += matchLen
                    continue
                }
                
                // Rakaransaya: 'r' after consonant followed by vowel
                if (matched == null && (ch == 'r' || ch == 'R') && lastWasConsonant && i + 2 <= english.length) {
                    val next = english[i + 1]
                    if (next in "aeiouAEIOU") {
                        if (result.isNotEmpty() && result.endsWith(HAL)) {
                            result.deleteCharAt(result.length - 1)
                            result.append(RAKARANSAYA)
                        }
                        matchLen = 1
                        isConsonant = true
                        needsHal = false
                        lastWasConsonant = true
                        i += matchLen
                        continue
                    }
                }
            }
            
            // Apply match
            if (matched != null) {
                result.append(matched)
                if (isConsonant && needsHal) {
                    result.append(HAL)
                    lastWasConsonant = true
                } else if (!isConsonant) {
                    lastWasConsonant = false
                }
                i += matchLen
            } else {
                // No match - output as-is
                result.append(english[i])
                lastWasConsonant = false
                i++
            }
        }
        
        return result.toString()
    }
    
    private fun flushSinglishBuffer() {
        englishBuffer.clear()
        currentSinhalaLength = 0
    }
    
    private fun clearSinglishBuffer() {
        englishBuffer.clear()
        currentSinhalaLength = 0
    }
    
    private fun commitDirect(text: String) {
        currentInputConnection?.commitText(text, 1)
    }
    
    private fun autoResetShift() {
        if (isShift && !isCaps) {
            isShift = false
            rebuildKeyboard()
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════
    
    private fun startRepeat(key: String) {
        isRepeating = true
        repeatRunnable = object : Runnable {
            override fun run() {
                if (isRepeating) {
                    processKey(key)
                    vibrate()
                    handler.postDelayed(this, repeatInterval.toLong())
                }
            }
        }
        handler.postDelayed(repeatRunnable!!, longPressDelay.toLong())
    }
    
    private fun stopRepeat() {
        isRepeating = false
        repeatRunnable?.let { handler.removeCallbacks(it) }
    }
    
    private fun vibrate() {
        if (!vibrateEnabled) return
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(
                    vibrateDuration.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(vibrateDuration.toLong())
            }
        } catch (_: Exception) {}
    }
    
    private fun openPopup() {
        try {
            val intent = Intent(this, PopupActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (_: Exception) {}
    }
    
    private fun rebuildKeyboard() {
        rootContainer ?: return
        
        hidePreview()
        keyInfoList.clear()
        
        keyboardContainer?.let { rootContainer?.removeView(it) }
        loadBackgroundImage()
        
        keyboardContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        if (showEmojiRow) keyboardContainer?.addView(createEmojiRow())
        keyboardView = createKeyboard()
        keyboardContainer?.addView(keyboardView)
        
        val params = FrameLayout.LayoutParams(-1, -2).apply {
            gravity = Gravity.BOTTOM
        }
        rootContainer?.addView(keyboardContainer, 1, params)
        
        val emojiH = if (showEmojiRow) dp(44) else 0
        val totalH = emojiH + dp(keyboardHeight) + navigationBarHeight
        rootContainer?.layoutParams?.height = totalH
        rootContainer?.setPadding(0, 0, 0, navigationBarHeight)
        
        rootContainer?.post { updateKeyBounds() }
    }
    
    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
    
    private fun parseColor(color: String): Int = try {
        Color.parseColor(color)
    } catch (_: Exception) {
        Color.BLACK
    }
}