package com.chyvacheck.pocketfiles.metadata;

public enum PhysicalFileStatus implements CodedEnum {

	/**
	 * The file is pending.
	 */
	PENDING(0),
	/**
	 * The file is active.
	 */
	ACTIVE(1),
	/**
	 * The file is orphaned.
	 */
	ORPHANED(2),
	/**
	 * The file is deleted.
	 */
	DELETED(3),
	/**
	 * The file is missing.
	 */
	MISSING(4),
	/**
	 * The file is failed.
	 */
	FAILED(5);

	/**
	 * The code of the status.
	 */
	private final int code;

	/**
	 * The constructor.
	 * 
	 * @param code The code of the status.
	 */
	PhysicalFileStatus(int code) {
		this.code = code;
	}

	/**
	 * Get the code of the status.
	 * 
	 * @return The code of the status.
	 */
	public int getCode() {
		return this.code;
	}

	public static PhysicalFileStatus fromCode(int code) {
		return CodedEnums.fromCode(
				values(),
				code,
				"Unknown physical file status code");
	}
}
