package com.hhly.mlottery.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hhly.mlottery.R;
import com.hhly.mlottery.adapter.core.BaseRecyclerViewAdapter;
import com.hhly.mlottery.adapter.core.BaseRecyclerViewHolder;
import com.hhly.mlottery.bean.Match;
import com.hhly.mlottery.bean.MatchOdd;
import com.hhly.mlottery.bean.websocket.WebSocketMatchChange;
import com.hhly.mlottery.bean.websocket.WebSocketMatchEvent;
import com.hhly.mlottery.bean.websocket.WebSocketMatchOdd;
import com.hhly.mlottery.bean.websocket.WebSocketMatchStatus;
import com.hhly.mlottery.util.AnimUtils;
import com.hhly.mlottery.util.HandicapUtils;
import com.hhly.mlottery.util.L;
import com.hhly.mlottery.util.MyConstants;
import com.hhly.mlottery.util.PreferenceUtil;
import com.hhly.mlottery.util.RxBus;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class RollBallAdapter extends BaseRecyclerViewAdapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_DEFAULT = 1;

    private int topDataCount = Integer.MAX_VALUE;
    private int handicap;
    private Map<Match, Boolean> isTopDataCacheMaps = new HashMap<>();

    private Context context;
    private boolean resetColor;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private List<Match> topLists;

    public RollBallAdapter(Context context) {
        super();
        this.context = context;
        sharedPreferences = context.getSharedPreferences("ROLLBALL_ADAPTER", Activity.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void setList(List list) {
        getList().clear();
        if (list == null) return;
        for (int i = list.size() - 1; i >= 0; i--) {
            Match data = (Match) list.get(i);
            boolean isTopData = false;
            if (isTopDataCacheMaps.containsKey(data)) {
                isTopData = isTopDataCacheMaps.get(data);
            }
            isTopDataCacheMaps.put(data, isTopData);
            int topDataCount = sharedPreferences.getInt(data.getThirdId(), 0);
            data.setIsTopData(topDataCount);
        }
        Collections.sort(list, new Match());
        getList().addAll(list);
    }

    public SharedPreferences getSharedPreperences() {
        return sharedPreferences;
    }

    @Override
    public int[] getItemLayouts() {
        return new int[]{R.layout.item_football_roll};
    }

    @Override
    public void onBindRecycleViewHolder(BaseRecyclerViewHolder viewHolder, int position) {
        int itemViewType = getRecycleViewItemType(position);
        switch (itemViewType) {
            case VIEW_TYPE_DEFAULT:
                this.bindDefaultView(viewHolder, position);
                break;
        }
    }

    @Override
    public int getRecycleViewItemType(int position) {
        return VIEW_TYPE_DEFAULT;
    }

    @SuppressLint("SetTextI18n")
    private void bindDefaultView(final BaseRecyclerViewHolder viewHolder, final int position) {
        final Match data = getItemByPosition(position);
        if (data == null) return;

        TextView tvRaceName = viewHolder.findViewById(R.id.tvRaceName);
        TextView tvHomeScore = viewHolder.findViewById(R.id.tvHomeScore);
        TextView tvTime = viewHolder.findViewById(R.id.tvTime);
        TextView tvKeepTime = viewHolder.findViewById(R.id.tvKeepTime);
        TextView tvGuestScore = viewHolder.findViewById(R.id.tvGuestScore);
        FrameLayout itemPositionControl = viewHolder.findViewById(R.id.fl_control);
        final ImageView ivItemPositionControl = viewHolder.findViewById(R.id.ivItemPositionControl);
        TextView tvHomeTeam = viewHolder.findViewById(R.id.tvHomeTeam);
        TextView tvHomeYellowCard = viewHolder.findViewById(R.id.tvHomeYellowCard);
        TextView tvHomeRedCard = viewHolder.findViewById(R.id.tvHomeRedCard);
        TextView tvHomeHalfScore = viewHolder.findViewById(R.id.tvHomeHalfScore);
        TextView tvGuestHalfScore = viewHolder.findViewById(R.id.tvGuestHalfScore);
        TextView tvGuestTeam = viewHolder.findViewById(R.id.tvGuestTeam);
        TextView tvGuestYelloCard = viewHolder.findViewById(R.id.tvGuestYelloCard);
        TextView tvGuestRedCard = viewHolder.findViewById(R.id.tvGuestRedCard);
        TextView tvLeftOdds_YA = viewHolder.findViewById(R.id.tv_odds_center1);
        TextView tvHandicapValue_YA_BLACK = viewHolder.findViewById(R.id.tv_odds_top1);
        TextView tvRightOdds_YA = viewHolder.findViewById(R.id.tv_odds_bottom1);
        TextView tvLeftOdds_DA = viewHolder.findViewById(R.id.tv_odds_center2);
        TextView tvHandicapValue_DA_BLACK = viewHolder.findViewById(R.id.tv_odds_top2);
        TextView tvRightOdds_DA = viewHolder.findViewById(R.id.tv_odds_bottom2);
        TextView keepTimeShuffle = viewHolder.findViewById(R.id.keepTimeShuffle);

        LinearLayout llOddsContent1 = viewHolder.findViewById(R.id.ll_odds_content1);
        LinearLayout llOddsContent2 = viewHolder.findViewById(R.id.ll_odds_content2);

        TextView tvRollDesc = viewHolder.findViewById(R.id.tv_roll_desc);// 描述

        // 初始化数据
        if (TextUtils.isEmpty(data.getKeepTime())) data.setKeepTime("0");
        this.initializedTextColor(tvKeepTime, tvGuestScore, tvHomeScore, tvHandicapValue_DA_BLACK, tvHandicapValue_YA_BLACK, tvLeftOdds_DA, tvLeftOdds_YA, tvRightOdds_DA, tvRightOdds_YA);
        tvLeftOdds_DA.setTextColor(context.getResources().getColor(R.color.res_name_color));
        tvLeftOdds_YA.setTextColor(context.getResources().getColor(R.color.res_name_color));
        keepTimeShuffle.setTextColor(context.getResources().getColor(R.color.text_about_color));


        // 置顶
        if (data.getIsTopData() > 0 || isTopDataCacheMaps.get(data)) {
            ivItemPositionControl.setImageResource(R.mipmap.roll_default);
        } else {
            ivItemPositionControl.setImageResource(R.mipmap.roll_top);
        }

        // webSocket实时推送事件动画处理
        if (data.getSocketPushType() != null) {
            switch (data.getSocketPushType()) {
                case STATUS:
                    break;
                case ODDS:
                    break;
                case EVENT:
                    this.setupEventAnimator(data, tvHomeScore, tvGuestScore);
                    break;
                case MATCHCHANGE:
                    this.setupMatchChangeAnimator(data, tvHomeTeam, tvGuestTeam);
                    break;
            }
        }

        //第一次进来根据文件选择选中哪个。默认是亚盘和欧赔
        boolean asize = PreferenceUtil.getBoolean(MyConstants.rbSizeBall, false);
        boolean eur = PreferenceUtil.getBoolean(MyConstants.RBOCOMPENSATE, true);
        boolean alet = PreferenceUtil.getBoolean(MyConstants.RBSECOND, true);
        boolean noshow = PreferenceUtil.getBoolean(MyConstants.RBNOTSHOW, false);

        L.d("aaaaa", "asize: " + asize + "   eur:" + eur + "   alet:" + alet + "   noshow:" + noshow);

        // 隐藏赔率
        if (noshow) {
            llOddsContent1.setVisibility(View.GONE);
            llOddsContent2.setVisibility(View.GONE);
            L.d("aaaaa", "都不显示！");
        } else if ((asize && eur) || (asize && alet) || (eur && alet)) {
            llOddsContent1.setVisibility(View.VISIBLE);
            llOddsContent2.setVisibility(View.VISIBLE);
            L.d("aaaaa", "显示两个！");
        } else {
            llOddsContent1.setVisibility(View.VISIBLE);
            llOddsContent2.setVisibility(View.GONE);
            L.d("aaaaa", "显示一个！");
        }


        // 赔率
        Map<String, MatchOdd> matchOddMap = data.getMatchOdds();
        MatchOdd asiaSize = null, euro = null, asiaLet = null;
        if (matchOddMap != null && matchOddMap.size() > 0) {
            for (Map.Entry<String, MatchOdd> matchOdd : matchOddMap.entrySet()) {
                switch (matchOdd.getKey()) {
                    case "asiaSize":// 大小盘数据
                        asiaSize = matchOddMap.get("asiaSize");
                        L.d("sssss", "大小盘口有数据 !");
                        break;
                    case "euro":// 欧赔数据
                        euro = matchOddMap.get("euro");
                        L.d("sssss", "欧赔盘口有数据 !");
                        break;
                    case "asiaLet":// 亚赔数据
                        asiaLet = matchOddMap.get("asiaLet");
                        L.d("sssss", "亚赔盘口有数据 !");
                        break;
                }
            }
        }

        L.d("sssss", "asize: " + asize + "   eur:" + eur + "   alet:" + alet + "   noshow:" + noshow);
        // 亚盘赔率
        if (alet) {
            setOddsItemData(tvHandicapValue_YA_BLACK, tvLeftOdds_YA, tvRightOdds_YA, asiaLet, data);
        }
        // 大小盘赔率
        if (asize) {
            if (!alet) {
                setOddsItemData(tvHandicapValue_YA_BLACK, tvLeftOdds_YA, tvRightOdds_YA, asiaSize, data);
            } else {
                setOddsItemData(tvHandicapValue_DA_BLACK, tvLeftOdds_DA, tvRightOdds_DA, asiaSize, data);
            }
        }
        // 欧盘赔率
        if (eur) {
            if (!alet && !asize) {
                setOddsItemData(tvHandicapValue_YA_BLACK, tvLeftOdds_YA, tvRightOdds_YA, euro, data);
            } else {
                setOddsItemData(tvHandicapValue_DA_BLACK, tvLeftOdds_DA, tvRightOdds_DA, euro, data);
            }
        }

        // 赔率的颜色变化状态
        if (Integer.parseInt(data.getKeepTime()) < 89) { // 手动操控封盘状态，时间如果大于89 则不显示颜色变化，防止在“--”状态下，后台推送的数据并非“--”，而显示颜色变化
            switch (handicap) {
                case 1: // 亚盘
                    if (!asiaLet.getLeftOdds().equals("-") && alet) {
                        setupOddTextColor(data, tvLeftOdds_YA, tvHandicapValue_YA_BLACK, tvRightOdds_YA);
                    }
                    break;
                case 2: // 大小球
                    if (!asiaSize.getLeftOdds().equals("-") && asize) {
                        if (!alet) {
                            setupOddTextColor(data, tvLeftOdds_YA, tvHandicapValue_YA_BLACK, tvRightOdds_YA);
                        } else {
                            setupOddTextColor(data, tvLeftOdds_DA, tvHandicapValue_DA_BLACK, tvRightOdds_DA);
                        }
                    }
                    break;
                case 3: // 欧赔
                    if (!euro.getLeftOdds().equals("-") && eur) {
                        if (!alet && !asize) {
                            setupOddTextColor(data, tvLeftOdds_YA, tvHandicapValue_YA_BLACK, tvRightOdds_YA);
                        } else {
                            setupOddTextColor(data, tvLeftOdds_DA, tvHandicapValue_DA_BLACK, tvRightOdds_DA);
                        }
                    }
                    break;
            }
        }

        // 比赛状态
        switch (data.getStatusOrigin()) {
            case "0": // 未开赛
                tvHomeScore.setVisibility(View.GONE);
                tvGuestScore.setVisibility(View.GONE);
                tvHomeHalfScore.setVisibility(View.GONE);
                tvGuestHalfScore.setVisibility(View.GONE);

                tvKeepTime.setText(context.getString(R.string.snooker_state_no_start));
                keepTimeShuffle.setVisibility(View.GONE);

                break;
            case "1": // 上半场进行时间

                if (Integer.parseInt(data.getKeepTime()) > 45) {
                    this.setupKeepTimeStyle(tvKeepTime, keepTimeShuffle, "45+", R.color.football_keeptime, true);

                } else {
                    tvKeepTime.setText(data.getKeepTime());
                    tvKeepTime.setTextColor(context.getResources().getColor(R.color.text_about_color));

                }


                this.startShuffleAnimation(keepTimeShuffle);

                tvKeepTime.setTextColor(context.getResources().getColor(R.color.text_about_color));
                tvGuestScore.setTextColor(context.getResources().getColor(R.color.text_about_color));
                tvHomeScore.setTextColor(context.getResources().getColor(R.color.text_about_color));

                tvHomeScore.setVisibility(View.VISIBLE);
                tvGuestScore.setVisibility(View.VISIBLE);
                tvHomeHalfScore.setVisibility(View.VISIBLE);
                tvGuestHalfScore.setVisibility(View.VISIBLE);
                break;
            case "2": // 中场
                this.setupKeepTimeStyle(tvKeepTime, keepTimeShuffle, context.getString(R.string.immediate_status_midfield), R.color.football_keeptime, false);
                tvHomeScore.setVisibility(View.VISIBLE);
                tvGuestScore.setVisibility(View.VISIBLE);
                tvHomeHalfScore.setVisibility(View.VISIBLE);
                tvGuestHalfScore.setVisibility(View.VISIBLE);

                tvKeepTime.setTextColor(context.getResources().getColor(R.color.text_about_color));
                tvGuestScore.setTextColor(context.getResources().getColor(R.color.text_about_color));
                tvHomeScore.setTextColor(context.getResources().getColor(R.color.text_about_color));
                break;
            case "3": // 下半场进行时间
                if (Integer.parseInt(data.getKeepTime()) > 90) {
                    this.setupKeepTimeStyle(tvKeepTime, keepTimeShuffle, "90+", R.color.football_keeptime, true);
                } else {
                    tvKeepTime.setText(data.getKeepTime());
                    tvKeepTime.setTextColor(context.getResources().getColor(R.color.text_about_color));

                }

                this.startShuffleAnimation(keepTimeShuffle);
                tvHomeScore.setVisibility(View.VISIBLE);
                tvGuestScore.setVisibility(View.VISIBLE);
                tvHomeHalfScore.setVisibility(View.VISIBLE);
                tvGuestHalfScore.setVisibility(View.VISIBLE);

                tvGuestScore.setTextColor(context.getResources().getColor(R.color.text_about_color));
                tvHomeScore.setTextColor(context.getResources().getColor(R.color.text_about_color));
                break;
            case "4": // 加时
                this.setupKeepTimeStyle(tvKeepTime, keepTimeShuffle, context.getString(R.string.immediate_status_overtime), R.color.football_keeptime, false);
                tvHomeScore.setVisibility(View.VISIBLE);
                tvGuestScore.setVisibility(View.VISIBLE);
                tvHomeHalfScore.setVisibility(View.VISIBLE);
                tvGuestHalfScore.setVisibility(View.VISIBLE);

                tvGuestScore.setTextColor(context.getResources().getColor(R.color.text_about_color));
                tvHomeScore.setTextColor(context.getResources().getColor(R.color.text_about_color));
                break;
            case "5": // 点球
                this.setupKeepTimeStyle(tvKeepTime, keepTimeShuffle, context.getString(R.string.immediate_status_point), R.color.football_keeptime, false);
                tvHomeScore.setVisibility(View.VISIBLE);
                tvGuestScore.setVisibility(View.VISIBLE);
                tvHomeHalfScore.setVisibility(View.VISIBLE);
                tvGuestHalfScore.setVisibility(View.VISIBLE);
                break;
            case "-1": // 完场
                this.setupKeepTimeStyle(tvKeepTime, keepTimeShuffle, context.getString(R.string.immediate_status_end), R.color.red, false);
                tvHomeScore.setTextColor(context.getResources().getColor(R.color.red));
                tvGuestScore.setTextColor(context.getResources().getColor(R.color.red));
                tvHomeScore.setVisibility(View.VISIBLE);
                tvGuestScore.setVisibility(View.VISIBLE);
                tvHomeHalfScore.setVisibility(View.VISIBLE);
                tvGuestHalfScore.setVisibility(View.VISIBLE);
                break;
            case "-10": // 取消

                this.setupKeepTimeStyle(tvKeepTime, keepTimeShuffle, context.getString(R.string.immediate_status_cancel), R.color.red, false);
                tvHomeScore.setVisibility(View.GONE);
                tvGuestScore.setVisibility(View.GONE);
                tvHomeHalfScore.setVisibility(View.GONE);
                tvGuestHalfScore.setVisibility(View.GONE);

                tvGuestScore.setTextColor(context.getResources().getColor(R.color.text_about_color));
                tvHomeScore.setTextColor(context.getResources().getColor(R.color.text_about_color));
                break;
            case "-11": // 待定

                this.setupKeepTimeStyle(tvKeepTime, keepTimeShuffle, context.getString(R.string.immediate_status_hold), R.color.red, false);
                tvHomeScore.setVisibility(View.GONE);
                tvGuestScore.setVisibility(View.GONE);
                tvHomeHalfScore.setVisibility(View.GONE);
                tvGuestHalfScore.setVisibility(View.GONE);

                tvGuestScore.setTextColor(context.getResources().getColor(R.color.text_about_color));
                tvHomeScore.setTextColor(context.getResources().getColor(R.color.text_about_color));
                break;
            case "-12": // 腰斩
                tvHomeScore.setVisibility(View.VISIBLE);
                tvGuestScore.setVisibility(View.VISIBLE);
                tvHomeHalfScore.setVisibility(View.VISIBLE);
                tvGuestHalfScore.setVisibility(View.VISIBLE);

                tvGuestScore.setTextColor(context.getResources().getColor(R.color.text_about_color));
                tvHomeScore.setTextColor(context.getResources().getColor(R.color.text_about_color));

                this.setupKeepTimeStyle(tvKeepTime, keepTimeShuffle, context.getString(R.string.immediate_status_cut), R.color.red, false);
                break;
            case "-13": // 中断
                tvHomeScore.setVisibility(View.VISIBLE);
                tvGuestScore.setVisibility(View.VISIBLE);
                tvHomeHalfScore.setVisibility(View.VISIBLE);
                tvGuestHalfScore.setVisibility(View.VISIBLE);

                tvGuestScore.setTextColor(context.getResources().getColor(R.color.text_about_color));
                tvHomeScore.setTextColor(context.getResources().getColor(R.color.text_about_color));

                this.setupKeepTimeStyle(tvKeepTime, keepTimeShuffle, context.getString(R.string.immediate_status_mesomere), R.color.bg_header, false);
                break;
            case "-14": // 推迟
                tvHomeScore.setVisibility(View.GONE);
                tvGuestScore.setVisibility(View.GONE);
                tvHomeHalfScore.setVisibility(View.GONE);
                tvGuestHalfScore.setVisibility(View.GONE);

                tvGuestScore.setTextColor(context.getResources().getColor(R.color.text_about_color));
                tvHomeScore.setTextColor(context.getResources().getColor(R.color.text_about_color));

                this.setupKeepTimeStyle(tvKeepTime, keepTimeShuffle, context.getString(R.string.immediate_status_postpone), R.color.red, false);
                break;
        }

        // 杯赛名称
        tvRaceName.setText(data.getRacename());
        tvRaceName.setTextColor(Color.parseColor(data.getRaceColor()));
        // 主队分数
        tvHomeScore.setText(data.getHomeScore());
        // 开赛时间
        tvTime.setText(data.getTime());
        // 客队分数
        tvGuestScore.setText(data.getGuestScore());
        // 主队名称
        tvHomeTeam.setText(data.getHometeam());
        // 主队黄牌数
        if (!data.getHome_yc().equals("0")) {
            tvHomeYellowCard.setVisibility(View.VISIBLE);
            tvHomeYellowCard.setText(data.getHome_yc());
        } else tvHomeYellowCard.setVisibility(View.INVISIBLE);
        // 主队红牌数
        if (!data.getHome_rc().equals("0")) {
            tvHomeRedCard.setVisibility(View.VISIBLE);
            tvHomeRedCard.setText(data.getHome_rc());
        } else tvHomeRedCard.setVisibility(View.INVISIBLE);
        // 半场主队得分数
        tvHomeHalfScore.setText(data.getHomeHalfScore());
        // 半场客服的分数
        tvGuestHalfScore.setText(data.getGuestHalfScore());
        // 客队名称
        tvGuestTeam.setText(data.getGuestteam());
        // 客队黄牌数
        if (!data.getGuest_yc().equals("0")) {
            tvGuestYelloCard.setVisibility(View.VISIBLE);
            tvGuestYelloCard.setText(data.getGuest_yc());
        } else tvGuestYelloCard.setVisibility(View.INVISIBLE);
        // 客队红牌数
        if (!data.getGuest_rc().equals("0")) {
            tvGuestRedCard.setVisibility(View.VISIBLE);
            tvGuestRedCard.setText(data.getGuest_rc());
        } else tvGuestRedCard.setVisibility(View.INVISIBLE);

        // 控制器
        itemPositionControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (data.getIsTopData() > 0) {
                    editor.putInt(data.getThirdId(), 0);
                    data.setIsTopData(0);
                    if (isTopDataCacheMaps.containsKey(data)) isTopDataCacheMaps.put(data, false);
                } else {
                    editor.putInt(data.getThirdId(), topDataCount--);
                    data.setIsTopData(topDataCount--);
                    if (isTopDataCacheMaps.containsKey(data)) isTopDataCacheMaps.put(data, true);
                }
                editor.commit();
                RollBallAdapter.this.transformMapper(position);
            }
        });

        // 完场描述
        tvRollDesc.setVisibility(TextUtils.isEmpty(data.getTxt()) ? View.GONE : View.VISIBLE);
        String name = data.getWinner() == data.getHomeId() ? data.getHometeam() : data.getGuestteam();
        tvRollDesc.setText(data.getTxt() + "," + name + context.getResources().getString(R.string.roll_desc_txt));
    }

    /**
     * 更新赔率数据
     *
     * @param tvMedium
     * @param tvLeft
     * @param tvRight
     * @param odds
     * @param data
     */
    private void setOddsItemData(TextView tvMedium, TextView tvLeft, TextView tvRight, MatchOdd odds, Match data) {
        L.d("sssss", "进来了 odds：" + odds);
        if (odds != null) {

            tvLeft.setText((Integer.parseInt(data.getKeepTime()) < 89 ? ("-".equals(odds.getMediumOdds()) ? "--" : odds.getMediumOdds() == null ? "--" : odds.getMediumOdds()) : "--")); // 中

            tvMedium.setText((Integer.parseInt(data.getKeepTime()) < 89 ? ("-".equals(odds.getLeftOdds()) ? context.getResources().getString(R.string.basket_handicap_feng) : odds.getLeftOdds() == null ? "--" : odds.getLeftOdds()) : context.getResources().getString(R.string.basket_handicap_feng))); // 上

            tvRight.setText((Integer.parseInt(data.getKeepTime()) < 89 ? ("-".equals(odds.getRightOdds()) ? "--" : odds.getRightOdds() == null ? "--" : odds.getRightOdds()) : "--")); // 下
        } else {
            this.setText(tvLeft, tvMedium, tvRight);
        }
        if (tvMedium.getText().equals(context.getResources().getString(R.string.basket_handicap_feng))) { // 如果当前状态是封盘，那么改变条目的背景颜色及文字颜色
            tvMedium.setBackgroundColor(context.getResources().getColor(R.color.item_background));
            tvMedium.setTextColor(context.getResources().getColor(R.color.white));
            tvLeft.setTextColor(context.getResources().getColor(R.color.res_pl_color));
        }
    }

    private void startShuffleAnimation(final TextView keepTimeShuffle) {
        keepTimeShuffle.setText("\'");
        keepTimeShuffle.setVisibility(View.VISIBLE);
        final AlphaAnimation alphaAnimation1 = new AlphaAnimation(1, 1);
        alphaAnimation1.setDuration(500);
        final AlphaAnimation alphaAnimation0 = new AlphaAnimation(0, 0);
        alphaAnimation0.setDuration(500);
        alphaAnimation1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                keepTimeShuffle.startAnimation(alphaAnimation0);
            }
        });

        alphaAnimation0.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                keepTimeShuffle.startAnimation(alphaAnimation1);

            }
        });
        keepTimeShuffle.startAnimation(alphaAnimation1);
    }

    private void setText(TextView... textViews) {
        for (TextView textView : textViews) {
            textView.setText(" ");
        }
    }

    private void setupKeepTimeStyle(TextView keepTime, TextView keepTimeShuffle, String text, int color, boolean isShowShuffle) {
        if (isShowShuffle) {
            keepTimeShuffle.setVisibility(View.VISIBLE);
        } else {
            keepTimeShuffle.setVisibility(View.GONE);
            keepTimeShuffle.clearAnimation();
        }
        keepTime.setText(text);
        keepTime.setTextColor(context.getResources().getColor(color));
        keepTimeShuffle.setTextColor(context.getResources().getColor(color));
    }

    private void setupEventAnimator(Match match, TextView homeScore, TextView guestScore) {
        if (match.getHomeTeamTextColorId() != 0) {
            homeScore.setTextColor(context.getResources().getColor(match.getHomeTeamTextColorId()));
            AnimUtils.toBigAnim2(homeScore);
            match.setHomeTeamTextColorId(0);
        } else if (match.getGuestTeamTextColorId() != 0) {
            guestScore.setTextColor(context.getResources().getColor(match.getGuestTeamTextColorId()));
            AnimUtils.toBigAnim2(guestScore);
            match.setGuestTeamTextColorId(0);
        }
        match.setSocketPushType(null);
    }

    private void setupMatchChangeAnimator(Match match, View... views) {
        for (View view : views) {
            AnimUtils.toBigAnim2(view);
        }
        match.setSocketPushType(null);
    }

    private void setVisiableStateOfThisViews(View... views) {
        for (View view : views) {
            view.setVisibility(View.VISIBLE);
        }
    }

    // 置顶颜色
    private void showShadow(final View view, float alpha) {
        view.setAlpha(alpha);
    }

    // 置顶操作
    private void transformMapper(int position) {
        topLists = getList();
        Match data = topLists.get(position);
        topLists.remove(data);
        topLists.add(0, data);
        Collections.sort(topLists, new Match());
        notifyDataSetChanged();
    }

    public List<Match> getTopLists() {
        return topLists;
    }

    // WebSocket推送消息分发处理
    public void updateItemFromWebSocket(Object o) {
        List<Match> matchList = getList();
        /*if (!notify_locked_tag) {*/
        for (int i = 0; i < matchList.size(); i++) {
            Match match = matchList.get(i);
            if (o instanceof WebSocketMatchStatus) {
                this.updateTypeStatus(match, i, (WebSocketMatchStatus) o);
            } else if (o instanceof WebSocketMatchOdd) {
                this.updateTypeOdds(match, i, (WebSocketMatchOdd) o);
            } else if (o instanceof WebSocketMatchEvent) {
                this.updateTypeEvent(match, i, (WebSocketMatchEvent) o);
            } else if (o instanceof WebSocketMatchChange) {
                this.updateTypeMatchChange(match, i, (WebSocketMatchChange) o);
            }
        }
    }

    // 状态推送
    private void updateTypeStatus(Match match, int position, WebSocketMatchStatus webSocketMatchStatus) {
        final Match[] target = new Match[1];
        Map<String, String> data = webSocketMatchStatus.getData();

        if (match.getThirdId().equals(webSocketMatchStatus.getThirdId())) {
            target[0] = match;

            //应该这样写吧  由未开场变成开场
            if ("0".equals(match.getStatusOrigin()) && "1".equals(data.get("statusOrigin"))) {// 未开场变成开场
                match.setHomeScore("0");
                match.setGuestScore("0");
            }

            match.setStatusOrigin(data.get("statusOrigin"));

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

            this.WebSocketMarkTopStatu(match);
            getList().set(position, match);
            notifyItemChanged(position);
        }

        // 界面1分钟后置于列表底部
        if (target[0] != null) {
            /** -10：取消，1分钟后清除,-12：腰斩，1分钟后清除,-14：推迟，1分钟后清除 */
            if ("-1".equals(target[0].getStatusOrigin()) || "-10".equals(target[0].getStatusOrigin()) || "-12".equals(target[0].getStatusOrigin()) || "-14".equals(target[0].getStatusOrigin())) {
                RxBus.getDefault().post(target[0]);
            }
        }
    }

    // 赔率推送
    private void updateTypeOdds(final Match match, final int position, WebSocketMatchOdd webSocketMatchOdd) {
        if (match.getThirdId().equals(webSocketMatchOdd.getThirdId())) {
            final List<Map<String, String>> matchOddDataLists = webSocketMatchOdd.getData();
            for (Map<String, String> oddDataMaps : matchOddDataLists) {
                // 亚赔
                if (oddDataMaps.get("handicap").equals("asiaLet")) {
                    handicap = 1;
                    MatchOdd asiaLetOdd = this.getMatchOdd(match, "asiaLet");
                    if (asiaLetOdd != null)
                        this.changeOddTextColor(match, asiaLetOdd.getLeftOdds(), oddDataMaps.get("leftOdds"), asiaLetOdd.getRightOdds(), oddDataMaps.get("rightOdds"), asiaLetOdd.getHandicapValue(), oddDataMaps.get("mediumOdds"));
                    else
                        this.changeOddTextColor(match, "0.00", oddDataMaps.get("leftOdds"), "0.00", oddDataMaps.get("rightOdds"), "0.00", oddDataMaps.get("mediumOdds"));
                    this.updateMatchOdd(match, asiaLetOdd, oddDataMaps, "asiaLet");
                    // 欧赔
                } else if (oddDataMaps.get("handicap").equals("euro")) {
                    handicap = 3;
                    MatchOdd euroOdd = this.getMatchOdd(match, "euro");
                    if (euroOdd != null)
                        this.changeOddTextColor(match, euroOdd.getLeftOdds(), oddDataMaps.get("leftOdds"), euroOdd.getRightOdds(), oddDataMaps.get("rightOdds"), euroOdd.getMediumOdds(), oddDataMaps.get("mediumOdds"));
                    else
                        this.changeOddTextColor(match, "0.00", oddDataMaps.get("leftOdds"), "0.00", oddDataMaps.get("rightOdds"), "0.00", oddDataMaps.get("mediumOdds"));
                    this.updateMatchOdd(match, euroOdd, oddDataMaps, "euro");
                    // 大小
                } else if (oddDataMaps.get("handicap").equals("asiaSize")) {
                    handicap = 2;
                    MatchOdd asiaSizeOdd = this.getMatchOdd(match, "asiaSize");
                    if (asiaSizeOdd != null)
                        this.changeOddTextColor(match, asiaSizeOdd.getLeftOdds(), oddDataMaps.get("leftOdds"), asiaSizeOdd.getRightOdds(), oddDataMaps.get("rightOdds"), asiaSizeOdd.getHandicapValue(), oddDataMaps.get("mediumOdds"));
                    else
                        this.changeOddTextColor(match, "0.00", oddDataMaps.get("leftOdds"), "0.00", oddDataMaps.get("rightOdds"), "0.00", oddDataMaps.get("mediumOdds"));
                    this.updateMatchOdd(match, asiaSizeOdd, oddDataMaps, "asiaSize");
                }
            }
            this.WebSocketMarkTopStatu(match);
            getList().set(position, match);
            notifyItemChanged(position);

            Observable.timer(5, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Long>() {
                @Override
                public void call(Long aLong) {
                    resetColor = true;
                    notifyItemChanged(position);
                }
            });
        }
    }

    // 事件推送
    private void updateTypeEvent(final Match match, final int position, WebSocketMatchEvent webSocketMatchEvent) {
        if (match.getThirdId().equals(webSocketMatchEvent.getThirdId())) {
            match.setItemBackGroundColorId(R.color.item_football_event_yellow);
            String eventType = webSocketMatchEvent.getData().get("eventType");
            if ("1".equals(eventType) || "2".equals(eventType) || "5".equals(eventType) || "6".equals(eventType)) {// 主队有效进球传
                // 客队有效进球5 or 客队进球取消6
                if (webSocketMatchEvent.getData().get("homeScore") != null) {
                    match.setHomeScore(webSocketMatchEvent.getData().get("homeScore"));
                }
                if (webSocketMatchEvent.getData().get("guestScore") != null) {
                    match.setGuestScore(webSocketMatchEvent.getData().get("guestScore"));
                }
            } else if ("3".equals(eventType) || "4".equals(eventType) || "7".equals(eventType) || "8".equals(
                    eventType)) {// 主队红牌3 or 主队红牌取消4
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
            this.WebSocketMarkTopStatu(match);
            match.setSocketPushType(Match.SOCKET_PUSH_TYPE.EVENT);
            getList().set(position, match);
            notifyItemChanged(position);

            // 五秒后把颜色修改回来
            Observable.timer(5000, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Long>() {
                @Override
                public void call(Long aLong) {
                    notifyItemChanged(position);
                }
            });
        }
    }

    // 比赛变换推送
    private void updateTypeMatchChange(Match match, int position, WebSocketMatchChange webSocketMatchChange) {
        if (match.getThirdId().equals(webSocketMatchChange.getThirdId())) {
            String language = PreferenceUtil.getString("language", "rCN");
            if ((webSocketMatchChange.getData().get("region").equals("zh") && language.equals(
                    "rCN")) || (webSocketMatchChange.getData().get("region").equals("zh-TW") && language.equals(
                    "rTW")) || (webSocketMatchChange.getData().get("region").equals("en") && language.equals(
                    "rEN")) || (webSocketMatchChange.getData().get("region").equals("ko") && language.equals(
                    "rKO")) || (webSocketMatchChange.getData().get("region").equals("id") && language.equals(
                    "rID")) || (webSocketMatchChange.getData().get("region").equals("th") && language.equals(
                    "rTH")) || (webSocketMatchChange.getData().get("region").equals("vi") && language.equals("rVI"))) {

                if (webSocketMatchChange.getData().get("hometeam") != null) {
                    match.setHometeam(webSocketMatchChange.getData().get("hometeam"));
                }

                if (webSocketMatchChange.getData().get("guestteam") != null) {
                    match.setGuestteam(webSocketMatchChange.getData().get("guestteam"));
                }

                if (webSocketMatchChange.getData().get("racename") != null) {
                    match.setRacename(webSocketMatchChange.getData().get("racename"));
                }
                this.WebSocketMarkTopStatu(match);
                match.setSocketPushType(Match.SOCKET_PUSH_TYPE.MATCHCHANGE);
                getList().set(position, match);
                notifyItemChanged(position);
            }
        }
    }

    // 当webSocket推送新消息过来时，要将原来置顶的消息继续保持置顶状态
    private void WebSocketMarkTopStatu(Match match) {
        for (Map.Entry<Match, Boolean> entry : isTopDataCacheMaps.entrySet()) {
            if (entry.getKey().equals(match)) {
                isTopDataCacheMaps.put(match, entry.getValue());
            }
        }
    }

    private MatchOdd getMatchOdd(Match match, String handicap) {
        return match.getMatchOdds().get(handicap);
    }

    // 赔率推送消息
    private void changeOddTextColor(Match match, String oldLeftOdds, String newLeftOdds, String oldRightOdds, String newRightOdds, String oldHandicapValue, String newHandicapValue) {
        float leftMatchOddF = oldLeftOdds != null && !oldLeftOdds.equals("-") ? Float.parseFloat(oldLeftOdds) : (float) 0.00;
        float rightMatchOddF = oldRightOdds != null && !oldRightOdds.equals("-") ? Float.parseFloat(oldRightOdds) : (float) 0.00;
        float leftMapOddF = Float.parseFloat(!newLeftOdds.equals("-") ? newLeftOdds : "0.00");
        float rightMapOddF = Float.parseFloat(!newRightOdds.equals("-") ? newRightOdds : "0.00");
        float midMatchOddF = oldHandicapValue != null && !oldHandicapValue.equals("-") ? Float.parseFloat(oldHandicapValue) : (float) 0.00;
        float midMapOddF = Float.parseFloat(!newHandicapValue.equals("-") ? newHandicapValue : "0.00");

        if (leftMatchOddF < leftMapOddF && leftMapOddF != 0.00) {// 左边的值升了
            match.setLeftOddTextColorId(R.color.odds_up_bg);
        } else if (leftMatchOddF > leftMapOddF && leftMapOddF != 0.00) {// 左边的值降了
            match.setLeftOddTextColorId(R.color.odds_down_bg);
        } else {
            match.setLeftOddTextColorId(R.color.white);
        }

        if (rightMatchOddF < rightMapOddF && rightMapOddF != 0.00) {// 右边的值升了
            match.setRightOddTextColorId(R.color.odds_up_bg);
        } else if (rightMatchOddF > rightMapOddF && rightMapOddF != 0.00) {// 右边的值降了
            match.setRightOddTextColorId(R.color.odds_down_bg);
        } else {
            match.setRightOddTextColorId(R.color.white);
        }

        if (midMatchOddF < midMapOddF && midMapOddF != 0.00) {// 中间的值升了
            match.setMidOddTextColorId(R.color.odds_up_bg);
        } else if (midMatchOddF > midMapOddF && midMapOddF != 0.00) {// 中间的值降了
            match.setMidOddTextColorId(R.color.odds_down_bg);
        } else {
            match.setMidOddTextColorId(R.color.white);
        }
    }

    private void setupOddTextColor(Match data, TextView leftOdds, TextView midOdds, TextView rightOdds) {
        if (!resetColor) {
            if (data.getMidOddTextColorId() != 0) {
                leftOdds.setBackgroundResource(data.getMidOddTextColorId());
                if (data.getMidOddTextColorId() != R.color.white) // 不是平 一定是红升绿降
                    leftOdds.setTextColor(context.getResources().getColor(R.color.white));
                data.setMidOddTextColorId(0);
            }
            if (data.getRightOddTextColorId() != 0) {
                rightOdds.setBackgroundResource(data.getRightOddTextColorId());
                if (data.getRightOddTextColorId() != R.color.white)
                    rightOdds.setTextColor(context.getResources().getColor(R.color.white));
                data.setRightOddTextColorId(0);
            }
            if (data.getLeftOddTextColorId() != 0) {
                midOdds.setBackgroundResource(data.getLeftOddTextColorId());
                if (data.getLeftOddTextColorId() != R.color.white)
                    midOdds.setTextColor(context.getResources().getColor(R.color.white));
                data.setLeftOddTextColorId(0);
            }
        } else {
            resetColor = false;
            leftOdds.setBackgroundResource(R.color.white);
            midOdds.setBackgroundResource(R.color.white);
            rightOdds.setBackgroundResource(R.color.white);
        }
    }

    private void initializedTextColor(TextView keepTime, TextView homeScore, TextView guestScore, TextView... textViews) {
        keepTime.setTextColor(context.getResources().getColor(R.color.res_pl_color));
        homeScore.setTextColor(context.getResources().getColor(R.color.res_pl_color));
        guestScore.setTextColor(context.getResources().getColor(R.color.res_pl_color));
        for (TextView tv : textViews) {
            tv.setBackgroundColor(context.getResources().getColor(R.color.white));
            tv.setTextColor(context.getResources().getColor(R.color.res_pl_color));
        }
    }


    private void updateMatchOdd(Match match, MatchOdd matchOdd, Map<String, String> oddDataMaps, String key) {
        if (matchOdd == null) {
            matchOdd = new MatchOdd(key, oddDataMaps.get("mediumOdds"), oddDataMaps.get("rightOdds"), oddDataMaps.get("leftOdds"));
            match.getMatchOdds().put(key, matchOdd);
        } else {
            if (!key.equals("euro")) {
                matchOdd.setTypeOddds(oddDataMaps.get("mediumOdds"), oddDataMaps.get("rightOdds"), oddDataMaps.get("leftOdds"));
            } else {
                matchOdd.setEuroTypeOdds(oddDataMaps.get("mediumOdds"), oddDataMaps.get("rightOdds"), oddDataMaps.get("leftOdds"));
            }
        }
    }
}
