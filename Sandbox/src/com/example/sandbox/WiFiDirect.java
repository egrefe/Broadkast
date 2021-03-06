package com.example.sandbox;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.view.View;

public class WiFiDirect implements ConnectionInfoListener {

	// Thread on which socket connection(s) is managed
		private Thread socketThread;
		
		
		private final String SERVICE_NAME = "Broadkast";
		static final int SERVER_PORT = 7878;

		// Used for managing WiFi Direct connections
		private WifiP2pManager manager;
		private Channel channel;
		private BroadcastReceiver receiver;
		private IntentFilter intentFilter;
		private WifiP2pDnsSdServiceRequest serviceRequest;


		// Info about current connection/group
		private WifiP2pDevice serviceDevice;
		private WifiP2pInfo p2pInfo;
		

		public WiFiDirect(Activity activity){

			// Set up manager, channel, and receiver
			manager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
			channel = manager.initialize(activity, activity.getMainLooper(), null);
			receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);

			// Set up intent filter for use in BroadcastReceiver
			intentFilter = new IntentFilter();
			intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
			intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
			intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
			//intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		}
		
		/* Called by 'Viewers' to connect to serviceDevice. This serviceDevice is currently
		 * just the most recently discovered device. Viewer should be able to select
		 * devices from list somehow.
		 */
		public void connectToDevice(){

			if(serviceDevice == null){
				//printMessage("No devices have been found.");
				return;
			}
			

			WifiP2pConfig config = new WifiP2pConfig();
			config.deviceAddress = serviceDevice.deviceAddress;
			config.wps.setup = WpsInfo.PBC;
			if (serviceRequest != null)
				manager.removeServiceRequest(channel, serviceRequest,
						new ActionListener() {

					@Override
					public void onSuccess() {
						//
					}

					@Override
					public void onFailure(int arg0) {
						//
					}
				});

			manager.connect(channel, config, new ActionListener() {

				@Override
				public void onSuccess() {
					//printMessage("Connected to device successfully");
				}

				@Override
				public void onFailure(int errorCode) {
					//printMessage("Failed to connect to device");
				}
			});
		}
		
		/*
		 * This function should be called by 'Broadcasters' so that other devices
		 * can discover and connect to them.
		 */
		public void startRegistration(){
			Map<String, String> record = new HashMap<String, String>();
			record.put("available", "visible");

			WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
					SERVICE_NAME, "_presence._tcp", record);
			manager.addLocalService(channel, service, new ActionListener() {

				@Override
				public void onSuccess() {

				}

				@Override
				public void onFailure(int error) {

				}
			});
		}
		
		/*
		 * Discovers services that have been registered by other devices. THis
		 * function should be called by devices who want to be 'Viewers'. servListener
		 * should add discover devices to a list so a 'Broadcaster' can be selected.
		 */
		public void discoverService(){
			/*
			 * Register listeners for DNS-SD services. These are callbacks invoked
			 * by the system when a service is actually discovered.
			 */

			DnsSdServiceResponseListener servListener = new DnsSdServiceResponseListener() {
				@Override
				public void onDnsSdServiceAvailable(String instanceName, String registrationType,
						WifiP2pDevice resourceType) {
					if(instanceName.equalsIgnoreCase(SERVICE_NAME)){
						// Add service
						//EditText editText = (EditText) findViewById(R.id.edit_message);
						//editText.setText(resourceType.deviceName + ":" + instanceName);
						serviceDevice = resourceType;
					}
				}
			};

			DnsSdTxtRecordListener txtListener = new DnsSdTxtRecordListener() {
				@Override
				/* Callback includes:
				 * fullDomain: full domain name: e.g "printer._ipp._tcp.local."
				 * record: TXT record dta as a map of key/value pairs.
				 * device: The device running the advertised service.
				 */

				public void onDnsSdTxtRecordAvailable(
						String fullDomain, Map record, WifiP2pDevice device) {
					//Log.d(TAG, "DnsSdTxtRecord available -" + record.toString());
					//buddies.put(device.deviceAddress, record.get("buddyname"));
				}
			};

			manager.setDnsSdResponseListeners(channel, servListener, txtListener);


			// After attaching listeners, create a service request and initiate
			// discovery.
			serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
			manager.addServiceRequest(channel, serviceRequest,
					new ActionListener() {

				@Override
				public void onSuccess() {
				}

				@Override
				public void onFailure(int arg0) {
				
				}
			});
			manager.discoverServices(channel, new ActionListener() {

				@Override
				public void onSuccess() {
					// Notify successful
				}

				@Override
				public void onFailure(int arg0) {
					// Notify failed
				}
			});
		}
		
		@Override
		public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
			//printMessage("group formed = " + p2pInfo.groupFormed + ", owner address = "
			//		+  p2pInfo.groupOwnerAddress.getHostAddress() + ", Is group owner = " +  p2pInfo.isGroupOwner);

			// Get p2pInfo
			this.p2pInfo = p2pInfo;
			
			
			
			// Check if thread has already been started
			if(socketThread != null)
				return;
			
			/*
			
			// Create thread for socket communication
			boolean ready = p2pInfo != null && p2pInfo.groupFormed;
			
			if (ready)
				printMessage(p2pInfo.isGroupOwner + " " + p2pInfo.groupOwnerAddress.getHostAddress() + p2pInfo.groupFormed);
			
			if (ready && !p2pInfo.isGroupOwner){
				socketThread = new Thread(new ClientThread(p2pInfo.groupOwnerAddress, this));
				socketThread.start();
			}
			else{
				if (ready && p2pInfo.isGroupOwner){
					// Create ServerThread
					socketThread = new Thread(new ServerThread(this));
					socketThread.start();
				}
			}
			*/
		}
}
