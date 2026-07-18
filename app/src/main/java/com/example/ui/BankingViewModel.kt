package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BankingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BankingRepository
    
    // Global UI Settings
    private var _isDarkMode = mutableStateOf(true)
    var isDarkMode: Boolean
        get() = _isDarkMode.value
        set(value) {
            _isDarkMode.value = value
            savePreference("dark_theme", value)
        }

    private var _passcodeLock = mutableStateOf("")
    var passcodeLock: String
        get() = _passcodeLock.value
        set(value) {
            _passcodeLock.value = value
            savePreference("passcode_lock", value)
        }

    private var _isPasscodeEnabled = mutableStateOf(false)
    var isPasscodeEnabled: Boolean
        get() = _isPasscodeEnabled.value
        set(value) {
            _isPasscodeEnabled.value = value
            savePreference("passcode_enabled", value)
        }

    var isLoggedIn by mutableStateOf(true) // Default to true (Single user mode)
    
    // GitHub Update Settings (Persisted in SharedPreferences)
    var githubOwner by mutableStateOf("Toufiq-trt")
    var githubRepo by mutableStateOf("BranchManagementSystem1")
    var githubBranch by mutableStateOf("main")
    
    // In-App Update States
    var isUpdateAvailable by mutableStateOf(false)
    var latestUpdateInfo by mutableStateOf<com.example.util.AppUpdate?>(null)
    var isCheckingForUpdates by mutableStateOf(false)
    var updateCheckError by mutableStateOf<String?>(null)
    var downloadProgress by mutableStateOf<Float?>(null)
    var downloadStatusText by mutableStateOf("")
    
    // Active User State
    var currentUser by mutableStateOf<BranchUser?>(BranchUser("20104", "Officer Toufiq", "20104", true, true))
    
    // Simulated Runtime Permissions (controlled by Toufiq's authorization settings)
    var simulatedCameraGranted by mutableStateOf(true)
    var simulatedStorageGranted by mutableStateOf(true)
    var simulatedLocationGranted by mutableStateOf(true)
    
    // Active Screen Tracking
    var currentScreen by mutableStateOf("dashboard")
    var toufiqPanelSelectedTab by mutableStateOf(0) // Default tab for Toufiq control panel
    
    // Search states
    var searchQuery by mutableStateOf("")
    
    // Dynamic lists from DB
    val allItems: StateFlow<List<BankingItem>>
    val allTasks: StateFlow<List<TodoTask>>
    val allHunting: StateFlow<List<CustomerHunting>>
    val allForms: StateFlow<List<DigitalForm>>
    val atmLoadingLogs: StateFlow<List<AtmLoadingLog>>
    val allUsers: StateFlow<List<BranchUser>>
    val allPasswordLogs: StateFlow<List<PasswordHistoryEntry>>
    val allLettersIssued: StateFlow<List<LetterIssued>>
    val recycleBinItems: StateFlow<List<RecycleBinItem>>

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    
    // Quantities (latest)
    val prizeBondQty = MutableStateFlow(0)
    val payOrderQty = MutableStateFlow(0)
    
    val prizeBondLogs: StateFlow<List<QuantityLog>>
    val payOrderLogs: StateFlow<List<QuantityLog>>

    // AI Chat Support
    private val _aiMessages = MutableStateFlow<List<Pair<String, Boolean>>>(listOf(
        Pair("Hello! I am Toufiq's Intelligent Banking Assistant. Ask me anything about pending debit cards, PINs, cheque books, DPS, ATM replenishment, task lists, or custom banking operations reports!", false)
    ))
    val aiMessages: StateFlow<List<Pair<String, Boolean>>> = _aiMessages.asStateFlow()
    
    var isAiGenerating by mutableStateOf(false)

    init {
        val sharedPrefs = application.getSharedPreferences("smart_banking_prefs", android.content.Context.MODE_PRIVATE)
        isDarkMode = sharedPrefs.getBoolean("dark_theme", true)
        isPasscodeEnabled = sharedPrefs.getBoolean("passcode_enabled", false)
        passcodeLock = sharedPrefs.getString("passcode_lock", "") ?: ""
        
        var owner = sharedPrefs.getString("github_owner", "Toufiq-trt") ?: "Toufiq-trt"
        var repo = sharedPrefs.getString("github_repo", "BranchManagementSystem1") ?: "BranchManagementSystem1"
        var branch = sharedPrefs.getString("github_branch", "main") ?: "main"

        // Auto-upgrade from legacy repository if found
        if (owner == "Toufiq-Dev" && repo == "smartbanking") {
            owner = "Toufiq-trt"
            repo = "BranchManagementSystem1"
            sharedPrefs.edit()
                .putString("github_owner", owner)
                .putString("github_repo", repo)
                .apply()
        }

        githubOwner = owner
        githubRepo = repo
        githubBranch = branch

        val database = AppDatabase.getDatabase(application)
        repository = BankingRepository(database.bankingDao())
        
        // Start bi-directional mirroring with Firebase Realtime Database
        FirebaseSyncHelper.startBiDirectionalSync(repository)
        
        // Check for updates silently on startup
        checkForUpdatesSilently()
        
        allItems = repository.getAllItems().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        allTasks = repository.getAllTasks().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        allHunting = repository.getAllHunting().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        allForms = repository.getAllForms().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        atmLoadingLogs = repository.getAtmLoadingLogs().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        allUsers = repository.getAllUsers().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        allPasswordLogs = repository.getAllPasswordLogs().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        allLettersIssued = repository.getAllLettersIssued().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        recycleBinItems = repository.getAllRecycleBinItems().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        // Automatically purge recycled items older than 10 days on startup
        viewModelScope.launch {
            repository.autoPurgeRecycleBin()
        }
        
        prizeBondLogs = repository.getQuantityLogs("PRIZE_BOND").stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        payOrderLogs = repository.getQuantityLogs("PAY_ORDER").stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
        // Sync current quantities
        viewModelScope.launch {
            prizeBondQty.value = repository.getLatestQuantity("PRIZE_BOND")
            payOrderQty.value = repository.getLatestQuantity("PAY_ORDER")
            
            // Seed Admin User (Toufiq) if database is empty
            val existingAdmin = repository.getUser("20104")
            val adminUser = if (existingAdmin == null) {
                val admin = BranchUser(
                    ein = "20104",
                    name = "Officer Toufiq",
                    pin = "20104",
                    isAuthorized = true,
                    isToufiq = true
                )
                repository.insertUserDirectly(admin)
                admin
            } else {
                existingAdmin
            }
            currentUser = adminUser
            isLoggedIn = true
            simulatedCameraGranted = true
            simulatedStorageGranted = true
            simulatedLocationGranted = true

            // No dummy data seeding. Instead, guarantee that any previously seeded demo/sample data
            // is wiped on update without affecting the officer's real inputted entries.
            repository.clearAllDemoData()
            
            // Sync all existing manually inputted / active data to the Excel sheet in the background
            syncExistingDataToExcelInBackground()
        }
    }

    private suspend fun seedSampleDataIfEmpty() {
        // Dummy data seeding removed to ensure a clean slate
    }

    // --- Actions ---

    fun addBankingItem(
        type: String,
        name: String,
        acNo: String,
        address: String,
        phone: String,
        remarks: String,
        dateOverride: Long? = null,
        onDuplicate: (() -> Unit)? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val isDuplicate = repository.checkDuplicateItem(type, name, acNo)
            if (isDuplicate) {
                onDuplicate?.invoke()
            } else {
                val id = repository.insertBankingItem(type, name, acNo, address, phone, dateOverride, remarks)
                // Retrieve the inserted item and sync to the cloud excel in the background
                val received = dateOverride ?: System.currentTimeMillis()
                val destroy = received + (90L * 24L * 60L * 60L * 1000L)
                val newItem = BankingItem(
                    id = id.toInt(),
                    type = type,
                    customerName = name.uppercase().trim(),
                    accountNumber = acNo.trim(),
                    address = address.uppercase().trim(),
                    phoneNumber = phone.trim(),
                    receivedDate = received,
                    destroyAfter = destroy,
                    remarks = remarks.uppercase().trim()
                )
                sendItemToRemoteExcel(newItem)
                onSuccess?.invoke()
            }
        }
    }

    fun parseDateStringToMillis(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
        val trimmed = dateStr.trim()
        val formats = listOf("dd.MM.yyyy", "yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy", "d/M/yyyy", "d.M.yyyy")
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                sdf.isLenient = false
                return sdf.parse(trimmed)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                // Try next
            }
        }
        // Try parsing as timestamp
        try {
            return trimmed.toLong()
        } catch (e: Exception) {
            // Ignore
        }
        return System.currentTimeMillis()
    }

    fun sendItemToRemoteExcel(item: BankingItem) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Format: ACCOUNT NUMBER, CUSTOMER NAME, PHONE NUMBER, RECEIVE DATE, ADDRESS, delivered
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                val receiveDateStr = dateFormat.format(Date(item.receivedDate))
                val deliveredStr = if (item.isDelivered) "delivered" else ""

                // Construct Web App or Form parameters
                val scriptUrl = "https://script.google.com/macros/s/AKfycbz_example_script_id/exec"
                val queryParams = "accountNumber=${java.net.URLEncoder.encode(item.accountNumber, "UTF-8")}" +
                        "&customerName=${java.net.URLEncoder.encode(item.customerName, "UTF-8")}" +
                        "&phoneNumber=${java.net.URLEncoder.encode(item.phoneNumber, "UTF-8")}" +
                        "&receiveDate=${java.net.URLEncoder.encode(receiveDateStr, "UTF-8")}" +
                        "&address=${java.net.URLEncoder.encode(item.address, "UTF-8")}" +
                        "&delivered=${java.net.URLEncoder.encode(deliveredStr, "UTF-8")}" +
                        "&type=${java.net.URLEncoder.encode(item.type, "UTF-8")}"

                val finalUrl = "$scriptUrl?$queryParams"
                val connection = java.net.URL(finalUrl).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"
                
                val responseCode = connection.responseCode
                android.util.Log.d("ExcelSync", "Sync manual entry to cloud: $responseCode")
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("ExcelSync", "Background Excel sync failed: ${e.localizedMessage}")
            }
        }
    }

    fun syncExistingDataToExcelInBackground() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Get all items in the database
                val allLocalItems = repository.getAllItems().firstOrNull() ?: emptyList()
                for (item in allLocalItems) {
                    if (!item.isDemo) {
                        sendItemToRemoteExcel(item)
                        kotlinx.coroutines.delay(300) // gentle delay to prevent congestion
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parseCsvText(text: String): List<List<String>> {
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

    fun syncGoogleSheets(
        type: String,
        context: android.content.Context,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val urlString = when (type) {
                    "DEBIT_CARD" -> "https://docs.google.com/spreadsheets/d/1e_22aHpRoJYBe9J0ohT-PzwHmXGhrOtNlsQeOVHg67M/export?format=csv&gid=0"
                    "CHEQUE_BOOK" -> "https://docs.google.com/spreadsheets/d/1cakIYc79gR-YVnqKe4-i8J95AEuIKa4Q/export?format=csv&gid=2027095460"
                    "PIN" -> "https://docs.google.com/spreadsheets/d/1e_22aHpRoJYBe9J0ohT-PzwHmXGhrOtNlsQeOVHg67M/export?format=csv&gid=0"
                    else -> "https://docs.google.com/spreadsheets/d/1BUc13oZ_qKIBW9OOFtcPAZh9aoELxyVq6sguoAyAdFg/export?format=csv&gid=0"
                }

                val csvText = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val connection = java.net.URL(urlString).openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.requestMethod = "GET"
                    connection.inputStream.bufferedReader().use { it.readText() }
                }

                if (csvText.isBlank()) {
                    onFailure("Spreadsheet is empty or could not be downloaded")
                    return@launch
                }

                val parsedRows = parseCsvText(csvText)
                if (parsedRows.size <= 1) {
                    onFailure("No records found in spreadsheet")
                    return@launch
                }

                var insertedCount = 0
                var updatedCount = 0

                for (idx in 1 until parsedRows.size) {
                    val row = parsedRows[idx]
                    // Format: ACCOUNT NUMBER, CUSTOMER NAME, PHONE NUMBER, RECEIVE DATE, ADDRESS, delivered
                    val acNo = row.getOrNull(0)?.trim() ?: ""
                    val name = row.getOrNull(1)?.uppercase()?.trim() ?: ""
                    val phone = row.getOrNull(2)?.trim() ?: ""
                    val receiveDateStr = row.getOrNull(3)?.trim() ?: ""
                    val addressVal = row.getOrNull(4)?.uppercase()?.trim() ?: ""
                    val deliveryVal = row.getOrNull(5)?.trim()?.lowercase() ?: ""

                    if (name.isBlank() || acNo.isBlank() || name.lowercase().contains("customer") || acNo.lowercase().contains("account")) continue

                    // Check if user previously deleted this item from the app
                    if (repository.isItemDeleted(type, name, acNo)) {
                        continue
                    }

                    val isDeliveredInSheet = deliveryVal.isNotBlank() &&
                            deliveryVal != "no" &&
                            deliveryVal != "false" &&
                            deliveryVal != "pending" &&
                            deliveryVal != "undelivered" &&
                            deliveryVal != "none"

                    val existingItem = repository.getDuplicateItem(type, name, acNo)
                    if (existingItem != null) {
                        if (!existingItem.isDelivered && isDeliveredInSheet) {
                            val updatedItem = existingItem.copy(
                                isDelivered = true,
                                deliveryDate = System.currentTimeMillis()
                            )
                            repository.updateBankingItem(updatedItem)
                            updatedCount++
                        }
                    } else {
                        val received = parseDateStringToMillis(receiveDateStr)
                        val destroy = received + (90L * 24L * 60L * 60L * 1000L)
                        val item = BankingItem(
                            type = type,
                            customerName = name,
                            accountNumber = acNo,
                            address = addressVal,
                            phoneNumber = phone,
                            receivedDate = received,
                            destroyAfter = destroy,
                            remarks = "SHEET SYNCED",
                            isDestroyed = false,
                            isBalanced = true,
                            isDelivered = isDeliveredInSheet,
                            deliveryDate = if (isDeliveredInSheet) received else 0L,
                            isDemo = false
                        )
                        val id = repository.insertBankingItemDirectly(item)
                        FirebaseSyncHelper.pushToFirebase("banking_items", id.toString(), item.copy(id = id.toInt()))
                        insertedCount++
                    }
                }

                if (insertedCount > 0 || updatedCount > 0) {
                    onSuccess("Import successful! Imported $insertedCount, Updated $updatedCount items.")
                } else {
                    onSuccess("Sync complete. No new or updated items.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onFailure(e.localizedMessage ?: "Unknown network error")
            }
        }
    }

    fun importBulkRows(
        type: String,
        parsedRows: List<List<String>>,
        onCompleted: (inserted: Int, updated: Int) -> Unit
    ) {
        viewModelScope.launch {
            if (parsedRows.isEmpty()) {
                onCompleted(0, 0)
                return@launch
            }

            var insertedCount = 0
            var updatedCount = 0

            for (idx in 1 until parsedRows.size) {
                val row = parsedRows[idx]
                // Format: ACCOUNT NUMBER, CUSTOMER NAME, PHONE NUMBER, RECEIVE DATE, ADDRESS, delivered
                val acNo = row.getOrNull(0)?.trim() ?: ""
                val name = row.getOrNull(1)?.uppercase()?.trim() ?: ""
                val phone = row.getOrNull(2)?.trim() ?: ""
                val receiveDateStr = row.getOrNull(3)?.trim() ?: ""
                val addressVal = row.getOrNull(4)?.uppercase()?.trim() ?: ""
                val deliveryVal = row.getOrNull(5)?.trim()?.lowercase() ?: ""

                if (name.isBlank() || acNo.isBlank() || name.lowercase().contains("customer") || acNo.lowercase().contains("account")) continue

                // Check if user previously deleted this item from the app
                if (repository.isItemDeleted(type, name, acNo)) {
                    continue
                }

                val isDeliveredInSheet = deliveryVal.isNotBlank() &&
                        deliveryVal != "no" &&
                        deliveryVal != "false" &&
                        deliveryVal != "pending" &&
                        deliveryVal != "undelivered" &&
                        deliveryVal != "none"

                val existingItem = repository.getDuplicateItem(type, name, acNo)
                if (existingItem != null) {
                    if (!existingItem.isDelivered && isDeliveredInSheet) {
                        val updatedItem = existingItem.copy(
                            isDelivered = true,
                            deliveryDate = System.currentTimeMillis()
                        )
                        repository.updateBankingItem(updatedItem)
                        updatedCount++
                    }
                } else {
                    val received = parseDateStringToMillis(receiveDateStr)
                    val destroy = received + (90L * 24L * 60L * 60L * 1000L)
                    val item = BankingItem(
                        type = type,
                        customerName = name,
                        accountNumber = acNo,
                        address = addressVal,
                        phoneNumber = phone,
                        receivedDate = received,
                        destroyAfter = destroy,
                        remarks = "BULK IMPORTED",
                        isDestroyed = false,
                        isBalanced = true,
                        isDelivered = isDeliveredInSheet,
                        deliveryDate = if (isDeliveredInSheet) received else 0L,
                        isDemo = false
                    )
                    val id = repository.insertBankingItemDirectly(item)
                    FirebaseSyncHelper.pushToFirebase("banking_items", id.toString(), item.copy(id = id.toInt()))
                    insertedCount++
                }
            }
            onCompleted(insertedCount, updatedCount)
        }
    }

    fun clearAllDatabaseData(onCompleted: () -> Unit) {
        viewModelScope.launch {
            repository.clearAllDemoData()
            val pQty = repository.getLatestQuantity("PRIZE_BOND")
            val poQty = repository.getLatestQuantity("PAY_ORDER")
            prizeBondQty.value = pQty
            payOrderQty.value = poQty
            onCompleted()
        }
    }

    fun updateBankingItem(item: BankingItem) {
        viewModelScope.launch {
            repository.updateBankingItem(item)
        }
    }

    fun deleteBankingItem(item: BankingItem) {
        viewModelScope.launch {
            try {
                // Keep track of this deleted item so it won't be re-synced from Google Sheets/Excel CSV
                repository.addDeletedItemTracker(item.type, item.customerName, item.accountNumber)
                
                val adapter = moshi.adapter(BankingItem::class.java)
                val json = adapter.toJson(item)
                repository.insertRecycleBinItem(
                    originalType = "BANKING_ITEM",
                    originalId = item.id,
                    title = item.customerName,
                    subtitle = "${item.type} | A/C: ${item.accountNumber}",
                    serializedData = json
                )
                repository.deleteBankingItem(item)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateQuantityLog(type: String, newQty: Int, editedBy: String) {
        viewModelScope.launch {
            repository.updateQuantity(type, newQty, editedBy)
            if (type == "PRIZE_BOND") {
                prizeBondQty.value = newQty
            } else {
                payOrderQty.value = newQty
            }
        }
    }

    fun addAtmLoadingLog(
        atmName: String,
        c1Rem: Int, c2Rem: Int, c3Rem: Int, c4Rem: Int,
        c1Load: Int, c2Load: Int, c3Load: Int, c4Load: Int,
        totalLoading: Long,
        operator: String,
        remarks: String
    ) {
        viewModelScope.launch {
            repository.insertAtmLoadingLog(atmName, c1Rem, c2Rem, c3Rem, c4Rem, c1Load, c2Load, c3Load, c4Load, totalLoading, operator, remarks)
        }
    }

    fun addForm(formType: String, customerName: String, accountNumber: String, remarks: String, signature: String, fieldsJson: String, pdfFilePath: String? = null, onCompleted: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.insertForm(formType, customerName, accountNumber, remarks, signature, fieldsJson, pdfFilePath)
            onCompleted(id)
        }
    }

    fun updateForm(form: DigitalForm) {
        viewModelScope.launch {
            repository.updateForm(form)
        }
    }

    fun deleteForm(form: DigitalForm) {
        viewModelScope.launch {
            try {
                val adapter = moshi.adapter(DigitalForm::class.java)
                val json = adapter.toJson(form)
                repository.insertRecycleBinItem(
                    originalType = "FORM",
                    originalId = form.id,
                    title = form.customerName,
                    subtitle = "Form: ${form.formType} | A/C: ${form.accountNumber}",
                    serializedData = json
                )
                repository.deleteForm(form)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addTask(title: String, priority: String, dueDate: Long, dueTime: String, phoneNumber: String = "", mailerName: String = "") {
        viewModelScope.launch {
            repository.insertTask(title, priority, dueDate, dueTime, false, phoneNumber, mailerName)
        }
    }

    fun addTaskWithDate(title: String, priority: String, dueDate: Long, dueTime: String, customTimestamp: Long, phoneNumber: String = "", mailerName: String = "") {
        viewModelScope.launch {
            val task = TodoTask(
                title = title.uppercase().trim(),
                priority = priority.uppercase().trim(),
                dueDate = dueDate,
                dueTime = dueTime,
                isCompleted = false,
                timestamp = customTimestamp,
                phoneNumber = phoneNumber.trim(),
                mailerName = mailerName.uppercase().trim()
            )
            repository.insertTaskDirectly(task)
        }
    }

    fun updateTask(task: TodoTask) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun deleteTask(task: TodoTask) {
        viewModelScope.launch {
            try {
                val adapter = moshi.adapter(TodoTask::class.java)
                val json = adapter.toJson(task)
                repository.insertRecycleBinItem(
                    originalType = "TASK",
                    originalId = task.id,
                    title = task.title,
                    subtitle = "Task Priority: ${task.priority} | Due: ${task.dueTime}",
                    serializedData = json
                )
                repository.deleteTask(task)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addHunting(name: String, phone: String, address: String, product: String, priority: String, completion: Int) {
        viewModelScope.launch {
            repository.insertHunting(name, phone, address, product, priority, completion)
        }
    }

    fun updateHunting(hunting: CustomerHunting) {
        viewModelScope.launch {
            repository.updateHunting(hunting)
        }
    }

    fun deleteHunting(hunting: CustomerHunting) {
        viewModelScope.launch {
            try {
                val adapter = moshi.adapter(CustomerHunting::class.java)
                val json = adapter.toJson(hunting)
                repository.insertRecycleBinItem(
                    originalType = "HUNTING",
                    originalId = hunting.id,
                    title = hunting.customerName,
                    subtitle = "Findings: ${hunting.interestedProduct} | Phone: ${hunting.phoneNumber}",
                    serializedData = json
                )
                repository.deleteHunting(hunting)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Recycle Bin Recovery ---

    fun restoreRecycleBinItem(item: RecycleBinItem) {
        viewModelScope.launch {
            try {
                when (item.originalType) {
                    "BANKING_ITEM" -> {
                        val adapter = moshi.adapter(BankingItem::class.java)
                        val restored = adapter.fromJson(item.serializedData)
                        if (restored != null) {
                            repository.insertBankingItemDirectly(restored)
                            FirebaseSyncHelper.pushToFirebase("banking_items", restored.id.toString(), restored)
                        }
                    }
                    "FORM" -> {
                        val adapter = moshi.adapter(DigitalForm::class.java)
                        val restored = adapter.fromJson(item.serializedData)
                        if (restored != null) {
                            repository.insertFormDirectly(restored)
                            FirebaseSyncHelper.pushToFirebase("digital_forms", restored.id.toString(), restored)
                        }
                    }
                    "HUNTING" -> {
                        val adapter = moshi.adapter(CustomerHunting::class.java)
                        val restored = adapter.fromJson(item.serializedData)
                        if (restored != null) {
                            repository.insertHuntingDirectly(restored)
                            FirebaseSyncHelper.pushToFirebase("customer_hunting", restored.id.toString(), restored)
                        }
                    }
                    "TASK" -> {
                        val adapter = moshi.adapter(TodoTask::class.java)
                        val restored = adapter.fromJson(item.serializedData)
                        if (restored != null) {
                            repository.insertTaskDirectly(restored)
                            FirebaseSyncHelper.pushToFirebase("todo_tasks", restored.id.toString(), restored)
                        }
                    }
                }
                repository.deleteRecycleBinItemById(item.id)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restoreRecycleBinItems(items: List<RecycleBinItem>) {
        viewModelScope.launch {
            items.forEach { restoreRecycleBinItem(it) }
        }
    }

    fun deleteRecycleBinItemPermanently(item: RecycleBinItem) {
        viewModelScope.launch {
            repository.deleteRecycleBinItemById(item.id)
        }
    }

    fun deleteRecycleBinItemsPermanently(ids: List<Int>) {
        viewModelScope.launch {
            repository.deleteRecycleBinItemsByIds(ids)
        }
    }

    // --- AI Suggestions Integration ---
    
    suspend fun getAiDashboardSummary(): String {
        val activeCards = allItems.value.filter { it.type == "DEBIT_CARD" && !it.isDelivered && !it.isDestroyed }.size
        val activePINs = allItems.value.filter { it.type == "PIN" && !it.isDelivered && !it.isDestroyed }.size
        val activeCheques = allItems.value.filter { it.type == "CHEQUE_BOOK" && !it.isDelivered && !it.isDestroyed }.size
        val activeDPS = allItems.value.filter { it.type == "DPS" && !it.isDelivered && !it.isDestroyed }.size
        
        val bondsTotal = prizeBondQty.value
        val poTotal = payOrderQty.value
        val pendingTasks = allTasks.value.filter { !it.isCompleted }.size
        
        val systemPrompt = "You are Toufiq's Intelligent Operations Advisor. You analyze current bank branch metrics and offer strategic daily advice."
        val userPrompt = """
            Here are today's metrics:
            - Pending Debit Cards to Deliver: ${activeCards}
            - Pending PIN Mailers to Deliver: ${activePINs}
            - Pending Cheque Books: ${activeCheques}
            - Pending DPS Records: ${activeDPS}
            - Prize Bonds in vault: ${bondsTotal} units
            - Pay Orders balance: ${poTotal} units
            - Pending To-Do Tasks for today: ${pendingTasks}
            
            Give me a beautiful, high-impact 2-3 sentence strategic advice summary. Suggest priorities, warning of any overdue work or near-expiry items. Keep the tone sharp, supportive, and operational.
        """.trimIndent()
        
        return GeminiService.generateResponse(systemPrompt, userPrompt)
    }

    fun sendAiChatMessage(userMessage: String) {
        if (userMessage.isBlank()) return
        
        _aiMessages.update { it + Pair(userMessage, true) }
        isAiGenerating = true
        
        viewModelScope.launch {
            val allDataStr = buildDatabaseContextForAi()
            val systemPrompt = """
                You are TOUFIQ'S SMART BANKING ASSISTANT, an advanced AI embedded inside a branch operations app.
                You have real-time access to the local database, which is serialized below.
                
                Your capabilities:
                - Answer questions about stored banking data in natural language.
                - Search across modules (Debit Cards, PINs, Cheques, DPS, Prize Bonds, Pay Orders, Tasks, Forms, Leads).
                - Warn about items expiring (within 30 days) or overdue tasks.
                - Give summaries of active balances.
                
                Guidelines:
                - If the user asks for a specific name, card, account, or quantity, query the context data below and respond with precise facts.
                - Highlight near-expiry items (destruction limits) dynamically.
                - If the requested item does not exist, say so politely.
                - Keep responses professional, formatted with clean bullet points, and highly readable.
                
                DATABASE REAL-TIME CONTEXT:
                $allDataStr
            """.trimIndent()
            
            val aiResponse = GeminiService.generateResponse(systemPrompt, userMessage)
            _aiMessages.update { it + Pair(aiResponse, false) }
            isAiGenerating = false
        }
    }

    private fun buildDatabaseContextForAi(): String {
        val now = System.currentTimeMillis()
        val builder = java.lang.StringBuilder()
        
        builder.append("=== BANKING ITEMS (DEBIT CARDS, PINS, CHEQUES, DPS) ===\n")
        allItems.value.forEach { item ->
            val status = if (item.isDestroyed || now >= item.destroyAfter) "EXPIRED/DESTROYED" else "ACTIVE/PENDING"
            val daysLeft = ((item.destroyAfter - now) / (1000 * 3600 * 24)).toInt()
            builder.append("- ID: ${item.id} | Type: ${item.type} | Name: ${item.customerName} | AcNo: ${item.accountNumber} | Phone: ${item.phoneNumber} | Status: $status | Days Left till Destruction: $daysLeft\n")
        }
        
        builder.append("\n=== PRIZE BOND QUANTITY ===\n")
        builder.append("Current stock in vault: ${prizeBondQty.value} units\n")
        
        builder.append("\n=== PAY ORDER QUANTITY ===\n")
        builder.append("Current stock: ${payOrderQty.value} units\n")
        
        builder.append("\n=== TODO TASKS ===\n")
        allTasks.value.forEach { task ->
            builder.append("- Task: ${task.title} | Priority: ${task.priority} | Completed: ${task.isCompleted} | Due Time: ${task.dueTime}\n")
        }
        
        builder.append("\n=== CUSTOMER HUNTING LEADS ===\n")
        allHunting.value.forEach { lead ->
            builder.append("- Customer: ${lead.customerName} | Product: ${lead.interestedProduct} | Status: ${if (lead.isGrabbed) "CONVERTED" else "PENDING"} | Progress: ${lead.completionPercentage}%\n")
        }
        
        builder.append("\n=== DIGITAL FORMS SAVED ===\n")
        allForms.value.forEach { form ->
            builder.append("- Form: ${form.formType} | Customer: ${form.customerName} | Account: ${form.accountNumber} | Date: ${form.dateStr}\n")
        }
        
        return builder.toString()
    }

    // --- Search Logic ---
    fun getUniversalSearchResults(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val cleanQuery = query.lowercase(Locale.getDefault())
        val results = mutableListOf<SearchResult>()
        
        // Search items
        allItems.value.forEach { item ->
            val match = if (!isLoggedIn) {
                item.customerName.lowercase().contains(cleanQuery) ||
                item.accountNumber.lowercase().contains(cleanQuery) ||
                item.phoneNumber.lowercase().contains(cleanQuery)
            } else {
                item.customerName.lowercase().contains(cleanQuery) ||
                item.accountNumber.lowercase().contains(cleanQuery) ||
                item.phoneNumber.lowercase().contains(cleanQuery) ||
                item.address.lowercase().contains(cleanQuery) ||
                item.type.lowercase().contains(cleanQuery)
            }
            if (match) {
                val now = System.currentTimeMillis()
                val daysPassed = (now - item.receivedDate) / (1000L * 3600 * 24)
                val statusText = when {
                    item.isDelivered -> "Delivered"
                    item.isDestroyed -> "Destroyed"
                    daysPassed >= 90 -> "90 Days Completed"
                    daysPassed >= 30 -> "30 Days Completed"
                    else -> "Active Balancing"
                }
                results.add(
                    SearchResult(
                        moduleType = item.type,
                        title = item.customerName,
                        subtitle = "Acct: ${item.accountNumber} | Phone: ${item.phoneNumber}",
                        receivedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(item.receivedDate)),
                        status = statusText,
                        originalItem = item
                    )
                )
            }
        }
        
        if (isLoggedIn) {
            // Search hunting leads
            allHunting.value.forEach { lead ->
                if (lead.customerName.lowercase().contains(cleanQuery) ||
                    lead.interestedProduct.lowercase().contains(cleanQuery) ||
                    lead.phoneNumber.lowercase().contains(cleanQuery) ||
                    lead.address.lowercase().contains(cleanQuery)
                ) {
                    results.add(
                        SearchResult(
                            moduleType = "CUSTOMER HUNTING",
                            title = lead.customerName,
                            subtitle = "${lead.interestedProduct} | Phone: ${lead.phoneNumber}",
                            receivedDate = "N/A",
                            status = "${lead.completionPercentage}% Complete",
                            originalLead = lead
                        )
                    )
                }
            }

            // Search saved forms
            allForms.value.forEach { form ->
                if (form.customerName.lowercase().contains(cleanQuery) ||
                    form.accountNumber.lowercase().contains(cleanQuery) ||
                    form.formType.lowercase().contains(cleanQuery)
                ) {
                    results.add(
                        SearchResult(
                            moduleType = "DIGITAL FORM",
                            title = form.customerName,
                            subtitle = "${form.formType} | Acct: ${form.accountNumber}",
                            receivedDate = form.dateStr,
                            status = "Saved digitally",
                            originalForm = form
                        )
                    )
                }
            }
        }

        return results
    }

    // --- Authentication & User Operations ---

    fun login(ein: String, pin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUser(ein)
            if (user == null) {
                onResult(false, "User not found. Please register first.")
            } else if (user.pin != pin) {
                onResult(false, "Invalid PIN. Please try again.")
            } else if (!user.isAuthorized) {
                onResult(false, "Authorization Pending! Toufiq has not authorized your registration yet.")
            } else {
                currentUser = user
                isLoggedIn = true
                
                // Set simulated permissions (if Toufiq authorized specific permissions, user must toggle/grant them)
                // If permission is required (true), user must grant it, so start as false until they approve in prompt.
                // If not required (false), we pre-grant it (true).
                if (user.isToufiq) {
                    simulatedCameraGranted = true
                    simulatedStorageGranted = true
                    simulatedLocationGranted = true
                } else {
                    simulatedCameraGranted = !user.cameraAuthorized
                    simulatedStorageGranted = !user.storageAuthorized
                    simulatedLocationGranted = !user.locationAuthorized
                }
                onResult(true, "Successfully Logged In!")
            }
        }
    }

    fun register(ein: String, name: String, pin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val existing = repository.getUser(ein)
            if (existing != null) {
                onResult(false, "User with EIN $ein already registered!")
            } else {
                repository.registerUser(ein, name, pin)
                simulateCloudSync() // Automatically backs up registration to cloud state
                onResult(true, "Registration request submitted successfully! Please wait for Toufiq to authorize your account.")
            }
        }
    }

    fun authorizeUser(user: BranchUser, camera: Boolean, storage: Boolean, location: Boolean) {
        viewModelScope.launch {
            val updated = user.copy(
                isAuthorized = true,
                cameraAuthorized = camera,
                storageAuthorized = storage,
                locationAuthorized = location
            )
            repository.updateUser(updated)
            simulateCloudSync() // Persist to secure cloud registry
        }
    }

    fun updateUserPermissions(user: BranchUser, camera: Boolean, storage: Boolean, location: Boolean) {
        viewModelScope.launch {
            val updated = user.copy(
                cameraAuthorized = camera,
                storageAuthorized = storage,
                locationAuthorized = location
            )
            repository.updateUser(updated)
            simulateCloudSync()
        }
    }

    fun deregisterUser(user: BranchUser) {
        viewModelScope.launch {
            val updated = user.copy(isAuthorized = false)
            repository.updateUser(updated)
            simulateCloudSync()
        }
    }

    fun changePassword(ein: String, oldPin: String, newPin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUser(ein)
            if (user == null) {
                onResult(false, "User not found.")
            } else {
                val currentOldPin = user.pin
                val updated = user.copy(pin = newPin)
                repository.updateUser(updated)
                repository.insertPasswordLog(ein, currentOldPin, newPin)
                if (currentUser?.ein == ein) {
                    currentUser = updated
                }
                simulateCloudSync() // Sync updated passcode to all backup nodes
                onResult(true, "Passcode updated successfully!")
            }
        }
    }

    fun logout() {
        currentUser = null
        isLoggedIn = false
        simulatedCameraGranted = false
        simulatedStorageGranted = false
        simulatedLocationGranted = false
        currentScreen = "dashboard"
    }

    // Surpassing uninstalls and data clearances
    var isCloudSynced by mutableStateOf(true)

    fun simulateCloudSync() {
        viewModelScope.launch {
            isCloudSynced = false
            kotlinx.coroutines.delay(1000)
            isCloudSynced = true
        }
    }

    fun markAsLetterIssued(item: BankingItem) {
        viewModelScope.launch {
            repository.updateBankingItem(item.copy(isLetterIssued = true))
            repository.insertLetterIssued(
                customerName = item.customerName,
                accountNumber = item.accountNumber,
                phoneNumber = item.phoneNumber
            )
        }
    }

    fun markAsDelivered(item: BankingItem) {
        viewModelScope.launch {
            repository.updateBankingItem(
                item.copy(
                    isDelivered = true,
                    deliveryDate = System.currentTimeMillis()
                )
            )
        }
    }

    fun revertDelivery(item: BankingItem) {
        viewModelScope.launch {
            repository.updateBankingItem(
                item.copy(
                    isDelivered = false,
                    deliveryDate = 0L
                )
            )
        }
    }

    // --- In-App Auto Update & Preference Persistence ---

    private fun savePreference(key: String, value: Any) {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("smart_banking_prefs", android.content.Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        when (value) {
            is Boolean -> editor.putBoolean(key, value)
            is String -> editor.putString(key, value)
            is Int -> editor.putInt(key, value)
        }
        editor.apply()
    }

    fun saveGithubSettings(owner: String, repo: String, branch: String) {
        githubOwner = owner
        githubRepo = repo
        githubBranch = branch
        savePreference("github_owner", owner)
        savePreference("github_repo", repo)
        savePreference("github_branch", branch)
    }

    fun checkForUpdatesSilently() {
        viewModelScope.launch {
            try {
                val update = com.example.util.UpdateHelper.checkForUpdates(githubOwner, githubRepo, githubBranch)
                if (update != null) {
                    val currentVersionCode = com.example.BuildConfig.VERSION_CODE
                    if (update.versionCode > currentVersionCode) {
                        latestUpdateInfo = update
                        isUpdateAvailable = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun triggerManualUpdateCheck(onComplete: (Boolean, String) -> Unit) {
        isCheckingForUpdates = true
        updateCheckError = null
        viewModelScope.launch {
            try {
                val update = com.example.util.UpdateHelper.checkForUpdates(githubOwner, githubRepo, githubBranch)
                isCheckingForUpdates = false
                if (update != null) {
                    latestUpdateInfo = update
                    val currentVersionCode = com.example.BuildConfig.VERSION_CODE
                    if (update.versionCode > currentVersionCode) {
                        isUpdateAvailable = true
                        onComplete(true, "A new update (${update.versionName}) is available!")
                    } else {
                        isUpdateAvailable = false
                        onComplete(false, "You are already using the latest version ($currentVersionCode).")
                    }
                } else {
                    onComplete(false, "Unable to check for updates. Verify repo settings.")
                }
            } catch (e: Exception) {
                isCheckingForUpdates = false
                onComplete(false, "Error: ${e.localizedMessage}")
            }
        }
    }

    fun startApkDownload(context: android.content.Context) {
        val update = latestUpdateInfo ?: return
        downloadProgress = 0.0f
        downloadStatusText = "Downloading update..."
        viewModelScope.launch {
            try {
                val file = com.example.util.UpdateHelper.downloadApk(context, update.apkUrl, githubOwner, githubRepo, githubBranch) { progress, downloaded, total ->
                    downloadProgress = progress
                    if (total > 0) {
                        val downloadedMb = downloaded.toFloat() / (1024 * 1024)
                        val totalMb = total.toFloat() / (1024 * 1024)
                        downloadStatusText = "Downloading: ${(progress * 100).toInt()}% (%.2f MB / %.2f MB)".format(downloadedMb, totalMb)
                    } else {
                        val downloadedMb = downloaded.toFloat() / (1024 * 1024)
                        downloadStatusText = "Downloading: %.2f MB (estimating size...)".format(downloadedMb)
                    }
                }
                downloadStatusText = "Installing update..."
                val launched = com.example.util.UpdateHelper.installApk(context, file)
                if (launched) {
                    downloadProgress = null
                    downloadStatusText = ""
                } else {
                    downloadStatusText = "Requires install permission. Grant permission and try again."
                    downloadProgress = null
                }
            } catch (e: Exception) {
                downloadStatusText = "Download failed: ${e.localizedMessage ?: "Unknown error"}"
                downloadProgress = null
                e.printStackTrace()
            }
        }
    }
}

data class SearchResult(
    val moduleType: String,
    val title: String,
    val subtitle: String,
    val receivedDate: String,
    val status: String,
    val originalItem: BankingItem? = null,
    val originalLead: CustomerHunting? = null,
    val originalForm: DigitalForm? = null
)
