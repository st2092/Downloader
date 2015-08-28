package com.example.sony.downloader;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

/**
 * This service downloads file in the background.
 * Even if the downloader application is exited, the service remains running
 * until the download(s) are complete.
 */
public class DownloaderService extends Service {
    public static final String ACTION_DOWNLOAD = "download";
    public static final String ACTION_DOWNLOAD_COMPLETE = "download_complete";

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
     */
    @Override
    public int
    onStartCommand(final Intent intent, int flags, int start_id)
    {
        String action = intent.getAction();
        if (action.equals(ACTION_DOWNLOAD))
        {
            // create a runnable task to deal with this download
            Runnable runner_job = new Runnable()
            {
                public void run()
                {
                    // download the file
                    String url = intent.getStringExtra("url");
                    Downloader.downloadFake(url);

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
                    sendBroadcast(done);
                }
            };

            // wrap the runnable into a Handler job to be given to background thread
            Handler handler = new Handler(handler_thread.getLooper());
            handler.post(runner_job);
        }

        return  START_STICKY;   // keep service running
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
