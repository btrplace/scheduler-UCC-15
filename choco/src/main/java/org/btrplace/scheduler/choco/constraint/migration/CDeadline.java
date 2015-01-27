package org.btrplace.scheduler.choco.constraint.migration;

import org.btrplace.model.Model;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.Constraint;
import org.btrplace.model.constraint.migration.Deadline;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.constraint.ChocoConstraint;
import org.btrplace.scheduler.choco.constraint.ChocoConstraintBuilder;
import org.btrplace.scheduler.choco.transition.VMTransition;
import org.btrplace.scheduler.choco.view.net.MigrateVMTransition;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.ICF;
import org.chocosolver.solver.constraints.LCF;
import org.chocosolver.solver.variables.VF;

import java.util.*;

/**
 * Created by vkherbac on 23/01/15.
 */
public class CDeadline implements ChocoConstraint {

    private Deadline dl;
    private List<MigrateVMTransition> migrationList;

    public CDeadline(Deadline dl) {
        this.dl = dl;
        migrationList = new ArrayList<>();
    }

    @Override
    public boolean inject(ReconfigurationProblem rp) throws SchedulerException {

        // Get the solver
        Solver s = rp.getSolver();

        // Get all migrations involved
        for (Iterator<VM> ite = dl.getInvolvedVMs().iterator(); ite.hasNext();) {
            VM vm = ite.next();
            VMTransition vt = rp.getVMAction(vm);
            if (vt instanceof MigrateVMTransition) {
                LCF.ifThen(VF.not(((MigrateVMTransition)vt).isStaying()),
                        ICF.arithm(vt.getEnd(), "<=", dl.getDeadline()));
            }
        }

        return true;
    }

    @Override
    public Set<VM> getMisPlacedVMs(Model m) {
        return Collections.emptySet();
    }

    /**
     * Builder associated to the constraint.
     */
    public static class Builder implements ChocoConstraintBuilder {
        @Override
        public Class<? extends Constraint> getKey() {
            return Deadline.class;
        }

        @Override
        public CDeadline build(Constraint c) {
            return new CDeadline((Deadline) c);
        }
    }
}
