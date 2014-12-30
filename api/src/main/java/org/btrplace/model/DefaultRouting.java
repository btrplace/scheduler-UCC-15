package org.btrplace.model;

import org.btrplace.model.view.Network;

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
    public List<Switch.Interface> getPath(Node n1, Node n2) {

        if (net == null) { return Collections.emptyList(); }

        // Return the first path found
        return getFirstPath(new ArrayList<>(Collections.singletonList(net.getSwitchInterface(n1))), n2);
    }

    @Override
    public int getMaxBW(Node n1, Node n2) {
        int max = 0;
        for (Switch.Interface inf : getPath(n1, n2)) {
            if (inf.getBandwidth() > max) {
                max = inf.getBandwidth();
            }
        }
        return max;
    }

    private List<Switch.Interface> getFirstPath(List<Switch.Interface> currentPath, Node dst) {

        for (Switch.Interface swif : currentPath.get(currentPath.size()-1).getHost().getInterfaces()) {
            if(currentPath.contains(swif)) { continue; }
            currentPath.add(swif);
            if(swif.getRemote() instanceof Node) {
                if (swif.getRemote().equals(dst)) { return currentPath; }
            }
            else {
                currentPath.add((Switch.Interface)swif.getRemote());
                return getFirstPath(currentPath, dst);
            }
            currentPath.remove(currentPath.size()-1);
        }
        currentPath.remove(currentPath.size()-1);
        return currentPath;
    }
}
