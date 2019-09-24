package pm.gnosis.tests.utils

import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.RecoveringSafe
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.security.db.EncryptedByteArray
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger

fun testRecoveringSafe(
    address: Solidity.Address,
    transactionHash: BigInteger?,
    recoverer: Solidity.Address,
    data: String = "",
    txGas: BigInteger = BigInteger.ZERO,
    dataGas: BigInteger = BigInteger.ZERO,
    operationalGas: BigInteger = BigInteger.ZERO,
    gasToken: Solidity.Address = ERC20Token.ETHER_TOKEN.address,
    gasPrice: BigInteger = BigInteger.ZERO,
    nonce: BigInteger = BigInteger.ZERO,
    operation: TransactionExecutionRepository.Operation = TransactionExecutionRepository.Operation.CALL,
    signatures: List<Signature> = emptyList()
) =
    RecoveringSafe(address, transactionHash, recoverer, data, txGas, dataGas, operationalGas, gasToken, gasPrice, nonce, operation, signatures)


fun testTransaction(
    address: Solidity.Address,
    value: Wei? = null,
    data: String? = null
) =
    Transaction(
        address,
        value = value,
        data = data
    )

fun testSafeTransaction(
    address: Solidity.Address,
    transaction: Transaction = testTransaction(address),
    operation: TransactionExecutionRepository.Operation = TransactionExecutionRepository.Operation.CALL
) =
    SafeTransaction(transaction, operation)


private val encryptedByteArrayConverter = EncryptedByteArray.Converter()

fun Solidity.Address.asOwner() =
    AccountsRepository.SafeOwner(this, encryptedByteArrayConverter.fromStorage(asEthereumAddressString()))

