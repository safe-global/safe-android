package pm.gnosis.heimdall.data.repositories.models

import android.content.Context
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import java.math.BigInteger

sealed class AbstractSafe {
    abstract fun displayName(context: Context): String
}

data class Safe(val address: Solidity.Address, val name: String? = null) : AbstractSafe() {

    override fun displayName(context: Context) = safeName(context, name)

}

data class PendingSafe(
    val address: Solidity.Address,
    val hash: BigInteger,
    val name: String?,
    val paymentToken: Solidity.Address,
    val paymentAmount: BigInteger,
    val isFunded: Boolean = false
) :
    AbstractSafe() {

    override fun displayName(context: Context) = safeName(context, name)

}

data class RecoveringSafe(
    val address: Solidity.Address,
    val transactionHash: BigInteger?,
    val name: String?,
    // This is the address that performs the recovery (e.g. the safe, multisend or a module)
    val recoverer: Solidity.Address,
    val data: String,
    val txGas: BigInteger,
    val dataGas: BigInteger,
    val gasToken: Solidity.Address,
    val gasPrice: BigInteger,
    val nonce: BigInteger,
    val operation: TransactionExecutionRepository.Operation,
    val signatures: List<Signature>
) :
    AbstractSafe() {

    override fun displayName(context: Context) = safeName(context, name)

}

private fun safeName(context: Context, name: String?): String = if (name.isNullOrBlank()) context.getString(R.string.your_safe) else name!!
