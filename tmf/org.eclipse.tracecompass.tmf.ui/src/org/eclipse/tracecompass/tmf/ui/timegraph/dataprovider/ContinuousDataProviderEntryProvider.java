/*******************************************************************************
 * Copyright (c) 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.timegraph.dataprovider;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.kernel.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.core.model.xy.AbstractTreeCommonXDataProvider;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphLineEntry;

import com.google.common.collect.Table;

public final class ContinuousDataProviderEntryProvider {
    private static final long BUILD_UPDATE_TIMEOUT = 500;

    public static void getEntriesFromDataProvider(ITmfTrace trace, ITmfTrace parentTrace, AbstractTreeCommonXDataProvider<@NonNull TmfStateSystemAnalysisModule, @NonNull ITmfTreeDataModel> dataProvider, Map<Long, DPEntry>  dpMap, Table<Object, Long, @NonNull TimeGraphEntry> timeGraphEntries, DataProviderBaseView view, IProgressMonitor monitor) {
        if (dataProvider == null) {
            return;
        }
        boolean complete = false;
        while (!complete && !monitor.isCanceled()) {
            TmfModelResponse<@NonNull List<@NonNull ITmfTreeDataModel>> response = dataProvider.fetchTree(new TimeQueryFilter(0, Long.MAX_VALUE, 2), monitor);
            if (response.getStatus() == ITmfResponse.Status.FAILED) {
                Activator.getDefault().logError(view.getClass().getSimpleName() + " Data Provider failed: " + response.getStatusMessage()); //$NON-NLS-1$
                return;
            } else if (response.getStatus() == ITmfResponse.Status.CANCELLED) {
                return;
            }
            complete = response.getStatus() == ITmfResponse.Status.COMPLETED;

            List<@NonNull ITmfTreeDataModel> model = response.getModel();
            if (model != null) {
                synchronized (view.getLock()) {
                    for (ITmfTreeDataModel entry : model) {
                        TimeGraphEntry uiEntry = timeGraphEntries.get(dataProvider, entry.getId());
                        if (entry.getParentId() != -1) {
                            if (uiEntry == null) {
                                uiEntry = new TimeGraphLineEntry(entry);
                                TimeGraphEntry parent = timeGraphEntries.get(dataProvider, entry.getParentId());
                                if (parent != null) {
                                    parent.addChild(uiEntry);
                                }
                                timeGraphEntries.put(dataProvider, entry.getId(), uiEntry);
                            } else {
                                if (uiEntry instanceof TimeGraphLineEntry) {
                                    ((TimeGraphLineEntry) uiEntry).updateModel(entry);
                                }
                            }
                        } else {
                            if (uiEntry instanceof TimeGraphLineEntry) {
                                ((TimeGraphLineEntry) uiEntry).updateModel(entry);
                            } else if (uiEntry == null) {
                                uiEntry = new DPEntry(dataProvider);
                                dpMap.put(uiEntry.getModel().getId(), (DPEntry) uiEntry);
                                timeGraphEntries.put(dataProvider, entry.getId(), uiEntry);
                            }
                        }
                    }
                }
            }

            if (monitor.isCanceled()) {
                return;
            }

            if (parentTrace.equals(trace)) {
                view.alignTime();
            }
            monitor.worked(1);

            if (!complete && !monitor.isCanceled()) {
                try {
                    Thread.sleep(BUILD_UPDATE_TIMEOUT);
                } catch (InterruptedException e) {
                    Activator.getDefault().logError("Failed to wait for data provider", e); //$NON-NLS-1$
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
