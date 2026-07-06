package com.example.genpox

import com.example.genpox.data.ConflictEngine
import com.example.genpox.data.DeploymentEngine
import com.example.genpox.data.Creature
import com.example.genpox.data.PoxAnomaly
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class ExtremeEngineTest {

    @Test
    fun testConflictEngineExtremeLowTelomeres() {
        // Create a specimen with 10% telomeres (extreme decay)
        val attacker = Creature(
            id = "specimen-decayed",
            name = "Decayed Specimen",
            sequence = "AAAAAAGGGGGGTIIITTTTCCCCX", // contains some anomalous codon patterns
            faction = "Infection",
            type = "Infector",
            vitality = 100,
            attack = 80,
            defense = 40,
            speed = 50,
            primaryWeapon = "Acid Spit",
            lore = "A decayed biological specimen",
            asciiArt = "[Bio]",
            discoveredAt = 123456789L,
            origin = "Sector 4",
            telomeres = 10
        )

        // Factor = 0.25 + 0.75 * 0.10 = 0.325
        // Expected HP = max(10, round(100 * 0.325)) = 33
        // Expected ATK = max(5, round(80 * 0.325)) = 26
        // Expected DEF base = max(5, round(40 * 0.325)) = 13
        // Expected SPD = max(5, round(50 * 0.325)) = 16

        val combatant = ConflictEngine.initializeCombatant(attacker, "New Moon", 80.0)

        assertEquals(33, combatant.maxHp)
        assertEquals(33, combatant.currentHp)
        assertEquals(26, combatant.attack)
        assertEquals(16, combatant.speed)
    }

    @Test
    fun testConflictEngineAdverseMoonPhases() {
        val infectionCreature = Creature(
            id = "infection-fighter",
            name = "Infection Specimen",
            sequence = "AAAAAAAAAAAAAAAAGGGGGGGGGGGGGGGGTTTTTTTTTTTTTTTCCCCCCCCCCCCCCCCC",
            faction = "Infection",
            type = "Titan",
            vitality = 100,
            attack = 50,
            defense = 40,
            speed = 50,
            primaryWeapon = "Claws",
            lore = "Infection sentinel",
            asciiArt = "[Infection]",
            discoveredAt = 123456789L,
            origin = "Swamp",
            telomeres = 100
        )

        // Infection gets +15 defense in Waxing Crescent, -10 in Waning Crescent.
        val baseDef = 40 // telomeres = 100 => factor = 1.0 => base defense = 40.
        
        val combatantWaxing = ConflictEngine.initializeCombatant(infectionCreature, "Waxing Crescent", 80.0)
        val combatantWaning = ConflictEngine.initializeCombatant(infectionCreature, "Waning Crescent", 80.0)

        assertEquals(baseDef + 15, combatantWaxing.defense)
        assertEquals(baseDef - 10, combatantWaning.defense)
    }

    @Test
    fun testConflictEngineCombatUnderExtremeDecay() {
        val c1 = Creature(
            id = "low-hp-1",
            name = "Attacker Low Telomeres",
            sequence = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            faction = "Mech",
            type = "Drone",
            vitality = 50,
            attack = 30,
            defense = 20,
            speed = 40,
            primaryWeapon = "Laser",
            lore = "Rusty drone",
            asciiArt = "[Drone]",
            discoveredAt = 123456789L,
            origin = "Factory",
            telomeres = 10
        )
        val c2 = Creature(
            id = "low-hp-2",
            name = "Defender Low Telomeres",
            sequence = "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
            faction = "Mech",
            type = "Turret",
            vitality = 50,
            attack = 30,
            defense = 20,
            speed = 30,
            primaryWeapon = "Blaster",
            lore = "Rusty turret",
            asciiArt = "[Turret]",
            discoveredAt = 123456789L,
            origin = "Factory",
            telomeres = 10
        )

        // Combat with extremely low HP (they will only have around 16 HP)
        val result = ConflictEngine.simulateCreatureVsCreature(c1, c2, "First Quarter", 90.0, seed = 123L)

        assertTrue("Combat logs should be populated", result.logs.isNotEmpty())
        assertTrue("Winner should be set", result.winnerName.isNotEmpty())
        assertTrue("Turns should be within bounds", result.totalTurns in 1..50)
    }

    @Test
    fun testDeploymentEngineExtremeDensityOverlap() {
        // Set up overlapping high-density anomalies
        val lat = 37.7749
        val lng = -122.4194

        val anom1 = PoxAnomaly(
            id = "anom-dense-1",
            name = "Singularity 1",
            lat = lat + 0.0001, // extremely close (approx 36 feet)
            lng = lng,
            gene = "GGGGGGGG",
            faction = "Parasite",
            distance = 36.0,
            heatZoneDiameter = 100.0, // boundary = 50 feet
            density = 80.0
        )
        val anom2 = PoxAnomaly(
            id = "anom-dense-2",
            name = "Singularity 2",
            lat = lat - 0.0001, // extremely close
            lng = lng,
            gene = "CCCCCCCC",
            faction = "Parasite",
            distance = 36.0,
            heatZoneDiameter = 150.0, // boundary = 75 feet
            density = 90.0
        )

        val combinedDensity = DeploymentEngine.calculateCombinedWaveDensity(lat, lng, listOf(anom1, anom2))
        
        // Output should integrate both densities and fall off with distance.
        assertTrue("Combined wave density should be non-zero", combinedDensity != 0.0)
    }

    @Test
    fun testDeploymentEngineStallDepthBounds() {
        // High effective defense should result in high stall depth, but coerced to 100.0
        val effectiveDef = 500
        val boundaryRadius = 20.0
        val lunarAge = 14.7 // Full Moon (cos = -1.0)
        
        val stallDepth = DeploymentEngine.calculateStallDepth(effectiveDef, boundaryRadius, lunarAge)
        assertEquals(100.0, stallDepth, 0.001)

        // Negative effective defense (extreme debuff) should be coerced to 0.0
        val negativeDef = -100
        val stallDepthZero = DeploymentEngine.calculateStallDepth(negativeDef, boundaryRadius, lunarAge)
        assertEquals(0.0, stallDepthZero, 0.001)
    }
}
