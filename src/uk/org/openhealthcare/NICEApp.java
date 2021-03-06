/*
*    Copyright (C) 2011  Open Health Care, R.Jones, Dr. VJ Joshi
*    Additions Copyright (C) 2011 Neil McPhail
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package uk.org.openhealthcare;

import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.lang.Boolean;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu; 
import android.view.MenuInflater;
import android.os.AsyncTask;


public class NICEApp extends ListActivity {

	private static boolean downloadLock = false;
	GuidelineData guidelines;
	ListView lv;

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.main_menu, menu);
	return true;
	} 
	
	public boolean onOptionsItemSelected(MenuItem item)
	{
	switch (item.getItemId()) {
	   case R.id.settings: 
		   Toast.makeText(getApplicationContext(), 
	               "Coming soon...", 
	               Toast.LENGTH_LONG).show();
	   			return true;
	   case R.id.help: Toast.makeText(getApplicationContext(), 
               "Choose an item.\nMake sure you have a PDF Reader installed.", 
               Toast.LENGTH_LONG).show();
				return true;
	   case R.id.feedback: Toast.makeText(getApplicationContext(), 
               "http://openhealthcare.org.uk\n\nCome say hello :)", 
               Toast.LENGTH_LONG).show();
	   			return true;	
	   case R.id.about: Toast.makeText(getApplicationContext(), 
               "Developers:\nRoss Jones / Dr VJ Joshi / Neil McPhail", 
               Toast.LENGTH_LONG).show();
				return true;
	   case R.id.get_all: 
		   
		    AlertDialog ad = new AlertDialog.Builder(this).create();  
		    //ad.setCancelable(false); // This blocks the 'BACK' button  
		    ad.setTitle("This will be SLOW");
		    ad.setMessage("Phone may appear to freeze\nPlease let it do its thing\n\nDownload will take approx 3 mins over WiFi (25Mb)");  
		    ad.setButton("Go", new DialogInterface.OnClickListener() {  
		        @Override  
		        public void onClick(DialogInterface dialog, int which) {  
			    new AsyncDownload().execute(guidelines.GetKeys());
				   dialog.dismiss();
		 		   
		        }  
		    });  
		    ad.show();  
		   
		   return true;

	    case R.id.search:

		   onSearchRequested();
		   return true;
			}
		   
	return false;
	}   

	public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  

	  try { 
		  guidelines = new GuidelineData(this);
	  } catch (Exception elocal) {
          Toast.makeText(getApplicationContext(), 
                  "Failed to load guideline list", 
                  Toast.LENGTH_LONG).show();		  
	  }
	  
	  String folderString = pathToStorage(null);
	  File folder = new File(folderString);
	  if ( ! folder.exists() ) {
		  folder.mkdir();
	  }
	  
	  Object[] c = guidelines.GetKeys();
	  Arrays.sort(c);
	  setListAdapter(new ArrayAdapter<String>(this, R.layout.list_item, (String[])c));
	  
	  lv = getListView();
	  lv.setTextFilterEnabled(true);

	  handleIntent(getIntent());

	  lv.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int arg2,
					long arg3) {

					String key = (String) ((TextView) view).getText();
					new AsyncDownload().execute(key);
					// Hash it and look for it on disk, if not on disk then download locally
					
			}		   
	  });

	}

	private void doMySearch(String query) {
		Toast.makeText(getApplicationContext(),
				"Search Button pressed",
				Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	      String query = intent.getStringExtra(SearchManager.QUERY);
	      lv.setFilterText(query);
	    }
	}

	public String MD5_Hash(String s) { 
		  MessageDigest m = null;

		  try {
	            	m = MessageDigest.getInstance("MD5");
		  } catch (NoSuchAlgorithmException e) {
	            	e.printStackTrace();
		  }

	    m.update(s.getBytes(),0,s.length());
	    String hash = new BigInteger(1, m.digest()).toString(16);
	    return hash;
	  }


	public String pathToStorage(String filename) {
		File sdcard = Environment.getExternalStorageDirectory();
		if ( filename != null )
			return sdcard.getAbsolutePath() + File.separator+ "nice_guidance" + File.separator + filename;		
		return sdcard.getAbsolutePath() + File.separator+ "nice_guidance" + File.separator;		
	}
	
	private class AsyncDownload extends AsyncTask<String, String, Boolean> {
		protected void onPreExecute() {
			Toast.makeText(getApplicationContext(),
					"Accessing / downloading",
					Toast.LENGTH_SHORT).show();
		}
		protected Boolean doInBackground(String... guidelinelist) {
			try {
			int count = guidelinelist.length;
			Boolean singlesuccess = Boolean.FALSE; // if called on a single file the pdf viewer may be opened
			for (int i = 0; i < count; i++){
				String url = guidelines.Get(guidelinelist[i]);
				String hash = MD5_Hash(url);
				String targetFile = pathToStorage(hash + ".pdf");
				File file = new File(targetFile);
				if (! file.exists() ) {
					if (downloadLock) {
						publishProgress("Please wait for previous files to download");
						return Boolean.FALSE;
					}
					downloadLock = true;
					DownloadPDF p = new DownloadPDF();
					try {
						p.DownloadFrom(url, targetFile);
						publishProgress("Downloaded " + guidelinelist[i] + " successfully");
						singlesuccess = Boolean.TRUE;
					} catch (Exception exc){
						publishProgress("Failed to download the PDF " + exc.toString());
					}
					downloadLock = false;
				} else {
					singlesuccess = Boolean.TRUE;
				}
			}
			if (count == 1  && singlesuccess && ! downloadLock ) {
				String url = guidelines.Get(guidelinelist[0]);
				String hash = MD5_Hash(url);
				String targetFile = pathToStorage(hash + ".pdf");
				File file = new File(targetFile);
                    		Uri path = Uri.fromFile(file);
                    		Intent intent = new Intent(Intent.ACTION_VIEW);
                    		intent.setDataAndType(path, "application/pdf");
                    		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    		try {
                        		startActivity(intent);
                    		} 
                    		catch (ActivityNotFoundException e) {
                    			publishProgress("No application available to view PDF files");
                    			// Can't do this in a thread.
                        	/*	Toast.makeText(getApplicationContext(), 
                            		"No Application Available to View PDF files", 
                            		Toast.LENGTH_LONG).show(); */
                    		}
			}
			if (count == 1) return singlesuccess;
			return Boolean.TRUE; } catch ( Exception eee ) {
/*				Toast.makeText(getApplicationContext(),
						eee.toString(),
						Toast.LENGTH_SHORT).show();*/
				return Boolean.FALSE;
			}
		}
		protected void onProgressUpdate(String... progress) {
			Toast.makeText(getApplicationContext(),
					progress[0],
					Toast.LENGTH_SHORT).show();
		}
	}

}
