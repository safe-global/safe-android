package pm.gnosis.tests.utils

import android.content.Context
import android.content.res.Resources
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.BDDMockito.given
import org.mockito.Mockito


fun Context.mockGetColor() {
    val resourceMock = Mockito.mock(Resources::class.java)
    given(resourceMock.getColor(anyInt())).willAnswer { it.arguments.first() as Int }
    given(resources).willReturn(resourceMock)
}


fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()
