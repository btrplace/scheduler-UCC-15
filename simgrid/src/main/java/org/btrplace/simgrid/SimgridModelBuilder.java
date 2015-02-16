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
public final class SimgridModelBuilder {

    private static NamingService<Node> nsNodes;
    private static NamingService<Switch> nsSwitches;
    private static ShareableResource rcCPU;
    private static List<Node> nodes;
    private static Map<String, List<Port>> links;

    public static Model build(File xml) {

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
        rcCPU = new ShareableResource("cpu", 1, 1);
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

    private static void importNodes(Model mo, File xml) throws Exception {

        if (!xml.exists()) throw new FileNotFoundException("File '" + xml.getName() + "' not found");

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xml);
        doc.getDocumentElement().normalize();

        // Get the list of sites
        NodeList nList = ((org.w3c.dom.Element) doc.getElementsByTagName("AS").item(0)).getElementsByTagName("AS");

        // For all sites
        for (int i = 0; i < nList.getLength(); i++) {

            org.w3c.dom.Node nNode = nList.item(i);

            if (nNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {

                org.w3c.dom.Element site = (org.w3c.dom.Element) nNode;

                String siteId = site.getAttribute("id");

                // Parse host to assign names on provided nodes
                nList = site.getElementsByTagName("host");
                for (int j = 0; j < nList.getLength(); j++) {
                    org.w3c.dom.Node node = nList.item(j);

                    if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                        org.w3c.dom.Element host = (org.w3c.dom.Element) node;

                        // Create a new node with the right name
                        String id = host.getAttribute("id").replace("." + siteId, "");
                        int core = Integer.valueOf(host.getAttribute("core"));
                        long power = Long.valueOf(host.getAttribute("power"));
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

    private static void importRoutes(NetworkView nv, File xml) throws Exception {

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
                        Switch sw = nv.newSwitch();
                        nsSwitches.register(sw, id);
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
                        int bandwidth = (int) (Long.valueOf(linksList.getAttribute("bandwidth")) / 1000000);
                        double latency = Double.valueOf(linksList.getAttribute("latency"));

                        //TODO: just a temporary patch
                        if (src.contains("renater") || dst.contains("renater")) {
                            continue;
                        }

                        // Create a new switch if detected (switch entry does not exists)
                        if (src.contains("-sw")) {
                            if (nsSwitches.resolve(src) == null) {
                                Switch sw = nv.newSwitch();
                                nsSwitches.register(sw, src);
                            }
                        }
                        if (dst.contains("-sw")) {
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

                        // Add the new route to the view
                        ((StaticRouting)netView.getRouting()).addStaticRoute(nodesMap, ports);
                    }
                }
                */
            }
        }
    }
}
