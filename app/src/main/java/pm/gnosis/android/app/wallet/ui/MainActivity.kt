package pm.gnosis.android.app.wallet.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import com.squareup.moshi.Moshi
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_main.*
import pm.gnosis.android.app.wallet.GnosisApplication
import pm.gnosis.android.app.wallet.R
import pm.gnosis.android.app.wallet.data.geth.GethRepository
import pm.gnosis.android.app.wallet.data.model.Wei
import pm.gnosis.android.app.wallet.data.remote.InfuraRepository
import pm.gnosis.android.app.wallet.di.component.DaggerViewComponent
import pm.gnosis.android.app.wallet.di.module.ViewModule
import pm.gnosis.android.app.wallet.ui.account.AccountFragment
import pm.gnosis.android.app.wallet.ui.multisig.MultisigFragment
import pm.gnosis.android.app.wallet.ui.scan.ScanFragment
import pm.gnosis.android.app.wallet.ui.tokens.TokensFragment
import pm.gnosis.android.app.wallet.util.ERC67Parser
import pm.gnosis.android.app.wallet.util.snackbar
import pm.gnosis.android.app.wallet.util.zxing.ZxingIntentIntegrator
import pm.gnosis.android.app.wallet.util.zxing.ZxingIntentIntegrator.SCAN_RESULT_EXTRA
import timber.log.Timber
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainActivity : AppCompatActivity() {
    @Inject lateinit var gethRepo: GethRepository
    @Inject lateinit var moshi: Moshi
    @Inject lateinit var infuraRepository: InfuraRepository

    val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.activity_main)

        bottom_navigation.setOnNavigationItemSelectedListener {
            val fragment = when (it.itemId) {
                R.id.action_account -> AccountFragment()
                R.id.action_multisig -> MultisigFragment()
                R.id.action_tokens -> TokensFragment()
                R.id.action_scan -> ScanFragment()
                else -> null
            }
            if (fragment != null) {
                replaceFragment(fragment)
                return@setOnNavigationItemSelectedListener true
            } else {
                return@setOnNavigationItemSelectedListener false
            }
        }

        bottom_navigation.selectedItemId = R.id.action_account

        /*scan_qr_btn.setOnClickListener {
            val integrator = ZxingIntentIntegrator(this)
            integrator.initiateScan(QR_CODE_TYPES)
        }
        account_address.text = gethRepo.getAccount().address.hex
        Timber.d(account_address.text.toString())

        account_card.setOnClickListener {
            AlertDialog.Builder(this)
                    .setTitle("Select account")
                    .setItems(gethRepo.getAccounts().map { it.address.hex }.toTypedArray(),
                            { dialog, which ->
                                val selected = (dialog as AlertDialog).listView.getItemAtPosition(which) as String
                                gethRepo.setActiveAccount(selected)
                                account_address.text = selected
                                disposables += getBalance()
                            })
                    .setOnDismissListener { snackbar(coordinator_layout, "Changed account") }
                    .show()
        }*/
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.main_content, fragment).commit()
    }

    override fun onStart() {
        super.onStart()
        disposables += getBalance()
        disposables += getLatestBlock()
    }

    private fun getBalance(): Disposable {
        return infuraRepository.getBalance()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = this::onBalance, onError = Timber::e)
    }

    private fun getLatestBlock(): Disposable {
        return Observable.interval(0L, 10, TimeUnit.SECONDS)
                .flatMap { _ -> infuraRepository.getLatestBlock() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = this::onRecentBlock, onError = Timber::e)
    }

    fun onBalance(balance: Wei) {
        val etherBalance = balance.toEther()
        //Java 7 bug - does not strip trailing zeroes when the number itself is zero
        // account_balance.text = if (etherBalance.compareTo(BigDecimal.ZERO) == 0) "0 Ξ" else etherBalance.stripTrailingZeros().toPlainString() + " Ξ"
    }

    private fun onRecentBlock(blockNumber: BigInteger) {
        // recent_block.text = blockNumber.toString(10)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ZxingIntentIntegrator.REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null && data.hasExtra(SCAN_RESULT_EXTRA)) {
                processQrCode(data.getStringExtra(SCAN_RESULT_EXTRA))
            } else if (resultCode == Activity.RESULT_CANCELED) {
                snackbar(coordinator_layout, "Cancelled by the user")
            }
        }
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    fun processQrCode(data: String) {
        val transactionDetails = ERC67Parser.parse(data)
        if (transactionDetails != null) {
            val intent = Intent(this, TransactionDetailsActivity::class.java)
            intent.putExtra(TransactionDetailsActivity.TRANSACTION_EXTRA, transactionDetails)
            startActivity(intent)
        } else {
            snackbar(coordinator_layout, "QRCode is not a valid transaction URI")
        }
    }

    fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(GnosisApplication[this].component)
                .viewModule(ViewModule(this))
                .build().inject(this)
    }
}
