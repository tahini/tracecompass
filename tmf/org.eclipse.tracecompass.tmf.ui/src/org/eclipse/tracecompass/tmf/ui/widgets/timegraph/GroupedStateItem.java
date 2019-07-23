/*******************************************************************************
 * Copyright (c) 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.widgets.timegraph;

import java.util.Map;

import org.eclipse.swt.graphics.RGB;

/**
 * Grouped state items are items that belong to a certain family
 *
 * @author Simon Delisle
 * @since 5.1
 */
public class GroupedStateItem extends StateItem {

    private final String fGroupeLabel;

    /**
     * Constructor
     *
     * @param rgb
     *            StateItem color
     * @param label
     *            StateItem label
     * @param groupLabel
     *            StateItem group
     */
    public GroupedStateItem(RGB rgb, String label, String groupLabel) {
        super(rgb, label);
        fGroupeLabel = groupLabel;
    }

    /**
     * Constructor
     *
     * @param style
     *            StateItem color
     * @param groupLabel
     *            StateItem group
     */
    public GroupedStateItem(Map<String, Object> style, String groupLabel) {
        super(style);
        fGroupeLabel = groupLabel;
    }

    /**
     * Get this stateItem group
     *
     * @return Group label
     */
    public String getGroupLabel() {
        return fGroupeLabel;
    }

}
