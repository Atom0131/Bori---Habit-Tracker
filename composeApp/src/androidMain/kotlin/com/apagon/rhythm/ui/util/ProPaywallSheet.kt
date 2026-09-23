package com.apagon.rhythm.ui.util

import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.billingclient.api.ProductDetails
import com.apagon.rhythm.data.billing.BillingRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProPaywallSheet(
    reason: String? = null,
    onDismiss: () -> Unit,
    onUpgrade: (productId: String) -> Unit,
    paywallViewModel: PaywallViewModel = koinViewModel()
) {
    val productDetails by paywallViewModel.productDetails.collectAsState()
    var selectedProductId by remember { mutableStateOf(BillingRepository.PRO_ANNUAL_ID) }

    val monthly = productDetails[BillingRepository.PRO_MONTHLY_ID]
    val annual = productDetails[BillingRepository.PRO_ANNUAL_ID]
    val pricesLoaded = monthly != null && annual != null
    val savingsPercent = remember(monthly, annual) { computeAnnualSavingsPercent(monthly, annual) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                "Upgrade to Pro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            if (reason != null) {
                Text(
                    reason,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                "Get the most out of Habit V3 with our premium features.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            ProFeatureItem("Unlimited Habits (Free: 7)")
            ProFeatureItem("Unlimited To-Do's (Free: 10)")
            ProFeatureItem("Unlimited Notebooks (Free: 3)")
            ProFeatureItem("Exclusive Custom Color Wheel")
            ProFeatureItem("Password & Biometric Journal Locks")
            ProFeatureItem("Unlimited Journal Entries (Free: 3/day)")
            ProFeatureItem("Unlimited Alarms & Reminders (Free: 3 each)")
            ProFeatureItem("Exclusive Pomodoro Focus Timers")

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlanOptionCard(
                    label = "Monthly",
                    priceText = monthly?.formattedPrice()?.let { "$it/mo" } ?: "…",
                    badge = null,
                    isSelected = selectedProductId == BillingRepository.PRO_MONTHLY_ID,
                    onClick = { selectedProductId = BillingRepository.PRO_MONTHLY_ID },
                    modifier = Modifier.weight(1f)
                )
                PlanOptionCard(
                    label = "Annual",
                    priceText = annual?.formattedPrice()?.let { "$it/yr" } ?: "…",
                    badge = savingsPercent?.let { "Save $it%" },
                    isSelected = selectedProductId == BillingRepository.PRO_ANNUAL_ID,
                    onClick = { selectedProductId = BillingRepository.PRO_ANNUAL_ID },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { onUpgrade(selectedProductId) },
                enabled = pricesLoaded,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (pricesLoaded) "Upgrade to Pro" else "Loading prices…", fontWeight = FontWeight.Bold)
            }

            TextButton(onClick = onDismiss) {
                Text("Maybe Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PlanOptionCard(
    label: String,
    priceText: String,
    badge: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        } else {
            Spacer(Modifier.height(18.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
        Text(
            priceText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProFeatureItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun ProductDetails.formattedPrice(): String? =
    subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice

private fun ProductDetails.priceAmountMicros(): Long? =
    subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.priceAmountMicros

/** Computes "Save X%" for the annual plan vs. paying monthly for a year, using raw price-micros
 * (currency-agnostic) rather than trying to parse the localized [formattedPrice] strings. */
private fun computeAnnualSavingsPercent(monthly: ProductDetails?, annual: ProductDetails?): Int? {
    val monthlyMicros = monthly?.priceAmountMicros() ?: return null
    val annualMicros = annual?.priceAmountMicros() ?: return null
    if (monthlyMicros <= 0) return null
    val yearlyIfMonthly = monthlyMicros * 12
    if (yearlyIfMonthly <= annualMicros) return null
    val savings = ((yearlyIfMonthly - annualMicros) * 100 / yearlyIfMonthly).toInt()
    return savings.takeIf { it > 0 }
}
