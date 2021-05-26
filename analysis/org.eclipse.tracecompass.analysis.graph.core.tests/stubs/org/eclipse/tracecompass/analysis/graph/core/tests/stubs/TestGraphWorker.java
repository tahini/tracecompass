/*******************************************************************************
 * Copyright (c) 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.graph.core.tests.stubs;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.graph.core.base.IGraphWorker;

/**
 * A stub graph worker for unit tests
 *
 * @author Geneviève Bastien
 */
public class TestGraphWorker implements IGraphWorker {

    /**
     *
     */
    private static final long serialVersionUID = 57296787802835537L;
    private final Integer fValue;

    /**
     * Constructor
     *
     * @param i An integer to represent this worker
     */
    public TestGraphWorker(final Integer i) {
        fValue = i;
    }

    public TestGraphWorker() {
        fValue = 0;
        System.out.println("Default constructor called");
    }

    @Override
    public String getHostId() {
        return "test";
    }

    @Override
    public int hashCode() {
        return fValue.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof TestGraphWorker) {
            return fValue.equals(((TestGraphWorker) obj).fValue);
        }
        return false;
    }
}