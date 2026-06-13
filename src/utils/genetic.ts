/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { Creature, NearbyUser } from "../types";

// Generates a random 8-letter AGTC fragment
export function generateRandomGeneBlock(): string {
  const nucleobases = ["A", "G", "T", "C"];
  let res = "";
  for (let i = 0; i < 8; i++) {
    res += nucleobases[Math.floor(Math.random() * nucleobases.length)];
  }
  return res;
}

// Generates a random 64-character AGTC sequence
export function generateRandom64Sequence(): string {
  let res = "";
  for (let i = 0; i < 8; i++) {
    res += generateRandomGeneBlock();
  }
  return res;
}

// Procedural fallback creature generator using DNA content
export function constructProceduralCreature(sequence: string, origin: string = "Created"): Creature {
  // Ratio counts
  let countA = 0;
  let countG = 0;
  let countT = 0;
  let countC = 0;

  for (let i = 0; i < sequence.length; i++) {
    const char = sequence[i];
    if (char === "A") countA++;
    else if (char === "G") countG++;
    else if (char === "T") countT++;
    else if (char === "C") countC++;
  }

  // Factions mapping based on dominant nucleobases
  let faction: "Infection" | "Mech" | "Parasite" | "Containment" = "Infection";
  const maxB = Math.max(countA, countG, countT, countC);
  if (maxB === countA) faction = "Containment";
  else if (maxB === countG) faction = "Infection";
  else if (maxB === countT) faction = "Mech";
  else if (maxB === countC) faction = "Parasite";

  // Name Generator parts
  const prefixes = ["Chitin", "Toxi", "Solder", "Tetan", "Glitch", "Phage", "Bio", "Xeno", "Vex", "Cyber"];
  const matrix = ["Weaver", "Swarm", "Shell", "Dredge", "Claw", "Suture", "Stinger", "Shell", "Latch", "Pincer"];
  const suffixes = ["v1", "Mk-III", "Pro", "404", "Omega", "B-9", "Delta", "X", "Prime", "Zeta"];

  const hash1 = (countA * 3 + countG * 7 + countT * 11 + countC * 13) % prefixes.length;
  const hash2 = (countA * 5 + countG * 2 + countT * 17 + countC * 19) % matrix.length;
  const hash3 = (countA * 9 + countG * 11 + countT * 3 + countC * 5) % suffixes.length;

  const name = `${prefixes[hash1]}-${matrix[hash2]} ${suffixes[hash3]}`;

  // Types based on dominant attributes
  let type = "Parasitic Cyborg Insectoid";
  if (faction === "Infection") type = "Organic Swarm Pathogen";
  if (faction === "Mech") type = "Autonomous Chasis Chitin";
  if (faction === "Containment") type = "Bacterial Nano Containment Grid";

  // Bio-tech stats influenced by nucleotide quantities
  // A = Vitality modifier
  // G = Attack modifier
  // T = Defense modifier
  // C = Speed modifier
  const vitality = 100 + countA * 5 + Math.floor(sequence.length / 2); // 100 to 250 approx
  const attack = Math.min(99, 15 + countG * 3);
  const defense = Math.min(99, 15 + countT * 3);
  const speed = Math.min(99, 15 + countC * 3);

  // Weapon name
  const rawWeapons = ["Corrosive Spite-Needle", "Vaporizing Plasma-Claw", "Shocking Laser-Stinger", "Bio-Acid Venting-Pod", "Micro-Phage Injector"];
  const weaponIndex = (countA + countG + countT + countC) % rawWeapons.length;
  const primaryWeapon = rawWeapons[weaponIndex];

  // Lore text
  const lore = `Synthesized from custom sub-gene segments. Displays aggressive ${faction.toLowerCase()}-sector behavior with optimized ${primaryWeapon} payloads. Integrated weapon clusters operate at ${attack}-rating.`;

  // Abstract ASCII visual matrix (5x5 grid representing node)
  const gridChars = [".", "o", "x", "#", "O"];
  let asciiArt = "";
  for (let r = 0; r < 5; r++) {
    for (let c = 0; c < 5; r < 4 && c === 4 ? asciiArt += "o\n" : null) {
      // Create interesting symmetric or algorithmic patterns
      const val = (r * countA + c * countG + (r + c) * countT) % gridChars.length;
      asciiArt += gridChars[val];
      c++;
    }
    if (r < 4) asciiArt += "\n";
  }

  return {
    id: `PX-${Math.random().toString(36).substr(2, 9).toUpperCase()}`,
    sequence,
    name,
    faction,
    type,
    vitality,
    attack,
    defense,
    speed,
    primaryWeapon,
    lore,
    asciiArt,
    discoveredAt: Date.now(),
    origin,
    telomeres: 100,
  };
}

export function getUnlockedMoves(sequence: string) {
  const moves: { type: 'healing' | 'evasive'; name: string; description: string }[] = [];
  if (!sequence || sequence.length <= 64) return moves;

  // Gene 1: Characters 64 to 72
  if (sequence.length >= 72) {
    const gene1 = sequence.slice(64, 72);
    let ga = 0;
    let tc = 0;
    for (const char of gene1) {
      if (char === 'A' || char === 'G') ga++;
      else if (char === 'T' || char === 'C') tc++;
    }
    if (ga >= tc) {
      moves.push({
        type: 'healing',
        name: 'BIO-DRAIN REPAIR',
        description: 'Repairs circuitry by siphoning 35 HP from defender.'
      });
    } else {
      moves.push({
        type: 'evasive',
        name: 'QUANTUM ESCAPE DEVIATION',
        description: 'Evasive shift. Negates next attack damage in this round.'
      });
    }
  }

  // Gene 2: Characters 72 to 80
  if (sequence.length >= 80) {
    const gene2 = sequence.slice(72, 80);
    const firstType = moves[0]?.type;
    if (firstType === 'healing') {
      moves.push({
        type: 'evasive',
        name: 'ELECTROMAGNETIC SHELL DEFLECT',
        description: 'Hardened shield. Negates defender damage for this round.'
      });
    } else {
      moves.push({
        type: 'healing',
        name: 'MICRO-PHAGE EXTRACTION',
        description: 'Siphons 35 HP from opponent to mend system hardware.'
      });
    }
  }

  return moves;
}

// Simulated predefined names and pools for P.O.X. bots
export const simulatedUids = [
  "BATTLE_BUG_7",
  "CYBER_GLITCH",
  "NANO_PHAGE_X",
  "TOXI_SWARM",
  "CONTAIN_UNIT_3",
  "X_VIRUS_MKT",
  "SLICER_GRID",
  "INFECTED_UNIT",
  "TRANSCEIVER_13",
  "POX_BOT_99",
];

export function createRandomNearbyUser(): NearbyUser {
  const uid = simulatedUids[Math.floor(Math.random() * simulatedUids.length)] + "_" + Math.floor(Math.random() * 900 + 100);
  const distance = Number((3 + Math.random() * 25).toFixed(1)); // inside 30ft
  
  // Create 2 random creatures they have for trade
  const creaturesAvailable: Creature[] = [
    constructProceduralCreature(generateRandom64Sequence(), `Owned by Client ${uid}`),
    constructProceduralCreature(generateRandom64Sequence(), `Owned by Client ${uid}`)
  ];
  
  return {
    uid,
    distance,
    activeTradePending: false,
    creaturesAvailable,
    targetSequence: generateRandom64Sequence(),
  };
}
