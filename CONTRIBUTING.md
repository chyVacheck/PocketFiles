# Contributing to PocketFiles

Thank you for your interest in contributing to PocketFiles.

PocketFiles aims to become a lightweight, reusable file management infrastructure for Java applications.

The project values simplicity, clear architecture, long-term maintainability, and reusable abstractions over framework-specific solutions.

PocketFiles is currently in an early stage of development. The public API and internal architecture may change while the project is being prepared for its first stable release.

## Before contributing

Before starting significant work, please open an issue to discuss the proposed change.

This helps ensure that the contribution is consistent with the goals and architecture of the project.

Small fixes, documentation improvements, and test additions may be submitted directly as pull requests.

## Development requirements

To build PocketFiles locally, you need:

- Java 21
- Git

The project uses the Gradle Wrapper, so a separate Gradle installation is not required.

## Building the project

Clone the repository:

```bash
git clone https://github.com/chyVacheck/PocketFiles.git
cd PocketFiles
```

Build the project and run all tests:

```bash
./gradlew build
```

On Windows:

```powershell
gradlew.bat build
```

## Project structure

```text
PocketFiles/
├── docs/
├── pocket-files-core/
│   └── build.gradle.kts
├── gradle/
├── gradlew
├── gradlew.bat
└── settings.gradle.kts
```

The main library implementation is located in the `pocket-files-core` module.

## Making changes

When contributing:

- Keep changes focused on a single purpose.
- Follow the existing project structure and naming conventions.
- Add or update tests when changing behavior.
- Add comments only where they explain non-obvious decisions.
- Avoid introducing dependencies unless they provide clear value.
- Do not include unrelated formatting or refactoring changes.

## Commit messages

Use short and descriptive commit messages.

Examples:

```text
feat: add local file storage implementation
fix: prevent duplicate metadata records
test: add database schema initialization tests
docs: clarify storage abstraction
refactor: simplify metadata repository
ci: update Gradle build workflow
```

## Pull requests

Before opening a pull request:

1. Make sure the project builds successfully.
2. Run all tests.
3. Review your changes for unrelated modifications.
4. Explain what was changed and why.

Run:

```bash
./gradlew build
```

A pull request should contain:

- A clear title.
- A short description of the problem.
- An explanation of the implemented solution.
- Tests for new or changed behavior when applicable.
- References to related issues when applicable.

## Design principles

Contributions should follow the core PocketFiles principles:

- Files are infrastructure, not business entities.
- Business logic should not depend on storage implementation details.
- Storage implementations should be replaceable.
- Metadata management belongs to the infrastructure layer.
- One physical file may have multiple business usages.
- Reuse should be preferred over duplication.

For substantial architectural changes, please discuss the proposal in an issue before implementation.

## Reporting bugs

When reporting a bug, include:

- PocketFiles version or commit hash.
- Java version.
- Operating system.
- Steps to reproduce the problem.
- Expected behavior.
- Actual behavior.
- Relevant logs or stack traces.

Please remove secrets and private data from logs before publishing them.

## License

By contributing to PocketFiles, you agree that your contributions will be licensed under the [MIT License](LICENSE).