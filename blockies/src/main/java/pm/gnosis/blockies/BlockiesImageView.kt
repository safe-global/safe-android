package pm.gnosis.blockies

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.ImageView
import java.util.*

class BlockiesImageView(context: Context, attributeSet: AttributeSet) : ImageView(context, attributeSet) {
    private var color: HSL? = null
    private var bgColor: HSL? = null
    private var spotColor: HSL? = null
    private var imageData: DoubleArray? = null
    private var randSeed = LongArray(4)
    private var scale = 16

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawOnCanvas(canvas)
    }

    private fun drawOnCanvas(canvas: Canvas) {
        val imageData = imageData ?: return
        val color = color ?: return
        val bgColor = bgColor ?: return
        val spotColor = spotColor ?: return

        val width = Math.sqrt(imageData.size.toDouble()).toInt()

        val w = width * scale
        val h = width * scale

        val background = toRGB(bgColor.h.toInt().toFloat(), bgColor.s.toInt().toFloat(), bgColor.l.toInt().toFloat())

        var paint = Paint()
        paint.style = Paint.Style.FILL
        paint.color = background
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        val main = toRGB(color.h.toInt().toFloat(), color.s.toInt().toFloat(), color.l.toInt().toFloat())
        val scolor = toRGB(spotColor.h.toInt().toFloat(), spotColor.s.toInt().toFloat(), spotColor.l.toInt().toFloat())

        for (i in imageData.indices) {
            val row = Math.floor((i / width).toDouble()).toInt()
            val col = i % width
            paint = Paint()

            paint.color = if (imageData[i] == 1.0) main else scolor

            if (imageData[i] > 0.0) {
                canvas.drawRect((col * scale).toFloat(), (row * scale).toFloat(), (col * scale + scale).toFloat(), (row * scale + scale).toFloat(), paint)
            }
        }
    }

    fun setAddress(address: String) {
        seedrand(address)
        color = createColor()
        bgColor = createColor()
        spotColor = createColor()
        imageData = createImageData()
        invalidate()
    }

    private fun toRGB(h: Float, s: Float, l: Float): Int {
        var h = h
        var s = s
        var l = l
        h %= 360.0f
        h /= 360f
        s /= 100f
        l /= 100f

        var q: Float

        q = if (l < 0.5) l * (1 + s) else l + s - s * l

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

    private fun hueToRGB(p: Float, q: Float, h: Float): Float {
        var hue = h
        if (hue < 0) hue += 1f
        if (hue > 1) hue -= 1f
        if (6 * hue < 1) return p + (q - p) * 6f * hue
        if (2 * hue < 1) return q

        return if (3 * hue < 2) p + (q - p) * 6f * (2.0f / 3.0f - hue) else p
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

    private fun seedrand(seed: String) {
        for (i in randSeed.indices) {
            randSeed[i] = 0
        }
        for (i in 0 until seed.length) {
            var test = randSeed[i % 4] shl 5
            if (test > Integer.MAX_VALUE shl 1 || test < Integer.MIN_VALUE shl 1)
                test = test.toInt().toLong()

            val test2 = test - randSeed[i % 4]
            randSeed[i % 4] = test2 + Character.codePointAt(seed, i)
        }

        for (i in randSeed.indices) randSeed[i] = randSeed[i].toInt().toLong()
    }

    companion object {
        const val SIZE = 8
    }

    data class HSL(val h: Double, val s: Double, val l: Double)
}
