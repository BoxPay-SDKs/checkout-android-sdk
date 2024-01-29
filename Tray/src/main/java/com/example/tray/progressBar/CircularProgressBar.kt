package com.example.tray.progressBar


import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CircularProgressBar(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progress = 0
    private var max = 100

    init {
        backgroundPaint.color = 0xFFC74848.toInt()
        progressPaint.style = Paint.Style.STROKE
        progressPaint.strokeCap = Paint.Cap.ROUND
    }

    fun setProgress(progress: Int) {
        this.progress = progress
        invalidate()
    }

    fun setMax(max: Int) {
        this.max = max
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val radius = width.coerceAtMost(height) / 2 - 20
        val strokeWidth = 20f  // Adjust the thickness of the progress ring

        // Draw the background circle with #E7E7E7 color
        backgroundPaint.color = 0xFFE7E7E7.toInt()
        canvas.drawCircle(width / 2, height / 2, radius, backgroundPaint)

        // Draw progress arc with rounded ends (progress bar as a ring)
        val rectProgress = RectF(
            20f + strokeWidth / 2,
            20f + strokeWidth / 2,
            width - 20 - strokeWidth / 2,
            height - 20 - strokeWidth / 2
        )
        val angle = 360f * (progress.toFloat() / max.toFloat())
        val startAngle = 0f  // Set the starting angle to 0 degrees

        // Draw a semi-transparent trail behind the progress
        val rectTrail = RectF(
            20f,
            20f,
            width - 20,
            height - 20
        )
        val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        trailPaint.color = 0x30E7E7E7  // Semi-transparent version of #E7E7E7
        canvas.drawArc(rectTrail, startAngle + angle, 360f - angle, false, trailPaint)

        // Draw the progress ring with gradient
        progressPaint.strokeWidth = strokeWidth
        progressPaint.shader = createGradientShader(rectProgress.centerX(), rectProgress.centerY(), startAngle)
        canvas.drawArc(rectProgress, startAngle, angle, false, progressPaint)
    }




    private fun createGradientShader(centerX: Float, centerY: Float, startAngle: Float): Shader {
        val gradientColors = intArrayOf(
            0xFFBADFFF.toInt(),
            0xFF61B2F8.toInt(),
            0xFFBADFFF.toInt()
        )

        // Calculate the adjusted starting angle for the gradient
        val adjustedStartAngle = startAngle - 90f

        // Check the Android version and use the appropriate constructor
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            SweepGradient(centerX, centerY, gradientColors, null)
        } else {
            val gradient = SweepGradient(centerX, centerY, gradientColors[0], gradientColors[2])
            val matrix = Matrix()
            matrix.setRotate(adjustedStartAngle, centerX, centerY)
            gradient.setLocalMatrix(matrix)
            gradient
        }
    }
}

