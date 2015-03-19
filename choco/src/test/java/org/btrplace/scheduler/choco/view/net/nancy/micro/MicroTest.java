package org.btrplace.scheduler.choco.view.net.nancy.micro;

import net.minidev.json.JSONObject;
import org.btrplace.json.JSONConverterException;
import org.btrplace.json.plan.ReconfigurationPlanConverter;
import org.btrplace.model.*;
import org.btrplace.model.constraint.Fence;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.model.constraint.migration.Sync;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.model.view.net.MinMTTRObjective;
import org.btrplace.model.view.net.NetworkView;
import org.btrplace.model.view.net.Switch;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.gantt.ActionsToCSV;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.DefaultChocoScheduler;
import org.btrplace.scheduler.choco.DefaultParameters;
import org.btrplace.scheduler.choco.constraint.migration.CSync;
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
public class MicroTest {

    @Test
    public void IntraNodeTest() throws SchedulerException,ContradictionException {

        String path = new File("").getAbsolutePath() +
                "/choco/src/test/java/org/btrplace/scheduler/choco/view/net/nancy/micro/";

        // Set nb of nodes and vms
        int nbSrcNodes = 1;
        int nbVMsPerNode = 4;
        int nbVMs = nbSrcNodes * nbVMsPerNode;

        // Set mem + cpu for Nodes and VMs
        int mem_src_node = 16, cpu_src_node = 8;
        int mem_dst_node = 16, cpu_dst_node = 8;
        int mem_vm_type1 = 2, cpu_vm_type1 = 2;
        int mem_vm_type2 = 4, cpu_vm_type2 = 2;

        // Set memoryUsed and dirtyRate (for all VMs)
        int vmType1_mem = 1000, vmType2_mem = 2000;
        double dirtyRateHigh = 40; // 21.44 mB/s,
        double dirtyRateLow = 0; // 1.5 mB/s,

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
        for (int i=0; i<nbSrcNodes; i++) { dstNodes.add(mo.newNode()); ma.addOnlineNode(dstNodes.get(i)); }


        // Add resource views
        ShareableResource rcMem = new ShareableResource("mem", 0, 0);
        ShareableResource rcCPU = new ShareableResource("cpu", 0, 0);
        for (Node n : srcNodes) { rcMem.setCapacity(n, mem_src_node); rcCPU.setCapacity(n, cpu_src_node); }
        for (Node n : dstNodes) { rcMem.setCapacity(n, mem_dst_node); rcCPU.setCapacity(n, cpu_dst_node); }
        
        // Create running VMs on src nodes
        List<VM> vms = new ArrayList<>();

        VM v;
        v = mo.newVM(); vms.add(v);
        rcMem.setConsumption(v, mem_vm_type1); rcCPU.setConsumption(v, cpu_vm_type1);
        ma.addRunningVM(v, srcNodes.get(0));
        mo.getAttributes().put(v, "memUsed", vmType1_mem);
        mo.getAttributes().put(v, "dirtyRate", dirtyRateLow);
        v = mo.newVM(); vms.add(v);
        rcMem.setConsumption(v, mem_vm_type2); rcCPU.setConsumption(v, cpu_vm_type2);
        ma.addRunningVM(v, srcNodes.get(0));
        mo.getAttributes().put(v, "memUsed", vmType2_mem);
        mo.getAttributes().put(v, "dirtyRate", dirtyRateHigh);
        v = mo.newVM(); vms.add(v);
        rcMem.setConsumption(v, mem_vm_type1); rcCPU.setConsumption(v, cpu_vm_type1);
        ma.addRunningVM(v,srcNodes.get(0));
        mo.getAttributes().put(v, "memUsed", vmType1_mem);
        mo.getAttributes().put(v, "dirtyRate", dirtyRateHigh);
        v = mo.newVM(); vms.add(v);
        rcMem.setConsumption(v, mem_vm_type2); rcCPU.setConsumption(v, cpu_vm_type2);
        ma.addRunningVM(v,srcNodes.get(0));
        mo.getAttributes().put(v, "memUsed", vmType2_mem);
        mo.getAttributes().put(v, "dirtyRate", dirtyRateLow);

        // Attach resources views
        mo.attach(rcMem);
        mo.attach(rcCPU);
        
        for (Node n : dstNodes) { mo.getAttributes().put(n, "boot", 120); /*~2 minutes to boot*/ }
        for (Node n : srcNodes) {  mo.getAttributes().put(n, "shutdown", 30); /*~30 seconds to shutdown*/ }

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
        Switch swMain = net.newSwitch();
        swMain.connect(1000, srcNodes);
        swMain.connect(1000, dstNodes);
        mo.attach(net);
        net.generateDot(path + "topology.dot", false);

        // Set parameters
        DefaultParameters ps = new DefaultParameters();
        ps.setVerbosity(2);
        ps.setTimeLimit(30);
        ps.doOptimize(true);

        // Set the custom migration transition
        ps.getTransitionFactory().remove(ps.getTransitionFactory().getBuilder(VMState.RUNNING, VMState.RUNNING).get(0));
        ps.getTransitionFactory().add(new MigrateVMTransition.Builder());

        // Register the Sync constraint
        ps.getConstraintMapper().register(new CSync.Builder());

        // Register custom objective
        ps.getConstraintMapper().register(new CMinMTTRObjective.Builder());
        //ps.getConstraintMapper().register(new CMinEnergyObjective.Builder());

        // Migrate all VMs to destination nodes
        List<SatConstraint> cstrs = new ArrayList<>();
        for (VM vm : vms) {
            cstrs.add(new Fence(vm, Collections.singleton(dstNodes.get(srcNodes.indexOf(ma.getVMLocation(vm))))));
        }
        //cstrs.add(new Sync(vms));

        // Shutdown src nodes
        //for (Node n : srcNodes) { cstrs.add(new Offline(n)); }

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
                FileWriter file = new FileWriter(path + "micro_intra-node.json");
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

    @Test
    public void InterNodeTest() throws SchedulerException,ContradictionException {

        String path = new File("").getAbsolutePath() +
                "/choco/src/test/java/org/btrplace/scheduler/choco/view/net/nancy/micro/";

        // Set nb of nodes and vms
        int nbSrcNodes = 4;
        int nbDstNodes = 1;
        int nbVMsPerNode = 2;
        int nbVMs = nbSrcNodes * nbVMsPerNode;

        // Set mem + cpu for Nodes and VMs
        int mem_src_node = 16, cpu_src_node = 4;
        int mem_dst_node = 16, cpu_dst_node = 8;
        int mem_vm_type1 = 2, cpu_vm_type1 = 1;
        int mem_vm_type2 = 2, cpu_vm_type2 = 1;

        // Set memoryUsed and dirtyRate (for all VMs)
        int vmType1_mem = 1000, vmType2_mem = 1500;
        double dirtyRateHigh = 30; // 21.44 mB/s,
        double dirtyRateLow = 5; // 1.5 mB/s,

        /* Define nodes and vms attributes in watts
        int nodeIdlePower = 110;
        int vmPower = 15;
        int maxConsumption = (nodeIdlePower*nbSrcNodes*2)+(vmPower*nbVMs)+170;*/

        // New default model
        Model mo = new DefaultModel();
        Mapping ma = mo.getMapping();

        // Create online source and destination nodes
        List<Node> srcNodes = new ArrayList<>(), dstNodes = new ArrayList<>();
        for (int i=0; i<nbSrcNodes; i++) { srcNodes.add(mo.newNode()); ma.addOnlineNode(srcNodes.get(i)); }
        for (int i=0; i<nbDstNodes; i++) { dstNodes.add(mo.newNode()); ma.addOnlineNode(dstNodes.get(i)); }

        // Create running VMs on src nodes
        List<VM> vms = new ArrayList<>();
        //for (int i=0; i<nbVMs; i++) { vms.add(mo.newVM()); ma.addRunningVM(vms.get(i),srcNodes.get(i%nbSrcNodes)); }

        // Add resource views
        ShareableResource rcMem = new ShareableResource("mem", 0, 0);
        ShareableResource rcCPU = new ShareableResource("cpu", 0, 0);
        for (Node n : srcNodes) { rcMem.setCapacity(n, mem_src_node); rcCPU.setCapacity(n, cpu_src_node); }
        for (Node n : dstNodes) { rcMem.setCapacity(n, mem_dst_node); rcCPU.setCapacity(n, cpu_dst_node); }
        
        // Put attributes
        VM v;
        v = mo.newVM(); vms.add(v);
        rcMem.setConsumption(v, mem_vm_type1); rcCPU.setConsumption(v, cpu_vm_type1);
        ma.addRunningVM(v, srcNodes.get(0));
        mo.getAttributes().put(v, "memUsed", vmType1_mem);
        mo.getAttributes().put(v, "dirtyRate", dirtyRateLow);
        v = mo.newVM(); vms.add(v);
        rcMem.setConsumption(v, mem_vm_type2); rcCPU.setConsumption(v, cpu_vm_type2);
        ma.addRunningVM(v, srcNodes.get(0));
        mo.getAttributes().put(v, "memUsed", vmType2_mem);
        mo.getAttributes().put(v, "dirtyRate", dirtyRateHigh);
        v = mo.newVM(); vms.add(v);
        rcMem.setConsumption(v, mem_vm_type1); rcCPU.setConsumption(v, cpu_vm_type1);
        ma.addRunningVM(v, srcNodes.get(1));
        mo.getAttributes().put(v, "memUsed", vmType1_mem);
        mo.getAttributes().put(v, "dirtyRate", dirtyRateHigh);
        v = mo.newVM(); vms.add(v);
        rcMem.setConsumption(v, mem_vm_type2); rcCPU.setConsumption(v, cpu_vm_type2);
        ma.addRunningVM(v, srcNodes.get(1));
        mo.getAttributes().put(v, "memUsed", vmType2_mem);
        mo.getAttributes().put(v, "dirtyRate", dirtyRateLow);
        v = mo.newVM(); vms.add(v);
        rcMem.setConsumption(v, mem_vm_type1); rcCPU.setConsumption(v, cpu_vm_type1);
        ma.addRunningVM(v, srcNodes.get(2));
        mo.getAttributes().put(v, "memUsed", vmType1_mem);
        mo.getAttributes().put(v, "dirtyRate", dirtyRateLow);
        v = mo.newVM(); vms.add(v);
        rcMem.setConsumption(v, mem_vm_type2); rcCPU.setConsumption(v, cpu_vm_type2);
        ma.addRunningVM(v, srcNodes.get(2));
        mo.getAttributes().put(v, "memUsed", vmType2_mem);
        mo.getAttributes().put(v, "dirtyRate", dirtyRateHigh);
        v = mo.newVM(); vms.add(v);
        rcMem.setConsumption(v, mem_vm_type1); rcCPU.setConsumption(v, cpu_vm_type1);
        ma.addRunningVM(v, srcNodes.get(3));
        mo.getAttributes().put(v, "memUsed", vmType1_mem);
        mo.getAttributes().put(v, "dirtyRate", dirtyRateHigh);
        v = mo.newVM(); vms.add(v);
        rcMem.setConsumption(v, mem_vm_type2); rcCPU.setConsumption(v, cpu_vm_type2);
        ma.addRunningVM(v, srcNodes.get(3));
        mo.getAttributes().put(v, "memUsed", vmType2_mem);
        mo.getAttributes().put(v, "dirtyRate", dirtyRateLow);
        
        // Attach resources view
        mo.attach(rcMem);
        mo.attach(rcCPU);

        for (Node n : dstNodes) { mo.getAttributes().put(n, "boot", 120); /*~2 minutes to boot*/ }
        for (Node n : srcNodes) {  mo.getAttributes().put(n, "shutdown", 30); /*~30 seconds to shutdown*/ }

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
        Switch swMain = net.newSwitch();
        swMain.connect(500, srcNodes);
        swMain.connect(1000, dstNodes);
        mo.attach(net);
        net.generateDot(path + "topology.dot", false);

        // Set parameters
        DefaultParameters ps = new DefaultParameters();
        ps.setVerbosity(2);
        ps.setTimeLimit(60);
        ps.doOptimize(true);

        // Set the custom migration transition
        ps.getTransitionFactory().remove(ps.getTransitionFactory().getBuilder(VMState.RUNNING, VMState.RUNNING).get(0));
        ps.getTransitionFactory().add(new MigrateVMTransition.Builder());

        // Register custom objective
        ps.getConstraintMapper().register(new CMinMTTRObjective.Builder());
        ps.getConstraintMapper().register(new CSync.Builder());
        
        //ps.getConstraintMapper().register(new CMinEnergyObjective.Builder());

        // Migrate all VMs to destination nodes
        List<SatConstraint> cstrs = new ArrayList<>();
        for (VM vm : vms) {
            cstrs.add(new Fence(vm, Collections.singleton(dstNodes.get(0))));
        }
        cstrs.add(new Sync(vms.get(0), vms.get(4)));
        cstrs.add(new Sync(vms.get(1), vms.get(5)));
        cstrs.add(new Sync(vms.get(2), vms.get(6)));
        cstrs.add(new Sync(vms.get(3), vms.get(7)));

        // Shutdown src nodes
        //for (Node n : srcNodes) { cstrs.add(new Offline(n)); }

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
                FileWriter file = new FileWriter(path + "micro_inter-node.json");
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
