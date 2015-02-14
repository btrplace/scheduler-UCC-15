package org.btrplace.model.view.net;

import org.btrplace.model.Node;
import org.btrplace.model.VM;
import org.btrplace.model.view.ModelView;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by vkherbac on 08/12/14.
 */
public class NetworkView implements ModelView, Cloneable {

    private List<Switch> switches;
    private Routing routing;
    private String viewId;
    private SwitchBuilder swBuilder;

    public static final String VIEW_ID = "NetworkView";

    public NetworkView() {  this(new DefaultRouting(), new DefaultSwitchBuilder()); }

    public NetworkView(SwitchBuilder sb) { this(new DefaultRouting(), sb); }

    public NetworkView(Routing routing) { this(routing, new DefaultSwitchBuilder()); }

    public NetworkView(Routing routing, SwitchBuilder sb) {
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

    public List<Node> getConnectedNodes() {
        List<Node> nodes = new ArrayList<>();
        for (Switch sw : switches) {
            for (Port p : sw.getPorts()) {
                if (p.getRemote().getHost() instanceof Node) {
                    if (!nodes.contains(p.getRemote().getHost())) nodes.add((Node) p.getRemote().getHost());
                }
            }
        }
        return nodes;
    }

    public boolean generateDot(String out, boolean fromLeftToRight) {

        int numLink = 0;
        List<Node> nodes = getConnectedNodes();
        Set<Port> drawedLinks = new HashSet<>();

        try {
            BufferedWriter dot = new BufferedWriter(new FileWriter(out));
            dot.append("digraph G {\n");
            if (fromLeftToRight) { dot.append("rankdir=LR;\n"); }
            // Draw nodes
            for (Node n : nodes) {
                dot.append("node").
                        append(String.valueOf(n.id())).
                        append(" [shape=box, color=green, label=\"Node ").
                        append(String.valueOf(n.id())).
                        append("\"];\n");
            }
            // Draw switches
            for (Switch s : switches) {
                numLink = 0;
                dot.append("switch").
                        append(String.valueOf(s.id())).
                        append(" [shape=record, color=blue, label=\"Switch ").
                        append(String.valueOf(s.id())).append("\\n[").
                        append(s.getCapacity() <= 0 ? "Unlimited" : bitsToString(s.getCapacity())+"/s").
                        append("]");
                for (Port p : s.getPorts()) {
                    numLink++;
                    dot.append("|<p").append(String.valueOf(numLink)).append(">/").append(String.valueOf(numLink));
                }
                dot.append("\"];\n");
            }
            // Draw links
            for (Switch s : switches) {
                numLink = 0;
                for (Port p : s.getPorts()) {
                    numLink++;
                    if (!drawedLinks.contains(p) && !drawedLinks.contains(p.getRemote())) {
                        dot.append("switch").
                                append(String.valueOf(s.id())).
                                append(":p").
                                append(String.valueOf(s.getPorts().indexOf(p) + 1)).
                                append(" -> ");

                        if (p.getRemote().getHost() instanceof Node) {
                            dot.append("node").append(String.valueOf(((Node) p.getRemote().getHost()).id()));
                        }
                        else {
                            dot.append("switch").
                                    append(String.valueOf(((Switch) p.getRemote().getHost()).id())).
                                    append(":p").
                                    append(String.valueOf(((Switch) p.getRemote().getHost()).getPorts().
                                            indexOf(p.getRemote()) + 1));
                        }
                        dot.append(" [arrowhead=none, color=red, label=\"").
                                append(bitsToString(Math.min(p.getBandwidth(), p.getRemote().getBandwidth()))).
                                append("/s\"]\n");
                        drawedLinks.add(p);
                        drawedLinks.add(p.getRemote());
                    }
                }
            }
            dot.append("}\n");
            dot.flush();
            dot.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    private String bitsToString(long megabits) {
        int unit = 1000;
        if (megabits < unit) return megabits + " mb";
        int exp = (int) (Math.log(megabits) / Math.log(unit));
        return new DecimalFormat("#.##").format(megabits / Math.pow(unit, exp)) + "GTPE".charAt(exp-1) + "b";
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
        return this.viewId.equals(((NetworkView) o).getIdentifier());
    }

    @Override
    public ModelView clone() {
        NetworkView net = new NetworkView(routing);
        net.getSwitches().addAll(switches);
        net.swBuilder = swBuilder.clone();
        return net;
    }

    @Override
    public boolean substituteVM(VM curId, VM nextId) {
        return false;
    }
}
