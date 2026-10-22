package com.example.genpox.map.tile

class FakeTileDataProvider : TileDataProvider {
    val requestedCoordinates = mutableListOf<TileCoordinate>()
    val tiles = mutableMapOf<TileCoordinate, TileGeometry>()

    override suspend fun getTile(z: Int, x: Int, y: Int): TileGeometry? {
        val coord = TileCoordinate(z, x, y)
        requestedCoordinates.add(coord)
        return tiles[coord] ?: TileGeometry(coord, emptyList(), emptyList())
    }

    override suspend fun getTiles(coordinates: List<TileCoordinate>): Map<TileCoordinate, TileGeometry> {
        requestedCoordinates.addAll(coordinates)
        return coordinates.associateWith { coord ->
            tiles[coord] ?: TileGeometry(coord, emptyList(), emptyList())
        }
    }
}
