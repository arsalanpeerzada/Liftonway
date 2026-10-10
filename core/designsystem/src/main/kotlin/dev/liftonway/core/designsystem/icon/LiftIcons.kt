package dev.liftonway.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Thin outline icons to match the designs, drawn on a 24-unit grid. Strokes are black so `Icon`'s tint
 * recolours them.
 */
object LiftIcons {
    val ArrowBack = line("arrow_back", "M20 12H4", "M10 6l-6 6 6 6")
    val ArrowForward = line("arrow_forward", "M4 12h16", "M14 6l6 6-6 6", width = 2.2f)
    val Close = line("close", "M18 6L6 18", "M6 6l12 12")
    val Search = line("search", circle(11f, 11f, 6.5f), "M20 20l-4.2-4.2")
    val Calendar = line("calendar", roundRect(3.5f, 5f, 17f, 15.5f, 2.5f), "M3.5 10h17", "M8 3v4", "M16 3v4")
    val Person = line("person", circle(12f, 8f, 4f), "M4.5 20.5c0-4 3.4-6.5 7.5-6.5s7.5 2.5 7.5 6.5")
    val Clock = line("clock", circle(12f, 12f, 8.5f), "M12 7.5V12l3 2")
    val Car = line(
        "car",
        "M5 16.5H3.8c-0.5 0-0.8-0.4-0.8-0.8v-3.4c0-0.7 0.3-1.3 0.8-1.7L5.6 7.6C6 7 6.6 6.7 7.3 6.7h7.9c0.6 0 1.2 0.3 1.6 0.7l2.7 3.1 1.8 0.6c0.6 0.2 1 0.8 1 1.4v3.2c0 0.5-0.4 0.8-0.8 0.8H19",
        "M9.5 16.5h5",
        circle(7.2f, 16.5f, 2f),
        circle(16.8f, 16.5f, 2f),
        "M4 11h15",
    )
    val ChevronRight = line("chevron_right", "M9 5l7 7-7 7")
    val ChevronDown = line("chevron_down", "M5 9l7 7 7-7")
    val Sliders = line("sliders", "M4 8h9", "M11 16h9", circle(16f, 8f, 2.5f), circle(7f, 16f, 2.5f))
    val Home = line("home", "M4 10.5L12 4l8 6.5V19a1 1 0 0 1-1 1h-4.5v-5.5h-5V20H5a1 1 0 0 1-1-1z")
    val Trips = line("trips", "M4.6 18.5A8.5 8.5 0 1 1 19.4 18.5", "M12 13.5l3.5-3.5", circle(12f, 14f, 1f))
    val Bell = line("bell", "M6 16V10.5a6 6 0 0 1 12 0V16l1.5 2h-15z", "M10 20.5a2.2 2.2 0 0 0 4 0")
    val Profile = line("profile", circle(12f, 12f, 8.5f), circle(12f, 10f, 3f), "M7 18.2c1.1-1.9 2.9-2.9 5-2.9s3.9 1 5 2.9")
    val Camera = line(
        "camera",
        "M4 8.5A1.5 1.5 0 0 1 5.5 7h2.3l1.4-2h5.6l1.4 2h2.3A1.5 1.5 0 0 1 20 8.5v9a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 17.5z",
        circle(12f, 12.5f, 3.2f),
    )
    val CameraAdd = line(
        "camera_add",
        "M14 6h-1.3l-1.4-2H7.7L6.3 6H4.5A1.5 1.5 0 0 0 3 7.5v9A1.5 1.5 0 0 0 4.5 18h12a1.5 1.5 0 0 0 1.5-1.5V11",
        circle(10.5f, 11.5f, 3f),
        "M19 3v5",
        "M16.5 5.5h5",
    )
    val Check = line("check", "M5 12.5l4.5 4.5L19 7.5", width = 2.2f)
    val Plus = line("plus", "M12 5v14", "M5 12h14")
    val ArrowDownRight = line("arrow_down_right", "M7 7l10 10", "M17 9v8H9", width = 2.4f)
    val ArrowUpLeft = line("arrow_up_left", "M17 17L7 7", "M7 15V7h8", width = 2.4f)
    val Banknote = line("banknote", roundRect(3f, 6.5f, 18f, 11f, 1.5f), circle(12f, 12f, 2.3f), "M6.5 12h0.01", "M17.5 12h0.01", width = 2f)
    val Card = line("card", roundRect(3f, 5.5f, 18f, 13f, 2f), "M3 10h18", "M7 15h3")
    val Trash = line("trash", "M4.5 7h15", "M9.5 7V4.5h5V7", "M6.5 7l1 12.5h9l1-12.5", "M10.5 11v5", "M13.5 11v5")
    val Logout = line("logout", "M14 4.5H6.5v15H14", "M10 12h10", "M17 9l3 3-3 3")
    val Alert = line("alert", circle(12f, 12f, 8.5f), "M12 8v4.5", "M12 15.8h0.01", width = 2f)
    val More = filled("more", circle(12f, 5.5f, 1.6f), circle(12f, 12f, 1.6f), circle(12f, 18.5f, 1.6f))
    val Star = filled("star", "M12 3.2l2.7 5.5 6 0.9-4.35 4.25 1 6L12 17l-5.35 2.85 1-6L3.3 9.6l6-0.9z")
    val Seat = line("seat", circle(12f, 7.5f, 3.5f), "M5 20c0-3.9 3.1-6.5 7-6.5s7 2.6 7 6.5")
}

private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

private fun roundRect(x: Float, y: Float, w: Float, h: Float, r: Float) =
    "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"

private fun line(name: String, vararg paths: String, width: Float = 1.6f): ImageVector = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
    paths.forEach {
        addPath(
            pathData = addPathNodes(it),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
}.build()

private fun filled(name: String, vararg paths: String): ImageVector = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
    paths.forEach { addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black)) }
}.build()
