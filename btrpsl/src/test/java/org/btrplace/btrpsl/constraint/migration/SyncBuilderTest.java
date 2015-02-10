/*
 * Copyright (c) 2014 University Nice Sophia Antipolis
 *
 * This file is part of btrplace.
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.btrplace.btrpsl.constraint.migration;

import org.btrplace.btrpsl.Script;
import org.btrplace.btrpsl.ScriptBuilder;
import org.btrplace.btrpsl.ScriptBuilderException;
import org.btrplace.model.DefaultModel;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.model.constraint.migration.Sync;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Set;

/**
 * Unit tests for {@link org.btrplace.btrpsl.constraint.OnlineBuilder}.
 *
 * @author Fabien Hermenier
 */
@Test
public class SyncBuilderTest {

    @DataProvider(name = "badSync")
    public Object[][] getBadSignatures() {
        return new String[][]{
                new String[]{"sync({});"},
                new String[]{"sync({VM2});"},
        };
    }

    @Test(dataProvider = "badSync", expectedExceptions = {ScriptBuilderException.class})
    public void testBadSignatures(String str) throws ScriptBuilderException {
        ScriptBuilder b = new ScriptBuilder(new DefaultModel());
        try {
            b.build("namespace test;\n" +
                    "VM[1..10] : tiny;\n" +
                    "@N[1..5] : defaultNode;\n" + str);
        } catch (ScriptBuilderException ex) {
            System.out.println(str + " " + ex.getMessage());
            throw ex;
        }
    }

    @DataProvider(name = "goodSync")
    public String[][] getGoodSignatures() {
        return new String[][]{
                new String[]{"sync(VM[1..10]);"},
                new String[]{"sync({VM1,VM6});"},
                new String[]{"sync({VM1,VM6,VM9});"}
        };
    }

    @Test(dataProvider = "goodSync")
    public void testGoodSignatures(String str) throws Exception {
        ScriptBuilder b = new ScriptBuilder(new DefaultModel());
        Script scr = b.build(
                "namespace test;\n" +
                "VM[1..10] : tiny;\n" +
                "@N[1..5] : defaultNode;\n" +
                "fence(VM[1..10],@N[1..5]);\n" + str
        );
        Set<SatConstraint> cstrs = scr.getConstraints();

        int count = 0;
        for (SatConstraint x : cstrs) {
            if (x instanceof Sync) { count++; }
        }
        Assert.assertEquals(count, 1);
    }
}
