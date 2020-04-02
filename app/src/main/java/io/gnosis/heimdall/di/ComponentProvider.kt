package io.gnosis.heimdall.di

import io.gnosis.heimdall.di.components.ApplicationComponent

interface ComponentProvider {
    fun get(): ApplicationComponent
}
