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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfEdge;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfEdge.EdgeType;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfVertex;
import org.eclipse.tracecompass.analysis.graph.core.building.TmfGraphBuilderModule;
import org.eclipse.tracecompass.analysis.graph.core.criticalpath.CriticalPathModule;
import org.eclipse.tracecompass.analysis.graph.core.tests.stubs.GraphOps;
import org.eclipse.tracecompass.analysis.os.linux.core.execution.graph.OsWorker;
import org.eclipse.tracecompass.analysis.os.linux.core.tests.stubs.trace.TmfXmlKernelTraceStub;
import org.eclipse.tracecompass.lttng2.kernel.core.tests.Activator;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
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
     * receives in a softirq and the other receives in a threaded IRQ, with new
     * network reception events
     *
     * @throws TmfTraceException
     *             Exception thrown by opening experiment
     * @throws TmfAnalysisException
     *             Exception thrown by analyses
     */
    @Test
    public void testNetworkExchangeWithWifi() throws TmfTraceException, TmfAnalysisException {
        ITmfTrace experiment = setUpExperiment("testfiles/graph/network_exchange_eth.xml", "testfiles/graph/network_exchange_wifi.xml");
        assertNotNull(experiment);
        try {
            internalTestNetworkExchangeWithWifi(experiment);
        } finally {
            experiment.dispose();
        }
    }

    private static void internalTestNetworkExchangeWithWifi(@NonNull ITmfTrace experiment) throws TmfAnalysisException {
        TmfGraphBuilderModule module = TmfTraceUtils.getAnalysisModuleOfClass(experiment, TmfGraphBuilderModule.class, TEST_ANALYSIS_ID);
        assertNotNull(module);
        module.schedule();
        assertTrue(module.waitForCompletion());

        TmfGraph graph = module.getGraph();
        assertNotNull(graph);

        Set<IGraphWorker> workers = graph.getWorkers();
        assertEquals(6, workers.size());

        // Prepare a worker map
        final int irqThread = 50;
        final int clientThread = 200;
        final int otherClient = 201;
        final int serverThread = 100;
        final int otherServer = 101;
        final int kernelThread = -1;
        Map<Integer, IGraphWorker> workerMap = new HashMap<>();
        for (IGraphWorker worker : workers) {
            workerMap.put(((OsWorker) worker).getHostThread().getTid(), worker);
        }
        // Make the expected graph
        TmfGraph expected = new TmfGraph();

        // other thread on client side
        IGraphWorker worker = workerMap.get(otherClient);
        assertNotNull(worker);
        expected.add(worker, new TmfVertex(10));
        expected.append(worker, new TmfVertex(15), EdgeType.PREEMPTED);
        expected.append(worker, new TmfVertex(60), EdgeType.RUNNING);

        // client thread
        worker = workerMap.get(clientThread);
        assertNotNull(worker);
        expected.add(worker, new TmfVertex(10));
        TmfVertex packet1Sent = new TmfVertex(13);
        expected.append(worker, packet1Sent, EdgeType.RUNNING);
        expected.append(worker, new TmfVertex(15), EdgeType.RUNNING);
        TmfVertex packet2Received = new TmfVertex(70);
        expected.append(worker, packet2Received, EdgeType.NETWORK, "irq/30-handler");
        expected.append(worker, new TmfVertex(75), EdgeType.PREEMPTED);

        // irq thread
        worker = workerMap.get(irqThread);
        assertNotNull(worker);
        expected.add(worker, new TmfVertex(55));
        expected.append(worker, new TmfVertex(60), EdgeType.PREEMPTED);
        expected.append(worker, new TmfVertex(65), EdgeType.RUNNING);
        expected.append(worker, new TmfVertex(75), EdgeType.RUNNING);

        // Other thread on server side
        worker = workerMap.get(otherServer);
        assertNotNull(worker);
        expected.add(worker, new TmfVertex(5));
        expected.append(worker, new TmfVertex(40), EdgeType.RUNNING);
        expected.append(worker, new TmfVertex(55), EdgeType.PREEMPTED);

        // Server thread
        worker = workerMap.get(serverThread);
        assertNotNull(worker);
        expected.add(worker, new TmfVertex(5));
        TmfVertex packet1Received = new TmfVertex(35);
        expected.append(worker, packet1Received, EdgeType.NETWORK);
        expected.append(worker, new TmfVertex(40), EdgeType.PREEMPTED);
        TmfVertex packet2Sent = new TmfVertex(45);
        expected.append(worker, packet2Sent, EdgeType.RUNNING);
        expected.append(worker, new TmfVertex(55), EdgeType.RUNNING);

        // Create the vertical links
        TmfEdge link = packet1Sent.linkVertical(packet1Received);
        link.setType(EdgeType.NETWORK);
        link = packet2Sent.linkVertical(packet2Received);
        link.setType(EdgeType.NETWORK);

        // kernel worker on server side
        worker = workerMap.get(kernelThread);
        assertNotNull(worker);
        expected.add(worker, new TmfVertex(30));
        expected.append(worker, new TmfVertex(33), EdgeType.RUNNING);

        GraphOps.checkEquality(expected, graph);

        // Test the critical path

        // Build the expected critical path
        expected = new TmfGraph();
        worker = workerMap.get(clientThread);
        assertNotNull(worker);
        expected.add(worker, new TmfVertex(10));
        expected.append(worker, new TmfVertex(13), EdgeType.RUNNING);
        packet1Sent = new TmfVertex(15);
        expected.append(worker, packet1Sent, EdgeType.RUNNING);
        packet2Received = new TmfVertex(70);
        expected.add(worker, packet2Received);
        expected.append(worker, new TmfVertex(75), EdgeType.PREEMPTED);

        worker = workerMap.get(serverThread);
        assertNotNull(worker);
        packet1Received = new TmfVertex(35);
        expected.add(worker, packet1Received);
        expected.append(worker, new TmfVertex(40), EdgeType.PREEMPTED);
        packet2Sent = new TmfVertex(45);
        expected.append(worker, packet2Sent, EdgeType.RUNNING);

        link = packet1Sent.linkVertical(packet1Received);
        link.setType(EdgeType.NETWORK);
        link = packet2Sent.linkVertical(packet2Received);
        link.setType(EdgeType.NETWORK);

        CriticalPathModule critPathModule = new CriticalPathModule(module);
        try {
            critPathModule.setTrace(experiment);
            critPathModule.setParameter(CriticalPathModule.PARAM_WORKER, workerMap.get(clientThread));
            critPathModule.schedule();
            assertTrue(critPathModule.waitForCompletion());

            TmfGraph criticalPath = critPathModule.getCriticalPath();
            assertNotNull(criticalPath);

            GraphOps.checkEquality(expected, criticalPath);
        } finally {
            critPathModule.dispose();
        }
    }

    /**
     * Test the graph building of a simple network exchange where both machines receive in softirqs
     *
     * @throws TmfTraceException
     *             Exception thrown by opening experiment
     * @throws TmfAnalysisException
     *             Exception thrown by analyses
     */
    @Test
    public void testNetworkExchange() throws TmfTraceException, TmfAnalysisException {
        ITmfTrace experiment = setUpExperiment("testfiles/graph/simple_network_server.xml", "testfiles/graph/simple_network_client.xml");
        assertNotNull(experiment);
        try {
            internalTestNetworkExchange(experiment);
        } finally {
            experiment.dispose();
        }
    }

    private static void internalTestNetworkExchange(@NonNull ITmfTrace experiment) throws TmfAnalysisException {
        TmfGraphBuilderModule module = TmfTraceUtils.getAnalysisModuleOfClass(experiment, TmfGraphBuilderModule.class, TEST_ANALYSIS_ID);
        assertNotNull(module);
        module.schedule();
        assertTrue(module.waitForCompletion());

        TmfGraph graph = module.getGraph();
        assertNotNull(graph);

        Set<IGraphWorker> workers = graph.getWorkers();
        assertEquals(6, workers.size());

        // Prepare a worker map
        final int clientThread = 200;
        final int otherClient = 201;
        final int serverThread = 100;
        final int otherServer = 101;
        OsWorker clientWorker = null;
        OsWorker serverWorker = null;
        Map<Integer, IGraphWorker> workerMap = new HashMap<>();
        for (IGraphWorker worker : workers) {
            OsWorker osWorker = (OsWorker) worker;
            if (osWorker.getHostThread().getTid() < 0) {
                if (osWorker.getHostId().equals("simple_network_server.xml")) {
                    serverWorker = osWorker;
                } else {
                    clientWorker = osWorker;
                }
            }
            workerMap.put(osWorker.getHostThread().getTid(), worker);
        }
        // Make the expected graph
        TmfGraph expected = new TmfGraph();

        // other thread on client side
        IGraphWorker worker = workerMap.get(otherClient);
        assertNotNull(worker);
        expected.add(worker, new TmfVertex(10));
        expected.append(worker, new TmfVertex(15), EdgeType.PREEMPTED);
        expected.append(worker, new TmfVertex(75), EdgeType.RUNNING);

        // client thread
        worker = workerMap.get(clientThread);
        assertNotNull(worker);
        expected.add(worker, new TmfVertex(10));
        TmfVertex packet1Sent = new TmfVertex(13);
        expected.append(worker, packet1Sent, EdgeType.RUNNING);
        expected.append(worker, new TmfVertex(15), EdgeType.RUNNING);
        TmfVertex packet2Received = new TmfVertex(70);
        expected.append(worker, packet2Received, EdgeType.NETWORK);
        expected.append(worker, new TmfVertex(75), EdgeType.PREEMPTED);

        // client kernel worker
        worker = clientWorker;
        assertNotNull(worker);
        expected.add(worker, new TmfVertex(60));
        expected.append(worker, new TmfVertex(65), EdgeType.RUNNING);

        // Other thread on server side
        worker = workerMap.get(otherServer);
        assertNotNull(worker);
        expected.add(worker, new TmfVertex(5));
        expected.append(worker, new TmfVertex(40), EdgeType.RUNNING);
        expected.append(worker, new TmfVertex(55), EdgeType.PREEMPTED);

        // Server thread
        worker = workerMap.get(serverThread);
        assertNotNull(worker);
        expected.add(worker, new TmfVertex(5));
        TmfVertex packet1Received = new TmfVertex(35);
        expected.append(worker, packet1Received, EdgeType.NETWORK);
        expected.append(worker, new TmfVertex(40), EdgeType.PREEMPTED);
        TmfVertex packet2Sent = new TmfVertex(45);
        expected.append(worker, packet2Sent, EdgeType.RUNNING);
        expected.append(worker, new TmfVertex(55), EdgeType.RUNNING);

        // Create the vertical links
        TmfEdge link = packet1Sent.linkVertical(packet1Received);
        link.setType(EdgeType.NETWORK);
        link = packet2Sent.linkVertical(packet2Received);
        link.setType(EdgeType.NETWORK);

        // kernel worker on server side
        worker = serverWorker;
        assertNotNull(worker);
        expected.add(worker, new TmfVertex(30));
        expected.append(worker, new TmfVertex(33), EdgeType.RUNNING);

        GraphOps.checkEquality(expected, graph);

        // Test the critical path

        // Build the expected critical path
        expected = new TmfGraph();
        worker = workerMap.get(clientThread);
        assertNotNull(worker);
        expected.add(worker, new TmfVertex(10));
        expected.append(worker, new TmfVertex(13), EdgeType.RUNNING);
        packet1Sent = new TmfVertex(15);
        expected.append(worker, packet1Sent, EdgeType.RUNNING);
        packet2Received = new TmfVertex(70);
        expected.add(worker, packet2Received);
        expected.append(worker, new TmfVertex(75), EdgeType.PREEMPTED);

        worker = workerMap.get(serverThread);
        assertNotNull(worker);
        packet1Received = new TmfVertex(35);
        expected.add(worker, packet1Received);
        expected.append(worker, new TmfVertex(40), EdgeType.PREEMPTED);
        packet2Sent = new TmfVertex(45);
        expected.append(worker, packet2Sent, EdgeType.RUNNING);

        link = packet1Sent.linkVertical(packet1Received);
        link.setType(EdgeType.NETWORK);
        link = packet2Sent.linkVertical(packet2Received);
        link.setType(EdgeType.NETWORK);

        CriticalPathModule critPathModule = new CriticalPathModule(module);
        try {
            critPathModule.setTrace(experiment);
            critPathModule.setParameter(CriticalPathModule.PARAM_WORKER, workerMap.get(clientThread));
            critPathModule.schedule();
            assertTrue(critPathModule.waitForCompletion());

            TmfGraph criticalPath = critPathModule.getCriticalPath();
            assertNotNull(criticalPath);

            GraphOps.checkEquality(expected, criticalPath);
        } finally {
            critPathModule.dispose();
        }
    }

}
