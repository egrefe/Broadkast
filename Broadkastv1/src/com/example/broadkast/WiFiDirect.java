package com.example.broadkast;

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
import android.os.Bundle;
import android.util.Log;

import com.example.broadkast.WiFiDirectServicesList.WiFiDevicesAdapter;

public class WiFiDirect extends Activity implements ConnectionInfoListener {

		// Thread on which socket connection(s) is managed
		private Thread socketThread;
		private WiFiDirectServicesList servicesList;
		
		// Thread on which screen is captured and transmitted
		private Thread screenCaptureThread;
		
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
		
		private String TAG;
		
		public void setList (WiFiDirectServicesList list){
			servicesList = list;
			TAG = getClass().getName();
		}
		
		@Override
		protected void onCreate(Bundle savedInstanceState){
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_main);

			manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
			channel = manager.initialize(this, getMainLooper(), null);
			receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);

			intentFilter = new IntentFilter();
			intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
			intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
			intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
			//intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		}
		
		@Override
		protected void onResume() {
			super.onResume();
			registerReceiver(receiver, intentFilter);
		}

		@Override
		protected void onPause() {
			super.onPause();
			unregisterReceiver(receiver);
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
			
			Log.i(TAG,"Connecting to device.");

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
			Log.i(TAG, "Start Registration!");
			Map<String, String> record = new HashMap<String, String>();
			record.put("available", "visible");

			WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
					SERVICE_NAME, "_presence._tcp", record);
			manager.addLocalService(channel, service, new ActionListener() {

				@Override
				public void onSuccess() {
					Log.i(TAG, "Successful registration!");
				}

				@Override
				public void onFailure(int error) {
					Log.i(TAG, "Failed registration!");
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
						Log.i(TAG, "Discovered!");
					//serviceDevice = resourceType;
						
                        if (servicesList != null) {
                            WiFiDevicesAdapter adapter = ((WiFiDevicesAdapter) servicesList
                                    .getListAdapter());
                            WiFiP2pService service = new WiFiP2pService();
                            service.device = resourceType;
                            service.instanceName = instanceName;
                            service.serviceRegistrationType = registrationType;
                            adapter.add(service);
                            adapter.notifyDataSetChanged();
                          }	
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
			// Get p2pInfo
			this.p2pInfo = p2pInfo;
			
			Log.i(TAG, "Connection Info Available");
			
			// Check if thread has already been started
			if(socketThread != null)
				return;
			
			
			
			// Create thread for socket communication
			boolean ready = p2pInfo != null && p2pInfo.groupFormed;
						
			if (ready && !p2pInfo.isGroupOwner){
				socketThread = new Thread(new ClientThread(p2pInfo.groupOwnerAddress,this));
				socketThread.start();
				Log.i(TAG, "Started Client");
			}
			else{
				if (ready && p2pInfo.isGroupOwner){
					// Create ServerThread
					ServerThread serverThread = new ServerThread(this);
					socketThread = new Thread(serverThread);
					socketThread.start();
					Log.i(TAG, "Started Server");
					
					// Create ScreenCaptureThread
					screenCaptureThread = new Thread(new ScreenCaptureThread(serverThread));
					screenCaptureThread.start();
					Log.i(TAG, "Started screen capture thread");
				}
			}
			
		}
		
		
		public void setServiceDevice(WifiP2pDevice serviceDevice){
			this.serviceDevice = serviceDevice;
		}
		
		public void stopBroadcasting(){
			manager.cancelConnect(channel, new ActionListener() {

				@Override
				public void onSuccess() {
				}

				@Override
				public void onFailure(int arg0) {
				
				}
			});
			
			this.p2pInfo = null;
			this.serviceDevice = null;
		}
		
		/*
		private ArrayList<Socket> sockets = new ArrayList<Socket>();
		
		public synchronized void addSocket(Socket s){			
			View v = KastPage.view;
			v = v.getRootView();
			v.setDrawingCacheEnabled(true);
			
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			v.getDrawingCache().compress(Bitmap.CompressFormat.PNG, 85, os);
			Log.i("WIFI", "Size of bitmap = " + os.size());
			
			
			
			File f = new File("/storage/sdcard0/Pictures/Screenshots/Screenshot_2013-10-31-12-13-28.png");
			byte[] buffer = new byte[300000];
			int bytes = 0;
			
			try{
				bytes = new FileInputStream(f).read(buffer);
			}
			catch(IOException e){
				e.printStackTrace();
			}
			
			
			sockets.add(s);
			
			write(os.toByteArray());			
		}
		
		public synchronized void write(byte[] buff){
			for(Socket s : sockets){
				try {
					s.getOutputStream().write(buff);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		public synchronized void write(byte[] buff, int offset, int numBytes){
			for(Socket s : sockets){
				try {
					s.getOutputStream().write(buff,offset,numBytes);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		*/
}
