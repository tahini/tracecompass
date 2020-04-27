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

import org.eclipse.tracecompass.tmf.core.util.Pair;

import com.google.common.collect.ImmutableList;

/**
 * A class who provides various color pairs for read/write data. Reads have a
 * blueish tint, while the writes are reddish.
 *
 * Those colors are taken from the {@href https://colorbrewer2.org} from
 * sequential palettes of blues and reds with 6 colors, eliminating the first
 * paler one
 *
 * @author Geneviève Bastien
 */
public class IODataPalette {

    private static final List<Pair<String, String>> COLOR_LIST;

    static {
        ImmutableList.Builder<Pair<String, String>> colorBuilder = new ImmutableList.Builder<>();

        // Middle color
        colorBuilder.add(new Pair<>(
                "#6baed6", //$NON-NLS-1$
                "#fb6a4a")); //$NON-NLS-1$
        // Paler
        colorBuilder.add(new Pair<>(
                "#eff3ff", //$NON-NLS-1$
                "#fee5d9")); //$NON-NLS-1$
        // Darker
        colorBuilder.add(new Pair<>(
                "#08519c", //$NON-NLS-1$
                "#a50f15")); //$NON-NLS-1$
        // Between paler and middle
        colorBuilder.add(new Pair<>(
                "#bdd7e7", //$NON-NLS-1$
                "#fcae91")); //$NON-NLS-1$
        // Between middle and darker
        colorBuilder.add(new Pair<>(
                "#3182bd", //$NON-NLS-1$
                "#de2d26")); //$NON-NLS-1$
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
