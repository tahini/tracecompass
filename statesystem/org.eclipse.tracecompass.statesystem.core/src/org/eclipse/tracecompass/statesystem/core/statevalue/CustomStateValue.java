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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;

/**
 * @author Geneviève Bastien
 * @since 2.0
 */
public abstract class CustomStateValue extends TmfStateValue {

    private static final int MAX_BUFFER_LENGTH = 100000;

    private static final Map<Byte, CustomStateValueFactory> CUSTOM_FACTORIES = new HashMap<>();

    public static void registerCustomFactory(Byte customId, CustomStateValueFactory factory) {
        CUSTOM_FACTORIES.put(customId, factory);
    }

    public static @Nullable CustomStateValueFactory getCustomFactory(Byte customId) {
        return CUSTOM_FACTORIES.get(customId);
    }

    public static void insertStringBytes(ByteBuffer buf, String str) {
        buf.putInt(str.length());
        buf.put(str.getBytes());
    }

    public static String readString(ByteBuffer buffer) {
        int strSize = buffer.getInt();
        byte[] array = new byte[strSize];
        buffer.get(array);
        return new String(array);
    }

    @Override
    public final Type getType() {
        return Type.CUSTOM;
    }

    @Override
    public boolean isNull() {
        return false;
    }

    public byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(MAX_BUFFER_LENGTH);
        getBytesToBuffer(buffer);
        int pos = buffer.position();
        byte[] bytes = new byte[pos];
        System.arraycopy(buffer.array(), 0, bytes, 0, pos);
        return bytes;
    }

    protected abstract void getBytesToBuffer(ByteBuffer buffer);

    public abstract Byte getCustomTypeId();

    @Override
    public int compareTo(@Nullable ITmfStateValue other) {
        if (other == null) {
            throw new IllegalArgumentException();
        }

        switch (other.getType()) {
        case DOUBLE:
            throw new StateValueTypeException("A String state value cannot be compared to a Double state value."); //$NON-NLS-1$
        case INTEGER:
            throw new StateValueTypeException("A String state value cannot be compared to an Integer state value."); //$NON-NLS-1$
        case LONG:
            throw new StateValueTypeException("A String state value cannot be compared to a Long state value."); //$NON-NLS-1$
        case NULL:
            /*
             * We assume that every string state value is greater than a null
             * state value.
             */
            return 1;
        case STRING:
            throw new StateValueTypeException("A String state value cannot be compared to a String state value."); //$NON-NLS-1$
        case CUSTOM:
        default:
            throw new StateValueTypeException("A String state value cannot be compared to the type " + other.getType()); //$NON-NLS-1$
        }
    }

}
