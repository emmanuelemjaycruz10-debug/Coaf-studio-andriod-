package com.emjaycruz.coaf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
var projectName by remember { mutableStateOf("My First Recording") }
var recording by remember { mutableStateOf(false) }
var message by remember { mutableStateOf("Your studio is ready.") }

MaterialTheme {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
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

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "YOUR MUSIC. YOUR SOUND.",
            color = White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(22.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Panel, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (recording) "● RECORDING" else "WAVEFORM",
                    color = if (recording) Color(0xFFFF5A70) else Purple,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "▂ ▅ ▃ ▇ ▄ ▆ ▂ ▇ ▃ ▅ ▂ ▆ ▄",
                    color = Purple,
                    fontSize = 20.sp
                )

                Text(
                    text = "Your vocal workspace",
                    color = Muted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

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
                containerColor = if (recording) Color(0xFFB82F4B) else Purple
            )
        ) {
            Text(
                text = if (recording) "STOP" else "●  RECORD VOCALS",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { message = "Effects workspace selected." },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("♫   VOCAL EFFECTS", color = White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { message = "Mixing workspace selected." },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("🎚   MIX & MASTER", color = White)
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = message,
            color = Muted,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.weight(1f))

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
