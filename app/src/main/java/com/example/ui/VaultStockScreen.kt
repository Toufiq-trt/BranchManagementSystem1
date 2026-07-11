package com.example.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*

@Composable
fun VaultStockScreen(
    viewModel: BankingViewModel,
    stockType: String, // "PRIZE_BOND" or "PAY_ORDER"
    modifier: Modifier = Modifier
) {
    val logs = if (stockType == "PRIZE_BOND") {
        viewModel.prizeBondLogs.collectAsStateWithLifecycle().value
    } else {
        viewModel.payOrderLogs.collectAsStateWithLifecycle().value
    }

    val currentQty = if (stockType == "PRIZE_BOND") {
        viewModel.prizeBondQty.collectAsState().value
    } else {
        viewModel.payOrderQty.collectAsState().value
    }

    var isEditing by remember { mutableStateOf(false) }
    var inputQuantity by remember { mutableStateOf("") }
    var operatorName by remember { mutableStateOf("Officer Toufiq") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (stockType == "PRIZE_BOND") "PRIZE BOND INVENTORY" else "PAY ORDER INVENTORY",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = GoldPrimary,
            letterSpacing = 1.sp
        )

        // Large Balance Display Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "CURRENT VAULT BALANCE",
                    fontSize = 11.sp,
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "$currentQty Units",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                if (viewModel.isLoggedIn) {
                    Button(
                        onClick = {
                            inputQuantity = currentQty.toString()
                            isEditing = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Adjust Vault Stock Balance")
                    }
                }
            }
        }

        // Vault Adjustment History Logs list
        Text(
            text = "Audit trail logs (No deletion allowed)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = GoldPrimary
        )

        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Stock logs ledger is currently empty.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Audit Balance Change",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Shifted: ${log.previousQuantity} -> ${log.newQuantity} units",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "By officer: ${log.editedBy}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(log.dateStr, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(log.timeStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Adjust balance alert dialog form
    if (isEditing) {
        AlertDialog(
            onDismissRequest = { isEditing = false },
            confirmButton = {
                Button(
                    onClick = {
                        val newQty = inputQuantity.toIntOrNull()
                        if (newQty != null && operatorName.isNotBlank()) {
                            viewModel.updateQuantityLog(stockType, newQty, operatorName)
                            isEditing = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark)
                ) {
                    Text("Apply Adjustment")
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditing = false }) {
                    Text("Cancel")
                }
            },
            title = {
                Text("Record Vault Stock Modification", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Updating will generate an immutable, cryptographic log of modification. Make sure details are exact.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    OutlinedTextField(
                        value = inputQuantity,
                        onValueChange = { inputQuantity = it },
                        label = { Text("New Stock Quantity") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = operatorName,
                        onValueChange = { operatorName = it },
                        label = { Text("Operator / Supervisor Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }
}
