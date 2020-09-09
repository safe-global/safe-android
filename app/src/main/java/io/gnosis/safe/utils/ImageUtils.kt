package io.gnosis.safe.utils

import android.graphics.*
import android.widget.ImageView
import androidx.core.view.setPadding
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import io.gnosis.safe.R

fun ImageView.loadTokenLogo(icon: String?, placeHolderResource: Int = R.drawable.ic_coin_placeholder) {
    setPadding(0)
    background = null
    setImageDrawable(null)
    colorFilter = null
    when {
        icon == "local::ethereum" -> {
            setImageResource(R.drawable.ic_ethereum_logo)
        }
        icon?.startsWith("local::") == true -> setImageResource(placeHolderResource)

        !icon.isNullOrBlank() ->
            Picasso.get()
                .load(icon)
                .placeholder(placeHolderResource)
                .error(placeHolderResource)
                .transform(CircleTransformation)
                .into(this)
        else -> setImageResource(placeHolderResource)
    }
}

object CircleTransformation : Transformation {
    override fun key() = "Circle Transformation"

    override fun transform(source: Bitmap?): Bitmap? {
        source ?: return null
        val size = source.width.coerceAtMost(source.height)

        val x = (source.width - size) / 2
        val y = (source.height - size) / 2

        val squaredBitmap = Bitmap.createBitmap(source, x, y, size, size)
        if (squaredBitmap != source) {
            source.recycle()
        }

        val bitmap = Bitmap.createBitmap(size, size, source.config)

        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.shader = BitmapShader(squaredBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.isAntiAlias = true

        val r = size / 2f
        canvas.drawCircle(r, r, r, paint)

        squaredBitmap.recycle()
        return bitmap
    }
}
