package com.diosaraiva.archutils.service;

import com.diosaraiva.archutils.util.SimpleJsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts between CSV and JSON representations.
 */
public final class CsvJsonConverter {

    private CsvJsonConverter() { }

    /** Converts CSV text to a JSON array of objects. */
    public static String csvToJson(String csv) {
        String[] lines = csv.split("\\r?\\n");
        if (lines.length < 1) return "[]";

        String[] headers = lines[0].split(",", -1);
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].trim().isEmpty()) continue;
            String[] values = lines[i].split(",", -1);
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < headers.length; j++) {
                row.put(headers[j], j < values.length ? values[j] : "");
            }
            rows.add(row);
        }
        return SimpleJsonParser.toArray(rows);
    }

    /** Converts a JSON array of objects to CSV text. */
    public static String jsonToCsv(String json) {
        List<Map<String, String>> rows = SimpleJsonParser.parseArray(json);
        if (rows.isEmpty()) return "";

        List<String> headers = new ArrayList<>(rows.get(0).keySet());
        StringBuilder sb = new StringBuilder(String.join(",", headers));
        for (Map<String, String> row : rows) {
            sb.append('\n');
            sb.append(headers.stream()
                    .map(h -> escapeCsv(row.getOrDefault(h, "")))
                    .collect(Collectors.joining(",")));
        }
        return sb.toString();
    }

    private static String escapeCsv(String val) {
        return val.contains(",") ? "\"" + val + "\"" : val;
    }
}
