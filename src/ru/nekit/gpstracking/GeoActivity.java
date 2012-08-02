package ru.nekit.gpstracking;

import java.util.ArrayList;

import ru.nekit.gpstracking.adapter.GeoListAdapter;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.Selection;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class GeoActivity extends SherlockActivity implements LocationListener, OnClickListener, OnItemClickListener 
{

	private static final int IDM_ADD_DESCRIPTION = 1;
	private static final int IDM_DELETE = 2;

	private LocationManager locationManager;
	private Location currentLocation;
	private GeoPointDBAdapter geoDB;
	private GeoListAdapter listAdapter;
	private ListView geoList;
	private TextView geoView;
	private boolean isPlay;
	private GeoPoint currentPoint;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		setTheme(R.style.Theme_Sherlock_Light);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		geoView = (TextView)findViewById(R.id.geoView);
		geoList = (ListView)findViewById(R.id.geoList);
		geoDB = new GeoPointDBAdapter(this);
		geoDB.open();
		//geoDB.removeAll();
		ArrayList<GeoPoint> dataSource = geoDB.selectAll();
		listAdapter = new GeoListAdapter(this, dataSource);
		geoList.setAdapter(listAdapter);
		geoList.setOnItemClickListener(this);
		registerForContextMenu(geoList);
		initGPS();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) 
	{
		super.onCreateContextMenu(menu, view, menuInfo);
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		currentPoint = listAdapter.getItem(info.position);
		menu.setHeaderTitle("Actions");
		menu.add(Menu.NONE, IDM_ADD_DESCRIPTION, Menu.NONE, ( "".equals(currentPoint.description) ||  currentPoint.description == null ) ? "Enter description" : "Change description");
		menu.add(Menu.NONE, IDM_DELETE, Menu.NONE, "Remove");
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) 
	{
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		currentPoint = listAdapter.getItem(info.position);
		switch (item.getItemId()) 
		{
		case IDM_ADD_DESCRIPTION:

			final EditText input = new EditText(this);
			input.setText(currentPoint.description);

			Editable etext = input.getText();
			Selection.setSelection(etext, input.length());

			new AlertDialog.Builder(this).setTitle( ( "".equals(currentPoint.description) ||  currentPoint.description == null ) ? "Enter description" : "Change description")
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) 
				{
					String value = input.getText().toString();
					setGeoPointDescription(value);
				}
			}).setNegativeButton("Cancel",  new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) 
				{
					dialog.dismiss();
				}
			}).setNeutralButton("Clear",  new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) 
				{
					setGeoPointDescription("");
				}
			})
			.setView(input)
			.setCancelable(false)
			.create()
			.show();

			break;
		case IDM_DELETE:

			removeGeoPoint();

			break;

		default:
			return super.onContextItemSelected(item);
		}
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 1, 0, "Play")
		.setIcon(R.drawable.ic_play)
		.setShowAsAction( MenuItem.SHOW_AS_ACTION_ALWAYS);
		menu.add(0, 2, 0, "Here")
		.setIcon(R.drawable.ic_location)
		.setShowAsAction( MenuItem.SHOW_AS_ACTION_ALWAYS);
		menu.add(0, 3, 0, "Clear")
		.setIcon(R.drawable.ic_content_discard)
		.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		return super.onCreateOptionsMenu(menu);
	}

	private void addGeoPoint(int type)
	{
		if( currentLocation != null )
		{
			GeoPoint point = new GeoPoint();
			point.latitude = currentLocation.getLatitude();
			point.longitude = currentLocation.getLongitude();
			point.altitude = currentLocation.getAltitude();
			point.speed = currentLocation.getSpeed();
			point.time = currentLocation.getTime();
			point.type = type;
			point.id = geoDB.insert(point);
			listAdapter.insert(point, 0);
			listAdapter.notifyDataSetChanged();
		}
	}

	private void removeGeoPoint()
	{
		geoDB.remove(currentPoint);
		listAdapter.remove(currentPoint);
		listAdapter.notifyDataSetChanged();
	}

	private void removeAllGeoPoints() 
	{
		geoDB.removeAll();
		listAdapter.clear();
		listAdapter.notifyDataSetChanged();
	}

	private void setGeoPointDescription(String value) 
	{
		currentPoint.description = value;
		geoDB.update(currentPoint);
		listAdapter.notifyDataSetChanged();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		int type = item.getItemId();
		switch(type)
		{
		case 1:

			isPlay = !isPlay;
			if( isPlay )
			{
				item.setIcon(R.drawable.ic_pause);
				addGeoPoint(GeoPoint.AUTO);
			}else{
				item.setIcon(R.drawable.ic_play);
			}

			break;

		case 2:

			addGeoPoint(GeoPoint.ME);

			break;

		case 3:

			new AlertDialog.Builder(this).setTitle("Clear all?")
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) 
				{
					removeAllGeoPoints();
				}
			}).setNegativeButton("No",  new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) 
				{
					dialog.dismiss();
				}
			})
			.setCancelable(true)
			.create()
			.show();

			break;

		default:

			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void initGPSListener()
	{
		locationManager =  (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		Criteria locationCritera = new Criteria();
		locationCritera.setAltitudeRequired(false);
		locationCritera.setBearingRequired(false);
		locationCritera.setCostAllowed(true);
		locationCritera.setPowerRequirement(Criteria.NO_REQUIREMENT);
		String providerName = locationManager.getBestProvider(locationCritera, true);
		currentLocation  = locationManager.getLastKnownLocation(providerName);
		updateView();
		locationManager.requestLocationUpdates(
				providerName,
				0,
				0,
				this);
	}

	private void initGPS() 
	{
		String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
		if( provider.equals("") )
		{
			Toast.makeText(this, "Adjustments are necessary", Toast.LENGTH_SHORT).show();
			Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);  
			startActivityForResult(gpsOptionsIntent, 1);  
		}
		else
		{
			initGPSListener();
			updateView();
		}
	}

	@Override
	protected void onRestart() 
	{
		initGPSListener();
		super.onRestart();
	}

	@Override
	protected void onPause() 
	{
		locationManager.removeUpdates(this);
		super.onPause();
	}

	@Override
	public void onClick(View v) {}

	@Override
	public void onLocationChanged(Location location) 
	{
		currentLocation = location;
		if ( isPlay )
		{
			addGeoPoint(GeoPoint.AUTO);
		}
		updateView();
	}

	private void updateView() 
	{
		if( currentLocation != null )
		{				
			geoView.setText("[lat: " + currentLocation.getLatitude() + " | lon:" + currentLocation.getLongitude() + "]" );
		}
	}

	@Override
	public void onProviderDisabled(String provider) {}

	@Override
	public void onProviderEnabled(String provider) {}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		if(  requestCode == 1 )
		{
			initGPS();
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	static class GeoPointDBAdapter {

		private GeoPointDBOpenHelper dbHelper;

		private static final String DATABASE_NAME = "geo_db.db";
		private static final String DATABASE_TABLE = "geo_point_list";
		private static final int DATABASE_VERSION = 1;

		private SQLiteDatabase db;

		public GeoPointDBAdapter(Context context) 
		{
			dbHelper = new GeoPointDBOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		public long insert(GeoPoint point) 
		{
			return db.insert(DATABASE_TABLE, null, GeoPointDBWripper.getContentValues(point, true));
		}

		public boolean removeAt(long id) 
		{
			return db.delete(DATABASE_TABLE, GeoPointDBWripper.ID + "=" + id, null) > 0;
		}

		public boolean remove(GeoPoint point) 
		{
			return removeAt(point.id);
		}

		public boolean updateAt(long id, GeoPoint point) 
		{
			return db.update(DATABASE_TABLE, GeoPointDBWripper.getContentValues(point), GeoPointDBWripper.ID + "=" + id, null) > 0; 
		}

		public boolean update(GeoPoint point) 
		{
			return updateAt(point.id, point);
		}

		public int removeAll() 
		{
			return db.delete(DATABASE_TABLE, null, null);
		}

		public Cursor getAllCursor() {
			return db.query(DATABASE_TABLE,
					new String[] 
							{ 
					GeoPointDBWripper.ID, 
					GeoPointDBWripper.LATITUDE, 
					GeoPointDBWripper.LONGITUDE, 
					GeoPointDBWripper.ALTITUDE,
					GeoPointDBWripper.TIME, 
					GeoPointDBWripper.SPEED,
					GeoPointDBWripper.TYPE, 
					GeoPointDBWripper.DESCRIPTION 
							},null, null, null, null, GeoPointDBWripper.ID + " DESC" );
		}

		public ArrayList<GeoPoint> selectAll() 
		{
			ArrayList<GeoPoint> list = new ArrayList<GeoPoint>();
			Cursor cursor = getAllCursor();
			if (cursor.moveToFirst()) {
				do {
					list.add(GeoPointDBWripper.getPoint(cursor));
				} while (cursor.moveToNext());
			}
			if (cursor != null && !cursor.isClosed()) 
			{
				cursor.close();
			}
			return list;
		}

		public void open() throws SQLiteException
		{
			try
			{
				db = dbHelper.getWritableDatabase();
			}catch(SQLiteException exception)
			{

				db = dbHelper.getReadableDatabase();
			}
		}

		public void close() 
		{
			db.close();
		}

		private class GeoPointDBOpenHelper extends SQLiteOpenHelper {

			public GeoPointDBOpenHelper(Context context, String name, CursorFactory factory, int version)
			{
				super(context, name, factory, version);
			}

			private String getCreateSchema()
			{
				return "CREATE TABLE " +
						DATABASE_TABLE +
						" (" + GeoPointDBWripper.ID 	+ " INTEGER PRIMARY KEY AUTOINCREMENT, " +
						GeoPointDBWripper.LONGITUDE 	+ " DOUBLE, " + 
						GeoPointDBWripper.LATITUDE 	+ " DOUBLE, " +
						GeoPointDBWripper.ALTITUDE 	+ " DOUBLE, " +
						GeoPointDBWripper.TYPE 		+ " INTEGER, " +
						GeoPointDBWripper.TIME 		+ " INTEGER, " +
						GeoPointDBWripper.SPEED 		+ " REAL, " +
						GeoPointDBWripper.DESCRIPTION + " TEXT); ";
			}

			@Override
			public void onCreate(SQLiteDatabase db) 
			{
				db.execSQL(getCreateSchema());
			}

			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
			{
				db.execSQL("DROP TABLE IF EXISTS" + DATABASE_TABLE);
				onCreate(db);
			}
		}
	}

	public static class GeoPoint
	{

		public static final int AUTO = 1;
		public static final int ME = 2;

		public long id;
		public double latitude;
		public double longitude;
		public double altitude;
		public long time;

		public float speed;

		public int type;
		public String description;

	}

	static class GeoPointDBWripper
	{

		public static final String ID = "_id";
		public static final String LATITUDE = "latitude";
		public static final String LONGITUDE = "longitude";
		public static final String ALTITUDE = "altitude";
		public static final String TIME = "time";
		public static final String TYPE = "type";
		public static final String SPEED = "speed";
		public static final String DESCRIPTION = "description";

		public static ContentValues getContentValues(GeoPoint point)
		{
			return getContentValues(point, false);
		}

		public static ContentValues getContentValues(GeoPoint point, boolean insert)
		{	
			ContentValues value = new ContentValues();
			if( !insert )
			{
				value.put(ID, 	point.id);
			}
			value.put(LATITUDE, 	point.latitude);
			value.put(LONGITUDE, 	point.longitude);
			value.put(ALTITUDE, 	point.altitude);
			value.put(TIME, 		point.time);
			value.put(SPEED, 		point.speed);
			value.put(TYPE, 		point.type);
			value.put(DESCRIPTION, 	point.description);
			return value;
		}

		public static GeoPoint getPoint(Cursor cursor)
		{
			GeoPoint value = new GeoPoint();
			value.id 		= cursor.getInt(0);
			value.latitude 	= cursor.getDouble(1);
			value.longitude = cursor.getDouble(2);
			value.altitude 	= cursor.getDouble(3);
			value.time 		= cursor.getLong(4);
			value.speed 	= cursor.getFloat(5);
			value.type 		= cursor.getInt(6);
			value.description = cursor.getString(7);
			return value;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int index, long i)
	{
		currentPoint = (GeoPoint) adapter.getItemAtPosition(index);
		String geoURI = "geo:" + currentPoint.latitude + "," + currentPoint.longitude + "?z=15";
		Uri geo = Uri.parse(geoURI);
		Intent geoMap = new Intent(Intent.ACTION_VIEW, geo);
		startActivity(Intent.createChooser(geoMap, "View on map"));
	}
}