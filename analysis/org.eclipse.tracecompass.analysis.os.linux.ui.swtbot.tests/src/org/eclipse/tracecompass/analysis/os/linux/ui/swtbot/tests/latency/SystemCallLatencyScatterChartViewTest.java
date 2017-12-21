/*******************************************************************************
 * Copyright (c) 2016, 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.ui.swtbot.tests.latency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.latency.SystemCallLatencyScatterView;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ctf.core.tests.shared.CtfTmfTestTraceUtils;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.XYDataProviderBaseTest;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.ConditionHelpers;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.SWTBotUtils;
import org.eclipse.tracecompass.tmf.ui.tests.shared.WaitUtils;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.linecharts.TmfCommonXAxisChartViewer;
import org.eclipse.ui.IViewPart;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.swtchart.Chart;
import org.swtchart.ISeries;
import org.swtchart.LineStyle;

/**
 * Tests of the scatter chart view
 *
 * @author Matthew Khouzam
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class SystemCallLatencyScatterChartViewTest extends XYDataProviderBaseTest {

    private static final @NonNull String TITLE = "Duration vs Time";
    private static final @NonNull String SERIES1_NAME = "bug446190/clock_gettime";
    private static final @NonNull String SERIES2_NAME = "bug446190/ioctl";

    private static final String VIEW_ID = SystemCallLatencyScatterView.ID;

    /**
     * Test to check the System Call Scatter view. When trace opens, there are a few
     * syscalls present, then we move to the zone without system calls, the tree
     * should be empty.
     *
     * TODO: Test the data
     *
     * @throws NoSuchMethodException
     *             Reflection exception should not happen
     * @throws SecurityException
     *             Reflection exception should not happen
     * @throws IllegalAccessException
     *             Reflection exception should not happen
     * @throws IllegalArgumentException
     *             Reflection exception should not happen
     * @throws InvocationTargetException
     *             Reflection exception should not happen
     */
    @Test
    public void testWithTrace() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        // Wait for analysis to finish.
        WaitUtils.waitForJobs();
        IViewPart viewSite = getSWTBotView().getViewReference().getView(true);
        assertTrue(viewSite instanceof SystemCallLatencyScatterView);
        final TmfCommonXAxisChartViewer chartViewer = (TmfCommonXAxisChartViewer) getChartViewer(viewSite);
        assertNotNull(chartViewer);
        fBot.waitUntil(ConditionHelpers.xyViewerIsReadyCondition(chartViewer));

        final Chart chart = getChart();
        assertNotNull(chart);
        SWTBotTreeItem[] items = getSWTBotView().bot().tree().getAllItems();
        for (SWTBotTreeItem item : items) {
            item.check();
        }

        SWTBotUtils.waitUntil(c -> c.getSeriesSet().getSeries().length > 0, chart, "No data available");

        /* Test type, style and color of series */
        verifyChartStyle();

        // Update the time range to a range where there is no data
        long noDataStart = 1412670961274443542L;
        long noDataEnd = 1412670961298823940L;
        TmfTimeRange windowRange = new TmfTimeRange(TmfTimestamp.fromNanos(noDataStart), TmfTimestamp.fromNanos(noDataEnd));
        TmfSignalManager.dispatchSignal(new TmfWindowRangeUpdatedSignal(this, windowRange));
        fBot.waitUntil(ConditionHelpers.windowRange(windowRange));
        fBot.waitUntil(ConditionHelpers.xyViewerIsReadyCondition(chartViewer));

        // Only the root item should be present
        items = getSWTBotView().bot().tree().getAllItems();
        assertEquals(1, items.length);
    }

    private void verifyChartStyle() {
        verifySeriesStyle(SERIES1_NAME, ISeries.SeriesType.LINE, null, LineStyle.NONE, false);
        verifySeriesStyle(SERIES2_NAME, ISeries.SeriesType.LINE, null, LineStyle.NONE, false);
    }

    @Override
    protected @NonNull String getMainSeriesName() {
        return SERIES2_NAME;
    }

    @Override
    protected @NonNull String getTitle() {
        return TITLE;
    }

    @Override
    protected String getViewID() {
        return VIEW_ID;
    }

    @Override
    protected ITmfTrace getTestTrace() {
        return CtfTmfTestTraceUtils.getTrace(CtfTestTrace.ARM_64_BIT_HEADER);
    }

    @Override
    protected void disposeTestTrace() {
        CtfTmfTestTraceUtils.dispose(CtfTestTrace.ARM_64_BIT_HEADER);
    }
}
