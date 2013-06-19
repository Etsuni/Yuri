package fr.nover.yuri;

import fr.nover.yuri.ShakeDetector.OnShakeListener;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.widget.Toast;

public class ShakeService extends Service implements TextToSpeech.OnInitListener, RecognitionListener{

	private ShakeDetector mShakeDetector; // Pour la d�tection du "shake"
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    
    private TextToSpeech mTts;
    Random random = new Random();
    
	String A_envoyer,Resultat="";
    
    String A_dire="";
	static String IPadress="";
    Boolean last_init=false, last_shake=false, Yuri=true;
	static Boolean TTS=true;
    
		// Logger tag
 	private static final String TAG="";
 		// Speech recognizer instance
 	private SpeechRecognizer speech = null;
 		// Timer used as timeout for the speech recognition
 	private Timer speechTimeout = null;
     
 		// Timer task used to reproduce the timeout input error that seems not be called on android 4.1.2
	public class SilenceTimer extends TimerTask {
		@Override
		public void run() {
			onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT);
		}
	}
	
	public void onCreate(){
    	super.onCreate();
    	
    	getConfig();
    	All_In_One.ServiceState(true);
        
    	mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mShakeDetector.setOnShakeListener(new OnShakeListener() {
            @Override
            public void onShake(int count) {
            	if(last_shake==false && ScreenReceiver.wasScreenOn){
            		last_shake=true;
            		A_dire=Random_String();
            		getTTS();}
                }
        });
        
        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        final BroadcastReceiver mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);
        SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
		IPadress=preferences.getString("IPadress", "");
     }

    public void onDestroy() {
        super.onDestroy();
        All_In_One.ServiceState(false);
        mShakeDetector.setOnShakeListener(new OnShakeListener(){
            @Override
            public void onShake(int count) {
            	//Disable 
                }
            });
        if (mTts != null){
        	mTts.stop();
            mTts.shutdown();}
    }
    
	public void onInit(int status) {
		Toast t = Toast.makeText(getApplicationContext(),A_dire,Toast.LENGTH_SHORT);
		t.show();
		mTts.speak(A_dire,TextToSpeech.QUEUE_FLUSH,null);
		if(last_init==false){
			android.os.SystemClock.sleep(1000);
			startVoiceRecognitionCycle();}
		else{fin();}
		}
	
	public void getTTS(){mTts = new TextToSpeech(this, this);}

	public IBinder onBind(Intent arg0) {return null;}
	
	private SpeechRecognizer getSpeechRevognizer(){
		if (speech == null) {
			speech = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
			speech.setRecognitionListener(this);}
		return speech;}

	public void startVoiceRecognitionCycle(){
		last_init=true;
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		getSpeechRevognizer().startListening(intent);}

	public void stopVoiceRecognition(){
		speechTimeout.cancel();
		if (speech != null) {
			speech.destroy();
			speech = null;}
	}

	public void onReadyForSpeech(Bundle params) {
		Log.d(TAG,"onReadyForSpeech");
		// create and schedule the input speech timeout
		speechTimeout = new Timer();
		speechTimeout.schedule(new SilenceTimer(), 3000);}

	public void onBeginningOfSpeech() {
		Log.d(TAG,"onBeginningOfSpeech");
		// Cancel the timeout because voice is arriving
		speechTimeout.cancel();}

	public void onBufferReceived(byte[] buffer) {Log.d(TAG,"onBufferReceived");}

	public void onEndOfSpeech() {Log.d(TAG,"onEndOfSpeech");}

	public void onError(int error) {
		String message;
		switch (error){
			case SpeechRecognizer.ERROR_AUDIO:
				message = "Erreur d'enregistrement audio";
				break;
			case SpeechRecognizer.ERROR_CLIENT:
				message = "Client side error";
				break;
			case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
				message = "Erreur de permission";
				break;
			case SpeechRecognizer.ERROR_NETWORK:
				message = "Erreur de r�seau";
				break;
			case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
				message = "Erreur de r�seau (trop long)";
				break;
			case SpeechRecognizer.ERROR_NO_MATCH:
				message = "Pas de correspondance";
				break;
			case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
				message = "La reconnaissance vocale est d�j� utilis�e";
				break;
			case SpeechRecognizer.ERROR_SERVER:
				message = "Erreur du serveur";
				break;
			case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
				message = "Pas d'ordre donn�";
				break;
			default:
				message = "Erreur inconnue";
				break;}
		
		Log.d(TAG,"onError code:" + error + " message: " + message);
		Toast t = Toast.makeText(getApplicationContext(),
				"Annul� : " + message.toString().toString(),
				Toast.LENGTH_SHORT);
		t.show();
		fin();}

	public void onResults(Bundle results) {
		String Resultat = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0).toString();
		Log.d(TAG,"Ordre : " + Resultat);
		
		if(Resultat.contains("Yuri") || Resultat.contains("Youri")){HTTP_Send(Resultat);}
		else{A_dire="Vous n'avez pas dit Yuri !";
			getTTS();}}

	public void onRmsChanged(float rmsdB) {}

	public void onPartialResults(Bundle arg0) {}

	public void HTTP_Send(String Enregistrement){
		Log.d(TAG,"Echange avec le serveur");
		
		// D�but de l'envoie au serveur
		HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost("http://"+IPadress);

	    try {
	        // Pr�pare les variables et les envoient
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair("message", Enregistrement.toString()));
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));

	        // Re�oit la r�ponse
	        HttpResponse responsePOST = httpclient.execute(httppost);
	        HttpEntity httpreponse = responsePOST.getEntity();
	        A_dire = EntityUtils.toString(httpreponse).trim();}
        
        catch(Exception e){
            Log.e("log_tag", "Error converting result "+e.toString());}
            
	    	Log.d(TAG,"R�ponse : "+A_dire);
            Toast t2 = Toast.makeText(getApplicationContext(),
    				A_dire.toString().toString(),
    				Toast.LENGTH_SHORT);
    		t2.show();
    		getTTS();}
    		
	public String Random_String(){
		ArrayList<String> list = new ArrayList<String>();
		list.add("Que voulez-vous, ma�tre ?");
		list.add("Comment puis-je vous aider, boss ?");
		list.add("Un truc � dire, ma poule ?!");
		
		int randomInt = random.nextInt(list.size());
        String Retour = list.get(randomInt).toString();
		
		return Retour;
	}

	public void onEvent(int arg0, Bundle arg1){}
	
	protected void onPause(){
	    	// If the screen is off then the device has been locked
	    PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
	    boolean isScreenOn = powerManager.isScreenOn();

	    if (!isScreenOn) {
	    	mShakeDetector.setOnShakeListener(new OnShakeListener() {
	            public void onShake(int count) {}});
	    }
	}
	
	protected void onResume(){
    	// If the screen is off then the device has been locked
    PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
    boolean isScreenOn = powerManager.isScreenOn();

    if (isScreenOn) {
    	mShakeDetector.setOnShakeListener(new OnShakeListener() {
            public void onShake(int count) {
            	if(last_shake==false){
	        		last_shake=true;
	        		A_dire=Random_String();
	        		getTTS();}
            }});
    }}

	public void fin(){
		android.os.SystemClock.sleep(3000);
		last_shake=last_init=false;
		mTts.stop();
        mTts.shutdown();}
	
	public void Etude_Resultat(String Resultat){
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
	    HttpGet httpGet = new HttpGet(IPadress+"/hcc/");
	    try {
	    	HttpResponse response = client.execute(httpGet);
	        StatusLine statusLine = response.getStatusLine();
	        int statusCode = statusLine.getStatusCode();
	        if (statusCode == 200) {
	          HttpEntity entity = response.getEntity();
	          InputStream content = entity.getContent();
	          BufferedReader reader = new BufferedReader(new InputStreamReader(content));
	          String line;
	          while ((line = reader.readLine()) != null) {
	            builder.append(line);}
	        String JSON = builder.toString();} 
	        else {
	          Log.e(ShakeService.class.toString(), "Failed to download file");}}
	    catch (ClientProtocolException e) {
	        e.printStackTrace();} 
	    catch (IOException e) {
	        e.printStackTrace();}
	    
		JSONObject object = new JSONObject();
		  try {
		    object.put("name", "Jack Hack");
		    object.put("score", new Integer(200));
		    object.put("current", new Double(152.32));
		    object.put("nickname", "Hacker");} 
		  catch (JSONException e) {
		    e.printStackTrace();}
		
	}

    public static void getConfigChange(String IPAdress, boolean Box_TTS){
    	if(IPAdress != ""){IPadress=IPAdress;}
    	TTS=Box_TTS;}
    
    public void getConfig(){
    	SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
    	String V_string=preferences.getString("IPadress", "");
    	if(V_string != ""){
    		IPadress=V_string;}
    		
    	boolean Box_tts=preferences.getBoolean("tts_pref", true);
    	TTS=Box_tts;}
}
