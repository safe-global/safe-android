package pm.gnosis.android.app.wallet.ui

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_generate_transaction.*
import pm.gnosis.android.app.wallet.GnosisApplication
import pm.gnosis.android.app.wallet.R
import pm.gnosis.android.app.wallet.data.geth.GethRepository
import pm.gnosis.android.app.wallet.data.model.TransactionDetails
import pm.gnosis.android.app.wallet.data.model.Wei
import pm.gnosis.android.app.wallet.di.component.DaggerViewComponent
import pm.gnosis.android.app.wallet.di.module.ViewModule
import pm.gnosis.android.app.wallet.util.ERC20
import pm.gnosis.android.app.wallet.util.asHexString
import pm.gnosis.android.app.wallet.util.hexAsBigInteger
import pm.gnosis.android.app.wallet.util.nullOnThrow
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject

//Currently just a mockup activity for debugging (could become a feature later on)
class GenerateTransactionActivity : AppCompatActivity() {
    @Inject lateinit var gethRepo: GethRepository

    private val accounts = mutableListOf<String>()
    private val tokenContracts = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.activity_generate_transaction)

        accounts.addAll(gethRepo.getAccounts().map { it.address.hex })
        tokenContracts.addAll(ERC20.verifiedTokens.map { "${it.key.asHexString()} (${it.value})" })

        to_address.setOnClickListener {
            AlertDialog.Builder(this)
                    .setTitle("Select account")
                    .setItems((accounts + tokenContracts).toTypedArray(),
                            { dialog, which ->
                                val selected = (dialog as AlertDialog).listView.getItemAtPosition(which) as String
                                to_address.setText(selected)
                            })
                    .setOnDismissListener {
                        val toAddress = to_address.text.toString().split(" ").first().hexAsBigInteger()
                        if (ERC20.verifiedTokens.containsKey(toAddress)) {
                            token_transaction.visibility = View.VISIBLE
                            no_token_transaction.visibility = View.GONE
                        } else {
                            token_transaction.visibility = View.GONE
                            no_token_transaction.visibility = View.VISIBLE
                        }
                    }
                    .show()
        }

        generate_transaction.setOnClickListener {
            val address = to_address.text.toString().split(" ").first().hexAsBigInteger()
            val value = nullOnThrow { Wei(BigDecimal(value.text.toString()).multiply(BigDecimal.TEN.pow(18)).toBigInteger()) }
            val transaction = TransactionDetails(
                    address = address,
                    value = value,
                    gas = nullOnThrow { BigInteger(gas.text.toString()) },
                    gasPrice = nullOnThrow { BigInteger(gas_price.text.toString()) },
                    data = nullOnThrow {
                        if (ERC20.verifiedTokens.containsKey(address)) {
                            ERC20.TokenTransfer(token_recipient.text.toString().hexAsBigInteger(), BigDecimal(token_amount.text.toString())).encode()
                        } else {
                            data.text.toString()
                        }
                    })
            val intent = Intent(this, TransactionDetailsActivity::class.java)
            intent.putExtra(TransactionDetailsActivity.TRANSACTION_EXTRA, transaction)
            startActivity(intent)
        }
    }

    private fun inject() {
        DaggerViewComponent.builder()
                .viewModule(ViewModule(this))
                .applicationComponent(GnosisApplication[this].component)
                .build()
                .inject(this)
    }
}
