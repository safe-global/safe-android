package pm.gnosis.tests.utils

import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler

fun runOnTestScheduler(test: (testScheduler: TestScheduler) -> Unit) {
    val testScheduler = TestScheduler()
    RxJavaPlugins.setComputationSchedulerHandler({ _ -> testScheduler })
    test(testScheduler)
    RxJavaPlugins.setComputationSchedulerHandler({ _ -> Schedulers.trampoline() })
}
