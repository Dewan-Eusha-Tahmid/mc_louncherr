package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.*
import com.example.ui.components.GamingCard
import com.example.ui.components.GlowButton
import com.example.ui.components.KGTopBar
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ModelCreatorScreen(
    currentProject: CustomModelProject,
    onSaveProject: (CustomModelProject) -> Unit,
    onExportZip: (CustomModelProject) -> Unit,
    onImportModel: () -> Unit,
    onNewModel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var project by remember(currentProject) { mutableStateOf(currentProject) }
    var selectedBoneIndex by remember { mutableIntStateOf(0) }
    var selectedCubeIndex by remember { mutableIntStateOf(0) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: 3D Viewport, 1: Hierarchy, 2: Transform, 3: UV Editor, 4: Animation

    // 3D Canvas states
    var rotY by remember { mutableFloatStateOf(35f) }
    var rotX by remember { mutableFloatStateOf(20f) }
    var isWireframe by remember { mutableStateOf(false) }

    // Animation preview states
    var isPlayingAnimation by remember { mutableStateOf(false) }
    var animCurrentTime by remember { mutableFloatStateOf(0f) }

    // Playback loop
    LaunchedEffect(isPlayingAnimation) {
        while (isPlayingAnimation) {
            delay(50)
            animCurrentTime = (animCurrentTime + 0.05f).mod(2.0f)
        }
    }

    val selectedBone = project.bones.getOrNull(selectedBoneIndex)
    val selectedCube = selectedBone?.cubes?.getOrNull(selectedCubeIndex)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BedrockBlack)
    ) {
        // Model Header Bar
        GamingCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = project.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Format: Bedrock 1.12.0 • ${project.bones.size} bones",
                        color = CyberCyan,
                        fontSize = 11.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            onSaveProject(project)
                            Toast.makeText(context, "Saved model project!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                    ) {
                        Text("SAVE", color = BedrockBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { onExportZip(project) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                    ) {
                        Text("EXPORT .ZIP", color = BedrockBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Sub-tabs: 3D Viewport, Hierarchy, Transforms, UV Grid, Animation
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            containerColor = ObsidianSurface,
            contentColor = CyberCyan,
            edgePadding = 16.dp,
            divider = {}
        ) {
            listOf("3D Viewport", "Hierarchy", "Transforms", "UV Editor", "Animation").forEachIndexed { index, title ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    text = { Text(title, fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) }
                )
            }
        }

        // Main content area based on tab
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            when (activeTab) {
                0 -> {
                    // TAB 0: Real 3D Viewport
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(ObsidianSurface)
                                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        rotY = (rotY + dragAmount.x * 0.7f).mod(360f)
                                        rotX = (rotX - dragAmount.y * 0.5f).coerceIn(-60f, 60f)
                                    }
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cx = size.width / 2f
                                val cy = size.height / 2f

                                val radY = Math.toRadians(rotY.toDouble())
                                val radX = Math.toRadians(rotX.toDouble())
                                val cosY = cos(radY).toFloat()
                                val sinY = sin(radY).toFloat()
                                val cosX = cos(radX).toFloat()
                                val sinX = sin(radX).toFloat()

                                fun project(x: Float, y: Float, z: Float): Offset {
                                    val x1 = x * cosY - z * sinY
                                    val z1 = x * sinY + z * cosY
                                    val y2 = y * cosX - z1 * sinX
                                    val scale = 4.5f
                                    return Offset(cx + x1 * scale, cy + y2 * scale)
                                }

                                // Ground Grid
                                val gridColor = CardBorder.copy(alpha = 0.5f)
                                for (i in -4..4) {
                                    val p1 = project(i * 8f, 0f, -32f)
                                    val p2 = project(i * 8f, 0f, 32f)
                                    drawLine(gridColor, p1, p2, 1f)
                                    val p3 = project(-32f, 0f, i * 8f)
                                    val p4 = project(32f, 0f, i * 8f)
                                    drawLine(gridColor, p3, p4, 1f)
                                }

                                // Reusable paths for face rendering
                                val topPath = Path()
                                val frontPath = Path()
                                val sidePath = Path()

                                // Draw all cubes in all bones
                                project.bones.forEachIndexed { bIdx, bone ->
                                    bone.cubes.forEachIndexed { cIdx, cube ->
                                        val isSelected = bIdx == selectedBoneIndex && cIdx == selectedCubeIndex
                                        val ox = cube.origin[0]
                                        val oy = -cube.origin[1] - cube.size[1] // inverted Y for Minecraft up
                                        val oz = cube.origin[2]
                                        val w = cube.size[0]
                                        val h = cube.size[1]
                                        val d = cube.size[2]

                                        val p000 = project(ox, oy, oz)
                                        val p100 = project(ox + w, oy, oz)
                                        val p101 = project(ox + w, oy, oz + d)
                                        val p001 = project(ox, oy, oz + d)

                                        val p010 = project(ox, oy + h, oz)
                                        val p110 = project(ox + w, oy + h, oz)
                                        val p111 = project(ox + w, oy + h, oz + d)
                                        val p011 = project(ox, oy + h, oz + d)

                                        val baseColor = if (isSelected) CyberCyan else LapisBlue
                                        val topColor = baseColor.copy(alpha = 0.9f)
                                        val frontColor = baseColor.copy(alpha = 0.75f)
                                        val sideColor = baseColor.copy(alpha = 0.6f)

                                        if (isWireframe) {
                                            val stroke = Stroke(width = 2f)
                                            val wireColor = if (isSelected) CyberCyan else TextSecondary
                                            drawLine(wireColor, p000, p100, 2f)
                                            drawLine(wireColor, p100, p101, 2f)
                                            drawLine(wireColor, p101, p001, 2f)
                                            drawLine(wireColor, p001, p000, 2f)
                                            drawLine(wireColor, p010, p110, 2f)
                                            drawLine(wireColor, p110, p111, 2f)
                                            drawLine(wireColor, p111, p011, 2f)
                                            drawLine(wireColor, p011, p010, 2f)
                                            drawLine(wireColor, p000, p010, 2f)
                                            drawLine(wireColor, p100, p110, 2f)
                                            drawLine(wireColor, p101, p111, 2f)
                                            drawLine(wireColor, p001, p011, 2f)
                                        } else {
                                            // Fill faces with reused paths
                                            topPath.rewind()
                                            topPath.moveTo(p000.x, p000.y)
                                            topPath.lineTo(p100.x, p100.y)
                                            topPath.lineTo(p110.x, p110.y)
                                            topPath.lineTo(p010.x, p010.y)
                                            topPath.close()
                                            drawPath(topPath, topColor)

                                            frontPath.rewind()
                                            frontPath.moveTo(p010.x, p010.y)
                                            frontPath.lineTo(p110.x, p110.y)
                                            frontPath.lineTo(p111.x, p111.y)
                                            frontPath.lineTo(p011.x, p011.y)
                                            frontPath.close()
                                            drawPath(frontPath, frontColor)

                                            sidePath.rewind()
                                            sidePath.moveTo(p100.x, p100.y)
                                            sidePath.lineTo(p101.x, p101.y)
                                            sidePath.lineTo(p111.x, p111.y)
                                            sidePath.lineTo(p110.x, p110.y)
                                            sidePath.close()
                                            drawPath(sidePath, sideColor)

                                            // Outline
                                            drawPath(topPath, Color.White.copy(alpha = 0.3f), style = Stroke(1f))
                                            drawPath(frontPath, Color.White.copy(alpha = 0.3f), style = Stroke(1f))
                                            drawPath(sidePath, Color.White.copy(alpha = 0.3f), style = Stroke(1f))
                                        }
                                    }
                                }
                            }

                            // Viewport HUD
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = isWireframe,
                                    onClick = { isWireframe = !isWireframe },
                                    label = { Text("Wireframe", fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: Hierarchy (Bones & Cubes)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val newBoneName = "bone_${project.bones.size}"
                                        val newBones = project.bones + ModelBone(name = newBoneName)
                                        project = project.copy(bones = newBones)
                                        selectedBoneIndex = newBones.lastIndex
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = LapisBlue)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Bone", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        selectedBone?.let { bone ->
                                            val newCube = ModelCube()
                                            val updatedBone = bone.copy(cubes = bone.cubes + newCube)
                                            val updatedBones = project.bones.toMutableList().apply {
                                                set(selectedBoneIndex, updatedBone)
                                            }
                                            project = project.copy(bones = updatedBones)
                                            selectedCubeIndex = updatedBone.cubes.lastIndex
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                                ) {
                                    Icon(Icons.Default.AddBox, contentDescription = null, tint = BedrockBlack, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Cube", fontSize = 12.sp, color = BedrockBlack, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        items(project.bones.indices.toList()) { bIdx ->
                            val bone = project.bones[bIdx]
                            val isBoneSelected = bIdx == selectedBoneIndex

                            GamingCard(
                                borderColor = if (isBoneSelected) CyberCyan else CardBorder,
                                onClick = {
                                    selectedBoneIndex = bIdx
                                    selectedCubeIndex = 0
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.AccountTree, contentDescription = null, tint = if (isBoneSelected) CyberCyan else TextSecondary, modifier = Modifier.size(18.dp))
                                        Text(bone.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("(${bone.cubes.size} cubes)", color = TextSecondary, fontSize = 11.sp)
                                    }

                                    Row {
                                        IconButton(
                                            onClick = {
                                                if (project.bones.size > 1) {
                                                    val updated = project.bones.toMutableList().apply { removeAt(bIdx) }
                                                    project = project.copy(bones = updated)
                                                    selectedBoneIndex = 0
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedstoneCrimson, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                // List cubes in this bone
                                if (isBoneSelected && bone.cubes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    bone.cubes.forEachIndexed { cIdx, cube ->
                                        val isCubeSelected = cIdx == selectedCubeIndex
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isCubeSelected) CardSurfaceVariant else Color.Transparent)
                                                .clickable { selectedCubeIndex = cIdx }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Cube #$cIdx: [${cube.size[0].toInt()}x${cube.size[1].toInt()}x${cube.size[2].toInt()}]",
                                                color = if (isCubeSelected) CyberCyan else TextSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            IconButton(
                                                onClick = {
                                                    val updatedCubes = bone.cubes.toMutableList().apply { removeAt(cIdx) }
                                                    val updatedBone = bone.copy(cubes = updatedCubes)
                                                    val updatedBones = project.bones.toMutableList().apply { set(bIdx, updatedBone) }
                                                    project = project.copy(bones = updatedBones)
                                                    selectedCubeIndex = 0
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Delete Cube", tint = TextMuted, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: Cube Transforms (Move, Scale, Pivot)
                    if (selectedCube != null && selectedBone != null) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text("Cube Dimensions (Size X / Y / Z)", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("W: " to 0, "H: " to 1, "D: " to 2).forEach { (prefix, axis) ->
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(prefix + selectedCube.size[axis].toInt(), color = TextSecondary, fontSize = 11.sp)
                                            Slider(
                                                value = selectedCube.size[axis],
                                                onValueChange = { newVal ->
                                                    val newSize = selectedCube.size.clone().apply { set(axis, newVal) }
                                                    val updatedCube = selectedCube.copy(size = newSize)
                                                    updateSelectedCube(project, selectedBoneIndex, selectedCubeIndex, updatedCube) { project = it }
                                                },
                                                valueRange = 1f..32f
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Text("Cube Origin Position (X / Y / Z)", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("X: " to 0, "Y: " to 1, "Z: " to 2).forEach { (prefix, axis) ->
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(prefix + selectedCube.origin[axis].toInt(), color = TextSecondary, fontSize = 11.sp)
                                            Slider(
                                                value = selectedCube.origin[axis],
                                                onValueChange = { newVal ->
                                                    val newOrig = selectedCube.origin.clone().apply { set(axis, newVal) }
                                                    val updatedCube = selectedCube.copy(origin = newOrig)
                                                    updateSelectedCube(project, selectedBoneIndex, selectedCubeIndex, updatedCube) { project = it }
                                                },
                                                valueRange = -32f..32f
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Text("Bone Pivot Point (X / Y / Z)", color = GoldIngot, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("X: " to 0, "Y: " to 1, "Z: " to 2).forEach { (prefix, axis) ->
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(prefix + selectedBone.pivot[axis].toInt(), color = TextSecondary, fontSize = 11.sp)
                                            Slider(
                                                value = selectedBone.pivot[axis],
                                                onValueChange = { newVal ->
                                                    val newPivot = selectedBone.pivot.clone().apply { set(axis, newVal) }
                                                    val updatedBone = selectedBone.copy(pivot = newPivot)
                                                    val updatedBones = project.bones.toMutableList().apply { set(selectedBoneIndex, updatedBone) }
                                                    project = project.copy(bones = updatedBones)
                                                },
                                                valueRange = -32f..32f
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Select a bone and cube in Hierarchy first", color = TextSecondary)
                        }
                    }
                }

                3 -> {
                    // TAB 3: UV Editor (64x64 Texture Grid)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "64x64 TEXTURE UV MAP",
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObsidianSurface)
                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val gridW = size.width / 64f
                                val gridH = size.height / 64f

                                // Draw grid
                                val gridColor = CardBorder.copy(alpha = 0.3f)
                                for (i in 0..64 step 8) {
                                    drawLine(gridColor, Offset(i * gridW, 0f), Offset(i * gridW, size.height), 1f)
                                    drawLine(gridColor, Offset(0f, i * gridH), Offset(size.width, i * gridH), 1f)
                                }

                                // Highlight selected cube UV box
                                selectedCube?.let { cube ->
                                    val u = cube.uv[0]
                                    val v = cube.uv[1]
                                    val w = cube.size[0] + cube.size[2]
                                    val h = cube.size[1] + cube.size[2]

                                    drawRect(
                                        color = CyberCyan.copy(alpha = 0.35f),
                                        topLeft = Offset(u * gridW, v * gridH),
                                        size = Size(w * gridW, h * gridH)
                                    )
                                    drawRect(
                                        color = CyberCyan,
                                        topLeft = Offset(u * gridW, v * gridH),
                                        size = Size(w * gridW, h * gridH),
                                        style = Stroke(2f)
                                    )
                                }
                            }
                        }

                        selectedCube?.let { cube ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("UV X: ${cube.uv[0].toInt()}", color = TextSecondary, fontSize = 11.sp)
                                    Slider(
                                        value = cube.uv[0],
                                        onValueChange = { newVal ->
                                            val newUv = floatArrayOf(newVal, cube.uv[1])
                                            updateSelectedCube(project, selectedBoneIndex, selectedCubeIndex, cube.copy(uv = newUv)) { project = it }
                                        },
                                        valueRange = 0f..64f
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("UV Y: ${cube.uv[1].toInt()}", color = TextSecondary, fontSize = 11.sp)
                                    Slider(
                                        value = cube.uv[1],
                                        onValueChange = { newVal ->
                                            val newUv = floatArrayOf(cube.uv[0], newVal)
                                            updateSelectedCube(project, selectedBoneIndex, selectedCubeIndex, cube.copy(uv = newUv)) { project = it }
                                        },
                                        valueRange = 0f..64f
                                    )
                                }
                            }
                        }
                    }
                }

                4 -> {
                    // TAB 4: Animation Timeline
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Keyframe Timeline (2.0s Loop)",
                                color = NetherPurple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            IconButton(
                                onClick = { isPlayingAnimation = !isPlayingAnimation }
                            ) {
                                Icon(
                                    imageVector = if (isPlayingAnimation) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = CyberCyan
                                )
                            }
                        }

                        Slider(
                            value = animCurrentTime,
                            onValueChange = { animCurrentTime = it },
                            valueRange = 0f..2.0f,
                            colors = SliderDefaults.colors(thumbColor = NetherPurple, activeTrackColor = NetherPurple)
                        )

                        Text("Time: ${String.format("%.2f", animCurrentTime)}s", color = TextSecondary, fontSize = 12.sp)

                        Button(
                            onClick = {
                                selectedBone?.let { bone ->
                                    val kf = AnimationKeyframe(
                                        time = animCurrentTime,
                                        boneName = bone.name,
                                        rotationOffset = floatArrayOf(0f, 15f, 0f)
                                    )
                                    val anim = ModelAnimation(keyframes = listOf(kf))
                                    project = project.copy(animations = listOf(anim))
                                    Toast.makeText(context, "Keyframe added at ${animCurrentTime}s for ${bone.name}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NetherPurple),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add Keyframe at Current Time", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun updateSelectedCube(
    project: CustomModelProject,
    boneIndex: Int,
    cubeIndex: Int,
    updatedCube: ModelCube,
    onUpdated: (CustomModelProject) -> Unit
) {
    val bone = project.bones.getOrNull(boneIndex) ?: return
    val updatedCubes = bone.cubes.toMutableList()
    if (cubeIndex in updatedCubes.indices) {
        updatedCubes[cubeIndex] = updatedCube
        val updatedBone = bone.copy(cubes = updatedCubes)
        val updatedBones = project.bones.toMutableList().apply { set(boneIndex, updatedBone) }
        onUpdated(project.copy(bones = updatedBones))
    }
}
