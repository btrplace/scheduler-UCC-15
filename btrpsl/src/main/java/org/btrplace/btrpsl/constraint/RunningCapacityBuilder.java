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

package org.btrplace.btrpsl.constraint;

import org.btrplace.btrpsl.element.BtrpOperand;
import org.btrplace.btrpsl.tree.BtrPlaceTree;
import org.btrplace.model.Node;
import org.btrplace.model.constraint.RunningCapacity;
import org.btrplace.model.constraint.SatConstraint;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * A builder for {@link org.btrplace.model.constraint.RunningCapacity} constraints.
 *
 * @author Fabien Hermenier
 */
public class RunningCapacityBuilder extends DefaultSatConstraintBuilder {

    /**
     * Make a new builder.
     */
    public RunningCapacityBuilder() {
        super("runningCapacity", new ConstraintParam[]{new ListOfParam("$n", 1, BtrpOperand.Type.node, false), new NumberParam("$nb")});
    }

    @Override
    public List<SatConstraint> buildConstraint(BtrPlaceTree t, List<BtrpOperand> args) {
        if (!checkConformance(t, args)) {
            return Collections.emptyList();
        }
        List<Node> ns = (List<Node>) params[0].transform(this, t, args.get(0));
        Number v = (Number) params[1].transform(this, t, args.get(1));
        if (v.doubleValue() < 0) {
            t.ignoreError("Parameter '" + params[1].getName() + "' expects a positive integer (" + v + " given)");
            return Collections.emptyList();
        }

        if (v != null && Math.rint(v.doubleValue()) != v.doubleValue()) {
            t.ignoreError("Parameter '" + params[1].getName() + "' expects an integer, not a real number (" + v + " given)");
            return Collections.emptyList();
        }

        return (ns != null && v != null) ?
                (List) Collections.singletonList(new RunningCapacity(new HashSet<>(ns), v.intValue())) :
                Collections.emptyList();
    }
}
