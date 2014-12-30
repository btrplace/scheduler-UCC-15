package org.btrplace.scheduler.choco.view;

import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.Switch;
import org.btrplace.model.VM;
import org.btrplace.model.view.ModelView;
import org.btrplace.model.view.Network;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.transition.MigrateVM;
import org.btrplace.scheduler.choco.transition.VMTransition;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.ICF;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;
import org.chocosolver.solver.variables.VF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by vkherbac on 30/12/14.
 */
public class CNetwork implements ChocoView {

    /**
     * The view identifier.
     */
    public static final String VIEW_ID = "Network";

    private Network net;
    private ReconfigurationProblem rp;
    private Solver solver;
    private Model source;
    List<Task> tasksList;
    List<IntVar> heightsList;

    public CNetwork(ReconfigurationProblem p, Network n) throws SchedulerException {
        net = n;
        rp = p;
        solver = p.getSolver();
        source = p.getSourceModel();
        tasksList = new ArrayList<Task>();
        heightsList = new ArrayList<IntVar>();
    }

    @Override
    public String getIdentifier() { return net.getIdentifier(); }

    @Override
    public boolean beforeSolve(ReconfigurationProblem rp) throws SchedulerException {

        for(Switch.Interface si : net.getAllInterfaces()) {

            for (VM vm : rp.getVMs()) {
                VMTransition a = rp.getVMAction(vm);

                if (a instanceof MigrateVM && (a.getCSlice().getHoster().getValue() != a.getDSlice().getHoster().getValue())) {

                    Node src = source.getMapping().getVMLocation(vm);
                    Node dst = rp.getNode(a.getDSlice().getHoster().getValue());

                    if (net.getPath(src, dst).contains(si)) {
                        tasksList.add(new Task(a.getStart(), a.getDuration(), a.getEnd()));
                        heightsList.add(((MigrateVM) a).getBandwidth());
                    }
                }
            }

            solver.post(ICF.cumulative(
                    tasksList.toArray(new Task[tasksList.size()]),
                    heightsList.toArray(new IntVar[heightsList.size()]),
                    VF.fixed(si.getBandwidth(), solver),
                    true
            ));

            tasksList.clear();
            heightsList.clear();
        }

        return true;
    }

    @Override
    public boolean insertActions(ReconfigurationProblem rp, ReconfigurationPlan p) { return true; }

    @Override
    public boolean cloneVM(VM vm, VM clone) { return true; }

    /**
     * Builder associated to the constraint.
     */
    public static class Builder implements ChocoModelViewBuilder {
        @Override
        public Class<? extends ModelView> getKey() {
            return Network.class;
        }

        @Override
        public SolverViewBuilder build(final ModelView v) throws SchedulerException {
            return new DelegatedBuilder(v.getIdentifier(), Collections.emptyList()) {
                @Override
                public ChocoView build(ReconfigurationProblem r) throws SchedulerException {
                    return new CNetwork(r, (Network) v);
                }
            };
        }
    }
}
