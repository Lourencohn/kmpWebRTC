package app.trovata.cast.feature.account

import androidx.compose.ui.graphics.vector.ImageVector

data class AccountStat(val label: String, val value: String, val delta: String, val deltaPositive: Boolean)

data class AccountRow(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val pill: String? = null,
    val pillJade: Boolean = false,
)

data class SupportRow(val label: String, val icon: ImageVector, val value: String? = null)
