package org.wordpress.android.ui.posts.photochooser;

import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.wordpress.android.R;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.DisplayUtils;

import uk.co.senab.photoview.PhotoViewAttacher;

public class PhotoChooserFragment extends Fragment {

    private static final int NUM_COLUMNS = 5;

    public enum PhotoChooserIcon {
        ANDROID_CAMERA,
        ANDROID_PICKER,
        WP_MEDIA
    }

    public interface OnPhotoChosenListener {
        void onPhotoTapped(Uri imageUri);
        void onPhotoDoubleTapped(Uri imageUri);
        void onPhotoLongPressed(Uri imageUri);
        void onIconClicked(PhotoChooserIcon icon);
    }

    private RecyclerView mRecycler;
    private View mPreviewFrame;

    public static PhotoChooserFragment newInstance() {
        Bundle args = new Bundle();
        PhotoChooserFragment fragment = new PhotoChooserFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.photo_chooser_fragment, container, false);

        mPreviewFrame = view.findViewById(R.id.frame_preview);

        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), NUM_COLUMNS);

        mRecycler = (RecyclerView) view.findViewById(R.id.recycler);
        mRecycler.setHasFixedSize(true);
        mRecycler.setLayoutManager(layoutManager);

        loadDevicePhotos();

        return view;
    }

    /*
     *   - single tap adds the photo to post
     *   - double tap previews the photo
     *   - long press enables multi-select
     */
    private final OnPhotoChosenListener mListener = new OnPhotoChosenListener() {
        @Override
        public void onPhotoTapped(Uri imageUri) {
            if (getActivity() instanceof EditPostActivity) {
                EditPostActivity activity = (EditPostActivity) getActivity();
                activity.addMedia(imageUri);
                activity.hidePhotoChooser();
            }
        }

        @Override
        public void onPhotoLongPressed(Uri imageUri) {
            // TODO: enable multi-select
        }

        @Override
        public void onPhotoDoubleTapped(Uri imageUri) {
            showPreview(imageUri);
        }

        @Override
        public void onIconClicked(PhotoChooserIcon icon) {
            if (getActivity() instanceof EditPostActivity) {
                EditPostActivity activity = (EditPostActivity) getActivity();
                activity.hidePhotoChooser();
                switch (icon) {
                    case ANDROID_CAMERA:
                        activity.launchCamera();
                        break;
                    case ANDROID_PICKER:
                        activity.launchPictureLibrary();
                        break;
                    case WP_MEDIA:
                        activity.startMediaGalleryAddActivity();
                        break;
                }
            }
        }
    };

    private void showPreview(Uri imageUri) {
        mRecycler.setEnabled(false);

        ImageView imgPreview = (ImageView) mPreviewFrame.findViewById(R.id.image_preview);
        imgPreview.setImageURI(imageUri);

        AniUtils.scaleIn(mPreviewFrame, AniUtils.Duration.SHORT);

        PhotoViewAttacher attacher = new PhotoViewAttacher(imgPreview);
        attacher.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
            @Override
            public void onPhotoTap(View view, float v, float v2) {
                hidePreview();
            }
        });
        attacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
                hidePreview();
            }
        });
    }

    private void hidePreview() {
        mRecycler.setEnabled(true);
        if (isPreviewShowing()) {
            AniUtils.scaleOut(mPreviewFrame, AniUtils.Duration.SHORT);
        }
    }

    private boolean isPreviewShowing() {
        return mPreviewFrame.getVisibility() == View.VISIBLE;
    }

    private static int getPhotoChooserImageWidth(Context context) {
        int displayWidth = DisplayUtils.getDisplayPixelWidth(context);
        return displayWidth / NUM_COLUMNS;
    }

    private static int getPhotoChooserImageHeight(Context context) {
        int imageWidth = getPhotoChooserImageWidth(context);
        return (int) (imageWidth * 0.75f);
    }

    private void loadDevicePhotos() {
        int imageWidth = getPhotoChooserImageWidth(getActivity());
        int imageHeight = getPhotoChooserImageHeight(getActivity());
        PhotoChooserAdapter adapter = new PhotoChooserAdapter(
                getActivity(), imageWidth, imageHeight, mListener);
        mRecycler.setAdapter(adapter);
        adapter.loadDevicePhotos();
    }
}
