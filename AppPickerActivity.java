package com.ace.widgets;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class PickerActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AppAdapter adapter;
    private List<AppModel> appList;
    private EditText searchBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picker);

        [span_2](start_span)// UI References from resources[span_2](end_span)
        recyclerView = findViewById(R.id.list);
        searchBar = findViewById(R.id.search);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        appList = new ArrayList<>();
        
        loadInstalledApps();

        [span_3](start_span)// Search logic for txt_app_name[span_3](end_span)
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApps(s.toString());
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadInstalledApps() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : packages) {
            // Filter for non-system apps or relevant targets
            if ((packageInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                appList.add(new AppModel(
                    packageInfo.loadLabel(pm).toString(),
                    packageInfo.packageName,
                    packageInfo.loadIcon(pm)
                ));
            }
        }
        adapter = new AppAdapter(this, appList);
        recyclerView.setAdapter(adapter);
    }

    private void filterApps(String text) {
        List<AppModel> filteredList = new ArrayList<>();
        for (AppModel item : appList) {
            if (item.getName().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(item);
            }
        }
        adapter.updateList(filteredList);
    }
}
