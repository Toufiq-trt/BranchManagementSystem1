package com.example.data

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

object FirebaseSyncHelper {
    private const val TAG = "FirebaseSyncHelper"
    private val scope = CoroutineScope(Dispatchers.IO)

    // Safe retrieval of database reference
    private fun getDbRef(): com.google.firebase.database.DatabaseReference? {
        return try {
            FirebaseDatabase.getInstance().reference
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Database is not configured: ${e.message}")
            null
        }
    }

    fun <T : Any> pushToFirebase(path: String, key: String, item: T) {
        scope.launch {
            try {
                val ref = getDbRef() ?: return@launch
                ref.child(path).child(key).setValue(item)
                    .addOnSuccessListener {
                        Log.d(TAG, "Successfully synced to Firebase: $path/$key")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to sync to Firebase: ${e.message}")
                    }
            } catch (t: Throwable) {
                Log.e(TAG, "Exception pushing to Firebase: ${t.message}")
            }
        }
    }

    fun deleteFromFirebase(path: String, key: String) {
        scope.launch {
            try {
                val ref = getDbRef() ?: return@launch
                ref.child(path).child(key).removeValue()
                    .addOnSuccessListener {
                        Log.d(TAG, "Successfully deleted from Firebase: $path/$key")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to delete from Firebase: ${e.message}")
                    }
            } catch (t: Throwable) {
                Log.e(TAG, "Exception deleting from Firebase: ${t.message}")
            }
        }
    }

    // Set up listeners to mirror Firebase state down to local Room database on startup
    fun startBiDirectionalSync(repository: BankingRepository) {
        scope.launch {
            try {
                // If local items already exist, we DO NOT register sync-down listeners to protect local modifications.
                val localItems = try {
                    repository.getAllItems().first()
                } catch (e: Exception) {
                    emptyList()
                }
                if (localItems.isNotEmpty()) {
                    Log.d(TAG, "Local database is already populated. Skipping sync-down listeners to protect local modifications.")
                    return@launch
                }

                val ref = getDbRef() ?: return@launch
                
                // 1. Sync banking_items
                ref.child("banking_items").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        scope.launch {
                            try {
                                snapshot.children.forEach { child ->
                                    val id = child.key?.toIntOrNull() ?: return@forEach
                                    val map = child.value as? Map<*, *> ?: return@forEach
                                    val item = BankingItem(
                                        id = id,
                                        type = map["type"] as? String ?: "",
                                        customerName = map["customerName"] as? String ?: "",
                                        accountNumber = map["accountNumber"] as? String ?: "",
                                        address = map["address"] as? String ?: "",
                                        phoneNumber = map["phoneNumber"] as? String ?: "",
                                        receivedDate = (map["receivedDate"] as? Number)?.toLong() ?: 0L,
                                        destroyAfter = (map["destroyAfter"] as? Number)?.toLong() ?: 0L,
                                        remarks = map["remarks"] as? String ?: "",
                                        isDestroyed = map["isDestroyed"] as? Boolean ?: false,
                                        isBalanced = map["isBalanced"] as? Boolean ?: true
                                    )
                                    repository.updateBankingItem(item)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing banking_items snapshot: ${e.message}")
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })

                // 2. Sync quantity_logs
                ref.child("quantity_logs").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        scope.launch {
                            try {
                                snapshot.children.forEach { child ->
                                    val id = child.key?.toIntOrNull() ?: return@forEach
                                    val map = child.value as? Map<*, *> ?: return@forEach
                                    val log = QuantityLog(
                                        id = id,
                                        itemType = map["itemType"] as? String ?: "",
                                        previousQuantity = (map["previousQuantity"] as? Number)?.toInt() ?: 0,
                                        newQuantity = (map["newQuantity"] as? Number)?.toInt() ?: 0,
                                        editedBy = map["editedBy"] as? String ?: "",
                                        dateStr = map["dateStr"] as? String ?: "",
                                        timeStr = map["timeStr"] as? String ?: "",
                                        timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L
                                    )
                                    repository.insertQuantityLogDirectly(log)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing quantity_logs snapshot: ${e.message}")
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })

                // 3. Sync todo_tasks
                ref.child("todo_tasks").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        scope.launch {
                            try {
                                snapshot.children.forEach { child ->
                                    val id = child.key?.toIntOrNull() ?: return@forEach
                                    val map = child.value as? Map<*, *> ?: return@forEach
                                    val task = TodoTask(
                                        id = id,
                                        title = map["title"] as? String ?: "",
                                        priority = map["priority"] as? String ?: "",
                                        dueDate = (map["dueDate"] as? Number)?.toLong() ?: 0L,
                                        dueTime = map["dueTime"] as? String ?: "",
                                        isCompleted = map["isCompleted"] as? Boolean ?: false,
                                        timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L
                                    )
                                    repository.insertTaskDirectly(task)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing todo_tasks snapshot: ${e.message}")
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })

                // 4. Sync customer_hunting
                ref.child("customer_hunting").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        scope.launch {
                            try {
                                snapshot.children.forEach { child ->
                                    val id = child.key?.toIntOrNull() ?: return@forEach
                                    val map = child.value as? Map<*, *> ?: return@forEach
                                    val hunting = CustomerHunting(
                                        id = id,
                                        customerName = map["customerName"] as? String ?: "",
                                        phoneNumber = map["phoneNumber"] as? String ?: "",
                                        address = map["address"] as? String ?: "",
                                        interestedProduct = map["interestedProduct"] as? String ?: "",
                                        isGrabbed = map["isGrabbed"] as? Boolean ?: false,
                                        priority = map["priority"] as? String ?: "",
                                        completionPercentage = (map["completionPercentage"] as? Number)?.toInt() ?: 0,
                                        timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L
                                    )
                                    repository.insertHuntingDirectly(hunting)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing customer_hunting snapshot: ${e.message}")
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })

                // 5. Sync branch_users
                ref.child("branch_users").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        scope.launch {
                            try {
                                snapshot.children.forEach { child ->
                                    val ein = child.key ?: return@forEach
                                    val map = child.value as? Map<*, *> ?: return@forEach
                                    val user = BranchUser(
                                        ein = ein,
                                        name = map["name"] as? String ?: "",
                                        pin = map["pin"] as? String ?: "",
                                        isAuthorized = map["isAuthorized"] as? Boolean ?: false,
                                        cameraAuthorized = map["cameraAuthorized"] as? Boolean ?: false,
                                        storageAuthorized = map["storageAuthorized"] as? Boolean ?: false,
                                        locationAuthorized = map["locationAuthorized"] as? Boolean ?: false,
                                        isToufiq = map["isToufiq"] as? Boolean ?: false,
                                        timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L
                                    )
                                    repository.insertUserDirectly(user)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing branch_users snapshot: ${e.message}")
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })

                // 6. Sync digital_forms
                ref.child("digital_forms").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        scope.launch {
                            try {
                                snapshot.children.forEach { child ->
                                    val id = child.key?.toIntOrNull() ?: return@forEach
                                    val map = child.value as? Map<*, *> ?: return@forEach
                                    val form = DigitalForm(
                                        id = id,
                                        formType = map["formType"] as? String ?: "",
                                        customerName = map["customerName"] as? String ?: "",
                                        accountNumber = map["accountNumber"] as? String ?: "",
                                        dateStr = map["dateStr"] as? String ?: "",
                                        remarks = map["remarks"] as? String ?: "",
                                        signaturePath = map["signaturePath"] as? String ?: "",
                                        jsonFields = map["jsonFields"] as? String ?: "{}",
                                        timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L
                                    )
                                    repository.insertFormDirectly(form)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing digital_forms snapshot: ${e.message}")
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })

            } catch (t: Throwable) {
                Log.e(TAG, "Exception during Firebase listener sync setup: ${t.message}")
            }
        }
    }
}
