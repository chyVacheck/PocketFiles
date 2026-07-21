package com.chyvacheck.pocketfiles.metadata;

public enum FileUsageStatus implements CodedEnum {

	/**
	 * The file is active.
	 */
	ACTIVE(1),

	/**
	 * The file is deleted.
	 */
	DELETED(2);

	/**
	 * The code of the status.
	 */
	private final int code;

	/**
	 * The constructor.
	 * 
	 * @param code The code of the status.
	 */
	FileUsageStatus(int code) {
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

	/**
	 * Get the status from the code.
	 * 
	 * @param code The code of the status.
	 * @return The status.
	 */
	public static FileUsageStatus fromCode(int code) {
		return CodedEnums.fromCode(
				values(),
				code,
				"Unknown file usage status code");
	}
}
