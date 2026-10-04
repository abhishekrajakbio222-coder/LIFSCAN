package com.example.ui.features

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentCheckoutDialog(
    title: String,
    amount: Double,
    currency: String,
    onDismiss: () -> Unit,
    onPaymentSuccess: (method: String) -> Unit
) {
    var selectedMethod by remember {
        mutableStateOf(
            if (currency == "NPR") "eSewa / Khalti Digital Wallet"
            else if (currency == "INR") "UPI / PhonePe / GPay"
            else "Credit / Debit Card (Stripe)"
        )
    }

    val paymentOptions = listOf(
        "eSewa / Khalti Digital Wallet" to "🇳🇵 Nepal Instant Mobile Pay",
        "UPI / PhonePe / GPay" to "🇮🇳 India Fast UPI & QR",
        "Credit / Debit Card (Stripe)" to "💳 Visa, MasterCard, Amex",
        "Razorpay Payment Gateway" to "⚡ NetBanking & Smart Wallets",
        "Direct Bank Wire Transfer" to "🏦 Instant Account Settlement"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payment, contentDescription = null, tint = TealPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Secure Payment Gateway", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                // Invoice summary card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TealLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("TOTAL PAYABLE AMOUNT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TealDark)
                        Text(
                            "$currency ${String.format("%.2f", amount)}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("SELECT PAYMENT METHOD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))

                paymentOptions.forEach { (method, desc) ->
                    val isSelected = selectedMethod == method
                    OutlinedCard(
                        onClick = { selectedMethod = method },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = CardDefaults.outlinedCardBorder(isSelected),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isSelected) TealPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedMethod = method },
                                colors = RadioButtonDefaults.colors(selectedColor = TealPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(method, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("256-bit SSL Encrypted & RBI/NRB Compliant", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPaymentSuccess(selectedMethod) },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Pay $currency ${String.format("%.2f", amount)}", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ExportHealthDataDialog(
    viewModel: LifscanViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var exportFormat by remember { mutableStateOf("PDF") }
    val clipboardManager = LocalClipboardManager.current
    var generatedPdfResult by remember { mutableStateOf<com.example.util.PdfHealthReportExporter.ExportResult?>(null) }

    val exportText = if (exportFormat == "PDF") viewModel.exportDataPDF() else viewModel.exportDataCSV()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Download, contentDescription = null, tint = TealPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Secure Medical Profile", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = exportFormat == "PDF",
                        onClick = { exportFormat = "PDF" },
                        label = { Text("Encrypted PDF Report") },
                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) }
                    )
                    FilterChip(
                        selected = exportFormat == "CSV",
                        onClick = { exportFormat = "CSV" },
                        label = { Text("Raw CSV") },
                        leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(10.dp)
                    ) {
                        Text(
                            exportText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (exportFormat == "PDF") {
                    Button(
                        onClick = {
                            val result = viewModel.generateSecureHealthProfilePdf(context)
                            viewModel.shareExportedPdf(context, result)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate & Share PDF")
                    }
                } else {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(exportText))
                            viewModel.showFeedback("Medical record ($exportFormat) copied to clipboard!")
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy CSV")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun OfflineSecurityStatusDialog(
    viewModel: LifscanViewModel,
    onDismiss: () -> Unit
) {
    val isBiometricEnabled by viewModel.isBiometricSecurityEnabled.collectAsState()
    val isHipaaComplianceActive by viewModel.isHipaaComplianceActive.collectAsState()
    val isOfflineEncryptionActive by viewModel.isOfflineEncryptionActive.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = SuccessGreen.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Offline Security & Encryption", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("SQLCipher 256-bit AES at Rest", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Encryption Status Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Database Encryption", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Surface(shape = RoundedCornerShape(6.dp), color = SuccessGreen.copy(alpha = 0.15f)) {
                                Text("SQLCipher v4", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text(
                            "All sensitive patient records, skin lesion photos, diagnostic biomarkers, prescriptions, and private chat logs are fully encrypted at rest using a 256-bit AES key.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("KeyStore Vault", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Surface(shape = RoundedCornerShape(6.dp), color = TealPrimary.copy(alpha = 0.15f)) {
                                Text("Hardware TEE / StrongBox", color = TealPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text(
                            "Master encryption keys are generated and protected inside the device's hardware-backed Android KeyStore security module.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("DataStore Security", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Surface(shape = RoundedCornerShape(6.dp), color = SkinScannerPink.copy(alpha = 0.15f)) {
                                Text("AES-GCM Auth", color = SkinScannerPink, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text(
                            "EncryptedDataStore protects session auth tokens, PIN codes, and sensitive profile preferences against unauthorized offline access.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Security Switches
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Biometric Lock & TEE", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Require biometric / PIN for sensitive records", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { viewModel.toggleBiometricSecurity() },
                                modifier = Modifier.testTag("biometric_security_switch")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("HIPAA & GDPR Encryption Mode", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Enforces military-grade offline data sanitization", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isHipaaComplianceActive,
                                onCheckedChange = { viewModel.toggleHipaaCompliance() },
                                modifier = Modifier.testTag("hipaa_compliance_switch")
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Done")
            }
        }
    )
}
