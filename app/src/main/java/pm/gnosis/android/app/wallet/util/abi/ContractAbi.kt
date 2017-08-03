package pm.gnosis.android.app.wallet.util.abi

// TODO: given an ABI generate the given sources (at compile time)
interface ContractAbi {
    val functions: List<AbiFunction<*>>
}
