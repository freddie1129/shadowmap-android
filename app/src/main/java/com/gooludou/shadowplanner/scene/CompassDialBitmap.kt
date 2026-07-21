package com.gooludou.shadowplanner.scene

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withRotation
import kotlin.math.cos
import kotlin.math.sin

object CompassDialBitmap {
    const val TEXTURE_SIZE_PX = 1024

    fun create(domeToOuterRadiusRatio: Float): Bitmap {
        val bitmap = createBitmap(
            TEXTURE_SIZE_PX,
            TEXTURE_SIZE_PX,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        val center = TEXTURE_SIZE_PX / 2f
        val diskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(58, 214, 220, 224)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, center - 1f, diskPaint)
        val ringRadius = center * domeToOuterRadiusRatio.coerceIn(0.55f, 0.92f)
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 255, 222, 138)
            strokeCap = Paint.Cap.SQUARE
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 246, 218)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val labelOutlinePaint = Paint(labelPaint).apply {
            color = Color.argb(210, 24, 24, 24)
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
        }
        CompassDial.marks().forEach { mark ->
            drawMark(canvas, mark, center, ringRadius, tickPaint, labelPaint, labelOutlinePaint)
        }
        return bitmap
    }

    private fun drawMark(
        canvas: Canvas,
        mark: CompassMark,
        center: Float,
        ringRadius: Float,
        tickPaint: Paint,
        labelPaint: Paint,
        labelOutlinePaint: Paint
    ) {
        val tickLength = tickLength(mark.type)
        tickPaint.strokeWidth = tickWidth(mark.type)
        val angle = Math.toRadians(mark.azimuthDegrees.toDouble())
        val directionX = sin(angle).toFloat()
        val directionY = -cos(angle).toFloat()
        canvas.drawLine(
            center + directionX * ringRadius,
            center + directionY * ringRadius,
            center + directionX * (ringRadius + tickLength),
            center + directionY * (ringRadius + tickLength),
            tickPaint
        )
        mark.label?.let { label ->
            drawLabel(
                canvas,
                mark,
                label,
                center,
                ringRadius + tickLength,
                labelPaint,
                labelOutlinePaint
            )
        }
    }

    private fun drawLabel(
        canvas: Canvas,
        mark: CompassMark,
        label: String,
        center: Float,
        tickOuterRadius: Float,
        labelPaint: Paint,
        labelOutlinePaint: Paint
    ) {
        val isCardinal = mark.type == CompassTickType.CARDINAL
        labelPaint.textSize = TEXTURE_SIZE_PX * if (isCardinal) 0.048f else 0.034f
        labelOutlinePaint.textSize = labelPaint.textSize
        labelOutlinePaint.strokeWidth = TEXTURE_SIZE_PX * if (isCardinal) 0.006f else 0.004f
        val labelRadius = tickOuterRadius + TEXTURE_SIZE_PX *
            if (isCardinal) 0.034f else 0.026f
        canvas.withRotation(mark.azimuthDegrees.toFloat(), center, center) {
            val baseline = center - labelRadius -
                (labelPaint.ascent() + labelPaint.descent()) / 2f
            drawText(label, center, baseline, labelOutlinePaint)
            drawText(label, center, baseline, labelPaint)
        }
    }

    private fun tickLength(type: CompassTickType): Float = TEXTURE_SIZE_PX * when (type) {
        CompassTickType.MEDIUM -> 0.012f
        CompassTickType.MAJOR -> 0.022f
        CompassTickType.CARDINAL -> 0.032f
    }

    private fun tickWidth(type: CompassTickType): Float = TEXTURE_SIZE_PX * when (type) {
        CompassTickType.MEDIUM -> 0.0015f
        CompassTickType.MAJOR -> 0.0025f
        CompassTickType.CARDINAL -> 0.0035f
    }
}
