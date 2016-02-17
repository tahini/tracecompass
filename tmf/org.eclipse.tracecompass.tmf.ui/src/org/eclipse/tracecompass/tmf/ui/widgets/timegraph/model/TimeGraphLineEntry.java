/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.ITimeGraphLineEntry;

/**
 * An entry for use in the time graph views
 *
 * @author Matthew Khouzam
 * @since 4.3
 */
public class TimeGraphLineEntry extends TimeGraphEntry implements ITimeGraphLineEntry {

    /**
     * Constructor
     *
     * @param model
     *            Time Graph model
     */
    public TimeGraphLineEntry(ITmfTreeDataModel model) {
        super(convert(model));
    }

    private static TimeGraphEntryModel convert(ITmfTreeDataModel model) {
        return new TimeGraphEntryModel(model.getId(), model.getParentId(), model.getName(), Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public void updateModel(@NonNull ITmfTreeDataModel model) {
        super.updateModel(convert(model));
    }

    /**
     * Constructor
     *
     * @param name
     *            the name of the entry
     * @param startTime
     *            the start time
     * @param endTime
     *            the end time
     */
    public TimeGraphLineEntry(String name, long startTime, long endTime) {
        super(name, startTime, endTime);
    }

    @Override
    public void addEvent(ITimeEvent event) {
        if (!(event instanceof TimeLineEvent)) {
            throw new IllegalArgumentException("Need to be a TimeLineEvent"); //$NON-NLS-1$
        }
        super.addEvent(event);
    }
}
