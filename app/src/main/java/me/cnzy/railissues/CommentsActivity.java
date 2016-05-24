package me.cnzy.railissues;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.LinkedList;

import me.cnzy.railissues.Utils.GetCommentsAsyncTask;
import me.cnzy.railissues.Utils.GetCommentsAsyncTaskCallback;
import me.cnzy.railissues.Utils.HTTPUtility;

public class CommentsActivity extends AppCompatActivity implements GetCommentsAsyncTaskCallback {

    public static final String TAG = "VI.CommentsActivity";

    private TextView tv_all_comments = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        tv_all_comments = (TextView) findViewById(R.id.tv_all_comments);

        Bundle intent_info = getIntent().getExtras();
        String issue_id = intent_info.getString("issue_id");

        new GetCommentsAsyncTask(this, null).execute(issue_id);
    }

    /**
     * after comments for one issue is received
     * @param token
     * @param action
     * @param reply
     */
    @Override
    public void getCommentsTaskResult(String token, String action, HTTPUtility.HTTPResult reply) {
        // format the comments
        Log.d(TAG, reply.response_text);
        StringBuilder sb = new StringBuilder();

        try {
            JSONArray comments = new JSONArray(reply.response_text);
            for (int step = 0; step < comments.length(); step++) {
                JSONObject one_comment = comments.getJSONObject(step);
                JSONObject user = one_comment.getJSONObject("user");
                if (sb.length() > 0) sb.append("\n--------");
                sb.append(user.optString("login"));
                sb.append(":\n");
                sb.append(one_comment.optString("body"));
            }

        } catch (JSONException e) {
            e.printStackTrace();
            //showFailureMessage("Failed", "Network Issues!");
            return;
        }

        // show the huge text in the text view
        tv_all_comments.setText(sb.toString());
    }
}
