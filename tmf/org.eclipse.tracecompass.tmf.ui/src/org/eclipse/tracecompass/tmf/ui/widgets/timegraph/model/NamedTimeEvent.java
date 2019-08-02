/*******************************************************************************
 * Copyright (c) 2017, 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model;

import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.model.timegraph.IElementResolver;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;

/**
 * {@link TimeEvent} with a label.
 *
 * @since 3.3
 * @author Loic Prieur-Drevon
 */
public class NamedTimeEvent extends TimeEvent {
    private final @NonNull String fLabel;

    /**
     * Constructor
     *
     * @param entry
     *            The entry to which this time event is assigned
     * @param time
     *            The timestamp of this event
     * @param duration
     *            The duration of this event
     * @param value
     *            The status assigned to the event
     * @param label
     *            This event's label
     */
    public NamedTimeEvent(ITimeGraphEntry entry, long time, long duration,
            int value, @NonNull String label) {
        super(entry, time, duration, value);
        fLabel = label.intern();
    }

    /**
     * Constructor
     *
     * @param entry
     *            The entry to which this time event is assigned
     * @param time
     *            The timestamp of this event
     * @param duration
     *            The duration of this event
     * @param value
     *            The status assigned to the event
     * @param label
     *            This event's label
     * @param activeProperties
     *            The active properties
     * @since 4.0
     */
    public NamedTimeEvent(TimeGraphEntry entry, long time, long duration, int value, String label, int activeProperties) {
        super(entry, time, duration, value, activeProperties);
        fLabel = label.intern();
    }

    /**
     * Constructor
     *
     * @param stateModel
     *            {@link ITimeGraphState} that represent this time event
     * @param entry
     *            The entry to which this time event is assigned
     * @param label
     *            This event's label
     * @since 5.1
     */
    public NamedTimeEvent(ITimeGraphState stateModel, ITimeGraphEntry entry, String label) {
        super(stateModel, entry);
        fLabel = label.intern();
    }

    @Override
    public @NonNull String getLabel() {
        return fLabel;
    }

    /**
     * @since 4.0
     */
    @Override
    public @NonNull Map<@NonNull String, @NonNull String> computeData() {
        Map<@NonNull String, @NonNull String> data = super.computeData();
        data.put(IElementResolver.LABEL_KEY, getLabel());
        return data;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NamedTimeEvent) {
            return super.equals(obj) && Objects.equals(fLabel, ((NamedTimeEvent) obj).fLabel);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fLabel);
    }
}
