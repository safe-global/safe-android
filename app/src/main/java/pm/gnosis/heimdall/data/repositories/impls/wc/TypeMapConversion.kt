package pm.gnosis.heimdall.data.repositories.impls.wc

import pm.gnosis.heimdall.data.repositories.impls.Session


fun Session.PayloadAdapter.PeerData.intoMap(params: MutableMap<String, Any?> = mutableMapOf()) =
    params.also {
        it["peerId"] = this.id
        it["peerMeta"] = (this.meta?.let { meta ->
            mapOf(
                "description" to (meta.description ?: ""),
                "url" to (meta.url ?: ""),
                "name" to (meta.name ?: "")
                // TODO add missing params
            )
        } ?: emptyMap<String, Any>())
    }

fun Session.PayloadAdapter.SessionParams.intoMap(params: MutableMap<String, Any?> = mutableMapOf()) =
    params.also {
        it["approved"] = approved
        it["chainId"] = chainId
        it["accounts"] = accounts
        it["message"] = message
    }

fun Session.PayloadAdapter.Error.intoMap(params: MutableMap<String, Any?> = mutableMapOf()) =
    params.also {
        it["code"] = code
        it["message"] = message
    }
