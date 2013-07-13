package com.taig.communicator.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import static com.taig.communicator.sample.R.id.button_custom_result_parser;
import static com.taig.communicator.sample.R.id.button_events;
import static com.taig.communicator.sample.R.id.button_simple_request;

public class Main extends Activity
{
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.main );

		findViewById( button_simple_request ).setOnClickListener( new View.OnClickListener()
		{
			@Override
			public void onClick( View view )
			{
				startActivity( new Intent( Main.this, SimpleRequest.class ) );
			}
		} );

		findViewById( button_events ).setOnClickListener( new View.OnClickListener()
		{
			@Override
			public void onClick( View view )
			{
				startActivity( new Intent( Main.this, Events.class ) );
			}
		} );

		findViewById( button_custom_result_parser ).setOnClickListener( new View.OnClickListener()
		{
			@Override
			public void onClick( View view )
			{
				startActivity( new Intent( Main.this, CustomResultParser.class ) );
			}
		} );
	}
}