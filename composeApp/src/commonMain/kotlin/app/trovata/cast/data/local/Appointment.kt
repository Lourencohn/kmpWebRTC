package app.trovata.cast.data.local

import app.trovata.cast.data.auth.AuthUser
import app.trovata.cast.data.auth.Company
import app.trovata.cast.data.auth.sellerIdentityOf

enum class AppointmentStatus(val code: String) {
    Scheduled("scheduled"),
    Done("done"),
    Missed("missed"),
    Cancelled("cancelled"),
    ;

    companion object {
        fun fromCode(code: String): AppointmentStatus = entries.firstOrNull { it.code == code } ?: Scheduled
    }
}

enum class AppointmentPriority(val code: String) {
    Normal("normal"),
    High("high"),
    ;

    companion object {
        fun fromCode(code: String): AppointmentPriority = entries.firstOrNull { it.code == code } ?: Normal
    }
}

enum class AppointmentOutcome(val code: String) {
    OrderPlaced("order_placed"),
    FollowUp("follow_up"),
    NotInterested("not_interested"),
    ;

    companion object {
        fun fromCode(code: String?): AppointmentOutcome? = entries.firstOrNull { it.code == code }
    }
}

data class AppointmentOwner(val empresaSlug: String, val sellerId: String)

fun appointmentOwnerOf(user: AuthUser?, company: Company?): AppointmentOwner? {
    val slug = company?.slug?.takeIf { it.isNotBlank() } ?: return null
    return AppointmentOwner(empresaSlug = slug, sellerId = sellerIdentityOf(user, company).id)
}

data class LinkedOrder(val orderId: String, val totalCents: Long)

data class Appointment(
    val appointmentId: String,
    val empresaSlug: String,
    val sellerId: String,
    val clienteId: Long?,
    val clientName: String,
    val clientShop: String?,
    val clientDocument: String?,
    val clientEmail: String?,
    val clientPhone: String?,
    val startsAtMs: Long,
    val durationMinutes: Int,
    val objective: String?,
    val notes: String?,
    val priority: AppointmentPriority,
    val status: AppointmentStatus,
    val outcome: AppointmentOutcome?,
    val outcomeNotes: String?,
    val nextStep: String?,
    val prepDone: Set<String>,
    val carrinhoId: Long?,
    val sessionId: String?,
    val followUpOfId: String?,
    val rescheduleCount: Int,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val linkedOrder: LinkedOrder? = null,
) {
    val endsAtMs: Long get() = startsAtMs + durationMinutes * 60_000L
}
