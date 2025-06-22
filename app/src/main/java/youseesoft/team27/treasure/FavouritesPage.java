package youseesoft.team27.treasure;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.widget.TextView;
import android.view.View;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class FavouritesPage extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ItemCardAdapter adapter;
    private List<ItemCard> favouriteList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourites);

        // Setup logout button
        TextView logoutButton = findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> logout());
        }

        // Setup RecyclerView
        recyclerView = findViewById(R.id.favourites_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // Setup bottom navigation
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setSelectedItemId(R.id.nav_favorites);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_favorites) {
                return true;
            } else if (id == R.id.nav_cart) {
                startActivity(new Intent(this, CartActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        // Load favorites
        loadFavorites();
    }

    private void logout() {
        try {
            FirebaseAuth.getInstance().signOut();
            // Navigate to login screen
            Intent intent = new Intent(FavouritesPage.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e("FavouritesPage", "Error during logout: " + e.getMessage());
            Toast.makeText(this, "Error signing out. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFavorites() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                final Map<String, Boolean> favorites;
                if (documentSnapshot.exists()) {
                    Map<String, Boolean> tempFavorites = (Map<String, Boolean>) documentSnapshot.get("favorites");
                    if (tempFavorites == null) {
                        favorites = new HashMap<>();
                    } else {
                        favorites = tempFavorites;
                    }

                    db.collection("item")
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (favouriteList == null) favouriteList = new ArrayList<>();
                            else favouriteList.clear();
                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                String itemId = document.getId();
                                if (favorites.containsKey(itemId) && favorites.get(itemId)) {
                                    String name = document.getString("name");
                                    double price = document.getDouble("price");
                                    String description = document.getString("description");

                                    // category
                                    String category = "";
                                    DocumentReference categoryRef = document.getDocumentReference("category");
                                    if (categoryRef != null) {
                                        category = categoryRef.getId();
                                    }

                                    // image
                                    String imageUrl = "";
                                    Object imagesObj = document.get("images");
                                    if (imagesObj instanceof List<?>) {
                                        List<?> imagesList = (List<?>) imagesObj;
                                        if (!imagesList.isEmpty()) {
                                            Object firstImage = imagesList.get(0);
                                            if (firstImage instanceof String) {
                                                imageUrl = (String) firstImage;
                                            } else if (firstImage instanceof Map) {
                                                Object urlObj = ((Map<?, ?>) firstImage).get("url");
                                                if (urlObj instanceof String) {
                                                    imageUrl = (String) urlObj;
                                                }
                                            }
                                        }
                                    }

                                    ItemCard item = new ItemCard(name, price, imageUrl, category, itemId);
                                    item.setId(itemId);
                                    item.setDescription(description);
                                    item.setFavorited(true);

                                    favouriteList.add(item);
                                }
                            }
                            if (adapter == null) {
                                adapter = new ItemCardAdapter(favouriteList, this, true);
                                recyclerView.setAdapter(adapter);
                            } else {
                                adapter.notifyDataSetChanged();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("Firestore", "Error loading items", e);
                            Toast.makeText(this, "Failed to load favourites", Toast.LENGTH_SHORT).show();
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e("Firestore", "Error loading user data", e);
                Toast.makeText(this, "Failed to load favourites", Toast.LENGTH_SHORT).show();
            });
    }
}
