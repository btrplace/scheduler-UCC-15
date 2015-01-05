package org.btrplace.model.view.net;

/**
 * Created by vkherbac on 05/01/15.
 */
public class Port {

    private int bandwidth;
    private NetworkElement host;
    private Port connectedTo;

    public Port(int bw, NetworkElement ne) {
        bandwidth=bw;
        host=ne;
    }

    public void connect(Port remotePort) { connectedTo=remotePort; }

    public int getBandwidth() { return bandwidth; }

    public NetworkElement getHost() { return host; }

    public Port getRemote() { return connectedTo; }
}
