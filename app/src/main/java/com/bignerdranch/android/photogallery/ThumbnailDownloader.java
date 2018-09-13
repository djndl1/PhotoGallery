package com.bignerdranch.android.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by djn on 9/7/18.
 */

public class ThumbnailDownloader<T> extends HandlerThread
{
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private boolean mHasQuit = false;
    private Handler mRequestHandler;
    private Handler mResponseHandler;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;
    private ConcurrentMap<T,String> mRequestMap = new ConcurrentHashMap<>();

    public interface ThumbnailDownloadListener<T> {
       void onThumbnailDownloader(T target, Bitmap thumbnail);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloadListener = listener;
    }

    /**
     *
     * @param responseHandler - a handler attached to the main thread, passed into the download thread
     *
     */
    public ThumbnailDownloader(Handler responseHandler)
    {
       super(TAG);
        mResponseHandler = responseHandler;
    }

    /**
     * callback before Looper loops
     * HandlerThread.onLooperPrepared() is called before the Looper checks the queue for the first time.
     This makes it a good place to create the Handler implementation.
     */
    @Override
    protected void onLooperPrepared()
    {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    /**
     * download the image and set the the response handler to execute a runnable directly,
     * without adding a message
     * @param target
     */
    private void handleRequest(final T target)
    {
        try {
            final String url = mRequestMap.get(target);

            if (url == null) {
                return;
            }

            byte[] bitmapByte = new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapByte, 0, bitmapByte.length);
            Log.i(TAG, "Bitmap created");

            //after an image is downloaded, mResponseHandler, which is passed from the main thread
            //to the download thread, will execute the Runnable below directly
            mResponseHandler.post(new Runnable() {
                public void run() {
                    //double check the requestMap and the existence of downloader thread
                    if (mRequestMap.get(target) != url || mHasQuit) {
                        return;
                    }

                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownloader(target, bitmap);//set the bimap on the PhotoHolder
                }
            });
        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }
    }

    public void clearQueue()
    {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);//remove all messages with MESSAGE_DOWNLOAD
        mRequestMap.clear();//remove all mappings
    }

    /**
     * receive a url and add a download message to the message queue
     * @param target the PhotoHolder holding the image
     * @param url the url of the image to be downloaded
     */
    public void queueThumbnail(T target, String url)
    {
        Log.i(TAG, "Got a URL: " + url);

        if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget();
            //download request for the target T
        }
    }


}
