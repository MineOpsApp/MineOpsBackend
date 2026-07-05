package MineOpsBackend.util;

import org.springframework.http.ResponseEntity;

public class CsvExportUtil {

    private CsvExportUtil() {}

    public static String escape(Object value) {
        if (value == null) return "";
        String s = value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    public static String row(Object... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(values[i]));
        }
        sb.append('\n');
        return sb.toString();
    }

    public static ResponseEntity<String> response(String filename, String csv) {
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=" + filename)
                .body(csv);
    }
}
