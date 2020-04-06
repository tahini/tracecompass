/**********************************************************************
 * Copyright (c) 2020 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.internal.analysis.os.linux.core.inputoutput;

import java.util.List;
import java.util.Objects;

import org.eclipse.tracecompass.tmf.core.dataprovider.X11ColorUtils;
import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.ImmutableList;

/**
 * A class who provides various color pairs for read/write data. Reads have a
 * blueish tint, while the writes are reddish.
 *
 * @author Geneviève Bastien
 */
public class IODataPalette {

    private static final List<Pair<String, String>> COLOR_LIST;

    static {
        ImmutableList.Builder<Pair<String, String>> colorBuilder = new ImmutableList.Builder<>();

        colorBuilder.add(new Pair<>(
                Objects.requireNonNull(X11ColorUtils.toHexColor("blue")), //$NON-NLS-1$
                Objects.requireNonNull(X11ColorUtils.toHexColor("red")))); //$NON-NLS-1$
        colorBuilder.add(new Pair<>(
                Objects.requireNonNull(X11ColorUtils.toHexColor("cyan")), //$NON-NLS-1$
                Objects.requireNonNull(X11ColorUtils.toHexColor("orange red")))); //$NON-NLS-1$
        colorBuilder.add(new Pair<>(
                Objects.requireNonNull(X11ColorUtils.toHexColor("deep sky blue")), //$NON-NLS-1$
                Objects.requireNonNull(X11ColorUtils.toHexColor("firebrick")))); //$NON-NLS-1$
        colorBuilder.add(new Pair<>(
                Objects.requireNonNull(X11ColorUtils.toHexColor("navy blue")), //$NON-NLS-1$
                Objects.requireNonNull(X11ColorUtils.toHexColor("web maroon")))); //$NON-NLS-1$
        colorBuilder.add(new Pair<>(
                Objects.requireNonNull(X11ColorUtils.toHexColor("pale turquoise")), //$NON-NLS-1$
                Objects.requireNonNull(X11ColorUtils.toHexColor("orange")))); //$NON-NLS-1$
        COLOR_LIST = colorBuilder.build();
    }

    private IODataPalette() {
        // Do nothing
    }

    /**
     * Get the list of color pairs for read/write data. For each element in the
     * list, the first element of the pair is the read color (blueish) and the
     * second color is the write (reddish).
     *
     * The color strings have an hexadecimal format: #xxxxxx
     *
     * @return The list of color pairs for read/write
     */
    public static List<Pair<String, String>> getColors() {
        return COLOR_LIST;
    }

}
