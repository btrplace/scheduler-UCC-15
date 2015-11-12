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

    @Override
    public String getIdentifier() { return net.getIdentifier(); }

    @Override
    public boolean beforeSolve(ReconfigurationProblem rp) throws SchedulerException {
        
        Model mo = rp.getSourceModel();
        Solver s = rp.getSolver();
        
        // Pre-compute duration and bandwidth for each VM migration
        for (VMTransition migration : rp.getVMActions()) {

            if (migration instanceof MigrateVMTransition) {
                
                VM vm = migration.getVM();
                IntVar bandwidth, duration;
                
                Node src = rp.getSourceModel().getMapping().getVMLocation(vm);
                Node dst = rp.getNode(migration.getDSlice().getHoster().getValue());
                
                if (dst == null) {
                    throw new SchedulerException(null, "Destination node for VM '" + vm + "' is not known !");
                }

                // Check if all attributes are defined
                if (mo.getAttributes().isSet(vm, "memUsed") &&
                        mo.getAttributes().isSet(vm, "dirtyRate") &&
                        mo.getAttributes().isSet(vm, "maxDirtySize") &&
                        mo.getAttributes().isSet(vm, "maxDirtyDuration")) {

                    double dirtyRate;
                    int memUsed, maxDirtyDuration, maxDirtySize;

                    // Get attribute vars
                    memUsed = mo.getAttributes().getInteger(vm, "memUsed");
                    dirtyRate = mo.getAttributes().getDouble(vm, "dirtyRate");
                    maxDirtySize = mo.getAttributes().getInteger(vm, "maxDirtySize");
                    maxDirtyDuration = mo.getAttributes().getInteger(vm, "maxDirtyDuration");

                    // Enumerated BW
                    int bw = net.getMaxBW(src, dst);
                    /*int step = maxBW;
                    List<Integer> bwEnum = new ArrayList<>();
                    for (int i = step; i <= maxBW; i += step) {
                        if (i > (int) (dirtyRate)) {
                            bwEnum.add(i);
                        }
                    }*/

                    // Enumerated duration
                    double durationMin, durationColdPages, durationHotPages, durationTotal;
                    /*List<Integer> durEnum = new ArrayList<>();
                    for (Integer bw : bwEnum) {*/

                        // Cheat a bit, real is less than theoretical !
                        double bandwidth_octet = bw / 9;

                        // Estimate duration
                        durationMin = memUsed / bandwidth_octet;
                        if (durationMin > maxDirtyDuration) {

                            durationColdPages = ((maxDirtySize + ((durationMin - maxDirtyDuration) * dirtyRate)) / (bandwidth_octet - dirtyRate));
                            durationHotPages = ((maxDirtySize / bandwidth_octet) * ((maxDirtySize / maxDirtyDuration) / (bandwidth_octet - (maxDirtySize / maxDirtyDuration))));
                            durationTotal = durationMin + durationColdPages + durationHotPages;
                        } else {
                            durationTotal = durationMin + (((maxDirtySize / maxDirtyDuration) * durationMin) / (bandwidth_octet - (maxDirtySize / maxDirtyDuration)));
                        }
                        /*durEnum.add((int) Math.round(durationTotal));
                    }

                    // Create the enumerated vars
                    bandwidth = VF.enumerated("bandwidth_enum", bwEnum.stream().mapToInt(i -> i).toArray(), s);
                    duration = VF.enumerated("duration_enum", durEnum.stream().mapToInt(i -> i).toArray(), s);*/

                    bandwidth = VF.fixed(bw, s);
                    duration = VF.fixed((int) Math.round(durationTotal), s);
                    
                    // Set the vars in the VM transition
                    ((MigrateVMTransition) migration).setBandwidth(bandwidth);
                    ((MigrateVMTransition) migration).setDuration(duration);

                    /* Associate vars using Tuples
                    Tuples tpl = new Tuples(true);
                    for (int i = 0; i < bwEnum.size(); i++) {
                        tpl.add(bwEnum.get(i), durEnum.get(i));
                    }
                    s.post(ICF.table(bandwidth, duration, tpl, ""));*/
                } else {
                    throw new SchedulerException(null, "Unable to retrieve attributes for the vm '" + vm + "'");
                }
            }
        }
        
        // Links limitation
        for (Port inputPort : net.getAllInterfaces()) {

            for (VM vm : rp.getVMs()) {
                VMTransition a = rp.getVMAction(vm);

                if (a != null && a instanceof MigrateVMTransition &&
                        (a.getCSlice().getHoster().getValue() != a.getDSlice().getHoster().getValue())) {

                    Node src = source.getMapping().getVMLocation(vm);
                    Node dst = rp.getNode(a.getDSlice().getHoster().getValue());
                    
                    List<Port> path = net.getPath(src, dst);

                    // If inputPort is on migration path
                    if (path.contains(inputPort)) {

                        // ONLY if inputPort is an INPUT port
                        if (path.indexOf(inputPort) < path.indexOf(inputPort.getRemote())) {
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
                        VF.fixed(Math.min(inputPort.getBandwidth(), inputPort.getRemote().getBandwidth()), solver),
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
