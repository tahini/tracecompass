/*******************************************************************************
 * Copyright (c) 2012, 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 ******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.inputoutput;

import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;

/**
 * State values that are used in the kernel event handler. It's much better to
 * use integer values whenever possible, since those take much less space in the
 * history file.
 *
 * @author Houssem Daoud
 * @since 2.0
 *
 */
@SuppressWarnings("javadoc")
public interface StateValues {

    /* CPU Status */
    static final int READING_REQUEST = 1;
    static final int WRITING_REQUEST = 2;
    static final int IN_BLOCK_QUEUE = 3;
    static final int IN_DRIVER_QUEUE = 4;

    static final ITmfStateValue READING_REQUEST_VALUE = TmfStateValue.newValueInt(READING_REQUEST);
    static final ITmfStateValue WRITING_REQUEST_VALUE = TmfStateValue.newValueInt(WRITING_REQUEST);
    static final ITmfStateValue IN_BLOCK_QUEUE_VALUE = TmfStateValue.newValueInt(IN_BLOCK_QUEUE);
    static final ITmfStateValue IN_DRIVER_QUEUE_VALUE = TmfStateValue.newValueInt(IN_DRIVER_QUEUE);

}
