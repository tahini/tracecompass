/**********************************************************************
 * Copyright (c) 2014 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Bernd Hufmann - Initial API and implementation
 *   Geneviève Bastien - Create and use base class for XY plots
 **********************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.ui.views.diskioactivity;

import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tracecompass.analysis.os.linux.core.inputoutput.Attributes;
import org.eclipse.tracecompass.analysis.os.linux.core.inputoutput.InputOutputAnalysisModule;
import org.eclipse.tracecompass.analysis.os.linux.core.inputoutput.StateValues;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.linecharts.TmfCommonXLineChartViewer;
import org.eclipse.swt.widgets.Composite;

/*
 * TODO: We need to change the way we increment written and read values.
 * Incrementing at the block_rq_event is not correct. We need to estimate
 * reading and writing speed by looking at requests between t1 and t2.
 */
/**
 *
 * @author Houssem Daoud
 * @since 2.0
 */

public class DisksIOActivityViewer extends TmfCommonXLineChartViewer {

    private TmfStateSystemAnalysisModule fModule = null;
    private final String diskname = new String("sdb"); //$NON-NLS-1$
    // Timeout between updates in the updateData thread
    private static final long BUILD_UPDATE_TIMEOUT = 500;
    private static final double RESOLUTION = 0.2;
    private static final int MB_TO_SECTOR = 2 * 1024;
    private static final int SECOND_TO_NANOSECOND = (int) Math.pow(10, 9);

    /**
     * Constructor
     *
     * @param parent
     *            parent view
     */
    public DisksIOActivityViewer(Composite parent) {
        super(parent, Messages.DiskIOActivityViewer_Title, Messages.DiskIOActivityViewer_XAxis, Messages.DiskIOActivityViewer_YAxis);
        setResolution(RESOLUTION);
    }

    @SuppressWarnings("null")
    @Override
    protected void initializeDataSource() {
        ITmfTrace trace = getTrace();
        if (trace != null) {
            fModule = TmfTraceUtils.getAnalysisModuleOfClass(trace, TmfStateSystemAnalysisModule.class, InputOutputAnalysisModule.ID);
            if (fModule == null) {
                return;
            }
            fModule.schedule();
        }
    }

    @Override
    protected void updateData(long start, long end, int nb, IProgressMonitor monitor) {

            if (getTrace() == null || fModule == null) {
                return;
            }
            fModule.waitForInitialization();
            ITmfStateSystem ss = fModule.getStateSystem();
            /*
             * Don't wait for the module completion, when it's ready, we'll know
             */
            if (ss == null) {
                return;
            }

            double[] xvalues = getXAxis(start, end, nb);
            setXAxis(xvalues);

            boolean complete = false;
            long currentEnd = start;

            while (!complete && currentEnd < end) {
                if (monitor.isCanceled()) {
                    return;
                }
                complete = ss.waitUntilBuilt(BUILD_UPDATE_TIMEOUT);
                currentEnd = ss.getCurrentEndTime();
                long traceStart = getStartTime();
                long traceEnd = getEndTime();
                long offset = this.getTimeOffset();

                /* Initialize quarks and series names */
                double[] fYValuesWritten = new double[xvalues.length];
                double[] fYValuesRead = new double[xvalues.length];
                String serieNameWritten = new String(diskname+" write").trim(); //$NON-NLS-1$
                String seriesNameRead = new String(diskname+" read").trim(); //$NON-NLS-1$
                /*
                 * TODO: It should only show active threads in the time range.
                 * If a tid does not have any memory value (only 1 interval in
                 * the time range with value null or 0), then its series should
                 * not be displayed.
                 */
                double prevX = xvalues[0];
                long prevTime = (long) prevX + offset;
                /*
                 * make sure that time is in the trace range after double to
                 * long conversion
                 */
                prevTime = Math.max(traceStart, prevTime);
                prevTime = Math.min(traceEnd, prevTime);
                for (int i = 1; i < xvalues.length; i++) {
                    if (monitor.isCanceled()) {
                        return;
                    }
                    double x = xvalues[i];
                    long time = (long) x + offset;
                    time = Math.max(traceStart, time);
                    time = Math.min(traceEnd, time);
                    try {
                        fYValuesWritten[i] = getSectorsInRange(prevTime, time,StateValues.WRITING_REQUEST)/(time-prevTime)*SECOND_TO_NANOSECOND /MB_TO_SECTOR;
                        fYValuesRead[i] = getSectorsInRange(prevTime, time,StateValues.READING_REQUEST)/(time-prevTime)*SECOND_TO_NANOSECOND /MB_TO_SECTOR;
                    } catch (TimeRangeException e) {
                        fYValuesWritten[i] = 0;
                        fYValuesRead[i] = 0;
                    }
                    prevTime = time;
                }
                setSeries(serieNameWritten, fYValuesWritten);
                setSeries(seriesNameRead, fYValuesRead);
                if (monitor.isCanceled()) {
                    return;
                }
                updateDisplay();

            }
    }

    double getSectorsInRange(long start,long end,int rw){

        String rw_attribute_name = null;
        if(rw == StateValues.READING_REQUEST){
            rw_attribute_name = Attributes.SECTORS_READ;
        } else {
            rw_attribute_name = Attributes.SECTORS_WRITTEN;
        }
        double currentCount = 0;
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return -1;
        }
        long startTime = Math.max(start, ss.getStartTime());
        long endTime = Math.min(end, ss.getCurrentEndTime());
        if (endTime < startTime) {
            return -1;
        }

        try {
            List<ITmfStateInterval> endState = ss.queryFullState(endTime);
            List<ITmfStateInterval> startState = ss.queryFullState(startTime);
            double countAtEnd = endState.get(ss.getQuarkAbsolute(Attributes.DISKS,diskname,rw_attribute_name)).getStateValue().unboxLong();
            if (countAtEnd == -1) {
                countAtEnd = 0;
            }
            double countAtStart = startState.get(ss.getQuarkAbsolute(Attributes.DISKS,diskname,rw_attribute_name)).getStateValue().unboxLong();
            if (countAtStart == -1) {
                countAtStart = 0;
            }
            List<Integer> driverslotsQuarks = ss.getQuarks(Attributes.DISKS, diskname,Attributes.DRIVER_QUEUE, "*");  //$NON-NLS-1$
            for (Integer driverSlotQuark : driverslotsQuarks) {
                Integer currentRequestQuark = ss.getQuarkRelative(driverSlotQuark, Attributes.CURRENT_REQUEST);
                int sizeQuark = ss.getQuarkRelative(driverSlotQuark, Attributes.REQUEST_SIZE);
                int statusQuark = ss.getQuarkRelative(driverSlotQuark, Attributes.STATUS);
                // interpolate at startTime
                long startrequest_sector = startState.get(currentRequestQuark).getStateValue().unboxLong();
                if( startrequest_sector != -1) {
                    if (startState.get(statusQuark).getStateValue().unboxInt() == rw) {
                    long runningTime = startState.get(currentRequestQuark).getEndTime()- startState.get(currentRequestQuark).getStartTime();
                    long runningEnd = startState.get(currentRequestQuark).getEndTime();
                    long startsize = startState.get(sizeQuark).getStateValue().unboxLong();
                    countAtStart = interpolateCount(countAtStart, startTime, runningEnd, runningTime, startsize);
                    }
                }
                // interpolate at EndTime
                long endrequest_sector = endState.get(currentRequestQuark).getStateValue().unboxLong();
                if( endrequest_sector != -1) {
                    if (startState.get(statusQuark).getStateValue().unboxInt() == rw) {
                    long runningTime = endState.get(currentRequestQuark).getEndTime()- endState.get(currentRequestQuark).getStartTime();
                    long runningEnd = endState.get(currentRequestQuark).getEndTime();
                    long endsize = endState.get(sizeQuark).getStateValue().unboxLong();
                    countAtEnd = interpolateCount(countAtEnd, endTime, runningEnd, runningTime, endsize);
                    }
                }
            }
            currentCount = countAtEnd - countAtStart;
        } catch (StateSystemDisposedException e) {
            e.printStackTrace();
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return currentCount;
    }
    private static double interpolateCount(double count, long ts, long runningEnd, long runningTime, long size) {

        double newCount = count;
        if (runningTime > 0) {
            long runningStart = runningEnd - runningTime;
            if (ts < runningStart) {
                return newCount;
            }
            double interpolation= (double)(ts - runningStart) * (double)size / (runningTime);
            newCount += interpolation;
        }
        return newCount;
    }

}
