import no.vaccsca.amandman.common.LatLng
import no.vaccsca.amandman.common.distanceTo
import no.vaccsca.amandman.common.dto.navigation.star.Star
import no.vaccsca.amandman.common.dto.navigation.star.StarFix
import integration.entities.ArrivalJson
import integration.entities.FixPointJson
import no.vaccsca.amandman.service.EstimationService.toRunwayArrivalEvent
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class DescentTrajectoryServiceTest {

    val inrex4m = Star(
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
            arrivalAirportIcao="ENGM",
            450
        )

    val arrivalJson2 =
        ArrivalJson(
            callsign="EZY9",
            icaoType="A320",
            assignedRunway="19L",
            assignedStar="ESEBA4M",
            assignedDirect=null,
            trackingController=null,
            scratchPad=null,
            latitude=59.77381134033203,
            longitude=14.915430068969727,
            flightLevel=31222,
            pressureAltitude=31476,
            groundSpeed=407,
            track=0,
            route= listOf(
                FixPointJson(name="ESSA", isOnStar=false, latitude=59.65194399999999, longitude=17.918611, isPassed=true),
                FixPointJson(name="ARS", isOnStar=false, latitude=59.586222, longitude=16.650333, isPassed=true),
                FixPointJson(name="BEDLA", isOnStar=false, latitude=59.628944000000004, longitude=16.225028, isPassed=true),
                FixPointJson(name="IBGAX", isOnStar=false, latitude=59.72222200000001, longitude=15.395833000000001, isPassed=true),
                FixPointJson(name="EBURI", isOnStar=false, latitude=59.8, longitude=14.660556000000001, isPassed=false),
                FixPointJson(name="TEKVA", isOnStar=false, latitude=59.984722, longitude=12.719444000000001, isPassed=false),
                FixPointJson(name="ESEBA", isOnStar=false, latitude=60.012778000000004, longitude=12.392222, isPassed=false),
                FixPointJson(name="KEGET", isOnStar=true, latitude=60.08972194444445, longitude=12.254888888888889, isPassed=false),
                FixPointJson(name="GM422", isOnStar=true, latitude=60.26261083333333, longitude=11.954832777777778, isPassed=false),
                FixPointJson(name="GM423", isOnStar=true, latitude=60.36525, longitude=12.033749722222222, isPassed=false),
                FixPointJson(name="TITLA", isOnStar=true, latitude=60.42136083333333, longitude=11.402721944444444, isPassed=false),
                FixPointJson(name="OSPAD", isOnStar=true, latitude=60.40099194444444, longitude=11.238730833333333, isPassed=false),
                FixPointJson(name="XIVTA", isOnStar=true, latitude=60.34008277777778, longitude=11.203132777777776, isPassed=false),
                FixPointJson(name="GME40", isOnStar=true, latitude=60.26238277777778, longitude=11.157691944444444, isPassed=false),
                FixPointJson(name="ENGM", isOnStar=false, latitude=60.20277800000001, longitude=11.083889000000001, isPassed=false)
            ),
            arrivalAirportIcao="ENGM",
            450
        )

    val adopi3m =
        Star(
            id="ADOPI3M",
            airport="ENGM",
            runway="19L",
            fixes=listOf(
                StarFix(id="ADOPI", typicalAltitude=null, typicalSpeedIas=250),
                StarFix(id="GM428", typicalAltitude=10000, typicalSpeedIas=220),
                StarFix(id="BAVAD", typicalAltitude=5000, typicalSpeedIas=200),
                StarFix(id="OSPAD", typicalAltitude=4000, typicalSpeedIas=180),
                StarFix(id="XIVTA", typicalAltitude=3500, typicalSpeedIas=170),
                StarFix(id="ENGM", typicalAltitude=700, typicalSpeedIas=null)
            )
        )

    val arrivalWithDirectRouting = ArrivalJson(
        callsign="SRR22X",
        icaoType="B77L",
        assignedRunway="19L",
        assignedStar="ADOPI3M",
        assignedDirect="BAVAD",
        trackingController="GWR",
        scratchPad=null,
        latitude=60.36001968383789,
        longitude=9.892060279846191,
        flightLevel=15670,
        pressureAltitude=15851,
        groundSpeed=402,
        track=0,
        route=listOf(
            FixPointJson(name="EIDW", isOnStar=false, latitude=53.421389, longitude=-6.2700000000000005, isPassed=true),
            FixPointJson(name="ROTEV", isOnStar=false, latitude=54.028806, longitude=-6.066222000000001, isPassed=true),
            FixPointJson(name="GOTNA", isOnStar=false, latitude=54.594842, longitude=-5.598013999999999, isPassed=true),
            FixPointJson(name="TRN", isOnStar=false, latitude=55.313410999999995, longitude=-4.783864, isPassed=true),
            FixPointJson(name="KLONN", isOnStar=false, latitude=58.390122000000005, longitude=2.828922, isPassed=true),
            FixPointJson(name="ADOPI", isOnStar=false, latitude=60.323611, longitude=9.383333, isPassed=true),
            FixPointJson(name="EXUDA", isOnStar=true, latitude=60.39744388888889, longitude=9.926332777777777, isPassed=true),
            FixPointJson(name="GM428", isOnStar=true, latitude=60.46558277777778, longitude=10.442388888888889, isPassed=true),
            FixPointJson(name="GM429", isOnStar=true, latitude=60.572943888888894, longitude=10.479721944444444, isPassed=true),
            FixPointJson(name="BAVAD", isOnStar=true, latitude=60.46611083333334, longitude=11.08444388888889, isPassed=false),
            FixPointJson(name="OSPAD", isOnStar=true, latitude=60.40099194444444, longitude=11.238730833333333, isPassed=false),
            FixPointJson(name="XIVTA", isOnStar=true, latitude=60.34008277777778, longitude=11.203132777777776, isPassed=false),
            FixPointJson(name="GME40", isOnStar=true, latitude=60.26238277777778, longitude=11.157691944444444, isPassed=false),
            FixPointJson(name="ENGM", isOnStar=false, latitude=60.20277800000001, longitude=11.083889000000001, isPassed=false)
        ),
        arrivalAirportIcao="ENGM",
        450
    )

    val eseba4m =
        Star(
            id="ESEBA4M",
            airport="ENGM",
            runway="19L",
            fixes= listOf(
                StarFix(id="ESEBA", typicalAltitude=null, typicalSpeedIas=250),
                StarFix(id="GM422", typicalAltitude=10000, typicalSpeedIas=220),
                StarFix(id="TITLA", typicalAltitude=5000, typicalSpeedIas=200),
                StarFix(id="OSPAD", typicalAltitude=4000, typicalSpeedIas=180),
                StarFix(id="XIVTA", typicalAltitude=3500, typicalSpeedIas=170),
                StarFix(id="ENGM", typicalAltitude=700, typicalSpeedIas=null)
            )
        )

    @Test
    fun `When direct routing, use preferred speed until next typical speed`() {
        val descentTrajectory = arrivalWithDirectRouting.toRunwayArrivalEvent(adopi3m, null, performance = b738performance)!!.descentTrajectory
        val directRoutingIndex = descentTrajectory.indexOfFirst { it.fixId == arrivalWithDirectRouting.assignedDirect }
        val expectedSpeedAtDirectRouting = adopi3m.fixes.find { it.id == arrivalWithDirectRouting.assignedDirect }!!.typicalSpeedIas!!

        descentTrajectory.subList(0, directRoutingIndex).forEach {
            assertTrue { it.ias > expectedSpeedAtDirectRouting }
        }
    }

    @Test
    fun `Descent trajectory should contain all fixes that has not been passed`() {
        val descentTrajectory = arrivalJson.toRunwayArrivalEvent(inrex4m, null, performance = b738performance)!!.descentTrajectory

        val remainingFixesOnRoute = arrivalJson.route.filter { !it.isPassed }.map { it.name }
        val fixesOnDescentTrajectory = descentTrajectory.mapNotNull { it.fixId }

        assertEquals(fixesOnDescentTrajectory, remainingFixesOnRoute, "Descent trajectory should contain all fixes that has not been passed")
    }

    @Test
    fun `Estimated TAS should not jump by more than 10 knots`() {
        val descentTrajectory = arrivalJson2.toRunwayArrivalEvent(eseba4m, null, performance = b738performance)!!.descentTrajectory

        descentTrajectory.forEach {
            println(
                "FixId: ${it.fixId}, ias: ${it.ias}, altitude: ${it.altitude}, remainingDistance: ${it.remainingDistance}, remainingTime: ${it.remainingTime}, groundSpeed: ${it.groundSpeed}, tas: ${it.tas}"
            )
        }

        val tasList = descentTrajectory.map { it.tas }
        val isJumping = tasList.zipWithNext().any { (prev, next) -> abs(prev - next) > 10 }
        assertEquals(false, isJumping, "TAS should not jump by more than 10 knots")
    }

    @Test
    fun `Estimated IAS should not jump by more than 5 knots`() {
        val descentTrajectory = arrivalJson.toRunwayArrivalEvent(inrex4m, null, performance = b738performance)!!.descentTrajectory

        val iasList = descentTrajectory.map { it.ias }
        val isJumping = iasList.zipWithNext().any { (prev, next) -> abs(prev - next) > 5 }
        assertEquals(false, isJumping, "IAS should not jump by more than 5 knots")
    }

    @Test
    fun `Estimated IAS should never exceed typical speed on STAR point`() {
        val descentTrajectory = arrivalJson
            .toRunwayArrivalEvent(inrex4m, null, performance = b738performance)!!
            .descentTrajectory.filter { it.fixId != null }

        assertEquals(descentTrajectory.size, 9)

        val isExceeding = descentTrajectory
            .filter { it.fixId != null }
            .any { point ->
                val starFix = inrex4m.fixes.find { it.id == point.fixId }
                if (starFix?.typicalSpeedIas == null) return@any false
                point.ias > starFix.typicalSpeedIas!!
            }

        assertEquals(false, isExceeding, "IAS should not exceed typical speed on STAR point")
    }

    @Test
    fun `Trajectory points that have a fix id should have the same coordinates as the corresponding fix on the aircraft's route`() {
        val descentTrajectory = arrivalJson
            .toRunwayArrivalEvent(inrex4m, null, performance = b738performance)!!
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
        val descentTrajectory = arrivalJson.toRunwayArrivalEvent(inrex4m, null, performance = b738performance)!!.descentTrajectory

        val descentTrajectory2 = arrivalWithDirectRouting.toRunwayArrivalEvent(adopi3m, null, performance = b738performance)!!.descentTrajectory

        val isExceeding = descentTrajectory.any { it.altitude < 10_000 && it.ias > 250 }
        assertEquals(false, isExceeding, "IAS should not exceed 250 below FL100")


        val isExceeding2 = descentTrajectory2.any { it.altitude < 10_000 && it.ias > 250 }
        assertEquals(false, isExceeding2, "IAS should not exceed 250 below FL100")
    }

    @Test
    fun `Altitude should never be increasing`() {
        val descentTrajectory = arrivalJson.toRunwayArrivalEvent(inrex4m, null, performance = b738performance)!!.descentTrajectory

        val altitudeList = descentTrajectory.map { it.altitude }
        val isIncreasing = altitudeList.zipWithNext().any { (prev, next) -> prev < next }
        assertEquals(false, isIncreasing, "Altitude should never be increasing")
    }

    @Test
    fun `Removing waypoint along a straight line should not affect ETA`() {
        val originalTrajectory = arrivalJson2.toRunwayArrivalEvent(inrex4m, null, performance = b738performance)!!.descentTrajectory

        val modifiedRoute = arrivalJson2.copy(
            route = arrivalJson2.route.filter { it.name != "TEKVA" }
        )

        val newTrajectory = modifiedRoute.toRunwayArrivalEvent(inrex4m, null, performance = b738performance)!!.descentTrajectory

        assertEquals(
            expected = originalTrajectory.first().remainingTime.inWholeSeconds.toDouble(),
            actual = newTrajectory.first().remainingTime.inWholeSeconds.toDouble(),
            absoluteTolerance = 1.0,
            "ETA should not change when removing a waypoint along a straight line"
        )
    }

    @Test
    fun `Removing waypoint along a curve should affect ETA`() {
        val originalTrajectory = arrivalJson2.toRunwayArrivalEvent(inrex4m, null, performance = b738performance)!!.descentTrajectory

        val modifiedRoute = arrivalJson2.copy(
            route = arrivalJson2.route.filter { it.name != "GM423" }
        )

        val newTrajectory = modifiedRoute.toRunwayArrivalEvent(inrex4m, null, performance = b738performance)!!.descentTrajectory

        val timeGained = originalTrajectory.first().remainingTime - newTrajectory.first().remainingTime

        assertTrue { timeGained > 30.seconds && timeGained < 60.seconds }

        assertNotEquals(
            illegal = originalTrajectory.first().remainingTime.inWholeSeconds.toDouble(),
            actual = newTrajectory.first().remainingTime.inWholeSeconds.toDouble(),
            absoluteTolerance = 45.0,
            "ETA should change when removing a waypoint along a curve"
        )
    }

    @Test
    fun `Descent trajectory should have same length as the remaining route`() {
        val remainingRoute =
            arrivalJson.route.filter { !it.isPassed }

        val descentTrajectoryLength =
            arrivalJson.toRunwayArrivalEvent(inrex4m, null, performance = b738performance)!!
                .descentTrajectory
                .zipWithNext()
                .sumOf { (previous, next) -> previous.position.distanceTo(next.position) }

        val remainingRouteLength = remainingRoute.zipWithNext()
            .sumOf { (current, next) ->
                val currentPosition = LatLng(current.latitude, current.longitude)
                val nextPosition = LatLng(next.latitude, next.longitude)
                currentPosition.distanceTo(nextPosition)
            }

        val distanceToFirstFix =
            LatLng(arrivalJson.latitude, arrivalJson.longitude)
                .distanceTo(LatLng(remainingRoute.first().latitude, remainingRoute.first().longitude))

        assertEquals(
            expected = descentTrajectoryLength,
            actual = remainingRouteLength + distanceToFirstFix,
            absoluteTolerance = 0.001,
            "Descent trajectory should have same length as the remaining route"
        )
    }

}