/*******************************************************************************
 * Copyright (c) 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Francis Giraldeau - Initial API and implementation
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.graph.core.tests.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfEdge.EdgeType;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.ITmfGraphVisitor;
import org.eclipse.tracecompass.analysis.graph.core.graph.TmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.graph.TmfGraphFactory;
import org.eclipse.tracecompass.analysis.graph.core.graph.TmfGraphOnDisk.WorkerSerializer;
import org.eclipse.tracecompass.analysis.graph.core.tests.stubs.TestGraphWorker;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.TmfEdge;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.TmfGraphStatistics;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.TmfVertex;
import org.eclipse.tracecompass.internal.analysis.graph.core.graph.TmfVertex.EdgeDirection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test the basic functionalities of the {@link TmfGraph}, {@link TmfVertex} and
 * {@link TmfEdge} classes.
 *
 * @author Geneviève Bastien
 * @author Francis Giraldeau
 */
public class TmfGraphOnDiskTest {

    private static final @NonNull IGraphWorker WORKER1 = new TestGraphWorker(1);
    private static final @NonNull IGraphWorker WORKER2 = new TestGraphWorker(2);
    private static final @NonNull IGraphWorker WORKER3 = new TestGraphWorker(3);
    private static final @NonNull String GRAPH_ID = "test.graph";

    private Path fGraphFile;
    private Path fGraphSegmentFile;
    private Path fGraphWorkerFiler;
    private ITmfGraph fGraph;

    public class TestWorkerSerializer implements WorkerSerializer {

        @Override
        public void serialize(@NonNull Map<@NonNull IGraphWorker, @NonNull Integer> workerAttrib) {
            // Serialize the graph worker hash map, close state system and
            // segment store
            try (FileOutputStream fos = new FileOutputStream(fGraphWorkerFiler.toString());
                    ObjectOutputStream oos = new ObjectOutputStream(fos);) {
                oos.writeObject(workerAttrib);

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        @Override
        public @NonNull Map<@NonNull IGraphWorker, @NonNull Integer> deserialize() {
            try (FileInputStream fis = new FileInputStream(fGraphWorkerFiler.toString());
                    ObjectInputStream ois = new ObjectInputStream(fis);) {

                return (Map<IGraphWorker, Integer>) ois.readObject();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return new HashMap<>();
            } catch (ClassNotFoundException c) {
                System.out.println("Class not found");
                c.printStackTrace();
                return new HashMap<>();
            }
        }

    }

    @Before
    public void createFile() throws IOException {
        fGraphFile = Files.createTempFile("tmpGraph", ".ht");
        fGraphSegmentFile = Files.createTempFile("tmpGraph", ".seg");
        fGraphWorkerFiler = Files.createTempFile("tmpGraph", ".workers");
        fGraph = Objects.requireNonNull(TmfGraphFactory.createGraphOnDisk(GRAPH_ID, fGraphFile.toFile(), fGraphSegmentFile, new TestWorkerSerializer()));
    }

    @After
    public void deleteFiles() throws IOException {
        Files.deleteIfExists(fGraphFile);
        Files.deleteIfExists(fGraphSegmentFile);
        Files.deleteIfExists(fGraphWorkerFiler);
    }

    /**
     * Test the graph constructor
     */
    @Test
    public void testDefaultConstructor() {
        ITmfGraph graph = TmfGraphFactory.createGraphOnDisk(GRAPH_ID, fGraphFile.toFile(), fGraphSegmentFile, new TestWorkerSerializer());
        assertNotNull(graph);
        Iterator<TmfVertex> it = graph.getNodesOf(WORKER1);
        assertEquals(0, ImmutableList.copyOf(it).size());
    }

    /**
     * Test the {@link TmfGraph#add(IGraphWorker, TmfVertex)} method: vertices
     * are added, but no edge between them is created
     */
    @Test
    public void testAddVertex() {

        // Add vertices for a single worker covering the entire graph.
        TmfVertex v0 = fGraph.createVertex(WORKER1, 0);
        fGraph.add(v0);
        TmfVertex v1 = fGraph.createVertex(WORKER1, 1);
        fGraph.add(v1);
        Iterator<TmfVertex> it = fGraph.getNodesOf(WORKER1);
        int count = 0;
        while (it.hasNext()) {
            TmfVertex vertex = it.next();
            assertEquals("Vertext at count " + count, count, vertex.getTs());
            fGraph.getEdgeFrom(vertex, EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
            fGraph.getEdgeFrom(vertex, EdgeDirection.INCOMING_HORIZONTAL_EDGE);
            fGraph.getEdgeFrom(vertex, EdgeDirection.OUTGOING_VERTICAL_EDGE);
            fGraph.getEdgeFrom(vertex, EdgeDirection.INCOMING_VERTICAL_EDGE);
            count++;
        }
        assertEquals(2, count);

        // Add vertices for another worker later on the graph, to make sure
        // vertex count is still ok
        TmfVertex v2 = fGraph.createVertex(WORKER2, 2);
        fGraph.add(v2);
        TmfVertex v3 = fGraph.createVertex(WORKER2, 3);
        fGraph.add(v3);
        it = fGraph.getNodesOf(WORKER1);
        count = 0;
        while (it.hasNext()) {
            TmfVertex vertex = it.next();
            assertEquals(count, vertex.getTs());
            fGraph.getEdgeFrom(vertex, EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
            fGraph.getEdgeFrom(vertex, EdgeDirection.INCOMING_HORIZONTAL_EDGE);
            fGraph.getEdgeFrom(vertex, EdgeDirection.OUTGOING_VERTICAL_EDGE);
            fGraph.getEdgeFrom(vertex, EdgeDirection.INCOMING_VERTICAL_EDGE);
            count++;
        }
        assertEquals(2, count);
        it = fGraph.getNodesOf(WORKER2);
        count = 0;
        while (it.hasNext()) {
            TmfVertex vertex = it.next();
            assertEquals(count + 2, vertex.getTs());
            fGraph.getEdgeFrom(vertex, EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
            fGraph.getEdgeFrom(vertex, EdgeDirection.INCOMING_HORIZONTAL_EDGE);
            fGraph.getEdgeFrom(vertex, EdgeDirection.OUTGOING_VERTICAL_EDGE);
            fGraph.getEdgeFrom(vertex, EdgeDirection.INCOMING_VERTICAL_EDGE);
            count++;
        }
        assertEquals(2, count);
    }

    /**
     * Test the {@link TmfGraph#append(IGraphWorker, TmfVertex)} and
     * {@link TmfGraph#append(IGraphWorker, TmfVertex, EdgeType)} methods:
     * vertices are added and links are created between them.
     */
    @Test
    public void testAppendVertex() {

        // Add vertices for a single worker covering the entire graph.
        TmfVertex v0 = fGraph.createVertex(WORKER1, 0);
        TmfEdge edge = fGraph.append(v0);
        assertNull("First edge of a worker", edge);
        TmfVertex v1 = fGraph.createVertex(WORKER1, 1);
        edge = fGraph.append(v1);
        assertNotNull(edge);
        assertEquals(EdgeType.DEFAULT, edge.getEdgeType());
        assertEquals(v1, edge.getVertexTo());
        assertEquals(v0, edge.getVertexFrom());
        assertEquals(v1.getTs() - v0.getTs(), edge.getDuration());

        Iterator<@NonNull TmfVertex> it = fGraph.getNodesOf(WORKER1);
        List<@NonNull TmfVertex> list = ImmutableList.copyOf(it);
        assertEquals(2, list.size());
        checkLinkHorizontal(list, fGraph);

        /* Append with a type */
        TmfVertex v2 = fGraph.createVertex(WORKER1, 2);
        edge = fGraph.append(v2, EdgeType.BLOCKED);
        assertNotNull(edge);
        assertEquals(EdgeType.BLOCKED, edge.getEdgeType());
        assertEquals(v2, edge.getVertexTo());
        assertEquals(v1, edge.getVertexFrom());
        assertEquals(v2.getTs() - v1.getTs(), edge.getDuration());

        it = fGraph.getNodesOf(WORKER1);
        list = ImmutableList.copyOf(it);
        assertEquals(3, list.size());
        checkLinkHorizontal(list, fGraph);

    }

    /**
     * Test that appending vertices in non chronological order gives error
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalVertex() {
        TmfVertex v0 = fGraph.createVertex(WORKER1, 0);
        TmfVertex v1 = fGraph.createVertex(WORKER1, 1);
        fGraph.add(v1);
        fGraph.add(v0);
    }

    /**
     * Test the {@link TmfGraph#link(TmfVertex, TmfVertex)} and
     * {@link TmfGraph#link(TmfVertex, TmfVertex, EdgeType)} methods
     */
    @Test
    public void testLink() {
        // Start with a first node
        TmfVertex v0 = fGraph.createVertex(WORKER1, 0);
        fGraph.add(v0);
        TmfVertex v1 = fGraph.createVertex(WORKER1, 1);

        // Link with second node not in graph
        TmfEdge edge = fGraph.link(v0, v1);
        assertNotNull(edge);
        assertEquals(EdgeType.DEFAULT, edge.getEdgeType());
        assertEquals(v1, edge.getVertexTo());
        assertEquals(v0, edge.getVertexFrom());
        assertEquals(v1.getTs() - v0.getTs(), edge.getDuration());

        Iterator<TmfVertex> it = fGraph.getNodesOf(WORKER1);
        assertEquals(2, ImmutableList.copyOf(it).size());
        TmfEdge edge1 = fGraph.getEdgeFrom(v1, EdgeDirection.INCOMING_HORIZONTAL_EDGE);
        assertNotNull(edge1);
        assertEquals(v0, edge1.getVertexFrom());
        edge1 = fGraph.getEdgeFrom(v1, EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
        assertNull(edge1);
        edge1 = fGraph.getEdgeFrom(v0, EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
        assertNotNull(edge1);
        assertEquals(v1, edge1.getVertexTo());

        // Link with second node for the same object
        TmfVertex v2 = fGraph.createVertex(WORKER1, 2);
        edge = fGraph.link(v1, v2, EdgeType.NETWORK);
        assertNotNull(edge);
        assertEquals(EdgeType.NETWORK, edge.getEdgeType());
        assertEquals(v2, edge.getVertexTo());
        assertEquals(v1, edge.getVertexFrom());
        assertEquals(v2.getTs() - v1.getTs(), edge.getDuration());

        it = fGraph.getNodesOf(WORKER1);
        assertEquals(3, ImmutableList.copyOf(it).size());
        edge1 = fGraph.getEdgeFrom(v2, EdgeDirection.INCOMING_HORIZONTAL_EDGE);
        assertNotNull(edge1);
        assertEquals(v1, edge1.getVertexFrom());
        edge1 = fGraph.getEdgeFrom(v1, EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
        assertNotNull(edge1);
        assertEquals(v2, edge1.getVertexTo());

        // Link with second node for another object
        TmfVertex v3 = fGraph.createVertex(WORKER2, 3);
        edge = fGraph.link(v2, v3, EdgeType.NETWORK);
        assertNotNull(edge);
        assertEquals(v3, edge.getVertexTo());
        assertEquals(v2, edge.getVertexFrom());
        assertEquals(EdgeType.NETWORK, edge.getEdgeType());

        it = fGraph.getNodesOf(WORKER1);
        assertEquals(3, ImmutableList.copyOf(it).size());

        it = fGraph.getNodesOf(WORKER2);
        assertEquals(1, ImmutableList.copyOf(it).size());

        edge1 = fGraph.getEdgeFrom(v2, EdgeDirection.OUTGOING_VERTICAL_EDGE);
        assertNotNull(edge1);
        assertEquals(v3, edge1.getVertexTo());
        edge1 = fGraph.getEdgeFrom(v3, EdgeDirection.INCOMING_VERTICAL_EDGE);
        assertNotNull(edge1);
        assertEquals(v2, edge1.getVertexFrom());

        // No duration vertical link with second node for another object
        TmfVertex v4 = fGraph.createVertex(WORKER3, 3);
        edge = fGraph.link(v3, v4, EdgeType.NETWORK, "test");
        assertNotNull(edge);
        assertEquals(v4, edge.getVertexTo());
        assertEquals(v3, edge.getVertexFrom());
        assertEquals(EdgeType.NETWORK, edge.getEdgeType());

        edge1 = fGraph.getEdgeFrom(v3, EdgeDirection.OUTGOING_VERTICAL_EDGE);
        assertNotNull(edge1);
        assertEquals(v4, edge1.getVertexTo());
        edge1 = fGraph.getEdgeFrom(v4, EdgeDirection.INCOMING_VERTICAL_EDGE);
        assertNotNull(edge1);
        assertEquals(v3, edge1.getVertexFrom());

    }

    /**
     * Verify that vertices in the list form a chain linked by edges and have no
     * vertical edges
     *
     * @param graph
     */
    private static void checkLinkHorizontal(List<@NonNull TmfVertex> list, ITmfGraph graph) {
        if (list.isEmpty()) {
            return;
        }
        for (int i = 0; i < list.size() - 1; i++) {
            TmfVertex v0 = list.get(i);
            TmfVertex v1 = list.get(i + 1);
            TmfEdge edgeOut = graph.getEdgeFrom(v0, EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
            assertNotNull(edgeOut);
            assertEquals(v0, edgeOut.getVertexFrom());
            assertEquals(v1, edgeOut.getVertexTo());
            TmfEdge edgeIn = graph.getEdgeFrom(v1, EdgeDirection.INCOMING_HORIZONTAL_EDGE);
            assertNotNull(edgeIn);
            assertEquals(v0, edgeIn.getVertexFrom());
            assertEquals(v1, edgeIn.getVertexTo());
            assertEquals(edgeOut.getEdgeType(), edgeIn.getEdgeType());
            assertNull(graph.getEdgeFrom(v1, EdgeDirection.OUTGOING_VERTICAL_EDGE));
            assertNull(graph.getEdgeFrom(v1, EdgeDirection.INCOMING_VERTICAL_EDGE));
            assertNull(graph.getEdgeFrom(v0, EdgeDirection.OUTGOING_VERTICAL_EDGE));
            assertNull(graph.getEdgeFrom(v0, EdgeDirection.INCOMING_VERTICAL_EDGE));
        }
    }

    /**
     * Test the {@link TmfGraph#link(TmfVertex, TmfVertex)} and
     * {@link TmfGraph#link(TmfVertex, TmfVertex, EdgeType)} methods
     */
    @Test
    public void testReReadGraph() {
        // Start with a first node
        TmfVertex v0 = fGraph.createVertex(WORKER1, 0);
        fGraph.add(v0);
        TmfVertex v1 = fGraph.createVertex(WORKER1, 1);

        // Link with second node not in graph
        TmfEdge edge = fGraph.link(v0, v1);

        TmfVertex v2 = fGraph.createVertex(WORKER1, 2);
        edge = fGraph.link(v1, v2, EdgeType.NETWORK);

        TmfVertex v3 = fGraph.createVertex(WORKER2, 3);
        edge = fGraph.link(v2, v3, EdgeType.NETWORK);

        TmfVertex v4 = fGraph.createVertex(WORKER3, 3);
        edge = fGraph.link(v3, v4, EdgeType.NETWORK, "test");

        fGraph.closeGraph(3);
        fGraph = null;

        ITmfGraph reOpenedGraph = Objects.requireNonNull(TmfGraphFactory.createGraphOnDisk(GRAPH_ID, fGraphFile.toFile(), fGraphSegmentFile, new TestWorkerSerializer()));
        // Assert the number of nodes per worker
        Iterator<TmfVertex> it = reOpenedGraph.getNodesOf(WORKER1);
        assertEquals(3, ImmutableList.copyOf(it).size());
        it = reOpenedGraph.getNodesOf(WORKER2);
        assertEquals(1, ImmutableList.copyOf(it).size());
        it = reOpenedGraph.getNodesOf(WORKER3);
        assertEquals(1, ImmutableList.copyOf(it).size());

        edge = reOpenedGraph.getEdgeFrom(v0, EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
        assertNotNull(edge);
        assertEquals(EdgeType.DEFAULT, edge.getEdgeType());
        assertEquals(v1, edge.getVertexTo());
        assertEquals(v0, edge.getVertexFrom());
        assertEquals(v1.getTs() - v0.getTs(), edge.getDuration());
        edge = reOpenedGraph.getEdgeFrom(v1, EdgeDirection.INCOMING_HORIZONTAL_EDGE);
        assertNotNull(edge);
        assertEquals(v0, edge.getVertexFrom());

        edge = reOpenedGraph.getEdgeFrom(v2, EdgeDirection.INCOMING_HORIZONTAL_EDGE);
        assertNotNull(edge);
        assertEquals(v1, edge.getVertexFrom());
        assertEquals(EdgeType.NETWORK, edge.getEdgeType());
        edge = reOpenedGraph.getEdgeFrom(v1, EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
        assertNotNull(edge);
        assertEquals(v2, edge.getVertexTo());

        edge = reOpenedGraph.getEdgeFrom(v2, EdgeDirection.OUTGOING_VERTICAL_EDGE);
        assertNotNull(edge);
        assertEquals(v3, edge.getVertexTo());
        assertEquals(EdgeType.NETWORK, edge.getEdgeType());
        edge = reOpenedGraph.getEdgeFrom(v3, EdgeDirection.INCOMING_VERTICAL_EDGE);
        assertNotNull(edge);
        assertEquals(v2, edge.getVertexFrom());

        edge = reOpenedGraph.getEdgeFrom(v3, EdgeDirection.OUTGOING_VERTICAL_EDGE);
        assertNotNull(edge);
        assertEquals(v4, edge.getVertexTo());
        edge = reOpenedGraph.getEdgeFrom(v4, EdgeDirection.INCOMING_VERTICAL_EDGE);
        assertNotNull(edge);
        assertEquals(v3, edge.getVertexFrom());

    }

    /**
     * Test the {@link ITmfGraph#getTail(IGraphWorker)} methods
     */
    @Test
    public void testTail() {
        TmfVertex v0 = fGraph.createVertex(WORKER1, 0);
        TmfVertex v1 = fGraph.createVertex(WORKER1, 1);
        TmfVertex v2 = fGraph.createVertex(WORKER2, 2);
        TmfVertex v3 = fGraph.createVertex(WORKER2, 3);
        fGraph.append(v0);
        fGraph.append(v1);
        fGraph.append(v2);
        fGraph.append(v3);
        fGraph.closeGraph(3);
        assertEquals(v1, fGraph.getTail(WORKER1));
        assertEquals(v3, fGraph.getTail(WORKER2));
    }

    /**
     * Test the {@link TmfGraph#getHead()} methods with 2 workers
     */
    @Test
    public void testHead() {
        TmfVertex v0 = fGraph.createVertex(WORKER1, 0);
        TmfVertex v1 = fGraph.createVertex(WORKER1, 1);
        TmfVertex v2 = fGraph.createVertex(WORKER2, 2);
        TmfVertex v3 = fGraph.createVertex(WORKER2, 3);
        fGraph.append(v0);
        fGraph.append(v1);
        fGraph.append(v2);
        fGraph.append(v3);
        fGraph.closeGraph(3);
        assertEquals(v0, fGraph.getHead(WORKER1));
        assertEquals(v0, fGraph.getHead(v1));
        assertEquals(v0, fGraph.getHead(v0));
        assertEquals(v2, fGraph.getHead(WORKER2));
        assertEquals(v2, fGraph.getHead(v2));
        assertEquals(v2, fGraph.getHead(v3));
    }

    /**
     * Test the {@link TmfGraph#getHead(TmfVertex)} methods with multiple
     * sequences of vertices
     */
    @Test
    public void testHeadSequence() {
        TmfVertex v0 = fGraph.createVertex(WORKER1, 0);
        TmfVertex v1 = fGraph.createVertex(WORKER1, 1);
        TmfVertex v2 = fGraph.createVertex(WORKER1, 2);
        TmfVertex v3 = fGraph.createVertex(WORKER1, 3);
        fGraph.append(v0);
        fGraph.append(v1);
        fGraph.add(v2);
        fGraph.append(v3);
        fGraph.closeGraph(3);
        assertEquals(v0, fGraph.getHead(v1));
        assertEquals(v0, fGraph.getHead(v0));
        assertEquals(v2, fGraph.getHead(v2));
        assertEquals(v2, fGraph.getHead(v3));
    }

    /**
     * The test {@link TmfGraph#getParentOf(TmfVertex)} method
     */
    @Test
    public void testParent() {
        TmfVertex v0 = fGraph.createVertex(WORKER1, 0);
        TmfVertex v1 = fGraph.createVertex(WORKER2, 1);
        fGraph.append(v0);
        fGraph.append(v1);
        assertEquals(WORKER1, fGraph.getParentOf(v0));
        assertNotSame(WORKER1, fGraph.getParentOf(v1));
        assertEquals(WORKER2, fGraph.getParentOf(v1));
    }

    private class ScanCountVertex implements ITmfGraphVisitor {
        public int nbVertex = 0;
        public int nbVLink = 0;
        public int nbHLink = 0;
        public int nbStartVertex = 0;

        @Override
        public void visitHead(TmfVertex node) {
            nbStartVertex++;
        }

        @Override
        public void visit(TmfVertex node) {
            nbVertex++;

        }

        @Override
        public void visit(TmfEdge edge, boolean horizontal) {
            if (horizontal) {
                nbHLink++;
            } else {
                nbVLink++;
            }
        }
    }

    /**
     * The following graph will be used
     *
     * <pre>
     * ____0___1___2___3___4___5___6___7___8___9___10___11___12___13___14___15
     *
     * A   *-------*       *---*-------*---*---*    *---*----*----*---------*
     *             |           |           |            |    |
     * B       *---*---*-------*   *-------*------------*    *----------*
     * </pre>
     */
    @SuppressWarnings("null")
    private void buildFullGraph() {
        TmfVertex[] vertexA;
        TmfVertex[] vertexB;
        long[] timesA = { 0, 2, 4, 5, 7, 8, 9, 10, 11, 12, 13, 15 };
        long[] timesB = { 1, 2, 3, 5, 6, 8, 11, 12, 14 };
        vertexA = new TmfVertex[timesA.length];
        vertexB = new TmfVertex[timesB.length];
        for (int i = 0; i < timesA.length; i++) {
            vertexA[i] = fGraph.createVertex(WORKER1, timesA[i]);
        }
        for (int i = 0; i < timesB.length; i++) {
            vertexB[i] = fGraph.createVertex(WORKER2, timesB[i]);
        }
        fGraph.append(vertexA[0]);
        fGraph.append(vertexA[1]);
        fGraph.add(vertexA[2]);
        fGraph.append(vertexA[3]);
        fGraph.append(vertexA[4]);
        fGraph.append(vertexA[5]);
        fGraph.append(vertexA[6]);
        fGraph.add(vertexA[7]);
        fGraph.append(vertexA[8]);
        fGraph.append(vertexA[9]);
        fGraph.append(vertexA[10]);
        fGraph.append(vertexA[11]);
        fGraph.append(vertexB[0]);
        fGraph.append(vertexB[1]);
        fGraph.append(vertexB[2]);
        fGraph.append(vertexB[3]);
        fGraph.add(vertexB[4]);
        fGraph.append(vertexB[5]);
        fGraph.append(vertexB[6]);
        fGraph.add(vertexB[7]);
        fGraph.append(vertexB[8]);
        fGraph.link(vertexA[1], vertexB[1]);
        fGraph.link(vertexB[3], vertexA[3]);
        fGraph.link(vertexA[5], vertexB[5]);
        fGraph.link(vertexB[6], vertexA[8]);
        fGraph.link(vertexA[9], vertexB[7]);
    }

    /**
     * Test the
     * {@link TmfGraph#scanLineTraverse(IGraphWorker, ITmfGraphVisitor)} method
     */
    @Test
    public void testScanCount() {
        buildFullGraph();
        ScanCountVertex visitor = new ScanCountVertex();
        fGraph.scanLineTraverse(fGraph.getHead(WORKER1), visitor);
        assertEquals(21, visitor.nbVertex);
        assertEquals(6, visitor.nbStartVertex);
        assertEquals(5, visitor.nbVLink);
        assertEquals(15, visitor.nbHLink);
    }

    /**
     * Test the {@link TmfGraphStatistics} class
     */
    @Test
    public void testGraphStatistics() {
        buildFullGraph();
        TmfGraphStatistics stats = new TmfGraphStatistics();
        stats.computeGraphStatistics(fGraph, WORKER1);
        assertEquals(12, stats.getSum(WORKER1).longValue());
        assertEquals(11, stats.getSum(WORKER2).longValue());
        assertEquals(23, stats.getSum().longValue());
    }

    /**
     * Test that exception is thrown if a node is linked horizontally to itself
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHorizontalSelfLink() {
        TmfVertex v1 = fGraph.createVertex(WORKER1, 1);
        fGraph.add(v1);
        fGraph.link(v1, v1);
    }

}
