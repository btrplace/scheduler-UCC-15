package org.btrplace.model.view.net;

import org.btrplace.model.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by vkherbac on 12/12/14.
 */
public interface Routing {

    void setNetwork(NetworkView net);

    List<Port> getPath(Node n1, Node n2);

    int getMaxBW(Node n1, Node n2);

    class NodesMap {
        private Node n1, n2;
        public NodesMap(Node n1, Node n2) { this.n1 = n1; this.n2 = n2; }
        public Node getSrc() { return n1; }
        public Node getDst() { return n2; }
        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (!(o instanceof NodesMap)) { return false; }
            return (((NodesMap)o).getSrc().equals(n1) && ((NodesMap)o).getDst().equals(n2));
        }
    }
}
