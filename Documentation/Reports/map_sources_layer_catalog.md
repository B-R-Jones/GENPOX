# G.E.N. Network: Map Sources & USGS Structure Layer Catalog

This document provides a comprehensive catalog of external GIS map data endpoints and the complete layer hierarchy of the USGS National Map `structures` service used by the Scanner tab radar system.

---

## 1. Overview of Map Sources

| Data Source | Provider | Endpoint URL | Query Method | Content Sourced |
| :--- | :--- | :--- | :--- | :--- |
| **OpenStreetMap (Overpass)** | OpenStreetMap Foundation | `https://overpass-api.de/api/interpreter`<br>`https://lz4.overpass-api.de/api/interpreter` | Overpass QL POST (`highway~"..."`) | Primary road network vector geometry (`highway`) |
| **FEMA USA Structures** | DHS / FEMA | `https://services2.arcgis.com/FiaPA4ga0iQKduv3/arcgis/rest/services/USA_Structures_View/FeatureServer/0/query` | ArcGIS REST GET (`esriGeometryEnvelope`) | Primary building polygon footprints (`rings`) |
| **USGS Structures** | USGS National Map | `https://carto.nationalmap.gov/arcgis/rest/services/structures/MapServer/{layerId}/query` | ArcGIS REST GET (`esriGeometryEnvelope`) | Secondary landmark points (`x, y`) & public facility structures |

---

## 2. USGS National Map Structures Service (`structures/MapServer`)

The USGS National Map `structures` service hosts 70 distinct layers (IDs 0–69) organized under Group containers (`Labels` and `Features`).

### Layer Hierarchy Catalog

```
structures/MapServer
├── [Group 0] Labels
│   ├── [Group 1] Landmarks & Government Buildings
│   │   ├── Layer 2: Cemeteries
│   │   ├── Layer 3: Post Offices
│   │   ├── Layer 4: City/Town Halls
│   │   ├── Layer 5: Courthouses
│   │   ├── Layer 6: State Capitols
│   │   ├── Layer 7: State Supreme Courts
│   │   ├── Layer 8: The White House
│   │   ├── Layer 9: U.S. Capitol
│   │   ├── Layer 10: U.S. Supreme Court
│   │   ├── Layer 11: Historic Sites/Points of Interest
│   │   └── Layer 12: National Symbols/Monuments
│   ├── [Group 13] Medical & Emergency Response
│   │   ├── Layer 14: Hospitals/Medical Centers
│   │   ├── Layer 15: Ambulance Services
│   │   └── Layer 16: Fire Stations/EMS Stations
│   ├── [Group 17] Law Enforcement
│   │   ├── Layer 18: Police Stations
│   │   └── Layer 19: Prisons/Correctional Facilities
│   ├── [Group 20] Education
│   │   ├── Layer 21: Colleges/Universities
│   │   ├── Layer 22: Technical/Trade Schools
│   │   └── Layer 23: Schools
│   ├── [Group 24] Recreation
│   │   ├── Layer 25: Campgrounds
│   │   ├── Layer 26: Trailheads
│   │   ├── Layer 27: Cabins
│   │   ├── Layer 28: Shelters
│   │   ├── Layer 29: Picnic Areas
│   │   ├── Layer 30: Headquarters
│   │   ├── Layer 31: Visitor/Information Centers
│   │   └── Layer 32: Ranger Stations
│   └── [Group 33] Energy
│       └── Layer 34: Alaska Pipeline
└── [Group 35] Features (Active Feature Data Source)
    ├── [Group 36] Landmarks & Government Buildings
    │   ├── Layer 37: Cemeteries
    │   ├── Layer 38: Post Offices
    │   ├── Layer 39: City/Town Halls
    │   ├── Layer 40: Courthouses
    │   ├── Layer 41: State Capitols
    │   ├── Layer 42: State Supreme Courts
    │   ├── Layer 43: The White House
    │   ├── Layer 44: U.S. Capitol
    │   ├── Layer 45: U.S. Supreme Court
    │   ├── Layer 46: Historic Sites/Points of Interest
    │   └── Layer 47: National Symbols/Monuments
    ├── [Group 48] Medical & Emergency Response
    │   ├── Layer 49: Hospitals/Medical Centers
    │   ├── Layer 50: Ambulance Services
    │   └── Layer 51: Fire Stations/EMS Stations
    ├── [Group 52] Law Enforcement
    │   ├── Layer 53: Police Stations
    │   └── Layer 54: Prisons/Correctional Facilities
    ├── [Group 55] Education
    │   ├── Layer 56: Colleges/Universities
    │   ├── Layer 57: Technical/Trade Schools
    │   └── Layer 58: Schools
    ├── [Group 59] Recreation
    │   ├── Layer 60: Campgrounds
    │   ├── Layer 61: Trailheads
    │   ├── Layer 62: Cabins
    │   ├── Layer 63: Shelters
    │   ├── Layer 64: Picnic Areas
    │   ├── Layer 65: Headquarters
    │   ├── Layer 66: Visitor/Information Centers
    │   └── Layer 67: Ranger Stations
    └── [Group 68] Energy
        └── Layer 69: Alaska Pipeline
```

---

## 3. Complete USGS Layer Reference Table

> [!NOTE]
> Group layers (e.g. 0, 1, 13, 17, 20, 24, 33, 35, 36, 48, 52, 55, 59, 68) serve strictly as organizational headers in the MapServer REST API and will return `HTTP 400 Invalid or missing input parameters` if queried directly. Only leaf feature layers under Group 35 contain queryable spatial geometry (`x, y` point coordinates).

| Layer ID | Layer Name | Type | Parent ID | Description & Scanner Usage |
| :---: | :--- | :--- | :---: | :--- |
| `0` | Labels | Group | `-1` | Parent container for text label styling |
| `1` | Landmarks & Government Buildings | Group | `0` | Label group for civic landmarks |
| `2` | Cemeteries | Label Leaf | `1` | Cemetery label points |
| `3` | Post Offices | Label Leaf | `1` | Post office label points |
| `4` | City/Town Halls | Label Leaf | `1` | City/Town hall label points |
| `5` | Courthouses | Label Leaf | `1` | Courthouse label points |
| `6` | State Capitols | Label Leaf | `1` | State capitol label points |
| `7` | State Supreme Courts | Label Leaf | `1` | State supreme court label points |
| `8` | The White House | Label Leaf | `1` | White House label point |
| `9` | U.S. Capitol | Label Leaf | `1` | U.S. Capitol label point |
| `10` | U.S. Supreme Court | Label Leaf | `1` | U.S. Supreme Court label point |
| `11` | Historic Sites/Points of Interest | Label Leaf | `1` | Historic site label points |
| `12` | National Symbols/Monuments | Label Leaf | `1` | National monument label points |
| `13` | Medical & Emergency Response | Group | `0` | Label group for emergency medical facilities |
| `14` | Hospitals/Medical Centers | Label Leaf | `13` | Hospital label points |
| `15` | Ambulance Services | Label Leaf | `13` | Ambulance service label points |
| `16` | Fire Stations/EMS Stations | Label Leaf | `13` | Fire station label points |
| `17` | Law Enforcement | Group | `0` | Label group for police/penal facilities |
| `18` | Police Stations | Label Leaf | `17` | Police station label points |
| `19` | Prisons/Correctional Facilities | Label Leaf | `17` | Prison/Correctional label points |
| `20` | Education | Group | `0` | Label group for educational facilities |
| `21` | Colleges/Universities | Label Leaf | `20` | College/University label points |
| `22` | Technical/Trade Schools | Label Leaf | `20` | Trade school label points |
| `23` | Schools | Label Leaf | `20` | School label points |
| `24` | Recreation | Group | `0` | Label group for public recreation sites |
| `25` | Campgrounds | Label Leaf | `24` | Campground label points |
| `26` | Trailheads | Label Leaf | `24` | Trailhead label points |
| `27` | Cabins | Label Leaf | `24` | Cabin label points |
| `28` | Shelters | Label Leaf | `24` | Shelter label points |
| `29` | Picnic Areas | Label Leaf | `24` | Picnic area label points |
| `30` | Headquarters | Label Leaf | `24` | Recreation HQ label points |
| `31` | Visitor/Information Centers | Label Leaf | `24` | Visitor center label points |
| `32` | Ranger Stations | Label Leaf | `24` | Ranger station label points |
| `33` | Energy | Group | `0` | Label group for energy infrastructure |
| `34` | Alaska Pipeline | Label Leaf | `33` | Pipeline label path |
| `35` | **Features** | **Group** | `-1` | **Primary parent container for queryable feature data** |
| `36` | Landmarks & Government Buildings | Group | `35` | Container for civic feature layers |
| `37` | Cemeteries | Feature Leaf | `36` | Cemetery location points |
| `38` | **Post Offices** | **Feature Leaf** | `36` | **US Postal Service facility points (Active Scanner Source)** |
| `39` | City/Town Halls | Feature Leaf | `36` | City/Town hall facility points |
| `40` | Courthouses | Feature Leaf | `36` | Courthouse facility points |
| `41` | State Capitols | Feature Leaf | `36` | State capitol facility points |
| `42` | State Supreme Courts | Feature Leaf | `36` | State supreme court facility points |
| `43` | The White House | Feature Leaf | `36` | White House location point |
| `44` | U.S. Capitol | Feature Leaf | `36` | U.S. Capitol location point |
| `45` | U.S. Supreme Court | Feature Leaf | `36` | U.S. Supreme Court location point |
| `46` | Historic Sites/Points of Interest | Feature Leaf | `36` | Historic site location points |
| `47` | National Symbols/Monuments | Feature Leaf | `36` | Monument location points |
| `48` | Medical & Emergency Response | Group | `35` | Container for emergency response feature layers |
| `49` | **Hospitals/Medical Centers** | **Feature Leaf** | `48` | **Hospital & medical center points (Active Scanner Source)** |
| `50` | Ambulance Services | Feature Leaf | `48` | Ambulance service points |
| `51` | **Fire Stations/EMS Stations** | **Feature Leaf** | `48` | **Fire station & EMS points (Active Scanner Source)** |
| `52` | Law Enforcement | Group | `35` | Container for law enforcement feature layers |
| `53` | **Police Stations** | **Feature Leaf** | `52` | **Police station facility points (Active Scanner Source)** |
| `54` | Prisons/Correctional Facilities | Feature Leaf | `52` | Prison/Correctional facility points |
| `55` | Education | Group | `35` | Container for educational facility feature layers |
| `56` | Colleges/Universities | Feature Leaf | `55` | College/University campus points |
| `57` | Technical/Trade Schools | Feature Leaf | `55` | Trade school location points |
| `58` | **Schools** | **Feature Leaf** | `55` | **K-12 Educational facility points (Active Scanner Source)** |
| `59` | Recreation | Group | `35` | Container for recreation feature layers |
| `60` | Campgrounds | Feature Leaf | `59` | Campground location points |
| `61` | Trailheads | Feature Leaf | `59` | Trailhead location points |
| `62` | Cabins | Feature Leaf | `59` | Cabin location points |
| `63` | Shelters | Feature Leaf | `59` | Shelter location points |
| `64` | Picnic Areas | Feature Leaf | `59` | Picnic area location points |
| `65` | Headquarters | Feature Leaf | `59` | Park HQ location points |
| `66` | Visitor/Information Centers | Feature Leaf | `59` | Visitor info center location points |
| `67` | Ranger Stations | Feature Leaf | `59` | Ranger station location points |
| `68` | Energy | Group | `35` | Container for energy infrastructure feature layers |
| `69` | Alaska Pipeline | Feature Leaf | `68` | Pipeline spatial path line |
