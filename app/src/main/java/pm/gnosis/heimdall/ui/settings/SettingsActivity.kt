package pm.gnosis.heimdall.ui.settings

import pm.gnosis.heimdall.ui.base.BaseActivity
import javax.inject.Inject


class SettingsActivity : BaseActivity() {
    @Inject
    lateinit var viewModel: SettingsContract
}