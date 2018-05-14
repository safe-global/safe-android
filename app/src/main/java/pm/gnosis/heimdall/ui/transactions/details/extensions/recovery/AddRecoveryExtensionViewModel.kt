package pm.gnosis.heimdall.ui.transactions.details.extensions.recovery

import io.reactivex.ObservableTransformer
import io.reactivex.Single
import okio.ByteString
import pm.gnosis.crypto.KeyGenerator
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AddRecoveryExtensionData
import pm.gnosis.heimdall.data.repositories.GnosisSafeExtensionRepository
import pm.gnosis.heimdall.data.repositories.TransactionDetailsRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.asBigInteger
import java.math.BigInteger
import javax.inject.Inject


class AddRecoveryExtensionViewModel @Inject constructor(
    private val bip39: Bip39,
    private val extensionRepository: GnosisSafeExtensionRepository,
    private val detailsRepository: TransactionDetailsRepository
) : AddRecoveryExtensionContract() {

    private var cachedSetupInfo: Pair<Solidity.Address, String>? = null

    override fun loadCreateRecoveryInfo(): Single<Pair<Solidity.Address, String>> =
        cachedSetupInfo?.let { Single.just(it) }
                ?: Single.fromCallable {
                    val mnemonic = bip39.generateMnemonic(languageId = R.id.english)
                    val hdNode = KeyGenerator.masterNode(ByteString.of(*bip39.mnemonicToSeed(mnemonic)))
                    val key = hdNode.derive(KeyGenerator.BIP44_PATH_ETHEREUM).deriveChild(0).keyPair
                    Solidity.Address(key.address.asBigInteger()) to mnemonic
                }
                    .doOnSuccess {
                        cachedSetupInfo = it
                    }

    override fun loadRecoveryOwner(transaction: Transaction?): Single<Pair<Solidity.Address, BigInteger>> =
        transaction?.let {
            detailsRepository.loadTransactionData(transaction)
                .map {
                    val data = it.toNullable()
                    when (data) {
                        is AddRecoveryExtensionData -> data.recoveryOwner to data.timeout
                        else -> throw IllegalStateException()
                    }
                }
        } ?: Single.error<Pair<Solidity.Address, BigInteger>>(IllegalStateException())

    override fun inputTransformer(safeAddress: Solidity.Address?) = ObservableTransformer<Solidity.Address, Result<SafeTransaction>> {
        it.flatMapSingle {
            extensionRepository.buildAddRecoverExtensionTransaction(it)
                .mapToResult()
        }
    }
}
