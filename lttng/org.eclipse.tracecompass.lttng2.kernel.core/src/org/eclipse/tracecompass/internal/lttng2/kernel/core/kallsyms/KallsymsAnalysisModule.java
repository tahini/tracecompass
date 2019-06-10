/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.kernel.core.kallsyms;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * An analysis that tracks the kernel symbols statedumped by LTTng, and also
 * tracks modifications through modules load/unload.
 *
 * @author Geneviève Bastien
 */
public class KallsymsAnalysisModule extends TmfStateSystemAnalysisModule {

    /**
     * ID of the analysis
     */
    public static final String ID = "org.eclipse.tracecompass.lttng2.kernel.core.kallsyms"; //$NON-NLS-1$

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            throw new NullPointerException("The trace should not be null"); //$NON-NLS-1$
        }
        return new KallsymsStateProvider(trace);
    }

}
