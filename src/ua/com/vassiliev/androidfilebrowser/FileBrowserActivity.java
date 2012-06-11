package ua.com.vassiliev.androidfilebrowser;
//Heavily based on code from
//https://github.com/mburman/Android-File-Explore
//	Version of Aug 13, 2011
//  This version is taken from the FileExplore from Android File Shareing server project.

//General Java imports 
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

//Android imports 
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


//Import of resources file for file browser
import ua.com.vassiliev.androidfilebrowser.R;


public class FileBrowserActivity extends Activity {
	// Stores names of traversed directories
	ArrayList<String> pathDirsList = new ArrayList<String>();

	// Check if the first level of the directory structure is the one showing
	private Boolean firstLvl = true;

	private static final String TAG = "F_PATH";

	private List<Item> fileList = new ArrayList<Item>();
	private File path = new File(Environment.getExternalStorageDirectory() + "");
	private String chosenFile;
	private static final int DIALOG_LOAD_FILE = 1000;

	ArrayAdapter<Item> adapter;
	//Intent Constants
	public static final String intentDirectoryParameter = 
			"ua.com.vassiliev.androidfilebrowser.directoryPath";
	public static final String intentReturnDirectoryParameter = 
			"ua.com.vassiliev.androidfilebrowser.directoryPathRet";
	public static final String INTENT_ACTION_SELECT_DIR = 
			"ua.com.vassiliev.androidfilebrowser.SELECT_DIRECTORY_ACTION";
	
	private static int currentAction = -1;
	private static final int SELECT_DIRECTORY = 1;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//In case of com.mburman.fileexplore.PICK_FOLDER
		//Expects com.mburman.fileexplore.directoryPath parameter to 
		// point to the start folder.
		// If empty or null, will start from SDcard root.
		setContentView(R.layout.ua_com_vassiliev_filebrowser_layout);
		
		Intent thisInt = this.getIntent();
		if(thisInt.getAction().equalsIgnoreCase(INTENT_ACTION_SELECT_DIR)) {
			//Log.d(TAG, "SELEC DIR ACTION");
			currentAction = SELECT_DIRECTORY;
		}
		String requestedStartDir = 
				thisInt.getStringExtra(intentDirectoryParameter);
		if(requestedStartDir!=null && requestedStartDir.length()>0) {
//			Log.d(TAG, 
//				"I have intent parameter:"+requestedStartDir
//			);
			this.path = new File(requestedStartDir);
			
		}//if(requesteStartDir!=null && requestedStartDir.length()>0) {
		else {
			Log.d(TAG,"Requested start dir is empty");
			this.path = Environment.getExternalStorageDirectory();
		}
		parceDirectoryPath();
		loadFileList();
		this.createFileListAdapter();
		this.initializeButtons();
		this.initializeFileListView();
		updateCurrentDirectoryTextView();
		Log.d(TAG, path.getAbsolutePath());
	}
	
	
	
	private void parceDirectoryPath() {
		pathDirsList.clear();
		String pathString = path.getAbsolutePath();
		String[] parts = pathString.split("/");
		int i=0;
		while(i<parts.length) {
			pathDirsList.add(parts[i]);
			i++;
		}
		if(pathDirsList.size()>0) 
			firstLvl = false;
		else	firstLvl = true;
	}
	
	private void initializeButtons() {
		Button upDirButton = (Button)this.findViewById(R.id.upDirectoryButton);
		upDirButton.setOnClickListener(
				new OnClickListener() {
					public void onClick(View v) {
						Log.d(TAG, "onclick for upDirButton");
						loadDirectoryUp();
						loadFileList();
						adapter.notifyDataSetChanged();
						updateCurrentDirectoryTextView();
					}
				}
				);//upDirButton.setOnClickListener(
		
		Button selectFolderButton = (Button)this.findViewById(R.id.selectCurrentDirectoryButton);
		selectFolderButton.setOnClickListener(
				new OnClickListener() {
					public void onClick(View v) {
						Log.d(TAG, "onclick for selectFolderButton");
						returnDirectoryFinishActivity();
					}
				}
				);//upDirButton.setOnClickListener(
	}
	
	private void loadDirectoryUp() {
		// present directory removed from list
		String s = pathDirsList.remove(pathDirsList.size() - 1);
		// path modified to exclude present directory
		path = new File(path.toString().substring(0,
				path.toString().lastIndexOf(s)));
		//fileList = null;
		fileList.clear();
		// if there are no more directories in the list, then
		// its the first level
		if (pathDirsList.isEmpty()) {
			firstLvl = true;
		}
	}
	
	private void updateCurrentDirectoryTextView() {
		int i=0;
		String curDirString = "";
		while(i<pathDirsList.size()) {
			curDirString +=pathDirsList.get(i)+"/";
			i++;
		}
		if(pathDirsList.size()==0) {
			((Button)this.findViewById(R.id.upDirectoryButton)).setEnabled(false);
			curDirString = "/";
		}
		else ((Button)this.findViewById(R.id.upDirectoryButton)).setEnabled(true);
		
		//Log.d(TAG, "Will set curr dir to:"+curDirString);
		((TextView)this.findViewById(
				R.id.currentDirectoryTextView)).setText(
				"Current directory:\n"+curDirString);
		
		
	}
	
	private void showToast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}
	
	
	
	private void initializeFileListView() {
		ListView lView = (ListView)this.findViewById(R.id.fileListView);
		lView.setBackgroundColor(Color.LTGRAY);
		LinearLayout.LayoutParams lParam =
				new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		lParam.setMargins(15, 5, 15, 5);
		lView.setAdapter(this.adapter);
		lView.setOnItemClickListener(
				new AdapterView.OnItemClickListener() {
					public void onItemClick(AdapterView parent, View view, int position, long id) {
						chosenFile = fileList.get(position).file;
						File sel = new File(path + "/" + chosenFile);
						Log.d(TAG, "Clicked:"+chosenFile);
						if (sel.isDirectory()) {
							if(sel.canRead()) {
								firstLvl = false;
								// Adds chosen directory to list
								pathDirsList.add(chosenFile);
								path = new File(sel + "");
								Log.d(TAG, "Just reloading the list");
								//returnDirectoryFinishActivity();
								loadFileList();
								adapter.notifyDataSetChanged();
								updateCurrentDirectoryTextView();
								Log.d(TAG, path.getAbsolutePath());
							} else {//if(sel.canRead()) {
								showToast("Path does not exist or cannot be read");
							}//} else {//if(sel.canRead()) {
						}//if (sel.isDirectory()) {
						// Checks if 'up' was clicked
						else if (chosenFile.equalsIgnoreCase("up") && !sel.exists()) {
							//Below is not needed as there is a separate "Up" button
//							loadDirectoryUp();
//							loadFileList();
//							adapter.notifyDataSetChanged();
//							Log.d(TAG, path.getAbsolutePath());
						}
						// File picked
						else {
							Log.d(TAG, "File select action is not defined");
						}
						//Log.d(TAG, "onClick finished");
					}//public void onClick(DialogInterface dialog, int which) {
				}//new OnItemClickListener() {
		);//lView.setOnClickListener(
	}//private void initializeFileListView() {
	
	private void returnDirectoryFinishActivity() {
		Intent retIntent = new Intent();
		retIntent.putExtra(
				intentReturnDirectoryParameter, 
				path.getAbsolutePath()
		);
		this.setResult(RESULT_OK, retIntent);
		this.finish();
	}//END private void returnDirectoryFinishActivity() {

	private void loadFileList() {
		try {
			path.mkdirs();
		} catch (SecurityException e) {
			Log.e(TAG, "unable to write on the sd card ");
		}
		fileList.clear();

		// Checks whether path exists
		if (path.exists() && path.canRead()) {
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String filename) {
					File sel = new File(dir, filename);
					// Filters based on whether the file is hidden or not
					if(currentAction==SELECT_DIRECTORY) {
						return (sel.isDirectory());
					}
					
					return (sel.isFile() || sel.isDirectory())
							&& !sel.isHidden();
				}
			};

			String[] fList = path.list(filter);
			for (int i = 0; i < fList.length; i++) {
				fileList.add(i, 
						new Item(fList[i], R.drawable.file_icon));
				// Convert into file path
				File sel = new File(path, fList[i]);

				// Set drawables
				if (sel.isDirectory()) {
					fileList.get(i).icon = R.drawable.folder_icon;
					Log.d("DIRECTORY", fileList.get(i).file);
				} else {
					//Log.d("FILE", fileList[i].file);
					Log.d("FILE", fileList.get(i).file);
				}
			}
			
			if(fList.length==0) {
				Log.d(TAG, "This directory is empty");
				fileList.add(0, 
						new Item("Directory is empty", -1));
			}

//			if (!firstLvl) {
//				Item temp[] = new Item[fileList.size() + 1];
//				for (int i = 0; i < fileList.size(); i++) {
//					temp[i + 1] = fileList.get(i);
//				}
//				temp[0] = new Item("Up", R.drawable.directory_up);
//				//fileList = temp;
//				fileList.add(0, 
//						new Item("Up", R.drawable.directory_up));
//			}
		} else {
			Log.e(TAG, "path does not exist or cannot be read");
		}
		//Log.d(TAG, "loadFileList finished");
	}//private void loadFileList() {
	
	private void createFileListAdapter(){
		adapter = new ArrayAdapter<Item>(this,
				android.R.layout.select_dialog_item, android.R.id.text1,
				fileList)
			{
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				// creates view
				//Log.d(TAG, "getView start");
				View view = super.getView(position, convertView, parent);
				TextView textView = (TextView) view
						.findViewById(android.R.id.text1);

				// put the image on the text view
				int drawableID = 0;
				if(fileList.get(position).icon != -1) {
					//Log.d(TAG, "Putting image");
					//If icon == -1, then directory is empty
					drawableID = fileList.get(position).icon;
				}
				textView.setCompoundDrawablesWithIntrinsicBounds(
						drawableID, 0, 0, 0);
				
				textView.setEllipsize(null);

				// add margin between image and text (support various screen
				// densities)
				int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
				textView.setCompoundDrawablePadding(dp5);
				textView.setBackgroundColor(Color.LTGRAY);
				return view;
			}//public View getView(int position, View convertView, ViewGroup parent) {
		};//adapter = new ArrayAdapter<Item>(this,
		//adapter.setNotifyOnChange(true);
		
		
		
	}//private createFileListAdapter(){

	private class Item {
		public String file;
		public int icon;

		public Item(String file, Integer icon) {
			this.file = file;
			this.icon = icon;
		}

		@Override
		public String toString() {
			return file;
		}
	}


}