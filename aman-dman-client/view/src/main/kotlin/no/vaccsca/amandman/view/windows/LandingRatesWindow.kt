package no.vaccsca.amandman.view.windows

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import no.vaccsca.amandman.model.timelineEvent.TimelineEvent
import no.vaccsca.amandman.view.entity.TimeRange
import no.vaccsca.amandman.view.tabpage.TimeRangeScrollBarHorizontal
import no.vaccsca.amandman.view.util.SharedValue
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.axis.*
import org.jfree.chart.labels.ItemLabelAnchor
import org.jfree.chart.labels.ItemLabelPosition
import org.jfree.chart.labels.StandardXYItemLabelGenerator
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.StandardXYBarPainter
import org.jfree.chart.renderer.xy.XYBarRenderer
import org.jfree.chart.ui.GradientPaintTransformType
import org.jfree.chart.ui.StandardGradientPaintTransformer
import org.jfree.chart.ui.TextAnchor
import org.jfree.data.RangeType
import org.jfree.data.time.Minute
import org.jfree.data.time.TimeSeries
import org.jfree.data.time.TimeSeriesCollection
import org.jfree.data.xy.XYBarDataset
import java.awt.*
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JComboBox
import javax.swing.JPanel
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


class LandingRatesGraph : JPanel() {

    private val timeSeries = TimeSeries("Landings")
    private val dataset = TimeSeriesCollection(timeSeries)
    private var chart: JFreeChart? = null
    private var plot: XYPlot? = null
    private var barDataset: XYBarDataset? = null
    private var currentBucketMillis = 10 * 60 * 1000L // default: 10 min

    private val bucketSelector = JComboBox(arrayOf("10 min", "30 min", "60 min"))

    private val availableTimeRange = SharedValue(
        initialValue = TimeRange(
            Clock.System.now() - 1.hours,
            Clock.System.now() + 3.hours,
        )
    )

    private val selectedTimeRange = SharedValue(
        initialValue = TimeRange(
            Clock.System.now() - 10.minutes,
            Clock.System.now() + 60.minutes,
        )
    )

    private val timeWindowScrollbar = TimeRangeScrollBarHorizontal(selectedTimeRange, availableTimeRange)

    init {
        layout = BorderLayout()

        chart = createChart()
        val chartPanel = ChartPanel(chart)
        chartPanel.background = Color.DARK_GRAY

        val controlPanel = JPanel().apply {
            background = Color.DARK_GRAY
            foreground = Color.WHITE
            add(bucketSelector)
        }

        bucketSelector.addActionListener {
            currentBucketMillis = when (bucketSelector.selectedItem as String) {
                "10 min" -> 10 * 60 * 1000L
                "30 min" -> 30 * 60 * 1000L
                "60 min" -> 60 * 60 * 1000L
                else -> 10 * 60 * 1000L
            }
            updateChartBarWidth()
        }

        selectedTimeRange.addListener {
            updateXAxisRange()
        }

        add(controlPanel, BorderLayout.NORTH)
        add(chartPanel, BorderLayout.CENTER)
        add(timeWindowScrollbar, BorderLayout.SOUTH)
        updateXAxisRange()
    }

    private fun createChart(): JFreeChart {
        val baseChart = ChartFactory.createXYBarChart(
            "",
            null,
            true,
            null,
            dataset
        )

        plot = baseChart.plot as XYPlot
        barDataset = XYBarDataset(dataset, currentBucketMillis.toDouble())
        plot!!.dataset = barDataset
        plot!!.isOutlineVisible = false

        plot!!.backgroundAlpha = 0.0f
        plot!!.domainGridlinePaint = Color.GRAY
        plot!!.rangeGridlinePaint = Color.GRAY
        plot!!.isRangeGridlinesVisible = false
        plot!!.isDomainGridlinesVisible = false

        baseChart.backgroundPaint = Color(50, 50, 50)
        baseChart.title.paint = Color.WHITE
        baseChart.removeLegend()

        val domainAxis = plot!!.domainAxis as DateAxis
        domainAxis.timeZone = TimeZone.getTimeZone("UTC") // <- Add this line
        domainAxis.labelPaint = Color.WHITE
        domainAxis.tickLabelPaint = Color.WHITE
        domainAxis.tickUnit = DateTickUnit(DateTickUnitType.MINUTE, 15)
        domainAxis.dateFormatOverride = SimpleDateFormat("HH:mm").apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val rangeAxis = plot!!.rangeAxis as NumberAxis
        rangeAxis.labelPaint = Color.WHITE
        rangeAxis.tickLabelPaint = Color.WHITE
        rangeAxis.standardTickUnits = NumberAxis.createIntegerTickUnits()
        rangeAxis.isAutoRange = true
        rangeAxis.autoRangeMinimumSize = 10.0
        rangeAxis.rangeType = RangeType.POSITIVE

        val renderer = XYBarRenderer()
        renderer.margin = 0.0
        renderer.barAlignmentFactor = 0.0
        renderer.setShadowVisible(false)
        renderer.setSeriesPaint(0, Color(0, 178, 0))
        renderer.barPainter = StandardXYBarPainter()

        renderer.isDrawBarOutline = true
        renderer.setSeriesOutlinePaint(0, Color.GRAY)
        renderer.setSeriesOutlineStroke(0, BasicStroke(1.0f))
        val baseGradient = GradientPaint(
            0f, 100f, Color(75, 243, 98),
            0f, 0f, Color(45, 123, 56),

        )
        renderer.setSeriesPaint(0, baseGradient)
        renderer.gradientPaintTransformer = StandardGradientPaintTransformer(GradientPaintTransformType.VERTICAL)


        renderer.defaultItemLabelsVisible = true
        renderer.defaultItemLabelGenerator = StandardXYItemLabelGenerator()
        renderer.defaultItemLabelPaint = Color.WHITE
        renderer.defaultItemLabelFont = Font("SansSerif", Font.BOLD, 12)
        renderer.defaultPositiveItemLabelPosition = ItemLabelPosition(ItemLabelAnchor.INSIDE12, TextAnchor.TOP_CENTER)

        plot!!.renderer = renderer

        return baseChart
    }

    private fun updateChartBarWidth() {
        barDataset = XYBarDataset(dataset, currentBucketMillis.toDouble())
        plot?.dataset = barDataset
    }

    fun updateData(allArrivalEvents: List<TimelineEvent>) {
        if (allArrivalEvents.isEmpty()) return

        val grouped = allArrivalEvents.groupBy {
            val timeUtc = it.scheduledTime.toEpochMilliseconds() / 1000
            Instant.fromEpochSeconds(timeUtc / (currentBucketMillis / 1000) * (currentBucketMillis / 1000))
        }.mapValues { (_, occurrences) -> occurrences.size }

        timeSeries.clear()

        for ((instant, count) in grouped.entries.sortedBy { it.key }) {
            val date = Date(instant.toEpochMilliseconds())
            val calendar = Calendar.getInstance()
            calendar.time = date
            val minute = Minute(date, calendar)
            timeSeries.addOrUpdate(minute, count)
        }

        updateChartBarWidth()
    }

    private fun updateXAxisRange() {
        val domainAxis = plot?.domainAxis as? DateAxis ?: return
        domainAxis.setRange(
            Date(selectedTimeRange.value.start.toEpochMilliseconds()),
            Date(selectedTimeRange.value.end.toEpochMilliseconds())
        )
    }
}
