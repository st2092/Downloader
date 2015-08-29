package com.example.sony.downloader;

import android.app.*;
import android.content.*;
import android.os.*;
import android.support.v7.app.ActionBarActivity;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.util.*;

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
    private String DOMAIN;

    private ArrayList<String> list_of_links;    // links for the ListView
    private ArrayAdapter<String> adapter;
    private MyReceiver my_receiver;
    private ArrayList<String> files_downloaded; // keeps track of all the files downloaded while app is open


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

        files_downloaded = new ArrayList<>();

        links_handler_thread = new HandlerThread("links_handler");
        links_handler_thread.start();

        // set up the ListView
        list_of_links = new ArrayList<>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list_of_links);
        ListView list_view = (ListView) findViewById(R.id.list_of_links);
        list_view.setAdapter(adapter);
        list_view.setOnItemClickListener(this); // 'this' will be AdapterView.OnItemClickListener
                                                // implemented in this activity

        // set up a broadcast receiver to receive notification when downloads are finished
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloaderService.ACTION_DOWNLOAD_COMPLETE);
        filter.addAction(DownloaderService.ACTION_FETCH_LINKS_COMPLETE);
        my_receiver = new MyReceiver();
        registerReceiver(my_receiver, filter);
    }

    /*
     * This method gets call when the application stops.
     * Unregisters the broadcast receiver.
     */
    @Override
    protected void
    onStop()
    {
        super.onStop();

        unregisterReceiver(my_receiver);
        Log.d("MainActivity", "onStop() called ...");
    }

    /*
     * This method gets call when the items in the ListView are clicked.
     * It initiates a request to the DownloadService to download the file.
     */
    @Override
    public void
    onItemClick(AdapterView<?> parent, View view, int index, long id)
    {
        EditText edit_text = (EditText) findViewById(R.id.the_url);
        String domain = edit_text.getText().toString();
        String url = list_of_links.get(index);

        if (!url.contains("http"))
        {
            // only add the domain if it is missing
            url = DOMAIN + url;
        }

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
        String web_page_url = edit_text.getText().toString();
        DOMAIN = web_page_url;

        // send request to DownloaderService using an intent
        Intent intent = new Intent(this, DownloaderService.class);
        intent.putExtra("url", web_page_url);
        intent.setAction(DownloaderService.ACTION_FETCH_LINKS);
        startService(intent);
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
                files_downloaded.add(intent.getStringExtra("filename"));
                Log.d("MainActivity", "filename is " + intent.getStringExtra("filename"));
                Toast.makeText(MainActivity.this, "done downloading from " + url, Toast.LENGTH_SHORT).show();
            }
            else if (action.equals(DownloaderService.ACTION_FETCH_LINKS_COMPLETE))
            {

                String url = intent.getStringExtra("url");
                updateListOfLinks(intent.getStringArrayExtra("links_array"));
                Toast.makeText(MainActivity.this, "done fetching links from " + url, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*
     * This method fills the ListView with all the links from a web page.
     * The web page is the one in the EditText at the time when the "Go"
     * button was clicked.
     */
    private void
    updateListOfLinks(String[] links)
    {
        list_of_links.clear();
        for (int i = 0; i < links.length; i++)
        {
            list_of_links.add(links[i]);
        }
        adapter.notifyDataSetChanged();
    }
}
