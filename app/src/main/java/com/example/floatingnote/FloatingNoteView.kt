package com.example.floatingnote

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.view.WindowManager

class FloatingNoteView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val params get() = layoutParams as WindowManager.LayoutParams
    private val editText: EditText
    private lateinit var previewText: TextView
    private val root: LinearLayout
    private var previewMode = false
    private val dot: View

    private var minimized = false
    private var tapCount = 0
    private val tapHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val resetTapCount = Runnable { tapCount = 0 }

    private var downX = 0f
    private var downY = 0f
    private var downRawX = 0f
    private var downRawY = 0f
    private var dragging = false
    private var resizing = false
    private var touchSlop = 24f

    private var initialSpan = 0f
    private var initialWidth = 0
    private var initialHeight = 0
    private var pinching = false
    private var gestureStarted = false

    private val saveHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var editTextFocused = false

    init {
        root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = context.getDrawable(R.drawable.rounded_bg)
            setPadding(12, 8, 12, 12)
        }
        val header = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END; setPadding(0, 0, 0, 4) }
        val mdToggle = TextView(context).apply {
            text = "≡"
            textSize = 14f
            gravity = Gravity.CENTER
            background = context.getDrawable(R.drawable.btn_bg)
            setOnClickListener { togglePreview() }
            setTextColor(0x80FFFFFF.toInt())
        }
        val lp = LinearLayout.LayoutParams(32, 32).apply { marginStart = 4 }
        header.addView(mdToggle, lp)
        val minusBtn = TextView(context).apply {
            text = ""
            textSize = 14f
            gravity = Gravity.CENTER
            background = context.getDrawable(R.drawable.btn_bg)
            setOnClickListener { toggleMinimize() }
            setPadding(0, 0, 0, 0)
            setTextColor(0xFF555555.toInt())
        }
        val lp2 = LinearLayout.LayoutParams(32, 32).apply { marginStart = 4 }
        header.addView(minusBtn, lp2)
        root.addView(header)
        editText = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
            setText(NoteRepository.loadText(context))
            setHint("Note…")
            setBackgroundColor(Color.TRANSPARENT)
            gravity = Gravity.TOP or Gravity.START
            setOnTouchListener { _, _ ->
                val p = params
                p.flags = p.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                wm.updateViewLayout(this@FloatingNoteView, p)
                requestFocus()
                false
            }
            setOnFocusChangeListener { _, hasFocus ->
                editTextFocused = hasFocus
                if (!hasFocus) {
                    val p = params
                    p.flags = p.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    wm.updateViewLayout(this@FloatingNoteView, p)
                }
            }
        }
        previewText = TextView(context).apply {
            background = context.getDrawable(R.drawable.rounded_bg)
            setPadding(12, 8, 12, 12)
            visibility = View.GONE
            movementMethod = android.text.method.LinkMovementMethod.getInstance()
        }
        root.addView(editText)
        root.addView(previewText, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        addView(root, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        editText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                saveHandler.removeCallbacksAndMessages(null)
                saveHandler.postDelayed({ NoteRepository.saveText(context, editText.text.toString()) }, 400)
            }
        })

        dot = View(context).apply {
            background = context.getDrawable(R.drawable.rounded_bg)
            visibility = View.GONE
            setOnTouchListener(DotTouchListener())
        }
        addView(dot, LayoutParams(40, 40))

        val resizeHandle = View(context).apply {
            background = context.getDrawable(R.drawable.resize_handle)
            alpha = 0f
            setOnTouchListener(ResizeHandleListener())
        }
        val handleSize = (24f * resources.displayMetrics.density).toInt()
        addView(resizeHandle, LayoutParams(handleSize, handleSize, Gravity.BOTTOM or Gravity.END))
    }

    private fun togglePreview() {
        previewMode = !previewMode
        if (previewMode) {
            editText.visibility = View.GONE
            previewText.visibility = View.VISIBLE
            previewText.text = MarkdownRenderer.render(context, editText.text.toString())
        } else {
            editText.visibility = View.VISIBLE
            previewText.visibility = View.GONE
        }
    }

    private fun toggleMinimize() {
        minimized = !minimized
        if (minimized) {
            root.visibility = View.GONE
            dot.visibility = View.VISIBLE
            params.width = 40
            params.height = 40
            wm.updateViewLayout(this, params)
        } else {
            root.visibility = View.VISIBLE
            dot.visibility = View.GONE
            params.width = NoteRepository.loadW(context)
            params.height = NoteRepository.loadH(context)
            wm.updateViewLayout(this, params)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (minimized) return false
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = params.x.toFloat()
                downY = params.y.toFloat()
                downRawX = ev.rawX
                downRawY = ev.rawY
                dragging = false
                pinching = false
                gestureStarted = false
                return false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (ev.pointerCount == 2 && !dragging && !gestureStarted) {
                    initialSpan = getSpan(ev)
                    initialWidth = params.width
                    initialHeight = params.height
                    pinching = true
                    gestureStarted = true
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (pinching && ev.pointerCount == 2) return true
                val dx = ev.rawX - downRawX
                val dy = ev.rawY - downRawY
                if (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop) {
                    if (!pinching) {
                        dragging = true
                        gestureStarted = true
                        return true
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (ev.pointerCount == 2) pinching = false
            }
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = params.x.toFloat()
                downY = params.y.toFloat()
                downRawX = ev.rawX
                downRawY = ev.rawY
                dragging = false
                pinching = false
                gestureStarted = false
                if (minimized) {
                    return true
                }
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (ev.pointerCount == 2 && !dragging && !resizing && !gestureStarted) {
                    initialSpan = getSpan(ev)
                    initialWidth = params.width
                    initialHeight = params.height
                    pinching = true
                    gestureStarted = true
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (pinching && ev.pointerCount == 2) {
                    val currentSpan = getSpan(ev)
                    val scale = currentSpan / initialSpan
                    val newW = (initialWidth * scale).toInt().coerceAtLeast(200).coerceAtMost(1200)
                    val newH = (initialHeight * scale).toInt().coerceAtLeast(120).coerceAtMost(800)
                    params.width = newW
                    params.height = newH
                    wm.updateViewLayout(this, params)
                    return true
                }
                val dx = ev.rawX - downRawX
                val dy = ev.rawY - downRawY
                if (minimized) {
                    params.x = (downX + dx).toInt()
                    params.y = (downY + dy).toInt()
                    wm.updateViewLayout(this, params)
                    return true
                }
                if (!dragging && !pinching && !resizing && (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop)) {
                    dragging = true
                    gestureStarted = true
                }
                if (dragging) {
                    params.x = (downX + dx).toInt()
                    params.y = (downY + dy).toInt()
                    wm.updateViewLayout(this, params)
                }
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (ev.pointerCount == 2) {
                    pinching = false
                    NoteRepository.saveSize(context, params.width, params.height)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging && !minimized) NoteRepository.savePos(context, params.x, params.y)
                if (minimized) {
                    val moved = kotlin.math.abs(ev.rawX - downRawX) > 8 || kotlin.math.abs(ev.rawY - downRawY) > 8
                    if (!moved) {
                        tapCount++
                        tapHandler.removeCallbacks(resetTapCount)
                        if (tapCount >= 2) {
                            tapCount = 0
                            toggleMinimize()
                        } else {
                            tapHandler.postDelayed(resetTapCount, 350)
                        }
                    } else {
                        tapCount = 0
                        NoteRepository.savePos(context, params.x, params.y)
                    }
                }
                dragging = false
                pinching = false
                gestureStarted = false
                return true
            }
        }
        return super.onTouchEvent(ev)
    }

    private inner class DotTouchListener : OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var downRawX = 0f
        private var downRawY = 0f
        private var tapCount = 0
        private val tapHandler = android.os.Handler(android.os.Looper.getMainLooper())
        private val resetTap = Runnable { tapCount = 0 }

        override fun onTouch(v: View, ev: MotionEvent): Boolean {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = params.x.toFloat()
                    downY = params.y.toFloat()
                    downRawX = ev.rawX
                    downRawY = ev.rawY
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - downRawX
                    val dy = ev.rawY - downRawY
                    if (kotlin.math.abs(dx) > 5 || kotlin.math.abs(dy) > 5) {
                        tapCount = 0
                        tapHandler.removeCallbacks(resetTap)
                        params.x = (downX + dx).toInt()
                        params.y = (downY + dy).toInt()
                        wm.updateViewLayout(this@FloatingNoteView, params)
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val dx = ev.rawX - downRawX
                    val dy = ev.rawY - downRawY
                    val moved = kotlin.math.abs(dx) > 5 || kotlin.math.abs(dy) > 5
                    if (!moved) {
                        tapCount++
                        tapHandler.removeCallbacks(resetTap)
                        if (tapCount >= 2) {
                            tapCount = 0
                            toggleMinimize()
                        } else {
                            tapHandler.postDelayed(resetTap, 350)
                        }
                    } else {
                        tapCount = 0
                        NoteRepository.savePos(context, params.x, params.y)
                    }
                    return true
                }
            }
            return false
        }
    }

    private inner class ResizeHandleListener : OnTouchListener {
        private var startWidth = 0
        private var startHeight = 0
        private var startRawX = 0f
        private var startRawY = 0f

        override fun onTouch(v: View, ev: MotionEvent): Boolean {
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startWidth = params.width
                    startHeight = params.height
                    startRawX = ev.rawX
                    startRawY = ev.rawY
                    resizing = true
                    gestureStarted = true
                    v.parent.requestDisallowInterceptTouchEvent(true)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - startRawX
                    val dy = ev.rawY - startRawY
                    val newW = (startWidth + dx).toInt().coerceAtLeast(200).coerceAtMost(1200)
                    val newH = (startHeight + dy).toInt().coerceAtLeast(120).coerceAtMost(800)
                    params.width = newW
                    params.height = newH
                    wm.updateViewLayout(this@FloatingNoteView, params)
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    resizing = false
                    gestureStarted = false
                    NoteRepository.saveSize(context, params.width, params.height)
                    return true
                }
            }
            return false
        }
    }

    private fun getSpan(ev: MotionEvent): Float {
        val dx = ev.getX(0) - ev.getX(1)
        val dy = ev.getY(0) - ev.getY(1)
        return kotlin.math.hypot(dx, dy)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        NoteRepository.saveText(context, editText.text.toString())
        try {
            NoteRepository.savePos(context, params.x, params.y)
            NoteRepository.saveSize(context, params.width, params.height)
        } catch (e: Exception) { }
    }
}
