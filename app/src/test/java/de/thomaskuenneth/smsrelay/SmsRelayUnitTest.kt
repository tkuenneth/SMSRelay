package de.thomaskuenneth.smsrelay

import org.junit.Assert.assertEquals
import org.junit.Test

class SmsRelayUnitTest {

    @Test
    fun `verify that removeBlanks removes blanks`() {
        assertEquals(
            BuildConfig.TEST_PHONE_01.contains(" "), true
        )
        assertEquals(
            BuildConfig.TEST_PHONE_02.contains(" "), false
        )
        assertEquals(
            BuildConfig.TEST_PHONE_01.removeBlanks(), BuildConfig.TEST_PHONE_02
        )
    }
}
