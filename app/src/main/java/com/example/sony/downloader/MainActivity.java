package com.example.sony.downloader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Scanner;

/*
 * This activity is the main GUI for the downloader application. You can type
 * an URL in the top edit text, then press "Go". Afterwards, all links in that
 * page are shown as a list view. You can click the listed items to download
 * them in the background using a service.
 *
 * Even if the downloader application is exited, the download(s) will still
 * continue in the background until finish. The service use must be told to
 * stop manually for it to stop, which occurs after a download is completed.
 */
public class MainActivity extends ActionBarActivity
        implements AdapterView.OnItemClickListener{

    // web domain where files will be downloaded from
    private static final String DOMAIN = "http://www.martystepp.com/files/";

    private ArrayList<String> list_of_links;    // links for the ListView
    private ArrayAdapter<String> adapter;

    // handler thread loops waiting for jobs/tasks (getting links from web page) to run
    // in a separate thread
    private HandlerThread links_handler_thread;

    /*
     * This method runs when the activity starts up.
     * Sets up the initial state of the GUI.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        links_handler_thread = new HandlerThread("links_handler");
        links_handler_thread.start();

        // set up the ListView
        list_of_links = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list_of_links);
        ListView list_view = (ListView) findViewById(R.id.list_of_links);
        list_view.setAdapter(adapter);
        list_view.setOnItemClickListener(this); // 'this' will be AdapterView.OnItemClickListener
                                                // implemented in this activity

        // set up a broadcast receiver to receive notification when downloads are finished
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloaderService.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(new MyReceiver(), filter);
    }

    /*
     * This method gets call when the items in the ListView are clicked.
     * It initiates a request to the DownloadService to download the file.
     */
    @Override
    public void
    onItemClick(AdapterView<?> parent, View view, int index, long id)
    {
        Log.d("MainActivity", "onItemClick called ...");
        EditText edit_text = (EditText) findViewById(R.id.the_url);
        String domain = edit_text.getText().toString();
        String url = domain + list_of_links.get(index);

        // send request to DownloadService using an intent
        Intent intent = new Intent(this, DownloaderService.class);
        intent.putExtra("url", url);
        intent.setAction(DownloaderService.ACTION_DOWNLOAD);
        startService(intent);
    }

    /*
     * This method gets call when the user clicks the "Go" button.
     * It fetches all the links contained in the given web page.
     */
    public void
    onGoButtonClick(View view)
    {
        list_of_links.clear();
        EditText edit_text = (EditText) findViewById(R.id.the_url);
        final String web_page_url = edit_text.getText().toString();

        // create a runnable task for obtaining the links on a web page
        // Android 3.0 and up requires network operation to be perform in a thread
        // to allow for smooth UI interface.
        Runnable runner = new Runnable() {
            public void run () {
                String[] all_links = Downloader.getAllLinks(web_page_url);

                for (int i = 0; i < all_links.length; i++) {
                    list_of_links.add(all_links[i]);
                    Log.d("onGoButtonClick", i + ") " + all_links[i]);
                }
                /*
                // Read in file list from a text file for testing purpose
                Scanner scan = new Scanner(getResources().openRawResource(R.raw.file_list));
                while (scan.hasNextLine())
                {
                    list_of_links.add(scan.nextLine());
                }
                */
                adapter.notifyDataSetChanged();
            }
        };

        // wrap the runnable into a Handler job to be given to our background thread
        Handler handler = new Handler(links_handler_thread.getLooper());
        handler.post(runner);
    }

    /*
     * This broadcast receiver listens for broadcast indicating "download complete" sent
     * by the DownloadService and reacts to them by showing a toast.
     */
    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void
        onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action.equals(DownloaderService.ACTION_DOWNLOAD_COMPLETE))
            {
                String url = intent.getStringExtra("url");
                Toast.makeText(MainActivity.this, "done downloading " + url, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
