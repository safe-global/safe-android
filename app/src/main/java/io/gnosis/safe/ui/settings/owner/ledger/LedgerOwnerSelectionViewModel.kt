package io.gnosis.safe.ui.settings.owner.ledger

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import pm.gnosis.model.Solidity
import timber.log.Timber
import javax.inject.Inject

class LedgerOwnerSelectionViewModel
@Inject constructor(
    private val ownersPager: LedgerOwnerPagingProvider,
    private val ledgerController: LedgerController,
    private val credentialsRepository: CredentialsRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<OwnerSelectionState>(appDispatchers) {

    private var ownerIndex: Long = 0
    private var derivationPath: String = ""

    override fun initialState() = OwnerSelectionState(ViewAction.Loading(true))

    private val device = ledgerController.connectedDevice!!

    fun loadOwners(context: Context, derivationPath: String) {

        Timber.i("--> loadOwners called")

        this.derivationPath = derivationPath
        if (!ledgerController.isConnected()) {
            ledgerController.connectToDevice(context, device, object : LedgerController.DeviceConnectedCallback {
                override fun onDeviceConnected(device: BluetoothDevice) {
                    safeLaunch {
                        getOwners(derivationPath)
                            .collectLatest {
                                updateState { OwnerSelectionState(DerivedOwners(it, derivationPath)) }
                            }
                    }
                }
            })
        } else {
            safeLaunch {
                getOwners(derivationPath)
                    .collectLatest {
                        updateState { OwnerSelectionState(DerivedOwners(it, derivationPath)) }
                    }
            }
        }
    }

    private fun getOwners(derivationPath: String): Flow<PagingData<OwnerHolder>> {
        val ownerItems = ownersPager.getOwnersStream(derivationPath)
            .map {
                it.map { address ->
                    val name = credentialsRepository.owner(address)?.name
                    OwnerHolder(address, name, name != null)
                }
            }
            .cachedIn(viewModelScope)
        return ownerItems
    }

    fun setOwnerIndex(index: Long, address: Solidity.Address) {
        ownerIndex = index
        safeLaunch {
            updateState {
                OwnerSelectionState(
                    OwnerSelected(
                        selectedOwner = OwnerHolder(
                            address = address,
                            name = null,
                            disabled = false
                        ), derivationPathWithIndex = derivationPath.replace(oldValue = "{index}", newValue = "$index")
                    )
                )
            }
        }
    }

    fun disconnectFromDevice() {
        ledgerController.teardownConnection()
    }

    fun reconnect(context: Context) {
        device?.let {
            ledgerController.connectToDevice(context, device, object : LedgerController.DeviceConnectedCallback {
                override fun onDeviceConnected(device: BluetoothDevice) {
                    Timber.i("---> device: $device")
                    loadOwners(context, derivationPath)
                }
            })
        } ?: Timber.d("Bluetooth device was null")
    }

    fun isConnected(): Boolean {
        device?.let {
            return ledgerController.isConnected()
        }
        return false
    }
}

data class OwnerSelectionState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class DerivedOwners(
    val newOwners: PagingData<OwnerHolder>,
    val derivationPath: String
) : BaseStateViewModel.ViewAction

data class OwnerSelected(
    val selectedOwner: OwnerHolder,
    val derivationPathWithIndex: String
) : BaseStateViewModel.ViewAction
