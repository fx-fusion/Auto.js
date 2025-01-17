package org.autojs.autojs.ui.main.drawer;

import android.app.AppOpsManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.stardust.app.AppOpsKt;
import com.stardust.app.GlobalAppContext;
import com.stardust.notification.NotificationListenerService;
import com.stardust.theme.ThemeColorManager;
import com.stardust.util.IntentUtil;
import com.stardust.view.accessibility.AccessibilityService;
import com.storyteller_f.bandage.Bandage;
import com.storyteller_f.bandage.Click;

import org.autojs.autojs.Pref;
import org.autojs.autojs.R;
import org.autojs.autojs.databinding.FragmentDrawerBinding;
import org.autojs.autojs.external.foreground.ForegroundService;
import org.autojs.autojs.network.NodeBB;
import org.autojs.autojs.network.UserService;
import org.autojs.autojs.network.VersionService;
import org.autojs.autojs.network.api.UserApi;
import org.autojs.autojs.network.entity.VersionInfo;
import org.autojs.autojs.network.entity.user.User;
import org.autojs.autojs.pluginclient.DevPluginService;
import org.autojs.autojs.theme.ThemeColorManagerCompat;
import org.autojs.autojs.tool.AccessibilityServiceTool;
import org.autojs.autojs.tool.Observers;
import org.autojs.autojs.tool.SimpleObserver;
import org.autojs.autojs.tool.WifiTool;
import org.autojs.autojs.ui.BaseActivity;
import org.autojs.autojs.ui.common.NotAskAgainDialog;
import org.autojs.autojs.ui.floating.CircularMenu;
import org.autojs.autojs.ui.floating.FloatyWindowManger;
import org.autojs.autojs.ui.main.MainActivity;
import org.autojs.autojs.ui.main.community.CommunityFragment;
import org.autojs.autojs.ui.settings.SettingsActivity;
import org.autojs.autojs.ui.update.UpdateInfoDialogBuilder;
import org.autojs.autojs.ui.user.LoginActivity;
import org.autojs.autojs.ui.user.WebActivity;
import org.autojs.autojs.ui.widget.BackgroundTarget;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


/**
 * Created by Stardust on 2017/1/30.
 * TODO these codes are so ugly!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 */
public class DrawerFragment extends androidx.fragment.app.Fragment {

    private static final String URL_DEV_PLUGIN = "https://www.autojs.org/topic/968/";
    private final DrawerMenuItem mStableModeItem = new DrawerMenuItem(R.drawable.ic_stable, R.string.text_stable_mode, R.string.key_stable_mode, null) {
        @Override
        public void setChecked(boolean checked) {
            super.setChecked(checked);
            if (checked)
                showStableModePromptIfNeeded();
        }
    };
    private final DrawerMenuItem mNotificationPermissionItem = new DrawerMenuItem(R.drawable.ic_ali_notification, R.string.text_notification_permission, 0, this::goToNotificationServiceSettings);
    private final DrawerMenuItem mUsageStatsPermissionItem = new DrawerMenuItem(R.drawable.ic_ali_notification, R.string.text_usage_stats_permission, 0, this::goToUsageStatsSettings);
    private final DrawerMenuItem mForegroundServiceItem = new DrawerMenuItem(R.drawable.ic_service_green, R.string.text_foreground_service, R.string.key_foreground_servie, this::toggleForegroundService);
    private final CommunityDrawerMenu mCommunityDrawerMenu = new CommunityDrawerMenu();
    @NonNull
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    private DrawerMenuAdapter mDrawerMenuAdapter;    private final DrawerMenuItem mAccessibilityServiceItem = new DrawerMenuItem(R.drawable.ic_service_green, R.string.text_accessibility_service, 0, this::enableOrDisableAccessibilityService);
    private Disposable mConnectionStateDisposable;
    private FragmentDrawerBinding inflate;    private final DrawerMenuItem mConnectionItem = new DrawerMenuItem(R.drawable.ic_connect_to_pc, R.string.debug, 0, this::connectOrDisconnectToRemote);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        inflate = FragmentDrawerBinding.inflate(inflater, container, false);
        return inflate.getRoot();
    }    private final DrawerMenuItem mFloatingWindowItem = new DrawerMenuItem(R.drawable.ic_robot_64, R.string.text_floating_window, 0, this::showOrDismissFloatingWindow);

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUpViews();
        inflate.avatar.setTag("avatar");
        Bandage.bind(this, inflate.getRoot());
    }    private final DrawerMenuItem mCheckForUpdatesItem = new DrawerMenuItem(R.drawable.ic_check_for_updates, R.string.text_check_for_updates, this::checkForUpdates);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConnectionStateDisposable = DevPluginService.getInstance().connectionState()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> {
                    setChecked(mConnectionItem, state.getState() == DevPluginService.State.CONNECTED);
                    setProgress(mConnectionItem, state.getState() == DevPluginService.State.CONNECTING);
                    if (state.getException() != null) {
                        showMessage(state.getException().getMessage());
                    }
                });
        EventBus.getDefault().register(this);

    }

    void setUpViews() {
        ThemeColorManager.addViewBackground(inflate.header);
        initMenuItems();
        if (Pref.isFloatingMenuShown()) {
            FloatyWindowManger.showCircularMenuIfNeeded();
            setChecked(mFloatingWindowItem, true);
        }
        setChecked(mConnectionItem, DevPluginService.getInstance().isConnected());
        if (Pref.isForegroundServiceEnabled()) {
            ForegroundService.start(GlobalAppContext.get());
            setChecked(mForegroundServiceItem, true);
        }
    }

    private void initMenuItems() {
        mDrawerMenuAdapter = new DrawerMenuAdapter(new ArrayList<>(Arrays.asList(
                new DrawerMenuGroup(R.string.text_service),
                mAccessibilityServiceItem,
                mStableModeItem,
                mNotificationPermissionItem,
                mForegroundServiceItem,
                mUsageStatsPermissionItem,

                new DrawerMenuGroup(R.string.text_script_record),
                mFloatingWindowItem,
                new DrawerMenuItem(R.drawable.ic_volume, R.string.text_volume_down_control, R.string.key_use_volume_control_record, null),

                new DrawerMenuGroup(R.string.text_others),
                mConnectionItem,
                new DrawerMenuItem(R.drawable.ic_personalize, R.string.text_theme_color, this::openThemeColorSettings),
                new DrawerMenuItem(R.drawable.ic_night_mode, R.string.text_night_mode, R.string.key_night_mode, this::toggleNightMode),
                mCheckForUpdatesItem
        )));
        inflate.drawerMenu.setAdapter(mDrawerMenuAdapter);
        inflate.drawerMenu.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Click(tag = "avatar")
    void loginOrShowUserInfo() {
        Disposable subscribe = UserService.getInstance()
                .me()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(user -> {
                            if (getActivity() == null)
                                return;
                            WebActivity.intent(this.getContext())
                                    .extra(WebActivity.EXTRA_URL, NodeBB.url("user/" + user.getUserslug()))
                                    .extra(Intent.EXTRA_TITLE, user.getUsername())
                                    .start();
                        },
                        error -> {
                            if (getActivity() == null)
                                return;
                            LoginActivity.intent(getActivity()).start();
                        }
                );
        compositeDisposable.add(subscribe);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        compositeDisposable.dispose();
    }

    void enableOrDisableAccessibilityService(@NonNull DrawerMenuItemViewHolder holder) {
        boolean isAccessibilityServiceEnabled = isAccessibilityServiceEnabled();
        boolean checked = holder.getSwitchCompat().isChecked();
        if (checked && !isAccessibilityServiceEnabled) {
            enableAccessibilityService();
        } else if (!checked && isAccessibilityServiceEnabled) {
            if (!AccessibilityService.Companion.disable()) {
                AccessibilityServiceTool.goToAccessibilitySetting();
            }
        }
    }

    void goToNotificationServiceSettings(@NonNull DrawerMenuItemViewHolder holder) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            return;
        }
        boolean enabled = NotificationListenerService.Companion.getInstance() != null;
        boolean checked = holder.getSwitchCompat().isChecked();
        if ((checked && !enabled) || (!checked && enabled)) {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        }
    }

    void goToUsageStatsSettings(@NonNull DrawerMenuItemViewHolder holder) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        boolean enabled = AppOpsKt.isOpPermissionGranted(getContext(), AppOpsManager.OPSTR_GET_USAGE_STATS);
        boolean checked = holder.getSwitchCompat().isChecked();
        if (checked && !enabled) {
            if (new NotAskAgainDialog.Builder(getContext(), "DrawerFragment.usage_stats")
                    .title(R.string.text_usage_stats_permission)
                    .content(R.string.description_usage_stats_permission)
                    .positiveText(R.string.ok)
                    .dismissListener(dialog -> IntentUtil.requestAppUsagePermission(getContext()))
                    .show() == null) {
                IntentUtil.requestAppUsagePermission(getContext());
            }
        }
        if (!checked && enabled) {
            IntentUtil.requestAppUsagePermission(getContext());
        }
    }

    void showOrDismissFloatingWindow(@NonNull DrawerMenuItemViewHolder holder) {
        boolean isFloatingWindowShowing = FloatyWindowManger.isCircularMenuShowing();
        boolean checked = holder.getSwitchCompat().isChecked();
        if (getActivity() != null && !getActivity().isFinishing()) {
            Pref.setFloatingMenuShown(checked);
        }
        if (checked && !isFloatingWindowShowing) {
            setChecked(mFloatingWindowItem, FloatyWindowManger.showCircularMenu());
            enableAccessibilityServiceByRootIfNeeded();
        } else if (!checked && isFloatingWindowShowing) {
            FloatyWindowManger.hideCircularMenu();
        }
    }

    void openThemeColorSettings(DrawerMenuItemViewHolder holder) {
        SettingsActivity.selectThemeColor(getActivity());
    }

    void toggleNightMode(@NonNull DrawerMenuItemViewHolder holder) {
        ((BaseActivity) getActivity()).setNightModeEnabled(holder.getSwitchCompat().isChecked());
    }

    private void enableAccessibilityServiceByRootIfNeeded() {
        Disposable subscribe = Observable.fromCallable(() -> Pref.shouldEnableAccessibilityServiceByRoot() && !isAccessibilityServiceEnabled())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(needed -> {
                    if (needed) {
                        enableAccessibilityServiceByRoot();
                    }
                });
        compositeDisposable.add(subscribe);

    }

    void connectOrDisconnectToRemote(@NonNull DrawerMenuItemViewHolder holder) {
        boolean checked = holder.getSwitchCompat().isChecked();
        boolean connected = DevPluginService.getInstance().isConnected();
        if (checked && !connected) {
            inputRemoteHost();
        } else if (!checked && connected) {
            DevPluginService.getInstance().disconnectIfNeeded();
        }
    }

    private void toggleForegroundService(@NonNull DrawerMenuItemViewHolder holder) {
        boolean checked = holder.getSwitchCompat().isChecked();
        if (checked) {
            ForegroundService.start(GlobalAppContext.get());
        } else {
            ForegroundService.stop(GlobalAppContext.get());
        }
    }

    private void inputRemoteHost() {
        String host = Pref.getServerAddressOrDefault(WifiTool.getRouterIp(getActivity()));
        new MaterialDialog.Builder(getActivity())
                .title(R.string.text_server_address)
                .input("", host, (dialog, input) -> {
                    Pref.saveServerAddress(input.toString());
                    Disposable subscribe = DevPluginService.getInstance().connectToServer(input.toString())
                            .subscribe(Observers.emptyConsumer(), this::onConnectException);
                    compositeDisposable.add(subscribe);
                })
                .neutralText(R.string.text_help)
                .onNeutral((dialog, which) -> {
                    setChecked(mConnectionItem, false);
                    IntentUtil.browse(getActivity(), URL_DEV_PLUGIN);
                })
                .cancelListener(dialog -> setChecked(mConnectionItem, false))
                .show();
    }

    private void onConnectException(@NonNull Throwable e) {
        setChecked(mConnectionItem, false);
        Toast.makeText(GlobalAppContext.get(), getString(R.string.error_connect_to_remote, e.getMessage()),
                Toast.LENGTH_LONG).show();
    }

    void checkForUpdates(DrawerMenuItemViewHolder holder) {
        setProgress(mCheckForUpdatesItem, true);
        VersionService.getInstance().checkForUpdates()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SimpleObserver<VersionInfo>() {

                    @Override
                    public void onNext(@io.reactivex.annotations.NonNull VersionInfo versionInfo) {
                        if (getActivity() == null)
                            return;
                        if (versionInfo.isNewer()) {
                            new UpdateInfoDialogBuilder(getActivity(), versionInfo)
                                    .show();
                        } else {
                            Toast.makeText(GlobalAppContext.get(), R.string.text_is_latest_version, Toast.LENGTH_SHORT).show();
                        }
                        setProgress(mCheckForUpdatesItem, false);
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        e.printStackTrace();
                        Toast.makeText(GlobalAppContext.get(), R.string.text_check_update_error, Toast.LENGTH_SHORT).show();
                        setProgress(mCheckForUpdatesItem, false);
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        syncSwitchState();
        syncUserInfo();
    }

    private void syncUserInfo() {
        Disposable subscribe = NodeBB.getInstance().getRetrofit()
                .create(UserApi.class)
                .me()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setUpUserInfo, error -> {
                    error.printStackTrace();
                    setUpUserInfo(null);
                });
        compositeDisposable.add(subscribe);
    }

    private void setUpUserInfo(@Nullable User user) {
        if (user == null) {
            inflate.username.setText(R.string.not_login);
            inflate.avatar.setIcon(R.drawable.profile_avatar_placeholder);
        } else {
            inflate.username.setText(user.getUsername());
            inflate.avatar.setUser(user);
        }
        setCoverImage(user);
    }

    private void setCoverImage(@Nullable User user) {
        if (user == null || TextUtils.isEmpty(user.getCoverUrl()) || user.getCoverUrl().equals("/assets/images/cover-default.png")) {
            inflate.defaultCover.setVisibility(View.VISIBLE);
            inflate.shadow.setVisibility(View.GONE);
            inflate.header.setBackgroundColor(ThemeColorManagerCompat.getColorPrimary());
        } else {
            inflate.defaultCover.setVisibility(View.GONE);
            inflate.shadow.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(NodeBB.BASE_URL + user.getCoverUrl())
                    .apply(new RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                    )
                    .into(new BackgroundTarget(inflate.header));
        }
    }

    private void syncSwitchState() {
        setChecked(mAccessibilityServiceItem, AccessibilityServiceTool.isAccessibilityServiceEnabled(getActivity()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            setChecked(mNotificationPermissionItem, NotificationListenerService.Companion.getInstance() != null);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setChecked(mUsageStatsPermissionItem, AppOpsKt.isOpPermissionGranted(getContext(), AppOpsManager.OPSTR_GET_USAGE_STATS));
        }
    }

    private void enableAccessibilityService() {
        if (!Pref.shouldEnableAccessibilityServiceByRoot()) {
            AccessibilityServiceTool.goToAccessibilitySetting();
            return;
        }
        enableAccessibilityServiceByRoot();
    }

    private void enableAccessibilityServiceByRoot() {
        setProgress(mAccessibilityServiceItem, true);
        Disposable subscribe = Observable.fromCallable(() -> AccessibilityServiceTool.enableAccessibilityServiceByRootAndWaitFor(4000))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(succeed -> {
                    if (!succeed) {
                        Toast.makeText(getContext(), R.string.text_enable_accessibitliy_service_by_root_failed, Toast.LENGTH_SHORT).show();
                        AccessibilityServiceTool.goToAccessibilitySetting();
                    }
                    setProgress(mAccessibilityServiceItem, false);
                });
        compositeDisposable.add(subscribe);
    }

    @Subscribe
    public void onCircularMenuStateChange(@NonNull CircularMenu.StateChangeEvent event) {
        setChecked(mFloatingWindowItem, event.getCurrentState() != CircularMenu.STATE_CLOSED);
    }

    @Subscribe
    public void onCommunityPageVisibilityChange(@NonNull CommunityFragment.VisibilityChange change) {
        if (change.visible) {
            mCommunityDrawerMenu.showCommunityMenu(mDrawerMenuAdapter);
        } else {
            mCommunityDrawerMenu.hideCommunityMenu(mDrawerMenuAdapter);
        }
        inflate.drawerMenu.scrollToPosition(0);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLoginStateChange(@NonNull UserService.LoginStateChange change) {
        syncUserInfo();
        if (mCommunityDrawerMenu.isShown()) {
            mCommunityDrawerMenu.setUserOnlineStatus(mDrawerMenuAdapter, change.isOnline());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDrawerOpen(MainActivity.DrawerOpenEvent event) {
        if (mCommunityDrawerMenu.isShown()) {
            mCommunityDrawerMenu.refreshNotificationCount(mDrawerMenuAdapter);
        }
    }

    private void showStableModePromptIfNeeded() {
        new NotAskAgainDialog.Builder(getContext(), "DrawerFragment.stable_mode")
                .title(R.string.text_stable_mode)
                .content(R.string.description_stable_mode)
                .positiveText(R.string.ok)
                .show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mConnectionStateDisposable.dispose();
        EventBus.getDefault().unregister(this);
    }

    private void showMessage(CharSequence text) {
        if (getContext() == null)
            return;
        Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show();
    }

    private void setProgress(@NonNull DrawerMenuItem item, boolean progress) {
        item.setProgress(progress);
        mDrawerMenuAdapter.notifyItemChanged(item);
    }

    private void setChecked(@NonNull DrawerMenuItem item, boolean checked) {
        item.setChecked(checked);
        mDrawerMenuAdapter.notifyItemChanged(item);
    }

    private boolean isAccessibilityServiceEnabled() {
        return AccessibilityServiceTool.isAccessibilityServiceEnabled(getActivity());
    }











}
