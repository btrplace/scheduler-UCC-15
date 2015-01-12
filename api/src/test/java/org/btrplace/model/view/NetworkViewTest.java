package org.btrplace.model.view;

import org.btrplace.model.DefaultModel;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.view.net.DefaultRouting;
import org.btrplace.model.view.net.NetworkView;
import org.btrplace.model.view.net.Switch;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by vkherbac on 08/12/14.
 */
public class NetworkViewTest {

    @Test
    public void testPath() {

        Model mo = new DefaultModel();
        Node n1 = mo.newNode();
        Node n2 = mo.newNode();

        NetworkView net = new NetworkView(new DefaultRouting());
        Switch s1 = net.newSwitch(2000);
        Switch sm = net.newSwitch(5000);
        Switch s2 = net.newSwitch(2000);

        s1.connect(1000, n1, sm);
        s2.connect(1000, n2, sm);

        Assert.assertTrue(net.getPath(n1, n2).containsAll(s1.getPorts()));
        Assert.assertTrue(net.getPath(n1, n2).containsAll(sm.getPorts()));
        Assert.assertTrue(net.getPath(n1, n2).containsAll(s2.getPorts()));
    }

    @Test(dependsOnMethods = {"testPath"})
    public void testMaxBW() {

        Model mo = new DefaultModel();
        Node n1 = mo.newNode();
        Node n2 = mo.newNode();

        NetworkView net = new NetworkView(new DefaultRouting());
        Switch s1 = net.newSwitch(2000);
        Switch sm = net.newSwitch(5000);
        Switch s2 = net.newSwitch(2000);

        s1.connect(1000, n1, sm);
        s2.connect(500, n2); // Bottleneck
        s2.connect(1000, sm);

        Assert.assertEquals(net.getMaxBW(n1, n2), 500);
    }
}
