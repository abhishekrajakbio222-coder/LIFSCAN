package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
import com.example.data.model.VaultAuditLogEntity
import com.example.data.security.SecurityKeyManager
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        UserEntity::class,
        SkinScanEntity::class,
        HealthReportEntity::class,
        SOSAlertEntity::class,
        AppointmentEntity::class,
        ChatMessageEntity::class,
        TransactionEntity::class,
        MedicalFacilityEntity::class,
        MedicationEntity::class,
        EmergencyContactEntity::class,
        VaultAuditLogEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun skinScanDao(): SkinScanDao
    abstract fun healthReportDao(): HealthReportDao
    abstract fun sosAlertDao(): SOSAlertDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun transactionDao(): TransactionDao
    abstract fun medicalFacilityDao(): MedicalFacilityDao
    abstract fun medicationDao(): MedicationDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun vaultAuditLogDao(): VaultAuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns the encrypted Room database instance protected at rest via SQLCipher
         * with a 256-bit AES key managed by the Android KeyStore.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = SecurityKeyManager.getDatabasePassphrase(context)
                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lifscan_encrypted_database.db"
                )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
