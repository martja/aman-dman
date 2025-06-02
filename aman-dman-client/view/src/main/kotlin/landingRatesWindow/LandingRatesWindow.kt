package landingRatesWindow

import kotlinx.datetime.Instant
import org.example.TimelineOccurrence
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
import java.text.FieldPosition
import java.text.SimpleDateFormat
import java.time.ZoneOffset
import java.util.*
import javax.swing.JComboBox
import javax.swing.JPanel


class LandingRatesGraph : JPanel() {

    private val timeSeries = TimeSeries("Landings")
    private val dataset = TimeSeriesCollection(timeSeries)
    private var chart: JFreeChart? = null
    private var plot: XYPlot? = null
    private var barDataset: XYBarDataset? = null
    private var currentBucketMillis = 10 * 60 * 1000L // default: 10 min

    private val bucketSelector = JComboBox(arrayOf("10 min", "30 min", "60 min"))

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

        add(controlPanel, BorderLayout.NORTH)
        add(chartPanel, BorderLayout.CENTER)
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
        domainAxis.labelPaint = Color.WHITE
        domainAxis.tickLabelPaint = Color.WHITE
        domainAxis.tickUnit = DateTickUnit(DateTickUnitType.MINUTE, 15)
        domainAxis.dateFormatOverride = object : SimpleDateFormat("HH:mm") {
            override fun format(date: Date, toAppendTo: StringBuffer, pos: FieldPosition): StringBuffer {
                val cal = Calendar.getInstance()
                cal.time = date
                return if (cal.get(Calendar.MINUTE) == 0) {
                    super.format(date, toAppendTo, pos)
                } else {
                    toAppendTo
                }
            }
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

    fun updateData(allArrivalOccurrences: List<TimelineOccurrence>) {
        if (allArrivalOccurrences.isEmpty()) return

        val grouped = allArrivalOccurrences.groupBy {
            val timeUtc = it.time.toEpochMilliseconds() / 1000
            Instant.fromEpochSeconds(timeUtc / (currentBucketMillis / 1000) * (currentBucketMillis / 1000))
        }.mapValues { (_, occurrences) -> occurrences.size }

        timeSeries.clear()

        for ((instant, count) in grouped.entries.sortedBy { it.key }) {
            val zdt = java.time.Instant.ofEpochSecond(instant.epochSeconds)
                .atZone(ZoneOffset.UTC)
            val minute = Minute(zdt.minute, zdt.hour, zdt.dayOfMonth, zdt.monthValue, zdt.year)
            timeSeries.addOrUpdate(minute, count)
        }

        updateChartBarWidth()
    }
}
