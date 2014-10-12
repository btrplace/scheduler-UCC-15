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

package org.btrplace.solver.choco.constraint;

import org.btrplace.model.Mapping;
import org.btrplace.model.Model;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.Constraint;
import org.btrplace.model.constraint.Sleeping;
import org.btrplace.solver.SchedulerException;
import org.btrplace.solver.choco.ReconfigurationProblem;

import java.util.Collections;
import java.util.Set;


/**
 * Naive implementation of {@link org.btrplace.model.constraint.Sleeping}.
 * This constraint is just a stub to be consistent with the model. It does not state any constraint
 * as the state has already been expressed inside {@link org.btrplace.solver.choco.ReconfigurationProblem}.
 *
 * @author Fabien Hermenier
 */
public class CSleeping implements ChocoConstraint {

    private Sleeping cstr;

    /**
     * Make a new constraint.
     *
     * @param c the constraint to rely on
     */
    public CSleeping(Sleeping c) {
        cstr = c;
    }

    @Override
    public boolean inject(ReconfigurationProblem rp) throws SchedulerException {
        if (cstr.isContinuous() && !cstr.getChecker().startsWith(rp.getSourceModel())) {
            rp.getLogger().error("Constraint {} is not satisfied initially", cstr);
            return false;
        }

        return true;
    }

    @Override
    public Set<VM> getMisPlacedVMs(Model m) {
        VM v = cstr.getInvolvedVMs().iterator().next();
        Mapping map = m.getMapping();
        if (!map.isSleeping(v)) {
            return Collections.singleton(v);
        }
        return Collections.emptySet();
    }

    @Override
    public String toString() {
        return cstr.toString();
    }

    /**
     * Builder associated to the constraint.
     */
    public static class Builder implements ChocoConstraintBuilder {
        @Override
        public Class<? extends Constraint> getKey() {
            return Sleeping.class;
        }

        @Override
        public CSleeping build(Constraint c) {
            return new CSleeping((Sleeping) c);
        }
    }
}
