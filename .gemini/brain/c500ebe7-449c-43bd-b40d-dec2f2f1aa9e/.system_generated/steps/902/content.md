Title: Live Content

Description: Fetched live

Source: https://docs.google.com/document/d/1eYaRzBxl0eNcFbF-Jg626-E3tl4ZKttJv126Njt2drI/export?format=txt

---

﻿Bio-Lab Tab: Complete System Design & Implementation Specification
1. System Overview
The Bio-Lab (represented programmatically as activeTab === 'combinator') functions as the core bio-synthesis matrix of the P.O.X. Console. It is comprised of a dual-reactor ecosystem controlled by a sub-tab navigation state (bioLabSubTab):
1. P.O.X. Reactor (pox): Standard-frequency cybernetic synthesizer. Utilizes real-time and lunar-modulated calendar wave patterns to automatically splice standard 8-character DNA gene blocks over set cycles (16 seconds standard, 8 seconds boosted).
2. Anomaly Engine (anomaly): Unstable high-energy synthesis matrix. Depletes nucleotide stockpiles to forge exotic anomalous gene blocks containing proprietary alphabetic characters (X, Z, Y, W, etc.) with unique tactical passive perks, utilizing a logarithmic chance mechanic coupled with Gaussian distribution peaks.
________________


2. Visual Theme & Color Schemes
The Bio-Lab dynamically toggles its aesthetic profile based on the active sub-tab. It leverages Tailwind CSS utilities overlaid on a structural terminal-style grid.
A. Sub-Tab 1: P.O.X. Reactor (pox)
* Primary Tint: Cybernetic Emerald Green (#00FF41)
* Accent Gradients: from-green-950/30 to-emerald-955/20 and from-[#00c93c] to-green-500
* Borders & Shadows: High-intensity green glow: shadow-[0_0_15px_rgba(0,255,65,0.05)]
* Typography Role: Clean, standard futuristic green readout utilizing font-mono and font-sans tracking.
B. Sub-Tab 2: Anomaly Engine (anomaly)
* Primary Tint: Quantum Purple / Unstable Cosmic Magenta (#a855f7)
* Accent Gradients: from-purple-950/20 to-pink-500/20 and from-purple-600 to-pink-500
* Borders & Shadows: Radiating purple twilight: shadow-[0_0_15px_rgba(168,85,247,0.15)]
* Typography Role: Deep anomalous purple, paired with the FlickeringPurpleText component to simulate voltage drops and tachyon ripples.
________________


3. Core State Variables, Refs & Imports
To replicate the state machine, maintain these react bindings:
Variable / Ref Name
	Type
	Initial Value
	Description
	bioLabSubTab
	'pox' | 'anomaly'
	'pox'
	Navigational toggle between the twin synthesizers.
	idleTime
	number
	16
	Countdown remaining (seconds) until the next synthesis cycle.
	idleTimeRef
	MutableRefObject<number>
	16
	Keeps standard timer hook synchronized in async tick loops.
	boostSecondsLeft
	number
	0
	Remainder of reactor acceleration boost state (seconds).
	sequences
	Array<{sequence: string, count: number}>
	[]
	Master ledger representing unlocked 8bp gene segments.
	anomalyEngineActive
	boolean
	false
	Unstable fusion state switch; toggles resource extraction.
	isGeneLogPopupOpen
	boolean
	false
	Expands the full scrolling history overlay of synthesized gene packets.
	isGeneLedgerExpanded
	boolean
	false
	Expand overlay for the Pair-Based Molecular Step-Search tool.
	discoveredPacketsLog
	Array<GenePacket>
	[]
	Chronological roster of past multi-gene batch generations.
	scrollingGene
	string
	"--------"
	8-character string mimicking current synthesizer output state.
	________________


4. Analytical Utilities & Mathematical Formulas
A. Lunar-Synchronized Base-Pair Wave Generator
Every calendar day, a deterministic wave configuration is calculated to shift spawn frequencies. It matches local synodic lunar cycles based on a fixed reference epoch.
* Reference Epoch: May 17, 2026, 01:54:00 UTC (Standard Reference New Moon)
* Synodic Lunar Month Constants: 29.53059 days
export interface WaveConfig {
  pair: string;              // "GA", "AT", "CG", etc.
  primary: string;           // "G"
  secondary: string;         // "A"
  isSuppressed: boolean;     // True if designated Week 3 Congested Decay is active
  lunarAge: number;          // Synodic offset in days
  phaseName: string;         // E.g. "Waxing Crescent", "Full Moon"
  primaryMultiplier: number;  // Base 1.125 + moon modifier
  secondaryMultiplier: number;// Base 1.625 + moon modifier
}


Deterministic Daily Core Calculations
1. Deterministic Hashing: Establish daily pseudo-random factors from Date strings (YYYY-MM-DD):

2. Lunar Phase: Calculates age relative to the Epoch:

3. 
4. Week 3 Decay (50/50 Suppression):
If Calendar Day of Month is within 
5. , it has a 
6.  chance to suppress active waves:

7. Amplitude Oscillatory Modifiers: The New and Full Moon modify spawn weights via sinusoid decay:

8. 
9. 
10. 
________________


B. Weighted Random Gene Synthesis
When the Standard Reactor ticks to 0, it constructs 8-character strings using the day's WaveConfig. If suppressed, characters default to a flat 
 chance. Under active wave states:
   * If index 
   *  is preceded by 
   *  equal to the Primary Catalyst (e.g. G), the Secondary Catalyst (e.g. A) has weight 
   *  (e.g. 
   * ).
   * Otherwise, the Primary Catalyst has weight 
   *  (e.g. 
   * ).
________________


C. Anomaly Engine Success Scale Algorithm
The Anomaly Engine scales logarithmic synthesis probability based on total Nucleotide reserves. It is designed with localized Gaussian distribution peaks every 
 of progress to reward deep-resonance tuning:


Gaussian Resonances & Peak Boosts
Localized high success spikes occur programmatically at indices 
:




________________


D. Anomalous Gene Attributes
Any gene is flagged as Anomalous (isAnomalousGene = true) if it is exactly 8 characters long and contains any symbol inside "XZYW?!$%&@#".
The primary leading character yields structural benefits assigned to the matching creature:
   * X or Z → Vortex Phase-Strike: Double-strike capabilities (Attacks twice per turn).
   * Y or W → Supernova Reverb: Self-destruction triggers on death, vaporizing opponents.
   * ? or ! → Quantum Extraction: Anomaly Harvesting amplification (+1 extra gene earned).
   * $ or % → Bio-Organic Siphon: Life Steal (+15 HP replenished during attack phases).
   * Default (Other symbols) → Temporal Mirror Shield: Evasion capabilities (
   *  total evasion rate).
________________


5. Periodic Reactor Heartbeat (Tick Engine)
The core loop operates within a 1000ms window. The algorithm evaluates state changes sequentially:








[ 1-Second Interval Ticks ]
                            │
               Is Global Power Status ON?
                ├── [NO] ──> Pause Loop
                └── [YES] ──> Proceed
                            │
              Decrement active Booster timers.
              Is Reactor frozen by a system loop?
                ├── [YES] ──> Halt tick progression
                └── [NO] ──> Draw countdown variable: "idleTime"
                            │
                  Decrement countdown (idleTime - 1)
                  Does countdown fall to <= 0?
                    ├── [NO] ──> Update UI visual clocks & exit
                    └── [YES] ──> [ TRIGGER SYNTHESIS EVENT ]




Trigger Synthesis Decision Matrix
   1. Unstable Fusion Mode Authorized (Anomaly Engine is ON and Stock 
   2. ):
   * Deplete exactly 
   *  nucleotides.
   * Roll a random real number 
   *  in 
   * .
   * If 
   * :
   * Play synthesis sound effect.
   * Inject generated anomalous block into gene ledger (sequences).
   * Throw a successful console log: "Anomaly Engine Fusion Successful!".
   * If 
   * :
   * Consume resources, trigger failure sound effect, and emit warnings: "Decoupling Failure. Decay logged in buffer".
   3. Standard Synthesis Mode (Anomaly Engine is OFF or Stock 
   4. ):
   * Synthesize a batch containing eight (8) standard genes using generateWaveGeneBlock.
   * Distribute genes into the ledger, tracking new unlocks vs multi-copy counts.
   * Increment experience structures.
   5. Reset Timer:
   * Set current countdown timer variables back to maximum thresholds (8 for active boost states, 16 for standard mode).
   6. ________________


6. Layout Hierarchy & Interactive Elements




  



Interactive Dynamic Popups
1. Pair-Based Molecular Step-Search Overlay
An expandable full-frame matrix overlay that allows hierarchical sequencing through nested DNA couples:
   * Selector Blocks: Allows users to step through a 
   *  array of all possible couples (AA, AC, AG, AT... TT).
   * Path Steps: Consists of four distinct hierarchical steps (
   * ). Selecting pairs dynamically builds the Prefix string, allowing targeted inventory filtering.
   * Undo / Reset: Single-action rollback controls to trim or clear the accumulated Prefix.
2. Synthesis Log Packet Archive
Provides a highly specialized chronological register:
   * Filtering Options: Search text blocks to filter archived core records.
   * Packet Renders: Renders past multi-unlock batches as stylized interactive tags, highlighting anomalous segments in glowing purple text.
   * Detailed Examination: Clicking any past gene loads its properties directly into a single-pane diagnostic reader to analyze gene age, rarity indexes, and splicing metrics.
3. Decrypted Anomaly Vault
   * Provides the anomalous segment view when the sub-tab is set to anomaly. It displays all secured anomalous blocks and tracks specialized custom stats.
   * Renders warning advisories: "INFO: Anomalous gene effects are unknown and inconsistent. Splice with caution!" directly inside the base layout border.
________________


7. Complete Implementation Code Template
Here is a self-contained component model demonstrating the layout structure:


import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Zap, Terminal, Search, Check } from 'lucide-react';


export default function RecreatedBioLab() {
  const [bioLabSubTab, setBioLabSubTab] = useState<'pox' | 'anomaly'>('pox');
  const [anomalyEngineActive, setAnomalyEngineActive] = useState<boolean>(false);
  const [idleTime, setIdleTime] = useState<number>(16);


  return (
    <div className="flex flex-col h-full bg-black text-white p-4 font-mono select-none">
      {/* 1. Subnav Header Bar */}
      <div className="flex border-b border-green-950/40 pb-2 gap-2 shrink-0">
        <button
          onClick={() => setBioLabSubTab('pox')}
          className={`px-4 py-1.5 text-[10px] font-bold border rounded uppercase tracking-wider cursor-pointer ${
            bioLabSubTab === 'pox'
              ? "bg-green-950/20 border-[#00FF41] text-[#00FF41] shadow-[0_0_8px_rgba(0,255,65,0.1)]"
              : "bg-black/40 border-green-955/40 text-green-700"
          }`}
        >
          P.O.X. Reactor
        </button>
        <button
          onClick={() => setBioLabSubTab('anomaly')}
          className={`px-4 py-1.5 text-[10px] font-bold border rounded uppercase tracking-wider cursor-pointer ${
            bioLabSubTab === 'anomaly'
              ? "bg-purple-950/20 border-purple-500 text-purple-300 shadow-[0_0_8px_rgba(168,85,247,0.15)]"
              : "bg-black/40 border-green-955/40 text-purple-805"
          }`}
        >
          Anomaly Engine
        </button>
      </div>


      {/* 2. Side-By-Side Main Grid layout */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 flex-grow mt-3 overflow-hidden">
        {/* PANEL 1: Cybernetic Synthesizer controls (Left) */}
        <div className={`bg-neutral-950 p-4 rounded border flex flex-col justify-between ${
          bioLabSubTab === 'anomaly' ? "border-purple-900/40" : "border-green-900/40"
        }`}>
          <div>
            <div className="flex justify-between items-center mb-1 text-[10px] uppercase text-green-700">
              <span>{bioLabSubTab === 'pox' ? "[ G.E.N. P.O.X. Reactor V2.4 ]" : "[ WARNING: UNKNOWN REACTOR ]"}</span>
              <span className={bioLabSubTab === 'anomaly' ? "text-purple-300 animate-pulse" : "text-[#00FF41]"}>SYSTEMS ON</span>
            </div>
            <h2 className="text-xs font-bold text-white tracking-wider mb-3 uppercase">
              {bioLabSubTab === 'pox' ? "Single-Node Cybernetic Synthesizer" : "Anomalous Synthesis Engine"}
            </h2>
          </div>


          {/* Core countdown displays */}
          <div className="flex flex-col items-center justify-center py-4">
            <div className={`text-2xl font-bold p-3 bg-black border-2 rounded tracking-widest ${
              bioLabSubTab === 'anomaly' ? 'border-purple-500 text-purple-350' : 'border-green-500 text-[#00FF41]'
            }`}>
              {bioLabSubTab === 'pox' ? "AGTCGTAC" : "??X%?Z!!"}
            </div>
            {/* Visual Progress Slider */}
            <div className="w-full max-w-sm bg-neutral-900 h-2 border rounded-full mt-3 p-0.5">
              <div 
                className={`h-full rounded-sm ${bioLabSubTab === 'anomaly' ? 'bg-purple-600' : 'bg-[#00FF41]'}`} 
                style={{ width: `${(idleTime / 16) * 100}%` }}
              />
            </div>
            <p className="text-[10px] mt-2 font-bold uppercase text-neutral-400">
              Gene Array Ready in: <span className="text-white font-mono">{idleTime}s</span>
            </p>
          </div>


          {/* Action Triggers */}
          <div className="space-y-2">
            <button className="w-full py-2 bg-neutral-900 hover:bg-neutral-800 border border-neutral-700 text-white rounded text-xs uppercase font-bold">
              Manual Acceleration (-2s)
            </button>
          </div>
        </div>


        {/* PANEL 2: Stock Gene ledger (Right) */}
        <div className={`bg-neutral-950 p-4 rounded border flex flex-col justify-between ${
          bioLabSubTab === 'anomaly' ? "border-purple-900/40" : "border-green-900/40"
        }`}>
          <div>
            <div className={`text-[10px] uppercase mb-1.5 ${bioLabSubTab === 'anomaly' ? "text-purple-400" : "text-green-700"}`}>
              [ SPLICER STOCK GENE LEDGER ]
            </div>
            <h2 className="text-xs font-bold text-white tracking-widest mb-1">Gene Inventory</h2>
            <p className={`text-[9px] ${bioLabSubTab === 'anomaly' ? "text-purple-500" : "text-[#00c93c]"}`}>
              These 8-character DNA blocks act as raw units to construct the 64-character target genome.
            </p>
          </div>


          <div className="flex-grow flex flex-col justify-center space-y-3">
            {bioLabSubTab === 'pox' ? (
              <button className="py-4 border border-green-500/40 bg-black hover:border-[#00FF41] text-[#00FF41] rounded uppercase text-xs font-bold">
                PAIR-BASED STEP-SEARCH
              </button>
            ) : (
              <div className="border border-purple-500/50 bg-[#0c0512] p-3 rounded">
                <div className="flex justify-between items-center mb-1">
                  <span className="text-purple-400 text-xs font-bold font-mono">Anomaly Engine State</span>
                  <input 
                    type="checkbox" 
                    checked={anomalyEngineActive} 
                    onChange={(e) => setAnomalyEngineActive(e.target.checked)} 
                    className="w-4 h-4 accent-purple-600"
                  />
                </div>
                <p className="text-[8px] text-neutral-400">Requires 250k standard nucleotides. Depletes 10,000 nucleotides per reactor cycle loop.</p>
              </div>
            )}
          </div>


          <div className="text-[9px] border-t pt-3 mt-2 border-neutral-800">
            {bioLabSubTab === 'pox' ? (
              <span className="text-green-700">INFO: Gene blocks are used in the CONSTRUCTOR to build your target P.O.X. sequence.</span>
            ) : (
              <span className="text-purple-500">INFO: Anomalous gene effects are unknown and inconsistent. Splice with caution!</span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

