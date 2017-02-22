package com.hhly.mlottery.frame.tennisfrag;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hhly.mlottery.R;
import com.hhly.mlottery.adapter.tennisball.TennisBallScoreAdapter;
import com.hhly.mlottery.bean.tennisball.MatchDataBean;
import com.hhly.mlottery.bean.tennisball.TennisBallBean;
import com.hhly.mlottery.config.BaseURLs;
import com.hhly.mlottery.config.StaticValues;
import com.hhly.mlottery.util.DateUtil;
import com.hhly.mlottery.util.DisplayUtil;
import com.hhly.mlottery.util.ToastTools;
import com.hhly.mlottery.util.net.VolleyContentFast;
import com.hhly.mlottery.widget.ExactSwipeRefreshLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * desc:网球比分列表Tab页面
 * Created by 107_tangrr on 2017/2/20 0020.
 */

public class TennisBallTabFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener {

    private static final int LOADING = 1;
    private static final int SUCCESS = 2;
    private static final int SUCCESS_AGAIN = 3;
    private static final int ERROR = 4;
    private static final String TENNIS_TYPE = "tennis_type";
    private int type;

    private Context mContext;
    private View mView;
    private RecyclerView tennis_recycler;
    private TextView tv_date;
    private List<MatchDataBean> mData = new ArrayList<>();
    private ArrayList<String> dates = new ArrayList<>();
    private ExactSwipeRefreshLayout swipeRefreshLayout;
    private TennisBallScoreAdapter mAdapter;
    private TennisDateChooseDialogFragment tennisDateChooseDialogFragment;

    private String currentData;
    private String tennisUrl;

    public static TennisBallTabFragment newInstance(int type) {
        TennisBallTabFragment fragment = new TennisBallTabFragment();
        Bundle args = new Bundle();
        args.putInt(TENNIS_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            type = getArguments().getInt(TENNIS_TYPE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mContext = getActivity();
        mView = View.inflate(mContext, R.layout.fragment_tennls_ball_tab, null);

        initView();
        initData(0);

        return mView;
    }

    private void initData(final int start) {

        mHandler.sendEmptyMessage(LOADING);

        if (type == 1) {
            tennisUrl = BaseURLs.TENNIS_RESULT_URL;
        } else if (type == 2) {
            tennisUrl = BaseURLs.TENNIS_SCHEDULE_URL;
        }

        Map<String, String> map = new HashMap<>();
        if (currentData != null) {
            map.put("date", currentData);
        }


        VolleyContentFast.requestJsonByGet(tennisUrl, map, new VolleyContentFast.ResponseSuccessListener<TennisBallBean>() {
            @Override
            public void onResponse(TennisBallBean jsonObject) {
                if (jsonObject.getResult() == 200) {
                    mData.clear();
                    dates.clear();
                    mData.addAll(jsonObject.getData().getMatchData());
                    dates.addAll(jsonObject.getData().getDates());
                    if (mAdapter == null) {
                        mAdapter = new TennisBallScoreAdapter(R.layout.item_tennis_score, mData, type);
                        tennis_recycler.setAdapter(mAdapter);
                    } else {
                        mAdapter.notifyDataSetChanged();
                    }
                    if (start == 0) {
                        mHandler.sendEmptyMessage(SUCCESS);
                    } else if (start == 1) {
                        mHandler.sendEmptyMessage(SUCCESS_AGAIN);
                    }
                } else {
                    mHandler.sendEmptyMessage(ERROR);
                }
            }
        }, new VolleyContentFast.ResponseErrorListener() {
            @Override
            public void onErrorResponse(VolleyContentFast.VolleyException exception) {
                mHandler.sendEmptyMessage(ERROR);
            }
        }, TennisBallBean.class);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LOADING:
                    swipeRefreshLayout.setRefreshing(true);
                    break;
                case SUCCESS:
                    swipeRefreshLayout.setRefreshing(false);
                    settingData(mData.size() == 0 ? "" : mData.get(0).getDate());
                    break;
                case SUCCESS_AGAIN:
                    swipeRefreshLayout.setRefreshing(false);
                    settingData(currentData);
                    break;
                case ERROR:
                    swipeRefreshLayout.setRefreshing(false);
                    ToastTools.showQuickCenter(mContext, "请求失败！");
                    break;
            }
        }
    };

    private void initView() {
        tennis_recycler = (RecyclerView) mView.findViewById(R.id.tennis_recycler);
        tv_date = (TextView) mView.findViewById(R.id.tv_date);
        tv_date.setOnClickListener(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        layoutManager.setOrientation(OrientationHelper.VERTICAL);
        tennis_recycler.setLayoutManager(layoutManager);
        swipeRefreshLayout = (ExactSwipeRefreshLayout) mView.findViewById(R.id.tennis_swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.bg_header);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setProgressViewOffset(false, 0, DisplayUtil.dip2px(getContext(), StaticValues.REFRASH_OFFSET_END));
    }

    @Override
    public void onRefresh() {
        initData(1);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_date:// 日期选择
                dateChooseShow();
                break;
        }
    }

    private void dateChooseShow() {
        tennisDateChooseDialogFragment = TennisDateChooseDialogFragment.newInstance(dates, currentData, new TennisDateChooseDialogFragment.OnDateChooseListener() {
            @Override
            public void onDateChoose(String date) {
                if (TextUtils.isEmpty(currentData) || !currentData.equals(date)) {
                    settingData(date);
                    initData(1);
                }
            }
        });
        if (!tennisDateChooseDialogFragment.isVisible()) {
            tennisDateChooseDialogFragment.show(getChildFragmentManager(), "tennisDateChooseDialogFragment");
        }
    }

    // 设置日期
    private void settingData(String data) {
        currentData = data;
        try {
            tv_date.setText(data + " " + DateUtil.getLotteryWeekOfDate(DateUtil.parseDate(data)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshData() {
        if(mAdapter != null){
            mAdapter.notifyDataSetChanged();
        }
    }
}
