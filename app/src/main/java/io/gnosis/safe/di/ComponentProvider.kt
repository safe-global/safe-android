package io.gnosis.safe.di

import io.gnosis.safe.di.components.ApplicationComponent

interface ComponentProvider {
    fun get(): ApplicationComponent
}
