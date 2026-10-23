package com.emjaycruz.coaf

import android.Manifest
import android.content.Context
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
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
private val RecordRed = Color(0xFFB3261E)

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
            repeat(16) {
                add(StudioTrack(it + 1))
            }
        }
    }

    var selectedTrack by remember { mutableIntStateOf(1) }
    var currentTab by remember { mutableStateOf("TRACKS") }
    var projectName by remember { mutableStateOf("My COAF Project") }
    var projectStatus by remember { mutableStateOf("Ready") }
    var isPlaying by remember { mutableStateOf(false) }

    fun notifyUser(message: String) {
        Toast.makeText(
            context,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    fun stopPlayer(track: StudioTrack, reset: Boolean = false) {
        try {
            track.player?.let { player ->
                if (player.isPlaying) player.pause()
                if (reset) player.seekTo(0)
            }
        } catch (_: Exception) {
        }

        track.playing = false

        if (reset) track.position = 0
    }

    fun stopAll() {
        tracks.forEach { track ->
            stopPlayer(track, reset = true)
        }

        isPlaying = false
        projectStatus = "Stopped"
    }

    fun pauseAll() {
        tracks.forEach { track ->
            stopPlayer(track)
        }

        isPlaying = false
        projectStatus = "Paused"
    }

    fun applyVolume(track: StudioTrack) {
        val gain = if (track.muted) 0f else track.volume

        try {
            track.player?.setVolume(gain, gain)
        } catch (_: Exception) {
        }
    }

    fun loadAudio(
        track: StudioTrack,
        uri: Uri,
        displayName: String
    ) {
        try {
            track.player?.release()
            track.player = null
            track.loaded = false

            val player = MediaPlayer()

            player.setOnPreparedListener { prepared ->
                track.player = prepared
                track.uri = uri
                track.duration = prepared.duration.coerceAtLeast(0)
                track.position = 0
                track.name = displayName.take(45)
                track.loaded = true
                track.playing = false

                applyVolume(track)

                notifyUser(
                    "Audio loaded on Track ${track.number}."
                )
            }

            player.setOnCompletionListener {
                track.playing = false
                track.position = track.duration
            }

            player.setOnErrorListener { _, _, _ ->
                track.loaded = false
                track.playing = false

                notifyUser("Could not play this audio file.")
                true
            }

            player.setDataSource(context, uri)
            player.prepareAsync()

        } catch (e: Exception) {
            notifyUser(
                "Audio error: ${e.localizedMessage ?: "unknown error"}"
            )
        }
    }

    fun startPlayback() {

        val hasSolo = tracks.any { it.loaded && it.solo }

        val playable = tracks.filter {
            it.loaded &&
                !it.muted &&
                (!hasSolo || it.solo)
        }

        if (playable.isEmpty()) {
            notifyUser("Import audio or record a track first.")
            return
        }

        playable.forEach { track ->
            try {
                val player = track.player ?: return@forEach

                if (
                    track.duration > 0 &&
                    track.position >= track.duration
                ) {
                    player.seekTo(0)
                    track.position = 0
                }

                if (!player.isPlaying) {
                    player.start()
                }

                track.playing = true

            } catch (_: Exception) {
            }
        }

        isPlaying = true
        projectStatus = "Playing"
    }

    fun loadRecording(track: StudioTrack, file: File) {
        loadAudio(
            track = track,
            uri = Uri.fromFile(file),
            displayName = file.nameWithoutExtension
        )
    }

    fun startRecording(track: StudioTrack) {

        if (track.number == 16) {
            notifyUser(
                "Track 16 is reserved for your instrumental."
            )
            return
        }

        if (tracks.any { it.recording }) {
            notifyUser(
                "Stop the current recording first."
            )
            return
        }

        try {
            track.player?.release()
            track.player = null
            track.loaded = false
            track.uri = null
            track.position = 0
            track.duration = 0

            val directory = File(
                context.getExternalFilesDir(null),
                "COAF_Recordings"
            )

            if (!directory.exists() && !directory.mkdirs()) {
                throw IllegalStateException(
                    "Could not create recording folder."
                )
            }

            val file = File(
                directory,
                "COAF_Track_${track.number}_${System.currentTimeMillis()}.m4a"
            )

            val recorder = MediaRecorder(context)

            recorder.setAudioSource(
                MediaRecorder.AudioSource.MIC
            )

            recorder.setOutputFormat(
                MediaRecorder.OutputFormat.MPEG_4
            )

            recorder.setAudioEncoder(
                MediaRecorder.AudioEncoder.AAC
            )

            recorder.setAudioEncodingBitRate(192000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(file.absolutePath)

            recorder.prepare()
            recorder.start()

            track.recorder = recorder
            track.recordingFile = file
            track.recording = true

            selectedTrack = track.number
            projectStatus = "Recording Track ${track.number}"

            notifyUser(
                "Recording started on Track ${track.number}."
            )

        } catch (e: Exception) {

            try {
                track.recorder?.release()
            } catch (_: Exception) {
            }

            track.recorder = null
            track.recording = false

            notifyUser(
                "Recording failed: ${
                    e.localizedMessage ?: "check microphone permission"
                }"
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
                "Recording was too short to save correctly."
            )
        } finally {

            try {
                track.recorder?.release()
            } catch (_: Exception) {
            }

            track.recorder = null
            track.recording = false
        }

        projectStatus = "Recording stopped"

        if (file != null && file.exists() && file.length() > 1000L) {
            loadRecording(track, file)
        } else {
            notifyUser(
                "No usable recording was saved. Try recording longer."
            )
        }
    }

    fun saveProject(uri: Uri) {

        try {
            val root = JSONObject()

            root.put("projectName", projectName)

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

            root.put("tracks", array)

            context.contentResolver.openOutputStream(uri)?.use {
                it.write(root.toString(2).toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException(
                "Could not write project file."
            )

            notifyUser("Project saved.")

        } catch (e: Exception) {
            notifyUser(
                "Save failed: ${e.localizedMessage ?: "unknown error"}"
            )
        }
    }

    fun openProject(uri: Uri) {

        try {
            val json = context.contentResolver
                .openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: throw IllegalStateException(
                    "Could not read project file."
                )

            val root = JSONObject(json)

            projectName = root.optString(
                "projectName",
                "My COAF Project"
            )

            val array = root.optJSONArray("tracks")
                ?: JSONArray()

            for (i in 0 until array.length()) {

                val item = array.optJSONObject(i) ?: continue
                val number = item.optInt("number", -1)

                val track = tracks.firstOrNull {
                    it.number == number
                } ?: continue

                track.name = item.optString(
                    "name",
                    "Audio Track $number"
                )

                track.volume = item.optDouble(
                    "volume",
                    1.0
                ).toFloat().coerceIn(0f, 1f)

                track.muted = item.optBoolean("muted", false)
                track.solo = item.optBoolean("solo", false)

                val uriText = item.optString("uri", "")

                if (uriText.isNotBlank()) {
                    try {
                        val audioUri = Uri.parse(uriText)

                        loadAudio(
                            track,
                            audioUri,
                            track.name
                        )

                    } catch (_: Exception) {
                        track.loaded = false
                    }
                }
            }

            notifyUser(
                "Project settings opened. Check audio access."
            )

        } catch (e: Exception) {
            notifyUser(
                "Open failed: ${e.localizedMessage ?: "invalid project"}"
            )
        }
    }

    val importLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                try {
                    context.contentResolver
                        .takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                } catch (_: Exception) {
                }

                val track = tracks.firstOrNull {
                    it.number == selectedTrack
                }

                if (track != null) {
                    loadAudio(
                        track,
                        uri,
                        "Imported Track ${track.number}"
                    )
                }
            }
        }

    val saveProjectLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(
                "application/json"
            )
        ) { uri ->
            if (uri != null) saveProject(uri)
        }

    val openProjectLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) openProject(uri)
        }

    var pendingRecordingTrack by remember {
        mutableStateOf<StudioTrack?>(null)
    }

    val microphonePermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            val track = pendingRecordingTrack
            pendingRecordingTrack = null

            if (granted && track != null) {
                startRecording(track)
            } else {
                notifyUser(
                    "Microphone permission is required to record."
                )
            }
        }

    fun requestRecording(track: StudioTrack) {

        val permission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        )

        if (permission == PackageManager.PERMISSION_GRANTED) {
            startRecording(track)
        } else {
            pendingRecordingTrack = track

            microphonePermissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {

            tracks.forEach { track ->

                try {
                    if (track.recording) {
                        track.recorder?.stop()
                    }
                } catch (_: Exception) {
                }

                try {
                    track.recorder?.release()
                } catch (_: Exception) {
                }

                try {
                    track.player?.release()
                } catch (_: Exception) {
                }

                track.recording = false
                track.recorder = null
                track.player = null
            }
        }
    }

    LaunchedEffect(isPlaying) {

        while (isPlaying) {

            tracks.forEach { track ->

                try {
                    val player = track.player

                    if (player != null && player.isPlaying) {
                        track.position = player.currentPosition
                    } else if (track.playing) {
                        track.playing = false
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
                .padding(16.dp)
        ) {

            Text(
                "COAF STUDIO",
                color = LightPurple,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                "CRUZ OF ALL FLAVORS",
                color = MutedText,
                fontSize = 11.sp,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(12.dp))

            Text(
                projectName,
                color = TextWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                projectStatus,
                color = TrackGreen,
                fontSize = 12.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            listOf(
                "TRACKS",
                "MIXER",
                "PLUGINS",
                "PROJECT"
            ).forEach { tab ->

                val selected = currentTab == tab

                Button(
                    onClick = { currentTab = tab },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        horizontal = 4.dp,
                        vertical = 8.dp
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) {
                            Purple
                        } else {
                            PanelBackground
                        }
                    )
                ) {
                    Text(
                        tab,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        when (currentTab) {

            "TRACKS" -> {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                importLauncher.launch(
                                    arrayOf(
                                        "audio/*",
                                        "application/octet-stream"
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Purple
                            )
                        ) {
                            Text(
                                "IMPORT AUDIO",
                                color = TextWhite
                            )
                        }
                    }
                }
