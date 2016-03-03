/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.latency;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;

/**
 * @since 2.0
 */
public class IntervalSegment implements ISegment {

    /**
     *
     */
    private static final long serialVersionUID = 3064409294604514508L;
    private final ITmfStateInterval fInterval;

    public IntervalSegment(ITmfStateInterval interval) {
        fInterval = interval;
    }

    @Override
    public int compareTo(@NonNull ISegment o) {

        return 0;
    }

    @Override
    public long getStart() {
        return fInterval.getStartTime();
    }

    @Override
    public long getEnd() {
        return fInterval.getEndTime();
    }

}
