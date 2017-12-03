package me.megamichiel.animationlib.config.type;

import me.megamichiel.animationlib.LazyValue;
import me.megamichiel.animationlib.config.MapConfig;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class XmlConfig extends MapConfig {

    private static final long serialVersionUID = 8119705465099280977L;

    private final transient Supplier<DocumentBuilderFactory> factory = LazyValue.of(DocumentBuilderFactory::newInstance);
    private final transient Supplier<Transformer> transformer = LazyValue.of(() -> {
        try {
            Transformer tf = TransformerFactory.newInstance().newTransformer();
            tf.setOutputProperty(OutputKeys.INDENT, "yes");
            tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            return tf;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    });

    @Override
    public String saveToString() {
        try {
            Document doc = factory.get().newDocumentBuilder().newDocument();

            Element head = doc.createElement("config");

            save(doc, toRawMap(), head);

            doc.appendChild(head);

            StringWriter writer = new StringWriter();
            transformer.get().transform(new DOMSource(doc), new StreamResult(writer));

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
        for (Object o : list) {
            Element item = doc.createElement("item");

            if (o instanceof Map) save(doc, (Map) o, item);
            else if (o instanceof List) save(doc, (List) o, item);
            else item.appendChild(doc.createTextNode(o.toString()));

            node.appendChild(item);
        }
    }

    @Override
    public void loadFromString(String dump) {
        super.loadFromString(dump);

        try {
            DocumentBuilder builder = factory.get().newDocumentBuilder();

            Document doc = builder.parse(new InputSource(new StringReader(dump)));;

            Element ele = doc.getDocumentElement();
            if (ele != null && "config".equals(ele.getTagName())) {
                Map<String, Object> map = new LinkedHashMap<>();

                NodeList children = doc.getChildNodes().item(0).getChildNodes();
                for (int i = 0, length = children.getLength(); i < length; i++) {
                    Node item = children.item(i);
                    if (item instanceof Element) {
                        Object value = load((Element) item);
                        if (value != null) map.put(((Element) item).getTagName(), value);
                    }
                }
                setAll(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Object load(Element node) {
        short type = node.getNodeType();
        if (type == 1 || type == 9) {
            NodeList children = node.getChildNodes();
            int length = children.getLength();
            if (node.hasAttribute("type") && "list".equals(node.getAttribute("type"))) {
                List<Object> list = new ArrayList<>();
                for (int i = 0; i < length; i++) {
                    Node item = children.item(i);
                    if (item instanceof Element && "item".equals(((Element) item).getTagName()))
                        list.add(load((Element) item));
                }
                return list;
            }
            if (length == 1 && children.item(0) instanceof Text)
                return ((Text) children.item(0)).getWholeText();
            Map<String, Object> map = new LinkedHashMap<>();
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
        transformer.get().setOutputProperty(
                "{http://xml.apache.org/xslt}indent-amount", Integer.toString(indent));
    }
}
