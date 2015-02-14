package org.btrplace.model.view.net;

import org.btrplace.model.DefaultModel;
import org.btrplace.model.Model;
import org.btrplace.model.Node;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/**
 * Created by vkherbac on 08/12/14.
 */
public class NetworkViewTest {

    @Test
    public void testPath() {

        Model mo = new DefaultModel();
        Node n1 = mo.newNode();
        Node n2 = mo.newNode();

        NetworkView net = new NetworkView(new DefaultRouting());
        Switch s1 = net.newSwitch(2000);
        Switch sm = net.newSwitch(5000);
        Switch s2 = net.newSwitch(2000);

        s1.connect(1000, n1, sm);
        s2.connect(1000, n2, sm);

        Assert.assertTrue(net.getPath(n1, n2).containsAll(s1.getPorts()));
        Assert.assertTrue(net.getPath(n1, n2).containsAll(sm.getPorts()));
        Assert.assertTrue(net.getPath(n1, n2).containsAll(s2.getPorts()));
    }

    @Test(dependsOnMethods = {"testPath"})
    public void testMaxBW() {

        Model mo = new DefaultModel();
        Node n1 = mo.newNode();
        Node n2 = mo.newNode();

        NetworkView net = new NetworkView(new DefaultRouting());
        Switch s1 = net.newSwitch(2000);
        Switch sm = net.newSwitch(5000);
        Switch s2 = net.newSwitch(2000);

        s1.connect(1000, n1, sm);
        s2.connect(500, n2); // Bottleneck
        s2.connect(1000, sm);

        Assert.assertEquals(net.getMaxBW(n1, n2), 500);
    }

    @Test
    public void readXML() {
        try {
            File fXmlFile = new File(new File("").getAbsolutePath() + "/api/src/test/resources/g5k_grenoble.xml");

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();

            System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

            NodeList nList = ((Element) doc.getElementsByTagName("AS").item(0)).getElementsByTagName("AS");

            System.out.println("----------------------------");

            for (int temp = 0; temp < nList.getLength(); temp++) {

                org.w3c.dom.Node nNode = nList.item(temp);

                System.out.println("\nCurrent Element : " + nNode.getNodeName());

                if (nNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {

                    Element eElement = (Element) nNode;

                    // AS
                    System.out.println("AS id : " + eElement.getAttribute("id"));

                    // Router
                    for (int i=0; i<eElement.getElementsByTagName("router").getLength(); i++) {
                        System.out.println("Router : " + ((Element) eElement.getElementsByTagName("router").item(i)).getAttribute("id"));
                    }

                    // Host
                    for (int i=0; i<eElement.getElementsByTagName("host").getLength(); i++) {
                        System.out.println("Host : core=" + ((Element) eElement.getElementsByTagName("host").item(i)).getAttribute("core"));
                        System.out.println("Host : id=" + ((Element) eElement.getElementsByTagName("host").item(i)).getAttribute("id"));
                        System.out.println("Host : power=" + ((Element) eElement.getElementsByTagName("host").item(i)).getAttribute("power"));
                    }

                    // Link
                    for (int i=0; i<eElement.getElementsByTagName("link").getLength(); i++) {
                        System.out.println("Link : bandwidth=" + ((Element) eElement.getElementsByTagName("link").item(i)).getAttribute("bandwidth"));
                        System.out.println("Link : id=" + ((Element) eElement.getElementsByTagName("link").item(i)).getAttribute("id"));
                        System.out.println("Link : latency=" + ((Element) eElement.getElementsByTagName("link").item(i)).getAttribute("latency"));
                    }

                    // Route
                    for (int i=0; i<eElement.getElementsByTagName("route").getLength(); i++) {
                        System.out.println("Route : src=" + ((Element) eElement.getElementsByTagName("route").item(i)).getAttribute("src"));
                        System.out.println("Route : dst=" + ((Element) eElement.getElementsByTagName("route").item(i)).getAttribute("dst"));
                        for (int j=0; j<((Element) eElement.getElementsByTagName("route").item(i)).getElementsByTagName("link_ctn").getLength(); j++) {
                            System.out.println("  link_ctn : id=" + ((Element) ((Element) eElement.getElementsByTagName("route").item(i))
                                    .getElementsByTagName("link_ctn").item(j)).getAttribute("id"));
                        }
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
