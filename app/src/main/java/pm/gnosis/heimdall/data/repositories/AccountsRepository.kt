package pm.gnosis.heimdall.data.repositories

import android.os.Parcelable
import io.reactivex.Completable
import io.reactivex.Single
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.TypeParceler
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.utils.EncryptedByteArrayParceler
import pm.gnosis.heimdall.utils.SolidityAddressParceler
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.security.db.EncryptedByteArray

interface AccountsRepository {

    fun owners(): Single<List<SafeOwner>>

    fun createOwner(): Single<SafeOwner>

    fun createOwnersFromPhrase(phrase: String, ids: List<Long>): Single<List<SafeOwner>>

    fun saveOwner(safeAddress: Solidity.Address, safeOwner: SafeOwner, paymentToken: ERC20Token): Completable

    fun signingOwner(safeAddress: Solidity.Address): Single<SafeOwner>

    fun sign(safeAddress: Solidity.Address, data: ByteArray): Single<Signature>

    fun sign(safeOwner: SafeOwner, data: ByteArray): Single<Signature>

    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    @TypeParceler<EncryptedByteArray, EncryptedByteArrayParceler>
    data class SafeOwner(val address: Solidity.Address, val privateKey: EncryptedByteArray) : Parcelable
}
