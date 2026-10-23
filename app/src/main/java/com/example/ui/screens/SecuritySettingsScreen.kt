package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ComicViewModel
import com.example.ui.components.ComicGrabTopBar
import com.example.ui.theme.*

@Composable
fun SecuritySettingsScreen(
    viewModel: ComicViewModel,
    modifier: Modifier = Modifier
) {
    val sec = viewModel.securityManager
    var isBiometricEnabled by remember { mutableStateOf(sec.isBiometricEnabled()) }
    var currentTimeout by remember { mutableIntStateOf(sec.getLockTimeoutSeconds()) }
    var showChangePinDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ComicGrabTopBar(
                title = "Security Settings",
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateBack() }
            )
        },
        containerColor = DeepForestBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Unlock Method Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardForestBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardForest),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Unlock Method",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PaleCream
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = TealAccent)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Use Fingerprint", color = PaleCream, fontWeight = FontWeight.SemiBold)
                                Text("Biometric unlock with PIN fallback", color = SageSurface, fontSize = 12.sp)
                            }
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = {
                                isBiometricEnabled = it
                                sec.setBiometricEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepForestDarker,
                                checkedTrackColor = TealAccent
                            )
                        )
                    }
                }
            }

            // Auto Lock Timeout Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardForestBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardForest),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Auto Lock Timeout",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PaleCream
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val timeouts = listOf(
                        Pair(0, "Immediately"),
                        Pair(60, "1 Minute"),
                        Pair(300, "5 Minutes"),
                        Pair(-1, "Never")
                    )

                    for ((seconds, label) in timeouts) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentTimeout == seconds,
                                onClick = {
                                    currentTimeout = seconds
                                    sec.setLockTimeoutSeconds(seconds)
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = TealAccent,
                                    unselectedColor = MutedGreen
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, color = PaleCream)
                        }
                    }
                }
            }

            // Change PIN Action
            Button(
                onClick = { showChangePinDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("change_pin_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkGreen,
                    contentColor = PaleCream
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Pin, contentDescription = null, tint = TealAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change 4-Digit PIN", fontWeight = FontWeight.SemiBold)
            }

            // Privacy Notice Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardForestBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DeepForestDarker),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = TealAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Security Architecture", color = PaleCream, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your PIN is stored as a salted SHA-256 hash using 5,000 iterations and a cryptographically secure random salt. The plaintext PIN is never stored or transmitted. The app lock gates access to your library, reader, downloads, and archives.",
                        color = SageSurface,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showChangePinDialog) {
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Change 4-Digit PIN", color = PaleCream, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                        label = { Text("New 4-Digit PIN", color = SageSurface) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PaleCream,
                            unfocusedTextColor = PaleCream,
                            focusedBorderColor = TealAccent
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
                        label = { Text("Confirm New PIN", color = SageSurface) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PaleCream,
                            unfocusedTextColor = PaleCream,
                            focusedBorderColor = TealAccent
                        ),
                        singleLine = true
                    )
                    if (pinError != null) {
                        Text(pinError!!, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length != 4) {
                            pinError = "PIN must be exactly 4 digits."
                        } else if (newPin != confirmPin) {
                            pinError = "PINs do not match."
                        } else {
                            sec.setupPin(newPin)
                            showChangePinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent, contentColor = DeepForestDarker)
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel", color = PaleCream)
                }
            },
            containerColor = CardForest
        )
    }
}
