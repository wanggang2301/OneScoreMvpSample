package com.hhly.mlottery.adapter.snooker;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.hhly.mlottery.R;
import com.hhly.mlottery.activity.SnookerPlayerInfoActivity;
import com.hhly.mlottery.bean.snookerbean.SnookerRankBean;

import java.util.List;

/**
 * @author: Wangg
 * @Name：SnookerRankAdapter
 * @Description:斯洛克资料库排名Adapter
 * @Created on:2017/2/20  11:04.
 */

public class SnookerRankAdapter extends BaseQuickAdapter<SnookerRankBean.WorldRankingListBean> {

    private Context mContext;

    private List<SnookerRankBean.WorldRankingListBean> worldRankingListBeen;

    public SnookerRankAdapter(Context context, List<SnookerRankBean.WorldRankingListBean> data) {
        super(R.layout.item_snooker_rank, data);
        mContext = context;
        worldRankingListBeen = data;
    }

    @Override
    protected void convert(BaseViewHolder holder, final SnookerRankBean.WorldRankingListBean w) {


        //item奇偶行不同背景颜色
        LinearLayout ll_sn = holder.getView(R.id.ll_sn);
        if (getViewHolderPosition(holder) % 2 == 0) {
            ll_sn.setBackgroundResource(R.color.white);
        } else {
            ll_sn.setBackgroundResource(R.color.snooker_rank_bg);
        }

        //top3不同颜色值
        if (getViewHolderPosition(holder) == 0 || getViewHolderPosition(holder) == 1 || getViewHolderPosition(holder) == 2) {
            ((TextView) holder.getView(R.id.tv_sn_rank)).setTextColor(mContext.getResources().getColor(R.color.snooker_rank_txt_top3));
            ((TextView) holder.getView(R.id.tv_sn_name)).setTextColor(mContext.getResources().getColor(R.color.snooker_rank_txt_top3));
            ((TextView) holder.getView(R.id.tv_sn_totalIntegral)).setTextColor(mContext.getResources().getColor(R.color.snooker_rank_txt_top3));
            ((TextView) holder.getView(R.id.tv_sn_totalBonus)).setTextColor(mContext.getResources().getColor(R.color.snooker_rank_txt_top3));
        } else {
            ((TextView) holder.getView(R.id.tv_sn_rank)).setTextColor(mContext.getResources().getColor(R.color.snooker_rank_txt));
            ((TextView) holder.getView(R.id.tv_sn_name)).setTextColor(mContext.getResources().getColor(R.color.snooker_rank_txt));
            ((TextView) holder.getView(R.id.tv_sn_totalIntegral)).setTextColor(mContext.getResources().getColor(R.color.snooker_rank_txt));
            ((TextView) holder.getView(R.id.tv_sn_totalBonus)).setTextColor(mContext.getResources().getColor(R.color.snooker_rank_txt));

        }

        holder.setText(R.id.tv_sn_rank, w.getIndex() + "");
        holder.setText(R.id.tv_sn_name, w.getName() + "");
        holder.setText(R.id.tv_sn_totalIntegral, w.getTotalIntegral() + "");
        holder.setText(R.id.tv_sn_totalBonus, w.getTotalBonus() + "");

        ll_sn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, w.getName() + "__" + w.getPlayerId(), Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(mContext, SnookerPlayerInfoActivity.class);

                intent.putExtra("playerId", w.getPlayerId());
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getViewHolderPosition(RecyclerView.ViewHolder viewHolder) {
        return super.getViewHolderPosition(viewHolder);
    }


}
