package io.gnosis.safe.utils

import android.graphics.*
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import io.gnosis.safe.R

fun ImageView.loadTokenLogo(
    icon: String?,
    @DrawableRes placeHolderResource: Int = R.drawable.ic_coin_placeholder,
    @DrawableRes backgroundDrawable: Int? = R.drawable.circle
) {
    setPadding(0)
    setImageDrawable(null)
    colorFilter = null
    background = backgroundDrawable?.let {
        ContextCompat.getDrawable(context, backgroundDrawable)
    }
    when {
        icon == "local::native_currency" -> {
            setImageResource(R.drawable.ic_native_logo)
        }
        icon?.startsWith("local::") == true -> {
            setImageResource(placeHolderResource)
        }
        !icon.isNullOrBlank() -> {
            //FIXME: workaround, as Picasso uses a final ApplicationContext for its #getDrawable() action, which will not listen to theme's attributes.
            // this would lead to day/night mode not handled correctly
            val placeholderDrawable = ContextCompat.getDrawable(context, placeHolderResource)!!
            Picasso.get()
                .load(icon)
                .placeholder(placeholderDrawable)
                .error(placeholderDrawable)
                .transform(CircleTransformation)
                .into(this)
        }
        else -> {
            setImageResource(placeHolderResource)
        }
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
