package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.MedicationEntity
import com.example.receiver.MedicationNotificationActionReceiver

object MedicationNotificationHelper {

    const val MEDICATION_CHANNEL_ID = "medication_reminders_channel"
    const val MEDICATION_CHANNEL_NAME = "Medication & Prescription Reminders"
    const val MEDICATION_CHANNEL_DESCRIPTION = "Timely alerts for daily prescribed medications and dosages"

    const val APPOINTMENT_CHANNEL_ID = "appointment_reminders_channel"
    const val APPOINTMENT_CHANNEL_NAME = "Doctor Consultation & Appointment Alerts"
    const val APPOINTMENT_CHANNEL_DESCRIPTION = "Alerts for upcoming scheduled doctor video calls and clinic visits"

    // Backwards compatibility alias
    const val CHANNEL_ID = MEDICATION_CHANNEL_ID

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val importance = NotificationManager.IMPORTANCE_HIGH
            
            // Medication Channel
            val medChannel = NotificationChannel(MEDICATION_CHANNEL_ID, MEDICATION_CHANNEL_NAME, importance).apply {
                description = MEDICATION_CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(medChannel)

            // Appointment Channel
            val aptChannel = NotificationChannel(APPOINTMENT_CHANNEL_ID, APPOINTMENT_CHANNEL_NAME, importance).apply {
                description = APPOINTMENT_CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(aptChannel)
        }
    }

    /**
     * Dispatches a local push notification for a prescribed medication
     */
    fun sendMedicationReminderNotification(
        context: Context,
        medication: MedicationEntity,
        customTitle: String? = null,
        customMessage: String? = null
    ): Boolean {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_SCREEN", "MEDICATION_SCHEDULE")
            putExtra("MEDICATION_ID", medication.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            medication.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = customTitle ?: "💊 Medication Reminder: ${medication.name}"
        val message = customMessage ?: "Time for your scheduled dose: ${medication.dosage} (${medication.timeSlot}). ${medication.instructions}"

        // Action 1: Mark Taken directly from notification
        val markTakenIntent = Intent(context, MedicationNotificationActionReceiver::class.java).apply {
            action = MedicationNotificationActionReceiver.ACTION_MARK_TAKEN
            putExtra(MedicationNotificationActionReceiver.EXTRA_MEDICATION_ID, medication.id)
            putExtra(MedicationNotificationActionReceiver.EXTRA_NOTIFICATION_ID, medication.id.hashCode())
            putExtra(MedicationNotificationActionReceiver.EXTRA_MED_NAME, medication.name)
            putExtra(MedicationNotificationActionReceiver.EXTRA_DOSAGE, medication.dosage)
        }
        val markTakenPendingIntent = PendingIntent.getBroadcast(
            context,
            (medication.id + "_taken").hashCode(),
            markTakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Snooze 15 minutes
        val snoozeIntent = Intent(context, MedicationNotificationActionReceiver::class.java).apply {
            action = MedicationNotificationActionReceiver.ACTION_SNOOZE
            putExtra(MedicationNotificationActionReceiver.EXTRA_MEDICATION_ID, medication.id)
            putExtra(MedicationNotificationActionReceiver.EXTRA_NOTIFICATION_ID, medication.id.hashCode())
            putExtra(MedicationNotificationActionReceiver.EXTRA_MED_NAME, medication.name)
            putExtra(MedicationNotificationActionReceiver.EXTRA_DOSAGE, medication.dosage)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (medication.id + "_snooze").hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, MEDICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setSummaryText("Prescribed by ${medication.doctorName}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "✅ Mark Taken", markTakenPendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "⏰ Snooze 15m", snoozePendingIntent)

        return try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                val notificationId = medication.id.hashCode()
                notificationManager.notify(notificationId, builder.build())
                true
            } else {
                false
            }
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Dispatches a local push notification for an upcoming doctor appointment
     */
    fun sendAppointmentReminderNotification(
        context: Context,
        appointmentId: String,
        doctorName: String,
        doctorSpecialty: String,
        clinicName: String,
        appointmentDate: String,
        timeSlot: String,
        isVideo: Boolean,
        customTitle: String? = null,
        customMessage: String? = null
    ): Boolean {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_SCREEN", "CONSULTATION_SCHEDULER")
            putExtra("APPOINTMENT_ID", appointmentId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            appointmentId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val modeLabel = if (isVideo) "HD Telehealth Video Call" else "In-Person Clinic Visit"
        val title = customTitle ?: "🩺 Upcoming Consultation: $doctorName"
        val message = customMessage ?: "Scheduled for $timeSlot ($appointmentDate). $doctorSpecialty at $clinicName [$modeLabel]."

        val builder = NotificationCompat.Builder(context, APPOINTMENT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setSummaryText("Tap to review appointment details or connect")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        return try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                val notificationId = appointmentId.hashCode()
                notificationManager.notify(notificationId, builder.build())
                true
            } else {
                false
            }
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Dispatches an immediate test push notification to verify sound, banner and vibration
     */
    fun sendTestNotification(context: Context, medName: String, dosage: String): Boolean {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, MEDICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("🔔 Test Dose Alert: $medName ($dosage)")
            .setContentText("Local push notifications are active and synchronized with your encrypted vault.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Prescription reminder system is active. You will receive alerts at scheduled intervals.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        return try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(1001, builder.build())
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Dispatches a rich notification for a freshly parsed medication from a scanned document.
     */
    fun sendParsedMedicationNotification(
        context: Context,
        medName: String,
        dosage: String,
        timing: String,
        instructions: String,
        doctorName: String
    ): Boolean {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_SCREEN", "MEDICATION_SCHEDULE")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            medName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "💊 Rx Reminder: $medName ($dosage)"
        val message = "Scheduled for $timing. $instructions (Prescribed by $doctorName)"

        val builder = NotificationCompat.Builder(context, MEDICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setSummaryText("WorkManager Background Dose Alert")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        return try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(medName.hashCode(), builder.build())
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
