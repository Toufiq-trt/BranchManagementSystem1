package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BankingItem
import com.example.ui.theme.*
import com.example.util.PdfHelper
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsScreen(
    viewModel: BankingViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.allItems.collectAsStateWithLifecycle()
    val atmLogs by viewModel.atmLoadingLogs.collectAsStateWithLifecycle()
    val prizeBondLogs by viewModel.prizeBondLogs.collectAsStateWithLifecycle()
    val payOrderLogs by viewModel.payOrderLogs.collectAsStateWithLifecycle()
    val lettersIssued by viewModel.allLettersIssued.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    val now = System.currentTimeMillis()

    // Top Search Bar State
    var searchQuery by remember { mutableStateOf("") }

    // PDF Preview States
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewTitle by remember { mutableStateOf("") }
    var previewHeaders by remember { mutableStateOf<List<String>?>(null) }
    var previewRows by remember { mutableStateOf<List<List<String>>?>(null) }
    var previewTextContent by remember { mutableStateOf<String?>(null) }
    var onConfirmDownload by remember { mutableStateOf<() -> Unit>({}) }

    fun formatPdfFileName(fileName: String): String {
        val todayStr = SimpleDateFormat("d-M-yyyy", Locale.getDefault()).format(Date())
        val nameWithoutExt = fileName.substringBeforeLast(".pdf", fileName)
        
        if (nameWithoutExt.startsWith("destruction_log_")) {
            val datePart = nameWithoutExt.substringAfter("destruction_log_").replace("_", " ")
            val formattedDate = try {
                val formats = listOf("dd MMM yyyy", "yyyy-MM-dd", "dd-MM-yyyy")
                var parsedDate: Date? = null
                for (fmt in formats) {
                    try {
                        parsedDate = SimpleDateFormat(fmt, java.util.Locale.US).parse(datePart)
                        if (parsedDate != null) break
                    } catch (e: Exception) {}
                    try {
                        parsedDate = SimpleDateFormat(fmt, java.util.Locale.getDefault()).parse(datePart)
                        if (parsedDate != null) break
                    } catch (e: Exception) {}
                }
                if (parsedDate != null) {
                    SimpleDateFormat("d-M-yyyy", java.util.Locale.getDefault()).format(parsedDate)
                } else {
                    datePart.replace(" ", "-")
                }
            } catch (e: Exception) {
                datePart.replace(" ", "-")
            }
            return "Destruction Log-$formattedDate.pdf"
        }
        
        val cleanName = nameWithoutExt
            .split("_")
            .filter { it.isNotBlank() }
            .map { word -> word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() } }
            .joinToString(" ")
            
        return "$cleanName-$todayStr.pdf"
    }

    fun previewAndDownloadTable(
        title: String,
        headers: List<String>,
        rows: List<List<String>>,
        fileName: String
    ) {
        val finalFileName = formatPdfFileName(fileName)
        previewTitle = title
        previewHeaders = headers
        previewRows = rows
        previewTextContent = null
        onConfirmDownload = {
            PdfHelper.generateTablePdf(context, finalFileName, title, headers, rows)
        }
        showPreviewDialog = true
    }

    fun previewAndDownloadText(
        title: String,
        content: String,
        fileName: String
    ) {
        val finalFileName = formatPdfFileName(fileName)
        previewTitle = title
        previewHeaders = null
        previewRows = null
        previewTextContent = content
        onConfirmDownload = {
            PdfHelper.generateAndSharePdf(context, finalFileName, title, content)
        }
        showPreviewDialog = true
    }

    // Tabs corresponding to requested options
    val tabs = listOf(
        "Active Cards",
        "Active Cheques",
        "Active DPS Slips",
        "Active Pins",
        "1 Month Complete",
        "Destruction List",
        "Destruction Log",
        "Mailed List",
        "ATM History",
        "Prize Bonds",
        "Payorders"
    )
    var selectedReportTab by remember { mutableStateOf(0) }

    // Helper for computing 1 Month Complete date period (e.g. 1-5-2026 to 1-06-2026)
    fun getOneMonthCompletePeriod(receivedDate: Long): String {
        val startStr = sdf.format(Date(receivedDate))
        val thirtyDaysMs = 30L * 24 * 3600 * 1000
        val endStr = sdf.format(Date(receivedDate + thirtyDaysMs))
        return "$startStr to $endStr"
    }

    // Filter items based on the search bar query
    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) {
            items
        } else {
            val q = searchQuery.lowercase().trim()
            items.filter {
                it.customerName.lowercase().contains(q) ||
                it.accountNumber.lowercase().contains(q) ||
                it.phoneNumber.lowercase().contains(q) ||
                it.address.lowercase().contains(q) ||
                it.type.lowercase().contains(q)
            }
        }
    }

    // Filter logs based on search query
    val filteredAtmLogs = remember(atmLogs, searchQuery) {
        if (searchQuery.isBlank()) atmLogs else {
            val q = searchQuery.lowercase().trim()
            atmLogs.filter {
                it.atmName.lowercase().contains(q) ||
                it.dateStr.lowercase().contains(q) ||
                it.operatorName.lowercase().contains(q)
            }
        }
    }

    val filteredPrizeBonds = remember(prizeBondLogs, searchQuery) {
        if (searchQuery.isBlank()) prizeBondLogs else {
            val q = searchQuery.lowercase().trim()
            prizeBondLogs.filter {
                it.editedBy.lowercase().contains(q) ||
                it.dateStr.lowercase().contains(q)
            }
        }
    }

    val filteredPayOrders = remember(payOrderLogs, searchQuery) {
        if (searchQuery.isBlank()) payOrderLogs else {
            val q = searchQuery.lowercase().trim()
            payOrderLogs.filter {
                it.editedBy.lowercase().contains(q) ||
                it.dateStr.lowercase().contains(q)
            }
        }
    }

    // Core Filtering Rules for Security Items
    val activeCards = filteredItems.filter { it.type == "DEBIT_CARD" && !it.isDelivered && !it.isDestroyed }
    val activeCheques = filteredItems.filter { it.type == "CHEQUE_BOOK" && !it.isDelivered && !it.isDestroyed }
    val activeDpsSlips = filteredItems.filter { it.type == "DPS" && !it.isDelivered && !it.isDestroyed }
    val activePins = filteredItems.filter { it.type == "PIN" && !it.isDelivered && !it.isDestroyed }

    // 1 Month Complete List: Crossed 30 days but not yet 90 days, undelivered, undestroyed, not already marked as mailed
    val oneMonthCompleteList = filteredItems.filter {
        val ageDays = ((now - it.receivedDate) / (1000 * 3600 * 24)).toInt()
        !it.isDelivered && !it.isDestroyed && ageDays >= 30 && ageDays < 90 && !it.isLetterIssued
    }

    // Destruction List: Completed 90 days or more without delivery, undestroyed
    val destructionList = filteredItems.filter {
        val ageDays = ((now - it.receivedDate) / (1000 * 3600 * 24)).toInt()
        !it.isDelivered && !it.isDestroyed && ageDays >= 90
    }

    // Destruction Log: Grouped date-wise list of accounts marked as destroyed
    val destructionLogGrouped = remember(filteredItems) {
        filteredItems.filter { it.isDestroyed }
            .groupBy { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it.destroyAfter)) }
    }

    // Mailed List: Items that were in 1 Month Complete and are now marked as mailed
    val mailedList = filteredItems.filter { it.isLetterIssued }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main Screen Header
        Text(
            text = "ALL REPORTS PORTAL",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = GoldPrimary,
            letterSpacing = 1.sp
        )

        // 1. SEARCH BAR
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by Name, A/C, Address, Phone...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Search", tint = Color.Gray)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPrimary,
                unfocusedBorderColor = Color.Gray
            )
        )

        // Only show tab bar if we are NOT searching. If searching, we show a single grouped report view
        if (searchQuery.isBlank()) {
            // 2. SCROLLABLE TAB ROW
            ScrollableTabRow(
                selectedTabIndex = selectedReportTab,
                containerColor = SlateDark,
                contentColor = GoldPrimary,
                edgePadding = 8.dp,
                modifier = Modifier.background(SlateDark, RoundedCornerShape(8.dp))
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedReportTab == index,
                        onClick = { selectedReportTab = index },
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 3. TAB CONTENT OR SEARCH RESULTS
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (searchQuery.isNotBlank()) {
                // RENDER GROUPED SEARCH RESULTS FOR ACTIVE SECURITY ITEMS
                val totalSearchCount = activeCards.size + activePins.size + activeCheques.size + activeDpsSlips.size
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SEARCH RESULTS ($totalSearchCount ACTIVE ITEMS)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = GoldPrimary
                    )

                    Button(
                        onClick = {
                            val results = activeCards + activePins + activeCheques + activeDpsSlips
                            if (results.isEmpty()) return@Button
                            val headers = listOf("TYPE", "AC NUMBER", "NAME", "PHONE NUMBER", "ADDRESS")
                            val rows = results.map { item ->
                                listOf(item.type, item.accountNumber, item.customerName, item.phoneNumber, item.address)
                            }
                            previewAndDownloadTable(
                                title = "SEARCH RESULTS FOR '${searchQuery.uppercase()}'",
                                headers = headers,
                                rows = rows,
                                fileName = "search_results_report.pdf"
                            )
                        },
                        enabled = totalSearchCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Results", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (totalSearchCount == 0) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No active matching records found.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1. DEBIT CARDS Group (First)
                        if (activeCards.isNotEmpty()) {
                            item {
                                Surface(
                                    color = SlateDark,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "DEBIT CARDS (${activeCards.size})",
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            items(activeCards) { item ->
                                SearchResultCard(item)
                            }
                        }

                        // 2. PINS Group (Second)
                        if (activePins.isNotEmpty()) {
                            item {
                                Surface(
                                    color = SlateDark,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text(
                                        text = "PINS (${activePins.size})",
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            items(activePins) { item ->
                                SearchResultCard(item)
                            }
                        }

                        // 3. CHEQUE BOOKS Group (Third)
                        if (activeCheques.isNotEmpty()) {
                            item {
                                Surface(
                                    color = SlateDark,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text(
                                        text = "CHEQUE BOOKS (${activeCheques.size})",
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            items(activeCheques) { item ->
                                SearchResultCard(item)
                            }
                        }

                        // 4. DPS SLIPS Group (Fourth)
                        if (activeDpsSlips.isNotEmpty()) {
                            item {
                                Surface(
                                    color = SlateDark,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text(
                                        text = "DPS SLIPS (${activeDpsSlips.size})",
                                        fontWeight = FontWeight.Bold,
                                        color = GoldPrimary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            items(activeDpsSlips) { item ->
                                SearchResultCard(item)
                            }
                        }
                    }
                }
            } else {
                // ---- TABS 0 - 3: ACTIVE ITEMS ----
                when (selectedReportTab) {
                    0, 1, 2, 3 -> {
                        val activeList = when (selectedReportTab) {
                            0 -> activeCards
                            1 -> activeCheques
                            2 -> activeDpsSlips
                            else -> activePins
                        }
                        val itemTypeLabel = when (selectedReportTab) {
                            0 -> "ACTIVE DEBIT CARDS"
                            1 -> "ACTIVE CHEQUE BOOKS"
                            2 -> "ACTIVE DPS SLIPS"
                            else -> "ACTIVE PINS"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$itemTypeLabel (${activeList.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GoldPrimary
                            )

                            Button(
                                onClick = {
                                    if (activeList.isEmpty()) return@Button
                                    val headers = listOf("TYPE", "AC NUMBER", "NAME", "PHONE NUMBER", "ADDRESS")
                                    val rows = activeList.map { item ->
                                        listOf(item.type, item.accountNumber, item.customerName, item.phoneNumber, item.address)
                                    }
                                    val pdfFilename = "${itemTypeLabel.lowercase().replace(" ", "_")}_report.pdf"
                                    previewAndDownloadTable(itemTypeLabel, headers, rows, pdfFilename)
                                },
                                enabled = activeList.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export PDF Table", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (activeList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No matching active records found.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(activeList) { item ->
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(item.customerName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                Text("Rec: ${sdf.format(Date(item.receivedDate))}", fontSize = 11.sp, color = GoldLight)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("A/C Number: ${item.accountNumber}", fontSize = 13.sp)
                                            Text("Phone: ${item.phoneNumber}", fontSize = 12.sp)
                                            Text("Address: ${item.address}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---- TAB 4: 1 MONTH COMPLETE ----
                    4 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1 Month Complete List (${oneMonthCompleteList.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GoldPrimary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Mark All as Mailed Button
                                if (oneMonthCompleteList.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            oneMonthCompleteList.forEach { item ->
                                                viewModel.markAsLetterIssued(item)
                                            }
                                            Toast.makeText(context, "All ${oneMonthCompleteList.size} items marked as Mailed!", Toast.LENGTH_LONG).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent, contentColor = SlateDark),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Mark All & Mailed", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (oneMonthCompleteList.isEmpty()) return@Button
                                        val headers = listOf("TYPE", "AC NUMBER", "NAME", "PHONE NUMBER", "ADDRESS", "1 MONTH COMPLETE")
                                        val rows = oneMonthCompleteList.map { item ->
                                            listOf(
                                                item.type,
                                                item.accountNumber,
                                                item.customerName,
                                                item.phoneNumber,
                                                item.address,
                                                getOneMonthCompletePeriod(item.receivedDate)
                                            )
                                        }
                                        previewAndDownloadTable("1 MONTH COMPLETE EXPIRY", headers, rows, "one_month_complete_report.pdf")
                                    },
                                    enabled = oneMonthCompleteList.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (oneMonthCompleteList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No records have crossed 30 days without delivery.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(oneMonthCompleteList) { item ->
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(item.customerName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                Text("[${item.type}]", fontSize = 11.sp, color = GoldLight)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("A/C Number: ${item.accountNumber}", fontSize = 13.sp)
                                            Text("Phone: ${item.phoneNumber}", fontSize = 12.sp)
                                            Text("Address: ${item.address}", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "1 Month Complete: ${getOneMonthCompletePeriod(item.receivedDate)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = GreenAccent
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---- TAB 5: DESTRUCTION LIST ----
                    5 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Destruction List (>= 90 Days) (${destructionList.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = RedAccent
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (destructionList.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            destructionList.forEach { item ->
                                                viewModel.updateBankingItem(
                                                    item.copy(
                                                        isDestroyed = true,
                                                        destroyAfter = System.currentTimeMillis()
                                                    )
                                                )
                                            }
                                            Toast.makeText(context, "All ${destructionList.size} items moved to Destruction Log!", Toast.LENGTH_LONG).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RedAccent, contentColor = Color.White),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Mark All Destroyed", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (destructionList.isEmpty()) return@Button
                                        val headers = listOf("TYPE", "AC NUMBER", "NAME", "PHONE NUMBER", "ADDRESS")
                                        val rows = destructionList.map { item ->
                                            listOf(item.type, item.accountNumber, item.customerName, item.phoneNumber, item.address)
                                        }
                                        previewAndDownloadTable("DESTRUCTION PENDING LIST", headers, rows, "destruction_pending_list.pdf")
                                    },
                                    enabled = destructionList.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (destructionList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No items pending destruction (age >= 90 days).", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(destructionList) { item ->
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(item.customerName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                Text("Rec: ${sdf.format(Date(item.receivedDate))}", fontSize = 11.sp, color = RedAccent)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("A/C Number: ${item.accountNumber} | [${item.type}]", fontSize = 13.sp)
                                            Text("Phone: ${item.phoneNumber}", fontSize = 12.sp)
                                            Text("Address: ${item.address}", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val ageDays = ((now - item.receivedDate) / (1000 * 3600 * 24)).toInt()
                                            Text("OVERDUE AGE: $ageDays Days", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RedAccent)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---- TAB 6: DESTRUCTION LOG (GROUPED BY DESTRUCTION DATE) ----
                    6 -> {
                        Text(
                            text = "Destruction Audit Log",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = GoldPrimary
                        )

                        if (destructionLogGrouped.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No destruction logs registered in the database.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                destructionLogGrouped.forEach { (destructionDateStr, destroyedItemsList) ->
                                    item {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = SlateDark),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text("Destruction Date: $destructionDateStr", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 13.sp)
                                                        Text("Destroyed Accounts: ${destroyedItemsList.size}", fontSize = 11.sp, color = Color.White)
                                                    }

                                                    Button(
                                                        onClick = {
                                                            val headers = listOf("TYPE", "AC NUMBER", "NAME", "PHONE NUMBER", "ADDRESS")
                                                            val rows = destroyedItemsList.map { item ->
                                                                listOf(item.type, item.accountNumber, item.customerName, item.phoneNumber, item.address)
                                                            }
                                                            val pdfFilename = "destruction_log_${destructionDateStr.replace(" ", "_")}.pdf"
                                                            previewAndDownloadTable("DESTRUCTED ON $destructionDateStr", headers, rows, pdfFilename)
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                                                        shape = RoundedCornerShape(4.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Download PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))

                                                // Small compact display of the accounts inside this date
                                                destroyedItemsList.forEach { item ->
                                                    Text(
                                                        text = "• A/C: ${item.accountNumber} | ${item.customerName} | [${item.type}]",
                                                        fontSize = 11.sp,
                                                        color = Color.LightGray,
                                                        modifier = Modifier.padding(bottom = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---- TAB 7: MAILED LIST ----
                    7 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mailed / Letters Issued (${mailedList.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GoldPrimary
                            )

                            Button(
                                onClick = {
                                    if (mailedList.isEmpty()) return@Button
                                    val headers = listOf("TYPE", "AC NUMBER", "NAME", "PHONE NUMBER", "ADDRESS", "1 MONTH COMPLETE")
                                    val rows = mailedList.map { item ->
                                        listOf(
                                            item.type,
                                            item.accountNumber,
                                            item.customerName,
                                            item.phoneNumber,
                                            item.address,
                                            getOneMonthCompletePeriod(item.receivedDate)
                                        )
                                    }
                                    previewAndDownloadTable("MAILED SECURITY ITEMS LOG", headers, rows, "mailed_items_list.pdf")
                                },
                                enabled = mailedList.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (mailedList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No items logged in mailed/letter issued list.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(mailedList) { item ->
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(item.customerName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                Text("[${item.type}]", fontSize = 11.sp, color = GreenAccent)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("A/C Number: ${item.accountNumber}", fontSize = 13.sp)
                                            Text("Phone: ${item.phoneNumber}", fontSize = 12.sp)
                                            Text("Address: ${item.address}", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Status: Letter Issued (Mailed)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = GreenAccent
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---- TAB 8: ATM HISTORY ----
                    8 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ATM Loading Logs (${filteredAtmLogs.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GoldPrimary
                            )

                            Button(
                                onClick = {
                                    if (filteredAtmLogs.isEmpty()) return@Button
                                    val headers = listOf("ATM NAME", "DATE", "TOTAL LOAD", "C1 LOAD", "C2 LOAD", "C3 LOAD", "C4 LOAD", "OPERATOR")
                                    val rows = filteredAtmLogs.map { log ->
                                        listOf(
                                            log.atmName,
                                            log.dateStr,
                                            "${log.loadingAmount} BDT",
                                            log.c1Loading.toString(),
                                            log.c2Loading.toString(),
                                            log.c3Loading.toString(),
                                            log.c4Loading.toString(),
                                            log.operatorName
                                        )
                                    }
                                    val textContent = "ATM LOADING REGISTER AUDIT\n\n" + filteredAtmLogs.joinToString("\n") {
                                        "${it.atmName} | ${it.dateStr} | Total: ${it.loadingAmount} | By: ${it.operatorName}"
                                    }
                                    previewAndDownloadText("ATM LOADING LOG", textContent, "atm_load_history.pdf")
                                },
                                enabled = filteredAtmLogs.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (filteredAtmLogs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No ATM loading history recorded.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(filteredAtmLogs) { log ->
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(log.atmName, fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 14.sp)
                                                Text(log.dateStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Total Loaded: ${log.loadingAmount} BDT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Cassette split: C1: ${log.c1Loading} | C2: ${log.c2Loading} | C3: ${log.c3Loading} | C4: ${log.c4Loading}", fontSize = 11.sp)
                                            Text("Operator EIN: ${log.operatorName}", fontSize = 11.sp)
                                            if (log.remarks.isNotBlank()) {
                                                Text("Notes: ${log.remarks}", fontSize = 11.sp, color = Color.LightGray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---- TAB 9: PRIZE BONDS ----
                    9 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Prize Bonds Audit Log (${filteredPrizeBonds.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GoldPrimary
                            )

                            Button(
                                onClick = {
                                    if (filteredPrizeBonds.isEmpty()) return@Button
                                    val reportText = "PRIZE BOND STOCK ADJUSTMENTS\n\n" + filteredPrizeBonds.joinToString("\n") {
                                        "Date: ${it.dateStr} | Adjusted: ${it.previousQuantity} -> ${it.newQuantity} | By: ${it.editedBy}"
                                    }
                                    previewAndDownloadText("PRIZE BOND AUDIT TRAIL", reportText, "prize_bonds_log.pdf")
                                },
                                enabled = filteredPrizeBonds.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (filteredPrizeBonds.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No Prize Bond inventory stock audits recorded.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(filteredPrizeBonds) { log ->
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Adjusted: ${log.previousQuantity} ➔ ${log.newQuantity} Bonds", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                Text(log.dateStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("By Officer: ${log.editedBy}", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---- TAB 10: PAYORDERS ----
                    10 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Payorders Audit Log (${filteredPayOrders.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GoldPrimary
                            )

                            Button(
                                onClick = {
                                    if (filteredPayOrders.isEmpty()) return@Button
                                    val reportText = "PAY ORDER STOCK ADJUSTMENTS\n\n" + filteredPayOrders.joinToString("\n") {
                                        "Date: ${it.dateStr} | Adjusted: ${it.previousQuantity} -> ${it.newQuantity} | By: ${it.editedBy}"
                                    }
                                    previewAndDownloadText("PAY ORDER AUDIT TRAIL", reportText, "payorders_log.pdf")
                                },
                                enabled = filteredPayOrders.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (filteredPayOrders.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No Payorder inventory stock audits recorded.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(filteredPayOrders) { log ->
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Adjusted: ${log.previousQuantity} ➔ ${log.newQuantity} Slips", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                Text(log.dateStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("By Officer: ${log.editedBy}", fontSize = 11.sp)
                                        }
                                    }
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
            headers = previewHeaders,
            rows = previewRows,
            textContent = previewTextContent,
            onDismiss = { showPreviewDialog = false },
            onDownload = {
                onConfirmDownload()
            }
        )
    }
}

@Composable
fun SearchResultCard(item: BankingItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.customerName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Surface(
                    color = GoldPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = item.type,
                        fontSize = 10.sp,
                        color = GoldLight,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("A/C Number: ${item.accountNumber}", fontSize = 13.sp)
            Text("Phone: ${item.phoneNumber}", fontSize = 12.sp)
            Text("Address: ${item.address}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}
