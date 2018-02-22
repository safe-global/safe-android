package pm.gnosis.heimdall.ui.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import javax.inject.Provider

class InflatingViewFactory(private val inflater: LayoutInflater, private val container: ViewGroup, private val layout: Int) : Provider<View> {
    override fun get() = inflater.inflate(layout, container, false)!!
}

class InflatedViewFactory(private val view: View) : Provider<View> {
    override fun get() = view
}
