package me.cnzy.railissues.Utils;

/**
 * Created by zhang on 7/29/2015.
 */
public interface GetCommentsAsyncTaskCallback {
    public void getCommentsTaskResult(String token, String action, HTTPUtility.HTTPResult reply);
}
