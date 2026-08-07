package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: BankingViewModel,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var isSyncingSheets by remember { mutableStateOf(false) }
    var isSyncingExcel by remember { mutableStateOf(false) }
    
    // Spinning animation for sync icons
    val transition = rememberInfiniteTransition(label = "SyncRotate")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    var passcodeSettingValue by remember { mutableStateOf(viewModel.passcodeLock) }
    var userMessage by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Title
        item {
            Text(
                text = "SYSTEM SETTINGS & CODES",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary,
                letterSpacing = 1.sp
            )
        }

        // 1. Cloud Sync Hub (Google Sheets & Excel)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Cloud Ledger Synchronizer", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GoldPrimary)
                    Text("Auto-balance registers to official central spreadsheets.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                    // Google Sheets row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Google Sheets Live Sync", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Active spreadsheet: Toufiq_Ledger_2026", fontSize = 11.sp, color = Color.Gray)
                        }
                        IconButton(
                            onClick = {
                                viewModel.syncExistingDataToExcelInBackground()
                                userMessage = "Syncing all local items to Google Sheets..."
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync Sheets",
                                tint = GoldPrimary,
                                modifier = Modifier.rotate(if (isSyncingSheets) rotation else 0f)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    // Apps Script Web App Configuration
                    var savedAppsScriptUrl by remember { mutableStateOf(viewModel.getSavedAppsScriptUrl()) }
                    var showScriptCodeDialog by remember { mutableStateOf(false) }

                    Text("Google Apps Script Web App URL (For Instant Sheet Delivery Write-Back)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GoldPrimary)
                    OutlinedTextField(
                        value = savedAppsScriptUrl,
                        onValueChange = {
                            savedAppsScriptUrl = it
                            viewModel.saveAppsScriptUrl(it)
                        },
                        placeholder = { Text("https://script.google.com/macros/s/.../exec", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                viewModel.syncExistingDataToExcelInBackground()
                                userMessage = "Pushing all delivered items to Google Sheets now!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp), tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Push All Delivered Items", fontSize = 11.sp, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = {
                                val scriptCode = """
function doGet(e) {
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  var params = e.parameter;
  var acct = params.accountNumber || "";
  var customerName = params.customerName || "";
  var deliveryDate = params.delivered || params.deliveryDate || params.delivery_date || "";
  var action = params.action || "UPDATE";
  
  if (!acct && !customerName) {
    return ContentService.createTextOutput("Missing Account Number or Customer Name");
  }
  
  var data = sheet.getDataRange().getValues();
  var found = false;
  
  for (var i = 1; i < data.length; i++) {
    var sheetAcct = String(data[i][0]).trim();
    var sheetName = String(data[i][1]).trim().toUpperCase();
    
    if ((acct && sheetAcct === String(acct).trim()) || (customerName && sheetName === String(customerName).trim().toUpperCase())) {
      found = true;
      if (action === "DELIVER" || action === "UPDATE") {
        sheet.getRange(i + 1, 6).setValue(deliveryDate); // Column 6 = DELIVERED
      } else if (action === "ACTIVATE") {
        sheet.getRange(i + 1, 6).setValue("");
      } else if (action === "DELETE") {
        sheet.deleteRow(i + 1);
      }
      break;
    }
  }
  
  if (!found && action === "DELIVER") {
    sheet.appendRow([acct, customerName, params.phoneNumber || "", params.receiveDate || "", params.address || "", deliveryDate, params.fatherName || "", params.regNo || ""]);
  }
  
  return ContentService.createTextOutput("SUCCESS");
}
                                """.trimIndent()

                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(scriptCode))
                                userMessage = "Google Apps Script code copied to clipboard!"
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp), tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Apps Script Code", fontSize = 11.sp, color = GoldPrimary)
                        }
                    }
                }
            }
        }

        // 1b. Spreadsheet Templates & Column Mapping Guides
        item {
            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Official Spreadsheet Templates", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GoldPrimary)
                    Text("Copy and set up your import sheets using the link below. Keep the column headings in the exact sequential order listed below.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                    // Template 1: Debit Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateDark.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("1. Debit Card", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GoldPrimary)
                            TextButton(
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                onClick = {
                                    val url = "https://docs.google.com/spreadsheets/d/1BUc13oZ_qKIBW9OOFtcPAZh9aoELxyVq6sguoAyAdFg/edit?usp=sharing"
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(url))
                                    userMessage = "Official Sheets link copied!"
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = GoldPrimary)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Copy Sheets Link", fontSize = 10.sp, color = GoldPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Columns: Column A (Customer Name) | Column B (Account Number) | Column C (Address / Location) | Column D (Phone Number) | Column E (Remarks / Courier No)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }

                    // Template 2: CHEQUE BOOK
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateDark.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("2. Cheque Book", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GoldPrimary)
                            TextButton(
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                onClick = {
                                    val url = "https://docs.google.com/spreadsheets/d/1BUc13oZ_qKIBW9OOFtcPAZh9aoELxyVq6sguoAyAdFg/edit?usp=sharing"
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(url))
                                    userMessage = "Official Sheets link copied!"
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp), tint = GoldPrimary)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Copy Sheets Link", fontSize = 10.sp, color = GoldPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Columns: Column A (Customer Name) | Column B (Account Number) | Column C (Address / Location) | Column D (Phone Number) | Column E (Remarks / Pages Count)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // 2. Personal Pin login security
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Officer Vault Security PIN", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GoldPrimary)
                    Text("Secures the device database when leaving terminal.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Secure Pin Lockout", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Switch(
                            checked = viewModel.isPasscodeEnabled,
                            onCheckedChange = { isEnabled ->
                                viewModel.isPasscodeEnabled = isEnabled
                                if (!isEnabled) {
                                    viewModel.passcodeLock = ""
                                    passcodeSettingValue = ""
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = GoldPrimary.copy(alpha = 0.3f))
                        )
                    }

                    if (viewModel.isPasscodeEnabled) {
                        OutlinedTextField(
                            value = passcodeSettingValue,
                            onValueChange = {
                                passcodeSettingValue = it
                                viewModel.passcodeLock = it
                            },
                            label = { Text("Set 4-Digit Passcode") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // 3. Look & Feel preferences
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Operative Interface Themes", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GoldPrimary)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Modern Slate Dark Theme", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Switch(
                            checked = viewModel.isDarkMode,
                            onCheckedChange = { viewModel.isDarkMode = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary, checkedTrackColor = GoldPrimary.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }

        // 4. GitHub Sync & Auto-Updates
        item {
            var inputOwner by remember { mutableStateOf(viewModel.githubOwner) }
            var inputRepo by remember { mutableStateOf(viewModel.githubRepo) }
            var inputBranch by remember { mutableStateOf(viewModel.githubBranch) }
            var isEditingSettings by remember { mutableStateOf(false) }

            val context = androidx.compose.ui.platform.LocalContext.current

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("GitHub OTA Software Updater", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GoldPrimary)
                    Text("Bridges automatic live updates directly from your customized GitHub repository releases.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                    if (isEditingSettings) {
                        OutlinedTextField(
                            value = inputOwner,
                            onValueChange = { inputOwner = it },
                            label = { Text("GitHub Owner / Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary)
                        )
                        OutlinedTextField(
                            value = inputRepo,
                            onValueChange = { inputRepo = it },
                            label = { Text("Repository Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary)
                        )
                        OutlinedTextField(
                            value = inputBranch,
                            onValueChange = { inputBranch = it },
                            label = { Text("Branch Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    viewModel.saveGithubSettings(inputOwner, inputRepo, inputBranch)
                                    isEditingSettings = false
                                    userMessage = "GitHub OTA parameters updated!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save Parameters", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    inputOwner = viewModel.githubOwner
                                    inputRepo = viewModel.githubRepo
                                    inputBranch = viewModel.githubBranch
                                    isEditingSettings = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary)
                            ) {
                                Text("Cancel", fontSize = 12.sp)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Repository Connected", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("${viewModel.githubOwner}/${viewModel.githubRepo} (${viewModel.githubBranch})", fontSize = 11.sp, color = Color.Gray)
                                }
                                TextButton(onClick = { isEditingSettings = true }) {
                                    Text("Configure", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                            // Action buttons & Update status
                            if (viewModel.isUpdateAvailable) {
                                val update = viewModel.latestUpdateInfo
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = GoldPrimary.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "New Update Available: v${update?.versionName ?: "Unknown"}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = GoldPrimary
                                            )
                                            val currentVersion = com.example.BuildConfig.VERSION_CODE
                                            Text(
                                                text = "Current: $currentVersion -> New: ${update?.versionCode}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }

                                        if (update?.releaseNotes?.isNotBlank() == true) {
                                            Text(
                                                text = "Release Notes: ${update.releaseNotes}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        if (viewModel.downloadProgress != null) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                LinearProgressIndicator(
                                                    progress = { viewModel.downloadProgress ?: 0f },
                                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                                    color = GoldPrimary,
                                                    trackColor = GoldPrimary.copy(alpha = 0.2f)
                                                )
                                                Text(
                                                    text = viewModel.downloadStatusText,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = GoldPrimary
                                                )
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    viewModel.startApkDownload(context)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Download & Install Update", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Status: Up to date", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = GreenAccent)
                                        val currentVerCode = com.example.BuildConfig.VERSION_CODE
                                        val currentVerName = com.example.BuildConfig.VERSION_NAME
                                        Text("Current running version: v$currentVerName (Code $currentVerCode)", fontSize = 11.sp, color = Color.Gray)
                                    }

                                    if (viewModel.isCheckingForUpdates) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = GoldPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Button(
                                            onClick = {
                                                viewModel.triggerManualUpdateCheck { success, msg ->
                                                    userMessage = msg
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Check Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4b. System Database Reset & Demo Purging
        item {
            var showClearDialog by remember { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Operational Database Management", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RedAccent)
                    Text("Permanently purge all pre-loaded demo records and demo logs. Any actual data that you have manually entered, or synced from spreadsheet templates, will remain completely secure.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    
                    Button(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RedAccent, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Purge Pre-loaded Demo Data", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    title = { Text("Confirm Demo Purge", color = RedAccent, fontWeight = FontWeight.Bold) },
                    text = { Text("Are you absolutely sure you want to delete only the pre-loaded sample/demo entries from the database? This will clear demo Debit Cards, demo PIN Mailers, demo DPS slips, demo Cheque Books, demo tasks, and demo hunting entries. Your manually added or synced records will not be deleted. This action cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.clearAllDatabaseData {
                                    showClearDialog = false
                                    userMessage = "Pre-loaded demo data cleared successfully!"
                                }
                            }
                        ) {
                            Text("Purge Demo Data Only", color = RedAccent, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }

        // 5. Branch Info Cards
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("CHIRIRBANDAR AUDIT LOGS SUMMARY", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Branch Officer ID: B-902-TFQ\nTerminal Name: chirirbandar-audit-tablet\nSecurity level: High-End Hardware Bound\nFingerprint Status: Mock Sensor Simulated", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }

        // Confirmation SnackBar / Toast
        if (userMessage.isNotBlank()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GreenAccent.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(userMessage, color = GreenAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Close, contentDescription = null, tint = GreenAccent, modifier = Modifier.clickable { userMessage = "" })
                    }
                }
            }
        }
    }
}
