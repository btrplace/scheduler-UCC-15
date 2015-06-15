package org.btrplace.scheduler.choco.view.net;

import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.view.ModelView;
import org.btrplace.model.view.net.NetworkView;
import org.btrplace.model.view.net.Port;
import org.btrplace.model.view.net.Switch;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.transition.VMTransition;
import org.btrplace.scheduler.choco.view.ChocoModelViewBuilder;
import org.btrplace.scheduler.choco.view.ChocoView;
import org.btrplace.scheduler.choco.view.DelegatedBuilder;
import org.btrplace.scheduler.choco.view.SolverViewBuilder;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.ICF;
import org.chocosolver.solver.constraints.nary.cumulative.Cumulative;
import org.chocosolver.solver.search.solution.Solution;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;
import org.chocosolver.solver.variables.VF;

import java.util.*;

/**
 * Created by vkherbac on 30/12/14.
 */
public class CNetworkView implements ChocoView {

    /**
     * The view identifier.
     */
    public static final String VIEW_ID = "NetworkView";

    private NetworkView net;
    private ReconfigurationProblem rp;
    private Solver solver;
    private Model source;
    List<Task> tasksList;
    List<IntVar> heightsList;

    public CNetworkView(ReconfigurationProblem p, NetworkView n) throws SchedulerException {
        net = n;
        rp = p;
        solver = p.getSolver();
        source = p.getSourceModel();
        tasksList = new ArrayList<>();
        heightsList = new ArrayList<>();
    }

    @Override
    public String getIdentifier() { return net.getIdentifier(); }

    @Override
    public boolean beforeSolve(ReconfigurationProblem rp) throws SchedulerException {

        // Links limitation
        Map<Port,Port> uniLinks = new HashMap<>(); // Create an exhaustive list of unidirectional links
        for (Port p : net.getAllInterfaces()) {
            // Full duplex (2 cumulatives per link)
            if (!uniLinks.containsKey(p)) {
                uniLinks.put(p, p.getRemote());
            }
            /* Half duplex (1 cumulative per link)
            if (!uniLinks.containsKey(p) && !uniLinks.containsKey(p.getRemote())) {
                uniLinks.put(p, p.getRemote());
            }*/
        }
        for (Port inputPort : uniLinks.keySet()) {

            for (VM vm : rp.getVMs()) {
                VMTransition a = rp.getVMAction(vm);

                if (a != null && a instanceof MigrateVMTransition &&
                        (a.getCSlice().getHoster().getValue() != a.getDSlice().getHoster().getValue())) {

                    Node src = source.getMapping().getVMLocation(vm);
                    Node dst = rp.getNode(a.getDSlice().getHoster().getValue());

                    // If inputPort is on migration path
                    if (net.getPath(src, dst).contains(inputPort)) {

                        // ONLY if inputPort is an INPUT port
                        if (net.getPath(src, dst).indexOf(inputPort) < net.getPath(src, dst).indexOf(uniLinks.get(inputPort))) {
                            tasksList.add(new Task(a.getStart(), a.getDuration(), a.getEnd()));
                            heightsList.add(((MigrateVMTransition) a).getBandwidth());
                        }
                    }
                }
            }
            if (!tasksList.isEmpty()) {
                solver.post(new Cumulative(
                        tasksList.toArray(new Task[tasksList.size()]),
                        heightsList.toArray(new IntVar[heightsList.size()]),
                        VF.fixed(Math.min(inputPort.getBandwidth(), uniLinks.get(inputPort).getBandwidth()), solver),
                        true
                        ,Cumulative.Filter.TIME
                        //,Cumulative.Filter.SWEEP
                        //,Cumulative.Filter.SWEEP_HEI_SORT
                        ,Cumulative.Filter.NRJ
                        ,Cumulative.Filter.HEIGHTS
                ));
            }
            tasksList.clear();
            heightsList.clear();
        }

        // Switches capacity limitation
        for(Switch sw : net.getSwitches()) {

            // Only if the capacity is limited
            if (sw.getCapacity() > 0) {

                for (VM vm : rp.getVMs()) {
                    VMTransition a = rp.getVMAction(vm);

                    if (a != null && a instanceof MigrateVMTransition &&
                            (a.getCSlice().getHoster().getValue() != a.getDSlice().getHoster().getValue())) {

                        Node src = source.getMapping().getVMLocation(vm);
                        Node dst = rp.getNode(a.getDSlice().getHoster().getValue());

                        if (!Collections.disjoint(sw.getPorts(), net.getPath(src, dst))) {
                            tasksList.add(new Task(a.getStart(), a.getDuration(), a.getEnd()));
                            heightsList.add(((MigrateVMTransition) a).getBandwidth());
                        }
                    }
                }

                solver.post(ICF.cumulative(
                        tasksList.toArray(new Task[tasksList.size()]),
                        heightsList.toArray(new IntVar[heightsList.size()]),
                        VF.fixed(sw.getCapacity(), solver),
                        true
                ));

                tasksList.clear();
                heightsList.clear();
            }
        }

        return true;
    }

    @Override
    public boolean insertActions(ReconfigurationProblem rp, Solution s, ReconfigurationPlan p) { return true; }

    @Override
    public boolean cloneVM(VM vm, VM clone) { return true; }

    /**
     * Builder associated to the constraint.
     */
    public static class Builder implements ChocoModelViewBuilder {
        @Override
        public Class<? extends ModelView> getKey() {
            return NetworkView.class;
        }

        @Override
        public SolverViewBuilder build(final ModelView v) throws SchedulerException {
            return new DelegatedBuilder(v.getIdentifier(), Collections.emptyList()) {
                @Override
                public ChocoView build(ReconfigurationProblem r) throws SchedulerException {
                    return new CNetworkView(r, (NetworkView) v);
                }
            };
        }
    }
}
