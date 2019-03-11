/**********************************************************************
 * Copyright (c) 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **********************************************************************/

package org.eclipse.tracecompass.tmf.core.model;

import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Output element style object for one style key. This class support style
 * inheritance. To avoid creating new style the element style can have a parent
 * style and will have all the same style properties values as the parent and
 * can add or override style properties.
 *
 * @author Simon Delisle
 * @since 5.1
 */
public class OutputElementStyle {
    private @Nullable String fParentStyleKey;
    private Map<String, Object> fValues;

    /**
     * Constructor.
     *
     * @param parentStyleKey
     *            Parent style key or null if there is no parent. The parent key
     *            should match an other style key and is used for style
     *            inheritance
     * @param values
     *            Style values or empty map if there is no values. Use to define
     *            different style properties. Example of keys: fg, bg, height,
     *            border-color, border-width, ...
     */
    public OutputElementStyle(@Nullable String parentStyleKey, Map<String, Object> values) {
        fParentStyleKey = parentStyleKey;
        fValues = values;
    }

    /**
     * Get the parent style key
     *
     * @return Parent key or null if there is no parent
     */
    public @Nullable String getParentKey() {
        return fParentStyleKey;
    }

    /**
     * Get the style values
     *
     * @return Map of values or empty if there is no given style
     */
    public Map<String, Object> getStyleValues() {
        return fValues;
    }

}
