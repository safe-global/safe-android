package pm.gnosis.heimdall.ui.account

import android.arch.persistence.room.EmptyResultSetException
import android.content.Context
import io.reactivex.functions.Function
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.accounts.base.models.Account
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.utils.QrCodeGenerator
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.models.Wei
import javax.inject.Inject

class AccountViewModel @Inject constructor(
        @ApplicationContext private val context: Context,
        private val accountsRepository: AccountsRepository,
        private val ethereumJsonRpcRepository: EthereumJsonRpcRepository,
        private val qrCodeGenerator: QrCodeGenerator
) : AccountContract() {

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context)
            .add({ it is EmptyResultSetException }, R.string.no_account_available)
            .build()

    override fun getQrCode(contents: String) =
            qrCodeGenerator.generateQrCode(contents)
                    .mapToResult()

    override fun getAccountAddress() =
            accountsRepository.loadActiveAccount()
                    .toObservable()
                    .onErrorResumeNext(Function { errorHandler.observable<Account>(it) })
                    .mapToResult()

    override fun getAccountBalance() =
            accountsRepository.loadActiveAccount().flatMapObservable {
                ethereumJsonRpcRepository.getBalance(it.address)
                        .onErrorResumeNext(Function { errorHandler.observable<Wei>(it) })
                        .mapToResult()
            }
}
