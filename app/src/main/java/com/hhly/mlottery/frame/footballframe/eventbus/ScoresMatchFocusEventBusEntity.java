package com.hhly.mlottery.frame.footballframe.eventbus;

/**
 * @author: Wangg
 * @Name：ScoresMatchFocusEventBusEntity
 * @Description:足球内页关注比赛后返回列表结果
 * @Created on:2016/12/1  15:05.
 */

public class ScoresMatchFocusEventBusEntity {
    private int fgIndex;

    public int getFgIndex() {
        return fgIndex;
    }

    public void setFgIndex(int fgIndex) {
        this.fgIndex = fgIndex;
    }

    public ScoresMatchFocusEventBusEntity(int fgIndex) {

        this.fgIndex = fgIndex;
    }
}
