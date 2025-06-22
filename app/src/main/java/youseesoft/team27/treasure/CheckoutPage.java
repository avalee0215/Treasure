package youseesoft.team27.treasure;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class CheckoutPage extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.checkout_page);

        // Setup logout button
        TextView logoutButton = findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> logout());
        }

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
    }

    private void logout() {
        try {
            FirebaseAuth.getInstance().signOut();
            // Navigate to login screen
            Intent intent = new Intent(CheckoutPage.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e("CheckoutPage", "Error during logout: " + e.getMessage());
            Toast.makeText(this, "Error signing out. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}
