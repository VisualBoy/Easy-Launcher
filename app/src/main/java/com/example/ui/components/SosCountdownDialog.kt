package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.SosState

@Composable
fun SosCountdownDialog(
    sosState: SosState,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(450),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val micWaveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveAlpha"
    )

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(4.dp, Color.White, RoundedCornerShape(28.dp))
                .testTag("sos_emergency_dialog"),
            colors = CardDefaults.cardColors(
                containerColor = if (sosState.isCompleted) Color(0xFF15803D) else Color(0xFF991B1B)
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Icon
                Icon(
                    imageVector = if (sosState.isCompleted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = "Emergenza",
                    tint = if (sosState.isCompleted) Color.White else Color.Yellow,
                    modifier = Modifier
                        .size(50.dp)
                        .scale(scale)
                )

                Text(
                    text = if (sosState.isCompleted) "SOS INVIATO" else "EMERGENZA SOS",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                // STATE 1: Initial 5-Second Cancellation Countdown
                if (sosState.countdownSeconds > 0) {
                    Box(
                        modifier = Modifier
                            .size(105.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(6.dp, Color(0xFFDC2626), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${sosState.countdownSeconds}",
                            color = Color(0xFFDC2626),
                            fontSize = 60.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Text(
                        text = "Hai 5 secondi per annullare prima che partano i soccorsi automatici.",
                        color = Color(0xFFFEF08A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                // STATE 2: 10-Second Voice Recording in Foreground Service
                if (sosState.isRecordingVoice) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(95.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(5.dp, Color(0xFFEF4444), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Microfono attivo",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(34.dp)
                                )
                                Text(
                                    text = "${sosState.voiceCountdownSeconds}s",
                                    color = Color(0xFFDC2626),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        Text(
                            text = "🔴 Registrazione Vocale (10s)",
                            color = Color(0xFFFEF08A),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Parla adesso: spiega dove ti trovi e cosa succede. L'audio e il testo verranno inviati su WhatsApp.",
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        // Live Transcription Box
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Trascrizione in tempo reale:",
                                    color = Color(0xFF93C5FD),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (sosState.transcribedText.isNotBlank())
                                        "\"${sosState.transcribedText}\""
                                    else
                                        "(In ascolto del messaggio vocale...)",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // STATE 3: Sending WhatsApp / SMS
                if (sosState.isSendingWhatsApp) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(46.dp),
                            strokeWidth = 4.dp
                        )
                        Text(
                            text = "Invio file vocale e trascrizione su WhatsApp...",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Status Message Text
                Text(
                    text = sosState.statusMessage,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                if (sosState.mapsUrl.isNotEmpty()) {
                    Text(
                        text = "📍 Coordinate GPS incluse nel messaggio WhatsApp.",
                        color = Color(0xFFFEF08A),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Huge Cancel / Close Button
                Button(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("sos_cancel_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (sosState.isCompleted) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = if (sosState.isCompleted) "Chiudi" else "Annulla SOS",
                            tint = if (sosState.isCompleted) Color(0xFF15803D) else Color(0xFF991B1B),
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (sosState.isCompleted) "CHIUDI" else "ANNULLA",
                            color = if (sosState.isCompleted) Color(0xFF15803D) else Color(0xFF991B1B),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

