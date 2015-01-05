package org.btrplace.model.view.net;

import org.btrplace.model.Node;
import org.btrplace.model.view.Network;

import java.util.List;

/**
 * Created by vkherbac on 12/12/14.
 */
public interface Routing {

    void setNetwork(Network net);

    List<Switch.Interface> getPath(Node n1, Node n2);

    int getMaxBW(Node n1, Node n2);
}
