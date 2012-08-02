package ru.nekit.gpstracking.adapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import ru.nekit.gpstracking.GeoActivity.GeoPoint;
import ru.nekit.gpstracking.R;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GeoListAdapter extends ArrayAdapter<GeoPoint> {

	Activity context;
	List<GeoPoint> dataSource;
	LayoutInflater inflater;

	static class ViewHolder {

		public ImageView type;
		public TextView position;
		public TextView time;
		public TextView description;
		public LinearLayout layout;
	}

	public GeoListAdapter(Activity context, List<GeoPoint> objects) {
		super(context, R.layout.list_item, objects);
		this.dataSource = objects;
		this.context 	= context;
		this.inflater 	= context.getLayoutInflater();
	}

	@Override
	public View getView(int index, View convertView, ViewGroup parent) {
		ViewHolder holder;
		View rowView = convertView;
		if (rowView == null) {
			rowView = inflater.inflate(R.layout.list_item, null, true);
			holder = new ViewHolder();
			holder.position = (TextView) rowView.findViewWithTag("position");
			holder.time = (TextView) rowView.findViewWithTag("time");
			holder.type = (ImageView)rowView.findViewWithTag("type");
			holder.layout = (LinearLayout)rowView.findViewWithTag("layout");
			rowView.setTag(holder);
		} else {
			holder = (ViewHolder) rowView.getTag();
		}
		GeoPoint item = dataSource.get(index);
		if( item.type == GeoPoint.ME )
		{
			holder.type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_location));
		}
		else
		{
			holder.type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_play));
		}
		String description = item.description;
		if( description == null || "".equals(description) )
		{
			if( holder.description != null )
			{
				holder.layout.removeView(holder.description);
				holder.description = null;
			}
		}
		else
		{
			if( holder.description == null )
			{
				holder.description = (TextView) inflater.inflate(R.layout.description_view, null);
				holder.layout.addView(holder.description);
			}
			holder.description.setText("Description: " + description);
		}
		String position = "Position [ lat: " + item.latitude + " | lon:" + item.longitude + " ]";
		holder.position.setText(position);
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		Date date = new Date(item.time);
		holder.time.setText("Time: " + sdf.format(date));
		return rowView;
	}
}
