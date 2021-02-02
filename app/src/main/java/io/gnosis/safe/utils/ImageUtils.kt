package io.gnosis.safe.utils

import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.squareup.picasso.Picasso
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
                .into(this)
        }
        else -> {
            setImageResource(placeHolderResource)
        }
    }
}
