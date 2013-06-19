/**
Cette application a été développée par Nicolas -Nover- Guilloux.
Elle a été créée afin d'interagir avec YURI, lui-même créé par Idleman.
Trouvez les travaux d'Idleman ici : http://blog.idleman.fr/?p=1788
Vous pouvez me contacter à cette adresse : Etsu@live.fr
**/

package fr.nover.yuri;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
 	
public class All_In_One extends Activity implements TextToSpeech.OnInitListener {
	
	ImageButton btnRec;
	static EditText IPadress;
	TextView Rep;
	TextView Recrep;
	static TextView tts_pref_false;
		
    private TextToSpeech mTts;
	    
	private Intent ShakeService;
	static boolean  servstate=false;;
	    
	    // Juste une valeur fixe de référence
	protected static final int RESULT_SPEECH = 1;
	protected static final int OPTION = 2;
	private static final int MY_DATA_CHECK_CODE = 3;
    	
	public void onCreate(Bundle savedInstanceState)
        {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yuri);
            
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
    	StrictMode.setThreadPolicy(policy);
    		
    	IPadress = (EditText)findViewById(R.id.IPadress);
    	tts_pref_false = (TextView) findViewById(R.id.tts_pref_false);
    	Recrep = (TextView) findViewById(R.id.Recrep);
    	Rep = (TextView) findViewById(R.id.Rep);
    	btnRec = (ImageButton) findViewById(R.id.btnRec);
    	getConfig();
    		
    	btnRec.setOnClickListener(new View.OnClickListener() {	
    		@Override
    		public void onClick(View v){
    			Initialisation();}});}
            
	public void onActivityResult(int requestCode, int resultCode, Intent data) // Installe un TTS s'il n'y en a pas, ou créer le mTts
        {
        switch (requestCode) {
    	case RESULT_SPEECH: {
    		if (resultCode == RESULT_OK && null != data) {

    			ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

    			Recrep.setText(text.get(0));
    				
    			String IPAdress = IPadress.getText().toString();
    				
    				// Début de l'envoie au serveur
    			HttpClient httpclient = new DefaultHttpClient();
    			HttpPost httppost = new HttpPost("http://"+IPAdress);

    			try {
    			        // Prépare les variables et les envoient
    			    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
    			    nameValuePairs.add(new BasicNameValuePair("message", Recrep.getText().toString()));
    			    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));

    			        // Reçoit la réponse
    			    HttpResponse responsePOST = httpclient.execute(httppost);
    			    HttpEntity httpreponse = responsePOST.getEntity();
    			    String result = EntityUtils.toString(httpreponse).trim(); // La transforme en string
    			    Rep.setText(result);}
    			catch(Exception e){
    			        Log.e("log_tag", "Error in http connection"+e.toString());}
    		        
    		    SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
    			if (preferences.getBoolean("tts_pref", true)==true){
    			    	getTTS();}
    		     }
    			break;}
    	
    			case OPTION: {getConfig();}
    			
    			case MY_DATA_CHECK_CODE:{
    	            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS){
    	                    // success, create the TTS instance
    	                mTts = new TextToSpeech(this, this);}
    	        }
    	}}

    public void onInit(int i){
        	// S'exécute dès la création du mTts
        String Rep2=Rep.getText().toString();
        mTts.speak(Rep2,TextToSpeech.QUEUE_FLUSH,null);}
          
    public void onDestroy(){
        	// Quitte le TTS
        if (mTts != null){
            mTts.stop();
            mTts.shutdown();}
        super.onDestroy();}

    public void getConfig(){
    	SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
    	String V_string=preferences.getString("IPadress", "");
    	if(V_string != ""){
    		IPadress.setText(V_string);}
    		
    	boolean Box_tts=preferences.getBoolean("tts_pref", true);
    	if(Box_tts==false){
    		tts_pref_false.setText("Attention ! Votre TTS est désactivé.");}
    	else{
    		tts_pref_false.setText(" ");}
    		
    	ShakeService=new Intent(All_In_One.this, ShakeService.class);
    	boolean Box_shake=preferences.getBoolean("shake", true);
    	if((Box_shake==true) && servstate==false){
    		startService(ShakeService);}
    		
    	if((Box_shake==false) && servstate==true){
    		stopService(ShakeService);}
         }
    	
    public void getTTS(){
    	Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA); // Va vérifier si il y a un TTS de disponible
        startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);}
        
    public void Initialisation(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "fr-FR");
			
		try {
			startActivityForResult(intent, RESULT_SPEECH);
			Recrep.setText("");}
			
		catch (ActivityNotFoundException a) {
			Toast t = Toast.makeText(getApplicationContext(),
					"Oh bah zut alors ! Ton Android n'a pas installé le STT ou ne le supporte pas. Regarde les options (langue et saisie).",
					Toast.LENGTH_SHORT);
			t.show();}
        }       
    public static void getConfigChange(String IPAdress, boolean TTS){
    	
    	if(IPAdress != ""){
    		IPadress.setText(IPAdress);}
    		
    	if(TTS==false){
    		tts_pref_false.setText("Attention ! Votre TTS est désactivé.");}
    	else{
    		tts_pref_false.setText(" ");}
         }
        
    public boolean onCreateOptionsMenu(Menu menu) {
    		// Inflate the menu; this adds items to the action bar if it is present.
    	getMenuInflater().inflate(R.menu.yuri, menu);
    	return super.onCreateOptionsMenu(menu);}   
    	
    public boolean onOptionsItemSelected(MenuItem item){
    	if(item.getItemId() == R.id.configuration){
    		startActivityForResult(new Intent(this, Configuration.class), OPTION);}
    	return super.onOptionsItemSelected(item);}
    
	public static void ServiceState(boolean etat){
		servstate=etat;}
     
}