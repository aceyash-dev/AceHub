package com.ace.widgets;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {
    private List<AppModel> apps;
    private Context context;

    public AppAdapter(Context context, List<AppModel> apps) {
        this.context = context;
        this.apps = apps;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AppModel app = apps.get(position);
        holder.name.setText(app.getName());
        holder.icon.setImageDrawable(app.getIcon());
        
        holder.itemView.setOnClickListener(v -> {
            [span_5](start_span)// Storing selection locally as per Privacy Policy[span_5](end_span)
            context.getSharedPreferences("AceHubPrefs", Context.MODE_PRIVATE)
                .edit()
                .putString("target_package", app.getPackageName())
                .apply();
            ((PickerActivity)context).finish();
        });
    }

    @Override
    public int getItemCount() { return apps.size(); }

    public void updateList(List<AppModel> newList) {
        this.apps = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView icon;

        public ViewHolder(View itemView) {
            super(itemView);
            [span_6](start_span)// Binding to IDs found in apk[span_6](end_span)
            name = itemView.findViewById(R.id.txt_app_name);
            icon = itemView.findViewById(R.id.img_app_icon);
        }
    }
}
