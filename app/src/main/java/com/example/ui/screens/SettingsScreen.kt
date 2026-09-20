package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.LauncherSettings
import com.example.data.models.LauncherThemeOption
import com.example.ui.components.GamingCard
import com.example.ui.components.GlowButton
import com.example.ui.components.KGTopBar
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    onUpdateSettings: (LauncherSettings) -> Unit,
    onBackupAllWorkspace: () -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showBackupSuccessDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Theme Selection
        item {
            GamingCard(borderColor = CyberCyan.copy(alpha = 0.4f)) {
                Text(
                    text = "GAMING THEME ACCENT",
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                val themes = listOf(
                    Triple(LauncherThemeOption.CYBER_BLUE, "Cyber Blue", CyberCyan),
                    Triple(LauncherThemeOption.OBSIDIAN_DARK, "Obsidian Dark", ObsidianDarkPrimary),
                    Triple(LauncherThemeOption.EMERALD_GREEN, "Emerald Green", EmeraldGreen),
                    Triple(LauncherThemeOption.REDSTONE_CRIMSON, "Redstone Crimson", RedstoneCrimson)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    themes.forEach { (themeOpt, label, accentColor) ->
                        val isSelected = settings.theme == themeOpt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) CardSurfaceVariant else ObsidianSurface)
                                .border(1.dp, if (isSelected) accentColor else CardBorder, RoundedCornerShape(10.dp))
                                .clickable { onUpdateSettings(settings.copy(theme = themeOpt)) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                                Text(
                                    text = label,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Performance Mode
        item {
            GamingCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Low Performance Mode",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Reduces 3D viewport canvas density for budget Android devices.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = settings.isLowPerformanceMode,
                        onCheckedChange = { onUpdateSettings(settings.copy(isLowPerformanceMode = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BedrockBlack,
                            checkedTrackColor = CyberCyan
                        )
                    )
                }
            }
        }

        // Workspace Storage Info
        item {
            GamingCard {
                Text(
                    text = "KG WORKSPACE STORAGE",
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "To guarantee zero conflict and full Play Store policy compliance, KG Launcher operates inside an isolated sandbox.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianSurface)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = settings.workspacePath,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        IconButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Workspace Path", settings.workspacePath))
                                Toast.makeText(context, "Path copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Path", tint = TextMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Workspace Backup & Clean Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onBackupAllWorkspace()
                        showBackupSuccessDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 46.dp)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null, tint = BedrockBlack, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("FULL BACKUP", color = BedrockBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        onClearCache()
                        Toast.makeText(context, "Temporary cache cleared!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CardSurfaceVariant),
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 46.dp)
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CLEAR CACHE", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // System & Device Diagnostics Card
        item {
            GamingCard(borderColor = CyberCyan.copy(alpha = 0.3f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SYSTEM & ENGINE DIAGNOSTICS",
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                val runtime = Runtime.getRuntime()
                val usedMemMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                val maxMemMb = runtime.maxMemory() / (1024 * 1024)
                val androidVersion = android.os.Build.VERSION.RELEASE
                val apiLevel = android.os.Build.VERSION.SDK_INT
                val deviceModel = "${android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${android.os.Build.MODEL}"

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Device Hardware", color = TextSecondary, fontSize = 12.sp)
                        Text(deviceModel, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Android OS", color = TextSecondary, fontSize = 12.sp)
                        Text("Android $androidVersion (API $apiLevel)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("JVM Memory", color = TextSecondary, fontSize = 12.sp)
                        Text("$usedMemMb MB / $maxMemMb MB Allocated", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Canvas Hardware Accel", color = TextSecondary, fontSize = 12.sp)
                        Text("Active (OpenGL ES / Skia)", color = CyberCyan, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Storage Protocol", color = TextSecondary, fontSize = 12.sp)
                        Text("Scoped Sandbox + FileProvider", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Legal & Mojang Disclaimer Card
        item {
            GamingCard(borderColor = CardBorder) {
                Text(
                    text = "ABOUT KG BEDROCK LAUNCHER",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Version 1.0.0-PRO (Build 2026)",
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = CardBorder)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "DISCLAIMER: NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.",
                    color = TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "KG Bedrock Launcher respects all Google Play Developer Program policies. Does not distribute APKs, bypass DRM, or modify game binaries.",
                    color = TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }

    if (showBackupSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showBackupSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen)
                    Text("Full Backup Created!", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("All worlds, skins, 3D models, and packs have been safely archived to KGLauncher/exports/kg_backup_all.zip", color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = { showBackupSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("OK", color = BedrockBlack, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
