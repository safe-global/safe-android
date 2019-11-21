package pm.gnosis.heimdall.helpers

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.*
import org.mockito.BDDMockito.*
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.capture

@RunWith(MockitoJUnitRunner::class)
class NfcActivityDelegateTest {

    @Mock
    lateinit var contextMock: Context

    @Mock
    lateinit var activityMock: Activity

    @Mock
    lateinit var adapterProviderMock: NfcAdapterProvider

    @Mock
    lateinit var adapterMock: NfcAdapter

    @Captor
    lateinit var callbackCaptor: ArgumentCaptor<NfcAdapter.ReaderCallback>

    private lateinit var delegate: NfcActivityDelegate

    @Before
    fun setUp() {
        delegate = NfcActivityDelegate(adapterProviderMock)
    }

    @Test
    fun init() {
        delegate.init(contextMock)
        then(adapterProviderMock).should().get(contextMock)
    }

    @Test
    fun initThrow() {
        given(adapterProviderMock.get(MockUtils.any())).willThrow(UnsupportedOperationException())
        delegate.init(contextMock)
        then(adapterProviderMock).should().get(contextMock)
    }

    @Test
    fun enable() {
        given(adapterProviderMock.get(MockUtils.any())).willReturn(adapterMock)
        delegate.init(contextMock)
        then(adapterProviderMock).should().get(contextMock)
        delegate.enable(activityMock)
        then(adapterMock).should().enableReaderMode(
            MockUtils.eq(activityMock),
            MockUtils.any<NfcAdapter.ReaderCallback>(),
            eq(NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK),
            isNull()
        )
    }

    @Test
    fun enableNoAdapter() {
        delegate.init(contextMock)
        then(adapterProviderMock).should().get(contextMock)
        delegate.enable(activityMock)
        then(adapterMock).shouldHaveZeroInteractions()
    }

    @Test
    fun enableThrow() {
        given(adapterProviderMock.get(MockUtils.any())).willReturn(adapterMock)
        given(adapterMock.enableReaderMode(MockUtils.any(), MockUtils.any(), anyInt(), MockUtils.any())).willThrow(UnsupportedOperationException())
        delegate.init(contextMock)
        then(adapterProviderMock).should().get(contextMock)
        delegate.enable(activityMock)
        then(adapterMock).should().enableReaderMode(
            MockUtils.eq(activityMock),
            MockUtils.any<NfcAdapter.ReaderCallback>(),
            eq(NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK),
            isNull()
        )
    }

    @Test
    fun disable() {
        given(adapterProviderMock.get(MockUtils.any())).willReturn(adapterMock)
        delegate.init(contextMock)
        then(adapterProviderMock).should().get(contextMock)
        delegate.disable(activityMock)
        then(adapterMock).should().disableForegroundDispatch(activityMock)
    }

    @Test
    fun disableNoAdapter() {
        delegate.init(contextMock)
        then(adapterProviderMock).should().get(contextMock)
        delegate.disable(activityMock)
        then(adapterMock).shouldHaveZeroInteractions()
    }

    @Test
    fun disableThrow() {
        given(adapterProviderMock.get(MockUtils.any())).willReturn(adapterMock)
        given(adapterMock.disableForegroundDispatch(MockUtils.any())).willThrow(UnsupportedOperationException())
        delegate.init(contextMock)
        then(adapterProviderMock).should().get(contextMock)
        delegate.disable(activityMock)
        then(adapterMock).should().disableForegroundDispatch(activityMock)
    }

    @Test
    fun registerNfcCallback() {
        // Register callback
        val callbackMock = mock(NfcAdapter.ReaderCallback::class.java)
        delegate.registerNfcCallback(callbackMock)
        then(adapterMock).shouldHaveZeroInteractions()

        // Enable reader mode
        given(adapterProviderMock.get(MockUtils.any())).willReturn(adapterMock)
        delegate.init(contextMock)
        then(adapterProviderMock).should().get(contextMock)
        delegate.enable(activityMock)
        then(adapterMock).should().enableReaderMode(
            MockUtils.eq(activityMock),
            capture(callbackCaptor),
            eq(NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK),
            isNull()
        )
        then(callbackMock).shouldHaveZeroInteractions()

        // Trigger callback
        val tagMock = mock(Tag::class.java)
        callbackCaptor.value.onTagDiscovered(tagMock)
        then(callbackMock).should().onTagDiscovered(tagMock)
        then(callbackMock).shouldHaveNoMoreInteractions()

        // Override callback
        val callbackMock2 = mock(NfcAdapter.ReaderCallback::class.java)
        delegate.registerNfcCallback(callbackMock2)

        // Trigger callback
        callbackCaptor.value.onTagDiscovered(tagMock)
        then(callbackMock).shouldHaveNoMoreInteractions()
        then(callbackMock2).should().onTagDiscovered(tagMock)
        then(callbackMock2).shouldHaveNoMoreInteractions()

        // Cannot unregister other callback
        delegate.unregisterNfcCallback(callbackMock)
        callbackCaptor.value.onTagDiscovered(tagMock)
        then(callbackMock).shouldHaveNoMoreInteractions()
        then(callbackMock2).should(times(2)).onTagDiscovered(tagMock)
        then(callbackMock2).shouldHaveNoMoreInteractions()

        // Unregister callback
        delegate.unregisterNfcCallback(callbackMock2)
        callbackCaptor.value.onTagDiscovered(tagMock)
        then(callbackMock).shouldHaveNoMoreInteractions()
        then(callbackMock2).shouldHaveNoMoreInteractions()
    }
}
