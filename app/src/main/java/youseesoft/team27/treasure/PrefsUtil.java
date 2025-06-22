package youseesoft.team27.treasure;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

public class PrefsUtil {
    public static void saveStringSet(Context context, String key, Set<String> set) {
        SharedPreferences prefs = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE);
        prefs.edit().putStringSet(key, set).apply();
    }

    // Save a list of ItemCard as a Set<String>
    public static void saveItemCardList(Context context, String key, List<ItemCard> list) {
        Set<String> set = new HashSet<>();
        for (ItemCard item : list) {
            // Use | as delimiter, escape | in fields if needed
            String serialized = item.getName() + "|" + item.getPrice() + "|" + item.getImageUrl() + "|" + item.getCategory();
            set.add(serialized);
        }
        saveStringSet(context, key, set);
    }

    public static Set<String> loadStringSet(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE);
        return new HashSet<>(prefs.getStringSet(key, new HashSet<>()));
    }

    public static List<ItemCard> getFavoriteItems(Context context, List<ItemCard> allItems) {
        Set<String> favoriteNames = loadStringSet(context, "favorites");
        List<ItemCard> favorites = new ArrayList<>();
        for (ItemCard item : allItems) {
            if (favoriteNames.contains(item.getName())) {
                item.setFavorited(true);
                favorites.add(item);
            }
        }
        return favorites;
    }
}
