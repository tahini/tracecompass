/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.model;

import com.google.common.collect.Multimap;

/**
 * This interface can be implemented by classes whose model elements have
 * additional metadata that can be used to filter them
 *
 * @since 4.1
 */
public interface IFilterableDataModel {

    /**
     * Get the metadata for this data model. The keys are the names of the
     * metadata field or aspect. A field may have multiple values associated
     * with it.
     *
     * @return A map of field names to values
     */
    Multimap<String, String> getMetadata();

}
