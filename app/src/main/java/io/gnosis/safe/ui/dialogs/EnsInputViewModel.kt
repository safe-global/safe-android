package io.gnosis.safe.ui.dialogs

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import io.gnosis.data.repositories.EnsRepository
import io.gnosis.safe.R
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
                when (it) {
                    is Offline -> {
                        throw it
                    }
                    is IllegalArgumentException -> {
                        throw EnsResolutionError(msgRes = R.string.ens_name_contains_illegal_character)
                    }
                    else -> {
                        throw EnsResolutionError(msg = it.cause?.localizedMessage ?: it.localizedMessage)
                    }
                }
            }
            .getOrNull()!!
    }
}

data class EnsResolutionError(val msg: String? = null, @StringRes val msgRes: Int = 0) : Throwable()
