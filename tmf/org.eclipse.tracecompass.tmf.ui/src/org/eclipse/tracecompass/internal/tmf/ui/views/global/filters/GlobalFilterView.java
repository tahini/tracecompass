/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ui.views.global.filters;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;

/**
 * @author Geneviève Bastien
 *
 */
public class GlobalFilterView extends TmfView {

    private @Nullable GlobalFilterViewer fViewer = null;

    public GlobalFilterView() {
        super("Global Filters View");
    }

    @Override
    public void setFocus() {
    	if (fViewer != null) {
    	    fViewer.setFocus();
    	}
    }

    @Override
    public void createPartControl(@Nullable Composite parent) {
        super.createPartControl(parent);

        if (parent == null) {
        	return;
        }
        fViewer = new GlobalFilterViewer(this, parent, SWT.NONE);

    }


}
