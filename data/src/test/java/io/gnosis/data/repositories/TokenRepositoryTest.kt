package io.gnosis.data.repositories

import io.gnosis.data.db.daos.Erc20TokenDao
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.*
import org.junit.Test

class TokenRepositoryTest {

    private val erc20TokenDao = mockk<Erc20TokenDao>()
    private val tokenRepository = TokenRepository(erc20TokenDao)

    @Test
    fun `loadTokens (token addresses) should return list balances`() = runBlockingTest {


        tokenRepository.loadTokens()
    }
}
