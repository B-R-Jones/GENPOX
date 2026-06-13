/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useEffect, useRef } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";

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

    // 1. Draw Anomaly Heat Zone circular ranges and markers
    bioAnomalies.forEach((anom) => {
      const isSelected = selectedAnomalyId === anom.id;
      const heatZoneDiameter = anom.heatZoneDiameter || 20;
      const heatZoneRadiusInMeters = (heatZoneDiameter / 2) * 0.3048;

      let colorHex = "#EF4444"; // Infection: Red
      let pulseColorClass = "bg-red-500";
      if (anom.faction === "Mech") {
        colorHex = "#EAB308";
        pulseColorClass = "bg-yellow-500";
      } else if (anom.faction === "Parasite") {
        colorHex = "#A855F7";
        pulseColorClass = "bg-purple-500";
      } else if (anom.faction === "Containment") {
        colorHex = "#22D3EE";
        pulseColorClass = "bg-cyan-500";
      }

      // Add Heat Zone Circle outline + fill representing temperature radiation
      const heatZoneCircle = L.circle([anom.lat, anom.lng], {
        radius: heatZoneRadiusInMeters,
        color: colorHex,
        weight: isSelected ? 2 : 1.2,
        opacity: isSelected ? 0.8 : 0.45,
        fillColor: colorHex,
        fillOpacity: isSelected ? 0.22 : 0.12,
        dashArray: isSelected ? "3, 3" : undefined,
      }).addTo(group);

      // Cybernetic heat spot marker tag
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
        // Stop bubbling and trigger selection
        L.DomEvent.stopPropagation(e);
        onSelectAnomaly(anom.id);
      });

      // Bind tactical information HUD
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
      let iconAvatar = "🧬";
      if (mission.creatureFaction === "Infection") {
        pulseColor = "bg-red-500 shadow-[0_0_10px_#ef4444]";
        iconAvatar = "⚠️";
      } else if (mission.creatureFaction === "Mech") {
        pulseColor = "bg-yellow-500 shadow-[0_0_10px_#eab308]";
        iconAvatar = "🤖";
      } else if (mission.creatureFaction === "Parasite") {
        pulseColor = "bg-purple-500 shadow-[0_0_10px_#a855f7]";
        iconAvatar = "👾";
      } else if (mission.creatureFaction === "Containment") {
        pulseColor = "bg-cyan-500 shadow-[0_0_10px_#22d3ee]";
        iconAvatar = "🛰️";
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
