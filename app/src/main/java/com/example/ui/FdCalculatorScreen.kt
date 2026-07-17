package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FdCalculatorScreen(
    viewModel: BankingViewModel,
    modifier: Modifier = Modifier
) {
    var amountText by remember { mutableStateOf("") }
    var tenure3m by remember { mutableStateOf(true) } // true = 3 Months, false = 1 Year
    var taxReturnYes by remember { mutableStateOf(true) } // true = Yes, false = No

    val keyboardController = LocalSoftwareKeyboardController.current

    val amount = amountText.toDoubleOrNull() ?: 0.0

    // Profit calculation:
    // Interest Rate: 3 Months = 8.00% p.a., 1 Year = 11.00% p.a.
    val rate = if (tenure3m) 0.08 else 0.11
    val tenureMonths = if (tenure3m) 3.0 else 12.0
    val grossProfit = amount * rate * (tenureMonths / 12.0)

    // Tax calculation:
    // Tax rate: 10% if Tax Return is Yes, 15% if No
    val taxRate = if (taxReturnYes) 0.10 else 0.15
    val taxAmount = grossProfit * taxRate

    val netInterest = grossProfit - taxAmount
    val totalMaturity = amount + netInterest

    val formatter = DecimalFormat("#,##,##0.00")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "FD CALCULATOR",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateDark)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Introductory signboard card removed as requested

            // Input Fields Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Investment Parameters",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GoldPrimary
                    )

                    // Principal Amount Input
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("FD Principal Amount") },
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

                    // Tenure Toggle
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Select Tenure",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { tenure3m = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (tenure3m) GoldPrimary else SlateDark,
                                    contentColor = if (tenure3m) SlateDark else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("3 Months", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { tenure3m = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!tenure3m) GoldPrimary else SlateDark,
                                    contentColor = if (!tenure3m) SlateDark else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("1 Year", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Tax Return Status Toggle
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Tax Return Submitted (TIN / PSR)?",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { taxReturnYes = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (taxReturnYes) GoldPrimary else SlateDark,
                                    contentColor = if (taxReturnYes) SlateDark else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Yes (10% Tax)", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { taxReturnYes = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!taxReturnYes) GoldPrimary else SlateDark,
                                    contentColor = if (!taxReturnYes) SlateDark else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("No (15% Tax)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Calculation Output Card
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
                            "ESTIMATED EARNINGS",
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

                    // Outputs
                    OutputRow("Principal Amount", "৳ ${formatter.format(amount)}")
                    OutputRow("Interest Rate", if (tenure3m) "8.00% p.a." else "11.00% p.a.")
                    OutputRow("Gross Interest", "৳ ${formatter.format(grossProfit)}", isBold = true)
                    OutputRow("AIT / Tax Deducted (${if (taxReturnYes) "10%" else "15%"})", "৳ ${formatter.format(taxAmount)}", color = RedAccent)
                    
                    if (tenure3m) {
                        OutputRow("after 3 months total profit after tax deduct:", "৳ ${formatter.format(netInterest)}", color = GreenAccent, isBold = true)
                    } else {
                        OutputRow("Monthly Gain after Tax Deduct:", "৳ ${formatter.format(netInterest / 12.0)}", color = GreenAccent, isBold = true)
                    }

                    HorizontalDivider(color = SlateSecondary.copy(alpha = 0.4f))

                    OutputRow("Net Interest Profit", "৳ ${formatter.format(netInterest)}", color = GreenAccent, isBold = true)
                    OutputRow("Total Maturity Value", "৳ ${formatter.format(totalMaturity)}", color = GoldLight, isBold = true, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun OutputRow(
    label: String,
    value: String,
    color: Color = Color.White,
    isBold: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            color = Color.LightGray,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}
