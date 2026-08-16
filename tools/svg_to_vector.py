"""Convert Affinity-exported Motif SVGs into Android VectorDrawable XML."""

from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from pathlib import Path

NS = {
    "svg": "http://www.w3.org/2000/svg",
}
ET.register_namespace("", NS["svg"])

MATRIX_RE = re.compile(
    r"matrix\(\s*([-\d.eE+]+)\s*,\s*([-\d.eE+]+)\s*,\s*([-\d.eE+]+)\s*,\s*"
    r"([-\d.eE+]+)\s*,\s*([-\d.eE+]+)\s*,\s*([-\d.eE+]+)\s*\)"
)
RGB_RE = re.compile(r"rgb\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)", re.I)
HEX_RE = re.compile(r"#([0-9a-fA-F]{3,8})")
VIEWBOX_RE = re.compile(r"viewBox=\"([^\"]+)\"")
TRANSFORM_RE = re.compile(r"transform=\"([^\"]+)\"")
PATH_RE = re.compile(r"<path\s+d=\"([^\"]+)\"\s+style=\"([^\"]*)\"\s*/>")
STYLE_FILL_RE = re.compile(r"fill:\s*([^;]+)")


def fmt(n: float) -> str:
    s = f"{n:.6f}".rstrip("0").rstrip(".")
    return s if s else "0"


def rgb_to_hex(value: str) -> str:
    value = value.strip()
    m = RGB_RE.fullmatch(value)
    if m:
        r, g, b = (int(m.group(i)) for i in range(1, 4))
        return f"#{r:02X}{g:02X}{b:02X}"
    m = HEX_RE.fullmatch(value)
    if m:
        h = m.group(1)
        if len(h) == 3:
            h = "".join(c * 2 for c in h)
        return f"#{h.upper()}"
    if value.lower() in ("none", "transparent"):
        return "none"
    return "#000000"


def parse_style(style: str) -> tuple[str, str]:
    fill = "#000000"
    for part in style.split(";"):
        if ":" not in part:
            continue
        k, v = part.split(":", 1)
        if k.strip() == "fill":
            fill = rgb_to_hex(v.strip())
    return fill, "evenOdd"


def convert(src: Path, dest: Path, dp_width: int) -> None:
    text = src.read_text(encoding="utf-8")
    vb = VIEWBOX_RE.search(text)
    if not vb:
        raise SystemExit(f"No viewBox in {src}")
    min_x, min_y, width, height = (float(x) for x in vb.group(1).split())
    aspect = height / width
    dp_height = max(1, round(dp_width * aspect))

    lines: list[str] = [
        '<?xml version="1.0" encoding="utf-8"?>',
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        f'    android:width="{dp_width}dp"',
        f'    android:height="{dp_height}dp"',
        f'    android:viewportWidth="{fmt(width)}"',
        f'    android:viewportHeight="{fmt(height)}">',
    ]

    # Walk the raw file so we keep group nesting without dealing with default namespaces.
    depth = 0
    group_stack: list[bool] = []

    # Apply viewBox origin as an implicit translate if needed.
    if min_x or min_y:
        lines.append(
            f'    <group android:translateX="{fmt(-min_x)}" android:translateY="{fmt(-min_y)}">'
        )
        extra_root = True
    else:
        extra_root = False

    for raw_line in text.splitlines():
        line = raw_line.strip()
        if line.startswith("<g"):
            tm = TRANSFORM_RE.search(line)
            attrs = []
            if tm:
                mm = MATRIX_RE.search(tm.group(1))
                if not mm:
                    raise SystemExit(f"Unsupported transform in {src}: {tm.group(1)}")
                a, b, c, d, e, f = (float(mm.group(i)) for i in range(1, 7))
                if abs(b) > 1e-6 or abs(c) > 1e-6:
                    raise SystemExit(f"Skew/rotate matrix not supported: {tm.group(1)}")
                if abs(a - 1.0) > 1e-6:
                    attrs.append(f'android:scaleX="{fmt(a)}"')
                if abs(d - 1.0) > 1e-6:
                    attrs.append(f'android:scaleY="{fmt(d)}"')
                if abs(e) > 1e-6:
                    attrs.append(f'android:translateX="{fmt(e)}"')
                if abs(f) > 1e-6:
                    attrs.append(f'android:translateY="{fmt(f)}"')
            indent = "    " * (2 + depth)
            if attrs:
                lines.append(f"{indent}<group {' '.join(attrs)}>")
            else:
                lines.append(f"{indent}<group>")
            group_stack.append(True)
            depth += 1
        elif line.startswith("</g"):
            depth -= 1
            indent = "    " * (2 + depth)
            lines.append(f"{indent}</group>")
            group_stack.pop()
        else:
            pm = PATH_RE.search(line)
            if not pm:
                continue
            d, style = pm.group(1), pm.group(2)
            fill, fill_type = parse_style(style)
            if fill == "none":
                continue
            indent = "    " * (2 + depth)
            lines.append(
                f'{indent}<path android:fillColor="{fill}" '
                f'android:fillType="{fill_type}" android:pathData="{d}" />'
            )

    if extra_root:
        lines.append("    </group>")
    lines.append("</vector>")
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {dest} ({dest.stat().st_size} bytes, {dp_width}x{dp_height}dp)")


def main() -> None:
    root = Path(__file__).resolve().parents[1]
    assets = root / "assets"
    out = root / "app" / "src" / "main" / "res" / "drawable"
    convert(assets / "earbud_left.svg", out / "motif_earbud_left.xml", 72)
    convert(assets / "earbud_right.svg", out / "motif_earbud_right.xml", 72)
    convert(assets / "case.svg", out / "motif_case.xml", 96)


if __name__ == "__main__":
    main()
