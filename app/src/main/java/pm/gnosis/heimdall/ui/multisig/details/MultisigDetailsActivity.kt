package pm.gnosis.heimdall.ui.multisig.details

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.dialog_multisig_add_input.view.*
import kotlinx.android.synthetic.main.dialog_show_qr_code.view.*
import kotlinx.android.synthetic.main.layout_multisig_details.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.*
import pm.gnosis.heimdall.data.repositories.models.MultisigWallet
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.base.FactoryPagerAdapter
import pm.gnosis.heimdall.ui.multisig.details.info.MultisigInfoFragment
import pm.gnosis.heimdall.ui.tokens.overview.TokensFragment
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsEthereumAddress
import timber.log.Timber
import javax.inject.Inject


class MultisigDetailsActivity : BaseActivity() {
    @Inject
    lateinit var viewModel: MultisigDetailsContract

    private val items = listOf(R.string.tab_title_info, R.string.tab_title_tokens)
    private val generateQrCodeClicks = PublishSubject.create<String>()
    private val removeMultisigClicks = PublishSubject.create<Unit>()
    private val editMultisigClicks = PublishSubject.create<String>()

    private var multisigWallet: MultisigWallet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_multisig_details)

        registerToolbar(layout_multisig_details_toolbar)
        layout_multisig_details_toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        val multisigAddress = intent.getStringExtra(EXTRA_MULTISIG_ADDRESS)!!
        val multisigName = intent.getStringExtra(EXTRA_MULTISIG_NAME)
        viewModel.setup(multisigAddress.hexAsEthereumAddress(), multisigName)

        layout_multisig_details_viewpager.adapter = pagerAdapter(multisigAddress)
        layout_multisig_details_tabbar.setupWithViewPager(layout_multisig_details_viewpager)
        layout_multisig_details_viewpager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                layout_multisig_details_appbar.setExpanded(true, true)
            }
        })
    }

    override fun onStart() {
        super.onStart()
        disposables += viewModel.observeMultisig()
                .subscribeBy(onNext = {
                    this.multisigWallet = it
                    if (!it.name.isNullOrBlank()) {
                        layout_multisig_details_toolbar.title = it.name
                        layout_multisig_details_toolbar.subtitle = it.address.asEthereumAddressString()
                    } else {
                        layout_multisig_details_toolbar.title = it.address.asEthereumAddressString()
                    }
                }, onError = Timber::e)

        disposables += generateQrCodeClicks
                .flatMapSingle {
                    viewModel.loadQrCode(it)
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSuccess { onQrCodeLoading(true) }
                            .doAfterTerminate { onQrCodeLoading(false) }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(onNext = this::onQrCode, onError = this::onQrCodeError)

        disposables += removeMultisigClicks
                .flatMapSingle { viewModel.deleteMultisig() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(onNext = { onWalletRemoved() }, onError = this::onWalletRemoveError)

        disposables += editMultisigClicks
                .flatMapSingle { viewModel.changeMultisigName(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(onNext = { onNameChanged() }, onError = this::onNameChangeError)
    }

    private fun onWalletRemoved() {
        toast(getString(R.string.wallet_remove_success, multisigWallet?.name ?: R.string.multisig))
        finish()
    }

    private fun onWalletRemoveError(throwable: Throwable) {
        snackbar(layout_multisig_details_coordinator, R.string.wallet_remove_error)
    }

    private fun onNameChanged() {
        snackbar(layout_multisig_details_coordinator, R.string.wallet_name_change_success)
    }

    private fun onNameChangeError(throwable: Throwable) {
        snackbar(layout_multisig_details_coordinator, R.string.wallet_name_change_error)
    }

    private fun onQrCode(qrCode: Bitmap) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_show_qr_code, null)
        dialogView.dialog_qr_code_image.setImageBitmap(qrCode)
        AlertDialog.Builder(this)
                .setView(dialogView)
                .show()
    }

    private fun onQrCodeError(throwable: Throwable) {
        Timber.e(throwable)
    }

    private fun onQrCodeLoading(isLoading: Boolean) {
        layout_multisig_details_progress_bar.visibility = if (isLoading) View.VISIBLE else View.INVISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.multisig_details_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.itemId ?: return false
        when {
            item.itemId == R.id.multisig_details_menu_delete -> {
                showRemoveDialog()
            }
            item.itemId == R.id.multisig_details_menu_change_name -> {
                showEditDialog()
            }
            item.itemId == R.id.multisig_details_menu_qr_code -> {
                multisigWallet?.address?.let {
                    generateQrCodeClicks.onNext(it.asEthereumAddressString())
                }
            }
            item.itemId == R.id.multisig_details_menu_clipboard -> {
                multisigWallet?.address?.let {
                    copyToClipboard(CLIPBOARD_ADDRESS_LABEL, it.asEthereumAddressString(), {
                        snackbar(layout_multisig_details_coordinator, R.string.address_clipboard_success)
                    })
                }
            }
            item.itemId == R.id.multisig_details_menu_share -> {
                multisigWallet?.address?.let {
                    shareExternalText(it.asEthereumAddressString(),
                            getString(R.string.share_multisig_address, multisigWallet?.name ?: getString(R.string.multisig)))
                }
            }
            else -> return false
        }
        return true
    }

    private fun showRemoveDialog() {
        AlertDialog.Builder(this)
                .setTitle(R.string.remove_wallet_dialog_title)
                .setMessage(getString(R.string.remove_wallet_dialog_description, multisigWallet?.name ?: getString(R.string.this_wallet)))
                .setPositiveButton(R.string.remove, { _, _ -> removeMultisigClicks.onNext(Unit) })
                .setNegativeButton(R.string.cancel, { _, _ -> })
                .show()
    }

    private fun showEditDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_multisig_add_input, null)
        multisigWallet?.name?.let { dialogView.dialog_add_multisig_text_name.setText(it) }
        dialogView.dialog_add_multisig_text_input_layout.visibility = View.GONE

        AlertDialog.Builder(this)
                .setTitle(R.string.change_name)
                .setView(dialogView)
                .setPositiveButton(R.string.change_name, { _, _ ->
                    editMultisigClicks.onNext(dialogView.dialog_add_multisig_text_name.text.toString())
                })
                .setNegativeButton(R.string.cancel, { _, _ -> })
                .show()
    }

    private fun positionToId(position: Int) = items.getOrElse(position, { -1 })

    private fun pagerAdapter(multisigAddress: String) = FactoryPagerAdapter(supportFragmentManager, FactoryPagerAdapter.Factory(items.size, {
        when (positionToId(it)) {
            R.string.tab_title_info -> {
                MultisigInfoFragment.createInstance(multisigAddress.asEthereumAddressString())
            }
            R.string.tab_title_tokens -> {
                TokensFragment.createInstance(multisigAddress.asEthereumAddressString())
            }
            else -> throw IllegalStateException("Unhandled tab position")
        }
    }, {
        getString(items[it])
    }))

    private fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(HeimdallApplication[this].component)
                .viewModule(ViewModule(this))
                .build()
                .inject(this)
    }

    companion object {
        private const val EXTRA_MULTISIG_NAME = "extra.string.multisig_name"
        private const val EXTRA_MULTISIG_ADDRESS = "extra.string.multisig_address"
        private const val CLIPBOARD_ADDRESS_LABEL = "multisig.address"
        fun createIntent(context: Context, multisig: MultisigWallet): Intent {
            val intent = Intent(context, MultisigDetailsActivity::class.java)
            intent.putExtra(EXTRA_MULTISIG_NAME, multisig.name)
            intent.putExtra(EXTRA_MULTISIG_ADDRESS, multisig.address.asEthereumAddressString())
            return intent
        }
    }
}
