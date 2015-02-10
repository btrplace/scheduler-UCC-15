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

package org.btrplace.btrpsl.constraint.energy;

import org.btrplace.btrpsl.Script;
import org.btrplace.btrpsl.ScriptBuilder;
import org.btrplace.btrpsl.ScriptBuilderException;
import org.btrplace.model.DefaultModel;
import org.btrplace.model.Model;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.model.view.power.EnergyView;
import org.btrplace.model.view.power.PowerBudget;
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
public class MaxWattsBuilderTest {

    @DataProvider(name = "badMaxWatts")
    public Object[][] getBadSignatures() {
        return new String[][]{
                new String[]{"maxWatts(-1,10,700);"},
                new String[]{"maxWatts(10, 9, 700);"},
                //TODO: throw a proper ScriptBuilderException if no number
                //new String[]{"maxWatts(,,);"},
                //new String[]{"maxWatts(1,,700);"},
        };
    }

    @Test(dataProvider = "badMaxWatts", expectedExceptions = {ScriptBuilderException.class})
    public void testBadSignatures(String str) throws ScriptBuilderException {

        Model mo = new DefaultModel();

        // Add the EnergyView
        EnergyView energyView = new EnergyView(1000);
        mo.attach(energyView);

        ScriptBuilder b = new ScriptBuilder(mo);
        try {
            b.build("namespace test; VM[1..10] : tiny;\n" +
                    "@N[1..5] : defaultNode;\n" + str
            );
        } catch (ScriptBuilderException ex) {
            System.out.println(str + " " + ex.getMessage());
            throw ex;
        }
    }

    @DataProvider(name = "goodMaxWatts")
    public Object[][] getGoodSignatures() {
        return new Object[][]{
                new Object[]{"maxWatts(0,100,700);", 700},
                new Object[]{"maxWatts(10,50,200);", 200},
        };
    }

    @Test(dataProvider = "goodMaxWatts")
    public void testGoodSignatures(String str, int watts) throws Exception {
        Model mo = new DefaultModel();

        // Add the EnergyView
        EnergyView energyView = new EnergyView(1000);
        mo.attach(energyView);

        ScriptBuilder b = new ScriptBuilder(mo);
        Script scr = b.build(
                "namespace test;\n" +
                "VM[1..10] : tiny;\n" +
                "@N[1..5] : defaultNode;\n" +
                "fence(VM[1..10],@N[1..5]);\n" + str
        );
        Set<SatConstraint> cstrs = scr.getConstraints();

        for (SatConstraint x : cstrs) {
            if (x instanceof PowerBudget) {
                Assert.assertTrue(((PowerBudget) x).getBudget() == watts);
                return;
            }
        }
        Assert.fail();
    }
}
