package io.gnosis.data.adapters

import com.squareup.moshi.JsonDataException
import io.gnosis.data.backend.dto.*
import io.gnosis.data.utils.formatBackendDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.threeten.bp.zone.TzdbZoneRulesProvider
import org.threeten.bp.zone.ZoneRulesProvider
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.stream.Collectors

class DataMoshiTest {
    init {
        initThreeTen()
    }
    private val moshi = dataMoshi
    private val adapter = moshi.adapter(TransactionDto::class.java)

    @Test
    fun `fromJson (multisig transaction) should return MultisigTransactionDto`() {
        val jsonString: String = readResource("multisig_transaction.json")

        val transactionDto = adapter.fromJson(jsonString)

        assertTrue(transactionDto is MultisigTransactionDto)
        println("transactionDto: $transactionDto")
        if (transactionDto is MultisigTransactionDto) {
            println(" transactionDto.executionDate: ${transactionDto.executionDate?.formatBackendDate()}")
            println("transactionDto.submissionDate: ${transactionDto.submissionDate?.formatBackendDate()}")
            println("      transactionDto.modified: ${transactionDto.modified?.formatBackendDate()}")
        }
    }

    @Test
    fun `fromJson (ethereum transaction) should return EthereumTransactionDto`() {
        val jsonString: String = readResource("ethereum_transaction.json")

        val transactionDto: TransactionDto? = adapter.fromJson(jsonString)

        assertTrue(transactionDto is EthereumTransactionDto)


    }

    @Test
    fun `fromJson (ethereum transaction with erc721 transfer) should return EthereumTransactionDto`() {
        val jsonString: String = readResource("ethereum_transaction_erc721_transfer.json")

        val transactionDto = adapter.fromJson(jsonString)

        assertTrue(transactionDto is EthereumTransactionDto)
    }

    @Test
    fun `fromJson (module transaction) should return ModuleTransactionDto`() {
        val jsonString: String = readResource("module_transaction.json")

        val transactionDto = adapter.fromJson(jsonString)

        assertTrue(transactionDto is ModuleTransactionDto)
    }

    @Test
    fun `fromJson (transaction with unknown txType) should return UnknownTransactionDto`() {
        val jsonString: String = readResource("unknown_transaction_type.json")

        val transactionDto = adapter.fromJson(jsonString)

        assertEquals(transactionDto, UnknownTransactionDto)
    }

    @Test(expected = JsonDataException::class)
    fun `fromJson (transaction with missing txType) should throw a JsonDataException`() {
        val jsonString: String = readResource("tx_type_missing_transaction.json")

        adapter.fromJson(jsonString)
    }

    private fun readResource(fileName: String): String {
        return BufferedReader(
            InputStreamReader(
                this::class.java.getClassLoader()?.getResourceAsStream(fileName)!!
            )
        ).lines().parallel().collect(Collectors.joining("\n"))
    }
}

fun Any.initThreeTen() {
    if (ZoneRulesProvider.getAvailableZoneIds().isEmpty()) {
        val stream = this.javaClass.classLoader!!.getResourceAsStream("TZDB.dat")
        stream.use(::TzdbZoneRulesProvider).apply {
            ZoneRulesProvider.registerProvider(this)
        }
    }
}
