package dev.gotenberg.facturx.postcheck;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public record MustangReport(boolean ok, List<String> errors, String rawOutput) {

    public static MustangReport from(Path xmlFile, int exitCode) throws IOException {
        boolean ok = exitCode == 0;
        if (xmlFile == null || !Files.isRegularFile(xmlFile)) {
            return new MustangReport(ok, List.of(), "");
        }
        byte[] bytes = Files.readAllBytes(xmlFile);
        String raw = new String(bytes, StandardCharsets.UTF_8).trim();
        List<String> errors = parseErrors(bytes);
        return new MustangReport(ok, errors, raw);
    }

    private static List<String> parseErrors(byte[] bytes) {
        List<String> out = new ArrayList<>();
        if (bytes.length == 0) {
            return out;
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
            collect(doc.getDocumentElement(), out);
        } catch (Exception ignored) {
        }
        return out;
    }

    private static void collect(Node node, List<String> out) {
        if (node == null) return;
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element el = (Element) node;
            String name = el.getNodeName().toLowerCase();
            String type = el.getAttribute("type").toLowerCase();
            if (name.equals("error") || name.endsWith(":error")
                    || (name.equals("message") && type.equals("error"))) {
                String text = el.getTextContent();
                if (text != null) {
                    text = text.trim();
                    if (!text.isEmpty()) {
                        out.add(text);
                    }
                }
                return;
            }
        }
        NodeList kids = node.getChildNodes();
        for (int i = 0; i < kids.getLength(); i++) {
            collect(kids.item(i), out);
        }
    }
}
