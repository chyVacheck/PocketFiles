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
    PocketFiles is a reusable file management infrastructure for Java applications.
</p>

---

## Overview

PocketFiles is a modern file management infrastructure that treats files as independent building blocks instead of implementation details of business entities.

Rather than tightly coupling files to invoices, users, products or any other domain model, PocketFiles introduces a reusable architecture where physical files, business usages and storage implementations are completely separated.

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

Over time this logic becomes duplicated across the application.

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

Business applications can work with **File Usage** identifiers, while PocketFiles manages physical files and storage independently.

A single physical file can safely exist in multiple business contexts without duplicating data.

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
- creation date.

A physical file exists independently from any business entity.

---

### File Usage

A file usage represents the relationship between a physical file and an external business object.

For example:

- Invoice
- Employee
- Company
- Product
- Email Attachment

Business code works with usages instead of physical files.

This allows:

- one physical file to be reused multiple times;
- storage providers to be replaced transparently;
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

## Project Status

🟢 **Active development**

PocketFiles is currently under active development.

The public API is still evolving and should not yet be considered stable.

---

## Roadmap

- Physical file management
- File usages
- Storage abstraction
- Local storage provider
- SQLite persistence
- File deduplication
- Spring Boot Starter
- Documentation
- Public release

---

## License

PocketFiles is licensed under the [MIT License](LICENSE).
