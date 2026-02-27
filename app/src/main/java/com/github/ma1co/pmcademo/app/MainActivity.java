package com.github.ma1co.pmcademo.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

// Extending BaseActivity guarantees the physical power button works
public class MainActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Manually building the UI so we can keep BaseActivity protections
        ListView listView = new ListView(this);
        String[] items = {"Start Alpha OS Dashboard (Home Wi-Fi)"};
        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));
        
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    // This will no longer crash, because WifiActivity is in the Manifest
                    startActivity(new Intent(MainActivity.this, WifiActivity.class));
                }
            }
        });
        
        setContentView(listView);
    }
}