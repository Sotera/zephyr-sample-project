package org.zephyr.service;

import java.util.List;

import org.zephyr.data.Entry;

/**
 * This implementation just passes through the values it is offered (category and types) and creates an Entry out of them.
 */
public class PassThroughCatalogService implements CatalogService {

    public Entry getEntry(String label, String value, List<String> types, String visibility, String metadata) {
        return new Entry(label, value, types, visibility, metadata);
    }

}
