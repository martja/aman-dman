import no.vaccsca.amandman.model.weather.Wind
import no.vaccsca.amandman.service.util.SpeedConversion
import kotlin.test.Test
import kotlin.test.assertEquals

class SpeedConversionTest {


    @Test
    fun`Convert IAS to TAS`() {

        assertEquals(
            107,
            SpeedConversion.iasToTAS(ias = 100, altitudeFt = 3000, tempCelsius = 20)
        )

        assertEquals(
            318,
            SpeedConversion.iasToTAS(ias = 250, altitudeFt = 15000, tempCelsius = -10)
        )
    }

    @Test
    fun`Convert TAS to IAS`() {
        assertEquals(
            100,
            SpeedConversion.tasToIAS(tas = 107, altitudeFt = 3000, tempCelsius = 20)
        )

        assertEquals(
            250,
            SpeedConversion.tasToIAS(tas = 318, altitudeFt = 15000, tempCelsius = -10)
        )
    }

    @Test
    fun `Convert MACH to IAS`() {
        val ias = SpeedConversion.machToIAS(mach = 0.783f, altitudeFt = 37000, satCelsius = -57).toDouble()
        assertEquals(254.0, ias, 5.0)

        val ias2 = SpeedConversion.machToIAS(mach = 0.416f, altitudeFt = 7000, satCelsius = -6).toDouble()
        assertEquals(240.0, ias2, 5.0)
    }

    @Test
    fun `Convert TAS to CAS at sea level with standard temp`() {
        val cas = SpeedConversion.tasToCAS(tasKts = 450.0, altitudeFt = 35000, satCelsius = -56)
        assertEquals(268.0, cas, 1.0) // Allow small tolerance
    }

    @Test
    fun `Convert GS to TAS based on wind`() {
        // No wind
        assertEquals(
            100,
            SpeedConversion.gsToTAS(gs = 100, wind = Wind(0, 0), track = 0)
        )

        // Headwind
        assertEquals(
            110,
            SpeedConversion.gsToTAS(gs = 100, wind = Wind(0, 10), track = 0)
        )

        // Tailwind
        assertEquals(
            90,
            SpeedConversion.gsToTAS(gs = 100, wind = Wind(180, 10), track = 0)
        )

        // Crosswind
        assertEquals(
            100.0,
            SpeedConversion.gsToTAS(gs = 100, wind = Wind(90, 10), track = 0).toDouble(),
            1.0
        )
    }

    @Test
    fun `Convert TAS to GS`() {
        // No wind
        assertEquals(
            100,
            SpeedConversion.tasToGS(tas = 100, wind = Wind(0, 0), track = 0)
        )

        // Headwind
        assertEquals(
            90,
            SpeedConversion.tasToGS(tas = 100, wind = Wind(0, 10), track = 0)
        )

        // Tailwind
        assertEquals(
            110,
            SpeedConversion.tasToGS(tas = 100, wind = Wind(180, 10), track = 0)
        )

        // Crosswind
        assertEquals(
            100.0,
            SpeedConversion.tasToGS(tas = 100, wind = Wind(90, 10), track = 0).toDouble(),
            1.0
        )
    }

}