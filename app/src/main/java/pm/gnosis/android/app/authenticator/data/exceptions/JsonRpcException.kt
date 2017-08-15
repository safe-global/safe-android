package pm.gnosis.android.app.authenticator.data.exceptions

import pm.gnosis.android.app.authenticator.data.model.JsonRpcError

class JsonRpcException(val jsonRpcError: JsonRpcError) : Exception() {
    val errorCode: Int?
        get() = jsonRpcError.code

    override val message: String?
        get() = jsonRpcError.message
}