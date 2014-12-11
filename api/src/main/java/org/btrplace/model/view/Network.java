package org.btrplace.model.view;

import org.btrplace.model.Node;
import org.btrplace.model.Switch;
import org.btrplace.model.VM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by vkherbac on 08/12/14.
 */
public class Network implements ModelView, Cloneable {

    private List<Switch> switches;

    /**
     * The base of the view identifier. Once instantiated, it is completed
     * by the network identifier.
     */
    public static final String VIEW_ID_BASE = "Network.";

    private String viewId;

    private String netId;

    public Network(String id) {
        switches = new ArrayList<>();
        netId = id;
        viewId = VIEW_ID_BASE + netId;
    }

    public Switch newSwitch(int capacity) {
        Switch s = new Switch(capacity);
        switches.add(s);
        return s;
    }

    public List<Switch> getSwitches() {
        return switches;
    }

    public int getMaxBW(Node n1, Node n2) {
        int maxBW = Integer.MAX_VALUE;
        for (Switch.Interface swif : getPath(n1, n2)) {
            if(swif.getBandwidth() < maxBW) { maxBW = swif.getBandwidth(); }
        }
        return maxBW;
    }

    public List<Switch.Interface> getPath(Node n1, Node n2) {

        // Return the first path found
        return getFirstPath(new ArrayList<>(Collections.singletonList(getSwitchInterface(n1))), n2);
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
            currentPath.remove(currentPath.get(currentPath.size()-1));
        }
        currentPath.remove(currentPath.get(currentPath.size()-1));
        return currentPath;
    }

    private Switch.Interface getSwitchInterface(Node n) {
        for (Switch sw : switches) {
            for (Switch.Interface swif : sw.getInterfaces()) {
                if (swif.getRemote().equals(n)) { return swif; }
            }
        }
        return null;
    }

    /**
     * Get the network identifier
     *
     * @return a non-empty string
     */
    public String getNetworkIdentifier() {
        return netId;
    }

    @Override
    public String getIdentifier() {
        return viewId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return this.netId.equals(((Network) o).getNetworkIdentifier());
    }

    @Override
    public ModelView clone() {
        Network net = new Network(netId);
        net.getSwitches().addAll(switches);
        return net;
    }

    @Override
    public boolean substituteVM(VM curId, VM nextId) {
        return false;
    }
}
