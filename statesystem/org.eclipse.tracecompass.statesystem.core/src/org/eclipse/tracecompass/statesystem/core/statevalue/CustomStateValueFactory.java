/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.statesystem.core.statevalue;

import java.nio.ByteBuffer;

/**
 * @since 2.0
 */
public interface CustomStateValueFactory {

    CustomStateValue readCustomValue(ByteBuffer buffer);

}
