package com.zulip.android.activities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.zulip.android.R;
import com.zulip.android.models.Person;

import java.util.List;
import java.util.Map;

/**
 * Created by Halley on 2018/1/4.
 */

public class NearbyListAdapter extends BaseAdapter {

    private List<Person> personList;
    private Map<String, Float> distanceMap;

    private LayoutInflater layoutInflater;

    public NearbyListAdapter(Context context, List<Person> personList, Map<String, Float> distanceMap)
    {
        this.layoutInflater = LayoutInflater.from(context);
        this.personList = personList;
        this.distanceMap = distanceMap;
    }

    @Override
    public int getCount() {
        return personList.size();
    }

    @Override
    public Object getItem(int position) {
        return personList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return personList.indexOf(getItem(position));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if(convertView == null){
            convertView = layoutInflater.inflate(R.layout.list_nearby_person, null);
            viewHolder = new ViewHolder(
                    (TextView) convertView.findViewById(R.id.name),
                    (TextView) convertView.findViewById(R.id.distance)
            );
            convertView.setTag(viewHolder);
        }
        else{
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Person person = (Person) getItem(position);
        viewHolder.title.setText(person.getName());
        viewHolder.distance.setText(String.format("%.1f", distanceMap.get(person.getEmail())).toString() + " meters");

        return convertView;
    }

    private class ViewHolder{
        TextView title;
        TextView distance;

        public ViewHolder(TextView title, TextView distance)
        {
            this.title = title;
            this.distance = distance;
        }
    }
}
