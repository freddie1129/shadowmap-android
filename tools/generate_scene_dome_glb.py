"""Generate the app-owned, unit-radius static sky guide GLB.

The Mapbox model layer scales this asset in metres at runtime. Dynamic solar elements are kept
out of the asset because their geometry changes with the selected time.
"""
import json
import math
import os
import struct

SEGMENTS = 96
vertices = []
normals = []

def add_line(a, b):
    """Emit a thin, two-sided triangle ribbon; Mapbox model layers do not support GLTF LINES."""
    dx, _, dz = b[0] - a[0], b[1] - a[1], b[2] - a[2]
    length = math.hypot(dx, dz)
    if length < 0.0001:
        nx, nz = 0.004, 0.0
    else:
        nx, nz = -dz / length * 0.004, dx / length * 0.004
    a0, a1 = (a[0] - nx, a[1], a[2] - nz), (a[0] + nx, a[1], a[2] + nz)
    b0, b1 = (b[0] - nx, b[1], b[2] - nz), (b[0] + nx, b[1], b[2] + nz)
    ribbon = (a0, b0, a1, a1, b0, b1)
    for vertex in ribbon:
        vertices.extend(vertex)
        # The guide is double-sided and unlit in practice, but Mapbox's model path expects
        # a valid NORMAL accessor for PBR model primitives.
        normals.extend((0.0, 1.0, 0.0))

def point(azimuth, altitude):
    azimuth = math.radians(azimuth)
    altitude = math.radians(altitude)
    return (
        math.sin(azimuth) * math.cos(altitude),
        math.sin(altitude),
        -math.cos(azimuth) * math.cos(altitude),
    )

for altitude in (0, 15, 30, 45, 60, 75):
    for index in range(SEGMENTS):
        add_line(point(index * 360 / SEGMENTS, altitude), point((index + 1) * 360 / SEGMENTS, altitude))
for azimuth in range(0, 360, 15):
    for index in range(12):
        add_line(point(azimuth, index * 90 / 12), point(azimuth, (index + 1) * 90 / 12))
# Compass ring and cardinal ticks are slightly below the horizon to prevent z-fighting.
for index in range(SEGMENTS):
    angle = index * 2 * math.pi / SEGMENTS
    next_angle = (index + 1) * 2 * math.pi / SEGMENTS
    add_line((1.12 * math.sin(angle), -0.015, -1.12 * math.cos(angle)), (1.12 * math.sin(next_angle), -0.015, -1.12 * math.cos(next_angle)))
for azimuth in (0, 90, 180, 270):
    angle = math.radians(azimuth)
    add_line((1.04 * math.sin(angle), -0.015, -1.04 * math.cos(angle)), (1.20 * math.sin(angle), -0.015, -1.20 * math.cos(angle)))

positions = struct.pack("<%sf" % len(vertices), *vertices)
normal_bytes = struct.pack("<%sf" % len(normals), *normals)
vertex_count = len(vertices) // 3
index_values = list(range(vertex_count))
index_bytes = struct.pack("<%sH" % vertex_count, *index_values)
position_triplets = list(zip(vertices[0::3], vertices[1::3], vertices[2::3]))
position_min = [min(values) for values in zip(*position_triplets)]
position_max = [max(values) for values in zip(*position_triplets)]
payload = positions + normal_bytes + index_bytes
while len(payload) % 4:
    payload += b"\0"
document = {
    "asset": {"version": "2.0", "generator": "shadowplanner"},
    "buffers": [{"byteLength": len(payload)}],
    "bufferViews": [{"buffer": 0, "byteOffset": 0, "byteLength": len(positions), "target": 34962}, {"buffer": 0, "byteOffset": len(positions), "byteLength": len(normal_bytes), "target": 34962}, {"buffer": 0, "byteOffset": len(positions) + len(normal_bytes), "byteLength": len(index_bytes), "target": 34963}],
    "accessors": [{"bufferView": 0, "componentType": 5126, "count": vertex_count, "type": "VEC3", "min": position_min, "max": position_max}, {"bufferView": 1, "componentType": 5126, "count": vertex_count, "type": "VEC3"}, {"bufferView": 2, "componentType": 5123, "count": vertex_count, "type": "SCALAR", "min": [0], "max": [vertex_count - 1]}],
    "materials": [{"alphaMode": "BLEND", "doubleSided": True, "pbrMetallicRoughness": {"baseColorFactor": [1, 0.72, 0.2, 0.48], "metallicFactor": 0, "roughnessFactor": 1}, "emissiveFactor": [0.35, 0.2, 0.02]}],
    "meshes": [{"primitives": [{"attributes": {"POSITION": 0, "NORMAL": 1}, "indices": 2, "mode": 4, "material": 0}]}],
    "nodes": [{"mesh": 0}],
    "scenes": [{"nodes": [0]}],
    "scene": 0,
}
encoded = json.dumps(document, separators=(",", ":")).encode("utf-8")
while len(encoded) % 4:
    encoded += b" "
glb = struct.pack("<4sII", b"glTF", 2, 12 + 8 + len(encoded) + 8 + len(payload))
glb += struct.pack("<I4s", len(encoded), b"JSON") + encoded
glb += struct.pack("<I4s", len(payload), b"BIN\0") + payload
os.makedirs("app/src/main/assets", exist_ok=True)
with open("app/src/main/assets/scene_dome.glb", "wb") as output:
    output.write(glb)
