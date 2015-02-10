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

import org.btrplace.btrpsl.constraint.ConstraintParam;
import org.btrplace.btrpsl.constraint.DefaultSatConstraintBuilder;
import org.btrplace.btrpsl.constraint.NumberParam;
import org.btrplace.btrpsl.element.BtrpOperand;
import org.btrplace.btrpsl.tree.BtrPlaceTree;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.model.view.power.PowerBudget;

import java.util.Collections;
import java.util.List;

/**
 * Created by vkherbac on 05/09/14.
 */
public class MaxWattsBuilder extends DefaultSatConstraintBuilder {

    /**
     * Make a new builder.
     */
    public MaxWattsBuilder() {
        super("maxWatts", new ConstraintParam[]{new NumberParam("$start"), new NumberParam("$end"), new NumberParam("$max")});
    }

    /**
     * Build an online constraint.
     *
     * @param args must be 1 set of vms. The set must not be empty
     * @return a constraint
     */
    public List<SatConstraint> buildConstraint(BtrPlaceTree t, List<BtrpOperand> args) {
        if (!checkConformance(t, args)) {
            return Collections.emptyList();
        }

        Number s = (Number) params[0].transform(this, t, args.get(0));
        Number e = (Number) params[1].transform(this, t, args.get(1));
        Number m = (Number) params[2].transform(this, t, args.get(2));

        if (s == null || e == null || m == null) {
            return Collections.emptyList();
        }

        // Verify start value
        if (s.doubleValue() < 0) {
            t.ignoreError("Parameter '" + params[0].getName() + "' expects a positive integer (" + s + " given)");
            return Collections.emptyList();
        }
        if (Math.rint(s.doubleValue()) != s.doubleValue()) {
            t.ignoreError("Parameter '" + params[0].getName() + "' expects an integer, not a real number (" + s + " given)");
            return Collections.emptyList();
        }

        // Verify end value
        if (e.doubleValue() < 0) {
            t.ignoreError("Parameter '" + params[1].getName() + "' expects a positive integer (" + e + " given)");
            return Collections.emptyList();
        }
        if (Math.rint(e.doubleValue()) != e.doubleValue()) {
            t.ignoreError("Parameter '" + params[1].getName() + "' expects an integer, not a real number (" + e + " given)");
            return Collections.emptyList();
        }

        // Verify coherence between start and end
        if (e.intValue() < s.intValue()) {
            t.ignoreError("Parameter '" + params[0].getName() + "' should be lower than '" + params[1].getName() + "')");
            return Collections.emptyList();
        }

        // Verify 'max watts' value
        if (m.doubleValue() < 0) {
            t.ignoreError("Parameter '" + params[2].getName() + "' expects a positive integer (" + m + " given)");
            return Collections.emptyList();
        }
        if (Math.rint(m.doubleValue()) != m.doubleValue()) {
            t.ignoreError("Parameter '" + params[2].getName() + "' expects an integer, not a real number (" + m + " given)");
            return Collections.emptyList();
        }

        return Collections.singletonList(new PowerBudget(s.intValue(), e.intValue(), m.intValue()));
    }
}