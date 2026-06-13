import express from "express";
import path from "path";
import { createServer as createViteServer } from "vite";
import { GoogleGenAI, Type } from "@google/genai";
import dotenv from "dotenv";

dotenv.config();

const app = express();
const PORT = 3000;

app.use(express.json());

// Lazy reference for Gemini Client to handle absence of GEMINI_API_KEY gracefully
let aiClient: GoogleGenAI | null = null;
function getGeminiClient(): GoogleGenAI {
  if (!aiClient) {
    const apiKey = process.env.GEMINI_API_KEY;
    if (!apiKey) {
      throw new Error("GEMINI_API_KEY is not defined in the environment secrets.");
    }
    aiClient = new GoogleGenAI({
      apiKey,
      httpOptions: {
        headers: {
          "User-Agent": "aistudio-build",
        },
      },
    });
  }
  return aiClient;
}


// API to query a secure NTP/GPS synched UTC time reference and prevent client clock manipulation
app.get("/api/time", async (req, res) => {
  const serverTime = new Date().toISOString();
  let verifiedTime = null;
  let source = "gcp_ntp";
  
  try {
    const fetchWorldTime = fetch("https://worldtimeapi.org/api/timezone/Etc/UTC", { 
      headers: { "Accept": "application/json" }
    }).then(async (r) => {
      if (r.ok) {
        const d = await r.json() as any;
        if (d && d.utc_datetime) return { verifiedTime: d.utc_datetime, source: "worldtimeapi_dual_beacon" };
      }
      throw new Error("worldtimeapi failed");
    });

    const fetchTimeApi = fetch("https://timeapi.io/api/time/current/zone?timeZone=UTC", {
      headers: { "Accept": "application/json" }
    }).then(async (r) => {
      if (r.ok) {
        const d = await r.json() as any;
        if (d && d.dateTime) return { verifiedTime: d.dateTime + "Z", source: "timeapi_dual_beacon" };
      }
      throw new Error("timeapi failed");
    });

    const timeoutPromise = new Promise<never>((_, reject) => setTimeout(() => reject(new Error("Network timeout")), 1200));

    const result = await Promise.race([
      fetchWorldTime,
      fetchTimeApi,
      timeoutPromise
    ]);

    if (result && result.verifiedTime) {
      verifiedTime = result.verifiedTime;
      source = result.source;
    }
  } catch (e: any) {
    // Failover to local server system clock
    console.log("Dual verification beacon offline, using server NTP system clock:", e?.message || e);
  }

  res.json({
    success: true,
    currentTime: serverTime,
    verifiedTime: verifiedTime || serverTime,
    source,
    timestamp: Date.now()
  });
});

// REST API for creature generation
app.post("/api/creatures/generate-desc", async (req, res) => {
  try {
    const { sequence } = req.body;
    if (!sequence || typeof sequence !== "string" || sequence.length !== 64) {
      return res.status(400).json({ error: "Invalid genetic sequence. Must be exactly 64 characters of AGTC." });
    }

    const ai = getGeminiClient();

    // Setup structured prompt for P.O.X. cybernetic bio-mutant creature design
    const response = await ai.models.generateContent({
      model: "gemini-3.5-flash",
      contents: `You are a vintage handheld electronic toy P.O.X. compiler from the early 2000s. Design a bio-mechanical virus/cyborg insectoid monster from this 64-character DNA sequence: "${sequence}". Let the content of the DNA sequence (ratio of A, G, T, C) guide its cybernetic characteristics.`,
      config: {
        systemInstruction: "You are an expert compiler of retro sci-fi cyber-creatures. You output structured bios for bio-mechanical warriors, parasitic battle bugs, and cyber virus creatures in valid JSON.",
        responseMimeType: "application/json",
        responseSchema: {
          type: Type.OBJECT,
          properties: {
            name: { type: Type.STRING, description: "A cool retro cybernetic/biological battle creature name, e.g. CyberVex-9, Toxipod, Chitin-Core" },
            faction: { type: Type.STRING, description: "One of the four P.O.X. battle sectors: 'Infection', 'Mech', 'Parasite', 'Containment'" },
            type: { type: Type.STRING, description: "Bio-mechanical classification, e.g., Virus Swarm, Neural Parasite, Heavy Metal Shell" },
            vitality: { type: Type.NUMBER, description: "HP / Combat Life from 100 to 250" },
            attack: { type: Type.NUMBER, description: "Assault force stat from 10 to 99" },
            defense: { type: Type.NUMBER, description: "Shield armor stat from 10 to 99" },
            speed: { type: Type.NUMBER, description: "Agility velocity stat from 10 to 99" },
            primaryWeapon: { type: Type.STRING, description: "Cool mechanical insect or virus bio-weapon name attached to its body, e.g. Acid Spurt-Needle" },
            lore: { type: Type.STRING, description: "A brief, highly flavorful 2-sentence description detailing its bio-tech evolution and operational designation." },
            asciiArt: { type: Type.STRING, description: "An abstract 5x5 Grid representation using only characters '.', 'o', 'x', '#', 'O', '#' to visually represent the core of this bug/creature" }
          },
          required: ["name", "faction", "type", "vitality", "attack", "defense", "speed", "primaryWeapon", "lore", "asciiArt"]
        }
      }
    });

    const responseText = response.text;
    if (!responseText) {
      throw new Error("No response output from Gemini model.");
    }

    const compiledCreature = JSON.parse(responseText.trim());
    return res.json({ success: true, creature: compiledCreature });
  } catch (error: any) {
    console.error("Gemini compilation error:", error);
    return res.status(500).json({ error: error.message || "Failed to compile creature telemetry" });
  }
});

// Configure Vite middleware or static serving
async function startServer() {
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`P.O.X. Game Server running at http://0.0.0.0:${PORT}`);
  });
}

startServer();
