package org.btrplace.scheduler.choco.view.net;

import org.btrplace.model.Mapping;
import org.btrplace.model.Model;
import org.btrplace.model.VM;
import org.btrplace.model.view.net.MinMTTRObjective;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.constraint.ChocoConstraintBuilder;
import org.btrplace.scheduler.choco.transition.NodeTransition;
import org.btrplace.scheduler.choco.transition.ShutdownableNode;
import org.btrplace.scheduler.choco.transition.Transition;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.IntConstraintFactory;
import org.chocosolver.solver.search.strategy.ISF;
import org.chocosolver.solver.search.strategy.IntStrategyFactory;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.search.strategy.strategy.StrategiesSequencer;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;
import org.chocosolver.solver.variables.VariableFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by vkherbac on 07/01/15.
 */
public class CMinMTTRObjective implements org.btrplace.scheduler.choco.constraint.CObjective {

    private ReconfigurationProblem rp;
    private Constraint costConstraint;
    private boolean costActivated = false;
    private List <AbstractStrategy> strategies;

    public CMinMTTRObjective() {
        strategies = new ArrayList<>();
    }

    @Override
    public boolean inject(ReconfigurationProblem rp) throws SchedulerException {

        this.rp = rp;
        Model mo = rp.getSourceModel();
        Mapping map = mo.getMapping();
        Solver s = rp.getSolver();

        List<IntVar> endVars = new ArrayList<IntVar>();

        // Set objective: Terminate all VM actions ASAP
        for (Transition m : rp.getVMActions()) { endVars.add(m.getEnd()); }
        for (Transition m : rp.getNodeActions()) { endVars.add(m.getEnd()); }
        IntVar[] costs = endVars.toArray(new IntVar[endVars.size()]);
        IntVar cost = VariableFactory.bounded(rp.makeVarLabel("costEndVars"), 0, Integer.MAX_VALUE / 100, s);
        costConstraint = IntConstraintFactory.sum(costs, cost);
        rp.getSolver().post(costConstraint);
        rp.setObjective(true, cost);

        // End vars per link
        endVars.clear();
        CNetworkView cnv = (CNetworkView) rp.getView(CNetworkView.VIEW_ID);
        if (cnv == null) {
            throw new SchedulerException(rp.getSourceModel(), "Solver View '" + CNetworkView.VIEW_ID +
                    "' is required but missing");
        }
        List<List<Task>> tasksPerLink = cnv.getTasksPerLink();
        if (!tasksPerLink.isEmpty()) {
            Collections.sort(tasksPerLink, (tasks, tasks2) -> tasks2.size() - tasks.size());
            for (List<Task> tasks : tasksPerLink) {
                if (!tasks.isEmpty()) {
                    endVars.clear();
                    for (Task t : tasks) {
                        endVars.add(t.getEnd());
                    }
                    strategies.add(ISF.custom(
                            IntStrategyFactory.minDomainSize_var_selector(),
                            IntStrategyFactory.mid_value_selector(),//.max_value_selector(),
                            IntStrategyFactory.split(), // Split from max
                            endVars.toArray(new IntVar[endVars.size()])
                    ));
                }
            }
        }

        /* End vars for all actions
        endVars.clear();
        for (Transition m : rp.getVMActions()) { endVars.add(m.getEnd()); }
        for (Transition m : rp.getNodeActions()) { endVars.add(m.getEnd()); }
        endVars.add(rp.getEnd());
        /*strategies.add(ISF.custom(
                IntStrategyFactory.minDomainSize_var_selector(),
                IntStrategyFactory.mid_value_selector(),//.max_value_selector(),
                IntStrategyFactory.split(), // Split from max
                endVars.toArray(new IntVar[endVars.size()])
        ));
        //strategies.add(ISF.maxDom_Split(endVars.toArray(new IntVar[endVars.size()])));
        */

        /* End vars for all VMs actions
        endVars.clear();
        for (VMTransition a : rp.getVMActions()) {
            endVars.add(a.getEnd());
        }
        strategies.add(ISF.custom(
                IntStrategyFactory.minDomainSize_var_selector(),
                IntStrategyFactory.mid_value_selector(),//.max_value_selector(),
                IntStrategyFactory.split(), // Split from max
                endVars.toArray(new IntVar[endVars.size()])
        ));*/

        // End vars for all Nodes shutdown actions
        endVars.clear();
        for (NodeTransition a : rp.getNodeActions()) {
            if (a instanceof ShutdownableNode) {
                endVars.add(a.getHostingEnd());
            }
        }
        if (!endVars.isEmpty()) {
            strategies.add(ISF.custom(
                    IntStrategyFactory.minDomainSize_var_selector(),
                    IntStrategyFactory.mid_value_selector(),//.max_value_selector(),
                    IntStrategyFactory.split(), // Split from max
                    endVars.toArray(new IntVar[endVars.size()])
            ));
        }

        /* End vars for all Nodes boot actions
        endVars.clear();
        for (NodeTransition a : rp.getNodeActions()) {
            if (a instanceof BootableNode) {
                endVars.add(a.getHostingEnd());
            }
        }
        if (!endVars.isEmpty()) {
            strategies.add(ISF.custom(
                    IntStrategyFactory.minDomainSize_var_selector(),
                    IntStrategyFactory.mid_value_selector(),//.max_value_selector(),
                    IntStrategyFactory.split(), // Split from max
                    endVars.toArray(new IntVar[endVars.size()])
            ));
        }*/

        /* End vars for all VMs actions per node
        for (Node n : rp.getNodes()) {
            endVars.clear();
            for (VMTransition a : rp.getVMActions()) {
                if (rp.getNode(n) == (a.getCSlice().getHoster().getValue())) {
                    endVars.add(a.getEnd());
                }
            }
            if (!endVars.isEmpty()) {
                //endVars.add(rp.getNodeAction(n).getHostingEnd());
                strategies.add(ISF.custom(
                        IntStrategyFactory.minDomainSize_var_selector(),
                        IntStrategyFactory.mid_value_selector(),//.max_value_selector(),
                        IntStrategyFactory.split(), // Split from max
                        endVars.toArray(new IntVar[endVars.size()])
                ));
            }
        }*/

        // Add strategy for the cost constraint
        strategies.add(ISF.custom(
                IntStrategyFactory.minDomainSize_var_selector(),
                IntStrategyFactory.mid_value_selector(), //.max_value_selector(),
                IntStrategyFactory.split(), // Split from max
                new IntVar[]{rp.getEnd()}
        ));
        //strategies.add(new IntStrategy(new IntVar[]{rp.getEnd(), cost}, new InputOrder<>(), new IntDomainMin()));

        // Add all defined strategies
        s.getSearchLoop().set(new StrategiesSequencer(s.getEnvironment(),strategies.toArray(new AbstractStrategy[strategies.size()])));
        //s.set(strategies.toArray(new AbstractStrategy[strategies.size()]));
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
            return MinMTTRObjective.class;
        }

        @Override
        public CMinMTTRObjective build(org.btrplace.model.constraint.Constraint cstr) {
            return new CMinMTTRObjective();
        }
    }
}