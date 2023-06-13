package io.gnosis.safe.ui.assets.collectibles.details

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentCollectiblesDetailsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.utils.dpToPx
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import javax.inject.Inject

class CollectiblesDetailsFragment : BaseViewBindingFragment<FragmentCollectiblesDetailsBinding>() {

    override fun screenId() = ScreenId.ASSETS_COLLECTIBLES_DETAILS

    private val navArgs by navArgs<CollectiblesDetailsFragmentArgs>()
    private val chain by lazy { navArgs.chain }
    private val contract by lazy { navArgs.contract }
    private val name by lazy { navArgs.name }
    private val id by lazy { navArgs.id }
    private val description by lazy { navArgs.description }
    private val uri by lazy { navArgs.uri }
    private val imageUri by lazy { navArgs.imageUri }

    @Inject
    lateinit var settingsHandler: SettingsHandler


    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentCollectiblesDetailsBinding =
        FragmentCollectiblesDetailsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {

            chainRibbon.text = chain.name
            try {
                chainRibbon.setTextColor(Color.parseColor(chain.textColor))
                chainRibbon.setBackgroundColor(Color.parseColor(chain.backgroundColor))
            } catch (e: Exception) {
                tracker.logException(e)
                chainRibbon.setTextColor(requireContext().getColorCompat(R.color.white))
                chainRibbon.setBackgroundColor(requireContext().getColorCompat(R.color.primary))
            }

            val width = resources.displayMetrics.widthPixels - dpToPx(32)
            collectibleImageContainer.layoutParams.width = width
            collectibleImageContainer.layoutParams.height = width
            collectibleImage.loadCollectibleImage(imageUri)
            collectibleName.text = name ?: getString(R.string.collectibles_unknown)
            collectibleId.text = id
            if (description != null) {
                collectibleDescription.text = description
            } else {
                collectibleDescription.visible(false)
            }
            collectibleContract.name = getString(R.string.collectibles_asset_contract)
            collectibleContract.setAddress(
                chain = chain,
                value = contract.asEthereumAddress(),
                showChainPrefix = settingsHandler.chainPrefixPrepend,
                copyChainPrefix = settingsHandler.chainPrefixCopy
            )

            collectibleUri.visible(false)
            //FIXME: uncomment when collectible uri should be visible
//            if (uri != null) {
//                collectibleUri.text = getString(R.string.collectibles_view_on, Uri.parse(uri).let { it.encodedAuthority })
//                collectibleUri.setOnClickListener {
//                    requireContext().openUrl(uri!!)
//                }
//            } else {
//                collectibleUri.visible(false)
//            }

            backButton.setOnClickListener {
                Navigation.findNavController(it).navigateUp()
            }
        }
    }
}

fun ImageView.loadCollectibleImage(logo: String?) {
    if (!logo.isNullOrBlank()) {
        Picasso.get()
            .load(logo)
            .into(object : Target {

                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

                override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                    (parent as View).visible(false)
                }

                override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                    (parent as View).visible(true)
                    setImageBitmap(bitmap)
                }
            })
    } else {
        (parent as View).visible(false)
    }
}
