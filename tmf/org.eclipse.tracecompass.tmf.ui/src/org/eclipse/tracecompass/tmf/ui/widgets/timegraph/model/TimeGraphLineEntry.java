/*******************************************************************************
 * Copyright (c) 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;

/**
 * An entry for use in the time graph views
 *
 * @author Matthew Khouzam
 * @since 5.0
 */
public class TimeGraphLineEntry extends TimeGraphEntry {

    /**
     * Constructor
     *
     * @param model
     *            Time Graph model
     */
    public TimeGraphLineEntry(@NonNull ITmfTreeDataModel model) {
        super(model);
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

    @Override
    public DisplayStyle getStyle() {
        return DisplayStyle.LINE;
    }

}
