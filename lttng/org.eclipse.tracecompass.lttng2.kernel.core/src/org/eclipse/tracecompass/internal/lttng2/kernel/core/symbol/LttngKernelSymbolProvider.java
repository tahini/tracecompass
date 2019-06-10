/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.kernel.core.symbol;

import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.log.TraceCompassLog;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.ScopeLog;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.kallsyms.KallsymsAnalysisModule;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.core.symbols.TmfResolvedSymbol;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 *
 *
 * @author Geneviève Bastien
 */
public class LttngKernelSymbolProvider implements ISymbolProvider {

    private static final Logger LOGGER = TraceCompassLog.getLogger(LttngKernelSymbolProvider.class);

    private final ITmfTrace fTrace;
    private final KallsymsAnalysisModule fModule;

    private final NavigableMap<Long, TmfResolvedSymbol> fSymbolMapping;

    private static Comparator<@NonNull Long> MAP_COMPARATOR = new Comparator<@NonNull Long>() {

        @Override
        public int compare(Long o1, Long o2) {
            return Long.compareUnsigned(o1, o2);
        }

    };

    /**
     * Constructor
     *
     * @param module
     *            The kallsyms module
     * @param trace
     *            The trace this symbol provider provides
     */
    public LttngKernelSymbolProvider(KallsymsAnalysisModule module, ITmfTrace trace) {
        fTrace = trace;
        module.schedule();
        module.waitForInitialization();
        fModule = module;
        fSymbolMapping = new TreeMap<>(MAP_COMPARATOR);
    }

    @Override
    public @NonNull ITmfTrace getTrace() {
        return fTrace;
    }

    @Override
    public void loadConfiguration(@Nullable IProgressMonitor monitor) {


        ITmfStateSystem stateSystem = fModule.getStateSystem();
        if (stateSystem == null) {
            return;
        }
        boolean done = stateSystem.waitUntilBuilt(0L);

        // Load the symbols done so far
        loadCurrentSymbols(stateSystem);
        // If the state system is finished building, just return
        if (done) {
            return;
        }

        // Let's load the symbols as the analysis is built
        Job job = new Job("loading kernel symbols") {
            @Override
            protected @Nullable IStatus run(final @Nullable IProgressMonitor monitor2) {
                try (ScopeLog jobLog = new ScopeLog(LOGGER, Level.FINE, "LttngKernelSymbolProvider:loading")) { //$NON-NLS-1$
                    IProgressMonitor mon = monitor;
                    if (mon == null) {
                        mon = new NullProgressMonitor();
                    }

                    boolean finished = false;
                    do {
                        finished = stateSystem.waitUntilBuilt(100L);
                        if (mon.isCanceled() || (monitor2 != null && monitor2.isCanceled())) {
                            return Status.CANCEL_STATUS;
                        }
                        loadCurrentSymbols(stateSystem);
                    } while (!finished);

                    return Status.OK_STATUS;

                }
            }

        };
        job.schedule();
    }

    private void loadCurrentSymbols(ITmfStateSystem stateSystem) {
        try {
            long time = stateSystem.getCurrentEndTime();
            List<ITmfStateInterval> states = stateSystem.queryFullState(time);

            for (Integer hostQuark : stateSystem.getSubAttributes(ITmfStateSystem.ROOT_ATTRIBUTE, false)) {
                for (Integer moduleQuark : stateSystem.getSubAttributes(hostQuark, false)) {
                    String moduleName = stateSystem.getAttributeName(moduleQuark);
                    for (Integer symbolQuark : stateSystem.getSubAttributes(moduleQuark, false)) {
                        String addrString = stateSystem.getAttributeName(symbolQuark);
                        long address = Long.parseUnsignedLong(addrString, 16);
                        ITmfStateInterval interval = states.get(symbolQuark);
                        String symbol = (String) interval.getValue();
                        fSymbolMapping.put(address, new TmfResolvedSymbol(address, symbol + ' ' + '(' + moduleName + ')'));
                    }
                }
            }
        } catch (StateSystemDisposedException e) {
            // StateSystem was disposed, nothing to do
        }
    }

    @Override
    public @Nullable TmfResolvedSymbol getSymbol(long address) {
        Entry<Long, TmfResolvedSymbol> floorEntry = fSymbolMapping.floorEntry(address);
        if (floorEntry == null) {
            return null;
        }
        // See if the symbol returned is the last symbol. In this case, don't
        // use the floor unless it hits the exact address
        TmfResolvedSymbol symbol = Objects.requireNonNull(floorEntry.getValue());
        long floorValue = symbol.getBaseAddress();
        return ((floorValue == fSymbolMapping.lastKey())
                && floorValue != address) ? null : symbol;
    }

    @Override
    public @Nullable TmfResolvedSymbol getSymbol(int pid, long timestamp, long address) {
        return getSymbol(address);
    }

}
