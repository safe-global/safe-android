package io.gnosis.safe.ui

import android.os.Bundle
import io.gnosis.safe.R
import io.gnosis.safe.ui.base.BaseActivity

class StartActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
    }
}
