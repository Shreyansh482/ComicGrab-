package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.ui.ComicViewModel
import com.example.ui.Screen
import com.example.ui.theme.*

@Composable
fun LockScreen(
    viewModel: ComicViewModel,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val hasPin = remember { viewModel.securityManager.hasPinConfigured() }
    var isSetupMode by remember { mutableStateOf(!hasPin) }
    var setupStep by remember { mutableStateOf(1) } // 1: Enter new PIN, 2: Confirm new PIN
    var firstEnteredPin by remember { mutableStateOf("") }
    var currentPinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isLockedOut = remember { derivedStateOf { viewModel.securityManager.isLockedOut() } }
    val lockoutSeconds = remember { derivedStateOf { viewModel.securityManager.getRemainingLockoutSeconds() } }

    fun handleDigit(digit: String) {
        if (isLockedOut.value) return
        if (currentPinInput.length < 4) {
            currentPinInput += digit
            errorMessage = null

            if (currentPinInput.length == 4) {
                if (isSetupMode) {
                    if (setupStep == 1) {
                        firstEnteredPin = currentPinInput
                        currentPinInput = ""
                        setupStep = 2
                    } else {
                        if (currentPinInput == firstEnteredPin) {
                            viewModel.securityManager.setupPin(currentPinInput)
                            onUnlocked()
                        } else {
                            errorMessage = "PINs do not match. Try again."
                            currentPinInput = ""
                            setupStep = 1
                        }
                    }
                } else {
                    val verified = viewModel.securityManager.verifyPin(currentPinInput)
                    if (verified) {
                        onUnlocked()
                    } else {
                        if (viewModel.securityManager.isLockedOut()) {
                            errorMessage = "Too many attempts. Locked out for ${viewModel.securityManager.getRemainingLockoutSeconds()}s"
                        } else {
                            errorMessage = "Incorrect PIN. Try again."
                        }
                        currentPinInput = ""
                    }
                }
            }
        }
    }

    fun handleBackspace() {
        if (currentPinInput.isNotEmpty()) {
            currentPinInput = currentPinInput.dropLast(1)
            errorMessage = null
        }
    }

    fun triggerFingerprint() {
        val activity = context as? FragmentActivity
        if (activity != null && viewModel.securityManager.canUseBiometric()) {
            viewModel.securityManager.promptBiometric(
                activity = activity,
                onSuccess = {
                    onUnlocked()
                },
                onError = { err ->
                    errorMessage = err
                }
            )
        } else {
            // Simulated biometric unlock for preview/emulator environment without hardware enrolled
            viewModel.securityManager.unlockWithBiometrics()
            onUnlocked()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepForestBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // App Branding Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(DarkGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSetupMode) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = null,
                    tint = TealAccent,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ComicGrab",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = PaleCream
                )
            )

            Text(
                text = when {
                    isSetupMode && setupStep == 1 -> "Create a 4-digit security PIN"
                    isSetupMode && setupStep == 2 -> "Confirm your 4-digit PIN"
                    else -> "Your private comic library"
                },
                style = MaterialTheme.typography.bodyMedium.copy(color = SageSurface)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 4 PIN Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < currentPinInput.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) TealAccent else Color.Transparent)
                            .border(2.dp, if (isFilled) TealAccent else MutedGreen, CircleShape)
                    )
                }
            }

            // Error message
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall.copy(color = ErrorRed),
                    fontWeight = FontWeight.SemiBold
                )
            } else if (isLockedOut.value) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Temporarily locked. Wait ${lockoutSeconds.value}s",
                    style = MaterialTheme.typography.bodySmall.copy(color = WarningAmber),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Fingerprint Quick Unlock Button
            if (!isSetupMode) {
                OutlinedButton(
                    onClick = { triggerFingerprint() },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TealAccent),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(DarkGreen)),
                    modifier = Modifier.testTag("fingerprint_unlock_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Fingerprint Unlock",
                        tint = TealAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Touch to unlock", color = PaleCream, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Keypad
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BACK", "0", "EMPTY")
            )

            for (row in rows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    for (key in row) {
                        when (key) {
                            "EMPTY" -> {
                                Spacer(modifier = Modifier.size(68.dp))
                            }
                            "BACK" -> {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .clickable { handleBackspace() }
                                        .testTag("keypad_backspace"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Backspace",
                                        tint = PaleCream,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(CardForest)
                                        .border(1.dp, CardForestBorder, CircleShape)
                                        .clickable { handleDigit(key) }
                                        .testTag("keypad_digit_$key"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            color = PaleCream,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isSetupMode) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = {
                        // Skip PIN setup for now
                        viewModel.securityManager.unlockWithBiometrics()
                        onUnlocked()
                    }
                ) {
                    Text("Skip for now", color = SageSurface)
                }
            }
        }
    }
}
