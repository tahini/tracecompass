/*******************************************************************************
 * Copyright (c) 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.graph.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.tracecompass.analysis.os.linux.core.execution.graph.OsInterruptContext;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.execution.graph.OsExecutionGraphProvider;
import org.eclipse.tracecompass.analysis.os.linux.core.execution.graph.OsSystemModel;
import org.eclipse.tracecompass.analysis.os.linux.core.model.HostThread;
import org.eclipse.tracecompass.analysis.os.linux.core.execution.graph.OsExecutionGraphProvider.Context;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Event Handler to handle the interrupt context stack of the model
 *
 * @author Francis Giraldeau
 * @author Geneviève Bastien
 */
public class EventContextHandler extends BaseHandler {

    private final Map<String, Consumer<ITmfEvent>> fHandlers = new HashMap<>();
    private final Set<IKernelAnalysisEventLayout> fLayouts = new HashSet<>();

    private final Consumer<ITmfEvent> fDefault = event -> wakingInContext(event);
    private final Consumer<ITmfEvent> fSoftIrqEntryHandler = event -> pushInterruptContext(event, Context.SOFTIRQ);
    private final Consumer<ITmfEvent> fSoftIrqExitHandler = event -> popInterruptContext(event, Context.SOFTIRQ);
    private final Consumer<ITmfEvent> fHRTimerExpireEntry = event -> pushInterruptContext(event, Context.HRTIMER);
    private final Consumer<ITmfEvent> fHRTimerExpireExit = event -> popInterruptContext(event, Context.HRTIMER);
    private final Consumer<ITmfEvent> fIrqHandlerEntry = event -> pushInterruptContext(event, Context.IRQ);
    private final Consumer<ITmfEvent> fIrqHandlerExit = event -> handleIrqExit(event);
    private final Consumer<ITmfEvent> fIpiEntry = event -> pushInterruptContext(event, Context.IPI);
    private final Consumer<ITmfEvent> fIpiExit = event -> popInterruptContext(event, Context.IPI);
    private final Consumer<ITmfEvent> fSchedSwitch = event -> handleSchedSwitch(event);

    private static class TraceCpu {

        private final ITmfTrace fTrace;
        private final Integer fCpu;

        public TraceCpu(ITmfTrace trace, Integer cpu) {
            fTrace = trace;
            fCpu = cpu;
        }

        @Override
        public int hashCode() {
            return Objects.hash(fTrace, fCpu);
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof TraceCpu)) {
                return false;
            }
            TraceCpu other = (TraceCpu) obj;
            return Objects.equals(this.fTrace, other.fTrace) && fCpu == other.fCpu;
        }

        public static @Nullable TraceCpu create(ITmfEvent event) {
            Integer cpu = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);
            if (cpu == null) {
                return null;
            }
            return new TraceCpu(event.getTrace(), cpu);
        }
    }

    private final List<TraceCpu> fPendingContexts = new ArrayList<>();

    /**
     * Constructor
     *
     * @param provider
     *            The parent graph provider
     * @param priority
     *            The priority of this handler. It will determine when it will be
     *            executed
     */
    public EventContextHandler(OsExecutionGraphProvider provider, int priority) {
        super(provider, priority);
    }

    @Override
    public void handleEvent(ITmfEvent event) {
        IKernelAnalysisEventLayout eventLayout = getProvider().getEventLayout(event.getTrace());
        if (!fLayouts.contains(eventLayout)) {
            populateHandlerMap(eventLayout);
            fLayouts.add(eventLayout);
        }
        fHandlers.getOrDefault(event.getName(), fDefault).accept(event);
    }

    private void populateHandlerMap(IKernelAnalysisEventLayout eventLayout) {
        fHandlers.put(eventLayout.eventSoftIrqEntry(), fSoftIrqEntryHandler);
        fHandlers.put(eventLayout.eventSoftIrqExit(), fSoftIrqExitHandler);
        fHandlers.put(eventLayout.eventHRTimerExpireEntry(), fHRTimerExpireEntry);
        fHandlers.put(eventLayout.eventHRTimerExpireExit(), fHRTimerExpireExit);
        fHandlers.put(eventLayout.eventIrqHandlerEntry(), fIrqHandlerEntry);
        fHandlers.put(eventLayout.eventIrqHandlerExit(), fIrqHandlerExit);
        for (String ipiName : eventLayout.getIPIIrqVectorsEntries()) {
            fHandlers.put(ipiName, fIpiEntry);
        }
        for (String ipiName : eventLayout.getIPIIrqVectorsEntries()) {
            fHandlers.put(ipiName, fIpiExit);
        }
        fHandlers.put(eventLayout.eventSchedSwitch(), fSchedSwitch);
    }

    private void pushInterruptContext(ITmfEvent event, Context ctx) {
        Integer cpu = NonNullUtils.checkNotNull(TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event));
        OsSystemModel system = getProvider().getSystem();

        OsInterruptContext interruptCtx = new OsInterruptContext(event, ctx);

        system.pushContextStack(event.getTrace().getHostId(), cpu, interruptCtx);
    }

    private void popInterruptContext(ITmfEvent event, Context ctx) {
        Integer cpu = NonNullUtils.checkNotNull(TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event));
        OsSystemModel system = getProvider().getSystem();

        /* TODO: add a warning bookmark if the interrupt context is not coherent */
        OsInterruptContext interruptCtx = system.peekContextStack(event.getTrace().getHostId(), cpu);
        if (interruptCtx.getContext() == ctx) {
            system.popContextStack(event.getTrace().getHostId(), cpu);
        }
    }

    private void handleIrqExit(ITmfEvent event) {
        // Pop the IRQ first
        popInterruptContext(event, Context.IRQ);
        // This is an irq exit, check the return value to see if there is a pending waking
        ITmfEventField content = event.getContent();
        if (content != null) {
            Integer fieldValue = content.getFieldValue(Integer.class, "ret"); //$NON-NLS-1$
            if (fieldValue != null && fieldValue == 2) {
                // Stay in context and note a pending waking on that CPU
                TraceCpu hostCpu = TraceCpu.create(event);
                if (hostCpu != null) {
                    fPendingContexts.add(hostCpu);
                    pushInterruptContext(event, Context.IRQ_EXTENDED);
                }
            }
        }
    }

    private void wakingInContext(ITmfEvent event) {
        if (fPendingContexts.isEmpty()) {
            // No pending contexts, do nothing
            return;
        }
        TraceCpu hostCpu = TraceCpu.create(event);
        if (hostCpu == null || !fPendingContexts.contains(hostCpu)) {
            // There is no trace CPU, or this CPU is not waiting for action
            return;
        }
        // If this event is a waking event, stay in context for this event, otherwise, pop the context
        IKernelAnalysisEventLayout eventLayout = getProvider().getEventLayout(event.getTrace());
        if (!event.getName().equals(eventLayout.eventSchedProcessWaking())) {
            popInterruptContext(event, Context.IRQ_EXTENDED);
        }
    }

    private void handleSchedSwitch(ITmfEvent event) {
        IKernelAnalysisEventLayout eventLayout = getProvider().getEventLayout(event.getTrace());
        OsSystemModel system = getProvider().getSystem();
        ITmfEventField content = event.getContent();

        Integer next = content.getFieldValue(Integer.class, eventLayout.fieldNextTid());
        Integer prev = content.getFieldValue(Integer.class, eventLayout.fieldPrevTid());
        if (next != null) {
            HostThread ht = new HostThread(event.getTrace().getHostId(), next);
            if (system.isIrqWorker(ht)) {
                pushInterruptContext(event, Context.THREADED_IRQ);
            }
        }
        if (prev != null) {
            HostThread ht = new HostThread(event.getTrace().getHostId(), prev);
            if (system.isIrqWorker(ht)) {
                popInterruptContext(event, Context.THREADED_IRQ);
            }
        }

    }
}
