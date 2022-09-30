package io.gnosis.safe.ui.safe.send_funds

import io.gnosis.safe.ui.assets.coins.CoinsViewData
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject

class AssetSelectionViewModel
@Inject constructor(
    appDispatchers: AppDispatchers
) : BaseStateViewModel<AssetSelectionState>(appDispatchers) {

    override fun initialState(): AssetSelectionState = AssetSelectionState(selectedAsset = null , viewAction = null)

    fun selectAssetForTransfer(assetData: CoinsViewData.CoinBalance) {
        //TODO: select asset
    }
}

data class AssetSelectionState(
    val selectedAsset: CoinsViewData.CoinBalance?,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
