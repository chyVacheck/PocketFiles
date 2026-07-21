package com.chyvacheck.pocketfiles.config;

import java.nio.file.Path;
import java.util.Objects;

public final class PocketFilesConfig {
	/**
	 * Defines the root directory where PocketFiles stores physical files.
	 * Only relative paths are stored in metadata, absolute paths are resolved from
	 * this directory.
	 */
	private final Path baseDirectory;

	/**
	 * Controls how deeply files are grouped by date-based directories.
	 */
	private final DirectoryDepth directoryDepth;

	/**
	 * The constructor.
	 *
	 * @param builder the builder
	 */
	private PocketFilesConfig(Builder builder) {
		this.baseDirectory = Objects.requireNonNull(builder.baseDirectory, "baseDirectory must not be null");
		this.directoryDepth = Objects.requireNonNull(builder.directoryDepth, "directoryDepth must not be null");
	}

	/**
	 * Returns the base directory.
	 *
	 * @return the base directory
	 */
	public Path getBaseDirectory() {
		return baseDirectory;
	}

	/**
	 * Returns the directory depth.
	 *
	 * @return the directory depth
	 */
	public DirectoryDepth getDirectoryDepth() {
		return directoryDepth;
	}

	/**
	 * Returns a builder.
	 *
	 * @return a builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * The builder.
	 */
	public static final class Builder {
		/**
		 * The base directory.
		 */
		private Path baseDirectory;
		/**
		 * DAY is a balanced default for small and medium projects:
		 * yyyy/MM/dd/file.ext
		 */
		private DirectoryDepth directoryDepth = DirectoryDepth.DAY;

		/**
		 * The constructor.
		 */
		private Builder() {
		}

		/**
		 * Sets the base directory.
		 *
		 * @param baseDirectory the base directory
		 * @return this builder
		 */
		public Builder baseDirectory(Path baseDirectory) {
			this.baseDirectory = baseDirectory;
			return this;
		}

		/**
		 * Sets the directory depth.
		 *
		 * @param directoryDepth the directory depth
		 * @return this builder
		 */
		public Builder directoryDepth(DirectoryDepth directoryDepth) {
			this.directoryDepth = directoryDepth;
			return this;
		}

		/**
		 * Builds the configuration.
		 *
		 * @return the configuration
		 */
		public PocketFilesConfig build() {
			return new PocketFilesConfig(this);
		}
	}
}
