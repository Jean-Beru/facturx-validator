# Factur-X / ZUGFeRD Validator

Docker container bundling [Mustang CLI](https://github.com/ZUGFeRD/mustangproject) to validate Factur-X / ZUGFeRD invoices.

## Coverage

| Feature | Tool |
| --- | --- |
| PDF/A-3 (via embedded VeraPDF) | ✓ Mustang |
| XMP metadata (`fx:` namespace, `DocumentType`, `DocumentFileName`, `ConformanceLevel`, `Version`) | ✓ Mustang |
| CII XML (XSD schema for MINIMUM, BASIC WL, BASIC, EN16931, EXTENDED) | ✓ Mustang |
| Business rules (Schematron: BR-xx, BR-CO-xx, BR-DE-xx, ...) | ✓ Mustang |
| `factur-x.xml` attachment presence | ✓ Mustang |
| `Subtype` in the embedded file stream | ✓ postcheck |
| `AFRelationship` in the `FileSpec` | ✓ postcheck |
| `/AF` array in the Document Catalog | ✓ postcheck |
| XMP <-> CII consistency (XMP `ConformanceLevel` vs actual CII profile) | ✓ postcheck |
| XAdES signature | ✗ FNFE-MPE |
| Visual readability of the PDF (MINIMUM / BASIC WL requirement) | ✗ FNFE-MPE |

✓ covered by this image. ✗ not covered, use the suggested external tool (e.g. the [FNFE-MPE validator](https://services.fnfe-mpe.org/)).

The `postcheck` is a second-stage Java validator (PDFBox 3.x) bundled in this image. It runs after Mustang via `entrypoint.sh` and its exit code is OR'ed with Mustang's.

## Build

```bash
docker build -t jean-beru/facturx-validator .
```

Build arguments:

- `JDK_VERSION` (default: `21.0.2_13-jre`)
- `MUSTANG_VERSION` (default: `2.23.0`)
- `MUSTANG_SHA256` checksum of the JAR

## Usage

```bash
docker run --rm -v "$PWD:/data" jean-beru/facturx-validator /data/invoice.pdf
```

The image takes the path to the PDF inside the container. Exit code `0` if the invoice is compliant, non-zero otherwise.

Pass `--debug` before the path to restore Mustang's full SLF4J log output (silenced to `error` by default):

```bash
docker run --rm -v "$PWD:/data" jean-beru/facturx-validator --debug /data/invoice.pdf
```

Pass `--format=yaml|md|xml` to merge Mustang and postcheck results into a single structured report on stdout (text output otherwise stays as-is):

```bash
docker run --rm -v "$PWD:/data" jean-beru/facturx-validator --format=yaml /data/invoice.pdf
```

## References

- Mustang Project: https://www.mustangproject.org/
