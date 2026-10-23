package com.emjaycruz.coaf

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

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
    var recording by mutableStateOf(false)
    var player: MediaPlayer? = null
    var recorder: MediaRecorder? = null
    var recordingFile: File? = null
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
    var pendingRecordingTrack by remember { mutableStateOf<StudioTrack?>(null) }

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
            notifyUser("Import audio or record a track first.")
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

                if (!player.isPlaying) player.start()
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

    fun loadRecordedAudio(track: StudioTrack, file: File) {
        try {
            val uri = Uri.fromFile(file)

            track.player?.release()
            track.player = null
            track.loaded = false

            val player = MediaPlayer()

            player.setOnPreparedListener { prepared ->
                track.player = prepared
                track.uri = uri
                track.duration = prepared.duration.coerceAtLeast(0)
                track.position = 0
                track.name = file.nameWithoutExtension.take(32)
                track.loaded = true
                track.playing = false

                val gain = if (track.muted) 0f else track.volume
                prepared.setVolume(gain, gain)

                notifyUser("Recording saved to Track ${track.number}.")
            }

            player.setOnCompletionListener {
                track.playing = false
                track.position = track.duration
            }

            player.setOnErrorListener { _, _, _ ->
                track.loaded = false
                notifyUser(
                    "Recording saved, but could not be loaded for playback."
                )
                true
            }

            player.setDataSource(file.absolutePath)
            player.prepareAsync()
        } catch (e: Exception) {
            notifyUser(
                "Could not load recording: ${e.localizedMessage ?: "unknown error"}"
            )
        }
    }

    fun startRecordingNow(track: StudioTrack) {
        if (track.number == 16) {
            notifyUser("Track 16 is reserved for your instrumental.")
            return
        }

        if (tracks.any { it.recording }) {
            notifyUser(
                "Stop the current recording before recording another track."
            )
            return
        }

        try {
            track.player?.pause()
            track.player?.release()
            track.player = null
            track.loaded = false
            track.uri = null
            track.position = 0
            track.duration = 0

            val recordingDir = File(
                context.getExternalFilesDir(null),
                "COAF_Recordings"
            )

            if (!recordingDir.exists() && !recordingDir.mkdirs()) {
                throw IllegalStateException(
                    "Could not create recording folder"
                )
            }

            val file = File(
                recordingDir,
                "COAF_Track_${track.number}_${System.currentTimeMillis()}.m4a"
            )

            val recorder = MediaRecorder(context)
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(192000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(file.absolutePath)

            recorder.setOnErrorListener { _, _, _ ->
                track.recording = false

                try {
                    track.recorder?.release()
                } catch (_: Exception) {
                }

                track.recorder = null
                notifyUser(
                    "Recording stopped because the recorder reported an error."
                )
            }

            recorder.prepare()
            recorder.start()

            track.recorder = recorder
            track.recordingFile = file
            track.recording = true
            selectedTrack = track.number
            projectStatus = "Recording Track ${track.number}"

            notifyUser("Recording started on Track ${track.number}.")
        } catch (e: Exception) {
            try {
                track.recorder?.release()
            } catch (_: Exception) {
            }

            track.recorder = null
            track.recording = false

            notifyUser(
                "Could not start recording: ${e.localizedMessage ?: "check microphone permission"}"
            )
        }
    }

    fun stopRecording(track: StudioTrack) {
        if (!track.recording) return

        val file = track.recordingFile

        try {
            track.recorder?.stop()
        } catch (_: Exception) {
            notifyUser(
                "Recording was too short or could not be finalized."
            )
        } finally {
            try {
                track.recorder?.reset()
            } catch (_: Exception) {
            }

            try {
                track.recorder?.release()
            } catch (_: Exception) {
            }

            track.recorder = null
            track.recording = false
        }

        projectStatus = "Recording stopped"

        if (file != null && file.exists() && file.length() > 1000L) {
            loadRecordedAudio(track, file)
        } else {
            notifyUser(
                "No usable audio was saved. Record for a little longer and try again."
            )
        }
    }
        val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val track = pendingRecordingTrack
        pendingRecordingTrack = null

        if (granted && track != null) {
            startRecordingNow(track)
        } else if (!granted) {
            notifyUser("Microphone permission is needed to record audio.")
        }
    }

    fun requestRecording(track: StudioTrack) {
        if (track.number == 16) {
            notifyUser("Track 16 is reserved for your instrumental.")
            return
        }

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startRecordingNow(track)
        } else {
            pendingRecordingTrack = track
            microphonePermissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val track = tracks[selectedTrack - 1]

            if (track.recording) {
                notifyUser(
                    "Stop recording before importing audio into this track."
                )
                return@rememberLauncherForActivityResult
            }

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
                    it.write(
                        json.toString(2).toByteArray(Charsets.UTF_8)
                    )
                }

                projectStatus = "Project saved"
                notifyUser("Project file saved.")
            } catch (e: Exception) {
                notifyUser(
                    "Save failed: ${e.localizedMessage ?: "unknown error"}"
                )
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
                projectName = json.optString(
                    "projectName",
                    "My COAF Project"
                )

                val array = json.optJSONArray("tracks") ?: JSONArray()

                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val number = item.optInt("number", -1)

                    if (number !in 1..16) continue

                    val track = tracks[number - 1]

                    track.name = item.optString("name", track.name)
                    track.volume = item.optDouble("volume", 1.0)
                        .toFloat()
                        .coerceIn(0f, 1f)
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
                                track.duration =
                                    prepared.duration.coerceAtLeast(0)
                                track.position = 0
                                track.loaded = true

                                val gain =
                                    if (track.muted) 0f else track.volume
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
                            notifyUser(
                                "A saved audio file could not be reopened."
                            )
                        }
                    } else {
                        track.uri = null
                        track.loaded = false
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
                    track.recorder?.stop()
                } catch (_: Exception) {
                }

                try {
                    track.recorder?.release()
                } catch (_: Exception) {
                }

                track.recorder = null

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

                    if (player != null &&
                        track.playing &&
                        player.isPlaying
                    ) {
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
                    projectName =
                        "COAF Project ${System.currentTimeMillis() % 10000}"
                    projectStatus = "New project"
                    notifyUser("New project created.")
                }
            ) {
                Text("NEW", color = LightPurple)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = PanelBackground
            )
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
                    if (tracks.any { it.recording }) {
                        "Recording in progress…"
                    } else {
                        projectStatus
                    },
                    color = if (
                        isPlaying || tracks.any { it.recording }
                    ) TrackGreen else MutedText,
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
                        containerColor = if (currentTab == tab) {
                            Purple
                        } else {
                            PanelBackground
                        }
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
                        onClick = {
                            audioPicker.launch(arrayOf("audio/*"))
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
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    contentPadding = PaddingValues(bottom = 18.dp)
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
                                toggleMute(track)
                            },
                            onSolo = {
                                track.solo = !track.solo
                            },
                            onVolumeChange = { value ->
                                updateTrackVolume(track, value)
                            },
                            onRecord = {
                                requestRecording(track)
                            },
                            onStopRecording = {
                                stopRecording(track)
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

                    Text(
                        "Individual track levels",
                        color = MutedText
                    )

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
                                    Row(
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${track.number}. ${track.name}",
                                            color = TextWhite,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Text(
                                            "${(track.volume * 100).toInt()}%",
                                            color = LightPurple
                                        )
                                    }

                                    Slider(
                                        value = track.volume,
                                        onValueChange = { value ->
                                            updateTrackVolume(track, value)
                                        },
                                        valueRange = 0f..1f
                                    )

                                    Row(
                                        horizontalArrangement =
                                            Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                toggleMute(track)
                                            }
                                        ) {
                                            Text(
                                                if (track.muted) {
                                                    "UNMUTE"
                                                } else {
                                                    "MUTE"
                                                }
                                                      "PLUGINS" -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    item {
                        Text(
                            "CRUZ PLUGINS",
                            color = LightPurple,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            "These audio-processing plugins are planned, " +
                                "but are not implemented yet.",
                            color = MutedText,
                            fontSize = 12.sp
                        )
                    }

                    items(
                        items = listOf(
                            "CRUZ Reverb",
                            "CRUZ Melodyne — Pitch Editor",
                            "CRUZ AutoTune",
                            "CRUZ Compressor",
                            "OptoComp",
                            "FET Comp",
                            "Bus Comp",
                            "CRUZ Delay",
                            "CRUZ EQ"
                        )
                    ) { pluginName ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = PanelBackground
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    pluginName,
                                    color = TextWhite,
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    "PLANNED",
                                    color = MutedText,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            "PROJECT" -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "PROJECT FILES",
                        color = LightPurple,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Save or reopen your project settings.",
                        color = MutedText
                    )

                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("Project name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedLabelColor = LightPurple,
                            unfocusedLabelColor = MutedText
                        )
                    )

                    Button(
                        onClick = {
                            val safeName = projectName.replace(
                                Regex("[^A-Za-z0-9_-]"),
                                "_"
                            )
                            saveProjectLauncher.launch("$safeName.json")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Purple
                        )
                    ) {
                        Text("SAVE PROJECT")
                    }

                    OutlinedButton(
                        onClick = {
                            loadProjectLauncher.launch(
                                arrayOf("application/json", "text/*")
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OPEN PROJECT", color = TextWhite)
                    }

                    Text(
                        "Previously imported audio must remain accessible " +
                            "for its saved reference to work. New recordings are stored inside the app's private files.",
                        color = MutedText,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackCard(
    track: StudioTrack,
    selected: Boolean,
    onSelect: () -> Unit,
    onMute: () -> Unit,
    onSolo: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onRecord: () -> Unit,
    onStopRecording: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                Color(0xFF302044)
            } else {
                PanelBackground
            }
        ),
        border = if (selected) {
            BorderStroke(1.dp, Purple)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (track.number == 16) {
                                Color(0xFF3B2A12)
                            } else {
                                Color(0xFF35234A)
                            },
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        track.number.toString().padStart(2, '0'),
                        color = if (track.number == 16) {
                            Color(0xFFFFD27D)
                        } else {
                            LightPurple
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        track.name,
                        color = TextWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )

                    Text(
                        when {
                            track.recording -> "● RECORDING"
                            track.loaded -> "${formatTime(track.position)} / ${formatTime(track.duration)}"
                            track.number == 16 -> "Import your beat here"
                            else -> "Empty track • tap to select"
                        },
                        color = if (track.recording) {
                            Color(0xFFFF7373)
                        } else {
                            MutedText
                        },
                        fontSize = 11.sp
                    )
                }

                if (track.recording) {
                    Text(
                        "REC",
                        color = Color(0xFFFF7373),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (track.loaded) {
                    Text(
                        "READY",
                        color = TrackGreen,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "VOL",
                    color = MutedText,
                    fontSize = 10.sp
                )

                Slider(
                    value = track.volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    "${(track.volume * 100).toInt()}%",
                    color = LightPurple,
                    fontSize = 10.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onMute) {
                    Text(if (track.muted) "UNMUTE" else "MUTE")
                }

                OutlinedButton(onClick = onSolo) {
                    Text(if (track.solo) "UNSOLO" else "SOLO")
                }

                if (track.number != 16) {
                    if (track.recording) {
                        Button(
                            onClick = onStopRecording,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFB3261E)
                            )
                        ) {
                            Text("■ STOP REC")
                        }
                    } else {
                        Button(
                            onClick = onRecord,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFB3261E)
                            )
                        ) {
                            Text("● REC")
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Int): String {
    val totalSeconds = milliseconds.coerceAtLeast(0) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "%d:%02d".format(minutes, seconds)
}      


                                                
                                  
