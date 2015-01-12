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
    private List<Integer> heights;

    public CEnergyView(ReconfigurationProblem p, EnergyView v) throws SchedulerException {
        ev = v;
        rp = p;
        solver = p.getSolver();
        source = p.getSourceModel();
        tasks = new ArrayList<>();
        heights = new ArrayList<>();
    }

    public void addRestriction(int start, int end, int power) {
        IntVar s, d, e;
        s = VF.fixed(start, solver);
        e = VF.fixed(end, solver);
        d = VF.fixed(end-start, solver);

        tasks.add(VariableFactory.task(s,d,e));
        heights.add(ev.getMaxPower()-power);
    }

    @Override
    public String getIdentifier() {
        return ev.getIdentifier();
    }

    @Override
    public boolean beforeSolve(ReconfigurationProblem rp) throws SchedulerException {

        Model mo = rp.getSourceModel();

        CPowerView powerView = (CPowerView) rp.getView(CPowerView.VIEW_ID);
        if (powerView == null) {
            powerView = new CPowerView(rp);
            if (!rp.addView(powerView)) {
                throw new SchedulerException(rp.getSourceModel(), "Unable to attach view '" + CPowerView.VIEW_ID + "'");
            }
        }

        // Add nodes consumption
        for (Node n : rp.getNodes()) {

            if (mo.getAttributes().isSet(n, "idlePower")) {
                heights.add(mo.getAttributes().getInteger(n, "idlePower"));
                IntVar duration = rp.makeUnboundedDuration(rp.makeVarLabel("Dur(", n, ")"));
                solver.post(IntConstraintFactory.arithm(duration, "<=", rp.getEnd()));
                tasks.add(VariableFactory.task(powerView.getPowerStart(rp.getNode(n)), duration,
                        powerView.getPowerEnd(rp.getNode(n))));
            }
            else {
                throw new SchedulerException(null, "Unable to retrieve attribute 'idlePower' for the node '" + n + "'");
            }
        }

        // Add VMs consumption
        for (VM v : rp.getVMs()) {

            if (mo.getAttributes().isSet(v, "power")) {

                int vmPower = mo.getAttributes().getInteger(v, "power");
                VMState currentState = rp.getSourceModel().getMapping().getState(v);
                VMState futureState = rp.getNextState(v);
                VMTransition vmt = rp.getVMAction(v);

                if (currentState.equals(VMState.RUNNING) && futureState.equals(VMState.RUNNING)) {

                    // No migration TODO: manage even if DSlice hoster is not instantiated
                    if(rp.getNode(vmt.getCSlice().getHoster().getValue()).equals(rp.getNode(vmt.getDSlice().getHoster().getValue()))) {
                        IntVar duration = rp.makeUnboundedDuration(rp.makeVarLabel("Dur(", v, ")"));
                        solver.post(IntConstraintFactory.arithm(duration, "<=", rp.getEnd()));
                        tasks.add(VariableFactory.task(rp.getStart(), duration, rp.getEnd()));
                        heights.add(vmPower);
                    }
                    else { //TODO: add energy overhead due to the live migration !
                        // Consumption on the source node
                        tasks.add(VariableFactory.task(vmt.getCSlice().getStart(), vmt.getCSlice().getDuration(), vmt.getCSlice().getEnd()));
                        heights.add(vmPower);

                        // Consumption on the destination node
                        tasks.add(VariableFactory.task(vmt.getDSlice().getStart(), vmt.getDSlice().getDuration(), vmt.getDSlice().getEnd()));
                        heights.add(vmPower);
                    }

                } else if (!currentState.equals(VMState.RUNNING) && futureState.equals(VMState.RUNNING)) {
                    // Add consumption on the destination node
                    tasks.add(VariableFactory.task(vmt.getDSlice().getStart(), vmt.getDSlice().getDuration(), vmt.getDSlice().getEnd()));
                    heights.add(vmPower);

                } else if (currentState.equals(VMState.RUNNING) && !futureState.equals(VMState.RUNNING)) {
                    // Add consumption on the source node
                    tasks.add(VariableFactory.task(vmt.getCSlice().getStart(), vmt.getCSlice().getDuration(), vmt.getCSlice().getEnd()));
                    heights.add(vmPower);
                }
            }
            else {
                throw new SchedulerException(null, "Unable to retrieve attribute 'powerConsumption' for the vm '" + v + "'");
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
            return new DelegatedBuilder(v.getIdentifier(), Collections.emptyList()) {
                @Override
                public ChocoView build(ReconfigurationProblem r) throws SchedulerException {
                    return new CEnergyView(r, (EnergyView) v);
                }
            };
        }
    }
}
