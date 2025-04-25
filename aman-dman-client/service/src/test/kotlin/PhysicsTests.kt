import org.example.Wind
import org.example.util.PhysicsUtils
import kotlin.test.Test
import kotlin.test.assertEquals

class PhysicsTests {


    @Test
    fun`Convert IAS to TAS`() {

        assertEquals(
            107,
            PhysicsUtils.iasToTas(ias = 100, altitudeFt = 3000, tempCelsius = 20)
        )

        assertEquals(
            318,
            PhysicsUtils.iasToTas(ias = 250, altitudeFt = 15000, tempCelsius = -10)
        )
    }

    @Test
    fun`Convert TAS to IAS`() {
        assertEquals(
            100,
            PhysicsUtils.tasToIAS(tas = 107, altitudeFt = 3000, tempCelsius = 20)
        )

        assertEquals(
            250,
            PhysicsUtils.tasToIAS(tas = 318, altitudeFt = 15000, tempCelsius = -10)
        )
    }

    @Test
    fun `Convert MACH to IAS`() {
        val ias = PhysicsUtils.machToIAS(mach = 0.787f, altitudeFt = 35000, satCelsius = -56).toDouble()
        assertEquals(268.0, ias, 5.0)

        val ias2 = PhysicsUtils.machToIAS(mach = 0.416f, altitudeFt = 7000, satCelsius = -6).toDouble()
        assertEquals(240.0, ias2, 5.0)
    }

    @Test
    fun `Convert GS to TAS based on wind`() {
        // No wind
        assertEquals(
            100,
            PhysicsUtils.gsToTas(gs = 100, wind = Wind(0, 0), track = 0)
        )

        // Headwind
        assertEquals(
            110,
            PhysicsUtils.gsToTas(gs = 100, wind = Wind(0, 10), track = 0)
        )

        // Tailwind
        assertEquals(
            90,
            PhysicsUtils.gsToTas(gs = 100, wind = Wind(180, 10), track = 0)
        )

        // Crosswind
        assertEquals(
            100.0,
            PhysicsUtils.gsToTas(gs = 100, wind = Wind(90, 10), track = 0).toDouble(),
            1.0
        )
    }

    @Test
    fun `Convert TAS to GS`() {
        // No wind
        assertEquals(
            100,
            PhysicsUtils.tasToGs(tas = 100, wind = Wind(0, 0), track = 0)
        )

        // Headwind
        assertEquals(
            90,
            PhysicsUtils.tasToGs(tas = 100, wind = Wind(0, 10), track = 0)
        )

        // Tailwind
        assertEquals(
            110,
            PhysicsUtils.tasToGs(tas = 100, wind = Wind(180, 10), track = 0)
        )

        // Crosswind
        assertEquals(
            100.0,
            PhysicsUtils.tasToGs(tas = 100, wind = Wind(90, 10), track = 0).toDouble(),
            1.0
        )
    }

}