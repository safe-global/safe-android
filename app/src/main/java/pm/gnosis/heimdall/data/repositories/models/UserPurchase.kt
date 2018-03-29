package pm.gnosis.heimdall.data.repositories.models


data class UserPurchase(val token: String, val orderId: String, val productId: String)
