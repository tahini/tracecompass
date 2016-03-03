/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.os.linux.core.latency.statistics;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis.KernelTidAspect;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfRecyclableAttribute;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.util.Pair;

public class SystemCallLatencyStateProvider extends AbstractTmfStateProvider {

    private final IKernelAnalysisEventLayout fLayout;
    private final Map<Integer, Pair<Integer, SystemCallValue>> fOngoingSystemCalls = new HashMap<>();

    private @Nullable TmfRecyclableAttribute fSysCallsAttrib = null;

    public SystemCallLatencyStateProvider(@NonNull ITmfTrace trace, IKernelAnalysisEventLayout layout) {
        super(trace, "syscall latency");
        fLayout = layout;
    }

    private static final int VERSION = 1;

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new SystemCallLatencyStateProvider(getTrace(), fLayout);
    }

    @Override
    protected void eventHandle(@NonNull ITmfEvent event) {
        IKernelAnalysisEventLayout layout = fLayout;
        final String eventName = event.getType().getName();

        final ITmfStateSystemBuilder ss = NonNullUtils.checkNotNull(getStateSystemBuilder());

        if (eventName.startsWith(layout.eventSyscallEntryPrefix()) ||
                eventName.startsWith(layout.eventCompatSyscallEntryPrefix())) {
            /* This is a system call entry event */

            TmfRecyclableAttribute attrib = fSysCallsAttrib;
            if (attrib == null) {
                attrib = new TmfRecyclableAttribute(ss, ITmfStateSystem.ROOT_ATTRIBUTE);
                fSysCallsAttrib = attrib;
            }
            Integer tid = KernelTidAspect.INSTANCE.resolve(event);
            if (tid == null) {
                // no information on this event/trace ?
                return;
            }

            /* Record the event's data into the intial system call info */
            // String syscallName = fLayout.getSyscallNameFromEvent(event);
            long startTime = event.getTimestamp().getValue();
            String syscallName = eventName.substring(layout.eventSyscallEntryPrefix().length());

            Map<String, String> args = event.getContent().getFieldNames().stream()
                .collect(Collectors.toMap(Function.identity(),
                        input -> checkNotNull(event.getContent().getField(input).getValue().toString())));

            SystemCallValue newSysCall = new SystemCallValue(syscallName, args);

            Integer availableQuark = attrib.getAvailableQuark();
            try {
                ss.modifyAttribute(startTime, newSysCall, availableQuark);
            } catch (StateValueTypeException | AttributeNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            fOngoingSystemCalls.put(tid, new Pair<>(availableQuark, newSysCall));


        } else if (eventName.startsWith(layout.eventSyscallExitPrefix())) {
            /* This is a system call exit event */

            Integer tid = KernelTidAspect.INSTANCE.resolve(event);
            if (tid == null) {
                return;
            }

            Pair<Integer, SystemCallValue> syscall = fOngoingSystemCalls.remove(tid);
            if (syscall == null) {
                /*
                 * We have not seen the entry event corresponding to this
                 * exit (lost event, or before start of trace).
                 */
                return;
            }
            SystemCallValue info = syscall.getSecond();

            TmfRecyclableAttribute attrib = fSysCallsAttrib;
            if (attrib == null) {
                attrib = new TmfRecyclableAttribute(ss, ITmfStateSystem.ROOT_ATTRIBUTE);
                fSysCallsAttrib = attrib;
            }

            long endTime = event.getTimestamp().getValue();
            int ret = ((Long) event.getContent().getField("ret").getValue()).intValue(); //$NON-NLS-1$
            info.setReturnValue(ret);
            attrib.recycleQuark(syscall.getFirst(), endTime);

        }
    }

}
