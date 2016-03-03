/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.statesystem.core.tests.backend.historytree;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.HTConfig;
import org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.HTInterval;
import org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.HistoryTree;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;
import org.eclipse.tracecompass.statesystem.core.tests.stubs.backend.HistoryTreeStub;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test writing and reading state values in the state history tree
 *
 * @author Geneviève Bastien
 */
public class HTValuesTest {

    /* Minimal allowed blocksize */
    private static final int BLOCK_SIZE = HistoryTree.TREE_HEADER_SIZE;
    private static final int NUM_CHILDREN  = 10;
    private static final int START = 10;
    private static final int END = 30;

    private File fTempFile;

    /**
     * Create the temporary file for this history tree
     */
    @Before
    public void setupTest() {
        try {
            fTempFile = File.createTempFile("tmpStateSystem", null);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Delete the temporary history tree file after the test
     */
    @After
    public void cleanup() {
        fTempFile.delete();
    }

    /**
     * Setup a history tree.
     *
     * @param maxChildren
     *            The max number of children per node in the tree (tree config
     *            option)
     */
    private HistoryTreeStub setupTree() {
        HistoryTreeStub ht = null;
        try {
            File newFile = fTempFile;
            assertNotNull(newFile);
            HTConfig config = new HTConfig(newFile,
                    BLOCK_SIZE,
                    NUM_CHILDREN, /* Number of children */
                    1, /* Provider version */
                    1); /* Start time */
            ht = new HistoryTreeStub(config);

        } catch (IOException e) {
            fail(e.getMessage());
        }

        assertNotNull(ht);
        return ht;
    }

    @Test
    public void testValueInt() {
        HistoryTree ht = setupTree();
        ht.insertInterval(new HTInterval(START, END, 1, TmfStateValue.newValueInt(3)));
        ht.closeTree(END);

    }
}
