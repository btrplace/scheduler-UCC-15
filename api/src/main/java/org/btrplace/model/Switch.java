package org.btrplace.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vkherbac on 08/12/14.
 */
public class Switch {

    private int capacity;
    private List<Interface> interfaces;

    /**
     * Make a new Switch.
     *
     * @param c the maximal capacity of the Switch.
     */
    public Switch(int c) {
        interfaces = new ArrayList<>();
        capacity = c;
    }

    public void connect(Switch sw, int bandwidth) {
        Interface local = new Interface(bandwidth, this);
        interfaces.add(local);
        Interface remote = new Interface(bandwidth, sw);
        sw.getInterfaces().add(remote);
        local.connect(remote);
        remote.connect(local);
    }

    public void connect(Node node, int bandwidth) {
        Interface local = new Interface(bandwidth, this);
        interfaces.add(local);
        local.connect(node);
    }

    public void connect(List<?> list, int bandwidth) {
        for (Object elt : list) {
            if(elt instanceof Node) {
                connect((Node) elt, bandwidth);
            }
            if(elt instanceof Switch) {
                connect((Switch) elt, bandwidth);
            }
        }
    }

    public List<Interface> getInterfaces() {
        return interfaces;
    }

    public class Interface {
        private int bandwidth;
        private Switch host;
        private Object connectedTo;
        public Interface(int bw, Switch sw) { bandwidth=bw; host=sw; }
        public void connect(Interface remoteInt) { connectedTo=remoteInt; }
        public void connect(Node n) { connectedTo=n; }
        public int getBandwidth() { return bandwidth; }
        public Switch getHost() { return host; }
        public Object getRemote() { return connectedTo; }
    }
}
