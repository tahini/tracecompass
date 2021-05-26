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

package org.eclipse.tracecompass.internal.analysis.graph.core.graph;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdge.EdgeType;
import org.eclipse.tracecompass.datastore.core.interval.IHTIntervalReader;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.datastore.core.serialization.SafeByteBufferFactory;
import org.eclipse.tracecompass.segmentstore.core.ISegment;

public class VerticalEdgeSegment implements ISegment {

    /**
     *
     */
    private static final long serialVersionUID = -8838245801345622368L;

    /**
     * The factory to read this segment from a buffer
     */
    public static final @NonNull IHTIntervalReader<@NonNull ISegment> EDGE_STATE_VALUE_FACTORY = buffer -> {
        return new VerticalEdgeSegment(buffer.getLong(), buffer.getLong(), buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getString());
    };

    private final long fStart;
    private final long fEnd;
    private final int fAttributeFrom;
    private final int fAttributeTo;
    private EdgeType fEdgeType;
    private @Nullable String fQualifier = null;

    /**
     * Constructor
     *
     * @param from
     *            The vertex this edge leaves from
     * @param to
     *            The vertex the edge leads to
     */
    protected VerticalEdgeSegment(long start, long end, int attributeFrom, int attributeTo, int edgeType, @Nullable String qualifier) {
        fStart = start;
        fEnd = end;
        fAttributeFrom = attributeFrom;
        fAttributeTo = attributeTo;
        fEdgeType = EdgeType.values()[edgeType];
        fQualifier = qualifier;
    }

    /**
     * @param attributeFrom
     * @param attributeTo
     * @param edgeType
     * @param qualifier
     */
    public VerticalEdgeSegment(long start, long end, int attributeFrom, int attributeTo, EdgeType edgeType, @Nullable String qualifier) {
        fStart = start;
        fEnd = end;
        fAttributeFrom = attributeFrom;
        fAttributeTo = attributeTo;
        fEdgeType = edgeType;
        fQualifier = qualifier;
    }

    @Override
    public long getStart() {
        return fStart;
    }

    @Override
    public long getEnd() {
        return fEnd;
    }

    public int getAttributeFrom() {
        return fAttributeFrom;
    }

    public int getAttributeTo() {
        return fAttributeTo;
    }

    public EdgeType getEdgeType() {
        return fEdgeType;
    }

    public @Nullable String getLinkQualifier() {
        return fQualifier;
    }

    @Override
    public void writeSegment(@NonNull ISafeByteBufferWriter buffer) {
        buffer.putLong(getStart());
        buffer.putLong(getEnd());
        buffer.putInt(fAttributeFrom);
        buffer.putInt(fAttributeTo);
        buffer.putInt(fEdgeType.ordinal());
        String qualifier = fQualifier;
        buffer.putString(qualifier == null ? StringUtils.EMPTY : qualifier);
    }

    @Override
    public int getSizeOnDisk() {
        String qualifier = fQualifier;
        return 2 * Long.BYTES + 3 * Integer.BYTES + SafeByteBufferFactory.getStringSizeInBuffer(qualifier == null ? StringUtils.EMPTY : qualifier);
    }

}
