package com.hhly.mlottery.bean;

import java.util.List;

/**
 * @ClassName: OneScoreGit
 * @author:Administrator luyao
 * @Description:
 * @data: 2016/8/9 11:58
 */
public class BasketSerach {

    /**
     * leagueName : 女世锦U21
     * leagueId : 39
     */

    public List<ResultListBean> resultList;

    public static class ResultListBean {
        public String leagueName;
        public String leagueId;
    }
}
