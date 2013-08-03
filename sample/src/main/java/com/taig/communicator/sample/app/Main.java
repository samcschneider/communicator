package com.taig.communicator.sample.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.taig.communicator.sample.R;
import com.taig.communicator.sample.io.*;

import java.util.HashMap;
import java.util.Map;

import static com.taig.communicator.sample.R.id.*;

public class Main extends Activity
{
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.main );

		final Map<Integer, Class<?>> activities = new HashMap<Integer, Class<?>>();
		activities.put( button_simple_request, SimpleRequest.class );
		activities.put( button_events, Events.class );
		activities.put( button_custom_result_parser, CustomResultParser.class );
		activities.put( button_form_send, FormSend.class );
		activities.put( button_cookies, Cookies.class );

		for( final Map.Entry<Integer, Class<?>> entry : activities.entrySet() )
		{
			findViewById( entry.getKey() ).setOnClickListener( new View.OnClickListener()
			{
				@Override
				public void onClick( View view )
				{
					startActivity(
						new Intent( Main.this, Example.class )
							.putExtra( Example.INTERACTION, entry.getValue().getName() ) );
				}
			} );
		}
	}
}