package org.btrplace.model.view.net;

import org.btrplace.model.Element;
import org.btrplace.model.Node;

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

    public void connect(int bandwidth, Element elt) {

        if (!(elt instanceof Switch) && !(elt instanceof Node)) {
            throw new ClassCastException("An object of the class '"+elt.getClass().getSimpleName()+
                    "' can not be connected to the network");
        }

        Port local = new Port(bandwidth, this);
        ports.add(local);

        Port remote = new Port(bandwidth, elt);
        local.connect(remote);
        remote.connect(local);

        if (elt instanceof Switch) {
            ((NetworkElement)elt).getPorts().add(remote);
        }
    }

    public void connect(int bandwidth, Element ...elts) {
        for (Element ne : elts) {  connect(bandwidth, ne); }
    }

    public void connect(int bandwidth, List<Element> elts) {
        connect(bandwidth, elts.toArray(new Element[elts.size()]));
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
