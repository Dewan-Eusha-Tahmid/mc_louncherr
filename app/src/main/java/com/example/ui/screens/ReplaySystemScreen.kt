package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CameraKeyframe
import com.example.data.models.ReplayProject
import com.example.ui.components.GamingCard
import com.example.ui.components.GlowButton
import com.example.ui.components.KGTopBar
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ReplaySystemScreen(
    currentReplay: ReplayProject,
    onSaveReplay: (ReplayProject) -> Unit,
    onExportReplay: (ReplayProject) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var replay by remember(currentReplay) { mutableStateOf(currentReplay) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentTick by remember { mutableLongStateOf(0L) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Playback loop
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(50) // 20 ticks per second = 50ms per tick
            val maxT = replay.keyframes.maxOfOrNull { it.tick } ?: 100L
            currentTick = if (currentTick >= maxT) {
                if (replay.isLoop) 0L else {
                    isPlaying = false
                    maxT
                }
            } else {
                currentTick + 1
            }
        }
    }

    Scaffold(
        topBar = {
            KGTopBar(
                title = replay.name,
                subtitle = "Companion Camera Trajectory & /camera Exporter",
                onBackClick = onBack
            )
        },
        containerColor = BedrockBlack,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Interactive 2D/3D Camera Trajectory Path Viewport
            item {
                GamingCard(borderColor = NetherPurple.copy(alpha = 0.5f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CAMERA TRAJECTORY PATH",
                            color = NetherPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Tick: $currentTick",
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianSurface)
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val scale = 1.2f

                            // Draw origin axis
                            drawLine(CardBorder, Offset(cx, 0f), Offset(cx, size.height), 1f)
                            drawLine(CardBorder, Offset(0f, cy), Offset(size.width, cy), 1f)

                            // Plot keyframes (X, Z plane)
                            val pts = replay.keyframes.map { kf ->
                                Offset(cx + kf.x * scale, cy + kf.z * scale)
                            }

                            for (i in 0 until pts.size - 1) {
                                drawLine(CyberCyan, pts[i], pts[i + 1], 3f)
                            }

                            pts.forEachIndexed { idx, pt ->
                                drawCircle(NetherPurple, radius = 6f, center = pt)
                                drawCircle(Color.White, radius = 3f, center = pt)
                            }

                            // Current camera animated position
                            if (pts.isNotEmpty()) {
                                val animIndex = ((currentTick.toFloat() / (replay.keyframes.maxOfOrNull { it.tick } ?: 100L).coerceAtLeast(1L)) * (pts.size - 1)).coerceIn(0f, (pts.size - 1).toFloat()).toInt()
                                val currentPt = pts[animIndex]
                                drawCircle(GoldIngot, radius = 8f, center = currentPt)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Timeline Scrubber & Controls
                    val maxTick = (replay.keyframes.maxOfOrNull { it.tick } ?: 100L).coerceAtLeast(1L)
                    Slider(
                        value = currentTick.toFloat(),
                        onValueChange = { currentTick = it.toLong() },
                        valueRange = 0f..maxTick.toFloat(),
                        colors = SliderDefaults.colors(thumbColor = NetherPurple, activeTrackColor = NetherPurple)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { isPlaying = !isPlaying }) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = CyberCyan
                                )
                            }
                            IconButton(onClick = { currentTick = 0L }) {
                                Icon(Icons.Default.Replay, contentDescription = "Restart", tint = TextSecondary)
                            }
                            FilterChip(
                                selected = replay.isLoop,
                                onClick = { replay = replay.copy(isLoop = !replay.isLoop) },
                                label = { Text("Loop", fontSize = 11.sp) }
                            )
                        }

                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NetherPurple),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Point", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Export Bedrock Commands Card
            item {
                GamingCard(borderColor = EmeraldGreen.copy(alpha = 0.5f)) {
                    Text(
                        text = "BEDROCK /camera COMMAND SEQUENCE",
                        color = EmeraldGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Real zero-client-mod commands that drive the Minecraft Bedrock camera in game:",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val commands = replay.generateBedrockCommands()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianSurface)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = commands.take(4).joinToString("\n") + if (commands.size > 4) "\n... (${commands.size - 4} more commands)" else "",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    GlowButton(
                        text = "COPY COMMAND SEQUENCE",
                        icon = Icons.Default.ContentCopy,
                        onClick = {
                            val allCmds = commands.joinToString("\n")
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Bedrock Camera Commands", allCmds))
                            Toast.makeText(context, "Copied ${commands.size} commands to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = EmeraldGreen,
                        contentColor = BedrockBlack,
                        testTag = "copy_camera_commands_button"
                    )
                }
            }

            // Keyframes List
            item {
                SectionHeader(title = "CAMERA KEYPOINTS", badgeText = "${replay.keyframes.size}")
            }

            itemsIndexed(replay.keyframes) { index, kf ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Point #$index (Tick ${kf.tick})", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Pos: (${kf.x.toInt()}, ${kf.y.toInt()}, ${kf.z.toInt()}) • Rot: (${kf.pitch.toInt()}°, ${kf.yaw.toInt()}°)", color = TextSecondary, fontSize = 11.sp)
                        }
                        IconButton(
                            onClick = {
                                if (replay.keyframes.size > 2) {
                                    val updated = replay.keyframes.toMutableList().apply { removeAt(index) }
                                    replay = replay.copy(keyframes = updated)
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedstoneCrimson, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    // Add Keyframe Dialog
    if (showAddDialog) {
        var xVal by remember { mutableStateOf("0") }
        var yVal by remember { mutableStateOf("70") }
        var zVal by remember { mutableStateOf("0") }
        var pitchVal by remember { mutableStateOf("0") }
        var yawVal by remember { mutableStateOf("0") }
        val nextTick = (replay.keyframes.maxOfOrNull { it.tick } ?: 0L) + 20L

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Camera Keyframe", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tick: $nextTick", color = CyberCyan, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = xVal, onValueChange = { xVal = it }, label = { Text("X") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = yVal, onValueChange = { yVal = it }, label = { Text("Y") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = zVal, onValueChange = { zVal = it }, label = { Text("Z") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = pitchVal, onValueChange = { pitchVal = it }, label = { Text("Pitch") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = yawVal, onValueChange = { yawVal = it }, label = { Text("Yaw") }, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val kf = CameraKeyframe(
                            tick = nextTick,
                            x = xVal.toFloatOrNull() ?: 0f,
                            y = yVal.toFloatOrNull() ?: 70f,
                            z = zVal.toFloatOrNull() ?: 0f,
                            pitch = pitchVal.toFloatOrNull() ?: 0f,
                            yaw = yawVal.toFloatOrNull() ?: 0f
                        )
                        replay = replay.copy(keyframes = replay.keyframes + kf)
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NetherPurple)
                ) {
                    Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
