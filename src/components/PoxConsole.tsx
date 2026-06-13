/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect, useRef, MouseEvent } from "react";
import { motion, AnimatePresence } from "motion/react";
import { 
  Dna, Cpu, ShieldAlert, Wifi, Volume2, VolumeX, 
  RefreshCw, Trash2, Check, X, Shield, Zap, Heart, Sparkles, Star,
  Info, AlertTriangle, Compass, Play, List, Users, Power, Terminal, FolderOpen, MapPin, Search, Camera
} from "lucide-react";
import { QRCodeSVG } from "qrcode.react";
import { Html5Qrcode } from "html5-qrcode";
import { LeafletScannerMap } from "./LeafletScannerMap";
import { Creature, GeneSequence, NearbyUser, TradeSession } from "../types";
import { 
  generateRandomGeneBlock, 
  generateRandom64Sequence, 
  constructProceduralCreature, 
  createRandomNearbyUser,
  getUnlockedMoves
} from "../utils/genetic";
import { sound } from "../utils/audio";
import { getDailyWaveConfig, generateWaveGeneBlock } from "../utils/wave";
import ForecastCalendar from "./ForecastCalendar";
import { Circle } from "./circle";

// Deterministic 128 daily target sequences generator
export const generateDailyBountySequences = (seedStr: string): string[] => {
  const seqs: string[] = [];
  let seed = 0;
  for (let i = 0; i < seedStr.length; i++) {
    seed += seedStr.charCodeAt(i);
  }
  const random = () => {
    const x = Math.sin(seed++) * 10000;
    return x - Math.floor(x);
  };
  const bases = ['A', 'G', 'T', 'C'];
  for (let i = 0; i < 128; i++) {
    let seq = "";
    for (let j = 0; j < 64; j++) {
      seq += bases[Math.floor(random() * 4)];
    }
    if (!seqs.includes(seq)) {
      seqs.push(seq);
    } else {
      i--;
    }
  }
  return seqs;
};

export interface GenMail {
  id: string;
  date: string;
  tagline: string;
  isRead: boolean;
  sequences: string[];
  type?: 'dispatch' | 'auto_hack_log' | 'trade_request' | 'defense_log';
  createdAt?: number;
  expiresAt?: number;
  autoHackLog?: {
    creature: Creature;
    log: string[];
    isDefeated?: boolean;
    genesGained?: string[];
    genesLost?: string[];
  };
  tradeRequest?: {
    id: string;
    partner: NearbyUser;
    partnerOfferedCreature: Creature;
    status: 'pending' | 'accepted' | 'declined';
    selectedCreatureToOffer?: Creature;
  };
}

const retroMapStyles = [
  { elementType: "geometry", stylers: [{ color: "#010d02" }] },
  { elementType: "labels.text.stroke", stylers: [{ color: "#010d02" }] },
  { elementType: "labels.text.fill", stylers: [{ color: "#00FF41" }] },
  {
    featureType: "administrative",
    elementType: "geometry",
    stylers: [{ color: "#003a00" }]
  },
  {
    featureType: "poi",
    elementType: "geometry",
    stylers: [{ color: "#001e00" }]
  },
  {
    featureType: "road",
    elementType: "geometry",
    stylers: [{ color: "#004000" }]
  },
  {
    featureType: "road",
    elementType: "geometry.stroke",
    stylers: [{ color: "#001e00" }]
  },
  {
    featureType: "water",
    elementType: "geometry",
    stylers: [{ color: "#000800" }]
  }
];

const getZoomFromRadius = (radius: number): number => {
  if (radius <= 30) return 21;
  if (radius <= 55) return 20;
  if (radius <= 80) return 19;
  return 18;
};

function getCoherence(seq: string, target: string): 'full' | 'partial' | 'none' {
  if (!seq || !target) return 'none';
  const seq64 = seq.substring(0, 64);
  const target64 = target.substring(0, 64);
  if (seq64 === target64) return 'full';
  
  let alignedMatches = 0;
  for (let i = 0; i < 8; i++) {
    const seqGene = seq64.substring(i * 8, (i + 1) * 8);
    const tgtGene = target64.substring(i * 8, (i + 1) * 8);
    if (seqGene && tgtGene && seqGene === tgtGene) {
      alignedMatches++;
    }
  }
  if (alignedMatches === 8) return 'full';
  if (alignedMatches >= 1) return 'partial';

  for (let i = 0; i < 8; i++) {
    const tgtGene = target64.substring(i * 8, (i + 1) * 8);
    if (tgtGene && seq.includes(tgtGene)) {
      return 'partial';
    }
  }

  return 'none';
}

const getEmotSoundDetails = (sequence: string) => {
  const chars = sequence.toUpperCase().replace(/[^AGTC]/g, "");
  if (chars.length === 0) return { oscillator: "none", frequencies: [], noteDelay: 0.08, noteDuration: 0.06 };
  
  const numNotes = Math.min(8, Math.max(4, Math.floor(chars.length / 8)));
  
  let acount = 0, gcount = 0, tcount = 0, ccount = 0;
  for (let i = 0; i < chars.length; i++) {
    if (chars[i] === 'A') acount++;
    else if (chars[i] === 'G') gcount++;
    else if (chars[i] === 'T') tcount++;
    else if (chars[i] === 'C') ccount++;
  }
  
  const maxCount = Math.max(acount, gcount, tcount, ccount);
  let oscType = "sine";
  if (maxCount === acount) oscType = "sine";
  else if (maxCount === gcount) oscType = "triangle";
  else if (maxCount === tcount) oscType = "sawtooth";
  else if (maxCount === ccount) oscType = "square";

  const noteDelay = 0.08 + (acount % 3) * 0.02;
  const frequencies: number[] = [];

  for (let i = 0; i < numNotes; i++) {
    const charBlock = chars.slice(i * 4, (i + 1) * 4);
    let baseValue = 0;
    for (let j = 0; j < charBlock.length; j++) {
      const c = charBlock[j];
      if (c === 'A') baseValue += 1;
      if (c === 'G') baseValue += 2;
      if (c === 'T') baseValue += 3;
      if (c === 'C') baseValue += 4;
    }
    const freq = 180 + (baseValue * 30) + (i * 40);
    frequencies.push(Math.round(freq));
  }

  const noteDuration = 0.06 + (gcount % 4) * 0.03;

  return {
    oscillator: oscType,
    frequencies,
    noteDelay: Number(noteDelay.toFixed(4)),
    noteDuration: Number(noteDuration.toFixed(4))
  };
};

const encodeCreatureToBase64 = (creature: Creature): string => {
  const compact = {
    id: creature.id,
    sequence: creature.sequence,
    name: creature.name,
    faction: creature.faction,
    type: creature.type,
    vitality: creature.vitality,
    attack: creature.attack,
    defense: creature.defense,
    spd: creature.speed,
    weapon: creature.primaryWeapon,
    lore: creature.lore,
    ascii: creature.asciiArt,
    appended: creature.appendedGenes || [],
    telomeres: creature.telomeres !== undefined ? creature.telomeres : 100
  };
  const jsonStr = JSON.stringify(compact);
  return btoa(unescape(encodeURIComponent(jsonStr)));
};

const decodeCreatureFromBase64 = (base64String: string): Creature | null => {
  try {
    const jsonStr = decodeURIComponent(escape(atob(base64String.trim())));
    const parsed = JSON.parse(jsonStr);
    if (!parsed.id || !parsed.sequence || !parsed.name) return null;
    return {
      id: parsed.id,
      sequence: parsed.sequence,
      name: parsed.name,
      faction: parsed.faction || "Infection",
      type: parsed.type || "Unknown Mutant",
      vitality: parsed.vitality || 100,
      attack: parsed.attack || 50,
      defense: parsed.defense || 50,
      speed: parsed.spd || parsed.speed || 50,
      primaryWeapon: parsed.weapon || parsed.primaryWeapon || "None",
      lore: parsed.lore || "Imported specimen.",
      asciiArt: parsed.ascii || parsed.asciiArt || "o.x.o\n.###.\nx.o.x",
      discoveredAt: Date.now(),
      origin: "Traded via QR code",
      appendedGenes: parsed.appended || [],
      telomeres: parsed.telomeres !== undefined ? parsed.telomeres : 100
    };
  } catch (e) {
    console.error("decodeCreatureFromBase64 error:", e);
    return null;
  }
};

interface NodeCrystalCanvasProps {
  metrics: {
    subnodeDepthG: number;
    subnodeDepthA: number;
    subnodeDepthT: number;
    subnodeDepthC: number;
    totalNodeDepth: number;
    subnodeWidthG: number;
    subnodeWidthA: number;
    subnodeWidthT: number;
    subnodeWidthC: number;
    totalNodeWidth: number;
    color: { r: number; g: number; b: number };
  };
  inventoryStrings: string[];
}

const NodeCrystalCanvas: React.FC<NodeCrystalCanvasProps> = ({ metrics, inventoryStrings }) => {
  const canvasRef = React.useRef<HTMLCanvasElement | null>(null);
  const metricsRef = React.useRef(metrics);
  const inventoryStringsRef = React.useRef(inventoryStrings);

  React.useEffect(() => {
    metricsRef.current = metrics;
  }, [metrics]);

  React.useEffect(() => {
    inventoryStringsRef.current = inventoryStrings;
  }, [inventoryStrings]);

  React.useEffect(() => {
    let animId: number;
    let cancelled = false;

    // Helper: Draw mathematically distorted line with wave displacement (Chaotic Wave Displacement)
    const drawWaveLine = (
      ctx: CanvasRenderingContext2D,
      x1: number,
      y1: number,
      x2: number,
      y2: number,
      segments: number,
      amplitude: number,
      frequency: number,
      phase: number
    ) => {
      ctx.beginPath();
      // Distance between points
      const dx = x2 - x1;
      const dy = y2 - y1;
      const len = Math.hypot(dx, dy);
      const angle = Math.atan2(dy, dx);

      ctx.moveTo(x1, y1);
      for (let i = 1; i <= segments; i++) {
        const t = i / segments;
        // Position along the straight line
        const px = x1 + dx * t;
        const py = y1 + dy * t;

        // Sine wave offset perp to line
        // We multiply sin (t * Math.PI) so displacement drops to 0 at the endpoints (protect joint structures)
        const envelope = Math.sin(t * Math.PI);
        const waveOffset = amplitude * Math.sin(t * len * frequency + phase) * envelope;

        // Add perpendicular offset vector
        const perpX = -Math.sin(angle) * waveOffset;
        const perpY = Math.cos(angle) * waveOffset;

        ctx.lineTo(px + perpX, py + perpY);
      }
      ctx.stroke();
    };

    const render = (timestamp: number) => {
      if (cancelled) return;
      const canvas = canvasRef.current;
      if (!canvas) {
        animId = requestAnimationFrame(render);
        return;
      }
      const ctx = canvas.getContext('2d');
      if (!ctx) {
        animId = requestAnimationFrame(render);
        return;
      }

      // Match high DPI
      const dpr = window.devicePixelRatio || 1;
      const width = 180;
      const height = 180;
      canvas.width = width * dpr;
      canvas.height = height * dpr;
      canvas.style.width = `${width}px`;
      canvas.style.height = `${height}px`;
      ctx.scale(dpr, dpr);

      const cx = width / 2;
      const cy = height / 2;

      // 1. Environmental & Data-Driven Modifiers:
      
      // A. LUNAR PHASE (The Cosmic Baseline) - 0.0 (New Moon) to 0.5 (Full Moon)
      const dCurrent = new Date();
      const seconds = dCurrent.getTime() / 1000;
      const knownNewMoon = 1704974220; // Known New Moon epoch
      const synodicMonth = 2551443; // 29.53059 days in seconds
      const diff = seconds - knownNewMoon;
      const rawPhase = (diff / synodicMonth) % 1;
      const phaseFraction = rawPhase < 0 ? rawPhase + 1 : rawPhase;
      const lunarPhase = 0.5 - Math.abs(phaseFraction - 0.5); // Ranges continuous: 0.0 (New Moon) to 0.5 (Full Moon)

      // B. DAILY DINUCLEOTIDE RESONANCE (The Genetic Antenna)
      const year = dCurrent.getFullYear();
      const month = dCurrent.getMonth() + 1;
      const day = dCurrent.getDate();
      const dateSeed = year * 10000 + month * 100 + day;
      const DINUCLEOTIDES = [
        "GG", "GA", "GT", "GC",
        "AG", "AA", "AT", "AC",
        "TG", "TA", "TT", "TC",
        "CG", "CA", "CT", "CC"
      ];
      const dailyResonancePair = DINUCLEOTIDES[dateSeed % 16];

      // Calculate frequency of dailyResonancePair in active inventory
      const list = inventoryStringsRef.current || [];
      let pairCount = 0;
      list.forEach(seq => {
        for (let i = 0; i < seq.length - 1; i++) {
          if (seq.substring(i, i + 2) === dailyResonancePair) {
            pairCount++;
          }
        }
      });
      const resonanceIntensity = Math.min(1.0, pairCount / 8);

      // Baseline radius R structural modification from Lunar Phase
      const lunarBaselineMod = 1.0 + lunarPhase * 0.15; // full moon expands radius up to 15%
      const R = (Math.min(width, height) / 2) * 0.65 * lunarBaselineMod;

      const m = metricsRef.current;
      const subWidthG = m.subnodeWidthG;
      const subWidthA = m.subnodeWidthA;
      const subWidthT = m.subnodeWidthT;
      const subWidthC = m.subnodeWidthC;
      const depthG = m.subnodeDepthG;
      const depthA = m.subnodeDepthA;
      const depthT = m.subnodeDepthT;
      const depthC = m.subnodeDepthC;
      const totDepth = m.totalNodeDepth;

      // Collection Balance calculations for Chaotic Wave displacement amplitude
      const widths = [subWidthG, subWidthA, subWidthT, subWidthC];
      const avgWidth = widths.reduce((sum, w) => sum + w, 0) / 4;
      const variance = widths.reduce((sum, w) => sum + Math.pow(w - avgWidth, 2), 0) / 4;
      const skewness = Math.sqrt(variance);

      // Amplitude: Skewed decks = jagged, unstable vibrations; Balanced decks = uniform waves.
      // Frequency: Tying to base generation / totalNodeDepth
      const waveAmplitude = 0.5 + skewness * 12.0; // skewness pushes wave variations
      const waveFrequency = 0.05 + totDepth * 0.08; // speed and spatial density of waves
      const ripplePhaseAngle = 0; // Perfectly static (no scrolling waves over time)

      // Unit anchor vertex positions (G top, T right, C bottom, A left)
      const ptG = { x: cx, y: cy - R * subWidthG };
      const ptT = { x: cx + R * subWidthT, y: cy };
      const ptC = { x: cx, y: cy + R * subWidthC };
      const ptA = { x: cx - R * subWidthA, y: cy };

      ctx.clearRect(0, 0, width, height);

      // Background grid (subtle radar boundaries and axes)
      ctx.save();
      ctx.strokeStyle = "rgba(0, 255, 65, 0.05)";
      ctx.lineWidth = 0.5;

      const steps = [0.25, 0.5, 0.75, 1.0];
      steps.forEach(step => {
        ctx.beginPath();
        ctx.moveTo(cx, cy - R * step);
        ctx.lineTo(cx + R * step, cy);
        ctx.lineTo(cx, cy + R * step);
        ctx.lineTo(cx - R * step, cy);
        ctx.closePath();
        ctx.stroke();

        ctx.beginPath();
        ctx.arc(cx, cy, R * step, 0, 2 * Math.PI);
        ctx.setLineDash([2, 5]);
        ctx.stroke();
        ctx.setLineDash([]);
      });

      ctx.strokeStyle = "rgba(0, 255, 65, 0.09)";
      ctx.beginPath();
      ctx.moveTo(cx, cy - R);
      ctx.lineTo(cx, cy + R);
      ctx.moveTo(cx - R, cy);
      ctx.lineTo(cx + R, cy);
      ctx.stroke();
      ctx.restore();

      // Strain color specifications
      const colorsList = [
        { r: 239, g: 68, b: 68, char: 'G' },   // Red (G)
        { r: 59, g: 130, b: 246, char: 'T' },  // Blue (T)
        { r: 234, g: 179, b: 8, char: 'C' },   // Yellow (C)
        { r: 34, g: 197, b: 94, char: 'A' }    // Green (A)
      ];

      // RESONANCE GLITCH SPIKE
      // Check if current cycle color crosses daily basepair bases
      const cycleDur = 4500;
      const tCycle = (timestamp / cycleDur) % 1;
      const activeIdx = Math.floor(tCycle * 4) % 4;
      const activeChar = colorsList[activeIdx].char;
      const isCrossesResonance = dailyResonancePair.includes(activeChar);

      // Gentle Multiplier scales subtle line-glow adjustments safely
      const glitchMultiplier = isCrossesResonance ? (1.0 + resonanceIntensity * 0.15) : 1.0;

      // Perfectly stationary offsets (no translation movement)
      const jitterX = 0;
      const jitterY = 0;

      // 2. Draw white-hot Full Moon Corona if Lunar Phase >= 0.35
      if (lunarPhase >= 0.35) {
        const coronaIntensity = (lunarPhase - 0.35) / 0.15; // 0.0 to 1.0
        ctx.save();
        const radCorona = ctx.createRadialGradient(cx, cy, 2, cx, cy, R * 1.5);
        radCorona.addColorStop(0, `rgba(255, 255, 255, ${coronaIntensity * 0.45})`);
        radCorona.addColorStop(0.2, `rgba(255, 255, 255, ${coronaIntensity * 0.25})`);
        radCorona.addColorStop(0.5, `rgba(239, 68, 68, ${coronaIntensity * 0.12})`);
        radCorona.addColorStop(0.8, `rgba(234, 179, 8, ${coronaIntensity * 0.06})`);
        radCorona.addColorStop(1.0, "rgba(0, 0, 0, 0)");
        ctx.fillStyle = radCorona;
        ctx.beginPath();
        ctx.arc(cx, cy, R * 1.5, 0, 2 * Math.PI);
        ctx.fill();
        ctx.restore();
      }

      // Quiet static cycle gradient getter
      const getCycleColorStr = (tVal: number, alpha: number) => {
        const t = (tVal / cycleDur) % 1;
        const pos = t * 4;
        const idx1 = Math.floor(pos) % 4;
        const idx2 = (idx1 + 1) % 4;
        const f = pos % 1;
        const r = Math.round(colorsList[idx1].r * (1 - f) + colorsList[idx2].r * f);
        const g = Math.round(colorsList[idx1].g * (1 - f) + colorsList[idx2].g * f);
        const b = Math.round(colorsList[idx1].b * (1 - f) + colorsList[idx2].b * f);
        return `rgba(${r}, ${g}, ${b}, ${alpha})`;
      };

      // CHROMATIC ABERRATION - Split G (Red) and T (Blue) rendering layers if lunar phase is high
      const isFullMoonSplit = lunarPhase >= 0.4;
      const abAmount = isFullMoonSplit ? (lunarPhase - 0.4) * 55.0 : 0.0; // split distance in pixels

      // 3. Render Inner Layers / Recursive Geometric Layering
      // Nested concentric iterations scaled down and phase shifted proportional to player's depth
      const numHarmonics = 3;
      const alphaG = depthG * (lunarPhase < 0.1 ? 0 : 0.62) + (depthG > 0 ? 0.08 : 0);
      const alphaT = depthT * (lunarPhase < 0.1 ? 0 : 0.62) + (depthT > 0 ? 0.08 : 0);
      const alphaC = depthC * (lunarPhase < 0.1 ? 0 : 0.62) + (depthC > 0 ? 0.08 : 0);
      const alphaA = depthA * (lunarPhase < 0.1 ? 0 : 0.62) + (depthA > 0 ? 0.08 : 0);

      // Draw shell outlines & fills
      const drawShellLayer = (scale: number, rotationOffset: number, alphaMod: number) => {
        ctx.save();
        ctx.translate(cx + jitterX, cy + jitterY);
        ctx.rotate(rotationOffset);

        // Unit points adjusted for scale
        const hG = { x: 0, y: -R * subWidthG * scale };
        const hT = { x: R * subWidthT * scale, y: 0 };
        const hC = { x: 0, y: R * subWidthC * scale };
        const hA = { x: -R * subWidthA * scale, y: 0 };

        // Fill inner geometry if not New Moon (where inner fill decays to 0 transparent isolation)
        if (lunarPhase >= 0.05) {
          ctx.beginPath();
          ctx.moveTo(hG.x, hG.y);
          ctx.lineTo(hT.x, hT.y);
          ctx.lineTo(hC.x, hC.y);
          ctx.lineTo(hA.x, hA.y);
          ctx.closePath();
          ctx.save();
          ctx.clip();

          // Draw overlapping vertex radial gradients in local space
          const radG = ctx.createRadialGradient(hG.x, hG.y, 0, hG.x, hG.y, R * 1.3);
          radG.addColorStop(0, `rgba(239, 68, 68, ${alphaG * alphaMod * 0.42})`);
          radG.addColorStop(1, "rgba(239, 68, 68, 0)");
          ctx.fillStyle = radG;
          ctx.fillRect(-width, -height, width * 2, height * 2);

          const radT = ctx.createRadialGradient(hT.x, hT.y, 0, hT.x, hT.y, R * 1.3);
          radT.addColorStop(0, `rgba(59, 130, 246, ${alphaT * alphaMod * 0.42})`);
          radT.addColorStop(1, "rgba(59, 130, 246, 0)");
          ctx.fillStyle = radT;
          ctx.fillRect(-width, -height, width * 2, height * 2);

          const radC = ctx.createRadialGradient(hC.x, hC.y, 0, hC.x, hC.y, R * 1.3);
          radC.addColorStop(0, `rgba(234, 179, 8, ${alphaC * alphaMod * 0.42})`);
          radC.addColorStop(1, "rgba(234, 179, 8, 0)");
          ctx.fillStyle = radC;
          ctx.fillRect(-width, -height, width * 2, height * 2);

          const radA = ctx.createRadialGradient(hA.x, hA.y, 0, hA.x, hA.y, R * 1.3);
          radA.addColorStop(0, `rgba(34, 197, 94, ${alphaA * alphaMod * 0.42})`);
          radA.addColorStop(1, "rgba(34, 197, 94, 0)");
          ctx.fillStyle = radA;
          ctx.fillRect(-width, -height, width * 2, height * 2);

          ctx.restore();
        }

        // Draw distorted frame boundaries
        const drawEdge = (pStart: { x: number; y: number }, pEnd: { x: number; y: number }, colStart: string, colEnd: string) => {
          const grad = ctx.createLinearGradient(pStart.x, pStart.y, pEnd.x, pEnd.y);
          grad.addColorStop(0, colStart);
          grad.addColorStop(1, colEnd);
          ctx.strokeStyle = grad;
          ctx.lineWidth = (0.7 + (totDepth / 4) * 0.8) * (scale + 0.1) * glitchMultiplier;
          
          if (scale === 1.0) {
            ctx.shadowColor = colStart;
            ctx.shadowBlur = (2 + (totDepth / 4) * 4) * glitchMultiplier;
          }

          drawWaveLine(ctx, pStart.x, pStart.y, pEnd.x, pEnd.y, idxSegmentCount, waveAmplitude * scale, waveFrequency, ripplePhaseAngle + scale * 10);
        };

        const idxSegmentCount = 20;

        // Chromatic split handling on draw values if Full Moon Active
        if (abAmount > 0) {
          // Draw Red Component slightly shifted
          ctx.save();
          // Shift slightly left
          ctx.translate(-abAmount * scale, 0);
          drawEdge(hG, hT, `rgba(239, 68, 68, ${alphaMod * 0.95})`, `rgba(239, 68, 68, ${alphaMod * 0.22})`);
          drawEdge(hT, hC, `rgba(239, 68, 68, ${alphaMod * 0.22})`, `rgba(239, 68, 68, ${alphaMod * 0.22})`);
          drawEdge(hC, hA, `rgba(239, 68, 68, ${alphaMod * 0.22})`, `rgba(239, 68, 68, ${alphaMod * 0.95})`);
          drawEdge(hA, hG, `rgba(239, 68, 68, ${alphaMod * 0.95})`, `rgba(239, 68, 68, ${alphaMod * 0.95})`);
          ctx.restore();

          // Draw Blue Component slightly shifted right
          ctx.save();
          ctx.translate(abAmount * scale, 0);
          drawEdge(hG, hT, `rgba(59, 130, 246, ${alphaMod * 0.22})`, `rgba(59, 130, 246, ${alphaMod * 0.95})`);
          drawEdge(hT, hC, `rgba(59, 130, 246, ${alphaMod * 0.95})`, `rgba(59, 130, 246, ${alphaMod * 0.22})`);
          drawEdge(hC, hA, `rgba(59, 130, 246, ${alphaMod * 0.22})`, `rgba(59, 130, 246, ${alphaMod * 0.22})`);
          drawEdge(hA, hG, `rgba(59, 130, 246, ${alphaMod * 0.22})`, `rgba(59, 130, 246, ${alphaMod * 0.22})`);
          ctx.restore();

          // Joint standard blend
          drawEdge(hG, hT, `rgba(234, 179, 8, ${alphaC * alphaMod})`, `rgba(239, 68, 68, ${alphaG * alphaMod})`);
          drawEdge(hT, hC, `rgba(34, 197, 94, ${alphaA * alphaMod})`, `rgba(234, 179, 8, ${alphaC * alphaMod})`);
          drawEdge(hC, hA, `rgba(234, 179, 8, ${alphaC * alphaMod})`, `rgba(34, 197, 94, ${alphaA * alphaMod})`);
          drawEdge(hA, hG, `rgba(34, 197, 94, ${alphaA * alphaMod})`, `rgba(239, 68, 68, ${alphaG * alphaMod})`);
        } else {
          // Standard full spectrum gradients
          drawEdge(hG, hT, `rgba(239, 68, 68, ${alphaG * alphaMod + 0.1})`, `rgba(59, 130, 246, ${alphaT * alphaMod + 0.1})`);
          drawEdge(hT, hC, `rgba(59, 130, 246, ${alphaT * alphaMod + 0.1})`, `rgba(234, 179, 8, ${alphaC * alphaMod + 0.1})`);
          drawEdge(hC, hA, `rgba(234, 179, 8, ${alphaC * alphaMod + 0.1})`, `rgba(34, 197, 94, ${alphaA * alphaMod + 0.1})`);
          drawEdge(hA, hG, `rgba(34, 197, 94, ${alphaA * alphaMod + 0.1})`, `rgba(239, 68, 68, ${alphaG * alphaMod + 0.1})`);
        }

        ctx.restore();
      };

      // 1. Draw outer main shell (no rotation offset/rotation = 0, scale = 1.0)
      drawShellLayer(1.0, 0, 1.0);

      // 2. Draw nested recursive echo harmonics. 
      // Pulse the concentric inner shells outward gently/slowly.
      for (let k = 1; k <= numHarmonics; k++) {
        const tPhase = (timestamp * 0.00008 - k * 0.3) % 1;
        const progress = tPhase < 0 ? tPhase + 1 : tPhase;
        const hScale = 0.15 + progress * 0.83; // Expand smoothly outward
        const alphaFade = Math.sin(progress * Math.PI) * (0.65 - (k * 0.1));
        drawShellLayer(hScale, 0, alphaFade);
      }

      // vertices coordinates in global (absolute, un-rotated outer shell coordinates) for point markers
      // 5. Render Vertices with chromatic flare glowing dots (completely static, no slow strobe effect)
      ctx.save();
      const dotMarkers = [
        { pt: ptG, col: "rgb(239, 68, 68)", size: 3.5 + alphaG * 2.0, abX: -abAmount * 0.3, abY: 0 },   // Red (G)
        { pt: ptT, col: "rgb(59, 130, 246)", size: 3.5 + alphaT * 2.0, abX: abAmount * 0.3, abY: 0 },  // Blue (T)
        { pt: ptC, col: "rgb(234, 179, 8)", size: 3.5 + alphaC * 2.0, abX: 0, abY: 0 },               // Yellow (C)
        { pt: ptA, col: "rgb(34, 197, 94)", size: 3.5 + alphaA * 2.0, abX: 0, abY: 0 }                // Green (A)
      ];

      dotMarkers.forEach(d => {
        ctx.beginPath();
        ctx.arc(d.pt.x + jitterX + d.abX, d.pt.y + jitterY, d.size, 0, 2 * Math.PI);
        ctx.fillStyle = d.col;
        ctx.shadowColor = d.col;
        ctx.shadowBlur = (6 + (totDepth / 4) * 12);
        ctx.fill();
      });
      ctx.restore();

      // Unit Anchor Point kept calm and static
      ctx.beginPath();
      ctx.arc(cx + jitterX, cy + jitterY, 1.5, 0, 2 * Math.PI);
      ctx.fillStyle = "rgba(0, 255, 65, 0.55)";
      ctx.shadowColor = "#00FF41";
      ctx.shadowBlur = 4;
      ctx.fill();

      animId = requestAnimationFrame(render);
    };

    animId = requestAnimationFrame(render);

    return () => {
      cancelled = true;
      cancelAnimationFrame(animId);
    };
  }, []);

  return (
    <div className="relative flex justify-center items-center my-0.5 select-none">
      <div className="absolute top-0.5 left-0.5 w-2.5 h-2.5 border-t-2 border-l-2 border-[#00FF41]/40" />
      <div className="absolute top-0.5 right-0.5 w-2.5 h-2.5 border-t-2 border-r-2 border-[#00FF41]/40" />
      <div className="absolute bottom-0.5 left-0.5 w-2.5 h-2.5 border-b-2 border-l-2 border-[#00FF41]/40" />
      <div className="absolute bottom-0.5 right-0.5 w-2.5 h-2.5 border-b-2 border-r-2 border-[#00FF41]/40" />
      <canvas
        ref={canvasRef}
        className="block bg-neutral-950/80 border border-green-950/60 rounded-md shadow-[0_0_20px_rgba(0,180,40,0.22)]"
      />
    </div>
  );
};

const FlickeringPurpleText: React.FC<{ text: string; className?: string }> = ({ text, className = "" }) => {
  const anomalousBases = "XZYW?!$%&@#";
  const [displayText, setDisplayText] = useState(text);

  useEffect(() => {
    let timeoutId: NodeJS.Timeout;

    const doFlicker = () => {
      if (!text || text.trim() === "") return;
      let charIndex = Math.floor(Math.random() * text.length);
      let attempts = 0;
      while (text[charIndex] === " " && attempts < 20) {
        charIndex = Math.floor(Math.random() * text.length);
        attempts++;
      }

      const randomBase = anomalousBases[Math.floor(Math.random() * anomalousBases.length)];
      const updated =
        text.substring(0, charIndex) +
        randomBase +
        text.substring(charIndex + 1);

      setDisplayText(updated);

      timeoutId = setTimeout(() => {
        setDisplayText(text);
        const nextDelay = 1500 + Math.random() * 2500;
        timeoutId = setTimeout(doFlicker, nextDelay);
      }, 80 + Math.random() * 120);
    };

    const initialDelay = 1500 + Math.random() * 2000;
    timeoutId = setTimeout(doFlicker, initialDelay);

    return () => {
      clearTimeout(timeoutId);
    };
  }, [text]);

  return <span className={className}>{displayText}</span>;
};

const FlickeringAnomalyTitle: React.FC = () => {
  const originalText = "GENETIC ANOMALY HARMONIZER";
  const anomalousBases = "XZYW?!$%&@#";
  const [displayText, setDisplayText] = useState(originalText);

  useEffect(() => {
    let timeoutId: NodeJS.Timeout;

    const doFlicker = () => {
      // Find a non-space character index
      let charIndex = Math.floor(Math.random() * originalText.length);
      while (originalText[charIndex] === " " && originalText.includes(" ")) {
        charIndex = Math.floor(Math.random() * originalText.length);
      }

      const randomBase = anomalousBases[Math.floor(Math.random() * anomalousBases.length)];
      const updated =
        originalText.substring(0, charIndex) +
        randomBase +
        originalText.substring(charIndex + 1);

      setDisplayText(updated);

      // Restore original text after 80ms - 200ms
      timeoutId = setTimeout(() => {
        setDisplayText(originalText);
        // Schedule next flicker after 1500ms - 4000ms
        const nextDelay = 1500 + Math.random() * 2500;
        timeoutId = setTimeout(doFlicker, nextDelay);
      }, 80 + Math.random() * 120);
    };

    // Start flicker scheduling
    const initialDelay = 1500 + Math.random() * 2000;
    timeoutId = setTimeout(doFlicker, initialDelay);

    return () => {
      clearTimeout(timeoutId);
    };
  }, []);

  return (
    <h2 className="text-xs font-bold text-white tracking-wider uppercase text-left font-mono">
      {displayText}
    </h2>
  );
};

/**
 * Shortened telomeres act as the specific mechanism that degrades stats.
 * Computes effective stats scaled by current remaining telomeres.
 */
export function getEffectiveStats(c: Creature) {
  const telomeres = c.telomeres !== undefined ? c.telomeres : 100;
  // linearly degrades from 100% down to 25% health/power at 0% telomeres
  const factor = 0.25 + 0.75 * (telomeres / 100);
  return {
    vitality: Math.max(10, Math.round(c.vitality * factor)),
    attack: Math.max(5, Math.round(c.attack * factor)),
    defense: Math.max(5, Math.round(c.defense * factor)),
    speed: Math.max(5, Math.round(c.speed * factor))
  };
}

export default function PoxConsole({ 
  isMobileView = false, 
  viewportProfile = "desktop",
  detectedDeviceName = "Generic Terminal",
  detectedPosture = "Standard Landscape Desktop",
  detectedRatio = 1.33
}: { 
  isMobileView?: boolean; 
  viewportProfile?: string;
  detectedDeviceName?: string;
  detectedPosture?: string;
  detectedRatio?: number;
}) {
  const MAPS_API_KEY =
    process.env.GOOGLE_MAPS_PLATFORM_KEY ||
    (import.meta as any).env?.VITE_GOOGLE_MAPS_PLATFORM_KEY ||
    (globalThis as any).GOOGLE_MAPS_PLATFORM_KEY ||
    '';
  const hasValidMapsKey = Boolean(MAPS_API_KEY) && MAPS_API_KEY !== 'YOUR_API_KEY' && MAPS_API_KEY.trim() !== '';

  // Device power & general settings
  const todayWave = getDailyWaveConfig(new Date());
  const tomorrowWave = getDailyWaveConfig(new Date(Date.now() + 24 * 60 * 60 * 1000));
  const dayAfterWave = getDailyWaveConfig(new Date(Date.now() + 2 * 24 * 60 * 60 * 1000));
  const [isPowered, setIsPowered] = useState<boolean>(true);
  const [isMuted, setIsMuted] = useState<boolean>(false);
  const [isCrystalDetailsOpen, setIsCrystalDetailsOpen] = useState<boolean>(false);
  const [isStatsDetailsOpen, setIsStatsDetailsOpen] = useState<boolean>(false);

  const [nodeStability, setNodeStability] = useState<number>(() => {
    const savedDate = localStorage.getItem("pox_stability_date");
    const today = new Date().toDateString();
    if (savedDate === today) {
      const savedStability = localStorage.getItem("pox_node_stability");
      return savedStability ? parseFloat(savedStability) : 0;
    }
    return 0;
  });

  const [nodeEmissivity, setNodeEmissivity] = useState<number>(() => {
    const savedDate = localStorage.getItem("pox_emissivity_date");
    const today = new Date().toDateString();
    if (savedDate === today) {
      const savedEmissivity = localStorage.getItem("pox_node_emissivity");
      return savedEmissivity ? parseFloat(savedEmissivity) : 0;
    }
    return 0;
  });

  useEffect(() => {
    const today = new Date().toDateString();
    localStorage.setItem("pox_node_stability", nodeStability.toString());
    localStorage.setItem("pox_stability_date", today);
  }, [nodeStability]);

  useEffect(() => {
    const today = new Date().toDateString();
    localStorage.setItem("pox_node_emissivity", nodeEmissivity.toString());
    localStorage.setItem("pox_emissivity_date", today);
  }, [nodeEmissivity]);
  
  // Selection/Focus Anchor for the panels
  const [activeTab, setActiveTab] = useState<'combinator' | 'splicer' | 'library' | 'transceiver' | 'stats' | 'gen_network' | 'area_scanner' | 'forecast' | 'inventory' | 'nodes'>('combinator');
  const [bioLabSubTab, setBioLabSubTab] = useState<'pox' | 'anomaly'>('pox');
  const [syncNetTab, setSyncNetTab] = useState<'sweep' | 'scanner'>('sweep');

  // DNA Combinator (Idle Generator) states
  const [sequences, setSequences] = useState<GeneSequence[]>(() => {
    const saved = localStorage.getItem("pox_gene_sequences");
    if (saved) {
      try { return JSON.parse(saved); } catch (e) {}
    }
    return [
      { sequence: "AGTCGTAC", count: 2, discoveredAt: Date.now() },
      { sequence: "CCCGGGAA", count: 1, discoveredAt: Date.now() },
      { sequence: "TTTAAACG", count: 1, discoveredAt: Date.now() },
      { sequence: "AACCGGTT", count: 1, discoveredAt: Date.now() }
    ];
  });

  // Node Crystal Unit calculations
  const inventoryStrings = React.useMemo(() => {
    const list: string[] = [];
    sequences.forEach(item => {
      // Unique-only calculation: treat count as 1 if it is > 0
      const cnt = (item.count && item.count > 0) ? 1 : 0;
      for (let i = 0; i < cnt; i++) {
        if (item.sequence && item.sequence.length === 8) {
          list.push(item.sequence.toUpperCase());
        }
      }
    });
    return list;
  }, [sequences]);

  const baseTallies = React.useMemo(() => {
    let g = 0;
    let a = 0;
    let t = 0;
    let c = 0;
    inventoryStrings.forEach(s => {
      for (let i = 0; i < s.length; i++) {
        const char = s[i];
        if (char === 'G') g++;
        else if (char === 'A') a++;
        else if (char === 'T') t++;
        else if (char === 'C') c++;
      }
    });
    const grandTotal = g + a + t + c;
    return { g, a, t, c, grandTotal };
  }, [inventoryStrings]);

  const nodeCrystalMetrics = React.useMemo(() => {
    const { g, a, t, c, grandTotal } = baseTallies;
    const V_MAX_METRIC = 131072;

    // Node Depth (Vertical Mountain)
    const subnodeDepthG = Math.max(0, Math.min(1.0, g / V_MAX_METRIC));
    const subnodeDepthA = Math.max(0, Math.min(1.0, a / V_MAX_METRIC));
    const subnodeDepthT = Math.max(0, Math.min(1.0, t / V_MAX_METRIC));
    const subnodeDepthC = Math.max(0, Math.min(1.0, c / V_MAX_METRIC));
    const totalNodeDepth = subnodeDepthG + subnodeDepthA + subnodeDepthT + subnodeDepthC;

    // Node Width (Horizontal Balance)
    const targetWidth = grandTotal > 0 ? (grandTotal / 4) : 1;
    const subnodeWidthG = Math.max(0, Math.min(1.0, g / targetWidth));
    const subnodeWidthA = Math.max(0, Math.min(1.0, a / targetWidth));
    const subnodeWidthT = Math.max(0, Math.min(1.0, t / targetWidth));
    const subnodeWidthC = Math.max(0, Math.min(1.0, c / targetWidth));
    const totalNodeWidth = subnodeWidthG + subnodeWidthA + subnodeWidthT + subnodeWidthC;

    // Weighted RGB average from assigned bases:
    // G = Red (239, 68, 68)
    // T = Blue (59, 130, 246)
    // C = Yellow (234, 179, 8)
    // A = Green (34, 197, 94)
    let r = 0, grn = 255, b = 65; // fallback to brand green
    if (totalNodeDepth > 0) {
      r = (239 * subnodeDepthG + 59 * subnodeDepthT + 234 * subnodeDepthC + 34 * subnodeDepthA) / totalNodeDepth;
      grn = (68 * subnodeDepthG + 130 * subnodeDepthT + 179 * subnodeDepthC + 197 * subnodeDepthA) / totalNodeDepth;
      b = (68 * subnodeDepthG + 246 * subnodeDepthT + 8 * subnodeDepthC + 94 * subnodeDepthA) / totalNodeDepth;
    }

    return {
      totalG: g,
      totalA: a,
      totalT: t,
      totalC: c,
      grandTotal,
      subnodeDepthG,
      subnodeDepthA,
      subnodeDepthT,
      subnodeDepthC,
      totalNodeDepth,
      targetWidth,
      subnodeWidthG,
      subnodeWidthA,
      subnodeWidthT,
      subnodeWidthC,
      totalNodeWidth,
      color: {
        r: Math.round(r),
        g: Math.round(grn),
        b: Math.round(b)
      }
    };
  }, [baseTallies]);
  
  const [idleTime, setIdleTime] = useState<number>(16); 
  const idleTimeRef = useRef<number>(16);
  const tickIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const tickerIntervalRef = useRef<NodeJS.Timeout | null>(null);
  useEffect(() => {
    idleTimeRef.current = idleTime;
  }, [idleTime]);
  const [scrollingGene, setScrollingGene] = useState<string>("--------");
  const [newestSequence, setNewestSequence] = useState<string>("");
  const [isFreshSequence, setIsFreshSequence] = useState<boolean>(false);
  const [recentSplicedGenes, setRecentSplicedGenes] = useState<{ sequence: string; isNew: boolean }[]>([]);
  
  const [discoveredPacketsLog, setDiscoveredPacketsLog] = useState<{
    id: string;
    timestamp: number;
    genes: { sequence: string; isNew: boolean }[];
  }[]>(() => {
    return [
      {
        id: "init-packet-1",
        timestamp: Date.now() - 60000,
        genes: [
          { sequence: "AGTCGTAC", isNew: false },
          { sequence: "CCCGGGAA", isNew: false },
          { sequence: "TTTAAACG", isNew: false },
          { sequence: "AACCGGTT", isNew: false },
          { sequence: "TCGATCGT", isNew: false },
          { sequence: "GCATGCAT", isNew: false },
          { sequence: "AATTTCCG", isNew: false },
          { sequence: "CGGCGGCC", isNew: false }
        ]
      }
    ];
  });
  const [isGeneLogPopupOpen, setIsGeneLogPopupOpen] = useState<boolean>(false);
  const [viewportGeneBalancePopupMailId, setViewportGeneBalancePopupMailId] = useState<string | null>(null);

  // Gene Discovery Log Enhanced State
  const [discoveryLogTab, setDiscoveryLogTab] = useState<"register" | "search">("register");
  const [discoverySearchStep, setDiscoverySearchStep] = useState<number>(0);
  const [discoverySearchPrefix, setDiscoverySearchPrefix] = useState<string>("");
  const [discoverySearchText, setDiscoverySearchText] = useState<string>("");
  const [discoverySelectedGene, setDiscoverySelectedGene] = useState<string | null>(null);

  useEffect(() => {
    if (!isGeneLogPopupOpen) {
      setDiscoveryLogTab("register");
      setDiscoverySearchStep(0);
      setDiscoverySearchPrefix("");
      setDiscoverySearchText("");
      setDiscoverySelectedGene(null);
    }
  }, [isGeneLogPopupOpen]);

  const [isAnomalousLedgerExpanded, setIsAnomalousLedgerExpanded] = useState<boolean>(false);
  const [activeLedgerMailId, setActiveLedgerMailId] = useState<string | null>(null);

  // Anomalous Genes Helper Functions:
  const isAnomalousGene = (seq: string): boolean => {
    return seq.length === 8 && [...seq].some(char => "XZYW?!$%&@#".includes(char));
  };

  const generateAnomalousGene = (): string => {
    const chars = "XZYW?!$%&@#";
    let result = "";
    for (let i = 0; i < 8; i++) {
      result += chars[Math.floor(Math.random() * chars.length)];
    }
    return result;
  };

  const getBenefitForAnomalousGene = (gene: string) => {
    const char = gene[0] || 'X';
    if (char === 'X' || char === 'Z') {
      return {
        id: "DOUBLE_STRIKE",
        name: "Vortex Phase-Strike",
        description: "Attacks twice in the same turn.",
        effectType: "double_attack"
      };
    } else if (char === 'Y' || char === 'W') {
      return {
        id: "SELF_DESTRUCT",
        name: "Supernova Reverb",
        description: "Upon fall, self-destructs and vaporizes the opponent creature.",
        effectType: "self_destruct"
      };
    } else if (char === '?' || char === '!') {
      return {
        id: "HARVEST_BOOST",
        name: "Quantum Extraction Unit",
        description: "Dramatically boosts gene harvesting rates (+1 extra gene on win).",
        effectType: "harvest_boost"
      };
    } else if (char === '$' || char === '%') {
      return {
        id: "HEALTH_REGEN",
        name: "Bio-Organic Siphon",
        description: "Heals 15 HP on each attack.",
        effectType: "health_regen"
      };
    } else {
      return {
        id: "PHASE_SHIFT",
        name: "Temporal Mirror Shield",
        description: "30% evasion chance to phase shift past incoming attacks.",
        effectType: "phase_shift"
      };
    }
  };

  const getAnomalousBenefits = (sequence: string) => {
    const benefits: { id: string; name: string; description: string; effectType: string }[] = [];
    for (let i = 0; i < sequence.length; i += 8) {
      const chunk = sequence.slice(i, i + 8);
      if (isAnomalousGene(chunk)) {
        const b = getBenefitForAnomalousGene(chunk);
        if (!benefits.some(existing => existing.id === b.id)) {
          benefits.push(b);
        }
      }
    }
    return benefits;
  };

  const trimInnerBattleLogs = (logLines: string[]): string[] => {
    const battleIndices: number[] = [];
    logLines.forEach((line, idx) => {
      if (
        line.includes("--- BATTLE REPORT AT") || 
        line.includes("--- NODE STABILITY REPORT AT") || 
        line.includes("--- DEFENSE LOG AT") || 
        line.includes("--- INCOMING NODE STABILITY BREACH")
      ) {
        battleIndices.push(idx);
      }
    });

    if (battleIndices.length <= 6) return logLines;

    // We keep only the last 6 battle entries
    const keepIndex = battleIndices[battleIndices.length - 6];
    // Keep the introduction/coms header (if any) before the first battle report starts
    let firstReportIdx = battleIndices[0];
    if (firstReportIdx < 0) firstReportIdx = 0;
    const introHeader = logLines.slice(0, firstReportIdx);
    const trimmedPart = logLines.slice(keepIndex);

    return [...introHeader, ...trimmedPart];
  };

  // Splicer (lab Target sequence builder) states
  const [splicerSlots, setSplicerSlots] = useState<(string | null)[]>([
    null, null, null, null, null, null, null, null
  ]); 
  const [isSplicing, setIsSplicing] = useState<boolean>(false);
  const [splicingProgress, setSplicingProgress] = useState<number>(0);
  const [activeSlotSelection, setActiveSlotSelection] = useState<number | null>(null);

  // G.E.N. Network target constructor states
  const [selectedDispatchSequence, setSelectedDispatchSequence] = useState<string | null>(null);
  const [netSplicerSlots, setNetSplicerSlots] = useState<(string | null)[]>([
    null, null, null, null, null, null, null, null
  ]);
  const [isNetSplicing, setIsNetSplicing] = useState<boolean>(false);
  const [netSplicingProgress, setNetSplicingProgress] = useState<number>(0);
  const [activeNetSlotSelection, setActiveNetSlotSelection] = useState<number | null>(null);

  // Creatures library states
  const [creatures, setCreatures] = useState<Creature[]>(() => {
    const saved = localStorage.getItem("pox_creatures");
    if (saved) {
      try { return JSON.parse(saved); } catch (e) {}
    }
    return [
      constructProceduralCreature("AGTCGTACCCCGGMAATTTAAACGAGTCGTACCCCGGMAATTTAAACGTTTAAACGAGTCGTAC"),
    ];
  });
  const [inspectedCreatureId, setInspectedCreatureId] = useState<string | null>(null);
  const [creatureCardOpenedFrom, setCreatureCardOpenedFrom] = useState<'Constructor' | 'Gen-Vault Data' | 'Trade' | 'Scanner'>('Gen-Vault Data');
  const [currentTime, setCurrentTime] = useState<Date>(() => new Date());
  const [timeOffset, setTimeOffset] = useState<number>(0);
  const [isTimeVerified, setIsTimeVerified] = useState<boolean>(false);
  const [timeSource, setTimeSource] = useState<string>("LOCAL_CLOCK");
  const [timeSkipDetected, setTimeSkipDetected] = useState<boolean>(false);
  
  // Decrease telomere length of a creature and handle possible disintegration / block return
  const decreaseTelomeres = (creatureId: string, amount: number, reason: string) => {
    setCreatures(cPrev => {
      const match = cPrev.find(c => c.id === creatureId);
      if (!match) return cPrev;

      const currentT = match.telomeres !== undefined ? match.telomeres : 100;
      const nextT = Math.max(0, currentT - amount);

      if (nextT <= 0) {
        // Creature is completely destroyed!
        // Gather all gene blocks from sequence (64 characters -> 8 blocks of 8 characters)
        const genome = match.sequence;
        const blocks: string[] = [];
        for (let i = 0; i < 8; i++) {
          const block = genome.slice(i * 8, (i + 1) * 8);
          if (block.length === 8) {
            blocks.push(block);
          }
        }
        // Appended genes
        if (match.appendedGenes) {
          match.appendedGenes.forEach(gene => {
            if (gene.length === 8) {
              blocks.push(gene);
            }
          });
        }

        // Return a portion (50%, rounded up) and permanently destroy the rest:
        const returnCount = Math.ceil(blocks.length / 2);
        const shuffled = [...blocks].sort(() => 0.5 - Math.random());
        const returnedBlocks = shuffled.slice(0, returnCount);
        const destroyedBlocks = shuffled.slice(returnCount);

        // Populate disintegration modal state
        setDisintegratedModal({
          name: match.name,
          returnedBlocks,
          destroyedBlocks
        });

        // Add returned blocks to player's sequences pool
        setSequences(sPrev => {
          const updated = [...sPrev];
          returnedBlocks.forEach(seq => {
            const idx = updated.findIndex(s => s.sequence === seq);
            if (idx >= 0) {
              updated[idx] = { ...updated[idx], count: updated[idx].count + 1 };
            } else {
              updated.push({ sequence: seq, count: 1, discoveredAt: Date.now() });
            }
          });
          return updated;
        });

        // Trigger log message
        triggerLog(`🚨 CHROMOSOMAL FAILURE: "${match.name}" disintegrated! Telomeres reached 0%. Returned ${returnedBlocks.length} gene blocks, lost ${destroyedBlocks.length} permanently!`, "warn");
        sound.playReject();

        if (defenderCreatureId === creatureId) {
          setDefenderCreatureId(null);
        }

        return cPrev.filter(c => c.id !== creatureId);
      } else {
        // Just deduct the telomeres
        triggerLog(`🧬 CELLULAR INTEGRITY: "${match.name}" telomeres shortened by -${amount}% due to ${reason}. [Current life: ${nextT}%]`, "warn");
        return cPrev.map(c => {
          if (c.id === creatureId) {
            return { ...c, telomeres: nextT };
          }
          return c;
        });
      }
    });
  };

  const [anomalyEngineActive, setAnomalyEngineActive] = useState<boolean>(false);
  const anomalyEngineActiveRef = useRef<boolean>(false);
  const baseTalliesRef = useRef<{ g: number; a: number; t: number; c: number; grandTotal: number }>({ g: 0, a: 0, t: 0, c: 0, grandTotal: 0 });

  const [devForceAnomaly, setDevForceAnomaly] = useState<boolean>(false);
  const devForceAnomalyRef = useRef<boolean>(false);

  useEffect(() => {
    anomalyEngineActiveRef.current = anomalyEngineActive;
  }, [anomalyEngineActive]);

  useEffect(() => {
    devForceAnomalyRef.current = devForceAnomaly;
  }, [devForceAnomaly]);

  useEffect(() => {
    baseTalliesRef.current = baseTallies;
  }, [baseTallies]);

  // Spectrum Wave Coupling calculation helper
  const getSpectrumWaveCoupling = (): number => {
    const now = currentTime || new Date();
    const hours = now.getHours();
    const minutes = now.getMinutes();
    const seconds = now.getSeconds();
    const daySeconds = hours * 3600 + minutes * 60 + seconds;
    const dayFraction = daySeconds / 86400;
    const baseValue = 80;
    const sinusoidalFluctuation = 12.375 * Math.sin(dayFraction * 2 * Math.PI * 4);
    const hackBonus = ((stats.totalHacksWon || 0) + (stats.interceptorHacksWon || 0)) * 0.0125;
    return Math.max(0, Math.min(100, baseValue + sinusoidalFluctuation + hackBonus));
  };

  // Logarithmic success chance with harmonic peaks every 14% and spectrum coupling modifier
  const getAnomalyEngineSuccessChance = (grandTotal: number): {
    baseChance: number;
    peakBoost: number;
    harmonicModifier: number;
    finalChance: number;
  } => {
    if (grandTotal <= 10000) {
      return { baseChance: 1, peakBoost: 0, harmonicModifier: 0, finalChance: 1 };
    }
    if (grandTotal >= 250000) {
      return { baseChance: 100, peakBoost: 0, harmonicModifier: 0, finalChance: 100 };
    }
    
    // Logarithmic scaling between 10k and 250k
    const minLog = Math.log(10000);
    const maxLog = Math.log(250000);
    const currentLog = Math.log(grandTotal);
    const t = (currentLog - minLog) / (maxLog - minLog); // 0.0 to 1.0
    
    const baseChance = 1.0 + 99.0 * t; // 1% to 100%
    
    // Notable peaks every 14% (14, 28, 42, 56, 70, 84, 98) using Gaussian distribution
    const multiplesOf14 = [14, 28, 42, 56, 70, 84, 98];
    let peakBoost = 0;
    multiplesOf14.forEach(peak => {
      const dist = Math.abs(baseChance - peak);
      if (dist < 5.0) {
        const boostVal = 6.5 * Math.exp(-Math.pow(dist / 1.8, 2));
        peakBoost = Math.max(peakBoost, boostVal);
      }
    });

    // Spectrum wave coupler harmonic adjustment
    const coupling = getSpectrumWaveCoupling();
    const harmonicModifier = (coupling - 80.0) * 0.25; 

    const finalChance = Math.min(100, Math.max(1, baseChance + peakBoost + harmonicModifier));

    return {
      baseChance,
      peakBoost,
      harmonicModifier,
      finalChance
    };
  };
  
  const timeOffsetRef = useRef<number>(0);
  const lastLocalTimeRef = useRef<number>(Date.now());
  const consecutiveSkipsRef = useRef<number>(0);

  // Sync with secure NTP GPS Peer beacon
  const syncWithSecureBeacon = async () => {
    try {
      const res = await fetch("/api/time");
      if (res.ok) {
        const data = await res.json();
        if (data.success) {
          const serverMs = new Date(data.verifiedTime).getTime();
          const localMs = Date.now();
          const offset = serverMs - localMs;
          
          setTimeOffset(offset);
          timeOffsetRef.current = offset;
          setIsTimeVerified(true);
          setTimeSource(data.source === "worldtimeapi_dual_beacon" ? "GPS_DUAL_BEACON" : "NTP_SECURE_BEACON");
          
          // Clear time-skip alarm with server authority override
          setTimeSkipDetected(false);
          consecutiveSkipsRef.current = 0;
          
          triggerLog(`[CHRONO] Secure clock sync successful. Offset: ${offset}ms via ${data.source.toUpperCase()}`, "success");
        } else {
          throw new Error("API reported failure");
        }
      } else {
        throw new Error(`HTTP status ${res.status}`);
      }
    } catch (e) {
      console.warn("Time beacon sync status check/fallback active:", e instanceof Error ? e.message : e);
      // Fail gracefully: fallback to local system time but mark as unverified
      setIsTimeVerified(false);
      setTimeSource("LOCAL_UNSECURED");
    }
  };

  useEffect(() => {
    // Initial sync
    syncWithSecureBeacon();
    
    // Periodically re-sync every 30 seconds to enforce clock alignment
    const syncTimer = setInterval(syncWithSecureBeacon, 30000);
    return () => clearInterval(syncTimer);
  }, []);

  // Real-time ticking effect with active anti-cheat time-skip checks
  useEffect(() => {
    // Initialize base time checker
    lastLocalTimeRef.current = Date.now();
    
    const clockTimer = setInterval(() => {
      const nowLocal = Date.now();
      const expectedTime = lastLocalTimeRef.current + 1000;
      const difference = nowLocal - expectedTime;

      // Anti-Time-Skip check: If local clock jumped backward by > 2s or forward by > 5s
      if (difference < -2000 || difference > 5000) {
        setTimeSkipDetected(true);
        consecutiveSkipsRef.current += 1;
        
        sound.playBeep(260, 0.4, "sawtooth");
        triggerLog(`[CHRONO ERROR] TIME WARP TRIGGERED! Host clock altered by ${(difference / 1000).toFixed(1)}s. Correcting telemetry...`, "warn");
        addScannerLog(`[WARNING] CHRONODYNAMIC EXCURSION DETECTED! Lock buffer enabled.`);
        
        // Correct the offset on-the-fly to pull actual date back to real time
        // Increase the offset so local shifts do not manipulate active game events
        const newOffset = timeOffsetRef.current - difference;
        setTimeOffset(newOffset);
        timeOffsetRef.current = newOffset;
      }

      // Track last system time
      lastLocalTimeRef.current = nowLocal;

      // Calculate state current Date with secure calibrated offset
      const adjustedTime = new Date(nowLocal + timeOffsetRef.current);
      setCurrentTime(adjustedTime);
    }, 1000);

    return () => clearInterval(clockTimer);
  }, [timeOffset]);

  // Archive sorting and filtering state variables
  const [applyLibFilters, setApplyLibFilters] = useState<boolean>(false);
  const [libSortBy, setLibSortBy] = useState<string>("name-asc");
  const [libFilterFaction, setLibFilterFaction] = useState<string>("ALL");
  const [libFilterType, setLibFilterType] = useState<string>("ALL");
  const [libFilterTag, setLibFilterTag] = useState<string>("ALL");
  const [libFilterDispatchGeneOnly, setLibFilterDispatchGeneOnly] = useState<boolean>(false);
  const [libMinVitality, setLibMinVitality] = useState<number>(0);
  const [libMaxVitality, setLibMaxVitality] = useState<number>(250);
  const [libMinAttack, setLibMinAttack] = useState<number>(0);
  const [libMaxAttack, setLibMaxAttack] = useState<number>(250);
  const [libMinDefense, setLibMinDefense] = useState<number>(0);
  const [libMaxDefense, setLibMaxDefense] = useState<number>(250);
  const [libMinSpeed, setLibMinSpeed] = useState<number>(0);
  const [libMaxSpeed, setLibMaxSpeed] = useState<number>(250);
  const [isFilterPanelExpanded, setIsFilterPanelExpanded] = useState<boolean>(true);
  const [viewingArchiveSearch, setViewingArchiveSearch] = useState<boolean>(false);

  // Synchronize state values of max filters to match highest stats in creature database
  useEffect(() => {
    if (creatures.length > 0) {
      const maxVit = Math.max(...creatures.map(c => c.vitality), 100);
      const maxAtk = Math.max(...creatures.map(c => c.attack), 100);
      const maxDef = Math.max(...creatures.map(c => c.defense), 100);
      const maxSpd = Math.max(...creatures.map(c => c.speed), 100);
      
      setLibMaxVitality(maxVit);
      setLibMaxAttack(maxAtk);
      setLibMaxDefense(maxDef);
      setLibMaxSpeed(maxSpd);
    }
  }, [creatures]);

  // Bio-Lab Reactor Catalyst Booster state
  const [boostSecondsLeft, setBoostSecondsLeft] = useState<number>(0);
  const boostSecondsLeftRef = useRef<number>(0);
  useEffect(() => {
    boostSecondsLeftRef.current = boostSecondsLeft;
  }, [boostSecondsLeft]);

  // Transceiver / Trading State
  const [nearbyUsers, setNearbyUsers] = useState<NearbyUser[]>([]);
  const [isScanning, setIsScanning] = useState<boolean>(false);
  const [activeTrade, setActiveTrade] = useState<TradeSession | null>(null);

  const [tradeSelectedCreature, setTradeSelectedCreature] = useState<Creature | null>(null);
  const [isTradeArchiveOpened, setIsTradeArchiveOpened] = useState<boolean>(false);
  const [isInboxTradeArchiveOpened, setIsInboxTradeArchiveOpened] = useState<boolean>(false);
  const [tradeArchiveSearch, setTradeArchiveSearch] = useState<string>("");
  const [tradeArchiveFaction, setTradeArchiveFaction] = useState<string>("ALL");
  const [tradeArchiveSort, setTradeArchiveSort] = useState<string>("name-asc");

  const [selectedGeneIndex, setSelectedGeneIndex] = useState<number | null>(null);
  const [showTradeDiagnostics, setShowTradeDiagnostics] = useState<boolean>(false);
  const [isGeneLedgerExpanded, setIsGeneLedgerExpanded] = useState<boolean>(false);

  // Hierarchical Base-Pair UI Step-Searching
  const [searchStep, setSearchStep] = useState<number>(0);
  const [currentSearchPrefix, setCurrentSearchPrefix] = useState<string>("");
  const [showIntermediateMatches, setShowIntermediateMatches] = useState<boolean>(false);
  const [slotSequenceFilter, setSlotSequenceFilter] = useState<string>("");

  useEffect(() => {
    setSlotSequenceFilter("");
  }, [activeSlotSelection]);

  useEffect(() => {
    if (!isGeneLedgerExpanded) {
      setSearchStep(0);
      setCurrentSearchPrefix("");
      setShowIntermediateMatches(false);
    }
  }, [isGeneLedgerExpanded]);

  // Target sequence for standard re-sequencing constructor
  const [targetSequence, setTargetSequence] = useState<string>(() => {
    const saved = localStorage.getItem("pox_target_sequence");
    if (saved && saved.length === 64) return saved;
    return generateRandom64Sequence();
  });

  // Forced construction & Reactor freeze states
  const [isReactorFrozen, setIsReactorFrozen] = useState<boolean>(false);
  const [reactorFreezeTimeLeft, setReactorFreezeTimeLeft] = useState<number>(0);
  const [isForcedConstructionActive, setIsForcedConstructionActive] = useState<boolean>(false);
  const [isForcedLoopActive, setIsForcedLoopActive] = useState<boolean>(false);
  const isForcedLoopActiveRef = useRef<boolean>(false);
  const [forcedConstructionLogs, setForcedConstructionLogs] = useState<string[]>([]);
  const isReactorFrozenRef = useRef<boolean>(false);

  const [isSyncNetOn, setIsSyncNetOn] = useState<boolean>(true);
  const [batteryStatus, setBatteryStatus] = useState<{ level: number | null; charging: boolean | null; error: boolean }>({
    level: null,
    charging: null,
    error: false
  });
  
  const forcedLogContainerRef = useRef<HTMLDivElement>(null);
  const combatLogRef = useRef<HTMLDivElement | null>(null);
  const scannerLogRef = useRef<HTMLDivElement | null>(null);


  useEffect(() => {
    isReactorFrozenRef.current = isReactorFrozen;
  }, [isReactorFrozen]);

  useEffect(() => {
    isForcedLoopActiveRef.current = isForcedLoopActive;
  }, [isForcedLoopActive]);

  // Battery detection logic
  useEffect(() => {
    let batteryObj: any = null;
    const handleLevelChange = () => {
      if (batteryObj) {
        setBatteryStatus({
          level: batteryObj.level,
          charging: batteryObj.charging,
          error: false
        });
      }
    };

    if (navigator && (navigator as any).getBattery) {
      (navigator as any).getBattery().then((battery: any) => {
        batteryObj = battery;
        setBatteryStatus({
          level: battery.level,
          charging: battery.charging,
          error: false
        });
        battery.addEventListener('levelchange', handleLevelChange);
        battery.addEventListener('chargingchange', handleLevelChange);
      }).catch(() => {
        setBatteryStatus(prev => ({ ...prev, error: true }));
      });
    } else {
      setBatteryStatus(prev => ({ ...prev, error: true }));
    }

    return () => {
      if (batteryObj) {
        try {
          batteryObj.removeEventListener('levelchange', handleLevelChange);
          batteryObj.removeEventListener('chargingchange', handleLevelChange);
        } catch (e) {}
      }
    };
  }, []);

  // Auto-scroll forced construction logs
  useEffect(() => {
    const timer = setTimeout(() => {
      if (forcedLogContainerRef.current) {
        forcedLogContainerRef.current.scrollTop = forcedLogContainerRef.current.scrollHeight;
      }
    }, 60);
    return () => clearTimeout(timer);
  }, [forcedConstructionLogs]);

  // Area Scanner States - Linked to GNPX badge toggling
  const scanRadius = React.useMemo(() => {
    return devForceAnomaly ? 120 : 60; // 120ft radius = 240ft diameter if GNPX toggled on (Max range), else 60ft radius = 120ft diameter standard
  }, [devForceAnomaly]);
  const [userCoords, setUserCoords] = useState<{lat: number; lng: number}>({ lat: 37.4221, lng: -122.0841 });
  const [geoError, setGeoError] = useState<string | null>(null);
  const [isMobileDevice, setIsMobileDevice] = useState<boolean>(false);
  const [lastMapsApiCallDay, setLastMapsApiCallDay] = useState<string>(() => {
    return localStorage.getItem("pox_last_maps_api_call_day") || "";
  });
  const [mapsCountdown, setMapsCountdown] = useState<string>("00:00:00");
  const [useHolographicScanner, setUseHolographicScanner] = useState<boolean>(true);
  const [mapsAuthError, setMapsAuthError] = useState<boolean>(false);
  const [selectedAnomalyId, setSelectedAnomalyId] = useState<string | null>(null);
  const [bioAnomalies, setBioAnomalies] = useState<any[]>([]);

  // NEW HARVESTING STATES & HANDLERS
  const [harvestingMissions, setHarvestingMissions] = useState<any[]>(() => {
    const saved = localStorage.getItem("pox_harvesting_missions");
    try {
      return saved ? JSON.parse(saved) : [];
    } catch (e) {
      return [];
    }
  });
  const [customTapCoords, setCustomTapCoords] = useState<{ lat: number; lng: number; distance: number } | null>(null);
  const [isDevSpeedActive, setIsDevSpeedActive] = useState<boolean>(false);
  const [isMapPopupOpen, setIsMapPopupOpen] = useState<boolean>(false);
  const [showingActiveHarvestsDetail, setShowingActiveHarvestsDetail] = useState<boolean>(false);
  const [showingTelemetryLogsDetail, setShowingTelemetryLogsDetail] = useState<boolean>(false);
  const [enlargedQrCreatureId, setEnlargedQrCreatureId] = useState<string | null>(null);
  const [isTradeQrEnlarged, setIsTradeQrEnlarged] = useState<boolean>(false);
  const [receivedTradeCreature, setReceivedTradeCreature] = useState<Creature | null>(null);
  const [tradeSelectedCreatureId, setTradeSelectedCreatureId] = useState<string | null>(null);
  const [isTradeQrCameraActive, setIsTradeQrCameraActive] = useState<boolean>(false);
  const [manualTradeUrlInput, setManualTradeUrlInput] = useState<string>("");
  const [nodesSubTab, setNodesSubTab] = useState<'friends' | 'qr_trade'>('qr_trade');
  const [disintegratedModal, setDisintegratedModal] = useState<{
    name: string;
    returnedBlocks: string[];
    destroyedBlocks: string[];
  } | null>(null);
  const [isOperatorQrEnlarged, setIsOperatorQrEnlarged] = useState<boolean>(false);
  const [genNetworkSubTab, setGenNetworkSubTab] = useState<'mail' | 'nodes'>('mail');
  
  const [newNodeName, setNewNodeName] = useState<string>("");
  const [newNodeId, setNewNodeId] = useState<string>("");
  const [isAddNodeOpen, setIsAddNodeOpen] = useState<boolean>(false);
  const [registeredNodes, setRegisteredNodes] = useState<any[]>(() => {
    try {
      const saved = localStorage.getItem('pox_registered_nodes');
      if (saved) {
        return JSON.parse(saved);
      }
    } catch (e) {
      console.error(e);
    }
    return [
      {
        id: "NODE-781A-BD9F-A29B",
        name: "ALPHA_NODE_01",
        assignedIp: "10.0.4.15",
        status: "ONLINE",
        emissivity: 92,
        addedAt: new Date(Date.now() - 3600000 * 24).toLocaleString()
      },
      {
        id: "NODE-314C-E890-F210",
        name: "SECTOR_DELTA_NODE",
        assignedIp: "10.0.9.43",
        status: "STANDBY",
        emissivity: 78,
        addedAt: new Date(Date.now() - 3600000 * 5).toLocaleString()
      }
    ];
  });

  React.useEffect(() => {
    try {
      localStorage.setItem('pox_registered_nodes', JSON.stringify(registeredNodes));
    } catch (e) {
      console.error(e);
    }
  }, [registeredNodes]);
  const [scannerLogs, setScannerLogs] = useState<string[]>(() => [
    `[${new Date().toLocaleTimeString()}] P2P TELEMETRY LINK ESTABLISHED`,
    `[${new Date().toLocaleTimeString()}] LOCKING WAVE RESONATOR OSCILLATOR`
  ]);

  const addScannerLog = (text: string) => {
    const time = new Date().toLocaleTimeString();
    setScannerLogs(prev => [...prev.slice(-30), `[${time}] ${text}`]);
  };

  const handleProcessTradePayload = (payload: string): boolean => {
    let base64String = payload.trim();
    
    if (base64String.includes("?trade=")) {
      try {
        const urlObj = new URL(base64String);
        base64String = urlObj.searchParams.get("trade") || "";
      } catch (e) {
        const parts = base64String.split("?trade=");
        if (parts.length > 1) {
          base64String = parts[1].split("&")[0];
        }
      }
    }

    if (!base64String) {
      triggerLog("INVALID TRADE CONTAINER: Empty transponder data.", "warn");
      return false;
    }

    const decoded = decodeCreatureFromBase64(base64String);
    if (decoded) {
      sound.playSynthesisSuccess();
      setCreatures(prev => {
        if (prev.some(c => c.id === decoded.id)) {
          triggerLog(`SPECIMEN "${decoded.name}" ALREADY DETECTED IN ARCHIVE.`, "info");
          setReceivedTradeCreature(decoded);
          return prev;
        }
        const nextT = Math.max(10, (decoded.telomeres !== undefined ? decoded.telomeres : 100) - 15);
        const mutatedDecoded = { ...decoded, telomeres: nextT };
        triggerLog(`[TRADE SUCCESS] SYNCHRONIZED SPECIMEN "${decoded.name}" INTO BIOLOGICAL STORAGE!`, "success");
        triggerLog(`🧬 TRANSCEIVER SCAN: Cosmic transmission scan shortened "${decoded.name}" telomeres by -15%. [Current life: ${nextT}%]`, "warn");
        return [mutatedDecoded, ...prev];
      });
      setReceivedTradeCreature(decoded);
      return true;
    } else {
      triggerLog("TRADE DECODE FAILED: Invalid DNA sequence transponder data format.", "warn");
      return false;
    }
  };

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const tradeParam = params.get("trade");
    if (tradeParam) {
      setTimeout(() => {
        const success = handleProcessTradePayload(tradeParam);
        if (success) {
          addScannerLog("LOCKED_TARGET: Serverless trade received and processed.");
          
          const url = new URL(window.location.href);
          url.searchParams.delete("trade");
          window.history.replaceState({}, "", url.pathname + url.search);
        }
      }, 800); // Small timeout to ensure app state has booted
    }
  }, []);

  // Camera scan hardware-level controller hook
  useEffect(() => {
    let qrScanner: Html5Qrcode | null = null;

    if (isTradeQrCameraActive) {
      const delayTimer = setTimeout(() => {
        try {
          qrScanner = new Html5Qrcode("trade-qr-reader");
          qrScanner.start(
            { facingMode: "environment" },
            {
              fps: 10,
              qrbox: (width: number, height: number) => {
                const size = Math.min(width, height, 220);
                return { width: size, height: size };
              }
            },
            (decodedText: string) => {
              addScannerLog("TARGET LOCKED: Decoded transponder barcode sequence.");
              handleProcessTradePayload(decodedText);
              setIsTradeQrCameraActive(false);
            },
            () => {
              // Silent for non-matched frames
            }
          ).catch((err: any) => {
            console.error("Failed to start camera scan:", err);
            triggerLog("CAMERA INITIALIZATION FAILED: Check web frame permissions.", "warn");
            setIsTradeQrCameraActive(false);
          });
        } catch (e) {
          console.error("Camera scanner instantiation error:", e);
          triggerLog("CAMERA SERVICE OFFLINE: Local resources blocked.", "warn");
          setIsTradeQrCameraActive(false);
        }
      }, 500);

      return () => {
        clearTimeout(delayTimer);
        if (qrScanner) {
          // Check if scanner was indeed started before trying to stop
          if (qrScanner.isScanning) {
            qrScanner.stop().catch((e: any) => console.log("Clean stop camera error:", e));
          }
        }
      };
    }
  }, [isTradeQrCameraActive]);

  useEffect(() => {
    localStorage.setItem("pox_harvesting_missions", JSON.stringify(harvestingMissions));
  }, [harvestingMissions]);

  useEffect(() => {
    const activeMissionsCount = harvestingMissions.filter(m => !m.isReturned).length;
    if (activeMissionsCount === 0) {
      setShowingActiveHarvestsDetail(false);
    }
  }, [harvestingMissions]);

  useEffect(() => {
    setCustomTapCoords(null);
    setShowingTelemetryLogsDetail(false);
    setEnlargedQrCreatureId(null);
    setIsTradeQrEnlarged(false);
  }, [selectedAnomalyId, activeTab, syncNetTab]);

  // Real-time tick engine for harvesting
  const handleTickHarvestingMissions = () => {
    setHarvestingMissions(prevMissions => {
      if (prevMissions.length === 0) return prevMissions;
      
      let shouldUpdate = false;
      const updated = prevMissions.map(m => {
        if (m.isReturned) return m;

        const now = Date.now();
        // Determine seconds elapsed. Support dev speed multiplier (+60x speed) if GNPX is toggled on
        const secondsPerTick = (isDevSpeedActive && devForceAnomaly) ? 60 : 1;
        const fakeElapsed = (m.elapsedSeconds || 0) + secondsPerTick;
        const elapsedSeconds = Math.min(m.totalDuration, fakeElapsed);
        
        const quarterDuration = m.totalDuration / 4;
        const currentQuarters = Math.min(4, Math.floor(elapsedSeconds / quarterDuration));
        
        let harvestedGenes = [...(m.harvestedGenes || [])];
        let quartersHarvested = m.quartersHarvested || 0;
        
        if (currentQuarters > quartersHarvested) {
          shouldUpdate = true;
          for (let q = quartersHarvested + 1; q <= currentQuarters; q++) {
            // Pick acquired gene depending on accuracy
            const anom = bioAnomalies.find(a => a.id === m.anomalyId);
            const baseGene = anom ? anom.gene : "AGTCGTAC";
            const heatZoneRadius = (anom?.heatZoneDiameter || 20) / 2;
            
            // Proximity ratio: 0.0 is center, 1.0 is maximum boundary of the circle
            const ratio = Math.min(1, m.dispatchDistance / Math.max(1, heatZoneRadius));
            const perfectYieldChance = 1 - ratio; // 100% chance at center, 0% at boundary
            
            let geneGot = baseGene;
            if (Math.random() > perfectYieldChance) {
              // Mutated gene (different/mutated sequence)
              const bases = ["A", "G", "T", "C"];
              const chars = baseGene.split("");
              // Mutate depending on distance
              const numMutations = ratio > 0.6 ? 2 : 1;
              for (let mut = 0; mut < numMutations; mut++) {
                const idx = Math.floor(Math.random() * 8);
                const oldBase = chars[idx];
                const choices = bases.filter(b => b !== oldBase);
                chars[idx] = choices[Math.floor(Math.random() * choices.length)];
              }
              geneGot = chars.join("");
            }
            harvestedGenes.push(geneGot);
          }
          quartersHarvested = currentQuarters;
        }

        const isCompleted = elapsedSeconds >= m.totalDuration;
        if (m.elapsedSeconds !== elapsedSeconds || quartersHarvested !== m.quartersHarvested || isCompleted !== m.isCompleted) {
          shouldUpdate = true;
          return {
            ...m,
            elapsedSeconds,
            quartersHarvested,
            harvestedGenes,
            isCompleted
          };
        }
        
        return m;
      });
      
      return shouldUpdate ? updated : prevMissions;
    });
  };

  useEffect(() => {
    const interval = setInterval(handleTickHarvestingMissions, 1000);
    return () => clearInterval(interval);
  }, [isDevSpeedActive, bioAnomalies]);

  const handleDispatchCreatureToHarvest = (creatureId: string, anomalyId: string) => {
    if (!isPowered) {
      triggerLog("SYS POWER OFFLINE. CANNOT INITIATE DISPATCH BEACONS.", "warn");
      sound.playReject();
      return;
    }
    const anom = bioAnomalies.find(a => a.id === anomalyId);
    const creature = creatures.find(c => c.id === creatureId);
    if (!anom || !creature) return;

    // Base 8 mins (480s), min 2 mins (120s) depending on speed (e.g., speed of 150 completes in 2 minutes)
    let totalDuration = Math.max(120, Math.round(480 - (creature.speed * 2.4)));
    if (nodeStability >= 100) {
      totalDuration *= 2;
    }
    
    // Set dispatch lat/lng/dist
    const lat = customTapCoords ? customTapCoords.lat : anom.lat;
    const lng = customTapCoords ? customTapCoords.lng : anom.lng;
    const dist = customTapCoords ? customTapCoords.distance : 0.0;

    const newMission = {
      id: `HSP-${Date.now()}-${Math.floor(Math.random() * 1000)}`,
      creatureId: creature.id,
      creatureName: creature.name,
      creatureFaction: creature.faction,
      anomalyId: anom.id,
      anomalyName: anom.name,
      lat,
      lng,
      dispatchDistance: dist,
      startTime: Date.now(),
      totalDuration,
      elapsedSeconds: 0,
      quartersHarvested: 0,
      harvestedGenes: [],
      isCompleted: false,
      isReturned: false
    };

    setHarvestingMissions(prev => [...prev, newMission]);
    sound.playBeep(440, 0.15, "square");
    setTimeout(() => sound.playBeep(880, 0.25, "sine"), 120);
    
    triggerLog(`Dispatched "${creature.name}" on harvest mission. Base duration: ${Math.round(totalDuration / 60)}m. Unit distance: ${dist.toFixed(1)}ft.`, "success");
    addScannerLog(`DISPATCH: "${creature.name}" targeted Anomaly ${anom.id} (Distance: ${dist.toFixed(1)}ft)`);
    setCustomTapCoords(null);
  };

  const handleRetrieveHarvestedGenes = (missionId: string) => {
    const mission = harvestingMissions.find(m => m.id === missionId);
    if (!mission) return;

    // Set as returned so it goes off the map and is freed up
    setHarvestingMissions(prev => prev.map(m => m.id === missionId ? { ...m, isReturned: true } : m));

    // Grant collected genes to sequences currency state!
    const newlyAcquired = mission.harvestedGenes || [];
    if (newlyAcquired.length > 0) {
      setSequences(prev => {
        let updated = [...prev];
        newlyAcquired.forEach(seq => {
          const match = updated.find(s => s.sequence === seq);
          if (match) {
            match.count += 1;
          } else {
            updated.push({
              sequence: seq,
              count: 1,
              discoveredAt: Date.now()
            });
          }
        });
        return updated;
      });

      sound.playSynthesisSuccess();
      triggerLog(`Retrieved "${mission.creatureName}" to terminal. Transferred ${newlyAcquired.length} genes to stockpile: ${newlyAcquired.join(", ")}`, "success");
      addScannerLog(`RECALL: "${mission.creatureName}" returned. Acquired genes: ${newlyAcquired.join(", ")}`);
    } else {
      sound.playBeep(330, 0.2, "sawtooth");
      triggerLog(`Retrieved "${mission.creatureName}" but zero gene sequences collected. Location scan was blank.`, "warn");
      addScannerLog(`RECALL: "${mission.creatureName}" returned empty.`);
    }
  };

  const getAvailableHarvestingCreatures = () => {
    const busyCreatureIds = new Set(
      harvestingMissions.filter(m => !m.isReturned).map(m => m.creatureId)
    );
    return creatures.filter(c => {
      const isBusy = busyCreatureIds.has(c.id);
      const isDefending = defenderCreatureId === c.id;
      const isAutoHacking = c.isAutoHacker;
      return !isBusy && !isDefending && !isAutoHacking;
    });
  };

  const handleMapTapHeatZone = (anomalyId: string, lat: number, lng: number, distanceInFeet: number) => {
    sound.playBeep(520, 0.08, "sine");
    setSelectedAnomalyId(anomalyId);
    setCustomTapCoords({ lat, lng, distance: distanceInFeet });
    triggerLog(`Harvest coordinate locked: ${distanceInFeet.toFixed(1)} ft from node.`, "info");
  };

  // Catch Maps authentication / activation issues globally
  useEffect(() => {
    const originalAuthFailure = (window as any).gm_authFailure;
    (window as any).gm_authFailure = () => {
      console.warn("Google Maps API authentication/activation failure caught.");
      setMapsAuthError(true);
      setUseHolographicScanner(true); // Fail-safe fallback to our pristine HUD
      if (originalAuthFailure) {
        try { originalAuthFailure(); } catch (err) {}
      }
    };
    return () => {
      (window as any).gm_authFailure = originalAuthFailure;
    };
  }, []);

  // Compute and distribute biological anomalies on user latitude/longitude
  useEffect(() => {
    const factions: ("Infection" | "Mech" | "Parasite" | "Containment")[] = [
      "Infection",
      "Mech",
      "Parasite",
      "Containment",
      "Infection"
    ];
    const names = [
      "MUTANT-Toxichloride Alpha",
      "NANO-Phage Beta 7",
      "ACTIVE PEER COMS HOVER",
      "CHITIN-Swarm Larva",
      "HYBRID Genome Signature"
    ];
    const genes = [
      "AGTCGTAC",
      "CCCGGGAA",
      "TTTAAACG",
      "AACCGGTT",
      "GGTTCCAA"
    ];

    // Seed angles to keep position consistent but offsets relative to physical latitude/longitude
    const generated = names.map((name, idx) => {
      const angle = (idx * (2 * Math.PI / 5)) + 0.45;
      const distance = 25 + (idx * 16) + (Math.sin(idx * 7) * 4); // 25 to 90 feet
      const dx = distance * Math.cos(angle);
      const dy = distance * Math.sin(angle);

      // Conversions: 1 deg lat ~ 364,000 ft, 1 deg lng ~ 288,000 ft (at latitude 37)
      const dLat = dy / 364000;
      const dLng = dx / 288000;

      const directions = ["N", "NE", "E", "SE", "S", "SW", "W", "NW"];
      const dirIndex = Math.round(((angle * 180 / Math.PI) % 360) / 45) % 8;

      return {
        id: `ANM-${idx + 104}`,
        name,
        distance,
        direction: directions[dirIndex] || "NE",
        lat: userCoords.lat + dLat,
        lng: userCoords.lng + dLng,
        dx,
        dy,
        gene: genes[idx],
        faction: factions[idx % factions.length],
        signalStrength: Math.floor(98 - distance * 0.7),
        mutantLoad: Math.floor(45 + (idx * 9) + (Math.cos(idx * 3) * 5)),
        heatZoneDiameter: 10 + (idx * 5) % 21 // Generates 10-30ft diameters
      };
    });

    setBioAnomalies(generated);
  }, [userCoords.lat, userCoords.lng]);

  useEffect(() => {
    setIsMobileDevice(isMobileView || /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent));
  }, [isMobileView]);

  const attemptLocationFetch = () => {
    if (!navigator.geolocation) {
      setGeoError("Geolocation is not supported by your device browser.");
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setUserCoords({
          lat: pos.coords.latitude,
          lng: pos.coords.longitude
        });
        setGeoError(null);
      },
      (err) => {
        console.warn("Geolocation warning, using backup coordinates:", err);
        if (err.code === 1) { // PERMISSION_DENIED
          setGeoError("Location Permission Required");
        } else {
          setGeoError("POSITION_UNAVAILABLE");
        }
      },
      { enableHighAccuracy: true, timeout: 5050 }
    );
  };

  const handleHarvestAnomalyGene = (anomalyId: string) => {
    if (!isPowered) {
      triggerLog("SYS POWER OFFLINE. CANNOT ACCESS ELECTRON-COUPLING EXTRACTOR.", "warn");
      sound.playReject();
      return;
    }
    const anomaly = bioAnomalies.find(a => a.id === anomalyId);
    if (!anomaly) return;
    if (anomaly.harvested) {
      triggerLog(`GENE CANNOT BE HARVESTED. Molecular trace is depleted.`, "warn");
      sound.playReject();
      return;
    }
    if (anomaly.distance > scanRadius) {
      triggerLog(`GENE OUT OF SYNC RANGE. Expand player Sync-Net boundary.`, "warn");
      sound.playReject();
      return;
    }

    // Play harvesting sounds
    sound.playBeep(440, 0.2, "square");
    setTimeout(() => sound.playBeep(880, 0.3, "sine"), 150);

    // Mark as harvested
    setBioAnomalies(prev => prev.map(a => a.id === anomalyId ? { ...a, harvested: true } : a));

    // Update sequences
    setSequences(prev => {
      const existing = prev.find(s => s.sequence === anomaly.gene);
      if (existing) {
        return prev.map(s => s.sequence === anomaly.gene ? { ...s, count: s.count + 1 } : s);
      } else {
        return [...prev, { sequence: anomaly.gene, count: 1, discoveredAt: Date.now() }];
      }
    });

    triggerLog(`[SYNC-NET RECOVERY] Extracted gene Block "${anomaly.gene}" into biological inventory.`, "success");
    sound.playTradeSuccess();
    setStats((prev) => ({
      ...prev,
      totalGenesAcquired: prev.totalGenesAcquired + 1
    }));
  };

  const renderBatteryStatus = () => {
    if (batteryStatus.error || batteryStatus.level === null) {
      return <span className="text-yellow-500 font-bold uppercase font-mono">UNKNOWN</span>;
    }
    if (batteryStatus.charging) {
      return <span className="text-white font-bold font-mono">MAX (WIRED)</span>;
    }
    return <span className="text-white font-bold font-mono">{Math.round(batteryStatus.level * 100)}%</span>;
  };

  useEffect(() => {
    attemptLocationFetch();
  }, []);

  useEffect(() => {
    let timer: any;
    if (activeTab === 'area_scanner' || (activeTab === 'transceiver' && syncNetTab === 'scanner')) {
      const updateTimer = () => {
        const now = new Date();
        const nextDay = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate() + 1));
        const diffMs = nextDay.getTime() - now.getTime();
        if (diffMs <= 0) {
          setMapsCountdown("00:00:00");
          return;
        }
        const h = Math.floor(diffMs / (1000 * 60 * 60));
        const m = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
        const s = Math.floor((diffMs % (1000 * 60)) / 1000);
        setMapsCountdown(`${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`);
      };
      updateTimer();
      timer = setInterval(updateTimer, 1000);
    }
    return () => clearInterval(timer);
  }, [activeTab, syncNetTab]);

  useEffect(() => {
    if ((activeTab === 'area_scanner' || (activeTab === 'transceiver' && syncNetTab === 'scanner')) && hasValidMapsKey && !geoError) {
      const todayStr = new Date().toISOString().split('T')[0];
      if (lastMapsApiCallDay !== todayStr) {
        localStorage.setItem("pox_last_maps_api_call_day", todayStr);
        setLastMapsApiCallDay(todayStr);
        triggerLog(`SATELLITE INTERFACE CONNECTED: Dialed Maps Satellite node for UTC segment ${todayStr}.`, "success");
      }
    }
  }, [activeTab, syncNetTab, hasValidMapsKey, geoError, lastMapsApiCallDay]);

  useEffect(() => {
    localStorage.setItem("pox_target_sequence", targetSequence);
  }, [targetSequence]);

  // Defender states
  const [defenderCreatureId, setDefenderCreatureId] = useState<string | null>(() => {
    return localStorage.getItem("pox_defender_creature_id") || null;
  });

  useEffect(() => {
    if (defenderCreatureId) {
      localStorage.setItem("pox_defender_creature_id", defenderCreatureId);
    } else {
      localStorage.removeItem("pox_defender_creature_id");
    }
  }, [defenderCreatureId]);

  // Hacking battle states
  const [activeHack, setActiveHack] = useState<{
    id: string;
    partner: NearbyUser;
    status: 'selecting_attacker' | 'battling' | 'completed';
    attacker?: Creature;
    defender?: Creature;
    attackerCurrentHp: number;
    defenderCurrentHp: number;
    battleLogs: string[];
    winnerUid: string | null;
    isTradeInterceptor?: boolean;
    usedSpecialMoves?: string[];
    harvestedGene?: string;
    harvestedExtraGenes?: string[];
  } | null>(null);

  // Auto scroll combat log to the bottom
  useEffect(() => {
    if (combatLogRef.current) {
      combatLogRef.current.scrollTop = combatLogRef.current.scrollHeight;
    }
  }, [activeHack?.battleLogs]);

  // Auto scroll scanner log to the bottom
  useEffect(() => {
    if (scannerLogRef.current) {
      scannerLogRef.current.scrollTop = scannerLogRef.current.scrollHeight;
    }
  }, [scannerLogs]);

  // G.E.N. Network email states
  const [genMails, setGenMails] = useState<GenMail[]>(() => {
    const saved = localStorage.getItem("pox_gen_network_mails");
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        if (parsed && parsed.length > 0) return parsed;
      } catch (e) {}
    }
    return [
      {
        id: "MAIL-9921DX",
        date: "2026-06-08 (TODAY)",
        tagline: "G.E.N. Network Daily Target Update: 128 High-Priority Genetic Sequences Dispatched",
        isRead: false,
        sequences: generateDailyBountySequences("2026-06-08"),
        type: "dispatch"
      },
      {
        id: "MAIL-9920DX",
        date: "2026-06-07 (YESTERDAY)",
        tagline: "Secure Broadcast Node: AGTC Sub-vector Desiderata Matrix Restructured",
        isRead: true,
        sequences: generateDailyBountySequences("2026-06-07"),
        type: "dispatch"
      },
      {
        id: "MAIL-9919DX",
        date: "2026-06-06",
        tagline: "P.O.X. Transceiver Sync Unit: Urgent Bio-Defense Harvest Request",
        isRead: true,
        sequences: generateDailyBountySequences("2026-06-06"),
        type: "dispatch"
      }
    ];
  });

  const [selectedMailId, setSelectedMailId] = useState<string | null>(null);
  const [mailPage, setMailPage] = useState<number>(0);
  const [selectedTradeOfferId, setSelectedTradeOfferId] = useState<string | null>(null);
  const [selectedExtensionSequence, setSelectedExtensionSequence] = useState<string | null>(null);
  const [autoHackerNonDetectionTimes, setAutoHackerNonDetectionTimes] = useState<Record<string, number>>({});

  useEffect(() => {
    localStorage.setItem("pox_gen_network_mails", JSON.stringify(genMails));
  }, [genMails]);

  // Memoized daily G.E.N. Network dispatch genes
  const dispatchGenes = React.useMemo(() => {
    const genes = new Set<string>();
    const todayMail = genMails.find((m) => m.type === 'dispatch' || (!m.type && m.sequences && m.sequences.length > 0));
    const todayTargets = todayMail ? todayMail.sequences : [];
    todayTargets.forEach((seq) => {
      for (let i = 0; i < seq.length; i += 8) {
        genes.add(seq.slice(i, i + 8));
      }
    });
    return genes;
  }, [genMails]);

  const checkCreatureHasDispatchGene = React.useCallback((seq: string) => {
    for (let i = 0; i < seq.length; i += 8) {
      if (dispatchGenes.has(seq.slice(i, i + 8))) return true;
    }
    return false;
  }, [dispatchGenes]);

  interface PlayerStatsData {
    totalSpliced: number;
    totalGenesAcquired: number;
    totalManualAccelerations: number;
    totalTradesCompleted: number;
    totalCreaturesSpliceHarvested: number;
    totalSignalBandsScanned: number;
    totalHacksInitiated: number;
    totalHacksWon: number;
    totalHacksLost: number;
    genNetworkMailsRead?: number;
    genNetworkBountyMatches?: number;
    interceptorHacksInitiated?: number;
    interceptorHacksWon?: number;
    interceptorHacksLost?: number;
    genNetworkConstructionsSucceeded?: number;
    genNetworkBonusTargetMatches?: number;
  }

  const [stats, setStats] = useState<PlayerStatsData>(() => {
    const saved = localStorage.getItem("pox_player_stats");
    if (saved) {
      try { return JSON.parse(saved); } catch (e) {}
    }
    return {
      totalSpliced: 1,
      totalGenesAcquired: 4,
      totalManualAccelerations: 0,
      totalTradesCompleted: 0,
      totalCreaturesSpliceHarvested: 0,
      totalSignalBandsScanned: 0,
      totalHacksInitiated: 0,
      totalHacksWon: 0,
      totalHacksLost: 0,
      genNetworkMailsRead: 0,
      genNetworkBountyMatches: 0,
      interceptorHacksInitiated: 0,
      interceptorHacksWon: 0,
      interceptorHacksLost: 0,
      genNetworkConstructionsSucceeded: 0,
      genNetworkBonusTargetMatches: 0
    };
  });

  useEffect(() => {
    localStorage.setItem("pox_player_stats", JSON.stringify(stats));
  }, [stats]);

  useEffect(() => {
    setSelectedGeneIndex(null);
  }, [inspectedCreatureId]);

  // Live Terminal Log Stream state (for footer and diagnostic metrics)
  const [logs, setLogs] = useState<{ time: string; text: string; type?: 'info' | 'warn' | 'success' }>({
    time: new Date().toTimeString().split(" ")[0],
    text: "MAIN BOOT SYSTEMS COMPASS SECTOR OK • RF LINK READY.",
    type: "success"
  });

  // Helper code to push events into status logger
  const triggerLog = (text: string, type: 'info' | 'warn' | 'success' = 'info') => {
    const timeStr = new Date().toTimeString().split(" ")[0];
    setLogs({ time: timeStr, text, type });
  };

  // Sync state to local storage
  useEffect(() => {
    localStorage.setItem("pox_gene_sequences", JSON.stringify(sequences));
  }, [sequences]);

  useEffect(() => {
    localStorage.setItem("pox_creatures", JSON.stringify(creatures));
  }, [creatures]);

  // Audio muting state control binding
  useEffect(() => {
    sound.isMuted = isMuted;
  }, [isMuted]);

  // Aggregate stats: calculate count of A, G, T, C across sequence gene assets
  const getAggregateNucleotides = () => {
    let A = 0, G = 0, T = 0, C = 0;
    sequences.forEach(s => {
      const match = s.sequence;
      const count = s.count;
      for (let i = 0; i < match.length; i++) {
        if (match[i] === "A") A += count;
        else if (match[i] === "G") G += count;
        else if (match[i] === "T") T += count;
        else if (match[i] === "C") C += count;
      }
    });
    return { A, G, T, C };
  };

  const totals = getAggregateNucleotides();

  // 1. Idle Gene Generator Logic (Every 16 seconds)
  useEffect(() => {
    // Prevent duplicate timers by cleaning up any pre-existing ticker / intervals immediately
    if (tickIntervalRef.current) clearInterval(tickIntervalRef.current);
    if (tickerIntervalRef.current) clearInterval(tickerIntervalRef.current);

    if (!isPowered) return;

    // Scrolling effect representing dynamic sequencing scans
    tickerIntervalRef.current = setInterval(() => {
      const chars = ["A", "G", "T", "C"];
      let temp = "";
      for (let i = 0; i < 8; i++) {
        temp += chars[Math.floor(Math.random() * chars.length)];
      }
      setScrollingGene(temp);
    }, 110);

    // Main 1-sec countdown
    tickIntervalRef.current = setInterval(() => {
      // Handle booster countdown decrement
      let expiredBooster = false;
      setBoostSecondsLeft((prevBoost) => {
        if (prevBoost > 0) {
          if (prevBoost === 1) {
            expiredBooster = true;
          }
          return prevBoost - 1;
        }
        return 0;
      });
      if (expiredBooster) {
        triggerLog("REACTOR BOOSTER EXPIRED: Bio-Lab tick rate returned to 16 seconds standard.", "warn");
      }

      // If reactor activity is frozen, don't progress the idle countdown timer
      if (isReactorFrozenRef.current) {
        return;
      }

      const isBoostActive = boostSecondsLeftRef.current > 0;
      const resetVal = isBoostActive ? 8 : 16;
      const prevVal = idleTimeRef.current;
      
      let nextVal = prevVal - 1;
      if (isBoostActive && prevVal > 8) {
        nextVal = 8;
      }

      if (nextVal <= 0) {
        if (devForceAnomalyRef.current) {
          // Dev force toggle active: Bypass fuel consumption & guarantee success
          const anomalousGene = generateAnomalousGene();
          sound.playSynthesisSuccess();
          triggerAddGeneSequence(anomalousGene);
          triggerLog(`[DEV MODE FUSION] Force-triggered successfully (Zero resources consumed). Gene block: ${anomalousGene}`, "success");
          addScannerLog(`[DEV INJECTED UNIT] Forced anomalous creation of: ${anomalousGene}`);
        } else if (anomalyEngineActiveRef.current && baseTalliesRef.current.grandTotal >= 250000) {
          // Calculate active scaling chance based on current grand total nucleotide stockpile
          const currentTotal = baseTalliesRef.current.grandTotal;
          const chanceMetrics = getAnomalyEngineSuccessChance(currentTotal);
          const roll = Math.random() * 100;

          // Consume 10k nucleotides
          consumeNucleotides(10000);

          if (roll <= chanceMetrics.finalChance) {
            const anomalousGene = generateAnomalousGene();
            sound.playSynthesisSuccess();
            triggerAddGeneSequence(anomalousGene);
            triggerLog(`[ANOMALY ENGINE] Fusion successful! Probability was ${chanceMetrics.finalChance.toFixed(3)}% (Rolled: ${roll.toFixed(3)}%). Generated anomalous block ${anomalousGene}.`, "success");
            addScannerLog(`[ANOMALY GENERATOR SUCCESS] Synthesized gene block ${anomalousGene} under unstable fusion conditions.`);
          } else {
            sound.playBeep(220, 0.35, "sawtooth");
            triggerLog(`[ANOMALY ENGINE] Decoupling failure. Fusion chanced: ${chanceMetrics.finalChance.toFixed(3)}% (Rolled: ${roll.toFixed(3)}%). 10,000 nucleotides decomposed in magnetic buffer.`, "warn");
            addScannerLog("[ANOMALY FAILED] Fusion was unsuccessful. Energy discharged without gene yield.");
          }
        } else {
          if (anomalyEngineActiveRef.current) {
            setAnomalyEngineActive(false);
            triggerLog("ANOMALY ENGINE SHUT DOWN: Nucleotide reserves fell below minimum 250k threshold.", "warn");
          }
          // Trigger standard gene splicing 8-gene packet
          const batch: string[] = [];
          for (let i = 0; i < 8; i++) {
            batch.push(generateWaveGeneBlock(todayWave));
          }
          sound.playCombinatorTick();
          triggerAddGeneSequences(batch);
        }
        nextVal = resetVal;
      }

      setIdleTime(nextVal);
    }, 1000);

    return () => {
      if (tickIntervalRef.current) {
        clearInterval(tickIntervalRef.current);
        tickIntervalRef.current = null;
      }
      if (tickerIntervalRef.current) {
        clearInterval(tickerIntervalRef.current);
        tickerIntervalRef.current = null;
      }
    };
  }, [isPowered]);

  const consumeNucleotides = (amount: number) => {
    const totalSequencesToConsume = Math.ceil(amount / 8);
    setSequences((prev) => {
      let remaining = totalSequencesToConsume;
      const updated = prev
        .map((s) => {
          if (isAnomalousGene(s.sequence)) {
            return s; // leave anomalous genes completely alone
          }
          if (remaining <= 0) {
            return s;
          }
          const toTake = Math.min(s.count, remaining);
          remaining -= toTake;
          return { ...s, count: s.count - toTake };
        })
        .filter((s) => s.count > 0);
      return updated;
    });
  };

  const handleToggleAnomalyEngine = (checked: boolean) => {
    if (checked) {
      if (baseTallies.grandTotal < 250000) {
        triggerLog("CANNOT ACTIVATE ANOMALY ENGINE: Requires a massive stockpile of minimum 250k total nucleotides.", "warn");
        return;
      }
      setAnomalyEngineActive(true);
      setNodeStability(0);
      setNodeEmissivity(0);
      triggerLog("ANOMALY ENGINE ENGAGED! High voltage strain resets Node Stability & Emissions to 0.", "warn");
      addScannerLog("[ANOMALY ENGINE INITIATION] All telemetry channels reset index to 0. Cosmic gene hunting activated!");
      sound.playBeep(120, 0.6, "sawtooth");
    } else {
      setAnomalyEngineActive(false);
      triggerLog("Anomaly Engine disengaged. Reactor power normalized. Standard Bio-Lab Reactor active.", "info");
      sound.playBeep(350, 0.15, "sine");
    }
  };

  // Add synthesized gene
  const triggerAddGeneSequence = (gene: string) => {
    setNewestSequence(gene);
    const alreadyDiscovered = sequences.some((s) => s.sequence === gene);
    
    setSequences((prev) => {
      const matchIdx = prev.findIndex((s) => s.sequence === gene);
      if (matchIdx >= 0) {
        const updated = [...prev];
        updated[matchIdx].count += 1;
        return updated;
      } else {
        return [...prev, { sequence: gene, count: 1, discoveredAt: Date.now() }];
      }
    });

    if (alreadyDiscovered) {
      setIsFreshSequence(false);
      setRecentSplicedGenes([{ sequence: gene, isNew: false }]);
      triggerLog(`CONSOLIDATED NUCLEOTIDE PATTERN: ${gene} (+1 owned)`, "info");
    } else {
      setIsFreshSequence(true);
      setRecentSplicedGenes([{ sequence: gene, isNew: true }]);
      triggerLog(`NEW RECOGNIZED GENE DISCOVERED: ${gene}`, "success");
    }

    setStats((prev) => ({
      ...prev,
      totalGenesAcquired: prev.totalGenesAcquired + 1
    }));
  };

  const triggerAddGeneSequences = (genes: string[]) => {
    if (genes.length === 0) return;
    setNewestSequence(genes[genes.length - 1]); // keep compatibility
    
    const newlySplicedList: { sequence: string; isNew: boolean }[] = [];
    const tempSequences = [...sequences];

    genes.forEach((gene) => {
      const matchIdx = tempSequences.findIndex((s) => s.sequence === gene);
      if (matchIdx >= 0) {
        newlySplicedList.push({ sequence: gene, isNew: false });
        tempSequences[matchIdx] = {
          ...tempSequences[matchIdx],
          count: tempSequences[matchIdx].count + 1
        };
      } else {
        newlySplicedList.push({ sequence: gene, isNew: true });
        tempSequences.push({ sequence: gene, count: 1, discoveredAt: Date.now() });
      }
    });

    setSequences((prev) => {
      const updated = [...prev];
      genes.forEach((gene) => {
        const matchIdx = updated.findIndex((s) => s.sequence === gene);
        if (matchIdx >= 0) {
          updated[matchIdx] = {
            ...updated[matchIdx],
            count: updated[matchIdx].count + 1
          };
        } else {
          updated.push({ sequence: gene, count: 1, discoveredAt: Date.now() });
        }
      });
      return updated;
    });

    setRecentSplicedGenes(newlySplicedList);
    setDiscoveredPacketsLog((prev) => {
      const newEntry = {
        id: Math.random().toString(36).substring(2, 9) + '-' + Date.now(),
        timestamp: Date.now(),
        genes: newlySplicedList
      };
      return [newEntry, ...prev].slice(0, 8);
    });

    setStats((prev) => ({
      ...prev,
      totalGenesAcquired: prev.totalGenesAcquired + genes.length
    }));

    const newlyDiscoveredCount = newlySplicedList.filter(g => g.isNew).length;
    if (newlyDiscoveredCount > 0) {
      triggerLog(`GENE SYNTHESIS COMPLETE: Packet ready (${newlyDiscoveredCount} new genes)`, "success");
    } else {
      triggerLog(`GENE SYNTHESIS COMPLETE: Packet ready (0 new genes)`, "info");
    }
  };

  // Click manual tactical acceleration
  const handleManualCombinatorClick = () => {
    if (!isPowered) return;
    sound.playCombinatorTick();
    setStats((prev) => ({
      ...prev,
      totalManualAccelerations: prev.totalManualAccelerations + 1
    }));

    const prevTime = idleTimeRef.current;
    const isBoostActive = boostSecondsLeftRef.current > 0;
    const resetVal = isBoostActive ? 8 : 16;

    let nextTime = prevTime > 2 ? prevTime - 2 : resetVal;

    // Trigger instant gene packet synthesis if accelerated to boundary reset
    if (prevTime <= 2) {
      const batch: string[] = [];
      for (let i = 0; i < 8; i++) {
        batch.push(generateWaveGeneBlock(todayWave));
      }
      triggerAddGeneSequences(batch);
    }

    setIdleTime(nextTime);
  };

  // Splicing Autofill for matching target sequence
  const handleAutofillSplicer = () => {
    sound.playBeep(880, 0.05, "sine");
    const updatedSlots = [...splicerSlots];
    let didChanges = false;
    let autoFilledCount = 0;

    // Work on a copy of inventory count to simulate decrements correctly
    const stockSequences = [...sequences];

    for (let i = 0; i < 8; i++) {
      if (updatedSlots[i] === null) {
        // Target required gene segment at slot i
        const requiredGene = targetSequence.substring(i * 8, (i + 1) * 8);

        // Find if this exact matching sequence is available in stock
        const stockIndex = stockSequences.findIndex(s => s.sequence === requiredGene && s.count > 0);
        if (stockIndex >= 0) {
          stockSequences[stockIndex] = {
            ...stockSequences[stockIndex],
            count: stockSequences[stockIndex].count - 1
          };
          updatedSlots[i] = requiredGene;
          didChanges = true;
          autoFilledCount++;
        }
      }
    }

    if (didChanges) {
      setSequences(stockSequences.filter(s => s.count > 0 || sequences.find(p => p.sequence === s.sequence)?.count === 0));
      setSplicerSlots(updatedSlots);
      triggerLog(`AUTO GENE: Filled ${autoFilledCount} matching slots with verified stock segments.`, "success");
    } else {
      triggerLog("AUTO GENE: No matching segments found in stock for any unfilled slots.", "warn");
    }
  };

  // Slot handlers
  const handleSlotClick = (slotIdx: number) => {
    if (isSplicing) return;
    sound.playBeep(880, 0.05, "sine");
    setActiveSlotSelection(slotIdx);
  };

  const handleSelectSequenceForSlot = (seq: string) => {
    if (activeSlotSelection === null) return;
    
    // Check if selected sequence matches the expected sequence for this slot location
    const expectedGene = targetSequence.substring(activeSlotSelection * 8, (activeSlotSelection + 1) * 8);
    if (seq !== expectedGene) {
      triggerLog(`GENOME MISMATCH: Gene does not match required slot sequence ${expectedGene}`, "warn");
      sound.playBeep(220, 0.2, "sawtooth");
      return;
    }

    // Reduce count by 1 in stock
    setSequences((prev) => {
      return prev.map((s) => {
        if (s.sequence === seq) {
          return { ...s, count: Math.max(0, s.count - 1) };
        }
        return s;
      }).filter((s) => s.count > 0 || prev.find(p => p.sequence === s.sequence)?.count === 0);
    });

    const updatedSlots = [...splicerSlots];
    
    // If there was already something in that slot, return it to stock
    const existing = updatedSlots[activeSlotSelection];
    if (existing) {
      setSequences((prev) => {
        const idx = prev.findIndex(p => p.sequence === existing);
        if (idx >= 0) {
          const u = [...prev];
          u[idx].count += 1;
          return u;
        } else {
          return [...prev, { sequence: existing, count: 1, discoveredAt: Date.now() }];
        }
      });
    }

    updatedSlots[activeSlotSelection] = seq;
    setSplicerSlots(updatedSlots);
    setActiveSlotSelection(null);
    sound.playBeep(440, 0.1, "triangle");
    triggerLog(`Assigned matching gene block to slot #${activeSlotSelection + 1}`, "success");
  };

  const handleEjectSlot = (slotIdx: number, e: React.MouseEvent) => {
    e.stopPropagation();
    const existing = splicerSlots[slotIdx];
    if (!existing) return;

    sound.playBeep(330, 0.1, "sawtooth");
    
    // Return to inventory stock
    setSequences((prev) => {
      const idx = prev.findIndex(p => p.sequence === existing);
      if (idx >= 0) {
        const u = [...prev];
        u[idx].count += 1;
        return u;
      } else {
        return [...prev, { sequence: existing, count: 1, discoveredAt: Date.now() }];
      }
    });

    const updated = [...splicerSlots];
    updated[slotIdx] = null;
    setSplicerSlots(updated);
    triggerLog(`Returned gene segment #${slotIdx + 1} to archive stock`, "info");
  };

  // G.E.N. Network specific splicing helpers
  const handleNetSlotClick = (slotIdx: number) => {
    if (isNetSplicing) return;
    sound.playBeep(880, 0.05, "sine");
    setActiveNetSlotSelection(slotIdx);
  };

  const handleSelectSequenceForNetSlot = (seq: string) => {
    if (activeNetSlotSelection === null || !selectedDispatchSequence) return;
    
    const expectedGene = selectedDispatchSequence.substring(activeNetSlotSelection * 8, (activeNetSlotSelection + 1) * 8);
    if (seq !== expectedGene) {
      triggerLog(`GENOME MISMATCH: Gene does not match required slot sequence ${expectedGene}`, "warn");
      sound.playBeep(220, 0.2, "sawtooth");
      return;
    }

    // Reduce count by 1 in stock
    setSequences((prev) => {
      return prev.map((s) => {
        if (s.sequence === seq) {
          return { ...s, count: Math.max(0, s.count - 1) };
        }
        return s;
      }).filter((s) => s.count > 0 || prev.find(p => p.sequence === s.sequence)?.count === 0);
    });

    const updatedSlots = [...netSplicerSlots];
    
    // If there was already something in that slot, return it to stock
    const existing = updatedSlots[activeNetSlotSelection];
    if (existing) {
      setSequences((prev) => {
        const idx = prev.findIndex(p => p.sequence === existing);
        if (idx >= 0) {
          const u = [...prev];
          u[idx].count += 1;
          return u;
        } else {
          return [...prev, { sequence: existing, count: 1, discoveredAt: Date.now() }];
        }
      });
    }

    updatedSlots[activeNetSlotSelection] = seq;
    setNetSplicerSlots(updatedSlots);
    setActiveNetSlotSelection(null);
    sound.playBeep(440, 0.1, "triangle");
    triggerLog(`Assigned matching G.E.N. gene block to slot #${activeNetSlotSelection + 1}`, "success");
  };

  const handleEjectNetSlot = (slotIdx: number, e: React.MouseEvent) => {
    e.stopPropagation();
    const existing = netSplicerSlots[slotIdx];
    if (!existing) return;

    sound.playBeep(330, 0.1, "sawtooth");
    
    // Return to inventory stock
    setSequences((prev) => {
      const idx = prev.findIndex(p => p.sequence === existing);
      if (idx >= 0) {
        const u = [...prev];
        u[idx].count += 1;
        return u;
      } else {
        return [...prev, { sequence: existing, count: 1, discoveredAt: Date.now() }];
      }
    });

    const updated = [...netSplicerSlots];
    updated[slotIdx] = null;
    setNetSplicerSlots(updated);
    triggerLog(`Returned G.E.N. gene segment #${slotIdx + 1} to archive stock`, "info");
  };

  const handleAutofillNetSplicer = () => {
    if (!selectedDispatchSequence) return;
    sound.playBeep(880, 0.05, "sine");
    const updatedSlots = [...netSplicerSlots];
    let didChanges = false;
    let autoFilledCount = 0;

    const stockSequences = [...sequences];

    for (let i = 0; i < 8; i++) {
      if (updatedSlots[i] === null) {
        const requiredGene = selectedDispatchSequence.substring(i * 8, (i + 1) * 8);

        const stockIndex = stockSequences.findIndex(s => s.sequence === requiredGene && s.count > 0);
        if (stockIndex >= 0) {
          stockSequences[stockIndex] = {
            ...stockSequences[stockIndex],
            count: stockSequences[stockIndex].count - 1
          };
          updatedSlots[i] = requiredGene;
          didChanges = true;
          autoFilledCount++;
        }
      }
    }

    if (didChanges) {
      setSequences(stockSequences.filter(s => s.count > 0 || sequences.find(p => p.sequence === s.sequence)?.count === 0));
      setNetSplicerSlots(updatedSlots);
      triggerLog(`G.E.N. AUTO GENE: Filled ${autoFilledCount} matching slots.`, "success");
    } else {
      triggerLog("G.E.N. AUTO GENE: No matching segments found in stock.", "warn");
    }
  };

  const applyBonusAndReset = (creature: Creature) => {
    let isBonusMatch = false;
    if (selectedDispatchSequence === targetSequence) {
      isBonusMatch = true;
      creature.vitality = Math.round(creature.vitality * 1.25);
      creature.attack = Math.round(creature.attack * 1.25);
      creature.defense = Math.round(creature.defense * 1.25);
      creature.speed = Math.round(creature.speed * 1.25);
      creature.name = `${creature.name} [G.E.N. PRIME]`;
      creature.origin = "G.E.N. Prime Target";
      
      const newRandTarget = generateRandom64Sequence();
      setTargetSequence(newRandTarget);
    }

    setCreatures((prev) => [creature, ...prev]);
    setCreatureCardOpenedFrom('Constructor');
    setInspectedCreatureId(creature.id);
    setActiveTab('library');
    
    setStats((prev) => ({
      ...prev,
      totalSpliced: prev.totalSpliced + 1,
      genNetworkConstructionsSucceeded: (prev.genNetworkConstructionsSucceeded || 0) + 1,
      genNetworkBonusTargetMatches: (prev.genNetworkBonusTargetMatches || 0) + (isBonusMatch ? 1 : 0)
    }));

    if (isBonusMatch) {
      triggerLog(`SUPER MATCH: Spliced "${creature.name}" with 1.25x G.E.N. PRIME stat bonus! Own target reset.`, "success");
    } else {
      triggerLog(`SUCCESSFUL ASSEMBLY: Spliced "${creature.name}" from G.E.N. broadcast target genome!`, "success");
    }
  };

  const handleConstructNetCreature = async () => {
    if (!isPowered) {
      triggerLog("SYS POWER OFFLINE. CANNOT COM-COGNATE SPLICING REACTOR.", "warn");
      sound.playReject();
      return;
    }
    if (netSplicerSlots.includes(null) || !selectedDispatchSequence) return;
    setIsNetSplicing(true);
    sound.playBeep(300, 0.3, "sawtooth");
    triggerLog("INITIATING SPLICING ENGINE PROCESS AND VERIFYING INTEGRITY CHANNELS...", "warn");

    const fullDNASeq = netSplicerSlots.join("");

    let prg = 0;
    const progressInterval = setInterval(() => {
      prg += 10;
      setNetSplicingProgress(prg);
      sound.playBeep(250 + prg * 5, 0.04, "sine");
      if (prg >= 100) {
        clearInterval(progressInterval);
      }
    }, 200);

    setTimeout(async () => {
      try {
        const res = await fetch("/api/creatures/generate-desc", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ sequence: fullDNASeq })
        });
        const data = await res.json();
        
        let spawned: Creature;
        
        if (data.success && data.creature) {
          spawned = {
            id: `PX-${Math.random().toString(36).substr(2, 9).toUpperCase()}`,
            sequence: fullDNASeq,
            name: data.creature.name,
            faction: data.creature.faction,
            type: data.creature.type,
            vitality: Number(data.creature.vitality) || 120,
            attack: Number(data.creature.attack) || 50,
            defense: Number(data.creature.defense) || 50,
            speed: Number(data.creature.speed) || 50,
            primaryWeapon: data.creature.primaryWeapon || "G.E.N. Beam",
            lore: data.creature.lore || "G.E.N. Target Construction.",
            asciiArt: data.creature.asciiArt || "o.x.o\n.###.\nx.o.x",
            discoveredAt: Date.now(),
            origin: "G.E.N. Coms Splicer"
          };
        } else {
          throw new Error("Splicer API error");
        }
        
        applyBonusAndReset(spawned);
      } catch (err) {
        const fallbackCreature = constructProceduralCreature(fullDNASeq, "G.E.N. Com-Net Protocol");
        applyBonusAndReset(fallbackCreature);
      } finally {
        setIsNetSplicing(false);
        setNetSplicingProgress(0);
        setNetSplicerSlots([null, null, null, null, null, null, null, null]);
        setSelectedDispatchSequence(null);
        sound.playSynthesisSuccess();
      }
    }, 2000);
  };

  // Constructs a mutant cyborg from 64-character DNA
  const handleConstructCreature = async () => {
    if (!isPowered) {
      triggerLog("SYS POWER OFFLINE. CANNOT COM-COGNATE SPLICING REACTOR.", "warn");
      sound.playReject();
      return;
    }
    if (splicerSlots.includes(null)) return;
    setIsSplicing(true);
    sound.playBeep(300, 0.3, "sawtooth");
    triggerLog("INITIATING MORPHOGENESIS THERMALS...", "warn");

    const fullDNASeq = splicerSlots.join("");

    // Splicing simulator countdown ticks
    let prg = 0;
    const progressInterval = setInterval(() => {
      prg += 10;
      setSplicingProgress(prg);
      sound.playBeep(250 + prg * 5, 0.04, "sine");
      if (prg >= 100) {
        clearInterval(progressInterval);
      }
    }, 200);

    setTimeout(async () => {
      try {
        const res = await fetch("/api/creatures/generate-desc", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ sequence: fullDNASeq })
        });
        const data = await res.json();
        
        if (data.success && data.creature) {
          const spawned: Creature = {
            id: `PX-${Math.random().toString(36).substr(2, 9).toUpperCase()}`,
            sequence: fullDNASeq,
            name: data.creature.name,
            faction: data.creature.faction,
            type: data.creature.type,
            vitality: Number(data.creature.vitality) || 120,
            attack: Number(data.creature.attack) || 50,
            defense: Number(data.creature.defense) || 50,
            speed: Number(data.creature.speed) || 50,
            primaryWeapon: data.creature.primaryWeapon || "Energy Lance",
            lore: data.creature.lore || "A fast splicing mutant warrior.",
            asciiArt: data.creature.asciiArt || "o.x.o\n.###.\nx.o.x",
            discoveredAt: Date.now(),
            origin: "Spliced Gene"
          };
          setCreatures((prev) => [spawned, ...prev]);
          setCreatureCardOpenedFrom('Constructor');
          setInspectedCreatureId(spawned.id);
          setActiveTab('library');
          setStats((prev) => ({
            ...prev,
            totalSpliced: prev.totalSpliced + 1
          }));
          triggerLog(`SUCCESSFUL ASSEMBLY OF ${spawned.name}!`, "success");
        } else {
          throw new Error("Splicer API error");
        }
      } catch (err) {
        // High quality fallback procedural local synthesis
        const fallbackCreature = constructProceduralCreature(fullDNASeq, "Local Mutation");
        setCreatures((prev) => [fallbackCreature, ...prev]);
        setCreatureCardOpenedFrom('Constructor');
        setInspectedCreatureId(fallbackCreature.id);
        setActiveTab('library');
        setStats((prev) => ({
          ...prev,
          totalSpliced: prev.totalSpliced + 1
        }));
        triggerLog(`COMPILED CREATURE LOCALLY: "${fallbackCreature.name}" created`, "success");
      } finally {
        setIsSplicing(false);
        setSplicingProgress(0);
        setSplicerSlots([null, null, null, null, null, null, null, null]);
        sound.playSynthesisSuccess();
      }
    }, 2200);
  };

  // Forces a 64-character genome construction bypassing standard gene availability constraints
  const handleForceConstruction = (customSequence?: string | any) => {
    if (isSplicing || isForcedConstructionActive) return;

    if (!isPowered) {
      triggerLog("SYS POWER OFFLINE. CANNOT FORCE CONSTRUCTION.", "warn");
      sound.playReject();
      return;
    }

    const finalCustomSeq = (typeof customSequence === 'string' && customSequence.length === 64) ? customSequence : undefined;
    const initialTargetSeq = finalCustomSeq || targetSequence;

    sound.playBeep(220, 0.4, "sawtooth");
    setIsForcedConstructionActive(true);
    setIsReactorFrozen(true);

    // Call the iteration engine
    runForcedConstructionCycle(initialTargetSeq, !!finalCustomSeq);
  };

  const runForcedConstructionCycle = (dnaSeq: string, isCustom: boolean) => {
    const activeWaveConfig = getDailyWaveConfig(new Date());

    setReactorFreezeTimeLeft(8.0);

    // Apply moon modifiers to failureChance (base 37.5)
    let failureChance = 37.5;
    let moonStatusLog = "None / Baseline";
    if (activeWaveConfig.isNewMoon) {
      failureChance += 3.75;
      moonStatusLog = "New Moon (+3.75% Debuff on construction)";
    } else if (activeWaveConfig.isFullMoon) {
      failureChance -= 3.75;
      moonStatusLog = "Full Moon (-3.75% Buff on construction)";
    }

    const isLooping = isForcedLoopActiveRef.current;

    const logs: string[] = [
      `[INIT] >> ENGAGING EMERGENCY GENE OVERRIDE SEQUENCE...`,
      ...(isLooping ? [
        `[STATUS: FREEZE MAINTAINED] >> Reactor thermal coils frozen for active construction loop.`,
        `[REASON] >> SYSTEMS RUNNING UNDER CRITICAL LOOP VOLTAGE.`
      ] : [
        `[WARNING] >> REACTOR OVERHEATING RISK: STABILIZING COILS FORCED TO FREEZE FOR 8.0 SECONDS.`
      ]),
      `[LUNAR STATUS] >> Phase: ${activeWaveConfig.phaseName} | Effective Debuff Mod: ${moonStatusLog}`,
      `[CALCULATE] >> Analysing 64 bases against available nucleotide archives (Fail Chance: ${failureChance.toFixed(2)}%)...`
    ];
    setForcedConstructionLogs(logs);

    let currentStock = [...sequences];

    // Helper to deduct a gene from stock
    const deductFromStock = (seq: string): boolean => {
      const idx = currentStock.findIndex(s => s.sequence === seq && s.count > 0);
      if (idx >= 0) {
        const copy = [...currentStock];
        if (copy[idx].count > 1) {
          copy[idx] = { ...copy[idx], count: copy[idx].count - 1 };
        } else {
          copy.splice(idx, 1);
        }
        currentStock = copy;
        return true;
      }
      return false;
    };

    // Helper to find a gene possessing expectedChar at index position j
    const findAndSacrificeGene = (expectedChar: string, j: number): string | null => {
      const idx = currentStock.findIndex(s => s.sequence[j] === expectedChar && s.count > 0);
      if (idx >= 0) {
        const matchingSeq = currentStock[idx].sequence;
        deductFromStock(matchingSeq);
        return matchingSeq;
      }
      return null;
    };

    let totalPrimaryConsumed = 0;
    let totalSacrificed = 0;
    const fullDNASeq = dnaSeq;
    
    // Step logs to play out second-by-second
    const stepLogs: { second: number; text: string }[] = [];

    // Analyze base by base
    for (let i = 0; i < 8; i++) {
      const expectedGene = fullDNASeq.substring(i * 8, (i + 1) * 8);
      let scaffoldStr = "";
      let scaffoldType = "";

      if (!isCustom && splicerSlots[i] === expectedGene) {
        scaffoldStr = expectedGene;
        scaffoldType = "PRE-ALIGNED MANUAL GENE";
      } else {
        // Look for exact matching gene first
        const hasMatch = currentStock.some(s => s.sequence === expectedGene && s.count > 0);
        if (hasMatch) {
          deductFromStock(expectedGene);
          scaffoldStr = expectedGene;
          scaffoldType = "MATCH STOCK RECRUITED";
          totalPrimaryConsumed++;
        } else {
          // Consume any random available gene
          const anyGene = currentStock.find(s => s.count > 0);
          if (anyGene) {
            deductFromStock(anyGene.sequence);
            scaffoldStr = anyGene.sequence;
            scaffoldType = `ANY GENE UNSTABLE BASE [${anyGene.sequence.slice(0, 4)}...]`;
            totalPrimaryConsumed++;
          } else {
            scaffoldStr = "--------";
            scaffoldType = "VOID SYNTHESIS SCAFFOLD";
          }
        }
      }

      stepLogs.push({
        second: Math.min(7, i + 1),
        text: `Slot #${i + 1} processing using scaffold: ${scaffoldType}`
      });

      // Evaluate 8 bases in this gene
      for (let j = 0; j < 8; j++) {
        const expectedChar = expectedGene[j];
        const scaffoldChar = scaffoldStr[j];
        
        // Match evaluation with success roll
        const roll = Math.random() * 100;

        if (scaffoldChar === expectedChar && roll >= failureChance) {
          // Perfectly appended! No sacrifice
        } else {
          // Appending failed (either due to character mismatch or random chance rule)
          // Find and sacrifice a gene having expectedChar at position j
          const sacrificedSeq = findAndSacrificeGene(expectedChar, j);
          
          if (sacrificedSeq) {
            totalSacrificed++;
            stepLogs.push({
              second: Math.min(7, i + 1),
              text: ` ➔ FAILED APPEND (pos ${j + 1}). Sacrificed gene ${sacrificedSeq} (depleting pool)`
            });
          } else {
            // If no gene fits, consume any available gene from stock
            const backupSacrifice = currentStock.find(s => s.count > 0);
            if (backupSacrifice) {
              deductFromStock(backupSacrifice.sequence);
              totalSacrificed++;
              stepLogs.push({
                second: Math.min(7, i + 1),
                text: ` ➔ FAILED APPEND (pos ${j + 1}). Sacrificed backup gene ${backupSacrifice.sequence} to guarantee placement`
              });
            } else {
              stepLogs.push({
                second: Math.min(7, i + 1),
                text: ` ➔ FAILED APPEND (pos ${j + 1}). Placed via thermal override with no remaining stock available`
              });
            }
          }

          // Reduce failure chance successively
          failureChance = Math.max(0, failureChance - 3.25);
        }
      }
    }

    // Capture state results at the outer scope, then fire them on complete!
    const finalStockResult = currentStock;

    // Run the 8-second interval ticker in tenths of a second
    let elapsedTenths = 0;
    const currentLogs = [...logs];

    const interval = setInterval(() => {
      elapsedTenths++;
      const currentSecondsFloat = elapsedTenths / 10;
      setReactorFreezeTimeLeft(Number(Math.max(0, 8.0 - currentSecondsFloat).toFixed(1)));

      const lastWholeSec = Math.floor((elapsedTenths - 1) / 10);
      const currWholeSec = Math.floor(elapsedTenths / 10);

      if (currWholeSec > lastWholeSec && currWholeSec <= 8) {
        currentLogs.push(`[${currWholeSec}.0s] >> Splicing thermal energy... Calibration progress: ${Math.round((currWholeSec / 8) * 100)}%`);
        
        // Add matching step logs for this elapsed second
        const matching = stepLogs.filter(el => el.second === currWholeSec);
        matching.forEach(el => {
          currentLogs.push(`  ${el.text}`);
        });

        setForcedConstructionLogs([...currentLogs]);
      }

      if (elapsedTenths >= 80) {
        clearInterval(interval);
        
        // Finalize state
        setSequences(finalStockResult);
        setSplicerSlots([null, null, null, null, null, null, null, null]);

        // Synthesize creature and save targetSequence before rotating
        const fallbackCreature = constructProceduralCreature(fullDNASeq, "Forced Unstable Splicing");
        fallbackCreature.id = `PX-${Math.random().toString(36).substr(2, 9).toUpperCase()}`;
        fallbackCreature.name = `${fallbackCreature.name} [FORCED]`;
        fallbackCreature.lore = `This hybrid genome was forced together in the bio-lab reactor. ${fallbackCreature.lore}`;
        fallbackCreature.origin = "Forced Synthesis";

        // Append to creatures list
        setCreatures(prev => [fallbackCreature, ...prev]);

        setStats(prev => ({
          ...prev,
          totalSpliced: prev.totalSpliced + 1
        }));

        sound.playSynthesisSuccess();

        // Check if loop is still active
        if (isForcedLoopActiveRef.current) {
          triggerLog(`[FORCED LOOP] Specimen "${fallbackCreature.name}" assembled successfully! Continuing loop iteration...`, "success");

          // For the next loop iteration, rotate target sequence if we are not custom-splicing
          let nextDna = fullDNASeq;
          if (!isCustom) {
            nextDna = generateRandom64Sequence();
            setTargetSequence(nextDna);
          }

          // Delay starting the next loop iteration by 1 second to keep UI legible
          setTimeout(() => {
            if (isForcedLoopActiveRef.current) {
              runForcedConstructionCycle(nextDna, isCustom);
            } else {
              setIsReactorFrozen(false);
              setIsForcedConstructionActive(false);
              triggerLog("FORCED LOOP DEACTIVATED. Reactor coils warming up. Standby.", "info");
            }
          }, 1000);

        } else {
          setIsReactorFrozen(false);
          setIsForcedConstructionActive(false);
          setCreatureCardOpenedFrom('Constructor');
          setInspectedCreatureId(fallbackCreature.id);
          setActiveTab('library');

          // Rotate target
          if (!isCustom) {
            const nextTarget = generateRandom64Sequence();
            setTargetSequence(nextTarget);
          }

          triggerLog(`FORCED CONSTRUCTION COMPLETED. Produced specimen: "${fallbackCreature.name}"!`, "success");
        }
      }
    }, 100);
  };

  // RF Transceiver location sector tracking scans
  const handleScanTransceiver = () => {
    if (isScanning) return;
    setIsScanning(true);
    sound.playBeep(640, 0.1, "sine");
    triggerLog("SWEEPING FOR PEER NODE SPECTRUM OSCILLATIONS...", "warn");
    setStats((prev) => ({
      ...prev,
      totalSignalBandsScanned: prev.totalSignalBandsScanned + 1
    }));
    
    setTimeout(() => {
      setIsScanning(false);
      const activeDailySeqs = genMails[0]?.sequences || [];
      const users: NearbyUser[] = Array.from({ length: 20 }, createRandomNearbyUser)
        .map(u => {
          if (activeDailySeqs.length > 0 && Math.random() < 0.5) {
            const matchedTarget = activeDailySeqs[Math.floor(Math.random() * activeDailySeqs.length)];
            return {
              ...u,
              targetSequence: matchedTarget
            };
          }
          return u;
        });
      setNearbyUsers(users);
      sound.playTradeSuccess();
      triggerLog("SIGNAL SCAN COMPLETED: 20 active P.O.X. hosts located within 30ft search grid.", "success");
    }, 1800);
  };

  // Hourly simulation incoming trade signal dispatched to G.E.N. Network mail inbox
  useEffect(() => {
    if (!isPowered) return;
    
    const encounterInterval = setInterval(() => {
      setGenMails(prev => {
        if (prev.length >= 24) return prev;

        sound.playAlertBuzzer();
        const randomUser = createRandomNearbyUser();
        
        // Find latest target sequence batch list from active dispatches
        const latestDispatch = prev.find(m => m.type === 'dispatch' || !m.type);
        const activeDailySeqs = latestDispatch?.sequences || [];
        
        let hasMatchedSequence = false;
        if (activeDailySeqs.length > 0 && Math.random() < 0.5) {
          randomUser.targetSequence = activeDailySeqs[Math.floor(Math.random() * activeDailySeqs.length)];
          hasMatchedSequence = true;
        }
        
        const offer = randomUser.creaturesAvailable[0];
        const tradeId = `TR-${Math.floor(Math.random() * 8999 + 1000)}`;

        const duration = hasMatchedSequence ? 1 * 60 * 60 * 1000 : 2 * 60 * 60 * 1000;

        const newTradeMail: GenMail = {
          id: `MAIL-TRADE-${tradeId}`,
          date: new Date().toISOString().split("T")[0],
          tagline: hasMatchedSequence
            ? `[G.E.N. BONUS MATCH] Incoming trade proposal from Host "${randomUser.uid}" (Target Sequence MATCHES dispatch targets!)`
            : `Peer Node Transfer Request by "${randomUser.uid}"`,
          isRead: false,
          sequences: [],
          type: 'trade_request',
          createdAt: Date.now(),
          expiresAt: Date.now() + duration,
          tradeRequest: {
            id: tradeId,
            partner: randomUser,
            partnerOfferedCreature: offer,
            status: 'pending'
          }
        };

        triggerLog(`NEW PROPOSAL: Peer Node Transfer Request routed into your G.E.N. inbox from Host "${randomUser.uid}".`, "warn");
        return [newTradeMail, ...prev];
      });
    }, 3600000); // 1 hour = 3600000 ms

    return () => clearInterval(encounterInterval);
  }, [isPowered]);

  // Automated hacker simulation protocol
  const runAutoHackerTurns = () => {
    const autoHackers = creatures.filter(c => c.isAutoHacker);
    if (autoHackers.length === 0) return;

    const timestamp = new Date().toTimeString().split(" ")[0];

    autoHackers.forEach(hacker => {
      if (nearbyUsers.length === 0) {
        setAutoHackerNonDetectionTimes(prev => {
          const count = (prev[hacker.id] || 0) + 1;
          if (count >= 5 && nodeEmissivity < 100) {
            setCreatures(cPrev => cPrev.map(c => {
              if (c.id === hacker.id) {
                return { ...c, isAutoHacker: false };
              }
              return c;
            }));
            
            setGenMails(mPrev => mPrev.map(m => {
              if (m.id === `MAIL-AUTO-${hacker.id}` && m.autoHackLog) {
                return {
                  ...m,
                  tagline: `[DEACTIVATED] G.E.N. Auto-Hack Terminal - "${hacker.name}" (CLEARED)`,
                  autoHackLog: {
                    ...m.autoHackLog,
                    log: [
                      ...m.autoHackLog.log,
                      `[${timestamp}] >> [PEER RADAR SILENT] Auto hacker ceased operations after 5 minutes of non-detection of adjacent Node hosts.`
                    ]
                  }
                };
              }
              return m;
            }));

            triggerLog(`Auto-Hacker "${hacker.name}" ceased active hacking operations due to a 5-minute period of non-detection of other players.`, "info");
          } else {
            setGenMails(mPrev => mPrev.map(m => {
              if (m.id === `MAIL-AUTO-${hacker.id}` && m.autoHackLog) {
                return {
                  ...m,
                  autoHackLog: {
                    ...m.autoHackLog,
                    log: [
                      ...m.autoHackLog.log,
                      `[${timestamp}] >> Sweep radar scanned... No active peer nodes found. (${count}/5 mins non-detection tolerance).`
                    ]
                  }
                };
              }
              return m;
            }));
          }
          return { ...prev, [hacker.id]: count };
        });
        return;
      }

      setAutoHackerNonDetectionTimes(prev => ({ ...prev, [hacker.id]: 0 }));

      const randomPeer = nearbyUsers[Math.floor(Math.random() * nearbyUsers.length)];
      const targetDefender = randomPeer.creaturesAvailable?.[0] || constructProceduralCreature("TTTAAACGTTTAAACGAGTCGTACCCCGGMAATTTAAACGTTTAAACGAGTCGTACCCCGGMAAT", "Peer Node Shield Unit");

      // Get effective stats degraded by remaining telomeres
      const effHacker = getEffectiveStats(hacker);
      const effTarget = getEffectiveStats(targetDefender);

      let attackerHp = effHacker.vitality;
      let defenderHp = effTarget.vitality;
      const combatLogs: string[] = [];

      combatLogs.push(`[${timestamp}] * AUTO-HACK INITIATED on Host: "${randomPeer.uid}"`);
      combatLogs.push(`[${timestamp}] * Target Defensive Payload: "${targetDefender.name}" (${effTarget.vitality} HP)`);

      let round = 1;
      const usedMoves: string[] = [];

      // Anomalous benefits calculations
      const attackerBenefits = getAnomalousBenefits(hacker.sequence);
      const defenderBenefits = getAnomalousBenefits(targetDefender.sequence);

      const hasDoubleStrikeAtk = attackerBenefits.some(b => b.id === 'DOUBLE_STRIKE');
      const hasSelfDestructAtk = attackerBenefits.some(b => b.id === 'SELF_DESTRUCT');
      const hasSiphonAtk = attackerBenefits.some(b => b.id === 'HEALTH_REGEN');
      const hasPhaseShiftAtk = attackerBenefits.some(b => b.id === 'PHASE_SHIFT');

      const hasDoubleStrikeDef = defenderBenefits.some(b => b.id === 'DOUBLE_STRIKE');
      const hasSelfDestructDef = defenderBenefits.some(b => b.id === 'SELF_DESTRUCT');
      const hasSiphonDef = defenderBenefits.some(b => b.id === 'HEALTH_REGEN');
      const hasPhaseShiftDef = defenderBenefits.some(b => b.id === 'PHASE_SHIFT');

      while (attackerHp > 0 && defenderHp > 0 && round < 30) {
        let attDmg = Math.max(8, Math.floor(effHacker.attack * (0.8 + Math.random() * 0.4)) - Math.floor(effTarget.defense * 0.22));
        let defDmg = Math.max(8, Math.floor(effTarget.attack * (0.8 + Math.random() * 0.4)) - Math.floor(effHacker.defense * 0.22));

        let specTriggered = "";

        const moves = getUnlockedMoves(hacker.sequence);
        if (moves.length > 0 && Math.random() < 0.35) {
          const unUsed = moves.filter(mv => !usedMoves.includes(mv.name));
          if (unUsed.length > 0) {
            const mvObj = unUsed[0];
            usedMoves.push(mvObj.name);
            if (mvObj.type === 'healing') {
              attackerHp = Math.min(effHacker.vitality, attackerHp + 35);
              attDmg += 35;
              specTriggered = `⚡ SPECIAL [${mvObj.name}]: Siphoning 35 HP from defender and repairing hardware armor.`;
            } else {
              defDmg = 0;
              specTriggered = `🛡️ SPECIAL [${mvObj.name}]: Adaptive evasion deflection sequence. Negating round damage.`;
            }
          }
        }

        // Apply Anomalous special benefits on each combat round
        if (hasDoubleStrikeAtk) {
          attDmg = Math.round(attDmg * 1.8);
          specTriggered += " [ANOMALY: Vortex Phase-Strike x1.8 Attack!]";
        }
        if (hasDoubleStrikeDef) {
          defDmg = Math.round(defDmg * 1.8);
          specTriggered += " [ANOMALY: Vortex Phase-Strike x1.8 Counter!]";
        }

        if (hasPhaseShiftAtk && Math.random() < 0.3) {
          defDmg = 0;
          specTriggered += " [ANOMALY: Temporal Mirror Shield Dodged Attack!]";
        }
        if (hasPhaseShiftDef && Math.random() < 0.3) {
          attDmg = 0;
          specTriggered += " [ANOMALY: Temporal Mirror Shield Opponent Dodged!]";
        }

        if (hasSiphonAtk) {
          attackerHp = Math.min(effHacker.vitality, attackerHp + 15);
          specTriggered += " [ANOMALY: Bio-Organic Siphon Regenerated +15 HP!]";
        }
        if (hasSiphonDef) {
          defenderHp = Math.min(effTarget.vitality, defenderHp + 15);
          specTriggered += " [ANOMALY: Bio-Organic Siphon Opponent Regenerated +15 HP!]";
        }

        defenderHp = Math.max(0, defenderHp - attDmg);
        if (defenderHp > 0) {
          attackerHp = Math.max(0, attackerHp - defDmg);
        }

        combatLogs.push(`[Round ${round}] "${hacker.name}" dealt -${attDmg} HP. "${targetDefender.name}" counterstrike: -${defDmg} HP.`);
        if (specTriggered) {
          combatLogs.push(`   >> ${specTriggered}`);
        }
        round++;
      }

      let playerWon = attackerHp > 0 && defenderHp <= 0;
      const tieGame = attackerHp <= 0 && defenderHp <= 0;
      let playerDefeated = defenderHp > 0 && attackerHp <= 0;

      // Handle self destruct
      if (playerDefeated && hasSelfDestructAtk) {
        combatLogs.push(`💥 ANOMALOUS OVERDRIVE [Supernova Reverb] triggered! ${hacker.name} self-destructed and vaporized ${targetDefender.name}!`);
        defenderHp = 0;
        playerWon = false;
        playerDefeated = false;
      } else if (playerWon && hasSelfDestructDef) {
        combatLogs.push(`💥 Hostile Defender anomaly triggered [Supernova Reverb]! Self-destructed and crushed your auto-hacker ${hacker.name}!`);
        attackerHp = 0;
        playerWon = false;
        playerDefeated = false;
      }

      let battleOutcomeLine = "";
      let isDefeated = false;
      const snatchedGenesList: string[] = [];

      if (playerWon) {
        battleOutcomeLine = `[${timestamp}] >>> SUCCESS: Host cybernetic sector breached! Snatched raw data genes from local target.`;
        const randIndex = Math.floor(Math.random() * 8);
        const autoSnatchedGene = targetDefender.sequence.slice(randIndex * 8, (randIndex + 1) * 8);
        snatchedGenesList.push(autoSnatchedGene);

        // EXTRA HARVEST BOOST gene
        const hasHarvestBoost = attackerBenefits.some(b => b.id === 'HARVEST_BOOST');
        if (hasHarvestBoost) {
          const randIndexEx = Math.floor(Math.random() * 8);
          const extraSnatchedGene = targetDefender.sequence.slice(randIndexEx * 8, (randIndexEx + 1) * 8);
          snatchedGenesList.push(extraSnatchedGene);
          combatLogs.push(`🌌 ANOMALOUS QUANTUM EXTRACT [Quantum Extraction Unit] activated! Snatched extra gene segment: ${extraSnatchedGene}`);
        }

        setSequences(sPrev => {
          let updated = [...sPrev];
          snatchedGenesList.forEach(gene => {
            const matched = updated.findIndex(sq => sq.sequence === gene);
            if (matched >= 0) {
              updated[matched] = { ...updated[matched], count: updated[matched].count + 1 };
            } else {
              updated.push({ sequence: gene, count: 1, discoveredAt: Date.now() });
            }
          });
          return updated;
        });

        setStats(sPrev => ({
          ...sPrev,
          totalHacksWon: (sPrev.totalHacksWon || 0) + 1,
          totalGenesAcquired: (sPrev.totalGenesAcquired || 0) + snatchedGenesList.length
        }));
        setNodeEmissivity(prev => Math.min(100, prev + (hacker.attack / 10)));
      } else if (attackerHp <= 0 && defenderHp <= 0) {
        battleOutcomeLine = `[${timestamp}] >>> STAGNATED TIE: Both bio-synthesizers crashed.`;
      } else {
        battleOutcomeLine = `[${timestamp}] >>> NEUTRALIZED: Specimen was neutralized and defeated in battle by host defensive grid in auto-hack!`;
        isDefeated = true;
        decreaseTelomeres(hacker.id, 25, "defeat in auto-hacking combat");
        setCreatures(cPrev => cPrev.map(c => {
          if (c.id === hacker.id) {
            return { ...c, isAutoHacker: false };
          }
          return c;
        }));
        setStats(sPrev => ({
          ...sPrev,
          totalHacksLost: (sPrev.totalHacksLost || 0) + 1
        }));
      }

      setGenMails(mPrev => {
        const mailId = `MAIL-AUTO-${hacker.id}`;
        const existingMailIdx = mPrev.findIndex(m => m.id === mailId);
        
        const appendedLines = [
          `\n--- BATTLE REPORT AT ${timestamp} ---`,
          ...combatLogs,
          battleOutcomeLine,
          isDefeated ? `[G.E.N. GRID REPORT] WARNING: Specimen was defeated and protocol deactivated.` : `[G.E.N. SYSTEM MONITOR] Stream tracking stable. Next query interval scheduled.`
        ];

        if (existingMailIdx >= 0) {
          const updatedMail = { ...mPrev[existingMailIdx] };
          if (updatedMail.autoHackLog) {
            updatedMail.tagline = isDefeated 
              ? `[DEFEATED] G.E.N. Auto-Hack Terminal - "${hacker.name}" (CLEARED)`
              : `[ACTIVE] G.E.N. Auto-Hack Terminal - Stream "${hacker.name}"`;
            updatedMail.isRead = false;
            updatedMail.expiresAt = Date.now() + 24 * 60 * 60 * 1000;
            updatedMail.autoHackLog = {
              ...updatedMail.autoHackLog,
              log: trimInnerBattleLogs([...updatedMail.autoHackLog.log, ...appendedLines]),
              isDefeated: isDefeated,
              genesGained: [...(updatedMail.autoHackLog.genesGained || []), ...snatchedGenesList]
            };
          }
          const updatedMails = [...mPrev];
          updatedMails[existingMailIdx] = updatedMail;
          return updatedMails;
        } else {
          return [{
            id: mailId,
            date: new Date().toISOString().split("T")[0],
            tagline: isDefeated 
              ? `[DEFEATED] G.E.N. Auto-Hack Terminal - "${hacker.name}" (CLEARED)`
              : `[ACTIVE] G.E.N. Auto-Hack Terminal - Stream "${hacker.name}"`,
            isRead: false,
            sequences: [],
            type: 'auto_hack_log',
            createdAt: Date.now(),
            expiresAt: Date.now() + 24 * 60 * 60 * 1000,
            autoHackLog: {
              creature: hacker,
              log: trimInnerBattleLogs([
                `[G.E.N. SYSTEM MONITOR INTERCEPT COMS] Unauthorized bio-mutant carrier waves intercepted from localhost. Tracking: "${hacker.name}"`,
                ...appendedLines
              ]),
              isDefeated: isDefeated,
              genesGained: snatchedGenesList
            }
          } as GenMail, ...mPrev];
        }
      });

      triggerLog(`Auto-Hacker "${hacker.name}" completed combat hack against peer host "${randomPeer.uid}".`, isDefeated ? "warn" : "success");
    });
  };

  useEffect(() => {
    if (!isPowered) return;
    const autoHackerInterval = setInterval(() => {
      runAutoHackerTurns();
    }, 60000); // 1 minute interval
    return () => clearInterval(autoHackerInterval);
  }, [isPowered, creatures, nearbyUsers]);

  // Simulated incoming security breach attempts from the Sync-Net
  const runSimulatedIncomingAttacks = () => {
    if (nodeStability >= 100) return;
    const defender = creatures.find(c => c.id === defenderCreatureId);
    const timestamp = new Date().toTimeString().split(" ")[0];

    if (!defender) {
      sound.playAlertBuzzer();
      triggerLog("CRITICAL DETECT ALERT: Incoming Sync-Net port breach. Deploy a DEFENDER creature to deflect hacks!", "warn");
      return;
    }

    // Player has a defender! Let's simulate a combat battle
    sound.playAlertBuzzer();
    triggerLog(`SYSTEM SECURITY SHIELD WATCH: Your defender "${defender.name}" is defending against a live hack request.`, "warn");

    // Hostile attacker specimen creation
    const hostiles = [
      "HOSTILE-PHAGE-NANO",
      "BREACH-MALVECTOR-X",
      "CYBER-STALKER-7",
      "GENE-EXTRACTOR-A9"
    ];
    // Get defender's effective stats degraded by remaining telomeres
    const effDefender = getEffectiveStats(defender);

    const attackerName = hostiles[Math.floor(Math.random() * hostiles.length)] + `-${Math.floor(Math.random() * 899 + 100)}`;
    const attacker = constructProceduralCreature(generateRandom64Sequence(), "Hostile Penetration Specimen");
    attacker.name = attackerName;
    attacker.vitality = Math.round(effDefender.vitality * (0.85 + Math.random() * 0.3));
    attacker.attack = Math.round(effDefender.attack * (0.8 + Math.random() * 0.3));
    attacker.defense = Math.round(effDefender.defense * (0.8 + Math.random() * 0.3));

    let attackerHp = attacker.vitality;
    let defenderHp = effDefender.vitality;
    const combatLogs: string[] = [];

    combatLogs.push(`[${timestamp}] * Incoming node stability breach detected from hostile node sweep.`);
    combatLogs.push(`[${timestamp}] * Hostile network intruder sequence: "${attacker.name}" (${attacker.vitality} HP)`);
    combatLogs.push(`[${timestamp}] * Deployed Node Defender: "${defender.name}" (${effDefender.vitality} HP)`);

    let round = 1;
    const usedMoves: string[] = [];

    // Anomalous benefits calculations
    const attackerBenefits = getAnomalousBenefits(attacker.sequence);
    const defenderBenefits = getAnomalousBenefits(defender.sequence);

    const hasDoubleStrikeAtk = attackerBenefits.some(b => b.id === 'DOUBLE_STRIKE');
    const hasSelfDestructAtk = attackerBenefits.some(b => b.id === 'SELF_DESTRUCT');
    const hasSiphonAtk = attackerBenefits.some(b => b.id === 'HEALTH_REGEN');
    const hasPhaseShiftAtk = attackerBenefits.some(b => b.id === 'PHASE_SHIFT');

    const hasDoubleStrikeDef = defenderBenefits.some(b => b.id === 'DOUBLE_STRIKE');
    const hasSelfDestructDef = defenderBenefits.some(b => b.id === 'SELF_DESTRUCT');
    const hasSiphonDef = defenderBenefits.some(b => b.id === 'HEALTH_REGEN');
    const hasPhaseShiftDef = defenderBenefits.some(b => b.id === 'PHASE_SHIFT');

    while (attackerHp > 0 && defenderHp > 0 && round < 30) {
      let attDmg = Math.max(8, Math.floor(attacker.attack * (0.8 + Math.random() * 0.4)) - Math.floor(effDefender.defense * 0.22));
      let defDmg = Math.max(8, Math.floor(effDefender.attack * (0.8 + Math.random() * 0.4)) - Math.floor(attacker.defense * 0.22));

      let specTriggered = "";

      // special moves
      const moves = getUnlockedMoves(defender.sequence);
      if (moves.length > 0 && Math.random() < 0.35) {
        const unUsed = moves.filter(mv => !usedMoves.includes(mv.name));
        if (unUsed.length > 0) {
          const mvObj = unUsed[0];
          usedMoves.push(mvObj.name);
          if (mvObj.type === 'healing') {
            defenderHp = Math.min(effDefender.vitality, defenderHp + 35);
            defDmg += 35;
            specTriggered = `🛡️ DEFENDER SPECIAL [${mvObj.name}]: Cellular repair protocol active. Restored 35 HP.`;
          } else {
            attDmg = 0;
            specTriggered = `🛡️ DEFENDER SPECIAL [${mvObj.name}]: Anti-phage firewall. Deflected attacker damage.`;
          }
        }
      }

      // Apply Anomalous special benefits on each combat round
      if (hasDoubleStrikeAtk) {
        attDmg = Math.round(attDmg * 1.8);
        specTriggered += " [ANOMALY: Vortex Phase-Strike x1.8 Attack!]";
      }
      if (hasDoubleStrikeDef) {
        defDmg = Math.round(defDmg * 1.8);
        specTriggered += " [ANOMALY: Vortex Phase-Strike x1.8 Counter!]";
      }

      if (hasPhaseShiftAtk && Math.random() < 0.3) {
        defDmg = 0;
        specTriggered += " [ANOMALY: Temporal Mirror Shield Dodged Attack!]";
      }
      if (hasPhaseShiftDef && Math.random() < 0.3) {
        attDmg = 0;
        specTriggered += " [ANOMALY: Temporal Mirror Shield Opponent Dodged!]";
      }

      if (hasSiphonAtk) {
        attackerHp = Math.min(attacker.vitality, attackerHp + 15);
        specTriggered += " [ANOMALY: Bio-Organic Siphon Regenerated +15 HP!]";
      }
      if (hasSiphonDef) {
        defenderHp = Math.min(effDefender.vitality, defenderHp + 15);
        specTriggered += " [ANOMALY: Bio-Organic Siphon Opponent Regenerated +15 HP!]";
      }

      attackerHp = Math.max(0, attackerHp - defDmg);
      if (attackerHp > 0) {
        defenderHp = Math.max(0, defenderHp - attDmg);
      }

      combatLogs.push(`[Round ${round}] Attacker dealt -${attDmg} HP. Defender "${defender.name}" counterstrike: -${defDmg} HP.`);
      if (specTriggered) {
        combatLogs.push(`   >> ${specTriggered}`);
      }
      round++;
    }

    let defenderWon = defenderHp > 0 && attackerHp <= 0;

    // Self-destruct triggers
    if (!defenderWon && defenderHp <= 0 && hasSelfDestructDef && attackerHp > 0) {
      combatLogs.push(`💥 ANOMALOUS OVERDRIVE [Supernova Reverb] triggered! Your defender self-destructed and vaporized intruder ${attacker.name}!`);
      attackerHp = 0;
      defenderWon = true; // Win on self-destruct tie defense to protect unit!
    } else if (defenderWon && attackerHp <= 0 && hasSelfDestructAtk && defenderHp > 0) {
      combatLogs.push(`💥 Intruder anomaly triggered [Supernova Reverb]! Self-destructed and crushed your defender ${defender.name}!`);
      defenderHp = 0;
      defenderWon = false;
    }

    let outcomeText = "";
    let lostGene = "";
    if (defenderWon) {
      outcomeText = `[${timestamp}] >>> BREACH DEFLECTED: Defender "${defender.name}" successfully neutralized the intruder vector!`;
      setNodeStability(prev => Math.min(100, prev + defender.defense / 10));
    } else {
      outcomeText = `[${timestamp}] >>> NODE SECURITY COMPROMISED: Defender "${defender.name}" was defeated and suffered telomere damage.`;
      decreaseTelomeres(defender.id, 25, "defeat in defensive shield combat");
      
      const ownedOnes = sequences.filter(s => s.count > 0);
      if (ownedOnes.length > 0) {
        const selection = ownedOnes[Math.floor(Math.random() * ownedOnes.length)];
        lostGene = selection.sequence;
        
        setSequences(sPrev => {
          return sPrev.map(s => {
            if (s.sequence === lostGene) {
              return { ...s, count: Math.max(0, s.count - 1) };
            }
            return s;
          }).filter(s => s.count > 0 || ['AGTCGTAC', 'CCCGGGAA', 'TTTAAACG', 'AACCGGTT'].includes(s.sequence));
        });
        
        combatLogs.push(`WARNING: Node breached! Intruders successfully extracted gene block ${lostGene}.`);
      }
    }

    const lostGeneArray = lostGene ? [lostGene] : [];

    setGenMails(mPrev => {
      const mailId = 'MAIL-DEFENSE-LOG';
      const existingMailIdx = mPrev.findIndex(m => m.id === mailId);
      const appends = [
        `\n--- NODE STABILITY REPORT AT ${timestamp} ---`,
        ...combatLogs,
        outcomeText,
        `[NODE SECURITY MONITOR] Shield tracking stable. Next security query interval standby.`
      ];

      // Set expiration to 1 day after now!
      const currentExpiresAt = Date.now() + 24 * 60 * 60 * 1000;

      if (existingMailIdx >= 0) {
        const updatedMail = { ...mPrev[existingMailIdx] };
        if (updatedMail.autoHackLog) {
          updatedMail.tagline = defenderWon
            ? `[SHIELD SAFE] G.E.N. Defender Logs - "${defender.name}" (BREACH SOLVED)`
            : `[BREACH WARNING] G.E.N. Defender Logs - "${defender.name}" (COMPROMISED)`;
          updatedMail.isRead = false;
          updatedMail.expiresAt = currentExpiresAt; // refresh lifetime
          updatedMail.autoHackLog = {
            ...updatedMail.autoHackLog,
            creature: defender, // refresh defender representation if changed
            log: trimInnerBattleLogs([...updatedMail.autoHackLog.log, ...appends]),
            genesLost: [...(updatedMail.autoHackLog.genesLost || []), ...lostGeneArray]
          };
        }
        const updatedMails = [...mPrev];
        updatedMails[existingMailIdx] = updatedMail;
        return updatedMails;
      } else {
        const newMail: GenMail = {
          id: mailId,
          date: new Date().toISOString().split("T")[0],
          tagline: defenderWon
            ? `[SHIELD SAFE] G.E.N. Defender Logs - "${defender.name}" (BREACH SOLVED)`
            : `[BREACH WARNING] G.E.N. Defender Logs - "${defender.name}" (COMPROMISED)`,
          isRead: false,
          sequences: [],
          type: 'defense_log',
          createdAt: Date.now(),
          expiresAt: currentExpiresAt,
          autoHackLog: {
            creature: defender,
            log: trimInnerBattleLogs([
              `[G.E.N. DEFENDER INTEGRITY RECORDER] System defense tracking logs. Recording active shield countermeasure outcomes.`,
              ...appends
            ]),
            genesLost: lostGeneArray
          }
        };
        return [newMail, ...mPrev];
      }
    });

    triggerLog(`Defender "${defender.name}" completed combat defense: ${defenderWon ? "Breach Deflected" : "System Compromised"}.`, defenderWon ? "success" : "warn");
  };

  // Trigger attacks from the Sync-Net against the player periodically
  useEffect(() => {
    if (!isPowered) return;
    const combatSimulationInterval = setInterval(() => {
      // At max stability, no hacks are made; at max emissivity, chance of a hack is doubled (0.40 * 2 = 0.80)
      if (nodeStability >= 100) return;
      const hackChance = nodeEmissivity >= 100 ? 0.80 : 0.40;
      if (Math.random() < hackChance) {
        runSimulatedIncomingAttacks();
      }
    }, 45000); // Check every 45s for high dynamic interactivity

    return () => clearInterval(combatSimulationInterval);
  }, [isPowered, creatures, defenderCreatureId, nodeStability, nodeEmissivity]);

  // Automatic G.E.N. Network email auto-expiration & dispatcher deduplicator
  useEffect(() => {
    if (!isPowered) return;
    const mailCleanupInterval = setInterval(() => {
      setGenMails(prev => {
        const now = Date.now();
        // 1. FILTER OUT EXPIRED EMAILS (time-passed trade requests, auto-hacks, defense logs)
        let filtered = prev.filter(mail => {
          if (mail.expiresAt && now > mail.expiresAt) {
            return false; // delete expired
          }
          return true;
        });

        // 2. DEDUPLICATE DISPATCH EMAILS (keep only the newest dispatch email)
        const dispatchMails = filtered.filter(m => m.type === 'dispatch' || (!m.type && m.sequences && m.sequences.length > 0));
        if (dispatchMails.length > 1) {
          // Find the newest dispatch (first one in the array, since we prepend)
          const newestDispatchId = dispatchMails[0].id;
          filtered = filtered.filter(m => {
            const isDispatch = m.type === 'dispatch' || (!m.type && m.sequences && m.sequences.length > 0);
            if (isDispatch) {
              return m.id === newestDispatchId;
            }
            return true;
          });
        }

        // Return updated mails if there were any deletions
        if (filtered.length !== prev.length) {
          return filtered;
        }
        return prev;
      });
    }, 5000); // Check every 5 seconds for fast response

    return () => clearInterval(mailCleanupInterval);
  }, [isPowered]);

  // Initiate trade outwards
  const handleInitiateTradeRequest = (targetUser: NearbyUser) => {
    sound.playBeep(720, 0.1, "sine");
    setActiveTrade({
      id: `TR-${Math.floor(Math.random() * 8999 + 1000)}`,
      partner: targetUser,
      status: 'requested_by_me',
      partnerOfferedCreature: targetUser.creaturesAvailable[0]
    });
    triggerLog(`Transmitting invitation telemetry packets to Node "${targetUser.uid}"`, "info");

    setTimeout(() => {
      setActiveTrade((prev) => {
        if (!prev) return null;
        if (Math.random() < 0.88) {
          sound.playSynthesisSuccess();
          triggerLog("CONNECTION ESTABLISHED: Node transfer channel secured. Host agreed.", "success");
          return { ...prev, status: 'accepted' };
        } else {
          sound.playReject();
          triggerLog("LINK REJECTED: Host node busy or out of range.", "warn");
          return { ...prev, status: 'rejected' };
        }
      });
    }, 2000);
  };

  const handleAcceptIncomingTrade = () => {
    if (!activeTrade) return;
    sound.playSynthesisSuccess();
    setActiveTrade((prev) => prev ? { ...prev, status: 'accepted' } : null);
    triggerLog("Incoming trade matched: Select code creature to dispatch", "success");
  };

  const handleRejectIncomingTrade = () => {
    sound.playReject();
    setActiveTrade(null);
    triggerLog("Refused peer sync invite. Connection severed.", "warn");
  };

  // Trade Interceptor Hack Trigger (Bypasses selection, auto-deploys designated defender, defender strikes first)
  const handleInitiateTradeInterceptorHack = (targetUser: NearbyUser, overrideDefender?: Creature) => {
    sound.playBeep(220, 0.3, "sawtooth");
    
    // Auto deploy attacker (Do not let player choose!)
    const chosenAttacker = creatures.find(c => c.id === defenderCreatureId) || creatures[0];
    const attacker = chosenAttacker || constructProceduralCreature("AGTCGTACAAACCCGGGAAATTTTAAACGAAACCCGGGAAAGGGCCCTTTAAACGAAACCCGGG", "Provisional Hack Driver");

    const defenderCandidates = targetUser.creaturesAvailable;
    const defender = overrideDefender || defenderCandidates[Math.floor(Math.random() * defenderCandidates.length)];

    setActiveHack({
      id: `HK-INT-${Math.floor(Math.random() * 8999 + 1000)}`,
      partner: targetUser,
      status: 'battling', // Directly bypass selections
      attacker,
      defender,
      attackerCurrentHp: attacker.vitality,
      defenderCurrentHp: defender.vitality,
      isTradeInterceptor: true,
      battleLogs: [
        `[${new Date().toTimeString().split(" ")[0]}] >> SYNC PORT COMPROMISED! Link Intercept launched.`,
        `[${new Date().toTimeString().split(" ")[0]}] >> DIRECT COUNTER-HACK ENGAGED. Attacker bypasses selection buffer.`,
        `[${new Date().toTimeString().split(" ")[0]}] >> AUTO-DEPLOYED Attacker: "${attacker.name}" (${attacker.vitality} HP).`,
        `[${new Date().toTimeString().split(" ")[0]}] >> TARGET Defender Node Host: "${defender.name}" (${defender.vitality} HP).`,
        `[${new Date().toTimeString().split(" ")[0]}] >> WARNING: Defender gains first-strike advantage. Prepare system buffers!`
      ],
      winnerUid: null
    });

    setStats((prev) => ({
      ...prev,
      interceptorHacksInitiated: (prev.interceptorHacksInitiated || 0) + 1,
      totalHacksInitiated: (prev.totalHacksInitiated || 0) + 1,
    }));

    // Reset active trade state to seamlessly pivot to combat view
    setActiveTrade(null);
    setActiveTab('transceiver');
    triggerLog(`SYNC PORT INTERCEPT: Cybernetic counter-hack running with Node ${targetUser.uid}.`, "warn");
  };

  // Hack-Battle Action Handlers
  const handleInitiateHack = (targetUser: NearbyUser) => {
    sound.playBeep(320, 0.15, "sawtooth");
    
    if (creatures.length === 0) {
      triggerLog("CONNECTION ABORTED: No compiled specimens in database to initiate attack vector!", "warn");
      return;
    }

    // Set defender creature from partner creatures:
    // Check if targetUser has any creature marked as defender (since simulated, none are by default, so we pick randomly)
    // "assuming no creature is set as a defender, randomly select one"
    const defenderCandidates = targetUser.creaturesAvailable;
    const defender = defenderCandidates[Math.floor(Math.random() * defenderCandidates.length)];

    setActiveHack({
      id: `HK-${Math.floor(Math.random() * 8999 + 1000)}`,
      partner: targetUser,
      status: 'selecting_attacker',
      defender,
      attackerCurrentHp: 100,
      defenderCurrentHp: 100,
      battleLogs: [
        `[${new Date().toTimeString().split(" ")[0]}] >> back-door exploit injected successfully.`,
        `[${new Date().toTimeString().split(" ")[0]}] >> Target passive guard specimen located: "${defender.name}" (${defender.vitality} HP).`,
        `[${new Date().toTimeString().split(" ")[0]}] >> Awaiting attacker sequence designation...`
      ],
      winnerUid: null
    });

    setStats((prev) => ({
      ...prev,
      totalHacksInitiated: (prev.totalHacksInitiated || 0) + 1
    }));

    triggerLog(`Malicious back-door socket opened at peer host ${targetUser.uid}.`, "warn");
  };

  const handleSelectAttackerForHack = (selectedAttacker: Creature) => {
    if (!activeHack) return;
    sound.playBeep(520, 0.1, "sine");

    setActiveHack(prev => {
      if (!prev) return null;
      return {
        ...prev,
        attacker: selectedAttacker,
        attackerCurrentHp: selectedAttacker.vitality,
        defenderCurrentHp: prev.defender?.vitality || 100,
        status: 'battling',
        battleLogs: [
          ...prev.battleLogs,
          `[${new Date().toTimeString().split(" ")[0]}] >> Uploading local attacker: "${selectedAttacker.name}" (${selectedAttacker.vitality} HP).`,
          `[${new Date().toTimeString().split(" ")[0]}] >> INTRUSION BATTLE INITIATED! Awaiting action packet launch...`
        ]
      };
    });

    triggerLog(`Deployed local cybernetic attacker "${selectedAttacker.name}" to breach peer defense.`, "success");
  };

  const handleRunHackRound = (specialMove?: { type: 'healing' | 'evasive'; name: string }) => {
    if (!activeHack || !activeHack.attacker || !activeHack.defender) return;

    sound.playBeep(640, 0.1, "square");
    sound.playBeep(400, 0.05, "sawtooth");

    const attacker = activeHack.attacker;
    const defender = activeHack.defender;

    const effAttacker = getEffectiveStats(attacker);
    const effDefender = getEffectiveStats(defender);

    const attackerBenefits = getAnomalousBenefits(attacker.sequence);
    const defenderBenefits = getAnomalousBenefits(defender.sequence);

    const hasDoubleStrikeAtk = attackerBenefits.some(b => b.id === 'DOUBLE_STRIKE');
    const hasSelfDestructAtk = attackerBenefits.some(b => b.id === 'SELF_DESTRUCT');
    const hasSiphonAtk = attackerBenefits.some(b => b.id === 'HEALTH_REGEN');
    const hasPhaseShiftAtk = attackerBenefits.some(b => b.id === 'PHASE_SHIFT');

    const hasDoubleStrikeDef = defenderBenefits.some(b => b.id === 'DOUBLE_STRIKE');
    const hasSelfDestructDef = defenderBenefits.some(b => b.id === 'SELF_DESTRUCT');
    const hasSiphonDef = defenderBenefits.some(b => b.id === 'HEALTH_REGEN');
    const hasPhaseShiftDef = defenderBenefits.some(b => b.id === 'PHASE_SHIFT');

    // Damage calculations based on aggressivity and block shells:
    let baseAtkDamage = Math.floor(effAttacker.attack * (0.85 + Math.random() * 0.3));
    if (hasDoubleStrikeAtk) {
      baseAtkDamage = Math.round(baseAtkDamage * 1.8);
    }
    const defBlock = Math.floor(effDefender.defense * 0.25);
    let dmgToDefender = Math.max(8, baseAtkDamage - defBlock);

    // Defender retaliates
    let baseDefDamage = Math.floor(effDefender.attack * (0.85 + Math.random() * 0.3));
    if (hasDoubleStrikeDef) {
      baseDefDamage = Math.round(baseDefDamage * 1.8);
    }
    const atkBlock = Math.floor(effAttacker.defense * 0.25);
    let dmgToAttacker = Math.max(8, baseDefDamage - atkBlock);

    if (hasPhaseShiftAtk && Math.random() < 0.3) {
      dmgToAttacker = 0;
    }
    if (hasPhaseShiftDef && Math.random() < 0.3) {
      dmgToDefender = 0;
    }

    // Apply special moves modifiers!
    let specialHealAmount = 0;
    if (specialMove) {
      if (specialMove.type === 'healing') {
        specialHealAmount = 35;
        // theft: siphons HP (takes additional 35 damage)
        dmgToDefender += 35;
      } else if (specialMove.type === 'evasive') {
        // automatic combat damage negation if used
        dmgToAttacker = 0;
      }
    }

    setActiveHack(prev => {
      if (!prev || !prev.attacker || !prev.defender) return null;

      const timestamp = new Date().toTimeString().split(" ")[0];
      const newLogs = [...prev.battleLogs];

      if (hasDoubleStrikeAtk) {
        newLogs.push(`[${timestamp}] 🌀 ANOMALY TRIGGERED [Vortex Phase-Strike] deals massive double strike damage!`);
      }
      if (hasDoubleStrikeDef) {
        newLogs.push(`[${timestamp}] 🌀 TARGET ANOMALY TRIGGERED [Vortex Phase-Strike] dealing counter double-strike payload!`);
      }
      if (hasPhaseShiftAtk && dmgToAttacker === 0 && baseDefDamage > 0) {
        newLogs.push(`[${timestamp}] 🌠 ANOMALY ACCELERATED [Temporal Mirror Shield] shifted phase, dodging counter strike entirely!`);
      }
      if (hasPhaseShiftDef && dmgToDefender === 0 && baseAtkDamage > 0) {
        newLogs.push(`[${timestamp}] 🌠 TARGET DEFLECTION [Temporal Mirror Shield] shifted target past your strike!`);
      }

      let nextAtkHp = prev.attackerCurrentHp;
      if (hasSiphonAtk) {
        nextAtkHp = Math.min(effAttacker.vitality, nextAtkHp + 15);
        newLogs.push(`[${timestamp}] 🧬 ANOMALOUS BIO-REGEN [Bio-Organic Siphon] siphoned +15 HP from defender.`);
      }

      let nextDefHp = prev.defenderCurrentHp;
      if (hasSiphonDef) {
        nextDefHp = Math.min(effDefender.vitality, nextDefHp + 15);
        newLogs.push(`[${timestamp}] 🧬 TARGET REGEN PROTOCOL [Bio-Organic Siphon] siphoned +15 HP from your unit.`);
      }
      let nextStatus = prev.status;
      let winnerUid = prev.winnerUid;
      const usedMoves = prev.usedSpecialMoves ? [...prev.usedSpecialMoves] : [];

      let localSnatchedGene = prev.harvestedGene;
      let localExtraGenes = prev.harvestedExtraGenes ? [...prev.harvestedExtraGenes] : [];

      if (specialMove) {
        usedMoves.push(specialMove.name);
        if (specialMove.type === 'healing') {
          newLogs.push(`[${timestamp}] >> TRG: [${specialMove.name}]! Siphoned 35 HP from defender and mended attacker.`);
        } else if (specialMove.type === 'evasive') {
          newLogs.push(`[${timestamp}] >> TRG: [${specialMove.name}]! Escape deviation shift. Defender strike NEGATED.`);
        }
      }

      if (specialHealAmount > 0) {
        nextAtkHp = Math.min(effAttacker.vitality, nextAtkHp + specialHealAmount);
      }

      if (prev.isTradeInterceptor) {
        // Defender responds FIRST!
        nextAtkHp = Math.max(0, nextAtkHp - dmgToAttacker);
        newLogs.push(`[${timestamp}] * PEER INITIAL STRIKE: "${defender.name}" fired ${defender.primaryWeapon} dealing -${dmgToAttacker} dmg (Blocked: ${atkBlock}).`);
        
        if (nextAtkHp <= 0) {
          // Attacker neutralized before they could retaliate!
          nextStatus = 'completed';
          winnerUid = prev.partner.uid;
          newLogs.push(`[${timestamp}] >>> BREACH COLLAPSED: Your attacking cyborg was neutralized before retaliating!`);
          newLogs.push(`[${timestamp}] >>> Interceptor breach defeated.`);
          
          decreaseTelomeres(attacker.id, 25, "defeat in intercept hacking combat");
          setStats(sPrev => ({
            ...sPrev,
            interceptorHacksLost: (sPrev.interceptorHacksLost || 0) + 1,
            totalHacksLost: (sPrev.totalHacksLost || 0) + 1
          }));
          sound.playReject();
          triggerLog(`BREACH REFUSED: Firewall neutralized your specimen before retaliation!`, "warn");
        } else {
          // Attacker survives and strikes back!
          nextDefHp = Math.max(0, nextDefHp - dmgToDefender);
          newLogs.push(`[${timestamp}] * YOUR RETALIATION: "${attacker.name}" responded with ${attacker.primaryWeapon} dealing -${dmgToDefender} dmg (Blocked: ${defBlock}).`);
          
          if (nextDefHp <= 0) {
            // Player wins!
            nextStatus = 'completed';
            winnerUid = 'PLAYER';
            
            // Winning standard gene
            const randomGeneIndex = Math.floor(Math.random() * 8);
            const snatchedGene = defender.sequence.slice(randomGeneIndex * 8, (randomGeneIndex + 1) * 8);
            localSnatchedGene = snatchedGene;
            
            // Appended genes autowin!
            const extraGenes: string[] = [];
            if (defender.sequence.length > 64) {
              const extraSec = defender.sequence.slice(64);
              for (let idx = 0; idx < extraSec.length; idx += 8) {
                extraGenes.push(extraSec.slice(idx, idx + 8));
              }
            }
            localExtraGenes = extraGenes;

            setSequences(sPrev => {
              let updated = [...sPrev];
              // Add standard snatched
              const matching = updated.findIndex(s => s.sequence === snatchedGene);
              if (matching >= 0) {
                updated[matching] = { ...updated[matching], count: updated[matching].count + 1 };
              } else {
                updated.push({ sequence: snatchedGene, count: 1, discoveredAt: Date.now() });
              }

              // Add extra genes
              extraGenes.forEach(ec => {
                const ecIdx = updated.findIndex(s => s.sequence === ec);
                if (ecIdx >= 0) {
                  updated[ecIdx] = { ...updated[ecIdx], count: updated[ecIdx].count + 1 };
                } else {
                  updated.push({ sequence: ec, count: 1, discoveredAt: Date.now() });
                }
              });

              return updated;
            });

            newLogs.push(`[${timestamp}] >>> HIGH-LEVEL SECURITY BREACH COMPLETE!`);
            newLogs.push(`[${timestamp}] >>> Snatched defensive gene "${snatchedGene}" into system vault!`);
            if (extraGenes.length > 0) {
              newLogs.push(`[${timestamp}] >>> AUTOMATIC APPRENTICE WIN: Snatched ${extraGenes.length} appended genes: [${extraGenes.join(", ")}]!`);
            }

            setStats(sPrev => ({
              ...sPrev,
              interceptorHacksWon: (sPrev.interceptorHacksWon || 0) + 1,
              totalHacksWon: (sPrev.totalHacksWon || 0) + 1,
              totalGenesAcquired: sPrev.totalGenesAcquired + 1 + extraGenes.length
            }));
            setNodeStability(prev => Math.min(100, prev + attacker.defense / 10));
            sound.playSynthesisSuccess();
            triggerLog(`INTERCEPT WIN: Snatched genes from peer.`, "success");
          }
        }
      } else {
        // Normal simultaneous turn
        nextDefHp = Math.max(0, nextDefHp - dmgToDefender);
        nextAtkHp = Math.max(0, nextAtkHp - dmgToAttacker);
        
        newLogs.push(`[${timestamp}] * YOUR IMPACT: "${attacker.name}" fired ${attacker.primaryWeapon} dealing -${dmgToDefender} dmg (Blocked: ${defBlock}).`);
        newLogs.push(`[${timestamp}] * PEER COUNTER: "${defender.name}" responded with ${defender.primaryWeapon} dealing -${dmgToAttacker} dmg (Blocked: ${atkBlock}).`);

        if (nextAtkHp <= 0 && nextDefHp > 0 && hasSelfDestructAtk) {
          nextDefHp = 0;
          newLogs.push(`[${timestamp}] 💥 ANOMALOUS OVERDRIVE [Supernova Reverb] triggered! Your cyborg self-destructed and vaporized ${defender.name}!`);
        } else if (nextDefHp <= 0 && nextAtkHp > 0 && hasSelfDestructDef) {
          nextAtkHp = 0;
          newLogs.push(`[${timestamp}] 💥 Hostile specimen anomaly triggered [Supernova Reverb]! Self-destructed and crushed your cyborg!`);
        }

        if (nextDefHp <= 0 && nextAtkHp > 0) {
          nextStatus = 'completed';
          winnerUid = 'PLAYER';
          const randomGeneIndex = Math.floor(Math.random() * 8);
          const snatchedGene = defender.sequence.slice(randomGeneIndex * 8, (randomGeneIndex + 1) * 8);
          localSnatchedGene = snatchedGene;
          
          const extraGenes: string[] = [];
          if (defender.sequence.length > 64) {
            const extraSec = defender.sequence.slice(64);
            for (let idx = 0; idx < extraSec.length; idx += 8) {
              extraGenes.push(extraSec.slice(idx, idx + 8));
            }
          }
          localExtraGenes = extraGenes;

          setSequences(sPrev => {
            let updated = [...sPrev];
            const matching = updated.findIndex(s => s.sequence === snatchedGene);
            if (matching >= 0) {
              updated[matching] = { ...updated[matching], count: updated[matching].count + 1 };
            } else {
              updated.push({ sequence: snatchedGene, count: 1, discoveredAt: Date.now() });
            }

            extraGenes.forEach(ec => {
              const ecIdx = updated.findIndex(s => s.sequence === ec);
              if (ecIdx >= 0) {
                updated[ecIdx] = { ...updated[ecIdx], count: updated[ecIdx].count + 1 };
              } else {
                updated.push({ sequence: ec, count: 1, discoveredAt: Date.now() });
              }
            });

            return updated;
          });

          newLogs.push(`[${timestamp}] >>> HIGH-LEVEL SECURITY BREACH COMPLETE!`);
          newLogs.push(`[${timestamp}] >>> Host computer compromised. Harvested gene "${snatchedGene}" into data bank!`);
          if (extraGenes.length > 0) {
            newLogs.push(`[${timestamp}] >>> AUTOMATIC APPRENTICE WIN: Recovered ${extraGenes.length} appended genes: [${extraGenes.join(", ")}]!`);
          }

          setStats(sPrev => ({
            ...sPrev,
            totalHacksWon: (sPrev.totalHacksWon || 0) + 1,
            totalGenesAcquired: sPrev.totalGenesAcquired + 1 + extraGenes.length
          }));
          setNodeEmissivity(prev => Math.min(100, prev + (attacker.attack / 10)));
          sound.playSynthesisSuccess();
          triggerLog(`CONGRATULATIONS: Hack breach complete!`, "success");
        } else if (nextAtkHp <= 0 && nextDefHp > 0) {
          nextStatus = 'completed';
          winnerUid = prev.partner.uid;
          newLogs.push(`[${timestamp}] >>> ACTION PACKET DEFLECTED! Attack vector incinerated.`);
          newLogs.push(`[${timestamp}] >>> Threat neutralized by host firewall daemon.`);

          decreaseTelomeres(attacker.id, 25, "defeat in manual hacking combat");
          setStats(sPrev => ({
            ...sPrev,
            totalHacksLost: (sPrev.totalHacksLost || 0) + 1
          }));
          sound.playReject();
          triggerLog(`BREACH REFUSED: Firewall expelled your attacking mutant specimen!`, "warn");
        } else if (nextAtkHp <= 0 && nextDefHp <= 0) {
          nextStatus = 'completed';
          winnerUid = 'TIE';
          newLogs.push(`[${timestamp}] >>> DOUBLE FATAL FAULT: Both specimens collapsed synchronously.`);
          
          decreaseTelomeres(attacker.id, 25, "defeat in manual hacking combat");
          setStats(sPrev => ({
            ...sPrev,
            totalHacksLost: (sPrev.totalHacksLost || 0) + 1
          }));
          sound.playReject();
        }
      }

      return {
        ...prev,
        defenderCurrentHp: nextDefHp,
        attackerCurrentHp: nextAtkHp,
        battleLogs: newLogs,
        status: nextStatus,
        winnerUid,
        usedSpecialMoves: usedMoves,
        harvestedGene: localSnatchedGene,
        harvestedExtraGenes: localExtraGenes
      };
    });
  };

  const handleClearHackSession = () => {
    if (activeHack?.openedFromInbox) {
      setActiveTab('inbox');
    }
    setActiveHack(null);
  };

  // Complete Swap Swap
  const handleExecuteTrade = (selectedCreatureToOffer: Creature) => {
    if (!activeTrade || !activeTrade.partnerOfferedCreature) return;

    sound.playTradeSuccess();
    
    setCreatures((prev) => {
      // Remove traded out creature
      const filtered = prev.filter(c => c.id !== selectedCreatureToOffer.id);
      
      // Setup incoming creature from partner
      const incoming: Creature = {
        ...activeTrade.partnerOfferedCreature!,
        id: `PX-${Math.random().toString(36).substr(2, 9).toUpperCase()}`,
        origin: `Traded from ${activeTrade.partner.uid}`,
        discoveredAt: Date.now()
      };

      return [incoming, ...filtered];
    });

    if (defenderCreatureId === selectedCreatureToOffer.id) {
      setDefenderCreatureId(null);
    }

    setStats((prev) => ({
      ...prev,
      totalTradesCompleted: prev.totalTradesCompleted + 1
    }));

    setActiveTrade((prev) => prev ? { ...prev, status: 'completed', playerOfferedCreature: selectedCreatureToOffer } : null);
    triggerLog(`TRADE EXCHANGED COMPLETED. MUTATION SYNC SUCCESS!`, "success");
  };

  const handleClearTradeSession = () => {
    setActiveTrade(null);
    setTradeSelectedCreature(null);
    setIsTradeArchiveOpened(false);
    setTradeArchiveSearch("");
    setTradeArchiveFaction("ALL");
    setTradeArchiveSort("name-asc");
  }

  const handleAcceptInboxTrade = (mailId: string, offerToGive: Creature) => {
    setGenMails(prev => prev.map(m => {
      if (m.id === mailId && m.tradeRequest) {
        // Remove traded out creature and add incoming creature
        setCreatures(cPrev => {
          const filtered = cPrev.filter(c => c.id !== offerToGive.id);
          const incoming: Creature = {
            ...m.tradeRequest!.partnerOfferedCreature,
            id: `PX-${Math.random().toString(36).substr(2, 9).toUpperCase()}`,
            origin: `Traded from ${m.tradeRequest!.partner.uid}`,
            discoveredAt: Date.now()
          };
          return [incoming, ...filtered];
        });

        if (defenderCreatureId === offerToGive.id) {
          setDefenderCreatureId(null);
        }

        // 45% chance that a trade hack intercept triggers!
        if (Math.random() < 0.45) {
          const tr = m.tradeRequest!;
          const playerDefender = creatures.find(c => c.id === defenderCreatureId) || creatures.find(c => c.id !== offerToGive.id) || offerToGive;
          
          setTimeout(() => {
            setActiveHack({
              id: `HK-${Math.floor(Math.random() * 89999 + 10000)}`,
              partner: tr.partner,
              status: 'battling',
              attacker: playerDefender, // Our defender acts as the attacker in the stat/variable schema
              defender: tr.partnerOfferedCreature, // Peer's creature acts as the defender in the stat/variable schema
              attackerCurrentHp: playerDefender.vitality,
              defenderCurrentHp: tr.partnerOfferedCreature.vitality,
              battleLogs: [
                 `[G.E.N. INTERCEPT ALERT] Security firewall breach detected during Node sync!`,
                 `[SYSTEM] Cyber-breach initiated by partner Node "${tr.partner.uid}".`,
                 `[SYSTEM] Designated Defender loaded: "${playerDefender.name}".`,
                 `[SYSTEM] Automated Attacker loaded: "${tr.partnerOfferedCreature.name}".`,
                 `[ALERT] Local defender initiated counter-beams, RESPONDING FIRST!`
              ],
              winnerUid: null,
              isTradeInterceptor: true,
              openedFromInbox: true
            });
            setActiveTab('transceiver');
            triggerLog(`SECURITY BREACH WARNING: Trade synchronization triggered active intercept hack. Countermeasures activated.`, "warn");
          }, 600);
        }

        setStats(sPrev => ({
          ...sPrev,
          totalTradesCompleted: (sPrev.totalTradesCompleted || 0) + 1,
          genNetworkTradesExchanged: (sPrev.genNetworkTradesExchanged || 0) + 1
        }));

        sound.playSynthesisSuccess();
        triggerLog(`NODE TRANSFER COMPLETED: Successfully sync-exchanged with "${m.tradeRequest!.partner.uid}".`, "success");

        return {
          ...m,
          tagline: `[COMPLETED] Transfer with "${m.tradeRequest!.partner.uid}" finalized`,
          tradeRequest: {
            ...m.tradeRequest!,
            status: 'accepted' as const,
            selectedCreatureToOffer: offerToGive
          }
        };
      }
      return m;
    }));
  };

  const handleDeclineInboxTrade = (mailId: string) => {
    setGenMails(prev => prev.map(m => {
      if (m.id === mailId && m.tradeRequest) {
        sound.playReject();
        triggerLog(`Node transfer request declined. Transceiver sequence connection closed.`, "info");
        return {
          ...m,
          tagline: `[DECLINED] Transfer request from "${m.tradeRequest!.partner.uid}" declined`,
          tradeRequest: {
            ...m.tradeRequest!,
            status: 'declined' as const
          }
        };
      }
      return m;
    }));
  };

  const handleDeleteCreature = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    sound.playBeep(220, 0.15, "sawtooth");
    setCreatures((prev) => prev.filter((c) => c.id !== id));
    if (inspectedCreatureId === id) {
      setInspectedCreatureId(null);
    }
    if (defenderCreatureId === id) {
      setDefenderCreatureId(null);
    }
    triggerLog(`Purged creature data sequence ${id} from memory sectors`, "warn");
  };

  const toggleFavoriteCreature = (id: string, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    const bot = creatures.find(c => c.id === id);
    if (!bot) return;
    const isCurrentlyFav = bot.isFavorite;
    if (!isCurrentlyFav) {
      const currentFavsCount = creatures.filter(c => c.isFavorite).length;
      if (currentFavsCount >= 8) {
        sound.playBeep(220, 0.35, "sawtooth"); // error buzz sound
        triggerLog("FAVORITE ERROR: Maximum of 8 favorites allowed! Clear existing favorites first.", "warn");
        return;
      }
    }
    sound.playBeep(isCurrentlyFav ? 580 : 880, 0.08, "sine");
    setCreatures((prev) => prev.map((c) => {
      if (c.id === id) {
        return { ...c, isFavorite: !c.isFavorite };
      }
      return c;
    }));
    triggerLog(isCurrentlyFav ? `REMOVED "${bot.name}" FROM FAVORITES` : `ADDED "${bot.name}" TO FAVORITES`, "success");
  };

  const handleSpliceExtract = (creatureId: string, extractedGene: string) => {
    sound.playBeep(200, 0.4, "sawtooth");
    sound.playSynthesisSuccess();

    // Add extracted Gene to sequences inventory
    setSequences((prev) => {
      const matchIdx = prev.findIndex((s) => s.sequence === extractedGene);
      if (matchIdx >= 0) {
        const updated = [...prev];
        updated[matchIdx].count += 1;
        return updated;
      } else {
        return [...prev, { sequence: extractedGene, count: 1, discoveredAt: Date.now() }];
      }
    });

    // Destroy the creature
    setCreatures((prev) => prev.filter((c) => c.id !== creatureId));
    
    if (defenderCreatureId === creatureId) {
      setDefenderCreatureId(null);
    }

    // Track statistics increment
    setStats((prev) => ({
      ...prev,
      totalCreaturesSpliceHarvested: prev.totalCreaturesSpliceHarvested + 1,
      totalGenesAcquired: prev.totalGenesAcquired + 1
    }));

    // Reset detail inspector to null
    setInspectedCreatureId(null);
    setSelectedGeneIndex(null);

    triggerLog(`MUTATION COMPRESSION SUCCESS: Extracted gene ${extractedGene}. Specimen incinerated securely.`, 'success');
  };

  // Render sequence with base-by-base colors
  const renderColorCodedSequence = (seq: string) => {
    if (!seq) return null;
    return (
      <span className="break-all font-mono font-bold tracking-widest text-[9px] block leading-normal select-all">
        {seq.split("").map((char, index) => {
          let colorClass = "text-white";
          const upperChar = char.toUpperCase();
          if (upperChar === "G") colorClass = "text-red-500";
          else if (upperChar === "A") colorClass = "text-[#00FF41]";
          else if (upperChar === "T") colorClass = "text-blue-400";
          else if (upperChar === "C") colorClass = "text-yellow-400";
          return (
            <span key={index} className={colorClass}>
              {char}
            </span>
          );
        })}
      </span>
    );
  };

  // Render sequence organized in 8-gene blocks colored the same as on the creature card
  const render8BlockColorCodedSequence = (seq: string) => {
    if (!seq) return null;
    return (
      <span className="font-mono text-[9px] leading-tight break-all font-bold tracking-widest flex flex-wrap gap-1 select-all">
        {Array.from({ length: Math.ceil(seq.length / 8) }).map((_, i) => {
          const cSeq = seq.slice(i * 8, (i + 1) * 8);
          const isAnom = isAnomalousGene(cSeq);
          return (
            <span 
              key={i} 
              className={`px-[1px] ${
                isAnom 
                  ? 'text-purple-400 bg-purple-950/20 shadow-[0_0_8px_rgba(168,85,247,0.5)] border border-purple-500/30 px-1 py-0.5 rounded font-bold' 
                  : i % 4 === 0 ? 'text-[#00FF41]' :
                    i % 4 === 1 ? 'text-amber-400' :
                    i % 4 === 2 ? 'text-blue-400' : 'text-purple-400'
              }`}
              title={isAnom ? `Anomalous Gene Chunk ${i+1}` : `Gene ${i+1}`}
            >
              {cSeq}
            </span>
          );
        })}
      </span>
    );
  };

  // Helper to compute trading metrics for diagnostics
  const getTradeDiagnostics = () => {
    if (!activeTrade) {
      return { 
        targetSeq: "", 
        receiverTargetSeq: "", 
        matchCount: 0, 
        thisNodeMatches: 0, 
        peerMatches: 0, 
        requesterName: "", 
        requesteeName: "" 
      };
    }

    const isIncoming = activeTrade.status === 'incoming_request';
    
    // Initiator's target sequence
    const targetSeq = isIncoming
      ? (activeTrade.partnerOfferedCreature?.sequence || "AGTCGTACCCCGGMAATTTAAACGAGTCGTACCCCGGMAATTTAAACGTTTAAACGAGTCGTAC")
      : (splicerSlots.filter(Boolean).length > 0 ? splicerSlots.map(s => s || "AGTCGTAC").join("") : "AGTCGTACCCCGGMAATTTAAACGAGTCGTACCCCGGMAATTTAAACGTTTAAACGAGTCGTAC");

    // Receiver's target sequence
    const receiverTargetSeq = isIncoming
      ? (splicerSlots.filter(Boolean).length > 0 ? splicerSlots.map(s => s || "AGTCGTAC").join("") : "AGTCGTACCCCGGMAATTTAAACGAGTCGTACCCCGGMAATTTAAACGTTTAAACGAGTCGTAC")
      : (activeTrade.partner.targetSequence || "AGTCGTACCCCGGMAATTTAAACGAGTCGTACCCCGGMAATTTAAACGTTTAAACGAGTCGTAC");

    // "Change: 'You (Local Node)' to the initiator target sequence"
    const requesterName = isIncoming ? `${activeTrade.partner.uid} (Peer Node)` : targetSeq;
    const requesteeName = isIncoming ? targetSeq : `${activeTrade.partner.uid} (Peer Node)`;

    // Calculate matching sequences for "This Node"
    let thisNodeMatches = 0;
    const thisNodeTarget = isIncoming ? targetSeq : receiverTargetSeq;
    const thisNodeGenes: string[] = [];
    for (let i = 0; i < 8; i++) {
      const chunk = thisNodeTarget.slice(i * 8, (i + 1) * 8);
      if (chunk.length === 8) {
        thisNodeGenes.push(chunk);
      }
    }
    thisNodeGenes.forEach(tc => {
      const found = sequences.find(item => item.sequence === tc);
      if (found) {
        thisNodeMatches += found.count;
      }
    });

    // Calculate matching sequences for "Peer Node"
    let peerMatches = 0;
    const peerTarget = isIncoming ? receiverTargetSeq : targetSeq;
    const peerGenes: string[] = [];
    for (let i = 0; i < 8; i++) {
      const chunk = peerTarget.slice(i * 8, (i + 1) * 8);
      if (chunk.length === 8) {
        peerGenes.push(chunk);
      }
    }
    const partnerGenes: string[] = [];
    activeTrade.partner.creaturesAvailable.forEach(creature => {
      for (let i = 0; i < 8; i++) {
        const gene = creature.sequence.slice(i * 8, (i + 1) * 8);
        if (gene.length === 8) {
          partnerGenes.push(gene);
        }
      }
    });
    peerGenes.forEach(tc => {
      const matches = partnerGenes.filter(c => c === tc);
      peerMatches += matches.length;
    });

    return { 
      targetSeq, 
      receiverTargetSeq, 
      matchCount: thisNodeMatches, 
      thisNodeMatches, 
      peerMatches, 
      requesterName, 
      requesteeName 
    };
  };

  const tradeDiag = getTradeDiagnostics();

  const panelClassName = isMobileView 
    ? "w-full h-full bg-[#050505] text-[#00FF41] font-mono flex flex-col overflow-y-auto relative" 
    : viewportProfile === "fold_inner"
      ? "w-full h-full bg-[#050505] text-[#00FF41] font-mono flex flex-col md:flex-row overflow-hidden relative"
      : "w-[1020px] max-w-full h-auto md:h-[660px] bg-[#050505] text-[#00FF41] font-mono flex flex-col md:flex-row overflow-hidden border-4 md:border-8 border-neutral-900 rounded shadow-2xl relative";

  return (
    <div 
      id="pox_sleek_panel" 
      className={panelClassName}
    >
      
      {/* 1. Sidebar Navigation (Sleek Theme design patterns) */}
      <nav className="w-full md:w-52 bg-neutral-900/50 border-b md:border-b-0 md:border-r border-green-900/30 flex flex-col p-3 md:p-4 justify-between gap-3 md:gap-4 shrink-0">
        
        <div className="space-y-4 md:space-y-3 flex flex-col min-h-0">
          <div className="border-b border-green-900/50 pb-2 flex-shrink-0">
            <h1 className="text-lg font-bold tracking-tighter text-white flex items-center gap-1.5 leading-none">
              <Dna className="w-5 h-5 text-[#00FF41] animate-pulse" /> G.E.N. P.O.X.
            </h1>
            <p className="text-[9px] text-green-700 font-mono tracking-widest mt-1 uppercase">Serial: PX-9921-X3</p>
          </div>

          {/* Active Navigation tab layout */}
          <div className="flex flex-row md:flex-col gap-1 overflow-x-auto md:overflow-y-auto md:max-h-[310px] text-xs font-semibold scrollbar-thin md:pr-1 flex-grow">
            
            <button 
              onClick={() => { sound.playBeep(440,0.05,"sine"); setActiveTab('combinator'); }}
              className={`flex items-center space-x-2 w-full p-2 md:p-1.5 rounded border transition-all text-left whitespace-nowrap cursor-pointer ${
                activeTab === 'combinator' 
                  ? 'text-white bg-green-900/40 border-green-500/60 shadow-[0_0_8px_rgba(0,255,65,0.15)] font-bold' 
                  : 'opacity-50 hover:opacity-100 border-transparent hover:bg-neutral-800/40 text-emerald-400'
              }`}
            >
              <span>{activeTab === 'combinator' ? "⬢" : "⬡"}</span>
              <span>BIO-LAB</span>
            </button>

            <button 
              onClick={() => { sound.playBeep(440,0.05,"sine"); setActiveTab('splicer'); }}
              className={`flex items-center space-x-2 w-full p-2 md:p-1.5 rounded border transition-all text-left whitespace-nowrap cursor-pointer ${
                activeTab === 'splicer' 
                  ? 'text-white bg-green-900/40 border-green-500/60 shadow-[0_0_8px_rgba(0,255,65,0.15)] font-bold' 
                  : 'opacity-50 hover:opacity-100 border-transparent hover:bg-neutral-800/40 text-emerald-400'
              }`}
            >
              <span>{activeTab === 'splicer' ? "⬢" : "⬡"}</span>
              <span>CONSTRUCTOR</span>
            </button>

            <button 
              onClick={() => { sound.playBeep(440,0.05,"sine"); setActiveTab('library'); }}
              className={`flex items-center space-x-2 w-full p-2 md:p-1.5 rounded border transition-all text-left whitespace-nowrap cursor-pointer ${
                activeTab === 'library' 
                  ? 'text-white bg-green-900/40 border-green-500/60 shadow-[0_0_8px_rgba(0,255,65,0.15)] font-bold' 
                  : 'opacity-50 hover:opacity-100 border-transparent hover:bg-neutral-800/40 text-emerald-400'
              }`}
            >
              <span>{activeTab === 'library' ? "⬢" : "⬡"}</span>
              <span>GEN-VAULT DATA</span>
            </button>

            <button 
              onClick={() => { sound.playBeep(440,0.05,"sine"); setActiveTab('transceiver'); }}
              className={`flex items-center space-x-2 w-full p-2 md:p-1.5 rounded border transition-all text-left whitespace-nowrap cursor-pointer ${
                activeTab === 'transceiver' 
                  ? (syncNetTab === 'scanner'
                      ? 'text-white bg-purple-900/40 border-purple-500/70 shadow-[0_0_8px_rgba(168,85,247,0.25)] font-bold'
                      : 'text-white bg-green-900/40 border-green-500/60 shadow-[0_0_8px_rgba(0,255,65,0.15)] font-bold'
                    )
                  : 'opacity-50 hover:opacity-100 border-transparent hover:bg-neutral-800/40 text-emerald-400'
              }`}
            >
              <span className={activeTab === 'transceiver' && syncNetTab === 'scanner' ? 'text-purple-400' : ''}>
                {activeTab === 'transceiver' ? "⬢" : "⬡"}
              </span>
              <span>SYNC-NET</span>
            </button>

            <button 
              onClick={() => { sound.playBeep(440,0.05,"sine"); setActiveTab('stats'); }}
              className={`flex items-center space-x-2 w-full p-2 md:p-1.5 rounded border transition-all text-left whitespace-nowrap cursor-pointer ${
                activeTab === 'stats' 
                  ? 'text-white bg-green-900/40 border-green-500/60 shadow-[0_0_8px_rgba(0,255,65,0.15)] font-bold' 
                  : 'opacity-50 hover:opacity-100 border-transparent hover:bg-neutral-800/40 text-emerald-400'
              }`}
            >
              <span>{activeTab === 'stats' ? "⬢" : "⬡"}</span>
              <span>PLAYER TELEMETRY</span>
            </button>

            <button 
              onClick={() => { sound.playBeep(440,0.05,"sine"); setActiveTab('gen_network'); setGenNetworkSubTab('mail'); }}
              className={`flex items-center space-x-2 w-full p-2 md:p-1.5 rounded border transition-all text-left whitespace-nowrap cursor-pointer relative ${
                (activeTab === 'gen_network' || activeTab === 'nodes') 
                  ? 'text-white bg-green-900/40 border-green-500/60 shadow-[0_0_8px_rgba(0,255,65,0.15)] font-bold' 
                  : 'opacity-50 hover:opacity-100 border-transparent hover:bg-neutral-800/40 text-emerald-400'
              }`}
            >
              <span>{(activeTab === 'gen_network' || activeTab === 'nodes') ? "⬢" : "⬡"}</span>
              <span className="flex-grow">G.E.N. NETWORK</span>
              {genMails.some(m => !m.isRead) && (
                <span className="w-1.5 h-1.5 rounded-full bg-red-500 absolute top-3 right-2.5 animate-pulse" />
              )}
            </button>

            <button 
              onClick={() => { sound.playBeep(440,0.05,"sine"); setActiveTab('forecast'); }}
              className={`flex items-center space-x-2 w-full p-2 md:p-1.5 rounded border transition-all text-left whitespace-nowrap cursor-pointer ${
                activeTab === 'forecast' 
                  ? 'text-white bg-green-900/40 border-green-500/60 shadow-[0_0_8px_rgba(0,255,65,0.15)] font-bold' 
                  : 'opacity-50 hover:opacity-100 border-transparent hover:bg-neutral-800/40 text-emerald-400'
              }`}
            >
              <span>{activeTab === 'forecast' ? "⬢" : "⬡"}</span>
              <span>FORECAST CALENDAR</span>
            </button>

            <button 
              onClick={() => { sound.playBeep(440,0.05,"sine"); setActiveTab('inventory'); }}
              className={`flex items-center space-x-2 w-full p-2 md:p-1.5 rounded border transition-all text-left whitespace-nowrap cursor-pointer ${
                activeTab === 'inventory' 
                  ? 'text-white bg-green-900/40 border-green-500/60 shadow-[0_0_8px_rgba(0,255,65,0.15)] font-bold' 
                  : 'opacity-50 hover:opacity-100 border-transparent hover:bg-neutral-800/40 text-emerald-400'
              }`}
            >
              <span>{activeTab === 'inventory' ? "⬢" : "⬡"}</span>
              <span>INVENTORY</span>
            </button>

          </div>
        </div>

        {/* Sidebar Footer System telemetry parameters */}
        <div className="space-y-3 flex-shrink-0">
          {/* Mute and power inline selectors */}
          <div className="flex gap-1.5 justify-between border-t border-green-900/20 pt-3">
            <button 
              onClick={() => { sound.playBeep(440, 0.05, "sine"); setIsMuted(!isMuted); }}
              className={`py-1 px-1 rounded border text-[8px] font-bold uppercase cursor-pointer flex-1 ${
                isMuted 
                  ? 'bg-neutral-800 border-neutral-700 text-neutral-500 hover:text-white' 
                  : 'bg-green-900/20 border-green-500/40 text-[#00FF41] hover:bg-green-900/40'
              }`}
              title="Toggle system sound synth"
            >
              {isMuted ? "MUTE" : "SOUND: ON"}
            </button>
            <button 
              onClick={() => { 
                sound.playSynthesisSuccess(); 
                setIsPowered(!isPowered); 
                triggerLog(isPowered ? "Reactor system offline. System components frozen." : "Reactor systems online. Reactor power initiated.", isPowered ? "warn" : "success");
              }}
              className={`py-1 px-1.5 rounded border text-[8px] font-bold uppercase cursor-pointer flex-grow ${
                isPowered 
                  ? 'bg-green-900/20 border-green-500/40 text-[#00FF41]' 
                  : 'bg-red-950/45 border-red-900/60 text-red-500 hover:bg-red-900/10'
              }`}
              title="Toggle systems power"
            >
              SYS: {isPowered ? "ON" : "OFF"}
            </button>
            <button 
              onClick={() => { 
                sound.playSynthesisSuccess(); 
                setIsSyncNetOn(!isSyncNetOn); 
                triggerLog(!isSyncNetOn ? "Sync-Net wireless link established." : "Sync-Net connection severed.", !isSyncNetOn ? "success" : "warn");
              }}
              className={`py-1 px-1.5 rounded border text-[8px] font-bold uppercase cursor-pointer flex-grow ${
                isSyncNetOn 
                  ? 'bg-green-900/20 border-green-500/40 text-[#00FF41]' 
                  : 'bg-neutral-950 border-neutral-800 text-neutral-500 hover:text-white'
              }`}
              title="Toggle Sync-Net state"
            >
              SYNC-NET: {isSyncNetOn ? "ON" : "OFF"}
            </button>
          </div>

          <div className="p-2.5 bg-black border border-green-900/30 rounded text-[9px] font-mono leading-relaxed space-y-1">
            <div className="flex justify-between">
              <span className="text-green-700">CATALYTIC EFFICIENCY:</span>
              <span className="text-white">25%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-green-700">SYNC-NET CONNECTION:</span>
              <span className={isSyncNetOn ? "text-[#00FF41] font-bold" : "text-neutral-500 font-bold"}>
                {isSyncNetOn ? "STABLE" : "NONE"}
              </span>
            </div>
            <div className="flex justify-between text-green-700">
              <span>BATT STAT:</span>
              {renderBatteryStatus()}
            </div>
            <div className="border-t border-green-900/20 my-1 pt-1" />
            <div className="flex justify-between">
              <span className="text-green-700">TIME COUPLING:</span>
              <span className={isMuted ? "text-neutral-500" : isTimeVerified ? "text-cyan-400 font-bold animate-pulse" : "text-yellow-500 font-bold"}>
                {isTimeVerified ? "SECURE_SYNC" : "UNVERIFIED"}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-green-700">BEACON NET:</span>
              <span className="text-white font-semibold uppercase">{timeSource}</span>
            </div>
            {timeSkipDetected && (
              <div className="mt-1.5 p-1 bg-red-950/20 border border-red-900/70 text-red-500 font-black text-[7.5px] uppercase rounded animate-pulse text-center leading-normal">
                ⚠️ CHRONODYNAMIC FLUX BLOCKED
                <button
                  type="button"
                  onClick={() => {
                    sound.playBeep(440, 0.05, "sine");
                    syncWithSecureBeacon();
                  }}
                  className="mt-1 w-full bg-red-900 text-white rounded text-[7px] py-0.5 cursor-pointer font-bold tracking-widest hover:bg-red-800 transition-colors uppercase"
                >
                  RESYNC AUTHORIZED BEACON
                </button>
              </div>
            )}
          </div>
        </div>

      </nav>

      {/* 2. Main Control Center area */}
      <main className="flex-1 flex flex-col min-w-0 bg-black/20">
        
        {/* Top Header / Currency Rollups */}
        <header className="h-auto md:h-14 border-b border-green-900/30 flex flex-col md:flex-row items-center px-4 md:px-6 justify-between bg-black/40 gap-2 py-2 md:py-0">
          <div className="flex space-x-3 md:space-x-5 text-[10px] md:text-xs uppercase tracking-widest text-[#00FF41]/80">
            <div>[ A (Adenine): <span className="text-white font-bold">{totals.A}</span> ]</div>
            <div>[ G (Guanine): <span className="text-white font-bold">{totals.G}</span> ]</div>
            <div>[ T (Thymine): <span className="text-white font-bold">{totals.T}</span> ]</div>
            <div>[ C (Cytosine): <span className="text-white font-bold">{totals.C}</span> ]</div>
          </div>
          
          <div className="flex items-center space-x-3 text-[10px]">
            {activeTrade?.status === 'incoming_request' ? (
              <span className="text-orange-500 animate-pulse font-bold bg-orange-950/40 border border-orange-500 px-1.5 py-0.5 rounded">
                ● INCOMING CONNECTION SIGNAL
              </span>
            ) : isScanning ? (
              <span className="text-amber-400 animate-pulse">● SWEEPING PEER SECTOR...</span>
            ) : (
              <span className="text-green-700 uppercase tracking-widest text-[9px]">● G.E.N. Network: Node Localized</span>
            )}
            
            <button
              onClick={() => {
                sound.playBeep(650, 0.05, "sine");
                setDevForceAnomaly(prev => !prev);
              }}
              className={`px-2 h-6 rounded-sm text-[10px] uppercase font-bold tracking-widest cursor-pointer transition-all ${
                devForceAnomaly 
                  ? "bg-orange-500/20 border-orange-500 text-orange-500 shadow-[0_0_10px_rgba(249,115,22,0.6)] animate-[pulse_1.5s_infinite]" 
                  : "bg-green-500/10 border-green-500/50 text-white hover:border-green-400"
              }`}
              title="Toggle Dev Mode: Instantly force anomaly success"
              id="active-marker"
            >
              GNPX
            </button>
          </div>
        </header>

        {/* Content Area - Responsive Desktop Grid overlay or tab display */}
        <section className="flex-1 p-4 overflow-y-auto min-h-0">
          
          {/* Sytem is Power Off layout */}
          {!isPowered ? (
            <div className="h-full flex flex-col items-center justify-center text-center p-8 border border-neutral-800 bg-neutral-950/20 max-w-lg mx-auto rounded">
              <Dna className="w-14 h-14 text-green-900/60 animate-spin" style={{ animationDuration: '30s' }} />
              <h2 className="text-base font-bold text-green-700 tracking-widest uppercase mt-4">System Offline</h2>
              <p className="text-xs text-green-900/80 mt-2 max-w-sm leading-relaxed">
                Terminal power is currently on reserve bypass standby. Toggle power [SYS] in sidebar to initiate genetic processes.
              </p>
              <button 
                onClick={() => { sound.playSynthesisSuccess(); setIsPowered(true); }}
                className="mt-6 px-4 py-2 bg-green-900/40 border border-green-500/80 hover:bg-green-900/80 text-white font-bold text-xs uppercase tracking-widest rounded-sm cursor-pointer"
              >
                Engage G.E.N Power
              </button>
            </div>
          ) : (
            // System is powered on. Adaptive Grid system:
            // Large screens focus the tab visually inside a bento container layout,
            // or displays the selected tab screen. Let's make it show the active tab as primary,
            // while rendering the secondary components side-by-side or beautifully focused!
            // This preserves high-precision layout of Sleek Interface while fitting perfectly on lower viewports.
            <div className="h-full flex flex-col">
              
              <AnimatePresence mode="wait">
                {activeTab === 'combinator' && (
                  <motion.div 
                    key="combinator"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    className="flex flex-col h-full space-y-3"
                  >
                    {/* Top level sub-tabs for Bio-Lab */}
                    <div className="flex border-b border-green-950/40 pb-2 gap-2 select-none shrink-0 font-mono">
                      <button
                        type="button"
                        onClick={() => {
                          sound.playBeep(520, 0.05, "sine");
                          setBioLabSubTab('pox');
                        }}
                        className={`px-4 py-1.5 text-[10px] font-bold border rounded transition-all uppercase tracking-wider cursor-pointer ${
                          bioLabSubTab === 'pox'
                            ? "bg-green-950/20 border-[#00FF41] text-[#00FF41] shadow-[0_0_8px_rgba(0,255,65,0.1)] font-extrabold"
                            : "bg-black/40 border-green-955/40 text-green-700 hover:text-green-400"
                        }`}
                      >
                        P.O.X. Reactor
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          sound.playBeep(520, 0.05, "sine");
                          setBioLabSubTab('anomaly');
                        }}
                        className={`px-4 py-1.5 text-[10px] font-bold border rounded transition-all uppercase tracking-wider cursor-pointer ${
                          bioLabSubTab === 'anomaly'
                            ? "bg-purple-950/20 border-purple-500 text-purple-300 shadow-[0_0_8px_rgba(168,85,247,0.15)] font-extrabold"
                            : "bg-black/40 border-green-955/40 text-purple-800 hover:text-purple-400"
                        }`}
                      >
                        {bioLabSubTab === 'anomaly' ? <FlickeringPurpleText text="Anomaly Engine" /> : "Anomaly Engine"}
                      </button>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 flex-grow overflow-hidden">
                      
                      {/* Panel 1: Genetic Combinator (Left) */}
                      <div className={`bg-neutral-900/20 p-4 rounded flex flex-col justify-between relative overflow-y-auto custom-pox-scrollbar min-h-[300px] border transition-all ${
                        bioLabSubTab === 'anomaly' ? "border-purple-900/40" : "border-green-900/40"
                      }`}>
                      {isGeneLogPopupOpen && (
                        <motion.div
                          key="gene-log-popup"
                          initial={{ opacity: 0, scale: 0.98 }}
                          animate={{ opacity: 1, scale: 1 }}
                          exit={{ opacity: 0, scale: 0.98 }}
                          className="absolute inset-0 bg-[#020d04]/99 border border-[#00FF41]/80 rounded-md p-4 flex flex-col z-40 overflow-hidden font-mono text-left shadow-[0_0_30px_rgba(0,255,65,0.15)]"
                        >
                          {/* Title header */}
                          <div className="flex justify-between items-center border-b border-green-955 pb-2 mb-2.5 select-none">
                            <span className="text-[10px] text-[#00FF41] font-bold tracking-widest uppercase flex items-center gap-1.5">
                              <span className="w-1.5 h-1.5 bg-[#00FF41] rounded-full animate-pulse" />
                              [ GENE SYNTHESIS LOG ]
                            </span>
                            <div className="flex items-center gap-2">
                              <button
                                type="button"
                                onClick={() => {
                                  sound.playBeep(440, 0.05, "sine");
                                  setDiscoveredPacketsLog([]);
                                  setDiscoverySelectedGene(null);
                                  triggerLog("GENE ARCHIVE: Cleared all packet records.", "info");
                                }}
                                className="px-2 py-0.5 bg-red-950/20 hover:bg-red-900 border border-red-900/60 text-red-400 hover:text-white rounded text-[8px] cursor-pointer font-bold select-none transition-colors"
                              >
                                ✕ CLEAR ALL
                              </button>
                              <button
                                type="button"
                                onClick={() => {
                                  sound.playBeep(440, 0.05, "sine");
                                  setIsGeneLogPopupOpen(false);
                                }}
                                className="px-2 py-0.5 bg-green-950/80 hover:bg-green-900 border border-green-800 text-[#00FF41] hover:text-white rounded text-[8.5px] cursor-pointer font-bold tracking-wider"
                              >
                                ✕ CLOSE
                              </button>
                            </div>
                          </div>

                          {/* Conditionally Rendered Single Pane Layout */}
                          <div className="flex-1 flex flex-col min-h-0 overflow-hidden">
                            {!discoverySelectedGene ? (
                              /* Spliced Packets Register / Log List */
                              <div className="flex-1 flex flex-col min-h-0 overflow-hidden space-y-2">
                                {/* Top Flavor Text Bar replacing filter entry box as requested */}
                                <div className="flex items-center justify-center gap-2 bg-green-950/15 border border-green-950/50 p-2.5 rounded-sm shrink-0 font-mono text-[8.5px] text-green-400 select-none uppercase tracking-wider text-center">
                                  <span className="w-1.5 h-1.5 bg-green-400 rounded-full inline-block animate-ping" />
                                  <span>Select any gene block to load detailed analysis</span>
                                </div>

                                <div className="flex-1 overflow-y-auto space-y-2 pr-0.5 custom-pox-scrollbar">
                                  {(() => {
                                    const filtered = discoveredPacketsLog.filter(packet => {
                                      if (!discoverySearchText) return true;
                                      return packet.genes.some(c => c.sequence.toUpperCase().includes(discoverySearchText));
                                    });

                                    if (filtered.length === 0) {
                                      return (
                                        <div className="text-center py-12 text-[9px] text-green-700 font-mono italic uppercase">
                                          No packets discovered match the search term.
                                        </div>
                                      );
                                    }

                                    return filtered.map((packet, pIdx) => {
                                      const uniqueCount = packet.genes.filter(c => c.isNew).length;
                                      const timeStr = new Date(packet.timestamp).toLocaleTimeString();
                                      return (
                                        <div key={packet.id || pIdx} className="border border-green-950/60 bg-black/40 p-2 rounded-sm space-y-1.5 hover:border-green-800/40 transition-colors">
                                          <div className="flex justify-between items-center text-[7.5px] text-green-500 border-b border-green-950/30 pb-1 select-none">
                                            <span className="font-bold flex items-center gap-1">
                                              <span className="text-neutral-600">#{discoveredPacketsLog.length - pIdx}</span> PACKET SPLICED
                                            </span>
                                            <span className="font-mono flex items-center gap-2">
                                              <span className="text-cyan-500 font-bold">{uniqueCount} NEW GENES</span>
                                              <span className="text-neutral-500">{timeStr}</span>
                                            </span>
                                          </div>
                                          <div className="grid grid-cols-4 gap-1">
                                            {packet.genes.map((gene, cIdx) => {
                                              const isAnom = isAnomalousGene(gene.sequence);
                                              const isSelected = discoverySelectedGene === gene.sequence;
                                              return (
                                                <button
                                                  key={cIdx}
                                                  onClick={() => {
                                                    sound.playBeep(450, 0.05, "sine");
                                                    setDiscoverySelectedGene(gene.sequence);
                                                  }}
                                                  className={`px-1 py-1 border text-center font-mono rounded-sm select-none transition-all text-[8.5px] font-bold cursor-pointer hover:scale-[1.02] ${
                                                    isSelected
                                                      ? isAnom
                                                        ? "border-purple-400 bg-purple-950/50 text-purple-300 shadow-[0_0_8px_rgba(168,85,247,0.3)] font-extrabold"
                                                        : "border-[#00FF41] bg-green-900/30 text-[#00FF41] shadow-[0_0_8px_rgba(0,255,65,0.2)]"
                                                      : gene.isNew
                                                        ? isAnom
                                                          ? "border-purple-650 bg-purple-950/20 text-purple-400 animate-pulse"
                                                          : "border-green-500/50 bg-green-950/10 text-[#00FF41] animate-pulse"
                                                        : isAnom
                                                          ? "border-purple-950/60 bg-purple-950/5 text-purple-300/70"
                                                          : "border-sky-950/50 bg-sky-950/10 text-sky-400/80"
                                                  }`}
                                                >
                                                  {gene.sequence}
                                                </button>
                                              );
                                            })}
                                          </div>
                                        </div>
                                      );
                                    });
                                  })()}
                                </div>
                              </div>
                            ) :
                              /* 2. Detailed Gene analysis view occupied over the log list */
                              (() => {
                                const seq = discoverySelectedGene;
                                const isAnom = isAnomalousGene(seq);
                                
                                // Count composition
                                let a = 0, t = 0, c = 0, g = 0, q = 0;
                                for (const char of seq) {
                                  if (char === 'A') a++;
                                  else if (char === 'T') t++;
                                  else if (char === 'C') c++;
                                  else if (char === 'G') g++;
                                  else q++;
                                }
                                const len = seq.length || 1;
                                const pA = Math.round((a / len) * 100);
                                const pT = Math.round((t / len) * 100);
                                const pC = Math.round((c / len) * 100);
                                const pG = Math.round((g / len) * 100);
                                const pQ = Math.round((q / len) * 100);

                                const inventoryItem = sequences.find(item => item.sequence === seq && item.count > 0);
                                const stockCount = inventoryItem ? inventoryItem.count : 0;

                                return (
                                  <div className={`flex-1 flex flex-col justify-between border p-4 rounded-sm bg-black/60 font-mono text-left animate-fade-in min-h-0 overflow-y-auto custom-pox-scrollbar ${
                                    isAnom ? "border-purple-500/50 shadow-[0_0_12px_rgba(168,85,247,0.15)]" : "border-green-955/65"
                                  }`}>
                                    <div className="space-y-3.5 min-h-0 overflow-y-auto custom-pox-scrollbar">
                                      {/* Header with BACK button */}
                                      <div className="flex items-center justify-between border-b border-green-955/20 pb-2 mb-3 shrink-0">
                                        <button
                                          onClick={() => {
                                            sound.playBeep(440, 0.05, "sine");
                                            setDiscoverySelectedGene(null);
                                          }}
                                          className="px-2 py-1 bg-green-950/65 hover:bg-green-900/85 border border-green-800 text-[#00FF41] font-bold text-[8px] rounded-sm cursor-pointer transition-colors flex items-center justify-center gap-1 shadow-sm select-none animate-pulse"
                                        >
                                          ◀ RETURN TO LOG
                                        </button>
                                        <div className={`text-[7.5px] font-bold uppercase tracking-wider ${isAnom ? "text-purple-400" : "text-emerald-500"}`}>
                                          {isAnom ? "🌌 ANOMALOUS COSMIC LINK" : "🧬 STANDARD GENE STRAND"}
                                        </div>
                                      </div>

                                      {/* Sequence display */}
                                      <div>
                                        <div className="text-[7px] text-neutral-500 uppercase tracking-widest leading-none mb-1">Gene String Sequence</div>
                                        <div className={`text-base font-black tracking-widest break-all select-all ${
                                          isAnom ? "text-purple-300 filter drop-shadow-[0_0_2px_#a855f7]" : "text-white"
                                        }`}>
                                          {seq}
                                        </div>
                                      </div>

                                      {/* Molecular Stats */}
                                      <div className="space-y-2 border-t border-green-950/50 pt-2.5 text-[8px]">
                                        <span className="text-neutral-500 uppercase tracking-widest block mb-1">Base Composition</span>
                                        
                                        {/* A */}
                                        <div>
                                          <div className="flex justify-between text-neutral-450 text-[7px] leading-none uppercase font-bold">
                                            <span>A (ADENINE)</span>
                                            <span>{pA}%</span>
                                          </div>
                                          <div className="w-full bg-neutral-950 h-1.5 rounded-sm overflow-hidden mt-1 border border-green-950/20">
                                            <div className="bg-[#00FF41] h-full" style={{ width: `${pA}%` }} />
                                          </div>
                                        </div>

                                        {/* T */}
                                        <div>
                                          <div className="flex justify-between text-neutral-450 text-[7px] leading-none uppercase font-bold">
                                            <span>T (THYMINE)</span>
                                            <span>{pT}%</span>
                                          </div>
                                          <div className="w-full bg-neutral-950 h-1.5 rounded-sm overflow-hidden mt-1 border border-green-950/20">
                                            <div className="bg-sky-400 h-full" style={{ width: `${pT}%` }} />
                                          </div>
                                        </div>

                                        {/* C */}
                                        <div>
                                          <div className="flex justify-between text-neutral-450 text-[7px] leading-none uppercase font-bold">
                                            <span>C (CYTOSINE)</span>
                                            <span>{pC}%</span>
                                          </div>
                                          <div className="w-full bg-neutral-950 h-1.5 rounded-sm overflow-hidden mt-1 border border-green-950/20">
                                            <div className="bg-amber-400 h-full" style={{ width: `${pC}%` }} />
                                          </div>
                                        </div>

                                        {/* G */}
                                        <div>
                                          <div className="flex justify-between text-neutral-450 text-[7px] leading-none uppercase font-bold">
                                            <span>G (GUANINE)</span>
                                            <span>{pG}%</span>
                                          </div>
                                          <div className="w-full bg-neutral-950 h-1.5 rounded-sm overflow-hidden mt-1 border border-green-950/20">
                                            <div className="bg-emerald-500 h-full" style={{ width: `${pG}%` }} />
                                          </div>
                                        </div>

                                        {/* Quantum bits */}
                                        {q > 0 && (
                                          <div>
                                            <div className="flex justify-between text-purple-400 text-[7px] leading-none uppercase font-bold">
                                              <span>QUANTUM MATRIX BIND</span>
                                              <span>{pQ}%</span>
                                            </div>
                                            <div className="w-full bg-neutral-950 h-1.5 rounded-sm overflow-hidden mt-1 border border-purple-950/20">
                                              <div className="bg-purple-500 h-full animate-pulse" style={{ width: `${pQ}%` }} />
                                            </div>
                                          </div>
                                        )}
                                      </div>

                                      {/* Splicing Status */}
                                      <div className="border-t border-green-950/50 pt-2.5 text-[8px]">
                                        <div className="flex justify-between items-center bg-black/50 p-1.5 border border-green-950 rounded-sm">
                                          <span className="text-neutral-500 font-bold uppercase">Splicer Hold:</span>
                                          <span className={`font-black uppercase tracking-wider ${stockCount > 0 ? "text-[#00FF41]" : "text-amber-500"}`}>
                                            {stockCount > 0 ? `DEPOSITED (x${stockCount})` : "OUT OF STOCK"}
                                          </span>
                                        </div>
                                      </div>
                                    </div>

                                    {/* Buttons / Actions */}
                                    <div className="border-t border-green-950/50 pt-2 mt-2 space-y-1.5 select-none shrink-0">
                                      {/* Play signature sound */}
                                      <button
                                        onClick={() => {
                                          sound.playBeep(520, 0.05, "sine");
                                          sound.playCreatureSequenceAudio(seq);
                                        }}
                                        className="w-full py-1.5 text-[8px] font-bold text-cyan-400 hover:text-white bg-cyan-950/25 hover:bg-cyan-900/40 border border-cyan-800/40 hover:border-cyan-400 rounded-sm cursor-pointer transition-all flex items-center justify-center gap-1.5 uppercase select-none active:scale-[0.98]"
                                      >
                                        <Volume2 className="w-3.5 h-3.5 text-cyan-400" />
                                        <span>SYNTHESIZE MOTIF</span>
                                      </button>

                                      {/* Copy */}
                                      <button
                                        onClick={() => {
                                          navigator.clipboard.writeText(seq);
                                          sound.playBeep(880, 0.08, "sine");
                                          triggerLog(`DUPLICATED GENE: ${seq}`, "success");
                                        }}
                                        className="w-full py-1.5 text-[8px] font-bold text-green-400 hover:text-white bg-green-950/25 hover:bg-[#00FF41]/20 border border-green-800/40 hover:border-green-400 rounded-sm cursor-pointer transition-all flex items-center justify-center gap-1.5 uppercase select-none active:scale-[0.98]"
                                      >
                                        <Check className="w-3.5 h-3.5 text-green-500" />
                                        <span>COPY SEQUENCE</span>
                                      </button>
                                    </div>
                                  </div>
                                );
                              })()}
                          </div>

                          {/* Reset logs footer */}
                          <div className="pt-2 mt-2.5 border-t border-green-950 text-[8px] text-neutral-500 flex justify-between items-center select-none font-mono shrink-0">
                            <span className="text-[#00FF41]/80 animate-pulse flex items-center gap-1 uppercase">
                              <span className="w-1 h-1 bg-[#00FF41] rounded-full inline-block" /> 
                              Standard Gene Search Active
                            </span>
                            <span className="text-green-800 uppercase tracking-widest text-[7.5px]">
                              Secure connection: AIS-DEV-ENV
                            </span>
                          </div>
                        </motion.div>
                      )}

                       {bioLabSubTab === 'pox' ? (
                        <div>
                          <div className="flex justify-between items-center mb-1 text-[10px] uppercase text-green-700">
                            <span>[ G.E.N. P.O.X. Tide Pool Reactor V2.4 ]</span>
                            <span className="text-[#00FF41]">SYSTEMS ON</span>
                          </div>
                          <h2 className="text-xs font-bold text-white tracking-wider mb-3 uppercase">Single-Node Cybernetic Synthesizer</h2>

                          {/* Top-Level Counts section as requested */}
                          <div className="grid grid-cols-2 gap-2 mb-4 border-t border-b border-green-950/70 py-2.5 bg-black/40 px-2 rounded-sm font-mono text-left">
                            <div>
                              <span className="block text-[8.5px] text-green-600 uppercase tracking-widest leading-none">Unique Gene IDs</span>
                              <span className="text-lg font-bold text-white leading-none block mt-1">
                                <span className="text-[#00FF41]">⬢</span> {sequences.length}
                              </span>
                            </div>
                            <div className="border-l border-green-950/85 pl-3">
                              <span className="block text-[8.5px] text-green-600 uppercase tracking-widest leading-none">Multi-Count Gene IDs</span>
                              <span className="text-lg font-bold text-white leading-none block mt-1">
                                <span className="text-[#00FF41]">⬢</span> {sequences.filter(s => s.count > 1).length}
                              </span>
                            </div>
                          </div>

                          {/* Dynamic Daily Base-Pair Wave HUD Display */}
                          <div className={`mb-2 border rounded-sm p-2 flex items-center justify-between gap-2.5 bg-black/60 font-mono text-[9px] ${
                            todayWave.isSuppressed 
                              ? "border-red-950/80 text-red-500 bg-red-955/5 animate-pulse" 
                              : "border-green-955 text-neutral-300"
                          }`}>
                            <div className="flex items-center gap-1.5 min-w-0">
                              <Zap className={`w-3.5 h-3.5 ${todayWave.isSuppressed ? "text-red-500" : "text-amber-400 animate-pulse"}`} />
                              <div>
                                <span className="text-[7.5px] text-green-600 block leading-none font-bold select-none uppercase">TODAY'S BASE-PAIR WAVE</span>
                                <span className="text-white font-bold tracking-wider uppercase truncate">
                                  {todayWave.isSuppressed ? "DORMANT (CONGESTED DECAY)" : `ACTIVE: ${todayWave.pair} WAVE`}
                                </span>
                              </div>
                            </div>
                            {!todayWave.isSuppressed && (
                              <div className="text-right shrink-0">
                                <span className="text-[10px] text-[#00FF41] font-bold block leading-none font-mono">{todayWave.primary} ➔ {todayWave.secondary}</span>
                                <span className="text-[7px] text-green-600 block leading-none font-bold mt-0.5">1.12x &amp; 1.62x BOOST</span>
                              </div>
                            )}
                            {todayWave.isSuppressed && (
                              <span className="text-[7px] text-red-400 bg-red-955/30 px-1 border border-red-955 rounded font-bold uppercase select-none">NULL</span>
                            )}
                          </div>

                          {/* Forecast Calendar Waves Compact Layout */}
                          <div className="grid grid-cols-2 gap-2 mb-3">
                            {/* Tomorrow's Wave */}
                            <div className={`border rounded-sm p-1.5 flex items-center justify-between gap-1 bg-black/45 font-mono text-[8px] ${
                              tomorrowWave.isSuppressed ? "border-red-955/60 text-red-400 bg-red-955/5 animate-pulse" : "border-neutral-900/60 text-neutral-450"
                            }`}>
                              <div className="min-w-0">
                                <span className="text-[6.5px] text-green-700 block leading-none font-bold select-none uppercase">TOMORROW BASE-PAIR</span>
                                <span className="text-white font-bold tracking-wider uppercase truncate text-[8px] mt-0.5 block">
                                  {tomorrowWave.isSuppressed ? "DORMANT" : `${tomorrowWave.pair} WAVE`}
                                </span>
                              </div>
                              {!tomorrowWave.isSuppressed && (
                                <span className="text-[8.5px] text-[#00FF41] font-bold font-mono pl-1 shrink-0">{tomorrowWave.primary}➔{tomorrowWave.secondary}</span>
                              )}
                            </div>

                            {/* Day After's Wave */}
                            <div className={`border rounded-sm p-1.5 flex items-center justify-between gap-1 bg-black/45 font-mono text-[8px] ${
                              dayAfterWave.isSuppressed ? "border-red-955/60 text-red-400 bg-red-955/5 animate-pulse" : "border-neutral-900/60 text-neutral-450"
                            }`}>
                              <div className="min-w-0">
                                <span className="text-[6.5px] text-green-700 block leading-none font-bold select-none uppercase">DAY AFTER TOMORROW</span>
                                <span className="text-white font-bold tracking-wider uppercase truncate text-[8px] mt-0.5 block">
                                  {dayAfterWave.isSuppressed ? "DORMANT" : `${dayAfterWave.pair} WAVE`}
                                </span>
                              </div>
                              {!dayAfterWave.isSuppressed && (
                                <span className="text-[8.5px] text-[#00FF41] font-bold font-mono pl-1 shrink-0">{dayAfterWave.primary}➔{dayAfterWave.secondary}</span>
                              )}
                            </div>
                          </div>
                        </div>
                      ) : (
                        <div>
                          <div className="flex justify-between items-center mb-1 text-[10px] uppercase text-purple-400">
                            <span>[ WARNING: UNKNOWN REACTOR ]</span>
                            <span className="text-purple-300 animate-pulse">SYSTEMS ON</span>
                          </div>
                          <div className="mb-3">
                            <FlickeringAnomalyTitle />
                          </div>

                          {/* Top-Level Counts section as requested */}
                          <div className="grid grid-cols-2 gap-2 mb-4 border-t border-b border-purple-900/50 py-2.5 bg-black/40 px-2 rounded-sm font-mono text-left">
                            <div 
                              onClick={() => { sound.playBeep(600,0.05,"sine"); setIsAnomalousLedgerExpanded(true); }}
                              className="cursor-pointer group hover:bg-purple-950/20 p-1 rounded-sm transition-all flex flex-col justify-between"
                              title="Click to Decrypt Anomaly Vault"
                            >
                              <span className="block text-[8.5px] text-purple-400 uppercase tracking-widest leading-none group-hover:text-purple-300">Anomalous Gene IDs</span>
                              <div className="text-lg font-bold text-white leading-none mt-1 flex items-center justify-between">
                                <span><span className="text-purple-400">⬢</span> {sequences.filter(s => isAnomalousGene(s.sequence)).length}</span>
                                <span className="text-[6.5px] text-purple-400 bg-purple-950/60 border border-purple-500/20 px-1 py-0.5 rounded opacity-70 group-hover:opacity-100 transition-opacity">OPEN ➔</span>
                              </div>
                            </div>
                            <div className="border-l border-purple-900/50 pl-3 pt-1">
                              <span className="block text-[8.5px] text-purple-400 uppercase tracking-widest leading-none">Total Gene Stock</span>
                              <span className="text-lg font-bold text-white leading-none block mt-1">
                                <span className="text-[#00FF41]">⬢</span> {sequences.length}
                              </span>
                            </div>
                          </div>
                        </div>
                      )}

                      <div className="flex-1 flex flex-col items-center justify-center space-y-2.5 my-1">
                        <div className={`text-xl md:text-2xl font-bold tracking-widest bg-black border-2 px-4 py-2 rounded select-all shadow-[inset_0_2px_8px_rgba(0,0,0,0.8)] font-mono ${
                          isForcedLoopActive 
                            ? "border-red-500 text-red-500 animate-[pulse_3s_infinite]" 
                            : bioLabSubTab === 'anomaly'
                              ? "border-purple-500 text-purple-300 animate-[pulse_5s_infinite]"
                              : "border-green-500 text-[#00FF41]"
                        }`}>
                          {isForcedLoopActive 
                            ? "--------" 
                            : bioLabSubTab === 'anomaly'
                              ? (!anomalyEngineActive 
                                  ? "--------" 
                                  : (() => {
                                      const syms = "XZYW?!$%&@#";
                                      let rez = "";
                                      for (let i = 0; i < scrollingGene.length; i++) {
                                        const idx = (scrollingGene.charCodeAt(i) + i) % syms.length;
                                        rez += syms[idx];
                                      }
                                      return rez;
                                    })())
                              : scrollingGene
                          }
                        </div>
                        
                        <div className={`w-full max-w-sm bg-neutral-900 h-2 border rounded-full overflow-hidden p-0.5 ${
                          isForcedLoopActive 
                            ? "border-red-950/80 shadow-[0_0_8px_rgba(239,68,68,0.2)]" 
                            : bioLabSubTab === 'anomaly'
                              ? anomalyEngineActive 
                                ? "border-purple-955" 
                                : "border-neutral-800"
                              : "border-green-950"
                        }`}>
                          <div 
                            className={isForcedLoopActive 
                              ? "bg-red-600 h-full rounded-sm animate-[pulse_2s_infinite]" 
                              : bioLabSubTab === 'anomaly'
                                ? anomalyEngineActive
                                  ? "bg-gradient-to-r from-purple-600 to-pink-500 h-full rounded-sm transition-all duration-1000 ease-linear"
                                  : "bg-neutral-800/40 h-full rounded-sm"
                                : "bg-gradient-to-r from-emerald-600 to-[#00FF41] h-full rounded-sm transition-all duration-1000 ease-linear"
                            }
                            style={{ 
                              width: isForcedLoopActive 
                                ? "100%" 
                                : (bioLabSubTab === 'anomaly' && !anomalyEngineActive)
                                  ? "0%"
                                  : `${(((boostSecondsLeft > 0 ? 8 : 16) - idleTime) / (boostSecondsLeft > 0 ? 8 : 16)) * 100}%` 
                            }}
                          />
                        </div>
                        
                        <div className="text-center font-mono text-[11px] tracking-wide space-y-1.5 flex flex-col items-center">
                          {isForcedLoopActive ? (
                            <div className="text-red-550 font-extrabold uppercase animate-pulse flex items-center gap-1.5 text-[9.5px] tracking-wider select-none bg-red-950/25 px-3 py-1 border border-red-500/30 rounded text-center max-w-[280px]">
                              ⚠️ REACTOR FROZEN: LOOPING FORCED CONSTRUCTION
                            </div>
                          ) : (
                            <div className={bioLabSubTab === 'anomaly' ? "text-purple-400 font-bold" : "text-green-500 font-bold"}>
                              {bioLabSubTab === 'anomaly' ? (
                                anomalyEngineActive ? (
                                  <><FlickeringPurpleText text="Anomalous Consolidation in:" /> <span className="text-white font-bold">{idleTime}s</span></>
                                ) : (
                                  <><FlickeringPurpleText text="Anomalous Consolidation:" /> <span className="text-neutral-500 font-bold">IDLE</span></>
                                )
                              ) : (
                                <>Gene Array Ready in: <span className="text-white font-bold">{idleTime}s</span></>
                              )}
                            </div>
                          )}
                          {boostSecondsLeft > 0 && !isForcedLoopActive && (
                            <div className={`text-[10px] font-bold px-2 py-0.5 border rounded animate-pulse font-mono flex items-center gap-1 ${
                              bioLabSubTab === 'anomaly'
                                ? "text-purple-300 bg-purple-950/20 border-purple-500/40"
                                : "text-[#00FF41] bg-[#00FF41]/15 border-[#00FF41]/40"
                            }`}>
                              <Zap className="w-3.5 h-3.5 text-amber-400" />
                              REACTOR BOOST ACTIVE: {Math.floor(boostSecondsLeft / 60)}:{(boostSecondsLeft % 60).toString().padStart(2, '0')} REMAINING
                            </div>
                          )}
                        </div>
                      </div>

                      <div className="space-y-2">
                        <button
                          type="button"
                          onClick={() => {
                            sound.playBeep(650, 0.05, "sine");
                            setIsGeneLogPopupOpen(true);
                          }}
                          className={`w-full py-2 border font-mono font-bold uppercase text-[10px] rounded-sm cursor-pointer transition-all flex items-center justify-center gap-1.5 active:scale-[0.99] ${
                            bioLabSubTab === 'anomaly'
                              ? "bg-purple-950/40 hover:bg-purple-900/60 border-purple-500/50 text-purple-300"
                              : "bg-blue-950/40 hover:bg-blue-900/60 border-blue-500/50 text-[#00E1FF]"
                          }`}
                        >
                          <Terminal className={`w-3.5 h-3.5 stroke-[2.5] ${bioLabSubTab === 'anomaly' ? "text-purple-300" : "text-[#00E1FF]"}`} />
                          <span>{bioLabSubTab === 'anomaly' ? "View Anomaly Discovery Log" : "View Gene Synthesis Log"}</span>
                        </button>

                        <button 
                          onClick={handleManualCombinatorClick}
                          className={`w-full py-2 border text-white font-bold uppercase text-xs transition-all cursor-pointer rounded-sm active:scale-[0.99] ${
                            bioLabSubTab === 'anomaly'
                              ? "bg-purple-900/40 border-purple-500/80 hover:bg-purple-900"
                              : "bg-green-900/40 border-green-500/80 hover:bg-green-900"
                          }`}
                        >
                          Manual Acceleration (-2s)
                        </button>
                      </div>
                    </div>

                    {/* Combinator Help Code & Sequence Ledger (Right) */}
                    <div className={`p-4 rounded flex flex-col justify-between relative overflow-y-auto custom-pox-scrollbar min-h-[300px] bg-neutral-900/20 border transition-all ${
                      bioLabSubTab === 'anomaly' ? "border-purple-900/45" : "border-green-900/40"
                    }`}>
                      <div>
                        <div className={`text-[10px] uppercase mb-1.5 font-mono ${
                          bioLabSubTab === 'anomaly' ? "text-purple-400" : "text-green-700"
                        }`}>
                          {bioLabSubTab === 'anomaly' ? (
                            <FlickeringPurpleText text="[ SPLICER STOCK GENE LEDGER ]" />
                          ) : (
                            "[ SPLICER STOCK GENE LEDGER ]"
                          )}
                        </div>
                        <h2 className="text-xs font-bold text-white tracking-wider mb-1.5 uppercase">Gene Inventory</h2>
                        <b className={`text-[10px] mb-3 block leading-tight font-mono ${
                          bioLabSubTab === 'anomaly' ? "text-purple-500/90" : "text-[#00c93c]"
                        }`}>
                          {bioLabSubTab === 'anomaly' ? (
                            <FlickeringPurpleText text="These 8-character DNA blocks act as raw units to construct the 64-character target genome." />
                          ) : (
                            "These 8-character DNA blocks act as raw units to construct the 64-character target genome."
                          )}
                        </b>
                      </div>

                      {/* Display Buttons and Stockpile Panels inside Default View */}
                      <div className="flex-1 flex flex-col justify-center min-h-[140px] min-h-0 space-y-3.5">
                        {/* pair-based array search button */}
                        {bioLabSubTab === 'pox' && (
                          <button 
                            onClick={() => { 
                              sound.playBeep(600,0.05,"sine"); 
                              setDiscoverySearchPrefix("");
                              setDiscoverySearchStep(0);
                              setIsGeneLedgerExpanded(true); 
                            }}
                            className="w-full py-3.5 px-4 bg-black/60 border border-green-955 hover:border-[#00FF41] hover:bg-green-955/20 text-[#00FF41] rounded flex flex-col items-center justify-center gap-1.5 transition-all cursor-pointer group text-center shadow-[0_0_15px_rgba(0,255,65,0.05)]"
                          >
                            <span className="text-sm font-bold tracking-wider text-white font-mono uppercase group-hover:text-[#00FF41] transition-colors flex items-center justify-center gap-1.5">
                              <Search className="w-4 h-4 text-[#00FF41] animate-pulse" />
                              PAIR-BASED STEP-SEARCH
                            </span>
                            <span className="text-[8.5px] font-mono text-green-500 uppercase tracking-widest bg-green-950/30 px-2 py-0.5 border border-green-500/20 rounded">
                              [ Run Hierarchical Query ▼ ]
                            </span>
                          </button>
                        )}

                        {/* G.E.N. Gene Stockpile Panel for P.O.X. Reactor Sub-Tab as a beautiful matching info panel display */}
                        {bioLabSubTab === 'pox' && (() => {
                          const uniqueStandardCount = sequences.filter(s => !isAnomalousGene(s.sequence)).length;
                          const totalStandardCopies = sequences.filter(s => !isAnomalousGene(s.sequence)).reduce((sum, s) => sum + (s.count || 0), 0);

                          const requiredGenesList = [];
                          for (let i = 0; i < 8; i++) {
                            requiredGenesList.push(targetSequence.substring(i * 8, (i + 1) * 8));
                          }
                          const ownedRequired = requiredGenesList.filter(rg => 
                            sequences.some(s => s.sequence === rg && (s.count || 0) > 0)
                          ).length;

                          return (
                            <div className="border border-green-900/30 p-2.5 rounded-sm font-mono text-[9px] bg-black/35 transition-colors">
                              <div className="flex items-center justify-between mb-1.5 select-none">
                                <span className="font-bold tracking-wider uppercase flex items-center gap-1.5 text-green-400">
                                  <span className="w-1.5 h-1.5 bg-green-500 rounded-full animate-ping" />
                                  Gene Stockpile Info
                                </span>
                              </div>

                              <div className="space-y-1 bg-black/45 p-1.5 rounded-sm border border-neutral-900/60 text-[7.5px] leading-normal mb-2.5">
                                {/* Stat 1: Total uniquely cataloged standard genes vs. ultimate possible permutations */}
                                <div className="flex justify-between items-center">
                                  <span className="text-neutral-500 uppercase font-bold">Unique Standard Genes:</span>
                                  <span className="font-bold text-white">
                                    {uniqueStandardCount.toLocaleString()} / 65,536 Permutations
                                  </span>
                                </div>

                                {/* Stat 2: Total raw inventory copies */}
                                <div className="flex justify-between items-center">
                                  <span className="text-neutral-500 uppercase font-bold">Total Gene Blocks Owned:</span>
                                  <span className="text-[#00FF41] font-black">
                                    {totalStandardCopies.toLocaleString()} Blocks
                                  </span>
                                </div>

                                {/* Stat 3: Target Sequence Alignment (the 8 required chunks) */}
                                <div className="flex justify-between items-center">
                                  <span className="text-neutral-500 uppercase font-bold">Target Alignment:</span>
                                  <span className={`font-black ${
                                    ownedRequired === 8 
                                      ? "text-cyan-400 font-extrabold animate-pulse" 
                                      : "text-amber-500"
                                  }`}>
                                    {ownedRequired} / 8 Segment Blocks {ownedRequired === 8 ? "✓" : ""}
                                  </span>
                                </div>
                              </div>

                              {/* Construction alignment index Dial Breakdown */}
                              <div className="p-2 border border-green-955 bg-[#041206] rounded-sm space-y-1 font-mono text-[7px]">
                                <div className="text-[7.5px] font-bold text-green-400 tracking-wider uppercase border-b border-green-955/40 pb-0.5 flex justify-between">
                                  <span>Constructor Alignment Metrics</span>
                                  <span className="text-white font-black">{((ownedRequired / 8) * 100).toFixed(0)}% Ready</span>
                                </div>

                                <div className="text-neutral-450 leading-normal text-[6.5px]">
                                  Tracks raw 8-character DNA gene blocks required to compile the full 64-character target genome sequence. Harvest nodes via map scanner to secure missing blocks.
                                </div>
                              </div>
                            </div>
                          );
                        })()}

                        {/* Anomalous Genes Ledger Button */}
                        {bioLabSubTab === 'anomaly' && (
                          <button 
                            onClick={() => { sound.playBeep(600,0.05,"sine"); setIsAnomalousLedgerExpanded(true); }}
                            className="w-full py-3.5 px-4 bg-black/60 border border-purple-500/80 hover:border-purple-400 hover:bg-purple-955/15 text-purple-300 rounded flex flex-col items-center justify-center gap-1.5 transition-all cursor-pointer group text-center shadow-[0_0_15px_rgba(168,85,247,0.05)]"
                          >
                            <span className="text-sm font-bold tracking-wider text-white font-mono uppercase group-hover:text-purple-400 transition-colors flex items-center justify-center gap-1.5">
                              {sequences.filter(s => isAnomalousGene(s.sequence)).length} Anomalous Genes Secured
                            </span>
                            <span className="text-[8.5px] font-mono text-purple-400 uppercase tracking-widest bg-purple-950/40 px-2 py-0.5 border border-purple-500/30 rounded shadow-[0_0_8px_rgba(168,85,247,0.3)]">
                              <FlickeringPurpleText text="[ DECRYPT ANOMALY VAULT ▼ ]" />
                            </span>
                          </button>
                        )}

                        {bioLabSubTab === 'anomaly' && (
                          <div className={`border p-2.5 rounded-sm font-mono text-[9px] transition-colors ${
                            anomalyEngineActive 
                              ? "border-purple-500/70 bg-[#0c0512]" 
                              : "border-purple-900/30 bg-black/35"
                          }`}>
                            <div className="flex items-center justify-between mb-1.5 select-none">
                              <span className={`font-bold tracking-wider uppercase flex items-center gap-1.5 ${
                                anomalyEngineActive ? "text-purple-400" : "text-purple-500"
                              }`}>
                                <span className="w-1.5 h-1.5 bg-purple-500 rounded-full animate-ping" />
                                <FlickeringPurpleText text="Anomaly Engine" />
                              </span>
                              <label className="relative inline-flex items-center cursor-pointer select-none">
                                <input 
                                  type="checkbox" 
                                  checked={anomalyEngineActive} 
                                  onChange={(e) => handleToggleAnomalyEngine(e.target.checked)}
                                  className="sr-only peer"
                                />
                                <div className="w-7 h-4 bg-neutral-800 border border-neutral-700/60 rounded-full peer peer-focus:ring-0 peer-checked:bg-purple-600 after:content-[''] after:absolute after:top-[4px] after:left-[3px] after:bg-neutral-400 after:rounded-full after:h-2 after:w-2 after:transition-all peer-checked:after:translate-x-3.5 peer-checked:after:bg-purple-200" />
                              </label>
                            </div>

                            <div className="space-y-1 bg-black/45 p-1.5 rounded-sm border border-neutral-900/60 text-[7px] leading-normal mb-2.5">
                              {/* Requirement 1: Material stockpile key */}
                              <div className="flex justify-between items-center">
                                <span className="text-neutral-500 uppercase">RESOURCE COUNT:</span>
                                <span className={`font-semibold ${
                                  baseTallies.grandTotal >= 250000 
                                    ? "text-[#00FF41]" 
                                    : "text-red-400 animate-pulse"
                                }`}>
                                  {baseTallies.grandTotal.toLocaleString()} / 250,000 NUCLEOTIDES  {baseTallies.grandTotal >= 250000 ? "✓" : "✗"}
                                </span>
                              </div>

                              {/* Requirement 2: Energy Drain Warnings */}
                              <div className="flex justify-between items-center">
                                <span className="text-neutral-500 uppercase">LOAD WARNING:</span>
                                <span className="text-yellow-500/90 font-bold uppercase">RESETS STABILITY &amp; EMISSIONS TO 0</span>
                              </div>

                              {/* Cycle Cost */}
                              <div className="flex justify-between items-center">
                                <span className="text-neutral-500 uppercase">CURRENT RATE:</span>
                                <span className={anomalyEngineActive ? "text-purple-400 font-bold" : "text-neutral-450"}>
                                  -10,000 BASES / LOOP
                                </span>
                              </div>
                            </div>

                            {/* Success Chance Live Dial Breakdown */}
                            {(() => {
                              const statsChance = getAnomalyEngineSuccessChance(baseTallies.grandTotal);
                              return (
                                <div className="p-2 border border-purple-950/70 bg-[#0f0417] rounded-sm space-y-1 font-mono text-[7px]">
                                  <div className="text-[7.5px] font-bold text-purple-400 tracking-wider uppercase border-b border-purple-950/40 pb-0.5 flex justify-between">
                                    <span><FlickeringPurpleText text="ANOMALOUS DISCOVERY" /></span>
                                    <span className="text-white font-black">{statsChance.finalChance.toFixed(3)}%</span>
                                  </div>

                                  {statsChance.peakBoost > 0 && (
                                    <div className="flex justify-between text-pink-400 animate-pulse">
                                      <span>Resonance Peak Wave Spike:</span>
                                      <span>+{statsChance.peakBoost.toFixed(1)}%</span>
                                    </div>
                                  )}
                                  <div className="flex justify-between text-cyan-400">
                                    <span>Spectrum Dial Coupling Modifier:</span>
                                    <span>{statsChance.harmonicModifier >= 0 ? "+" : ""}{statsChance.harmonicModifier.toFixed(3)}%</span>
                                  </div>
                                </div>
                              );
                            })()}
                          </div>
                        )}
                      </div>

                      {bioLabSubTab === 'pox' && (
                        <div className="text-[10px] text-green-700/80 leading-relaxed font-mono pt-3 border-t border-green-900/20 mt-3">
                          INFO: Gene blocks are used in the CONSTRUCTOR to build your target P.O.X. sequence.
                        </div>
                      )}

                      {/* Display the Unique Patterns and Anomalous Genes windows as Absolute Overlays covering only this right pane */}
                      <AnimatePresence>
                        {isGeneLedgerExpanded && (
                          <motion.div
                            key="pair-base-search-popup"
                            initial={{ opacity: 0, scale: 0.98 }}
                            animate={{ opacity: 1, scale: 1 }}
                            exit={{ opacity: 0, scale: 0.98 }}
                            className="absolute inset-0 bg-[#020d04]/99 border border-[#00FF41]/80 rounded-md p-4 flex flex-col z-40 overflow-hidden font-mono text-left shadow-[0_0_20px_rgba(0,255,65,0.1)]"
                          >
                            {/* Title header */}
                            <div className="flex justify-between items-center border-b border-green-950 pb-1.5 mb-2 select-none">
                              <span className="text-[10px] text-[#00FF41] font-bold tracking-widest uppercase flex items-center gap-1.5 font-mono">
                                <span className="w-1.5 h-1.5 bg-[#00FF41] rounded-full animate-pulse" />
                                [ MOLECULAR STEP-SEARCH ]
                              </span>
                              <button
                                type="button"
                                onClick={() => {
                                  sound.playBeep(440, 0.05, "sine");
                                  setIsGeneLedgerExpanded(false);
                                }}
                                className="px-2 py-0.5 bg-red-950/80 hover:bg-red-900 border border-red-800 text-red-500 hover:text-white rounded text-[8px] cursor-pointer font-bold tracking-wider"
                              >
                                ✕ CLOSE
                              </button>
                            </div>

                            {/* Progress Monitor bar */}
                            <div className="bg-black/55 border border-green-955/40 p-2 rounded-sm text-left mb-2">
                              <div className="flex justify-between items-center text-[7.5px] text-green-500 font-bold uppercase tracking-wider mb-1">
                                <span>Hierarchical Filter Progress</span>
                                <span className="text-cyan-400 font-mono font-bold">Phase {discoverySearchStep + 1} / 4</span>
                              </div>

                              {/* Visual Phase Circles */}
                              <div className="grid grid-cols-4 gap-1 mb-1.5 text-center text-[8px] font-bold">
                                {Array.from({ length: 4 }).map((_, stepIdx) => {
                                  const stepLabels = ["1-2bp", "3-4bp", "5-6bp", "7-8bp"];
                                  const isPast = discoverySearchStep > stepIdx;
                                  const isCurrent = discoverySearchStep === stepIdx;
                                  return (
                                    <div
                                      key={stepIdx}
                                      className={`p-0.5 rounded-sm border text-[7.5px] transition-colors ${
                                        isCurrent
                                          ? "border-[#00FF41] bg-[#00FF41]/10 text-[#00FF41]"
                                          : isPast
                                            ? "border-green-950 text-green-600 bg-neutral-950"
                                            : "border-neutral-900 text-neutral-600 bg-neutral-950"
                                      }`}
                                    >
                                      {stepLabels[stepIdx]}
                                    </div>
                                  );
                                })}
                              </div>

                              {/* active prefix query breakdown */}
                              <div className="flex justify-between items-center bg-black/80 px-2 py-1 rounded border border-green-950/60 text-[8px]">
                                <div className="text-[9px] tracking-widest font-bold flex items-center gap-0.5">
                                  <span className="text-neutral-500 font-normal uppercase text-[6.5px] mr-1">Prefix:</span>
                                  {Array.from({ length: 4 }).map((_, stepIdx) => {
                                    const chunk = discoverySearchPrefix.substring(stepIdx * 2, (stepIdx + 1) * 2);
                                    const isActive = discoverySearchStep === stepIdx;
                                    return (
                                      <span
                                        key={stepIdx}
                                        className={`px-0.5 rounded-sm font-extrabold font-mono ${
                                          isActive
                                            ? "bg-[#00FF41]/15 text-white border border-green-500/40 animate-pulse"
                                            : chunk
                                              ? "text-[#00FF41] bg-[#00FF41]/5"
                                              : "text-neutral-700"
                                        }`}
                                      >
                                        {chunk || "••"}
                                      </span>
                                    );
                                  })}
                                </div>

                                <div className="flex gap-1 font-mono">
                                  {discoverySearchStep > 0 && (
                                    <button
                                      onClick={() => {
                                        sound.playBeep(350, 0.05, "sine");
                                        setDiscoverySearchPrefix((prev) => prev.substring(0, prev.length - 2));
                                        setDiscoverySearchStep((prev) => prev - 1);
                                      }}
                                      className="px-1.5 py-0.5 bg-neutral-900 border border-neutral-750 text-neutral-400 hover:text-white rounded text-[7px] cursor-pointer font-bold uppercase select-none transition-colors"
                                    >
                                      Undo
                                    </button>
                                  )}
                                  <button
                                    onClick={() => {
                                      sound.playBeep(250, 0.05, "sine");
                                      setDiscoverySearchPrefix("");
                                      setDiscoverySearchStep(0);
                                    }}
                                    className="px-1.5 py-0.5 bg-red-955/20 border border-red-955/40 text-red-500 hover:bg-red-955/40 rounded text-[7px] cursor-pointer font-bold uppercase select-none transition-colors"
                                  >
                                    Reset
                                  </button>
                                </div>
                              </div>
                            </div>

                            {/* Chromosome selection layout */}
                            <div className="flex-1 overflow-y-auto pr-0.5 custom-pox-scrollbar">
                              {discoverySearchStep < 4 ? (
                                <div className="space-y-1.5">
                                  <div className="text-[7.5px] uppercase font-bold text-green-700 font-mono tracking-widest text-left select-none">
                                    Select Couple to filter sequences
                                  </div>
                                  <div className="grid grid-cols-4 gap-1">
                                    {(() => {
                                      const OPTIONS = [
                                        "AA", "AC", "AG", "AT",
                                        "CA", "CC", "CG", "CT",
                                        "GA", "GC", "GG", "GT",
                                        "TA", "TC", "TG", "TT"
                                      ];
                                      const loggedUniqueGenes = Array.from(new Set([
                                        ...discoveredPacketsLog.flatMap(p => p.genes.map(c => c.sequence)),
                                        ...sequences.map(s => s.sequence)
                                      ]));

                                      return OPTIONS.map(pair => {
                                        const tentative = discoverySearchPrefix + pair;
                                        const matches = loggedUniqueGenes.filter(seq => seq.toUpperCase().startsWith(tentative));
                                        const count = matches.length;
                                        const available = count > 0;

                                        return (
                                          <button
                                            key={pair}
                                            disabled={!available}
                                            onClick={() => {
                                              sound.playBeep(480, 0.04, "sine");
                                              setDiscoverySearchPrefix(tentative);
                                              setDiscoverySearchStep(prev => prev + 1);
                                              if (matches.length > 0) {
                                                setDiscoverySelectedGene(matches[0]);
                                              }
                                            }}
                                            className={`py-1.5 border rounded-sm font-mono text-center select-none transition-all ${
                                              available
                                                ? "bg-black hover:bg-green-955/15 hover:border-green-400 text-white border-green-955 cursor-pointer"
                                                : "bg-neutral-950 border-neutral-900 text-neutral-600 opacity-20 cursor-not-allowed"
                                            }`}
                                          >
                                            <div className="font-extrabold text-[9px] tracking-wider font-mono">
                                              {pair}
                                            </div>
                                            {available && (
                                              <div className="text-[6px] text-[#00FF41] font-bold tracking-tighter">
                                                {count} types
                                              </div>
                                            )}
                                          </button>
                                        );
                                      });
                                    })()}
                                  </div>
                                </div>
                              ) : (
                                /* Step results view */
                                <div className="space-y-1.5">
                                  <div className="text-[7.5px] uppercase font-bold text-green-500 font-mono tracking-widest text-left flex justify-between select-none">
                                    <span>Resolved Molecular Matches</span>
                                    <button 
                                      onClick={() => {
                                        sound.playBeep(300, 0.05, "sine");
                                        setDiscoverySearchPrefix("");
                                        setDiscoverySearchStep(0);
                                      }}
                                      className="text-cyan-400 hover:underline cursor-pointer text-[7px]"
                                    >
                                      Restart Search
                                    </button>
                                  </div>
                                  <div className="grid grid-cols-1 gap-1">
                                    {(() => {
                                      const loggedUniqueGenes = Array.from(new Set([
                                        ...discoveredPacketsLog.flatMap(p => p.genes.map(c => c.sequence)),
                                        ...sequences.map(s => s.sequence)
                                      ]));
                                      const matches = loggedUniqueGenes.filter(seq => seq.toUpperCase().startsWith(discoverySearchPrefix));

                                      if (matches.length === 0) {
                                        return (
                                          <div className="text-center py-4 text-[8px] text-green-700 italic">
                                            No matching standard or anomalous genes found.
                                          </div>
                                        );
                                      }

                                      return matches.map((seq, idx) => {
                                        const isAnom = isAnomalousGene(seq);
                                        const isSel = discoverySelectedGene === seq;
                                        return (
                                          <div
                                            key={idx}
                                            onClick={() => {
                                              sound.playBeep(450, 0.05, "sine");
                                              setDiscoverySelectedGene(seq);
                                            }}
                                            className={`py-1 px-1.5 flex justify-between items-center rounded border cursor-pointer select-none transition-all ${
                                              isSel
                                                ? isAnom
                                                  ? "border-purple-400 bg-purple-950/20 text-purple-300 shadow-[0_0_8px_rgba(168,85,247,0.2)]"
                                                  : "border-[#00FF41] bg-green-955/25 text-[#00FF41] shadow-[0_0_8px_rgba(0,255,65,0.15)]"
                                                : "border-green-95b/60 bg-black/45 hover:border-green-800 text-zinc-300"
                                            }`}
                                          >
                                            <span className={`text-[8.5px] font-bold tracking-widest font-mono ${isAnom ? "text-purple-400" : "text-white"}`}>
                                              {seq}
                                            </span>
                                            <span className={`text-[6px] uppercase font-bold border px-1 rounded ${
                                              isAnom 
                                                ? "border-purple-500/30 bg-purple-950/40 text-purple-400" 
                                                : "border-green-955 bg-green-955/35 text-green-500"
                                            }`}>
                                              {isAnom ? "Anomaly" : "Standard"}
                                            </span>
                                          </div>
                                        );
                                      });
                                    })()}
                                  </div>
                                </div>
                              )}
                            </div>

                            <div className="text-[7.5px] text-zinc-500/70 leading-normal font-mono pt-1.5 border-t border-green-950 mt-2 select-none uppercase">
                              Query archives using hierarchical molecular couplers.
                            </div>
                          </motion.div>
                        )}

                        {isAnomalousLedgerExpanded && (
                          <motion.div
                            key="anomalous-genes-popup"
                            initial={{ opacity: 0, scale: 0.98 }}
                            animate={{ opacity: 1, scale: 1 }}
                            exit={{ opacity: 0, scale: 0.98 }}
                            className="absolute inset-0 bg-[#0c0512]/98 border border-purple-500/60 rounded-md p-4 flex flex-col z-40 overflow-hidden font-mono text-left"
                          >
                            {/* Title header */}
                            <div className="flex justify-between items-center border-b border-purple-955 pb-2 mb-2 select-none">
                              <span className="text-[10px] text-purple-400 font-bold tracking-widest uppercase flex items-center gap-1.5 font-mono">
                                <span className="w-1.5 h-1.5 bg-purple-500 rounded-full animate-ping" />
                                <FlickeringPurpleText text="[ ANOMALOUS GENE VAULT ]" />
                              </span>
                              <button
                                type="button"
                                onClick={() => {
                                  sound.playBeep(440, 0.05, "sine");
                                  setIsAnomalousLedgerExpanded(false);
                                }}
                                className="px-2 py-0.5 bg-red-950/80 hover:bg-red-900 border border-red-800 text-red-500 hover:text-white rounded text-[8.5px] cursor-pointer font-bold tracking-wider"
                              >
                                ✕ CLOSE
                              </button>
                            </div>

                            <div className="text-[8px] text-purple-500 uppercase tracking-widest mb-2 border-b border-purple-955 pb-1 flex justify-between select-none font-mono font-bold">
                              <span><FlickeringPurpleText text="ANOMALOUS GENES LISTED BELOW" /></span>
                              <span>{sequences.filter(s => isAnomalousGene(s.sequence)).length} ANOMALOUS GENES SECURED</span>
                            </div>

                            {/* Scrollable list of Anomalous Genes */}
                            <div className="flex-1 overflow-y-auto space-y-1.5 pr-0.5 custom-pox-scrollbar font-mono">
                              {sequences.filter(s => isAnomalousGene(s.sequence)).sort((a, b) => b.count - a.count).map((item, idx) => {
                                const benefits = getBenefitForAnomalousGene(item.sequence);
                                return (
                                  <div 
                                    key={idx}
                                    className="bg-purple-955/10 p-2.5 border border-purple-500/30 rounded flex flex-col gap-1.5 hover:bg-purple-500/5 transition-all font-mono"
                                  >
                                    <div className="flex justify-between items-center">
                                      <div className="flex items-center gap-2 font-mono">
                                        <span className="w-1.5 h-1.5 bg-purple-500 rounded-full animate-pulse font-mono" />
                                        <span className="text-xs font-bold text-purple-300 tracking-widest select-all font-mono">{item.sequence}</span>
                                      </div>
                                      <div className="flex items-center gap-1.5 font-mono">
                                        <span className="text-purple-400 font-semibold text-[8px] font-mono">STOCK:</span>
                                        <span className="bg-purple-900/40 text-white px-2 py-0.5 rounded text-xs border border-purple-500/30 font-bold font-mono">
                                          x{item.count}
                                        </span>
                                      </div>
                                    </div>
                                    <div className="text-[9.5px] text-zinc-300 bg-black/60 border border-purple-950/40 px-2.5 py-1.5 rounded font-mono leading-relaxed">
                                      <span className="text-purple-400 font-bold uppercase text-[8px]">PERK:</span> {benefits.name} - <span className="text-gray-400">{benefits.description}</span>
                                    </div>
                                  </div>
                                );
                              })}
                              {sequences.filter(s => isAnomalousGene(s.sequence)).length === 0 && (
                                <p className="text-center py-6 text-xs text-purple-400 font-mono italic">No anomalous genes resolved. Harvest deep thermal heat spots via the map scanner.</p>
                              )}
                            </div>

                            <div className="text-[9px] text-purple-500 leading-relaxed font-mono pt-2 border-t border-purple-955 mt-2 select-none">
                              Provides structural augmentation to whole custom combat squads.
                            </div>
                          </motion.div>
                        )}
                      </AnimatePresence>
                    </div>

                    </div>

                  </motion.div>
                )}

                {activeTab === 'splicer' && (
                  <motion.div 
                    key="splicer"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    className="grid grid-cols-1 md:grid-cols-2 gap-4 h-full"
                  >
                    
                    {/* Panel 2: Target Constructor (Left) */}
                    <div className="bg-neutral-900/20 border border-green-900/40 p-5 rounded flex flex-col justify-between">
                      {isForcedConstructionActive ? (
                        <div className="flex-1 flex flex-col justify-between p-4 bg-black/90 border border-red-950/80 rounded relative min-h-[350px]">
                          <div>
                            <div className="flex justify-between items-center mb-2 text-[9px] uppercase text-red-500 font-bold tracking-widest animate-pulse">
                              <span>[ EMERGENCY FORCED COMPILATION ]</span>
                              <span>REACTOR STATUS: FROZEN ({reactorFreezeTimeLeft}s)</span>
                            </div>
                            <h3 className="text-xs font-black text-white uppercase tracking-wider mb-2">BYPASSING GENE COMPILATION LOCKS</h3>
                          </div>

                          {/* Scrolling Terminal Output logs */}
                          <div 
                            ref={forcedLogContainerRef}
                            className="flex-1 my-3 p-2.5 bg-neutral-950 border border-red-950/60 rounded font-mono text-[9px] text-[#00FF41] overflow-y-auto space-y-1.5 max-h-[220px] select-text scrollbar-thin"
                          >
                            {forcedConstructionLogs.map((log, idx) => (
                              <div key={idx} className={log.includes("FAILED") || log.includes("WARNING") || log.includes("Failed") || log.includes("Sacrificed") ? "text-red-400 font-semibold" : "text-emerald-400"}>
                                {log}
                              </div>
                            ))}
                          </div>

                          <div className="space-y-2">
                            <div className="flex items-center gap-1.5 justify-center py-2 bg-red-950/30 border border-red-900/50 rounded">
                              <span className="w-2.5 h-2.5 bg-red-500 rounded-full animate-ping" />
                              <span className="text-[9px] text-red-400 font-extrabold uppercase tracking-widest text-center px-1">
                                {isForcedLoopActive ? "LOOP CASCADE ACTIVE: REACTOR COILS FROZEN" : `FREEZING REACTOR GENE CYCLE: ${reactorFreezeTimeLeft}s REMAINING`}
                              </span>
                            </div>

                            {isForcedLoopActive && (
                              <button
                                onClick={() => {
                                  sound.playReject();
                                  setIsForcedLoopActive(false);
                                  triggerLog("ABORTING LOOP PACKET CASCADE... Reactor will complete current cycle and warm up.", "warn");
                                }}
                                className="w-full py-1.5 bg-red-900/40 hover:bg-red-950 border border-red-500/80 text-white font-bold uppercase text-[9px] font-mono tracking-widest cursor-pointer rounded-sm active:scale-[0.99] transition-all flex items-center justify-center gap-1.5"
                              >
                                ✕ EXIT AUTO-SYNTHESIS LOOP CASCADE
                              </button>
                            )}
                          </div>
                        </div>
                      ) : isSplicing ? (
                        <div className="flex-1 flex flex-col items-center justify-center p-6 text-center">
                          <motion.div 
                            animate={{ rotate: 360 }}
                            transition={{ repeat: Infinity, duration: 2, ease: "linear" }}
                            className="text-[#00FF41] mb-4"
                          >
                            <Dna className="w-14 h-14" />
                          </motion.div>
                          <h3 className="text-sm font-bold text-[#00FF41] tracking-widest uppercase animate-pulse">AUTOTRONIC MORPHOGENESIS ENGINE ENGAGED</h3>
                          <p className="text-[10px] text-green-600 max-w-xs mt-2 leading-relaxed">Processing 64-character sequencing algorithm using premium cyber insect DNA matrices...</p>
                          <div className="w-48 bg-neutral-900 border border-green-950 rounded-full h-3 mt-4 p-0.5 overflow-hidden">
                            <div className="bg-[#00FF41] h-full rounded-sm shadow-[0_0_8px_#00FF41]" style={{ width: `${splicingProgress}%` }} />
                          </div>
                          <span className="text-[9px] text-green-700 mt-2 uppercase font-mono tracking-widest">SPLICING PACKETS BUFFER: {splicingProgress}%</span>
                        </div>
                      ) : (
                        <div className="flex flex-col justify-between h-full">
                          <div>
                            <div className="flex justify-between items-center mb-1 text-[10px] uppercase text-green-700">
                              <span>[ CREATURE CONSTRUCTOR ]</span>
                              <span>64-CHAR ASSEMBLY GRID</span>
                            </div>
                            <h2 className="text-xs font-bold text-white tracking-wider mb-2">TARGET GENOME RE-SEQUENCING</h2>
                            <p className="text-[10px] text-green-700 mb-3 font-mono leading-tight">Fill all slots with stockpiled genes to assemble the target genome.</p>

                            {/* Dash grid representations of 8 slots */}
                            <div className="grid grid-cols-4 gap-1.5 bg-black p-2 rounded border border-green-900/40 mb-3">
                              {splicerSlots.map((slot, idx) => {
                                const isAnom = slot ? isAnomalousGene(slot) : false;
                                return (
                                  <div 
                                    key={idx}
                                    onClick={() => handleSlotClick(idx)}
                                    className={`relative h-10 rounded flex flex-col items-center justify-center border transition-all cursor-pointer ${
                                      activeSlotSelection === idx 
                                        ? isAnom
                                          ? 'border-purple-400 bg-purple-950/35 shadow-[0_0_15px_rgba(168,85,247,0.4)]'
                                          : 'border-[#00FF41] bg-green-950/20 shadow-[0_0_8px_rgba(0,255,102,0.15)]' 
                                        : slot 
                                          ? isAnom
                                            ? 'border-purple-500 bg-purple-950/25 shadow-[0_0_10px_rgba(168,85,247,0.25)]'
                                            : 'border-green-800 bg-neutral-900/60' 
                                          : 'border-green-900/20 border-dashed hover:border-green-500 hover:bg-neutral-900/40'
                                    }`}
                                  >
                                    <span className="text-[7.5px] text-green-700 absolute top-0.5 left-1 select-none font-sans">#{idx + 1}</span>
                                    {slot ? (
                                      <>
                                        <span className={`text-[10px] font-bold tracking-widest leading-none mt-1 ${isAnom ? 'text-purple-300 filter drop-shadow-[0_0_4px_#9333ea]' : 'text-[#00FF41]'}`}>{slot.slice(0,4)}</span>
                                        <span className={`text-[8px] font-semibold tracking-wider leading-none ${isAnom ? 'text-purple-400' : 'text-green-500'}`}>{slot.slice(4)}</span>
                                        <button 
                                          onClick={(e) => handleEjectSlot(idx, e)}
                                          className="absolute -top-1 -right-1 bg-red-950 text-red-500 hover:bg-red-900 border border-red-900 rounded-full w-3.5 h-3.5 flex items-center justify-center text-[7px]"
                                          title="Eject gene block"
                                        >
                                          ×
                                        </button>
                                      </>
                                    ) : (
                                      <span className="text-green-950 text-[10px] font-bold tracking-widest">----</span>
                                    )}
                                  </div>
                                );
                              })}
                            </div>
                          </div>

                          <div className="space-y-3">
                            {/* Required literal target sequence */}
                            <div className="p-2.5 bg-[#050c06] border border-[#00FF41]/40 rounded shadow-[inset_0_0_8px_rgba(0,255,102,0.1)] text-left">
                              <div className="flex justify-between items-center mb-1 select-none">
                                <span className="text-[8.5px] text-[#00FF41] font-bold uppercase tracking-widest">[ Required Target Sequence ]</span>
                                <span className="text-[8px] text-green-500 font-mono font-bold">64-CHAR GENOME GOAL</span>
                              </div>
                              <div className="break-all text-[11px] leading-relaxed font-mono select-all font-extrabold tracking-widest flex flex-wrap gap-x-2 gap-y-1">
                                {Array.from({ length: 8 }).map((_, i) => {
                                  const segment = targetSequence.slice(i * 8, (i + 1) * 8);
                                  const isAnom = isAnomalousGene(segment);
                                  return (
                                    <span
                                      key={i}
                                      className={`${
                                        isAnom 
                                          ? 'text-purple-400 bg-purple-950/20 shadow-[0_0_8px_rgba(168,85,247,0.5)] border border-purple-500/30 px-1 py-0.5 rounded font-bold' 
                                          : i % 4 === 0 ? 'text-[#00FF41]' :
                                            i % 4 === 1 ? 'text-amber-400' :
                                            i % 4 === 2 ? 'text-blue-400' : 'text-purple-400'
                                      }`}
                                    >
                                      {segment}
                                    </span>
                                  );
                                })}
                              </div>
                            </div>

                            {/* Currently constructed DNA string */}
                            <div className="p-2.5 bg-black border border-dashed border-green-900 rounded text-left">
                              <div className="text-[8.5px] text-green-600 mb-1 font-bold uppercase tracking-widest select-none">[ Your Current Spliced Specimen ]</div>
                              <div className="break-all text-[11px] text-neutral-400 leading-relaxed font-mono font-bold tracking-widest">
                                {splicerSlots.map(s => s || "--------").join("")}
                              </div>
                            </div>

                            <div className="flex gap-2">
                              <button
                                onClick={handleAutofillSplicer}
                                className="px-3 py-2 border border-green-500/50 text-white font-bold text-xs hover:bg-green-900/20 rounded-sm active:scale-[0.98] transition-all cursor-pointer font-mono whitespace-nowrap"
                              >
                                AUTO SLOT
                              </button>
                              <button
                                onClick={handleConstructCreature}
                                disabled={splicerSlots.includes(null)}
                                className={`flex-1 py-2 rounded text-xs font-bold uppercase tracking-widest transition-all ${
                                  splicerSlots.includes(null)
                                    ? "bg-green-950/20 text-green-900 border border-green-950 text-center cursor-not-allowed"
                                    : "bg-[#00FF41] text-black hover:bg-green-400 active:scale-[0.98] cursor-pointer shadow-[0_0_12px_rgba(0,255,65,0.3)] font-mono"
                                }`}
                              >
                                {splicerSlots.includes(null) ? "SYNTHESIZE GENOME" : "INITIALIZE MORPHOGENESIS"}
                              </button>
                            </div>

                            <div className="space-y-2">
                              <div className="flex gap-2">
                                <button
                                  type="button"
                                  onClick={() => {
                                    setIsForcedLoopActive(false);
                                    handleForceConstruction();
                                  }}
                                  disabled={isSplicing || isForcedConstructionActive}
                                  className="flex-1 py-1.5 bg-red-950/20 hover:bg-neutral-900 border border-red-950 text-red-500 hover:text-red-400 text-[10px] font-mono hover:border-red-500 rounded cursor-pointer font-bold tracking-wider active:scale-[0.98] transition-all flex items-center justify-center gap-1"
                                >
                                  <Zap className="w-3.5 h-3.5 text-red-500 animate-pulse" />
                                  FORCE SINGLE
                                </button>
                                <button
                                  type="button"
                                  onClick={() => {
                                    setIsForcedLoopActive(true);
                                    handleForceConstruction();
                                  }}
                                  disabled={isSplicing || isForcedConstructionActive}
                                  className="flex-1 py-1.5 bg-red-950/25 hover:bg-red-950/60 leading-tight border border-red-500 rounded text-red-400 text-[10px] font-mono hover:shadow-[0_0_10px_rgba(239,68,68,0.2)] flex flex-col items-center justify-center cursor-pointer font-bold tracking-wider active:scale-[0.98] transition-all"
                                >
                                  <span className="flex items-center gap-1 text-red-500 uppercase font-extrabold text-[10px]">
                                    <ShieldAlert className="w-3.5 h-3.5 text-red-500 animate-pulse" />
                                    FORCE AUTO-LOOP
                                  </span>
                                </button>
                              </div>
                              <span className="text-[7.5px] text-red-400/80 leading-tight block uppercase font-semibold text-center select-all font-mono">
                                Warning: Freezes bio-lab reactor for 8s; gene block attrition is 37.5%
                              </span>
                            </div>
                          </div>
                        </div>
                      )}
                    </div>

                    {/* Splicer select gene inventory (Right) */}
                    <div className="bg-neutral-900/20 border border-green-900/40 p-5 rounded flex flex-col justify-between">
                      {activeSlotSelection !== null ? (
                        <div className="flex flex-col justify-between h-full">
                          <div>
                            <div className="flex justify-between items-center mb-1 text-[10px] uppercase text-[#00FF41]">
                              <span>ASSIGN SLOT PROTOCOL #{activeSlotSelection + 1}</span>
                              <button onClick={() => { sound.playBeep(450,0.05,"sine"); setActiveSlotSelection(null); }} className="text-red-500 hover:text-white font-bold cursor-pointer">[ CANCEL ]</button>
                            </div>
                            <h2 className="text-xs font-bold text-white tracking-wider mb-1">CHOOSE GENE SEGMENT</h2>
                            <p className="text-[10px] text-green-700 font-mono mb-2 leading-tight">Selecting one gene stock decants it directly into the assembly matrix.</p>
                            
                            {/* Expected gene info block */}
                            <div className="bg-green-950/20 border border-green-500/30 p-2 rounded mb-3 text-left animate-fade-in">
                              <div className="text-[8.5px] text-[#00FF41] font-bold tracking-wider uppercase">[ REQUIRED SEGMENT FOR SLOT #{activeSlotSelection + 1} ]</div>
                              <div className="font-mono text-cyan-400 font-extrabold text-sm tracking-widest mt-0.5">
                                {targetSequence.substring(activeSlotSelection * 8, (activeSlotSelection + 1) * 8)}
                              </div>
                            </div>
                          </div>

                           {/* Filter Input for Slot Selector List */}
                           <div className="flex items-center gap-2 bg-black/80 border border-green-950/80 p-1.5 rounded mb-3 text-left">
                             <Search className="w-3.5 h-3.5 text-green-700 shrink-0" />
                             <input
                               type="text"
                               placeholder="FILTER BY SEQUENCE..."
                               value={slotSequenceFilter}
                               onChange={(e) => {
                                 sound.playBeep(450, 0.02, "sine");
                                 setSlotSequenceFilter(e.target.value.toUpperCase());
                               }}
                               className="bg-transparent border-0 text-[10px] text-white focus:outline-none focus:ring-0 w-full placeholder-green-900 uppercase font-mono"
                             />
                             {slotSequenceFilter && (
                               <button 
                                 onClick={() => setSlotSequenceFilter("")} 
                                 className="text-red-500 text-[8px] hover:underline uppercase shrink-0 font-bold font-mono"
                               >
                                 Clear
                               </button>
                             )}
                           </div>


                          </div>
                        ) : (
                          <div className="flex-grow flex flex-col items-center justify-center text-center p-6 border-2 border-dashed border-green-950/40 rounded bg-black/10 select-none">
                            <FolderOpen className="w-10 h-10 text-green-800 mb-2 animate-pulse" />
                            <span className="text-[10px] font-bold tracking-wider text-green-600 font-mono uppercase mb-1">
                              [ NO ACTIVE CONSTRUCTOR SLOT SELECTION ]
                            </span>
                            <p className="text-[9.5px] text-green-700 font-mono max-w-xs leading-normal">
                              Choose a gene slot on the left panel to display compatible genes
                            </p>
                          </div>
                        )}
                      </div>
                    </motion.div>
                  )}

                  {activeTab === 'library' && (
                    <motion.div
                      key="library"
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      exit={{ opacity: 0, y: -10 }}
                      className="h-full flex flex-col min-h-0"
                    >
                      {inspectedCreatureId ? (
                        (() => {
                          const bot = creatures.find(c => c.id === inspectedCreatureId);
                          if (!bot) return null;
                          return (
                            <div className="bg-neutral-900/20 border border-green-900/40 p-4 rounded flex-1 flex flex-col justify-between overflow-y-auto animate-fade-in">
                              <div className="space-y-3.5">
                                <div className="flex justify-between items-start border-b border-green-900/30 pb-2">
                                  <div>
                                    <div className="flex items-center gap-2 flex-wrap">
                                      <h3 className="text-sm font-bold text-white uppercase tracking-wider">{bot.name}</h3>
                                      <span className="text-[8.5px] font-mono tracking-widest text-[#00FF41]">{bot.id}</span>
                                      {defenderCreatureId === bot.id && (
                                        <span className="bg-blue-950 border border-blue-400/50 text-blue-400 px-1.5 py-0.5 rounded text-[7.5px] font-bold font-sans flex items-center gap-0.5 shadow-[0_0_6px_rgba(59,130,246,0.2)] select-none">
                                          <Shield className="w-2.5 h-2.5" /> DEFENDER
                                        </span>
                                      )}
                                      {getCoherence(bot.sequence, targetSequence) === "full" ? (
                                        <span className="px-1.5 py-0.5 bg-emerald-950 text-[#00FF41] border border-emerald-500 rounded text-[7.5px] font-bold font-mono tracking-widest animate-pulse select-none">
                                          FULL COHERENCE
                                        </span>
                                      ) : getCoherence(bot.sequence, targetSequence) === "partial" ? (
                                        <span className="px-1.5 py-0.5 bg-yellow-950/80 text-yellow-500 border border-yellow-600/55 rounded text-[7.5px] font-bold font-mono tracking-wider select-none">
                                          PARTIAL COHERENCE
                                        </span>
                                      ) : null}
                                    </div>
                                    <span className="text-[10px] text-green-700 block font-mono mt-0.5">{bot.type}</span>
                                  </div>
                                  <span className={`px-2 py-0.5 rounded text-[10px] font-bold text-black uppercase tracking-wider ${
                                    bot.faction === "Infection" ? "bg-red-500 font-sans" :
                                    bot.faction === "Mech" ? "bg-blue-400 font-sans" :
                                    bot.faction === "Parasite" ? "bg-purple-500 font-sans" : "bg-green-400 font-sans"
                                  }`}>
                                    {bot.faction}
                                  </span>
                                </div>

                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                  <div className="space-y-2">
                                    <div className="grid grid-cols-2 gap-2">
                                      <div className="bg-black border border-green-900/40 hover:border-green-500/30 p-2.5 rounded flex flex-col items-center justify-center font-mono text-center text-xs leading-none text-[#00FF41] select-none transition-colors">
                                        <span className="text-[7px] text-green-700 font-bold uppercase tracking-widest mb-2 border-b border-green-950 pb-1 w-full block">BIO-PHYSICAL RECON</span>
                                        <pre className="tracking-widest font-bold font-mono my-auto py-1 leading-[1.1]">{bot.asciiArt}</pre>
                                      </div>
                                      {(() => {
                                        const qrData = JSON.stringify({
                                          id: bot.id,
                                          name: bot.name,
                                          seq: bot.sequence,
                                          stats: { hp: bot.vitality, atk: bot.attack, def: bot.defense, spd: bot.speed },
                                          appended: bot.appendedGenes || [],
                                          moves: getUnlockedMoves(bot.sequence).map(m => m.name),
                                          visual: { faction: bot.faction, type: bot.type, ascii: bot.asciiArt },
                                          sound: getEmotSoundDetails(bot.sequence)
                                        });
                                        return (
                                          <div 
                                            onClick={() => { sound.playBeep(880, 0.05, "sine"); setEnlargedQrCreatureId(bot.id); decreaseTelomeres(bot.id, 15, "QR coupling utilization"); }}
                                            title="Click to expand genetic transponder"
                                            className="bg-black border border-green-900/40 hover:border-green-500/60 p-2.5 rounded flex flex-col items-center justify-center text-center select-none cursor-pointer transition-all hover:shadow-[0_0_12px_rgba(0,255,65,0.15)] group relative min-h-[92px]"
                                          >
                                            <span className="text-[7px] text-green-750 font-black uppercase tracking-widest mb-1 w-full text-center truncate group-hover:text-[#00FF41] transition-colors">
                                              GENETIC QR COUPLING
                                            </span>
                                            <div className="p-1 bg-black/90 border border-green-950 group-hover:border-green-500/40 rounded flex items-center justify-center transition-all bg-neutral-950 shadow-[inset_0_0_6px_rgba(0,0,0,0.8)]">
                                              <QRCodeSVG 
                                                value={qrData} 
                                                size={56} 
                                                bgColor="#000000" 
                                                fgColor="#00FF41" 
                                                level="L" 
                                                includeMargin={false} 
                                              />
                                            </div>
                                            <span className="text-[5.5px] text-zinc-500 font-bold font-mono tracking-widest uppercase mt-1 leading-none group-hover:text-[#00FF41]/80 transition-colors animate-pulse">
                                              [ CLICK TO ENLARGE ]
                                            </span>
                                          </div>
                                        );
                                      })()}
                                    </div>

                                    <div className="bg-neutral-950 p-2 border border-green-900/30 rounded">
                                      <span className="text-[8.5px] text-green-700 tracking-wider block font-bold uppercase mb-1">64-CHARACTER INTEGRATION SEQUENCE</span>
                                      <div className="font-mono text-[9.5px] leading-tight break-all font-bold tracking-widest dark:text-white flex flex-wrap">
                                        {Array.from({ length: 8 }).map((_, i) => {
                                          const cSeq = bot.sequence.slice(i * 8, (i + 1) * 8);
                                          const isAnom = isAnomalousGene(cSeq);
                                          return (
                                          <span 
                                            key={i} 
                                            className={`px-[1px] ${
                                              isAnom 
                                                ? 'text-purple-400 bg-purple-950/20 shadow-[0_0_8px_rgba(168,85,247,0.5)] border border-purple-500/30 px-1 py-0.5 rounded font-bold' 
                                                : i % 4 === 0 ? 'text-[#00FF41]' :
                                                  i % 4 === 1 ? 'text-amber-400' :
                                                  i % 4 === 2 ? 'text-blue-400' : 'text-purple-400'
                                            }`}
                                            title={isAnom ? `Anomalous Gene Chunk ${i+1}` : `Gene ${i+1}`}
                                          >
                                            {cSeq}
                                          </span>
                                        );
                                      })}
                                    </div>
                                  </div>
                                </div>

                                {/* Right column: Weapon list, lore and Combat Stats bar */}
                                <div className="space-y-3 flex flex-col justify-between">
                                  <div className="bg-black/60 p-2.5 rounded border border-green-900/20 space-y-1">
                                    <span className="text-[8px] text-green-700 uppercase font-mono block select-none">ARMAMENT DESIGNATION</span>
                                    <span className="text-white font-bold tracking-wider text-xs block">{bot.primaryWeapon}</span>
                                    <p className="text-[10px] text-green-500/90 leading-relaxed font-mono italic">
                                      "{bot.lore}"
                                    </p>
                                  </div>

                                  {(() => {
                                    const eff = getEffectiveStats(bot);
                                    const telomeres = bot.telomeres !== undefined ? bot.telomeres : 100;
                                    const isDegraded = telomeres < 100;
                                    return (
                                      <div className="bg-black/40 p-2 rounded border border-green-900/20 space-y-1.5 text-[10px]">
                                        {/* HP */}
                                        <div className="flex items-center justify-between font-mono">
                                          <span className="flex items-center gap-1"><Heart className="w-3.5 h-3.5 text-red-500" /> VITALITY:</span>
                                          <span className="text-white font-bold text-xs">
                                            {eff.vitality} HP{isDegraded && <span className="text-red-500/80 text-[8.5px] font-normal ml-1">({bot.vitality})</span>}
                                          </span>
                                        </div>
                                        {/* ATK */}
                                        <div className="flex items-center justify-between">
                                          <span className="flex items-center gap-1"><Zap className="w-3.5 h-3.5 text-amber-400" /> AGGRESSIVITY:</span>
                                          <div className="flex items-center gap-2">
                                            <div className="w-20 bg-neutral-800 h-1.5 rounded overflow-hidden">
                                              <div className="bg-amber-400 h-full" style={{ width: `${eff.attack}%` }} />
                                            </div>
                                            <span className="text-white font-bold">
                                              {eff.attack}{isDegraded && <span className="text-red-500/80 text-[8.5px] font-normal ml-0.5">({bot.attack})</span>}
                                            </span>
                                          </div>
                                        </div>
                                        {/* DEF */}
                                        <div className="flex items-center justify-between">
                                          <span className="flex items-center gap-1"><Shield className="w-3.5 h-3.5 text-blue-400" /> BLOCK SHELLS:</span>
                                          <div className="flex items-center gap-2">
                                            <div className="w-20 bg-neutral-800 h-1.5 rounded overflow-hidden">
                                              <div className="bg-blue-400 h-full" style={{ width: `${eff.defense}%` }} />
                                            </div>
                                            <span className="text-white font-bold">
                                              {eff.defense}{isDegraded && <span className="text-red-500/80 text-[8.5px] font-normal ml-0.5">({bot.defense})</span>}
                                            </span>
                                          </div>
                                        </div>
                                        {/* SPD */}
                                        <div className="flex items-center justify-between">
                                          <span className="flex items-center gap-1"><Sparkles className="w-3.5 h-3.5 text-purple-400" /> SPEED RATE:</span>
                                          <div className="flex items-center gap-2">
                                            <div className="w-20 bg-neutral-800 h-1.5 rounded overflow-hidden">
                                              <div className="bg-purple-300 h-full" style={{ width: `${eff.speed}%` }} />
                                            </div>
                                            <span className="text-white font-bold">
                                              {eff.speed}{isDegraded && <span className="text-red-500/80 text-[8.5px] font-normal ml-0.5">({bot.speed})</span>}
                                            </span>
                                          </div>
                                        </div>

                                        {/* TELOMERE LIFE */}
                                        <div className="flex flex-col gap-0.5 pt-1.5 border-t border-green-950/60">
                                          <div className="flex items-center justify-between font-mono text-[9px]">
                                            <span className="flex items-center gap-1 text-green-400 font-bold"><Dna className="w-3 h-3 text-green-400 animate-pulse" /> TELOMERES:</span>
                                            <span className="text-white font-extrabold text-[10px]">{telomeres}%</span>
                                          </div>
                                          <div className="w-full bg-neutral-800 h-1.5 rounded overflow-hidden border border-green-950/40">
                                            <div 
                                              className={`h-full transition-all duration-350 ${
                                                telomeres > 65 ? 'bg-green-500' :
                                                telomeres > 30 ? 'bg-amber-500' :
                                                'bg-red-500 animate-pulse'
                                              }`}
                                              style={{ width: `${telomeres}%` }} 
                                            />
                                          </div>
                                        </div>
                                      </div>
                                    );
                                  })()}
                                </div>
                              </div>

                              {/* Unlocked Moves (for extended sequences) */}
                              {bot.sequence.length > 64 && (
                                <div className="p-3 bg-amber-950/20 border border-amber-900/40 rounded-lg select-none">
                                  <div className="text-[10px] text-amber-500 font-bold uppercase tracking-wider mb-2 flex items-center gap-1 font-mono">
                                    <Sparkles className="w-3.5 h-3.5 text-amber-400" /> UNLOCKED MUTANT SPECIAL ACTIONS ({getUnlockedMoves(bot.sequence).length}):
                                  </div>
                                  <div className="space-y-2">
                                    {getUnlockedMoves(bot.sequence).map((mv, mvIdx) => (
                                      <div key={mvIdx} className="bg-black/40 border border-amber-900/10 p-2 rounded flex flex-col font-mono text-[9px] text-[#00FF41]">
                                        <div className="flex justify-between items-center mb-0.5">
                                          <span className="font-bold tracking-wider text-amber-300 uppercase">
                                            [{mv.type.toUpperCase()}] {mv.name}
                                          </span>
                                          <span className="text-[8px] text-neutral-400">ACTIVE IN HACKS</span>
                                        </div>
                                        <span className="text-green-600 uppercase text-[8.5px] leading-snug">{mv.description}</span>
                                      </div>
                                    ))}
                                  </div>
                                </div>
                              )}
                            </div>

                            {/* Splicer Extraction Console */}
                            <div className="mt-4 p-3 bg-black/60 border border-red-900/40 rounded-lg space-y-3 font-sans">
                              <div className="flex justify-between items-center pb-1.5 border-b border-red-950">
                                <span className="text-[10px] text-red-400 font-bold tracking-wider uppercase flex items-center gap-1">
                                  <AlertTriangle className="w-3.5 h-3.5" /> GENE DESTRUCT-HARVEST MATRIX
                                </span>
                                <span className="text-[8px] text-neutral-500 font-mono">1 EXTRACTABLE NODE LIMIT</span>
                              </div>
                              
                              <p className="text-[10px] text-neutral-400 leading-normal">
                                Select exactly <strong className="text-[#00FF41]">one (1) GENE node</strong> below to harvest back into your Bio-Lab gene inventory. Doing so will permanently <strong className="text-red-500">incinerate this creature</strong> and delete the remaining genes.
                              </p>

                              <div className="grid grid-cols-4 sm:grid-cols-8 gap-1.5 font-mono">
                                {Array.from({ length: bot.sequence.length / 8 }).map((_, i) => {
                                  const gene = bot.sequence.slice(i * 8, (i + 1) * 8);
                                  const isSelected = selectedGeneIndex === i;
                                  const isAnom = isAnomalousGene(gene);
                                  const ownedCount = sequences.find(s => s.sequence === gene)?.count || 0;
                                  const doesNotExistInInventory = ownedCount === 0;
                                  return (
                                    <button
                                      key={i}
                                      type="button"
                                      onClick={() => {
                                        sound.playBeep(600 + i * 50, 0.05, "sine");
                                        setSelectedGeneIndex(i);
                                      }}
                                      className={`relative py-3 px-1 rounded border flex flex-col items-center justify-center transition-all cursor-pointer ${
                                        isSelected
                                          ? isAnom
                                            ? 'bg-purple-950/40 border-purple-500 shadow-[0_0_12px_rgba(168,85,247,0.4)] text-purple-300 font-bold'
                                            : 'bg-red-950/40 border-red-500 shadow-[0_0_8px_rgba(239,68,68,0.25)] text-[#00FF41] font-bold'
                                          : isAnom
                                            ? 'bg-purple-950/20 border-purple-900/60 text-purple-300 shadow-[0_0_8px_rgba(168,85,247,0.2)] hover:border-purple-400'
                                            : doesNotExistInInventory
                                              ? 'bg-amber-950/20 border-amber-500/50 text-amber-400 hover:border-amber-400 shadow-[0_0_4px_rgba(245,158,11,0.15)]'
                                              : 'bg-neutral-950 border-neutral-800 text-neutral-400 hover:border-red-900/40 hover:text-white'
                                      }`}
                                    >
                                      <span className="text-[7px] text-neutral-500 font-sans block absolute top-0.5 left-1">#{i + 1}</span>
                                      {doesNotExistInInventory && !isSelected && (
                                        <span className="absolute top-0.5 right-1.5 text-[6.5px] font-bold tracking-wider text-amber-400 uppercase font-sans">
                                          NEW
                                        </span>
                                      )}
                                      <span className="text-[10px] font-mono tracking-widest mt-1.5">{gene}</span>
                                    </button>
                                  );
                                })}
                              </div>

                              {selectedGeneIndex !== null && (
                                <div className="flex flex-col sm:flex-row items-center gap-2 pt-2 border-t border-red-950 mt-1">
                                  <span className="text-[9.5px] text-red-400 flex-grow leading-snug">
                                    CONVERT GENE <strong className="text-white bg-red-900/60 px-1 py-0.5 rounded tracking-widest font-mono">"{bot.sequence.slice(selectedGeneIndex * 8, (selectedGeneIndex + 1) * 8)}"</strong> INTO ARCHIVE STOCK. THIS CREATURE WILL BE PURGED.
                                  </span>
                                  <button
                                    type="button"
                                    onClick={() => {
                                      const pickedGene = bot.sequence.slice(selectedGeneIndex * 8, (selectedGeneIndex + 1) * 8);
                                      handleSpliceExtract(bot.id, pickedGene);
                                    }}
                                    className="w-full sm:w-auto px-4 py-2 bg-red-800 hover:bg-red-700 text-white font-bold rounded-sm uppercase text-xs cursor-pointer tracking-wider shrink-0 shadow-[0_0_12px_rgba(239,68,68,0.3)] transition-all active:scale-95"
                                  >
                                    ACTIVATE HARVEST INCINERATION
                                  </button>
                                </div>
                              )}
                            </div>

                            {/* Gene Sequence Extension Lab */}
                            <div className="mt-4 p-3 bg-black/60 border border-amber-900/40 rounded-lg space-y-3 font-sans">
                              <div className="flex justify-between items-center pb-1.5 border-b border-amber-950">
                                <span className="text-[10px] text-amber-400 font-bold tracking-wider uppercase flex items-center gap-1 font-sans">
                                  <Sparkles className="w-3.5 h-3.5 text-amber-500" /> GENE SEQUENCE EXTENSION LAB
                                </span>
                                <span className="text-[8px] text-neutral-500 font-mono">
                                  EXTENSIONS: {Math.max(0, (bot.sequence.length - 64) / 8)} / 2 MAXIMUM
                                </span>
                              </div>

                              <p className="text-[10px] text-neutral-400 leading-normal">
                                Combine custom 8-letter gene sequences from your Bio-Lab gene inventory onto this specimen to trigger stat recalibration and unlock dynamic combat maneuvers. Limit 2 additional extensions.
                              </p>

                              {bot.sequence.length < 80 ? (
                                <div className="space-y-3">
                                  {(() => {
                                    const lastFour = bot.sequence.slice(-4);
                                    const prefixToMatch = lastFour.split("").reverse().join("");
                                    const matchingGenes = sequences.filter(s => s.count > 0 && s.sequence.substring(0, 4) === prefixToMatch);
                                    
                                    return (
                                      <>
                                        <div className="bg-amber-950/20 border border-amber-900/35 p-2 rounded text-[9.5px] text-amber-500 font-mono leading-relaxed">
                                          <span className="font-extrabold uppercase text-amber-400 block mb-0.5">🧬 GENE ALIGNMENT RULE:</span>
                                          The next gene's first 4 letters must mirror/reverse the active target's suffix <span className="text-white underline font-bold px-0.5 bg-zinc-900 rounded font-mono">"{lastFour}"</span> &rarr; <span className="text-amber-300 font-bold bg-amber-950/60 px-1 py-0.5 rounded border border-amber-800/40 font-mono">Must start with "{prefixToMatch}"</span>.
                                        </div>

                                        <div className="text-[9.5px] uppercase font-bold text-amber-500 select-none font-mono">
                                          AVAILABLE GENE STOCKPILES IN INVENTORY:
                                        </div>

                                        {matchingGenes.length > 0 ? (
                                          <div className="grid grid-cols-2 sm:grid-cols-4 gap-1.5 max-h-36 overflow-y-auto custom-pox-scrollbar pr-1 font-mono">
                                            {matchingGenes.map((seqObj) => {
                                              const isChosen = selectedExtensionSequence === seqObj.sequence;
                                              return (
                                                <button
                                                  key={seqObj.sequence}
                                                  type="button"
                                                  onClick={() => {
                                                    sound.playBeep(480, 0.05, "sine");
                                                    setSelectedExtensionSequence(isChosen ? null : seqObj.sequence);
                                                  }}
                                                  className={`relative py-2 px-1.5 rounded border flex flex-col items-center justify-center transition-all cursor-pointer ${
                                                    isChosen
                                                      ? 'bg-amber-950/45 border-amber-500 text-amber-400 font-bold shadow-[0_0_8px_rgba(245,158,11,0.25)] font-mono'
                                                      : 'bg-neutral-950 border-neutral-800 text-neutral-400 hover:border-amber-950 hover:text-white font-mono'
                                                  }`}
                                                >
                                                  <span className="text-[9.5px] font-mono tracking-wider font-semibold">{seqObj.sequence}</span>
                                                  <span className="text-[7.5px] text-amber-700 font-mono mt-0.5">Stock: {seqObj.count}</span>
                                                </button>
                                              );
                                            })}
                                          </div>
                                        ) : (
                                          <div className="p-2.5 bg-neutral-950 border border-neutral-800 rounded font-mono text-[9px] text-amber-600 leading-normal">
                                            ✕ No matching gene blocks in stock that start with <strong className="text-white select-all bg-amber-950/40 px-1 rounded">"{prefixToMatch}"</strong>.
                                            <br />
                                            Please visit the <strong className="text-white bg-slate-900 border border-zinc-700 px-1 py-0.5 rounded font-mono">DNA COMBINATOR</strong> tab to synthesize patterns starting with this prefix.
                                          </div>
                                        )}
                                      </>
                                    );
                                  })()}

                                  {selectedExtensionSequence && (
                                    <div className="flex flex-col sm:flex-row items-center gap-2 pt-2 border-t border-amber-950/30 mt-1">
                                      <span className="text-[9px] text-amber-500 flex-grow leading-normal">
                                        APPEND GENE <strong className="text-white bg-amber-950/60 px-1 py-0.5 rounded tracking-widest font-mono">"{selectedExtensionSequence}"</strong> TO INSTANCE RECTIFIER BLOCK. RECALCULATING STATISTICS POTENCY...
                                      </span>
                                      <button
                                        type="button"
                                        onClick={() => {
                                          if (!selectedExtensionSequence) return;
                                          sound.playBeep(640, 0.2, "sine");
                                          sound.playSynthesisSuccess();

                                          // Deduct 1 count from sequences inventory
                                          setSequences((prev) => {
                                            return prev.map(s => {
                                              if (s.sequence === selectedExtensionSequence) {
                                                return { ...s, count: Math.max(0, s.count - 1) };
                                              }
                                              return s;
                                            }).filter(s => s.count > 0 || ['AGTCGTAC', 'CCCGGGAA', 'TTTAAACG', 'AACCGGTT'].includes(s.sequence)); // Keep base ones
                                          });

                                          // Update the creature
                                          const nextSeq = bot.sequence + selectedExtensionSequence;
                                          const proc = constructProceduralCreature(nextSeq, bot.origin);

                                          setCreatures((prev) => {
                                            return prev.map(c => {
                                              if (c.id === bot.id) {
                                                const currentAppended = c.appendedGenes || [];
                                                return {
                                                  ...c,
                                                  sequence: nextSeq,
                                                  appendedGenes: [...currentAppended, selectedExtensionSequence],
                                                  vitality: proc.vitality,
                                                  attack: proc.attack,
                                                  defense: proc.defense,
                                                  speed: proc.speed,
                                                  type: proc.type,
                                                  faction: proc.faction,
                                                  lore: proc.lore,
                                                  primaryWeapon: proc.primaryWeapon
                                                };
                                              }
                                              return c;
                                            });
                                          });

                                          triggerLog(`MUTATION MERGER SUCCESS: Appended gene "${selectedExtensionSequence}" onto specimen "${bot.name}". Stats re-optimized!`, "success");
                                          setSelectedExtensionSequence(null);
                                        }}
                                        className="w-full sm:w-auto px-4 py-2 bg-amber-800 hover:bg-amber-700 text-black font-bold rounded-sm uppercase text-xs cursor-pointer tracking-wider shrink-0 transition-all shadow-[0_0_10px_rgba(245,158,11,0.2)] active:scale-95"
                                      >
                                        APPLY EXTENSION
                                      </button>
                                    </div>
                                  )}
                                </div>
                              ) : (
                                <div className="p-2.5 bg-green-950/15 border border-green-900/45 rounded text-center">
                                  <span className="text-[10px] text-[#00FF41] font-bold block uppercase tracking-wider">▲ STABILIZATION INTEGRITY UNLOCKED</span>
                                  <p className="text-[9.5px] text-green-700 font-mono mt-0.5 leading-relaxed">
                                    Specimen sequence has been appended with exactly 2 maximum additional genes. Signal capacity stabilized.
                                  </p>
                                </div>
                              )}
                            </div>

                            <div className="flex flex-wrap sm:flex-nowrap gap-2 mt-4 pt-2.5 border-t border-green-900/20 text-xs text-white">
                              <button 
                                onClick={() => { 
                                  sound.playBeep(440,0.05,"sine"); 
                                  setInspectedCreatureId(null); 
                                  if (creatureCardOpenedFrom === 'Constructor') {
                                    setActiveTab('splicer');
                                  } else if (creatureCardOpenedFrom === 'Trade') {
                                    setActiveTab('gen_network');
                                    setGenNetworkSubTab('nodes');
                                    setNodesSubTab('qr_trade');
                                  } else if (creatureCardOpenedFrom === 'Scanner') {
                                    setActiveTab('transceiver');
                                    setSyncNetTab('scanner');
                                  } else {
                                    setActiveTab('library');
                                  }
                                }}
                                className="flex-grow py-1.5 bg-neutral-900 border border-green-900/60 hover:text-white hover:border-[#00FF41] uppercase font-bold text-[#00FF41] text-center cursor-pointer transition-all active:scale-[0.98]"
                              >
                                ✕ Return to {
                                  creatureCardOpenedFrom === 'Constructor' 
                                    ? 'Constructor' 
                                    : creatureCardOpenedFrom === 'Trade' 
                                      ? 'P2P Trade Portal' 
                                      : creatureCardOpenedFrom === 'Scanner'
                                        ? 'Anomaly Screen'
                                        : 'Gen-Vault Data'
                                }
                              </button>
                              
                              {creatureCardOpenedFrom === 'Trade' && (
                                <button 
                                  type="button"
                                  onClick={() => {
                                    sound.playSynthesisSuccess();
                                    setTradeSelectedCreatureId(bot.id);
                                    setActiveTab('gen_network');
                                    setGenNetworkSubTab('nodes');
                                    setNodesSubTab('qr_trade');
                                    setInspectedCreatureId(null);
                                    triggerLog(`SUCCESS: Selected "${bot.name}" for P2P carrier trade.`, "success");
                                  }}
                                  className="px-5 py-1.5 bg-[#00FF41]/20 border border-[#00FF41] hover:bg-[#00FF41]/30 font-bold uppercase cursor-pointer transition-all text-[#00FF41] text-[10px] animate-pulse whitespace-nowrap"
                                >
                                  🤝 SELECT FOR TRADE
                                </button>
                              )}

                              {creatureCardOpenedFrom === 'Scanner' && (
                                <button 
                                  type="button"
                                  disabled={!getAvailableHarvestingCreatures().some(c => c.id === bot.id)}
                                  onClick={() => {
                                    if (selectedAnomalyId) {
                                      sound.playSynthesisSuccess();
                                      handleDispatchCreatureToHarvest(bot.id, selectedAnomalyId);
                                      setActiveTab('transceiver');
                                      setSyncNetTab('scanner');
                                      setInspectedCreatureId(null);
                                      triggerLog(`SUCCESS: Dispatched "${bot.name}" to harvest anomaly spatial bounds.`, "success");
                                    }
                                  }}
                                  className={`px-5 py-1.5 font-bold uppercase cursor-pointer transition-all text-white text-[10px] animate-pulse whitespace-nowrap ${
                                    getAvailableHarvestingCreatures().some(c => c.id === bot.id)
                                      ? "bg-purple-600 hover:bg-purple-500 border border-purple-400 shadow-[0_0_8px_rgba(168,85,247,0.4)]"
                                      : "bg-neutral-800 border border-neutral-700 text-neutral-500 cursor-not-allowed animate-none"
                                  }`}
                                >
                                  🧬 DISPATCH TO HARVEST
                                </button>
                              )}
                              
                              <button 
                                type="button"
                                onClick={() => { 
                                  sound.playBeep(440,0.05,"sine"); 
                                  const nextId = defenderCreatureId === bot.id ? null : bot.id;
                                  setDefenderCreatureId(nextId);
                                  triggerLog(nextId ? `Designated "${bot.name}" as official defender.` : `Cleared "${bot.name}" as designated defender.`, "success");
                                }}
                                className={`px-3 py-1.5 rounded flex items-center justify-center gap-1 font-bold uppercase cursor-pointer transition-all border text-[10px] ${
                                  defenderCreatureId === bot.id 
                                    ? "bg-blue-950/60 border-blue-500 text-blue-400 shadow-[0_0_8px_rgba(59,130,246,0.3)]" 
                                    : "bg-neutral-950 border-neutral-700 hover:border-blue-500 text-neutral-400 hover:text-white"
                                }`}
                              >
                                {defenderCreatureId === bot.id ? "★ DEFENDER READY" : "SET AS DEFENDER"}
                              </button>

                              <button 
                                type="button"
                                onClick={() => { 
                                  sound.playBeep(520, 0.05, "sine"); 
                                  setCreatures(prev => prev.map(c => {
                                    if (c.id === bot.id) {
                                      const nextVal = !c.isAutoHacker;
                                      triggerLog(nextVal ? `Designated "${bot.name}" as active G.E.N. Auto-Hacker.` : `Deactivated Auto-Hacker protocol for "${bot.name}".`, "warn");
                                      return { ...c, isAutoHacker: nextVal };
                                    }
                                    return c;
                                  }));
                                }}
                                className={`px-3 py-1.5 rounded flex items-center justify-center gap-1 font-bold uppercase cursor-pointer transition-all border text-[10px] ${
                                  bot.isAutoHacker 
                                    ? "bg-amber-950/60 border-amber-500 text-amber-400 shadow-[0_0_8px_rgba(245,158,11,0.3)]" 
                                    : "bg-neutral-950 border-neutral-700 hover:border-amber-500 text-neutral-400 hover:text-white"
                                }`}
                              >
                                ⚙️ {bot.isAutoHacker ? "AUTO-HACKER" : "AUTO-HACKER"}
                              </button>

                              <button 
                                type="button"
                                onClick={() => { 
                                  toggleFavoriteCreature(bot.id);
                                }}
                                className={`px-3 py-1.5 rounded flex items-center justify-center gap-1 font-bold uppercase cursor-pointer transition-all border text-[10px] ${
                                  bot.isFavorite 
                                    ? "bg-yellow-950/60 border-yellow-500 text-yellow-500 shadow-[0_0_8px_rgba(234,179,8,0.3)]" 
                                    : "bg-neutral-950 border-neutral-700 hover:border-yellow-500 text-neutral-400 hover:text-white"
                                }`}
                              >
                                {bot.isFavorite ? "★ FAVORITED" : "☆ FAVORITE"}
                              </button>

                              <button 
                                onClick={() => {
                                  sound.playCreatureSequenceAudio(bot.sequence);
                                  triggerLog(`Simulated dynamic sonic battle cry for "${bot.name}" finished.`, "info");
                                }}
                                className="px-5 py-1.5 bg-[#00FF41]/10 border border-[#00FF41] hover:bg-[#00FF41]/20 font-bold uppercase cursor-pointer transition-all text-black dark:text-[#00FF41]"
                              >
                                AUDIO EMIT
                              </button>
                            </div>
                          </div>
                        );
                      })()
                    ) : (
                      // Main library list display layout with detailed sorting and filtering controls
                      (() => {
                        // Helper to gather all tags of a specimen
                        const getCreatureTags = (item: Creature): string[] => {
                          const tags: string[] = [];
                          if (item.isFavorite) tags.push("FAVORITE");
                          if (defenderCreatureId === item.id) tags.push("DEFENDER");
                          if (item.isAutoHacker) tags.push("AUTO-HACKER");
                          
                          const coh = getCoherence(item.sequence, targetSequence);
                          if (coh === 'full') tags.push("FULL COHERENCE");
                          else if (coh === 'partial') tags.push("PARTIAL COHERENCE");
                          
                          if (item.attack >= 75 || item.defense >= 75 || item.vitality >= 75 || item.speed >= 75) {
                            tags.push("ALPHA GENE");
                          }
                          
                          if (item.appendedGenes && item.appendedGenes.length > 0) {
                            tags.push("MODIFIED");
                          }
                          
                          if (item.origin === "Created") {
                            tags.push("ORIGINAL");
                          } else if (item.origin && item.origin.startsWith("Traded")) {
                            tags.push("MERCENARY");
                          }
                          return tags;
                        };

                        // Dynamically deduce all distinct creature types present in library
                        const uniqueTypes = Array.from(new Set(creatures.map(c => c.type || "Unknown Type"))).filter(Boolean) as string[];
                        
                        // Selectable tag categories
                        const availableTags = ["FAVORITE", "DEFENDER", "AUTO-HACKER", "FULL COHERENCE", "PARTIAL COHERENCE", "ALPHA GENE", "MODIFIED", "ORIGINAL", "MERCENARY"];

                        // Calculate dynamic maximum bounds based on real database entries
                        const computedMaxVitality = creatures.length > 0 ? Math.max(...creatures.map(c => c.vitality), 100) : 250;
                        const computedMaxAttack = creatures.length > 0 ? Math.max(...creatures.map(c => c.attack), 100) : 250;
                        const computedMaxDefense = creatures.length > 0 ? Math.max(...creatures.map(c => c.defense), 100) : 250;
                        const computedMaxSpeed = creatures.length > 0 ? Math.max(...creatures.map(c => c.speed), 100) : 250;

                        // Calculate active filters count
                        const activeFiltersCount = !applyLibFilters ? 0 : (
                          (libFilterFaction !== "ALL" ? 1 : 0) +
                          (libFilterType !== "ALL" ? 1 : 0) +
                          (libFilterTag !== "ALL" ? 1 : 0) +
                          (libFilterDispatchGeneOnly ? 1 : 0) +
                          (libMinVitality > 0 ? 1 : 0) +
                          (libMaxVitality < computedMaxVitality ? 1 : 0) +
                          (libMinAttack > 0 ? 1 : 0) +
                          (libMaxAttack < computedMaxAttack ? 1 : 0) +
                          (libMinDefense > 0 ? 1 : 0) +
                          (libMaxDefense < computedMaxDefense ? 1 : 0) +
                          (libMinSpeed > 0 ? 1 : 0) +
                          (libMaxSpeed < computedMaxSpeed ? 1 : 0)
                        );

                        // Filter & Sort specimens array
                        const filteredSortedCreatures = creatures
                          .filter((item) => {
                            if (!applyLibFilters) return true;
                            
                            // Faction
                            if (libFilterFaction !== "ALL" && item.faction !== libFilterFaction) return false;
                            
                            // Type
                            if (libFilterType !== "ALL" && item.type !== libFilterType) return false;
                            
                            // Tag
                            const itemTags = getCreatureTags(item);
                            if (libFilterTag !== "ALL" && !itemTags.includes(libFilterTag)) return false;

                            // G.E.N. Dispatch Gene Match filter
                            if (libFilterDispatchGeneOnly && !checkCreatureHasDispatchGene(item.sequence)) return false;
                            
                            // Stats boundaries
                            if (item.vitality < libMinVitality || item.vitality > libMaxVitality) return false;
                            if (item.attack < libMinAttack || item.attack > libMaxAttack) return false;
                            if (item.defense < libMinDefense || item.defense > libMaxDefense) return false;
                            if (item.speed < libMinSpeed || item.speed > libMaxSpeed) return false;
                            
                            return true;
                          })
                          .sort((a, b) => {
                            switch (libSortBy) {
                              case "name-asc":
                                        return a.name.localeCompare(b.name);
                              case "name-desc":
                                        return b.name.localeCompare(a.name);
                              case "type-asc":
                                        return (a.type || "").localeCompare(b.type || "");
                              case "type-desc":
                                        return (b.type || "").localeCompare(a.type || "");
                              case "faction-asc":
                                        return a.faction.localeCompare(b.faction);
                              case "vitality-desc":
                                        return b.vitality - a.vitality;
                              case "attack-desc":
                                        return b.attack - a.attack;
                              case "defense-desc":
                                        return b.defense - a.defense;
                              case "speed-desc":
                                        return b.speed - a.speed;
                              case "tags-desc":
                                        return getCreatureTags(b).length - getCreatureTags(a).length;
                              default:
                                        return 0;
                            }
                          });

                        const handleResetFilters = () => {
                          sound.playBeep(420, 0.1, "sine");
                          setLibSortBy("name-asc");
                          setLibFilterFaction("ALL");
                          setLibFilterType("ALL");
                          setLibFilterTag("ALL");
                          setLibFilterDispatchGeneOnly(false);
                          setLibMinVitality(0);
                          setLibMaxVitality(computedMaxVitality);
                          setLibMinAttack(0);
                          setLibMaxAttack(computedMaxAttack);
                          setLibMinDefense(0);
                          setLibMaxDefense(computedMaxDefense);
                          setLibMinSpeed(0);
                          setLibMaxSpeed(computedMaxSpeed);
                        };

                        return (
                          <div className="bg-neutral-900/20 border border-green-900/40 p-3 sm:p-5 rounded flex-1 flex flex-col justify-between overflow-y-auto custom-pox-scrollbar relative">
                            <div className="space-y-4">
                              <div className="flex justify-between items-start flex-wrap gap-2">
                                <div>
                                  <div className="flex items-center gap-1.5 text-[10px] uppercase text-green-700">
                                    <span>[ GENETIC VAULT REGISTRY ]</span>
                                    <span className="text-[#00FF41]">ACTIVE SELECTION: {filteredSortedCreatures.length} / {creatures.length}</span>
                                  </div>
                                  <h2 className="text-xs font-bold text-white tracking-wider">STORED P.O.X. SEQUENCES</h2>
                                </div>
                                <div className="flex items-center gap-2">
                                  {/* Filter Activation Force Switch */}
                                  <button
                                    type="button"
                                    onClick={() => {
                                      sound.playBeep(480, 0.05, "sine");
                                      setApplyLibFilters(!applyLibFilters);
                                    }}
                                    className={`px-2 py-1 text-[8.5px] font-black uppercase border rounded cursor-pointer transition-all flex items-center gap-1 ${
                                      applyLibFilters 
                                        ? "bg-[#00FF41]/15 border-[#00FF41] text-[#00FF41]"
                                        : "bg-neutral-950 border-neutral-850 text-neutral-500 hover:text-white"
                                    }`}
                                  >
                                    <span className={`w-1.5 h-1.5 rounded-full ${applyLibFilters ? "bg-[#00FF41] animate-pulse" : "bg-neutral-700"}`} />
                                    {applyLibFilters ? "FILTERS ACTIVE" : "FILTERS BYPASSED"}
                                  </button>

                                  <button
                                    type="button"
                                    onClick={() => {
                                      sound.playBeep(520, 0.05, "sine");
                                      setIsFilterPanelExpanded(!isFilterPanelExpanded);
                                    }}
                                    className="px-2 py-1 text-[8.5px] font-bold border border-green-900 bg-neutral-950 text-[#00FF41] hover:bg-green-950/20 rounded cursor-pointer transition-all flex items-center gap-1"
                                  >
                                    {isFilterPanelExpanded ? "⬘ COLLAPSE FILTERS" : "⬙ EXPLORE FILTER SUITE"}
                                    {activeFiltersCount > 0 && (
                                      <span className="bg-[#00FF41] text-black px-1 rounded-full text-[7.5px] scale-90 font-mono font-bold">
                                        {activeFiltersCount}
                                      </span>
                                    )}
                                  </button>
                                </div>
                              </div>

                              {/* Accordion Collapsible Filter Console */}
                              {isFilterPanelExpanded && (
                                <motion.div
                                  initial={{ opacity: 0, height: 0 }}
                                  animate={{ opacity: 1, height: "auto" }}
                                  className="border border-green-950 bg-black/60 p-3 rounded space-y-3.5 text-[9.5px]"
                                >
                                  {/* Section 1: Dropdown Filters & Sorters */}
                                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                                    {/* Sort dropdown */}
                                    <div>
                                      <label className="text-green-700 font-bold block mb-1">SORT SEQUENCE:</label>
                                      <select
                                        value={libSortBy}
                                        onChange={(e) => { sound.playBeep(480, 0.02, "sine"); setLibSortBy(e.target.value); }}
                                        className="w-full bg-neutral-950 border border-green-900 rounded p-1 text-white font-mono text-[9px] focus:outline-none focus:border-[#00FF41]"
                                      >
                                        <option value="name-asc">NAME (A ➔ Z)</option>
                                        <option value="name-desc">NAME (Z ➔ A)</option>
                                        <option value="type-asc">CREATURE TYPE (A ➔ Z)</option>
                                        <option value="type-desc">CREATURE TYPE (Z ➔ A)</option>
                                        <option value="faction-asc">FACTION (A ➔ Z)</option>
                                        <option value="vitality-desc">VITALITY (HIGH ➔ LOW)</option>
                                        <option value="attack-desc">ATTACK (HIGH ➔ LOW)</option>
                                        <option value="defense-desc">DEFENSE (HIGH ➔ LOW)</option>
                                        <option value="speed-desc">SPEED (HIGH ➔ LOW)</option>
                                        <option value="tags-desc">TAG DENSITY (MAX ➔ MIN)</option>
                                      </select>
                                    </div>

                                    {/* Faction filter */}
                                    <div>
                                      <label className="text-green-700 font-bold block mb-1">FACTION CLASSIFER:</label>
                                      <select
                                        value={libFilterFaction}
                                        onChange={(e) => { sound.playBeep(480, 0.02, "sine"); setLibFilterFaction(e.target.value); }}
                                        className="w-full bg-neutral-950 border border-green-900 rounded p-1 text-white font-mono text-[9px] focus:outline-none focus:border-[#00FF41]"
                                      >
                                        <option value="ALL">ALL FACTIONS</option>
                                        <option value="Infection">INFECTION</option>
                                        <option value="Mech">MECH</option>
                                        <option value="Parasite">PARASITE</option>
                                        <option value="Containment">CONTAINMENT</option>
                                      </select>
                                    </div>

                                    {/* Type filter */}
                                    <div>
                                      <label className="text-green-700 font-bold block mb-1">CREATURE TYPE:</label>
                                      <select
                                        value={libFilterType}
                                        onChange={(e) => { sound.playBeep(480, 0.02, "sine"); setLibFilterType(e.target.value); }}
                                        className="w-full bg-neutral-950 border border-green-900 rounded p-1 text-white font-mono text-[9px] focus:outline-none focus:border-[#00FF41]"
                                      >
                                        <option value="ALL">ALL CREATURE TYPES</option>
                                        {uniqueTypes.map((type) => (
                                          <option key={type} value={type}>{type.toUpperCase()}</option>
                                        ))}
                                      </select>
                                    </div>

                                    {/* Creature tag filter */}
                                    <div>
                                      <label className="text-green-700 font-bold block mb-1">CREATURE TAGS:</label>
                                      <select
                                        value={libFilterTag}
                                        onChange={(e) => { sound.playBeep(480, 0.02, "sine"); setLibFilterTag(e.target.value); }}
                                        className="w-full bg-neutral-950 border border-green-900 rounded p-1 text-white font-mono text-[9px] focus:outline-none focus:border-[#00FF41]"
                                      >
                                        <option value="ALL">ALL TAGS</option>
                                        {availableTags.map((tag) => (
                                          <option key={tag} value={tag}>{tag}</option>
                                        ))}
                                      </select>
                                    </div>
                                  </div>

                                  {/* Section 2: Disabled */}
                                  <div className="hidden">
                                    
                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-2">
                                      {/* Vitality range */}
                                      <div className="space-y-1">
                                        <div className="flex justify-between font-mono text-[9px]">
                                          <span className="text-neutral-400 font-semibold">VITALITY BOUNDS:</span>
                                          <span className="text-emerald-400 font-bold">{libMinVitality} ➔ {libMaxVitality}</span>
                                        </div>
                                        <div className="flex gap-2 items-center">
                                          <input
                                            type="range" min="0" max={computedMaxVitality} value={libMinVitality}
                                            onChange={(e) => { sound.playBeep(320 + Number(e.target.value)*2, 0.01); setLibMinVitality(Number(e.target.value)); }}
                                            className="flex-1 accent-emerald-500 h-1 cursor-pointer bg-neutral-900 rounded"
                                            title="Minimum Vitality limit"
                                          />
                                          <span className="text-neutral-600 text-[8px]">MIN</span>
                                          <input
                                            type="range" min="0" max={computedMaxVitality} value={libMaxVitality}
                                            onChange={(e) => { sound.playBeep(420 + Number(e.target.value)*2, 0.01); setLibMaxVitality(Number(e.target.value)); }}
                                            className="flex-1 accent-[#00FF41] h-1 cursor-pointer bg-neutral-900 rounded"
                                            title="Maximum Vitality limit"
                                          />
                                          <span className="text-neutral-600 text-[8px]">MAX</span>
                                        </div>
                                      </div>

                                      {/* Attack range */}
                                      <div className="space-y-1">
                                        <div className="flex justify-between font-mono text-[9px]">
                                          <span className="text-neutral-400 font-semibold">ATTACK BOUNDS:</span>
                                          <span className="text-[#00FF41] font-bold">{libMinAttack} ➔ {libMaxAttack}</span>
                                        </div>
                                        <div className="flex gap-2 items-center">
                                          <input
                                            type="range" min="0" max={computedMaxAttack} value={libMinAttack}
                                            onChange={(e) => { sound.playBeep(320 + Number(e.target.value)*2, 0.01); setLibMinAttack(Number(e.target.value)); }}
                                            className="flex-1 accent-emerald-500 h-1 cursor-pointer bg-neutral-900 rounded"
                                            title="Minimum Attack limit"
                                          />
                                          <span className="text-neutral-600 text-[8px]">MIN</span>
                                          <input
                                            type="range" min="0" max={computedMaxAttack} value={libMaxAttack}
                                            onChange={(e) => { sound.playBeep(420 + Number(e.target.value)*2, 0.01); setLibMaxAttack(Number(e.target.value)); }}
                                            className="flex-1 accent-[#00FF41] h-1 cursor-pointer bg-neutral-900 rounded"
                                            title="Maximum Attack limit"
                                          />
                                          <span className="text-neutral-600 text-[8px]">MAX</span>
                                        </div>
                                      </div>

                                      {/* Defense range */}
                                      <div className="space-y-1">
                                        <div className="flex justify-between font-mono text-[9px]">
                                          <span className="text-neutral-400 font-semibold">DEFENSE BOUNDS:</span>
                                          <span className="text-[#00FF41] font-bold">{libMinDefense} ➔ {libMaxDefense}</span>
                                        </div>
                                        <div className="flex gap-2 items-center">
                                          <input
                                            type="range" min="0" max={computedMaxDefense} value={libMinDefense}
                                            onChange={(e) => { sound.playBeep(320 + Number(e.target.value)*2, 0.01); setLibMinDefense(Number(e.target.value)); }}
                                            className="flex-1 accent-emerald-500 h-1 cursor-pointer bg-neutral-900 rounded"
                                            title="Minimum Defense limit"
                                          />
                                          <span className="text-neutral-600 text-[8px]">MIN</span>
                                          <input
                                            type="range" min="0" max={computedMaxDefense} value={libMaxDefense}
                                            onChange={(e) => { sound.playBeep(420 + Number(e.target.value)*2, 0.01); setLibMaxDefense(Number(e.target.value)); }}
                                            className="flex-1 accent-[#00FF41] h-1 cursor-pointer bg-neutral-900 rounded"
                                            title="Maximum Defense limit"
                                          />
                                          <span className="text-neutral-600 text-[8px]">MAX</span>
                                        </div>
                                      </div>

                                      {/* Speed range */}
                                      <div className="space-y-1">
                                        <div className="flex justify-between font-mono text-[9px]">
                                          <span className="text-neutral-400 font-semibold">SPEED BOUNDS:</span>
                                          <span className="text-emerald-400 font-bold">{libMinSpeed} ➔ {libMaxSpeed}</span>
                                        </div>
                                        <div className="flex gap-2 items-center">
                                          <input
                                            type="range" min="0" max={computedMaxSpeed} value={libMinSpeed}
                                            onChange={(e) => { sound.playBeep(320 + Number(e.target.value)*2, 0.01); setLibMinSpeed(Number(e.target.value)); }}
                                            className="flex-1 accent-emerald-500 h-1 cursor-pointer bg-neutral-900 rounded"
                                            title="Minimum Speed limit"
                                          />
                                          <span className="text-neutral-600 text-[8px]">MIN</span>
                                          <input
                                            type="range" min="0" max={computedMaxSpeed} value={libMaxSpeed}
                                            onChange={(e) => { sound.playBeep(420 + Number(e.target.value)*2, 0.01); setLibMaxSpeed(Number(e.target.value)); }}
                                            className="flex-1 accent-[#00FF41] h-1 cursor-pointer bg-neutral-900 rounded"
                                            title="Maximum Speed limit"
                                          />
                                          <span className="text-neutral-600 text-[8px]">MAX</span>
                                        </div>
                                      </div>
                                    </div>

                                    {/* G.E.N. Dispatch Gene Match filter options & Repositioned Clear Filters */}
                                    <div className="border-t border-green-950/80 pt-2 flex items-center justify-between gap-3 flex-wrap">
                                      <label className="flex items-center gap-2 text-neutral-400 font-bold cursor-pointer select-none">
                                        <input
                                          type="checkbox"
                                          checked={libFilterDispatchGeneOnly}
                                          onChange={(e) => {
                                            sound.playBeep(440, 0.05, "sine");
                                            setLibFilterDispatchGeneOnly(e.target.checked);
                                          }}
                                          className="w-3 h-3 accent-[#00FF41] rounded border-green-900 bg-neutral-950 cursor-pointer"
                                        />
                                        <span className="text-[#00FF41]/85 text-[8.5px] uppercase tracking-wider font-mono">ONLY SHOW SEQUENCES MATCHING G.E.N. NETWORK DISPATCHES</span>
                                      </label>

                                      <button
                                        type="button"
                                        onClick={handleResetFilters}
                                        className="px-3 py-1 bg-neutral-950 border border-green-800 hover:border-red-500 text-neutral-400 hover:text-white rounded uppercase font-bold text-[8.5px] cursor-pointer tracking-wider"
                                      >
                                        ✕ CLEAR FILTERS
                                      </button>
                                    </div>
                                  </div>
                                </motion.div>
                              )}
                            </div>

                            {/* Stored Specimens Search and Filters (Two-Step Layout) */}
                            {/* STEP 1: Filter Summary screen (Always visible as base panel) */}
                            <div className="flex-1 flex flex-col justify-center items-center py-8 px-4 bg-black/40 border border-green-950 rounded mt-3 text-center space-y-4">
                              <div className="space-y-1">
                                {applyLibFilters ? (
                                  <div className="text-sm font-black text-[#00FF41] tracking-widest uppercase font-mono">
                                    SEQUENCES FOUND: {filteredSortedCreatures.length}
                                  </div>
                                ) : (
                                  <div className="text-sm font-black text-rose-500 tracking-widest uppercase font-mono animate-pulse">
                                    NO FILTERS APPLIED
                                  </div>
                                )}
                                <p className="text-[10px] text-green-700 font-mono tracking-wide">
                                  {applyLibFilters 
                                    ? "Matching sequence(s) loaded; click button below to view" 
                                    : "Enable filters or reconfigure search parameters using the filter options above."}
                                </p>
                              </div>

                              <button
                                type="button"
                                onClick={() => {
                                  sound.playBeep(800, 0.08, "sine");
                                  setViewingArchiveSearch(true);
                                }}
                                className="w-full max-w-xs py-3 bg-[#00FF41] hover:bg-green-405 text-black font-extrabold text-xs uppercase tracking-widest rounded transition-all cursor-pointer shadow-[0_0_15px_rgba(0,255,102,0.35)] active:scale-95 font-mono flex items-center justify-center gap-1.5"
                              >
                                <Search className="w-4 h-4 text-black stroke-[2.5]" />
                                <span>VIEW SEQUENCES</span>
                              </button>
                            </div>

                            {/* STEP 2: Dedicated Window Overlay system for listing matching creatures */}
                            {viewingArchiveSearch && (
                              <motion.div
                                key="archive-search-window"
                                initial={{ opacity: 0, scale: 0.98 }}
                                animate={{ opacity: 1, scale: 1 }}
                                exit={{ opacity: 0, scale: 0.98 }}
                                className="absolute inset-x-0 bottom-0 top-[2px] bg-[#020d04]/98 border-2 border-green-500/85 rounded flex flex-col z-40 p-4 overflow-hidden shadow-[0_0_25px_rgba(0,255,102,0.22)] font-mono"
                              >
                                {/* Custom Titlebar for the Window */}
                                <div className="flex justify-between items-center bg-green-950/40 border border-green-500/40 px-3 py-1.5 rounded-t-sm mb-3">
                                  <span className="text-[9.5px] text-[#00FF41] font-bold tracking-widest uppercase flex items-center gap-1.5 select-none">
                                    <span className="w-1.5 h-1.5 bg-[#00FF41] rounded-full animate-pulse" />
                                    [ GEN-VAULT DIRECTORY: P.O.X. SEQUENCE FILE ]
                                  </span>
                                  <button
                                    type="button"
                                    onClick={() => {
                                      sound.playBeep(440, 0.05, "sine");
                                      setViewingArchiveSearch(false);
                                    }}
                                    className="px-2 py-0.5 bg-red-950 hover:bg-red-900 border border-red-500/50 text-red-400 hover:text-white rounded text-[8.5px] cursor-pointer font-bold tracking-wider transition-colors uppercase"
                                  >
                                    ✕ CLOSE WINDOW
                                  </button>
                                </div>

                                {/* Summary metrics header */}
                                <div className="flex justify-between items-center text-[9.5px] mb-2 px-1 text-green-600 border-b border-green-950 pb-1.5 select-none">
                                  <span>SYSTEM TARGET MATCHES: <strong className="text-[#00FF41] font-black">{filteredSortedCreatures.length}</strong> SPECIMENS</span>
                                  <span className="text-neutral-500">STATUS: GEN-VAULT OPEN</span>
                                </div>

                                {/* List Area - Scrolls independently, taking maximum height! */}
                                <div className="flex-1 overflow-y-auto space-y-2 pr-1 scrollbar-thin bg-black/60 border border-green-950 rounded p-2.5">
                                  {filteredSortedCreatures.map((item, idx) => {
                                    const currentTags = getCreatureTags(item);
                                    return (
                                      <div 
                                        key={idx}
                                        onClick={() => { 
                                          sound.playBeep(700, 0.05, "sine"); 
                                          if (creatureCardOpenedFrom !== 'Trade' && creatureCardOpenedFrom !== 'Scanner') {
                                            setCreatureCardOpenedFrom('Gen-Vault Data');
                                          }
                                          setInspectedCreatureId(item.id);
                                        }}
                                        className="flex justify-between items-center bg-black/95 p-3.5 border-l-2 border-green-500 hover:bg-neutral-900/60 transition-colors cursor-pointer group relative rounded-sm"
                                      >
                                        <div className="min-w-0 flex-1 pr-3">
                                          <div className="flex items-center gap-1.5 flex-wrap">
                                            <span className="text-xs font-bold text-white group-hover:text-[#00FF41] transition-all truncate">{item.name}</span>
                                            {item.isFavorite && (
                                              <span className="flex items-center gap-0.5 border border-yellow-500/50 bg-yellow-950/80 px-1 py-0.5 rounded text-[7.5px] font-bold text-yellow-400 font-sans shadow-[0_0_6px_rgba(234,179,8,0.25)] select-none">
                                                ★ FAV
                                              </span>
                                            )}
                                            {defenderCreatureId === item.id && (
                                              <span className="flex items-center gap-0.5 border border-blue-500/50 bg-blue-950/80 px-1 py-0.5 rounded text-[7.5px] font-bold text-blue-400 font-sans shadow-[0_0_6px_rgba(59,130,246,0.15)] select-none">
                                                <Shield className="w-2.5 h-2.5" /> DEFENDER
                                              </span>
                                            )}
                                            {currentTags.includes("FULL COHERENCE") ? (
                                              <span className="px-1.5 py-0.5 bg-emerald-950 text-[#00FF41] border border-emerald-500 rounded text-[7.5px] font-bold font-mono tracking-widest animate-pulse select-none">
                                                FULL COHERENCE
                                              </span>
                                            ) : currentTags.includes("PARTIAL COHERENCE") ? (
                                              <span className="px-1.5 py-0.5 bg-yellow-950/80 text-yellow-500 border border-yellow-600/55 rounded text-[7.5px] font-bold font-mono tracking-wider select-none">
                                                PARTIAL COHERENCE
                                              </span>
                                            ) : null}
                                            <span className="text-[8px] text-green-750 font-mono select-none">{item.id}</span>
                                          </div>
                                          
                                          {/* Sub-label indicators */}
                                          <div className="flex flex-wrap gap-1.5 items-center mt-1">
                                            <span className="text-[8.5px] text-green-600 font-mono font-bold">{item.type}</span>
                                            {currentTags.map((t) => {
                                              if (t === "DEFENDER" || t === "FULL COHERENCE" || t === "PARTIAL COHERENCE") return null;
                                              return (
                                                <span 
                                                  key={t}
                                                  className="px-1 py-[1.5px] bg-neutral-900 border border-green-950 rounded text-[6.5px] font-bold text-neutral-400 uppercase tracking-wide font-sans select-none"
                                                >
                                                  {t}
                                                </span>
                                              );
                                            })}
                                          </div>

                                          {/* Inline stats breakdown */}
                                          <div className="flex gap-3 text-[7.5px] font-mono text-neutral-500 mt-1 uppercase">
                                            <span>VIT: <strong className="text-white font-bold">{item.vitality}</strong></span>
                                            <span>ATK: <strong className="text-white font-bold">{item.attack}</strong></span>
                                            <span>DEF: <strong className="text-white font-bold">{item.defense}</strong></span>
                                            <span>SPD: <strong className="text-white font-bold">{item.speed}</strong></span>
                                          </div>
                                        </div>
                                        <div className="flex items-center gap-2">
                                          <button 
                                            type="button"
                                            onClick={(e) => toggleFavoriteCreature(item.id, e)}
                                            className="text-yellow-600 hover:text-yellow-400 p-1 cursor-pointer transition-colors"
                                            title={item.isFavorite ? "Remove from Favorites" : "Add to Favorites"}
                                          >
                                            <Star className={`w-3.5 h-3.5 ${item.isFavorite ? "fill-yellow-400 text-yellow-400" : "text-green-800 hover:text-yellow-400"}`} />
                                          </button>
                                          <span className={`text-[8.5px] px-1.5 py-0.5 rounded-sm uppercase font-bold text-black font-sans ${
                                            item.faction === "Infection" ? "bg-red-500" :
                                            item.faction === "Mech" ? "bg-blue-400" :
                                            item.faction === "Parasite" ? "bg-purple-500" : "bg-green-400"
                                          }`}>
                                            {item.faction.slice(0, 3)}
                                          </span>
                                          <button 
                                            onClick={(e) => handleDeleteCreature(item.id, e)}
                                            className="text-green-800 hover:text-red-500 transition-colors p-1 cursor-pointer"
                                            title="Unbind sequence specimen"
                                          >
                                            <Trash2 className="w-3.5 h-3.5" />
                                          </button>
                                        </div>
                                        {checkCreatureHasDispatchGene(item.sequence) && (
                                          <div className="absolute bottom-1 right-7 pointer-events-none select-none text-[6.5px] uppercase font-bold text-amber-400 font-mono tracking-wider bg-amber-950/75 px-1 py-[0.5px] border border-amber-600/40 rounded shadow-[0_0_6px_rgba(251,191,36,0.15)] animate-pulse" title="SPECIMEN CONTAINS ACTIVE DISPATCH GENE">
                                            🧬 G.E.N. MATCH
                                          </div>
                                        )}
                                      </div>
                                    );
                                  })}
                                  {filteredSortedCreatures.length === 0 && (
                                    <div className="text-center text-green-800 py-16 text-xs font-mono border border-dashed border-green-950 bg-black/20 p-4 rounded">
                                      No specimens match search criteria. Readjust filter parameters!
                                    </div>
                                  )}
                                </div>


                              </motion.div>
                            )}

                            <span className="text-[9px] text-green-700/80 mt-3 block font-mono border-t border-green-900/20 pt-2 select-none">
                              WARNING: Trading creatures with other emulators within 30ft transfers custody. Transferred specimens are cleared permanently from memory sectors upon accepted linkage.
                            </span>
                          </div>
                        );
                      })()
                    )}

                  </motion.div>
                )}

                {activeTab === 'transceiver' && (
                  <motion.div 
                    key="transceiver"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    className="flex flex-col h-full overflow-hidden text-left"
                  >
                    {!activeHack && (
                      <div className="flex border-b border-green-950/45 text-[10.5px] font-mono tracking-wider mb-4 select-none shrink-0 gap-1 pb-[1px]">
                        <button
                          type="button"
                          onClick={() => {
                            sound.playBeep(440, 0.05, "sine");
                            setSyncNetTab('sweep');
                          }}
                          className={`px-4 py-2 border-t-2 transition-all cursor-pointer flex items-center gap-1.5 uppercase font-mono font-bold ${
                            syncNetTab === 'sweep'
                              ? 'border-green-500 bg-green-950/20 text-white font-extrabold shadow-[0_-2px_6px_rgba(0,255,65,0.05)]'
                              : 'border-transparent text-green-700 hover:text-green-400 hover:bg-green-905/5'
                          }`}
                        >
                          SWEEP FUNCTION
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            sound.playBeep(440, 0.05, "sine");
                            setSyncNetTab('scanner');
                          }}
                          className={`px-4 py-2 border-t-2 transition-all cursor-pointer flex items-center gap-1.5 uppercase font-mono font-bold ${
                            syncNetTab === 'scanner'
                              ? 'border-purple-500 bg-purple-950/20 text-white font-extrabold shadow-[0_-2px_6px_rgba(168,85,247,0.05)]'
                              : 'border-transparent text-purple-700 hover:text-purple-400 hover:bg-neutral-800/10'
                          }`}
                        >
                          AREA SCANNER
                        </button>
                      </div>
                    )}

                    <div className={`flex-1 min-h-0 ${syncNetTab === 'scanner' ? 'overflow-y-auto custom-pox-scrollbar' : 'overflow-y-auto'}`}>
                      {syncNetTab === 'sweep' ? (
                        <div className={activeHack ? "grid grid-cols-1 md:grid-cols-2 gap-4 h-full relative" : "w-full h-full relative"}>
                          {activeHack ? (
                      /* ACTIVE BREACH ATTACK VECTOR ARENA */
                      <>
                        {/* Column 1: Attacker Selection or Live Battle Controls */}
                        <div className="bg-neutral-900/20 border border-red-950/60 p-5 rounded flex flex-col justify-between overflow-y-auto">
                          {activeHack.status === 'selecting_attacker' ? (
                            <div className="space-y-4 flex flex-col justify-between h-full">
                              <div>
                                <div className="flex justify-between items-center mb-1 text-[9px] uppercase text-red-500 font-bold tracking-widest font-mono">
                                  <span>[ OUTWARD EXPLOIT INITIATED ]</span>
                                  <span className="animate-pulse">● SELECT STRIKER</span>
                                </div>
                                <h2 className="text-xs font-bold text-white tracking-wider mb-1">DESIGNATE ATTACK SPECIMEN</h2>
                                <p className="text-[10px] text-red-400 font-mono mb-3 leading-snug">
                                  Select an active cyborg from your archive stock to transmit into peer firewall breach socket at host "{activeHack.partner.uid}".
                                </p>
                              </div>

                              <div className="flex-1 max-h-[140px] overflow-y-auto bg-neutral-950 border border-red-950 rounded p-2.5 space-y-1.5 scrollbar-thin">
                                {creatures.map((mine, idx) => (
                                  <div 
                                    key={idx}
                                    onClick={() => handleSelectAttackerForHack(mine)}
                                    className="p-2 rounded bg-black border border-red-950/60 hover:border-red-500 transition-all cursor-pointer flex justify-between items-center group"
                                  >
                                    <div className="min-w-0 flex-1 pr-2 text-white font-mono">
                                      <div className="flex items-center gap-1 flex-wrap">
                                        <span className="text-[11px] font-bold text-neutral-300 group-hover:text-red-400 block truncate">{mine.name}</span>
                                        {getCoherence(mine.sequence, targetSequence) === 'full' ? (
                                          <span className="px-1 bg-emerald-950 text-[#00FF41] border border-emerald-500 rounded text-[6.5px] font-bold font-mono tracking-widest select-none leading-none">
                                            FULL COH
                                          </span>
                                        ) : getCoherence(mine.sequence, targetSequence) === 'partial' ? (
                                          <span className="px-1 bg-yellow-950/80 text-yellow-500 border border-yellow-600/55 rounded text-[6.5px] font-bold font-mono tracking-wider select-none leading-none">
                                            PART COH
                                          </span>
                                        ) : null}
                                      </div>
                                      <span className="text-[8px] text-red-450 font-mono block select-none">
                                        ATK: {mine.attack} &bull; DEF: {mine.defense} &bull; SPD: {mine.speed}
                                      </span>
                                    </div>
                                    <span className="text-[8px] text-red-450 bg-red-950 border border-red-900 px-2 py-0.5 rounded font-mono font-bold uppercase transition-colors group-hover:bg-red-900 group-hover:text-white">
                                      STRIKE
                                    </span>
                                  </div>
                                ))}
                                {creatures.length === 0 && (
                                  <div className="text-center text-red-900 py-6 text-xs font-mono">
                                    No compiled specimens detected in containment vaults!
                                  </div>
                                )}
                              </div>

                              <button 
                                onClick={handleClearHackSession} 
                                className="w-full text-center text-neutral-500 hover:text-white text-[10px] uppercase font-bold py-1.5 border border-neutral-800 rounded mt-1 cursor-pointer transition-all active:scale-[0.98]"
                              >
                                ABORT BREACH SOCKET
                              </button>
                            </div>
                          ) : activeHack.status === 'battling' && activeHack.attacker ? (
                            /* LIVE INTENSE COMBAT STAGE */
                            <div className="space-y-4 flex flex-col justify-between h-full">
                              <div>
                                <div className="flex justify-between items-center text-[9px] uppercase font-bold text-red-500 font-mono">
                                  <span>[ INTRUSION STREAM ENGAGED ]</span>
                                  <span className="text-red-505 font-black animate-pulse font-mono tracking-widest">● BATTLE ENGAGED</span>
                                </div>
                                <h3 className="text-xs font-bold text-white tracking-wider mb-2">FIREWALL INTERFACE BREACH</h3>
                              </div>

                              {/* Target Host Guard (Defender) block */}
                              <div className="bg-neutral-950 p-2.5 rounded border border-red-950/60 font-mono space-y-1.5">
                                <div className="flex justify-between items-center text-[9px]">
                                  <span className="text-neutral-500 uppercase">🛡️ HOST DEFENDER DAEMON:</span>
                                  <span className={`px-1.5 py-0.5 rounded text-[8px] font-sans font-bold text-black ${
                                    activeHack.defender?.faction === 'Infection' ? 'bg-red-500' :
                                    activeHack.defender?.faction === 'Mech' ? 'bg-blue-400' :
                                    activeHack.defender?.faction === 'Parasite' ? 'bg-purple-500' : 'bg-green-400'
                                  }`}>
                                    {activeHack.defender?.faction}
                                  </span>
                                </div>
                                <div className="flex justify-between font-mono">
                                  <span className="text-white font-bold text-[11px] truncate">"{activeHack.defender?.name}"</span>
                                  <span className="text-red-500 font-bold text-[10.5px]">{activeHack.defenderCurrentHp} / {activeHack.defender?.vitality} HP</span>
                                </div>
                                {/* HP progress bar */}
                                <div className="w-full bg-neutral-900 h-1.5 rounded overflow-hidden">
                                  <div 
                                    className="h-full bg-red-605 transition-all duration-300"
                                    style={{ width: `${Math.max(0, (activeHack.defenderCurrentHp / (activeHack.defender?.vitality || 100)) * 100)}%` }}
                                  />
                                </div>
                              </div>

                              {/* Cool VS connector */}
                              <div className="flex items-center justify-center -my-1 text-red-700 font-black text-xs font-mono tracking-widest select-none">
                                &lt;⚡ BREACH FORCE IMPACT VS FIREWALL ⚡&gt;
                              </div>

                              {/* Attacking Specimen (Player) block */}
                              <div className="bg-neutral-950 p-2.5 rounded border border-green-950 font-mono space-y-1.5">
                                <div className="flex justify-between items-center text-[9px]">
                                  <span className="text-[#00FF41] uppercase">🚀 YOUR COMBAT PAYLOAD:</span>
                                  <span className={`px-1.5 py-0.5 rounded text-[8px] font-sans font-bold text-black ${
                                    activeHack.attacker?.faction === 'Infection' ? 'bg-red-500' :
                                    activeHack.attacker?.faction === 'Mech' ? 'bg-blue-400' :
                                    activeHack.attacker?.faction === 'Parasite' ? 'bg-purple-500' : 'bg-green-400'
                                  }`}>
                                    {activeHack.attacker?.faction}
                                  </span>
                                </div>
                                <div className="flex justify-between font-mono">
                                  <span className="text-white font-bold text-[11px] truncate">"{activeHack.attacker?.name}"</span>
                                  <span className="text-[#00FF41] font-bold text-[10.5px]">{activeHack.attackerCurrentHp} / {activeHack.attacker?.vitality} HP</span>
                                </div>
                                {/* HP progress bar */}
                                <div className="w-full bg-neutral-900 h-1.5 rounded overflow-hidden">
                                  <div 
                                    className="h-full bg-[#00FF41] transition-all duration-300"
                                    style={{ width: `${Math.max(0, (activeHack.attackerCurrentHp / (activeHack.attacker?.vitality || 100)) * 100)}%` }}
                                  />
                                </div>
                              </div>

                              {/* LAUNCH Round Trigger with mutant capabilities */}
                              {activeHack.attacker && activeHack.attacker.sequence.length > 64 && (() => {
                                const moves = getUnlockedMoves(activeHack.attacker.sequence);
                                if (moves.length === 0) return null;
                                return (
                                  <div className="space-y-1.5 border border-amber-950/30 bg-black/40 p-2.5 rounded font-sans">
                                    <div className="text-[8.5px] text-amber-500 uppercase tracking-wider font-mono font-bold">
                                      🛸 SPECIAL MUTANT maneuvers (ONCE PER BATTLE):
                                    </div>
                                    <div className="grid grid-cols-1 gap-1.5">
                                      {moves.map((mv, mvIdx) => {
                                        const hasBeenUsed = activeHack.usedSpecialMoves?.includes(mv.name);
                                        return (
                                          <button
                                            key={mvIdx}
                                            type="button"
                                            disabled={hasBeenUsed}
                                            onClick={() => {
                                              handleRunHackRound({ type: mv.type, name: mv.name });
                                            }}
                                            className={`text-[9.5px] px-2.5 py-1.5 font-mono rounded border flex items-center justify-between text-left transition-all ${
                                              hasBeenUsed
                                                ? 'bg-neutral-950 border-neutral-900 text-neutral-600 cursor-not-allowed select-none'
                                                : mv.type === 'healing'
                                                ? 'bg-emerald-950/30 border-emerald-900/60 text-emerald-400 hover:bg-emerald-900/20 cursor-pointer shadow-[inset_0_0_4px_rgba(16,185,129,0.1)]'
                                                : 'bg-indigo-950/30 border-indigo-900/60 text-cyan-400 hover:bg-indigo-900/20 cursor-pointer shadow-[inset_0_0_4px_rgba(6,182,212,0.1)]'
                                            }`}
                                          >
                                            <div className="flex flex-col">
                                              <span className="font-bold tracking-widest uppercase text-[9px]">
                                                {mv.type === 'healing' ? '⚡ siph' : '🛡️ evas'} | {mv.name}
                                              </span>
                                              <span className="text-[7.5px] text-neutral-400 mt-0.5 leading-tight">{mv.description}</span>
                                            </div>
                                            {!hasBeenUsed && (
                                              <span className="text-[7.5px] bg-[#00FF41]/10 text-[#00FF41] px-1 rounded border border-[#00FF41]/20 font-bold whitespace-nowrap">
                                                LOAD
                                              </span>
                                            )}
                                          </button>
                                        );
                                      })}
                                    </div>
                                  </div>
                                );
                              })()}

                              <button
                                type="button"
                                onClick={() => handleRunHackRound()}
                                className="w-full py-3 bg-red-800 hover:bg-red-650 text-white font-bold text-[11px] uppercase tracking-wider rounded-sm animate-pulse border border-red-500 cursor-pointer shadow-[0_0_15px_rgba(239,68,68,0.35)] transition-all active:scale-[0.98]"
                              >
                                FIRE COMBAT ACTION PACKET
                              </button>
                            </div>
                          ) : (
                            /* COMPLETED BATTLE SCREEN */
                            <div className="flex flex-col items-center justify-center text-center space-y-4 py-8 h-full">
                              {activeHack.winnerUid === 'PLAYER' ? (
                                (() => {
                                  const snatched = activeHack.harvestedGene || "";
                                  const extraSnatched = activeHack.harvestedExtraGenes || [];
                                  
                                  // 1. Matches a gene in the daily target (targetSequence)
                                  const dailyTargetGenes: string[] = [];
                                  for (let i = 0; i < 64; i += 8) {
                                    dailyTargetGenes.push(targetSequence.slice(i, i + 8));
                                  }
                                  const matchesDaily = snatched && dailyTargetGenes.includes(snatched);
                                  
                                  // 2. Matches one from the G.E.N. Network target list
                                  const matchesGenNetwork = snatched && genMails.some(mail => 
                                    mail.sequences && mail.sequences.some(seq => seq.includes(snatched))
                                  );
                                  
                                  // 3. Sought in a request from another player
                                  const soughtByAnotherPlayer = snatched && (
                                    nearbyUsers.some(user => user.targetSequence && user.targetSequence.includes(snatched)) ||
                                    genMails.some(mail => 
                                      mail.type === 'trade_request' && 
                                      mail.tradeRequest && 
                                      mail.tradeRequest.partner.targetSequence && 
                                      mail.tradeRequest.partner.targetSequence.includes(snatched)
                                    )
                                  );

                                  return (
                                    <div className="w-full max-w-sm bg-neutral-950 p-4 border border-[#00FF41]/45 rounded font-mono text-left space-y-3 shadow-[0_0_15px_rgba(0,255,65,0.15)]">
                                      <div className="flex items-center gap-2">
                                        <div className="w-8 h-8 rounded-full bg-green-950 border border-[#00FF41] flex items-center justify-center text-[#00FF41] shrink-0 animate-pulse">
                                          <Check className="w-4 h-4" />
                                        </div>
                                        <div>
                                          <span className="text-[10px] font-black text-[#00FF41] uppercase tracking-wider block">BREACH SUCCESSFUL</span>
                                          <span className="text-[7px] text-green-700 uppercase">SYS_OVERRIDE_0x0DF0F41A_SECURE</span>
                                        </div>
                                      </div>
                                      
                                      <div className="p-2 bg-black border border-green-950 rounded space-y-1">
                                        <div className="text-[7.5px] text-neutral-500 uppercase tracking-widest font-bold">DNA DECRYPTED:</div>
                                        
                                        <div className="flex items-center justify-between">
                                          <span className="text-[10.5px] font-black text-white tracking-widest bg-emerald-955/40 border border-emerald-500/30 px-1.5 py-0.5 rounded select-all font-mono">
                                            🧬 {snatched || "UNKNOWN"}
                                          </span>
                                          <span className="text-[7.5px] text-[#00FF41] font-bold bg-green-950 border border-green-800/40 px-1 py-0.5 rounded">
                                            PRIMARY HARVEST
                                          </span>
                                        </div>

                                        {extraSnatched.length > 0 && (
                                          <div className="pt-0.5">
                                            <div className="text-[6.5px] text-neutral-500 uppercase font-black tracking-wider leading-none mb-1">Recovered Appended Genes:</div>
                                            <div className="flex flex-wrap gap-1">
                                              {extraSnatched.map((ec, idx) => (
                                                <span key={idx} className="text-[8px] font-mono font-bold text-cyan-400 bg-cyan-950/45 border border-cyan-800/40 px-1 py-0.5 rounded select-all">
                                                  {ec}
                                                </span>
                                              ))}
                                            </div>
                                          </div>
                                        )}
                                      </div>

                                      <div className="space-y-1">
                                        <div className="text-[7.5px] text-green-700 font-bold uppercase tracking-wider">GENETIC DISPATCH LEDGER INTELLIGENCE:</div>
                                        
                                        {/* Match Indicators checklist */}
                                        <div className="space-y-1 text-[7.5px]/relaxed">
                                          {/* Daily target match */}
                                          <div className={`p-1 rounded border flex justify-between items-center ${
                                            matchesDaily 
                                              ? 'bg-amber-950/30 border-amber-500/40 text-amber-400 font-bold'
                                              : 'bg-black/40 border-neutral-900 text-neutral-500'
                                          }`}>
                                            <span>🎯 DAILY MATRIX COHERENCE GOAL:</span>
                                            <span>{matchesDaily ? "● MATCH FOUND" : "⬡ NO COHERENCE"}</span>
                                          </div>

                                          {/* G.E.N. Network Target list */}
                                          <div className={`p-1 rounded border flex justify-between items-center ${
                                            matchesGenNetwork 
                                              ? 'bg-cyan-950/30 border-cyan-500/40 text-cyan-400 font-bold'
                                              : 'bg-black/40 border-neutral-900 text-neutral-500'
                                          }`}>
                                            <span>📡 G.E.N. NETWORK TARGET LIST:</span>
                                            <span>{matchesGenNetwork ? "● MATCHED BROADCAST" : "⬡ UNLISTED GENE"}</span>
                                          </div>

                                          {/* Pear request matching */}
                                          <div className={`p-1 rounded border flex justify-between items-center ${
                                            soughtByAnotherPlayer 
                                              ? 'bg-purple-950/30 border-purple-500/40 text-purple-400 font-bold'
                                              : 'bg-black/40 border-neutral-900 text-neutral-500'
                                          }`}>
                                            <span>👥 PEER NODE TRADE DESIDERATA:</span>
                                            <span>{soughtByAnotherPlayer ? "● SOUGHT BY PEER" : "⬡ NO PENDING WANTS"}</span>
                                          </div>
                                        </div>
                                      </div>
                                    </div>
                                  );
                                })()
                              ) : activeHack.winnerUid === 'TIE' ? (
                                <>
                                  <div className="w-12 h-12 rounded-full bg-neutral-500/10 border-2 border-neutral-500 flex items-center justify-center text-neutral-400">
                                    <AlertTriangle className="w-7 h-7" />
                                  </div>
                                  <span className="text-sm font-bold text-neutral-400 uppercase block tracking-widest text-center">MUTUAL SYSTEM FAULT</span>
                                  <p className="text-[10px] text-neutral-550 max-w-xs font-mono leading-relaxed">Both attacking specimen and passive defending firewalls experienced clean stack discharge synchronously.</p>
                                </>
                              ) : (
                                <>
                                  <div className="w-12 h-12 rounded-full bg-red-500/10 border-2 border-red-500 flex items-center justify-center text-red-500">
                                    <ShieldAlert className="w-7 h-7" />
                                  </div>
                                  {activeHack.isTradeInterceptor ? (
                                    <>
                                      <span className="text-sm font-bold text-red-500 uppercase block tracking-widest text-center">FIREWALL BREACHED</span>
                                      <p className="text-[10px] text-red-405 max-w-xs font-mono leading-relaxed bg-black/60 p-3 rounded border border-red-950">
                                        Security Breached. Peer host "{activeHack.partner.uid}" successfully pierced our firewall defenses. Designated defender "{activeHack.attacker?.name}" was neutralized.
                                      </p>
                                    </>
                                  ) : (
                                    <>
                                      <span className="text-sm font-bold text-red-500 uppercase block tracking-widest text-center">FIREWALL EXPULSION</span>
                                      <p className="text-[10px] text-red-405 max-w-xs font-mono leading-relaxed bg-black/60 p-3 rounded border border-red-950">
                                        Breach blocked. Defender "{activeHack.defender?.name}" successfully pushed our hacker bio-mutant package out of system sectors.
                                      </p>
                                    </>
                                  )}
                                </>
                              )}

                              <button 
                                onClick={handleClearHackSession} 
                                className="px-5 py-2 bg-neutral-900 hover:bg-neutral-800 border border-neutral-800 text-white font-bold text-[10px] hover:text-[#00FF41] rounded-sm cursor-pointer uppercase tracking-wider transition-all"
                              >
                                CLOSE MALICIOUS SOCKET
                              </button>
                            </div>
                          )}
                        </div>

                        {/* Column 2: Terminal stream logs */}
                        <div className="bg-neutral-900/20 border border-red-955/60 p-5 rounded flex flex-col justify-between h-full">
                          <div>
                            <div className="text-[9px] uppercase text-red-650 font-bold tracking-widest mb-1 select-none flex items-center gap-1 font-mono">
                              <Terminal className="w-3.5 h-3.5" /> [ PORT 3000 BREACH BUFFER STREAM ]
                            </div>
                            <h2 className="text-xs font-bold text-white tracking-wider mb-2">HEX DECRYPTER SYSTEM LOG</h2>
                          </div>

                          <div ref={combatLogRef} className="flex-1 max-h-[220px] overflow-y-auto space-y-1 bg-black/90 p-3 border border-red-950/60 rounded font-mono text-[9px] text-[#00FF41] scrollbar-thin">
                            {activeHack.battleLogs.map((logLine, lIdx) => (
                              <div key={lIdx} className="leading-snug break-words">
                                {logLine}
                              </div>
                            ))}
                          </div>

                          <div className="p-2.5 bg-red-950/20 border border-red-900/40 rounded text-[8.5px] leading-normal mt-3 text-red-405 font-mono font-bold">
                            <span className="font-bold text-red-400 uppercase block mb-0.5">WARNING SYSTEM OVERRIDE:</span>
                            Breach hacks snatch genuine genes from target genetic arrays, but a loss reports leakages.
                          </div>
                        </div>
                      </>
                    ) : (
                      <>
                        {/* Panel 4: Trade / Nearby Sync (Left) */}
                        <div className="bg-neutral-900/20 border border-green-900/40 p-5 rounded flex flex-col justify-between">
                      {activeTrade ? (
                        <div className="flex flex-col justify-between h-full bg-black/45 border border-green-500/40 p-3 rounded-lg relative overflow-hidden">
                          <div className="flex justify-between items-center pb-2 border-b border-green-900/40 text-[9px] text-green-600 font-mono">
                            <span>SYNC-NET NODE TRANSFER</span>
                            <span className="text-[#00FF41] font-bold animate-pulse">{activeTrade.status.toUpperCase()}</span>
                          </div>

                          {showTradeDiagnostics && (
                            <motion.div
                              key="trade-diagnostics-popup"
                              initial={{ opacity: 0, scale: 0.98 }}
                              animate={{ opacity: 1, scale: 1 }}
                              exit={{ opacity: 0, scale: 0.98 }}
                              className="absolute inset-0 bg-[#020d04]/98 border border-[#00FF41]/60 rounded-md p-4 flex flex-col z-40 overflow-hidden font-mono text-left"
                            >
                              <div className="flex justify-between items-center border-b border-green-950 pb-2 mb-3 select-none">
                                <span className="text-[10px] text-[#00FF41] font-bold tracking-widest uppercase flex items-center gap-1.5">
                                  <span className="w-1.5 h-1.5 bg-[#00FF41] rounded-full animate-pulse" />
                                  [ LINK INITIATOR ANALYZER ]
                                </span>
                                <button
                                  type="button"
                                  onClick={() => {
                                    sound.playBeep(440, 0.05, "sine");
                                    setShowTradeDiagnostics(false);
                                  }}
                                  className="px-2 py-0.5 bg-red-950/80 hover:bg-red-900 border border-red-800 text-red-500 hover:text-white rounded text-[8.5px] cursor-pointer font-bold tracking-wider"
                                >
                                  ✕ CLOSE
                                </button>
                              </div>

                              <div className="flex-1 overflow-y-auto space-y-3.5 pr-0.5 custom-pox-scrollbar">
                                {/* Initiator Target Sequence */}
                                <div className="p-2.5 bg-neutral-950 border border-green-950/60 rounded">
                                  <span className="text-green-500 text-[9px] tracking-wider block uppercase select-none mb-1">Initiator Target Sequence</span>
                                  {renderColorCodedSequence(tradeDiag.targetSeq)}
                                </div>

                                {/* Receiver Target Sequence */}
                                <div className="p-2.5 bg-neutral-950 border border-green-950/60 rounded">
                                  <span className="text-green-500 text-[9px] tracking-wider block uppercase select-none mb-1">Receiver Target Sequence</span>
                                  {renderColorCodedSequence(tradeDiag.receiverTargetSeq)}
                                </div>

                                {/* Sequence Match Telemetry */}
                                <div className="p-2.5 bg-neutral-950 border border-green-950/60 rounded">
                                  <span className="text-yellow-500 text-[9px] tracking-wider block uppercase select-none mb-1">Sequence Match Telemetry</span>
                                  <div className="text-amber-400 font-bold block text-[9.5px] leading-relaxed uppercase">
                                    <div>You (This Node) owns: {tradeDiag.thisNodeMatches} matching sequences</div>
                                    <div>{activeTrade?.partner?.uid || "Peer"} (Peer Node) owns: {tradeDiag.peerMatches} of matching sequences</div>
                                  </div>
                                </div>
                              </div>
                            </motion.div>
                          )}

                          <div className="flex-1 my-3 pr-1">


                            {/* State: Requested outer sync */}
                            {activeTrade.status === 'requested_by_me' && (
                              <div className="flex flex-col items-center justify-center text-center py-4 space-y-3">
                                <RefreshCw className="w-8 h-8 text-amber-500 animate-spin" />
                                <span className="text-xs text-amber-400 font-bold uppercase animate-pulse">transmitting match connection...</span>
                                <p className="text-[10px] text-green-600 max-w-xs font-mono leading-relaxed">Sending peer link invitation to user UID: "{activeTrade.partner.uid}" over 30ft frequency.</p>
                                
                                <div className="w-full bg-red-950/20 border border-red-900/50 p-2.5 rounded text-center space-y-1 mt-2 select-none">
                                  <span className="text-[8.5px] text-red-400 font-bold block uppercase tracking-wide">⚠️ INTERCEPT NODE VULNERABILITY</span>
                                  <button 
                                    onClick={() => handleInitiateTradeInterceptorHack(activeTrade.partner)}
                                    className="w-full bg-red-900 hover:bg-red-600 font-bold text-[9px] text-white py-1.5 rounded uppercase cursor-pointer"
                                  >
                                    ⚡ INJECT INT-ATTACK PACKET
                                  </button>
                                </div>
                              </div>
                            )}

                            {/* State: Rejected transfer */}
                            {activeTrade.status === 'rejected' && (
                              <div className="flex flex-col items-center justify-center text-center py-3 space-y-2">
                                <ShieldAlert className="w-8 h-8 text-red-500" />
                                <span className="text-xs text-red-500 font-bold uppercase select-none">TRANSMISSION SEVERED</span>
                                <p className="text-[10px] text-green-600 max-w-xs font-mono leading-normal">Node declined linkage request or has left the 30ft signal field.</p>
                                <button onClick={handleClearTradeSession} className="mt-2 px-3 py-1 bg-red-950/50 border border-red-900 text-red-500 hover:bg-red-900 text-[10px] font-bold cursor-pointer rounded-sm">✕ SEVER CONNECTION</button>
                              </div>
                            )}

                            {/* State: Incoming request alert popup */}
                            {activeTrade.status === 'incoming_request' && (
                              <div className="space-y-4">
                                <div className="text-center">
                                  <span className="text-xs text-red-400 font-bold block animate-pulse">ALERT: LOCAL NODE PING CONNECTION</span>
                                  <p className="text-[10px] text-green-500 mt-1">Host node UID: <strong className="text-white font-semibold">{activeTrade.partner.uid}</strong></p>
                                </div>
                                
                                <div className="bg-black p-3 rounded border border-green-900 text-center space-y-0.5">
                                  <span className="text-[8.5px] text-green-500 block uppercase select-none font-bold">THEY OFFER CREATURE:</span>
                                  <span className="text-[#00FF41] font-bold text-xs uppercase block">{activeTrade.partnerOfferedCreature?.name}</span>
                                  <span className="text-[9px] text-green-700 block truncate">{activeTrade.partnerOfferedCreature?.type}</span>
                                </div>

                                <div className="bg-red-950/20 border border-red-900/50 p-2.5 rounded text-center space-y-1 select-none">
                                  <span className="text-[8.5px] text-red-400 font-bold block uppercase tracking-wide">⚠️ VULNERABLE PEER SYNC-LINE DETECTED!</span>
                                  <button 
                                    onClick={() => handleInitiateTradeInterceptorHack(activeTrade.partner, activeTrade.partnerOfferedCreature)}
                                    className="w-full bg-red-900 hover:bg-red-600 font-bold text-[9px] text-white py-1.5 rounded uppercase cursor-pointer"
                                  >
                                    ⚡ EXPLOIT & COUNTER-HACK NODE
                                  </button>
                                </div>

                                <div className="flex gap-2">
                                  <button onClick={handleRejectIncomingTrade} className="flex-1 bg-red-950 border border-red-900 text-red-500 py-2 rounded-sm uppercase font-bold text-[10px] hover:bg-red-900/40 cursor-pointer">[ REJECT ]</button>
                                  <button onClick={handleAcceptIncomingTrade} className="flex-1 bg-[#00FF41] text-black py-2 rounded-sm uppercase font-bold text-[10px] hover:bg-green-400 cursor-pointer shadow-[0_0_10px_rgba(0,255,102,0.3)]">[ ACCEPT LINK ]</button>
                                </div>
                              </div>
                            )}

                            {/* State: Link Accepted! Prompt select specimen to trade out */}
                            {activeTrade.status === 'accepted' && (
                              (() => {
                                const partnerTarget = activeTrade.partner.targetSequence || targetSequence || "";
                                const fullMatches = creatures.filter(c => getCoherence(c.sequence, partnerTarget) === 'full');
                                const partialMatches = creatures.filter(c => getCoherence(c.sequence, partnerTarget) === 'partial');

                                return (
                                  <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 h-full min-h-0 text-left">
                                    {/* LEFT PANE - TRADE MANAGER & SPECIMEN LISTS */}
                                    <div className="flex flex-col bg-black/40 border border-green-900/30 p-3.5 rounded-lg space-y-3.5 min-h-0 justify-between">
                                      {/* Header message */}
                                      <div className="text-center py-0.5 select-none border-b border-green-900/20 pb-2">
                                        <span className="text-xs text-[#00FF41] font-black block tracking-widest uppercase">Peer Node Link Active</span>
                                        <span className="text-[9.5px] text-green-600 block mt-0.5 uppercase font-mono">Sync-Net Transfer Ready</span>
                                      </div>

                                      {/* Requirement 1: Requester's daily target sequence recolored with 8-gene pattern */}
                                      <div className="space-y-1 font-mono">
                                        <span className="text-[9px] text-green-500 font-bold uppercase tracking-wider block select-none">Requester daily target</span>
                                        <div className="p-2.5 bg-black rounded border border-green-950 font-bold break-all select-all tracking-widest leading-relaxed">
                                          {render8BlockColorCodedSequence(partnerTarget)}
                                        </div>
                                      </div>

                                      {/* Requirement 2: Full & Partial Coherence Matches selection */}
                                      <div className="flex-1 flex flex-col space-y-3 overflow-y-auto pr-1 select-none custom-pox-scrollbar max-h-[300px]">
                                        {/* Full Coherence Section */}
                                        <div className="space-y-1.5">
                                          <div className="flex justify-between items-center px-1">
                                            <span className="text-[9px] text-emerald-400 font-extrabold uppercase tracking-wide">Full Coherence Matches</span>
                                            <span className="text-[8px] text-[#00FF41] font-mono font-bold bg-emerald-950/40 px-1.5 rounded border border-emerald-900/30">
                                              {fullMatches.length} CREATURES
                                            </span>
                                          </div>

                                          <div className="space-y-1 bg-emerald-950/5 p-1.5 rounded border border-emerald-910/20 max-h-[140px] overflow-y-auto custom-pox-scrollbar">
                                            {fullMatches.length > 0 ? (
                                              fullMatches.map((mine) => {
                                                const isSelected = tradeSelectedCreature?.id === mine.id;
                                                return (
                                                  <div
                                                    key={mine.id}
                                                    onClick={() => {
                                                      sound.playBeep(440, 0.05, "sine");
                                                      setTradeSelectedCreature(mine);
                                                    }}
                                                    className={`p-1.5 rounded cursor-pointer transition-all flex justify-between items-center text-left text-[9px] ${
                                                      isSelected
                                                        ? "bg-emerald-955/30 border-2 border-[#00FF41] shadow-[0_0_8px_rgba(0,255,65,0.15)]"
                                                        : "bg-black border border-emerald-900/20 hover:border-emerald-500/60"
                                                    }`}
                                                  >
                                                    <div className="min-w-0 flex-1 pr-1.5 font-mono">
                                                      <div className="flex items-center gap-1.5 flex-wrap">
                                                        <span className="text-[10px] font-bold text-white block truncate">{mine.name}</span>
                                                        <span className="text-[7px] text-emerald-400 font-bold bg-black border border-emerald-900/60 px-1 rounded-sm leading-none py-0.5">
                                                          {mine.faction.toUpperCase()}
                                                        </span>
                                                      </div>
                                                      <div className="text-[8px] text-neutral-450 flex items-center gap-1.5 flex-wrap mt-0.5">
                                                        <span>HP:<strong className="text-white">{mine.vitality}</strong></span>
                                                        <span>ATK:<strong className="text-white">{mine.attack}</strong></span>
                                                        <span>DEF:<strong className="text-white">{mine.defense}</strong></span>
                                                        <span>SPD:<strong className="text-white">{mine.speed}</strong></span>
                                                      </div>
                                                    </div>
                                                    <span className={`text-[8px] px-1.5 py-0.5 rounded font-black uppercase whitespace-nowrap font-mono border ${
                                                      isSelected 
                                                        ? "text-black bg-[#00FF41] border-[#00FF41]" 
                                                        : "text-emerald-400 border-emerald-900/45"
                                                    }`}>
                                                      {isSelected ? "LOADED" : "SELECT"}
                                                    </span>
                                                  </div>
                                                );
                                              })
                                            ) : (
                                              <div className="text-center text-green-700 py-3 text-[9px] uppercase tracking-wide font-mono select-none">
                                                Zero matched specimens in vault.
                                              </div>
                                            )}
                                          </div>
                                        </div>

                                        {/* Partial Coherence Section */}
                                        <div className="space-y-1.5">
                                          <div className="flex justify-between items-center px-1">
                                            <span className="text-[9px] text-amber-400 font-extrabold uppercase tracking-wide">Partial Coherence Matches</span>
                                            <span className="text-[8px] text-amber-400 font-mono font-bold bg-amber-950/40 px-1.5 rounded border border-amber-900/30">
                                              {partialMatches.length} CREATURES
                                            </span>
                                          </div>

                                          <div className="space-y-1 bg-amber-950/5 p-1.5 rounded border border-amber-910/20 max-h-[140px] overflow-y-auto custom-pox-scrollbar">
                                            {partialMatches.length > 0 ? (
                                              partialMatches.map((mine) => {
                                                const isSelected = tradeSelectedCreature?.id === mine.id;
                                                return (
                                                  <div
                                                    key={mine.id}
                                                    onClick={() => {
                                                      sound.playBeep(440, 0.05, "sine");
                                                      setTradeSelectedCreature(mine);
                                                    }}
                                                    className={`p-1.5 rounded cursor-pointer transition-all flex justify-between items-center text-left text-[9px] ${
                                                      isSelected
                                                        ? "bg-amber-955/30 border-2 border-amber-500 shadow-[0_0_8px_rgba(245,158,11,0.15)]"
                                                        : "bg-black border border-amber-900/20 hover:border-amber-500/60"
                                                    }`}
                                                  >
                                                    <div className="min-w-0 flex-1 pr-1.5 font-mono">
                                                      <div className="flex items-center gap-1.5 flex-wrap">
                                                        <span className="text-[10px] font-bold text-white block truncate">{mine.name}</span>
                                                        <span className="text-[7px] text-amber-400 font-bold bg-black border border-amber-900/60 px-1 rounded-sm leading-none py-0.5">
                                                          {mine.faction.toUpperCase()}
                                                        </span>
                                                      </div>
                                                      <div className="text-[8px] text-neutral-400 flex items-center gap-1.5 flex-wrap mt-0.5">
                                                        <span>HP:<strong className="text-white">{mine.vitality}</strong></span>
                                                        <span>ATK:<strong className="text-white">{mine.attack}</strong></span>
                                                        <span>DEF:<strong className="text-white">{mine.defense}</strong></span>
                                                        <span>SPD:<strong className="text-white">{mine.speed}</strong></span>
                                                      </div>
                                                    </div>
                                                    <span className={`text-[8px] px-1.5 py-0.5 rounded font-black uppercase whitespace-nowrap font-mono border ${
                                                      isSelected 
                                                        ? "text-black bg-amber-400 border-amber-400" 
                                                        : "text-amber-400 border-amber-900/45"
                                                    }`}>
                                                      {isSelected ? "LOADED" : "SELECT"}
                                                    </span>
                                                  </div>
                                                );
                                              })
                                            ) : (
                                              <div className="text-center text-amber-700 py-3 text-[9px] uppercase tracking-wide font-mono select-none">
                                                Zero matched specimens in vault.
                                              </div>
                                            )}
                                          </div>
                                        </div>
                                      </div>

                                      {/* Action Buttons */}
                                      <div className="pt-3 border-t border-green-900/30 flex gap-2 w-full mt-auto font-mono select-none">
                                        <button
                                          type="button"
                                          onClick={() => {
                                            sound.playBeep(440, 0.05, "sine");
                                            handleClearTradeSession();
                                          }}
                                          className="flex-1 py-1.5 bg-red-950/20 hover:bg-red-900 border border-red-500/50 text-red-500 hover:text-white font-bold uppercase text-[9px] rounded-sm cursor-pointer tracking-wider transition-all active:scale-[0.98]"
                                        >
                                          CANCEL TRANSFER
                                        </button>
                                        <button
                                          type="button"
                                          disabled={!tradeSelectedCreature}
                                          onClick={() => {
                                            if (tradeSelectedCreature) {
                                              handleExecuteTrade(tradeSelectedCreature);
                                            }
                                          }}
                                          className={`flex-1 py-1.5 font-extrabold uppercase text-[9px] rounded-sm tracking-wider transition-all duration-150 ${
                                            tradeSelectedCreature
                                              ? "bg-[#00FF41] hover:bg-green-400 text-black cursor-pointer shadow-[0_0_12px_rgba(0,255,65,0.25)] active:scale-95"
                                              : "bg-neutral-900 border border-neutral-800 text-neutral-600 cursor-not-allowed"
                                          }`}
                                        >
                                          {tradeSelectedCreature ? "⚡ COMPLETE MUTATION SWAP" : "AWAITING SAMPLE..."}
                                        </button>
                                      </div>
                                    </div>

                                    {/* RIGHT PANE - CONDENSED MINIATURE CREATURE CARDS */}
                                    <div className="flex flex-col gap-3 min-h-0 bg-black/20 border border-purple-900/20 p-3 rounded-lg overflow-y-auto custom-pox-scrollbar">
                                      {/* Requester miniature creature card */}
                                      {(() => {
                                        const reqCreature = activeTrade.partnerOfferedCreature;
                                        if (!reqCreature) {
                                          return (
                                            <div className="flex-1 flex flex-col items-center justify-center border border-dashed border-red-900/40 p-3 rounded bg-black/10 select-none font-mono">
                                              <span className="text-[10px] text-red-500 font-bold uppercase">No Requester Creature Offered</span>
                                            </div>
                                          );
                                        }

                                        let factionColor = "text-red-400 border-red-900/55 bg-red-950/15";
                                        if (reqCreature.faction === "Mech") {
                                          factionColor = "text-yellow-400 border-yellow-900/50 bg-yellow-950/15";
                                        } else if (reqCreature.faction === "Parasite") {
                                          factionColor = "text-purple-400 border-purple-900/50 bg-purple-950/15";
                                        } else if (reqCreature.faction === "Containment") {
                                          factionColor = "text-cyan-400 border-cyan-900/55 bg-cyan-950/15";
                                        } else if (reqCreature.faction === "Infection") {
                                          factionColor = "text-emerald-400 border-emerald-900/50 bg-emerald-950/15";
                                        }

                                        return (
                                          <div className={`p-3 border rounded-lg flex flex-col justify-between space-y-2 font-mono ${factionColor}`}>
                                            {/* Header */}
                                            <div className="flex justify-between items-center border-b border-white/10 pb-1.5 select-none md:flex-row flex-col gap-1 text-center md:text-left">
                                              <div>
                                                <span className="text-[7.5px] font-bold uppercase block tracking-widest text-[#00FF41]">P.O.X. Sequence Offered</span>
                                                <span className="text-white text-xs font-black uppercase tracking-wider">{reqCreature.name}</span>
                                              </div>
                                              <span className="text-[7.5px] bg-black/65 px-1.5 py-0.5 rounded border border-white/15 uppercase font-bold tracking-wider">
                                                {reqCreature.faction}
                                              </span>
                                            </div>

                                            {/* ASCII Art + stats */}
                                            <div className="flex gap-2.5 items-stretch min-h-0 flex-col sm:flex-row">
                                              {reqCreature.asciiArt && (
                                                <div className="text-[6.5px] font-mono leading-[1.05] tracking-widest whitespace-pre bg-black/80 border border-white/10 p-1 rounded flex items-center justify-center overflow-hidden w-20 h-14 shrink-0 text-white select-all">
                                                  {reqCreature.asciiArt}
                                                </div>
                                              )}
                                              <div className="flex-1 grid grid-cols-2 gap-x-2 gap-y-1 text-[8.5px] leading-tight text-white/80 self-center">
                                                <div>HP: <b className="text-white">{reqCreature.vitality}</b></div>
                                                <div>ATK: <b className="text-white">{reqCreature.attack}</b></div>
                                                <div>DEF: <b className="text-white">{reqCreature.defense}</b></div>
                                                <div>SPD: <b className="text-white">{reqCreature.speed}</b></div>
                                                <div className="col-span-2 text-[7.5px] text-white/50 truncate">TYPE: <b className="text-white">{reqCreature.type || "UNKNOWN"}</b></div>
                                              </div>
                                            </div>

                                            {/* DNA Sequence Block */}
                                            <div className="bg-black/60 border border-white/5 p-1.5 rounded text-[8.5px] font-mono">
                                              <span className="text-[7px] text-white/45 block uppercase tracking-wide mb-1 leading-none select-none">Sequence Array (64-Ch)</span>
                                              <div className="break-all tracking-widest select-all leading-normal">
                                                {render8BlockColorCodedSequence(reqCreature.sequence)}
                                              </div>
                                            </div>
                                          </div>
                                        );
                                      })()}

                                      {/* Selected Player miniature creature card */}
                                      {tradeSelectedCreature ? (
                                        (() => {
                                          let factionColor = "text-red-400 border-red-900/55 bg-red-950/15";
                                          if (tradeSelectedCreature.faction === "Mech") {
                                            factionColor = "text-yellow-400 border-yellow-900/50 bg-yellow-950/15";
                                          } else if (tradeSelectedCreature.faction === "Parasite") {
                                            factionColor = "text-purple-400 border-purple-900/50 bg-purple-950/15";
                                          } else if (tradeSelectedCreature.faction === "Containment") {
                                            factionColor = "text-cyan-400 border-cyan-900/55 bg-cyan-950/15";
                                          } else if (tradeSelectedCreature.faction === "Infection") {
                                            factionColor = "text-emerald-400 border-emerald-900/50 bg-emerald-950/15";
                                          }

                                          return (
                                            <div className={`p-3 border rounded-lg flex flex-col justify-between space-y-2 font-mono ${factionColor}`}>
                                              {/* Header */}
                                              <div className="flex justify-between items-center border-b border-white/10 pb-1.5 select-none md:flex-row flex-col gap-1 text-center md:text-left font-mono">
                                                <div>
                                                  <span className="text-[7.5px] font-bold uppercase block tracking-widest text-[#00FF41]">PLAYER OFFER SPECIMEN</span>
                                                  <span className="text-white text-xs font-black uppercase tracking-wider">{tradeSelectedCreature.name}</span>
                                                </div>
                                                <span className="text-[7.5px] bg-black/65 px-1.5 py-0.5 rounded border border-white/15 uppercase font-bold tracking-wider">
                                                  {tradeSelectedCreature.faction}
                                                </span>
                                              </div>

                                              {/* ASCII Art + stats */}
                                              <div className="flex gap-2.5 items-stretch min-h-0 flex-col sm:flex-row font-mono">
                                                {tradeSelectedCreature.asciiArt && (
                                                  <div className="text-[6.5px] font-mono leading-[1.05] tracking-widest whitespace-pre bg-black/80 border border-white/10 p-1 rounded flex items-center justify-center overflow-hidden w-20 h-14 shrink-0 text-white select-all">
                                                    {tradeSelectedCreature.asciiArt}
                                                  </div>
                                                )}
                                                <div className="flex-1 grid grid-cols-2 gap-x-2 gap-y-1 text-[8.5px] leading-tight text-white/80 self-center font-mono">
                                                  <div>HP: <b className="text-white">{tradeSelectedCreature.vitality}</b></div>
                                                  <div>ATK: <b className="text-white">{tradeSelectedCreature.attack}</b></div>
                                                  <div>DEF: <b className="text-white">{tradeSelectedCreature.defense}</b></div>
                                                  <div>SPD: <b className="text-white">{tradeSelectedCreature.speed}</b></div>
                                                  <div className="col-span-2 text-[7.5px] text-white/50 truncate font-mono">TYPE: <b className="text-white">{tradeSelectedCreature.type || "UNKNOWN"}</b></div>
                                                </div>
                                              </div>

                                              {/* DNA Sequence Block */}
                                              <div className="bg-black/60 border border-white/5 p-1.5 rounded text-[8.5px] font-mono">
                                                <span className="text-[7px] text-white/45 block uppercase tracking-wide mb-1 leading-none select-none font-mono">Sequence Array (64-Ch)</span>
                                                <div className="break-all tracking-widest select-all leading-normal font-mono">
                                                  {render8BlockColorCodedSequence(tradeSelectedCreature.sequence)}
                                                </div>
                                              </div>
                                            </div>
                                          );
                                        })()
                                      ) : (
                                        <div className="flex-1 flex flex-col items-center justify-center border-2 border-dashed border-green-950/40 p-5 rounded-lg bg-black/10 select-none font-mono min-h-[140px]">
                                          <span className="text-[9px] text-green-700 font-extrabold uppercase tracking-widest animate-pulse">Awaiting player selection...</span>
                                        </div>
                                      )}
                                    </div>
                                  </div>
                                );
                              })()
                            )}

                            {/* State: Transaction swapped successfully and completed */}
                            {activeTrade.status === 'completed' && (
                              <div className="flex flex-col items-center justify-center text-center space-y-3 py-2">
                                <div className="w-10 h-10 rounded-full bg-green-500/10 border-2 border-[#00FF41] flex items-center justify-center text-[#00FF41] animate-bounce">
                                  <Check className="w-6 h-6" />
                                </div>
                                <span className="text-xs font-bold text-[#00FF41] uppercase block">BARTER AGREEMENT SECURED</span>
                                <div className="text-[9.5px] leading-relaxed text-green-600 font-mono space-y-1 max-w-xs">
                                  <p>Dispatched creature specimen <strong className="text-white font-semibold">"{activeTrade.playerOfferedCreature?.name}"</strong></p>
                                  <p>Harvested peer variant <strong className="text-white font-semibold">"{activeTrade.partnerOfferedCreature?.name}"</strong> from Node host {activeTrade.partner.uid}.</p>
                                </div>
                                <button onClick={handleClearTradeSession} className="px-5 py-1.5 bg-green-950/40 border border-green-500 text-white font-bold text-[9px] hover:bg-green-900 rounded-sm cursor-pointer uppercase font-mono tracking-wider">CLOSE SYNC LOG</button>
                              </div>
                            )}

                          </div>
                        </div>
                      ) : (
                        // Standard Peer list scans
                        <div className="flex flex-col h-full">
                          <div>
                            <h2 className="text-xs font-bold text-white tracking-wider mb-2 mt-1">Sync-Net Peer Node Locator</h2>
                            <p className="text-[10px] text-green-700 font-mono mb-3 leading-tight">Sync-Net allows communication between local nodes; affected by spectrum oscillations.</p>

                            <button 
                              onClick={handleScanTransceiver}
                              disabled={isScanning}
                              className={`w-full py-2.5 rounded font-bold text-xs uppercase tracking-wider text-center border cursor-pointer active:scale-95 transition-all mb-2 font-mono ${
                                isScanning 
                                  ? "bg-neutral-900 border-neutral-800 text-neutral-600 pointer-events-none" 
                                  : "bg-green-900/20 border-green-500/60 text-[#00FF41] hover:bg-[#00FF41] hover:text-black shadow-[0_0_8px_rgba(0,255,102,0.1)]"
                              }`}
                            >
                              {isScanning ? "SWEEPING FREQUENCY MATRIX..." : "SWEEP FOR NEARBY NODES"}
                            </button>
                          </div>

                          {nearbyUsers.length > 0 && (
                            <div className="flex items-center justify-between bg-black/30 border border-green-950/60 px-3 py-1.5 rounded text-[9px] font-mono select-none mt-1">
                              <span className="text-green-600 font-bold">TOTAL PEERS DETECTED: <b className="text-white font-black">{nearbyUsers.length}</b></span>
                              <span className="text-cyan-400 font-bold">G.E.N. MATCHES: <b className="text-white font-black">{nearbyUsers.filter(u => u.targetSequence && genMails.some(m => m.sequences.includes(u.targetSequence!))).length}</b></span>
                            </div>
                          )}

                          <div className="flex-1 min-h-[280px] md:min-h-[320px] max-h-[460px] overflow-y-auto space-y-2 pr-1 scrollbar-thin bg-black/45 border border-green-900/30 p-2.5 rounded mt-2 custom-pox-scrollbar">
                            {(() => {
                              const processedUsers = nearbyUsers.map(u => {
                                const isGenBountyMatch = u.targetSequence && genMails.some(m => m.sequences.includes(u.targetSequence!));
                                return { user: u, isGenBountyMatch };
                              });

                              const sortedUsers = [...processedUsers].sort((a, b) => {
                                if (a.isGenBountyMatch && !b.isGenBountyMatch) return -1;
                                if (!a.isGenBountyMatch && b.isGenBountyMatch) return 1;
                                return a.user.distance - b.user.distance;
                              });

                              return sortedUsers.map(({ user, isGenBountyMatch }, idx) => {
                                let signalStrength = "STRONG";
                                let signalColor = "text-[#00FF41]";
                                if (user.distance > 22) {
                                  signalStrength = "WEAK";
                                  signalColor = "text-yellow-600";
                                } else if (user.distance > 12) {
                                  signalStrength = "GOOD";
                                  signalColor = "text-yellow-400";
                                }

                                const showDivider = idx > 0 && sortedUsers[idx - 1].isGenBountyMatch && !isGenBountyMatch;

                                return (
                                  <div key={idx}>
                                    {showDivider && (
                                      <div className="flex items-center gap-2 my-2 py-1 select-none">
                                        <div className="h-[1px] bg-green-950/75 flex-1" />
                                        <span className="text-[7.5px] font-black text-green-700 tracking-widest uppercase font-mono px-1.5">STANDARD COGNATE PEERS</span>
                                        <div className="h-[1px] bg-green-950/75 flex-1" />
                                      </div>
                                    )}
                                    {idx === 0 && isGenBountyMatch && (
                                      <div className="flex items-center gap-2 mb-2 select-none">
                                        <div className="h-[1px] bg-cyan-950/60 flex-1" />
                                        <span className="text-[7.5px] font-black text-cyan-400 tracking-widest uppercase font-mono px-1.5">HIGH-COHERENCE RECIPIENTS</span>
                                        <div className="h-[1px] bg-cyan-950/60 flex-1" />
                                      </div>
                                    )}
                                    <div 
                                      className={`p-2 border rounded flex justify-between items-center transition-all ${
                                        isGenBountyMatch 
                                          ? 'border-cyan-500 bg-cyan-950/20 shadow-[0_0_8px_rgba(6,182,212,0.15)]' 
                                          : 'border-green-900/20 bg-black/80 hover:border-green-800'
                                      }`}
                                    >
                                      <div className="min-w-0 pr-2">
                                        <div className="flex items-center gap-1.5 flex-wrap">
                                          <span className="font-bold text-white text-[11px] block select-all">{user.uid}</span>
                                          {isGenBountyMatch && (
                                            <span className="text-[7px] bg-cyan-950 text-cyan-400 border border-cyan-500/50 px-1 py-0.5 rounded font-extrabold uppercase animate-pulse select-none">
                                              ★ G.E.N MATCH
                                            </span>
                                          )}
                                        </div>
                                        <div className="flex items-center gap-2 text-[8.5px] text-green-700 font-mono mt-0.5 select-none">
                                          <span>Dist: <b className="text-white font-bold">{user.distance.toFixed(1)}ft</b></span>
                                          <span>&bull;</span>
                                          <span>Link: <span className={`font-mono font-black uppercase ${signalColor}`}>{signalStrength}</span></span>
                                        </div>
                                      </div>
                                      <div className="flex items-center gap-1.5 font-mono select-none">
                                        <button 
                                          onClick={() => handleInitiateTradeRequest(user)}
                                          className="bg-green-900/40 hover:bg-[#00FF41] hover:text-black border border-green-900 hover:border-[#00FF41] text-[#00FF41] font-extrabold text-[9px] px-2.5 py-1.5 rounded transition-all active:scale-95 cursor-pointer flex items-center gap-0.5 uppercase shadow-[inset_0_0_4px_rgba(0,0,0,0.5)]"
                                          title="Initiate peer node transfer connection"
                                        >
                                          <Wifi className="w-2.5 h-2.5" /> SYNC
                                        </button>
                                        <button 
                                          onClick={() => handleInitiateHack(user)}
                                          className="bg-red-950/40 hover:bg-red-500 hover:text-white border border-red-900 hover:border-red-500 text-red-400 font-extrabold text-[9px] px-2.5 py-1.5 rounded transition-all active:scale-95 cursor-pointer flex items-center gap-0.5 uppercase shadow-[inset_0_0_4px_rgba(0,0,0,0.5)]"
                                          title="Initiate malicious breach attack vector"
                                        >
                                          <Zap className="w-2.5 h-2.5" /> HACK
                                        </button>
                                      </div>
                                    </div>
                                  </div>
                                );
                              });
                            })()}
                            {nearbyUsers.length === 0 && !isScanning && (
                              <div className="text-center text-green-800 py-12 text-xs font-mono select-none">
                                No nearby nodes detected. Perform a sweep to check for nearby nodes.
                              </div>
                            )}
                          </div>
                        </div>
                      )}
                    </div>

                    </>)}
                        </div>
                      ) : (
                        <div className="h-auto flex flex-col p-5 bg-neutral-950/45 border border-purple-900/40 rounded min-h-[500px] pb-16 mb-8 relative overflow-visible text-left">
                          {/* Retro styling grid */}
                          <div className="absolute inset-0 pointer-events-none bg-[radial-gradient(rgba(168,85,247,0.012)_1px,transparent_1px)] [background-size:16px_16px]" />
                          
                          {/* REDESIGNED AREA SCANNER PAGE: 2-FRAME SETUP */}
                          <div className="z-10 grid grid-cols-1 md:grid-cols-2 gap-4 flex-1 items-stretch relative min-h-[420px]">
                            
                            {/* Left Frame: Anomalies Nearby (Details sidebar if selected, list if not) */}
                            <div className="bg-black/60 border border-purple-900/40 p-4 rounded flex flex-col justify-between min-h-[420px] md:h-full text-left font-mono relative overflow-hidden shadow-[inset_0_1px_3px_rgba(0,0,0,0.6)]">
                              <div className="absolute inset-0 pointer-events-none bg-[radial-gradient(rgba(168,85,247,0.01)_1px,transparent_1px)] [background-size:12px_12px]" />
                              
                              <div className="z-10 flex flex-col h-full justify-between space-y-4">
                                <div>

                                  {selectedAnomalyId ? (() => {
                                    const anom = bioAnomalies.find(a => a.id === selectedAnomalyId);
                                    if (!anom) return null;
                                    const isHarvestable = anom.distance <= scanRadius;
                                    
                                    let factionColor = "text-red-400 border-red-955 bg-red-950/20";
                                    if (anom.faction === "Mech") {
                                      factionColor = "text-yellow-400 border-yellow-950 bg-yellow-950/20";
                                    } else if (anom.faction === "Parasite") {
                                      factionColor = "text-purple-400 border-purple-955 bg-purple-950/20";
                                    } else if (anom.faction === "Containment") {
                                      factionColor = "text-cyan-400 border-cyan-950 bg-cyan-950/20";
                                    }

                                    return (
                                      <div className="space-y-3.5 antialiased">
                                        <div className="flex items-center justify-between border-b border-purple-900/20 pb-2 gap-1.5">
                                          <h3 className="text-xs font-black text-white tracking-widest uppercase truncate max-w-[100px]">
                                            ANOMALY SPEC
                                          </h3>
                                          <div className="flex items-center gap-1 shrink-0">
                                            <button
                                              type="button"
                                              onClick={() => {
                                                sound.playBeep(420, 0.05, "sine");
                                                if (devForceAnomaly) {
                                                  setIsDevSpeedActive(prev => !prev);
                                                }
                                                addScannerLog(`DEV_SPEED: Overclock speed toggled to ${!isDevSpeedActive ? "60x" : "1x"}`);
                                              }}
                                              className={`text-[7.5px] border font-mono px-1.5 py-0.5 rounded uppercase font-bold cursor-pointer transition-all active:scale-95 ${
                                                isDevSpeedActive 
                                                  ? "bg-amber-955 border-amber-500 text-amber-500 animate-pulse font-extrabold" 
                                                  : "border-neutral-800 text-neutral-500 hover:text-neutral-300"
                                              }`}
                                              title="Enable 60x fast forward multiplier"
                                            >
                                              ⚡ DEV (60x)
                                            </button>
                                            <button
                                              type="button"
                                              onClick={() => {
                                                sound.playBeep(450, 0.05, "sine");
                                                setSelectedAnomalyId(null);
                                                addScannerLog("LOCKED_TARGET: Released lock channels.");
                                              }}
                                              className="text-[8px] border border-purple-900/60 hover:bg-neutral-900 hover:text-white px-1.5 py-0.5 rounded text-purple-400 uppercase shrink-0 cursor-pointer transition-all active:scale-95"
                                            >
                                              BACK
                                            </button>
                                          </div>
                                        </div>

                                        <div className={`border p-2 rounded ${factionColor} font-mono space-y-1.5`}>
                                          <div className="text-[10px] uppercase font-bold text-white flex items-center justify-between">
                                            <span>ANOMALY ID:</span>
                                            <span className="text-[9px] px-1 bg-black/40 rounded border border-current font-bold">{anom.id}</span>
                                          </div>
                                          <div className="text-[8px] opacity-80 grid grid-cols-2 gap-y-1">
                                            <div>SECTOR TYPE:</div>
                                            <div className="text-right font-bold text-white uppercase">{anom.faction}</div>
                                            <div>DIST/DIR:</div>
                                            <div className="text-right text-white font-bold">{anom.distance.toFixed(1)} FT &bull; {anom.direction}</div>
                                          </div>
                                        </div>

                                        <div className="bg-black/60 border border-purple-955 p-2 rounded text-[8.5px] space-y-1 leading-normal">
                                          <div className="text-purple-400 font-bold text-[8px] uppercase tracking-wider select-none border-b border-purple-900/20 pb-1 flex items-center justify-between">
                                            <span><FlickeringPurpleText text="Detected Anomalous Gene" /></span>
                                          </div>
                                          <div className="text-white bg-neutral-950 border border-purple-900/20 py-1 px-1.5 rounded text-center text-[10px] select-all font-mono tracking-wider font-extrabold shadow-[2px_2px_0px_rgba(168,85,247,0.03)] border-dashed">
                                            {anom.gene}
                                          </div>
                                          <div className="text-purple-500 text-[7.5px] leading-tight pt-1">
                                            STOCK COUNT: <strong className="text-white">{(sequences.find(s => s.sequence === anom.gene)?.count) || 0}</strong> SAVED
                                          </div>
                                        </div>

                                        {/* Sync Net Sweep Validation Button */}
                                        <div className="space-y-2 pt-1 border-t border-purple-955/45">
                                          {(() => {
                                            // Check if there is an active/incomplete mission for this specific anomaly
                                            const activeMission = harvestingMissions.find(
                                              (m) => m.anomalyId === anom.id && !m.isReturned
                                            );

                                            if (activeMission) {
                                              // Display active mission status & retrieval controls
                                              const remainingSec = Math.max(0, activeMission.totalDuration - (activeMission.elapsedSeconds || 0));
                                              const progressPercent = Math.min(100, Math.round(((activeMission.elapsedSeconds || 0) / activeMission.totalDuration) * 100));
                                              const remainingStr = `${Math.floor(remainingSec / 60)}m ${(remainingSec % 60).toString().padStart(2, "0")}s`;

                                              return (
                                                <div className="bg-neutral-900 border border-purple-900/60 p-2 text-left rounded font-mono space-y-2">
                                                  <div className="flex items-center justify-between font-bold text-[8.5px] uppercase text-white pb-1 border-b border-purple-950/50">
                                                    <span className="text-amber-400 flex items-center gap-1.5">
                                                      <span className="w-1.5 h-1.5 bg-amber-500 rounded-full animate-ping inline-block" />
                                                      HARVEST IN PROGRESS
                                                    </span>
                                                    <span className="text-neutral-500">{activeMission.creatureName}</span>
                                                  </div>

                                                  <div className="text-[8px] leading-relaxed text-neutral-400 space-y-0.5">
                                                    <div className="flex justify-between">
                                                      <span>EST. DURATION:</span>
                                                      <span className="text-white font-bold">{Math.round(activeMission.totalDuration / 60)} MINS</span>
                                                    </div>
                                                    <div className="flex justify-between">
                                                      <span>DISTANCE:</span>
                                                      <span className="text-white font-bold">{activeMission.dispatchDistance.toFixed(1)} FT</span>
                                                    </div>
                                                    <div className="flex justify-between">
                                                      <span>EST. QUALITY:</span>
                                                      <span className="text-purple-400 font-bold">
                                                        {Math.max(0, Math.round(100 - (activeMission.dispatchDistance / ((anom.heatZoneDiameter || 20) / 2)) * 100))}% ACCURACY
                                                      </span>
                                                    </div>
                                                  </div>

                                                  {/* Progress Bar inside sidebar */}
                                                  <div className="w-full bg-neutral-950 border border-purple-955/60 h-2 rounded overflow-hidden">
                                                    <div 
                                                      className="bg-[#00E1FF] h-full transition-all duration-300" 
                                                      style={{ width: `${progressPercent}%` }}
                                                    />
                                                  </div>

                                                  <div className="bg-black/85 p-1 px-1.5 rounded border border-purple-955/50 text-[7.5px] text-purple-500">
                                                    <b>ACQUIRED BASES ({activeMission.harvestedGenes.length}/4):</b>{" "}
                                                    <span className="text-white font-bold">
                                                      {activeMission.harvestedGenes.length > 0 
                                                        ? activeMission.harvestedGenes.join(", ") 
                                                        : "SCANNING ORBITAL RADIATIVES..."}
                                                    </span>
                                                  </div>

                                                  {activeMission.isCompleted ? (
                                                    <button
                                                      type="button"
                                                      onClick={() => handleRetrieveHarvestedGenes(activeMission.id)}
                                                      className="w-full py-1.5 bg-purple-600 hover:bg-purple-500 text-white font-black text-[9px] uppercase rounded border border-purple-400 cursor-pointer shadow-[0_0_8px_#a855f7] transition-all hover:scale-102 flex items-center justify-center gap-1 animate-pulse"
                                                    >
                                                      <Check className="w-3.5 h-3.5 mr-1" />
                                                      RECALL CYBORG & BANK STOCKPILE
                                                    </button>
                                                  ) : (
                                                    <div className="text-[7px] text-zinc-500 italic text-center leading-tight">
                                                      Retrieval available upon mission completion. Genes are banked on safe return.
                                                    </div>
                                                  )}
                                                </div>
                                              );
                                            }

                                            if (anom.harvested) {
                                              return (
                                                <div className="border border-neutral-800 bg-neutral-950/80 p-2 text-center rounded text-[9px] text-neutral-500 font-bold uppercase select-none">
                                                  ✕ SIGNATURE DEPLOYED &bull; EMPTY
                                                </div>
                                              );
                                            }

                                            if (!isHarvestable) {
                                              return (
                                                <div className="border border-red-955 bg-red-955/15 p-2 rounded text-[8.5px] text-red-400 text-center font-bold">
                                                  <p className="uppercase font-mono text-[9px] text-red-500 mb-0.5">✕ GENE OUT OF RANGE</p>
                                                  <span className="text-[7.5px] leading-normal tracking-wide block">
                                                    ANOMALY DISTANCE ({anom.distance.toFixed(0)} FT) EXCEEDS TERMINAL RADIO RANGE ({scanRadius.toFixed(0)} FT).
                                                  </span>
                                                </div>
                                              );
                                            }

                                            // Create standard dispatch Setup
                                            const availableCyborgs = getAvailableHarvestingCreatures();
                                            const lockDist = customTapCoords ? customTapCoords.distance : 0.0;
                                            const bioRadius = (anom.heatZoneDiameter || 20) / 2;
                                            const angleRatio = lockDist / Math.max(1, bioRadius);
                                            const yieldChance = Math.max(0, Math.round(100 - angleRatio * 100));

                                            return (
                                              <div className="space-y-2 mt-1 z-10 text-left">
                                                <div className="bg-purple-950/10 border border-purple-955/50 p-1.5 rounded text-[8px] font-mono leading-tight space-y-1">
                                                  <div className="text-white font-bold uppercase flex justify-between">
                                                    <span>HARVEST TARGET ZONE LOCK</span>
                                                    <span className="text-purple-400">
                                                      (DIA: {anom.heatZoneDiameter}FT)
                                                    </span>
                                                  </div>
                                                  <div className="text-neutral-400 text-[7.5px]">
                                                    TAP MAP WITHIN HEAT RADIUS CIRCLE TO PLACE BEACON CORRELATIONS, OR DISPATCH TO CENTER CORRELATION FOR MAX ACCURACY.
                                                  </div>
                                                  <div className="flex justify-between text-purple-400 font-bold border-t border-purple-955/40 pt-1">
                                                    <span>DISTANCE PINPOINT:</span>
                                                    <span>{customTapCoords ? `${lockDist.toFixed(1)} FT FROM NODE` : "CENTER NODE (0.0 FT)"}</span>
                                                  </div>
                                                  <div className="flex justify-between text-white">
                                                    <span>STATION ACCURACY:</span>
                                                    <span className="font-bold">{yieldChance}% FLUID SPECIMEN EXTRACT</span>
                                                  </div>
                                                </div>

                                                <div className="text-[8px] font-bold uppercase text-neutral-400 tracking-wider">
                                                  Select Sequence to Harvest Anomaly:
                                                </div>

                                                <div className="flex flex-col items-center justify-center p-3 border border-dashed border-purple-900/30 bg-black/20 rounded gap-2 text-center select-none">
                                                  <button
                                                    type="button"
                                                    onClick={() => {
                                                      sound.playBeep(440, 0.05, "sine");
                                                      setCreatureCardOpenedFrom('Scanner');
                                                      setActiveTab('library');
                                                    }}
                                                    className="w-full py-1.5 bg-purple-950/40 hover:bg-purple-900/60 border border-purple-500/50 hover:border-purple-400 font-bold uppercase cursor-pointer transition-all text-white text-[9px] tracking-wider rounded flex items-center justify-center gap-1.5 mx-auto"
                                                  >
                                                    <Search className="w-3.5 h-3.5 text-purple-400" />
                                                    SELECT SEQUENCE FROM GEN-VAULT
                                                  </button>
                                                  {availableCyborgs.length === 0 && (
                                                    <span className="text-[7.5px] text-purple-500 font-mono italic">
                                                      No offline specimens ready. Construct active defenders.
                                                    </span>
                                                  )}
                                                </div>
                                              </div>
                                            );
                                          })()}
                                        </div>
                                        
                                        {/* Return to anomaly list button similar to re-calibrate button */}
                                        <button
                                          type="button"
                                          onClick={() => {
                                            sound.playBeep(450, 0.05, "sine");
                                            setSelectedAnomalyId(null);
                                            addScannerLog("LOCKED_TARGET: Released lock channels.");
                                          }}
                                          className="w-full py-1.5 bg-black/40 hover:bg-purple-950/20 border border-purple-955/40 hover:border-purple-600/40 rounded text-[8.5px] text-purple-400 tracking-wider uppercase flex items-center justify-center gap-1.5 select-none font-bold cursor-pointer"
                                        >
                                          <List className="w-3.5 h-3.5 text-purple-400" />
                                          VIEW ALL ANOMALIES LIST
                                        </button>
                                      </div>
                                    );
                                  })() : (
                                    <div className="space-y-3 antialiased">
                                      <h3 className="text-xs font-black text-purple-400 tracking-widest uppercase">
                                        <FlickeringPurpleText text="ANOMALIES NEARBY" />
                                      </h3>
                                      
                                      <p className="text-[8.5px] leading-normal text-purple-500 font-sans tracking-wide mb-2">
                                        <strong className="text-white font-mono">{bioAnomalies.length}</strong> anomalies detected nearby. Select an anomaly from the list below to see more info:
                                      </p>

                                      {/* Anomalies interactive list layout */}
                                      <div className="bg-neutral-950 border border-purple-955/60 rounded p-1.5 space-y-1 max-h-[220px] overflow-y-auto custom-pox-scrollbar">
                                        {bioAnomalies.map((anom) => {
                                          let indicatorColor = "bg-red-500 shadow-[0_0_6px_rgba(239,68,68,0.7)]";
                                          if (anom.faction === "Mech") indicatorColor = "bg-yellow-500 shadow-[0_0_6px_rgba(234,179,8,0.7)]";
                                          else if (anom.faction === "Parasite") indicatorColor = "bg-purple-500 shadow-[0_0_6px_rgba(168,85,247,0.7)]";
                                          else if (anom.faction === "Containment") indicatorColor = "bg-cyan-500 shadow-[0_0_6px_rgba(34,211,238,0.7)]";

                                          if (anom.harvested) {
                                            indicatorColor = "bg-neutral-600 shadow-none";
                                          }

                                          return (
                                            <button
                                              key={anom.id}
                                              type="button"
                                              onClick={() => {
                                                sound.playBeep(420 + anom.distance, 0.05, "sine");
                                                setSelectedAnomalyId(anom.id);
                                                addScannerLog(`LOCKED_TARGET: Coupled target ID ${anom.id}`);
                                              }}
                                              className="w-full text-left p-1.5 bg-black/40 hover:bg-purple-950/25 border border-purple-955/20 hover:border-purple-850/40 rounded flex items-center justify-between text-[8px] cursor-pointer transition-all shrink-0 font-mono"
                                            >
                                              <div className="flex items-center gap-1.5">
                                                <div className={`w-1.5 h-1.5 rounded-full ${indicatorColor} shrink-0`} />
                                                <span className={`text-[8.5px] font-bold ${anom.harvested ? 'text-neutral-500 line-through' : 'text-neutral-200'}`}>
                                                  ID: {anom.id}
                                                </span>
                                              </div>
                                              <span className="text-white text-[8px] font-bold">{Math.round(anom.distance)}ft {anom.direction}</span>
                                            </button>
                                          );
                                        })}
                                      </div>
                                      
                                      <button
                                        type="button"
                                        onClick={() => {
                                          sound.playBeep(620, 0.1, "sine");
                                          triggerLog("Triggered Area Scanner frequency sweep lock array...", "info");
                                          addScannerLog("RE-CALIBRATE: Sync-net sweeps executed");
                                          // Slightly nudge anomalies
                                          setBioAnomalies(prev => prev.map(a => {
                                            const nextDist = Math.max(15, Math.min(95, a.distance + (Math.random() * 8) - 4));
                                            const rawAngle = Math.atan2(a.dy, a.dx) + (Math.random() * 0.1) - 0.05;
                                            const dx = nextDist * Math.cos(rawAngle);
                                            const dy = nextDist * Math.sin(rawAngle);
                                            const dLat = dy / 364000;
                                            const dLng = dx / 288000;

                                            return {
                                              ...a,
                                              distance: nextDist,
                                              dx,
                                              dy,
                                              lat: userCoords.lat + dLat,
                                              lng: userCoords.lng + dLng,
                                              signalStrength: Math.floor(98 - nextDist * 0.7),
                                            };
                                          }));
                                        }}
                                        className="w-full py-1.5 bg-black/40 hover:bg-purple-950/20 border border-purple-955/40 hover:border-purple-600/40 rounded text-[8.5px] text-purple-400 tracking-wider uppercase flex items-center justify-center gap-1 select-none font-bold cursor-pointer"
                                      >
                                        <RefreshCw className="w-3.5 h-3.5 text-purple-500 animate-spin" style={{ animationDuration: '6s' }} />
                                        SCAN FOR ANOMALIES
                                      </button>

                                      <button
                                        type="button"
                                        onClick={() => {
                                          sound.playBeep(520, 0.08, "sine");
                                          setIsMapPopupOpen(true);
                                          addScannerLog("TACTICAL_MAP: Maximized coordinates tracking grid overlay feed.");
                                        }}
                                        className="w-full mt-2 py-1.5 bg-purple-955/80 hover:bg-purple-900 border border-purple-500/40 hover:border-purple-400 text-purple-400 hover:text-white rounded text-[8.5px] font-black uppercase tracking-widest transition-all duration-150 active:scale-95 flex items-center justify-center gap-2 cursor-pointer shadow-[0_0_10px_rgba(168,85,247,0.2)] select-none animate-pulse"
                                      >
                                        <MapPin className="w-3.5 h-3.5 animate-bounce text-purple-400" />
                                        VIEW SCANNER MAP RADAR
                                      </button>
                                    </div>
                                  )}
                                </div>
                              </div>
                            </div>

                            {/* Right Frame: Active Harvests + Telemetry receiver feed */}
                            <div className="bg-black/60 border border-purple-900/40 p-4 rounded flex flex-col justify-between min-h-[420px] md:h-full text-left font-mono relative overflow-hidden shadow-[inset_0_1px_3px_rgba(0,0,0,0.6)]">
                              <div className="absolute inset-0 pointer-events-none bg-[radial-gradient(rgba(168,85,247,0.012)_1px,transparent_1px)] [background-size:12px_12px]" />
                              
                              <div className="z-10 flex flex-col h-full justify-between space-y-3.5">
                                {showingActiveHarvestsDetail ? (
                                  <div className="flex-1 flex flex-col min-h-0 h-full justify-between">
                                    <div className="flex-grow flex flex-col min-h-0">
                                      <div className="flex justify-between items-center border-b border-purple-900/40 pb-2 mb-2 select-none">
                                        <h3 className="text-xs font-black text-purple-400 tracking-widest uppercase flex items-center gap-1.5">
                                          <span className="w-1.5 h-1.5 bg-purple-400 rounded-full animate-ping" />
                                          DETAILED MONITORS ({harvestingMissions.filter(m => !m.isReturned).length})
                                        </h3>
                                        <button
                                          type="button"
                                          onClick={() => {
                                            sound.playBeep(450, 0.05, "sine");
                                            setShowingActiveHarvestsDetail(false);
                                          }}
                                          className="px-2 py-0.5 bg-neutral-900 hover:bg-neutral-850 border border-purple-900/60 hover:border-purple-400 text-purple-400 hover:text-white rounded text-[8px] font-black uppercase tracking-wider cursor-pointer transition-all active:scale-95 flex items-center gap-1"
                                        >
                                          BACK
                                        </button>
                                      </div>

                                      <p className="text-[8.5px] leading-normal text-purple-500 font-sans tracking-wide mb-3 select-none">
                                        FULL PROTOCOL TELEMETRY INTERFACE FEED:
                                      </p>

                                      {/* Detailed list - taking up the whole rest of space */}
                                      <div className="flex-1 space-y-2.5 overflow-y-auto custom-pox-scrollbar pr-0.5 min-h-[180px] max-h-[220px]">
                                        {harvestingMissions.filter(m => !m.isReturned).map(m => {
                                          const remainingSec = Math.max(0, m.totalDuration - (m.elapsedSeconds || 0));
                                          const progressPercent = Math.min(100, Math.round(((m.elapsedSeconds || 0) / m.totalDuration) * 100));
                                          const remainingStr = `${Math.floor(remainingSec / 60)}m ${(remainingSec % 60).toString().padStart(2, "0")}s`;

                                          return (
                                            <div key={m.id} className="bg-neutral-900/80 border border-purple-800/40 p-2.5 text-left rounded font-mono space-y-2 relative">
                                              <div className="flex items-center justify-between font-bold text-[9px] uppercase text-white pb-1 border-b border-purple-950">
                                                <span className="text-amber-400 flex items-center gap-1.5">
                                                  <span className="w-1.5 h-1.5 bg-amber-500 rounded-full animate-ping inline-block" />
                                                  HARVEST ACTIVE
                                                </span>
                                                <span className="text-white font-extrabold">{m.creatureName}</span>
                                              </div>

                                              <div className="text-[8.5px] leading-relaxed text-zinc-400 grid grid-cols-2 gap-y-1 gap-x-2">
                                                <div>ANOMALY TARGET ID:</div>
                                                <div className="text-right text-white font-bold">{m.anomalyId}</div>
                                                <div>FLUID DISTANCE:</div>
                                                <div className="text-right text-white font-bold">{m.dispatchDistance.toFixed(1)} FT</div>
                                                <div>TIME REMAINING:</div>
                                                <div className="text-right text-white font-mono font-bold animate-pulse">{remainingStr}</div>
                                              </div>

                                              <div className="w-full bg-neutral-950 border border-purple-955 h-1.5 rounded overflow-hidden">
                                                <div 
                                                  className="bg-[#00E1FF] h-full transition-all duration-300" 
                                                  style={{ width: `${progressPercent}%` }}
                                                />
                                              </div>

                                              <div className="bg-black/95 p-1 px-1.5 rounded border border-purple-955 text-[8px] text-purple-400 flex justify-between items-center">
                                                <span><b>GENE PACKETS ({m.harvestedGenes.length}/4):</b></span>
                                                <span className="text-white font-mono font-bold">
                                                  {m.harvestedGenes.length > 0 
                                                    ? m.harvestedGenes.join(", ") 
                                                    : "INTEGRATION LINKING..."}
                                                </span>
                                              </div>

                                              {m.isCompleted && (
                                                <button
                                                  type="button"
                                                  onClick={() => handleRetrieveHarvestedGenes(m.id)}
                                                  className="w-full py-1.5 bg-purple-600 hover:bg-purple-500 text-white font-black text-[9px] uppercase rounded border border-purple-400 cursor-pointer shadow-[0_0_10px_#a855f7] transition-all flex items-center justify-center gap-1 animate-pulse"
                                                >
                                                  <Check className="w-4 h-4 mr-1" />
                                                  RECALL CYBORG & BANK STOCKPILE
                                                </button>
                                              )}
                                            </div>
                                          );
                                        })}
                                      </div>
                                    </div>

                                    <div className="pt-2 border-t border-purple-955/20 text-center">
                                      <button
                                        type="button"
                                        onClick={() => {
                                          sound.playBeep(450, 0.05, "sine");
                                          setShowingActiveHarvestsDetail(false);
                                        }}
                                        className="px-3.5 py-1.5 bg-neutral-950 hover:bg-neutral-800 border border-neutral-800 hover:border-purple-800 text-neutral-400 hover:text-purple-400 rounded text-[8.5px] uppercase font-bold tracking-widest cursor-pointer transition-all active:scale-95 inline-block"
                                      >
                                        ← RETURN TO SCAN FEED
                                      </button>
                                    </div>
                                  </div>
                                ) : showingTelemetryLogsDetail ? (
                                  <div className="flex-grow flex flex-col min-h-0 h-full">
                                    <div className="flex justify-between items-center border-b border-purple-900/40 pb-2 mb-2 select-none">
                                      <h3 className="text-xs font-black text-purple-400 tracking-widest uppercase flex items-center gap-1.5">
                                        <span className="w-1.5 h-1.5 bg-purple-400 rounded-full animate-ping" />
                                        SCANCOM TELEMETRY FEED
                                      </h3>
                                      <button
                                        type="button"
                                        onClick={() => {
                                          sound.playBeep(450, 0.05, "sine");
                                          setShowingTelemetryLogsDetail(false);
                                        }}
                                        className="px-2 py-0.5 bg-neutral-900 hover:bg-neutral-850 border border-purple-900/60 hover:border-purple-400 text-purple-400 hover:text-white rounded text-[8px] font-black uppercase tracking-wider cursor-pointer transition-all active:scale-95 flex items-center gap-1"
                                      >
                                        BACK
                                      </button>
                                    </div>

                                    <p className="text-[8.5px] leading-normal text-purple-500 font-sans tracking-wide mb-3 select-none">
                                      SECURED BI-DIRECTIONAL TERMINAL LINK FEED:
                                    </p>

                                    <div 
                                      className="flex-1 overflow-y-auto space-y-1 bg-black/90 p-2.5 border border-purple-955 rounded font-mono text-[8.5px] text-purple-400 custom-pox-scrollbar min-h-[180px] max-h-[220px]"
                                    >
                                      {scannerLogs.map((log, idx) => (
                                        <div key={idx} className="font-mono leading-normal select-all">
                                          {log}
                                        </div>
                                      ))}
                                    </div>

                                    <div className="pt-2.5 border-t border-purple-955/20 text-center mt-3">
                                      <button
                                        type="button"
                                        onClick={() => {
                                          sound.playBeep(450, 0.05, "sine");
                                          setShowingTelemetryLogsDetail(false);
                                        }}
                                        className="px-3.5 py-1.5 bg-neutral-950 hover:bg-neutral-800 border border-neutral-800 hover:border-purple-800 text-neutral-400 hover:text-purple-400 rounded text-[8.5px] uppercase font-bold tracking-widest cursor-pointer transition-all active:scale-95 inline-block"
                                      >
                                        ← RETURN TO SCAN FEED
                                      </button>
                                    </div>
                                  </div>
                                ) : (
                                  <>
                                    <div className="flex-1 flex flex-col min-h-0">
                                      <h3 className="text-xs font-black text-purple-400 tracking-widest uppercase mb-1 flex items-center justify-between select-none">
                                        <span><FlickeringPurpleText text="ACTIVE HARVESTS" /></span>
                                        <span className="text-[7.5px] bg-purple-500/10 text-purple-400 border border-purple-900/40 px-1 rounded animate-pulse font-extrabold">
                                          {harvestingMissions.filter(m => !m.isReturned).length} ACTIVE
                                        </span>
                                      </h3>
                                      <p className="text-[8.5px] leading-normal text-purple-500 font-sans tracking-wide mb-3 select-none">
                                        Real-time tracking of active anomaly harvests:
                                      </p>

                                      {/* Active Harvests list - Either shows a clean button shortcut, or empty slot */}
                                      <div className="flex-1 pr-0.5 flex flex-col justify-center">
                                        {(() => {
                                          const activeMissions = harvestingMissions.filter(m => !m.isReturned);
                                          if (activeMissions.length === 0) {
                                            return (
                                              <div className="flex-grow flex flex-col items-center justify-center text-center p-2 border border-dashed border-purple-955/40 bg-black/30 rounded my-1 select-none py-3">
                                                <h4 className="text-[8.5px] text-purple-500 uppercase tracking-widest font-extrabold"><FlickeringPurpleText text="NO ACTIVE HARVESTS" /></h4>
                                                <p className="text-[7.5px] text-purple-500/80 max-w-[180px] mt-0.5 leading-normal">
                                                  Select a sequence and press dispatch.
                                                </p>
                                              </div>
                                            );
                                          }

                                          return (
                                            <div className="flex-grow flex flex-col items-center justify-center text-center p-3 border border-purple-900/30 bg-purple-950/15 rounded my-1 select-none min-h-[100px] space-y-1 font-mono">
                                              <div className="w-6 h-6 rounded-full bg-purple-950/40 border border-purple-500/40 flex items-center justify-center text-purple-400">
                                                <Terminal className="w-3.5 h-3.5 animate-pulse text-purple-400" />
                                              </div>
                                              <h4 className="text-[9px] text-white font-extrabold uppercase tracking-widest">
                                                {activeMissions.length} HARVEST PROCESSES ACTIVE
                                              </h4>
                                              <button
                                                type="button"
                                                onClick={() => {
                                                  sound.playBeep(450, 0.05, "sine");
                                                  setShowingActiveHarvestsDetail(true);
                                                }}
                                                className="px-3 py-1 bg-purple-950 hover:bg-purple-900 border border-purple-500/80 hover:border-purple-400 text-purple-400 hover:text-white rounded text-[8px] font-black uppercase tracking-widest transition-all duration-150 active:scale-95 cursor-pointer shadow-[0_0_8px_rgba(168,85,247,0.2)]"
                                              >
                                                VIEW ACTIVE DETAILS ({activeMissions.length})
                                              </button>
                                            </div>
                                          );
                                        })()}
                                      </div>
                                    </div>

                                    {/* Integrated Radar Status Feed */}
                                    <div className="border-t border-purple-955/45 pt-2.5 flex flex-col space-y-1.5 text-left select-none">
                                      <div className="text-[7.5px] text-purple-500 uppercase font-black tracking-widest flex items-center justify-between mb-0.5">
                                        <span><FlickeringPurpleText text="Anomaly Sweep Active" /></span>
                                        <span className="text-[7px] text-purple-400 font-mono font-bold animate-pulse">Scanner Active</span>
                                      </div>
                                      
                                      <div className="grid grid-cols-2 gap-1.5 bg-neutral-950/65 p-1.5 rounded border border-purple-955/25 text-[8px] font-mono leading-relaxed">
                                        <div className="text-neutral-400">MAXIMUM DIAMETER:</div>
                                        <div className="text-right text-purple-400 font-bold">120FT</div>
                                        <div className="text-neutral-400">MINIMUM DIAMETER:</div>
                                        <div className="text-right text-white font-bold">30FT</div>
                                      </div>

                                      {(() => {
                                        const hours = currentTime.getHours();
                                        const minutes = currentTime.getMinutes();
                                        const seconds = currentTime.getSeconds();
                                        const daySeconds = hours * 3600 + minutes * 60 + seconds;
                                        const dayFraction = daySeconds / 86400;
                                        const waveVal = Math.sin(dayFraction * 2 * Math.PI * 4);
                                        const pct = Math.round((waveVal + 1) * 50);
                                        const filled = Math.max(0, Math.min(10, Math.round(pct / 10)));
                                        const bar = "▰".repeat(filled) + "▱".repeat(10 - filled);
                                        
                                        let waveStatus = "NEUTRAL WAVE";
                                        let waveColor = "text-yellow-500";
                                        if (waveVal > 0.8) {
                                          waveStatus = "MAX EXPANSION PEAK";
                                          waveColor = "text-purple-400 font-extrabold animate-pulse";
                                        } else if (waveVal < -0.8) {
                                          waveStatus = "MIN CONTRACT TROUGH";
                                          waveColor = "text-red-400 font-extrabold";
                                        } else if (waveVal > 0) {
                                          waveStatus = "ASCENDING SEQUENCE";
                                          waveColor = "text-purple-500";
                                        } else if (waveVal < 0) {
                                          waveStatus = "DESCENDING SEQUENCE";
                                          waveColor = "text-amber-500";
                                        }

                                        return (
                                          <div className="bg-black/85 p-1.5 rounded border border-purple-955/40 space-y-1 text-[8px] font-mono text-left leading-normal">
                                            <div className="flex justify-between items-center text-[7.5px]">
                                              <span className="text-zinc-500 font-bold text-[7px]">TROUGH (30F)</span>
                                              <span className={`tracking-widest ${waveColor}`}>{bar} {pct}%</span>
                                              <span className="text-zinc-500 font-bold text-[7px]">PEAK (120F)</span>
                                            </div>
                                            <div className="flex justify-between items-center text-[7.5px] border-t border-purple-955/20 pt-1">
                                              <span className="text-neutral-400">OSCILLATOR STATUS:</span>
                                              <span className={`${waveColor} uppercase font-bold text-[7px]`}>{waveStatus}</span>
                                            </div>
                                            <div className="text-[7.5px] text-purple-400 tracking-widest font-extrabold uppercase animate-pulse border border-purple-500/20 bg-purple-950/20 px-1 py-0.5 rounded text-center leading-none">
                                              FIXED RANGE ACTIVE: 60FT
                                            </div>
                                          </div>
                                        );
                                      })()}
                                    </div>

                                    <div className="border-t border-purple-955/40 pt-2.5 flex flex-col mt-auto select-none">
                                      <div className="text-[7.5px] text-purple-500 uppercase font-black tracking-widest flex items-center justify-between select-none mb-1.5">
                                        <span><FlickeringPurpleText text="Scanner Log Telemetry" /></span>
                                        <span className="text-[7px] text-red-400 animate-pulse uppercase font-mono font-bold">Warning: Telemetry Corruption</span>
                                      </div>
                                      <button
                                        type="button"
                                        onClick={() => {
                                          sound.playBeep(450, 0.05, "sine");
                                          setShowingTelemetryLogsDetail(true);
                                        }}
                                        className="w-full py-1.5 bg-black hover:bg-neutral-950 border border-purple-955/65 hover:border-purple-400 text-purple-500 hover:text-purple-400 rounded text-[8px] font-black uppercase tracking-widest transition-all duration-150 active:scale-[0.98] cursor-pointer flex items-center justify-center gap-1.5 animate-pulse"
                                      >
                                        <Terminal className="w-3.5 h-3.5 text-purple-400" />
                                        OPEN SCANCOM TELEMETRY LOGS ({scannerLogs.length})
                                      </button>
                                    </div>
                                  </>
                                )}
                              </div>
                            </div>

                          </div>

                          {/* DYNAMIC MAP MINIMIZABLE RADAR WINDOW OVERLAY */}
                          <AnimatePresence>
                            {isMapPopupOpen && (
                              <div className="absolute inset-0 bg-[#0c0512]/98 border border-purple-500/60 rounded p-4 flex flex-col z-[3000] overflow-hidden font-mono text-left shadow-[0_0_30px_rgba(168,85,247,0.25)]">
                                {/* Map Header */}
                                <div className="flex justify-between items-center border-b border-purple-900 pb-2 mb-2 select-none">
                                  <span className="text-[10px] text-purple-400 font-bold tracking-widest uppercase flex items-center gap-1.5 font-mono">
                                    <span className="w-1.5 h-1.5 bg-purple-500 rounded-full animate-ping" />
                                    [ LOCAL MAP FEED &bull; TACTICAL OVERLAY RADAR ]
                                  </span>
                                  <button
                                    type="button"
                                    onClick={() => {
                                      sound.playBeep(440, 0.05, "sine");
                                      setIsMapPopupOpen(false);
                                      addScannerLog("TACTICAL_MAP: Minimized coordinates map radar.");
                                    }}
                                    className="px-2.5 py-1 bg-red-955/80 hover:bg-red-905 border border-red-800 text-red-500 hover:text-white rounded text-[8.5px] cursor-pointer font-bold tracking-wider transition-all duration-150 active:scale-95 uppercase flex items-center gap-1"
                                  >
                                    ✕ MINIMIZE MAP OVERLAY
                                  </button>
                                </div>

                                <div className="text-[7.5px] text-purple-500 uppercase tracking-widest mb-2 border-b border-purple-955/50 pb-1 flex justify-between select-none">
                                  <span>SATELLITE BEACON COUPLING TRANSCEIVER: CONNECTED</span>
                                  <span>ACTIVE RANGE RADIUS: {scanRadius.toFixed(1)} FT</span>
                                </div>

                                {/* Map Viewport Area */}
                                <div className="flex-1 w-full bg-black/80 rounded border border-purple-955/60 overflow-hidden relative min-h-[350px] sm:min-h-[400px] flex items-center justify-center shadow-[inset_0_1px_5px_rgba(0,0,0,0.8)]">
                                  <LeafletScannerMap
                                    userCoords={userCoords}
                                    scanRadius={scanRadius}
                                    bioAnomalies={bioAnomalies}
                                    selectedAnomalyId={selectedAnomalyId}
                                    onSelectAnomaly={(id) => {
                                      sound.playBeep(420, 0.05, "sine");
                                      setSelectedAnomalyId(id);
                                      addScannerLog(`MAP LOCK: Targeted node specimen ID ${id}`);
                                    }}
                                    harvestingMissions={harvestingMissions}
                                    onMapTapHeatZone={(anomalyId, lat, lng, dist) => {
                                      handleMapTapHeatZone(anomalyId, lat, lng, dist);
                                      addScannerLog(`MAP TAPPED: Fine extract coordinates locked to ${dist.toFixed(1)}ft from epicenter`);
                                    }}
                                  />
                                  
                                  {/* Overlay tag for Geolocation / Fallback Status */}
                                  <div className="absolute top-2 left-2 z-[400] bg-black/85 border border-purple-955/80 px-2 py-1 rounded text-[7px] font-mono leading-none flex items-center gap-1.5 select-none pointer-events-none">
                                    {geoError ? (
                                      <>
                                        <span className="w-1.5 h-1.5 bg-yellow-500 rounded-full animate-pulse" />
                                        <span className="text-yellow-500 uppercase font-bold">MUTATION FEED: SIMULATOR (IFRAME SECURE COMS)</span>
                                      </>
                                    ) : (
                                      <>
                                        <span className="w-1.5 h-1.5 bg-purple-500 rounded-full animate-ping" />
                                        <span className="text-purple-400 uppercase font-bold">MUTATION FEED: LIVE GPS BEACON RESOLVED</span>
                                      </>
                                    )}
                                  </div>
                                </div>

                                {/* Map footer elements */}
                                <div className="mt-3 pt-2 border-t border-purple-955/45 flex flex-col sm:flex-row items-center justify-between gap-3 text-[8px] text-neutral-500 select-none">
                                  <div className="flex flex-wrap items-center gap-1.5 font-mono">
                                    <span className="text-purple-400 font-bold uppercase">BEACON STATUS:</span>
                                    <span className="text-white bg-purple-950 px-1 border border-purple-800 rounded animate-pulse">
                                      COUPLING CHANNELS SAFE
                                    </span>
                                    <span>P2P RANGE LINK: {scanRadius} FT</span>
                                  </div>
                                  
                                  <div className="text-purple-500 text-right uppercase">
                                    Click any hot zones circles above to adjust extractor pinpoints.
                                  </div>
                                </div>
                              </div>
                            )}
                          </AnimatePresence>
                        </div>
                      )}
                    </div>
                  </motion.div>
                )}

                {activeTab === 'stats' && (
                  <motion.div 
                    key="stats"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    className="flex flex-col gap-2.5 h-full relative overflow-y-auto pr-1"
                  >
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                      {/* Left Panel: Primary Stats Telemetry */}
                    <div className="bg-neutral-900/20 border border-green-900/40 p-3 rounded flex flex-col justify-between overflow-y-auto font-mono text-left">
                      <div>
                        <h2 className="text-sm font-bold text-white tracking-wider mb-1.5">SECTOR SECURITY WATCH</h2>
                        <p className="text-[10px] text-green-700 font-mono mb-2.5 leading-tight">
                          Monitor node stability and emissivity indexes here; access historical node data via the button below.
                        </p>

                        <div className="space-y-2.5">
                          {/* Access Stats Button */}
                          <button
                            type="button"
                            onClick={() => {
                              sound.playBeep(520, 0.08, "sine");
                              setIsStatsDetailsOpen(true);
                            }}
                            className="w-full py-2 bg-green-950/70 hover:bg-green-900/85 border border-green-700 text-[#00FF41] hover:text-white rounded text-[9px] font-extrabold uppercase tracking-widest transition-all duration-150 cursor-pointer text-center flex items-center justify-center gap-2 shadow-[0_0_15px_rgba(0,255,65,0.08)]"
                          >
                            [ HISTORICAL NODE DATA ]
                          </button>

                          <div className="border border-green-950 bg-black/45 p-2.5 rounded-lg space-y-2.5">
                            {/* Meter 1: Node Stability */}
                            <div className="space-y-1">
                              <div className="flex justify-between items-end">
                                <span className="text-[8.5px] text-neutral-400 font-extrabold uppercase tracking-wider">Node Stability Indicator:</span>
                                <span className="text-[#00FF41] text-xs font-black">{nodeStability.toFixed(1)}%</span>
                              </div>
                              <div className="w-full h-2 bg-neutral-950 border border-green-950 rounded-full overflow-hidden p-0.5">
                                <div 
                                  className="h-full rounded-full bg-gradient-to-r from-emerald-600 to-[#00FF41] shadow-[0_0_8px_#00FF41] transition-all duration-300"
                                  style={{ width: `${nodeStability}%` }}
                                />
                              </div>
                              <p className="text-[7.5px] text-neutral-500 leading-normal select-none">
                                Increase stability by defending node
                              </p>
                              {nodeStability >= 100 ? (
                                <div className="border border-green-500/30 bg-green-950/30 p-1.5 rounded text-[7.5px] text-[#00FF41] flex flex-col gap-0.5">
                                  <span className="font-extrabold">[#] NODE SHIELD: ACTIVE:</span>
                                  <span className="leading-snug text-neutral-405">Hostile sweep networks are blocked. No inbound port breaches will target you today.</span>
                                  <span className="leading-snug text-amber-500 italic mt-0.5">[!] Harvest times doubled</span>
                                </div>
                              ) : (
                                <div className="text-[7px] text-neutral-555 italic">
                                  Goal: Reach 100% to disable incoming hacks entirely (harvest duration doubles).
                                </div>
                              )}
                            </div>

                            {/* Meter 2: Node Emissivity */}
                            <div className="space-y-1">
                              <div className="flex justify-between items-end">
                                <span className="text-[8.5px] text-neutral-400 font-extrabold uppercase tracking-wider">Node Emissivity Spike:</span>
                                <span className="text-amber-400 text-xs font-black">{nodeEmissivity.toFixed(1)}%</span>
                              </div>
                              <div className="w-full h-2 bg-neutral-950 border border-green-950 rounded-full overflow-hidden p-0.5">
                                <div 
                                  className="h-full rounded-full bg-gradient-to-r from-amber-600 to-yellow-400 shadow-[0_0_8px_rgba(251,191,36,0.5)] transition-all duration-300"
                                  style={{ width: `${nodeEmissivity}%` }}
                                />
                              </div>
                              <p className="text-[7.5px] text-neutral-500 leading-normal select-none">
                                Increase emissivity by attacking nodes
                              </p>
                              {nodeEmissivity >= 100 ? (
                                <div className="border border-amber-500/30 bg-amber-950/30 p-1.5 rounded text-[7.5px] text-amber-300 flex flex-col gap-0.5">
                                  <span className="font-extrabold">🤖 EMISSIVITY LOCK OVERWRITE COMPLETE:</span>
                                  <span className="leading-snug text-neutral-405">Auto-hackers overwrite detection buffers; they will stay online even in peer-silent segments indefinitely.</span>
                                  <span className="leading-snug text-red-500 italic mt-0.5">⚡ WARNING: Electromagnetic leakage is high. Inbound hack risk is doubled.</span>
                                </div>
                              ) : (
                                <div className="text-[7px] text-neutral-555 italic">
                                  Goal: Reach 100% to keep auto-hackers permanently online (inbound breach chance doubles).
                                </div>
                              )}
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* Right Panel: Dynamic Indexes & Diagnostic Gauge */}
                    <div className="bg-neutral-900/20 border border-green-900/40 p-3 rounded flex flex-col justify-between overflow-y-auto">
                      <div>
                        <h2 className="text-sm font-bold text-white tracking-wider mb-1.5">NODE CRYSTAL TELEMETRY</h2>
                        
                        <div className="flex flex-col items-center justify-center p-2.5 bg-black/45 border border-green-900/20 rounded-lg mt-2 relative overflow-hidden">
                          {/* Centered Canvas */}
                          <NodeCrystalCanvas metrics={nodeCrystalMetrics} inventoryStrings={inventoryStrings} />
                          <div className="text-[7px] text-center font-mono mt-2 text-green-500/80 uppercase tracking-widest leading-none">
                            [ NODE CRYSTAL VIRTUAL CHROMATIC ]
                          </div>

                          {/* Cohesive, compact Depth / Width meters stacked at bottom */}
                          <div className="grid grid-cols-2 gap-2 w-full mt-2 font-mono text-[8.5px]">
                            {/* Node Depth compact progress */}
                            <div className="bg-neutral-950/80 p-1.5 rounded border border-green-950/50">
                              <div className="flex justify-between items-center text-neutral-400">
                                <span>DEPTH:</span>
                                <span className="font-extrabold text-[#00FF41]">{nodeCrystalMetrics.totalNodeDepth.toFixed(3)}</span>
                              </div>
                              <div className="w-full h-1 bg-neutral-900 rounded-full mt-1 overflow-hidden">
                                <div 
                                  className="h-full transition-all duration-300"
                                  style={{ 
                                    width: `${Math.min(100, (nodeCrystalMetrics.totalNodeDepth / 4) * 100)}%`,
                                    backgroundColor: `rgb(${nodeCrystalMetrics.color.r}, ${nodeCrystalMetrics.color.g}, ${nodeCrystalMetrics.color.b})`
                                  }}
                                />
                              </div>
                            </div>

                            {/* Node Width compact progress */}
                            <div className="bg-neutral-950/80 p-1.5 rounded border border-green-950/50">
                              <div className="flex justify-between items-center text-neutral-400">
                                <span>WIDTH:</span>
                                <span className="font-extrabold text-blue-400">{nodeCrystalMetrics.totalNodeWidth.toFixed(3)}</span>
                              </div>
                              <div className="w-full h-1 bg-neutral-900 rounded-full mt-1 overflow-hidden">
                                <div 
                                  className="h-full transition-all duration-300"
                                  style={{ 
                                    width: `${Math.min(100, (nodeCrystalMetrics.totalNodeWidth / 4) * 100)}%`,
                                    backgroundColor: `rgb(${nodeCrystalMetrics.color.r}, ${nodeCrystalMetrics.color.g}, ${nodeCrystalMetrics.color.b})`
                                  }}
                                />
                              </div>
                            </div>
                          </div>

                          {/* Dedicated Action Button below the crystal */}
                          <button
                            type="button"
                            onClick={() => {
                              sound.playBeep(440, 0.05, "sine");
                              setIsCrystalDetailsOpen(true);
                            }}
                            className="w-full mt-2.5 py-1.5 bg-green-950/50 hover:bg-green-900/65 border border-green-800 text-green-400 hover:text-white rounded text-[8px] font-bold uppercase tracking-wider transition-all duration-150 cursor-pointer text-center flex items-center justify-center gap-1.5"
                          >
                            [ VIEW DETAILED NODE CRYSTAL TELEMETRY ]
                          </button>
                        </div>
                      </div>
                    </div>

                    {/* CLOSE GRID OF PANELS */}
                    </div>

                    {/* Cool Visual ASCII Gauge representing Power Coupling Factor (spans full-width below left/right panes) */}
                    {(() => {
                      const now = currentTime || new Date();
                      const hours = now.getHours();
                      const minutes = now.getMinutes();
                      const seconds = now.getSeconds();
                      const daySeconds = hours * 3600 + minutes * 60 + seconds;
                      const dayFraction = daySeconds / 86400;

                      const computedCoupling = getSpectrumWaveCoupling();
                      const baseValue = 80;
                      const sinusoidalFluctuation = 12.375 * Math.sin(dayFraction * 2 * Math.PI * 4);
                      const hackBonus = ((stats.totalHacksWon || 0) + (stats.interceptorHacksWon || 0)) * 0.0125;

                      const filledCount = Math.max(0, Math.min(20, Math.round((computedCoupling / 100) * 20)));
                      const emptyCount = 20 - filledCount;
                      const asciiBar = "█".repeat(filledCount) + "▒".repeat(emptyCount);

                      const points = [];
                      for (let x = 0; x <= 100; x++) {
                        const xFraction = x / 100;
                        const yVal = 14 - 10 * Math.sin(xFraction * 2 * Math.PI * 4);
                        points.push(`${x},${yVal}`);
                      }
                      const pathData = `M ${points.join(" L ")}`;
                      const currentX = dayFraction * 100;
                      const currentY = 14 - 10 * Math.sin(dayFraction * 2 * Math.PI * 4);

                      return (
                        <div className="border border-[#00FF41]/30 p-2.5 bg-black/60 rounded relative font-mono text-left w-full shadow-[0_0_15px_rgba(0,255,65,0.04)]">
                          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 items-center">
                            {/* Left Column: Coupler Bar progress representation */}
                            <div className="space-y-1">
                              <div className="text-center text-[7.5px] text-[#00FF41] font-mono font-bold uppercase py-0.5 tracking-wider">
                                SPECTRUM DIAL COUPLING VALUE
                              </div>
                              <div className="bg-neutral-950 p-1.5 border border-green-950 rounded relative h-[48px] flex items-center justify-center">
                                <div className="text-[10px] tracking-normal font-mono font-bold text-center text-[#00FF41] select-none flex items-center justify-center gap-1.5 w-full">
                                  <span className="text-emerald-600 select-none font-light">{asciiBar}</span>
                                  <span className="text-[#00FF41] font-black">{computedCoupling.toFixed(4)}% COUPLER</span>
                                </div>
                              </div>
                            </div>

                            {/* Right Column: Wave oscillator diagram */}
                            <div className="space-y-1">
                              <div className="text-center text-[7.5px] text-[#00FF41] font-mono font-bold uppercase py-0.5 tracking-wider">
                                SPECTRUM WAVE OSCILLATOR
                              </div>
                              <div className="bg-neutral-950 p-1.5 border border-green-950 rounded relative h-[48px] flex flex-col justify-between">
                                <svg className="w-full h-8 overflow-visible" viewBox="0 0 100 28" preserveAspectRatio="none">
                                  {/* Grid line at base 80% */}
                                  <line x1="0" y1="14" x2="100" y2="14" stroke="#166534" strokeWidth="0.5" strokeDasharray="1,1" strokeLinecap="round" />
                                  {/* Sinusoidal path */}
                                  <path d={pathData} fill="none" stroke="#00FF41" strokeWidth="0.85" className="opacity-75" />
                                  {/* Current point marker */}
                                  <circle cx={currentX} cy={currentY} r="1.5" fill="#ef4444" className="animate-pulse" />
                                </svg>
                                <div className="flex justify-between text-[6.5px] text-[#00FF41]/60 font-mono mt-1">
                                  <span>FLUES: {sinusoidalFluctuation >= 0 ? "+" : ""}{sinusoidalFluctuation.toFixed(3)}%</span>
                                  <span>BONUS: +{hackBonus.toFixed(3)}%</span>
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      );
                    })()}

                    {/* Highly stylized retro popover detailed logs drawer overlay */}
                    {isCrystalDetailsOpen && (
                      <div className="absolute inset-0 bg-[#020502]/98 border-2 border-green-500 rounded-lg p-4 flex flex-col justify-between z-[9999] overflow-hidden font-mono text-left shadow-[0_0_50px_rgba(0,255,40,0.45)] animate-fade-in">
                        {/* Header */}
                        <div className="flex justify-between items-center bg-green-950/60 border border-green-500/30 px-3 py-2 rounded mb-3 flex-shrink-0">
                          <span className="text-[9px] text-[#00FF41] font-bold tracking-widest uppercase flex items-center gap-2">
                            <span className="w-1.5 h-1.5 bg-[#00FF41] rounded-full animate-ping" />
                            [ MATRIX COUPLING SPECTRUMS & RECOVERY ]
                          </span>
                          <button
                            type="button"
                            onClick={() => {
                              sound.playBeep(440, 0.05, "sine");
                              setIsCrystalDetailsOpen(false);
                            }}
                            className="px-2.5 py-1 bg-red-950 hover:bg-red-900 border border-red-800 hover:border-red-650 text-red-500 hover:text-white rounded text-[8px] cursor-pointer font-bold tracking-wider transition-all duration-150 active:scale-95 uppercase"
                          >
                            ✕ CLOSE LOGS
                          </button>
                        </div>

                        {/* Body - Scrollable */}
                        <div className="flex-1 overflow-y-auto space-y-3.5 pr-1 text-[9px] leading-relaxed custom-scrollbar">
                          <div className="p-2.5 bg-black border border-green-950 rounded font-mono">
                            <h3 className="text-emerald-400 font-extrabold mb-1.5 uppercase tracking-wider border-b border-green-950/60 pb-1 flex items-center gap-1">
                              <span>⌬</span> BIOPOLYMER SUB-NODES FREQUENCY SPECTRUM
                            </h3>
                            <p className="text-neutral-500 text-[8px] mb-3 leading-normal">
                              Computed values representing raw biochemical sequencing of synthesized gene elements. Sub-nodes correspond to biological attributes.
                            </p>

                            <div className="grid grid-cols-2 gap-2 mt-2">
                              <div className="bg-neutral-900/40 p-2 rounded border border-green-950/50">
                                <span className="text-neutral-500 block text-[7.5px] uppercase">TOTAL NODE DEPTH:</span>
                                <span className="text-[#00FF41] text-xs font-black">{nodeCrystalMetrics.totalNodeDepth.toFixed(6)}</span>
                                <span className="text-[7px] text-green-700 block mt-0.5 leading-none">V_MAX CAP limit: 131,072</span>
                              </div>
                              <div className="bg-neutral-900/40 p-2 rounded border border-green-950/50">
                                <span className="text-neutral-500 block text-[7.5px] uppercase">TOTAL NODE WIDTH:</span>
                                <span className="text-blue-400 text-xs font-black">{nodeCrystalMetrics.totalNodeWidth.toFixed(6)}</span>
                                <span className="text-[7px] text-blue-700 block mt-0.5 leading-none">Quadrant dispersion index</span>
                              </div>
                            </div>

                            {/* Node details */}
                            <div className="space-y-3 mt-3.5 border-t border-green-950/40 pt-3">
                              {/* G */}
                              <div className="space-y-1">
                                <div className="flex justify-between items-center text-[8.5px]">
                                  <span className="text-red-500 font-bold">&#9670; G (INFECTION / ACCELERATION)</span>
                                  <span className="text-white font-bold">{nodeCrystalMetrics.totalG.toLocaleString()} bases <span className="text-neutral-500 font-normal">[{nodeCrystalMetrics.subnodeDepthG.toFixed(3)}D &bull; {nodeCrystalMetrics.subnodeWidthG.toFixed(3)}W]</span></span>
                                </div>
                                <div className="grid grid-cols-2 gap-2">
                                  <div className="bg-neutral-950/60 p-1 rounded">
                                    <div className="flex justify-between text-[7px] text-neutral-500 mb-0.5">
                                      <span>Depth Scale</span>
                                      <span>{(nodeCrystalMetrics.subnodeDepthG * 100).toFixed(1)}%</span>
                                    </div>
                                    <div className="h-1 bg-neutral-900 rounded-full overflow-hidden">
                                      <div className="h-full bg-red-600" style={{ width: `${nodeCrystalMetrics.subnodeDepthG * 100}%` }} />
                                    </div>
                                  </div>
                                  <div className="bg-neutral-950/60 p-1 rounded">
                                    <div className="flex justify-between text-[7px] text-neutral-500 mb-0.5">
                                      <span>Width Dispersion</span>
                                      <span>{(nodeCrystalMetrics.subnodeWidthG * 100).toFixed(1)}%</span>
                                    </div>
                                    <div className="h-1 bg-neutral-900 rounded-full overflow-hidden">
                                      <div className="h-full bg-red-400" style={{ width: `${nodeCrystalMetrics.subnodeWidthG * 100}%` }} />
                                    </div>
                                  </div>
                                </div>
                              </div>

                              {/* A */}
                              <div className="space-y-1">
                                <div className="flex justify-between items-center text-[8.5px]">
                                  <span className="text-[#00FF41] font-bold">&#9670; A (CONTAINMENT / VITALITY)</span>
                                  <span className="text-white font-bold">{nodeCrystalMetrics.totalA.toLocaleString()} bases <span className="text-neutral-500 font-normal">[{nodeCrystalMetrics.subnodeDepthA.toFixed(3)}D &bull; {nodeCrystalMetrics.subnodeWidthA.toFixed(3)}W]</span></span>
                                </div>
                                <div className="grid grid-cols-2 gap-2">
                                  <div className="bg-neutral-950/60 p-1 rounded">
                                    <div className="flex justify-between text-[7px] text-neutral-500 mb-0.5">
                                      <span>Depth Scale</span>
                                      <span>{(nodeCrystalMetrics.subnodeDepthA * 100).toFixed(1)}%</span>
                                    </div>
                                    <div className="h-1 bg-neutral-900 rounded-full overflow-hidden">
                                      <div className="h-full bg-green-500" style={{ width: `${nodeCrystalMetrics.subnodeDepthA * 100}%` }} />
                                    </div>
                                  </div>
                                  <div className="bg-neutral-950/60 p-1 rounded">
                                    <div className="flex justify-between text-[7px] text-neutral-500 mb-0.5">
                                      <span>Width Dispersion</span>
                                      <span>{(nodeCrystalMetrics.subnodeWidthA * 100).toFixed(1)}%</span>
                                    </div>
                                    <div className="h-1 bg-neutral-900 rounded-full overflow-hidden">
                                      <div className="h-full bg-green-300" style={{ width: `${nodeCrystalMetrics.subnodeWidthA * 100}%` }} />
                                    </div>
                                  </div>
                                </div>
                              </div>

                              {/* T */}
                              <div className="space-y-1">
                                <div className="flex justify-between items-center text-[8.5px]">
                                  <span className="text-blue-400 font-bold">&#9670; T (DEFENSE / STRUCTURE)</span>
                                  <span className="text-white font-bold">{nodeCrystalMetrics.totalT.toLocaleString()} bases <span className="text-neutral-500 font-normal">[{nodeCrystalMetrics.subnodeDepthT.toFixed(3)}D &bull; {nodeCrystalMetrics.subnodeWidthT.toFixed(3)}W]</span></span>
                                </div>
                                <div className="grid grid-cols-2 gap-2">
                                  <div className="bg-neutral-950/60 p-1 rounded">
                                    <div className="flex justify-between text-[7px] text-neutral-500 mb-0.5">
                                      <span>Depth Scale</span>
                                      <span>{(nodeCrystalMetrics.subnodeDepthT * 100).toFixed(1)}%</span>
                                    </div>
                                    <div className="h-1 bg-neutral-900 rounded-full overflow-hidden">
                                      <div className="h-full bg-blue-600" style={{ width: `${nodeCrystalMetrics.subnodeDepthT * 100}%` }} />
                                    </div>
                                  </div>
                                  <div className="bg-neutral-950/60 p-1 rounded">
                                    <div className="flex justify-between text-[7px] text-neutral-500 mb-0.5">
                                      <span>Width Dispersion</span>
                                      <span>{(nodeCrystalMetrics.subnodeWidthT * 100).toFixed(1)}%</span>
                                    </div>
                                    <div className="h-1 bg-neutral-900 rounded-full overflow-hidden">
                                      <div className="h-full bg-blue-400" style={{ width: `${nodeCrystalMetrics.subnodeWidthT * 100}%` }} />
                                    </div>
                                  </div>
                                </div>
                              </div>

                              {/* C */}
                              <div className="space-y-1">
                                <div className="flex justify-between items-center text-[8.5px]">
                                  <span className="text-yellow-400 font-bold">&#9670; C (PARASITE / VELOCITY)</span>
                                  <span className="text-white font-bold">{nodeCrystalMetrics.totalC.toLocaleString()} bases <span className="text-neutral-500 font-normal">[{nodeCrystalMetrics.subnodeDepthC.toFixed(3)}D &bull; {nodeCrystalMetrics.subnodeWidthC.toFixed(3)}W]</span></span>
                                </div>
                                <div className="grid grid-cols-2 gap-2">
                                  <div className="bg-neutral-950/60 p-1 rounded">
                                    <div className="flex justify-between text-[7px] text-neutral-500 mb-0.5">
                                      <span>Depth Scale</span>
                                      <span>{(nodeCrystalMetrics.subnodeDepthC * 100).toFixed(1)}%</span>
                                    </div>
                                    <div className="h-1 bg-neutral-900 rounded-full overflow-hidden">
                                      <div className="h-full bg-yellow-500" style={{ width: `${nodeCrystalMetrics.subnodeDepthC * 100}%` }} />
                                    </div>
                                  </div>
                                  <div className="bg-neutral-950/60 p-1 rounded">
                                    <div className="flex justify-between text-[7px] text-neutral-500 mb-0.5">
                                      <span>Width Dispersion</span>
                                      <span>{(nodeCrystalMetrics.subnodeWidthC * 100).toFixed(1)}%</span>
                                    </div>
                                    <div className="h-1 bg-neutral-900 rounded-full overflow-hidden">
                                      <div className="h-full bg-yellow-300" style={{ width: `${nodeCrystalMetrics.subnodeWidthC * 100}%` }} />
                                    </div>
                                  </div>
                                </div>
                              </div>
                            </div>
                          </div>

                          {/* Stats Info */}
                          <div className="bg-black/80 p-2.5 rounded border border-green-950 space-y-1 text-neutral-500 select-none">
                            <div className="flex justify-between">
                              <span>GRAND TOTAL NUCLEOTIDES DETECTED:</span>
                              <strong className="text-white">{nodeCrystalMetrics.grandTotal.toLocaleString()}</strong>
                            </div>
                            <div className="flex justify-between">
                              <span>TARGET SYNAPSE WIDTH (Q25%):</span>
                              <strong className="text-white">{nodeCrystalMetrics.targetWidth.toFixed(4)}</strong>
                            </div>
                            <div className="flex justify-between">
                              <span>CHROMATIC MATRIX AVERAGE:</span>
                              <strong style={{ color: `rgb(${nodeCrystalMetrics.color.r}, ${nodeCrystalMetrics.color.g}, ${nodeCrystalMetrics.color.b})` }}>
                                RGB({nodeCrystalMetrics.color.r}, {nodeCrystalMetrics.color.g}, {nodeCrystalMetrics.color.b})
                              </strong>
                            </div>
                          </div>
                        </div>

                        {/* Return to telemetry streams */}
                        <button
                          type="button"
                          onClick={() => {
                            sound.playBeep(440, 0.05, "sine");
                            setIsCrystalDetailsOpen(false);
                          }}
                          className="w-full mt-3 py-2 bg-green-950 hover:bg-green-900 border border-green-500 text-[#00FF41] hover:text-white rounded text-[8.5px] font-bold uppercase tracking-wider transition-all duration-150 cursor-pointer text-center"
                        >
                          RETURN TO TELEMETRY STREAM
                        </button>
                      </div>
                    )}

                    {isStatsDetailsOpen && (
                      <div className="absolute inset-0 bg-[#020402]/99 border-2 border-green-500 rounded-lg p-4 flex flex-col justify-between z-[9999] overflow-hidden font-mono text-left shadow-[0_0_50px_rgba(0,240,40,0.45)] animate-fade-in">
                        {/* Header */}
                        <div className="flex justify-between items-center bg-green-950/60 border border-green-500/30 px-3 py-2 rounded mb-3 flex-shrink-0">
                          <span className="text-[9px] text-[#00FF41] font-bold tracking-widest uppercase flex items-center gap-2">
                            <span className="w-1.5 h-1.5 bg-[#00FF41] rounded-full animate-ping" />
                            [ SYNC-NET SECTOR TELEMETRY STATS ]
                          </span>
                          <button
                            type="button"
                            onClick={() => {
                              sound.playBeep(440, 0.05, "sine");
                              setIsStatsDetailsOpen(false);
                            }}
                            className="px-2.5 py-1 bg-red-950 hover:bg-red-900 border border-red-800 hover:border-red-650 text-red-500 hover:text-white rounded text-[8px] cursor-pointer font-bold tracking-wider transition-all duration-150 active:scale-95 uppercase"
                          >
                            ✕ CLOSE LOG
                          </button>
                        </div>

                        {/* Body - Scrollable */}
                        <div className="flex-1 overflow-y-auto space-y-3 pr-1 text-[9px] leading-relaxed custom-scrollbar">
                          <div className="p-2.5 bg-black border border-green-950 rounded font-mono">
                            <h3 className="text-emerald-400 font-extrabold mb-1.5 uppercase tracking-wider border-b border-green-950/60 pb-1 flex items-center gap-1">
                              <span>⌬</span> RECORDED SECTOR METRICS & METADATA
                            </h3>
                            <p className="text-neutral-500 text-[8px] mb-3 leading-normal">
                              Historical record of player network transfers, biological reactor accelerations, hacker campaigns, and gene data acquisitions.
                            </p>

                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 mt-2">
                              <div className="bg-neutral-900/40 p-2 rounded border border-green-950/50 flex justify-between items-center">
                                <span className="text-neutral-500 uppercase text-[7.5px]">TOTAL GENES ACQUIRED:</span>
                                <span className="text-[#00FF41] font-extrabold text-[10px]">{stats.totalGenesAcquired}</span>
                              </div>
                              <div className="bg-neutral-900/40 p-2 rounded border border-green-950/50 flex justify-between items-center">
                                <span className="text-neutral-500 uppercase text-[7.5px]">REACTOR ACCELERATIONS:</span>
                                <span className="text-amber-400 font-bold text-[10px]">{stats.totalManualAccelerations}</span>
                              </div>
                              <div className="bg-neutral-900/40 p-2 rounded border border-green-950/30 flex justify-between items-center">
                                <span className="text-neutral-500 uppercase text-[7.5px]">CREATURES CONSTRUCTED:</span>
                                <span className="text-[#00FF41] font-bold text-[10px]">{stats.totalSpliced}</span>
                              </div>
                              <div className="bg-neutral-900/40 p-2 rounded border border-green-950/30 flex justify-between items-center">
                                <span className="text-neutral-500 uppercase text-[7.5px]">SPLICED GENES FROM SPECIMENS:</span>
                                <span className="text-red-400 font-bold text-[10px]">{stats.totalCreaturesSpliceHarvested}</span>
                              </div>
                              <div className="bg-neutral-900/40 p-2 rounded border border-green-950/30 flex justify-between items-center">
                                <span className="text-neutral-500 uppercase text-[7.5px]">SYNC-NET FREQUENCY SCANS:</span>
                                <span className="text-blue-400 font-bold text-[10px]">{stats.totalSignalBandsScanned}</span>
                              </div>
                              <div className="bg-neutral-900/40 p-2 rounded border border-green-950/30 flex justify-between items-center">
                                <span className="text-neutral-500 uppercase text-[7.5px]">SYNC-NET DISPATCH TRANSFERS:</span>
                                <span className="text-purple-400 font-bold text-[10px]">{stats.totalTradesCompleted}</span>
                              </div>
                            </div>

                            <h4 className="text-emerald-500/80 font-bold text-[8.5px] uppercase mt-4 mb-2 tracking-widest border-b border-green-950/40 pb-1 flex items-center gap-1">
                              <span>🛡️</span> SYSTEM SECURITY & DEFENSE MATRIX
                            </h4>

                            <div className="space-y-1.5">
                              <div className="p-2 bg-neutral-950 rounded border border-green-950/40 flex justify-between items-center text-[8.5px]">
                                <span className="text-neutral-400">ACTIVE PLAYER-INITIATED HACKS:</span>
                                <div className="space-x-2 font-black text-[9px]">
                                  <span className="text-neutral-500">INIT: <strong className="text-white">{stats.totalHacksInitiated || 0}</strong></span>
                                  <span className="text-green-400">WON: <strong className="text-emerald-400">{stats.totalHacksWon || 0}</strong></span>
                                  <span className="text-red-400 font-black">LOST: <strong className="text-red-500">{stats.totalHacksLost || 0}</strong></span>
                                </div>
                              </div>

                              <div className="p-2 bg-neutral-950 rounded border border-green-950/40 flex justify-between items-center text-[8.5px]">
                                <span className="text-neutral-400">SYNC-NET INTERCEPT ACTIONS:</span>
                                <div className="space-x-2 font-black text-[9px]">
                                  <span className="text-neutral-500">INIT: <strong className="text-white">{stats.interceptorHacksInitiated || 0}</strong></span>
                                  <span className="text-cyan-400">WON: <strong className="text-cyan-300">{stats.interceptorHacksWon || 0}</strong></span>
                                  <span className="text-red-400 font-black">LOST: <strong className="text-red-500">{stats.interceptorHacksLost || 0}</strong></span>
                                </div>
                              </div>

                              <div className="p-2 bg-neutral-950 rounded border border-green-950/40 flex justify-between items-center text-[8.5px]">
                                <span className="text-neutral-400">G.E.N. NETWORK AUTOMATED HACK ROSTER:</span>
                                <div className="space-x-2 font-black text-[9px]">
                                  <span className="text-cyan-400 font-black">WINS: <strong className="text-cyan-300">{stats.autoHackerWins || 0}</strong></span>
                                  <span className="text-red-450 font-black">LOSSES: <strong className="text-red-400">{stats.autoHackerLosses || 0}</strong></span>
                                </div>
                              </div>
                            </div>

                            <h4 className="text-cyan-500/80 font-bold text-[8.5px] uppercase mt-4 mb-2 tracking-widest border-b border-green-950/40 pb-1 flex items-center gap-1">
                              <span>📡</span> CENTRAL NETWORK EXCHANGE & EMAILS
                            </h4>

                            <div className="space-y-1.5">
                              <div className="grid grid-cols-2 gap-2 text-[8px]">
                                <div className="bg-neutral-900/30 p-2 rounded border border-green-950/40 flex justify-between items-center">
                                  <span className="text-neutral-400">G.E.N. INBOX EMAILS READ:</span>
                                  <strong className="text-white">{stats.genNetworkMailsRead || 0}</strong>
                                </div>
                                <div className="bg-neutral-900/30 p-2 rounded border border-green-950/40 flex justify-between items-center">
                                  <span className="text-neutral-400">SECURED BOUNTY TARGETS:</span>
                                  <strong className="text-yellow-400 font-bold">
                                    {creatures.filter(c => genMails.some(m => m.sequences.includes(c.sequence))).length} MATCHED
                                  </strong>
                                </div>
                                <div className="bg-neutral-900/30 p-2 rounded border border-green-950/40 flex justify-between items-center">
                                  <span className="text-neutral-400">EMAIL TRANSFERS EXCHANGED:</span>
                                  <strong className="text-purple-400">{stats.genNetworkTradesExchanged || 0}</strong>
                                </div>
                                <div className="bg-neutral-900/30 p-2 rounded border border-green-950/40 flex justify-between items-center">
                                  <span className="text-neutral-400">BONUS MATRIX MATCHS:</span>
                                  <strong className="text-amber-400">{stats.genNetworkBonusTargetMatches || 0}</strong>
                                </div>
                              </div>

                              <div className="p-2 bg-neutral-950 rounded border border-green-950/40 flex justify-between items-center text-[8.5px]">
                                <span className="text-neutral-400">G.E.N. SECURE CONSTRUCTIONS COMPLETED:</span>
                                <strong className="text-emerald-400">{stats.genNetworkConstructionsSucceeded || 0} COMPLETED</strong>
                              </div>
                            </div>
                          </div>
                        </div>

                        {/* Return with Close */}
                        <button
                          type="button"
                          onClick={() => {
                            sound.playBeep(440, 0.05, "sine");
                            setIsStatsDetailsOpen(false);
                          }}
                          className="w-full mt-3 py-2 bg-green-950 hover:bg-green-900 border border-green-500 text-[#00FF41] hover:text-white rounded text-[8.5px] font-bold uppercase tracking-wider transition-all duration-150 cursor-pointer text-center"
                        >
                          RETURN TO LIVE METRICS STREAM
                        </button>
                      </div>
                    )}
                  </motion.div>
                )}

                {activeTab === 'gen_network' && (
                  <motion.div 
                    key="gen_network"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    className="flex flex-col h-full overflow-hidden text-left"
                  >
                    {/* G.E.N. NETWORK SUB-TABS SELECTOR */}
                    <div className="flex border-b border-green-950/45 text-[10.5px] font-mono tracking-wider font-bold mb-4 select-none shrink-0 gap-1">
                      <button
                        type="button"
                        onClick={() => {
                          sound.playBeep(440, 0.05, "sine");
                          setGenNetworkSubTab('mail');
                          setActiveTab('gen_network');
                        }}
                        className={`px-4 py-2 border-t-2 transition-all cursor-pointer flex items-center gap-1.5 uppercase ${
                          activeTab === 'gen_network'
                            ? 'border-green-500 bg-green-950/20 text-white font-extrabold shadow-[0_-2px_6px_rgba(0,255,65,0.05)]'
                            : 'border-transparent text-green-700 hover:text-green-400 hover:bg-green-950/5'
                        }`}
                      >
                        G.E.N. Mail
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          sound.playBeep(440, 0.05, "sine");
                          setGenNetworkSubTab('nodes');
                          setActiveTab('nodes');
                        }}
                        className={`px-4 py-2 border-t-2 transition-all cursor-pointer flex items-center gap-1.5 uppercase ${
                          activeTab === 'nodes'
                            ? 'border-green-500 bg-green-950/20 text-white font-extrabold shadow-[0_-2px_6px_rgba(0,255,65,0.05)]'
                            : 'border-transparent text-green-700 hover:text-green-400 hover:bg-green-950/5'
                        }`}
                      >
                        Static Nodes
                      </button>
                    </div>
                    {/* Left Column: Inbox List */}
                    <div className={`bg-neutral-900/20 border border-green-900/40 p-4 sm:p-5 rounded-lg flex flex-col justify-between overflow-y-auto h-full w-full ${selectedMailId ? 'hidden' : 'flex'}`}>
                      <div>
                        <h2 className="text-sm font-bold text-white tracking-widest uppercase mb-2">G.E.N. Mail</h2>
                        <p className="text-[10px] text-green-700 font-mono mb-4 leading-normal">
                          Daily dispatches from G.E.N. P.O.X., stability and emission logs, and peer node sequence transfer requests.
                        </p>
                        
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                          {genMails.slice(mailPage * 12, (mailPage + 1) * 12).map((mail) => {
                            const isSelected = selectedMailId === mail.id;
                            return (
                              <button
                                key={mail.id}
                                id={`mail-btn-${mail.id}`}
                                onClick={() => {
                                  sound.playBeep(520, 0.08, "sine");
                                  setSelectedMailId(mail.id);
                                  setGenMails(prev => prev.map(m => {
                                    if (m.id === mail.id && !m.isRead) {
                                      setStats(s => ({
                                        ...s,
                                        genNetworkMailsRead: (s.genNetworkMailsRead || 0) + 1
                                      }));
                                      return { ...m, isRead: true };
                                    }
                                    return m;
                                  }));
                                }}
                                className={`w-full p-3 rounded border text-left flex flex-col justify-between transition-all cursor-pointer ${
                                  isSelected
                                    ? 'bg-green-950/40 border-green-500 shadow-[0_0_8px_rgba(0,255,65,0.1)]'
                                    : 'bg-black/40 border-green-900/40 hover:border-green-600/60'
                                }`}
                              >
                                <div className="flex justify-between items-start w-full gap-2 mb-1">
                                  <span className="text-[9.5px] text-[#00FF41] font-mono font-semibold tracking-wider select-none uppercase">
                                    {mail.id}
                                  </span>
                                  <span className="text-[9px] text-neutral-400 font-mono select-none">
                                    {mail.date}
                                  </span>
                                </div>
                                <h4 className="text-xs font-bold text-white leading-snug line-clamp-2">
                                  {mail.tagline}
                                </h4>
                                <div className="flex justify-between items-center mt-2.5">
                                  <span className={`text-[8px] px-1.5 py-0.5 rounded font-mono font-bold uppercase ${
                                    mail.isRead 
                                      ? 'text-green-600 bg-green-950/10 border border-green-950/30' 
                                      : 'text-red-400 bg-red-950/30 border border-red-950/50 animate-pulse'
                                  }`}>
                                    {mail.isRead ? 'DECRYPTED' : 'UNREAD SOURCE'}
                                  </span>
                                  <span className="text-[8px] text-neutral-500 font-mono">
                                    {mail.type === 'trade_request' ? 'Node Transfer' : mail.type === 'dispatch' ? 'Dispatch List' : mail.type === 'defense_log' ? 'Security Shield' : 'System Logs'}
                                  </span>
                                </div>
                              </button>
                            );
                          })}
                        </div>

                        {genMails.length > 12 && (
                          <div className="flex justify-center items-center gap-3 mt-4 border-t border-green-900/10 pt-3">
                            <button
                              onClick={() => {
                                sound.playBeep(450, 0.05, "sine");
                                setMailPage(0);
                              }}
                              className={`px-3 py-1 font-mono text-[9.5px] tracking-widest uppercase border cursor-pointer rounded transition-all ${
                                mailPage === 0
                                  ? "border-green-500 bg-green-500/20 text-white font-bold"
                                  : "border-green-950 bg-black/40 text-green-700 hover:border-green-600 hover:text-[#00FF41]"
                              }`}
                            >
                              [ PAGE 1 (1-12) ]
                            </button>
                            <button
                              onClick={() => {
                                sound.playBeep(450, 0.05, "sine");
                                setMailPage(1);
                              }}
                              className={`px-3 py-1 font-mono text-[9.5px] tracking-widest uppercase border cursor-pointer rounded transition-all ${
                                mailPage === 1
                                  ? "border-green-500 bg-[#00FF41]/10 text-white font-bold"
                                  : "border-green-950 bg-black/40 text-green-700 hover:border-green-600 hover:text-[#00FF41]"
                              }`}
                            >
                              [ PAGE 2 (13-24) ]
                            </button>
                          </div>
                        )}
                      </div>
                      
                      <div className="pt-3 mt-4 border-t border-green-900/20 text-[9px] text-green-700 font-mono text-center uppercase tracking-widest select-none">
                        DECRYPTION ENGINE: ACTIVE 256-BIT QUANTUM COMS PROTECTION
                      </div>
                    </div>

                    {/* Right Column: Mail Content Details */}
                    <div className={`relative bg-neutral-900/10 border border-green-900/40 p-4 sm:p-5 rounded-lg flex flex-col justify-between overflow-y-auto text-left ${!selectedMailId ? 'hidden' : 'w-full h-full'}`}>
                      {selectedMailId && (
                        /* Upper return navigation header bar */
                        <div className="bg-neutral-940/25 border border-green-950/50 p-2.5 rounded-md flex justify-end items-center mb-4 font-mono select-none w-full shadow-[inset_0_0_8px_rgba(0,0,0,0.85)]">
                          <button
                            type="button"
                            onClick={() => {
                              sound.playBeep(440, 0.05, "sine");
                              setSelectedMailId(null);
                              setSelectedDispatchSequence(null);
                              setViewportGeneBalancePopupMailId(null);
                            }}
                            className="px-3.5 py-1.5 bg-red-950 hover:bg-red-900 hover:text-white border border-red-500 text-red-100 font-bold uppercase text-[9px] cursor-pointer tracking-wider flex items-center gap-1 transition-all"
                          >
                            ✕ CLOSE MESSAGE & RETURN
                          </button>
                        </div>
                      )}

                      {selectedMailId ? (() => {
                        const mail = genMails.find(m => m.id === selectedMailId);
                        if (!mail) return null;

                        if (selectedDispatchSequence) {
                          const isSplicingMatch = selectedDispatchSequence === targetSequence;
                          return (
                            <div className="space-y-4 flex flex-col h-full justify-between font-mono text-left">
                              <div className="border-b border-green-900/30 pb-3 flex justify-between items-center">
                                <div>
                                  <div className="flex items-center gap-1.5 text-[9px] text-[#00FF41]">
                                    <span className="w-1.5 h-1.5 bg-yellow-400 rounded-full animate-ping" />
                                    <span>G.E.N. TARGET ASSEMBLY PROTOCOL</span>
                                    {isSplicingMatch && (
                                      <span className="bg-yellow-500/20 text-yellow-400 px-1.5 py-0.5 rounded border border-yellow-500/40 text-[8px] font-bold tracking-wider uppercase animate-pulse">
                                        ⚡ 1.25x PRIME BONUS ELIGIBLE
                                      </span>
                                    )}
                                  </div>
                                  <h3 className="text-sm font-bold text-white tracking-widest uppercase">
                                    TARGET: {selectedDispatchSequence.slice(0, 16)}...
                                  </h3>
                                </div>
                                <button
                                  type="button"
                                  onClick={() => {
                                    sound.playBeep(350, 0.05, "sine");
                                    setSelectedDispatchSequence(null);
                                    setNetSplicerSlots([null, null, null, null, null, null, null, null]);
                                    setActiveNetSlotSelection(null);
                                  }}
                                  className="px-2.5 py-1 text-[9px] font-bold border border-red-500/40 text-red-400 hover:bg-red-950/20 rounded cursor-pointer transition-all"
                                >
                                  ◀ CLOSE SPLICER
                                </button>
                              </div>

                              {isNetSplicing ? (
                                <div className="flex-1 flex flex-col items-center justify-center p-6 text-center select-none">
                                  <motion.div 
                                    animate={{ rotate: -360 }}
                                    transition={{ repeat: Infinity, duration: 2.2, ease: "linear" }}
                                    className="text-emerald-400 mb-4"
                                  >
                                    <Dna className="w-14 h-14" />
                                  </motion.div>
                                  <h3 className="text-sm font-bold text-emerald-400 tracking-widest uppercase animate-pulse font-mono">G.E.N. SYNCHRONIZED RE-SEQUENCING ENGAGED</h3>
                                  <p className="text-[10px] text-green-600 max-w-xs mt-2 leading-relaxed font-mono">Processing structural node alignments for target sequence...</p>
                                  <div className="w-48 bg-neutral-900 border border-green-950 rounded-full h-3 mt-4 p-0.5 overflow-hidden">
                                    <div className="bg-emerald-400 h-full rounded-sm shadow-[0_0_8px_#34d399]" style={{ width: `${netSplicingProgress}%` }} />
                                  </div>
                                  <span className="text-[9px] text-green-500 mt-2 uppercase tracking-widest font-bold font-mono">PROGRESS: {netSplicingProgress}%</span>
                                </div>
                              ) : (
                                <div className="grid grid-cols-1 md:grid-cols-5 gap-4 flex-1 my-3">
                                  {/* Left sub-column: Target grid slots [md:col-span-3] */}
                                  <div className="md:col-span-3 flex flex-col justify-between space-y-3">
                                    <div className="space-y-2">
                                      <div className="text-[9.5px] text-green-500 uppercase tracking-wider font-semibold select-none">
                                        🧬 8-NODAL ALIGNMENT CHASSIS
                                      </div>
                                      <div className="grid grid-cols-4 gap-1.5 bg-black p-2 rounded border border-green-900/40">
                                        {netSplicerSlots.map((slot, idx) => {
                                          const isActive = activeNetSlotSelection === idx;
                                          return (
                                            <div
                                              key={idx}
                                              onClick={() => handleNetSlotClick(idx)}
                                              className={`relative h-11 rounded flex flex-col items-center justify-center border transition-all cursor-pointer ${
                                                isActive
                                                  ? 'border-emerald-400 bg-emerald-950/20 shadow-[0_0_8px_rgba(52,211,153,0.15)]'
                                                  : slot
                                                  ? 'border-emerald-800 bg-neutral-900/60'
                                                  : 'border-green-900/20 border-dashed hover:border-green-500 hover:bg-neutral-900/40'
                                              }`}
                                            >
                                              <span className="text-[7.5px] text-green-750 absolute top-0.5 left-1 select-none font-bold">#{idx + 1}</span>
                                              {slot ? (
                                                <div className="text-center">
                                                  <span className="text-[9px] font-bold text-emerald-400 tracking-widest block leading-none">{slot.slice(0,4)}</span>
                                                  <span className="text-[8px] font-semibold text-green-500 tracking-wider block leading-none mt-0.5">{slot.slice(4)}</span>
                                                  <button
                                                    onClick={(e) => handleEjectNetSlot(idx, e)}
                                                    className="absolute -top-1 -right-1 bg-red-950 text-red-500 hover:bg-red-900 border border-red-900/40 rounded-full w-3.5 h-3.5 flex items-center justify-center text-[7px]"
                                                    title="Eject gene block"
                                                  >
                                                    ×
                                                  </button>
                                                </div>
                                              ) : (
                                                <span className="text-green-950 text-[10px] font-bold tracking-widest">----</span>
                                              )}
                                            </div>
                                          );
                                        })}
                                      </div>
                                    </div>

                                    {/* Goal sequence display */}
                                    <div className="p-2 bg-neutral-950/90 border border-emerald-950 rounded select-all">
                                      <span className="text-[8.5px] text-emerald-400 font-bold block mb-0.5 uppercase tracking-widest">[ Required Blueprint sequences ]</span>
                                      <span className="break-all text-[8px] text-emerald-400 leading-normal font-bold tracking-widest font-mono">
                                        {selectedDispatchSequence}
                                      </span>
                                    </div>

                                    {/* Built specimen so far */}
                                    <div className="p-2 bg-black/60 border border-dashed border-green-900/30 rounded">
                                      <span className="text-[8.5px] text-neutral-500 block mb-0.5 uppercase tracking-widest">[ Current aligned matrix ]</span>
                                      <span className="break-all text-[8px] text-neutral-400 leading-normal font-bold tracking-widest font-mono">
                                        {netSplicerSlots.map(s => s || "--------").join("")}
                                      </span>
                                    </div>

                                    <div className="flex gap-2">
                                      <button
                                        type="button"
                                        onClick={handleAutofillNetSplicer}
                                        className="px-3 py-1.5 border border-emerald-500/40 hover:bg-emerald-900/20 text-white font-bold text-[9.5px] rounded transition-all active:scale-[0.98] cursor-pointer"
                                      >
                                        AUTO FILL
                                      </button>
                                      
                                      <button
                                        type="button"
                                        disabled={netSplicerSlots.includes(null)}
                                        onClick={handleConstructNetCreature}
                                        className={`flex-1 py-1.5 text-[9.5px] rounded font-bold uppercase tracking-widest transition-all border ${
                                          netSplicerSlots.includes(null)
                                            ? 'bg-neutral-950 text-green-900 border-green-950 cursor-not-allowed'
                                            : 'bg-emerald-500 hover:bg-emerald-400 text-black border-emerald-400 shadow-[0_0_10px_rgba(52,211,153,0.25)] cursor-pointer'
                                        }`}
                                      >
                                        {netSplicerSlots.includes(null) ? "NODAL LOCK REQUIRED" : "SYNTHESIZE SPECIES"}
                                      </button>
                                    </div>
                                  </div>

                                  {/* Right sub-column: available matching gene block picker [md:col-span-2] */}
                                  <div className="md:col-span-2 bg-neutral-950/40 border border-green-950 rounded p-2.5 flex flex-col justify-between">
                                    {activeNetSlotSelection !== null ? (
                                      <div className="flex flex-col justify-between h-full space-y-2">
                                        <div className="space-y-1">
                                          <div className="flex justify-between items-center text-[8px] text-emerald-400">
                                            <span className="font-bold">SLOT #{activeNetSlotSelection + 1} INTEL:</span>
                                            <button 
                                              type="button"
                                              onClick={() => setActiveNetSlotSelection(null)} 
                                              className="text-red-500 hover:text-white font-bold cursor-pointer"
                                            >
                                              [ CANCEL ]
                                            </button>
                                          </div>
                                          <div className="text-[9px] text-neutral-400 font-mono">
                                            Select match segment for slot #{activeNetSlotSelection + 1}:
                                          </div>
                                          
                                          {/* Segment expected display */}
                                          <div className="bg-emerald-950/25 border border-emerald-500/20 p-1.5 rounded">
                                            <span className="text-[7.5px] text-emerald-500 font-bold block uppercase">[ REQUIRED ]</span>
                                            <span className="font-mono text-emerald-400 font-semibold text-[11px] tracking-wider select-all">
                                              {selectedDispatchSequence.substring(activeNetSlotSelection * 8, (activeNetSlotSelection + 1) * 8)}
                                            </span>
                                          </div>
                                        </div>

                                        <div className="flex-1 max-h-[140px] overflow-y-auto divide-y divide-green-950/40 border border-green-900/20 rounded px-1.5 mt-2 bg-black/50 custom-pox-scrollbar">
                                          {sequences.filter(s => s.count > 0).map((item, idx) => {
                                            const expected = selectedDispatchSequence.substring(activeNetSlotSelection * 8, (activeNetSlotSelection + 1) * 8);
                                            const isMatching = item.sequence === expected;
                                            
                                            return (
                                              <div
                                                key={idx}
                                                onClick={() => isMatching && handleSelectSequenceForNetSlot(item.sequence)}
                                                className={`py-1.5 flex justify-between items-center transition-all ${
                                                  isMatching
                                                    ? 'cursor-pointer hover:bg-emerald-950/20 text-[#00FF41] font-bold'
                                                    : 'opacity-25 cursor-not-allowed select-none text-neutral-600'
                                                }`}
                                              >
                                                <span className="font-mono text-[9px] tracking-widest">{item.sequence}</span>
                                                <div className="flex items-center gap-1">
                                                  <span className="text-[8px]">x{item.count}</span>
                                                  {isMatching && <span className="bg-emerald-900 border border-emerald-500/30 text-white font-mono text-[7px] px-0.5 rounded font-bold">MUTATE</span>}
                                                </div>
                                              </div>
                                            );
                                          })}
                                          {sequences.filter(s => s.count > 0).length === 0 && (
                                            <div className="text-center text-green-950 text-[9px] py-6 select-none font-mono">No stock genes available.</div>
                                          )}
                                        </div>
                                      </div>
                                    ) : (
                                      <div className="flex flex-col justify-between h-full text-green-700 font-mono text-[9px] space-y-2 leading-normal">
                                        <div>
                                          <div className="text-[9.5px] text-emerald-500 font-bold uppercase mb-1">
                                            [ ASSEMBLY DIRECTIVES ]
                                          </div>
                                          <p className="leading-relaxed">
                                            These broadcast sequences represent custom research targets. Synthesizing these target bio-creatures successfully integrates them into local inventory directories.
                                          </p>
                                        </div>
                                        <div className="border border-green-950 bg-black/35 p-2 rounded text-[8px] leading-relaxed">
                                          <p className="text-emerald-400 font-bold mb-0.5">DYNAMIC MATCH DETECTED:</p>
                                          If the target matches your dynamic console goal sequence, the system gains a <strong>1.25x stats multiplier bonus</strong> and resets standard console goals automatically!
                                        </div>
                                      </div>
                                    )}
                                  </div>
                                </div>
                              )}
                              
                              <p className="text-[8px] text-emerald-600/40 leading-snug select-none border-t border-green-950/30 pt-1 uppercase">
                                G.E.N. NETWORK COMS SECURITY PROTOCOL SECURE DIRECT SPLICING PORT 3000 INTEGRITY GUARANTEED.
                              </p>
                            </div>
                          );
                        }

                        if (mail.type === 'defense_log' && mail.autoHackLog) {
                          const defenderCreature = mail.autoHackLog.creature;
                          return (
                            <div className="space-y-4 flex flex-col h-full justify-between font-mono">
                              <div className="space-y-3 font-mono">
                                <div className="border-b border-green-900/30 pb-3 font-mono">
                                  <div className="flex justify-between items-center mb-1 text-[9.5px] font-mono">
                                    <span className="text-blue-500 font-bold">NODE SECURITY: SYSTEM DEFENSE MONITOR</span>
                                    <span className="text-[#00FF41] font-bold font-mono">NODE SHIELD: ACTIVE</span>
                                  </div>
                                  <h3 className="text-sm font-bold text-white tracking-widest uppercase font-mono">
                                    {mail.tagline}
                                  </h3>
                                </div>

                                <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
                                  {/* Left: Deployed Shield Card */}
                                  <div className="md:col-span-2 bg-black/60 border border-green-950 p-3.5 rounded font-mono space-y-3 flex flex-col justify-between">
                                    <div>
                                      <div className="flex justify-between items-start">
                                        <span className="text-[10px] text-blue-400 font-bold uppercase select-none font-mono font-bold">NODE DEFENDER</span>
                                        <span className="text-[8px] bg-blue-950/40 text-blue-400 px-1.5 py-0.5 rounded border border-blue-500/35 font-bold font-mono">
                                          {defenderCreature.faction}
                                        </span>
                                      </div>
                                      <div className="text-white font-black text-sm tracking-widest mt-1.5 uppercase select-all font-mono">
                                        "{defenderCreature.name}"
                                      </div>
                                      <div className="text-[8.5px] text-neutral-500 select-all font-mono break-all mt-1 bg-black/40 p-1.5 rounded border border-neutral-900 leading-tight">
                                        {render8BlockColorCodedSequence(defenderCreature.sequence)}
                                      </div>
                                    </div>

                                    <div className="space-y-1 text-[9.5px] leading-relaxed text-neutral-300 font-mono">
                                      <div className="flex justify-between border-b border-neutral-900/40 pb-0.5">
                                        <span className="text-neutral-500 font-mono">VIT/HP:</span>
                                        <span className="text-[#00FF41] font-bold font-mono">{defenderCreature.vitality}</span>
                                      </div>
                                      <div className="flex justify-between border-b border-neutral-900/40 pb-0.5">
                                        <span className="text-neutral-500 font-mono">ATTACK:</span>
                                        <span className="text-red-400 font-bold font-mono">{defenderCreature.attack}</span>
                                      </div>
                                      <div className="flex justify-between border-b border-neutral-900/40 pb-0.5">
                                        <span className="text-neutral-500 font-mono">DEFENSE:</span>
                                        <span className="text-blue-400 font-bold font-mono">{defenderCreature.defense}</span>
                                      </div>
                                      <div className="flex justify-between border-b border-neutral-900/40 pb-0.5">
                                        <span className="text-neutral-500 font-mono">WEAPON:</span>
                                        <span className="text-neutral-200 font-mono">{defenderCreature.primaryWeapon}</span>
                                      </div>
                                      <div className="flex justify-between">
                                        <span className="text-neutral-500 font-mono">STATUS:</span>
                                        <span className="text-blue-400 font-bold animate-pulse font-mono">
                                          DEFENDING ACTIVE
                                        </span>
                                      </div>
                                    </div>
                                  </div>

                                  {/* Right: Defender Battle Logs Terminal */}
                                  <div className="md:col-span-3 bg-neutral-950/80 border border-green-950 p-3.5 rounded flex flex-col justify-between h-[310px] font-mono relative overflow-hidden">
                                    <div className="text-[8.5px] text-blue-500 uppercase tracking-wider font-bold mb-1.5 flex justify-between items-center font-mono">
                                      <span>NODE DEFENDER LOG:</span>
                                      <button 
                                        onClick={() => { sound.playBeep(650, 0.05, "sine"); setViewportGeneBalancePopupMailId(mail.id); }}
                                        className="text-[8px] bg-[#00FF41]/15 text-[#00FF41] hover:bg-[#00FF41]/25 border border-[#00FF41]/35 px-2 py-0.5 rounded font-mono font-bold tracking-tight cursor-pointer shadow-[0_0_8px_rgba(0,255,65,0.15)] animate-pulse"
                                        title="View list of genes gained/lost for entirety of this battle log"
                                      >
                                        [ TRANSFERS AUDIT ]
                                      </button>
                                    </div>
                                    <div className="flex-1 overflow-y-auto font-mono text-[9px] leading-relaxed text-neutral-300 p-2 border border-neutral-900 bg-black/40 space-y-1 scrollbar-thin scrollbar-thumb-blue-950 scrollbar-track-transparent custom-pox-scrollbar max-h-[260px]">
                                      {mail.autoHackLog.log.map((line, lIdx) => {
                                        const isHeal = line.includes("SPECIAL") && (line.includes("Cellular") || line.includes("Restored"));
                                        const isEvasion = line.includes("SPECIAL") && line.includes("firewall");
                                        const isSuccess = line.includes("DEFLECTED");
                                        const isDefeat = line.includes("COMPROMISED") || line.includes("defeated");
                                        const isSessionDivider = line.includes("TRANSMISSION") || line.includes("LOG AT") || line.includes("LOG ON") || line.includes("REPORT AT") || line.includes("DEFENSE LOG AT") || line.includes("INCOMING NODE STABILITY BREACH") || line.includes("NODE STABILITY REPORT");

                                        return (
                                          <div 
                                            key={lIdx} 
                                            className={`${
                                              isHeal ? "text-emerald-400" :
                                              isEvasion ? "text-cyan-400" :
                                              isSuccess ? "text-[#00FF41] font-bold" :
                                              isDefeat ? "text-red-400 font-bold" :
                                              isSessionDivider ? "text-blue-400 font-extrabold border-b border-blue-950 pb-1 mt-3" :
                                              "text-neutral-400"
                                            }`}
                                          >
                                            {line}
                                          </div>
                                        );
                                      })}
                                    </div>

                                    {/* TRANSFERS AUDIT OVERLAY INLINE */}
                                    <AnimatePresence>
                                      {viewportGeneBalancePopupMailId === mail.id && (() => {
                                        const gained = mail.autoHackLog.genesGained || [];
                                        const lost = mail.autoHackLog.genesLost || [];
                                        return (
                                          <motion.div
                                            key="transfers-audit-popup"
                                            initial={{ opacity: 0, scale: 0.98 }}
                                            animate={{ opacity: 1, scale: 1 }}
                                            exit={{ opacity: 0, scale: 0.98 }}
                                            className="absolute inset-0 bg-[#0d0202]/98 border border-red-500/60 p-3.5 flex flex-col z-40 overflow-hidden font-mono text-left"
                                          >
                                            <div className="flex justify-between items-center border-b border-red-955 pb-1.5 mb-2 select-none">
                                              <span className="text-[10px] text-red-400 font-bold tracking-widest uppercase flex items-center gap-1.5 font-mono">
                                                🚨 DEFENSE GENE AUDIT
                                              </span>
                                              <button
                                                type="button"
                                                onClick={() => {
                                                  sound.playBeep(440, 0.05, "sine");
                                                  setViewportGeneBalancePopupMailId(null);
                                                }}
                                                className="px-2 py-0.5 rounded text-[8.5px] cursor-pointer font-bold tracking-wider border bg-red-950/80 hover:bg-red-900 border-red-800 text-red-500 hover:text-white"
                                              >
                                                ✕ CLOSE AUDIT
                                              </button>
                                            </div>

                                            <p className="text-[8.5px] text-neutral-405 mb-2 leading-snug font-mono select-none text-neutral-400">
                                              Surrendered defense transmission records.
                                            </p>

                                            <div className="flex-1 overflow-y-auto space-y-2 pr-0.5 custom-pox-scrollbar font-mono">
                                              <div>
                                                <div className="text-[9px] text-red-400 font-bold tracking-wider uppercase mb-1.5 font-mono">
                                                  SURRENDERED GENES ({lost.length})
                                                </div>
                                                <div className="grid grid-cols-2 gap-1.5">
                                                  {lost.map((seq, sIdx) => {
                                                    const isAnom = isAnomalousGene(seq);
                                                    return (
                                                      <div 
                                                        key={sIdx}
                                                        className={`p-1.5 rounded border flex flex-col font-mono text-[9px] select-all ${
                                                          isAnom 
                                                            ? "border-purple-500 bg-purple-950/20 text-purple-300" 
                                                            : "border-red-900/40 bg-red-950/5 text-red-400"
                                                        }`}
                                                      >
                                                        <span className="font-extrabold tracking-widest">{seq}</span>
                                                      </div>
                                                    );
                                                  })}
                                                  {lost.length === 0 && (
                                                    <p className="col-span-2 text-center text-[9px] text-neutral-500 py-4 italic font-mono">No compromised sequences.</p>
                                                  )}
                                                </div>
                                              </div>
                                            </div>
                                          </motion.div>
                                        );
                                      })()}
                                    </AnimatePresence>
                                  </div>
                                </div>
                              </div>
                            </div>
                          );
                        }

                        if (mail.type === 'auto_hack_log' && mail.autoHackLog) {
                          const hackerCreature = mail.autoHackLog.creature;
                          return (
                            <div className="space-y-4 flex flex-col h-full justify-between">
                              <div className="space-y-3">
                                <div className="border-b border-green-900/30 pb-3 font-mono">
                                  <div className="flex justify-between items-center mb-1 text-[9.5px]">
                                    <span className="text-amber-500 font-bold">🤖 SOURCE: CENTRAL G.E.N. COMS INTEGRITY MONITOR</span>
                                    <span className="text-red-500 font-bold">DETECTION ENCRYPT: ACTIVE</span>
                                  </div>
                                  <h3 className="text-sm font-bold text-white tracking-widest uppercase font-mono">
                                    {mail.tagline}
                                  </h3>
                                </div>

                                <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
                                  {/* Left: Creature Card Panel */}
                                  <div className="md:col-span-2 bg-black/60 border border-green-950 p-3.5 rounded font-mono space-y-3 flex flex-col justify-between">
                                    <div>
                                      <div className="flex justify-between items-start">
                                        <span className="text-[10px] text-green-400 font-bold uppercase select-none">DETECTED CYBORG</span>
                                        <span className="text-[8px] bg-[#00FF41]/20 text-[#00FF41] px-1.5 py-0.5 rounded border border-[#00FF41]/35 font-bold">
                                          {hackerCreature.faction}
                                        </span>
                                      </div>
                                      <div className="text-white font-black text-sm tracking-widest mt-1.5 uppercase select-all">
                                        "{hackerCreature.name}"
                                      </div>
                                      <div className="text-[8.5px] text-neutral-500 select-all font-mono break-all mt-1 bg-black/40 p-1.5 rounded border border-neutral-900 leading-tight">
                                        {render8BlockColorCodedSequence(hackerCreature.sequence)}
                                      </div>
                                    </div>

                                    <div className="space-y-1 text-[9.5px] leading-relaxed text-neutral-300">
                                      <div className="flex justify-between border-b border-neutral-900/40 pb-0.5">
                                        <span className="text-neutral-500">VIT/HP:</span>
                                        <span className="text-[#00FF41] font-bold">{hackerCreature.vitality}</span>
                                      </div>
                                      <div className="flex justify-between border-b border-neutral-900/40 pb-0.5">
                                        <span className="text-neutral-500">ATTACK:</span>
                                        <span className="text-red-400 font-bold">{hackerCreature.attack}</span>
                                      </div>
                                      <div className="flex justify-between border-b border-neutral-900/40 pb-0.5">
                                        <span className="text-neutral-500">DEFENSE:</span>
                                        <span className="text-blue-400 font-bold">{hackerCreature.defense}</span>
                                      </div>
                                      <div className="flex justify-between border-b border-neutral-900/40 pb-0.5">
                                        <span className="text-neutral-500">WEAPON:</span>
                                        <span className="text-neutral-200">{hackerCreature.primaryWeapon}</span>
                                      </div>
                                      <div className="flex justify-between">
                                        <span className="text-neutral-500">STATUS:</span>
                                        <span className={mail.autoHackLog.isDefeated ? "text-red-500 font-extrabold animate-pulse" : "text-green-400 font-bold"}>
                                          {mail.autoHackLog.isDefeated ? "DEFEATED IN ACTIONS" : "MONITORED ACTIVE"}
                                        </span>
                                      </div>
                                    </div>

                                    {hackerCreature.sequence.length > 64 && (
                                      <div className="p-2 border border-green-950 bg-green-950/10 rounded">
                                        <div className="text-[8px] text-[#00FF41] font-bold tracking-widest uppercase">
                                          🧬 MUTANT UPGRADES LOADED:
                                        </div>
                                        <div className="space-y-1 mt-1">
                                          {getUnlockedMoves(hackerCreature.sequence).map((mv, idx) => (
                                            <div key={idx} className="flex justify-between text-[7.5px] text-neutral-400 leading-tight">
                                              <span>{mv.type === 'healing' ? '⚡ ' : '🛡️ '} {mv.name}</span>
                                              <span className="text-green-650">[READY]</span>
                                            </div>
                                          ))}
                                        </div>
                                      </div>
                                    )}
                                  </div>

                                  {/* Right: Auto-Hacker Battle Logs Terminal */}
                                  <div className="md:col-span-3 bg-neutral-950/80 border border-green-950 p-3.5 rounded flex flex-col justify-between h-[310px] relative overflow-hidden">
                                    <div className="text-[8.5px] text-green-500 uppercase tracking-wider font-bold mb-1.5 flex justify-between items-center">
                                      <span>🖥️ LOG STREAM INTERCEPT:</span>
                                      <button 
                                        onClick={() => { sound.playBeep(650, 0.05, "sine"); setViewportGeneBalancePopupMailId(mail.id); }}
                                        className="text-[8px] bg-[#00FF41]/15 text-[#00FF41] hover:bg-[#00FF41]/25 border border-[#00FF41]/35 px-2 py-0.5 rounded font-mono font-bold tracking-tight cursor-pointer shadow-[0_0_8px_rgba(0,255,65,0.15)] animate-pulse"
                                        title="View list of genes gained/lost for entirety of this battle log"
                                      >
                                        [ TRANSFERS AUDIT ]
                                      </button>
                                      <span className="text-red-500/80 font-mono text-[8.5px] animate-pulse">● FEED ONLINE</span>
                                    </div>
                                    <div className="flex-1 overflow-y-auto font-mono text-[9px] leading-relaxed text-neutral-300 p-2 border border-neutral-900 bg-black/40 space-y-1 scrollbar-thin scrollbar-thumb-green-950 scrollbar-track-transparent custom-pox-scrollbar max-h-[260px]">
                                      {mail.autoHackLog.log.map((line, lIdx) => {
                                        const isHeal = line.includes("SPECIAL") && line.includes("Siphoning");
                                        const isEvasion = line.includes("SPECIAL") && line.includes("evasion");
                                        const isSuccess = line.includes("SUCCESS");
                                        const isDefeat = line.includes("DEFEATED") || line.includes("NEUTRALIZED");
                                        const isSessionDivider = line.includes("TRANSMISSION") || line.includes("REPORT AT") || line.includes("REPORT ON");

                                        return (
                                          <div 
                                            key={lIdx} 
                                            className={`${
                                              isHeal ? "text-emerald-400" :
                                              isEvasion ? "text-cyan-400" :
                                              isSuccess ? "text-[#00FF41] font-bold" :
                                              isDefeat ? "text-red-400 font-bold" :
                                              isSessionDivider ? "text-amber-500 font-extrabold border-b border-amber-950 pb-1 mt-3" :
                                              "text-neutral-400"
                                            }`}
                                          >
                                            {line}
                                          </div>
                                        );
                                      })}
                                    </div>

                                    {/* TRANSFERS AUDIT OVERLAY INLINE */}
                                    <AnimatePresence>
                                      {viewportGeneBalancePopupMailId === mail.id && (() => {
                                        const gained = mail.autoHackLog.genesGained || [];
                                        const lost = mail.autoHackLog.genesLost || [];
                                        return (
                                          <motion.div
                                            key="transfers-audit-popup"
                                            initial={{ opacity: 0, scale: 0.98 }}
                                            animate={{ opacity: 1, scale: 1 }}
                                            exit={{ opacity: 0, scale: 0.98 }}
                                            className="absolute inset-0 bg-[#020d04]/98 border border-[#00FF41]/60 rounded p-3.5 flex flex-col z-40 overflow-hidden font-mono text-left"
                                          >
                                            <div className="flex justify-between items-center border-b border-green-955 pb-1.5 mb-2 select-none">
                                              <span className="text-[10px] text-[#00FF41] font-bold tracking-widest uppercase flex items-center gap-1.5 font-mono">
                                                🤖 AUTO-HACK ACQUISITIONS
                                              </span>
                                              <button
                                                type="button"
                                                onClick={() => {
                                                  sound.playBeep(440, 0.05, "sine");
                                                  setViewportGeneBalancePopupMailId(null);
                                                }}
                                                className="px-2 py-0.5 rounded text-[8.5px] cursor-pointer font-bold tracking-wider border bg-green-950/80 hover:bg-green-900 border-green-850 text-green-500 hover:text-white"
                                              >
                                                ✕ CLOSE AUDIT
                                              </button>
                                            </div>

                                            <p className="text-[8.5px] text-neutral-405 mb-2 leading-snug font-mono select-none text-neutral-400 font-mono">
                                              Snatched target gene stream records.
                                            </p>

                                            <div className="flex-1 overflow-y-auto space-y-2 pr-0.5 custom-pox-scrollbar font-mono">
                                              <div>
                                                <div className="text-[9px] text-[#00FF41] font-bold tracking-wider uppercase mb-1.5 font-mono">
                                                  🛡️ SNATCHED GENE INFLOW ({gained.length})
                                                </div>
                                                <div className="grid grid-cols-2 gap-1.5">
                                                  {gained.map((seq, sIdx) => {
                                                    const isAnom = isAnomalousGene(seq);
                                                    return (
                                                      <div 
                                                        key={sIdx}
                                                        className={`p-1.5 rounded border flex flex-col font-mono text-[9px] select-all ${
                                                          isAnom 
                                                            ? "border-purple-500 bg-purple-950/20 text-purple-300"
                                                            : "border-green-900/40 bg-green-950/10 text-emerald-400"
                                                        }`}
                                                      >
                                                        <span className="font-extrabold tracking-widest">{seq}</span>
                                                      </div>
                                                    );
                                                  })}
                                                  {gained.length === 0 && (
                                                    <p className="col-span-2 text-center text-[9px] text-neutral-500 py-4 italic font-mono">No genes gained.</p>
                                                  )}
                                                </div>
                                              </div>
                                            </div>
                                          </motion.div>
                                        );
                                      })()}
                                    </AnimatePresence>
                                  </div>
                                </div>
                              </div>

                              <p className="text-[9px] text-[#00FF41]/40 font-mono uppercase mt-2 select-none leading-normal">
                                SECURITY BREACH DEEP RADAR TELEMETRY: Unauthorized genetic transceivers transmit encrypted stream packets directly onto localhost socket streams. Access is strictly governed.
                              </p>
                            </div>
                          );
                        }

                        if (mail.type === 'trade_request' && mail.tradeRequest) {
                          const tr = mail.tradeRequest;
                          const partnerCreature = tr.partnerOfferedCreature;
                          const status = tr.status;

                          const chosenOfferToGive = creatures.find(c => c.id === selectedTradeOfferId);
                          
                          const dailyMail = genMails.find(m => m.id === "MAIL-9921DX");
                          const dailySeqs = dailyMail ? dailyMail.sequences : [];
                          const partnerOfferMatchesDispatch = dailySeqs.includes(partnerCreature.sequence);
                          const partnerDesireMatchesDispatch = tr.partner.targetSequence && dailySeqs.includes(tr.partner.targetSequence);

                          return (
                            <div className="space-y-4 flex flex-col h-full justify-between font-mono">
                              <div className="space-y-3 font-mono">
                                <div className="border-b border-green-900/30 pb-3 font-mono">
                                  <div className="flex justify-between items-center mb-1 text-[9.5px]">
                                    <span className="text-cyan-400 font-bold">SECURED Node SYNCHRONIZER Transfer</span>
                                    <span className="text-white font-bold">{mail.date}</span>
                                  </div>
                                  <h3 className="text-xs sm:text-xs font-bold text-white tracking-widest uppercase">
                                    {mail.tagline}
                                  </h3>
                                </div>

                                <div className="grid grid-cols-1 md:grid-cols-5 gap-3.5">
                                  {/* Left Panel: Peer Offer Creature */}
                                  <div className="md:col-span-2 bg-black/60 border border-green-950 p-3.5 rounded font-mono space-y-3 flex flex-col justify-between">
                                    <div>
                                      <div className="flex justify-between items-start flex-wrap gap-1">
                                        <span className="text-[9.5px] text-cyan-400 font-bold uppercase select-none">PEER PROPOSER OFFER</span>
                                        <div className="flex gap-1 items-center">
                                          {partnerOfferMatchesDispatch && (
                                            <span className="px-1.5 py-0.5 bg-purple-950/80 text-purple-400 border border-purple-800/40 rounded text-[7.5px] font-bold font-mono tracking-wider leading-none">
                                              DISPATCH MATCH
                                            </span>
                                          )}
                                          <span className="text-[7.5px] bg-cyan-950/40 text-cyan-400 px-1.5 py-0.5 rounded border border-cyan-800/40 font-bold">
                                            {partnerCreature.faction}
                                          </span>
                                        </div>
                                      </div>
                                      <div className="text-white font-black text-xs sm:text-sm tracking-widest mt-1.5 uppercase select-all">
                                        "{partnerCreature.name}"
                                      </div>
                                      <div className="text-[8.5px] text-neutral-500 font-mono break-all mt-1 bg-black/40 p-1.5 rounded border border-neutral-900 leading-tight">
                                        {render8BlockColorCodedSequence(partnerCreature.sequence)}
                                      </div>
                                    </div>

                                    <div className="space-y-0.5 text-[9px] leading-normal text-neutral-300">
                                      <div className="flex justify-between border-b border-neutral-900/40 pb-0.5">
                                        <span className="text-neutral-500">VIT/HP:</span>
                                        <span className="text-[#00FF41] font-bold">{partnerCreature.vitality}</span>
                                      </div>
                                      <div className="flex justify-between border-b border-neutral-900/40 pb-0.5">
                                        <span className="text-neutral-500">ATTACK:</span>
                                        <span className="text-red-400 font-bold">{partnerCreature.attack}</span>
                                      </div>
                                      <div className="flex justify-between border-b border-neutral-900/40 pb-0.5">
                                        <span className="text-neutral-500">DEFENSE:</span>
                                        <span className="text-blue-400 font-bold">{partnerCreature.defense}</span>
                                      </div>
                                      <div className="flex justify-between border-b border-neutral-900/40 pb-0.5">
                                        <span className="text-neutral-500">WEAPON:</span>
                                        <span className="text-neutral-200">{partnerCreature.primaryWeapon}</span>
                                      </div>
                                      {tr.partner.targetSequence && (
                                        <div className="p-1.5 bg-cyan-950/15 border border-cyan-900/40 rounded text-[9px] mt-1.5 text-cyan-400 font-mono leading-normal relative overflow-hidden">
                                          Node Desires Target gene DNA: 
                                          <div className="font-bold text-white tracking-widest mt-0.5 break-all select-all text-[8px]">{renderColorCodedSequence(tr.partner.targetSequence)}</div>
                                          {partnerDesireMatchesDispatch && (
                                            <div className="mt-1 text-[7.5px] text-purple-400 font-mono font-bold flex items-center gap-1">
                                              <span className="w-1.5 h-1.5 rounded-full bg-purple-500 animate-pulse" />
                                              DESIRED SEQUENCE MATCHES DAILY DISPATCH
                                            </div>
                                          )}
                                        </div>
                                      )}
                                    </div>
                                    
                                    <div className="border border-green-950/20 bg-neutral-950/40 p-1.5 rounded text-center">
                                      <span className="text-[7px] text-neutral-500 block uppercase">PEER NODE ID:</span>
                                      <span className="text-[9px] text-white font-bold font-mono tracking-widest block uppercase select-all">{tr.partner.uid}</span>
                                    </div>
                                  </div>

                                  {/* Right Panel: Player Response */}
                                  <div className="md:col-span-3 bg-neutral-950/80 border border-green-950 p-3.5 rounded flex flex-col justify-between min-h-[310px]">
                                    <div className="text-[8.5px] text-green-500 uppercase tracking-wider font-bold mb-1.5 flex justify-between items-center select-none font-mono">
                                      <span>📥 TRANSACTION SYNC cockpit:</span>
                                      <span className={`text-[8px] font-mono font-bold leading-none px-1.5 py-0.5 rounded ${
                                        status === 'accepted' ? 'bg-green-950 text-[#00FF41] border border-green-550' :
                                        status === 'declined' ? 'bg-red-950 text-red-500 border border-red-900/40' :
                                        'bg-amber-950/45 text-amber-500 border border-amber-900/40 animate-pulse'
                                      }`}>
                                        {status === 'pending' ? 'PENDING SYNC' : status === 'accepted' ? 'SYNC COMPLETED' : 'ABORTED CONNECTION'}
                                      </span>
                                    </div>

                                    {status === 'pending' ? (
                                      <div className="space-y-3.5 flex flex-col flex-1 justify-between">
                                        <div className="bg-black/45 p-2.5 rounded border border-green-950 text-[9.5px] space-y-1.5 leading-normal">
                                          <p className="text-white font-mono font-bold">[ ACTION REQUIRED ]</p>
                                          <p className="text-neutral-400">
                                            Select a sequence from your Gen-Vault to transfer in exchange for the offered sequence.
                                          </p>

                                          {isInboxTradeArchiveOpened ? (
                                            <div className="space-y-2 border border-green-950/50 bg-black/70 p-2.5 rounded text-left">
                                              <div className="flex justify-between items-center border-b border-green-950/40 pb-1 mb-1">
                                                <span className="text-[9px] font-mono text-green-500 font-bold uppercase select-none tracking-wider">Archive Selection</span>
                                                <button 
                                                  type="button"
                                                  onClick={() => {
                                                    sound.playBeep(440, 0.05, "sine");
                                                    setIsInboxTradeArchiveOpened(false);
                                                  }}
                                                  className="text-[8px] text-red-500 hover:text-white border border-red-900/40 px-1 py-0.5 rounded font-mono font-bold cursor-pointer transition-colors"
                                                >
                                                  ✕ CLOSE
                                                </button>
                                              </div>

                                              {/* Search fields inline */}
                                              <div className="space-y-1 bg-black/60 p-2 border border-green-955 rounded font-mono text-[8.5px]">
                                                <div className="flex gap-1.5 items-center">
                                                  <span className="text-green-600 uppercase text-[7.5px] font-bold shrink-0">Filter:</span>
                                                  <input 
                                                    type="text"
                                                    value={tradeArchiveSearch}
                                                    onChange={(e) => setTradeArchiveSearch(e.target.value)}
                                                    placeholder="Search spec..."
                                                    className="w-full bg-black text-[#00FF41] border border-green-955 rounded px-1 py-0.5 placeholder-green-955 text-[8px] uppercase tracking-wide focus:outline-none focus:border-green-500 font-mono"
                                                  />
                                                </div>
                                                <div className="grid grid-cols-2 gap-1.5">
                                                  <div>
                                                    <select 
                                                      value={tradeArchiveFaction}
                                                      onChange={(e) => setTradeArchiveFaction(e.target.value)}
                                                      className="w-full bg-black text-[#00FF41] border border-[#00FF41]/20 rounded px-1 py-0.5 text-[7.5px] uppercase focus:outline-none focus:border-green-500 font-mono"
                                                    >
                                                      <option value="ALL">ALL FACTIONS</option>
                                                      <option value="Infection">Infection</option>
                                                      <option value="Mech">Mech</option>
                                                      <option value="Parasite">Parasite</option>
                                                      <option value="Containment">Containment</option>
                                                    </select>
                                                  </div>
                                                  <div>
                                                    <select 
                                                      value={tradeArchiveSort}
                                                      onChange={(e) => setTradeArchiveSort(e.target.value)}
                                                      className="w-full bg-black text-[#00FF41] border border-[#00FF41]/20 rounded px-1 py-0.5 text-[7.5px] uppercase focus:outline-none focus:border-green-500 font-mono"
                                                    >
                                                      <option value="name-asc">A-Z Name</option>
                                                      <option value="vitality-desc">Vitality (H-L)</option>
                                                      <option value="attack-desc">Attack (H-L)</option>
                                                    </select>
                                                  </div>
                                                </div>
                                              </div>

                                              <div className="overflow-y-auto max-h-28 h-28 border border-neutral-900 bg-neutral-950/60 p-1.5 space-y-1 rounded custom-pox-scrollbar">
                                                {(() => {
                                                  const filtered = creatures.filter(c => {
                                                    if (tradeArchiveSearch.trim()) {
                                                      const q = tradeArchiveSearch.toLowerCase();
                                                      const nameMatch = c.name.toLowerCase().includes(q);
                                                      const typeMatch = (c.type || "").toLowerCase().includes(q);
                                                      if (!nameMatch && !typeMatch) return false;
                                                    }
                                                    if (tradeArchiveFaction !== "ALL" && c.faction !== tradeArchiveFaction) {
                                                      return false;
                                                    }
                                                    return true;
                                                  }).sort((a, b) => {
                                                    switch (tradeArchiveSort) {
                                                      case "name-asc": return a.name.localeCompare(b.name);
                                                      case "vitality-desc": return b.vitality - a.vitality;
                                                      case "attack-desc": return b.attack - a.attack;
                                                      default: return 0;
                                                    }
                                                  });

                                                  if (filtered.length === 0) {
                                                    return <div className="text-center py-4 text-green-700 text-[8.5px] uppercase font-mono">No specimens found</div>;
                                                  }

                                                  return filtered.map((c) => {
                                                    const isSel = selectedTradeOfferId === c.id;
                                                    const partnerDesiresThis = tr.partner.targetSequence && c.sequence.includes(tr.partner.targetSequence);
                                                    const coherence = getCoherence(c.sequence, tr.partner.targetSequence || "");
                                                    return (
                                                      <button
                                                        key={c.id}
                                                        type="button"
                                                        onClick={() => {
                                                          sound.playBeep(450, 0.05, "sine");
                                                          setSelectedTradeOfferId(c.id);
                                                          setIsInboxTradeArchiveOpened(false);
                                                        }}
                                                        className={`w-full p-1.5 rounded border text-left flex justify-between items-center transition-all cursor-pointer ${
                                                          isSel 
                                                            ? 'bg-[#00FF41]/10 border-[#00FF41] text-[#00FF41]'
                                                            : 'bg-black/60 border-neutral-900 hover:border-green-955 text-neutral-400'
                                                        }`}
                                                      >
                                                        <div className="flex flex-col font-mono text-[8px]">
                                                          <div className="flex items-center gap-1 flex-wrap">
                                                            <span className="font-bold text-[8.5px] uppercase font-mono text-white">"{c.name}"</span>
                                                            {coherence === 'full' && (
                                                              <span className="px-1 bg-emerald-950 text-[#00FF41] border border-emerald-500 rounded text-[6px] font-bold font-mono tracking-widest leading-none">
                                                                FULL COH
                                                              </span>
                                                            )}
                                                            {coherence === 'partial' && (
                                                              <span className="px-1 bg-yellow-950/80 text-yellow-500 border border-yellow-600/55 rounded text-[6px] font-bold font-mono tracking-wider leading-none">
                                                                PART COH
                                                              </span>
                                                            )}
                                                          </div>
                                                          <span className="text-[7px] font-mono text-neutral-500">HP: {c.vitality} | ATK: {c.attack}</span>
                                                        </div>
                                                        {partnerDesiresThis && (
                                                          <span className="text-[6px] bg-[#00FF41]/10 text-[#00FF41] border border-[#00FF41]/35 px-1 rounded font-bold animate-pulse font-mono uppercase">
                                                            BONUS TARGET
                                                          </span>
                                                        )}
                                                      </button>
                                                    );
                                                  });
                                                })()}
                                              </div>
                                            </div>
                                          ) : (
                                            <div className="space-y-2 text-left">
                                              {selectedTradeOfferId ? (() => {
                                                const c = creatures.find(cr => cr.id === selectedTradeOfferId);
                                                if (!c) return null;
                                                const partnerDesiresThis = tr.partner.targetSequence && c.sequence.includes(tr.partner.targetSequence);
                                                return (
                                                  <div className="bg-green-955/20 border border-[#00FF41]/30 rounded p-2 flex justify-between items-center relative gap-2 font-mono">
                                                    <div className="text-left space-y-0.5 min-w-0 flex-1">
                                                      <div className="font-bold text-[7.5px] text-zinc-500 uppercase leading-none">Selected offer:</div>
                                                      <div className="text-[9px] font-black text-[#00FF41] uppercase">"{c.name}"</div>
                                                      <div className="text-[7.5px] text-neutral-400 max-w-[150px] truncate block leading-none mt-0.5">
                                                        {render8BlockColorCodedSequence(c.sequence)}
                                                      </div>
                                                      <div className="text-[7px] text-neutral-500">HP: {c.vitality} | ATK: {c.attack}</div>
                                                    </div>
                                                    <div className="flex flex-col items-end gap-1 shrink-0">
                                                      {partnerDesiresThis && (
                                                        <span className="text-[6px] bg-[#00FF41]/10 text-[#00FF41] border border-[#00FF41]/35 px-1 rounded font-bold animate-pulse uppercase leading-none py-0.5 select-none hover:opacity-100 font-bold">
                                                          BONUS TARGET
                                                        </span>
                                                      )}
                                                      <div className="flex gap-1">
                                                        <button
                                                          type="button"
                                                          onClick={() => {
                                                            sound.playBeep(440, 0.05, "sine");
                                                            setIsInboxTradeArchiveOpened(true);
                                                          }}
                                                          className="text-[7.5px] text-green-400 hover:text-white border border-green-950/60 hover:bg-green-955/25 px-1 py-0.5 rounded uppercase font-bold cursor-pointer transition-all"
                                                        >
                                                          CHANGE
                                                        </button>
                                                        <button
                                                          type="button"
                                                          onClick={() => {
                                                            sound.playBeep(440, 0.05, "sine");
                                                            setSelectedTradeOfferId(null);
                                                          }}
                                                          className="text-[7.5px] text-red-500 hover:text-white border border-red-950/60 hover:bg-red-955/20 px-1 py-0.5 rounded uppercase font-bold cursor-pointer transition-all"
                                                        >
                                                          EJECT
                                                        </button>
                                                      </div>
                                                    </div>
                                                  </div>
                                                );
                                              })() : (
                                                <div className="flex flex-col items-center justify-center border border-dashed border-green-955 rounded p-4 bg-neutral-950/30 text-center space-y-2 select-none">
                                                  <span className="text-green-800 text-[14px]">🧬</span>
                                                  <span className="text-[7.5px] text-zinc-500 font-mono uppercase tracking-widest block font-bold">No offer selected</span>
                                                  <button
                                                    type="button"
                                                    onClick={() => {
                                                      sound.playBeep(440, 0.05, "sine");
                                                      setIsInboxTradeArchiveOpened(true);
                                                    }}
                                                    className="py-1 px-2.5 bg-green-955/80 hover:bg-green-900 border border-green-600/40 hover:border-[#00FF41]/85 text-[#00FF41] rounded font-bold uppercase text-[8px] cursor-pointer tracking-wider transition-all"
                                                  >
                                                    CHOOSE OFFER SPECIMEN
                                                  </button>
                                                </div>
                                              )}
                                            </div>
                                          )}
                                        </div>

                                        <div className="grid grid-cols-2 gap-2 font-mono">
                                          <button
                                            type="button"
                                            onClick={() => handleDeclineInboxTrade(mail.id)}
                                            className="py-2 bg-neutral-900 hover:bg-neutral-800 text-neutral-400 border border-neutral-800 rounded-sm font-bold text-[9.5px] uppercase tracking-wider cursor-pointer"
                                          >
                                            DECLINE
                                          </button>
                                          
                                          <button
                                            type="button"
                                            disabled={!chosenOfferToGive}
                                            onClick={() => handleAcceptInboxTrade(mail.id, chosenOfferToGive!)}
                                            className={`py-2 rounded-sm font-bold text-[9.5px] uppercase tracking-wider transition-all border cursor-pointer ${
                                              chosenOfferToGive
                                                ? 'bg-emerald-800 text-white hover:bg-emerald-750 border-emerald-500 shadow-[0_0_10px_rgba(16,185,129,0.3)]'
                                                : 'bg-neutral-950 text-neutral-700 border-neutral-900 cursor-not-allowed select-none'
                                            }`}
                                          >
                                            INITIATE TRANSFER
                                          </button>
                                        </div>
                                      </div>
                                    ) : status === 'accepted' ? (
                                      <div className="flex-1 flex flex-col items-center justify-center text-center space-y-3.5 p-4">
                                        <div className="w-11 h-11 bg-green-950/50 border-2 border-[#00FF41] rounded-full flex items-center justify-center text-[#00FF41] shadow-[0_0_12px_rgba(0,255,100,0.25)]">
                                          ✓
                                        </div>
                                        <div className="space-y-1 select-none">
                                          <h4 className="text-white font-mono font-bold text-xs uppercase tracking-widest">TRANSACTION SECURED</h4>
                                          <p className="text-[10px] text-neutral-400 max-w-xs leading-relaxed">
                                            The node transfer transaction swapped player sequence "{tr.selectedCreatureToOffer?.name}" for peer bio-mutant node element "{partnerCreature.name}". Complete.
                                          </p>
                                        </div>
                                      </div>
                                    ) : (
                                      <div className="flex-1 flex flex-col items-center justify-center text-center space-y-3.5 p-4">
                                        <div className="w-11 h-11 bg-red-950/50 border-2 border-red-500 rounded-full flex items-center justify-center text-red-500 select-none">
                                          ⚠
                                        </div>
                                        <div className="space-y-1 select-none">
                                          <h4 className="text-white font-mono font-bold text-xs uppercase tracking-widest">CONNECTION ABORTED</h4>
                                          <p className="text-[10px] text-neutral-500 max-w-xs leading-normal">
                                            Transceiver transfer request declined on this stream socket. Desynchronization successfully executed.
                                          </p>
                                        </div>
                                      </div>
                                    )}
                                  </div>
                                </div>
                              </div>

                              <p className="text-[9px] text-[#00FF41]/40 font-mono uppercase mt-2 select-none leading-normal">
                                INFO: Transfers completed over the G.E.N. Network are automatically scanned for network security. Anomalous data will be captured and offending nodes sanctioned.
                              </p>
                            </div>
                          );
                        }

                        const ownedSeqList = creatures.map(c => c.sequence);
                        const matchedOwned = mail.sequences.filter(seq => ownedSeqList.includes(seq));
                        const matchedPeers = nearbyUsers.filter(u => u.targetSequence && mail.sequences.includes(u.targetSequence));

                        return (
                          <div className="space-y-4 flex flex-col h-full justify-between">
                            <div className="space-y-3">
                              {/* Header details */}
                              <div className="border-b border-green-900/30 pb-3 font-mono">
                                <div className="flex justify-between items-center mb-1 text-[9.5px]">
                                  <span className="text-green-500 font-bold">FROM: TRANSMISSION_NODE_A_CENTRAL</span>
                                  <span className="text-[#00FF41] font-bold">{mail.date}</span>
                                </div>
                                <h3 className="text-sm font-bold text-white tracking-widest uppercase">
                                  {mail.tagline}
                                </h3>
                              </div>

                              {/* Target sequence dispatch counts summaries */}
                              <div className="grid grid-cols-2 gap-3 pb-2 border-b border-neutral-950">
                                <div className="bg-black/55 border border-green-900/30 p-2.5 rounded text-center">
                                  <span className="text-[8.5px] text-neutral-400 font-mono uppercase block">ARCHIVE COMPLIANCY MATRIX</span>
                                  <span className={`text-xs font-mono font-bold block mt-1 ${
                                    matchedOwned.length > 0 ? 'text-[#00FF41]' : 'text-neutral-500'
                                  }`}>
                                    {matchedOwned.length} / 128 MATCHES
                                  </span>
                                </div>
                                <div className="bg-black/55 border border-green-900/30 p-2.5 rounded text-center">
                                  <span className="text-[8.5px] text-neutral-400 font-mono uppercase block">PEER SYNC-NET CAPTURES</span>
                                  <span className={`text-xs font-mono font-bold block mt-1 ${
                                    matchedPeers.length > 0 ? 'text-cyan-400' : 'text-neutral-500'
                                  }`}>
                                    {matchedPeers.length} PEERS ONLINE
                                  </span>
                                </div>
                              </div>

                              <div>
                                <h5 className="text-[10px] text-green-500 font-bold uppercase tracking-wider mb-2">
                                  [ DISPATCH TARGET SEQUENCES: 128 HIGH-PRIORITY PATHWAYS ]
                                </h5>
                                
                                {/* 128 sequences grid listing */}
                                <div className="border border-green-900/25 bg-black/60 rounded p-2 overflow-y-auto max-h-64 h-64 space-y-1.5 custom-pox-scrollbar">
                                  {mail.sequences.map((seq, sIndex) => {
                                    const isOwnedMatch = ownedSeqList.includes(seq);
                                    const matchedPeerNode = nearbyUsers.find(u => u.targetSequence === seq);

                                    return (
                                      <div 
                                        key={sIndex} 
                                        className={`flex flex-col sm:flex-row justify-between sm:items-center bg-black/40 p-2 rounded border text-[9.5px] font-mono leading-none gap-2 ${
                                          isOwnedMatch 
                                            ? 'border-green-500 shadow-[inset_0_0_6px_rgba(0,255,100,0.15)] bg-green-950/10' 
                                            : matchedPeerNode
                                            ? 'border-cyan-500/80 bg-cyan-950/10'
                                            : 'border-green-950/50 hover:border-green-900'
                                        }`}
                                      >
                                        <div className="flex items-center gap-2">
                                          <span className="text-green-700 font-bold">[{String(sIndex + 1).padStart(3, '0')}]</span>
                                          <span className="text-white tracking-widest break-all select-all font-semibold font-mono text-[8px] sm:text-[9.5px]">
                                            {seq}
                                          </span>
                                        </div>
                                        <div className="flex gap-2.5 items-center justify-end select-none">
                                          {isOwnedMatch && (
                                            <span className="text-[8px] px-1.5 py-0.5 rounded bg-green-950 text-[#00FF41] border border-green-500 font-extrabold uppercase">
                                              SECURED
                                            </span>
                                          )}
                                          {matchedPeerNode?.uid && (
                                            <span className="text-[8px] px-1.5 py-0.5 rounded bg-cyan-950 text-cyan-400 border border-cyan-500 font-extrabold uppercase">
                                              PEER: {matchedPeerNode.uid}
                                              {stats.totalHacksInitiated !== undefined && (
                                                <span className="ml-1 text-green-500">*</span>
                                              )}
                                            </span>
                                          )}
                                          {!isOwnedMatch && !matchedPeerNode && (
                                            <span className="text-[7.5px] text-green-900 uppercase font-bold pr-1 select-none">
                                              DESIDERATA
                                            </span>
                                          )}
                                          <button
                                            type="button"
                                            onClick={() => {
                                              sound.playBeep(450, 0.05, "sine");
                                              setSelectedDispatchSequence(seq);
                                              setNetSplicerSlots([null, null, null, null, null, null, null, null]);
                                              setActiveNetSlotSelection(null);
                                            }}
                                            className="px-2 py-0.5 ml-2 text-[8px] font-extrabold uppercase bg-emerald-950 hover:bg-emerald-900 hover:text-white text-emerald-400 border border-emerald-500 rounded cursor-pointer transition-all active:scale-95"
                                          >
                                            Splicer
                                          </button>
                                        </div>
                                      </div>
                                    );
                                  })}
                                </div>
                              </div>
                            </div>

                            <p className="text-[9.5px] text-green-700 font-mono mt-3 uppercase leading-tight">
                              INFORMATION DISSEMINATION REGULATION: This message incorporates 128 active target sequences. Satisfied matching targets empower P.O.X. RF node trade modules on transceiver synchronization locks.
                            </p>
                          </div>
                        );
                      })() : (
                        <div className="flex flex-col items-center justify-center h-full text-center p-6 space-y-2">
                          <Terminal className="w-8 h-8 text-[#00FF41] animate-pulse" />
                          <h3 className="text-sm font-extrabold text-[#00FF41] tracking-widest uppercase">
                            NO BROADCAST SELECTED
                          </h3>
                          <p className="text-[10px] text-green-700 font-mono max-w-xs leading-normal">
                            Activate any decrypted G.E.N. broadcast thread inside your secure inbox queue to display high-priority target genome list.
                          </p>
                        </div>
                      )}
                    </div>
                  </motion.div>
                )}
                       {false && (
                  <motion.div 
                    key="area_scanner"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    className="h-auto flex flex-col p-5 bg-neutral-950/45 border border-green-900/40 rounded min-h-[500px] pb-16 mb-8 relative overflow-visible text-left"
                  >
                    {/* Retro styling grid */}
                    <div className="absolute inset-0 pointer-events-none bg-[radial-gradient(rgba(0,255,65,0.012)_1px,transparent_1px)] [background-size:16px_16px]" />
                    
                    {/* Header bar featuring Title and Toggle Map Trigger button */}
                    <div className="z-10 border-b border-green-900/40 pb-3 mb-4 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 font-mono relative">
                      <div>
                        <h2 className="text-xs sm:text-xs font-black text-white tracking-widest uppercase flex items-center gap-1.5">
                          SCANNER CONTROLS
                        </h2>
                      </div>

                      <button
                        type="button"
                        onClick={() => {
                          sound.playBeep(520, 0.08, "sine");
                          setIsMapPopupOpen(true);
                          addScannerLog("TACTICAL_MAP: Maximized coordinates tracking grid overlay feed.");
                        }}
                        className="px-3 py-1.5 bg-emerald-950/80 hover:bg-emerald-900 border border-emerald-500/40 hover:border-[#00FF41] text-[#00FF41] hover:text-white rounded text-[8.5px] font-black uppercase tracking-widest transition-all duration-150 active:scale-95 flex items-center gap-2 cursor-pointer shadow-[0_0_10px_rgba(16,185,129,0.2)] select-none animate-pulse"
                      >
                        <MapPin className="w-3.5 h-3.5 animate-bounce" />
                        VIEW SCANNER MAP RADAR
                      </button>
                    </div>

                    {/* REDESIGNED AREA SCANNER PAGE: 2-FRAME SETUP */}
                    <div className="z-10 grid grid-cols-1 md:grid-cols-2 gap-4 flex-1 items-stretch relative min-h-[420px]">
                      
                      {/* Left Frame: Anomalies Nearby (Details sidebar if selected, list if not) */}
                      <div className="bg-black/60 border border-green-900/40 p-4 rounded flex flex-col justify-between min-h-[420px] md:h-full text-left font-mono relative overflow-hidden shadow-[inset_0_1px_3px_rgba(0,0,0,0.6)]">
                        <div className="absolute inset-0 pointer-events-none bg-[radial-gradient(rgba(0,255,65,0.01)_1px,transparent_1px)] [background-size:12px_12px]" />
                        
                        <div className="z-10 flex flex-col h-full justify-between space-y-4">
                          <div>

                            {selectedAnomalyId ? (() => {
                              const anom = bioAnomalies.find(a => a.id === selectedAnomalyId);
                              if (!anom) return null;
                              const isHarvestable = anom.distance <= scanRadius;
                              
                              let factionColor = "text-red-400 border-red-950 bg-red-950/20";
                              if (anom.faction === "Mech") {
                                factionColor = "text-yellow-400 border-yellow-950 bg-yellow-950/20";
                              } else if (anom.faction === "Parasite") {
                                factionColor = "text-purple-400 border-purple-950 bg-purple-950/20";
                              } else if (anom.faction === "Containment") {
                                factionColor = "text-cyan-400 border-cyan-950 bg-cyan-950/20";
                              }

                              return (
                                <div className="space-y-3.5 antialiased">
                                  <div className="flex items-center justify-between border-b border-green-900/20 pb-2 gap-1.5">
                                    <h3 className="text-xs font-black text-white tracking-widest uppercase truncate max-w-[100px]">
                                      ANOMALY SPEC
                                    </h3>
                                    <div className="flex items-center gap-1 shrink-0">
                                      <button
                                        type="button"
                                        onClick={() => {
                                          sound.playBeep(420, 0.05, "sine");
                                          if (devForceAnomaly) {
                                            setIsDevSpeedActive(prev => !prev);
                                          }
                                          addScannerLog(`DEV_SPEED: Overclock speed toggled to ${!isDevSpeedActive ? "60x" : "1x"}`);
                                        }}
                                        className={`text-[7.5px] border font-mono px-1.5 py-0.5 rounded uppercase font-bold cursor-pointer transition-all active:scale-95 ${
                                          isDevSpeedActive 
                                            ? "bg-amber-955 border-amber-500 text-amber-500 animate-pulse font-extrabold" 
                                            : "border-neutral-800 text-neutral-500 hover:text-neutral-300"
                                        }`}
                                        title="Enable 60x fast forward multiplier"
                                      >
                                        ⚡ DEV (60x)
                                      </button>
                                      <button
                                        type="button"
                                        onClick={() => {
                                          sound.playBeep(450, 0.05, "sine");
                                          setSelectedAnomalyId(null);
                                          addScannerLog("LOCKED_TARGET: Released lock channels.");
                                        }}
                                        className="text-[8px] border border-green-900/60 hover:bg-neutral-900 hover:text-white px-1.5 py-0.5 rounded text-[#00FF41] uppercase shrink-0 cursor-pointer transition-all active:scale-90"
                                      >
                                        BACK
                                      </button>
                                    </div>
                                  </div>

                                  <div className={`border p-2 rounded ${factionColor} font-mono space-y-1.5`}>
                                    <div className="text-[10px] uppercase font-bold text-white flex items-center justify-between">
                                      <span>ANOMALY ID:</span>
                                      <span className="text-[9px] px-1 bg-black/40 rounded border border-current font-bold">{anom.id}</span>
                                    </div>
                                    <div className="text-[8px] opacity-80 grid grid-cols-2 gap-y-1">
                                      <div>SECTOR TYPE:</div>
                                      <div className="text-right font-bold text-white uppercase">{anom.faction}</div>
                                      <div>DIST/DIR:</div>
                                      <div className="text-right text-white font-bold">{anom.distance.toFixed(1)} FT &bull; {anom.direction}</div>
                                    </div>
                                  </div>

                                  <div className="bg-black/60 border border-green-950 p-2 rounded text-[8.5px] space-y-1 leading-normal">
                                    <div className="text-[#00FF41] font-bold text-[8px] uppercase tracking-wider select-none border-b border-green-900/20 pb-1 flex items-center justify-between">
                                      <span>Detected Anomalous Gene</span>
                                    </div>
                                    <div className="text-white bg-neutral-950 border border-green-900/20 py-1 px-1.5 rounded text-center text-[10px] select-all font-mono tracking-wider font-extrabold shadow-[2px_2px_0px_rgba(0,255,65,0.03)] border-dashed">
                                      {anom.gene}
                                    </div>
                                    <div className="text-green-700 text-[7.5px] leading-tight pt-1">
                                      STOCK COUNT: <strong className="text-white">{(sequences.find(s => s.sequence === anom.gene)?.count) || 0}</strong> SAVED
                                    </div>
                                  </div>

                                  {/* Sync Net Sweep Validation Button */}
                                  <div className="space-y-2 pt-1 border-t border-green-950/45">
                                    {(() => {
                                      // Check if there is an active/incomplete mission for this specific anomaly
                                      const activeMission = harvestingMissions.find(
                                        (m) => m.anomalyId === anom.id && !m.isReturned
                                      );

                                      if (activeMission) {
                                        // Display active mission status & retrieval controls
                                        const remainingSec = Math.max(0, activeMission.totalDuration - (activeMission.elapsedSeconds || 0));
                                        const progressPercent = Math.min(100, Math.round(((activeMission.elapsedSeconds || 0) / activeMission.totalDuration) * 100));
                                        const remainingStr = `${Math.floor(remainingSec / 60)}m ${(remainingSec % 60).toString().padStart(2, "0")}s`;

                                        return (
                                          <div className="bg-neutral-900 border border-green-900/60 p-2 text-left rounded font-mono space-y-2">
                                            <div className="flex items-center justify-between font-bold text-[8.5px] uppercase text-white pb-1 border-b border-green-950/50">
                                              <span className="text-amber-400 flex items-center gap-1">
                                                <span className="w-1.5 h-1.5 bg-amber-500 rounded-full animate-ping inline-block" />
                                                HARVEST IN PROGRESS
                                              </span>
                                              <span className="text-neutral-500">{activeMission.creatureName}</span>
                                            </div>

                                            <div className="text-[8px] leading-relaxed text-neutral-400 space-y-0.5">
                                              <div className="flex justify-between">
                                                <span>EST. DURATION:</span>
                                                <span className="text-white font-bold">{Math.round(activeMission.totalDuration / 60)} MINS</span>
                                              </div>
                                              <div className="flex justify-between">
                                                <span>DISTANCE:</span>
                                                <span className="text-white font-bold">{activeMission.dispatchDistance.toFixed(1)} FT</span>
                                              </div>
                                              <div className="flex justify-between">
                                                <span>EST. QUALITY:</span>
                                                <span className="text-[#00FF41] font-bold">
                                                  {Math.max(0, Math.round(100 - (activeMission.dispatchDistance / ((anom.heatZoneDiameter || 20) / 2)) * 100))}% ACCURACY
                                                </span>
                                              </div>
                                              <div className="flex justify-between">
                                                <span>TIME REMAINING:</span>
                                                <span className="text-white font-mono font-bold">{remainingStr}</span>
                                              </div>
                                            </div>

                                            {/* Progress Bar inside sidebar */}
                                            <div className="w-full bg-neutral-950 border border-green-950/60 h-2 rounded overflow-hidden">
                                              <div 
                                                className="bg-[#00E1FF] h-full transition-all duration-300" 
                                                style={{ width: `${progressPercent}%` }}
                                              />
                                            </div>

                                            <div className="bg-black/85 p-1 px-1.5 rounded border border-green-950/50 text-[7.5px] text-green-500">
                                              <b>ACQUIRED BASES ({activeMission.harvestedGenes.length}/4):</b>{" "}
                                              <span className="text-white font-bold">
                                                {activeMission.harvestedGenes.length > 0 
                                                  ? activeMission.harvestedGenes.join(", ") 
                                                  : "SCANNING ORBITAL RADIATIVES..."}
                                              </span>
                                            </div>

                                            {activeMission.isCompleted ? (
                                              <button
                                                type="button"
                                                onClick={() => handleRetrieveHarvestedGenes(activeMission.id)}
                                                className="w-full py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white font-black text-[9px] uppercase rounded border border-emerald-400 cursor-pointer shadow-[0_0_8px_#10b981] transition-all hover:scale-102 flex items-center justify-center gap-1 animate-pulse"
                                              >
                                                <Check className="w-3.5 h-3.5 mr-1" />
                                                RECALL CYBORG & BANK STOCKPILE
                                              </button>
                                            ) : (
                                              <div className="text-[7px] text-zinc-500 italic text-center leading-tight">
                                                Retrieval available upon mission completion. Genes are banked on safe return.
                                              </div>
                                            )}
                                          </div>
                                        );
                                      }

                                      if (anom.harvested) {
                                        return (
                                          <div className="border border-neutral-800 bg-neutral-950/80 p-2 text-center rounded text-[9px] text-neutral-500 font-bold uppercase select-none">
                                            ✕ SIGNATURE DEPLOYED &bull; EMPTY
                                          </div>
                                        );
                                      }

                                      if (!isHarvestable) {
                                        return (
                                          <div className="border border-red-950 bg-red-950/15 p-2 rounded text-[8.5px] text-red-400 text-center font-bold">
                                            <p className="uppercase font-mono text-[9px] text-red-500 mb-0.5">✕ GENE OUT OF RANGE</p>
                                            <span className="text-[7.5px] leading-normal tracking-wide block">
                                              ANOMALY DISTANCE ({anom.distance.toFixed(0)} FT) EXCEEDS TERMINAL RADIO RANGE ({scanRadius.toFixed(0)} FT).
                                            </span>
                                          </div>
                                        );
                                      }

                                      // Create standard dispatch Setup
                                      const availableCyborgs = getAvailableHarvestingCreatures();
                                      const lockDist = customTapCoords ? customTapCoords.distance : 0.0;
                                      const bioRadius = (anom.heatZoneDiameter || 20) / 2;
                                      const angleRatio = lockDist / Math.max(1, bioRadius);
                                      const yieldChance = Math.max(0, Math.round(100 - angleRatio * 100));

                                      return (
                                        <div className="space-y-2 mt-1 z-10 text-left">
                                          <div className="bg-emerald-950/10 border border-emerald-950/50 p-1.5 rounded text-[8px] font-mono leading-tight space-y-1">
                                            <div className="text-white font-bold uppercase flex justify-between">
                                              <span>HARVEST TARGET ZONE LOCK</span>
                                              <span className="text-[#00FF41]">
                                                (DIA: {anom.heatZoneDiameter}FT)
                                              </span>
                                            </div>
                                            <div className="text-neutral-400 text-[7.5px]">
                                              TAP MAP WITHIN HEAT RADIUS CIRCLE TO PLACE BEACON CORRELATIONS, OR DISPATCH TO CENTER CORRELATION FOR MAX ACCURACY.
                                            </div>
                                            <div className="flex justify-between text-[#00FF41] font-bold border-t border-emerald-950/40 pt-1">
                                              <span>DISTANCE PINPOINT:</span>
                                              <span>{customTapCoords ? `${lockDist.toFixed(1)} FT FROM NODE` : "CENTER NODE (0.0 FT)"}</span>
                                            </div>
                                            <div className="flex justify-between text-white">
                                              <span>STATION ACCURACY:</span>
                                              <span className="font-bold">{yieldChance}% FLUID SPECIMEN EXTRACT</span>
                                            </div>
                                          </div>

                                          <div className="text-[8px] font-bold uppercase text-neutral-400 tracking-wider">
                                            Select Sequence to Harvest Anomaly:
                                          </div>

                                          {availableCyborgs.length > 0 ? (
                                            <div className="space-y-1.5 max-h-[145px] overflow-y-auto custom-pox-scrollbar pr-0.5">
                                              {availableCyborgs.map((c) => {
                                                const speedDuration = Math.max(120, Math.round(480 - (c.speed * 2.4)));
                                                const speedMinutes = Math.round(speedDuration / 60);

                                                return (
                                                  <div 
                                                    key={c.id} 
                                                    className="bg-black/50 hover:bg-neutral-900 p-1.5 px-2 border border-green-950/40 rounded flex items-center justify-between gap-2 transition-all"
                                                  >
                                                    <div className="font-mono text-[8.5px] truncate max-w-[125px]">
                                                      <div className="text-white font-black flex items-center gap-1">
                                                        <span className="w-1.5 h-1.5 bg-green-500 rounded-full inline-block" />
                                                        {c.name}
                                                      </div>
                                                    </div>

                                                    <button
                                                      type="button"
                                                      onClick={() => handleDispatchCreatureToHarvest(c.id, anom.id)}
                                                      className="px-2 py-1 bg-emerald-950 hover:bg-emerald-900 border border-emerald-700 text-[#00FF41] hover:text-white rounded text-[8px] font-bold uppercase shrink-0 transition-transform active:scale-95 cursor-pointer"
                                                    >
                                                      DISPATCH
                                                    </button>
                                                  </div>
                                                );
                                              })}
                                            </div>
                                          ) : (
                                            <div className="border border-neutral-900 bg-neutral-950 p-2 text-center rounded text-[7.5px] text-neutral-500 italic leading-snug">
                                              [!] NO AVAILABLE CYBORGS FOR DISPATCH.<br/>
                                              CONSTRUCT NEW SEC DEFENDERS TO HARVEST GENE ANOMALIES.
                                            </div>
                                          )}
                                        </div>
                                      );
                                    })()}
                                  </div>
                                  
                                  {/* Return to anomaly list button similar to re-calibrate button */}
                                  <button
                                    type="button"
                                    onClick={() => {
                                      sound.playBeep(450, 0.05, "sine");
                                      setSelectedAnomalyId(null);
                                      addScannerLog("LOCKED_TARGET: Released lock channels.");
                                    }}
                                    className="w-full py-1.5 bg-black/40 hover:bg-green-950/20 border border-green-950/40 hover:border-green-600/40 rounded text-[8.5px] text-[#00FF41] tracking-wider uppercase flex items-center justify-center gap-1.5 select-none font-bold cursor-pointer"
                                  >
                                    <List className="w-3.5 h-3.5 text-green-500" />
                                    VIEW ALL ANOMALIES LIST
                                  </button>
                                </div>
                              );
                            })() : (
                              <div className="space-y-3 antialiased">
                                <h3 className="text-xs font-black text-[#00FF41] tracking-widest uppercase">
                                  ANOMALIES NEARBY
                                </h3>
                                
                                <p className="text-[8.5px] leading-normal text-green-700 font-sans tracking-wide mb-2">
                                  <strong className="text-white font-mono">{bioAnomalies.length}</strong> anomalies detected nearby. Select an anomaly from the list below to see more info:
                                </p>

                                {/* Anomalies interactive list layout */}
                                <div className="bg-neutral-950 border border-green-950/60 rounded p-1.5 space-y-1 max-h-[220px] overflow-y-auto custom-pox-scrollbar">
                                  {bioAnomalies.map((anom) => {
                                    const isHarvestable = anom.distance <= scanRadius;
                                    let indicatorColor = "bg-red-500 shadow-[0_0_6px_rgba(239,68,68,0.7)]";
                                    if (anom.faction === "Mech") indicatorColor = "bg-yellow-500 shadow-[0_0_6px_rgba(234,179,8,0.7)]";
                                    else if (anom.faction === "Parasite") indicatorColor = "bg-purple-500 shadow-[0_0_6px_rgba(168,85,247,0.7)]";
                                    else if (anom.faction === "Containment") indicatorColor = "bg-cyan-500 shadow-[0_0_6px_rgba(34,211,238,0.7)]";

                                    if (anom.harvested) {
                                      indicatorColor = "bg-neutral-600 shadow-none";
                                    }

                                    return (
                                      <button
                                        key={anom.id}
                                        type="button"
                                        onClick={() => {
                                          sound.playBeep(420 + anom.distance, 0.05, "sine");
                                          setSelectedAnomalyId(anom.id);
                                          addScannerLog(`LOCKED_TARGET: Coupled target ID ${anom.id}`);
                                        }}
                                        className="w-full text-left p-1.5 bg-black/40 hover:bg-green-950/25 border border-green-950/20 hover:border-green-800/40 rounded flex items-center justify-between text-[8px] cursor-pointer transition-all shrink-0 font-mono"
                                      >
                                        <div className="flex items-center gap-1.5">
                                          <div className={`w-1.5 h-1.5 rounded-full ${indicatorColor} shrink-0`} />
                                          <span className={`text-[8.5px] font-bold ${anom.harvested ? 'text-neutral-500 line-through' : 'text-neutral-200'}`}>
                                            ID: {anom.id}
                                          </span>
                                        </div>
                                        <span className="text-white text-[8px] font-bold">{Math.round(anom.distance)}ft {anom.direction}</span>
                                      </button>
                                    );
                                  })}
                                </div>
                                
                                <button
                                  type="button"
                                  onClick={() => {
                                    sound.playBeep(620, 0.1, "sine");
                                    triggerLog("Triggered Area Scanner frequency sweep lock array...", "info");
                                    addScannerLog("RE-CALIBRATE: Sync-net sweeps executed");
                                    // Slightly nudge anomalies
                                    setBioAnomalies(prev => prev.map(a => {
                                      const nextDist = Math.max(15, Math.min(95, a.distance + (Math.random() * 8) - 4));
                                      const rawAngle = Math.atan2(a.dy, a.dx) + (Math.random() * 0.1) - 0.05;
                                      const dx = nextDist * Math.cos(rawAngle);
                                      const dy = nextDist * Math.sin(rawAngle);
                                      const dLat = dy / 364000;
                                      const dLng = dx / 288000;

                                      return {
                                        ...a,
                                        distance: nextDist,
                                        dx,
                                        dy,
                                        lat: userCoords.lat + dLat,
                                        lng: userCoords.lng + dLng,
                                        signalStrength: Math.floor(98 - nextDist * 0.7),
                                      };
                                    }));
                                  }}
                                  className="w-full py-1.5 bg-black/40 hover:bg-green-950/20 border border-green-950/40 hover:border-green-600/40 rounded text-[8.5px] text-[#00FF41] tracking-wider uppercase flex items-center justify-center gap-1 select-none font-bold cursor-pointer"
                                >
                                  <RefreshCw className="w-3.5 h-3.5 text-green-500 animate-spin" style={{ animationDuration: '6s' }} />
                                  SCAN FOR ANOMALIES
                                </button>
                              </div>
                            )}
                          </div>
                        </div>
                      </div>

                      {/* Right Frame: Active Harvests + Telemetry receiver feed */}
                      <div className="bg-black/60 border border-green-900/40 p-4 rounded flex flex-col justify-between min-h-[420px] md:h-full text-left font-mono relative overflow-hidden shadow-[inset_0_1px_3px_rgba(0,0,0,0.6)]">
                        <div className="absolute inset-0 pointer-events-none bg-[radial-gradient(rgba(0,255,65,0.012)_1px,transparent_1px)] [background-size:12px_12px]" />
                        
                        <div className="z-10 flex flex-col h-full justify-between space-y-3.5">
                          {showingActiveHarvestsDetail ? (
                            <div className="flex-1 flex flex-col min-h-0 h-full justify-between">
                              <div className="flex-grow flex flex-col min-h-0">
                                <div className="flex justify-between items-center border-b border-green-900/40 pb-2 mb-2 select-none">
                                  <h3 className="text-xs font-black text-[#00FF41] tracking-widest uppercase flex items-center gap-1.5">
                                    <span className="w-1.5 h-1.5 bg-[#00FF41] rounded-full animate-ping" />
                                    DETAILED MONITORS ({harvestingMissions.filter(m => !m.isReturned).length})
                                  </h3>
                                  <button
                                    type="button"
                                    onClick={() => {
                                      sound.playBeep(450, 0.05, "sine");
                                      setShowingActiveHarvestsDetail(false);
                                    }}
                                    className="px-2 py-0.5 bg-neutral-900 hover:bg-neutral-800 border border-green-900/60 hover:border-[#00FF41] text-[#00FF41] hover:text-white rounded text-[8px] font-black uppercase tracking-wider cursor-pointer transition-all active:scale-95 flex items-center gap-1"
                                  >
                                    BACK
                                  </button>
                                </div>

                                <p className="text-[8.5px] leading-normal text-green-700 font-sans tracking-wide mb-3 select-none">
                                  FULL PROTOCOL TELEMETRY INTERFACE FEED:
                                </p>

                                {/* Detailed list - taking up the whole rest of space */}
                                <div className="flex-1 space-y-2.5 overflow-y-auto custom-pox-scrollbar pr-0.5 min-h-[180px] max-h-[220px]">
                                  {harvestingMissions.filter(m => !m.isReturned).map(m => {
                                    const remainingSec = Math.max(0, m.totalDuration - (m.elapsedSeconds || 0));
                                    const progressPercent = Math.min(100, Math.round(((m.elapsedSeconds || 0) / m.totalDuration) * 100));
                                    const remainingStr = `${Math.floor(remainingSec / 60)}m ${(remainingSec % 60).toString().padStart(2, "0")}s`;

                                    return (
                                      <div key={m.id} className="bg-neutral-900/80 border border-green-800/40 p-2.5 text-left rounded font-mono space-y-2 relative">
                                        <div className="flex items-center justify-between font-bold text-[9px] uppercase text-white pb-1 border-b border-green-950">
                                          <span className="text-amber-400 flex items-center gap-1.5">
                                            <span className="w-1.5 h-1.5 bg-amber-500 rounded-full animate-ping inline-block" />
                                            HARVEST ACTIVE
                                          </span>
                                          <span className="text-white font-extrabold">{m.creatureName}</span>
                                        </div>

                                        <div className="text-[8.5px] leading-relaxed text-zinc-400 grid grid-cols-2 gap-y-1 gap-x-2">
                                          <div>ANOMALY TARGET ID:</div>
                                          <div className="text-right text-white font-bold">{m.anomalyId}</div>
                                          <div>FLUID DISTANCE:</div>
                                          <div className="text-right text-white font-bold">{m.dispatchDistance.toFixed(1)} FT</div>
                                          <div>TIME REMAINING:</div>
                                          <div className="text-right text-white font-mono font-bold animate-pulse">{remainingStr}</div>
                                        </div>

                                        <div className="w-full bg-neutral-950 border border-green-950 h-1.5 rounded overflow-hidden">
                                          <div 
                                            className="bg-[#00E1FF] h-full transition-all duration-300" 
                                            style={{ width: `${progressPercent}%` }}
                                          />
                                        </div>

                                        <div className="bg-black/95 p-1 px-1.5 rounded border border-green-950 text-[8px] text-green-400 flex justify-between items-center">
                                          <span><b>GENE PACKETS ({m.harvestedGenes.length}/4):</b></span>
                                          <span className="text-white font-mono font-bold">
                                            {m.harvestedGenes.length > 0 
                                              ? m.harvestedGenes.join(", ") 
                                              : "INTEGRATION LINKING..."}
                                          </span>
                                        </div>

                                        {m.isCompleted && (
                                          <button
                                            type="button"
                                            onClick={() => handleRetrieveHarvestedGenes(m.id)}
                                            className="w-full py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white font-black text-[9px] uppercase rounded border border-emerald-400 cursor-pointer shadow-[0_0_10px_#10b981] transition-all flex items-center justify-center gap-1 animate-pulse"
                                          >
                                            <Check className="w-4 h-4 mr-1" />
                                            RECALL CYBORG &amp; BANK STOCKPILE
                                          </button>
                                        )}
                                      </div>
                                    );
                                  })}
                                </div>
                              </div>

                              <div className="pt-2 border-t border-green-950/20 text-center">
                                <button
                                  type="button"
                                  onClick={() => {
                                    sound.playBeep(450, 0.05, "sine");
                                    setShowingActiveHarvestsDetail(false);
                                  }}
                                  className="px-3.5 py-1.5 bg-neutral-950 hover:bg-neutral-800 border border-neutral-800 hover:border-green-800 text-neutral-400 hover:text-[#00FF41] rounded text-[8.5px] uppercase font-bold tracking-widest cursor-pointer transition-all active:scale-95 inline-block"
                                >
                                  ← RETURN TO SCAN FEED
                                </button>
                              </div>
                            </div>
                          ) : showingTelemetryLogsDetail ? (
                            <div className="flex-grow flex flex-col min-h-0 h-full">
                              <div className="flex justify-between items-center border-b border-green-900/40 pb-2 mb-2 select-none">
                                <h3 className="text-xs font-black text-[#00FF41] tracking-widest uppercase flex items-center gap-1.5">
                                  <span className="w-1.5 h-1.5 bg-[#00FF41] rounded-full animate-ping" />
                                  SCANCOM TELEMETRY FEED
                                </h3>
                                <button
                                  type="button"
                                  onClick={() => {
                                    sound.playBeep(450, 0.05, "sine");
                                    setShowingTelemetryLogsDetail(false);
                                  }}
                                  className="px-2 py-0.5 bg-neutral-900 hover:bg-neutral-800 border border-green-900/60 hover:border-[#00FF41] text-[#00FF41] hover:text-white rounded text-[8px] font-black uppercase tracking-wider cursor-pointer transition-all active:scale-95 flex items-center gap-1"
                                >
                                  BACK
                                </button>
                              </div>

                              <p className="text-[8.5px] leading-normal text-green-700 font-sans tracking-wide mb-3 select-none">
                                SECURED BI-DIRECTIONAL TERMINAL LINK FEED:
                              </p>

                              <div 
                                ref={scannerLogRef}
                                className="flex-1 overflow-y-auto space-y-1 bg-black/90 p-2.5 border border-green-950 rounded font-mono text-[8.5px] text-[#00FF41] custom-pox-scrollbar min-h-[180px] max-h-[220px]"
                              >
                                {scannerLogs.map((log, idx) => (
                                  <div key={idx} className="font-mono leading-normal select-all">
                                    {log}
                                  </div>
                                ))}
                              </div>

                              <div className="pt-2.5 border-t border-green-950/20 text-center mt-3">
                                <button
                                  type="button"
                                  onClick={() => {
                                    sound.playBeep(450, 0.05, "sine");
                                    setShowingTelemetryLogsDetail(false);
                                  }}
                                  className="px-3.5 py-1.5 bg-neutral-950 hover:bg-neutral-800 border border-neutral-800 hover:border-green-800 text-neutral-400 hover:text-[#00FF41] rounded text-[8.5px] uppercase font-bold tracking-widest cursor-pointer transition-all active:scale-95 inline-block"
                                >
                                  ← RETURN TO SCAN FEED
                                </button>
                              </div>
                            </div>
                          ) : (
                            <>
                              <div className="flex-1 flex flex-col min-h-0">
                                <h3 className="text-xs font-black text-[#00FF41] tracking-widest uppercase mb-1 flex items-center justify-between select-none">
                                  <span>ACTIVE HARVESTS</span>
                                  <span className="text-[7.5px] bg-[#00FF41]/10 text-[#00FF41] border border-green-900/40 px-1 rounded animate-pulse font-extrabold">
                                    {harvestingMissions.filter(m => !m.isReturned).length} ACTIVE
                                  </span>
                                </h3>
                                <p className="text-[8.5px] leading-normal text-green-700 font-sans tracking-wide mb-3 select-none">
                                  Real-time tracking of active anomaly harvests:
                                </p>

                                {/* Active Harvests list - Either shows a clean button shortcut, or empty slot */}
                                <div className="flex-1 pr-0.5 flex flex-col justify-center">
                                  {(() => {
                                    const activeMissions = harvestingMissions.filter(m => !m.isReturned);
                                    if (activeMissions.length === 0) {
                                      return (
                                        <div className="flex-grow flex flex-col items-center justify-center text-center p-2 border border-dashed border-green-950/40 bg-black/30 rounded my-1 select-none py-3">
                                          <h4 className="text-[8.5px] text-green-700 uppercase tracking-widest font-extrabold">NO ACTIVE HARVESTS</h4>
                                          <p className="text-[7.5px] text-green-800 max-w-[180px] mt-0.5 leading-normal">
                                            Select a sequence and press dispatch.
                                          </p>
                                        </div>
                                      );
                                    }

                                    // Display button option when active harvests exist to avoid cramming
                                    return (
                                      <div className="flex-grow flex flex-col items-center justify-center text-center p-3 border border-green-900/30 bg-[#00FF41]/5 rounded my-1 select-none min-h-[100px] space-y-1 font-mono">
                                        <div className="w-6 h-6 rounded-full bg-green-950/40 border border-green-500/40 flex items-center justify-center text-[#00FF41]">
                                          <Terminal className="w-3.5 h-3.5 animate-pulse text-emerald-400" />
                                        </div>
                                        <h4 className="text-[9px] text-white font-extrabold uppercase tracking-widest">
                                          {activeMissions.length} HARVEST PROCESS{activeMissions.length === 1 ? '' : 'ES'} ACTIVE
                                        </h4>
                                        <button
                                          type="button"
                                          onClick={() => {
                                            sound.playBeep(450, 0.05, "sine");
                                            setShowingActiveHarvestsDetail(true);
                                          }}
                                          className="px-3 py-1 bg-emerald-950 hover:bg-emerald-900 border border-emerald-500/80 hover:border-[#00FF41] text-[#00FF41] hover:text-white rounded text-[8px] font-black uppercase tracking-widest transition-all duration-150 active:scale-95 cursor-pointer shadow-[0_0_8px_rgba(0,128,128,0.2)]"
                                        >
                                          VIEW ACTIVE DETAILS ({activeMissions.length})
                                        </button>
                                      </div>
                                    );
                                  })()}
                                </div>
                              </div>

                              {/* Integrated Radar Status Feed */}
                              <div className="border-t border-green-950/40 pt-2.5 flex flex-col space-y-1.5 text-left select-none">
                                <div className="text-[7.5px] text-green-700 uppercase font-black tracking-widest flex items-center justify-between mb-0.5">
                                  <span>Anomaly Sweep Active</span>
                                  <span className="text-[7px] text-[#00FF41] font-mono font-bold animate-pulse">Scanner Active</span>
                                </div>
                                
                                <div className="grid grid-cols-2 gap-1.5 bg-neutral-950/65 p-1.5 rounded border border-green-950/25 text-[8px] font-mono leading-relaxed">
                                  <div className="text-neutral-400">MAXIMUM DIAMETER:</div>
                                  <div className="text-right text-[#00FF41] font-bold">120FT</div>
                                  <div className="text-neutral-400">MINIMUM DIAMETER:</div>
                                  <div className="text-right text-white font-bold">30FT</div>
                                </div>

                                {(() => {
                                  const hours = currentTime.getHours();
                                  const minutes = currentTime.getMinutes();
                                  const seconds = currentTime.getSeconds();
                                  const daySeconds = hours * 3600 + minutes * 60 + seconds;
                                  const dayFraction = daySeconds / 86400;
                                  const waveVal = Math.sin(dayFraction * 2 * Math.PI * 4); // -1 to 1
                                  const pct = Math.round((waveVal + 1) * 50); // 0 to 100
                                  const filled = Math.max(0, Math.min(10, Math.round(pct / 10)));
                                  const bar = "▰".repeat(filled) + "▱".repeat(10 - filled);
                                  
                                  // Determine if near peak, mid, or trough
                                  let waveStatus = "NEUTRAL WAVE";
                                  let waveColor = "text-yellow-500";
                                  if (waveVal > 0.8) {
                                    waveStatus = "MAX EXPANSION PEAK";
                                    waveColor = "text-emerald-400 font-extrabold animate-pulse";
                                  } else if (waveVal < -0.8) {
                                    waveStatus = "MIN CONTRACT TROUGH";
                                    waveColor = "text-red-400 font-extrabold";
                                  } else if (waveVal > 0) {
                                    waveStatus = "ASCENDING SEQUENCE";
                                    waveColor = "text-emerald-500";
                                  } else if (waveVal < 0) {
                                    waveStatus = "DESCENDING SEQUENCE";
                                    waveColor = "text-amber-500";
                                  }

                                  return (
                                    <div className="bg-black/85 p-1.5 rounded border border-green-950/40 space-y-1 text-[8px] font-mono text-left leading-normal">
                                      <div className="flex justify-between items-center text-[7.5px]">
                                        <span className="text-zinc-500 font-bold text-[7px]">TROUGH (30F)</span>
                                        <span className={`tracking-widest ${waveColor}`}>{bar} {pct}%</span>
                                        <span className="text-zinc-500 font-bold text-[7px]">PEAK (120F)</span>
                                      </div>
                                      <div className="flex justify-between items-center text-[7.5px] border-t border-green-950/20 pt-1">
                                        <span className="text-neutral-400">OSCILLATOR STATUS:</span>
                                        <span className={`${waveColor} uppercase font-bold text-[7px]`}>{waveStatus}</span>
                                      </div>
                                      <div className="text-[7.5px] text-[#00FF41] tracking-widest font-extrabold uppercase animate-pulse border border-[#00FF41]/20 bg-[#00FF41]/10 px-1 py-0.5 rounded text-center leading-none">
                                        FIXED RANGE ACTIVE: 60FT
                                      </div>
                                    </div>
                                  );
                                })()}
                              </div>

                              {/* Systems Telemetry Logs toggle button to reclaim vertical space */}
                              <div className="border-t border-green-950/40 pt-2.5 flex flex-col mt-auto select-none">
                                <div className="text-[7.5px] text-green-700 uppercase font-black tracking-widest flex items-center justify-between select-none mb-1.5">
                                  <span>Scanner Log Telemetry</span>
                                  <span className="text-[7px] text-red-400 animate-pulse uppercase font-mono font-bold">Warning: Telemetry Corruption</span>
                                </div>
                                <button
                                  type="button"
                                  onClick={() => {
                                    sound.playBeep(450, 0.05, "sine");
                                    setShowingTelemetryLogsDetail(true);
                                  }}
                                  className="w-full py-1.5 bg-black hover:bg-neutral-900 border border-green-950/60 hover:border-[#00FF41] text-green-500 hover:text-[#00FF41] rounded text-[8px] font-black uppercase tracking-widest transition-all duration-150 active:scale-[0.98] cursor-pointer flex items-center justify-center gap-1.5 animate-pulse"
                                >
                                  <Terminal className="w-3.5 h-3.5 text-emerald-400" />
                                  OPEN SCANCOM TELEMETRY LOGS ({scannerLogs.length})
                                </button>
                              </div>
                            </>
                          )}
                        </div>
                      </div>

                    </div>

                    {/* DYNAMIC MAP MINIMIZABLE RADAR WINDOW OVERLAY */}
                    <AnimatePresence>
                      {isMapPopupOpen && (
                        <motion.div
                          key="scanner-map-popup"
                          initial={{ opacity: 0, scale: 0.98 }}
                          animate={{ opacity: 1, scale: 1 }}
                          exit={{ opacity: 0, scale: 0.98 }}
                          className="absolute inset-0 bg-[#020502]/98 border border-[#00FF41]/60 rounded p-4 flex flex-col z-[3000] overflow-hidden font-mono text-left shadow-[0_0_30px_rgba(0,255,65,0.25)]"
                        >
                          {/* Map Header */}
                          <div className="flex justify-between items-center border-b border-green-900 pb-2 mb-2 select-none">
                            <span className="text-[10px] text-[#00FF41] font-bold tracking-widest uppercase flex items-center gap-1.5 font-mono">
                              <span className="w-1.5 h-1.5 bg-green-500 rounded-full animate-ping" />
                              [ LOCAL MAP FEED &bull; TACTICAL OVERLAY RADAR ]
                            </span>
                            <button
                              type="button"
                              onClick={() => {
                                sound.playBeep(440, 0.05, "sine");
                                setIsMapPopupOpen(false);
                                addScannerLog("TACTICAL_MAP: Minimized coordinates map radar.");
                              }}
                              className="px-2.5 py-1 bg-red-950/80 hover:bg-red-900 border border-red-800 text-red-500 hover:text-white rounded text-[8.5px] cursor-pointer font-bold tracking-wider transition-all duration-150 active:scale-95 uppercase flex items-center gap-1"
                            >
                              ✕ MINIMIZE MAP OVERLAY
                            </button>
                          </div>

                          <div className="text-[7.5px] text-green-700 uppercase tracking-widest mb-2 border-b border-green-950/50 pb-1 flex justify-between select-none">
                            <span>SATELLITE BEACON COUPLING TRANSCEIVER: CONNECTED</span>
                            <span>ACTIVE RANGE RADIUS: {scanRadius.toFixed(1)} FT</span>
                          </div>

                          {/* Map Viewport Area */}
                          <div className="flex-1 w-full bg-black/80 rounded border border-green-950/60 overflow-hidden relative min-h-[350px] sm:min-h-[400px] flex items-center justify-center shadow-[inset_0_1px_5px_rgba(0,0,0,0.8)]">
                            <LeafletScannerMap
                              userCoords={userCoords}
                              scanRadius={scanRadius}
                              bioAnomalies={bioAnomalies}
                              selectedAnomalyId={selectedAnomalyId}
                              onSelectAnomaly={(id) => {
                                sound.playBeep(420, 0.05, "sine");
                                setSelectedAnomalyId(id);
                                addScannerLog(`MAP LOCK: Targeted node specimen ID ${id}`);
                              }}
                              harvestingMissions={harvestingMissions}
                              onMapTapHeatZone={(anomalyId, lat, lng, dist) => {
                                handleMapTapHeatZone(anomalyId, lat, lng, dist);
                                addScannerLog(`MAP TAPPED: Fine extract coordinates locked to ${dist.toFixed(1)}ft from epicenter`);
                              }}
                            />
                            
                            {/* Overlay tag for Geolocation / Fallback Status */}
                            <div className="absolute top-2 left-2 z-[400] bg-black/85 border border-green-950/80 px-2 py-1 rounded text-[7px] font-mono leading-none flex items-center gap-1.5 select-none pointer-events-none">
                              {geoError ? (
                                <>
                                  <span className="w-1.5 h-1.5 bg-yellow-500 rounded-full animate-pulse" />
                                  <span className="text-yellow-500 uppercase font-bold">MUTATION FEED: SIMULATOR (IFRAME SECURE COMS)</span>
                                </>
                              ) : (
                                <>
                                  <span className="w-1.5 h-1.5 bg-[#00FF41] rounded-full animate-ping" />
                                  <span className="text-[#00FF41] uppercase font-bold">MUTATION FEED: LIVE GPS BEACON RESOLVED</span>
                                </>
                              )}
                            </div>
                          </div>

                          {/* Map footer elements */}
                          <div className="mt-3 pt-2 border-t border-green-950/45 flex flex-col sm:flex-row items-center justify-between gap-3 text-[8px] text-neutral-500 select-none">
                            <div className="flex flex-wrap items-center gap-1.5 font-mono">
                              <span className="text-[#00FF41] font-bold uppercase">BEACON STATUS:</span>
                              <span className="text-white bg-green-950 px-1 border border-green-800 rounded animate-pulse">
                                COUPLING CHANNELS SAFE
                              </span>
                              <span>P2P RANGE LINK: {scanRadius} FT</span>
                            </div>
                            
                            <div className="text-green-700 text-right uppercase">
                              Click any hot zones circles above to adjust extractor pinpoints.
                            </div>
                          </div>
                        </motion.div>
                      )}
                    </AnimatePresence>

                  </motion.div>
                )}

                {activeTab === 'forecast' && (
                  <motion.div 
                    key="forecast"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    className="h-full min-h-[500px]"
                  >
                    <ForecastCalendar 
                      onPlayBeep={(f, d, t) => sound.playBeep(f, d, t)}  
                      targetSequence={targetSequence} 
                      gameTime={currentTime}
                    />
                  </motion.div>
                )}

                {activeTab === 'inventory' && (
                  <motion.div 
                    key="inventory"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    className="h-full flex flex-col p-6 bg-neutral-950/45 border border-green-900/40 rounded min-h-[500px] relative overflow-hidden text-left"
                  >
                    {/* Retro background decorative scan lines and vector accents */}
                    <div className="absolute inset-0 pointer-events-none bg-[radial-gradient(rgba(0,255,65,0.015)_1px,transparent_1px)] [background-size:16px_16px]" />
                    <div className="absolute top-2 left-2 text-[8px] text-green-700 font-mono select-none">
                      AUXILIARY HARDWARE INVENTORY LOCKER [CHANNEL_B]
                    </div>
                    <div className="absolute bottom-2 right-2 text-[8px] text-[#00FF41] font-mono select-none animate-pulse">
                      SECTOR_STATUS: ACTIVE // MODULE_0x0DF0F41A_ONLINE
                    </div>

                    <div className="z-10 w-full flex flex-col h-full justify-between space-y-4">
                      {/* Header */}
                      <div className="border-b border-green-900/40 pb-2 flex justify-between items-center font-mono">
                        <div>
                          <h2 className="text-xs sm:text-xs font-bold text-white tracking-widest uppercase">
                            🔬 STORAGE STORAGE DIRECTORY
                          </h2>
                          <p className="text-[8.5px] text-green-600 block mt-0.5">BETA FIRMWARE EXPANSION ONLINE</p>
                        </div>
                        <span className="text-[8px] bg-emerald-950 border border-emerald-800 text-[#00FF41] px-1.5 py-0.5 rounded font-bold uppercase animate-pulse">
                          STREAM CH_B SECURE
                        </span>
                      </div>

                      {/* Items grid */}
                      <div className="flex-1 overflow-y-auto custom-pox-scrollbar space-y-4 pr-1">
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
                          {/* Item Card: Temporary Reactor Catalysis Boost */}
                          <div className="bg-black/60 border border-amber-900/40 hover:border-amber-500/80 p-4 rounded-md font-mono flex flex-col justify-between space-y-3.5 transition-all text-left">
                            <div className="space-y-1.5">
                              <div className="flex justify-between items-start">
                                <span className="text-[9px] text-amber-500 font-bold bg-amber-950/40 border border-amber-900/40 px-1.5 py-0.5 rounded">
                                  ACTIVE CATALYST
                                </span>
                                <span className="text-[8px] text-white/55 bg-neutral-900 px-1.5 py-0.5 rounded uppercase font-bold border border-neutral-800">
                                  INFINITE USES
                                </span>
                              </div>
                              <h3 className="text-white font-black text-xs tracking-wider uppercase">
                                TEMPORARY REACTOR CATALYSIS BOOST
                              </h3>
                              <p className="text-[9.5px]/relaxed text-neutral-400">
                                Synthetically increases thermal circulation within the Bio-Lab Reactor, forcing rapid sequence condensation. Reduces the baseline tick rate of the reactor from <strong>16 seconds to 8 seconds</strong> for exactly <strong>5 minutes (300s)</strong>.
                              </p>
                            </div>

                            <div className="space-y-2 text-center">
                              {boostSecondsLeft > 0 ? (
                                <div className="bg-emerald-950/30 border border-emerald-500/40 p-2.5 rounded text-center space-y-1">
                                  <span className="text-[7.5px] text-[#00FF41] block font-bold tracking-widest uppercase select-none animate-pulse">
                                    [ TRANSMUTATION IN PROCESS ]
                                  </span>
                                  <span className="text-white font-extrabold text-sm font-mono tracking-widest block">
                                    {Math.floor(boostSecondsLeft / 60)}:{(boostSecondsLeft % 60).toString().padStart(2, '0')}
                                  </span>
                                </div>
                              ) : (
                                <button
                                  type="button"
                                  onClick={() => {
                                    sound.playSynthesisSuccess();
                                    sound.playBeep(880, 0.45, "triangle");
                                    setBoostSecondsLeft(300);
                                    // Clamp current tick if it is larger than 8
                                    setIdleTime(prev => Math.min(prev, 8));
                                    triggerLog("BIO-LAB ACTIVATION: Temporary Reactor Catalysis Boost activated. Reactor tick speed boosted to 8s/cycle for 5 minutes.", "success");
                                  }}
                                  className="w-full py-2 bg-amber-600 hover:bg-amber-550 border border-amber-500 text-black font-black text-[10px] uppercase tracking-wider rounded-sm transition-all shadow-[0_0_10px_rgba(245,158,11,0.2)] cursor-pointer hover:scale-[1.01] active:scale-95"
                                >
                                  ⚡ POWER ON INJECTOR
                                </button>
                              )}
                            </div>
                          </div>

                          {/* Placeholder Item Card for Vault Expansion */}
                          <div className="bg-neutral-950/40 border border-neutral-900 border-dashed p-4 rounded-md font-mono flex flex-col justify-center items-center text-center space-y-2 select-none opacity-60 min-h-[160px]">
                            <span className="text-[14px]">⬡</span>
                            <div className="space-y-0.5">
                              <h4 className="text-[9.5px] text-neutral-500 uppercase tracking-widest font-bold">UNREACHABLE SLOT 0x02</h4>
                              <p className="text-[8px] text-neutral-600 max-w-xs leading-normal">
                                Connect high-frequency cybernetic specimen to unlock advanced sub-tier inventory directory channels.
                              </p>
                            </div>
                          </div>
                        </div>
                      </div>

                      {/* Footer telemetry */}
                      <div className="border-t border-green-950/40 pt-2 flex justify-between text-[8px] text-neutral-500 font-mono">
                        <span>SLOTS IN USE: 1 / 24 BAYS</span>
                        <span>SIGNAL COMPATIBILITY: STABLE (100%)</span>
                        <span>VAULT_ALLOCATION: SECURE</span>
                      </div>
                    </div>
                  </motion.div>
                )}

                {activeTab === 'nodes' && (
                  <motion.div 
                    key="nodes"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    className="flex flex-col h-full overflow-hidden text-left"
                  >
                    {/* G.E.N. NETWORK SUB-TABS SELECTOR */}
                    <div className="flex border-b border-green-950/45 text-[10.5px] font-mono tracking-wider font-bold mb-4 select-none shrink-0 gap-1">
                      <button
                        type="button"
                        onClick={() => {
                          sound.playBeep(440, 0.05, "sine");
                          setGenNetworkSubTab('mail');
                          setActiveTab('gen_network');
                        }}
                        className={`px-4 py-2 border-t-2 transition-all cursor-pointer flex items-center gap-1.5 uppercase ${
                          activeTab === 'gen_network'
                            ? 'border-green-500 bg-green-950/20 text-white font-extrabold shadow-[0_-2px_6px_rgba(0,255,65,0.05)]'
                            : 'border-transparent text-green-700 hover:text-green-400 hover:bg-green-950/5'
                        }`}
                      >
                        G.E.N. Mail
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          sound.playBeep(440, 0.05, "sine");
                          setGenNetworkSubTab('nodes');
                          setActiveTab('nodes');
                        }}
                        className={`px-4 py-2 border-t-2 transition-all cursor-pointer flex items-center gap-1.5 uppercase ${
                          activeTab === 'nodes'
                            ? 'border-green-500 bg-green-950/20 text-white font-extrabold shadow-[0_-2px_6px_rgba(0,255,65,0.05)]'
                            : 'border-transparent text-green-700 hover:text-green-400 hover:bg-green-950/5'
                        }`}
                      >
                        Static Nodes
                      </button>
                    </div>

                    <div className="flex-grow overflow-y-auto h-full p-6 bg-neutral-950/45 border border-green-900/40 rounded min-h-[500px] relative text-left font-mono">
                      <div className="absolute inset-0 pointer-events-none bg-[radial-gradient(rgba(0,255,65,0.015)_1px,transparent_1px)] [background-size:16px_16px]" />

                      <div className="z-10 w-full flex flex-col h-full justify-between space-y-4">
                      {/* Header */}
                      <div className="border-b border-green-900/40 pb-2 flex justify-between items-center">
                        <div>
                          <h2 className="text-xs sm:text-xs font-bold text-white tracking-widest uppercase">
                            STATIC NODE MANAGEMENT
                          </h2>
                        </div>
                        <span className="text-[8px] bg-emerald-950 border border-emerald-800 text-[#00FF41] px-1.5 py-0.5 rounded font-bold uppercase animate-pulse">
                          P2P LINK SECURE
                        </span>
                      </div>

                      {/* Sub tab selectors */}
                      <div className="flex border-b border-green-950/45 text-[9px] font-mono tracking-wider font-bold">
                        <button
                          type="button"
                          onClick={() => {
                            sound.playBeep(440, 0.05, "sine");
                            setNodesSubTab('qr_trade');
                          }}
                          className={`px-3.5 py-1.5 border-t-2 transition-all cursor-pointer ${
                            nodesSubTab === 'qr_trade'
                              ? 'border-green-500 bg-green-950/20 text-white font-black'
                              : 'border-transparent text-green-700 hover:text-green-400 hover:bg-green-950/5'
                          }`}
                        >
                          SEQUENCE TRANSFER
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            sound.playBeep(440, 0.05, "sine");
                            setNodesSubTab('friends');
                          }}
                          className={`px-3.5 py-1.5 border-t-2 transition-all cursor-pointer ${
                            nodesSubTab === 'friends'
                              ? 'border-green-500 bg-green-950/20 text-white font-black'
                              : 'border-transparent text-green-700 hover:text-green-400 hover:bg-green-950/5'
                          }`}
                        >
                          STATIC NODES REGISTRY
                        </button>
                      </div>

                      {/* Content Split: Left QR / Right Friends */}
                      {nodesSubTab === 'friends' && (
                        <div className="flex-1 grid grid-cols-1 md:grid-cols-5 gap-6 items-stretch my-2">
                        {/* Left Column: QR Code & ID ID card */}
                        <div className="md:col-span-2 bg-black/60 border border-green-955/40 p-4 rounded flex flex-col justify-between space-y-4 text-center">
                          <div className="space-y-2">
                            <span className="text-[9px] text-emerald-400 font-extrabold uppercase tracking-widest block bg-emerald-950/30 border border-emerald-900/40 py-1 px-2 rounded">
                              ● NODE SIGNATURE
                            </span>
                            <p className="text-[8px] text-neutral-450 leading-relaxed max-w-[220px] mx-auto">
                              Scan signature transmission below to add this node to your Static Nodes registry.
                            </p>
                          </div>

                          {/* Deterministic QR Code visual - REPLACED with warning and Tap to reveal */}
                          <div 
                            onClick={() => {
                              sound.playBeep(600, 0.05, "sine");
                              setIsOperatorQrEnlarged(true);
                            }}
                            className="mx-auto bg-neutral-950/90 border border-amber-900/40 p-2.5 rounded-md w-[140px] h-[140px] flex flex-col items-center justify-center shadow-[inset_0_0_10px_rgba(245,158,11,0.05)] relative group hover:border-amber-500/60 transition-all cursor-pointer select-none"
                            title="Tap to reveal official QR payload"
                          >
                            {/* Decorative Corner Vector Reticles in Amber */}
                            <div className="absolute top-1 left-1 w-1.5 h-1.5 border-t border-l border-amber-500/60" />
                            <div className="absolute top-1 right-1 w-1.5 h-1.5 border-t border-r border-amber-500/60" />
                            <div className="absolute bottom-1 left-1 w-1.5 h-1.5 border-b border-l border-amber-500/60" />
                            <div className="absolute bottom-1 right-1 w-1.5 h-1.5 border-b border-r border-amber-500/60" />

                            <AlertTriangle className="w-5 h-5 text-amber-500 animate-pulse mb-1" />
                            <span className="text-[7.5px] text-amber-500 font-extrabold uppercase tracking-widest text-center mb-1">
                              TELOMERE LOSS
                            </span>
                            <p className="text-[6.5px] text-amber-400/80 leading-snug text-center max-w-[124px]">
                              Scanning sequences directly causes telomere length degradation & sequential structural instability.
                            </p>
                            <span className="mt-1 text-[6.5px] text-[#00FF41]/80 group-hover:text-white font-bold tracking-widest uppercase bg-[#00FF41]/10 px-1 py-0.5 border border-[#00FF41]/20 rounded group-hover:bg-[#00FF41]/20 transition-all">
                              TAP TO REVEAL
                            </span>
                          </div>

                          {/* Profile Details ID card */}
                          <div className="space-y-1.5 p-2 bg-black/40 border border-green-955/40 rounded leading-tight">
                            <div className="text-[10px] text-white font-extrabold tracking-widest uppercase">
                              OPERATOR: <span className="text-emerald-400 select-all">"OPERATOR_GEN_09"</span>
                            </div>
                            <div className="text-[8.5px] text-neutral-500 font-mono break-all select-all">
                              UNIQUE SIGNATURE ID: <span className="text-neutral-400 select-all">NODE-0DF0F41A-F05B-442B</span>
                            </div>
                          </div>

                          <button
                            type="button"
                            onClick={() => {
                              sound.playSynthesisSuccess();
                              navigator.clipboard.writeText("OPERATOR_GEN_09#NODE-0DF0F41A-F05B-442B");
                              triggerLog("My Node Signature copied to system clipboard.", "success");
                            }}
                            className="py-1.5 bg-green-950/40 border border-green-700/60 text-green-400 hover:text-white hover:bg-green-900/30 text-[8.5px] uppercase rounded transition-all cursor-pointer font-bold tracking-wider"
                          >
                            COPY NODE SIGNATURE
                          </button>
                        </div>

                        {/* Right Column: Friends List */}
                        <div className="md:col-span-3 bg-neutral-950/60 border border-green-955/40 p-4 rounded flex flex-col justify-between text-left">
                          <div className="space-y-3 flex flex-col">
                            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                              <span className="text-[9px] text-blue-400 font-extrabold uppercase tracking-widest flex items-center gap-1.5 bg-blue-950/15 border border-blue-900/30 py-1 px-2 rounded">
                                <Dna className="w-3.5 h-3.5 text-blue-400" />
                                <span>REGISTERED STATIC NODES ({registeredNodes.length})</span>
                              </span>
                              <button
                                type="button"
                                onClick={() => {
                                  sound.playBeep(500, 0.05, "sine");
                                  setIsAddNodeOpen(!isAddNodeOpen);
                                  if (!isAddNodeOpen) {
                                    const radNames = ["RECEPTOR_EPSILON", "KRONOS_GATEWAY", "CYBER_STATION_D4", "ORBIT_LINK_99", "OMNIPRESENCE_X"];
                                    const rName = radNames[Math.floor(Math.random() * radNames.length)] + "_" + Math.floor(Math.random() * 100);
                                    const rId = "NODE-" + Math.random().toString(36).substring(2, 6).toUpperCase() + "-" + Math.random().toString(36).substring(2, 6).toUpperCase() + "-" + Math.random().toString(36).substring(2, 6).toUpperCase();
                                    setNewNodeName(rName);
                                    setNewNodeId(rId);
                                  }
                                }}
                                className="px-2 py-1 bg-blue-950 hover:bg-blue-900 text-blue-400 hover:text-white border border-blue-800 hover:border-blue-500 rounded text-[8.5px] font-bold uppercase tracking-wider transition-all duration-150 cursor-pointer active:scale-95 flex items-center gap-1"
                              >
                                {isAddNodeOpen ? "✕ CANCEL" : "✚ ADD STATIC NODE"}
                              </button>
                            </div>

                            {/* Add Static Node Form */}
                            {isAddNodeOpen && (
                              <div className="p-3 bg-blue-950/10 border border-blue-500/20 rounded-md space-y-3 relative text-[9px]">
                                <div className="absolute top-1 right-2">
                                  <span className="text-[6.5px] text-blue-400 font-mono">P2P_PROT_v4.2</span>
                                </div>
                                <h4 className="text-[9.5px] font-bold text-blue-400 uppercase tracking-widest border-b border-blue-500/10 pb-1">
                                  REGISTER NEW P2P STATION
                                </h4>
                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                                  <div className="space-y-1">
                                    <label className="text-[7.5px] text-neutral-400 font-bold uppercase block">Node Operator Name:</label>
                                    <input
                                      type="text"
                                      value={newNodeName}
                                      onChange={(e) => setNewNodeName(e.target.value.replace(/[^a-zA-Z0-9_-]/g, ''))}
                                      placeholder="e.g. DELTA_STATION_4"
                                      className="w-full px-2 py-1 bg-black text-[#00FF41] border border-blue-950 rounded font-mono text-[9px] focus:outline-none focus:border-blue-500 font-bold"
                                    />
                                  </div>
                                  <div className="space-y-1">
                                    <label className="text-[7.5px] text-neutral-400 font-bold uppercase block">Signature ID Key:</label>
                                    <input
                                      type="text"
                                      value={newNodeId}
                                      onChange={(e) => setNewNodeId(e.target.value)}
                                      placeholder="NODE-XXXX-XXXX-XXXX"
                                      className="w-full px-2 py-1 bg-black text-[#00FF41] border border-blue-950 rounded font-mono text-[9px] focus:outline-none focus:border-blue-500 font-bold select-all"
                                    />
                                  </div>
                                </div>

                                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 pt-1 border-t border-blue-500/5">
                                  <button
                                    type="button"
                                    onClick={() => {
                                      sound.playBeep(420, 0.05, "sine");
                                      const radNames = ["GAMMA_UNIT", "TRINITY_BRIDGE", "SPECTRE_NODE_06", "ZEPHYR_REACTION", "NEXUS_MATRIX"];
                                      const rName = radNames[Math.floor(Math.random() * radNames.length)] + "_" + Math.floor(Math.random() * 100);
                                      const rId = "NODE-" + Math.random().toString(36).substring(2, 6).toUpperCase() + "-" + Math.random().toString(36).substring(2, 6).toUpperCase() + "-" + Math.random().toString(36).substring(2, 6).toUpperCase();
                                      setNewNodeName(rName);
                                      setNewNodeId(rId);
                                    }}
                                    className="text-[7.5px] text-blue-500 hover:text-blue-300 font-bold uppercase tracking-wider text-left"
                                  >
                                    🔄 RE-GENERATE KEYS
                                  </button>

                                  <button
                                    type="button"
                                    onClick={() => {
                                      if (!newNodeName.trim()) {
                                        sound.playBeep(300, 0.15, "sawtooth");
                                        triggerLog("Node registration failed: Name cannot be empty.", "warn");
                                        return;
                                      }
                                      if (!newNodeId.trim()) {
                                        sound.playBeep(300, 0.15, "sawtooth");
                                        triggerLog("Node registration failed: Signature ID cannot be empty.", "warn");
                                        return;
                                      }
                                      
                                      if (registeredNodes.some(n => n.id.toLowerCase() === newNodeId.trim().toLowerCase())) {
                                        sound.playBeep(300, 0.15, "sawtooth");
                                        triggerLog("Node signature is already registered.", "warn");
                                        return;
                                      }

                                      const ips = ["10.0.8.12", "192.168.1.55", "104.12.4.99", "18.232.14.7", "10.0.112.4"];
                                      const rIp = ips[Math.floor(Math.random() * ips.length)];
                                      const statuses: ('ONLINE' | 'STANDBY')[] = ["ONLINE", "STANDBY"];
                                      const rStatus = statuses[Math.floor(Math.random() * statuses.length)];
                                      const rEmis = Math.floor(Math.random() * 30) + 70;

                                      const created = {
                                        id: newNodeId.trim().toUpperCase(),
                                        name: newNodeName.trim().toUpperCase(),
                                        assignedIp: rIp,
                                        status: rStatus,
                                        emissivity: rEmis,
                                        addedAt: new Date().toLocaleString()
                                      };

                                      sound.playSynthesisSuccess();
                                      setRegisteredNodes(prev => [created, ...prev]);
                                      setIsAddNodeOpen(false);
                                      triggerLog(`Registered static node "${created.name}" successfully to local registry.`, "success");
                                      addScannerLog(`P2P LINKED: Established connection with peer [${created.name}]`);
                                    }}
                                    className="px-3 py-1.5 bg-blue-600 hover:bg-blue-500 text-white font-bold uppercase rounded cursor-pointer transition-transform active:scale-95 text-[8.5px] tracking-wider shadow-[0_0_8px_rgba(37,99,235,0.4)] flex items-center justify-center"
                                  >
                                    ✚ CONNECT P2P LINK
                                  </button>
                                </div>
                              </div>
                            )}
                          </div>

                          {/* Registered Nodes List */}
                          {registeredNodes.length === 0 ? (
                            <div className="flex-1 flex flex-col items-center justify-center text-center p-6 border border-dashed border-neutral-900 rounded my-3 select-none">
                              {/* Simple beautiful radar scope skeleton outline */}
                              <div className="w-14 h-14 rounded-full border border-dashed border-neutral-800 flex items-center justify-center animate-spin [animation-duration:12s] mb-3">
                                <div className="w-8 h-8 rounded-full border border-neutral-800 flex items-center justify-center">
                                  <div className="w-2 h-2 rounded-full bg-neutral-800" />
                                </div>
                              </div>
                              <h4 className="text-[9.5px]/snug text-neutral-500 uppercase tracking-widest font-black font-mono">● NO ACTIVE NODES DETECTED</h4>
                              <p className="text-[8px] text-neutral-600 max-w-xs mt-1 leading-normal text-center">
                                Static node registry empty. Scan or manually enter a node signature to add the node to the registry.
                              </p>
                            </div>
                          ) : (
                            <div className="flex-1 my-3 space-y-2 overflow-y-auto pr-1 max-h-[300px] scrollbar-thin">
                              {registeredNodes.map((node) => {
                                return (
                                  <div 
                                    key={node.id} 
                                    className="bg-black/50 border border-neutral-900 hover:border-blue-900/40 p-2.5 rounded flex items-center justify-between gap-3 transition-colors text-left"
                                  >
                                    <div className="flex-grow min-w-0 space-y-1">
                                      <div className="flex items-center gap-1.5 min-w-0">
                                        <span className={`w-1.5 h-1.5 rounded-full flex-shrink-0 ${
                                          node.status === "ONLINE" 
                                            ? "bg-[#00FF41] animate-pulse" 
                                            : node.status === "STANDBY" 
                                              ? "bg-amber-400" 
                                              : "bg-neutral-600"
                                        }`} />
                                        <span className="text-white font-bold text-[10px] uppercase truncate tracking-wide">{node.name}</span>
                                      </div>
                                      <div className="flex flex-col sm:flex-row sm:items-center text-[7.5px] text-neutral-450 gap-y-0.5 gap-x-3 font-mono">
                                        <span className="text-blue-400 select-all font-semibold font-mono">{node.id}</span>
                                        {node.assignedIp && <span>IP: <span className="select-all text-neutral-300 font-mono">{node.assignedIp}</span></span>}
                                        <span>EMISSIVITY: <span className="text-[#00FF41] font-mono">{node.emissivity}%</span></span>
                                      </div>
                                    </div>

                                    <div className="flex items-center gap-1.5 flex-shrink-0">
                                      <button
                                        type="button"
                                        onClick={() => {
                                          sound.playBeep(440, 0.05, "sine");
                                          navigator.clipboard.writeText(`${node.name}#${node.id}`);
                                          triggerLog(`Node signature "${node.name}" copied.`, "success");
                                        }}
                                        className="p-1 bg-neutral-900 hover:bg-neutral-800 border border-neutral-800 hover:border-neutral-700 rounded text-[7.5px] font-black uppercase text-neutral-300 cursor-pointer active:scale-95"
                                        title="Copy Credentials"
                                      >
                                        COPY
                                      </button>
                                      <button
                                        type="button"
                                        onClick={() => {
                                          sound.playBeep(320, 0.1, "sawtooth");
                                          setRegisteredNodes(prev => prev.filter(n => n.id !== node.id));
                                          triggerLog(`Severed link with node "${node.name}".`, "warn");
                                          addScannerLog(`P2P SEVERED: Terminated link with [${node.name}]`);
                                        }}
                                        className="p-1 bg-red-950/25 hover:bg-red-900/60 border border-red-900/45 hover:border-red-500 rounded text-[7.5px] text-red-400 hover:text-white cursor-pointer active:scale-95 uppercase transition-colors"
                                        title="Sever P2P Link"
                                      >
                                        SEVER
                                      </button>
                                    </div>
                                  </div>
                                );
                              })}
                            </div>
                          )}

                          <div className="border-t border-green-950/40 pt-2 text-[8px] text-neutral-650 flex justify-between select-none">
                            <span>REGISTRY_CAP: {registeredNodes.length} / 128 NODES</span>
                          </div>
                        </div>
                      </div>
                      )}

                      {nodesSubTab === 'qr_trade' && (
                        <div className="flex-1 grid grid-cols-1 lg:grid-cols-5 gap-6 items-stretch my-2 font-mono">
                          {/* Column 1: Generate Trade QR (To Send) */}
                          <div className="lg:col-span-3 bg-black/60 border border-green-955/45 p-4 rounded flex flex-col justify-between space-y-4">
                            <div className="space-y-3.5">
                              <span className="text-[9px] text-[#00FF41] font-extrabold uppercase tracking-widest block bg-green-950/30 border border-green-900/40 py-1 px-2 rounded">
                                SELECT & SEND P.O.X. SEQUENCE
                              </span>

                              {/* Specimen selector */}
                              <div className="space-y-2 text-left bg-neutral-950/45 p-3.5 border border-green-950 rounded-sm">
                                <div className="flex justify-between items-center">
                                  <label className="text-[8.5px] text-[#00FF41] font-bold uppercase tracking-wider block">
                                    Selected P.O.X. Sequence:
                                  </label>
                                  {tradeSelectedCreatureId && (
                                    <button
                                      type="button"
                                      onClick={() => {
                                        sound.playBeep(440, 0.05, "sine");
                                        setTradeSelectedCreatureId(null);
                                      }}
                                      className="text-[7.5px] text-red-400 hover:text-red-300 font-bold uppercase transition-transform active:scale-95 cursor-pointer"
                                    >
                                      ✕ CLEAR
                                    </button>
                                  )}
                                </div>
                                
                                {tradeSelectedCreatureId ? (() => {
                                  const selected = creatures.find(c => c.id === tradeSelectedCreatureId);
                                  return (
                                    <div className="flex justify-between items-center bg-black/50 p-2.5 border border-green-950/60 rounded-sm">
                                      <div>
                                        <span className="text-white text-xs font-bold uppercase">{selected?.name || "Unknown"}</span>
                                        <p className="text-[8px] text-neutral-400 font-mono">TYPE: {selected?.type} • ID: {selected?.id}</p>
                                      </div>
                                      <button
                                        type="button"
                                        onClick={() => {
                                          sound.playBeep(440, 0.05, "sine");
                                          setCreatureCardOpenedFrom('Trade');
                                          setActiveTab('library');
                                        }}
                                        className="px-2 py-1 bg-green-950 hover:bg-green-900 border border-green-700 hover:border-green-400 text-[8px] font-bold text-white uppercase rounded transition-all cursor-pointer"
                                      >
                                        CHANGE SPECIMEN
                                      </button>
                                    </div>
                                  );
                                })() : (
                                  <div className="flex flex-col items-center justify-center p-4 border border-dashed border-green-900/30 bg-black/20 rounded gap-2 text-center select-none">
                                    <span className="text-[9px] text-green-700 font-mono">NO SEQUENCE SELECTED FOR TRANSFER</span>
                                    <button
                                      type="button"
                                      onClick={() => {
                                        sound.playBeep(440, 0.05, "sine");
                                        setCreatureCardOpenedFrom('Trade');
                                        setActiveTab('library');
                                      }}
                                      className="px-4 py-1.5 bg-[#00FF41]/10 border border-[#00FF41] hover:bg-[#00FF41]/20 font-bold uppercase cursor-pointer transition-all text-[#00FF41] text-[9px] tracking-wider rounded flex items-center justify-center gap-1.5 mx-auto"
                                    >
                                      <Search className="w-3.5 h-3.5" />
                                      SELECT FROM GEN-VAULT DATA
                                    </button>
                                  </div>
                                )}
                              </div>

                              {tradeSelectedCreatureId && (() => {
                                const selected = creatures.find(c => c.id === tradeSelectedCreatureId);
                                if (!selected) return null;
                                const tradeUrl = window.location.origin + window.location.pathname + "?trade=" + encodeCreatureToBase64(selected);

                                return (
                                  <div className="bg-neutral-955 p-4 border border-green-900/45 rounded flex flex-col md:flex-row items-center gap-4 text-left">
                                    {/* QR Code */}
                                    <div 
                                      onClick={() => {
                                        sound.playBeep(520, 0.05, "sine");
                                        setIsTradeQrEnlarged(true);
                                      }}
                                      className="bg-white p-2.5 rounded shadow-[0_0_12px_rgba(0,255,65,0.15)] flex-shrink-0 cursor-pointer hover:scale-105 hover:shadow-[0_0_20px_rgba(0,255,65,0.3)] transition-all relative group"
                                      title="Click to embiggen QR code"
                                    >
                                      <QRCodeSVG
                                        value={tradeUrl}
                                        size={120}
                                        bgColor="#FFFFFF"
                                        fgColor="#000000"
                                        level="M"
                                        includeMargin={false}
                                      />
                                      <div className="absolute inset-0 bg-black/10 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity rounded">
                                        <span className="text-[7.5px] bg-[#00FF41] border border-black/20 text-black px-1.5 py-0.5 rounded font-black uppercase tracking-wider animate-pulse shadow-md">
                                          🔍 EMBIGGEN
                                        </span>
                                      </div>
                                    </div>
                                    <div className="flex-1 space-y-2 text-left min-w-0">
                                      <span className="text-[9px] text-[#00FF41] font-bold block uppercase tracking-wider">
                                        DYNAMIC TRANSMISSION PACKET LOCKED
                                      </span>
                                      <div className="space-y-1 text-[8.5px] text-neutral-450 leading-relaxed font-mono">
                                        <div>SPECIMEN: <strong className="text-white uppercase">{selected.name}</strong></div>
                                        <div>SIGNATURE ID: <span className="text-white">{selected.id}</span></div>
                                        <div className="break-all font-mono leading-normal">
                                          Sequence:<br/>
                                          <span className="text-emerald-400 font-bold select-all tracking-wider">{selected.sequence}</span>
                                        </div>
                                      </div>
                                      <div className="flex gap-2.5 pt-1">
                                        <button
                                          type="button"
                                          onClick={() => {
                                            sound.playSynthesisSuccess();
                                            navigator.clipboard.writeText(tradeUrl);
                                            triggerLog(`Trade link for "${selected.name}" copy to system clipboard complete.`, "success");
                                          }}
                                          className="px-2.5 py-1 bg-green-950 border border-green-600 hover:bg-green-900 text-green-400 hover:text-white transition-all rounded text-[8px] font-bold uppercase cursor-pointer"
                                        >
                                          📋 COPY LINK
                                        </button>
                                        <a
                                          href={tradeUrl}
                                          target="_blank"
                                          rel="noopener noreferrer"
                                          className="px-2.5 py-1 bg-neutral-900 border border-neutral-700 hover:border-[#00FF41] text-neutral-400 hover:text-white transition-all rounded text-[8px] font-bold uppercase text-center"
                                          onClick={() => sound.playBeep(440, 0.05, "sine")}
                                        >
                                          🔗 OPEN LINK
                                        </a>
                                      </div>
                                    </div>
                                  </div>
                                );
                              })()}

                            </div>
                          </div>

                          {/* Column 2: Receive & Scan Trade QR (To Receive) */}
                          <div className="lg:col-span-2 bg-neutral-950/60 border border-green-955/40 p-4 rounded flex flex-col justify-between space-y-4">
                            <div className="space-y-3.5">
                              <span className="text-[9px] text-blue-400 font-extrabold uppercase tracking-widest block bg-blue-950/15 border border-blue-900/30 py-1 px-2 rounded">
                                ▼ CAPTURE INCOMING TRANSMISSION
                              </span>
                              <p className="text-[10px] text-neutral-400 leading-relaxed font-sans">
                                Scan or enter a P.O.X. sequence signal from another node's console to transfer the sequence from one node to another.
                              </p>

                              {/* Native Camera Scanner button */}
                              <div className="space-y-3 flex flex-col items-center w-full">
                                {!isTradeQrCameraActive ? (
                                  <button
                                    type="button"
                                    onClick={() => {
                                      sound.playBeep(520, 0.08, "sine");
                                      setIsTradeQrCameraActive(true);
                                    }}
                                    className="w-full py-3 bg-blue-950/40 hover:bg-blue-900/40 border border-blue-700/60 text-blue-400 hover:text-white rounded text-[10px] uppercase font-bold tracking-widest flex items-center justify-center gap-2 cursor-pointer transition-all active:scale-98"
                                  >
                                    <Camera className="w-3.5 h-3.5 text-blue-400" />
                                    <span>BEGIN P.O.X. SIGNATURE SCAN</span>
                                  </button>
                                ) : (
                                  <div className="w-full flex flex-col items-center gap-2">
                                    <span className="text-[8.5px] text-yellow-500 font-bold animate-pulse font-mono block uppercase">
                                      ● OPTICAL SENSORS ENGAGED
                                    </span>
                                    {/* Custom SQUARE aimed wrapper with reticles and laser scan line */}
                                    <div className="relative w-full max-w-[240px] aspect-square rounded border-2 border-blue-500/80 bg-black overflow-hidden shadow-[0_0_20px_rgba(59,130,246,0.3)]">
                                      <div id="trade-qr-reader" className="w-full h-full" />
                                      <div className="absolute inset-4 pointer-events-none z-[19] border border-[#00FF41]/20">
                                        <div className="absolute top-0 left-0 w-3.5 h-3.5 border-t-2 border-l-2 border-[#00FF41]" />
                                        <div className="absolute top-0 right-0 w-3.5 h-3.5 border-t-2 border-r-2 border-[#00FF41]" />
                                        <div className="absolute bottom-0 left-0 w-3.5 h-3.5 border-b-2 border-l-2 border-[#00FF41]" />
                                        <div className="absolute bottom-0 right-0 w-3.5 h-3.5 border-b-2 border-r-2 border-[#00FF41]" />
                                        <div className="w-full h-[1.5px] bg-[#00FF41] opacity-75 shadow-[0_0_8px_#00FF41] absolute top-1/2 animate-bounce" />
                                      </div>
                                    </div>
                                    <button
                                      type="button"
                                      onClick={() => {
                                        sound.playBeep(440, 0.05, "sine");
                                        setIsTradeQrCameraActive(false);
                                      }}
                                      className="px-4 py-1.5 bg-red-955 border border-red-800 text-red-500 hover:text-white rounded text-[8.5px] uppercase font-bold cursor-pointer transition-all mt-1 font-mono"
                                    >
                                      ✕ TERMINATE SENSOR SCAN
                                    </button>
                                  </div>
                                )}

                                <div className="w-full flex items-center gap-2">
                                  <div className="h-[1px] bg-green-950/50 flex-1" />
                                  <span className="text-[8px] text-neutral-600 uppercase tracking-widest">MANUAL ENTRY</span>
                                  <div className="h-[1px] bg-green-950/50 flex-1" />
                                </div>

                                {/* Manual url input */}
                                <div className="w-full space-y-1.5">
                                  <input
                                    type="text"
                                    placeholder="Enter P.O.X. Signature..."
                                    value={manualTradeUrlInput}
                                    onChange={(e) => setManualTradeUrlInput(e.target.value)}
                                    className="w-full bg-neutral-950 border border-neutral-800 text-neutral-200 rounded p-2 text-[9.5px] font-mono focus:outline-none focus:border-blue-500 placeholder-neutral-700 text-left"
                                  />
                                  <button
                                    type="button"
                                    onClick={() => {
                                      sound.playBeep(440, 0.05, "sine");
                                      if (manualTradeUrlInput.trim()) {
                                        const parsed = handleProcessTradePayload(manualTradeUrlInput);
                                        if (parsed) {
                                          setManualTradeUrlInput("");
                                        }
                                      } else {
                                        triggerLog("Pasted buffer is empty.", "warn");
                                      }
                                    }}
                                    className="w-full py-1.5 bg-neutral-900 border border-neutral-700 hover:border-[#00FF41] text-neutral-400 hover:text-white font-bold rounded text-[8.5px] uppercase cursor-pointer transition-all"
                                  >
                                    INITIATE TRANSFER
                                  </button>
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      )}

                      {/* Footer telemetry */}
                      <div className="border-t border-green-900/30 pt-2 flex justify-between text-[8px] text-neutral-500 font-mono">
                        <span>P2P SIGNALS ROUTED: SECURE LOOPBACK</span>
                        <span>INTERFEROMETER SENSITIVITY: 99.8%</span>
                        <span>COM_SOCKET_ALLOCATION: PORTAL_CHANNELS_STABLE</span>
                      </div>
                    </div>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>



            </div>
          )}
        </section>

        {/* 3. Bottom Status Log line matching Sleek Theme */}
        <footer className="h-10 bg-black border-t border-green-900/30 flex items-center px-4 overflow-hidden select-none">
          <div className="flex space-x-3 text-[9px] text-green-700 font-mono items-center w-full">
            <span className="text-white font-bold select-all">[{logs.time}]</span>
            <span className={`uppercase font-semibold select-all font-mono tracking-wider ${
              logs.type === 'warn' ? 'text-orange-500 font-bold' : 
              logs.type === 'success' ? 'text-[#00FF41] font-bold shadow-[0_0_4px_#00FF41/20]' : 'text-green-500'
            }`}>
              {logs.text}
            </span>
          </div>
        </footer>

      </main>

      {/* DYNAMIC GENEPOOL TRANSPONDER INJECTOR ENLARGED QR COVER */}
      <AnimatePresence>
        {isOperatorQrEnlarged && (() => {
          const qrData = "OPERATOR_GEN_09#NODE-0DF0F41A-F05B-442B";

          return (
            <motion.div
              key="enlarged-operator-qr-popup"
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="absolute inset-[3%] md:inset-[6%] bg-[#020502]/98 border-2 border-green-500 rounded-lg p-5 flex flex-col justify-between z-[99999] overflow-hidden font-mono text-left shadow-[0_0_50px_rgba(0,255,65,0.6)]"
            >
              {/* Titlebar */}
              <div className="flex justify-between items-center bg-green-950/40 border border-green-500/40 px-3 py-1.5 rounded-t-sm mb-3">
                <span className="text-[9px] text-[#00FF41] font-bold tracking-widest uppercase flex items-center gap-1.5 font-mono">
                  <span className="w-1.5 h-1.5 bg-green-500 rounded-full animate-ping" />
                  [ SECURE P2P SIGNATURE &bull; TRANSCEIVER BROADCAST ]
                </span>
                <button
                  type="button"
                  onClick={() => {
                    sound.playBeep(440, 0.05, "sine");
                    setIsOperatorQrEnlarged(false);
                  }}
                  className="px-2.5 py-1 bg-red-950/80 hover:bg-red-900 border border-red-800 text-red-500 hover:text-white rounded text-[8.5px] cursor-pointer font-bold tracking-wider transition-all duration-150 active:scale-95 uppercase"
                >
                  ✕ CLOSE SIGNATURE
                </button>
              </div>

              {/* Main Body */}
              <div className="flex-1 flex flex-col items-center justify-center p-3">
                <div className="bg-black border border-green-500 p-4 rounded flex items-center justify-center shadow-[0_0_20px_rgba(0,255,65,0.2)] max-w-[220px] w-full aspect-square relative bg-neutral-950">
                  <QRCodeSVG 
                    value={qrData} 
                    size={188} 
                    bgColor="#000000" 
                    fgColor="#00FF41" 
                    level="L" 
                    includeMargin={true} 
                  />
                </div>
                
                <p className="text-[9px] text-[#00FF41] font-mono tracking-wider uppercase mt-4 text-center max-w-sm font-semibold select-none leading-relaxed">
                  SECURE OPERATOR TRANSMISSION ACTIVE...<br/>
                  Scan representation to register OPERATOR_GEN_09 as a static trusted neighbor node.
                </p>
              </div>

              {/* Operator details */}
              <div className="bg-black/60 p-3 rounded border border-green-950 text-[9px] font-mono grid grid-cols-2 gap-x-4 gap-y-1 mt-3 leading-normal">
                <div>OPERATOR: <strong className="text-white uppercase font-black">OPERATOR_GEN_09</strong></div>
                <div>SIGNATURE ID: <strong className="text-[#00FF41]">NODE-0DF0F41A-F05B-442B</strong></div>
                <div className="col-span-2 text-[8px] text-green-700 font-bold tracking-wider select-none truncate">
                  STATIC BROADCAST LINK PAYLOAD: {qrData}
                </div>
              </div>
            </motion.div>
          );
        })()}

        {enlargedQrCreatureId && (() => {
          const bot = creatures.find(c => c.id === enlargedQrCreatureId);
          if (!bot) return null;
          
          const qrData = JSON.stringify({
            id: bot.id,
            name: bot.name,
            seq: bot.sequence,
            stats: {
              hp: bot.vitality,
              atk: bot.attack,
              def: bot.defense,
              spd: bot.speed
            },
            appended: bot.appendedGenes || [],
            moves: getUnlockedMoves(bot.sequence).map(m => m.name),
            visual: {
              faction: bot.faction,
              type: bot.type,
              ascii: bot.asciiArt
            },
            sound: getEmotSoundDetails(bot.sequence)
          });

          return (
            <motion.div
              key="enlarged-qr-popup"
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="absolute inset-[3%] md:inset-[6%] bg-[#020502]/98 border-2 border-green-500 rounded-lg p-5 flex flex-col justify-between z-[99999] overflow-hidden font-mono text-left shadow-[0_0_50px_rgba(0,255,65,0.6)]"
            >
              {/* Titlebar */}
              <div className="flex justify-between items-center bg-green-950/40 border border-green-500/40 px-3 py-1.5 rounded-t-sm mb-3">
                <span className="text-[9px] text-[#00FF41] font-bold tracking-widest uppercase flex items-center gap-1.5 font-mono">
                  <span className="w-1.5 h-1.5 bg-green-500 rounded-full animate-ping" />
                  [ HIGH-POTENCY GENETIC LINKER &bull; TRANSPONDER ]
                </span>
                <button
                  type="button"
                  onClick={() => {
                    sound.playBeep(440, 0.05, "sine");
                    setEnlargedQrCreatureId(null);
                  }}
                  className="px-2.5 py-1 bg-red-950/80 hover:bg-red-900 border border-red-800 text-red-500 hover:text-white rounded text-[8.5px] cursor-pointer font-bold tracking-wider transition-all duration-150 active:scale-95 uppercase"
                >
                  ✕ DISCONNECT LINK
                </button>
              </div>

              {/* Main Body */}
              <div className="flex-1 flex flex-col items-center justify-center p-3">
                <div className="bg-black border border-green-500 p-4 rounded flex items-center justify-center shadow-[0_0_20px_rgba(0,255,65,0.2)] max-w-[220px] w-full aspect-square relative bg-neutral-950">
                  <QRCodeSVG 
                    value={qrData} 
                    size={188} 
                    bgColor="#000000" 
                    fgColor="#00FF41" 
                    level="L" 
                    includeMargin={true} 
                  />
                </div>
                
                <p className="text-[9px] text-[#00FF41] font-mono tracking-wider uppercase mt-4 text-center max-w-sm font-semibold select-none leading-relaxed">
                  TRANSONIC RADIER BEACON BROADCASTING...<br/>
                  Scan using any QR-compliant receiver to sync genetic codes, move definitions, stats, and wave structures.
                </p>
              </div>

              {/* Miniature Stats summary inside QR popup */}
              <div className="bg-black/60 p-3 rounded border border-green-950 text-[9px] font-mono grid grid-cols-2 gap-x-4 gap-y-1 mt-3 leading-normal">
                <div>NAME: <strong className="text-white uppercase">{bot.name}</strong></div>
                <div>FACTION: <strong className="text-white uppercase">{bot.faction}</strong></div>
                <div>TYPE: <strong className="text-white uppercase">{bot.type}</strong></div>
                <div>VITALITY FORCE: <strong className="text-[#00FF41]">{bot.vitality} HP</strong></div>
                <div className="col-span-2 text-[8px] text-green-700 font-bold tracking-wider select-none truncate">
                  ENCODED PAYLOAD SEQUENCE: {bot.sequence}
                </div>
              </div>
            </motion.div>
          );
        })()}

        {isTradeQrEnlarged && tradeSelectedCreatureId && (() => {
          const selected = creatures.find(c => c.id === tradeSelectedCreatureId);
          if (!selected) return null;
          const tradeUrl = window.location.origin + window.location.pathname + "?trade=" + encodeCreatureToBase64(selected);

          return (
            <motion.div
              key="enlarged-trade-qr-popup"
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="absolute inset-[3%] md:inset-[6%] bg-[#020502]/98 border-2 border-[#00FF41] rounded-lg p-5 flex flex-col justify-between z-[99999] overflow-hidden font-mono text-left shadow-[0_0_50px_rgba(0,255,65,0.6)]"
            >
              {/* Titlebar */}
              <div className="flex justify-between items-center bg-green-950/40 border border-green-500/40 px-3 py-1.5 rounded-t-sm mb-3">
                <span className="text-[9px] text-[#00FF41] font-bold tracking-widest uppercase flex items-center gap-1.5 font-mono">
                  <span className="w-1.5 h-1.5 bg-green-500 rounded-full animate-ping" />
                  [ SECURE P2P TRANSFER CARRIER QR &bull; TRANSCEIVER ]
                </span>
                <button
                  type="button"
                  onClick={() => {
                    sound.playBeep(440, 0.05, "sine");
                    setIsTradeQrEnlarged(false);
                  }}
                  className="px-2.5 py-1 bg-red-950/80 hover:bg-red-900 border border-red-800 text-red-500 hover:text-white rounded text-[8.5px] cursor-pointer font-bold tracking-wider transition-all duration-150 active:scale-95 uppercase"
                >
                  ✕ CLOSE PREVIEW
                </button>
              </div>

              {/* Main Body */}
              <div className="flex-1 flex flex-col items-center justify-center p-3">
                <div className="bg-white p-4 rounded flex items-center justify-center shadow-[0_0_30px_rgba(0,255,65,0.35)] max-w-[260px] w-full aspect-square relative hover:scale-102 transition-transform cursor-pointer">
                  <QRCodeSVG 
                    value={tradeUrl} 
                    size={220} 
                    bgColor="#FFFFFF" 
                    fgColor="#000000" 
                    level="Q" 
                    includeMargin={false} 
                  />
                </div>
                
                <p className="text-[9px] text-[#00FF41] font-mono tracking-wider uppercase mt-4 text-center max-w-sm font-semibold select-none leading-relaxed">
                  P2P TRANSCEIVER ENCODED PAYLOAD READY...<br/>
                  Scan with the optical receiver or any camera on another terminal/phone to clone this specimen!
                </p>
              </div>

              {/* Stats Summary */}
              <div className="bg-black/60 p-3 rounded border border-green-950/80 text-[9px] font-mono grid grid-cols-2 gap-x-4 gap-y-1.5 mt-3 leading-normal">
                <div>SPECIMEN: <strong className="text-white uppercase">{selected.name}</strong></div>
                <div>CLASSIFICATION: <strong className="text-white uppercase">{selected.type}</strong></div>
                <div>FACTION: <strong className="text-white uppercase">{selected.faction}</strong></div>
                <div>SIGNATURE ID: <strong className="text-white">{selected.id}</strong></div>
                <div className="col-span-2 text-[8px] text-emerald-400 font-bold tracking-wider select-all truncate">
                  URL_PAYLOAD: {tradeUrl}
                </div>
              </div>
            </motion.div>
          );
        })()}

        {receivedTradeCreature && (
          <motion.div
            key="received-trade-popup"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            className="absolute inset-[3%] md:inset-[6%] bg-[#020502]/98 border-2 border-blue-500 rounded-lg p-5 flex flex-col justify-between z-[99999] overflow-hidden font-mono text-left shadow-[0_0_50px_rgba(59,130,246,0.6)]"
          >
            {/* Titlebar */}
            <div className="flex justify-between items-center bg-blue-950/40 border border-blue-500/40 px-3 py-1.5 rounded-t-sm mb-3">
              <span className="text-[9px] text-blue-400 font-bold tracking-widest uppercase flex items-center gap-1.5 font-mono">
                <span className="w-1.5 h-1.5 bg-blue-500 rounded-full animate-ping" />
                [ 🧬 RETRIEVED GENETIC COUPLING SPECIMEN ]
              </span>
              <button
                type="button"
                onClick={() => {
                  sound.playBeep(440, 0.05, "sine");
                  setReceivedTradeCreature(null);
                }}
                className="px-2.5 py-1 bg-red-950/80 hover:bg-red-900 border border-red-800 text-red-500 hover:text-white rounded text-[8.5px] cursor-pointer font-bold tracking-wider transition-all duration-150 active:scale-95 uppercase"
              >
                ✕ DISMISS PAYLOAD
              </button>
            </div>

            {/* Main body info */}
            <div className="flex-1 flex flex-col items-center justify-center p-3 text-center space-y-4">
              <div className="font-mono text-[7.5px] leading-[1.05] tracking-widest whitespace-pre bg-black border border-blue-900/55 p-3.5 rounded text-blue-400 select-all h-[110px] w-[180px] flex items-center justify-center overflow-hidden">
                {receivedTradeCreature.asciiArt}
              </div>
              
              <div className="space-y-1">
                <span className="text-[9px] text-[#00FF41] font-bold block uppercase tracking-widest bg-emerald-950/20 px-2 py-0.5 rounded border border-emerald-900/30">
                  🧬 ARCHIVE COUPLING DETECTED
                </span>
                <h3 className="text-white text-lg font-black uppercase tracking-wider">{receivedTradeCreature.name}</h3>
                <p className="text-[8.5px] text-neutral-400 font-mono">TYPE: {receivedTradeCreature.type} ({receivedTradeCreature.faction})</p>
              </div>

              <div className="text-[9.5px]/relaxed text-neutral-500 max-w-sm font-sans italic">
                "{receivedTradeCreature.lore || "No bio-log recorded for this specific specimen."}"
              </div>

              {/* Stats overview */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-2 w-full max-w-sm mt-2 text-[9px] font-mono text-left">
                <div className="bg-neutral-900/50 border border-neutral-800 p-1.5 rounded">
                  <span className="text-neutral-500 block text-[7.5px]">VITALITY</span>
                  <strong className="text-white">{receivedTradeCreature.vitality} HP</strong>
                </div>
                <div className="bg-neutral-900/50 border border-neutral-800 p-1.5 rounded">
                  <span className="text-neutral-500 block text-[7.5px]">ATTACK</span>
                  <strong className="text-white">{receivedTradeCreature.attack} ATK</strong>
                </div>
                <div className="bg-neutral-900/50 border border-neutral-800 p-1.5 rounded">
                  <span className="text-neutral-500 block text-[7.5px]">DEFENSE</span>
                  <strong className="text-white">{receivedTradeCreature.defense} DEF</strong>
                </div>
                <div className="bg-neutral-900/50 border border-neutral-800 p-1.5 rounded">
                  <span className="text-neutral-500 block text-[7.5px]">SPEED</span>
                  <strong className="text-white">{receivedTradeCreature.speed} SPD</strong>
                </div>
              </div>
            </div>

            <div className="mt-3 bg-neutral-900/30 border border-blue-950 p-2.5 rounded font-mono text-[8.5px] text-neutral-400 uppercase leading-snug">
              This specimen's complete DNA strand packet was successfully parsed client-side and saved permanently inside local biological host storage chambers: 
              <span className="text-[#00FF41] block text-[9px] break-all select-all font-bold mt-1">{receivedTradeCreature.sequence}</span>
            </div>

            <button
              type="button"
              onClick={() => {
                sound.playSynthesisSuccess();
                setReceivedTradeCreature(null);
              }}
              className="mt-3.5 py-2.5 bg-blue-950 border border-blue-600 hover:bg-blue-900 text-blue-300 hover:text-white rounded text-[9px] font-bold uppercase tracking-widest cursor-pointer transition-all active:scale-98 text-center"
            >
              INTEGRATE GENE SEQUENCE INTO DATABASE
            </button>
          </motion.div>
        )}

        {disintegratedModal && (
          <motion.div
            key="telomere-failure-popup"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            className="absolute inset-[3%] md:inset-[10%] bg-black/98 border-2 border-red-500 rounded-lg p-5 flex flex-col justify-between z-[99999] overflow-hidden font-mono text-left shadow-[0_0_50px_rgba(239,68,68,0.6)]"
          >
            {/* Titlebar */}
            <div className="flex justify-between items-center bg-red-950/40 border border-red-500/40 px-3 py-1.5 rounded-t-sm mb-3">
              <span className="text-[9px] text-red-400 font-bold tracking-widest uppercase flex items-center gap-1.5 font-mono">
                <span className="w-1.5 h-1.5 bg-red-500 rounded-full animate-ping" />
                [ GENOME STABILITY FAILURE ]
              </span>
            </div>

            {/* Content area */}
            <div className="flex-1 overflow-y-auto space-y-4 pr-1">
              <div className="bg-red-950/10 border border-red-900/30 p-4 rounded-md">
                <h3 className="text-white text-base md:text-lg font-black uppercase tracking-wider text-red-500 mb-1">
                  SEQUENCE TERMINATED: {disintegratedModal.name}
                </h3>
                <p className="text-[10px]/relaxed text-neutral-300">
                  CRITICAL ALARM: Telomere length reached <span className="text-red-500 font-bold">0%</span>. Cellular transcription has failed, leading to full chromosomal instability. The species' genetic carrier construct has disintegrated entirely.
                </p>
              </div>

              {/* Genes list container */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Kept Genes (Returned to pool) */}
                <div className="bg-neutral-900/40 border border-emerald-950/25 p-3 rounded-md">
                  <div className="flex justify-between items-center mb-2 border-b border-emerald-900 pb-1.5">
                    <span className="text-[10px] text-emerald-400 font-extrabold uppercase tracking-wide">
                      GENES KEPT ({disintegratedModal.returnedBlocks.length})
                    </span>
                    <span className="text-[8px] text-emerald-600 font-mono">RETURNED TO POOL</span>
                  </div>
                  {disintegratedModal.returnedBlocks.length === 0 ? (
                    <span className="text-[9px] text-neutral-600 italic font-mono uppercase">None</span>
                  ) : (
                    <div className="flex flex-wrap gap-1.5 max-h-[140px] overflow-y-auto pr-1">
                      {disintegratedModal.returnedBlocks.map((seq, i) => (
                        <span key={i} className="text-[9px] font-mono px-2 py-0.5 bg-emerald-950/30 text-emerald-400 border border-emerald-900/30 rounded">
                          {seq}
                        </span>
                      ))}
                    </div>
                  )}
                  <p className="text-[8px] text-neutral-500 mt-2 italic leading-tight">
                    Recovered gene blocks have been returned to your Gen-Vault.
                  </p>
                </div>

                {/* Lost Genes (Destroyed permanently) */}
                <div className="bg-neutral-900/40 border border-red-950/25 p-3 rounded-md">
                  <div className="flex justify-between items-center mb-2 border-b border-red-900 pb-1.5">
                    <span className="text-[10px] text-red-400 font-extrabold uppercase tracking-wide">
                      GENES LOST ({disintegratedModal.destroyedBlocks.length})
                    </span>
                    <span className="text-[8px] text-red-600 font-mono">DESTROYED</span>
                  </div>
                  {disintegratedModal.destroyedBlocks.length === 0 ? (
                    <span className="text-[9px] text-neutral-600 italic font-mono uppercase">None</span>
                  ) : (
                    <div className="flex flex-wrap gap-1.5 max-h-[140px] overflow-y-auto pr-1">
                      {disintegratedModal.destroyedBlocks.map((seq, i) => (
                        <span key={i} className="text-[9px] font-mono px-2 py-0.5 bg-red-950/30 text-red-400 border border-red-900/30 rounded line-through">
                          {seq}
                        </span>
                      ))}
                    </div>
                  )}
                  <p className="text-[8px] text-neutral-500 mt-2 italic leading-tight">
                    These gene blocks have degraded completely and are permanently lost.
                  </p>
                </div>
              </div>
            </div>

            {/* Action buttons */}
            <div className="pt-3 border-t border-neutral-900 mt-3 flex flex-col sm:flex-row gap-2">
              <button
                type="button"
                onClick={() => {
                  sound.playSynthesisSuccess();
                  setDisintegratedModal(null);
                  setActiveTab('library');
                  setInspectedCreatureId(null);
                  setEnlargedQrCreatureId(null);
                }}
                className="w-full py-2.5 bg-red-950/20 hover:bg-neutral-900 border border-red-500 text-red-400 hover:text-white rounded text-[9.5px] font-bold uppercase tracking-widest cursor-pointer transition-all active:scale-98 text-center"
              >
                RETURN TO GEN-VAULT DATA
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

    </div>
  );
}
