package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppointmentEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.EmergencyContactEntity
import com.example.data.model.HealthReportEntity
import com.example.data.model.MedicalFacilityEntity
import com.example.data.model.MedicationEntity
import com.example.data.model.SOSAlertEntity
import com.example.data.model.SkinScanEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.model.VaultAuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE phone = :phone AND role = :role LIMIT 1")
    suspend fun getUserByPhoneAndRole(phone: String, role: UserRole): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'DOCTOR'")
    fun getDoctors(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = 'AMBULANCE_DRIVER'")
    fun getAmbulanceDrivers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)
}

@Dao
interface SkinScanDao {
    @Query("SELECT * FROM skin_scans WHERE patientId = :patientId ORDER BY timestamp DESC")
    fun getScansForPatient(patientId: String): Flow<List<SkinScanEntity>>

    @Query("SELECT * FROM skin_scans WHERE patientId = :patientId ORDER BY timestamp DESC")
    suspend fun getScansListForPatient(patientId: String): List<SkinScanEntity>

    @Query("SELECT * FROM skin_scans WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncScans(): List<SkinScanEntity>

    @Query("UPDATE skin_scans SET syncStatus = 'SYNCED', isCloudSynced = 1 WHERE patientId = :patientId")
    suspend fun markAllScansSynced(patientId: String)

    @Query("SELECT * FROM skin_scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<SkinScanEntity>>

    @Query("SELECT * FROM skin_scans WHERE id = :scanId LIMIT 1")
    suspend fun getScanById(scanId: String): SkinScanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: SkinScanEntity)

    @Query("DELETE FROM skin_scans WHERE id = :scanId")
    suspend fun deleteScan(scanId: String)
}

@Dao
interface HealthReportDao {
    @Query("SELECT * FROM health_reports WHERE patientId = :patientId ORDER BY timestamp DESC")
    fun getReportsForPatient(patientId: String): Flow<List<HealthReportEntity>>

    @Query("SELECT * FROM health_reports WHERE patientId = :patientId ORDER BY timestamp DESC")
    suspend fun getReportsListForPatient(patientId: String): List<HealthReportEntity>

    @Query("SELECT * FROM health_reports WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncReports(): List<HealthReportEntity>

    @Query("UPDATE health_reports SET syncStatus = 'SYNCED' WHERE patientId = :patientId")
    suspend fun markAllReportsSynced(patientId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: HealthReportEntity)

    @Query("DELETE FROM health_reports WHERE id = :reportId")
    suspend fun deleteReport(reportId: String)
}

@Dao
interface SOSAlertDao {
    @Query("SELECT * FROM sos_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<SOSAlertEntity>>

    @Query("SELECT * FROM sos_alerts WHERE status != 'RESOLVED' ORDER BY timestamp DESC")
    fun getActiveAlerts(): Flow<List<SOSAlertEntity>>

    @Query("SELECT * FROM sos_alerts WHERE id = :alertId LIMIT 1")
    suspend fun getAlertById(alertId: String): SOSAlertEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: SOSAlertEntity)

    @Update
    suspend fun updateAlert(alert: SOSAlertEntity)
}

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments ORDER BY timestamp DESC")
    fun getAllAppointments(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE status = 'UPCOMING' OR status = 'CONFIRMED' OR status = 'RESCHEDULED' ORDER BY timestamp ASC")
    fun getActiveUpcomingAppointments(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE status = 'UPCOMING' ORDER BY timestamp ASC")
    suspend fun getAllUpcomingAppointments(): List<AppointmentEntity>

    @Query("SELECT * FROM appointments WHERE patientId = :patientId ORDER BY timestamp DESC")
    fun getAppointmentsForPatient(patientId: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE patientId = :patientId ORDER BY timestamp DESC")
    suspend fun getAppointmentsListForPatient(patientId: String): List<AppointmentEntity>

    @Query("SELECT * FROM appointments WHERE doctorId = :doctorId ORDER BY timestamp DESC")
    fun getAppointmentsForDoctor(doctorId: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE status = :status ORDER BY timestamp DESC")
    fun getAppointmentsByStatus(status: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE appointmentDate = :date ORDER BY timeSlot ASC")
    fun getAppointmentsByDate(date: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE id = :appointmentId LIMIT 1")
    suspend fun getAppointmentById(appointmentId: String): AppointmentEntity?

    @Query("SELECT * FROM appointments WHERE doctorId = :doctorId AND appointmentDate = :date")
    suspend fun getBookedSlotsForDoctorOnDate(doctorId: String, date: String): List<AppointmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity)

    @Update
    suspend fun updateAppointment(appointment: AppointmentEntity)

    @Query("UPDATE appointments SET status = 'CANCELLED' WHERE id = :appointmentId")
    suspend fun cancelAppointmentById(appointmentId: String)

    @Query("UPDATE appointments SET appointmentDate = :newDate, timeSlot = :newTimeSlot, status = 'RESCHEDULED' WHERE id = :appointmentId")
    suspend fun rescheduleAppointmentById(appointmentId: String, newDate: String, newTimeSlot: String)

    @Query("DELETE FROM appointments WHERE id = :appointmentId")
    suspend fun deleteAppointment(appointmentId: String)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTransactionsForUser(userId: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)
}

@Dao
interface MedicalFacilityDao {
    @Query("SELECT * FROM medical_facilities ORDER BY distanceKm ASC")
    fun getAllFacilities(): Flow<List<MedicalFacilityEntity>>

    @Query("SELECT * FROM medical_facilities WHERE facilityType = :type ORDER BY distanceKm ASC")
    fun getFacilitiesByType(type: String): Flow<List<MedicalFacilityEntity>>

    @Query("SELECT * FROM medical_facilities WHERE id = :id LIMIT 1")
    suspend fun getFacilityById(id: String): MedicalFacilityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFacilities(facilities: List<MedicalFacilityEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFacility(facility: MedicalFacilityEntity)

    @Update
    suspend fun updateFacility(facility: MedicalFacilityEntity)

    @Query("DELETE FROM medical_facilities WHERE id = :id")
    suspend fun deleteFacility(id: String)
}

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE patientId = :patientId ORDER BY scheduledHour ASC, scheduledMinute ASC")
    fun getMedicationsForPatient(patientId: String): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE isReminderEnabled = 1 ORDER BY scheduledHour ASC, scheduledMinute ASC")
    suspend fun getActiveRemindersList(): List<MedicationEntity>

    @Query("SELECT * FROM medications WHERE patientId = :patientId ORDER BY scheduledHour ASC, scheduledMinute ASC")
    suspend fun getMedicationsListForPatient(patientId: String): List<MedicationEntity>

    @Query("SELECT * FROM medications ORDER BY scheduledHour ASC")
    fun getAllMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE id = :medicationId LIMIT 1")
    suspend fun getMedicationById(medicationId: String): MedicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity)

    @Update
    suspend fun updateMedication(medication: MedicationEntity)

    @Query("UPDATE medications SET isTakenToday = :isTaken, streakDays = CASE WHEN :isTaken THEN streakDays + 1 ELSE streakDays END WHERE id = :medicationId")
    suspend fun setMedicationTakenStatus(medicationId: String, isTaken: Boolean)

    @Query("UPDATE medications SET isReminderEnabled = :enabled WHERE id = :medicationId")
    suspend fun toggleReminder(medicationId: String, enabled: Boolean)

    @Query("DELETE FROM medications WHERE id = :medicationId")
    suspend fun deleteMedication(medicationId: String)
}

@Dao
interface EmergencyContactDao {
    @Query("SELECT * FROM emergency_contacts WHERE patientId = :patientId ORDER BY isPrimary DESC, timestamp DESC")
    fun getContactsForPatient(patientId: String): Flow<List<EmergencyContactEntity>>

    @Query("SELECT * FROM emergency_contacts ORDER BY isPrimary DESC, timestamp DESC")
    fun getAllContacts(): Flow<List<EmergencyContactEntity>>

    @Query("SELECT * FROM emergency_contacts WHERE patientId = :patientId AND isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryContact(patientId: String): EmergencyContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContactEntity)

    @Update
    suspend fun updateContact(contact: EmergencyContactEntity)

    @Query("UPDATE emergency_contacts SET isPrimary = 0 WHERE patientId = :patientId")
    suspend fun clearPrimaryFlags(patientId: String)

    @Query("UPDATE emergency_contacts SET isPrimary = 1 WHERE id = :contactId")
    suspend fun setPrimary(contactId: String)

    @Query("DELETE FROM emergency_contacts WHERE id = :contactId")
    suspend fun deleteContact(contactId: String)
}

@Dao
interface VaultAuditLogDao {
    @Query("SELECT * FROM vault_audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<VaultAuditLogEntity>>

    @Query("SELECT * FROM vault_audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 30): Flow<List<VaultAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: VaultAuditLogEntity)

    @Query("DELETE FROM vault_audit_logs")
    suspend fun clearLogs()
}
