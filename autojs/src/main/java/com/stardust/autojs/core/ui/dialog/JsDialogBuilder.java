package com.stardust.autojs.core.ui.dialog;

import android.content.Context;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.MaterialDialog;
import com.stardust.autojs.core.eventloop.EventEmitter;
import com.stardust.autojs.core.looper.Loopers;
import com.stardust.autojs.core.looper.Timer;
import com.stardust.autojs.runtime.ScriptRuntime;
import com.stardust.util.UiHandler;

/**
 * Created by Stardust on 2018/4/17.
 */

public class JsDialogBuilder extends MaterialDialog.Builder {

    @NonNull
    private final EventEmitter mEmitter;
    private final UiHandler mUiHandler;
    private final Timer mTimer;
    private final Loopers mLoopers;
    private JsDialog mDialog;
    private volatile int mWaitId = -1;


    public JsDialogBuilder(@NonNull Context context, @NonNull ScriptRuntime runtime) {
        super(context);
        mTimer = runtime.timers.getTimerForCurrentThread();
        mLoopers = runtime.loopers;
        mEmitter = new EventEmitter(runtime.bridges);
        mUiHandler = runtime.uiHandler;
        setUpEvents();
    }

    public static int defaultMaxListeners() {
        return EventEmitter.defaultMaxListeners();
    }

    private void setUpEvents() {
        showListener(dialog -> emit("show", dialog));
        onAny((dialog, which) -> {
            switch (which) {
                case NEUTRAL:
                    emit("neutral", dialog);
                    emit("any", "neutral", dialog);
                    break;
                case NEGATIVE:
                    emit("negative", dialog);
                    emit("any", "negative", dialog);
                    break;
                case POSITIVE:
                    EditText editText = dialog.getInputEditText();
                    if (editText != null) {
                        emit("input", editText.getText().toString());
                    }
                    emit("positive", dialog);
                    emit("any", "positive", dialog);
                    break;
            }
        });
        dismissListener(dialog -> {
            mTimer.postDelayed(() -> mLoopers.doNotWaitWhenIdle(mWaitId), 0);
            emit("dismiss", dialog);
        });
        cancelListener(dialog -> emit("cancel", dialog));
    }

    public void onShowCalled() {
        mTimer.postDelayed(() -> mWaitId = mLoopers.waitWhenIdle(), 0);
    }

    public JsDialog getDialog() {
        return mDialog;
    }

    public JsDialog buildDialog() {
        mDialog = new JsDialog(this, mEmitter, mUiHandler);
        return mDialog;
    }

    @NonNull
    public JsDialogBuilder once(String eventName, Object listener) {
        mEmitter.once(eventName, listener);
        return this;
    }

    @NonNull
    public JsDialogBuilder on(String eventName, Object listener) {
        mEmitter.on(eventName, listener);
        return this;
    }

    @NonNull
    public JsDialogBuilder addListener(String eventName, Object listener) {
        mEmitter.addListener(eventName, listener);
        return this;
    }

    public boolean emit(String eventName, Object... args) {
        return mEmitter.emit(eventName, args);
    }

    public String[] eventNames() {
        return mEmitter.eventNames();
    }

    public int listenerCount(String eventName) {
        return mEmitter.listenerCount(eventName);
    }

    public Object[] listeners(String eventName) {
        return mEmitter.listeners(eventName);
    }

    @NonNull
    public JsDialogBuilder prependListener(String eventName, Object listener) {
        mEmitter.prependListener(eventName, listener);
        return this;
    }

    @NonNull
    public JsDialogBuilder prependOnceListener(String eventName, Object listener) {
        mEmitter.prependOnceListener(eventName, listener);
        return this;
    }

    @NonNull
    public JsDialogBuilder removeAllListeners() {
        mEmitter.removeAllListeners();
        return this;
    }

    @NonNull
    public JsDialogBuilder removeAllListeners(String eventName) {
        mEmitter.removeAllListeners(eventName);
        return this;
    }

    @NonNull
    public JsDialogBuilder removeListener(String eventName, Object listener) {
        mEmitter.removeListener(eventName, listener);
        return this;
    }

    public int getMaxListeners() {
        return mEmitter.getMaxListeners();
    }

    @NonNull
    public JsDialogBuilder setMaxListeners(int n) {
        mEmitter.setMaxListeners(n);
        return this;
    }

}
