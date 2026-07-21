package com.chyvacheck.pocketfiles.metadata;

public final class CodedEnums {
	private CodedEnums() {
	}

	public static <T extends Enum<T> & CodedEnum> T fromCode(
			T[] values,
			int code,
			String errorMessagePrefix) {
		for (T value : values) {
			if (value.getCode() == code) {
				return value;
			}
		}

		throw new IllegalArgumentException(errorMessagePrefix + ": " + code);
	}
}