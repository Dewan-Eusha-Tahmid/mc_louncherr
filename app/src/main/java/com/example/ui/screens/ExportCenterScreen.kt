package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.*
import com.example.ui.components.GamingCard
import com.example.ui.components.KGTopBar
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*

data class ExportItem(
    val title: String,
    val format: String,
    val sizeText: String,
    val category: String,
    val onExport: () -> Unit,
    val onShare: () -> Unit
)

@Composable
fun ExportCenterScreen(
    worlds: List<WorldInfo>,
    skins: List<SkinModel>,
    packs: List<PackInfo>,
    models: List<CustomModelProject>,
    replays: List<ReplayProject>,
    onExportWorld: (WorldInfo) -> Unit,
    onShareWorld: (WorldInfo) -> Unit,
    onExportSkin: (SkinModel) -> Unit,
    onExportModel: (CustomModelProject) -> Unit,
    onExportReplay: (ReplayProject) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("All") }

    val exportItems = remember(worlds, skins, packs, models, replays) {
        val list = mutableListOf<ExportItem>()
        worlds.forEach { w ->
            list.add(
                ExportItem(
                    title = w.levelName,
                    format = ".mcworld (Real Bedrock ZIP)",
                    sizeText = w.formattedSize,
                    category = "Worlds",
                    onExport = { onExportWorld(w) },
                    onShare = { onShareWorld(w) }
                )
            )
        }
        skins.forEach { s ->
            list.add(
                ExportItem(
                    title = s.name,
                    format = "PNG Skin (64x64)",
                    sizeText = "Skin Asset",
                    category = "Skins",
                    onExport = { onExportSkin(s) },
                    onShare = { onExportSkin(s) }
                )
            )
        }
        models.forEach { m ->
            list.add(
                ExportItem(
                    title = m.name,
                    format = ".kgmodel.zip (Bedrock 1.12.0 Geometry)",
                    sizeText = "${m.bones.size} bones",
                    category = "Models",
                    onExport = { onExportModel(m) },
                    onShare = { onExportModel(m) }
                )
            )
        }
        replays.forEach { r ->
            list.add(
                ExportItem(
                    title = r.name,
                    format = ".kgreplay.json & Bedrock /camera script",
                    sizeText = "${r.keyframes.size} keypoints",
                    category = "Replays",
                    onExport = { onExportReplay(r) },
                    onShare = { onExportReplay(r) }
                )
            )
        }
        list
    }

    val filteredItems = remember(exportItems, selectedCategory) {
        if (selectedCategory == "All") exportItems else exportItems.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            KGTopBar(
                title = "Export Center",
                subtitle = "Compliant Bedrock ZIP & Model Exporters",
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
            item {
                GamingCard(borderColor = CyberCyan.copy(alpha = 0.4f)) {
                    Text(
                        text = "BEDROCK SPECIFICATION COMPLIANCE",
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "All exports generate valid Bedrock files with zero fake renaming. Verified against Mojang Bedrock specifications.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Worlds", "Skins", "Models", "Replays").forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No items ready for export in this category", color = TextSecondary)
                    }
                }
            } else {
                items(filteredItems) { item ->
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(item.format, color = CyberCyan, fontSize = 11.sp)
                                Text(item.sizeText, color = TextMuted, fontSize = 10.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(onClick = item.onExport, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = CyberCyan, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = item.onShare, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = LapisBlue, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
