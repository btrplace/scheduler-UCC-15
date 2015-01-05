package org.btrplace.model.view.net;

import org.btrplace.model.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vkherbac on 08/12/14.
 */
public class Switch implements Element,NetworkElement {

    private int id;
    private int capacity;
    private List<Port> ports;

    /**
     * Make a new Switch.
     *
     * @param c     the maximal capacity of the Switch.
     * @param id    the switch id
     */
    public Switch(int id, int c) {
        ports = new ArrayList<>();
        this.id = id;
        capacity = c;
    }

    public void connect(int bandwidth, NetworkElement ne) {
        Port local = new Port(bandwidth, this);
        ports.add(local);
        Port remote = new Port(bandwidth, ne);
        ne.getPorts().add(remote);
        local.connect(remote);
        remote.connect(local);
    }

    public void connect(int bandwidth, NetworkElement ...nes) {
        for (NetworkElement ne : nes) {
            connect(bandwidth, ne);
        }
    }

    public void connect(int bandwidth, List<NetworkElement> nes) {
        for (NetworkElement ne : nes) {
            connect(bandwidth, ne);
        }
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Switch)) {
            return false;
        }

        Switch sw = (Switch) o;

        return id == sw.id();
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public List<Port> getPorts() {
        return ports;
    }
}
