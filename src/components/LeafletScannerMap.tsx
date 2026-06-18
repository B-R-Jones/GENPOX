/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useEffect, useRef } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

const absHash = (str: string): number => {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = (hash << 5) - hash + str.charCodeAt(i);
    hash |= 0; // Convert to 32bit integer
  }
  return Math.abs(hash);
};

const getBoundaryRadius = (anomaly: any, theta: number): number => {
  const seed = absHash(anomaly.id);
  const r0 = (anomaly.heatZoneDiameter || 20) / 2; // in feet
  const epsilon = 0.15 + (seed % 3) * 0.05;
  const k = 3 + (seed % 3);
  const phi = (seed % 360) * (Math.PI / 180.0);
  return r0 * (1.0 + epsilon * Math.cos(k * theta + phi));
};

const isPointInsideAnomaly = (lat: number, lng: number, anomaly: any): boolean => {
  const dy = (lat - anomaly.lat) * 111000 * 3.28084;
  const dx = (lng - anomaly.lng) * 111000 * Math.cos(lat * Math.PI / 180) * 3.28084;
  const dist = Math.sqrt(dx * dx + dy * dy);
  
  const theta = Math.atan2(dy, dx);
  const boundaryRad = getBoundaryRadius(anomaly, theta);
  return dist <= boundaryRad;
};

interface LeafletScannerMapProps {
  userCoords: { lat: number; lng: number };
  scanRadius: number;
  bioAnomalies: any[];
  selectedAnomalyId: string | null;
  onSelectAnomaly: (id: string) => void;
  harvestingMissions: any[];
  onMapTapHeatZone: (anomalyId: string, lat: number, lng: number, distanceInFeet: number) => void;
}

export const LeafletScannerMap: React.FC<LeafletScannerMapProps> = ({
  userCoords,
  scanRadius,
  bioAnomalies,
  selectedAnomalyId,
  onSelectAnomaly,
  harvestingMissions,
  onMapTapHeatZone,
}) => {
  const mapContainerRef = useRef<HTMLDivElement | null>(null);
  const mapInstanceRef = useRef<L.Map | null>(null);
  const circleRef = useRef<L.Circle | null>(null);
  const playerMarkerRef = useRef<L.Marker | null>(null);
  const anomaliesLayerGroupRef = useRef<L.LayerGroup | null>(null);

  // Convert feet to meters
  const radiusInMeters = scanRadius * 0.3048;

  // Zoom translation Helper
  const getZoomFromRadius = (r: number): number => {
    if (r <= 30) return 20;
    if (r <= 55) return 19;
    if (r <= 80) return 18;
    return 17;
  };

  // Initialize Map
  useEffect(() => {
    if (!mapContainerRef.current) return;

    // Create Map Instance if not already created
    if (!mapInstanceRef.current) {
      const initialZoom = getZoomFromRadius(scanRadius);
      const map = L.map(mapContainerRef.current, {
        center: [userCoords.lat, userCoords.lng],
        zoom: initialZoom,
        zoomControl: true,
        attributionControl: false,
        maxZoom: 22,
        minZoom: 15,
      });

      // CartoDB Dark Matter (gorgeous high-tech gamified retro-black tiles)
      L.tileLayer("https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png", {
        maxZoom: 22,
        subdomains: "abcd",
        keepBuffer: 12,          // Pre-fetch tiles 12 columns/rows outside current viewport
        updateWhenIdle: false,   // Load tiles on the fly
      }).addTo(map);

      // Layer group for bio anomalies, heat zones, and harvesting missions markers
      const anomaliesGroup = L.layerGroup().addTo(map);
      anomaliesLayerGroupRef.current = anomaliesGroup;

      // Scanning range circular visual
      const circle = L.circle([userCoords.lat, userCoords.lng], {
        radius: radiusInMeters,
        color: "#A855F7",
        weight: 1.5,
        fillColor: "#A855F7",
        fillOpacity: 0.12,
        dashArray: "4, 4",
      }).addTo(map);
      circleRef.current = circle;

      // Custom pulsing player beacon
      const playerIcon = L.divIcon({
        html: `
          <div class="relative flex items-center justify-center" style="width: 40px; height: 40px;">
            <span class="absolute inline-flex h-full w-full rounded-full bg-purple-400 opacity-25 animate-ping"></span>
            <span class="relative inline-flex rounded-full h-5 w-5 bg-purple-500 border-2 border-black shadow-[0_0_10px_#a855f7]"></span>
          </div>
        `,
        className: "",
        iconSize: [41, 41],
        iconAnchor: [20, 20],
      });

      const playerMarker = L.marker([userCoords.lat, userCoords.lng], {
        icon: playerIcon,
        title: "PLAYER TRANSCIEVER COMS CHASSIS",
      }).addTo(map);
      playerMarkerRef.current = playerMarker;

      // Map click handler to detect taps in Heat Zones
      map.on("click", (e: L.LeafletMouseEvent) => {
        const clickedLatLng = e.latlng;
        let closestAnom: any = null;
        let minDistanceInFeet = Infinity;

        // Verify if click coordinates are inside any scanRange anomaly's heat zone
        bioAnomalies.forEach((anom) => {
          const distanceInMeters = map.distance(clickedLatLng, L.latLng(anom.lat, anom.lng));
          const distanceInFeet = distanceInMeters * 3.28084;
          const heatZoneRadius = (anom.heatZoneDiameter || 20) / 2;

          if (distanceInFeet <= heatZoneRadius) {
            if (distanceInFeet < minDistanceInFeet) {
              minDistanceInFeet = distanceInFeet;
              closestAnom = anom;
            }
          }
        });

        if (closestAnom) {
          onMapTapHeatZone(closestAnom.id, clickedLatLng.lat, clickedLatLng.lng, minDistanceInFeet);
        }
      });

      mapInstanceRef.current = map;
    }

    return () => {
      // Clean up map when component unmounts
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
        circleRef.current = null;
        playerMarkerRef.current = null;
        anomaliesLayerGroupRef.current = null;
      }
    };
  }, []);

  // Update map center when coordinates change (preserves manual zoom level)
  useEffect(() => {
    const map = mapInstanceRef.current;
    if (!map) return;

    const activeZoom = map.getZoom() || getZoomFromRadius(scanRadius);
    map.setView([userCoords.lat, userCoords.lng], activeZoom);

    if (playerMarkerRef.current) {
      playerMarkerRef.current.setLatLng([userCoords.lat, userCoords.lng]);
    }
  }, [userCoords.lat, userCoords.lng]);

  // Update scanner range circle center and radius without snapping map viewport zoom
  useEffect(() => {
    if (circleRef.current) {
      circleRef.current.setLatLng([userCoords.lat, userCoords.lng]);
      circleRef.current.setRadius(radiusInMeters);
    }
  }, [userCoords.lat, userCoords.lng, radiusInMeters]);

  // Render & Update Anomalies + Heat Zones + Harvesting Creatures dynamically
  useEffect(() => {
    const map = mapInstanceRef.current;
    const group = anomaliesLayerGroupRef.current;
    if (!map || !group) return;

    // Clear previous elements
    group.clearLayers();

    // Helper to calculate distance in feet
    const getDistanceInFeet = (lat1: number, lng1: number, lat2: number, lng2: number): number => {
      const dy = (lat2 - lat1) * 111000 * 3.28084;
      const dx = (lng2 - lng1) * 111000 * Math.cos(lat1 * Math.PI / 180) * 3.28084;
      return Math.sqrt(dx * dx + dy * dy);
    };

    // Group overlapping anomalies
    const visited = new Set<string>();
    const groupedAnomalies: any[][] = [];

    bioAnomalies.forEach((anom) => {
      if (visited.has(anom.id)) return;

      const currentGroup: any[] = [];
      const queue: any[] = [anom];
      visited.add(anom.id);

      while (queue.length > 0) {
        const curr = queue.shift();
        currentGroup.push(curr);

        const currSeed = absHash(curr.id);
        const currR0 = (curr.heatZoneDiameter || 20) / 2;
        const currEpsilon = 0.15 + (currSeed % 3) * 0.05;
        const currMaxRad = currR0 * (1.0 + currEpsilon);

        bioAnomalies.forEach((other) => {
          if (visited.has(other.id)) return;

          const otherSeed = absHash(other.id);
          const otherR0 = (other.heatZoneDiameter || 20) / 2;
          const otherEpsilon = 0.15 + (otherSeed % 3) * 0.05;
          const otherMaxRad = otherR0 * (1.0 + otherEpsilon);

          const dist = getDistanceInFeet(curr.lat, curr.lng, other.lat, other.lng);
          if (dist < (currMaxRad + otherMaxRad)) {
            visited.add(other.id);
            queue.push(other);
          }
        });
      }
      groupedAnomalies.push(currentGroup);
    });

    // Draw Anomaly groups (fills and outline contours)
    groupedAnomalies.forEach((g) => {
      if (g.length === 0) return;

      const hasSelected = g.some((anom) => selectedAnomalyId === anom.id);
      const leadAnom = g[0];

      let colorHex = "#EF4444"; // Infection: Red
      let pulseColorClass = "bg-red-500";
      let gradId = "grad-infection";
      if (leadAnom.faction === "Mech") {
        colorHex = "#EAB308";
        pulseColorClass = "bg-yellow-500";
        gradId = "grad-mech";
      } else if (leadAnom.faction === "Parasite") {
        colorHex = "#A855F7";
        pulseColorClass = "bg-purple-500";
        gradId = "grad-parasite";
      } else if (leadAnom.faction === "Containment") {
        colorHex = "#22D3EE";
        pulseColorClass = "bg-cyan-500";
        gradId = "grad-containment";
      }

      // Draw individual wavy fills referencing the global SVG radial gradient definition for a completely smooth fade
      g.forEach((anom) => {
        const points: [number, number][] = [];
        const steps = 36;
        for (let i = 0; i <= steps; i++) {
          const theta = (i * 2 * Math.PI) / steps;
          const r = getBoundaryRadius(anom, theta); // in feet
          const rMeters = r * 0.3048;
          const dy = rMeters * Math.sin(theta);
          const dx = rMeters * Math.cos(theta);
          const dLat = dy / 111000;
          const dLng = dx / (111000 * Math.cos(anom.lat * Math.PI / 180));
          points.push([anom.lat + dLat, anom.lng + dLng]);
        }

        L.polygon(points, {
          stroke: false,
          fillColor: `url(#${gradId})`,
          fillOpacity: hasSelected ? 1.0 : 0.75,
          interactive: false,
        }).addTo(group);
      });

      // Draw merged outline contour using outside segments
      const outlineSegments: [number, number][][] = [];
      g.forEach((anom) => {
        const steps = 72;
        const getLatLngAtAngle = (theta: number): [number, number] => {
          const r = getBoundaryRadius(anom, theta); // in feet
          const rMeters = r * 0.3048;
          const dy = rMeters * Math.sin(theta);
          const dx = rMeters * Math.cos(theta);
          const dLat = dy / 111000;
          const dLng = dx / (111000 * Math.cos(anom.lat * Math.PI / 180));
          return [anom.lat + dLat, anom.lng + dLng];
        };

        for (let i = 0; i < steps; i++) {
          const theta1 = (i * 2 * Math.PI) / steps;
          const theta2 = ((i + 1) * 2 * Math.PI) / steps;
          const p1 = getLatLngAtAngle(theta1);
          const p2 = getLatLngAtAngle(theta2);

          // Check if midpoint is outside all other anomalies in the group
          const midLat = (p1[0] + p2[0]) / 2;
          const midLng = (p1[1] + p2[1]) / 2;

          const isOutside = g.every((other) => {
            if (other.id === anom.id) return true;
            return !isPointInsideAnomaly(midLat, midLng, other);
          });

          if (isOutside) {
            outlineSegments.push([p1, p2]);
          }
        }
      });

      if (outlineSegments.length > 0) {
        L.polyline(outlineSegments, {
          color: colorHex,
          weight: hasSelected ? 2 : 1.2,
          opacity: hasSelected ? 0.8 : 0.45,
          dashArray: hasSelected ? "3, 3" : undefined,
        }).addTo(group);
      }

      // Draw individual targetable markers
      g.forEach((anom) => {
        const isSelected = selectedAnomalyId === anom.id;
        const heatZoneDiameter = anom.heatZoneDiameter || 20;

        const borderClass = isSelected 
          ? "border-2 border-purple-400 scale-125 ring-2 ring-purple-500/50" 
          : "border border-black";

        const anomalyIcon = L.divIcon({
          html: `
            <div class="relative flex items-center justify-center cursor-pointer font-mono" style="width: 24px; height: 24px;">
              <span class="absolute inline-flex h-full w-full rounded-full ${pulseColorClass} opacity-20 animate-ping"></span>
              <span class="relative inline-flex rounded-full h-3.5 w-3.5 ${pulseColorClass} ${borderClass} shadow-[0_0_8px_rgba(0,0,0,0.6)]"></span>
            </div>
          `,
          className: "",
          iconSize: [24, 24],
          iconAnchor: [12, 12],
        });

        const marker = L.marker([anom.lat, anom.lng], {
          icon: anomalyIcon,
          title: `${anom.name} (Heat Zone: ${heatZoneDiameter}ft Diameter)`,
        });

        marker.on("click", (e) => {
          L.DomEvent.stopPropagation(e);
          onSelectAnomaly(anom.id);
        });

        marker.bindTooltip(`
          <div style="background-color: #0c100e; color: #A855F7; border: 1px solid #581c87; padding: 5px 9px; font-family: monospace; font-size: 9px; border-radius: 4px; line-height: 1.3;">
            <b style="color: white; font-size: 10px;">${anom.name}</b><br/>
            HEAT ZONE: <span style="color: ${colorHex}; font-weight: bold;">${heatZoneDiameter} FT DIA</span><br/>
            DIST: ${anom.distance.toFixed(1)} FT &bull; SIGN: ${anom.gene}<br/>
            MIN/MAX CHANCE: Center 100% / Edge 0%
          </div>
        `, {
          direction: "top",
          offset: [0, -10],
          opacity: 0.95,
          className: "cyber-map-tooltip",
        });

        group.addLayer(marker);
      });
    });

    // 2. Draw Active Harvesting Creatures with interactive real-time popups
    harvestingMissions.forEach((mission) => {
      if (mission.isReturned) return; // Skip claims returned to player

      const now = Date.now();
      const elapsedSec = Math.floor((now - mission.startTime) / 1000);
      const progressPercent = Math.min(100, Math.round((elapsedSec / mission.totalDuration) * 100));
      const remainingSec = Math.max(0, mission.totalDuration - elapsedSec);
      const minStr = Math.floor(remainingSec / 60).toString().padStart(2, "0");
      const secStr = (remainingSec % 60).toString().padStart(2, "0");
      const remainingStr = `${minStr}:${secStr}`;

      let pulseColor = "bg-blue-500 shadow-[0_0_10px_#3b82f6]";
      let iconAvatar = "DNA";
      if (mission.creatureFaction === "Infection") {
        pulseColor = "bg-red-500 shadow-[0_0_10px_#ef4444]";
        iconAvatar = "!";
      } else if (mission.creatureFaction === "Mech") {
        pulseColor = "bg-yellow-500 shadow-[0_0_10px_#eab308]";
        iconAvatar = "MCH";
      } else if (mission.creatureFaction === "Parasite") {
        pulseColor = "bg-purple-500 shadow-[0_0_10px_#a855f7]";
        iconAvatar = "PR";
      } else if (mission.creatureFaction === "Containment") {
        pulseColor = "bg-cyan-500 shadow-[0_0_10px_#22d3ee]";
        iconAvatar = "CT";
      }

      const harvesterIcon = L.divIcon({
        html: `
          <div class="relative flex items-center justify-center cursor-pointer" style="width: 32px; height: 32px;">
            <span class="absolute inline-flex h-full w-full rounded-full ${pulseColor} opacity-25 animate-ping"></span>
            <span class="relative inline-flex rounded-full h-5 w-5 bg-neutral-950 border-2 border-purple-500 flex items-center justify-center text-[10px] shadow-lg">
              ${iconAvatar}
            </span>
            <span class="absolute -top-1.5 -right-1 text-[7px] bg-black text-purple-400 font-bold border border-purple-900 px-0.5 rounded scale-90">
              ${progressPercent}%
            </span>
          </div>
        `,
        className: "",
        iconSize: [32, 32],
        iconAnchor: [16, 16],
      });

      const hMarker = L.marker([mission.lat, mission.lng], {
        icon: harvesterIcon,
      });

      // Bind descriptive pop-up with full tactical metadata info card
      hMarker.bindPopup(`
        <div style="background-color: #070707; color: #a855f7; border: 2px solid #581c87; padding: 8px 12px; font-family: monospace; font-size: 9.5px; line-height: 1.4; min-width: 170px; border-radius: 6px; box-shadow: 0 0 15px rgba(168, 85, 247, 0.25);">
          <div style="border-b: 1px solid #581c87; padding-bottom: 4px; margin-bottom: 5px; color: white; font-weight: bold; font-size: 11px; text-transform: uppercase;">
             ${iconAvatar} ${mission.creatureName}
          </div>
          <p style="margin: 0 0 4px 0; font-size: 8px; color: #a8a29e;">MISSION STATUS: <strong style="color: ${mission.isCompleted ? '#d8b4fe' : '#eab308'}; font-weight: bold;">${mission.isCompleted ? 'READY FOR RETRIEVAL' : 'EXTRACTING GENE SEQUENCES'}</strong></p>
          <div style="background-color: #000; padding: 4px; border-radius: 3px; font-size: 8.5px; border: 1px solid #581c87; margin-bottom: 4px;">
            <b>GENE PACKETS:</b> ${mission.harvestedGenes.length}/4 INFLOW<br/>
            <b>ACQUIRED:</b> <span style="color: white; font-weight: bold;">${mission.harvestedGenes.join(", ") || "SCANNING..."}</span>
          </div>
          <div style="display: flex; justify-content: space-between; font-size: 8.5px; color: #a8a29e; margin-top: 4px;">
            <span>PROGRESS: <b>${progressPercent}%</b></span>
            <span>TIME: <b>${remainingStr}</b></span>
          </div>
          ${mission.isCompleted ? '<div style="background-color: #581c87; color: white; font-weight: bold; text-align: center; font-size: 8.5px; margin-top: 6px; border-radius: 2px; padding: 2px;">RECALL FROM TACTICAL HUD SCREEN</div>' : ''}
        </div>
      `, {
        className: "cyber-map-popup",
        offset: [0, -10],
      });

      group.addLayer(hMarker);
    });

  }, [bioAnomalies, selectedAnomalyId, onSelectAnomaly, harvestingMissions]);

  return (
    <div className="relative w-full h-full rounded border border-purple-950/40 overflow-hidden bg-neutral-950">
      {/* Global SVG Radial Gradients Defs for hardware-accelerated, completely smooth vector gradients */}
      <svg style={{ position: "absolute", width: 0, height: 0, overflow: "hidden" }}>
        <defs>
          <radialGradient id="grad-infection" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor="#EF4444" stopOpacity="0.45" />
            <stop offset="100%" stopColor="#EF4444" stopOpacity="0.0" />
          </radialGradient>
          <radialGradient id="grad-mech" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor="#EAB308" stopOpacity="0.45" />
            <stop offset="100%" stopColor="#EAB308" stopOpacity="0.0" />
          </radialGradient>
          <radialGradient id="grad-parasite" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor="#A855F7" stopOpacity="0.45" />
            <stop offset="100%" stopColor="#A855F7" stopOpacity="0.0" />
          </radialGradient>
          <radialGradient id="grad-containment" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor="#22D3EE" stopOpacity="0.45" />
            <stop offset="100%" stopColor="#22D3EE" stopOpacity="0.0" />
          </radialGradient>
        </defs>
      </svg>

      {/* Visual cybernetic scanner crosshair overlay on map */}
      <div className="absolute inset-0 pointer-events-none z-[1000] border-2 border-purple-500/10 flex items-center justify-center">
        {/* Radar concentric circular lines */}
        <div className="w-[120px] h-[120px] border border-purple-500/5 rounded-full absolute pointer-events-none" />
        <div className="w-[240px] h-[240px] border border-purple-500/5 rounded-full absolute pointer-events-none" />
        <div className="w-[360px] h-[360px] border border-purple-500/5 rounded-full absolute pointer-events-none" />
        
        {/* Center scope sights */}
        <div className="w-[12px] h-[1px] bg-purple-500/30 absolute" />
        <div className="w-[1px] h-[12px] bg-purple-500/30 absolute" />
      </div>

       {/* DOM element where Leaflet maps attaches */}
      <div ref={mapContainerRef} className="w-full h-full select-none" />
    </div>
  );
};
