package pm.gnosis.heimdall.ui.account

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.heimdall.common.util.generateQrCode
import pm.gnosis.heimdall.common.util.mapToResult
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import javax.inject.Inject

class AccountViewModel @Inject constructor(
        private val accountsRepository: AccountsRepository,
        private val ethereumJsonRpcRepository: EthereumJsonRpcRepository
): AccountContract() {

    override fun getQrCode(contents: String) =
            Observable.fromCallable {
                generateQrCode(contents)
            }
                    .subscribeOn(Schedulers.computation())
                    .mapToResult()

    override fun getAccountAddress() =
            accountsRepository.loadActiveAccount()
                    .toObservable()
                    .mapToResult()

    override fun getAccountBalance() =
            ethereumJsonRpcRepository.getBalance()
            .mapToResult()
}
