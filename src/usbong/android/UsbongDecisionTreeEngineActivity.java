/*
 * Copyright 2012-2013 Michael Syson
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package usbong.android;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.commons.lang3.StringEscapeUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import usbong.android.features.node.PaintActivity;
import usbong.android.features.node.QRCodeReaderActivity;
import usbong.android.multimedia.audio.AudioRecorder;
import usbong.android.utils.FedorMyLocation;
import usbong.android.utils.FedorMyLocation.LocationResult;
import usbong.android.utils.UsbongUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class UsbongDecisionTreeEngineActivity extends Activity implements TextToSpeech.OnInitListener{
//	private static final boolean UsbongUtils.USE_UNESCAPE=true; //allows the use of \n (new line) in the decision tree

//	private static boolean USE_ENG_ONLY=true; //uses English only	
//	private static boolean UsbongUtils.IS_IN_DEBUG_MODE=false;

	private static final String myPackageName="usbong.android";
	
	private int currLanguageBeingUsed;
	
	public static final int YES_NO_DECISION_SCREEN=0;	
	public static final int MULTIPLE_RADIO_BUTTONS_SCREEN=1;	
	public static final int MULTIPLE_CHECKBOXES_SCREEN=2;	
	public static final int AUDIO_RECORD_SCREEN=3;
	public static final int PHOTO_CAPTURE_SCREEN=4;	
	public static final int TEXTFIELD_SCREEN=5;	
	public static final int TEXTFIELD_WITH_UNIT_SCREEN=6;	
	public static final int TEXTFIELD_NUMERICAL_SCREEN=7;
	public static final int TEXTAREA_SCREEN=8;
	public static final int TEXT_DISPLAY_SCREEN=9;	
	public static final int IMAGE_DISPLAY_SCREEN=10;
	public static final int TEXT_IMAGE_DISPLAY_SCREEN=11;
	public static final int CLASSIFICATION_SCREEN=12;		
	public static final int DATE_SCREEN=13;	
	public static final int TIMESTAMP_DISPLAY_SCREEN=14;		
	public static final int GPS_LOCATION_SCREEN=15;		
	public static final int VIDEO_FROM_FILE_SCREEN=16;	
	public static final int LINK_SCREEN=17;			
	public static final int SEND_TO_WEBSERVER_SCREEN=18;		
	public static final int SEND_TO_CLOUD_BASED_SERVICE_SCREEN=19;	
	public static final int PAINT_SCREEN=20;
	public static final int QR_CODE_READER_SCREEN=21;
	public static final int CLICKABLE_IMAGE_DISPLAY_SCREEN=22;
	public static final int TEXT_CLICKABLE_IMAGE_DISPLAY_SCREEN=23;
	public static final int DCAT_SUMMARY_SCREEN=24;			
	public static final int MULTIPLE_RADIO_BUTTONS_WITH_ANSWER_SCREEN=25;	
	public static final int TEXTFIELD_WITH_ANSWER_SCREEN=26;	
	public static final int TEXTAREA_WITH_ANSWER_SCREEN=27;	

	public static final int END_STATE_SCREEN=28;		
	
	private static int currScreen=TEXTFIELD_SCREEN;
	
	public static final int PLEASE_CHOOSE_AN_ANSWER_ALERT_TYPE=0;
	public static final int PLEASE_ANSWER_FIELD_ALERT_TYPE=1;

	
	private Button backButton;
	private Button nextButton;	
	
	private Button stopButton;
	private Button recordButton;
	private Button playButton;
	
	private static AudioRecorder currAudioRecorder;

	private Button paintButton;
	private Button photoCaptureButton;
	private ImageView myImageView;

	private Button qrCodeReaderButton;

	public static Intent photoCaptureIntent;
	public static Intent paintIntent;
	public static Intent qrCodeReaderIntent;
	
	private static String myPictureName="default"; //change this later in the code
	private static String myPaintName="default"; //change this later in the code
	private static String myQRCodeReaderName="default"; //change this later in the code

	private boolean hasReachedEndOfAllDecisionTrees;
	private boolean isFirstQuestionForDecisionTree;

	private boolean usedBackButton;
	
	private boolean performedCapturePhoto;
	private boolean performedRunPaint;
	private boolean performedGetQRCode;
	
	private String currUsbongNode="";
	private String nextUsbongNodeIfYes;
	private String nextUsbongNodeIfNo;

	private String currUsbongNodeWithoutAnswer="";
	
	private String textFieldUnit="";
		
	private int usbongNodeContainerCounter=-1;//because I do a ++, so that the first element would be at 0;
	private int requiredTotalCheckedBoxes;
	private Vector<String> usbongNodeContainer;
	private Vector<String> classificationContainer;
	private Vector<String> radioButtonsContainer;
	private Vector<String> usbongAnswerContainer;
	private Vector<String> checkBoxesContainer;
	private Vector<String> decisionTrackerContainer; //added by Mike, Feb. 2, 2013

	private String noStringValue;
	private String yesStringValue;

//	private String myTreeDirectory="usbong_trees/";
	private String myTree="no tree selected.";//"input.xml";
	private String myOutputDirectory=UsbongUtils.getDateTimeStamp()+"/"; //add the ".csv" after appending the timestampe //output.csv
	
	private static UsbongDecisionTreeEngineActivity instance;
    private static TextToSpeech mTts;
    private int MY_DATA_CHECK_CODE=0;
	private static final int EMAIL_SENDING_SUCCESS=99;

	public ListView treesListView;
	
	private CustomDataAdapter mCustomAdapter;
	private ArrayList<String> listOfTreesArrayList;

	private ArrayAdapter<CharSequence> monthAdapter;
	private ArrayAdapter<CharSequence> dayAdapter;
	
	private List<String> attachmentFilePaths;
	
	private FedorMyLocation myLocation;
	
	private boolean isInTreeLoader;
	
	private static String myQRCodeContent;
    private static boolean hasReturnedFromAnotherActivity; //camera, paint, email, etc
	private static boolean wasNextButtonPressed;
	private static boolean hasUpdatedDecisionTrackerContainer;
	
	private static boolean isAnOptionalNode;
	private static String currentAnswer;
	private static int usbongAnswerContainerCounter;
	
    private int padding_in_dp = 5;  // 5 dps
    private int padding_in_px;
    
    private String myMultipleRadioButtonsWithAnswerScreenAnswer;
    private String myTextFieldWithAnswerScreenAnswer;
    private String myTextAreaWithAnswerScreenAnswer;
    private String timestampString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
                
        instance=this;

        UsbongUtils.myAssetManager = getAssets();
        
        //if return is null, then currScreen=0
        currScreen=Integer.parseInt(getIntent().getStringExtra("currScreen")); 

        //default..
        currLanguageBeingUsed=UsbongUtils.LANGUAGE_ENGLISH;
        
        //==================================================================
        //text-to-speech stuff
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);

        mTts = new TextToSpeech(this,this);
		mTts.setLanguage(new Locale("eng", "EN"));//default
        //==================================================================
        
    	usbongNodeContainer = new Vector<String>();
    	classificationContainer = new Vector<String>();
    	radioButtonsContainer = new Vector<String>();
    	usbongAnswerContainer = new Vector<String>();
    	checkBoxesContainer = new Vector<String>();
    	decisionTrackerContainer = new Vector<String>();

    	usedBackButton=false;
    	currentAnswer="";    	    	
    	
    	try{    		
    		UsbongUtils.createUsbongFileStructure();
    		//create the usbong_demo_tree and store it in sdcard/usbong/usbong_trees
    		UsbongUtils.storeAssetsFileIntoSDCard(this,"usbong_demo_tree.xml");
    	}
    	catch(IOException ioe) {
    		ioe.printStackTrace();
    	}
    	
    	//Reference: http://stackoverflow.com/questions/2793004/java-lista-addalllistb-fires-nullpointerexception
    	//Last accessed: 14 March 2012
    	attachmentFilePaths = new ArrayList<String>();            	
//		attachmentFilePaths.clear();
//		System.out.println(">>>>>>>>> attachmentFilePaths.clear!");
		currAudioRecorder = null;
		
		myQRCodeContent="";
	    hasReturnedFromAnotherActivity=false; //camera, paint, email, etc

	    //added by Mike, March 4, 2013
	    usbongAnswerContainerCounter=0;
	    
        //reference: Labeeb P's answer from stackoverflow;
        //http://stackoverflow.com/questions/4275797/view-setpadding-accepts-only-in-px-is-there-anyway-to-setpadding-in-dp;
        //last accessed: 23 May 2013
        final float scale = getResources().getDisplayMetrics().density;
        padding_in_px = (int) (padding_in_dp * scale + 0.5f);
	    
    	initTreeLoader();
    }
    
    public class MyOnItemSelectedListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent,
            View view, int pos, long id) {
/*
        	Toast.makeText(parent.getContext(), "The month is " +
              parent.getItemAtPosition(pos).toString(), Toast.LENGTH_LONG).show();
*/              
        }

        public void onNothingSelected(AdapterView parent) {
          // Do nothing.
        }
    }
    
	public void initTreeLoader()
	{
		setContentView(R.layout.tree_list_interface);				

		isInTreeLoader=true;
		
		listOfTreesArrayList = UsbongUtils.getTreeArrayList(UsbongUtils.USBONG_TREES_FILE_PATH);
		
		mCustomAdapter = new CustomDataAdapter(this, R.layout.tree_loader, listOfTreesArrayList);
		
		treesListView = (ListView)findViewById(R.id.tree_list_view);
		treesListView.setLongClickable(true);
		treesListView.setAdapter(mCustomAdapter);

    	String pleaseMakeSureThatXMLTreeExistsString = (String) getResources().getText(R.string.pleaseMakeSureThatXMLTreeExistsString);
    	String alertString = (String) getResources().getText(R.string.alertStringValueEnglish);

		if (listOfTreesArrayList.isEmpty()){
        	new AlertDialog.Builder(UsbongDecisionTreeEngineActivity.this).setTitle(alertString)
			.setMessage(pleaseMakeSureThatXMLTreeExistsString)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {					
				@Override
				public void onClick(DialogInterface dialog, int which) {
		    		finish();    
					Intent toUsbongMainActivityIntent = new Intent(UsbongDecisionTreeEngineActivity.this, UsbongMainActivity.class);
					toUsbongMainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
					startActivity(toUsbongMainActivityIntent);
				}
			}).show();	        		        	
		  }		
	}
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		if (!isInTreeLoader) {
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.speak_and_set_language_menu, menu);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{		
		if (mTts.isSpeaking()) {
			mTts.stop();
		}
		
		StringBuffer sb = new StringBuffer();
		switch(item.getItemId())
		{
			case(R.id.set_language):
				final Dialog dialog = new Dialog(this);
			
				dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				dialog.setContentView(R.layout.set_language_dialog);

				Button selectButton = (Button)dialog.findViewById(R.id.select_button);

		        RadioGroup radioGroup = (RadioGroup)dialog.findViewById(R.id.multiple_radio_buttons_radiogroup);
		        ArrayList<String> myTransArrayList = UsbongUtils.getAvailableTranslationsArrayList(myTree);
/*		        
		        if ((myTransArrayList==null) || (myTransArrayList.size()==0)) {
					TextView myTextView = (TextView)dialog.findViewById(R.id.set_language_textview);
		        	myTextView.setText("No language available to select from.");
		        	selectButton.setText("OK");
					selectButton.setOnClickListener(new View.OnClickListener() {					
						public void onClick(View v) {
							dialog.cancel();
						}					
					});
		        }
*/
		        if (myTransArrayList==null) {
		        	myTransArrayList = new ArrayList<String>();
		        }
		        //add the language setting of the xml tree to the list
		        myTransArrayList.add(0, UsbongUtils.getDefaultLanguage());
		        
		        final int totalTrans = myTransArrayList.size();
		        for (int i=0; i<totalTrans; i++) {
		        	RadioButton radioButton = new RadioButton(getBaseContext());		        	
		        	radioButton.setText(myTransArrayList.get(i));
		            radioButton.setChecked(false);
		            radioButton.setTextSize(20);
		            radioButton.setId(i);
//		            radioButton.setTextColor(Color.parseColor("#4a452a"));	

		            if (radioButton.getText().toString().equals(UsbongUtils.getSetLanguage())) {
		            	radioButton.setChecked(true);
		            }
		        
		            radioButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton myRadioButton,
								boolean isChecked) {
							if (isChecked) {
								UsbongUtils.setLanguage(myRadioButton.getText().toString());				        		
							}
						}		            	
		            });		            
		            
		            radioGroup.addView(radioButton);
		        }		     		        

				selectButton.setOnClickListener(new View.OnClickListener() {					
					public void onClick(View v) {
						currLanguageBeingUsed = UsbongUtils.getLanguageID(UsbongUtils.getSetLanguage());
						initParser();
						//cancel the dialog the setLanguage() method has already been called when button is checked
				        dialog.cancel();
					}					
				});

				
				Button cancelButton = (Button)dialog.findViewById(R.id.cancel_button);
				cancelButton.setOnClickListener(new View.OnClickListener() {					
					public void onClick(View v) {
						dialog.cancel();
					}					
				});
				
				//Reference: http://stackoverflow.com/questions/6204972/override-dialog-onbackpressed; last accessed: 18 Aug. 2012
				dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
		            @Override
		            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		                if (keyCode == KeyEvent.KEYCODE_BACK) {
		                	   dialog.cancel();
		                       return true;
		                }
		                return false;
		            }
		        });	
				dialog.show();
				return true;
			case(R.id.speak):
//				Log.d(">>>>currScreen",currScreen+"");
				switch(currScreen) {
					//edit later, Mike, May 23, 2013
					case DCAT_SUMMARY_SCREEN:
						break;
						
			    	case LINK_SCREEN:
			    	case MULTIPLE_RADIO_BUTTONS_SCREEN:
			    	case MULTIPLE_RADIO_BUTTONS_WITH_ANSWER_SCREEN:
				        sb.append(((TextView) UsbongUtils.applyTagsInView(new TextView(this), UsbongUtils.IS_TEXTVIEW, currUsbongNode)).getText().toString()+". ");

				        int totalRadioButtonsInContainer = radioButtonsContainer.size();
				        for (int i=0; i<totalRadioButtonsInContainer; i++) {
					        sb.append(((RadioButton) UsbongUtils.applyTagsInView(new RadioButton(this), UsbongUtils.IS_RADIOBUTTON, radioButtonsContainer.elementAt(i))).getText().toString()+". ");
				        }		     		        
						break;
			    	case MULTIPLE_CHECKBOXES_SCREEN:
				        sb.append(((TextView) UsbongUtils.applyTagsInView(new TextView(this), UsbongUtils.IS_TEXTVIEW, currUsbongNode)).getText().toString()+". ");

				        int totalCheckBoxesInContainer = checkBoxesContainer.size();
				        for (int i=0; i<totalCheckBoxesInContainer; i++) {
					        sb.append(((CheckBox) UsbongUtils.applyTagsInView(new CheckBox(this), UsbongUtils.IS_CHECKBOX, checkBoxesContainer.elementAt(i))).getText().toString()+". ");
				        }		     		        
				        break;
			    	case AUDIO_RECORD_SCREEN:
				        sb.append(((TextView) UsbongUtils.applyTagsInView(new TextView(this), UsbongUtils.IS_TEXTVIEW, currUsbongNode)).getText().toString()+". ");

				        Button recordButton = (Button)findViewById(R.id.record_button);
				        Button stopButton = (Button)findViewById(R.id.stop_button);
				        Button playButton = (Button)findViewById(R.id.play_button);

				        sb.append(recordButton.getText()+". ");
				        sb.append(stopButton.getText()+". ");
				        sb.append(playButton.getText()+". ");
				        break;
					case PAINT_SCREEN:
			    		sb.append(((TextView) UsbongUtils.applyTagsInView(new TextView(this), UsbongUtils.IS_TEXTVIEW, currUsbongNode)).getText().toString()+". ");

			    		Button paintButton = (Button)findViewById(R.id.paint_button);
				        sb.append(paintButton.getText()+". ");
			    		break;
					case PHOTO_CAPTURE_SCREEN:
			    		sb.append(((TextView) UsbongUtils.applyTagsInView(new TextView(this), UsbongUtils.IS_TEXTVIEW, currUsbongNode)).getText().toString()+". ");

			    		Button photoCaptureButton = (Button)findViewById(R.id.photo_capture_button);
				        sb.append(photoCaptureButton.getText()+". ");
			    		break;
					case TEXTFIELD_SCREEN:
					case TEXTFIELD_WITH_ANSWER_SCREEN:						
					case TEXTFIELD_WITH_UNIT_SCREEN:
					case TEXTFIELD_NUMERICAL_SCREEN:
					case TEXTAREA_SCREEN:
					case TEXTAREA_WITH_ANSWER_SCREEN:						
						sb.append(((TextView) UsbongUtils.applyTagsInView(new TextView(this), UsbongUtils.IS_TEXTVIEW, currUsbongNode)).getText().toString()+". ");
				        break;    	
					case CLASSIFICATION_SCREEN:
				        sb.append(((TextView) UsbongUtils.applyTagsInView(new TextView(this), UsbongUtils.IS_TEXTVIEW, currUsbongNode)).getText().toString()+". ");

				        int totalClassificationsInContainer = classificationContainer.size();
				        for (int i=0; i<totalClassificationsInContainer; i++) {
					        sb.append(((TextView) UsbongUtils.applyTagsInView(new TextView(this), UsbongUtils.IS_TEXTVIEW, classificationContainer.elementAt(i))).getText().toString()+". ");
				        }		     		        
				        break;    	
					case DATE_SCREEN:				       
					case TEXT_DISPLAY_SCREEN:
					case TEXT_IMAGE_DISPLAY_SCREEN:
					case GPS_LOCATION_SCREEN:
					case QR_CODE_READER_SCREEN:
					case TIMESTAMP_DISPLAY_SCREEN:						
				        sb.append(((TextView) UsbongUtils.applyTagsInView(new TextView(this), UsbongUtils.IS_TEXTVIEW, currUsbongNode)).getText().toString()+". ");
//				        Log.d(">>>>sb",sb.toString());
				        break;
					case CLICKABLE_IMAGE_DISPLAY_SCREEN:				       
					case IMAGE_DISPLAY_SCREEN:
					case VIDEO_FROM_FILE_SCREEN:							
				        break;    	
					case YES_NO_DECISION_SCREEN:
					case SEND_TO_WEBSERVER_SCREEN:
					case SEND_TO_CLOUD_BASED_SERVICE_SCREEN:
				        sb.append(((TextView) UsbongUtils.applyTagsInView(new TextView(this), UsbongUtils.IS_TEXTVIEW, currUsbongNode)).getText().toString()+". ");
				        sb.append(yesStringValue+". ");
				        sb.append(noStringValue+". ");
				        break;    	
/*						
					case PAINT_SCREEN:
				    	if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_FILIPINO) {
							sb.append((String) getResources().getText(R.string.UsbongPaintScreenTextViewFILIPINO));
				    	}
				    	else if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_JAPANESE) {
							sb.append((String) getResources().getText(R.string.UsbongPaintScreenTextViewJAPANESE));				    						    		
				    	}
				    	else { //if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_ENGLISH) {
							sb.append((String) getResources().getText(R.string.UsbongPaintScreenTextViewENGLISH));				    						    		
				    	}
				    	break;    		
*/				    	
					case END_STATE_SCREEN:
				    	if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_FILIPINO) {
							sb.append((String) getResources().getText(R.string.UsbongEndStateTextViewFILIPINO));				    		
				    	}
				    	else if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_JAPANESE) {
							sb.append((String) getResources().getText(R.string.UsbongEndStateTextViewJAPANESE));				    						    		
				    	}
				    	else { //if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_ENGLISH) {
							sb.append((String) getResources().getText(R.string.UsbongEndStateTextViewENGLISH));				    						    		
				    	}
				    	break;    		
				}
	        	//it's either com.svox.pico (default) or com.svox.classic (Japanese, etc)        				
				mTts.setEngineByPackageName("com.svox.pico"); //note: this method is already deprecated
				switch (currLanguageBeingUsed) {
					case UsbongUtils.LANGUAGE_FILIPINO:				    
						mTts.setLanguage(new Locale("spa", "ESP"));
						mTts.speak(UsbongUtils.convertFilipinoToSpanishAccentFriendlyText(sb.toString()), TextToSpeech.QUEUE_ADD, null); //QUEUE_FLUSH			
						break;
					case UsbongUtils.LANGUAGE_JAPANESE:
				        mTts.setEngineByPackageName("com.svox.classic"); //note: this method is already deprecated
						mTts.setLanguage(new Locale("ja", "ja_JP"));
						mTts.speak(sb.toString(), TextToSpeech.QUEUE_ADD, null); //QUEUE_FLUSH			
						break;
					case UsbongUtils.LANGUAGE_ENGLISH:
						mTts.setLanguage(new Locale("eng", "EN"));
						mTts.speak(sb.toString(), TextToSpeech.QUEUE_ADD, null); //QUEUE_FLUSH			
						break;
					default:
						mTts.setLanguage(new Locale("eng", "EN"));
						mTts.speak(sb.toString(), TextToSpeech.QUEUE_ADD, null); //QUEUE_FLUSH			
						break;
				}
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onInit(int status) {
		//answer from Eternal Learner, stackoverflow
		//Reference: http://stackoverflow.com/questions/9473070/texttospeech-setenginebypackagename-doesnt-set-anything;
		//last accessed: Nov. 7, 2012
		//mTts.getDefaultEngine() and mTts.setEngineByPackageName(...) can only be called only when onInit(...) is reached		
/*		
		String myEngine = mTts.getDefaultEngine();
        System.out.println(">>>>>>>>>>>>>>> myEngine: "+myEngine);

        //it's either com.svox.pico (default) or com.svox.classic (Japanese, etc)        
        mTts.setEngineByPackageName(myEngine); //note: this method is already deprecated
*/
	}

    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {

    	if (requestCode == MY_DATA_CHECK_CODE) {
    		/*
        	if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance
                mTts = new TextToSpeech(this, this);
            } else {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(
                    TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
*/
        	if (mTts==null) {
                Intent installIntent = new Intent();
                installIntent.setAction(
                    TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);        		        	
            }
            mTts = new TextToSpeech(this, this);        		
        }
        else if (requestCode==EMAIL_SENDING_SUCCESS) {
    		finish();    		
			Intent toUsbongMainActivityIntent = new Intent(UsbongDecisionTreeEngineActivity.this, UsbongMainActivity.class);
			toUsbongMainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
			startActivity(toUsbongMainActivityIntent);
    		if (mTts!=null) {
    			mTts.shutdown();
    		}
        }
    }
/*    
	public void onDestroy() {
		if (mTts!=null) {
			mTts.shutdown();
		}
	}
*/	
    public static UsbongDecisionTreeEngineActivity getInstance() {
    	return instance;
    }
    
    public static void setMyIntent(Intent i) {
      getInstance().setIntent(i);
    }
    
    public static void setCurrScreen(int cs) {
    	currScreen=cs;
    }
    
    /*
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (getParent()!=null) { 
	        if (keyCode == KeyEvent.KEYCODE_BACK) {
	        	processReturnToMainMenuActivity();
	        	return false;
	        }
    	}
        return super.onKeyDown(keyCode, event);
    }
    */
    
    //added by Mike, Feb. 2, 2013
    @Override
	public void onBackPressed() {
    	processReturnToMainMenuActivity();    
    }
    
    @Override
    public void onRestart() 
    {
        super.onRestart();
        
    	//added by Mike, Dec. 24, 2012
//    	Log.d(">>>>>> onActivityResult", "inside!!!");
        hasReturnedFromAnotherActivity=true; //camera, paint, email, etc

        switch(currScreen) {
        	case PHOTO_CAPTURE_SCREEN:
        		initTakePhotoScreen();
        		break;
        }
        initParser(); 
    }

    @Override
    public void onPause() {
    	super.onPause();
    	if (myLocation!=null) {
    	  myLocation.cancelTimer();    	
    	}
    }
    
	public void parseYesNoAnswers(XmlPullParser parser) {
		try {
			if (parser.getName().equals("transition")) {				

				//check if the first transition's name is "No"
//				  if (parser.getAttributeValue(1).toString().equals(noStringValue)) {

				//if the edge or arrow doesn't have a label (e.g. Yes, No, Any), make it an "Any" as default
				 //added by Mike, Aug. 13, 2012
				if (parser.getAttributeValue(null, "name") == null) {
					  nextUsbongNodeIfYes = /*parser.getAttributeValue(null,"to").toString();*/parser.getAttributeValue(0).toString();
					  nextUsbongNodeIfNo = /*parser.getAttributeValue(null,"to").toString();*/parser.getAttributeValue(0).toString();						  							  							  					
				}
				 //added by Mike, March 8, 2012
				 //add the || just in case the language is Filipino, but we used "Yes/No" for the transitions
				 else if (parser.getAttributeValue(null,"name").equals(noStringValue)
						  || parser.getAttributeValue(null,"name").equals("No")) {
					  nextUsbongNodeIfNo = /*parser.getAttributeValue(null,"to").toString();*/parser.getAttributeValue(0).toString();
					  //do two nextTag()'s, because <transition> has a closing tag
					  parser.nextTag();
					  parser.nextTag();						  
					  nextUsbongNodeIfYes = /*parser.getAttributeValue(null,"to").toString();*/parser.getAttributeValue(0).toString();
				  }
//				  else if (parser.getAttributeValue(1).toString().equals(yesStringValue)) { // if it is "Yes"
				  else if (parser.getAttributeValue(null,"name").equals(yesStringValue)
				  		  || parser.getAttributeValue(null,"name").equals("Yes")){
				  	  nextUsbongNodeIfYes = /*parser.getAttributeValue(null,"to").toString();*/parser.getAttributeValue(0).toString();
					  //do two nextTag()'s, because <transition> has a closing tag
					  parser.nextTag();
					  parser.nextTag();						  
					  nextUsbongNodeIfNo = /*parser.getAttributeValue(null,"to").toString();*/parser.getAttributeValue(0).toString();						  							  
				  }
				  else { // if it is "Any"
					  nextUsbongNodeIfYes = /*parser.getAttributeValue(null,"to").toString();*/parser.getAttributeValue(0).toString();
					  nextUsbongNodeIfNo = /*parser.getAttributeValue(null,"to").toString();*/parser.getAttributeValue(0).toString();						  							  							  
				  }				  
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}		
	}
    
	//Reference: 
	//http://wiki.forum.nokia.com/index.php/How_to_parse_an_XML_file_in_Java_ME_with_kXML ;Last accessed on: June 2,2010
	//http://kxml.sourceforge.net/kxml2/ ;Last accessed on: June 2,2010    
    //http://developer.android.com/reference/org/xmlpull/v1/XmlPullParser.html; last accessed on: Aug. 23, 2011
	public void initParser() {
		hasReachedEndOfAllDecisionTrees=false;
		
//			decisionTrackerContainer.addElement(usbongAnswerContainer.lastElement());
		
		if (wasNextButtonPressed) {
			if (!hasUpdatedDecisionTrackerContainer) {
				decisionTrackerContainer.addElement(usbongAnswerContainer.elementAt(usbongAnswerContainerCounter-1));
				hasUpdatedDecisionTrackerContainer=true;
			}
			
//			System.out.println(">>>>>>>>>> wasNextButtonPressed");
//			System.out.println(">>>>>>>>>> usbongAnswerContainerCounter: "+usbongAnswerContainerCounter);

			for (int i=0; i<usbongAnswerContainer.size(); i++) {
				System.out.println(i+": "+usbongAnswerContainer.elementAt(i));
			}			
			
			if ((!usbongAnswerContainer.isEmpty()) && (usbongAnswerContainerCounter < usbongAnswerContainer.size())) {
				currentAnswer = usbongAnswerContainer.elementAt(usbongAnswerContainerCounter);
//				System.out.println(">>>> loob currentAnswer: "+currentAnswer);
			}
			else {
				currentAnswer="";
			}

			
			wasNextButtonPressed=false;
		}

		try {
		  XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
	      factory.setNamespaceAware(true);
		  XmlPullParser parser = factory.newPullParser();		 		  
		  
		  //if *.xml is blank
//		  if (UsbongUtils.getFileFromSDCardAsReader(UsbongUtils.BASE_FILE_PATH + myTreeDirectory + myTree + ".xml") == null) { 
//		  UsbongUtils.getTreeFromSDCardAsReader(/*UsbongUtils.USBONG_TREES_FILE_PATH + */myTree));// + ".xml")		  
//		  if (!hasRetrievedTree) {
		  InputStreamReader isr = UsbongUtils.getTreeFromSDCardAsReader(myTree);
			  if (isr==null) {
				  Toast.makeText(getApplicationContext(), "Error loading: "+myTree, Toast.LENGTH_LONG).show();  
				  return;
			  }
//			  hasRetrievedTree=true;
//		  }
		  parser.setInput(isr);	
				  
		  while(parser.nextTag() != XmlPullParser.END_DOCUMENT) {
			  //if this tag does not have an attribute; e.g. END_TAG
			  if (parser.getAttributeCount()==-1) {
				  continue;
			  }
			  //if this is the first process-definition tag
			  else if (parser.getAttributeCount()>1) { 
				  if ((currUsbongNode.equals("")) && (parser.getName().equals("process-definition"))) {
					  currLanguageBeingUsed=UsbongUtils.getLanguageID(parser.getAttributeValue(null, "lang"));
					  UsbongUtils.setDefaultLanguage(UsbongUtils.getLanguageBasedOnID(currLanguageBeingUsed));
					  
					  //added by Mike, Feb. 2, 2013
					  decisionTrackerContainer.removeAllElements();					  
				  }
				  continue;
			  }
			  else if (parser.getAttributeCount()==1) {
				  //if currUsbongNode is still blank
				  //getName() is not the name attribute, but the name of the tag (e.g. start-state, decision-node, task-node)
				  if ((currUsbongNode.equals("")) && (parser.getName().equals("start-state"))) {
					  //the next tag would be transition
					  //Example:
				      //<start-state name="start-state1">
					  //  <transition to="Is there tender swelling behind the ear?"></transition>
					  //</start-state>

					  //do two next()'s to skip the "\n", and proceed to <transition>...
//					  parser.next();
//					  parser.next();
					  //or do one nextTag();
					  parser.nextTag();
					  currUsbongNode=parser.getAttributeValue(0).toString();
					  
					  isFirstQuestionForDecisionTree=true;

					  //added by Mike, Dec. 24, 2012
					  //reset myQRCodeContent to blank
					  myQRCodeContent="";
					  continue;
				  }
				  if ((!currUsbongNode.equals("")) && (parser.getAttributeValue(0).toString().equals(currUsbongNode)) &&
						  !(parser.getName().equals("transition"))) { //make sure that the tag is not a transition node 
				      if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_FILIPINO) {
				    	noStringValue = (String) getResources().getText(R.string.noStringValueFilipino);
				    	yesStringValue = (String) getResources().getText(R.string.yesStringValueFilipino);
				      }
				      else if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_JAPANESE) {
				    	noStringValue = (String) getResources().getText(R.string.noStringValueJapanese); //noStringValue
				    	yesStringValue = (String) getResources().getText(R.string.yesStringValueJapanese); //yesStringValue    		
					  }						  
				      else { //if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_ENGLISH) {
				    	noStringValue = (String) getResources().getText(R.string.noStringValueEnglish); //noStringValue
				    	yesStringValue = (String) getResources().getText(R.string.yesStringValueEnglish); //yesStringValue    		
					  }						  

					  //if this is a decision tag and the next tag is transition...
					  //Example:
					  //<decision name="Is there tender swelling behind the ear?">
					  //  <transition to="Is there pus draining from the ear?" name="No"></transition>
					  //  <transition to="MASTOIDITIS" name="Yes"></transition>
					  //</decision>
					  if (parser.getName().equals("decision")) {//transition")) {//(parser.getAttributeCount()>1) {
						  currScreen=YES_NO_DECISION_SCREEN;
						  parser.nextTag(); //go to the next tag
						  parseYesNoAnswers(parser);
					  }
					  else if (parser.getName().equals("end-state")) { 
						  //temporarily do this
						  currScreen=END_STATE_SCREEN;
					  }
					  else if (parser.getName().equals("task-node")) { 
						  	StringTokenizer st = new StringTokenizer(currUsbongNode, "~");
							String myStringToken = st.nextToken();							

							if (myStringToken.equals(currUsbongNode)) {//if this is the task-node for classification and treatment/management plan								
								//<task-node name="SOME DEHYDRATION">
								//	<task name="Give fluid, zinc supplements and food for some dehydration (Plan B)"></task>
								//	<task name="If child also has a severe classification: 1) Refer URGENTLY to hospital with mother giving frequent sips of ORS on the way, 2) Advise the mother to continue breastfeeding"></task>
								//	<task name="Advise mother when to return immediately"></task>
								//	<task name="Follow-up in 5 days if not improving"></task>
								//	<transition to="end-state1" name="to-end"></transition>
								//</task-node>
								parser.nextTag(); //go to task tag

								currScreen = CLASSIFICATION_SCREEN;
								classificationContainer.removeAllElements();
								while(!parser.getName().equals("transition")) {
									  classificationContainer.addElement(parser.getAttributeValue(0).toString());
									  //do two nextTag()'s, because <task> has a closing tag
									  parser.nextTag();
									  parser.nextTag();		
								}
								nextUsbongNodeIfYes = parser.getAttributeValue(0).toString();
								nextUsbongNodeIfNo = parser.getAttributeValue(0).toString();						  							  							  
							}
							else { //this is a task-node that has "~"								
								if (myStringToken.equals("radioButtons")) {
									//<task-node name="radioButtons~1~Life is good">
									//	<task name="Strongly Disagree"></task>
									//	<task name="Moderately Disagree"></task>
									//	<task name="Slightly Disagree"></task>
									//	<task name="Slightly Agree"></task>
									//	<task name="Moderately Agree"></task>
									//	<task name="Strongly Agree"></task>
									//	<transition to="radioButtons~1~I don't think that the world is a good place" name="Any"></transition>
									//</task-node>
									parser.nextTag(); //go to task tag

									//radioButtons by definition requires only 1 ticked button in the group
									//requiredTotalCheckedBoxes = Integer.parseInt(st.nextToken());
/*																		
									String t=st.nextToken();
									//do this so that we are sure that the token that we are going to check is the last token
									while(st.hasMoreTokens()) {
										t=st.nextToken();
									}
*/									
									currScreen=MULTIPLE_RADIO_BUTTONS_SCREEN;
									
									radioButtonsContainer.removeAllElements();
									while(!parser.getName().equals("transition")) {
										  radioButtonsContainer.addElement(parser.getAttributeValue(0).toString());
										  
										  //do two nextTag()'s, because <task> has a closing tag
										  parser.nextTag();
										  parser.nextTag();		
									}
									parseYesNoAnswers(parser);
								}
								if (myStringToken.equals("radioButtonsWithAnswer")) {
									//<task-node name="radioButtonsWithAnswer~You see your teacher approaching you. What do you do?Answer=0">
									//	<task name="Greet him."></task>
									//	<task name="Run away."></task>
									//	<task name="Ignore him."></task>
									//	<transition to="textDisplay~Hello!" name="Yes"></transition>
									//	<transition to="textDisplay~You feel sad that you didn't greet him." name="No"></transition>
									//</task-node>
									parser.nextTag(); //go to task tag
									//radioButtons by definition requires only 1 ticked button in the group
									currScreen=MULTIPLE_RADIO_BUTTONS_WITH_ANSWER_SCREEN;
									
									radioButtonsContainer.removeAllElements();
									while(!parser.getName().equals("transition")) {
										  radioButtonsContainer.addElement(parser.getAttributeValue(0).toString());
										  
										  //do two nextTag()'s, because <task> has a closing tag
										  parser.nextTag();
										  parser.nextTag();		
									}
									parseYesNoAnswers(parser);
								}								
								else if (myStringToken.equals("link")) {
									//<task-node name="link~1~What will you do?">
									//	<task name="textDisplay~You ate the fruit.~I will eat the fruit."></task>
									//	<task name="textDisplay~You looked for another solution.~I will look for another solution"></task>
									//	<transition to="textDisplay~You ate the fruit.~I will eat the fruit." name="Any"></transition>
									//</task-node>
									//the transition is the default
									parser.nextTag(); //go to task tag

									//radioButtons by definition requires only 1 ticked button in the group
									//requiredTotalCheckedBoxes = Integer.parseInt(st.nextToken());
/*																		
									String t=st.nextToken();
									//do this so that we are sure that the token that we are going to check is the last token
									while(st.hasMoreTokens()) {
										t=st.nextToken();
									}
*/									
									currScreen=LINK_SCREEN;
									
									radioButtonsContainer.removeAllElements();
									while(!parser.getName().equals("transition")) {
										  radioButtonsContainer.addElement(parser.getAttributeValue(0).toString());
										  Log.d(">>>>>parser.getAttributeValue(0).toString()",parser.getAttributeValue(0).toString());
										  //do two nextTag()'s, because <task> has a closing tag
										  parser.nextTag();
										  parser.nextTag();		
									}
									parseYesNoAnswers(parser);
								}
								else if (myStringToken.equals("checkList")) {
									//<task-node name="checkList~2~Check for SEVERE DEHYDRATION. Has at least two (2) of the following signs:">
									//	<task name="Is lethargic or unconscious."></task>
									//	<task name="Has sunken eyes."></task>
									//	<task name="Is not able to drink or is drinking poorly."></task>
									//	<task name="Skin pinch goes back VERY slowly (longer than 2 seconds)."></task>
									//	<transition to="SEVERE DEHYDRATION" name="Yes"></transition>
									//	<transition to="checkList-Check for SOME DEHYDRATION. Has at least two (2) of the following signs:" name="No"></transition>
									//</task-node>
									parser.nextTag(); //go to task tag
									requiredTotalCheckedBoxes = Integer.parseInt(st.nextToken());
/*																		
									String t=st.nextToken();
									//do this so that we are sure that the token that we are going to check is the last token
									while(st.hasMoreTokens()) {
										t=st.nextToken();
									}
*/									
/*									
									//added by Mike, Aug. 20, 2010; st.nextToken()
									if (t.equals(GENERAL_DANGER_SIGNS_CHECKBOX_QUESTION)) { //Does the question pertain to general danger signs?
										isAGeneralDangerSignsCheckBox=true;
									}
									//added by Mike, Sept. 17, 2010
									else if (t.equals(SOME_DEHYDRATION_CHECKBOX_QUESTION)) {
										isSomeDehydrationCheckBox=true;
									}
									else if (t.equals(SEVERE_DEHYDRATION_CHECKBOX_QUESTION)) {
										isSevereDehydrationCheckBox=true;
									}
*/									
									currScreen=MULTIPLE_CHECKBOXES_SCREEN;

									checkBoxesContainer.removeAllElements();
									while(!parser.getName().equals("transition")) {
										  checkBoxesContainer.addElement(parser.getAttributeValue(0).toString());
										  
										  //do two nextTag()'s, because <task> has a closing tag
										  parser.nextTag();
										  parser.nextTag();		
									}
									parseYesNoAnswers(parser);
								}
								else if (myStringToken.equals("sendToWebServer")) { 								
								  currScreen=SEND_TO_WEBSERVER_SCREEN;
								  parser.nextTag(); //go to the next tag
								  parseYesNoAnswers(parser);
								}
								else if (myStringToken.equals("sendToCloudBasedService")) { 								
								  currScreen=SEND_TO_CLOUD_BASED_SERVICE_SCREEN;
								  parser.nextTag(); //go to the next tag
								  parseYesNoAnswers(parser);
								}
								else if (myStringToken.equals("dcatSummary")) { 								
								  currScreen=DCAT_SUMMARY_SCREEN;
								  parser.nextTag(); //go to the next tag
								  parseYesNoAnswers(parser);
								}									
								else if (myStringToken.equals("qrCodeReader")) { 								
									//<task-node name="qrCodeReader~Scan Patient's QR Code ID?">
									//  <transition to="textField~Family Name:" name="Any"></transition>
									//</task-node>
									currScreen=QR_CODE_READER_SCREEN;
									parser.nextTag(); //go to the next tag
									parseYesNoAnswers(parser);
								}
								else if (myStringToken.equals("textField")) { 
									//<task-node name="textField~For how many days?">
									//  <transition to="Does the child have wheezing? (child must be calm)" name="Any"></transition>
									//</task-node>
									parser.nextTag(); //go to transition tag
									currScreen=TEXTFIELD_SCREEN;
									
									parseYesNoAnswers(parser);
								}
								else if (myStringToken.equals("textFieldWithAnswer")) { 
									//<task-node name="textFieldWithAnswer~Who is the founder of Usbong (nickname)?Answer=Mike">
									//  <transition to="Does the child have wheezing? (child must be calm)" name="Any"></transition>
									//</task-node>
									parser.nextTag(); //go to transition tag
									currScreen=TEXTFIELD_WITH_ANSWER_SCREEN;
									
									parseYesNoAnswers(parser);
								}
								else if (myStringToken.equals("textFieldWithUnit")) { 
									//<task-node name="textFieldWithUnit~Days~For how many days?">
									//  <transition to="Does the child have wheezing? (child must be calm)" name="Any"></transition>
									//</task-node>
									parser.nextTag(); //go to transition tag
									textFieldUnit = st.nextToken();									
									currScreen=TEXTFIELD_WITH_UNIT_SCREEN;
									
									parseYesNoAnswers(parser);
								}																								
								else if (myStringToken.equals("textFieldNumerical")) { 
									//<task-node name="textFieldNumerical~PatientID">
									//  <transition to="Does the child have wheezing? (child must be calm)" name="Any"></transition>
									//</task-node>
									parser.nextTag(); //go to transition tag
									currScreen=TEXTFIELD_NUMERICAL_SCREEN;
									
									parseYesNoAnswers(parser);
								}																								
								else if (myStringToken.equals("textArea")) { 
									//<task-node name="textArea~Comments">
									//  <transition to="Does the child have wheezing? (child must be calm)" name="Any"></transition>
									//</task-node>
									parser.nextTag(); //go to transition tag
									currScreen=TEXTAREA_SCREEN;
									
									parseYesNoAnswers(parser);
								}
								else if (myStringToken.equals("textAreaWithAnswer")) { 
									//<task-node name="textAreaWithAnswer~Who is the founder of Usbong (nickname)?Answer=Mike">
									//  <transition to="Does the child have wheezing? (child must be calm)" name="Any"></transition>
									//</task-node>
									parser.nextTag(); //go to transition tag
									currScreen=TEXTAREA_WITH_ANSWER_SCREEN;
									
									parseYesNoAnswers(parser);
								}
								else if (myStringToken.equals("date")) { //special?
									//<task-node name="date~Birthday">
									//  <transition to="textField~Address" name="Any"></transition>
									//</task-node>
									parser.nextTag(); //go to transition tag
									currScreen=DATE_SCREEN;

									parseYesNoAnswers(parser);
								}
								else if (myStringToken.equals("timestampDisplay")) { //special?
									//<task-node name="timestampDisplay~Timecheck">
									//  <transition to="textDisplay~Comments" name="Any"></transition>
									//</task-node>
									parser.nextTag(); //go to transition tag
									currScreen=TIMESTAMP_DISPLAY_SCREEN;

									parseYesNoAnswers(parser);
								}
								else if ((myStringToken.equals("special")) || (myStringToken.equals("textDisplay"))) { //special?
									//<task-node name="special~Give a trial of rapid acting inhaled bronchodilator for up to 3 times 15-20 minutes apart. Count the breaths and look for chest indrawing again, and then classify.">
									//  <transition to="Does the child have wheezing? (child must be calm)" name="Any"></transition>
									//</task-node>
									parser.nextTag(); //go to transition tag
									currScreen=TEXT_DISPLAY_SCREEN;

									parseYesNoAnswers(parser);
								}
								else if ((myStringToken.equals("specialImage")) || (myStringToken.equals("imageDisplay"))) { //special?
									parser.nextTag(); //go to transition tag
									currScreen=IMAGE_DISPLAY_SCREEN;

									parseYesNoAnswers(parser);
								}
								else if (myStringToken.equals("clickableImageDisplay")) { 
									parser.nextTag(); //go to transition tag
									currScreen=CLICKABLE_IMAGE_DISPLAY_SCREEN;

									parseYesNoAnswers(parser);
								}
								else if (myStringToken.equals("textImageDisplay")) { //special?
									parser.nextTag(); //go to transition tag
									currScreen=TEXT_IMAGE_DISPLAY_SCREEN;

									parseYesNoAnswers(parser);
								}
								else if (myStringToken.equals("textClickableImageDisplay")) { 
									parser.nextTag(); //go to transition tag
									currScreen=TEXT_CLICKABLE_IMAGE_DISPLAY_SCREEN;

									parseYesNoAnswers(parser);
								}
								else if (myStringToken.equals("videoFromFile")) { 
									parser.nextTag(); //go to transition tag
									currScreen=VIDEO_FROM_FILE_SCREEN;

									parseYesNoAnswers(parser);
								}
								else if (myStringToken.equals("gps")) { //special?
									//<task-node name="gps~My Location">
									//  <transition to="Does the child have wheezing? (child must be calm)" name="Any"></transition>
									//</task-node>
									parser.nextTag(); //go to transition tag
									currScreen=GPS_LOCATION_SCREEN;

									parseYesNoAnswers(parser);
								}
								else if (myStringToken.equals("photoCapture")) { 
									
									parser.nextTag(); //go to transition tag
									currScreen=PHOTO_CAPTURE_SCREEN;

									parseYesNoAnswers(parser);
								}
								else if (myStringToken.equals("audioRecord")) { 
									parser.nextTag(); //go to transition tag
									currScreen=AUDIO_RECORD_SCREEN;

									parseYesNoAnswers(parser);
								}
								else if (myStringToken.equals("paint")) { 
									parser.nextTag(); //go to transition tag
									currScreen=PAINT_SCREEN;

									parseYesNoAnswers(parser);
								}
							}
					  }/*
					  else { //this is a currIMCICaseList number
						usbongNodeContainerCounter++;
						currUsbongNode=(String)usbongNodeContainer.elementAt(usbongNodeContainerCounter);
						continue;
					  }*/
					  break;
				  }
				  //TODO dosage guide/table
				  //It would be great if someone adds the above TODO as well.
			  }
		  }
		}
		catch(Exception e) {
			e.printStackTrace();
		}
/*		
		if ((!usedBackButton) && (!hasReturnedFromAnotherActivity)){
			usbongNodeContainer.addElement(currUsbongNode);
			usbongNodeContainerCounter++;
		}
		else {
			usedBackButton=false;
			hasReturnedFromAnotherActivity=false;
		}
*/		
		initUsbongScreen();
	}

	public void initUsbongScreen() {		
        //Reference: http://www.anddev.org/tinytut_-_get_resources_by_name__getidentifier_-t460.html; last accessed 14 Sept 2011
        Resources myRes = getResources();
        Drawable myDrawableImage;
        
        //added by Mike, Feb. 13, 2013
        isAnOptionalNode = UsbongUtils.isAnOptionalNode(currUsbongNode);

        String myStringToken="";
//		if (usedBackButton) {
        
//        System.out.println(">>>>>> currentAnswer: "+currentAnswer);
        
    		StringTokenizer st = new StringTokenizer(currentAnswer, ",");
    		if ((st != null) && (st.hasMoreTokens())) {
	    		myStringToken = st.nextToken();
	    		currentAnswer = currentAnswer.replace(myStringToken+",", "");
    		}	    		
	    		
    		StringTokenizer st_two = new StringTokenizer(currentAnswer, ";");
	    		
	    	if (st_two!=null) {
    			if (currentAnswer.length()>1) {
    				myStringToken = st_two.nextToken(); //get next element (i.e. 1 in "Y,1;")	    			
    			}
    			else {
    				myStringToken="";
    			}	    			
    		}
/*
	    		while (st.hasMoreTokens()) { //get last element (i.e. 1 in "Y,1;")
	    			myStringToken = st.nextToken(); 
	    		}
*/	    		
//	    		myStringToken = myStringToken.replace(";", "");

//		}

		switch(currScreen) {
	    	case MULTIPLE_RADIO_BUTTONS_SCREEN:
		    	setContentView(R.layout.multiple_radio_buttons_screen);
		        initBackNextButtons();
		        TextView myMultipleRadioButtonsScreenTextView = (TextView)findViewById(R.id.radio_buttons_textview);
		        myMultipleRadioButtonsScreenTextView = (TextView) UsbongUtils.applyTagsInView(myMultipleRadioButtonsScreenTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);

		        RadioGroup radioGroup = (RadioGroup)findViewById(R.id.multiple_radio_buttons_radiogroup);
		        int totalRadioButtonsInContainer = radioButtonsContainer.size();
		        for (int i=0; i<totalRadioButtonsInContainer; i++) {
		            View radioButtonView = new RadioButton(getBaseContext());
		            RadioButton radioButton = (RadioButton) UsbongUtils.applyTagsInView(radioButtonView, UsbongUtils.IS_RADIOBUTTON, radioButtonsContainer.elementAt(i).toString());
		            radioButton.setTextSize(20);
		            radioButton.setId(i);
		            radioButton.setTextColor(Color.parseColor("#4a452a"));			        

		            int myStringTokenInt;
		            try {
		            	myStringTokenInt = Integer.parseInt(myStringToken);
		            }		            
		            catch (NumberFormatException e) {//if myStringToken is not an int;
		            	myStringTokenInt=-1;
	            	}

		            
		            if ((!myStringToken.equals("")) && (i == myStringTokenInt)) {
		            	radioButton.setChecked(true);
		            }
		            else {
			            radioButton.setChecked(false);
		            }
		            
		            radioGroup.addView(radioButton);
		        }		     		        
				break;
	    	case MULTIPLE_RADIO_BUTTONS_WITH_ANSWER_SCREEN:
		    	setContentView(R.layout.multiple_radio_buttons_screen);
		        initBackNextButtons();
		       
    			String myMultipleRadioButtonsWithAnswerScreenStringToken = "";
//    			Log.d(">>>>>>>>currUsbongNode", currUsbongNode);
	    		currUsbongNodeWithoutAnswer=currUsbongNode.replace("Answer=", "~");

	    		StringTokenizer myMultipleRadioButtonsWithAnswerScreenStringTokenizer = new StringTokenizer(currUsbongNodeWithoutAnswer, "~");
	    		
	    		if (myMultipleRadioButtonsWithAnswerScreenStringTokenizer != null) {
	    			myMultipleRadioButtonsWithAnswerScreenStringToken = myMultipleRadioButtonsWithAnswerScreenStringTokenizer.nextToken();
	    			
		    		while (myMultipleRadioButtonsWithAnswerScreenStringTokenizer.hasMoreTokens()) { //get last element (i.e. 0 in "radioButtonsWithAnswer~You see your teacher approaching you. What do you do?Answer=0")
		    			myMultipleRadioButtonsWithAnswerScreenStringToken = myMultipleRadioButtonsWithAnswerScreenStringTokenizer.nextToken(); 
		    		}
	    		}
	    		myMultipleRadioButtonsWithAnswerScreenAnswer=myMultipleRadioButtonsWithAnswerScreenStringToken.toString();
//    			Log.d(">>>>>>>>myMultipleRadioButtonsWithAnswerScreenAnswer", myMultipleRadioButtonsWithAnswerScreenAnswer);

    			currUsbongNodeWithoutAnswer=currUsbongNodeWithoutAnswer.substring(0, currUsbongNodeWithoutAnswer.length()-myMultipleRadioButtonsWithAnswerScreenAnswer.length()-1); //do a -1 for the last tilde    			
//    			Log.d(">>>>>>>>currUsbongNodeWithoutAnswer", currUsbongNodeWithoutAnswer);
    			
		        TextView myMultipleRadioButtonsWithAnswerScreenTextView = (TextView)findViewById(R.id.radio_buttons_textview);
		        myMultipleRadioButtonsWithAnswerScreenTextView = (TextView) UsbongUtils.applyTagsInView(myMultipleRadioButtonsWithAnswerScreenTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNodeWithoutAnswer);

		        RadioGroup myMultipleRadioButtonsWithAnswerRadioGroup = (RadioGroup)findViewById(R.id.multiple_radio_buttons_radiogroup);
		        int myMultipleRadioButtonsWithAnswerTotalRadioButtonsInContainer = radioButtonsContainer.size();
		        for (int i=0; i<myMultipleRadioButtonsWithAnswerTotalRadioButtonsInContainer; i++) {
		            View radioButtonView = new RadioButton(getBaseContext());
		            RadioButton radioButton = (RadioButton) UsbongUtils.applyTagsInView(radioButtonView, UsbongUtils.IS_RADIOBUTTON, radioButtonsContainer.elementAt(i).toString());
		            radioButton.setTextSize(20);
		            radioButton.setId(i);
		            radioButton.setTextColor(Color.parseColor("#4a452a"));			        

		            if ((!myStringToken.equals("")) && (i == Integer.parseInt(myStringToken))) {
		            	radioButton.setChecked(true);
		            }
		            else {
			            radioButton.setChecked(false);
		            }
		            
		            myMultipleRadioButtonsWithAnswerRadioGroup.addView(radioButton);
		        }		     		        
				break;

	    	case LINK_SCREEN:
	    		//use same contentView as multiple_radio_buttons_screen
		    	setContentView(R.layout.multiple_radio_buttons_screen);
		        initBackNextButtons();
		        TextView myLinkScreenTextView = (TextView)findViewById(R.id.radio_buttons_textview);
		        myLinkScreenTextView = (TextView) UsbongUtils.applyTagsInView(myLinkScreenTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);
		        
		        RadioGroup myLinkScreenRadioGroup = (RadioGroup)findViewById(R.id.multiple_radio_buttons_radiogroup);
		        int myLinkScreenTotalRadioButtonsInContainer = radioButtonsContainer.size();
		        for (int i=0; i<myLinkScreenTotalRadioButtonsInContainer; i++) {
		            View radioButtonView = new RadioButton(getBaseContext());
		            RadioButton radioButton = (RadioButton) UsbongUtils.applyTagsInView(radioButtonView, UsbongUtils.IS_RADIOBUTTON, UsbongUtils.trimUsbongNodeName(radioButtonsContainer.elementAt(i).toString()));

					Log.d(">>>>>radioButton",radioButton.getText().toString());

//		            radioButton.setChecked(false);
		            radioButton.setTextSize(20);
		            radioButton.setId(i);
		            radioButton.setTextColor(Color.parseColor("#4a452a"));			        

		            if ((!myStringToken.equals("")) && (i == Integer.parseInt(myStringToken))) {
		            	radioButton.setChecked(true);
		            }
		            else {
			            radioButton.setChecked(false);
		            }
		            
		            myLinkScreenRadioGroup.addView(radioButton);
		        }		     		        
				break;
	    	case MULTIPLE_CHECKBOXES_SCREEN:
		    	setContentView(R.layout.multiple_checkboxes_screen);
		        initBackNextButtons();
		        TextView myMultipleCheckBoxesScreenTextView = (TextView)findViewById(R.id.checkboxes_textview);
		        myMultipleCheckBoxesScreenTextView = (TextView) UsbongUtils.applyTagsInView(myMultipleCheckBoxesScreenTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);

		        LinearLayout myMultipleCheckboxesLinearLayout = (LinearLayout)findViewById(R.id.multiple_checkboxes_linearlayout);
		        int totalCheckBoxesInContainer = checkBoxesContainer.size();
		        		        
	    		StringTokenizer myMultipleCheckboxStringTokenizer = new StringTokenizer(myStringToken, ",");
	    		Vector<String> myCheckedAnswers = new Vector<String>();	    		
//	    		int counter=0;	    		
	    		
	    		while (myMultipleCheckboxStringTokenizer.countTokens()>0) {
	    			String myMultipleCheckboxStringToken = myMultipleCheckboxStringTokenizer.nextToken();
	    			if (myMultipleCheckboxStringToken != null) {
		    			myCheckedAnswers.add(myMultipleCheckboxStringToken);	    				
	    			}
	    			else {
	    				break;
	    			}
//	    			counter++;
	    		}
	    		
		        for (int i=0; i<totalCheckBoxesInContainer; i++) {
		            CheckBox checkBox = new CheckBox(getBaseContext());
//		            checkBox.setText(StringEscapeUtils.unescapeJava(checkBoxesContainer.elementAt(i).toString()));
		            checkBox = (CheckBox) UsbongUtils.applyTagsInView(checkBox, UsbongUtils.IS_CHECKBOX, StringEscapeUtils.unescapeJava(checkBoxesContainer.elementAt(i).toString()));
			            
		            for (int k=0; k<myCheckedAnswers.size(); k++) {
		            	try {
			            	if (i==Integer.parseInt(myCheckedAnswers.elementAt(k))) {
			            		checkBox.setChecked(true);
			            	}
		            	}
		            	catch (NumberFormatException e) {//if myCheckedAnswers.elementAt(k) is not an int;
		            		continue;
		            	}
		            }
		            
		            checkBox.setTextSize(20);
			        checkBox.setTextColor(Color.parseColor("#4a452a"));			        
		            myMultipleCheckboxesLinearLayout.addView(checkBox);
		        }		     		        
		        break;
	    	case AUDIO_RECORD_SCREEN:
		        setContentView(R.layout.audio_recorder_screen);
		        initRecordAudioScreen();
		        initBackNextButtons();

		        TextView myAudioRecorderTextView = (TextView)findViewById(R.id.audio_recorder_textview);
		        myAudioRecorderTextView = (TextView) UsbongUtils.applyTagsInView(myAudioRecorderTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);

		        Button recordButton = (Button)findViewById(R.id.record_button);
		        Button stopButton = (Button)findViewById(R.id.stop_button);
		        Button playButton = (Button)findViewById(R.id.play_button);

		        if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_FILIPINO) {
		    		recordButton.setText((String) getResources().getText(R.string.UsbongRecordTextViewFILIPINO));				    		
		    		stopButton.setText((String) getResources().getText(R.string.UsbongStopTextViewFILIPINO));				    		
		    		playButton.setText((String) getResources().getText(R.string.UsbongPlayTextViewFILIPINO));				    		
		        }
		    	else if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_JAPANESE) {
		    		recordButton.setText((String) getResources().getText(R.string.UsbongRecordTextViewJAPANESE));				    		
		    		stopButton.setText((String) getResources().getText(R.string.UsbongStopTextViewJAPANESE));				    		
		    		playButton.setText((String) getResources().getText(R.string.UsbongPlayTextViewJAPANESE));				    		
		    	}
		    	else { //if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_ENGLISH) {
		    		recordButton.setText((String) getResources().getText(R.string.UsbongRecordTextViewENGLISH));				    		
		    		stopButton.setText((String) getResources().getText(R.string.UsbongStopTextViewENGLISH));				    		
		    		playButton.setText((String) getResources().getText(R.string.UsbongPlayTextViewENGLISH));				    		
		    	}
		        break;
	        case PHOTO_CAPTURE_SCREEN:
		    	setContentView(R.layout.photo_capture_screen);
		    	if (!performedCapturePhoto) {
 		    	  initTakePhotoScreen();
		    	}
		    	initBackNextButtons();

		        TextView myPhotoCaptureScreenTextView = (TextView)findViewById(R.id.photo_capture_textview);
		        myPhotoCaptureScreenTextView = (TextView) UsbongUtils.applyTagsInView(myPhotoCaptureScreenTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);
	    		Button photoCaptureButton = (Button)findViewById(R.id.photo_capture_button);

		        if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_FILIPINO) {
		        	photoCaptureButton.setText((String) getResources().getText(R.string.UsbongTakePhotoTextViewFILIPINO));				    		
		        }
		    	else if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_JAPANESE) {
		    		photoCaptureButton.setText((String) getResources().getText(R.string.UsbongTakePhotoTextViewJAPANESE));				    		
		    	}
		    	else { //if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_ENGLISH) {
		    		photoCaptureButton.setText((String) getResources().getText(R.string.UsbongTakePhotoTextViewENGLISH));				    		
		    	}
		        /*
		        if (UsbongUtils.USE_UNESCAPE) {
		        	myPhotoCaptureScreenTextView.setText(StringEscapeUtils.unescapeJava(UsbongUtils.trimUsbongNodeName(currUsbongNode)));
		        }
		        else {
		        	myPhotoCaptureScreenTextView.setText(UsbongUtils.trimUsbongNodeName(currUsbongNode));		        	
		        }		    	
*/		        
		    	break;		    	
	        case PAINT_SCREEN:
		    	setContentView(R.layout.paint_screen);
		    	if (!performedRunPaint) {
 		    	  initPaintScreen();
		    	}
		    	initBackNextButtons();

		        TextView myPaintScreenTextView = (TextView)findViewById(R.id.paint_textview);
		        myPaintScreenTextView = (TextView) UsbongUtils.applyTagsInView(myPaintScreenTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);

	    		Button paintButton = (Button)findViewById(R.id.paint_button);

		        if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_FILIPINO) {
		        	paintButton.setText((String) getResources().getText(R.string.UsbongRunPaintTextViewFILIPINO));				    		
		        }
		    	else if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_JAPANESE) {
		    		paintButton.setText((String) getResources().getText(R.string.UsbongRunPaintTextViewJAPANESE));				    		
		    	}
		    	else { //if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_ENGLISH) {
		    		paintButton.setText((String) getResources().getText(R.string.UsbongRunPaintTextViewENGLISH));				    		
		    	}
		    	break;		    	
	        case QR_CODE_READER_SCREEN:
		    	setContentView(R.layout.qr_code_reader_screen);

		    	if (!performedGetQRCode) {
 		    	  initQRCodeReaderScreen();
		    	}
		    	
		    	initBackNextButtons();

		        TextView myQRCodeReaderScreenTextView = (TextView)findViewById(R.id.qr_code_reader_textview);
		        myQRCodeReaderScreenTextView = (TextView) UsbongUtils.applyTagsInView(myQRCodeReaderScreenTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);

	    		Button qrCodeReaderButton = (Button)findViewById(R.id.qr_code_reader_button);

		        if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_FILIPINO) {
		        	qrCodeReaderButton .setText((String) getResources().getText(R.string.UsbongQRCodeReaderTextViewFILIPINO));				    		
		        }
		    	else if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_JAPANESE) {
		    		qrCodeReaderButton .setText((String) getResources().getText(R.string.UsbongQRCodeReaderTextViewJAPANESE));				    		
		    	}
		    	else { //if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_ENGLISH) {
		    		qrCodeReaderButton .setText((String) getResources().getText(R.string.UsbongQRCodeReaderTextViewENGLISH));				    		
		    	}		    	
		    	break;		    	
			case TEXTFIELD_SCREEN:
		    	setContentView(R.layout.textfield_screen);
		        initBackNextButtons();

		        TextView myTextFieldScreenTextView = (TextView)findViewById(R.id.textfield_textview);
		        myTextFieldScreenTextView = (TextView) UsbongUtils.applyTagsInView(myTextFieldScreenTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);

		        EditText myTextFieldScreenEditText = (EditText)findViewById(R.id.textfield_edittext);
		        myTextFieldScreenEditText.setText(myStringToken);
		        
		        break;    	
			case TEXTFIELD_WITH_ANSWER_SCREEN:
		    	setContentView(R.layout.textfield_screen);
		        initBackNextButtons();
		        
    			String myTextFieldWithAnswerScreenStringToken = "";
//    			Log.d(">>>>>>>>currUsbongNode", currUsbongNode);
	    		currUsbongNodeWithoutAnswer=currUsbongNode.replace("Answer=", "~");

	    		StringTokenizer myTextFieldWithAnswerScreenStringTokenizer = new StringTokenizer(currUsbongNodeWithoutAnswer, "~");
	    		
	    		if (myTextFieldWithAnswerScreenStringTokenizer != null) {
	    			myTextFieldWithAnswerScreenStringToken = myTextFieldWithAnswerScreenStringTokenizer.nextToken();
	    			
		    		while (myTextFieldWithAnswerScreenStringTokenizer.hasMoreTokens()) { //get last element (i.e. Mike in "textFieldWithAnswer~Who is the founder of Usbong (nickname)?Answer=Mike")
		    			myTextFieldWithAnswerScreenStringToken = myTextFieldWithAnswerScreenStringTokenizer.nextToken(); 
		    		}
	    		}
	    		myTextFieldWithAnswerScreenAnswer=myTextFieldWithAnswerScreenStringToken.toString();
    			currUsbongNodeWithoutAnswer=currUsbongNodeWithoutAnswer.substring(0, currUsbongNodeWithoutAnswer.length()-myTextFieldWithAnswerScreenAnswer.length()-1); //do a -1 for the last tilde    			
		        
		        TextView myTextFieldWithAnswerScreenTextView = (TextView)findViewById(R.id.textfield_textview);
		        myTextFieldWithAnswerScreenTextView = (TextView) UsbongUtils.applyTagsInView(myTextFieldWithAnswerScreenTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNodeWithoutAnswer);

		        EditText myTextFieldScreenWithAnswerEditText = (EditText)findViewById(R.id.textfield_edittext);
		        myTextFieldScreenWithAnswerEditText.setText(myStringToken);		        
		        break;    				
			case TEXTAREA_SCREEN:
		    	setContentView(R.layout.textarea_screen);
		        initBackNextButtons();

		        TextView myTextAreaScreenTextView = (TextView)findViewById(R.id.textarea_textview);
		        myTextAreaScreenTextView = (TextView) UsbongUtils.applyTagsInView(myTextAreaScreenTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);

		        EditText myTextAreaScreenEditText = (EditText)findViewById(R.id.textarea_edittext);
		        myTextAreaScreenEditText.setText(myStringToken);
		        break;    	
			case TEXTAREA_WITH_ANSWER_SCREEN:
		    	setContentView(R.layout.textarea_screen);
		        initBackNextButtons();
		        
    			String myTextAreaWithAnswerScreenStringToken = "";
//    			Log.d(">>>>>>>>currUsbongNode", currUsbongNode);
	    		currUsbongNodeWithoutAnswer=currUsbongNode.replace("Answer=", "~");

	    		StringTokenizer myTextAreaWithAnswerScreenStringTokenizer = new StringTokenizer(currUsbongNodeWithoutAnswer, "~");
	    		
	    		if (myTextAreaWithAnswerScreenStringTokenizer != null) {
	    			myTextAreaWithAnswerScreenStringToken = myTextAreaWithAnswerScreenStringTokenizer.nextToken();
	    			
		    		while (myTextAreaWithAnswerScreenStringTokenizer.hasMoreTokens()) { //get last element (i.e. Mike in "textFieldWithAnswer~Who is the founder of Usbong (nickname)?Answer=Mike")
		    			myTextAreaWithAnswerScreenStringToken = myTextAreaWithAnswerScreenStringTokenizer.nextToken(); 
		    		}
	    		}
	    		myTextAreaWithAnswerScreenAnswer=myTextAreaWithAnswerScreenStringToken.toString();
    			currUsbongNodeWithoutAnswer=currUsbongNodeWithoutAnswer.substring(0, currUsbongNodeWithoutAnswer.length()-myTextAreaWithAnswerScreenAnswer.length()-1); //do a -1 for the last tilde    			
		        
		        TextView myTextAreaWithAnswerScreenTextView = (TextView)findViewById(R.id.textarea_textview);
		        myTextAreaWithAnswerScreenTextView = (TextView) UsbongUtils.applyTagsInView(myTextAreaWithAnswerScreenTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNodeWithoutAnswer);

		        EditText myTextAreaScreenWithAnswerEditText = (EditText)findViewById(R.id.textarea_edittext);
		        myTextAreaScreenWithAnswerEditText.setText(myStringToken);		        
		        break;    				
			case TEXTFIELD_WITH_UNIT_SCREEN:
		    	setContentView(R.layout.textfield_with_unit_screen);
		        initBackNextButtons();
		        
		        TextView myTextFieldWithUnitScreenTextView = (TextView)findViewById(R.id.textfield_textview);
		        myTextFieldWithUnitScreenTextView = (TextView) UsbongUtils.applyTagsInView(myTextFieldWithUnitScreenTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);
		        EditText myEditText = (EditText)findViewById(R.id.textfield_edittext);
		        myEditText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		        myEditText.setText(myStringToken);
		        
		        TextView myUnitScreenTextView = (TextView)findViewById(R.id.textfieldunit_textview);
		        myUnitScreenTextView.setText(textFieldUnit);		        		        
		        break;    	
			case TEXTFIELD_NUMERICAL_SCREEN:
		    	setContentView(R.layout.textfield_screen);
		        initBackNextButtons();

		        TextView myTextFieldNumericalScreenTextView = (TextView)findViewById(R.id.textfield_textview);
		        myTextFieldNumericalScreenTextView = (TextView) UsbongUtils.applyTagsInView(myTextFieldNumericalScreenTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);
		        EditText myTextFieldNumericalScreenEditText = (EditText)findViewById(R.id.textfield_edittext);
		        myTextFieldNumericalScreenEditText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
		        myTextFieldNumericalScreenEditText.setText(myStringToken);		        
		        break;    	
			case CLASSIFICATION_SCREEN:
		    	setContentView(R.layout.classification_screen);
		        initBackNextButtons();
		        TextView myClassificationScreenTextView = (TextView)findViewById(R.id.classification_textview);
		        myClassificationScreenTextView = (TextView) UsbongUtils.applyTagsInView(myClassificationScreenTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);	            
		        
		        LinearLayout myClassificationLinearLayout = (LinearLayout)findViewById(R.id.classification_linearlayout);
		        int totalClassificationsInContainer = classificationContainer.size();
		        for (int i=0; i<totalClassificationsInContainer; i++) {
		            TextView myTextView = new TextView(getBaseContext());
		            //consider removing this code below; not needed; Mike, May 23, 2013
		            myTextView = (TextView) UsbongUtils.applyTagsInView(myTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);

		        	int bulletCount = i+1;
		            if (UsbongUtils.USE_UNESCAPE) {
			        	myTextView.setText(bulletCount+") "+StringEscapeUtils.unescapeJava(classificationContainer.elementAt(i).toString()));
			        }
			        else {
			        	myTextView.setText(bulletCount+") "+UsbongUtils.trimUsbongNodeName(classificationContainer.elementAt(i).toString()));		        	
			        }
		            
		            //add 5 so that the text does not touch the left border
		            myTextView.setPadding(padding_in_px, 0, 0, 0);
			        myTextView.setTextSize(24);
//			        myTextView.setTextColor(Color.WHITE);
			        myTextView.setTextColor(Color.parseColor("#4a452a"));			        
			        myClassificationLinearLayout.addView(myTextView);
		        }		     		        
		        break;    	
		    //fix this
			case DCAT_SUMMARY_SCREEN:
		    	setContentView(R.layout.dcat_summary_screen);
		        initBackNextButtons();
		        TextView myDCATSummaryScreenTextView = (TextView)findViewById(R.id.dcat_summary_textview);
		        myDCATSummaryScreenTextView = (TextView) UsbongUtils.applyTagsInView(myDCATSummaryScreenTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);
		        
		        String weightsString = "1.9;2.1;2.6;1.8;2.4;1.8;.7;1.0;1.6;2.6;6.9;5.7;3.3;2.2;3.3;3.3;2;2;1.7;1.9;3.9;1.3;2.5;.8";
				StringTokenizer myWeightsStringTokenizer = new StringTokenizer(weightsString, ";");
				String myWeightString = myWeightsStringTokenizer.nextToken();
				/*
				while (st.hasMoreTokens()) {
					myStringToken = st.nextToken(); 
				}
*/
				double myWeightedScoreInt=0;
				double myNegotiatedWeightedScoreInt=0;
				
				double[][] dcatSum = new double[8][4];
				
				final int sumWeightedRatingIndex=0;
				final int sumWeightedScoreIndex=1;
				final int sumNegotiatedRatingIndex=2;
				final int sumNegotiatedScoreIndex=3;
				
				int currStandard=0;//standard 1
//				boolean hasReachedNegotiated=false;
				boolean hasReachedStandardTotal=false;
		        		
		        LinearLayout myDCATSummaryLinearLayout = (LinearLayout)findViewById(R.id.dcat_summary_linearlayout);
		        int totalElementsInDCATSummaryBasedOnUsbongNodeContainer = usbongNodeContainer.size();
		        
//		        for (int i=0; i<totalElementsInDCATSummaryBasedOnUsbongNodeContainer; i++) {		        	
		        for (int i=0; i<totalElementsInDCATSummaryBasedOnUsbongNodeContainer; i++) {		        	

		        	TextView myTextView = new TextView(getBaseContext());	            	
		            myTextView.setPadding(padding_in_px, 0, 0, 0); //add 5 so that the text does not touch the left border
			        myTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,24);
			        myTextView.setTextColor(Color.parseColor("#4a452a"));			        

			        //the only way to check if the element is already the last item in the standard
			        //is if the next element in the node container has "STANDARD", but not the first standard
	        		if ((i+1>=totalElementsInDCATSummaryBasedOnUsbongNodeContainer) || 
	        		   (i+1<totalElementsInDCATSummaryBasedOnUsbongNodeContainer) &&
	        				((usbongNodeContainer.elementAt(i+1).toString().contains("STANDARD")))&&
	        				(!(usbongNodeContainer.elementAt(i+1).toString().contains("STANDARD ONE")))
	        		   )
	        		{	
	        			int tempCurrStandard=currStandard+1; //do a +1 since currStandard begins at 0

			            TextView myIssuesTextView = new TextView(getBaseContext());			            

	        			//added by Mike, May 31, 2013
	        			if (!usbongAnswerContainer.elementAt(i).toString().equals("dcat_end;")) {
	
			        		String s = usbongAnswerContainer.elementAt(i).toString().replace(";", "");
			        		s = s.replace("A,", "");
			        		if (!s.equals("")) {
			        			myIssuesTextView  = (TextView) UsbongUtils.applyTagsInView(myIssuesTextView, UsbongUtils.IS_TEXTVIEW, "ISSUES: "+s+"{br}");
			        		}
			        		else {
			        			myIssuesTextView  = (TextView) UsbongUtils.applyTagsInView(myIssuesTextView, UsbongUtils.IS_TEXTVIEW, "ISSUES: none{br}");
			        		}

				            myIssuesTextView.setPadding(padding_in_px, 0, 0, 0); //add 5 so that the text does not touch the left border
				            myIssuesTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,24);
				            myIssuesTextView.setTextColor(Color.parseColor("#4a452a"));			        
				            myDCATSummaryLinearLayout.addView(myIssuesTextView);
	        			}
	        			
		        		if (myWeightsStringTokenizer.hasMoreElements()) {
			        		//get the next weight
		        			myWeightString = myWeightsStringTokenizer.nextToken();
		        		}

			            myTextView = (TextView) UsbongUtils.applyTagsInView(myTextView, UsbongUtils.IS_TEXTVIEW, 
				        		"//--------------------"+
		        				" STANDARD "+tempCurrStandard+" (TOTAL){br}"+ 
		        				"Total (Rating): "+String.format("%.2f",dcatSum[currStandard][sumWeightedRatingIndex]) +"{br}"+
				        		"Total (Weighted Score): "+String.format("%.2f",dcatSum[currStandard][sumWeightedScoreIndex])+"{br}"+
		        				"Total (Negotiated Rating): "+String.format("%.2f",dcatSum[currStandard][sumNegotiatedRatingIndex])+"{br}"+
				        		"Total (Negotiated WS): "+String.format("%.2f",dcatSum[currStandard][sumNegotiatedScoreIndex])+"{br}"+
				        		"//--------------------"
		        				);		
			            hasReachedStandardTotal=true;
		        		currStandard++;
	        		}

	        		if (hasReachedStandardTotal) {
	        			hasReachedStandardTotal=false;
		        	}		        	
	        		else if (usbongNodeContainer.elementAt(i).toString().contains("ISSUES")){
		        		String s = usbongAnswerContainer.elementAt(i).toString().replace(";", "");
		        		s = s.replace("A,", "");
		        		if (!s.equals("")) {
			        		myTextView = (TextView) UsbongUtils.applyTagsInView(myTextView, UsbongUtils.IS_TEXTVIEW, "ISSUES: "+s+"{br}");
		        		}
		        		else {
			        		myTextView = (TextView) UsbongUtils.applyTagsInView(myTextView, UsbongUtils.IS_TEXTVIEW, "ISSUES: none{br}");
		        		}
	        			
		        		if (myWeightsStringTokenizer.hasMoreElements()) {
			        		//get the next weight
		        			myWeightString = myWeightsStringTokenizer.nextToken();
		        		}
		        	}
		        	else if (usbongNodeContainer.elementAt(i).toString().contains("Weighted")){
			            TextView myWeightedTextView = new TextView(getBaseContext());
			            myWeightedTextView = (TextView) UsbongUtils.applyTagsInView(myWeightedTextView, UsbongUtils.IS_TEXTVIEW, usbongNodeContainer.elementAt(i).toString().replace("{br}(Weighted Score)",""));
			            myWeightedTextView.setPadding(padding_in_px, 0, 0, 0); //add 5 so that the text does not touch the left border
			            myWeightedTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,24);
			            myWeightedTextView.setTextColor(Color.parseColor("#4a452a"));			        
			            
			            myDCATSummaryLinearLayout.addView(myWeightedTextView);

		        		int weightedAnswer = Integer.parseInt(usbongAnswerContainer.elementAt(i).toString().replace(";", ""));
		        		if (weightedAnswer<=0) {
		        			weightedAnswer=0;
		        		}

		        		//the weight is in double
		        		myWeightedScoreInt = weightedAnswer * Double.parseDouble(myWeightString);
		        		if (myWeightedScoreInt<=0) {
		        			myWeightedScoreInt=0;
		        			myTextView.setBackgroundColor(Color.YELLOW);
		        		}
		        		
		        		dcatSum[currStandard][sumWeightedRatingIndex]+=weightedAnswer;
		        		dcatSum[currStandard][sumWeightedScoreIndex]+=myWeightedScoreInt;		        		
		        		
		        		myTextView = (TextView) UsbongUtils.applyTagsInView(myTextView, UsbongUtils.IS_TEXTVIEW, 
		        				"Weighted: " +myWeightedScoreInt);
		        	}
		        	else if (usbongNodeContainer.elementAt(i).toString().contains("Negotiated")){
		        		int negotiatedAnswer = Integer.parseInt(usbongAnswerContainer.elementAt(i).toString().replace(";", ""));
		        		if (negotiatedAnswer<=0) {
		        			negotiatedAnswer=0;
		        		}
		        		
		        		//the weight is in double
		        		myNegotiatedWeightedScoreInt =  negotiatedAnswer * Double.parseDouble(myWeightString);
		        		if (myNegotiatedWeightedScoreInt<=0) {
		        			myNegotiatedWeightedScoreInt=0;
		        			myTextView.setBackgroundColor(Color.YELLOW);
		        		}

		        		dcatSum[currStandard][sumNegotiatedRatingIndex]+=negotiatedAnswer;
		        		dcatSum[currStandard][sumNegotiatedScoreIndex]+=myNegotiatedWeightedScoreInt;		        		

		        		myTextView = (TextView) UsbongUtils.applyTagsInView(myTextView, UsbongUtils.IS_TEXTVIEW, 
		        				"Negotiated: " + myNegotiatedWeightedScoreInt);		        				        				        		
//		        		hasReachedNegotiated=true;
		        	}
		        	else {
			            myTextView = (TextView) UsbongUtils.applyTagsInView(myTextView, UsbongUtils.IS_TEXTVIEW, usbongNodeContainer.elementAt(i).toString()+"{br}");
		        	}

//	        		if (!hasReachedStandardTotal) {
	        			myDCATSummaryLinearLayout.addView(myTextView);
//	        		}
//	        		else {
//	        			hasReachedStandardTotal=false;
//	        		}
		        }		     		        
		        break;    	
			case DATE_SCREEN:
		    	setContentView(R.layout.date_screen);
		        initBackNextButtons();
		        
		        TextView myDateScreenTextView = (TextView)findViewById(R.id.date_textview);
		        myDateScreenTextView = (TextView) UsbongUtils.applyTagsInView(myDateScreenTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);

		        //Reference: http://code.google.com/p/android/issues/detail?id=2037
		        //last accessed: 21 Aug. 2012
		        Configuration userConfig = new Configuration();
		        Settings.System.getConfiguration( getContentResolver(), userConfig );
		        Calendar date = Calendar.getInstance( userConfig.locale);
		        
		        //Reference: http://www.androidpeople.com/android-spinner-default-value;
		        //last accessed: 21 Aug. 2012		        
		        //month-------------------------------
		        int month = date.get(Calendar.MONTH); //first month of the year is 0
		    	Spinner dateMonthSpinner = (Spinner) findViewById(R.id.date_month_spinner);
		        monthAdapter = ArrayAdapter.createFromResource(
		                this, R.array.months_array, android.R.layout.simple_spinner_item);
/*		        monthAdapter = ArrayAdapter.createFromResource(
                this, R.array.months_array, R.layout.date_textview);
*/                
		    	monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		        dateMonthSpinner.setAdapter(monthAdapter);
		        dateMonthSpinner.setSelection(month);
//		        System.out.println(">>>>>>>>>>>>>> month"+month);
//		        Log.d(">>>>>>myStringToken",myStringToken);
	        	
		        for (int i=0; i<monthAdapter.getCount(); i++) {
//		        	Log.d(">>>>>>monthAdapter",monthAdapter.getItem(i).toString());
		        	
		        	if (myStringToken.contains(monthAdapter.getItem(i).toString())) {
		        		dateMonthSpinner.setSelection(i);
		        		
		        		//added by Mike, March 4, 2013
		        		myStringToken = myStringToken.replace(monthAdapter.getItem(i).toString(), "");
		        	}
		        }		        		        
		        //-------------------------------------
		        
		        //day----------------------------------
		        //Reference: http://docs.oracle.com/javase/1.5.0/docs/api/java/util/Calendar.html#MONTH
		        //last accessed: 21 Aug 2012
				int day = date.get(Calendar.DAY_OF_MONTH); //first day of the month is 1
				day = day - 1; //do this to offset, when retrieving the day in strings.xml
		        Spinner dateDaySpinner = (Spinner) findViewById(R.id.date_day_spinner);
		        dayAdapter = ArrayAdapter.createFromResource(
		                this, R.array.day_array, android.R.layout.simple_spinner_item);
		        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		        dateDaySpinner.setAdapter(dayAdapter);		
		        dateDaySpinner.setSelection(day);
//		        System.out.println(">>>>>>>>>>>>>> day"+day);

//		        Log.d(">>>>>myStringToken",myStringToken);
//		        System.out.println(">>>>>>>> myStringToken"+myStringToken);
		        StringTokenizer myDateStringTokenizer = new StringTokenizer(myStringToken, ",");
		        String myDayStringToken="";
				if (!myStringToken.equals("")) {
					myDayStringToken = myDateStringTokenizer.nextToken();	
				}

				for (int i=0; i<dayAdapter.getCount(); i++) {		        	
		        	if (myDayStringToken.contains(dayAdapter.getItem(i).toString())) {
		        		dateDaySpinner.setSelection(i);
		        		
		        		myStringToken = myStringToken.replace(dayAdapter.getItem(i).toString()+",", "");
//		        		System.out.println(">>>>>>>>>>>myStringToken: "+myStringToken);
		        	}
		        }
		        //-------------------------------------				
				
		        //year---------------------------------
				int year = date.get(Calendar.YEAR);
		        EditText myDateYearEditText = (EditText)findViewById(R.id.date_edittext);
		        myDateYearEditText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);		        
		        //added by Mike, March 4, 2013
		        if (myStringToken.equals("")) {
		        	myDateYearEditText.setText(""+year);		        
		        }
		        else {
		        	myDateYearEditText.setText(myStringToken);
		        }		        
		        //-------------------------------------		        
		        break;    	
		        
			case TEXT_DISPLAY_SCREEN:
		    	setContentView(R.layout.text_display_screen);
		        initBackNextButtons();

		        TextView mySpecialScreenTextView = (TextView)findViewById(R.id.special_textview);
		        mySpecialScreenTextView = (TextView) UsbongUtils.applyTagsInView(mySpecialScreenTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);
		        break;    	
			case TIMESTAMP_DISPLAY_SCREEN:
		    	setContentView(R.layout.timestamp_display_screen);
		        initBackNextButtons();

		        TextView myTimeDisplayScreenTextView = (TextView)findViewById(R.id.time_display_textview);
		        timestampString = UsbongUtils.getCurrTimeStamp();
		        myTimeDisplayScreenTextView = (TextView) UsbongUtils.applyTagsInView(myTimeDisplayScreenTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode+"{br}"+timestampString);
		        break;    			        
			case IMAGE_DISPLAY_SCREEN:
		    	setContentView(R.layout.image_display_screen);
		        initBackNextButtons();
		        ImageView myImageDisplayScreenImageView = (ImageView)findViewById(R.id.special_imageview);		       
		        
//		        if (!UsbongUtils.setImageDisplay(myImageDisplayScreenImageView, /*UsbongUtils.USBONG_TREES_FILE_PATH + */myTree+".utree/res/" +UsbongUtils.getResName(currUsbongNode))) {
		        if (!UsbongUtils.setImageDisplay(myImageDisplayScreenImageView, myTree, UsbongUtils.getResName(currUsbongNode))) {
		        //Reference: http://www.anddev.org/tinytut_-_get_resources_by_name__getidentifier_-t460.html; last accessed 14 Sept 2011
//			        Resources myRes = getResources();
			        myDrawableImage = myRes.getDrawable(myRes.getIdentifier("no_image", "drawable", myPackageName));
			        myImageDisplayScreenImageView.setImageDrawable(myDrawableImage);		        		        	
		        }		        
		        break;    	
			case CLICKABLE_IMAGE_DISPLAY_SCREEN:
		    	setContentView(R.layout.clickable_image_display_screen);
		        initBackNextButtons();
		        ImageButton myClickableImageDisplayScreenImageButton = (ImageButton)findViewById(R.id.clickable_image_display_imagebutton);		       
		        
		        if (!UsbongUtils.setClickableImageDisplay(myClickableImageDisplayScreenImageButton, myTree, UsbongUtils.getResName(currUsbongNode))) {
		        //Reference: http://www.anddev.org/tinytut_-_get_resources_by_name__getidentifier_-t460.html; last accessed 14 Sept 2011
//			        Resources myRes = getResources();
			        myDrawableImage = myRes.getDrawable(myRes.getIdentifier("no_image", "drawable", myPackageName));
			        myClickableImageDisplayScreenImageButton.setBackgroundDrawable(myDrawableImage);		        		        	
		        }		  
		        myClickableImageDisplayScreenImageButton.setOnClickListener(new OnClickListener() {
	                @Override
	                public void onClick(View v) {
//	                	myMessage = UsbongUtils.applyTagsInString(currUsbongNode).toString();	    				
	                	
	                	TextView tv = (TextView) UsbongUtils.applyTagsInView(new TextView(UsbongDecisionTreeEngineActivity.getInstance()), UsbongUtils.IS_TEXTVIEW, currUsbongNode);
	                	if (tv.toString().equals("")) {
	                		tv.setText("No message.");
	                	}

	                	new AlertDialog.Builder(UsbongDecisionTreeEngineActivity.this).setTitle("Hey!")
//	            		.setMessage(myMessage)
	            		.setView(tv)
	            		.setPositiveButton("OK", new DialogInterface.OnClickListener() {					
	            			@Override
	            			public void onClick(DialogInterface dialog, int which) {	            				
	            			}
	            		}).show();
	                }
		        });
		        break;    			        
			case TEXT_CLICKABLE_IMAGE_DISPLAY_SCREEN:
		    	setContentView(R.layout.text_clickable_image_display_screen);
		        initBackNextButtons();

		        TextView myTextClickableImageDisplayTextView = (TextView)findViewById(R.id.text_clickable_image_display_textview);
		        myTextClickableImageDisplayTextView = (TextView) UsbongUtils.applyTagsInView(myTextClickableImageDisplayTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);

		        
		        ImageButton myTextClickableImageDisplayScreenImageButton = (ImageButton)findViewById(R.id.clickable_image_display_imagebutton);	
		        
		        if (!UsbongUtils.setClickableImageDisplay(myTextClickableImageDisplayScreenImageButton, myTree, UsbongUtils.getResName(currUsbongNode))) {
		        //Reference: http://www.anddev.org/tinytut_-_get_resources_by_name__getidentifier_-t460.html; last accessed 14 Sept 2011
//			        Resources myRes = getResources();
			        myDrawableImage = myRes.getDrawable(myRes.getIdentifier("no_image", "drawable", myPackageName));
			        myTextClickableImageDisplayScreenImageButton.setBackgroundDrawable(myDrawableImage);		        		        	
		        }		  
		        myTextClickableImageDisplayScreenImageButton.setOnClickListener(new OnClickListener() {
	                @Override
	                public void onClick(View v) {
//	                	myMessage = UsbongUtils.applyTagsInString(currUsbongNode).toString();	    				
	                	
	                	TextView tv = (TextView) UsbongUtils.applyTagsInView(new TextView(UsbongDecisionTreeEngineActivity.getInstance()), UsbongUtils.IS_TEXTVIEW, UsbongUtils.getAlertName(currUsbongNode));
	                	if (tv.toString().equals("")) {
	                		tv.setText("No message.");
	                	}

	                	new AlertDialog.Builder(UsbongDecisionTreeEngineActivity.this).setTitle("Hey!")
//	            		.setMessage(myMessage)
	            		.setView(tv)
	            		.setPositiveButton("OK", new DialogInterface.OnClickListener() {					
	            			@Override
	            			public void onClick(DialogInterface dialog, int which) {	            				
	            			}
	            		}).show();
	                }
		        });
		        break;    	

			case VIDEO_FROM_FILE_SCREEN:
		    	setContentView(R.layout.video_from_file_screen);
		        initBackNextButtons();
		        VideoView myVideoFromFileScreenVideoView = (VideoView)findViewById(R.id.video_from_file_videoview);		       
		        myVideoFromFileScreenVideoView.setVideoPath(UsbongUtils.getPathOfVideoFile(myTree, UsbongUtils.getResName(currUsbongNode)));
/*
		        int width = myVideoFromFileScreenVideoView.getDefaultSize(0, 240);
		        int height = myVideoFromFileScreenVideoView.getDefaultSize(0, 320);

		        myVideoFromFileScreenVideoView.setMeasuredDimension(width, height);
*/		        
		        myVideoFromFileScreenVideoView.start();  		        
		        break;    	
			case TEXT_IMAGE_DISPLAY_SCREEN:
		    	setContentView(R.layout.text_image_display_screen);
		        initBackNextButtons();

		        TextView mytextImageDisplayTextView = (TextView)findViewById(R.id.text_image_display_textview);
		        mytextImageDisplayTextView = (TextView) UsbongUtils.applyTagsInView(mytextImageDisplayTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);
		        ImageView myTextImageDisplayImageView = (ImageView)findViewById(R.id.image_display_imageview);		       
		        
//		        if (!UsbongUtils.setImageDisplay(myTextImageDisplayImageView, /*UsbongUtils.USBONG_TREES_FILE_PATH + */myTree+".utree/res/" +UsbongUtils.getResName(currUsbongNode))) {
		        if (!UsbongUtils.setImageDisplay(myTextImageDisplayImageView, myTree, UsbongUtils.getResName(currUsbongNode))) {
		        //Reference: http://www.anddev.org/tinytut_-_get_resources_by_name__getidentifier_-t460.html; last accessed 14 Sept 2011
//			        Resources myRes = getResources();
			        myDrawableImage = myRes.getDrawable(myRes.getIdentifier("no_image", "drawable", myPackageName));
			        myTextImageDisplayImageView.setImageDrawable(myDrawableImage);		        		        	
		        }
		        break;    	
			case GPS_LOCATION_SCREEN:
		    	setContentView(R.layout.gps_location_screen);
		        initBackNextButtons();

		        TextView myGPSLocationTextView = (TextView)findViewById(R.id.gps_location_textview);
		        myGPSLocationTextView = (TextView) UsbongUtils.applyTagsInView(myGPSLocationTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);

		        final TextView myLongitudeTextView = (TextView)findViewById(R.id.longitude_textview);
            	final TextView myLatitudeTextView = (TextView)findViewById(R.id.latitude_textview);

		        LocationResult locationResult = new LocationResult(){
		            @Override
		            public void gotLocation(Location location){
		                //Got the location!
		            	try {
		            		System.out.println(">>>>>>>>>>>>>>>>>location: "+location);

			            	if (location!=null) {		            	
				            	myLongitudeTextView.setText("long: "+location.getLongitude());
				            	myLatitudeTextView.setText("lat: "+location.getLatitude());
			            	}
			            	else {
			            		Toast.makeText(UsbongDecisionTreeEngineActivity.getInstance(), "Error getting location. Please make sure you are not inside a building.", Toast.LENGTH_SHORT).show();			            		
			            	}
		            	}
		            	catch (Exception e ){
		            		Toast.makeText(UsbongDecisionTreeEngineActivity.getInstance(), "Error getting location.", Toast.LENGTH_SHORT).show();
		            		e.getStackTrace();
		            	}
		            }
		        };
		        myLocation = new FedorMyLocation();
		        
		        //get location only if there's no value yet for long (and lat)
		        if (myLongitudeTextView.getText().toString().equals("long: loading...")) {
		        	myLocation.getLocation(getInstance(), locationResult);		        
		        }
		        break;    	
			case YES_NO_DECISION_SCREEN:
		    	setContentView(R.layout.yes_no_decision_screen);
		        initBackNextButtons();

		        TextView myYesNoDecisionScreenTextView = (TextView)findViewById(R.id.yes_no_decision_textview);
		        myYesNoDecisionScreenTextView = (TextView) UsbongUtils.applyTagsInView(myYesNoDecisionScreenTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);

		        RadioButton myYesRadioButton = (RadioButton)findViewById(R.id.yes_radiobutton);
		        myYesRadioButton.setText(yesStringValue);
		        myYesRadioButton.setTextSize(20);

		        RadioButton myNoRadioButton = (RadioButton)findViewById(R.id.no_radiobutton);		        
		        myNoRadioButton.setText(noStringValue);
		        myNoRadioButton.setTextSize(20);		        
		        
		        if (myStringToken.equals("N")) {
		        	myNoRadioButton.setChecked(true);
		        }
		        else if ((myStringToken.equals("Y"))){
		        	myYesRadioButton.setChecked(true);		        	
		        }
		        break;    	
			case SEND_TO_CLOUD_BASED_SERVICE_SCREEN:
		    	setContentView(R.layout.yes_no_decision_screen);
		        initBackNextButtons();

		        TextView mySendToCloudBasedServiceScreenTextView = (TextView)findViewById(R.id.yes_no_decision_textview);
		        mySendToCloudBasedServiceScreenTextView = (TextView) UsbongUtils.applyTagsInView(mySendToCloudBasedServiceScreenTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);

		        RadioButton mySendToCloudBasedServiceScreenYesRadioButton = (RadioButton)findViewById(R.id.yes_radiobutton);
		        mySendToCloudBasedServiceScreenYesRadioButton.setText(yesStringValue);
		        mySendToCloudBasedServiceScreenYesRadioButton.setTextSize(20);

		        RadioButton mySendToCloudBasedServiceScreenNoRadioButton = (RadioButton)findViewById(R.id.no_radiobutton);		        
		        mySendToCloudBasedServiceScreenNoRadioButton.setText(noStringValue);
		        mySendToCloudBasedServiceScreenNoRadioButton.setTextSize(20);		        

		        if (myStringToken.equals("N")) {
		        	mySendToCloudBasedServiceScreenNoRadioButton.setChecked(true);
		        }
		        else if ((myStringToken.equals("Y"))){
		        	mySendToCloudBasedServiceScreenYesRadioButton.setChecked(true);		        	
		        }		        
		        break;    	
			case SEND_TO_WEBSERVER_SCREEN:
		    	setContentView(R.layout.send_to_webserver_screen);
		        initBackNextButtons();

		        TextView mySendToWebserverScreenTextView = (TextView)findViewById(R.id.send_to_webserver_textview);
		        mySendToWebserverScreenTextView = (TextView) UsbongUtils.applyTagsInView(mySendToWebserverScreenTextView, UsbongUtils.IS_TEXTVIEW, currUsbongNode);

		        TextView myWebserverURLScreenTextView = (TextView)findViewById(R.id.webserver_url_textview);
		        myWebserverURLScreenTextView.setText(UsbongUtils.getDestinationServerURL());
		        
		        RadioButton mySendToWebserverYesRadioButton = (RadioButton)findViewById(R.id.yes_radiobutton);
		        mySendToWebserverYesRadioButton.setText(yesStringValue);
		        mySendToWebserverYesRadioButton.setTextSize(20);

		        RadioButton mySendToWebserverNoRadioButton = (RadioButton)findViewById(R.id.no_radiobutton);		        
		        mySendToWebserverNoRadioButton.setText(noStringValue);
		        mySendToWebserverNoRadioButton.setTextSize(20);		        

		        if (myStringToken.equals("N")) {
		        	mySendToWebserverNoRadioButton.setChecked(true);
		        }
		        else if ((myStringToken.equals("Y"))){
		        	mySendToWebserverYesRadioButton.setChecked(true);		        	
		        }		        
		        break;    	
			case END_STATE_SCREEN:
		    	setContentView(R.layout.end_state_screen);

		        TextView endStateTextView = (TextView)findViewById(R.id.end_state_textview);
		    	if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_FILIPINO) {
		    		endStateTextView.setText((String) getResources().getText(R.string.UsbongEndStateTextViewFILIPINO));				    		
		    	}
		    	else if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_JAPANESE) {
		    		endStateTextView.setText((String) getResources().getText(R.string.UsbongEndStateTextViewJAPANESE));				    						    		
		    	}
		    	else { //if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_ENGLISH) {
		    		endStateTextView.setText((String) getResources().getText(R.string.UsbongEndStateTextViewENGLISH));				    						    		
		    	}
		    	initBackNextButtons();
		        break;    	
    	}
		View myLayout= findViewById(R.id.parent_layout_id);
        if (!UsbongUtils.setBackgroundImage(myLayout, myTree, "bg")) {
    		myLayout.setBackgroundResource(R.drawable.bg);//default bg
        }
        
		if ((!usedBackButton) && (!hasReturnedFromAnotherActivity)){
			usbongNodeContainer.addElement(currUsbongNode);
			usbongNodeContainerCounter++;
		}
		else {
			usedBackButton=false;
			hasReturnedFromAnotherActivity=false;
		}
	}
   
    public void initBackNextButtons()
    {
    	initBackButton();
    	initNextButton();
    }

    public void initBackButton()
    {
    	backButton = (Button)findViewById(R.id.back_button);
    	backButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mTts.isSpeaking()) {
					mTts.stop();
				}

				usedBackButton=true;
				
				decisionTrackerContainer.addElement("B;");
				
				if (!usbongAnswerContainer.isEmpty()) {
/*					
					currentAnswer = usbongAnswerContainer.lastElement();
					usbongAnswerContainer.removeElementAt(usbongAnswerContainer.size()-1);					
*/
					usbongAnswerContainerCounter--;
					
					if (usbongAnswerContainerCounter<0) {
						usbongAnswerContainerCounter=0;
					}
					
					currentAnswer = usbongAnswerContainer.elementAt(usbongAnswerContainerCounter);
				}

				if (!usbongNodeContainer.isEmpty()) {
					usbongNodeContainer.removeElementAt(usbongNodeContainerCounter);                            
	                usbongNodeContainerCounter--;
				}

                if (usbongNodeContainerCounter>=0) {
                	currUsbongNode=(String)usbongNodeContainer.elementAt(usbongNodeContainerCounter);
                }
                else { 
                	processReturnToMainMenuActivity();
/*                	initParser();
                	return;
*/
                }
            	initParser();
			}
    	});    
    }

    public void initNextButton()
    {
    	nextButton = (Button)findViewById(R.id.next_button);
    	nextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				wasNextButtonPressed=true;
				hasUpdatedDecisionTrackerContainer=false;
				
				if (mTts.isSpeaking()) {
					mTts.stop();
				}
				if (currAudioRecorder!=null) {
					try {					
						//if stop button is pressable
						if (stopButton.isEnabled()) { 
							currAudioRecorder.stop();
						}
						if (currAudioRecorder.isPlaying()){
							currAudioRecorder.stopPlayback();
						}					
					}
					catch (Exception e) {
						e.printStackTrace();
					}
					String path = currAudioRecorder.getPath();
//					System.out.println(">>>>>>>>>>>>>>>>>>>currAudioRecorder: "+currAudioRecorder);
					if (!attachmentFilePaths.contains(path)) {
						attachmentFilePaths.add(path);
//						System.out.println(">>>>>>>>>>>>>>>>adding path: "+path);
					}							
				}

		    	//END_STATE_SCREEN = last screen
		    	if (currScreen==END_STATE_SCREEN) {
		    		//"save" the output into the SDCard as "output.txt"
		    		int usbongAnswerContainerSize = usbongAnswerContainer.size();
		    		StringBuffer outputStringBuffer = new StringBuffer();
		    		for(int i=0; i<usbongAnswerContainerSize;i++) {
		    			outputStringBuffer.append(usbongAnswerContainer.elementAt(i));
		    		}
		    		myOutputDirectory=UsbongUtils.getDateTimeStamp()+"/";
//		    		System.out.println(">>>>>>>>>>>>> outputStringBuffer: " + outputStringBuffer.toString());
		    		UsbongUtils.storeOutputInSDCard(UsbongUtils.BASE_FILE_PATH + myOutputDirectory + UsbongUtils.getDateTimeStamp() + ".csv", outputStringBuffer.toString());

//					wasNextButtonPressed=false; //no need to make this true, because this is the last node
					hasUpdatedDecisionTrackerContainer=true;
					
		    		/*
		    		//send to server
		    		UsbongUtils.performFileUpload(UsbongUtils.BASE_FILE_PATH + myOutputDirectory + UsbongUtils.getTimeStamp() + ".csv");
		    		 
		    		//send to email
		    		Intent emailIntent = UsbongUtils.performEmailProcess(UsbongUtils.BASE_FILE_PATH + myOutputDirectory + UsbongUtils.getTimeStamp() + ".csv", attachmentFilePaths);
		    		emailIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//		    		emailIntent.addFlags(RESULT_OK);
		    		startActivityForResult(Intent.createChooser(emailIntent, "Email:"),EMAIL_SENDING_SUCCESS);
*/
		    		finish();
		    	}
		    	else {			
		    		if (currScreen==YES_NO_DECISION_SCREEN) {
				        RadioButton myYesRadioButton = (RadioButton)findViewById(R.id.yes_radiobutton);
				        RadioButton myNoRadioButton = (RadioButton)findViewById(R.id.no_radiobutton);

				        if (myYesRadioButton.isChecked()) {
							currUsbongNode = nextUsbongNodeIfYes;
							
							UsbongUtils.addElementToContainer(usbongAnswerContainer, "Y;", usbongAnswerContainerCounter);
							usbongAnswerContainerCounter++;

							initParser();								        	
				        }
				        else if (myNoRadioButton.isChecked()) {
							currUsbongNode = nextUsbongNodeIfNo; 
//							usbongAnswerContainer.addElement("N;");															
							UsbongUtils.addElementToContainer(usbongAnswerContainer, "N;", usbongAnswerContainerCounter);
							usbongAnswerContainerCounter++;
							
							initParser();								        					        	
				        }
				        else { //if no radio button was checked				        	
				        		if (!isAnOptionalNode) {
			    					showRequiredFieldAlert(PLEASE_CHOOSE_AN_ANSWER_ALERT_TYPE);
			    					wasNextButtonPressed=false;
			    					hasUpdatedDecisionTrackerContainer=true;
			    					return;
				        		}
				        		else {
						    		currUsbongNode = nextUsbongNodeIfYes; //nextUsbongNodeIfNo will also do, since this is "Any"
//									usbongAnswerContainer.addElement("A;");															
									UsbongUtils.addElementToContainer(usbongAnswerContainer, "A;", usbongAnswerContainerCounter);
									usbongAnswerContainerCounter++;
						    		
						    		initParser();				
				        		}
				        }
		    		}	
		    		else if (currScreen==SEND_TO_WEBSERVER_SCREEN) {
				        RadioButton myYesRadioButton = (RadioButton)findViewById(R.id.yes_radiobutton);
				        RadioButton myNoRadioButton = (RadioButton)findViewById(R.id.no_radiobutton);

				        if (myYesRadioButton.isChecked()) {
							currUsbongNode = nextUsbongNodeIfYes; 
//							usbongAnswerContainer.addElement("Y;");			
							UsbongUtils.addElementToContainer(usbongAnswerContainer, "Y;", usbongAnswerContainerCounter);
							usbongAnswerContainerCounter++;
							
							decisionTrackerContainer.addElement(usbongAnswerContainer.lastElement());
//							wasNextButtonPressed=false; //no need to make this true, because "Y;" has already been added to decisionTrackerContainer
							hasUpdatedDecisionTrackerContainer=true;
							
				    		//edited by Mike, March 4, 2013
				    		//"save" the output into the SDCard as "output.txt"
//				    		int usbongAnswerContainerSize = usbongAnswerContainer.size();
				    		int usbongAnswerContainerSize = usbongAnswerContainerCounter;
				    					    		
				    		StringBuffer outputStringBuffer = new StringBuffer();
				    		for(int i=0; i<usbongAnswerContainerSize;i++) {
				    			outputStringBuffer.append(usbongAnswerContainer.elementAt(i));
				    		}

				        	myOutputDirectory=UsbongUtils.getDateTimeStamp()+"/";
				    		UsbongUtils.storeOutputInSDCard(UsbongUtils.BASE_FILE_PATH + myOutputDirectory + UsbongUtils.getDateTimeStamp() + ".csv", outputStringBuffer.toString());

				    		//send to server
				    		UsbongUtils.performFileUpload(UsbongUtils.BASE_FILE_PATH + myOutputDirectory + UsbongUtils.getDateTimeStamp() + ".csv");
				        }
				        else if (myNoRadioButton.isChecked()) {
							currUsbongNode = nextUsbongNodeIfNo; 
//							usbongAnswerContainer.addElement("N;");															
							UsbongUtils.addElementToContainer(usbongAnswerContainer, "N;", usbongAnswerContainerCounter);
							usbongAnswerContainerCounter++;
				        }
				        else { //if no radio button was checked				        	
			        		if (!isAnOptionalNode) {
				        		showRequiredFieldAlert(PLEASE_CHOOSE_AN_ANSWER_ALERT_TYPE);
		    					wasNextButtonPressed=false;
		    					hasUpdatedDecisionTrackerContainer=true;
		    					return;
			        		}
			        		else {
					    		currUsbongNode = nextUsbongNodeIfYes; //nextUsbongNodeIfNo will also do, since this is "Any"
//								usbongAnswerContainer.addElement("A;");															
					    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "A;", usbongAnswerContainerCounter);
								usbongAnswerContainerCounter++;

								initParser();				
			        		}
				        }
				        /*
						currUsbongNode = nextUsbongNodeIfYes; //nextUsbongNodeIfNo will also do, since this is "Any"
						usbongAnswerContainer.addElement("Any;");															
*/						
						initParser();				
		    		}	
		    		else if (currScreen==SEND_TO_CLOUD_BASED_SERVICE_SCREEN) {
				        RadioButton myYesRadioButton = (RadioButton)findViewById(R.id.yes_radiobutton);
				        RadioButton myNoRadioButton = (RadioButton)findViewById(R.id.no_radiobutton);

				        if (myYesRadioButton.isChecked()) {
							currUsbongNode = nextUsbongNodeIfYes; 
//							usbongAnswerContainer.addElement("Y;");															
				    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "Y;", usbongAnswerContainerCounter);
							usbongAnswerContainerCounter++;

							decisionTrackerContainer.addElement(usbongAnswerContainer.lastElement());
//							wasNextButtonPressed=false; //no need to make this true, because "Y;" has already been added to decisionTrackerContainer
							hasUpdatedDecisionTrackerContainer=true;
							
							StringBuffer sb = new StringBuffer();
							for (int i=0; i<decisionTrackerContainer.size();i++) {
								sb.append(decisionTrackerContainer.elementAt(i));
							}
							Log.d(">>>>>>>>>>>>>decisionTrackerContainer", sb.toString());								
							
				    		//edited by Mike, March 4, 2013
				    		//"save" the output into the SDCard as "output.txt"
//				    		int usbongAnswerContainerSize = usbongAnswerContainer.size();
				    		int usbongAnswerContainerSize = usbongAnswerContainerCounter;

				    		StringBuffer outputStringBuffer = new StringBuffer();
				    		for(int i=0; i<usbongAnswerContainerSize; i++) {
				    			outputStringBuffer.append(usbongAnswerContainer.elementAt(i));
				    		}

				        	myOutputDirectory=UsbongUtils.getDateTimeStamp()+"/";
				    		UsbongUtils.storeOutputInSDCard(UsbongUtils.BASE_FILE_PATH + myOutputDirectory + UsbongUtils.getDateTimeStamp() + ".csv", outputStringBuffer.toString());

				    		//send to email
				    		Intent emailIntent = UsbongUtils.performEmailProcess(UsbongUtils.BASE_FILE_PATH + myOutputDirectory + UsbongUtils.getDateTimeStamp() + ".csv", attachmentFilePaths);
				    		/*emailIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
*/
//				    		emailIntent.addFlags(RESULT_OK);
//				    		startActivityForResult(Intent.createChooser(emailIntent, "Email:"),EMAIL_SENDING_SUCCESS);
				    		//answer from Llango J, stackoverflow
				    		//Reference: http://stackoverflow.com/questions/7479883/problem-with-sending-email-goes-back-to-previous-activity;
				    		//last accessed: 22 Oct. 2012
				    		startActivity(Intent.createChooser(emailIntent, "Email:"));
				        }
				        else if (myNoRadioButton.isChecked()) {
							currUsbongNode = nextUsbongNodeIfNo; 
//							usbongAnswerContainer.addElement("N;");															
				    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "N;", usbongAnswerContainerCounter);
							usbongAnswerContainerCounter++;
				        }
				        else { //if no radio button was checked				        	
			        		if (!isAnOptionalNode) {
			        			showRequiredFieldAlert(PLEASE_CHOOSE_AN_ANSWER_ALERT_TYPE);
		    					wasNextButtonPressed=false;
		    					hasUpdatedDecisionTrackerContainer=true;
		    					
		    					return;
			        		}
			        		else {
					    		currUsbongNode = nextUsbongNodeIfYes; //nextUsbongNodeIfNo will also do, since this is "Any"
//								usbongAnswerContainer.addElement("A;");															
					    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "A;", usbongAnswerContainerCounter);
								usbongAnswerContainerCounter++;

								initParser();				
			        		}
				        }
/*				        
						currUsbongNode = nextUsbongNodeIfYes; //nextUsbongNodeIfNo will also do, since this is "Any"
						usbongAnswerContainer.addElement("Any;");															
*/						
						initParser();				
		    		}	
		    		else if (currScreen==MULTIPLE_CHECKBOXES_SCREEN) {
//			    		requiredTotalCheckedBoxes	
				        LinearLayout myMultipleCheckboxesLinearLayout = (LinearLayout)findViewById(R.id.multiple_checkboxes_linearlayout);
				        StringBuffer sb = new StringBuffer();
				        int totalCheckedBoxes=0;
				        int totalCheckBoxChildren = myMultipleCheckboxesLinearLayout.getChildCount();
				        //begin with i=1, because i=0 is for checkboxes_textview
				        for(int i=1; i<totalCheckBoxChildren; i++) {
				        	if (((CheckBox)myMultipleCheckboxesLinearLayout.getChildAt(i)).isChecked()) {
				        		totalCheckedBoxes++;
				        		sb.append(","+(i-1)); //do a (i-1) so that i starts with 0 for the checkboxes (excluding checkboxes_textview) to maintain consistency with the other components
				        	}
				        }
				        
				        if (totalCheckedBoxes>=requiredTotalCheckedBoxes) {
							currUsbongNode = nextUsbongNodeIfYes; 	
			        		sb.insert(0,"Y"); //insert in front of stringBuffer
							sb.append(";");
				        }
				        else {
							currUsbongNode = nextUsbongNodeIfNo; 				        					        	
							sb.delete(0,sb.length());
							sb.append("N,;"); //make sure to add the comma
				        }				        
//		    			usbongAnswerContainer.addElement(sb.toString());
			    		UsbongUtils.addElementToContainer(usbongAnswerContainer, sb.toString(), usbongAnswerContainerCounter);
						usbongAnswerContainerCounter++;

		    			initParser();
		    		}	
		    		else if (currScreen==MULTIPLE_RADIO_BUTTONS_SCREEN) {
						currUsbongNode = nextUsbongNodeIfYes; //nextUsbongNodeIfNo will also do, since this is "Any"
		    			RadioGroup myRadioGroup = (RadioGroup)findViewById(R.id.multiple_radio_buttons_radiogroup);				        				        		    			

//		    			if (UsbongUtils.IS_IN_DEBUG_MODE==false) {
			    			if (myRadioGroup.getCheckedRadioButtonId()==-1) { //no radio button checked
				        		if (!UsbongUtils.IS_IN_DEBUG_MODE) {
				    				if (!isAnOptionalNode) {
					    				showRequiredFieldAlert(PLEASE_CHOOSE_AN_ANSWER_ALERT_TYPE);
				    					wasNextButtonPressed=false;
				    					hasUpdatedDecisionTrackerContainer=true;
				    					return;
					        		}
				        		}
//				        		else {
					    		currUsbongNode = nextUsbongNodeIfYes; //nextUsbongNodeIfNo will also do, since this is "Any"
					    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "A;", usbongAnswerContainerCounter);
								usbongAnswerContainerCounter++;

					    		initParser();				
//				        		}
			    			}
			    			else {
//				    			usbongAnswerContainer.addElement(myRadioGroup.getCheckedRadioButtonId()+";");
					    		UsbongUtils.addElementToContainer(usbongAnswerContainer, myRadioGroup.getCheckedRadioButtonId()+";", usbongAnswerContainerCounter);
								usbongAnswerContainerCounter++;

				    			initParser();
			    			}
//		    			}
/*		    			
		    			else {
//			    			usbongAnswerContainer.addElement(myRadioGroup.getCheckedRadioButtonId()+";");
				    		UsbongUtils.addElementToContainer(usbongAnswerContainer, myRadioGroup.getCheckedRadioButtonId()+";", usbongAnswerContainerCounter);
							usbongAnswerContainerCounter++;
			    			
			    			initParser();		    				
		    			}
*/		    			
		    		}
		    		else if (currScreen==MULTIPLE_RADIO_BUTTONS_WITH_ANSWER_SCREEN) {
						currUsbongNode = nextUsbongNodeIfYes; //nextUsbongNodeIfNo will also do, since this is "Any"
		    			RadioGroup myRadioGroup = (RadioGroup)findViewById(R.id.multiple_radio_buttons_radiogroup);				        				        		    			

/*		    			if (UsbongUtils.IS_IN_DEBUG_MODE==false) {
 */
			    			if (myRadioGroup.getCheckedRadioButtonId()==-1) { //no radio button checked			    							    				
				        		if (!UsbongUtils.IS_IN_DEBUG_MODE) {
				        			if (!isAnOptionalNode) {
					    				showRequiredFieldAlert(PLEASE_CHOOSE_AN_ANSWER_ALERT_TYPE);
				    					wasNextButtonPressed=false;
				    					hasUpdatedDecisionTrackerContainer=true;
				    					return;
				        			}
				        		}
//				        		else {
			        			currUsbongNode = nextUsbongNodeIfYes; //choose Yes if "Any"
					    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "A,"+myRadioGroup.getCheckedRadioButtonId()+";", usbongAnswerContainerCounter);
								usbongAnswerContainerCounter++;

					    		initParser();				
//				        		}
			    			}
			    			else {			    				
			        			if (myMultipleRadioButtonsWithAnswerScreenAnswer.equals(""+myRadioGroup.getCheckedRadioButtonId())) {
	    							currUsbongNode = nextUsbongNodeIfYes; 	
						    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "Y,"+myRadioGroup.getCheckedRadioButtonId()+";", usbongAnswerContainerCounter);
	    				        }
	    				        else {
	    							currUsbongNode = nextUsbongNodeIfNo; 				        					        	
						    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "N,"+myRadioGroup.getCheckedRadioButtonId()+";", usbongAnswerContainerCounter);
	    				        }				        

								usbongAnswerContainerCounter++;
				    			initParser();
			    			}
		    		}
		    		else if (currScreen==LINK_SCREEN) {		    			
		    			RadioGroup myRadioGroup = (RadioGroup)findViewById(R.id.multiple_radio_buttons_radiogroup);				        				        		    			

		    			try {
		    				currUsbongNode = UsbongUtils.getLinkFromRadioButton(radioButtonsContainer.elementAt(myRadioGroup.getCheckedRadioButtonId()));		    				
		    			}
		    			catch(Exception e) {
		    				//if the user hasn't ticked any radio button yet
		    				//put the currUsbongNode to default
			    			currUsbongNode = UsbongUtils.getLinkFromRadioButton(nextUsbongNodeIfYes); //nextUsbongNodeIfNo will also do, since this is "Any"
			    			//of course, showPleaseAnswerAlert() will be called			    			  
		    			}		    			
			    			if (myRadioGroup.getCheckedRadioButtonId()==-1) { //no radio button checked
//				    			if (!UsbongUtils.IS_IN_DEBUG_MODE) {
				    				if (!isAnOptionalNode) {
					    				showRequiredFieldAlert(PLEASE_CHOOSE_AN_ANSWER_ALERT_TYPE);
				    					wasNextButtonPressed=false;
				    					hasUpdatedDecisionTrackerContainer=true;
				    					
				    					return;
					        		}
//				    			}
//				        		else {
						    		currUsbongNode = nextUsbongNodeIfYes; //nextUsbongNodeIfNo will also do, since this is "Any"
						    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "A;", usbongAnswerContainerCounter);
									usbongAnswerContainerCounter++;
									
									initParser();				
//				        		}
			    			}
			    			else {
//				    			usbongAnswerContainer.addElement(myRadioGroup.getCheckedRadioButtonId()+";");
					    		UsbongUtils.addElementToContainer(usbongAnswerContainer, myRadioGroup.getCheckedRadioButtonId()+";", usbongAnswerContainerCounter);
								usbongAnswerContainerCounter++;

				    			initParser();
			    			}
/*		    			}
		    			else {
			    			usbongAnswerContainer.addElement(myRadioGroup.getCheckedRadioButtonId()+";");
					        initParser();		    				
		    			}
*/		    			
		    		}
		    		else if ((currScreen==TEXTFIELD_SCREEN) 
		    				|| (currScreen==TEXTFIELD_WITH_UNIT_SCREEN)
		    				|| (currScreen==TEXTFIELD_NUMERICAL_SCREEN)) {
		    			currUsbongNode = nextUsbongNodeIfYes; //= nextIMCIQuestionIfNo will also do
				        EditText myTextFieldScreenEditText = (EditText)findViewById(R.id.textfield_edittext);

//				        if (UsbongUtils.IS_IN_DEBUG_MODE==false) {
					        //if it's blank
			    			if (myTextFieldScreenEditText.getText().toString().trim().equals("")) {
						        if (!UsbongUtils.IS_IN_DEBUG_MODE) {
				    				if (!isAnOptionalNode) {
					    				showRequiredFieldAlert(PLEASE_ANSWER_FIELD_ALERT_TYPE);
				    					wasNextButtonPressed=false;
				    					hasUpdatedDecisionTrackerContainer=true;
				    					return;
					        		}
						        }
//				        		else {
						    		currUsbongNode = nextUsbongNodeIfYes; //nextUsbongNodeIfNo will also do, since this is "Any"
//									usbongAnswerContainer.addElement("A;");															
						    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "A,;", usbongAnswerContainerCounter);
									usbongAnswerContainerCounter++;
						    		
						    		initParser();				
//				        		}
			    			}
							else {
//								usbongAnswerContainer.addElement("A,"+myTextFieldScreenEditText.getText()+";");							
					    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "A,"+myTextFieldScreenEditText.getText()+";", usbongAnswerContainerCounter);
								usbongAnswerContainerCounter++;
								
								initParser();
							}
/*			    			
				        }
				        else {
//							usbongAnswerContainer.addElement("A,"+myTextFieldScreenEditText.getText()+";");							
				    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "A,"+myTextFieldScreenEditText.getText()+";", usbongAnswerContainerCounter);
							usbongAnswerContainerCounter++;

				        	initParser();				        	
				        }
*/				        
		    		}
		    		else if (currScreen==TEXTFIELD_WITH_ANSWER_SCREEN) {
		    			currUsbongNode = nextUsbongNodeIfYes; 
				        EditText myTextFieldScreenEditText = (EditText)findViewById(R.id.textfield_edittext);
					        //if it's blank
			    			if (myTextFieldScreenEditText.getText().toString().trim().equals("")) {
				        		if (!UsbongUtils.IS_IN_DEBUG_MODE) {
				        			if (!isAnOptionalNode) {
					    				showRequiredFieldAlert(PLEASE_ANSWER_FIELD_ALERT_TYPE);
				    					wasNextButtonPressed=false;
				    					hasUpdatedDecisionTrackerContainer=true;
				    					return;
				        			}
				        		}
					    		currUsbongNode = nextUsbongNodeIfYes; //choose Yes if "Any"
					    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "A,"+myTextFieldScreenEditText.getText().toString().trim()+";", usbongAnswerContainerCounter);
								usbongAnswerContainerCounter++;
					    		
					    		initParser();				
			    			}
							else {
			        			if (myTextFieldWithAnswerScreenAnswer.equals(myTextFieldScreenEditText.getText().toString().trim())) {
									currUsbongNode = nextUsbongNodeIfYes; 	
						    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "Y,"+myTextFieldScreenEditText.getText().toString().trim()+";", usbongAnswerContainerCounter);
						        }
						        else {
									currUsbongNode = nextUsbongNodeIfNo; 				        					        	
						    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "N,"+myTextFieldScreenEditText.getText().toString().trim()+";", usbongAnswerContainerCounter);
						        }				        

								usbongAnswerContainerCounter++;
				    			initParser();
							}
		    		}
		    		else if ((currScreen==TEXTAREA_SCREEN)) {
		    			currUsbongNode = nextUsbongNodeIfYes; //= nextIMCIQuestionIfNo will also do
				        EditText myTextAreaScreenEditText = (EditText)findViewById(R.id.textarea_edittext);

//				        if (UsbongUtils.IS_IN_DEBUG_MODE==false) {
					        //if it's blank
			    			if (myTextAreaScreenEditText.getText().toString().trim().equals("")) {
						        if (!UsbongUtils.IS_IN_DEBUG_MODE) {							        	
					        		if (!isAnOptionalNode) {
					    				showRequiredFieldAlert(PLEASE_ANSWER_FIELD_ALERT_TYPE);
				    					wasNextButtonPressed=false;
				    					hasUpdatedDecisionTrackerContainer=true;
				    					return;
					        		}
						        }
//				        		else {
						    		currUsbongNode = nextUsbongNodeIfYes; //nextUsbongNodeIfNo will also do, since this is "Any"
						    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "A,;", usbongAnswerContainerCounter);
									usbongAnswerContainerCounter++;
									
									initParser();				
//				        		}
			    			}
							else {
//								usbongAnswerContainer.addElement("A,"+myTextAreaScreenEditText.getText()+";");							
					    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "A,"+myTextAreaScreenEditText.getText()+";", usbongAnswerContainerCounter);
								usbongAnswerContainerCounter++;
								
								initParser();
							}
				        /*}
				        else {
//							usbongAnswerContainer.addElement("A,"+myTextAreaScreenEditText.getText()+";");							
				    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "A,"+myTextAreaScreenEditText.getText()+";", usbongAnswerContainerCounter);
							usbongAnswerContainerCounter++;
							
							initParser();				        	
				        }*/
		    		}
		    		else if (currScreen==TEXTAREA_WITH_ANSWER_SCREEN) {
		    			currUsbongNode = nextUsbongNodeIfYes; 
				        EditText myTextAreaScreenEditText = (EditText)findViewById(R.id.textarea_edittext);
					        //if it's blank
			    			if (myTextAreaScreenEditText.getText().toString().trim().equals("")) {
				        		if (!UsbongUtils.IS_IN_DEBUG_MODE) {
				        			if (!isAnOptionalNode) {
					    				showRequiredFieldAlert(PLEASE_ANSWER_FIELD_ALERT_TYPE);
				    					wasNextButtonPressed=false;
				    					hasUpdatedDecisionTrackerContainer=true;
				    					return;
				        			}
				        		}
					    		currUsbongNode = nextUsbongNodeIfYes; //choose Yes if "Any"
					    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "A,"+myTextAreaScreenEditText.getText().toString().trim()+";", usbongAnswerContainerCounter);
								usbongAnswerContainerCounter++;
					    		
					    		initParser();				
			    			}
							else {
			        			if (myTextAreaWithAnswerScreenAnswer.equals(myTextAreaScreenEditText.getText().toString().trim())) {
									currUsbongNode = nextUsbongNodeIfYes; 	
						    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "Y,"+myTextAreaScreenEditText.getText().toString().trim()+";", usbongAnswerContainerCounter);
						        }
						        else {
									currUsbongNode = nextUsbongNodeIfNo; 				        					        	
						    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "N,"+myTextAreaScreenEditText.getText().toString().trim()+";", usbongAnswerContainerCounter);
						        }				        

								usbongAnswerContainerCounter++;
				    			initParser();
							}
		    		}
		    		else if (currScreen==GPS_LOCATION_SCREEN) {
		    			currUsbongNode = nextUsbongNodeIfYes; //= nextIMCIQuestionIfNo will also do
						TextView myLongitudeTextView = (TextView)findViewById(R.id.longitude_textview);
			            TextView myLatitudeTextView = (TextView)findViewById(R.id.latitude_textview);

//						usbongAnswerContainer.addElement(myLongitudeTextView.getText()+","+myLatitudeTextView.getText()+";");							
			    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "A,"+myLongitudeTextView.getText()+","+myLatitudeTextView.getText()+";", usbongAnswerContainerCounter);
						usbongAnswerContainerCounter++;

			            initParser();				        	

		    		}
		    		else if (currScreen==DATE_SCREEN) {
		    			currUsbongNode = nextUsbongNodeIfYes;
				    	Spinner dateMonthSpinner = (Spinner) findViewById(R.id.date_month_spinner);
				        Spinner dateDaySpinner = (Spinner) findViewById(R.id.date_day_spinner);
				        EditText myDateYearEditText = (EditText)findViewById(R.id.date_edittext);
/*		    			usbongAnswerContainer.addElement("A,"+monthAdapter.getItem(dateMonthSpinner.getSelectedItemPosition()).toString() +
								 						 dayAdapter.getItem(dateDaySpinner.getSelectedItemPosition()).toString() + "," +
								 						 myDateYearEditText.getText().toString()+";");		    					
*/
			    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "A,"+monthAdapter.getItem(dateMonthSpinner.getSelectedItemPosition()).toString() +
								 						 dayAdapter.getItem(dateDaySpinner.getSelectedItemPosition()).toString() + "," +
								 						 myDateYearEditText.getText().toString()+";", usbongAnswerContainerCounter);
						usbongAnswerContainerCounter++;

//		    			System.out.println(">>>>>>>>>>>>>Date screen: "+usbongAnswerContainer.lastElement());
		    			initParser();				        	
		    		}		    		
		    		else if (currScreen==TIMESTAMP_DISPLAY_SCREEN) {
		    			currUsbongNode = nextUsbongNodeIfYes;
			    		UsbongUtils.addElementToContainer(usbongAnswerContainer, timestampString+";", usbongAnswerContainerCounter);
						usbongAnswerContainerCounter++;

		    			initParser();				        	
		    		}		    				    		
		    		else if (currScreen==QR_CODE_READER_SCREEN) {
		    			currUsbongNode = nextUsbongNodeIfYes; //= nextIMCIQuestionIfNo will also do

		    			if (!myQRCodeContent.equals("")) {
//		    				usbongAnswerContainer.addElement("Y,"+myQRCodeContent+";");							
				    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "Y,"+myQRCodeContent+";", usbongAnswerContainerCounter);
							usbongAnswerContainerCounter++;
		    			}
		    			else {
//		    				usbongAnswerContainer.addElement("N;");									    				
				    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "N;", usbongAnswerContainerCounter);
							usbongAnswerContainerCounter++;
		    			}
		    			initParser();				        	
		    		}
		    		else if ((currScreen==DCAT_SUMMARY_SCREEN)) {
		    			currUsbongNode = nextUsbongNodeIfYes; //= nextIMCIQuestionIfNo will also do

		    			UsbongUtils.addElementToContainer(usbongAnswerContainer, "dcat_end;", usbongAnswerContainerCounter);
						usbongAnswerContainerCounter++;
						
						initParser();		    		}

		    		else { //TODO: do this for now		    		
		    			currUsbongNode = nextUsbongNodeIfYes; //nextUsbongNodeIfNo will also do, since this is "Any"
//						usbongAnswerContainer.addElement("A;");															
			    		UsbongUtils.addElementToContainer(usbongAnswerContainer, "A;", usbongAnswerContainerCounter);
						usbongAnswerContainerCounter++;

						initParser();				
		    		}		    		
		    	}
			}
    	});
    }
    
    public void initRecordAudioScreen() {    	    	
        String timeStamp = UsbongUtils.getDateTimeStamp();
//        final AudioRecorder recorder = new AudioRecorder("/usbong/" + timeStamp,currUsbongNode);
//        currAudioRecorder = recorder;        
        
        stopButton = (Button) findViewById(R.id.stop_button);
        stopButton.setEnabled(false); 
        recordButton = (Button) findViewById(R.id.record_button);
        playButton = (Button) findViewById(R.id.play_button);
        playButton.setEnabled(false);         	

        /*
        if ((currAudioRecorder!=null) && (!currAudioRecorder.hasRecordedData())) {
            playButton.setEnabled(false);         	
        }
        else {
        	currAudioRecorder = new AudioRecorder("/usbong/" + timeStamp,currUsbongNode);
        }
*/        
    	currAudioRecorder = new AudioRecorder("/usbong/" + timeStamp,currUsbongNode);

        // add a click listener to the button
        recordButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                	currAudioRecorder.start();
					stopButton.setEnabled(true);
					recordButton.setEnabled(false);
			        playButton.setEnabled(false);
				} catch (IOException e) {
						e.printStackTrace();
					}
	            }
	        });    
 

	    // add a click listener to the button
        stopButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	try {
            		currAudioRecorder.stop();
					recordButton.setEnabled(true); 
					playButton.setEnabled(true);
					stopButton.setEnabled(false);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        });    

        // add a click listener to the button
        playButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	try {
            		currAudioRecorder.play();
					recordButton.setEnabled(true); 
					//play.setEnabled(false);
					stopButton.setEnabled(false);
            	} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        });    
	    	
	    new AlertDialog.Builder(UsbongDecisionTreeEngineActivity.this).setTitle("Usbong Tip")
     	.setMessage("Press the Record button to start recording. Press Stop to stop the recording. And press Play to hear what you've recorded!") // When you're done, hit the menu button and save your work.
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		}).show();		
    }
    
    public void initTakePhotoScreen()
    {
    	myPictureName=currUsbongNode; //make the name of the picture the name of the currUsbongNode

//		String path = "/sdcard/usbong/"+ UsbongUtils.getTimeStamp() +"/"+ myPictureName +".jpg";
		String path = UsbongUtils.BASE_FILE_PATH + UsbongUtils.getDateTimeStamp()+"/"+ myPictureName +".jpg";		
		//only add path if it's not already in attachmentFilePaths
		if (!attachmentFilePaths.contains(path)) {
			attachmentFilePaths.add(path);
		}
		
    	setContentView(R.layout.photo_capture_screen);
    	myImageView = (ImageView) findViewById(R.id.CameraImage);

    	File imageFile = new File(path);
        
        if(imageFile.exists())
        {
        	Bitmap myBitmap = BitmapFactory.decodeFile(path);
        	if(myBitmap != null)
        	{
        		myImageView.setImageBitmap(myBitmap);
        	}
        	//Read more: http://www.brighthub.com/mobile/google-android/articles/64048.aspx#ixzz0yXLCazcU                	  
        }
        else
        {        	
        }
    	photoCaptureButton = (Button)findViewById(R.id.photo_capture_button);
		photoCaptureIntent = new Intent().setClass(this, CameraActivity.class);
		photoCaptureIntent.putExtra("myPictureName",myPictureName);
		photoCaptureButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(UsbongDecisionTreeEngineActivity.photoCaptureIntent);
			}
    	});

    }

    public void initPaintScreen()
    {
    	myPaintName=currUsbongNode; //make the name of the picture the name of the currUsbongNode

//		String path = "/sdcard/usbong/"+ UsbongUtils.getTimeStamp() +"/"+ myPictureName +".jpg";
		String path = UsbongUtils.BASE_FILE_PATH + UsbongUtils.getDateTimeStamp()+"/"+ myPaintName +".jpg";		

		//only add path if it's not already in attachmentFilePaths
		if (!attachmentFilePaths.contains(path)) {
			attachmentFilePaths.add(path);
		}

    	setContentView(R.layout.paint_screen);
    	myImageView = (ImageView) findViewById(R.id.PaintImage);

    	File imageFile = new File(path);
        if(imageFile.exists())
        {
        	Bitmap myBitmap = BitmapFactory.decodeFile(path);
        	if(myBitmap != null)
        	{
        		myImageView.setImageBitmap(myBitmap);
        	}
        	//Read more: http://www.brighthub.com/mobile/google-android/articles/64048.aspx#ixzz0yXLCazcU                	  
        }
        else
        {        	
        }
    	paintButton = (Button)findViewById(R.id.paint_button);
    	paintIntent = new Intent().setClass(this, PaintActivity.class);
		paintIntent.putExtra("myPaintName",myPaintName);
		paintButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(UsbongDecisionTreeEngineActivity.paintIntent);
			}
    	});
    }
    
    public void initQRCodeReaderScreen() {
    	myQRCodeReaderName=currUsbongNode; 
    	setContentView(R.layout.qr_code_reader_screen);

    	TextView qrCodeReaderResultTextView = (TextView)findViewById(R.id.qr_code_reader_result_textview);
    	TextView qrCodeReaderContentTextView = (TextView)findViewById(R.id.qr_code_reader_content_textview);
    	
    	if (!myQRCodeContent.equals("")) {
	    	if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_FILIPINO) {
	    		qrCodeReaderResultTextView.setText((String) getResources().getText(R.string.UsbongQRCodeReaderContentResultFILIPINO));
	    	}
	    	else if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_JAPANESE) {
	    		qrCodeReaderResultTextView.setText((String) getResources().getText(R.string.UsbongQRCodeReaderContentResultJAPANESE));				    						    		
	    	}
	    	else { //if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_ENGLISH) {
	    		qrCodeReaderResultTextView.setText((String) getResources().getText(R.string.UsbongQRCodeReaderContentResultENGLISH));				    						    		
	    	}

//    		qrCodeReaderResultTextView.setText("QR Code content successfully saved!");
    		qrCodeReaderContentTextView.setText("--\n"+myQRCodeContent);
    	}
    	else {
    		if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_FILIPINO) {
	    		qrCodeReaderResultTextView.setText((String) getResources().getText(R.string.UsbongQRCodeReaderContentResultNoneFILIPINO));
	    	}
	    	else if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_JAPANESE) {
	    		qrCodeReaderResultTextView.setText((String) getResources().getText(R.string.UsbongQRCodeReaderContentResultNoneJAPANESE));				    						    		
	    	}
	    	else { //if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_ENGLISH) {
	    		qrCodeReaderResultTextView.setText((String) getResources().getText(R.string.UsbongQRCodeReaderContentResultNoneENGLISH));				    						    		
	    	}
//    		qrCodeReaderResultTextView.setText("No QR Code content has been saved yet!");    		
    	}

    	qrCodeReaderButton = (Button)findViewById(R.id.qr_code_reader_button);
    	qrCodeReaderIntent = new Intent().setClass(this, QRCodeReaderActivity.class);
    	qrCodeReaderIntent.putExtra("myQRCodeReaderName",myQRCodeReaderName);
    	qrCodeReaderButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(UsbongDecisionTreeEngineActivity.qrCodeReaderIntent);
			}
    	});    	
    }
    
    public static void setQRCodeContent(String s) {
    	myQRCodeContent = s;
    }

    public void decrementCurrScreen() {
    	currScreen--;
    	initUsbongScreen();
    }

    //this is not used at the moment (Sept 5, 2011)
    public void incrementCurrScreen() {
    	currScreen++;
    	initUsbongScreen();
    }
    
    private void processReturnToMainMenuActivity() {
	    	String myPromptTitle="";
	    	String myPromptMessage="";
	    	String myPromptPositiveButtonText="";
	    	String myPromptNegativeButtonText="";
	    	
	    	if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_FILIPINO) {
	    		myPromptTitle = ((String) getResources().getText(R.string.alertStringValueFilipino));
	    		myPromptMessage = ((String) getResources().getText(R.string.areYouSureYouWantToReturnToMainMenuFilipino));
	    		myPromptPositiveButtonText=(String) getResources().getText(R.string.yesStringValueFilipino);
	    		myPromptNegativeButtonText=(String) getResources().getText(R.string.noStringValueFilipino);  
	    	}
	    	else if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_JAPANESE) {
	    		myPromptTitle = ((String) getResources().getText(R.string.alertStringValueJapanese));				    						    		
	    		myPromptMessage = ((String) getResources().getText(R.string.areYouSureYouWantToReturnToMainMenuJapanese));
	    		myPromptPositiveButtonText=(String) getResources().getText(R.string.yesStringValueJapanese);
	    		myPromptNegativeButtonText=(String) getResources().getText(R.string.noStringValueJapanese);  
	    	}
	    	else { //if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_ENGLISH) {
	    		myPromptTitle = ((String) getResources().getText(R.string.alertStringValueEnglish));				    						    		        	    		
	    		myPromptMessage = ((String) getResources().getText(R.string.areYouSureYouWantToReturnToMainMenuEnglish));
	    		myPromptPositiveButtonText=(String) getResources().getText(R.string.yesStringValueEnglish);
	    		myPromptNegativeButtonText=(String) getResources().getText(R.string.noStringValueEnglish);  
	    	}
	
	    	//added by Mike, Feb. 2, 2013
	    	AlertDialog.Builder prompt = new AlertDialog.Builder(UsbongDecisionTreeEngineActivity.this);
			prompt.setTitle(myPromptTitle);
			prompt.setMessage(myPromptMessage); 
			prompt.setPositiveButton(myPromptPositiveButtonText, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					decisionTrackerContainer.removeAllElements();
					//return to main activity
		    		finish();    
					Intent toUsbongMainActivityIntent = new Intent(UsbongDecisionTreeEngineActivity.this, UsbongMainActivity.class);
					toUsbongMainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
					startActivity(toUsbongMainActivityIntent);
				}
			});
			prompt.setNegativeButton(myPromptNegativeButtonText, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			prompt.show();
    }
    
	private class CustomDataAdapter extends ArrayAdapter<String>
	{
		private ArrayList<String> items;
		
		public CustomDataAdapter(Context context, int textViewResourceId, ArrayList<String> items) {
            super(context, textViewResourceId, items);
            this.items = items;
		}
		
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.tree_loader, null);
                }
                final String o = items.get(position);
                if (o != null) {
                	TextView dataCurrentTextView = (TextView)v.findViewById(R.id.tree_item);
                	dataCurrentTextView.setText(o.toString());
                	dataCurrentTextView.setOnClickListener(new OnClickListener() {
            			@Override
            			public void onClick(View v) {
            				isInTreeLoader=false;
            				
            				myTree = o.toString();
            		        initParser();
            			}
                	});
/*                	
                	Button selectButton = (Button)v.findViewById(R.id.select_tree_button);
                	selectButton.setOnClickListener(new OnClickListener() {
            			@Override
            			public void onClick(View v) {
            				myTree = o.toString();
            		        initParser();
            			}
                	});
*/                	
                }
                return v;
        }
	}
	
	public void showRequiredFieldAlert(int type) {
		String requiredFieldAlertString="";
		String alertString = "";
		
		switch(type) {
			case PLEASE_CHOOSE_AN_ANSWER_ALERT_TYPE:
		        if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_FILIPINO) {
		        	requiredFieldAlertString = (String) getResources().getText(R.string.pleaseChooseAnAnswerStringValueFilipino);
		        }
		        else if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_JAPANESE) {
		        	requiredFieldAlertString = (String) getResources().getText(R.string.pleaseChooseAnAnswerStringValueJapanese);
		        }
		        else {
		        	requiredFieldAlertString = (String) getResources().getText(R.string.pleaseChooseAnAnswerStringValue);		        	
		        }
		        break;
			case PLEASE_ANSWER_FIELD_ALERT_TYPE:
		        if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_FILIPINO) {
		        	requiredFieldAlertString = (String) getResources().getText(R.string.pleaseAnswerFieldStringValueFilipino);
		        }
		        else if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_JAPANESE) {
		        	requiredFieldAlertString = (String) getResources().getText(R.string.pleaseAnswerFieldStringValueJapanese);
		        }
		        else {
		        	requiredFieldAlertString = (String) getResources().getText(R.string.pleaseAnswerFieldStringValue);		        	
		        }
				break;
		}
		requiredFieldAlertString = "{big}"+requiredFieldAlertString+"{/big}";

        if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_FILIPINO) {
    		alertString = (String) getResources().getText(R.string.alertStringValueFilipino);
        }
        else if (currLanguageBeingUsed==UsbongUtils.LANGUAGE_JAPANESE) {
    		alertString = (String) getResources().getText(R.string.alertStringValueJapanese);
        }
        else {
    		alertString = (String) getResources().getText(R.string.alertStringValueEnglish);
        }
    	
    	TextView requiredFieldAlertStringTextView = (TextView) UsbongUtils.applyTagsInView(new TextView(UsbongDecisionTreeEngineActivity.getInstance()), UsbongUtils.IS_TEXTVIEW, requiredFieldAlertString);
    	TextView alertStringTextView = (TextView) UsbongUtils.applyTagsInView(new TextView(UsbongDecisionTreeEngineActivity.getInstance()), UsbongUtils.IS_TEXTVIEW, alertString);
    	
    	new AlertDialog.Builder(UsbongDecisionTreeEngineActivity.this).setTitle(alertStringTextView.getText().toString())
//		.setMessage(requiredFieldAlertStringTextView.toString())
		.setView(requiredFieldAlertStringTextView)
    	.setPositiveButton("OK", new DialogInterface.OnClickListener() {					
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		}).show();
	}
}