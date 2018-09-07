package com.bignerdranch.android.photogallery;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by djn on 9/5/18.
 */

public class PhotoGalleryFragment extends Fragment
{
    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;

    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>>
    {
        @Override
        protected List<GalleryItem> doInBackground(Void... params)
        {
            return new FlickrFetchr().fetchItems();
        }

        /**
         * resets the adapter since the model data is now changed after fetching
         * The result of the background computation is passed to this step as a parameter.
         * @param galleryItems
         */
        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems)
        {
            mItems = galleryItems;
            setupAdapter();
        }
    }

    public static PhotoGalleryFragment newInstance()
    {
       return new PhotoGalleryFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = v.findViewById(R.id.photo_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        setupAdapter();

        return v;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemsTask().execute();

        mThumbnailDownloader = new ThumbnailDownloader<>();
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        mThumbnailDownloader.quit();// ends the thread when the fragment is destroyed
        Log.i(TAG, "Background thread destroyed");
    }

    private class PhotoHolder extends RecyclerView.ViewHolder
    {
        private ImageView mItemImageView;

        /**
         * Though PhotoHolder do need an itemView, this itemView is inflated in the adapter, not
         * passed automatically.
         * @param itemView
         */
        public PhotoHolder(View itemView)
        {
            super(itemView);

            mItemImageView = (ImageView) itemView.findViewById(R.id.item_image_view);
        }

        public void bindDrawable(Drawable drawable)
        {
            mItemImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>
    {
        private List<GalleryItem> mGalleryItem;

        public PhotoAdapter(List<GalleryItem> galleryItems)
        {
            mGalleryItem = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View v = inflater.inflate(R.layout.list_item_gallery, parent, false);
            return new PhotoHolder(v);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
           GalleryItem item = mGalleryItem.get(position);
            Drawable placeholder = getResources().getDrawable(R.drawable.bill_up_close);
            holder.bindDrawable(placeholder);
            mThumbnailDownloader.queueThumbnail(holder, item.getUrl());
        }

        @Override
        public int getItemCount() {
            return mGalleryItem.size();
        }
    }

    /**
     * Setup a PhotoAdapter every time the fragment is added
     * The FetchItemsTask runs in background when the fragment is created, meaning the fragment may
     * not be attached to the activity when model data is returned
     */
    private void setupAdapter()
    {
        if (isAdded()) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }
}
