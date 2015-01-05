package org.btrplace.scheduler.choco.view;

import org.btrplace.model.*;
import org.btrplace.model.view.net.Network;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.model.view.net.Switch;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.event.Action;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.DefaultParameters;
import org.btrplace.scheduler.choco.DefaultReconfigurationProblemBuilder;
import org.btrplace.scheduler.choco.Parameters;
import org.btrplace.scheduler.choco.ReconfigurationProblem;
import org.btrplace.scheduler.choco.constraint.mttr.CMinMTTR;
import org.btrplace.scheduler.choco.view.net.MigrateVMTransition;
import org.chocosolver.solver.Cause;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.impl.IntervalIntVarImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by vkherbac on 30/12/14.
 */
public class CNetworkTest {

    @Test
    public void test() throws SchedulerException {

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
        //Node n3 = mo.newNode();

        ma.addOnlineNode(n1);
        ma.addOnlineNode(n2);
        //ma.addOnlineNode(n3);
        ma.addRunningVM(vm1, n1);
        ma.addRunningVM(vm2, n1);

        // Put vm attributes, TODO: use a real percentage from attr 'mem'
        mo.getAttributes().put(vm1, "memUsed", mem_vm1); // 1Go
        mo.getAttributes().put(vm2, "memUsed", mem_vm2); // 1.5Go
        mo.getAttributes().put(vm1, "dirtyRate", dr_vm1); // octets/s
        mo.getAttributes().put(vm2, "dirtyRate", dr_vm2); // octets/s

        // Add a resource view
        ShareableResource rc = new ShareableResource("mem", 0, 0);
        rc.setConsumption(vm1, 2);
        rc.setConsumption(vm2, 3);
        rc.setCapacity(n1, 5);
        rc.setCapacity(n2, 5);
        //rc.setCapacity(n3, 5);
        mo.attach(rc);

        // Add a simple network
        Network net = new Network();
        Switch s1 = net.newSwitch(2000);
        Switch sm = net.newSwitch(2000);
        Switch s2 = net.newSwitch(2000);
        mo.attach(net);

        // Connect switches and nodes
        s1.connect(1000, n1);
        s2.connect(1000, n2);
        sm.connect(1000, s1, s2);

        // Set the custom transition
        Parameters ps = new DefaultParameters().setVerbosity(10);
        ps.getTransitionFactory().remove(ps.getTransitionFactory().getBuilder(VMState.RUNNING, VMState.RUNNING).get(0));
        ps.getTransitionFactory().add(new MigrateVMTransition.Builder());

        // New reconfiguration problem
        ReconfigurationProblem rp = new DefaultReconfigurationProblemBuilder(mo)
                .setParams(ps)
                .build();

        // Migrate VMs from n1 to n2
        MigrateVMTransition mig1 = (MigrateVMTransition) rp.getVMAction(vm1);
        MigrateVMTransition mig2 = (MigrateVMTransition) rp.getVMAction(vm2);
        try {
            mig1.getDSlice().getHoster().instantiateTo(rp.getNode(n2), Cause.Null);
            mig2.getDSlice().getHoster().instantiateTo(rp.getNode(n2), Cause.Null);
        } catch (ContradictionException e) {
            e.printStackTrace();
        }

        // Set objective
        CMinMTTR obj = new CMinMTTR();
        obj.inject(rp);

        // Solve
        ReconfigurationPlan p = rp.solve(0, false);

        Assert.assertNotNull(p);

        // Get allocated BW for each migration
        for (Variable v : rp.getSolver().getVars()) {
            if (v.getName().contains("vm#0") && v.getName().contains("bandwidth")) {
                bw_vm1 = ((IntervalIntVarImpl) v).getValue();
            }
            if (v.getName().contains("vm#1") && v.getName().contains("bandwidth")) {
                bw_vm2 = ((IntervalIntVarImpl) v).getValue();
            }
        }

        // Verify migrations duration
        for (Action a : p.getActions()) {
            if (a.toString().contains("migrate(vm=vm#0, from=node#0, to=node#1)")) {
                Assert.assertTrue((a.getEnd()-a.getStart()) == (int)(double) ((mem_vm1*8)/(bw_vm1-(int)(double)(dr_vm1*8))));
            }
            if (a.toString().contains("migrate(vm=vm#1, from=node#0, to=node#1)")) {
                Assert.assertTrue((a.getEnd()-a.getStart()) == (int)(double) ((mem_vm2*8)/(bw_vm2-(int)(double)(dr_vm2*8))));
            }
        }
    }
}
