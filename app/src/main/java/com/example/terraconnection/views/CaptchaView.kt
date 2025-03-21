package com.example.terraconnection.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class CaptchaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var captchaText: String = ""
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val random = Random
    private val colors = arrayOf(
        Color.BLACK,
        Color.rgb(50, 50, 50),
        Color.rgb(30, 30, 30)
    )
    private val fonts = arrayOf(
        Typeface.DEFAULT_BOLD,
        Typeface.MONOSPACE,
        Typeface.SERIF
    )

    fun setCaptchaText(text: String) {
        captchaText = text
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Set background
        canvas.drawColor(Color.WHITE)
        
        // Draw noise (dots)
        paint.color = Color.LTGRAY
        for (i in 0..40) {
            val x = random.nextFloat() * width
            val y = random.nextFloat() * height
            canvas.drawCircle(x, y, 2f, paint)
        }

        // Draw noise (lines)
        for (i in 0..4) {
            paint.color = Color.GRAY
            paint.strokeWidth = random.nextFloat() * 2 + 1
            canvas.drawLine(
                random.nextFloat() * width,
                random.nextFloat() * height,
                random.nextFloat() * width,
                random.nextFloat() * height,
                paint
            )
        }

        if (captchaText.isEmpty()) return

        val charWidth = width / captchaText.length.toFloat()
        val centerY = height / 2f

        captchaText.forEachIndexed { index, char ->
            // Randomize paint properties for each character
            paint.color = colors[random.nextInt(colors.size)]
            paint.typeface = fonts[random.nextInt(fonts.size)]
            paint.textSize = height * 0.6f
            paint.strokeWidth = 0f
            paint.style = Paint.Style.FILL

            // Calculate position
            val xPos = index * charWidth + charWidth / 2
            val yPos = centerY + (Math.sin(index.toDouble() + random.nextDouble()) * 10).toFloat()

            // Save canvas state before rotation
            canvas.save()

            // Rotate the canvas for this character
            canvas.rotate(
                (random.nextFloat() * 30 - 15),
                xPos,
                yPos
            )

            // Draw the character
            val textWidth = paint.measureText(char.toString())
            canvas.drawText(
                char.toString(),
                xPos - textWidth / 2,
                yPos + paint.textSize / 3,
                paint
            )

            // Restore canvas state
            canvas.restore()
        }

        // Draw wave effect
        paint.color = Color.argb(50, 0, 0, 0)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        val path = Path()
        path.moveTo(0f, centerY)
        for (i in 0..width step 10) {
            path.lineTo(
                i.toFloat(),
                centerY + (Math.sin(i.toDouble() * 0.05) * 10).toFloat()
            )
        }
        canvas.drawPath(path, paint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 400
        val desiredHeight = 150

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> desiredWidth.coerceAtMost(widthSize)
            else -> desiredWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> desiredHeight.coerceAtMost(heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }
} 