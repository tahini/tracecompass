/*******************************************************************************
 * Copyright (c) 2021 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.graph.core.graph;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.jdt.annotation.Nullable;

/**
 * @author Geneviève Bastien
 * @since 3.0
 */
public class TmfGraphFactory {

    /**
     *
     *
     * @return
     */
    public static @Nullable ITmfGraph createGraphOnDisk(String id, File htFile, Path segmentFile) {
        TmfGraphOnDisk graph = new TmfGraphOnDisk();
        try {
            graph.init(id, htFile, segmentFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return graph;
    }

}
