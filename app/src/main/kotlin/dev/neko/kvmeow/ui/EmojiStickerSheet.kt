package dev.neko.kvmeow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neko.kvmeow.unicode.UnicodeInputEngine

private val DeepIndigo = Color(0xFF2E1A47)
private val SoftLavender = Color(0xFFE6E6FA)
private val DarkSlate = Color(0xFF1E1E2E)
private val AmberGold = Color(0xFFFFFBEB)
private val CardPurple = Color(0xFF3B225C)
private val AccentLilac = Color(0xFFA78BFA)
private val KawaiiPink = Color(0xFFFFAFCC)

@Composable
fun EmojiStickerPicker(
    onSendEmoji: (String) -> Unit,
    onSendStickerMedia: (uri: String?, mimeType: String, fallbackText: String) -> Unit,
    onClose: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Smileys") }

    val categories = listOf("Smileys", "Cats & Animals", "Coding & Tech", "KitStickers", "Gestures")

    val smileys = listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
        "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰",
        "😘", "😗", "😙", "😚", "😋", "😛", "😜", "🤪",
        "🤨", "🧐", "🤓", "😎", "🤩", "🥳", "😏", "😒"
    )

    val catsAnimals = listOf(
        "🐱", "😸", "😹", "😻", "😼", "😽", "🙀", "😿",
        "😾", "🐈", "🐈‍⬛", "🐾", "🐶", "🦊", "🐼", "🐨",
        "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐙", "🦄"
    )

    val codingTech = listOf(
        "💻", "🖥️", "⌨️", "🖱️", "💾", "📡", "🚀", "🔥",
        "⚡", "💡", "🛠️", "⚙️", "🔧", "📦", "📈", "📊",
        "🔒", "🔑", "🧪", "👾", "✨", "🎉", "💯", "✅"
    )

    val gestures = listOf(
        "👍", "👎", "👌", "🤌", "✌️", "🤞", "🫰", "🤟",
        "🤘", "🤙", "👈", "👉", "👆", "🖕", "👇", "☝️",
        "👋", "🤚", "🖐️", "✋", "🖖", "👏", "🙌", "🤝"
    )

    // Sticker packs (KitStickerStudio & Mixtjis assets)
    val kitStickers = listOf(
        Triple("🐾 Kawaii Cat Headset", "image/png", "https://cdn.jsdelivr.net/gh/t3hkitty/stickers/cat_headset.png"),
        Triple("☕ Caffeine Keep-Alive", "image/png", "https://cdn.jsdelivr.net/gh/t3hkitty/stickers/caffeine_cat.png"),
        Triple("🔥 Aggressive 10-Key", "image/png", "https://cdn.jsdelivr.net/gh/t3hkitty/stickers/numpad_fire.png"),
        Triple("💻 Dual Host Win+Mac", "image/png", "https://cdn.jsdelivr.net/gh/t3hkitty/stickers/dual_host.png"),
        Triple("✨ Mission Control Swipe", "image/png", "https://cdn.jsdelivr.net/gh/t3hkitty/stickers/mission_control.png")
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepIndigo),
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .border(2.dp, CardPurple, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌸 Emoji & KitSticker Studio Bridge",
                    color = AmberGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = KawaiiPink)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category Tabs
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (cat in categories) {
                    val isSelected = cat == selectedCategory
                    Button(
                        onClick = { selectedCategory = cat },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) KawaiiPink else CardPurple
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) DarkSlate else SoftLavender,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grid Content
            if (selectedCategory == "KitStickers") {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(kitStickers) { (name, mime, url) ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardPurple),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSendStickerMedia(null, mime, url)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = name, color = AmberGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(text = "Paste Sticker 📋", color = AccentLilac, fontSize = 11.sp)
                            }
                        }
                    }
                }
            } else {
                val emojiList = when (selectedCategory) {
                    "Cats & Animals" -> catsAnimals
                    "Coding & Tech" -> codingTech
                    "Gestures" -> gestures
                    else -> smileys
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(emojiList) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(CardPurple.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { onSendEmoji(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}
