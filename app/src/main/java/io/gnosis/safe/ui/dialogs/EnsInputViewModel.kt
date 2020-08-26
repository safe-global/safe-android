package io.gnosis.safe.ui.dialogs

import androidx.lifecycle.ViewModel
import io.gnosis.data.repositories.EnsRepository
import io.gnosis.safe.helpers.Offline
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
                it ?: throw EnsResolutionError()
            }
            .onFailure {
                if (it is Offline) {
                    throw it
                } else {
                    throw EnsResolutionError(it.cause?.localizedMessage ?: it.localizedMessage)
                }
            }
            .getOrNull()!!
    }
}

class EnsResolutionError(val msg: String? = null) : Throwable()
