package youseesoft.team27.treasure;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.firebase.firestore.SetOptions;

public class FirestoreUtil {
    private static final String TAG = "FirestoreUtil";
    private static final String USERS_COLLECTION = "users";
    private static final String FAVORITES_FIELD = "favorites";
    private static final String CART_FIELD = "cart";

    private static String getUserId() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : "guest";
        return userId;
    }

    public static void saveFavorites(Context context, Map<String, Boolean> favorites) {
        String userId = getUserId();
        if (userId.equals("guest")) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                List<CartItem> cartItems = new ArrayList<>();
                if (documentSnapshot.exists() && documentSnapshot.contains(CART_FIELD)) {
                    Object cartObj = documentSnapshot.get(CART_FIELD);
                    if (cartObj instanceof List) {
                        cartItems = (List<CartItem>) cartObj;
                    }
                }
                Map<String, Object> userData = new HashMap<>();
                userData.put(FAVORITES_FIELD, favorites);
                userData.put(CART_FIELD, cartItems);
                db.collection(USERS_COLLECTION)
                    .document(userId)
                    .set(userData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Favorites saved successfully");
                        saveFavoritesToLocal(context, favorites);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error saving favorites", e);
                        saveFavoritesToLocal(context, favorites);
                    });
            });
    }

    public static void saveCartItems(Context context, List<CartItem> cartItems) {
        String userId = getUserId();
        if (userId.equals("guest")) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                Map<String, Boolean> favorites = new HashMap<>();
                if (documentSnapshot.exists() && documentSnapshot.contains(FAVORITES_FIELD)) {
                    Object favoritesObj = documentSnapshot.get(FAVORITES_FIELD);
                    if (favoritesObj instanceof Map) {
                        favorites = (Map<String, Boolean>) favoritesObj;
                    }
                }
                Map<String, Object> userData = new HashMap<>();
                userData.put(CART_FIELD, cartItems);
                userData.put(FAVORITES_FIELD, favorites);
                db.collection(USERS_COLLECTION)
                    .document(userId)
                    .set(userData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Cart items saved successfully");
                        saveCartItemsToLocal(context, cartItems);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error saving cart items", e);
                        saveCartItemsToLocal(context, cartItems);
                    });
            });
    }

    public static void loadUserData(Context context, OnDataLoadedListener listener) {
        String userId = getUserId();
        if (userId.equals("guest")) {
            // Load from local storage for guest users
            Map<String, Boolean> favorites = loadFavoritesFromLocal(context);
            List<CartItem> cartItems = loadCartItemsFromLocal(context);
            listener.onDataLoaded(favorites, cartItems);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(USERS_COLLECTION)
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                Map<String, Boolean> favorites = new HashMap<>();
                List<CartItem> cartItems = new ArrayList<>();

                if (documentSnapshot.exists()) {
                    // Load favorites
                    Object favoritesObj = documentSnapshot.get(FAVORITES_FIELD);
                    if (favoritesObj instanceof Map) {
                        favorites = (Map<String, Boolean>) favoritesObj;
                    }

                    // Load cart items
                    Object cartObj = documentSnapshot.get(CART_FIELD);
                    if (cartObj instanceof List) {
                        cartItems = (List<CartItem>) cartObj;
                    }
                }

                // Save to local storage as backup
                saveFavoritesToLocal(context, favorites);
                saveCartItemsToLocal(context, cartItems);

                listener.onDataLoaded(favorites, cartItems);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading user data", e);
                // Load from local storage as fallback
                Map<String, Boolean> favorites = loadFavoritesFromLocal(context);
                List<CartItem> cartItems = loadCartItemsFromLocal(context);
                listener.onDataLoaded(favorites, cartItems);
            });
    }

    public static void getPicksCounter(OnPicksCounterLoadedListener listener) {
        String userId = getUserId();
        if (userId.equals("guest")) {
            listener.onPicksCounterLoaded(0);
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(USERS_COLLECTION).document(userId).get()
            .addOnSuccessListener(documentSnapshot -> {
                long counter = 0;
                if (documentSnapshot.exists() && documentSnapshot.contains("picksCounter")) {
                    Object value = documentSnapshot.get("picksCounter");
                    if (value instanceof Number) {
                        counter = ((Number) value).longValue();
                    }
                }
                listener.onPicksCounterLoaded(counter);
            })
            .addOnFailureListener(e -> listener.onPicksCounterLoaded(0));
    }

    public static void setPicksCounter(long counter) {
        String userId = getUserId();
        if (userId.equals("guest")) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("picksCounter", counter);
        db.collection(USERS_COLLECTION).document(userId).set(data, SetOptions.merge());
    }

    public static void incrementCategoryViewCount(String category) {
        String userId = getUserId();
        if (userId.equals("guest")) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userDoc = db.collection(USERS_COLLECTION).document(userId);
        userDoc.get().addOnSuccessListener(snapshot -> {
            Map<String, Long> counts = new HashMap<>();
            if (snapshot.exists() && snapshot.contains("categoryViewCounts")) {
                Map<String, Object> map = (Map<String, Object>) snapshot.get("categoryViewCounts");
                for (String key : map.keySet()) {
                    Object val = map.get(key);
                    if (val instanceof Number) {
                        counts.put(key, ((Number) val).longValue());
                    }
                }
            }
            long current = counts.getOrDefault(category, 0L);
            counts.put(category, current + 1);
            Map<String, Object> update = new HashMap<>();
            update.put("categoryViewCounts", counts);
            userDoc.set(update, SetOptions.merge());
        });
    }

    public static void getCategoryViewCounts(OnCategoryViewCountsLoadedListener listener) {
        String userId = getUserId();
        if (userId.equals("guest")) {
            listener.onCategoryViewCountsLoaded(new HashMap<>());
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(USERS_COLLECTION).document(userId).get().addOnSuccessListener(snapshot -> {
            Map<String, Long> counts = new HashMap<>();
            if (snapshot.exists() && snapshot.contains("categoryViewCounts")) {
                Map<String, Object> map = (Map<String, Object>) snapshot.get("categoryViewCounts");
                for (String key : map.keySet()) {
                    Object val = map.get(key);
                    if (val instanceof Number) {
                        counts.put(key, ((Number) val).longValue());
                    }
                }
            }
            listener.onCategoryViewCountsLoaded(counts);
        });
    }

    // Local storage methods
    private static void saveFavoritesToLocal(Context context, Map<String, Boolean> favorites) {
        SharedPreferences prefs = context.getSharedPreferences("favorites_" + getUserId(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        for (Map.Entry<String, Boolean> entry : favorites.entrySet()) {
            editor.putBoolean(entry.getKey(), entry.getValue());
        }
        editor.apply();
    }

    private static Map<String, Boolean> loadFavoritesFromLocal(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("favorites_" + getUserId(), Context.MODE_PRIVATE);
        Map<String, Boolean> favorites = new HashMap<>();
        Map<String, ?> all = prefs.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            if (entry.getValue() instanceof Boolean) {
                favorites.put(entry.getKey(), (Boolean) entry.getValue());
            }
        }
        return favorites;
    }

    private static void saveCartItemsToLocal(Context context, List<CartItem> cartItems) {
        SharedPreferences prefs = context.getSharedPreferences("cart_" + getUserId(), Context.MODE_PRIVATE);
        String json = new Gson().toJson(cartItems);
        prefs.edit().putString("cart_items", json).apply();
    }

    private static List<CartItem> loadCartItemsFromLocal(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("cart_" + getUserId(), Context.MODE_PRIVATE);
        String json = prefs.getString("cart_items", null);
        Type type = new TypeToken<List<CartItem>>() {}.getType();
        List<CartItem> list = new Gson().fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    public interface OnDataLoadedListener {
        void onDataLoaded(Map<String, Boolean> favorites, List<CartItem> cartItems);
    }

    public interface OnPicksCounterLoadedListener {
        void onPicksCounterLoaded(long counter);
    }

    public interface OnCategoryViewCountsLoadedListener {
        void onCategoryViewCountsLoaded(Map<String, Long> counts);
    }
} 