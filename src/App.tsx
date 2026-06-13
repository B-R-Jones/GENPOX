/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from "react";
import PoxConsole from "./components/PoxConsole";

export default function App() {
  const [windowWidth, setWindowWidth] = useState<number>(typeof window !== "undefined" ? window.innerWidth : 1024);
  const [windowHeight, setWindowHeight] = useState<number>(typeof window !== "undefined" ? window.innerHeight : 768);

  // Dynamic system/viewport detector
  useEffect(() => {
    const handleResize = () => {
      setWindowWidth(window.innerWidth);
      setWindowHeight(window.innerHeight);
    };

    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  // Proactive device and posture autodetection definitions:
  const [deviceInfo, setDeviceInfo] = useState(() => {
    return {
      name: "Detecting Device...",
      posture: "Detecting Posture...",
      ratio: 1.33,
      isMobile: false,
      isFoldInner: false
    };
  });

  useEffect(() => {
    const handleResize = () => {
      setWindowWidth(window.innerWidth);
      setWindowHeight(window.innerHeight);

      const ua = typeof navigator !== "undefined" ? navigator.userAgent : "";
      let name = "Generic Desktop Terminal";
      if (/Android.*Mobile|Mobile|iPhone|iPod|BlackBerry|IEMobile|Opera Mini/i.test(ua)) {
        if (/Pixel/i.test(ua)) {
          name = "Google Pixel Device";
        } else if (/Samsung|Galaxy/i.test(ua)) {
          name = "Samsung Galaxy Device";
        } else if (/iPhone/i.test(ua)) {
          name = "Apple iPhone Device";
        } else {
          name = "Mobile Touch Terminal";
        }
      } else if (/iPad|Android|Tablet/i.test(ua)) {
        if (/iPad/i.test(ua)) {
          name = "Apple iPad Unit";
        } else {
          name = "Android Tablet Slate";
        }
      } else if (/Macintosh/i.test(ua)) {
        name = "Apple Macintosh Station";
      } else if (/Windows/i.test(ua)) {
        name = "Windows Workstation Node";
      } else if (/Linux/i.test(ua)) {
        name = "Linux Computer Engine";
      }

      const width = window.innerWidth;
      const height = window.innerHeight;
      const r = width / height;

      let posture = "Standard Landscape Mode";
      let isMob = width < 680;
      let isInnerFold = false;

      if (width < 680) {
        posture = r < 0.6 ? "Tall Portrait Mode" : "Compact Handheld Mode";
      } else if (width >= 680 && width <= 1040) {
        if (Math.abs(r - 1) < 0.45) {
          posture = "Dual-Display Unfolded Posture";
          isInnerFold = true;
        } else {
          posture = "Symmetrical Tablet Slate";
        }
      } else {
        if (r > 2.0) {
          posture = "Ultra-Wide Panoramic View";
        } else if (r < 0.8) {
          posture = "Rotated Vertical Display";
        } else {
          posture = "Standard Landscape Desktop";
        }
      }

      setDeviceInfo({
        name,
        posture,
        ratio: r,
        isMobile: isMob,
        isFoldInner: isInnerFold
      });
    };

    handleResize();
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  const isMobile = deviceInfo.isMobile;
  const isFoldInner = deviceInfo.isFoldInner;
  const viewportProfile = isFoldInner ? "fold_inner" : isMobile ? "mobile" : "desktop";

  return (
    <div className="min-h-screen bg-[#060b07] bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-[#07130c] via-[#030604] to-[#010201] font-sans antialiased text-[#00ff66] selection:bg-[#00ff66]/10 selection:text-white relative overflow-hidden flex flex-col justify-between h-screen w-screen">
      {/* Background cyber grid pattern */}
      <div className="absolute inset-0 bg-[linear-gradient(rgba(0,255,102,0.008)_1px,transparent_1px),linear-gradient(90deg,rgba(0,255,102,0.008)_1px,transparent_1px)] bg-[size:40px_40px] pointer-events-none z-0" />

      {/* Dynamic Halo ambient shadows */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 w-[700px] h-[300px] bg-emerald-950/20 rounded-full filter blur-[110px] pointer-events-none z-0" />

      {/* Main Container: Adapts automatically depending on detected viewport size */}
      <main className="relative z-10 flex-1 flex items-center justify-center overflow-hidden w-full h-full p-0 sm:p-2 md:p-4">
        <PoxConsole 
          isMobileView={isMobile} 
          viewportProfile={viewportProfile} 
          detectedDeviceName={deviceInfo.name}
          detectedPosture={deviceInfo.posture}
          detectedRatio={deviceInfo.ratio}
        />
      </main>

      {/* Simple, unobtrusive subtle status footer indicator showing active auto-detected environment */}
      <footer className="relative z-10 py-2 px-4 bg-neutral-950 border-t border-green-950/40 text-[9px] font-mono text-neutral-500 flex justify-between items-center sm:flex-row flex-col gap-1 select-none">
        <div className="flex items-center gap-1.5 text-emerald-500/70">
          <span className="w-1.5 h-1.5 rounded-full bg-[#00ff66] animate-pulse" />
          <span>AUTONOMOUS HARDWARE DETECTOR ENGAGED</span>
        </div>
        <div className="uppercase tracking-wider flex items-center gap-2">
          <span>DRV: <strong className="text-white">v1.4.1</strong></span>
          <span>•</span>
          <span>DEV: <strong className="text-white">{deviceInfo.name}</strong></span>
          <span>•</span>
          <span>POSTURE: <strong className="text-[#00ff66]">{deviceInfo.posture}</strong></span>
          <span>•</span>
          <span>RATIO: <strong className="text-neutral-400">{deviceInfo.ratio.toFixed(3)}</strong></span>
        </div>
      </footer>
    </div>
  );
}
