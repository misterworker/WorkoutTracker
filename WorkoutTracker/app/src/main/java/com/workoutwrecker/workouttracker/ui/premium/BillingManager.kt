/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.premium

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.*
import com.google.common.collect.ImmutableList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.workoutwrecker.workouttracker.SecurePreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.pow


class BillingManager(private val context: Context, private val billingUpdatesListener: BillingUpdatesListener,
                     private val fragment: PremiumPurchaseFragment) :
    PurchasesUpdatedListener {

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .enablePendingPurchases()
        .setListener(this)
        .build()
    private var functions: FirebaseFunctions

    interface BillingUpdatesListener {
        fun onBillingClientSetupFinished()
        fun onPurchasesUpdated(purchases: List<Purchase>)
        fun onPurchaseCancelled()
        fun onPurchaseError(errorCode: Int)
    }

    init {
        startServiceConnection {
            billingUpdatesListener.onBillingClientSetupFinished()
        }
        functions = FirebaseFunctions.getInstance()
    }

    fun initiatePurchase(activity: Activity, productId: String, basePlanId: String) {
        queryProductDetails(activity, productId, basePlanId)
    }

    private fun queryProductDetails(activity: Activity, productId: String, basePlanId: String) {
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                ImmutableList.of(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS) // Specify SUBS for subscriptions
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Product details queried successfully")
                if (productDetailsList.isNotEmpty()) {
                    Log.d(TAG, "Product Details: $productDetailsList")
                    initiatePurchase(activity, productDetailsList[0], basePlanId)
                } else {
                    Log.e(TAG, "No products found")
                    billingUpdatesListener.onPurchaseError(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
                }
            } else {
                Log.e(TAG, "Error querying product details: ${billingResult.debugMessage}")
            }
        }
    }

    private fun initiatePurchase(activity: Activity, productDetails: ProductDetails, basePlanId: String) {
        val subscriptionOfferDetails = productDetails.subscriptionOfferDetails

        if (!subscriptionOfferDetails.isNullOrEmpty()) {
            // Find the correct base plan
            val offerDetail = subscriptionOfferDetails.find { it.basePlanId == basePlanId }
            if (offerDetail != null) {
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        ImmutableList.of(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offerDetail.offerToken) // Include the offer token
                                .build()
                        )
                    )
                    .build()

                val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
                Log.d(TAG, "Launch Billing Flow result: ${billingResult.debugMessage}")
            } else {
                Log.e(TAG, "No subscription offer details available for base plan ID: $basePlanId")
                billingUpdatesListener.onPurchaseError(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
            }
        } else {
            Log.e(TAG, "No subscription offer details available")
            billingUpdatesListener.onPurchaseError(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        Log.d(TAG, "OnPurchasesUpdated Called")
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            fragment.isProcessingPurchase = true
            billingUpdatesListener.onPurchasesUpdated(purchases)
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            billingUpdatesListener.onPurchaseCancelled()
        } else {
            Log.e(TAG, billingResult.debugMessage)
            billingUpdatesListener.onPurchaseError(billingResult.responseCode)
        }
    }

    fun handlePurchase(purchase: Purchase, attempt: Int) {
        if (attempt > 5){
            return
        }
        var timeTaken = 0.0
        if (attempt != 1){
            timeTaken = attempt-1.toDouble().pow(2.0) //Start with 0 seconds delay, then 1, 4, 9, 16 seconds
        }
        Log.d(TAG, "Time Taken: $timeTaken")

        Log.d(TAG, "Received purchase: ${purchase.purchaseState}")
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) { // Run in IO thread to avoid UI blocking
                    delay((timeTaken * 1000).toLong())
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            fragment.viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                withContext(Dispatchers.Main) {
                                    Log.d(TAG, "Purchase acknowledged successfully")
                                    Toast.makeText(context, "Purchase acknowledged (2/2)",
                                        Toast.LENGTH_SHORT).show()
                                    fragment.onPurchaseSuccess()
                                }
                            }
                        } else {
                            Log.e(TAG, "Failed to acknowledge purchase: $billingResult")
                            fragment.showFailedAckText(timeTaken.toInt())
                            handlePurchase(purchase, attempt+1)
                        }
                    }
                }
            }
        }
    }

    fun verifyPurchaseWithFirebase(purchaseToken: String,
                                   basePlanId: String, callback: (Boolean) -> Unit){
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
                        updateUserSubscriptionStatus(result, basePlanId, callback)
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

    private fun updateUserSubscriptionStatus(purchase: Map<String, Any>, basePlanId: String,
                                             callback: (Boolean) -> Unit) {

        if (basePlanId != purchase["basePlanId"]){callback(false)}
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return callback(false)
        val db = FirebaseFirestore.getInstance()
        val userSubscription = hashMapOf(
            "isPremium" to true,
            "basePlanId" to basePlanId,
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
                securePrefManager.storeString("basePlanId", basePlanId)
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

    fun queryPurchases() {
        Log.d(TAG, "Query Purchases Called")
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "PurchasesList: $purchasesList")
                billingUpdatesListener.onPurchasesUpdated(purchasesList)
            } else {
                Log.e(TAG, "Error querying purchases: ${billingResult.responseCode}")
            }
        }
    }

    private fun startServiceConnection(executeOnSuccess: () -> Unit) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing Service connected successfully")
                    executeOnSuccess()
                } else {
                    Log.e(TAG, "Billing Service setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.e(TAG, "Billing Service disconnected")
                // Implement retry logic to handle lost connections to Google Play.
                if (!billingClient.isReady) {
                    startServiceConnection { billingUpdatesListener.onBillingClientSetupFinished() }
                }
            }
        })
    }

    fun endConnection() {
        Log.e(TAG, "Connection ended")
        billingClient.endConnection()
    }

    companion object {
        private const val TAG = "BillingManager"
    }
}
