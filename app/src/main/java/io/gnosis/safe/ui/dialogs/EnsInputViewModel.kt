package io.gnosis.safe.ui.dialogs

import androidx.lifecycle.ViewModel
import io.gnosis.data.repositories.EnsRepository
import io.gnosis.safe.di.Repositories
import pm.gnosis.model.Solidity
import javax.inject.Inject

class EnsInputViewModel
@Inject constructor(
    repositories: Repositories
) : ViewModel() {

    private val ensRepository: EnsRepository by lazy { repositories.ensRepository() }

    suspend fun processEnsInput(input: CharSequence): Solidity.Address = ensRepository.resolve(input.toString()) ?: throw AddressNotFound()
}

class AddressNotFound : Throwable()
