package org.btrplace.scheduler.choco.constraint.migration;

import org.btrplace.model.Model;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.Constraint;
import org.btrplace.model.constraint.migration.Serialize;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.constraint.ChocoConstraint;
import org.btrplace.scheduler.choco.constraint.ChocoConstraintBuilder;
import org.btrplace.scheduler.choco.transition.VMTransition;
import org.btrplace.scheduler.choco.view.net.MigrateVMTransition;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.ICF;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;
import org.chocosolver.solver.variables.VF;

import java.util.*;

/**
 * Created by vkherbac on 23/01/15.
 */
public class CSerialize implements ChocoConstraint {

    private Serialize ser;
    private List<MigrateVMTransition> migrationList;

    public CSerialize(Serialize ser) {
        this.ser = ser;
        migrationList = new ArrayList<>();
    }

    @Override
    public boolean inject(ReconfigurationProblem rp) throws SchedulerException {

        // Get the solver
        Solver s = rp.getSolver();

        // Not enough VMs
        if(ser.getInvolvedVMs().size() < 2) {
            return true;
        }

        // Get all migrations involved
        for (Iterator<VM> ite = ser.getInvolvedVMs().iterator(); ite.hasNext();) {
            VM vm = ite.next();
            VMTransition vt = rp.getVMAction(vm);
            if (vt instanceof MigrateVMTransition) {
                migrationList.add((MigrateVMTransition) vt);
            }
        }

        // Not enough migrations
        if (migrationList.size() < 2) {
            return true;
        }

        // Using a cumulative
        List<Task> tasks = new ArrayList<>();
        for (MigrateVMTransition mig : migrationList) {
            tasks.add(new Task(mig.getStart(), mig.getDuration(), mig.getEnd()));
        }
        IntVar heights[] = new IntVar[tasks.size()];
        Arrays.fill(heights, VF.fixed(1, s));
        s.post(ICF.cumulative(
                tasks.toArray(new Task[tasks.size()]),
                heights,
                VF.fixed(1, s),
                true
        ));

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
            return Serialize.class;
        }

        @Override
        public CSerialize build(Constraint c) {
            return new CSerialize((Serialize) c);
        }
    }
}
