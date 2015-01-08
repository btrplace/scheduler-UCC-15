package org.btrplace.model.view.net;

import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.view.ModelView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vkherbac on 08/12/14.
 */
public class Network implements ModelView, Cloneable {

    private List<Switch> switches;
    private Routing routing;
    private String viewId;
    private SwitchBuilder swBuilder;

    public static final String VIEW_ID = "Network";

    public Network() {  this(new DefaultRouting(), new DefaultSwitchBuilder()); }

    public Network(SwitchBuilder sb) { this(new DefaultRouting(), sb); }

    public Network(Routing routing) { this(routing, new DefaultSwitchBuilder()); }

    public Network(Routing routing, SwitchBuilder sb) {
        this.viewId = VIEW_ID;
        switches = new ArrayList<>();
        swBuilder = sb;
        setRouting(routing);
    }

    public Switch newSwitch(int capacity) {
        Switch s = swBuilder.newSwitch(capacity);
        switches.add(s);
        return s;
    }

    public Switch newSwitch() {
        return newSwitch(-1);
    }

    public void setRouting(Routing routing) {
        this.routing = routing;
        routing.setNetwork(this);
    }

    public List<Switch> getSwitches() {
        return switches;
    }

    public List<Port> getPath(Node n1, Node n2) {
        return routing.getPath(n1, n2);
    }

    public Routing getRouting() { return routing; }

    public int getMaxBW(Node n1, Node n2) {
        int maxBW = Integer.MAX_VALUE;
        for (Port swif : getPath(n1, n2)) {
            if(swif.getBandwidth() < maxBW) { maxBW = swif.getBandwidth(); }
        }
        return maxBW;
    }

    public List<Port> getAllInterfaces() {
        List<Port> list = new ArrayList<>();
        for (Switch s : switches) {
            for (Port si : s.getPorts()) {
                if(!list.contains(si)) list.add(si);
            }
        }
        return list;
    }

    public Port getSwitchInterface(Node n) {
        for (Switch sw : switches) {
            for (Port p : sw.getPorts()) {
                if (p.getRemote().getHost().equals(n)) { return p; }
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
        net.swBuilder = swBuilder.clone();
        return net;
    }

    @Override
    public boolean substituteVM(VM curId, VM nextId) {
        return false;
    }
}
