/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.inputoutput;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis.KernelAnalysisModule;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;

/**
 * State System Module for Input Output traces
 *
 * @author Houssem Daoud
 * @since 2.0
 */
public class InputOutputAnalysisModule extends TmfStateSystemAnalysisModule {

    /** The ID of this analysis module */
    public static final String ID = "org.eclipse.tracecompass.analysis.os.linux.inputoutput"; //$NON-NLS-1$

    @Override
    protected ITmfStateProvider createStateProvider() {
        ITmfTrace trace = checkNotNull(getTrace());
        IKernelAnalysisEventLayout layout;

        if (trace instanceof IKernelTrace) {
            layout = ((IKernelTrace) trace).getKernelEventLayout();
        } else {
            /* Fall-back to the base LttngEventLayout */
            layout = IKernelAnalysisEventLayout.DEFAULT_LAYOUT;
        }

        return new InputOutputStateProvider(trace, layout);
    }

    @Override
    protected StateSystemBackendType getBackendType() {
        return StateSystemBackendType.FULL;
    }

    @Override
    protected Iterable<IAnalysisModule> getDependentAnalyses() {
        Set<IAnalysisModule> modules = new HashSet<>();

        ITmfTrace trace = getTrace();
        if (trace == null) {
            throw new IllegalStateException();
        }
        /*
         * This analysis depends on the LTTng kernel analysis, so it's added to
         * dependent modules.
         */
        Iterable<KernelAnalysisModule> kernelModules = TmfTraceUtils.getAnalysisModulesOfClass(trace, KernelAnalysisModule.class);
        for (KernelAnalysisModule kernelModule : kernelModules) {
            /* Only add the first one we find, if there is one */
            modules.add(kernelModule);
            break;
        }
        return modules;
    }

    /**
     * @param start start interval
     * @param end end interval
     * @return table of latencies
     */
    public ArrayList<Long> getLatencyTables(long start, long end) {
        ArrayList<Long> map = new ArrayList<>();
        ITmfTrace trace = getTrace();
        ITmfStateSystem ss = getStateSystem();
        if (trace == null || ss == null) {
            return map;
        }

        try {
            int driverQueueQuark;
            driverQueueQuark = ss.getQuarkAbsolute(Attributes.DISKS,"sdb",Attributes.DRIVER_QUEUE); //$NON-NLS-1$
            List<Integer> slotsQuark = ss.getSubAttributes(driverQueueQuark, false);
            for (Integer slotQuark : slotsQuark){
                Integer currentRequestQuark = ss.getQuarkRelative(slotQuark, Attributes.CURRENT_REQUEST);
                List<ITmfStateInterval> intervals = StateSystemUtils.queryHistoryRange(ss, currentRequestQuark, start, end);
                for (ITmfStateInterval currentInterval : intervals){
                    if(!currentInterval.getStateValue().isNull()){
                        Long latency = currentInterval.getEndTime() - currentInterval.getStartTime();
                        //Long sector = currentInterval.getStateValue().unboxLong();
                        map.add(latency/1000000);
                    }
                }
            }
        } catch (AttributeNotFoundException | StateSystemDisposedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return map;
        }
        return map;
    }
}
