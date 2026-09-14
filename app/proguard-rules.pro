# ResonanceLab ProGuard Rules

# RevenueCat Purchases SDK
-keep class com.revenuecat.purchases.** { *; }
-dontwarn com.revenuecat.purchases.**

# RevenueCat Billing
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# Kotlinx Coroutines
-keepnames class kotlinx.coroutines.internal.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
