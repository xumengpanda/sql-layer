/**
 * Copyright (C) 2011 Akiban Technologies Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package com.akiban.server;

import com.akiban.server.error.PersistitAdapterException;
import com.akiban.server.rowdata.IndexDef;
import com.akiban.server.rowdata.RowDef;
import com.akiban.server.service.tree.TreeService;
import com.persistit.Tree;
import com.persistit.exception.PersistitException;
import com.persistit.exception.PersistitInterruptedException;

import java.util.HashMap;
import java.util.Map;

public class PersistitAccumulatorTableStatusCache implements TableStatusCache {
    private Map<Integer, InternalTableStatus> tableStatusMap = new HashMap<Integer, InternalTableStatus>();
    private TreeService treeService;

    public PersistitAccumulatorTableStatusCache(TreeService treeService) {
        this.treeService = treeService;
    }

    @Override
    public synchronized void rowDeleted(int tableID) {
        getInternalTableStatus(tableID).rowDeleted();
    }

    @Override
    public synchronized void rowWritten(int tableID) {
        getInternalTableStatus(tableID).rowWritten();
    }

    @Override
    public synchronized void truncate(int tableID) throws PersistitInterruptedException {
        getInternalTableStatus(tableID).truncate();
    }

    @Override
    public synchronized void drop(int tableID) throws PersistitInterruptedException {
        InternalTableStatus ts = tableStatusMap.remove(tableID);
        if(ts != null) {
            ts.setAutoIncrement(0, true);
            ts.setRowCount(0);
            ts.setOrdinal(0);
            ts.setRowDef(null, null);
        }
    }

    @Override
    public synchronized void setAutoIncrement(int tableID, long value) throws PersistitInterruptedException {
        getInternalTableStatus(tableID).setAutoIncrement(value, false);
    }

    @Override
    public synchronized void setOrdinal(int tableID, int value) throws PersistitInterruptedException {
        getInternalTableStatus(tableID).setOrdinal(value);
    }

    @Override
    public synchronized void setRowDef(int tableID, RowDef rowDef) {
        InternalTableStatus ts = tableStatusMap.get(tableID);
        if(ts == null) {
            throw new IllegalArgumentException("Unknown table ID " + tableID + " for RowDef " + rowDef);
        }
        Tree tree = getTreeForRowDef(rowDef);
        ts.setRowDef(rowDef, tree);
    }

    @Override
    public synchronized long createNewUniqueID(int tableID) throws PersistitInterruptedException {
        return getInternalTableStatus(tableID).createNewUniqueID();
    }

    @Override
    public synchronized TableStatus getTableStatus(int tableID) {
        return getInternalTableStatus(tableID);
    }

    @Override
    public synchronized void loadAllInVolume(String volumeName) throws Exception {
        // Nothing to do
    }

    @Override
    public synchronized void detachAIS() {
        for(InternalTableStatus ts : tableStatusMap.values()) {
            ts.setRowDef(null, null);
        }
    }
    
    //
    // Internal
    //
    
    private synchronized InternalTableStatus getInternalTableStatus(int tableID) {
        InternalTableStatus ts = tableStatusMap.get(tableID);
        if(ts == null) {
            ts = new InternalTableStatus();
            tableStatusMap.put(tableID, ts);
        }
        return ts;
    }
    
    private Tree getTreeForRowDef(RowDef rowDef) {
        IndexDef indexDef = (IndexDef) rowDef.getPKIndex().indexDef();
        assert indexDef != null : rowDef;
        try {
            treeService.populateTreeCache(indexDef);
            return indexDef.getTreeCache().getTree();
        }
        catch (PersistitException e) {
            throw new PersistitAdapterException(e);
        }
    }

    private class InternalTableStatus implements TableStatus {
        private volatile RowDef rowDef;
        private volatile AccumulatorAdapter ordinal;
        private volatile AccumulatorAdapter rowCount;
        private volatile AccumulatorAdapter uniqueID;
        private volatile AccumulatorAdapter autoIncrement;

        @Override
        public long getAutoIncrement() throws PersistitInterruptedException {
            return autoIncrement.getSnapshot();
        }

        @Override
        public int getOrdinal() throws PersistitInterruptedException {
            return (int) ordinal.getSnapshot();
        }

        @Override
        public long getRowCount() throws PersistitInterruptedException {
            return rowCount.getSnapshot();
        }

        @Override
        public long getUniqueID() throws PersistitInterruptedException {
            return uniqueID.getSnapshot();
        }

        @Override
        public RowDef getRowDef() {
            return rowDef;
        }

        void rowDeleted() {
            rowCount.updateAndGet(-1);
        }

        void rowWritten() {
            rowCount.updateAndGet(1);
        }

        void setRowCount(long rowCountValue) throws PersistitInterruptedException {
            rowCount.set(rowCountValue);
        }

        void setAutoIncrement(long autoIncrementValue, boolean evenIfLess) throws PersistitInterruptedException {
            autoIncrement.set(autoIncrementValue, evenIfLess);
        }

        void setOrdinal(int ordinalValue) throws PersistitInterruptedException {
            ordinal.set(ordinalValue);
        }

        synchronized void setRowDef(RowDef rowDef, Tree tree) {
            this.rowDef = rowDef;
            if(rowDef == null && tree == null) {
                return;
            }
            ordinal = new AccumulatorAdapter(AccumulatorAdapter.AccumInfo.ORDINAL, treeService, tree);
            rowCount = new AccumulatorAdapter(AccumulatorAdapter.AccumInfo.ROW_COUNT, treeService, tree);
            uniqueID = new AccumulatorAdapter(AccumulatorAdapter.AccumInfo.UNIQUE_ID, treeService, tree);
            autoIncrement = new AccumulatorAdapter(AccumulatorAdapter.AccumInfo.AUTO_INC, treeService, tree);
        }
        
        long createNewUniqueID() throws PersistitInterruptedException {
            return uniqueID.updateAndGet(1);
        }

        void truncate() throws PersistitInterruptedException {
            setRowCount(0);
            setAutoIncrement(0, true);
        }
    }
}