package org.btrplace.scheduler.choco.view.net.nancy;

import net.minidev.json.JSONObject;
import org.btrplace.json.JSONConverterException;
import org.btrplace.json.plan.ReconfigurationPlanConverter;
import org.btrplace.model.*;
import org.btrplace.model.constraint.Fence;
import org.btrplace.model.constraint.Offline;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.model.view.net.MinMTTRObjective;
import org.btrplace.model.view.net.NetworkView;
import org.btrplace.model.view.net.Switch;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.gantt.ActionsToCSV;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.DefaultChocoScheduler;
import org.btrplace.scheduler.choco.DefaultParameters;
import org.btrplace.scheduler.choco.view.net.CMinMTTRObjective;
import org.btrplace.scheduler.choco.view.net.MigrateVMTransition;
import org.chocosolver.solver.exception.ContradictionException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by vkherbac on 19/02/15.
 */
public class NancyG5kTest {

    @Test
    public void Scenario1Test() throws SchedulerException,ContradictionException {

        String path = new File("").getAbsolutePath() +
                "/choco/src/test/java/org/btrplace/scheduler/choco/view/net/nancy/";

        // Set nb of nodes and vms
        int nbSrcNodes = 16;
        int nbVMs = nbSrcNodes * 3;

        // Set mem + cpu for VMs and Nodes
        int mem_vm = 2, mem_node = 16; // VMs: 2GB,     Nodes: 16GB
        int cpu_vm = 1, cpu_srcNode = 4, cpu_dstNode = 8;  // VMs: 1 VCPUs, Nodes: 4 to 8 CPUs

        // Set memoryUsed for all VMs
        int memUsed = 1000; // 1 GB

        /* Define nodes and vms attributes in watts
        int nodeIdlePower = 110;
        int vmPower = 15;
        int maxConsumption = (nodeIdlePower*nbSrcNodes*2)+(vmPower*nbVMs)+170;*/

        // New default model
        Model mo = new DefaultModel();
        Mapping ma = mo.getMapping();

        // Create online source nodes and offline destination nodes
        List<Node> srcNodes = new ArrayList<>(), dstNodes = new ArrayList<>();
        for (int i=0; i<nbSrcNodes; i++) { srcNodes.add(mo.newNode()); ma.addOnlineNode(srcNodes.get(i)); }
        for (int i=0; i<nbSrcNodes; i++) { dstNodes.add(mo.newNode()); ma.addOfflineNode(dstNodes.get(i)); }

        // Create running VMs on src nodes
        List<VM> vms = new ArrayList<>();
        for (int i=0; i<nbVMs; i++) { vms.add(mo.newVM()); ma.addRunningVM(vms.get(i),srcNodes.get(i%nbSrcNodes)); }

        // Put attributes
        for (VM vm : vms) {
            mo.getAttributes().put(vm, "memUsed", memUsed);
            mo.getAttributes().put(vm, "dirtyRate", 3.0);
            mo.getAttributes().put(vm, "maxDirtySize", 96);
            mo.getAttributes().put(vm, "maxDirtyDuration", 2);
        }
        for (Node n : dstNodes) { mo.getAttributes().put(n, "boot", 120); /*~2 minutes to boot*/ }
        for (Node n : srcNodes) {  mo.getAttributes().put(n, "shutdown", 30); /*~30 seconds to shutdown*/ }

        // Add resource views
        ShareableResource rcMem = new ShareableResource("mem", 0, 0);
        ShareableResource rcCPU = new ShareableResource("cpu", 0, 0);
        for (Node n : srcNodes) { rcMem.setCapacity(n, mem_node); rcCPU.setCapacity(n, cpu_srcNode); }
        for (Node n : dstNodes) { rcMem.setCapacity(n, mem_node); rcCPU.setCapacity(n, cpu_dstNode); }
        for (VM vm : vms) { rcMem.setConsumption(vm, mem_vm); rcCPU.setConsumption(vm, cpu_vm); }
        mo.attach(rcMem);
        mo.attach(rcCPU);

        /* Add the EnergyView and set nodes & vms consumption
        EnergyView energyView = new EnergyView(maxConsumption);
        energyView.setMigrationOverhead(40); // 40% energy overhead during migration
        energyView.setBootOverhead(30); // 30% energy overhead during boot
        for (Node n : srcNodes) { energyView.setConsumption(n, nodeIdlePower); }
        for (Node n : dstNodes) { energyView.setConsumption(n, nodeIdlePower); }
        for (VM vm : vms) { energyView.setConsumption(vm, vmPower); }
        mo.attach(energyView);*/

        // Add a NetworkView view using Nancy g5k topology
        NetworkView net = new NetworkView();
        Switch swSrc1 = net.newSwitch();
        swSrc1.connect(1000, srcNodes.subList(0,4));
        Switch swSrc2 = net.newSwitch();
        swSrc2.connect(1000, srcNodes.subList(4,8));
        Switch swSrc3 = net.newSwitch();
        swSrc3.connect(1000, srcNodes.subList(8,12));
        Switch swSrc4 = net.newSwitch();
        swSrc4.connect(1000, srcNodes.subList(12,16));
        Switch swDst = net.newSwitch();
        swDst.connect(1000, dstNodes);
        Switch swMain = net.newSwitch();
        swMain.connect(10000, swSrc1, swSrc2, swSrc3, swSrc4, swDst);
        mo.attach(net);
        net.generateDot(path + "topology.dot", false);

        // Set parameters
        DefaultParameters ps = new DefaultParameters();
        ps.setVerbosity(2);
        ps.setTimeLimit(60);
        ps.doOptimize(true);

        // Set the custom migration transition
        ps.getTransitionFactory().remove(ps.getTransitionFactory().getBuilder(VMState.RUNNING, VMState.RUNNING));
        ps.getTransitionFactory().add(new MigrateVMTransition.Builder());

        // Register custom objective
        ps.getConstraintMapper().register(new CMinMTTRObjective.Builder());
        //ps.getConstraintMapper().register(new CMinEnergyObjective.Builder());

        // Migrate all VMs to destination nodes
        List<SatConstraint> cstrs = new ArrayList<>();
        for (VM vm : vms) {
            cstrs.add(new Fence(vm, Collections.singleton(dstNodes.get(srcNodes.indexOf(ma.getVMLocation(vm))))));
        }

        // Shutdown src nodes
        for (Node n : srcNodes) { cstrs.add(new Offline(n)); }

        /* Register new PowerBudget constraint and add continuous power budgets
        ps.getConstraintMapper().register(new CPowerBudget.Builder());
        cstrs.add(new PowerBudget(125, 500, 1430));*/

        // Set a custom objective
        DefaultChocoScheduler sc = new DefaultChocoScheduler(ps);
        Instance i = new Instance(mo, cstrs,  new MinMTTRObjective());

        ReconfigurationPlan p;
        try {
            p = sc.solve(i);
            Assert.assertNotNull(p);

            ReconfigurationPlanConverter planConverter = new ReconfigurationPlanConverter();
            JSONObject obj = null;
            try {
                obj =  planConverter.toJSON(p);
            } catch (JSONConverterException e) {
                System.err.println("Error while converting plan: " + e.toString());
                e.printStackTrace();
            }
            try {
                FileWriter file = new FileWriter(path + "nancy_scenario1.json");
                file.write(obj.toJSONString());
                file.flush();
                file.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            ActionsToCSV.convert(p.getActions(), path + "actions.csv");
            //energyView.plotConsumption(p, path + "energy.csv");
            System.err.println(p);
            System.err.flush();

        } finally {
            System.err.println(sc.getStatistics());
        }
    }
}
