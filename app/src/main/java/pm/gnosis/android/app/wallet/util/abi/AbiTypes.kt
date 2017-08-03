package pm.gnosis.android.app.wallet.util.abi

interface AbiType
class AbiUInt8 : AbiType
class AbiUInt32 : AbiType
class AbiUInt64 : AbiType
class AbiUInt128 : AbiType
class AbiUInt256 : AbiType
class AbiInt8 : AbiType
class AbiInt32 : AbiType
class AbiInt64 : AbiType
class AbiInt128 : AbiType
class AbiInt256 : AbiType
class AbiAddress : AbiType
class AbiBool : AbiType
class AbiBytes : AbiType
class AbiString : AbiType


class AbiFunctionArgument<out T : AbiType>(val name: String, val type: T)
class AbiFunction<out T : AbiType>(val arguments: List<AbiFunctionArgument<AbiType>> = emptyList(), val returnType: T)