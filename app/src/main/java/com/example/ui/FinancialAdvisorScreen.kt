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
                                        val isHeader = trimmed.startsWith("১.") || trimmed.startsWith("২.") || trimmed.startsWith("৩.") || trimmed.startsWith("৪.") ||
                                                trimmed.contains("গ্রাহকের সংক্ষিপ্ত") || trimmed.contains("আয় ও খরচের অনুপাত") || trimmed.contains("আর্থিক স্বাধীনতার") || trimmed.contains("সম্পূর্ণ ব্যাংকিং বিনিয়োগ")

                                        if (isHeader) {
                                            Text(
                                                text = trimmed,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = GoldPrimary,
                                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                            )
                                        } else if (trimmed.contains("ভালো (Good)") || trimmed.contains("✅")) {
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
                                        } else if (trimmed.startsWith("ধাপ") || trimmed.startsWith("Step")) {
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
                You are a senior Financial Advisor at a leading Bangladeshi bank.
                Provide a complete, professional, highly accurate financial and investment advisory report in BANGLA LANGUAGE.
                
                Follow this EXACT format and headings in Bangla:

                ১. গ্রাহকের সংক্ষিপ্ত আর্থিক বিবরণী
                - Name, Age, Profession
                - Total Income, Total Expense, Net Savings, Cash in Hand
                - Loan status (if any: details; if none: "কোনো ঋণ নেই")
                - Running Savings status (if any: DPS/FDR details; if none: "কোনো ডিপিএস/এফডিআর নেই")

                ২. আয় ও খরচের অনুপাত বিশ্লেষণ (৫০/৩০/২০ বাজেট নিয়ম)
                - Explain 50/30/20 Rule: 50% Needs, 30% Wants, 20% Savings
                - State exact amounts for 50%, 30%, 20% based on total income
                - State Income : Expense : Savings ratio
                - State Percent of budget used ((Total Expense / Total Income) * 100)
                - Analyze whether customer's needs and savings fit this range.
                - If YES: mark "ভালো (Good)"
                - If NO: give exact BDT guidance on how much to reduce expenses to fit 50% limit and reach 20% savings.

                ৩. আর্থিক স্বাধীনতার সময়সীমা (Financial Freedom / FIRE Calculator)
                - Annual Expenses (Total Expense * 12)
                - Target FIRE Fund (25 * Annual Expense)
                - Calculate estimated number of years to reach Financial Freedom assuming 10% annual compound growth.

                ৪. ধাপ অনুযায়ী সম্পূর্ণ ব্যাংকিং বিনিয়োগ পরিকল্পনা (Step-by-Step Wealth Plan)
                - Step 1: Emergency Reserve (3-6 months expense in liquid/short FDR)
                - Step 2: DPS Strategy (3-year DPS at 10%-11% interest)
                - Step 3: FDR Strategy (1-year / 3-month FDR at 8%-11%, explain 10%-15% AIT/tax deduction, explain monthly payout vs 1-year reinvestment)
                - Step 4: Double Money Scheme (6-7 years to double FDR)
                - Step 5: Loan Advice (Bank loans 10%-14% vs 25-yr home loan 9%)

                LANGUAGE: Professional, polite, clear BANGLA.
            """.trimIndent()

            val prompt = """
                Customer Data:
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
                Has Loan: ${input.hasLoan} (Amount: BDT ${input.loanAmount}, Outstanding: BDT ${input.loanOutstanding}, EMI: BDT ${input.loanEmi}, Tenure: ${input.loanFinishDate})
                Has Running Savings: ${input.hasRunningSavings} (Category: ${input.savingsCategory}, Amount: BDT ${input.savingsAmount}, Tenure: ${input.savingsTenure} yrs, Rate: ${input.savingsInterestRate}%, Type: ${input.fdrPayoutType}, Start: ${input.savingsStartDate}, Finish: ${input.savingsFinishDate})
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

    // Fallback Deterministic Rule Engine in BANGLA
    generateRuleBasedAdvisorAnalysis(input)
}

private fun generateRuleBasedAdvisorAnalysis(input: PdfHelper.FinancialAdvisorInput): String {
    val fmt = NumberFormat.getNumberInstance(Locale.US)

    val inc = if (input.totalIncome > 0) input.totalIncome else (input.salary + input.otherIncome)
    val exp = if (input.totalExpense > 0) input.totalExpense else input.monthlyExpenses
    val netSav = if (input.monthlySavings > 0) input.monthlySavings else (inc - exp).coerceAtLeast(0.0)

    // Budget ratios
    val needsLimit = inc * 0.50
    val wantsLimit = inc * 0.30
    val savingsLimit = inc * 0.20

    // Needs expenses (Rent + Food + Electricity + Transport + EMI)
    val needsExpense = input.rentExpense + input.foodExpense + input.electricityExpense + input.transportExpense + (if (input.hasLoan) input.loanEmi else 0.0)
    val wantsExpense = input.otherExpense

    val usedPct = if (inc > 0) (exp / inc) * 100.0 else 0.0
    val isBudgetGood = needsExpense <= needsLimit && netSav >= savingsLimit

    // Ratio expression
    val ratioExp = if (inc > 0) {
        val expUnits = ((exp / inc) * 10).toInt()
        val savUnits = ((netSav / inc) * 10).toInt()
        "১০ : $expUnits : $savUnits (আয় : খরচ : সঞ্চয়)"
    } else "৪ : ৩ : ১"

    // FIRE Calculation
    val annualExpense = exp * 12.0
    val targetFireFund = annualExpense * 25.0 // 25x rule
    val currentLiquid = input.cashInHand + (if (input.hasRunningSavings && input.savingsCategory == "FDR") input.savingsAmount else 0.0)

    // Calculate years to FIRE
    var yearsToFi = 0.0
    if (netSav > 0) {
        var accumulated = currentLiquid
        var month = 0
        val rMonthly = 0.10 / 12.0 // 10% annual interest
        while (accumulated < targetFireFund && month < 600) { // max 50 years
            accumulated = (accumulated * (1 + rMonthly)) + netSav
            month++
        }
        yearsToFi = month / 12.0
    }

    val sb = StringBuilder()

    // Section 1
    sb.appendLine("১. গ্রাহকের সংক্ষিপ্ত আর্থিক বিবরণী")
    sb.appendLine("• গ্রাহকের নাম: ${input.name.ifBlank { "সম্মানিত গ্রাহক" }} (বয়স: ${input.age} বছর, পেশা: ${input.employment})")
    sb.appendLine("• মোট মাসিক আয়: ৳ ${fmt.format(inc)} (বেতন: ৳ ${fmt.format(input.salary)} + অন্যান্য: ৳ ${fmt.format(input.otherIncome)})")
    sb.appendLine("• মোট মাসিক খরচ: ৳ ${fmt.format(exp)} (বাসা ভাড়া: ৳ ${fmt.format(input.rentExpense)}, খাবার: ৳ ${fmt.format(input.foodExpense)}, বিদ্যুৎ/বিল: ৳ ${fmt.format(input.electricityExpense)}, যাতায়াত: ৳ ${fmt.format(input.transportExpense)}, অন্যান্য: ৳ ${fmt.format(input.otherExpense)})")
    sb.appendLine("• অবশইষ্ট নিট সঞ্চয়: ৳ ${fmt.format(netSav)}")
    sb.appendLine("• হাতে নগদ টাকা: ৳ ${fmt.format(input.cashInHand)}")
    if (input.hasLoan) {
        sb.appendLine("• ঋণ স্থিতি: মূল ঋণ ৳ ${fmt.format(input.loanAmount)}, বর্তমান বকেয়া ৳ ${fmt.format(input.loanOutstanding)}, মাসিক কিস্তি ৳ ${fmt.format(input.loanEmi)}, মেয়াদের শেষ: ${input.loanFinishDate}")
    } else {
        sb.appendLine("• ঋণ স্থিতি: বর্তমানে কোনো ঋণ নেই (একটি ইতিবাচক আর্থিক দিক)")
    }
    if (input.hasRunningSavings) {
        sb.appendLine("• চলমান সঞ্চয়: ক্যাটাগরি: ${input.savingsCategory}, পরিমাণ: ৳ ${fmt.format(input.savingsAmount)}, মেয়াদ: ${input.savingsTenure} বছর, সুদের হার: ${input.savingsInterestRate}%, ধরন: ${input.fdrPayoutType}, মেয়াদ: ${input.savingsStartDate} থেকে ${input.savingsFinishDate}")
    } else {
        sb.appendLine("• চলমান সঞ্চয়: বর্তমানে কোনো ডিপিএস বা এফডিআর চালু নেই")
    }
    sb.appendLine()

    // Section 2
    sb.appendLine("২. আয় ও খরচের অনুপাত বিশ্লেষণ (৫০/৩০/২০ বাজেট নিয়ম)")
    sb.appendLine("• ৫০/৩০/২০ আদর্শ বাজেট স্ট্যান্ডার্ড:")
    sb.appendLine("  - ৫০% মৌলিক প্রয়োজনীয় খরচ (Needs Limit): ৳ ${fmt.format(needsLimit)}")
    sb.appendLine("  - ৩০% জীবনযাত্রা ও ইচ্ছা (Wants Limit): ৳ ${fmt.format(wantsLimit)}")
    sb.appendLine("  - ২০% বাধ্যতামূলক সঞ্চয় (Mandatory Savings Limit): ৳ ${fmt.format(savingsLimit)}")
    sb.appendLine("• বাজেটের অনুপাত (Ratio Expression): $ratioExp")
    sb.appendLine("• ব্যবহৃত বাজেটের হার (Percent of Budget Used): ${"%.1f".format(usedPct)}%")
    sb.appendLine()
    if (isBudgetGood) {
        sb.appendLine("• বাজেট মূল্যায়ন: ✅ ভালো (Good) - আপনার আয় ও খরচের অনুপাত আদর্শ বাজেটের সীমারেখার মধ্যে রয়েছে।")
    } else {
        sb.appendLine("• বাজেট মূল্যায়ন: ⚠️ সতর্কতা (Needs Optimization) - আপনার প্রয়োজনীয় খরচ অতিরিক্ত বা সঞ্চয়ের হার কম।")
        sb.appendLine("• সংশোধনের পরামর্শ:")
        sb.appendLine("  আপনার মোট আয় ৳ ${fmt.format(inc)} এর মধ্যে সর্বোচ্চ ৫০% বা ৳ ${fmt.format(needsLimit)} হলো মৌলিক খরচের সীমা। আপনার বর্তমান মৌলিক খরচ ৳ ${fmt.format(needsExpense)}। প্রয়োজনীয় খরচ কমিয়ে এই সীমার মধ্যে আনুন।")
        sb.appendLine("  ৩০% বা ৳ ${fmt.format(wantsLimit)} জীবনযাত্রার ব্যয়ের জন্য বরাদ্দ রাখুন এবং প্রতি মাসে ন্যূনতম ২০% অর্থাৎ ৳ ${fmt.format(savingsLimit)} সঞ্চয় নিশ্চিত করুন।")
    }
    sb.appendLine()

    // Section 3
    sb.appendLine("৩. আর্থিক স্বাধীনতার সময়সীমা (Financial Freedom / FIRE Calculator)")
    sb.appendLine("• আনুমানিক বার্ষিক খরচ: ৳ ${fmt.format(annualExpense)}")
    sb.appendLine("• আর্থিক স্বাধীনতার লক্ষ্যমাত্রা (Target FIRE Fund = ২৫ x বার্ষিক খরচ): ৳ ${fmt.format(targetFireFund)}")
    if (netSav > 0) {
        sb.appendLine("• বর্তমান সঞ্চয় গতি ও বার্ষিক ১০% ব্যাংক রিটার্ন চক্রবৃদ্ধি হিসাবে আপনার আর্থিক স্বাধীনতা অর্জনে সময় লাগবে:")
        sb.appendLine("  👉 আনুমানিক ${"%.1f".format(yearsToFi)} বছর (${yearsToFi.toInt()} বছর ${( (yearsToFi - yearsToFi.toInt()) * 12 ).toInt()} মাস)")
    } else {
        sb.appendLine("• বর্তমানে কোনো নিট সঞ্চয় না থাকায় আর্থিক স্বাধীনতার নির্দিষ্ট সময়সীমা নির্ধারণ করা সম্ভব নয়। দ্রুত খরচ কমিয়ে নিট সঞ্চয় বাড়ানোর পরামর্শ দেওয়া হচ্ছে।")
    }
    sb.appendLine()

    // Section 4
    sb.appendLine("৪. ধাপ অনুযায়ী সম্পূর্ণ ব্যাংকিং বিনিয়োগ পরিকল্পনা (Step-by-Step Wealth Plan)")
    sb.appendLine("ধাপ ১ (জরুরি তহবিল গঠন - Emergency Fund):")
    val emergencyFund = exp * 6.0
    sb.appendLine("• আপনার ৬ মাসের আনুমানিক খরচ ৳ ${fmt.format(emergencyFund)}। আপনার হাতে থাকা ৳ ${fmt.format(input.cashInHand)} এর মধ্যে ৳ ${fmt.format(emergencyFund.coerceAtMost(input.cashInHand))} একটি তরল সেভিংস অ্যাকাউন্ট বা ৩ মাস মেয়াদী এফডিআর-এ জরুরি ফান্ড হিসেবে সংরক্ষিত রাখুন।")
    sb.appendLine()

    sb.appendLine("ধাপ ২ (ডিপিএস সঞ্চয় শুরু - DPS Savings Strategy):")
    val dpsRec = (netSav * 0.80).coerceAtLeast(2000.0)
    val dpsMaturity3Yr = dpsRec * 36 * 1.16 // approx 10.5% compounded
    sb.appendLine("• আপনার বর্তমান নিট উদ্বৃত্ত সঞ্চয় ৳ ${fmt.format(netSav)} থেকে প্রতি মাসে ৳ ${fmt.format(dpsRec)} দিয়ে ৩ বছর মেয়াদী ডিপিএস (DPS) চালু করুন।")
    sb.appendLine("• বাংলাদেশে ৩ বছর মেয়াদী ডিপিএস-এ ব্যাংকগুলোতে বর্তমানে ১০% - ১১% সুদে আকর্ষণীয় রিটার্ন পাওয়া যায়।")
    sb.appendLine("• ৩ বছর পর মেচুরিটিতে আপনার আনুমানিক প্রাপ্তি দাঁড়াবে ৳ ${fmt.format(dpsMaturity3Yr)}।")
    sb.appendLine()

    sb.appendLine("ধাপ ৩ (এফডিআর ও রি-ইনভেস্টমেন্ট কৌশল - FDR & Compounding):")
    sb.appendLine("• ডিপিএস মেচুরিটির টাকা (৳ ${fmt.format(dpsMaturity3Yr)}) সাথে সাথে ১ বছর মেয়াদী এফডিআর (FDR)-এ বিনিয়োগ করুন।")
    sb.appendLine("• বাংলাদেশে ১ বছর মেয়াদী এফডিআর-এ সুদের হার ১১% - ১২% (যার ওপর ১০%-১৫% সোর্স ট্যাক্স বা AIT কাটা হয়)। ৩ মাস মেয়াদী এফডিআর-এ সুদের হার সাধারণত ৮% হয়ে থাকে।")
    sb.appendLine("• এফডিআর মুনাফার ২ টি পছন্দনীয় পদ্ধতি রয়েছে:")
    sb.appendLine("  ১) মাসিক প্রফিট স্কিম (Monthly Benefit): প্রতি মাসের প্রফিট সেভিংস অ্যাকাউন্টে যুক্ত হবে এবং সেটি দিয়ে আবার নতুন ডিপিএস খোলা যাবে।")
    sb.appendLine("  ২) বার্ষিক চক্রবৃদ্ধি (Annual Reinvestment): মেচুরিটির মুনাফা মূলধনের সাথে যুক্ত করে পুনঃবিনিয়োগ করলে দ্রুত চক্রবৃদ্ধি বৃদ্ধি ঘটে।")
    sb.appendLine()

    sb.appendLine("ধাপ ৪ (ডাবল মানি স্কিম - Double Money Scheme):")
    sb.appendLine("• ব্যাংকগুলোর ডাবল মানি স্কিমে এফডিআর মূলধন রাখলে আনুমানিক ৬ থেকে ৭ বছরে আপনার জমাকৃত টাকা দ্বিগুণ হয়ে যাবে।")
    sb.appendLine()

    sb.appendLine("ধাপ ৫ (ঋণ ব্যবস্থাপনা পরামর্শ - Loan Advisory):")
    if (input.hasLoan) {
        sb.appendLine("• ব্যাংক ঋণের সুদের হার সাধারণত ১০% থেকে ১৪% পর্যন্ত হয়ে থাকে। আপনার ঋণের বর্তমান বকেয়া ৳ ${fmt.format(input.loanOutstanding)} এবং মাসিক কিস্তি ৳ ${fmt.format(input.loanEmi)}।")
        sb.appendLine("• নতুন কোনো বিনিয়োগের পূর্বে অতিরিক্ত সুদের ঋণ দ্রুত পরিশোধ করা বুদ্ধিমানের কাজ হবে।")
        sb.appendLine("• উল্লেখ্য, ২৫ বছর মেয়াদী দীর্ঘমেয়াদী হোম লোন (Home Loan)-এ সুদের হার প্রায় ৯% এর কাছাকাছি সুবিধাজনক পাওয়া যায়।")
    } else {
        sb.appendLine("• আপনার বর্তমানে কোনো ঋণ নেই। এটি আপনার অত্যন্ত শক্তিশালী ব্যাংকিং সুবিধা। কোনো উচ্চ সুদের লোন (১০%-১৪%) না নিয়ে সম্পূর্ণ উদ্বৃত্ত অর্থ ডিপিএস ও এফডিআর চক্রবৃদ্ধিতে ব্যবহারের পরামর্শ দেওয়া হচ্ছে।")
    }

    return sb.toString()
}
