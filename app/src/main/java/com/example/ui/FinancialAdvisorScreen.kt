package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SlateDark
import com.example.util.PdfHelper
import kotlin.math.pow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialAdvisorScreen(
    viewModel: BankingViewModel,
    onBack: () -> Unit = { viewModel.currentScreen = "dashboard" }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- Input Form State ---
    // 1. Personal Info
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("32") }
    var employment by remember { mutableStateOf("Service") } // Job / Business

    // 2. Monthly Income
    var salary by remember { mutableStateOf("60000") }
    var otherIncome by remember { mutableStateOf("10000") }

    // 3. Expenses Breakdown
    var rentExpense by remember { mutableStateOf("15000") }
    var foodExpense by remember { mutableStateOf("12000") }
    var electricityExpense by remember { mutableStateOf("3000") }
    var transportExpense by remember { mutableStateOf("4000") }
    var otherExpense by remember { mutableStateOf("6000") }

    // 4. Cash in Hand
    var cashInHand by remember { mutableStateOf("50000") }

    // 5. Loan Option (Default No)
    var hasLoan by remember { mutableStateOf(false) }
    var loanAmount by remember { mutableStateOf("200000") }
    var loanOutstanding by remember { mutableStateOf("150000") }
    var loanEmi by remember { mutableStateOf("5000") }
    var loanFinishDate by remember { mutableStateOf("3 Years") }

    // 6. Running Savings Option (DPS / FDR - Default No)
    var hasRunningSavings by remember { mutableStateOf(false) }
    var savingsCategory by remember { mutableStateOf("DPS") } // DPS or FDR or Both
    var savingsAmount by remember { mutableStateOf("5000") }
    var savingsTenure by remember { mutableStateOf("3") }
    var savingsInterestRate by remember { mutableStateOf("10.5") }
    var fdrPayoutType by remember { mutableStateOf("Interest Reinvested (1 Year)") } // Monthly Interest vs Reinvested after 3 Months / 1 Year
    var savingsStartDate by remember { mutableStateOf("01-01-2024") }
    var savingsFinishDate by remember { mutableStateOf("01-01-2027") }

    // Live Calculations
    val totalIncome = (salary.toDoubleOrNull() ?: 0.0) + (otherIncome.toDoubleOrNull() ?: 0.0)
    val totalExpense = (rentExpense.toDoubleOrNull() ?: 0.0) +
            (foodExpense.toDoubleOrNull() ?: 0.0) +
            (electricityExpense.toDoubleOrNull() ?: 0.0) +
            (transportExpense.toDoubleOrNull() ?: 0.0) +
            (otherExpense.toDoubleOrNull() ?: 0.0)
    val netSavings = (totalIncome - totalExpense).coerceAtLeast(0.0)

    // --- Output State ---
    var isLoading by remember { mutableStateOf(false) }
    var advisorReportText by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Form, 1: Report

    val fmt = remember { NumberFormat.getNumberInstance(Locale.US) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "FINANCIAL ADVISOR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = GoldPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Senior Banking Wealth & Investment Advisory",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GoldPrimary
                        )
                    }
                },
                actions = {
                    if (advisorReportText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val input = PdfHelper.FinancialAdvisorInput(
                                    name = name,
                                    age = age,
                                    employment = employment,
                                    salary = salary.toDoubleOrNull() ?: 0.0,
                                    otherIncome = otherIncome.toDoubleOrNull() ?: 0.0,
                                    rentExpense = rentExpense.toDoubleOrNull() ?: 0.0,
                                    foodExpense = foodExpense.toDoubleOrNull() ?: 0.0,
                                    electricityExpense = electricityExpense.toDoubleOrNull() ?: 0.0,
                                    transportExpense = transportExpense.toDoubleOrNull() ?: 0.0,
                                    otherExpense = otherExpense.toDoubleOrNull() ?: 0.0,
                                    totalIncome = totalIncome,
                                    totalExpense = totalExpense,
                                    monthlySavings = netSavings,
                                    cashInHand = cashInHand.toDoubleOrNull() ?: 0.0,
                                    hasLoan = hasLoan,
                                    loanAmount = loanAmount.toDoubleOrNull() ?: 0.0,
                                    loanOutstanding = loanOutstanding.toDoubleOrNull() ?: 0.0,
                                    loanEmi = loanEmi.toDoubleOrNull() ?: 0.0,
                                    loanFinishDate = loanFinishDate,
                                    hasRunningSavings = hasRunningSavings,
                                    savingsCategory = savingsCategory,
                                    savingsAmount = savingsAmount.toDoubleOrNull() ?: 0.0,
                                    savingsTenure = savingsTenure.toDoubleOrNull() ?: 0.0,
                                    savingsInterestRate = savingsInterestRate.toDoubleOrNull() ?: 0.0,
                                    fdrPayoutType = fdrPayoutType,
                                    savingsStartDate = savingsStartDate,
                                    savingsFinishDate = savingsFinishDate,
                                    monthlyExpenses = totalExpense
                                )
                                val fileName = "Financial_Advisor_Report_${name.ifBlank { "Client" }}.pdf"
                                PdfHelper.generateFinancialAdvisorPdf(context, fileName, input, advisorReportText)
                            }
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = GoldPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SlateDark
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Mode Selector Bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SlateDark.copy(alpha = 0.95f),
                contentColor = GoldPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("গ্রাহকের তথ্য ফর্ম", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("বিশ্লেষণ ও গাইডলাইন", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = GoldPrimary, strokeWidth = 3.dp)
                        Text(
                            text = "আর্থিক উপদেষ্টা আপনার বাজেট, সঞ্চয় ও ব্যাংক রিটার্ন প্রস্তুত করছে...",
                            color = GoldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            text = "৫০/৩০/২০ বাজেট নিয়ম ও FIRE ক্যালকুলেশন প্রস্তুত করা হচ্ছে",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 11.5.sp
                        )
                    }
                }
            } else if (selectedTab == 0) {
                // Form Input Tab
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Surface(
                            color = SlateDark,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("আর্থিক তথ্য সংগ্রহ ফর্ম", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 14.sp)
                                    Text("সঠিক ব্যাংকিং গাইডলাইন পেতে আপনার আয়, খরচ ও বর্তমান সঞ্চয়ের তথ্য লিখুন।", fontSize = 11.sp, color = Color.LightGray)
                                }
                            }
                        }
                    }

                    // 1. Personal Information
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("১. ব্যক্তিগত তথ্য (Personal Information)", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text("গ্রাহকের নাম") },
                                        placeholder = { Text("উদাহরণ: মোঃ রফিকুল ইসলাম") },
                                        modifier = Modifier.weight(1.5f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = age,
                                        onValueChange = { age = it },
                                        label = { Text("বয়স (Age)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                                OutlinedTextField(
                                    value = employment,
                                    onValueChange = { employment = it },
                                    label = { Text("পেশা / ব্যবসা (Job/Business)") },
                                    placeholder = { Text("উদাহরণ: সার্ভিস / ব্যবসা / শিক্ষকতা") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    // 2. Monthly Income
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("২. মাসিক আয় (Monthly Income)", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = salary,
                                        onValueChange = { salary = it },
                                        label = { Text("বেতন থেকে আয় (Tk)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = otherIncome,
                                        onValueChange = { otherIncome = it },
                                        label = { Text("অন্যান্য আয় (Tk)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }

                    // 3. Monthly Expenses
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("৩. মাসিক খরচ খাতসমূহ (Monthly Expenses)", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 13.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = rentExpense,
                                        onValueChange = { rentExpense = it },
                                        label = { Text("বাসা ভাড়া (Rent Tk)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = foodExpense,
                                        onValueChange = { foodExpense = it },
                                        label = { Text("খাবার খরচ (Food Tk)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = electricityExpense,
                                        onValueChange = { electricityExpense = it },
                                        label = { Text("বিদ্যুৎ ও ইউটিলিটি (Tk)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = transportExpense,
                                        onValueChange = { transportExpense = it },
                                        label = { Text("যাতায়াত খরচ (Tk)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                                OutlinedTextField(
                                    value = otherExpense,
                                    onValueChange = { otherExpense = it },
                                    label = { Text("অন্যান্য খরচ (Others Tk)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    // 4. Auto-Calculated Savings Summary Live Card
                    item {
                        Surface(
                            color = SlateDark,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("স্বয়ংক্রিয় সঞ্চয় হিসাব (Savings Calculation)", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 13.sp)
                                    Icon(Icons.Default.Calculate, contentDescription = null, tint = GoldLight)
                                }
                                HorizontalDivider(color = GoldPrimary.copy(alpha = 0.3f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("মোট আয় (Total Income):", fontSize = 12.sp, color = Color.White)
                                    Text("৳ ${fmt.format(totalIncome)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GoldLight)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("মোট খরচ (Total Expenses):", fontSize = 12.sp, color = Color.White)
                                    Text("৳ ${fmt.format(totalExpense)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.LightGray)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("অবশিষ্ট নিট সঞ্চয় (Net Savings):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GoldPrimary)
                                    Text("৳ ${fmt.format(netSavings)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GoldPrimary)
                                }
                            }
                        }
                    }

                    // 5. Cash in Hand
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("৪. হাতে নগদ টাকা (Cash in Hand)", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 13.sp)
                                OutlinedTextField(
                                    value = cashInHand,
                                    onValueChange = { cashInHand = it },
                                    label = { Text("হাতে নগদ বা সেভিংস ব্যালেন্স (Tk)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    // 6. Loan Option (Default NO)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("৫. কোনো ব্যাংক ঋণ আছে কি? (Loan)", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 13.sp)
                                        Text("ডিফল্ট: না (যদি ঋণ থাকে তবে অন করুন)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Switch(
                                        checked = hasLoan,
                                        onCheckedChange = { hasLoan = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = SlateDark,
                                            checkedTrackColor = GoldPrimary
                                        )
                                    )
                                }

                                if (hasLoan) {
                                    HorizontalDivider(color = GoldPrimary.copy(alpha = 0.2f))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = loanAmount,
                                            onValueChange = { loanAmount = it },
                                            label = { Text("ঋণের মূল পরিমাণ (Tk)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = loanOutstanding,
                                            onValueChange = { loanOutstanding = it },
                                            label = { Text("বর্তমান বকেয়া (Tk)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = loanEmi,
                                            onValueChange = { loanEmi = it },
                                            label = { Text("মাসিক কিস্তি/EMI (Tk)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = loanFinishDate,
                                            onValueChange = { loanFinishDate = it },
                                            label = { Text("ঋণ শেষের মেয়াদ/তারিখ") },
                                            placeholder = { Text("যেমন: 3 Years / 2027") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 7. Running Savings (DPS/FDR Option - Default NO)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("৬. চলমান ডিপিএস বা এফডিআর সঞ্চয় আছে কি?", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 13.sp)
                                        Text("ডিফল্ট: না (চলমান সঞ্চয় থাকলে অন করুন)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Switch(
                                        checked = hasRunningSavings,
                                        onCheckedChange = { hasRunningSavings = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = SlateDark,
                                            checkedTrackColor = GoldPrimary
                                        )
                                    )
                                }

                                if (hasRunningSavings) {
                                    HorizontalDivider(color = GoldPrimary.copy(alpha = 0.2f))

                                    // Category Choice
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = savingsCategory == "DPS",
                                            onClick = { savingsCategory = "DPS" },
                                            label = { Text("DPS (ডিপিএস)", fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = GoldPrimary,
                                                selectedLabelColor = SlateDark
                                            )
                                        )
                                        FilterChip(
                                            selected = savingsCategory == "FDR",
                                            onClick = { savingsCategory = "FDR" },
                                            label = { Text("FDR (এফডিআর)", fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = GoldPrimary,
                                                selectedLabelColor = SlateDark
                                            )
                                        )
                                        FilterChip(
                                            selected = savingsCategory == "Both",
                                            onClick = { savingsCategory = "Both" },
                                            label = { Text("উভয়ই (Both)", fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = GoldPrimary,
                                                selectedLabelColor = SlateDark
                                            )
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = savingsAmount,
                                            onValueChange = { savingsAmount = it },
                                            label = { Text(if (savingsCategory == "DPS") "মাসিক ডিপিএস (Tk)" else "এফডিআর মূলধন (Tk)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = savingsTenure,
                                            onValueChange = { savingsTenure = it },
                                            label = { Text("মেয়াদ (বছর)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }

                                    OutlinedTextField(
                                        value = savingsInterestRate,
                                        onValueChange = { savingsInterestRate = it },
                                        label = { Text("সুদ / মুনাফার হার (%)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    if (savingsCategory == "FDR" || savingsCategory == "Both") {
                                        Text("এফডিআর মুনাফা পরিশোধের ধরণ (FDR Type):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FilterChip(
                                                selected = fdrPayoutType.contains("Monthly"),
                                                onClick = { fdrPayoutType = "Monthly Interest Payout" },
                                                label = { Text("মাসিক মুনাফা তোলা", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = GoldPrimary,
                                                    selectedLabelColor = SlateDark
                                                )
                                            )
                                            FilterChip(
                                                selected = fdrPayoutType.contains("Reinvested"),
                                                onClick = { fdrPayoutType = "Interest Reinvested (1 Year)" },
                                                label = { Text("১ বছর পর চক্রবৃদ্ধি", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = GoldPrimary,
                                                    selectedLabelColor = SlateDark
                                                )
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = savingsStartDate,
                                            onValueChange = { savingsStartDate = it },
                                            label = { Text("শুরুর তারিখ") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = savingsFinishDate,
                                            onValueChange = { savingsFinishDate = it },
                                            label = { Text("মেয়াদ শেষের তারিখ") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action Button
                    item {
                        Button(
                            onClick = {
                                isLoading = true
                                scope.launch {
                                    val inputData = PdfHelper.FinancialAdvisorInput(
                                        name = name.ifBlank { "সম্মানিত গ্রাহক" },
                                        age = age.ifBlank { "32" },
                                        employment = employment.ifBlank { "সার্ভিস / ব্যবসা" },
                                        salary = salary.toDoubleOrNull() ?: 0.0,
                                        otherIncome = otherIncome.toDoubleOrNull() ?: 0.0,
                                        rentExpense = rentExpense.toDoubleOrNull() ?: 0.0,
                                        foodExpense = foodExpense.toDoubleOrNull() ?: 0.0,
                                        electricityExpense = electricityExpense.toDoubleOrNull() ?: 0.0,
                                        transportExpense = transportExpense.toDoubleOrNull() ?: 0.0,
                                        otherExpense = otherExpense.toDoubleOrNull() ?: 0.0,
                                        totalIncome = totalIncome,
                                        totalExpense = totalExpense,
                                        monthlySavings = netSavings,
                                        cashInHand = cashInHand.toDoubleOrNull() ?: 0.0,
                                        hasLoan = hasLoan,
                                        loanAmount = loanAmount.toDoubleOrNull() ?: 0.0,
                                        loanOutstanding = loanOutstanding.toDoubleOrNull() ?: 0.0,
                                        loanEmi = loanEmi.toDoubleOrNull() ?: 0.0,
                                        loanFinishDate = loanFinishDate,
                                        hasRunningSavings = hasRunningSavings,
                                        savingsCategory = savingsCategory,
                                        savingsAmount = savingsAmount.toDoubleOrNull() ?: 0.0,
                                        savingsTenure = savingsTenure.toDoubleOrNull() ?: 0.0,
                                        savingsInterestRate = savingsInterestRate.toDoubleOrNull() ?: 0.0,
                                        fdrPayoutType = fdrPayoutType,
                                        savingsStartDate = savingsStartDate,
                                        savingsFinishDate = savingsFinishDate,
                                        monthlyExpenses = totalExpense
                                    )

                                    val resultText = runFinancialAdvisorAnalysis(inputData)
                                    advisorReportText = resultText
                                    isLoading = false
                                    selectedTab = 1
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("বিশ্লেষণ ও গাইডলাইন তৈরি করুন", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            } else {
                // Rendered Output Roadmap Tab
                if (advisorReportText.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("কোনো রিপোর্ট পাওয়া যায়নি", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("অনুগ্রহ করে গ্রাহকের তথ্য ফর্মে তথ্য পূরণ করে 'বিশ্লেষণ ও গাইডলাইন তৈরি করুন' বাটনে চাপ দিন।", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { selectedTab = 0 },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark)
                            ) {
                                Text("গ্রাহকের তথ্য ফর্মে ফিরে যান")
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Surface(
                            color = SlateDark,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("আর্থিক পরামর্শক ও ব্যাংকিং ওয়েলথ রিপোর্ট", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 13.sp)
                                    Text("অফিশিয়াল ব্যাংকিং অ্যাডভাইজরি | ৫০/৩০/২০ বাজেট ও FIRE অ্যানালাইসিস", fontSize = 11.sp, color = Color.LightGray)
                                }
                                Button(
                                    onClick = {
                                        val input = PdfHelper.FinancialAdvisorInput(
                                            name = name,
                                            age = age,
                                            employment = employment,
                                            salary = salary.toDoubleOrNull() ?: 0.0,
                                            otherIncome = otherIncome.toDoubleOrNull() ?: 0.0,
                                            rentExpense = rentExpense.toDoubleOrNull() ?: 0.0,
                                            foodExpense = foodExpense.toDoubleOrNull() ?: 0.0,
                                            electricityExpense = electricityExpense.toDoubleOrNull() ?: 0.0,
                                            transportExpense = transportExpense.toDoubleOrNull() ?: 0.0,
                                            otherExpense = otherExpense.toDoubleOrNull() ?: 0.0,
                                            totalIncome = totalIncome,
                                            totalExpense = totalExpense,
                                            monthlySavings = netSavings,
                                            cashInHand = cashInHand.toDoubleOrNull() ?: 0.0,
                                            hasLoan = hasLoan,
                                            loanAmount = loanAmount.toDoubleOrNull() ?: 0.0,
                                            loanOutstanding = loanOutstanding.toDoubleOrNull() ?: 0.0,
                                            loanEmi = loanEmi.toDoubleOrNull() ?: 0.0,
                                            loanFinishDate = loanFinishDate,
                                            hasRunningSavings = hasRunningSavings,
                                            savingsCategory = savingsCategory,
                                            savingsAmount = savingsAmount.toDoubleOrNull() ?: 0.0,
                                            savingsTenure = savingsTenure.toDoubleOrNull() ?: 0.0,
                                            savingsInterestRate = savingsInterestRate.toDoubleOrNull() ?: 0.0,
                                            fdrPayoutType = fdrPayoutType,
                                            savingsStartDate = savingsStartDate,
                                            savingsFinishDate = savingsFinishDate,
                                            monthlyExpenses = totalExpense
                                        )
                                        val fileName = "Financial_Advisor_Report_${name.ifBlank { "Client" }}.pdf"
                                        PdfHelper.generateFinancialAdvisorPdf(context, fileName, input, advisorReportText)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PDF প্রিন্ট", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(advisorReportText.split("\n").size) { index ->
                                    val line = advisorReportText.split("\n")[index]
                                    val trimmed = line.trim()
                                    if (trimmed.isNotBlank()) {
                                        val isHeader = trimmed.startsWith("১.") || trimmed.startsWith("২.") || trimmed.startsWith("৩.") || trimmed.startsWith("৪.") || trimmed.startsWith("৫.") ||
                                                trimmed.contains("গ্রাহকের সংক্ষিপ্ত") || trimmed.contains("আয় ও খরচের অনুপাত") || trimmed.contains("আর্থিক স্বাধীনতার") || trimmed.contains("সম্পূর্ণ ব্যাংকিং বিনিয়োগ") || trimmed.contains("বাজেট পুনর্গঠন") || trimmed.contains("কোটিপতি")

                                        val isTreeLine = trimmed.contains("├──") || trimmed.contains("└──") || trimmed.contains("──>") || trimmed.contains("▼") || (trimmed.startsWith("│") && !trimmed.contains("http")) || trimmed.startsWith("[মাসিক") || trimmed.startsWith("[৩ বছর") || trimmed.startsWith("[৬ বছর") || trimmed.startsWith("[২৫-৩০") || trimmed.startsWith("+---") || trimmed.startsWith("|") || trimmed.contains("---|") || (trimmed.contains("|") && trimmed.endsWith("|"))

                                        if (isHeader) {
                                            Text(
                                                text = trimmed,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = GoldPrimary,
                                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                            )
                                        } else if (isTreeLine) {
                                            Surface(
                                                color = Color(0xFF0F172A),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = line,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.5.sp,
                                                    color = Color(0xFFE2B714),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        } else if (trimmed.contains("ভালো (Good)") || trimmed.contains("✅") || trimmed.contains("ফাইনানশিয়াল ফ্রীডম")) {
                                            Surface(
                                                color = Color(0xFF1B382B),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = trimmed,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF66BB6A),
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        } else if (trimmed.contains("সতর্কতা") || trimmed.contains("⚠️")) {
                                            Surface(
                                                color = Color(0xFF38231B),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = trimmed,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFFFFA726),
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        } else if (trimmed.startsWith("ধাপ") || trimmed.startsWith("Step") || trimmed.startsWith("মাইলফলক")) {
                                            Surface(
                                                color = SlateDark.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = trimmed,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = GoldLight,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = line,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 17.sp
                                            )
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
}

private suspend fun runFinancialAdvisorAnalysis(input: PdfHelper.FinancialAdvisorInput): String = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
        try {
            val systemInstruction = """
                You are an elite, senior Financial Advisor at Shimanto Bank PLC, Bangladesh.
                Your goal is to provide 100% mathematically accurate, realistic, highly structured, visual step-by-step Financial Freedom & Wealth Roadmap (কোটিপতি ও ফাইনানশিয়াল ফ্রীডম রোডম্যাপ) in BANGLA tailored strictly to the client's actual financial numbers.

                CORE MATHEMATICAL & ADVISORY MANDATES:
                1. Exact Math & 50/30/20 Budgeting Rule:
                   - 50% Needs (মৌলিক প্রয়োজন: বাসা ভাড়া, ইউটিলিটি, লোন ইএমআই, খাদ্য, যাতায়াত). Sub-items MUST SUM EXACTLY to 50% of Income!
                   - 30% Wants/Lifestyle & Travel (জীবনযাত্রা, বিনোদন ও ভ্রমণ): Sub-items MUST SUM EXACTLY to 30% of Income! (e.g. Vacation/Travel budget + Personal Expenses).
                   - 20% Savings & Investment Target (সঞ্চয় ও বিনিয়োগ): Emergency Reserve + DPS. Sub-items MUST SUM EXACTLY to 20%!

                2. Practical Advice for Low Current Savings:
                   - Do NOT demand unrealistic jumps (e.g., if customer currently saves BDT 3,000, do NOT force them to start a BDT 9,000 DPS immediately).
                   - Start Phase 1 with what the customer CAN realistically save RIGHT NOW (e.g. BDT 2,000 - 3,000/month DPS).
                   - Provide a step-by-step gradual increase strategy (e.g. adding BDT 1,000/year as income increases) to comfortably reach the 20% savings target over time.

                3. Visual ASCII Tree Diagram (গাঠনিক ফ্লো-চার্ট):
                   Draw a clean visual tree chart using ASCII characters (`├──`, `└──`, `│`, `──>`, `▼`) mapping monthly income down to exact 50/30/20 sub-items, emergency reserve, and compounding investment stages.

                4. Precise Compounding Investment Math (ডিপিএস -> এফডিআর -> ২য় ডিপিএস -> ডাবল মানি স্কিম):
                   - DPS Compound Interest Formula: M = P * [((1 + i)^n - 1) / i] * (1 + i) where i = 10.5% / 12 = 0.00875.
                   - FDR Net Return: 11% annual rate with 15% AIT source tax deduction (Net Annual = Principal * 9.35%).
                   - 2nd DPS starting at Year 4 using (Base DPS + Step-Up + FDR Monthly Net Profit).
                   - Double Money Scheme (7-Year doubling).
                   - Long-term compounding wealth snowball up to 25-30 years reaching Financial Freedom (৳১ কোটি+ থেকে ৳৩+ কোটি!).

                5. Required 6 Structured Sections in BANGLA:
                   ১. গ্রাহকের প্রোফাইল ও গাণিতিক বাজেট বিশ্লেষণ (50/30/20 Exact Math Budget Analysis)
                   ২. বাজেট প্রবাহ ও গাণিতিক দৃশ্যমান ছক (Visual ASCII Budget Tree)
                   ৩. বাস্তবসম্মত ধাপ-ভিত্তিক চক্রবৃদ্ধি ডিপিএস রোডম্যাপ (Step-by-step Starting Small to Scaling Up Roadmap)
                   ৪. বছরভিত্তিক প্রবৃদ্ধি ও কোটিপতি রোডম্যাপ ছক (Milestone Wealth Table formatted with '|' columns)
                   ৫. জীবনযাত্রার নিরাপত্তা, চিকিৎসা, ভ্রমণ ও প্যাসিভ ইনকাম (Life Security, Travel & Monthly Passive Income)
                   ৬. বর্তমান সাধারণ পদ্ধতি বনাম AI এডভাইজর রোডম্যাপ তুলনা ছক (Comparison Table: Traditional Savings vs AI Compounding Roadmap)

                6. Table Formatting:
                   Use clean ASCII table format with '|' columns for sections 4 and 6.

                CRITICAL: Every sub-allocation must sum up to its exact header budget. No math errors!
            """.trimIndent()

            val prompt = """
                Customer Profile Input:
                Name: ${input.name}
                Age: ${input.age}
                Profession: ${input.employment}
                Salary: BDT ${input.salary}
                Other Income: BDT ${input.otherIncome}
                Rent Expense: BDT ${input.rentExpense}
                Food Expense: BDT ${input.foodExpense}
                Electricity Expense: BDT ${input.electricityExpense}
                Transport Expense: BDT ${input.transportExpense}
                Other Expense: BDT ${input.otherExpense}
                Total Income: BDT ${input.totalIncome}
                Total Expense: BDT ${input.totalExpense}
                Net Monthly Savings: BDT ${input.monthlySavings}
                Cash in Hand: BDT ${input.cashInHand}
                Has Loan: ${input.hasLoan} (Amount: BDT ${input.loanAmount}, Outstanding: BDT ${input.loanOutstanding}, EMI: BDT ${input.loanEmi})
                Has Running Savings: ${input.hasRunningSavings} (Category: ${input.savingsCategory}, Amount: BDT ${input.savingsAmount}, Rate: ${input.savingsInterestRate}%)
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemInstruction))
                    })
                })
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(25, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val respStr = response.body?.string() ?: ""
                val respJson = JSONObject(respStr)
                val candidates = respJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val cand = candidates.getJSONObject(0)
                    val content = cand.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        if (text.isNotBlank()) return@withContext text
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    generateRuleBasedAdvisorAnalysis(input)
}

private fun generateRuleBasedAdvisorAnalysis(input: PdfHelper.FinancialAdvisorInput): String {
    val fmt = NumberFormat.getNumberInstance(Locale.US)

    val salary = if (input.salary > 0) input.salary else 0.0
    val otherInc = if (input.otherIncome > 0) input.otherIncome else 0.0
    var inc = if (input.totalIncome > 0) input.totalIncome else (salary + otherInc)
    if (inc <= 0) inc = 30000.0

    val rent = if (input.rentExpense > 0) input.rentExpense else 0.0
    val food = if (input.foodExpense > 0) input.foodExpense else 0.0
    val elec = if (input.electricityExpense > 0) input.electricityExpense else 0.0
    val trans = if (input.transportExpense > 0) input.transportExpense else 0.0
    val loanEmi = if (input.hasLoan && input.loanEmi > 0) input.loanEmi else 0.0
    val otherExp = if (input.otherExpense > 0) input.otherExpense else 0.0

    val itemizedExp = rent + food + elec + trans + loanEmi + otherExp
    var exp = if (input.totalExpense > 0) input.totalExpense else itemizedExp
    if (exp <= 0) exp = inc * 0.80

    val actualSavings = (inc - exp).coerceAtLeast(0.0)

    // Standard 50/30/20 Rule Targets
    val targetNeeds = inc * 0.50  // 50% Needs
    val targetWants = inc * 0.30  // 30% Wants / Lifestyle
    val targetSavings = inc * 0.20 // 20% Savings Target

    // 1. Needs Sub-Allocations (MUST sum to targetNeeds EXACTLY)
    val rentAlloc = if (rent > 0) rent.coerceAtMost(targetNeeds * 0.70) else (targetNeeds * 0.55)
    val foodUtilAlloc = targetNeeds - rentAlloc // Exact balance to hit targetNeeds

    // 2. Wants / Lifestyle Sub-Allocations (MUST sum to targetWants EXACTLY)
    val travelLeisureMonthly = (inc * 0.05).coerceAtLeast(500.0).coerceAtMost(targetWants * 0.30)
    val personalWantsAlloc = targetWants - travelLeisureMonthly // Exact balance to hit targetWants

    // 3. Realistic Savings Starting Amount & Step-up
    val initialSavingsTarget = if (actualSavings > 1000.0) actualSavings else (inc * 0.10)
    val medicalReserveMonthly = (initialSavingsTarget * 0.15).coerceAtLeast(300.0)
    val startingDps = (initialSavingsTarget - medicalReserveMonthly).coerceAtLeast(1000.0)
    val stepUpAmount = ((targetSavings - initialSavingsTarget) / 3.0).coerceAtLeast(500.0)

    // Compound DPS Helper
    fun calcDpsMaturity(monthly: Double, months: Int, annualRate: Double = 0.105): Double {
        val r = annualRate / 12.0
        if (r <= 0) return monthly * months
        return monthly * (((1.0 + r).pow(months.toDouble()) - 1.0) / r) * (1.0 + r)
    }

    // Step 1: 3-Year DPS (Year 1-3)
    val dps1Maturity = calcDpsMaturity(startingDps, 36, 0.105)
    val dps1Deposit = startingDps * 36
    val dps1Profit = dps1Maturity - dps1Deposit

    // Step 2: 1-Year FDR (Year 4-6)
    val fdr1NetMonthlyYield = (dps1Maturity * 0.11 * 0.85) / 12.0
    val dps2Monthly = startingDps + stepUpAmount + fdr1NetMonthlyYield
    val dps2Maturity = calcDpsMaturity(dps2Monthly, 36, 0.105)
    val dps2Deposit = dps2Monthly * 36
    val dps2Profit = dps2Maturity - dps2Deposit

    val totalFund6Yr = dps1Maturity + dps2Maturity

    // Step 3: 7-Year Double Money Scheme (Year 7-13)
    val doubleMoneyScheme13Yr = totalFund6Yr * 2.0
    val dps3Monthly = startingDps + (stepUpAmount * 2.0)
    val dps3Maturity = calcDpsMaturity(dps3Monthly, 84, 0.105)
    val dps3Deposit = dps3Monthly * 84
    val totalFund13Yr = doubleMoneyScheme13Yr + dps3Maturity

    // Step 4: Long-term compounding (Year 14-25)
    val dps4Monthly = dps3Monthly + stepUpAmount
    val dps4Maturity = calcDpsMaturity(dps4Monthly, 84, 0.105)
    val totalFund20Yr = totalFund13Yr * 2.0 + dps4Maturity

    val dps5Monthly = dps4Monthly + stepUpAmount
    val dps5Maturity = calcDpsMaturity(dps5Monthly, 60, 0.105)
    val totalFund25Yr = totalFund20Yr * 1.6 + dps5Maturity

    val totalDeposit25Yr = dps1Deposit + dps2Deposit + dps3Deposit + (dps4Monthly * 84) + (dps5Monthly * 60)
    val totalProfit25Yr = totalFund25Yr - totalDeposit25Yr
    val monthlyPassiveIncome25Yr = (totalFund25Yr * 0.105 * 0.85) / 12.0

    // Traditional Unplanned Savings Comparison (25 Years)
    val tradDeposit25Yr = startingDps * 12 * 25
    val tradTotal25Yr = calcDpsMaturity(startingDps, 300, 0.03)
    val tradProfit25Yr = tradTotal25Yr - tradDeposit25Yr
    val tradMonthlyReturn25Yr = (tradTotal25Yr * 0.03 * 0.85) / 12.0

    val sb = StringBuilder()

    // Section 1
    sb.appendLine("১. গ্রাহকের প্রোফাইল ও গাণিতিক বাজেট বিশ্লেষণ (50/30/20 Exact Math Analysis)")
    sb.appendLine("• গ্রাহকের নাম: ${input.name.ifBlank { "সম্মানিত গ্রাহক" }} (বয়স: ${input.age} বছর, পেশা: ${input.employment})")
    sb.appendLine("• মোট মাসিক আয়: ৳ ${fmt.format(inc)} | মোট মাসিক খরচ: ৳ ${fmt.format(exp)}")
    sb.appendLine("• বর্তমান নিট সঞ্চয়: ৳ ${fmt.format(actualSavings)} (আয়ের ${"%.1f".format((actualSavings / inc) * 100)}%)")
    if (input.hasLoan && input.loanEmi > 0) {
        sb.appendLine("• ⚠️ লোন তথ্য: মাসিক ইএমআই ৳ ${fmt.format(input.loanEmi)} (বকেয়া ৳ ${fmt.format(input.loanOutstanding)})")
    }
    if (input.cashInHand > 0) {
        sb.appendLine("• 💵 হাতে নগদ অর্থ: ৳ ${fmt.format(input.cashInHand)}")
    }
    sb.appendLine()
    sb.appendLine("💡 ফাইনানশিয়াল এডভাইজরের বাজেট বিন্যাস (৫০/৩০/২০ রুল):")
    sb.appendLine("  ১) মৌলিক প্রয়োজন (৫০% = ৳ ${fmt.format(targetNeeds)}): বাসা ভাড়া ৳ ${fmt.format(rentAlloc)} + খাদ্য, বিদ্যুৎ, ট্রান্সপোর্ট ও লোন ৳ ${fmt.format(foodUtilAlloc)} (মোট ৳ ${fmt.format(targetNeeds)})")
    sb.appendLine("  ২) জীবনযাত্রা ও বিনোদন (৩০% = ৳ ${fmt.format(targetWants)}): ব্যক্তিগত ও পারিবারিক খরচ ৳ ${fmt.format(personalWantsAlloc)} + ভ্রমণ/ভ্যাকেশন সঞ্চয় ৳ ${fmt.format(travelLeisureMonthly)}/মাস (বছরে ৳ ${fmt.format(travelLeisureMonthly * 12)}) (মোট ৳ ${fmt.format(targetWants)})")
    sb.appendLine("  ৩) সঞ্চয় ও বিনিয়োগ (২০% = ৳ ${fmt.format(targetSavings)}): ইমার্জেন্সি রিভার্স ফান্ড ৳ ${fmt.format(medicalReserveMonthly)}/মাস + ডিপিএস ৳ ${fmt.format(startingDps)}/মাস")
    sb.appendLine("     * পরামর্শ: বর্তমানে আপনার সামর্থ্য অনুযায়ী ৳ ${fmt.format(startingDps)} টাকার ডিপিএস দিয়ে শুরু করুন। পরবর্তীতে প্রতি বছর বাজেট অপটিমাইজ করে ডিপিএস ৳ ${fmt.format(stepUpAmount)} করে বাড়িয়ে ২০% লক্ষ্যমাত্রায় পৌঁছান।")
    sb.appendLine()

    // Section 2
    sb.appendLine("২. বাজেট প্রবাহ ও গাণিতিক দৃশ্যমান ছক (Visual Budget Tree Structure)")
    sb.appendLine("মাসিক আয় থেকে কোটিপতি ফান্ডের নিখুঁত ফ্লো-চার্ট:")
    sb.appendLine("[মাসিক আয়: ৳${fmt.format(inc)}]")
    sb.appendLine("  ├── ৳${fmt.format(targetNeeds)} (৫০% মৌলিক প্রয়োজন)")
    sb.appendLine("  │     ├── ৳${fmt.format(rentAlloc)} ──> বাসা ভাড়া")
    sb.appendLine("  │     └── ৳${fmt.format(foodUtilAlloc)} ──> খাদ্য, ইউটিলিটি, ট্রান্সপোর্ট ও লোন")
    sb.appendLine("  ├── ৳${fmt.format(targetWants)} (৩০% জীবনযাত্রা, ভ্রমণ ও অন্যান্য)")
    sb.appendLine("  │     ├── ৳${fmt.format(travelLeisureMonthly)} ──> ভ্রমণ ও বিনোদন বাজেট (বছরে ৳${fmt.format(travelLeisureMonthly * 12)})")
    sb.appendLine("  │     └── ৳${fmt.format(personalWantsAlloc)} ──> ব্যক্তিগত ও পারিবারিক কেনাকাটা")
    sb.appendLine("  └── ৳${fmt.format(targetSavings)} (২০% সঞ্চয় ও কোটিপতি স্নোবল ফান্ড)")
    sb.appendLine("        ├── ৳${fmt.format(medicalReserveMonthly)} ──> ইমার্জেন্সি ও চিকিৎসা সুরক্ষা ফান্ড")
    sb.appendLine("        └── ৳${fmt.format(startingDps)} ──> ১ম ৩-বছর মেয়াদী ডিপিএস (DPS @ ১০.৫%)")
    sb.appendLine("              │")
    sb.appendLine("              ▼ [৩ বছর পর মেচুরিটি: ৳${fmt.format(dps1Maturity)}]")
    sb.appendLine("        ১-বছর মেয়াদী FDR (১১% সুদে)")
    sb.appendLine("              │")
    sb.appendLine("              ▼ [মাসিক নিট প্রফিট: ৳${fmt.format(fdr1NetMonthlyYield)} (AIT সোর্স ট্যাক্স বাদ)]")
    sb.appendLine("        ২য় বর্ধিত ডিপিএস (মাসিক ৳${fmt.format(dps2Monthly)})")
    sb.appendLine("              │")
    sb.appendLine("              ▼ [৬ বছর পর মোট পুঞ্জীভূত ফান্ড: ৳${fmt.format(totalFund6Yr)}]")
    sb.appendLine("        ডাবল মানি স্কিম (৭ বছরে দ্বিগুণ বৃদ্ধি)")
    sb.appendLine("              │")
    sb.appendLine("              ▼ [২৫ বছর পর কোটিপতি ফান্ড: ৳${fmt.format(totalFund25Yr)}]")
    sb.appendLine()

    // Section 3
    sb.appendLine("৩. বাস্তবসম্মত ধাপ-ভিত্তিক চক্রবৃদ্ধি ডিপিএস রোডম্যাপ (Step-by-Step Compounding Roadmap)")
    sb.appendLine("ধাপ ১ (বর্তমানে সামর্থ্য অনুযায়ী শুরু):")
    sb.appendLine("• আপনার বর্তমান সামর্থ্য অনুযায়ী প্রতি মাসে ৳ ${fmt.format(startingDps)} টাকা ১০.৫% সুদে ৩ বছর (৩৬ মাস) মেয়াদী ডিপিএস-এ রাখুন।")
    sb.appendLine("• ৩৬ মাসে আপনার মোট জমা আসল: ৳ ${fmt.format(dps1Deposit)} | অর্জিত মুনাফা: ৳ ${fmt.format(dps1Profit)}")
    sb.appendLine("• ৩ বছর পর মেচুরিটিতে পাবেন: ৳ ${fmt.format(dps1Maturity)}")
    sb.appendLine()
    sb.appendLine("ধাপ ২ (এফডিআর মুনাফা দিয়ে ২য় ডিপিএস রি-ইনভেস্টমেন্ট):")
    sb.appendLine("• মেচুরিটির ৳ ${fmt.format(dps1Maturity)} টাকা ১-বছর মেয়াদী এফডিআর (FDR)-এ ১১% সুদে জমা রাখুন।")
    sb.appendLine("• ১৫% AIT সোর্স ট্যাক্স বাদ দিয়ে প্রতি মাসে নিট লাভ আসবে ৳ ${fmt.format(fdr1NetMonthlyYield)} টাকা।")
    sb.appendLine("• মূল আসল FDR-এ অক্ষত রেখে, এই প্রফিট (৳ ${fmt.format(fdr1NetMonthlyYield)}) + আপনার নিয়মিত ডিপিএস বাজেট যোগ করে ২য় ডিপিএস শুরু করুন (মাসিক ৳ ${fmt.format(dps2Monthly)})।")
    sb.appendLine("• ৬ষ্ঠ বছর শেষে মোট অর্জিত ফান্ড দাঁড়াবে: ৳ ${fmt.format(totalFund6Yr)}")
    sb.appendLine()
    sb.appendLine("ধাপ ৩ (ডাবল মানি স্কিমে দ্বিগুণ প্রবৃদ্ধি):")
    sb.appendLine("• ৬ বছর পর অর্জিত ৳ ${fmt.format(totalFund6Yr)} টাকাকে ব্যাংকের ৭-বছর মেয়াদী ডাবল মানি স্কিমে জমা রাখুন।")
    sb.appendLine("• ৭ বছর পর (১৩তম বছরে) এটি দ্বিগুণ হয়ে দাঁড়াবে: ৳ ${fmt.format(doubleMoneyScheme13Yr)} (পাশাপাশি চলমান সঞ্চয়সহ মোট ৳ ${fmt.format(totalFund13Yr)})।")
    sb.appendLine()

    // Section 4
    sb.appendLine("৪. বছরভিত্তিক প্রবৃদ্ধি ও কোটিপতি রোডম্যাপ ছক (Milestone Wealth Table)")
    sb.appendLine("-----------------------------------------------------------------------------------------")
    sb.appendLine("| সময়কাল   | জমাকৃত মূলধন (আসল) | অর্জিত মোট মুনাফা  | পুঞ্জীভূত মোট সম্পদ   | প্রধান বিনিয়োগ মাধ্যম |")
    sb.appendLine("-----------------------------------------------------------------------------------------")
    sb.appendLine("| ৩ বছর পর  | ৳ ${fmt.format(dps1Deposit)} | ৳ ${fmt.format(dps1Profit)} | ৳ ${fmt.format(dps1Maturity)} | ১ম ডিপিএস মেচুরিটি |")
    sb.appendLine("| ৬ বছর পর  | ৳ ${fmt.format(dps1Deposit + dps2Deposit)} | ৳ ${fmt.format(totalFund6Yr - (dps1Deposit + dps2Deposit))} | ৳ ${fmt.format(totalFund6Yr)} | এফডিআর + ২য় ডিপিএস |")
    sb.appendLine("| ১৩ বছর পর | ৳ ${fmt.format(dps1Deposit + dps2Deposit + dps3Deposit)} | ৳ ${fmt.format(totalFund13Yr - (dps1Deposit + dps2Deposit + dps3Deposit))} | ৳ ${fmt.format(totalFund13Yr)} | ১ম ডাবল মানি স্কিম |")
    sb.appendLine("| ২০ বছর পর | ৳ ${fmt.format(dps1Deposit + dps2Deposit + dps3Deposit + dps4Monthly * 84)} | ৳ ${fmt.format(totalFund20Yr - (dps1Deposit + dps2Deposit + dps3Deposit + dps4Monthly * 84))} | ৳ ${fmt.format(totalFund20Yr)} | ২য় ডাবল মানি স্কিম |")
    sb.appendLine("| ২৫ বছর পর | ৳ ${fmt.format(totalDeposit25Yr)} | ৳ ${fmt.format(totalProfit25Yr)} | ৳ ${fmt.format(totalFund25Yr)} | 🎯 কোটিপতি ও ফ্রীডম! |")
    sb.appendLine("-----------------------------------------------------------------------------------------")
    sb.appendLine()

    // Section 5
    sb.appendLine("৫. জীবনযাত্রার নিরাপত্তা, চিকিৎসা, ভ্রমণ ও প্যাসিভ ইনকাম (Life Security, Travel & Monthly Passive Income)")
    sb.appendLine("• 🏥 চিকিৎসা ও ইমার্জেন্সি ফান্ড: প্রতি মাসে ৳ ${fmt.format(medicalReserveMonthly)} জমিয়ে ৩ বছরে ৳ ${fmt.format(medicalReserveMonthly * 36)} টাকার নিরাপত্তা সঞ্চয় গড়ে উঠবে।")
    sb.appendLine("• ✈️ বার্ষিক ভ্রমণ ও বিনোদন বাজেট: প্রতি মাসে ৳ ${fmt.format(travelLeisureMonthly)} হিসেবে প্রতি বছর ভ্রমণের জন্য ৳ ${fmt.format(travelLeisureMonthly * 12)} টাকা বরাদ্দ থাকবে।")
    sb.appendLine("• 🏆 আজীবন প্যাসিভ ইনকাম: ২৫ বছর পর ৳ ${fmt.format(totalFund25Yr)} টাকা এফডিআর-এ জমা রাখলে প্রতি মাসে নিট প্যাসিভ আয় আসবে ৳ ${fmt.format(monthlyPassiveIncome25Yr)} টাকা! কাজ না করেও আপনার পরিবার আজীবন আর্থিক নিরাপত্তায় থাকবে।")
    sb.appendLine()

    // Section 6: Comparison Table
    sb.appendLine("৬. বর্তমান সাধারণ পদ্ধতি বনাম AI এডভাইজর রোডম্যাপ তুলনা ছক (Comparison Table)")
    sb.appendLine("-------------------------------------------------------------------------------------------------------")
    sb.appendLine("| আর্থিক ব্যবস্থাপনা পদ্ধতি               | ২৫ বছরে জমা আসল   | ২৫ বছর পর মোট সম্পদ | ২৫ বছর পর মাসিক প্যাসিভ আয় |")
    sb.appendLine("-------------------------------------------------------------------------------------------------------")
    sb.appendLine("| ১. সাধারণ অনিয়মিত সঞ্চয় (Traditional)  | ৳ ${fmt.format(tradDeposit25Yr)}   | ৳ ${fmt.format(tradTotal25Yr)}     | ৳ ${fmt.format(tradMonthlyReturn25Yr)} /মাস |")
    sb.appendLine("| ২. AI এডভাইজর চক্রবৃদ্ধি রোডম্যাপ (AI Roadmap) | ৳ ${fmt.format(totalDeposit25Yr)}   | ৳ ${fmt.format(totalFund25Yr)} (কোটি!) | ৳ ${fmt.format(monthlyPassiveIncome25Yr)} /মাস (আজীবন!) |")
    sb.appendLine("-------------------------------------------------------------------------------------------------------")
    val timesMore = if (tradTotal25Yr > 0) totalFund25Yr / tradTotal25Yr else 10.0
    sb.appendLine("🔥 গাণিতিক পার্থক্য: AI এডভাইজর রোডম্যাপ অনুসরণ করলে সঠিক চক্রবৃদ্ধি ও এফডিআর রি-ইনভেস্টমেন্টের মাধ্যমে আপনার সম্পদ সাধারণ সঞ্চয়ের চেয়ে প্রায় ${"%.1f".format(timesMore)} গুণ বৃদ্ধি পাবে!")

    return sb.toString()
}
