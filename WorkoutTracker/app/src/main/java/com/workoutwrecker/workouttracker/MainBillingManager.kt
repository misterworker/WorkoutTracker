/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.workoutwrecker.workouttracker.ui.premium.BillingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow

class MainBillingManager( // Billing Manager for Main Activity
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val billingClient: BillingClient,
    private val billingUpdatesListener: BillingUpdatesListener
) {
    companion object {
        private const val TAG = "MainBillingManager"
    }
    interface BillingUpdatesListener {
        fun onRecoverProgress(purchasesList: List<Purchase>)
    }
    private var functions: FirebaseFunctions = FirebaseFunctions.getInstance()

    fun getFirebaseBasePlan(callback: (String) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return callback("")

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val basePlanId = document.getString("basePlanId") ?: ""
                val purchaseToken = document.getString("purchaseToken") ?: ""
                callback(purchaseToken)  // Return both values via callback
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to get document", Toast.LENGTH_SHORT).show()
                callback("")  // Return empty values on failure
            }
    }

    fun queryPurchasesRecovery() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "PurchasesList: $purchasesList")
                billingUpdatesListener.onRecoverProgress(purchasesList)
            } else {
                Log.e(TAG, "Error querying purchases: ${billingResult.responseCode}")
            }
        }
    }

    fun verifyPurchaseWithFirebase(purchaseToken: String, callback: (Boolean) -> Unit){
        val data = hashMapOf(
            "token" to purchaseToken,
        )

        functions
            .getHttpsCallable("verify_purchase")
            .call(data)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Purchase verified successfully with Firebase")
                    val result = task.result?.data as? Map<String, Any>
                    if (result != null) {
                        Log.d("BillingManager", "Result from Firebase: $result")
                        updateUserSubscriptionStatus(result, callback)
                    } else {
                        Log.e(TAG, "No data received from Firebase function")
                        callback(false)
                    }
                } else {
                    Log.e(TAG, "Failed to verify purchase with Firebase", task.exception)
                    callback(false)
                }
            }
    }

    private fun updateUserSubscriptionStatus(purchase: Map<String, Any>, callback: (Boolean) -> Unit) {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return callback(false)
        val db = FirebaseFirestore.getInstance()
        val userSubscription = hashMapOf(
            "isPremium" to true,
            "basePlanId" to purchase["basePlanId"],
            "purchaseToken" to purchase["purchaseToken"],
            "paymentState" to purchase["paymentState"],
            "purchaseTime" to purchase["purchaseTime"],
            "expiryTime" to purchase["expiryTime"],
        )
        Log.d(TAG, "User Subscription: $userSubscription")
        db.collection("users").document(userId)
            .set(userSubscription, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "User subscription status updated in Firebase")
                // Securely store subscription data using EncryptedSharedPreferences
                val securePrefManager = SecurePreferenceManager(context)

                // Save the relevant user subscription data
                securePrefManager.storeString("basePlanId", purchase["basePlanId"] as String)
                securePrefManager.storeString("purchaseToken", purchase["purchaseToken"] as String)
                securePrefManager.storeString("paymentState",
                    (purchase["paymentState"] as String).toString()
                )
                securePrefManager.storeString("purchaseTime", purchase["purchaseTime"] as String)
                securePrefManager.storeString("expiryTime", purchase["expiryTime"] as String)
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating user subscription status in Firebase", e)
                callback(false)
            }
    }

    fun handlePurchase(purchase: Purchase, attempt: Int = 1, callback: (Boolean) -> Unit) {
        if (attempt > 5) {
            callback(false);return
        }
        Log.d(TAG, "Received purchase state: ${purchase.purchaseState}")

        // Only handle purchases that are completed (PURCHASED)
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {callback(false);return}
        if (purchase.isAcknowledged) {callback(false);return}

        var timeTaken = 0.0
        if (attempt != 1) {
            timeTaken = (attempt - 1).toDouble().pow(2.0) // Retry with exponential backoff: 1, 4, 9, 16 seconds, etc.
        }
        Log.d(TAG, "Time Taken (delay before retry): $timeTaken seconds")

        // Use lifecycle scope to run the acknowledgment in the background thread
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            delay((timeTaken * 1000).toLong()) // Delay before retrying based on the attempt number

            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            // Acknowledge the purchase using the BillingClient
            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase acknowledged successfully")
                    callback(true)
                } else {
                    Log.e(TAG, "Failed to acknowledge purchase: $billingResult")
                    // Retry by calling handlePurchase again with the incremented attempt number
                    handlePurchase(purchase, attempt + 1, callback)
                }
            }
        }
    }

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "BillingClient setup finished.")
                    // Billing Client is ready. Start purchase recovery.
                    queryPurchasesRecovery()
                } else {
                    Log.e(TAG, "BillingClient setup failed: ${billingResult.responseCode}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.e(TAG, "Billing service disconnected.")
                // Implement retry logic if needed.
            }
        })
    }

    fun endConnection() {
        billingClient.endConnection()
    }
}
