"""Generates the app's Lottie animations into core/designsystem/src/main/res/raw and a preview page.

Run from anywhere:  python android/tools/lottie/generate.py
Every animation is original vector work in the Lift On Way palette, so there is nothing to license.
"""

import json
import os

from lottie_kit import (EASE, EASE_IN, EASE_OUT, LINEAR, Comp, anim, ellipse, fill, group, line, osc, path, rect,
                        repeater, round_corners, shape, stroke, trim)

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.normpath(os.path.join(HERE, "..", "..", "core", "designsystem", "src", "main", "res", "raw"))

GREEN = "#04BF68"
GREEN_DARK = "#03A85B"
GREEN_SOFT = "#E3F7EC"
INK = "#1F2A44"
CORAL = "#F47458"
AMBER = "#EAB054"
AMBER_DARK = "#D4952F"
BLUE = "#367CFF"
WHITE = "#FFFFFF"
CIRCLE_BG = "#F4F4F4"


# Reusable pieces ---------------------------------------------------------------------------------

def wheel(name, x, spin):
    spokes = shape("spokes", [rect(4, 18, 2), rect(18, 4, 2)], [fill("#9CA3AF")], r=spin)
    return group(name, [
        shape("tyre", [ellipse(40)], [fill("#1F2937")]),
        shape("rim", [ellipse(23)], [fill("#E8ECF2")]),
        spokes,
        shape("hub", [ellipse(7)], [fill("#6B7280")]),
    ], p=(x, 12))


def car(body, body_dark, glass, spin=0, passengers=False, luggage=False):
    """Side view facing right; origin on the wheel axle line, about 220 wide."""
    parts = [
        shape("shadow", [ellipse(224, 10, p=(0, 34))], [fill("#000000", 10)]),
        shape("cabin", [path([(-72, -22), (-48, -58), (36, -58), (70, -22)])], [fill(body)], [round_corners(10)]),
        shape("body", [rect(214, 38, 15, p=(0, -5))], [fill(body)]),
        shape("bumper", [rect(218, 7, 3.5, p=(0, 12))], [fill(body_dark)]),
        shape("rear window", [path([(-61, -25), (-43, -51), (-8, -51), (-8, -25)])], [fill(glass)], [round_corners(4)]),
        shape("front window", [path([(-1, -25), (-1, -51), (31, -51), (57, -25)])], [fill(glass)], [round_corners(4)]),
    ]
    if passengers:
        for i, x in enumerate((-30, 18)):
            parts.append(group(f"passenger {i}", [
                shape("shoulders", [rect(20, 12, 6, p=(0, -27))], [fill(BLUE if i == 0 else CORAL)]),
                shape("head", [ellipse(13, p=(0, -38))], [fill("#F2C6A0")]),
                shape("hair", [rect(14, 6, 3, p=(0, -44))], [fill("#3B2F2F")]),
            ], p=(x, 0)))
    parts += [
        shape("door line", [line((-4, -24), (-4, 8))], [stroke(body_dark, 2)]),
        shape("handle", [rect(10, 3, 1.5, p=(10, -15))], [fill(body_dark)]),
        shape("headlight", [rect(8, 8, 2, p=(103, -13))], [fill("#FFE08A")]),
        shape("tail light", [rect(6, 11, 2, p=(-104, -12))], [fill(CORAL)]),
    ]
    if luggage:
        parts += [
            shape("rack", [line((-40, -60), (44, -60))], [stroke(INK, 2.5)]),
            shape("bag 1", [rect(36, 14, 3, p=(-16, -68))], [fill(CORAL)]),
            shape("bag 2", [rect(22, 18, 3, p=(16, -70))], [fill("#2A9D8F")]),
        ]
    parts += [wheel("rear wheel", -64, spin), wheel("front wheel", 64, spin)]
    return parts


def pin(color, dot=WHITE):
    """Map pin with its point at the origin, 28 wide and 36 tall."""
    k = 7.73
    return [
        shape("pin", [path([(0, 0), (-14, -22), (0, -36), (14, -22)],
                           ins=[(6, -8), (0, 8), (-k, 0), (0, -k)], outs=[(-6, -8), (0, -k), (k, 0), (0, 8)])],
              [fill(color)]),
        shape("pin dot", [ellipse(10, p=(0, -22))], [fill(dot)]),
    ]


def scroll_left(x0, y, distance, total, left_edge):
    """Linear left drift that wraps from `left_edge` back to `left_edge + distance`, seamless over `total`."""
    t1 = (x0 - left_edge) / distance * total
    return anim([(0, [x0, y], LINEAR), (t1, [left_edge, y], "hold"), (t1 + 0.01, [left_edge + distance, y], LINEAR),
                 (total, [x0, y])])


# Animations --------------------------------------------------------------------------------------

def splash_logo():
    c = Comp("splash_logo", 200, 200, 100)
    c.layer("pulse", [shape("ring", [ellipse(112)], [stroke(WHITE, 3, o=anim([(48, 0), (52, 55), (95, 0)]))],
                            s=anim([(48, [100, 100]), (95, [150, 150])], EASE_OUT))], p=(100, 108))
    c.layer("arc", [shape("arc", [ellipse(112)], [stroke(WHITE, 15, cap=2)],
                          [trim(s=9, e=anim([(0, 9), (34, 91)], EASE_OUT))])], p=(100, 108))
    c.layer("bar", [shape("bar", [rect(15, 50, 7.5)], [fill(WHITE)])],
            p=anim([(18, [100, 20]), (36, [100, 68], EASE_OUT), (44, [100, 61]), (52, [100, 64])]),
            o=anim([(18, 0), (26, 100)]))
    return c


def travel_car():
    """The carpool on the open road: Welcome, Home and the first walkthrough page."""
    total = 240
    c = Comp("travel_car", 400, 260, total)
    c.layer("sky", [shape("blob", [ellipse(380, 210, p=(200, 120))], [fill("#EAF7F4")])])
    for i, (x, y, sc) in enumerate(((95, 70, 100), (300, 52, 80))):
        c.layer(f"cloud {i}", [group("cloud", [
            shape("puffs", [ellipse(44, 30, p=(0, 0)), ellipse(34, 26, p=(24, 4)), ellipse(30, 22, p=(-22, 6)),
                            rect(70, 14, 7, p=(2, 10))], [fill(WHITE)]),
        ], s=(sc, sc))], p=osc([x, y], [10, 0], 240, total, phase=i))
    c.layer("hills", [
        shape("hill far", [ellipse(300, 150, p=(300, 225))], [fill("#C9EDE0")]),
        shape("hill near", [ellipse(240, 110, p=(80, 228))], [fill("#B3E4D2")]),
    ])
    for i, x0 in enumerate((40, 150, 260, 370)):
        tall = i % 2 == 0
        c.layer(f"tree {i}", [group("tree", [
            shape("trunk", [rect(5, 22, 2, p=(0, -11))], [fill("#8D6E63")]),
            shape("crown", [ellipse(28 if tall else 22, 40 if tall else 30, p=(0, -36 if tall else -30))],
                  [fill("#3FBF8A" if tall else "#2E9E6F")]),
        ])], p=scroll_left(x0, 214, 440, total, -20))
    c.layer("ground", [shape("ground", [rect(400, 46, 0, p=(200, 237))], [fill(WHITE)]),
                       shape("road line", [line((0, 214), (400, 214))], [stroke(INK, 3)]),
                       shape("lane", [line((0, 228), (400, 228))],
                             [stroke("#D9DEE8", 3, cap=2, dash=(18, 16, anim([(0, 0), (total, 34 * 12)], LINEAR)))])])
    spin = anim([(0, 0), (total, 360 * 6)], LINEAR)
    c.layer("car", [group("car", car("#F7C873", "#E2AE4F", "#CFE3F7", spin=spin, passengers=True, luggage=True))],
            p=osc([200, 186], [0, -1.6], 40, total), s=(92, 92))
    return c


def find_ride():
    total = 200
    c = Comp("find_ride", 360, 300, total)
    c.layer("circle", [shape("bg", [ellipse(230, p=(180, 150))], [fill(CIRCLE_BG)])])
    c.layer("car", [group("car", car("#DCE6F5", "#C3D2E8", "#3D4A5C"))], p=(180, 182), s=(80, 80))
    for i, (x, y, color, delay) in enumerate(((84, 92, GREEN, 0), (276, 106, CORAL, 60), (262, 222, BLUE, 120))):
        c.layer(f"sparkle {i}", [shape("plus", [rect(4, 16, 2), rect(16, 4, 2)], [fill(color)])], p=(x, y),
                s=anim([(delay, [0, 0]), (delay + 20, [110, 110], EASE_OUT), (delay + 40, [0, 0]), (total, [0, 0])]),
                r=anim([(delay, 0), (delay + 40, 90)]))
    route = [(0, [140, 150]), (50, [222, 138]), (100, [208, 186]), (150, [140, 178]), (200, [140, 150])]
    c.layer("magnifier", [group("magnifier", [
        shape("handle", [line((28, 28), (56, 56))], [stroke(BLUE, 11, cap=2)]),
        shape("lens", [ellipse(76)], [stroke(BLUE, 9), fill(WHITE, 30)]),
        shape("glint", [path([(-24, -6), (-6, -24)], closed=False)], [stroke(WHITE, 4, o=80)]),
    ])], p=anim(route), r=osc(0, 6, 100, total))
    return c


def post_ride():
    total = 210
    c = Comp("post_ride", 360, 300, total)
    c.layer("circle", [shape("bg", [ellipse(230, p=(190, 150))], [fill(CIRCLE_BG)])])
    c.layer("car", [group("car", car("#DCE6F5", "#C3D2E8", "#3D4A5C"))], p=(200, 196), s=(80, 80))
    fade = anim([(0, 100), (185, 100), (205, 0), (total, 0)])
    lines = []
    for i, (w, start) in enumerate(((40, 20), (46, 34), (30, 48))):
        lines.append(shape(f"line {i}", [line((-24, -20 + i * 13), (-24 + w, -20 + i * 13))], [stroke("#C9D1DE", 5)],
                           [trim(e=anim([(start, 0), (start + 18, 100, EASE_OUT)]))]))
    c.layer("document", [group("document", [
        shape("paper", [rect(70, 88, 8)], [stroke(INK, 2.5), fill(WHITE)]),
        *lines,
        group("stamp", [shape("badge", [ellipse(26)], [fill(GREEN)]),
                        shape("tick", [path([(-6, 0), (-1.5, 5), (7, -5)], closed=False)], [stroke(WHITE, 3.5)],
                              [trim(e=anim([(84, 0), (98, 100)]))])],
              p=(26, 34), s=anim([(70, [0, 0]), (84, [120, 120], EASE_OUT), (94, [100, 100])])),
    ])], p=osc([102, 112], [0, -5], 70, total), o=fade, r=-6)
    c.layer("pin", [group("pin", pin(GREEN))],
            p=anim([(96, [214, 40]), (118, [214, 128], EASE_IN), (128, [214, 116]), (138, [214, 128]), (total, [214, 128])]),
            s=anim([(96, [100, 100]), (118, [100, 100]), (122, [118, 84]), (130, [96, 104]), (138, [100, 100])]),
            o=anim([(96, 0), (104, 100), (185, 100), (205, 0)]))
    return c


def success_check():
    c = Comp("success_check", 200, 200, 72)
    c.layer("halo", [shape("halo", [ellipse(120)], [stroke(GREEN, 4, o=anim([(10, 60), (44, 0)]))],
                           s=anim([(10, [100, 100]), (44, [150, 150])], EASE_OUT))], p=(100, 100))
    for k, (color, size, rot, start) in enumerate(((GREEN, 10, 0, 14), (AMBER, 7, 22.5, 18))):
        c.layer(f"burst {k}", [group("burst", [
            shape("dot", [ellipse(size)], [fill(color)],
                  p=anim([(start, [0, -66]), (start + 28, [0, -94], EASE_OUT)]),
                  s=anim([(start, [100, 100]), (start + 28, [0, 0])])),
            repeater(8, 45),
        ], r=rot)], p=(100, 100))
    c.layer("disc", [shape("disc", [ellipse(120)], [fill(GREEN)])], p=(100, 100),
            s=anim([(0, [0, 0]), (14, [112, 112], EASE_OUT), (22, [100, 100])]))
    c.layer("tick", [shape("tick", [path([(-25, 2), (-8, 19), (26, -17)], closed=False)], [stroke(WHITE, 11)],
                           [trim(e=anim([(14, 0), (34, 100)], EASE_OUT))])], p=(100, 100))
    return c


def empty_box():
    total = 160
    c = Comp("empty_box", 220, 190, total)
    c.layer("blob", [shape("blob", [ellipse(160, 120, p=(110, 112))], [fill("#E6EAF2")])],
            s=osc([100, 100], [3, 3], 160, total), a=(110, 112), p=(110, 112))
    for i, (x, color, delay) in enumerate(((92, INK, 0), (126, INK, 55), (110, GREEN, 105))):
        c.layer(f"float {i}", [shape("squiggle", [path([(0, 0), (4, -6), (0, -12), (4, -18)], closed=False,
                                                       ins=[(0, 0), (2, 2), (-2, 2), (0, 0)],
                                                       outs=[(2, -2), (-2, -2), (2, -2), (0, 0)])],
                                     [stroke(color, 1.8)])],
                p=anim([(delay, [x, 66]), (delay + 55, [x + (i - 1) * 8, 18], EASE_OUT), (total, [x, 66])]),
                o=anim([(delay, 0), (delay + 12, 100), (delay + 50, 0), (total, 0)]))
    face = [stroke(INK, 2.4), fill(WHITE)]
    c.layer("box", [group("box", [
        shape("back left flap", [path([(60, 80), (110, 60), (100, 42), (50, 62)])], face,
              a=(85, 70), p=(85, 70), r=osc(0, 5, 80, total)),
        shape("back right flap", [path([(110, 60), (160, 80), (170, 62), (120, 42)])], face,
              a=(135, 70), p=(135, 70), r=osc(0, -5, 80, total, phase=1)),
        shape("inside", [path([(60, 80), (110, 60), (160, 80), (110, 100)])], [fill(INK)]),
        shape("left face", [path([(60, 80), (110, 100), (110, 152), (60, 132)])], face),
        shape("right face", [path([(110, 100), (160, 80), (160, 132), (110, 152)])], face),
        shape("front left flap", [path([(60, 80), (110, 100), (96, 118), (46, 98)])], face,
              a=(85, 90), p=(85, 90), r=osc(0, -4, 80, total, phase=1)),
        shape("front right flap", [path([(110, 100), (160, 80), (174, 98), (124, 118)])], face,
              a=(135, 90), p=(135, 90), r=osc(0, 4, 80, total)),
    ])], p=osc([0, 0], [0, -3], 80, total))
    return c


def no_rides():
    """Plays 0-30 once (the slash), then loops 30-150."""
    total = 150
    c = Comp("no_rides", 260, 180, total)
    c.layer("car", [group("car", car(GREEN, GREEN_DARK, WHITE))], p=osc([130, 112], [0, -1.5], 40, 120), s=(78, 78))
    slash = [line((76, 150), (186, 44))]
    c.layer("slash", [
        shape("gap", slash, [stroke(WHITE, 15)], [trim(e=anim([(6, 0), (26, 100)], EASE_OUT))]),
        shape("slash", slash, [stroke("#E5484D", 7)], [trim(e=anim([(6, 0), (26, 100)], EASE_OUT))]),
    ])
    return c


def searching():
    total = 120
    c = Comp("searching", 220, 220, total)
    for i in range(3):
        d = i * 40
        for shift in (0, -total):
            start = d + shift
            c.layer(f"ring {i}{'b' if shift else ''}", [
                shape("ring", [ellipse(60)], [stroke(GREEN, 3, o=anim([(0, 80), (total, 0)], EASE_OUT))],
                      s=anim([(0, [60, 60]), (total, [300, 300])], EASE_OUT))
            ], p=(110, 120), ip=max(start, 0), op=start + total, st_=start)
    c.layer("shadow", [shape("shadow", [ellipse(30, 8)], [fill("#000000", 14)],
                             s=osc([100, 100], [-18, -18], 60, total))], p=(110, 124))
    c.layer("pin", [group("pin", pin(GREEN), s=(160, 160))], p=osc([110, 118], [0, -8], 60, total))
    return c


def wallet_coins():
    total = 150
    c = Comp("wallet_coins", 220, 200, total)
    c.layer("wallet back", [shape("back", [rect(132, 92, 16, p=(110, 116))], [fill(GREEN_DARK)])],
            a=(110, 160), p=(110, 160),
            s=anim([(0, [100, 100]), (34, [100, 100]), (40, [104, 96]), (48, [100, 100]), (84, [100, 100]),
                    (90, [104, 96]), (98, [100, 100]), (134, [100, 100]), (140, [104, 96]), (148, [100, 100])]))
    for i, start in enumerate((0, 50, 100)):
        x = 96 + i * 12
        c.layer(f"coin {i}", [group("coin", [
            shape("coin", [ellipse(36)], [fill(AMBER)]),
            shape("ring", [ellipse(24)], [stroke(AMBER_DARK, 3)]),
            shape("shine", [rect(4, 10, 2, p=(0, 0))], [fill("#FFF3D6")]),
        ])], p=anim([(start, [x, -24]), (start + 38, [x, 112], EASE_IN), (total, [x, 112])]),
            r=anim([(start, -30), (start + 38, 20)]))
    c.layer("wallet front", [group("front", [
        shape("pocket", [rect(132, 74, 16, p=(110, 128))], [fill(GREEN)]),
        shape("clasp", [rect(34, 24, 9, p=(160, 126))], [fill(GREEN_DARK)]),
        shape("stud", [ellipse(8, p=(166, 126))], [fill(WHITE)]),
    ])], a=(110, 165), p=(110, 165),
        s=anim([(0, [100, 100]), (34, [100, 100]), (40, [104, 96]), (48, [100, 100]), (84, [100, 100]),
                (90, [104, 96]), (98, [100, 100]), (134, [100, 100]), (140, [104, 96]), (148, [100, 100])]))
    return c


def bell():
    total = 150
    c = Comp("bell", 200, 200, total)
    swing = anim([(0, 0), (10, 16), (22, -13), (34, 9), (46, -5), (58, 2), (70, 0), (total, 0)])
    c.layer("waves", [
        shape("left", [path([(-46, -18), (-56, 0), (-46, 18)], closed=False, ins=[(0, 0), (0, -10), (0, 0)], outs=[(0, 0), (0, 10), (0, 0)])],
              [stroke(AMBER, 4, o=anim([(4, 0), (12, 100), (40, 0)]))]),
        shape("right", [path([(46, -18), (56, 0), (46, 18)], closed=False, ins=[(0, 0), (0, -10), (0, 0)], outs=[(0, 0), (0, 10), (0, 0)])],
              [stroke(AMBER, 4, o=anim([(4, 0), (12, 100), (40, 0)]))]),
    ], p=(100, 92))
    c.layer("bell", [group("bell", [
        shape("clapper", [ellipse(16, p=(0, 44))], [fill(AMBER_DARK)]),
        shape("dome", [path([(-34, 30), (-30, -8), (0, -40), (30, -8), (34, 30)],
                            ins=[(0, 0), (0, 12), (-18, 0), (0, -18), (0, -10)],
                            outs=[(0, -10), (0, -18), (18, 0), (0, 12), (0, 0)])], [fill(AMBER)]),
        shape("rim", [rect(84, 12, 6, p=(0, 32))], [fill(AMBER_DARK)]),
        shape("knob", [ellipse(12, p=(0, -42))], [fill(AMBER_DARK)]),
    ], a=(0, -44), p=(0, -44), r=swing)], p=(100, 96))
    c.layer("badge", [shape("badge", [ellipse(18)], [fill(CORAL)])], p=(128, 58),
            s=anim([(0, [0, 0]), (16, [120, 120], EASE_OUT), (24, [100, 100])]))
    return c


def route_map():
    total = 200
    c = Comp("route_map", 300, 250, total)
    c.layer("phone", [
        shape("frame", [rect(150, 220, 22, p=(150, 125))], [stroke(INK, 3), fill(WHITE)]),
        shape("map", [rect(132, 190, 14, p=(150, 128))], [fill("#EAF7F1")]),
        shape("streets", [line((84, 70), (216, 98)), line((84, 160), (216, 140)), line((120, 33), (128, 222)),
                          line((182, 33), (174, 222))], [stroke(WHITE, 7)]),
        shape("speaker", [rect(36, 5, 2.5, p=(150, 24))], [fill(INK)]),
    ])
    fade = anim([(0, 100), (178, 100), (196, 0), (total, 0)])
    c.layer("route", [
        shape("route", [path([(110, 190), (124, 150), (178, 140), (172, 96), (186, 72)], closed=False)],
              [stroke(GREEN, 4.5, dash=(8, 7, 0))], [trim(e=anim([(10, 0), (80, 100)], EASE))]),
        shape("start", [ellipse(14, p=(110, 190))], [stroke(WHITE, 3), fill(GREEN)]),
    ], o=fade)
    c.layer("pin", [group("pin", pin(CORAL))],
            p=anim([(76, [186, 20]), (96, [186, 74], EASE_IN), (104, [186, 66]), (112, [186, 74]), (total, [186, 74])]),
            o=anim([(76, 0), (82, 100), (178, 100), (196, 0)]))
    return c


def loading_dots():
    c = Comp("loading_dots", 120, 40, 60)
    for i in range(3):
        s = i * 8
        c.layer(f"dot {i}", [shape("dot", [ellipse(10)], [fill(WHITE)])],
                p=anim([(s, [40 + i * 20, 22]), (s + 12, [40 + i * 20, 14]), (s + 24, [40 + i * 20, 22]), (60, [40 + i * 20, 22])]),
                o=anim([(s, 55), (s + 12, 100), (s + 24, 55), (60, 55)]))
    return c


ALL = [splash_logo, travel_car, find_ride, post_ride, success_check, empty_box, no_rides, searching, wallet_coins,
       bell, route_map, loading_dots]


def main():
    os.makedirs(OUT, exist_ok=True)
    comps = [f() for f in ALL]
    for comp in comps:
        comp.save(OUT)
    cards = "".join(
        f'<figure><div class="stage {"dark" if comp.name in ("splash_logo", "loading_dots") else ""}" id="{comp.name}"></div>'
        f"<figcaption>{comp.name}</figcaption></figure>" for comp in comps)
    data = json.dumps({comp.name: comp.json() for comp in comps})
    html = f"""<!doctype html><html><head><meta charset="utf-8"><title>Lift On Way animations</title>
<style>body{{font-family:sans-serif;background:#F3F5FD;margin:16px}}main{{display:flex;flex-wrap:wrap;gap:16px}}
figure{{margin:0;background:#fff;border-radius:12px;padding:8px}}.stage{{width:300px;height:220px}}.dark{{background:#04BF68}}
figcaption{{text-align:center;font-size:13px;color:#333}}</style></head><body><main>{cards}</main>
<script src="https://cdnjs.cloudflare.com/ajax/libs/bodymovin/5.12.2/lottie.min.js"></script>
<script>const D={data};for(const k in D){{lottie.loadAnimation({{container:document.getElementById(k),renderer:'svg',loop:true,autoplay:true,animationData:D[k]}});}}</script>
</body></html>"""
    with open(os.path.join(HERE, "preview.html"), "w", encoding="utf-8") as f:
        f.write(html)
    print(f"Wrote {len(comps)} animations to {OUT}")


if __name__ == "__main__":
    main()
