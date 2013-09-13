package org.zephyr.enricher;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zephyr.data.Entry;
import org.zephyr.data.Record;
import org.zephyr.service.CatalogService;
import org.zephyr.util.LanguageDetector;
import org.zephyr.util.UUIDHelper;

import com.cybozu.labs.langdetect.Language;

public class LanguageByTypeEnricher implements Enricher {

    private final static Logger logger = LoggerFactory.getLogger(LanguageByTypeEnricher.class);

    private int minLength = 20;
    private Set<String> types;
    private CatalogService catalogService;
    private final LanguageDetector detector;

    private static final String LANGUAGE_DETECT_APPEND = "_language";
    private static final String LANGUAGE_DETECT_CONFIDENCE_APPEND = "_language_confidence";

    public LanguageByTypeEnricher(Set<String> types, CatalogService catalogService) {
        this(types, "langProfiles" + File.separator, catalogService);
    }

    public LanguageByTypeEnricher(Set<String> types, String languageProfilesRoot, CatalogService catalogService) {
        this.types = types;
        this.detector = LanguageDetector.getInstance(languageProfilesRoot);
        this.catalogService = catalogService;
    }

    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }

    @Override
    public void enrich(Record record) {
        List<Entry> values = new ArrayList<Entry>();
        for (Entry value : record) {
            for (String type : value.getTypes()) {
                if (types.contains(type) && (minLength != -1 && value.getValue().length() > minLength)) {
                    values.add(value);
                }
            }
        }
        // do check for all values
        for (Entry value : values) {
            // check this value
            Language mostProbableLanguage = detector.detectMostLikelyLanguage(value.getValue());
            if (mostProbableLanguage != null) {
                String metadataUuid = UUIDHelper.generateUUID();
                try {
                    // put resulting language in new CV
                    Entry languageValue = catalogService.getEntry(value.getLabel() + LANGUAGE_DETECT_APPEND, mostProbableLanguage.lang, Collections.<String>emptyList(), value.getVisibility(), metadataUuid);
                    Entry confidenceValue = catalogService.getEntry(value.getLabel() + LANGUAGE_DETECT_CONFIDENCE_APPEND, String.valueOf(mostProbableLanguage.prob), Collections.<String>emptyList(), value.getVisibility(), metadataUuid);

                    value.setMetadata(value.getMetadata() + "\u0000" + metadataUuid);
                    record.add(languageValue);
                    if (confidenceValue != null)
                        record.add(confidenceValue);
                } catch (RuntimeException e) {
                    // log the error and move on
                    logger.error("We could not find the category for one of the provided items", e);
                }
            }
        }
    }

}
