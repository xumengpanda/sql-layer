/**
 * Copyright (C) 2009-2013 Akiban Technologies, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.foundationdb.ais.model;

import com.foundationdb.qp.memoryadapter.MemoryTableFactory;
import com.foundationdb.util.ArgumentValidation;

import java.util.*;

public class UserTable extends Table
{
    public static UserTable create(AkibanInformationSchema ais,
                                   String schemaName,
                                   String tableName,
                                   Integer tableId)
    {
        UserTable userTable = new UserTable(ais, schemaName, tableName, tableId);
        ais.addUserTable(userTable);
        return userTable;
    }

    /**
     * Create an independent copy of an existing UserTable.
     * @param ais Destination AkibanInformationSchema.
     * @param userTable UserTable to copy.
     * @return The new copy of the UserTable.
     */
    public static UserTable create(AkibanInformationSchema ais, UserTable userTable) {
        UserTable copy = create(ais, userTable.tableName.getSchemaName(), userTable.tableName.getTableName(), userTable.getTableId());
        copy.setUuid(userTable.getUuid());
        return copy;
    }

    private UserTable(AkibanInformationSchema ais, String schemaName, String tableName, Integer tableId)
    {
        super(ais, schemaName, tableName, tableId);
        this.fullTextIndexes = new HashSet<>();
        this.unmodifiableFullTextIndexes = Collections.unmodifiableCollection(fullTextIndexes);
    }

    @Override
    public boolean isUserTable()
    {
        return true;
    }

    @Override
    protected void addIndex(TableIndex index)
    {
        super.addIndex(index);
        if (index.isPrimaryKey()) {
            assert primaryKey == null;
            primaryKey = new PrimaryKey(index);
        }
    }

    @Override
    public void dropColumns() {
        columnsStale = true;
        super.dropColumns();
    }

    @Override
    public void removeIndexes(Collection<TableIndex> indexesToDrop) {
        if((primaryKey != null) && indexesToDrop.contains(primaryKey.getIndex())) {
            primaryKey = null;
        }
        super.removeIndexes(indexesToDrop);
    }
    
    /**
    * Returns the columns in this table that are constrained to match the given column, e.g.
     * customer.cid and order.cid. These will be ordered by the table they appear on, root to leaf.
     * The given column will itself be in the resulting list. The list is calculated anew each time
     * and may be modified as needed by the caller.
     * @param column the column for which to find matching columns.
     * @return a new list of columns equivalent to the given column, including that column itself.
     */
    List<Column> matchingColumns(Column column)
    {
        // TODO: make this a AISValidation check
        ArgumentValidation.isTrue(column + " doesn't belong to " + getName(), column.getTable() == this);
        List<Column> matchingColumns = new ArrayList<>();
        matchingColumns.add(column);
        findMatchingAncestorColumns(column, matchingColumns);
        findMatchingDescendantColumns(column, matchingColumns);
        Collections.sort(matchingColumns, COLUMNS_BY_TABLE_DEPTH);
        return matchingColumns;
    }

    private void findMatchingAncestorColumns(Column fromColumn, List<Column> matchingColumns)
    {
        Join join = ((UserTable)fromColumn.getTable()).getParentJoin();
        if (join != null) {
            JoinColumn ancestorJoinColumn = null;
            for (JoinColumn joinColumn : join.getJoinColumns()) {
                if (joinColumn.getChild() == fromColumn) {
                    ancestorJoinColumn = joinColumn;
                }
            }
            if (ancestorJoinColumn != null) {
                Column ancestorColumn = ancestorJoinColumn.getParent();
                matchingColumns.add(ancestorColumn);
                findMatchingAncestorColumns(ancestorJoinColumn.getParent(), matchingColumns);
            }
        }
    }

    private void findMatchingDescendantColumns(Column fromColumn, List<Column> matchingColumns)
    {
        for (Join join : getChildJoins()) {
            JoinColumn descendantJoinColumn = null;
            for (JoinColumn joinColumn : join.getJoinColumns()) {
                if (joinColumn.getParent() == fromColumn) {
                    descendantJoinColumn = joinColumn;
                }
            }
            if (descendantJoinColumn != null) {
                Column descendantColumn = descendantJoinColumn.getChild();
                matchingColumns.add(descendantColumn);
                join.getChild().findMatchingDescendantColumns(descendantJoinColumn.getChild(), matchingColumns);
            }
        }
    }

    public void addCandidateParentJoin(Join parentJoin)
    {
        candidateParentJoins.add(parentJoin);
    }

    public void addCandidateChildJoin(Join childJoin)
    {
        candidateChildJoins.add(childJoin);
    }

    public void removeCandidateParentJoin(Join parentJoin)
    {
        candidateParentJoins.remove(parentJoin);
    }

    public void removeCandidateChildJoin(Join childJoin)
    {
        candidateChildJoins.remove(childJoin);
    }

    public List<Join> getCandidateParentJoins()
    {
        return Collections.unmodifiableList(candidateParentJoins);
    }

    public List<Join> getCandidateChildJoins()
    {
        return Collections.unmodifiableList(candidateChildJoins);
    }

    public boolean hasChildren() {
        return !getCandidateChildJoins().isEmpty();
    }

    public Join getParentJoin()
    {
        Join parentJoin = null;
        Group group = getGroup();
        if (group != null) {
            for (Join candidateParentJoin : candidateParentJoins) {
                if (candidateParentJoin.getGroup() == group) {
                    parentJoin = candidateParentJoin;
                }
            }
        }
        return parentJoin;
    }

    public List<Join> getChildJoins()
    {
        List<Join> childJoins = new ArrayList<>();
        Group group = getGroup();
        if (group != null) {
            for (Join candidateChildJoin : candidateChildJoins) {
                if (candidateChildJoin.getGroup() == group) {
                    childJoins.add(candidateChildJoin);
                }
            }
        }
        return childJoins;
    }

    public Column getAutoIncrementColumn()
    {
        Column autoIncrementColumn = null;
        for (Column column : getColumns()) {
            if (column.getInitialAutoIncrementValue() != null) {
                autoIncrementColumn = column;
            }
        }
        return autoIncrementColumn;
    }
    
    public Column getIdentityColumn() 
    {
        Column identity = null;
        for (Column column : getColumns()) {
            if (column.getIdentityGenerator() != null) {
                identity = column;
            }
        }
        return identity;
    }

    @Override
    public Collection<TableIndex> getIndexes()
    {
        Collection<TableIndex> indexes = super.getIndexes();
        return removeInternalColumnIndexes(indexes);
    }

    public Collection<TableIndex> getIndexesIncludingInternal()
    {
        return super.getIndexes();
    }

    @Override
    public TableIndex getIndex(String indexName)
    {
        TableIndex index = null;
        if (indexName.equals(Index.PRIMARY_KEY_CONSTRAINT)) {
            // getPrimaryKey has logic for handling hidden PK
            PrimaryKey primaryKey = getPrimaryKey();
            index = primaryKey == null ? null : primaryKey.getIndex();
        } else {
            index = super.getIndex(indexName);
        }
        return index;
    }

    public boolean isDescendantOf(UserTable other) {
        if (getGroup() == null || !getGroup().equals(other.getGroup())) {
            return false;
        }
        UserTable possibleDescendant = this;
        while (possibleDescendant != null) {
            if (possibleDescendant.equals(other)) {
                return true;
            }
            possibleDescendant = possibleDescendant.parentTable();
        }
        return false;
    }

    public Index getIndexIncludingInternal(String indexName)
    {
        return super.getIndex(indexName);
    }

    @Override
    public void traversePreOrder(Visitor visitor)
    {
        for (Column column : getColumns()) {
            visitor.visitColumn(column);
        }
        for (Index index : getIndexes()) {
            visitor.visitIndex(index);
            index.traversePreOrder(visitor);
        }
    }

    @Override
    public void traversePostOrder(Visitor visitor)
    {
        for (Column column : getColumns()) {
            visitor.visitColumn(column);
        }
        for (Index index : getIndexes()) {
            index.traversePostOrder(visitor);
            visitor.visitIndex(index);
        }
    }

    public void traverseTableAndDescendants(Visitor visitor) {
        List<UserTable> remainingTables = new ArrayList<>();
        List<Join> remainingJoins = new ArrayList<>();
        remainingTables.add(this);
        remainingJoins.addAll(getCandidateChildJoins());
        // Add before visit in-case visitor changes group or joins
        while(!remainingJoins.isEmpty()) {
            Join join = remainingJoins.remove(remainingJoins.size() - 1);
            UserTable child = join.getChild();
            remainingTables.add(child);
            remainingJoins.addAll(child.getCandidateChildJoins());
        }
        for(UserTable table : remainingTables) {
            visitor.visitUserTable(table);
        }
    }

    public void setInitialAutoIncrementValue(Long initialAutoIncrementValue)
    {
        for (Column column : getColumns()) {
            if (column.getInitialAutoIncrementValue() != null) {
                column.setInitialAutoIncrementValue(initialAutoIncrementValue);
            }
        }
    }

    public synchronized PrimaryKey getPrimaryKey()
    {
        PrimaryKey declaredPrimaryKey = primaryKey;
        if (declaredPrimaryKey != null) {
            
            // TODO: This could be replace by a call to PrimaryKey#isAkibanPK()
            // But there is some dependecy here which causes the tests to fail if you do so.
            List<IndexColumn> pkColumns = primaryKey.getIndex().getKeyColumns();
            if (pkColumns.size() == 1 && pkColumns.get(0).getColumn().isAkibanPKColumn()) {
                declaredPrimaryKey = null;
            }
        }
        return declaredPrimaryKey;
    }

    public synchronized PrimaryKey getPrimaryKeyIncludingInternal()
    {
        return primaryKey;
    }

    public synchronized void endTable(NameGenerator generator)
    {
        // Creates a PK for a pk-less table.
        if (primaryKey == null) {
            // Find primary key index
            TableIndex primaryKeyIndex = null;
            for (TableIndex index : getIndexesIncludingInternal()) {
                if (index.isPrimaryKey()) {
                    primaryKeyIndex = index;
                }
            }
            if (primaryKeyIndex == null) {
                final int rootID;
                if(group == null) {
                    rootID = getTableId();
                } else {
                    assert group.getRoot() != null : "Null root: " + group;
                    rootID = group.getRoot().getTableId();
                }
                primaryKeyIndex = createAkibanPrimaryKeyIndex(generator.generateIndexID(rootID));
            }
            assert primaryKeyIndex != null : this;
            primaryKey = new PrimaryKey(primaryKeyIndex);
        }
        
        // Put the columns into our list
        TreeSet<String> entities = new TreeSet<String>();
        for (Column column : getColumns()) {
            entities.add(column.getName());
        }

        // put the child tables into their ordered list.
        TreeMap<String, UserTable> childTables = new TreeMap<String, UserTable>();
        for (Join childJoin : candidateChildJoins ) {
            String childName;
            if (childJoin.getChild().getName().getSchemaName().equals(getName().getSchemaName())) {
                childName = childJoin.getChild().getName().getTableName();
            } else {
                childName = childJoin.getChild().getName().toString();
            }
            childTables.put(childName, childJoin.getChild());
        }
       
        // Mangle the child table names to be unique with the "_"
        for (String child : childTables.keySet()) {
            String tryName = child;
            while (entities.contains(tryName)) {
                tryName = "_" + tryName;
            }
            childTables.get(child).nameForOutput = tryName;
            entities.add(tryName);
        }
        
        if (nameForOutput == null) {
            Join parentJoin = getParentJoin();
            if ((parentJoin != null) &&
                    parentJoin.getParent().getName().getSchemaName().equals(getName().getSchemaName())) {
                nameForOutput = getName().getTableName();
            } else {
                nameForOutput = getName().toString(); 
            }
        }
    }

    public Integer getDepth()
    {
        if (depth == null && getGroup() != null) {
            synchronized (this) {
                if (depth == null && getGroup() != null) {
                    depth = getParentJoin() == null ? 0 : getParentJoin().getParent().getDepth() + 1;
                }
            }
        }
        return depth;
    }

    public Boolean isRoot()
    {
        return getGroup() == null || getParentJoin() == null;
    }

    public HKey hKey()
    {
        assert getGroup() != null;
        if (hKey == null) {
            computeHKey();
        }
        return hKey;
    }

    public List<Column> allHKeyColumns()
    {
        assert getGroup() != null;
        assert getPrimaryKeyIncludingInternal() != null;
        if (allHKeyColumns == null) {
            allHKeyColumns = new ArrayList<>();
            for (HKeySegment segment : hKey().segments()) {
                for (HKeyColumn hKeyColumn : segment.columns()) {
                    allHKeyColumns.add(hKeyColumn.column());
                }
            }
            allHKeyColumns = Collections.unmodifiableList(allHKeyColumns);
        }
        return allHKeyColumns;
    }

    public boolean containsOwnHKey()
    {
        hKey(); // Ensure hKey and containsOwnHKey are computed
        return containsOwnHKey;
    }

    public UserTable parentTable()
    {
        Join join = getParentJoin();
        return join == null ? null : join.getParent();
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    // Descendent tables whose hkeys are affected by a change to this table's PK or FK.
    public List<UserTable> hKeyDependentTables()
    {
        if (hKeyDependentTables == null) {
            synchronized (lazyEvaluationLock) {
                if (hKeyDependentTables == null) {
                    hKeyDependentTables = new ArrayList<>();
                    for (Join join : getChildJoins()) {
                        UserTable child = join.getChild();
                        if (!child.containsOwnHKey()) {
                            addTableAndDescendents(child, hKeyDependentTables);
                        }
                    }
                }
            }
        }
        return hKeyDependentTables;
    }

    public boolean hasMemoryTableFactory()
    {
        return tableFactory != null;
    }

    public MemoryTableFactory getMemoryTableFactory()
    {
        return tableFactory;
    }

    public void setMemoryTableFactory(MemoryTableFactory tableFactory)
    {
        this.tableFactory = tableFactory;
    }

    public boolean hasVersion()
    {
        return version != null;
    }

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getNameForOutput() {
        return nameForOutput;
    }
    
    private void addTableAndDescendents(UserTable table, List<UserTable> accumulator)
    {
        accumulator.add(table);
        for (Join join : table.getChildJoins()) {
            addTableAndDescendents(join.getChild(), accumulator);
        }
    }
    
    private void computeHKey()
    {
        hKey = new HKey(this);
        List<Column> hKeyColumns = new ArrayList<>();
        if (!isRoot()) {
            // Start with the parent's hkey
            Join join = getParentJoin();
            HKey parentHKey = join.getParent().hKey();
            // Start forming this table's full hkey by including all of the parent hkey columns, but replacing
            // columns participating in the join (to this table) by columns from this table.
            for (HKeySegment parentHKeySegment : parentHKey.segments()) {
                HKeySegment segment = hKey.addSegment(parentHKeySegment.table());
                for (HKeyColumn parentHKeyColumn : parentHKeySegment.columns()) {
                    Column columnInChild = join.getMatchingChild(parentHKeyColumn.column());
                    Column segmentColumn = columnInChild == null ? parentHKeyColumn.column() : columnInChild;
                    segment.addColumn(segmentColumn);
                    hKeyColumns.add(segmentColumn);
                }
            }
        }
        // This table's hkey also includes any PK columns not already included.
        HKeySegment newSegment = hKey.addSegment(this);
        for (Column pkColumn : getPrimaryKeyIncludingInternal().getColumns()) {
            if (!hKeyColumns.contains(pkColumn)) {
                newSegment.addColumn(pkColumn);
            }
        }
        // Determine whether the table contains its own hkey, i.e., whether all hkey columns come from this table.
        containsOwnHKey = true;
        for (HKeySegment segment : hKey().segments()) {
            for (HKeyColumn hKeyColumn : segment.columns()) {
                if (hKeyColumn.column().getTable() != this) {
                    containsOwnHKey = false;
                }
            }
        }
    }

    private TableIndex createAkibanPrimaryKeyIndex(int indexID)
    {
        // Create a column for a PK
        Column pkColumn = Column.create(this,
                                        Column.AKIBAN_PK_NAME,
                                        getColumns().size(),
                                        Types.BIGINT); // adds column to table
        pkColumn.setNullable(false);
        TableIndex pkIndex = TableIndex.create(ais,
                                               this,
                                               Index.PRIMARY_KEY_CONSTRAINT,
                                               indexID,
                                               true,
                                               Index.PRIMARY_KEY_CONSTRAINT);
        IndexColumn.create(pkIndex, pkColumn, 0, true, null);
        return pkIndex;
    }

    private static Collection<TableIndex> removeInternalColumnIndexes(Collection<TableIndex> indexes)
    {
        Collection<TableIndex> declaredIndexes = new ArrayList<>(indexes);
        for (Iterator<TableIndex> iterator = declaredIndexes.iterator(); iterator.hasNext();) {
            TableIndex index = iterator.next();
            List<IndexColumn> indexColumns = index.getKeyColumns();
            if (indexColumns.size() == 1 && indexColumns.get(0).getColumn().isAkibanPKColumn()) {
                iterator.remove();
            }
        }
        return declaredIndexes;
    }

    public PendingOSC getPendingOSC() {
        return pendingOSC;
    }

    public void setPendingOSC(PendingOSC pendingOSC) {
        this.pendingOSC = pendingOSC;
    }

    /** Return all full text indexes in which this table participates. */
    public Collection<FullTextIndex> getFullTextIndexes() {
        return unmodifiableFullTextIndexes;
    }

    /** Return full text indexes that index this table. */
    public Collection<FullTextIndex> getOwnFullTextIndexes() {
        if (fullTextIndexes.isEmpty()) return Collections.emptyList();
        Collection<FullTextIndex> result = new ArrayList<>();
        for (FullTextIndex index : fullTextIndexes) {
            if (index.getIndexedTable() == this) {
                result.add(index);
            }
        }
        return result;
    }

    public void addFullTextIndex(FullTextIndex index) {
        fullTextIndexes.add(index);
    }

    public FullTextIndex getFullTextIndex(String indexName) {
        for (FullTextIndex index : fullTextIndexes) {
            if ((index.getIndexedTable() == this) &&
                (index.getIndexName().getName().equals(indexName))) {
                return index;
            }
        }
        return null;
    }

    // State

    private final List<Join> candidateParentJoins = new ArrayList<>();
    private final List<Join> candidateChildJoins = new ArrayList<>();
    private final Object lazyEvaluationLock = new Object();

    private UUID uuid;
    private PrimaryKey primaryKey;
    private HKey hKey;
    private boolean containsOwnHKey;
    private List<Column> allHKeyColumns;
    private Integer depth = null;
    private volatile List<UserTable> hKeyDependentTables;
    private MemoryTableFactory tableFactory;
    private Integer version;
    private PendingOSC pendingOSC;
    private final Collection<FullTextIndex> fullTextIndexes;
    private final Collection<FullTextIndex> unmodifiableFullTextIndexes;
    private String nameForOutput;
    
    // consts

    private static final Comparator<Column> COLUMNS_BY_TABLE_DEPTH = new Comparator<Column>() {
        @Override
        public int compare(Column o1, Column o2) {
            return o1.getUserTable().getDepth() - o2.getUserTable().getDepth();
        }
    };
}