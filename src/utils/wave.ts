/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

// Synodic lunar month duration in days
export const LUNAR_MONTH_DAYS = 29.53059;

// June 15, 2026 is a real New Moon.
// Let's establish a stable reference new moon around noon on May 17, 2026
export const REFERENCE_NEW_MOON = new Date("2026-05-17T01:54:00Z");

export interface WaveConfig {
  pair: string;              // E.g., "GA", "AT", "CG"
  primary: string;           // "G"
  secondary: string;         // "A"
  isSuppressed: boolean;     // 50/50 chance of suppression in the designated week
  lunarAge: number;          // Days since last new moon
  isLunarInfluence: boolean; // Within new moon + 7 days
  phaseName: string;         // E.g., "New Moon", "Waxing Crescent", etc.
  isNewMoon: boolean;        // Exactly New Moon (apex)
  isFullMoon: boolean;        // Exactly Full Moon (apex)
  primaryMultiplier: number;  // Base 1.125, modified by lunar phase
  secondaryMultiplier: number;// Base 1.625, modified by lunar phase
}

/**
 * Deterministically generates a hash from a date string (YYYY-MM-DD)
 */
export function getDeterministicHash(dateStr: string): number {
  let hash = 0;
  for (let i = 0; i < dateStr.length; i++) {
    hash = (hash << 5) - hash + dateStr.charCodeAt(i);
    hash |= 0;
  }
  return Math.abs(hash);
}

/**
 * Calculates wave, lunar age, phase name, and suppression status for any date
 */
export function getDailyWaveConfig(date: Date): WaveConfig {
  const y = date.getFullYear();
  const m = date.getMonth();
  const d = date.getDate();
  const dateStr = `${y}-${(m + 1).toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}`;
  
  const hash = getDeterministicHash(dateStr);
  
  // Lunar calculations
  const diffDays = (date.getTime() - REFERENCE_NEW_MOON.getTime()) / (1000 * 60 * 60 * 24);
  const lunarAge = ((diffDays % LUNAR_MONTH_DAYS) + LUNAR_MONTH_DAYS) % LUNAR_MONTH_DAYS;
  
  // Every New Moon (lunarAge 0 to 1) and the 7 days following (up to 8.0 days from start) has general influence
  const isLunarInfluence = lunarAge < 8.0;

  // Let's determine the exact phases of the day
  let phaseName = "Waxing Crescent";
  let isNewMoon = false;
  let isFullMoon = false;

  // Define exact thresholds for the 8 standard lunar phases
  if (lunarAge < 1.0 || lunarAge > 28.53059) {
    phaseName = "New Moon";
    isNewMoon = true;
  } else if (lunarAge >= 1.0 && lunarAge < 6.38) {
    phaseName = "Waxing Crescent";
  } else if (lunarAge >= 6.38 && lunarAge < 8.38) {
    phaseName = "First Quarter";
  } else if (lunarAge >= 8.38 && lunarAge < 13.77) {
    phaseName = "Waxing Gibbous";
  } else if (lunarAge >= 13.77 && lunarAge < 15.77) {
    phaseName = "Full Moon";
    isFullMoon = true;
  } else if (lunarAge >= 15.77 && lunarAge < 21.15) {
    phaseName = "Waning Gibbous";
  } else if (lunarAge >= 21.15 && lunarAge < 23.15) {
    phaseName = "Third Quarter";
  } else {
    phaseName = "Waning Crescent";
  }

  // Predictable wave pair choice matching 1/4 chance for each starting letter
  const bases = ["A", "G", "T", "C"];
  const primaryIdx = hash % 4; // Equal 1/4 chance
  const secondaryIdx = (hash + 1 + (hash % 3)) % 4; // Different than primary
  
  const primary = bases[primaryIdx];
  const secondary = bases[secondaryIdx];
  const pair = primary + secondary;

  // "One full week of a calendar month, create a 50/50 chance for the wave to not occur."
  // Designate Week 3 of any month (Days 15 through 21) as the unstable period.
  let isSuppressed = false;
  if (d >= 15 && d <= 21) {
    isSuppressed = (hash % 100) < 50; // Stable, predetermined 50/50 chance
  }

  // Calculate spawn coefficients with precise moon modifiers
  // The new and full moon bonuses will now decay and accumulate from peak to trough across the intervening month
  // Base values: primary = 1.125, secondary = 1.625
  const moonAngle = (2 * Math.PI * lunarAge) / LUNAR_MONTH_DAYS - Math.PI / 2;
  const moonModifier = 0.0125 * Math.sin(moonAngle);

  let primaryMultiplier = 1.125 + moonModifier;
  let secondaryMultiplier = 1.625 + moonModifier;

  return {
    pair,
    primary,
    secondary,
    isSuppressed,
    lunarAge,
    isLunarInfluence,
    phaseName,
    isNewMoon,
    isFullMoon,
    primaryMultiplier,
    secondaryMultiplier
  };
}

/**
 * Custom weighted random codon generator influenced by the daily wave pair and its modifiers
 */
export function generateWaveGeneBlock(wave: WaveConfig | null): string {
  const bases = ["A", "G", "T", "C"];
  
  // If wave is suppressed or null, fall back to standard unweighted generation
  if (!wave || wave.isSuppressed) {
    let res = "";
    for (let i = 0; i < 8; i++) {
      res += bases[Math.floor(Math.random() * bases.length)];
    }
    return res;
  }

  const b1 = wave.primary;
  const b2 = wave.secondary;
  const m1 = wave.primaryMultiplier;
  const m2 = wave.secondaryMultiplier;

  let res = "";
  for (let i = 0; i < 8; i++) {
    const prevChar = i > 0 ? res[i - 1] : "";
    const weights: Record<string, number> = { A: 1.0, G: 1.0, T: 1.0, C: 1.0 };

    if (prevChar === b1) {
      // If primary catalyst just spawned, next letter has customized secondary multiplier (e.g. 1.625 or modified)
      weights[b2] = m2;
    } else {
      // Otherwise, primary catalyst has customized primary multiplier (e.g. 1.125 or modified)
      weights[b1] = m1;
    }

    // Weighted selector
    const sum = weights.A + weights.G + weights.T + weights.C;
    const r = Math.random() * sum;
    
    if (r < weights.A) {
      res += "A";
    } else if (r < weights.A + weights.G) {
      res += "G";
    } else if (r < weights.A + weights.G + weights.T) {
      res += "T";
    } else {
      res += "C";
    }
  }

  return res;
}
