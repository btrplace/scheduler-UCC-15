package org.btrplace.model.view.net;

import org.btrplace.model.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by vkherbac on 12/12/14.
 */
public class DefaultRouting implements Routing {

    private Network net;

    public DefaultRouting() {}

    public DefaultRouting(Network net) {
        setNetwork(net);
    }

    @Override
    public void setNetwork(Network net) {
        this.net = net;
    }

    @Override
    public List<Port> getPath(Node n1, Node n2) {

        if (net == null) { return Collections.emptyList(); }

        // Return the first path found
        List<Port> path = new ArrayList<>();
        for (Port p : n1.getPorts()) {
            path = getFirstPath(new ArrayList<>(Collections.singletonList(p.getRemote())), n2);
            if (!path.isEmpty()) { break; }
        }
        return path;
    }

    @Override
    public int getMaxBW(Node n1, Node n2) {
        int max = 0;
        for (Port inf : getPath(n1, n2)) {
            if (inf.getBandwidth() > max) {
                max = inf.getBandwidth();
            }
        }
        return max;
    }

    private List<Port> getFirstPath(List<Port> currentPath, Node dst) {

        for (Port p : currentPath.get(currentPath.size()-1).getHost().getPorts()) {
            if(currentPath.contains(p)) { continue; }
            currentPath.add(p);
            if(p.getRemote().getHost() instanceof Node) {
                if (p.getRemote().getHost().equals(dst)) { return currentPath; }
            }
            else {
                currentPath.add(p.getRemote());
                return getFirstPath(currentPath, dst);
            }
            currentPath.remove(currentPath.size()-1);
        }
        currentPath.remove(currentPath.size()-1);
        return currentPath;
    }
}
