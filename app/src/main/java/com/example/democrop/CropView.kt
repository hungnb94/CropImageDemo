package com.example.democrop

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageView


@RequiresApi(Build.VERSION_CODES.O)
class CropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    AppCompatImageView(context, attrs, defStyleAttr), View.OnTouchListener {


    private var paint: Paint? = null;
    private var points: ArrayList<Point> = ArrayList()
    private var bitmapMain: Bitmap? = null;
    private var leftX: Int = 0
    private var rightX: Int = 0
    private var upY: Int = 0
    private var downY: Int = 0

    companion object {

    }

    fun setBitmap(bitmap: Bitmap) {
        bitmapMain = bitmap
    }

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(10f, 20f), 0f)
            strokeWidth = 5f
            color = Color.WHITE
        }
        setOnTouchListener(this)
        points = ArrayList()


    }

    private val path = Path()

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        path.reset()
        var first = true
        (0 until points?.size!! step 2).forEach {
            var p = points!![it]
            when {
                first -> {
                    first = false
                    path.moveTo(p.x.toFloat(), p.y.toFloat())
                }
                it < points?.size!! - 1 -> {
                    var next: Point = points!![it + 1]
                    path.quadTo(p.x.toFloat(), p.y.toFloat(), next.x.toFloat(), next.y.toFloat())
                }
                else -> {
                    path.lineTo(p.x.toFloat(), p.y.toFloat())
                }
            }
        }
        if (points!!.size > 0) {
            path.lineTo(points!![0].x.toFloat(), points!![0].y.toFloat())
        }

        canvas?.drawPath(path, paint!!)

    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_UP || event?.action == MotionEvent.ACTION_OUTSIDE || event?.action == MotionEvent.ACTION_CANCEL) {
            return true
        }

        var x = event?.x?.toInt()
        var y = event?.y?.toInt()
        if (event?.action == MotionEvent.ACTION_DOWN) {
            leftX = x!!
            rightX = x
            downY = y!!
            upY = y
        }

        var p = Point()
        p.x = x!!
        p.y = y!!
        if (x < leftX) {
            leftX = x
        }
        if (x > rightX) {
            rightX = x
        }
        if (y > downY) {
            downY = y
        }
        if (y < upY) {
            upY = y
        }
        points?.add(p)
        invalidate()
        return true
    }

    fun clear() {
        leftX = 0
        rightX = 0
        upY = 0
        downY = 0

        points?.clear()
        paint?.apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
        }
        invalidate()
    }

    fun getCropImage(): Bitmap {
        val resultingImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(resultingImage)

        val paint = Paint()
        paint.isAntiAlias = true

        canvas.drawPath(path, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmapMain!!, 0f, 0f, paint)

        return resultingImage
    }


}