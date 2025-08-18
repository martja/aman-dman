import no.vaccsca.amandman.service.util.WeatherUtils
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class WeatherTest {

    @Test
    fun `Should calculate standard temperature correctly`() {
        val expectedTemperature = WeatherUtils.getStandardTemperatureAt(35000)
        assertEquals(-54, expectedTemperature)
    }

}