# P.O.X. Network Manager Specification

The **P.O.X. Network Manager** is a peer-to-peer (P2P) networking utility that coordinates ad-hoc advertising, discovery, and connection management, as well as simulating networking packets using Google Nearby Connections.

- **Class Definition**: [PoxNetworkManager.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/network/PoxNetworkManager.kt)

---

## 1. Features and Functionalities

### A. P2P Advertising & Discovery
Uses Google Play Services Nearby Connections API to establish a localized ad-hoc grid:
- **Service Identifier**: `com.example.genpox.HYDRA_NET`.
- **Strategy**: `Strategy.P2P_CLUSTER` (enables multi-peer ad-hoc meshes).
- **Advertising**: Node announces its existence and sector coordinates to nearby peers.
- **Discovery**: Actively scans for neighboring beacons, posting found endpoints to a `discoveredPeers` flow.

### B. Hydra Packet Serialization Protocol (`HydraPacket`)
Packets are serialized using `kotlinx.serialization` to negotiate transactions and sync game state:
1. **Beacon**: Node identifiers, active sector, and public key coordinates.
2. **Ping**: Timestamp for synchronization checks.
3. **SpecimenOffer & SpecimenResponse**: Structured data to negotiate P2P creature trading with cryptographically signed payloads.
4. **NetworkMessage**: Text-based messaging with support for binary payload attachments (DNA sequences, genes, wastes).
5. **HackingIntrusion**: Requests intrusion testing, transferring the hacker creature statistics to the target node.
6. **HackingCounterResponse**: Returns defense profiles (active vault defenders, firewall complexity) to initiate hacking combat.
7. **HackingResultReport**: Synchronizes hacking result logs, siphoned payloads, and final specimen telomere statuses across nodes.

### C. Simulation Loopback Daemon
Allows full testing of networking workflows without physical peer devices:
- Generates simulated mock nodes (e.g. `MOCK-ALPHA`, `MOCK-BETA`).
- Simulates loopback transactions and returns realistic packet structures, which permits offline development and automated UI testing.

---

## 2. Integration and Usage

### Where it is used:
- [NetworkViewModel.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/NetworkViewModel.kt): Binds network actions (advertising toggle, trade triggers, messaging) to the user dashboard.
- [MainViewModel.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainViewModel.kt): Coordinates incoming hacking intrusion packets and routes counter-defense responses through the `ConflictEngine`.
- [ImportAuditTest.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/test/java/com/example/genpox/ImportAuditTest.kt): Audits the network manager to ensure zero UI dependencies.
