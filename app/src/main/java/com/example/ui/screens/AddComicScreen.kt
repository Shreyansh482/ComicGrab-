package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ComicViewModel
import com.example.ui.Screen
import com.example.ui.components.ComicGrabTopBar
import com.example.ui.theme.*

@Composable
fun AddComicScreen(
    viewModel: ComicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUrlState by viewModel.currentInputUrl.collectAsState()
    var urlText by remember { mutableStateOf(currentUrlState) }

    LaunchedEffect(currentUrlState) {
        if (currentUrlState.isNotBlank()) {
            urlText = currentUrlState
        }
    }

    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val analyzeError by viewModel.analyzeError.collectAsState()
    var showDiagnosticDialog by remember { mutableStateOf(false) }

    fun pasteFromClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val text = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
        if (!text.isNullOrBlank()) {
            urlText = text.trim()
            viewModel.currentInputUrl.value = urlText
        }
    }

    fun copyDiagnosticsToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("ComicGrab Diagnostics", viewModel.getDiagnosticSummary())
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "Diagnostic report copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun startAnalysis() {
        if (urlText.isNotBlank()) {
            viewModel.currentInputUrl.value = urlText
            viewModel.analyzeComicUrl(urlText) { discovered ->
                viewModel.navigateTo(Screen.SelectChapters(discovered))
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ComicGrabTopBar(
                title = "Add Comic",
                canNavigateBack = true,
                onNavigateBack = { viewModel.navigateBack() },
                actions = {
                    IconButton(onClick = { showDiagnosticDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Diagnostic Log",
                            tint = TealAccent
                        )
                    }
                }
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

            // URL Input Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardForestBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardForest),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Comic Series or Chapter URL",
                        style = MaterialTheme.typography.titleSmall.copy(color = PaleCream, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = urlText,
                        onValueChange = {
                            urlText = it
                            viewModel.currentInputUrl.value = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_comic_url_input"),
                        placeholder = { Text("https://example.com/comic-series", color = MutedText) },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { pasteFromClipboard() }) {
                                Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste", tint = TealAccent)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = DarkGreen,
                            focusedTextColor = PaleCream,
                            unfocusedTextColor = PaleCream,
                            focusedContainerColor = DeepForestDarker,
                            unfocusedContainerColor = DeepForestDarker
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { startAnalysis() },
                        enabled = !isAnalyzing && urlText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("analyze_url_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealAccent,
                            contentColor = DeepForestDarker,
                            disabledContainerColor = DarkGreen,
                            disabledContentColor = MutedText
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = DeepForestDarker,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing website...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyze URL", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            // Diagnostic Error Card with Retry & Log actions
            if (analyzeError != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ErrorRed.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = ErrorRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Analysis Failed",
                                color = ErrorRed,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = analyzeError!!,
                            color = PaleCream,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { startAnalysis() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ErrorRed,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showDiagnosticDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PaleCream),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(SageSurface)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Diagnostics")
                            }
                        }
                    }
                }
            }

            // Quick Samples to test easily
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardForestBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardForest),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quick Test Sources",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = PaleCream)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap to test chapter discovery and real JPEG conversions:",
                        style = MaterialTheme.typography.bodySmall.copy(color = SageSurface)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                urlText = "https://manhwatop.com/manga/solo-leveling"
                                viewModel.currentInputUrl.value = urlText
                                startAnalysis()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGreen, contentColor = PaleCream),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Solo Leveling", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                urlText = "https://asuracomic.net/series/the-beginning-after-the-end"
                                viewModel.currentInputUrl.value = urlText
                                startAnalysis()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGreen, contentColor = PaleCream),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("TBATE", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Supported Formats Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardForestBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = CardForest),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Supported formats & sites",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PaleCream
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val items = listOf(
                        "Standard HTTP and HTTPS comic addresses",
                        "Madara, MangaStream, MangaKakalot & standard comic engines",
                        "Automatic lazy-loaded image extraction",
                        "Verified JPEG conversion & private local storage"
                    )

                    for (item in items) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = TealAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = item, color = SageSurface, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Diagnostic Notice
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardForestBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DeepForestDarker),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = TealAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Websites protected by Cloudflare Turnstile, CAPTCHAs, or logins cannot be scraped without browser interaction. Real errors are logged above.",
                        color = SageSurface,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Diagnostic Summary Dialog
    if (showDiagnosticDialog) {
        val summary = viewModel.getDiagnosticSummary()
        AlertDialog(
            onDismissRequest = { showDiagnosticDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = TealAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Diagnostic Report", color = PaleCream, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Structured trace of website requests, status codes, and operations (never contains PIN or credentials):",
                        color = SageSurface,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = DeepForestDarker,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CardForestBorder, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = summary,
                            color = PaleCream,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp),
                            lineHeight = 15.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { copyDiagnosticsToClipboard() },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent, contentColor = DeepForestDarker)
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Report", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiagnosticDialog = false }) {
                    Text("Close", color = PaleCream)
                }
            },
            containerColor = CardForest
        )
    }
}
