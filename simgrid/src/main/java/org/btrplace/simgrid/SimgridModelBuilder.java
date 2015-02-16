/*
 * Copyright (c) 2014 University Nice Sophia Antipolis
 *
 * This file is part of btrplace.
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.btrplace.simgrid;

import org.btrplace.model.DefaultModel;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.btrplace.model.view.NamingService;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.model.view.net.NetworkView;
import org.btrplace.model.view.net.Port;
import org.btrplace.model.view.net.StaticRouting;
import org.btrplace.model.view.net.Switch;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Default implementation for a {@link org.btrplace.model.Model}.
 *
 * @author Vincent Kherbache
 */
public class SimgridModelBuilder {

    private NamingService<Node> nsNodes;
    private NamingService<Switch> nsSwitches;
    private ShareableResource rcCPU;
    private List<Node> nodes;
    private Map<String, List<Port>> links;

    public Model build(File xml) {

        nodes = new ArrayList<>();
        links = new HashMap<>();

        Model mo = new DefaultModel();

        // Create and attach the nodes' namingService view
        nsNodes = NamingService.newNodeNS();
        mo.attach(nsNodes);

        // Create and attach the switches' namingService view
        nsSwitches = NamingService.newSwitchNS();
        mo.attach(nsSwitches);

        // Create and attach the shareableResource view
        rcCPU = new ShareableResource("core", 1, 1);
        mo.attach(rcCPU);

        // Import nodes from Simgrid XML file
        try {
            importNodes(mo, xml);
        } catch(Exception e) {
            System.err.println("Error during Simgrid nodes import: " + e.toString());
            e.printStackTrace();
        }

        // Create and attach the network view
        NetworkView netView = new NetworkView(new StaticRouting());
        mo.attach(netView);

        // Import routes from Simgrid XML file
        try {
            importRoutes(netView, xml);
        } catch(Exception e) {
            System.err.println("Error during Simgrid routes import: " + e.toString());
            e.printStackTrace();
        }

        return mo;
    }

    private void importNodes(Model mo, File xml) throws Exception {

        if (!xml.exists()) throw new FileNotFoundException("File '" + xml.getName() + "' not found");

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xml);
        doc.getDocumentElement().normalize();

        // Root is on the first AS (id='grid5000.fr')
        Element root = (Element) doc.getElementsByTagName("AS").item(0);

        // Get the list of sites
        NodeList sitesList = root.getElementsByTagName("AS");

        // For all sites
        for (int i=0; i<sitesList.getLength(); i++) {

            org.w3c.dom.Node nSite = sitesList.item(i);

            if (nSite.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {

                org.w3c.dom.Element eSite = (org.w3c.dom.Element) nSite;

                String siteId = eSite.getAttribute("id");

                // Parse host to assign names on provided nodes
                NodeList hostsList = eSite.getElementsByTagName("host");
                for (int j = 0; j < hostsList.getLength(); j++) {
                    org.w3c.dom.Node node = hostsList.item(j);

                    if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                        org.w3c.dom.Element host = (org.w3c.dom.Element) node;

                        // Create a new node with the right name
                        String id = host.getAttribute("id").replace("." + siteId, "");
                        int core = Integer.valueOf(host.getAttribute("core"));
                        double power = Double.valueOf(host.getAttribute("power"));
                        Node n = mo.newNode();
                        nsNodes.register(n, id);
                        nodes.add(n);

                        // Set cpu capacity
                        rcCPU.setCapacity(n, core);
                    }
                }
            }
        }
    }

    private void importRoutes(NetworkView nv, File xml) throws Exception {

        if (!xml.exists()) throw new FileNotFoundException("File '" + xml.getName() + "' not found");

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xml);
        doc.getDocumentElement().normalize();

        // Root is on the first AS (id='grid5000.fr')
        Element root = (Element) doc.getElementsByTagName("AS").item(0);

        // Get the list of sites
        NodeList sitesList = root.getElementsByTagName("AS");

        // For all sites
        for (int i=0; i<sitesList.getLength(); i++) {

            org.w3c.dom.Node nSite = sitesList.item(i);

            if (nSite.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {

                Element eSite = (Element) nSite;

                String siteId = eSite.getAttribute("id");

                boolean found;

                // Parse router as a switch
                NodeList routersList = eSite.getElementsByTagName("router");
                for (int j=0; j<routersList.getLength(); j++) {
                    org.w3c.dom.Node nRouter = routersList.item(j);

                    if (nRouter.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                        Element eRouter = (Element) nRouter;

                        // Create the new switch
                        String id = eRouter.getAttribute("id").replace("." + siteId, "");
                        Switch sw = nv.newSwitch();
                        nsSwitches.register(sw, id);
                    }
                }

                // Parse links
                NodeList linksList = eSite.getElementsByTagName("link");
                for (int j = 0; j < linksList.getLength(); j++) {
                    org.w3c.dom.Node nLink = linksList.item(j);

                    if (nLink.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                        Element eLink = (Element) nLink;

                        // Connect elements
                        String id = eLink.getAttribute("id");
                        String src = id.split("_")[0].replace("." + siteId, "");
                        String dst = id.split("_")[1].replace("." + siteId, "");
                        int bandwidth = (int) (Long.valueOf(eLink.getAttribute("bandwidth")) / 1000000);
                        double latency = Double.valueOf(eLink.getAttribute("latency"));

                        //TODO: just a temporary patch (doublon: already declared in root links)
                        if (src.contains("renater") || dst.contains("renater")) { continue; }

                        // Create a new switch if detected (switch entry does not exists)
                        if (src.contains("-sw") || src.contains("force10") || src.contains("mxl1")
                                || src.contains("edgeiron") || src.contains("sgriffon") || src.contains("sgraphene")) {
                            if (nsSwitches.resolve(src) == null) {
                                Switch sw = nv.newSwitch();
                                nsSwitches.register(sw, src);
                            }
                        }
                        if (dst.contains("-sw") || dst.contains("force10") || dst.contains("mxl1")
                                || dst.contains("edgeiron") || dst.contains("sgriffon") || dst.contains("sgraphene")) {
                            if (nsSwitches.resolve(dst) == null) {
                                Switch sw = nv.newSwitch();
                                nsSwitches.register(sw, dst);
                            }
                        }

                        if (nsSwitches.resolve(src) != null) {
                            // Connect two switches
                            if (nsSwitches.resolve(dst) != null) {
                                nsSwitches.resolve(src).connect(bandwidth, nsSwitches.resolve(dst));
                                links.put(id, Arrays.asList(
                                    nsSwitches.resolve(src).getPorts().get(nsSwitches.resolve(src).getPorts().size() - 1),
                                    nsSwitches.resolve(dst).getPorts().get(nsSwitches.resolve(dst).getPorts().size() - 1))
                                );
                            }
                            // Switch to node
                            else {
                                nsSwitches.resolve(src).connect(bandwidth, nsNodes.resolve(dst));
                                links.put(id, Arrays.asList(
                                    nsSwitches.resolve(src).getPorts().get(nsSwitches.resolve(src).getPorts().size() - 1),
                                    nv.getSwitchInterface(nsNodes.resolve(dst)))
                                );
                            }

                        } else if (nsSwitches.resolve(dst) != null) {

                            // Node to switch
                            nsSwitches.resolve(dst).connect(bandwidth, nsNodes.resolve(src));
                            links.put(id, Arrays.asList(
                                nv.getSwitchInterface(nsNodes.resolve(src)),
                                nsSwitches.resolve(dst).getPorts().get(nsSwitches.resolve(dst).getPorts().size() - 1))
                            );
                        }
                    }
                }

                /* TODO: routes that are not between two end nodes are useless
                // Parse routes
                NodeList routesList = site.getElementsByTagName("route");
                for (int j=0; j<routesList.getLength(); j++) {
                    org.w3c.dom.Node nRoute = routesList.item(j);

                    if (nRoute.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                        Element route = (Element) nRoute;

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

                        // Add the new route to the view
                        ((StaticRouting)netView.getRouting()).addStaticRoute(nodesMap, ports);
                    }
                }
                */
            }
        }

        // Add inter-sites links
        NodeList linksList = root.getElementsByTagName("link");

        // For all sites
        for (int i=0; i<linksList.getLength(); i++) {

            org.w3c.dom.Node nLink = linksList.item(i);

            // Only look at first child
            if (root.equals(nLink.getParentNode()) && nLink.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                Element eLink = (Element) nLink;

                // Connect root switches
                String id = eLink.getAttribute("id");
                String src = id.split("_")[0].contains("gw-") ? id.split("_")[0].split("\\.")[0] : id.split("_")[0];
                String dst = id.split("_")[1].contains("gw-") ? id.split("_")[1].split("\\.")[0] : id.split("_")[1];
                int bandwidth = (int) (Long.valueOf(eLink.getAttribute("bandwidth")) / 1000000);
                double latency = Double.valueOf(eLink.getAttribute("latency"));

                // Replace '.' by '-' for consistency
                if (src.contains("renater")) src = src.replace(".", "-");
                if (dst.contains("renater")) dst = dst.replace(".", "-");

                // We assumes they are all switches
                if (nsSwitches.resolve(src) == null) {
                    Switch sw = nv.newSwitch();
                    nsSwitches.register(sw, src);
                }
                if (nsSwitches.resolve(dst) == null) {
                    Switch sw = nv.newSwitch();
                    nsSwitches.register(sw, dst);
                }

                // Connect them together
                nsSwitches.resolve(src).connect(bandwidth, nsSwitches.resolve(dst));
                links.put(id, Arrays.asList(
                    nsSwitches.resolve(src).getPorts().get(nsSwitches.resolve(src).getPorts().size() - 1),
                    nsSwitches.resolve(dst).getPorts().get(nsSwitches.resolve(dst).getPorts().size() - 1))
                );
            }
        }
    }
}
