<p align="center">
    <img
        src="docs/assets/pocketfiles-mascot.png"
        alt="PocketFiles mascot"
        width="420"
    >
</p>

<h1 align="center">PocketFiles</h1>

<p align="center">
    <strong>Designed around files, not business entities.</strong>
</p>

<p align="center">
    <a href="https://github.com/chyVacheck/PocketFiles/actions/workflows/build.yml">
        <img
            src="https://github.com/chyVacheck/PocketFiles/actions/workflows/build.yml/badge.svg"
            alt="Build"
        >
    </a>
</p>

<p align="center">
    PocketFiles is a reusable file management infrastructure for Java applications.
</p>

---

## Overview

PocketFiles is a modern file management infrastructure that treats files as independent building blocks instead of implementation details of business entities.

Rather than tightly coupling files to invoices, users, products or any other domain model, PocketFiles introduces a reusable architecture where physical files, business usages and storage implementations are separated.

This makes it possible to build applications that remain independent from storage providers while keeping file management centralized, maintainable and reusable.

---

## Why PocketFiles?

In many applications, file management is implemented separately for every business entity.

For example:

- invoices store attachments;
- users store avatars;
- companies store logos;
- products store images.

As a result, every module ends up implementing the same responsibilities:

- uploading files;
- validation;
- metadata management;
- storage paths;
- deletion rules;
- access control;
- lifecycle management;
- deduplication.

Over time, this logic becomes duplicated across the application.

PocketFiles takes a different approach.

Instead of designing file management around business entities, it is designed around files themselves.

```text
Business Entity
       │
       ▼
  File Usage
       │
       ▼
 Physical File
       │
       ▼
    Storage
```

Business applications work with **File Usage** identifiers, while PocketFiles manages physical files and storage independently.

A single physical file can safely exist in multiple business contexts without duplicating its binary content.

> **PocketFiles treats files as first-class infrastructure, not as implementation details of business entities.**

---

## Core Concepts

### Physical File

A physical file represents the actual stored binary object together with its technical metadata.

Typical metadata includes:

- original file name;
- stored file name;
- MIME type;
- checksum;
- file size;
- storage path;
- creation date;
- lifecycle status.

A physical file exists independently from any business entity.

### File Usage

A file usage represents the relationship between a physical file and an external business object.

For example:

- Invoice
- Employee
- Company
- Product
- Email Attachment

Business code works with file usages instead of physical files.

This allows:

- one physical file to be reused multiple times;
- duplicated binary content to be avoided;
- storage implementations to be replaced transparently;
- business entities to remain storage-independent;
- lifecycle management to be centralized.

---

## Design Philosophy

PocketFiles is built around several core principles.

- Files are infrastructure, not business objects.
- One physical file can have multiple usages.
- Storage implementations should be replaceable.
- Business logic should not depend on storage details.
- Metadata belongs to the infrastructure layer.
- Reuse should be preferred over duplication.
- Public APIs should remain explicit and predictable.

---

## Requirements

- Java 21 or later
- Gradle 9 or later when building from source
- Read and write access to the local filesystem

---

## Quick Start

Create a `PocketFilesConfig` and initialize the main facade:

```java
import com.chyvacheck.pocketfiles.PocketFiles;
import com.chyvacheck.pocketfiles.config.DirectoryDepth;
import com.chyvacheck.pocketfiles.config.PocketFilesConfig;

import java.nio.file.Path;

PocketFilesConfig config = PocketFilesConfig.builder()
        .baseDirectory(Path.of("./pocket-files"))
        .directoryDepth(DirectoryDepth.DAY)
        .build();

PocketFiles pocketFiles = PocketFiles.create(config);
```

`DirectoryDepth.DAY` stores physical files using a date-based directory structure:

```text
yyyy/MM/dd/file.ext
```

It is the default directory depth, so the configuration can also be shortened:

```java
PocketFilesConfig config = PocketFilesConfig.builder()
        .baseDirectory(Path.of("./pocket-files"))
        .build();

PocketFiles pocketFiles = PocketFiles.create(config);
```

---

## Saving a File

Save a file using only the required data:

```java
import com.chyvacheck.pocketfiles.service.SaveFileCommand;
import com.chyvacheck.pocketfiles.service.SaveFileResult;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

Path sourcePath = Path.of("./documents/invoice.pdf");

UUID fileUsageUuid;

try (InputStream inputStream = Files.newInputStream(sourcePath)) {
    SaveFileResult result = pocketFiles.save(
            SaveFileCommand.of(
                    inputStream,
                    "invoice.pdf"
            )
    );

    fileUsageUuid = result.fileUsageMetadata().uuid();

    System.out.println(fileUsageUuid);
}
```

PocketFiles stores the physical file and creates a file usage that can be referenced by business code.

---

## Saving Business Usage Metadata

A file can be saved together with metadata describing how it is used by the application:

```java
UUID fileUsageUuid;

try (InputStream inputStream = Files.newInputStream(sourcePath)) {
    SaveFileResult result = pocketFiles.save(
            SaveFileCommand.at(
                    inputStream,
                    "invoice.pdf",
                    "application/pdf",
                    "invoice",
                    "Invoice",
                    "INV-2026-001",
                    "Customer invoice",
                    """
                    {
                      "source": "billing"
                    }
                    """
            )
    );

    fileUsageUuid = result.fileUsageMetadata().uuid();

    System.out.println(fileUsageUuid);
}
```

The available usage fields are:

- `usageType` — describes how the file is used;
- `ownerType` — identifies the type of business object;
- `ownerId` — identifies the concrete business object;
- `displayName` — optional human-readable name;
- `metadataJson` — optional application-specific JSON metadata.

These fields are optional and do not affect the identity of the physical file.

---

## Opening a File

Open the physical file through its file usage identifier:

```java
import java.io.InputStream;
import java.util.UUID;

try (InputStream inputStream = pocketFiles.open(fileUsageUuid).inputStream()) {
    byte[] content = inputStream.readAllBytes();
}
```

Business code does not need to know the physical storage path.

PocketFiles resolves the physical file internally from the supplied file usage identifier.

---

## File Deduplication

PocketFiles identifies reusable physical files using their SHA-256 checksum and file size.

When the same binary content is saved more than once:

- the existing physical file is reused;
- a new file usage is created;
- the binary content is not stored again.

This allows one physical file to be referenced safely from multiple business contexts.

```text
Invoice A ──────┐
                │
Invoice B ──────┼──▶ Physical File
                │
Email ──────────┘
```

Each business context receives its own file usage identifier while sharing the same physical binary object.

---

## File Lifecycle

PocketFiles separates the lifecycle of a file usage from the lifecycle of its physical file.

Manage the lifecycle through the main facade:

```java
pocketFiles.delete(fileUsageUuid);
pocketFiles.restore(fileUsageUuid);

pocketFiles.delete(fileUsageUuid);
pocketFiles.purge(fileUsageUuid);
```

The lifecycle is intentionally explicit:

```text
save
  ↓
ACTIVE Physical File
ACTIVE File Usage

delete last active usage
  ↓
ORPHANED Physical File
DELETED File Usage

restore
  ↓
ACTIVE Physical File
ACTIVE File Usage

purge
  ↓
DELETED Physical File
DELETED File Usage
binary removed from storage
```

### Delete

`delete` marks the referenced file usage as `DELETED`.

When the physical file has no remaining active file usages, it becomes `ORPHANED`.

The binary content remains in storage and can still be restored.

### Restore

`restore` marks a deleted file usage as `ACTIVE`.

When its physical file is `ORPHANED`, the physical file is also returned to the `ACTIVE` state.

A physical file that has already been permanently deleted cannot be restored.

### Purge

`purge` permanently removes the physical file from storage and marks its metadata as `DELETED`.

The referenced file usage must already be marked as `DELETED`, the physical file must be `ORPHANED`, and it must not have any active file usages.

File usage metadata remains available as a historical record after the physical binary has been purged.

---

## Building from Source

Clone the repository:

```bash
git clone https://github.com/chyVacheck/PocketFiles.git
cd PocketFiles
```

Build the project and run all tests:

```bash
./gradlew clean build
```

The generated JAR is available in:

```text
pocket-files-core/build/libs/
```

---

## Project Status

🟢 **Active development**

PocketFiles `0.1.0` provides the first functional version of the core file management infrastructure.

The public API is still evolving and should not yet be considered stable. Breaking changes may occur between `0.x` releases.

---

## Roadmap

- [x] Physical file management
- [x] File usages
- [x] Local storage provider
- [x] SQLite persistence
- [x] SHA-256 file deduplication
- [x] Delete, restore and purge lifecycle
- [ ] Storage abstraction
- [ ] Spring Boot Starter
- [ ] Configurable validation
- [ ] Background cleanup
- [ ] Extended documentation
- [ ] Public package release

---

## License

PocketFiles is licensed under the [MIT License](LICENSE).
