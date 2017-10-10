package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.data.db.GnosisAuthenticatorDb
import pm.gnosis.heimdall.data.db.model.MultisigWalletDb
import pm.gnosis.heimdall.data.repositories.MultisigRepository
import pm.gnosis.heimdall.data.repositories.model.MultisigWallet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SqliteMultisigRepository @Inject constructor(
        gnosisAuthenticatorDb: GnosisAuthenticatorDb
) : MultisigRepository {

    private val multisigWalletDao = gnosisAuthenticatorDb.multisigWalletDao()

    override fun observeMultisigWallets() =
            multisigWalletDao.observeMultisigWallets()
                    .map { it.mapNotNull { wallet -> wallet.address?.let { MultisigWallet(it, wallet.name) } } }
                    .subscribeOn(Schedulers.io())

    override fun addMultisigWallet(address: String, name: String) =
            Completable.fromCallable {
                val multisigWallet = MultisigWalletDb(name, address)
                multisigWalletDao.insertMultisigWallet(multisigWallet)
            }.subscribeOn(Schedulers.io())

    override fun removeMultisigWallet(address: String) =
            Completable.fromCallable {
                multisigWalletDao.removeMultisigWallet(address)
            }.subscribeOn(Schedulers.io())

    override fun updateMultisigWalletName(address: String, newName: String) =
            Completable.fromCallable {
                val multisigWallet = MultisigWalletDb(newName, address)
                multisigWalletDao.updateMultisigWallet(multisigWallet)
            }.subscribeOn(Schedulers.io())
}