package io.gnosis.data.models

import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.gnosis.data.adapters.OperationEnumAdapter
import io.gnosis.data.db.BigDecimalNumberAdapter
import org.junit.Before
import org.junit.Test
import pm.gnosis.common.adapters.moshi.MoshiBuilderFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.stream.Collectors

class MultisigTransactionDtoTest {

    private var moshi = MoshiBuilderFactory.makeMoshiBuilder()
        .add(BigDecimalNumberAdapter())
        .add(OperationEnumAdapter())
//        .add(TransactionType::class.java, EnumJsonAdapter.create(TransactionType::class.java).withUnknownFallback(TransactionType.UNKNOWN))
        .add(transactionDtoJsonAdapterFactory)
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(TransactionDto::class.java)

    @Before
    fun setup() {

    }

    @Test
    fun `deserialze transaction`() {

        val jsonString: String = readResource("multisig_transaction.json")

        val transactionDto = adapter.fromJson(jsonString)

        println("transactionDto: $transactionDto")

    }
    @Test
    fun `deserialze transaction with unknown txType`() {

        val jsonString: String = readResource("unknown_transaction_type.json")

        val transactionDto = adapter.fromJson(jsonString)

        println("transactionDto: $transactionDto")

    }
    private fun readResource(fileName: String): String {
        return BufferedReader(
            InputStreamReader(
                this::class.java.getClassLoader()?.getResourceAsStream(fileName)!!
            )
        ).lines().parallel().collect(Collectors.joining("\n"))
    }

}
