package org.btrplace.model.view.net;

import org.btrplace.model.DefaultModel;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

/**
 * Created by vkherbac on 08/12/14.
 */
public class RoutingTest {

    @Test
    public void routingTest() {

        String path = new File("").getAbsolutePath() + "/api/src/test/resources/net/";

        Model mo = new DefaultModel();

        File routingXML = new File(getClass().getClassLoader().getResource("net/routing-test.xml").getFile());

        NetworkView net = new NetworkView(new StaticRouting());
        mo.attach(net);

        List<Node> nodes = ((StaticRouting) net.getRouting()).importXML(mo, routingXML);

        net.generateDot(path + "routing-test.dot", false);

        // Test different routes
        Assert.assertEquals(net.getPath(nodes.get(0), nodes.get(1)).size(), 2);
        Assert.assertEquals(net.getPath(nodes.get(2), nodes.get(3)).size(), 2);
        Assert.assertEquals(net.getPath(nodes.get(0), nodes.get(2)).size(), 4);
        Assert.assertEquals(net.getPath(nodes.get(1), nodes.get(3)).size(), 4);
    }
}
