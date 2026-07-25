package MineOpsBackend.util;

import org.springframework.http.ResponseEntity;

public class CsvExportUtil {

    private CsvExportUtil() {}

    public static String escape(Object value) {
        if (value == null) return "";
        String s = value.toString();
        // Formula-injection guard: a cell starting with = + - @ is interpreted as a live formula
        // by Excel/Google Sheets the moment the export is opened. These exports include free-text
        // fields a worker or buyer controls directly (descriptions, names), so a leading quote
        // forces spreadsheet apps to treat the value as literal text instead of executing it.
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
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
