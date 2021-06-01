/*******************************************************************************
 * Copyright (c) 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Francis Giraldeau - Initial implementation and API
 *   Geneviève Bastien - Initial implementation and API
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.graph.core.graph.ondisk;

import java.util.Comparator;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfVertex;

/**
 * Timed vertex for TmfGraph
 *
 * @author Francis Giraldeau
 * @author Geneviève Bastien
 * @since 3.0
 */
public class TmfVertex implements ITmfVertex {

    /**
     * Describe the four edges coming in and out of a vertex
     */
    public enum EdgeDirection {
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
    public static Comparator<TmfVertex> ascending = Objects.requireNonNull(Comparator.nullsLast(Comparator.comparing(TmfVertex::getTs)));

    /**
     * Compare vertices by descending timestamps
     */
    public static Comparator<TmfVertex> descending = Objects.requireNonNull(Comparator.nullsLast(Comparator.comparing(TmfVertex::getTs).reversed()));

    private final long fTimestamp;
    private final int fAttribute;


    /**
     * @param timestamp
     * @param attribute
     */
    public TmfVertex(long timestamp, Integer attribute) {
        fTimestamp = timestamp;
        fAttribute = attribute;
    }

    /*
     * Getters and setters
     */
    @Override
    public long getTs() {
        return fTimestamp;
    }

    /**
     * Returns the unique ID of this node
     *
     * @return the vertex's id
     */
    public int getAttribute() {
        return fAttribute;
    }

    @Override
    public int compareTo(@Nullable ITmfVertex other) {
        if (other == null) {
            return 1;
        }
        return Long.compare(fTimestamp, other.getTs());
    }

    @Override
    public String toString() {
        return "[" + fAttribute + "," + fTimestamp + "]"; //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        // Equal when attribute and timestamp are the same
        if (!(obj instanceof TmfVertex)) {
            return false;
        }
        TmfVertex other = (TmfVertex) obj;
        return this.fAttribute == other.fAttribute && this.fTimestamp == other.fTimestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.fAttribute, this.fTimestamp);
    }

}
