package com.chyvacheck.pocketfiles.metadata.transaction;

import java.sql.Connection;

/**
 * Callback used to execute metadata operations inside a single database
 * transaction.
 *
 * <p>
 * The provided {@link Connection} is managed by
 * {@link MetadataTransactionManager}.
 * Implementations should use this connection for all repository calls that must
 * be
 * part of the same transaction.
 *
 * @param <T> result type returned from the transaction
 */
@FunctionalInterface
public interface TransactionCallback<T> {

	/**
	 * Executes database operations using the provided transaction connection.
	 *
	 * @param connection database connection with transaction started
	 * @return transaction result
	 * @throws Exception if the transaction operation fails
	 */
	T execute(Connection connection) throws Exception;
}
