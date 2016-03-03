/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.latency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;

import com.google.common.collect.ImmutableList;

/**
 * @since 2.0
 */
public class StateSystemSegmentStore implements ISegmentStore<ISegment> {

    private final ReadWriteLock fLock = new ReentrantReadWriteLock(false);
    private final ITmfStateSystem fStateSystem;
    private final List<Integer> fSegmentQuarks;

    Comparator<ITmfStateInterval> cmp = (x, y) -> {
        if (x.getStartTime() < y.getStartTime()) {
            return -1;
        }
        if (x.getEndTime() < y.getEndTime()) {
            return -1;
        }
        return 1;
    };


    public StateSystemSegmentStore(ITmfStateSystem ss) {
        fStateSystem = ss;
        List<Integer> quarks = Collections.EMPTY_LIST;
        try {
            quarks = ImmutableList.copyOf(ss.getSubAttributes(ITmfStateSystem.ROOT_ATTRIBUTE, false));
        } catch (AttributeNotFoundException e) {
            quarks = Collections.EMPTY_LIST;
            e.printStackTrace();
        }
        fSegmentQuarks = quarks;
    }

    @Override
    public boolean add(@NonNull ISegment arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(@Nullable Collection<? extends @NonNull ISegment> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {

    }

    @Override
    public boolean contains(@Nullable Object arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(@Nullable Collection<?> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Iterator<@NonNull ISegment> iterator() {
        return new SegmentIterator();
    }

    private class SegmentIterator implements Iterator<@NonNull ISegment> {



        private final long fEndTime;
        private final Queue<@NonNull ITmfStateInterval> fSegmentQueue = new PriorityQueue<>(fSegmentQuarks.size(), cmp);

        public SegmentIterator() {
            fEndTime = fStateSystem.getCurrentEndTime();
            long startTime = fStateSystem.getStartTime();
            /* For each quark, get the first non-null interval */
            for (Integer quark : fSegmentQuarks) {
                @Nullable ITmfStateInterval firstInterval = StateSystemUtils.queryUntilNonNullValue(fStateSystem, quark, startTime, fEndTime);
                if (firstInterval != null) {
                    fSegmentQueue.add(firstInterval);
                }
            }
        }

        @Override
        public boolean hasNext() {
            return !fSegmentQueue.isEmpty();
        }

        @Override
        public @NonNull ISegment next() {
            ITmfStateInterval interval = fSegmentQueue.poll();
            /* Get the next interval for this attribute */
            @Nullable ITmfStateInterval firstInterval = StateSystemUtils.queryUntilNonNullValue(fStateSystem, interval.getAttribute(), interval.getEndTime() + 1, fEndTime);
            if (firstInterval != null) {
                fSegmentQueue.add(firstInterval);
            }
            return new IntervalSegment(interval);
        }

    }

    @Override
    public boolean remove(@Nullable Object arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeAll(@Nullable Collection<?> arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean retainAll(@Nullable Collection<?> arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Object @NonNull [] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T @NonNull [] toArray(T @NonNull [] arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NonNull Iterable<@NonNull ISegment> getIntersectingElements(long position) {
        List<@NonNull ISegment> segments = new ArrayList<>();
        for (Integer quark : fSegmentQuarks) {
            try {
                ITmfStateInterval firstInterval = fStateSystem.querySingleState(position, quark);
                segments.add(new IntervalSegment(firstInterval));
            } catch (AttributeNotFoundException | StateSystemDisposedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return segments;
    }

    @Override
    public @NonNull Iterable<@NonNull ISegment> getIntersectingElements(long start, long end) {
        List<@NonNull ISegment> segments = new ArrayList<>();
        for (Integer quark : fSegmentQuarks) {
            try {
            List<ITmfStateInterval> intervals = StateSystemUtils.queryHistoryRange(fStateSystem, quark, start, end);

            segments.addAll(intervals.stream().map(v -> new IntervalSegment(v)).collect(Collectors.toSet()));
            } catch (AttributeNotFoundException | StateSystemDisposedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        return segments;
    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub

    }

}
