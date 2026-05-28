package app.trovata.cast.data.remote.sfa.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoggedUserDto(
    val id: Long,
    val nome: String? = null,
    @SerialName("email_login") val emailLogin: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val whatsapp: String? = null,
)

@Serializable
data class EmpresaUsuarioDto(
    val id: Long,
    @SerialName("usuario_id") val usuarioId: Long? = null,
    @SerialName("empresa_id") val empresaId: Long? = null,
    val situacao: String? = null,
)

@Serializable
data class CompanyDto(
    val id: Long,
    @SerialName("nome_fantasia") val nomeFantasia: String? = null,
    @SerialName("razao_social") val razaoSocial: String? = null,
    val cnpj: String? = null,
    @SerialName("imagem_logotipo") val imagemLogotipo: String? = null,
)
