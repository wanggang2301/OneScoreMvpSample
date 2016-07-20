package com.hhly.mlottery.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.hhly.mlottery.R;
import com.hhly.mlottery.adapter.basketball.SportsDialogAdapter;
import com.hhly.mlottery.adapter.football.TabsAdapter;
import com.hhly.mlottery.frame.footframe.BasketDatasaseHandicapFragment;
import com.hhly.mlottery.util.MDStatusBarCompat;
import com.hhly.mlottery.widget.ExactSwipeRefrashLayout;
import com.hhly.mlottery.widget.NoScrollListView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yixq
 * @date 2016/7/15 16:53
 * @des 篮球资料库详情
 */
public class BasketballDatabaseDetailsActivity extends AppCompatActivity implements AppBarLayout.OnOffsetChangedListener, View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    private String mThirdId = "936707";

    private ViewPager mViewPager;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private AppBarLayout appBarLayout;
    private TabLayout mTabLayout;
    private TabsAdapter mTabsAdapter;
    private Toolbar toolbar;
    private LinearLayout mBasketLayoutHeader;
    private CoordinatorLayout mCoordinatorLayout;

    private String[] TITLES;

    /**
     * 返回按钮
     */
    private ImageView mBack;
    /**
     * 收藏按钮
     */
    private TextView mCollect;

    LinearLayout headLayout;// 小头部

    private ExactSwipeRefrashLayout mRefreshLayout; //下拉刷新

    private BasketDatasaseHandicapFragment mBasketDatasaseHandicapFragment;
    private BasketDatasaseHandicapFragment mBasketDatasaseHandicapFragment2;
//    private BasketDatasaseHandicapFragment mBasketDatasaseHandicapFragment3;
//    private BasketDatasaseHandicapFragment mBasketDatasaseHandicapFragment4;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basketball_database_details);


        mBasketDatasaseHandicapFragment = BasketDatasaseHandicapFragment.newInstance("");
        mBasketDatasaseHandicapFragment2 = BasketDatasaseHandicapFragment.newInstance("");
//        mBasketDatasaseHandicapFragment3= BasketDatasaseHandicapFragment.newInstance("");
//        mBasketDatasaseHandicapFragment4 = BasketDatasaseHandicapFragment.newInstance("");

        initView();
        setListener();

    }

    public String getmThirdId() {
        return mThirdId;
    }

    /**
     * 初始化界面
     */
    private void initView() {
//        TITLES = new String[]{"赛程" , "排行" , "让分盘" ,"大小盘" ， "统计"};
        TITLES = new String[]{ "让分盘" ,"大小盘"};

        toolbar = (Toolbar) findViewById(R.id.basket_database_details_toolbar);
        setSupportActionBar(toolbar);

        mBasketLayoutHeader = (LinearLayout) findViewById(R.id.basket_database_details_img);
        mCollapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        mViewPager = (ViewPager) findViewById(R.id.basket_details_view_pager);
        appBarLayout = (AppBarLayout) findViewById(R.id.basket_database_details_appbar);
        mTabLayout = (TabLayout) findViewById(R.id.basket_database_details_tab_layout);
        mTabsAdapter = new TabsAdapter(getSupportFragmentManager());
        mTabsAdapter.setTitles(TITLES);

        MDStatusBarCompat.setCollapsingToolbar(this, mCoordinatorLayout, appBarLayout, mBasketLayoutHeader, toolbar);


        mTabsAdapter.addFragments(mBasketDatasaseHandicapFragment, mBasketDatasaseHandicapFragment2);
        mViewPager.setOffscreenPageLimit(2);//设置预加载页面的个数。
        mViewPager.setAdapter(mTabsAdapter);
        mTabLayout.setupWithViewPager(mViewPager);

        appBarLayout.addOnOffsetChangedListener(this);


        headLayout = (LinearLayout) findViewById(R.id.basket_database_details_header_layout);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                isHindShow(position);
                if (position == 4) {
                    appBarLayout.setExpanded(false);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mRefreshLayout = (ExactSwipeRefrashLayout) findViewById(R.id.basket_database_details_refresh_layout);
        mRefreshLayout.setColorSchemeResources(R.color.tabhost);
//        mRefreshLayout.setFocusable(true);
        mRefreshLayout.setOnRefreshListener(this);




        mBack = (ImageView) this.findViewById(R.id.basket_database_details_back);

//        mTitleScore = (RelativeLayout) this.findViewById(R.id.ll_basket_title_score);
        mCollect = (TextView) this.findViewById(R.id.basket_database_details_collect);

    }

    /**
     * 设置监听
     */
    private void setListener() {
        mBack.setOnClickListener(this);
        mCollect.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.basket_database_details_back:

                setResult(Activity.RESULT_OK);

                finish();
                overridePendingTransition(R.anim.push_fix_out, R.anim.push_left_out);
                break;
            case R.id.basket_database_details_collect:
                setDialog();
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            setResult(Activity.RESULT_OK);

            finish();
            overridePendingTransition(R.anim.push_fix_out, R.anim.push_left_out);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {

        if (mCollapsingToolbarLayout.getHeight() + verticalOffset < mBasketLayoutHeader.getHeight()) {
            mRefreshLayout.setEnabled(false);
        } else {
            mRefreshLayout.setEnabled(true);
        }
        if ((-verticalOffset) == appBarLayout.getTotalScrollRange()) {
            headLayout.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
        } else {
            headLayout.setBackgroundColor(getResources().getColor(R.color.transparency));
        }

    }

    /**
     * Fragment页面统计
     */
    private boolean isFragment0 = true;
    private boolean is0 = false;
    private boolean isFragment1 = false;
    private boolean is1 = false;
    private boolean isFragment2 = false;
    private boolean is2 = false;
    private boolean isFragment3 = false;
    private boolean is3 = false;
    private boolean isFragment4 = false;
    private boolean is4 = false;

    private void isHindShow(int position){
        switch (position) {
            case 0:// 分析
                isFragment0 = true;
                isFragment1 = false;
                isFragment2 = false;
                isFragment3 = false;
                isFragment4 = false;
                break;
            case 1:// 欧赔
                isFragment0 = false;
                isFragment1 = true;
                isFragment2 = false;
                isFragment3 = false;
                isFragment4 = false;
                break;
            case 2:// 亚盘
                isFragment0 = false;
                isFragment1 = false;
                isFragment2 = true;
                isFragment3 = false;
                isFragment4 = false;
                break;
            case 3:// 大小
                isFragment0 = false;
                isFragment1 = false;
                isFragment2 = false;
                isFragment3 = true;
                isFragment4 = false;
                break;
            case 4:// 聊球
                isFragment0 = false;
                isFragment1 = false;
                isFragment2 = false;
                isFragment3 = false;
                isFragment4 = true;
                break;
        }
        if (is0) {
            is0 = false;
        }
        if (is1) {
            is1 = false;
        }
        if (is2) {
            is2 = false;
        }
        if (is3) {
            is3 = false;
        }
        if (is4) {
            is4 = false;
        }

        if (isFragment0) {
            is0 = true;
        }
        if (isFragment1) {
            is1 = true;
        }
        if (isFragment2) {
            is2 = true;
        }
        if (isFragment3) {
            is3 = true;
        }
        if (isFragment4) {
            is4 = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isFragment0) {
            is0 = true;
        }
        if (isFragment1) {
            is1 = true;
        }
        if (isFragment2) {
            is2 = true;
        }
        if (isFragment3) {
            is3 = true;
        }
        if (isFragment4) {
            is4 = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (is0) {
            is0 = false;
        }
        if (is1) {
            is1 = false;
        }
        if (is2) {
            is2 = false;
        }
        if (is3) {
            is3 = false;
        }
        if (is4) {
            is4 = false;
        }
    }

    /**
     * 下拉刷新
     */
    @Override
    public void onRefresh() {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mRefreshLayout.setRefreshing(false);
                    Toast.makeText(BasketballDatabaseDetailsActivity.this, "aaaaa", Toast.LENGTH_SHORT).show();
                }
            }, 1000);
    }


    int currentDialogPosition = 0; // 当前选中的赛季（默认第一个）

    public void setDialog(){
        // Dialog 设置
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this, R.style.AlertDialog);
        LayoutInflater infla = LayoutInflater.from(this);
        View view = infla.inflate(R.layout.sports_alertdialog, null);
        TextView titleView = (TextView) view.findViewById(R.id.titleView);
        Button dataOk = (Button)view.findViewById(R.id.sports_btn_ok);
        titleView.setText("赛季");


        final List<String> data = new ArrayList<>();
        data.add("15-16");
        data.add("14-15");
        data.add("13-14");
        data.add("12-13");
        data.add("12-13");
        data.add("12-13");
        data.add("12-13");
        data.add("12-13");
        data.add("12-13");
        data.add("12-13");
        data.add("12-13");

        final SportsDialogAdapter mAdapter = new SportsDialogAdapter(data ,this , currentDialogPosition);

        final AlertDialog mAlertDialog = mBuilder.create();
        mAlertDialog.setCanceledOnTouchOutside(true);//设置空白处点击 dialog消失
        /**
         * 根据List数据条数加载不同的view （数据多加载可滑动View）
         */
        ScrollView scroll = (ScrollView)view.findViewById(R.id.basket_sports_scroll);//数据多时显示
        NoScrollListView scrollListview = (NoScrollListView) view.findViewById(R.id.sport_date_scroll);
        ListView listview = (ListView) view.findViewById(R.id.sport_date);//数据少时显示
        if (data.size() > 5) {
            scrollListview.setAdapter(mAdapter);
            scrollListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    currentDialogPosition = position;
                    mAdapter.updateDatas(currentDialogPosition);
                    mAdapter.notifyDataSetChanged();
                }
            });
            scroll.setVisibility(View.VISIBLE);
            listview.setVisibility(View.GONE);
            Toast.makeText(BasketballDatabaseDetailsActivity.this, "size " + data.size(), Toast.LENGTH_SHORT).show();
        }
        else{
            listview.setAdapter(mAdapter);
            listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    currentDialogPosition = position;
                    mAdapter.updateDatas(currentDialogPosition);
                    mAdapter.notifyDataSetChanged();
                }
            });
            scroll.setVisibility(View.GONE);
            listview.setVisibility(View.VISIBLE);
            Toast.makeText(BasketballDatabaseDetailsActivity.this, "size " + data.size(), Toast.LENGTH_SHORT).show();
        }

        dataOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAlertDialog.dismiss();
                Toast.makeText(BasketballDatabaseDetailsActivity.this, "确定 "+currentDialogPosition, Toast.LENGTH_SHORT).show();
            }
        });

        mAlertDialog.show();
        mAlertDialog.getWindow().setContentView(view);
    }
}
