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
import com.gojuno.koptional.None
import com.gojuno.koptional.toOptional
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import pm.gnosis.blockies.Blockies
import pm.gnosis.blockies.BlockiesPainter
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.ShortcutRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.transactions.create.CreateAssetTransferActivity
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.nullOnThrow
import timber.log.Timber
import javax.inject.Inject

class DefaultShortcutRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val addressBookRepository: AddressBookRepository,
    private val safeRepository: GnosisSafeRepository
) : ShortcutRepository {


    private val iconDimension = context.resources.getDimension(R.dimen.shortcut_icon)

    private val blockiesPainter = BlockiesPainter().apply {
        setDimensions(iconDimension, iconDimension)
    }

    override fun init() {
        shortcutManager()?.let {
            safeRepository.observeSafes()
                .subscribeOn(Schedulers.io())
                .flatMapSingle(::createShortcutInfo)
                .subscribeBy(onNext = ::setupSafeShortcuts, onError = Timber::e)
        }
    }

    @TargetApi(Build.VERSION_CODES.N_MR1)
    private fun createShortcutInfo(safes: List<Safe>): Single<List<ShortcutInfo>> =
    // This should only be done if we can use the shortcut manager
        Observable.merge(safes.map { safe ->
            addressBookRepository.loadAddressBookEntry(safe.address)
                .map { it.toOptional() }
                .onErrorReturnItem(None)
                .flatMapObservable { info ->
                    info.toNullable()?.let {
                        Observable.just(
                            ShortcutInfo.Builder(context, it.address.asEthereumAddressString())
                                .setShortLabel(context.getString(R.string.send_from_x, it.name))
                                .setLongLabel(context.getString(R.string.send_from_x, it.name))
                                .setIcon(Icon.createWithBitmap(blockiesBitmap(it.address)))
                                .setIntent(CreateAssetTransferActivity.createIntent(context, it.address, ERC20Token.ETHER_TOKEN).prepare())
                                .build()
                        )
                    } ?: Observable.empty<ShortcutInfo>()
                }
        }).toList()

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
