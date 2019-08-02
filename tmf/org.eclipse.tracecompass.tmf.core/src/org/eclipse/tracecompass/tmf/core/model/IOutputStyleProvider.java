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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;

/**
 * This interface represent a style provider. Data provider that implements this
 * interface can provide styling information according to
 * {@link OutputStyleModel}. The provided model will contain the information to
 * properly format elements returned by this data provider (ex. entries, states,
 * XY series)
 *
 * @author Simon Delisle
 * @since 5.1
 */
public interface IOutputStyleProvider {

    /**
     * This methods computes a style map for this specific data provider. It
     * returns a {@link TmfModelResponse} that contains the model. The model can
     * be used to provide default style for a provider. The map contains a style
     * object attached to a style key.
     *
     * @param fetchParameters
     *            Map of parameters that can be used to compute the style map
     * @param monitor
     *            A ProgressMonitor to cancel task
     * @return A {@link TmfModelResponse} with the style model
     */
    TmfModelResponse<OutputStyleModel> fetchStyle(Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor);
}
