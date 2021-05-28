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

package org.eclipse.tracecompass.internal.analysis.graph.core.graph.ondisk;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdge.EdgeType;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.VerticalEdgeSegment;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;

/**
 * Factory class to create different kind of edges from different types of
 * arguments.
 *
 * @author Geneviève Bastien
 * @since 2.2
 *
 */
public class EdgeFactory {

    public static @Nullable TmfEdge createEdge(ITmfStateInterval interval) {
        Object value = interval.getValue();
        if (value == null || (value instanceof Integer && ((Integer) value == EdgeType.EPS.ordinal() || (Integer) value == EdgeType.NO_EDGE.ordinal()))) {
            return null;
        }
        TmfEdge edge = new TmfEdge(new TmfVertex(interval.getStartTime(), interval.getAttribute()), new TmfVertex(interval.getEndTime() + 1, interval.getAttribute()));
        edge.setEdgeType(EdgeType.values()[(Integer) value]);
        return edge;
    }

    public static @Nullable TmfEdge createEdge(ISegment segment) {
        if (!(segment instanceof VerticalEdgeSegment)) {
            return null;
        }
        VerticalEdgeSegment edgeValue = (VerticalEdgeSegment) segment;
        TmfEdge edge = new TmfEdge(new TmfVertex(edgeValue.getStart(), edgeValue.getAttributeFrom()), new TmfVertex(edgeValue.getEnd(), edgeValue.getAttributeTo()));
        edge.setEdgeType(edgeValue.getEdgeType(), edgeValue.getLinkQualifier());
        return edge;
    }

}
