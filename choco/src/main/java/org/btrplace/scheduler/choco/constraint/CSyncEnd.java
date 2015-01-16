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

package org.btrplace.scheduler.choco.constraint;

import org.btrplace.model.Model;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.Constraint;
import org.btrplace.model.view.net.SyncEnd;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.transition.VMTransition;
import org.btrplace.scheduler.choco.view.net.MigrateVMTransition;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.ICF;
import org.chocosolver.solver.constraints.LCF;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.VF;

import java.util.*;

/**
 * Created by vkherbac on 01/09/14.
 */
public class CSyncEnd implements ChocoConstraint {

    private SyncEnd sec;
    private List<MigrateVMTransition> migrationList;

    /**
     * Make a new constraint
     *
     * @param sec the SyncEnd constraint to rely on
     */
    public CSyncEnd(SyncEnd sec) {
        this.sec = sec;
        migrationList = new ArrayList<>();
    }

    @Override
    public Set<VM> getMisPlacedVMs(Model model) {
        return Collections.emptySet();
    }

    @Override
    public boolean inject(ReconfigurationProblem rp) {

        // Get the solver
        Solver s = rp.getSolver();

        // Not enough VMs
        if(sec.getInvolvedVMs().size() < 2) {
            return true;
        }

        // Get all migrations involved
        for (Iterator<VM> ite = sec.getInvolvedVMs().iterator(); ite.hasNext();) {
            VM vm = ite.next();
            VMTransition vt = rp.getVMAction(vm);
            if (vt instanceof MigrateVMTransition) {
                migrationList.add((MigrateVMTransition)vt);
            }
        }

        // Not enough migrations
        if (migrationList.size() < 2) {
            return true;
        }

        for (int i=0; i<migrationList.size(); i++) {
            for (int j=i+1; j<migrationList.size(); j++) {

                BoolVar moveFirst = VF.not((migrationList.get(i)).isStaying());
                BoolVar moveSecond = VF.not((migrationList.get(j)).isStaying());

                // If moveFirst and moveSecond, then SyncEnd
                LCF.ifThen(LCF.and(moveFirst, moveSecond),
                        ICF.arithm(migrationList.get(i).getEnd(), "=", migrationList.get(j).getEnd()));
            }
        }

        return true;
    }

    /**
     * Builder associated to the constraint.
     */
    public static class Builder implements ChocoConstraintBuilder {
        @Override
        public Class<? extends Constraint> getKey() {
            return SyncEnd.class;
        }

        @Override
        public CSyncEnd build(Constraint c) {
            return new CSyncEnd((SyncEnd) c);
        }
    }
}
