package org.btrplace.scheduler.choco.constraint;

import org.btrplace.model.*;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.model.view.net.NetworkView;
import org.btrplace.model.view.net.Switch;
import org.btrplace.model.view.net.SyncEnd;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.event.Action;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.DefaultParameters;
import org.btrplace.scheduler.choco.DefaultReconfigurationProblemBuilder;
import org.btrplace.scheduler.choco.Parameters;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.constraint.mttr.CMinMTTR;
import org.btrplace.scheduler.choco.view.net.CSyncEnd;
import org.btrplace.scheduler.choco.view.net.MigrateVMTransition;
import org.chocosolver.solver.Cause;
import org.chocosolver.solver.exception.ContradictionException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by vkherbac on 13/01/15.
 */
public class CSyncEndTest {

    @Test
    public void Test() throws SchedulerException,ContradictionException {

        // Init bandwidth, memoryUsed and dirtyRate for each VM
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
        s2.connect(1000, n3);
        sm.connect(1000, s1);
        sm.connect(1000, s2);

        // Set the custom migration transition
        Parameters ps = new DefaultParameters().setVerbosity(10);
        ps.getTransitionFactory().remove(ps.getTransitionFactory().getBuilder(VMState.RUNNING, VMState.RUNNING).get(0));
        ps.getTransitionFactory().add(new MigrateVMTransition.Builder());

        // Register the constraint SyncEnd
        ps.getConstraintMapper().register(new CSyncEnd.Builder());

        // New reconfiguration problem
        ReconfigurationProblem rp = new DefaultReconfigurationProblemBuilder(mo)
                .setParams(ps)
                .build();

        // Migrate VMs to n3
        MigrateVMTransition mig1 = (MigrateVMTransition) rp.getVMAction(vm1);
        MigrateVMTransition mig2 = (MigrateVMTransition) rp.getVMAction(vm2);
        mig1.getDSlice().getHoster().instantiateTo(rp.getNode(n3), Cause.Null);
        mig2.getDSlice().getHoster().instantiateTo(rp.getNode(n3), Cause.Null);

        // DEBUG: manually add the SyncEnd constraint
        new CSyncEnd(new SyncEnd(vm1, vm2)).inject(rp);

        // Set objective
        CMinMTTR obj = new CMinMTTR();
        //CMaxBWObjective obj = new CMaxBWObjective();
        obj.inject(rp);

        // Solve
        ReconfigurationPlan p = rp.solve(0, false);
        Assert.assertNotNull(p);

        // Verify that migrations complete at the same time
        int end_vm1 = -1, end_vm2 = -2;
        for (Action a : p.getActions()) {
            if (a.toString().contains("migrate")) {
                if (a.toString().contains("vm#0")) { end_vm1 = a.getEnd(); }
                if (a.toString().contains("vm#1")) { end_vm2 = a.getEnd(); }
            }
        }
        Assert.assertEquals(end_vm1, end_vm2);
    }
}
