/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic TimeEvent implementation
 *
 * @author Matthew Khouzam
 * @since 4.3
 */
public class TimeLineEvent implements ITimeEvent {

    private final List<Long> fValues;
    private final ITimeGraphEntry fEntry;
    private final long fTime;
    private final long fDuration;

    /**
     * Standard constructor
     *
     * @param entry
     *            The entry matching this event
     * @param time
     *            The timestamp of this event
     * @param duration
     *            The duration of the event
     */
    public TimeLineEvent(ITimeGraphEntry entry, long time, long duration) {
        this(entry, time, duration, new ArrayList<>());
    }

    /**
     * Standard constructor
     *
     * @param entry
     *            The entry matching this event
     * @param time
     *            The timestamp of this event
     * @param duration
     *            The duration of the event
     * @param values
     *            The values to display
     */
    public TimeLineEvent(ITimeGraphEntry entry, long time, long duration, List<Long> values) {
        fEntry = entry;
        fTime = time;
        fDuration = duration;
        fValues = values;
    }

    /**
     * Add a value
     *
     * @param value
     *            the value to add, it will be displayed as a line
     */
    public void addValue(long value) {
        fValues.add(value);
    }

    /**
     * Get the values to display
     *
     * @return the values to be displayed
     */
    public List<Long> getValues() {
        return fValues;
    }

    @Override
    public ITimeGraphEntry getEntry() {
        return fEntry;
    }

    @Override
    public long getTime() {
        return fTime;
    }

    @Override
    public long getDuration() {
        return fDuration;
    }

    @Override
    public ITimeEvent splitBefore(long splitTime) {
        return (splitTime > fTime ? new TimeLineEvent(fEntry, fTime, Math.min(fDuration, splitTime - fTime), fValues) : null);
    }

    @Override
    public ITimeEvent splitAfter(long splitTime) {
        return (splitTime < fTime + fDuration ? new TimeLineEvent(fEntry, Math.max(fTime, splitTime), fDuration - Math.max(0, splitTime - fTime),
                fValues) : null);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[TimeLineEvent fValues=").append(fValues).append(", fEntry=").append(fEntry).append(", fTime=").append(fTime).append(", fDuration=").append(fDuration).append("]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        return builder.toString();
    }

}
