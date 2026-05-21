package dev.gotenberg.facturx.postcheck;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

final class Fixtures {

    private Fixtures() {}

    static final class Builder {
        String fileName = "valid.pdf";
        boolean attach = true;
        String subtype = "text/xml";
        boolean stripSubtype = false;
        String afRelationship = "Data";
        boolean writeAfArray = true;
        String xmpLevel = "BASIC WL";
        String xmpDocumentType = "INVOICE";
        String xmpDocumentFileName = "factur-x.xml";
        String xmpVersion = "1.0";
        boolean writeXmp = true;
        String ciiUrn = "urn:factur-x.eu:1p0:basicwl";
        boolean writeCii = true;

        Builder fileName(String v)            { this.fileName = v; return this; }
        Builder noAttach()                    { this.attach = false; return this; }
        Builder subtype(String v)             { this.subtype = v; return this; }
        Builder stripSubtype()                { this.stripSubtype = true; return this; }
        Builder afRelationship(String v)      { this.afRelationship = v; return this; }
        Builder stripAfRelationship()         { this.afRelationship = null; return this; }
        Builder noAfArray()                   { this.writeAfArray = false; return this; }
        Builder xmpLevel(String v)            { this.xmpLevel = v; return this; }
        Builder xmpDocumentType(String v)     { this.xmpDocumentType = v; return this; }
        Builder xmpDocumentFileName(String v) { this.xmpDocumentFileName = v; return this; }
        Builder xmpVersion(String v)          { this.xmpVersion = v; return this; }
        Builder noXmp()                       { this.writeXmp = false; return this; }
        Builder ciiUrn(String v)              { this.ciiUrn = v; return this; }

        Path build(Path dir) throws IOException {
            Path out = dir.resolve(fileName);
            Files.createDirectories(dir);

            try (PDDocument doc = new PDDocument()) {
                doc.addPage(new PDPage());

                if (writeXmp) {
                    String xmp = """
                        <?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
                        <x:xmpmeta xmlns:x="adobe:ns:meta/">
                          <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                            <rdf:Description rdf:about=""
                              xmlns:fx="urn:factur-x:pdfa:CrossIndustryDocument:invoice:1p0#">
                              <fx:DocumentType>%s</fx:DocumentType>
                              <fx:DocumentFileName>%s</fx:DocumentFileName>
                              <fx:Version>%s</fx:Version>
                              <fx:ConformanceLevel>%s</fx:ConformanceLevel>
                            </rdf:Description>
                          </rdf:RDF>
                        </x:xmpmeta>
                        <?xpacket end="w"?>
                        """.formatted(
                            nullToEmpty(xmpDocumentType),
                            nullToEmpty(xmpDocumentFileName),
                            nullToEmpty(xmpVersion),
                            nullToEmpty(xmpLevel));
                    PDMetadata md = new PDMetadata(doc, new ByteArrayInputStream(xmp.getBytes(StandardCharsets.UTF_8)));
                    doc.getDocumentCatalog().setMetadata(md);
                }

                if (attach) {
                    String cii = writeCii ? """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <rsm:CrossIndustryInvoice
                          xmlns:rsm="urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100"
                          xmlns:ram="urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100">
                          <rsm:ExchangedDocumentContext>
                            <ram:GuidelineSpecifiedDocumentContextParameter>
                              <ram:ID>%s</ram:ID>
                            </ram:GuidelineSpecifiedDocumentContextParameter>
                          </rsm:ExchangedDocumentContext>
                        </rsm:CrossIndustryInvoice>
                        """.formatted(nullToEmpty(ciiUrn)) : "<empty/>";
                    byte[] xmlBytes = cii.getBytes(StandardCharsets.UTF_8);

                    PDEmbeddedFile ef = new PDEmbeddedFile(doc, new ByteArrayInputStream(xmlBytes));
                    if (subtype != null) {
                        ef.setSubtype(subtype);
                    }
                    if (stripSubtype) {
                        ef.getCOSObject().removeItem(COSName.SUBTYPE);
                    }

                    PDComplexFileSpecification spec = new PDComplexFileSpecification();
                    spec.setFile(FacturxAttachment.FILE_NAME);
                    spec.setFileUnicode(FacturxAttachment.FILE_NAME);
                    spec.setEmbeddedFile(ef);
                    spec.setEmbeddedFileUnicode(ef);
                    if (afRelationship != null) {
                        spec.getCOSObject().setName(COSName.getPDFName("AFRelationship"), afRelationship);
                    }

                    PDEmbeddedFilesNameTreeNode tree = new PDEmbeddedFilesNameTreeNode();
                    tree.setNames(Map.of(FacturxAttachment.FILE_NAME, spec));
                    PDDocumentNameDictionary names = new PDDocumentNameDictionary(doc.getDocumentCatalog());
                    names.setEmbeddedFiles(tree);
                    doc.getDocumentCatalog().setNames(names);

                    if (writeAfArray) {
                        COSArray af = new COSArray();
                        af.add(spec.getCOSObject());
                        doc.getDocumentCatalog().getCOSObject().setItem(COSName.getPDFName("AF"), af);
                    }
                }

                doc.save(out.toFile());
            }
            return out;
        }

        private static String nullToEmpty(String s) { return s == null ? "" : s; }
    }

    static Builder valid()              { return new Builder(); }
    static Builder noAttachment()       { return new Builder().fileName("no_attachment.pdf").noAttach(); }
    static Builder wrongSubtype()       { return new Builder().fileName("wrong_subtype.pdf").subtype("application/octet-stream"); }
    static Builder missingSubtype()     { return new Builder().fileName("missing_subtype.pdf").stripSubtype(); }
    static Builder wrongAfRelationship(){ return new Builder().fileName("wrong_afrel.pdf").afRelationship("Unspecified"); }
    static Builder missingAfRelationship(){ return new Builder().fileName("missing_afrel.pdf").stripAfRelationship(); }
    static Builder noAfArray()          { return new Builder().fileName("no_af_array.pdf").noAfArray(); }
    static Builder xmpCiiMismatch()     { return new Builder().fileName("xmp_cii_mismatch.pdf").xmpLevel("EN 16931"); }
    static Builder noXmp()              { return new Builder().fileName("no_xmp.pdf").noXmp(); }
    static Builder unknownUrn()         { return new Builder().fileName("unknown_urn.pdf").ciiUrn("urn:nope"); }
}
