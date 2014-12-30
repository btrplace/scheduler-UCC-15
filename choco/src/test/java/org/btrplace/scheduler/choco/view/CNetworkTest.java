package org.btrplace.scheduler.choco.view;

import org.btrplace.model.*;
import org.btrplace.model.constraint.Offline;
import org.btrplace.model.view.Network;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.event.Action;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.ChocoScheduler;
import org.btrplace.scheduler.choco.DefaultChocoScheduler;
import org.btrplace.scheduler.choco.transition.MigrateVM;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;

/**
 * Created by vkherbac on 30/12/14.
 */
public class CNetworkTest {

    @Test
    public void test() throws SchedulerException {
        Model mo = new DefaultModel();
        Mapping ma = mo.getMapping();

        VM vm1 = mo.newVM();
        VM vm2 = mo.newVM();

        Node n1 = mo.newNode();
        Node n2 = mo.newNode();

        ma.addOnlineNode(n1);
        ma.addOnlineNode(n2);
        ma.addRunningVM(vm1, n1);
        ma.addRunningVM(vm2, n1);

        // TODO: use a real percentage from attr 'mem'
        mo.getAttributes().put(vm1, "memUsed", 1000); // 1Go
        mo.getAttributes().put(vm2, "memUsed", 1200); // 1.5Go

        mo.getAttributes().put(vm1, "dirtyRate", 21.44); // octets/s
        mo.getAttributes().put(vm2, "dirtyRate", 19.28); // octets/s

        ShareableResource rc = new ShareableResource("foo", 0, 0);
        rc.setConsumption(vm1, 2);
        rc.setConsumption(vm2, 3);
        rc.setCapacity(n1, 5);
        rc.setCapacity(n2, 5);
        mo.attach(rc);

        Network net = new Network(new DefaultRouting());
        Switch s1 = net.newSwitch(2000);
        Switch sm = net.newSwitch(5000);
        Switch s2 = net.newSwitch(2000);
        mo.attach(net);

        s1.connect(n1, 1000);
        s1.connect(sm, 1000);
        s2.connect(n2, 1000);
        s2.connect(sm, 1000);

        ChocoScheduler cra = new DefaultChocoScheduler();

        // Set custom VMTransitionBuilder
        cra.getTransitionFactory().remove(cra.getTransitionFactory().getBuilder(VMState.RUNNING, VMState.RUNNING).get(0));
        cra.getTransitionFactory().add(new MigrateVM.Builder());

        ReconfigurationPlan p = cra.solve(mo, Collections.singleton(new Offline(n1)));

        Assert.assertNotNull(p);

        //TODO: -1 because we don't use the full 1Gb/s BW
        for (Action a : p.getActions()) {
            if (a.toString().equals("{action=migrate(vm=vm#0, from=node#0, to=node#1)}")) {
                Assert.assertTrue((a.getEnd()-a.getStart()-1) == (int)(double) ((1000*8)/(1000-(21.44*8))));
            }
            if (a.toString().equals("{action=migrate(vm=vm#1, from=node#0, to=node#1)}")) {
                Assert.assertTrue((a.getEnd()-a.getStart()-1) == (int)(double) ((1200*8)/(1000-(19.28*8))));
            }
        }
    }
}
