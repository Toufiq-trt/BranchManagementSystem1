package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: BankingViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.allItems.collectAsStateWithLifecycle()
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val prizeBondVal by viewModel.prizeBondQty.collectAsState()
    val payOrderVal by viewModel.payOrderQty.collectAsState()
    val atmLogs by viewModel.atmLoadingLogs.collectAsStateWithLifecycle()
    val allHunting by viewModel.allHunting.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Counts
    val now = System.currentTimeMillis()
    val pendingDebit = items.filter { it.type == "DEBIT_CARD" && !it.isDelivered && !it.isDestroyed }.size
    val pendingPin = items.filter { it.type == "PIN" && !it.isDelivered && !it.isDestroyed }.size
    val pendingCheque = items.filter { it.type == "CHEQUE_BOOK" && !it.isDelivered && !it.isDestroyed }.size
    val pendingDps = items.filter { it.type == "DPS" && !it.isDelivered && !it.isDestroyed }.size
    val pendingTasks = tasks.filter { !it.isCompleted }.size
    
    // Today's ATM loading total
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayAtmLoading = atmLogs.filter { it.dateStr == todayStr }.sumOf { it.loadingAmount }

    // PDF Preview States
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewTitle by remember { mutableStateOf("") }
    var previewTextContent by remember { mutableStateOf<String?>(null) }
    var onConfirmDownload by remember { mutableStateOf<() -> Unit>({}) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Premium Gradient Banner Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(SlateDark, SlateSecondary)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(
                    text = "TOUFIQ'S BALANCING SYSTEM",
                    color = GoldPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Welcome back to",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = "Toufiq's Smart Banking Tracker",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                tint = GoldPrimary.copy(alpha = 0.2f),
                modifier = Modifier
                    .size(75.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 10.dp)
                    .clickable { viewModel.checkForUpdatesSilently() }
            )
        }

        // OTA Software Update Prominent Banner
        if (viewModel.isUpdateAvailable) {
            Card(
                colors = CardDefaults.cardColors(containerColor = GoldPrimary.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("settings") },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update Icon",
                        tint = GoldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Software Update Available: v${viewModel.latestUpdateInfo?.versionName ?: ""}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = GoldPrimary
                        )
                        Text(
                            text = "Tap to review release notes and install the latest OTA build.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Quick Statistics Grid Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Branch Balance Metrics",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary,
                letterSpacing = 0.5.sp
            )
            IconButton(
                onClick = {
                    val activeDebitList = items.filter { it.type == "DEBIT_CARD" && !it.isDelivered && !it.isDestroyed }
                    val activePinList = items.filter { it.type == "PIN" && !it.isDelivered && !it.isDestroyed }
                    val activeDpsList = items.filter { it.type == "DPS" && !it.isDelivered && !it.isDestroyed }
                    val activeChequeList = items.filter { it.type == "CHEQUE_BOOK" && !it.isDelivered && !it.isDestroyed }

                    val reportDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
                    val reportTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    val officerName = viewModel.currentUser?.name ?: "Officer Toufiq"

                    val formattedDate = SimpleDateFormat("d-M-yyyy", Locale.getDefault()).format(Date())
                    val pdfFilename = "Branch Share Active-$formattedDate.pdf"
                    val pdfTitle = "BRANCH SMART ACTIVE BALANCE REPORT"

                    val fullReportText = buildString {
                        appendLine("========================================")
                        appendLine("      TOUFIQ'S SMART BANKING SUITE      ")
                        appendLine("========================================")
                        appendLine("Date: $reportDate")
                        appendLine("Time: $reportTime")
                        appendLine("Officer Name: $officerName")
                        appendLine("========================================")
                        appendLine()
                        
                        appendLine("--- ACTIVE DEBIT CARDS (${activeDebitList.size}) ---")
                        if (activeDebitList.isEmpty()) {
                            appendLine("No active debit cards.")
                        } else {
                            activeDebitList.forEachIndexed { idx, item ->
                                appendLine("${idx + 1}. Name: ${item.customerName} | A/C: ${item.accountNumber} | Phone: ${item.phoneNumber}")
                            }
                        }
                        appendLine()
                        
                        appendLine("--- ACTIVE PINS (${activePinList.size}) ---")
                        if (activePinList.isEmpty()) {
                            appendLine("No active PINs.")
                        } else {
                            activePinList.forEachIndexed { idx, item ->
                                appendLine("${idx + 1}. Name: ${item.customerName} | A/C: ${item.accountNumber} | Phone: ${item.phoneNumber}")
                            }
                        }
                        appendLine()
                        
                        appendLine("--- ACTIVE DPS SLIPS (${activeDpsList.size}) ---")
                        if (activeDpsList.isEmpty()) {
                            appendLine("No active DPS Slips.")
                        } else {
                            activeDpsList.forEachIndexed { idx, item ->
                                appendLine("${idx + 1}. Name: ${item.customerName} | A/C: ${item.accountNumber} | Phone: ${item.phoneNumber}")
                            }
                        }
                        appendLine()
                        
                        appendLine("--- ACTIVE CHEQUE BOOKS (${activeChequeList.size}) ---")
                        if (activeChequeList.isEmpty()) {
                            appendLine("No active Cheque Books.")
                        } else {
                            activeChequeList.forEachIndexed { idx, item ->
                                appendLine("${idx + 1}. Name: ${item.customerName} | A/C: ${item.accountNumber} | Phone: ${item.phoneNumber}")
                            }
                        }
                        appendLine()
                        
                        appendLine("--- ACTIVE PRIZE BONDS ---")
                        appendLine("Current vault quantity: $prizeBondVal Pcs")
                        appendLine()
                        
                        appendLine("--- ACTIVE PAYORDERS ---")
                        appendLine("Current vault quantity: $payOrderVal Pcs")
                        appendLine()
                        appendLine("========================================")
                        appendLine("Generated Securely - Operations Registry")
                    }
                    
                    previewTitle = pdfTitle
                    previewTextContent = fullReportText
                    onConfirmDownload = {
                        com.example.util.PdfHelper.generateAndSharePdf(context, pdfFilename, pdfTitle, fullReportText)
                    }
                    showPreviewDialog = true
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = "Share Branch Balance PDF",
                    tint = GoldPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 8-Card Statistics & Calculators Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                StatCard(
                    title = "DEBIT CARD & PIN",
                    value = pendingDebit.toString(),
                    icon = Icons.Default.CreditCard,
                    iconColor = GoldPrimary,
                    onClick = { onNavigate("debit_card") }
                )
            }
            item {
                StatCard(
                    title = "Cheque Books",
                    value = pendingCheque.toString(),
                    icon = Icons.Default.Book,
                    iconColor = GreenAccent,
                    onClick = { onNavigate("cheque_book") }
                )
            }
            item {
                StatCard(
                    title = "DPS SLIP",
                    value = pendingDps.toString(),
                    icon = Icons.Default.FolderZip,
                    iconColor = GoldLight,
                    onClick = { onNavigate("dps") }
                )
            }
            item {
                StatCard(
                    title = "Prize Bonds",
                    value = "$prizeBondVal Pcs",
                    icon = Icons.Default.ConfirmationNumber,
                    iconColor = GoldPrimary,
                    onClick = { onNavigate("prize_bond") }
                )
            }
            item {
                StatCard(
                    title = "Pay Orders",
                    value = "$payOrderVal Pcs",
                    icon = Icons.Default.ReceiptLong,
                    iconColor = OrangeAccent,
                    onClick = { onNavigate("pay_order") }
                )
            }
            item {
                StatCard(
                    title = "FD CALCULATOR",
                    value = "Fixed Deposit",
                    icon = Icons.Default.Calculate,
                    iconColor = GoldPrimary,
                    onClick = { onNavigate("fd_calc") }
                )
            }
            item {
                StatCard(
                    title = "Loan Calculator",
                    value = "EMI Estimates",
                    icon = Icons.Default.Calculate,
                    iconColor = GreenAccent,
                    onClick = { onNavigate("loan_calc") }
                )
            }
            item {
                StatCard(
                    title = "DPS Calculator",
                    value = "DPS Savings",
                    icon = Icons.Default.Calculate,
                    iconColor = GoldLight,
                    onClick = { onNavigate("dps_calc") }
                )
            }
        }

        // Branch Operations & Marketing (ATM Replenishment & Customer Hunting)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Branch Operations & Marketing",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary,
                letterSpacing = 0.5.sp
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ATM Replenishment Navigation
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate("atm_calc") },
                    colors = CardDefaults.cardColors(containerColor = SlateDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(GoldPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LocalAtm, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("ATM REPLENISHMENT", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                            Text("Replenishment Calc", fontSize = 10.sp, color = Color.LightGray)
                        }
                    }
                }

                // Customer Hunting Navigation
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate("hunting") },
                    colors = CardDefaults.cardColors(containerColor = SlateDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(GreenAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = GreenAccent, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("CUSTOMER FINDINGS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                            Text("Customer Lead Finder", fontSize = 10.sp, color = Color.LightGray)
                        }
                    }
                }
            }
        }

        // Today's Bottom Actions Row (To Do Tasks & All Forms)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { onNavigate("todo_list") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SlateSecondary, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PlaylistAddCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("To Do Tasks", fontSize = 13.sp)
            }

            Button(
                onClick = { onNavigate("forms") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Feed, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ALL FORMS", fontSize = 13.sp)
            }
        }
    }

    if (showPreviewDialog) {
        PdfPreviewDialog(
            title = previewTitle,
            textContent = previewTextContent,
            onDismiss = { showPreviewDialog = false },
            onDownload = { onConfirmDownload() }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
