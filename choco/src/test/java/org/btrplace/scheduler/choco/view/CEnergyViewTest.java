package org.btrplace.scheduler.choco.view;

import org.btrplace.model.*;
import org.btrplace.model.constraint.Ban;
import org.btrplace.model.constraint.MinMTTR;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.model.view.net.NetworkView;
import org.btrplace.model.view.power.EnergyView;
import org.btrplace.model.view.power.PowerBudget;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.DefaultChocoScheduler;
import org.btrplace.scheduler.choco.DefaultParameters;
import org.btrplace.scheduler.choco.view.power.CPowerBudget;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vins on 11/01/15.
 */
public class CEnergyViewTest {

    @Test
    public void Test() {

        // Config
        int nbSrcNodes = 3;
        int nbVMPerSrcNode = 2;
        int nbVMs = nbSrcNodes * nbVMPerSrcNode;

        // Define nodes capacity and vms consumption
        int cpu_vm = 2, cpu_srcNode = 4,  cpu_dstNode = 6;  // VMs: 2 VCPUs, srcNodes: 8 CPUs, dstNodes: 10 CPUs

        // Define nodes and vms attributes in watts
        int nodeIdlePower = 110;
        int vmPower = 15;
        int maxConsumption = ((nodeIdlePower + (vmPower*nbVMPerSrcNode)) * nbSrcNodes * 2) + 1;

        // New default model
        Model mo = new DefaultModel();
        Mapping ma = mo.getMapping();

        // Create 15 online source nodes + 15 offline destination nodes
        List<Node> srcNodes = new ArrayList<>();
        List<Node> dstNodes = new ArrayList<>();
        for (int i=0; i<nbSrcNodes; i++) { srcNodes.add(mo.newNode()); ma.addOnlineNode(srcNodes.get(i)); }
        for (int i=0; i<nbSrcNodes; i++) { dstNodes.add(mo.newNode()); ma.addOnlineNode(dstNodes.get(i)); }

        // Create running VMs: 4 per source node
        List<VM> vms = new ArrayList<>();
        for (int i=0; i<nbVMs; i++) { vms.add(mo.newVM()); ma.addRunningVM(vms.get(i),srcNodes.get(i%nbSrcNodes)); }

        // Put vms attributes
        for (VM vm : vms) { mo.getAttributes().put(vm, "power", vmPower); }

        // Put nodes attributes
        for (Node n : srcNodes) { mo.getAttributes().put(n, "idlePower", nodeIdlePower); }
        for (Node n : dstNodes) { mo.getAttributes().put(n, "idlePower", nodeIdlePower); }

        // Add CPU resource view
        ShareableResource rcCPU = new ShareableResource("cpu", 0, 0);
        for (Node n : srcNodes) { rcCPU.setCapacity(n, cpu_srcNode); }
        for (Node n : dstNodes) { rcCPU.setCapacity(n, cpu_dstNode); }
        for (VM vm : vms) { rcCPU.setConsumption(vm, cpu_vm); }
        mo.attach(rcCPU);

        // Add the EnergyView
        EnergyView energyView = new EnergyView(maxConsumption);
        mo.attach(energyView);

        NetworkView nv = new NetworkView();
        mo.attach(nv);

        // Set resolution parameters
        DefaultParameters ps = new DefaultParameters();
        ps.setVerbosity(2);
        ps.setTimeLimit(5);
        ps.doOptimize(true);

        // Register new PowerBudget constraint
        ps.getConstraintMapper().register(new CPowerBudget.Builder());


        List<SatConstraint> cstrs = new ArrayList<>();
        for (VM vm : vms) {
            cstrs.add(new Ban(vm, srcNodes));
        }

        // Add a power budget
        cstrs.add(new PowerBudget(0, 100, (2*(nodeIdlePower + (vmPower*3)))));

        // Set the objective
        DefaultChocoScheduler sc = new DefaultChocoScheduler(ps);
        Instance i = new Instance(mo, cstrs,  new MinMTTR());

        // Trying to solve
        try {
            ReconfigurationPlan p = sc.solve(i);
            Assert.assertNotNull(p);
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
