# Factur-X / ZUGFeRD Validator

Docker container bundling [Mustang CLI](https://github.com/ZUGFeRD/mustangproject) to validate Factur-X / ZUGFeRD invoices produced by Gotenberg.

## What is validated

Mustang checks:

- **PDF/A-3** via embedded VeraPDF.
- **XMP** metadata: `fx:` namespace, `DocumentType`, `DocumentFileName`, `ConformanceLevel`, `Version`.
- **CII XML**: XSD schema for the detected profile (MINIMUM, BASIC WL, BASIC, EN16931, EXTENDED).
- **Business rules**: official Schematron per profile (BR-xx, BR-CO-xx, BR-DE-xx, ...).
- **Attachment**: presence of `factur-x.xml` inside the PDF.

## What is not covered by Mustang

The following items are not reported as Factur-X errors by Mustang. Items marked `postcheck` are now covered by a second-stage Java validator (PDFBox 3.x) bundled in this image; the others still require an external tool (e.g. the FNFE-MPE validator):

- `Subtype` in the embedded file stream. -- `postcheck`
- `AFRelationship` in the `FileSpec`. -- `postcheck`
- `/AF` array in the Document Catalog. -- `postcheck`
- XMP <-> CII consistency (XMP `ConformanceLevel` vs actual CII profile). -- `postcheck`
- XAdES signature.
- Visual readability of the PDF (MINIMUM / BASIC WL requirement).

The `postcheck` runs after Mustang via `entrypoint.sh`. Its exit code is OR'ed with Mustang's.

## Build

```bash
docker build -t gotenberg/facturx-validator validator/
```

Build arguments:

- `JDK_VERSION` (default: `21.0.2_13-jre`)
- `MUSTANG_VERSION` (default: `2.23.0`)
- `MUSTANG_SHA256` checksum of the JAR

## Usage

```bash
docker run --rm -v "$PWD:/data" gotenberg/facturx-validator /data/invoice.pdf
```

The image takes a single argument: the path to the PDF inside the container. Exit code `0` if the invoice is compliant, non-zero otherwise.

## References

- Mustang Project: https://www.mustangproject.org/
- Factur-X specifications: see `specs/` at the workspace root.
