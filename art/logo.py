"""Draws the Bloodmoon Events logo as 64x64 pixel art and writes scaled PNGs. No dependencies.

Run: python3 art/logo.py
Writes art/logo-512.png (CurseForge/Modrinth) and src/main/resources/assets/bloodmoonevents/icon.png (128x128, in-game).
"""
import math
import os
import random
import struct
import zlib

N = 64
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
random.seed(7)

img = [[(0, 0, 0)] * N for _ in range(N)]


def put(x, y, c):
    if 0 <= x < N and 0 <= y < N:
        img[y][x] = c


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


# --- Sky: crimson near the horizon fading to near-black at the top, banded like pixel art ---
TOP = (14, 4, 10)
MID = (58, 8, 14)
LOW = (120, 18, 18)
for y in range(N):
    t = y / (N - 1)
    c = mix(TOP, MID, t / 0.6) if t < 0.6 else mix(MID, LOW, (t - 0.6) / 0.4)
    # Quantise into bands with a checkerboard dither on band edges
    for x in range(N):
        band = int(t * 9 + (0.5 if (x + y) % 2 else 0.0)) / 9
        cc = mix(TOP, MID, band / 0.6) if band < 0.6 else mix(MID, LOW, (band - 0.6) / 0.4)
        put(x, y, cc)

# Stars, dim and only in the dark upper sky
for _ in range(26):
    x, y = random.randrange(N), random.randrange(0, 26)
    put(x, y, (120, 70, 80) if random.random() < 0.7 else (200, 140, 140))

# --- The blood moon ---
CX, CY, R = 32, 26, 20
for y in range(N):
    for x in range(N):
        d = math.hypot(x + 0.5 - CX, y + 0.5 - CY)
        if R < d <= R + 3:
            # Glow halo, dithered so it reads as pixel art
            g = 1 - (d - R) / 3
            if (x + y) % 2 == 0 or g > 0.6:
                put(x, y, mix(img[y][x], (170, 25, 25), 0.55 * g))
        elif d <= R:
            # Base: bright red, darker toward the lower-right edge for a little volume
            shade = ((x - CX) + (y - CY)) / (2 * R)
            c = mix((235, 60, 45), (150, 18, 22), max(0.0, min(1.0, 0.5 + shade)))
            if d > R - 1.2:
                c = mix(c, (110, 10, 16), 0.6)  # rim
            put(x, y, c)

# Craters (darker blotches) and a highlight crescent
for (mx, my, mr) in [(22, 18, 3.4), (41, 20, 2.6), (20, 31, 2.4), (44, 32, 3.0), (37, 11, 1.8), (16, 24, 1.5)]:
    for y in range(N):
        for x in range(N):
            if math.hypot(x + 0.5 - mx, y + 0.5 - my) <= mr and math.hypot(x + 0.5 - CX, y + 0.5 - CY) <= R - 1:
                put(x, y, mix(img[y][x], (120, 12, 18), 0.55))
for y in range(N):
    for x in range(N):
        d = math.hypot(x + 0.5 - CX, y + 0.5 - CY)
        if R - 3 < d <= R - 1.5 and (x - CX) + (y - CY) < -12:
            put(x, y, mix(img[y][x], (255, 140, 110), 0.5))

# --- Foreground silhouettes ---
SIL = (8, 2, 4)
EDGE = (60, 6, 10)  # rim light from the moon on silhouette tops

# Rolling ground line
ground = [52 + round(1.6 * math.sin(x / 7.0) + 1.1 * math.sin(x / 3.3 + 1)) for x in range(N)]


def sprite(art, ox, oy, color=SIL):
    for dy, row in enumerate(art):
        for dx, ch in enumerate(row):
            if ch == '#':
                put(ox + dx, oy + dy, color)


ZOMBIE = [  # side view facing right, arms out, mid-stride
    ".######......",
    ".######......",
    ".######......",
    ".####E#......",
    ".######......",
    ".######......",
    "..####.......",
    "..##########.",
    "..##########.",
    "..####.......",
    "..####.......",
    "..####.......",
    "..####.......",
    "..####.......",
    "..####.......",
    "..####.......",
    ".##..##......",
    ".##..##......",
    "##....##.....",
    "##....##.....",
]
ZOMBIE_L = [row[::-1] for row in ZOMBIE]
CREEPER = [
    "########",
    "########",
    "#E####E#",
    "########",
    "###..###",
    "###..###",
    "########",
    "..####..",
    "..####..",
    "..####..",
    "..####..",
    "..####..",
    "..####..",
    "########",
    "########",
]
SPIDER = [
    "....######....",
    "#..########..#",
    ".#.#E####E#.#.",
    "..##########..",
    ".#.########.#.",
    "#..#......#..#",
]
EYES = (255, 70, 40)


def mob(art, ox, oy):
    for dy, row in enumerate(art):
        for dx, ch in enumerate(row):
            if ch == '#':
                put(ox + dx, oy + dy, SIL)
            elif ch == 'E':
                put(ox + dx, oy + dy, EYES)


# Cobblestone pillar (one block wide, like the zombies build) with a zombie standing on top,
# right in front of the moon: the mod's signature move
ZOMBIE_STAND = ZOMBIE[:16] + ["..####......."] * 4
PX, PW, BLOCK, PTOP = 29, 6, 6, 34
STONES = [  # 6x6 cobblestone texture: 0 = mortar, 1 = dark stone, 2 = light stone
    "211022",
    "211012",
    "001100",
    "120221",
    "220211",
    "100011",
]
STONE_COLORS = {"0": (34, 10, 12), "1": (96, 38, 38), "2": (134, 60, 56)}
for y in range(PTOP, N):
    for x in range(PX, PX + PW):
        lx, ly = (x - PX) % BLOCK, (y - PTOP) % BLOCK
        flip = ((y - PTOP) // BLOCK) % 2  # vary the pattern per block
        c = STONE_COLORS[STONES[ly][(BLOCK - 1 - lx) if flip else lx]]
        if x == PX + PW - 1:
            c = mix(c, (0, 0, 0), 0.45)  # shaded side
        put(x, y, c)
mob(ZOMBIE_STAND, PX - 2, PTOP - 20)

# Ground fill
for x in range(N):
    for y in range(ground[x], N):
        put(x, y, SIL)

# The horde on the ground
mob(ZOMBIE, 3, ground[6] - 20)
mob(CREEPER, 16, ground[19] - 15)
mob(ZOMBIE_L, 48, ground[52] - 20)
mob(SPIDER, 37, ground[43] - 6)

# Moon rim light: any silhouette pixel with brighter sky directly above gets a red edge
snapshot = [row[:] for row in img]
for y in range(1, N):
    for x in range(N):
        if snapshot[y][x] == SIL and snapshot[y - 1][x] != SIL and sum(snapshot[y - 1][x]) > 60:
            put(x, y, EDGE)

# Red fog hugging the ground
for y in range(46, 56):
    for x in range(N):
        if (x * 3 + y * 5) % 7 == 0 and img[y][x] != SIL:
            put(x, y, mix(img[y][x], (150, 30, 30), 0.35))

# Dark vignette frame so it pops as a square tile
for y in range(N):
    for x in range(N):
        edge = min(x, y, N - 1 - x, N - 1 - y)
        if edge == 0:
            put(x, y, mix(img[y][x], (0, 0, 0), 0.6))
        elif edge == 1:
            put(x, y, mix(img[y][x], (0, 0, 0), 0.3))


def write_png(path, scale):
    size = N * scale
    raw = bytearray()
    for y in range(size):
        raw.append(0)
        row = img[y // scale]
        for x in range(size):
            raw.extend(row[x // scale])

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 2, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)


write_png(os.path.join(ROOT, "art", "logo-512.png"), 8)
write_png(os.path.join(ROOT, "src", "main", "resources", "assets", "bloodmoonevents", "icon.png"), 2)
print("wrote art/logo-512.png and assets/bloodmoonevents/icon.png")
