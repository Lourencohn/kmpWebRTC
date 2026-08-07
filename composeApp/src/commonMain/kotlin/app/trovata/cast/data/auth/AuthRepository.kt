package app.trovata.cast.data.auth

import app.trovata.cast.data.remote.sfa.AccountApi
import app.trovata.cast.data.remote.sfa.KeycloakAuthService
import app.trovata.cast.data.remote.sfa.SfaApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

class AuthRepository(
    private val store: AuthStore,
    private val keycloak: KeycloakAuthService,
    private val account: AccountApi,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val refreshMutex = Mutex()

    private var tokens: AuthTokens? = null
    private var selectedEmpresaId: Long? = null

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _user = MutableStateFlow<AuthUser?>(null)
    val user: StateFlow<AuthUser?> = _user.asStateFlow()

    private val _companies = MutableStateFlow<List<Company>>(emptyList())
    val companies: StateFlow<List<Company>> = _companies.asStateFlow()

    private val _activeCompany = MutableStateFlow<Company?>(null)
    val activeCompany: StateFlow<Company?> = _activeCompany.asStateFlow()

    init {
        tokens = readTokens()
        selectedEmpresaId = store.get(KEY_EMPRESA)?.toLongOrNull()
        _user.value = readUser()
        _activeCompany.value = readActiveCompany()
        _isAuthenticated.value = hasUsableSession() && selectedEmpresaId != null
    }

    suspend fun login(username: String, password: String): LoginResult {
        val tokenResult = keycloak.login(username.trim(), password)
        val newTokens = when (tokenResult) {
            is SfaApiResult.Ok -> tokenResult.value
            is SfaApiResult.Fail -> return LoginResult.Failure(tokenResult.message)
        }
        persistTokens(newTokens)
        tokens = newTokens

        val userResult = account.loggedUser(newTokens.accessToken)
        val user = (userResult as? SfaApiResult.Ok)?.value
        _user.value = user
        persistUser(user)

        val resolved = resolveCompanies(newTokens.accessToken, user?.id)
        _companies.value = resolved

        return when {
            resolved.isEmpty() -> LoginResult.Failure("Nenhuma empresa disponível para este usuário")
            resolved.size == 1 -> {
                selectCompany(resolved.first().id)
                LoginResult.Success(needsCompanySelection = false)
            }
            else -> LoginResult.Success(needsCompanySelection = true)
        }
    }

    fun selectCompany(empresaId: Long) {
        selectedEmpresaId = empresaId
        store.put(KEY_EMPRESA, empresaId.toString())
        val company = _companies.value.firstOrNull { it.id == empresaId }
        _activeCompany.value = company
        persistActiveCompany(company)
        _isAuthenticated.value = hasUsableSession()
    }

    fun empresaId(): Long? = selectedEmpresaId

    suspend fun accessToken(): String? = refreshMutex.withLock {
        val current = tokens ?: return null
        val now = nowMs()
        if (now < current.accessExpiresAtMs - SKEW_MS) return current.accessToken
        if (now < current.refreshExpiresAtMs - SKEW_MS) {
            when (val refreshed = keycloak.refresh(current.refreshToken)) {
                is SfaApiResult.Ok -> {
                    persistTokens(refreshed.value)
                    tokens = refreshed.value
                    return refreshed.value.accessToken
                }
                is SfaApiResult.Fail -> { clearSession(); return null }
            }
        }
        clearSession()
        return null
    }

    fun logout() = clearSession()

    private fun clearSession() {
        tokens = null
        selectedEmpresaId = null
        _user.value = null
        _companies.value = emptyList()
        _activeCompany.value = null
        for (key in ALL_KEYS) store.put(key, null)
        _isAuthenticated.value = false
    }

    private suspend fun resolveCompanies(token: String, userId: Long?): List<Company> {
        val all = (account.companies(token) as? SfaApiResult.Ok)?.value ?: emptyList()
        val allowed = userId?.let { (account.userCompanyIds(token, it) as? SfaApiResult.Ok)?.value } ?: emptySet()
        return if (allowed.isEmpty()) all else all.filter { it.id in allowed }
    }

    private fun hasUsableSession(): Boolean {
        val current = tokens ?: return false
        return nowMs() < current.refreshExpiresAtMs - SKEW_MS && selectedEmpresaId != null
    }

    private fun persistTokens(t: AuthTokens) {
        store.put(KEY_ACCESS, t.accessToken)
        store.put(KEY_REFRESH, t.refreshToken)
        store.put(KEY_ACCESS_EXP, t.accessExpiresAtMs.toString())
        store.put(KEY_REFRESH_EXP, t.refreshExpiresAtMs.toString())
    }

    private fun readTokens(): AuthTokens? {
        val access = store.get(KEY_ACCESS) ?: return null
        val refresh = store.get(KEY_REFRESH) ?: return null
        return AuthTokens(
            accessToken = access,
            refreshToken = refresh,
            accessExpiresAtMs = store.get(KEY_ACCESS_EXP)?.toLongOrNull() ?: 0,
            refreshExpiresAtMs = store.get(KEY_REFRESH_EXP)?.toLongOrNull() ?: 0,
        )
    }

    private fun persistUser(user: AuthUser?) {
        store.put(KEY_USER_ID, user?.id?.toString())
        store.put(KEY_USER_NAME, user?.name)
        store.put(KEY_USER_EMAIL, user?.email)
        store.put(KEY_USER_AVATAR, user?.avatarUrl)
    }

    private fun readUser(): AuthUser? {
        val id = store.get(KEY_USER_ID)?.toLongOrNull() ?: return null
        return AuthUser(
            id = id,
            name = store.get(KEY_USER_NAME),
            email = store.get(KEY_USER_EMAIL),
            avatarUrl = store.get(KEY_USER_AVATAR),
            whatsapp = null,
        )
    }

    private fun persistActiveCompany(company: Company?) {
        store.put(KEY_COMPANY_SLUG, company?.slug)
        store.put(KEY_COMPANY_NAME, company?.name)
        store.put(KEY_COMPANY_LEGAL, company?.legalName)
        store.put(KEY_COMPANY_CNPJ, company?.cnpj)
        store.put(KEY_COMPANY_LOGO, company?.logoUrl)
    }

    private fun readActiveCompany(): Company? {
        val id = selectedEmpresaId ?: return null
        val name = store.get(KEY_COMPANY_NAME) ?: return null
        return Company(
            id = id,
            slug = store.get(KEY_COMPANY_SLUG).orEmpty(),
            name = name,
            legalName = store.get(KEY_COMPANY_LEGAL),
            cnpj = store.get(KEY_COMPANY_CNPJ),
            logoUrl = store.get(KEY_COMPANY_LOGO),
        )
    }

    private companion object {
        const val SKEW_MS = 30_000L
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_ACCESS_EXP = "access_expires_at"
        const val KEY_REFRESH_EXP = "refresh_expires_at"
        const val KEY_EMPRESA = "empresa_id"
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_USER_AVATAR = "user_avatar"
        const val KEY_COMPANY_SLUG = "company_slug"
        const val KEY_COMPANY_NAME = "company_name"
        const val KEY_COMPANY_LEGAL = "company_legal"
        const val KEY_COMPANY_CNPJ = "company_cnpj"
        const val KEY_COMPANY_LOGO = "company_logo"
        val ALL_KEYS = listOf(
            KEY_ACCESS, KEY_REFRESH, KEY_ACCESS_EXP, KEY_REFRESH_EXP, KEY_EMPRESA,
            KEY_USER_ID, KEY_USER_NAME, KEY_USER_EMAIL, KEY_USER_AVATAR,
            KEY_COMPANY_SLUG, KEY_COMPANY_NAME, KEY_COMPANY_LEGAL, KEY_COMPANY_CNPJ, KEY_COMPANY_LOGO,
        )
    }
}
