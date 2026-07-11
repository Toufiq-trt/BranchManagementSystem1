package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TodoTask
import com.example.ui.theme.*
import com.example.util.PdfHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoLogScreen(
    viewModel: BankingViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    var isAddingTask by remember { mutableStateOf(false) }

    // Task details
    var taskTitle by remember { mutableStateOf("") }
    var taskPriority by remember { mutableStateOf("HIGH") }
    var taskDueTime by remember { mutableStateOf("16:00") }

    // PDF Preview States
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewTitle by remember { mutableStateOf("") }
    var previewTextContent by remember { mutableStateOf<String?>(null) }
    var onConfirmDownload by remember { mutableStateOf<() -> Unit>({}) }

    // Date Wise registration selection (Day, Month, Year dropdowns)
    val calendar = Calendar.getInstance()
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH).toString()) }
    var selectedMonth by remember { mutableStateOf((calendar.get(Calendar.MONTH) + 1).toString()) }
    var selectedYear by remember { mutableStateOf("2026") }

    // Group tasks by their registration date (creation timestamp)
    val tasksByDate = remember(tasks) {
        val groups = tasks.groupBy { sdf.format(Date(it.timestamp)) }
        // Sort keys by date descending
        groups.toSortedMap { d1, d2 ->
            try {
                val date1 = sdf.parse(d1) ?: Date(0)
                val date2 = sdf.parse(d2) ?: Date(0)
                date2.compareTo(date1) // Descending (most recent first)
            } catch (e: Exception) {
                d2.compareTo(d1)
            }
        }
    }

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
                    text = "HISTORICAL TODO LOGS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Register and manage tasks date-wise",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            IconButton(
                onClick = { isAddingTask = !isAddingTask },
                colors = IconButtonDefaults.iconButtonColors(containerColor = GoldPrimary, contentColor = SlateDark)
            ) {
                Icon(imageVector = if (isAddingTask) Icons.Default.Close else Icons.Default.Add, contentDescription = "Add")
            }
        }

        // Add task with custom date wise registration option
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
                    Text("Register Task Date Wise", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 14.sp)
                    
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Task Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = taskDueTime,
                            onValueChange = { taskDueTime = it },
                            label = { Text("Due Time") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        // Priority selection
                        var expandedPri by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Button(
                                onClick = { expandedPri = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SlateDark)
                            ) {
                                Text("Pri: $taskPriority", color = GoldPrimary, fontSize = 12.sp)
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

                    // Date Wise Dropdowns for Day, Month, Year
                    Text("Registration Date", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = GoldPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Day Selection
                        var expDay by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { expDay = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Day: $selectedDay", fontSize = 11.sp, color = Color.White)
                            }
                            DropdownMenu(expanded = expDay, onDismissRequest = { expDay = false }) {
                                (1..31).forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text(d.toString()) },
                                        onClick = {
                                            selectedDay = d.toString()
                                            expDay = false
                                        }
                                    )
                                }
                            }
                        }

                        // Month Selection
                        var expMonth by remember { mutableStateOf(false) }
                        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                        Box(modifier = Modifier.weight(1.2f)) {
                            Button(
                                onClick = { expMonth = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val mIdx = selectedMonth.toIntOrNull()?.minus(1)?.coerceIn(0, 11) ?: 0
                                Text(monthNames[mIdx], fontSize = 11.sp, color = Color.White)
                            }
                            DropdownMenu(expanded = expMonth, onDismissRequest = { expMonth = false }) {
                                monthNames.forEachIndexed { idx, m ->
                                    DropdownMenuItem(
                                        text = { Text(m) },
                                        onClick = {
                                            selectedMonth = (idx + 1).toString()
                                            expMonth = false
                                        }
                                    )
                                }
                            }
                        }

                        // Year Selection
                        var expYear by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { expYear = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(selectedYear, fontSize = 11.sp, color = Color.White)
                            }
                            DropdownMenu(expanded = expYear, onDismissRequest = { expYear = false }) {
                                listOf("2026", "2025", "2027").forEach { y ->
                                    DropdownMenuItem(
                                        text = { Text(y) },
                                        onClick = {
                                            selectedYear = y
                                            expYear = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (taskTitle.isNotBlank()) {
                                // Construct timestamp based on inputs
                                val customCal = Calendar.getInstance()
                                customCal.set(Calendar.YEAR, selectedYear.toInt())
                                customCal.set(Calendar.MONTH, selectedMonth.toInt() - 1)
                                customCal.set(Calendar.DAY_OF_MONTH, selectedDay.toInt())
                                customCal.set(Calendar.HOUR_OF_DAY, 9)
                                customCal.set(Calendar.MINUTE, 0)
                                val customTimestamp = customCal.timeInMillis

                                viewModel.addTaskWithDate(
                                    title = taskTitle,
                                    priority = taskPriority,
                                    dueDate = customTimestamp,
                                    dueTime = taskDueTime,
                                    customTimestamp = customTimestamp
                                )

                                taskTitle = ""
                                isAddingTask = false
                                Toast.makeText(context, "Task registered date wise successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please enter a task title.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Register Task")
                    }
                }
            }
        }

        if (tasksByDate.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No date wise tasks logged in system.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                tasksByDate.forEach { (dateStr, groupTasks) ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = dateStr,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                // Download option for this specific date group
                                Button(
                                    onClick = {
                                        val reportText = """
                                            BRANCH OPERATIONS - DATE WISE TASK LOG
                                            Date: $dateStr
                                            
                                            TASKS REGISTERED FOR THIS DATE:
                                            =======================================
                                            ${groupTasks.mapIndexed { idx, task ->
                                                "${idx + 1}. [${task.priority}] ${task.title} | Due: ${task.dueTime} | Status: ${if (task.isCompleted) "COMPLETED" else "PENDING"}"
                                            }.joinToString("\n")}
                                            
                                            Branch Operations Audit Register.
                                        """.trimIndent()
                                        val formattedDate = try {
                                            val parsedDate = sdf.parse(dateStr)
                                            if (parsedDate != null) {
                                                SimpleDateFormat("d-M-yyyy", Locale.getDefault()).format(parsedDate)
                                            } else {
                                                dateStr.replace(" ", "-")
                                            }
                                        } catch (e: Exception) {
                                            dateStr.replace(" ", "-")
                                        }
                                        val reportFileName = "Task Log-$formattedDate.pdf"
                                        
                                        previewTitle = "TASK LOG $dateStr"
                                        previewTextContent = reportText
                                        onConfirmDownload = {
                                            PdfHelper.generateAndSharePdf(context, reportFileName, "TASK LOG $dateStr", reportText)
                                        }
                                        showPreviewDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Download", fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    items(groupTasks, key = { it.id }) { task ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = { isChecked ->
                                            viewModel.updateTask(task.copy(isCompleted = isChecked))
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = GreenAccent, uncheckedColor = GoldPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = task.title,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
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
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = task.priority,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (task.priority) {
                                                        "HIGH" -> RedAccent
                                                        "MEDIUM" -> OrangeAccent
                                                        else -> SlateSecondary
                                                    }
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(task.dueTime, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        }
                                    }
                                }

                                if (viewModel.isLoggedIn && viewModel.currentUser?.isToufiq == true) {
                                    IconButton(
                                        onClick = { viewModel.deleteTask(task) },
                                        colors = IconButtonDefaults.iconButtonColors(contentColor = RedAccent)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
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
