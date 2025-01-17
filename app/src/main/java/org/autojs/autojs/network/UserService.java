package org.autojs.autojs.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;
import com.stardust.util.Objects;

import org.autojs.autojs.network.api.UserApi;
import org.autojs.autojs.network.entity.notification.Notification;
import org.autojs.autojs.network.entity.notification.NotificationResponse;
import org.autojs.autojs.network.entity.user.User;
import org.greenrobot.eventbus.EventBus;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;

/**
 * Created by Stardust on 2017/9/20.
 */

public class UserService {

    private static final UserService sInstance = new UserService();
    @NonNull
    private final Retrofit mRetrofit;
    @NonNull
    private final UserApi mUserApi;
    @NonNull
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    @Nullable
    private volatile User mUser;

    UserService() {
        mRetrofit = NodeBB.getInstance().getRetrofit();
        mUserApi = mRetrofit.create(UserApi.class);
    }

    @NonNull
    public static UserService getInstance() {
        return sInstance;
    }

    public Observable<ResponseBody> login(String userName, final String password) {
        return NodeBB.getInstance()
                .getXCsrfToken()
                .flatMap(token ->
                        mUserApi.login(token, userName, password)
                                .doOnError(error -> {
                                    if (error instanceof HttpException && ((HttpException) error).code() == 403) {
                                        NodeBB.getInstance().invalidateXCsrfToken();
                                    }
                                }))
                .doOnComplete(this::refreshOnlineStatus);

    }


    public Observable<ResponseBody> register(String email, String userName, String password) {
        return NodeBB.getInstance()
                .getXCsrfToken()
                .flatMap(token -> mUserApi.register(token, email, userName, password, password));
    }

    public boolean isOnline() {
        return mUser != null;
    }

    private void setUser(@Nullable User user) {
        User old = mUser;
        mUser = user;
        if (mUser != null) {
//            CrashReport.setUserId(mUser.getUid());
        }
        if (!Objects.equals(old, mUser)) {
            if (user == null) {
                NodeBB.getInstance().invalidateXCsrfToken();
            }
            EventBus.getDefault().post(new LoginStateChange(user != null));
        }
    }

    @NonNull
    public Observable<Boolean> refreshOnlineStatus() {
        PublishSubject<Boolean> online = PublishSubject.create();
        Disposable subscribe = mRetrofit.create(UserApi.class)
                .me()
                .subscribeOn(Schedulers.io())
                .subscribe(user -> {
                    setUser(user);
                    online.onNext(true);
                    online.onComplete();
                }, error -> {
                    setUser(null);
                    online.onNext(false);
                    online.onComplete();
                });
        compositeDisposable.add(subscribe);
        return online;
    }

    public Observable<ResponseBody> logout() {
        return NodeBB.getInstance()
                .getXCsrfToken()
                .flatMap(mUserApi::logout)
                .doOnError(Throwable::printStackTrace)
                .doOnComplete(this::refreshOnlineStatus);
    }

    public Observable<User> me() {
        return NodeBB.getInstance().getRetrofit()
                .create(UserApi.class)
                .me()
                .doOnNext(this::setUser)
                .doOnError(error -> setUser(null));
    }

    public Observable<List<Notification>> getNotifications() {
        return NodeBB.getInstance().getRetrofit()
                .create(UserApi.class)
                .getNotifitions()
                .map(NotificationResponse::getNotifications);
    }

    public static class LoginStateChange {
        private final boolean mOnline;

        public LoginStateChange(boolean online) {
            mOnline = online;
        }

        public boolean isOnline() {
            return mOnline;
        }
    }

}
