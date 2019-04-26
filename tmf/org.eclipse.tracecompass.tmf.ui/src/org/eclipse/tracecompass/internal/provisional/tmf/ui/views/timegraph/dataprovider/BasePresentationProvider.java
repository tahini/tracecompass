/*******************************************************************************
 * Copyright (c) 2014, 2018 École Polytechnique de Montréal and others.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Florian Wininger - Initial API and implementation
 *   Geneviève Bastien - Review of the initial implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.provisional.tmf.ui.views.timegraph.dataprovider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils;

/**
 * A basic presentation provider for the default DataProviderBaseView.
 *
 * TODO: Have the data providers provide the better defaults, so this is not
 * needed, at least not like it
 *
 * @author Geneviève Bastien
 */
@NonNullByDefault({})
public class BasePresentationProvider extends TimeGraphPresentationProvider {

    private static final long[] COLOR_SEED = { 0x0000ff, 0xff0000, 0x00ff00,
            0xff00ff, 0x00ffff, 0xffff00, 0x000000, 0xf07300
    };

    private static final int COLOR_MASK = 0xffffff;

    private List<StateItem> fStateValues = new ArrayList<>();
    /**
     * Average width of the characters used for state labels. Is computed in the
     * first call to postDrawEvent(). Is null before that.
     */
    private @Nullable Integer fAverageCharacterWidth = null;
    /*
     * Maps the value of an event with the corresponding index in the
     * stateValues list
     */
    private Map<Integer, Integer> fStateIndex = new HashMap<>();
    private Map<String, Integer> fLabelMap = new HashMap<>();
    private StateItem[] stateTable = new StateItem[0];

    /**
     * Presentation provider constructor
     */
    public BasePresentationProvider() {
    }

    @Override
    public int getStateTableIndex(@Nullable ITimeEvent event) {
        if (event instanceof NullTimeEvent) {
            return INVISIBLE;
        }
        if (event instanceof TimeEvent && ((TimeEvent) event).hasValue()) {
            TimeEvent tcEvent = (TimeEvent) event;

            int value = tcEvent.getValue();

            // Draw state only if state is already known
            Integer index = fStateIndex.get(value);
            if (index != null) {
                return index;
            }
        } else if (event instanceof TimeEvent && !((TimeEvent) event).hasValue()) {
            // Does the event have a label
            String label = event.getLabel();
            if (label != null) {
                Integer index = fLabelMap.get(label);
                if (index == null) {
                    // Add this label
                    index = addState(label);
                }
                Integer value = fStateIndex.get(index);
                if (value != null) {
                    return value;
                }
            }
        }
        return TRANSPARENT;
    }

    @Override
    public StateItem[] getStateTable() {
        return stateTable;
    }

    @Override
    public @Nullable String getEventName(@Nullable ITimeEvent event) {
        if (event == null) {
            return null;
        }
        String label = event.getLabel();
        if (event instanceof TimeEvent && ((TimeEvent) event).hasValue()) {
            TimeEvent tcEvent = (TimeEvent) event;

            int value = tcEvent.getValue();

            Integer index = fStateIndex.get(value);
            if (index != null) {
                return fStateValues.get(index.intValue()).getStateString();
            }
            return null;
        } else if (label != null) {
            return label;
        }
        return "multiple state";
    }

    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event, long hoverTime) {
        /*
         * TODO: Add the XML elements to support adding extra information in the
         * tooltips and implement this
         */
        return Collections.emptyMap();
    }

    /**
     * Returns the average character width, measured in pixels, of the font
     * described by the receiver.
     *
     * @param gc
     *            The graphic context
     * @return the average character width of the font
     */
    @Deprecated
    private static int getAverageCharWidth(GC gc) {
        return gc.getFontMetrics().getAverageCharWidth();
    }

    @Override
    public void postDrawEvent(@Nullable ITimeEvent event, @Nullable Rectangle bounds, @Nullable GC gc) {
        if (event == null || bounds == null || gc == null) {
            return;
        }
        // See if the state is too short to show text
        Integer averageCharacterWidth = fAverageCharacterWidth;
        if (averageCharacterWidth == null) {
            averageCharacterWidth = getAverageCharWidth(gc);
            fAverageCharacterWidth = averageCharacterWidth;
        }
        if (bounds.width <= averageCharacterWidth) {
            return;
        }
        String eventName = getEventName(event);
        if (eventName == null) {
            return;
        }

        Color stateColor = gc.getBackground();
        gc.setForeground(Utils.getDistinctColor(stateColor.getRGB()));
        Utils.drawText(gc, eventName, bounds.x, bounds.y, bounds.width, bounds.height, true, true);
    }

    /**
     * Add a new state in the time graph view. This allow to define at runtime
     * new states that cannot be known at the conception of this analysis.
     *
     * @param name
     *            The string associated with the state
     * @return the value for this state
     */
    public synchronized int addState(String name) {
        // Find a value for this name, start at 10000
        int value = 10000;
        while (fStateIndex.get(value) != null) {
            value++;
        }
        addOrUpdateState(value, name);
        Display.getDefault().asyncExec(this::fireColorSettingsChanged);
        return value;
    }

    private synchronized void addOrUpdateState(int value, String name) {
        // FIXME Allow this case
        if (value < 0) {
            return;
        }

        final RGB colorRGB = calcColor(name);

        StateItem item = new StateItem(colorRGB, name);

        Integer index = fStateIndex.get(value);
        if (index == null) {
            /* Add the new state value */
            fStateIndex.put(value, fStateValues.size());
            fStateValues.add(item);
            fLabelMap.put(name, value);
        } else {
            /* Override a previous state value */
            fStateValues.set(index, item);
        }
        stateTable = fStateValues.toArray(new StateItem[fStateValues.size()]);
    }

    /*
     * This method will always return the same color for a same name, no matter
     * the value, so that different traces with the same XML analysis will
     * display identically states with the same name.
     */
    private static RGB calcColor(String name) {
        long hash = name.hashCode(); // hashcodes can be Integer.MIN_VALUE.
        long base = COLOR_SEED[(int) (Math.abs(hash) % COLOR_SEED.length)];
        int x = (int) ((hash & COLOR_MASK) ^ base);
        final int r = (x >> 16) & 0xff;
        final int g = (x >> 8) & 0xff;
        final int b = x & 0xff;
        return new RGB(r, g, b);
    }

    /**
     * Return whether an integer value has a corresponding index in the
     * available states
     *
     * @param status
     *            The numerical status of the event
     * @return <code>true</code> if the numerical value is an existing value in
     *         the available states
     */
    public boolean hasIndex(int status) {
        return fStateIndex.containsKey(status);
    }

}
