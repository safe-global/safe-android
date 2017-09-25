package pm.gnosis.heimdall.accounts.repositories.impl

import io.reactivex.Completable
import io.reactivex.Single
import okio.ByteString
import org.ethereum.geth.Address
import org.ethereum.geth.BigInt
import org.ethereum.geth.Geth
import org.ethereum.geth.KeyStore
import pm.gnosis.heimdall.accounts.models.Account
import pm.gnosis.heimdall.accounts.models.Transaction
import pm.gnosis.heimdall.accounts.repositories.AccountsRepository
import pm.gnosis.heimdall.common.PreferencesManager
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.heimdall.common.util.edit
import pm.gnosis.crypto.KeyGenerator
import pm.gnosis.utils.generateRandomString
import pm.gnosis.utils.toHexString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GethAccountsRepository @Inject constructor(
        private val gethAccountManager: GethAccountManager,
        private val gethKeyStore: KeyStore,
        private val preferencesManager: PreferencesManager
) : AccountsRepository {
    override fun loadActiveAccount(): Single<Account> {
        return Single.fromCallable {
            gethAccountManager.getActiveAccount()
        }.map {
            Account(it.address.hex)
        }
    }

    override fun signTransaction(transaction: Transaction): Single<String> {
        return Single.fromCallable {
            val account = gethAccountManager.getActiveAccount()
            val tx = Geth.newTransaction(
                    transaction.nonce.toLong(),
                    Address(transaction.to.asEthereumAddressString()),
                    BigInt(transaction.value.toLong()),
                    BigInt(transaction.adjustedStartGas.toLong()),
                    BigInt(transaction.gasPrice.toLong()),
                    transaction.data)


            val signed = gethKeyStore.signTxPassphrase(
                    account, gethAccountManager.getAccountPassphrase(), tx, BigInt(transaction.chainId.toLong()))

            signed.encodeRLP().toHexString()
        }
    }

    override fun saveAccountFromMnemonic(mnemonic: String, accountIndex: Long): Completable =
            Single.fromCallable {
                val hdNode = KeyGenerator().masterNode(ByteString.of(*pm.gnosis.mnemonic.Bip39.mnemonicToSeed(mnemonic)))
                hdNode.derive(KeyGenerator.BIP44_PATH_ETHEREUM).deriveChild(accountIndex).keyPair
            }.flatMapCompletable {
                saveAccount(it.privKeyBytes ?: throw IllegalStateException("Private key must not be null"))
            }


    override fun saveMnemonic(mnemonic: String): Completable = Completable.fromCallable {
        preferencesManager.prefs.edit {
            //TODO: in the future this needs to be encrypted
            putString(PreferencesManager.MNEMONIC_KEY, mnemonic)
        }
    }

    override fun saveAccount(privateKey: ByteArray): Completable =
            Completable.fromCallable {
                gethKeyStore.importECDSAKey(privateKey, generateRandomString())
            }
}
