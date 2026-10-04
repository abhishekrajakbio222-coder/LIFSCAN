package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.local.AppDatabase
import com.example.worker.HealthWorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for handling interactive action buttons on local medication notifications
 * (e.g. "Mark as Taken" and "Snooze 15m") directly from the Android notification shade.
 */
class MedicationNotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_TAKEN = "com.example.lifscan.ACTION_MARK_MEDICATION_TAKEN"
        const val ACTION_SNOOZE = "com.example.lifscan.ACTION_SNOOZE_MEDICATION"

        const val EXTRA_MEDICATION_ID = "EXTRA_MEDICATION_ID"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
        const val EXTRA_MED_NAME = "EXTRA_MED_NAME"
        const val EXTRA_DOSAGE = "EXTRA_DOSAGE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val medicationId = intent.getStringExtra(EXTRA_MEDICATION_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, medicationId.hashCode())
        val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Medication"
        val dosage = intent.getStringExtra(EXTRA_DOSAGE) ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)

        when (action) {
            ACTION_MARK_TAKEN -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        db.medicationDao().setMedicationTakenStatus(medicationId, true)
                    } catch (_: Exception) {
                    } finally {
                        pendingResult.finish()
                    }
                }
                Toast.makeText(
                    context,
                    "✅ Dose logged for $medName ($dosage). Great job!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            ACTION_SNOOZE -> {
                HealthWorkScheduler.snoozeMedicationReminder(
                    context = context,
                    medicationId = medicationId,
                    medName = medName,
                    dosage = dosage,
                    snoozeMinutes = 15
                )
                Toast.makeText(
                    context,
                    "⏰ Reminder snoozed for 15 minutes: $medName",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
