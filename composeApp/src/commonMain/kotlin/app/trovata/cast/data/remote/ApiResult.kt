package app.trovata.cast.data.remote

sealed class ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>()
    data class Fail(val code: String, val message: String, val status: Int) : ApiResult<Nothing>()
}
