package youseesoft.team27.treasure;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ItemCardAdapter extends RecyclerView.Adapter<ItemCardAdapter.ItemViewHolder> {

    private List<ItemCard> itemList;
    private Context context;
    private boolean isFavouritesPage;

    public ItemCardAdapter(List<ItemCard> itemList, Context context, boolean isFavouritesPage) {
        this.itemList = itemList;
        this.context = context;
        this.isFavouritesPage = isFavouritesPage;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_card, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        ItemCard item = itemList.get(position);
        holder.itemName.setText(item.getName());
        holder.itemPrice.setText("$" + String.format("%.2f", item.getPrice()));

        Glide.with(context)
                .load(item.getImageUrl())
                .placeholder(R.drawable.no_image)
                .error(R.drawable.no_image)
                .into(holder.itemImage);

        // favourites
        String id = item.getId();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                Map<String, Boolean> loadedFavorites = new HashMap<>();
                if (documentSnapshot.exists()) {
                    Object favoritesObj = documentSnapshot.get("favorites");
                    if (favoritesObj instanceof Map) {
                        loadedFavorites = (Map<String, Boolean>) favoritesObj;
                    }
                }
                boolean isFav = id != null && !id.isEmpty() && loadedFavorites.containsKey(id) && loadedFavorites.get(id);
                item.setFavorited(isFav);
                holder.heartIcon.setImageResource(isFav ? R.drawable.heartredfilled : R.drawable.heartoutline);
            });

        // click heart button
        holder.heartIcon.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            ItemCard clickedItem = itemList.get(pos);
            boolean isNowFav = !clickedItem.isFavorited();
            clickedItem.setFavorited(isNowFav);

            if (id != null && !id.isEmpty()) {
                db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Map<String, Object> userData = new HashMap<>();
                        Map<String, Boolean> loadedFavorites = new HashMap<>();
                        if (documentSnapshot.exists()) {
                            Object favoritesObj = documentSnapshot.get("favorites");
                            if (favoritesObj instanceof Map) {
                                loadedFavorites = (Map<String, Boolean>) favoritesObj;
                            }
                        }
                        if (isNowFav) {
                            loadedFavorites.put(id, true);
                            holder.heartIcon.setImageResource(R.drawable.heartredfilled);
                            Toast.makeText(context, "Added to favourites", Toast.LENGTH_SHORT).show();
                        } else {
                            loadedFavorites.remove(id);
                            holder.heartIcon.setImageResource(R.drawable.heartoutline);
                            Toast.makeText(context, "Removed from favourites", Toast.LENGTH_SHORT).show();
                            if (isFavouritesPage) {
                                itemList.remove(pos);
                                notifyItemRemoved(pos);
                            }
                        }
                        userData.put("favorites", loadedFavorites);
                        db.collection("users")
                            .document(userId)
                            .set(userData)
                            .addOnSuccessListener(aVoid -> {
                                // Also save to local storage as backup
                                SharedPreferences prefs = UserPrefsUtil.getFavoritesPrefs(context);
                                SharedPreferences.Editor editor = prefs.edit();
                                if (isNowFav) {
                                    editor.putBoolean(id, true);
                                } else {
                                    editor.remove(id);
                                }
                                editor.apply();
                            });
                    });
            }
        });

        // cart
        holder.cartIcon.setOnClickListener(v -> {
            CartItem cartItem = new CartItem(
                    item.getId(),
                    item.getName(),
                    item.getPrice(),
                    item.getImageUrl(),
                    true // default: checked
            );

            List<CartItem> cartList = UserPrefsUtil.loadCartItems(context);

            // Check repetition
            boolean isDuplicate = false;
            for (CartItem c : cartList) {
                if (c.getName().equals(cartItem.getName())) {
                    isDuplicate = true;
                    break;
                }
            }

            if (!isDuplicate) {
                cartList.add(cartItem);
                UserPrefsUtil.saveCartItems(context, cartList);
                Toast.makeText(context, item.getName() + " added to cart", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Item already in cart", Toast.LENGTH_SHORT).show();
            }
        });

        // move to details page
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailsActivity.class);
            intent.putExtra("itemId", item.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage, heartIcon, cartIcon;
        TextView itemName, itemPrice;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.item_image);
            heartIcon = itemView.findViewById(R.id.heart_icon);
            cartIcon = itemView.findViewById(R.id.cart_icon);
            itemName = itemView.findViewById(R.id.item_name);
            itemPrice = itemView.findViewById(R.id.item_price);
        }
    }
}
