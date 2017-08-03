package pm.gnosis.android.app.wallet.util.abi

class ERC20TokenAbi : ContractAbi {
    override val functions
        get() = listOf(approve, totalSupply, transferFrom, balanceOf, transfer, allowance)

    val approve = SolidityFunction("approve", listOf(
            SolidityFunctionArgument("_spender", SolidityAddress()),
            SolidityFunctionArgument("_value", SolidityUInt256())), SolidityBool())

    val totalSupply = SolidityFunction("totalSupply", returnType = SolidityUInt256())

    val transferFrom = SolidityFunction("transferFrom", listOf(
            SolidityFunctionArgument("_from", SolidityAddress()),
            SolidityFunctionArgument("_to", SolidityAddress()),
            SolidityFunctionArgument("_value", SolidityUInt256())), SolidityBool())

    val balanceOf = SolidityFunction("balanceOf", listOf(
            SolidityFunctionArgument("_owner", SolidityAddress())), SolidityUInt256())

    val transfer = SolidityFunction("transfer", listOf(
            SolidityFunctionArgument("_to", SolidityAddress()),
            SolidityFunctionArgument("_value", SolidityUInt256())), SolidityBool())

    val allowance = SolidityFunction("allowance", listOf(
            SolidityFunctionArgument("_owner", SolidityAddress()),
            SolidityFunctionArgument("_spender", SolidityAddress())), SolidityUInt256())
}
