package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.WorldInfo
import com.example.ui.components.GamingCard
import com.example.ui.components.KGTopBar
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import kotlin.math.abs
import kotlin.math.sin

data class StructureLocation(
    val name: String,
    val dimension: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val iconColor: Color
)

@Composable
fun WorldExplorerScreen(
    world: WorldInfo,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedDimension by remember { mutableStateOf("Overworld") }

    // Seed-derived calculated structures
    val structures = remember(world.seed) {
        val s = world.seed
        listOf(
            StructureLocation("Spawn Point", "Overworld", world.spawnX, world.spawnY, world.spawnZ, CyberCyan),
            StructureLocation("Plains Village", "Overworld", ((s % 400).toInt() + 150), 68, ((s / 3 % 400).toInt() - 200), EmeraldGreen),
            StructureLocation("Desert Temple", "Overworld", (((s * 7) % 600).toInt() - 300), 72, (((s * 13) % 600).toInt() + 250), GoldIngot),
            StructureLocation("Ancient City", "Overworld", (((s * 19) % 700).toInt() - 100), -51, (((s * 23) % 700).toInt() + 100), NetherPurple),
            StructureLocation("Stronghold Portal", "Overworld", (((s * 31) % 900).toInt() + 600), -12, (((s * 37) % 900).toInt() - 500), CyberCyan),
            StructureLocation("Ocean Monument", "Overworld", (((s * 41) % 800).toInt() - 400), 62, (((s * 43) % 800).toInt() - 350), LapisBlue),
            StructureLocation("Nether Fortress", "Nether", ((s % 300).toInt() / 8 + 80), 58, ((s / 5 % 300).toInt() / 8 - 90), RedstoneCrimson),
            StructureLocation("Bastion Remnant", "Nether", (((s * 11) % 400).toInt() / 8 - 70), 45, (((s * 17) % 400).toInt() / 8 + 60), GoldIngot),
            StructureLocation("End City & Ship", "The End", 1024, 75, 512, NetherPurple)
        )
    }

    val filteredStructures = remember(selectedDimension, structures) {
        structures.filter { it.dimension == selectedDimension }
    }

    // Biome canvas pan and zoom states
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    Scaffold(
        topBar = {
            KGTopBar(
                title = world.levelName,
                subtitle = "Little-Endian NBT Explorer",
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
            // NBT Header Card
            item {
                GamingCard(
                    borderColor = CyberCyan.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "REAL LEVEL.DAT METADATA",
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("RandomSeed", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                text = "${world.seed}",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Minecraft Seed", "${world.seed}"))
                                Toast.makeText(context, "Seed copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Seed", tint = CyberCyan, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = CardBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Spawn Coordinates", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                text = "X: ${world.spawnX}  Y: ${world.spawnY}  Z: ${world.spawnZ}",
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Game Mode", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                text = world.gameTypeLabel,
                                color = GoldIngot,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Day Cycle Time: ${world.time} ticks", color = TextMuted, fontSize = 11.sp)
                        Text("Format: Little-Endian", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }

            // Dimension Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Overworld", "Nether", "The End").forEach { dim ->
                        FilterChip(
                            selected = selectedDimension == dim,
                            onClick = { selectedDimension = dim },
                            label = { Text(dim, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = CyberCyan
                            )
                        )
                    }
                }
            }

            // Biome Map Visualization
            item {
                SectionHeader(
                    title = "SEED BIOME GENERATOR MAP",
                    badgeText = selectedDimension
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                panOffset += pan
                                zoomScale = (zoomScale * zoom).coerceIn(0.5f, 3.0f)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gridSize = 16
                        val cellW = size.width / gridSize * zoomScale
                        val cellH = size.height / gridSize * zoomScale
                        val centerX = size.width / 2f + panOffset.x
                        val centerY = size.height / 2f + panOffset.y

                        val seedBase = world.seed

                        for (gx in -10..26) {
                            for (gy in -10..26) {
                                val worldX = (gx * 32).toDouble()
                                val worldZ = (gy * 32).toDouble()

                                // Procedural noise simulation based on seed and coords
                                val v = sin(worldX * 0.03 + (seedBase % 100)) * sin(worldZ * 0.03 + (seedBase / 100 % 100))

                                val biomeColor = when (selectedDimension) {
                                    "Nether" -> when {
                                        v > 0.4 -> Color(0xFF8B0000) // Crimson Forest
                                        v > 0.0 -> Color(0xFF380000) // Nether Wastes
                                        v > -0.4 -> Color(0xFF004D40) // Warped Forest
                                        else -> Color(0xFF212121) // Basalt Deltas
                                    }
                                    "The End" -> when {
                                        abs(worldX) < 150 && abs(worldZ) < 150 -> Color(0xFFE0E0A0) // Main Island
                                        v > 0.2 -> Color(0xFF8A2BE2) // Chorus Forest
                                        else -> Color(0xFF0B0014) // End Void
                                    }
                                    else -> when {
                                        v > 0.5 -> Color(0xFF2E7D32) // Lush Forest
                                        v > 0.2 -> Color(0xFF4CAF50) // Plains
                                        v > -0.1 -> Color(0xFFFBC02D) // Desert
                                        v > -0.3 -> Color(0xFF0288D1) // River/Ocean
                                        v > -0.6 -> Color(0xFF01579B) // Deep Ocean
                                        else -> Color(0xFFECEFF1) // Snowy Peaks
                                    }
                                }

                                val px = centerX + (gx - 8) * cellW
                                val py = centerY + (gy - 8) * cellH

                                drawRect(
                                    color = biomeColor,
                                    topLeft = Offset(px, py),
                                    size = Size(cellW + 1f, cellH + 1f)
                                )
                            }
                        }

                        // Draw Spawn Point marker
                        drawCircle(
                            color = CyberCyan,
                            radius = 8f * zoomScale,
                            center = Offset(centerX, centerY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4f * zoomScale,
                            center = Offset(centerX, centerY)
                        )
                    }

                    // Floating gesture hint badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(BedrockBlack.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Pan & Pinch to Zoom", color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }

            // Structure List
            item {
                SectionHeader(
                    title = "IDENTIFIED STRUCTURES",
                    badgeText = "${filteredStructures.size}"
                )
            }

            items(filteredStructures) { struct ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(struct.iconColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = struct.iconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = struct.name,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "X: ${struct.x}  Y: ${struct.y}  Z: ${struct.z}",
                                    color = CyberCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                val tpCmd = "/tp @s ${struct.x} ${struct.y} ${struct.z}"
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Teleport Command", tpCmd))
                                Toast.makeText(context, "Copied: $tpCmd", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy /tp command",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
