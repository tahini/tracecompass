/*******************************************************************************
 * Copyright (c) 2018, 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.views.timegraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.IFilterProperty;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.BaseDataProviderTimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ILinkEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NamedTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry.Sampling;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeLinkEvent;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

/**
 * {@link AbstractTimeGraphView} for views with data providers.
 *
 * @author Loic Prieur-Drevon
 * @since 4.0
 */
public class BaseDataProviderTimeGraphView extends AbstractTimeGraphView {

    /**
     * Timeout between updates in the build thread in ms
     */
    protected static final long BUILD_UPDATE_TIMEOUT = 500;

    /**
     * Table of (data provider, model id) to time graph entry. The table should be
     * filled by {@link #buildEntryList} and is read by {@link #zoomEntries} and
     * {@link #getLinkList}.
     */
    protected final Table<ITimeGraphDataProvider<? extends @NonNull TimeGraphEntryModel>, Long, @NonNull TimeGraphEntry> fEntries = HashBasedTable.create();

    private final String fProviderId;

    private final Map<String, TimeGraphEntry> fAggregateGroup = new HashMap<>();

    /**
     * Constructs a time graph view that contains a time graph viewer.
     *
     * By default, the view uses a single default column in the name space that
     * shows the time graph entry name. To use multiple columns and/or customized
     * label texts, the subclass constructor must call
     * {@link #setTreeColumns(String[])} and/or
     * {@link #setTreeLabelProvider(TreeLabelProvider)}.
     *
     * @param id
     *            The id of the view
     * @param pres
     *            The presentation provider
     * @param providerId
     *            the ID for the {@link ITimeGraphDataProvider} to use to populate
     *            this view
     */
    public BaseDataProviderTimeGraphView(String id, TimeGraphPresentationProvider pres, String providerId) {
        super(id, pres);
        fProviderId = providerId;
    }

    /**
     * Get a data provider ID
     *
     * @return the data provider ID
     * @since 3.3
     */
    protected String getProviderId() {
        return fProviderId;
    }

    @Override
    protected void buildEntryList(@NonNull ITmfTrace trace, @NonNull ITmfTrace parentTrace, @NonNull IProgressMonitor monitor) {
        ITimeGraphDataProvider<@NonNull TimeGraphEntryModel> dataProvider = DataProviderManager
                .getInstance().getDataProvider(trace, getProviderId(), ITimeGraphDataProvider.class);
        if (dataProvider == null) {
            return;
        }
        boolean complete = false;
        while (!complete && !monitor.isCanceled()) {
            TmfModelResponse<TmfTreeModel<@NonNull TimeGraphEntryModel>> response = dataProvider.fetchTree(FetchParametersUtils.timeQueryToMap(new TimeQueryFilter(0, Long.MAX_VALUE, 2)), monitor);
            if (response.getStatus() == ITmfResponse.Status.FAILED) {
                Activator.getDefault().logError(getClass().getSimpleName() + " Data Provider failed: " + response.getStatusMessage()); //$NON-NLS-1$
                return;
            } else if (response.getStatus() == ITmfResponse.Status.CANCELLED) {
                return;
            }
            complete = response.getStatus() == ITmfResponse.Status.COMPLETED;

            TmfTreeModel<@NonNull TimeGraphEntryModel> model = response.getModel();
            if (model != null) {
                synchronized (fEntries) {
                    /*
                     * The provider may send entries unordered and parents may
                     * not exist when child is constructor, we'll re-unite
                     * families at the end
                     */
                    List<TimeGraphEntry> orphaned = new ArrayList<>();
                    for (TimeGraphEntryModel entry : model.getEntries()) {
                        TimeGraphEntry uiEntry = fEntries.get(dataProvider, entry.getId());
                        Map<@NonNull String, @NonNull Object> entryData = entry.getData();
                        String entryGroup = null;
                        if (entryData != null) {
                            Object group = entryData.get("group");
                            if (group instanceof String) {
                                entryGroup = (String) group;
                            }
                        }
                        if (entry.getParentId() != -1) {
                            if (uiEntry == null) {
                                uiEntry = new TimeGraphEntry(entry);
                                TimeGraphEntry parent = fEntries.get(dataProvider, entry.getParentId());
                                if (parent != null) {
                                    parent.addChild(uiEntry);
                                } else {
                                    orphaned.add(uiEntry);
                                }
                                fEntries.put(dataProvider, entry.getId(), uiEntry);
                            } else {
                                uiEntry.updateModel(entry);
                            }
                        } else {
                            setStartTime(Long.min(getStartTime(), entry.getStartTime()));
                            setEndTime(Long.max(getEndTime(), entry.getEndTime() + 1));

                            if (uiEntry != null) {
                                uiEntry.updateModel(entry);
                            } else {
                                // Do not assume that parentless entries are
                                // trace entries
                                if (entryGroup != null) {
                                    TimeGraphEntry aggregateEntry = fAggregateGroup.get(entryGroup);
                                    if (aggregateEntry instanceof AggregateTraceEntry) {
                                        ((AggregateTraceEntry) aggregateEntry).addTraceEntry(entry, trace, dataProvider);
                                        uiEntry = aggregateEntry;
                                        // continue;
                                    } else {
                                        uiEntry = new AggregateTraceEntry(entry, trace, dataProvider, entryGroup);
                                        fAggregateGroup.put(entryGroup, uiEntry);
                                    }
                                } else {
                                    uiEntry = new TraceEntry(entry, trace, dataProvider);
                                }
                                fEntries.put(dataProvider, entry.getId(), uiEntry);
                                addToEntryList(parentTrace, Collections.singletonList(uiEntry));
                            }
                        }
                    }
                    // Find missing parents
                    for (TimeGraphEntry orphanedEntry : orphaned) {
                        TimeGraphEntry parent = fEntries.get(dataProvider, orphanedEntry.getEntryModel().getParentId());
                        if (parent != null) {
                            parent.addChild(orphanedEntry);
                        }
                    }
                }

                long start = getStartTime();
                long end = getEndTime();
                final long resolution = Long.max(1, (end - start) / getDisplayWidth());
                @NonNull Iterable<@NonNull TimeGraphEntry> entries;
                synchronized (fEntries) {
                    entries = ImmutableList.copyOf(fEntries.values());
                }
                zoomEntries(entries, start, end, resolution, monitor);
            }

            if (monitor.isCanceled()) {
                return;
            }

            if (parentTrace.equals(getTrace())) {
                synchingToTime(getTimeGraphViewer().getSelectionBegin());
                refresh();
            }
            monitor.worked(1);

            if (!complete && !monitor.isCanceled()) {
                try {
                    Thread.sleep(BUILD_UPDATE_TIMEOUT);
                } catch (InterruptedException e) {
                    Activator.getDefault().logError("Failed to wait for data provider", e); //$NON-NLS-1$
                }
            }
        }
    }

    /**
     * @since 5.1
     */
    protected static class AggregateTraceEntry extends TraceEntry {
        private final @NonNull Map<@NonNull Long, @NonNull ITmfTrace> fTraces = new HashMap<>();
        private final @NonNull Map<@NonNull Long, @NonNull ITimeGraphDataProvider<? extends TimeGraphEntryModel>> fProviders = new HashMap<>();
        private final @NonNull Set<@NonNull TimeGraphEntryModel> fModels = new HashSet<>();
        private final @NonNull String fAggregateName;

        /**
         * Constructor
         *
         * @param model
         *            trace level model
         * @param trace
         *            The trace corresponding to this trace entry.
         * @param provider
         *            reference to the provider for this trace and view
         * @param name
         *            The visible name for this {@link AggregateTraceEntry}
         */
        public AggregateTraceEntry(@NonNull TimeGraphEntryModel model, @NonNull ITmfTrace trace,
                @NonNull ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider, @NonNull String name) {
            super(model, trace, provider);
            fModels.add(model);
            fTraces.put(model.getId(), trace);
            fProviders.put(model.getId(), provider);
            fAggregateName = name;
        }

        @Override
        public String getName() {
            return fAggregateName;
        }

        @Override
        public long getStartTime() {
            long startTime = Long.MAX_VALUE;
            for (TimeGraphEntryModel model : fModels) {
                startTime = Math.min(startTime, model.getStartTime());
            }
            return startTime;
        }

        @Override
        public long getEndTime() {
            long endTime = Long.MIN_VALUE;
            for (TimeGraphEntryModel model : fModels) {
                endTime = Math.max(endTime, model.getEndTime());
            }
            return endTime;
        }

        /**
         * Add a trace entry to this {@link AggregateTraceEntry}
         *
         * @param model
         *            Trace entry model
         * @param trace
         *            Trace associated to this {@link AggregateTraceEntry}
         * @param provider
         *            {@link ITimeGraphDataProvider} associated to this
         *            {@link AggregateTraceEntry}
         */
        public void addTraceEntry(@NonNull TimeGraphEntryModel model, @NonNull ITmfTrace trace,
                @NonNull ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider) {
            fTraces.put(model.getId(), trace);
            fProviders.put(model.getId(), provider);
            fModels.add(model);
        }

        /**
         * Get for this trace entry's trace
         *
         * @param id
         *            Parent entry ID
         *
         * @return the trace for this trace entry and its children
         */
        public @NonNull ITmfTrace getTrace(long id) {
            ITmfTrace trace = fTraces.get(id);
            if (trace != null) {
                return trace;
            }
            throw new IllegalStateException(this + " should have a trace associated to this id: " + id); //$NON-NLS-1$
        }

        /**
         * Get the data provider for this {@link AggregateTraceEntry}
         *
         * @param id
         *            Parent entry ID
         *
         * @return this entry's {@link ITimeGraphDataProvider}
         */
        public @NonNull ITimeGraphDataProvider<? extends TimeGraphEntryModel> getProvider(long id) {
            ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider = fProviders.get(id);
            if(provider != null) {
                return provider;
            }
            throw new IllegalStateException(this + " should have a provider associated to this id: " + id); //$NON-NLS-1$
        }

        /**
         * Get all the data providers associated to this
         * {@link AggregateTraceEntry}
         *
         * @return Collection of {@link ITimeGraphDataProvider}
         */
        public @NonNull Collection<@NonNull ITimeGraphDataProvider<? extends TimeGraphEntryModel>> getProviders() {
            return fProviders.values();
        }

    }

    /**
     * Class to represent a parent entry, for which we keep a link to the trace
     * and data provider to avoid having to do so for its children. This type of
     * entry is otherwise not different from any other time graph entry.
     *
     * @author Loic Prieur-Drevon
     * @since 3.3
     */
    protected static class TraceEntry extends TimeGraphEntry {
        private final @NonNull ITmfTrace fTrace;
        private final @NonNull ITimeGraphDataProvider<? extends TimeGraphEntryModel> fProvider;

        /**
         * Constructor
         *
         * @param model
         *            trace level model
         * @param trace
         *            The trace corresponding to this trace entry.
         * @param provider
         *            reference to the provider for this trace and view
         */
        public TraceEntry(@NonNull TimeGraphEntryModel model, @NonNull ITmfTrace trace,
                @NonNull ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider) {
            super(model);
            fTrace = trace;
            fProvider = provider;
        }

        /**
         * Getter for this trace entry's trace
         *
         * @return the trace for this trace entry and its children
         */
        public @NonNull ITmfTrace getTrace() {
            return fTrace;
        }

        /**
         * Getter for the data provider for this {@link TraceEntry}
         *
         * @return this entry's {@link ITimeGraphDataProvider}
         */
        public @NonNull ITimeGraphDataProvider<? extends TimeGraphEntryModel> getProvider() {
            return fProvider;
        }
    }

    /**
     * Get the {@link ITmfTrace} from a {@link TimeGraphEntry}'s parent.
     *
     * @param entry
     *            queried {@link TimeGraphEntry}.
     * @return the {@link ITmfTrace}
     * @since 3.3
     */
    public static @NonNull ITmfTrace getTrace(TimeGraphEntry entry) {
        TraceEntry traceEntry = getTraceEntry(entry);
        if (traceEntry instanceof AggregateTraceEntry) {
            return ((AggregateTraceEntry) traceEntry).getTrace(getTraceEntryId(entry));
        }
        return traceEntry.getTrace();
    }

    /**
     * Get the {@link ITimeGraphDataProvider} from a {@link TimeGraphEntry}'s
     * parent.
     *
     * @param entry
     *            queried {@link TimeGraphEntry}.
     * @return the {@link ITimeGraphDataProvider}
     * @since 3.3
     */
    public static ITimeGraphDataProvider<? extends TimeGraphEntryModel> getProvider(TimeGraphEntry entry) {
        TraceEntry traceEntry = getTraceEntry(entry);
        if (traceEntry instanceof AggregateTraceEntry) {
            return ((AggregateTraceEntry) traceEntry).getProvider(getTraceEntryId(entry));
        }
        return traceEntry.getProvider();
    }

    private static long getTraceEntryId(TimeGraphEntry entry) {
        long id = entry.getEntryModel().getId();
        TimeGraphEntry parent = entry;
        while (parent != null) {
            if (parent instanceof TraceEntry) {
                return id;
            }
            id = parent.getEntryModel().getParentId();
            parent = parent.getParent();
        }
        throw new IllegalStateException(entry + " should have a TraceEntry parent ID"); //$NON-NLS-1$
    }

    private static TraceEntry getTraceEntry(TimeGraphEntry entry) {
        ITimeGraphEntry parent = entry;
        while (parent != null) {
            if (parent instanceof TraceEntry) {
                return ((TraceEntry) parent);
            }
            parent = parent.getParent();
        }
        throw new IllegalStateException(entry + " should have a TraceEntry parent"); //$NON-NLS-1$
    }

    @Override
    protected void zoomEntries(@NonNull Iterable<@NonNull TimeGraphEntry> entries, long zoomStartTime, long zoomEndTime, long resolution, @NonNull IProgressMonitor monitor) {
        if (resolution < 0) {
            // StateSystemUtils.getTimes would throw an illegal argument exception.
            return;
        }

        long start = Long.min(zoomStartTime, zoomEndTime);
        long end = Long.max(zoomStartTime, zoomEndTime);
        List<@NonNull Long> times = StateSystemUtils.getTimes(start, end, resolution);
        Sampling sampling = new Sampling(start, end, resolution);
        Multimap<ITimeGraphDataProvider<? extends TimeGraphEntryModel>, Long> providersToModelIds = filterGroupEntries(entries, zoomStartTime, zoomEndTime);
        SubMonitor subMonitor = SubMonitor.convert(monitor, getClass().getSimpleName() + "#zoomEntries", providersToModelIds.size()); //$NON-NLS-1$

        for (Entry<ITimeGraphDataProvider<? extends TimeGraphEntryModel>, Collection<Long>> entry : providersToModelIds.asMap().entrySet()) {
            ITimeGraphDataProvider<? extends TimeGraphEntryModel> dataProvider = entry.getKey();
            SelectionTimeQueryFilter filter = new SelectionTimeQueryFilter(times, entry.getValue());
            Map<@NonNull String, @NonNull Object> parameters = FetchParametersUtils.selectionTimeQueryToMap(filter);
            Multimap<@NonNull Integer, @NonNull String> regexesMap = getRegexes();
            if (!regexesMap.isEmpty()) {
                parameters.put(DataProviderParameterUtils.REGEX_MAP_FILTERS_KEY, regexesMap.asMap());
            }
            TmfModelResponse<TimeGraphModel> response = dataProvider.fetchRowModel(parameters, monitor);

            TimeGraphModel model = response.getModel();
            if (model != null) {
                zoomEntries(fEntries.row(dataProvider), model.getRows(), response.getStatus() == ITmfResponse.Status.COMPLETED, sampling);
            }
            subMonitor.worked(1);
        }
    }

    /**
     * Filter the entries to return only the Non Null {@link TimeGraphEntry} which
     * intersect the time range.
     *
     * @param visible
     *            the input list of visible entries
     * @param zoomStartTime
     *            the leftmost time bound of the view
     * @param zoomEndTime
     *            the rightmost time bound of the view
     * @return A Multimap of data providers to their visible entries' model IDs.
     */
    private static Multimap<ITimeGraphDataProvider<? extends TimeGraphEntryModel>, Long> filterGroupEntries(Iterable<TimeGraphEntry> visible,
            long zoomStartTime, long zoomEndTime) {
        Multimap<ITimeGraphDataProvider<? extends TimeGraphEntryModel>, Long> providersToModelIds = HashMultimap.create();
        for (TimeGraphEntry entry : visible) {
            if (zoomStartTime <= entry.getEndTime() && zoomEndTime >= entry.getStartTime() && entry.hasTimeEvents()) {
                ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider = getProvider(entry);
                if (provider != null) {
                    providersToModelIds.put(provider, entry.getModel().getId());
                }
            }
        }
        return providersToModelIds;
    }

    private void zoomEntries(Map<Long, TimeGraphEntry> map, List<ITimeGraphRowModel> model, boolean completed, Sampling sampling) {
        boolean isZoomThread = Thread.currentThread() instanceof ZoomThread;
        for (ITimeGraphRowModel rowModel : model) {
            TimeGraphEntry entry;
            synchronized (fEntries) {
                entry = map.get(rowModel.getEntryID());
            }

            if (entry != null) {
                List<ITimeEvent> events = createTimeEvents(entry, rowModel.getStates());
                if (isZoomThread) {
                    applyResults(() -> {
                        entry.setZoomedEventList(events);
                        if (completed) {
                            entry.setSampling(sampling);
                        }
                    });
                } else {
                    entry.setEventList(events);
                }
            }
        }
    }

    /**
     * Create {@link ITimeEvent}s for an entry from the list of
     * {@link ITimeGraphState}s, filling in the gaps.
     *
     * @param entry
     *            the {@link TimeGraphEntry} on which we are working
     * @param values
     *            the list of {@link ITimeGraphState}s from the
     *            {@link ITimeGraphDataProvider}.
     * @return a contiguous List of {@link ITimeEvent}s
     */
    protected List<ITimeEvent> createTimeEvents(TimeGraphEntry entry, List<ITimeGraphState> values) {
        List<ITimeEvent> events = new ArrayList<>(values.size());
        ITimeEvent prev = null;
        for (ITimeGraphState state : values) {
            ITimeEvent event = createTimeEvent(entry, state);
            if (prev != null) {
                long prevEnd = prev.getTime() + prev.getDuration();
                if (prevEnd < event.getTime() && (getTimeEventFilterDialog() == null || !getTimeEventFilterDialog().hasActiveSavedFilters())) {
                    // fill in the gap.
                    TimeEvent timeEvent = new TimeEvent(entry, prevEnd, event.getTime() - prevEnd);
                    if (getTimeEventFilterDialog() != null && getTimeEventFilterDialog().isFilterActive()) {
                        timeEvent.setProperty(IFilterProperty.DIMMED, true);
                    }
                    events.add(timeEvent);
                }
            }
            prev = event;
            events.add(event);
        }
        return events;
    }

    /**
     * Create a {@link TimeEvent} for a {@link TimeGraphEntry} and a
     * {@link TimeGraphState}
     *
     * @param entry
     *            {@link TimeGraphEntry} for which we create a state
     * @param state
     *            {@link ITimeGraphState} from the data provider
     * @return a new {@link TimeEvent} for these arguments
     *
     * @since 3.3
     */
    protected TimeEvent createTimeEvent(TimeGraphEntry entry, ITimeGraphState state) {
        String label = state.getLabel();
        if (state.getValue() == Integer.MIN_VALUE && label == null) {
            return new NullTimeEvent(entry, state.getStartTime(), state.getDuration());
        }
        if (label != null) {
            return new NamedTimeEvent(state, entry, label);
//            return new NamedTimeEvent(entry, state.getStartTime(), state.getDuration(), state.getValue(), label, state.getActiveProperties());
        }
        return new TimeEvent(state, entry);
//        return new TimeEvent(entry, state.getStartTime(), state.getDuration(), state.getValue(), state.getActiveProperties());
    }

    @Override
    protected List<@NonNull ILinkEvent> getLinkList(long zoomStartTime, long zoomEndTime, long resolution,
            @NonNull IProgressMonitor monitor) {
        List<@NonNull TimeGraphEntry> traceEntries = getEntryList(getTrace());
        if (traceEntries == null) {
            return Collections.emptyList();
        }
        List<@NonNull ILinkEvent> linkList = new ArrayList<>();
        List<@NonNull Long> times = StateSystemUtils.getTimes(zoomStartTime, zoomEndTime, resolution);
        TimeQueryFilter queryFilter = new TimeQueryFilter(times);

        for (TraceEntry entry : Iterables.filter(traceEntries, TraceEntry.class)) {
            ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider = entry.getProvider();
            TmfModelResponse<List<ITimeGraphArrow>> response = provider.fetchArrows(FetchParametersUtils.timeQueryToMap(queryFilter), monitor);
            List<ITimeGraphArrow> model = response.getModel();

            if (model != null) {
                for (ITimeGraphArrow arrow : model) {
                    ITimeGraphEntry prevEntry;
                    ITimeGraphEntry nextEntry;
                    synchronized (fEntries) {
                        prevEntry = fEntries.get(provider, arrow.getSourceId());
                        nextEntry = fEntries.get(provider, arrow.getDestinationId());
                    }
                    if (prevEntry != null && nextEntry != null) {
                        linkList.add(new TimeLinkEvent(prevEntry, nextEntry, arrow.getStartTime(), arrow.getDuration(), arrow.getValue()));
                    }
                }
            }
        }
        return linkList;
    }

    @Override
    protected void resetView(ITmfTrace viewTrace) {
        List<@NonNull TimeGraphEntry> entryList = getEntryList(viewTrace);
        super.resetView(viewTrace);
        // Remove the entries for this trace
        if (entryList != null) {
            synchronized (fEntries) {
                for (TimeGraphEntry entry : entryList) {
                    if (entry instanceof AggregateTraceEntry) {
                        Collection<@NonNull ITimeGraphDataProvider<? extends TimeGraphEntryModel>> providers = ((AggregateTraceEntry) entry).getProviders();
                        for (ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider : providers) {
                            fEntries.row(provider).clear();
                        }
                    } else if (entry instanceof TraceEntry) {
                        fEntries.row(((TraceEntry) entry).getProvider()).clear();
                    }
                }
            }
            fAggregateGroup.clear();
        }
    }

    @Override
    protected void loadingTrace(@NonNull ITmfTrace trace) {
        ITimeGraphDataProvider<@NonNull TimeGraphEntryModel> dataProvider = DataProviderManager
                .getInstance().getDataProvider(trace, getProviderId(), ITimeGraphDataProvider.class);
        ITimeGraphPresentationProvider presentationProvider = getPresentationProvider();
        if (dataProvider != null && presentationProvider instanceof BaseDataProviderTimeGraphPresentationProvider) {
            ((BaseDataProviderTimeGraphPresentationProvider) presentationProvider).setDataProvider(dataProvider);
        }
    }



}
