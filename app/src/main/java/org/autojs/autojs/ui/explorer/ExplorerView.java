package org.autojs.autojs.ui.explorer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.stardust.pio.PFiles;

import org.autojs.autojs.R;
import org.autojs.autojs.databinding.ScriptFileListCategoryBinding;
import org.autojs.autojs.databinding.ScriptFileListDirectoryBinding;
import org.autojs.autojs.databinding.ScriptFileListFileBinding;
import org.autojs.autojs.model.explorer.Explorer;
import org.autojs.autojs.model.explorer.ExplorerChangeEvent;
import org.autojs.autojs.model.explorer.ExplorerDirPage;
import org.autojs.autojs.model.explorer.ExplorerFileItem;
import org.autojs.autojs.model.explorer.ExplorerItem;
import org.autojs.autojs.model.explorer.ExplorerPage;
import org.autojs.autojs.model.explorer.ExplorerProjectPage;
import org.autojs.autojs.model.explorer.ExplorerSampleItem;
import org.autojs.autojs.model.explorer.ExplorerSamplePage;
import org.autojs.autojs.model.explorer.Explorers;
import org.autojs.autojs.model.script.ScriptFile;
import org.autojs.autojs.model.script.Scripts;
import org.autojs.autojs.theme.widget.ThemeColorSwipeRefreshLayout;
import org.autojs.autojs.tool.Observers;
import org.autojs.autojs.ui.common.ScriptLoopDialog;
import org.autojs.autojs.ui.common.ScriptOperations;
import org.autojs.autojs.ui.project.BuildActivity;
import org.autojs.autojs.ui.viewmodel.ExplorerItemList;
import org.autojs.autojs.ui.widget.BindableViewHolder;
import org.autojs.autojs.workground.WrapContentGridLayoutManger;
import org.greenrobot.eventbus.Subscribe;

import java.util.Objects;
import java.util.Stack;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Stardust on 2017/8/21.
 */

public class ExplorerView extends ThemeColorSwipeRefreshLayout implements SwipeRefreshLayout.OnRefreshListener, PopupMenu.OnMenuItemClickListener {

    protected static final int VIEW_TYPE_ITEM = 0;
    protected static final int VIEW_TYPE_PAGE = 1;
    //category是类别，也即"文件", "文件夹"那两个
    protected static final int VIEW_TYPE_CATEGORY = 2;
    private static final String LOG_TAG = "ExplorerView";
    private static final int positionOfCategoryDir = 0;
    private final ExplorerAdapter mExplorerAdapter = new ExplorerAdapter();
    private final Stack<ExplorerPageState> mPageStateHistory = new Stack<>();
    protected OnItemClickListener mOnItemClickListener;
    protected ExplorerItem mSelectedItem;
    @NonNull
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    private ExplorerItemList mExplorerItemList = new ExplorerItemList();
    private RecyclerView mExplorerItemListView;
    private ExplorerProjectToolbar mProjectToolbar;
    private Function<ExplorerItem, Boolean> mFilter;
    private OnItemOperatedListener mOnItemOperatedListener;
    private Explorer mExplorer;
    @NonNull
    private ExplorerPageState mCurrentPageState = new ExplorerPageState();
    private boolean mDirSortMenuShowing = false;
    private int mDirectorySpanSize = 2;
    public ExplorerView(Context context) {
        super(context);
        init();
    }

    public ExplorerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ExplorerPage getCurrentPage() {
        return mCurrentPageState.page;
    }

    public void setRootPage(ExplorerPage page) {
        mPageStateHistory.clear();
        setCurrentPageState(new ExplorerPageState(page));
        loadItemList();
    }

    private void setCurrentPageState(@NonNull ExplorerPageState currentPageState) {
        mCurrentPageState = currentPageState;
        if (mCurrentPageState.page instanceof ExplorerProjectPage) {
            mProjectToolbar.setVisibility(VISIBLE);
            mProjectToolbar.setProject(currentPageState.page.toScriptFile());
        } else {
            mProjectToolbar.setVisibility(GONE);
        }
    }

    protected void enterDirectChildPage(ExplorerPage childItemGroup) {
        mCurrentPageState.scrollY = ((LinearLayoutManager) Objects.requireNonNull(mExplorerItemListView.getLayoutManager())).findLastCompletelyVisibleItemPosition();
        mPageStateHistory.push(mCurrentPageState);
        setCurrentPageState(new ExplorerPageState(childItemGroup));
        loadItemList();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public ExplorerItemList.SortConfig getSortConfig() {
        return mExplorerItemList.getSortConfig();
    }

    public void setSortConfig(ExplorerItemList.SortConfig sortConfig) {
        mExplorerItemList.setSortConfig(sortConfig);
    }

    public void setExplorer(Explorer explorer, ExplorerPage rootPage) {
        if (mExplorer != null)
            mExplorer.unregisterChangeListener(this);
        mExplorer = explorer;
        setRootPage(rootPage);
        mExplorer.registerChangeListener(this);
    }

    public void setExplorer(Explorer explorer, ExplorerPage rootPage, @NonNull ExplorerPage currentPage) {
        if (mExplorer != null)
            mExplorer.unregisterChangeListener(this);
        mExplorer = explorer;
        mPageStateHistory.clear();
        setCurrentPageState(new ExplorerPageState(rootPage));
        mExplorer.registerChangeListener(this);
        enterChildPage(currentPage);
    }

    public void enterChildPage(@NonNull ExplorerPage childPage) {
        ScriptFile root = mCurrentPageState.page.toScriptFile();
        ScriptFile dir = childPage.toScriptFile();
        Stack<ScriptFile> dirs = new Stack<>();
        while (!dir.equals(root)) {
            dir = dir.getParentFile();
            if (dir == null) {
                break;
            }
            dirs.push(dir);
        }
        ExplorerDirPage parent = null;
        while (!dirs.empty()) {
            dir = dirs.pop();
            ExplorerDirPage dirPage = new ExplorerDirPage(dir, parent);
            mPageStateHistory.push(new ExplorerPageState(dirPage));
            parent = dirPage;
        }
        setCurrentPageState(new ExplorerPageState(childPage));
        loadItemList();
    }

    public void setOnItemOperatedListener(OnItemOperatedListener onItemOperatedListener) {
        mOnItemOperatedListener = onItemOperatedListener;
    }

    public boolean canGoBack() {
        return !mPageStateHistory.empty();
    }

    public void goBack() {
        setCurrentPageState(mPageStateHistory.pop());
        loadItemList();
    }

    public void setDirectorySpanSize(int directorySpanSize) {
        mDirectorySpanSize = directorySpanSize;
    }

    public void setFilter(Function<ExplorerItem, Boolean> filter) {
        mFilter = filter;
        reload();
    }

    public void reload() {
        loadItemList();
    }

    private void init() {
        Log.d(LOG_TAG, "item bg = " + Integer.toHexString(ContextCompat.getColor(getContext(), R.color.item_background)));
        setOnRefreshListener(this);
        inflate(getContext(), R.layout.explorer_view, this);
        mExplorerItemListView = findViewById(R.id.explorer_item_list);
        mProjectToolbar = findViewById(R.id.project_toolbar);
        initExplorerItemListView();
    }

    private void initExplorerItemListView() {
        mExplorerItemListView.setAdapter(mExplorerAdapter);
        WrapContentGridLayoutManger manager = new WrapContentGridLayoutManger(getContext(), 2);
        manager.setDebugInfo("ExplorerView");
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                //For directories
                if (position > positionOfCategoryDir && position < positionOfCategoryFile()) {
                    return mDirectorySpanSize;
                }
                //For files and category
                return 2;
            }
        });
        mExplorerItemListView.setLayoutManager(manager);
    }

    private int positionOfCategoryFile() {
        if (mCurrentPageState.dirsCollapsed)
            return 1;
        return mExplorerItemList.groupCount() + 1;
    }

    @SuppressLint("CheckResult")
    private void loadItemList() {
        setRefreshing(true);
        Disposable subscribe = mExplorer.fetchChildren(mCurrentPageState.page)
                .subscribeOn(Schedulers.io())
                .flatMapObservable(page -> {
                    mCurrentPageState.page = page;
                    return Observable.fromIterable(page);
                })
                .filter(f -> mFilter == null || mFilter.apply(f))
                .collectInto(mExplorerItemList.cloneConfig(), ExplorerItemList::add)
                .observeOn(Schedulers.computation())
                .doOnSuccess(ExplorerItemList::sort)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    mExplorerItemList = list;
                    mExplorerAdapter.notifyDataSetChanged();
                    setRefreshing(false);
                    post(() ->
                            mExplorerItemListView.scrollToPosition(mCurrentPageState.scrollY)
                    );
                });
        compositeDisposable.add(subscribe);
    }

    @Subscribe
    public void onExplorerChange(@NonNull ExplorerChangeEvent event) {
        Log.d(LOG_TAG, "on explorer change: " + event);
        if ((event.getAction() == ExplorerChangeEvent.ALL)) {
            loadItemList();
            return;
        }
        String currentDirPath = mCurrentPageState.page.getPath();
        String changedDirPath = event.getPage().getPath();
        ExplorerItem item = event.getItem();
        String changedItemPath = item == null ? null : item.getPath();
        if (currentDirPath.equals(changedItemPath) || (currentDirPath.equals(changedDirPath) &&
                event.getAction() == ExplorerChangeEvent.CHILDREN_CHANGE)) {
            loadItemList();
            return;
        }
        if (currentDirPath.equals(changedDirPath)) {
            int i;
            switch (event.getAction()) {
                case ExplorerChangeEvent.CHANGE:
                    i = mExplorerItemList.update(item, event.getNewItem());
                    if (i >= 0) {
                        mExplorerAdapter.notifyItemChanged(item, i);
                    }
                    break;
                case ExplorerChangeEvent.CREATE:
                    mExplorerItemList.insertAtFront(event.getNewItem());
                    mExplorerAdapter.notifyItemInserted(event.getNewItem(), 0);
                    break;
                case ExplorerChangeEvent.REMOVE:
                    i = mExplorerItemList.remove(item);
                    if (i >= 0) {
                        mExplorerAdapter.notifyItemRemoved(item, i);
                    }
                    break;
            }
        }
    }

    @Override
    public void onRefresh() {
        mExplorer.notifyChildrenChanged(mCurrentPageState.page);
        mProjectToolbar.refresh();
    }

    public ScriptFile getCurrentDirectory() {
        return getCurrentPage().toScriptFile();
    }

    @Override
    public boolean onMenuItemClick(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.rename) {
            new ScriptOperations(getContext(), this, getCurrentPage())
                    .rename((ExplorerFileItem) mSelectedItem)
                    .subscribe(Observers.emptyObserver());
        } else if (itemId == R.id.delete) {
            new ScriptOperations(getContext(), this, getCurrentPage())
                    .delete(mSelectedItem.toScriptFile());
        } else if (itemId == R.id.run_repeatedly) {
            new ScriptLoopDialog(getContext(), mSelectedItem.toScriptFile())
                    .show();
            notifyOperated();
        } else if (itemId == R.id.create_shortcut) {
            new ScriptOperations(getContext(), this, getCurrentPage())
                    .createShortcut(mSelectedItem.toScriptFile());
        } else if (itemId == R.id.open_by_other_apps) {
            Scripts.INSTANCE.openByOtherApps(mSelectedItem.toScriptFile());
            notifyOperated();
        } else if (itemId == R.id.send) {
            Scripts.INSTANCE.send(mSelectedItem.toScriptFile());
            notifyOperated();
        } else if (itemId == R.id.timed_task) {
            new ScriptOperations(getContext(), this, getCurrentPage())
                    .timedTask(mSelectedItem.toScriptFile());
            notifyOperated();
        } else if (itemId == R.id.action_build_apk) {
            BuildActivity.intent(getContext())
                    .extra(BuildActivity.EXTRA_SOURCE, mSelectedItem.getPath())
                    .start();
            notifyOperated();
        } else if (itemId == R.id.action_sort_by_date) {
            sort(ExplorerItemList.SORT_TYPE_DATE, mDirSortMenuShowing);
        } else if (itemId == R.id.action_sort_by_type) {
            sort(ExplorerItemList.SORT_TYPE_TYPE, mDirSortMenuShowing);
        } else if (itemId == R.id.action_sort_by_name) {
            sort(ExplorerItemList.SORT_TYPE_NAME, mDirSortMenuShowing);
        } else if (itemId == R.id.action_sort_by_size) {
            sort(ExplorerItemList.SORT_TYPE_SIZE, mDirSortMenuShowing);
        } else if (itemId == R.id.reset) {
            Disposable subscribe = Explorers.Providers.workspace().resetSample(mSelectedItem.toScriptFile())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(ignored -> Snackbar.make(this, R.string.text_reset_succeed, Snackbar.LENGTH_SHORT).show(), Observers.toastMessage());
            compositeDisposable.add(subscribe);
        } else {
            return false;
        }
        return true;
    }

    protected void notifyOperated() {
        if (mOnItemOperatedListener != null) {
            mOnItemOperatedListener.OnItemOperated(mSelectedItem);
        }
    }

    private void sort(final int sortType, final boolean isDir) {
        setRefreshing(true);
        Disposable subscribe = Observable.fromCallable(() -> {
                    if (isDir) {
                        mExplorerItemList.sortItemGroup(sortType);
                    } else {
                        mExplorerItemList.sortFile(sortType);
                    }
                    return mExplorerItemList;
                }).subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(o -> {
                    mExplorerAdapter.notifyDataSetChanged();
                    setRefreshing(false);
                });
        compositeDisposable.add(subscribe);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mExplorer != null)
            mExplorer.registerChangeListener(this);

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mExplorer.unregisterChangeListener(this);
    }

    protected BindableViewHolder<?> onCreateViewHolder(@NonNull LayoutInflater inflater, ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            return new ExplorerItemViewHolder(inflater.inflate(R.layout.script_file_list_file, parent, false));
        } else if (viewType == VIEW_TYPE_PAGE) {
            return new ExplorerPageViewHolder(inflater.inflate(R.layout.script_file_list_directory, parent, false));
        } else {
            return new CategoryViewHolder(inflater.inflate(R.layout.script_file_list_category, parent, false));
        }
    }

    protected RecyclerView getExplorerItemListView() {
        return mExplorerItemListView;
    }


    public interface OnItemClickListener {
        void onItemClick(View view, ExplorerItem item);
    }

    public interface OnItemOperatedListener {
        void OnItemOperated(ExplorerItem item);
    }

    private static class ExplorerPageState {

        ExplorerPage page;

        boolean dirsCollapsed;

        boolean filesCollapsed;

        int scrollY;

        ExplorerPageState() {
        }

        ExplorerPageState(ExplorerPage page) {
            this.page = page;
        }
    }

    private class ExplorerAdapter extends RecyclerView.Adapter<BindableViewHolder<?>> {

        @Override
        @NonNull
        public BindableViewHolder<?> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            return ExplorerView.this.onCreateViewHolder(inflater, parent, viewType);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onBindViewHolder(@NonNull BindableViewHolder<?> holder, int position) {
            int positionOfCategoryFile = positionOfCategoryFile();
            BindableViewHolder bindableViewHolder = holder;
            if (position == positionOfCategoryDir || position == positionOfCategoryFile) {
                bindableViewHolder.bind(position == positionOfCategoryDir, position);
                return;
            }
            if (position < positionOfCategoryFile) {
                bindableViewHolder.bind(mExplorerItemList.getItemGroup(position - 1), position);
                return;
            }
            bindableViewHolder.bind(mExplorerItemList.getItem(position - positionOfCategoryFile - 1), position);
        }

        @Override
        public int getItemViewType(int position) {
            int positionOfCategoryFile = positionOfCategoryFile();
            if (position == positionOfCategoryDir || position == positionOfCategoryFile) {
                return VIEW_TYPE_CATEGORY;
            } else if (position < positionOfCategoryFile) {
                return VIEW_TYPE_PAGE;
            } else {
                return VIEW_TYPE_ITEM;
            }
        }

        int getItemPosition(ExplorerItem item, int i) {
            if (item instanceof ExplorerPage) {
                return i + positionOfCategoryDir + 1;
            }
            return i + positionOfCategoryFile() + 1;
        }

        public void notifyItemChanged(ExplorerItem item, int i) {
            notifyItemChanged(getItemPosition(item, i));
        }

        public void notifyItemRemoved(ExplorerItem item, int i) {
            notifyItemRemoved(getItemPosition(item, i));
        }

        public void notifyItemInserted(ExplorerItem item, int i) {
            notifyItemInserted(getItemPosition(item, i));
        }

        @Override
        public int getItemCount() {
            int count = 0;
            if (!mCurrentPageState.dirsCollapsed) {
                count += mExplorerItemList.groupCount();
            }
            if (!mCurrentPageState.filesCollapsed) {
                count += mExplorerItemList.itemCount();
            }
            return count + 2;
        }
    }

    protected class ExplorerItemViewHolder extends BindableViewHolder<ExplorerItem> {

        @NonNull
        private final ScriptFileListFileBinding bind;
        GradientDrawable mFirstCharBackground;
        private ExplorerItem mExplorerItem;

        ExplorerItemViewHolder(@NonNull View itemView) {
            super(itemView);
            bind = ScriptFileListFileBinding.bind(itemView);
            mFirstCharBackground = (GradientDrawable) bind.firstChar.getBackground();
            bind.item.setOnClickListener(view -> onItemClick());
            bind.run.setOnClickListener(view -> run());
            bind.edit.setOnClickListener(view -> edit());
            bind.more.setOnClickListener(view -> showOptionMenu());
        }

        @Override
        public void bind(@NonNull ExplorerItem item, int position) {
            mExplorerItem = item;
            bind.name.setText(ExplorerViewHelper.getDisplayName(item));
            bind.desc.setText(PFiles.getHumanReadableSize(item.getSize()));
            bind.firstChar.setText(ExplorerViewHelper.getIconText(item));
            mFirstCharBackground.setColor(ExplorerViewHelper.getIconColor(item));
            bind.edit.setVisibility(item.isEditable() ? VISIBLE : GONE);
            bind.run.setVisibility(item.isExecutable() ? VISIBLE : GONE);
        }

        void onItemClick() {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(itemView, mExplorerItem);
            }
            notifyOperated();
        }

        void run() {
            Scripts.INSTANCE.run(new ScriptFile(mExplorerItem.getPath()));
            notifyOperated();
        }

        void edit() {
            Scripts.INSTANCE.edit(getContext(), new ScriptFile(mExplorerItem.getPath()));
            notifyOperated();
        }

        void showOptionMenu() {
            mSelectedItem = mExplorerItem;
            PopupMenu popupMenu = new PopupMenu(getContext(), bind.more);
            popupMenu.inflate(R.menu.menu_script_options);
            Menu menu = popupMenu.getMenu();
            if (!mExplorerItem.isExecutable()) {
                menu.removeItem(R.id.run_repeatedly);
                menu.removeItem(R.id.more);
            }
            if (!mExplorerItem.canDelete()) {
                menu.removeItem(R.id.delete);
            }
            if (!mExplorerItem.canRename()) {
                menu.removeItem(R.id.rename);
            }
            if (!(mExplorerItem instanceof ExplorerSampleItem)) {
                menu.removeItem(R.id.reset);
            }
            popupMenu.setOnMenuItemClickListener(ExplorerView.this);
            popupMenu.show();
        }
    }

    protected class ExplorerPageViewHolder extends BindableViewHolder<ExplorerPage> {
        @NonNull
        private final ScriptFileListDirectoryBinding bind;
        private ExplorerPage mExplorerPage;

        ExplorerPageViewHolder(@NonNull View itemView) {
            super(itemView);
            bind = ScriptFileListDirectoryBinding.bind(itemView);
            bind.item.setOnClickListener(view -> onItemClick());
            bind.more.setOnClickListener(view -> showOptionMenu());

        }

        @Override
        public void bind(ExplorerPage data, int position) {
            bind.name.setText(ExplorerViewHelper.getDisplayName(data));
            bind.icon.setImageResource(ExplorerViewHelper.getIcon(data));
            bind.more.setVisibility(data instanceof ExplorerSamplePage ? GONE : VISIBLE);
            mExplorerPage = data;

        }

        void onItemClick() {
            enterDirectChildPage(mExplorerPage);
        }

        void showOptionMenu() {
            mSelectedItem = mExplorerPage;
            PopupMenu popupMenu = new PopupMenu(getContext(), bind.more);
            popupMenu.inflate(R.menu.menu_dir_options);
            popupMenu.setOnMenuItemClickListener(ExplorerView.this);
            popupMenu.show();
        }
    }

    class CategoryViewHolder extends BindableViewHolder<Boolean> {

        @NonNull
        private final ScriptFileListCategoryBinding bind;
        private boolean mIsDir;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            bind = ScriptFileListCategoryBinding.bind(itemView);
            bind.order.setOnClickListener(view -> changeSortOrder());
            bind.sort.setOnClickListener(view -> showSortOptions());
            bind.back.setOnClickListener(view -> back());
            bind.titleContainer.setOnClickListener(view -> collapseOrExpand());
        }

        @Override
        public void bind(Boolean isDirCategory, int position) {
            bind.title.setText(isDirCategory ? R.string.text_directory : R.string.text_file);
            mIsDir = isDirCategory;
            if (isDirCategory && canGoBack()) {
                bind.back.setVisibility(VISIBLE);
            } else {
                bind.back.setVisibility(GONE);
            }
            if (isDirCategory) {
                bind.collapse.setRotation(mCurrentPageState.dirsCollapsed ? -90 : 0);
                bind.order.setImageResource(mExplorerItemList.isDirSortedAscending() ?
                        R.drawable.ic_ascending_order : R.drawable.ic_descending_order);
            } else {
                bind.collapse.setRotation(mCurrentPageState.filesCollapsed ? -90 : 0);
                bind.order.setImageResource(mExplorerItemList.isFileSortedAscending() ?
                        R.drawable.ic_ascending_order : R.drawable.ic_descending_order);
            }
        }

        void changeSortOrder() {
            if (mIsDir) {
                bind.order.setImageResource(mExplorerItemList.isDirSortedAscending() ?
                        R.drawable.ic_ascending_order : R.drawable.ic_descending_order);
                mExplorerItemList.setDirSortedAscending(!mExplorerItemList.isDirSortedAscending());
                sort(mExplorerItemList.getDirSortType(), mIsDir);
            } else {
                bind.order.setImageResource(mExplorerItemList.isFileSortedAscending() ?
                        R.drawable.ic_ascending_order : R.drawable.ic_descending_order);
                mExplorerItemList.setFileSortedAscending(!mExplorerItemList.isFileSortedAscending());
                sort(mExplorerItemList.getFileSortType(), mIsDir);
            }
        }

        void showSortOptions() {
            PopupMenu popupMenu = new PopupMenu(getContext(), bind.sort);
            popupMenu.inflate(R.menu.menu_sort_options);
            popupMenu.setOnMenuItemClickListener(ExplorerView.this);
            mDirSortMenuShowing = mIsDir;
            popupMenu.show();

        }

        void back() {
            if (canGoBack()) {
                goBack();
            }
        }

        void collapseOrExpand() {
            if (mIsDir) {
                mCurrentPageState.dirsCollapsed = !mCurrentPageState.dirsCollapsed;
            } else {
                mCurrentPageState.filesCollapsed = !mCurrentPageState.filesCollapsed;
            }
            mExplorerAdapter.notifyDataSetChanged();
        }
    }
}
