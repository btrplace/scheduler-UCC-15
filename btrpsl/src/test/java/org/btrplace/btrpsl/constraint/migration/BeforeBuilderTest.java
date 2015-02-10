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
import org.btrplace.model.constraint.migration.Deadline;
import org.btrplace.model.constraint.migration.Precedence;
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
public class BeforeBuilderTest {

    @DataProvider(name = "badBefore")
    public Object[][] getBadSignatures() {
        return new String[][]{
                new String[]{"before({},{});"},
                new String[]{"before(VM[1..5],{});"},
                new String[]{"before({},VM[6..10]);"},
                new String[]{"before({},\"50\");"},
                new String[]{"before(VM[1..5],\"\");"}
        };
    }

    @Test(dataProvider = "badBefore", expectedExceptions = {ScriptBuilderException.class})
    public void testBadSignatures(String str) throws ScriptBuilderException {
        ScriptBuilder b = new ScriptBuilder(new DefaultModel());
        try {
            b.build("namespace test; VM[1..10] : tiny;\n@N[1..5] : defaultNode;\n" + str);
        } catch (ScriptBuilderException ex) {
            System.out.println(str + " " + ex.getMessage());
            throw ex;
        }
    }

    @DataProvider(name = "goodBefore")
    public Object[][] getGoodSignatures() {
        return new Object[][]{
                new Object[]{"before(VM[1..5],VM[6..10]);", 5*5},
                new Object[]{"before({VM1},{VM6});", 1},
                new Object[]{"before(VM[1..5],\"10\");", 5},
                new Object[]{"before({VM2},\"10\");", 1}
        };
    }

    @Test(dataProvider = "goodBefore")
    public void testGoodSignatures(String str, int nbCstr) throws Exception {
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
            if (x instanceof Precedence) { count++; }
            if (x instanceof Deadline) { count++; }
        }
        Assert.assertEquals(nbCstr, count);
    }
}
