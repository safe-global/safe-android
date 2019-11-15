package pm.gnosis.heimdall.ui.safe.pairing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.awaitFirst
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.di.modules.ApplicationModule
import pm.gnosis.heimdall.ui.safe.helpers.RecoverSafeOwnersHelper
import pm.gnosis.heimdall.utils.AuthenticatorInfo
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.model.Solidity
import pm.gnosis.utils.nullOnThrow
import timber.log.Timber
import java.math.BigInteger
import java.math.RoundingMode
import javax.inject.Inject

abstract class PairingStartContract : ViewModel() {

    abstract val observableState: LiveData<ViewUpdate>

    abstract fun setup(safeAddress: Solidity.Address)

    abstract fun loadAuthenticatorInfo(safeInfo: SafeInfo? = null)

    abstract fun estimate()

    sealed class ViewUpdate {

        data class Balance(
            val balanceBefore: String,
            val fee: String,
            val balanceAfter: String,
            val tokenSymbol: String,
            val sufficient: Boolean
        ) : ViewUpdate()

        data class Authenticator(
            val info: AuthenticatorSetupInfo
        ) : ViewUpdate()

        data class Error(
            val error: Throwable
        ) : ViewUpdate()
    }
}

class PairingStartViewModel @Inject constructor(
    private val recoverSafeOwnersHelper: RecoverSafeOwnersHelper,
    private val gnosisSafeRepository: GnosisSafeRepository,
    private val accountsRepository: AccountsRepository,
    private val tokenRepository: TokenRepository,
    private val transactionExecutionRepository: TransactionExecutionRepository,
    private val appDispatcher: ApplicationModule.AppCoroutineDispatchers
) : PairingStartContract() {

    private val errorHandler = CoroutineExceptionHandler { _, e ->
        viewModelScope.launch {
            _state.postValue(
                ViewUpdate.Error(e)
            )
            Timber.e(e)
        }
    }

    override val observableState: LiveData<ViewUpdate>
        get() = _state
    private val _state = MutableLiveData<ViewUpdate>()

    private lateinit var safeAddress: Solidity.Address
    private lateinit var safeInfo: SafeInfo

    override fun setup(safeAddress: Solidity.Address) {
        this.safeAddress = safeAddress
    }

    override fun estimate() {

        viewModelScope.launch(appDispatcher.background + errorHandler) {

            val paymentToken = tokenRepository.loadPaymentToken(safeAddress).await()

            safeInfo = gnosisSafeRepository.loadInfo(safeAddress).awaitFirst()

            val owner = accountsRepository.signingOwner(safeAddress).await()
            val extension = Solidity.Address(BigInteger.valueOf(Long.MAX_VALUE))
            val pairingTransaction = recoverSafeOwnersHelper.buildRecoverTransaction(
                safeInfo,
                safeInfo.owners.subList(2, safeInfo.owners.size).toSet(),
                setOf(owner.address, extension)
            )

            val safeOwner = accountsRepository.signingOwner(safeAddress).await()

            val executeInfo = transactionExecutionRepository.loadExecuteInformation(safeAddress, paymentToken.address, pairingTransaction, safeOwner).await()
            val gasFee = executeInfo.gasCosts()

            _state.postValue(
                ViewUpdate.Balance(
                    paymentToken.displayString(executeInfo.balance),
                    ERC20TokenWithBalance(paymentToken, gasFee).displayString(roundingMode = RoundingMode.UP),
                    paymentToken.displayString(executeInfo.balance - gasFee),
                    paymentToken.symbol,
                    executeInfo.balance > gasFee
                )
            )
        }
    }

    override fun loadAuthenticatorInfo(safeInfo: SafeInfo?) {

        viewModelScope.launch(appDispatcher.background + errorHandler) {

            val owners = safeInfo?.let { it.owners } ?: this@PairingStartViewModel.safeInfo.owners

            val owner = accountsRepository.signingOwner(safeAddress).await()

            val info = findAuthenticatorInfoFromOwners(owners - owner.address)

            val setupInfo = AuthenticatorSetupInfo(owner, info)

            _state.postValue(
                ViewUpdate.Authenticator(setupInfo)
            )
        }
    }

    private fun findAuthenticatorInfoFromOwners(owners: List<Solidity.Address>): AuthenticatorInfo {
        for (owner in owners) {
            nullOnThrow { gnosisSafeRepository.loadAuthenticatorInfo(owner) }?.let {
                return it
            }
        }
        return AuthenticatorInfo(AuthenticatorInfo.Type.EXTENSION, owners.first(), null)
    }
}
