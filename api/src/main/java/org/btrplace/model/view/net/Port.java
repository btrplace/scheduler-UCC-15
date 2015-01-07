package org.btrplace.model.view.net;

import org.btrplace.model.Element;

/**
 * Created by vkherbac on 05/01/15.
 */
public class Port {

    private int bandwidth;
    private Element host;
    private Port connectedTo;

    public Port(int bw, Element ne) {
        bandwidth=bw;
        host=ne;
    }

    public void connect(Port remotePort) { connectedTo=remotePort; }

    public int getBandwidth() { return bandwidth; }

    public Element getHost() { return host; }

    public Port getRemote() { return connectedTo; }
}
