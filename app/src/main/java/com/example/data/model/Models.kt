package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole(val displayName: String) {
    PATIENT("Patient"),
    DOCTOR("Doctor"),
    AMBULANCE_DRIVER("Ambulance Driver"),
    ADMIN("Admin / Facilities")
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val phone: String,
    val countryCode: String,
    val countryName: String,
    val name: String,
    val role: UserRole,
    val email: String = "",
    val gender: String = "Not Specified",
    val age: Int = 28,
    val bloodGroup: String = "O+",
    val medicalHistory: String = "No known allergies. Up-to-date vaccinations.",
    val emergencyContactName: String = "Family Emergency Contact",
    val emergencyContactPhone: String = "+977-9841234567",
    val isLicenseVerified: Boolean = false,
    val licenseDocumentUrl: String = "",
    val specialization: String = "General Medicine",
    val clinicAffiliation: String = "Bir Hospital / Teaching Hospital",
    val consultationFee: Double = 500.0,
    val vehicleNumber: String = "BA 1 PA 2045",
    val vehicleType: String = "Advanced Life Support (ALS) Ambulance",
    val isOnline: Boolean = true,
    val language: String = "English",
    val biometricEnabled: Boolean = true,
    val preferredCurrency: String = "NPR",
    val walletBalance: Double = 3500.0,
    val rating: Float = 4.9f,
    val totalConsultationsOrTrips: Int = 42,
    val doctorExperienceYears: Int = 10,
    val doctorAvailableDays: String = "Mon, Tue, Wed, Thu, Fri, Sat",
    val doctorAvailableTimeSlots: String = "09:00 AM, 10:30 AM, 02:00 PM, 04:30 PM, 06:00 PM",
    val doctorConsultationModes: String = "VIDEO_CALL, IN_CLINIC",
    val doctorLanguages: String = "English, Nepali, Hindi",
    val doctorBio: String = "Experienced clinical specialist providing comprehensive patient care and teleconsultations."
)

@Entity(tableName = "skin_scans")
data class SkinScanEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val patientName: String,
    val conditionName: String,
    val confidenceScore: Float,
    val riskLevel: String, // "Low", "Moderate", "High", "Critical"
    val affectedArea: String,
    val symptomsDescription: String,
    val aiAnalysisSummary: String,
    val recommendedTreatment: String,
    val medicationAdvice: String,
    val recommendedSpecialist: String,
    val imageBase64OrUri: String = "",
    val sampleSkinType: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = true,
    val isCloudSynced: Boolean = true,
    val syncStatus: String = "SYNCED" // "SYNCED", "PENDING_SYNC", "LOCAL_OFFLINE"
)

@Entity(tableName = "health_reports")
data class HealthReportEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val title: String,
    val category: String, // "Blood Test", "Dermatology", "Cardiology", "Radiology", "Prescription"
    val doctorOrLabName: String,
    val date: String,
    val summary: String,
    val vitalsBloodPressure: String = "120/80 mmHg",
    val vitalsHeartRate: String = "72 bpm",
    val vitalsSpO2: String = "98%",
    val vitalsBloodSugar: String = "95 mg/dL",
    val isEncrypted: Boolean = true,
    val fileFormat: String = "PDF",
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED" // "SYNCED", "PENDING_SYNC", "LOCAL_OFFLINE"
)

@Entity(tableName = "sos_alerts")
data class SOSAlertEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val patientName: String,
    val patientPhone: String,
    val locationAddress: String,
    val latitude: Double,
    val longitude: Double,
    val emergencyType: String, // "Severe Trauma", "Cardiac Emergency", "Skin Allergy Shock", "Respiratory Distress", "General SOS"
    val status: String, // "PENDING", "DISPATCHED", "ACCEPTED", "ARRIVED", "RESOLVED"
    val assignedDriverId: String = "",
    val assignedDriverName: String = "",
    val assignedDriverPhone: String = "",
    val ambulanceVehicleNumber: String = "",
    val estimatedArrivalMinutes: Int = 7,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "appointments",
    indices = [
        androidx.room.Index(value = ["patientId"]),
        androidx.room.Index(value = ["doctorId"]),
        androidx.room.Index(value = ["appointmentDate"]),
        androidx.room.Index(value = ["status"])
    ]
)
data class AppointmentEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val patientName: String,
    val patientPhone: String = "+977-9841234567",
    val doctorId: String,
    val doctorName: String,
    val doctorSpecialty: String,
    val clinicName: String,
    val appointmentDate: String,
    val timeSlot: String,
    val fee: Double,
    val currency: String = "NPR",
    val status: String = "UPCOMING", // "UPCOMING", "CONFIRMED", "IN_PROGRESS", "COMPLETED", "CANCELLED", "RESCHEDULED"
    val isVideoConsultation: Boolean = true,
    val consultationType: String = "VIDEO_CALL", // "VIDEO_CALL", "IN_CLINIC", "HOME_VISIT"
    val notes: String = "Consultation regarding dermatological symptoms and health scan.",
    val symptoms: String = "",
    val prescriptionNotes: String = "",
    val meetingLink: String = "https://lifscan.telehealth.live/room/med-consult",
    val isPaid: Boolean = true,
    val paymentMethod: String = "eSewa / Khalti",
    val rating: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED" // "SYNCED", "PENDING_SYNC", "LOCAL_OFFLINE"
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: UserRole,
    val receiverId: String,
    val message: String,
    val attachmentType: String = "NONE", // "NONE", "IMAGE", "PRESCRIPTION", "REPORT", "SKIN_SCAN"
    val attachmentUrl: String = "",
    val attachmentName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isEncrypted: Boolean = true
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userRole: UserRole,
    val title: String,
    val amount: Double,
    val currency: String,
    val paymentMethod: String, // "eSewa/Khalti (Nepal)", "UPI (India)", "Credit/Debit Card", "Stripe", "Razorpay", "Bank Transfer"
    val transactionType: String, // "PAYMENT", "EARNING", "WITHDRAWAL", "REFUND"
    val status: String, // "SUCCESS", "PENDING", "FAILED"
    val invoiceNumber: String,
    val serviceDetails: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "medical_facilities")
data class MedicalFacilityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val facilityType: String, // "GOVT_HOSPITAL", "PRIVATE_HOSPITAL", "PHARMACY", "CLINIC", "LIVE_AMBULANCE", "DIAGNOSTIC_LAB", "BLOOD_BANK"
    val address: String,
    val distanceKm: Double,
    val phone: String,
    val rating: Float,
    val isOpen24x7: Boolean,
    val services: String,
    val emergencyAvailable: Boolean = true,
    val latitude: Double = 27.7172,
    val longitude: Double = 85.3240,
    val totalBeds: Int = 100,
    val availableIcuBeds: Int = 8,
    val availableVentilators: Int = 4,
    val oxygenSupplyStatus: String = "OPTIMAL", // "OPTIMAL", "ADEQUATE", "LOW", "CRITICAL"
    val activeDoctorsOnDuty: Int = 12,
    val ambulanceFleetCount: Int = 4,
    val isVerified: Boolean = true,
    val adminContactPerson: String = "Medical Superintendent",
    val emergencyHelpline: String = "102",
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val name: String,
    val dosage: String, // e.g. "500 mg", "1 tablet", "2 puffs"
    val frequency: String, // "Daily", "Twice a day", "Every 8 hours", "As needed"
    val timeSlot: String, // "Morning (08:00 AM)", "Afternoon (01:00 PM)", "Evening (07:00 PM)", "Night (10:00 PM)"
    val scheduledHour: Int = 8,
    val scheduledMinute: Int = 0,
    val doctorName: String = "Dr. Sandeep Adhikari",
    val instructions: String = "Take with water after meal",
    val remainingDoses: Int = 14,
    val totalDoses: Int = 20,
    val isReminderEnabled: Boolean = true,
    val isTakenToday: Boolean = false,
    val streakDays: Int = 4,
    val isEncrypted: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED" // "SYNCED", "PENDING_SYNC", "LOCAL_OFFLINE"
)

@Entity(tableName = "emergency_contacts")
data class EmergencyContactEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val name: String,
    val relationship: String, // "Spouse", "Parent", "Sibling", "Doctor", "Friend", "Guardian"
    val phone: String,
    val email: String = "",
    val isPrimary: Boolean = true,
    val autoDialOnSos: Boolean = true,
    val autoSmsOnSos: Boolean = true,
    val isEncrypted: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED" // "SYNCED", "PENDING_SYNC", "LOCAL_OFFLINE"
)

@Entity(tableName = "vault_audit_logs")
data class VaultAuditLogEntity(
    @PrimaryKey val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val accessType: String, // "BIOMETRIC_VAULT_UNLOCK", "VIEW_RECORD", "EXPORT_PDF_REPORT", "CLOUD_BACKUP_SYNC", "CLOUD_BACKUP_RESTORE", "ADD_RECORD", "EMERGENCY_CONTACT_EDIT"
    val description: String,
    val recordTitle: String = "",
    val authenticatedUser: String = "Patient (Biometrics Verified)",
    val deviceFingerprint: String = "Android Keystore Encrypted AES-256",
    val status: String = "AUTHORIZED" // "AUTHORIZED", "WARNING", "DENIED"
)

data class CloudBackupInfo(
    val backupId: String,
    val timestamp: Long,
    val recordsCount: Int,
    val byteSize: Long,
    val sha256Checksum: String,
    val encryptionStandard: String = "AES-256-GCM (Hardware Backed KeyStore)",
    val deviceName: String = "Android Secure Enclave"
)

data class AsianCountry(
    val code: String,
    val name: String,
    val dialCode: String,
    val flagEmoji: String,
    val currency: String,
    val currencySymbol: String,
    val defaultEmergencyNumber: String,
    val exampleNumber: String
)
