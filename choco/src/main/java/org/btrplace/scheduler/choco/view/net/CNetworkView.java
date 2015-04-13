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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public List<List<MigrateVMTransition>> getMigrationsPerLink() {

        List<MigrateVMTransition> migrationsList = new ArrayList<>();
        List<List<MigrateVMTransition>> migrationsPerLink = new ArrayList<>();

        List<Port> links = new ArrayList<>();
        for (Port si : net.getAllInterfaces()) {
            if (!links.contains(si) && !links.contains(si.getRemote())) {
                links.add(si);
                for (VM vm : rp.getVMs()) {
                    VMTransition a = rp.getVMAction(vm);

                    if (a != null && a instanceof MigrateVMTransition &&
                            (a.getCSlice().getHoster().getValue() != a.getDSlice().getHoster().getValue())) {

                        Node src = source.getMapping().getVMLocation(vm);
                        Node dst = rp.getNode(a.getDSlice().getHoster().getValue());

                        if (net.getPath(src, dst).contains(si)) {
                            migrationsList.add((MigrateVMTransition)a);
                        }
                    }
                }
                if (!migrationsList.isEmpty()) migrationsPerLink.add(new ArrayList<MigrateVMTransition>(migrationsList));
                migrationsList.clear();
            }
        }
        return migrationsPerLink;
    }

    @Override
    public String getIdentifier() { return net.getIdentifier(); }

    @Override
    public boolean beforeSolve(ReconfigurationProblem rp) throws SchedulerException {

        List<Port> links = new ArrayList<>();

        // Interfaces limitation
        for(Port si : net.getAllInterfaces()) {

            // Only add one cumulative per link (local+remote ports)
            if (!links.contains(si) && !links.contains(si.getRemote())) {
                links.add(si);

                for (VM vm : rp.getVMs()) {
                    VMTransition a = rp.getVMAction(vm);

                    if (a != null && a instanceof MigrateVMTransition &&
                            (a.getCSlice().getHoster().getValue() != a.getDSlice().getHoster().getValue())) {

                        Node src = source.getMapping().getVMLocation(vm);
                        Node dst = rp.getNode(a.getDSlice().getHoster().getValue());

                        if (net.getPath(src, dst).contains(si)) {
                            tasksList.add(new Task(a.getStart(), a.getDuration(), a.getEnd()));
                            heightsList.add(((MigrateVMTransition) a).getBandwidth());
                        }
                    }
                }
                if (!tasksList.isEmpty()) {
                    solver.post(new Cumulative(
                            tasksList.toArray(new Task[tasksList.size()]),
                            heightsList.toArray(new IntVar[heightsList.size()]),
                            VF.fixed(si.getBandwidth(), solver),
                            true
                            ,Cumulative.Filter.TIME
                            //,Cumulative.Filter.SWEEP
                            //,Cumulative.Filter.SWEEP_HEI_SORT
                            ,Cumulative.Filter.NRJ
                            ,Cumulative.Filter.HEIGHTS
                    ));
                }
                //tasksPerLink.add(new ArrayList<Task>(tasksList));

                tasksList.clear();
                heightsList.clear();
            }
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
