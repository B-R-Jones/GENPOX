package com.example.genpox.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.genpox.audio.PoxSynthManager
import com.example.genpox.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class ScannerViewModel(
    private val repository: DataRepository,
    private val synthManager: PoxSynthManager,
    val reactorSalt: StateFlow<Float>
) : ViewModel() {

    var onLog: ((String) -> Unit)? = null

    private fun addLog(message: String) {
        onLog?.invoke(message)
    }

    // State flows from Repository
    val activeMissions: StateFlow<List<HarvestMission>> = repository.activeMissions
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val harvestingCreatureIds: StateFlow<Set<String>> = repository.activeMissions
        .map { missions -> missions.filter { !it.isReturned }.map { it.creatureId }.toSet() }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val activeMissionCoords: StateFlow<Set<String>> = repository.activeMissions
        .map { missions -> missions.filter { !it.isReturned }.map { "${it.lat},${it.lng}" }.toSet() }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val allMissions: StateFlow<List<HarvestMission>> = repository.allMissions
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val depletedAnomalyCoords: StateFlow<Set<String>> = repository.allMissions
        .map { missions -> missions.filter { it.isReturned }.map { "${it.lat},${it.lng}" }.toSet() }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val scanRadius: StateFlow<Float> = repository.scanRadius
        .stateIn(viewModelScope, SharingStarted.Eagerly, 55f)

    // State Variables
    private val _trackedMissionId = MutableStateFlow<String?>(null)
    val trackedMissionId: StateFlow<String?> = _trackedMissionId.asStateFlow()

    private val _selectedAnomalyId = MutableStateFlow<String?>(null)
    val selectedAnomalyId: StateFlow<String?> = _selectedAnomalyId.asStateFlow()

    private val _scannerSubTab = MutableStateFlow("radar")
    val scannerSubTab: StateFlow<String> = _scannerSubTab.asStateFlow()

    private val _listSubTab = MutableStateFlow("anomalies")
    val listSubTab: StateFlow<String> = _listSubTab.asStateFlow()

    private val _latitude = MutableStateFlow(37.4220)
    val latitude: StateFlow<Double> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow(-122.0841)
    val longitude: StateFlow<Double> = _longitude.asStateFlow()

    private val _zoomMultiplier = MutableStateFlow(1.0f)
    val zoomMultiplier: StateFlow<Float> = _zoomMultiplier.asStateFlow()

    private val _roads = MutableStateFlow<List<List<Pair<Double, Double>>>>(emptyList())
    val roads: StateFlow<List<List<Pair<Double, Double>>>> = _roads.asStateFlow()

    private val _buildings = MutableStateFlow<List<BuildingStructure>>(emptyList())
    val buildings: StateFlow<List<BuildingStructure>> = _buildings.asStateFlow()

    private val _proceduralHeatwaves = MutableStateFlow<List<String>>(emptyList())
    val proceduralHeatwaves: StateFlow<List<String>> = _proceduralHeatwaves.asStateFlow()

    private val _rawAnomalies = MutableStateFlow<List<PoxAnomaly>>(emptyList())
    val rawAnomalies: StateFlow<List<PoxAnomaly>> = _rawAnomalies.asStateFlow()

    private val _celestialWells = MutableStateFlow<List<CelestialWell>>(emptyList())
    val celestialWells: StateFlow<List<CelestialWell>> = _celestialWells.asStateFlow()

    private val _showAnomaliesOnMap = MutableStateFlow(true)
    val showAnomaliesOnMap: StateFlow<Boolean> = _showAnomaliesOnMap.asStateFlow()

    private val _showFontsOnMap = MutableStateFlow(true)
    val showFontsOnMap: StateFlow<Boolean> = _showFontsOnMap.asStateFlow()

    private val _showWellsOnMap = MutableStateFlow(true)
    val showWellsOnMap: StateFlow<Boolean> = _showWellsOnMap.asStateFlow()

    private val _cachedCells = MutableStateFlow<List<String>>(emptyList())

    private val _reactorSalt = MutableStateFlow(0.05f)

    // Map anomalies combining logic (pinned + normal)
    val anomalies: StateFlow<List<PoxAnomaly>> = combine(
        _latitude, _longitude, _rawAnomalies, activeMissions
    ) { userLat, userLng, rawList, ongoingMissions ->
        val pinnedAnomalies = ongoingMissions.filter { !it.isReturned }.map { m ->
            val animId = "ANM-ACTIVE-${m.creatureId}"
            val animName = "Active Anomaly"
            val dist = calculateDistanceInFeet(userLat, userLng, m.lat, m.lng)
            PoxAnomaly(
                id = animId,
                name = animName,
                lat = m.lat,
                lng = m.lng,
                gene = m.harvestedPayload.firstOrNull() ?: "AGTCGTAC",
                faction = m.creatureFaction,
                distance = dist,
                heatZoneDiameter = 500.0 * 3.28084,
                density = 0.0
            )
        }
        val remaining = rawList.filter { raw -> pinnedAnomalies.none { pinned -> Math.abs(pinned.lat - raw.lat) < 0.0001 && Math.abs(pinned.lng - raw.lng) < 0.0001 } }

        val updatedPinned = pinnedAnomalies.map {
            it.copy(distance = calculateDistanceInFeet(userLat, userLng, it.lat, it.lng))
        }
        val updatedRemaining = remaining.map {
            it.copy(distance = calculateDistanceInFeet(userLat, userLng, it.lat, it.lng))
        }

        updatedPinned + updatedRemaining
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val selectedAnomaly: StateFlow<PoxAnomaly?> = combine(selectedAnomalyId, anomalies) { id, list ->
        list.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var fetchRoadsJob: Job? = null
    private var lastCheckedCellKey = ""
    private var missionTickerJob: Job? = null

    init {
        viewModelScope.launch {
            reactorSalt.collect { _reactorSalt.value = it }
        }

        val startLat = _latitude.value
        val startLng = _longitude.value
        val ghDLat = 300.0 / 364000.0
        val ghDLng = -200.0 / (364000.0 * Math.cos(Math.toRadians(startLat)))
        val magDLat = -500.0 / 364000.0
        val magDLng = 400.0 / (364000.0 * Math.cos(Math.toRadians(startLat)))
        val gravDLat = 400.0 / 364000.0
        val gravDLng = 600.0 / (364000.0 * Math.cos(Math.toRadians(startLat)))

        _celestialWells.value = listOf(
            CelestialWell("WELL-GH", "Venusian Greenhouse Anomaly", startLat + ghDLat, startLng + ghDLng, 800.0, "GREENHOUSE", 1.0),
            CelestialWell("WELL-MAG", "Jovian Magnetospheric Anomaly", startLat + magDLat, startLng + magDLng, 1000.0, "MAGNETOSPHERIC", 1.0),
            CelestialWell("WELL-GRAV", "Asteroid Gravitational Anomaly", startLat + gravDLat, startLng + gravDLng, 600.0, "GRAVITATIONAL", 1.0)
        )

        loadCachedCells()
        startHeatwaveSimulation()
        startMissionTicker()
        generateNearbyAnomalies(startLat, startLng)
    }

    fun setTrackedMissionId(id: String?) {
        _trackedMissionId.value = id
        if (id != null) {
            addLog("LOCKED_TARGET: Coupled target ID $id")
        } else {
            addLog("LOCKED_TARGET: Released lock channels.")
        }
    }

    fun setSelectedAnomalyId(id: String?) {
        _selectedAnomalyId.value = id
    }

    fun setScannerSubTab(subTab: String) {
        _scannerSubTab.value = subTab
    }

    fun setListSubTab(subTab: String) {
        _listSubTab.value = subTab
    }

    fun toggleAnomaliesOnMap() {
        _showAnomaliesOnMap.value = !_showAnomaliesOnMap.value
    }

    fun toggleFontsOnMap() {
        _showFontsOnMap.value = !_showFontsOnMap.value
    }

    fun toggleWellsOnMap() {
        _showWellsOnMap.value = !_showWellsOnMap.value
    }

    fun updateLocation(lat: Double, lng: Double) {
        _latitude.value = lat
        _longitude.value = lng
        generateNearbyAnomalies(lat, lng)
        fetchRoadsIfNeeded(lat, lng)
    }

    fun updateZoom(zoom: Float) {
        if (_zoomMultiplier.value != zoom) {
            _zoomMultiplier.value = zoom
            fetchRoadsIfNeeded(_latitude.value, _longitude.value)
        }
    }

    fun refreshMap() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearMapCache()
            fetchRoads(_latitude.value, _longitude.value)
        }
    }

    private fun getCellKey(lat: Double, lng: Double): String {
        val cellX = Math.floor(lat / 0.015).toInt()
        val cellY = Math.floor(lng / 0.015).toInt()
        return "$cellX,$cellY"
    }

    private fun addCachedCellToState(cellKey: String) {
        _cachedCells.value = (_cachedCells.value + cellKey).distinct()
    }

    private fun loadCachedCells() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getAllCachedRoadCells()
            _cachedCells.value = list.map { it.cellKey }
        }
    }

    private fun startHeatwaveSimulation() {
        viewModelScope.launch(Dispatchers.Default) {
            val baseCities = listOf(
                Pair(37.4220, -122.0841), // Palo Alto
                Pair(40.7128, -74.0060),  // New York
                Pair(51.5074, -0.1278),   // London
                Pair(35.6762, 139.6503),  // Tokyo
                Pair(-33.8688, 151.2093)  // Sydney
            )
            var ticks = 0L
            while (true) {
                val list = mutableListOf<String>()
                baseCities.forEach { (baseLat, baseLng) ->
                    val cellX = Math.floor(baseLat / 0.015).toInt()
                    val cellY = Math.floor(baseLng / 0.015).toInt()

                    list.add("$cellX,$cellY")

                    val offset1X = (kotlin.math.sin(ticks * 0.1 + cellX) * 2).toInt()
                    val offset1Y = (kotlin.math.cos(ticks * 0.1 + cellY) * 2).toInt()
                    list.add("${cellX + offset1X},${cellY + offset1Y}")

                    val offset2X = (kotlin.math.cos(ticks * 0.07 - cellX) * 3).toInt()
                    val offset2Y = (kotlin.math.sin(ticks * 0.07 - cellY) * 3).toInt()
                    list.add("${cellX + offset2X},${cellY + offset2Y}")
                }
                _proceduralHeatwaves.value = list
                ticks++
                delay(3000)
            }
        }
    }

    private fun fetchRoadsIfNeeded(lat: Double, lng: Double) {
        val cellKey = getCellKey(lat, lng)
        if (cellKey != lastCheckedCellKey) {
            lastCheckedCellKey = cellKey
            fetchRoads(lat, lng)
        }
    }

    private fun generateFallbackRoads(lat: Double, lng: Double): List<List<RoadPoint>> {
        val result = mutableListOf<List<RoadPoint>>()
        val latStep = 0.00135
        val lngStep = 0.00135 / kotlin.math.cos(Math.toRadians(lat))

        val startLat = Math.floor(lat / latStep) * latStep - 37.0 * latStep
        val startLng = Math.floor(lng / lngStep) * lngStep - 37.0 * lngStep

        for (i in 0..74) {
            val rLat = startLat + i * latStep
            val path = mutableListOf<RoadPoint>()
            for (j in 0..74) {
                val rLng = startLng + j * lngStep
                path.add(RoadPoint(rLat, rLng))
            }
            result.add(path)
        }
        for (j in 0..74) {
            val rLng = startLng + j * lngStep
            val path = mutableListOf<RoadPoint>()
            for (i in 0..74) {
                val rLat = startLat + i * latStep
                path.add(RoadPoint(rLat, rLng))
            }
            result.add(path)
        }
        return result
    }

    private fun generateFallbackBuildings(lat: Double, lng: Double): List<List<RoadPoint>> {
        val result = mutableListOf<List<RoadPoint>>()
        val random = java.util.Random((lat.hashCode() + lng.hashCode()).toLong())
        val latStep = 0.00135
        val lngStep = 0.00135 / kotlin.math.cos(Math.toRadians(lat))

        val startLat = Math.floor(lat / latStep) * latStep - 15.0 * latStep
        val startLng = Math.floor(lng / lngStep) * lngStep - 15.0 * lngStep

        for (i in 0..30 step 2) {
            for (j in 0..30 step 2) {
                if (random.nextFloat() < 0.4f) {
                    val bLat = startLat + i * latStep + 0.0003
                    val bLng = startLng + j * lngStep + 0.0003

                    val w = 0.0002
                    val h = 0.0002 / kotlin.math.cos(Math.toRadians(lat))

                    val polygon = listOf(
                        RoadPoint(bLat, bLng),
                        RoadPoint(bLat + w, bLng),
                        RoadPoint(bLat + w, bLng + h),
                        RoadPoint(bLat, bLng + h),
                        RoadPoint(bLat, bLng)
                    )
                    result.add(polygon)
                }
            }
        }
        return result
    }

    private fun fetchRoads(lat: Double, lng: Double) {
        val oldJob = fetchRoadsJob
        fetchRoadsJob = viewModelScope.launch(Dispatchers.IO) {
            oldJob?.let {
                it.cancel()
                try {
                    it.join()
                } catch (e: Exception) {}
            }

            val cellX = Math.floor(lat / 0.015).toInt()
            val cellY = Math.floor(lng / 0.015).toInt()
            val radius = 3

            val cellsToQuery = mutableListOf<String>()
            for (dx in -radius..radius) {
                for (dy in -radius..radius) {
                    cellsToQuery.add("${cellX + dx},${cellY + dy}")
                }
            }

            val cachedRoadCells = mutableListOf<CachedRoadCell>()
            val cachedBuildingCells = mutableListOf<CachedBuildingCell>()
            val now = System.currentTimeMillis()
            var allCachedAndValid = true

            for (key in cellsToQuery) {
                val dbRoadCell = repository.getCachedRoadCell(key)
                val dbBuildingCell = repository.getCachedBuildingCell(key)
                if (dbRoadCell != null && dbBuildingCell != null &&
                    (now - dbRoadCell.fetchedAt) < 24 * 60 * 60 * 1000L &&
                    (now - dbBuildingCell.fetchedAt) < 24 * 60 * 60 * 1000L) {
                    cachedRoadCells.add(dbRoadCell)
                    cachedBuildingCells.add(dbBuildingCell)
                } else {
                    allCachedAndValid = false
                }
            }

            if (allCachedAndValid) {
                addLog("SYS: Loaded map grid from local cache.")
                val maxCellsToLoad = mutableListOf<String>()
                for (dx in -radius..radius) {
                    for (dy in -radius..radius) {
                        maxCellsToLoad.add("${cellX + dx},${cellY + dy}")
                    }
                }
                val allRoads = mutableListOf<List<Pair<Double, Double>>>()
                val allBuildings = mutableListOf<BuildingStructure>()
                maxCellsToLoad.forEach { key ->
                    val roadCell = repository.getCachedRoadCell(key)
                    val buildingCell = repository.getCachedBuildingCell(key)
                    if (roadCell != null && (now - roadCell.fetchedAt) < 24 * 60 * 60 * 1000L) {
                        try {
                            val roadsList = Json.decodeFromString<List<List<RoadPoint>>>(roadCell.roadsJson)
                            roadsList.forEach { road ->
                                allRoads.add(road.map { Pair(it.lat, it.lng) })
                            }
                        } catch (e: Exception) {}
                    }
                    if (buildingCell != null && (now - buildingCell.fetchedAt) < 24 * 60 * 60 * 1000L) {
                        try {
                            val buildingsList = Json.decodeFromString<List<BuildingStructure>>(buildingCell.buildingsJson)
                            allBuildings.addAll(buildingsList)
                        } catch (e: Exception) {}
                    }
                }
                _roads.value = allRoads.distinct()
                if (allRoads.isNotEmpty() && allBuildings.isEmpty()) {
                    val fallback = generateFallbackBuildings(lat, lng)
                    val fallbackStructures = fallback.map { BuildingStructure(points = it, isFallback = true) }
                    _buildings.value = fallbackStructures.distinct()
                } else {
                    _buildings.value = allBuildings.distinct()
                }
                return@launch
            }

            val minLat = (cellX - radius) * 0.015
            val maxLat = (cellX + radius + 1) * 0.015
            val minLng = (cellY - radius) * 0.015
            val maxLng = (cellY + radius + 1) * 0.015

            val endpoints = listOf(
                "https://overpass-api.de/api/interpreter",
                "https://lz4.overpass-api.de/api/interpreter",
                "https://overpass.osm.ch/api/interpreter",
                "https://overpass.openstreetmap.fr/api/interpreter",
                "https://overpass.kumi.systems/api/interpreter"
            )

            var success = false
            val query = "[out:json];(way($minLat,$minLng,$maxLat,$maxLng)[highway~\"motorway|trunk|primary|secondary|tertiary|unclassified|residential\"];way($minLat,$minLng,$maxLat,$maxLng)[building];);out geom;"
            var fetchedRoads: List<List<RoadPoint>> = emptyList()
            var fetchedBuildings: List<BuildingStructure> = emptyList()
            var femaBuildings: List<BuildingStructure> = emptyList()
            var usgsBuildings: List<BuildingStructure> = emptyList()

            try {
                addLog("SYS: Querying FEMA USA Structures (Primary)...")
                val femaUrl = "https://services2.arcgis.com/FiaPA4ga0iQKduv3/arcgis/rest/services/USA_Structures_View/FeatureServer/0/query" +
                        "?geometry=$minLng,$minLat,$maxLng,$maxLat" +
                        "&geometryType=esriGeometryEnvelope" +
                        "&spatialRel=esriSpatialRelIntersects" +
                        "&inSR=4326" +
                        "&outSR=4326" +
                        "&returnGeometry=true" +
                        "&outFields=OBJECTID" +
                        "&f=json"

                val url = java.net.URL(femaUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "GenPoxRadar/1.0 (brent@example.com)")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val text = connection.inputStream.bufferedReader().use { it.readText() }
                    femaBuildings = parseArcgisJson(text)
                    addLog("SYS: Loaded ${femaBuildings.size} buildings from FEMA.")
                } else {
                    addLog("SYS: FEMA query failed: HTTP $responseCode")
                }
            } catch (e: Exception) {
                addLog("SYS: FEMA query error: ${e.message}")
            }

            try {
                addLog("SYS: Querying USGS Structures (Secondary)...")
                val usgsUrl = "https://carto.nationalmap.gov/arcgis/rest/services/structures/FeatureServer/0/query" +
                        "?geometry=$minLng,$minLat,$maxLng,$maxLat" +
                        "&geometryType=esriGeometryEnvelope" +
                        "&spatialRel=esriSpatialRelIntersects" +
                        "&inSR=4326" +
                        "&outSR=4326" +
                        "&returnGeometry=true" +
                        "&outFields=OBJECTID" +
                        "&f=json"

                val url = java.net.URL(usgsUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "GenPoxRadar/1.0 (brent@example.com)")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val text = connection.inputStream.bufferedReader().use { it.readText() }
                    usgsBuildings = parseArcgisJson(text)
                    addLog("SYS: Loaded ${usgsBuildings.size} buildings from USGS.")
                } else {
                    addLog("SYS: USGS query failed: HTTP $responseCode")
                }
            } catch (e: Exception) {
                addLog("SYS: USGS query error: ${e.message}")
            }

            var osmBuildings: List<BuildingStructure> = emptyList()
            for (baseUrl in endpoints) {
                try {
                    addLog("SYS: Querying OpenStreetMap ($baseUrl)...")
                    val url = java.net.URL(baseUrl)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("User-Agent", "GenPoxRadar/1.0 (brent@example.com)")
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    connection.setRequestProperty("Accept", "*/*")
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.doOutput = true

                    val postData = "data=" + java.net.URLEncoder.encode(query, "UTF-8")
                    connection.outputStream.use { os ->
                        os.write(postData.toByteArray(java.nio.charset.StandardCharsets.UTF_8))
                    }

                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        val text = connection.inputStream.bufferedReader().use { it.readText() }
                        fetchedRoads = parseOverpassJson(text, "highway")
                        val rawBuildings = parseOverpassJson(text, "building")
                        osmBuildings = rawBuildings.map { BuildingStructure(points = it, isFallback = false) }

                        if (fetchedRoads.isNotEmpty() || osmBuildings.isNotEmpty()) {
                            addLog("SYS: Loaded map data from $baseUrl.")
                            success = true
                            break
                        }
                    } else {
                        addLog("SYS: Endpoint $baseUrl failed: HTTP $responseCode")
                    }
                } catch (e: Exception) {
                    addLog("SYS: Endpoint $baseUrl error: ${e.message}")
                }
            }

            fun getBBox(building: BuildingStructure): BuildingBBox {
                var minL = Double.MAX_VALUE
                var maxL = -Double.MAX_VALUE
                var minLn = Double.MAX_VALUE
                var maxLn = -Double.MAX_VALUE
                building.points.forEach { pt ->
                    if (pt.lat < minL) minL = pt.lat
                    if (pt.lat > maxL) maxL = pt.lat
                    if (pt.lng < minLn) minLn = pt.lng
                    if (pt.lng > maxLn) maxLn = pt.lng
                }
                return BuildingBBox(minL, maxL, minLn, maxLn)
            }

            val mergedBuildings = femaBuildings.toMutableList()
            val mergedBoxes = mutableListOf<BuildingBBox>()

            femaBuildings.forEach { b ->
                val box = getBBox(b)
                val dLat = box.maxLat - box.minLat
                val dLng = box.maxLng - box.minLng
                if (dLat in 0.0..0.005 && dLng in 0.0..0.005) {
                    mergedBoxes.add(box)
                }
            }

            usgsBuildings.forEach { usgs ->
                val usgsBox = getBBox(usgs)
                val dLat = usgsBox.maxLat - usgsBox.minLat
                val dLng = usgsBox.maxLng - usgsBox.minLng
                val intersects = mergedBoxes.any { box -> usgsBox.intersects(box) }
                if (!intersects) {
                    mergedBuildings.add(usgs)
                    if (dLat in 0.0..0.005 && dLng in 0.0..0.005) {
                        mergedBoxes.add(usgsBox)
                    }
                }
            }

            if (success && osmBuildings.isNotEmpty()) {
                osmBuildings.forEach { osm ->
                    val osmBox = getBBox(osm)
                    val intersectingBox = mergedBoxes.find { box -> osmBox.intersects(box) }
                    if (intersectingBox == null) {
                        mergedBuildings.add(osm)
                        val dLat = osmBox.maxLat - osmBox.minLat
                        val dLng = osmBox.maxLng - osmBox.minLng
                        if (dLat in 0.0..0.005 && dLng in 0.0..0.005) {
                            mergedBoxes.add(osmBox)
                        }
                    }
                }
            }
            fetchedBuildings = mergedBuildings

            if (!success) {
                addLog("SYS: All OpenStreetMap endpoints failed. Generating fallback grid.")
                fetchedRoads = generateFallbackRoads(lat, lng)
                if (fetchedBuildings.isEmpty()) {
                    fetchedBuildings = generateFallbackBuildings(lat, lng).map { BuildingStructure(points = it, isFallback = true) }
                }
            }

            if (success && fetchedBuildings.isEmpty()) {
                fetchedBuildings = generateFallbackBuildings(lat, lng).map { BuildingStructure(points = it, isFallback = true) }
            }

            val cellRoadsMap = mutableMapOf<String, MutableList<List<RoadPoint>>>()
            val cellBuildingsMap = mutableMapOf<String, MutableList<BuildingStructure>>()
            cellsToQuery.forEach { key ->
                cellRoadsMap[key] = mutableListOf()
                cellBuildingsMap[key] = mutableListOf()
            }

            fetchedRoads.forEach { way ->
                if (way.size >= 2) {
                    val cellsForThisRoad = mutableSetOf<String>()
                    way.forEach { pt ->
                        val cx = Math.floor(pt.lat / 0.015).toInt()
                        val cy = Math.floor(pt.lng / 0.015).toInt()
                        val key = "$cx,$cy"
                        if (cellsToQuery.contains(key)) {
                            cellsForThisRoad.add(key)
                        }
                    }
                    cellsForThisRoad.forEach { key ->
                        cellRoadsMap[key]?.add(way)
                    }
                }
            }

            fetchedBuildings.forEach { way ->
                if (way.points.size >= 2) {
                    val cellsForThisBuilding = mutableSetOf<String>()
                    way.points.forEach { pt ->
                        val cx = Math.floor(pt.lat / 0.015).toInt()
                        val cy = Math.floor(pt.lng / 0.015).toInt()
                        val key = "$cx,$cy"
                        if (cellsToQuery.contains(key)) {
                            cellsForThisBuilding.add(key)
                        }
                    }
                    cellsForThisBuilding.forEach { key ->
                        cellBuildingsMap[key]?.add(way)
                    }
                }
            }

            for ((key, roadsList) in cellRoadsMap) {
                val roadsJson = Json.encodeToString(roadsList)
                repository.insertCachedRoadCell(CachedRoadCell(key, roadsJson, now))
                addCachedCellToState(key)
            }

            for ((key, buildingsList) in cellBuildingsMap) {
                val buildingsJson = Json.encodeToString(buildingsList)
                repository.insertCachedBuildingCell(CachedBuildingCell(key, buildingsJson, now))
            }

            val maxCellsToLoad = mutableListOf<String>()
            for (dx in -radius..radius) {
                for (dy in -radius..radius) {
                    maxCellsToLoad.add("${cellX + dx},${cellY + dy}")
                }
            }
            val allRoads = mutableListOf<List<Pair<Double, Double>>>()
            val allBuildings = mutableListOf<BuildingStructure>()
            maxCellsToLoad.forEach { key ->
                val roadCell = repository.getCachedRoadCell(key)
                val buildingCell = repository.getCachedBuildingCell(key)
                if (roadCell != null && (now - roadCell.fetchedAt) < 24 * 60 * 60 * 1000L) {
                    try {
                        val roadsList = Json.decodeFromString<List<List<RoadPoint>>>(roadCell.roadsJson)
                        roadsList.forEach { road ->
                            allRoads.add(road.map { Pair(it.lat, it.lng) })
                        }
                    } catch (e: Exception) {}
                }
                if (buildingCell != null && (now - buildingCell.fetchedAt) < 24 * 60 * 60 * 1000L) {
                    try {
                        val buildingsList = Json.decodeFromString<List<BuildingStructure>>(buildingCell.buildingsJson)
                        allBuildings.addAll(buildingsList)
                    } catch (e: Exception) {}
                }
            }
            _roads.value = allRoads.distinct()
            _buildings.value = allBuildings.distinct()
        }
    }

    private fun parseOverpassJson(jsonStr: String, type: String): List<List<RoadPoint>> {
        val result = mutableListOf<List<RoadPoint>>()
        try {
            val root = org.json.JSONObject(jsonStr)
            val elements = root.optJSONArray("elements") ?: return emptyList()
            for (i in 0 until elements.length()) {
                val element = elements.getJSONObject(i)
                val tags = element.optJSONObject("tags")
                val isBuilding = tags?.has("building") == true
                val isHighway = tags?.has("highway") == true

                val matches = if (type == "building") isBuilding else isHighway
                if (!matches) continue

                val geometry = element.optJSONArray("geometry") ?: continue
                val path = mutableListOf<RoadPoint>()
                for (j in 0 until geometry.length()) {
                    val pt = geometry.getJSONObject(j)
                    val ptLat = pt.getDouble("lat")
                    val ptLon = pt.getDouble("lon")
                    path.add(RoadPoint(ptLat, ptLon))
                }
                if (path.size >= 2) {
                    result.add(path)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun parseArcgisJson(jsonStr: String): List<BuildingStructure> {
        val result = mutableListOf<BuildingStructure>()
        try {
            val root = org.json.JSONObject(jsonStr)
            val features = root.optJSONArray("features") ?: return emptyList()
            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                val geometry = feature.optJSONObject("geometry") ?: continue
                val rings = geometry.optJSONArray("rings") ?: continue
                if (rings.length() > 0) {
                    val ring = rings.getJSONArray(0)
                    val points = mutableListOf<RoadPoint>()
                    for (j in 0 until ring.length()) {
                        val pt = ring.getJSONArray(j)
                        val lng = pt.getDouble(0)
                        val lat = pt.getDouble(1)
                        points.add(RoadPoint(lat, lng))
                    }
                    if (points.size >= 2) {
                        result.add(BuildingStructure(points = points, isFallback = false))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun generateNearbyAnomalies(lat: Double, lng: Double) {
        val baseSeed = (lat * 100).toInt() + (lng * 100).toInt()
        val random = java.util.Random(baseSeed.toLong())
        val factions = listOf("Infection", "Mech", "Parasite", "Containment")

        val list = mutableListOf<PoxAnomaly>()
        for (i in 0 until 5) {
            var dLat = 0.0
            var dLng = 0.0
            var distMeters = 0.0
            var attempts = 0

            while (attempts < 100) {
                val angle = random.nextDouble() * 2.0 * Math.PI
                distMeters = 200.0 + random.nextDouble() * 3800.0

                dLat = (distMeters * kotlin.math.cos(angle)) / 111000.0
                dLng = (distMeters * kotlin.math.sin(angle)) / (111000.0 * kotlin.math.cos(Math.toRadians(lat)))

                val candidateLat = lat + dLat
                val candidateLng = lng + dLng
                val overlaps = list.any { existing ->
                    val dx = (existing.lng - candidateLng) * 111000.0 * kotlin.math.cos(Math.toRadians(lat))
                    val dy = (existing.lat - candidateLat) * 111000.0
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                    dist < 200.0
                }
                if (!overlaps) break
                attempts++
            }

            val distanceInFeet = distMeters * 3.28084
            val gene = WaveMath.generateAnomalousGene(random)

            val densityRaw = (random.nextDouble() * 0.66) - 0.33
            val density = Math.round(densityRaw * 100.0) / 100.0

            list.add(
                PoxAnomaly(
                    id = "ANM-$i-$baseSeed",
                    name = "Anomaly #${baseSeed % 100 + i}",
                    lat = lat + dLat,
                    lng = lng + dLng,
                    gene = gene,
                    faction = factions[random.nextInt(4)],
                    distance = distanceInFeet,
                    heatZoneDiameter = 500.0 * 3.28084,
                    density = density
                )
            )
        }
        _rawAnomalies.value = list
    }

    fun getSynodicResonanceMod(faction: String, phaseName: String): Int {
        return DeploymentEngine.getSynodicResonanceMod(faction, phaseName)
    }

    private fun hasCoherenceShield(creature: Creature): Boolean {
        if (WaveMath.getAnomalousBenefits(creature.sequence).any { it.id == "COHERENCE_SHIELD" }) {
            return true
        }
        creature.appendedGenes.forEach { gene ->
            if (WaveMath.isAnomalousGene(gene) && WaveMath.getBenefitForAnomalousGene(gene).id == "COHERENCE_SHIELD") {
                return true
            }
        }
        return false
    }

    fun dispatchMission(creature: Creature, anomaly: PoxAnomaly) {
        viewModelScope.launch {
            val wave = WaveMath.getDailyWaveConfig(System.currentTimeMillis())
            val boundaryRadius = anomaly.getBoundaryRadiusForPlayer(_latitude.value, _longitude.value)
            val phaseFraction = wave.lunarAge / DeploymentEngine.LUNAR_MONTH_DAYS
            val lunarPhaseScale = (1.0 - kotlin.math.cos(phaseFraction * 2.0 * Math.PI)) / 2.0
            val lunarResistanceMod = 0.7 + 0.6 * lunarPhaseScale
            val R_base = boundaryRadius * 0.1
            val R_anom = R_base * lunarResistanceMod

            val resonanceMod = DeploymentEngine.getSynodicResonanceMod(creature.faction, wave.phaseName)
            val effectiveDefense = DeploymentEngine.calculateEffectiveDefense(creature.defense, creature.faction, wave.phaseName)

            val stalledDepth = DeploymentEngine.calculateStallDepth(effectiveDefense, boundaryRadius, wave.lunarAge)
            val dispatchDistance = boundaryRadius * (1.0 - stalledDepth / 100.0)

            val mutationInterval = DeploymentEngine.calculateMutationInterval(stalledDepth, wave.lunarAge)

            val t = if (anomaly.distance > 0.0) (dispatchDistance / anomaly.distance).coerceIn(0.0, 1.0) else 0.0
            val landingLat = anomaly.lat + t * (_latitude.value - anomaly.lat)
            val landingLng = anomaly.lng + t * (_longitude.value - anomaly.lng)

            val combinedDensity = DeploymentEngine.calculateCombinedWaveDensity(landingLat, landingLng, _rawAnomalies.value)

            val descentDistance = boundaryRadius * (stalledDepth / 100.0)
            val densityShift = 0.2 * (lunarPhaseScale - 0.5)
            val effectiveDensity = (combinedDensity + densityShift).coerceIn(-0.33, 0.33)

            val hasCoherenceShield = hasCoherenceShield(creature)
            val finalDensity = if (hasCoherenceShield && effectiveDensity > 0.0) 0.0 else effectiveDensity

            val metrics = DeploymentEngine.calculateTransitMetrics(
                creatureSpeed = creature.speed,
                anomalyDistance = anomaly.distance,
                boundaryRadius = boundaryRadius,
                stalledDepth = stalledDepth,
                effectiveDensity = effectiveDensity,
                hasCoherenceShield = hasCoherenceShield
            )

            val travelDuration = metrics.travelDuration
            val descentDuration = metrics.descentDuration
            val harvestDuration = metrics.harvestDuration
            val ascentDuration = metrics.ascentDuration
            val transitBackDuration = metrics.returnDuration
            val calculatedDuration = metrics.totalDuration

            val densityVal = Math.round(combinedDensity * 100.0).toInt()
            val effDensityVal = Math.round(effectiveDensity * 100.0).toInt()
            val missionLogsList = listOf(
                "[LAUNCH] Specimen \"${creature.name}\" dispatched to Anomaly ${anomaly.id}.",
                "[LANDING_COORD] $landingLat,$landingLng",
                "[TELEMETRY] Synodic Resonance: ${if (resonanceMod >= 0) "+" else ""}$resonanceMod DEF (${wave.phaseName}).",
                "[TELEMETRY] Effective Defense: $effectiveDefense | Anomaly Resistance: ${String.format(java.util.Locale.US, "%.1f", R_anom)} (Base: ${R_base.toInt()}, Lunar: ${String.format(java.util.Locale.US, "%.2f", lunarResistanceMod)}x).",
                "[TELEMETRY] Stalled Depth resolved: ${stalledDepth.toInt()}% (${String.format(java.util.Locale.US, "%.1f", dispatchDistance)}ft from epicenter).",
                "[TELEMETRY] Combined Wave Density -> Base: ${if (densityVal >= 0) "+" else ""}$densityVal% | Active Shifted: ${if (effDensityVal >= 0) "+" else ""}$effDensityVal%.",
                "[TELEMETRY] Coherence Shield status: ${if (hasCoherenceShield) "ACTIVE (Drag Imm.)" else "INACTIVE"}.",
                "[TELEMETRY] Durations -> Travel: ${travelDuration}s | Descent: ${descentDuration}s | Harvest: ${harvestDuration}s | Ascent: ${ascentDuration}s | Return: ${transitBackDuration}s.",
                "[TELEMETRY] Mutation Well active. Expected base decay: every ${mutationInterval}s."
            )

            val mission = HarvestMission(
                id = "MSN-" + UUID.randomUUID().toString().take(6),
                creatureId = creature.id,
                creatureName = creature.name,
                creatureFaction = creature.faction,
                lat = anomaly.lat,
                lng = anomaly.lng,
                startTime = System.currentTimeMillis(),
                totalDuration = calculatedDuration,
                harvestedPayload = listOf(anomaly.gene),
                isCompleted = false,
                isReturned = false,
                dispatchDistance = dispatchDistance,
                stalledDepth = stalledDepth,
                originalSequence = creature.sequence,
                elapsedSeconds = 0L,
                missionLogs = missionLogsList,
                phase = "TRAVEL",
                travelDuration = travelDuration,
                descentDuration = descentDuration,
                harvestDuration = harvestDuration,
                ascentDuration = ascentDuration,
                transitBackDuration = transitBackDuration,
                currentPhaseElapsed = 0L
            )

            repository.insertMission(mission)
            setTrackedMissionId(mission.id)
            addLog("DSP: Dispatched \"${creature.name}\" to harvest spot [${anomaly.gene}]")
            synthManager.playBeep(520f, 0.3f, "triangle")
        }
    }

    fun recallMission(mission: HarvestMission) {
        viewModelScope.launch {
            val landingLog = mission.missionLogs.find { it.startsWith("[LANDING_COORD] ") }
            val (landingLat, landingLng) = if (landingLog != null) {
                val coords = landingLog.substringAfter("[LANDING_COORD] ").split(",")
                coords[0].toDouble() to coords[1].toDouble()
            } else {
                mission.lat to mission.lng
            }

            val otherAnom = _rawAnomalies.value.find { anom ->
                (Math.abs(anom.lat - mission.lat) > 0.0001 || Math.abs(anom.lng - mission.lng) > 0.0001) &&
                calculateDistanceInFeet(landingLat, landingLng, anom.lat, anom.lng) <= anom.getBoundaryRadiusForPlayer(landingLat, landingLng)
            }

            fun isGeneSequence(s: String): Boolean {
                return s.length == 8 && s.all { it in "AGTC" }
            }

            val primaryGene = mission.harvestedPayload.find { isGeneSequence(it) } ?: "AGTCGTAC"
            var updatedPrimaryGene = primaryGene
            var spilloverSuccess = false
            var hybridGene: String? = null
            val finalLogs = mission.missionLogs.toMutableList()

            if (otherAnom != null) {
                val dA = calculateDistanceInFeet(landingLat, landingLng, mission.lat, mission.lng)
                val dB = calculateDistanceInFeet(landingLat, landingLng, otherAnom.lat, otherAnom.lng)

                val baseSpilloverChance = 0.125
                val factor = 1.0 - (dB / (dA + dB)).coerceIn(0.0, 1.0)
                val spilloverChance = baseSpilloverChance * factor

                val roll = Math.random()
                finalLogs.add("[TELEMETRY] Adjacent anomaly boundary overlap: ${otherAnom.id} (crosstalk active).")
                if (roll <= spilloverChance) {
                    spilloverSuccess = true
                    val targetGene = primaryGene
                    val overlapGene = otherAnom.gene
                    if (targetGene.length == 8 && overlapGene.length == 8) {
                        hybridGene = targetGene.substring(0, 4) + overlapGene.substring(4, 8)
                        updatedPrimaryGene = hybridGene
                        finalLogs.add("[TELEMETRY] Hybridization success: $hybridGene (Target $targetGene Weld Overlap $overlapGene).")
                    } else {
                        updatedPrimaryGene = overlapGene
                        finalLogs.add("[TELEMETRY] Crosstalk spillover: Harvested adjacent gene $overlapGene.")
                    }
                } else {
                    finalLogs.add("[TELEMETRY] Crosstalk failed. Target signature remains dominant.")
                }
            }

            val finalPayload = mission.harvestedPayload.toMutableList()
            val primaryIndex = finalPayload.indexOf(primaryGene)
            if (primaryIndex != -1) {
                finalPayload[primaryIndex] = updatedPrimaryGene
            } else {
                finalPayload.add(0, updatedPrimaryGene)
            }

            val bonusGene = WaveMath.generateAnomalousGene()
            finalPayload.add(bonusGene)

            val genesToInsert = mutableListOf<String>()
            var deltaA = 0L
            var deltaG = 0L
            var deltaT = 0L
            var deltaC = 0L

            finalPayload.forEach { item ->
                if (isGeneSequence(item)) {
                    genesToInsert.add(item)
                } else {
                    when (item.uppercase()) {
                        "STOCK_A" -> deltaA++
                        "STOCK_G" -> deltaG++
                        "STOCK_T" -> deltaT++
                        "STOCK_C" -> deltaC++
                    }
                }
            }

            val toInsertOrUpdate = mutableListOf<GeneSequence>()
            genesToInsert.distinct().forEach { gene ->
                val tm = GeneticsEngine.calculateMeltingTemperature(gene, _reactorSalt.value.toDouble())
                val mfe = GeneticsEngine.calculateMinimumFreeEnergy(gene)
                toInsertOrUpdate.add(
                    GeneSequence(
                        sequence = gene,
                        count = finalPayload.count { it == gene },
                        discoveredAt = System.currentTimeMillis(),
                        averageQScore = 30.0,
                        meltingTemp = tm,
                        minimumFreeEnergy = mfe
                    )
                )
            }
            repository.insertGeneSequences(toInsertOrUpdate)

            if (deltaA > 0 || deltaG > 0 || deltaT > 0 || deltaC > 0) {
                val currentA = repository.rawStockA.first()
                val currentG = repository.rawStockG.first()
                val currentT = repository.rawStockT.first()
                val currentC = repository.rawStockC.first()

                val newA = currentA + deltaA
                val newG = currentG + deltaG
                val newT = currentT + deltaT
                val newC = currentC + deltaC

                repository.saveRawStocks(newA, newG, newT, newC)
                finalLogs.add("[RESOURCES] Siphoned resources deposited to stockpile: A: +$deltaA, G: +$deltaG, T: +$deltaT, C: +$deltaC")
            }

            val updated = mission.copy(isReturned = true, harvestedPayload = finalPayload, missionLogs = finalLogs)
            repository.insertMission(updated)

            if (_trackedMissionId.value == mission.id) {
                _trackedMissionId.value = null
            }

            addLog("RCL: Mission returned. Payload processed: ${finalPayload.joinToString()}")
            synthManager.playSynthesisSuccess()
        }
    }

    fun recallAllActiveMissions() {
        viewModelScope.launch {
            try {
                val active = repository.activeMissions.first()
                val ongoing = active.filter { !it.isReturned }
                if (ongoing.isEmpty()) {
                    addLog("DEV: No active harvesters found to recall.")
                    return@launch
                }
                ongoing.forEach { m ->
                    val updated = m.copy(
                        isCompleted = true,
                        phase = "COMPLETED",
                        currentPhaseElapsed = 0L
                    )
                    recallMission(updated)
                }
                addLog("DEV: Recalled ${ongoing.size} active harvesters.")
            } catch (e: Exception) {
                addLog("DEV ERROR: Failed to recall harvesters: ${e.message}")
            }
        }
    }

    private fun startMissionTicker() {
        missionTickerJob?.cancel()
        missionTickerJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(1000L)
                val activeMissionsList = activeMissions.value
                if (activeMissionsList.isNotEmpty()) {
                    activeMissionsList.forEach { m ->
                        if (!m.isReturned) {
                            val nextElapsed = m.elapsedSeconds + 1
                            val currentPhaseElapsed = m.currentPhaseElapsed + 1

                            var nextPhase = m.phase
                            var nextPhaseElapsed = currentPhaseElapsed
                            val phaseLogs = mutableListOf<String>()

                            when (m.phase) {
                                "TRAVEL" -> {
                                    if (currentPhaseElapsed >= m.travelDuration) {
                                        nextPhase = "DESCENT"
                                        nextPhaseElapsed = 0L
                                        phaseLogs.add("[TELEMETRY] Arrived at boundary. Initiating descent.")
                                    }
                                }
                                "DESCENT" -> {
                                    if (currentPhaseElapsed >= m.descentDuration) {
                                        nextPhase = "HARVESTING"
                                        nextPhaseElapsed = 0L
                                        phaseLogs.add("[TELEMETRY] Depth reached. Initiating extraction.")
                                    }
                                }
                                "HARVESTING" -> {
                                    if (currentPhaseElapsed >= m.harvestDuration) {
                                        nextPhase = "ASCENT"
                                        nextPhaseElapsed = 0L
                                        phaseLogs.add("[TELEMETRY] Extraction complete. Commencing ascent.")
                                    }
                                }
                                "ASCENT" -> {
                                    if (currentPhaseElapsed >= m.ascentDuration) {
                                        nextPhase = "TRANST_BACK"
                                        nextPhaseElapsed = 0L
                                        phaseLogs.add("[TELEMETRY] Boundary reached. Transitioning to transit.")
                                    }
                                }
                                "TRANST_BACK" -> {
                                    if (currentPhaseElapsed >= m.transitBackDuration) {
                                        nextPhase = "COMPLETED"
                                        nextPhaseElapsed = 0L
                                        phaseLogs.add("[COMPLETE] Payload returned.")
                                    }
                                }
                            }

                            val isCompleted = nextPhase == "COMPLETED"

                            val waveConfig = WaveMath.getDailyWaveConfig(System.currentTimeMillis())
                            val waveCouplingVal = WaveMath.getSpectrumWaveCoupling(System.currentTimeMillis())
                            val mutationInterval = DeploymentEngine.calculateMutationInterval(m.stalledDepth, waveConfig.lunarAge)

                            val newLogs = m.missionLogs.toMutableList()
                            val newHarvestedPayload = m.harvestedPayload.toMutableList()
                            phaseLogs.forEach { log ->
                                newLogs.add(log)
                            }

                            var resolvedPhase = nextPhase
                            var resolvedPhaseElapsed = nextPhaseElapsed
                            var resolvedCompleted = isCompleted

                            if (!resolvedCompleted && (m.phase == "DESCENT" || m.phase == "HARVESTING" || m.phase == "ASCENT")) {
                                val creature = repository.getCreatureById(m.creatureId)
                                if (creature != null) {
                                    val landingLog = m.missionLogs.find { it.startsWith("[LANDING_COORD] ") }
                                    val (landingLat, landingLng) = if (landingLog != null) {
                                        val coords = landingLog.substringAfter("[LANDING_COORD] ").split(",")
                                        coords[0].toDouble() to coords[1].toDouble()
                                    } else {
                                        m.lat to m.lng
                                    }

                                    val anomaly = _rawAnomalies.value.find { Math.abs(it.lat - m.lat) < 0.0001 && Math.abs(it.lng - m.lng) < 0.0001 }
                                        ?: PoxAnomaly(
                                            id = "ANM-MUTATED",
                                            name = "Mutated Anomaly",
                                            lat = m.lat,
                                            lng = m.lng,
                                            gene = m.harvestedPayload.firstOrNull() ?: "AGTCGTAC",
                                            faction = m.creatureFaction,
                                            distance = 0.0,
                                            heatZoneDiameter = 500.0 * 3.28084,
                                            density = 0.0
                                        )

                                    val overlappingWells = _celestialWells.value.filter { well -> well.contains(landingLat, landingLng) }

                                    var currentCreature: Creature = creature
                                    val isLoggingTick = nextElapsed % 5 == 0L

                                    val anomalyResult = ConflictEngine.simulateCreatureVsEnvironment(
                                        creature = currentCreature,
                                        hazard = anomaly,
                                        userLat = landingLat,
                                        userLng = landingLng,
                                        totalTicks = 1,
                                        lunarAge = waveConfig.lunarAge,
                                        waveCoupling = waveCouplingVal
                                    )
                                    currentCreature = currentCreature.copy(telomeres = anomalyResult.finalTelomeres)

                                    anomalyResult.logs.forEach { log ->
                                        if (log.startsWith("Tick ") && isLoggingTick) {
                                            newLogs.add("[HAZARD] Anomaly: $log")
                                        } else if (log.contains("Coherence Shield") && currentPhaseElapsed == 1L) {
                                            newLogs.add(log)
                                        }
                                    }

                                    val isHarvestingCompleteTick = m.phase == "HARVESTING" && nextPhase == "ASCENT"
                                    if (isHarvestingCompleteTick && anomalyResult.harvestedPayload.isNotEmpty()) {
                                        newHarvestedPayload.addAll(anomalyResult.harvestedPayload)
                                    }

                                    var survived = anomalyResult.survived
                                    if (survived) {
                                        for (well in overlappingWells) {
                                            val wellResult = ConflictEngine.simulateCreatureVsEnvironment(
                                                creature = currentCreature,
                                                hazard = well,
                                                userLat = landingLat,
                                                userLng = landingLng,
                                                totalTicks = 1,
                                                lunarAge = waveConfig.lunarAge,
                                                waveCoupling = waveCouplingVal
                                            )
                                            currentCreature = currentCreature.copy(telomeres = wellResult.finalTelomeres)

                                            wellResult.logs.forEach { log ->
                                                if (log.startsWith("Tick ") && isLoggingTick) {
                                                    newLogs.add("[HAZARD] ${well.name}: $log")
                                                } else if (log.contains("Coherence Shield") && currentPhaseElapsed == 1L) {
                                                    newLogs.add(log)
                                                }
                                            }

                                            if (isHarvestingCompleteTick && wellResult.harvestedPayload.isNotEmpty()) {
                                                newHarvestedPayload.addAll(wellResult.harvestedPayload)
                                            }

                                            if (!wellResult.survived) {
                                                survived = false
                                                break
                                            }
                                        }
                                    }

                                    repository.insertCreature(currentCreature)

                                    if (currentCreature.telomeres <= 0) {
                                        newLogs.add("[FATAL] Specimen's telomeres completely decayed under extreme environmental hazard. Mission aborted.")
                                        resolvedPhase = "COMPLETED"
                                        resolvedPhaseElapsed = 0L
                                        resolvedCompleted = true
                                    }

                                    if (!resolvedCompleted && nextElapsed % mutationInterval == 0L) {
                                        val bases = listOf("A", "G", "T", "C")
                                        val chars = currentCreature.sequence.toCharArray()
                                        val mutationIndex = (Math.random() * chars.size).toInt()
                                        val oldBase = chars[mutationIndex].toString()
                                        val choices = bases.filter { it != oldBase }
                                        val newBase = choices[(Math.random() * choices.size).toInt()]
                                        chars[mutationIndex] = newBase[0]
                                        val mutatedSequence = String(chars)

                                        val proc = compileDeterministicOffline(mutatedSequence)
                                        val updatedCreature = currentCreature.copy(
                                            sequence = mutatedSequence,
                                            vitality = proc.vitality,
                                            attack = proc.attack,
                                            defense = proc.defense,
                                            speed = proc.speed,
                                            faction = proc.faction,
                                            type = proc.type,
                                            primaryWeapon = proc.primaryWeapon,
                                            lore = proc.lore,
                                            isMutated = true,
                                            originalSequence = currentCreature.originalSequence ?: m.originalSequence ?: currentCreature.sequence
                                        )
                                        repository.insertCreature(updatedCreature)
                                        newLogs.add("[MUTATION] Pos $mutationIndex: $oldBase -> $newBase. Stats re-calculated.")
                                    }
                                }
                            }

                            val updatedMission = m.copy(
                                elapsedSeconds = nextElapsed,
                                isCompleted = resolvedCompleted,
                                missionLogs = newLogs,
                                phase = resolvedPhase,
                                currentPhaseElapsed = resolvedPhaseElapsed,
                                harvestedPayload = newHarvestedPayload
                            )
                            if (resolvedCompleted) {
                                recallMission(updatedMission)
                            } else {
                                repository.insertMission(updatedMission)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun compileDeterministicOffline(sequence: String, reagent: String? = null): Creature {
        val upperSeq = sequence.uppercase()
        var aCount = 0
        var gCount = 0
        var tCount = 0
        var cCount = 0
        for (char in upperSeq) {
            when (char) {
                'A' -> aCount++
                'G' -> gCount++
                'T' -> tCount++
                'C' -> cCount++
            }
        }

        var aCountFaction = aCount
        var gCountFaction = gCount
        var tCountFaction = tCount
        var cCountFaction = cCount

        when (reagent) {
            "REAGENT_ALIGN_CONTAINMENT" -> cCountFaction += 15
            "REAGENT_ALIGN_INFECTION" -> aCountFaction += 15
            "REAGENT_ALIGN_MECH" -> gCountFaction += 15
            "REAGENT_ALIGN_PARASITE" -> tCountFaction += 15
        }

        val faction = when (maxOf(aCountFaction, gCountFaction, tCountFaction, cCountFaction)) {
            aCountFaction -> "Infection"
            gCountFaction -> "Mech"
            tCountFaction -> "Parasite"
            else -> "Containment"
        }

        val type = when (faction) {
            "Infection" -> "Acidic Virus Swarm"
            "Mech" -> "Heavy Metal Shell"
            "Parasite" -> "Neural Parasite"
            else -> "Bio-Containment Unit"
        }

        val baseSeed = upperSeq.hashCode()
        val random = java.util.Random(baseSeed.toLong())

        val vitality = 100 + aCount * 5
        val attack = 20 + gCount * 2
        val defense = 20 + cCount * 2
        val speed = 20 + tCount * 2

        val name = when (faction) {
            "Infection" -> "Toxipod-${random.nextInt(99)}"
            "Mech" -> "Chitin-Shell-${random.nextInt(99)}"
            "Parasite" -> "CyberVex-${random.nextInt(99)}"
            else -> "SectorGuard-${random.nextInt(99)}"
        }

        val weapon = when (faction) {
            "Infection" -> "Acid Spurt-Needle"
            "Mech" -> "Titanium Piston Jaw"
            "Parasite" -> "Synapse Siphon"
            else -> "Plasma Shield Mesh"
        }

        val lore = "Deterministic offline specimen. Gene sequence frequencies reveal highly stable $faction bindings, evolving standard operational designation as a $type."

        val grid = StringBuilder()
        for (r in 0 until 5) {
            for (col in 0 until 5) {
                val valRand = random.nextInt(4)
                val char = when (valRand) {
                    0 -> "."
                    1 -> "o"
                    2 -> "x"
                    else -> "#"
                }
                grid.append(char)
            }
            if (r < 4) grid.append("\n")
        }

        return Creature(
            id = UUID.randomUUID().toString(),
            sequence = sequence,
            name = name,
            faction = faction,
            type = type,
            vitality = vitality,
            attack = attack,
            defense = defense,
            speed = speed,
            primaryWeapon = weapon,
            lore = lore,
            asciiArt = grid.toString(),
            discoveredAt = System.currentTimeMillis(),
            origin = "Local Compiled Unit"
        )
    }

    private fun calculateDistanceInFeet(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        return DeploymentEngine.calculateDistanceInFeet(lat1, lng1, lat2, lng2)
    }
}
