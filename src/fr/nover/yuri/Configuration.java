/**
Cette application a �t� d�velopp�e par Nicolas -Nover- Guilloux.
Elle a �t� cr��e afin d'interagir avec YURI, lui-m�me cr�� par Idleman.
Trouvez les travaux d'Idleman ici : http://blog.idleman.fr/?p=1788
Vous pouvez me contacter � cette adresse : Etsu@live.fr
**/

package fr.nover.yuri;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Configuration extends PreferenceActivity {
	
	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.option_configuration);}
	
	protected void onPause(){}

}
