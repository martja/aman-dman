import org.example.EstimationService.toRunwayArrivalOccurrence
import org.example.entities.navigation.star.Star
import org.example.entities.navigation.star.StarFix
import org.example.integration.entities.ArrivalJson
import org.example.integration.entities.FixPointJson
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals

class DescentTrajectoryServiceTest {

    val star = Star(
        id="INREX4M",
        airport="ENGM",
        runway="19L",
        fixes= listOf(
            StarFix(id="INREX", typicalAltitude=null, typicalSpeedIas=250),
            StarFix(id="GM418", typicalAltitude=11000, typicalSpeedIas=220),
            StarFix(id="TITLA", typicalAltitude=5000, typicalSpeedIas=200),
            StarFix(id="OSPAD", typicalAltitude=4000, typicalSpeedIas=180),
            StarFix(id="XIVTA", typicalAltitude=3500, typicalSpeedIas=170),
            StarFix(id="ENGM", typicalAltitude=700, typicalSpeedIas=null)
        ))

    val arrivalJson =
        ArrivalJson(
            callsign="SAS4411",
            icaoType="A20N",
            assignedRunway="19L",
            assignedStar="INREX4M",
            assignedDirect=null,
            trackingController=null,
            scratchPad=null,
            latitude=62.09387969970703,
            longitude=11.931380271911621,
            flightLevel=38000,
            pressureAltitude=38195,
            groundSpeed=449,
            track=0,
            route= listOf(
                FixPointJson(name="ENTC", isOnStar=false, latitude=69.681389, longitude=18.917778000000002, isPassed=true),
                FixPointJson(name="TC707", isOnStar=false, latitude=69.57579388888888, longitude=18.847716944444443, isPassed=true),
                FixPointJson(name="IPTUK", isOnStar=false, latitude=69.33083277777777, longitude=18.486110833333335, isPassed=true),
                FixPointJson(name="KAMPE", isOnStar=false, latitude=68.89629694444446, longitude=17.955566944444445, isPassed=true),
                FixPointJson(name="AMIMO", isOnStar=false, latitude=67.875, longitude=16.441388888888888, isPassed=true),
                FixPointJson(name="GUBAV", isOnStar=false, latitude=65.773333, longitude=13.944721999999999, isPassed=true),
                FixPointJson(name="TUXOT", isOnStar=false, latitude=63.977582999999996, longitude=12.321442000000001, isPassed=true),
                FixPointJson(name="SOMUB", isOnStar=false, latitude=63.349166999999994, longitude=11.805000000000001, isPassed=true),
                FixPointJson(name="ABUNO", isOnStar=false, latitude=62.0, longitude=11.940256, isPassed=false),
                FixPointJson(name="INREX", isOnStar=false, latitude=61.01388899999999, longitude=12.216667, isPassed=false),
                FixPointJson(name="IXUMA", isOnStar=true, latitude=60.819027777777784, longitude=11.989721944444444, isPassed=false),
                FixPointJson(name="GM418", isOnStar=true, latitude=60.65119388888889, longitude=11.7955, isPassed=false),
                FixPointJson(name="TITLA", isOnStar=true, latitude=60.42136083333333, longitude=11.402721944444444, isPassed=false),
                FixPointJson(name="OSPAD", isOnStar=true, latitude=60.40099194444444, longitude=11.238730833333333, isPassed=false),
                FixPointJson(name="XIVTA", isOnStar=true, latitude=60.34008277777778, longitude=11.203132777777776, isPassed=false),
                FixPointJson(name="GME40", isOnStar=true, latitude=60.26238277777778, longitude=11.157691944444444, isPassed=false),
                FixPointJson(name="ENGM", isOnStar=false, latitude=60.20277800000001, longitude=11.083889000000001, isPassed=false)
            ),
            arrivalAirportIcao="ENGM"
        )

    @Test
    fun `Descent trajectory should contain all fixes that has not been passed`() {
        val descentTrajectory = arrivalJson.toRunwayArrivalOccurrence(star, null)!!.descentTrajectory

        val remainingFixesOnRoute = arrivalJson.route.filter { !it.isPassed }.map { it.name }
        val fixesOnDescentTrajectory = descentTrajectory.mapNotNull { it.fixId }

        assertEquals(fixesOnDescentTrajectory, remainingFixesOnRoute, "Descent trajectory should contain all fixes that has not been passed")
    }

    @Test
    fun `Estimated TAS should not jump by more than 10 knots`() {
        val descentTrajectory = arrivalJson.toRunwayArrivalOccurrence(star, null)!!.descentTrajectory

        val tasList = descentTrajectory.map { it.tas }
        val isJumping = tasList.zipWithNext().any { (prev, next) -> abs(prev - next) > 10 }
        assertEquals(false, isJumping, "TAS should not jump by more than 10 knots")
    }

    @Test
    fun `Estimated IAS should not jump by more than 5 knots`() {
        val descentTrajectory = arrivalJson.toRunwayArrivalOccurrence(star, null)!!.descentTrajectory

        val iasList = descentTrajectory.map { it.ias }
        val isJumping = iasList.zipWithNext().any { (prev, next) -> abs(prev - next) > 5 }
        assertEquals(false, isJumping, "IAS should not jump by more than 5 knots")
    }

    @Test
    fun `Estimated IAS should never exceed typical speed on STAR point`() {
        val descentTrajectory = arrivalJson
            .toRunwayArrivalOccurrence(star, null)!!
            .descentTrajectory.filter { it.fixId != null }

        assertEquals(descentTrajectory.size, 9)

        val isExceeding = descentTrajectory
            .filter { it.fixId != null }
            .any { point ->
                val starFix = star.fixes.find { it.id == point.fixId }
                if (starFix?.typicalSpeedIas == null) return@any false
                point.ias > starFix.typicalSpeedIas!!
            }

        assertEquals(false, isExceeding, "IAS should not exceed typical speed on STAR point")
    }

    @Test
    fun `Trajectory points that have a fix id should have the same coordinates as the corresponding fix on the aircraft's route`() {
        val descentTrajectory = arrivalJson
            .toRunwayArrivalOccurrence(star, null)!!
            .descentTrajectory
            .filter { it.fixId != null }

        val isMatching = descentTrajectory.all { point ->
            val routeFix = arrivalJson.route.find { it.name == point.fixId }!!
            point.position.lat == routeFix.latitude && point.position.lon == routeFix.longitude
        }

        assertEquals(true, isMatching, "Coordinates should match the fix coordinates")
    }

    @Test
    fun `Estimated IAS should not be more than 250 below FL100`() {
        val descentTrajectory = arrivalJson.toRunwayArrivalOccurrence(star, null)!!.descentTrajectory

        val isExceeding = descentTrajectory.any { it.altitude < 10_000 && it.ias > 250 }
        assertEquals(false, isExceeding, "IAS should not exceed 250 below FL100")
    }

    @Test
    fun `Altitude should never be increasing`() {
        val descentTrajectory = arrivalJson.toRunwayArrivalOccurrence(star, null)!!.descentTrajectory

        val altitudeList = descentTrajectory.map { it.altitude }
        val isIncreasing = altitudeList.zipWithNext().any { (prev, next) -> prev < next }
        assertEquals(false, isIncreasing, "Altitude should never be increasing")
    }

}