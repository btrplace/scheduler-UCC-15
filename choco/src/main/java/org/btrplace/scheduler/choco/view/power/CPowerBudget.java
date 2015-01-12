package org.btrplace.scheduler.choco.view.power;

import org.btrplace.model.Model;
import org.btrplace.model.VM;
import org.btrplace.model.constraint.Constraint;
import org.btrplace.model.view.power.EnergyView;
import org.btrplace.model.view.power.PowerBudget;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.constraint.ChocoConstraint;
import org.btrplace.scheduler.choco.constraint.ChocoConstraintBuilder;
import org.chocosolver.solver.Solver;

import java.util.Collections;
import java.util.Set;

/**
 * Created by vins on 11/01/15.
 */
public class CPowerBudget implements ChocoConstraint {

    private PowerBudget pb;

    public CPowerBudget(PowerBudget pb) {
        this.pb = pb;
    }

    @Override
    public boolean inject(ReconfigurationProblem rp) throws SchedulerException {

        // Get the solver
        Solver s = rp.getSolver();
        Model mo = rp.getSourceModel();

        CEnergyView energyView = (CEnergyView) rp.getView(CEnergyView.VIEW_ID);
        if (energyView == null) {
            throw new SchedulerException(rp.getSourceModel(), "View '" + EnergyView.VIEW_ID + "' is required but missing");
        }

        energyView.addRestriction(pb.getStart(), pb.getEnd(), pb.getBudget());

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
            return PowerBudget.class;
        }

        @Override
        public CPowerBudget build(Constraint c) {
            return new CPowerBudget((PowerBudget) c);
        }
    }
}
