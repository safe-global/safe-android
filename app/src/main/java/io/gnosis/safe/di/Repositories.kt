package io.gnosis.safe.di

import dagger.Lazy
import io.gnosis.data.repositories.EnsRepository
import io.gnosis.data.repositories.SafeRepository
import pm.gnosis.ethereum.EthereumRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Repositories
@Inject constructor(
    private val safeRepository: Lazy<SafeRepository>,
    private val ethereumRepository: Lazy<EthereumRepository>,
    private val ensRepository: Lazy<EnsRepository>
) {

    fun safeRepository(): SafeRepository = safeRepository.get()

    fun ethereumRepository(): EthereumRepository = ethereumRepository.get()

    fun ensRepository(): EnsRepository = ensRepository.get()
}
