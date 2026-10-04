package com.example.ui.features

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmergencyContactEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.LifscanViewModel
import com.example.ui.viewmodel.ScreenNav

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(viewModel: LifscanViewModel) {
    val context = LocalContext.current
    val contacts by viewModel.patientEmergencyContacts.collectAsState()
    val primaryContact by viewModel.primaryEmergencyContact.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var contactToEdit by remember { mutableStateOf<EmergencyContactEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<EmergencyContactEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = EmergencyRed.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ContactEmergency,
                                    contentDescription = "Emergency Contacts",
                                    tint = EmergencyRed,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Emergency Contacts",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "SQLCipher 256-bit Encrypted • SOS Auto-Dial",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenNav.PatientDashboard) },
                        modifier = Modifier.testTag("emergency_contacts_back_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("dark_mode_toggle_contacts")
                    ) {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Dark Mode",
                            tint = if (isDarkMode) WarningAmber else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact") },
                text = { Text("Add Emergency Contact", fontWeight = FontWeight.Bold) },
                containerColor = EmergencyRed,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_emergency_contact_fab")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
        ) {
            // Security & Integration Banner
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) SurfaceVariantDark else EmergencyRed.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmergencyRed.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = "Encrypted Vault",
                                    tint = EmergencyRed,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "SOS Automatic Dispatch Integration",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDarkMode) TextPrimaryDark else EmergencyRedDark
                            )
                            Text(
                                "When you trigger the SOS Emergency button, your primary contacts receive an immediate SMS alert with your live GPS location and an auto-dial prompt.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Primary Contact Hero Card
            if (primaryContact != null) {
                item {
                    Text(
                        "PRIMARY EMERGENCY RESPONDER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    PrimaryContactHeroCard(
                        contact = primaryContact!!,
                        onDial = { viewModel.dialContact(context, primaryContact!!.phone) },
                        onSms = {
                            viewModel.smsContact(
                                context,
                                primaryContact!!.phone,
                                "🚨 LIFSCAN MEDICAL EMERGENCY: I have triggered an emergency alert. Please assist immediately! (Lat: 27.7058, Lng: 85.3134)"
                            )
                        },
                        onEdit = { contactToEdit = primaryContact }
                    )
                }
            }

            // All Contacts List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SAVED EMERGENCY CONTACTS (${contacts.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Encrypted in SQLCipher",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (contacts.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.ContactPhone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No Emergency Contacts Added",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Add family members, doctors, or caretakers so they can be alerted instantly during an emergency.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add First Contact")
                            }
                        }
                    }
                }
            } else {
                items(contacts, key = { it.id }) { contact ->
                    EmergencyContactItemCard(
                        contact = contact,
                        isDarkMode = isDarkMode,
                        onDial = { viewModel.dialContact(context, contact.phone) },
                        onSms = {
                            viewModel.smsContact(
                                context,
                                contact.phone,
                                "🚨 LIFSCAN MEDICAL ALERT: Emergency assistance requested. Location: Near Bir Hospital, Kathmandu."
                            )
                        },
                        onSetPrimary = { viewModel.setPrimaryEmergencyContact(contact.id) },
                        onEdit = { contactToEdit = contact },
                        onDelete = { showDeleteConfirmDialog = contact }
                    )
                }
            }
        }
    }

    // Add Contact Dialog
    if (showAddDialog) {
        AddEditContactDialog(
            existingContact = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, relationship, phone, email, isPrimary, autoDial, autoSms ->
                viewModel.addEmergencyContact(name, relationship, phone, email, isPrimary, autoDial, autoSms)
                showAddDialog = false
            }
        )
    }

    // Edit Contact Dialog
    if (contactToEdit != null) {
        AddEditContactDialog(
            existingContact = contactToEdit,
            onDismiss = { contactToEdit = null },
            onSave = { name, relationship, phone, email, isPrimary, autoDial, autoSms ->
                val updated = contactToEdit!!.copy(
                    name = name,
                    relationship = relationship,
                    phone = phone,
                    email = email,
                    isPrimary = isPrimary,
                    autoDialOnSos = autoDial,
                    autoSmsOnSos = autoSms
                )
                viewModel.updateEmergencyContact(updated)
                contactToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = EmergencyRed) },
            title = { Text("Delete Emergency Contact?") },
            text = {
                Text("Are you sure you want to remove ${showDeleteConfirmDialog!!.name} from your encrypted emergency contacts?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEmergencyContact(showDeleteConfirmDialog!!.id, showDeleteConfirmDialog!!.name)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PrimaryContactHeroCard(
    contact: EmergencyContactEntity,
    onDial: () -> Unit,
    onSms: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                contact.name.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                contact.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmergencyRed,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text(
                                    "PRIMARY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "${contact.relationship} • ${contact.phone}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Contact", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDial,
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                    modifier = Modifier.weight(1f).testTag("primary_contact_call_btn")
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Direct Dial", fontWeight = FontWeight.Bold)
                }
                FilledTonalButton(
                    onClick = onSms,
                    modifier = Modifier.weight(1f).testTag("primary_contact_sms_btn")
                ) {
                    Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send SOS SMS", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EmergencyContactItemCard(
    contact: EmergencyContactEntity,
    isDarkMode: Boolean,
    onDial: () -> Unit,
    onSms: () -> Unit,
    onSetPrimary: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (contact.isPrimary) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (contact.isPrimary) EmergencyRed.copy(alpha = 0.15f) else TealPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (contact.isPrimary) Icons.Default.Star else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (contact.isPrimary) EmergencyRed else TealPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                contact.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (contact.isPrimary) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = EmergencyRed.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "Primary",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmergencyRed,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            "${contact.relationship} • ${contact.phone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = EmergencyRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (contact.email.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(contact.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onDial,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onSms,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SMS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (!contact.isPrimary) {
                    OutlinedButton(
                        onClick = onSetPrimary,
                        modifier = Modifier.weight(1.2f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("Set Primary", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditContactDialog(
    existingContact: EmergencyContactEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, relationship: String, phone: String, email: String, isPrimary: Boolean, autoDial: Boolean, autoSms: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(existingContact?.name ?: "") }
    var relationship by remember { mutableStateOf(existingContact?.relationship ?: "Spouse") }
    var phone by remember { mutableStateOf(existingContact?.phone ?: "+977-98") }
    var email by remember { mutableStateOf(existingContact?.email ?: "") }
    var isPrimary by remember { mutableStateOf(existingContact?.isPrimary ?: false) }
    var autoDial by remember { mutableStateOf(existingContact?.autoDialOnSos ?: true) }
    var autoSms by remember { mutableStateOf(existingContact?.autoSmsOnSos ?: true) }

    val relationships = listOf("Spouse", "Parent", "Sibling", "Doctor", "Friend", "Guardian", "Ambulance / Hospital")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (existingContact == null) Icons.Default.PersonAdd else Icons.Default.Edit,
                    contentDescription = null,
                    tint = EmergencyRed
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (existingContact == null) "Add Emergency Contact" else "Edit Emergency Contact",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact Full Name *") },
                    placeholder = { Text("e.g. Suman Shrestha") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("contact_name_input")
                )

                Text("Relationship Category", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    relationships.take(4).forEach { rel ->
                        FilterChip(
                            selected = relationship == rel,
                            onClick = { relationship = rel },
                            label = { Text(rel, fontSize = 11.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    relationships.drop(4).forEach { rel ->
                        FilterChip(
                            selected = relationship == rel,
                            onClick = { relationship = rel },
                            label = { Text(rel, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Phone Number *") },
                    placeholder = { Text("+977-9841234567") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("contact_phone_input")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    placeholder = { Text("contact@example.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Switches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Set as Primary SOS Responder", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Switch(checked = isPrimary, onCheckedChange = { isPrimary = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-Send GPS SMS on SOS", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = autoSms, onCheckedChange = { autoSms = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onSave(name, relationship, phone, email, isPrimary, autoDial, autoSms)
                    }
                },
                enabled = name.isNotBlank() && phone.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                modifier = Modifier.testTag("save_emergency_contact_btn")
            ) {
                Text("Save Contact")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
