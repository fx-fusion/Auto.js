package org.autojs.autojs.ui.main.community;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.stardust.util.BackPressedHandler;

import org.autojs.autojs.databinding.FragmentCommunityBinding;
import org.autojs.autojs.network.NodeBB;
import org.autojs.autojs.ui.main.QueryEvent;
import org.autojs.autojs.ui.main.ViewPagerFragment;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.net.URLEncoder;

/**
 * Created by Stardust on 2017/8/22.
 */
public class CommunityFragment extends ViewPagerFragment implements BackPressedHandler {
    private static final String TAG = "CommunityFragment";

    private FragmentCommunityBinding inflate;

    public static class LoadUrl {
        public final String url;

        public LoadUrl(String url) {
            this.url = url;
        }

    }

    public static class VisibilityChange {
        public final boolean visible;

        public VisibilityChange(boolean visible) {
            this.visible = visible;
        }
    }

    private static final String POSTS_PAGE_PATTERN = "[\\S\\s]+/topic/[0-9]+/[\\S\\s]+";

    WebView mWebView;

    public CommunityFragment() {
        super(0);
        setArguments(new Bundle());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,@Nullable Bundle savedInstanceState) {
        inflate = FragmentCommunityBinding.inflate(getLayoutInflater());
        return inflate.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUpViews();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EventBus.getDefault().register(this);
    }
    void setUpViews() {
        Log.d(TAG, "setUpViews() called");
        mWebView = inflate.ewebView.getWebView();
        String url = "https://www.autojs.org/";
        Bundle savedWebViewState = getArguments().getBundle("savedWebViewState");
        if (savedWebViewState != null) {
            mWebView.restoreState(savedWebViewState);
        } else {
            mWebView.loadUrl(url);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Bundle savedWebViewState = new Bundle();
        mWebView.saveState(savedWebViewState);
        getArguments().putBundle("savedWebViewState", savedWebViewState);
    }

    @Override
    public boolean onBackPressed(Activity activity) {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return false;
    }


    @Override
    protected void onFabClick(FloatingActionButton fab) {
        if (isInPostsPage()) {
            mWebView.loadUrl("javascript:$('button[component=\"topic/reply\"]').click()");
        } else {
            mWebView.loadUrl("javascript:$('#new_topic').click()");
        }
    }

    @Subscribe
    public void loadUrl(@NonNull LoadUrl loadUrl) {
        mWebView.loadUrl(NodeBB.url(loadUrl.url));
    }

    @Subscribe
    public void submitQuery(@NonNull QueryEvent event) {
        if (!isShown() || event == QueryEvent.CLEAR) {
            return;
        }
        String query = URLEncoder.encode(event.getQuery());
        String url = String.format("http://www.autojs.org/search?term=%s&in=titlesposts", query);
        mWebView.loadUrl(url);
        event.collapseSearchView();
    }

    private boolean isInPostsPage() {
        String url = mWebView.getUrl();
        return url != null &&  url.matches(POSTS_PAGE_PATTERN);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onPageShow() {
        super.onPageShow();
        EventBus.getDefault().post(new VisibilityChange(true));
    }

    @Override
    public void onPageHide() {
        super.onPageHide();
        EventBus.getDefault().post(new VisibilityChange(false));
    }
}
