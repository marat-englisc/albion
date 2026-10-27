package com.marat.albion.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Albion's own 24-unit pen-stroke icon family. No filled or mixed-weight glyphs. */
object AlbionIcons {
    private fun pen(name: String, data: String, mirror: Boolean = false) = ImageVector.Builder(
        name = "Albion.$name", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f, autoMirror = mirror,
    ).addPath(
        pathData = PathParser().parsePathString(data).toNodes(),
        fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.45f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
    ).build()

    val Layers = pen("Cards", "M4 5h16v12H4z M7 20h10 M7 9h7 M7 12h10")
    val MenuBook = pen("Grammar", "M12 5v15 M12 6C9 3 5 3 2 4v14c4-1 7 0 10 2 3-2 6-3 10-2V4c-3-1-7-1-10 2 M5 8h3 M5 11h3 M16 8h3 M16 11h3", true)
    val AutoStories = pen("Reading", "M5 3h13v18H5c-2 0-2-4 0-4h13 M5 3v14 M9 7h5 M9 10h5 M18 3h2v18H5")
    val EditNote = pen("Practice", "M4 4h11 M4 9h8 M4 14h5 M4 19h5 M12 20l1-5 7-7 3 3-7 7z M18 10l3 3")
    val Insights = pen("Progress", "M3 3v18h18 M7 16v-4 M12 16V8 M17 16V5")
    val Settings = pen("Settings", "M4 6h16 M4 12h16 M4 18h16 M8 3v6 M16 9v6 M10 15v6")
    val ArrowForward = pen("Forward", "M4 12h16 M14 6l6 6-6 6", true)
    val ArrowBack = pen("Back", "M20 12H4 M10 6l-6 6 6 6", true)
    val East = ArrowForward
    val West = ArrowBack
    val Close = pen("Close", "M6 6l12 12 M18 6L6 18")
    val Clear = Close
    val Check = pen("Check", "M4 12l5 5L20 6")
    val TaskAlt = pen("Mastered", "M20 11v1a8 8 0 1 1-4-7 M9 10l3 3 8-9")
    val Add = pen("Add", "M12 4v16 M4 12h16")
    val Search = pen("Search", "M16 10a6 6 0 1 1-12 0 6 6 0 0 1 12 0 M15 15l6 6")
    val BookmarkBorder = pen("Bookmark", "M6 3h12v18l-6-4-6 4z")
    val Bookmark = pen("Saved", "M6 3h12v18l-6-4-6 4z M9 9l2 2 4-4")
    val Bookmarks = pen("Bookmarks", "M5 6h12v16l-6-4-6 4z M9 2h12v16")
    val VolumeUp = pen("Listen", "M3 9h4l5-5v16l-5-5H3z M16 8c2 2 2 6 0 8 M19 5c4 4 4 10 0 14", true)
    val Mic = pen("Microphone", "M9 5a3 3 0 0 1 6 0v7a3 3 0 0 1-6 0z M5 11v1a7 7 0 0 0 14 0v-1 M12 19v3 M8 22h8")
    val RecordVoiceOver = pen("Speech", "M8 9a3 3 0 1 1 0-6 3 3 0 0 1 0 6 M2 21v-3a6 6 0 0 1 12 0v3 M17 5c3 2 3 6 0 8 M20 2c5 4 5 10 0 14")
    val PlayArrow = pen("Play", "M7 4l13 8-13 8z")
    val Stop = pen("Stop", "M6 6h12v12H6z")
    val SlowMotionVideo = pen("Slow", "M20 12a8 8 0 1 1-8-8 M12 1v6 M18 3l-3 4 M23 7l-5 2 M10 9l5 3-5 3z")
    val Undo = pen("Undo", "M8 4L3 9l5 5 M3 9h11a6 6 0 0 1 0 12", true)
    val Redo = pen("Redo", "M16 4l5 5-5 5 M21 9H10a6 6 0 0 0 0 12", true)
    val History = pen("History", "M3 10a9 9 0 1 1 2 8 M3 4v6h6 M12 7v6l4 2")
    val ExpandMore = pen("Expand", "M6 9l6 6 6-6")
    val KeyboardArrowDown = ExpandMore
    val Menu = pen("Contents", "M4 6h16 M4 12h16 M4 18h16")
    val List = pen("List", "M8 5h13 M8 12h13 M8 19h13 M3 5h1 M3 12h1 M3 19h1", true)
    val MoreHoriz = pen("More", "M4 12h1 M11.5 12h1 M19 12h1")
    val Info = pen("Information", "M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0 M12 11v6 M12 7h.01")
    val HelpOutline = pen("Help", "M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0 M9 9a3 3 0 1 1 5 2c-2 1-2 2-2 3 M12 17h.01")
    val Lightbulb = pen("Hint", "M8 15c0-2-3-3-3-7a7 7 0 0 1 14 0c0 4-3 5-3 7 M8 18h8 M10 21h4 M12 5v6")
    val WorkspacePremium = pen("Achievement", "M18 8a6 6 0 1 1-12 0 6 6 0 0 1 12 0 M8 13L6 22l6-3 6 3-2-9 M10 8l1.5 1.5L15 6")
    val Route = pen("Path", "M6 7a2 2 0 1 1 0-4 2 2 0 0 1 0 4 M18 21a2 2 0 1 1 0-4 2 2 0 0 1 0 4 M6 7v5h12v5")
    val Timeline = pen("Timeline", "M3 17l6-7 5 3 7-9 M3 17h.01 M9 10h.01 M14 13h.01 M21 4h.01")
    val Keyboard = pen("Keyboard", "M2 5h20v14H2z M6 9h1 M11 9h1 M16 9h1 M6 12h1 M11 12h1 M16 12h1 M7 16h10")
    val GridView = pen("Grid", "M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z")
    val ViewWeek = pen("Columns", "M3 4h18v16H3z M9 4v16 M15 4v16")
    val Rule = pen("Rule", "M3 6l2 2 4-4 M13 6h8 M3 17l5-5 M3 12l5 5 M13 15h8", true)
    val CheckBox = pen("Checked", "M19 12v8H3V4h11 M8 10l4 4L21 3")
    val CheckBoxOutlineBlank = pen("Unchecked", "M3 4h17v16H3z")
    val BorderColor = pen("Highlight", "M7 15l-3 4h7L22 8l-7-7L4 12z M12 4l7 7 M2 23h20")
    val TextFields = pen("Type", "M2 5h12 M8 5v15 M4 20h8 M14 10h8 M18 10v10 M15 20h6")
    val FormatQuote = pen("Quote", "M3 5h6v7H5c0 3 1 4 4 5v2c-5-1-6-4-6-7z M15 5h6v7h-4c0 3 1 4 4 5v2c-5-1-6-4-6-7z")
    val CloudOff = pen("Offline", "M5 17a4 4 0 0 1-1-8 7 7 0 0 1 12-4 M18 9a4 4 0 0 1 2 8 M2 2l20 20")
    val ReportProblem = pen("Attention", "M12 3l10 18H2z M12 9v5 M12 17h.01")
    val Inventory2 = pen("Collection", "M3 4h18v5H3z M5 9v12h14V9 M9 13h6")
    val Inventory = Inventory2
}
