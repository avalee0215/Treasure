package youseesoft.team27.treasure;

import java.io.Serializable;

public class CartItem implements Serializable {
    private String id;
    private String name;
    private double price;
    private String imageUrl;
    private boolean isChecked;

    public CartItem() {
        // Required empty constructor for Firestore
    }

    public CartItem(String id, String name, double price, String imageUrl, boolean isChecked) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.isChecked = isChecked;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public boolean isChecked() { return isChecked; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPrice(double price) { this.price = price; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setChecked(boolean checked) { isChecked = checked; }
}
