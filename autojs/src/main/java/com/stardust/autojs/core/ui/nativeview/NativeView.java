package com.stardust.autojs.core.ui.nativeview;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stardust.autojs.core.ui.JsViewHelper;
import com.stardust.autojs.core.ui.ViewExtras;
import com.stardust.autojs.core.ui.attribute.ViewAttributes;
import com.stardust.autojs.rhino.NativeJavaObjectWithPrototype;

import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

public class NativeView extends NativeJavaObjectWithPrototype {

    private static final String LOG_TAG = "NativeView";
    private final ViewAttributes mViewAttributes;
    private final View mView;
    @NonNull
    private final ViewPrototype mViewPrototype;
    public NativeView(Scriptable scope, View view, Class<?> staticType, @NonNull com.stardust.autojs.runtime.ScriptRuntime runtime) {
        super(scope, view, staticType);
        mViewAttributes = ViewExtras.getViewAttributes(view, runtime.ui.getResourceParser());
        mView = view;
        mViewPrototype = new ViewPrototype(mView, mViewAttributes, scope, runtime);
        prototype = new NativeJavaObjectWithPrototype(scope, mViewPrototype, mViewPrototype.getClass());
        prototype.setPrototype(new NativeObject());
    }

    @Override
    public boolean has(String name, Scriptable start) {
        if (mViewAttributes.contains(name)) {
            return true;
        }
        return super.has(name, start);
    }

    @Nullable
    @Override
    public Object get(@NonNull String name, Scriptable start) {
        if (super.has(name, start)) {
            return super.get(name, start);
        } else {
            View view = JsViewHelper.findViewByStringId(mView, name);
            if (view != null) {
                return view;
            }
        }
        return Scriptable.NOT_FOUND;
    }

    @NonNull
    public ViewPrototype getViewPrototype() {
        return mViewPrototype;
    }

    @Override
    public View unwrap() {
        return mView;
    }

    public static class ScrollEvent {
        public int scrollX;
        public int scrollY;
        public int oldScrollX;
        public int oldScrollY;

        public ScrollEvent(int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
            this.scrollX = scrollX;
            this.scrollY = scrollY;
            this.oldScrollX = oldScrollX;
            this.oldScrollY = oldScrollY;
        }
    }

    public static class LongClickEvent {
        public final View view;

        public LongClickEvent(View view) {
            this.view = view;
        }
    }

}
