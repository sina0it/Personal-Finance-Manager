package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object CategoryIconHelper {

    fun getIcon(name: String): ImageVector {
        return when (name.lowercase()) {
            "restaurant", "food" -> Icons.Default.Restaurant
            "shopping_bag", "shop" -> Icons.Default.ShoppingBag
            "directions_car", "transport" -> Icons.Default.DirectionsCar
            "local_gas_station", "fuel" -> Icons.Default.LocalGasStation
            "receipt_long", "bills" -> Icons.AutoMirrored.Filled.ReceiptLong
            "home", "rent" -> Icons.Default.Home
            "sports_esports", "entertainment" -> Icons.Default.SportsEsports
            "local_hospital", "health" -> Icons.Default.LocalHospital
            "school", "education" -> Icons.Default.School
            "flight", "travel" -> Icons.Default.Flight
            "subscriptions" -> Icons.Default.Subscriptions
            "payments", "salary" -> Icons.Default.Payments
            "laptop", "freelance" -> Icons.Default.Laptop
            "storefront", "business" -> Icons.Default.Storefront
            "trending_up", "investment" -> Icons.AutoMirrored.Filled.TrendingUp
            "card_giftcard", "gift" -> Icons.Default.CardGiftcard
            "account_balance", "bank" -> Icons.Default.AccountBalance
            "credit_card", "card" -> Icons.Default.CreditCard
            "account_balance_wallet", "wallet", "cash" -> Icons.Default.AccountBalanceWallet
            else -> Icons.Default.Category
        }
    }

    fun parseColor(hex: String, fallback: Color = Color(0xFF00C896)): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            fallback
        }
    }
}

@Composable
fun CategoryAvatar(
    iconName: String,
    colorHex: String,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp,
    modifier: Modifier = Modifier
) {
    val color = CategoryIconHelper.parseColor(colorHex)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = CategoryIconHelper.getIcon(iconName),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(iconSize)
        )
    }
}
