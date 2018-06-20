package pm.gnosis.heimdall.data.repositories.models

import android.content.Context
import pm.gnosis.heimdall.R
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import java.math.BigInteger

sealed class AbstractSafe {
    abstract fun displayName(context: Context): String
}

data class Safe(val address: Solidity.Address, val name: String? = null) : AbstractSafe() {

    override fun displayName(context: Context) = safeName(context, name)

}

data class PendingSafe(val hash: BigInteger, val name: String?, val address: Solidity.Address, val payment: Wei, val isFunded: Boolean = false) :
    AbstractSafe() {

    override fun displayName(context: Context) = safeName(context, name)

}

private fun safeName(context: Context, name: String?): String = if (name.isNullOrBlank()) context.getString(R.string.your_safe) else name!!
