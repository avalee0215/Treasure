package youseesoft.team27.treasure;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private TextView resultsText;
    private List<ItemCard> searchResults = new ArrayList<>();
    private ItemCardAdapter searchAdapter;
    private RecyclerView searchRecyclerView;
    private String query;
    private int currentSearchId = 0;
    private LinearLayout noResultsLayout;
    private RecyclerView noResultsRecycler;
    private Button noResultsHomeButton;
    private ItemCardAdapter noResultsAdapter;
    private List<ItemCard> noResultsItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        db = FirebaseFirestore.getInstance();

        EditText searchBar = findViewById(R.id.search_bar);
        if (searchBar != null) {
            searchBar.requestFocus();
        }
        resultsText = findViewById(R.id.resultsText);

        searchRecyclerView = findViewById(R.id.searchRecyclerView);
        searchRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        searchAdapter = new ItemCardAdapter(searchResults, this, false); // Add one more value false for the FavouritesPage
        searchRecyclerView.setAdapter(searchAdapter);

        // bottom navigation reset
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);

        // Reset bottom nav
        nav.getMenu().setGroupCheckable(0, true, false); // temporarily disable check
        for (int i = 0; i < nav.getMenu().size(); i++) {
            nav.getMenu().getItem(i).setChecked(false);
        }
        nav.getMenu().setGroupCheckable(0, true, true); // re-enable check
        nav.setSelectedItemId(0);

        Log.d("NavTest", "After reset, selected item ID = " + nav.getSelectedItemId());

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

        // Set initial query from intent
        query = getIntent().getStringExtra("query");
        if (query != null) {
            searchBar.setText(query);
            updateSearchResults(query);
            resultsText.setText("Loading results for '" + query + "'...");
        }

        // Update results text when user presses Enter/Return
        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            query = searchBar.getText().toString().trim();
            if (!query.isEmpty()) {
                updateSearchResults(query);
                resultsText.setText("Loading results for '" + query + "'...");
                return true;
            }
            return false;
        });

        noResultsLayout = findViewById(R.id.no_results_layout);
        noResultsRecycler = findViewById(R.id.no_results_recycler);
        noResultsHomeButton = findViewById(R.id.no_results_home_button);

        noResultsRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        noResultsAdapter = new ItemCardAdapter(noResultsItems, this, false);
        noResultsRecycler.setAdapter(noResultsAdapter);

        noResultsHomeButton.setOnClickListener(v -> {
            Intent intent = new Intent(SearchActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        // Setup logout button
        TextView logoutButton = findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                try {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(SearchActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    android.widget.Toast.makeText(this, "Error signing out. Please try again.", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateSearchResults(String query) {
        searchResults.clear();
        int searchId = ++currentSearchId;
        String lowerQuery = query.toLowerCase();

        db.collection("item")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (searchId != currentSearchId) return; // Ignore old results
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Map<String,Object> data = document.getData();
                                String name = data.get("name") != null ? data.get("name").toString() : "";
                                if (name.toLowerCase().contains(lowerQuery)) {
                                    dataToItemCard(document.getId(), data);
                                }

                                System.out.println(document.getId() + " => " + document.getData());

                            }
                            System.out.println(searchResults);

                            updateResultsDisplay();
                        } else {
                            System.out.println("Error getting documents: " + task.getException());
                        }
                    }
                });
    }

    private void dataToItemCard(String id, Map<String, Object> data) {
        String name = data.get("name") != null ? data.get("name").toString() : "";
        if (name.isEmpty()) {
            System.out.println("Item name is empty, skipping this item.");
            return;
        }

        double price = 0.0;
        if (data.get("price") instanceof Number) {
            price = ((Number) data.get("price")).doubleValue();
        }

        String imageUrl = "";
        Object imagesObj = data.get("images");
        if (imagesObj instanceof List) {
            List<?> images = (List<?>) imagesObj;
            if (!images.isEmpty() && images.get(0) != null) {
                imageUrl = images.get(0).toString();
            }
        }

        String category = data.get("category") != null ? data.get("category").toString() : "";

        ItemCard itemCard = new ItemCard(name, price, imageUrl, category, id);
        itemCard.setId(id);
        searchResults.add(itemCard);
    }

    private void updateResultsDisplay() {
        if (searchResults.isEmpty()) {
            resultsText.setText("Oops! No treasures found... yet.");
            searchRecyclerView.setVisibility(View.GONE);
            noResultsLayout.setVisibility(View.VISIBLE);
            loadRandomItemsForNoResults();
        } else {
            resultsText.setText(searchResults.size() + " results found for '" + query + "'");
            searchRecyclerView.setVisibility(View.VISIBLE);
            noResultsLayout.setVisibility(View.GONE);
            searchAdapter.notifyDataSetChanged();
        }
    }

    private void loadRandomItemsForNoResults() {
        noResultsItems.clear();
        db.collection("item")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ItemCard> allItems = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Map<String, Object> data = document.getData();
                        String name = data.get("name") != null ? data.get("name").toString() : "";
                        if (name.isEmpty()) continue;
                        double price = 0.0;
                        if (data.get("price") instanceof Number) {
                            price = ((Number) data.get("price")).doubleValue();
                        }
                        String imageUrl = "";
                        Object imagesObj = data.get("images");
                        if (imagesObj instanceof List) {
                            List<?> images = (List<?>) imagesObj;
                            if (!images.isEmpty() && images.get(0) != null) {
                                imageUrl = images.get(0).toString();
                            }
                        }
                        String category = data.get("category") != null ? data.get("category").toString() : "";

                        ItemCard item = new ItemCard(name, price, imageUrl, category, document.getId());
                        item.setId(document.getId());
                        allItems.add(item);
                    }
                    java.util.Collections.shuffle(allItems);
                    for (int i = 0; i < Math.min(2, allItems.size()); i++) {
                        noResultsItems.add(allItems.get(i));
                    }
                    noResultsAdapter.notifyDataSetChanged();
                });
    }
}
