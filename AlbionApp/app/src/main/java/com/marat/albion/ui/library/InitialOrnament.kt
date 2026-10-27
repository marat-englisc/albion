package com.marat.albion.ui.library

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.marat.albion.ui.theme.Ink

/** Small printer's flourishes, contained in the initial's two-line space. */
internal fun DrawScope.drawInitialOrnament(frame: Rect) {
    val stroke = 0.55.dp.toPx()
    val inset = stroke
    val left = frame.left + inset
    val right = frame.right - inset
    val top = frame.top + inset
    val bottom = frame.bottom - inset
    val turn = frame.width * 0.22f
    val gold = Ink.Gold
    for (mirror in listOf(false, true)) {
        fun p(x: Float, y: Float) = if (mirror) Offset(right - x, bottom - y) else Offset(left + x, top + y)
        val vine = Path().apply {
            val a = p(turn * 1.5f, 0f); moveTo(a.x, a.y)
            val b = p(0f, 0f); lineTo(b.x, b.y)
            val c = p(0f, frame.height * 0.58f); lineTo(c.x, c.y)
            val d = p(turn * 0.85f, frame.height * 0.72f)
            val e = p(turn * 1.2f, frame.height * 0.53f)
            val f = p(turn * 0.32f, frame.height * 0.49f)
            cubicTo(d.x, d.y, e.x, e.y, f.x, f.y)
        }
        drawPath(vine, gold, style = Stroke(stroke))
        val jewel = p(turn * 1.5f, 0f)
        drawCircle(gold, stroke * 1.2f, jewel)
    }
}
