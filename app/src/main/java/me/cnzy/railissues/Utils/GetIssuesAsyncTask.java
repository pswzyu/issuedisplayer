package me.cnzy.railissues.Utils;

import java.io.IOException;

import android.os.AsyncTask;
import android.util.Log;
import me.cnzy.railissues.Utils.HTTPUtility.HTTPResult;

/**
 * Created by zhang on 7/28/2015.
 */
public class GetIssuesAsyncTask extends AsyncTask<String, Void, HTTPResult> {

	public static final String TAG = "ID.GetIssuesAsyncTask";
	
    private GetIssuesAsyncTaskCallback callback = null;
    private String token = null;
    private String action = null;

    private HTTPUtility http = null;

    /**
     *
     *
     * @param p_callback the activity that created this task
     * @param p_token the token marks who triggered this task
     */
    public GetIssuesAsyncTask(GetIssuesAsyncTaskCallback p_callback, String p_token) {

    	callback = p_callback;
        token = p_token;

        http = new HTTPUtility();
    }

    /**
     *
     * @param params should contain only one element, the number of the issue
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

        if (params == null || params.length == 0) return null;

        // do the post
        HTTPResult result = null;
        try {
            result = http.doGet(params[0], null);
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

        callback.getIssuesTaskResult(token, action, result);
    }

}
