package pm.gnosis.heimdall.ui.safe.deploy

import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import javax.inject.Inject


class DeploySafeViewModel @Inject constructor(
        private val repository: GnosisSafeRepository
): DeploySafeContract()