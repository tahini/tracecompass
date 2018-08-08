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
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tracecompass.tmf.core.component.ITmfComponent;
import org.eclipse.tracecompass.tmf.core.signal.TmfRegexFilterAppliedSignal;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class GlobalFilterViewer extends Composite {

    private Text fFilterText;
    private final ITmfComponent fComponent;

    public GlobalFilterViewer(ITmfComponent component, Composite parent, int style) {
        super(parent, style);
        fComponent = component;
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 0;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        container.setLayout(layout);

        Composite labels = createCLabelsArea(container);
        Text text = createFilterTextArea(container, labels);
        fFilterText = text;
    }

    private static Composite createCLabelsArea(Composite container) {
        Composite labels = new Composite(container, SWT.NONE);
        GridData gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        labels.setLayoutData(gd);
        RowLayout rl = new RowLayout(SWT.HORIZONTAL);
        rl.marginTop = 0;
        rl.marginBottom = 0;
        rl.marginLeft = 0;
        rl.marginRight = 0;
        labels.setLayout(rl);
        return labels;
    }

    private Text createFilterTextArea(Composite container, Composite labels) {
        Text filterText = new Text(container, SWT.BORDER | SWT.SEARCH | SWT.ICON_CANCEL | SWT.ICON_SEARCH);
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        filterText.setLayoutData(gridData);
//        if (fRegex != null) {
//            filterText.setText(fRegex);
//        }
        Color background = filterText.getBackground();
        filterText.addModifyListener(e -> {
//            fRegex = filterText.getText();
            filterText.setBackground(background);
//            fView.restartZoomThread();
        });

        filterText.addKeyListener(new KeyListener() {

            @Override
            public void keyReleased(@Nullable KeyEvent e) {
                // Do nothing
            }

            @Override
            public void keyPressed(@Nullable KeyEvent e) {
            	if (e == null) {
            		return;
            	}
                if (e.character == SWT.CR) {
                    saveFilter(container, labels, filterText.getText());
                }
            }

        });
        return filterText;
    }

    private void saveFilter(Composite parent, Composite labels, String text) {
        if (text.isEmpty()) {
            return;
        }
        createCLabels(parent, labels, text);
        fComponent.broadcast(new TmfRegexFilterAppliedSignal(this, text));
    }

    private void createCLabels(Composite parent, Composite labels, String currentRegex) {
        CLabel filter = new CLabel(labels, SWT.NONE);
        filter.setText(currentRegex);
        filter.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));
        filter.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_YELLOW));
        filter.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(@Nullable MouseEvent e2) {
            	if (e2 == null) {
            		return;
            	}
                deleteCLabel(parent, filter, e2);
            }
        });
        parent.layout();

//        Rectangle bounds = parent.getShell().getBounds();
//        Point size = parent.computeSize(SWT.DEFAULT, SWT.DEFAULT);
//        Rectangle trim = parent.getShell().computeTrim(0, 0, size.x, size.y);
//        parent.getShell().setBounds(bounds.x + bounds.width - trim.width, bounds.y + bounds.height - trim.height, trim.width, trim.height);
    }

    private void deleteCLabel(Composite parent, CLabel cLabel, MouseEvent e) {
        Rectangle imageBounds;
        imageBounds = cLabel.getImage().getBounds();
        imageBounds.x += cLabel.getLeftMargin();
        imageBounds.y = (cLabel.getSize().y - imageBounds.height) / 2;
        if (imageBounds.contains(e.x, e.y)) {
//            fFilterRegexes.removeIf(regex -> regex.equals(cLabel.getText()));
            e.widget.dispose();
            parent.layout();
//            Rectangle bounds = parent.getShell().getBounds();
//            Point size = parent.computeSize(SWT.DEFAULT, SWT.DEFAULT);
//            Rectangle trim = parent.getShell().computeTrim(0, 0, size.x, size.y);
//            int x = bounds.x + (bounds.width - trim.width);
//            Display.getDefault().asyncExec(() -> {
//                parent.getShell().setLocation(x, bounds.y);
//            });
//            fView.restartZoomThread();
        }
        fComponent.broadcast(new TmfRegexFilterAppliedSignal(this, "")); //$NON-NLS-1$
    }

    @Override
    public boolean setFocus() {
        return fFilterText.setFocus();
    }

}
