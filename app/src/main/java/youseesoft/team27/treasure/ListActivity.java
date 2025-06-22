package youseesoft.team27.treasure;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ListActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private ItemCardAdapter adapter;
    private List<ItemCard> itemList = new ArrayList<>();
    private String selectedCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new ItemCardAdapter(itemList, this, false);
        recyclerView.setAdapter(adapter);

        TextView categoryHeader = findViewById(R.id.category_header);
        selectedCategory = getIntent().getStringExtra("category");
        if (selectedCategory != null && !selectedCategory.isEmpty()) {
            categoryHeader.setText(selectedCategory);
            categoryHeader.setVisibility(View.VISIBLE);
            fetchItemsForCategory(selectedCategory);
        }

        EditText searchBar = findViewById(R.id.search_bar);
        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            String query = searchBar.getText().toString().trim();
            if (!query.isEmpty()) {
                Intent intent = new Intent(ListActivity.this, SearchActivity.class);
                intent.putExtra("query", query);
                startActivity(intent);
                return true;
            }
            return false;
        });

        // bottom navigation reset
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);

        // reset bottom nav
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
    }

    private void fetchItemsForCategory(String categoryName) {
        String categoryPath = getCategoryDocPath(categoryName);
        Log.d("ListActivity", "categoryName = " + categoryName);
        Log.d("ListActivity", "categoryPath = " + categoryPath);
        Log.d("DEBUG", "selectedCategory: " + selectedCategory);

        db.document(categoryPath).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                db.collection("item")
                        .whereEqualTo("category", snapshot.getReference())
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d("ListActivity", "Firestore query successful");
                                int count = 0;
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Map<String, Object> data = document.getData();
                                    ItemCard item = ItemCard.fromFirestore(data);
                                    item.setId(document.getId());
                                    itemList.add(item);
                                    Log.d("ListActivity", "Loaded item: " + item.getName());
                                    count++;
                                }
                                Log.d("ListActivity", "Total items loaded: " + count);
                                adapter.notifyDataSetChanged();
                            } else {
                                Log.e("ListActivity", "Firestore query failed", task.getException());
                            }
                        });
            } else {
                Log.e("ListActivity", "Category path not found: " + categoryPath);
            }
        });
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

}
