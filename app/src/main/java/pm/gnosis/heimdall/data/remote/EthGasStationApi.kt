package pm.gnosis.heimdall.data.remote

import io.reactivex.Observable
import pm.gnosis.heimdall.data.remote.models.EthGasStationPrices
import retrofit2.http.GET


interface EthGasStationApi {

    companion object {
        const val BASE_URL = "https://ethgasstation.info/json/"
    }

    @GET("ethgasAPI.json")
    fun loadGasPrices(): Observable<EthGasStationPrices>
}