package edu.virginia.lib;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import net.sf.saxon.dom.DocumentBuilderImpl;

/**
 * A cmomand line tool that reads Fedora FOXML files, and outputs a CSV 
 * file that maps pids, datastream names, to the URL to which they redirect.
 */
public class ExternalDSMapper {
    public static void main( String[] args ) {
        
        ExternalDSMapper m = new ExternalDSMapper();
        
        File root = new File(args[0]);
        if (root.isDirectory()) {
            m.processDir(root);
        } else if (root.isFile()) {
            m.processFile(root);
        }
    }
    
    private XPath xpath;

    public ExternalDSMapper() {
        xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                if (prefix.equals("foxml")) {
                    return "info:fedora/fedora-system:def/foxml#";
                } else {
                    return null;
                }
            }

            @Override
            public String getPrefix(String namespaceURI) {
                if (namespaceURI.equals("info:fedora/fedora-system:def/foxml#")) {
                    return "foxml";
                } else {
                    return null;
                }
            }

            @Override
            public Iterator getPrefixes(String namespaceURI) {
                if (namespaceURI.equals("info:fedora/fedora-system:def/foxml#")) {
                    return Collections.singleton(getPrefix("foxml")).iterator();
                }
                return null;
            }
        });

    }
    
    
    private void processFile(File f) {
        DocumentBuilder b = new DocumentBuilderImpl();
        try {
            Document doc = b.parse(f);
            String pid = (String) xpath.compile("/foxml:digitalObject/@PID").evaluate(doc, XPathConstants.STRING);
            Matcher m = Pattern.compile("\\Quva-lib:\\E([0-9][0-9])?([0-9][0-9])?([0-9][0-9])?([0-9][0-9])?([0-9])?").matcher(pid);
            String iiifPath = (m.matches() ? "/var/www/html/iiif/uva-lib/" + Objects.toString(m.group(1), "") + "/" + Objects.toString(m.group(2), "") + "/" + Objects.toString(m.group(3), "") + "/" + Objects.toString(m.group(4), "") + "/" + Objects.toString(m.group(1), "") + Objects.toString(m.group(2), "") + Objects.toString(m.group(3), "") + Objects.toString(m.group(4), "") + ".jp2" : "");
            XPathExpression ds = xpath.compile("/foxml:digitalObject/foxml:datastream[@ID='MAX' and @CONTROL_GROUP='E']/foxml:datastreamVersion/foxml:contentLocation");
            NodeList nl = (NodeList) ds.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nl.getLength() ; i ++) {
                System.out.println(pid + "," + iiifPath + "," + nl.item(i).getAttributes().getNamedItem("REF").getNodeValue());
            }
        } catch (SAXException | IOException e) {
            System.err.println(f.getAbsolutePath() + " unable to be parsed as XML.");
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        
    }
    
    private void processDir(File dir) {
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
               processFile(f);
            } else {
               processDir(f); 
            }
        }
    }
}
