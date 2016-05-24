package me.cnzy.railissues;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by pswzy on 5/23/2016.
 */
public class IssuesListAdapter extends BaseAdapter {

    Context context;
    ArrayList<GithubIssueEntry> issue_entries = new ArrayList<>();

    public IssuesListAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        return issue_entries.size();
    }

    public void addAllFromList(List<GithubIssueEntry> new_entries) {
        issue_entries.addAll(new_entries);
        notifyDataSetChanged();
    }

    @Override
    public Object getItem(int position) {
        return issue_entries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return issue_entries.get(position).number;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View use_view = convertView;

        if (use_view == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            use_view = inflater.inflate(R.layout.view_one_issue, parent, false);
        }

        TextView tv_title = (TextView) use_view.findViewById(R.id.tv_issue_title);
        TextView tv_content = (TextView) use_view.findViewById(R.id.tv_issue_content);

        GithubIssueEntry one_entry = issue_entries.get(position);
        tv_title.setText(one_entry.title);
        tv_content.setText(one_entry.content);
        return use_view;
    }
}
