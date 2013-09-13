package org.zephyr.preprocessor;

import java.io.InputStream;
import java.util.Scanner;

public class PanoramioPreprocessor implements Preprocessor {

    private static final String DELIMITER = ",";
    private static final String DOUBLE_QUOTE = "\"";
    private static final String SINGLE_QUOTE = "'";
    private static final char DOUBLE_QUOTE_CHAR = '"';
    private static final char SINGLE_QUOTE_CHAR = '\'';

    public PanoramioPreprocessor() {

    }

    @Override
    public byte[] process(InputStream inputStream) {

        Scanner scanner = new Scanner(inputStream);
        StringBuilder builder = new StringBuilder();
        while (scanner.hasNextLine()) {
            String content = scanner.nextLine();
            String fields[] = content.split(DELIMITER);
            for (String field : fields) {
                builder.append(cleanQuotes(field));
                builder.append(DELIMITER);
            }
            builder.replace(builder.length() - 1, builder.length(), "\n");
        }

        return builder.toString().getBytes();
    }

    private String cleanQuotes(String value) {
        if (value.length() == 0)
            return DOUBLE_QUOTE + DOUBLE_QUOTE;

        StringBuilder newValue = new StringBuilder();
        if (value.startsWith(SINGLE_QUOTE)) {
            newValue.append(value.replaceFirst(SINGLE_QUOTE, ""));
        } else if (value.startsWith(DOUBLE_QUOTE)) {
            newValue.append(value.replaceFirst(DOUBLE_QUOTE, ""));
        } else {
            newValue.append(value);
        }

        if (newValue.length() == 0)
            return DOUBLE_QUOTE + DOUBLE_QUOTE;

        if (newValue.charAt(newValue.length() - 1) == SINGLE_QUOTE_CHAR || newValue.charAt(newValue.length() - 1) == DOUBLE_QUOTE_CHAR) {
            newValue.deleteCharAt(newValue.length() - 1);
        }

        return DOUBLE_QUOTE + newValue.toString().replaceAll(DOUBLE_QUOTE, "\\\"").replaceAll(SINGLE_QUOTE, "\\'") + DOUBLE_QUOTE;
    }

}
