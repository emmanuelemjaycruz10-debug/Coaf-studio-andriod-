package com.emjaycruz.coaf

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

private val Background = Color(0xFF101014)
private val Panel = Color(0xFF1D1B25)
private val Purple = Color(0xFF9B5CFF)
private val White = Color(0xFFF5F2FA)
private val Muted = Color(0xFFAAA4B8)
private val Green = Color(0xFF51D6A3)

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

    var selectedBeat by remember {
        mutableStateOf<Uri?>(null)
    }

    var currentScreen by remember {
        mutableStateOf("home")
    }

    var message by remember {
        mutableStateOf("Your studio is ready.")
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedBeat = uri
            message = "Instrumental imported successfully."
        }
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
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
                    onClick = { currentScreen = "home" }
                ) {
                    Text("← BACK TO STUDIO", color = White)
                }

                Spacer(Modifier.height(18.dp))
            }

            when (currentScreen) {

                "home" -> {
                    Text(
                        text = "YOUR MUSIC. YOUR SOUND.",
                        color = White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(16.dp))

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

                    Spacer(Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(
                                Panel,
                                RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (recording)
                                    "● RECORDING"
                                else
                                    "COAF WORKSPACE",
                                color = if (recording)
                                    Color(0xFFFF5A70)
                                else
                                    Purple,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "▂ ▅ ▃ ▇ ▄ ▆ ▂ ▇ ▃ ▅",
                                color = Purple,
                                fontSize = 21.sp
                            )

                            Text(
                                text = "Your vocal workspace",
                                color = Muted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

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
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (recording)
                                Color(0xFFB82F4B)
                            else
                                Purple
                        )
                    ) {
                        Text(
                            text = if (recording)
                                "STOP"
                            else
                                "● RECORD VOCALS",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            audioPicker.launch(
                                arrayOf("audio/*")
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            "♫  IMPORT BEAT / MP3",
                            color = White
                        )
                    }

                    if (selectedBeat != null) {
                        Spacer(Modifier.height(12.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Panel,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(14.dp)
                        ) {
                            Text(
                                "INSTRUMENTAL SELECTED",
                                color = Green,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                selectedBeat.toString(),
                                color = Muted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            currentScreen = "effects"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            "♫  CRUZ VOCAL EFFECTS",
                            color = White
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            currentScreen = "mix"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            "🎚  MIX & MASTER",
                            color = White
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = message,
                        color = Muted,
                        fontSize = 13.sp
                    )
                }

                "effects" -> {
                    Text(
                        "CRUZ PLUGIN SUITE",
                        color = Purple,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        "Your signature vocal-processing workspace",
                        color = Muted,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(22.dp))

                    PluginCard(
                        "CRUZ REVERB",
                        "Room size • Decay • Wet/Dry"
                    )

                    PluginCard(
                        "CRUZ MELODYNE",
                        "Graphical pitch editing"
                    )

                    PluginCard(
                        "CRUZ AUTOTUNE",
                        "Key • Scale • Retune speed"
                    )

                    PluginCard(
                        "CRUZ COMPRESSOR",
                        "Dynamics and vocal control"
                    )

                    PluginCard(
                        "CRUZ OPTOCOMP",
                        "Smooth vocal levelling"
                    )

                    PluginCard(
                        "CRUZ FET COMP",
                        "Punchy vocal compression"
                    )

                    PluginCard(
                        "CRUZ BUS COMP",
                        "Glue and cohesion"
                    )

                    PluginCard(
                        "CRUZ DELAY",
                        "Time • Feedback • Stereo"
                    )

                    PluginCard(
                        "CRUZ EQ",
                        "Frequency and tone shaping"
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        "Plugin interfaces and audio processing " +
                            "will be implemented in upcoming stages.",
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

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "PROJECT",
                        color = Muted,
                        fontSize = 11.sp
                    )

                    Text(
                        projectName.ifBlank { "Untitled Project" },
                        color = White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(20.dp))

                    PluginCard(
                        "VOCAL TRACK",
                        "Your recorded vocal"
                    )

                    PluginCard(
                        "INSTRUMENTAL TRACK",
                        if (selectedBeat != null)
                            "Beat selected"
                        else
                            "No instrumental imported"
                    )

                    PluginCard(
                        "MASTER CHANNEL",
                        "Final output processing"
                    )

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            audioPicker.launch(
                                arrayOf("audio/*")
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Purple
                        )
                    ) {
                        Text("IMPORT INSTRUMENTAL")
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Multitrack playback, effect routing, " +
                            "mixing and audio export are planned " +
                            "for the audio-engine stages.",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(30.dp))

            Text(
                text = "MADE FOR THE KING OF ALL FLAVORS 👑",
                color = Purple,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
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
            .background(
                Panel,
                RoundedCornerShape(14.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = name,
            color = Purple,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = description,
            color = Muted,
            fontSize = 12.sp
        )
    }
}