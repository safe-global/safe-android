package pm.gnosis.heimdall.ui.safe.recover.address

import android.arch.persistence.room.EmptyResultSetException
import android.content.Context
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.exceptions.InvalidAddressException
import javax.inject.Inject

class CheckSafeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safeRepository: GnosisSafeRepository
) : CheckSafeContract() {

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context).build()

    override fun checkSafe(address: CharSequence): Single<Result<Boolean>> =
        Single.fromCallable {
            address.toString().asEthereumAddress()?.apply {
                if (!asEthereumAddressString().equals(address.toString(), true)) throw InvalidAddressException()
            } ?: throw InvalidAddressException()
        }
            .subscribeOn(Schedulers.computation())
            .flatMap(::checkSafeExists)
            .flatMap { safeRepository.checkSafe(it).firstOrError() }
            .onErrorResumeNext {
                when (it) {
                    is InvalidAddressException -> Single.just(false)
                    else -> errorHandler.single(it)
                }
            }
            .mapToResult()

    private fun checkSafeExists(address: Solidity.Address) =
        safeRepository.loadSafe(address).map { it as AbstractSafe }
            .onErrorResumeNext {
                safeRepository.loadPendingSafe(address)
            }
            .onErrorResumeNext {
                safeRepository.loadRecoveringSafe(address)
            }
            .map<Solidity.Address> { throw SimpleLocalizedException(context.getString(R.string.safe_already_exists)) }
            .onErrorResumeNext {
                when (it) {
                    is EmptyResultSetException -> Single.just(address)
                    else -> Single.error(it)
                }
            }
}
