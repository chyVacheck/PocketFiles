package com.chyvacheck.pocketfiles.metadata;

import com.chyvacheck.pocketfiles.storage.StorageDirectories;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Factory responsible for creating JDBC connections to the local SQLite
 * metadata database.
 *
 * <p>
 * The database file is stored inside the PocketFiles base directory as
 * {@code metadata.db}.
 * This class does not create storage directories by itself. The storage
 * directories should be
 * initialized before opening a connection.
 */
public final class DatabaseConnectionFactory {

	private final StorageDirectories storageDirectories;

	/**
	 * Creates a new database connection factory.
	 *
	 * @param storageDirectories storage directory resolver used to locate the
	 *                           metadata database file
	 * @throws NullPointerException if {@code storageDirectories} is null
	 */
	public DatabaseConnectionFactory(StorageDirectories storageDirectories) {
		this.storageDirectories = Objects.requireNonNull(storageDirectories, "storageDirectories must not be null");
	}

	/**
	 * Creates a new JDBC connection to the SQLite metadata database.
	 *
	 * <p>
	 * If the database file does not exist, SQLite will create it automatically.
	 * However, the parent directory must already exist.
	 *
	 * @return opened JDBC connection
	 * @throws SQLException if the connection cannot be opened
	 */
	public Connection createConnection() throws SQLException {
		String url = "jdbc:sqlite:" + this.storageDirectories.getDatabasePath();

		return DriverManager.getConnection(url);
	}
}