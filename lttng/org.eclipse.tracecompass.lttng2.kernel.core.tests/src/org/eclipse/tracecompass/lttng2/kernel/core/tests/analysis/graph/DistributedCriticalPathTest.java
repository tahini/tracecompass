/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.lttng2.kernel.core.tests.analysis.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfEdge;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfEdge.EdgeType;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfVertex;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfVertex.EdgeDirection;
import org.eclipse.tracecompass.analysis.graph.core.building.TmfGraphBuilderModule;
import org.eclipse.tracecompass.analysis.os.linux.core.execution.graph.OsWorker;
import org.eclipse.tracecompass.analysis.os.linux.core.tests.stubs.trace.TmfXmlKernelTraceStub;
import org.eclipse.tracecompass.lttng2.kernel.core.tests.Activator;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.matching.IEventMatchingKey;
import org.eclipse.tracecompass.tmf.core.event.matching.ITmfMatchEventDefinition;
import org.eclipse.tracecompass.tmf.core.event.matching.TmfEventMatching;
import org.eclipse.tracecompass.tmf.core.event.matching.TmfEventMatching.Direction;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.junit.Before;
import org.junit.Test;

/**
 * Test critical path for distributed traces
 *
 * @author Geneviève Bastien
 */
public class DistributedCriticalPathTest {

    private static final String EXPERIMENT = "CritPathExperiment";
    private static int BLOCK_SIZE = 1000;
    private static final @NonNull String TEST_ANALYSIS_ID = "org.eclipse.tracecompass.analysis.os.linux.execgraph";

    private static class StubEventKey implements IEventMatchingKey {

        private final int fMsgId;

        /**
         * Constructor
         *
         * @param msgId
         *            A message ID
         */
        public StubEventKey(int msgId) {
            fMsgId = msgId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(fMsgId);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (o instanceof StubEventKey) {
                StubEventKey key = (StubEventKey) o;
                return key.fMsgId == fMsgId;
            }
            return false;
        }
    }

    private static class StubEventMatching implements ITmfMatchEventDefinition {

        @Override
        public IEventMatchingKey getEventKey(ITmfEvent event) {
            Integer fieldValue = event.getContent().getFieldValue(Integer.class, "msgid");
            if (fieldValue == null) {
                return null;
            }
            return new StubEventKey(fieldValue);
        }

        @Override
        public boolean canMatchTrace(ITmfTrace trace) {
            return (trace instanceof TmfXmlKernelTraceStub);
        }

        @Override
        public Direction getDirection(ITmfEvent event) {
            String evname = event.getName();
            /* Is the event a tcp socket in or out event */
            if ("net_if_receive_skb".equals(evname)) {
                return Direction.EFFECT;
            } else if ("net_dev_queue".equals(evname)) {
                return Direction.CAUSE;
            }
            return null;
        }

    }

    /**
     *
     */
    @Before
    public void setUp() {
        TmfEventMatching.registerMatchObject(new StubEventMatching());
    }

    /**
     * Setup the trace for the tests
     *
     * @param traceFiles
     *            File names relative to this plugin for the trace file to load
     * @return The trace with its graph module executed
     * @throws TmfTraceException
     */
    private ITmfTrace setUpExperiment(String... traceFiles) throws TmfTraceException {
        ITmfTrace[] traces = new ITmfTrace[traceFiles.length];
        int i = 0;
        for (String traceFile : traceFiles) {
            TmfXmlKernelTraceStub trace = new TmfXmlKernelTraceStub();
            IPath filePath = Activator.getAbsoluteFilePath(traceFile);
            IStatus status = trace.validate(null, filePath.toOSString());
            if (!status.isOK()) {
                fail(status.getException().getMessage());
            }
            trace.initTrace(null, filePath.toOSString(), ITmfEvent.class);
            traces[i++] = trace;
        }

        TmfExperiment experiment = new TmfExperiment(ITmfEvent.class, EXPERIMENT, traces, BLOCK_SIZE, null);
        experiment.traceOpened(new TmfTraceOpenedSignal(this, experiment, null));

        IAnalysisModule module = null;
        for (IAnalysisModule mod : TmfTraceUtils.getAnalysisModulesOfClass(experiment, TmfGraphBuilderModule.class)) {
            module = mod;
        }
        assertNotNull(module);
        module.schedule();
        module.waitForCompletion();
        return experiment;
    }

    /**
     * Test the graph building of a simple network exchange where one machine
     * receives in a softirq and the other receives in a threaded IRQ
     *
     * @throws TmfTraceException
     *             Exception thrown by opening experiment
     */
    @Test
    public void testNetworkExchange() throws TmfTraceException {
        ITmfTrace experiment = setUpExperiment("testfiles/graph/network_exchange_eth.xml", "testfiles/graph/network_exchange_wifi.xml");
        assertNotNull(experiment);
        try {
            internalTestNetworkExchange(experiment);
        } finally {
            experiment.dispose();
        }
    }

    private static void internalTestNetworkExchange(@NonNull ITmfTrace experiment) {
        TmfGraphBuilderModule module = TmfTraceUtils.getAnalysisModuleOfClass(experiment, TmfGraphBuilderModule.class, TEST_ANALYSIS_ID);
        assertNotNull(module);
        module.schedule();
        assertTrue(module.waitForCompletion());

        TmfGraph graph = module.getGraph();
        assertNotNull(graph);

        Set<IGraphWorker> workers = graph.getWorkers();
        assertEquals(7, workers.size());

        // Prepare a worker map
        final int swapperThread = 0;
        final int irqThread = 50;
        final int clientThread = 200;
        final int otherClient =  201;
        final int serverThread = 100;
        final int otherServer = 101;
        final int kernelThread = -1;
        Map<Integer, IGraphWorker> workerMap = new HashMap<>();
        for (IGraphWorker worker : workers) {
            workerMap.put(((OsWorker) worker).getHostThread().getTid(), worker);
        }
        for (IGraphWorker worker : workers) {
            assertTrue(worker instanceof OsWorker);
            OsWorker lttngWorker = (OsWorker) worker;
            switch (lttngWorker.getHostThread().getTid()) {
            case swapperThread: {
                // swapper of wifi trace
                List<TmfVertex> nodesOf = graph.getNodesOf(lttngWorker);
                assertEquals(2, nodesOf.size());
                /* Check first vertice has outgoing edge preempted */
                TmfVertex v = nodesOf.get(0);
                assertEquals(15, v.getTs());
                assertNull(v.getEdge(EdgeDirection.INCOMING_HORIZONTAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.OUTGOING_VERTICAL_EDGE));
                TmfEdge edge = v.getEdge(EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
                assertNotNull(edge);
                assertEquals(EdgeType.RUNNING, edge.getType());

                /* Check second vertice has outgoing edge running */
                v = nodesOf.get(1);
                assertEquals(v, edge.getVertexTo());
                assertEquals(60, v.getTs());
                assertNull(v.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.OUTGOING_VERTICAL_EDGE));
                edge = v.getEdge(EdgeDirection.INCOMING_HORIZONTAL_EDGE);
                assertNotNull(edge);
                assertEquals(EdgeType.RUNNING, edge.getType());
                assertNull(v.getEdge(EdgeDirection.OUTGOING_HORIZONTAL_EDGE));
            }
                break;
            case irqThread: {
                // threaded IRQ thread
                List<TmfVertex> nodesOf = graph.getNodesOf(lttngWorker);
                assertEquals(5, nodesOf.size());

                /* Check first vertice has outgoing edge preempted */
                TmfVertex v = nodesOf.get(0);
                assertEquals(55, v.getTs());
                assertNull(v.getEdge(EdgeDirection.INCOMING_HORIZONTAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.OUTGOING_VERTICAL_EDGE));
                TmfEdge edge = v.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE);
                assertNotNull(edge);
                assertEquals(workerMap.get(swapperThread), graph.getParentOf(edge.getVertexFrom()));
                edge = v.getEdge(EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
                assertNotNull(edge);
                assertEquals(EdgeType.PREEMPTED, edge.getType());

                /* Check second vertice has outgoing edge running */
                v = nodesOf.get(1);
                assertEquals(v, edge.getVertexTo());
                assertEquals(60, v.getTs());
                assertNull(v.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.OUTGOING_VERTICAL_EDGE));
                assertNotNull(v.getEdge(EdgeDirection.INCOMING_HORIZONTAL_EDGE));
                edge = v.getEdge(EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
                assertNotNull(edge);
                assertEquals(EdgeType.RUNNING, edge.getType());

                /* Check third vertice has outgoing edge preempted */
                v = nodesOf.get(2);
                assertEquals(v, edge.getVertexTo());
                assertEquals(65, v.getTs());
                assertNull(v.getEdge(EdgeDirection.OUTGOING_VERTICAL_EDGE));
                edge = v.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE);
                assertNotNull(edge);
                assertEquals(workerMap.get(serverThread), graph.getParentOf(edge.getVertexFrom()));
                assertEquals(EdgeType.NETWORK, edge.getType());
                assertNotNull(v.getEdge(EdgeDirection.INCOMING_HORIZONTAL_EDGE));
                edge = v.getEdge(EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
                assertNotNull(edge);
                assertEquals(EdgeType.RUNNING, edge.getType());

                /* Check 4th vertice */
                v = nodesOf.get(3);
                assertEquals(v, edge.getVertexTo());
                assertEquals(70, v.getTs());
                assertNull(v.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE));
                edge = v.getEdge(EdgeDirection.OUTGOING_VERTICAL_EDGE);
                assertNotNull(edge);
                assertEquals(workerMap.get(clientThread), graph.getParentOf(edge.getVertexTo()));
                assertNotNull(v.getEdge(EdgeDirection.INCOMING_HORIZONTAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.OUTGOING_HORIZONTAL_EDGE));

                /* Check 5th vertice */
                v = nodesOf.get(4);
                assertEquals(v, edge.getVertexTo());
                assertEquals(75, v.getTs());
                assertNull(v.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.OUTGOING_VERTICAL_EDGE));
                assertNotNull(v.getEdge(EdgeDirection.INCOMING_HORIZONTAL_EDGE));
                assertNotNull(v.getEdge(EdgeDirection.OUTGOING_HORIZONTAL_EDGE));
            }
                break;
            case serverThread: {
                // server thread of the ethernet trace
                List<TmfVertex> nodesOf = graph.getNodesOf(lttngWorker);
                assertEquals(5, nodesOf.size());

                /* Check first vertice has incoming vertical edge */
                TmfVertex v = nodesOf.get(0);
                assertEquals(5, v.getTs());
                assertNull(v.getEdge(EdgeDirection.INCOMING_HORIZONTAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.OUTGOING_VERTICAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE));
                TmfEdge edge = v.getEdge(EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
                assertNotNull(edge);
                assertEquals(EdgeType.NETWORK, edge.getType());

                /* Check second vertice has incoming vertical edge */
                v = nodesOf.get(1);
                assertEquals(35, v.getTs());
                assertNull(v.getEdge(EdgeDirection.INCOMING_HORIZONTAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.OUTGOING_VERTICAL_EDGE));
                assertNotNull(v.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE));
                edge = v.getEdge(EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
                assertNotNull(edge);
                assertEquals(EdgeType.PREEMPTED, edge.getType());

                /* Check third vertice, the process has CPU */
                v = nodesOf.get(2);
                assertEquals(v, edge.getVertexTo());
                assertEquals(40, v.getTs());
                assertNull(v.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.OUTGOING_VERTICAL_EDGE));
                assertNotNull(v.getEdge(EdgeDirection.INCOMING_HORIZONTAL_EDGE));
                edge = v.getEdge(EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
                assertNotNull(edge);
                assertEquals(EdgeType.RUNNING, edge.getType());

                /* Check 4th vertex, from which the packet is sent */
                v = nodesOf.get(3);
                assertEquals(v, edge.getVertexTo());
                assertEquals(45, v.getTs());
                assertNull(v.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE));
                edge = v.getEdge(EdgeDirection.OUTGOING_VERTICAL_EDGE);
                assertNotNull(edge);
                assertEquals(EdgeType.NETWORK, edge.getType());
                assertNotNull(v.getEdge(EdgeDirection.INCOMING_HORIZONTAL_EDGE));
                edge = v.getEdge(EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
                assertNotNull(edge);
                assertEquals(EdgeType.RUNNING, edge.getType());

                /* Check 5th vertex, the process is sheduled out */
                v = nodesOf.get(4);
                assertEquals(v, edge.getVertexTo());
                assertEquals(55, v.getTs());
                assertNull(v.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.OUTGOING_VERTICAL_EDGE));
                assertNotNull(v.getEdge(EdgeDirection.INCOMING_HORIZONTAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.OUTGOING_HORIZONTAL_EDGE));
            }
                break;
            case otherServer: {
                // other thread in ethernet trace
                List<TmfVertex> nodesOf = graph.getNodesOf(lttngWorker);
                assertEquals(2, nodesOf.size());
                /* Check first vertice has outgoing edge preempted */
                TmfVertex v = nodesOf.get(0);
                assertEquals(5, v.getTs());
                assertNull(v.getEdge(EdgeDirection.INCOMING_HORIZONTAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.OUTGOING_VERTICAL_EDGE));
                TmfEdge edge = v.getEdge(EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
                assertNotNull(edge);
                assertEquals(EdgeType.RUNNING, edge.getType());

                /* Check second vertex */
                v = nodesOf.get(1);
                assertEquals(v, edge.getVertexTo());
                assertEquals(40, v.getTs());
                assertNull(v.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.OUTGOING_VERTICAL_EDGE));
                assertNotNull(v.getEdge(EdgeDirection.INCOMING_HORIZONTAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.OUTGOING_HORIZONTAL_EDGE));
            }
                break;
            case clientThread: {
                // client thread of the wifi trace
                List<TmfVertex> nodesOf = graph.getNodesOf(lttngWorker);
                assertEquals(5, nodesOf.size());
                /* Check first vertice has outgoing edge preempted */
                TmfVertex v = nodesOf.get(0);
                assertEquals(10, v.getTs());
                assertNull(v.getEdge(EdgeDirection.INCOMING_HORIZONTAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.OUTGOING_VERTICAL_EDGE));
                TmfEdge edge = v.getEdge(EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
                assertNotNull(edge);
                assertEquals(EdgeType.RUNNING, edge.getType());

                /* Check second vertex, the send */
                v = nodesOf.get(1);
                assertEquals(v, edge.getVertexTo());
                assertEquals(13, v.getTs());
                assertNull(v.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE));
                assertNotNull(v.getEdge(EdgeDirection.INCOMING_HORIZONTAL_EDGE));
                edge = v.getEdge(EdgeDirection.OUTGOING_VERTICAL_EDGE);
                assertNotNull(edge);
                assertEquals(EdgeType.NETWORK, edge.getType());
                assertEquals(workerMap.get(serverThread), graph.getParentOf(edge.getVertexTo()));
                edge = v.getEdge(EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
                assertNotNull(edge);
                assertEquals(EdgeType.RUNNING, edge.getType());

                /* Check third vertex, scheduled out */
                v = nodesOf.get(2);
                assertEquals(v, edge.getVertexTo());
                assertEquals(15, v.getTs());
                assertNull(v.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.OUTGOING_VERTICAL_EDGE));
                assertNotNull(v.getEdge(EdgeDirection.INCOMING_HORIZONTAL_EDGE));
                edge = v.getEdge(EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
                assertNotNull(edge);
                assertEquals(EdgeType.BLOCKED, edge.getType());

                /* Check 4th vertex, woken up */
                v = nodesOf.get(3);
                assertEquals(v, edge.getVertexTo());
                assertEquals(70, v.getTs());
                assertNull(v.getEdge(EdgeDirection.OUTGOING_VERTICAL_EDGE));
                assertNotNull(v.getEdge(EdgeDirection.INCOMING_HORIZONTAL_EDGE));
                edge = v.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE);
                assertNotNull(edge);
                assertEquals(EdgeType.PREEMPTED, edge.getType());
                assertEquals(workerMap.get(irqThread), graph.getParentOf(edge.getVertexFrom()));
                edge = v.getEdge(EdgeDirection.OUTGOING_HORIZONTAL_EDGE);
                assertNotNull(edge);
                assertEquals(EdgeType.BLOCKED, edge.getType());

                /* Check 5th vertex, scheduled in */
                v = nodesOf.get(3);
                assertEquals(v, edge.getVertexTo());
                assertEquals(75, v.getTs());
                assertNull(v.getEdge(EdgeDirection.OUTGOING_VERTICAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE));
                assertNotNull(v.getEdge(EdgeDirection.INCOMING_HORIZONTAL_EDGE));
                assertNotNull(v.getEdge(EdgeDirection.OUTGOING_HORIZONTAL_EDGE));
            }
                break;
            case otherClient: {
                // other thread in the wifi trace
                List<TmfVertex> nodesOf = graph.getNodesOf(lttngWorker);
                assertEquals(1, nodesOf.size());
                /* Check first vertice has outgoing edge preempted */
                TmfVertex v = nodesOf.get(0);
                assertEquals(5, v.getTs());
                assertNull(v.getEdge(EdgeDirection.INCOMING_HORIZONTAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.INCOMING_VERTICAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.OUTGOING_VERTICAL_EDGE));
                assertNull(v.getEdge(EdgeDirection.OUTGOING_HORIZONTAL_EDGE));
            }
                break;
            case kernelThread:
                // Kernel thread
                // Do not test
                break;
            default:
                fail("Unknown worker");
                break;
            }
        }
    }

}
