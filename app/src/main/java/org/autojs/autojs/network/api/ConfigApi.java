package org.autojs.autojs.network.api;

import androidx.annotation.NonNull;

import org.autojs.autojs.network.entity.config.Config;

import io.reactivex.Observable;
import retrofit2.http.GET;

/**
 * Created by Stardust on 2017/10/26.
 */

public interface ConfigApi {

    @NonNull
    @GET("/api/config")
    Observable<Config> getConfig();

}
