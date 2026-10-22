package app.trovata.cast.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

private fun strokeIcon(name: String, pathData: String, strokeWidth: Float = 1.6f): ImageVector =
    ImageVector.Builder(
        name = "trovata.$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()

object TrovataIcons {
    val plus = strokeIcon("plus", "M12 5v14M5 12h14")
    val search = strokeIcon("search", "M11 19a8 8 0 1 1 0-16 8 8 0 0 1 0 16zm6-2l4 4")
    val back = strokeIcon("back", "M15 6l-6 6 6 6")
    val chev = strokeIcon("chev", "M9 6l6 6-6 6")
    val chevDown = strokeIcon("chevDown", "M6 9l6 6 6-6")
    val bell = strokeIcon("bell", "M6 8a6 6 0 0 1 12 0c0 7 3 8 3 8H3s3-1 3-8M10 21a2 2 0 0 0 4 0")
    val cart = strokeIcon("cart", "M3 5h2l2 12h12l2-9H7M9 21a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3zm10 0a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z")
    val mic = strokeIcon("mic", "M12 3a3 3 0 0 0-3 3v6a3 3 0 1 0 6 0V6a3 3 0 0 0-3-3zM5 11a7 7 0 0 0 14 0M12 18v3")
    val micOff = strokeIcon("micOff", "M9 9V6a3 3 0 0 1 5.5-1.7M15 12V9M3 3l18 18M5 11a7 7 0 0 0 11.7 5.1M9.3 18.5A7 7 0 0 0 12 19v2")
    val video = strokeIcon("video", "M3 7a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7zm13 3l5-3v10l-5-3")
    val hangup = strokeIcon("hangup", "M3 11c5-5 13-5 18 0l-2 2-3-1-1-2c-2-1-4-1-6 0l-1 2-3 1-2-2z")
    val pointer = strokeIcon("pointer", "M5 3l5 18 3-8 8-3z")
    val play = strokeIcon("play", "M7 4.5v15l12.5-7.5z")
    val pause = strokeIcon("pause", "M8 5v14M16 5v14", strokeWidth = 2.4f)
    val stop = strokeIcon("stop", "M6.5 6.5h11v11h-11z")
    val record = strokeIcon("record", "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 15.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z")
    val check = strokeIcon("check", "M4 12l5 5L20 6", strokeWidth = 1.8f)
    val share = strokeIcon("share", "M4 12v7a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-7M16 6l-4-4-4 4M12 2v14")
    val copy = strokeIcon("copy", "M9 9V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2h-4M5 9h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2z")
    val link = strokeIcon("link", "M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1")
    val send = strokeIcon("send", "M22 2L11 13M22 2l-7 20-4-9-9-4z")
    val user = strokeIcon("user", "M20 21a8 8 0 1 0-16 0M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8z")
    val users = strokeIcon("users", "M16 21a6 6 0 1 0-12 0M10 14a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM22 21a6 6 0 0 0-6-6m-2-7a4 4 0 1 0 8 0 4 4 0 0 0-8 0")
    val filter = strokeIcon("filter", "M3 5h18M6 12h12M10 19h4")
    val grid = strokeIcon("grid", "M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z")
    val trash = strokeIcon("trash", "M4 7h16M9 7V4h6v3M6 7l1 13a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-13")
    val clock = strokeIcon("clock", "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 7v5l3 2")
    val star = strokeIcon("star", "M12 2l3 6.5 7 .9-5 5 1.4 7L12 18l-6.4 3.4L7 14.4l-5-5 7-.9z")
    val layers = strokeIcon("layers", "M12 3l9 5-9 5-9-5 9-5zM3 13l9 5 9-5M3 18l9 5 9-5")
    val swatch = strokeIcon("swatch", "M3 3h8v8H3zM13 3h8v8h-8zM3 13h8v8H3zM13 13h8v8h-8z")
    val eye = strokeIcon("eye", "M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12zm10 3a3 3 0 1 0 0-6 3 3 0 0 0 0 6z")
    val msg = strokeIcon("msg", "M21 12a8 8 0 0 1-8 8H7l-4 3v-11a8 8 0 0 1 8-8h2a8 8 0 0 1 8 8z")
    val lock = strokeIcon("lock", "M6 11h12v9a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2v-9zm2 0V7a4 4 0 0 1 8 0v4")
    val sliders = strokeIcon("sliders", "M4 6h12M4 18h6M14 18h6M4 12h6M14 12h6M16 4v4M10 16v4M10 10v4")
    val arrowRight = strokeIcon("arrowRight", "M5 12h14M13 5l7 7-7 7")
    val signal = strokeIcon("signal", "M2 12h2M6 9v6M10 6v12M14 9v6M18 12h2")
    val refresh = strokeIcon("refresh", "M20 12a8 8 0 1 1-2.3-5.7M20 3v5h-5")
    val calendar = strokeIcon("calendar", "M5 5h14a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1zM4 10h16M8 3v4M16 3v4")
    val edit = strokeIcon("edit", "M4 20h4L19 9l-4-4L4 16v4zM13.5 6.5l4 4")
    val alert = strokeIcon("alert", "M12 4l9 16H3l9-16zM12 10v4M12 17h.01")
}
