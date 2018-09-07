/*******************************************************************************
 * Copyright (c) 2012, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.signal;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.filter.ITmfFilter;
import org.eclipse.tracecompass.tmf.core.filter.TraceCompassFilter;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Signal indicating an event filter has been applied.
 *
 * @author Patrick Tasse
 */
public class TmfEventFilterAppliedSignal extends TmfSignal {

    private final ITmfTrace fTrace;
    private final TraceCompassFilter fFilter;

    /**
     * Constructor for a new signal.
     *
     * @param source
     *            The object sending this signal
     * @param trace
     *            The trace to which filter is applied
     * @param filter
     *            The applied event filter or null
     */
    public TmfEventFilterAppliedSignal(Object source, ITmfTrace trace, @NonNull ITmfFilter filter) {
        super(source);
        fTrace = trace;
        fFilter = TraceCompassFilter.fromEventFilter(filter, trace);
    }

    /**
     * Constructor for a new signal.
     *
     * @param source
     *            The object sending this signal
     * @param trace
     *            The trace to which filter is applied
     * @param regex
     *            The filter regex string, as per the syntax of
     *            {@link org.eclipse.tracecompass.tmf.filter.parser}
     * @since 4.1
     */
    public TmfEventFilterAppliedSignal(Object source, ITmfTrace trace, @NonNull String regex) {
        super(source);
        fTrace = trace;
        fFilter = TraceCompassFilter.fromRegex(regex, trace);
    }

    /**
     * Get the trace object concerning this signal
     *
     * @return The trace
     */
    public ITmfTrace getTrace() {
        return fTrace;
    }

    /**
     * Get the event filter being applied. This filter should be applied on data
     * sources based on events. For other types of data sources, use the
     * {@link #getFilter()} method to get the full filter.
     *
     * @return The filter
     * @deprecated Use the {@link #getFilter()} method instead
     */
    @Deprecated
    public ITmfFilter getEventFilter() {
        return fFilter.getEventFilter();
    }

    /**
     * Get the filter that is being applied
     *
     * @return The filter being applied
     *
     * @since 4.1
     */
    public TraceCompassFilter getFilter() {
        return fFilter;
    }

    @Override
    public String toString() {
        return "[TmfEventFilterAppliedSignal (" + fTrace.getName() + " : " + fFilter + ")]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
