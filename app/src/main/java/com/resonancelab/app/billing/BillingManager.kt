package com.resonancelab.app.billing

import android.app.Activity
import android.content.Context
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.Offerings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Manages RevenueCat SDK lifecycle, entitlement verification ("pro_access"),
 * purchases, restore flows, and Judge/Demo debug bypass toggles.
 */
class BillingManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        const val ENTITLEMENT_PRO = "pro_access"
        // Replace with your RevenueCat public Google API key
        const val REVENUECAT_API_KEY = "goog_resonancelab_demo_api_key"
    }

    private val _isRevenueCatPro = MutableStateFlow(false)
    val isRevenueCatPro: StateFlow<Boolean> = _isRevenueCatPro

    // Judge / Demo debug bypass toggle for instant verification without credit card
    private val _debugBypassEnabled = MutableStateFlow(false)
    val debugBypassEnabled: StateFlow<Boolean> = _debugBypassEnabled

    // Combined Pro status (true if purchased OR debug bypass enabled)
    val isPro: StateFlow<Boolean> = combine(_isRevenueCatPro, _debugBypassEnabled) { rcPro, bypass ->
        rcPro || bypass
    }.stateIn(scope, SharingStarted.Eagerly, false)

    private val _offerings = MutableStateFlow<Offerings?>(null)
    val offerings: StateFlow<Offerings?> = _offerings

    private val _billingStatusMessage = MutableStateFlow<String?>(null)
    val billingStatusMessage: StateFlow<String?> = _billingStatusMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        initializeRevenueCat()
    }

    private fun initializeRevenueCat() {
        try {
            Purchases.logLevel = com.revenuecat.purchases.LogLevel.DEBUG
            Purchases.configure(
                PurchasesConfiguration.Builder(context, REVENUECAT_API_KEY)
                    .build()
            )

            // Listen for customer info updates
            Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { customerInfo ->
                updateEntitlements(customerInfo)
            }

            // Fetch current customer info and offerings
            refreshCustomerInfo()
            fetchOfferings()
        } catch (e: Exception) {
            e.printStackTrace()
            _billingStatusMessage.value = "RevenueCat demo mode active: ${e.message}"
        }
    }

    fun refreshCustomerInfo() {
        try {
            Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    updateEntitlements(customerInfo)
                }

                override fun onError(error: PurchasesError) {
                    _billingStatusMessage.value = "Customer info fetch: ${error.message}"
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun fetchOfferings() {
        try {
            _isLoading.value = true
            Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
                override fun onReceived(offerings: Offerings) {
                    _isLoading.value = false
                    _offerings.value = offerings
                }

                override fun onError(error: PurchasesError) {
                    _isLoading.value = false
                    _billingStatusMessage.value = "Offerings load notice: ${error.message}"
                }
            })
        } catch (e: Exception) {
            _isLoading.value = false
            e.printStackTrace()
        }
    }

    private fun updateEntitlements(customerInfo: CustomerInfo) {
        val hasPro = customerInfo.entitlements[ENTITLEMENT_PRO]?.isActive == true
        _isRevenueCatPro.value = hasPro
    }

    /**
     * Executes purchase flow for selected RevenueCat package.
     */
    fun purchasePackage(
        activity: Activity,
        rcPackage: Package,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _isLoading.value = true
        try {
            Purchases.sharedInstance.purchase(
                PurchaseParams.Builder(activity, rcPackage).build(),
                object : PurchaseCallback {
                    override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                        _isLoading.value = false
                        updateEntitlements(customerInfo)
                        onSuccess()
                    }

                    override fun onError(error: PurchasesError, userCancelled: Boolean) {
                        _isLoading.value = false
                        if (!userCancelled) {
                            onError(error.message)
                        }
                    }
                }
            )
        } catch (e: Exception) {
            _isLoading.value = false
            onError(e.message ?: "Purchase initialization failed")
        }
    }

    /**
     * Restores previous purchases.
     */
    fun restorePurchases(onSuccess: () -> Unit, onError: (String) -> Unit) {
        _isLoading.value = true
        try {
            Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    _isLoading.value = false
                    updateEntitlements(customerInfo)
                    if (customerInfo.entitlements[ENTITLEMENT_PRO]?.isActive == true) {
                        onSuccess()
                    } else {
                        onError("No active Pro entitlement found on this account")
                    }
                }

                override fun onError(error: PurchasesError) {
                    _isLoading.value = false
                    onError(error.message)
                }
            })
        } catch (e: Exception) {
            _isLoading.value = false
            onError(e.message ?: "Restore failed")
        }
    }

    /**
     * Toggles Judge/Demo debug bypass to verify Pro tier capabilities without a credit card.
     */
    fun setDebugBypass(enabled: Boolean) {
        _debugBypassEnabled.value = enabled
    }

    fun clearStatusMessage() {
        _billingStatusMessage.value = null
    }
}
