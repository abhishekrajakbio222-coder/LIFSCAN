package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.BiometricAuthHelper
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BiometricAuthTest {

    @Test
    fun testBiometricAvailabilityCheck() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val status = BiometricAuthHelper.checkBiometricAvailability(context)
        assertNotNull(status)
        val label = BiometricAuthHelper.getHardwareStatusLabel(status)
        assertTrue(label.isNotBlank())
    }

    @Test
    fun testHardwareStatusLabels() {
        val labelAvailable = BiometricAuthHelper.getHardwareStatusLabel(BiometricAuthHelper.BiometricStatus.AVAILABLE)
        assertTrue(labelAvailable.contains("Fingerprint") || labelAvailable.contains("Biometric"))

        val labelNoHw = BiometricAuthHelper.getHardwareStatusLabel(BiometricAuthHelper.BiometricStatus.NO_HARDWARE)
        assertTrue(labelNoHw.contains("Android Keystore") || labelNoHw.contains("Encryption"))
    }
}
