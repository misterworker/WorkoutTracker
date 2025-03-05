/*
 * Copyright (c) 2024.
 * All rights reserved.
 * This file is part of the Workout Tracker App.
 * Unauthorized copying and distribution is prohibited.
 */

package com.workoutwrecker.workouttracker.ui.premium

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.workoutwrecker.workouttracker.R
import com.workoutwrecker.workouttracker.SecurePreferenceManager
import com.android.billingclient.api.Purchase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.workoutwrecker.workouttracker.databinding.FragmentPremiumPurchaseBinding

class PremiumPurchaseFragment : Fragment(), BillingManager.BillingUpdatesListener {

    private var _binding: FragmentPremiumPurchaseBinding? = null
    private val binding get() = _binding!! // This ensures that you use the non-null version of the binding
    private lateinit var viewPager: ViewPager2
    private lateinit var billingManager: BillingManager
    private lateinit var securePrefManager: SecurePreferenceManager
    private var selectedBasePlanId: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isReturning = false
    var isProcessingPurchase = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPremiumPurchaseBinding.inflate(inflater, container, false)

        securePrefManager = SecurePreferenceManager(requireContext())

        binding.closeButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.basicButton.setOnClickListener {
            selectedBasePlanId = "basic"
            billingManager.initiatePurchase(requireActivity(), "premium", "basic")
        }

        binding.fitButton.setOnClickListener {
            selectedBasePlanId = "fit"
            billingManager.initiatePurchase(requireActivity(), "premium", "fit")
        }

        binding.jackedButton.setOnClickListener {
            selectedBasePlanId = "jacked"
            billingManager.initiatePurchase(requireActivity(), "premium_2", "jacked")
        }

        binding.eliteButton.setOnClickListener {
            selectedBasePlanId = "elite"
            billingManager.initiatePurchase(requireActivity(), "premium_2", "elite")
        }

        updateButtonsBasedOnSubscription()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!isProcessingPurchase) {
                    // Only handle back press if we are on the premium_container_fragment
                    if (parentFragmentManager.findFragmentByTag("premium_container_fragment") != null) {
                        parentFragmentManager.popBackStack(
                            "premium_container_fragment",
                            FragmentManager.POP_BACK_STACK_INCLUSIVE
                        )
                        // Optionally pop one more stack if needed
                        parentFragmentManager.popBackStack()
                    } else {
                        // Let the system handle back press
                        isEnabled = false // This will disable this callback only
                        requireActivity().onBackPressed() // Forward to the default behavior
                    }
                }
            }
        })
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        billingManager = BillingManager(requireContext(), this, this)
        viewPager = requireActivity().findViewById(R.id.premium_viewpager)
    }

    private fun updateButtonsBasedOnSubscription() {
        val basePlanId = securePrefManager.getString("basePlanId")
        val paymentState = securePrefManager.getString("paymentState")

        Log.d(TAG, "Base Plan ID: $basePlanId\nPayment State: $paymentState")

        if (paymentState == "SUBSCRIPTION_STATE_ACTIVE") {
            when (basePlanId) {
                "basic" -> { binding.basicButton.text = getString(R.string.activated)
                    binding.fitButton.text = getString(R.string.tier_1_biannual_price)
                    binding.basicButton.isEnabled = false
                    binding.fitButton.isEnabled = true
                }
                "fit" -> {
                    binding.fitButton.text = getString(R.string.activated)
                    binding.basicButton.text = getString(R.string.tier_1_monthly_price)
                    binding.basicButton.isEnabled = true
                    binding.fitButton.isEnabled = false
                }
                "jacked" -> {
                    binding.jackedButton.text = getString(R.string.activated)
                    binding.basicButton.isEnabled = false
                    binding.fitButton.isEnabled = false
                    binding.basicButton.text = getString(R.string.tier_2_purchased)
                    binding.fitButton.text = getString(R.string.tier_2_purchased)
                    binding.eliteButton.text = getString(R.string.tier_2_biannual_price)
                    binding.jackedButton.isEnabled = false
                    binding.eliteButton.isEnabled = true
                }
                "elite" -> {
                    binding.eliteButton.text = getString(R.string.activated)
                    binding.basicButton.isEnabled = false
                    binding.basicButton.text = getString(R.string.tier_2_purchased)
                    binding.fitButton.text = getString(R.string.tier_2_purchased)
                    binding.fitButton.isEnabled = false
                    binding.jackedButton.text = getString(R.string.tier_2_monthly_price)
                    binding.jackedButton.isEnabled = true
                    binding.eliteButton.isEnabled = false
                }
            }
        } else if (paymentState == "SUBSCRIPTION_STATE_PENDING") {
            when (basePlanId) {
                "basic" -> binding.basicButton.text = getString(R.string.pending)
                "fit" -> binding.fitButton.text = getString(R.string.pending)
                "jacked" -> binding.jackedButton.text = getString(R.string.pending)
                "elite" -> binding.eliteButton.text = getString(R.string.pending)
            }
        }
    }

    override fun onBillingClientSetupFinished() {
        Log.d(TAG, "Billing client setup finished")
    }

    override fun onPurchasesUpdated(purchases: List<Purchase>) {
        showLoadingOverlay()

        for (purchase in purchases) {
            if (purchase.isAcknowledged) { //Google Play auto disallows >1 unacknowledged purchase
                continue
            }
            if (selectedBasePlanId == null){
                Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT).show()
                continue
            }
            Log.d(TAG, "On purchases updated: $purchase")

            val productIds = purchase.products
            if (productIds.isEmpty()) {continue}

            val productId = productIds[0]

            // Log the product ID
            Log.d(TAG, "Product ID: $productId")
            Log.d(TAG, "Selected Base Plan ID: $selectedBasePlanId")

            val purchaseToken = purchase.purchaseToken
            securePrefManager.storeString("purchaseToken", purchaseToken)
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: continue
            val db = FirebaseFirestore.getInstance()
            val updateData = mapOf(
                "purchaseToken" to purchaseToken
            )
            db.collection("users").document(userId)
                .set(updateData, SetOptions.merge())

            billingManager.verifyPurchaseWithFirebase(purchaseToken,
                selectedBasePlanId.toString()) { success->
                updateButtonsBasedOnSubscription()
                if (!success) {
                    Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT).show()
                    return@verifyPurchaseWithFirebase
                }
                Toast.makeText(requireContext(), "Purchase verified (1/2)", Toast.LENGTH_SHORT).show()
                billingManager.handlePurchase(purchase, 1)
            }
        }
    }

    override fun onPurchaseCancelled() {
        hideLoadingOverlay()
        // Handle purchase cancellation
        Log.d(TAG, "Purchase was cancelled")
        Toast.makeText(requireContext(), "Purchase cancelled", Toast.LENGTH_SHORT).show()
    }

    override fun onPurchaseError(errorCode: Int) {
        hideLoadingOverlay()
        // Handle purchase error
        Log.e(TAG, "Error during purchase: $errorCode")
        Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT).show()
    }

    fun onPurchaseSuccess() {
        Log.d("Success", "OnPurchaseSuccess")
        hideLoadingOverlay()
    }

    fun showFailedAckText(retryTime: Int) {
        binding.failedAckText.visibility = View.VISIBLE
        binding.failedAckText.text = getString(R.string.failed_ack, retryTime)
    }

    private fun showLoadingOverlay() {
        if (isProcessingPurchase) {
            setViewPagerSwipingEnabled(false)
            Log.d("Show", "Showloading")
            disableAllViews(binding.root)
            binding.loadingOverlay.visibility = View.VISIBLE
        }
    }

    private fun hideLoadingOverlay() {
        setViewPagerSwipingEnabled(true)
        enableAllViews(binding.root)
        binding.loadingOverlay.visibility = View.GONE
        binding.failedAckText.visibility = View.GONE
        isProcessingPurchase = false
    }

    private fun setViewPagerSwipingEnabled(isEnabled: Boolean) {
        viewPager.isUserInputEnabled = isEnabled
    }

    private fun disableAllViews(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child.id == R.id.loading_overlay || child.id == R.id.progress_bar){ continue }
            else if (child is ViewGroup) {
                disableAllViews(child)
            } else {
                child.isEnabled = false
            }
        }
    }

    private fun enableAllViews(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is ViewGroup) {
                enableAllViews(child) // Recursively enable child ViewGroups
            } else {
                child.isEnabled = true // Enable the view here
            }
        }
    }

    override fun onPause() {
        super.onPause()
        isReturning = true
    }

//    override fun onResume() {
//        super.onResume()
//        if (isReturning && isProcessingPurchase) {
//            billingManager.queryPurchases()
//        }
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null // Clean up the binding reference
        if (::billingManager.isInitialized) {
            billingManager.endConnection()
        }
    }

    companion object {
        private const val TAG = "PremiumPurchaseFragment"
    }
}
