package org.autojs.autojs.tool;

import android.content.Context;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.MaterialDialog;

import org.autojs.autojs.R;

/**
 * Created by Stardust on 2017/4/18.
 */

public class MaterialDialogFactory {
    public static MaterialDialog createProgress(@NonNull Context context) {
        return new MaterialDialog.Builder(context)
                .progress(true, 0)
                .cancelable(false)
                .content(R.string.text_processing)
                .build();
    }

    public static MaterialDialog showProgress(@NonNull Context context) {
        MaterialDialog dialog = createProgress(context);
        dialog.show();
        return dialog;
    }
}
