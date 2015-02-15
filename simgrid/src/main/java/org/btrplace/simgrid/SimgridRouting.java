package org.btrplace.simgrid;

import org.btrplace.model.Node;
import org.btrplace.model.view.net.*;
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
public class SimgridRouting implements Routing {

    private NetworkView net;
    private Map<String, Node> nodes;
    private Map<String, Switch> switches;
    private Map<String, List<Port>> links;
    private File xml;

    public SimgridRouting(Map<String, Node> nodes, File xml) {
        this.nodes = nodes;
        switches = new HashMap<>();
        links = new HashMap<>();
        this.xml = xml;
    }

    public Switch getSwitch(String name) {
        return switches.get(name);
    }

    public Node getNode(String name) {
        return nodes.get(name);
    }

    public Map<String, Node> getNodesMap() {
        return nodes;
    }

    public Map<String, Switch> getSwitchesMap() {
        return switches;
    }

    @Override
    public void setNetwork(NetworkView net) {

        this.net = net;

        // Import static routes from XML file
        try {
            importRoutes();
        } catch(Exception e) {
            System.err.println("Error during static routes' import: " + e.toString());
        }
    }

    @Override
    public List<Port> getPath(Node n1, Node n2) {

        NodesMap nodesMap = new NodesMap(n1, n2);

        // Return the first path found; TODO: useless routes ! wait for topo5k update
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

    public List<Port> getPath(String n1, String n2) {

        // Return the first path found
        return getFirstPath(new ArrayList<>(Collections.singletonList(net.getSwitchInterface(nodes.get(n1)))), nodes.get(n2));
    }

    private void importRoutes() throws Exception {

        if (!xml.exists()) throw new FileNotFoundException("File '" + xml.getName() + "' not found");

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xml);
        doc.getDocumentElement().normalize();

        // Get the list of sites
        NodeList nList = ((Element) doc.getElementsByTagName("AS").item(0)).getElementsByTagName("AS");

        // For all sites
        for (int i=0; i<nList.getLength(); i++) {

            org.w3c.dom.Node nNode = nList.item(i);

            if (nNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {

                Element site = (Element) nNode;

                String siteId = site.getAttribute("id");

                boolean found;

                // Parse router as a switch
                nList = site.getElementsByTagName("router");
                for (int j=0; j<nList.getLength(); j++) {
                    org.w3c.dom.Node node = nList.item(j);

                    if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                        Element router = (Element) node;

                        // Create the new switch
                        String id = router.getAttribute("id").replace("." + siteId, "");
                        Switch sw = net.newSwitch();
                        switches.put(id, sw);
                    }
                }

                // Parse host to assign names on provided nodes
                nList = site.getElementsByTagName("host");
                for (int j = 0; j < nList.getLength(); j++) {
                    org.w3c.dom.Node node = nList.item(j);

                    if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                        Element host = (Element) node;

                        // Just check if the node exists
                        String id = host.getAttribute("id").replace("." + siteId, "");
                        int core = Integer.valueOf(host.getAttribute("core"));
                        long power = Long.valueOf(host.getAttribute("power"));
                        if (!nodes.containsKey(id)) {
                            throw new Exception("Node '" + id + "' not found");
                        }
                    }
                }

                // Parse links
                nList = site.getElementsByTagName("link");
                for (int j = 0; j < nList.getLength(); j++) {
                    org.w3c.dom.Node node = nList.item(j);

                    if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                        Element linksList = (Element) node;

                        // Connect elements
                        String id = linksList.getAttribute("id");
                        String src = id.split("_")[0].replace("." + siteId, "");
                        String dst = id.split("_")[1].replace("." + siteId, "");
                        int bandwidth = (int) (Long.valueOf(linksList.getAttribute("bandwidth"))/1000000);
                        double latency = Double.valueOf(linksList.getAttribute("latency"));

                        //TODO: just a temporary patch
                        if (src.contains("renater") || dst.contains("renater")) {
                            continue;
                        }

                        // Create a new switch if detected (switch entry does not exists)
                        if (src.contains("-sw")) {
                            if (!switches.containsKey(src)) {
                                Switch sw = net.newSwitch();
                                switches.put(src, sw);
                            }
                        }
                        if (dst.contains("-sw")) {
                            if (!switches.containsKey(dst)) {
                                Switch sw = net.newSwitch();
                                switches.put(dst, sw);
                            }
                        }

                        if (switches.containsKey(src)) {
                            // Connect two switches
                            if (switches.containsKey(dst)) {
                                switches.get(src).connect(bandwidth, switches.get(dst));
                                links.put(id, Arrays.asList(switches.get(src).getPorts().get(switches.get(src).getPorts().size() - 1),
                                        switches.get(dst).getPorts().get(switches.get(dst).getPorts().size() - 1)));
                            }
                            // Switch to node
                            else {
                                switches.get(src).connect(bandwidth, nodes.get(dst));
                                links.put(id, Arrays.asList(switches.get(src).getPorts().get(switches.get(src).getPorts().size() - 1),
                                        net.getSwitchInterface(nodes.get(dst))));
                            }
                        } else if (switches.containsKey(dst)) {

                            // Node to switch
                            switches.get(dst).connect(bandwidth, nodes.get(src));
                            links.put(id, Arrays.asList(net.getSwitchInterface(nodes.get(src)),
                                    switches.get(dst).getPorts().get(switches.get(dst).getPorts().size() - 1)));
                        }
                    }
                }

                /* TODO: routes that are not between two end nodes are useless
                // Parse routes
                nList = site.getElementsByTagName("route");
                for (int j=0; j<nList.getLength(); j++) {
                    org.w3c.dom.Node node = nList.item(j);

                    if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                        Element route = (Element) node;

                        String src = route.getAttribute("src").replace("."+siteId, "");
                        String dst = route.getAttribute("dst").replace("."+siteId, "");

                        org.btrplace.model.Element srcElt = null, dstElt = null;

                        for (String n : nodes.keySet()) {
                            if (n.equals(src)) { srcElt = nodes.get(n); break; }
                        }
                        if (srcElt == null) {
                            for (String n : switches.keySet()) {
                                if (n.equals(src)) { srcElt = switches.get(n); break; }
                            }
                            if (srcElt == null) throw new Exception("Cannot find the id '"+ src +"'");
                        }
                        for (String n : nodes.keySet()) {
                            if (n.equals(dst)) { dstElt = nodes.get(n); break; }
                        }
                        if (dstElt == null) {
                            for (String n : switches.keySet()) {
                                if (n.equals(dst)) { dstElt = switches.get(n); break; }
                            }
                            if (dstElt == null) throw new Exception("Cannot find the id '"+ dst +"'");
                        }

                        NodesMap nodesMap = new NodesMap(srcElt, dstElt);
                        List<Port> ports = new ArrayList<>();

                        // Parse route's links
                        NodeList linksList = route.getElementsByTagName("link_ctn");
                        for (int k=0; k<linksList.getLength(); k++) {
                            Element link = (Element) linksList.item(k);
                            String id = link.getAttribute("id");

                            // Get all the link's ports
                            found = false;
                            for (String l : links.keySet()) {
                                if (l.equals(id)) { ports.addAll(links.get(l)); found = true; break; }
                            }
                            if (!found) throw new Exception("Cannot find the link with id '"+ id +"'");
                        }

                        // Add the new route
                        routes.put(nodesMap, ports);
                    }
                }
                */
            }
        }
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
