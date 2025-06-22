package youseesoft.team27.treasure;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<CartItem> cartList;
    private Context context;
    private OnCartChangeListener listener;

    public interface OnCartChangeListener {
        void onCartChanged(List<CartItem> updatedList);
    }

    public CartAdapter(List<CartItem> cartList, Context context, OnCartChangeListener listener) {
        this.cartList = cartList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartList.get(position);

        holder.name.setText(item.getName());
        holder.price.setText("$" + String.format("%.2f", item.getPrice()));
        holder.checkBox.setChecked(item.isChecked());

        Glide.with(context)
                .load(item.getImageUrl())
                .placeholder(R.drawable.no_image)
                .error(R.drawable.no_image)
                .into(holder.image);

        // checkbox toggle
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setChecked(isChecked);
            saveToPrefs();
            listener.onCartChanged(cartList);
        });

        // x button toggle
        holder.removeBtn.setOnClickListener(v -> {
            cartList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, cartList.size());
            saveToPrefs();
            listener.onCartChanged(cartList);
        });

        // Open details page on item click
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailsActivity.class);
            intent.putExtra("itemId", item.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return cartList.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, price;
        CheckBox checkBox;
        ImageView removeBtn;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.cart_item_image);
            name = itemView.findViewById(R.id.cart_item_name);
            price = itemView.findViewById(R.id.cart_item_price);
            checkBox = itemView.findViewById(R.id.cart_item_checkbox);
            removeBtn = itemView.findViewById(R.id.cart_item_remove);
        }
    }

    private void saveToPrefs() {
        UserPrefsUtil.saveCartItems(context, cartList);
    }
}

