package com.chyvacheck.pocketfiles.metadata;

import com.chyvacheck.pocketfiles.storage.StorageDirectories;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
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

	private static final String ENABLE_FOREIGN_KEYS_SQL = """
			PRAGMA foreign_keys = ON
			""";

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
	 * <p>
	 * Foreign key checks are enabled for every created connection.
	 *
	 * @return opened JDBC connection
	 * @throws SQLException if the connection cannot be opened or configured
	 */
	public Connection createConnection() throws SQLException {
		String url = "jdbc:sqlite:" + this.storageDirectories.getDatabasePath();

		Connection connection = DriverManager.getConnection(url);

		this.enableForeignKeys(connection);

		return connection;
	}

	/**
	 * Enables SQLite foreign key checks for the given connection.
	 *
	 * <p>
	 * SQLite applies this setting per connection, so it must be enabled each time
	 * a new connection is opened.
	 *
	 * @param connection opened JDBC connection
	 * @throws SQLException if the PRAGMA statement cannot be executed
	 */
	private void enableForeignKeys(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(ENABLE_FOREIGN_KEYS_SQL);
		}
	}
}