package pm.gnosis.android.app.wallet.util.abi

interface SolidityType
class SolidityUInt8 : SolidityType
class SolidityUInt32 : SolidityType
class SolidityUInt64 : SolidityType
class SolidityUInt128 : SolidityType
class SolidityUInt256 : SolidityType
class SolidityInt8 : SolidityType
class SolidityInt32 : SolidityType
class SolidityInt64 : SolidityType
class SolidityInt128 : SolidityType
class SolidityInt256 : SolidityType
class SolidityAddress : SolidityType
class SolidityBool : SolidityType
class SolidityBytes : SolidityType
class SolidityString : SolidityType
class SolidityVoid : SolidityType


class SolidityFunctionArgument<out T : SolidityType>(val name: String, val type: T)
class SolidityFunction<out T : SolidityType>(val name: String, val arguments: List<SolidityFunctionArgument<SolidityType>> = emptyList(), val returnType: T)