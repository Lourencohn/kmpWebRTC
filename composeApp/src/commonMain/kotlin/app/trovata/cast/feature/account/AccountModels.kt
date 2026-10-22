package app.trovata.cast.feature.account

enum class AccountIcon { Profile, Company, Document, Help, Terms, Privacy, Version }

data class AccountStat(val label: String, val value: String, val delta: String, val deltaPositive: Boolean)

data class AccountRow(
    val label: String,
    val value: String,
    val icon: AccountIcon,
    val pill: String? = null,
    val pillJade: Boolean = false,
)

data class SupportRow(val label: String, val icon: AccountIcon, val value: String? = null)
