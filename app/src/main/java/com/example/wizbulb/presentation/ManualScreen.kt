package com.example.wizbulb.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text

@Composable
fun ManualScreen() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080C10))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text          = "MANUAL",
                fontSize      = 9.sp,
                fontWeight    = FontWeight.Light,
                color         = Color(0xFF37474F),
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(10.dp))

            // ON / OFF
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconLabelButton("ON", Color(0xFF1565C0), { WizController.turnOn() }) {
                    Icon(Icons.Default.PowerSettingsNew, null, tint = Color.White,            modifier = Modifier.size(20.dp))
                }
                IconLabelButton("OFF", Color(0xFF1A237E), { WizController.turnOff() }) {
                    Icon(Icons.Default.PowerSettingsNew, null, tint = Color(0xFF546E7A), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // BRIGHT / DIM
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconLabelButton("BRIGHT", Color(0xFF4A148C), { WizController.bright() }) {
                    Icon(Icons.Default.BrightnessHigh, null, tint = Color(0xFFCE93D8), modifier = Modifier.size(20.dp))
                }
                IconLabelButton("DIM", Color(0xFF0D1B6E), { WizController.dim() }) {
                    Icon(Icons.Default.BrightnessLow,  null, tint = Color(0xFF90CAF9), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // R / G / B
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ColourButton(Color(0xFFB71C1C), "R") { WizController.red() }
                ColourButton(Color(0xFF1B5E20), "G") { WizController.green() }
                ColourButton(Color(0xFF0D47A1), "B") { WizController.blue() }
            }
        }

    }
}

@Composable
private fun IconLabelButton(
    label: String,
    bgColor: Color,
    onClick: () -> Unit,
    size: Dp = 44.dp,
    icon: @Composable () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick  = onClick,
            colors   = ButtonDefaults.buttonColors(backgroundColor = bgColor),
            modifier = Modifier.size(size)
        ) { icon() }
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 8.sp, color = Color(0xFF455A64), letterSpacing = 1.sp)
    }
}

@Composable
private fun ColourButton(color: Color, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick  = onClick,
            colors   = ButtonDefaults.buttonColors(backgroundColor = color),
            modifier = Modifier.size(36.dp)
        ) {}
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 8.sp, color = Color(0xFF455A64), letterSpacing = 1.sp)
    }
}
