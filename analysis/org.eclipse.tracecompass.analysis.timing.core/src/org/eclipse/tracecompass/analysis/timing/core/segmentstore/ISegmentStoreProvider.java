/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.timing.core.segmentstore;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * Segment store provider. Useful to populate views.
 *
 * @author Matthew Khouzam
 * @since 2.0
 */
public interface ISegmentStoreProvider {

    /**
     * Add a listener for the viewers
     *
     * @param listener
     *                     listener for each type of viewer
     */
    void addListener(IAnalysisProgressListener listener);

    /**
     * Remove listener for the viewers
     *
     * @param listener
     *                     listener for each type of viewer
     */
    void removeListener(IAnalysisProgressListener listener);

    /**
     * Return the pre-defined set of segment aspects exposed by this analysis.
     *
     * It should not be null, but could be empty.
     *
     * @return The segment aspects for this analysis
     */
    Iterable<ISegmentAspect> getSegmentAspects();

    /**
     * Returns the result in a from the analysis in a ISegmentStore
     *
     * @return Results from the analysis in a ISegmentStore
     */
    @Nullable
    ISegmentStore<ISegment> getSegmentStore();

    /**
     * Returns the result in a from the analysis in a ISegmentStore
     *
     * @param predicates
     *                       The List of predicate
     *
     * @return Results from the analysis in a ISegmentStore
     * @since 4.1
     */
    default @Nullable Iterable<ISegment> getSegmentStore(Map<@NonNull Integer, @NonNull Predicate<@NonNull Map<@NonNull String, @NonNull String>>> predicates) {
        ISegmentStore segStore = getSegmentStore();
        Predicate<@NonNull Map<@NonNull String, @NonNull String>> superPredicate = (map) -> true;
        for (Predicate<@NonNull Map<@NonNull String, @NonNull String>> predicate : predicates.values()) {
            superPredicate = (Predicate<Map<String, String>>) superPredicate.and(predicate);
        }
        return Iterables.filter(segStore, superPredicate);
    }

}