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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PickForYouAdapter extends RecyclerView.Adapter<PickForYouAdapter.ItemViewHolder> {

    private List<ItemCard> itemList;
    private Context context;

    public PickForYouAdapter(List<ItemCard> itemList, Context context) {
        this.itemList = itemList;
        this.context = context;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_card, parent, false);

        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.width = (int) (parent.getResources().getDisplayMetrics().density * 200); // 250dp
        view.setLayoutParams(layoutParams);

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

        holder.heartIcon.setOnClickListener(v -> {
            boolean isNowFav = !item.isFavorited();
            item.setFavorited(isNowFav);

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
                        }
                        userData.put("favorites", loadedFavorites);
                        db.collection("users")
                            .document(userId)
                            .set(userData)
                            .addOnSuccessListener(aVoid -> {
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


