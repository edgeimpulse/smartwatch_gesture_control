package com.example.wizbulb.presentation

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text

private enum class EditField { NONE, IP, PORT }

@Composable
fun SettingsScreen(context: Context) {
    val initial    = remember { BulbSettings.load(context) }
    var ip         by remember { mutableStateOf(initial.first) }
    var portStr    by remember { mutableStateOf(initial.second.toString()) }
    var saved      by remember { mutableStateOf(true) }
    var testStatus by remember { mutableStateOf("") }
    var editing    by remember { mutableStateOf(EditField.NONE) }

    if (editing != EditField.NONE) {
        val isIp = editing == EditField.IP
        TextEntryOverlay(
            label        = if (isIp) "IP address" else "Port",
            initialValue = if (isIp) ip else portStr,
            inputType    = if (isIp) android.text.InputType.TYPE_CLASS_TEXT
                           else      android.text.InputType.TYPE_CLASS_NUMBER,
            onDone       = { value ->
                if (isIp) ip = value else portStr = value
                saved   = false
                editing = EditField.NONE
            }
        )
        return
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().background(Color(0xFF080C10))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
        ) {
            Text(
                text          = "SETTINGS",
                fontSize      = 9.sp,
                fontWeight    = FontWeight.Light,
                color         = Color(0xFF37474F),
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(8.dp))

            SettingsChip("IP address", ip)  { editing = EditField.IP }
            Spacer(Modifier.height(6.dp))
            SettingsChip("Port", portStr)   { editing = EditField.PORT }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val port = portStr.toIntOrNull() ?: BulbSettings.DEFAULT_PORT
                        portStr    = port.toString()
                        WizController.configure(ip, port)
                        BulbSettings.save(context, ip, port)
                        saved      = true
                        testStatus = ""
                    },
                    colors   = ButtonDefaults.buttonColors(
                        backgroundColor = if (saved) Color(0xFF1B5E20) else Color(0xFF1565C0)
                    ),
                    modifier = Modifier.weight(1f)
                ) { Text(if (saved) "Saved" else "SAVE", fontSize = 11.sp) }

                Button(
                    onClick = {
                        val port = portStr.toIntOrNull() ?: BulbSettings.DEFAULT_PORT
                        testStatus = "Testing..."
                        WizController.test(ip, port) { reachable ->
                            testStatus = if (reachable) "Reachable ✓" else "No response"
                        }
                    },
                    colors   = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1A237E)),
                    modifier = Modifier.weight(1f)
                ) { Text("TEST", fontSize = 11.sp) }
            }

            if (testStatus.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text          = testStatus,
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.Light,
                    color         = when {
                        testStatus.startsWith("Reachable") -> Color(0xFF00E5FF)
                        testStatus == "Testing..."         -> Color(0xFF90CAF9)
                        else                               -> Color(0xFFEF5350)
                    },
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsChip(label: String, value: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D1626), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 9.sp, color = Color(0xFF546E7A), letterSpacing = 1.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 13.sp, color = Color.White)
    }
}

@Composable
private fun TextEntryOverlay(
    label: String,
    initialValue: String,
    inputType: Int,
    onDone: (String) -> Unit
) {
    val currentValue = remember { mutableStateOf(initialValue) }
    val onDoneRef    = rememberUpdatedState(onDone)
    val editTextRef  = remember { mutableStateOf<EditText?>(null) }

    BackHandler { onDoneRef.value(currentValue.value) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080C10))
            .padding(top = 30.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(label, fontSize = 9.sp, color = Color(0xFF546E7A), letterSpacing = 2.sp)

        Spacer(Modifier.height(14.dp))

        // Native EditText — reliable IME input on Wear OS
        AndroidView(
            factory = { ctx ->
                EditText(ctx).apply {
                    editTextRef.value = this
                    this.inputType = inputType
                    imeOptions = EditorInfo.IME_ACTION_DONE
                    setText(initialValue)
                    setSelection(initialValue.length)
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    textSize = 20f
                    gravity  = Gravity.CENTER
                    setSingleLine(true)
                    setPadding(0, 0, 0, 0)

                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            currentValue.value = s?.toString() ?: ""
                        }
                        override fun afterTextChanged(s: Editable?) {}
                    })

                    setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            onDoneRef.value(text?.toString() ?: "")
                            true
                        } else false
                    }

                    postDelayed({
                        requestFocus()
                        val imm = ctx.getSystemService(InputMethodManager::class.java)
                        imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                    }, 150)
                }
            },
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        )

        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1976D2)))
        Spacer(Modifier.height(10.dp))

        Text(
            text          = "press back to save",
            fontSize      = 9.sp,
            color         = Color(0xFF37474F),
            textAlign     = TextAlign.Center,
            letterSpacing = 1.sp
        )
    }
}
