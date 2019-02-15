/*******************************************************************************
 * Copyright (c) 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.timegraph.dataprovider;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * Class to encapsulate a {@link TimeGraphEntryModel} for the trace level
 * and the relevant data provider
 *
 * @author Loic Prieur-Drevon
 * @thanks Loic!
 * @since 3.3
 */
final class TraceEntry extends TimeGraphEntry {

    /**
     * Constructor
     *
     * @param model
     *            trace level model
     * @param trace
     *            The trace corresponding to this trace entry.
     */
    public TraceEntry(TimeGraphEntryModel model, @NonNull ITmfTrace trace) {
        super(new TimeGraphEntryModel(-1, -1, trace.getName(), trace.getStartTime().toNanos(), trace.getEndTime().toNanos(), false));
    }

    @Override
    public boolean hasTimeEvents() {
        return false;
    }

}