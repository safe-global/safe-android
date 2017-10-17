package pm.gnosis.heimdall.data.remote

import pm.gnosis.heimdall.data.model.JsonRpcRequest
import pm.gnosis.heimdall.data.model.JsonRpcResult

open class BulkRequest(vararg calls: SubRequest<out Any?>) {

    private val callMap = HashMap<Int, SubRequest<out Any?>>()

    init {
        calls.forEach {
            val id = it.params.id
            if (callMap.containsKey(id)) {
                throw IllegalStateException("Duplicate ids!")
            }
            callMap[id] = it
        }
    }

    fun body(): Collection<JsonRpcRequest> = callMap.map { it.value.params }

    fun parse(results: Collection<JsonRpcResult>) {
        results.forEach {
            callMap[it.id]?.parse(it)
        }
    }

    class SubRequest<D>(val params: JsonRpcRequest, private val adapter: (JsonRpcResult) -> D) {
        var value: D? = null

        fun parse(result: JsonRpcResult) {
            value = adapter(result)
        }
    }
}