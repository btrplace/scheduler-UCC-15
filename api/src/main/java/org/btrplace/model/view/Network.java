package org.btrplace.model.view;

import org.btrplace.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vkherbac on 08/12/14.
 */
public class Network implements ModelView, Cloneable {

    private List<Switch> switches;
    private Routing routing;
    private String viewId;

    /**
     * The base of the view identifier. Once instantiated, it is completed
     * by the network identifier.
     */
    public static final String VIEW_ID = "Network";

    public Network() {
        this(new DefaultRouting());
    }

    public Network(Routing routing) {
        this.viewId = VIEW_ID;
        switches = new ArrayList<>();
        setRouting(routing);
    }

    public Switch newSwitch(int capacity) {
        Switch s = new Switch(capacity);
        switches.add(s);
        return s;
    }

    public void setRouting(Routing routing) {
        this.routing = routing;
        routing.setNetwork(this);
    }

    public List<Switch> getSwitches() {
        return switches;
    }

    public List<Switch.Interface> getPath(Node n1, Node n2) {
        return routing.getPath(n1, n2);
    }

    public int getMaxBW(Node n1, Node n2) {
        int maxBW = Integer.MAX_VALUE;
        for (Switch.Interface swif : getPath(n1, n2)) {
            if(swif.getBandwidth() < maxBW) { maxBW = swif.getBandwidth(); }
        }
        return maxBW;
    }

    public List<Switch.Interface> getAllInterfaces() {
        List<Switch.Interface> list = new ArrayList<>();
        for (Switch s : switches) {
            for (Switch.Interface si : s.getInterfaces()) {
                if(!list.contains(si)) list.add(si);
            }
        }
        return list;
    }

    public Switch.Interface getSwitchInterface(Node n) {
        for (Switch sw : switches) {
            for (Switch.Interface swif : sw.getInterfaces()) {
                if (swif.getRemote().equals(n)) { return swif; }
            }
        }
        return null;
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
        return this.viewId.equals(((Network) o).getIdentifier());
    }

    @Override
    public ModelView clone() {
        Network net = new Network(routing);
        net.getSwitches().addAll(switches);
        return net;
    }

    @Override
    public boolean substituteVM(VM curId, VM nextId) {
        return false;
    }
}
