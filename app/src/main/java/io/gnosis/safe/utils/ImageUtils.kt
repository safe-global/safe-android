package io.gnosis.safe.utils

import android.widget.ImageView
import androidx.core.view.setPadding
import com.squareup.picasso.Picasso
import io.gnosis.safe.R

fun ImageView.loadTokenLogo(icon: String?) {
    setPadding(0)
    background = null
    setImageDrawable(null)
    colorFilter = null
    when {
        icon == "local::ethereum" -> {
            setImageResource(R.drawable.ic_ethereum_logo)
        }
        icon?.startsWith("local::") == true -> {
//            setImageResource(R.drawable.circle_background)
        }
        !icon.isNullOrBlank() ->
            Picasso.get()
                .load(icon)
//                .placeholder(R.drawable.circle_background)
//                .error(R.drawable.circle_background)
//                .transform(CircleTransformation)
                .into(this)
        else -> {
        }
//            setImageResource(R.drawable.circle_background)
    }
}
