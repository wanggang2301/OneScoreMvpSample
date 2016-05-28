package com.hhly.mlottery.frame.footframe;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.hhly.mlottery.R;
import com.hhly.mlottery.activity.FiltrateMatchConfigActivity;
import com.hhly.mlottery.activity.FootballMatchDetailActivity;
import com.hhly.mlottery.adapter.ImmediateAdapter;
import com.hhly.mlottery.adapter.ImmediateInternationalAdapter;
import com.hhly.mlottery.bean.HotFocusLeagueCup;
import com.hhly.mlottery.bean.ImmediateMatchs;
import com.hhly.mlottery.bean.LeagueCup;
import com.hhly.mlottery.bean.Match;
import com.hhly.mlottery.bean.MatchOdd;
import com.hhly.mlottery.bean.websocket.WebSocketMatchChange;
import com.hhly.mlottery.bean.websocket.WebSocketMatchEvent;
import com.hhly.mlottery.bean.websocket.WebSocketMatchOdd;
import com.hhly.mlottery.bean.websocket.WebSocketMatchStatus;
import com.hhly.mlottery.callback.FocusMatchClickListener;
import com.hhly.mlottery.callback.RecyclerViewItemClickListener;
import com.hhly.mlottery.callback.RequestHostFocusCallBack;
import com.hhly.mlottery.config.BaseURLs;
import com.hhly.mlottery.config.StaticValues;
import com.hhly.mlottery.frame.ScoresFragment;
import com.hhly.mlottery.util.DeviceInfo;
import com.hhly.mlottery.util.DisplayUtil;
import com.hhly.mlottery.util.FiltrateCupsMap;
import com.hhly.mlottery.util.HotFocusUtils;
import com.hhly.mlottery.util.L;
import com.hhly.mlottery.util.MyConstants;
import com.hhly.mlottery.util.PreferenceUtil;
import com.hhly.mlottery.util.StringUtils;
import com.hhly.mlottery.util.cipher.MD5Util;
import com.hhly.mlottery.util.net.VolleyContentFast;
import com.hhly.mlottery.util.websocket.HappySocketClient;
import com.hhly.mlottery.util.websocket.HappySocketClient.SocketResponseCloseListener;
import com.hhly.mlottery.util.websocket.HappySocketClient.SocketResponseErrorListener;
import com.hhly.mlottery.util.websocket.HappySocketClient.SocketResponseMessageListener;
import com.hhly.mlottery.widget.ExactSwipeRefrashLayout;

import org.java_websocket.drafts.Draft_17;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;

/**
 * @author Tenney
 * @ClassName: ImmediateFragment
 * @Description: 即时
 * @date 2015-10-15 上午9:53:24
 */
public class ImmediateFragment extends Fragment implements OnClickListener, SocketResponseErrorListener, SocketResponseCloseListener, SocketResponseMessageListener,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";


    private static final String TAG = "ImmediateFragment";
    // private static Logger logger;

    public static final int REQUEST_FILTRATE_CODE = 0x10;
    public static final int REQUEST_SET_CODE = 0x11;
    public static final int REQUEST_DETAIL_CODE = 0x12;

    public static final int LOAD_DATA_STATUS_INIT = 0;
    public static final int LOAD_DATA_STATUS_LOADING = 1;
    public static final int LOAD_DATA_STATUS_SUCCESS = 2;
    public static final int LOAD_DATA_STATUS_ERROR = 3;

    public static int mLoadDataStatus = LOAD_DATA_STATUS_INIT;// 加载数据状态
    public static boolean isNetSuccess = true;// 告诉筛选页面数据是否加载成功

   // private SlideListView mListView;

/*    private ImageView mBackImgBtn;// 返回菜单
    private ImageView mFilterImgBtn;// 筛选
    private ImageView mSetImgBtn;// 设置
    private TextView mTitleTv;// 标题*/


    private TextView mReloadTvBtn;// 重新加载按钮

    private RelativeLayout mUnconectionLayout;// 没有网络提示
    private ExactSwipeRefrashLayout mSwipeRefreshLayout;// 下拉刷新

    private LinearLayout mLoadingLayout;// 正在加载
    private LinearLayout mErrorLayout;// 网络异常

    private View mNoDataLayout;// 无数据
    private TextView mNoDataTextView;

    private Context mContext;

    private ImmediateAdapter mAdapter;

    private ImmediateInternationalAdapter mInternationalAdapter;

    private List<Match> mAllMatchs;// 所有比赛
    private List<Match> mMatchs;// 显示的比赛
    public static List<LeagueCup> mCups;// 全部联赛
    public static LeagueCup[] mCheckedCups;
    private View mView;

    // 判断是否加载过数据
    private boolean isLoadedData = false;
    public static boolean isCheckedDefualt = false;// true为默认选中全部，但是在筛选页面不选中
    private boolean isWebSocketStart = false;// 不使用websocket时可设置为true

    private HappySocketClient mSocketClient;
    private URI mSocketUri = null;

    private int mHandicap = 1;// 盘口 1.亚盘 2.大小球 3.欧赔 4.不显示

    private FocusMatchClickListener mFocusClickListener;// 关注点击事件

    private static final int DESTROY_VIEW_SENCOND = 60 * 1000;// 60秒
    private static final int GOAL_COLOR_SENCOND = 15 * 1000;// 15秒
    private static final int GREEN_RED_SENCOND = 5 * 1000;// 5秒
    private static final int RELAOD_DATA_SEBCOND = 60 * 1000;// 10秒

    Handler mLoadHandler = new Handler();
    private Runnable mLoadingDataThread = new Runnable() {
        @Override
        public void run() {
            // 在这里数据内容加载到Fragment上
            initData();
        }
    };

    private Vibrator mVibrator;
    private SoundPool mSoundPool;
    private HashMap<Integer, Integer> mSoundMap = new HashMap<>();

//	private ImageView mLoadingImg;
//	private Animation mLoadingAnimation;

    private ImmediateNetStateReceiver mNetStateReceiver;

    public static EventBus imEventBus;



    private static final String FRAGMENT_INDEX = "fragment_index";
    private final int FIRST_FRAGMENT = 0;
    private final int SECOND_FRAGMENT = 1;
    private final int THIRD_FRAGMENT = 2;
    private final int FOUR_FRAGMENT = 2;

    private TextView mFragmentView;

    private int mCurIndex = -1;
    /** 标志位，标志已经初始化完成 */
    private boolean isPrepared;
    /** 是否已被加载过一次，第二次就不再去请求数据了 */
    private boolean mHasLoadedOnce;


    private RecyclerView mRecyclerView;

    LinearLayoutManager layoutManager;


    private String teamLogoPre;

    private String teamLogoSuff;




    public static ImmediateFragment newInstance(String param1, String param2) {
        ImmediateFragment fragment = new ImmediateFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }



    public static ImmediateFragment newInstance(int index) {
        Bundle bundle = new Bundle();
        bundle.putInt(FRAGMENT_INDEX, index);
        ImmediateFragment fragment = new ImmediateFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mSocketUri = new URI(BaseURLs.WS_SERVICE);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        imEventBus=new EventBus();
        imEventBus.register(this);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        L.d(TAG, "___onCreateView____");

/*
        if (mView==null) {
            mView = inflater.inflate(R.layout.football_immediate, container, false);
            Bundle bundle=getArguments();
            if (bundle!=null){
                mCurIndex=bundle.getInt(FRAGMENT_INDEX);
            }
            isPrepared=true;
            initView();

            lazyLoad();
        }

        ViewGroup parent=(ViewGroup)mView.getParent();
        if (parent!=null){
            parent.removeView(mView);
        }*/



        mContext = getActivity();
        mView = inflater.inflate(R.layout.football_immediate, container, false);



        initMedia();
        initView();
        initBroadCase();

        return mView;
    }

   /* protected void lazyLoad(){
        if (!isPrepared || !isVisible || mHasLoadedOnce) {
            return;
        }
      //  if (!isLoadedData && mLoadDataStatus != LOAD_DATA_STATUS_LOADING) {
            mViewHandler.sendEmptyMessage(VIEW_STATUS_LOADING);
            mLoadHandler.postDelayed(mLoadingDataThread, 0);
      //  } else {

     //   }

    }*/


    private void initBroadCase() {
        if (getActivity() != null) {
            mNetStateReceiver = new ImmediateNetStateReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            getActivity().registerReceiver(mNetStateReceiver, filter);
        }

    }

    private void initMedia() {
        mVibrator = (Vibrator) getActivity().getSystemService(Service.VIBRATOR_SERVICE);
        mSoundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5); //
        mSoundMap.put(1, mSoundPool.load(getActivity(), R.raw.sound1, 1)); //
        mSoundMap.put(2, mSoundPool.load(getActivity(), R.raw.sound2, 1));
        mSoundMap.put(3, mSoundPool.load(getActivity(), R.raw.sound3, 1));
    }

    // 初始化控件
    private void initView() {
        // 加载动画
//		mLoadingImg = (ImageView) mView.findViewById(R.id.iv_loading_img);
//		mLoadingAnimation = AnimationUtils.loadAnimation(mContext, R.anim.cirle);
//		mLoadingAnimation.setInterpolator(new LinearInterpolator());
//		mLoadingImg.startAnimation(mLoadingAnimation);
        layoutManager = new LinearLayoutManager(getActivity());


        mSwipeRefreshLayout = (ExactSwipeRefrashLayout) mView.findViewById(R.id.football_immediate_swiperefreshlayout);// 数据板块，listview
        mSwipeRefreshLayout.setColorSchemeResources(R.color.bg_header);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setProgressViewOffset(false, 0, DisplayUtil.dip2px(getContext(), StaticValues.REFRASH_OFFSET_END));
        //mSwipeRefreshLayout.setRefreshing(true);

        // 加载数据的listview
      //  mListView = (SlideListView) mView.findViewById(R.id.immediate_listview);
         mRecyclerView=(RecyclerView)mView.findViewById(R.id.recyclerview_immedia);

        mRecyclerView.setLayoutManager(layoutManager);


//		if (AppConstants.isGOKeyboard) {
        //左划禁止 mod_forbid
     //   mListView.initSlideMode(SlideListView.MOD_FORBID);
      //  mListView.setSwipeRefreshLayout(mSwipeRefreshLayout);

//		}

        mReloadTvBtn = (TextView) mView.findViewById(R.id.network_exception_reload_btn);
        mReloadTvBtn.setOnClickListener(this);

        mNoDataLayout = mView.findViewById(R.id.football_immediate_unfocus_ll);// 没有数据板块
        mNoDataTextView = (TextView) mView.findViewById(R.id.football_immediate_no_data_tv);
        mNoDataTextView.setText(R.string.immediate_no_data);

        mUnconectionLayout = (RelativeLayout) mView.findViewById(R.id.unconection_layout);// 没有网络板块
        mUnconectionLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });

        mLoadingLayout = (LinearLayout) mView.findViewById(R.id.football_immediate_loading_ll);// Loading板块
        mErrorLayout = (LinearLayout) mView.findViewById(R.id.network_exception_layout);// 网络错误板块

        mFocusClickListener = new FocusMatchClickListener() {// 关注按钮事件
            @Override
            public void onClick(View view, String third) {

//				if (AppConstants.isGOKeyboard) {
              //  mListView.slideBack();
//				}

                //String focusIds = PreferenceUtil.getString("focus_ids", "");

                boolean isCheck = (Boolean) view.getTag();// 检查之前是否被选中
                if (!isCheck) {// 插入数据
                    FocusFragment.addFocusId(third);
                   // ((TextView) view).setText(R.string.cancel_favourite);
                    ((ImageView)view).setImageResource(R.mipmap.football_focus);

                    view.setTag(true);
                } else {// 删除
                    FocusFragment.deleteFocusId(third);
                  //  ((TextView) view).setText(R.string.favourite);
                    ((ImageView)view).setImageResource(R.mipmap.football_nomal);

                    view.setTag(false);
                }
                ((ScoresFragment) getParentFragment()).focusCallback();
            }
        };


    }




    private final static int VIEW_STATUS_LOADING = 1;
    private final static int VIEW_STATUS_NO_ANY_DATA = 2;
    private final static int VIEW_STATUS_SUCCESS = 3;
    private final static int VIEW_STATUS_NET_ERROR = 4;
    private final static int VIEW_STATUS_FLITER_NO_DATA = 5;

    private Handler mViewHandler = new Handler() {
        public void handleMessage(Message msg) {
            isNetSuccess = true;
            switch (msg.what) {
                case VIEW_STATUS_LOADING:
                    mErrorLayout.setVisibility(View.GONE);
//				mUnconectionLayout.setVisibility(View.GONE);
                    mSwipeRefreshLayout.setVisibility(View.VISIBLE);
                    mSwipeRefreshLayout.setRefreshing(true);
                    mNoDataLayout.setVisibility(View.GONE);
                    if (!isLoadedData) {
                        mLoadingLayout.setVisibility(View.VISIBLE);
                    }
                    mLoadDataStatus = LOAD_DATA_STATUS_LOADING;
                    break;
                case VIEW_STATUS_NO_ANY_DATA:
                    mLoadingLayout.setVisibility(View.GONE);
                    mErrorLayout.setVisibility(View.GONE);
                    mSwipeRefreshLayout.setVisibility(View.GONE);
                    mSwipeRefreshLayout.setRefreshing(false);
                    mUnconectionLayout.setVisibility(View.GONE);
                    mNoDataLayout.setVisibility(View.VISIBLE);
                    mNoDataTextView.setText(R.string.immediate_no_match);
                    mLoadDataStatus = LOAD_DATA_STATUS_SUCCESS;
                    break;
                case VIEW_STATUS_SUCCESS:
                    mLoadingLayout.setVisibility(View.GONE);
                    mSwipeRefreshLayout.setRefreshing(false);
                    //mListView.setVisibility(View.VISIBLE);
                    mErrorLayout.setVisibility(View.GONE);
                    mNoDataLayout.setVisibility(View.GONE);
                    mUnconectionLayout.setVisibility(View.GONE);
                    mSwipeRefreshLayout.setVisibility(View.VISIBLE);
                    mLoadDataStatus = LOAD_DATA_STATUS_SUCCESS;
                    break;
                case VIEW_STATUS_NET_ERROR:
                    //mListView.setVisibility(View.GONE);
                    mSwipeRefreshLayout.setRefreshing(false);
                    if (isLoadedData) {
                        if (!isPause && getActivity() != null && !isError) {
                            Toast.makeText(getActivity(), R.string.exp_net_status_txt, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        //mSwipeRefreshLayout.setVisibility(View.GONE);
                        mLoadingLayout.setVisibility(View.GONE);
                        mNoDataLayout.setVisibility(View.GONE);

                        mErrorLayout.setVisibility(View.VISIBLE);
                        mLoadDataStatus = LOAD_DATA_STATUS_ERROR;
                    }

                    isNetSuccess = false;
                    break;
                case VIEW_STATUS_FLITER_NO_DATA:
                    mLoadingLayout.setVisibility(View.GONE);
                    mErrorLayout.setVisibility(View.GONE);
                    mSwipeRefreshLayout.setVisibility(View.GONE);
                    mNoDataTextView.setText(R.string.immediate_no_data);
                    mUnconectionLayout.setVisibility(View.GONE);
                    mNoDataLayout.setVisibility(View.VISIBLE);
                    mLoadDataStatus = LOAD_DATA_STATUS_SUCCESS;
                    break;
                default:
                    break;
            }
        }

        ;
    };

    // 初始化数据
    private synchronized void initData() {

        // Log.e(TAG, "url:"+BaseURLs.URL_ImmediateMatchs);
//        isStartInitData = true;

        VolleyContentFast.requestJsonByGet(BaseURLs.URL_ImmediateMatchs, new VolleyContentFast.ResponseSuccessListener<ImmediateMatchs>() {
            @Override
            public synchronized void onResponse(final ImmediateMatchs jsonMatch) {
                if (jsonMatch == null || jsonMatch.getImmediateMatch() == null) {
                    mViewHandler.sendEmptyMessage(VIEW_STATUS_NET_ERROR);
                    return;
                }

                mAllMatchs = jsonMatch.getImmediateMatch();// 获取所有赛程
                mMatchs = new ArrayList<Match>();//
                // mMatchs.addAll(mAllMatchs);//用这种方式是把all的引用赋给它了，操作起来比较麻烦

                if (getActivity() == null) {
                    return;
                }


                teamLogoPre = jsonMatch.getTeamLogoPre();
                teamLogoSuff = jsonMatch.getTeamLogoSuff();


                HotFocusUtils hotFocusUtils = new HotFocusUtils();
                hotFocusUtils.loadHotFocusData(getActivity(), new RequestHostFocusCallBack() {

                    @Override
                    public void callBack(HotFocusLeagueCup hotFocusLeagueCup) {
                        // hotFocusLeagueCup = null;
                        List<String> hotList = null;

                        if (hotFocusLeagueCup == null) {
                            hotList = new ArrayList<String>();
                        } else {
                            hotList = hotFocusLeagueCup.getHotLeagueIds();
                        }

                        if (FiltrateCupsMap.immediateCups.length != 0) {// 判断是否已经筛选过
                            for (Match m : mAllMatchs) {// 已选择的
                                for (String checkedId : FiltrateCupsMap.immediateCups) {
                                    if (m.getRaceId().equals(checkedId)) {
                                        mMatchs.add(m);
                                        break;
                                    }
                                }
                            }
                        } else {// 没有筛选过
                            for (Match m : mAllMatchs) {// 默认显示热门赛程
                                for (String hotId : hotList) {
                                    if (m.getRaceId().equals(hotId)) {
                                        mMatchs.add(m);
                                        break;
                                    }
                                }
                            }
                        }
                        mCups = jsonMatch.getAll();
                        mNoDataTextView.setText(R.string.immediate_no_data);
                        if (mMatchs.size() == 0) {// 没有热门赛事，显示全部
                            // mNoDataLayout.setVisibility(View.VISIBLE);
                            // mLoadingLayout.setVisibility(View.GONE);
                            // mListView.setVisibility(View.GONE);
                            // mErrorLayout.setVisibility(View.GONE);
                            mMatchs.addAll(mAllMatchs);
                            mCheckedCups = mCups.toArray(new LeagueCup[mCups.size()]);
                            if (mMatchs.size() == 0) {// 一个赛事都没有，显示“暂无赛事”
                                //  mRecyclerView.setLayoutManager(layoutManager);

                               /* if (AppConstants.isGOKeyboard) {
                                    *//*mInternationalAdapter = new ImmediateInternationalAdapter(mContext, mMatchs, R.layout.item_football_international);
                                    mInternationalAdapter.setItemPaddingRight(mListView.getItemPaddingRight());
                                    mInternationalAdapter.setFocusClickListener(mFocusClickListener);
                                    mListView.setAdapter(mInternationalAdapter);*//*
                                } else {*/
                                    mAdapter = new ImmediateAdapter(mContext, mMatchs, teamLogoPre, teamLogoSuff);
                                    //  mAdapter.setItemPaddingRight(mListView.getItemPaddingRight());
                                    mAdapter.setmFocusMatchClickListener(mFocusClickListener);
                                    mRecyclerView.setAdapter(mAdapter);


                                    mAdapter.setmOnItemClickListener(new RecyclerViewItemClickListener() {
                                        @Override
                                        public void onItemClick(View view, String data) {
                                            String thirdId = data;
                                            Intent intent = new Intent(getActivity(), FootballMatchDetailActivity.class);
                                            intent.putExtra("thirdId", thirdId);
                                            intent.putExtra("currentFragmentId", 0);

                                            getParentFragment().startActivityForResult(intent, REQUEST_DETAIL_CODE);
                                        }
                                    });
                               // }

                                isLoadedData = true;
                                mViewHandler.sendEmptyMessage(VIEW_STATUS_NO_ANY_DATA);
                                startWebsocket();
                                return;
                            }// 否则走下面，不需要再写
                        } else {
                            List<LeagueCup> tempHotCups = new ArrayList<LeagueCup>();

                            if (FiltrateCupsMap.immediateCups.length != 0) {
                                for (LeagueCup cup : mCups) {
                                    for (String checkedId : FiltrateCupsMap.immediateCups) {
                                        if (cup.getRaceId().equals(checkedId)) {
                                            tempHotCups.add(cup);
                                            break;
                                        }
                                    }
                                }
                            } else {
                                for (LeagueCup cup : mCups) {
                                    for (String hotId : hotList) {
                                        if (cup.getRaceId().equals(hotId)) {
                                            tempHotCups.add(cup);
                                            break;
                                        }
                                    }
                                }
                            }

                            mCheckedCups = tempHotCups.toArray(new LeagueCup[tempHotCups.size()]);
                        }

                        //mRecyclerView.setLayoutManager(layoutManager);


                      /*  if (AppConstants.isGOKeyboard) {
                           *//* mInternationalAdapter = new ImmediateInternationalAdapter(mContext, mMatchs, R.layout.item_football_international);
                            mInternationalAdapter.setItemPaddingRight(mListView.getItemPaddingRight());
                            mInternationalAdapter.setFocusClickListener(mFocusClickListener);
                            mListView.setAdapter(mInternationalAdapter);*//*
                        } else {*/
                            mAdapter = new ImmediateAdapter(mContext, mMatchs, teamLogoPre, teamLogoSuff);
                            //  mAdapter.setItemPaddingRight(mListView.getItemPaddingRight());
                            mAdapter.setmFocusMatchClickListener(mFocusClickListener);
                            mRecyclerView.setAdapter(mAdapter);


                            mAdapter.setmOnItemClickListener(new RecyclerViewItemClickListener() {
                                @Override
                                public void onItemClick(View view, String data) {
                                    String thirdId = data;
                                    Intent intent = new Intent(getActivity(), FootballMatchDetailActivity.class);
                                    intent.putExtra("thirdId", thirdId);
                                    getParentFragment().startActivityForResult(intent, REQUEST_DETAIL_CODE);
                                }
                            });
                      //  }

                        isLoadedData = true;
                        mViewHandler.sendEmptyMessage(VIEW_STATUS_SUCCESS);
                        startWebsocket();
                    }
                });
            }
        }, new VolleyContentFast.ResponseErrorListener() {
            @Override
            public void onErrorResponse(VolleyContentFast.VolleyException exception) {
                mViewHandler.sendEmptyMessage(VIEW_STATUS_NET_ERROR);

            }
        }, ImmediateMatchs.class);

//
    }

    private synchronized void startWebsocket() {

        if (mSocketClient == null || (mSocketClient != null && mSocketClient.isClosed())) {
            // client.close();
            mSocketClient = new HappySocketClient(mSocketUri, new Draft_17());
            mSocketClient.setSocketResponseMessageListener(this);
            mSocketClient.setSocketResponseCloseListener(this);
            mSocketClient.setSocketResponseErrorListener(this);
            try {
                mSocketClient.connect();
            } catch (IllegalThreadStateException e) {
                mSocketClient.close();
                L.e(TAG, "IllegalThreadStateException.........");
            }

            if (isError) {
                initData();
            }

            isError = false;
        }

        L.d(TAG, "websocket start status..");
        L.e(TAG, "websocket isClosed = " + mSocketClient.isClosed());
        L.e(TAG, "websocket isClosing = " + mSocketClient.isClosing());
        L.e(TAG, "websocket isConnecting = " + mSocketClient.isConnecting());
        L.e(TAG, "websocket isFlushAndClose = " + mSocketClient.isFlushAndClose());
        L.e(TAG, "websocket isOpen = " + mSocketClient.isOpen());
        L.e(TAG, "isWebSocketStart = " + isWebSocketStart);
    }

    Handler mSocketHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            L.e(TAG, "__handleMessage__");

            L.e(TAG, "msg.arg1 = " + msg.arg1);
            if (msg.arg1 == 1) {
                String ws_json = (String) msg.obj;
                L.e(TAG, "ws_json = " + ws_json);
                WebSocketMatchStatus webSocketMatchStatus = null;
                try {
                    webSocketMatchStatus = JSON.parseObject(ws_json, WebSocketMatchStatus.class);
                } catch (Exception e) {
                    ws_json = ws_json.substring(0, ws_json.length() - 1);
                    // Log.e(TAG, "ws_json = " + ws_json);
                    webSocketMatchStatus = JSON.parseObject(ws_json, WebSocketMatchStatus.class);
                }
                // Log.e(TAG, "----webSocketMatchStatus -----" +
                // webSocketMatchStatus.getThirdId());
                updateListViewItemStatus(webSocketMatchStatus);
            } else if (msg.arg1 == 2) {
                String ws_json = (String) msg.obj;
                L.e(TAG, "ws_json = " + ws_json);
                WebSocketMatchOdd webSocketMatchOdd = null;
                try {
                    webSocketMatchOdd = JSON.parseObject(ws_json, WebSocketMatchOdd.class);
                } catch (Exception e) {
                    ws_json = ws_json.substring(0, ws_json.length() - 1);
                    webSocketMatchOdd = JSON.parseObject(ws_json, WebSocketMatchOdd.class);
                }

                //L.e(TAG, "----webSocketMatchStatus -----" + webSocketMatchOdd.getThirdId());
                updateListViewItemOdd(webSocketMatchOdd);
            } else if (msg.arg1 == 4) {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        if (getActivity() == null) {
                            return;
                        }

//                        Toast.makeText(getActivity(), R.string.immediate_reload_toast, Toast.LENGTH_LONG).show();
                        mViewHandler.sendEmptyMessage(VIEW_STATUS_LOADING);
                        initData();
                    }
                }, RELAOD_DATA_SEBCOND);

            } else if (msg.arg1 == 3) {
                String ws_json = (String) msg.obj;
                L.e(TAG, "ws_json = " + ws_json);
                WebSocketMatchEvent webSocketMatchEvent = null;
                try {
                    webSocketMatchEvent = JSON.parseObject(ws_json, WebSocketMatchEvent.class);
                } catch (Exception e) {
                    ws_json = ws_json.substring(0, ws_json.length() - 1);
                    webSocketMatchEvent = JSON.parseObject(ws_json, WebSocketMatchEvent.class);
                }

                updateListViewItemEvent(webSocketMatchEvent);
            } else if (msg.arg1 == 5) {
                String ws_json = (String) msg.obj;
                L.e(TAG, "ws_json = " + ws_json);
                WebSocketMatchChange webSocketMatchChange = null;
                try {
                    webSocketMatchChange = JSON.parseObject(ws_json, WebSocketMatchChange.class);
                } catch (Exception e) {
                    ws_json = ws_json.substring(0, ws_json.length() - 1);
                    webSocketMatchChange = JSON.parseObject(ws_json, WebSocketMatchChange.class);
                }
                upateListViewItemMatchChange(webSocketMatchChange);
            }
        }
    };

    private void upateListViewItemMatchChange(WebSocketMatchChange webSocketMatchChange) {
        synchronized (mAllMatchs) {
            for (final Match match : mAllMatchs) {
                String language = PreferenceUtil.getString("language", "rCN");

                if (match.getThirdId().equals(webSocketMatchChange.getThirdId())) {
                    if ((webSocketMatchChange.getData().get("region").equals("zh") && language.equals("rCN"))
                            || (webSocketMatchChange.getData().get("region").equals("zh-TW") && language.equals("rTW"))
                            || (webSocketMatchChange.getData().get("region").equals("en") && language.equals("rEN"))
                            || (webSocketMatchChange.getData().get("region").equals("ko") && language.equals("rKO"))
                            || (webSocketMatchChange.getData().get("region").equals("id") && language.equals("rID"))
                            || (webSocketMatchChange.getData().get("region").equals("th") && language.equals("rTH"))
                            || (webSocketMatchChange.getData().get("region").equals("vi") && language.equals("rVI"))) {

                        if (webSocketMatchChange.getData().get("hometeam") != null) {
                            match.setHometeam(webSocketMatchChange.getData().get("hometeam"));
                        }

                        if (webSocketMatchChange.getData().get("guestteam") != null) {
                            match.setGuestteam(webSocketMatchChange.getData().get("guestteam"));
                        }

                        if (webSocketMatchChange.getData().get("racename") != null) {
                            match.setRacename(webSocketMatchChange.getData().get("racename"));
                        }

                        updateListView(match);
                        break;
                    }
                }
            }
        }
    }

    private void updateListViewItemEvent(WebSocketMatchEvent webSocketMatchEvent) {

        synchronized (mAllMatchs) {

            for (final Match match : mAllMatchs) {
                if (match.getThirdId().equals(webSocketMatchEvent.getThirdId())) {// 查找对应的比赛对象
                    match.setItemBackGroundColorId(R.color.item_football_event_yellow);
                    String eventType = webSocketMatchEvent.getData().get("eventType");
                    if ("1".equals(eventType) || "2".equals(eventType) || "5".equals(eventType) || "6".equals(eventType)) {// 主队有效进球传
                        // 客队有效进球5or客队进球取消6
                        if (webSocketMatchEvent.getData().get("homeScore") != null) {
                            match.setHomeScore(webSocketMatchEvent.getData().get("homeScore"));
                        }

                        if (webSocketMatchEvent.getData().get("guestScore") != null) {
                            match.setGuestScore(webSocketMatchEvent.getData().get("guestScore"));
                        }

                    } else if ("3".equals(eventType) || "4".equals(eventType) || "7".equals(eventType) || "8".equals(eventType)) {// 主队红牌3or主队红牌取消4

                        if (webSocketMatchEvent.getData().get("home_rc") != null) {
                            match.setHome_rc(webSocketMatchEvent.getData().get("home_rc"));
                        }

                        if (webSocketMatchEvent.getData().get("guest_rc") != null) {
                            match.setGuest_rc(webSocketMatchEvent.getData().get("guest_rc"));
                        }

                        if (webSocketMatchEvent.getData().get("home_yc") != null) {
                            match.setHome_yc(webSocketMatchEvent.getData().get("home_yc"));
                        }

                        if (webSocketMatchEvent.getData().get("guest_yc") != null) {
                            match.setGuest_yc(webSocketMatchEvent.getData().get("guest_yc"));
                        }
                    }

                    if ("1".equals(eventType) || "2".equals(eventType) || "3".equals(eventType) || "4".equals(eventType)) {// 主场红名
                        match.setHomeTeamTextColorId(R.color.red);
                    } else if ("5".equals(eventType) || "6".equals(eventType) || "7".equals(eventType) || "8".equals(eventType)) {// 客场红名
                        match.setGuestTeamTextColorId(R.color.red);
                    }

                    updateListView(match);// 先换颜色
                    new Handler().postDelayed(new Runnable() {// 五秒后把颜色修改回来
                        @Override
                        public void run() {
                            match.setItemBackGroundColorId(R.color.white);
                            match.setHomeTeamTextColorId(R.color.msg);
                            match.setGuestTeamTextColorId(R.color.msg);
                            updateListView(match);
                        }
                    }, GOAL_COLOR_SENCOND);

                    // 下面是触发系统声音，注意只有在页面出现的比赛才提示

                    boolean isShow = false;

                    for (Match showMatch : mMatchs) {
                        if (showMatch.getThirdId().equals(match.getThirdId())) {
                            isShow = true;
                        }
                    }

                    if (!isShow) {// 不属于就不做下面操作
                        break;
                    }

                    if (getActivity() == null) {
                        return;
                    }

                    boolean isSetFocus = PreferenceUtil.getBoolean(MyConstants.GAMEATTENTION, true);// 设置中的关注选项
                    String fucus_id = PreferenceUtil.getString(FocusFragment.FOCUS_ISD, "");
                    boolean isFocus = false;// 判断该球赛是否属于关注的

                    if (isSetFocus) {
                        if ("".equals(fucus_id) || ",".equals(fucus_id)) {
                            isFocus = false;
                        } else {
                            String[] ids = fucus_id.split("[,]");
                            for (String id : ids) {
                                if (match.getThirdId().equals(id)) {
                                    isFocus = true;
                                    break;
                                }
                            }
                        }
                    }

                    if ((isSetFocus && isFocus) || (!isSetFocus)) {//

                        if ("1".equals(eventType) || "2".equals(eventType)) {
                            int soundId = PreferenceUtil.getInt(MyConstants.HOSTTEAMINDEX, 1);
                            if (soundId != 4) {
                                mSoundPool.play(mSoundMap.get(soundId), 1, 1, 0, 0, 1);
                            }
                            boolean isShake = PreferenceUtil.getBoolean(MyConstants.VIBRATEGOALHINT, true);
                            if (isShake) {
                                mVibrator.vibrate(1000);
                            }

                        } else if ("5".equals(eventType) || "6".equals(eventType)) {
                            int soundId = PreferenceUtil.getInt(MyConstants.GUESTTEAM, 2);
                            if (soundId != 4) {
                                mSoundPool.play(mSoundMap.get(soundId), 1, 1, 0, 0, 1);
                            }
                            boolean isShake = PreferenceUtil.getBoolean(MyConstants.VIBRATEGOALHINT, true);
                            if (isShake) {
                                mVibrator.vibrate(1000);
                            }
                        } else if ("3".equals(eventType) || "4".equals(eventType) || "7".equals(eventType) || "8".equals(eventType)) {

                            boolean isShoud = PreferenceUtil.getBoolean(MyConstants.VOICEREDHINT, true);
                            if (isShoud) {
                                mSoundPool.play(mSoundMap.get(3), 1, 1, 0, 0, 1);
                            }

                            boolean isShake = PreferenceUtil.getBoolean(MyConstants.VIBRATEREDHINT, true);
                            if (isShake) {
                                mVibrator.vibrate(1000);
                            }
                        }
                    }
                    break;
                }
            }

        }
        // }
    }

    private void updateListViewItemOdd(WebSocketMatchOdd webSocketMatchOdd) {
        // Match targetMatch = null;
        List<Map<String, String>> data = webSocketMatchOdd.getData();
        synchronized (mAllMatchs) {
            for (Match match : mAllMatchs) {// all里面的match
                if (match.getThirdId().equals(webSocketMatchOdd.getThirdId())) {
                    // try {
                    // targetMatch = (Match) match.clone();
                    updateMatchOdd(match, data);
                    updateListView(match);
                    // } catch (CloneNotSupportedException e) {
                    // e.printStackTrace();
                    // }
                    break;
                }
            }
        }

        // for (Match match : mMatchs) {//显示的match
        // if (match.getThirdId().equals(webSocketMatchOdd.getThirdId())) {
        // //updateMatchOdd(match, data);
        //
        // break;
        // }
        // }

        // if (targetMatch != null) {
        // updateMatchOdd(targetMatch, data);
        // updateListView(targetMatch);
        // } else {
        // Log.e(TAG, "targetMatch is null,webSocketMatchOdd.id = " +
        // webSocketMatchOdd.getThirdId());
        // }
    }

    private void updateListViewItemStatus(WebSocketMatchStatus webSocketMatchStatus) {
        // Match targetMatch = null;
        Map<String, String> data = webSocketMatchStatus.getData();
        final Match[] target = new Match[1];

        synchronized (mAllMatchs) {
            for (Match match : mAllMatchs) {
                if (match.getThirdId().equals(webSocketMatchStatus.getThirdId())) {
                    // try {
                    // targetMatch = (Match) match.clone();
                    // match = targetMatch;// 改变引用
                    updateMatchStatus(match, data);// 会修改mMatch里面的引用
                    updateListView(match);
                    // } catch (CloneNotSupportedException e) {
                    // e.printStackTrace();
                    // }
                    target[0] = match;
                    break;
                }
            }
        }

        if (target[0] != null) {
            if ("-1".equals(target[0].getStatusOrigin()) || "-10".equals(target[0].getStatusOrigin()) || "-12".equals(target[0].getStatusOrigin()) || "-14".equals(target[0].getStatusOrigin())) {
                // L.d(TAG, "1分钟后界面销毁");

                new Handler().postDelayed(new Runnable() {// 五秒后把颜色修改回来
                    @Override
                    public void run() {

                        synchronized (mCups) {
                            LeagueCup targetCup = null;
                            for (LeagueCup cup : mCups) {
                                if (target[0].getRaceId().equals(cup.getRaceId())) {
                                    targetCup = cup;
                                    break;
                                }
                            }

                            if (targetCup != null) {
                                if (targetCup.getCount() > 1) {
                                    targetCup.setCount(targetCup.getCount() - 1);
                                } else {
                                    mCups.remove(targetCup);
                                }
                            }
                        }

                        synchronized (mCheckedCups) {
                            LeagueCup targetCup = null;
                            for (LeagueCup cup : mCheckedCups) {
                                if (target[0].getRaceId().equals(cup.getRaceId())) {
                                    targetCup = cup;
                                }
                            }

                            if (targetCup != null) {
                                if (targetCup.getCount() > 1) {
                                    targetCup.setCount(targetCup.getCount() - 1);
                                } else {
                                    // LeagueCup[] tempCups = new
                                    // LeagueCup[mCups.size()];//
                                    // int index = 0;

                                    List<LeagueCup> tempCups = new ArrayList<LeagueCup>();

                                    for (LeagueCup acup : mCups) {
                                        for (LeagueCup cup : mCheckedCups) {
                                            if (acup.getRaceId().equals(cup.getRaceId())) {
                                                // tempCups[index] =
                                                // cup;
                                                // index++;
                                                tempCups.add(acup);
                                                break;
                                            }
                                        }
                                    }
                                    mCheckedCups = tempCups.toArray(new LeagueCup[]{});
                                }
                            }
                        }

                        mAllMatchs.remove(target[0]);
                        mMatchs.remove(target[0]);
                        updateAdapter();

                        if (mMatchs.size() == 0) {
                            if (mAllMatchs.size() == mMatchs.size()) {
                                mViewHandler.sendEmptyMessage(VIEW_STATUS_NO_ANY_DATA);
                            } else {
                                mViewHandler.sendEmptyMessage(VIEW_STATUS_FLITER_NO_DATA);
                            }
                        }

                    }
                }, DESTROY_VIEW_SENCOND);//
            }
        }
    }

    /**
     * 更新单个比赛状态
     *
     * @param match
     * @param data
     */
    private void updateMatchStatus(Match match, Map<String, String> data) {
        match.setStatusOrigin(data.get("statusOrigin"));
        if ("0".equals(match.getStatusOrigin()) && "1".equals(match.getStatusOrigin())) {// 未开场变成开场
            match.setHomeScore("0");
            match.setGuestScore("0");
        }
        if (data.get("keepTime") != null) {
            match.setKeepTime(data.get("keepTime"));
        }
        if (data.get("home_yc") != null) {
            match.setHome_yc(data.get("home_yc"));
        }
        if (data.get("guest_yc") != null) {
            match.setGuest_yc(data.get("guest_yc"));
        }

        if (data.get("homeHalfScore") != null) {
            match.setHomeHalfScore(data.get("homeHalfScore"));
        }

        if (data.get("guestHalfScore") != null) {
            match.setGuestHalfScore(data.get("guestHalfScore"));
        }

    }

    private void updateMatchOdd(Match match, List<Map<String, String>> datas) {
        L.e(TAG, "update match odd id = " + match.getThirdId());
        for (Map<String, String> map : datas) {
            if (map.get("handicap").equals("asiaLet")) {
                Map<String, MatchOdd> odds = match.getMatchOdds();
                MatchOdd odd = odds.get("asiaLet");

                if (odd == null) {//
                    odd = new MatchOdd();
                    odd.setHandicap("asiaLet");
                    odd.setHandicapValue(map.get("mediumOdds"));
                    odd.setRightOdds(map.get("rightOdds"));
                    odd.setLeftOdds(map.get("leftOdds"));
                    odds.put("asiaLet", odd);
                }

                if (mHandicap == 1) {
                    changeOddTextColor(match, odd.getLeftOdds(), odd.getRightOdds(), map.get("leftOdds"), map.get("rightOdds"), odd.getHandicapValue(), map.get("mediumOdds"));
                }
                L.w(TAG, "odd = " + odd);
                odd.setLeftOdds(map.get("leftOdds"));
                odd.setRightOdds(map.get("rightOdds"));
                odd.setHandicapValue(map.get("mediumOdds"));

            } else if (map.get("handicap").equals("euro")) {
                Map<String, MatchOdd> odds = match.getMatchOdds();
                MatchOdd odd = odds.get("euro");
                if (odd == null) {//
                    odd = new MatchOdd();
                    odd.setHandicap("euro");
                    odd.setMediumOdds(map.get("mediumOdds"));
                    odd.setRightOdds(map.get("rightOdds"));
                    odd.setLeftOdds(map.get("leftOdds"));
                    odds.put("euro", odd);
                }
                if (mHandicap == 3) {
                    changeOddTextColor(match, odd.getLeftOdds(), odd.getRightOdds(), map.get("leftOdds"), map.get("rightOdds"), odd.getMediumOdds(), map.get("mediumOdds"));
                }
                L.w(TAG, "odd = " + odd);
                odd.setLeftOdds(map.get("leftOdds"));
                odd.setRightOdds(map.get("rightOdds"));
                odd.setMediumOdds(map.get("mediumOdds"));

            } else if (map.get("handicap").equals("asiaSize")) {
                Map<String, MatchOdd> odds = match.getMatchOdds();
                MatchOdd odd = odds.get("asiaSize");
                if (odd == null) {//
                    odd = new MatchOdd();
                    odd.setHandicap("asiaSize");
                    odd.setHandicapValue(map.get("mediumOdds"));
                    odd.setRightOdds(map.get("rightOdds"));
                    odd.setLeftOdds(map.get("leftOdds"));
                    odds.put("asiaSize", odd);
                }
                if (mHandicap == 2) {
                    changeOddTextColor(match, odd.getLeftOdds(), odd.getRightOdds(), map.get("leftOdds"), map.get("rightOdds"), odd.getHandicapValue(), map.get("mediumOdds"));
                }
                L.w(TAG, "odd = " + odd);
                odd.setLeftOdds(map.get("leftOdds"));
                odd.setRightOdds(map.get("rightOdds"));
                odd.setHandicapValue(map.get("mediumOdds"));

            }
        }

        // updateAdapter();
    }

    private Map<String, Integer> singleMatchChangeOddColorCount = new HashMap<String, Integer>();// 同一场比赛，操作红升绿降线程个数

    /**
     * 修改match的赔率的颜色，是一个引用，包括mAllMatch和mMatch
     *
     * @param match
     * @param leftMatchOdd
     * @param rightMatchOdd
     * @param leftMapOdd
     * @param rightMapOdd
     */
    public void changeOddTextColor(final Match match, String leftMatchOdd, String rightMatchOdd, String leftMapOdd, String rightMapOdd, String midMatchOdd, String midMapOdd) {
        // 先设置默认颜色，因为一开始可能没值
        resetOddColor(match);

        try {
            int keepTime = Integer.parseInt(match.getKeepTime());
            if (keepTime >= 89) {// 显示封盘，不需要修改颜色
                return;
            }
        } catch (Exception e) {
        }

        if ("-".equals(leftMatchOdd) || "-".equals(rightMatchOdd) || "-".equals(leftMapOdd) || "-".equals(rightMapOdd) || "-".equals(midMatchOdd) || "-".equals(midMapOdd) || "|".equals(midMatchOdd)
                || "|".equals(midMapOdd) || StringUtils.isEmpty(leftMatchOdd) || StringUtils.isEmpty(rightMatchOdd) || StringUtils.isEmpty(leftMapOdd) || StringUtils.isEmpty(rightMapOdd)
                || StringUtils.isEmpty(midMatchOdd) || StringUtils.isEmpty(midMapOdd)) {
            return;
        }

        try {
            float leftMatchOddF = Float.parseFloat(leftMatchOdd);
            float rightMatchOddF = Float.parseFloat(rightMatchOdd);
            float leftMapOddF = Float.parseFloat(leftMapOdd);
            float rightMapOddF = Float.parseFloat(rightMapOdd);
            float midMatchOddF = Float.parseFloat(midMatchOdd);
            float midMapOddF = Float.parseFloat(midMapOdd);

            if (leftMatchOddF < leftMapOddF) {// 左边的值升了
                match.setLeftOddTextColorId(R.color.odd_rise_red);
            } else if (leftMatchOddF > leftMapOddF) {// 左边的值降了
                match.setLeftOddTextColorId(R.color.odd_drop_green);
            } else {
                match.setLeftOddTextColorId(R.color.content_txt_light_grad);
            }

            if (rightMatchOddF < rightMapOddF) {// 右边的值升了
                match.setRightOddTextColorId(R.color.odd_rise_red);
            } else if (rightMatchOddF > rightMapOddF) {// 右边的值降了
                match.setRightOddTextColorId(R.color.odd_drop_green);
            } else {
                match.setRightOddTextColorId(R.color.content_txt_light_grad);
            }

            if (midMatchOddF < midMapOddF) {// 中间的值升了
                match.setMidOddTextColorId(R.color.odd_rise_red);
            } else if (midMatchOddF > midMapOddF) {// 中间的值降了
                match.setMidOddTextColorId(R.color.odd_drop_green);
            } else {
                if (mHandicap == 1 || mHandicap == 2) {// 亚盘，大小球是黑色
                    match.setMidOddTextColorId(R.color.content_txt_black);
                } else {
                    match.setMidOddTextColorId(R.color.content_txt_light_grad);
                }
            }

        } catch (Exception e) {

        }

        int count = 0;

        if (singleMatchChangeOddColorCount.get(match.getThirdId()) != null) {
            count = singleMatchChangeOddColorCount.get(match.getThirdId());
        }

        singleMatchChangeOddColorCount.put(match.getThirdId(), ++count);

        new Handler().postDelayed(new Runnable() {// 五秒后把颜色修改回来
            @Override
            public void run() {
                synchronized (singleMatchChangeOddColorCount) {
                    int count = singleMatchChangeOddColorCount.get(match.getThirdId());
                    count--;
                    singleMatchChangeOddColorCount.put(match.getThirdId(), count);
                    if (count == 0) {
                        resetOddColor(match);
                        updateListView(match);
                    }
                }
            }
        }, GREEN_RED_SENCOND);

    }

    /**
     * 重置赔率颜色
     *
     * @param match
     */
    private void resetOddColor(Match match) {
        match.setLeftOddTextColorId(R.color.content_txt_light_grad);
        match.setRightOddTextColorId(R.color.content_txt_light_grad);

        if (mHandicap == 1 || mHandicap == 2) {// 亚盘，大小球是黑色

            match.setMidOddTextColorId(R.color.content_txt_black);
        } else {
            match.setMidOddTextColorId(R.color.content_txt_light_grad);
        }
    }

    private void updateListView(Match targetMatch) {
        updateAdapter();
    }


    public void reLoadData(){
        mViewHandler.sendEmptyMessage(VIEW_STATUS_LOADING);
        mLoadHandler.postDelayed(mLoadingDataThread, 0);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.network_exception_reload_btn:

                mViewHandler.sendEmptyMessage(VIEW_STATUS_LOADING);
                mLoadHandler.post(mLoadingDataThread);

                break;

            case R.layout.item_header_unconection_view:
                L.w(TAG, "head onclick");
                break;
            default:
                break;

        }
    }


    /**刷选返回
     * 接受消息的页面实现
     * */
    public void onEventMainThread(Map<String,Object> map) {
        String[] checkedIds = (String[])((LinkedList)map.get(FiltrateMatchConfigActivity.RESULT_CHECKED_CUPS_IDS)).toArray(new String[]{});
        FiltrateCupsMap.immediateCups = checkedIds;
        mMatchs.clear();
        for (Match match : mAllMatchs) {
            boolean isExistId = false;
            for (String checkedId : checkedIds) {
                if (match.getRaceId().equals(checkedId)) {
                    isExistId = true;
                    break;
                }
            }
            if (isExistId) {
                mMatchs.add(match);
            }
        }
        List<LeagueCup> leagueCupList = new ArrayList<LeagueCup>();

        for (LeagueCup cup : mCups) {
            boolean isExistId = false;
            for (String checkedId : checkedIds) {
                if (checkedId.equals(cup.getRaceId())) {
                    isExistId = true;
                    break;
                }
            }

            if (isExistId) {
                leagueCupList.add(cup);
            }
        }

        mCheckedCups = leagueCupList.toArray(new LeagueCup[]{});

        updateAdapter();

        isCheckedDefualt =(boolean)map.get(FiltrateMatchConfigActivity.CHECKED_DEFUALT);

        if (mMatchs.size() == 0) {// 没有比赛
            mViewHandler.sendEmptyMessage(VIEW_STATUS_FLITER_NO_DATA);
        } else {
            mViewHandler.sendEmptyMessage(VIEW_STATUS_SUCCESS);
        }
    }


    /**
     * 设置
     * 接受消息的页面实现
     */
    public void onEventMainThread(Integer currentFragmentId) {

        if (PreferenceUtil.getBoolean(MyConstants.RBSECOND, true)) {
            mHandicap = 1;
        } else if (PreferenceUtil.getBoolean(MyConstants.rbSizeBall, false)) {
            mHandicap = 2;// 大小球
        } else if (PreferenceUtil.getBoolean(MyConstants.RBOCOMPENSATE, false)) {
            mHandicap = 3;
        } else if (PreferenceUtil.getBoolean(MyConstants.RBNOTSHOW, false)) {
            mHandicap = 4;
        }

        if (mMatchs == null) {
            return;
        }
        synchronized (mMatchs) {
            for (Match match : mMatchs) {
                resetOddColor(match);
            }
            updateAdapter();
        }
    }

 /**
     * 比赛详情返回FootballMatchDetailActivity
     * 接受消息的页面实现
     */
    public void onEventMainThread(String currentFragmentId) {
        updateAdapter();
        ((ScoresFragment) getParentFragment()).focusCallback();
    }


    private void updateAdapter() {

       /* if (AppConstants.isGOKeyboard) {
            if (mInternationalAdapter == null) {
                return;
            }

            mInternationalAdapter.updateDatas(mMatchs);
            mInternationalAdapter.notifyDataSetChanged();
        } else {*/
            if (mAdapter == null) {
                return;
            }
            mAdapter.updateDatas(mMatchs);
            mAdapter.notifyDataSetChanged();
      //  }
    }



    @Override
    public void onResume() {
        super.onResume();
        L.v(TAG, "___onResume___");

        isPause = false;
        if (isDestroy) {
            return;
        }


        if (!isLoadedData && mLoadDataStatus != LOAD_DATA_STATUS_LOADING) {
            mViewHandler.sendEmptyMessage(VIEW_STATUS_LOADING);
            mLoadHandler.postDelayed(mLoadingDataThread, 0);
        } else {

        }

    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
//        L.v(TAG, "___onHiddenChanged___");
//        L.v(TAG, "hidden = "+hidden);
        if (hidden) {
            isPause = true;
            if (mSocketClient != null) {
                isDestroy = true;
                mSocketClient.close();
            }
        } else {
            isPause = false;
            isDestroy = false;
            if (mLoadDataStatus != LOAD_DATA_STATUS_LOADING) {
                mViewHandler.sendEmptyMessage(VIEW_STATUS_LOADING);
                mLoadHandler.postDelayed(mLoadingDataThread, 0);
            }
        }
    }

    private boolean isDestroy = false;



    @Override
    public void onDestroy() {
        super.onDestroy();
        L.d(TAG, "__onDestroy___");
        if (mSocketClient != null) {
            isDestroy = true;
            mSocketClient.close();
        }

        if (getActivity() != null && mNetStateReceiver != null) {
            getActivity().unregisterReceiver(mNetStateReceiver);
        }



    }

    private boolean isError = false;

    @Override
    public void onError(Exception exception) {
        L.e(TAG, "websocket error");
        isError = true;
        restartSocket();
    }

    @Override
    public void onClose(String message) {
        L.e(TAG, "websocket close");

        if (!isError) {
            restartSocket();
        }
    }

    private Timer timer = new Timer();
    private boolean isTimerStart = false;

    private synchronized void restartSocket() {
        if (!isDestroy) {
            if (!isTimerStart) {
                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        if (!isDestroy) {
                            startWebsocket();
                        } else {
                            timer.cancel();
                        }
                    }
                };
                timer.schedule(timerTask, 2000, 5000);
                isTimerStart = true;
            }
        }
    }

    @Override
    public void onMessage(String message) {
        L.w(TAG, "message = " + message);
        // L.e(TAG, "websocket isClosed = " + client.isClosed());
        // L.e(TAG, "websocket isClosing = " + client.isClosing());
        // L.e(TAG, "websocket isConnecting = " + client.isConnecting());
        // L.e(TAG, "websocket isFlushAndClose = " + client.isFlushAndClose());
        // L.e(TAG, "websocket isOpen = " + client.isOpen());

        if (message.startsWith("CONNECTED")) {
            String id = "android" + DeviceInfo.getDeviceId(getActivity());
            // if (logger != null) {
            // logger.debug(DateUtil.getMillisDateTime(new Date()) +
            // " CONNECTED-----" + id);
            // }
            // L.d(TAG, "id = " + id);
            id = MD5Util.getMD5(id);
            // L.d(TAG, "md5 id = " + id);
            // client.send("SUBSCRIBE\nid:" + id +
            // "\ndestination:/topic/USER.topic.app\n\n");
            mSocketClient.send("SUBSCRIBE\nid:" + id + "\ndestination:/topic/USER.topic.app\n\n");
            isWebSocketStart = true;
            return;
        } else if (message.startsWith("MESSAGE")) {
            String[] msgs = message.split("\n");
            // System.out.println("msgs.length = " +
            // msgs.length);
            String ws_json = msgs[msgs.length - 1];

            // if (logger != null) {
            // logger.debug(ws_json);
            // }

            // System.out.println("msgs last = " + ws_json);
            String type = "";
            try {
                JSONObject jsonObject = new JSONObject(ws_json);
                type = jsonObject.getString("type");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (!"".equals(type)) {
                Message msg = Message.obtain();
                msg.obj = ws_json;
                msg.arg1 = Integer.parseInt(type);

                mSocketHandler.sendMessage(msg);
            }
        }
        mSocketClient.send("\n");
    }

    @Override
    public void onRefresh() {
        L.e(TAG, "__onRefresh___");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                initData();
            }
        }, 0);

    }

//    private boolean isStartInitData = false;

    public class ImmediateNetStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context con, Intent arg1) {
            L.d(TAG, "NetState ___ onReceive ");
            L.d(TAG, "action  " + arg1.getAction());

            ConnectivityManager manager = (ConnectivityManager) con.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
//            NetworkInfo gprs = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
//            NetworkInfo wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (activeNetwork == null || !activeNetwork.isAvailable()) {
                if (mErrorLayout.getVisibility() != View.VISIBLE) {
                    mUnconectionLayout.setVisibility(View.VISIBLE);
                }

                isWebSocketStart = false;
                if (mSocketClient != null) {
                    mSocketClient.close();
                }
                mSocketClient = null;

            }
//            else {
//                if (!isStartInitData) {
//                    mViewHandler.sendEmptyMessage(VIEW_STATUS_LOADING);
//                    initData();
//                }
//            }
        }
    }


    public static boolean isPause = false;

    @Override
    public void onPause() {
        isPause = true;
        super.onPause();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        L.w(TAG, "immediate fragment detach..");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
      //  EventBus.getDefault().unregister(this);
        imEventBus.unregister(this);

        L.d("100", "onDestroyView");
        L.w(TAG, "immediate fragment destroy view..");
    }
}
