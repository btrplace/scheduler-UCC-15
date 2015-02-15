package org.btrplace.simgrid;

import org.btrplace.model.DefaultModel;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.view.net.NetworkView;
import org.btrplace.model.view.net.StaticRouting;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vkherbac on 08/12/14.
 */
public class SimgridTest {

    String path = new File("").getAbsolutePath() + "/simgrid/src/test/resources/";

    @Test
    public void routingTest() {

        File g5kXML = new File(getClass().getClassLoader().getResource("g5k_grenoble.xml").getFile());

        // Init the model with g5k_grenoble XML file
        Model mo = new SimgridModel(g5kXML);

        // Get the nodes map
        Map<String, Node> nodes = ((SimgridModel)mo).getNodes();

        // Get networking view
        NetworkView net = (NetworkView) mo.getView(NetworkView.VIEW_ID);

        // Write topology scheme
        net.generateDot(path + "g5k_grenoble.dot", false);

        // Test different routes
        Assert.assertEquals(net.getPath(nodes.get("edel-1"), nodes.get("edel-2")).size(), 2);
        Assert.assertEquals(net.getPath(nodes.get("edel-1"), nodes.get("edel-72")).size(), 4);
    }
}
