package de.thomaskuenneth.smsrelay

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import de.thomaskuenneth.smsrelay.test.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmsRelayInstrumentedTest {

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    var permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.READ_CONTACTS)

    @Test
    fun `number with blanks can be matched to name`() {
        val name = appContext.getName(BuildConfig.TEST_PHONE_01)
        assertEquals(BuildConfig.TEST_NAME_01, name)
    }

    @Test
    fun `number without blanks can be matched to name`() {
        val name = appContext.getName(BuildConfig.TEST_PHONE_02)
        assertEquals(BuildConfig.TEST_NAME_01, name)
    }
}
