package pm.gnosis.ticker.di

import dagger.Binds
import dagger.Module
import pm.gnosis.ticker.data.repositories.TickerRepository
import pm.gnosis.ticker.data.repositories.impls.DefaultTickerRepository

@Module
abstract class TickerBindingModule {
    @Binds
    abstract fun bindTickerRepository(tickerRepository: DefaultTickerRepository): TickerRepository
}
