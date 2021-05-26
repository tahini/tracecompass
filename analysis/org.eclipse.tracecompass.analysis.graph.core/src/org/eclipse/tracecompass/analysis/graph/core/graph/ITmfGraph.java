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

package org.eclipse.tracecompass.analysis.graph.core.graph;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdge.EdgeType;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.TmfEdge;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.TmfVertex;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.TmfVertex.EdgeDirection;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;

/**
 * Interface for undirected, unweighed, timed graph data type for dependencies between
 * elements of a system.
 *
 * Vertices are timed: each vertex has a timestamp associated, so the vertex
 * belongs to an object (the key of the multimap) at a given time. This is why
 * we use a ListMultimap to represent the graph, instead of a simple list.
 *
 * @author Geneviève Bastien
 * @since 2.2
 */
public interface ITmfGraph {

    TmfVertex createVertex(IGraphWorker worker, long timestamp);

    /**
     * Add node to the provided object without linking
     *
     * @param worker
     *            The key of the object the vertex belongs to
     * @param vertex
     *            The new vertex
     */
    void add(TmfVertex vertex);

    /**
     * Add node to object's list and make horizontal link with tail.
     *
     * @param worker
     *            The key of the object the vertex belongs to
     * @param vertex
     *            The new vertex
     * @return The edge constructed
     */
    @Nullable TmfEdge append(TmfVertex vertex);

    /**
     * Add node to object's list and make horizontal link with tail.
     *
     * @param worker
     *            The key of the object the vertex belongs to
     * @param vertex
     *            The new vertex
     * @param type
     *            The type of edge to create
     * @return The edge constructed
     */
    @Nullable TmfEdge append(TmfVertex vertex, EdgeType type);

    /**
     * Add node to object's list and make horizontal link with tail.
     *
     * @param worker
     *            The key of the object the vertex belongs to
     * @param vertex
     *            The new vertex
     * @param type
     *            The type of edge to create
     * @param linkQualifier
     *            An optional qualifier to identify this link
     * @return The edge constructed
     * @since 2.1
     */
    @Nullable TmfEdge append(TmfVertex vertex, EdgeType type, @Nullable String linkQualifier);

    /**
     * Add a link between two vertices of the graph. The from vertex must be in
     * the graph. If the 'to' vertex is not in the graph, it will be appended to
     * the object the 'from' vertex is for. Otherwise a vertical or horizontal
     * link will be created between the vertices.
     *
     * Caution: this will remove without warning any previous link from the
     * 'from' vertex
     *
     * @param from
     *            The source vertex
     * @param to
     *            The destination vertex
     * @return The newly created edge
     */
    @Nullable TmfEdge link(TmfVertex from, TmfVertex to);

    /**
     * Add a link between two vertices of the graph. The from vertex must be in
     * the graph. If the 'to' vertex is not in the graph, it will be appended to
     * the object the 'from' vertex is for. Otherwise a vertical or horizontal
     * link will be created between the vertices.
     *
     * Caution: this will remove without warning any previous link from the
     * 'from' vertex
     *
     * @param from
     *            The source vertex
     * @param to
     *            The destination vertex
     * @param type
     *            The type of edge to create
     * @return The newly created edge
     */
    @Nullable TmfEdge link(TmfVertex from, TmfVertex to, EdgeType type);

    /**
     * Add a link between two vertices of the graph. The from vertex must be in the
     * graph. If the 'to' vertex is not in the graph, it will be appended to the
     * object the 'from' vertex is for. Otherwise a vertical or horizontal link will
     * be created between the vertices.
     *
     * Caution: this will remove without warning any previous link from the 'from'
     * vertex
     *
     * @param from
     *            The source vertex
     * @param to
     *            The destination vertex
     * @param type
     *            The type of edge to create
     * @param linkQualifier
     *            An optional qualifier to identify this link
     * @return The newly created edge
     * @since 2.1
     */
    @Nullable TmfEdge link(TmfVertex from, TmfVertex to, EdgeType type, String linkQualifier);

    /**
     * Returns tail node of the provided object
     *
     * @param worker
     *            The key of the object the vertex belongs to
     * @return The last vertex of obj
     */
    @Nullable TmfVertex getTail(IGraphWorker worker);

    /**
     * Removes the last vertex of the provided object
     *
     * @param worker
     *            The key of the object the vertex belongs to
     * @return The removed vertex
     */
    @Nullable TmfVertex removeTail(IGraphWorker worker);

    /**
     * Returns head node of the provided object. This is the very first node of
     * an object
     *
     * @param worker
     *            The key of the object the vertex belongs to
     * @return The head vertex
     */
    @Nullable TmfVertex getHead(IGraphWorker worker);

    /**
     * Returns the head node of the object of the nodeMap that has the earliest
     * head vertex time
     *
     * @return The head vertex
     */
    @Nullable TmfVertex getHead();

    /**
     * Returns head vertex from a given node. That is the first of the current
     * sequence of edges, the one with no left edge when going back through the
     * original vertex's left edge
     *
     * @param vertex
     *            The vertex for which to get the head
     * @return The head vertex from the requested vertex
     */
    TmfVertex getHead(TmfVertex vertex);

    /**
     * Returns all nodes of the provided object.
     *
     * @param obj
     *            The key of the object the vertex belongs to
     * @return The list of vertices for the object
     */
    Iterator<TmfVertex> getNodesOf(IGraphWorker obj);

    /**
     * Returns the object the vertex belongs to
     *
     * @param node
     *            The vertex to get the parent for
     * @return The object the vertex belongs to
     */
    @Nullable IGraphWorker getParentOf(TmfVertex node);

    /**
     * Returns the graph objects
     *
     * @return The vertex map
     */
    Set<IGraphWorker> getWorkers();

    /**
     * Returns the number of vertices in the graph
     *
     * @return number of vertices
     */
    int size();

    // ----------------------------------------------
    // Graph operations and visits
    // ----------------------------------------------

    /**
     * Visits a graph from the start vertex and every vertex of the graph having
     * a path to/from them that intersects the start vertex
     *
     * Each time the worker changes, it goes back to the beginning of the
     * current horizontal sequence and visits all nodes from there.
     *
     * Parts of the graph that are totally disjoints from paths to/from start
     * will not be visited by this method
     *
     * @param start
     *            The vertex to start the scan for
     * @param visitor
     *            The visitor
     */
    default void scanLineTraverse(final @Nullable TmfVertex start, final ITmfGraphVisitor visitor) {
        if (start == null) {
            return;
        }
        Deque<TmfVertex> stack = new ArrayDeque<>();
        HashSet<TmfVertex> visited = new HashSet<>();
        stack.add(start);
        while (!stack.isEmpty()) {
            TmfVertex curr = stack.removeFirst();
            if (visited.contains(curr)) {
                continue;
            }
            // process one line
            TmfVertex n = getHead(curr);
            visitor.visitHead(n);
            while (true) {
                visitor.visit(n);
                visited.add(n);

                // Only visit links up-right, guarantee to visit once only
                TmfEdge edge = n.getEdge(EdgeDirection.OUTGOING_VERTICAL_EDGE);
                if (edge != null) {
                    stack.addFirst(edge.getVertexTo());
                    visitor.visit(edge, false);
                }
                edge = n.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE);
                if (edge != null) {
                    stack.addFirst(edge.getVertexFrom());
                }
                edge = n.getEdge(EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
                if (edge != null) {
                    visitor.visit(edge, true);
                    n = edge.getVertexTo();
                } else {
                    // end of the horizontal list
                    break;
                }
            }
        }
    }

    /**
     * @see TmfGraph#scanLineTraverse(TmfVertex, ITmfGraphVisitor)
     *
     * @param start
     *            The worker from which to start the scan
     * @param visitor
     *            The visitor
     */
    default void scanLineTraverse(@Nullable IGraphWorker start, final ITmfGraphVisitor visitor) {
        if (start == null) {
            return;
        }
        scanLineTraverse(getHead(start), visitor);
    }

    /**
     * Return the vertex for an object at a given timestamp, or the first vertex
     * after the timestamp
     *
     * @param startTime
     *            The desired time
     * @param worker
     *            The object for which to get the vertex
     * @return Vertex at timestamp or null if no vertex at or after timestamp
     */
    @Nullable TmfVertex getVertexAt(ITmfTimestamp startTime, IGraphWorker worker);

    /**
     * Returns whether the graph is completed or not
     *
     * @return whether the graph is done building
     */
    boolean isDoneBuilding();

    /**
     * Countdown the latch to show that the graph is done building
     */
    void closeGraph(long endTime);

    @Nullable TmfEdge getEdgeFrom(TmfVertex vertex, EdgeDirection outgoingHorizontalEdge);

}
