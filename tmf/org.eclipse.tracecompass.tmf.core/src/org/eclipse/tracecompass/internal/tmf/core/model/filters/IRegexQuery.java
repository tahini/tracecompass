package org.eclipse.tracecompass.internal.tmf.core.model.filters;

import org.eclipse.jdt.annotation.NonNull;

import com.google.common.collect.Multimap;

/**
 * Interface for query filter with a regex predicate
 *
 * @author Jean-Christian
 *
 */
public interface IRegexQuery {
    /**
     * Get the regexes use to filter the queried data. It is a multimap of filter
     * strings by property. The data provider will use the filter strings to
     * determine whether the property should be activated or not.
     *
     * @return The multimap of regexes by property.
     */
    public Multimap<@NonNull Integer, @NonNull String> getRegexes();
}
