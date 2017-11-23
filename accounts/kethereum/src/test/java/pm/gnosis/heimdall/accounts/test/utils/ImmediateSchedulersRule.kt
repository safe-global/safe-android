package pm.gnosis.heimdall.accounts.test.utils

import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class ImmediateSchedulersRule : TestRule {
    private val immediate = Schedulers.trampoline()

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                RxJavaPlugins.setIoSchedulerHandler { _ -> immediate }
                RxJavaPlugins.setComputationSchedulerHandler { _ -> immediate }
                RxJavaPlugins.setNewThreadSchedulerHandler { _ -> immediate }

                try {
                    base.evaluate()
                } finally {
                    RxJavaPlugins.reset()
                }
            }
        }
    }
}



