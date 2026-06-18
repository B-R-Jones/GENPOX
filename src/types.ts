/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

export interface GeneSequence {
  sequence: string; // Exactly 8 characters of A, G, T, C
  count: number;    // Quantity owned, acts as currency
  discoveredAt: number; // Timestamp
}

export interface Creature {
  id: string; // Unique identifier or 64-char sequence
  sequence: string; // 64-character AGTC
  name: string;
  faction: 'Infection' | 'Mech' | 'Parasite' | 'Containment';
  type: string;
  vitality: number;
  attack: number;
  defense: number;
  speed: number;
  primaryWeapon: string;
  lore: string;
  asciiArt: string; 
  discoveredAt: number;
  origin: string; // "Created" or "Traded (from UID)"
  isAutoHacker?: boolean;
  appendedGenes?: string[];
  isFavorite?: boolean;
  telomeres?: number; // Current telomere length out of 100
  isFullCoherence?: boolean;
  coherenceType?: 'Natural' | 'Forced';
  isMutated?: boolean;
  originalSequence?: string;
}

export interface NearbyUser {
  uid: string;
  distance: number; // inside 30ft, e.g., 8.4, 23.1
  activeTradePending: boolean;
  creaturesAvailable: Creature[]; // Simulated library for them
  targetSequence?: string; // 64-character target DNA pathway for G.E.N. Network matching
}

export interface TradeSession {
  id: string;
  partner: NearbyUser;
  status: 'idle' | 'requested_by_me' | 'incoming_request' | 'accepted' | 'rejected' | 'completed';
  partnerOfferedCreature?: Creature;
  playerOfferedCreature?: Creature;
}
