package youseesoft.team27.treasure;

import java.util.Map;
import java.util.Objects;
import java.util.List;

public class ItemCard {
    private String name;
    private double price;
    private String imageUrl;
    private String category;
    private String itemId;
    private String description;
    private String id;
    private boolean favorited = false;
    public ItemCard() {}

    public ItemCard(String name, double price, String imageUrl, String category, String itemId) {
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
        this.itemId = itemId;
    }

    public static ItemCard fromFirestore(Map<String, Object> data) {
        String name = data.get("name") != null ? data.get("name").toString() : "";
        double price = data.get("price") instanceof Number ? ((Number) data.get("price")).doubleValue() : 0.0;

        String imageUrl = "";
        Object imagesObj = data.get("images");
        if (imagesObj instanceof List) {
            List<?> images = (List<?>) imagesObj;
            if (!images.isEmpty() && images.get(0) != null) {
                imageUrl = images.get(0).toString();
            }
        }

        String category = data.get("category") != null ? data.get("category").toString() : "";
        String description = data.get("description") != null ? data.get("description").toString() : "";
        String id = data.get("id") != null ? data.get("id").toString() : "";

        ItemCard item = new ItemCard(name, price, imageUrl, category, "");
        item.setDescription(description);
        item.setId(id);
        return item;
    }

    // Getters and Setters
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public boolean isFavorited() { return favorited; }
    public void setFavorited(boolean favorited) { this.favorited = favorited; }
}
