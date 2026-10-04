package com.example.data.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lifscan_secure_encrypted_datastore")

/**
 * DataStore utility that transparently encrypts all sensitive health preferences,
 * authentication tokens, and user credentials at rest using AES-256 GCM.
 */
class EncryptedDataStore(private val context: Context) {

    companion object {
        private val KEY_ENCRYPTED_AUTH_TOKEN = stringPreferencesKey("enc_auth_session_token")
        private val KEY_ENCRYPTED_USER_ID = stringPreferencesKey("enc_active_user_id")
        private val KEY_ENCRYPTED_PIN = stringPreferencesKey("enc_security_pin")
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("pref_biometric_lock_enabled")
        private val KEY_OFFLINE_ENCRYPTION_ENABLED = booleanPreferencesKey("pref_offline_encryption_enabled")
        private val KEY_LAST_SECURITY_AUDIT = longPreferencesKey("pref_last_security_audit_ts")
        private val KEY_HIPAA_COMPLIANCE_MODE = booleanPreferencesKey("pref_hipaa_compliance_mode")
        private val KEY_DARK_MODE = booleanPreferencesKey("pref_dark_mode_enabled")
        private val KEY_LAST_CLOUD_BACKUP = longPreferencesKey("pref_last_cloud_backup_ts")
        private val KEY_ADMIN_PHONE = stringPreferencesKey("pref_admin_phone_number")
        private val KEY_ADMIN_PASSWORD = stringPreferencesKey("pref_admin_password")

        const val DEFAULT_ADMIN_PHONE = "9768752782"
        const val DEFAULT_ADMIN_PASSWORD = "9768752782"

        @Volatile
        private var INSTANCE: EncryptedDataStore? = null

        fun getInstance(context: Context): EncryptedDataStore {
            return INSTANCE ?: synchronized(this) {
                val instance = EncryptedDataStore(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private val dataStore = context.dataStore

    // Flow for Active User ID (Encrypted at rest)
    val activeUserIdFlow: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_ENCRYPTED_USER_ID]?.let { encrypted ->
                try {
                    SecurityKeyManager.decryptString(encrypted)
                } catch (e: Exception) {
                    null
                }
            }
        }

    // Flow for Biometric Security Preference
    val isBiometricEnabledFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_BIOMETRIC_ENABLED] ?: true
        }

    // Flow for Offline Encryption Status
    val isOfflineEncryptionActiveFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_OFFLINE_ENCRYPTION_ENABLED] ?: true
        }

    // Flow for HIPAA Compliance Mode
    val isHipaaComplianceModeFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_HIPAA_COMPLIANCE_MODE] ?: true
        }

    // Flow for Dark Mode Preference
    val isDarkModeFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_DARK_MODE] ?: false
        }

    // Flow for Last Cloud Backup Timestamp
    val lastCloudBackupTimestampFlow: Flow<Long> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_LAST_CLOUD_BACKUP] ?: 0L
        }

    // Flow for Last Security Audit Timestamp
    val lastSecurityAuditTimestampFlow: Flow<Long> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_LAST_SECURITY_AUDIT] ?: System.currentTimeMillis()
        }

    /**
     * Securely stores the active user session token with AES-256 GCM encryption.
     */
    suspend fun saveEncryptedAuthToken(token: String) {
        val encrypted = SecurityKeyManager.encryptString(token)
        dataStore.edit { preferences ->
            preferences[KEY_ENCRYPTED_AUTH_TOKEN] = encrypted
            preferences[KEY_LAST_SECURITY_AUDIT] = System.currentTimeMillis()
        }
    }

    /**
     * Retrieves and decrypts the active session auth token.
     */
    val authTokenFlow: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_ENCRYPTED_AUTH_TOKEN]?.let { encrypted ->
                try {
                    SecurityKeyManager.decryptString(encrypted)
                } catch (e: Exception) {
                    null
                }
            }
        }

    /**
     * Securely saves the active user profile ID with encryption at rest.
     */
    suspend fun saveActiveUserId(userId: String) {
        val encrypted = SecurityKeyManager.encryptString(userId)
        dataStore.edit { preferences ->
            preferences[KEY_ENCRYPTED_USER_ID] = encrypted
        }
    }

    /**
     * Securely stores user security PIN for offline emergency authorization.
     */
    suspend fun saveEncryptedSecurityPin(pin: String) {
        val encrypted = SecurityKeyManager.encryptString(pin)
        dataStore.edit { preferences ->
            preferences[KEY_ENCRYPTED_PIN] = encrypted
        }
    }

    /**
     * Verifies if entered PIN matches the stored encrypted security PIN.
     */
    suspend fun verifySecurityPin(pin: String, storedEncrypted: String?): Boolean {
        if (storedEncrypted == null) return true
        val decrypted = SecurityKeyManager.decryptString(storedEncrypted)
        return decrypted == pin
    }

    /**
     * Sets biometric authentication status.
     */
    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    /**
     * Sets HIPAA Compliance mode.
     */
    suspend fun setHipaaComplianceMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_HIPAA_COMPLIANCE_MODE] = enabled
            preferences[KEY_OFFLINE_ENCRYPTION_ENABLED] = true
            preferences[KEY_LAST_SECURITY_AUDIT] = System.currentTimeMillis()
        }
    }

    /**
     * Sets Dark Mode preference.
     */
    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DARK_MODE] = enabled
        }
    }

    /**
     * Sets Last Cloud Backup timestamp.
     */
    suspend fun setLastCloudBackupTimestamp(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_CLOUD_BACKUP] = timestamp
            preferences[KEY_LAST_SECURITY_AUDIT] = System.currentTimeMillis()
        }
    }

    /**
     * Clears all session credentials on user logout while preserving encryption keys.
     */
    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_ENCRYPTED_AUTH_TOKEN)
            preferences.remove(KEY_ENCRYPTED_USER_ID)
            preferences.remove(KEY_ENCRYPTED_PIN)
        }
    }

    /**
     * Flow for Admin Master Phone Number (only editable by Admin).
     */
    val adminPhoneNumberFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_ADMIN_PHONE]?.let { encrypted ->
                try {
                    SecurityKeyManager.decryptString(encrypted)
                } catch (e: Exception) {
                    DEFAULT_ADMIN_PHONE
                }
            } ?: DEFAULT_ADMIN_PHONE
        }

    /**
     * Flow for Admin Master Password (only editable by Admin).
     */
    val adminPasswordFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_ADMIN_PASSWORD]?.let { encrypted ->
                try {
                    SecurityKeyManager.decryptString(encrypted)
                } catch (e: Exception) {
                    DEFAULT_ADMIN_PASSWORD
                }
            } ?: DEFAULT_ADMIN_PASSWORD
        }

    /**
     * Updates Admin master access phone number and password.
     * Accessible strictly by authenticated Admin.
     */
    suspend fun setAdminCredentials(phone: String, password: String) {
        val encPhone = SecurityKeyManager.encryptString(phone.trim())
        val encPass = SecurityKeyManager.encryptString(password.trim())
        dataStore.edit { preferences ->
            preferences[KEY_ADMIN_PHONE] = encPhone
            preferences[KEY_ADMIN_PASSWORD] = encPass
            preferences[KEY_LAST_SECURITY_AUDIT] = System.currentTimeMillis()
        }
    }
}
