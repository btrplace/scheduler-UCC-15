package org.btrplace.scheduler.choco.view.energy;

import org.btrplace.model.*;
import org.btrplace.model.constraint.Fence;
import org.btrplace.model.constraint.MinMTTR;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.model.view.power.EnergyView;
import org.btrplace.model.view.power.PowerBudget;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.gantt.ActionsToCSV;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.DefaultChocoScheduler;
import org.btrplace.scheduler.choco.DefaultParameters;
import org.btrplace.scheduler.choco.view.power.CPowerBudget;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by vins on 11/01/15.
 */
public class CEnergyViewTest {

    @Test
    public void DiscreteTest() {

        String path = new File("").getAbsolutePath() +
                "/choco/src/test/java/org/btrplace/scheduler/choco/view/net/";

        // Config
        int nbSrcNodes = 3;
        int nbVMPerSrcNode = 2;
        int nbVMs = nbSrcNodes * nbVMPerSrcNode;

        // Define nodes capacity and vms consumption
        int cpu_vm = 2, cpu_srcNode = 4,  cpu_dstNode = 6;  // VMs: 2 VCPUs, srcNodes: 8 CPUs, dstNodes: 10 CPUs

        // Define nodes and vms attributes in watts
        int nodeIdlePower = 110;
        int vmPower = 15;
        int maxConsumption = ((nodeIdlePower + (vmPower*nbVMPerSrcNode)) * nbSrcNodes) + (nodeIdlePower*nbSrcNodes);

        // New default model
        Model mo = new DefaultModel();
        Mapping ma = mo.getMapping();

        // Create source and destination nodes
        List<Node> srcNodes = new ArrayList<>(), dstNodes = new ArrayList<>();
        for (int i=0; i<nbSrcNodes; i++) { srcNodes.add(mo.newNode()); ma.addOnlineNode(srcNodes.get(i)); }
        for (int i=0; i<nbSrcNodes; i++) { dstNodes.add(mo.newNode()); ma.addOnlineNode(dstNodes.get(i)); }

        // Create running VMs: 4 per source node
        List<VM> vms = new ArrayList<>();
        for (int i=0; i<nbVMs; i++) { vms.add(mo.newVM()); ma.addRunningVM(vms.get(i),srcNodes.get(i%nbSrcNodes)); }

        // Add CPU resource view
        ShareableResource rcCPU = new ShareableResource("cpu", 0, 0);
        for (Node n : srcNodes) { rcCPU.setCapacity(n, cpu_srcNode); }
        for (Node n : dstNodes) { rcCPU.setCapacity(n, cpu_dstNode); }
        for (VM vm : vms) { rcCPU.setConsumption(vm, cpu_vm); }
        mo.attach(rcCPU);

        // Set custom boot and shutdown durations
        for (Node n : dstNodes) { mo.getAttributes().put(n, "boot", 2); /*~2 minutes to boot*/ }
        for (Node n : srcNodes) {  mo.getAttributes().put(n, "shutdown", 2); /*~30 seconds to shutdown*/ }

        // Add the EnergyView and set nodes & vms consumption
        EnergyView energyView = new EnergyView(maxConsumption);
        energyView.setMigrationOverhead(40); // 40% energy overhead during migration
        energyView.setBootOverhead(30); // 30% energy overhead during boot
        for (Node n : srcNodes) { energyView.setConsumption(n, nodeIdlePower); }
        for (Node n : dstNodes) { energyView.setConsumption(n, nodeIdlePower); }
        for (VM vm : vms) { energyView.setConsumption(vm, vmPower); }
        mo.attach(energyView);

        // Set resolution parameters
        DefaultParameters ps = new DefaultParameters();
        ps.setVerbosity(2);
        ps.setTimeLimit(5);
        ps.doOptimize(false);

        // Register new PowerBudget constraint
        ps.getConstraintMapper().register(new CPowerBudget.Builder());

        // Add a discrete power budget (2 dst nodes / 3 VMs per node)
        List<SatConstraint> cstrs = new ArrayList<>();
        cstrs.add(new PowerBudget((2*nodeIdlePower)+(vmPower*nbVMPerSrcNode*nbSrcNodes)));

        // Set the objective
        DefaultChocoScheduler sc = new DefaultChocoScheduler(ps);
        Instance i = new Instance(mo, cstrs,  new MinMTTR());

        // Trying to solve
        try {
            ReconfigurationPlan p = sc.solve(i);
            Assert.assertNotNull(p);
            ActionsToCSV.convert(p.getActions(), path + "actions.csv");
            System.err.println(p);
            System.err.flush();
        } catch (SchedulerException e) {
            e.printStackTrace();
        } finally {
            System.err.println(sc.getStatistics());
            System.err.flush();
        }
        //Assert.fail();
    }

    @Test
    public void ContinuousTest() {

        String path = new File("").getAbsolutePath() +
                "/choco/src/test/java/org/btrplace/scheduler/choco/view/net/";

        // Config
        int nbSrcNodes = 5;
        int nbVMPerSrcNode = 2;
        int nbVMs = nbSrcNodes * nbVMPerSrcNode;

        // Define nodes and vms attributes in watts
        int nodeIdlePower = 110;
        int vmPower = 15;
        int maxConsumption = ((nodeIdlePower+(vmPower*nbVMPerSrcNode))*nbSrcNodes) + (nodeIdlePower*nbSrcNodes) + 55;
        //int maxConsumption = Integer.MAX_VALUE;

        // New default model
        Model mo = new DefaultModel();
        Mapping ma = mo.getMapping();

        // Create source and destination nodes
        List<Node> srcNodes = new ArrayList<>(), dstNodes = new ArrayList<>();
        for (int i=0; i<nbSrcNodes; i++) { srcNodes.add(mo.newNode()); ma.addOnlineNode(srcNodes.get(i)); }
        for (int i=0; i<nbSrcNodes; i++) { dstNodes.add(mo.newNode()); ma.addOfflineNode(dstNodes.get(i)); }

        // Create running VMs: 4 per source node
        List<VM> vms = new ArrayList<>();
        for (int i=0; i<nbVMs; i++) { vms.add(mo.newVM()); ma.addRunningVM(vms.get(i),srcNodes.get(i%nbSrcNodes)); }

        // Set custom boot and shutdown durations
        for (Node n : dstNodes) { mo.getAttributes().put(n, "boot", 2); /*~2 minutes to boot*/ }
        for (Node n : srcNodes) {  mo.getAttributes().put(n, "shutdown", 2); /*~30 seconds to shutdown*/ }

        // Add the EnergyView and set nodes & vms consumption
        EnergyView energyView = new EnergyView(maxConsumption);
        energyView.setMigrationOverhead(40); // 40% energy overhead during migration
        energyView.setBootOverhead(30); // 30% energy overhead during boot
        for (Node n : srcNodes) { energyView.setConsumption(n, nodeIdlePower); }
        for (Node n : dstNodes) { energyView.setConsumption(n, nodeIdlePower); }
        for (VM vm : vms) { energyView.setConsumption(vm, vmPower); }
        mo.attach(energyView);

        // Set resolution parameters
        DefaultParameters ps = new DefaultParameters();
        ps.setVerbosity(2);
        ps.setTimeLimit(5);
        ps.doOptimize(false);

        // Force symmetric migrations
        List<SatConstraint> cstrs = new ArrayList<>();
        for (VM vm : vms) {
            cstrs.add(new Fence(vm, Collections.singleton(dstNodes.get(srcNodes.indexOf(ma.getVMLocation(vm))))));
        }

        // Register new PowerBudget constraint and add continuous power budgets
        ps.getConstraintMapper().register(new CPowerBudget.Builder());
        cstrs.add(new PowerBudget(0, 100, ((nbSrcNodes+1)*(nodeIdlePower+(vmPower*nbVMPerSrcNode)))+55));
        //cstrs.add(new PowerBudget(0, 2, 200));

        // Set the objective
        DefaultChocoScheduler sc = new DefaultChocoScheduler(ps);
        Instance i = new Instance(mo, cstrs,  new MinMTTR());

        // Trying to solve
        try {
            ReconfigurationPlan p = sc.solve(i);
            Assert.assertNotNull(p);
            ActionsToCSV.convert(p.getActions(), path + "actions.csv");
            System.err.println(p);
            System.err.flush();
        } catch (SchedulerException e) {
            e.printStackTrace();
        } finally {
            System.err.println(sc.getStatistics());
            System.err.flush();
        }
        //Assert.fail();
    }
}
