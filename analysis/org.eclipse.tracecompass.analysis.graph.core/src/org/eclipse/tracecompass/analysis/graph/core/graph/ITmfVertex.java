/*******************************************************************************
 * Copyright (c) 2021 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.graph.core.graph;

import java.util.Comparator;
import java.util.Objects;


/**
 * Interface for vertices
 *
 * @author Geneviève Bastien
 * @since 2.2
 */
public interface ITmfVertex extends Comparable<ITmfVertex>{
    /**
     * Describe the four edges coming in and out of a vertex
     */
    enum EdgeDirection {
        /**
         * Constant for the outgoing vertical edge (to other object)
         */
        OUTGOING_VERTICAL_EDGE,
        /**
         * Constant for the incoming vertical edge (from other object)
         */
        INCOMING_VERTICAL_EDGE,
        /**
         * Constant for the outgoing horizontal edge (to same object)
         */
        OUTGOING_HORIZONTAL_EDGE,
        /**
         * Constant for the incoming horizontal edge (from same object)
         */
        INCOMING_HORIZONTAL_EDGE
    }

    /**
     * Compare vertices by ascending timestamps
     */
    static Comparator<ITmfVertex> ascending = Objects.requireNonNull(Comparator.nullsLast(Comparator.comparing(ITmfVertex::getTs)));

    /**
     * Compare vertices by descending timestamps
     */
    static Comparator<ITmfVertex> descending = Objects.requireNonNull(Comparator.nullsLast(Comparator.comparing(ITmfVertex::getTs).reversed()));

    /**
     * Returns the timestamps of this node
     *
     * @return the timstamp
     */
    public long getTs();

}