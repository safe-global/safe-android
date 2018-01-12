package pm.gnosis.heimdall.helpers

import io.reactivex.*
import java.util.concurrent.CopyOnWriteArraySet

interface ObservableStore<T> {
    fun load(): Single<T>
    fun observe(): Observable<T>
}

abstract class ValueStore<T> : ObservableStore<T>, ObservableOnSubscribe<T>, SingleOnSubscribe<T> {

    private val dataLock = Any()

    private var emitters = CopyOnWriteArraySet<ObservableEmitter<T>>()

    override fun subscribe(e: ObservableEmitter<T>) {
        emitters.add(e)
        e.setCancellable {
            emitters.remove(e)
        }
        e.onNext(dataSet())
    }

    override fun subscribe(e: SingleEmitter<T>) {
        e.onSuccess(dataSet())
    }

    abstract protected fun dataSet(): T

    override fun load(): Single<T> = Single.create(this)

    override fun observe(): Observable<T> = Observable.create(this)

    protected fun publish() {
        emitters.forEach { it.onNext(dataSet()) }
    }

    protected fun transaction(action: () -> Unit) {
        synchronized(dataLock) {
            action()
        }
    }
}

abstract class SetStore<T>(private val set: MutableSet<T> = HashSet()) : ValueStore<Set<T>>() {

    override fun dataSet() = HashSet(set)

    fun contains(entry: T) = set.contains(entry)

    fun add(entry: T) {
        transaction {
            set.add(entry)
        }
        publish()
    }

    fun remove(entry: T) {
        transaction {
            set.remove(entry)
        }
        publish()
    }

    fun clear() {
        set.clear()
    }
}