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
import java.util.Objects;

/**
 * Generic TimeEvent implementation
 *
 * @author Matthew Khouzam
 * @since 5.0
 */
public class TimeLineEvent extends TimeEvent {

    private final List<Long> fValues;

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
        super(entry, time, duration);
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
    public int hashCode() {
        return Objects.hash(super.hashCode(), fValues);
    }

    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) {
            return false;
        }
        if(obj instanceof TimeLineEvent) {
            TimeLineEvent lineEvent = (TimeLineEvent) obj;
            return Objects.equals(getValues(), lineEvent.getValues());
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[TimeLineEvent Values=").append(getValues()) //$NON-NLS-1$
                .append(", Entry=").append(getEntry()) //$NON-NLS-1$
                .append(", fTime=").append(getTime()) //$NON-NLS-1$
                .append(", Duration=").append(getDuration()) //$NON-NLS-1$
                .append(']');
        return builder.toString();
    }

}
