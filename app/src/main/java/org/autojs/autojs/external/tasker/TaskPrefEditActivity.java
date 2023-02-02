package org.autojs.autojs.external.tasker;

import static org.autojs.autojs.ui.edit.EditorView.EXTRA_CONTENT;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.storyteller_f.bandage.Click;
import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractAppCompatPluginActivity;

import org.autojs.autojs.R;
import org.autojs.autojs.databinding.ActivityTaskerEditBinding;
import org.autojs.autojs.external.ScriptIntents;
import org.autojs.autojs.model.explorer.ExplorerDirPage;
import org.autojs.autojs.model.explorer.Explorers;
import org.autojs.autojs.ui.BaseActivity;
import org.autojs.autojs.ui.explorer.ExplorerView;
import org.json.JSONObject;


/**
 * Created by Stardust on 2017/3/27.
 */
public class TaskPrefEditActivity extends AbstractAppCompatPluginActivity {

    @Nullable
    private String mSelectedScriptFilePath;
    @Nullable
    private String mPreExecuteScript;
    private ActivityTaskerEditBinding inflate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inflate = ActivityTaskerEditBinding.inflate(getLayoutInflater());
        setContentView(inflate.getRoot());
        inflate.editScript.setTag("edit_script");
        setUpViews();
    }

    void setUpViews() {
        BaseActivity.setToolbarAsBack(this, R.id.toolbar, getString(R.string.text_please_choose_a_script));
        initScriptListRecyclerView();
    }


    private void initScriptListRecyclerView() {
        ExplorerView explorerView = findViewById(R.id.script_list);
        explorerView.setExplorer(Explorers.external(), ExplorerDirPage.createRoot(Environment.getExternalStorageDirectory()));
        explorerView.setOnItemClickListener((view, item) -> {
            mSelectedScriptFilePath = item.getPath();
            finish();
        });
    }


    @Click(tag = "edit_script")
    void editPreExecuteScript() {
        TaskerScriptEditActivity.edit(this, getString(R.string.text_pre_execute_script), getString(R.string.summary_pre_execute_script), mPreExecuteScript == null ? "" : mPreExecuteScript);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            Explorers.external().refreshAll();
        } else if (item.getItemId() == R.id.action_clear_file_selection) {
            mSelectedScriptFilePath = null;
        } else {
            mPreExecuteScript = null;
        }
        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tasker_script_edit_menu, menu);
        return true;
    }


//    @Override
//    public boolean isBundleValid(@NonNull Bundle bundle) {
//        return ScriptIntents.isTaskerBundleValid(bundle);
//    }

//    @Override
//    public void onPostCreateWithPreviousResult(@NonNull Bundle bundle, @NonNull String s) {
//        mSelectedScriptFilePath = bundle.getString(ScriptIntents.EXTRA_KEY_PATH);
//        mPreExecuteScript = bundle.getString(ScriptIntents.EXTRA_KEY_PRE_EXECUTE_SCRIPT);
//    }

//    @Nullable
//    @Override
//    public Bundle getResultBundle() {
//        Bundle bundle = new Bundle();
//        bundle.putString(ScriptIntents.EXTRA_KEY_PATH, mSelectedScriptFilePath);
//        bundle.putString(ScriptIntents.EXTRA_KEY_PRE_EXECUTE_SCRIPT, mPreExecuteScript);
//        return bundle;
//    }

//    @NonNull
//    @Override
//    public String getResultBlurb(@NonNull Bundle bundle) {
//        String blurb = bundle.getString(ScriptIntents.EXTRA_KEY_PATH);
//        if (TextUtils.isEmpty(blurb)) {
//            blurb = bundle.getString(ScriptIntents.EXTRA_KEY_PRE_EXECUTE_SCRIPT);
//        }
//        if (TextUtils.isEmpty(blurb)) {
//            blurb = getString(R.string.text_path_is_empty);
//        }
//        return blurb;
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @NonNull Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            mPreExecuteScript = data.getStringExtra(EXTRA_CONTENT);
        }
    }

    @Override
    public boolean isJsonValid(@NonNull JSONObject jsonObject) {
        return false;
    }

    @Override
    public void onPostCreateWithPreviousResult(@NonNull JSONObject previousJsonObject, @NonNull String previousBlurb) {

    }

    @Nullable
    @Override
    public JSONObject getResultJson() {
        return null;
    }

    @NonNull
    @Override
    public String getResultBlurb(@NonNull JSONObject jsonObject) {
        return null;
    }
}
