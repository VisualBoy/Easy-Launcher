package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.accessibility.SosManager
import com.example.accessibility.TorchController
import com.example.accessibility.TtsManager
import com.example.model.ContactItem
import com.example.notifications.WhatsAppNotificationService
import com.example.services.WhatsAppAccessibilityFallbackService
import com.example.ui.theme.CardPhoneGreen
import com.example.ui.theme.CardSettingsGold
import com.example.ui.theme.CardSosRed

@Composable
fun SettingsDialog(
    sosManager: SosManager,
    torchController: TorchController,
    ttsManager: TtsManager,
    onTestVoiceAssistant: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isNotifEnabled by remember {
        mutableStateOf(WhatsAppNotificationService.isNotificationServiceEnabled(context))
    }
    var isAccessEnabled by remember {
        mutableStateOf(WhatsAppAccessibilityFallbackService.isAccessibilityServiceEnabled(context))
    }

    val contacts by sosManager.emergencyContacts.collectAsState()
    var showAddContactDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .border(2.dp, Color.White, RoundedCornerShape(28.dp))
                .testTag("settings_dialog"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = CardSettingsGold,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Impostazioni & Assistenza",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF27272A), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = Color.White)
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Accessibility & Notification Permission Status
                    item {
                        Text(
                            text = "PERMESSI DI SISTEMA ACCESSIBILITÀ",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Notification Listener Status Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Accesso Notifiche WhatsApp",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = if (isNotifEnabled) "Attivo (Risposta automatica via RemoteInput abilitata)" else "Richiesto per leggere e rispondere ai messaggi",
                                        color = if (isNotifEnabled) Color(0xFF4ADE80) else Color(0xFFF87171),
                                        fontSize = 13.sp
                                    )
                                }
                                Button(
                                    onClick = {
                                        WhatsAppNotificationService.openNotificationSettings(context)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isNotifEnabled) Color(0xFF334155) else CardPhoneGreen
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(if (isNotifEnabled) "Verifica" else "Attiva")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Accessibility Fallback Service Status Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Servizio Accessibilità UI",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = if (isAccessEnabled) "Attivo (Automazione tasti e fallback WhatsApp)" else "Necessario per il controllo vocale su app terze",
                                        color = if (isAccessEnabled) Color(0xFF4ADE80) else Color(0xFFF87171),
                                        fontSize = 13.sp
                                    )
                                }
                                Button(
                                    onClick = {
                                        WhatsAppAccessibilityFallbackService.openAccessibilitySettings(context)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isAccessEnabled) Color(0xFF334155) else CardPhoneGreen
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(if (isAccessEnabled) "Verifica" else "Attiva")
                                }
                            }
                        }
                    }

                    // 2. Emergency Contacts Setup
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CONTATTI DI EMERGENZA SOS",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { showAddContactDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00E5FF))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Aggiungi", color = Color(0xFF00E5FF))
                            }
                        }
                    }

                    items(contacts) { contact ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = CardSosRed,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = contact.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "${contact.phoneNumber} (${contact.relationship})",
                                            color = Color.LightGray,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val updated = contacts.filter { it.id != contact.id }
                                        sosManager.emergencyContacts.value = updated
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Rimuovi",
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Quick Test & Accessibility Utilities
                    item {
                        Text(
                            text = "TEST E STRUMENTI",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    torchController.toggleTorch()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.FlashlightOn, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Torcia")
                            }

                            Button(
                                onClick = {
                                    onDismiss()
                                    onTestVoiceAssistant()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Microfono AI")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                onDismiss()
                                sosManager.startInstantVoiceSos()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CardSosRed),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test SOS Vocale 10s + WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showAddContactDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var rel by remember { mutableStateOf("Famiglia") }

        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = { Text("Nuovo Contatto SOS", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome (es. Marco)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Numero (es. +39333...)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rel,
                        onValueChange = { rel = it },
                        label = { Text("Relazione (es. Figlio, Medico)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            val newItem = ContactItem(
                                id = "sos_${System.currentTimeMillis()}",
                                name = name.trim(),
                                phoneNumber = phone.trim(),
                                isEmergencyContact = true,
                                relationship = rel.trim()
                            )
                            sosManager.emergencyContacts.value = contacts + newItem
                            showAddContactDialog = false
                        }
                    }
                ) {
                    Text("Salva")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text("Annulla")
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
