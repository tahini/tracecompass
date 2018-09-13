/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.filter;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.filter.TmfFilterHelper;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * A common class for all filters, either event filters or regexes
 *
 * @since 4.1
 */
@NonNullByDefault
public class TraceCompassFilter {

    private static final String NO_REGEX = "";
    private final @Nullable ITmfFilter fEventFilter;
    private final String fRegex;

    private static final Map<ITmfTrace, TraceCompassFilter> FILTER_MAP = new HashMap<>();

    private TraceCompassFilter(@Nullable ITmfFilter filter, String regex) {
        fEventFilter = filter;
        fRegex = regex;
    }

    /**
     * Factory method to get a new filter from an event filter
     *
     * @param filter
     *            The event filter from which to create this filter
     * @param trace
     *            The trace this filter applies to
     * @return A new filter
     */
    public static TraceCompassFilter fromEventFilter(@Nullable ITmfFilter filter, ITmfTrace trace) {
        String regex = filter == null ? NO_REGEX : TmfFilterHelper.getRegexFromFilter(filter);
        TraceCompassFilter traceCompassFilter = new TraceCompassFilter(filter, regex);
        FILTER_MAP.put(trace, traceCompassFilter);
        return traceCompassFilter;
    }

    /**
     * Factory method to get a new filter from a regex
     *
     * @param regex
     *            The regex from which to create the filter
     * @param trace
     *            The trace this filter applies to
     * @return A new filter
     */
    public static TraceCompassFilter fromRegex(String regex, ITmfTrace trace) {
        ITmfFilter filter = TmfFilterHelper.buildFilterFromRegex(regex, trace);
        TraceCompassFilter traceCompassFilter = new TraceCompassFilter(filter, regex);
        FILTER_MAP.put(trace, traceCompassFilter);
        return traceCompassFilter;
    }

    /**
     * Get the event filter being applied. This filter should be applied on data
     * sources based on events. For other types of data sources, use the
     * {@link #getRegex()} method to get the filter string.
     *
     * @return The filter, or <code>null</code> if filters should be removed
     */
    public @Nullable ITmfFilter getEventFilter() {
        return fEventFilter;
    }

    /**
     * Get the filter regex, that should be used to filter data from a source
     * that is not based on event. For event-based filters, use
     * {@link #getEventFilter()} instead
     *
     * @return The regex to filter anything that is not event-based
     */
    public String getRegex() {
        return fRegex;
    }

    /**
     * Remove filters for the closed trace
     *
     * @param signal
     *            The trace closed signal
     */
    @TmfSignalHandler
    public static void traceClosed(final TmfTraceClosedSignal signal) {
        FILTER_MAP.remove(signal.getTrace());
    }

    /**
     * Get the filter that is active for a given trace
     *
     * @param trace
     *            The trace to get the filter for
     * @return The filter to apply, or <code>null</code> if no filter is set
     */
    public static @Nullable TraceCompassFilter getFilterForTrace(ITmfTrace trace) {
        return FILTER_MAP.get(trace);
    }

}
