/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.lttng2.kernel.core.tests.symbols;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.tests.stubs.trace.TmfXmlKernelTraceStub;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.kallsyms.KallsymsAnalysisModule;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.symbol.LttngKernelSymbolProvider;
import org.eclipse.tracecompass.lttng2.kernel.core.tests.Activator;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.symbols.TmfResolvedSymbol;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.junit.Test;

/**
 * Test the {@link LttngKernelSymbolProvider} and {@link KallsymsAnalysisModule}
 * classes
 *
 * @author Geneviève Bastien
 */
public class LttngKallsymsSymbolTest {

    private static final String TRACE_FILE = "testfiles/symbols/kernel_symbols.xml";
    private static final IProgressMonitor NULL_MONITOR = new NullProgressMonitor();

    /**
     * Setup the trace for the tests
     *
     * @return The trace with its graph module executed
     */
    private @NonNull ITmfTrace setUpTrace() {
        TmfXmlKernelTraceStub trace = new TmfXmlKernelTraceStub();
        IPath filePath = Activator.getAbsoluteFilePath(TRACE_FILE);
        IStatus status = trace.validate(null, filePath.toOSString());
        if (!status.isOK()) {
            trace.dispose();
            fail(status.getException().getMessage());
        }
        try {
            trace.initTrace(null, filePath.toOSString(), TmfEvent.class);
        } catch (TmfTraceException e) {
            trace.dispose();
            fail(e.getMessage());
        }
        trace.traceOpened(new TmfTraceOpenedSignal(this, trace, null));
        return trace;
    }

    /**
     * @throws TmfAnalysisException
     *             Exception thrown the test
     *
     */
    @Test
    public void testSymbolProvider() throws TmfAnalysisException {
        ITmfTrace trace = setUpTrace();
        KallsymsAnalysisModule module = new KallsymsAnalysisModule();
        try {

            module.setTrace(trace);
            module.schedule();
            assertTrue(module.waitForCompletion());

            LttngKernelSymbolProvider symbolProvider = new LttngKernelSymbolProvider(module, trace);

            int pid = 5;
            long largeAddress = Long.parseUnsignedLong("ffffffffc0158210", 16);
            symbolProvider.loadConfiguration(NULL_MONITOR);

            // Test some symbols right on the value with direct symbol access
            assertEquals("start_init ([KERNEL])", getSymbolName(symbolProvider.getSymbol(123)));
            assertEquals("lower_value ([KERNEL])", getSymbolName(symbolProvider.getSymbol(100)));
            assertEquals("big_unsigned ([KERNEL])", getSymbolName(symbolProvider.getSymbol(largeAddress)));
            assertNull(symbolProvider.getSymbol(50));

            // Test some symbols between values with direct symbol access
            assertEquals("in_between_with_module (myModule2)", getSymbolName(symbolProvider.getSymbol(1300)));
            assertEquals("lower_value ([KERNEL])", getSymbolName(symbolProvider.getSymbol(105)));
            // last symbol expected to be null
            assertEquals(null, getSymbolName(symbolProvider.getSymbol(largeAddress + 10)));

            // Test some symbols right on the value with time and pid
            assertEquals("start_init ([KERNEL])", getSymbolName(symbolProvider.getSymbol(pid, 5L, 123)));
            assertEquals("lower_value ([KERNEL])", getSymbolName(symbolProvider.getSymbol(pid, 5L, 100)));
            assertEquals("big_unsigned ([KERNEL])", getSymbolName(symbolProvider.getSymbol(pid, 5L, largeAddress)));

            // Test some symbols between values with time and pid
            assertEquals("in_between_with_module (myModule2)", getSymbolName(symbolProvider.getSymbol(pid, 5L, 1300)));
            assertEquals("lower_value ([KERNEL])", getSymbolName(symbolProvider.getSymbol(pid, 5L, 105)));
            // last symbol expected to be null
            assertEquals("big_unsigned ([KERNEL])", getSymbolName(symbolProvider.getSymbol(pid, 5L, largeAddress + 10)));
        } finally {
            trace.dispose();
            module.dispose();
        }
    }

    private static @Nullable String getSymbolName(@Nullable TmfResolvedSymbol symbol) {
        if (symbol == null) {
            return null;
        }
        return symbol.getSymbolName();
    }

}
