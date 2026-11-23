package dev.neko.kvmeow.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.util.Locale

/**
 * 🎙️ Meeting Audio Capture & Headset Routing Deck Panel 🎙️
 *
 * Displays:
 * 1. Headset & Host Audio Bridge Status (Soundcore C30i / LAN/USB PCM Streamer)
 * 2. Record / Pause / Stop controls with active pulsating waveform visualization
 * 3. Live recording timer (MM:SS)
 * 4. Whisper transcription & Zettelkasten (.anymd) handoff status card
 */
@Composable
fun MeetingAudioControlCard(
    theme: ThemePalette,
    isRecording: Boolean,
    elapsedMs: Long,
    amplitudeRms: Float,
    isBridgeActive: Boolean,
    bridgeStatus: String,
    lastSavedFile: File?,
    onToggleRecording: () -> Unit,
    onToggleBridge: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, theme.cardBorder, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Title & Bridge Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🎙️ Meeting Audio & Headset",
                        color = theme.primaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // LAN / USB Audio Bridge toggle button
                Button(
                    onClick = onToggleBridge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBridgeActive) theme.statusActive else theme.buttonBackground
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(26.dp)
                        .border(1.dp, theme.cardBorder, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = if (isBridgeActive) "🎧 Bridge ON (C30i)" else "🎧 Bridge OFF",
                        color = if (isBridgeActive) theme.background else theme.secondaryText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle / Routing Status
            Text(
                text = "Routing: PC/Mac Stream (LAN/USB 28200) ➔ Soundcore C30i + AudioRecord",
                color = theme.secondaryText.copy(alpha = 0.7f),
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Recording Controls & Live Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Record/Stop Action Button
                Button(
                    onClick = onToggleRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color(0xFFEF4444) else theme.accentPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Record",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRecording) "Stop & Save" else "Record Meeting",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Live Timer Display
                val seconds = (elapsedMs / 1000) % 60
                val minutes = (elapsedMs / (1000 * 60))
                val timeString = String.format(Locale.US, "%02d:%02d", minutes, seconds)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isRecording) "REC $timeString" else "IDLE 00:00",
                        color = if (isRecording) theme.primaryText else theme.secondaryText,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Waveform Visualizer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(theme.surface, RoundedCornerShape(8.dp))
                    .border(1.dp, theme.cardBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val barCount = 18
                for (idx in 0 until barCount) {
                    val scale = if (isRecording) {
                        val factor = ((idx * 7) % 11) / 10f
                        (amplitudeRms * 2f * factor + 0.15f).coerceIn(0.15f, 1.0f)
                    } else {
                        0.15f
                    }
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight(scale)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isRecording) theme.accentPrimary else theme.cardBorder)
                    )
                }
            }

            // Recent Note Handoff Card
            if (lastSavedFile != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.buttonBackground, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "✅ Saved: ${lastSavedFile.name}",
                            color = theme.primaryText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Handoff: local Whisper + Zettelkasten (.anymd)",
                            color = theme.secondaryText,
                            fontSize = 9.sp
                        )
                    }
                    Text(
                        text = "16kHz WAV",
                        color = theme.accentSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
