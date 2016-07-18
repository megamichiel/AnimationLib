package me.megamichiel.animationlib.config;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlConfig extends MapConfig {

    private DocumentBuilderFactory factory;
    private final Transformer transformer;

    {
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String saveToString() {
        try {
            if (factory == null)
                factory = DocumentBuilderFactory.newInstance();

            Document doc = factory.newDocumentBuilder().newDocument();

            Element head = doc.createElement("config");

            save(doc, toRawMap(), head);

            doc.appendChild(head);

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));

            return writer.getBuffer().toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    private void save(Document doc, Map<?, ?> map, Node node) {
        for (Map.Entry entry : map.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            Element ele = doc.createElement(key);

            if (value instanceof Map) save(doc, (Map) value, ele);
            else if (value instanceof List) save(doc, (List) value, ele);
            else ele.appendChild(doc.createTextNode(value.toString()));

            node.appendChild(ele);
        }
    }

    private void save(Document doc, List<?> list, Element node) {
        node.setAttribute("type", "list");
        for (int i = 0, size = list.size(); i < size; i++) {
            Object o = list.get(i);
            Element item = doc.createElement("item");
            item.setAttribute("index", Integer.toString(i + 1));

            if (o instanceof Map) save(doc, (Map) o, item);
            else if (o instanceof List) save(doc, (List) o, item);
            else item.appendChild(doc.createTextNode(o.toString()));

            node.appendChild(item);
        }
    }

    @Override
    public XmlConfig loadFromString(String dump) {
        super.loadFromString(dump);

        if (factory == null)
            factory = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc = builder.parse(new InputSource(new StringReader(dump)));;

            Map<String, Object> map = new HashMap<>();

            NodeList children = doc.getChildNodes();
            for (int i = 0, length = children.getLength(); i < length; i++) {
                Node item = children.item(i);
                if (item instanceof Element) {
                    Object value = load((Element) item);
                    if (value != null)
                        map.put(((Element) item).getTagName(), value);
                }
            }
            deserialize(m -> m, map);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return this;
    }

    private Object load(Element node) {
        switch (node.getNodeType()) {
            case 1: case 9:
                NodeList children = node.getChildNodes();
                int length = children.getLength();
                if (node.hasAttribute("type") && "list".equals(node.getAttribute("type"))) {
                    List<Object> list = new ArrayList<>();
                    NodeList items = node.getElementsByTagName("item");
                    length = items.getLength();
                    for (int i = 1; ; i++) {
                        String s = Integer.toString(i);
                        boolean found = false;
                        for (int j = 0; j < length; j++) {
                            Node item = items.item(j);
                            if (item instanceof Element
                                    && s.equals(((Element) item).getAttribute("index"))) {
                                list.add(load((Element) item));
                                found = true;
                                break;
                            }
                        }
                        if (!found) return list;
                    }
                }
                if (length == 1 && children.item(0) instanceof Text)
                    return ((Text) children.item(0)).getWholeText();
                Map<String, Object> map = new HashMap<>();
                for (int i = 0; i < length; i++) {
                    Node item = children.item(i);
                    if (item instanceof Element) {
                        Object value = load((Element) item);
                        if (value != null)
                            map.put(((Element) item).getTagName(), value);
                    }
                }
                return map;
        }
        return null;
    }

    @Override
    public void setIndent(int indent) {
        super.setIndent(indent);
        transformer.setOutputProperty(
                "{http://xml.apache.org/xslt}indent-amount",
                Integer.toString(indent));
    }
}
