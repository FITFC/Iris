package kroppeb.stareval.parser;

import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;

public final class ParserOptions {
	private final Char2ObjectMap<? extends OpResolver<? extends UnaryOp>> unaryOpResolvers;
	private final Char2ObjectMap<? extends OpResolver<? extends BinaryOp>> binaryOpResolvers;
	private final TokenRules tokenRules;


	private ParserOptions(
			Char2ObjectMap<? extends OpResolver<? extends UnaryOp>> unaryOpResolvers,
			Char2ObjectMap<? extends OpResolver<? extends BinaryOp>> binaryOpResolvers,
			TokenRules tokenRules) {
		this.unaryOpResolvers = unaryOpResolvers;
		this.binaryOpResolvers = binaryOpResolvers;
		this.tokenRules = tokenRules;
	}

	TokenRules getParserParts() {
		return this.tokenRules;
	}

	OpResolver<? extends UnaryOp> getUnaryOpResolver(char c) {
		return this.unaryOpResolvers.get(c);
	}

	OpResolver<? extends BinaryOp> getBinaryOpResolver(char c) {
		return this.binaryOpResolvers.get(c);
	}

	public static class Builder {
		private final Char2ObjectMap<Map<String, UnaryOp>> unaryOpResolvers = new Char2ObjectOpenHashMap<>();
		private final Char2ObjectMap<Map<String, BinaryOp>> binaryOpResolvers = new Char2ObjectOpenHashMap<>();
		private TokenRules tokenRules = null;

		public void addUnaryOp(String s, UnaryOp op) {
			Map<String, UnaryOp> mp = this.unaryOpResolvers
					.computeIfAbsent(s.charAt(0), (c) -> new Object2ObjectOpenHashMap<>());
			UnaryOp previous = mp.put(s.substring(1), op);
			assert previous == null;
		}

		public void addBinaryOp(String s, BinaryOp op) {
			Map<String, BinaryOp> mp = this.binaryOpResolvers
					.computeIfAbsent(s.charAt(0), (c) -> new Object2ObjectOpenHashMap<>());
			BinaryOp previous = mp.put(s.substring(1), op);
			assert previous == null;
		}

		public void setParserParts(TokenRules tokenRules) {
			this.tokenRules = tokenRules;
		}

		private static <T> Char2ObjectMap<? extends OpResolver<? extends T>> convertOp(
				Char2ObjectMap<? extends Map<String, T>> ops) {
			Char2ObjectMap<OpResolver<T>> result = new Char2ObjectOpenHashMap<>();

			ops.char2ObjectEntrySet().forEach(entry -> {
						Map<String, T> map = entry.getValue();

						if (map.size() > 2) {
							throw new RuntimeException("Not supported atm");
						}

						if (map.containsKey("")) {
							if (map.size() == 1) {
								result.put(entry.getCharKey(), new SingleCharOpResolver<>(map.get("")));
							} else {
								for (Map.Entry<String, T> subEntry : map.entrySet()) {
									if (!"".equals(subEntry.getKey())) {
										if (subEntry.getKey().length() != 1) {
											throw new RuntimeException("Not supported atm");
										}

										result.put(entry.getCharKey(), new SingleDualCharOpResolver<>(
												map.get(""),
												subEntry.getValue(),
												subEntry.getKey().charAt(0)
										));
									}
								}
							}
						} else {
							for (Map.Entry<String, T> subEntry : map.entrySet()) {
								if (subEntry.getKey().length() != 1) {
									throw new RuntimeException("Not supported atm");
								}

								result.put(entry.getCharKey(), new SingleDualCharOpResolver<>(
										map.get(""),
										subEntry.getValue(),
										subEntry.getKey().charAt(0)
								));
							}
						}
					}

			);
			return result;
		}

		public ParserOptions build() {
			return new ParserOptions(
					convertOp(this.unaryOpResolvers),
					convertOp(this.binaryOpResolvers),
					this.tokenRules == null ? new TokenRules() {} : this.tokenRules);
		}
	}

	/**
	 * Defines a set of rules that allows the tokenizer to identify tokens within a string.
	 */
	public interface TokenRules {
		static boolean isNumber(final char c) {
			return c >= '0' && c <= '9';
		}

		static boolean isLowerCaseLetter(final char c) {
			return c >= 'a' && c <= 'z';
		}

		static boolean isUpperCaseLetter(final char c) {
			return c >= 'A' && c <= 'Z';
		}

		static boolean isLetter(final char c) {
			return isLowerCaseLetter(c) || isUpperCaseLetter(c);
		}

		default boolean isIdStart(final char c) {
			return isLetter(c) || c == '_';
		}

		default boolean isIdPart(final char c) {
			return this.isIdStart(c) || isNumber(c);
		}

		default boolean isNumberStart(final char c) {
			return isNumber(c) || c == '.';
		}

		default boolean isNumberPart(final char c) {
			return this.isNumberStart(c) || isLetter(c);
		}

		default boolean isAccessStart(final char c) {
			return this.isIdStart(c) || isNumber(c);
		}

		default boolean isAccessPart(final char c) {
			return this.isAccessStart(c);
		}
	}
}
