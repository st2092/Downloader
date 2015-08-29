package com.example.sony.downloader;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.Log;

/**
 * This service downloads file in the background.
 * Even if the downloader application is exited, the service remains running
 * until the download(s) are complete.
 */
public class DownloaderService extends Service {
    public static final String ACTION_DOWNLOAD = "download";
    public static final String ACTION_DOWNLOAD_COMPLETE = "download_complete";
    public static final String ACTION_FETCH_LINKS = "fetch_links";
    public static final String ACTION_FETCH_LINKS_COMPLETE = "fetch_links_complete";

    // constant ID sent when we broadcast a download-complete message
    public static final int ID_NOTIFICATION_DL_COMPLETE = 1234;

    // handler thread loops waiting for jobs/tasks (downloads) to run
    // in a separate thread
    private HandlerThread handler_thread;

    /*
     * This method runs when the service starts up.
     * Set up initial state of the handler thread.
     */
    @Override
    public void
    onCreate()
    {
        super.onCreate();
        handler_thread = new HandlerThread("jobs_handler");
        handler_thread.start();
    }

    /*
     * This method gets call each time a request come in from the application via an intent.
     * It processes the request by enqueuing a new download job in the background handler thread.
     * If the request to fetch for all the links on the web page, then a new fetch job runs
     * in the background handler thread.
     */
    @Override
    public int
    onStartCommand(final Intent intent, int flags, int start_id)
    {
        String action = intent.getAction();
        if (action.equals(ACTION_DOWNLOAD))
        {
            Log.d("DownloaderService", "starting Action Download - DownloaderService");
            // create a runnable task to deal with this download
            Runnable runner_job = new Runnable()
            {
                public void run()
                {
                    // download the file
                    String url = intent.getStringExtra("url");
                    String filename = Downloader.download(url);

                    // show a notification in the top notification bar
                    Notification.Builder builder = new Notification.Builder(DownloaderService.this)
                            .setContentTitle("Download Complete")
                            .setContentText(url)
                            .setAutoCancel(true)
                            .setSmallIcon(R.drawable.icon_download);
                    Notification notification = builder.build();
                    NotificationManager notification_manager = (NotificationManager)
                            getSystemService(Context.NOTIFICATION_SERVICE);
                    notification_manager.notify(ID_NOTIFICATION_DL_COMPLETE, notification);

                    // broadcast a message back to the application to inform it that we are done downloading
                    Intent done = new Intent();
                    done.setAction(ACTION_DOWNLOAD_COMPLETE);
                    done.putExtra("url", url);
                    done.putExtra("filename", filename);
                    sendBroadcast(done);
                }
            };

            // wrap the runnable into a Handler job to be given to background thread
            Handler handler = new Handler(handler_thread.getLooper());
            handler.post(runner_job);
        }
        else if (action.equals(ACTION_FETCH_LINKS))
        {
            Log.d("DownloaderService", "starting Action Fetch Links - DownloaderService");

            // create a runnable task for obtaining the links on a web page
            // Android 3.0 and up requires network operation to be perform in a thread
            // to allow for smooth UI interface.
            Runnable runner_job = new Runnable()
            {
                public void run() {
                    String url = intent.getStringExtra("url");
                    String[] all_links = Downloader.getAllLinks(url);

                    // broadcast a message back to the application to inform fetching links is complete
                    Intent done = new Intent();
                    done.setAction(ACTION_FETCH_LINKS_COMPLETE);
                    done.putExtra("url", url);
                    done.putExtra("links_array", all_links);
                    sendBroadcast(done);
                }
            };

            // wrap the runnable into a Handler job to be given to background thread
            Handler handler = new Handler(handler_thread.getLooper());
            handler.post(runner_job);
        }

        if (action.equals(ACTION_DOWNLOAD)) {
            return START_STICKY;   // keep service running
        }
        else {
            return START_NOT_STICKY;    // do not keep service running
        }
    }

    /*
     * This method specifies how our service will deal with binding.
     * We do not choose to support binding, which is indicated by
     * returning null.
     */
    @Override
    public IBinder
    onBind(Intent intent)
    {
        return null;
    }
}
