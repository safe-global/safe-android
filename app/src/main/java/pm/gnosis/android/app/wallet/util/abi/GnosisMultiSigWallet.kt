package pm.gnosis.android.app.wallet.util.abi

class GnosisMultiSigWallet : ContractAbi {
    override val functions: List<SolidityFunction<*>>
        get() = listOf(addOwner, removeOwner, replaceOwner, changeRequirement, submitTransaction, confirmTransaction, revokeConfirmation, executeTransaction, isConfirmed)

    val addOwner = SolidityFunction("addOwner", listOf(SolidityFunctionArgument("owner", SolidityAddress())), SolidityVoid())
    val removeOwner = SolidityFunction("removeOwner", listOf(SolidityFunctionArgument("owner", SolidityAddress())), SolidityVoid())
    val replaceOwner = SolidityFunction("replaceOwner", listOf(
            SolidityFunctionArgument("owner", SolidityAddress()),
            SolidityFunctionArgument("newOwner", SolidityAddress())), SolidityVoid())
    val changeRequirement = SolidityFunction("changeRequirement", listOf(
            SolidityFunctionArgument("_required", SolidityUInt256())), SolidityVoid())
    val submitTransaction = SolidityFunction("submitTransaction", listOf(
            SolidityFunctionArgument("destination", SolidityAddress()),
            SolidityFunctionArgument("value", SolidityUInt256()),
            SolidityFunctionArgument("data", SolidityBytes())), SolidityUInt256())
    val confirmTransaction = SolidityFunction("confirmTransaction", listOf(
            SolidityFunctionArgument("transactionId", SolidityUInt256())), SolidityVoid())
    val revokeConfirmation = SolidityFunction("revokeConfirmation", listOf(
            SolidityFunctionArgument("transactionId", SolidityInt256())), SolidityVoid())
    val executeTransaction = SolidityFunction("executeTransaction", listOf(
            SolidityFunctionArgument("transactionId", SolidityInt256())), SolidityVoid())
    val isConfirmed = SolidityFunction("isConfirmed", listOf(
            SolidityFunctionArgument("transactionId", SolidityInt256())), SolidityBool())
}
