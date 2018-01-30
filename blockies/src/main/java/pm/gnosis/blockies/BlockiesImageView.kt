package pm.gnosis.blockies

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.ImageView
import java.util.*

class BlockiesImageView(context: Context, attributeSet: AttributeSet) : ImageView(context, attributeSet) {
    private val canvasPaint = Paint().apply { style = Paint.Style.FILL }
    private val randSeed = LongArray(4)
    private var dimen = 0.0f
    private var offsetX = 0.0f
    private var offsetY = 0.0f
    private val path = Path()

    private var color: HSL? = null
    private var bgColor: HSL? = null
    private var spotColor: HSL? = null
    private var imageData: DoubleArray? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawOnCanvas(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        dimen = Math.min(measuredWidth, measuredHeight).toFloat()
        offsetX = measuredWidth - dimen
        offsetY = measuredHeight - dimen
        path.addCircle(offsetX + (dimen / 2), offsetY + (dimen / 2), dimen / 2, Path.Direction.CCW)
        path.close()
    }

    private fun drawOnCanvas(canvas: Canvas) {
        val imageData = imageData ?: return
        val color = color ?: return
        val bgColor = bgColor ?: return
        val spotColor = spotColor ?: return

        canvas.save()
        canvas.clipPath(path)
        canvasPaint.color = bgColor.toRgb()
        canvas.drawRect(
                offsetX, offsetY, offsetX + dimen, offsetY + dimen,
                canvasPaint
        )

        val scale = dimen / SIZE
        val main = color.toRgb()
        val sColor = spotColor.toRgb()

        for (i in imageData.indices) {
            val col = i % SIZE
            val row = i / SIZE

            canvasPaint.color = if (imageData[i] == 1.0) main else sColor

            if (imageData[i] > 0.0) {
                canvas.drawRect(offsetX + (col * scale), offsetY + (row * scale), offsetX + (col * scale + scale), offsetY + (row * scale + scale), canvasPaint)
            }
        }

        canvas.restore()
    }

    fun getCroppedBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val color = -0xbdbdbe
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawCircle((bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat(),
                (bitmap.width / 2).toFloat(), paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    fun setAddress(address: String) {
        seedFromAddress(address)
        color = createColor()
        bgColor = createColor()
        spotColor = createColor()
        imageData = createImageData()
        invalidate()
    }

    private fun createImageData(): DoubleArray {
        val width = SIZE
        val height = SIZE

        val dataWidth = Math.ceil((width / 2).toDouble())
        val mirrorWidth = width - dataWidth

        val data = DoubleArray(SIZE * SIZE)
        var dataCount = 0
        for (y in 0 until height) {
            var row = DoubleArray(dataWidth.toInt())
            var x = 0
            while (x < dataWidth) {
                row[x] = Math.floor(rand() * 2.3)
                x++
            }

            var r = Arrays.copyOfRange(row, 0, mirrorWidth.toInt())
            r = reverse(r)
            row = concat(row, r)

            for (i in row.indices) {
                data[dataCount] = row[i]
                dataCount++
            }
        }

        return data
    }

    private fun reverse(data: DoubleArray): DoubleArray {
        for (i in 0 until data.size / 2) {
            val temp = data[i]
            data[i] = data[data.size - i - 1]
            data[data.size - i - 1] = temp
        }
        return data
    }

    private fun concat(a: DoubleArray, b: DoubleArray): DoubleArray {
        val aLen = a.size
        val bLen = b.size
        val c = DoubleArray(aLen + bLen)
        System.arraycopy(a, 0, c, 0, aLen)
        System.arraycopy(b, 0, c, aLen, bLen)
        return c
    }

    private fun createColor(): HSL {
        val h = Math.floor(rand() * 360.0)
        val s = rand() * 60.0 + 40.0
        val l = (rand() + rand() + rand() + rand()) * 25.0
        return HSL(h, s, l)
    }

    private fun rand(): Double {
        val t = (randSeed[0] xor (randSeed[0] shl 11)).toInt()
        randSeed[0] = randSeed[1]
        randSeed[1] = randSeed[2]
        randSeed[2] = randSeed[3]
        randSeed[3] = randSeed[3] xor (randSeed[3] shr 19) xor t.toLong() xor (t shr 8).toLong()
        val t1 = Math.abs(randSeed[3]).toDouble()

        return t1 / Integer.MAX_VALUE
    }

    private fun seedFromAddress(address: String) {
        randSeed.indices.forEach { randSeed[it] = 0 }

        (0 until address.length).forEach {
            var test = randSeed[it % 4] shl 5
            if (test > Integer.MAX_VALUE shl 1 || test < Integer.MIN_VALUE shl 1) test = test.toInt().toLong()

            val test2 = test - randSeed[it % 4]
            randSeed[it % 4] = test2 + Character.codePointAt(address, it)
        }

        randSeed.indices.forEach { randSeed[it] = randSeed[it].toInt().toLong() }
    }

    companion object {
        const val SIZE = 8
    }

    data class HSL(val h: Double, val s: Double, val l: Double) {
        fun toRgb(): Int {
            var h = h.toFloat()
            var s = s.toFloat()
            var l = l.toFloat()
            h %= 360.0f
            h /= 360f
            s /= 100f
            l /= 100f

            val q = if (l < 0.5) l * (1 + s) else l + s - s * l
            val p = 2 * l - q

            var r = Math.max(0f, hueToRGB(p, q, h + 1.0f / 3.0f))
            var g = Math.max(0f, hueToRGB(p, q, h))
            var b = Math.max(0f, hueToRGB(p, q, h - 1.0f / 3.0f))

            r = Math.min(r, 1.0f)
            g = Math.min(g, 1.0f)
            b = Math.min(b, 1.0f)

            val red = (r * 255).toInt()
            val green = (g * 255).toInt()
            val blue = (b * 255).toInt()
            return Color.rgb(red, green, blue)
        }
    }
}
