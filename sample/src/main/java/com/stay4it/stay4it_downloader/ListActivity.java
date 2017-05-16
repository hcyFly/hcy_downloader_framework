package com.stay4it.stay4it_downloader;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.stay4it.downloader.notify.DataWatcher;
import com.stay4it.downloader.entities.DownloadEntry;
import com.stay4it.downloader.DownloadManager;
import com.stay4it.downloader.utilities.Trace;

import java.util.ArrayList;

public class ListActivity extends ActionBarActivity {

    private DownloadManager mDownloadManager;
    private ArrayList<DownloadEntry> mDownloadEntries = new ArrayList<>();
    private DataWatcher watcher = new DataWatcher() {
        @Override
        public void notifyUpdate(DownloadEntry data) {
            int index = mDownloadEntries.indexOf(data);
            if (index != -1) {
                mDownloadEntries.remove(index);
                mDownloadEntries.add(index, data);
                adapter.notifyDataSetChanged();
            }
            Trace.e(data.toString());
        }
    };
    private ListView mDownloadLsv;
    private DownloadAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDownloadManager = DownloadManager.getInstance(this);
        setContentView(R.layout.activity_list);
        mDownloadEntries.add(new DownloadEntry("http://shouji.360tpcdn.com/150723/de6fd89a346e304f66535b6d97907563/com.sina.weibo_2057.apk"));
        mDownloadEntries.add(new DownloadEntry("http://shouji.360tpcdn.com/150706/f67f98084d6c788a0f4593f588ea9dfc/com.taobao.taobao_121.apk"));
        mDownloadEntries.add(new DownloadEntry("http://shouji.360tpcdn.com/150720/789cd3f2facef6b27004d9f813599463/com.mfw.roadbook_147.apk"));
        mDownloadEntries.add(new DownloadEntry("http://shouji.360tpcdn.com/150810/10805820b9fbe1eeda52be289c682651/com.qihoo.vpnmaster_3019020.apk"));
        mDownloadEntries.add(new DownloadEntry("http://shouji.360tpcdn.com/150730/580642ffcae5fe8ca311c53bad35bcf2/com.taobao.trip_3001032.apk"));
        mDownloadEntries.add(new DownloadEntry("http://shouji.360tpcdn.com/150807/42ac3ad85a189125701e69ccff36ad7a/com.eg.android.AlipayGphone_78.apk"));
        mDownloadEntries.add(new DownloadEntry("http://shouji.360tpcdn.com/150813/9e775b5afb66feb960941cd8879af0b8/com.sankuai.meituan_291.apk"));
        mDownloadEntries.add(new DownloadEntry("http://shouji.360tpcdn.com/150706/5a9bec48b764a892df801424278a4285/com.mt.mtxx.mtxx_434.apk"));
        mDownloadEntries.add(new DownloadEntry("http://shouji.360tpcdn.com/150707/2ef5e16e0b8b3135aa714ad9b56b9a3d/com.happyelements.AndroidAnimal_25.apk"));
        mDownloadEntries.add(new DownloadEntry("http://shouji.360tpcdn.com/150716/aea8ca0e6617b0989d3dcce0bb9877d5/com.cmge.xianjian.a360_30.apk"));
        DownloadEntry entry = null;
        DownloadEntry realEntry = null;
        for (int i = 0; i < mDownloadEntries.size(); i++) {
            entry = mDownloadEntries.get(i);
            realEntry = mDownloadManager.queryDownloadEntry(entry.id);
            if (realEntry != null) {
                mDownloadEntries.remove(i);
                mDownloadEntries.add(i, realEntry);
            }
        }
        mDownloadLsv = (ListView) findViewById(R.id.mDownloadLsv);
        adapter = new DownloadAdapter();
        mDownloadLsv.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDownloadManager.addObserver(watcher);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mDownloadManager.removeObserver(watcher);
    }

    class DownloadAdapter extends BaseAdapter {

        private ViewHolder holder;

        @Override
        public int getCount() {
            return mDownloadEntries != null ? mDownloadEntries.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            return mDownloadEntries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null || convertView.getTag() == null) {
                convertView = LayoutInflater.from(ListActivity.this).inflate(R.layout.activity_list_item, null);
                holder = new ViewHolder();
                holder.mDownloadBtn = (Button) convertView.findViewById(R.id.mDownloadBtn);
                holder.mDownloadLabel = (TextView) convertView.findViewById(R.id.mDownloadLabel);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final DownloadEntry entry = mDownloadEntries.get(position);
            holder.mDownloadLabel.setText(entry.name + " is " + entry.status + " "
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

        //noinspection SimplifiableIfStatement
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
