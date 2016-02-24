/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.statesystem;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.Activator;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;

/**
 * This class allows to recycle state system attributes. Instead of creating a
 * lot of short-lived attributes, it is sometimes useful to re-use an attribute
 * (and its whole sub-tree) that was previously used and is no longer required.
 * This class keeps a list of children attributes of a base quark and grows that
 * list as needed.
 *
 * @author Geneviève Bastien
 * @since 2.0
 */
public class TmfRecyclableAttribute {

    private final ITmfStateSystemBuilder fSs;
    private final Integer fBaseQuark;
    private final Queue<@Nullable Integer> fAvailableQuarks = new PriorityQueue<>();
    private final Set<Integer> fInUseQuarks = new TreeSet<>();
    private int count = 0;

    /**
     * Constructor
     *
     * @param ss
     *            The state system
     * @param baseQuark
     *            The base quark under which to add the recyclable attributes
     */
    public TmfRecyclableAttribute(ITmfStateSystemBuilder ss, Integer baseQuark) {
        fSs = ss;
        fBaseQuark = baseQuark;
    }

    /**
     * Get an available quark. If there is one available, it will be reused,
     * otherwise a new quark will be created under the base quark. The name of
     * the attributes is a sequential integer. So the first quark to be added
     * will be named '0', the next one '1', etc.
     *
     * @return An available quark
     */
    public synchronized Integer getAvailableQuark() {
        Integer quark = fAvailableQuarks.poll();
        if (quark == null) {
            quark = fSs.getQuarkRelativeAndAdd(fBaseQuark, String.valueOf(count));
            count++;
        }
        fInUseQuarks.add(quark);
        return quark;
    }

    /**
     * Recycle a quark so that it can be reused by calling the
     * {@link #getAvailableQuark()} method. The quark has to have been obtained
     * from a previous call to {@link #getAvailableQuark()}. It will set the
     * quark's value in the state system to a null value.
     *
     * It is assumed that it will be reused in the same context each time, so
     * all children are kept and set to null in this method. The quarks are
     * still available for the caller, nothing prevents from re-using them
     * without referring to this class. That means if any attribute's value need
     * to be non-null after recycling the quark, the caller can do it after
     * calling this method.
     *
     * @param quark
     *            The quark to recycle.
     * @param ts
     *            The timestamp at which to close this attribute.
     */
    public synchronized void recycleQuark(Integer quark, long ts) {
        if (!fInUseQuarks.remove(quark)) {
            throw new IllegalArgumentException();
        }
        setQuarkToNull(quark, ts);
        try {
            for (Integer subQuark : fSs.getSubAttributes(quark, true)) {
                setQuarkToNull(subQuark, ts);
            }
        } catch (AttributeNotFoundException e) {
            Activator.logError("Error getting sub-attributes", e); //$NON-NLS-1$
        }
        fAvailableQuarks.add(quark);
    }

    private void setQuarkToNull(int quark, long ts) {
        try {
            fSs.modifyAttribute(ts, TmfStateValue.nullValue(), quark);
        } catch (StateValueTypeException | AttributeNotFoundException e) {
            Activator.logError("Error setting the values of an attribute to null", e); //$NON-NLS-1$
        }
    }

}
