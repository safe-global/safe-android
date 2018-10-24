package pm.gnosis.tests.utils

import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.RecoveringSafe
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
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
