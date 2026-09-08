package com.example.ui

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.provider.MediaStore
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.accessibility.NotificationRepository
import com.example.accessibility.SosManager
import com.example.accessibility.TorchController
import com.example.accessibility.TtsManager
import com.example.model.ContactItem
import com.example.ui.components.NotificationsDialog
import com.example.ui.components.PhoneContactsDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SosCountdownDialog
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainLauncherScreen(
    torchController: TorchController,
    sosManager: SosManager,
    ttsManager: TtsManager,
    onStartVoiceRecognition: () -> Unit
) {
    val context = LocalContext.current

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.ITALIAN) }
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale.ITALIAN) }

    // Live Clock & Date State
    var currentTime by remember { mutableStateOf(timeFormat.format(Date())) }
    var currentDate by remember {
        mutableStateOf(dateFormat.format(Date()).replaceFirstChar { it.uppercase() })
    }

    // Battery State
    var batteryPct by remember { mutableStateOf(85) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now).replaceFirstChar { it.uppercase() }

            try {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.let {
                    if (it in 1..100) batteryPct = it
                }
            } catch (e: Exception) {
                // Ignore battery lookup error on emulator
            }
            delay(5000)
        }
    }

    // Notification Counts
    val missedCalls by NotificationRepository.missedCallsCount.collectAsState()
    val unreadWhatsApp by NotificationRepository.unreadWhatsAppCount.collectAsState()
    val isTorchOn by torchController.isTorchOn.collectAsState()
    val sosState by sosManager.sosState.collectAsState()
    val emergencyContacts by sosManager.emergencyContacts.collectAsState()

    // Dialog Visibilities
    var showSosDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showPhoneDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Sync active SOS state with dialog
    LaunchedEffect(sosState.isActive) {
        if (sosState.isActive) {
            showSosDialog = true
        }
    }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("main_launcher_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // -------------------------------------------------------------
            // TOP BAR: Battery (Left), Huge Clock & Date (Center), Signal (Right)
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Left: Battery Indicator with Shadow
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryFull,
                            contentDescription = "Livello batteria $batteryPct%",
                            tint = if (batteryPct > 20) Color(0xFF4ADE80) else Color(0xFFEF4444),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$batteryPct%",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Center: Huge Clock with High Contrast Shadow
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentTime,
                            color = Color.White,
                            style = TextStyle(
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp,
                                textAlign = TextAlign.Center,
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.9f),
                                    offset = Offset(3f, 4f),
                                    blurRadius = 8f
                                )
                            ),
                            modifier = Modifier.testTag("clock_display")
                        )
                    }

                    // Right: Signal Bars
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SignalCellularAlt,
                            contentDescription = "Segnale cellulare",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Date Subtitle
                Text(
                    text = currentDate,
                    color = Color(0xFFF1F5F9),
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.8f),
                            offset = Offset(1f, 2f),
                            blurRadius = 4f
                        )
                    ),
                    modifier = Modifier.offset(y = (-4).dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // -------------------------------------------------------------
                // NOTIFICATION SUMMARY PILL (Deep Shadow, Glowing border)
                // -------------------------------------------------------------
                val notificationSummaryText = when {
                    missedCalls > 0 && unreadWhatsApp > 0 -> "$missedCalls chiamate perse, $unreadWhatsApp messaggi"
                    missedCalls > 0 -> "$missedCalls chiamate perse"
                    unreadWhatsApp > 0 -> "$unreadWhatsApp messaggi WhatsApp"
                    else -> "Nessun nuovo messaggio o chiamata"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .shadow(8.dp, RoundedCornerShape(50), ambientColor = Color.Black)
                        .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(50))
                        .clickable {
                            showNotificationsDialog = true
                        }
                        .testTag("notification_banner"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (missedCalls > 0 || unreadWhatsApp > 0) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = if (missedCalls > 0 || unreadWhatsApp > 0) Color(0xFFFACC15) else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = notificationSummaryText,
                            color = Color.White,
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                shadow = Shadow(
                                    color = Color.Black,
                                    offset = Offset(1f, 2f),
                                    blurRadius = 4f
                                )
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // -------------------------------------------------------------
            // 6 BIG TACTILE ACTION CARDS (2 Columns x 3 Rows)
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ROW 1: Telefono & Messaggi/WhatsApp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TactileLauncherCard(
                        title = "Telefono",
                        icon = Icons.Default.Call,
                        containerColor = CardPhoneGreen,
                        pressedColor = CardPhoneGreenPressed,
                        badgeCount = missedCalls,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        testTag = "card_phone",
                        onClick = {
                            showPhoneDialog = true
                        }
                    )

                    TactileLauncherCard(
                        title = "Messaggi\nWhatsApp",
                        icon = Icons.Default.Chat,
                        containerColor = CardWhatsAppBlue,
                        pressedColor = CardWhatsAppBluePressed,
                        badgeCount = unreadWhatsApp,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        testTag = "card_whatsapp",
                        onClick = {
                            val pm = context.packageManager
                            val launchIntent = pm.getLaunchIntentForPackage("com.whatsapp")
                            if (launchIntent != null) {
                                try {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(launchIntent)
                                } catch (e: Exception) {
                                    showNotificationsDialog = true
                                }
                            } else {
                                showNotificationsDialog = true
                            }
                        }
                    )
                }

                // ROW 2: Torcia/Luce & Fotocamera/Foto
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TactileLauncherCard(
                        title = if (isTorchOn) "Torcia\nACCESA" else "Torcia\nLuce",
                        icon = if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        containerColor = if (isTorchOn) Color(0xFFF59E0B) else CardTorchOrange,
                        pressedColor = CardTorchOrangePressed,
                        isGlowing = isTorchOn,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        testTag = "card_torch",
                        onClick = {
                            torchController.toggleTorch()
                        }
                    )

                    TactileLauncherCard(
                        title = "Fotocamera\nFoto",
                        icon = Icons.Default.PhotoCamera,
                        containerColor = CardCameraTeal,
                        pressedColor = CardCameraTealPressed,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        testTag = "card_camera",
                        onClick = {
                            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val altIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(altIntent)
                                } catch (e2: Exception) {
                                    // ignore fallback
                                }
                            }
                        }
                    )
                }

                // ROW 3: Impostazioni & SOS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TactileLauncherCard(
                        title = "Impostazioni",
                        icon = Icons.Default.Settings,
                        containerColor = CardSettingsGold,
                        pressedColor = CardSettingsGoldPressed,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        testTag = "card_settings",
                        onClick = {
                            showSettingsDialog = true
                        }
                    )

                    TactileLauncherCard(
                        title = "SOS\nSoccorsi",
                        icon = Icons.Default.MedicalServices,
                        containerColor = CardSosRed,
                        pressedColor = CardSosRedPressed,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        testTag = "card_sos",
                        onClick = {
                            showSosDialog = true
                            sosManager.startSosSequence(coroutineScope)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // -------------------------------------------------------------
            // BOTTOM VOICE ASSISTANT PILL (Glowing Cyan Border & Mic Button)
            // -------------------------------------------------------------
            val infiniteTransition = rememberInfiniteTransition(label = "voice_glow")
            val glowBorderAlpha by infiniteTransition.animateFloat(
                initialValue = 0.7f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1100),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glowAlpha"
            )

            Card(
                onClick = onStartVoiceRecognition,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .shadow(12.dp, RoundedCornerShape(38.dp), spotColor = NeonCyanGlow)
                    .border(
                        3.dp,
                        NeonCyanGlow.copy(alpha = glowBorderAlpha),
                        RoundedCornerShape(38.dp)
                    )
                    .testTag("voice_assistant_button"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF090E1A)),
                shape = RoundedCornerShape(38.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    // Cyan Circle Button with Microphone and Deep Shadow
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(6.dp, CircleShape, spotColor = NeonCyanGlow)
                            .clip(CircleShape)
                            .background(NeonCyanGlow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Parla all'assistente vocale",
                            tint = Color.Black,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Assistente Vocale",
                            color = NeonCyanGlow,
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                shadow = Shadow(color = Color.Black, offset = Offset(1f, 1f), blurRadius = 2f)
                            )
                        )
                        Text(
                            text = "Tocca e parla...",
                            color = Color.White,
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                shadow = Shadow(color = Color.Black, offset = Offset(2f, 2f), blurRadius = 4f)
                            )
                        )
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // MODAL DIALOGS
    // -------------------------------------------------------------
    if (showSosDialog) {
        SosCountdownDialog(
            sosState = sosState,
            onCancel = {
                sosManager.cancelSos()
                showSosDialog = false
            }
        )
    }

    if (showNotificationsDialog) {
        NotificationsDialog(
            ttsManager = ttsManager,
            onStartReplyVoice = { recipient, key ->
                showNotificationsDialog = false
                onStartVoiceRecognition()
            },
            onDismiss = { showNotificationsDialog = false }
        )
    }

    if (showPhoneDialog) {
        PhoneContactsDialog(
            contacts = emergencyContacts,
            onDismiss = { showPhoneDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            sosManager = sosManager,
            torchController = torchController,
            ttsManager = ttsManager,
            onTestVoiceAssistant = {
                showSettingsDialog = false
                onStartVoiceRecognition()
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

/**
 * Big tactile accessible launcher card with extra large iconography, bold high-contrast shadow typography,
 * deep card elevation shadow, and glossy highlight.
 */
@Composable
fun TactileLauncherCard(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    pressedColor: Color,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    isGlowing: Boolean = false,
    testTag: String = "",
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        label = "pressScale"
    )

    val cardColor = if (isPressed) pressedColor else containerColor

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (isGlowing) 20.dp else 14.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = if (isGlowing) Color.Yellow else containerColor.copy(alpha = 0.95f),
                ambientColor = Color.Black
            )
            .border(
                width = if (isGlowing) 3.5.dp else 2.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        Color.White.copy(alpha = 0.40f)
                    )
                ),
                shape = RoundedCornerShape(26.dp)
            )
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(26.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.18f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Extra Large Icon with Deep Drop Shadow Badge
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .shadow(10.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.85f))
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.30f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Extra Large Bold Label with High Contrast Shadow
                Text(
                    text = title,
                    color = Color.White,
                    style = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(2.5f, 3.5f),
                            blurRadius = 8f
                        )
                    )
                )
            }

            // Notification Badge if count > 0
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(36.dp)
                        .shadow(6.dp, CircleShape, spotColor = Color.Black)
                        .clip(CircleShape)
                        .background(BadgeRed)
                        .border(2.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$badgeCount",
                        color = Color.White,
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            shadow = Shadow(color = Color.Black, offset = Offset(1f, 1f), blurRadius = 2f)
                        )
                    )
                }
            }
        }
    }
}

