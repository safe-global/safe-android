package io.gnosis.safe.ui.dialogs

import androidx.lifecycle.ViewModel
import io.gnosis.data.repositories.EnsRepository
import pm.gnosis.model.Solidity
import javax.inject.Inject

class EnsInputViewModel
@Inject constructor(
    private val ensRepository: EnsRepository
) : ViewModel() {

    suspend fun processEnsInput(input: CharSequence): Solidity.Address = ensRepository.resolve(input.toString()) ?: throw AddressNotFound()
}

class AddressNotFound : Throwable()
