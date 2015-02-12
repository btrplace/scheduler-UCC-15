package org.btrplace.scheduler.choco.view.power;

import org.btrplace.model.Mapping;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.view.power.EnergyView;
import org.btrplace.model.view.power.MinEnergyObjective;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.constraint.ChocoConstraintBuilder;
import org.btrplace.scheduler.choco.transition.ShutdownableNode;
import org.btrplace.scheduler.choco.transition.VMTransition;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.ICF;
import org.chocosolver.solver.constraints.IntConstraintFactory;
import org.chocosolver.solver.search.strategy.ISF;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.variables.InputOrder;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.search.strategy.strategy.IntStrategy;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;
import org.chocosolver.solver.variables.VariableFactory;

import java.util.*;

/**
 * Created by vkherbac on 07/01/15.
 */
public class CMinEnergyObjective implements org.btrplace.scheduler.choco.constraint.CObjective {

    private ReconfigurationProblem rp;
    private Constraint costConstraint;
    private boolean costActivated = false;
    private List <AbstractStrategy> strategies;

    public CMinEnergyObjective() {
        strategies = new ArrayList<>();
    }

    @Override
    public boolean inject(ReconfigurationProblem rp) throws SchedulerException {

        this.rp = rp;
        Model mo = rp.getSourceModel();
        Mapping map = mo.getMapping();
        Solver solver = rp.getSolver();

        CEnergyView cEnergyView = (CEnergyView) rp.getView(CEnergyView.VIEW_ID);
        EnergyView energyView = ((EnergyView)mo.getView(EnergyView.VIEW_ID));
        if (cEnergyView == null || energyView == null) {
            throw new SchedulerException(rp.getSourceModel(), "View '" + EnergyView.VIEW_ID + "' is required but missing");
        }

        // Get energy consumption from energyView
        Map<String, List> energy = cEnergyView.computeEnergy();
        List<Task> tasks = energy.get("tasks");
        List<IntVar> heights = energy.get("heights");

        // Compute all energy costs
        List<IntVar> costs = new ArrayList<>();
        for (int i=0; i<tasks.size(); i++) {
            IntVar cost = VariableFactory.bounded(rp.makeVarLabel("cost"), 0, Integer.MAX_VALUE / 100, solver);
            solver.post(ICF.times(tasks.get(i).getDuration(), heights.get(i), cost));
            costs.add(cost);
        }

        // Sum all costs and set objective
        IntVar cost = VariableFactory.bounded(rp.makeVarLabel("costEnergy"), 0, Integer.MAX_VALUE / 100, solver);
        costConstraint = IntConstraintFactory.sum(costs.toArray(new IntVar[costs.size()]), cost);
        solver.post(costConstraint);
        rp.setObjective(true, cost);

        // Per node decommissioning (Boot dst node -> Migrate -> Shutdown src node) strategy
        List<IntVar> endVars = new ArrayList<>();
        for (Node n : rp.getNodes()) {
            endVars.clear();

            if (rp.getNodeAction(n) instanceof ShutdownableNode) {

                for (VMTransition a : rp.getVMActions()) {

                    if (rp.getNode(n) == (a.getCSlice().getHoster().getValue())) {

                        // Boot dst
                        if (!endVars.contains(rp.getNodeAction(rp.getNode(a.getDSlice().getHoster().getValue())).getEnd())) {
                            endVars.add(rp.getNodeAction(rp.getNode(a.getDSlice().getHoster().getValue())).getEnd());
                        }

                        // Migrate all
                        endVars.add(a.getEnd());
                    }
                }

                // Shutdown
                endVars.add(rp.getNodeAction(n).getEnd());
            }

            if (!endVars.isEmpty()) {
                //endVars.add(rp.getNodeAction(n).getHostingEnd());
                strategies.add(ISF.custom(
                        ISF.maxDomainSize_var_selector(),
                        ISF.mid_value_selector(),//.max_value_selector(),
                        ISF.split(), // Split from max
                        endVars.toArray(new IntVar[endVars.size()])
                ));
            }
        }

        // Set cost first in the strategy
        strategies.add(new IntStrategy(new IntVar[]{cost, rp.getEnd()}, new InputOrder<>(), new IntDomainMin()));

        // Add all defined strategies
        //solver.getSearchLoop().set(new StrategiesSequencer(solver.getEnvironment(),strategies.toArray(new AbstractStrategy[strategies.size()])));
        solver.set(strategies.toArray(new AbstractStrategy[strategies.size()]));
        return true;
    }

    @Override
    public void postCostConstraints() {
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
            return MinEnergyObjective.class;
        }

        @Override
        public CMinEnergyObjective build(org.btrplace.model.constraint.Constraint cstr) {
            return new CMinEnergyObjective();
        }
    }
}