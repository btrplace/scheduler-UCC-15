package org.btrplace.model.view.net;

import org.btrplace.model.Node;

import java.util.*;

/**
 * Created by vkherbac on 08/01/15.
 */
public class VHPCStaticRouting implements Routing {

    private Network net;
    private List<Node> srcNodes, dstNodes;
    private Switch srcSw, dstSw;
    private Map<NodesMap, Integer> useCable;

    public VHPCStaticRouting(List<Node> srcNodes, List<Node> dstNodes) {
        this.srcNodes = srcNodes;
        this.dstNodes = dstNodes;
    }

    @Override
    public void setNetwork(Network net) {
        this.net = net;

        // Create the switches
        srcSw = net.newSwitch();
        dstSw = net.newSwitch();

        // Connect them together with 3*1Gb Links (Corresponds to the 3 first ports of each Switch)
        srcSw.connect(1000, dstSw, dstSw, dstSw);

        // Connect to nodes
        srcSw.connect(1000, srcNodes);
        dstSw.connect(1000, dstNodes);

        // Init random static routes
        useCable = new HashMap<NodesMap, Integer>();
        for(Node src : srcNodes) {
            for (Node dst : dstNodes) {
                useCable.put(new NodesMap(src, dst), new Random().nextInt(3));
            }
        }
    }

    @Override
    public List<Port> getPath(Node n1, Node n2) {

        List<Port> path = new ArrayList<Port>();

        // From src to dst nodes only !
        if (srcNodes.contains(n1) && dstNodes.contains(n2)) {
            path.add(net.getSwitchInterface(n1));
            for (NodesMap nm : useCable.keySet()) {
                if (nm.equals(new NodesMap(n1, n2))) {
                    path.add(srcSw.getPorts().get(useCable.get(nm)));
                }
            }
            path.add(path.get(path.size()-1).getRemote());
            path.add(net.getSwitchInterface(n2));
        }

        return path;
    }

    @Override
    public int getMaxBW(Node n1, Node n2) {
        return 1000;
    }

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
