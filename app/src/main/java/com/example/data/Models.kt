package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "banking_items")
data class BankingItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "DEBIT_CARD", "PIN", "CHEQUE_BOOK", "DPS"
    val customerName: String,
    val accountNumber: String,
    val address: String,
    val phoneNumber: String,
    val receivedDate: Long, // timestamp
    val destroyAfter: Long, // receivedDate + 90 days
    val remarks: String,
    val isDestroyed: Boolean = false,
    val isBalanced: Boolean = true,
    val isDelivered: Boolean = false,
    val deliveryDate: Long = 0L,
    val isLetterIssued: Boolean = false
)

@Entity(tableName = "quantity_logs")
data class QuantityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemType: String, // "PRIZE_BOND", "PAY_ORDER"
    val previousQuantity: Int,
    val newQuantity: Int,
    val editedBy: String,
    val dateStr: String, // YYYY-MM-DD
    val timeStr: String, // HH:MM:SS
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "atm_loading_logs")
data class AtmLoadingLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val atmName: String, // "ATM-25 CHIRIRBANDAR", "ATM-42 THAKURGAON", etc.
    val dateStr: String,
    val timeStr: String,
    val c1Remaining: Int,
    val c2Remaining: Int,
    val c3Remaining: Int,
    val c4Remaining: Int,
    val c1Loading: Int,
    val c2Loading: Int,
    val c3Loading: Int,
    val c4Loading: Int,
    val loadingAmount: Long,
    val operatorName: String,
    val remarks: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "digital_forms")
data class DigitalForm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val formType: String, // "ACCOUNT_SERVICE", "CRF", "SUPPLEMENTARY", "CREDIT_CARD", "DPS", "FIXED_DEPOSIT", "AUTHORIZATION", "BGB_LOAN_TOPUP"
    val customerName: String,
    val accountNumber: String,
    val dateStr: String, // Auto-filled date
    val remarks: String,
    val signaturePath: String = "", // Base64 encoded or svg representation
    val jsonFields: String = "{}", // Store form-specific fields in JSON
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "todo_tasks")
data class TodoTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val priority: String, // "HIGH", "MEDIUM", "LOW"
    val dueDate: Long, // Start of day timestamp
    val dueTime: String, // "16:00"
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "customer_hunting")
data class CustomerHunting(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerName: String,
    val phoneNumber: String,
    val address: String,
    val interestedProduct: String,
    val isGrabbed: Boolean = false,
    val priority: String, // "HIGH", "MEDIUM", "LOW"
    val completionPercentage: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "branch_users")
data class BranchUser(
    @PrimaryKey val ein: String,
    val name: String,
    val pin: String,
    val isAuthorized: Boolean = false,
    val cameraAuthorized: Boolean = false,
    val storageAuthorized: Boolean = false,
    val locationAuthorized: Boolean = false,
    val isToufiq: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "password_history")
data class PasswordHistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ein: String,
    val oldPin: String,
    val newPin: String,
    val changeTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "letters_issued")
data class LetterIssued(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerName: String,
    val accountNumber: String,
    val phoneNumber: String,
    val letterIssueDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "recycle_bin")
data class RecycleBinItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalType: String, // "BANKING_ITEM", "FORM", "TASK", "HUNTING"
    val originalId: Int,
    val title: String,
    val subtitle: String,
    val serializedData: String,
    val deletedTimestamp: Long = System.currentTimeMillis()
)
