package com.netcompss.ffmpeg4android_client;



import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Random;

import com.netcompss.ffmpeg4android.IFfmpgefRemoteServiceBridge;
import com.netcompss.ffmpeg4android.LicenseCheckJNI;
import com.netcompss.ffmpeg4android.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * This class use the Template Method design pattern, 
 * the invokeService() virtual method is implemented at children level and called in this class template method onServiceConnected()
 * @author ehasson
 *
 */
public class BaseWizard extends Base {
	
	protected IFfmpgefRemoteServiceBridge remoteService;
	protected boolean started = false;
	protected RemoteServiceConnection conn = null;
	protected boolean invokeFlag = false;
	protected ProgressDialog progressDialog;
	
	protected Button convertButton;
	protected Button playButton;
	protected Button shareButton;
	protected Button selectButton;
	protected int PICK_REQUEST_CODE = 0;
	
	protected static String workingFolder;
	protected String outputFile;
	protected String outputFilePath;
	protected String inputFilePath;
	protected Prefs _prefs = null;
	
	protected String commandStr;
	
	
	private String progressDialogMessage;
	private String progressDialogTitle;
	
	private int notificationIcon;
	private String notificationTitle = null;
	private String notificationMessage = null;
	private String notificationfinishedMessageTitle = null;
	private String notificationStoppedMessage = null;
	private String notificationfinishedMessageDesc = null;
	
	public void setNotificationfinishedMessageDesc(
			String notificationfinishedMessageDesc) {
		this.notificationfinishedMessageDesc = notificationfinishedMessageDesc;
	}


	protected String[] commandComplex;
	
	
	public String[] getCommandComplex() {
		return commandComplex;
	}
	public void setCommandComplex(String[] commandComplex) {
		this.commandComplex = commandComplex;
	}
	public String getNotificationfinishedMessageTitle() {
		return notificationfinishedMessageTitle;
	}
	public void setNotificationfinishedMessageTitle(String notificationfinishedMessage) {
		this.notificationfinishedMessageTitle = notificationfinishedMessage;
	}
	public String getNotificationStoppedMessage() {
		return notificationStoppedMessage;
	}
	public void setNotificationStoppedMessage(String notificationStoppedMessage) {
		this.notificationStoppedMessage = notificationStoppedMessage;
	}
	public void setNotificationTitle(String notificationTitle) {
		this.notificationTitle = notificationTitle;
	}
	public void setNotificationMessage(String notificationMessage) {
		this.notificationMessage = notificationMessage;
	}

	public void setNotificationIcon(int notificationIcon) {
		this.notificationIcon = notificationIcon;
	}
	public String getProgressDialogMessage() {
		return progressDialogMessage;
	}
	
	public void setProgressDialogMessage(String progressDialogMessage) {
		this.progressDialogMessage = progressDialogMessage;
	}
	public String getProgressDialogTitle() {
		return progressDialogTitle;
	}
	public void setProgressDialogTitle(String progressDialogTitle) {
		this.progressDialogTitle = progressDialogTitle;
	}
	
	

	public String getCommand() {
		return commandStr;
	}
	public void setCommand(String commandStr) {
		Log.i(Prefs.TAG, "Command is set");
		this.commandStr = commandStr;
	}
	
	
	public static String getWorkingFolder() {
		return workingFolder;
	}
	public String getOutputFile() {
		return outputFile;
	}
	public String getInputFilePath() {
		return inputFilePath;
	}
	
	private void setRemoteNotificaitonIcon() {
		if (notificationIcon != -1)
			Prefs.setRemoteNotificationIconId(getApplicationContext(), notificationIcon);
	}
	
	private void setRemoteNotificationInfo() {
		try {
			if (remoteService != null) {
				Log.i(Prefs.TAG, "setting remote notification info");
				if (notificationTitle != null)
					remoteService.setNotificationTitle(notificationTitle);
				if (notificationMessage != null)
					remoteService.setNotificationMessage(notificationMessage);
			}
			else {
				Log.w(Prefs.TAG, "remoteService is null, can't set remote notification info");
			}
		} catch (RemoteException e1) {
			Log.w(Prefs.TAG, e1.getMessage(), e1);
		}
	}
	
	// called from onServiceConnected
	public void invokeService() {
		Log.i(Prefs.TAG, "invokeService called");
		
		
		 setRemoteNotificationInfo();
		  
		  // this call with handle license gracefully.
		  // If it will be removed, the fail will be in the native code, causing the progress dialog to start.
		  //if (! isLicenseValid()) return;
		  
		  if (invokeFlag) {
			  if(conn == null) {
				  Toast.makeText(this, "Cannot invoke - service not bound", Toast.LENGTH_SHORT).show();
			  } else {
				  try {
					  String command = getCommand();
					  
					  if (remoteService != null) {
						  if (command != null)
							  remoteService.setFfmpegCommand(command);
						  else {
							  remoteService.setComplexFfmpegCommand(commandComplex);
						  }
						  remoteService.setWorkingFolder(Prefs.getWorkFolder());
						  runWithCommand(command);
						  
					  }
					  else {
						  Log.w( Prefs.TAG, "Invoke failed, remoteService is null." );
					  }

				  } catch (android.os.DeadObjectException e) {
					  Log.d( Prefs.TAG, "ignoring DeadObjectException (FFmpeg process exit)");
				  } catch (RemoteException re) {
					  Log.e( Prefs.TAG, re.getMessage(), re );
				  }
			  }
			  invokeFlag = false;
		  }
		  else {
			  Log.d(Prefs.TAG, "Not invoking");
			  
		  }
	}
	
	
	protected boolean invokeFileInfoServiceFlag = false;
	
	public void getInputFileAndOutputFileFromCommand(String workingFolder, String inputFileName) {

	 }
	
	public IFfmpgefRemoteServiceBridge getRemoteService() {
		return remoteService;
	}
	
	public void setWorkingFolder(String workingFolder) {
		Prefs.setWorkFolder(workingFolder);
	}
	
	
	public boolean isLicenseValid() {
		 LicenseCheckJNI lm = new LicenseCheckJNI();
		  int rc = lm.licenseCheck(Prefs.getWorkingFolderForNative());
		  if (rc < 0) {
			  if (rc == -1)
				  Toast.makeText(this, "Trail Expired. contact support.", Toast.LENGTH_SHORT).show();
			  else if (rc == -2) 
				  Toast.makeText(this, "License invalid contact support", Toast.LENGTH_SHORT).show();
			  else 
				  Toast.makeText(this, "License check failed. contact support." + rc, Toast.LENGTH_SHORT).show();
			  
			  return false;
			  
		  }
		  else {
			  return true;
		  }
	}
	

	public void runTranscoing() {
		setRemoteNotificaitonIcon();
		releaseService();
		stopService();
		startService();
		invokeFlag = true;
		bindService();
	}
	
	 public void startAct(Class act) {
		 Intent intent = new Intent(this, act);
		 Log.d(Prefs.TAG, "Starting act:" + act);
		 this.startActivity(intent);
	 }
	
	public void runWithCommand(String command) {
		  Prefs p = new Prefs();
		  p.setContext(getApplicationContext());

		  deleteLogs();
		  FileUtils.writeToLocalLog("command: " + command);
		  FileUtils.writeToLocalLog("Input file size: " + Prefs.inputFileSize);
		  Log.d( Prefs.TAG, "Client invokeService()" );
		  Random rand = new Random();
		  int randInt  = rand.nextInt(1000);
		  TranscodeBackground t= new TranscodeBackground(this, remoteService, randInt);
		  t.setProgressDialogTitle(progressDialogTitle);
		  t.setProgressDialogMessage(progressDialogMessage);
		  t.setNotificationIcon(notificationIcon);
		  t.setNotificationfinishedMessageTitle(notificationfinishedMessageTitle);
		  t.setNotificationfinishedMessageDesc(notificationfinishedMessageDesc);
		  t.setNotificationStoppedMessage(notificationStoppedMessage);
		  t.execute();
	}
	
	
	public void copyLicenseAndDemoFilesFromAssetsToSDIfNeeded() {

		File destVid = null;
		File destLic = null;
		//String workingFolderPath = Environment.getExternalStorageDirectory() + Prefs.WORKING_DIRECTORY;
		String workingFolderPath = Prefs.getWorkFolder();
		Log.i(Prefs.TAG, "workingFolderPath: " + workingFolderPath);
		try {
			if (!FileUtils.checkIfFolderExists(workingFolderPath)) {
				
				boolean isFolderCreated = FileUtils.createFolder(workingFolderPath);
				Log.i(Prefs.TAG, workingFolderPath + " created? " + isFolderCreated);
				if (isFolderCreated) {
					
					destVid = new File(workingFolderPath + "in.mp4");
					Log.i(Prefs.TAG, "Adding vid file at " + destVid.getAbsolutePath());
					InputStream is = getApplication().getAssets().open("in.mp4");
					BufferedOutputStream o = null;
					try {
						byte[] buff = new byte[10000];
						int read = -1;
						o = new BufferedOutputStream(new FileOutputStream(destVid), 10000);
						while ((read = is.read(buff)) > -1) { 
							o.write(buff, 0, read);
						}
					} finally {
						is.close();
						if (o != null) o.close(); 

					}
					Log.i(Prefs.TAG, "Copy " + destVid.getAbsolutePath() + " from assets to SDCARD finished succesfully");
					
					
					boolean createLic = true;
					try {
						is = getApplication().getAssets().open("ffmpeglicense.lic");
					} catch (Exception e) {
						Log.i(Prefs.TAG, "License file does not exist in the assets.");
						createLic = false;
					}
					
					if (createLic) {
					destLic = new File(workingFolderPath + "ffmpeglicense.lic");
					Log.i(Prefs.TAG, "Adding lic file at " + destLic.getAbsolutePath());
					
					o = null;
					try {
						byte[] buff = new byte[10000];
						int read = -1;
						o = new BufferedOutputStream(new FileOutputStream(destLic), 10000);
						while ((read = is.read(buff)) > -1) { 
							o.write(buff, 0, read);
						}
					} finally {
						is.close();
						if (o != null) o.close();  

					}
					Log.i(Prefs.TAG, "Copy " + destLic.getAbsolutePath() + " from assets to SDCARD finished succesfully");
					}
					
					
					//Toast.makeText(this, "Demo video created at: " + workingFolderPath , Toast.LENGTH_SHORT).show();
				}
				else {
					Toast.makeText(this, "Working folder was not created, You need SDCARD to use this app!", Toast.LENGTH_LONG).show();
				}
				
			}
			else {
				Log.d(Prefs.TAG, "Working directory exists, not coping assests (license file and demo videos)");
				Toast.makeText(this, "Sample videos located at: " + workingFolderPath , Toast.LENGTH_SHORT).show();
			}
			
			if (!FileUtils.checkIfFolderExists(_prefs.getOutFolder())) {
				boolean isFolderCreated = FileUtils.createFolder(_prefs.getOutFolder());
				Log.i(Prefs.TAG, _prefs.getOutFolder() + " created? " + isFolderCreated);
			}
			else {
				Log.d(Prefs.TAG, "output directory exists.");
				
			}
		} catch (FileNotFoundException e) {
			Log.e(Prefs.TAG, e.getMessage());
		} catch (IOException e) {
			Log.e(Prefs.TAG, e.getMessage());
		}
	}
	
	
	
	protected void startService(){
		if (started) {
			Toast.makeText(this, "Service already started", Toast.LENGTH_SHORT).show();
		} else {
			
			Intent i = new Intent("com.netcompss.ffmpeg4android.FFMpegRemoteServiceBridge");
			PackageManager packageManager = getPackageManager();
			List<ResolveInfo> services = packageManager.queryIntentServices(i, 0);
			Log.i(Prefs.TAG, "!!!!!!!!!!!!!!!!!!services.size(): " +  services.size());

			
			if (services.size() > 0) {
			    ResolveInfo service = services.get(0);
			    i.setClassName(service.serviceInfo.packageName, service.serviceInfo.name);
			    i.setAction("com.netcompss.ffmpeg4android.FFMpegRemoteServiceBridge");
			    
			    if (!invokeFileInfoServiceFlag) {
					i.addCategory("Base");
					Log.i(Prefs.TAG, "putting Base categoty");
				}
				else {
					i.addCategory("Info");
					Log.i(Prefs.TAG, "putting Info categoty");
				}
			    
			    ComponentName cn = startService(i);
			    Log.d(Prefs.TAG, "started: " + cn.getClassName());
			}

			started = true;
			Log.d( Prefs.TAG, "Client startService()" );
		}

	}

	// this is not working, not stopping the remote service.
	protected void stopService() {
		Log.d( Prefs.TAG, "Client stopService()" );
		//Intent i = new Intent();
		Intent i = new Intent("com.netcompss.ffmpeg4android.FFMpegRemoteServiceBridge");
		//i.setClassName("com.netcompss.ffmpeg4android", "com.netcompss.ffmpeg4android.FFMpegRemoteServiceBridge");
		stopService(i);
		started = false;
	}
	
	protected void bindService() {
		Log.d(Prefs.TAG," bindService() called");
		if(conn == null) {
			conn = new RemoteServiceConnection();
			//Intent i = new Intent();
			Intent i = new Intent("com.netcompss.ffmpeg4android.FFMpegRemoteServiceBridge");
			//i.setClassName("com.netcompss.ffmpeg4android", "com.netcompss.ffmpeg4android.FFMpegRemoteServiceBridge");
			bindService(i, conn, Context.BIND_AUTO_CREATE);
			Log.d( Prefs.TAG, "Client bindService()" );
		} else {
			Log.d(Prefs.TAG," Client Cannot bind - service already bound");
			//Toast.makeText(this, "Client Cannot bind - service already bound", Toast.LENGTH_SHORT).show();
		}
	}
  
	protected void releaseService() {
		if(conn != null) {
			unbindService(conn);
			conn = null;
			Log.d( Prefs.TAG , "releaseService()" );
		} else {
			//Toast.makeText(this, "Client Cannot unbind - service not bound", Toast.LENGTH_SHORT).show();
			Log.d( Prefs.TAG , "Client Cannot unbind - service not bound");
		}
	}
	
	
	
	 public class RemoteServiceConnection implements ServiceConnection {
	    	public void onServiceConnected(ComponentName className, 
	    			IBinder boundService ) {
	    		Log.d( Prefs.TAG, "Client onServiceConnected()" );
	    		remoteService = IFfmpgefRemoteServiceBridge.Stub.asInterface((IBinder)boundService);
	    		
	    		
	    		if (invokeFileInfoServiceFlag)
	    			invokeFileInfoService(inputFilePath);
	    		else
	    			invokeService();

	    	}

	        public void onServiceDisconnected(ComponentName className) {
	          remoteService = null;
			  Log.d( Prefs.TAG, "onServiceDisconnected" );
	        }
	    };
	    
	    

	    
	    public void handleServiceFinished() {
	    	Log.i(Prefs.TAG, "FFMPEG finished.");
	    	Toast.makeText(this, getString(R.string.notif_message_ok), Toast.LENGTH_LONG).show();
	    }
	    
	    protected void handleInfoServiceFinished() {
	    	Log.i(Prefs.TAG, "FFMPEG finished (info).");
	    	removeDialog(FILE_INFO_DIALOG);
	    	showDialog(FILE_INFO_DIALOG);
	    	invokeFileInfoServiceFlag = false;
	    	
	    	
	    }
	    
	   
	    
	    private String getRealPathFromURI(Uri contentUri) {
	        String[] proj = { MediaStore.Images.Media.DATA };
	        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
	        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	        cursor.moveToFirst();
	        return cursor.getString(column_index);
	    }
	    
	    public void invokeFileInfoService(String inputFilePath) {
			  Log.i(Prefs.TAG, "invokeFileInfoService called");
			  
			  if (invokeFlag) {

				  if(conn == null) {
					  Toast.makeText(this, "Cannot invoke - service not bound", Toast.LENGTH_SHORT).show();
				  } else {
					  try {
						  //FileUtils.deleteFile(workingFolder + outputFile);
						  String command = "ffmpeg -i " + inputFilePath;
						  if (remoteService != null) {
							  deleteLogs();
							  FileUtils.writeToLocalLog("command: " + command);
							  Log.i(Prefs.TAG, "command: " + command);
							  remoteService.setFfmpegCommand(command);
							  Log.d( Prefs.TAG, "Client invokeService()" );
							  remoteService.runTranscoding();
						  }
						  else {
							  Log.w( Prefs.TAG, "Invoke failed, remoteService is null." );
						  }

					  } catch (android.os.DeadObjectException e) {
						 
						  Log.d( Prefs.TAG, "ignoring DeadObjectException (FFmpeg process exit)");

					  } catch (RemoteException re) {
						  Log.e( Prefs.TAG, re.getMessage(), re );
					  }
				  }
				  handleInfoServiceFinished();
				  invokeFlag = false;
			  }
			  else {
				  Log.d(Prefs.TAG, "Not invoking");
				  
			  }
		  }
	    
	    
	    public void deleteLogs() {
		      FileUtils.deleteFile(Prefs.getVkLogFilePath());
		      FileUtils.deleteFile(Prefs.getFfmpeg4androidLogFilePath());
		      FileUtils.deleteFile(Prefs.getVideoKitLogFilePath());
	    }
		public void setOutputFilePath(String outputFilePath) {
			this.outputFilePath = outputFilePath;
			this.outputFile = FileUtils.getFileNameFromFilePath(outputFilePath);
		}
	    
	    
	    
	    


}
