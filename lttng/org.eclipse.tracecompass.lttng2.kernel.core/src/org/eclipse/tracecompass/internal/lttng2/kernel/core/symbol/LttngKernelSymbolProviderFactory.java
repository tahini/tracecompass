/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.kernel.core.symbol;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.kallsyms.KallsymsAnalysisModule;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProviderFactory;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * @author gbastien
 *
 */
public class LttngKernelSymbolProviderFactory implements ISymbolProviderFactory {

    @Override
    public @Nullable ISymbolProvider createProvider(@NonNull ITmfTrace trace) {
        /*
         * This applies only to UST traces that fulfill the DebugInfo analysis
         * requirements.
         */
        KallsymsAnalysisModule module = TmfTraceUtils.getAnalysisModuleOfClass(trace,
                KallsymsAnalysisModule.class, KallsymsAnalysisModule.ID);

        if (module != null) {
            return new LttngKernelSymbolProvider(module, trace);
        }
        return null;
    }

}
