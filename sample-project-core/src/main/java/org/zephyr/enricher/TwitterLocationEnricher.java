package org.zephyr.enricher;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zephyr.data.Entry;
import org.zephyr.data.Record;
import org.zephyr.schema.validator.DecimalDegreesLatitudeValidator;
import org.zephyr.schema.validator.DecimalDegreesLongitudeValidator;
import org.zephyr.schema.validator.Validator;
import org.zephyr.service.CatalogService;

public class TwitterLocationEnricher implements Enricher {

    private static final Logger logger = LoggerFactory.getLogger(TwitterLocationEnricher.class);

    private final CatalogService catalogService;
    private final Pattern locationPattern;
    private final Validator latitudeValidator;
    private final Validator longitudeValidator;

    public TwitterLocationEnricher(CatalogService catalogService) {
        this.catalogService = catalogService;
        locationPattern = Pattern.compile("([0-9\\.-]+).+?([0-9\\.-]+)");
        latitudeValidator = new DecimalDegreesLatitudeValidator();
        longitudeValidator = new DecimalDegreesLongitudeValidator();
    }

    @Override
    public void enrich(Record record) {
        Entry latEntry = null;
        Entry lonEntry = null;
        Entry userLocationEntry = null;
        for (Entry value : record) {
            if (value.getLabel().equals("latitude")) {
                latEntry = value;
            } else if (value.getLabel().equals("longitude")) {
                lonEntry = value;
            } else if (value.getLabel().equals("user_location")) {
                userLocationEntry = value;
            }
        }
        if ((latEntry == null || lonEntry == null) && (userLocationEntry != null)) {
            // try to pull it out of the user_location CV
            String locationField = userLocationEntry.getValue();
            Matcher matcher = locationPattern.matcher(locationField);
            if (matcher.find()) {
                if (matcher.groupCount() == 2) {
                    String latVal = matcher.group(1);
                    String lonVal = matcher.group(2);
                    if (latitudeValidator.isValid(latVal) && longitudeValidator.isValid(lonVal)) {
                        try {
                            latEntry = catalogService.getEntry("latitude", latVal, Collections.<String>emptyList(), userLocationEntry.getVisibility(), "");
                            lonEntry = catalogService.getEntry("longitude", lonVal, Collections.<String>emptyList(), userLocationEntry.getVisibility(), "");

                            record.add(latEntry);
                            record.add(lonEntry);
                        } catch (RuntimeException e) {
                            logger.error("We could not find the category for one of the provided items", e);
                        }
                    }
                }
            }
        }
    }

}
