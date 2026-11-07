"""Generates Albion launcher/splash vectors: Geist "A" inside a square hairline frame.

Usage: python scripts/mark.py app/src/main/res/font/geist.ttf app/src/main/res  (needs fonttools)
"""
import sys
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.pens.boundsPen import BoundsPen

font_path, res = sys.argv[1], sys.argv[2]
font = instantiateVariableFont(TTFont(font_path), {"wght": 560})
gs = font.getGlyphSet()
name = font.getBestCmap()[ord("A")]
bp = BoundsPen(gs)
gs[name].draw(bp)
x0, y0, x1, y1 = bp.bounds
HEIGHT = 25.0  # glyph cap height in viewport units
s = HEIGHT / (y1 - y0)
cx, cy = 54.0, 54.0
tx = cx - (x0 + x1) / 2 * s
ty = cy + (y0 + y1) / 2 * s  # y flip
sp = SVGPathPen(gs, lambda v: f"{v:.3f}".rstrip("0").rstrip("."))
gs[name].draw(TransformPen(sp, (s, 0, 0, -s, tx, ty)))
glyph = sp.getCommands()

FRAME = 21.0  # half side of the frame; whole mark stays inside the 66 dp safe zone (33 half)
frame = f"M{cx-FRAME},{cy-FRAME}h{2*FRAME}v{2*FRAME}h{-2*FRAME}z"


def vector(comment, glyph_color, frame_color, background=None):
    bg = f'    <path android:pathData="M0,0h108v108h-108z" android:fillColor="{background}" />\n' if background else ""
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!-- {comment} -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
{bg}    <path android:pathData="{frame}" android:strokeColor="{frame_color}" android:strokeWidth="1.4" android:fillColor="#00000000" />
    <path android:pathData="{glyph}" android:fillColor="{glyph_color}" />
</vector>
'''


note = "Generated from bundled Geist (wght 560) by scripts/mark.py; all ink inside the 66 dp safe zone."
open(f"{res}/drawable/ic_launcher_foreground.xml", "w", encoding="utf8", newline="\n").write(vector(note, "#FFFAFAFA", "#FFFAFAFA"))
open(f"{res}/drawable/splash_mark.xml", "w", encoding="utf8", newline="\n").write(vector(note, "#FFFAFAFA", "#FFFAFAFA"))
open(f"{res}/drawable/ic_launcher_monochrome.xml", "w", encoding="utf8", newline="\n").write(vector(note, "#FF000000", "#FF000000"))
open(f"{res}/drawable/ic_launcher_background.xml", "w", encoding="utf8", newline="\n").write('''<?xml version="1.0" encoding="utf-8"?>
<!-- Чёрный монолит Hero с едва заметным холодным бликом. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient android:type="linear" android:startX="0" android:startY="0" android:endX="108" android:endY="108">
                <item android:offset="0" android:color="#FF1A1A1A" />
                <item android:offset="0.55" android:color="#FF111111" />
                <item android:offset="1" android:color="#FF0B0B0B" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
''')
print("ok", name, glyph[:80])
