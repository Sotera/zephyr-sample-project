package org.zephyr.enricher;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zephyr.data.Entry;
import org.zephyr.data.Record;
import org.zephyr.service.CatalogService;
import org.zephyr.util.LanguageDetector;
import org.zephyr.util.UUIDHelper;

import com.cybozu.labs.langdetect.Language;

public class LanguageEnricher implements Enricher {

    private final static Logger logger = LoggerFactory.getLogger(LanguageEnricher.class);

    private CatalogService catalogService;
    protected final String sourceCategory;
    protected final String destinationCategory;
    protected final String destinationConfidenceCategory;
    private final LanguageDetector detector;

    public LanguageEnricher(String sourceCategory, String destinationLanguageCategory, CatalogService catalogService) {
        this(sourceCategory, destinationLanguageCategory, null, "langProfiles" + File.separator, catalogService);
    }

    public LanguageEnricher(String sourceCategory, String destinationLanguageCategory, String destinationConfidenceCategory, String languageProfilesRoot, CatalogService catalogService) {
        this.sourceCategory = sourceCategory;
        this.destinationCategory = destinationLanguageCategory;
        this.destinationConfidenceCategory = destinationConfidenceCategory;
        this.detector = LanguageDetector.getInstance(languageProfilesRoot);
        this.catalogService = catalogService;
    }

    @Override
    public void enrich(Record record) {
        List<Entry> values = new ArrayList<Entry>();
        for (Entry value : record) {
            if (value.getLabel().equals(sourceCategory)) {
                values.add(value);
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
                    Entry languageValue = catalogService.getEntry(destinationCategory, mostProbableLanguage.lang, Collections.<String>emptyList(), value.getVisibility(), metadataUuid);
                    Entry confidenceValue = null;
                    if (destinationConfidenceCategory != null) {
                        confidenceValue = catalogService.getEntry(destinationConfidenceCategory, String.valueOf(mostProbableLanguage.prob), Collections.<String>emptyList(), value.getVisibility(), metadataUuid);
                    }

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
