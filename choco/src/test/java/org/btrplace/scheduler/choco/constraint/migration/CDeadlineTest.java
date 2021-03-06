package org.btrplace.scheduler.choco.constraint.migration;

import org.btrplace.model.*;
import org.btrplace.model.constraint.Fence;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.model.constraint.migration.Deadline;
import org.btrplace.model.view.net.MinMTTRObjective;
import org.btrplace.model.view.net.NetworkView;
import org.btrplace.model.view.net.Switch;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.event.Action;
import org.btrplace.plan.gantt.ActionsToCSV;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.DefaultChocoScheduler;
import org.btrplace.scheduler.choco.DefaultParameters;
import org.btrplace.scheduler.choco.view.net.CMinMTTRObjective;
import org.btrplace.scheduler.choco.view.net.MigrateVMTransition;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by vkherbac on 23/01/15.
 */
public class CDeadlineTest {

    @Test
    public void test() throws SchedulerException {

        String path = new File("").getAbsolutePath() +
                "/choco/src/test/java/org/btrplace/scheduler/choco/view/net/";

        // Set nb of nodes and vms
        int nbSrcNodes = 20;
        int nbVMs = nbSrcNodes * 4;

        // Set memoryUsed and dirtyRate (for all VMs)
        int memUsed = 1000; // 1 GB
        double dirtyRate = 21.44; // 21.44 mB/s,

        // New default model
        Model mo = new DefaultModel();
        Mapping ma = mo.getMapping();

        // Create src and dst nodes
        List<Node> srcNodes = new ArrayList<>(), dstNodes = new ArrayList<>();
        for (int i=0; i<nbSrcNodes; i++) { srcNodes.add(mo.newNode()); ma.addOnlineNode(srcNodes.get(i)); }
        for (int i=0; i<nbSrcNodes; i++) { dstNodes.add(mo.newNode()); ma.addOnlineNode(dstNodes.get(i)); }

        // Create running VMs on src nodes
        List<VM> vms = new ArrayList<>();
        for (int i=0; i<nbVMs; i++) { vms.add(mo.newVM()); ma.addRunningVM(vms.get(i),srcNodes.get(i%nbSrcNodes)); }

        // Put attributes
        for (VM vm : vms) {
            mo.getAttributes().put(vm, "memUsed", memUsed);
            mo.getAttributes().put(vm, "dirtyRate", dirtyRate);
        }
        for (Node n : dstNodes) { mo.getAttributes().put(n, "boot", 120); /*~2 minutes to boot*/ }
        for (Node n : srcNodes) {  mo.getAttributes().put(n, "shutdown", 30); /*~30 seconds to shutdown*/ }

        // Add a simple network
        NetworkView net = new NetworkView();
        Switch s1 = net.newSwitch();
        Switch sm = net.newSwitch();
        Switch s2 = net.newSwitch();
        s1.connect(1000, srcNodes);
        s2.connect(1000, dstNodes);
        sm.connect(1000, s1, s2);
        mo.attach(net);
        //net.generateDot(path + "topology.dot", false);

        // Set parameters
        DefaultParameters ps = new DefaultParameters();
        ps.setVerbosity(1);
        ps.setTimeLimit(50);
        //ps.setMaxEnd(800);
        ps.doOptimize(false);

        // Add the custom vm transition builder for migrations
        ps.getTransitionFactory().remove(ps.getTransitionFactory().getBuilder(VMState.RUNNING, VMState.RUNNING));
        ps.getTransitionFactory().add(new MigrateVMTransition.Builder());

        // Register custom objective
        ps.getConstraintMapper().register(new CMinMTTRObjective.Builder());

        // Register the Deadline constraint
        ps.getConstraintMapper().register(new CDeadline.Builder());

        // Migrate all VMs to destination nodes
        List<SatConstraint> cstrs = new ArrayList<>();
        for (VM vm : vms) {
            cstrs.add(new Fence(vm, Collections.singleton(dstNodes.get(srcNodes.indexOf(ma.getVMLocation(vm))))));
        }

        // Add the Deadline Constraint (migrate the 'last' VM at the beginning)
        cstrs.add(new Deadline(vms.get(vms.size()-1), "00:00:10"));

        // Set the custom objective
        DefaultChocoScheduler sc = new DefaultChocoScheduler(ps);
        Instance i = new Instance(mo, cstrs, new MinMTTRObjective());

        // Trying to solve
        ReconfigurationPlan p;
        try {
            p = sc.solve(i);

            // A solution must be found
            Assert.assertNotNull(p);

            // Verify that the last migration (id) end before the specified deadline
            for (Action a : p.getActions()) {
                if (a.toString().contains("vm#"+vms.get(vms.size()-1))) {
                    Assert.assertTrue(a.getEnd() <= 10);
                }
            }

            ActionsToCSV.convert(p.getActions(), path + "actions.csv");
            System.err.println(p);
            System.err.flush();
        } catch (SchedulerException e) {
            e.printStackTrace();
        } finally {
            System.err.println(sc.getStatistics());
            System.err.flush();
        }
    }
}
