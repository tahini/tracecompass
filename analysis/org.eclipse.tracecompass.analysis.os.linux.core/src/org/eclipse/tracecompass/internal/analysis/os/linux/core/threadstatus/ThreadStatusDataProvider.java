/*******************************************************************************
 * Copyright (c) 2017, 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.os.linux.core.threadstatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelAnalysisModule;
import org.eclipse.tracecompass.analysis.os.linux.core.model.OsStrings;
import org.eclipse.tracecompass.analysis.os.linux.core.model.ProcessStatus;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.Activator;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.Attributes;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.StateValues;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.registry.LinuxStyle;
import org.eclipse.tracecompass.internal.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils.QuarkIterator;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.interval.TmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.IOutputStyleProvider;
import org.eclipse.tracecompass.tmf.core.model.ITimeEventStyleStrings;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.OutputStyleModel;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphStateFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;

/**
 * Thread status data provider, used by the Control Flow view for example.
 *
 * @author Simon Delisle
 */
public class ThreadStatusDataProvider extends AbstractTmfTraceDataProvider implements ITimeGraphDataProvider<@NonNull ThreadEntryModel>, IOutputStyleProvider {

    /**
     * Extension point ID.
     */
    public static final @NonNull String ID = "org.eclipse.tracecompass.internal.analysis.os.linux.core.threadstatus.ThreadStatusDataProvider"; //$NON-NLS-1$

    /**
     * CPU tooltip key.
     */
    public static final @NonNull String CPU = "cpu"; //$NON-NLS-1$

    /**
     * Parameter key used when the thread tree should be filtered by active
     * thread
     */
    public static final @NonNull String ACTIVE_THREAD_FILTER_KEY = "active_thread_filter"; //$NON-NLS-1$

    private static final String WILDCARD = "*"; //$NON-NLS-1$
    private static final Set<Integer> ACTIVE_STATES = ImmutableSet.of(StateValues.PROCESS_STATUS_RUN_USERMODE,
            StateValues.PROCESS_STATUS_RUN_SYSCALL, StateValues.PROCESS_STATUS_INTERRUPTED);
    /**
     * Atomic Long so that every {@link ThreadEntryModel} has a unique ID.
     */
    private static final AtomicLong fAtomicLong = new AtomicLong();

    private static final @NonNull Map<@NonNull String, @NonNull OutputElementStyle> STATE_MAP;
    private static final @NonNull OutputElementStyle DEFAULT_ELEMENT = new OutputElementStyle(null, LinuxStyle.UNKNOWN.toMap());
    private static final int LINK_VALUE = 8;

    static {
        ImmutableMap.Builder<String, OutputElementStyle> builder = new ImmutableMap.Builder<>();
        /*
         * ADD STATE MAPPING HERE
         */
        builder.put(String.valueOf(ProcessStatus.UNKNOWN.getStateValue().unboxInt()), DEFAULT_ELEMENT);
        builder.put(String.valueOf(ProcessStatus.RUN.getStateValue().unboxInt()), new OutputElementStyle(null, LinuxStyle.USERMODE.toMap()));
        builder.put(String.valueOf(ProcessStatus.RUN_SYTEMCALL.getStateValue().unboxInt()), new OutputElementStyle(null, LinuxStyle.SYSCALL.toMap()));
        builder.put(String.valueOf(ProcessStatus.INTERRUPTED.getStateValue().unboxInt()), new OutputElementStyle(null, LinuxStyle.INTERRUPTED.toMap()));
        builder.put(String.valueOf(ProcessStatus.WAIT_BLOCKED.getStateValue().unboxInt()), new OutputElementStyle(null, LinuxStyle.WAIT_BLOCKED.toMap()));
        builder.put(String.valueOf(ProcessStatus.WAIT_CPU.getStateValue().unboxInt()), new OutputElementStyle(null, LinuxStyle.WAIT_FOR_CPU.toMap()));
        builder.put(String.valueOf(ProcessStatus.WAIT_UNKNOWN.getStateValue().unboxInt()), new OutputElementStyle(null, LinuxStyle.WAIT_UNKNOWN.toMap()));

        LinuxStyle link = LinuxStyle.LINK;
        ImmutableMap.Builder<String, Object> linkBuilder = new ImmutableMap.Builder<>();
        linkBuilder.putAll(link.toMap());
        linkBuilder.put(ITimeEventStyleStrings.itemTypeProperty(), ITimeEventStyleStrings.linkType());
        builder.put(String.valueOf(LINK_VALUE), new OutputElementStyle(null, linkBuilder.build()));
        STATE_MAP = builder.build();
    }

    private final KernelAnalysisModule fModule;
    private final long fTraceId = fAtomicLong.getAndIncrement();

    /**
     * Map of quarks by model ID.
     */
    private final Map<Long, Integer> fQuarkMap = new HashMap<>();

    /**
     * Map of {@link ThreadEntryModel}, key is a pair [threadId, cpuId], only used
     * when building
     */
    private final Map<Pair<Integer, Integer>, ThreadEntryModel.Builder> fBuildMap = new HashMap<>();

    /**
     * Last queried time for fetch entries to avoid querying the same range twice.
     * Set to {@link Long#MAX_VALUE} when the build is complete.
     */
    private long fLastEnd = Long.MIN_VALUE;

    /**
     * Cache threadID to a {@link ThreadEntryModel} for faster lookups
     * when building link list
     */
    private final TreeMultimap<Integer, ThreadEntryModel.Builder> fTidToEntry = TreeMultimap.create(Comparator.naturalOrder(),
            Comparator.comparing(ThreadEntryModel.Builder::getStartTime));

    /** Cache for entry metadata */
    private final Map<Long, @NonNull Multimap<@NonNull String, @NonNull Object>> fEntryMetadata = new HashMap<>();

    /**
     * Constructor
     *
     * @param trace
     *            The trace for which this provider will be built.
     * @param module
     *            the {@link KernelAnalysisModule} to access the underlying
     *            {@link ITmfStateSystem}
     *
     */
    public ThreadStatusDataProvider(@NonNull ITmfTrace trace, KernelAnalysisModule module) {
        super(trace);
        fModule = module;
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull TmfTreeModel<@NonNull ThreadEntryModel>> fetchTree(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        if (fLastEnd == Long.MAX_VALUE) {
            return new TmfModelResponse<>(new TmfTreeModel<>(Collections.emptyList(), filter(fTidToEntry, fetchParameters)), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
        }

        fModule.waitForInitialization();
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        /*
         * As we are caching the intermediate result, we only want a single thread to
         * update them.
         */
        synchronized (fBuildMap) {
            boolean complete = ss.waitUntilBuilt(0);
            @NonNull List<@NonNull ThreadEntryModel> list = Collections.emptyList();
            /* Don't query empty state system */
            if (ss.getNbAttributes() > 0 && ss.getStartTime() != Long.MIN_VALUE) {
                long end = ss.getCurrentEndTime();
                fLastEnd = Long.max(fLastEnd, ss.getStartTime());

                TreeMultimap<Integer, ITmfStateInterval> execNamesPPIDs = TreeMultimap.create(Comparator.naturalOrder(),
                        Comparator.comparing(ITmfStateInterval::getStartTime));

                /*
                 * Create a List with the threads' PPID and EXEC_NAME quarks for the 2D query .
                 */
                List<Integer> quarks = new ArrayList<>(ss.getQuarks(Attributes.THREADS, WILDCARD, Attributes.EXEC_NAME));
                quarks.addAll(ss.getQuarks(Attributes.THREADS, WILDCARD, Attributes.PPID));
                try {
                    for (ITmfStateInterval interval : ss.query2D(quarks, Long.min(fLastEnd, end), end)) {
                        if (monitor != null && monitor.isCanceled()) {
                            return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                        }
                        execNamesPPIDs.put(interval.getAttribute(), interval);
                    }
                } catch (TimeRangeException | StateSystemDisposedException e) {
                    return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, e.getClass().getName() + ':' + String.valueOf(e.getMessage()));
                }

                // update the trace Entry.
                fTidToEntry.replaceValues(Integer.MIN_VALUE, Collections.singleton(new ThreadEntryModel.Builder(fTraceId, Collections.singletonList(getTrace().getName()),
                        ss.getStartTime(), end, Integer.MIN_VALUE, Integer.MIN_VALUE)));

                for (Integer threadQuark : ss.getQuarks(Attributes.THREADS, WILDCARD)) {
                    String threadAttributeName = ss.getAttributeName(threadQuark);
                    Pair<Integer, Integer> entryKey = Attributes.parseThreadAttributeName(threadAttributeName);
                    int threadId = entryKey.getFirst();
                    if (threadId < 0) {
                        // ignore the 'unknown' (-1) thread
                        continue;
                    }

                    int execNameQuark = ss.optQuarkRelative(threadQuark, Attributes.EXEC_NAME);
                    int ppidQuark = ss.optQuarkRelative(threadQuark, Attributes.PPID);
                    NavigableSet<ITmfStateInterval> ppidIntervals = execNamesPPIDs.get(ppidQuark);
                    for (ITmfStateInterval execNameInterval : execNamesPPIDs.get(execNameQuark)) {
                        if (monitor != null && monitor.isCanceled()) {
                            return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                        }
                        updateEntry(threadQuark, entryKey, ppidIntervals, execNameInterval);
                    }
                }

                fLastEnd = end;

                list = filter(fTidToEntry, fetchParameters);
            }

            for (ThreadEntryModel model : list) {
                fEntryMetadata.put(model.getId(), model.getMetadata());
            }

            if (complete) {
                fBuildMap.clear();
                fLastEnd = Long.MAX_VALUE;
                return new TmfModelResponse<>(new TmfTreeModel<>(Collections.emptyList(), list), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
            }

            return new TmfModelResponse<>(new TmfTreeModel<>(Collections.emptyList(), list), ITmfResponse.Status.RUNNING, CommonStatusMessage.RUNNING);
        }
    }

    private void updateEntry(Integer threadQuark, Pair<Integer, Integer> entryKey,
            NavigableSet<ITmfStateInterval> ppidIntervals, ITmfStateInterval execNameInterval) {
        Object value = execNameInterval.getValue();
        if (value == null) {
            fBuildMap.remove(entryKey);
            return;
        }

        ThreadEntryModel.Builder entry = fBuildMap.get(entryKey);
        long startTime = execNameInterval.getStartTime();
        long endTime = execNameInterval.getEndTime() + 1;
        String execName = String.valueOf(value);
        int threadId = entryKey.getFirst();
        int ppid = getPpid(ppidIntervals, endTime);

        if (entry == null) {
            long id = fAtomicLong.getAndIncrement();
            entry = new ThreadEntryModel.Builder(id, Collections.singletonList(execName), startTime, endTime, threadId, ppid);
            fQuarkMap.put(id, threadQuark);
        } else {
            /*
             * Update the name of the entry to the latest execName and the parent thread id
             * to the latest ppid. We must make a copy as the Models are immutable.
             */
            entry.setEndTime(endTime);
            entry.setPpid(ppid);
            entry.setName(Collections.singletonList(execName));
        }
        fBuildMap.put(entryKey, entry);
        fTidToEntry.put(threadId, entry);
    }

    /**
     * Find the parent PID for a given time from a thread's sorted PPID intervals.
     *
     * @param ppidIterator
     *            a navigable set sorted by increasing start time
     * @param t
     *            the time stamp at which we want to know the PPID
     * @return the entry's PPID or -1 if we could not find it.
     */
    private static int getPpid(NavigableSet<ITmfStateInterval> ppidIterator, long t) {
        ITmfStateInterval ppidInterval = ppidIterator.lower(new TmfStateInterval(t, t + 1, 0, 0));
        if (ppidInterval != null) {
            Object o = ppidInterval.getValue();
            if (o instanceof Integer) {
                return (Integer) o;
            }
        }
        return -1;
    }

    /**
     * Filter the threads from a {@link TreeMultimap} according to if they are
     * active or not
     *
     * @param tidToEntry
     *            Threads to filter
     * @param filter
     *            time range to query
     * @return a list of the active threads
     */
    private @NonNull List<@NonNull ThreadEntryModel> filter(TreeMultimap<Integer, ThreadEntryModel.Builder> tidToEntry, @NonNull Map<@NonNull String, @NonNull Object> parameters) {
        TimeQueryFilter filter = FetchParametersUtils.createTimeQuery(parameters);
        if (filter == null) {
            return Collections.emptyList();
        }

        // avoid putting everything as a child of the swapper thread.
        Boolean isActiveFilter = DataProviderParameterUtils.extractBoolean(parameters, ACTIVE_THREAD_FILTER_KEY);
        if (isActiveFilter == null || !isActiveFilter) {
            ImmutableList.Builder<ThreadEntryModel> builder = ImmutableList.builder();
            for (ThreadEntryModel.Builder entryBuilder : tidToEntry.values()) {
                builder.add(build(entryBuilder));
            }
            return builder.build();
        }
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return Collections.emptyList();
        }

        long start = Long.max(filter.getStart(), ss.getStartTime());
        long end = Long.min(filter.getEnd(), ss.getCurrentEndTime());
        if (start > end) {
            return Collections.emptyList();
        }
        List<@NonNull Long> selectedItems = DataProviderParameterUtils.extractSelectedItems(parameters);
        if (selectedItems != null) {
            Set<Long> cpus = Sets.newHashSet(selectedItems);
            List<@NonNull Integer> quarks = ss.getQuarks(Attributes.THREADS, WILDCARD, Attributes.CURRENT_CPU_RQ);
            Set<ThreadEntryModel> models = new HashSet<>();
            Map<Integer, Integer> rqToPidCache = new HashMap<>();
            try {
                for (ITmfStateInterval interval : ss.query2D(quarks, Long.max(ss.getStartTime(), start), end)) {
                    Object o = interval.getValue();
                    if (o instanceof Number && cpus.contains(((Number) o).longValue())) {
                        int attribute = interval.getAttribute();

                        try {
                            // Get the name of the thread
                            int nameQuark = ss.getQuarkRelative(ss.getParentAttributeQuark(attribute), Attributes.EXEC_NAME);
                            Iterable<@NonNull ITmfStateInterval> names2d = ss.query2D(Collections.singleton(nameQuark), interval.getStartTime(), interval.getEndTime());
                            Iterable<@NonNull String> names = Iterables.transform(names2d, intervalName -> String.valueOf(intervalName.getValue()));

                            int tid = rqToPidCache.computeIfAbsent(attribute, a -> Attributes.parseThreadAttributeName(ss.getAttributeName(ss.getParentAttributeQuark(a))).getFirst());
                            //Skip Idle (thread 0)
                            if (tid == 0) {
                                continue;
                            }
                            for (ThreadEntryModel.Builder model : tidToEntry.get(tid)) {
                                if (interval.getStartTime() <= model.getEndTime() &&
                                        model.getStartTime() <= interval.getEndTime()) {
                                    ThreadEntryModel build = build(model);
                                    if (!Iterables.any(names, name -> name.equals(build.getName()))) {
                                        continue;
                                    }
                                    models.add(build);
                                }
                            }
                        } catch (AttributeNotFoundException e) {
                            Activator.getDefault().logWarning("Unable to get the quark for the attribute name", e); //$NON-NLS-1$
                        }
                    }
                }
            } catch (IndexOutOfBoundsException | TimeRangeException e) {
                Activator.getDefault().logError("Invalid query parameters", e); //$NON-NLS-1$
            } catch (StateSystemDisposedException e) {
                return Collections.emptyList();
            }
            return Lists.newArrayList(models);
        }
        ImmutableList.Builder<ThreadEntryModel> builder = ImmutableList.builder();
        for (ThreadEntryModel.Builder thread : tidToEntry.values()) {
            Integer statusQuark = fQuarkMap.get(thread.getId());
            if (statusQuark == null) {
                continue;
            }
            QuarkIterator iterator = new QuarkIterator(ss, statusQuark, start, end);
            Iterator<Object> values = Iterators.transform(iterator, ITmfStateInterval::getValue);
            if (Iterators.any(values, ACTIVE_STATES::contains)) {
                builder.add(build(thread));
            }
        }
        return builder.build();
    }

    private ThreadEntryModel build(ThreadEntryModel.Builder entryBuilder) {
        if (entryBuilder.getId() == fTraceId) {
            return entryBuilder.build(-1);
        }
        long parentId = entryBuilder.getPpid() > 0 ? findEntry(entryBuilder.getPpid(), entryBuilder.getEndTime()) : fTraceId;
        return entryBuilder.build(parentId);
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull TimeGraphModel> fetchRowModel(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, IProgressMonitor monitor) {
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        TreeMultimap<Integer, ITmfStateInterval> intervals = TreeMultimap.create(Comparator.naturalOrder(),
                Comparator.comparing(ITmfStateInterval::getStartTime));
        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(fetchParameters);
        Map<Long, Integer> selectedIdsToQuarks = getSelectedIdsToQuarks(filter);
        Collection<Integer> stateAndSyscallQuarks = addSyscall(selectedIdsToQuarks.values(), ss);
        Collection<Long> times = getTimes(ss, filter);
        try {
            /* Do the actual query */
            for (ITmfStateInterval interval : ss.query2D(stateAndSyscallQuarks, times)) {
                if (monitor != null && monitor.isCanceled()) {
                    return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                }
                intervals.put(interval.getAttribute(), interval);
            }
        } catch (TimeRangeException | StateSystemDisposedException e) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, String.valueOf(e.getMessage()));
        }

        Map<@NonNull Integer, @NonNull Predicate< @NonNull Multimap<@NonNull String, @NonNull Object>>> predicates = new HashMap<>();
        Multimap<@NonNull Integer, @NonNull String> regexesMap = DataProviderParameterUtils.extractRegexFilter(fetchParameters);
        if (regexesMap != null) {
            predicates.putAll(computeRegexPredicate(regexesMap));
        }

        @NonNull List<@NonNull ITimeGraphRowModel> rows = new ArrayList<>();
        for (Entry<Long, Integer> entry : selectedIdsToQuarks.entrySet()) {
            int quark = entry.getValue();
            NavigableSet<ITmfStateInterval> states = intervals.get(quark);
            NavigableSet<ITmfStateInterval> syscalls = intervals.get(ss.optQuarkRelative(quark, Attributes.SYSTEM_CALL));

            if (monitor != null && monitor.isCanceled()) {
                return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
            }
            List<ITimeGraphState> eventList = new ArrayList<>();
            states.forEach(i -> {
                ITimeGraphState timegraphState = createTimeGraphState(i, syscalls);
                Long key = Objects.requireNonNull(entry.getKey());
                applyFilterAndAddState(eventList, timegraphState, key, predicates, monitor);
            });
            rows.add(new TimeGraphRowModel(entry.getKey(), eventList));
        }
        return new TmfModelResponse<>(new TimeGraphModel(rows), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private Map<Long, Integer> getSelectedIdsToQuarks(SelectionTimeQueryFilter filter) {
        Map<Long, Integer> map = new LinkedHashMap<>();
        for (Long id : filter.getSelectedItems()) {
            Integer quark = fQuarkMap.get(id);
            if (quark != null) {
                map.put(id, quark);
            }
        }
        return map;
    }

    private static Collection<Integer> addSyscall(Collection<Integer> quarks, ITmfStateSystem ss) {
        Collection<Integer> copy = new HashSet<>(quarks);
        for (Integer quark : quarks) {
            int syscallQuark = ss.optQuarkRelative(quark, Attributes.SYSTEM_CALL);
            if (syscallQuark != ITmfStateSystem.INVALID_ATTRIBUTE) {
                copy.add(syscallQuark);
            }
        }
        return copy;
    }

    /**
     * Filter and deduplicate the time stamps for the statesystem
     *
     * @param ss
     *            this provider's {@link ITmfStateSystem}
     * @param filter
     *            the query object
     * @return a Set of filtered timestamps that intersect the state system's time
     *         range
     */
    private static @NonNull Collection<@NonNull Long> getTimes(ITmfStateSystem ss, TimeQueryFilter filter) {
        long start = ss.getStartTime();
        // use a HashSet to deduplicate time stamps
        Collection<@NonNull Long> times = new HashSet<>();
        for (long t : filter.getTimesRequested()) {
            if (t >= start) {
                times.add(t);
            }
        }
        return times;
    }

    private static @NonNull ITimeGraphState createTimeGraphState(ITmfStateInterval interval, NavigableSet<ITmfStateInterval> syscalls) {
        long startTime = interval.getStartTime();
        long duration = interval.getEndTime() - startTime + 1;
        Object status = interval.getValue();
        TimeGraphState state = null;
        if (status instanceof Integer) {
            int s = (int) status;
            if (s == StateValues.PROCESS_STATUS_RUN_SYSCALL) {
                // intervals are sorted by start time
                ITmfStateInterval syscall = syscalls.floor(new TmfStateInterval(startTime, startTime + 1, 0, 0));

                if (syscall != null) {
                    Object value = syscall.getValue();
                    if (value instanceof String) {
                        state = new TimeGraphState(startTime, duration, s, String.valueOf(value));
                    }
                }
            }
            if (state == null) {
                state = new TimeGraphState(startTime, duration, s);
            }
            state.setStyle(STATE_MAP.getOrDefault(String.valueOf(s), DEFAULT_ELEMENT));
            return state;
        }
        return new TimeGraphState(startTime, duration, Integer.MIN_VALUE);
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> fetchArrows(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, IProgressMonitor monitor) {
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }
        List<@NonNull ITimeGraphArrow> linkList = new ArrayList<>();
        /**
         * MultiMap of the current thread intervals, grouped by CPU, by increasing start
         * time.
         */
        TreeMultimap<Integer, ITmfStateInterval> currentThreadIntervalsMap = TreeMultimap.create(
                Comparator.naturalOrder(),
                Comparator.comparing(ITmfStateInterval::getStartTime));
        List<Integer> quarks = ss.getQuarks(Attributes.CPUS, WILDCARD, Attributes.CURRENT_THREAD);
        TimeQueryFilter filter = FetchParametersUtils.createTimeQuery(fetchParameters);
        Collection<Long> times = getTimes(ss, filter);
        try {
            /* Do the actual query */
            for (ITmfStateInterval interval : ss.query2D(quarks, times)) {
                if (monitor != null && monitor.isCanceled()) {
                    return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                }
                currentThreadIntervalsMap.put(interval.getAttribute(), interval);
            }

            /* Get the arrows. */
            for (Collection<ITmfStateInterval> currentThreadIntervals : currentThreadIntervalsMap.asMap().values()) {
                if (monitor != null && monitor.isCanceled()) {
                    return new TmfModelResponse<>(null, ITmfResponse.Status.CANCELLED, CommonStatusMessage.TASK_CANCELLED);
                }
                linkList.addAll(createCpuArrows(ss, (NavigableSet<ITmfStateInterval>) currentThreadIntervals));
            }
        } catch (TimeRangeException | StateSystemDisposedException e) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, String.valueOf(e.getMessage()));
        }
        return new TmfModelResponse<>(linkList, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    /**
     * Create the list of arrows to follow the current thread on a CPU
     *
     * @param trace
     *            trace displayed in the view
     * @param entryList
     *            entry list for this trace
     * @param intervals
     *            sorted collection of the current thread intervals for a CPU
     * @return the list of arrows to follow the current thread on a CPU
     * @throws StateSystemDisposedException
     *             If the query is sent after the state system has been disposed
     */
    private List<@NonNull TimeGraphArrow> createCpuArrows(ITmfStateSystem ss, NavigableSet<ITmfStateInterval> intervals)
            throws StateSystemDisposedException {
        if (intervals.isEmpty()) {
            return Collections.emptyList();
        }
        /*
         * Add the previous interval if it is the first query iteration and the first
         * interval has currentThread=0. Add the following interval if the last interval
         * has currentThread=0. These are diagonal arrows crossing the query iteration
         * range.
         */
        ITmfStateInterval first = intervals.first();
        long start = first.getStartTime() - 1;
        if (start >= ss.getStartTime() && Objects.equals(first.getValue(), 0)) {
            intervals.add(ss.querySingleState(start, first.getAttribute()));
        }
        ITmfStateInterval last = intervals.last();
        long end = last.getEndTime() + 1;
        if (end <= ss.getCurrentEndTime() && Objects.equals(last.getValue(), 0)) {
            intervals.add(ss.querySingleState(end, last.getAttribute()));
        }

        List<@NonNull TimeGraphArrow> linkList = new ArrayList<>();
        long prevEnd = 0;
        long lastEnd = 0;
        long prevEntry = -1;
        for (ITmfStateInterval currentThreadInterval : intervals) {
            long time = currentThreadInterval.getStartTime();
            if (time != lastEnd) {
                /*
                 * Don't create links where there are gaps in intervals due to the resolution
                 */
                prevEntry = -1;
                prevEnd = 0;
            }
            Integer tid = (Integer) currentThreadInterval.getValue();
            lastEnd = currentThreadInterval.getEndTime() + 1;
            long nextEntry = -1;
            if (tid != null && tid > 0) {
                nextEntry = findEntry(tid, time);
                if (prevEntry >= 0 && nextEntry >= 0) {
                    linkList.add(new TimeGraphArrow(prevEntry, nextEntry, prevEnd, time - prevEnd, LINK_VALUE));
                }
                prevEntry = nextEntry;
                prevEnd = lastEnd;
            }
        }
        return linkList;
    }

    /**
     * Get the thread entry id for a given TID and time
     *
     * @param tid
     *            queried TID
     * @param time
     *            queried time stamp
     * @return the id for the desired thread or -1 if it does not exist
     */
    private long findEntry(int tid, long time) {
        /*
         * FIXME TreeMultimap values are Navigable Sets sorted by start time, find the
         * values using floor and the relevant anonymous class if ever the iteration
         * below slows down.
         */
        ThreadEntryModel.Builder entry = Iterables.find(fTidToEntry.get(tid),
                cfe -> cfe.getStartTime() <= time && time <= cfe.getEndTime(), null);
        return entry != null ? entry.getId() : fTraceId;
    }

    @Override
    public @NonNull String getId() {
        return ID;
    }

    @Override
    public @NonNull TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> fetchTooltip(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.STATE_SYSTEM_FAILED);
        }

        boolean completed = ss.waitUntilBuilt(0);
        ITmfResponse.Status status = completed ? ITmfResponse.Status.COMPLETED : ITmfResponse.Status.RUNNING;
        String statusMessage = completed ? CommonStatusMessage.COMPLETED : CommonStatusMessage.RUNNING;

        // TODO server: Parameters validation should be handle separately. It
        // can be either in the data provider itself or before calling it. It
        // will avoid the creation of filters and the content of the map can be
        // use directly.
        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(fetchParameters);
        if (filter == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.INCORRECT_QUERY_PARAMETERS);
        }
        Integer quark = fQuarkMap.get(filter.getSelectedItems().iterator().next());
        if (quark == null) {
            return new TmfModelResponse<>(null, status, statusMessage);
        }

        long start = filter.getStart();
        int currentCpuRqQuark = ss.optQuarkRelative(quark, Attributes.CURRENT_CPU_RQ);
        if (currentCpuRqQuark == ITmfStateSystem.INVALID_ATTRIBUTE || start < ss.getStartTime() || start > ss.getCurrentEndTime()) {
            return new TmfModelResponse<>(null, status, statusMessage);
        }

        try {
            ITmfStateInterval interval = ss.querySingleState(start, currentCpuRqQuark);
            Object value = interval.getValue();
            if (value instanceof Integer) {
                return new TmfModelResponse<>(ImmutableMap.of(OsStrings.cpu(), String.valueOf(value)), status, statusMessage);
            }
        } catch (StateSystemDisposedException e) {
            /* Ignored */
        }
        return new TmfModelResponse<>(null, status, statusMessage);
    }

    @Deprecated
    @Override
    public TmfModelResponse<List<ThreadEntryModel>> fetchTree(@NonNull TimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        @NonNull Map<@NonNull String, @NonNull Object> parameters = FetchParametersUtils.timeQueryToMap(filter);
        TmfModelResponse<@NonNull TmfTreeModel<@NonNull ThreadEntryModel>> response = fetchTree(parameters, monitor);
        TmfTreeModel<@NonNull ThreadEntryModel> model = response.getModel();
        List<ThreadEntryModel> treeModel = null;
        if (model != null) {
            treeModel = model.getEntries();
        }
        return new TmfModelResponse<>(treeModel, response.getStatus(), response.getStatusMessage());
    }

    @Deprecated
    @Override
    public @NonNull TmfModelResponse<@NonNull List<@NonNull ITimeGraphRowModel>> fetchRowModel(@NonNull SelectionTimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        @NonNull Map<@NonNull String, @NonNull Object> parameters = FetchParametersUtils.selectionTimeQueryToMap(filter);
        TmfModelResponse<@NonNull TimeGraphModel> response = fetchRowModel(parameters, monitor);
        TimeGraphModel model = response.getModel();
        List<@NonNull ITimeGraphRowModel> rows = null;
        if (model != null) {
            rows = model.getRows();
        }
        return new TmfModelResponse<>(rows, response.getStatus(), response.getStatusMessage());
    }

    @Deprecated
    @Override
    public @NonNull TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> fetchArrows(@NonNull TimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        @NonNull Map<@NonNull String, @NonNull Object> parameters = FetchParametersUtils.timeQueryToMap(filter);
        return fetchArrows(parameters, monitor);
    }

    @Deprecated
    @Override
    public @NonNull TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> fetchTooltip(@NonNull SelectionTimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        @NonNull Map<@NonNull String, @NonNull Object> parameters = FetchParametersUtils.selectionTimeQueryToMap(filter);
        return fetchTooltip(parameters, monitor);
    }

    @Override
    public @NonNull Multimap<@NonNull String, @NonNull Object> getFilterData(long entryId, long time, @Nullable IProgressMonitor monitor) {
        Multimap<@NonNull String, @NonNull Object> data = ITimeGraphStateFilter.mergeMultimaps(ITimeGraphDataProvider.super.getFilterData(entryId, time, monitor),
                fEntryMetadata.getOrDefault(entryId, ImmutableMultimap.of()));
        SelectionTimeQueryFilter filter = new SelectionTimeQueryFilter(Collections.singletonList(time), Collections.singleton(Objects.requireNonNull(entryId)));
        TmfModelResponse<Map<String, String>> response = fetchTooltip(filter, monitor);
        Map<@NonNull String, @NonNull String> model = response.getModel();
        if (model != null) {
            for (Entry<String, String> entry : model.entrySet()) {
                data.put(entry.getKey(), entry.getValue());
            }
        }
        return data;
    }

    @Override
    public TmfModelResponse<OutputStyleModel> fetchStyle(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(new OutputStyleModel(STATE_MAP), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    /**
     * Get the style map for this data provider
     *
     * @return The style map
     */
    public static @NonNull Map<@NonNull String, @NonNull OutputElementStyle> getStyleMap() {
        return STATE_MAP;
    }

}
