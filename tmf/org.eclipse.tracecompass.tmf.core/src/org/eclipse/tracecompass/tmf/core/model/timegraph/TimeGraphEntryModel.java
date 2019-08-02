/**********************************************************************
 * Copyright (c) 2017, 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **********************************************************************/

package org.eclipse.tracecompass.tmf.core.model.timegraph;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeDataModel;

/**
 * Implementation of {@link ITimeGraphEntryModel}.
 *
 * @author Simon Delisle
 * @since 4.0
 */
public class TimeGraphEntryModel extends TmfTreeDataModel implements ITimeGraphEntryModel {
    private final long fStartTime;
    private final long fEndTime;
    private final boolean fHasRowModel;
    private @Nullable Map<String, Object> fData;

    /**
     * Constructor
     *
     * @param id
     *            Entry ID
     * @param parentId
     *            Parent ID
     * @param name
     *            Entry name to be displayed
     * @param startTime
     *            Start time
     * @param endTime
     *            End time
     */
    public TimeGraphEntryModel(long id, long parentId, String name, long startTime, long endTime) {
        this(id, parentId, Collections.singletonList(name), startTime, endTime, true);
    }

    /**
     * Constructor
     *
     * @param id
     *            Entry ID
     * @param parentId
     *            Parent ID
     * @param name
     *            Entry name to be displayed
     * @param startTime
     *            Start time
     * @param endTime
     *            End time
     * @param hasRowModel
     *            true if the entry has a row model
     */
    public TimeGraphEntryModel(long id, long parentId, String name, long startTime, long endTime, boolean hasRowModel) {
        this(id, parentId, Collections.singletonList(name), startTime, endTime, hasRowModel);
    }

    /**
     * Constructor
     *
     * @param id
     *            Entry ID
     * @param parentId
     *            Parent ID
     * @param labels
     *            Entry labels to be displayed
     * @param startTime
     *            Start time
     * @param endTime
     *            End time
     * @since 5.0
     */
    public TimeGraphEntryModel(long id, long parentId, List<String> labels, long startTime, long endTime) {
        this(id, parentId, labels, startTime, endTime, true);
    }

    /**
     * Constructor
     *
     * @param id
     *            Entry ID
     * @param parentId
     *            Parent ID
     * @param labels
     *            Entry labels to be displayed
     * @param startTime
     *            Start time
     * @param endTime
     *            End time
     * @param hasRowModel
     *            true if the entry has a row model
     * @since 5.0
     */
    public TimeGraphEntryModel(long id, long parentId, List<String> labels, long startTime, long endTime, boolean hasRowModel) {
        super(id, parentId, labels);
        fStartTime = startTime;
        fEndTime = endTime;
        fHasRowModel = hasRowModel;
    }

    @Override
    public long getStartTime() {
        return fStartTime;
    }

    @Override
    public long getEndTime() {
        return fEndTime;
    }

    @Override
    public boolean hasRowModel() {
        return fHasRowModel;
    }

    /**
     * @since 5.1
     */
    @Override
    public @Nullable Map<String, Object> getData() {
        return fData;
    }

    /**
     * Set additional data for this entry
     *
     * @param data
     *            Map of additional data
     * @since 5.1
     */
    public void setData(Map<String, Object> data) {
        // TODO Bad do not set
        fData = data;
    }

    @Override
    public String toString() {
        return "<name=" + getLabels() + " id=" + getId() + " parentId=" + getParentId() //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                + " start=" + fStartTime + " end=" + fEndTime + " hasRowModel=" + hasRowModel() + ">"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }
}
