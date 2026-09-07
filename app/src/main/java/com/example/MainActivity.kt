package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var lastRecognizedText by remember { mutableStateOf("") }
                var errorText by remember { mutableStateOf("") }

                var timeText by remember { mutableStateOf("") }
                var dateText by remember { mutableStateOf("") }

                LaunchedEffect(Unit) {
                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ITALIAN)
                    while (true) {
                        val now = LocalDateTime.now()
                        timeText = now.format(timeFormatter)
                        dateText = now.format(dateFormatter)
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ITALIAN) else it.toString() }
                        delay(1000)
                    }
                }

                Scaffold(
                    containerColor = Color(0xFF141416),
                    bottomBar = { BottomNavBar() }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Time & Date Header
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(
                                text = timeText,
                                fontSize = 80.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = dateText,
                                fontSize = 22.sp,
                                color = Color.LightGray
                            )
                        }

                        // Central Voice Button
                        VoiceInputComponent(
                            onResult = { text ->
                                lastRecognizedText = text
                                errorText = ""
                            },
                            onError = { error ->
                                errorText = error
                            }
                        )
                        
                        // Status Text
                        if (lastRecognizedText.isNotEmpty()) {
                            Text(
                                text = "\"$lastRecognizedText\"",
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        } else if (errorText.isNotEmpty()) {
                            Text(
                                text = errorText,
                                color = Color.Red,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Accessible Grid
                        Column(
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    AccessibleButton(
                                        icon = Icons.Default.Phone,
                                        text = null,
                                        backgroundColor = Color(0xFF1976D2),
                                        onClick = {}
                                    )
                                }
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    AccessibleButton(
                                        icon = null,
                                        text = "SOS",
                                        backgroundColor = Color(0xFFD32F2F),
                                        onClick = {}
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    AccessibleButton(
                                        icon = Icons.Default.Chat,
                                        text = null,
                                        backgroundColor = Color(0xFF4CAF50),
                                        onClick = {}
                                    )
                                }
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    AccessibleButton(
                                        icon = Icons.Default.Person,
                                        text = null,
                                        backgroundColor = Color(0xFF1976D2),
                                        onClick = {}
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccessibleButton(
    icon: ImageVector?,
    text: String?,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(150.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(8.dp, Color.White, CircleShape)
            .clickable(onClick = onClick)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Color.White,
                modifier = Modifier.size(80.dp)
            )
        } else if (text != null) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BottomNavBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF222226))
            .padding(vertical = 20.dp, horizontal = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = "Home",
            tint = Color.White,
            modifier = Modifier.size(44.dp)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Undo,
            contentDescription = "Indietro",
            tint = Color.White,
            modifier = Modifier.size(44.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { /* Handle manual voice trigger */ }
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice",
                tint = Color(0xFF4285F4),
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "VOICE",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
