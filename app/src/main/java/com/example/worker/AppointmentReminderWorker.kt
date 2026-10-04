package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.util.MedicationNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * WorkManager CoroutineWorker for scheduling and executing background
 * doctor consultation and upcoming appointment reminders.
 */
class AppointmentReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val targetedAppointmentId = inputData.getString(KEY_APPOINTMENT_ID)
            val targetedDoctorName = inputData.getString(KEY_DOCTOR_NAME)
            val targetedSpecialty = inputData.getString(KEY_DOCTOR_SPECIALTY)
            val targetedClinicName = inputData.getString(KEY_CLINIC_NAME)
            val targetedDate = inputData.getString(KEY_APPOINTMENT_DATE)
            val targetedTimeSlot = inputData.getString(KEY_TIME_SLOT)
            val targetedIsVideo = inputData.getBoolean(KEY_IS_VIDEO, true)

            // If targeted appointment parameters are provided directly via WorkManager input
            if (!targetedAppointmentId.isNullOrBlank() && !targetedDoctorName.isNullOrBlank()) {
                MedicationNotificationHelper.sendAppointmentReminderNotification(
                    context = applicationContext,
                    appointmentId = targetedAppointmentId,
                    doctorName = targetedDoctorName,
                    doctorSpecialty = targetedSpecialty ?: "Specialist Physician",
                    clinicName = targetedClinicName ?: "Central Health Hub",
                    appointmentDate = targetedDate ?: "Today",
                    timeSlot = targetedTimeSlot ?: "Scheduled Time",
                    isVideo = targetedIsVideo,
                    customTitle = "🩺 Upcoming Doctor Consultation: $targetedDoctorName",
                    customMessage = "Reminder: Your appointment is scheduled for $targetedTimeSlot ($targetedDate) at $targetedClinicName."
                )
                return@withContext Result.success()
            }

            // Otherwise, query Room Database for upcoming scheduled appointments
            val database = AppDatabase.getDatabase(applicationContext)
            val appointmentDao = database.appointmentDao()
            val upcomingAppointments = appointmentDao.getAllUpcomingAppointments()

            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val tomorrowStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

            for (apt in upcomingAppointments) {
                // If appointment is today or tomorrow, trigger reminder alert
                val isTodayOrSoon = apt.appointmentDate == todayStr || apt.appointmentDate == tomorrowStr || apt.appointmentDate.contains("Today", ignoreCase = true)
                if (isTodayOrSoon && apt.status == "UPCOMING") {
                    val dateLabel = if (apt.appointmentDate == todayStr) "Today" else "Tomorrow (${apt.appointmentDate})"
                    MedicationNotificationHelper.sendAppointmentReminderNotification(
                        context = applicationContext,
                        appointmentId = apt.id,
                        doctorName = apt.doctorName,
                        doctorSpecialty = apt.doctorSpecialty,
                        clinicName = apt.clinicName,
                        appointmentDate = dateLabel,
                        timeSlot = apt.timeSlot,
                        isVideo = apt.isVideoConsultation,
                        customTitle = "📅 Doctor Consultation Alert: ${apt.doctorName}",
                        customMessage = "Your consultation is scheduled for $dateLabel at ${apt.timeSlot} (${apt.clinicName}). Tap to prepare."
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_APPOINTMENT_ID = "key_appointment_id"
        const val KEY_DOCTOR_NAME = "key_doctor_name"
        const val KEY_DOCTOR_SPECIALTY = "key_doctor_specialty"
        const val KEY_CLINIC_NAME = "key_clinic_name"
        const val KEY_APPOINTMENT_DATE = "key_appointment_date"
        const val KEY_TIME_SLOT = "key_time_slot"
        const val KEY_IS_VIDEO = "key_is_video"
    }
}
