package app.trovata.cast.data.sample

enum class ClientSegment { Recentes, Top, Atencao, Novos }

enum class ClientTag { Novo, TopVenda }

data class Client(
    val name: String,
    val shop: String,
    val city: String,
    val hue: Double,
    val ltv: String,
    val lastSession: String,
    val activity: List<Float>,
    val live: Boolean = false,
    val tag: ClientTag? = null,
)

data class ReadyToApproach(
    val name: String,
    val shop: String,
    val hue: Double,
    val sinceLabel: String,
    val urgency: ReadyUrgency,
)

enum class ReadyUrgency { Urgente, Morno }

object SampleClients {
    val readyToApproach: List<ReadyToApproach> = listOf(
        ReadyToApproach("Luciana Vidal", "Bossa Loja · Porto Alegre", 320.0, "sem contato há 41d", ReadyUrgency.Urgente),
        ReadyToApproach("Renata Sá", "Concept Maré · Recife", 280.0, "sem contato há 23d", ReadyUrgency.Morno),
    )

    val recent: List<Client> = listOf(
        Client(
            name = "Diego Albuquerque",
            shop = "Trama Multimarcas",
            city = "BH",
            hue = 28.0,
            ltv = "R$ 64,8k",
            lastSession = "agora",
            activity = listOf(2f, 3f, 1f, 4f, 3f, 5f, 6f),
            live = true,
        ),
        Client(
            name = "Paulo Henrique",
            shop = "PH Atacadinho",
            city = "Goiânia",
            hue = 200.0,
            ltv = "R$ 8,2k",
            lastSession = "há 1h",
            activity = listOf(0f, 1f, 2f, 3f, 4f, 3f, 5f),
            tag = ClientTag.Novo,
        ),
        Client(
            name = "Marcia Fontes",
            shop = "Studio Marcia",
            city = "Curitiba",
            hue = 340.0,
            ltv = "R$ 52,0k",
            lastSession = "ontem",
            activity = listOf(4f, 3f, 5f, 4f, 6f, 5f, 4f),
        ),
    )

    val month: List<Client> = listOf(
        Client(
            name = "Eduardo Bastos",
            shop = "EB Atacado",
            city = "Salvador",
            hue = 160.0,
            ltv = "R$ 19,3k",
            lastSession = "há 6d",
            activity = listOf(3f, 3f, 2f, 3f, 2f, 2f, 3f),
        ),
        Client(
            name = "Tiago Brandão",
            shop = "Brandão Multimar",
            city = "São Luís",
            hue = 180.0,
            ltv = "R$ 14,7k",
            lastSession = "há 11d",
            activity = listOf(2f, 4f, 3f, 2f, 1f, 2f, 2f),
        ),
        Client(
            name = "Camila Rocha",
            shop = "Saudade Boutique",
            city = "Niterói",
            hue = 14.0,
            ltv = "R$ 41,6k",
            lastSession = "há 18d",
            activity = listOf(5f, 3f, 4f, 2f, 3f, 2f, 1f),
            tag = ClientTag.TopVenda,
        ),
    )

    data class SegmentCount(val segment: ClientSegment, val label: String, val count: Int)

    val segments: List<SegmentCount> = listOf(
        SegmentCount(ClientSegment.Recentes, "Recentes", 12),
        SegmentCount(ClientSegment.Top, "Top venda", 8),
        SegmentCount(ClientSegment.Atencao, "Atenção", 5),
        SegmentCount(ClientSegment.Novos, "Novos", 3),
    )
}
