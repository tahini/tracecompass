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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdge.EdgeType;
import org.eclipse.tracecompass.internal.analysis.graph.core.base.Messages;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.TmfEdge;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.TmfVertex;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.TmfVertex.EdgeDirection;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.VerticalEdgeSegment;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.segmentstore.core.SegmentStoreFactory;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.StateSystemFactory;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils.QuarkIterator;
import org.eclipse.tracecompass.statesystem.core.backend.IStateHistoryBackend;
import org.eclipse.tracecompass.statesystem.core.backend.StateHistoryBackendFactory;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;

/**
 * @author Geneviève Bastien
 * @since 2.2
 */
public class TmfGraphOnDisk implements ITmfGraph {

    private static int VERSION = 1;
    private static final String WORKERS = "WORKERS"; //$NON-NLS-1$
    private @Nullable ITmfStateSystemBuilder fSs;
    private @Nullable ISegmentStore<ISegment> fSegStore;
    private BiMap<IGraphWorker, Integer> fWorkerAttrib = HashBiMap.create();
    // Maps a state system attribute to the timestamp of the latest vertex
    private Map<Integer, Long> fCurrentWorkerLatestTime = new HashMap<>();
    private Integer fCount = 0;
    private final WorkerSerializer fWorkerSerializer;

    public interface WorkerSerializer {
        void serialize(Map<IGraphWorker, Integer> workerAttrib);
        Map<IGraphWorker, Integer> deserialize();
    }


    public TmfGraphOnDisk(WorkerSerializer workerSerializer) {
        fWorkerSerializer = workerSerializer;
    }

    public void init(String id, File htFile, Path segmentFile) throws IOException {
        /* If the target file already exists, do not rebuild it uselessly */
        // TODO for now we assume it's complete. Might be a good idea to check
        // at least if its range matches the trace's range.

        if (htFile.exists()) {
            /* Load an existing history */
            final int version = VERSION;
            try {
                IStateHistoryBackend backend = StateHistoryBackendFactory.createHistoryTreeBackendExistingFile(
                        id, htFile, version);
                fSs = StateSystemFactory.newStateSystem(backend, false);
            } catch (IOException e) {
                /*
                 * There was an error opening the existing file. Perhaps it was
                 * corrupted, perhaps it's an old version? We'll just
                 * fall-through and try to build a new one from scratch instead.
                 */
            }
        }

        if (fSs == null) {
            /* Size of the blocking queue to use when building a state history */
            final int QUEUE_SIZE = 10000;

            IStateHistoryBackend backend = StateHistoryBackendFactory.createHistoryTreeBackendNewFile(
                    id, htFile, VERSION, 0, QUEUE_SIZE);
            fSs = StateSystemFactory.newStateSystem(backend);
        }

        ISegmentStore<ISegment> segStore = SegmentStoreFactory.createOnDiskSegmentStore(segmentFile, VerticalEdgeSegment.EDGE_STATE_VALUE_FACTORY, VERSION);
        fSegStore = segStore;

        Map<IGraphWorker, Integer> deserialize = fWorkerSerializer.deserialize();
        fWorkerAttrib.putAll(deserialize);

    }

    public ITmfStateSystemBuilder getStateSystem() {
        return Objects.requireNonNull(fSs);
    }

    public ISegmentStore<ISegment> getSegmentStore() {
        return Objects.requireNonNull(fSegStore);
    }

    @Override
    public TmfVertex createVertex(IGraphWorker worker, long timestamp) {
        ITmfStateSystemBuilder ss = getStateSystem();
        Integer attribute = fWorkerAttrib.computeIfAbsent(worker, (graphWorker) -> ss.getQuarkAbsoluteAndAdd(WORKERS, String.valueOf(fCount++)));
        return new TmfVertex(timestamp, attribute);
    }

    private void checkCurrentWorkerTime(TmfVertex vertex) {
        Long latestTime = fCurrentWorkerLatestTime.getOrDefault(vertex.getAttribute(), 0L);
        if (latestTime > vertex.getTs()) {
            throw new IllegalArgumentException("Vertex is earlier than latest time for this worker"); //$NON-NLS-1$
        }
    }

    @Override
    public void add(TmfVertex vertex) {
        checkCurrentWorkerTime(vertex);
        ITmfStateSystemBuilder ss = getStateSystem();
        int attribute = vertex.getAttribute();
        Object currentEdge = ss.queryOngoing(attribute);
        if (currentEdge != null) {
            ss.updateOngoingState(EdgeType.NO_EDGE.ordinal(), attribute);
        }
        ss.modifyAttribute(vertex.getTs(), EdgeType.EPS.ordinal(), attribute);
        fCurrentWorkerLatestTime.put(attribute, vertex.getTs());
    }

    @Override
    public @Nullable TmfEdge append(TmfVertex vertex) {
        return append(vertex, EdgeType.DEFAULT);
    }

    @Override
    public @Nullable TmfEdge append(TmfVertex vertex, EdgeType type) {
        checkCurrentWorkerTime(vertex);
        ITmfStateSystemBuilder ss = getStateSystem();
        int attribute = vertex.getAttribute();
        Object currentEdge = ss.queryOngoing(attribute);
        if (currentEdge != null) {
            ss.updateOngoingState(type.ordinal(), attribute);
        }
        ss.modifyAttribute(vertex.getTs(), EdgeType.EPS.ordinal(), attribute);
        fCurrentWorkerLatestTime.put(attribute, vertex.getTs());
        try {
            return vertex.getTs() == ss.getStartTime() || currentEdge == null ? null : EdgeFactory.createEdge(ss.querySingleState(vertex.getTs() - 1, attribute));
        } catch (StateSystemDisposedException e) {
            return null;
        }
    }

    @Override
    public @Nullable TmfEdge append(TmfVertex vertex, EdgeType type, @Nullable String linkQualifier) {
        checkCurrentWorkerTime(vertex);
        ITmfStateSystemBuilder ss = getStateSystem();
        int attribute = vertex.getAttribute();
        Object currentEdge = ss.queryOngoing(attribute);
        if (currentEdge != null) {
            ss.updateOngoingState(type.ordinal(), attribute);
        }
        ss.modifyAttribute(vertex.getTs(), EdgeType.EPS.ordinal(), attribute);
        fCurrentWorkerLatestTime.put(attribute, vertex.getTs());
        try {
            return vertex.getTs() == ss.getStartTime() || currentEdge == null ? null : EdgeFactory.createEdge(ss.querySingleState(vertex.getTs() - 1, attribute));
        } catch (StateSystemDisposedException e) {
            return null;
        }
    }

    private static @Nullable ISegment findEdgeSegment(ISegmentStore<ISegment> segmentStore, long ts, int attribute, boolean from) {
        for (ISegment interval : segmentStore.getIntersectingElements(ts)) {
            if (interval instanceof VerticalEdgeSegment) {
                VerticalEdgeSegment edgeValue = (VerticalEdgeSegment) interval;
                if (from && interval.getStart() == ts && edgeValue.getAttributeFrom() == attribute) {
                    return interval;
                } else if (!from && interval.getEnd() == ts && edgeValue.getAttributeTo() == attribute) {
                    return interval;
                }
            }
        }

        return null;
    }

    @Override
    public @Nullable TmfEdge getEdgeFrom(TmfVertex vertex, EdgeDirection direction) {
        try {
            ITmfStateSystemBuilder ss = getStateSystem();
            ISegmentStore<ISegment> segmentStore = getSegmentStore();
            switch (direction) {
            case INCOMING_HORIZONTAL_EDGE: {
                if (vertex.getTs() == ss.getStartTime()) {
                    return null;
                }
                ITmfStateInterval interval = ss.querySingleState(vertex.getTs() - 1, vertex.getAttribute());
                return EdgeFactory.createEdge(interval);
            }
            case INCOMING_VERTICAL_EDGE:
            {
                ISegment interval = findEdgeSegment(segmentStore, vertex.getTs(), vertex.getAttribute(), false);
                return interval != null ? EdgeFactory.createEdge(interval) : null;
            }
            case OUTGOING_HORIZONTAL_EDGE: {
                ITmfStateInterval interval = ss.querySingleState(vertex.getTs(), vertex.getAttribute());

                return EdgeFactory.createEdge(interval);
            }
            case OUTGOING_VERTICAL_EDGE: {
                ISegment interval = findEdgeSegment(segmentStore, vertex.getTs(), vertex.getAttribute(), true);
                return interval != null ? EdgeFactory.createEdge(interval) : null;
            }
            default:
                break;

            }

        } catch (StateSystemDisposedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public @Nullable TmfEdge link(TmfVertex from, TmfVertex to) {
        return link(from, to, EdgeType.DEFAULT);
    }

    @Override
    public @Nullable TmfEdge link(TmfVertex from, TmfVertex to, EdgeType type) {
        return link(from, to, type, StringUtils.EMPTY);
    }

    @Override
    public @Nullable TmfEdge link(TmfVertex from, TmfVertex to, EdgeType type, String linkQualifier) {
        if (from.equals(to)) {
            throw new IllegalArgumentException("A node cannot link to itself"); //$NON-NLS-1$
        }

        try {
            // Are vertexes in the graph? From should be, to can be added
            if (!isVertexInGraph(from)) {
                throw new IllegalArgumentException(Messages.TmfGraph_FromNotInGraph);
            }
            boolean toInGraph = isVertexInGraph(to);
            // Are they from the same worker? Add horizontal link
            if (from.getAttribute() == to.getAttribute()) {
                return append(to, type, linkQualifier);
            }
            // From different workers, add a vertical link
            if (!toInGraph) {
                add(to);
            }
            ISegmentStore<ISegment> segmentStore = getSegmentStore();
            VerticalEdgeSegment verticalEdgeSegment = new VerticalEdgeSegment(from.getTs(), to.getTs(), from.getAttribute(), to.getAttribute(), type, linkQualifier);
            segmentStore.add(verticalEdgeSegment);
            return EdgeFactory.createEdge(verticalEdgeSegment);
        } catch (StateSystemDisposedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private boolean isVertexInGraph(TmfVertex from) throws StateSystemDisposedException {
        Long ts = fCurrentWorkerLatestTime.get(from.getAttribute());
        if (ts == null || from.getTs() > ts) {
            return false;
        }
        if (from.getTs() == ts) {
            return true;
        }
        ITmfStateInterval interval = getStateSystem().querySingleState(from.getTs(), from.getAttribute());
        // The start time should be the timestamp, otherwise, it means the vertex is not in the graph and can't be added
        return (interval.getStartTime() == from.getTs());
    }

    @Override
    public @Nullable TmfVertex getTail(IGraphWorker worker) {
        Integer attribute = fWorkerAttrib.get(worker);
        if (attribute == null) {
            return null;
        }
        ITmfStateSystemBuilder ss = getStateSystem();
        try {
            long currentEndTime = ss.getCurrentEndTime();
            ITmfStateInterval interval = ss.querySingleState(currentEndTime, attribute);
            Object value = interval.getValue();
            if (value == null || (Integer) value == EdgeType.EPS.ordinal()) {
                return new TmfVertex(interval.getStartTime(), attribute);
            }
            return new TmfVertex(currentEndTime, attribute);
        } catch (StateSystemDisposedException e) {
            // Nothing to do, but don't know about the head, just return vertex
            return null;
        }
    }

    @Override
    public @Nullable TmfVertex getHead(IGraphWorker worker) {
        Integer attribute = fWorkerAttrib.get(worker);
        if (attribute == null) {
            return null;
        }
        return getHeadForAttribute(attribute);
    }

    @Override
    public TmfVertex getHead(TmfVertex vertex) {
        TmfVertex headVertex = vertex;
        TmfEdge edgeFrom = getEdgeFrom(vertex, EdgeDirection.INCOMING_HORIZONTAL_EDGE);
        while (edgeFrom != null) {
            headVertex = edgeFrom.getVertexFrom();
            edgeFrom = getEdgeFrom(headVertex, EdgeDirection.INCOMING_HORIZONTAL_EDGE);
        }
        return headVertex;
    }

    private @Nullable TmfVertex getHeadForAttribute(int attribute) {
        ITmfStateSystemBuilder ss = getStateSystem();
        try {
            ITmfStateInterval interval = ss.querySingleState(ss.getStartTime(), attribute);
            Object value = interval.getValue();
            if (value == null) {
                return new TmfVertex(interval.getEndTime() + 1, attribute);
            }
            return new TmfVertex(interval.getStartTime(), attribute);
        } catch (StateSystemDisposedException e) {
            // Return null, nothing to do
            return null;
        }
    }

    @Override
    public Iterator<TmfVertex> getNodesOf(IGraphWorker worker) {
        ITmfStateSystemBuilder ss = getStateSystem();
        Integer attribute = fWorkerAttrib.get(worker);
        if (attribute == null) {
            return Collections.emptyIterator();
        }
        long currentEndTime = ss.waitUntilBuilt(0) ? ss.getCurrentEndTime() : ss.getCurrentEndTime() + 1;
        QuarkIterator quarkIterator = new StateSystemUtils.QuarkIterator(ss, attribute, ss.getStartTime(), currentEndTime, 1);
        return new Iterator<TmfVertex>() {

            private boolean fGotFirst = false;
            private @Nullable ITmfStateInterval fCurrentInterval = null;

            @Override
            public boolean hasNext() {
                if (fCurrentInterval != null) {
                    return true;
                }
                if (!quarkIterator.hasNext()) {
                    return false;
                }
                fCurrentInterval = quarkIterator.next();
                return fCurrentInterval.getValue() != (Integer) EdgeType.EPS.ordinal();
            }

            @Override
            public TmfVertex next() {
                hasNext();
                ITmfStateInterval next = fCurrentInterval;
                if (next == null) {
                    throw new NoSuchElementException();
                }
                if (!fGotFirst) {
                    fGotFirst = true;
                    if (next.getValue() != null) {
                        // First interval is not null, so we return a vertex at the start time, and keep the interval for next query
                        return new TmfVertex(Math.min(currentEndTime, next.getStartTime()), attribute);
                    }
                }
                fCurrentInterval = null;
                return new TmfVertex(Math.min(currentEndTime, next.getEndTime() + 1), attribute);
            }

        };
    }

    @Override
    public @Nullable IGraphWorker getParentOf(TmfVertex node) {
        return fWorkerAttrib.inverse().get(node.getAttribute());
    }

    @Override
    public Set<IGraphWorker> getWorkers() {
        return fWorkerAttrib.keySet();
    }

    @Override
    public @Nullable TmfVertex getVertexAt(ITmfTimestamp startTime, IGraphWorker worker) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isDoneBuilding() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void closeGraph(long endTime) {
        // Serialize the graph worker as a hash map, close state system and segment
        // store
        fWorkerSerializer.serialize(ImmutableMap.copyOf(fWorkerAttrib));
        ITmfStateSystemBuilder stateSystem = getStateSystem();
        stateSystem.closeHistory(endTime);
        ISegmentStore<ISegment> segmentStore = getSegmentStore();
        segmentStore.close(false);

    }

}
