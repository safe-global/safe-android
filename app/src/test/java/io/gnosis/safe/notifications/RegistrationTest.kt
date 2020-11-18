package io.gnosis.safe.notifications

import io.gnosis.safe.notifications.models.Registration
import junit.framework.TestCase.assertEquals
import org.junit.Test

class RegistrationTest {

    @Test
    fun testHash() {

        val registration = Registration(
            uuid = UUID,
            safes = listOf(SAFE_1, SAFE_2),
            cloudMessagingToken = CLOUD_MESSAGING_TOKEN,
            bundle = BUNDLE,
            version = VERSION,
            deviceType = DEVICE_TYPE,
            buildNumber = BUILD_NUMBER,
            timestamp = TIMESTAMP
        )

        assertEquals("0x32938555b77412e2583786eb3ae139b93ca23f1c5f20e75df830e7406b9c6f14", registration.hash())
    }


    companion object {
        const val UUID = "33971c4e-fb98-4e18-a08d-13c881ae292a"
        const val SAFE_1 = "0x4dEBDD6CEe25b2F931D2FE265D70e1a533B02453"
        const val SAFE_2 = "0x72ac1760daF52986421b1552BdCa04707E78950e"
        const val CLOUD_MESSAGING_TOKEN =
            "dSh5Se1XgEiTiY-4cv1ixY:APA91bG3vYjy9VgB3X3u5EsBphJABchb8Xgg2cOSSekPsxDsfE5xyBeu6gKY0wNhbJHgQUQQGocrHx0Shbx6JMFx2VOyhJx079AduN01NWD1-WjQerY5s3l-cLnHoNNn8fJfARqSUb3G"
        const val BUNDLE = "io.gnosis.multisig.prod.mainnet"
        const val VERSION = "2.7.0"
        const val DEVICE_TYPE = "IOS"
        const val BUILD_NUMBER = "199"
        const val TIMESTAMP = "1605186645155"
    }
}
