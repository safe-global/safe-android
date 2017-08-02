package pm.gnosis.android.app.wallet.util.abi

interface AbiType
class UInt8
class UInt32
class UInt64
class UInt128
class UInt256
class Int8
class Int32
class Int64
class Int128
class Int256
class Address
class Bool
class Bytes
class String


class AbiFunctionArgument<out T : AbiType>(val name: String, val type: T)
class AbiFunction(val arguments: List<AbiFunctionArgument<AbiType>> = emptyList())