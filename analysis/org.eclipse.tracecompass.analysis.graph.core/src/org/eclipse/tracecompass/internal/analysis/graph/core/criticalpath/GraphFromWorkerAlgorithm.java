/*******************************************************************************
 * Copyright (c) 2017 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.graph.core.criticalpath;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfVertex;
import org.eclipse.tracecompass.analysis.graph.core.criticalpath.CriticalPathAlgorithmException;

/**
 *
 *
 * @author Geneviève Bastien
 */
public class GraphFromWorkerAlgorithm extends AbstractCriticalPathAlgorithm {

    /**
     * Constructor
     *
     * @param graph
     *            The full graph
     */
    public GraphFromWorkerAlgorithm(TmfGraph graph) {
        super(graph);
    }

    @Override
    public TmfGraph compute(TmfVertex start, @Nullable TmfVertex end) throws CriticalPathAlgorithmException {
        return getGraph();
    }

}
