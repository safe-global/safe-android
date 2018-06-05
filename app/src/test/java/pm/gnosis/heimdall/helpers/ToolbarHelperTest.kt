package pm.gnosis.heimdall.helpers

import android.support.v4.widget.NestedScrollView
import android.support.v4.widget.NestedScrollView.OnScrollChangeListener
import android.view.View
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.stubbing.Answer
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class ToolbarHelperTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var shadowView: View

    @Mock
    lateinit var scrollView: NestedScrollView

    private val random = Random()

    private lateinit var helper: ToolbarHelper

    @Before
    fun setUp() {
        helper = ToolbarHelper()
    }

    @Test
    fun testShadowUpdateNoInitialScroll() {
        var listener: OnScrollChangeListener? = null
        given(shadowView.height).willReturn(56)
        given(scrollView.scrollY).willReturn(0)
        given(scrollView.setOnScrollChangeListener(MockUtils.any<OnScrollChangeListener>())).will(
            Answer {
                listener = it.arguments.first() as OnScrollChangeListener
            }
        )

        helper.setupShadow(shadowView, scrollView)

        then(shadowView).should().alpha = 0f
        then(shadowView).should().height
        then(shadowView).shouldHaveNoMoreInteractions()

        val scrollX = random.nextInt(50) + 1 // No zero values
        val scrollY = random.nextInt(50) + 1 // No zero values
        listener!!.onScrollChange(scrollView, scrollX, scrollY, 0, 0)

        then(shadowView).should().alpha = (1f - ((56 - scrollY) / 56f))
        then(shadowView).should(times(2)).height
        then(shadowView).shouldHaveNoMoreInteractions()

        listener!!.onScrollChange(scrollView, 5, 20000, 0, 0)

        then(shadowView).should().alpha = 1f
        then(shadowView).should(times(3)).height
        then(shadowView).shouldHaveNoMoreInteractions()

        listener!!.onScrollChange(scrollView, 0, 0, 0, 0)

        then(shadowView).should(times(2)).alpha = 0f
        then(shadowView).should(times(4)).height
        then(shadowView).shouldHaveNoMoreInteractions()
    }

    @Test
    fun testShadowUpdateInitialScroll() {
        var listener: OnScrollChangeListener? = null
        val initialScrollY = random.nextInt(50) + 1 // No zero values
        given(shadowView.height).willReturn(56)
        given(scrollView.scrollY).willReturn(initialScrollY)
        given(scrollView.setOnScrollChangeListener(MockUtils.any<OnScrollChangeListener>())).will(
            Answer {
                listener = it.arguments.first() as OnScrollChangeListener
            }
        )

        helper.setupShadow(shadowView, scrollView)

        then(shadowView).should().alpha = (1f - ((56 - initialScrollY) / 56f))
        then(shadowView).should().height
        then(shadowView).shouldHaveNoMoreInteractions()

        val scrollX = random.nextInt(50) + 1 // No zero values
        val scrollY = random.nextInt(50) + 1 // No zero values
        listener!!.onScrollChange(scrollView, scrollX, scrollY, 0, 0)

        then(shadowView).should().alpha = (1f - ((56 - scrollY) / 56f))
        then(shadowView).should(times(2)).height
        then(shadowView).shouldHaveNoMoreInteractions()

        listener!!.onScrollChange(scrollView, 5, 20000, 0, 0)

        then(shadowView).should().alpha = 1f
        then(shadowView).should(times(3)).height
        then(shadowView).shouldHaveNoMoreInteractions()

        listener!!.onScrollChange(scrollView, 0, 0, 0, 0)

        then(shadowView).should(times(1)).alpha = 0f
        then(shadowView).should(times(4)).height
        then(shadowView).shouldHaveNoMoreInteractions()
    }

    @Test
    fun testShadowUpdateZeroHeight() {
        var listener: OnScrollChangeListener? = null
        val initialScrollY = random.nextInt(50) + 1 // No zero values
        given(shadowView.height).willReturn(0)
        given(scrollView.scrollY).willReturn(initialScrollY)
        given(scrollView.setOnScrollChangeListener(MockUtils.any<OnScrollChangeListener>())).will(
            Answer {
                listener = it.arguments.first() as OnScrollChangeListener
            }
        )

        helper.setupShadow(shadowView, scrollView)

        then(shadowView).should().alpha = 0f
        then(shadowView).should().height
        then(shadowView).shouldHaveNoMoreInteractions()

        val scrollX = random.nextInt(50) + 1 // No zero values
        val scrollY = random.nextInt(50) + 1 // No zero values
        listener!!.onScrollChange(scrollView, scrollX, scrollY, 0, 0)

        then(shadowView).should(times(2)).alpha = 0f
        then(shadowView).should(times(2)).height
        then(shadowView).shouldHaveNoMoreInteractions()

        listener!!.onScrollChange(scrollView, 5, 20000, 0, 0)

        then(shadowView).should(times(3)).alpha = 0f
        then(shadowView).should(times(3)).height
        then(shadowView).shouldHaveNoMoreInteractions()

        listener!!.onScrollChange(scrollView, 0, 0, 0, 0)

        then(shadowView).should(times(4)).alpha = 0f
        then(shadowView).should(times(4)).height
        then(shadowView).shouldHaveNoMoreInteractions()
    }

}
