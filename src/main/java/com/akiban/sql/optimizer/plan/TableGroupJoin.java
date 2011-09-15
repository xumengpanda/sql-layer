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

package com.akiban.sql.optimizer.plan;

import com.akiban.ais.model.Join;

public class TableGroupJoin extends BasePlanElement
{
    private TableGroup group;
    private TableSource parent, child;
    private ConditionExpression condition;
    private Join join;

    public TableGroupJoin(TableGroup group,
                          TableSource parent, TableSource child,
                          ConditionExpression condition, Join join) {
        this.group = group;
        this.parent = parent;
        parent.setGroup(group);
        this.child = child;
        this.condition = condition;
        ((ComparisonCondition)
         condition).setImplementation(ConditionExpression.Implementation.GROUP_JOIN);
        child.setParentJoin(this);
        this.join = join;
    }

    public TableGroup getGroup() {
        return group;
    }
    protected void setGroup(TableGroup group) {
        this.group = group;
    }

    public TableSource getParent() {
        return parent;
    }
    public TableSource getChild() {
        return child;
    }
    public ConditionExpression getCondition() {
        return condition;
    }
    public Join getJoin() {
        return join;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toString(hashCode(), 16) +
            "(" + join + ")";
    }

    @Override
    protected boolean maintainInDuplicateMap() {
        return true;
    }

    @Override
    protected void deepCopy(DuplicateMap map) {
        super.deepCopy(map);
        group = (TableGroup)group.duplicate(map);
        parent = (TableSource)parent.duplicate(map);
        child = (TableSource)child.duplicate(map);
    }
    
}
