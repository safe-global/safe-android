package io.gnosis.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import io.gnosis.data.BuildConfig
import io.gnosis.data.models.Chain.Companion.TABLE_NAME
import pm.gnosis.utils.nullOnThrow
import java.io.Serializable
import java.math.BigInteger
import java.net.URI

@Entity(tableName = TABLE_NAME)
data class Chain(
    @PrimaryKey
    @ColumnInfo(name = COL_CHAIN_ID)
    val chainId: BigInteger,

    @ColumnInfo(name = COL_CHAIN_L2)
    val l2: Boolean,

    @ColumnInfo(name = COL_CHAIN_NAME)
    val name: String,

    @ColumnInfo(name = COL_CHAIN_SHORT_NAME)
    val shortName: String,

    @ColumnInfo(name = COL_TEXT_COLOR)
    val textColor: String,

    @ColumnInfo(name = COL_BACKGROUND_COLOR)
    val backgroundColor: String,

    @ColumnInfo(name = COL_RPC_URI)
    val rpcUri: String,

    @ColumnInfo(name = COL_RPC_AUTHENTICATION)
    val rpcAuthentication: RpcAuthentication,

    @ColumnInfo(name = COL_BLOCK_EXPLORER_TEMPLATE_ADDRESS)
    val blockExplorerTemplateAddress: String,

    @ColumnInfo(name = COL_BLOCK_EXPLORER_TEMPLATE_TX_HASH)
    val blockExplorerTemplateTxHash: String,

    @ColumnInfo(name = COL_ENS_REGISTRY_ADDRESS)
    val ensRegistryAddress: String?,

    @ColumnInfo(name = COL_FEATURES)
    val features: List<Feature>
) : Serializable {

    @Ignore
    var currency: Currency = Currency.DEFAULT_CURRENCY

    @Entity(
        tableName = Currency.TABLE_NAME,
        foreignKeys = [
            ForeignKey(
                entity = Chain::class,
                parentColumns = [Chain.COL_CHAIN_ID],
                childColumns = [Currency.COL_CHAIN_ID],
                onUpdate = ForeignKey.CASCADE,
                onDelete = ForeignKey.CASCADE
            )
        ]
    )
    data class Currency(
        @PrimaryKey
        @ColumnInfo(name = Currency.COL_CHAIN_ID)
        val chainId: BigInteger,

        @ColumnInfo(name = Currency.COL_NAME)
        val name: String,

        @ColumnInfo(name = Currency.COL_SYMBOL)
        val symbol: String,

        @ColumnInfo(name = Currency.COL_DECIMALS)
        val decimals: Int,

        @ColumnInfo(name = Currency.COL_LOGO_URI)
        val logoUri: String
    ) : Serializable {

        companion object {
            const val TABLE_NAME = "native_currency"
            const val COL_CHAIN_ID = "chain_id"
            const val COL_NAME = "name"
            const val COL_SYMBOL = "symbol"
            const val COL_DECIMALS = "decimals"
            const val COL_LOGO_URI = "logo_uri"

            val DEFAULT_CURRENCY = Currency(
                BuildConfig.CHAIN_ID.toBigInteger(),
                BuildConfig.NATIVE_CURRENCY_NAME,
                BuildConfig.NATIVE_CURRENCY_SYMBOL,
                18,
                "local::native_currency"
            )
        }
    }

    enum class Feature {
        EIP1559
        //TODO: add more features
    }

    companion object {
        const val TABLE_NAME = "chains"

        const val COL_CHAIN_NAME = "chain_name"
        const val COL_CHAIN_SHORT_NAME = "chain_short_name"
        const val COL_CHAIN_ID = "chain_id"
        const val COL_CHAIN_L2 = "l2"
        const val COL_BACKGROUND_COLOR = "background_color"
        const val COL_RPC_URI = "rpc_uri"
        const val COL_RPC_AUTHENTICATION = "rpc_authentication"
        const val COL_BLOCK_EXPLORER_TEMPLATE_ADDRESS = "block_explorer_address_uri"
        const val COL_BLOCK_EXPLORER_TEMPLATE_TX_HASH = "block_explorer_tx_hash_uri"
        const val COL_ENS_REGISTRY_ADDRESS = "ens_registry_address"
        const val COL_TEXT_COLOR = "text_color"
        const val COL_FEATURES = "features"

        val ID_MAINNET = BigInteger.valueOf(1)
        val ID_GOERLI = BigInteger.valueOf(5)
        val ID_GNOSIS = BigInteger.valueOf(100)

        val DEFAULT_CHAIN =  Chain(
            BuildConfig.CHAIN_ID.toBigInteger(),
            BuildConfig.CHAIN_L2,
            BuildConfig.BLOCKCHAIN_NAME,
            BuildConfig.BLOCKCHAIN_SHORT_NAME,
            BuildConfig.CHAIN_TEXT_COLOR,
            BuildConfig.CHAIN_BACKGROUND_COLOR,
            BuildConfig.BLOCKCHAIN_EXPLORER_URL,
            RpcAuthentication.API_KEY_PATH,
            BuildConfig.BLOCKCHAIN_NET_URL + "address/",
            BuildConfig.BLOCKCHAIN_NET_URL + "tx/",
            io.gnosis.contracts.BuildConfig.ENS_REGISTRY,
            BuildConfig.CHAIN_FEATURES.mapNotNull { nullOnThrow { Feature.valueOf(it) } }
        ).apply {
            currency = Currency.DEFAULT_CURRENCY
        }
    }
}

fun Chain.baseRpcUrl(): String {
    return if (rpcAuthentication == RpcAuthentication.API_KEY_PATH) {
        URI.create(rpcUri).resolve(BuildConfig.INFURA_API_KEY).toString()
    } else {
        rpcUri
    }
}
