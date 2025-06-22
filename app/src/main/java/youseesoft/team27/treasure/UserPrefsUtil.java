package youseesoft.team27.treasure;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserPrefsUtil {
    private static final String PREF_FAVORITES = "favourites_";
    private static final String PREF_CART = "cart_";

    private static String getUserId() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : "guest";
        return userId;
    }

    public static SharedPreferences getFavoritesPrefs(Context context) {
        return context.getSharedPreferences(PREF_FAVORITES + getUserId(), Context.MODE_PRIVATE);
    }

    public static SharedPreferences getCartPrefs(Context context) {
        return context.getSharedPreferences(PREF_CART + getUserId(), Context.MODE_PRIVATE);
    }

    public static void saveCartItems(Context context, List<CartItem> cartItems) {
        // Save to Firestore
        FirestoreUtil.saveCartItems(context, cartItems);
    }

    public static List<CartItem> loadCartItems(Context context) {
        SharedPreferences prefs = getCartPrefs(context);
        String json = prefs.getString("cart_items", null);
        Type type = new TypeToken<List<CartItem>>() {}.getType();
        List<CartItem> list = new Gson().fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    public static void saveFavorites(Context context, Map<String, Boolean> favorites) {
        // Save to Firestore
        FirestoreUtil.saveFavorites(context, favorites);
    }

    public static Map<String, Boolean> loadFavorites(Context context) {
        SharedPreferences prefs = getFavoritesPrefs(context);
        Map<String, Boolean> favorites = new HashMap<>();
        Map<String, ?> all = prefs.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            if (entry.getValue() instanceof Boolean) {
                favorites.put(entry.getKey(), (Boolean) entry.getValue());
            }
        }
        return favorites;
    }

    public static void clearUserData(Context context) {
        getFavoritesPrefs(context).edit().clear().apply();
        getCartPrefs(context).edit().clear().apply();
    }
} 