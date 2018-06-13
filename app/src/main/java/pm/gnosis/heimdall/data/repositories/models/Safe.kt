package pm.gnosis.heimdall.data.repositories.models

import android.content.Context
import pm.gnosis.heimdall.R
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import java.math.BigInteger

sealed class AbstractSafe

data class Safe(val address: Solidity.Address, val name: String? = null) : AbstractSafe() {

    fun displayName(context: Context) = safeName(context, name)

}

data class PendingSafe(val hash: BigInteger, val name: String?, val address: Solidity.Address, val payment: Wei, val isFunded: Boolean = false) :
    AbstractSafe() {

    fun displayName(context: Context) = safeName(context, name)

}

private fun safeName(context: Context, name: String?): String = if (name.isNullOrEmpty()) context.getString(R.string.your_safe) else name!!
