package pm.gnosis.heimdall.ui.safe.create

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import pm.gnosis.crypto.ECDSASignature
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.data.remote.RelayServiceApi
import pm.gnosis.heimdall.data.remote.models.RelaySafeCreation
import pm.gnosis.heimdall.data.remote.models.RelaySafeCreationParams
import pm.gnosis.heimdall.data.remote.models.push.ServiceSignature
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.accounts.utils.hash
import pm.gnosis.svalinn.utils.ethereum.getDeployAddressFromNonce
import pm.gnosis.utils.*
import java.math.BigInteger
import java.security.SecureRandom
import javax.inject.Inject

class CreateSafeConfirmRecoveryPhraseViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val bip39: Bip39,
    private val relayServiceApi: RelayServiceApi,
    private val gnosisSafeRepository: GnosisSafeRepository
) : CreateSafeConfirmRecoveryPhraseContract() {

    private var browserExtensionAddress: Solidity.Address? = null
    private val secureRandom = SecureRandom()

    private val threshold get() = browserExtensionAddress?.let { 2 } ?: 1

    override fun setup(browserExtensionAddress: Solidity.Address?) {
        this.browserExtensionAddress = browserExtensionAddress
    }

    override fun createSafe(): Single<Solidity.Address> =
        loadSafeOwners()
            .map {
                RelaySafeCreationParams(
                    owners = it,
                    threshold = threshold,
                    s = BigInteger(252, secureRandom)
                )
            }
            .flatMap { request -> relayServiceApi.safeCreation(request).map { request to it } }
            // Check returned s parameter
            .map { (request, response) ->
                assertResponse(request, response)
                response to Transaction(
                    address = Solidity.Address(BigInteger.ZERO),
                    gas = response.tx.gas,
                    gasPrice = response.tx.gasPrice,
                    data = response.tx.data,
                    nonce = response.tx.nonce
                )
            }
            // Check returned safe address
            .flatMap { (response, tx) ->
                checkGeneratedSafeAddress(tx.hash(), response.signature, response.safe).andThen(
                    Single.just(response to tx)
                )
            }
            .flatMap { (response, tx) ->
                val transactionHash =
                    tx.hash(ECDSASignature(response.signature.r, response.signature.s).apply { v = response.signature.v.toByte() }).asBigInteger()
                gnosisSafeRepository.addPendingSafe(response.safe, transactionHash, null, response.payment)
                    .andThen(Single.just(response.safe))
            }
            .subscribeOn(Schedulers.io())

    private fun assertResponse(request: RelaySafeCreationParams, response: RelaySafeCreation) {
        if (request.s != response.signature.s)
            throw IllegalStateException("Client provided parameter s does not match the one returned by the service")
        if (response.tx.value.value != BigInteger.ZERO)
            throw IllegalStateException("Creation transaction should not require value")
        if (response.tx.nonce != BigInteger.ZERO)
            throw IllegalStateException("Creation transaction should have nonce zero")
        if (response.tx.gasPrice > MAX_GAS_PRICE)
            throw IllegalStateException("Creation transaction should not have a gasPrice higher than $MAX_GAS_PRICE")
        // Web3py seems to add an empty body for empty data, I would say this is a bug
        // see https://github.com/gnosis/bivrost-kotlin/issues/49
        // TODO: Add constructor encoding/decoding to bifrost. Then split the data in init data and constructor and decode the constructor to validate
        val safeSetup = GnosisSafe.Setup.encode(
            _owners = SolidityBase.Vector(request.owners),
            _threshold = Solidity.UInt256(request.threshold.toBigInteger()),
            to = Solidity.Address(BigInteger.ZERO),
            data = Solidity.Bytes(byteArrayOf())
        ) + "0000000000000000000000000000000000000000000000000000000000000000"
        val expectedConstructor = SolidityBase.encodeTuple(listOf(
            MATER_COPY_ADDRESS, Solidity.Bytes(safeSetup.hexToByteArray()),
            FUNDER_ADDRESS, ERC20Token.ETHER_TOKEN.address, Solidity.UInt256(response.payment.value)
        ))
        val responseData = response.tx.data.removeHexPrefix()
        val contractData = responseData.removeSuffix(expectedConstructor)
        // Check if the constructor data could be removed
        if (contractData == responseData)
            throw IllegalStateException("Unexpected proxy constructor data")
        val initDataLength = contractData.length - SOLIDITY_SWARM_INFO_LENGTH
        if (initDataLength != BuildConfig.PROXY_INIT_DATA_LENGTH)
            throw IllegalStateException("Unexpected proxy init data")
        val initData = contractData.substring(0, initDataLength)
        val initDataHash = Sha3Utils.keccak(initData.toByteArray()).toHex()
        if (initDataHash != BuildConfig.PROXY_INIT_DATA_HASH)
            throw IllegalStateException("Unexpected proxy init data")
        // TODO: we could check the swarm hash to make sure that the bytecode was generated by the same source code and same compiler
    }

    private fun checkGeneratedSafeAddress(transactionHash: ByteArray, signature: ServiceSignature, safeAddress: Solidity.Address) =
        accountsRepository.recover(transactionHash, signature.toSignature())
            .flatMapCompletable {
                Completable.fromCallable {
                    if (getDeployAddressFromNonce(
                            it,
                            BigInteger.ZERO
                        ).value != safeAddress.value
                    ) throw IllegalStateException("Address returned does not match address from signature")
                }
            }
            .subscribeOn(Schedulers.computation())

    private fun loadSafeOwners() =
        accountsRepository.loadActiveAccount().map { it.address }
            .flatMap { deviceAddress ->
                val mnemonicSeed = bip39.mnemonicToSeed(getRecoveryPhrase())
                Single.zip(
                    accountsRepository.accountFromMnemonicSeed(mnemonicSeed, 0).map { it.first },
                    accountsRepository.accountFromMnemonicSeed(mnemonicSeed, 1).map { it.first },
                    BiFunction<Solidity.Address, Solidity.Address, List<Solidity.Address>> { recoveryAccount1, recoveryAccount2 ->
                        listOfNotNull(
                            deviceAddress,
                            browserExtensionAddress,
                            recoveryAccount1,
                            recoveryAccount2
                        )
                    }
                )
            }

    companion object {
        private const val SOLIDITY_SWARM_INFO_LENGTH = 86
        private val MAX_GAS_PRICE = "200000000000".toBigInteger() // 200 GWei
        private val MATER_COPY_ADDRESS = BuildConfig.CURRENT_SAFE_MASTER_COPY_ADDRESS.asEthereumAddress()!!
        private val FUNDER_ADDRESS = BuildConfig.SAFE_CREATION_FUNDER.asEthereumAddress()!!
    }
}
