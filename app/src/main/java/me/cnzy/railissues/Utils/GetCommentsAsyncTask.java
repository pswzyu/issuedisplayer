package me.cnzy.railissues.Utils;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

import me.cnzy.railissues.Utils.HTTPUtility.HTTPResult;

/**
 * Created by zhang on 7/28/2015.
 */
public class GetCommentsAsyncTask extends AsyncTask<String, Void, HTTPResult> {

	public static final String TAG = "CM.SearchAT";

    private GetCommentsAsyncTaskCallback callback = null;
    private String token = null;
    private String action = null;

    private HTTPUtility http = null;

    /**
     *
     *
     * @param p_callback the activity that created this task
     * @param p_token the token marks who triggered this task
     */
    public GetCommentsAsyncTask(GetCommentsAsyncTaskCallback p_callback, String p_token) {

    	callback = p_callback;
        token = p_token;

        http = new HTTPUtility();
    }

    /**
     *
     * @param params the first param is the action, create, refine or fetch
     * for create, should have "create", search_id, img file absolute path, rect string
     * for refine
     * for fetch, params are "fetch", search_id, img_type, image_name("null" for grabcut), "cacheok"/"nocache"
     * 	
     *
     * @return
     */
    @Override
    protected HTTPResult doInBackground(String... params) {
    	
    	if (params == null) {
    		Log.d(TAG, "Params is null!");
    	} else {
    		String info = "Length:"+params.length+"|";
    		for (int step = 0; step != params.length; ++step) {
    			info += "|"+params[step]+"|";
    		}
    		Log.d(TAG, info);
    	}

        // do the post
        HTTPResult result = null;
        try {
            result = http.doPost(Config.getRepoIssuesURL(), null);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                http.cleanUp();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (result != null && result.succeeded == true) {
            return result;
        } else {
            return null;
        }


    }
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(HTTPResult result) {
        super.onPostExecute(result);

        callback.getCommentsTaskResult(token, action, result);
    }

}
