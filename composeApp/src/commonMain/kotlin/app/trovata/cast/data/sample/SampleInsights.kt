package app.trovata.cast.data.sample

import androidx.compose.ui.graphics.Color

enum class InsightTrend { Up, Flat, Down }

data class KpiCard(
    val label: String,
    val value: String,
    val delta: String,
    val spark: List<Float>,
    val trend: InsightTrend,
)

data class FocusEntry(
    val productName: String,
    val timeInFocus: String,
    val conversion: Int,
    val accent: FocusAccent,
)

enum class FocusAccent { Jade, Brand, Warn, Muted }

data class FunnelStep(
    val label: String,
    val count: Int,
    val percent: Int,
)

object SampleInsights {
    val revenueSpark: List<Float> = listOf(62f, 78f, 71f, 88f, 96f, 84f, 102f, 118f, 142f)

    val kpis: List<KpiCard> = listOf(
        KpiCard("Sessões", "87", "+12 vs mês anterior", listOf(4f, 6f, 5f, 7f, 6f, 8f, 9f), InsightTrend.Up),
        KpiCard("Conversão", "72%", "↑ 4pp · acima da meta", listOf(60f, 64f, 62f, 68f, 70f, 72f, 72f), InsightTrend.Up),
        KpiCard("Ticket médio", "R$ 4,2k", "+R$ 280 vs ago", listOf(3.5f, 3.6f, 3.8f, 4.0f, 3.9f, 4.1f, 4.2f), InsightTrend.Up),
        KpiCard("Tempo em call", "14m", "−2m · mais objetivo", listOf(18f, 17f, 16f, 15f, 14f, 14f, 14f), InsightTrend.Down),
    )

    val focus: List<FocusEntry> = listOf(
        FocusEntry("Bolsa Elegance Monograma", "2m 14s", 74, FocusAccent.Jade),
        FocusEntry("Bolsa New Cristal Nude", "1m 48s", 52, FocusAccent.Brand),
        FocusEntry("Bolsa Dumond Shopping", "1m 22s", 38, FocusAccent.Brand),
        FocusEntry("Bolsa Carbono Trama", "3m 02s", 12, FocusAccent.Warn),
        FocusEntry("Bolsa Cute Bear Bege", "0m 54s", 8, FocusAccent.Muted),
    )

    val funnel: List<FunnelStep> = listOf(
        FunnelStep("Sessões agendadas", 95, 100),
        FunnelStep("Realizadas", 87, 92),
        FunnelStep("Com pedido", 63, 66),
        FunnelStep("Repetiram em ≤30d", 41, 43),
    )

    const val insightText: String =
        "Bolsa Elegance Monograma está convertendo 74% das vezes que aparece. " +
            "Em 9 das 11 últimas sessões, foi a primeira que o cliente abriu sozinho. " +
            "Considere abrir a próxima call por ela."

    data class Period(val label: String, val active: Boolean)

    val periods: List<Period> = listOf(
        Period("Semana", false),
        Period("Mês", true),
        Period("Trimestre", false),
        Period("Coleção", false),
    )

    val headerNumberMain: String = "R$ 142,38"
    val headerNumberSuffix: String = "k"
    val headerDelta: String = "↑ +18,2%"
    val headerCompare: String = "vs ago/25 · R$ 120,4k"

    data class HeroStat(val label: String, val value: String, val sub: String?)

    val heroStats: List<HeroStat> = listOf(
        HeroStat("Meta mês", "R$ 160k", "89%"),
        HeroStat("Faltam", "7 dias", null),
        HeroStat("Projeção", "R$ 168k", "+5%"),
    )
}

object InsightsHeroPalette {
    val gradientTop: Color = Color(0xFF0A1429)
    val gradientBottom: Color = Color(0xFF050C1C)
    val accent: Color = Color(0xFF3DDB8A)
}
