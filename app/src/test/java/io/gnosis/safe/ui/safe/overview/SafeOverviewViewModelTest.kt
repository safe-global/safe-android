package io.gnosis.safe.ui.safe.overview

import android.app.Application
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.tests.utils.TestPreferences

class SafeOverviewViewModelTest {

    private lateinit var preferences: TestPreferences
    private lateinit var preferencesManager: PreferencesManager
    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setup() {
        preferences = spyk()
        val application = mockk<Application>().apply {
            every { getSharedPreferences(any(), any()) } returns preferences
        }
        preferencesManager = PreferencesManager(application)
    }

    @Test
    fun `If terms already agreed, advance() method should be called and bottom sheet should not be shown`() {
        preferences.edit().putBoolean(SafeOverviewViewModel.TERMS_AGREED, true).apply()

        val viewModel = SafeOverviewViewModel(preferencesManager)
        viewModel.termsBottomSheetDialog = mockk(relaxed = true)

        val callback: () -> Unit = mockk(relaxed = true)

        viewModel.checkTerms(callback)

        verify { callback.invoke() }
        verify(exactly = 0) { viewModel.termsBottomSheetDialog.show() }
    }

    @Test
    fun `If terms not agreed, advance() method should not be called when agree button not hit`() {
        preferences.edit().putBoolean(SafeOverviewViewModel.TERMS_AGREED, false).apply()

        val viewModel = SafeOverviewViewModel(preferencesManager)
        viewModel.termsBottomSheetDialog = mockk(relaxed = true)
        val callback: () -> Unit = mockk(relaxed = true)

        viewModel.checkTerms(callback)

        verify { viewModel.termsBottomSheetDialog.show() }
        verify(exactly = 0) { callback.invoke() }
    }
    
    // TODO: Test if advance is called when user clicks agree
    // TODO: Test if preference is stored when user clicks agree
    // TODO: Test if termsBottomSheetDialog  is dismissed clicks agree or no_thanks

}
