package de.seuhd.campuscoffee.data.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

/**
 * Declarative HTTP client for the OpenStreetMap API.
 */
public interface OsmClient {
    /**
     * Fetches a node by its ID from the OpenStreetMap API.
     *
     * @param nodeId the OSM node ID
     * @return the XML response body
     */
    @GetExchange("/node/{id}")
    String fetchNode(@PathVariable("id") Long nodeId);
}
