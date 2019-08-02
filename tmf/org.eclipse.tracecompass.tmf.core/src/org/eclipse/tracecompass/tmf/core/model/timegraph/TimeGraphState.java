/**********************************************************************
 * Copyright (c) 2017, 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **********************************************************************/

package org.eclipse.tracecompass.tmf.core.model.timegraph;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.TmfStrings;
import org.eclipse.tracecompass.tmf.core.model.OutputElementStyle;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Implementation of {@link ITimeGraphState}
 *
 * @author Simon Delisle
 * @since 4.0
 */
public class TimeGraphState implements ITimeGraphState {
    private final long fStartTime;
    private final long fDuration;
    private final int fValue;
    private final @Nullable String fLabel;
    private @Nullable OutputElementStyle fStyle;
    private @Nullable Map<String, Object> fData;

    /**
     * A map of properties to activate or deactivate
     */
    private int fActiveProperties = 0;

    /**
     * Constructor
     *
     * @param time
     *            Time
     * @param duration
     *            State duration
     * @param value
     *            Type of state (event type)
     */
    public TimeGraphState(long time, long duration, int value) {
        fStartTime = time;
        fDuration = duration;
        fValue = value;
        fLabel = null;
    }

    /**
     * Constructor
     *
     * @param time
     *            Time
     * @param duration
     *            State duration
     * @param value
     *            Type of state (event type)
     * @param label
     *            State label
     */
    public TimeGraphState(long time, long duration, int value, String label) {
        fStartTime = time;
        fDuration = duration;
        fValue = value;
        fLabel = label;
    }

    @Override
    public long getStartTime() {
        return fStartTime;
    }

    @Override
    public long getDuration() {
        return fDuration;
    }

    @Override
    public int getValue() {
        return fValue;
    }

    @Override
    public @Nullable String getLabel() {
        return fLabel;
    }

    @Override
    public @Nullable OutputElementStyle getStyle() {
        return fStyle;
    }

    /**
     * Set the style for this state
     *
     * @param style
     *            Style
     * @since 5.1
     */
    public void setStyle(OutputElementStyle style) {
        fStyle = style;
    }

    @Override
    public @Nullable Map<String, Object> getData() {
        return fData;
    }

    /**
     * Set the additional data for this state
     *
     * @param data
     *            Map of additional data to include
     * @since 5.1
     */
    public void setData(Map<String, Object> data) {
        fData = data;
    }

    @Override
    public Multimap<String, Object> getMetadata() {
        Multimap<String, Object> toTest = HashMultimap.create();
        String label = getLabel();
        if (label != null) {
            toTest.put(IElementResolver.LABEL_KEY, label);
        }
        toTest.put(TmfStrings.startTime(), fStartTime);
        toTest.put(TmfStrings.endTime(), fStartTime + fDuration);
        toTest.put(TmfStrings.duration(), fDuration);
        return toTest;
    }

    @Override
    public int getActiveProperties() {
        return fActiveProperties;
    }

    @Override
    public void setActiveProperties(int activeProperties) {
        fActiveProperties = activeProperties;
    }

    @Deprecated
    @Override
    public Map<String, String> computeData() {
        Map<String, String> toTest = new HashMap<>();
        String label = getLabel();
        if (label != null) {
            toTest.put(IElementResolver.LABEL_KEY, label);
        }
        return toTest;
    }
}
