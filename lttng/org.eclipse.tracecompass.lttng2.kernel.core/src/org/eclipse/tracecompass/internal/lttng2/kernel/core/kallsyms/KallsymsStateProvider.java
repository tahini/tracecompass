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
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Kernel symbol state provider. The symbols are saved in a state system where
 * those coming from the statedump are assigned to the whole trace and those
 * coming from modules coming/going are valid only from/to the time of the event.
 *
 * The structure of the state system is as follows:
 *
 * <pre>
 * Host ID
 *   |-> module name
 *   |  |-> symbol address => name
 *   |-> [kernel] when no module specified
 *      |-> symbol address => name
 * </pre>
 *
 * @author Geneviève Bastien
 */
public class KallsymsStateProvider extends AbstractTmfStateProvider {

    private static final String ID = "org.eclipse.tracecompass.lttng2.kernel.kallsyms.provider"; //$NON-NLS-1$
    private static final int VERSION = 1;

    private static final String STATEDUMP_KALLSYMS_EVENT = "lttng_kallsyms_kernel_symbol"; //$NON-NLS-1$

    /**
     * Constructor
     *
     * @param trace
     *            The trace this provider is for
     */
    public KallsymsStateProvider(ITmfTrace trace) {
        super(trace, ID);
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new KallsymsStateProvider(getTrace());
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        String name = event.getName();
        if (!name.equals(STATEDUMP_KALLSYMS_EVENT)) {
            return;
        }
        ITmfStateSystemBuilder ssb = this.getStateSystemBuilder();
        if (ssb == null) {
            throw new NullPointerException("State system should not be null at this point");
        }

        Long address = event.getContent().getFieldValue(Long.class, "addr");
        String symbol = event.getContent().getFieldValue(String.class, "symbol");
        if (address == null || symbol == null) {
            // Data not available
            return;
        }
        String module = event.getContent().getFieldValue(String.class, "module");
        module = (module == null || module.isEmpty()) ? "[KERNEL]" : module;

        int moduleQuark = ssb.getQuarkAbsoluteAndAdd(event.getTrace().getHostId(), module, Long.toUnsignedString(address, 16));
        ssb.updateOngoingState(symbol, moduleQuark);

    }

}
