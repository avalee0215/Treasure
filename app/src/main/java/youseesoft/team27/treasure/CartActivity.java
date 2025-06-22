package youseesoft.team27.treasure;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CartActivity extends AppCompatActivity {

    private RecyclerView cartRecyclerView;
    private TextView totalPriceText;
    private Button checkoutButton;
    private CartAdapter adapter;
    private List<CartItem> cartItemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        // Setup logout button
        TextView logoutButton = findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> logout());
        }

        cartRecyclerView = findViewById(R.id.cart_recycler_view);
        totalPriceText = findViewById(R.id.total_price_text);
        checkoutButton = findViewById(R.id.checkout_button);

        cartItemList = loadCartFromPrefs();

        adapter = new CartAdapter(cartItemList, this, updatedList -> updateTotalPrice(updatedList));

        cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartRecyclerView.setAdapter(adapter);

        updateTotalPrice(cartItemList);

        TextView cartHeader = findViewById(R.id.cart_header);
        cartHeader.setVisibility(View.VISIBLE);

        checkoutButton.setOnClickListener(v -> {
            // erase only checked item
            Iterator<CartItem> iterator = cartItemList.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().isChecked()) {
                    iterator.remove();
                }
            }

            // Save updated cart using UserPrefsUtil
            UserPrefsUtil.saveCartItems(this, cartItemList);

            // checkout page
            Intent intent = new Intent(this,CheckoutPage.class);
            startActivity(intent);
            finish();
        });

        // Bottom Navigation
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setSelectedItemId(R.id.nav_cart);
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
                return true;
            }
            return false;
        });
    }

    private void logout() {
        try {
            FirebaseAuth.getInstance().signOut();
            // Navigate to login screen
            Intent intent = new Intent(CartActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e("CartActivity", "Error during logout: " + e.getMessage());
            Toast.makeText(this, "Error signing out. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private List<CartItem> loadCartFromPrefs() {
        return UserPrefsUtil.loadCartItems(this);
    }

    private void updateTotalPrice(List<CartItem> updatedList) {
        double total = 0.0;
        for (CartItem item : updatedList) {
            if (item.isChecked()) {
                total += item.getPrice();
            }
        }
        totalPriceText.setText("Total: $" + String.format("%.2f", total));
    }
}

