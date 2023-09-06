package de.jochenr.quarkus.framework;

import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public class ConfigHelper {

	/*
	 * Nur die Properties laden, die ein bestimmtes Präfix haben.
	 */
	public static Properties loadPropertiesWithPrefix(String prefix) {
		Properties result = new Properties();
		Config cfg = ConfigProvider.getConfig();
		Iterable<String> propertyNames = cfg.getPropertyNames();

		Map<String, String> tmpMap = StreamSupport.stream(propertyNames.spliterator(), false)
				.filter(e -> e.startsWith(prefix + "."))
				.collect(Collectors.toMap(
						e -> e.trim(),
						e -> cfg.getValue(e, String.class)));

		result.putAll(tmpMap);

		return result;
	}

	/*
	 * Nur die Properties laden, die ein bestimmtes Präfix haben. Dabei kann (per
	 * zweitem Parameter) der Päfix oder ein Teil des Präfixes abgeschnitten werden.
	 * 
	 * In Anlehnung an
	 * https://github.com/hammock-project/hammock/blob/master/core/src/main/java/ws/ament/hammock/core/config/ConfigLoader.java#L35
	 * 
	 */
	public static Properties loadPropertiesWithPrefixStripped(String prefix, String prefixToStrip) {
		Properties result = new Properties();
		Config cfg = ConfigProvider.getConfig();
		Iterable<String> propertyNames = cfg.getPropertyNames();

		Map<String, String> tmpMap = StreamSupport.stream(propertyNames.spliterator(), false)
				.filter(e -> e.startsWith(prefix + "."))
				.collect(Collectors.toMap(
						new PrefixStripper(prefixToStrip),
						e -> cfg.getValue(e, String.class)));

		result.putAll(tmpMap);

		return result;
	}

	private static class PrefixStripper implements Function<String, String> {
		private String toRemove;
		private Function<String, String> delegate;

		private PrefixStripper(String prefixToStrip) {
			toRemove = prefixToStrip + ".";
			this.delegate = s -> s.replaceFirst(toRemove, "");
		}

		@Override
		public String apply(String s) {
			return delegate.apply(s);
		}
	}

	public static boolean isLocalhost(String hostname) {
		return "localhost".equalsIgnoreCase(hostname) || "127.0.0.1".equals(hostname);
	}

}
