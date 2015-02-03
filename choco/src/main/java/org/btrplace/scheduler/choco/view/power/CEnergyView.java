package org.btrplace.scheduler.choco.view.power;

import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.VMState;
import org.btrplace.model.view.ModelView;
import org.btrplace.model.view.power.EnergyView;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.transition.VMTransition;
import org.btrplace.scheduler.choco.view.*;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.ICF;
import org.chocosolver.solver.constraints.IntConstraintFactory;
import org.chocosolver.solver.constraints.LCF;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;
import org.chocosolver.solver.variables.VF;
import org.chocosolver.solver.variables.VariableFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by vins on 11/01/15.
 */
public class CEnergyView implements ChocoView {

    /**
     * The view identifier.
     */
    public static final String VIEW_ID = "EnergyView";

    private EnergyView ev;
    private ReconfigurationProblem rp;
    private Solver solver;
    private Model source;
    private List<Task> tasks;
    private List<IntVar> heights;
    private CPowerView cPowerView;
    private int maxDiscretePower = 0;


    public CEnergyView(ReconfigurationProblem p, EnergyView v) throws SchedulerException {
        ev = v;
        rp = p;
        solver = p.getSolver();
        source = p.getSourceModel();
        tasks = new ArrayList<>();
        heights = new ArrayList<>();

        // Retrieve or create the PowerView
        cPowerView = (CPowerView) rp.getView(CPowerView.VIEW_ID);
        if (cPowerView == null) {
            cPowerView = new CPowerView(rp);
            if (!rp.addView(cPowerView)) {
                throw new SchedulerException(rp.getSourceModel(), "Unable to attach view '" + CPowerView.VIEW_ID + "'");
            }
        }

    }

    public void cap(int start, int end, int power) {
        IntVar s, d, e;
        s = VF.fixed(start, solver);
        e = VF.fixed(end, solver);

        IntVar eMin = VF.bounded("powerBudgetEnd", start, rp.getEnd().getUB(), solver);
        solver.post(ICF.minimum(eMin, e, rp.getEnd()));
        d = rp.makeUnboundedDuration();
        solver.post(IntConstraintFactory.arithm(d, "<=", rp.getEnd()));

        tasks.add(VariableFactory.task(s,d,eMin));
        heights.add(VF.fixed(ev.getMaxPower()-power, solver));
    }

    public void cap(int power) {
        maxDiscretePower = power;
    }

    @Override
    public String getIdentifier() {
        return ev.getIdentifier();
    }

    @Override
    public boolean beforeSolve(ReconfigurationProblem rp) throws SchedulerException {

        Model mo = rp.getSourceModel();

        // Add constraints for continuous model
        for (Node n : rp.getNodes()) {  // Add nodes consumption
            IntVar duration = rp.makeUnboundedDuration(rp.makeVarLabel("Dur(", n, ")"));
            solver.post(IntConstraintFactory.arithm(duration, "<=", rp.getEnd()));
            tasks.add(VariableFactory.task(cPowerView.getPowerStart(rp.getNode(n)), duration,
                    cPowerView.getPowerEnd(rp.getNode(n))));
            heights.add(VF.fixed(ev.getConsumption(n), solver));
        }
        for (VM v : rp.getVMs()) {  // Add VMs consumption
            int vmPower = ev.getConsumption(v);
            VMState currentState = rp.getSourceModel().getMapping().getState(v);
            VMState futureState = rp.getNextState(v);
            VMTransition vmt = rp.getVMAction(v);

            IntVar duration = rp.makeUnboundedDuration(rp.makeVarLabel("Dur(", v, ")"));
            solver.post(IntConstraintFactory.arithm(duration, "<=", rp.getEnd()));

            // Relocate or Migrate
            if (currentState.equals(VMState.RUNNING) && futureState.equals(VMState.RUNNING)) {
                //TODO: in the case of a live migration, add the transfer overhead
                tasks.add(VariableFactory.task(rp.getStart(), duration, rp.getEnd()));
                heights.add(VF.fixed(vmPower, solver));

            // Boot / Resume
            } else if ((currentState.equals(VMState.READY) && futureState.equals(VMState.RUNNING)) ||
                       (currentState.equals(VMState.SLEEPING) && futureState.equals(VMState.RUNNING))) {
                tasks.add(VariableFactory.task(vmt.getStart(), duration, rp.getEnd()));
                heights.add(VF.fixed(vmPower, solver));

            // Halt / Kill / Sleep
            } else if ((currentState.equals(VMState.RUNNING) && futureState.equals(VMState.READY)) ||
                       (currentState.equals(VMState.RUNNING) && futureState.equals(VMState.KILLED)) ||
                       (currentState.equals(VMState.RUNNING) && futureState.equals(VMState.SLEEPING))) {
                tasks.add(VariableFactory.task(rp.getStart(), duration, rp.getEnd()));
                heights.add(VF.fixed(vmPower, solver));

            // Resume
            } else if (currentState.equals(VMState.SLEEPING) && futureState.equals(VMState.RUNNING)) {
                tasks.add(VariableFactory.task(vmt.getStart(), duration, rp.getEnd()));
                heights.add(VF.fixed(vmPower, solver));
            }
        }
        // Post the resulting cumulative constraint
        if (!tasks.isEmpty()) {
            solver.post(ICF.cumulative(
                    tasks.toArray(new Task[tasks.size()]),
                    heights.toArray(new IntVar[heights.size()]),
                    VF.fixed(ev.getMaxPower(), solver),
                    true
            ));
        }

        // Add constraints for discrete model
        if (maxDiscretePower > 0) {
            List<IntVar> powList = new ArrayList<>();
            for (Node n : rp.getNodes()) {  // Nodes consumption
                int idlePower = ev.getConsumption(n);
                IntVar cons = VF.bounded(rp.makeVarLabel("powerConsumption(" + n + ")"), 0, idlePower, rp.getSolver());
                LCF.ifThenElse(rp.getNodeAction(n).getState(),
                        ICF.arithm(cons, "=", idlePower),
                        ICF.arithm(cons, "=", 0));
                powList.add(cons);
            }
            int vmsPow = 0;
            for (VM v : rp.getFutureRunningVMs()) {  // VMs consumption
                vmsPow += ev.getConsumption(v);
            }
            // Post the constraint
            solver.post(ICF.sum(powList.toArray(new IntVar[powList.size()]), "<=", VF.fixed(maxDiscretePower - vmsPow, solver)));
        }

        return true;
    }

    @Override
    public boolean insertActions(ReconfigurationProblem rp, ReconfigurationPlan p) {
        return true;
    }

    @Override
    public boolean cloneVM(VM vm, VM clone) {
        return true;
    }

    /**
     * Builder associated to the constraint.
     */
    public static class Builder implements ChocoModelViewBuilder {
        @Override
        public Class<? extends ModelView> getKey() {
            return EnergyView.class;
        }

        @Override
        public SolverViewBuilder build(final ModelView v) throws SchedulerException {
            //return new DelegatedBuilder(v.getIdentifier(), Arrays.asList(CPowerView.VIEW_ID)) {
            return new DelegatedBuilder(v.getIdentifier(), Collections.emptyList()) {
                @Override
                public ChocoView build(ReconfigurationProblem r) throws SchedulerException {
                    return new CEnergyView(r, (EnergyView) v);
                }
            };
        }
    }
}
