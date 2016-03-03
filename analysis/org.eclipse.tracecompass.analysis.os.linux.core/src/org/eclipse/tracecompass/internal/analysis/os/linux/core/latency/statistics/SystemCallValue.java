/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.analysis.os.linux.core.latency.statistics;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.statesystem.core.statevalue.CustomStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.CustomStateValueFactory;

public class SystemCallValue extends CustomStateValue {

    public final static Byte CUSTOM_TYPE_ID = 59;
    public final static CustomStateValueFactory FACTORY = (buffer) -> {
        String name = CustomStateValue.readString(buffer);
        int mapSize = buffer.getInt();
        Map<String, String> args = new HashMap<>();
        for (int i = 0; i<mapSize; i++) {
            String key = CustomStateValue.readString(buffer);
            String value = CustomStateValue.readString(buffer);
            args.put(key, value);
        }
        return new SystemCallValue(name, args);

    };

    private final String fName;
    private final Map<String, String> fArgs;
    private int fRet;

    public SystemCallValue(String name, Map<String, String> args) {
        fName = name;
        fArgs = args;
    }

    @Override
    protected void getBytesToBuffer(ByteBuffer buffer) {
        CustomStateValue.insertStringBytes(buffer, fName);
        buffer.putInt(fArgs.size());
        for (Entry<String, String> entry : fArgs.entrySet()) {
            CustomStateValue.insertStringBytes(buffer, entry.getKey());
            CustomStateValue.insertStringBytes(buffer, entry.getValue());
        }
    }

    @Override
    public @NonNull Byte getCustomTypeId() {
        return CUSTOM_TYPE_ID;
    }

    public static Byte getTypeId() {
        return CUSTOM_TYPE_ID;
    }

    public String getName() {
        return fName;
    }

    public Map<String, String> getArgs() {
        return fArgs;
    }

    public int getReturnValue() {
        return fRet;
    }

    public void setReturnValue(int ret) {
        fRet = ret;
    }

    @Override
    public String toString() {
        return "Syscall: " + fName;
    }

}
