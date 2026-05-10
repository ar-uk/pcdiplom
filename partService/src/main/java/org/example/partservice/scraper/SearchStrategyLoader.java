package org.example.partservice.scraper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads search strategies from a JSON configuration file.
 * Supports both classpath resources and filesystem paths.
 */
public class SearchStrategyLoader {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Load search strategies from classpath resource (default: search-strategies.json)
     */
    public static List<SearchStrategy> loadSearchStrategies() {
        return loadSearchStrategies("search-strategies.json");
    }

    /**
     * Load search strategies from a specific resource path
     */
    public static List<SearchStrategy> loadSearchStrategies(String resourcePath) {
        try {
            // Try classpath first
            InputStream inputStream = SearchStrategyLoader.class.getClassLoader()
                    .getResourceAsStream(resourcePath);

            if (inputStream == null) {
                // Try with leading slash
                inputStream = SearchStrategyLoader.class.getResourceAsStream("/" + resourcePath);
            }

            if (inputStream == null) {
                System.err.println("Warning: Resource file not found: " + resourcePath);
                return new ArrayList<>();
            }

            String jsonContent = new String(inputStream.readAllBytes());
            SearchStrategyConfig config = objectMapper.readValue(jsonContent, SearchStrategyConfig.class);

            System.out.println("Successfully loaded " + config.getStrategies().size() + " search strategies");
            return config.getStrategies();
        } catch (Exception e) {
            System.err.println("Error loading search strategies: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Internal wrapper class for JSON deserialization
     */
    public static class SearchStrategyConfig {
        private List<SearchStrategy> strategies;

        public List<SearchStrategy> getStrategies() {
            return strategies;
        }

        public void setStrategies(List<SearchStrategy> strategies) {
            this.strategies = strategies;
        }
    }
}
