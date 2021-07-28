/*
Copyright(c) Dorin Duminica. All rights reserved.
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
  1. Redistributions of source code must retain the above copyright notice,
	 this list of conditions and the following disclaimer.
  2. Redistributions in binary form must reproduce the above copyright notice,
	 this list of conditions and the following disclaimer in the documentation
	 and/or other materials provided with the distribution.
  3. Neither the name of the copyright holder nor the names of its
	 contributors may be used to endorse or promote products derived from this
	 software without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.opensourcesoftware.mobiletouchpad;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity implements DiscoveryThread.DiscoveryThreadListener {

    private static final String TAG = "SettingsActivity";

    private DiscoveryThread mDiscoveryThread = null;
    private DiscoveryThreadItemListAdapter mServerListViewAdapter = null;
    private ListView mServerListView = null;
    private final ArrayList<DiscoveryThread.MEVSystemItem> mServerList = new ArrayList<>();
    private Button btnScrollNatural = null;
    private Button btnScrollMultiplier = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        setTitle(getResources().getString(R.string.title_activity_settings));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        TextView tvAppVersion = findViewById(R.id.tvAppVersion);
        tvAppVersion.setText(String.format(Locale.ENGLISH, "Version: %s.%d", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        mServerListView = findViewById(R.id.lvServerList);
        btnScrollNatural = findViewById(R.id.btnScrollNatural);
        btnScrollNatural.setText(AppPrefs.getScrollNatural() ? R.string.yesno_yes : R.string.yesno_no);
        btnScrollNatural.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppPrefs.setScrollNatural(!AppPrefs.getScrollNatural());
                AppPrefs.savePreferences(SettingsActivity.this);
                btnScrollNatural.setText(AppPrefs.getScrollNatural() ? R.string.yesno_yes : R.string.yesno_no);
            }
        });
        btnScrollMultiplier = findViewById(R.id.btnScrollMultiplier);
        btnScrollMultiplier.setText(AppPrefs.getScrollMultiplierText());
        btnScrollMultiplier.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showScrollMultiplierOptions();
            }
        });

        mServerListViewAdapter = new DiscoveryThreadItemListAdapter(this, mServerList);
        mServerListView.setAdapter(mServerListViewAdapter);

        mServerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                DiscoveryThread.MEVSystemItem item = mServerList.get(position);
                AppPrefs.setHostSystem(item.getName(), item.getHostIP());
                AppPrefs.savePreferences(SettingsActivity.this);

            }
        });

        mDiscoveryThread = new DiscoveryThread(19999, this, this);
        mDiscoveryThread.start();
    }

    private void showScrollMultiplierOptions() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(R.string.settings_scroll_multiplier);
        float[] multipliers = {0.5f, 1.0f, 2.0f, 2.5f, 3.0f};
        String[] items = new String[multipliers.length];
        int checkedItem = 1;
        float scrollMultiplier = AppPrefs.getScrollMultiplier();
        for (int x = 0; x < multipliers.length; x++) {
            items[x] = AppPrefs.formatMultiplier(multipliers[x]);
            if (multipliers[x] == scrollMultiplier) {
                checkedItem = x;
            }
        }
        alertDialog.setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AppPrefs.setScrollMultiplier(multipliers[which]);
                AppPrefs.savePreferences(SettingsActivity.this);
                btnScrollMultiplier.setText(AppPrefs.getScrollMultiplierText());
                dialog.dismiss();
            }
        });
        AlertDialog alert = alertDialog.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (mDiscoveryThread != null) {
            mDiscoveryThread.interrupt();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return true;
    }

    @Override
    public void onSystemFound(DiscoveryThread.MEVSystemItem item) {
        Log.d(TAG, "onSystemFound: ");
        runOnUiThread(() -> {
            boolean exists = false;
            for (DiscoveryThread.MEVSystemItem x : mServerList) {
                exists = (x.getHostIP().equals(item.getHostIP()) && x.getName().equals(item.getName()));
                if (exists) break;
            }
            if (!exists) {
                mServerList.add(item);
                mServerListViewAdapter.notifyDataSetChanged();
            }
        });
    }

    public class DiscoveryThreadItemListAdapter extends BaseAdapter {
        private final ArrayList<DiscoveryThread.MEVSystemItem> mList;
        private final LayoutInflater layoutInflater;

        public DiscoveryThreadItemListAdapter(Context aContext, ArrayList<DiscoveryThread.MEVSystemItem> list) {
            this.mList = list;
            layoutInflater = LayoutInflater.from(aContext);
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View v, ViewGroup vg) {
            ViewHolder holder;
            if (v == null) {
                v = layoutInflater.inflate(R.layout.system_item, null);
                holder = new ViewHolder();
                holder.tvHostIP = (TextView) v.findViewById(R.id.tvHostIP);
                holder.tvName = (TextView) v.findViewById(R.id.tvName);
                v.setTag(holder);
            } else {
                holder = (ViewHolder) v.getTag();
            }
            holder.tvName.setText(mList.get(position).getName());
            holder.tvHostIP.setText(mList.get(position).getHostIP());
            return v;
        }

        class ViewHolder {
            TextView tvName;
            TextView tvHostIP;
        }
    }
}


