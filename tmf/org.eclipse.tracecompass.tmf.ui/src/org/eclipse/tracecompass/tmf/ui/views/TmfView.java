/*******************************************************************************
 * Copyright (c) 2009, 2017 Ericsson and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *   Bernd Hufmann - Added possibility to pin view
 *   Marc-Andre Laperle - Support for view alignment
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.views;

import java.text.MessageFormat;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener2;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.ITmfImageConstants;
import org.eclipse.tracecompass.internal.tmf.ui.Messages;
import org.eclipse.tracecompass.internal.tmf.ui.views.TimeAlignViewsAction;
import org.eclipse.tracecompass.internal.tmf.ui.views.TmfActiveFilterHeader;
import org.eclipse.tracecompass.internal.tmf.ui.views.TmfActiveFilterHeader.IActiveFilterHeaderListener;
import org.eclipse.tracecompass.internal.tmf.ui.views.TmfAlignmentSynchronizer;
import org.eclipse.tracecompass.tmf.core.component.ITmfComponent;
import org.eclipse.tracecompass.tmf.core.filter.TraceCompassFilter;
import org.eclipse.tracecompass.tmf.core.signal.TmfEventFilterAppliedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfEventSearchAppliedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

/**
 * Basic abstract TMF view class implementation. <br>
 * It registers any sub class to the signal manager for receiving and sending
 * TMF signals. <br>
 * Subclasses may optionally implement the {@link ITmfTimeAligned},
 * {@link ITmfAllowMultiple} and {@link ITmfPinnable} interfaces to enable those
 * features.
 *
 * @author Francois Chouinard
 */
public abstract class TmfView extends ViewPart implements ITmfComponent {

    private static final String ACT_ON_FILTER = "global.filter"; //$NON-NLS-1$
    private static final String ACT_ON_SEARCH = "global.search"; //$NON-NLS-1$
    private static final TmfAlignmentSynchronizer TIME_ALIGNMENT_SYNCHRONIZER = TmfAlignmentSynchronizer.getInstance();
    private final String fName;
    /** This allows us to keep track of the view sizes */
    private Composite fParentComposite;
    private ControlAdapter fControlListener;

    private boolean fActOnSearch = false;
    private boolean fActOnFilter = false;

    /**
     * Action class for pinning of TmfView.
     */
    protected PinTmfViewAction fPinAction;

    private static TimeAlignViewsAction fAlignViewsAction;

    /** The save action */
    private IAction fSaveAction;
    private TmfActiveFilterHeader fHeaderBar;

    /**
     * The separator used between the primary and secondary id of a view id.
     *
     * @since 3.2
     */
    public static final String VIEW_ID_SEPARATOR = ":"; //$NON-NLS-1$

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Constructor. Creates a TMF view and registers to the signal manager.
     *
     * @param viewName
     *            A view name
     */
    public TmfView(String viewName) {
        super();
        fName = viewName;
        TmfSignalManager.register(this);
    }

    /**
     * Disposes this view and de-registers itself from the signal manager
     */
    @Override
    public void dispose() {
        TmfSignalManager.deregister(this);

        /* Workaround for Bug 490400: Clear the action bars */
        IActionBars bars = getViewSite().getActionBars();
        bars.getToolBarManager().removeAll();
        bars.getMenuManager().removeAll();

        super.dispose();
    }

    // ------------------------------------------------------------------------
    // ITmfComponent
    // ------------------------------------------------------------------------

    @Override
    public String getName() {
        return fName;
    }

    @Override
    public void broadcast(TmfSignal signal) {
        TmfSignalManager.dispatchSignal(signal);
    }

    @Override
    public void broadcastAsync(TmfSignal signal) {
        TmfSignalManager.dispatchSignalAsync(signal);
    }

    // ------------------------------------------------------------------------
    // View pinning support
    // ------------------------------------------------------------------------

    /**
     * Returns whether the view is pinned.
     *
     * @return if the view is pinned
     */
    public boolean isPinned() {
        return ((fPinAction != null) && (fPinAction.isPinned()));
    }

    /**
     * Method adds a pin action to the TmfView. For example, this action can be
     * used to ignore time synchronization signals from other TmfViews. <br>
     *
     * Uses {@link ITmfPinnable#setPinned(ITmfTrace)} to propagate the state of
     * the action button.
     */
    protected void contributePinActionToToolBar() {
        if (fPinAction == null && this instanceof ITmfPinnable) {
            fPinAction = new PinTmfViewAction((ITmfPinnable) this);

            IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
            toolBarManager.add(new Separator(IWorkbenchActionConstants.PIN_GROUP));
            toolBarManager.add(fPinAction);
        }
    }

    @Override
    public void createPartControl(final Composite parent) {
        fParentComposite = parent;
        IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();

        if (this instanceof ITmfAllowMultiple) {
            contributeNewViewActionToLocalMenu(menuManager);
        }
        IAction saveAction = fSaveAction;
        if (saveAction == null) {
            saveAction = createSaveAction();
            fSaveAction = saveAction;
        }
        if (saveAction != null) {
            menuManager.add(new Separator());
            menuManager.add(saveAction);
            menuManager.add(new Separator());
        }
        if (this instanceof ITmfTimeAligned) {
            contributeAlignViewsActionToLocalMenu(menuManager);

            fControlListener = new ControlAdapter() {
                @Override
                public void controlResized(ControlEvent e) {
                    /*
                     * When switching perspective, the view can be resized just
                     * before it is made visible. Queue the time alignment to
                     * ensure it occurs when the parent composite is visible.
                     */
                    e.display.asyncExec(() -> {
                        TIME_ALIGNMENT_SYNCHRONIZER.handleViewResized(TmfView.this);
                    });
                }
            };
            parent.addControlListener(fControlListener);

            getSite().getPage().addPartListener(new IPartListener() {
                @Override
                public void partOpened(IWorkbenchPart part) {
                    // do nothing
                }

                @Override
                public void partDeactivated(IWorkbenchPart part) {
                    // do nothing
                }

                @Override
                public void partClosed(IWorkbenchPart part) {
                    if (part == TmfView.this && fControlListener != null && !fParentComposite.isDisposed()) {
                        fParentComposite.removeControlListener(fControlListener);
                        fControlListener = null;
                        getSite().getPage().removePartListener(this);
                        TIME_ALIGNMENT_SYNCHRONIZER.handleViewClosed(TmfView.this);
                    }
                }

                @Override
                public void partBroughtToTop(IWorkbenchPart part) {
                    // do nothing
                }

                @Override
                public void partActivated(IWorkbenchPart part) {
                    // do nothing
                }
            });
        }

        if (!menuManager.isEmpty()) {
            menuManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        }

        IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
        toolBarManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        /* Subclass tool bar contributions should be added to this group */

        if (this instanceof ITmfPinnable) {
            contributePinActionToToolBar();
        }

        // Add common menus for views that respond to filters and searches
        if (respondToFilter()) {
            // Build the menu
            IDialogSettings settings = Activator.getDefault().getDialogSettings();
            IDialogSettings section = settings.getSection(getClass().getName());
            if (section == null) {
                section = settings.addNewSection(getClass().getName());
            }
            contributeFilterActionsToMenuManager(menuManager, section);

            // Create an events table header bar
            GridLayout gl = new GridLayout(1, false);
            gl.marginHeight = 0;
            gl.marginWidth = 0;
            parent.setLayout(gl);

            fHeaderBar = new TmfActiveFilterHeader(parent, SWT.NONE, new IActiveFilterHeaderListener() {
                @Override
                public void filterSelected(TraceCompassFilter filter) {
                    // No action to take
                }

                @Override
                public void filterRemoved(TraceCompassFilter filter) {
                    removeFilter(filter);
                }

            });
        }

    }

    /**
     * Get the save action
     *
     * @return the save action
     * @since 3.3
     */
    protected @Nullable IAction createSaveAction() {
        return null;
    }

    /**
     * Add the "New view" action to the view menu. This action spawns a new view
     * of the same type as the caller.
     */
    private void contributeNewViewActionToLocalMenu(IMenuManager menuManager) {
        if (!menuManager.isEmpty()) {
            menuManager.add(new Separator());
        }
        if (this instanceof ITmfPinnable) {
            MenuManager newViewMenu = new MenuManager(MessageFormat.format(Messages.TmfView_NewViewActionText, this.getTitle()));
            newViewMenu.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath(ITmfImageConstants.IMG_UI_NEW_VIEW));
            newViewMenu.setRemoveAllWhenShown(true);
            newViewMenu.addMenuListener(new IMenuListener2() {
                @Override
                public void menuAboutToShow(IMenuManager mgr) {
                    Set<@NonNull ITmfTrace> openedTraces = TmfTraceManager.getInstance().getOpenedTraces();
                    ITmfTrace activeTrace = ((ITmfPinnable) TmfView.this).getTrace();
                    if (activeTrace != null) {
                        mgr.add(new NewTmfViewAction(TmfView.this, activeTrace, true));
                        mgr.add(new Separator());
                    }
                    for (ITmfTrace trace : openedTraces) {
                        mgr.add(new NewTmfViewAction(TmfView.this, trace));
                    }
                    mgr.add(new Separator());
                    mgr.add(new NewTmfViewAction(TmfView.this, null));
                }

                @Override
                public void menuAboutToHide(IMenuManager manager) {
                    /*
                     * Clear menu to release action references to trace
                     * instances
                     */
                    manager.removeAll();
                }
            });
            menuManager.add(newViewMenu);
        } else {
            NewTmfViewAction action = new NewTmfViewAction(TmfView.this);
            menuManager.add(action);
        }
    }

    private static void contributeAlignViewsActionToLocalMenu(IMenuManager menuManager) {
        if (fAlignViewsAction == null) {
            fAlignViewsAction = new TimeAlignViewsAction();
        }
        if (!menuManager.isEmpty()) {
            menuManager.add(new Separator());
        }
        menuManager.add(fAlignViewsAction);
    }

    /**
     * Returns the parent control of the view
     *
     * @return the parent control
     *
     * @since 1.0
     */
    public Composite getParentComposite() {
        return fParentComposite;
    }

    /**
     * Return the Eclipse view ID in the format 'Primary ID':'Secondary ID' or
     * simply 'Primary ID' if secondary ID is null
     *
     * @return This view's view ID
     * @since 2.2
     */
    protected @NonNull String getViewId() {
        IViewSite viewSite = getViewSite();
        String secondaryId = viewSite.getSecondaryId();
        if (secondaryId == null) {
            return String.valueOf(viewSite.getId());
        }
        return viewSite.getId() + VIEW_ID_SEPARATOR + secondaryId;
    }

    /**
     * Return whether this view is expected to respond to global filters and
     * searches. Each view has the responsibility to determine how they will
     * respond, but the main class will create a menu for users to
     * activate/deactivate the filtering/search and prepare the zone where the
     * filters and searches can be individually managed in each view.
     * <p>
     * By default, views will not respond.
     * <p>
     * A class overriding this method should also override the
     * {@link #globalFilterApplied(TraceCompassFilter, boolean, boolean)} method
     * as this is where the behavior will be implemented. Also, it is advised to
     * override the {@link #defaultResponseToFilter()} and
     * {@link #defaultResponseToSearch()} methods to define the default behavior
     * on filter and searches respectively.
     *
     * @return <code>true</code> if the view is expected to act on filter and
     *         search applied signals, <code>false</code> otherwise
     * @since 4.1
     */
    protected boolean respondToFilter() {
        return false;
    }

    /**
     * Define what should be the default response to global filters for this
     * view. This method has effect only if {@link #respondToFilter()} is true
     * and this default behavior can be overridden by users.
     *
     * @return <code>true</code> if the view should respond to the global filter
     *         signal by default.
     * @since 4.1
     */
    protected boolean defaultResponseToFilter() {
        return true;
    }

    /**
     * Define what should be the default response to global searches for this
     * view. This method has effect only if {@link #respondToFilter()} is true
     * and this default behavior can be overridden by users
     *
     * @return <code>true</code> if the view should respond to the global search
     *         signal by default.
     * @since 4.1
     */
    protected boolean defaultResponseToSearch() {
        return false;
    }

    /**
     * Set or remove the global regex filter value, after a global filter signal
     * has been broadcasted
     *
     * @param signal
     *            the signal carrying the regex value
     * @since 4.1
     */
    @TmfSignalHandler
    public final void regexFilterApplied(TmfEventFilterAppliedSignal signal) {
        if (respondToFilter() && fActOnFilter) {
            globalFilterApplied(signal.getFilter(), false, false);
            fHeaderBar.clearFilters();
            fHeaderBar.addFilter(signal.getFilter());
        }
    }

    /**
     * Set or remove the global regex filter value, after a global search signal
     * has been broadcasted
     *
     * @param signal
     *            the signal carrying the regex value
     * @since 4.1
     */
    @TmfSignalHandler
    public final void regexSearchApplied(TmfEventSearchAppliedSignal signal) {
        if (respondToFilter() && fActOnSearch) {
            globalFilterApplied(signal.getFilter(), true, false);
            fHeaderBar.clearFilters();
            fHeaderBar.addFilter(signal.getFilter());
        }
    }

    /**
     * A global filter or search has been broadcasted. The view may act upon
     * this filter. This method will only be called if
     * {@link #respondToFilter()} return <code>true</code>. The default
     * implementation is to do nothing
     *
     * @param filter
     *            The filter that is applied
     * @param isSearch
     *            Whether this filter comes from a search or filter signal.
     *            Views may act differently in each case
     * @param remove
     *            Whether to remove this filter or apply it
     * @since 4.1
     */
    protected void globalFilterApplied(@NonNull TraceCompassFilter filter, boolean isSearch, boolean remove) {
        // Let the views implement their behavior
    }

    private void contributeFilterActionsToMenuManager(IMenuManager menuManager, IDialogSettings section) {
        MenuManager filterMenuManager = new MenuManager(Messages.TmfView_MenuFilters);

        String value = section.get(ACT_ON_SEARCH);
        boolean performAction = (value == null) ? defaultResponseToSearch() : section.getBoolean(ACT_ON_SEARCH);
        fActOnSearch = performAction;
        Action actOnSearchAction = new Action(Messages.TmfView_MenuFiltersActOnSearch, IAction.AS_CHECK_BOX) {
            @Override
            public void run() {
                boolean search = isChecked();
                fActOnSearch = search;
                section.put(ACT_ON_SEARCH, search);
            }
        };
        actOnSearchAction.setChecked(performAction);
        filterMenuManager.add(actOnSearchAction);

        value = section.get(ACT_ON_FILTER);
        performAction = (value == null) ? defaultResponseToFilter() : section.getBoolean(ACT_ON_FILTER);
        fActOnFilter = performAction;
        Action actOnFilter = new Action(Messages.TmfView_MenuFiltersActOnFilter, IAction.AS_CHECK_BOX) {
            @Override
            public void run() {
                boolean filter = isChecked();
                fActOnFilter = filter;
                section.put(ACT_ON_FILTER, filter);
            }
        };
        actOnFilter.setChecked(true);
        filterMenuManager.add(actOnFilter);
        menuManager.add(filterMenuManager);
    }

    private void removeFilter(TraceCompassFilter filter) {
        globalFilterApplied(filter, true, true);
        fHeaderBar.removeFilter(filter);
    }

    /**
     * @return
     * @since 4.1
     */
    protected final static GridData getFillGridData() {
        return GridDataFactory.fillDefaults().grab(true, true).create();
    }

}
