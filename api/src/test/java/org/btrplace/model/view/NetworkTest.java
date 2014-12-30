package org.btrplace.model.view;

import org.btrplace.model.*;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by vkherbac on 08/12/14.
 */
public class NetworkTest {

    @Test
    public void testPath() {

        Model mo = new DefaultModel();
        Node n1 = mo.newNode();
        Node n2 = mo.newNode();

        Network net = new Network(new DefaultRouting());
        Switch s1 = net.newSwitch(2000);
        Switch sm = net.newSwitch(5000);
        Switch s2 = net.newSwitch(2000);

        s1.connect(n1, 1000);
        s1.connect(sm, 1000);
        s2.connect(n2, 1000);
        s2.connect(sm, 1000);

        Assert.assertTrue(net.getPath(n1, n2).containsAll(s1.getInterfaces()));
        Assert.assertTrue(net.getPath(n1, n2).containsAll(sm.getInterfaces()));
        Assert.assertTrue(net.getPath(n1, n2).containsAll(s2.getInterfaces()));
    }

    @Test(dependsOnMethods = {"testPath"})
    public void testMaxBW() {

        Model mo = new DefaultModel();
        Node n1 = mo.newNode();
        Node n2 = mo.newNode();

        Network net = new Network(new DefaultRouting());
        Switch s1 = net.newSwitch(2000);
        Switch sm = net.newSwitch(5000);
        Switch s2 = net.newSwitch(2000);

        s1.connect(n1, 1000);
        s1.connect(sm, 1000);
        s2.connect(n2, 500);
        s2.connect(sm, 1000);

        Assert.assertEquals(net.getMaxBW(n1, n2), 500);
    }
}
