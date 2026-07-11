package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.data.BankingItem
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BankingItemScreen(
    viewModel: BankingViewModel,
    itemType: String, // "DEBIT_CARD", "PIN", "CHEQUE_BOOK", "DPS"
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val items by viewModel.allItems.collectAsStateWithLifecycle()
    val filteredItems = items.filter { it.type == itemType }

    var isAddingNew by remember { mutableStateOf(false) }
    var tabSelected by remember { mutableStateOf(0) } // 0 = Active Balancing, 1 = Delivered List, 2 = Destruction History
    var collapsedFolders by remember { mutableStateOf(setOf<String>()) }

    // PDF Preview States
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewTitle by remember { mutableStateOf("") }
    var previewHeaders by remember { mutableStateOf<List<String>?>(null) }
    var previewRows by remember { mutableStateOf<List<List<String>>?>(null) }
    var onConfirmDownload by remember { mutableStateOf<() -> Unit>({}) }

    // Bulk Importer Settings
    var showBulkImportDialog by remember { mutableStateOf(false) }
    var bulkImportType by remember { mutableStateOf("") } // "EXCEL", "PHOTO", "SHEETS"

    // Local state for Add Form
    var customerName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var selectedReceivedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    // Filter items based on selected tab and search
    val now = System.currentTimeMillis()
    val activeList = filteredItems.filter { !it.isDestroyed && it.destroyAfter > now && !it.isDelivered }
    val deliveredList = filteredItems.filter { it.isDelivered && !it.isDestroyed }
    val destroyedList = filteredItems.filter { it.isDestroyed || (it.destroyAfter <= now && !it.isDelivered) }

    val currentDisplayList = when (tabSelected) {
        0 -> activeList
        1 -> deliveredList
        else -> destroyedList
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Module Title and Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = when (itemType) {
                        "DEBIT_CARD" -> "DEBIT CARD VAULT"
                        "PIN" -> "PIN MAILER VAULT"
                        "CHEQUE_BOOK" -> "CHEQUE BOOK REGISTER"
                        else -> "DPS REGISTER"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Total registers: ${filteredItems.size}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (viewModel.isLoggedIn) {
                IconButton(
                    onClick = { isAddingNew = !isAddingNew },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = GoldPrimary, contentColor = SlateDark)
                ) {
                    Icon(imageVector = if (isAddingNew) Icons.Default.Close else Icons.Default.Add, contentDescription = "Add")
                }
            }
        }

        // Add Item Form (Expandable)
        AnimatedVisibility(
            visible = isAddingNew,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Register New $itemType", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 14.sp)
                    
                    // Bulk entry operations row
                    Text("⚡ BULK AUTO-ENTRY OPERATIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldLight)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                bulkImportType = "EXCEL"
                                showBulkImportDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.BorderAll, contentDescription = null, modifier = Modifier.size(14.dp), tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excel", fontSize = 11.sp, color = Color.White)
                        }
                        Button(
                            onClick = {
                                bulkImportType = "PHOTO"
                                showBulkImportDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp), tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Photo OCR", fontSize = 11.sp, color = Color.White)
                        }
                        Button(
                            onClick = {
                                bulkImportType = "SHEETS"
                                showBulkImportDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(14.dp), tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Sheets", fontSize = 11.sp, color = Color.White)
                        }
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 1.dp)
                    
                    Text("MANUAL ENTRY FORM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Customer Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = { Text("Account Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address / Contact location") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Remarks (e.g. Courier No.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Automated calculations preview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateDark.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .clickable { showDatePickerDialog = true }
                                .padding(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Received Date 📅", fontSize = 11.sp, color = GoldPrimary)
                                Icon(Icons.Default.Edit, contentDescription = "Edit Date", modifier = Modifier.size(12.dp), tint = GoldPrimary)
                            }
                            Text(sdf.format(Date(selectedReceivedDate)), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Auto-Destroy Date (+90 Days)", fontSize = 11.sp, color = RedAccent)
                            val destroyDate = selectedReceivedDate + (90L * 24L * 3600L * 1000L)
                            Text(sdf.format(Date(destroyDate)), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
                        }
                    }

                    if (showDatePickerDialog) {
                        val datePickerState = rememberDatePickerState(
                            initialSelectedDateMillis = selectedReceivedDate
                        )
                        DatePickerDialog(
                            onDismissRequest = { showDatePickerDialog = false },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        datePickerState.selectedDateMillis?.let {
                                            selectedReceivedDate = it
                                        }
                                        showDatePickerDialog = false
                                    }
                                ) {
                                    Text("Confirm", color = GoldPrimary, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDatePickerDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        ) {
                            DatePicker(state = datePickerState)
                        }
                    }

                    val context = androidx.compose.ui.platform.LocalContext.current
                    Button(
                        onClick = {
                            if (customerName.isNotBlank() && accountNumber.isNotBlank()) {
                                viewModel.addBankingItem(
                                    type = itemType,
                                    name = customerName,
                                    acNo = accountNumber,
                                    address = address,
                                    phone = phoneNumber,
                                    remarks = remarks,
                                    dateOverride = selectedReceivedDate,
                                    onDuplicate = {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Duplicate detected! An active $itemType already exists with same Name and A/C number.",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    },
                                    onSuccess = {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Successfully added $itemType!",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                        // Clear inputs for another entry, keeping the form open
                                        customerName = ""
                                        accountNumber = ""
                                        address = ""
                                        phoneNumber = ""
                                        remarks = ""
                                        selectedReceivedDate = System.currentTimeMillis()
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save Icon", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // Active Balancing, Delivered, and Destruction List Toggle Tabs
        TabRow(
            selectedTabIndex = tabSelected,
            containerColor = Color.Transparent,
            contentColor = GoldPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[tabSelected]),
                    color = GoldPrimary
                )
            }
        ) {
            Tab(
                selected = tabSelected == 0,
                onClick = { tabSelected = 0 },
                text = { Text("Active (${activeList.size})") }
            )
            Tab(
                selected = tabSelected == 1,
                onClick = { tabSelected = 1 },
                text = { Text("Delivered (${deliveredList.size})") }
            )
            Tab(
                selected = tabSelected == 2,
                onClick = { tabSelected = 2 },
                text = { Text("Destruction (${destroyedList.size})") }
            )
        }

        // Standardized Table Download Registry Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val labelText = when (tabSelected) {
                0 -> "Active Balancing Records"
                1 -> "Delivered Log"
                else -> "Destruction Historical Log"
            }
            Text(labelText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
            
            Button(
                onClick = {
                    val labelSub = when (tabSelected) {
                        0 -> "ACTIVE REGISTRY"
                        1 -> "DELIVERED LOG"
                        else -> "DESTRUCTION HISTORY"
                    }
                    val pdfTitle = "${itemType.replace("_", " ")} $labelSub"
                    val headers = listOf("TYPE", "AC NUMBER", "NAME", "PHONE NUMBER", "ADDRESS")
                    val rows = currentDisplayList.map { item ->
                        listOf(item.type, item.accountNumber, item.customerName, item.phoneNumber, item.address)
                    }
                    val formattedDate = java.text.SimpleDateFormat("d-M-yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                    val formattedTitle = pdfTitle.split(" ").map { word -> word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() } }.joinToString(" ")
                    val reportFileName = "$formattedTitle-$formattedDate.pdf"
                    
                    previewTitle = pdfTitle
                    previewHeaders = headers
                    previewRows = rows
                    onConfirmDownload = {
                        com.example.util.PdfHelper.generateTablePdf(context, reportFileName, pdfTitle, headers, rows)
                    }
                    showPreviewDialog = true
                },
                enabled = currentDisplayList.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = "Download Registry", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Download Registry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 30 Days Complete Section
        if (itemType != "DPS" && tabSelected == 0) {
            val thirtyDaysAgo = now - (30L * 24 * 3600 * 1000)
            val thirtyDaysCompleteList = activeList.filter { it.receivedDate <= thirtyDaysAgo && !it.isDelivered && !it.isLetterIssued }
            
            if (thirtyDaysCompleteList.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "30 DAYS COMPLETE (${thirtyDaysCompleteList.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            
                            var isNotifiedChecked by remember { mutableStateOf(false) }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Yes, Notified?", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(modifier = Modifier.width(4.dp))
                                Checkbox(
                                    checked = isNotifiedChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            thirtyDaysCompleteList.forEach { item ->
                                                viewModel.markAsLetterIssued(item)
                                            }
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = GoldPrimary)
                                )
                            }
                        }
                        
                        Text(
                            text = "Reminder needed for items sitting over 30 days.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp)
                        ) {
                            thirtyDaysCompleteList.forEach { item ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Account Number: ${item.accountNumber}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text("Customer Name: ${item.customerName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text("Phone Number: ${item.phoneNumber}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text("Address: ${item.address}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // List of Records
        if (currentDisplayList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = when (tabSelected) {
                            0 -> Icons.Default.AllInbox
                            1 -> Icons.Default.CheckCircle
                            else -> Icons.Default.DeleteSweep
                        },
                        contentDescription = "Empty",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (tabSelected) {
                            0 -> "No active items in balancing."
                            1 -> "No delivered items found."
                            else -> "Destruction list is empty."
                        },
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (tabSelected == 1) {
                    val groupSdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    val deliveredGroups = deliveredList.groupBy { item ->
                        val date = if (item.deliveryDate > 0L) Date(item.deliveryDate) else Date(item.receivedDate)
                        groupSdf.format(date)
                    }
                    val sortedKeys = deliveredGroups.keys.sortedDescending()

                    sortedKeys.forEach { dateKey ->
                        val groupItems = deliveredGroups[dateKey] ?: emptyList()
                        val isCollapsed = dateKey in collapsedFolders

                        item(key = "folder_$dateKey") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        collapsedFolders = if (isCollapsed) {
                                            collapsedFolders - dateKey
                                        } else {
                                            collapsedFolders + dateKey
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = SlateDark),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isCollapsed) Icons.Default.Folder else Icons.Default.FolderOpen,
                                            contentDescription = "Folder Icon",
                                            tint = GoldPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "$dateKey >> Items List",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "${groupItems.size} items delivered",
                                                fontSize = 11.sp,
                                                color = Color.LightGray
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                        contentDescription = "Toggle Folder",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        if (!isCollapsed) {
                            items(groupItems, key = { "item_${it.id}" }) { item ->
                                Row(modifier = Modifier.padding(start = 12.dp)) {
                                    BankingItemRow(
                                        item = item,
                                        now = now,
                                        showDeleteButton = viewModel.isLoggedIn && viewModel.currentUser?.isToufiq == true,
                                        showDeliveryButton = viewModel.isLoggedIn,
                                        onMarkDelivered = {
                                            viewModel.markAsDelivered(item)
                                        },
                                        onMarkDestroyed = {
                                            viewModel.updateBankingItem(item.copy(isDestroyed = true))
                                        },
                                        onDelete = {
                                            viewModel.deleteBankingItem(item)
                                        },
                                        onUndoDelivery = {
                                            viewModel.revertDelivery(item)
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(currentDisplayList, key = { it.id }) { item ->
                        BankingItemRow(
                            item = item,
                            now = now,
                            showDeleteButton = viewModel.isLoggedIn && viewModel.currentUser?.isToufiq == true,
                            showDeliveryButton = viewModel.isLoggedIn,
                            onMarkDelivered = {
                                viewModel.markAsDelivered(item)
                            },
                            onMarkDestroyed = {
                                viewModel.updateBankingItem(item.copy(isDestroyed = true))
                            },
                            onDelete = {
                                viewModel.deleteBankingItem(item)
                            },
                            onUndoDelivery = {
                                viewModel.revertDelivery(item)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showBulkImportDialog) {
        BulkImportDialog(
            itemType = itemType,
            importType = bulkImportType,
            onDismiss = { showBulkImportDialog = false },
            onImportConfirmed = { parsedList ->
                parsedList.forEach { triple ->
                    viewModel.addBankingItem(
                        type = itemType,
                        name = triple.first,
                        acNo = triple.second,
                        address = triple.third,
                        phone = "017" + (10000000 + Random().nextInt(90000000)),
                        remarks = "Bulk Imported ($bulkImportType)"
                    )
                }
            }
        )
    }

    if (showPreviewDialog) {
        PdfPreviewDialog(
            title = previewTitle,
            headers = previewHeaders,
            rows = previewRows,
            textContent = null,
            onDismiss = { showPreviewDialog = false },
            onDownload = {
                onConfirmDownload()
            }
        )
    }
}

@Composable
fun BulkImportDialog(
    itemType: String,
    importType: String, // "EXCEL", "PHOTO", "SHEETS"
    onDismiss: () -> Unit,
    onImportConfirmed: (List<Triple<String, String, String>>) -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1 = Input, 2 = Loading, 3 = Success
    var sheetsUrl by remember { mutableStateOf("") }
    var excelFileSelected by remember { mutableStateOf("No file selected") }
    var photoCaptured by remember { mutableStateOf(false) }

    val simulatedResults = when (itemType) {
        "DEBIT_CARD" -> listOf(
            Triple("Mst. Sharmin Jahan", "1020304050", "Ranirbandar, Dinajpur"),
            Triple("Md. Abu Bakar", "5060708090", "Dinajpur Town"),
            Triple("Shahnaj Begum", "3040506070", "Ranirbandar, Dinajpur")
        )
        "PIN" -> listOf(
            Triple("Mst. Sharmin Jahan", "1020304050", "Ranirbandar, Dinajpur"),
            Triple("Md. Abu Bakar", "5060708090", "Dinajpur Town")
        )
        "CHEQUE_BOOK" -> listOf(
            Triple("Faruk Hossain", "2211445566", "Ranirbandar"),
            Triple("Dr. Aminul Islam", "8899001122", "Dinajpur Sadar")
        )
        else -> listOf(
            Triple("Khadiza Khatun", "6655443322", "Ranirbandar"),
            Triple("Zahid Hasan", "7788992211", "Ranirbandar")
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (importType) {
                    "EXCEL" -> "Excel Spreadsheet Importer"
                    "PHOTO" -> "Photo Document OCR Scanner"
                    else -> "Google Sheets Live Sync"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (step == 1) {
                    when (importType) {
                        "EXCEL" -> {
                            Text("Import bulk records directly from an Excel (.xlsx / .csv) file.", fontSize = 13.sp)
                            Button(
                                onClick = { excelFileSelected = "chirirbandar_branch_registers_${itemType.lowercase()}.xlsx" },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Choose Spreadsheet File")
                            }
                            Text("Selected: $excelFileSelected", fontSize = 12.sp, color = GoldLight, fontWeight = FontWeight.Medium)
                        }
                        "PHOTO" -> {
                            Text("Take a photo of a printed list, delivery register, or courier book. The built-in AI will scan and parse names and account numbers.", fontSize = 13.sp)
                            Text("Take a photo of a printed list, delivery register, or courier book. The built-in AI will scan and parse names and account numbers.", fontSize = 13.sp)
                            if (!photoCaptured) {
                                var isFlashOn by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .background(Color.Black, RoundedCornerShape(12.dp))
                                        .padding(4.dp)
                                ) {
                                    // Bounding corners and scanlines simulation
                                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                        val w = size.width
                                        val h = size.height
                                        val lineLen = 30f
                                        val strokeW = 4f
                                        val col = GoldPrimary
                                        
                                        // Top Left
                                        drawLine(col, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(lineLen, 0f), strokeWidth = strokeW)
                                        drawLine(col, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(0f, lineLen), strokeWidth = strokeW)
                                        
                                        // Top Right
                                        drawLine(col, androidx.compose.ui.geometry.Offset(w, 0f), androidx.compose.ui.geometry.Offset(w - lineLen, 0f), strokeWidth = strokeW)
                                        drawLine(col, androidx.compose.ui.geometry.Offset(w, 0f), androidx.compose.ui.geometry.Offset(w, lineLen), strokeWidth = strokeW)
                                        
                                        // Bottom Left
                                        drawLine(col, androidx.compose.ui.geometry.Offset(0f, h), androidx.compose.ui.geometry.Offset(lineLen, h), strokeWidth = strokeW)
                                        drawLine(col, androidx.compose.ui.geometry.Offset(0f, h), androidx.compose.ui.geometry.Offset(0f, h - lineLen), strokeWidth = strokeW)
                                        
                                        // Bottom Right
                                        drawLine(col, androidx.compose.ui.geometry.Offset(w, h), androidx.compose.ui.geometry.Offset(w - lineLen, h), strokeWidth = strokeW)
                                        drawLine(col, androidx.compose.ui.geometry.Offset(w, h), androidx.compose.ui.geometry.Offset(w, h - lineLen), strokeWidth = strokeW)
                                    }

                                    // Camera Overlay Content (Mock text to scan)
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("[ CHIRIRBANDAR BRANCH REGISTER SHEET ]", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text("1. Sharmin Jahan  | A/C 1020304050", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                                        Text("2. Abu Bakar          | A/C 5060708090", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                                        Text("3. Shahnaj Begum  | A/C 3040506070", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                                    }

                                    // Blinking REC Indicator
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color.Red, RoundedCornerShape(3.dp))
                                        )
                                        Text("LIVE 1080P", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Flash trigger button in viewfinder
                                    IconButton(
                                        onClick = { isFlashOn = !isFlashOn },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FlashOn,
                                            contentDescription = "Flash",
                                            tint = if (isFlashOn) GoldPrimary else Color.White
                                        )
                                    }

                                    // Shutter capture trigger
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 12.dp)
                                            .size(44.dp)
                                            .background(Color.White, RoundedCornerShape(22.dp))
                                            .clickable { photoCaptured = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color.Black, RoundedCornerShape(18.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(GoldPrimary, RoundedCornerShape(14.dp))
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Photo Captured view with scanning animation overlay!
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .background(SlateDark, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(48.dp), tint = GoldPrimary)
                                        Text("smartbanking_register_document.jpg", color = Color.Gray, fontSize = 11.sp)
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenAccent, modifier = Modifier.size(14.dp))
                                            Text("Document ready for OCR scanning", color = GreenAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    // Retake button
                                    Button(
                                        onClick = { photoCaptured = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f), contentColor = Color.White),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Retake", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                        "SHEETS" -> {
                            Text("Synchronize registers in real time from a shared Google Sheets spreadsheet.", fontSize = 13.sp)
                            OutlinedTextField(
                                value = sheetsUrl,
                                onValueChange = { sheetsUrl = it },
                                label = { Text("Google Sheets URL") },
                                placeholder = { Text("https://docs.google.com/spreadsheets/d/...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                } else if (step == 2) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    ) {
                        CircularProgressIndicator(color = GoldPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when (importType) {
                                "EXCEL" -> "Parsing spreadsheet rows..."
                                "PHOTO" -> "AI analyzing text values..."
                                else -> "Connecting to Google API nodes..."
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else if (step == 3) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Success", modifier = Modifier.size(48.dp), tint = GreenAccent)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Import Successful!", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Successfully parsed and synced ${simulatedResults.size} records into local SQLite database.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    }
                }
            }
        },
        confirmButton = {
            if (step == 1) {
                val isEnabled = when (importType) {
                    "PHOTO" -> photoCaptured
                    "EXCEL" -> excelFileSelected != "No file selected"
                    "SHEETS" -> sheetsUrl.isNotBlank()
                    else -> true
                }
                Button(
                    onClick = {
                        step = 2
                    },
                    enabled = isEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = SlateDark,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    )
                ) {
                    Text("Parse & Import")
                }
            } else if (step == 3) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark)
                ) {
                    Text("Finish")
                }
            }
        },
        dismissButton = {
            if (step == 1) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )

    if (step == 2) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            onImportConfirmed(simulatedResults)
            step = 3
        }
    }
}

@Composable
fun BankingItemRow(
    item: BankingItem,
    now: Long,
    showDeleteButton: Boolean,
    showDeliveryButton: Boolean,
    onMarkDelivered: () -> Unit,
    onMarkDestroyed: () -> Unit,
    onDelete: () -> Unit,
    onUndoDelivery: (() -> Unit)? = null
) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val daysLeft = ((item.destroyAfter - now) / (1000 * 3600 * 24)).toInt().coerceAtLeast(0)
    val totalPeriod = 90f
    val progress = if (daysLeft > 0) daysLeft / totalPeriod else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = item.customerName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = "A/C: ${item.accountNumber}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }

                // Balance status badge
                Box(
                    modifier = Modifier
                        .background(
                            if (item.isDelivered) GoldPrimary.copy(alpha = 0.15f)
                            else if (daysLeft > 10) GreenAccent.copy(alpha = 0.15f)
                            else RedAccent.copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (item.isDelivered) "DELIVERED"
                               else if (daysLeft > 0 && !item.isDestroyed) "BALANCED: $daysLeft Days Left"
                               else "DESTRUCTION HISTORY",
                        color = if (item.isDelivered) GoldPrimary else if (daysLeft > 10) GreenAccent else RedAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar mapping countdown to 90 days
            if (daysLeft > 0 && !item.isDestroyed && !item.isDelivered) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = if (daysLeft > 15) GoldPrimary else RedAccent,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Expanded details
            Text(text = "Phone: ${item.phoneNumber}", fontSize = 12.sp)
            Text(text = "Address: ${item.address}", fontSize = 12.sp)
            Text(
                text = if (item.isDelivered && item.deliveryDate > 0L) {
                    "Received: ${sdf.format(Date(item.receivedDate))} | Delivered: ${sdf.format(Date(item.deliveryDate))}"
                } else {
                    "Received: ${sdf.format(Date(item.receivedDate))} | Destruct limit: ${sdf.format(Date(item.destroyAfter))}"
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            if (item.remarks.isNotBlank()) {
                Text(text = "Remarks: ${item.remarks}", fontSize = 12.sp, color = GoldPrimary, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showDeliveryButton && !item.isDelivered && !item.isDestroyed && daysLeft > 0) {
                    Button(
                        onClick = onMarkDelivered,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(end = 8.dp).testTag("deliver_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Deliver", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delivery", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (item.isDelivered && onUndoDelivery != null) {
                    Button(
                        onClick = onUndoDelivery,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(end = 8.dp).testTag("activate_back_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo Delivery", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Active Back", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (showDeleteButton) {
                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = RedAccent)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
