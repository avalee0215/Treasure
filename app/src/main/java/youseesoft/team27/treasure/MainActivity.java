package youseesoft.team27.treasure;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private RecyclerView pickForYouRecyclerView;
    private PickForYouAdapter picksAdapter;
    private List<ItemCard> picksList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "UserPreferences";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Inflate home_page.xml into content_frame
        LayoutInflater inflater = LayoutInflater.from(this);
        View contentView = inflater.inflate(R.layout.home_page, null);
        FrameLayout contentFrame = findViewById(R.id.content_frame);
        contentFrame.addView(contentView);

        // Setup logout button
        TextView logoutButton = findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> logout());
        }

        // Setup BottomNavigation
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        if (nav != null) {
            nav.setSelectedItemId(R.id.nav_home);

            nav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                Log.d("MainActivity", "nav clicked: " + id);
                if (id == R.id.nav_home) {
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
        }

        // Pick For You RecyclerView
        pickForYouRecyclerView = contentView.findViewById(R.id.pickForYouRecyclerView);
        if (pickForYouRecyclerView != null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            pickForYouRecyclerView.setLayoutManager(layoutManager);

            picksAdapter = new PickForYouAdapter(picksList, this);
            pickForYouRecyclerView.setAdapter(picksAdapter);
        }

        // Category buttons
        ImageButton electronicsButton = contentView.findViewById(R.id.electronicsButton);
        ImageButton clothingButton = contentView.findViewById(R.id.clothingButton);
        ImageButton homeDecorButton = contentView.findViewById(R.id.homeDecorButton);
        ImageButton booksToysButton = contentView.findViewById(R.id.booksToysButton);

        if (electronicsButton != null) {
            electronicsButton.setOnClickListener(v -> openCategory("Electronics and Gadgets"));
        }
        if (clothingButton != null) {
            clothingButton.setOnClickListener(v -> openCategory("Clothing and Accessories"));
        }
        if (homeDecorButton != null) {
            homeDecorButton.setOnClickListener(v -> openCategory("Home and Decor"));
        }
        if (booksToysButton != null) {
            booksToysButton.setOnClickListener(v -> openCategory("Books, Toys and Collectables"));
        }

        // Search bar
        EditText searchBar = contentView.findViewById(R.id.search_bar);
        if (searchBar != null) {
            searchBar.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getAction() == KeyEvent.ACTION_DOWN &&
                                event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {

                    String query = searchBar.getText().toString().trim();
                    if (!query.isEmpty()) {
                        Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                        intent.putExtra("query", query);
                        startActivity(intent);
                        return true;
                    }
                }
                return false;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPicksForYou();
    }

    private void loadPicksForYou() {
        FirestoreUtil.getCategoryViewCounts(counts -> {
            String mostViewedCategory = getMostViewedCategory(counts);
            loadRandomItemsFromCategory(mostViewedCategory);
            if (picksAdapter != null) {
                picksAdapter.notifyDataSetChanged();
            }
        });
    }

    private String getMostViewedCategory(Map<String, Long> counts) {
        String maxCategory = null;
        long maxCount = -1;
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                maxCategory = entry.getKey();
            }
        }
        return maxCategory;
    }

    private void loadRandomItemsFromCategory(String category) {
        picksList.clear();
        int itemCount = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE ? 4 : 2;

        if (category == null || category.isEmpty()) {
            // No most viewed category, get 2 random items from all
            db.collection("item")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        List<ItemCard> allItems = new ArrayList<>();
                        for (var doc : querySnapshot) {
                            ItemCard item = ItemCard.fromFirestore(doc.getData());
                            item.setId(doc.getId());
                            allItems.add(item);
                        }
                        Collections.shuffle(allItems);
                        for (int i = 0; i < Math.min(itemCount, allItems.size()); i++) {
                            picksList.add(allItems.get(i));
                        }
                        picksAdapter.notifyDataSetChanged();
                    });
        } else {
            // Get category document reference
            String categoryPath = getCategoryDocPath(category);
            db.document(categoryPath).get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    db.collection("item")
                            .whereEqualTo("category", snapshot.getReference())
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                List<ItemCard> categoryItems = new ArrayList<>();
                                for (var doc : querySnapshot) {
                                    ItemCard item = ItemCard.fromFirestore(doc.getData());
                                    item.setId(doc.getId());
                                    categoryItems.add(item);
                                }
                                Collections.shuffle(categoryItems);
                                for (int i = 0; i < Math.min(itemCount, categoryItems.size()); i++) {
                                    picksList.add(categoryItems.get(i));
                                }
                                picksAdapter.notifyDataSetChanged();
                            });
                } else {
                    // Fallback: load 2 random items from all
                    db.collection("item")
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                List<ItemCard> allItems = new ArrayList<>();
                                for (var doc : querySnapshot) {
                                    ItemCard item = ItemCard.fromFirestore(doc.getData());
                                    item.setId(doc.getId());
                                    allItems.add(item);
                                }
                                Collections.shuffle(allItems);
                                for (int i = 0; i < Math.min(itemCount, allItems.size()); i++) {
                                    picksList.add(allItems.get(i));
                                }
                                picksAdapter.notifyDataSetChanged();
                            });
                }
            });
        }
    }

    private String getCategoryDocPath(String categoryName) {
        switch (categoryName) {
            case "Electronics and Gadgets":
                return "/category/eH8JoTeqKgHAvo36mF5t";
            case "Clothing and Accessories":
                return "/category/xCRxq5zZ9YoqsmHQs6r5";
            case "Home and Decor":
                return "/category/ET11SrAHmAg4vueCjTW0";
            case "Books, Toys and Collectables":
                return "/category/mHKSyp3L3uJfa3NWfopO";
            default:
                return "";
        }
    }

    private void openCategory(String categoryName) {
        FirestoreUtil.incrementCategoryViewCount(categoryName);
        Intent intent = new Intent(MainActivity.this, ListActivity.class);
        intent.putExtra("category", categoryName);
        startActivity(intent);
    }

    private void logout() {
        try {
            mAuth.signOut();
            // Clear any stored preferences
            prefs.edit().clear().apply();
            // Navigate to login screen
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e("MainActivity", "Error during logout: " + e.getMessage());
            Toast.makeText(this, "Error signing out. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}

