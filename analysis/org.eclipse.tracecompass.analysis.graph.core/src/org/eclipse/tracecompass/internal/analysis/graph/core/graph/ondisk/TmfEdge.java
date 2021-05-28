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

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdge;

/**
 * Edge of a TmfGraph
 *
 * @author Francis Giraldeau
 * @author Geneviève Bastien
 */
public class TmfEdge implements ITmfEdge {

    private final TmfVertex fVertexFrom;
    private final TmfVertex fVertexTo;
    private EdgeType fType;
    private @Nullable String fQualifier = null;

    /**
     * Constructor
     *
     * @param from
     *            The vertex this edge leaves from
     * @param to
     *            The vertex the edge leads to
     */
    public TmfEdge(TmfVertex from, TmfVertex to) {
        fVertexFrom = from;
        fVertexTo = to;
        fType = EdgeType.DEFAULT;
    }

    @Override
    public TmfVertex getVertexFrom() {
        return fVertexFrom;
    }

    @Override
    public TmfVertex getVertexTo() {
        return fVertexTo;
    }

    @Override
    public EdgeType getEdgeType() {
        return fType;
    }

    @Override
    public void setEdgeType(final EdgeType type) {
        fType = type;
    }

    @Override
    public void setEdgeType(EdgeType type, @Nullable String linkQualifier) {
        fType = type;
        fQualifier = linkQualifier;
    }

    @Override
    public @Nullable String getLinkQualifier() {
        return fQualifier;
    }

    @Override
    public long getDuration() {
        return fVertexTo.getTs() - fVertexFrom.getTs();
    }

    @Override
    public String toString() {
        return "[" + fVertexFrom + "--" + fType + "->" + fVertexTo + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

}
