package org.autojs.autojs.ui.edit.debug;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.autojs.autojs.R;
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder;

public class CodeEvaluateDialogBuilder extends ThemeColorMaterialDialogBuilder {

    private static final String KEY_CODE = CodeEvaluateDialogBuilder.class.getName() + ".code";
    private final SharedPreferences mSharedPreferences;
    private CodeEvaluator mCodeEvaluator;
    private TextView mResult;
    private EditText mCode;

    public CodeEvaluateDialogBuilder(@NonNull Context context) {
        super(context);
        setupViews();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mCode.setText(mSharedPreferences.getString(KEY_CODE, ""));
    }

    private void setupViews() {
        View view = View.inflate(context, R.layout.dialog_code_evaluate, null);
        customView(view, true);
        mResult = view.findViewById(R.id.result);
        mCode = view.findViewById(R.id.code);
        positiveText(R.string.text_execute);
        negativeText(R.string.text_close);
        autoDismiss(false);
        onNegative((dialog, which) -> dialog.dismiss());
        onPositive(((dialog, which) -> {
            Editable code = mCode.getText();
            if (!TextUtils.isEmpty(code)) {
                String codeStr = code.toString();
                mSharedPreferences.edit().putString(KEY_CODE, codeStr).apply();
                mResult.setText(mCodeEvaluator.eval(codeStr));
            }
        }));
    }

    @NonNull
    public CodeEvaluateDialogBuilder codeEvaluator(CodeEvaluator evaluator) {
        mCodeEvaluator = evaluator;
        return this;
    }


}
