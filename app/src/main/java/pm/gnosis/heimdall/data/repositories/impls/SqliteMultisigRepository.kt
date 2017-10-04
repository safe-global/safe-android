package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.data.db.GnosisAuthenticatorDb
import pm.gnosis.heimdall.data.db.MultisigWallet
import pm.gnosis.heimdall.data.repositories.MultisigRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SqliteMultisigRepository @Inject constructor(
        private val gnosisAuthenticatorDb: GnosisAuthenticatorDb
) : MultisigRepository {

    private val multisigWalletDao = gnosisAuthenticatorDb.multisigWalletDao()

    override fun observeMultisigWallets() =
            multisigWalletDao.observeMultisigWallets()
                    .subscribeOn(Schedulers.io())

    override fun addMultisigWallet(address: String, name: String) =
            Completable.fromCallable {
                val multisigWallet = MultisigWallet()
                multisigWallet.name = name
                multisigWallet.address = address
                multisigWalletDao.insertMultisigWallet(multisigWallet)
            }.subscribeOn(Schedulers.io())

    override fun removeMultisigWallet(multisigWallet: MultisigWallet) =
            Completable.fromCallable {
                multisigWalletDao.removeMultisigWallet(multisigWallet)
            }.subscribeOn(Schedulers.io())

    override fun updateMultisigWalletName(address: String, newName: String) =
            Completable.fromCallable {
                val multisigWallet = MultisigWallet()
                multisigWallet.name = newName
                multisigWallet.address = address
                multisigWalletDao.updateMultisigWallet(multisigWallet)
            }.subscribeOn(Schedulers.io())
}