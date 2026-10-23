package com.emjaycruz.coaf

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Background = Color(0xFF101014)
private val Panel = Color(0xFF1D1B25)
private val Purple = Color(0xFF9B5CFF)
private val White = Color(0xFFF5F2FA)
private val Muted = Color(0xFFAAA4B8)
private val Green = Color(0xFF51D6A3)

private data class AudioTrack(
    val number: Int,
    val name: String,
    val uri: Uri? = null,
    val muted: Boolean = false,
    val solo: Boolean = false,
    val volume: Float = 1f
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            COAFStudioScreen()
        }
    }
}

@Composable
fun COAFStudioScreen() {
    var projectName by remember {
        mutableStateOf("My First Recording")
    }

    var recording by remember {
        mutableStateOf(false)
    }

    var currentScreen by remember {
        mutableStateOf("home")
    }

    var message by remember {
        mutableStateOf("Your studio is ready.")
    }

    var selectedTrack by remember {
        mutableIntStateOf(16)
    }

    var tracks by remember {
        mutableStateOf(
            (1..16).map { number ->
                AudioTrack(
                    number = number,
                    name = when (number) {
                        1 -> "Lead Vocal"
                        2 -> "Vocal Double"
                        3 -> "Harmony"
                        4 -> "Background Vocal"
                        5 -> "Ad-libs"
                        6 -> "Vocal Layer 2"
                        7 -> "Vocal Layer 3"
                        8 -> "Chorus Vocal"
                        9 -> "Verse Vocal"
                        10 -> "High Harmony"
                        11 -> "Low Harmony"
                        12 -> "Effects Return"
                        13 -> "Extra Vocal 1"
                        14 -> "Extra Vocal 2"
                        15 -> "Extra Audio"
                        else -> "Instrumental"
                    }
                )
            }
        )
    }

    var player by remember {
        mutableStateOf<MediaPlayer?>(null)
    }

    var playingTrack by remember {
        mutableIntStateOf(-1)
    }

    var isPlaying by remember {
        mutableStateOf(false)
    }

    var duration by remember {
        mutableIntStateOf(0)
    }

    var position by remember {
        mutableIntStateOf(0)
    }

    var seekRequest by remember {
        mutableIntStateOf(0)
    }

    var playerError by remember {
        mutableStateOf<String?>(null)
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val number = selectedTrack
            val name = if (number == 16) {
                "Instrumental"
            } else {
                tracks[number - 1].name
            }

            tracks = tracks.toMutableList().also {
                it[number - 1] = it[number - 1].copy(
                    name = name,
                    uri = uri
                )
            }

            message = "Audio imported to Track $number."
            playerError = null
        }
    }

    fun stopPlayback() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }

        player?.release()
        player = null
        playingTrack = -1
        isPlaying = false
        duration = 0
        position = 0
    }

    DisposableEffect(Unit) {
        onDispose {
            player?.release()
            player = null
        }
    }

    LaunchedEffect(isPlaying, playingTrack, seekRequest) {
        while (isPlaying) {
            val activePlayer = player ?: break

            try {
                position = activePlayer.currentPosition
                if (activePlayer.isPlaying.not()) {
                    isPlaying = false
                    break
                }
            } catch (_: Exception) {
                isPlaying = false
                break
            }

            delay(250)
        }
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "C.O.A.F",
                color = Purple,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "STUDIO",
                color = White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                "CRUZ OF ALL FLAVORS",
                color = Muted,
                fontSize = 11.sp,
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(20.dp))

            if (currentScreen != "home") {
                OutlinedButton(
                    onClick = { currentScreen = "home" }
                ) {
                    Text("← BACK TO STUDIO", color = White)
                }

                Spacer(Modifier.height(12.dp))
            }

            when (currentScreen) {
                "home" -> {
                    Text(
                        "YOUR MUSIC. YOUR SOUND.",
                        color = White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("Project name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            focusedBorderColor = Purple,
                            unfocusedBorderColor = Muted,
                            focusedLabelColor = Purple,
                            unfocusedLabelColor = Muted,
                            cursorColor = Purple
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            currentScreen = "tracks"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Purple
                        )
                    ) {
                        Text(
                            "🎚 OPEN 16-TRACK STUDIO",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            recording = !recording
                            message = if (recording) {
                                "Recording mode selected."
                            } else {
                                "Recording mode stopped."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (recording) {
                                Color(0xFFB82F4B)
                            } else {
                                Panel
                            }
                        )
                    ) {
                        Text(
                            if (recording) "STOP" else "● RECORD VOCALS"
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            currentScreen = "effects"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("♫ CRUZ VOCAL EFFECTS", color = White)
                    }

                    OutlinedButton(
                        onClick = {
                            currentScreen = "mix"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("MIX & MASTER", color = White)
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(message, color = Muted, fontSize = 13.sp)
                }

                "tracks" -> {
                    Text(
                        "PROJECT TRACKS",
                        color = Purple,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        projectName.ifBlank { "Untitled Project" },
                        color = Muted,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    tracks.forEach { track ->
                        val isSelected = selectedTrack == track.number

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .background(
                                    if (isSelected) {
                                        Color(0xFF30233F)
                                    } else {
                                        Panel
                                    },
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "TRACK ${
                                            track.number.toString()
                                                .padStart(2, '0')
                                        }",
                                        color = Purple,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        track.name,
                                        color = White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        if (track.uri == null) {
                                            "No audio imported"
                                        } else {
                                            "Audio loaded"
                                        },
                                        color = if (track.uri != null) {
                                            Green
                                        } else {
                                            Muted
                                        },
                                        fontSize = 11.sp
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        selectedTrack = track.number
                                        audioPicker.launch(
                                            arrayOf("audio/*")
                                        )
                                    }
                                ) {
                                    Text("IMPORT", color = Green)
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        selectedTrack = track.number
                                        if (track.uri == null) {
                                            message =
                                                "Import audio into Track ${track.number} first."
                                            return@TextButton
                                        }

                                        if (
                                            playingTrack == track.number &&
                                            isPlaying
                                        ) {
                                            player?.pause()
                                            isPlaying = false
                                        } else {
                                            stopPlayback()
                                            playerError = null

                                            try {
                                                val newPlayer = MediaPlayer()
                                                newPlayer.setAudioAttributes(
                                                    AudioAttributes.Builder()
                                                        .setUsage(
                                                            AudioAttributes.USAGE_MEDIA
                                                        )
                                                        .setContentType(
                                                            AudioAttributes.CONTENT_TYPE_MUSIC
                                                        )
                                                        .build()
                                                )

                                                newPlayer.setDataSource(
                                                    this@COAFStudioScreen
                                                        .applicationContext,
                                                    track.uri
                                                )

                                                newPlayer.setOnPreparedListener {
                                                    duration = it.duration
                                                    player = it
                                                    playingTrack = track.number
                                                    it.start()
                                                    isPlaying = true
                                                    message =
                                                        "Playing Track ${track.number}."
                                                }

                                                newPlayer.setOnCompletionListener {
                                                    isPlaying = false
                                                    position = 0
                                                }

                                                newPlayer.setOnErrorListener { mp, _, _ ->
                                                    mp.release()
                                                    player = null
                                                    isPlaying = false
                                                    playerError =
                                                        "Couldn't play this audio file."
                                                    true
                                                }

                                                newPlayer.prepareAsync()
                                                player = newPlayer
                                            } catch (e: Exception) {
                                                playerError =
                                                    "Playback error: ${
                                                        e.localizedMessage
                                                            ?: "unsupported file"
                                                    }"
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        if (
                                            playingTrack == track.number &&
                                            isPlaying
                                        ) "PAUSE" else "PLAY",
                                        color = White
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        stopPlayback()
                                        message = "Playback stopped."
                                    }
                                ) {
                                    Text("STOP", color = White)
                                }

                                TextButton(
                                    onClick = {
                                        tracks = tracks.toMutableList().also {
                                            it[track.number - 1] =
                                                it[track.number - 1].copy(
                                                    muted = !track.muted
                                                )
                                        }
                                    }
                                ) {
                                    Text(
                                        if (track.muted) "MUTED" else "MUTE",
                                        color = if (track.muted) {
                                            Color(0xFFFF5A70)
                                        } else {
                                            Muted
                                        }
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        tracks = tracks.map {
                                            it.copy(
                                                solo = it.number == track.number &&
                                                    !track.solo
                                            )
                                        }
                                    }
                                ) {
                                    Text(
                                        if (track.solo) "SOLO" else "SOLO",
                                        color = if (track.solo) Green else Muted
                                    )
                                }
                            }

                            Text(
                                "VOLUME  ${(track.volume * 100).toInt()}%",
                                color = Muted,
                                fontSize = 11.sp
                            )

                            Slider(
                                value = track.volume,
                                onValueChange = { value ->
                                    tracks = tracks.toMutableList().also {
                                        it[track.number - 1] =
                                            it[track.number - 1].copy(
                                                volume = value
                                            )
                                    }

                                    if (playingTrack == track.number) {
                                        try {
                                            player?.setVolume(value, value)
                                        } catch (_: Exception) {
                                        }
                                    }
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Purple,
                                    activeTrackColor = Purple
                                )
                            )
                        }
                    }

                    if (playingTrack >= 0 && duration > 0) {
                        Spacer(Modifier.height(12.dp))

                        Text(
                            "PLAYBACK POSITION",
                            color = Purple,
                            fontWeight = FontWeight.Bold
                        )

                        Slider(
                            value = position.coerceIn(0, duration).toFloat(),
                            onValueChange = {
                                position = it.toInt()
                            },
                            onValueChangeFinished = {
                                try {
                                    player?.seekTo(position)
                                    seekRequest++
                                } catch (_: Exception) {
                                }
                            },
                            valueRange = 0f..duration.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Purple,
                                activeTrackColor = Purple
                            )
                        )

                        Text(
                            "${formatTime(position)} / ${formatTime(duration)}",
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }

                    playerError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = Color(0xFFFF5A70))
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        "Note: Tracks can be imported and played individually. " +
                            "Synchronized multitrack mixing is a future stage.",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }

                "effects" -> {
                    Text(
                        "CRUZ PLUGIN SUITE",
                        color = Purple,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    listOf(
                        "CRUZ REVERB" to "Room size • Decay • Wet/Dry",
                        "CRUZ MELODYNE" to "Graphical pitch editing",
                        "CRUZ AUTOTUNE" to "Key • Scale • Retune speed",
                        "CRUZ COMPRESSOR" to "Dynamics and vocal control",
                        "CRUZ OPTOCOMP" to "Smooth vocal levelling",
                        "CRUZ FET COMP" to "Punchy vocal compression",
                        "CRUZ BUS COMP" to "Glue and cohesion",
                        "CRUZ DELAY" to "Time • Feedback • Stereo",
                        "CRUZ EQ" to "Frequency and tone shaping"
                    ).forEach { (name, description) ->
                        PluginCard(name, description)
                    }

                    Text(
                        "Audio processing for these plugins is still to be implemented.",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }

                "mix" -> {
                    Text(
                        "MIX & MASTER",
                        color = Purple,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    PluginCard(
                        "16-TRACK PROJECT",
                        "Open the track list to import and play audio."
                    )

                    Button(
                        onClick = {
                            currentScreen = "tracks"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Purple
                        )
                    ) {
                        Text("OPEN TRACKS")
                    }

                    Text(
                        "Synchronized mixing and audio export are not implemented yet.",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "MADE FOR THE KING OF ALL FLAVORS 👑",
                color = Purple,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

private fun formatTime(milliseconds: Int): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun PluginCard(
    name: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(Panel, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(
            name,
            color = Purple,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            description,
            color = Muted,
            fontSize = 12.sp
        )
    }
}
