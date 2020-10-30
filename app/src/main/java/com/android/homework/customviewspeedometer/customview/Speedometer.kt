package com.android.homework.customviewspeedometer.customview

import android.animation.ArgbEvaluator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.android.homework.customviewspeedometer.R
import kotlin.math.*

class Speedometer @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private companion object {
        const val STATE_KEY_START_POINTER_POSITION = "pointer_position"
        const val STATE_KEY_COLOR_POINTER = "color_pointer"
        const val SUPER_STATE = "super_state"

        const val DEFAULT_UNIT_NAME = "KM/H"

        const val DEFAULT_SCALE_START_VALUE = 0
        const val DEFAULT_SCALE_END_VALUE = 120
        const val DEFAULT_START_POINTER_POSITION = 5
        const val DEFAULT_SCALE_DIVISION_STEP = 20

        const val DEFAULT_COLOR_TEXT = Color.BLACK
        const val DEFAULT_COLOR_BORDER = Color.BLACK
        const val DEFAULT_COLOR_SCALE = Color.WHITE
        const val DEFAULT_COLOR_BACKGROUND_SCALE = Color.BLUE
        const val DEFAULT_COLOR_INNER_BACKGROUND = Color.DKGRAY
        const val DEFAULT_COLOR_POINTER = Color.YELLOW
        const val DEFAULT_COLOR_START_DOT_POINTER = Color.BLACK

        const val PREFERRED_SIZE = 300

        const val BORDER_RADIUS = 1f
        const val BACKGROUND_SCALE_RADIUS = 0.95f
        const val BACKGROUND_INNER_RADIUS = 0.6f
        const val SHORT_SCALE_LINE = 0.9f
        const val LONG_SCALE_LINE = 0.8f

        const val TEXT_SIZE = 40f
        const val WIDTH_SCALE = 0.007f
        const val WIDTH_POINTER = 0.02f
        const val DURATION_ANIMATION: Long = 5000
        const val PROPERTY_NAME_COLOR = "color"
        const val PROPERTY_NAME_MOTION = "motion"
    }

    private var chosenWidth = PREFERRED_SIZE
    private var chosenHeight = PREFERRED_SIZE

    private var unitName = DEFAULT_UNIT_NAME

    private var scaleStartValue = DEFAULT_SCALE_START_VALUE
    private var scaleEndValue = DEFAULT_SCALE_END_VALUE
    private var startPointerPosition = DEFAULT_START_POINTER_POSITION
    private var scaleDivisionStep = DEFAULT_SCALE_DIVISION_STEP

    private var colorText = DEFAULT_COLOR_TEXT
    private var colorBorder = DEFAULT_COLOR_BORDER
    private var colorScale = DEFAULT_COLOR_SCALE
    private var colorBackgroundScale = DEFAULT_COLOR_BACKGROUND_SCALE
    private var colorInnerBackground = DEFAULT_COLOR_INNER_BACKGROUND
    private var colorPointer = DEFAULT_COLOR_POINTER
    private var colorStartDotPointer = DEFAULT_COLOR_START_DOT_POINTER

    private lateinit var borderPaint: Paint
    private lateinit var scalePaint: Paint
    private lateinit var backgroundScalePaint: Paint
    private lateinit var innerBackgroundPaint: Paint
    private lateinit var pointerPaint: Paint
    private lateinit var startDotPointerPaint: Paint
    private lateinit var textPaint: Paint

    private var step = 0f
    private var valueAnimator: ValueAnimator? = null

    init {
        readAttrs(context, attrs)
        initDrawingTools()
    }

    private fun readAttrs(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(
                attrs,
                R.styleable.Speedometer
        )

        try {
            val chars: CharSequence? = typedArray.getText(R.styleable.Speedometer_android_text)
            unitName = chars?.toString() ?: unitName

            scaleEndValue = typedArray.getInt(R.styleable.Speedometer_endValueScale, scaleEndValue)
            startPointerPosition = typedArray.getInt(R.styleable.Speedometer_startPointerPosition, startPointerPosition)
            scaleDivisionStep = typedArray.getInt(R.styleable.Speedometer_scaleDivisionStep, scaleDivisionStep)

            colorText = typedArray.getColor(R.styleable.Speedometer_colorText, colorText)
            colorScale = typedArray.getColor(R.styleable.Speedometer_colorScale, colorScale)
            colorBackgroundScale = typedArray.getColor(R.styleable.Speedometer_colorBackgroundScale, colorBackgroundScale)
            colorInnerBackground = typedArray.getColor(R.styleable.Speedometer_colorInnerBackground, colorInnerBackground)
            colorPointer = typedArray.getColor(R.styleable.Speedometer_colorPointer, colorPointer)
        } finally {
            typedArray.recycle()
        }
    }

    private fun initDrawingTools() {
        borderPaint = Paint()
        borderPaint.color = colorBorder
        borderPaint.style = Paint.Style.FILL

        backgroundScalePaint = Paint()
        backgroundScalePaint.color = colorBackgroundScale
        backgroundScalePaint.style = Paint.Style.FILL

        innerBackgroundPaint = Paint()
        innerBackgroundPaint.color = colorInnerBackground
        innerBackgroundPaint.style = Paint.Style.FILL

        scalePaint = Paint()
        scalePaint.color = colorScale
        scalePaint.style = Paint.Style.STROKE
        scalePaint.strokeWidth = WIDTH_SCALE

        textPaint = Paint()
        textPaint.textSize = TEXT_SIZE
        textPaint.color = colorText
        textPaint.style = Paint.Style.FILL

        pointerPaint = Paint()
        pointerPaint.color = colorPointer
        pointerPaint.strokeWidth = WIDTH_POINTER

        startDotPointerPaint = Paint()
        startDotPointerPaint.color = colorStartDotPointer
        startDotPointerPaint.style = Paint.Style.FILL

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        chosenWidth = chooseDimension(width, widthMode)
        chosenHeight = chooseDimension(height, heightMode)

        chosenWidth = min(chosenWidth, chosenHeight)
        chosenHeight = chosenWidth / 2

        step = (Math.PI / scaleEndValue).toFloat()

        setMeasuredDimension(chosenWidth, chosenHeight)
    }

    private fun chooseDimension(size: Int, mode: Int): Int =
            when (mode) {
                MeasureSpec.AT_MOST, MeasureSpec.EXACTLY -> size
                MeasureSpec.UNSPECIFIED -> PREFERRED_SIZE
                else -> 0
            }

    override fun onSaveInstanceState(): Parcelable? =
            Bundle().apply {
                putInt(STATE_KEY_START_POINTER_POSITION, startPointerPosition)
                putInt(STATE_KEY_COLOR_POINTER, pointerPaint.color)
                putParcelable(SUPER_STATE, super.onSaveInstanceState())
            }

    override fun onRestoreInstanceState(state: Parcelable?) {
        var superState = state

        if (state is Bundle) {
            startPointerPosition = state.getInt(STATE_KEY_START_POINTER_POSITION)
            pointerPaint.color = state.getInt(STATE_KEY_COLOR_POINTER)
            superState = state.getParcelable(SUPER_STATE)
            invalidate()
        }

        super.onRestoreInstanceState(superState)
    }

    fun increaseSpeed() {
        runAnimation(pointerPaint.color, Color.RED, startPointerPosition, scaleEndValue)
    }

    fun reduceSpeed() {
        runAnimation(pointerPaint.color, colorPointer, startPointerPosition, scaleStartValue)
    }

    private fun runAnimation(startPositionColor: Int, endPositionColor: Int, startPositionMotion: Int, endPositionMotion: Int) {
        val colorValue = PropertyValuesHolder.ofInt(PROPERTY_NAME_COLOR, startPositionColor, endPositionColor)
        val motionValue = PropertyValuesHolder.ofInt(PROPERTY_NAME_MOTION, startPositionMotion, endPositionMotion)

        valueAnimator = ValueAnimator.ofPropertyValuesHolder(colorValue, motionValue).apply {
            setEvaluator(ArgbEvaluator())
            addUpdateListener { anim ->
                val color = anim.getAnimatedValue(PROPERTY_NAME_COLOR) as Int
                val motion = anim.getAnimatedValue(PROPERTY_NAME_MOTION) as Int

                pointerPaint.color = color
                startPointerPosition = motion
                invalidate()
            }
            interpolator = AccelerateDecelerateInterpolator()
            duration = DURATION_ANIMATION
            start()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.save()
        canvas?.translate(chosenWidth.toFloat() / 2, chosenHeight.toFloat())
        canvas?.scale(.5f * chosenWidth, -1f * chosenHeight)
        drawBorder(canvas)
        drawBackgroundScale(canvas)
        drawInnerBackground(canvas)
        drawScale(canvas)
        canvas?.restore()

        canvas?.save()
        canvas?.translate(chosenWidth.toFloat() / 2, 0f)
        drawLabels(canvas)
        canvas?.restore()

        canvas?.save()
        canvas?.translate(chosenWidth.toFloat() / 2, chosenHeight.toFloat())
        canvas?.scale(.5f * chosenWidth, -1f * chosenHeight)
        canvas?.rotate(90 - 180.toFloat() * (startPointerPosition / scaleEndValue))
        drawPointer(canvas)
        canvas?.restore()
    }

    private fun drawPointer(canvas: Canvas?) {
        canvas?.drawLine(0.01f, 0f, 0f, BACKGROUND_SCALE_RADIUS, pointerPaint)
        canvas?.drawLine(-0.01f, 0f, 0f, BACKGROUND_SCALE_RADIUS, pointerPaint)
        canvas?.drawCircle(0f, 0f, .05f, startDotPointerPaint)
    }

    private fun drawLabels(canvas: Canvas?) {
        val textPadding = 0.85f
        val coefficient = chosenHeight * LONG_SCALE_LINE * textPadding

        for (j in 0..scaleEndValue step scaleDivisionStep) {
            val x = cos(Math.PI - step * j) * coefficient
            val y = sin(Math.PI - step * j) * coefficient
            val text = j.toString()
            val textLength = textPaint.measureText(text).roundToInt()
            canvas?.drawText(text, (x - textLength / 2).toFloat(), (chosenHeight - y).toFloat(), textPaint)
        }
        canvas?.drawText(unitName, -textPaint.measureText(unitName) / 2, chosenHeight - chosenHeight * 0.15f, textPaint)
    }

    private fun drawScale(canvas: Canvas?) {
        for (i in 0..scaleEndValue) {
            val x1 = (cos(Math.PI - step * i) * BACKGROUND_SCALE_RADIUS).toFloat()
            val y1 = (sin(Math.PI - step * i) * BACKGROUND_SCALE_RADIUS).toFloat()
            var x2: Float
            var y2: Float
            if (i % scaleDivisionStep == 0) {
                x2 = x1 * LONG_SCALE_LINE
                y2 = y1 * LONG_SCALE_LINE
            } else {
                x2 = x1 * SHORT_SCALE_LINE
                y2 = y1 * SHORT_SCALE_LINE
            }

            canvas?.drawLine(x1, y1, x2, y2, scalePaint)
        }
    }

    private fun drawInnerBackground(canvas: Canvas?) {
        canvas?.drawCircle(0f, 0f, BACKGROUND_INNER_RADIUS, innerBackgroundPaint)
    }

    private fun drawBackgroundScale(canvas: Canvas?) {
        canvas?.drawCircle(0f, 0f, BACKGROUND_SCALE_RADIUS, backgroundScalePaint)
    }

    private fun drawBorder(canvas: Canvas?) {
        canvas?.drawCircle(0f, 0f, BORDER_RADIUS, borderPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                val newValue = getTouchValue(event.x, event.y)
                startPointerPosition = newValue
                if (pointerPaint.color != colorPointer) pointerPaint.color = colorPointer
                invalidate()
                true
            }
            else -> super.onTouchEvent(event)
        }

    }

    private fun getTouchValue(x: Float, y: Float): Int {
        return if (x != 0f && y != 0f) {
            val startX = width / 2
            val startY = height

            val adjacentLeg = startX - x
            val oppositeLeg = startY - y
            val angle = acos(adjacentLeg / (sqrt((adjacentLeg * adjacentLeg + oppositeLeg * oppositeLeg).toDouble())))

            (scaleEndValue * (angle / Math.PI)).roundToInt()
        } else {
            startPointerPosition
        }
    }

}