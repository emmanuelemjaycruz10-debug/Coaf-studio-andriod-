package com.emjaycruz.coaf

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
    var duration by mutableIntStateOf(0)
    var position by mutableIntStateOf(0)
    var volume by mutableFloatStateOf(1f)
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
            repeat(16) { add(StudioTrack(it + 1)) }
        }
    }

    var selectedTrack by remember { mutableIntStateOf(1) }
    var currentTab by remember { mutableStateOf("TRACKS") }
    var projectName by remember { mutableStateOf("My COAF Project") }
    var isPlaying by remember { mutableStateOf(false) }
    var projectStatus by remember { mutableStateOf("Ready") }

    fun notifyUser(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun pauseAll() {
        tracks.forEach { track ->
            try {
                track.player?.pause()
            } catch (_: Exception) {
            }
            track.playing = false
        }
        isPlaying = false
        projectStatus = "Paused"
    }

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
        isPlaying = false
        projectStatus = "Stopped"
    }

    fun startPlayback() {
        val hasSolo = tracks.any { it.loaded && it.solo }

        val playable = tracks.filter {
            it.loaded && !it.muted && (!hasSolo || it.solo)
        }

        if (playable.isEmpty()) {
            notifyUser("Import audio into a track first.")
            return
        }

        playable.forEach { track ->
            try {
                val player = track.player ?: return@forEach

                if (track.position >= track.duration &&
                    track.duration > 0
                ) {
                    player.seekTo(0)
                    track.position = 0
                }

                player.start()
                track.playing = true
            } catch (_: Exception) {
            }
        }

        isPlaying = true
        projectStatus = "Playing"
    }

    fun updateTrackVolume(track: StudioTrack, value: Float) {
        track.volume = value
        val gain = if (track.muted) 0f else value

        try {
            track.player?.setVolume(gain, gain)
        } catch (_: Exception) {
        }
    }

    fun toggleMute(track: StudioTrack) {
        track.muted = !track.muted
        val gain = if (track.muted) 0f else track.volume

        try {
            track.player?.setVolume(gain, gain)
        } catch (_: Exception) {
        }
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
                track.loaded = false

                val player = MediaPlayer()

                player.setOnPreparedListener { prepared ->
                    track.player = prepared
                    track.uri = uri
                    track.duration = prepared.duration.coerceAtLeast(0)
                    track.position = 0
                    track.name = uri.lastPathSegment
                        ?.substringAfterLast('/')
                        ?.take(32)
                        ?: "Imported Audio"
                    track.loaded = true

                    val gain = if (track.muted) 0f else track.volume
                    prepared.setVolume(gain, gain)

                    notifyUser("Audio loaded into Track ${track.number}")
                }

                player.setOnCompletionListener {
                    track.playing = false
                    track.position = track.duration
                }

                player.setOnErrorListener { _, _, _ ->
                    track.playing = false
                    track.loaded = false
                    notifyUser("This audio file could not be played.")
                    true
                }

                player.setDataSource(context, uri)
                player.prepareAsync()
            } catch (e: Exception) {
                notifyUser(
                    "Could not load audio: ${e.localizedMessage ?: "unknown error"}"
                )
            }
        }
    }

    val saveProjectLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val json = JSONObject()
                json.put("projectName", projectName)

                val array = JSONArray()

                tracks.forEach { track ->
                    val item = JSONObject()
                    item.put("number", track.number)
                    item.put("name", track.name)
                    item.put("uri", track.uri?.toString() ?: "")
                    item.put("volume", track.volume.toDouble())
                    item.put("muted", track.muted)
                    item.put("solo", track.solo)
                    array.put(item)
                }

                json.put("tracks", array)

                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(json.toString(2).toByteArray(Charsets.UTF_8))
                }

                projectStatus = "Project saved"
                notifyUser("Project file saved.")
            } catch (e: Exception) {
                notifyUser("Save failed: ${e.localizedMessage ?: "unknown error"}")
            }
        }
    }

    val loadProjectLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: throw IllegalStateException("Unable to read project")

                val json = JSONObject(text)
                projectName = json.optString("projectName", "My COAF Project")
                val array = json.optJSONArray("tracks") ?: JSONArray()

                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val number = item.optInt("number", -1)

                    if (number !in 1..16) continue

                    val track = tracks[number - 1]
                    track.name = item.optString("name", track.name)
                    track.volume = item.optDouble("volume", 1.0)
                        .toFloat().coerceIn(0f, 1f)
                    track.muted = item.optBoolean("muted", false)
                    track.solo = item.optBoolean("solo", false)

                    val savedUri = item.optString("uri", "")

                    if (savedUri.isNotBlank()) {
                        try {
                            val audioUri = Uri.parse(savedUri)

                            track.player?.release()
                            track.player = null
                            track.loaded = false

                            val player = MediaPlayer()

                            player.setOnPreparedListener { prepared ->
                                track.player = prepared
                                track.uri = audioUri
                                track.duration = prepared.duration.coerceAtLeast(0)
                                track.position = 0
                                track.loaded = true

                                val gain = if (track.muted) 0f else track.volume
                                prepared.setVolume(gain, gain)
                            }

                            player.setOnErrorListener { _, _, _ ->
                                track.loaded = false
                                true
                            }

                            player.setDataSource(context, audioUri)
                            player.prepareAsync()
                        } catch (_: Exception) {
                            track.loaded = false
                            notifyUser("A saved audio file could not be reopened.")
                        }
                    }
                }

                projectStatus = "Project loaded"
                notifyUser("Project settings loaded.")
            } catch (e: Exception) {
                notifyUser(
                    "Could not load project: ${e.localizedMessage ?: "invalid file"}"
                )
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

    LaunchedEffect(tracks) {
        while (true) {
            tracks.forEach { track ->
                try {
                    val player = track.player
                    if (player != null && track.playing && player.isPlaying) {
                        track.position = player.currentPosition
                    }
                } catch (_: Exception) {
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PanelBackground)
                .padding(18.dp)
        ) {
            Text(
                "COAF STUDIO",
                color = LightPurple,
                fontSize = 25.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "CRUZ OF ALL FLAVORS",
                color = TextWhite,
                fontSize = 11.sp,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "A studio for the King of All Flavors 👑",
                color = MutedText,
                fontSize = 12.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("PROJECT", color = MutedText, fontSize = 10.sp)
                Text(
                    projectName,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            OutlinedButton(onClick = {
                projectName = "COAF Project ${System.currentTimeMillis() % 10000}"
                projectStatus = "New project"
                notifyUser("New project created.")
            }) {
                Text("NEW", color = LightPurple)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBackground)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    "TRANSPORT",
                    color = LightPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { startPlayback() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple)
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
                    projectStatus,
                    color = if (isPlaying) TrackGreen else MutedText,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("TRACKS", "MIXER", "PLUGINS", "PROJECT").forEach { tab ->
                Button(
                    onClick = { currentTab = tab },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentTab == tab) Purple else PanelBackground
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
                    Column(Modifier.weight(1f)) {
                        Text(
                            "MULTITRACK WORKSPACE",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            "Selected: Track $selectedTrack",
                            color = MutedText,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = { audioPicker.launch(arrayOf("audio/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) {
                        Text("+ AUDIO")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    items(items = tracks, key = { it.number }) { track ->
                        TrackCard(
                            track = track,
                            selected = selectedTrack == track.number,
                            onSelect = { selectedTrack = track.number },
                            onMute = { toggleMute(track) },
                            onSolo = { track.solo = !track.solo },
                            onVolumeChange = { value ->
                                updateTrackVolume(track, value)
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
                    Text("Individual track levels", color = MutedText)
                    Spacer(Modifier.height(10.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = tracks.filter { it.loaded },
                            key = { it.number }
                        ) { track ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = PanelBackground
                                )
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${track.number}. ${track.name}",
                                      
