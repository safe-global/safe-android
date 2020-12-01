package io.gnosis.safe.ui.dialogs

import androidx.lifecycle.ViewModel
import io.gnosis.data.repositories.EnsInvalidError
import io.gnosis.data.repositories.EnsRepository
import pm.gnosis.model.Solidity
import javax.inject.Inject

class EnsInputViewModel
@Inject constructor(
    private val ensRepository: EnsRepository
) : ViewModel() {

    suspend fun processEnsInput(input: CharSequence): Solidity.Address {
        return kotlin.runCatching {
            ensRepository.resolve(input.toString())
        }
            .onSuccess {
                it
            }
            .onFailure {
                when (it) {
                    is IllegalArgumentException -> {
                        throw EnsInvalidError()
                    }
                    else -> {
                        throw it
                    }
                }
            }
            .getOrNull()!!
    }
}
