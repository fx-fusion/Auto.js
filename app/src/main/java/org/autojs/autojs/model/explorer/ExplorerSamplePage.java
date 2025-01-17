package org.autojs.autojs.model.explorer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stardust.pio.PFile;

import java.io.File;

public class ExplorerSamplePage extends ExplorerDirPage {

    private boolean mRoot = false;

    public ExplorerSamplePage(PFile file, ExplorerPage parent) {
        super(file, parent);
    }

    public ExplorerSamplePage(String path, ExplorerPage parent) {
        super(path, parent);
    }

    public ExplorerSamplePage(@NonNull File file, ExplorerPage parent) {
        super(file, parent);
    }

    @Nullable
    public static ExplorerSamplePage createRoot(PFile dir) {
        ExplorerSamplePage page = new ExplorerSamplePage(dir, null);
        page.mRoot = true;
        return page;
    }

    public boolean isRoot() {
        return mRoot;
    }

}
