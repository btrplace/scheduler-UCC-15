package org.btrplace.scheduler.choco.view.net;

import org.btrplace.model.*;
import org.btrplace.model.constraint.Fence;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.model.view.net.MaxBWObjective;
import org.btrplace.model.view.net.NetworkView;
import org.btrplace.model.view.net.Switch;
import org.btrplace.model.view.net.VHPCStaticRouting;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.event.Action;
import org.btrplace.plan.gantt.ActionsToCSV;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.*;
import org.btrplace.scheduler.choco.constraint.mttr.CMinMTTR;
import org.btrplace.scheduler.choco.view.net.CMaxBWObjective;
import org.btrplace.scheduler.choco.view.net.MigrateVMTransition;
import org.chocosolver.solver.Cause;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.impl.IntervalIntVarImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by vkherbac on 30/12/14.
 */
public class CNetworkViewTest {

    @Test
    public void SwitchCapacityBottleneckTest() throws SchedulerException,ContradictionException {

        // Init bandwidth, memoryUsed and dirtyRate for each VM
        int     bw_vm1 = 0,     bw_vm2 = 0;
        int     mem_vm1 = 1000, mem_vm2 = 1500;
        double  dr_vm1 = 21.44, dr_vm2 = 19.28;

        Model mo = new DefaultModel();
        Mapping ma = mo.getMapping();

        VM vm1 = mo.newVM();
        VM vm2 = mo.newVM();
        Node n1 = mo.newNode();
        Node n2 = mo.newNode();
        Node n3 = mo.newNode();

        ma.addOnlineNode(n1);
        ma.addOnlineNode(n2);
        ma.addOnlineNode(n3);
        ma.addRunningVM(vm1, n1);
        ma.addRunningVM(vm2, n2);

        // Put vm attributes, TODO: use a real percentage from shareableResource 'mem'
        mo.getAttributes().put(vm1, "memUsed", mem_vm1); // Go
        mo.getAttributes().put(vm2, "memUsed", mem_vm2);
        mo.getAttributes().put(vm1, "dirtyRate", dr_vm1); // octets/s
        mo.getAttributes().put(vm2, "dirtyRate", dr_vm2);

        // Add a resource view
        ShareableResource rc = new ShareableResource("mem", 0, 0);
        rc.setConsumption(vm1, 2);
        rc.setConsumption(vm2, 3);
        rc.setCapacity(n1, 2);
        rc.setCapacity(n2, 3);
        rc.setCapacity(n3, 5);
        mo.attach(rc);

        // Add a simple network
        NetworkView net = new NetworkView();
        Switch s1 = net.newSwitch(2000);
        Switch sm = net.newSwitch(1000); // Bottleneck
        Switch s2 = net.newSwitch(2000);
        mo.attach(net);

        // Connect switches and nodes
        s1.connect(1000, n1, n2);
        s2.connect(2000, n3);
        sm.connect(2000, s1, s2);

        // Set the custom transition
        Parameters ps = new DefaultParameters().setVerbosity(10);
        ps.getTransitionFactory().remove(ps.getTransitionFactory().getBuilder(VMState.RUNNING, VMState.RUNNING).get(0));
        ps.getTransitionFactory().add(new MigrateVMTransition.Builder());

        // New reconfiguration problem
        ReconfigurationProblem rp = new DefaultReconfigurationProblemBuilder(mo)
                .setParams(ps)
                .build();

        // Migrate VMs to n3
        MigrateVMTransition mig1 = (MigrateVMTransition) rp.getVMAction(vm1);
        MigrateVMTransition mig2 = (MigrateVMTransition) rp.getVMAction(vm2);
        mig1.getDSlice().getHoster().instantiateTo(rp.getNode(n3), Cause.Null);
        mig2.getDSlice().getHoster().instantiateTo(rp.getNode(n3), Cause.Null);

        // Set objective
        CMinMTTR obj = new CMinMTTR();
        obj.inject(rp);

        // Solve
        ReconfigurationPlan p = rp.solve(0, false);

        Assert.assertNotNull(p);

        // Get allocated BW for each migration
        for (Variable v : rp.getSolver().getVars()) {
            if (v.getName().contains("bandwidth")) {
                if (v.getName().contains("vm#0")) {
                    bw_vm1 = ((IntervalIntVarImpl) v).getValue();
                }
                if (v.getName().contains("vm#1")) {
                    bw_vm2 = ((IntervalIntVarImpl) v).getValue();
                }
            }
        }

        // Verify migrations duration
        for (Action a : p.getActions()) {
            if (a.toString().contains("migrate")) {
                if (a.toString().contains("vm#0")) {
                    Assert.assertTrue((a.getEnd()-a.getStart()) == (int)(double)((mem_vm1*8)/(bw_vm1-(int)(double)(dr_vm1*8))));
                }
                if (a.toString().contains("vm#1")) {
                    Assert.assertTrue((a.getEnd()-a.getStart()) == (int)(double)((mem_vm2*8)/(bw_vm2-(int)(double)(dr_vm2*8))));
                }
            }
        }
    }

    @Test
    public void SwitchPortBottleneckTest() throws SchedulerException,ContradictionException {

        // Init bandwidth, memoryUsed and dirtyRate for each VM
        int     bw_vm1 = 0,     bw_vm2 = 0;
        int     mem_vm1 = 1000, mem_vm2 = 1500;
        double  dr_vm1 = 21.44, dr_vm2 = 19.28;

        Model mo = new DefaultModel();
        Mapping ma = mo.getMapping();

        VM vm1 = mo.newVM();
        VM vm2 = mo.newVM();
        Node n1 = mo.newNode();
        Node n2 = mo.newNode();
        Node n3 = mo.newNode();

        ma.addOnlineNode(n1);
        ma.addOnlineNode(n2);
        ma.addOnlineNode(n3);
        ma.addRunningVM(vm1, n1);
        ma.addRunningVM(vm2, n2);

        // Put vm attributes
        mo.getAttributes().put(vm1, "memUsed", mem_vm1); // Go
        mo.getAttributes().put(vm2, "memUsed", mem_vm2);
        mo.getAttributes().put(vm1, "dirtyRate", dr_vm1); // octets/s
        mo.getAttributes().put(vm2, "dirtyRate", dr_vm2);

        // Add a resource view
        ShareableResource rc = new ShareableResource("mem", 0, 0);
        rc.setConsumption(vm1, 2);
        rc.setConsumption(vm2, 3);
        rc.setCapacity(n1, 2);
        rc.setCapacity(n2, 3);
        rc.setCapacity(n3, 5);
        mo.attach(rc);

        // Add a simple network
        NetworkView net = new NetworkView();
        Switch s1 = net.newSwitch(2000);
        Switch sm = net.newSwitch(2000);
        Switch s2 = net.newSwitch(2000);
        mo.attach(net);

        // Connect switches and nodes
        s1.connect(1000, n1, n2);
        s2.connect(2000, n3);
        sm.connect(2000, s1);
        sm.connect(1000, s2); // Link bottleneck between two switches

        // Set the custom transition
        Parameters ps = new DefaultParameters().setVerbosity(10);
        ps.getTransitionFactory().remove(ps.getTransitionFactory().getBuilder(VMState.RUNNING, VMState.RUNNING).get(0));
        ps.getTransitionFactory().add(new MigrateVMTransition.Builder());

        // New reconfiguration problem
        ReconfigurationProblem rp = new DefaultReconfigurationProblemBuilder(mo)
                .setParams(ps)
                .build();

        // Migrate VMs to n3
        MigrateVMTransition mig1 = (MigrateVMTransition) rp.getVMAction(vm1);
        MigrateVMTransition mig2 = (MigrateVMTransition) rp.getVMAction(vm2);
        mig1.getDSlice().getHoster().instantiateTo(rp.getNode(n3), Cause.Null);
        mig2.getDSlice().getHoster().instantiateTo(rp.getNode(n3), Cause.Null);

        // Set objective
        CMinMTTR obj = new CMinMTTR();
        obj.inject(rp);

        // Solve
        ReconfigurationPlan p = rp.solve(0, false);

        Assert.assertNotNull(p);

        // Get allocated BW for each migration
        for (Variable v : rp.getSolver().getVars()) {
            if (v.getName().contains("bandwidth")) {
                if (v.getName().contains("vm#0")) {
                    bw_vm1 = ((IntervalIntVarImpl) v).getValue();
                }
                if (v.getName().contains("vm#1")) {
                    bw_vm2 = ((IntervalIntVarImpl) v).getValue();
                }
            }
        }

        // Verify migrations duration
        for (Action a : p.getActions()) {
            if (a.toString().contains("migrate")) {
                if (a.toString().contains("vm#0")) {
                    Assert.assertTrue((a.getEnd()-a.getStart()) == (int)(double)((mem_vm1*8)/(bw_vm1-(int)(double)(dr_vm1*8))));
                }
                if (a.toString().contains("vm#1")) {
                    Assert.assertTrue((a.getEnd()-a.getStart()) == (int)(double)((mem_vm2*8)/(bw_vm2-(int)(double)(dr_vm2*8))));
                }
            }
        }
    }

    /**
     * Force sequential migrations by ensuring that (max(BW)/2 < DR))
     *
     * @throws SchedulerException
     */
    @Test
    public void SequentialOnlyTest() throws SchedulerException,ContradictionException {

        // Init bandwidth, memoryUsed and dirtyRate for each VM
        int     bw_vm1 = 0,     bw_vm2 = 0;
        int     mem_vm1 = 1000, mem_vm2 = 1500;
        double  dr_vm1 = 21.44, dr_vm2 = 19.28;

        Model mo = new DefaultModel();
        Mapping ma = mo.getMapping();

        VM vm1 = mo.newVM();
        VM vm2 = mo.newVM();
        Node n1 = mo.newNode();
        Node n2 = mo.newNode();
        Node n3 = mo.newNode();

        ma.addOnlineNode(n1);
        ma.addOnlineNode(n2);
        ma.addOnlineNode(n3);
        ma.addRunningVM(vm1, n1);
        ma.addRunningVM(vm2, n2);

        // Put vm attributes
        mo.getAttributes().put(vm1, "memUsed", mem_vm1); // Go
        mo.getAttributes().put(vm2, "memUsed", mem_vm2);
        mo.getAttributes().put(vm1, "dirtyRate", dr_vm1); // octets/s
        mo.getAttributes().put(vm2, "dirtyRate", dr_vm2);

        // Add a resource view
        ShareableResource rc = new ShareableResource("mem", 0, 0);
        rc.setConsumption(vm1, 2);
        rc.setConsumption(vm2, 3);
        rc.setCapacity(n1, 2);
        rc.setCapacity(n2, 3);
        rc.setCapacity(n3, 5);
        mo.attach(rc);

        // Add a simple network
        NetworkView net = new NetworkView();
        Switch s1 = net.newSwitch(10000);
        Switch sm = net.newSwitch(10000);
        Switch s2 = net.newSwitch(10000);
        mo.attach(net);

        // Connect switches and nodes
        s1.connect(1000, n1, n2);
        s2.connect(2000, n3);
        sm.connect(2000, s1);
        sm.connect(300, s2); // Link bottleneck between two switches (300/2)<(19.28*8)

        // Set the custom migration transition
        Parameters ps = new DefaultParameters().setVerbosity(10);
        ps.getTransitionFactory().remove(ps.getTransitionFactory().getBuilder(VMState.RUNNING, VMState.RUNNING).get(0));
        ps.getTransitionFactory().add(new MigrateVMTransition.Builder());

        // New reconfiguration problem
        ReconfigurationProblem rp = new DefaultReconfigurationProblemBuilder(mo)
                .setParams(ps)
                .build();

        // Migrate VMs to n3
        MigrateVMTransition mig1 = (MigrateVMTransition) rp.getVMAction(vm1);
        MigrateVMTransition mig2 = (MigrateVMTransition) rp.getVMAction(vm2);
        mig1.getDSlice().getHoster().instantiateTo(rp.getNode(n3), Cause.Null);
        mig2.getDSlice().getHoster().instantiateTo(rp.getNode(n3), Cause.Null);

        // Set objective
        //CMinMTTR obj = new CMinMTTR();
        CMaxBWObjective obj = new CMaxBWObjective();
        obj.inject(rp);

        // Solve
        ReconfigurationPlan p = rp.solve(60, false);

        Assert.assertNotNull(p);

        // Get allocated BW for each migration
        for (Variable v : rp.getSolver().getVars()) {
            if (v.getName().contains("bandwidth")) {
                if (v.getName().contains("vm#0")) {
                    bw_vm1 = ((IntervalIntVarImpl) v).getValue();
                }
                if (v.getName().contains("vm#1")) {
                    bw_vm2 = ((IntervalIntVarImpl) v).getValue();
                }
            }
        }

        int start_vm1 = 0, start_vm2 = 0;

        // Verify migrations duration
        for (Action a : p.getActions()) {
            if (a.toString().contains("migrate")) {
                if (a.toString().contains("vm#0")) {
                    Assert.assertTrue((a.getEnd()-a.getStart()) == (int)(double)((mem_vm1*8)/(bw_vm1-(int)(double)(dr_vm1*8))));
                    start_vm1 = a.getStart();
                }
                if (a.toString().contains("vm#1")) {
                    Assert.assertTrue((a.getEnd()-a.getStart()) == (int)(double)((mem_vm2*8)/(bw_vm2-(int)(double)(dr_vm2*8))));
                    start_vm2 = a.getStart();
                }
            }
        }

        // Verify that migrations are scheduled sequentially
        Assert.assertNotEquals(start_vm1, start_vm2);
    }

    @Test
    public void VHPCTest() throws SchedulerException,ContradictionException {

        // Init mem + cpu for VMs and Nodes
        int mem_vm = 2, mem_node = 24; // VMs: 2GB,     Nodes: 24GB
        int cpu_vm = 2, cpu_node = 8;  // VMs: 2 VCPUs, Nodes: 8 CPUs
        int nbSrcNodes = 15;
        int nbVMs = nbSrcNodes * 4;
        // Init memoryUsed and dirtyRate (for all VMs)
        int memUsed = 1000; // 1 GB
        double dirtyRate = 21.44; // 21.44 mB/s,

        // New default model
        Model mo = new DefaultModel();
        Mapping ma = mo.getMapping();

        // Create 15 online source nodes + 15 offline destination nodes
        List<Node> srcNodes = new ArrayList<>();
        List<Node> dstNodes = new ArrayList<>();
        for (int i=0; i<nbSrcNodes; i++) { srcNodes.add(mo.newNode()); ma.addOnlineNode(srcNodes.get(i)); }
        for (int i=0; i<nbSrcNodes; i++) { dstNodes.add(mo.newNode()); ma.addOfflineNode(dstNodes.get(i)); }

        // Create running VMs: 4 per source node
        List<VM> vms = new ArrayList<>();
        for (int i=0; i<nbVMs; i++) { vms.add(mo.newVM()); ma.addRunningVM(vms.get(i),srcNodes.get(i%nbSrcNodes)); }

        // Put attributes
        for (VM vm : vms) {
            mo.getAttributes().put(vm, "memUsed", memUsed);
            mo.getAttributes().put(vm, "dirtyRate", dirtyRate);
        }
        for (Node n : dstNodes) {
            mo.getAttributes().put(n, "boot", 120); // ~2 minutes to boot
        }
        for (Node n : srcNodes) {
            mo.getAttributes().put(n, "shutdown", 30); // ~30 seconds to shutdown
        }

        // Add resource views
        ShareableResource rcMem = new ShareableResource("mem", 0, 0);
        ShareableResource rcCPU = new ShareableResource("cpu", 0, 0);
        for (Node n : srcNodes) { rcMem.setCapacity(n, mem_node); rcCPU.setCapacity(n, cpu_node); }
        for (Node n : dstNodes) { rcMem.setCapacity(n, mem_node); rcCPU.setCapacity(n, cpu_node); }
        for (VM vm : vms) { rcMem.setConsumption(vm, mem_vm); rcCPU.setConsumption(vm, cpu_vm); }
        mo.attach(rcMem);
        mo.attach(rcCPU);

        // Add a NetworkView view using the static VHPC routing
        NetworkView net = new NetworkView(new VHPCStaticRouting(srcNodes, dstNodes));
        mo.attach(net);

        // Set the custom migration transition
        DefaultParameters ps = new DefaultParameters();
        ps.setVerbosity(2);
        ps.setTimeLimit(5);
        //ps.setMaxEnd();
        ps.doOptimize(false);
        ps.getTransitionFactory().remove(ps.getTransitionFactory().getBuilder(VMState.RUNNING, VMState.RUNNING).get(0));
        ps.getTransitionFactory().add(new MigrateVMTransition.Builder());

        List<SatConstraint> cstrs = new ArrayList<>();
        // Migrate all VMs to destination nodes
        for (VM vm : vms) {
            cstrs.add(new Fence(vm, Collections.singleton(dstNodes.get(srcNodes.indexOf(ma.getVMLocation(vm))))));
        }

        // Set custom objective
        DefaultChocoScheduler sc = new DefaultChocoScheduler(ps);
        sc.getConstraintMapper().register(new CMaxBWObjective.Builder());
        Instance i = new Instance(mo, cstrs,  new MaxBWObjective());

        try {
            ReconfigurationPlan p = sc.solve(i);
            Assert.assertNotNull(p);
            System.err.println(p);
            ActionsToCSV.convert(p.getActions(), "src/test/java/org/btrplace/scheduler/choco/view/plan/gantt/test.csv");
            System.err.flush();

        } finally {
            System.err.println(sc.getStatistics());
        }
        //Assert.fail();
    }
}
