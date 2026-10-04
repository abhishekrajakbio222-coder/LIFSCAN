package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ai.GeminiHealthService
import com.example.data.cloud.FirestoreSyncService
import com.example.data.local.AppDatabase
import com.example.data.model.EmergencyContactEntity
import com.example.data.model.MedicationEntity
import com.example.data.repository.LifscanRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmergencySOSAndDocumentScanTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: LifscanRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LifscanRepository(
            database = database,
            firestoreSyncService = FirestoreSyncService()
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testMedicalDocumentScanAndGeminiCategorization() = runBlocking {
        // Test parsing of prescription document
        val analysis = GeminiHealthService.analyzeMedicalDocument(
            bitmap = null,
            documentCategory = "Prescription (Rx)",
            userNotes = "Patient prescribed Amlodipine for elevated blood pressure"
        )

        assertNotNull(analysis)
        assertTrue(analysis.documentTitle.isNotBlank())
        assertEquals("Prescription (Rx)", analysis.documentType)
        assertTrue("Expected extracted medications", analysis.extractedMedications.isNotEmpty())

        val firstMed = analysis.extractedMedications.first()
        assertTrue("Medication name should not be blank", firstMed.name.isNotBlank())
        assertTrue("Dosage should not be blank", firstMed.dosage.isNotBlank())

        // Save extracted medications to local Room database
        val savedCount = repository.saveExtractedMedicationsToSchedule(
            patientId = "patient_test_01",
            doctorName = analysis.doctorOrClinicName,
            medications = analysis.extractedMedications
        )

        assertTrue(savedCount > 0)
        val allMeds = database.medicationDao().getMedicationsForPatient("patient_test_01").first()
        assertEquals(savedCount, allMeds.size)
        assertEquals(firstMed.name, allMeds.first().name)
    }

    @Test
    fun testLocationAwareEmergencySOSTriggerAndFirebaseSignal() = runBlocking {
        val patientId = "patient_sos_01"

        // Insert emergency contacts
        val contact1 = EmergencyContactEntity(
            id = "contact_01",
            patientId = patientId,
            name = "Rojina Shrestha",
            relationship = "Spouse",
            phone = "+977-9841234567",
            email = "rojina@example.com",
            isPrimary = true,
            autoDialOnSos = true,
            autoSmsOnSos = true,
            isEncrypted = true,
            timestamp = System.currentTimeMillis()
        )
        val contact2 = EmergencyContactEntity(
            id = "contact_02",
            patientId = patientId,
            name = "Dr. Sandeep Adhikari",
            relationship = "Primary Cardiologist",
            phone = "+977-9841000102",
            email = "sandeep@birhospital.org",
            isPrimary = false,
            autoDialOnSos = false,
            autoSmsOnSos = true,
            isEncrypted = true,
            timestamp = System.currentTimeMillis()
        )
        database.emergencyContactDao().insertContact(contact1)
        database.emergencyContactDao().insertContact(contact2)

        val savedContacts = database.emergencyContactDao().getContactsForPatient(patientId).first()
        assertEquals(2, savedContacts.size)

        // Trigger Location-Aware Emergency SOS
        val alert = repository.triggerEmergencySOS(
            patientId = patientId,
            patientName = "Aayush Shrestha",
            patientPhone = "+977-9841234567",
            locationAddress = "Kanti Path, Kathmandu (Near Bir Hospital Trauma Hub)",
            emergencyType = "Cardiac / Chest Pain",
            latitude = 27.7172,
            longitude = 85.3240,
            accuracyMeters = 2.5f,
            contactsNotified = savedContacts.map { it.phone }
        )

        assertNotNull(alert)
        assertEquals("Cardiac / Chest Pain", alert.emergencyType)
        assertEquals(27.7172, alert.latitude, 0.001)
        assertEquals(85.3240, alert.longitude, 0.001)
        assertTrue(alert.status.isNotBlank())

        // Verify stored in Room database
        val storedAlerts = repository.getActiveSOSAlerts().first()
        assertTrue(storedAlerts.any { it.id == alert.id })

        // Test alert resolution in Firebase & Room
        repository.updateSOSStatus(alert, "RESOLVED")
        val updatedAlert = database.sosAlertDao().getAlertById(alert.id)
        assertEquals("RESOLVED", updatedAlert?.status)
    }
}
