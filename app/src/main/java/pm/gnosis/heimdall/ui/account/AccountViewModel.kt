package pm.gnosis.heimdall.ui.account

import android.arch.persistence.room.EmptyResultSetException
import android.content.Context
import io.reactivex.Observable
import io.reactivex.functions.Function
import io.reactivex.functions.Predicate
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.accounts.base.models.Account
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.util.QrCodeGenerator
import pm.gnosis.heimdall.common.util.mapToResult
import pm.gnosis.heimdall.data.model.Wei
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import javax.inject.Inject

class AccountViewModel @Inject constructor(
        @ApplicationContext private val context: Context,
        private val accountsRepository: AccountsRepository,
        private val ethereumJsonRpcRepository: EthereumJsonRpcRepository,
        private val qrCodeGenerator: QrCodeGenerator
) : AccountContract() {

    private val errorHandler = LocalizedException.networkErrorHandlerBuilder(context)
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
            ethereumJsonRpcRepository.getBalance()
                    .onErrorResumeNext(Function { errorHandler.observable<Wei>(it) })
                    .mapToResult()
}
