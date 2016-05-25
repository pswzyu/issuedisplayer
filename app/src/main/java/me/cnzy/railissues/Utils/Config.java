package me.cnzy.railissues.Utils;

/**
 * Created by pswzy on 5/23/2016.
 */
public class Config {

    public static String getRepoIssuesURL() {
        return "https://api.github.com/repos/rails/rails/issues?sort=update";
        //return "https://api.github.com/repos/pswzyu/issuedisplayer/issues?sort=update";
    }
    public static String getIssueCommentsURL(String issueID) {
        return "https://api.github.com/repos/rails/rails/issues/"+issueID+"/comments";
        //return "https://api.github.com/repos/pswzyu/issuedisplayer/issues/"+issueID+"/comments";
    }
}
