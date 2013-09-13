package org.zephyr.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.zephyr.data.Pair;
import org.zephyr.data.ProcessingResult;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class TwitterParserFactory implements ParserFactory {

    public TwitterParserFactory() {

    }

    @Override
    public Parser newParser(InputStream inputStream) {
        TwitterParser parser = new TwitterParser();
        parser.reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
        return parser;
    }

    private static class TwitterParser extends Parser {

        private static final int TWITTER_ID_LENGTH = 18;
        private List<String> headers;
        private BufferedReader reader;
        private Set<String> languageSet;

        public TwitterParser() {
            this.headers = Lists.newArrayList("id", "timestamp", "user", "userLocation", "latitude", "longitude", "text", "language");
            languageSet = Sets.newHashSet("af", "ar", "bg", "bn", "cs", "da", "de", "el", "en", "es", "et", "fa", "fi", "fr", "gu", "he",
                    "hi", "hr", "hu", "id", "it", "ja", "kn", "ko", "lt", "lv", "mk", "ml", "mr", "ne", "nl", "no",
                    "pa", "pl", "pt", "ro", "ru", "sk", "sl", "so", "sq", "sv", "sv", "sw", "ta", "te", "th", "tl",
                    "tr", "uk", "ur", "vi", "zh-cn", "zh-tw");
        }


        @Override
        public ProcessingResult<List<Pair>, byte[]> parse() throws IOException {
            if (this.reader == null) {
                throw new IOException("This parser has not yet been initialized!");
            }

            // keep reading until the first split string we come to is a twitter id
            ProcessingResult<List<Pair>, byte[]> processingResult;
            boolean doneProcessing = false;
            boolean emptyReader = false;
            List<String> currentValues = new ArrayList<String>();
            StringBuilder rawData = new StringBuilder();
            while (!doneProcessing) {
                reader.mark(1024);
                String line = this.reader.readLine();
                if (line != null) {
                    String values[] = line.split("\t");
                    // apparently calling split on a string that has nothing but a tab results in an array of length 0, so this is to deal with that case
                    if (values.length == 0) {
                        currentValues.add("");
                        rawData.append(line);
                    } else {
                        if (values[0].length() == TWITTER_ID_LENGTH) {
                            try {
                                @SuppressWarnings("unused")
                                Long twitterIdAsLong = Long.parseLong(values[0]);
                                if (currentValues.size() != 0) {
                                    doneProcessing = true;
                                }
                            } catch (NumberFormatException e) {
                                // not done processing
                            }
                        }
                        if (!doneProcessing) {
                            currentValues.addAll(Lists.newArrayList(values));
                            rawData.append(line);
                        }
                    }
                } else {
                    // out of data in this inputstream
                    doneProcessing = true;
                    emptyReader = true;
                }
            }

            if (emptyReader && currentValues.size() == 0) {
                return null;
            }

            if (currentValues.size() < this.headers.size() - 1) {
                processingResult = new ProcessingResult<List<Pair>, byte[]>(("Reader Status: " + emptyReader + " " + currentValues.size() + "|" + rawData.toString()).getBytes(),
                        new ParseException("The number of values returned by the next data row(s) was: " + currentValues.size() +
                                ", which is less than the amount necessary to indicate an entire row."));
            } else {
                List<Pair> pairs = new ArrayList<Pair>();
                for (int i = 0; i < 6; i++) {
                    pairs.add(new Pair(headers.get(i), currentValues.get(i)));
                }
                int lastIndexOfLastTextLine = currentValues.size() - 1;
                if (languageSet.contains(currentValues.get(lastIndexOfLastTextLine))) {
                    //
                    lastIndexOfLastTextLine--;
                }
                StringBuilder textBuilder = new StringBuilder();
                for (int i = 6; i <= lastIndexOfLastTextLine; i++) {
                    textBuilder.append(currentValues.get(i));
                    if (i != lastIndexOfLastTextLine) {
                        textBuilder.append("{CONTROL_CHARACTER}");
                    }
                }
                pairs.add(new Pair(headers.get(6), textBuilder.toString()));
                // TODO: commenting this out for right now to test our new LanguageDetector mechanism
//	            if (currentValues.size() - 1 != lastIndexOfLastTextLine) {
//	                pairs.add(new Pair(headers.get(7), currentValues.get(currentValues.size() - 1)));
//	            }
                processingResult = new ProcessingResult<List<Pair>, byte[]>(pairs);
            }

            reader.reset();
            return processingResult;

        }

    }


}
