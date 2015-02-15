package org.btrplace.model.view.net;

import org.btrplace.model.DefaultModel;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vkherbac on 08/12/14.
 */
public class RoutingTest {

    @Test
    public void routingTest() {

        String path = new File("").getAbsolutePath() + "/api/src/test/resources/net/";

        Model mo = new DefaultModel();

        List<Node> nodes = new ArrayList<>();
        for (int i=0; i<4; i++) {
            nodes.add(mo.newNode());
        }

        File routingXML = new File(path + "routing-test.xml");

        NetworkView net = new NetworkView(new StaticRouting(nodes, routingXML));
        mo.attach(net);

        net.generateDot(path + "routing-test.dot", false);

        // Test different routes
        Assert.assertEquals(net.getPath(nodes.get(0), nodes.get(1)).size(), 2);
        Assert.assertEquals(net.getPath(nodes.get(2), nodes.get(3)).size(), 2);
        Assert.assertEquals(net.getPath(nodes.get(0), nodes.get(2)).size(), 4);
        Assert.assertEquals(net.getPath(nodes.get(1), nodes.get(3)).size(), 4);
    }

    @Test
    public void g5kTest() {

        String path = new File("").getAbsolutePath() + "/api/src/test/resources/net/";

        Model mo = new DefaultModel();

        List<Node> nodes = new ArrayList<>();
        for (int i=0; i<(10+72+34); i++) {
            nodes.add(mo.newNode());
        }

        File g5kXML = new File(path + "g5k_grenoble.xml");

        NetworkView net = new NetworkView(new G5kStaticRouting(nodes, g5kXML));
        mo.attach(net);

        net.generateDot(path + "g5k_grenoble.dot", false);

        // Test different routes
        Assert.assertEquals(net.getPath(nodes.get(0), nodes.get(1)).size(), 2);
        Assert.assertEquals(net.getPath(nodes.get(10), nodes.get(1)).size(), 4);
    }
}
