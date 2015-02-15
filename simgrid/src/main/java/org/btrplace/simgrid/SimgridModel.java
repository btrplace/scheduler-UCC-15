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

import org.btrplace.model.*;
import org.btrplace.model.Node;
import org.btrplace.model.view.ModelView;
import org.btrplace.model.view.net.NetworkView;
import org.w3c.dom.*;

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
public class SimgridModel implements Model, Cloneable {

    private Mapping cfg;

    private Map<String, ModelView> resources;

    private Attributes attrs;

    private SimgridElementBuilder elemBuilder;

    private File xml;

    /**
     * Make a new instance relying on a {@link org.btrplace.simgrid.SimgridElementBuilder}.
     *
     * @param xml the Simgrid's XML description file to parse
     */
    public SimgridModel(File xml) {
        this(new SimgridElementBuilder(), xml);
    }

    /**
     * Make a new instance relying on a specific elementBuilder.
     *
     * @param seb the custom elementBuilder to use
     * @param xml the Simgrid's XML description file to parse
     */
    public SimgridModel(SimgridElementBuilder seb, File xml) {

        this.resources = new HashMap<>();
        attrs = new DefaultAttributes();
        cfg = new DefaultMapping();
        elemBuilder = seb;
        this.xml = xml;

        // Import nodes from Simgrid XML file
        try {
            importSimgridNodes(xml);
        } catch(Exception e) {
            System.err.println("Error during Simgrid import: " + e.toString());
            e.printStackTrace();
        }

        // Create and attach the network view
        NetworkView netView = new NetworkView(new SimgridRouting(elemBuilder.getNodes(), xml));
        attach(netView);
    }

    private void importSimgridNodes(File xml) throws Exception {

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

                        // Create a new node withx the right name
                        String id = host.getAttribute("id").replace("." + siteId, "");
                        int core = Integer.valueOf(host.getAttribute("core"));
                        long power = Long.valueOf(host.getAttribute("power"));
                        elemBuilder.newNode(id);
                    }
                }
            }
        }
    }

    public Map<String, Node> getNodes() {
        return elemBuilder.getNodes();
    }

    public Node getNode(String name) {
        return elemBuilder.getNode(name);
    }

    public boolean containsNode(String name) {
        return elemBuilder.containsNode(name);
    }

    @Override
    public ModelView getView(String id) {
        return this.resources.get(id);
    }

    @Override
    public boolean attach(ModelView v) {
        if (this.resources.containsKey(v.getIdentifier())) {
            return false;
        }
        this.resources.put(v.getIdentifier(), v);
        return true;
    }

    @Override
    public Collection<ModelView> getViews() {
        return this.resources.values();
    }

    @Override
    public Mapping getMapping() {
        return this.cfg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Model that = (Model) o;

        if (!cfg.equals(that.getMapping())) {
            return false;
        }

        if (!attrs.equals(that.getAttributes())) {
            return false;
        }
        Collection<ModelView> thatViews = that.getViews();
        return resources.size() == thatViews.size() && resources.values().containsAll(thatViews);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cfg, resources, attrs);
    }

    @Override
    public boolean detach(ModelView v) {
        return resources.remove(v.getIdentifier()) != null;
    }

    @Override
    public void clearViews() {
        this.resources.clear();
    }

    @Override
    public Attributes getAttributes() {
        return attrs;
    }

    @Override
    public void setAttributes(Attributes a) {
        attrs = a;
    }

    @Override
    public Model clone() {
        SimgridModel m = new SimgridModel((SimgridElementBuilder)elemBuilder.clone(), xml);
        MappingUtils.fill(cfg, m.cfg);
        for (ModelView rc : resources.values()) {
            m.attach(rc.clone());
        }
        m.setAttributes(this.getAttributes().clone());
        return m;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Mapping:\n");
        b.append(getMapping());
        b.append("\nAttributes:\n");
        b.append(getAttributes());
        b.append("\nViews:\n");
        for (Map.Entry<String, ModelView> entry : resources.entrySet()) {
            b.append(entry.getKey()).append(": ");
            b.append(entry.getValue()).append("\n");
        }
        return b.toString();
    }

    @Override
    public VM newVM() {
        return elemBuilder.newVM();
    }

    @Override
    public VM newVM(int id) {
        return elemBuilder.newVM(id);
    }

    @Override
    public Node newNode() {
        return elemBuilder.newNode();
    }

    @Override
    public Node newNode(int id) {
        return elemBuilder.newNode(id);
    }

    @Override
    public boolean contains(VM v) {
        return elemBuilder.contains(v);
    }

    @Override
    public boolean contains(Node n) {
        return elemBuilder.contains(n);
    }
}
