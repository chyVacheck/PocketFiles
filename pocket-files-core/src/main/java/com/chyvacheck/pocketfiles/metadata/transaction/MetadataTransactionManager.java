package com.chyvacheck.pocketfiles.metadata.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import com.chyvacheck.pocketfiles.metadata.DatabaseConnectionFactory;

public final class MetadataTransactionManager {

	private final DatabaseConnectionFactory databaseConnectionFactory;

	public MetadataTransactionManager(DatabaseConnectionFactory databaseConnectionFactory) {
		this.databaseConnectionFactory = Objects.requireNonNull(
				databaseConnectionFactory,
				"databaseConnectionFactory must not be null");
	}

	/**
	 * Executes the given callback inside a database transaction.
	 *
	 * @param callback callback to execute inside the transaction
	 * @param <T>      result type returned from the callback
	 * @return result of the callback
	 * @throws SQLException if the transaction operation fails
	 */
	public <T> T execute(TransactionCallback<T> callback) throws SQLException {
		Objects.requireNonNull(callback, "callback must not be null");

		try (Connection connection = this.databaseConnectionFactory.createConnection()) {
			boolean previousAutoCommit = connection.getAutoCommit();

			connection.setAutoCommit(false);

			try {
				T result = callback.execute(connection);

				connection.commit();

				return result;
			} catch (Exception exception) {
				connection.rollback();

				if (exception instanceof SQLException sqlException) {
					throw sqlException;
				}

				throw new SQLException("Transaction failed", exception);
			} finally {
				connection.setAutoCommit(previousAutoCommit);
			}
		}
	}
}
