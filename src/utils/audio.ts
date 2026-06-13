/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

// Synthesize retro 2000s electronic toys/POX audio chimes using standard Web Audio API
class SoundSnythesizer {
  private ctx: AudioContext | null = null;
  public isMuted: boolean = false;

  private initCtx() {
    if (!this.ctx) {
      this.ctx = new (window.AudioContext || (window as any).webkitAudioContext)();
    }
  }

  public playBeep(freq = 880, duration = 0.08, type: OscillatorType = "square") {
    if (this.isMuted) return;
    try {
      this.initCtx();
      if (!this.ctx) return;
      
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      
      osc.type = type;
      osc.frequency.setValueAtTime(freq, this.ctx.currentTime);
      
      gain.gain.setValueAtTime(0.04, this.ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + duration);
      
      osc.connect(gain);
      gain.connect(this.ctx.destination);
      
      osc.start();
      osc.stop(this.ctx.currentTime + duration);
    } catch (e) {
      console.warn("Audio Context blocked or failed to load:", e);
    }
  }

  // Play a cool techno-combinator tick
  public playCombinatorTick() {
    this.playBeep(440, 0.03, "sawtooth");
  }

  // Play a beautiful DNA synthesis success chime
  public playSynthesisSuccess() {
    if (this.isMuted) return;
    try {
      this.initCtx();
      if (!this.ctx) return;
      const now = this.ctx.currentTime;
      const notes = [523.25, 587.33, 659.25, 698.46, 783.99, 880.00]; // C5 D5 E5 F5 G5 A5
      notes.forEach((freq, idx) => {
        const osc = this.ctx!.createOscillator();
        const gain = this.ctx!.createGain();
        
        osc.type = "sine";
        osc.frequency.setValueAtTime(freq, now + idx * 0.08);
        
        gain.gain.setValueAtTime(0.05, now + idx * 0.08);
        gain.gain.exponentialRampToValueAtTime(0.001, now + idx * 0.08 + 0.15);
        
        osc.connect(gain);
        gain.connect(this.ctx!.destination);
        
        osc.start(now + idx * 0.08);
        osc.stop(now + idx * 0.08 + 0.15);
      });
    } catch (e) {}
  }

  // Alert buzzer for incoming trade
  public playAlertBuzzer() {
    if (this.isMuted) return;
    try {
      this.initCtx();
      if (!this.ctx) return;
      const now = this.ctx.currentTime;
      
      const osc1 = this.ctx.createOscillator();
      const osc2 = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      
      osc1.frequency.setValueAtTime(150, now);
      osc2.frequency.setValueAtTime(155, now);
      
      osc1.type = "sawtooth";
      osc2.type = "sawtooth";
      
      gain.gain.setValueAtTime(0.06, now);
      gain.gain.linearRampToValueAtTime(0.001, now + 0.4);
      
      osc1.connect(gain);
      osc2.connect(gain);
      gain.connect(this.ctx.destination);
      
      osc1.start();
      osc2.start();
      
      osc1.stop(now + 0.4);
      osc2.stop(now + 0.4);
    } catch (e) {}
  }

  // Reject trade / cancel buzzer
  public playReject() {
    this.playBeep(220, 0.25, "sawtooth");
  }

  // Play a dynamic sound sequence influenced by the creature's AGTC genome
  public playCreatureSequenceAudio(sequence: string) {
    if (this.isMuted) return;
    try {
      this.initCtx();
      if (!this.ctx) return;
      const now = this.ctx.currentTime;
      
      const chars = sequence.toUpperCase().replace(/[^AGTC]/g, "");
      if (chars.length === 0) return;
      
      const numNotes = Math.min(8, Math.max(4, Math.floor(chars.length / 8)));
      
      let acount = 0, gcount = 0, tcount = 0, ccount = 0;
      for (let i = 0; i < chars.length; i++) {
        if (chars[i] === 'A') acount++;
        else if (chars[i] === 'G') gcount++;
        else if (chars[i] === 'T') tcount++;
        else if (chars[i] === 'C') ccount++;
      }
      
      const maxCount = Math.max(acount, gcount, tcount, ccount);
      let oscType: OscillatorType = "sine";
      if (maxCount === acount) oscType = "sine";
      else if (maxCount === gcount) oscType = "triangle";
      else if (maxCount === tcount) oscType = "sawtooth";
      else if (maxCount === ccount) oscType = "square";

      const noteDelay = 0.08 + (acount % 3) * 0.02;
      
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
        const noteDuration = 0.06 + (gcount % 4) * 0.03;
        
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        
        osc.type = oscType;
        osc.frequency.setValueAtTime(freq, now + i * noteDelay);
        
        gain.gain.setValueAtTime(0.04, now + i * noteDelay);
        gain.gain.exponentialRampToValueAtTime(0.001, now + i * noteDelay + noteDuration);
        
        osc.connect(gain);
        gain.connect(this.ctx.destination);
        
        osc.start(now + i * noteDelay);
        osc.stop(now + i * noteDelay + noteDuration);
      }
    } catch (e) {
      console.warn("Dynamic creature audio emit failed:", e);
    }
  }

  // Trade Success Sweep
  public playTradeSuccess() {
    if (this.isMuted) return;
    try {
      this.initCtx();
      if (!this.ctx) return;
      const now = this.ctx.currentTime;
      
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      
      osc.type = "triangle";
      osc.frequency.setValueAtTime(300, now);
      osc.frequency.exponentialRampToValueAtTime(1200, now + 0.5);
      
      gain.gain.setValueAtTime(0.05, now);
      gain.gain.linearRampToValueAtTime(0.001, now + 0.5);
      
      osc.connect(gain);
      gain.connect(this.ctx.destination);
      
      osc.start();
      osc.stop(now + 0.5);
    } catch (e) {}
  }
}

export const sound = new SoundSnythesizer();
