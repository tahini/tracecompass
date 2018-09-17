/*******************************************************************************
 * Copyright (c) 2012, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Mathieu Denis <mathieu.denis@polymtl.ca> - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.viewers;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.tmf.core.component.TmfComponent;
import org.eclipse.tracecompass.tmf.core.filter.TraceCompassFilter;

/**
 * Abstract class that extends {@link TmfComponent} to be specific to viewers.
 *
 * It allows the access to the control and the parent of a viewer.
 *
 * @author Mathieu Denis
 */
public abstract class TmfViewer extends TmfComponent implements ITmfViewer {

    /**
     * The parent composite that holds the viewer
     */
    protected Composite fParent;

    /**
     * Default constructor. The viewer have to be initialize through the
     * {@link TmfViewer#init(Composite, String)} function later on.
     */
    public TmfViewer() {
        super();
    }

    /**
     * Constructor that initializes the parent of the viewer
     *
     * @param parent
     *            The parent composite that holds this viewer
     *
     * @see TmfComponent#TmfComponent(String)
     */
    public TmfViewer(Composite parent) {
        this(parent, ""); //$NON-NLS-1$
    }

    /**
     * Constructor that initializes the parent of the viewer and that sets the
     * name of the viewer
     *
     * @param parent
     *            The parent composite that holds this viewer
     * @param name
     *            The name of the viewer
     */
    public TmfViewer(Composite parent, String name) {
        init(parent, name);
    }

    /**
     * Performs initialization of the viewer. It initializes the component. Need
     * to be called when the default constructor is used.
     *
     * @param parent
     *            The parent composite of the viewer
     * @param name
     *            The name to give to this viewer
     * @see TmfComponent#init(String)
     */
    public void init(Composite parent, String name) {
        super.init(name);
        fParent = parent;
    }

    /**
     * @return the parent of this viewer
     */
    public Composite getParent() {
        return fParent;
    }

    /**
     * A filter is being applied on this viewer. The default implementation for
     * this viewer is to do nothing. Child implementations can override this
     * method to act on searches or filters
     *
     * @param filter
     *            The filter to apply
     * @param isSearch
     *            Whether this filter is a the result of a search or a filter
     * @param remove
     *            Whether this filter is to be removed to added
     * @since 4.1
     */
    public void globalFilterApplied(TraceCompassFilter filter, boolean isSearch, boolean remove) {
        // Nothing by default, let children implementation implement this
    }
}
