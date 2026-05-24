package app.trovata.cast.data.sample

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.trovata.cast.ui.icons.TrovataIcons

data class BrandIdentity(
    val name: String,
    val tagline: String,
    val collection: String,
    val skuCount: Int,
    val colors: List<Color>,
)

data class AccountStat(val label: String, val value: String, val delta: String, val deltaPositive: Boolean)

data class AccountRow(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val pill: String? = null,
    val pillJade: Boolean = false,
)

data class SupportRow(val label: String, val icon: ImageVector, val value: String? = null)

object SampleAccount {
    const val displayName: String = "Camila Tavares"
    const val role: String = "Representante · Atelier Norte"
    const val email: String = "camila@ateliernorte.com.br"
    const val avatarHue: Double = 340.0
    const val activeClients: Int = 47
    const val monthsOnNetwork: Int = 14
    const val tierLabel: String = "Top 3%"

    val brand: BrandIdentity = BrandIdentity(
        name = "Atelier Norte",
        tagline = "Moda atacado · Coleções Outono 26, Inverno 25",
        collection = "47 SKUs ativos · tabela B (atacado)",
        skuCount = 47,
        colors = listOf(
            Color(0xFF3F4744),
            Color(0xFFA5806A),
            Color(0xFFC7B79B),
            Color(0xFF5E5B57),
            Color(0xFFE2D7B8),
        ),
    )

    val performance: List<AccountStat> = listOf(
        AccountStat("Vendas", "R$ 142k", "+18%", true),
        AccountStat("Sessões", "87", "+12", true),
        AccountStat("Conversão", "72%", "+4pp", true),
    )

    val account: List<AccountRow> = listOf(
        AccountRow("Perfil", "Camila Tavares · $email", TrovataIcons.user),
        AccountRow("Marcas representadas", "Atelier Norte", TrovataIcons.layers, pill = "+ adicionar"),
        AccountRow("Catálogos", "Outono 26 · Inverno 25", TrovataIcons.grid),
        AccountRow("Tabela de preços", "Tabela B · atacado", TrovataIcons.sliders),
        AccountRow("Sincronização ERP", "Conectado · Bling", TrovataIcons.signal, pill = "ativo", pillJade = true),
        AccountRow("Notificações", "WhatsApp + push", TrovataIcons.bell),
    )

    val support: List<SupportRow> = listOf(
        SupportRow("Central de ajuda", TrovataIcons.msg),
        SupportRow("Termos de uso", TrovataIcons.lock),
        SupportRow("Política de privacidade", TrovataIcons.eye),
        SupportRow("Versão", TrovataIcons.star, value = "1.2.0"),
    )

    val brandGradientTop: Color = Color(0xFF1F2521)
    val brandGradientBottom: Color = Color(0xFF161B17)
}
