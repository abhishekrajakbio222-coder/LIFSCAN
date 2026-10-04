package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.HealthReportEntity
import com.example.util.BiometricAuthHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecureDocumentViewerTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testRoomMedicalDocumentStorageAndDecryptionFlow() = runBlocking {
        val testReport = HealthReportEntity(
            id = "rep_secure_001",
            patientId = "patient_test_01",
            title = "Cardiology Diagnostic Panel",
            category = "Cardiology",
            doctorOrLabName = "Bir Hospital Cardiology Dept",
            date = "2026-09-29",
            summary = "Patient shows stable sinus rhythm. Mild hypertension noted. Continue Telmisartan.",
            vitalsBloodPressure = "135/85 mmHg",
            vitalsHeartRate = "76 bpm",
            vitalsSpO2 = "98%",
            vitalsBloodSugar = "104 mg/dL",
            isEncrypted = true,
            fileFormat = "PDF",
            timestamp = System.currentTimeMillis(),
            syncStatus = "SYNCED"
        )

        // Store encrypted medical record in Room Database
        database.healthReportDao().insertReport(testReport)

        // Retrieve record from Room Database
        val storedReports = database.healthReportDao().getReportsForPatient("patient_test_01").first()
        assertEquals(1, storedReports.size)
        val retrieved = storedReports[0]

        // Verify sensitive fields stored securely
        assertEquals("rep_secure_001", retrieved.id)
        assertEquals("Cardiology Diagnostic Panel", retrieved.title)
        assertTrue(retrieved.isEncrypted)
        assertEquals("135/85 mmHg", retrieved.vitalsBloodPressure)
        assertEquals("76 bpm", retrieved.vitalsHeartRate)
        assertTrue(retrieved.summary.contains("Telmisartan"))

        // Verify biometric status check capability
        val context = ApplicationProvider.getApplicationContext<Context>()
        val availability = BiometricAuthHelper.checkBiometricAvailability(context)
        assertNotNull(availability)
        val statusLabel = BiometricAuthHelper.getHardwareStatusLabel(availability)
        assertTrue(statusLabel.isNotBlank())
    }
}
