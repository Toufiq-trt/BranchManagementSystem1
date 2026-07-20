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
import androidx.compose.material.icons.filled.Percent
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
fun LoanCalculatorScreen(
    viewModel: BankingViewModel,
    modifier: Modifier = Modifier
) {
    var amountText by remember { mutableStateOf("") }
    var interestText by remember { mutableStateOf("9.0") }
    var tenureText by remember { mutableStateOf("5") } // Default 5 years
    var tenureInYears by remember { mutableStateOf(true) } // true = Years, false = Months

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val interestRate = interestText.toDoubleOrNull() ?: 0.0
    val tenureVal = tenureText.toIntOrNull() ?: 0

    val n = if (tenureInYears) tenureVal * 12 else tenureVal
    val r = (interestRate / 12.0) / 100.0

    // EMI Calculation
    val emi = if (amount > 0.0 && r > 0.0 && n > 0) {
        val base = (1.0 + r).pow(n.toDouble())
        (amount * r * base) / (base - 1.0)
    } else if (amount > 0.0 && n > 0 && r == 0.0) {
        amount / n
    } else {
        0.0
    }

    val totalPayable = if (n > 0) emi * n else 0.0
    val extraPaid = if (totalPayable > amount) totalPayable - amount else 0.0

    val formatter = DecimalFormat("#,##,##0.00")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "LOAN CALCULATOR",
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
                        "Loan Eligibility & Pricing",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GoldPrimary
                    )

                    // Principal Loan Amount
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Requested Loan Amount") },
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
                        label = { Text("Annual Interest Rate") },
                        suffix = { Icon(Icons.Default.Percent, contentDescription = null, modifier = Modifier.size(16.dp), tint = GoldPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = SlateSecondary,
                            focusedLabelColor = GoldPrimary
                        )
                    )

                    // Loan Tenure
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = tenureText,
                            onValueChange = { tenureText = it },
                            label = { Text("Tenure") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.2f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = SlateSecondary,
                                focusedLabelColor = GoldPrimary
                            )
                        )

                        // Tenure units selector
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .background(SlateDark, RoundedCornerShape(8.dp))
                                .padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { tenureInYears = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (tenureInYears) GoldPrimary else Color.Transparent,
                                    contentColor = if (tenureInYears) SlateDark else Color.White
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Yrs", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { tenureInYears = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!tenureInYears) GoldPrimary else Color.Transparent,
                                    contentColor = if (!tenureInYears) SlateDark else Color.White
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Mths", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            "ESTIMATED REPAYMENT SCHEDULE",
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

                    OutputRow("Principal Requested", "৳ ${formatter.format(amount)}")
                    OutputRow("Annual Interest Rate", "$interestRate% p.a.")
                    OutputRow("Total Number of Installments", "$n Months")
                    
                    HorizontalDivider(color = SlateSecondary.copy(alpha = 0.4f))

                    OutputRow("Monthly EMI Installment", "৳ ${formatter.format(emi)}", color = GoldLight, isBold = true, fontSize = 16.sp)
                    OutputRow("Extra Interest Cost", "৳ ${formatter.format(extraPaid)}", color = RedAccent)
                    OutputRow("Total Repayable (Principal+Interest)", "৳ ${formatter.format(totalPayable)}", color = GreenAccent, isBold = true, fontSize = 15.sp)
                }
            }
        }
    }
}
