package org.example.view

import org.example.config.AircraftPerformanceData
import org.example.model.entities.navdata.LatLng
import org.example.model.entities.navigation.AircraftPosition
import org.example.model.entities.navigation.RoutePoint
import org.example.model.entities.navigation.star.Constraint
import org.example.model.entities.navigation.star.Star
import org.example.model.entities.navigation.star.StarFix
import org.example.service.DescentProfileService.generateDescentSegments
import org.example.state.ApplicationState
import org.example.util.NavigationUtils.dmsToDecimal
import java.awt.BorderLayout
import javax.swing.JPanel

class VerticalProfileVisualization(
    val state: ApplicationState
) : JPanel(BorderLayout()) {

    fun starFix(id: String, block: StarFix.StarFixBuilder.() -> Unit): StarFix {
        return StarFix.StarFixBuilder(id).apply(block).build()
    }

    val lunip4l = Star(
        id = "LUNIP4L",
        airfieldElevationFt = 700,
        fixes = listOf(
            starFix("LUNIP") {
                speed(Constraint.Max(250))
            },
            starFix("DEVKU") {
                altitude(Constraint.Min(12000))
                speed(Constraint.Max(250))
            },
            starFix("GM416") {
                altitude(Constraint.Exact(11000))
                speed(Constraint.Max(220))
            },
            starFix("GM417") {
                altitude(Constraint.Exact(11000))
                speed(Constraint.Max(220))
            },
            starFix("GM415") {
                altitude(Constraint.Exact(11000))
                speed(Constraint.Max(220))
            },
            starFix("GM414") {
                altitude(Constraint.Exact(11000))
                speed(Constraint.Max(220))
            },
            starFix("INSUV") {
                altitude(Constraint.Min(5000))
                altitude(Constraint.Exact(5000))
                speed(Constraint.Max(220))
            },
            starFix("NOSLA") {
                speed(Constraint.Max(200))
            },
            starFix("XEMEN") {
                altitude(Constraint.Exact(3500))
                speed(Constraint.Max(200))
            }
        )
    )


    val currentPosition = AircraftPosition(
        dmsToDecimal("""58°50'25.3"N  011°20'7.2"E"""),
        22000,
        250,
        180
    )

    val testRoute = listOf(
        RoutePoint("CURRENT", currentPosition.position),
        RoutePoint("LUNIP", dmsToDecimal("""59°10'60.0"N  011°18'55.0"E""")),
        RoutePoint("DEVKU", dmsToDecimal("""59°27'7.9"N  011°15'34.4"E""")),
        RoutePoint("GM416", dmsToDecimal("""59°37'49.7"N  011°13'1.2"E""")),
        RoutePoint("GM417", dmsToDecimal("""59°39'55.7"N  011°24'37.9"E""")),
        RoutePoint("GM415", dmsToDecimal("""59°43'57.3"N  011°34'9.0"E""")),
        RoutePoint("GM414", dmsToDecimal("""59°49'18.7"N  011°40'29.1"E""")),
        RoutePoint("INSUV", dmsToDecimal("""59°55'32.2"N  011°6'50.6"E""")),
        RoutePoint("NOSLA", dmsToDecimal("""59°59'1.2"N  010°59'51.2"E""")),
        RoutePoint("XEMEN", dmsToDecimal("""60°2'10.4"N  011°1'39.4"E""")),
        RoutePoint("ONE", dmsToDecimal("""60°10'40.6"N  011°6'41.0"E""")),
    )

    val descentSegments = testRoute.generateDescentSegments(
        currentPosition,
        verticalWeatherProfile = state.verticalWeatherProfile,
        lunip4l,
        aircraftPerformance = AircraftPerformanceData.get("B738")
    )

    init {
        background = java.awt.Color.DARK_GRAY
    }

    override fun paintComponent(g: java.awt.Graphics) {
        super.paintComponent(g)

        val minAlt = descentSegments.minOf { it.targetAltitude }
        val maxAlt = descentSegments.maxOf { it.targetAltitude }

        val diagramMargin = 30

        val totalLengthNm = descentSegments.first().remainingDistance
        val totalLengthSeconds = descentSegments.first().remainingTime.inWholeSeconds
        val totalAirLengthNm = descentSegments.zipWithNext { a, b ->
            val legDuration = a.remainingTime.inWholeSeconds - b.remainingTime.inWholeSeconds
            val airDistance = a.tas * (legDuration / 3600.0)
            airDistance
        }.sum()

        val pxPerFt = (height - diagramMargin*2).toFloat() / (maxAlt - minAlt).toFloat()
        val pxPerNm = (width - diagramMargin*2).toFloat() / totalLengthNm
        val pxPerSecond = (width - diagramMargin*2).toFloat() / totalLengthSeconds

        var prevX = diagramMargin
        var prevTimeX = diagramMargin
        var prevY = diagramMargin
        var prevInbound: String? = null


        descentSegments.forEach {
            val yPos = (height - pxPerFt * (it.targetAltitude - minAlt)).toInt() - diagramMargin
            val xPos = width - (pxPerNm * it.remainingDistance).toInt() - diagramMargin
            val xPosTime = width - (pxPerSecond * it.remainingTime.inWholeSeconds).toInt() - diagramMargin

            // Visualize remaninging time wrt height
            g.color = java.awt.Color.GRAY
            g.drawLine(prevTimeX, prevY, xPosTime, yPos)
            prevTimeX = xPosTime

            // Visualize remaninging distance wrt height
            g.color = java.awt.Color.WHITE
            g.drawLine(prevX, prevY, xPos, yPos)
            prevX = xPos
            prevY = yPos

            if (prevInbound != it.inbound && prevInbound != null) {
                g.drawString(prevInbound, xPos, yPos - 5)
                g.drawString(it.remainingTime.toString(), xPos, yPos + 15)
            }
            prevInbound = it.inbound

            g.drawOval(xPos - 3, yPos - 3, 6, 6)
            //g.drawString("FL${(it.targetAltitude / 100.0).toInt().toString().padStart(3, '0')}: ${it.remainingDistance} nm / ${it.remainingTime}", 30, yPos)
        }
    }
}