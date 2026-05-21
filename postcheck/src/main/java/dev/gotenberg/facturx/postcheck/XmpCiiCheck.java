package dev.gotenberg.facturx.postcheck;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.XMPSchema;
import org.apache.xmpbox.type.TextType;
import org.apache.xmpbox.xml.DomXmpParser;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

public final class XmpCiiCheck {

    public static final String NAME = "XMP-CII.Consistency";

    private static final String FX_NS = "urn:factur-x:pdfa:CrossIndustryDocument:invoice:1p0#";
    private static final String EXPECTED_DOCUMENT_TYPE = "INVOICE";
    private static final String EXPECTED_VERSION = "1.0";

    private static final Map<String, String> URN_TO_LEVEL = Map.ofEntries(
            Map.entry("urn:factur-x.eu:1p0:minimum", "MINIMUM"),
            Map.entry("urn:factur-x.eu:1p0:basicwl", "BASIC WL"),
            Map.entry("urn:cen.eu:en16931:2017#compliant#urn:factur-x.eu:1p0:basic", "BASIC"),
            Map.entry("urn:cen.eu:en16931:2017", "EN 16931"),
            Map.entry("urn:cen.eu:en16931:2017#conformant#urn:factur-x.eu:1p0:extended", "EXTENDED")
    );

    public record Result(CheckResult result, Profile profile) {}

    private XmpCiiCheck() {}

    public static Result run(PDDocument doc, PDComplexFileSpecification spec) {
        if (spec == null) {
            return new Result(
                    CheckResult.fail(NAME, FacturxAttachment.FILE_NAME + " not found in EmbeddedFiles"),
                    null);
        }

        String xmpLevel;
        String docType;
        String docFile;
        String version;
        try {
            XMPSchema fx = readFxSchema(doc);
            if (fx == null) {
                return new Result(CheckResult.fail(NAME, "XMP fx: schema missing"), null);
            }
            xmpLevel = property(fx, "ConformanceLevel");
            docType  = property(fx, "DocumentType");
            docFile  = property(fx, "DocumentFileName");
            version  = property(fx, "Version");
        } catch (Exception e) {
            return new Result(CheckResult.fail(NAME, "Cannot parse XMP: " + e.getMessage()), null);
        }

        String urn;
        String ciiLevel;
        try {
            urn = ciiProfileUrn(spec);
            ciiLevel = URN_TO_LEVEL.get(urn);
        } catch (Exception e) {
            return new Result(CheckResult.fail(NAME, "Cannot parse factur-x.xml: " + e.getMessage()), null);
        }

        Profile profile = new Profile(xmpLevel, ciiLevel, urn);

        if (ciiLevel == null) {
            return new Result(CheckResult.fail(NAME, "Unknown CII URN: " + urn), profile);
        }
        if (!EXPECTED_DOCUMENT_TYPE.equals(docType)) {
            return new Result(CheckResult.fail(NAME, "fx:DocumentType=" + docType + ", expected INVOICE"), profile);
        }
        if (!FacturxAttachment.FILE_NAME.equals(docFile)) {
            return new Result(CheckResult.fail(NAME, "fx:DocumentFileName=" + docFile
                    + ", expected " + FacturxAttachment.FILE_NAME), profile);
        }
        if (!EXPECTED_VERSION.equals(version)) {
            return new Result(CheckResult.fail(NAME, "fx:Version=" + version + ", expected " + EXPECTED_VERSION), profile);
        }
        if (!ciiLevel.equals(xmpLevel)) {
            return new Result(CheckResult.fail(NAME,
                    "fx:ConformanceLevel=" + xmpLevel + " != CII profile=" + ciiLevel), profile);
        }
        return new Result(CheckResult.pass(NAME), profile);
    }

    private static XMPSchema readFxSchema(PDDocument doc) throws Exception {
        PDMetadata md = doc.getDocumentCatalog().getMetadata();
        if (md == null) {
            return null;
        }
        try (InputStream in = md.createInputStream()) {
            DomXmpParser parser = new DomXmpParser();
            parser.setStrictParsing(false);
            XMPMetadata meta = parser.parse(in.readAllBytes());
            return meta.getSchema(FX_NS);
        }
    }

    private static String property(XMPSchema fx, String localName) throws Exception {
        TextType t = fx.getUnqualifiedTextProperty(localName);
        return t == null ? null : t.getStringValue();
    }

    private static String ciiProfileUrn(PDComplexFileSpecification spec) throws Exception {
        PDEmbeddedFile ef = spec.getEmbeddedFileUnicode();
        if (ef == null) {
            ef = spec.getEmbeddedFile();
        }
        if (ef == null) {
            throw new IOException("factur-x.xml has no embedded stream");
        }
        byte[] xml = ef.toByteArray();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Document d = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml));

        XPath xp = XPathFactory.newInstance().newXPath();
        xp.setNamespaceContext(new SimpleNs(Map.of(
                "rsm", "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100",
                "ram", "urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100"
        )));
        return xp.evaluate(
                "/rsm:CrossIndustryInvoice/rsm:ExchangedDocumentContext"
                + "/ram:GuidelineSpecifiedDocumentContextParameter/ram:ID", d).trim();
    }

    private static final class SimpleNs implements NamespaceContext {
        private final Map<String, String> map;

        SimpleNs(Map<String, String> map) { this.map = map; }

        @Override public String getNamespaceURI(String prefix) {
            String uri = map.get(prefix);
            return uri == null ? XMLConstants.NULL_NS_URI : uri;
        }
        @Override public String getPrefix(String namespaceURI) { return null; }
        @Override public Iterator<String> getPrefixes(String namespaceURI) { return null; }
    }
}
