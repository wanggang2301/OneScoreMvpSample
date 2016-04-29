package com.hhly.mlottery.adapter.cpiadapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.hhly.mlottery.R;

import java.util.List;
import java.util.Map;

/**
 * Created by 103TJL on 2016/4/11.
 * 选择公司的适配器
 */
public class CpiCompanyAdapter extends BaseAdapter {

    private List<Map<String, String>> cpiCompanyList;

    private Context context;
    private int defItem;//当前的item的position(用于点击item设置item背景颜色)
    private LayoutInflater mInflater;
    private ListView mListView;

    public CpiCompanyAdapter(Context context, List<Map<String, String>> cpiCompanyList, ListView mListView) {
        super();
        this.context = context;
        this.cpiCompanyList = cpiCompanyList;
        this.mListView = mListView;
        this.mInflater = LayoutInflater.from(context);
//        this.mCheckedTextView = mCheckedTextView;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return cpiCompanyList.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return cpiCompanyList.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    /**
     * 根据选中的position更改item背景颜色
     */
//    public void setDefSelect( int position) {
//        this.defItem = position;
//        notifyDataSetChanged();
//    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ListViewItem item;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_dialog_company, null);
            item = new ListViewItem(convertView);
            convertView.setTag(item);
        } else {
            item = (ListViewItem) convertView.getTag();
        }
        item.checkedTextView.setText(cpiCompanyList.get(position).get("date"));
        //默认皇冠和浩博
        if (position == 0) {
            mListView.setItemChecked(position, true);
            item.checkedTextView.setChecked(true);
        }
        if (position == 1) {
            mListView.setItemChecked(position, true);
            item.checkedTextView.setChecked(true);
        }

        return convertView;
    }

    private static class ListViewItem {
        public CheckedTextView checkedTextView;

        public ListViewItem(View v) {
            checkedTextView = (CheckedTextView) v.findViewById(R.id.item_checkedTextView);
        }
    }
}
