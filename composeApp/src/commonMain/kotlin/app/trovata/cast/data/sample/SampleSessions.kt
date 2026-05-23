package app.trovata.cast.data.sample

data class SessionClient(
    val id: String,
    val name: String,
    val shop: String,
    val city: String,
    val hue: Double,
    val previousSessions: Int,
)

enum class SessionTag(val label: String) {
    Reposicao("Reposição"),
    PrimeiraSessao("Primeira sessão"),
    TopVenda("Top venda"),
}

data class UpcomingSession(
    val id: String,
    val client: SessionClient,
    val time: String,
    val day: String,
    val itemsHint: String,
    val tag: SessionTag?,
)

data class LiveWaitingSession(
    val id: String,
    val client: SessionClient,
    val openedFor: String,
    val viewingHint: String,
)

enum class HistoryStatus(val label: String) {
    Fechado("fechado"),
    EmRevisao("em revisão"),
}

data class HistorySession(
    val id: String,
    val client: SessionClient,
    val total: String,
    val items: Int,
    val status: HistoryStatus,
)

data class SellerHomeData(
    val collectionEyebrow: String,
    val greetingTitle: String,
    val greetingSubtitle: String,
    val nowWaiting: LiveWaitingSession?,
    val today: List<UpcomingSession>,
    val thisWeek: List<UpcomingSession>,
    val history: List<HistorySession>,
)

data class SessionChecklistItem(
    val id: String,
    val label: String,
    val detail: String,
    val ready: Boolean,
)

data class SessionPrepData(
    val client: SessionClient,
    val scheduledFor: String,
    val itemsCount: Int,
    val skuCount: Int,
    val checklist: List<SessionChecklistItem>,
)

object SampleSessions {
    private val diego = SessionClient(
        id = "diego-trama",
        name = "Diego Albuquerque",
        shop = "Trama Multimarcas",
        city = "Belo Horizonte",
        hue = 28.0,
        previousSessions = 6,
    )

    private val renata = SessionClient(
        id = "renata-mare",
        name = "Renata Sá",
        shop = "Concept Maré",
        city = "Recife",
        hue = 280.0,
        previousSessions = 4,
    )

    private val paulo = SessionClient(
        id = "paulo-ph",
        name = "Paulo Henrique",
        shop = "Loja PH Atacadinho",
        city = "Goiânia",
        hue = 200.0,
        previousSessions = 0,
    )

    private val marcia = SessionClient(
        id = "marcia-studio",
        name = "Marcia Fontes",
        shop = "Studio Marcia",
        city = "Curitiba",
        hue = 340.0,
        previousSessions = 9,
    )

    private val luciana = SessionClient(
        id = "luciana-bossa",
        name = "Luciana Vidal",
        shop = "Bossa Loja",
        city = "Salvador",
        hue = 12.0,
        previousSessions = 11,
    )

    private val eduardo = SessionClient(
        id = "eduardo-eb",
        name = "Eduardo Bastos",
        shop = "EB Atacado",
        city = "Fortaleza",
        hue = 160.0,
        previousSessions = 3,
    )

    private val helena = SessionClient(
        id = "helena-quintal",
        name = "Helena Vargas",
        shop = "Quintal Conceito",
        city = "São Paulo",
        hue = 320.0,
        previousSessions = 2,
    )

    private val joao = SessionClient(
        id = "joao-vento",
        name = "João Mendes",
        shop = "Vento Sul",
        city = "Florianópolis",
        hue = 95.0,
        previousSessions = 1,
    )

    val home: SellerHomeData = SellerHomeData(
        collectionEyebrow = "Sessões",
        greetingTitle = "Sua agenda de hoje",
        greetingSubtitle = "Atelier Norte · Outono 26 · 3 hoje, 2 te chamando",
        nowWaiting = LiveWaitingSession(
            id = "live-diego",
            client = diego,
            openedFor = "Sala aberta há 1m 12s",
            viewingHint = "Ele está vendo o catálogo Outono 26",
        ),
        today = listOf(
            UpcomingSession(
                id = "up-renata",
                client = renata,
                time = "14:30",
                day = "hoje",
                itemsHint = "12 itens",
                tag = SessionTag.Reposicao,
            ),
            UpcomingSession(
                id = "up-paulo",
                client = paulo,
                time = "15:15",
                day = "hoje",
                itemsHint = "novo cliente",
                tag = SessionTag.PrimeiraSessao,
            ),
            UpcomingSession(
                id = "up-marcia",
                client = marcia,
                time = "16:45",
                day = "hoje",
                itemsHint = "8 itens",
                tag = null,
            ),
        ),
        thisWeek = listOf(
            UpcomingSession(
                id = "wk-helena",
                client = helena,
                time = "10:00",
                day = "qui",
                itemsHint = "preview Outono",
                tag = SessionTag.TopVenda,
            ),
            UpcomingSession(
                id = "wk-joao",
                client = joao,
                time = "11:30",
                day = "qui",
                itemsHint = "novo cliente",
                tag = SessionTag.PrimeiraSessao,
            ),
            UpcomingSession(
                id = "wk-diego2",
                client = diego,
                time = "09:15",
                day = "sex",
                itemsHint = "reposição rápida",
                tag = SessionTag.Reposicao,
            ),
        ),
        history = listOf(
            HistorySession(
                id = "h-luciana",
                client = luciana,
                total = "R$ 3.840,00",
                items = 14,
                status = HistoryStatus.Fechado,
            ),
            HistorySession(
                id = "h-eduardo",
                client = eduardo,
                total = "R$ 1.420,00",
                items = 6,
                status = HistoryStatus.EmRevisao,
            ),
        ),
    )

    val incoming: LiveWaitingSession = home.nowWaiting!!

    val prep: SessionPrepData = SessionPrepData(
        client = diego,
        scheduledFor = "Hoje · 14:00",
        itemsCount = 5,
        skuCount = 32,
        checklist = listOf(
            SessionChecklistItem(
                id = "catalogo",
                label = "Catálogo Outono 26",
                detail = "5 produtos · 32 SKUs prontos",
                ready = true,
            ),
            SessionChecklistItem(
                id = "audio",
                label = "Microfone testado",
                detail = "Áudio claro, sem eco",
                ready = true,
            ),
            SessionChecklistItem(
                id = "conexao",
                label = "Conexão estável",
                detail = "Wi-Fi 5GHz · 24 Mbps de upload",
                ready = true,
            ),
            SessionChecklistItem(
                id = "tabela",
                label = "Tabela de preços",
                detail = "Selecione antes de chamar",
                ready = false,
            ),
        ),
    )
}
