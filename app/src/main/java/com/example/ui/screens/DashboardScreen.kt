package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.launcher.MinecraftStatus
import com.example.data.models.WorldInfo
import com.example.ui.components.GamingCard
import com.example.ui.components.GlowButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatBadge
import com.example.ui.theme.*

data class QuickToolItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun DashboardScreen(
    minecraftStatus: MinecraftStatus,
    worldsCount: Int,
    skinsCount: Int,
    packsCount: Int,
    modelsCount: Int,
    storageUsageText: String,
    onPlayClick: () -> Unit,
    onInstallPlayStoreClick: () -> Unit,
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickTools = listOf(
        QuickToolItem("world_manager", "Worlds", "Import/Export .mcworld", Icons.Default.Public, CyberCyan),
        QuickToolItem("skin_manager", "3D Skins", "PNG Import & 3D Preview", Icons.Default.Face, EmeraldGreen),
        QuickToolItem("model_creator", "Model Creator", "Bones, Cubes & UV Grid", Icons.Default.Category, LapisBlue),
        QuickToolItem("replay_system", "Replay Companion", "Camera Trajectory & Cmds", Icons.Default.Videocam, NetherPurple),
        QuickToolItem("voice_mic", "Voice / Mic", "Speech to Minecraft Cmds", Icons.Default.Mic, RedstoneCrimson),
        QuickToolItem("pack_manager", "Packs & Addons", "Secure .mcpack unzipper", Icons.Default.Extension, GoldIngot),
        QuickToolItem("cave_viewer", "Cave Viewer", "3D Slice & Layer Grid", Icons.Default.Layers, CyberCyan),
        QuickToolItem("tools_modular", "Toolbox Hub", "Analyzers & Converters", Icons.Default.Build, EmeraldGreen)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Launch Banner
        item {
            GamingCard(
                borderColor = if (minecraftStatus.isInstalled) CyberCyan.copy(alpha = 0.5f) else RedstoneCrimson.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (minecraftStatus.isInstalled) EmeraldGreen else RedstoneCrimson)
                            )
                            Text(
                                text = if (minecraftStatus.isInstalled) "BEDROCK DETECTED" else "NOT INSTALLED",
                                color = if (minecraftStatus.isInstalled) EmeraldGreen else RedstoneCrimson,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (minecraftStatus.isInstalled) "Minecraft Bedrock" else "Minecraft Missing",
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = if (minecraftStatus.isInstalled) {
                                "Version: ${minecraftStatus.versionName ?: "Active"}"
                            } else {
                                "Package: com.mojang.minecraftpe"
                            },
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primaryContainer, ObsidianSurface)
                                )
                            )
                            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (minecraftStatus.isInstalled) Icons.Default.SportsEsports else Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = if (minecraftStatus.isInstalled) CyberCyan else RedstoneCrimson,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (minecraftStatus.isInstalled) {
                    GlowButton(
                        text = "PLAY MINECRAFT",
                        icon = Icons.Default.PlayArrow,
                        onClick = onPlayClick,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = CyberCyan,
                        contentColor = BedrockBlack,
                        testTag = "play_minecraft_button"
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Minecraft Bedrock Edition was not found on this device. Install official version from Google Play Store:",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        GlowButton(
                            text = "GET ON PLAY STORE",
                            icon = Icons.Default.Shop,
                            onClick = onInstallPlayStoreClick,
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = EmeraldGreen,
                            contentColor = BedrockBlack,
                            testTag = "install_play_store_button"
                        )
                    }
                }
            }
        }

        // Metrics / Storage Usage Overview
        item {
            SectionHeader(title = "WORKSPACE OVERVIEW", badgeText = "ISOLATED")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBadge(
                    label = "Worlds",
                    value = "$worldsCount",
                    icon = Icons.Default.Public,
                    accentColor = CyberCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("world_manager") }
                )
                StatBadge(
                    label = "Skins",
                    value = "$skinsCount",
                    icon = Icons.Default.Face,
                    accentColor = EmeraldGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("skin_manager") }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBadge(
                    label = "Packs",
                    value = "$packsCount",
                    icon = Icons.Default.Extension,
                    accentColor = GoldIngot,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("pack_manager") }
                )
                StatBadge(
                    label = "3D Models",
                    value = "$modelsCount",
                    icon = Icons.Default.Category,
                    accentColor = NetherPurple,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("model_creator") }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = LapisBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "KG Workspace Storage:",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = storageUsageText,
                        color = CyberCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Quick Tools Grid
        item {
            SectionHeader(
                title = "QUICK TOOLS",
                actionText = "View All",
                onActionClick = { onNavigateTo("tools_modular") }
            )
        }

        // 2-column quick tools cards
        items(quickTools.chunked(2)) { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (tool in pair) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                            .clickable { onNavigateTo(tool.id) }
                            .padding(14.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(tool.accentColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tool.icon,
                                    contentDescription = null,
                                    tint = tool.accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = tool.title,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = tool.subtitle,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
