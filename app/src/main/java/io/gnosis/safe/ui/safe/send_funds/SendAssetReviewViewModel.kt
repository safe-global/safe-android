package io.gnosis.safe.ui.safe.send_funds

import io.gnosis.contracts.ERC20Contract
import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.Safe
import io.gnosis.data.models.transaction.*
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.data.utils.SemVer
import io.gnosis.data.utils.calculateSafeTxHash
import io.gnosis.safe.ui.assets.coins.CoinsViewData
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.ui.transactions.details.MissingOwnerCredential
import io.gnosis.safe.ui.transactions.details.SigningMode
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.toHexString
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject

class SendAssetReviewViewModel
@Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val safeRepository: SafeRepository,
    private val credentialsRepository: CredentialsRepository,
    private val settingsHandler: SettingsHandler,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SendAssetReviewState>(appDispatchers) {

    lateinit var activeSafe: Safe
        private set

    private lateinit var fromAddress: Solidity.Address
    private lateinit var toAddress: Solidity.Address
    private lateinit var transferAmount: BigInteger
    private lateinit var transferAddress: Solidity.Address
    private lateinit var safeTxHash: String
    private lateinit var data: String
    private lateinit var txExecutionInfo: DetailedExecutionInfo.MultisigExecutionDetails

    private lateinit var amountString: String
    private lateinit var selectedAsset: CoinsViewData.CoinBalance

    private var transferValue = BigInteger.ZERO
    private var safeNonce: BigInteger? = null
    private var minSafeNonce: BigInteger? = null
    private var safeTxGas: BigInteger? = null
    private var proposedSafeTxGas: BigInteger? = null

    override fun initialState(): SendAssetReviewState =
        SendAssetReviewState(viewAction = null)

    init {
        safeLaunch {
            activeSafe = safeRepository.getActiveSafe()!!
        }
    }

    fun loadTxEstimationData(
        chainId: BigInteger,
        from: Solidity.Address,
        to: Solidity.Address,
        amount: String,
        asset: CoinsViewData.CoinBalance
    ) {
        amountString = amount
        selectedAsset = asset
        fromAddress = from
        toAddress = to
        transferAmount =
            BigDecimal(amount).times(BigDecimal.TEN.pow(selectedAsset.decimals)).toBigInteger()
        safeLaunch {
            if (safeNonce != null) {
                updateState {
                    SendAssetReviewState(EstimationDataLoaded)
                }
            }
            val txEstimation = transactionRepository.estimateTransaction(
                chainId,
                from,
                to,
                transferAmount
            )
            minSafeNonce = txEstimation.currentNonce
            if (safeNonce == null) {
                safeNonce = txEstimation.recommendedNonce
            }
            if (SemVer.parse(activeSafe.version!!, ignoreExtensions = true) < SemVer(1, 3, 0)) {
                proposedSafeTxGas = txEstimation.safeTxGas
                if (safeTxGas == null) {
                    safeTxGas = proposedSafeTxGas
                }
            }
            updateState {
                SendAssetReviewState(EstimationDataLoaded)
            }
        }
    }

    fun onAdvancedParamsEdit() {
        safeNonce?.let {
            safeLaunch {
                updateState {
                    SendAssetReviewState(
                        viewAction = ViewAction.NavigateTo(
                            SendAssetReviewFragmentDirections.actionSendAssetReviewFragmentToEditAdvancedParamsFragment(
                                activeSafe.chain,
                                safeNonce.toString(),
                                minSafeNonce.toString(),
                                safeTxGas?.toString(),
                                proposedSafeTxGas?.toString()
                            )
                        )
                    )
                }
                updateState {
                    SendAssetReviewState(
                        viewAction = ViewAction.None
                    )
                }
            }
        }
    }

    fun updateAdvancedParams(nonce: String?, txGas: String?) {
        nonce?.let {
            safeNonce = BigInteger(it)
        }
        txGas?.let {
            safeTxGas = BigInteger(it)
        }
    }

    fun onConfirm() {

        safeLaunch {

            val contractVersion = activeSafe.version?.let {
                SemVer.parse(it)
            } ?: SemVer(0, 0, 0)

            txExecutionInfo = DetailedExecutionInfo.MultisigExecutionDetails(
                nonce = safeNonce!!,
                safeTxGas = safeTxGas ?: BigInteger.ZERO
            )

            val txDetails =
                if (selectedAsset.address.asEthereumAddress() == Solidity.Address(BigInteger.ZERO)) {
                    transferAddress = toAddress
                    transferValue = transferAmount
                    data = "0x"
                    TransactionDetails(
                        txInfo = TransactionInfo.Custom(
                            to = AddressInfo(toAddress),
                            value = transferAmount
                        ),
                        txData = TxData(
                            data,
                            null,
                            AddressInfo(toAddress),
                            transferAmount,
                            Operation.CALL
                        ),
                        detailedExecutionInfo = txExecutionInfo,
                        safeAppInfo = null
                    )
                } else {
                    transferAddress = selectedAsset.address.asEthereumAddress()!!
                    data = ERC20Contract.Transfer.encode(toAddress, Solidity.UInt256(transferAmount))
                    TransactionDetails(
                        txInfo = TransactionInfo.Transfer(
                            AddressInfo(fromAddress),
                            AddressInfo(toAddress),
                            TransferInfo.Erc20Transfer(
                                selectedAsset.address.asEthereumAddress()!!,
                                null,
                                null,
                                null,
                                null,
                                transferAmount
                            ),
                            TransactionDirection.OUTGOING
                        ),
                        txData = TxData(
                            data,
                            null,
                            AddressInfo(toAddress),
                            BigInteger.ZERO,
                            Operation.CALL
                        ),
                        detailedExecutionInfo = txExecutionInfo,
                        safeAppInfo = null
                    )
                }

            safeTxHash =
                calculateSafeTxHash(
                    implementationVersion = contractVersion,
                    chainId = activeSafe.chainId,
                    safeAddress = activeSafe.address,
                    transaction = txDetails,
                    executionInfo = txExecutionInfo
                ).toHexString()

            updateState {
                SendAssetReviewState(
                    ViewAction.NavigateTo(
                        SendAssetReviewFragmentDirections.actionSendAssetReviewFragmentToSigningOwnerSelectionFragment(
                            missingSigners = activeSafe.signingOwners.map {
                                it.asEthereumAddressString()
                            }.toTypedArray(),
                            signingMode = SigningMode.INITIATE_TRANSFER,
                            safeTxHash = safeTxHash
                        )
                    )
                )
            }
            updateState { SendAssetReviewState(ViewAction.None) }
        }
    }

    fun initiateTransfer(owner: Solidity.Address, signedSafeTxHash: String? = null) {
        safeLaunch {
            val selectedOwner = credentialsRepository.owner(owner) ?: throw MissingOwnerCredential
            kotlin.runCatching {
                transactionRepository.proposeTransaction(
                    chainId = activeSafe.chainId,
                    safeAddress = activeSafe.address,
                    toAddress = transferAddress,
                    value = transferValue,
                    data = data,
                    nonce = txExecutionInfo.nonce,
                    signature = signedSafeTxHash ?: credentialsRepository.signWithOwner(
                        selectedOwner,
                        safeTxHash.hexToByteArray()
                    ),
                    safeTxGas = txExecutionInfo.safeTxGas.toLong(),
                    safeTxHash = safeTxHash,
                    sender = selectedOwner.address
                )
            }.onSuccess {
                updateState {
                    SendAssetReviewState(
                        ViewAction.NavigateTo(
                            SendAssetReviewFragmentDirections.actionSendAssetReviewFragmentToSendAssetSuccessFragment(
                                activeSafe.chain,
                                safeTxHash,
                                amountString,
                                selectedAsset.symbol
                            )
                        )
                    )
                }
            }.onFailure {
                throw TxTransferFailed(it.cause ?: it)
            }
        }
    }

    fun isChainPrefixPrependEnabled() = settingsHandler.chainPrefixPrepend

    fun isChainPrefixCopyEnabled() = settingsHandler.chainPrefixCopy
}

data class SendAssetReviewState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

object EstimationDataLoaded : BaseStateViewModel.ViewAction

class TxTransferFailed(override val cause: Throwable) : Throwable(cause)
