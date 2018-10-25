package pm.gnosis.tests.utils

import io.reactivex.Single
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.mock
import pm.gnosis.heimdall.helpers.PasswordValidationCondition
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupContract
import pm.gnosis.svalinn.common.utils.Result

fun newPasswordContractMock(
    validatePasswordReturn: ((String) -> Single<Collection<PasswordValidationCondition>>)? = null,
    passwordToHash: ((String) -> Single<Result<ByteArray>>)? = null
): PasswordSetupContract = mock(PasswordSetupContract::class.java).apply {
    validatePasswordReturn?.let {
        given(validatePassword(UIMockUtils.any())).willAnswer { invocation -> it(invocation.arguments.first() as String) }
    }
    passwordToHash?.let {
        given(passwordToHash(UIMockUtils.any())).willAnswer { invocation -> it(invocation.arguments.first() as String) }
    }
}
