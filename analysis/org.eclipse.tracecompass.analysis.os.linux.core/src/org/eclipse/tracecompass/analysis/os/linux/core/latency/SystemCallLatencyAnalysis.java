/*******************************************************************************
 * Copyright (c) 2015 EfficiOS Inc., Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.latency;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.IAnalysisProgressListener;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.latency.statistics.SystemCallLatencyStateProvider;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.latency.statistics.SystemCallValue;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.statevalue.CustomStateValue;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.ImmutableList;

/**
 * @author Alexandre Montplaisir
 * @since 2.0
 */
public class SystemCallLatencyAnalysis extends TmfStateSystemAnalysisModule implements ISegmentStoreProvider {

    /**
     * The ID of this analysis
     */
    public static final String ID = "org.eclipse.tracecompass.analysis.os.linux.latency.syscall"; //$NON-NLS-1$

    private static final Collection<ISegmentAspect> BASE_ASPECTS =
            checkNotNull(ImmutableList.of(SyscallNameAspect.INSTANCE));

    private static class SyscallNameAspect implements ISegmentAspect {
        public static final ISegmentAspect INSTANCE = new SyscallNameAspect();

        private SyscallNameAspect() { }

        @Override
        public String getHelpText() {
            return checkNotNull(Messages.SegmentAspectHelpText_SystemCall);
        }
        @Override
        public String getName() {
            return checkNotNull(Messages.SegmentAspectName_SystemCall);
        }
        @Override
        public @Nullable Comparator<?> getComparator() {
            return null;
        }
        @Override
        public @Nullable String resolve(ISegment segment) {
            if (segment instanceof SystemCall) {
                return ((SystemCall) segment).getName();
            }
            return EMPTY_STRING;
        }
    }

    @Override
    protected @NonNull ITmfStateProvider createStateProvider() {
        ITmfTrace trace = checkNotNull(getTrace());
        IKernelAnalysisEventLayout layout;

        if (trace instanceof IKernelTrace) {
            layout = ((IKernelTrace) trace).getKernelEventLayout();
        } else {
            /* Fall-back to the base LttngEventLayout */
            layout = IKernelAnalysisEventLayout.DEFAULT_LAYOUT;
        }

        return new SystemCallLatencyStateProvider(trace, layout);
    }

    private final ListenerList<IAnalysisProgressListener> fListeners = new ListenerList<>(ListenerList.IDENTITY);

    @Override
    public void addListener(IAnalysisProgressListener listener) {
        fListeners.add(listener);
    }

    @Override
    public void removeListener(IAnalysisProgressListener listener) {
        fListeners.remove(listener);
    }

    /**
     * Returns all the listeners
     *
     * @return latency listeners
     */
    protected Iterable<IAnalysisProgressListener> getListeners() {
        List<IAnalysisProgressListener> listeners = new ArrayList<>();
        for (Object listener : fListeners.getListeners()) {
            if (listener != null) {
                listeners.add((IAnalysisProgressListener) listener);
            }
        }
        return listeners;
    }

    @Override
    public Iterable<ISegmentAspect> getSegmentAspects() {
        return BASE_ASPECTS;
    }

    @Override
    public @Nullable ISegmentStore<@NonNull ISegment> getSegmentStore() {
        ITmfStateSystem ss = getStateSystem();
        if (ss == null) {
            return null;
        }
        return new StateSystemSegmentStore(ss);
    }

    @Override
    protected boolean executeAnalysis(@Nullable IProgressMonitor monitor) {
        CustomStateValue.registerCustomFactory(SystemCallValue.CUSTOM_TYPE_ID, SystemCallValue.FACTORY);
        boolean success = super.executeAnalysis(monitor);
        if (success) {
            ISegmentStore<ISegment> segStore = getSegmentStore();
            if (segStore != null) {
                for (IAnalysisProgressListener listener : getListeners()) {
                    listener.onComplete(this, segStore);
                }
            }

        }
        return success;
    }


}
