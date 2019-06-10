/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.eclipse.tracecompass.lttng2.kernel.core.tests.symbols;

import static org.junit.Assert.assertNotNull;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.symbol.LttngKernelSymbolProvider;
import org.eclipse.tracecompass.lttng2.kernel.core.trace.LttngKernelTrace;
import org.eclipse.tracecompass.lttng2.lttng.kernel.core.tests.shared.LttngKernelTestTraceUtils;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.SymbolProviderManager;
import org.junit.Test;

/**
 * Test the LTTng kernel's kallsyms symbol provider with an actual trace
 *
 * @author Geneviève Bastien
 */
public class LttngKernelKallsymsTest {

    /**
     * Test that the symbol provider is present for the trace
     */
    @Test
    public void testSymbolProvider() {
        LttngKernelTrace trace = LttngKernelTestTraceUtils.getTrace(CtfTestTrace.KERNEL);
        try {
            // Open the trace so the analysis are filled
            trace.traceOpened(new TmfTraceOpenedSignal(this, trace, null));

            // Get the symbol providers and make sure one of the has is a
            // kallsyms provider
            Collection<@NonNull ISymbolProvider> symbolProviders = SymbolProviderManager.getInstance().getSymbolProviders(trace);
            ISymbolProvider kallsymsProvider = null;
            for (ISymbolProvider provider : symbolProviders) {
                if (provider instanceof LttngKernelSymbolProvider) {
                    kallsymsProvider = provider;
                }
            }
            assertNotNull(kallsymsProvider);
        } finally {
            LttngKernelTestTraceUtils.dispose(CtfTestTrace.KERNEL);
        }
    }

}
