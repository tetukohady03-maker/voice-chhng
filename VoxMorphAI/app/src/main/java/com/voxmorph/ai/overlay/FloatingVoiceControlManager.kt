package com.voxmorph.ai.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.voxmorph.ai.MainActivity
import com.voxmorph.ai.data.model.VoicePreset
import com.voxmorph.ai.data.prefs.AppSettings
import kotlin.math.abs

/**
 * Draggable, transparent Floating Voice Control widget (SYSTEM_ALERT_WINDOW).
 * Supports minimized pill & expanded control panel, edge-snapping so it stays clear
 * of Mobile Legends / game buttons, quick preset switching, mute, ON/OFF, volume,
 * engine status, and live latency readout.
 */
class FloatingVoiceControlManager(
    private val context: Context,
    private val onToggleEngine: () -> Unit,
    private val onToggleMute: () -> Unit,
    private val onSelectPreset: (VoicePreset) -> Unit,
    private val onVolumeChange: (Float) -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var rootView: LinearLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var isExpanded = false
    private var isTemporarilyHidden = false
    private var presets: List<VoicePreset> = emptyList()
    private var activePresetName = "Feminine Voice"
    private var engineActive = false
    private var muted = false
    private var latencyMs = 0f
    private var currentSettings = AppSettings()

    private var pillLabel: TextView? = null
    private var statusText: TextView? = null
    private var expandedPanel: LinearLayout? = null
    private var toggleBtn: Button? = null
    private var muteBtn: Button? = null
    private var presetContainer: LinearLayout? = null

    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(context)
    fun isShowing(): Boolean = rootView != null

    @SuppressLint("ClickableViewAccessibility")
    fun show(settings: AppSettings) {
        if (!canDrawOverlays()) return
        currentSettings = settings
        if (rootView != null) {
            applyVisualSettings(settings)
            return
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (settings.floatingDockRight) 760 else 24
            y = 260
        }
        layoutParams = params

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = roundedBg(Color.parseColor("#E60F1324"), Color.parseColor("#8B5CF6"), 20f)
            alpha = settings.floatingOpacity
        }

        // Minimized header pill (draggable)
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val pill = TextView(context).apply {
            text = "● VM AI"
            setTextColor(Color.parseColor("#00D4FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(4), dp(2), dp(8), dp(2))
        }
        pillLabel = pill

        val expandBtn = TextView(context).apply {
            text = "▾"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dp(6), dp(2), dp(6), dp(2))
            setOnClickListener {
                isExpanded = !isExpanded
                expandedPanel?.visibility = if (isExpanded) View.VISIBLE else View.GONE
                text = if (isExpanded) "▴" else "▾"
            }
        }

        val hideBtn = TextView(context).apply {
            text = "—"
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(dp(6), dp(2), dp(4), dp(2))
            setOnClickListener {
                isTemporarilyHidden = !isTemporarilyHidden
                if (isTemporarilyHidden) {
                    isExpanded = false
                    expandedPanel?.visibility = View.GONE
                    container.alpha = 0.35f
                    pill.text = "●"
                } else {
                    container.alpha = currentSettings.floatingOpacity
                    refreshUi()
                }
            }
        }

        header.addView(pill)
        header.addView(expandBtn)
        header.addView(hideBtn)
        container.addView(header)

        // Expanded Quick Control Panel
        val panel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, dp(8), 0, 0)
        }
        expandedPanel = panel

        val status = TextView(context).apply {
            text = "Engine: Standby | 0.0 ms"
            setTextColor(Color.parseColor("#CBD5E1"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, 0, 0, dp(6))
        }
        statusText = status
        panel.addView(status)

        // Row: ON/OFF + Mute
        val actionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val btnOnOff = Button(context).apply {
            text = "START"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            background = roundedBg(Color.parseColor("#7C3AED"), Color.parseColor("#A78BFA"), 12f)
            setOnClickListener { onToggleEngine() }
        }
        toggleBtn = btnOnOff

        val btnMute = Button(context).apply {
            text = "MUTE"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            background = roundedBg(Color.parseColor("#1E293B"), Color.parseColor("#38BDF8"), 12f)
            setOnClickListener { onToggleMute() }
        }
        muteBtn = btnMute

        actionRow.addView(btnOnOff, LinearLayout.LayoutParams(dp(86), dp(36)).apply { marginEnd = dp(6) })
        actionRow.addView(btnMute, LinearLayout.LayoutParams(dp(86), dp(36)))
        panel.addView(actionRow)

        // Monitoring Volume Slider
        val volLabel = TextView(context).apply {
            text = "Monitor Volume"
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setPadding(0, dp(6), 0, dp(2))
        }
        panel.addView(volLabel)

        val seekBar = SeekBar(context).apply {
            max = 150
            progress = (settings.monitoringVolume * 100).toInt().coerceIn(0, 150)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) onVolumeChange(progress / 100f)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        panel.addView(seekBar, LinearLayout.LayoutParams(dp(178), LinearLayout.LayoutParams.WRAP_CONTENT))

        // Quick Preset Chips
        val presetTitle = TextView(context).apply {
            text = "Quick Voices"
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setPadding(0, dp(4), 0, dp(4))
        }
        panel.addView(presetTitle)

        val pContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        presetContainer = pContainer
        panel.addView(pContainer)

        // Open Dashboard Button
        val openDashBtn = Button(context).apply {
            text = "Buka Dashboard"
            setTextColor(Color.parseColor("#00D4FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            background = roundedBg(Color.parseColor("#111827"), Color.parseColor("#00D4FF"), 12f)
            setOnClickListener {
                val intent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(intent)
            }
        }
        panel.addView(openDashBtn, LinearLayout.LayoutParams(dp(178), dp(36)).apply { topMargin = dp(6) })
        container.addView(panel)

        // Drag handler on header
        var initX = 0
        var initY = 0
        var touchX = 0f
        var touchY = 0f
        header.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x
                    initY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > 4 || abs(dy) > 4) {
                        params.x = initX + dx
                        params.y = initY + dy
                        windowManager.updateViewLayout(container, params)
                        true
                    } else false
                }
                else -> false
            }
        }

        rootView = container
        windowManager.addView(container, params)
        refreshUi()
    }

    fun hide() {
        rootView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        rootView = null
        layoutParams = null
    }

    fun applyVisualSettings(settings: AppSettings) {
        currentSettings = settings
        rootView?.let { v ->
            v.alpha = if (isTemporarilyHidden) 0.35f else settings.floatingOpacity
            v.scaleX = settings.floatingScale
            v.scaleY = settings.floatingScale
        }
    }

    fun updateState(
        isEngineRunning: Boolean,
        isMuted: Boolean,
        activeName: String,
        estLatencyMs: Float,
        quickPresets: List<VoicePreset>
    ) {
        engineActive = isEngineRunning
        muted = isMuted
        activePresetName = activeName
        latencyMs = estLatencyMs
        presets = quickPresets.take(5)
        refreshUi()
    }

    private fun refreshUi() {
        val indicator = when {
            muted -> "🔇"
            engineActive -> "🎙️"
            else -> "●"
        }
        if (!isTemporarilyHidden) {
            pillLabel?.text = "$indicator ${activePresetName.take(12)}"
        }
        val stateLabel = if (engineActive) (if (muted) "Muted" else "LIVE") else "Standby"
        statusText?.text = "Engine: $stateLabel | %.1f ms".format(latencyMs)
        toggleBtn?.text = if (engineActive) "STOP" else "START"
        muteBtn?.text = if (muted) "UNMUTE" else "MUTE"

        presetContainer?.let { container ->
            container.removeAllViews()
            presets.forEach { preset ->
                val chip = TextView(context).apply {
                    text = if (preset.name == activePresetName) "▸ ${preset.name}" else preset.name
                    setTextColor(
                        if (preset.name == activePresetName) Color.parseColor("#00D4FF")
                        else Color.parseColor("#E2E8F0")
                    )
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    background = roundedBg(
                        if (preset.name == activePresetName) Color.parseColor("#2E1065") else Color.parseColor("#1E293B"),
                        Color.parseColor("#475569"),
                        10f
                    )
                    setOnClickListener { onSelectPreset(preset) }
                }
                container.addView(
                    chip,
                    LinearLayout.LayoutParams(dp(178), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = dp(4)
                    }
                )
            }
        }
    }

    private fun roundedBg(fillColor: Int, strokeColor: Int, radiusDp: Float): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp.toInt()).toFloat()
            setColor(fillColor)
            setStroke(dp(1), strokeColor)
        }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()
}
