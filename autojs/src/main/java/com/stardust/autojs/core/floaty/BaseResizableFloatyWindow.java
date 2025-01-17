package com.stardust.autojs.core.floaty;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stardust.autojs.R;
import com.stardust.autojs.core.ui.inflater.inflaters.Exceptions;
import com.stardust.autojs.runtime.exception.ScriptInterruptedException;
import com.stardust.concurrent.VolatileDispose;
import com.stardust.enhancedfloaty.FloatyService;
import com.stardust.enhancedfloaty.ResizableFloaty;
import com.stardust.enhancedfloaty.ResizableFloatyWindow;
import com.stardust.enhancedfloaty.WindowBridge;

/**
 * Created by Stardust on 2017/12/5.
 */

public class BaseResizableFloatyWindow extends ResizableFloatyWindow {

    private final VolatileDispose<RuntimeException> mInflateException = new VolatileDispose<>();
    private View mCloseButton;
    private int mOffset;
    public BaseResizableFloatyWindow(@NonNull Context context, ViewSupplier viewSupplier) {
        this(new MyFloaty(context, viewSupplier));
        mOffset = context.getResources().getDimensionPixelSize(R.dimen.floaty_window_offset);
    }


    private BaseResizableFloatyWindow(@NonNull MyFloaty floaty) {
        super(floaty);
    }

    public RuntimeException waitForCreation() {
        return mInflateException.blockedGetOrThrow(ScriptInterruptedException.class);
    }

    @NonNull
    @Override
    protected WindowBridge onCreateWindowBridge(WindowManager.LayoutParams params) {
        return new WindowBridge.DefaultImpl(params, getWindowManager(), getWindowView()) {
            @Override
            public int getX() {
                return super.getX() + mOffset;
            }

            @Override
            public int getY() {
                return super.getY() + mOffset;
            }

            @Override
            public void updatePosition(int x, int y) {
                super.updatePosition(x - mOffset, y - mOffset);
            }
        };
    }

    @Override
    public void onCreate(FloatyService service, WindowManager manager) {
        try {
            super.onCreate(service, manager);
        } catch (RuntimeException e) {
            mInflateException.setAndNotify(e);
            return;
        }
        mInflateException.setAndNotify(Exceptions.NO_EXCEPTION);
    }

    public void setOnCloseButtonClickListener(View.OnClickListener listener) {
        mCloseButton.setOnClickListener(listener);
    }

    public boolean isAdjustEnabled() {
        return getMoveCursor().getVisibility() == View.VISIBLE;
    }

    public void setAdjustEnabled(boolean enabled) {
        if (!enabled) {
            getMoveCursor().setVisibility(View.GONE);
            getResizer().setVisibility(View.GONE);
            mCloseButton.setVisibility(View.GONE);
        } else {
            getMoveCursor().setVisibility(View.VISIBLE);
            getResizer().setVisibility(View.VISIBLE);
            mCloseButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onViewCreated(@NonNull View view) {
        super.onViewCreated(view);
        mCloseButton = view.findViewById(R.id.close);
    }

    public void disableWindowFocus() {
        WindowManager.LayoutParams windowLayoutParams = getWindowLayoutParams();
        windowLayoutParams.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        updateWindowLayoutParams(windowLayoutParams);
    }

    public void requestWindowFocus() {
        WindowManager.LayoutParams windowLayoutParams = getWindowLayoutParams();
        windowLayoutParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        updateWindowLayoutParams(windowLayoutParams);
        getWindowView().requestLayout();
    }

    public interface ViewSupplier {

        @NonNull
        View inflate(Context context, ViewGroup parent);

    }

    private static class MyFloaty implements ResizableFloaty {


        private final ViewSupplier mContentViewSupplier;
        private final Context mContext;
        private View mRootView;


        public MyFloaty(Context context, ViewSupplier supplier) {
            mContentViewSupplier = supplier;
            mContext = context;
        }

        @Override
        public View inflateView(FloatyService floatyService, ResizableFloatyWindow resizableFloatyWindow) {
            mRootView = View.inflate(mContext, R.layout.floaty_window, null);
            FrameLayout container = mRootView.findViewById(R.id.container);
            View contentView = mContentViewSupplier.inflate(mContext, container);
            return mRootView;
        }

        @Nullable
        @Override
        public View getResizerView(@NonNull View view) {
            return view.findViewById(R.id.resizer);
        }

        @Nullable
        @Override
        public View getMoveCursorView(@NonNull View view) {
            return view.findViewById(R.id.move_cursor);
        }
    }
}
