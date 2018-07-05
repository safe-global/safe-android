package pm.gnosis.heimdall.data.repositories.impls

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.os.Build
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import pm.gnosis.blockies.Blockies
import pm.gnosis.blockies.BlockiesPainter
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.ShortcutRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.tokens.select.SelectTokenActivity
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.nullOnThrow
import timber.log.Timber
import javax.inject.Inject

class DefaultShortcutRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safeRepository: GnosisSafeRepository
) : ShortcutRepository {


    private val iconDimension = context.resources.getDimension(R.dimen.shortcut_icon)

    private val blockiesPainter = BlockiesPainter().apply {
        setDimensions(iconDimension, iconDimension)
    }

    override fun init() {
        shortcutManager()?.let {
            safeRepository.observeDeployedSafes()
                .subscribeOn(Schedulers.io())
                .map(::createShortcutInfo)
                .subscribeBy(onNext = ::setupSafeShortcuts, onError = Timber::e)
        }
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private fun createShortcutInfo(safes: List<Safe>): List<ShortcutInfo> =
    // This should only be done if we can use the shortcut manager
        shortcutManager()?.let {
            safes.map {
                ShortcutInfo.Builder(context, it.address.asEthereumAddressString())
                    .setShortLabel(context.getString(R.string.send_from_x, it.displayName(context)))
                    .setLongLabel(context.getString(R.string.send_from_x, it.displayName(context)))
                    .setIcon(Icon.createWithBitmap(blockiesBitmap(it.address)))
                    .setIntent(SelectTokenActivity.createIntent(context, it.address).prepare())
                    .build()
            }
        } ?: emptyList()

    private fun blockiesBitmap(address: Solidity.Address) =
        Bitmap.createBitmap(iconDimension.toInt(), iconDimension.toInt(), Bitmap.Config.ARGB_8888).apply {
            blockiesPainter.draw(Canvas(this), Blockies.fromAddress(address)!!)
        }

    private fun Intent.prepare(): Intent = apply {
        action = Intent.ACTION_VIEW
        flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private fun setupSafeShortcuts(shortcuts: List<ShortcutInfo>) {
        // This should only be done if we can use the shortcut manager
        shortcutManager()?.let {
            it.removeAllDynamicShortcuts()
            it.dynamicShortcuts = shortcuts
        }
    }

    private fun shortcutManager(): ShortcutManager? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
            nullOnThrow { context.getSystemService(ShortcutManager::class.java) }
        else null
}
