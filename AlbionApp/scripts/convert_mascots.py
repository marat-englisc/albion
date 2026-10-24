"""
Готовит маскота Финна для приложения: ../mascot/finn_*.png → app/src/main/res/drawable-nodpi/finn_*.webp.

Кроме целых поз готовит «выглядывания»:
- finn_peek_hold / finn_peek_side — отдельные рисунки: Финн держится за верхний край и выглядывает
  из-за бокового. Обрезаются по содержимому; скрипт печатает, где на картинке проходит край
  (доли CUT в Peek.kt должны с ними совпадать);
- finn_peek_* из PEEKS — голова с шарфом из обычной позы для «вниз головой» и «из угла».

Исходники 1254×1254 весят ~1,2 МБ; в APK кладём 512×512 WebP с прозрачностью (~40 КБ).
Холст не обрезается: все позы остаются в одном масштабе, и Финн не «прыгает» в размере между экранами.
Тело в исходниках чуть-чуть прозрачное (альфа 250–254) — доводим до 255, чтобы фон не просвечивал.

Запуск из папки AlbionApp (нужен Pillow):  python scripts/convert_mascots.py
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT.parent / "mascot"
TARGET = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
SIZE = 512
PEEK_HEIGHT = 300

# Выглядывание → поза-источник. Голова и шарф — верхняя половина фигуры.
PEEKS = {
    "hello": "welcome_back",
    "cheer": "try_again",
}
PEEK_FRACTION = 0.52

# Готовые рисунки выглядывания → имя в APK.
PEEK_ART = {"hold_edge": "finn_peek_hold", "peek_side": "finn_peek_side"}
PEEK_ART_HEIGHT = 360


def convert(src: Path, dst: Path) -> None:
    image = Image.open(src).convert("RGBA").resize((SIZE, SIZE), Image.LANCZOS)
    alpha = image.getchannel("A").point(lambda a: 255 if a >= 235 else a)
    image.putalpha(alpha)
    image.save(dst, "WEBP", quality=88, method=6)


def peek(src: Path, dst: Path) -> None:
    image = Image.open(src).convert("RGBA")
    solid = image.getchannel("A").point(lambda a: 255 if a > 40 else 0)
    _, top, _, bottom = solid.getbbox()
    cut = int(top + (bottom - top) * PEEK_FRACTION)
    left, _, right, _ = solid.crop((0, top, image.width, cut)).getbbox()
    head = image.crop((left, top, right, cut))
    width = round(head.width * PEEK_HEIGHT / head.height)
    head = head.resize((width, PEEK_HEIGHT), Image.LANCZOS)
    head.putalpha(head.getchannel("A").point(lambda a: 255 if a >= 235 else a))
    head.save(dst, "WEBP", quality=88, method=6)


def peek_art(src: Path, dst: Path) -> str:
    """Обрезает рисунок по содержимому и сообщает, где на нём проходит край, за который держится Финн."""
    image = Image.open(src).convert("RGBA")
    solid = image.getchannel("A").point(lambda a: 255 if a > 128 else 0)
    left, top, right, bottom = solid.getbbox()
    pixels = solid.load()
    if "hold" in src.stem:
        # Край — нижняя граница тела в середине рисунка (по бокам ниже свисают крылья).
        middle = range(left + (right - left) * 45 // 100, left + (right - left) * 70 // 100)
        edge = max(max(y for y in range(top, bottom) if pixels[x, y]) for x in middle)
        cut = f"edge at height {(edge - top) / (bottom - top):.4f}"
    else:
        # Край — правая граница тела в верхней половине (ниже за край заходит крыло).
        rows = range(top + (bottom - top) // 5, top + (bottom - top) // 2)
        edge = max(max(x for x in range(left, right) if pixels[x, y]) for y in rows)
        cut = f"edge at width {(edge - left) / (right - left):.4f}"
    art = image.crop((left, top, right, bottom))
    width = round(art.width * PEEK_ART_HEIGHT / art.height)
    art = art.resize((width, PEEK_ART_HEIGHT), Image.LANCZOS)
    art.putalpha(art.getchannel("A").point(lambda a: 255 if a >= 235 else a))
    art.save(dst, "WEBP", quality=88, method=6)
    return cut


def main() -> None:
    TARGET.mkdir(parents=True, exist_ok=True)
    for src in sorted(SOURCE.glob("finn_*.png")):
        if src.stem.removeprefix("finn_") in PEEK_ART:
            continue
        dst = TARGET / (src.stem + ".webp")
        convert(src, dst)
        print(f"{src.name} -> {dst.name}  {dst.stat().st_size // 1024} KB")
    for name, pose in PEEKS.items():
        dst = TARGET / f"finn_peek_{name}.webp"
        peek(SOURCE / f"finn_{pose}.png", dst)
        print(f"finn_{pose}.png -> {dst.name}  {dst.stat().st_size // 1024} KB")
    for source, name in PEEK_ART.items():
        dst = TARGET / f"{name}.webp"
        cut = peek_art(SOURCE / f"finn_{source}.png", dst)
        print(f"finn_{source}.png -> {dst.name}  {dst.stat().st_size // 1024} KB, {cut}")


if __name__ == "__main__":
    main()
