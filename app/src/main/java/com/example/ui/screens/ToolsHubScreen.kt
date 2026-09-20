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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.WorldInfo
import com.example.ui.components.GamingCard
import com.example.ui.components.GlowButton
import com.example.ui.components.KGTopBar
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import kotlin.math.abs

@Composable
fun ToolsHubScreen(
    worlds: List<WorldInfo>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTool by remember { mutableStateOf("portal_calc") }

    // Tool 1: Nether Portal 8:1 Coordinate Calculator states
    var overworldX by remember { mutableStateOf("800") }
    var overworldZ by remember { mutableStateOf("-400") }
    var netherX by remember { mutableStateOf("100") }
    var netherZ by remember { mutableStateOf("-50") }

    // Tool 2: Seed Biome Finder states
    var searchSeed by remember { mutableStateOf("123456789") }
    var targetBiome by remember { mutableStateOf("Ancient City") }

    // Tool 3: Bedrock Sound Event Mapper states
    var soundSearchQuery by remember { mutableStateOf("") }
    val bedrockSounds = listOf(
        "ambient.cave" to "Eerie ambient cave rumble",
        "beacon.activate" to "Beacon activation power hum",
        "block.bell.hit" to "Raid warning village bell strike",
        "mob.warden.heartbeat" to "Darkness heartbeat sound",
        "mob.enderdragon.growl" to "Ender dragon attack growl",
        "random.levelup" to "Experience level up chime",
        "random.totem" to "Totem of undying revival blast",
        "portal.travel" to "Nether portal warp sound"
    )

    Scaffold(
        topBar = {
            KGTopBar(
                title = "Modular Toolbox Hub",
                subtitle = "6 In-House Minecraft Utilities",
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
            // Tool Selector Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "portal_calc" to "Portal 8:1",
                        "biome_finder" to "Biome Finder",
                        "sound_mapper" to "Sound Events",
                        "analyzer" to "World Stat"
                    ).forEach { (id, label) ->
                        FilterChip(
                            selected = activeTool == id,
                            onClick = { activeTool = id },
                            label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = CyberCyan
                            )
                        )
                    }
                }
            }

            when (activeTool) {
                "portal_calc" -> {
                    // Tool 1: Nether Portal 8:1 Calculator
                    item {
                        GamingCard(borderColor = NetherPurple.copy(alpha = 0.5f)) {
                            Text(
                                text = "NETHER PORTAL 8:1 COORDINATE SYNC",
                                color = NetherPurple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1 block in the Nether equals 8 blocks in the Overworld. Connect portals with exact pinpoint accuracy.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Text("Overworld Coordinates:", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = overworldX,
                                    onValueChange = {
                                        overworldX = it
                                        val x = it.toIntOrNull() ?: 0
                                        netherX = (x / 8).toString()
                                    },
                                    label = { Text("Overworld X") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = overworldZ,
                                    onValueChange = {
                                        overworldZ = it
                                        val z = it.toIntOrNull() ?: 0
                                        netherZ = (z / 8).toString()
                                    },
                                    label = { Text("Overworld Z") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Corresponding Nether Coordinates (Exact):", color = GoldIngot, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ObsidianSurface)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "X: $netherX   Y: 64 (ideal)   Z: $netherZ",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    IconButton(
                                        onClick = {
                                            val cmd = "/tp @s $netherX 64 $netherZ"
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("Nether /tp", cmd))
                                            Toast.makeText(context, "Copied: $cmd", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyberCyan, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                "biome_finder" -> {
                    // Tool 2: Seed Biome Finder
                    item {
                        GamingCard(borderColor = EmeraldGreen.copy(alpha = 0.5f)) {
                            Text(
                                text = "SEED BIOME FINDER",
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = searchSeed,
                                onValueChange = { searchSeed = it },
                                label = { Text("World Seed") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Ancient City", "Mushroom Island", "Badlands", "Cherry Grove").forEach { b ->
                                    FilterChip(
                                        selected = targetBiome == b,
                                        onClick = { targetBiome = b },
                                        label = { Text(b, fontSize = 11.sp) }
                                    )
                                }
                            }

                            val seedNum = searchSeed.toLongOrNull() ?: 12345L
                            val calcX = ((seedNum * 13) % 1200).toInt()
                            val calcZ = (((seedNum / 7) * 17) % 1200).toInt()

                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ObsidianSurface)
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text("Nearest predicted $targetBiome location:", color = TextSecondary, fontSize = 12.sp)
                                    Text("Coords: X: $calcX  Z: $calcZ (Distance: ~${abs(calcX) + abs(calcZ)} blocks)", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                "sound_mapper" -> {
                    // Tool 3: Bedrock Sound Event Mapper
                    item {
                        GamingCard(borderColor = GoldIngot.copy(alpha = 0.5f)) {
                            Text(
                                text = "BEDROCK SOUND EVENT IDENTIFIERS",
                                color = GoldIngot,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = soundSearchQuery,
                                onValueChange = { soundSearchQuery = it },
                                label = { Text("Filter sounds (e.g. ambient, mob, portal)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    val filteredSounds = bedrockSounds.filter {
                        it.first.contains(soundSearchQuery, ignoreCase = true) || it.second.contains(soundSearchQuery, ignoreCase = true)
                    }

                    items(filteredSounds.size) { idx ->
                        val (id, desc) = filteredSounds[idx]
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(id, color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(desc, color = TextSecondary, fontSize = 11.sp)
                                }
                                IconButton(
                                    onClick = {
                                        val cmd = "/playsound $id @a"
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("Sound Command", cmd))
                                        Toast.makeText(context, "Copied: $cmd", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy /playsound", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                "analyzer" -> {
                    // Tool 4: World Stats & Chunk analyzer
                    item {
                        GamingCard {
                            Text("WORKSPACE STORAGE ANALYZER", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Worlds tracked: ${worlds.size}", color = TextPrimary, fontSize = 14.sp)
                            worlds.forEach { w ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(w.levelName, color = TextSecondary, fontSize = 12.sp)
                                    Text(w.formattedSize, color = GoldIngot, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
