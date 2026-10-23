package com.emjaycruz.coaf

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

private val StudioBackground = Color(0xFF100D18)
private val PanelBackground = Color(0xFF20182C)
private val Purple = Color(0xFF9B5CFF)
private val LightPurple = Color(0xFFD0B5FF)
private val TextWhite = Color(0xFFF7F2FF)
private val MutedText = Color(0xFFB7AFC4)
private val TrackGreen = Color(0xFF72E0B0)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = StudioBackground
                ) {
                    CoafStudio()
                }
            }
        }
    }
}

private class StudioTrack(val number: Int) {
    var name by mutableStateOf(
        if (number == 16) "BEAT / INSTRUMENTAL"
        else "Audio Track $number"
    )

    var uri by mutableStateOf<Uri?>(null)
    var duration by mutableStateOf(0)
    var position by mutableStateOf(0)
    var volume by mutableStateOf(1f)
    var muted by mutableStateOf(false)
    var solo by mutableStateOf(false)
    var loaded by mutableStateOf(false)
    var playing by mutableStateOf(false)
    var player: MediaPlayer? = null
}

@Composable
private fun CoafStudio() {
    val context = LocalContext.current

    val tracks = remember {
        mutableStateListOf<StudioTrack>().apply {
            repeat(16) { index ->
                add(StudioTrack(index + 1))
            }
        }
    }

    var selectedTrack by remember { mutableStateOf(1) }
    var currentTab by remember { mutableStateOf("TRACKS") }
    var projectName by remember { mutableStateOf("My COAF Project") }
    var playing by remember { mutableStateOf(false) }

    val selected = tracks[selectedTrack - 1]

    fun stopAll() {
        tracks.forEach { track ->
            try {
                track.player?.pause()
                track.player?.seekTo(0)
            } catch (_: Exception) {
            }

            track.position = 0
            track.playing = false
        }

        playing = false
    }

    fun pauseAll() {
        tracks.forEach { track ->
            try {
                track.player?.pause()
            } catch (_: Exception) {
            }

            track.playing = false
        }

        playing = false
    }

    fun startPlayback() {
        val hasAudio = tracks.any { it.loaded && !it.muted }

        if (!hasAudio) {
            Toast.makeText(
                context,
                "Import an audio file first.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val hasSolo = tracks.any { it.solo && it.loaded }

        tracks.forEach { track ->
            val shouldPlay =
                track.loaded &&
                !track.muted &&
                (!hasSolo || track.solo)

            if (shouldPlay) {
                try {
                    track.player?.start()
                    track.playing = true
                } catch (_: Exception) {
                }
            }
        }

        playing = true
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val track = tracks[selectedTrack - 1]

            try {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }

                track.player?.release()
                track.player = null

                val player = MediaPlayer()
                player.setDataSource(context, uri)

                player.setOnPreparedListener { prepared ->
                    track.player = prepared
                    track.duration = prepared.duration.coerceAtLeast(0)
                    track.position = 0
                    track.uri = uri
                    track.name = uri.lastPathSegment
                        ?.substringAfterLast('/')
                        ?.take(32)
                        ?: "Imported Audio"

                    track.loaded = true
                    prepared.setVolume(
                        if (track.muted) 0f else track.volume,
                        if (track.muted) 0f else track.volume
                    )

                    Toast.makeText(
                        context,
                        "Audio loaded into Track ${track.number}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                player.setOnCompletionListener {
                    track.playing = false
                    track.position = track.duration
                }

                player.setOnErrorListener { _, _, _ ->
                    track.playing = false

                    Toast.makeText(
                        context,
                        "Unable to play this audio file.",
                        Toast.LENGTH_LONG
                    ).show()

                    true
                }

                player.prepareAsync()

            } catch (error: Exception) {
                Toast.makeText(
                    context,
                    "Could not open audio: ${error.localizedMessage ?: "unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tracks.forEach { track ->
                try {
                    track.player?.release()
                } catch (_: Exception) {
                }

                track.player = null
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            tracks.forEach { track ->
                val player = track.player

                if (player != null && track.playing) {
                    try {
                        if (player.isPlaying) {
                            track.position = player.currentPosition
                        } else if (
                            track.duration > 0 &&
                            track.position >= track.duration
                        ) {
                            track.playing = false
                        }
                    } catch (_: Exception) {
                    }
                }
            }

            delay(250)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBackground)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PanelBackground)
                .padding(18.dp)
        ) {
            Text(
                text = "COAF STUDIO",
                color = LightPurple,
                fontSize = 25.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "CRUZ OF ALL FLAVORS",
                color = TextWhite,
                fontSize = 11.sp,
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "A studio for the King of All Flavors 👑",
                color = MutedText,
                fontSize = 12.sp
            )
        }

        // Project information
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "PROJECT",
                    color = MutedText,
                    fontSize = 10.sp
                )

                Text(
                    projectName,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            OutlinedButton(
                onClick = {
                    projectName = "COAF Project ${System.currentTimeMillis() % 10000}"

                    Toast.makeText(
                        context,
                        "Project name updated.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ) {
                Text("NEW", color = LightPurple)
            }
        }

        // Playback controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = PanelBackground
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    "TRANSPORT",
                    color = LightPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { startPlayback() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Purple
                        )
                    ) {
                        Text("▶ PLAY")
                    }

                    OutlinedButton(
                        onClick = { pauseAll() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ⅱ PAUSE", color = TextWhite)
                    }

                    OutlinedButton(
                        onClick = { stopAll() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("■ STOP", color = TextWhite)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    if (playing) "Playback running"
                    else "Playback stopped or paused",
                    color = if (playing) TrackGreen else MutedText,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Navigation tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(
                    androidx.compose.foundation.rememberScrollState()
                )
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("TRACKS", "MIXER", "PLUGINS", "PROJECT").forEach { tab ->
                Button(
                    onClick = { currentTab = tab },
                    colors = ButtonDefaults.buttonColors(
                        containerColor =
                            if (currentTab == tab) Purple
                            else PanelBackground
                    )
                ) {
                    Text(tab, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        when (currentTab) {
            "TRACKS" -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "MULTITRACK WORKSPACE",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        Text(
                            "Select a track before importing audio.",
                            color = MutedText,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = {
                            audioPicker.launch(
                                arrayOf(
                                    "audio/*"
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Purple
                        )
                    ) {
                        Text("+ AUDIO")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(
                        items = tracks,
                        key = { it.number }
                    ) { track ->
                        TrackCard(
                            track = track,
                            selected = selectedTrack == track.number,
                            onSelect = {
                                selectedTrack = track.number
                            },
                            onMute = {
                                track.muted = !track.muted

                                val gain =
                                    if (track.muted) 0f else track.volume

                                try {
                                    track.player?.setVolume(gain, gain)
                                } catch (_: Exception) {
                                }
                            },
                            onSolo = {
                                track.solo = !track.solo
                            },
                            onVolumeChange = { value ->
                                track.volume = value

                                val gain =
                                    if (track.muted) 0f else value

                                try {
                                    track.player?.setVolume(gain, gain)
                                } catch (_: Exception) {
                                }
                            }
                        )
                    }
                }
            }

            "MIXER" -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        "TRACK MIXER",
                        color = LightPurple,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Adjust each loaded track's volume, mute, or solo from the TRACKS tab.",
                        color = MutedText
                    )

                    Spacer(Modifier.height(14.dp))

                    tracks.filter { it.loaded }.forEach { track ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = PanelBackground
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    "Track ${track.number}: ${track.name}",
                                    color = TextWhite
                                )

                                Slider(
                                    value = track.volume,
                                    onValueChange = { value ->
                                        track.volume = value

                                        val gain =
                                            if (track.muted) 0f else value

                                        try {
                                            track.player?.setVolume(
                                                gain,
                                                gain
                                            )
                                        } catch (_: Exception) {
                                        }
                                    },
                                    valueRange = 0f..1f
                                )

                                Text(
                                    "Volume ${(track.volume * 100).toInt()}%",
                                    color = MutedText
                                )
                            }
                        }
                    }

                    if (tracks.none { it.loaded }) {
                        Text(
                            "Import an audio file to begin mixing.",
                            color = MutedText
                        )
                    }
                }
            }

            "PLUG
