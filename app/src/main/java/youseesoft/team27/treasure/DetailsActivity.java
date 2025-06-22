package youseesoft.team27.treasure;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import android.widget.ImageButton;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import android.app.Dialog;
import java.util.HashMap;


public class DetailsActivity extends AppCompatActivity {
    private boolean isInCart = false;
    private String itemId, itemName = "", itemImageUrl = "";
    private double itemPrice = 0.0;
    private static final String[] CONDITION_TEXTS = {
            "May need some repair but still lovable :3",
            "May need a touch up but still good :o",
            "Vintage but good ;)",
            "Very good :D",
            "Like new xD"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        // Setup logout button
        TextView logoutButton = findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> logout());
        }

        TextView nameView = findViewById(R.id.details_name);
        TextView priceView = findViewById(R.id.details_price);
        TextView categoryView = findViewById(R.id.details_category);
        ViewPager2 imagePager = findViewById(R.id.details_image_pager);
        TextView descriptionView = findViewById(R.id.details_description);
        LinearLayout conditionStars = findViewById(R.id.condition_stars);
        TextView conditionText = findViewById(R.id.condition_text);
        RecyclerView suggestedRecyclerView = findViewById(R.id.suggestedRecyclerView);
        suggestedRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        List<ItemCard> suggestedItems = new ArrayList<>();
        ItemCardAdapter suggestedAdapter = new ItemCardAdapter(suggestedItems, this, false);
        suggestedRecyclerView.setAdapter(suggestedAdapter);

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < nav.getMenu().size(); i++) {
            nav.getMenu().getItem(i).setChecked(false);
        }
        nav.getMenu().setGroupCheckable(0, true, true);
        nav.setSelectedItemId(0);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_favorites) {
                startActivity(new Intent(this, FavouritesPage.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_cart) {
                startActivity(new Intent(this, CartActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        itemId = getIntent().getStringExtra("itemId");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("item").document(itemId).get().addOnSuccessListener(document -> {
            if (document.exists()) {
                itemName = document.getString("name");
                nameView.setText(itemName != null ? itemName : "");

                if (document.get("price") instanceof Number) {
                    itemPrice = ((Number) document.get("price")).doubleValue();
                }
                priceView.setText("$" + String.format("%.2f", itemPrice));

                String description = document.getString("description");
                descriptionView.setText(description != null ? description : "");

                Object imagesObj = document.get("images");
                List<String> imageUrls = new ArrayList<>();
                if (imagesObj instanceof List) {
                    List<?> images = (List<?>) imagesObj;
                    for (Object url : images) {
                        if (url != null) imageUrls.add(url.toString());
                    }
                }
                if (!imageUrls.isEmpty()) itemImageUrl = imageUrls.get(0);
                ImagePagerAdapter pagerAdapter = new ImagePagerAdapter(imageUrls, this);
                imagePager.setAdapter(pagerAdapter);

                pagerAdapter.setOnImageClickListener(position -> {
                    Dialog dialog = new Dialog(DetailsActivity.this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                    dialog.setContentView(R.layout.zoom_image_dialog);

                    ViewPager2 zoomPager = dialog.findViewById(R.id.zoom_image_pager);
                    ZoomImagePagerAdapter zoomAdapter = new ZoomImagePagerAdapter(DetailsActivity.this, imageUrls);
                    zoomPager.setAdapter(zoomAdapter);
                    zoomPager.setCurrentItem(position, false);

                    ImageButton zoomLeftButton = dialog.findViewById(R.id.zoom_left_button);
                    ImageButton zoomRightButton = dialog.findViewById(R.id.zoom_right_button);

                    zoomLeftButton.setOnClickListener(v1 -> {
                        int current = zoomPager.getCurrentItem();
                        if (current > 0) {
                            zoomPager.setCurrentItem(current - 1, true);
                        }
                    });

                    zoomRightButton.setOnClickListener(v1 -> {
                        int current = zoomPager.getCurrentItem();
                        if (current < imageUrls.size() - 1) {
                            zoomPager.setCurrentItem(current + 1, true);
                        }
                    });

                    ImageView closeButton = dialog.findViewById(R.id.close_button);
                    closeButton.setOnClickListener(v -> dialog.dismiss());

                    dialog.show();
                });

                TabLayout tabLayout = findViewById(R.id.image_pager_indicator);
                new TabLayoutMediator(tabLayout, imagePager, (tab, position) -> {
                    tab.setCustomView(R.layout.tab_dot);
                }).attach();

                int condition = 3;
                Object condObj = document.get("condition");
                if (condObj instanceof Number) {
                    condition = ((Number) condObj).intValue();
                    if (condition < 1 || condition > 5) condition = 3;
                }
                conditionStars.removeAllViews();
                for (int i = 1; i <= 5; i++) {
                    ImageView star = new ImageView(this);
                    star.setImageResource(i <= condition ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
                    int size = (int) getResources().getDimension(R.dimen.star_size);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                    params.setMarginEnd(4);
                    star.setLayoutParams(params);
                    conditionStars.addView(star);
                }
                conditionText.setText(CONDITION_TEXTS[condition - 1]);
       
                Object categoryRefObj = document.get("category");
                if (categoryRefObj instanceof DocumentReference) {
                    DocumentReference categoryRef = (DocumentReference) categoryRefObj;
                    categoryRef.get().addOnSuccessListener(categorySnapshot -> {
                        categoryView.setText(categorySnapshot.exists() ? categorySnapshot.getString("name") : "");
                    });

                    db.collection("item")
                            .whereEqualTo("category", categoryRef)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                List<ItemCard> allSuggested = new ArrayList<>();
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    if (!doc.getId().equals(itemId)) {
                                        String suggestedName = doc.getString("name");
                                        double suggestedPrice = doc.getDouble("price") != null ? doc.getDouble("price") : 0.0;
                                        String suggestedImageUrl = "";
                                        Object suggestedImagesObj = doc.get("images");
                                        if (suggestedImagesObj instanceof List) {
                                            List<?> images = (List<?>) suggestedImagesObj;
                                            if (!images.isEmpty() && images.get(0) != null) {
                                                suggestedImageUrl = images.get(0).toString();
                                            }
                                        }
                                        ItemCard item = new ItemCard(suggestedName, suggestedPrice, suggestedImageUrl, "", "");
                                        item.setId(doc.getId());
                                        allSuggested.add(item);
                                    }
                                }
                                Collections.shuffle(allSuggested);
                                suggestedItems.clear();
                                for (int i = 0; i < Math.min(2, allSuggested.size()); i++) {
                                    suggestedItems.add(allSuggested.get(i));
                                }
                                suggestedAdapter.notifyDataSetChanged();
                            });
                } else {
                    categoryView.setText(document.getString("category"));
                }

                List<CartItem> cartList = UserPrefsUtil.loadCartItems(this);
                for (CartItem c : cartList) {
                    if (c.getId().equals(itemId)) {
                        isInCart = true;
                        break;
                    }
                }

                Button addToCartButton = findViewById(R.id.add_to_cart_button);
                if (isInCart) {
                    addToCartButton.setText("Remove from Cart");
                    addToCartButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.shadow));
                }

                addToCartButton.setOnClickListener(v -> {
                    List<CartItem> currentCart = UserPrefsUtil.loadCartItems(this);
                    CartItem cartItem = new CartItem(itemId, itemName, itemPrice, itemImageUrl, true);

                    if (!isInCart) {
                        boolean isDuplicate = false;
                        for (CartItem c : currentCart) {
                            if (c.getId().equals(cartItem.getId())) {
                                isDuplicate = true;
                                break;
                            }
                        }
                        if (!isDuplicate) {
                            currentCart.add(cartItem);
                            UserPrefsUtil.saveCartItems(this, currentCart);
                        }

                        addToCartButton.setText("Remove from Cart");
                        addToCartButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.shadow));
                        Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show();
                        isInCart = true;

                    } else {
                        for (int i = 0; i < currentCart.size(); i++) {
                            if (currentCart.get(i).getId().equals(cartItem.getId())) {
                                currentCart.remove(i);
                                break;
                            }
                        }
                        UserPrefsUtil.saveCartItems(this, currentCart);

                        addToCartButton.setText("Add to Cart");
                        addToCartButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.copper));
                        Toast.makeText(this, "Removed from cart", Toast.LENGTH_SHORT).show();
                        isInCart = false;
                    }
                });
            }
        });

        ImageView heartIcon = findViewById(R.id.details_heart);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final boolean[] isFavorited = {false};

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Object favoritesObj = documentSnapshot.get("favorites");
                    if (favoritesObj instanceof Map) {
                        Map<String, Boolean> favorites = (Map<String, Boolean>) favoritesObj;
                        isFavorited[0] = favorites.containsKey(itemId) && favorites.get(itemId);
                    }
                }
                heartIcon.setImageResource(isFavorited[0] ? R.drawable.heartredfilled : R.drawable.heartoutline);
            });

        heartIcon.setOnClickListener(v -> {
            isFavorited[0] = !isFavorited[0];
            
            if (itemId != null && !itemId.isEmpty()) {
                db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Map<String, Object> userData = new HashMap<>();
                        Map<String, Boolean> favorites = new HashMap<>();
                        
                        if (documentSnapshot.exists()) {
                            Object favoritesObj = documentSnapshot.get("favorites");
                            if (favoritesObj instanceof Map) {
                                favorites = (Map<String, Boolean>) favoritesObj;
                            }
                        }

                        if (isFavorited[0]) {
                            favorites.put(itemId, true);
                            heartIcon.setImageResource(R.drawable.heartredfilled);
                            Toast.makeText(this, "Added to favourites", Toast.LENGTH_SHORT).show();
                        } else {
                            favorites.remove(itemId);
                            heartIcon.setImageResource(R.drawable.heartoutline);
                            Toast.makeText(this, "Removed from favourites", Toast.LENGTH_SHORT).show();
                        }

                        userData.put("favorites", favorites);
                        db.collection("users")
                            .document(userId)
                            .set(userData)
                            .addOnSuccessListener(aVoid -> {
                                // Also save to local storage as backup
                                SharedPreferences prefs = UserPrefsUtil.getFavoritesPrefs(this);
                                SharedPreferences.Editor editor = prefs.edit();
                                if (isFavorited[0]) {
                                    editor.putBoolean(itemId, true);
                                } else {
                                    editor.remove(itemId);
                                }
                                editor.apply();
                            });
                    });
            }
        });
    }

    private void logout() {
        try {
            FirebaseAuth.getInstance().signOut();
            // Navigate to login screen
            Intent intent = new Intent(DetailsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e("DetailsActivity", "Error during logout: " + e.getMessage());
            Toast.makeText(this, "Error signing out. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}

