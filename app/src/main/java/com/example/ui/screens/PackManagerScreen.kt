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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.PackInfo
import com.example.data.models.PackType
import com.example.ui.components.GamingCard
import com.example.ui.components.GlowButton
import com.example.ui.components.KGTopBar
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*

@Composable
fun PackManagerScreen(
    packs: List<PackInfo>,
    onImportPack: () -> Unit,
    onTogglePackEnabled: (PackInfo) -> Unit,
    onDeletePack: (PackInfo) -> Unit,
    onDuplicatePack: (PackInfo) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: All, 1: Resource, 2: Behavior

    val filteredPacks = remember(packs, selectedTab) {
        when (selectedTab) {
            1 -> packs.filter { it.packType == PackType.RESOURCE }
            2 -> packs.filter { it.packType == PackType.BEHAVIOR }
            else -> packs
        }
    }

    Scaffold(
        topBar = {
            KGTopBar(
                title = "Resource & Behavior Packs",
                subtitle = "manifest.json Parser • .mcpack / .mcaddon",
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
            // Import Actions Header
            item {
                GamingCard(borderColor = GoldIngot.copy(alpha = 0.4f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "PACK MANAGER",
                                color = GoldIngot,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Installed Packs: ${packs.size}",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = GoldIngot,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    GlowButton(
                        text = "IMPORT .MCPACK / .MCADDON",
                        icon = Icons.Default.FileOpen,
                        onClick = onImportPack,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = GoldIngot,
                        contentColor = BedrockBlack,
                        testTag = "import_pack_button"
                    )
                }
            }

            // Filter Tabs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All (${packs.size})", "Resources", "Behaviors").forEachIndexed { index, title ->
                        FilterChip(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            label = { Text(title, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = CyberCyan
                            )
                        )
                    }
                }
            }

            if (filteredPacks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Extension, contentDescription = null, tint = TextMuted, modifier = Modifier.size(44.dp))
                            Text("No Packs in this category", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Import any standard Bedrock .mcpack or .mcaddon file.", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(filteredPacks, key = { it.id }) { pack ->
                    GamingCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (pack.packType == PackType.RESOURCE) CyberCyan.copy(alpha = 0.15f) else NetherPurple.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (pack.packType == PackType.RESOURCE) Icons.Default.Texture else Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = if (pack.packType == PackType.RESOURCE) CyberCyan else NetherPurple,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = pack.name,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${pack.packType.name} • v${pack.version} • Min: ${pack.minEngineVersion}",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "UUID: ${pack.uuid.take(18)}...",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Switch(
                                checked = pack.isEnabled,
                                onCheckedChange = { onTogglePackEnabled(pack) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = BedrockBlack,
                                    checkedTrackColor = CyberCyan
                                )
                            )
                        }

                        if (pack.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = pack.description,
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 2
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { onDuplicatePack(pack) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Duplicate", fontSize = 11.sp)
                            }
                            IconButton(onClick = { onDeletePack(pack) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedstoneCrimson, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
