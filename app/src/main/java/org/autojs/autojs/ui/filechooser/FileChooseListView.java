package org.autojs.autojs.ui.filechooser;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.stardust.pio.PFile;
import com.stardust.pio.PFiles;
import com.storyteller_f.bandage.Bandage;
import com.storyteller_f.bandage.Click;
import com.storyteller_f.bandage.OnCheckedChanged;

import org.autojs.autojs.R;
import org.autojs.autojs.databinding.FileChooseListDirectoryBinding;
import org.autojs.autojs.databinding.FileChooseListFileBinding;
import org.autojs.autojs.model.explorer.ExplorerItem;
import org.autojs.autojs.model.explorer.ExplorerPage;
import org.autojs.autojs.model.script.ScriptFile;
import org.autojs.autojs.ui.explorer.ExplorerViewHelper;
import org.autojs.autojs.ui.explorer.ExplorerView;
import org.autojs.autojs.ui.widget.BindableViewHolder;
import org.autojs.autojs.ui.widget.CheckBoxCompat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import androidx.recyclerview.widget.SimpleItemAnimator;

/**
 * Created by Stardust on 2017/10/19.
 */

public class FileChooseListView extends ExplorerView {

    private int mMaxChoice = 1;
    private final LinkedHashMap<PFile, Integer> mSelectedFiles = new LinkedHashMap<>();
    private boolean mCanChooseDir = false;

    public FileChooseListView(Context context) {
        super(context);
        init();
    }

    public FileChooseListView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setMaxChoice(int maxChoice) {
        mMaxChoice = maxChoice;
    }

    public void setCanChooseDir(boolean canChooseDir) {
        mCanChooseDir = canChooseDir;
    }

    public List<PFile> getSelectedFiles() {
        ArrayList<PFile> list = new ArrayList<>(mSelectedFiles.size());
        for (Map.Entry<PFile, Integer> entry : mSelectedFiles.entrySet()) {
            list.add(entry.getKey());
        }
        return list;
    }

    private void init() {
        ((SimpleItemAnimator) getExplorerItemListView().getItemAnimator())
                .setSupportsChangeAnimations(false);
    }

    @Override
    protected BindableViewHolder<?> onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            return new ExplorerItemViewHolder(inflater.inflate(R.layout.file_choose_list_file, parent, false));
        } else if (viewType == VIEW_TYPE_PAGE) {
            return new ExplorerPageViewHolder(inflater.inflate(R.layout.file_choose_list_directory, parent, false));
        } else {
            return super.onCreateViewHolder(inflater, parent, viewType);
        }
    }

    private void check(ScriptFile file, int position) {
        if (mSelectedFiles.size() == mMaxChoice) {
            Map.Entry<PFile, Integer> itemToUncheck = mSelectedFiles.entrySet().iterator().next();
            int positionOfItemToUncheck = itemToUncheck.getValue();
            mSelectedFiles.remove(itemToUncheck.getKey());
            getExplorerItemListView().getAdapter().notifyItemChanged(positionOfItemToUncheck);
        }
        mSelectedFiles.put(file, position);
    }


    class ExplorerItemViewHolder extends BindableViewHolder<ExplorerItem> {
        GradientDrawable mFirstCharBackground;

        private ExplorerItem mExplorerItem;
        private final FileChooseListFileBinding bind;

        ExplorerItemViewHolder(View itemView) {
            super(itemView);
            bind = FileChooseListFileBinding.bind(itemView);
            bind.item.setTag("item");
            bind.checkbox.setTag("checkbox");
            Bandage.bind(this,bind.getRoot());
            mFirstCharBackground = (GradientDrawable) bind.firstChar.getBackground();
        }

        @Override
        public void bind(ExplorerItem item, int position) {
            mExplorerItem = item;
            bind.name.setText(ExplorerViewHelper.getDisplayName(item));
            bind.desc.setText(PFiles.getHumanReadableSize(item.getSize()));
            bind.firstChar.setText(ExplorerViewHelper.getIconText(item));
            mFirstCharBackground.setColor(ExplorerViewHelper.getIconColor(item));
            bind.checkbox.setChecked(mSelectedFiles.containsKey(mExplorerItem.toScriptFile()), false);
        }

        @Click(tag = "item")
        void onItemClick() {
            bind.checkbox.toggle();
        }

        @OnCheckedChanged(tag = "checkbox")
        void onCheckedChanged() {
            if (bind.checkbox.isChecked()) {
                check(mExplorerItem.toScriptFile(), getAdapterPosition());
            } else {
                mSelectedFiles.remove(mExplorerItem.toScriptFile());
            }
        }


    }

    class ExplorerPageViewHolder extends BindableViewHolder<ExplorerPage> {

        private ExplorerPage mExplorerPage;
        private final FileChooseListDirectoryBinding bind;

        ExplorerPageViewHolder(View itemView) {
            super(itemView);
            bind = FileChooseListDirectoryBinding.bind(itemView);
            bind.item.setTag("item");
            bind.checkbox.setTag("checkbox");
            Bandage.bind(this,bind.getRoot());
            bind.checkbox.setVisibility(mCanChooseDir ? VISIBLE : GONE);
        }

        @Override
        public void bind(ExplorerPage data, int position) {
            mExplorerPage = data;
            bind.name.setText(ExplorerViewHelper.getDisplayName(data));
            bind.icon.setImageResource(ExplorerViewHelper.getIcon(data));
            if (mCanChooseDir) {
                bind.checkbox.setChecked(mSelectedFiles.containsKey(data.toScriptFile()), false);
            }
        }

        @Click(tag = "item")
        void onItemClick() {
            enterDirectChildPage(mExplorerPage);
        }

        @OnCheckedChanged(tag = "checkbox")
        void onCheckedChanged() {
            if (bind.checkbox.isChecked()) {
                check(mExplorerPage.toScriptFile(), getAdapterPosition());
            } else {
                mSelectedFiles.remove(mExplorerPage.toScriptFile());
            }
        }

    }

}
