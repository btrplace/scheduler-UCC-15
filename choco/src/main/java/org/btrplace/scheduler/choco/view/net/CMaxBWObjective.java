package org.btrplace.scheduler.choco.view.net;

import org.btrplace.model.Mapping;
import org.btrplace.model.Model;
import org.btrplace.model.VM;
import org.btrplace.model.view.net.MaxBWObjective;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.constraint.ChocoConstraintBuilder;
import org.btrplace.scheduler.choco.transition.Transition;
import org.btrplace.scheduler.choco.transition.VMTransition;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.IntConstraintFactory;
import org.chocosolver.solver.search.strategy.ISF;
import org.chocosolver.solver.search.strategy.IntStrategyFactory;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.variables.InputOrder;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.search.strategy.strategy.IntStrategy;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.VariableFactory;

import java.util.*;

/**
 * Created by vkherbac on 07/01/15.
 */
public class CMaxBWObjective implements org.btrplace.scheduler.choco.constraint.CObjective {

    private ReconfigurationProblem rp;
    private Constraint costConstraint;
    private boolean costActivated = false;
    private List <AbstractStrategy> strategies;

    public CMaxBWObjective() {
        strategies = new ArrayList<>();
    }

    @Override
    public boolean inject(ReconfigurationProblem rp) throws SchedulerException {

        this.rp = rp;
        Model mo = rp.getSourceModel();
        Mapping map = mo.getMapping();
        Solver s = rp.getSolver();

        // Set objective: Terminate all actions ASAP
        List<IntVar> ends = new ArrayList<>();
        for (Transition m : rp.getVMActions()) { ends.add(m.getEnd()); }
        for (Transition m : rp.getNodeActions()) { ends.add(m.getEnd()); }
        IntVar[] costs = ends.toArray(new IntVar[ends.size()]);
        IntVar cost = VariableFactory.bounded(rp.makeVarLabel("costEndVars"), 0, Integer.MAX_VALUE / 100, s);
        costConstraint = IntConstraintFactory.sum(costs, cost);
        rp.setObjective(true, cost);

        // Add migration strategies
        Set<VM> manageableVMs = new HashSet<>(rp.getManageableVMs());
        for (VMTransition vmt : rp.getVMActions(manageableVMs)) {
            if (vmt instanceof MigrateVMTransition) {

                // Strategy for min migration start time
                strategies.add(ISF.custom(
                        IntStrategyFactory.minDomainSize_var_selector(),
                        IntStrategyFactory.mid_value_selector(),//.min_value_selector(),
                        IntStrategyFactory.split(), // Split from min
                        vmt.getStart()
                ));

                // Strategy for max migration BW
                strategies.add(ISF.custom(
                        IntStrategyFactory.minDomainSize_var_selector(),
                        IntStrategyFactory.mid_value_selector(),//.max_value_selector(),
                        IntStrategyFactory.reverse_split(), // Split from max
                        ((MigrateVMTransition) vmt).getBandwidth()
                ));
            }
        }

        // Add strategy for the cost constraint
        strategies.add(new IntStrategy(new IntVar[]{rp.getEnd(), cost}, new InputOrder<>(), new IntDomainMin()));

        // Add all defined strategies
        //s.getSearchLoop().set(new StrategiesSequencer(s.getEnvironment(),strategies.toArray(new AbstractStrategy[strategies.size()])));
        s.set(strategies.toArray(new AbstractStrategy[strategies.size()]));

        postCostConstraints();

        return true;
    }

    @Override
    public void postCostConstraints() {
        if (!costActivated) {
            rp.getLogger().debug("Post the cost-oriented constraint");
            costActivated = true;
            rp.getSolver().post(costConstraint);
        }
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
        public Class<? extends org.btrplace.model.constraint.Constraint> getKey() {
            return MaxBWObjective.class;
        }

        @Override
        public CMaxBWObjective build(org.btrplace.model.constraint.Constraint cstr) {
            return new CMaxBWObjective();
        }
    }
}