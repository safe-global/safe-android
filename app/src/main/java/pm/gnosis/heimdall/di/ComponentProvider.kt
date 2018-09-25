package pm.gnosis.heimdall.di

import pm.gnosis.heimdall.di.components.ApplicationComponent

interface ComponentProvider {
    fun get(): ApplicationComponent
}
