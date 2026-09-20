package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.core.content.ContextCompat
import com.example.data.speech.KGSpeechManager
import com.example.data.speech.SpeechState
import com.example.ui.components.GamingCard
import com.example.ui.components.GlowButton
import com.example.ui.components.KGTopBar
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*

@Composable
fun VoiceMicScreen(
    speechManager: KGSpeechManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val speechState by speechManager.speechState.collectAsState()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            speechManager.startListening()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice commands", Toast.LENGTH_SHORT).show()
        }
    }

    val commandShortcuts = listOf(
        "/say" to "Broadcast",
        "/tp @s" to "Teleport",
        "/give @s diamond" to "Give Item",
        "/locate structure" to "Locate",
        "/time set day" to "Day Time",
        "/weather clear" to "Clear Weather"
    )

    Scaffold(
        topBar = {
            KGTopBar(
                title = "Voice to Bedrock Command",
                subtitle = "Zero Secret Recording • Android Recognizer",
                onBackClick = onBack
            )
        },
        containerColor = BedrockBlack,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Speech Status Hero Card
            GamingCard(
                borderColor = if (speechState.isListening) RedstoneCrimson else CyberCyan.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (speechState.isListening) RedstoneCrimson else EmeraldGreen)
                            )
                            Text(
                                text = if (speechState.isListening) "LISTENING..." else "MIC STANDBY",
                                color = if (speechState.isListening) RedstoneCrimson else EmeraldGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (speechState.isListening) "Speak your command..." else "Tap Mic to Speak",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    // Explicit Mic Button
                    IconButton(
                        onClick = {
                            if (speechState.isListening) {
                                speechManager.stopListening()
                            } else {
                                if (hasMicPermission) {
                                    speechManager.startListening()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (speechState.isListening) RedstoneCrimson else CyberCyan)
                            .testTag("voice_mic_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (speechState.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Microphone",
                            tint = if (speechState.isListening) Color.White else BedrockBlack,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                if (speechState.partialText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Listening: ${speechState.partialText}",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }

                speechState.error?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = err, color = RedstoneCrimson, fontSize = 12.sp)
                }
            }

            // Recognized Command Output Card
            GamingCard(
                borderColor = if (speechState.recognizedText.isNotBlank()) EmeraldGreen.copy(alpha = 0.5f) else CardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "MINECRAFT COMMAND BUFFER",
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    if (speechState.recognizedText.isNotBlank()) {
                        IconButton(
                            onClick = { speechManager.clearText() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    if (speechState.recognizedText.isNotBlank()) {
                        Text(
                            text = speechState.recognizedText,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    } else {
                        Text(
                            text = "Say e.g. 'diamond sword' or 'set time 1000' to compose commands.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlowButton(
                        text = "COPY COMMAND",
                        icon = Icons.Default.ContentCopy,
                        onClick = {
                            val txt = speechState.recognizedText.trim()
                            if (txt.isNotBlank()) {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Bedrock Command", txt))
                                Toast.makeText(context, "Command copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = speechState.recognizedText.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        containerColor = EmeraldGreen,
                        contentColor = BedrockBlack,
                        testTag = "copy_voice_command_button"
                    )

                    Button(
                        onClick = { speechManager.clearText() },
                        enabled = speechState.recognizedText.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardSurfaceVariant)
                    ) {
                        Text("Clear", color = TextPrimary)
                    }
                }
            }

            // Command Prefix Helpers
            SectionHeader(title = "ADD COMMAND PREFIX")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(commandShortcuts) { (cmd, label) ->
                    AssistChip(
                        onClick = { speechManager.appendCommand(cmd) },
                        label = { Text(cmd, fontWeight = FontWeight.Bold, color = CyberCyan) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = CardSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
