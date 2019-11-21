package pm.gnosis.heimdall.helpers

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.tests.utils.MockUtils

@RunWith(MockitoJUnitRunner::class)
class NfcHelperTest {

    @Mock
    lateinit var contextMock: Context

    @Mock
    lateinit var adapterProviderMock: NfcAdapterProvider

    @Mock
    lateinit var adapterMock: NfcAdapter

    private lateinit var helper: NfcHelper

    @Before
    fun setUp() {
        helper = NfcHelper(adapterProviderMock)
    }

    @Test
    fun initWithContext() {
        given(adapterProviderMock.get(MockUtils.any())).willReturn(adapterMock)
        assertEquals(true, helper.init(contextMock))
        then(adapterProviderMock).should().get(contextMock)
        then(adapterProviderMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun initWithContextNoAdapter() {
        given(adapterProviderMock.get(MockUtils.any())).willReturn(null)
        assertEquals(false, helper.init(contextMock))
        then(adapterProviderMock).should().get(contextMock)
        then(adapterProviderMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun initWithContextThrow() {
        given(adapterProviderMock.get(MockUtils.any())).willThrow(UnsupportedOperationException())
        assertEquals(false, helper.init(contextMock))
        then(adapterProviderMock).should().get(contextMock)
        then(adapterProviderMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun initWithoutContext() {
        assertEquals(false, helper.init(null))
        then(adapterProviderMock).shouldHaveZeroInteractions()
    }

    @Test
    fun enableActivityAsHost() {
        val callbackMock = mock(NfcAdapter.ReaderCallback::class.java)
        val hostActivityMock = mock(Activity::class.java, withSettings().extraInterfaces(NfcHost::class.java))
        helper.enable(hostActivityMock, callbackMock)
        then(hostActivityMock as NfcHost).should().registerNfcCallback(callbackMock)
    }

    @Test
    fun enableContextAsHost() {
        val callbackMock = mock(NfcAdapter.ReaderCallback::class.java)
        val hostContextMock = mock(Context::class.java, withSettings().extraInterfaces(NfcHost::class.java))
        helper.init(hostContextMock)
        helper.enable(null, callbackMock)
        then(hostContextMock as NfcHost).should().registerNfcCallback(callbackMock)
    }

    @Test
    fun enableNoHost() {
        given(adapterProviderMock.get(MockUtils.any())).willReturn(adapterMock)
        val callbackMock = mock(NfcAdapter.ReaderCallback::class.java)
        val activityMock = mock(Activity::class.java)
        helper.init(contextMock)
        helper.enable(activityMock, callbackMock)
        then(adapterMock).should()
            .enableReaderMode(activityMock, callbackMock, NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null)
    }

    @Test
    fun enableNoHostNoActivity() {
        given(adapterProviderMock.get(MockUtils.any())).willReturn(adapterMock)
        val callbackMock = mock(NfcAdapter.ReaderCallback::class.java)
        helper.init(contextMock)
        helper.enable(null, callbackMock)
        then(adapterMock).shouldHaveZeroInteractions()
    }

    @Test
    fun enableNoHostNoAdapter() {
        given(adapterProviderMock.get(MockUtils.any())).willReturn(null)
        val callbackMock = mock(NfcAdapter.ReaderCallback::class.java)
        val activityMock = mock(Activity::class.java)
        helper.init(contextMock)
        helper.enable(activityMock, callbackMock)
    }

    @Test
    fun disableActivityAsHost() {
        val callbackMock = mock(NfcAdapter.ReaderCallback::class.java)
        val hostActivityMock = mock(Activity::class.java, withSettings().extraInterfaces(NfcHost::class.java))
        helper.disable(hostActivityMock, callbackMock)
        then(hostActivityMock as NfcHost).should().unregisterNfcCallback(callbackMock)
    }

    @Test
    fun disableContextAsHost() {
        val callbackMock = mock(NfcAdapter.ReaderCallback::class.java)
        val hostContextMock = mock(Context::class.java, withSettings().extraInterfaces(NfcHost::class.java))
        helper.init(hostContextMock)
        helper.disable(null, callbackMock)
        then(hostContextMock as NfcHost).should().unregisterNfcCallback(callbackMock)
    }

    @Test
    fun disableNoHost() {
        given(adapterProviderMock.get(MockUtils.any())).willReturn(adapterMock)
        val callbackMock = mock(NfcAdapter.ReaderCallback::class.java)
        val activityMock = mock(Activity::class.java)
        helper.init(contextMock)
        helper.disable(activityMock, callbackMock)
        then(adapterMock).should()
            .disableForegroundDispatch(activityMock)
    }

    @Test
    fun disableNoHostNoActivity() {
        given(adapterProviderMock.get(MockUtils.any())).willReturn(adapterMock)
        val callbackMock = mock(NfcAdapter.ReaderCallback::class.java)
        helper.init(contextMock)
        helper.disable(null, callbackMock)
        then(adapterMock).shouldHaveZeroInteractions()
    }

    @Test
    fun disableNoHostNoAdapter() {
        given(adapterProviderMock.get(MockUtils.any())).willReturn(null)
        val callbackMock = mock(NfcAdapter.ReaderCallback::class.java)
        val activityMock = mock(Activity::class.java)
        helper.init(contextMock)
        helper.disable(activityMock, callbackMock)
    }
}
