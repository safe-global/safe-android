package pm.gnosis.heimdall.ui.account

import android.arch.persistence.room.EmptyResultSetException
import android.content.Context
import io.reactivex.Observable
import io.reactivex.functions.Function
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Account
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import javax.inject.Inject

class AccountViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountsRepository: AccountsRepository,
    private val ethereumRepository: EthereumRepository,
    private val qrCodeGenerator: QrCodeGenerator
) : AccountContract() {

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context)
        .add({ it is EmptyResultSetException }, R.string.no_account_available)
        .build()

    override fun getQrCode(contents: String) = qrCodeGenerator.generateQrCode(contents).mapToResult()

    override fun getAccountAddress() =
        accountsRepository.loadActiveAccount()
            .toObservable()
            .onErrorResumeNext(Function { errorHandler.observable<Account>(it) })
            .mapToResult()

    override fun getAccountBalance(): Observable<Result<Wei>> =
        accountsRepository.loadActiveAccount()
            .flatMapObservable {
                ethereumRepository.getBalance(it.address)
                    .onErrorResumeNext(Function { errorHandler.observable<Wei>(it) })
                    .mapToResult()
            }
}
