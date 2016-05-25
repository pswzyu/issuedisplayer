package me.cnzy.railissues;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.LinkedList;
import java.util.List;

import me.cnzy.railissues.Utils.Config;
import me.cnzy.railissues.Utils.GetCommentsAsyncTask;
import me.cnzy.railissues.Utils.GetCommentsAsyncTaskCallback;
import me.cnzy.railissues.Utils.HTTPUtility;

public class CommentsActivity extends AppCompatActivity implements GetCommentsAsyncTaskCallback, View.OnClickListener {

    public static final String TAG = "VI.CommentsActivity";

    private String next_page_link = null;
    private String prev_page_link = null;

    private TextView tv_all_comments = null;
    private Button btn_next_page = null;
    private Button btn_prev_page = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        tv_all_comments = (TextView) findViewById(R.id.tv_all_comments);
        btn_next_page = (Button) findViewById(R.id.btn_next_page);
        btn_prev_page = (Button) findViewById(R.id.btn_prev_page);
        btn_next_page.setOnClickListener(this);
        btn_prev_page.setOnClickListener(this);

        Bundle intent_info = getIntent().getExtras();
        String issue_id = intent_info.getString("issue_id");

        new GetCommentsAsyncTask(this, null).execute(Config.getIssueCommentsURL(issue_id));
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
        if (reply == null || !reply.succeeded) return;
        Log.d(TAG, reply.response_text);

        next_page_link = null;
        prev_page_link = null;

        // if there are linkes provided for next, save it
        if (reply.head_fields.containsKey("Link")) {
            List<String> link_list = reply.head_fields.get("Link");
            String link_str = link_list.get(0);
            // <https://api.github.com/repositories/8514/issues?sort=update&page=33>; rel="next", <https://api.github.com/repositories/8514/issues?sort=update&page=33>; rel="last", <https://api.github.com/repositories/8514/issues?sort=update&page=1>; rel="first", <https://api.github.com/repositories/8514/issues?sort=update&page=31>; rel="prev"
            String[] link_infos = link_str.split(",");
            for (String one_link_info : link_infos) {
                // if the type of this link is next, then we save it
                String[] link_and_type = one_link_info.split(";");
                // if the type is next
                if (link_and_type[1].trim().equals("rel=\"next\"")) {
                    String link = link_and_type[0].trim();
                    link = link.substring(1, link.length() - 1); // remove the < and >
                    next_page_link = link;
                }
                // if it is a prev link
                if (link_and_type[1].trim().equals("rel=\"prev\"")) {
                    String link = link_and_type[0].trim();
                    link = link.substring(1, link.length() - 1); // remove the < and >
                    prev_page_link = link;
                }
            }
        }

        // if there is no next page, we disable the page buttons
        btn_next_page.setEnabled(next_page_link!=null);
        btn_prev_page.setEnabled(prev_page_link!=null);

        StringBuilder sb = new StringBuilder();

        try {
            JSONArray comments = new JSONArray(reply.response_text);
            for (int step = 0; step < comments.length(); step++) {
                JSONObject one_comment = comments.getJSONObject(step);
                JSONObject user = one_comment.getJSONObject("user");
                if (sb.length() > 0) sb.append("\n-------------------\n");
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

    @Override
    public void onClick(View v) {
        String link_to_use = null;
        if (v == btn_next_page) {
            link_to_use = next_page_link;
        } else if (v == btn_prev_page) {
            link_to_use = prev_page_link;
        }
        if (link_to_use == null) return;

        new GetCommentsAsyncTask(this, null).execute(link_to_use);
    }
}
