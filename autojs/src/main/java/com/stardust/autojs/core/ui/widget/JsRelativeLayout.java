package com.stardust.autojs.core.ui.widget;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.stardust.autojs.core.ui.JsViewHelper;

/**
 * Created by Stardust on 2017/5/14.
 */

public class JsRelativeLayout extends RelativeLayout {
    public JsRelativeLayout(Context context) {
        super(context);
    }

    public JsRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public JsRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public JsRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Nullable
    public View id(String id) {
        return JsViewHelper.findViewByStringId(this, id);
    }
}
