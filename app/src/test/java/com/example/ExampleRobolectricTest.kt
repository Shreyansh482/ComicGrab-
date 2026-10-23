package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.SecurityManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppNameResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ComicGrab", appName)
    }

    @Test
    fun testSecurityManagerPinHashing() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val security = SecurityManager(context)

        // Setup PIN
        val setupSuccess = security.setupPin("1234")
        assertTrue(setupSuccess)
        assertTrue(security.hasPinConfigured())

        // Lock
        security.lock()
        assertFalse(security.isUnlocked.value)

        // Invalid PIN
        val wrongAttempt = security.verifyPin("9999")
        assertFalse(wrongAttempt)
        assertFalse(security.isUnlocked.value)

        // Correct PIN
        val correctAttempt = security.verifyPin("1234")
        assertTrue(correctAttempt)
        assertTrue(security.isUnlocked.value)
    }
}
