package com.example.api.corejavaproject.kafka;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.orc.OrcFile;
import org.apache.orc.Reader;
import org.apache.orc.RecordReader;
import org.apache.orc.TypeDescription;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

/**
 * ORC File Reader - reads data from ORC (Optimized Row Columnar) datalake files.
 * Pure Java SE implementation using Apache ORC library.
 *
 * Usage:
 *   OrcReader reader = new OrcReader("/path/to/data.orc");
 *   while (reader.hasNext()) {
 *       Map<String, Object> row = reader.next();
 *       System.out.println(row);
 *   }
 *   reader.close();
 */
public class OrcReader implements Closeable, Iterator<Map<String, Object>> {
    private final Reader reader;
    private final TypeDescription schema;
    private final List<String> columnNames;
    private final List<TypeDescription> columnTypes;
    private RecordReader recordIterator;
    private Map<String, Object> nextRow;
    private boolean hasNext;
    private long rowsRead;

    /**
     * Open an ORC file for reading
     *
     * @param orcFilePath Path to the ORC file
     */
    public OrcReader(String orcFilePath) throws IOException {
        this(orcFilePath, new Configuration());
    }

    /**
     * Open an ORC file for reading with custom Hadoop Configuration
     *
     * @param orcFilePath Path to the ORC file
     * @param conf Hadoop configuration
     */
    public OrcReader(String orcFilePath, Configuration conf) throws IOException {
        Path path = new Path(orcFilePath);
        this.reader = OrcFile.createReader(path, OrcFile.readerOptions(conf).filesystem(
            org.apache.hadoop.fs.FileSystem.get(conf)));
        this.schema = reader.getSchema();

        // Extract column names and types from schema
        this.columnNames = new ArrayList<>();
        this.columnTypes = new ArrayList<>();

        if (schema != null && schema.getCategory() == TypeDescription.Category.STRUCT) {
            List<String> fieldNames = schema.getFieldNames();
            List<TypeDescription> children = schema.getChildren();
            for (int i = 0; i < fieldNames.size(); i++) {
                columnNames.add(fieldNames.get(i));
                columnTypes.add(children.get(i));
            }
        }

        // Create record reader
        this.recordIterator = reader.rows();
        this.rowsRead = 0;
        loadNextRow();
    }

    private void loadNextRow() {
        try {
            // ORC RecordReader.next() returns boolean - true if there's more data
            // After calling next(), getCurrentValue() gives us the current row
            boolean hasMore = recordIterator.next();

            if (hasMore) {
                Object currentValue = recordIterator.getCurrentValue();

                if (currentValue != null) {
                    nextRow = convertOrcRow(currentValue);
                    hasNext = true;
                    rowsRead++;
                } else {
                    nextRow = null;
                    hasNext = false;
                }
            } else {
                nextRow = null;
                hasNext = false;
            }
        } catch (IOException e) {
            nextRow = null;
            hasNext = false;
            e.printStackTrace();
        }
    }

    /**
     * Convert ORC row to Map<String, Object>
     * ORC returns rows as instances of org.apache.orc.mapred.OrcStruct or similar
     */
    private Map<String, Object> convertOrcRow(Object orcRow) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Use reflection to handle OrcStruct since we can't import it directly
        // OrcStruct has: getFieldValue(int), getNumFields()
        try {
            Class<?> orcStructClass = Class.forName("org.apache.orc.mapred.OrcStruct");
            if (orcStructClass.isInstance(orcRow)) {
                java.lang.reflect.Method getFieldValue = orcStructClass.getMethod("getFieldValue", int.class);
                java.lang.reflect.Method getNumFields = orcStructClass.getMethod("getNumFields");

                int numFields = (Integer) getNumFields.invoke(orcRow);
                for (int i = 0; i < Math.min(numFields, columnNames.size()); i++) {
                    String colName = columnNames.get(i);
                    Object value = getFieldValue.invoke(orcRow, i);
                    TypeDescription colType = (i < columnTypes.size()) ? columnTypes.get(i) : null;
                    result.put(colName, convertValue(value, colType));
                }
                return result;
            }
        } catch (ClassNotFoundException e) {
            // OrcStruct class not found, try Object[] approach
        } catch (Exception e) {
            // Reflection failed, continue to fallback
        }

        // Fallback for Object[] or other row types
        if (orcRow instanceof Object[]) {
            Object[] arr = (Object[]) orcRow;
            for (int i = 0; i < Math.min(arr.length, columnNames.size()); i++) {
                String colName = columnNames.get(i);
                Object value = arr[i];
                TypeDescription colType = (i < columnTypes.size()) ? columnTypes.get(i) : null;
                result.put(colName, convertValue(value, colType));
            }
        } else if (orcRow != null) {
            // Last resort: treat as single column with raw value
            if (columnNames.size() > 0) {
                result.put(columnNames.get(0), convertValue(orcRow, columnTypes.size() > 0 ? columnTypes.get(0) : null));
            } else {
                result.put("_raw", orcRow.toString());
            }
        }

        return result;
    }

    /**
     * Convert ORC value to Java object with proper type handling
     */
    private Object convertValue(Object value, TypeDescription type) {
        if (value == null) {
            return null;
        }

        if (type == null) {
            return value.toString();
        }

        TypeDescription.Category category = type.getCategory();

        switch (category) {
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case STRING:
            case BINARY:
                return value;

            case CHAR:
            case VARCHAR:
            case DECIMAL:
                return value.toString();

            case DATE:
            case TIMESTAMP:
            case TIMESTAMP_INSTANT:
                return value;

            case STRUCT:
            case LIST:
            case MAP:
                return value.toString();

            default:
                return value.toString();
        }
    }

    /**
     * Check if there are more rows to read
     */
    @Override
    public boolean hasNext() {
        return hasNext;
    }

    /**
     * Get the next row as a Map of column name -> value
     */
    @Override
    public Map<String, Object> next() {
        Map<String, Object> current = nextRow;
        loadNextRow();
        return current;
    }

    /**
     * Get the next row without advancing the iterator
     */
    public Map<String, Object> peek() {
        return nextRow;
    }

    /**
     * Get the number of rows read so far
     */
    public long getRowsRead() {
        return rowsRead;
    }

    /**
     * Get the schema of this ORC file
     */
    public TypeDescription getSchema() {
        return schema;
    }

    /**
     * Get the column names
     */
    public List<String> getColumnNames() {
        return columnNames;
    }

    /**
     * Get the column types
     */
    public List<TypeDescription> getColumnTypes() {
        return columnTypes;
    }

    /**
     * Get the total number of rows in the file
     */
    public long getNumberOfRows() {
        return reader.getNumberOfRows();
    }

    /**
     * Close the reader and release resources
     */
    @Override
    public void close() throws IOException {
        if (recordIterator != null) {
            recordIterator.close();
            recordIterator = null;
        }
        if (reader != null) {
            reader.close();
        }
    }

    /**
     * Convert a row to JSON string
     */
    public String toJson(Map<String, Object> row) {
        if (row == null || row.isEmpty()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;

        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;

            sb.append("\"").append(entry.getKey()).append("\":");

            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof Number) {
                sb.append(value);
            } else {
                sb.append("\"").append(escapeJsonString(value.toString())).append("\"");
            }
        }

        sb.append("}");
        return sb.toString();
    }

    private String escapeJsonString(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java OrcReader <path-to-orc-file>");
            System.out.println("Example: java OrcReader /path/to/data.orc");
            return;
        }

        String orcFilePath = args[0];

        System.out.println("=".repeat(60));
        System.out.println("  ORC File Reader");
        System.out.println("=".repeat(60));
        System.out.println("File: " + orcFilePath);
        System.out.println("-".repeat(60));

        try (OrcReader reader = new OrcReader(orcFilePath)) {
            System.out.println("Schema: " + reader.getSchema());
            System.out.println("Columns: " + reader.getColumnNames());
            System.out.println("Total rows: " + reader.getNumberOfRows());
            System.out.println("-".repeat(60));

            int count = 0;
            int maxRows = 10;
            while (reader.hasNext() && count < maxRows) {
                Map<String, Object> row = reader.next();
                System.out.println("Row " + (count + 1) + ": " + reader.toJson(row));
                count++;
            }

            System.out.println("-".repeat(60));
            System.out.println("Rows read: " + reader.getRowsRead());
        }
    }
}