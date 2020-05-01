/*******************************************************************************
 * Copyright (c) 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.dataprovider;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.format.DataSizeWithUnitFormat;
import org.eclipse.tracecompass.common.core.format.DataSpeedWithUnitFormat;
import org.eclipse.tracecompass.common.core.format.DecimalUnitFormat;
import org.eclipse.tracecompass.common.core.format.SubSecondTimeWithUnitFormat;

/**
 * Data Provider description, used to list the available providers for a trace
 * without triggering the analysis or creating the providers. Supplies
 * information such as the extension point ID, type of provider and help text.
 *
 * @author Loic Prieur-Drevon
 * @author Bernd Hufmann
 * @since 5.0
 */
@NonNullByDefault
public interface IDataProviderDescriptor {

    /**
     * The type of the data provider. The purpose of the type to indicate
     * to the clients the type of viewer to visualize or whether they can
     * share a common x-axis (e.g. time).
     *
     * The following types share common x-axis/time axis:
     *
     * {@link #TREE_TIME_XY}
     * {@link #TIME_GRAPH}
     *
     * @author Loic Prieur-Drevon
     * @author Bernd Hufmann
     */
    public enum ProviderType {
        /**
         * A provider for a table data structure implemented as virtual table.
         */
        TABLE,
        /**
         * A provider for a tree, whose entries have XY series. The x-series is time.
         */
        TREE_TIME_XY,
        /**
         * A provider for a Time Graph model, which has entries with a start and end
         * time, each entry has a series of states, arrows link from one series to
         * another
         */
        TIME_GRAPH
    }

    /**
     * The type of data that a value represents. Mostly for numeric value, as
     * the data type will help decide how to format the data to be displayed to
     * the user
     *
     * @since 6.0
     */
    public enum DataType {
        /**
         * Data represent a decimal number
         */
        NUMBER(new DecimalUnitFormat()),
        /**
         * Data represent a time in nanoseconds, can be negative
         */
        NANOSECONDS(SubSecondTimeWithUnitFormat.getInstance()),
        /**
         * Data represent a binary size, in bytes
         */
        BYTES(DataSizeWithUnitFormat.getInstance()),
        /**
         * Data represent a binary speed, in bytes/second
         */
        BINARY_SPEED(DataSpeedWithUnitFormat.getInstance()),
        /**
         * Any other type of data. Metric that use this data type may use
         * additional formatter.
         */
        OTHER(new Format() {

            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public StringBuffer format(@Nullable Object obj, @Nullable StringBuffer toAppendTo, @Nullable FieldPosition pos) {
                if (toAppendTo == null) {
                    return new StringBuffer(String.valueOf(obj));
                }
                return Objects.requireNonNull(toAppendTo.append(String.valueOf(obj)));
            }

            @Override
            public @Nullable Object parseObject(@Nullable String source, @Nullable ParsePosition pos) {
                return null;
            }

        });

        private Format fFormatter;

        private DataType(Format formatter) {
            fFormatter = formatter;
        }

        /**
         * Get the formatter for this data type
         *
         * @return The formatter
         */
        public Format getFormatter() {
            return fFormatter;
        }
    }

    /**
     * Gets the name of the data provide
     *
     * @return the name
     */
    String getName();

    /**
     * Getter for this data provider's ID.
     *
     * @return the ID for this data provider.
     */
    String getId();

    /**
     * Getter for this data provider's type
     *
     * @return this data provider's type
     */
    ProviderType getType();

    /**
     * Getter for the description of this data provider.
     *
     * @return a short description of this data provider.
     */
    String getDescription();

    /**
     * Getter for the type of data that this data provider providers. It applies
     * to providers of type {@link ProviderType#TREE_TIME_XY}, with typically
     * numerical series.
     *
     * @return The data format, or <code>null</code> if no data format is
     *         specified or does not apply
     * @since 6.0
     */
    default @Nullable DataType getDataType() {
        return null;
    }
}
