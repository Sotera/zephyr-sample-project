package org.zephyr.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.cybozu.labs.langdetect.Language;

public class LanguageDetector {
    private static final Logger logger = LoggerFactory.getLogger(LanguageDetector.class);

    private LanguageDetector() {

    }

    private synchronized LanguageDetector loadProfile(String path) {
        if (DetectorFactory.getLangList() == null || DetectorFactory.getLangList().size() == 0) {
            // open all files on classpath
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(ClassLoader.getSystemClassLoader().getResourceAsStream(path + "languages.dat")));
                String fileName = null;
                List<String> contents = new ArrayList<String>();
                while ((fileName = br.readLine()) != null) {
                    BufferedReader innerReader = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(path + fileName), Charset.forName("UTF-8")));
                    StringBuilder contentsOfFile = new StringBuilder();
                    String line = null;
                    while ((line = innerReader.readLine()) != null) {
                        contentsOfFile.append(line);
                        contentsOfFile.append("\n");
                    }
                    contents.add(contentsOfFile.toString());
                }
                DetectorFactory.loadProfile(contents);
            } catch (LangDetectException e) {
                logger.error("Exception caught in lang detection: ", e);
            } catch (IOException e) {
                logger.error("Exception caught in opening stream from classpath URL", e);
            }
        }
        return this;
    }

    private static class LanguageDetectorHolder {
        public static final LanguageDetector instance = new LanguageDetector();
    }

    public static LanguageDetector getInstance(String path) {
        return LanguageDetectorHolder.instance.loadProfile(path);
    }

    public Language detectMostLikelyLanguage(String text) {
        try {
            Detector d = DetectorFactory.create();
            d.append(text);
            List<Language> languages = d.getProbabilities();
            if (languages != null && languages.size() > 0) {
                return languages.get(0);
            } else {
                return null;
            }
        } catch (LangDetectException e) {
            //logger.warn("Could not detect language.  Error was: ", e);
            return null;
        }
    }
}

