package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CustomerHunting
import com.example.data.TodoTask
import com.example.ui.theme.*

@Composable
fun TaskAndHuntingScreen(
    viewModel: BankingViewModel,
    tabType: String, // "TODO" or "HUNTING"
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val huntingList by viewModel.allHunting.collectAsStateWithLifecycle()

    var isAddingTask by remember { mutableStateOf(false) }
    var todoTabSelected by remember { mutableStateOf(0) } // 0 = Active, 1 = Completed

    // Task addition states
    var taskTitle by remember { mutableStateOf("") }
    var taskPriority by remember { mutableStateOf("HIGH") }
    var taskDueTime by remember { mutableStateOf("16:00") }

    // Customer hunting addition states
    var leadName by remember { mutableStateOf("") }
    var leadPhone by remember { mutableStateOf("") }
    var leadAddress by remember { mutableStateOf("") }
    var leadProduct by remember { mutableStateOf("") }
    var leadPriority by remember { mutableStateOf("HIGH") }
    var leadCompletion by remember { mutableStateOf(0) }

    // PDF Preview States
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewTitle by remember { mutableStateOf("") }
    var previewTextContent by remember { mutableStateOf<String?>(null) }
    var onConfirmDownload by remember { mutableStateOf<() -> Unit>({}) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (tabType == "TODO") "DAILY TASKS OPERATIVE" else "CUSTOMER LEADS HUNTING",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (tabType == "TODO") "Google Tasks Integration (Offline Active)" else "Campaign Leads & Conversions",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            val canAdd = if (tabType == "TODO") true else viewModel.isLoggedIn
            if (canAdd) {
                IconButton(
                    onClick = { isAddingTask = !isAddingTask },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = GoldPrimary, contentColor = SlateDark)
                ) {
                    Icon(imageVector = if (isAddingTask) Icons.Default.Close else Icons.Default.Add, contentDescription = "Add")
                }
            }
        }

        // Expanded addition Form
        AnimatedVisibility(
            visible = isAddingTask,
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
                    if (tabType == "TODO") {
                        Text("Add New Task", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 14.sp)
                        OutlinedTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            label = { Text("Task Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = taskDueTime,
                                onValueChange = { taskDueTime = it },
                                label = { Text("Due Time (e.g. 16:00)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            // Priority Selector
                            var expandedPri by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = { expandedPri = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = SlateDark)
                                ) {
                                    Text("Pri: $taskPriority", color = GoldPrimary)
                                }
                                DropdownMenu(expanded = expandedPri, onDismissRequest = { expandedPri = false }) {
                                    listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
                                        DropdownMenuItem(
                                            text = { Text(p) },
                                            onClick = {
                                                taskPriority = p
                                                expandedPri = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = {
                                if (taskTitle.isNotBlank()) {
                                    viewModel.addTask(taskTitle, taskPriority, System.currentTimeMillis(), taskDueTime)
                                    taskTitle = ""
                                    isAddingTask = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark)
                        ) {
                            Text("Confirm Save Task")
                        }
                    } else {
                        Text("Register Campaign Lead", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 14.sp)
                        OutlinedTextField(
                            value = leadName,
                            onValueChange = { leadName = it },
                            label = { Text("Client Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = leadPhone,
                            onValueChange = { leadPhone = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = leadProduct,
                            onValueChange = { leadProduct = it },
                            label = { Text("Interested Banking Product") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = leadAddress,
                            onValueChange = { leadAddress = it },
                            label = { Text("Work/Home Address") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Priority & Initial slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Initial Progress: $leadCompletion%", fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Slider(
                                value = leadCompletion.toFloat(),
                                onValueChange = { leadCompletion = it.toInt() },
                                valueRange = 0f..100f,
                                modifier = Modifier.weight(2f),
                                colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary)
                            )
                        }

                        Button(
                            onClick = {
                                if (leadName.isNotBlank() && leadPhone.isNotBlank()) {
                                    viewModel.addHunting(
                                        name = leadName,
                                        phone = leadPhone,
                                        address = leadAddress,
                                        product = leadProduct,
                                        priority = leadPriority,
                                        completion = leadCompletion
                                    )
                                    leadName = ""
                                    leadPhone = ""
                                    leadAddress = ""
                                    leadProduct = ""
                                    leadCompletion = 0
                                    isAddingTask = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark)
                        ) {
                            Text("Save Campaign Lead")
                        }
                    }
                }
            }
        }

        // List View Content
        if (tabType == "TODO") {
            val context = LocalContext.current
            
            // Sub-tabs for Daily Tasks: To Do Tasks vs Completed Task
            TabRow(
                selectedTabIndex = todoTabSelected,
                containerColor = Color.Transparent,
                contentColor = GoldPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[todoTabSelected]),
                        color = GoldPrimary
                    )
                }
            ) {
                Tab(
                    selected = todoTabSelected == 0,
                    onClick = { todoTabSelected = 0 },
                    text = { Text("To Do Tasks (${tasks.filter { !it.isCompleted }.size})", fontSize = 12.sp) }
                )
                Tab(
                    selected = todoTabSelected == 1,
                    onClick = { todoTabSelected = 1 },
                    text = { Text("Completed Task (${tasks.filter { it.isCompleted }.size})", fontSize = 12.sp) }
                )
            }

            val displayedTasks = if (todoTabSelected == 0) {
                tasks.filter { !it.isCompleted }
            } else {
                tasks.filter { it.isCompleted }
            }

            if (displayedTasks.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (todoTabSelected == 0) "No active pending tasks!" else "No completed tasks yet.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    items(displayedTasks, key = { it.id }) { task ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val scale by animateFloatAsState(if (task.isCompleted) 1.1f else 1.0f, label = "CompletedScale")
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = { isChecked ->
                                            viewModel.updateTask(task.copy(isCompleted = isChecked))
                                        },
                                        modifier = Modifier.scale(scale),
                                        colors = CheckboxDefaults.colors(checkedColor = GreenAccent, uncheckedColor = GoldPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = task.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        when (task.priority) {
                                                            "HIGH" -> RedAccent.copy(alpha = 0.15f)
                                                            "MEDIUM" -> OrangeAccent.copy(alpha = 0.15f)
                                                            else -> SlateSecondary.copy(alpha = 0.15f)
                                                        },
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = task.priority,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (task.priority) {
                                                        "HIGH" -> RedAccent
                                                        "MEDIUM" -> OrangeAccent
                                                        else -> SlateSecondary
                                                    }
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(task.dueTime, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        }
                                    }
                                }

                                if (viewModel.isLoggedIn && viewModel.currentUser?.isToufiq == true) {
                                    IconButton(
                                        onClick = { viewModel.deleteTask(task) },
                                        colors = IconButtonDefaults.iconButtonColors(contentColor = RedAccent)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Report Download & Share Action Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val totalCount = tasks.size
                    val completedCount = tasks.filter { it.isCompleted }.size
                    val completionPercent = if (totalCount > 0) (completedCount * 100 / totalCount) else 0
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("DAILY REPORT PROGRESS", fontSize = 11.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)
                            Text("Tasks Completed: $completedCount of $totalCount ($completionPercent%)", fontSize = 12.sp, color = Color.White)
                        }
                        
                        Button(
                            onClick = {
                                val reportText = """
                                    BRANCH MANAGEMENT SYSTEM - DAILY TASK REPORT
                                    Smart Banking Tracker
                                    Date: ${java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())}
                                    
                                    SUMMARY:
                                    - Total Tasks Registered: $totalCount
                                    - Tasks Completed: $completedCount
                                    - Tasks Pending: ${totalCount - completedCount}
                                    - Completion Rate: $completionPercent%
                                    
                                    PENDING TASKS BREAKDOWN:
                                    ${tasks.filter { !it.isCompleted }.mapIndexed { i, t -> "${i + 1}. [${t.priority}] ${t.title} (Due: ${t.dueTime})" }.joinToString("\n").ifEmpty { "None (All Tasks Completed!)" }}
                                    
                                    COMPLETED TASKS BREAKDOWN:
                                    ${tasks.filter { t -> t.isCompleted }.mapIndexed { i, t -> "${i + 1}. [${t.priority}] ${t.title} - COMPLETED" }.joinToString("\n").ifEmpty { "No tasks completed yet." }}
                                    
                                    Report generated via Branch Operations App.
                                """.trimIndent()

                                val reportFileName = "Daily Tasks Report-${java.text.SimpleDateFormat("d-M-yyyy", java.util.Locale.getDefault()).format(java.util.Date())}.pdf"
                                
                                previewTitle = "DAILY TASK PROGRESS REPORT"
                                previewTextContent = reportText
                                onConfirmDownload = {
                                    com.example.util.PdfHelper.generateAndSharePdf(context, reportFileName, "DAILY TASK PROGRESS REPORT", reportText)
                                }
                                showPreviewDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download Report", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Hunting Leads View
            if (huntingList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Zero active hunting leads registered.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    items(huntingList, key = { it.id }) { lead ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(lead.customerName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("Interest: ${lead.interestedProduct}", fontSize = 12.sp, color = GoldPrimary, fontWeight = FontWeight.Medium)
                                    }

                                    Checkbox(
                                        checked = lead.isGrabbed,
                                        onCheckedChange = { isChecked ->
                                            val newPerc = if (isChecked) 100 else 50
                                            viewModel.updateHunting(lead.copy(isGrabbed = isChecked, completionPercentage = newPerc))
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = GreenAccent, uncheckedColor = GoldPrimary)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Phone: ${lead.phoneNumber} | Location: ${lead.address}", fontSize = 11.sp)
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Conversion Progress: ${lead.completionPercentage}%", fontSize = 11.sp, modifier = Modifier.weight(1.5f))
                                    LinearProgressIndicator(
                                        progress = { lead.completionPercentage / 100f },
                                        modifier = Modifier
                                            .weight(2f)
                                            .height(6.dp),
                                        color = if (lead.isGrabbed) GreenAccent else GoldPrimary,
                                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (lead.completionPercentage < 100) {
                                        TextButton(
                                            onClick = {
                                                val nextP = (lead.completionPercentage + 25).coerceAtMost(100)
                                                viewModel.updateHunting(lead.copy(completionPercentage = nextP, isGrabbed = nextP >= 100))
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = GoldPrimary)
                                        ) {
                                            Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Advance Progress")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    if (viewModel.isLoggedIn && viewModel.currentUser?.isToufiq == true) {
                                        IconButton(
                                            onClick = { viewModel.deleteHunting(lead) },
                                            colors = IconButtonDefaults.iconButtonColors(contentColor = RedAccent)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
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
