/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.segmentstore.core.tests;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.segmentstore.core.arraylist.ArrayListStore;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;

/**
 * Unit tests for intersecting elements in an ArrayListStore
 *
 * @author France Lapointe Nguyen
 * @author Matthew Khouzam
 */
public class ArrayListStoreTest extends AbstractTestSegmentStore {

    @Override
    protected ISegmentStore<@NonNull TestSegment> getSegmentStore() {
        return new ArrayListStore<>();
    }

    @Override
    protected ISegmentStore<@NonNull TestSegment> getSegmentStore(@NonNull TestSegment @NonNull [] data) {
        return new ArrayListStore<>(data);
    }
}