package me.cnzy.railissues;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

import me.cnzy.railissues.Utils.Config;
import me.cnzy.railissues.Utils.GetCommentsAsyncTask;
import me.cnzy.railissues.Utils.GetCommentsAsyncTaskCallback;
import me.cnzy.railissues.Utils.GetIssuesAsyncTask;
import me.cnzy.railissues.Utils.GetIssuesAsyncTaskCallback;
import me.cnzy.railissues.Utils.HTTPUtility;

public class MainActivity extends AppCompatActivity implements GetIssuesAsyncTaskCallback,
        AdapterView.OnItemClickListener, View.OnClickListener {

    public static final String TAG = "VI.MainActivity";
    private ProgressBar pb_get_issues = null;
    private ListView lv_issue_list = null;
    private IssuesListAdapter issue_list_adapter = null;
    private View v_list_footer = null;
    private Button btn_load_more_issues = null;
    private String next_page_link = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        // get handler for views
        pb_get_issues = (ProgressBar) findViewById(R.id.pb_get_issues);
        lv_issue_list = (ListView) findViewById(R.id.lv_issue_list);
        issue_list_adapter = new IssuesListAdapter(this);
        lv_issue_list.setAdapter(issue_list_adapter);
        lv_issue_list.setOnItemClickListener(this);
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v_list_footer = inflater.inflate(R.layout.view_issue_list_footer, lv_issue_list, false);
        btn_load_more_issues = (Button)v_list_footer.findViewById(R.id.btn_load_more_issues);
        btn_load_more_issues.setOnClickListener(this);
        lv_issue_list.addFooterView(v_list_footer);

        // get all the issues in json format
        startGetIssues();
    }

    /**
     * start the async task to do the internet connection and fetch the issues
     * the issues are returned in json format
     */
    private void startGetIssues() {
        // first show a progress icon
        pb_get_issues.setVisibility(View.VISIBLE);
        new GetIssuesAsyncTask(this, null).execute(Config.getRepoIssuesURL());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * this is the callback after the "GetIssuesAsyncTask" is finished
     * @param token
     * @param action
     * @param reply
     */
    @Override
    public void getIssuesTaskResult(String token, String action, HTTPUtility.HTTPResult reply) {
        // hide the progress bar
        pb_get_issues.setVisibility(View.GONE);

        if (!reply.succeeded) {
            // TODO: alert user that a network faulure has happened
            return;
        }
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
                    break; // there can only be one next link
                }
            }
            // if there is no next page, we hide the load more button
            if (next_page_link == null) {
                btn_load_more_issues.setVisibility(View.GONE);
            } else { // if there is, show the load more button
                btn_load_more_issues.setVisibility(View.VISIBLE);
            }
        }

        Log.d(TAG, reply.response_text);
        LinkedList<GithubIssueEntry> issue_list = new LinkedList<>();

        try {
            JSONArray issues = new JSONArray(reply.response_text);
            for (int step = 0; step < issues.length(); step++) {
                JSONObject one_issue = issues.getJSONObject(step);
                GithubIssueEntry issue_entry = new GithubIssueEntry();
                issue_entry.title = one_issue.optString("title", "");
                issue_entry.content = one_issue.optString("body", "");
                issue_entry.number = one_issue.optLong("number", -1);
                issue_list.add(issue_entry);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            //showFailureMessage("Failed", "Network Issues!");
            return;
        }

        // use the issue_list to update the list view
        issue_list_adapter.addAllFromList(issue_list);
    }

    /**
     * this function is called when an item in the list is clicked
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (id < 0) {
            Log.e(TAG, "The issue user clicked on has a invalid issue number.");
            return;
        }
        // start the comments activity to show the comments
        Intent intent = new Intent(MainActivity.this, CommentsActivity.class);
        Bundle info = new Bundle();
        info.putString("issue_id", Long.toString(id));
        intent.putExtras(info);
        startActivity(intent);
    }

    /**
     * this function is called when the load more issues button is clicked
     * @param v
     */
    @Override
    public void onClick(View v) {
        // load more issues
        if (next_page_link == null) return;
        new GetIssuesAsyncTask(this, null).execute(next_page_link);
    }
}
