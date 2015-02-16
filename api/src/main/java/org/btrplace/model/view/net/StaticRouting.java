package org.btrplace.model.view.net;

import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Created by vkherbac on 08/01/15.
 */
public class StaticRouting implements Routing {

    protected NetworkView net;
    protected File xml;

    protected List<Switch> switches;
    protected Map<Integer, List<Port>> links;
    protected Map<NodesMap, List<Port>> routes;


    public StaticRouting() {
        switches = new ArrayList<>();
        links = new HashMap<>();
        routes = new HashMap<>();
    }

    @Override
    public void setNetwork(NetworkView net) {
        this.net = net;
    }

    @Override
    public List<Port> getPath(Node n1, Node n2) {

        NodesMap nodesMap = new NodesMap(n1, n2);

        // Check for a static route
        for (NodesMap nm : routes.keySet()) {
            if (nm.equals(nodesMap)) {
                return routes.get(nm);
            }
        }

        // If not found, return the first path found
        return getFirstPath(new ArrayList<>(Collections.singletonList(net.getSwitchInterface(n1))), n2);
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

    public void addStaticRoute(NodesMap nm, List<Port> ports) {
        routes.put(nm, ports);
    }

    public List<Node> importXML(Model mo, File xml) {

        List<Node> nodes = new ArrayList<>();

        try {
            if (!xml.exists()) throw new FileNotFoundException("File '" + xml.getName() + "' not found");

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xml);
            doc.getDocumentElement().normalize();

            org.w3c.dom.Node root = doc.getDocumentElement();
            NodeList nList;

            boolean found;

            // Parse nodes
            nList = ((Element) root).getElementsByTagName("node");
            for (int i = 0; i < nList.getLength(); i++) {
                org.w3c.dom.Node node = nList.item(i);

                if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    Element elt = (Element) node;

                    // Create the node and add it to the list
                    int id = Integer.valueOf(elt.getAttribute("id"));
                    int cpu = Integer.valueOf(elt.getAttribute("cpu"));
                    int ram = Integer.valueOf(elt.getAttribute("ram"));
                    if (!mo.contains(new Node(id))) {
                        Node n = mo.newNode(id);
                        nodes.add(n);
                    }
                /*found = false;
                for (Node n : nodes) { if (n.id() == id) { found = true; break; } }
                if (!found) throw new Exception("Node with id '"+ id +"' not found");*/
                }
            }

            // Parse switches
            nList = ((Element) root).getElementsByTagName("switch");
            for (int i = 0; i < nList.getLength(); i++) {
                org.w3c.dom.Node node = nList.item(i);

                if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    Element elt = (Element) node;

                    // Create the new Switch
                    int id = Integer.valueOf(elt.getAttribute("id"));
                    int capacity = Integer.valueOf(elt.getAttribute("capacity"));
                    switches.add(net.newSwitch(id, capacity));
                }
            }

            // Parse links
            nList = ((Element) root).getElementsByTagName("link");
            for (int i = 0; i < nList.getLength(); i++) {
                org.w3c.dom.Node node = nList.item(i);

                if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    Element elt = (Element) node;

                    // Connect elements
                    int id = Integer.valueOf(elt.getAttribute("id"));
                    int bandwidth = Integer.valueOf(elt.getAttribute("bandwidth"));
                    String leftType = elt.getAttribute("left").split("_")[0];
                    int leftId = Integer.valueOf(elt.getAttribute("left").split("_")[1]);
                    String rightType = elt.getAttribute("right").split("_")[0];
                    int rightId = Integer.valueOf(elt.getAttribute("right").split("_")[1]);

                    if (leftType.equals("switch")) {
                        Switch leftSwitch = null;
                        for (Switch sw : switches) {
                            if (sw.id() == leftId) leftSwitch = sw;
                        }
                        if (leftSwitch == null)
                            throw new Exception("Cannot find the switch with id '" + leftId + "'");

                        if (rightType.equals("switch")) {
                            Switch rightSwitch = null;
                            for (Switch sw : switches) {
                                if (sw.id() == rightId) rightSwitch = sw;
                            }
                            if (rightSwitch == null)
                                throw new Exception("Cannot find the switch with id '" + rightId + "'");

                            // Connect two switches
                            leftSwitch.connect(bandwidth, rightSwitch);
                            links.put(id, Arrays.asList(leftSwitch.getPorts().get(leftSwitch.getPorts().size() - 1),
                                    rightSwitch.getPorts().get(rightSwitch.getPorts().size() - 1)));
                        } else {
                            Node rightNode = null;
                            for (Node n : nodes) {
                                if (n.id() == rightId) rightNode = n;
                            }
                            if (rightNode == null)
                                throw new Exception("Cannot find the node with id '" + rightId + "'");

                            // Connect switch to node
                            leftSwitch.connect(bandwidth, rightNode);
                            links.put(id, Arrays.asList((net.getSwitchInterface(rightNode))));
                        }
                    } else {
                        Node leftNode = null;
                        for (Node n : nodes) {
                            if (n.id() == leftId) leftNode = n;
                        }
                        if (leftNode == null) throw new Exception("Cannot find the node with id '" + leftId + "'");

                        if (rightType.equals("switch")) {
                            Switch rightSwitch = null;
                            for (Switch sw : switches) {
                                if (sw.id() == rightId) rightSwitch = sw;
                            }
                            if (rightSwitch == null)
                                throw new Exception("Cannot find the switch with id '" + rightId + "'");

                            // Connect node to switch
                            rightSwitch.connect(bandwidth, leftNode);
                            links.put(id, Arrays.asList(net.getSwitchInterface(leftNode)));
                        } else {
                            throw new Exception("Cannot link two nodes together !");
                        }
                    }
                }
            }

            // Parse routes
            nList = ((Element) root).getElementsByTagName("route");
            for (int i = 0; i < nList.getLength(); i++) {
                org.w3c.dom.Node node = nList.item(i);

                if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    Element elt = (Element) node;

                    int src = Integer.valueOf(elt.getAttribute("src"));
                    Node srcNode = null;
                    for (Node n : nodes) {
                        if (n.id() == src) srcNode = n;
                    }
                    if (srcNode == null) throw new Exception("Cannot find the node with id '" + src + "'");

                    int dst = Integer.valueOf(elt.getAttribute("dst"));
                    Node dstNode = null;
                    for (Node n : nodes) {
                        if (n.id() == dst) dstNode = n;
                    }
                    if (dstNode == null) throw new Exception("Cannot find the node with id '" + dst + "'");

                    NodesMap nodesMap = new NodesMap(srcNode, dstNode);
                    List<Port> ports = new ArrayList<>();

                    // Parse route's links
                    NodeList lnks = elt.getElementsByTagName("lnk");
                    for (int j = 0; j < lnks.getLength(); j++) {
                        Element lnk = (Element) lnks.item(j);
                        int id = Integer.valueOf(lnk.getAttribute("id"));

                        // Get all the link's ports
                        if (links.containsKey(id)) {
                            ports.addAll(links.get(id));
                        } else {
                            throw new Exception("Cannot find the link with id '" + id + "'");
                        }
                    }

                    // Add the new route
                    routes.put(nodesMap, ports);
                }
            }
        }
        catch (Exception e) {
            System.err.println("Error during XML import: " + e.toString());
            e.printStackTrace();
            return null;
        }

        return nodes;
    }

    protected List<Port> getFirstPath(List<Port> currentPath, Node dst) {

        if (currentPath.get(currentPath.size()-1).getHost() instanceof Switch) {
            for (Port p : ((Switch) currentPath.get(currentPath.size() - 1).getHost()).getPorts()) {
                if (currentPath.contains(p)) {
                    continue;
                }
                currentPath.add(p);
                if (p.getRemote().getHost() instanceof Node) {
                    if (p.getRemote().getHost().equals(dst)) {
                        return currentPath;
                    }
                } else {
                    currentPath.add(p.getRemote());
                    return getFirstPath(currentPath, dst);
                }
                currentPath.remove(currentPath.size() - 1);
            }
            currentPath.remove(currentPath.size() - 1);
            return currentPath;

        } else {
            return Collections.emptyList();
        }
    }
}
