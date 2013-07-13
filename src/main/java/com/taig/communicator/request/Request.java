package com.taig.communicator.request;

import com.taig.communicator.io.Cancelabe;
import com.taig.communicator.event.Event;
import com.taig.communicator.event.State;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class Request<T> implements Cancelabe, Runnable
{
	protected String method;

	protected URL url;

	protected Event<T>.Proxy event;

	protected LoadingState state = new LoadingState();

	protected boolean cancelled = false;

	protected boolean cache = false;

	protected int chunkLength = -1;

	protected int connectTimeout = 0;

	protected int contentLength = -1;

	protected int modifiedSince = 0;

	protected int readTimeout = 0;

	protected boolean redirect = false;

	protected boolean userInteraction = false;

	protected Map<String, List<String>> headers = new HashMap<String, List<String>>();

	public Request( String method, URL url, Event<T> event )
	{
		this.method = method;
		this.url = url;

		if( event != null )
		{
			this.event = event.new Proxy();
		}
	}

	public State getState()
	{
		return state.current;
	}

	public URL getUrl()
	{
		return url;
	}

	public Event<T> getEvent()
	{
		return event.getEvent();
	}

	@Override
	public void cancel()
	{
		this.cancelled = true;
	}

	public boolean isCancelled()
	{
		return cancelled;
	}

	public Request<T> addHeader( String key, String value )
	{
		if( !this.headers.containsKey( key ) )
		{
			this.headers.put( key, new ArrayList<String>() );
		}

		this.headers.get( key ).add( value );
		return this;
	}

	public Request<T> addHeaders( String key, List<String> values )
	{
		if( !this.headers.containsKey( key ) )
		{
			this.headers.put( key, new ArrayList<String>() );
		}

		this.headers.get( key ).addAll( values );
		return this;
	}

	public Request<T> addHeaders( Map<String, List<String>> values )
	{
		this.headers.putAll( values );
		return this;
	}

	public Request<T> setHeaders( Map<String, List<String>> headers )
	{
		this.headers = headers;
		return this;
	}

	public Request<T> allowUserInteraction( boolean allow )
	{
		this.userInteraction = allow;
		return this;
	}

	public Request<T> ifModifiedSince( int modifiedSince )
	{
		this.modifiedSince = modifiedSince;
		return this;
	}

	public Request<T> followRedirects( boolean follow )
	{
		this.redirect = follow;
		return this;
	}

	public Request<T> streamChunks( int chunkLength )
	{
		this.chunkLength = chunkLength;
		return this;
	}

	public Request<T> streamFixedLength( int contentLength )
	{
		this.contentLength = contentLength;
		return this;
	}

	public Request<T> timeoutConnect( int timeout )
	{
		this.connectTimeout = timeout;
		return this;
	}

	public Request<T> timeoutRead( int timeout )
	{
		this.readTimeout = timeout;
		return this;
	}

	public Request<T> useCache( boolean use )
	{
		this.cache = use;
		return this;
	}

	public HttpURLConnection connect() throws IOException
	{
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setAllowUserInteraction( userInteraction );
		connection.setConnectTimeout( connectTimeout );
		connection.setDoInput( true );
		connection.setIfModifiedSince( modifiedSince );
		connection.setInstanceFollowRedirects( redirect );
		connection.setReadTimeout( readTimeout );
		connection.setRequestMethod( method );
		connection.setUseCaches( cache );

		if( chunkLength > 0 )
		{
			connection.setChunkedStreamingMode( chunkLength );
		}

		if( contentLength >= 0 )
		{
			connection.setFixedLengthStreamingMode( contentLength );
		}

		for( Map.Entry<String, List<String>> header : this.headers.entrySet() )
		{
			for( String value : header.getValue() )
			{
				connection.setRequestProperty( header.getKey(), value );
			}
		}

		return connection;
	}

	/**
	 * Execute this request.
	 *
	 * If an {@link Event} is specified, its finish callbacks {@link Event#onSuccess(Response)},
	 * {@link Event#onFailure(Throwable)}, {@link Event#onSuccess(Response)} and the according
	 * {@link Event#onEvent(com.taig.communicator.event.State)} calls will not be executed.
	 *
	 * @return	The {@link Response} object that keeps the connection response meta data (such as response code) and
	 * 			the payload that will be <code>null</code> if the HTTP server returned an error.
	 * @throws IOException
	 */
	public Response<T> request() throws IOException
	{
		state.start();
		HttpURLConnection connection = connect();

		try
		{
			send( connection );
			Response<T> response = new Response<T>(
					url,
					connection.getResponseCode(),
					connection.getResponseMessage(),
					connection.getHeaderFields(),
					receive( connection )
			);
			state.success();
			return response;
		}
		catch( IOException exception )
		{
			state.failure();
			throw exception;
		}
		finally
		{
			connection.disconnect();
		}
	}

	public void run()
	{
		try
		{
			state.success( request() );
		}
		catch( IOException exception )
		{
			state.failure( exception );
		}
	}

	protected abstract void send( HttpURLConnection connection ) throws IOException;

	protected abstract T receive( HttpURLConnection connection ) throws IOException;

	protected class LoadingState
	{
		protected State current = State.IDLE;

		public void start()
		{
			current = State.START;

			if( event != null )
			{
				event.start();
			}
		}

		public void cancel()
		{
			current = State.CANCEL;

			if( event != null )
			{
				event.cancel();
			}
		}

		public void send()
		{
			current = State.SEND;
		}

		public void sending( int current, int total )
		{
			if( event != null )
			{
				event.send( current, total );
			}
		}

		public void receive()
		{
			current = State.RECEIVE;
		}

		public void receiving( int current, int total )
		{
			if( event != null )
			{
				event.receive( current, total );
			}
		}

		public void success()
		{
			current = State.SUCCESS;
		}

		public void success( Response<T> response )
		{
			if( event != null )
			{
				event.success( response );
			}
		}

		public void failure()
		{
			current = State.FAILURE;
		}

		public void failure( Throwable error )
		{
			if( event != null )
			{
				event.failure( error );
			}
		}
	}
}