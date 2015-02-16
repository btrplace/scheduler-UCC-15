package org.btrplace.simgrid;

import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.view.NamingService;
import org.btrplace.model.view.net.NetworkView;
import org.btrplace.model.view.net.Switch;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

/**
 * Created by vkherbac on 08/12/14.
 */
public class SimgridModelBuilderTest {

    String path = new File("").getAbsolutePath() + "/simgrid/src/test/resources/";

    @Test
    public void routingTest() {

        File g5kXML = new File(getClass().getClassLoader().getResource("grid5000.xml").getFile());

        // Init the model with g5k_grenoble XML file
        Model mo = new SimgridModelBuilder().build(g5kXML);

        // Get the nodes naming service
        NamingService<Node> nsNodes = (NamingService<Node>) mo.getView(NamingService.ID + "node");

        // Get the switches naming service
        NamingService<Switch> nsSwitches = (NamingService<Switch>) mo.getView(NamingService.ID + "switch");

        // Get networking view
        NetworkView net = (NetworkView) mo.getView(NetworkView.VIEW_ID);

        // Write topology scheme
        net.generateDot(path + "grid5000.dot", false);

        // Get list of nodes and switches
        List<Node> nodes = net.getConnectedNodes();
        List<Switch> switches = net.getSwitches();

        // Test number of nodes and switches
        Assert.assertEquals(nodes.size(), 1035);
        Assert.assertEquals(switches.size(), 36);

        // Test different routes
        Assert.assertEquals(net.getPath(nsNodes.resolve("edel-1"), nsNodes.resolve("edel-2")).size(), 2);
        Assert.assertEquals(net.getPath(nsNodes.resolve("edel-1"), nsNodes.resolve("edel-72")).size(), 4);
    }
}
