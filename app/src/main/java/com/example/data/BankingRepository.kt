package com.example.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class BankingRepository(private val bankingDao: BankingDao) {

    // --- Banking Items ---
    fun getItemsByType(type: String): Flow<List<BankingItem>> = bankingDao.getItemsByType(type)

    fun getAllItems(): Flow<List<BankingItem>> = bankingDao.getAllItems()

    suspend fun insertBankingItem(
        type: String,
        customerName: String,
        accountNumber: String,
        address: String,
        phoneNumber: String,
        receivedDateOverride: Long? = null,
        remarks: String,
        isDemo: Boolean = false
    ): Long {
        val received = receivedDateOverride ?: System.currentTimeMillis()
        val destroy = received + (90L * 24L * 60L * 60L * 1000L) // 90 days later
        val item = BankingItem(
            type = type,
            customerName = customerName,
            accountNumber = accountNumber,
            address = address,
            phoneNumber = phoneNumber,
            receivedDate = received,
            destroyAfter = destroy,
            remarks = remarks,
            isDestroyed = false,
            isBalanced = true, // Automatically create active balancing entry
            isDemo = isDemo
        )
        val id = bankingDao.insertItem(item)
        val finalItem = item.copy(id = id.toInt())
        FirebaseSyncHelper.pushToFirebase("banking_items", id.toString(), finalItem)
        return id
    }

    suspend fun updateBankingItem(item: BankingItem) {
        bankingDao.updateItem(item)
        FirebaseSyncHelper.pushToFirebase("banking_items", item.id.toString(), item)
    }

    suspend fun deleteBankingItem(item: BankingItem) {
        bankingDao.deleteItem(item)
        FirebaseSyncHelper.deleteFromFirebase("banking_items", item.id.toString())
    }

    // --- Quantity Logs (Prize Bond, Pay Order) ---
    fun getQuantityLogs(type: String): Flow<List<QuantityLog>> = bankingDao.getQuantityLogs(type)

    suspend fun getLatestQuantity(type: String): Int {
        return bankingDao.getLatestQuantityLog(type)?.newQuantity ?: 0
    }

    suspend fun updateQuantity(type: String, newQty: Int, editedBy: String, isDemo: Boolean = false): Long {
        val latestLog = bankingDao.getLatestQuantityLog(type)
        val prevQty = latestLog?.newQuantity ?: 0
        val now = Date()
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)
        
        val log = QuantityLog(
            itemType = type,
            previousQuantity = prevQty,
            newQuantity = newQty,
            editedBy = editedBy,
            dateStr = dateStr,
            timeStr = timeStr,
            timestamp = System.currentTimeMillis(),
            isDemo = isDemo
        )
        val id = bankingDao.insertQuantityLog(log)
        val finalLog = log.copy(id = id.toInt())
        FirebaseSyncHelper.pushToFirebase("quantity_logs", id.toString(), finalLog)
        return id
    }

    // --- ATM Loading Logs ---
    fun getAtmLoadingLogs(): Flow<List<AtmLoadingLog>> = bankingDao.getAtmLoadingLogs()

    suspend fun insertAtmLoadingLog(
        atmName: String,
        c1Remaining: Int,
        c2Remaining: Int,
        c3Remaining: Int,
        c4Remaining: Int,
        c1Loading: Int,
        c2Loading: Int,
        c3Loading: Int,
        c4Loading: Int,
        loadingAmount: Long,
        operatorName: String,
        remarks: String
    ): Long {
        val now = Date()
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)
        
        val log = AtmLoadingLog(
            atmName = atmName,
            dateStr = dateStr,
            timeStr = timeStr,
            c1Remaining = c1Remaining,
            c2Remaining = c2Remaining,
            c3Remaining = c3Remaining,
            c4Remaining = c4Remaining,
            c1Loading = c1Loading,
            c2Loading = c2Loading,
            c3Loading = c3Loading,
            c4Loading = c4Loading,
            loadingAmount = loadingAmount,
            operatorName = operatorName,
            remarks = remarks,
            timestamp = System.currentTimeMillis()
        )
        val id = bankingDao.insertAtmLoadingLog(log)
        val finalLog = log.copy(id = id.toInt())
        FirebaseSyncHelper.pushToFirebase("atm_loading_logs", id.toString(), finalLog)
        return id
    }

    // --- Digital Forms ---
    fun getAllForms(): Flow<List<DigitalForm>> = bankingDao.getAllForms()
    fun getFormsByType(type: String): Flow<List<DigitalForm>> = bankingDao.getFormsByType(type)
    suspend fun getFormById(id: Int): DigitalForm? = bankingDao.getFormById(id)
    
    suspend fun insertForm(
        formType: String,
        customerName: String,
        accountNumber: String,
        remarks: String,
        signaturePath: String = "",
        jsonFields: String = "{}",
        pdfFilePath: String? = null
    ): Long {
        val now = Date()
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        val form = DigitalForm(
            formType = formType,
            customerName = customerName,
            accountNumber = accountNumber,
            dateStr = dateStr,
            remarks = remarks,
            signaturePath = signaturePath,
            jsonFields = jsonFields,
            timestamp = System.currentTimeMillis(),
            pdfFilePath = pdfFilePath
        )
        val id = bankingDao.insertForm(form)
        val finalForm = form.copy(id = id.toInt())
        FirebaseSyncHelper.pushToFirebase("digital_forms", id.toString(), finalForm)
        return id
    }

    suspend fun updateForm(form: DigitalForm) {
        bankingDao.updateForm(form)
        FirebaseSyncHelper.pushToFirebase("digital_forms", form.id.toString(), form)
    }

    suspend fun deleteForm(form: DigitalForm) {
        bankingDao.deleteForm(form)
        FirebaseSyncHelper.deleteFromFirebase("digital_forms", form.id.toString())
    }

    // --- Todo Tasks ---
    fun getAllTasks(): Flow<List<TodoTask>> = bankingDao.getAllTasks()
    
    suspend fun insertTask(title: String, priority: String, dueDate: Long, dueTime: String, isDemo: Boolean = false): Long {
        val task = TodoTask(
            title = title,
            priority = priority,
            dueDate = dueDate,
            dueTime = dueTime,
            isCompleted = false,
            isDemo = isDemo
        )
        val id = bankingDao.insertTask(task)
        val finalTask = task.copy(id = id.toInt())
        FirebaseSyncHelper.pushToFirebase("todo_tasks", id.toString(), finalTask)
        return id
    }

    suspend fun updateTask(task: TodoTask) {
        bankingDao.updateTask(task)
        FirebaseSyncHelper.pushToFirebase("todo_tasks", task.id.toString(), task)
    }

    suspend fun deleteTask(task: TodoTask) {
        bankingDao.deleteTask(task)
        FirebaseSyncHelper.deleteFromFirebase("todo_tasks", task.id.toString())
    }

    // --- Customer Hunting ---
    fun getAllHunting(): Flow<List<CustomerHunting>> = bankingDao.getAllHunting()
    
    suspend fun insertHunting(
        customerName: String,
        phoneNumber: String,
        address: String,
        interestedProduct: String,
        priority: String,
        completionPercentage: Int,
        isDemo: Boolean = false
    ): Long {
        val hunting = CustomerHunting(
            customerName = customerName,
            phoneNumber = phoneNumber,
            address = address,
            interestedProduct = interestedProduct,
            isGrabbed = completionPercentage >= 100,
            priority = priority,
            completionPercentage = completionPercentage,
            isDemo = isDemo
        )
        val id = bankingDao.insertHunting(hunting)
        val finalHunting = hunting.copy(id = id.toInt())
        FirebaseSyncHelper.pushToFirebase("customer_hunting", id.toString(), finalHunting)
        return id
    }

    suspend fun updateHunting(hunting: CustomerHunting) {
        bankingDao.updateHunting(hunting)
        FirebaseSyncHelper.pushToFirebase("customer_hunting", hunting.id.toString(), hunting)
    }

    suspend fun deleteHunting(hunting: CustomerHunting) {
        bankingDao.deleteHunting(hunting)
        FirebaseSyncHelper.deleteFromFirebase("customer_hunting", hunting.id.toString())
    }

    // --- Branch Users ---
    fun getAllUsers(): Flow<List<BranchUser>> = bankingDao.getAllUsers()

    suspend fun getUser(ein: String): BranchUser? = bankingDao.getUser(ein)

    suspend fun registerUser(ein: String, name: String, pin: String): Long {
        val user = BranchUser(
            ein = ein,
            name = name,
            pin = pin,
            isAuthorized = false,
            cameraAuthorized = false,
            storageAuthorized = false,
            locationAuthorized = false,
            isToufiq = false
        )
        // Log the first password to history
        insertPasswordLog(ein, "", pin)
        val id = bankingDao.insertUser(user)
        FirebaseSyncHelper.pushToFirebase("branch_users", ein, user)
        return id
    }

    suspend fun insertUserDirectly(user: BranchUser): Long {
        val id = bankingDao.insertUser(user)
        FirebaseSyncHelper.pushToFirebase("branch_users", user.ein, user)
        return id
    }

    suspend fun updateUser(user: BranchUser) {
        bankingDao.updateUser(user)
        FirebaseSyncHelper.pushToFirebase("branch_users", user.ein, user)
    }

    suspend fun deleteUser(user: BranchUser) {
        bankingDao.deleteUser(user)
        FirebaseSyncHelper.deleteFromFirebase("branch_users", user.ein)
    }

    // --- Password History ---
    fun getAllPasswordLogs(): Flow<List<PasswordHistoryEntry>> = bankingDao.getAllPasswordLogs()

    fun getPasswordLogsForUser(ein: String): Flow<List<PasswordHistoryEntry>> = bankingDao.getPasswordLogsForUser(ein)

    suspend fun insertPasswordLog(ein: String, oldPin: String, newPin: String): Long {
        val entry = PasswordHistoryEntry(
            ein = ein,
            oldPin = oldPin,
            newPin = newPin,
            changeTimestamp = System.currentTimeMillis()
        )
        val id = bankingDao.insertPasswordLog(entry)
        val finalEntry = entry.copy(id = id.toInt())
        FirebaseSyncHelper.pushToFirebase("password_history", id.toString(), finalEntry)
        return id
    }

    // --- Letters Issued ---
    fun getAllLettersIssued(): Flow<List<LetterIssued>> = bankingDao.getAllLettersIssued()

    suspend fun insertLetterIssued(customerName: String, accountNumber: String, phoneNumber: String): Long {
        val entry = LetterIssued(
            customerName = customerName,
            accountNumber = accountNumber,
            phoneNumber = phoneNumber,
            letterIssueDate = System.currentTimeMillis()
        )
        val id = bankingDao.insertLetterIssued(entry)
        val finalEntry = entry.copy(id = id.toInt())
        FirebaseSyncHelper.pushToFirebase("letters_issued", id.toString(), finalEntry)
        return id
    }

    // --- Direct insertions called by sync receiver (without triggering Firebase push feedback loops) ---
    suspend fun insertQuantityLogDirectly(log: QuantityLog): Long {
        return bankingDao.insertQuantityLog(log)
    }

    suspend fun insertTaskDirectly(task: TodoTask): Long {
        return bankingDao.insertTask(task)
    }

    suspend fun insertHuntingDirectly(hunting: CustomerHunting): Long {
        return bankingDao.insertHunting(hunting)
    }

    suspend fun insertFormDirectly(form: DigitalForm): Long {
        return bankingDao.insertForm(form)
    }

    // --- Recycle Bin ---
    fun getAllRecycleBinItems(): Flow<List<RecycleBinItem>> = bankingDao.getAllRecycleBinItems()

    suspend fun getRecycleBinItemById(id: Int): RecycleBinItem? = bankingDao.getRecycleBinItemById(id)

    suspend fun insertRecycleBinItem(originalType: String, originalId: Int, title: String, subtitle: String, serializedData: String): Long {
        val item = RecycleBinItem(
            originalType = originalType,
            originalId = originalId,
            title = title,
            subtitle = subtitle,
            serializedData = serializedData,
            deletedTimestamp = System.currentTimeMillis()
        )
        return bankingDao.insertRecycleBinItem(item)
    }

    suspend fun deleteRecycleBinItemById(id: Int) {
        bankingDao.deleteRecycleBinItemById(id)
    }

    suspend fun deleteRecycleBinItemsByIds(ids: List<Int>) {
        bankingDao.deleteRecycleBinItemsByIds(ids)
    }

    suspend fun autoPurgeRecycleBin() {
        val tenDaysAgo = System.currentTimeMillis() - (10L * 24L * 60L * 60L * 1000L)
        bankingDao.autoPurgeRecycleBin(tenDaysAgo)
    }

    suspend fun insertBankingItemDirectly(item: BankingItem): Long {
        return bankingDao.insertItem(item)
    }

    suspend fun checkDuplicateItem(type: String, name: String, accountNumber: String): Boolean {
        return bankingDao.checkDuplicateItem(type, name, accountNumber) != null
    }

    suspend fun clearAllDemoData() {
        bankingDao.clearDemoBankingItems()
        bankingDao.clearDemoTodoTasks()
        bankingDao.clearDemoCustomerHunting()
        bankingDao.clearDemoQuantityLogs()
    }

    suspend fun clearAllData() {
        bankingDao.clearBankingItems()
        bankingDao.clearTodoTasks()
        bankingDao.clearCustomerHunting()
        bankingDao.clearDigitalForms()
        bankingDao.clearQuantityLogs()
        bankingDao.clearAtmLoadingLogs()
        bankingDao.clearLettersIssued()
        bankingDao.clearRecycleBin()
        bankingDao.clearPasswordHistory()
    }
}
