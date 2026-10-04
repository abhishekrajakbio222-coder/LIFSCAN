package com.example.data.interceptor

import com.example.data.local.VaultAuditLogDao
import com.example.data.model.VaultAuditLogEntity
import java.util.UUID

/**
 * Interceptor that intercepts and records read/write/update/delete/export operations
 * on the encrypted Medical Vault database, logging timestamp, action type,
 * record reference, and authorization state into a secure audit table for user review.
 */
class VaultDatabaseLogger(
    private val vaultAuditLogDao: VaultAuditLogDao
) {
    suspend fun logOperation(
        actionType: VaultActionType,
        recordTitle: String,
        details: String,
        user: String = "Authorized User",
        status: String = "AUTHORIZED"
    ) {
        val logEntity = VaultAuditLogEntity(
            id = "log_${UUID.randomUUID().toString().take(8)}",
            timestamp = System.currentTimeMillis(),
            accessType = actionType.name,
            description = details,
            recordTitle = recordTitle,
            authenticatedUser = user,
            deviceFingerprint = "Android Keystore Encrypted AES-256 GCM",
            status = status
        )
        try {
            vaultAuditLogDao.insertLog(logEntity)
        } catch (_: Exception) {
            // Failsafe so logging never crashes core DB operations
        }
    }
}

enum class VaultActionType {
    DB_READ,
    DB_WRITE,
    DB_UPDATE,
    DB_DELETE,
    BIOMETRIC_AUTH_SUCCESS,
    BIOMETRIC_AUTH_FAILURE,
    EXPORT_PDF_REPORT,
    CLOUD_BACKUP_SYNC,
    CLOUD_BACKUP_RESTORE,
    EMERGENCY_SOS_DISPATCH,
    EMERGENCY_CONTACT_EDIT,
    MEDICATION_SCHEDULE_UPDATE
}
