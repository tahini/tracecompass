/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.core.filter;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.tracecompass.internal.provisional.tmf.core.model.filter.parser.FilterCu;
import org.eclipse.tracecompass.tmf.core.filter.ITmfFilter;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Helper class to convert to/from {@link ITmfFilter} and filter regexes
 *
 * @author Geneviève Bastien
 */
@NonNullByDefault
public final class TmfFilterHelper {

    private TmfFilterHelper() {
        // nothing to do
    }

    /**
     * Build an event filter from the regex string in parameter
     *
     * @param regex
     *            The filter regex
     * @param trace
     *            The trace this filter applies to
     * @return An event filter
     */
    public static ITmfFilter buildFilterFromRegex(String regex, ITmfTrace trace) {
        FilterCu compile = FilterCu.compile(regex);
        if (compile == null) {
            throw new NullPointerException("Invalid regex"); //$NON-NLS-1$
        }
        return compile.getEventFilter(trace);
    }

    /**
     * Get the regex that corresponds to this filter. The regex should be in the
     * filter language described in the
     * {@link org.eclipse.tracecompass.tmf.filter.parser} plugin. And as it may
     * be used to filter anything, so it may not be the direct string
     * representing of the original filter. For instance, a ITmfFilter specific
     * for events will do a smart conversion, so that the parameters of the
     * filter do not match only events, but are generic enough to match any data
     * source.
     *
     * The default implementation of this method returns a regex that will
     * return true at all time.
     *
     * @param filter
     *            The filter object to convert to regex
     *
     * @return The regex String, using the
     *         {@link org.eclipse.tracecompass.tmf.filter.parser} syntax
     * @since 4.1
     */
    public static String getRegexFromFilter(ITmfFilter filter) {
        // TODO: implement this probably using a visitor
        return ""; //$NON-NLS-1$
    }

}
