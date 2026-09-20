package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.WorldInfo
import com.example.ui.components.GamingCard
import com.example.ui.components.KGTopBar
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun CaveViewerScreen(
    world: WorldInfo,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedY by remember { mutableFloatStateOf(-58f) } // Default to diamond level
    var is3DMode by remember { mutableStateOf(true) }
    var rotationAngle by remember { mutableFloatStateOf(45f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    val layerDescription = remember(selectedY) {
        when {
            selectedY <= -60 -> "Bedrock Boundary (Y: -64 to -60)"
            selectedY in -59f..-50f -> "Deepslate Diamond Zone (High Ore Density)"
            selectedY in -49f..-10f -> "Deepslate Caverns / Ancient Deep Dark"
            selectedY in -9f..0f -> "Deepslate Transition Layer"
            selectedY in 1f..30f -> "Lush Caves & Dripstone Aquifers"
            selectedY in 31f..62f -> "Upper Stone Caverns & Iron Deposits"
            selectedY in 63f..128f -> "Surface / Mountain Caves & Coal Veins"
            else -> "High Alpine Peaks"
        }
    }

    Scaffold(
        topBar = {
            KGTopBar(
                title = "Cave & Layer Viewer",
                subtitle = "${world.levelName} • Y: ${selectedY.toInt()}",
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = { is3DMode = !is3DMode }) {
                        Icon(
                            imageVector = if (is3DMode) Icons.Default.ViewInAr else Icons.Default.Layers,
                            contentDescription = "Toggle 3D/2D",
                            tint = CyberCyan
                        )
                    }
                }
            )
        },
        containerColor = BedrockBlack,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Interactive Canvas Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            panOffset += pan
                            zoomScale = (zoomScale * zoom).coerceIn(0.5f, 3.5f)
                            if (is3DMode) {
                                rotationAngle = (rotationAngle + rotation).mod(360f)
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2f + panOffset.x
                    val centerY = size.height / 2f + panOffset.y
                    val seed = world.seed

                    if (is3DMode) {
                        // 3D Isometric Cave Slices
                        val angleRad = Math.toRadians(rotationAngle.toDouble())
                        val cosA = cos(angleRad).toFloat()
                        val sinA = sin(angleRad).toFloat()

                        val layersToDraw = listOf(selectedY + 16f, selectedY, selectedY - 16f)

                        for (layerY in layersToDraw) {
                            val alpha = if (layerY == selectedY) 1.0f else 0.35f
                            val yOffset = (layerY - selectedY) * 2.5f * zoomScale

                            val gridSize = 8
                            val spacing = 26f * zoomScale

                            for (ix in -gridSize..gridSize) {
                                for (iz in -gridSize..gridSize) {
                                    val worldX = (ix * 16).toDouble()
                                    val worldZ = (iz * 16).toDouble()

                                    // 3D noise simulation
                                    val density = sin(worldX * 0.05 + layerY * 0.08 + (seed % 99)) *
                                            cos(worldZ * 0.05 + (seed / 99 % 99))

                                    // Only draw non-air structures / cave walls
                                    if (density > -0.25) {
                                        val isoX = centerX + (ix * cosA - iz * sinA) * spacing
                                        val isoY = centerY + (ix * sinA + iz * cosA) * spacing * 0.5f - yOffset

                                        val blockColor = when {
                                            density > 0.6 && layerY < -50 -> CyberCyan // Diamond Ore
                                            density > 0.5 -> RedstoneCrimson // Redstone
                                            density > 0.35 -> GoldIngot // Gold / Iron
                                            density < 0.0 && layerY < 0 -> Color(0xFF003830) // Deep dark sculk
                                            layerY < 0 -> Color(0xFF1E293B) // Deepslate
                                            else -> Color(0xFF475569) // Stone
                                        }.copy(alpha = alpha)

                                        val blockPath = Path().apply {
                                            val s = 10f * zoomScale
                                            moveTo(isoX, isoY - s)
                                            lineTo(isoX + s, isoY - s * 0.5f)
                                            lineTo(isoX, isoY)
                                            lineTo(isoX - s, isoY - s * 0.5f)
                                            close()
                                        }
                                        drawPath(path = blockPath, color = blockColor)
                                    }
                                }
                            }
                        }
                    } else {
                        // 2D Horizontal Slice View
                        val gridSize = 16
                        val cellW = size.width / gridSize * zoomScale
                        val cellH = size.height / gridSize * zoomScale

                        for (ix in -gridSize / 2..gridSize / 2) {
                            for (iz in -gridSize / 2..gridSize / 2) {
                                val worldX = (ix * 16).toDouble()
                                val worldZ = (iz * 16).toDouble()
                                val density = sin(worldX * 0.05 + selectedY * 0.08 + (seed % 99)) *
                                        cos(worldZ * 0.05 + (seed / 99 % 99))

                                val px = centerX + ix * cellW
                                val py = centerY + iz * cellH

                                val cellColor = when {
                                    density < -0.35 -> Color(0xFF070A10) // Air / Cavern Void
                                    density > 0.6 && selectedY < -50 -> CyberCyan // Diamond
                                    density > 0.45 -> EmeraldGreen // Lush Moss
                                    density > 0.3 -> GoldIngot // Ore
                                    selectedY < 0 -> Color(0xFF1E293B) // Deepslate
                                    else -> Color(0xFF475569) // Stone
                                }

                                drawRect(
                                    color = cellColor,
                                    topLeft = Offset(px, py),
                                    size = Size(cellW - 1f, cellH - 1f)
                                )
                            }
                        }
                    }
                }

                // HUD Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BedrockBlack.copy(alpha = 0.8f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            text = if (is3DMode) "3D Isometric Cavern Projection" else "2D Horizontal Layer Slice",
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Coords: X: ~0  Y: ${selectedY.toInt()}  Z: ~0",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Legend
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BedrockBlack.copy(alpha = 0.85f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(CyberCyan))
                            Text("Diamond", color = TextSecondary, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(GoldIngot))
                            Text("Ore", color = TextSecondary, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF1E293B)))
                            Text("Deepslate", color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Controls Card: Y-Level Slider & Rotation
            GamingCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Layer Altitude (Y)",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Y = ${selectedY.toInt()}",
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Slider(
                    value = selectedY,
                    onValueChange = { selectedY = it },
                    valueRange = -64f..320f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan,
                        inactiveTrackColor = CardBorder
                    )
                )

                Text(
                    text = layerDescription,
                    color = EmeraldGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (is3DMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("3D Rotation", color = TextSecondary, fontSize = 12.sp)
                        Text("${rotationAngle.toInt()}°", color = TextPrimary, fontSize = 12.sp)
                    }
                    Slider(
                        value = rotationAngle,
                        onValueChange = { rotationAngle = it },
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(
                            thumbColor = LapisBlue,
                            activeTrackColor = LapisBlue,
                            inactiveTrackColor = CardBorder
                        )
                    )
                }
            }
        }
    }
}
