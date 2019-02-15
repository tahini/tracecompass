/*******************************************************************************
 * Copyright (c) 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.timegraph.dataprovider;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.tmf.core.model.xy.AbstractTreeCommonXDataProvider;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.IFilterProperty;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ISeriesModel;
import org.eclipse.tracecompass.tmf.core.model.xy.ITmfXyModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.AbstractTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NamedTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry.Sampling;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeLineEvent;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

/**
 *
 *
 * @author Matthew Khouzam
 * @since 4.3
 */
public class DataProviderBaseView extends AbstractTimeGraphView {

    private final List<AbstractTreeCommonXDataProvider> fXProviders = new ArrayList<>();
    private final List<ITimeGraphDataProvider<@NonNull TimeGraphEntryModel>> fTProviders = new ArrayList<>();
    private final Table<Object, Long, @NonNull TimeGraphEntry> fEntries = HashBasedTable.create();
    private final Map<String, TraceEntry> fTraceRoot = new LinkedHashMap<>();
    private static final AtomicLong fIds = new AtomicLong(-100000000);
    private final Map<Long, DPEntry> fDpMap = new HashMap<>();
    private final Object fLock = new Object();
    private final Collection<String> fProviderIds;

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
     * @param providerIds
     *            the IDs for the data providers to use to populate
     *            this view
     */
    public DataProviderBaseView(String id, TimeGraphPresentationProvider pres, Collection<String> providerIds) {
        super(id, pres);
        fProviderIds = providerIds;
    }

    @Override
    protected void buildEntryList(@NonNull ITmfTrace trace, @NonNull ITmfTrace parentTrace, @NonNull IProgressMonitor monitor) {
        // TODO: store and sort
        DataProviderManager dpm = DataProviderManager.getInstance();
        Collection<String> ids = fProviderIds;
        TraceEntry traceEntry = getTraceEntry(trace, parentTrace, fTraceRoot, this);
        fEntries.put(trace, traceEntry.getModel().getId(), traceEntry);
        for (String id : ids) {
            ITimeGraphDataProvider<@NonNull TimeGraphEntryModel> tDataProvider = dpm.getDataProvider(trace, id, ITimeGraphDataProvider.class);
            if (tDataProvider != null) {
                DiscreteDataProviderEntryProvider.getEntriesFromDataProvider(trace, parentTrace, tDataProvider, fDpMap, fEntries, this, monitor);
                fTProviders.add(tDataProvider);
            }
            AbstractTreeCommonXDataProvider<@NonNull TmfStateSystemAnalysisModule, @NonNull ITmfTreeDataModel> xyDataProvider = dpm.getDataProvider(trace, id, AbstractTreeCommonXDataProvider.class);
            if (xyDataProvider != null) {
                ContinuousDataProviderEntryProvider.getEntriesFromDataProvider(trace, parentTrace, xyDataProvider, fDpMap, fEntries, this, monitor);
                fXProviders.add(xyDataProvider);
            }
        }
        long start = getStartTime();
        long end = getEndTime();
        final long resolution = Long.max(1, (end - start) / getDisplayWidth());
        zoomEntries(fEntries.values(), start, end, resolution, monitor);
    }



    @Override
    public void addToEntryList(ITmfTrace trace, List<@NonNull TimeGraphEntry> list) {
        super.addToEntryList(trace, list);
    }

    private static TraceEntry getTraceEntry(ITmfTrace trace, ITmfTrace parentTrace, Map<String, TraceEntry> traceRoot, DataProviderBaseView view) {
        TraceEntry uiEntry = traceRoot.get(trace.getHostId());
        if (uiEntry == null) {
            uiEntry = new TraceEntry(null, trace);
            view.addToEntryList(parentTrace, Collections.singletonList(uiEntry));
            traceRoot.put(trace.getHostId(), uiEntry);
        }
        return uiEntry;
    }



    @Override
    protected void zoomEntries(@NonNull Iterable<@NonNull TimeGraphEntry> entries, long zoomStartTime, long zoomEndTime, long resolution, @NonNull IProgressMonitor monitor) {
        super.zoomEntries(entries, zoomStartTime, zoomEndTime, resolution, monitor);
        if (resolution < 0) {
            // StateSystemUtils.getTimes would throw an illegal argument
            // exception.
            return;
        }

        long start = Long.min(zoomStartTime, zoomEndTime);
        long end = Long.max(zoomStartTime, zoomEndTime);
        List<@NonNull Long> times = StateSystemUtils.getTimes(start, end, resolution);
        Sampling sampling = new Sampling(start, end, resolution);
        Multimap<Object, Long> providersToModelIds = filterGroupEntries(entries, zoomStartTime, zoomEndTime);
        SubMonitor subMonitor = SubMonitor.convert(monitor, getClass().getSimpleName() + "#zoomEntries(IO)", providersToModelIds.size()); //$NON-NLS-1$

        for (Entry<Object, Collection<Long>> entry : providersToModelIds.asMap().entrySet()) {
            // Blind cast
            Object key = entry.getKey();
            if (key instanceof AbstractTreeCommonXDataProvider) {
                AbstractTreeCommonXDataProvider<@NonNull TmfStateSystemAnalysisModule, @NonNull ITmfTreeDataModel> dataProvider = (AbstractTreeCommonXDataProvider<@NonNull TmfStateSystemAnalysisModule, @NonNull ITmfTreeDataModel>) key;
                SelectionTimeQueryFilter filter = new SelectionTimeQueryFilter(times, entry.getValue());
                TmfModelResponse<@NonNull ITmfXyModel> response = dataProvider.fetchXY(filter, monitor);

                ITmfXyModel model = response.getModel();
                if (model != null) {
                    Map<Long, TimeGraphEntry> row = fEntries.row(dataProvider);
                    zoomEntries(row, model, response.getStatus() == ITmfResponse.Status.COMPLETED, sampling);
                }
                subMonitor.worked(1);
            } else if(key instanceof ITimeGraphDataProvider) {
                ITimeGraphDataProvider<? extends TimeGraphEntryModel> dataProvider  = (ITimeGraphDataProvider<? extends TimeGraphEntryModel>) key;
                SelectionTimeQueryFilter filter = new SelectionTimeQueryFilter(times, entry.getValue());
                TmfModelResponse<@NonNull List<ITimeGraphRowModel>> response = dataProvider.fetchRowModel(filter, monitor);

                List<ITimeGraphRowModel> model = response.getModel();
                if (model != null) {
                    Map<Long, TimeGraphEntry> row = fEntries.row(dataProvider);
                    zoomEntries(row, model, response.getStatus() == ITmfResponse.Status.COMPLETED, sampling);
                }
                subMonitor.worked(1);

            }
        }
    }

    private void zoomEntries(Map<Long, TimeGraphEntry> map, List<ITimeGraphRowModel> model, boolean completed, Sampling sampling) {
        boolean isZoomThread = Thread.currentThread() instanceof ZoomThread;
        for (ITimeGraphRowModel rowModel : model) {
            TimeGraphEntry entry = map.get(rowModel.getEntryID());

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

    private void zoomEntries(Map<Long, TimeGraphEntry> map, ITmfXyModel model, boolean completed, Sampling sampling) {
        boolean isZoomThread = Thread.currentThread() instanceof ZoomThread;
        for (TimeGraphEntry entry : map.values()) {
            String uniqueName = getUniqueName(entry);
            ISeriesModel rowModel = model.getData().get(uniqueName);
            if (rowModel == null) {
                continue;
            }
            List<ITimeEvent> events = createTimeEvents(entry, rowModel);
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


    private static String getUniqueName(TimeGraphEntry entry) {
        List<String> names = new ArrayList<>();
        TimeGraphEntry current = entry;
        names.add(current.getName());
        while (current.getParent() != null) {
            current = current.getParent();
            names.add(current.getName());
        }
        StringJoiner stringJoiner = new StringJoiner(File.separator);
        for (int i = names.size() - 1; i >= 0; i--) {
            stringJoiner.add(names.get(i));
        }
        return stringJoiner.toString();
    }

    private static List<ITimeEvent> createTimeEvents(TimeGraphEntry entry, ISeriesModel rowModel) {
        List<ITimeEvent> timeEvents = new ArrayList<>();
        long[] xAxis = rowModel.getXAxis();
        long step = 1;
        for (int index = 0; index < xAxis.length; index++) {
            Long yValue = (long) rowModel.getData()[index];
            timeEvents.add(new TimeLineEvent(entry, xAxis[index], step, Collections.singletonList(yValue)));
        }
        return timeEvents;
    }

    private List<ITimeEvent> createTimeEvents(TimeGraphEntry entry, List<ITimeGraphState> values) {
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
        if (state.getValue() == Integer.MIN_VALUE) {
            return new NullTimeEvent(entry, state.getStartTime(), state.getDuration());
        }
        String label = state.getLabel();
        if (label != null) {
            return new NamedTimeEvent(entry, state.getStartTime(), state.getDuration(), state.getValue(), label, state.getActiveProperties());
        }
        return new TimeEvent(entry, state.getStartTime(), state.getDuration(), state.getValue(), state.getActiveProperties());
    }

    private Multimap<Object, Long> filterGroupEntries(Iterable<TimeGraphEntry> visible,
            long zoomStartTime, long zoomEndTime) {
        Multimap<Object, Long> providersToModelIds = HashMultimap.create();
        for (TimeGraphEntry entry : visible) {
            if (zoomStartTime <= entry.getEndTime() && zoomEndTime >= entry.getStartTime() && entry.hasTimeEvents()) {
                long id = entry.getModel().getId();
                if (fEntries.containsColumn(id)) {
                    Map<Object, @NonNull TimeGraphEntry> colMap = fEntries.column(id);
                    Optional<@NonNull Entry<Object, @NonNull TimeGraphEntry>> firstColor = colMap.entrySet().stream().findFirst();
                    if (firstColor.isPresent()) {
                        providersToModelIds.put(firstColor.get().getKey(), id);
                    }
                }
            }
        }
        return providersToModelIds;
    }

    /*
     * Helpers
     */

    public static long getId() {
        return fIds.incrementAndGet();
    }

    public void clampTimes(long startTime, long endTime) {
        setStartTime(Long.min(getStartTime(), startTime));
        setEndTime(Long.max(getEndTime(), endTime));
    }

    public void alignTime() {
        synchingToTime(getTimeGraphViewer().getSelectionBegin());
        refresh();
    }

    /*
     * Make these visible
     */
    @Override
    public long getStartTime() {
        return super.getStartTime();
    }

    @Override
    public long getEndTime() {
        return super.getEndTime();
    }

    @Override
    public int getDisplayWidth() {
        return super.getDisplayWidth();
    }

    public Object getLock() {
        return fLock;
    }

}
