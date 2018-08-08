package org.eclipse.tracecompass.tmf.core.signal;

/**
 * Signal indicating a regex filter has been applied
 *
 * @author Jean-Christian Kouame
 * @since 4.1
 *
 */
public class TmfRegexFilterAppliedSignal extends TmfSignal {

    private final String fRegex;

    /**
     * Constructor for a new signal.
     *
     * @param source
     *            The object sending this signal
     * @param regex
     *            The applied regex filter or null
     */
    public TmfRegexFilterAppliedSignal(Object source, String regex) {
        super(source);
        fRegex = regex;
    }

    /**
     * Get the regex filter being applied
     *
     * @return The filter
     */
    public String getRegex() {
        return fRegex;
    }

    @Override
    public String toString() {
        return "[TmfEventFilterAppliedSignal (" + fRegex + ")]"; //$NON-NLS-1$ //$NON-NLS-2$
    }


}
