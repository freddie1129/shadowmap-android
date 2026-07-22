"""Generate indexed, unit-size GLB primitives for Mapbox sky model layers."""

import json
import math
import os
import struct

ASSET_DIRECTORY = "app/src/main/assets"
SEGMENT_RADIAL_SEGMENTS = 12
SPHERE_LATITUDE_SEGMENTS = 12
SPHERE_LONGITUDE_SEGMENTS = 24


def write_glb(filename, positions, normals, indices):
    if len(positions) != len(normals):
        raise ValueError(f"{filename}: positions and normals must have equal counts")
    if not positions or not indices or len(indices) % 3:
        raise ValueError(f"{filename}: indexed triangle geometry is required")
    if min(indices) < 0 or max(indices) >= len(positions):
        raise ValueError(f"{filename}: index is outside the vertex buffer")
    if len(positions) > 65535:
        raise ValueError(f"{filename}: primitive exceeds unsigned-short index capacity")
    position_values = [value for point in positions for value in point]
    normal_values = [value for point in normals for value in point]
    position_bytes = struct.pack(f"<{len(position_values)}f", *position_values)
    normal_bytes = struct.pack(f"<{len(normal_values)}f", *normal_values)
    index_bytes = struct.pack(f"<{len(indices)}H", *indices)
    binary = bytearray()
    views = []

    def add_buffer(data, target):
        while len(binary) % 4:
            binary.append(0)
        view = {
            "buffer": 0,
            "byteOffset": len(binary),
            "byteLength": len(data),
            "target": target,
        }
        views.append(view)
        binary.extend(data)
        return len(views) - 1

    position_view = add_buffer(position_bytes, 34962)
    normal_view = add_buffer(normal_bytes, 34962)
    index_view = add_buffer(index_bytes, 34963)
    minimum = [min(point[axis] for point in positions) for axis in range(3)]
    maximum = [max(point[axis] for point in positions) for axis in range(3)]
    document = {
        "asset": {"version": "2.0", "generator": "shadowplanner-sky-primitives"},
        "buffers": [{"byteLength": len(binary)}],
        "bufferViews": views,
        "accessors": [
            {
                "bufferView": position_view,
                "componentType": 5126,
                "count": len(positions),
                "type": "VEC3",
                "min": minimum,
                "max": maximum,
            },
            {
                "bufferView": normal_view,
                "componentType": 5126,
                "count": len(normals),
                "type": "VEC3",
            },
            {
                "bufferView": index_view,
                "componentType": 5123,
                "count": len(indices),
                "type": "SCALAR",
                "min": [min(indices)],
                "max": [max(indices)],
            },
        ],
        "materials": [
            {
                "doubleSided": True,
                "pbrMetallicRoughness": {
                    "baseColorFactor": [1.0, 1.0, 1.0, 1.0],
                    "metallicFactor": 0.0,
                    "roughnessFactor": 1.0,
                },
                "emissiveFactor": [1.0, 1.0, 1.0],
            }
        ],
        "meshes": [
            {
                "primitives": [
                    {
                        "attributes": {"POSITION": 0, "NORMAL": 1},
                        "indices": 2,
                        "material": 0,
                        "mode": 4,
                    }
                ]
            }
        ],
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
    total_length = 12 + 8 + len(encoded) + 8 + len(binary)
    glb = struct.pack("<4sII", b"glTF", 2, total_length)
    glb += struct.pack("<I4s", len(encoded), b"JSON") + encoded
    glb += struct.pack("<I4s", len(binary), b"BIN\0") + binary
    os.makedirs(ASSET_DIRECTORY, exist_ok=True)
    with open(os.path.join(ASSET_DIRECTORY, filename), "wb") as output:
        output.write(glb)


def segment_geometry():
    positions = []
    normals = []
    indices = []
    for y in (-0.5, 0.5):
        for index in range(SEGMENT_RADIAL_SEGMENTS):
            angle = 2.0 * math.pi * index / SEGMENT_RADIAL_SEGMENTS
            normal = (math.cos(angle), 0.0, math.sin(angle))
            positions.append((normal[0] * 0.5, y, normal[2] * 0.5))
            normals.append(normal)
    for index in range(SEGMENT_RADIAL_SEGMENTS):
        next_index = (index + 1) % SEGMENT_RADIAL_SEGMENTS
        lower = index
        upper = index + SEGMENT_RADIAL_SEGMENTS
        lower_next = next_index
        upper_next = next_index + SEGMENT_RADIAL_SEGMENTS
        indices.extend((lower, lower_next, upper, upper, lower_next, upper_next))
    for y, normal_y, reverse in ((-0.5, -1.0, True), (0.5, 1.0, False)):
        center_index = len(positions)
        positions.append((0.0, y, 0.0))
        normals.append((0.0, normal_y, 0.0))
        ring_start = len(positions)
        for index in range(SEGMENT_RADIAL_SEGMENTS):
            angle = 2.0 * math.pi * index / SEGMENT_RADIAL_SEGMENTS
            positions.append((math.cos(angle) * 0.5, y, math.sin(angle) * 0.5))
            normals.append((0.0, normal_y, 0.0))
        for index in range(SEGMENT_RADIAL_SEGMENTS):
            current = ring_start + index
            following = ring_start + (index + 1) % SEGMENT_RADIAL_SEGMENTS
            indices.extend(
                (center_index, following, current) if reverse else
                (center_index, current, following)
            )
    return positions, normals, indices


def sphere_geometry():
    positions = []
    normals = []
    indices = []
    for latitude_index in range(SPHERE_LATITUDE_SEGMENTS + 1):
        latitude = math.pi * latitude_index / SPHERE_LATITUDE_SEGMENTS
        y = math.cos(latitude)
        horizontal = math.sin(latitude)
        for longitude_index in range(SPHERE_LONGITUDE_SEGMENTS + 1):
            longitude = 2.0 * math.pi * longitude_index / SPHERE_LONGITUDE_SEGMENTS
            point = (
                math.cos(longitude) * horizontal,
                y,
                math.sin(longitude) * horizontal,
            )
            positions.append(point)
            normals.append(point)
    for latitude_index in range(SPHERE_LATITUDE_SEGMENTS):
        for longitude_index in range(SPHERE_LONGITUDE_SEGMENTS):
            first = latitude_index * (SPHERE_LONGITUDE_SEGMENTS + 1) + longitude_index
            second = first + SPHERE_LONGITUDE_SEGMENTS + 1
            indices.extend((first, second, first + 1, first + 1, second, second + 1))
    return positions, normals, indices


write_glb("scene_sun_segment.glb", *segment_geometry())
write_glb("scene_sun_sphere.glb", *sphere_geometry())
