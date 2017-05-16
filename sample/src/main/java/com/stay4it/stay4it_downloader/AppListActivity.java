package com.stay4it.stay4it_downloader;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.gson.reflect.TypeToken;
import com.stay4it.downloader.DownloadManager;
import com.stay4it.downloader.entities.DownloadEntry;
import com.stay4it.downloader.notify.DataWatcher;
import com.stay4it.downloader.utilities.Constants;
import com.stay4it.downloader.utilities.Trace;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class AppListActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {

    private DownloadManager mDownloadManager;
    public ArrayList<AppEntry> applist;
    private DataWatcher watcher = new DataWatcher() {
        @Override
        public void notifyUpdate(DownloadEntry data) {
//            TextView label = (TextView) mDownloadLsv.findViewWithTag(data.id);
//            if (label != null){
//                label.setText(data.status + "\n"
//                        + Formatter.formatShortFileSize(getApplicationContext(), data.currentLength)
//                        + "/" + Formatter.formatShortFileSize(getApplicationContext(), data.totalLength));
//            }
            adapter.notifyDataSetChanged();
            Trace.e(data.toString());
        }
    };
    private ListView mDownloadLsv;
    private DownloadAdapter adapter;
    private RequestQueue mQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDownloadManager = DownloadManager.getInstance(this);
        mQueue = Volley.newRequestQueue(this);
        setContentView(R.layout.activity_list);
        initializeData();

        mDownloadLsv = (ListView) findViewById(R.id.mDownloadLsv);
        adapter = new DownloadAdapter();
        mDownloadLsv.setAdapter(adapter);
        mDownloadLsv.setOnItemClickListener(this);
    }

    private void initializeData() {
        Type type = new TypeToken<ArrayList<AppEntry>>() {
        }.getType();
        String url = "http://api.stay4it.com/v1/public/core/?service=downloader.applist";
        GsonRequest request = new GsonRequest<ArrayList<AppEntry>>(Request.Method.GET, url,
                type, new Response.Listener<ArrayList<AppEntry>>() {

            @Override
            public void onResponse(ArrayList<AppEntry> response) {
                for (AppEntry appEntry : response) {
                    Trace.e(appEntry.toString());
                }
                applist = response;
                adapter.notifyDataSetChanged();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("TAG", error.getMessage(), error);
            }
        });
        mQueue.add(request);
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
        mDownloadManager.addObserver(watcher);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mDownloadManager.removeObserver(watcher);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AppEntry app = applist.get(position);
        Intent intent = new Intent(this, AppDetailActivity.class);
        intent.putExtra(Constants.KEY_APP_ENTRY, app);
        startActivity(intent);
    }

    class DownloadAdapter extends BaseAdapter {

        private ViewHolder holder;

        @Override
        public int getCount() {
            return applist != null ? applist.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            return applist.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null || convertView.getTag() == null) {
                convertView = LayoutInflater.from(AppListActivity.this).inflate(R.layout.activity_applist_item, null);
                holder = new ViewHolder();
                holder.mDownloadBtn = (Button) convertView.findViewById(R.id.mDownloadBtn);
                holder.mDownloadLabel = (TextView) convertView.findViewById(R.id.mDownloadLabel);
                holder.mDownloadStatusLabel = (TextView) convertView.findViewById(R.id.mDownloadStatusLabel);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final AppEntry app = applist.get(position);
            final DownloadEntry entry = mDownloadManager.containsDownloadEntry(app.url) ? mDownloadManager.queryDownloadEntry(app.url) : app.generateDownloadEntry();
            holder.mDownloadLabel.setText(app.name + "  " + app.size + "\n" + app.desc);

            holder.mDownloadStatusLabel.setTag(entry.id);
            holder.mDownloadStatusLabel.setText(entry.status + "\n"
                    + Formatter.formatShortFileSize(getApplicationContext(), entry.currentLength)
                    + "/" + Formatter.formatShortFileSize(getApplicationContext(), entry.totalLength));
            holder.mDownloadBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (entry.status == DownloadEntry.DownloadStatus.idle || entry.status == DownloadEntry.DownloadStatus.cancelled) {
                        mDownloadManager.add(entry);
                    } else if (entry.status == DownloadEntry.DownloadStatus.downloading || entry.status == DownloadEntry.DownloadStatus.waiting) {
                        mDownloadManager.pause(entry);
                    } else if (entry.status == DownloadEntry.DownloadStatus.paused) {
                        mDownloadManager.resume(entry);
                    }
                }
            });
            return convertView;
        }
    }

    static class ViewHolder {
        TextView mDownloadLabel;
        TextView mDownloadStatusLabel;
        Button mDownloadBtn;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement hcy
        if (id == R.id.action_settings) {
            if (item.getTitle().equals("pause all")) {
                item.setTitle(R.string.action_recover_all);
                mDownloadManager.pauseAll();
            } else {
                item.setTitle(R.string.action_pause_all);
                mDownloadManager.recoverAll();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
