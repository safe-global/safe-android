package io.gnosis.safe.ui.dialogs

import androidx.lifecycle.ViewModel
import io.gnosis.data.repositories.EnsInvalidError
import io.gnosis.data.repositories.UnstoppableDomainsRepository
import pm.gnosis.model.Solidity
import javax.inject.Inject
import com.unstoppabledomains.exceptions.*;
import com.unstoppabledomains.exceptions.ns.NSExceptionCode
import com.unstoppabledomains.exceptions.ns.NamingServiceException
import java.util.concurrent.ExecutionException

class UnstoppableInputViewModel
@Inject constructor(
        private val unstoppableRepository: UnstoppableDomainsRepository
) : ViewModel() {

    suspend fun processInput(input: CharSequence): Solidity.Address {
        return kotlin.runCatching {
            unstoppableRepository.resolve(input.toString())
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
