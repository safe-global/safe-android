package pm.gnosis.android.app.wallet.util.abi

class ERC20TokenAbi : ContractAbi {
    override val functions
        get() = listOf(approve)


    val approve = AbiFunction(listOf(
            AbiFunctionArgument("_spender", AbiAddress()),
            AbiFunctionArgument("_value", AbiUInt256())), AbiBool())

}