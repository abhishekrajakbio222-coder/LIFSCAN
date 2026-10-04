package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.local.AppDatabase
import com.example.data.model.MedicationEntity
import com.example.util.MedicationNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager Worker that evaluates medication schedules
 * from the encrypted Room database and triggers push notifications
 * at the exact times specified in their daily plan or on periodic intervals.
 */
class MedicationReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_MEDICATION_ID = "KEY_MEDICATION_ID"
        const val KEY_MED_NAME = "KEY_MED_NAME"
        const val KEY_DOSAGE = "KEY_DOSAGE"
        const val KEY_TIME_SLOT = "KEY_TIME_SLOT"
        const val KEY_DOCTOR_NAME = "KEY_DOCTOR_NAME"
        const val KEY_INSTRUCTIONS = "KEY_INSTRUCTIONS"
        const val KEY_IS_SNOOZE = "KEY_IS_SNOOZE"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val medicationDao = database.medicationDao()

            val specificMedId = inputData.getString(KEY_MEDICATION_ID)
            val isSnooze = inputData.getBoolean(KEY_IS_SNOOZE, false)

            if (!specificMedId.isNullOrBlank()) {
                // Targeted medication alert fired from WorkManager
                val med = medicationDao.getMedicationById(specificMedId)
                if (med != null) {
                    if (med.isReminderEnabled && (!med.isTakenToday || isSnooze)) {
                        MedicationNotificationHelper.sendMedicationReminderNotification(
                            context = applicationContext,
                            medication = med,
                            customTitle = if (isSnooze) "⏰ Snoozed Dose: ${med.name} (${med.dosage})" else "💊 Prescription Dose Alert: ${med.name} (${med.dosage})",
                            customMessage = "Scheduled for ${med.timeSlot}. ${med.instructions} (Prescribed by ${med.doctorName})"
                        )
                    }
                    // If not a snooze, reschedule for the next day at the same time
                    if (!isSnooze && med.isReminderEnabled) {
                        HealthWorkScheduler.scheduleMedicationReminder(
                            context = applicationContext,
                            medication = med
                        )
                    }
                } else {
                    // Fallback to inputData payload if not yet found
                    val medName = inputData.getString(KEY_MED_NAME) ?: "Prescription Dose"
                    val dosage = inputData.getString(KEY_DOSAGE) ?: "1 dose"
                    val timeSlot = inputData.getString(KEY_TIME_SLOT) ?: "Scheduled Time"
                    val instructions = inputData.getString(KEY_INSTRUCTIONS) ?: "Take with water"
                    val doctor = inputData.getString(KEY_DOCTOR_NAME) ?: "Prescribing Physician"

                    val fallbackMed = MedicationEntity(
                        id = specificMedId,
                        patientId = "patient_user",
                        name = medName,
                        dosage = dosage,
                        frequency = "Daily",
                        timeSlot = timeSlot,
                        doctorName = doctor,
                        instructions = instructions,
                        isReminderEnabled = true,
                        isTakenToday = false
                    )

                    MedicationNotificationHelper.sendMedicationReminderNotification(
                        context = applicationContext,
                        medication = fallbackMed
                    )
                }
            } else {
                // Periodic or immediate full check across active medications
                val activeMedications = medicationDao.getActiveRemindersList()
                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(Calendar.MINUTE)

                for (med in activeMedications) {
                    val scheduledHour = med.scheduledHour
                    val scheduledMin = med.scheduledMinute

                    val matchesExactOrClose = (scheduledHour == currentHour && Math.abs(scheduledMin - currentMinute) <= 25)
                    val matchesTimeSlot = when {
                        currentHour in 6..10 && med.timeSlot.contains("Morning", ignoreCase = true) -> true
                        currentHour in 12..15 && (med.timeSlot.contains("Afternoon", ignoreCase = true) || med.timeSlot.contains("Lunch", ignoreCase = true) || med.timeSlot.contains("Noon", ignoreCase = true)) -> true
                        currentHour in 18..21 && med.timeSlot.contains("Evening", ignoreCase = true) -> true
                        currentHour in 21..23 && (med.timeSlot.contains("Night", ignoreCase = true) || med.timeSlot.contains("Bed", ignoreCase = true)) -> true
                        else -> false
                    }

                    if ((matchesExactOrClose || matchesTimeSlot) && !med.isTakenToday && med.isReminderEnabled) {
                        MedicationNotificationHelper.sendMedicationReminderNotification(
                            context = applicationContext,
                            medication = med,
                            customTitle = "💊 Prescription Dose Alert: ${med.name} (${med.dosage})",
                            customMessage = "Scheduled for ${med.timeSlot}. Take with water as prescribed by ${med.doctorName}."
                        )
                    }
                }
            }

            MedicationNotificationHelper.createNotificationChannel(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

/**
 * Unified WorkScheduler to register periodic and exact background tasks for
 * medication tracking and doctor appointment reminders via AndroidX WorkManager.
 */
object HealthWorkScheduler {

    const val MEDICATION_PERIODIC_TAG = "lifscan_medication_periodic_reminders"
    const val APPOINTMENT_PERIODIC_TAG = "lifscan_appointment_periodic_reminders"
    const val EXACT_MEDICATION_WORK_TAG = "lifscan_medication_exact_reminders"
    const val EXACT_APPOINTMENT_WORK_TAG = "lifscan_appointment_exact_reminders"

    /**
     * Enqueues periodic background workers running every 15 minutes to evaluate medication and appointment schedules.
     */
    fun schedulePeriodicHealthReminders(context: Context) {
        val medWorkRequest = PeriodicWorkRequestBuilder<MedicationReminderWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        )
            .addTag(MEDICATION_PERIODIC_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MEDICATION_PERIODIC_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            medWorkRequest
        )

        val aptWorkRequest = PeriodicWorkRequestBuilder<AppointmentReminderWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        )
            .addTag(APPOINTMENT_PERIODIC_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            APPOINTMENT_PERIODIC_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            aptWorkRequest
        )
    }

    /**
     * Calculates the millisecond delay until the next occurrence of a scheduled hour and minute.
     */
    fun calculateInitialDelayMs(scheduledHour: Int, scheduledMinute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, scheduledHour)
            set(Calendar.MINUTE, scheduledMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                // If scheduled time has already passed today, target tomorrow
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }

    /**
     * Schedules a specific medication reminder via WorkManager OneTimeWorkRequest.
     */
    fun scheduleMedicationReminder(
        context: Context,
        medication: MedicationEntity,
        overrideDelayMinutes: Long? = null
    ) {
        val delayMs = if (overrideDelayMinutes != null) {
            TimeUnit.MINUTES.toMillis(overrideDelayMinutes)
        } else {
            calculateInitialDelayMs(medication.scheduledHour, medication.scheduledMinute)
        }

        val inputData = workDataOf(
            MedicationReminderWorker.KEY_MEDICATION_ID to medication.id,
            MedicationReminderWorker.KEY_MED_NAME to medication.name,
            MedicationReminderWorker.KEY_DOSAGE to medication.dosage,
            MedicationReminderWorker.KEY_TIME_SLOT to medication.timeSlot,
            MedicationReminderWorker.KEY_DOCTOR_NAME to medication.doctorName,
            MedicationReminderWorker.KEY_INSTRUCTIONS to medication.instructions,
            MedicationReminderWorker.KEY_IS_SNOOZE to false
        )

        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInputData(inputData)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag(EXACT_MEDICATION_WORK_TAG)
            .addTag("med_${medication.id}")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "${EXACT_MEDICATION_WORK_TAG}_${medication.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        // Ensure periodic health reminders worker is active
        schedulePeriodicHealthReminders(context)
    }

    /**
     * Schedules WorkManager notification jobs for all parsed medications extracted from scanned medical documents.
     */
    fun scheduleRemindersForParsedMedications(
        context: Context,
        medications: List<MedicationEntity>
    ) {
        for (med in medications) {
            scheduleMedicationReminder(context, med)
        }
        schedulePeriodicHealthReminders(context)
    }

    /**
     * Snoozes a medication notification for a specified number of minutes.
     */
    fun snoozeMedicationReminder(
        context: Context,
        medicationId: String,
        medName: String,
        dosage: String,
        snoozeMinutes: Long = 15
    ) {
        val inputData = workDataOf(
            MedicationReminderWorker.KEY_MEDICATION_ID to medicationId,
            MedicationReminderWorker.KEY_MED_NAME to medName,
            MedicationReminderWorker.KEY_DOSAGE to dosage,
            MedicationReminderWorker.KEY_IS_SNOOZE to true
        )

        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInputData(inputData)
            .setInitialDelay(snoozeMinutes, TimeUnit.MINUTES)
            .addTag(EXACT_MEDICATION_WORK_TAG)
            .addTag("snooze_$medicationId")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "snooze_${medicationId}_${System.currentTimeMillis()}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Cancels a scheduled medication reminder in WorkManager.
     */
    fun cancelMedicationReminder(context: Context, medicationId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("${EXACT_MEDICATION_WORK_TAG}_$medicationId")
    }

    /**
     * Schedules a specific doctor consultation reminder through WorkManager with metadata payload.
     */
    fun scheduleAppointmentReminder(
        context: Context,
        appointmentId: String,
        doctorName: String,
        doctorSpecialty: String,
        clinicName: String,
        appointmentDate: String,
        timeSlot: String,
        isVideo: Boolean,
        delayMinutes: Long = 0
    ) {
        val inputData = workDataOf(
            AppointmentReminderWorker.KEY_APPOINTMENT_ID to appointmentId,
            AppointmentReminderWorker.KEY_DOCTOR_NAME to doctorName,
            AppointmentReminderWorker.KEY_DOCTOR_SPECIALTY to doctorSpecialty,
            AppointmentReminderWorker.KEY_CLINIC_NAME to clinicName,
            AppointmentReminderWorker.KEY_APPOINTMENT_DATE to appointmentDate,
            AppointmentReminderWorker.KEY_TIME_SLOT to timeSlot,
            AppointmentReminderWorker.KEY_IS_VIDEO to isVideo
        )

        val workRequestBuilder = OneTimeWorkRequestBuilder<AppointmentReminderWorker>()
            .setInputData(inputData)
            .addTag(EXACT_APPOINTMENT_WORK_TAG)

        if (delayMinutes > 0) {
            workRequestBuilder.setInitialDelay(delayMinutes, TimeUnit.MINUTES)
        }

        WorkManager.getInstance(context).enqueueUniqueWork(
            "${EXACT_APPOINTMENT_WORK_TAG}_$appointmentId",
            ExistingWorkPolicy.REPLACE,
            workRequestBuilder.build()
        )
    }

    /**
     * Triggers an immediate one-time schedule check for both medications and appointments.
     */
    fun triggerImmediateCheck(context: Context) {
        val medWork = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .addTag(EXACT_MEDICATION_WORK_TAG)
            .build()

        val aptWork = OneTimeWorkRequestBuilder<AppointmentReminderWorker>()
            .addTag(EXACT_APPOINTMENT_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            EXACT_MEDICATION_WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            medWork
        )

        WorkManager.getInstance(context).enqueueUniqueWork(
            EXACT_APPOINTMENT_WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            aptWork
        )
    }
}

/**
 * Backwards-compatible alias for MedicationWorkScheduler
 */
object MedicationWorkScheduler {
    fun schedulePeriodicReminders(context: Context) {
        HealthWorkScheduler.schedulePeriodicHealthReminders(context)
    }

    fun triggerImmediateCheck(context: Context) {
        HealthWorkScheduler.triggerImmediateCheck(context)
    }

    fun scheduleRemindersForParsedMedications(context: Context, medications: List<MedicationEntity>) {
        HealthWorkScheduler.scheduleRemindersForParsedMedications(context, medications)
    }
}
