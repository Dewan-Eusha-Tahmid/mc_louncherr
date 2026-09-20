package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.SkinModel
import com.example.ui.components.GamingCard
import com.example.ui.components.GlowButton
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SkinManagerScreen(
    skins: List<SkinModel>,
    onImportSkin: (isAlex: Boolean) -> Unit,
    onDeleteSkin: (SkinModel) -> Unit,
    onRenameSkin: (SkinModel, String) -> Unit,
    onEquipSkin: (SkinModel) -> Unit,
    onLaunchMinecraft: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSkin by remember { mutableStateOf<SkinModel?>(skins.firstOrNull()) }
    var isAlexMode by remember { mutableStateOf(false) }
    var showEquipInstructionsDialog by remember { mutableStateOf(false) }
    var equippedFile by remember { mutableStateOf<File?>(null) }
    var skinToRename by remember { mutableStateOf<SkinModel?>(null) }
    var renameInput by remember { mutableStateOf("") }

    // 3D rotation state
    var rotY by remember { mutableFloatStateOf(25f) }
    var rotX by remember { mutableFloatStateOf(10f) }

    // Update selection if empty
    LaunchedEffect(skins) {
        if (selectedSkin == null && skins.isNotEmpty()) {
            selectedSkin = skins.first()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 3D Preview Box
        item {
            GamingCard(
                borderColor = CyberCyan.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "3D SKIN VIEWER",
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = selectedSkin?.name ?: "No Skin Selected",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = !isAlexMode,
                            onClick = { isAlexMode = false },
                            label = { Text("Steve (4px)") }
                        )
                        FilterChip(
                            selected = isAlexMode,
                            onClick = { isAlexMode = true },
                            label = { Text("Alex (3px)") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3D Model interactive canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                rotY = (rotY + dragAmount.x * 0.6f).mod(360f)
                                rotX = (rotX - dragAmount.y * 0.4f).coerceIn(-45f, 45f)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f - 20f

                        val radY = Math.toRadians(rotY.toDouble())
                        val radX = Math.toRadians(rotX.toDouble())
                        val cosY = cos(radY).toFloat()
                        val sinY = sin(radY).toFloat()
                        val cosX = cos(radX).toFloat()
                        val sinX = sin(radX).toFloat()

                        // Color palette from skin file or fallback
                        val skinColor = Color(0xFFD49B74)
                        val hairColor = Color(0xFF4A3319)
                        val shirtColor = Color(0xFF00A2B4)
                        val pantsColor = Color(0xFF283593)
                        val shoesColor = Color(0xFF424242)

                        fun project(x: Float, y: Float, z: Float): Offset {
                            // Rotate around Y
                            val x1 = x * cosY - z * sinY
                            val z1 = x * sinY + z * cosY
                            // Rotate around X
                            val y2 = y * cosX - z1 * sinX
                            val z2 = y * sinX + z1 * cosX

                            val scale = 3.2f
                            return Offset(cx + x1 * scale, cy + y2 * scale)
                        }

                        fun drawCube(
                            x: Float, y: Float, z: Float,
                            w: Float, h: Float, d: Float,
                            topColor: Color, frontColor: Color, sideColor: Color
                        ) {
                            val halfW = w / 2f
                            val halfD = d / 2f

                            val p000 = project(x - halfW, y, z - halfD)
                            val p100 = project(x + halfW, y, z - halfD)
                            val p101 = project(x + halfW, y, z + halfD)
                            val p001 = project(x - halfW, y, z + halfD)

                            val p010 = project(x - halfW, y + h, z - halfD)
                            val p110 = project(x + halfW, y + h, z - halfD)
                            val p111 = project(x + halfW, y + h, z + halfD)
                            val p011 = project(x - halfW, y + h, z + halfD)

                            // Draw Top Face
                            val topPath = Path().apply {
                                moveTo(p000.x, p000.y)
                                lineTo(p100.x, p100.y)
                                lineTo(p110.x, p110.y)
                                lineTo(p010.x, p010.y)
                                close()
                            }
                            drawPath(topPath, topColor)

                            // Front face
                            val frontPath = Path().apply {
                                moveTo(p010.x, p010.y)
                                lineTo(p110.x, p110.y)
                                lineTo(p111.x, p111.y)
                                lineTo(p011.x, p011.y)
                                close()
                            }
                            drawPath(frontPath, frontColor)

                            // Side face
                            val sidePath = Path().apply {
                                moveTo(p100.x, p100.y)
                                lineTo(p101.x, p101.y)
                                lineTo(p111.x, p111.y)
                                lineTo(p110.x, p110.y)
                                close()
                            }
                            drawPath(sidePath, sideColor)
                        }

                        val armW = if (isAlexMode) 6f else 8f

                        // Head (8x8x8)
                        drawCube(0f, -32f, 0f, 16f, 16f, 16f, hairColor, skinColor, skinColor.copy(alpha = 0.8f))
                        // Torso (8x12x4)
                        drawCube(0f, -16f, 0f, 16f, 24f, 8f, shirtColor, shirtColor, shirtColor.copy(alpha = 0.8f))
                        // Left Arm
                        drawCube(-(8f + armW / 2f), -16f, 0f, armW, 24f, 8f, shirtColor, skinColor, skinColor.copy(alpha = 0.8f))
                        // Right Arm
                        drawCube(8f + armW / 2f, -16f, 0f, armW, 24f, 8f, shirtColor, skinColor, skinColor.copy(alpha = 0.8f))
                        // Left Leg
                        drawCube(-4f, 8f, 0f, 8f, 24f, 8f, pantsColor, pantsColor, shoesColor)
                        // Right Leg
                        drawCube(4f, 8f, 0f, 8f, 24f, 8f, pantsColor, pantsColor, shoesColor)
                    }

                    // Touch hint overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(BedrockBlack.copy(alpha = 0.7f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Drag anywhere to rotate 360°", color = TextSecondary, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                selectedSkin?.let { skin ->
                    GlowButton(
                        text = "EQUIP IN MINECRAFT BEDROCK",
                        icon = Icons.Default.Checkroom,
                        onClick = {
                            onEquipSkin(skin)
                            showEquipInstructionsDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = CyberCyan,
                        contentColor = BedrockBlack,
                        testTag = "equip_skin_button"
                    )
                }
            }
        }

        // Import Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onImportSkin(false) },
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LapisBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import Steve", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onImportSkin(true) },
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CardSurfaceVariant)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import Alex", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            SectionHeader(
                title = "SAVED SKINS",
                badgeText = "${skins.size}"
            )
        }

        if (skins.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Face, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                        Text("No skins imported yet", color = TextSecondary, fontSize = 14.sp)
                        Text("Tap 'Import Steve' or 'Import Alex' to load any 64x64 PNG file.", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(skins, key = { it.id }) { skin ->
                val isSelected = selectedSkin?.id == skin.id
                GamingCard(
                    borderColor = if (isSelected) CyberCyan else CardBorder,
                    onClick = { selectedSkin = skin },
                    modifier = Modifier.fillMaxWidth()
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
                                    .background(if (skin.isAlex) EmeraldGreen.copy(alpha = 0.15f) else CyberCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = if (skin.isAlex) EmeraldGreen else CyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = skin.name,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (skin.isAlex) "Alex Model (3px)" else "Steve Model (4px)",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    skinToRename = skin
                                    renameInput = skin.name
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { onDeleteSkin(skin) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedstoneCrimson, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Equip Instructions Dialog
    if (showEquipInstructionsDialog) {
        AlertDialog(
            onDismissRequest = { showEquipInstructionsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen)
                    Text("Skin Exported for Minecraft!", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Your skin was successfully exported to KGLauncher/exports/skins/ ready for Minecraft Bedrock:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianSurface)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "1. Open Minecraft Bedrock\n2. Tap Dressing Room / Profile\n3. Tap Edit Character > Classic Skins\n4. Tap Owned > Choose New Skin\n5. Pick the exported PNG!",
                            color = CyberCyan,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEquipInstructionsDialog = false
                        onLaunchMinecraft()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("OPEN MINECRAFT", color = BedrockBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEquipInstructionsDialog = false }) {
                    Text("Got it", color = TextSecondary)
                }
            }
        )
    }

    // Rename Skin Dialog
    skinToRename?.let { skin ->
        AlertDialog(
            onDismissRequest = { skinToRename = null },
            title = { Text("Rename Skin", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Skin Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            onRenameSkin(skin, renameInput.trim())
                        }
                        skinToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("Save", color = BedrockBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { skinToRename = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
