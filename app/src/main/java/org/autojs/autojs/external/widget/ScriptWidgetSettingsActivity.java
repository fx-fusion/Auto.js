package org.autojs.autojs.external.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.autojs.autojs.R;
import org.autojs.autojs.databinding.ActivityScriptWidgetSettingsBinding;
import org.autojs.autojs.model.explorer.Explorer;
import org.autojs.autojs.model.explorer.ExplorerDirPage;
import org.autojs.autojs.model.explorer.ExplorerFileProvider;
import org.autojs.autojs.model.script.Scripts;
import org.autojs.autojs.ui.BaseActivity;
import org.autojs.autojs.ui.explorer.ExplorerView;

/**
 * Created by Stardust on 2017/7/11.
 */
public class ScriptWidgetSettingsActivity extends BaseActivity {

    @Nullable
    private String mSelectedScriptFilePath;
    private Explorer mExplorer;
    private int mAppWidgetId;
    private ActivityScriptWidgetSettingsBinding inflate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inflate = ActivityScriptWidgetSettingsBinding.inflate(getLayoutInflater());
        setContentView(inflate.getRoot());
        setUpViews();
        mAppWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    void setUpViews() {
        BaseActivity.setToolbarAsBack(this, R.id.toolbar, getString(R.string.text_please_choose_a_script));
        initScriptListRecyclerView();
    }


    private void initScriptListRecyclerView() {
        mExplorer = new Explorer(new ExplorerFileProvider(Scripts.INSTANCE.getFILE_FILTER()), 0);
        ExplorerView explorerView = findViewById(R.id.script_list);
        explorerView.setExplorer(mExplorer, ExplorerDirPage.createRoot(Environment.getExternalStorageDirectory()));
        explorerView.setOnItemClickListener((view, file) -> {
            mSelectedScriptFilePath = file.getPath();
            finish();
        });
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            mExplorer.refreshAll();
        } else if (item.getItemId() == R.id.action_clear_file_selection) {
            mSelectedScriptFilePath = null;
        }
        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.script_widget_settings_menu, menu);
        return true;
    }

    @Override
    public void finish() {
        if (ScriptWidget.updateWidget(this, mAppWidgetId, mSelectedScriptFilePath)) {
            setResult(RESULT_OK, new Intent()
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));

        } else {
            setResult(RESULT_CANCELED, new Intent()
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId));
        }
        super.finish();
    }


}
