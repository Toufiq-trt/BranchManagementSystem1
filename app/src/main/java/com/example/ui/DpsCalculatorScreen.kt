package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.DecimalFormat
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DpsCalculatorScreen(
    viewModel: BankingViewModel,
    modifier: Modifier = Modifier
) {
    var monthlyDepositText by remember { mutableStateOf("") }
    var interestText by remember { mutableStateOf("7.5") }
    var tenureText by remember { mutableStateOf("5") } // Default 5 years
    var taxReturnYes by remember { mutableStateOf(true) } // true = Yes (10% tax), false = No (15% tax)

    val monthlyDeposit = monthlyDepositText.toDoubleOrNull() ?: 0.0
    val interestRate = interestText.toDoubleOrNull() ?: 0.0
    val tenureYears = tenureText.toIntOrNull() ?: 0

    val n = tenureYears * 12
    val i = (interestRate / 12.0) / 100.0

    // DPS Compound Formula: M = P * [ (1 + i)^n - 1 ] / i * (1 + i)
    val maturityValue = if (monthlyDeposit > 0.0 && n > 0) {
        if (i > 0.0) {
            monthlyDeposit * ((1.0 + i).pow(n.toDouble()) - 1.0) / i * (1.0 + i)
        } else {
            monthlyDeposit * n
        }
    } else {
        0.0
    }

    val totalSavings = monthlyDeposit * n
    val profit = if (maturityValue > totalSavings) maturityValue - totalSavings else 0.0

    val taxRate = if (taxReturnYes) 0.10 else 0.15
    val taxAmount = profit * taxRate
    val pocketAmount = maturityValue - taxAmount

    val formatter = DecimalFormat("#,##,##0.00")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "DPS CALCULATOR",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.currentScreen = "dashboard" }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GoldPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateDark),
                windowInsets = WindowInsets(0.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Introductory signboard removed as requested
            
            // Inputs Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Deposit Parameters",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GoldPrimary
                    )

                    // Monthly Deposit
                    OutlinedTextField(
                        value = monthlyDepositText,
                        onValueChange = { monthlyDepositText = it },
                        label = { Text("Monthly Deposit Installment") },
                        prefix = { Text("৳ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = SlateSecondary,
                            focusedLabelColor = GoldPrimary
                        )
                    )

                    // Annual Interest Rate
                    OutlinedTextField(
                        value = interestText,
                        onValueChange = { interestText = it },
                        label = { Text("DPS Annual Interest Rate") },
                        suffix = { Text("%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = SlateSecondary,
                            focusedLabelColor = GoldPrimary
                        )
                    )

                    // Tenure (Years) and Tax Return side-by-side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = tenureText,
                            onValueChange = { tenureText = it },
                            label = { Text("Tenure") },
                            suffix = { Text("Yrs") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = SlateSecondary,
                                focusedLabelColor = GoldPrimary
                            )
                        )

                        Column(
                            modifier = Modifier.weight(1.5f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Tax Return Submitted?",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { taxReturnYes = true },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (taxReturnYes) GoldPrimary else SlateDark,
                                        contentColor = if (taxReturnYes) SlateDark else Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("YES (10%)", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }

                                Button(
                                    onClick = { taxReturnYes = false },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!taxReturnYes) GoldPrimary else SlateDark,
                                        contentColor = if (!taxReturnYes) SlateDark else Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("NO (15%)", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Calculations Output Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ESTIMATED CUMULATIVE SAVINGS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    HorizontalDivider(color = SlateSecondary.copy(alpha = 0.4f))

                    OutputRow("Total Savings:", "৳ ${formatter.format(totalSavings)}")
                    OutputRow("Compund Interest Earned :", "৳ ${formatter.format(profit)}", color = GreenAccent)
                    OutputRow("Maturity Value:", "৳ ${formatter.format(maturityValue)}")
                    OutputRow("AIT /Tax:", "৳ ${formatter.format(taxAmount)}", color = RedAccent)
                    
                    HorizontalDivider(color = SlateSecondary.copy(alpha = 0.4f))

                    OutputRow("Pocket Amount:", "৳ ${formatter.format(pocketAmount)}", color = GoldLight, isBold = true, fontSize = 16.sp)
                }
            }
        }
    }
}
