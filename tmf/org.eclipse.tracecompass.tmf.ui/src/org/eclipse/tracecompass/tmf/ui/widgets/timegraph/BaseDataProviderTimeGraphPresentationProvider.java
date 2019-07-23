/*******************************************************************************
 * Copyright (c) 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.widgets.timegraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.model.IOutputStyleProvider;
import org.eclipse.tracecompass.tmf.core.model.ITimeEventStyleStrings;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;
import org.eclipse.tracecompass.tmf.core.model.OutputStyleModel;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * {@link TimeGraphPresentationProvider} for presentation provider that uses
 * data provider
 *
 * @since 5.1
 */
public class BaseDataProviderTimeGraphPresentationProvider extends TimeGraphPresentationProvider {
    private final String fProviderId;
//    private OutputStyleModel fStyles;
    private Map<@NonNull String, @NonNull OutputElementStyle> fStylesMap;
    private StateItem[] fStateTable;
    private Map<String, Integer> fLabelToIndex = new HashMap<>();

    public BaseDataProviderTimeGraphPresentationProvider(String dataProviderId) {
        super();
        fProviderId = dataProviderId;
    }

    protected String getProviderId() {
        return fProviderId;
    }

    public Map<@NonNull String, @NonNull OutputElementStyle> getStyles() {
        ITmfTrace parentTrace = getTrace();
        if (fStylesMap == null && parentTrace != null) {
            fStylesMap = new HashMap<>();
            ITimeGraphDataProvider provider = DataProviderManager.getInstance().getDataProvider(parentTrace, getProviderId(), ITimeGraphDataProvider.class);
            if (parentTrace instanceof TmfExperiment) {
                Collection<@NonNull ITmfTrace> traces = TmfTraceManager.getTraceSet(parentTrace);
                for (ITmfTrace trace : traces) {
                    provider = DataProviderManager.getInstance().getDataProvider(trace, fProviderId, ITimeGraphDataProvider.class);
                    if (provider instanceof IOutputStyleProvider) {
                        TmfModelResponse<@NonNull OutputStyleModel> styleResponse = ((IOutputStyleProvider) provider).fetchStyle(getStyleParameters(), null);
                        OutputStyleModel styleModel = styleResponse.getModel();
                        if (styleModel != null) {
                            Map<@NonNull String, @NonNull OutputElementStyle> currentStyleMap = styleModel.getStyles();
                            fStylesMap.putAll(currentStyleMap);
                        }
                    }
                }
            } else {
                if (provider instanceof IOutputStyleProvider) {
                    TmfModelResponse<@NonNull OutputStyleModel> styleResponse = ((IOutputStyleProvider) provider).fetchStyle(getStyleParameters(), null);
                    OutputStyleModel styleModel = styleResponse.getModel();
                    if (styleModel != null) {
                        fStylesMap = styleModel.getStyles();
                    }
                }
            }
        }
        return fStylesMap;


//        if (fStyles == null && trace != null) {
//            ITimeGraphDataProvider provider = DataProviderManager.getInstance().getDataProvider(trace, fProviderId, ITimeGraphDataProvider.class);
//            if (provider instanceof IOutputStyleProvider) {
//                TmfModelResponse<@NonNull OutputStyleModel> styleResponse = ((IOutputStyleProvider) provider).fetchStyle(getStyleParameters(), null);
//                fStyles = styleResponse.getModel();
//            }
//        }
//        return fStyles;
    }

    public Map<String, Object> getStyleParameters() {
        return Collections.emptyMap();
    }

    @Override
    public StateItem[] getStateTable() {
        if (fStateTable == null) {
            Map<@NonNull String, @NonNull OutputElementStyle> styles = getStyles();
            if (styles == null || styles.isEmpty()) {
                return new StateItem[0];
            }
            List<StateItem> stateItemList = new ArrayList<>();
            int tableIndex = 0;
//            for (Entry<@NonNull String, @NonNull OutputElementStyle> styleEntry : styles.getStyles().entrySet()) {
            for (Entry<@NonNull String, @NonNull OutputElementStyle> styleEntry : fStylesMap.entrySet()) {
                Map<@NonNull String, @NonNull Object> elementStyle = styleEntry.getValue().getStyleValues();
                // TODO Handle other style, right now it is just color
                fLabelToIndex.put(styleEntry.getKey(), tableIndex);
                tableIndex++;

                // TODO I don't like that, not sure what to do here
                Object groupLabel = elementStyle.get("group");
                if (groupLabel instanceof String) {
                    stateItemList.add(new GroupedStateItem(elementStyle, (String) groupLabel));
                } else {
                    stateItemList.add(new StateItem(elementStyle));
                }
            }
            fStateTable = stateItemList.toArray(new StateItem[stateItemList.size()]);
            return fStateTable;
        }
        return fStateTable;
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        if (event instanceof NullTimeEvent) {
            return INVISIBLE;
        }

        if(event instanceof TimeEvent) {
            if (((TimeEvent) event).hasValue()) {
                Integer index = fLabelToIndex.get(String.valueOf(((TimeEvent) event).getValue()));
                return index != null ? index: 0;
            }

        }
        return TRANSPARENT;
    }

    /**
     * Get the style map of a given ITimeEvent
     *
     * @param event
     *            the time event
     * @return the style map, as detailed in {@link ITimeEventStyleStrings}
     * @since 3.0
     */
    @Override
    public Map<String, Object> getEventStyle(ITimeEvent event) {
        Map<String, Object> styles = new HashMap<>();
        if(event instanceof TimeEvent) {
            ITimeGraphState model = ((TimeEvent) event).getStateModel();
            OutputElementStyle eventStyle = model.getStyle();
            if (eventStyle != null) {
                styles.putAll(eventStyle.getStyleValues());
            }
        }
        return styles;
    }

    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event, long hoverTime) {
        ITimeGraphEntry entry = event.getEntry();

        if (event instanceof TimeEvent && ((TimeEvent) event).hasValue() && entry instanceof TimeGraphEntry) {
            ITmfTreeDataModel model = ((TimeGraphEntry) entry).getEntryModel();
            ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider = BaseDataProviderTimeGraphView.getProvider((TimeGraphEntry) entry);
            if (provider != null) {
                return getTooltip(provider, model.getId(), hoverTime);
            }
        }

        return Collections.emptyMap();
    }

    private static Map<String, String> getTooltip(ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider, long id, long hoverTime) {
        SelectionTimeQueryFilter filter = new SelectionTimeQueryFilter(Collections.singletonList(hoverTime), Collections.singleton(id));
        TmfModelResponse<Map<String, String>> response = provider.fetchTooltip(FetchParametersUtils.selectionTimeQueryToMap(filter), null);
        Map<String, String> tooltip = response.getModel();

        if (tooltip == null) {
            return Collections.emptyMap();
        }

        Map<String, String> retMap = new LinkedHashMap<>();
        retMap.putAll(tooltip);
        return retMap;
    }

    @Override
    public void refresh() {
//        fStyles = null;
        fStylesMap = null;
        fStateTable = null;
        super.refresh();
    }
}
