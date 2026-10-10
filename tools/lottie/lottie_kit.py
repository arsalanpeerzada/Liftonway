"""A tiny Lottie authoring kit: just enough of the Bodymovin schema to build the app's vector animations.

Conventions:
- Lists of children are written bottom-to-top (painter's order) and reversed on output, because Lottie draws
  the first item on top.
- Values are static unless you pass `anim(...)`.
"""

import json

EASE = (0.42, 0, 0.58, 1)       # ease in-out
EASE_OUT = (0.16, 0, 0.3, 1)    # decelerate
EASE_IN = (0.7, 0, 0.84, 0)     # accelerate
LINEAR = (0, 0, 1, 1)


def rgba(hex_color, alpha=1.0):
    h = hex_color.lstrip("#")
    return [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)] + [alpha]


def st(v):
    return {"a": 0, "k": v}


def anim(keys, ease=EASE):
    """keys: [(frame, value)] or [(frame, value, ease)] or [(frame, value, 'hold')]."""
    out = []
    for i, key in enumerate(keys):
        t, v = key[0], key[1]
        e = key[2] if len(key) > 2 else ease
        k = {"t": t, "s": v if isinstance(v, list) else [v]}
        if i < len(keys) - 1:
            if e == "hold":
                k["h"] = 1
            else:
                k["o"] = {"x": [e[0]], "y": [e[1]]}
                k["i"] = {"x": [e[2]], "y": [e[3]]}
        out.append(k)
    return {"a": 1, "k": out}


def val(v):
    if isinstance(v, dict) and "a" in v:
        return v
    return st(list(v) if isinstance(v, tuple) else v)


def osc(base, amp, period, total, phase=0, ease=EASE):
    """Smooth back-and-forth around `base` (scalar or list); seamless when total is a multiple of period."""
    keys = []
    t = 0
    flip = 1 if phase == 0 else -1
    while t <= total:
        if isinstance(base, (list, tuple)):
            v = [b + a * flip for b, a in zip(base, amp)]
        else:
            v = base + amp * flip
        keys.append((t, v, ease))
        flip = -flip
        t += period / 2
    return anim(keys)


# Shapes ------------------------------------------------------------------------------------------

def rect(w, h, r=0, p=(0, 0)):
    return {"ty": "rc", "d": 1, "p": val(p), "s": val([w, h]), "r": val(r)}


def ellipse(w, h=None, p=(0, 0)):
    return {"ty": "el", "d": 1, "p": val(p), "s": val([w, w if h is None else h])}


def path(verts, closed=True, ins=None, outs=None):
    z = [[0, 0] for _ in verts]
    return {"ty": "sh", "d": 1, "ks": st({"c": closed, "v": [list(v) for v in verts],
                                          "i": [list(v) for v in (ins or z)], "o": [list(v) for v in (outs or z)]})}


def line(a, b):
    return path([a, b], closed=False)


def round_corners(r):
    return {"ty": "rd", "r": st(r)}


def fill(color, o=100):
    return {"ty": "fl", "c": st(rgba(color)), "o": val(o), "r": 1}


def stroke(color, w, o=100, cap=2, join=2, dash=None):
    s = {"ty": "st", "c": st(rgba(color)), "o": val(o), "w": val(w), "lc": cap, "lj": join, "ml": 4}
    if dash:
        d, g, off = dash
        s["d"] = [{"n": "d", "nm": "dash", "v": val(d)}, {"n": "g", "nm": "gap", "v": val(g)},
                  {"n": "o", "nm": "offset", "v": val(off)}]
    return s


def trim(s=0, e=100, o=0):
    return {"ty": "tm", "s": val(s), "e": val(e), "o": val(o), "m": 1}


def repeater(copies, rotation, offset=0):
    return {"ty": "rp", "c": st(copies), "o": st(offset), "m": 1,
            "tr": {"ty": "tr", "p": st([0, 0]), "a": st([0, 0]), "s": st([100, 100]), "r": st(rotation),
                   "so": st(100), "eo": st(100)}}


def tr(p=(0, 0), a=(0, 0), s=(100, 100), r=0, o=100):
    return {"ty": "tr", "p": val(p), "a": val(a), "s": val(s), "r": val(r), "o": val(o),
            "sk": st(0), "sa": st(0)}


def shape(name, geoms, paints, modifiers=(), **t):
    """A leaf group: geometry, then modifiers (trim, round corners, repeater), then paints (first paint on top)."""
    return {"ty": "gr", "nm": name, "it": list(geoms) + list(modifiers) + list(paints) + [tr(**t)]}


def group(name, children, **t):
    return {"ty": "gr", "nm": name, "it": list(reversed(children)) + [tr(**t)]}


# Layers and files --------------------------------------------------------------------------------

class Comp:
    def __init__(self, name, w, h, op, fr=60):
        self.name, self.w, self.h, self.op, self.fr = name, w, h, op, fr
        self.layers = []

    def layer(self, name, children, p=(0, 0), a=(0, 0), s=(100, 100), r=0, o=100, ip=0, op=None, st_=0, parent=None):
        ind = len(self.layers) + 1
        lay = {"ddd": 0, "ind": ind, "ty": 4, "nm": name, "sr": 1,
               "ks": {"o": val(o), "r": val(r), "p": val(p), "a": val(a), "s": val(s)},
               "ao": 0, "shapes": list(reversed(children)), "ip": ip, "op": self.op if op is None else op,
               "st": st_, "bm": 0}
        if parent:
            lay["parent"] = parent
        self.layers.append(lay)
        return ind

    def json(self):
        return {"v": "5.7.4", "fr": self.fr, "ip": 0, "op": self.op, "w": self.w, "h": self.h, "nm": self.name,
                "ddd": 0, "assets": [], "layers": list(reversed(self.layers)), "markers": []}

    def save(self, folder):
        with open(f"{folder}/{self.name}.json", "w", encoding="utf-8") as f:
            json.dump(self.json(), f, separators=(",", ":"))
