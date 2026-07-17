package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BankingDao {
    // --- Banking Items (Debit Cards, PINs, Cheques, DPS) ---
    @Query("SELECT * FROM banking_items WHERE type = :type ORDER BY receivedDate DESC")
    fun getItemsByType(type: String): Flow<List<BankingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: BankingItem): Long

    @Update
    suspend fun updateItem(item: BankingItem)

    @Delete
    suspend fun deleteItem(item: BankingItem)

    @Query("SELECT * FROM banking_items ORDER BY receivedDate DESC")
    fun getAllItems(): Flow<List<BankingItem>>

    // --- Quantity Logs (Prize Bonds, Pay Orders) ---
    @Query("SELECT * FROM quantity_logs WHERE itemType = :itemType ORDER BY timestamp DESC")
    fun getQuantityLogs(itemType: String): Flow<List<QuantityLog>>

    @Query("SELECT * FROM quantity_logs WHERE itemType = :itemType ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestQuantityLog(itemType: String): QuantityLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuantityLog(log: QuantityLog): Long

    // --- ATM Loading Logs ---
    @Query("SELECT * FROM atm_loading_logs ORDER BY timestamp DESC")
    fun getAtmLoadingLogs(): Flow<List<AtmLoadingLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAtmLoadingLog(log: AtmLoadingLog): Long

    // --- Digital Forms ---
    @Query("SELECT * FROM digital_forms ORDER BY timestamp DESC")
    fun getAllForms(): Flow<List<DigitalForm>>

    @Query("SELECT * FROM digital_forms WHERE formType = :formType ORDER BY timestamp DESC")
    fun getFormsByType(formType: String): Flow<List<DigitalForm>>

    @Query("SELECT * FROM digital_forms WHERE id = :id")
    suspend fun getFormById(id: Int): DigitalForm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForm(form: DigitalForm): Long

    @Update
    suspend fun updateForm(form: DigitalForm)

    @Delete
    suspend fun deleteForm(form: DigitalForm)

    // --- Todo Tasks ---
    @Query("SELECT * FROM todo_tasks ORDER BY isCompleted ASC, dueDate ASC")
    fun getAllTasks(): Flow<List<TodoTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TodoTask): Long

    @Update
    suspend fun updateTask(task: TodoTask)

    @Delete
    suspend fun deleteTask(task: TodoTask)

    // --- Customer Hunting ---
    @Query("SELECT * FROM customer_hunting ORDER BY isGrabbed ASC, priority DESC")
    fun getAllHunting(): Flow<List<CustomerHunting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHunting(hunting: CustomerHunting): Long

    @Update
    suspend fun updateHunting(hunting: CustomerHunting)

    @Delete
    suspend fun deleteHunting(hunting: CustomerHunting)

    // --- Branch Users ---
    @Query("SELECT * FROM branch_users ORDER BY timestamp DESC")
    fun getAllUsers(): Flow<List<BranchUser>>

    @Query("SELECT * FROM branch_users WHERE ein = :ein LIMIT 1")
    suspend fun getUser(ein: String): BranchUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: BranchUser): Long

    @Update
    suspend fun updateUser(user: BranchUser)

    @Delete
    suspend fun deleteUser(user: BranchUser)

    // --- Password History ---
    @Query("SELECT * FROM password_history ORDER BY changeTimestamp DESC")
    fun getAllPasswordLogs(): Flow<List<PasswordHistoryEntry>>

    @Query("SELECT * FROM password_history WHERE ein = :ein ORDER BY changeTimestamp DESC")
    fun getPasswordLogsForUser(ein: String): Flow<List<PasswordHistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPasswordLog(entry: PasswordHistoryEntry): Long

    // --- Letters Issued ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLetterIssued(letter: LetterIssued): Long

    @Query("SELECT * FROM letters_issued ORDER BY letterIssueDate DESC")
    fun getAllLettersIssued(): Flow<List<LetterIssued>>

    // --- Recycle Bin ---
    @Query("SELECT * FROM recycle_bin ORDER BY deletedTimestamp DESC")
    fun getAllRecycleBinItems(): Flow<List<RecycleBinItem>>

    @Query("SELECT * FROM recycle_bin WHERE id = :id")
    suspend fun getRecycleBinItemById(id: Int): RecycleBinItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecycleBinItem(item: RecycleBinItem): Long

    @Query("DELETE FROM recycle_bin WHERE id = :id")
    suspend fun deleteRecycleBinItemById(id: Int)

    @Query("DELETE FROM recycle_bin WHERE id IN (:ids)")
    suspend fun deleteRecycleBinItemsByIds(ids: List<Int>)

    @Query("DELETE FROM recycle_bin WHERE deletedTimestamp < :cutoff")
    suspend fun autoPurgeRecycleBin(cutoff: Long)

    @Query("SELECT * FROM banking_items WHERE type = :type AND customerName = :name AND accountNumber = :accountNumber LIMIT 1")
    suspend fun checkDuplicateItem(type: String, name: String, accountNumber: String): BankingItem?

    @Query("DELETE FROM banking_items WHERE isDemo = 1")
    suspend fun clearDemoBankingItems()

    @Query("DELETE FROM todo_tasks WHERE isDemo = 1")
    suspend fun clearDemoTodoTasks()

    @Query("DELETE FROM customer_hunting WHERE isDemo = 1")
    suspend fun clearDemoCustomerHunting()

    @Query("DELETE FROM quantity_logs WHERE isDemo = 1")
    suspend fun clearDemoQuantityLogs()

    @Query("DELETE FROM banking_items")
    suspend fun clearBankingItems()

    @Query("DELETE FROM todo_tasks")
    suspend fun clearTodoTasks()

    @Query("DELETE FROM customer_hunting")
    suspend fun clearCustomerHunting()

    @Query("DELETE FROM digital_forms")
    suspend fun clearDigitalForms()

    @Query("DELETE FROM quantity_logs")
    suspend fun clearQuantityLogs()

    @Query("DELETE FROM atm_loading_logs")
    suspend fun clearAtmLoadingLogs()

    @Query("DELETE FROM letters_issued")
    suspend fun clearLettersIssued()

    @Query("DELETE FROM recycle_bin")
    suspend fun clearRecycleBin()

    @Query("DELETE FROM password_history")
    suspend fun clearPasswordHistory()
}
