package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.accessibility.NotificationRepository
import com.example.accessibility.TtsManager
import com.example.model.CapturedNotification
import com.example.notifications.WhatsAppNotificationService
import com.example.ui.theme.CardPhoneGreen
import com.example.ui.theme.CardWhatsAppBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsDialog(
    ttsManager: TtsManager,
    onStartReplyVoice: (recipient: String, notificationKey: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val notifications by NotificationRepository.notifications.collectAsState()
    var replyingToItem by remember { mutableStateOf<CapturedNotification?>(null) }
    var replyText by remember { mutableStateOf("") }
    var replyStatus by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .border(2.dp, Color.White, RoundedCornerShape(28.dp))
                .testTag("notifications_dialog"),
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
                    Column {
                        Text(
                            text = "Notifiche e Messaggi",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (notifications.isEmpty()) "Nessun nuovo messaggio" else "${notifications.size} messaggi / chiamate",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF27272A), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = Color.White
                        )
                    }
                }

                // Inline Reply Field if active
                replyingToItem?.let { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, CardWhatsAppBlue, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Rispondi a ${item.sender}:",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            OutlinedTextField(
                                value = replyText,
                                onValueChange = { replyText = it },
                                placeholder = { Text("Scrivi la risposta...") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color.White,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(onClick = { replyingToItem = null }) {
                                    Text("Annulla", color = Color.LightGray)
                                }
                                Button(
                                    onClick = {
                                        if (replyText.isNotBlank()) {
                                            val replyAction = WhatsAppNotificationService.activeReplyActions[item.notificationKey]
                                            if (replyAction != null) {
                                                val ok = WhatsAppNotificationService.sendReply(context, replyAction, replyText)
                                                if (ok) {
                                                    replyStatus = "Risposta inviata a ${item.sender}"
                                                    ttsManager.speak("Risposta inviata con successo a ${item.sender}")
                                                    replyingToItem = null
                                                    replyText = ""
                                                } else {
                                                    replyStatus = "Errore invio"
                                                }
                                            } else {
                                                onStartReplyVoice(item.sender, item.notificationKey)
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CardWhatsAppBlue)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Invia")
                                }
                            }
                        }
                    }
                }

                if (replyStatus.isNotEmpty()) {
                    Text(
                        text = replyStatus,
                        color = Color(0xFF4ADE80),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Notification Items List
                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Non ci sono notifiche recenti.",
                            color = Color.Gray,
                            fontSize = 18.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(notifications, key = { it.notificationKey }) { item ->
                            NotificationCardItem(
                                item = item,
                                onReply = {
                                    replyingToItem = item
                                }
                            )
                        }
                    }
                }

                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val pm = context.packageManager
                            val launchIntent = pm.getLaunchIntentForPackage("com.whatsapp")
                            if (launchIntent != null) {
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(launchIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CardWhatsAppBlue),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(54.dp)
                    ) {
                        Icon(Icons.Default.Message, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apri WhatsApp", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedButton(
                        onClick = {
                            NotificationRepository.clearAll()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(54.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Cancella tutto")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cancella", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCardItem(
    item: CapturedNotification,
    onReply: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (item.isWhatsApp) CardWhatsAppBlue else if (item.isMissedCall) Color(0xFFEF4444) else Color(0xFF0284C7),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.isMissedCall) Icons.Default.PhoneMissed else Icons.Default.Message,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.sender,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.appName,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }

                Text(
                    text = formatTime(item.timestamp),
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            }

            if (item.message.isNotBlank()) {
                Text(
                    text = item.message,
                    color = Color(0xFFF1F5F9),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Quick actions row
            if (item.isWhatsApp || item.hasReplyAction) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onReply,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF38BDF8))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rispondi", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}
