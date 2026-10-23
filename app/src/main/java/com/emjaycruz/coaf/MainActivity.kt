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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
private val Red = Color(0xFFFF5A70)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            COAFStudioScreen()
        }
    }
}

private data class AudioTrack(
    val number: Int,
    val name: String,
    val uri: Uri? = null,
    val muted: Boolean = false,
    val solo: Boolean = false,
    val volume: Float = 1f
)

@Composable
fun COAFStudioScreen() {

    // Correct context reference for loading audio.
    val context = LocalContext.current

    var projectName by remember {
        mutableStateOf("My First Recording")
    }

    var currentScreen by remember {
        mutableStateOf("home")
    }

    var message by remember {
        mutableStateOf("Your studio is ready.")
    }

    var tracks by remember {
        mutableStateOf(
            (1..16).map { number ->
                AudioTrack(
                    number = number,
                    name = if (number == 16) {
                        "Instrumental / Beat"
                    } else {
                        "Audio Track $number"
                    }
                )
            }
        )
    }

    var player by remember {
        mutableStateOf<MediaPlayer?>(null)
    }

    var playingTrack by remember {
        mutableIntStateOf(0)
    }

    var isPlaying by remember {
        mutableStateOf(false)
    }

    var playbackPosition by remember {
        mutableLongStateOf(0L)
    }

    var playbackDuration by remember {
        mutableLongStateOf(0L)
    }

    var seekPosition by remember {
        mutableFloatStateOf(0f)
    }

    var isSeeking by remember {
        mutableStateOf(false)
    }

    var selectedImportTrack by remember {
        mutableIntStateOf(16)
    }

    fun updateTrack(
        number: Int,
        update: (AudioTrack) -> AudioTrack
    ) {
        tracks = tracks.map { track ->
            if (track.number == number) update(track) else track
        }
    }

    fun stopPlayback() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }

        try {
            player?.release()
        } catch (_: Exception) {
        }

        player = null
        playingTrack = 0
        isPlaying = false
        playbackPosition = 0L
        playbackDuration = 0L
        seekPosition = 0f
        isSeeking = false
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val trackNumber = selectedImportTrack

            updateTrack(trackNumber) { oldTrack ->
                oldTrack.copy(uri = uri)
            }

            message = if (trackNumber == 16) {
                "Instrumental imported into Track 16."
            } else {
                "Audio imported into Track $trackNumber."
            }
        }
    }

    // Keep the playback position updated while audio is playing.
    LaunchedEffect(isPlaying, player) {
        while (isPlaying) {
            val activePlayer = player

            if (activePlayer != null) {
                try {
                    if (activePlayer.isPlaying && !isSeeking) {
                        playbackPosition =
                            activePlayer.currentPosition.toLong()

                        playbackDuration =
                            activePlayer.duration.toLong()

                        if (playbackDuration > 0L) {
                            seekPosition =
                                playbackPosition.toFloat() /
                                playbackDuration.toFloat()
                        }
                    }
                } catch (_: Exception) {
                    // The player may have finished or been released.
                }
            }

            delay(250)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                player?.release()
            } catch (_: Exception) {
            }
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Purple,
            background = Background,
            surface = Panel
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {

            Text(
                text = "C.O.A.F",
                color = Purple,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "STUDIO",
                color = White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "CRUZ OF ALL FLAVORS",
                color = Muted,
                fontSize = 11.sp,
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(24.dp))

            if (currentScreen != "home") {
                OutlinedButton(
                    onClick = {
                        currentScreen = "home"
                    }
                ) {
                    Text(
                        "← BACK TO STUDIO",
                        color = White
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            when (currentScreen) {

                "home" -> {

                    Text(
                        "YOUR MUSIC. YOUR SOUND.",
                        color = White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = projectName,
                        onValueChange = {
                            projectName = it
                        },
                        label = {
                            Text("Project name")
                        },
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

                    Spacer(Modifier.height(20.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Panel,
                                RoundedCornerShape(18.dp)
                            )
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "COAF WORKSPACE",
                            color = Purple,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "▂ ▅ ▃ ▇ ▄ ▆ ▂ ▇ ▃ ▅",
                            color = Purple,
                            fontSize = 23.sp
                        )

                        Text(
                            "16-track audio workspace",
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = {
                            currentScreen = "tracks"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            "♫  OPEN 16-TRACK WORKSPACE",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            selectedImportTrack = 16
                            audioPicker.launch(
                                arrayOf("audio/*")
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            "♫  IMPORT INSTRUMENTAL / MP3",
                            color = White
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            currentScreen = "effects"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CRUZ VOCAL EFFECTS")
                    }

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = {
                            currentScreen = "mix"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("MIX & MASTER")
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        message,
                        color = Muted,
                        fontSize = 13.sp
                    )
                }

                "tracks" -> {

                    Text(
                        "MULTITRACK WORKSPACE",
                        color = Purple,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        projectName.ifBlank {
                            "Untitled Project"
                        },
                        color = White,
                        fontSize = 15.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    // Master playback controls.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Panel,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {

                        Text(
                            if (isPlaying) {
                                "PLAYING TRACK $playingTrack"
                            } else {
                                "AUDIO PLAYER"
                            },
                            color = Green,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(12.dp))

                        Slider(
                            value = seekPosition.coerceIn(0f, 1f),
                            onValueChange = { value ->
                                seekPosition = value
                                isSeeking = true
                            },
                            onValueChangeFinished = {
                                val activePlayer = player

                                if (activePlayer != null &&
                                    playbackDuration > 0L
                                ) {
                                    try {
                                        activePlayer.seekTo(
                                            (
                                                seekPosition *
                                                playbackDuration
                                            ).toInt()
                                        )

                                        playbackPosition =
                                            activePlayer.currentPosition
                                                .toLong()
                                    } catch (_: Exception) {
                                    }
                                }

                                isSeeking = false
                            },
                            enabled = playbackDuration > 0L,
                            colors = SliderDefaults.colors(
                                thumbColor = Purple,
                                activeTrackColor = Purple
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {
                            Text(
                                formatTime(playbackPosition),
                                color = Muted,
                                fontSize = 11.sp
                            )

                            Text(
                                formatTime(playbackDuration),
                                color = Muted,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                stopPlayback()
                                message = "Playback stopped."
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "STOP PLAYBACK",
                                color = White
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        "TRACKS 1–15: VOCALS & OTHER AUDIO",
                        color = Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(10.dp))

                    tracks.forEach { track ->

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .background(
                                    Panel,
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(14.dp)
                        ) {

                            Text(
                                "${track.number}. ${track.name}",
                                color = if (track.number == 16) {
                                    Green
                                } else {
                                    Purple
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                if (track.uri != null) {
                                    "Audio file imported"
                                } else {
                                    "No audio imported"
                                },
                                color = Muted,
                                fontSize = 11.sp
                            )

                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {

                                Button(
                                    onClick = {
                                        selectedImportTrack =
                                            track.number

                                        audioPicker.launch(
                                            arrayOf("audio/*")
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "IMPORT",
                                        fontSize = 10.sp
                                    )
                                }

                                Button(
                                    onClick = {
                                        val uri = track.uri

                                        if (uri == null) {
                                            message =
                                                "Import audio into Track " +
                                                "${track.number} first."
                                        } else {
                                            try {
                                                if (
                                                    playingTrack ==
                                                        track.number &&
                                                    player != null
                                                ) {
                                                    if (isPlaying) {
                                                        player?.pause()
                                                        isPlaying = false
                                                        message =
                                                            "Playback paused."
                                                    } else {
                                                        player?.start()
                                                        isPlaying = true
                                                        message =
                                                        
