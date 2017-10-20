package pm.gnosis.heimdall.ui.multisig.details.info

import android.content.Context
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.common.util.DataResult
import pm.gnosis.heimdall.common.util.ErrorResult
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.heimdall.data.model.Wei
import pm.gnosis.heimdall.data.repositories.MultisigRepository
import pm.gnosis.heimdall.data.repositories.model.MultisigWalletInfo
import pm.gnosis.heimdall.test.utils.ImmediateSchedulersRule
import pm.gnosis.heimdall.test.utils.MockUtils
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class MultisigInfoViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var repository: MultisigRepository

    lateinit var viewModel: MultisigInfoViewModel

    @Before
    fun setup() {
        viewModel = MultisigInfoViewModel(context, repository)
    }

    private fun callSetupAndCheck(address: BigInteger, info: MultisigWalletInfo,
                                  repositoryInvocations: Int = 1, totalInvocations: Int = repositoryInvocations,
                                  ignoreCached: Boolean = false) {
        // Setup with address
        viewModel.setup(address)

        // Verify that the repository is called and expected version is returned
        val observer = TestObserver.create<Result<MultisigWalletInfo>>()
        viewModel.loadMultisigInfo(ignoreCached).subscribe(observer)
        observer.assertNoErrors().assertValueCount(1).assertValue(DataResult(info))
        Mockito.verify(repository, Mockito.times(repositoryInvocations)).loadMultisigWalletInfo(address)
        Mockito.verify(repository, Mockito.times(totalInvocations)).loadMultisigWalletInfo(MockUtils.any())
    }

    @Test
    fun setupViewModelClearCache() {
        val address1 = BigInteger.ZERO
        val info1 = MultisigWalletInfo("Test1", Wei(BigInteger.ONE), 0, emptyList())
        given(repository.loadMultisigWalletInfo(address1)).willReturn(Observable.just(info1))

        val address2 = BigInteger.ONE
        val info2 = MultisigWalletInfo("Test2", Wei(BigInteger.ONE), 0, emptyList())
        given(repository.loadMultisigWalletInfo(address2)).willReturn(Observable.just(info2))

        callSetupAndCheck(address1, info1)

        callSetupAndCheck(address2, info2, totalInvocations = 2)
    }

    @Test
    fun setupViewModelKeepCache() {
        val address = BigInteger.ZERO
        val info = MultisigWalletInfo("Test", Wei(BigInteger.ONE), 0, emptyList())
        given(repository.loadMultisigWalletInfo(MockUtils.any())).willReturn(Observable.just(info))

        callSetupAndCheck(address, info)

        callSetupAndCheck(address, info)
    }

    @Test
    fun loadMultisigInfoIgnoreCache() {
        val address = BigInteger.ZERO
        val info = MultisigWalletInfo("Test", Wei(BigInteger.ONE), 0, emptyList())
        given(repository.loadMultisigWalletInfo(address)).willReturn(Observable.just(info))

        callSetupAndCheck(address, info)

        callSetupAndCheck(address, info, 2, ignoreCached = true)
    }

    @Test
    fun loadMultisigInfoError() {
        viewModel.setup(BigInteger.ZERO)

        val exception = IllegalStateException("test")
        given(repository.loadMultisigWalletInfo(MockUtils.any())).willReturn(Observable.error(exception))

        val observer = TestObserver.create<Result<MultisigWalletInfo>>()
        viewModel.loadMultisigInfo(true).subscribe(observer)
        observer.assertNoErrors().assertValueCount(1).assertValue(ErrorResult(exception))
    }

}