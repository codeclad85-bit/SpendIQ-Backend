package com.expense.expense_tracker.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/product-comparison")
public class ProductComparisonController {

    private static final String SERPER_URL =
            "https://google.serper.dev/shopping";

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;

    public ProductComparisonController(JsonMapper jsonMapper) {
        this.httpClient = HttpClient.newHttpClient();
        this.jsonMapper = jsonMapper;
    }

    // =========================================================
    // SEARCH PRODUCTS USING SERPER
    // =========================================================

    @PostMapping("/search")
    public ResponseEntity<?> searchProducts(
            @RequestBody Map<String, Object> request) {

        try {

            Object queryObject = request.get("query");

            if (queryObject == null
                    || queryObject.toString().trim().isBlank()) {

                return ResponseEntity.badRequest().body(
                        Map.of(
                                "message",
                                "Product search query is required."
                        )
                );
            }

            String query =
                    queryObject.toString().trim();

            String apiKey =
                    System.getenv("SERPER_API_KEY");

            if (apiKey == null || apiKey.isBlank()) {

                return ResponseEntity
                        .internalServerError()
                        .body(
                                Map.of(
                                        "message",
                                        "Serper API key is not configured."
                                )
                        );
            }

            // Request sent to Serper Shopping API
            Map<String, Object> serperBody =
                    new LinkedHashMap<>();

            serperBody.put("q", query);

            // India
            serperBody.put("gl", "in");

            // English
            serperBody.put("hl", "en");

            serperBody.put("num", 10);

            String requestJson =
                    jsonMapper.writeValueAsString(
                            serperBody
                    );

            HttpRequest serperRequest =
                    HttpRequest
                            .newBuilder()
                            .uri(
                                    URI.create(
                                            SERPER_URL
                                    )
                            )
                            .header(
                                    "X-API-KEY",
                                    apiKey
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest
                                            .BodyPublishers
                                            .ofString(
                                                    requestJson
                                            )
                            )
                            .build();

            HttpResponse<String> serperResponse =
                    httpClient.send(
                            serperRequest,
                            HttpResponse
                                    .BodyHandlers
                                    .ofString()
                    );

            System.out.println(
                    "Serper Response Code: "
                            + serperResponse.statusCode()
            );

            if (serperResponse.statusCode() < 200
                    || serperResponse.statusCode() >= 300) {

                System.out.println(
                        "Serper Error: "
                                + serperResponse.body()
                );

                return ResponseEntity
                        .status(502)
                        .body(
                                Map.of(
                                        "message",
                                        "Product search service is temporarily unavailable."
                                )
                        );
            }

            JsonNode root =
                    jsonMapper.readTree(
                            serperResponse.body()
                    );

            JsonNode shopping =
                    root.path("shopping");

            List<Map<String, Object>> products =
                    new ArrayList<>();

            if (shopping.isArray()) {

                for (JsonNode item : shopping) {

                    String title =
                            item.path("title")
                                    .asText("")
                                    .trim();

                    String source =
                            item.path("source")
                                    .asText("")
                                    .trim();

                    String priceText =
                            item.path("price")
                                    .asText("")
                                    .trim();

                    String link =
                            item.path("link")
                                    .asText("")
                                    .trim();

                    String imageUrl =
                            item.path("imageUrl")
                                    .asText("")
                                    .trim();

                    String delivery =
                            item.path("delivery")
                                    .asText("")
                                    .trim();

                    String rating =
                            item.path("rating")
                                    .asText("")
                                    .trim();

                    String ratingCount =
                            item.path("ratingCount")
                                    .asText("")
                                    .trim();

                    double price =
                            extractPrice(priceText);

                    // Ignore unusable results
                    if (title.isBlank()
                            || price <= 0) {
                        continue;
                    }

                    Map<String, Object> product =
                            new LinkedHashMap<>();

                    product.put(
                            "name",
                            title
                    );

                    product.put(
                            "price",
                            round(price)
                    );

                    product.put(
                            "priceText",
                            priceText
                    );

                    product.put(
                            "store",
                            source.isBlank()
                                    ? "Online Store"
                                    : source
                    );

                    product.put(
                            "productUrl",
                            link
                    );

                    product.put(
                            "imageUrl",
                            imageUrl
                    );

                    product.put(
                            "delivery",
                            delivery
                    );

                    product.put(
                            "rating",
                            rating
                    );

                    product.put(
                            "ratingCount",
                            ratingCount
                    );

                    products.add(product);
                }
            }

            // Cheapest first
            products.sort(
                    Comparator.comparingDouble(
                            product ->
                                    ((Number)
                                            product.get("price"))
                                            .doubleValue()
                    )
            );

            // Add ranking
            for (int i = 0;
                 i < products.size();
                 i++) {

                products
                        .get(i)
                        .put(
                                "rank",
                                i + 1
                        );
            }

            Map<String, Object> result =
                    new LinkedHashMap<>();

            result.put(
                    "query",
                    query
            );

            result.put(
                    "totalProducts",
                    products.size()
            );

            result.put(
                    "products",
                    products
            );

            if (!products.isEmpty()) {

                Map<String, Object> cheapest =
                        products.get(0);

                result.put(
                        "lowestPrice",
                        cheapest.get("price")
                );

                result.put(
                        "lowestPriceProduct",
                        cheapest.get("name")
                );

                result.put(
                        "lowestPriceStore",
                        cheapest.get("store")
                );
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {

            System.out.println(
                    "Product Search Error: "
                            + e.getMessage()
            );

            e.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .body(
                            Map.of(
                                    "message",
                                    "Unable to search products."
                            )
                    );
        }
    }

    // =========================================================
    // COMPARE PRODUCTS
    // =========================================================

    @PostMapping("/compare")
    public ResponseEntity<?> compareProducts(
            @RequestBody Map<String, Object> request) {

        try {

            Object productsObject =
                    request.get("products");

            if (!(productsObject
                    instanceof List<?> productsList)
                    || productsList.isEmpty()) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                Map.of(
                                        "message",
                                        "At least one product is required."
                                )
                        );
            }

            List<ProductOption> products =
                    new ArrayList<>();

            for (Object item : productsList) {

                if (!(item instanceof Map<?, ?> map)) {
                    continue;
                }

                Object nameObject =
                        map.get("name");

                Object priceObject =
                        map.get("price");

                if (nameObject == null
                        || priceObject == null) {
                    continue;
                }

                String name =
                        nameObject
                                .toString()
                                .trim();

                double price;

                try {

                    price =
                            Double.parseDouble(
                                    priceObject
                                            .toString()
                            );

                } catch (Exception e) {

                    continue;
                }

                if (name.isBlank()
                        || price <= 0) {
                    continue;
                }

                String store =
                        getValue(
                                map.get("store"),
                                "Online Store"
                        );

                String specifications =
                        getValue(
                                map.get(
                                        "specifications"
                                ),
                                ""
                        );

                String productUrl =
                        getValue(
                                map.get(
                                        "productUrl"
                                ),
                                ""
                        );

                products.add(
                        new ProductOption(
                                name,
                                price,
                                store,
                                specifications,
                                productUrl
                        )
                );
            }

            if (products.isEmpty()) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                Map.of(
                                        "message",
                                        "No valid products were provided."
                                )
                        );
            }

            double lowestPrice =
                    products
                            .stream()
                            .mapToDouble(
                                    product ->
                                            product.price
                            )
                            .min()
                            .orElse(0);

            double highestPrice =
                    products
                            .stream()
                            .mapToDouble(
                                    product ->
                                            product.price
                            )
                            .max()
                            .orElse(0);

            double averagePrice =
                    products
                            .stream()
                            .mapToDouble(
                                    product ->
                                            product.price
                            )
                            .average()
                            .orElse(0);

            // Price based score
            for (ProductOption product : products) {

                double score;

                if (highestPrice == lowestPrice) {

                    score = 100;

                } else {

                    score =
                            100
                            - (
                                (
                                    product.price
                                    - lowestPrice
                                )
                                /
                                (
                                    highestPrice
                                    - lowestPrice
                                )
                              ) * 40;
                }

                product.valueScore =
                        (int) Math.round(score);
            }

            products.sort(
                    Comparator
                            .comparingInt(
                                    (ProductOption product) ->
                                            product.valueScore
                            )
                            .reversed()
            );

            List<Map<String, Object>> comparison =
                    new ArrayList<>();

            for (int i = 0;
                 i < products.size();
                 i++) {

                ProductOption product =
                        products.get(i);

                Map<String, Object> item =
                        new LinkedHashMap<>();

                item.put(
                        "rank",
                        i + 1
                );

                item.put(
                        "name",
                        product.name
                );

                item.put(
                        "store",
                        product.store
                );

                item.put(
                        "price",
                        round(product.price)
                );

                item.put(
                        "specifications",
                        product.specifications
                );

                item.put(
                        "productUrl",
                        product.productUrl
                );

                item.put(
                        "valueScore",
                        product.valueScore
                );

                item.put(
                        "lowestPrice",
                        product.price
                                == lowestPrice
                );

                comparison.add(item);
            }

            ProductOption cheapest =
                    products
                            .stream()
                            .min(
                                    Comparator
                                            .comparingDouble(
                                                    product ->
                                                            product.price
                                            )
                            )
                            .orElse(
                                    products.get(0)
                            );

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put(
                    "totalProducts",
                    products.size()
            );

            response.put(
                    "lowestPrice",
                    round(lowestPrice)
            );

            response.put(
                    "highestPrice",
                    round(highestPrice)
            );

            response.put(
                    "averagePrice",
                    round(averagePrice)
            );

            response.put(
                    "bestPriceProduct",
                    cheapest.name
            );

            response.put(
                    "bestPriceStore",
                    cheapest.store
            );

            response.put(
                    "comparison",
                    comparison
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            System.out.println(
                    "Product Comparison Error: "
                            + e.getMessage()
            );

            return ResponseEntity
                    .internalServerError()
                    .body(
                            Map.of(
                                    "message",
                                    "Unable to compare products."
                            )
                    );
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private String getValue(
            Object value,
            String defaultValue) {

        if (value == null) {
            return defaultValue;
        }

        String text =
                value.toString().trim();

        if (text.isBlank()) {
            return defaultValue;
        }

        return text;
    }

    private double extractPrice(
            String priceText) {

        if (priceText == null
                || priceText.isBlank()) {
            return 0;
        }

        try {

            String cleaned =
                    priceText
                            .replace(",", "")
                            .replaceAll(
                                    "[^0-9.]",
                                    ""
                            );

            if (cleaned.isBlank()) {
                return 0;
            }

            return Double.parseDouble(
                    cleaned
            );

        } catch (Exception e) {

            return 0;
        }
    }

    private double round(double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }

    // =========================================================
    // PRODUCT OPTION
    // =========================================================

    private static class ProductOption {

        String name;
        double price;
        String store;
        String specifications;
        String productUrl;
        int valueScore;

        ProductOption(
                String name,
                double price,
                String store,
                String specifications,
                String productUrl) {

            this.name = name;
            this.price = price;
            this.store = store;
            this.specifications =
                    specifications;
            this.productUrl =
                    productUrl;
        }
    }
}