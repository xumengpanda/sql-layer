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

package com.akiban.server.test.it.keyupdate;

import com.akiban.ais.model.Index;
import com.akiban.server.api.dml.scan.NewRow;
import org.junit.Test;

public final class GroupIndexLjUpdateIT extends GIUpdateITBase {

    @Test
    public void placeholderNoOrphan() {
        groupIndex("c.name, o.when");
        writeAndCheck(
                createNewRow(c, 1L, "Bergy"),
                "Bergy, null, 1, null => " + depthUntil(c)
        );
        writeAndCheck(
                createNewRow(o, 10L, 1L, "01-01-2001"),
                "Bergy, 01-01-2001, 1, 10 => " + depthUntil(o)
        );
    }

    @Test
    public void placeholderWithOrphan() {
        groupIndex("c.name, o.when");
        writeAndCheck(
                createNewRow(o, 10L, 1L, "01-01-2001")
        );
        writeAndCheck(
                createNewRow(c, 1L, "Bergy"),
                "Bergy, 01-01-2001, 1, 10 => " + depthUntil(o)
        );
    }

    @Test
    public void deleteSecondOinCO() {
        groupIndex("c.name, o.when");
        final NewRow customer, firstOrder, secondOrder;
        writeAndCheck(
                customer = createNewRow(c, 1L, "Joe"),
                "Joe, null, 1, null => " + depthUntil(c)
        );
        writeAndCheck(
                firstOrder = createNewRow(o, 11L, 1L, "01-01-01"),
                "Joe, 01-01-01, 1, 11 => " + depthUntil(o)
        );
        writeAndCheck(
                secondOrder = createNewRow(o, 12L, 1L, "02-02-02"),
                "Joe, 01-01-01, 1, 11 => " + depthUntil(o),
                "Joe, 02-02-02, 1, 12 => " + depthUntil(o)
        );
        deleteAndCheck(
                secondOrder,
                "Joe, 01-01-01, 1, 11 => " + depthUntil(o)
        );
        deleteAndCheck(
                firstOrder,
                "Joe, null, 1, null => " + depthUntil(c)
        );
        deleteAndCheck(
                customer
        );
    }

    @Test
    public void coiGIsNoOrphan() {
        groupIndex("c.name, o.when, i.sku");
        // write write write
        writeAndCheck(
                createNewRow(c, 1L, "Horton"),
                "Horton, null, null, 1, null, null => " + depthUntil(c)
        );
        writeAndCheck(
                createNewRow(o, 11L, 1L, "01-01-2001"),
                "Horton, 01-01-2001, null, 1, 11, null => " + depthUntil(o)
        );
        writeAndCheck(
                createNewRow(i, 101L, 11L, 1111),
                "Horton, 01-01-2001, 1111, 1, 11, 101 => " + depthUntil(i)
        );
        writeAndCheck(
                createNewRow(i, 102L, 11L, 2222),
                "Horton, 01-01-2001, 1111, 1, 11, 101 => " + depthUntil(i),
                "Horton, 01-01-2001, 2222, 1, 11, 102 => " + depthUntil(i)
        );

        writeAndCheck(
                createNewRow(i, 103L, 11L, 3333),
                "Horton, 01-01-2001, 1111, 1, 11, 101 => " + depthUntil(i),
                "Horton, 01-01-2001, 2222, 1, 11, 102 => " + depthUntil(i),
                "Horton, 01-01-2001, 3333, 1, 11, 103 => " + depthUntil(i)
        );

        writeAndCheck(
                createNewRow(o, 12L, 1L, "02-02-2002"),
                "Horton, 01-01-2001, 1111, 1, 11, 101 => " + depthUntil(i),
                "Horton, 01-01-2001, 2222, 1, 11, 102 => " + depthUntil(i),
                "Horton, 01-01-2001, 3333, 1, 11, 103 => " + depthUntil(i),
                "Horton, 02-02-2002, null, 1, 12, null => " + depthUntil(o)
        );

        writeAndCheck(createNewRow(a, 10001L, 1L, "Causeway"),
                "Horton, 01-01-2001, 1111, 1, 11, 101 => " + depthUntil(i),
                "Horton, 01-01-2001, 2222, 1, 11, 102 => " + depthUntil(i),
                "Horton, 01-01-2001, 3333, 1, 11, 103 => " + depthUntil(i),
                "Horton, 02-02-2002, null, 1, 12, null => " + depthUntil(o)
        );

        // update parent
        updateAndCheck(
                createNewRow(o, 11L, 1L, "01-01-2001"),
                createNewRow(o, 11L, 1L, "01-01-1999"), // party!
                "Horton, 01-01-1999, 1111, 1, 11, 101 => " + depthUntil(i),
                "Horton, 01-01-1999, 2222, 1, 11, 102 => " + depthUntil(i),
                "Horton, 01-01-1999, 3333, 1, 11, 103 => " + depthUntil(i),
                "Horton, 02-02-2002, null, 1, 12, null => " + depthUntil(o)
        );
        // update child
        updateAndCheck(
                createNewRow(i, 102L, 11L, 2222),
                createNewRow(i, 102L, 11L, 2442),
                "Horton, 01-01-1999, 1111, 1, 11, 101 => " + depthUntil(i),
                "Horton, 01-01-1999, 2442, 1, 11, 102 => " + depthUntil(i),
                "Horton, 01-01-1999, 3333, 1, 11, 103 => " + depthUntil(i),
                "Horton, 02-02-2002, null, 1, 12, null => " + depthUntil(o)
        );

        // delete child
        deleteAndCheck(
                createNewRow(i, 102L, 11L, 222211),
                "Horton, 01-01-1999, 1111, 1, 11, 101 => " + depthUntil(i),
                "Horton, 01-01-1999, 3333, 1, 11, 103 => " + depthUntil(i),
                "Horton, 02-02-2002, null, 1, 12, null => " + depthUntil(o)
        );
        // delete grandparent
        deleteAndCheck(
                createNewRow(o, 11L, 1L, "01-01-2001"),
                "Horton, 02-02-2002, null, 1, 12, null => " + depthUntil(o)
        );
    }

    @Test
    public void createGIOnFullyPopulatedTables() {
        writeRows(
                createNewRow(c, 1L, "Horton"),
                createNewRow(o, 11L, 1L, "01-01-2001"),
                createNewRow(i, 101L, 11L, 1111)
        );
        groupIndex("name_when_sku", "c.name, o.when, i.sku");
        checkIndex("name_when_sku",
                "Horton, 01-01-2001, 1111, 1, 11, 101 => " + depthUntil(i)
        );
    }

    @Test
    public void createGIOnPartiallyPopulatedTablesFromRoot() {
        writeRows(
                createNewRow(c, 1L, "Horton"),
                createNewRow(o, 11L, 1L, "01-01-2001")
        );
        groupIndex("name_when_sku", "c.name, o.when, i.sku");
        checkIndex("name_when_sku",
                "Horton, 01-01-2001, null, 1, 11, null => " + depthUntil(o)
        );
    }
    
    @Test
    public void createGIOnPartiallyPopulatedTablesFromMiddle() {
        writeRows(
                createNewRow(c, 1L, "Horton"),
                createNewRow(o, 11L, 1L, "01-01-2001")
        );
        groupIndex("when_sku","o.when, i.sku");
        checkIndex("when_sku",
                "01-01-2001, null, 1, 11, null => " + depthUntil(o)
        );
    }

    @Test
    public void ihIndexNoOrphans() {
        String indexName = groupIndex("i.sku, h.handling_instructions");
        writeRows(
                createNewRow(c, 1L, "Horton"),
                createNewRow(o, 11L, 1L, "01-01-2001"),
                createNewRow(i, 101L, 11L, 1111),
                createNewRow(h, 1001L, 101L, "handle with care")
        );
        checkIndex(indexName, "1111, handle with care, 1, 11, 101, 1001 => " + depthUntil(h));

        // delete from root on up
        dml().deleteRow(session(), createNewRow(c, 1L, "Horton"));
        checkIndex(indexName, "1111, handle with care, 1, 11, 101, 1001 => " + depthUntil(h));

        dml().deleteRow(session(), createNewRow(o, 11L, 1L, "01-01-2001 => " + depthUntil(h)));
        checkIndex(indexName, "1111, handle with care, null, 11, 101, 1001 => " + depthUntil(h));

        dml().deleteRow(session(), createNewRow(i, 101L, 11L, 1111));
        checkIndex(indexName);

        dml().deleteRow(session(), createNewRow(h, 1001L, 101L, "handle with care"));
        checkIndex(indexName);
    }

    @Test
    public void ihIndexOIsOrphaned() {
        String indexName = groupIndex("i.sku, h.handling_instructions");
        writeRows(
                createNewRow(o, 11L, 1L, "01-01-2001"),
                createNewRow(i, 101L, 11L, 1111),
                createNewRow(h, 1001L, 101L, "handle with care")
        );
        checkIndex(indexName, "1111, handle with care, 1, 11, 101, 1001 => " + depthUntil(h));

        // delete from root on up

        dml().deleteRow(session(), createNewRow(o, 11L, 1L, "01-01-2001"));
        checkIndex(indexName, "1111, handle with care, null, 11, 101, 1001 => " + depthUntil(h));

        dml().deleteRow(session(), createNewRow(i, 101L, 11L, 1111));
        checkIndex(indexName);

        dml().deleteRow(session(), createNewRow(h, 1001L, 101L, "handle with care"));
        checkIndex(indexName);
    }

    @Test
    public void ihIndexIIsOrphaned() {
        String indexName = groupIndex("i.sku, h.handling_instructions");
        writeRows(
                createNewRow(c, 1L, "Horton"),
                createNewRow(c, -1L, "Notroh"),
                createNewRow(i, 101L, 11L, 1111),
                createNewRow(h, 1001L, 101L, "handle with care")
        );
        checkIndex(indexName, "1111, handle with care, null, 11, 101, 1001 => " + depthUntil(h));

        // delete from root on up

        dml().deleteRow(session(), createNewRow(i, 101L, 11L, 1111));
        checkIndex(indexName);

        dml().deleteRow(session(), createNewRow(h, 1001L, 101L, "handle with care"));
        checkIndex(indexName);
    }

    @Test
    public void ihIndexIIsOrphanedButCExists() {
        String indexName = groupIndex("i.sku, h.handling_instructions");
        writeRows(
                createNewRow(c, 1L, "Horton"),
                createNewRow(i, 101L, 11L, 1111),
                createNewRow(h, 1001L, 101L, "handle with care")
        );
        checkIndex(indexName, "1111, handle with care, null, 11, 101, 1001 => " + depthUntil(h));

        // delete from root on up

        dml().deleteRow(session(), createNewRow(i, 101L, 11L, 1111));
        checkIndex(indexName);

        dml().deleteRow(session(), createNewRow(h, 1001L, 101L, "handle with care"));
        checkIndex(indexName);
    }

    @Test
    public void ihIndexHIsOrphaned() {
        String indexName = groupIndex("i.sku, h.handling_instructions");
        writeRows(
                createNewRow(h, 1001L, 101L, "handle with care")
        );
        checkIndex(indexName);

        // delete from root on up

        dml().deleteRow(session(), createNewRow(h, 1001L, 101L, "handle with care"));
        checkIndex(indexName);
    }

    @Test
    public void adoptionChangesHKeyNoCustomer() {
        String indexName = groupIndex("i.sku, h.handling_instructions");
        writeRows(
                createNewRow(i, 101L, 11L, 1111),
                createNewRow(h, 1001L, 101L, "handle with care")
        );
        checkIndex(indexName,
                "1111, handle with care, null, 11, 101, 1001 => " + depthUntil(h)
        );

        // bring an o that adopts the i
        dml().writeRow(session(), createNewRow(o, 11L, 1L, "01-01-2001"));
        checkIndex(indexName,
                "1111, handle with care, 1, 11, 101, 1001 => " + depthUntil(h)
        );
    }

    @Test
    public void adoptionChangesHKeyWithC() {
        String indexName = groupIndex("i.sku, h.handling_instructions");
        writeRows(
                createNewRow(c, 1L, "Horton"),
                createNewRow(i, 101L, 11L, 1111),
                createNewRow(h, 1001L, 101L, "handle with care")
        );
        checkIndex(indexName,
                "1111, handle with care, null, 11, 101, 1001 => " + depthUntil(h)
        );

        // bring an o that adopts the i
        dml().writeRow(session(), createNewRow(o, 11L, 1L, "01-01-2001"));
        checkIndex(indexName,
                "1111, handle with care, 1, 11, 101, 1001 => " + depthUntil(h)
        );
    }

    @Test
    public void testTwoBranches() {
        groupIndex("when_name", "o.when, c.name");
        groupIndex("second_idx", "c.name, a.street");
        writeRows(
                createNewRow(c, 1L, "Horton"),
                createNewRow(o, 11L, 1L, "01-01-2001"),
                createNewRow(o, 12L, 1L, "03-03-2003"),
                createNewRow(a, 21L, 1L, "Harrington"),
                createNewRow(a, 22L, 1L, "Causeway"),
                createNewRow(c, 2L, "David"),
                createNewRow(o, 13L, 2L, "02-02-2002"),
                createNewRow(a, 23L, 2L, "Highland")
        );

        checkIndex(
                "when_name",
                "01-01-2001, Horton, 1, 11 => " + depthUntil(o),
                "02-02-2002, David, 2, 13 => " + depthUntil(o),
                "03-03-2003, Horton, 1, 12 => " + depthUntil(o)
        );
        checkIndex(
                "second_idx",
                "David, Highland, 2, 23 => " + depthUntil(a),
                "Horton, Causeway, 1, 22 => " + depthUntil(a),
                "Horton, Harrington, 1, 21 => " + depthUntil(a)
        );
    }

    @Test
    public void updateModifiesHKeyWithinBranch() {
        // branch is I-H, we're modifying the hkey of an H
        String indexName = groupIndex("i.sku, h.handling_instructions");
        writeRows(createNewRow(c, 1L, "Horton"));
        checkIndex(indexName);

        writeRows(createNewRow(o, 11L, 1L, "01-01-2001"));
        checkIndex(indexName);

        writeRows(createNewRow(i, 101L, 11L, "1111"));
        checkIndex(indexName, "1111, null, 1, 11, 101, null => " + depthUntil(i));

        writeRows(createNewRow(h, 1001L, 101L, "don't break"));
        checkIndex(indexName, "1111, don't break, 1, 11, 101, 1001 => " + depthUntil(h));

        writeRows(createNewRow(c, 2L, "David"));
        checkIndex(indexName, "1111, don't break, 1, 11, 101, 1001 => " + depthUntil(h));

        writeRows(createNewRow(o, 12L, 2L, "02-02-2002"));
        checkIndex(indexName, "1111, don't break, 1, 11, 101, 1001 => " + depthUntil(h));

        writeRows(createNewRow(i, 102L, 12L, "2222"));
        checkIndex(
                indexName,
                "1111, don't break, 1, 11, 101, 1001 => " + depthUntil(h),
                "2222, null, 2, 12, 102, null => " + depthUntil(i)
        );

        dml().updateRow(
                session(),
                createNewRow(h, 1001L, 101L, "don't break"),
                createNewRow(h, 1001L, 102L, "don't break"),
                null
        );

        checkIndex(
                indexName,
                "1111, null, 1, 11, 101, null => " + depthUntil(i),
                "2222, don't break, 2, 12, 102, 1001 => " + depthUntil(h)
        );
    }

    @Test
    public void updateModifiesHKeyDirectlyAboveBranch() {
        // branch is I-H, we're modifying the hkey of an I
        String indexName = groupIndex("i.sku, h.handling_instructions");
        writeRows(
                createNewRow(c, 1L, "Horton"),
                createNewRow(o, 11L, 1L, "01-01-2001"),
                createNewRow(i, 101L, 11L, "1111"),
                createNewRow(h, 1001L, 101L, "don't break"),
                createNewRow(c, 2L, "David"),
                createNewRow(o, 12L, 2L, "02-02-2002"),
                createNewRow(i, 102L, 12L, "2222")
        );
        checkIndex(
                indexName,
                "1111, don't break, 1, 11, 101, 1001 => " + depthUntil(h),
                "2222, null, 2, 12, 102, null => " + depthUntil(i)
        );

        dml().updateRow(
                session(),
                createNewRow(i, 101L, 11L, "1111"),
                createNewRow(i, 101L, 12L, "1111"),
                null
        );

        checkIndex(
                indexName,
                "1111, don't break, 2, 12, 101, 1001 => " + depthUntil(h),
                "2222, null, 2, 12, 102, null => " + depthUntil(i)
        );
    }

    @Test
    public void updateModifiesHKeyHigherAboveBranch() {
        // branch is I-H, we're modifying the hkey of an O referenced by an I
        String indexName = groupIndex("i.sku, h.handling_instructions");
        writeRows(
                createNewRow(c, 1L, "Horton"),
                createNewRow(o, 11L, 1L, "01-01-2001"),
                createNewRow(i, 101L, 11L, "1111"),
                createNewRow(h, 1001L, 101L, "don't break"),
                createNewRow(c, 2L, "David"),
                createNewRow(o, 12L, 2L, "02-02-2002"),
                createNewRow(i, 102L, 12L, "2222")
        );
        checkIndex(
                indexName,
                "1111, don't break, 1, 11, 101, 1001 => " + depthUntil(h),
                "2222, null, 2, 12, 102, null => " + depthUntil(i)
        );

        dml().updateRow(
                session(),
                createNewRow(o, 11L, 1L, "01-01-2001"),
                createNewRow(o, 11L, 2L, "01-01-2001"),
                null
        );

        checkIndex(
                indexName,
                "1111, don't break, 2, 11, 101, 1001 => " + depthUntil(h),
                "2222, null, 2, 12, 102, null => " + depthUntil(i)
        );
    }

    @Test
    public void updateOrphansHKeyWithinBranch() {
        // branch is I-H, we're modifying the hkey of an H
        String indexName = groupIndex("i.sku, h.handling_instructions");
        writeRows(
                createNewRow(c, 1L, "Horton"),
                createNewRow(o, 11L, 1L, "01-01-2001"),
                createNewRow(i, 101L, 11L, "1111"),
                createNewRow(h, 1001L, 101L, "don't break"),
                createNewRow(c, 2L, "David"),
                createNewRow(o, 12L, 2L, "02-02-2002"),
                createNewRow(i, 102L, 12L, "2222")
        );
        checkIndex(
                indexName,
                "1111, don't break, 1, 11, 101, 1001 => " + depthUntil(h),
                "2222, null, 2, 12, 102, null => " + depthUntil(i)
        );

        dml().updateRow(
                session(),
                createNewRow(h, 1001L, 101L, "don't break"),
                createNewRow(h, 1001L, 666L, "don't break"),
                null
        );

        checkIndex(indexName,
                "1111, null, 1, 11, 101, null => " + depthUntil(i),
                "2222, null, 2, 12, 102, null => " + depthUntil(i)
        );
    }

    @Test
    public void updateMovesHKeyWithinBranch() {
        // branch is I-H, we're modifying the hkey of an H
        String indexName = groupIndex("i.sku, h.handling_instructions");
        writeRows(
                createNewRow(c, 1L, "Horton"),
                createNewRow(o, 11L, 1L, "01-01-2001"),
                createNewRow(i, 101L, 11L, "1111"),
                createNewRow(h, 1001L, 101L, "don't break"),
                createNewRow(c, 2L, "David"),
                createNewRow(o, 12L, 2L, "02-02-2002"),
                createNewRow(i, 102L, 12L, "2222"),

                createNewRow(o, 66L, 6L,
 "03-03-2003"),
                createNewRow(i, 666L, 66L, "6666")
        );
        checkIndex(
                indexName,
                "1111, don't break, 1, 11, 101, 1001 => " + depthUntil(h),
                "2222, null, 2, 12, 102, null => " + depthUntil(i),
                "6666, null, 6, 66, 666, null => " + depthUntil(i)
        );

        dml().updateRow(
                session(),
                createNewRow(h, 1001L, 101L, "don't break"),
                createNewRow(h, 1001L, 666L, "don't break"),
                null
        );

        checkIndex(
                indexName,
                "1111, null, 1, 11, 101, null => " + depthUntil(i),
                "2222, null, 2, 12, 102, null => " + depthUntil(i),
                "6666, don't break, 6, 66, 666, 1001 => " + depthUntil(h)
        );
    }

    @Test
    public void updateOrphansHKeyDirectlyAboveBranch() {
        // branch is I-H, we're modifying the hkey of an I
        String indexName = groupIndex("i.sku, h.handling_instructions");
        writeRows(
                createNewRow(c, 1L, "Horton"),
                createNewRow(o, 11L, 1L, "01-01-2001"),
                createNewRow(i, 101L, 11L, "1111"),
                createNewRow(h, 1001L, 101L, "don't break"),
                createNewRow(c, 2L, "David"),
                createNewRow(o, 12L, 2L, "02-02-2002"),
                createNewRow(i, 102L, 12L, "2222")
        );
        checkIndex(
                indexName,
                "1111, don't break, 1, 11, 101, 1001 => " + depthUntil(h),
                "2222, null, 2, 12, 102, null => " + depthUntil(i)
        );

        dml().updateRow(
                session(),
                createNewRow(i, 101L, 11L, "1111"),
                createNewRow(i, 101L, 66L, "1111"),
                null
        );

        checkIndex(
                indexName,
                "1111, don't break, null, 66, 101, 1001 => " + depthUntil(h),
                "2222, null, 2, 12, 102, null => " + depthUntil(i)
        );
    }

    @Test
    public void updateOrphansHKeyHigherAboveBranch() {
        // branch is I-H, we're modifying the hkey of an O referenced by an I
        String indexName = groupIndex("i.sku, h.handling_instructions");
        writeRows(
                createNewRow(c, 1L, "Horton"),
                createNewRow(o, 11L, 1L, "01-01-2001"),
                createNewRow(i, 101L, 11L, "1111"),
                createNewRow(h, 1001L, 101L, "don't break"),
                createNewRow(c, 2L, "David"),
                createNewRow(o, 12L, 2L, "02-02-2002"),
                createNewRow(i, 102L, 12L, "2222")
        );
        checkIndex(
                indexName,
                "1111, don't break, 1, 11, 101, 1001 => " + depthUntil(h),
                "2222, null, 2, 12, 102, null => " + depthUntil(i)
        );

        dml().updateRow(
                session(),
                createNewRow(o, 11L, 1L, "01-01-2001"),
                createNewRow(o, 11L, 6L, "01-01-2001"),
                null
        );

        checkIndex(
                indexName,
                "1111, don't break, 6, 11, 101, 1001 => " + depthUntil(h),
                "2222, null, 2, 12, 102, null => " + depthUntil(i)
        );
    }

    /**
     * Create the endgame of {@linkplain #updateOrphansHKeyHigherAboveBranch} initially, as a santy check
     */
    @Test
    public void originallyOrphansHKeyHigherAboveBranch() {
        String indexName = groupIndex("i.sku, h.handling_instructions");
        writeRows(
                createNewRow(c, 1L, "Horton"),
                createNewRow(o, 11L, 6L, "01-01-2001"),
                createNewRow(i, 101L, 11L, "1111"),
                createNewRow(h, 1001L, 101L, "don't break"),
                createNewRow(c, 2L, "David"),
                createNewRow(o, 12L, 2L, "02-02-2002"),
                createNewRow(i, 102L, 12L, "2222")
        );
        checkIndex(
                indexName,
                "1111, don't break, 6, 11, 101, 1001 => " + depthUntil(h),
                "2222, null, 2, 12, 102, null => " + depthUntil(i)
        );
    }

    public GroupIndexLjUpdateIT() {
        super(Index.JoinType.LEFT);
    }
}
