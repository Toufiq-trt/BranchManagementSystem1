package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.util.PdfHelper
import java.text.NumberFormat
import java.util.*

@Composable
fun AtmCalculatorScreen(
    viewModel: BankingViewModel,
    modifier: Modifier = Modifier
) {
    val atmLogs by viewModel.atmLoadingLogs.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedAtm by remember { mutableStateOf("ATM-25 CHIRIRBANDAR") }
    
    // Remaining Inputs
    var c1Remaining by remember { mutableStateOf("") }
    var c2Remaining by remember { mutableStateOf("") }
    var c3Remaining by remember { mutableStateOf("") }
    var c4Remaining by remember { mutableStateOf("") }

    // Loading Inputs (for ATM-25 setup after Required Cash To Load)
    var c1LoadingInput by remember { mutableStateOf("") }
    var c2LoadingInput by remember { mutableStateOf("") }
    var c3LoadingInput by remember { mutableStateOf("") }
    var c4LoadingInput by remember { mutableStateOf("") }

    var operatorName by remember { mutableStateOf("Officer Toufiq") }
    var remarks by remember { mutableStateOf("") }

    // PDF Preview States
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewTitle by remember { mutableStateOf("") }
    var previewTextContent by remember { mutableStateOf<String?>(null) }
    var onConfirmDownload by remember { mutableStateOf<() -> Unit>({}) }

    // Automatically Reset form to blank on selectedAtm switch
    LaunchedEffect(selectedAtm) {
        c1Remaining = ""
        c2Remaining = ""
        c3Remaining = ""
        c4Remaining = ""
        c1LoadingInput = ""
        c2LoadingInput = ""
        c3LoadingInput = ""
        c4LoadingInput = ""
        remarks = ""
    }

    // Capacity Rules
    val isAtm25 = selectedAtm.contains("ATM-25")
    val c1Cap = if (isAtm25) 2000 else 2500
    val c2Cap = if (isAtm25) 2000 else 2500
    val c3Cap = if (isAtm25) 2000 else 2000
    val c4Cap = if (isAtm25) 2000 else 2000

    // Input sanitizer - maximum 4 digits and does not exceed capacity
    fun sanitizeInput(input: String, maxCap: Int): String {
        val filtered = input.filter { it.isDigit() }
        if (filtered.isEmpty()) return ""
        val truncated = if (filtered.length > 4) filtered.take(4) else filtered
        val intVal = truncated.toIntOrNull() ?: 0
        return if (intVal > maxCap) maxCap.toString() else truncated
    }

    // Remaining notes parsing
    val c1RemInt = c1Remaining.toIntOrNull() ?: 0
    val c2RemInt = c2Remaining.toIntOrNull() ?: 0
    val c3RemInt = c3Remaining.toIntOrNull() ?: 0
    val c4RemInt = c4Remaining.toIntOrNull() ?: 0

    // Math calculations for remaining
    val remNotes1000 = c1RemInt + c2RemInt
    val remNotes500 = c3RemInt + c4RemInt
    val remainingAmount = (remNotes1000 * 1000L) + (remNotes500 * 500L)

    // Required Cash To Load (Automatic Calculation)
    val c1Required = (c1Cap - c1RemInt).coerceAtLeast(0)
    val c2Required = (c2Cap - c2RemInt).coerceAtLeast(0)
    val c3Required = (c3Cap - c3RemInt).coerceAtLeast(0)
    val c4Required = (c4Cap - c4RemInt).coerceAtLeast(0)
    val requiredCashToLoad = ((c1Required + c2Required) * 1000L) + ((c3Required + c4Required) * 500L)

    // Cassette Wise Load Counts depending on ATM type:
    // For ATM-25: uses manual inputs.
    // For other ATMs: C1 is 2500, C2 is (2500 - (C1+C2 remaining)), C3 is 2000, C4 is (2000 - (C3+C4 remaining))
    val finalC1Load = if (isAtm25) {
        c1LoadingInput.toIntOrNull() ?: 0
    } else {
        2500
    }
    val finalC2Load = if (isAtm25) {
        c2LoadingInput.toIntOrNull() ?: 0
    } else {
        (2500 - (c1RemInt + c2RemInt)).coerceAtLeast(0)
    }
    val finalC3Load = if (isAtm25) {
        c3LoadingInput.toIntOrNull() ?: 0
    } else {
        2000
    }
    val finalC4Load = if (isAtm25) {
        c4LoadingInput.toIntOrNull() ?: 0
    } else {
        (2000 - (c3RemInt + c4RemInt)).coerceAtLeast(0)
    }

    // Total calculated load amount
    val totalLoadAmount = ((finalC1Load + finalC2Load) * 1000L) + ((finalC3Load + finalC4Load) * 500L)

    // Formatters in English
    val englishFormatter = NumberFormat.getNumberInstance(Locale.US)
    fun formatTakaEnglish(amount: Long): String {
        return "BDT ${englishFormatter.format(amount)}"
    }

    var activeTab by remember { mutableStateOf(0) } // 0 = Calculator, 1 = History Logs

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ATM REPLENISHMENT CENTER",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = GoldPrimary,
            letterSpacing = 1.sp
        )

        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.Transparent,
            contentColor = GoldPrimary
        ) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Calculate, contentDescription = null)
                    Text("Calculator")
                }
            }
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Text("Loading History")
                }
            }
        }

        if (activeTab == 0) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ATM Picker
                item {
                    Text("Select Target ATM Terminal", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GoldPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val atms = listOf("ATM-25", "ATM-42", "ATM-43", "ATM-44")
                        atms.forEach { name ->
                            val fullName = when (name) {
                                "ATM-25" -> "ATM-25 CHIRIRBANDAR"
                                "ATM-42" -> "ATM-42 THAKURGAON"
                                "ATM-43" -> "ATM-43 PANCHAGARH"
                                else -> "ATM-44 NILPHAMARI"
                            }
                            val isSel = selectedAtm == fullName
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSel) GoldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedAtm = fullName }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                  Text(
                                    text = name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) SlateDark else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Selected Terminal Information
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(selectedAtm, fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Cassette 1 & 2: 1000 Taka (Cap: $c1Cap notes)\nCassette 3 & 4: 500 Taka (Cap: $c3Cap notes)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Cassette Inputs
                item {
                    Text("Input Remaining Notes Left In ATM (Max $c1Cap notes, 4-digits max)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GoldPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = c1Remaining,
                            onValueChange = { c1Remaining = sanitizeInput(it, c1Cap) },
                            label = { Text("C1 (1000)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = c2Remaining,
                            onValueChange = { c2Remaining = sanitizeInput(it, c2Cap) },
                            label = { Text("C2 (1000)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = c3Remaining,
                            onValueChange = { c3Remaining = sanitizeInput(it, c3Cap) },
                            label = { Text("C3 (500)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = c4Remaining,
                            onValueChange = { c4Remaining = sanitizeInput(it, c4Cap) },
                            label = { Text("C4 (500)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                // Calculator Results Panel
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateDark)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("REPLENISHMENT LIVE PREVIEW", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Remaining Taka (English):", color = Color.White)
                                Text(formatTakaEnglish(remainingAmount), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Remaining 1000 Notes:", color = Color.White.copy(alpha = 0.7f))
                                Text("$remNotes1000 Pcs", color = Color.White)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Remaining 500 Notes:", color = Color.White.copy(alpha = 0.7f))
                                Text("$remNotes500 Pcs", color = Color.White)
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Required Cash To Load:", color = GoldPrimary, fontWeight = FontWeight.Bold)
                                Text(formatTakaEnglish(requiredCashToLoad), color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }

                            if (!isAtm25) {
                                Text(
                                    "Formula Loading Schedule:\n- Cassette 1: Load $finalC1Load notes | - Cassette 2: Load $finalC2Load notes\n- Cassette 3: Load $finalC3Load notes | - Cassette 4: Load $finalC4Load notes",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Calculated Total Load Amount:", color = GoldPrimary, fontWeight = FontWeight.Bold)
                                    Text(formatTakaEnglish(totalLoadAmount), color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                // Setup Loading Inputs only for ATM-25 setup after Required Cash To Load
                if (isAtm25) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("ATM-25 LOADING SETUP", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GoldPrimary)
                                Text("Specify the precise loaded notes for each cassette:", fontSize = 11.sp, color = Color.LightGray)
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = c1LoadingInput,
                                        onValueChange = { c1LoadingInput = sanitizeInput(it, c1Cap) },
                                        label = { Text("Load C1 (1000)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = c2LoadingInput,
                                        onValueChange = { c2LoadingInput = sanitizeInput(it, c2Cap) },
                                        label = { Text("Load C2 (1000)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = c3LoadingInput,
                                        onValueChange = { c3LoadingInput = sanitizeInput(it, c3Cap) },
                                        label = { Text("Load C3 (500)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = c4LoadingInput,
                                        onValueChange = { c4LoadingInput = sanitizeInput(it, c4Cap) },
                                        label = { Text("Load C4 (500)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                                
                                HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Calculated Total Load Amount:", color = GoldPrimary, fontWeight = FontWeight.Bold)
                                    Text(formatTakaEnglish(totalLoadAmount), color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }

                // Commit Transaction Form
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Commit Loading Log", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GoldPrimary)
                            OutlinedTextField(
                                value = operatorName,
                                onValueChange = { operatorName = it },
                                label = { Text("Officer / Operator Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = remarks,
                                onValueChange = { remarks = it },
                                label = { Text("Remarks") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    if (operatorName.isBlank()) {
                                        Toast.makeText(context, "Operator Name is required", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (totalLoadAmount <= 0) {
                                        Toast.makeText(context, "Total load amount must be greater than 0", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    
                                    viewModel.addAtmLoadingLog(
                                        atmName = selectedAtm,
                                        c1Rem = c1RemInt, c2Rem = c2RemInt, c3Rem = c3RemInt, c4Rem = c4RemInt,
                                        c1Load = finalC1Load, c2Load = finalC2Load, c3Load = finalC3Load, c4Load = finalC4Load,
                                        totalLoading = totalLoadAmount,
                                        operator = operatorName,
                                        remarks = remarks
                                    )
                                    
                                    Toast.makeText(context, "Transaction log committed successfully!", Toast.LENGTH_SHORT).show()
                                    
                                    // Reset fields
                                    c1Remaining = ""
                                    c2Remaining = ""
                                    c3Remaining = ""
                                    c4Remaining = ""
                                    c1LoadingInput = ""
                                    c2LoadingInput = ""
                                    c3LoadingInput = ""
                                    c4LoadingInput = ""
                                    remarks = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Loading Record & Print Log Receipt")
                            }
                        }
                    }
                }
            }
        } else {
            // Logs history list
            if (atmLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No ATM loading transactions registered yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(atmLogs) { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(log.atmName, fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 13.sp)
                                    Text("${log.dateStr} | ${log.timeStr}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Total Load: ${formatTakaEnglish(log.loadingAmount)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Loaded By: ${log.operatorName}", fontSize = 11.sp, color = Color.LightGray)
                                    }
                                    
                                    // Download transaction report PDF button
                                    IconButton(
                                        onClick = {
                                            val receivedC1And2 = log.c1Remaining + log.c2Remaining
                                            val receivedC3And4 = log.c3Remaining + log.c4Remaining
                                            val totalReceivedAmt = (receivedC1And2 * 1000L) + (receivedC3And4 * 500L)
                                            val loaded1000s = log.c1Loading + log.c2Loading
                                            val loaded500s = log.c3Loading + log.c4Loading
                                            
                                            val slipContent = """
==================================================
        TOUFIQS SMART BANKING SOLUTION
==================================================
ATM REPLENISHMENT TRANSACTION SLIP
==================================================
Terminal: ${log.atmName}
Operator: ${log.operatorName}
Date: ${log.dateStr}
Time: ${log.timeStr}

CASSETTE WISE RECEIVED (REMAINING)
--------------------------------------------------
Cassette 1 (1000 Taka): ${log.c1Remaining} notes
Cassette 2 (1000 Taka): ${log.c2Remaining} notes
Cassette 3 ( 500 Taka): ${log.c3Remaining} notes
Cassette 4 ( 500 Taka): ${log.c4Remaining} notes

Total Received 1000 Notes: $receivedC1And2 Pcs
Total Received 500 Notes: $receivedC3And4 Pcs
Total Received Amount: BDT ${englishFormatter.format(totalReceivedAmt)}

CASSETTE WISE LOADED (REPLENISHMENT)
--------------------------------------------------
Cassette 1 Loaded: ${log.c1Loading} notes
Cassette 2 Loaded: ${log.c2Loading} notes
Cassette 3 Loaded: ${log.c3Loading} notes
Cassette 4 Loaded: ${log.c4Loading} notes

Total Loaded 1000 Notes: $loaded1000s Pcs
Total Loaded 500 Notes: $loaded500s Pcs
TOTAL LOAD AMOUNT: BDT ${englishFormatter.format(log.loadingAmount)}

==================================================
Remarks: ${log.remarks.ifBlank { "N/A" }}
==================================================
              DESIGNED BY TOUFIQ
==================================================
                                            """.trimIndent()
                                            
                                            val formattedDate = try {
                                                val parts = log.dateStr.split("-")
                                                if (parts.size == 3) {
                                                    val d = parts[2].toInt().toString()
                                                    val m = parts[1].toInt().toString()
                                                    val y = parts[0]
                                                    "$d-$m-$y"
                                                } else {
                                                    log.dateStr.replace("-", "_")
                                                }
                                            } catch (e: Exception) {
                                                log.dateStr.replace("-", "_")
                                            }
                                            val shortAtmName = log.atmName.split(" ").firstOrNull() ?: "ATM"
                                            val reportFileName = "$shortAtmName Replenishment Report-$formattedDate.pdf"

                                            previewTitle = "ATM LOAD REPORT"
                                            previewTextContent = slipContent
                                            onConfirmDownload = {
                                                PdfHelper.generateAndSharePdf(
                                                    context = context,
                                                    fileName = reportFileName,
                                                    title = "ATM LOAD REPORT",
                                                    content = slipContent
                                                )
                                            }
                                            showPreviewDialog = true
                                        },
                                        modifier = Modifier.background(GoldPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Download Report Slip", tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                    }
                                }
                                if (log.remarks.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Remarks: ${log.remarks}", fontSize = 11.sp, color = GoldPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPreviewDialog) {
        PdfPreviewDialog(
            title = previewTitle,
            headers = null,
            rows = null,
            textContent = previewTextContent,
            onDismiss = { showPreviewDialog = false },
            onDownload = {
                onConfirmDownload()
            }
        )
    }
}
