package com.example.genpox

import com.example.genpox.data.*
import org.junit.Assert.*
import org.junit.Test

class ConflictEngineTest {

    private fun createDummyCreature(
        id: String,
        name: String,
        faction: String,
        vitality: Int = 100,
        attack: Int = 30,
        defense: Int = 20,
        speed: Int = 15,
        sequence: String = "A".repeat(64),
        telomeres: Int = 100,
        isAutoDefender: Boolean = false,
        isAutoHacker: Boolean = false,
        heatRes: Double = 0.0,
        radRes: Double = 0.0,
        kineticRes: Double = 0.0,
    ): Creature = Creature(
        id = id,
        sequence = sequence,
        name = name,
        faction = faction,
        type = "Cyborg Hybrid",
        vitality = vitality,
        attack = attack,
        defense = defense,
        speed = speed,
        primaryWeapon = "Standard Claws",
        lore = "Procedural test creature.",
        asciiArt = "",
        discoveredAt = System.currentTimeMillis(),
        origin = "Local Compile",
        isAutoDefender = isAutoDefender,
        isAutoHacker = isAutoHacker,
        telomeres = telomeres,
        heatResistance = heatRes,
        radResistance = radRes,
        kineticResistance = kineticRes,
    )

    @Test
    fun testInitializeCombatantScalingAndResonance() {
        // Creature with 100% telomeres (Scale factor = 1.0)
        val c100 = createDummyCreature("c1", "Unit-1", "Infection", vitality = 120, defense = 40, speed = 20)
        val combatant100 = ConflictEngine.initializeCombatant(c100, "New Moon", 50.0)

        assertEquals(120, combatant100.maxHp)
        assertEquals(20, combatant100.speed)

        // Infection faction on New Moon has 0 resonance modifier
        assertEquals(40, combatant100.defense)

        // Creature with 50% telomeres (Scale factor = 0.25 + 0.75 * 0.5 = 0.625)
        val c50 = createDummyCreature("c2", "Unit-2", "Infection", vitality = 100, attack = 40, defense = 20, speed = 16, telomeres = 50)
        // Infection faction on Waxing Crescent has +15 resonance defense modifier
        val combatant50 = ConflictEngine.initializeCombatant(c50, "Waxing Crescent", 50.0)

        // Scaled stats:
        // HP: 100 * 0.625 = 63
        // Attack: 40 * 0.625 = 25
        // Defense: Base defense = 20 * 0.625 = 13. Waxing Crescent adds +15 -> 28.
        // Speed: 16 * 0.625 = 10
        assertEquals(63, combatant50.maxHp)
        assertEquals(25, combatant50.attack)
        assertEquals(28, combatant50.defense)
        assertEquals(10, combatant50.speed)
    }

    @Test
    fun testMovesetUnlocking() {
        // Creature with sequence length 80
        // Slot 1 (64..72): GGGGGGGG (all Gs) -> A+G = 8, T+C = 0 -> Bio-Drain Repair
        // Slot 2 (72..80): CCCCCCCC -> Alternative to Bio-Drain is Electromagnetic Shell Deflect
        val seq80 = "A".repeat(64) + "G".repeat(8) + "C".repeat(8)
        val creature = createDummyCreature("c3", "Unit-3", "Mech", sequence = seq80)

        val combatant = ConflictEngine.initializeCombatant(creature, "New Moon", 50.0)
        assertEquals(2, combatant.moveset.size)
        assertEquals("BIO_DRAIN_REPAIR", combatant.moveset[0])
        assertEquals("ELECTROMAGNETIC_SHELL_DEFLECT", combatant.moveset[1])
    }

    @Test
    fun testCreatureVsCreatureCombat() {
        // High speed attacker with high attack
        val attacker = createDummyCreature("att", "Attacker", "Parasite", vitality = 150, attack = 35, defense = 10, speed = 80)
        // Slow defender
        val defender = createDummyCreature("def", "Defender", "Containment", vitality = 80, attack = 15, defense = 5, speed = 10)

        val result = ConflictEngine.simulateCreatureVsCreature(attacker, defender, "New Moon", 50.0, seed = 1234L)

        assertTrue(result.totalTurns > 0)
        assertTrue(result.logs.isNotEmpty())
        assertTrue(result.wasAttackerVictor)
        assertEquals("Attacker", result.winnerName)
        assertTrue(result.finalAttackerHp > 0)
        assertEquals(0, result.finalDefenderHp)
    }

    @Test
    fun testGeographicHazardIntegration() {
        val anomaly = PoxAnomaly(
            id = "ANM-TEST",
            name = "Test Anomaly",
            lat = 40.7128,
            lng = -74.0060,
            gene = "XZYW?!$%",
            faction = "Infection",
            distance = 100.0,
            heatZoneDiameter = 1000.0, // Radius = 500 feet
            density = 0.8,
        )

        // Coordinates exactly at epicenter should be contained
        assertTrue(anomaly.contains(40.7128, -74.0060))
        // Distance to epicenter should be 0.0
        assertEquals(0.0, anomaly.distanceTo(40.7128, -74.0060), 0.1)

        val well = CelestialWell(
            id = "WELL-TEST",
            name = "Test Well",
            lat = 40.7128,
            lng = -74.0060,
            radiusFeet = 200.0,
            type = "GREENHOUSE",
            hazardPower = 1.2,
        )

        // Center should be contained
        assertTrue(well.contains(40.7128, -74.0060))
        assertEquals(1.2, well.getHazardPowerAt(40.7128, -74.0060, 15.0), 0.001)
        assertEquals("HEAT", well.hazardType)
    }

    @Test
    fun testCreatureVsEnvironmentMitigationAndPayload() {
        val well = CelestialWell(
            id = "WELL-TEST",
            name = "Test Well",
            lat = 40.7128,
            lng = -74.0060,
            radiusFeet = 1000.0,
            type = "MAGNETOSPHERIC", // Radiation type hazard
            hazardPower = 1.0,
        )

        // Unprotected creature
        val rawCreature = createDummyCreature("raw", "RawSpecimen", "Infection", telomeres = 100)
        val resultRaw = ConflictEngine.simulateCreatureVsEnvironment(
            rawCreature,
            well,
            40.7128,
            -74.0060,
            totalTicks = 10,
            lunarAge = 15.0,
            waveCoupling = 50.0,
        )

        // Protected creature (100% Radiation Resistance)
        val safeCreature = createDummyCreature("safe", "RadShield", "Infection", telomeres = 100, radRes = 1.0)
        val resultSafe = ConflictEngine.simulateCreatureVsEnvironment(
            safeCreature,
            well,
            40.7128,
            -74.0060,
            totalTicks = 10,
            lunarAge = 15.0,
            waveCoupling = 50.0,
        )

        assertTrue("Protected creature should suffer less telomere decay", resultSafe.finalTelomeres > resultRaw.finalTelomeres)
        assertEquals(100, resultSafe.finalTelomeres) // 100% resistance means 0 damage
        assertTrue(resultSafe.survived)
        assertTrue(resultSafe.harvestedPayload.contains("STOCK_G")) // Magnetospheric well returns STOCK_G
    }

    @Test
    fun testCreatureVsPlayerNodeFirewallAndRedirection() {
        val hacker = createDummyCreature("hack", "HackerBot", "Parasite", speed = 90, attack = 30)
        val baseComplexity = 40.0

        // Case 1: Undefended Node Hacking
        val resultUndefended = ConflictEngine.simulateCreatureVsPlayerNode(
            hacker = hacker,
            targetNodeId = "NODE-1",
            defender = null,
            vaultAutoDefenders = emptyList(),
            baseFirewallComplexity = baseComplexity,
            phaseName = "New Moon",
            waveCoupling = 50.0,
        )

        assertTrue(resultUndefended.success)
        assertNull(resultUndefended.directCombatResult)
        assertEquals(1, resultUndefended.siphonedPayload.size)

        // Case 2: Defended Node Redirection
        val defender = createDummyCreature("def", "DefenderShield", "Containment", vitality = 200, defense = 80, speed = 10)
        val resultDefended = ConflictEngine.simulateCreatureVsPlayerNode(
            hacker = hacker,
            targetNodeId = "NODE-2",
            defender = defender,
            vaultAutoDefenders = emptyList(),
            baseFirewallComplexity = baseComplexity,
            phaseName = "New Moon",
            waveCoupling = 50.0,
        )

        // Hacking should redirect, initiating combat
        assertNotNull(resultDefended.directCombatResult)
        assertEquals("DefenderShield", resultDefended.directCombatResult?.winnerName) // Defender is strong
        assertFalse(resultDefended.success) // Hacker lost, decryption failed
    }

    @Test
    fun testFirewallComplexityScaling() {
        val hacker = createDummyCreature("hack", "HackerBot", "Parasite", speed = 90, attack = 30)
        val baseComplexity = 100.0

        // Auto-defenders in the vault
        val defender1 = createDummyCreature("d1", "Def-1", "Containment", defense = 15, isAutoDefender = true)
        val defender2 = createDummyCreature("d2", "Def-2", "Containment", defense = 25, isAutoDefender = true)
        val nonAuto = createDummyCreature("d3", "Def-3", "Containment", defense = 50, isAutoDefender = false)

        val vaultDefenders = listOf(defender1, defender2, nonAuto)

        val result = ConflictEngine.simulateCreatureVsPlayerNode(
            hacker = hacker,
            targetNodeId = "NODE-3",
            defender = null,
            vaultAutoDefenders = vaultDefenders,
            baseFirewallComplexity = baseComplexity,
            phaseName = "New Moon",
            waveCoupling = 50.0,
        )

        // Check if complexity logs show Base (100.0) + (15 + 25) * 2.0 = 180.0
        val expectedComplexityLog = "Target Firewall Complexity: 180.0"
        assertTrue(result.logs.any { it.contains(expectedComplexityLog) })
    }
}
