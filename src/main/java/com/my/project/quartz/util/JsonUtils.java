package com.my.project.quartz.util;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {

	public static final ObjectMapper mapper = new ObjectMapper();

	static {
		// to prevent exception when encountering unknown property:
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		// JsonParser.Feature for configuring parsing settings:
		// to allow C/C++ style comments in JSON (non-standard, disabled by default)
		// (note: with Jackson 2.5, there is also `mapper.enable(feature)` / `mapper.disable(feature)`)
		mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		// to allow (non-standard) unquoted field names in JSON:
		mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
		mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
		// to allow use of apostrophes (single quotes), non standard
		mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
		mapper.configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true);
		mapper.configure(JsonParser.Feature.ALLOW_MISSING_VALUES, true);
		// to force escaping of non-ASCII characters:
		mapper.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
	}

	public static String toJsonString(Object o) {
		try {
			return mapper.writeValueAsString(o);
		} catch (Exception e) {
			throw new RuntimeException("object to json string error", e);
		}
    }

    public static <T> T jsonToObject(String json, Class<T> clazz) {
		try {
			return mapper.readValue(json, clazz);
		} catch (Exception e) {
			throw new RuntimeException("json string to object error", e);
		}
    }

    public static <K, V> Map<K, V> jsonToMap(String json, Class<K> keyClazz, Class<V> valueClazz) {
    	try {
    		return mapper.readValue(json, new TypeReference<Map<K, V>>() {});
    	} catch (Exception e) {
    		throw new RuntimeException("json string to map error", e);
    	}
    }

    public static <T> List<T> jsonToList(String json, Class<T> clazz) {
    	try {
			return mapper.readValue(json, new TypeReference<List<T>>() {});
		} catch (Exception e) {
			throw new RuntimeException("json string to list error", e);
		}
    }
}
