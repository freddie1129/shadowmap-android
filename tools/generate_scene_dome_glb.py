"""Generate the app-owned static sky dome with an embedded compass dial."""

import io
import json
import math
import os
import struct

try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError as error:
    raise SystemExit(
        "Pillow is required to generate scene_dome.glb: python3 -m pip install Pillow"
    ) from error

DOME_SEGMENTS = 180
TEXTURE_SIZE = 2048
TEXTURE_RENDER_SCALE = 2
DESIGN_TEXTURE_SIZE = 1024
GRID_HALF_WIDTH = 0.004
DISK_RADIUS = 1.22
GRID_POSITIONS = []
GRID_NORMALS = []

def dome_point(azimuth_degrees, altitude_degrees):
    azimuth = math.radians(azimuth_degrees)
    altitude = math.radians(altitude_degrees)
    return (
        math.sin(azimuth) * math.cos(altitude),
        math.sin(altitude),
        -math.cos(azimuth) * math.cos(altitude),
    )


def add_ribbon(a, b):
    dx, _, dz = b[0] - a[0], b[1] - a[1], b[2] - a[2]
    length = math.hypot(dx, dz)
    nx, nz = ((GRID_HALF_WIDTH, 0.0) if length < 0.0001 else
              (-dz / length * GRID_HALF_WIDTH, dx / length * GRID_HALF_WIDTH))
    a0, a1 = (a[0] - nx, a[1], a[2] - nz), (a[0] + nx, a[1], a[2] + nz)
    b0, b1 = (b[0] - nx, b[1], b[2] - nz), (b[0] + nx, b[1], b[2] + nz)
    for vertex in (a0, b0, a1, a1, b0, b1):
        GRID_POSITIONS.extend(vertex)
        GRID_NORMALS.extend((0.0, 1.0, 0.0))


for altitude in (0, 15, 30, 45, 60, 75):
    for index in range(DOME_SEGMENTS):
        add_ribbon(
            dome_point(index * 360 / DOME_SEGMENTS, altitude),
            dome_point((index + 1) * 360 / DOME_SEGMENTS, altitude),
        )
for azimuth in range(0, 360, 15):
    for index in range(18):
        add_ribbon(dome_point(azimuth, index * 5), dome_point(azimuth, (index + 1) * 5))


def render_pixels(design_pixels):
    return round(
        design_pixels * TEXTURE_SIZE / DESIGN_TEXTURE_SIZE * TEXTURE_RENDER_SCALE
    )


def draw_rotated_text(image, text, center_x, center_y, font_size, rotation_degrees):
    font = ImageFont.load_default(size=font_size)
    stroke_width = max(2, round(font_size * 0.075))
    bounds = font.getbbox(text, stroke_width=stroke_width)
    padding = stroke_width * 2
    width = bounds[2] - bounds[0] + padding * 2
    height = bounds[3] - bounds[1] + padding * 2
    label = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    ImageDraw.Draw(label).text(
        (width / 2, height / 2),
        text,
        font=font,
        anchor="mm",
        fill=(255, 246, 218, 255),
        stroke_width=stroke_width,
        stroke_fill=(24, 24, 24, 220),
    )
    rotated = label.rotate(
        -rotation_degrees,
        resample=Image.Resampling.BICUBIC,
        expand=True,
    )
    image.alpha_composite(
        rotated,
        (
            round(center_x - rotated.width / 2),
            round(center_y - rotated.height / 2),
        ),
    )


def compass_png():
    render_size = TEXTURE_SIZE * TEXTURE_RENDER_SCALE
    image = Image.new("RGBA", (render_size, render_size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    center = render_size // 2
    outer_radius = center - render_pixels(2)
    ring_radius = round(center / DISK_RADIUS)
    draw.ellipse(
        (
            center - outer_radius,
            center - outer_radius,
            center + outer_radius,
            center + outer_radius,
        ),
        fill=(214, 220, 224, 58),
    )
    # Keep the map visible beneath the dome and tint only the compass area outside it.
    draw.ellipse(
        (
            center - ring_radius,
            center - ring_radius,
            center + ring_radius,
            center + ring_radius,
        ),
        fill=(0, 0, 0, 0),
    )

    for azimuth in range(0, 360, 5):
        cardinal = azimuth in (0, 90, 180, 270)
        major = azimuth % 15 == 0
        tick_length = render_pixels(33 if cardinal else (23 if major else 12))
        tick_width = render_pixels(4 if cardinal else (3 if major else 2))
        angle = math.radians(azimuth)
        direction = (math.sin(angle), -math.cos(angle))
        start = (round(center + direction[0] * ring_radius),
                 round(center + direction[1] * ring_radius))
        end = (round(center + direction[0] * (ring_radius + tick_length)),
               round(center + direction[1] * (ring_radius + tick_length)))
        draw.line((start, end), fill=(255, 222, 138, 220), width=tick_width)
        if major:
            label = {0: "N", 90: "E", 180: "S", 270: "W"}.get(azimuth, f"{azimuth}°")
            label_radius = ring_radius + tick_length + render_pixels(27 if cardinal else 24)
            draw_rotated_text(
                image,
                label,
                center + direction[0] * label_radius,
                center + direction[1] * label_radius,
                render_pixels(44 if cardinal else 28),
                azimuth,
            )

    image = image.resize(
        (TEXTURE_SIZE, TEXTURE_SIZE),
        resample=Image.Resampling.LANCZOS,
    )
    output = io.BytesIO()
    image.save(output, format="PNG", optimize=True)
    return output.getvalue()


grid_positions = struct.pack(f"<{len(GRID_POSITIONS)}f", *GRID_POSITIONS)
grid_normals = struct.pack(f"<{len(GRID_NORMALS)}f", *GRID_NORMALS)
grid_vertex_count = len(GRID_POSITIONS) // 3
grid_indices = struct.pack(f"<{grid_vertex_count}H", *range(grid_vertex_count))

disk_positions = [(0.0, -0.02, 0.0)]
disk_uvs = [(0.5, 0.5)]
for index in range(DOME_SEGMENTS + 1):
    angle = index * 2 * math.pi / DOME_SEGMENTS
    x, z = DISK_RADIUS * math.sin(angle), -DISK_RADIUS * math.cos(angle)
    disk_positions.append((x, -0.02, z))
    disk_uvs.append(((x / DISK_RADIUS + 1) / 2, (z / DISK_RADIUS + 1) / 2))
disk_normals = [(0.0, 1.0, 0.0)] * len(disk_positions)
disk_indices = []
for index in range(DOME_SEGMENTS):
    disk_indices.extend((0, index + 1, index + 2))

binary = bytearray()
buffer_views = []


def add_buffer(data, target=None):
    while len(binary) % 4:
        binary.append(0)
    view = {"buffer": 0, "byteOffset": len(binary), "byteLength": len(data)}
    if target is not None:
        view["target"] = target
    buffer_views.append(view)
    binary.extend(data)
    return len(buffer_views) - 1


grid_position_view = add_buffer(grid_positions, 34962)
grid_normal_view = add_buffer(grid_normals, 34962)
grid_index_view = add_buffer(grid_indices, 34963)
disk_position_bytes = struct.pack(f"<{len(disk_positions) * 3}f", *(value for point in disk_positions for value in point))
disk_normal_bytes = struct.pack(f"<{len(disk_normals) * 3}f", *(value for point in disk_normals for value in point))
disk_uv_bytes = struct.pack(f"<{len(disk_uvs) * 2}f", *(value for point in disk_uvs for value in point))
disk_index_bytes = struct.pack(f"<{len(disk_indices)}H", *disk_indices)
disk_position_view = add_buffer(disk_position_bytes, 34962)
disk_normal_view = add_buffer(disk_normal_bytes, 34962)
disk_uv_view = add_buffer(disk_uv_bytes, 34962)
disk_index_view = add_buffer(disk_index_bytes, 34963)
image_view = add_buffer(compass_png())

grid_points = list(zip(GRID_POSITIONS[0::3], GRID_POSITIONS[1::3], GRID_POSITIONS[2::3]))
accessors = [
    {"bufferView": grid_position_view, "componentType": 5126, "count": grid_vertex_count,
     "type": "VEC3", "min": [min(values) for values in zip(*grid_points)],
     "max": [max(values) for values in zip(*grid_points)]},
    {"bufferView": grid_normal_view, "componentType": 5126, "count": grid_vertex_count, "type": "VEC3"},
    {"bufferView": grid_index_view, "componentType": 5123, "count": grid_vertex_count,
     "type": "SCALAR", "min": [0], "max": [grid_vertex_count - 1]},
    {"bufferView": disk_position_view, "componentType": 5126, "count": len(disk_positions),
     "type": "VEC3", "min": [-DISK_RADIUS, -0.02, -DISK_RADIUS],
     "max": [DISK_RADIUS, -0.02, DISK_RADIUS]},
    {"bufferView": disk_normal_view, "componentType": 5126, "count": len(disk_normals), "type": "VEC3"},
    {"bufferView": disk_uv_view, "componentType": 5126, "count": len(disk_uvs), "type": "VEC2"},
    {"bufferView": disk_index_view, "componentType": 5123, "count": len(disk_indices),
     "type": "SCALAR", "min": [0], "max": [len(disk_positions) - 1]},
]

document = {
    "asset": {"version": "2.0", "generator": "shadowplanner"},
    "buffers": [{"byteLength": len(binary)}],
    "bufferViews": buffer_views,
    "accessors": accessors,
    "images": [{"bufferView": image_view, "mimeType": "image/png"}],
    "samplers": [{"magFilter": 9729, "minFilter": 9987, "wrapS": 33071, "wrapT": 33071}],
    "textures": [{"sampler": 0, "source": 0}],
    "materials": [
        {"alphaMode": "BLEND", "doubleSided": True,
         "pbrMetallicRoughness": {"baseColorFactor": [1, 0.72, 0.2, 0.48], "metallicFactor": 0,
                                   "roughnessFactor": 1}, "emissiveFactor": [0.35, 0.2, 0.02]},
        {"alphaMode": "BLEND", "doubleSided": True,
         "pbrMetallicRoughness": {"baseColorTexture": {"index": 0}, "metallicFactor": 0,
                                   "roughnessFactor": 1}, "emissiveTexture": {"index": 0}},
    ],
    "meshes": [{"primitives": [
        {"attributes": {"POSITION": 0, "NORMAL": 1}, "indices": 2, "mode": 4, "material": 0},
        {"attributes": {"POSITION": 3, "NORMAL": 4, "TEXCOORD_0": 5}, "indices": 6,
         "mode": 4, "material": 1},
    ]}],
    "nodes": [{"mesh": 0}],
    "scenes": [{"nodes": [0]}],
    "scene": 0,
}
while len(binary) % 4:
    binary.append(0)
document["buffers"][0]["byteLength"] = len(binary)
encoded = json.dumps(document, separators=(",", ":")).encode("utf-8")
while len(encoded) % 4:
    encoded += b" "
glb = struct.pack("<4sII", b"glTF", 2, 12 + 8 + len(encoded) + 8 + len(binary))
glb += struct.pack("<I4s", len(encoded), b"JSON") + encoded
glb += struct.pack("<I4s", len(binary), b"BIN\0") + binary
os.makedirs("app/src/main/assets", exist_ok=True)
with open("app/src/main/assets/scene_dome.glb", "wb") as output:
    output.write(glb)
