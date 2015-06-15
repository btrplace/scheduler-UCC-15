package org.btrplace.model.view.net;

import org.btrplace.model.Node;

import java.util.*;

/**
 * Created by vkherbac on 12/12/14.
 */
public class DefaultRouting implements Routing {

    private NetworkView net;

    public DefaultRouting() {}

    @Override
    public void setNetwork(NetworkView net) {
        this.net = net;
    }

    @Override
    public List<Port> getPath(Node n1, Node n2) {

        if (net == null) { return Collections.emptyList(); }

        // Return the first path found (ordered list of ports)
        return getFirstPath(new ArrayList<>(Arrays.asList(net.getSwitchInterface(n1).getRemote(),
                net.getSwitchInterface(n1))), n2);
        //return getIndirectPath(net.getSwitchInterface(n1), net.getSwitchInterface(n2));
    }

    @Override
    public int getMaxBW(Node n1, Node n2) {
        int max = Integer.MAX_VALUE;
        for (Port inf : getPath(n1, n2)) {
            if (inf.getBandwidth() < max) {
                max = inf.getBandwidth();
            }
        }
        return max;
    }

    /**
     * Get the first path found between two nodes
     * @param currentPath the initial path, it typically contains the first port(s) => recursive function
     * @param dst the destination node
     * @return the ordered list of ports that make the path
     */
    private List<Port> getFirstPath(List<Port> currentPath, Node dst) {

        if (currentPath.get(currentPath.size()-1).getHost() instanceof Switch) {
            for (Port p : ((Switch) currentPath.get(currentPath.size() - 1).getHost()).getPorts()) {
                if (currentPath.contains(p)) {
                    continue;
                }
                currentPath.add(p);
                if (p.getRemote().getHost() instanceof Node) {
                    if (p.getRemote().getHost().equals(dst)) {
                        currentPath.add(p.getRemote());
                        return currentPath;
                    }
                } else {
                    currentPath.add(p.getRemote());
                    return getFirstPath(currentPath, dst);
                }
                currentPath.remove(currentPath.size() - 1);
            }
            currentPath.remove(currentPath.size() - 1);
            return getFirstPath(currentPath, dst);

        } else {
            return Collections.emptyList();
        }
    }

    protected List<Port> getIndirectPath(Port srcPort, Port dstPort) {

        Switch srcSwitch = (Switch) srcPort.getHost();
        Map<Switch,Port> srcConnectedSwitches = new HashMap<>();
        for (Port p : srcSwitch.getPorts()) {
            if(p.getRemote().getHost() instanceof Switch) {
                srcConnectedSwitches.put((Switch) p.getRemote().getHost(), p);
            }
        }
        Switch dstSwitch = (Switch) dstPort.getHost();
        Map<Switch,Port> dstConnectedSwitches = new HashMap<>();
        for (Port p : dstSwitch.getPorts()) {
            if(p.getRemote().getHost() instanceof Switch) {
                dstConnectedSwitches.put((Switch) p.getRemote().getHost(), p);
            }
        }
        List mainSwitch = new ArrayList<>(srcConnectedSwitches.keySet());
        mainSwitch.retainAll(dstConnectedSwitches.keySet());

        if (mainSwitch.size() != 1) {
            // Problem !
            return Collections.emptyList();
        }

        return Arrays.asList(
                srcPort,
                srcPort.getRemote(),
                srcConnectedSwitches.get(mainSwitch.get(0)),
                dstConnectedSwitches.get(mainSwitch.get(0)),
                dstPort.getRemote(),
                dstPort
        );
    }
}
