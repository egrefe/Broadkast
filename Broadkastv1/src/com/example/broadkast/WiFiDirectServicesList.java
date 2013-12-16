/**
 * This class was taken from the WiFiDirectServiceDiscovery sample
 * application provided through the Android SDK. It is used to 
 * list broadcasting devices on the ViewPage of our application.
 */

package com.example.broadkast;

import java.util.ArrayList;
import java.util.List;

import android.app.ListFragment;
import android.content.Context;
import android.graphics.Color;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * A simple ListFragment that shows the available services as published by the
 * peers
 */
public class WiFiDirectServicesList extends ListFragment {

	WiFiDirect wifiD;
	WiFiDevicesAdapter listAdapter = null;

	interface DeviceClickListener {
		public void connectP2p(WiFiP2pService wifiP2pService);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.devices_list, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		listAdapter = new WiFiDevicesAdapter(this.getActivity(),
				android.R.layout.simple_list_item_2, android.R.id.text1,
				new ArrayList<WiFiP2pService>());
		setListAdapter(listAdapter);
	}

	@Override
	/**
	 * Connects to a broadcaster that has been clicked on
	 */
	public void onListItemClick(ListView l, View v, int position, long id) {

		Log.i("WIFI", "List item clicked.");
		wifiD.setServiceDevice(((WiFiP2pService) l.getItemAtPosition(position)).device);
		wifiD.connectToDevice();

		((TextView) v.findViewById(android.R.id.text2)).setText("GET READY!!!");

	}

	public void setWiFiDirect(WiFiDirect wifiD) {
		this.wifiD = wifiD;
	}

	public class WiFiDevicesAdapter extends ArrayAdapter<WiFiP2pService> {

		private List<WiFiP2pService> items;

		public WiFiDevicesAdapter(Context context, int resource,
				int textViewResourceId, List<WiFiP2pService> items) {
			super(context, resource, textViewResourceId, items);
			this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getActivity()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(android.R.layout.simple_list_item_2, null);
			}
			WiFiP2pService service = items.get(position);
			if (service != null) {
				TextView nameText = (TextView) v
						.findViewById(android.R.id.text1);
				nameText.setTextColor(Color.WHITE);

				if (nameText != null) {
					nameText.setText(service.device.deviceName + " - "
							+ service.instanceName);
				}
				TextView statusText = (TextView) v
						.findViewById(android.R.id.text2);
				statusText.setTextColor(Color.LTGRAY);
				statusText.setText(getDeviceStatus(service.device.status));
			}
			return v;
		}

	}

	public static String getDeviceStatus(int statusCode) {
		switch (statusCode) {
		case WifiP2pDevice.CONNECTED:
			return "Connected";
		case WifiP2pDevice.INVITED:
			return "Invited";
		case WifiP2pDevice.FAILED:
			return "Failed";
		case WifiP2pDevice.AVAILABLE:
			return "Available";
		case WifiP2pDevice.UNAVAILABLE:
			return "Unavailable";
		default:
			return "Unknown";

		}
	}

}
