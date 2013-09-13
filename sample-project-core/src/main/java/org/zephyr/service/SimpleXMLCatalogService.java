package org.zephyr.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.zephyr.data.Entry;

/**
 * If the term is not found in the XML file, it throws a RuntimeException.
 */
@Deprecated
public class SimpleXMLCatalogService implements CatalogService {

    private Map<String, CatalogEntry> entries = new HashMap<String, CatalogEntry>();

    public SimpleXMLCatalogService(String catalogTermsFile) {
        try {
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            //File file = new File(ClassLoader.getSystemClassLoader().getResource("catalog-terms.xml").getFile());
            Document doc = docBuilder.parse(this.getClass().getClassLoader().getResourceAsStream(catalogTermsFile));
            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile("//catalog/term");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            if (result != null) {
                NodeList nodes = (NodeList) result;
                for (int i = 0; i < nodes.getLength(); i++) {
                    Node node = nodes.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        if (!element.hasChildNodes()) {
                            if (element.hasAttributes()) {
                                NamedNodeMap attrs = element.getAttributes();
                                if (attrs.getNamedItem("category") != null && attrs.getNamedItem("type") != null && attrs.getNamedItem("alias") != null) {
                                    String category = attrs.getNamedItem("category").getNodeValue();
                                    entries.put(category, new CatalogEntry(category, attrs.getNamedItem("alias").getNodeValue(), attrs.getNamedItem("type").getNodeValue()));
                                }
                            }
                        }
                    }
                }
            }
            entries = Collections.unmodifiableMap(entries);
        } catch (Throwable t) {
            // catch literally everything that can be thrown; there is nothing more we can do here.
            throw new RuntimeException(t);
        }
    }

    public Entry getEntry(String label, String value, List<String> types, String visibility, String metadata) {
        CatalogEntry entry = entries.get(label);
        if (entry == null) {
            throw new RuntimeException("The requested catalog term, " + label + ", was not found in the catalog-terms.xml on the classpath.");
        }
        return new Entry(entry.category, value, types, visibility, metadata);
    }

    private static class CatalogEntry {
        public final String category;
        @SuppressWarnings("unused")
        // TODO: Do something with Alias?
        public final String alias;
        @SuppressWarnings("unused")
        public final String type;

        public CatalogEntry(final String category, final String alias, final String type) {
            this.category = category;
            this.alias = alias;
            this.type = type;
        }

    }

}
