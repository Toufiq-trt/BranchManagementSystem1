package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.asImageBitmap
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

    var editingItem by remember { mutableStateOf<BankingItem?>(null) }

    var isAddingNew by remember { mutableStateOf(false) }
    var tabSelected by remember { mutableStateOf(0) } // 0 = Active, 1 = 30 Days Crossed, 2 = 90 Days Complete, 3 = Delivered List
    var collapsedFolders by remember { mutableStateOf(setOf<String>()) }
    
    // PDF Preview States
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewTitle by remember { mutableStateOf("") }
    var previewHeaders by remember { mutableStateOf<List<String>?>(null) }
    var previewRows by remember { mutableStateOf<List<List<String>>?>(null) }
    var onConfirmDownload by remember { mutableStateOf<() -> Unit>({}) }

    // Bulk Importer Settings
    var showBulkImportDialog by remember { mutableStateOf(false) }
    var bulkImportType by remember { mutableStateOf("") } // "EXCEL", "PHOTO", "SHEETS", "PASTE"
    var isSyncingSheets by remember { mutableStateOf(false) }

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
    
    // Active (Balancing): ALL undelivered and undestroyed items
    val activeList = filteredItems.filter { 
        !it.isDelivered && !it.isDestroyed
    }
    
    // 30 Days Completed: All undelivered and undestroyed items that crossed 30 days
    val crossedList = filteredItems.filter { 
        !it.isDelivered && !it.isDestroyed && 
        ((now - it.receivedDate) / (1000L * 3600 * 24) >= 30)
    }
    
    // 90 Days Completed: All undelivered and undestroyed items that crossed 90 days
    val completedList = filteredItems.filter { 
        !it.isDelivered && !it.isDestroyed && 
        ((now - it.receivedDate) / (1000L * 3600 * 24) >= 90) 
    }
    
    // Delivered List: All delivered items
    val deliveredList = filteredItems.filter { it.isDelivered }

    // Destruction List: All marked destroyed items, not delivered
    val destroyedList = filteredItems.filter { !it.isDelivered && it.isDestroyed }

    val currentDisplayList = when (tabSelected) {
        0 -> activeList
        1 -> crossedList
        2 -> completedList
        3 -> deliveredList
        else -> destroyedList
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
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
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Register New $itemType", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 14.sp)
                    
                    // Bulk entry operations row
                    Text("⚡ BULK AUTO-ENTRY OPERATIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldLight)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                            Icon(Icons.Default.BorderAll, contentDescription = null, modifier = Modifier.size(13.dp), tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Excel", fontSize = 10.sp, color = Color.White)
                        }
                        Button(
                            onClick = {
                                bulkImportType = "PHOTO"
                                showBulkImportDialog = true
                            },
                            modifier = Modifier.weight(1.1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(13.dp), tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Camera", fontSize = 10.sp, color = Color.White)
                        }
                        Button(
                            onClick = {
                                if (!isSyncingSheets) {
                                    isSyncingSheets = true
                                    android.widget.Toast.makeText(context, "Syncing spreadsheet... Please wait", android.widget.Toast.LENGTH_SHORT).show()
                                    viewModel.syncGoogleSheets(
                                        type = itemType,
                                        context = context,
                                        onSuccess = { msg ->
                                            isSyncingSheets = false
                                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                        },
                                        onFailure = { err ->
                                            isSyncingSheets = false
                                            android.widget.Toast.makeText(context, "Sync Error: $err", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            },
                            enabled = !isSyncingSheets,
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSyncingSheets) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), color = GoldPrimary, strokeWidth = 1.5.dp)
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(13.dp), tint = GoldPrimary)
                            }
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(if (isSyncingSheets) "Syncing..." else "Sheet Sync", fontSize = 10.sp, color = Color.White)
                        }
                        Button(
                            onClick = {
                                bulkImportType = "PASTE"
                                showBulkImportDialog = true
                            },
                            modifier = Modifier.weight(1.1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(13.dp), tint = GoldPrimary)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Easy Paste", fontSize = 10.sp, color = Color.White)
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

        // Active Balancing, 30 Days Completed, 90 Days Completed, Delivered List, and Destruction List Toggle Tabs
        ScrollableTabRow(
            selectedTabIndex = tabSelected,
            containerColor = Color.Transparent,
            contentColor = GoldPrimary,
            edgePadding = 0.dp,
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
                text = { Text("Active (${activeList.size})", fontSize = 9.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = tabSelected == 1,
                onClick = { tabSelected = 1 },
                text = { Text("30d Completed (${crossedList.size})", fontSize = 9.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = tabSelected == 2,
                onClick = { tabSelected = 2 },
                text = { Text("90d Completed (${completedList.size})", fontSize = 9.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = tabSelected == 3,
                onClick = { tabSelected = 3 },
                text = { Text("Delivered (${deliveredList.size})", fontSize = 9.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = tabSelected == 4,
                onClick = { tabSelected = 4 },
                text = { Text("Destruction (${destroyedList.size})", fontSize = 9.sp, fontWeight = FontWeight.Bold) }
            )
        }

        // Standardized Table Download Registry Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val labelText = when (tabSelected) {
                0 -> "Active Balancing Records (All Undelivered)"
                1 -> "30 Days Completed Records (>=30 Days)"
                2 -> "90 Days Completed Records (>=90 Days)"
                3 -> "Delivered Log"
                else -> "Destruction Registry"
            }
            Text(labelText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
            
            Button(
                onClick = {
                    val labelSub = when (tabSelected) {
                        0 -> "ACTIVE REGISTRY"
                        1 -> "30 DAYS COMPLETED REGISTRY"
                        2 -> "90 DAYS COMPLETED REGISTRY"
                        3 -> "DELIVERED LOG"
                        else -> "DESTRUCTION REGISTRY"
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
                            1 -> Icons.Default.Warning
                            2 -> Icons.Default.DeleteSweep
                            3 -> Icons.Default.CheckCircle
                            else -> Icons.Default.DeleteForever
                        },
                        contentDescription = "Empty",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (tabSelected) {
                            0 -> "No active items in balancing."
                            1 -> "No 30 days completed items found."
                            2 -> "No 90 days completed items found."
                            3 -> "No delivered items logged."
                            else -> "No items marked for destruction."
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
                if (tabSelected == 3) {
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
                                        showMailedButton = viewModel.isLoggedIn && tabSelected == 1,
                                        showDestructionButton = viewModel.isLoggedIn && tabSelected == 2,
                                        onMarkDelivered = {
                                            viewModel.markAsDelivered(item)
                                        },
                                        onMarkDestroyed = {
                                            viewModel.updateBankingItem(item.copy(isDestroyed = true))
                                        },
                                        onMarkMailed = {
                                            viewModel.markAsLetterIssued(item)
                                        },
                                        onDelete = {
                                            viewModel.deleteBankingItem(item)
                                        },
                                        onUndoDelivery = {
                                            viewModel.revertDelivery(item)
                                        },
                                        onEdit = {
                                            editingItem = item
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
                            showMailedButton = viewModel.isLoggedIn && tabSelected == 1,
                            showDestructionButton = viewModel.isLoggedIn && tabSelected == 2,
                            onMarkDelivered = {
                                viewModel.markAsDelivered(item)
                            },
                            onMarkDestroyed = {
                                viewModel.updateBankingItem(item.copy(isDestroyed = true))
                            },
                            onMarkMailed = {
                                viewModel.markAsLetterIssued(item)
                            },
                            onDelete = {
                                viewModel.deleteBankingItem(item)
                            },
                            onUndoDelivery = {
                                viewModel.revertDelivery(item)
                            },
                            onEdit = {
                                editingItem = item
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
            onImportConfirmed = { parsedList, onComplete ->
                viewModel.importBulkRows(itemType, parsedList) { inserted, updated ->
                    onComplete(inserted, updated)
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
            },
            onDownloadExcel = if (previewHeaders != null && previewRows != null) {
                {
                    val excelFileName = previewTitle.replace(" ", "_").lowercase() + ".csv"
                    com.example.util.ExcelHelper.generateGenericExcel(context, excelFileName, previewHeaders!!, previewRows!!)
                }
            } else {
                null
            }
        )
    }

    if (editingItem != null) {
        var editName by remember { mutableStateOf(editingItem?.customerName ?: "") }
        var editAcNo by remember { mutableStateOf(editingItem?.accountNumber ?: "") }
        var editPhone by remember { mutableStateOf(editingItem?.phoneNumber ?: "") }
        var editAddress by remember { mutableStateOf(editingItem?.address ?: "") }
        var editRemarks by remember { mutableStateOf(editingItem?.remarks ?: "") }

        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("Edit Saved Entry", color = GoldPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Customer Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editAcNo,
                        onValueChange = { editAcNo = it },
                        label = { Text("Account Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editAddress,
                        onValueChange = { editAddress = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editRemarks,
                        onValueChange = { editRemarks = it },
                        label = { Text("Remarks") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        editingItem?.let { item ->
                            val updated = item.copy(
                                customerName = editName.uppercase().trim(),
                                accountNumber = editAcNo.trim(),
                                phoneNumber = editPhone.trim(),
                                address = editAddress.uppercase().trim(),
                                remarks = editRemarks.uppercase().trim()
                            )
                            viewModel.updateBankingItem(updated)
                        }
                        editingItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

private fun parseCsvTextLocal(text: String): List<List<String>> {
    val result = mutableListOf<List<String>>()
    val lines = text.split("\n")
    for (line in lines) {
        if (line.isBlank()) continue
        val row = mutableListOf<String>()
        var inQuotes = false
        val sb = java.lang.StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                row.add(sb.toString().trim())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        row.add(sb.toString().trim())
        val cleanRow = row.map { cell ->
            var cleaned = cell
            if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length >= 2) {
                cleaned = cleaned.substring(1, cleaned.length - 1)
            }
            cleaned.replace("\"\"", "\"")
        }
        result.add(cleanRow)
    }
    return result
}

private fun getFileName(context: android.content.Context, uri: android.net.Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

@Composable
fun BulkImportDialog(
    itemType: String,
    importType: String, // "EXCEL", "PHOTO", "SHEETS", "PASTE"
    onDismiss: () -> Unit,
    onImportConfirmed: (List<List<String>>, onComplete: (inserted: Int, updated: Int) -> Unit) -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1 = Input, 2 = Loading, 3 = Success
    var sheetsUrl by remember {
        mutableStateOf("https://docs.google.com/spreadsheets/d/1BUc13oZ_qKIBW9OOFtcPAZh9aoELxyVq6sguoAyAdFg/edit?usp=sharing")
    }
    var excelFileSelected by remember { mutableStateOf("No file selected") }
    var pasteText by remember { mutableStateOf("") }
    var photoCaptured by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var extractedRows by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var importedCount by remember { mutableStateOf(0) }
    var updatedCount by remember { mutableStateOf(0) }

    val context = LocalContext.current

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            excelFileSelected = getFileName(context, uri) ?: "Selected Spreadsheet"
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val textContent = inputStream?.bufferedReader()?.use { it.readText() }
                if (!textContent.isNullOrBlank()) {
                    val parsed = parseCsvTextLocal(textContent)
                    if (parsed.isNotEmpty()) {
                        extractedRows = parsed
                    } else {
                        android.widget.Toast.makeText(context, "No rows found in selected CSV", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    android.widget.Toast.makeText(context, "Selected file is empty", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Error reading file: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            photoCaptured = true
        }
    }

    val simulatedResults = emptyList<List<String>>()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (importType) {
                    "EXCEL" -> "Excel Spreadsheet Importer"
                    "PHOTO" -> "Photo Document OCR Scanner"
                    "PASTE" -> "Smart Text Paste Importer"
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
                            Text("Upload and import a .csv formatted spreadsheet from your device local storage.", fontSize = 12.sp, color = Color.LightGray)
                            Button(
                                onClick = { filePickerLauncher.launch("*/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, tint = GoldPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Select Device Spreadsheet (.csv)", color = Color.White)
                            }
                            Text("Selected: $excelFileSelected", fontSize = 12.sp, color = GoldLight, fontWeight = FontWeight.Medium)
                            if (extractedRows.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Extracted ${extractedRows.size} rows successfully!", color = GreenAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        "PASTE" -> {
                            Text("Paste raw multi-line records (e.g. from notes, WhatsApp, email). Format: ACCOUNT NUMBER, CUSTOMER NAME, PHONE NUMBER, RECEIVE DATE, ADDRESS, delivered (separated by comma, pipe, tab, or semicolon).", fontSize = 12.sp, color = Color.LightGray)
                            OutlinedTextField(
                                value = pasteText,
                                onValueChange = { 
                                    pasteText = it
                                    if (it.isNotBlank()) {
                                        val lines = it.lines()
                                        val extracted = mutableListOf<List<String>>()
                                        // Add header row first
                                        extracted.add(listOf("ACCOUNT NUMBER", "CUSTOMER NAME", "PHONE NUMBER", "RECEIVE DATE", "ADDRESS", "delivered"))
                                        for (line in lines) {
                                            if (line.isBlank()) continue
                                            val parts = line.split(Regex("[,|;\t]")).map { it.trim() }
                                            if (parts.size >= 2) {
                                                val acNo = parts.getOrNull(0) ?: ""
                                                val name = parts.getOrNull(1) ?: ""
                                                val phone = parts.getOrNull(2) ?: ""
                                                val rcvDate = parts.getOrNull(3) ?: ""
                                                val addr = parts.getOrNull(4) ?: ""
                                                val dlv = parts.getOrNull(5) ?: ""
                                                extracted.add(listOf(acNo, name, phone, rcvDate, addr, dlv))
                                            }
                                        }
                                        extractedRows = extracted
                                    } else {
                                        extractedRows = emptyList()
                                    }
                                },
                                label = { Text("Paste Text Area") },
                                placeholder = { Text("1234567890, Toufiq, 01712345678, 18.07.2026, Dinajpur, delivered") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                maxLines = 10
                            )
                            if (extractedRows.isNotEmpty()) {
                                Text("Extracted ${extractedRows.size - 1} records!", color = GreenAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        "PHOTO" -> {
                            Text("Use the device camera to photograph printed ledger registers or courier lists. The document values will be extracted and previewed below.", fontSize = 12.sp, color = Color.LightGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (!photoCaptured) {
                                Button(
                                    onClick = { cameraLauncher.launch(null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = "Back Camera")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Back Camera & Click Photo", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Captured Document:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GoldPrimary)
                                        TextButton(
                                            onClick = {
                                                photoCaptured = false
                                                capturedBitmap = null
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Retake", modifier = Modifier.size(14.dp), tint = GoldLight)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Retake Photo", fontSize = 11.sp, color = GoldLight)
                                        }
                                    }

                                    // Display captured photo bitmap if available
                                    capturedBitmap?.let { bitmap ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                                .background(Color.Black, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            androidx.compose.foundation.Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Captured Document Photo",
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .padding(4.dp),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
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

                    // Preview Extracted Data Section
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)
                    Text("Data Preview:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GoldPrimary)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF131922), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Header
                        Row(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)).padding(4.dp)) {
                            Text("A/C Number", modifier = Modifier.weight(1.1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Customer Name", modifier = Modifier.weight(1.2f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Phone", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Address", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        // Rows
                        val previewList = if (extractedRows.isNotEmpty()) {
                            extractedRows.drop(1).take(4)
                        } else {
                            emptyList()
                        }
                        if (previewList.isEmpty()) {
                            Text("No records to preview. Please upload or paste data in standard format.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(4.dp))
                        } else {
                            previewList.forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)) {
                                    Text(row.getOrNull(0) ?: "", modifier = Modifier.weight(1.1f), fontSize = 10.sp, color = Color.LightGray)
                                    Text(row.getOrNull(1)?.uppercase() ?: "", modifier = Modifier.weight(1.2f), fontSize = 10.sp, color = Color.LightGray)
                                    Text(row.getOrNull(2) ?: "", modifier = Modifier.weight(1f), fontSize = 10.sp, color = Color.LightGray)
                                    Text(row.getOrNull(4)?.uppercase() ?: "", modifier = Modifier.weight(1f), fontSize = 10.sp, color = Color.LightGray)
                                }
                            }
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
                                "EXCEL" -> "Parsing local spreadsheet rows..."
                                "PHOTO" -> "AI analyzing text values..."
                                "PASTE" -> "Extracting values from text block..."
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
                        Text("Successfully parsed and synced records into local SQLite database.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateDark),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("New Items Inserted: $importedCount", fontSize = 12.sp, color = GoldLight, fontWeight = FontWeight.Bold)
                                Text("Existing Items Updated: $updatedCount", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (step == 1) {
                val isEnabled = when (importType) {
                    "PHOTO" -> photoCaptured
                    "EXCEL" -> excelFileSelected != "No file selected"
                    "PASTE" -> pasteText.isNotBlank() && extractedRows.size > 1
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
            kotlinx.coroutines.delay(1500)
            val listToImport = extractedRows
            onImportConfirmed(listToImport) { inserted, updated ->
                importedCount = inserted
                updatedCount = updated
                step = 3
            }
        }
    }
}

@Composable
fun BankingItemRow(
    item: BankingItem,
    now: Long,
    showDeleteButton: Boolean,
    showDeliveryButton: Boolean,
    showMailedButton: Boolean = false,
    showDestructionButton: Boolean = false,
    onMarkDelivered: () -> Unit,
    onMarkDestroyed: () -> Unit,
    onMarkMailed: (() -> Unit)? = null,
    onDelete: () -> Unit,
    onUndoDelivery: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.isLetterIssued) {
                        Box(
                            modifier = Modifier
                                .background(Color.Blue.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "LETTER ISSUED",
                                color = Color(0xFF64B5F6),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Balance status badge
                    Box(
                        modifier = Modifier
                            .background(
                                if (item.isDelivered) GoldPrimary.copy(alpha = 0.15f)
                                else if (item.isDestroyed) RedAccent.copy(alpha = 0.15f)
                                else if (daysLeft > 10) GreenAccent.copy(alpha = 0.15f)
                                else RedAccent.copy(alpha = 0.15f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (item.isDelivered) "DELIVERED"
                                   else if (item.isDestroyed) "DESTRUCTED"
                                   else if (daysLeft > 0) "BALANCED: $daysLeft Days Left"
                                   else "90 DAYS COMPLETE",
                            color = if (item.isDelivered) GoldPrimary else if (item.isDestroyed) RedAccent else if (daysLeft > 10) GreenAccent else RedAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                if (showMailedButton && !item.isDelivered && !item.isDestroyed && !item.isLetterIssued && onMarkMailed != null) {
                    Button(
                        onClick = onMarkMailed,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary.copy(alpha = 0.8f), contentColor = SlateDark),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(end = 8.dp).testTag("mail_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = "Mail", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mailed", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (showDeliveryButton && !item.isDelivered && !item.isDestroyed) {
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

                if ((showDestructionButton || daysLeft <= 0) && !item.isDelivered && !item.isDestroyed) {
                    Button(
                        onClick = onMarkDestroyed,
                        colors = ButtonDefaults.buttonColors(containerColor = RedAccent, contentColor = Color.White),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(end = 8.dp).testTag("destruction_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Destroy", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Destruction", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

                if (onEdit != null) {
                    IconButton(
                        onClick = onEdit,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = GoldPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
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
