package org.autojs.autojs.ui.main.drawer;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.autojs.autojs.R;
import org.autojs.autojs.ui.widget.BindableViewHolder;

/**
 * Created by Stardust on 2017/12/10.
 */

public class DrawerMenuGroupViewHolder extends BindableViewHolder<DrawerMenuItem> {

    private final TextView mTextView;

    public DrawerMenuGroupViewHolder(@NonNull View itemView) {
        super(itemView);
        mTextView = itemView.findViewById(R.id.title);
    }

    @Override
    public void bind(@NonNull DrawerMenuItem data, int position) {
        mTextView.setText(data.getTitle());
        int padding = itemView.getResources().getDimensionPixelOffset(R.dimen.divider_drawer_menu_group);
        itemView.setPadding(0, position == 0 ? 0 : padding, 0, 0);
    }
}
