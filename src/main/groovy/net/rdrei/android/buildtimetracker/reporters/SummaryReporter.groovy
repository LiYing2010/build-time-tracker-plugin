package net.rdrei.android.buildtimetracker.reporters

import net.rdrei.android.buildtimetracker.Timing
import java.util.concurrent.TimeUnit
import jline.TerminalFactory
import org.gradle.api.logging.Logger

class SummaryReporter extends AbstractBuildTimeTrackerReporter {
    def static final UNICODE_SQUARE = "▇"
    def static final ASCII_SQUARE = "▒"
    def static final FILL = " "

    String barStyle

    SummaryReporter(Map<String, String> options, Logger logger) {
        super(options, logger)

        barStyle = getOption("barstyle", "unicode")
    }

    @Override
    def run(List<Timing> timings) {
        if (timings.size() == 0) return

        def threshold = getOption("threshold", "50").toInteger()

        if (getOption("ordered", "false").toBoolean()) {
            timings = timings.sort(false, { it.ms })
        }

        logger.quiet("== Build Time Summary ==")
        formatTable(timings, threshold)
    }

    // Thanks to @sindresorhus for the logic. https://github.com/sindresorhus/time-grunt
    def formatTable(List<Timing> timings, int threshold) {
        def total = timings.sum { t -> t.ms }
        def longestTaskName = timings.collect { it.path.length() }.max()
        def longestTiming = timings*.ms.max()
        def maxColumns = TerminalFactory.get().width
        if (maxColumns == null) maxColumns = 80

        def maxBarWidth
        if (longestTaskName > maxColumns / 2) {
            maxBarWidth = (maxColumns - 20) / 2
        } else {
            maxBarWidth = maxColumns - (longestTaskName + 20)
        }

        def longestBar = maxBarWidth * longestTiming / total

        for (timing in timings) {
            if (timing.ms >= threshold) {
                logger.quiet(sprintf("%s %s (%s)",
                        createBar(timing.ms / total, timing.ms / longestTiming, maxBarWidth),
                        shortenTaskName(timing.path, maxBarWidth),
                        FormattingUtils.formatDuration(timing.ms)))
            }
        }
    }

    def static shortenTaskName(String taskName, def max) {
        if (taskName.length() < max) { return taskName }

        int partLength = Math.floor((max - 3) / 2) as int
        def start = taskName.substring(0, partLength + 1)
        def end = taskName.substring(taskName.length() - partLength)

        start.trim() + '…' + end.trim()
    }

    def createBar(def fracOfTotal, def fracOfMax, def max) {
        def symbol = barStyle == "ascii" ? ASCII_SQUARE : UNICODE_SQUARE

        def roundedTotal = Math.round(fracOfTotal * 100)
        def barLength = Math.ceil(max * fracOfMax)
        def bar = FILL * (max - barLength) + symbol * barLength
        def formatted = roundedTotal < 10 ? " " + roundedTotal : roundedTotal;

        return (barStyle != "none" ? bar + ' ' : '') + formatted + '%'
    }

}
